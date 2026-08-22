#!/usr/bin/env python3
"""Build and validate the immutable release metadata used by the Snake site."""

from __future__ import annotations

import argparse
import hashlib
import html
import json
import os
import re
import shutil
import sys
import tempfile
from http.server import SimpleHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path
from threading import Thread
from urllib.error import HTTPError, URLError
from urllib.parse import unquote, urlparse
from urllib.request import Request, urlopen


VERSION_PATTERN = re.compile(r"^[1-9][0-9]*\.(?:0|[1-9][0-9]*)\.(?:0|[1-9][0-9]*)$")
SHA256_PATTERN = re.compile(r"^[a-fA-F0-9]{64}$")
PLATFORM_FORMATS = {
    "ANDROID": "APK",
    "WINDOWS": "MSI",
    "MACOS": "DMG",
    "LINUX": "DEB",
}
PLATFORM_FILE_NAMES = {
    "ANDROID": "snake-{version}.apk",
    "WINDOWS": "snake-{version}-windows.msi",
    "MACOS": "snake-{version}-macos.dmg",
    "LINUX": "snake-{version}-linux.deb",
}
PLATFORM_LABELS = {
    "ANDROID": "Android",
    "WINDOWS": "Windows",
    "MACOS": "macOS",
    "LINUX": "Linux",
}
FORMAT_LABELS = {
    "APK": "APK",
    "MSI": "MSI",
    "DMG": "DMG",
    "DEB": "DEB",
}
PUBLIC_ASSET_HOSTS = {
    "github.com",
    "objects.githubusercontent.com",
    "release-assets.githubusercontent.com",
}


class ReleaseError(ValueError):
    """Raised when release input cannot be safely published."""


def load_json(path: Path) -> dict:
    try:
        with path.open(encoding="utf-8") as stream:
            value = json.load(stream)
    except (OSError, json.JSONDecodeError) as error:
        raise ReleaseError(f"Could not read JSON file {path}: {error}") from error
    if not isinstance(value, dict):
        raise ReleaseError(f"JSON document {path} must contain an object")
    return value


def write_json(path: Path, value: dict) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open("w", encoding="utf-8", newline="\n") as stream:
        json.dump(value, stream, indent=2, sort_keys=True)
        stream.write("\n")


def validate_version(version: str) -> str:
    if not isinstance(version, str) or not VERSION_PATTERN.fullmatch(version):
        raise ReleaseError(
            "release version must use semantic-version text with a positive major version "
            "such as 1.0.0"
        )
    return version


def validate_tag(version: str, tag: str) -> str:
    validate_version(version)
    if tag != f"v{version}":
        raise ReleaseError(f"release tag {tag!r} must match version as v{version}")
    return tag


def validate_base_path(base_path: str) -> str:
    if (
        not isinstance(base_path, str)
        or not base_path.startswith("/")
        or not base_path.endswith("/")
        or "//" in base_path
        or ".." in base_path
        or any(character.isspace() for character in base_path)
    ):
        raise ReleaseError("pagesBasePath must be a clean absolute path ending with '/'")
    return base_path


def validate_repository(repository: str) -> str:
    if not isinstance(repository, str) or not re.fullmatch(r"[^/\s]+/[^/\s]+", repository):
        raise ReleaseError("repository must have the owner/name form")
    return repository


