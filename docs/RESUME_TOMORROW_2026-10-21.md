# Pride Core — Resume Development on October 21, 2026

**Project:** Pride Region / Cobblemon MMORPG (Minecraft 1.21.1, Fabric, Java 21)

**Repository:** https://github.com/Breathinglions/Pridecore

## Current verified milestone

- Fabric server-side **Pride Flashpoint procedural generator v0.1.0** source committed.
- GitHub Actions build **succeeded**: https://github.com/Breathinglions/Pridecore/actions/runs/39548700851
- Standalone layout tests passed for **1,000 procedural seeds**.
- Fabric Java compilation/remapping, JAR manifest/entrypoint checks, and artifact upload succeeded.
- Build artifact name: **PrideFlashpoint-Fabric-1.21.1-JAR**; the artifact ZIP contains `pride-flashpoint-procedural-0.1.0.jar`.
- **NOT yet verified inside a running Minecraft server**. Do not claim full in-game compatibility until staging test passes.
- Repository is **public** at this checkpoint. Do not commit credentials, sensitive data, or proprietary assets.

## First thing tomorrow: test in a disposable Multiworld dimension

1. Download the JAR artifact from the successful GitHub Actions run (not the Gradle source ZIP). Extract the `.jar` and place it in the **test server** `mods/` folder.
2. Confirm Minecraft **1.21.1**, appropriate **Fabric Loader/Fabric API**, and **Java 21**, then restart the disposable server.
3. As an operator with permission level 4, run:
   ```mcfunction
   /pridefp preview 1047
   ```
   This only reports layout dimensions and does not edit the world.
4. For world placement, work only in a **backed-up disposable** Multiworld dimension. The config `config/pride_flashpoint_dev.properties` may first be created when `/pridefp generate` is attempted. Set:
   ```properties
   enabled=true
   allowed_dimension=EXACT_TEST_DIMENSION_REGISTRY_ID
   ```
   The dimension registry identifier may be different from the Multiworld display name `flashpoint_test`. Do **not** guess it.
5. Ensure a fully **empty, loaded** build volume around the test player, with sufficient headroom (generator size approximately **100×76×9 blocks**, origin around **12 blocks above the player's feet**). Then run:
   ```mcfunction
   /pridefp generate 1047
   /pridefp status
   ```
6. Inspect all rooms, passages, lighting, floor geometry, boss room, and server logs. On the **same server session**, run:
   ```mcfunction
   /pridefp clear
   ```
   Check for incomplete cleanup and errors.
7. **Critical safety constraint:** v0.1 stores its cleanup journal only in memory. A server restart, crash, or reload can lose tracked cleanup state. Do not test generation in the main world, and do not assume crash-safe rollback.

## Development milestones after staging

1. **v0.2 quality and safety:** Fix startup/command/API errors; validate Multiworld dimensions; persist generation journals and recovery; improve chunk handling, rollback, observability, and performance; run a real Minecraft smoke test.
2. **Core platform:** Modular services, configuration, events, player profiles, database, permissions, persistence, and server migration tooling.
3. **GTS & economy:** Pokémon listings, secure escrow/trading, auctions, Wonder Trade, shops, currency, anti-duplication and rollback.
4. **Co-op endgame:** Party system, matchmaking, isolated Flashpoints, Operations, world bosses, scripted encounters, progression, rewards.
5. **Cinematics & UI:** Server-driven cutscene events, boss introductions, camera choreography via optional client companion, dialogue, audio/particles, and player interfaces.

## Project direction

Pride Core is intended as **the full Cobblemon MMORPG platform** (inspired by the breadth of features on multiplayer servers such as Delta), not just a dungeon generator. The current compiled JAR is **only the staging Flashpoint geometry prototype**. Future modules remain **planned**, not implemented. Keep modules separable and audit builds at each milestone.

## First handoff message

“Continue Pride Core from `docs/RESUME_TOMORROW_2026-10-21.md`. First review the in-game testing checklist and status of the Fabric 1.21.1 Flashpoint v0.1 mod. Do not claim features are implemented until verified.”
