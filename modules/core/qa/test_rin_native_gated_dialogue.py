#!/usr/bin/env python3
"""Check actual generated native Cobblemon Rin overlay has no future spoiler choices."""
import importlib.util
import json
from pathlib import Path
import tempfile
from zipfile import ZipFile

ROOT=Path(__file__).resolve().parents[1]
generator=ROOT/"campaign"/"build_native_overlay.py"
spec=importlib.util.spec_from_file_location("native_overlay_rin",generator)
module=importlib.util.module_from_spec(spec)
spec.loader.exec_module(module)

with tempfile.TemporaryDirectory() as temp:
    import subprocess,sys
    archive=Path(temp)/"native.zip"
    subprocess.run([sys.executable,str(generator),"--campaign",
        str(ROOT/"campaign"/"acts.json"),"--out",str(archive)],check=True)
    with ZipFile(archive) as z:
        rin=json.loads(z.read("data/pride_story/dialogues/rin.json"))
        npc=json.loads(z.read("data/pride_story/npcs/rin.json"))
        for story_npc in ("mom", "ranger", "rin"):
            spawn = z.read(f"data/pride_story/function/spawn/{story_npc}.mcfunction").decode("utf8")
            # Every post-spawn selector must require the actual native class.
            assert spawn.count(f'nbt={{NPCClass:"pride_story:{story_npc}"}}') == 3
            assert f'{{npc:"pride_story:{story_npc}"}}' in spawn
        assert npc["interaction"]["dialogue"]=="pride_story:rin"
        original=json.dumps(rin)
        assert "pridecore story rin open" in original
        assert "pridecore story native rin_canyon" not in original
        assert "pridecore story native rin_summit" not in original
        assert "evidence against Eclipse" not in original
        assert "summit" not in original.lower()
        base_buttons=rin["pages"][0]["input"]["options"]
        assert len(base_buttons)==2
        assert base_buttons[0]["value"]=="talk"
        assert "pridecore story rin open" in str(base_buttons[0]["action"])
        for chapter in ("rin_route1","rin_canyon","rin_summit"):
            screen=json.loads(z.read(f"data/pride_story/dialogues/{chapter}.json"))
            face=screen["speakers"]["npc"]["face"]
            assert face["modelType"]=="npc"
            assert face["identifier"]=="pride_skins:rin", face
            buttons=screen["pages"][0]["input"]["options"]
            assert [x["value"] for x in buttons]==["challenge","bye"]
            assert "pridecore story rin challenge" in str(buttons[0]["action"])
            for other in {"rin_route1","rin_canyon","rin_summit"}-{chapter}:
                # In a page's action never pass a client-chosen encounter ID.
                assert f"pridecore story rin challenge {other}" not in str(screen)
        replay=json.loads(z.read("data/pride_story/dialogues/rin_return.json"))
        assert replay["speakers"]["npc"]["face"]["identifier"]=="pride_skins:rin"
        assert [x["value"] for x in replay["pages"][0]["input"]["options"]]==["bye"]
        for item in z.namelist():
            if "dialogues/" in item and item.endswith(".json"):
                json.loads(z.read(item))

source=(ROOT/"src/main/java/com/prideregion/core/story/StoryModule.java").read_text()
assert "RinChapterGate.choose(" in source
assert "launchRctStoryBattle(source.getServer(), player," in source
assert "RinDialogueTicket" in source
# Recomputed encounter is authoritative; no temporary ticket required.
assert "String encounter = scene.encounter();" in source
assert 'CommandManager.literal("rin")' in source
assert "getCommandManager().executeWithPrefix" in source
assert "StoryLeagueBadgeGate.allows(roster.minBadges()" in source
assert "pride_npc_rin" in source
assert "RIN_DIALOGUES.remove(player.getUuid())" in source
# An expired or dropped second-page native GUI ticket must not block an
# otherwise authorized physical Rin battle, but story+badge checks remain.
challenge=source.split("private static int challengeRin(",1)[1].split(
    "private static int nativeChoice(",1)[0]
