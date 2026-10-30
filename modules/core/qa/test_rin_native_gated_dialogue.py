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
            buttons=screen["pages"][0]["input"]["options"]
            assert [x["value"] for x in buttons]==["challenge","bye"]
            assert "pridecore story rin challenge" in str(buttons[0]["action"])
            for other in {"rin_route1","rin_canyon","rin_summit"}-{chapter}:
                # In a page's action never pass a client-chosen encounter ID.
                assert f"pridecore story rin challenge {other}" not in str(screen)
        replay=json.loads(z.read("data/pride_story/dialogues/rin_return.json"))
        assert [x["value"] for x in replay["pages"][0]["input"]["options"]]==["bye"]
        for item in z.namelist():
            if "dialogues/" in item and item.endswith(".json"):
                json.loads(z.read(item))

source=(ROOT/"src/main/java/com/prideregion/core/story/StoryModule.java").read_text()
assert "RinChapterGate.choose(" in source
assert "launchRctStoryBattle(source.getServer(), player," in source
assert "RinDialogueTicket" in source
assert "ticket.scene.encounter()" in source
assert 'CommandManager.literal("rin")' in source
assert "getCommandManager().executeWithPrefix" in source
assert "StoryLeagueBadgeGate.allows(roster.minBadges()" in source
assert "pride_npc_rin" in source
assert "RIN_DIALOGUES.remove(player.getUuid())" in source
# A 45-second lease expired while real users read Rin's native dialogue.
# Keep a real bound lifetime of at least five minutes, with all original
# server-side chapter, NPC identity, distance and badge rechecks intact.
import re
lease=re.search(r"RIN_DIALOGUE_LIFETIME_TICKS\\s*=\\s*20\\s*\\*\\s*60\\s*\\*\\s*(\\d+)",source)
assert lease is not None and int(lease.group(1)) >= 5
assert "int remaining = RIN_DIALOGUE_LIFETIME_TICKS" in source
assert "--ticket.remaining <= 0" in source
assert "rinNpcNearby(player, ticket.npc)" in source
assert "RinChapterGate.choose(profile.facts(), player.getCommandTags())" in source
print("PASS: one Rin, three isolated Cobblemon chapter screens, server-gated battle handoff")
