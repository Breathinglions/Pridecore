#!/usr/bin/env python3
"""Regression checks for Module 02's additive FTB narrative overlay.

Run from the repository root:
    python3 modules/core/qa/test_ftb_story_overlay.py
"""
from __future__ import annotations

import json
import pathlib
import re
import subprocess
import sys
import tempfile

ROOT = pathlib.Path(__file__).resolve().parents[3]
GENERATOR = ROOT / "modules/core/campaign/build_ftb_story_chapters.py"
STORY = ROOT / "modules/core/src/main/resources/story/default.json"
EXPECTED_GROUPS = (6, 5, 5, 5, 4, 2)
KEY = re.compile(r"^\s*(chapter|quest|task)\.([0-9A-F]{16})\.[^:]+:")


def run() -> None:
    with tempfile.TemporaryDirectory(prefix="pride-m02-ftb-") as work:
        out = pathlib.Path(work) / "overlay"
        subprocess.run(
            [sys.executable, str(GENERATOR), "--story", str(STORY), "--out", str(out)],
            check=True, cwd=ROOT, capture_output=True, text=True
        )
        manifest = json.loads((out / "manifest.json").read_text(encoding="utf-8"))
        assert manifest["no_rewards"] is True
        assert manifest["existing_files_untouched"] is True
        assert len(manifest["chapters"]) == 6
        assert tuple(ch["count"] for ch in manifest["chapters"]) == EXPECTED_GROUPS
        assert len(manifest["quests"]) == 27
        chapters = list((out / "chapters").glob("*.snbt"))
        assert len(chapters) == 6
        for chapter in chapters:
            data = chapter.read_text(encoding="utf-8")
            assert "rewards: [ ]" in data
            assert "type: \"advancement\"" in data
        entries = (out / "PRIDE_STORY_EN_US_IMPORT.snbtfrag").read_text(encoding="utf-8").splitlines()
        title_keys = [line.split(":", 1)[0].strip() for line in entries if KEY.match(line)]
        assert title_keys, "Expected FTB localization keys"
        assert not any(line.startswith("\\t") for line in entries), "Literal slash-t in localization"
        assert len(set(title_keys)) == len(title_keys), "Duplicate localization keys"
        badge_tasks = [line for line in entries if "Win the official " in line]
        assert len(badge_tasks) == 9, "Expected eight gym and Champion task titles"
        assert all(line.startswith("\t") for line in badge_tasks), "Incorrect gym task key indentation"
        ids = set()
        for chapter in manifest["chapters"]:
            assert chapter["id"] not in ids
            ids.add(chapter["id"])
        for item in manifest["quests"].values():
            for token in (item["id"], item["task"]):
                assert token not in ids, "FTB stable ID collision"
                ids.add(token)
        print("PASS: Module 02 FTB overlay: 6 chapters / 27 quests / 9 badge titles / stable IDs")


if __name__ == "__main__":
    run()
