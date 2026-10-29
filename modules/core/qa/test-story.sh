#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT
javac --release 21 -d "$TMP" \
 "$ROOT/src/main/java/com/prideregion/core/ProfileState.java" \
 "$ROOT/src/main/java/com/prideregion/core/ProfileStore.java" \
 "$ROOT/src/main/java/com/prideregion/core/LegacyTagView.java" \
 "$ROOT/src/main/java/com/prideregion/core/ProgressionView.java" \
 "$ROOT/src/main/java/com/prideregion/core/ProgressionService.java" \
 "$ROOT/src/main/java/com/prideregion/core/story/LegacyOnboardingGate.java" \
 "$ROOT/src/main/java/com/prideregion/core/story/NativeNpcIdentity.java" \
 "$ROOT/src/main/java/com/prideregion/core/story/StoryNpcDialogueResolver.java" \
 "$ROOT/src/main/java/com/prideregion/core/story/StoryDialogueAnchorLedger.java" \
 "$ROOT/src/main/java/com/prideregion/core/story/StoryPrivateSceneDirector.java" \
 "$ROOT/src/main/java/com/prideregion/core/story/StoryPrivateSceneEligibility.java" \
 "$ROOT/src/main/java/com/prideregion/core/story/StoryDefinition.java" \
 "$ROOT/src/main/java/com/prideregion/core/story/StoryEngine.java" \
 "$ROOT/src/main/java/com/prideregion/core/story/StoryEvidenceEngine.java" \
 "$ROOT/src/main/java/com/prideregion/core/story/StoryCombatPolicy.java" \
 "$ROOT/src/main/java/com/prideregion/core/story/StoryPokemonOnlyParties.java" \
 "$ROOT/src/main/java/com/prideregion/core/story/StoryBattleProof.java" \
 "$ROOT/src/main/java/com/prideregion/core/story/StoryBattleSessionRouter.java" \
 "$ROOT/src/main/java/com/prideregion/core/story/StoryBattleLaunchLedger.java" \
 "$ROOT/src/main/java/com/prideregion/core/story/StoryWorldMarkerCatalog.java" \
 "$ROOT/src/main/java/com/prideregion/core/story/StoryArenaReadiness.java" \
 "$ROOT/src/main/java/com/prideregion/core/story/StoryZoneReadiness.java" \
 "$ROOT/src/main/java/com/prideregion/core/story/StoryTrainerBindingGate.java" \
 "$ROOT/src/main/java/com/prideregion/core/story/StoryFinaleObjectiveGuide.java" \
 "$ROOT/src/main/java/com/prideregion/core/story/StoryLeagueBadgeGate.java" \
 "$ROOT/src/main/java/com/prideregion/core/story/StoryDimensionGate.java" \
 "$ROOT/src/main/java/com/prideregion/core/story/StoryCinematicTimeline.java" \
 "$ROOT/src/main/java/com/prideregion/core/story/StoryEncounterAvailability.java" \
 "$ROOT/src/main/java/com/prideregion/core/story/StoryBattlePhaseTracker.java" \
 "$ROOT/src/main/java/com/prideregion/core/story/StoryBattleResultRetryQueue.java" \
 "$ROOT/src/main/java/com/prideregion/core/story/ResonanceFinaleController.java" \
 "$ROOT/src/main/java/com/prideregion/core/story/StoryPlaythroughReset.java" \
 "$ROOT/src/main/java/com/prideregion/core/story/ResonanceFinaleCompletion.java" \
 "$ROOT/src/main/java/com/prideregion/core/story/ResonanceCheckpointStore.java" \
 "$ROOT/src/main/java/com/prideregion/core/story/StoryObjectiveGuide.java" \
 "$ROOT/src/test/java/com/prideregion/core/story/StoryEngineTests.java" \
 "$ROOT/src/test/java/com/prideregion/core/story/StoryNpcDialogueResolverTests.java" \
 "$ROOT/src/test/java/com/prideregion/core/story/StoryDialogueAnchorLedgerTests.java" \
 "$ROOT/src/test/java/com/prideregion/core/story/StoryPrivateSceneDirectorTests.java" \
 "$ROOT/src/test/java/com/prideregion/core/story/StoryPrivateSceneEligibilityTests.java" \
 "$ROOT/src/test/java/com/prideregion/core/story/StoryEvidenceEngineTests.java" \
 "$ROOT/src/test/java/com/prideregion/core/story/StoryCombatPolicyTests.java" \
 "$ROOT/src/test/java/com/prideregion/core/story/StoryPokemonOnlyPartiesTests.java" \
 "$ROOT/src/test/java/com/prideregion/core/story/StoryBattleProofTests.java" \
 "$ROOT/src/test/java/com/prideregion/core/story/StoryBattleSessionRouterTests.java" \
 "$ROOT/src/test/java/com/prideregion/core/story/StoryBattleLaunchLedgerTests.java" \
 "$ROOT/src/test/java/com/prideregion/core/story/StoryWorldMarkerCatalogTests.java" \
 "$ROOT/src/test/java/com/prideregion/core/story/StoryArenaReadinessTests.java" \
 "$ROOT/src/test/java/com/prideregion/core/story/StoryZoneReadinessTests.java" \
 "$ROOT/src/test/java/com/prideregion/core/story/StoryTrainerBindingGateTests.java" \
 "$ROOT/src/test/java/com/prideregion/core/story/StoryFinaleObjectiveGuideTests.java" \
 "$ROOT/src/test/java/com/prideregion/core/story/StoryLeagueBadgeGateTests.java" \
 "$ROOT/src/test/java/com/prideregion/core/story/StoryDimensionGateTests.java" \
 "$ROOT/src/test/java/com/prideregion/core/story/StoryCinematicTimelineTests.java" \
 "$ROOT/src/test/java/com/prideregion/core/story/StoryEncounterAvailabilityTests.java" \
 "$ROOT/src/test/java/com/prideregion/core/story/StoryBattlePhaseTrackerTests.java" \
 "$ROOT/src/test/java/com/prideregion/core/story/StoryBattleResultRetryQueueTests.java" \
 "$ROOT/src/test/java/com/prideregion/core/story/StoryCampaignEndToEndTests.java" \
 "$ROOT/src/test/java/com/prideregion/core/story/StoryMultiplayerIsolationTests.java" \
 "$ROOT/src/test/java/com/prideregion/core/story/StoryBattlePersistenceRecoveryTests.java" \
 "$ROOT/src/test/java/com/prideregion/core/story/ResonanceFinaleControllerTests.java" \
 "$ROOT/src/test/java/com/prideregion/core/story/StoryPlaythroughResetTests.java" \
 "$ROOT/src/test/java/com/prideregion/core/story/ResonanceFinaleCompletionTests.java" \
 "$ROOT/src/test/java/com/prideregion/core/story/ResonanceCheckpointStoreTests.java"
