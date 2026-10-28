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
 "$ROOT/src/main/java/com/prideregion/core/story/StoryObjectiveGuide.java" \
 "$ROOT/src/test/java/com/prideregion/core/story/StoryEngineTests.java" \
 "$ROOT/src/test/java/com/prideregion/core/story/StoryEvidenceEngineTests.java"
java -cp "$TMP" com.prideregion.core.story.StoryEngineTests
java -cp "$TMP" com.prideregion.core.story.StoryEvidenceEngineTests
python3 - "$ROOT/src/main/resources/story/default.json" <<'PY'
import json,sys
d=json.load(open(sys.argv[1]))
assert d["version"]==1
assert len(d["npcs"])==27
assert [n["id"] for n in d["npcs"][:6]] == ["mom","sequoia","theo","avery","ranger","thorn"]
for npc in d["npcs"]:
 assert npc["completedFact"].startswith("pride:")
 assert all(x["id"] for x in npc["nodes"])
print("PASS: bundled story JSON validated")
PY
