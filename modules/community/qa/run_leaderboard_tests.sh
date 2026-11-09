#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
OUT="$(mktemp -d)"
trap 'rm -rf "$OUT"' EXIT
javac -encoding UTF-8 -d "$OUT" \
    "$ROOT/src/main/java/com/prideregion/community/QuizEngine.java" \
    "$ROOT/src/main/java/com/prideregion/community/QuizLeaderboard.java" \
    "$ROOT/src/test/java/com/prideregion/community/QuizLeaderboardTest.java"
java -cp "$OUT" com.prideregion.community.QuizLeaderboardTest
