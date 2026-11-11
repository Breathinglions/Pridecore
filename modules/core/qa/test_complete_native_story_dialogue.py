#!/usr/bin/env python3
"""Contract test for ALL authored Cobblemon storyline choices (not just Rin)."""
import json
import subprocess
import sys
import tempfile
from pathlib import Path
from zipfile import ZipFile

ROOT = Path(__file__).resolve().parents[1]
story = json.loads((ROOT/"src/main/resources/story/default.json").read_text("utf8"))
by_id = {n["id"]: n for n in story["npcs"]}
assert len(by_id) == 27, "The canonical NPC graph changed"

with tempfile.TemporaryDirectory(prefix="pride-native-audit-") as temp:
    out = Path(temp)/"native.zip"
    subprocess.run([
        sys.executable, str(ROOT/"campaign/build_native_overlay.py"),
        "--campaign", str(ROOT/"campaign/acts.json"),
        "--out", str(out)], check=True)
    with ZipFile(out) as archive:
        assert archive.testzip() is None
        actors = sorted(
            p.removeprefix("data/pride_story/npcs/").removesuffix(".json")
            for p in archive.namelist()
            if p.startswith("data/pride_story/npcs/") and p.endswith(".json"))
        assert len(actors) == 20
        assert "ranger_epilogue" not in actors
        assert "rin_canyon" not in actors and "rin_summit" not in actors
        assert "thorn" not in actors

        checked=0
        for actor in actors:
            native=json.loads(archive.read(f"data/pride_story/dialogues/{actor}.json"))
            buttons=native["pages"][0]["input"]["options"]
            # Mom, Rin and Rowan intentionally use their own player-specific
            # entry interactions. They must not expose future chapters.
            if actor in {"mom", "rin", "ranger_rowan",
                         "eclipse_operative", "sable", "director_cael"}:
                assert "Continue our journey." not in json.dumps(native)
                continue
            definition=by_id[actor]
            main=next(x for x in definition["nodes"] if x["id"]==definition["start"])
            choice_id="explore" if actor=="ranger" else "continue"
            action=next(x for x in main["options"] if x["id"]==choice_id)
            assert buttons[0]["text"]==action["text"], actor
            assert f"pridecore story native {actor} {choice_id}" in str(buttons[0]["action"]),actor
            assert native["pages"][0]["lines"]==[main["text"]],actor
            assert "Continue our journey." not in json.dumps(native),actor
            extra=[x for x in main["options"] if x["id"]=="learn_more"]
            if extra:
                assert len(extra)==1 and len(native["pages"])==2,actor
                detail=next(x for x in definition["nodes"] if x["id"]==extra[0]["next"])
                assert buttons[1]["text"]==extra[0]["text"],actor
                assert native["pages"][1]["lines"]==[detail["text"]],actor
                assert "pridecore story" not in str(native["pages"][1]["input"]["options"]),actor
            else:
                assert len(native["pages"])==1,actor
            checked+=1

        # One physical boss NPC: the opening page is neutral and exposes no
        # alternate encounter ID or post-battle story grant. The server chooses
        # which PRIVATE phase screen this exact player is authorized to see.
        for npc, battle_id in (
                ("eclipse_operative", "eclipse_lieutenant"),
                ("sable", "sable"),
                ("director_cael", "director_cael")):
            opening=json.loads(archive.read(
                f"data/pride_story/dialogues/{npc}.json"))
            assert [o["value"] for o in opening["pages"][0]["input"]["options"]]==["talk","bye"]
            assert "pridecore story boss open" in json.dumps(opening)
            assert f"pridecore story native {npc} continue" not in json.dumps(opening)
            intro=json.loads(archive.read(
                f"data/pride_story/dialogues/{npc}_challenge.json"))
            intro_text=json.dumps(intro)
            assert "pridecore story boss challenge" in intro_text
            assert f"pridecore story native {npc} continue" not in intro_text
            assert [o["value"] for o in intro["pages"][0]["input"]["options"]]==["challenge","bye"]
            after=json.loads(archive.read(
                f"data/pride_story/dialogues/{npc}_aftermath.json"))
            canonical=by_id[npc]
            main=next(x for x in canonical["nodes"]
                      if x["id"]==canonical["start"])
            choice=next(x for x in main["options"]
                        if x["id"]=="continue")
            assert after["pages"][0]["lines"]==[main["text"]]
            assert after["pages"][0]["input"]["options"][0]["text"]==choice["text"]
            assert f"pridecore story native {npc} continue" in json.dumps(after)
            assert len(after["pages"])==2
            repeat=json.loads(archive.read(
                f"data/pride_story/dialogues/{npc}_return.json"))
            assert "pridecore story native" not in json.dumps(repeat)
            locked=json.loads(archive.read(
                f"data/pride_story/dialogues/{npc}_locked.json"))
            assert "pridecore story boss challenge" not in json.dumps(locked)
            assert "pridecore story native" not in json.dumps(locked)
        rowan=json.loads(archive.read("data/pride_story/dialogues/ranger_rowan.json"))
        rowan_text=json.dumps(rowan)
        assert "pridecore story rowan open" in rowan_text
        assert "ranger_epilogue" not in rowan_text
        assert "Resonance Engine" not in rowan_text
        assert [x["value"] for x in rowan["pages"][0]["input"]["options"]]==["talk","bye"]
        for chapter,role in [("ranger_rowan_case","ranger_rowan"),
                             ("ranger_rowan_finale","ranger_epilogue")]:
            native=json.loads(archive.read(f"data/pride_story/dialogues/{chapter}.json"))
            canonical=by_id[role]
            main=next(x for x in canonical["nodes"] if x["id"]==canonical["start"])
            action=next(x for x in main["options"] if x["id"]=="continue")
            assert native["pages"][0]["lines"]==[main["text"]]
            assert native["pages"][0]["input"]["options"][0]["text"]==action["text"]
            assert f"pridecore story native {role} continue" in str(native)
        return_page=json.loads(archive.read("data/pride_story/dialogues/ranger_rowan_return.json"))
        assert "pridecore story native" not in json.dumps(return_page)
        assert [x["value"] for x in return_page["pages"][0]["input"]["options"]]==["bye"]