assert "RinChapterGate.choose(" in challenge
assert "StoryLeagueBadgeGate.allows(roster.minBadges()" in challenge
assert "nearbyRin(player).orElseThrow" in challenge
assert "launchRctStoryBattle(source.getServer(), player," in challenge
assert "if (ticket == null" not in challenge
assert "if (ticket != null" in challenge
# A 45-second lease expired while real users read Rin's native dialogue.
# Keep a real bound lifetime of at least five minutes, with all original
# server-side chapter, NPC identity, distance and badge rechecks intact.
import re
lease=re.search(r"RIN_DIALOGUE_LIFETIME_TICKS\s*=\s*20\s*\*\s*60\s*\*\s*(\d+)",source)
assert lease is not None and int(lease.group(1)) >= 5
assert "int remaining = RIN_DIALOGUE_LIFETIME_TICKS" in source
assert "--ticket.remaining <= 0" in source
# Recomputed encounter is authoritative; no temporary ticket required.
assert "String encounter = scene.encounter();" in source
assert "RinChapterGate.choose(profile.facts(), player.getCommandTags())" in source
# Route 1 intro and defeat lines are Rin's speech, not quest objectives.
assert 'return encounterId != null && encounterId.startsWith("rin_")' in source
assert '? "Rin: " : "[Pride Region] "' in source
assert 'Text.literal(battleDialoguePrefix(encounterId) + roster.intro())' in source
assert 'Text.literal(battleDialoguePrefix(resolvedEncounter) + victoryLine)' in source
assert 'Text.literal("[Pride Region] Next objective: ")' in source
# Canyon victory must not strand players after the native chapter selector
# moves them into Rin's return dialogue. Only the server-verified win fact can
# complete the story checkpoint; reconnect/sync must repair saved wins.
engine = (ROOT / "src/main/java/com/prideregion/core/story/StoryEngine.java").read_text()
assert 'boolean reconcileRinCanyonVictory(UUID playerId)' in engine
assert '"pride:rin_canyon_won"' in engine.split(
    'boolean reconcileRinCanyonVictory(UUID playerId)',1)[1].split(
    'public synchronized void cancel(',1)[0]
assert 'progression.transition(playerId, "pride:rival_debate",' in engine
assert 'Set.of("pride:rival_theory"), Set.of()).changed()' in engine
assert '"rin_canyon".equals(resolvedEncounter)' in source
assert 'engine.reconcileRinCanyonVictory(player.getUuid())' in source
assert 'if (canyonWin || summitWin)' in source
assert 'boolean canyonWin = active.reconcileRinCanyonVictory(' in source
# The third Rin battle uses the same canonical physical NPC and trusted
# Cobblemon victory callback. A saved Summit win must complete the chapter,
# including after disconnect/rejoin, without needing inaccessible old dialogue.
assert 'boolean reconcileRinSummitVictory(UUID playerId)' in engine
summit_reconcile = engine.split('boolean reconcileRinSummitVictory(UUID playerId)',1)[1].split(
    'public synchronized boolean reconcileEmberfallVictory(',1)[0]
for expected in ('"pride:ice_research"', '"pride:frozen_warning"',
                 '"pride:clue_ice"', '"pride:rin_summit_won"',
                 '"pride:rin_canyon_won"', '"pride:rival_reunited"',
                 '"pride:rival_ally"'):
    assert expected in summit_reconcile, f"Summit story handoff missing {expected}"
assert '"rin_summit".equals(resolvedEncounter)' in source
assert 'engine.reconcileRinSummitVictory(player.getUuid())' in source
assert 'boolean summitWin = active.reconcileRinSummitVictory(' in source
assert 'if (canyonWin || summitWin)' in source
print("PASS: Rin battle dialogue attributed to Rin; next objectives remain system messages")
print("PASS: one Rin, three isolated Cobblemon chapter screens, server-gated battle handoff")