def validate_config(config: dict) -> dict:
    required = {"gameName", "pagesBasePath", "release", "browser", "platforms"}
    missing = required.difference(config)
    if missing:
        raise ReleaseError(f"release configuration is missing: {', '.join(sorted(missing))}")
    if not isinstance(config["gameName"], str) or not config["gameName"].strip():
        raise ReleaseError("gameName must be a non-empty string")
    validate_base_path(config["pagesBasePath"])

    release = config["release"]
    if not isinstance(release, dict):
        raise ReleaseError("release configuration must be an object")
    default_version = release.get("defaultVersion")
    validate_version(default_version)
    android_version_code = release.get("androidVersionCode")
    if (
        isinstance(android_version_code, bool)
        or not isinstance(android_version_code, int)
        or android_version_code <= 0
    ):
        raise ReleaseError("release.androidVersionCode must be a positive integer")

    browser = config["browser"]
    if not isinstance(browser, dict):
        raise ReleaseError("browser configuration must be an object")
    if browser.get("availability") != "AVAILABLE":
        raise ReleaseError("the browser artifact must be AVAILABLE")
    path_template = browser.get("pathTemplate")
    if not isinstance(path_template, str) or "{version}" not in path_template:
        raise ReleaseError("browser.pathTemplate must contain {version}")
    browser_path = path_template.format(version=default_version)
    validate_relative_versioned_path(browser_path, default_version, "browser.pathTemplate")

    platforms = config["platforms"]
    if not isinstance(platforms, list):
        raise ReleaseError("platforms must be an array")
    seen: set[str] = set()
    for entry in platforms:
        if not isinstance(entry, dict):
            raise ReleaseError("each platform configuration must be an object")
        platform = entry.get("platform")
        package_format = entry.get("format")
        if platform not in PLATFORM_FORMATS:
            raise ReleaseError(f"unsupported platform: {platform!r}")
        if platform in seen:
            raise ReleaseError(f"duplicate platform configuration: {platform}")
        seen.add(platform)
        if package_format != PLATFORM_FORMATS[platform]:
            raise ReleaseError(f"{platform} must use format {PLATFORM_FORMATS[platform]}")
        if entry.get("availability") not in {"AVAILABLE", "UNAVAILABLE"}:
            raise ReleaseError(f"{platform} has invalid availability")
        template = entry.get("fileNameTemplate")
        if not isinstance(template, str) or "{version}" not in template:
            raise ReleaseError(f"{platform} fileNameTemplate must contain {{version}}")
        file_name = template.format(version=default_version)
        validate_file_name(file_name)
        expected_file = PLATFORM_FILE_NAMES[platform].format(version=default_version)
        if file_name != expected_file:
            raise ReleaseError(f"{platform} fileNameTemplate must produce {expected_file}")
        if "latest" in template.lower():
            raise ReleaseError(f"{platform} fileNameTemplate must not use latest")
    missing_platforms = set(PLATFORM_FORMATS).difference(seen)
    if missing_platforms:
        raise ReleaseError(
            f"release configuration must explicitly define: {', '.join(sorted(missing_platforms))}"
        )
    if "latest" in json.dumps(config, sort_keys=True).lower():
        raise ReleaseError("release configuration must not contain mutable latest URLs")
    return config


def validate_relative_versioned_path(path: str, version: str, field: str) -> str:
    if (
        not isinstance(path, str)
        or path.startswith("/")
        or not path.endswith("/")
        or "//" in path
        or ".." in path
        or f"/{version}/" not in f"/{path}"
    ):
        raise ReleaseError(f"{field} must be a relative versioned directory path")
    return path


def validate_file_name(file_name: str) -> str:
    if (
        not isinstance(file_name, str)
        or not file_name
        or "/" in file_name
        or "\\" in file_name
        or file_name in {".", ".."}
    ):
        raise ReleaseError(f"invalid artifact file name: {file_name!r}")
    return file_name


def expected_file_name(platform_config: dict, version: str) -> str:
    file_name = platform_config["fileNameTemplate"].format(version=version)
    validate_file_name(file_name)
    return file_name


def sha256_file(path: Path) -> str:
    digest = hashlib.sha256()
    try:
        with path.open("rb") as stream:
            for chunk in iter(lambda: stream.read(1024 * 1024), b""):
                digest.update(chunk)
    except OSError as error:
        raise ReleaseError(f"could not read artifact {path}: {error}") from error
    return digest.hexdigest()


def release_asset_url(repository: str, tag: str, file_name: str) -> str:
    validate_repository(repository)
    validate_file_name(file_name)
    return f"https://github.com/{repository}/releases/download/{tag}/{file_name}"