java -cp "$TMP" com.prideregion.core.story.StoryEngineTests
java -cp "$TMP" com.prideregion.core.story.StoryNpcDialogueResolverTests
java -cp "$TMP" com.prideregion.core.story.StoryDialogueAnchorLedgerTests
java -cp "$TMP" com.prideregion.core.story.StoryPrivateSceneDirectorTests
java -cp "$TMP" com.prideregion.core.story.StoryPrivateSceneEligibilityTests
java -cp "$TMP" com.prideregion.core.story.StoryEvidenceEngineTests
java -cp "$TMP" com.prideregion.core.story.StoryCombatPolicyTests
java -cp "$TMP" com.prideregion.core.story.StoryPokemonOnlyPartiesTests
java -cp "$TMP" com.prideregion.core.story.StoryBattleProofTests
java -cp "$TMP" com.prideregion.core.story.StoryBattleSessionRouterTests
java -cp "$TMP" com.prideregion.core.story.StoryBattleLaunchLedgerTests
java -cp "$TMP" com.prideregion.core.story.StoryWorldMarkerCatalogTests
java -cp "$TMP" com.prideregion.core.story.StoryArenaReadinessTests
java -cp "$TMP" com.prideregion.core.story.StoryZoneReadinessTests
java -cp "$TMP" com.prideregion.core.story.StoryTrainerBindingGateTests
java -cp "$TMP" com.prideregion.core.story.StoryFinaleObjectiveGuideTests
java -cp "$TMP" com.prideregion.core.story.StoryLeagueBadgeGateTests
java -cp "$TMP" com.prideregion.core.story.StoryDimensionGateTests
java -cp "$TMP" com.prideregion.core.story.StoryCinematicTimelineTests
java -cp "$TMP" com.prideregion.core.story.StoryEncounterAvailabilityTests
java -cp "$TMP" com.prideregion.core.story.StoryBattlePhaseTrackerTests
java -cp "$TMP" com.prideregion.core.story.StoryBattleResultRetryQueueTests
java -cp "$TMP" com.prideregion.core.story.StoryCampaignEndToEndTests
java -cp "$TMP" com.prideregion.core.story.StoryMultiplayerIsolationTests
java -cp "$TMP" com.prideregion.core.story.StoryBattlePersistenceRecoveryTests
java -cp "$TMP" com.prideregion.core.story.ResonanceFinaleControllerTests
java -cp "$TMP" com.prideregion.core.story.StoryPlaythroughResetTests
java -cp "$TMP" com.prideregion.core.story.ResonanceFinaleCompletionTests
java -cp "$TMP" com.prideregion.core.story.ResonanceCheckpointStoreTests
python3 - "$ROOT/src/main/resources/story/default.json" "$ROOT/src/main/java/com/prideregion/core/story/NativeNpcIdentity.java" <<'PY'
import json,sys,re,pathlib
d=json.load(open(sys.argv[1]))
java=pathlib.Path(sys.argv[2]).read_text()
allowed=java.split("ALLOWED = Set.of(",1)[1].split(");",1)[0]
npc_ids=set(re.findall(r'"([a-z0-9_]+)"',allowed))
assert npc_ids == {npc["id"] for npc in d["npcs"]}, "Physical NPC binding allowlist must match authored story JSON"
assert d["version"]==1
assert len(d["npcs"])==27
league=next(n for n in d["npcs"] if n["id"]=="league_guide")
assert "pride:finale_completed" in league["requires"], "Victory Road cannot skip the engine finale"
ranger=next(n for n in d["npcs"] if n["id"]=="ranger")
assert "pride:rin_route1_won" in ranger["requires"], "Route 1 rival cannot be skipped"
for npc_id, required_battle in {
 "eclipse_operative": "pride:eclipse_lieutenant_won",
 "heat_technician": "pride:rin_canyon_won",
 "ghost_medium": "pride:rin_summit_won",
 "director_cael": "pride:sable_battle_won",
 "ranger_epilogue": "pride:cael_battle_won",
}.items():
 target=next(n for n in d["npcs"] if n["id"]==npc_id)
 assert required_battle in target["requires"], f"{npc_id} bypasses {required_battle}"
