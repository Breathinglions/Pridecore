# PrideCore Flashpoints v0.3.4 — Native Cobblemon Battle Victory Prototype

**Staging only · Minecraft 1.21.1 · Fabric · Java 21 · Cobblemon exactly 1.8.1+1.21.1**

v0.3.4 adds **real native Cobblemon BATTLE_VICTORY event tracking** for one operator-registered boss. It does NOT grant rewards, manage parties, classify forfeits as wipes or automatically clear dungeons. Preserve all proven v0.3.3 capture behavior.

## What changed

- `CobblemonEvents.BATTLE_VICTORY` is subscribed using the exact Cobblemon 1.8.1 public event observable, version-gated and fail-closed via reflection (no compile-time dependency on Cobblemon's Kotlin jar).
- The event exposes its `battle`, `winners`, `losers`, and `wasWildCapture`. Each battle actor has a `pokemonList` and `playerUUIDs`. A battle Pokémon has a persistent `originalPokemon.uuid` matching the pre-battle persistent Pokémon UUID (not the entity UUID).
- A *single PLAYER actor* fighting a *single WILD actor* qualifies only if the PLAYER actor is the operator who armed the boss, and the WILD actor contains exactly the armed Pokémon UUID. Wild-capture-ending battles are excluded to avoid mislabeling catches as defeats.
- Outcomes: **DEFEATED** if the registered player wins; **PLAYER_LOST** if the registered wild boss wins. PLAYER_LOST is NOT automatically classified as a party wipe (forfeits and other battle-ending conditions may be involved).
- Exactly one durable result per armed boss (CRC-checked atomic `config/pride_flashpoint_native_battle.bin`), survives Minecraft restarts, cannot be overwritten by another battle.
- New commands `/pridefp boss battle arm`, `/pridefp boss battle status` and `/pridefp boss battle reset`. Existing capture commands remain unchanged; capture and battle modes cannot be armed simultaneously.
- No production rewards, no auto removal of Pokémon, no automated dungeon clearing and no cinematics.

## Safe testing

1. Confirm the **previous captured boss run is finished**. If the old seed-1047 dungeon is still standing, back up `multiworld:fptest` and `config/`, clear it with `/pridefp clear`, wait for `No dungeon tracked; no recovery journal`, and verify the blocks are gone. Then use `/pridefp boss native reset` to clear the *completed* native capture journal; do not delete its files manually. A previous simulated boss journal should be reset only after cleanup using `/pridefp boss reset`. Resolve any separately tracked entity UUIDs safely before clearing.
2. Stop the server; replace **only** the v0.3.3 JAR with v0.3.4. Restart. Do NOT install both versions together.
3. Keep the disposable staging dimension allowlisted in `config/pride_flashpoint_dev.properties`:
   ```properties
   enabled=true
   allowed_dimension=multiworld:fptest
   enable_test_encounters=true
   ```
4. Run `/pridefp compat` and confirm **both** the capture listener and battle listener report installed. If battle subscription is OFF, stop and send logs; do not arm. The event reflection path has not yet been validated by an actual battle in-game.
5. In a **new empty test location** in `multiworld:fptest`, generate a Standard dungeon (e.g. seed 1047). Wait until `/pridefp status` says `COMPLETE`. Use `/pridefp encounter 11` to verify its boss Pokémon and location, and then run `/pridefp encounter spawn 11` once.
6. As the SAME operator who intends to battle the boss, register its UUID **before the fight**:
   ```mcfunction
   /pridefp boss battle arm
   /pridefp boss battle status
   ```
   Arming refuses zero or multiple nearby Pokémon, or a species/level mismatch, and will not overwrite capture/boss-simulation records.
7. **Battle the wild boss and defeat it normally, do not capture it for this specific test.** Once the battle ends:
   ```mcfunction
   /pridefp boss battle status
   ```
   Expect `[NATIVE BATTLE DEFEATED]` including its exact Pokémon UUID, operator UUID and battle UUID. No rewards or Pokémon cleanup is performed. If the registered player loses to the boss, expect `PLAYER_LOST` instead—not `WIPED`.
8. Before wiping this test instance, restart and run `/pridefp boss battle status` again to verify journal persistence.
9. Only after backing up and confirming no live encounter risk, `/pridefp clear` removes dungeon blocks with the existing journal. After visually confirming that the dungeon and block journal are gone, run `/pridefp boss battle reset` to remove the *result record only*. Do NOT use WorldEdit to clear unknown structures.

## Safety boundaries

- **Do not treat this as real party/raid progression yet.** Only a single player versus a single wild Pokémon event is accepted. Allies, multiwild battles, NPC battles, capture-ending battles and unrelated Pokémon are ignored.
- A loss is NOT necessarily a party wipe. Battle forfeits, disconnects, capture attempts and combat-escape outcomes require separate verified integration before objectives/rewards exist.
- The mod cannot retroactively detect a battle that occurred before `arm`. The original captured Garchomp remains the player's property, unaffected by this release.
- All new event records persist in dedicated files and do not alter the existing capture simulation or UUID association journals.

## Validation

Pure Java unit tests cover correct win/loss, wrong player/wrong Pokémon, ambiguous actors, wild-capture exclusion, duplicate completion, atomic replacement, restart roundtrip and CRC damage rejection. GitHub Actions also compiles/remaps the Fabric JAR and checks source safety contracts. **A successful CI build is not proof that the listener works on the server**; the next gate is a real in-game battle.

Upstream tag-1.8.1 API references:
- https://gitlab.com/cable-mc/cobblemon/-/raw/1.8.1/common/src/main/kotlin/com/cobblemon/mod/common/api/events/battles/BattleVictoryEvent.kt
- https://gitlab.com/cable-mc/cobblemon/-/raw/1.8.1/common/src/main/kotlin/com/cobblemon/mod/common/api/battles/model/actor/BattleActor.kt
- https://gitlab.com/cable-mc/cobblemon/-/raw/1.8.1/common/src/main/kotlin/com/cobblemon/mod/common/battles/pokemon/BattlePokemon.kt
