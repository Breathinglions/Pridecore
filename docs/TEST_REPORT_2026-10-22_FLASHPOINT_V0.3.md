# Pride Core Flashpoint v0.3 — User staging smoke test

**Reported:** 2026-10-22

**Environment:** Minecraft 1.21.1 Fabric, disposable `multiworld:fptest` Multiworld test dimension.

**Build:** `pride-flashpoint-procedural-0.3.0.jar`, https://github.com/Breathinglions/Pridecore/actions/runs/39865414506

## Confirmed by player report

After receiving v0.3 and the test instructions to generate a Standard dungeon without issuing any manual `/forceload` commands, the operator replied: **"okay yes it works"**. This is user-reported confirmation that the new Standard generation and automatic chunk readiness worked in staging without manual force-loading. Exact server logs / TPS metrics were not provided. Do not infer other unreported tests.

## Next acceptance tests

1. Use `/pridefp status` and confirm `phase=COMPLETE`.
2. Use `/pridefp encounters` and `/pridefp encounter 11` to inspect deterministic room roles and boss marker; these must not spawn Pokémon.
3. Keep `enable_test_encounters=false` until the non-destructive checks pass.
4. After a world backup, optionally opt in to a **single controlled Cobblemon spawn** and verify command behavior, species, level and spawn location; wild test spawns are not managed/removed by block cleanup.
5. Run `/pridefp clear`, verify journal removed and area visually clear. Recover-test a server restart only in disposable backed-up staging.
6. Measure TPS and generation timings; gameplay length of 15–25 min remains a design target, not a measured result.

**Status:** Automatic chunk-loading test reported PASS. Encounter blueprint, optional Cobblemon spawning, and v0.3 cleanup still need explicit confirmation.


## Confirmed encounter blueprint and boss marker (operator screenshot)

The operator supplied an in-game screenshot of three successful admin commands:
- `/pridefp status` showed `Seed 1047, phase=COMPLETE, cursor=431648/431648, placed this session=52884, persistent journal=true, origin=-29,-20,1`.
- `/pridefp encounters` showed all 16 Standard room types, including the two elites, puzzle, rest, treasure and boss assignments.
- `/pridefp encounter 11` reported `Room 11 BOSS at 132,-19,100 — garchomp level 55`.

**Result:** Read-only encounter blueprint reporting and final boss placement command are **confirmed working in game**. This does NOT demonstrate that Garchomp physically spawned, a boss battle ran, rewards were issued, or that cleanup completed for this generated dungeon. Opt-in spawning remains disabled until intentionally tested in the disposable backed-up world.

**Next isolated test:** To test exactly one Cobblemon spawn, operator may enable `enable_test_encounters=true` in `config/pride_flashpoint_dev.properties` and run `/pridefp encounter spawn 11`. The command emits Cobblemon `spawnpokemonat` but currently only dispatches it, and a successful dispatch does not prove an entity appeared. Wild test-spawned Pokémon are **not managed by block cleanup**. A user confirmation of the physical Pokémon and manual controlled cleanup will be required before marking integration passed.
