#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
IMPACTOR_JAR="${1:-$ROOT/libs/Impactor-Fabric-5.3.5+1.21.1.jar}"
if [ ! -f "$IMPACTOR_JAR" ]; then
  echo 'Usage: qa/run_impactor_api_checks.sh /path/to/Impactor-Fabric-5.3.5+1.21.1.jar' >&2
  exit 2
fi
OUT="$(mktemp -d)"
trap 'rm -rf "$OUT"' EXIT
javac -encoding UTF-8 -cp "$IMPACTOR_JAR" -d "$OUT" \
  "$ROOT/src/main/java/com/prideregion/community/RewardLedger.java" \
  "$ROOT/src/main/java/com/prideregion/community/economy/RewardPayoutCoordinator.java" \
  "$ROOT/src/main/java/com/prideregion/community/economy/ImpactorWalletBridge.java" \
  "$ROOT/src/test/java/com/prideregion/community/economy/RewardPayoutCoordinatorTest.java"
java -cp "$OUT" com.prideregion.community.economy.RewardPayoutCoordinatorTest
