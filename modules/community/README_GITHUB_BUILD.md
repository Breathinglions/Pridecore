# Pride Community module — Fabric 1.21.1

Independent development module for Pokémon trivia/scramble, announcements, and Impactor economy integration.

This lives in `modules/community` so it does **not** replace or rebuild the existing Flashpoint module. The GitHub Actions job `Build Pride Community (Fabric 1.21.1)` runs core tests, payout coordinator tests, and attempts to compile a Fabric JAR.

## Critical safeguards
- Rewards default to disabled (`payoutsEnabled: false`).
- Existing CRZQuiz, LuckPerms, Styled Chat, Cobblemon, FTB, and LegendaryQuests remain untouched.
- Development JAR artifacts are not production-approved until multiplayer, crash-recovery, and live wallet QA pass.
- The Impactor 5.3.5 mod is an external `modCompileOnly` dependency from its exact published Modrinth Maven version; it is not bundled or modified.
- No secrets or payment credentials are stored in this repository.

## Running builds
Use GitHub Actions → **Build Pride Community (Fabric 1.21.1)** → Run workflow (after the branch has the workflow); or create a pull request to trigger CI. Download the JAR from the completed job's artifacts only after it passes.

The source was imported from Pride Community v0.2 development archive. See `README_START_HERE.md` and `ECONOMY_AND_EVENTS_SPEC.md` for limitations.

## Status
Build and Minecraft runtime compatibility are being tested through this pull request. Do NOT add the generated JAR to PebbleHost until the tests and operator QA are complete.
