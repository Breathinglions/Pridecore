#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
DIR="$(mktemp -d)"
trap 'rm -rf "$DIR"' EXIT
javac -encoding UTF-8 -d "$DIR" "$ROOT/src/main/java/com/prideregion/community/RewardNotice.java" "$ROOT/src/test/java/com/prideregion/community/RewardNoticeTest.java"
java -cp "$DIR" com.prideregion.community.RewardNoticeTest
