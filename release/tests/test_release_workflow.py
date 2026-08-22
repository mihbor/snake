import unittest
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]


class ReleaseWorkflowTest(unittest.TestCase):
    def test_publication_runs_from_main_without_a_pushed_tag(self) -> None:
        workflow = (ROOT / ".github/workflows/release.yml").read_text(encoding="utf-8")

        self.assertIn("branches:\n      - main", workflow)
        self.assertIn("workflow_dispatch:", workflow)
        self.assertNotIn("tags:\n", workflow)
        self.assertNotIn("GITHUB_REF_NAME", workflow)
        self.assertIn('release_tag="v${version}"', workflow)
        self.assertIn('gh release create "$release_tag"', workflow)
        self.assertIn('--target "$GITHUB_SHA"', workflow)
        self.assertIn('--tag "$release_tag"', workflow)


if __name__ == "__main__":
    unittest.main()