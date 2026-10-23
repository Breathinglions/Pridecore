# Pride Core Flashpoints v0.3.3 — Native Cobblemon 1.8.1 boss capture TEST (STAGING ONLY)

Minecraft **1.21.1 · Fabric · Java 21 · Cobblemon exactly `1.8.1+1.21.1`**.

## What actually changed

- Verified upstream Cobblemon 1.8.1 source tag: `CobblemonEvents.POKEMON_CAPTURED` is an `EventObservable<PokemonCapturedEvent>`, and the event exposes `pokemon` and `player`. Pokemon has its own stable `uuid`, which is **not the same identity as the transient Minecraft entity UUID**.
- Adds a **version-gated subscriber** using the published Java Consumer observable API. It is installed only if the loaded Cobblemon version string matches `1.8.1+1.21.1`. It registers after server startup; if API reflection fails, registration is disabled and errors are logged instead of crashing or pretending an event was detected.
- Operator command `/pridefp boss native arm` explicitly registers the **persistent Pokemon UUID** of exactly one live Cobblemon Pokemon within the Standard boss-room marker area, and binds it to the current dimension, seed, block origin, and **operator player UUID**.
- Capturing **that exact Pokémon with that exact player** triggers a real Cobblemon event; the capture state is durably saved to `config/pride_flashpoint_capture.bin` as `CAPTURED`. Only the first correct capture counts.
- `/pridefp boss native status` reports the real captured/armed state. **No item rewards, XP, party credit, battle victory detection, automated despawning, camera cinematics or block cleanup** have been added.
- Separate from the v0.3.2 operator simulation journal; old `/pridefp boss status` is still simulation-only. Native capture state uses a **different file** and command subtree. This prevents mislabeling a simulated capture as real.
- `/pridefp clear` **refuses** cleanup while the boss is still ARMED. Once CAPTURED, block cleanup is permitted. `/pridefp boss native reset` can remove the completed association **only after dungeon cleanup**.
- Preserves existing Classic/Standard generator, chunk readiness, prior recovery fixes and UUID ledger.

Cobblemon upstream 1.8.1 reference:
- https://gitlab.com/cable-mc/cobblemon/-/raw/1.8.1/common/src/main/kotlin/com/cobblemon/mod/common/api/events/CobblemonEvents.kt
- https://gitlab.com/cable-mc/cobblemon/-/raw/1.8.1/common/src/main/kotlin/com/cobblemon/mod/common/api/events/pokemon/PokemonCapturedEvent.kt
- https://gitlab.com/cable-mc/cobblemon/-/raw/1.8.1/common/src/main/kotlin/com/cobblemon/mod/common/api/reactive/Observable.kt

## Install + safe test procedure

**Do not test in production.** First back up `multiworld:fptest` and your `config/` directory, especially the journal files. Never delete active journals manually. If the old seed 1047 dungeon is still running its v0.3.2 CAPTURED simulation, clear its blocks via `/pridefp clear` and then `/pridefp boss reset` before starting a new run. If there are tracked room UUIDs, resolve those separately; forgetting a room UUID does not remove a Pokémon.

1. Stop the server, remove the v0.3.2 JAR and install **only** the v0.3.3 JAR. Restart the Fabric 1.21.1 server. Confirm your Cobblemon mod is still `1.8.1+1.21.1`.
2. Keep `config/pride_flashpoint_dev.properties` set to your backed-up **disposable** world:
   ```properties
   enabled=true
   allowed_dimension=multiworld:fptest
   enable_test_encounters=true
   ```
3. In `multiworld:fptest`, run `/pridefp compat`. It should explicitly say native capture listener installed. If not, **stop** and send server logs.
4. In a new large empty staging area, generate a new dungeon:
   ```mcfunction
   /pridefp generate standard 1047
   /pridefp status
   ```
   Wait until `phase=COMPLETE`.
5. Find the expected boss species/level/coords via `/pridefp encounter 11`. While in the same world, spawn exactly **one** boss using `/pridefp encounter spawn 11`, after confirming the room is clear of unrelated Pokémon.
6. **Before capturing it**, and as the player who intends to capture it, run:
   ```mcfunction
   /pridefp boss native arm
   /pridefp boss native status
   ```
   If this reports zero or multiple Pokémon, stop; do not arm any unrelated Pokémon. Verify the wild Pokémon is the expected boss. The initial arm command requires an operator and is *not* group/party enabled.
7. Capture the registered boss using a Poké Ball. Then run:
   ```mcfunction
   /pridefp boss native status
   ```
   The expected server message is `[PRIDE FP NATIVE] Confirmed boss Pokémon captured!`, and `status` should read `[NATIVE CAPTURE CAPTURED]`. This result should also survive a server restart.
8. After recording the result, run `/pridefp clear` and visually confirm cleanup and journal deletion. Then run `/pridefp boss native reset` to clear the completed capture-association file and enable a fresh test. **Reset does not alter captured Pokémon.**

## Limitations and safeguards

- **Capture-only native detection:** defeat, party wipe, fled battles, tournament results and capture by someone other than the arming player are **not automatically tracked**. Continue using manual v0.3.2 simulation for those; automatic event-driven battle recognition is a later increment.
- The initial arm scans exactly one Cobblemon Pokémon near boss room 11. It does not validate species or level against a server callback: use the blueprint and operator visual inspection in the disposable test world. Exact UUID is then required to resolve a capture.
- Only if the server advertises exact Cobblemon `1.8.1+1.21.1` is the native subscriber installed; a nonmatching dependency disables native features but preserves world and journals.
- No rewards are issued; no Pokémon are despawned or modified. The player keeps any legitimately caught Pokémon. The journal is a **progress record**, not a capture control.
- Don't leave permanently armed test encounters unattended. A failed/ambiguous capture should leave the ledger and dungeon intact for diagnosis.
- Automatic chunk loading/placement remains a prototype suitable only for a disposable world.

## Tests

CI tests Classic/Standard geometry, journal recovery, operator-only simulated boss state and native exact-UUID capture journal transitions, wrong Pokémon/player rejection, duplicate capture rejection, restart persistence, and CRC corruption. Source audits verify the version gate and subscriber call. **No CI test replaces the actual runtime Cobblemon 1.8.1 capture test** on your Fabric server.
