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
# A visible NPC name and role tag do not prove the saved Cobblemon NPC class.
# Regression: a resource_ranger retagged pride_npc_mom must be refused.
identity=(src / "NativeNpcIdentity.java").read_text(encoding="utf8")
assert 'return "pride_story:" + canonical;' in identity
assert 'public static boolean matchesRequiredClass' in identity
assert 'npc.writeNbt(new NbtCompound()).getString("NPCClass")' in module
assert 'NativeNpcIdentity.matchesRequiredClass(' in module
assert 'Wrong native story NPC class' in module
assert 'Cannot assign " + tag' in module
assert 'INVALID story marker on ' in module
rewards = (root / "src/main/java/com/prideregion/core/OnboardingRewards.java").read_text(encoding="utf8")
assert 'NativeNpcIdentity.matchesRequiredClass(' in rewards, (
    "Theo/Avery kit grants must verify the native Cobblemon NPCClass")
assert '"pride_npc_professor_sequoia", npcClass(e)' in module, (
    "Sequoia's cinematic must not trust a tag on a different NPC")
for expected in (
    'case "pride_npc_professor_sequoia" -> "pride_region:professor_sequoia"',
    'case "pride_npc_technician_theo" -> "pride_region:technician_theo"',
    'case "pride_npc_avery" -> "pride_region:avery"',
):
    assert expected in identity, expected
# The actual amethyst blocks are clickable, but only at a builder-placed,
# authenticated forest marker. Generic amethyst elsewhere stays vanilla.
for required in (
    'UseBlockCallback.EVENT.register',
    'FOREST_CRYSTAL_BLOCKS.contains(world.getBlockState(clicked).getBlock())',
    'new Box(clicked).expand(3.0D)',
    'markerEntity.getType() == EntityType.INTERACTION',
    'markerEntity.getCommandTags().contains("pride_clue_forest")',
    'StoryWorldMarkerCatalog.exclusiveStoryMarker(',
    'return discoverStoryEvidence(sp, "forest");',
    'StoryAdvancementBridge.synchronize(player, engine);',
    'sendNextObjective(player, afterClick);',
):
    assert required in module, f"Unsafe or missing crystal-click quest handoff: {required}"
evidence = (src / "StoryEvidenceEngine.java").read_text(encoding="utf8")
guide = (src / "StoryObjectiveGuide.java").read_text(encoding="utf8")
assert '"pride:forest_investigation".equals(player.storyStage())' in evidence
assert '"pride:forest_traces_discovered"' in evidence and guide
assert 'Set.of(clue.earnedFact()), Set.of()).changed()' in evidence
# Both the first click and an older save with a persisted mine clue must
# produce the proper follow-up, without a fabricated second discovery.
for required in (
    'String stageBeforeClick = engine.currentStage(player.getUuid());',
    'StoryEvidenceEngine.reconcileSavedObjective(',
    '|| !stageBeforeClick.equals(afterClick)',
    'if (verdantWin || stonehavenWin || clueObjective || canyonWin || emberfallWin)',
    'if (!changes.isEmpty() || verdantWin || stonehavenWin || clueObjective || canyonWin || emberfallWin)',
):
    assert required in module, f"Mine evidence repair not wired: {required}"
assert 'case "mine" -> "pride:rock_investigation".equals(player.storyStage())' in evidence
assert 'case "pride:rock_investigation" ->' in evidence and 'player.facts().contains("pride:clue_mine")' in evidence
assert '"pride:mine_evidence_discovered"' in evidence and guide
print("PASS: story Cobblemon overlay passthrough, native action verification, and legacy service isolation")