def generate_manifest(
    config: dict,
    version: str | None = None,
    tag: str | None = None,
    repository: str = "mihbor/snake",
    artifacts_dir: Path = Path("release/artifacts"),
    source_revision: str = "local",
    pages_base_path: str | None = None,
) -> dict:
    validate_config(config)
    release_config = config["release"]
    version = validate_version(version or release_config["defaultVersion"])
    tag = validate_tag(version, tag or f"v{version}")
    validate_repository(repository)
    if not isinstance(source_revision, str) or not source_revision.strip():
        raise ReleaseError("sourceRevision must be a non-empty string")
    base_path = validate_base_path(pages_base_path or config["pagesBasePath"])

    browser_config = config["browser"]
    browser_path = browser_config["pathTemplate"].format(version=version)
    validate_relative_versioned_path(browser_path, version, "browser.path")
    manifest = {
        "release": {"version": version, "tag": tag},
        "gameName": config["gameName"],
        "pagesBasePath": base_path,
        "browser": {
            "version": version,
            "availability": "AVAILABLE",
            "playUrl": browser_path,
            "path": browser_path,
        },
        "artifacts": [],
        "sourceRevision": source_revision,
    }

    for platform_config in config["platforms"]:
        platform = platform_config["platform"]
        package_format = platform_config["format"]
        file_name = expected_file_name(platform_config, version)
        availability = platform_config["availability"]
        artifact = {
            "platform": platform,
            "version": version,
            "format": package_format,
            "availability": availability,
            "fileName": file_name,
        }
        if availability == "AVAILABLE":
            path = artifacts_dir / file_name
            if not path.is_file():
                raise ReleaseError(f"available {platform} artifact is missing: {path}")
            if path.stat().st_size <= 0:
                raise ReleaseError(f"available {platform} artifact is empty: {path}")
            artifact["downloadUrl"] = release_asset_url(repository, tag, file_name)
            artifact["sha256"] = sha256_file(path)
        manifest["artifacts"].append(artifact)
    validate_manifest(manifest)
    return manifest


def validate_manifest(manifest: dict) -> dict:
    required = {"release", "gameName", "pagesBasePath", "browser", "artifacts", "sourceRevision"}
    missing = required.difference(manifest)
    if missing:
        raise ReleaseError(f"manifest is missing: {', '.join(sorted(missing))}")
    release = manifest["release"]
    if not isinstance(release, dict):
        raise ReleaseError("manifest.release must be an object")
    version = validate_version(release.get("version"))
    validate_tag(version, release.get("tag"))
    if not isinstance(manifest["gameName"], str) or not manifest["gameName"].strip():
        raise ReleaseError("manifest.gameName must be non-empty")
    validate_base_path(manifest["pagesBasePath"])
    if not isinstance(manifest["sourceRevision"], str) or not manifest["sourceRevision"].strip():
        raise ReleaseError("manifest.sourceRevision must be non-empty")

    browser = manifest["browser"]
    if not isinstance(browser, dict):
        raise ReleaseError("manifest.browser must be an object")
    if browser.get("version") != version:
        raise ReleaseError("browser version does not match release version")
    if browser.get("availability") != "AVAILABLE":
        raise ReleaseError("browser must be available for publication")
    browser_path = browser.get("path")
    if browser.get("playUrl") != browser_path:
        raise ReleaseError("browser playUrl and path must match")
    validate_relative_versioned_path(browser_path, version, "browser.path")

    artifacts = manifest["artifacts"]
    if not isinstance(artifacts, list):
        raise ReleaseError("manifest.artifacts must be an array")
    seen: set[str] = set()
    for artifact in artifacts:
        if not isinstance(artifact, dict):
            raise ReleaseError("each manifest artifact must be an object")
        platform = artifact.get("platform")
        if platform not in PLATFORM_FORMATS:
            raise ReleaseError(f"unsupported manifest platform: {platform!r}")
        if platform in seen:
            raise ReleaseError(f"duplicate manifest platform: {platform}")
        seen.add(platform)
        package_format = artifact.get("format")
        if package_format != PLATFORM_FORMATS[platform]:
            raise ReleaseError(f"{platform} must use format {PLATFORM_FORMATS[platform]}")
        if artifact.get("version") != version:
            raise ReleaseError(f"{platform} version does not match release version")
        file_name = validate_file_name(artifact.get("fileName"))
        expected_file = PLATFORM_FILE_NAMES[platform].format(version=version)
        if file_name != expected_file:
            raise ReleaseError(f"{platform} file name must be {expected_file}")
        availability = artifact.get("availability")
        if availability not in {"AVAILABLE", "UNAVAILABLE"}:
            raise ReleaseError(f"{platform} has invalid availability")
        if "latest" in json.dumps(artifact, sort_keys=True).lower():
            raise ReleaseError(f"{platform} contains a mutable latest reference")
        if availability == "AVAILABLE":
            url = artifact.get("downloadUrl")
            parsed = urlparse(url) if isinstance(url, str) else None
            if (
                parsed is None
                or parsed.scheme != "https"
                or parsed.netloc != "github.com"
                or parsed.params
                or parsed.query
                or parsed.fragment
                or not re.fullmatch(
                    rf"/[^/]+/[^/]+/releases/download/{re.escape(release['tag'])}/{re.escape(file_name)}",
                    parsed.path,
                )
            ):
                raise ReleaseError(f"{platform} downloadUrl is not a tag-scoped public GitHub URL")
            sha = artifact.get("sha256")
            if not isinstance(sha, str) or not SHA256_PATTERN.fullmatch(sha):
                raise ReleaseError(f"{platform} requires a 64-character sha256")
        else:
            if artifact.get("downloadUrl") is not None:
                raise ReleaseError(f"unavailable {platform} must not have a downloadUrl")
            if artifact.get("sha256") is not None:
                raise ReleaseError(f"unavailable {platform} must not have a sha256")
    missing_platforms = set(PLATFORM_FORMATS).difference(seen)
    if missing_platforms:
        raise ReleaseError(
            f"manifest must explicitly define: {', '.join(sorted(missing_platforms))}"
        )
    return manifest


