# PrideCore Flashpoints v0.3.0 — Staging test build

Minecraft **1.21.1** / Fabric / **Java 21**. This is a staging-only module, not yet the complete Pride Core MMORPG.

## What v0.3 adds

- **Automatic chunk readiness for generation, preflight, placement and cleanup.** No manual `/forceload` should be needed for an empty, isolated test world. Synchronous FULL chunk requests are capped at **one per server tick**; the normal Minecraft chunk manager handles eventual unloading. PrideCore does not add persistent force-load tickets. **Caution:** loading an ungenerated chunk may generate world terrain.
- **Deterministic Standard dungeon encounter blueprint:** 16 rooms, including entrance, combat, two elites, puzzle, rest, treasure and boss rooms. Room coordinates are validated to be walkable in automated tests.
- **Operator-only Cobblemon integration test:** uses Cobblemon's existing `/spawnpokemonat` command, only on a *completed Standard staging dungeon*, only when explicitly enabled, and at most once per combat room per running session. This is a manual spawn test, **not** an automatic encounter/battle/reward system.
- Preserves v0.2.3 cleanup behavior: read-only journal recovery, no fresh-preflight cancellation, cleanup verification and persistent recovery journal.

## Operators: first test

**Before changing JARs, confirm the previous dungeon is visually cleared and `/pridefp status` reports no journal.** If there's a pending journal, back up and finish recovery before generating another test. Keep all testing in a **backed-up disposable Multiworld dimension** and preserve existing server configurations.

1. Stop the server and back up `multiworld:fptest` plus `config/pride_flashpoint_active.bin` if present.
2. Remove v0.2.3 from `mods/`. Install **only** the `pride-flashpoint-procedural-0.3.0.jar` from successful GitHub Actions.
3. Start the server with Fabric 1.21.1 / Java 21 / Fabric API. Enter **multiworld:fptest** and use the existing `config/pride_flashpoint_dev.properties`:
   ```properties
   enabled=true
   allowed_dimension=multiworld:fptest
   enable_test_encounters=false
   ```
   The new opt-in key is **false when absent** from older config files. This mod reads configuration on each relevant command.
4. Start in a large empty *disposable* area:
   ```mcfunction
   /pridefp preview standard 1047
   /pridefp generate standard 1047
   /pridefp status
   ```
   No manual `/forceload` should be needed. Initial chunk loading can produce brief server lag.
5. Once `phase=COMPLETE`:
   ```mcfunction
   /pridefp encounters
   /pridefp encounter 11
   ```
   These commands **only report** room roles and boss-marker details; they do not spawn Pokémon.
6. For an optional, isolated Cobblemon spawn smoke test only, back up the test world and opt in via `enable_test_encounters=true`. Run:
   ```mcfunction
   /pridefp encounter spawn 11
   ```
   This issues `spawnpokemonat` as an operator if Cobblemon's command is installed. Check visually whether a wild Pokémon appears. **No encounter mobs are automatically tracked, removed or rewarded.** Don't test spawns near other players or valuable Pokémon.
7. Run `/pridefp clear`, then `/pridefp status` until completion and verify the generated blocks disappeared and journal was removed. **Cleanup does not remove wild Pokémon spawned by the optional test command.**

## Not included yet

- Automated encounter triggers, trainer battles, group/party/instance allocation, kill tracking, miniboss objectives, final boss phases, rewards, cinematics, cinematics client companion, economy or GTS.
- Safe production-world chunk allocation or automatic persistent chunk-ticket ownership. This module uses synchronous chunk requests with bounded rate, which may generate terrain or briefly increase TPS cost.
- Multi-party and true crash-recovery integration tests at scale.

The target Standard dungeon gameplay duration remains **15–25 minutes**, but it requires encounters and objectives before it can be measured. **Do not install this test module into the live Pride Region world.**

## Source/testing

`bash tools/test-layout.sh` runs Classic, Standard, journal and encounter-plan tests. GitHub Actions compiles the remapped Fabric JAR and also runs `tools/audit-recovery-flow.py` and `tools/audit-v03.py`, which are static safety guards, not runtime Minecraft integration tests.
