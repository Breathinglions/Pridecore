# Pride Core Flashpoints v0.2 — Staging In-Game Test Report

**Date reported:** October 22, 2026  
**Environment:** Minecraft 1.21.1, Fabric server, separate Multiworld test world (`fptest` per screenshot)  
**Build:** v0.2.0, https://github.com/Breathinglions/Pridecore/actions/runs/39692028718  
**Tester evidence:** Player screenshot and chat confirmation.

## Observations and confirmed tests

- `/pridefp generate standard 1047` initially refused to proceed because at least one intersecting chunk was not loaded. The preflight reported **no blocks placed**, correctly protecting the test environment.
- User force-loaded the required region and retried.
- User shared an overhead screenshot of the expanded dungeon generated in the test world.
- The expanded **188×164×14** layout with larger rooms, taller walls, corridors, and regular roof lighting is visible in the screenshot.
- User reported: **“worked good and I was able to go inside.”** This supports successful in-game generation and entrance to the dungeon.
- No observed crash or block-placement error in the screenshot. This is a **user-reported smoke test**, not a comprehensive server-log or performance audit.

## Outstanding tests

- Verify that the **full route from entrance to boss arena** is navigable in game for the larger dungeon.
- **Critical v0.2 recovery check:** Make a full backup of the disposable test world and journal, then restart the server with a generated dungeon outstanding. Re-enter its dimension; run `/pridefp status`, then `/pridefp clear`, and verify complete cleanup. Keep chunks loaded during cleanup.
- Check that `/pridefp status` reports no outstanding journal after cleanup.
- Remove temporary `/forceload` tickets in the test world after successful cleanup.
- Test multiple seeds, generation time/TPS, Cobblemon large-model clearance, and conflicts with concurrent players.
- Actual 15–25-minute Flashpoint duration remains a target and cannot be validated without encounters/objectives.
- Persistent recovery is **implemented** but not yet **server-tested**. Do not claim it is fully verified until the restart test passes.

## Design observations

- Expanded footprint gives more room than v0.1.
- Ceiling light pattern looks evenly gridded. Consider per-theme randomized placement for atmosphere.
- Preserve the original v0.1 algorithm as the Classic preset.

## Safety reminder

Testing stays in the backed-up disposable world only; do not enable generation in the production Pride Region dimension.


## Follow-up — cleanup outcome reported October 22, 2026

The operator subsequently reported: **“journal removed and dungeon cleared.”** This is a successful user-observed cleanup outcome for the v0.2 Standard Flashpoint test, with the persistent journal removed. This report occurred immediately after instructions to restart, check `/pridefp status`, and clear the recovered dungeon. The user explicitly confirmed that **the server was NOT restarted before cleanup**. Cross-restart recovery has **NOT been tested** and must remain pending. No server log or automated assertions were supplied.

**Next quality gates:** perform the restart/recovery test in a backed-up disposable world, confirm no residual blocks and no pending journal, remove temporary `/forceload` tickets, and begin isolated Cobblemon encounter integration planning. Always retain backup/staging-only restrictions.