def read_template(path: Path) -> str:
    try:
        return path.read_text(encoding="utf-8")
    except OSError as error:
        raise ReleaseError(f"could not read template {path}: {error}") from error


def download_items(manifest: dict) -> str:
    version = manifest["release"]["version"]
    game_name = html.escape(manifest["gameName"])
    items: list[str] = []
    for artifact in manifest["artifacts"]:
        platform = artifact["platform"]
        platform_label = PLATFORM_LABELS[platform]
        format_label = FORMAT_LABELS[artifact["format"]]
        if artifact["availability"] == "AVAILABLE":
            label = f"Download {platform_label} {format_label} for {game_name} {version}"
            items.append(
                "        <li class=\"download-item\">"
                f"<a class=\"download-link\" href=\"{html.escape(artifact['downloadUrl'], quote=True)}\">"
                f"{html.escape(label)}"
                "</a></li>"
            )
    if not items:
        return '        <li class="download-item download-item--unavailable">No native downloads are available for this release.</li>'
    return "\n".join(items)


def render_site(manifest: dict, template_path: Path, styles_path: Path, output_dir: Path) -> None:
    validate_manifest(manifest)
    template = read_template(template_path)
    version = manifest["release"]["version"]
    values = {
        "GAME_NAME": html.escape(manifest["gameName"]),
        "VERSION": html.escape(version),
        "PLAY_URL": html.escape(manifest["browser"]["playUrl"], quote=True),
        "DOWNLOAD_ITEMS": download_items(manifest),
    }
    rendered = template
    for key, value in values.items():
        rendered = rendered.replace("{{" + key + "}}", value)
    unresolved = re.findall(r"\{\{[^}]+\}\}", rendered)
    if unresolved:
        raise ReleaseError(f"landing page contains unresolved placeholders: {', '.join(unresolved)}")
    if version not in rendered or manifest["gameName"] not in rendered:
        raise ReleaseError("landing page is missing the current release identity")
    output_dir.mkdir(parents=True, exist_ok=True)
    try:
        (output_dir / "index.html").write_text(rendered, encoding="utf-8", newline="\n")
        shutil.copyfile(styles_path, output_dir / "styles.css")
    except OSError as error:
        raise ReleaseError(f"could not write landing page to {output_dir}: {error}") from error


