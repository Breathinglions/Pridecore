#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
mkdir -p build/standalone-tests
javac --release 21 -d build/standalone-tests \
  src/main/java/com/prideregion/flashpoints/layout/DungeonLayout.java \
  src/main/java/com/prideregion/flashpoints/layout/DungeonGenerator.java \
  src/main/java/com/prideregion/flashpoints/layout/StandardDungeonGenerator.java \
  src/main/java/com/prideregion/flashpoints/encounter/DungeonEncounterPlan.java \
  src/main/java/com/prideregion/flashpoints/encounter/EncounterLedger.java \
  src/main/java/com/prideregion/flashpoints/encounter/BossProgress.java \
  src/main/java/com/prideregion/flashpoints/encounter/BossProgressJournal.java \
  src/main/java/com/prideregion/flashpoints/encounter/NativeCaptureJournal.java \
  src/main/java/com/prideregion/flashpoints/encounter/BattleOutcomeRules.java \
  src/main/java/com/prideregion/flashpoints/encounter/NativeBattleProgress.java \
  src/main/java/com/prideregion/flashpoints/encounter/NativeBattleJournal.java \
  src/main/java/com/prideregion/flashpoints/StagingJournal.java \
  src/test/java/com/prideregion/flashpoints/layout/DungeonGeneratorTest.java \
  src/test/java/com/prideregion/flashpoints/layout/StandardDungeonGeneratorTest.java \
  src/test/java/com/prideregion/flashpoints/encounter/DungeonEncounterPlanTest.java \
  src/test/java/com/prideregion/flashpoints/encounter/EncounterLedgerTest.java \
  src/test/java/com/prideregion/flashpoints/encounter/BossProgressTest.java \
  src/test/java/com/prideregion/flashpoints/encounter/NativeCaptureJournalTest.java \
  src/test/java/com/prideregion/flashpoints/encounter/NativeBattleTest.java \
  src/test/java/com/prideregion/flashpoints/StagingJournalTest.java
java -ea -cp build/standalone-tests com.prideregion.flashpoints.layout.DungeonGeneratorTest
java -ea -cp build/standalone-tests com.prideregion.flashpoints.layout.StandardDungeonGeneratorTest
java -ea -cp build/standalone-tests com.prideregion.flashpoints.StagingJournalTest
java -ea -cp build/standalone-tests com.prideregion.flashpoints.encounter.DungeonEncounterPlanTest
java -ea -cp build/standalone-tests com.prideregion.flashpoints.encounter.EncounterLedgerTest
java -ea -cp build/standalone-tests com.prideregion.flashpoints.encounter.BossProgressTest
java -ea -cp build/standalone-tests com.prideregion.flashpoints.encounter.NativeCaptureJournalTest
java -ea -cp build/standalone-tests com.prideregion.flashpoints.encounter.NativeBattleTest
