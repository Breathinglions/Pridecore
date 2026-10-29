#!/usr/bin/env python3
"""Source guardrails for physical Cobblemon story NPC interaction.

Standalone Java tests prove selection/anchor logic. These source checks ensure
the Fabric adapter keeps those safeguards wired to actual entity interactions.
"""
from pathlib import Path
root = Path(__file__).resolve().parents[1]
src = root / "src/main/java/com/prideregion/core/story"
module = (src / "StoryModule.java").read_text(encoding="utf-8")
engine = (src / "StoryEngine.java").read_text(encoding="utf-8")
resolver = (src / "StoryNpcDialogueResolver.java").read_text(encoding="utf-8")
anchor = (src / "StoryDialogueAnchorLedger.java").read_text(encoding="utf-8")
for required in [
    'UseEntityCallback.EVENT.register',
    '"cobblemon:npc".equals(Registries.ENTITY_TYPE.getId(entity.getType()).toString())',
    'StoryNpcDialogueResolver.reservedNativeService(nativeTags.getFirst())',
    'running.beginBoundNpc(sp.getUuid(), nativeTags.getFirst())',
    'DIALOGUE_ANCHORS.bind(',
    'StoryDialogueAnchorLedger.permits(',
    'DIALOGUE_ANCHORS.clear(player.getUuid());',
    'DIALOGUE_ANCHORS.clearAll();',
    'new ClickEvent(ClickEvent.Action.RUN_COMMAND, command)',
    'String command = "/pridecore story respond "',
    'LongArgumentType.getLong(ctx, "revision")',
    'active.chooseDisplayed(player.getUuid(), expectedNpc, expectedNode,',
    'new HoverEvent(HoverEvent.Action.SHOW_TEXT,',
]:
    assert required in module, f"Unwired physical NPC feature: {required}"
assert 'Choose: /pridecore' not in module
assert '"Conversation finished. Your progress was saved."' not in engine
assert 'before.revision() != session.profileRevision()' in engine
assert 'session.nodeId().equals(nodeId)' in engine
assert 'session.npcId().equals(npcId)' in engine
for service in [
    "pride_npc_professor_sequoia", "pride_npc_technician_theo",
    "pride_npc_avery", "pride_npc_thorn",
]:
    assert f'"{service}"' in resolver, f"Native service hijack risk: {service}"
assert '"pride_npc_rin", List.of("rin_summit", "rin_canyon", "rin")' in resolver
assert '"pride_npc_ranger_rowan", List.of("ranger_epilogue", "ranger_rowan")' in resolver
assert 'MAX_DISTANCE_SQUARED = 36.0D' in anchor
assert 'bound.entity().equals(entity)' in anchor
assert 'bound.dimension().equals(dimension)' in anchor
print("PASS: native Cobblemon story dialogue, clickable replies and service isolation remain wired")
