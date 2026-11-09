#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
mkdir -p "$ROOT/qa/out"
javac -encoding UTF-8 -d "$ROOT/qa/out" \
  "$ROOT/src/main/java/com/prideregion/community/QuizEngine.java" \
  "$ROOT/src/main/java/com/prideregion/community/RewardLedger.java" \
  "$ROOT/src/test/java/com/prideregion/community/QuizEngineTest.java"
java -cp "$ROOT/qa/out" com.prideregion.community.QuizEngineTest
