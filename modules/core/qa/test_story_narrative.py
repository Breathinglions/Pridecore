#!/usr/bin/env python3
"""No game instructions in authored narrative; story gates may never drift."""
from pathlib import Path
import json
import re

ROOT = Path(__file__).resolve().parents[1]
RESOURCES = ROOT / "src/main/resources/story"
story = json.loads((RESOURCES / "default.json").read_text(encoding="utf-8"))
legacy = json.loads((RESOURCES / "legacy_stock_dialogue_v1.json").read_text(encoding="utf-8"))
rosters = json.loads((RESOURCES / "story_battles.json").read_text(encoding="utf-8"))
campaign = json.loads((ROOT / "campaign/story_battles.json").read_text(encoding="utf-8"))
assert campaign == rosters, "The story battle rosters are not identical in bundle/campaign"

# The migration cannot quietly rewrite a player's gates, grants or dialogue
# graph. Exactly the text/choice-label fields are permitted to differ.
def without_prose(value):
    if isinstance(value, dict):
        return {k: without_prose(v) for k, v in value.items() if k != "text"}
    if isinstance(value, list):
        return [without_prose(v) for v in value]
    return value

assert without_prose(story) == without_prose(legacy), (
    "Narrative rewrite accidentally changed facts, gates, options or chapter order"
)
assert len(story["npcs"]) == 27

forbidden = [
    r"\bdefeat\b", r"\bscripted (?:battle|boss)\b", r"\bboss phase\b",
    r"\b(?:win|finish) (?:the|this) battle\b",
    r"\b(?:minecraft|rct|mod) (?:combat|battle|system)\b",
    r"\b(?:quest|story) (?:state|flag|system|dialogue|journal)\b",
    r"\bexisting (?:starter|registration|gym|league|field kit) (?:service|system)\b",
    r"\b(?:this|the) dialogue (?:does|cannot|doesn't)\b",
    r"\bcontinue the pride region story\b",
    r"\btrainer is already in a battle\b",
    r"\b(?:mob waves?|minecraft damage|melee attack)\b",
]
patterns = [re.compile(s, re.IGNORECASE) for s in forbidden]
texts = []
for npc in story["npcs"]:
    for node in npc["nodes"]:
        texts.append((f"{npc['id']}.{node['id']}", node["text"]))
        for option in node["options"]:
            texts.append((f"{npc['id']}.{node['id']}.{option['id']}", option["text"]))
for entry in rosters["encounters"]:
    texts.append((entry["id"] + ".intro", entry["intro"]))
    texts.append((entry["id"] + ".win", entry["win"]))

java = ROOT / "src/main/java/com/prideregion/core/story"
objective = (java / "StoryObjectiveGuide.java").read_text(encoding="utf-8")
for name, line in re.findall(r'Map.entry\("(pride:[\w]+)", "([^"]+)"\)', objective):
    texts.append((f"objective.{name}", line))
finale = (java / "StoryFinaleObjectiveGuide.java").read_text(encoding="utf-8")
for line in re.findall(r'case (?:LOCKED|CONDUITS|SHIELDS|STABILIZE|COMPLETE) -> "([^"]+)"', finale):
    texts.append(("finale", line))
cinematics = (java / "StoryCinematicTimeline.java").read_text(encoding="utf-8")
for line in re.findall(r'new Cue\(\d+, "([^"]+)"', cinematics):
    texts.append(("cinematic", line))

for location, text in texts:
    assert text.strip(), f"Blank authored story text: {location}"
    for regex in patterns:
        assert not regex.search(text), (
            f"In-world story sounds like combat instructions at {location}: {text}"
        )
manager = (java / "StoryConfigManager.java").read_text(encoding="utf-8")
assert "legacyStockDialogue()" in manager
assert 'existing.get("text").equals(original.get("text"))' in manager
assert '"/story/legacy_stock_dialogue_v1.json"' in manager
print(f"PASS: {len(texts)} narrative lines and preserved 27-NPC progression graph "
      "contain no combat tutorials, game mechanics, or stock-dialogue overwrite")
