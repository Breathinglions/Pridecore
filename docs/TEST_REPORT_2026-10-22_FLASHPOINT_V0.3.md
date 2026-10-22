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
