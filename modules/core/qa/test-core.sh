#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT
javac --release 21 -d "$TMP" \
 "$ROOT/src/main/java/com/prideregion/core/ProfileState.java" \
 "$ROOT/src/main/java/com/prideregion/core/ProfileStore.java" \
 "$ROOT/src/main/java/com/prideregion/core/LegacyTagView.java" \
 "$ROOT/src/main/java/com/prideregion/core/PrideCoreConfig.java" \
 "$ROOT/src/test/java/com/prideregion/core/ProgressionTests.java"
java -cp "$TMP" com.prideregion.core.ProgressionTests