def verify_staged_site(staged_dir: Path, manifest: dict) -> None:
    validate_manifest(manifest)
    index_path = staged_dir / "index.html"
    release_path = staged_dir / "release.json"
    browser_path = staged_dir / manifest["browser"]["path"]
    if not index_path.is_file() or not release_path.is_file():
        raise ReleaseError("staged site must contain index.html and release.json")
    if not browser_path.is_dir() or not (browser_path / "index.html").is_file():
        raise ReleaseError("staged site is missing the browser distribution")
    page = index_path.read_text(encoding="utf-8")
    play_url = manifest["browser"]["playUrl"]
    if f'href="{play_url}"' not in page:
        raise ReleaseError("staged landing page does not point to the manifest browser path")
    for artifact in manifest["artifacts"]:
        url = artifact.get("downloadUrl")
        if artifact["availability"] == "AVAILABLE" and url and f'href="{html.escape(url, quote=True)}"' not in page:
            raise ReleaseError(f"staged landing page is missing {artifact['platform']} action")


def stage_site(
    manifest: dict,
    template_path: Path,
    styles_path: Path,
    browser_dir: Path,
    output_dir: Path,
) -> None:
    validate_manifest(manifest)
    if not browser_dir.is_dir() or not (browser_dir / "index.html").is_file():
        raise ReleaseError(f"browser distribution is missing index.html: {browser_dir}")
    output_dir.parent.mkdir(parents=True, exist_ok=True)
    temporary_dir: Path | None = Path(tempfile.mkdtemp(prefix=f".{output_dir.name}-", dir=output_dir.parent))
    try:
        render_site(manifest, template_path, styles_path, temporary_dir)
        write_json(temporary_dir / "release.json", manifest)
        browser_target = temporary_dir / manifest["browser"]["path"]
        browser_target.parent.mkdir(parents=True, exist_ok=True)
        shutil.copytree(browser_dir, browser_target)
        verify_staged_site(temporary_dir, manifest)
        if output_dir.exists():
            shutil.rmtree(output_dir)
        os.replace(temporary_dir, output_dir)
        temporary_dir = None
    except OSError as error:
        raise ReleaseError(f"could not stage site: {error}") from error
    finally:
        if temporary_dir is not None and temporary_dir.exists():
            shutil.rmtree(temporary_dir, ignore_errors=True)


