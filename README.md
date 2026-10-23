# PrideCore Flashpoints v0.3.2 — Boss outcome simulation and persistence (STAGING ONLY)

**Minecraft 1.21.1 / Fabric / Java 21.** This small release is a testable, server-side boss state-machine foundation. It does **not** automatically detect Cobblemon captures, battle victories, or party wipes.

## New in v0.3.2

- Durable, CRC-checked and atomically replaced simulated boss progression stored in `config/pride_flashpoint_boss.bin`.
- Exactly one progression record bound to a specific staging dungeon dimension, seed and block origin.
- Supported state transitions: `READY -> ACTIVE -> CAPTURED/DEFEATED/WIPED` and `WIPED -> ACTIVE` on explicit `retry`. Both `CAPTURED` and `DEFEATED` are terminal. No double resolution or transition skipping.
- Admin-only **`/pridefp boss simulate`** commands are a *test harness*, not callbacks from Cobblemon. The game has **NOT** detected or attributed any capture/battle/wipe.
- The simulated result has no reward impact, player party changes, Pokémon entity changes, battle control, or dungeon block modifications.
- Existing v0.3.1 persistent encounter UUID tracking and v0.2.3 restart-safe cleanup remain intact.

## Installation

1. **Back up** the disposable `multiworld:fptest` world and entire `config/` directory before replacing the JAR.
2. If there is a tracked Pokémon UUID from a previous test, handle its in-world status first. `/pridefp encounter forget <room>` only forgets a UUID; it never deletes a Pokémon. Do not remove active journals manually.
3. Remove the previous `pride-flashpoint-procedural-0.3.1.jar`, install **only** `pride-flashpoint-procedural-0.3.2.jar`, then restart Minecraft.
4. Keep `config/pride_flashpoint_dev.properties` pointing at your *disposable* Multiworld dimension:
   ```properties
   allowed_dimension=multiworld:fptest
   enabled=true
   enable_test_encounters=true
   ```
   The last setting permits testing of UUID tracking and boss simulations. Turn it back off when not testing.

## Cobblemon compatibility

Run `/pridefp compat` in Minecraft to display the Cobblemon mod version actually loaded on your server. This does not modify the world and will help us pin the correct native capture and battle event API in the next development increment. The simulated outcomes do **not** call Cobblemon battle or capture APIs.

## Operator smoke test

If the existing Standard dungeon and its recovery journal are still present, `/pridefp status` should identify its seed/world/origin. If no dungeon is active, generate a new Standard dungeon **only in a new empty area**; never generate over any existing structure.

```mcfunction
/pridefp status
/pridefp boss status
/pridefp boss simulate start
/pridefp boss simulate capture
/pridefp boss status
```

**Expected:** Boss status says `[SIMULATION ONLY] Boss CAPTURED attempts=1` with the dungeon seed/origin. This simulates the outcome observed earlier when the player captured Garchomp; it is **not** proof of an in-game Cobblemon capture event.

To test wipe/retry instead, use a fresh boss simulation record in a separate test after cleanup/reset:

```mcfunction
/pridefp boss simulate start
/pridefp boss simulate wipe
/pridefp boss simulate retry
/pridefp boss simulate defeat
```

**Restart test:** With the simulation ledger still present, restart the server and run `/pridefp boss status`; it should report the same saved state. The server must also preserve any active dungeon recovery journal.

`/pridefp boss simulate capture` cannot be repeated after a terminal outcome. No rewards are issued, and no Pokémon will be changed. Capturing actual wild Pokémon still uses ordinary Cobblemon gameplay.

After resolving any tracked Pokémon and clearing the dungeon blocks with `/pridefp clear` (verify the journal is removed), you may run:

```mcfunction
/pridefp boss reset
```

`reset` deletes **only** the boss simulation journal and is refused while a dungeon/job/recovery journal exists. It never deletes Pokémon or the dungeon itself.

## Not yet implemented

- Native Cobblemon capture/battle callbacks, party attribution, defeated-versus-despawned distinction, server-restart event reconciliation.
- Reward distribution, lockouts/anti-duplication, matchmaking, actual boss objectives, cinematic camera work.
- Automated end-to-end gameplay for the 15–25 minute Standard Flashpoint goal.

**Release gate:** Compile tests for this stage machine are not a substitute for Minecraft integration tests. Keep this staging mod off your production Pride Region world. Before integrating Cobblemon event hooks, pin the exact server-side Cobblemon version: APIs across the supported Minecraft 1.21.1 releases may differ.
