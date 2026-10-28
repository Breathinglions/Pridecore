#!/usr/bin/env python3
"""Validate the authored Module 02 battle rosters without invoking Minecraft.

Rosters are content specifications, NOT live RCT/Cobblemon trainer definitions.
The runtime battle adapter and NPC battle launch are still required.
"""
import json
import pathlib
import re

ROOT = pathlib.Path(__file__).resolve().parents[1]
data = json.loads((ROOT / "campaign/story_battles.json").read_text(encoding="utf-8"))
expected = {
    "rin_route1": "rin",
    "rin_canyon": "rin",
    "rin_summit": "rin",
    "eclipse_lieutenant": "eclipse_operative",
    "sable": "sable",
    "director_cael": "director_cael",
}
assert data["schema"] == 1 and data["mode"] == "STAGING_ONLY"
assert len(data["encounters"]) == len(expected)
ids = [e["id"] for e in data["encounters"]]
assert set(ids) == set(expected) and len(ids) == len(set(ids))
for entry in data["encounters"]:
    assert entry["actor"] == expected[entry["id"]]
    assert isinstance(entry["minBadges"], int) and 0 <= entry["minBadges"] <= 8
    assert 1 <= len(entry["team"]) <= 6
    assert entry["intro"].strip() and entry["win"].strip()
    for pokemon in entry["team"]:
        assert re.fullmatch(r"[a-z0-9_-]+", pokemon["species"])
        assert isinstance(pokemon["level"], int) and 1 <= pokemon["level"] <= 100
print("PASS: six versioned story encounter roster definitions validated (NOT runtime tested)")