def smoke_test(site_dir: Path, manifest: dict, base_path: str | None = None, timeout: float = 10.0) -> None:
    validate_manifest(manifest)
    if not site_dir.is_dir():
        raise ReleaseError(f"staged site directory is missing: {site_dir}")
    served_base_path = validate_base_path(base_path or manifest["pagesBasePath"])
    with tempfile.TemporaryDirectory(dir=site_dir.parent) as directory:
        server_root = Path(directory)
        mapped_site = server_root / served_base_path.lstrip("/").rstrip("/")
        if served_base_path == "/":
            mapped_site = server_root
        mapped_site.parent.mkdir(parents=True, exist_ok=True)
        shutil.copytree(site_dir, mapped_site, dirs_exist_ok=True)

        class QuietHandler(SimpleHTTPRequestHandler):
            def log_message(self, format: str, *args: object) -> None:
                return

        def handler(*args: object, **kwargs: object) -> QuietHandler:
            return QuietHandler(*args, directory=str(server_root), **kwargs)

        server = ThreadingHTTPServer(("127.0.0.1", 0), handler)
        thread = Thread(target=server.serve_forever, daemon=True)
        thread.start()
        try:
            origin = f"http://127.0.0.1:{server.server_port}"

            def fetch(relative_path: str) -> bytes:
                request_path = f"/{served_base_path.lstrip('/')}{relative_path}"
                request = Request(f"{origin}{request_path}", headers={"User-Agent": "snake-local-smoke/1.0"})
                try:
                    with urlopen(request, timeout=timeout) as response:
                        if response.status != 200:
                            raise ReleaseError(f"local smoke request returned HTTP {response.status}: {relative_path}")
                        return response.read()
                except (HTTPError, URLError, TimeoutError) as error:
                    raise ReleaseError(f"local smoke request failed for {relative_path}: {error}") from error

            page = fetch("index.html").decode("utf-8")
            css = fetch("styles.css")
            release_json = json.loads(fetch("release.json").decode("utf-8"))
            if release_json != manifest:
                raise ReleaseError("staged release.json differs from the validated manifest")
            play_url = manifest["browser"]["playUrl"]
            if f'href="{play_url}"' not in page:
                raise ReleaseError("local smoke page is missing the versioned browser action")
            if not css:
                raise ReleaseError("local smoke page returned an empty stylesheet")

            browser_path = manifest["browser"]["path"]
            browser_entry = fetch(f"{browser_path}index.html").decode("utf-8")
            script_sources = re.findall(r"<script[^>]+src=[\"']([^\"']+)[\"']", browser_entry)
            if not script_sources:
                raise ReleaseError("browser entry point does not declare a script")
            for source in script_sources:
                if source.startswith(("/", "http://", "https://")):
                    raise ReleaseError("browser entry point contains an absolute script path")
                if not fetch(f"{browser_path}{source}"):
                    raise ReleaseError(f"browser script is empty: {source}")
            browser_asset_dir = site_dir / browser_path
            wasm_files = {
                asset.name
                for asset in browser_asset_dir.iterdir()
                if asset.is_file() and asset.suffix == ".wasm"
            }
            if not wasm_files:
                raise ReleaseError("browser distribution does not contain a Wasm asset")
            for wasm_file in wasm_files:
                if not fetch(f"{browser_path}{wasm_file}"):
                    raise ReleaseError(f"browser Wasm asset is empty: {wasm_file}")
        finally:
            server.shutdown()
            server.server_close()
            thread.join(timeout=timeout)


def validate_public_links(manifest: dict, timeout: float = 20.0) -> None:
    validate_manifest(manifest)
    for artifact in manifest["artifacts"]:
        if artifact["availability"] != "AVAILABLE":
            continue
        url = artifact["downloadUrl"]
        request = Request(url, headers={"User-Agent": "snake-release-validator/1.0"})
        try:
            with urlopen(request, timeout=timeout) as response:
                if response.status < 200 or response.status >= 400:
                    raise ReleaseError(f"{artifact['platform']} URL returned HTTP {response.status}")
                if response.read(1) == b"":
                    raise ReleaseError(f"{artifact['platform']} URL returned an empty asset")
                requested_url = urlparse(url)
                final_url_text = response.geturl()
                final_url = urlparse(final_url_text) if isinstance(final_url_text, str) else None
                if final_url is None or final_url.scheme != "https" or final_url.hostname not in PUBLIC_ASSET_HOSTS:
                    raise ReleaseError(f"{artifact['platform']} URL redirected outside public GitHub asset hosts")
                if final_url.hostname == requested_url.hostname and final_url.path != requested_url.path:
                    raise ReleaseError(f"{artifact['platform']} URL redirected to a different asset")
                if final_url.hostname == requested_url.hostname:
                    final_file_name = unquote(final_url.path).rsplit("/", maxsplit=1)[-1]
                    if final_file_name != artifact["fileName"]:
                        raise ReleaseError(f"{artifact['platform']} URL redirected to a different asset")
                else:
                    headers = getattr(response, "headers", None)
                    get_filename = getattr(headers, "get_filename", None)
                    final_file_name = get_filename() if callable(get_filename) else None
                    if final_file_name is not None and final_file_name != artifact["fileName"]:
                        raise ReleaseError(f"{artifact['platform']} URL returned a different asset")
        except (HTTPError, URLError, TimeoutError) as error:
            raise ReleaseError(f"{artifact['platform']} URL is not publicly reachable: {error}") from error


