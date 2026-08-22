import copy
import json
import sys
import tempfile
import unittest
from email.message import Message
from pathlib import Path
from unittest.mock import MagicMock, patch


ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT))

from release.tools.release import (  # noqa: E402
    ReleaseError,
    generate_manifest,
    render_site,
    smoke_test,
    stage_site,
    validate_config,
    validate_tag,
    validate_public_links,
    validate_manifest,
)


class ReleaseToolsTest(unittest.TestCase):
    def setUp(self) -> None:
        self.temporary_directory = tempfile.TemporaryDirectory(dir=ROOT)
        self.workspace = Path(self.temporary_directory.name)
        self.artifacts_directory = self.workspace / "artifacts"
        self.artifacts_directory.mkdir()
        self.config = json.loads((ROOT / "release/release-config.json").read_text(encoding="utf-8"))
        for platform in self.config["platforms"]:
            file_name = platform["fileNameTemplate"].format(version="1.0.0")
            (self.artifacts_directory / file_name).write_bytes(f"fixture-{platform['platform']}".encode())

    def tearDown(self) -> None:
        self.temporary_directory.cleanup()

    def test_complete_release_manifest_and_staged_page_are_coherent(self) -> None:
        manifest = generate_manifest(
            self.config,
            version="1.0.0",
            tag="v1.0.0",
            repository="mihbor/snake",
            artifacts_dir=self.artifacts_directory,
            source_revision="fixture-revision",
        )
        validate_manifest(manifest)

        page_directory = self.workspace / "page"
        render_site(
            manifest,
            ROOT / "site/index.template.html",
            ROOT / "site/styles.css",
            page_directory,
        )
        page = (page_directory / "index.html").read_text(encoding="utf-8")
        self.assertIn("Snake", page)
        self.assertIn("1.0.0", page)
        self.assertIn("Play in browser", page)
        for label in ("Android", "Windows", "macOS", "Linux"):
            self.assertIn(label, page)

        browser_directory = self.workspace / "browser"
        browser_directory.mkdir()
        (browser_directory / "index.html").write_text(
            '<script src="composeApp.js"></script>',
            encoding="utf-8",
        )
        (browser_directory / "composeApp.js").write_text("console.log('fixture');", encoding="utf-8")
        (browser_directory / "game.wasm").write_bytes(b"wasm fixture")
        staged_directory = self.workspace / "staged"
        stage_site(
            manifest,
            ROOT / "site/index.template.html",
            ROOT / "site/styles.css",
            browser_directory,
            staged_directory,
        )
        self.assertTrue((staged_directory / "release.json").is_file())
        self.assertTrue((staged_directory / "releases/1.0.0/play/index.html").is_file())
        smoke_test(staged_directory, manifest, base_path="/snake/")
        smoke_test(staged_directory, manifest, base_path="/")

    def test_explicitly_unavailable_platform_has_no_current_link(self) -> None:
        config = copy.deepcopy(self.config)
        unavailable = next(platform for platform in config["platforms"] if platform["platform"] == "LINUX")
        unavailable["availability"] = "UNAVAILABLE"

        manifest = generate_manifest(
            config,
            artifacts_dir=self.artifacts_directory,
            source_revision="fixture-revision",
        )
        linux_artifact = next(artifact for artifact in manifest["artifacts"] if artifact["platform"] == "LINUX")
        self.assertEqual("UNAVAILABLE", linux_artifact["availability"])
        self.assertNotIn("downloadUrl", linux_artifact)

        output_directory = self.workspace / "page"
        render_site(
            manifest,
            ROOT / "site/index.template.html",
            ROOT / "site/styles.css",
            output_directory,
        )
        page = (output_directory / "index.html").read_text(encoding="utf-8")
        self.assertNotIn("snake-1.0.0-linux.deb", page)
        self.assertIn("Download Android APK for Snake 1.0.0", page)

    def test_mixed_release_and_duplicate_platforms_are_rejected(self) -> None:
        manifest = generate_manifest(
            self.config,
            artifacts_dir=self.artifacts_directory,
            source_revision="fixture-revision",
        )
        mixed_version = copy.deepcopy(manifest)
        mixed_version["artifacts"][0]["version"] = "2.0.0"
        with self.assertRaises(ReleaseError):
            validate_manifest(mixed_version)

        duplicate_platform = copy.deepcopy(manifest)
        duplicate_platform["artifacts"].append(copy.deepcopy(duplicate_platform["artifacts"][0]))
        with self.assertRaises(ReleaseError):
            validate_manifest(duplicate_platform)

    def test_missing_artifact_and_mutable_latest_url_are_rejected(self) -> None:
        missing = copy.deepcopy(self.config)
        missing["platforms"][0]["availability"] = "AVAILABLE"
        (self.artifacts_directory / "snake-1.0.0.apk").unlink()
        with self.assertRaises(ReleaseError):
            generate_manifest(missing, artifacts_dir=self.artifacts_directory)

        (self.artifacts_directory / "snake-1.0.0.apk").write_bytes(b"fixture-ANDROID")
        manifest = generate_manifest(self.config, artifacts_dir=self.artifacts_directory)
        manifest["artifacts"][0]["downloadUrl"] = "https://github.com/mihbor/snake/releases/download/latest/snake-1.0.0.apk"
        with self.assertRaises(ReleaseError):
            validate_manifest(manifest)

    def test_android_version_code_must_be_positive(self) -> None:
        config = copy.deepcopy(self.config)
        config["release"]["androidVersionCode"] = 0
        with self.assertRaises(ReleaseError):
            validate_config(config)

    def test_release_version_must_have_positive_major_for_native_packages(self) -> None:
        with self.assertRaises(ReleaseError):
            validate_tag("0.1.0", "v0.1.0")
        self.assertEqual("v1.0.0", validate_tag("1.0.0", "v1.0.0"))

    def test_public_link_redirect_to_another_release_is_rejected(self) -> None:
        config = copy.deepcopy(self.config)
        for platform in config["platforms"]:
            platform["availability"] = (
                "AVAILABLE" if platform["platform"] == "ANDROID" else "UNAVAILABLE"
            )
        manifest = generate_manifest(
            config,
            version="1.0.0",
            tag="v1.0.0",
            repository="mihbor/snake",
            artifacts_dir=self.artifacts_directory,
            source_revision="fixture-revision",
        )
        response = MagicMock()
        response.status = 200
        response.read.return_value = b"asset"
        response.geturl.return_value = (
            "https://github.com/mihbor/snake/releases/download/v0.9.0/snake-1.0.0.apk"
        )
        response.__enter__.return_value = response

        with patch("release.tools.release.urlopen", return_value=response):
            with self.assertRaises(ReleaseError):
                validate_public_links(manifest)

    def test_public_link_redirect_to_github_asset_host_is_checked_by_filename(self) -> None:
        config = copy.deepcopy(self.config)
        for platform in config["platforms"]:
            platform["availability"] = (
                "AVAILABLE" if platform["platform"] == "ANDROID" else "UNAVAILABLE"
            )
        manifest = generate_manifest(
            config,
            version="1.0.0",
            tag="v1.0.0",
            repository="mihbor/snake",
            artifacts_dir=self.artifacts_directory,
            source_revision="fixture-revision",
        )
        response = MagicMock()
        response.status = 200
        response.read.return_value = b"asset"
        response.geturl.return_value = "https://release-assets.githubusercontent.com/asset/123"
        headers = Message()
        headers.add_header("Content-Disposition", "attachment", filename="snake-1.0.0.apk")
        response.headers = headers
        response.__enter__.return_value = response

        with patch("release.tools.release.urlopen", return_value=response):
            validate_public_links(manifest)


if __name__ == "__main__":
    unittest.main()