module=(ROOT/"src/main/java/com/prideregion/core/story/StoryModule.java").read_text("utf8")
for fragment in [
    'CommandManager.literal("rowan")',
    'private static int openRowanChapter(',
    'private static boolean rowanNpcNearby(',
    'StoryWorldMarkerCatalog.exclusiveStoryMarker(',
    'NativeNpcIdentity.matchesRequiredClass(',
    'rowanScene(p.facts())',
    'if (rowanScene(p.facts()) != ticket.scene) continue;',
    '"opendialogue " + ticket.scene.dialogue()',
    'ROWAN_DIALOGUES.remove(handler.getPlayer().getUuid())',
    'ROWAN_DIALOGUES.clear();',
]:
    assert fragment in module, f"Rowan native chapter server gate missing: {fragment}"

for fragment in [
    'CommandManager.literal("boss")',
    'private static int openStoryBoss(',
    'private static int challengeStoryBoss(',
    'bossScene(ticket.role, profile, player.getCommandTags())',
    'ticket.scene != BossScene.CHALLENGE',
    'ticket.npc',
    'bossNpcNearby(viewer, ticket.role, ticket.npc)',
    'StoryTrainerBindingGate.permitsCanonicalBoss(',
    'bossRole.npcClassId().equals(npcClass(trainer))',
    'launchRctStoryBattle(source.getServer(), player,',
    'actor.npc(), ticket.role.encounter, false, true',
    'BOSS_DIALOGUES.remove(handler.getPlayer().getUuid())',
    'BOSS_DIALOGUES.clear();',
]:
    assert fragment in module, f"Single-actor Eclipse boss protection missing: {fragment}"
print(f"PASS: {checked} authored NPC conversations, Rowan routing and all three single-actor Eclipse bosses")