def command_generate_manifest(args: argparse.Namespace) -> None:
    config = validate_config(load_json(args.config))
    manifest = generate_manifest(
        config=config,
        version=args.version,
        tag=args.tag,
        repository=args.repository,
        artifacts_dir=args.artifacts_dir,
        source_revision=args.source_revision,
        pages_base_path=args.pages_base_path,
    )
    write_json(args.output, manifest)


def command_validate_manifest(args: argparse.Namespace) -> None:
    validate_manifest(load_json(args.input))


def command_validate_config(args: argparse.Namespace) -> None:
    validate_config(load_json(args.input))


def command_render_site(args: argparse.Namespace) -> None:
    manifest = validate_manifest(load_json(args.manifest))
    render_site(manifest, args.template, args.styles, args.output)


def command_stage(args: argparse.Namespace) -> None:
    manifest = validate_manifest(load_json(args.manifest))
    stage_site(manifest, args.template, args.styles, args.browser_dir, args.output)


def command_smoke_test(args: argparse.Namespace) -> None:
    smoke_test(args.site, load_json(args.manifest), args.base_path, args.timeout)


def command_validate_links(args: argparse.Namespace) -> None:
    validate_public_links(load_json(args.manifest), timeout=args.timeout)


def add_common_paths(parser: argparse.ArgumentParser) -> None:
    parser.add_argument("--manifest", type=Path, required=True)


def build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(description=__doc__)
    commands = parser.add_subparsers(dest="command", required=True)

    generate = commands.add_parser("generate-manifest")
    generate.add_argument("--config", type=Path, required=True)
    generate.add_argument("--version")
    generate.add_argument("--tag")
    generate.add_argument("--repository", default=os.environ.get("GITHUB_REPOSITORY", "mihbor/snake"))
    generate.add_argument("--artifacts-dir", type=Path, default=Path("release/artifacts"))
    generate.add_argument("--source-revision", default=os.environ.get("GITHUB_SHA", "local"))
    generate.add_argument("--pages-base-path")
    generate.add_argument("--output", type=Path, required=True)
    generate.set_defaults(function=command_generate_manifest)

    validate = commands.add_parser("validate-manifest")
    validate.add_argument("--input", type=Path, required=True)
    validate.set_defaults(function=command_validate_manifest)

    config = commands.add_parser("validate-config")
    config.add_argument("--input", type=Path, required=True)
    config.set_defaults(function=command_validate_config)

    render = commands.add_parser("render-site")
    add_common_paths(render)
    render.add_argument("--template", type=Path, required=True)
    render.add_argument("--styles", type=Path, required=True)
    render.add_argument("--output", type=Path, required=True)
    render.set_defaults(function=command_render_site)

    stage = commands.add_parser("stage")
    add_common_paths(stage)
    stage.add_argument("--template", type=Path, required=True)
    stage.add_argument("--styles", type=Path, required=True)
    stage.add_argument("--browser-dir", type=Path, required=True)
    stage.add_argument("--output", type=Path, required=True)
    stage.set_defaults(function=command_stage)

    smoke = commands.add_parser("smoke-test")
    smoke.add_argument("--manifest", type=Path, required=True)
    smoke.add_argument("--site", type=Path, required=True)
    smoke.add_argument("--base-path")
    smoke.add_argument("--timeout", type=float, default=10.0)
    smoke.set_defaults(function=command_smoke_test)

    links = commands.add_parser("validate-links")
    add_common_paths(links)
    links.add_argument("--timeout", type=float, default=20.0)
    links.set_defaults(function=command_validate_links)

    tag = commands.add_parser("validate-tag")
    tag.add_argument("--version", required=True)
    tag.add_argument("--tag", required=True)
    tag.set_defaults(function=lambda args: validate_tag(args.version, args.tag))
    return parser


def main(argv: list[str] | None = None) -> int:
    args = build_parser().parse_args(argv)
    try:
        args.function(args)
    except (ReleaseError, OSError) as error:
        print(f"release validation failed: {error}", file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    raise SystemExit(main())