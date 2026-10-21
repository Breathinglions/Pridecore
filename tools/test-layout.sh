#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
mkdir -p build/standalone-tests
javac --release 21 -d build/standalone-tests \
  src/main/java/com/prideregion/flashpoints/layout/DungeonLayout.java \
  src/main/java/com/prideregion/flashpoints/layout/DungeonGenerator.java \
  src/main/java/com/prideregion/flashpoints/layout/StandardDungeonGenerator.java \
  src/main/java/com/prideregion/flashpoints/StagingJournal.java \
  src/test/java/com/prideregion/flashpoints/layout/DungeonGeneratorTest.java \
  src/test/java/com/prideregion/flashpoints/layout/StandardDungeonGeneratorTest.java \
  src/test/java/com/prideregion/flashpoints/StagingJournalTest.java
java -ea -cp build/standalone-tests com.prideregion.flashpoints.layout.DungeonGeneratorTest
java -ea -cp build/standalone-tests com.prideregion.flashpoints.layout.StandardDungeonGeneratorTest
java -ea -cp build/standalone-tests com.prideregion.flashpoints.StagingJournalTest
