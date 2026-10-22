# PrideCore Flashpoint v0.3.1 — UUID encounter tracking prototype

**Minecraft 1.21.1 · Fabric server · Java 21 · disposable staging dimension only**

This is one part of the Pride Core MMORPG, **not** a production-ready MMORPG, boss battle, or raid encounter system.

## What v0.3.1 adds

- A **checksummed, atomic, persistent ledger** of exact Pokémon entity UUIDs for the 16-room Standard dungeon. It lives at `config/pride_flashpoint_entities.bin`.
- **Manual opt-in tracking** of a *single* existing Pokémon near a specific combat room, with exact UUID binding, only in the explicitly allowed test dimension.
- **Read-only inspection** of tracked UUIDs, including whether an entity is currently loaded. "Not loaded/absent" does NOT imply it is dead, captured, despawned or safe to forget.
- **Explicit forgetting** of a binding. Forgetting changes the ledger only; **does not delete, capture, despawn or otherwise modify the Pokémon**.
- New dungeon creation and block cleanup are refused while tracked UUIDs remain unresolved. An operator must **handle the Pokémon separately** and explicitly forget its room association before clearing the structure.
- Preserves v0.3.0 procedural geometry, automatic chunk readiness and manual opt-in test-spawn commands, plus the v0.2.3 recovery/cleanup fixes.

This initial binding deliberately does **not** inspect Cobblemon species or native battle state. It identifies the mod's entity type `cobblemon:pokemon` using vanilla Minecraft's registry, and requires exactly **one** Pokémon entity in a bounded area around the room marker. The operator must verify visually that the candidate is the intended encounter. No heuristic auto-despawn or auto-capture is performed.

## Safe test sequence

**First** back up `multiworld:fptest`, your `config/` directory and any active Flashpoint journals. Stop the server, remove the previous v0.3.0 JAR and install **only** the v0.3.1 JAR. Restart.

Keep:
```properties
enabled=true
allowed_dimension=multiworld:fptest
enable_test_encounters=true
```
The last property opts into **operator-controlled testing**, not automatic encounter spawning.

In `multiworld:fptest`, the boss assigned for Standard seed 1047 was room 11, Garchomp level 55. If the **existing dungeon and Garchomp are still present** and the recovery journal is recognized, do **not** generate over them.

Use:
```mcfunction
/pridefp status
/pridefp encounter tracked
/pridefp encounter track 11
/pridefp encounter tracked
```

- `/pridefp encounter track 11` searches only near room 11's recorded coordinate. It refuses if **zero or multiple** Pokémon are found. If exactly one is found, it persists the UUID without changing the Pokémon.
- If it fails, **do not** spawn more Pokémon or try to bypass the safeguard.
- You can restart, then run `/pridefp encounter tracked` to confirm the UUID is retained.
- **When the Pokémon has been handled separately**, run `/pridefp encounter forget 11` to drop the association. This does **not** remove the Pokémon; the operator remains responsible for it.
- Only then run `/pridefp clear` to remove the dungeon blocks. Cleanup never deletes Cobblemon entities.

## What remains unbuilt

- Authentic Cobblemon spawn-event ownership from creation time; a UUID must currently be associated manually.
- Species validation, safe per-instance despawn logic, capture/battle-state checks, kill tracking, boss victory/defeat integration, rewards, party matchmaking, automatic objectives, cinematic presentation.
- Full PvE balance to reach the agreed 15–25 minute Standard Flashpoint target.

The `pride_flashpoint_entities.bin` file should not be deleted manually. If it is corrupted, the mod refuses tracking changes, and further diagnosis is necessary. Do not use the prototype on the live Pride Region world.

## Regression checks

GitHub Actions compiles the Fabric JAR, tests Classic/Standard layouts and persistent journals, runs the encounter ledger round-trip/corruption tests, and audits key source safety contracts. **Compiling is not an in-game test.** New entity tracking needs the server operator's staging verification.
