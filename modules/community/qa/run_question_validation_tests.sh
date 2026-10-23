#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
TEMP="$(mktemp -d)"
trap 'rm -rf "$TEMP"' EXIT
javac -encoding UTF-8 -d "$TEMP" \
  "$ROOT/src/main/java/com/prideregion/community/QuizEngine.java" \
  "$ROOT/src/main/java/com/prideregion/community/QuizQuestionValidator.java" \
  "$ROOT/src/test/java/com/prideregion/community/QuizQuestionValidatorTest.java"
java -cp "$TEMP" com.prideregion.community.QuizQuestionValidatorTest
