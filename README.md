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
3. Install the `pride-flashpoint-procedural-0.2.0.jar` built by GitHub Actions. Start server.
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
