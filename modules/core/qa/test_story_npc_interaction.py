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
    'return ActionResult.PASS;',
    'DIALOGUE_ANCHORS.clear(sp.getUuid());',
    'private static int nativeChoice(ServerCommandSource source, String npc, String option)',
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
# Real Cobblemon story NPCs MUST never open the old chat transcript.
# The native dialogue resource and original Cobblemon overlay own that path.
native_section = module.split('// Story-only real Cobblemon NPCs already own a full native', 1)[1].split(
    'if (entity.getType() != EntityType.VILLAGER)', 1)[0]
assert 'show(sp, opened.get())' not in native_section
assert 'beginBoundNpc' not in native_section
assert 'return ActionResult.PASS;' in native_section
assert 'running.cancel(sp.getUuid())' in native_section
assert 'StoryNpcDialogueResolver.reservedNativeService' in native_section
native_handler = module.split('private static int nativeChoice(', 1)[1].split(
    'private static int choose(', 1)[0]
assert 'player.getWorld().getOtherEntities' in native_handler
assert 'active.choose(player.getUuid(), option)' in native_handler
assert 'NativeNpcIdentity.tagFor(npc)' in native_handler
generator = (root / 'campaign/build_native_overlay.py').read_text(encoding='utf8')
assert '"interaction":{"type":"dialogue","dialogue":f"pride_story:{dialogue_id}"}' in generator
assert 'q.player.run_command(\'pridecore story native ' in generator
assert 'mom_dialogue() if npc == "mom"' in generator
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
print("PASS: story Cobblemon overlay passthrough, native action verification, and legacy service isolation")
