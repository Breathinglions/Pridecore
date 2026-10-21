# PrideCore — Procedural Flashpoint Prototype

Fabric **Minecraft 1.21.1**, Java **21**, server-side staging generator. Generates 12 connected rooms, corridors, lighting, walls and boss chamber geometry from a seed. This is **not** a complete Cobblemon Flashpoint or party instance manager.

## Build

GitHub Actions `Build Pride Flashpoint Fabric JAR` compiles and uploads the remapped JAR. You can also run `gradle build` using Gradle 8.12.1 and JDK 21. Install the resulting `pride-flashpoint-procedural-0.1.0.jar` alongside a matching Fabric API on your disposable test server.

## Safeguards

Disabled by default. The mod creates `config/pride_flashpoint_dev.properties` on first run. To enable, set `enabled=true` and set `allowed_dimension` to the **exact registry ID** of a **backed-up disposable test dimension**. A Multiworld display name does **not** guarantee that registry ID. It refuses to generate outside the allowlist, refuses existing blocks, and places a limited number of blocks per server tick. For testing only; cleanup journal is in memory and is lost when the server restarts. Do not run on your main world.

## Operator commands

- `/pridefp preview 1047`: seed and voxel dimensions, no world edits
- `/pridefp generate 1047`: full empty-region preflight, then paced placement
- `/pridefp status`: generation progress
- `/pridefp cancel`: stop generation
- `/pridefp clear`: clear only tracked, unmodified blocks during the same server session

The build volume is 100 × 76 × 9, roughly 12 blocks above the triggering player's feet. All intersecting chunks must be loaded. Place only in a disposable, empty staging space.

## Missing production features

No Cobblemon encounters, party instances, boss AI, rewards, persistent journals, crash-safe rollback, or dedicated world allocation yet. Before testing on live infrastructure, these features and a real Minecraft integration smoke test must be completed.
