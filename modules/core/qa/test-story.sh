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
 "$ROOT/src/main/java/com/prideregion/core/story/ResonanceFinaleController.java" \
 "$ROOT/src/main/java/com/prideregion/core/story/StoryPlaythroughReset.java" \
 "$ROOT/src/main/java/com/prideregion/core/story/ResonanceFinaleCompletion.java" \
 "$ROOT/src/main/java/com/prideregion/core/story/ResonanceCheckpointStore.java" \
 "$ROOT/src/main/java/com/prideregion/core/story/StoryObjectiveGuide.java" \
 "$ROOT/src/test/java/com/prideregion/core/story/StoryEngineTests.java" \
 "$ROOT/src/test/java/com/prideregion/core/story/StoryEvidenceEngineTests.java" \
 "$ROOT/src/test/java/com/prideregion/core/story/StoryBattleProofTests.java" \
 "$ROOT/src/test/java/com/prideregion/core/story/StoryBattleSessionRouterTests.java" \
 "$ROOT/src/test/java/com/prideregion/core/story/ResonanceFinaleControllerTests.java" \
 "$ROOT/src/test/java/com/prideregion/core/story/StoryPlaythroughResetTests.java" \
 "$ROOT/src/test/java/com/prideregion/core/story/ResonanceFinaleCompletionTests.java" \
 "$ROOT/src/test/java/com/prideregion/core/story/ResonanceCheckpointStoreTests.java"
java -cp "$TMP" com.prideregion.core.story.StoryEngineTests
java -cp "$TMP" com.prideregion.core.story.StoryEvidenceEngineTests
java -cp "$TMP" com.prideregion.core.story.StoryBattleProofTests
java -cp "$TMP" com.prideregion.core.story.StoryBattleSessionRouterTests
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
assert [n["id"] for n in d["npcs"][:6]] == ["mom","sequoia","theo","avery","ranger","thorn"]
for npc in d["npcs"]:
 assert npc["completedFact"].startswith("pride:")
 assert all(x["id"] for x in npc["nodes"])
print("PASS: bundled story JSON validated")
PY
