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
 "$ROOT/src/main/java/com/prideregion/core/story/StoryDefinition.java" \
 "$ROOT/src/main/java/com/prideregion/core/story/StoryEngine.java" \
 "$ROOT/src/main/java/com/prideregion/core/story/StoryEvidenceEngine.java" \
 "$ROOT/src/main/java/com/prideregion/core/story/StoryBattleProof.java" \
 "$ROOT/src/main/java/com/prideregion/core/story/StoryBattleSessionRouter.java" \
 "$ROOT/src/main/java/com/prideregion/core/story/StoryBattleLaunchLedger.java" \
 "$ROOT/src/main/java/com/prideregion/core/story/ResonanceFinaleController.java" \
 "$ROOT/src/main/java/com/prideregion/core/story/StoryPlaythroughReset.java" \
 "$ROOT/src/main/java/com/prideregion/core/story/ResonanceFinaleCompletion.java" \
 "$ROOT/src/main/java/com/prideregion/core/story/ResonanceCheckpointStore.java" \
 "$ROOT/src/main/java/com/prideregion/core/story/StoryObjectiveGuide.java" \
 "$ROOT/src/test/java/com/prideregion/core/story/StoryEngineTests.java" \
 "$ROOT/src/test/java/com/prideregion/core/story/StoryEvidenceEngineTests.java" \
 "$ROOT/src/test/java/com/prideregion/core/story/StoryBattleProofTests.java" \
 "$ROOT/src/test/java/com/prideregion/core/story/StoryBattleSessionRouterTests.java" \
 "$ROOT/src/test/java/com/prideregion/core/story/StoryBattleLaunchLedgerTests.java" \
 "$ROOT/src/test/java/com/prideregion/core/story/ResonanceFinaleControllerTests.java" \
 "$ROOT/src/test/java/com/prideregion/core/story/StoryPlaythroughResetTests.java" \
 "$ROOT/src/test/java/com/prideregion/core/story/ResonanceFinaleCompletionTests.java" \
 "$ROOT/src/test/java/com/prideregion/core/story/ResonanceCheckpointStoreTests.java"
java -cp "$TMP" com.prideregion.core.story.StoryEngineTests
java -cp "$TMP" com.prideregion.core.story.StoryEvidenceEngineTests
java -cp "$TMP" com.prideregion.core.story.StoryBattleProofTests
java -cp "$TMP" com.prideregion.core.story.StoryBattleSessionRouterTests
java -cp "$TMP" com.prideregion.core.story.StoryBattleLaunchLedgerTests
java -cp "$TMP" com.prideregion.core.story.ResonanceFinaleControllerTests
java -cp "$TMP" com.prideregion.core.story.StoryPlaythroughResetTests
java -cp "$TMP" com.prideregion.core.story.ResonanceFinaleCompletionTests
java -cp "$TMP" com.prideregion.core.story.ResonanceCheckpointStoreTests
python3 - "$ROOT/src/main/resources/story/default.json" <<'PY'
import json,sys
d=json.load(open(sys.argv[1]))
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
