#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
mkdir -p build/standalone-tests
javac --release 21 -d build/standalone-tests \
  src/main/java/com/prideregion/flashpoints/layout/DungeonLayout.java \
  src/main/java/com/prideregion/flashpoints/layout/DungeonGenerator.java \
  src/test/java/com/prideregion/flashpoints/layout/DungeonGeneratorTest.java
java -ea -cp build/standalone-tests com.prideregion.flashpoints.layout.DungeonGeneratorTest
