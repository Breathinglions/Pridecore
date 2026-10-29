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
    "shield_alpha": "resonance_shield_alpha",
    "shield_beta": "resonance_shield_beta",
    "shield_gamma": "resonance_shield_gamma",
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
    assert entry["minBadges"] >= 0
    assert 1 <= len(entry["team"]) <= 6
    for pokemon in entry["team"]:
        assert re.fullmatch(r"[a-z0-9_-]+", pokemon["species"])
        assert isinstance(pokemon["level"], int) and 1 <= pokemon["level"] <= 100
# Rival continuity: single character, growing team, increasing progression gate.
rivals = [next(e for e in data["encounters"] if e["id"] == battle)
          for battle in ("rin_route1", "rin_canyon", "rin_summit")]
assert [r["minBadges"] for r in rivals] == sorted(r["minBadges"] for r in rivals)
assert [len(r["team"]) for r in rivals] == sorted(len(r["team"]) for r in rivals)
assert [max(p["level"] for p in r["team"]) for r in rivals] == sorted(
    max(p["level"] for p in r["team"]) for r in rivals)
# Ensure no unapproved battle content can override official League opponents.
assert all(e["actor"] not in {"thorn","rook","ashen","kai","vega","draven","freya","nyx","magnus"}
           for e in data["encounters"])
print("PASS: nine versioned story encounter roster definitions validated (NOT runtime tested)")
