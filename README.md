# v0.2.3 — Recovery chunk-loading regression fix (STAGING ONLY)

**Confirmed screenshot:** In a backed-up disposable `multiworld:fptest`, the dungeon journal survived restart and cleanup began, but the job halted at X=96, Z=-8 because a chunk was not reported as loaded **even though chunk [6,-1] had a force-load ticket**. Restart-safe cleanup is **NOT YET verified end-to-end**.

**Patch scope:**
- **Skip air-only layout voxels before chunk readiness checks** during cleanup, verification, placement, and forensic orphan inspection. The failure at Z=-8 was on an empty perimeter row with no dungeon blocks. Full empty-volume preflight remains unchanged.
- For non-air positions during **cleanup or verification**, request a FULL chunk synchronously on demand if the server reports it unavailable. Bound requests to at most **one chunk per server tick**. Cache recovered chunks for the job.
- Any true loading/write failure still halts cleanup and **retains the existing journal**. The v0.2.2 recovered-job state correction and v0.2.1 post-cleanup verification remain intact.
- Does not add automatic permanent `/forceload` tickets or remove operator-created tickets; avoid altering tickets to unrelated structures.

**Safe test procedure (must be a backed-up disposable test world):**
1. Stop server. Copy/back up `multiworld:fptest` and `config/pride_flashpoint_active.bin`. Do NOT delete the journal or regenerate.
2. Replace only v0.2.2 JAR with v0.2.3 JAR; install one copy. Restart server.
3. Enter `multiworld:fptest`; `/pridefp status` should show **seed 1047, origin -76,-39,-8**. If it doesn't, **stop**.
4. Run `/pridefp clear`. The chunk warming code attempts to load required chunks at most one per server tick. It may take several seconds. Progress: `/pridefp status`.
5. Wait for `Cleanup complete ... Recovery journal removed.`, then **visually** inspect the area. If any dungeon blocks remain, stop and report the output; do not delete/overwrite files.
6. After confirmed clearance, release operator-added force-load tickets with `/forceload remove -80 -16 111 159` **only** if those are the tickets you added for this test.

**Limitations:** On-demand `getChunk(..., FULL, true)` may generate missing chunks, so this patch is **not for production dimensions**. It is capped at one synchronous request per server tick, but disk operations may cause noticeable lag. Cleanup still only removes blocks whose state matches the recorded palette in the persistent journal. No actual server smoke test has yet validated this release; CI compilation and audits are not a substitute. Known issue: new instance creation still needs better automated chunk management, and crash recovery is single-journal.

---

# v0.2.2 — Fix confirmed restart cleanup regression (staging only)

**Issue #1 root cause confirmed from source:** In v0.2.1, `clear()` recreated a recovered dungeon with the default `Phase.PREFLIGHT`, so it immediately took the `Preflight cancelled without changes` branch and dropped the in-memory journal reference. The on-disk journal remained intact. A subsequent server restart restored its in-memory reference.

**Changes in v0.2.2:**
- Recovered jobs are explicitly marked `Phase.ABORTED` (already generated) before entering the `CLEANING` phase. This bypasses the erroneous fresh-preflight cancellation.
- `/pridefp status` and `/pridefp clear` can reload the existing valid journal from disk if the in-memory pointer becomes empty.
- The status command no longer incorrectly asserts that no journal exists when the disk record is present.
- Cleanup is refused without an existing journal. The v0.2.1 second-pass verification remains in place before journal deletion.
- Build workflow includes source-level recovery contract checks; **these aren't a substitute for actual Minecraft restart/cleanup testing**.

**Current operator recovery, ONLY on a backed-up disposable Multiworld test world:**
1. Back up the world and `config/pride_flashpoint_active.bin`. Preserve the original journal file.
2. Stop the server and replace only the old `pride-flashpoint-procedural-0.2.1.jar` with `pride-flashpoint-procedural-0.2.2.jar` (both must not coexist).
3. Start the server, enter `multiworld:fptest` and ensure the 188×164 target footprint is loaded. For the known dungeon at origin `-76,-39,-8`, previous test coverage was `/forceload add -80 -16 111 159` in **that same dimension**.
4. Run `/pridefp status`; verify **seed 1047**, world `multiworld:fptest`, origin `-76,-39,-8`.
5. Run `/pridefp clear`, then `/pridefp status`. Allow cleanup and the verification pass to finish over server ticks; visually confirm the dungeon disappeared.
6. Only after completed verified cleanup, release the temporary force-loading tickets (if desired) and remove backup per your normal retention policy.
7. If seed/dimension/origin don't match, or any error occurs, **stop and preserve the journal**. No manual blind clearing.

`/pridefp orphan inspect` and `orphan adopt` are unnecessary if the original journal exists.

**Status:** Java CI compilation/build checked after commit; real in-game restart cleanup with v0.2.2 is **not yet verified**. Standard gameplay still lacks encounters and boss mechanics.

---

# v0.2.1 — Staging-only orphan recovery patch

**Context:** An operator confirmed a Standard dungeon remained after a server restart, while the v0.2 status said `No dungeon tracked; no recovery journal`. Exact cause of the missing file has NOT been established. This patch adds a cautious recovery route; it does not assert that the missing-journal cause is solved.