assert [n["id"] for n in d["npcs"][:6]] == ["mom","sequoia","theo","avery","ranger","thorn"]
for npc in d["npcs"]:
 assert npc["completedFact"].startswith("pride:")
 assert all(x["id"] for x in npc["nodes"])
# Every major boss victory must block its intended narrative continuation.
gate_map={
 "ranger":"pride:rin_route1_won",
 "heat_technician":"pride:rin_canyon_won",
 "ghost_medium":"pride:rin_summit_won",
 "eclipse_operative":"pride:eclipse_lieutenant_won",
 "director_cael":"pride:sable_battle_won",
 "ranger_epilogue":"pride:cael_battle_won",
 "league_guide":"pride:finale_completed",
}
for npc_id, fact in gate_map.items():
 npc=next(n for n in d["npcs"] if n["id"] == npc_id)
 assert fact in npc["requires"], f"Missing story gate {fact} on {npc_id}"
print("PASS: mandatory chapter battle gates validated")
print("PASS: bundled story JSON validated")
PY
python3 - "$ROOT/src/main/java/com/prideregion/core/story/StoryEffects.java" <<'PY'
import pathlib,sys
effects=pathlib.Path(sys.argv[1]).read_text()
assert "instanceof SimpleParticleType" in effects, "Parameterized mod particles must not be used as generic overrides"
assert "ParticleEffect effect = fallback" in effects, "Vanilla particle fallback must remain available"
assert "catch (RuntimeException presentationFailure)" in effects, "Particle failures must be isolated from gameplay"
assert "world.spawnParticles(fallback" in effects, "Failed modded effects must retry vanilla"
print("PASS: story effect override safeguards")
PY

# Optional staging prop template must never ship with unknown or broad kill selectors.
python3 "$ROOT/qa/test_staging_world.py"
python3 "$ROOT/qa/test_story_combat_boundary.py"
python3 "$ROOT/qa/test_story_narrative.py"
python3 "$ROOT/qa/test_story_npc_interaction.py"