Before use, **back up the test world and its config folder** and replace v0.2.0 with the new JAR; never load both JARs. In the same disposable Multiworld dimension used previously, with the matching allowlist config and all intersecting chunks loaded:

```mcfunction
/pridefp orphan inspect standard 1047 -76 -39 -8
/pridefp status
```

This is a **read-only scan**, looking for at least **99%** of expected seed-1047 generated blocks at the exact known origin. It never changes blocks. If the match threshold fails, **stop** and do not attempt a blind world edit. If inspection reports PASSED and you confirm the dimension and origin are correct:

```mcfunction
/pridefp orphan adopt
/pridefp status
```

This writes a durable recovery journal but **does not modify blocks**. After making/confirming a backup, run `/pridefp clear` in the same world, with the entire region loaded. Cleanup only removes blocks matching the expected palette at their expected positions. A second verification pass checks that those matching generated blocks are gone **before deleting the journal**.

**Limits:** This process can also remove indistinguishable matching blocks at expected positions, even if somebody replaced them. Keep the test region private and isolated. Successful staging verification of this patch is still pending. Do **NOT** run on a live world. The persistence loss root cause still needs startup logs and a check of `config/pride_flashpoint_active.bin` before/after restart.

---

# PrideCore — Procedural Flashpoint v0.2 STAGING

Minecraft **1.21.1** / Fabric / Java **21**. This standalone server-side test mod is **one module of PrideCore**, not the full MMORPG.

## What changed since v0.1

- **Classic** remains the tested 100×76×9, 12-room procedural Deepslate Ruins layout.
- **Standard** adds a 188×164×14, 16-room layout with wider corridors, larger rooms and 11-block vertical clearance, plus a larger boss arena. Target gameplay duration is **15–25 minutes once encounter gameplay exists**; empty traversal is not yet timed.
- **Staging recovery journal:** saved to `config/pride_flashpoint_active.bin` before the first block is placed. On restart, `/pridefp status` identifies a recovered dungeon; enter its exact dimension and use `/pridefp clear` to remove expected generated blocks. The journal stays until cleanup finishes. Corrupt journals stop generation rather than being deleted.
- Still one dungeon at a time; not a multi-party instance system.

## Commands (operator level 4)

```mcfunction
/pridefp preview 1047
/pridefp generate 1047
/pridefp preview standard 1047
/pridefp generate standard 1047
/pridefp status
/pridefp cancel
/pridefp clear
```

The old commands keep generating the **Classic** v0.1 layout. Preview edits **nothing**. Standard generation requires many loaded chunks; do not use it on your main world.

## Installation & cautious testing

1. **Remove the old v0.1 JAR** from `mods/` before installing v0.2; never install both.
2. Back up the disposable Multiworld test dimension before generating. Confirm Fabric API and Minecraft 1.21.1 with Java 21.
3. Install the `pride-flashpoint-procedural-0.2.1.jar` built by GitHub Actions. Start server.
4. Existing staging config: `config/pride_flashpoint_dev.properties` with `enabled=true` and `allowed_dimension=EXACT_TEST_DIMENSION_REGISTRY_ID`. The world label may not equal its registry ID; **never use a vanilla overworld/nether/end dimension**.
5. Start with `/pridefp preview standard 1047` (no world writes).
6. For generation, work over a large completely empty area, well above ground. Ensure all chunks in the approximately **188×164** build footprint are already loaded (for example via temporary `/forceload` within the test dimension); this v0.2 prototype refuses unloaded chunks rather than forcing them to load. The build origin is located roughly 12 blocks above the player's feet at the layout entrance.
7. Run `/pridefp generate standard 1047` and watch `/pridefp status`. Verify interior paths before cleanup.
8. Run `/pridefp clear` in that test dimension. Watch `/pridefp status` until it reports no journal. Do not delete `pride_flashpoint_active.bin` manually.
9. Restart **only inside this disposable world** to test recovery if desired: after generating, restart, run `/pridefp status` and `/pridefp clear` in the original dimension. No automatic post-restart cleanup is attempted.

## Safety limitations

- Recovery cleanup deletes **only blocks with the expected generated palette/block state at expected coordinates**. If another person places an indistinguishable matching block inside the reserved test build volume, it may also be cleared. Keep the entire region private/isolated while the recovery journal exists.
- The journal is flushed to disk before writing blocks. This gives a best-effort recovery path after restart, but does **not** replace a server backup and isn't yet validated under power loss, plugin reloads, or simultaneous mod instances.
- A failed or unloaded chunk halts generation or cleanup while retaining the journal. Load the region, then run `/pridefp clear` again.
- Journal schema is versioned and stores the entire block layout, so a later change to the generator does not invalidate the saved coordinates. Only one outstanding journal is supported.
- Not production ready: no persistent party instances, Cobblemon battles, rewards, matchmaking, crash/power-loss-integration tests, or cinematics.

## Development

GitHub Actions builds/remaps the JAR and runs standalone regression tests. Validated in-game **v0.1** previously; **v0.2** needs a new staging-world smoke test before production use.
