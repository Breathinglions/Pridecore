# PrideCore Flashpoints v0.4.0 — Nonlethal Minecraft melee combat prototype

**STAGING ONLY · Minecraft 1.21.1 · Fabric/Java 21 · Cobblemon 1.8.1+1.21.1**

v0.4.0 begins the requested **hybrid Minecraft weapon combat + weakened boss capture**. This release supports **one registered operator, one wild boss, swords and axes only**. It preserves v0.3.4's native Cobblemon capture and traditional battle-victory listeners.

## New functionality

- A separate **300-point ACTION boss HP meter** at `config/pride_flashpoint_action.bin`, bound to the exact wild Pokémon UUID, registered player and the Standard dungeon's seed/dimension/origin.
- A Fabric `AttackEntityCallback` intercepts left-click attacks against only the **registered** Pokémon, **before vanilla melee damage**, and returns `SUCCESS` to consume that attack. It does **not** lower actual Cobblemon Pokémon HP, kill an entity, award XP/loot or affect unrelated Pokémon.
- Sword: **9** custom HP damage per accepted swing; axe: **12**. Accepted attacks have a 10-tick rate limit, require a charged vanilla attack, up-close range, the registered operator, and the boss remaining in the arena. Other players' direct melee hits on that exact registered boss are cancelled but never credited.
- HP is **clamped to 20% (60/300)**, entering `WEAKENED` at the threshold; no further meter damage is possible. The boss remains a live, wild Cobblemon Pokémon. Native Poké Balls can be thrown at it. The existing `POKEMON_CAPTURED` event marks this ACTION record `CAPTURED` only on an exact Pokémon UUID **and operator UUID** match. A captured boss belongs to the player and is never removed by the mod.
- The ACTION meter and captured result are **persisted after each accepted hit** in a CRC-checked, atomic-write journal, surviving server restarts. A missing/damaged journal causes defensive refusal, not blind cleanup.
- Dungeon block cleanup **refuses while ACTION state is ACTIVE or WEAKENED**, including after restart. Once CAPTURED, the existing `/pridefp clear` may run and afterward `/pridefp boss action reset` removes **only** the ACTION ledger.
- Separate test commands under `/pridefp boss action` avoid changing the proven capture and battle journals.

## Important limits

**This is the first controlled melee test, not a finished action RPG boss.**
- No bows/projectile interception, damage from other players/mobs/environment, custom boss attacks, telegraphs, dodge AI, scaling, boss-bar GUI, gear stats, rewards, camera cinematics or instance-wide party credit.
- The Pokémon's **actual Cobblemon HP remains unchanged** when the custom meter falls. `WEAKENED` signals an operator capture *opportunity*, **not a guaranteed or boosted catch rate**. We still need to safely verify capture chance with Cobblemon 1.8.1.
- Vanilla melee against the specific armed Pokémon is cancelled for players through the Fabric interaction callback. **Other sources of damage are not blocked in this prototype**, so never run it in a public/production dimension or expose the test boss to environmental hazards.
- The code intentionally has **no `finish` command** in this release. The first acceptance test is `ACTIVE → WEAKENED → CAPTURED`; defeat-by-weapons requires a separately audited alternative completion implementation.
- `enable_action_combat` defaults to **false**, separately from `enable_test_encounters`. No running instance is automatically opted in.

## Operator test: use a disposable backed-up world only

1. Verify your previous v0.3.4 boss's `DEFEATED` result survived restart (if you haven't already). Then back up `multiworld:fptest` and **the entire `config/` directory**. In the test dimension run `/pridefp clear`; verify `/pridefp status` and empty geometry. Run `/pridefp boss battle reset` **only after cleanup**. If any other native/simulated/tracked records remain, handle and reset them individually. Never delete a live journal by hand.
2. Stop the server. Remove the **0.3.4 JAR**, install **only 0.4.0**, then restart.
3. In `config/pride_flashpoint_dev.properties`, retain your disposable world allowance and **add**:
   ```properties
   enabled=true
   allowed_dimension=multiworld:fptest
   enable_test_encounters=true
   enable_action_combat=true
   ```
   Do not set this in the main survival/production world.
4. Confirm `/pridefp compat` still reports the **native capture listener installed**. Generate the Standard dungeon in a NEW empty test location:
   ```mcfunction
   /pridefp generate standard 1047
   /pridefp status
   /pridefp encounter 11
   ```
   Wait until `phase=COMPLETE`.
5. Spawn one boss and register it for action combat (as the **same operator** who will fight/capture):
   ```mcfunction
   /pridefp encounter spawn 11
   /pridefp boss action arm
   /pridefp boss action status
   ```
   Confirm `[ACTION BOSS ACTIVE]` at 300/300 HP. If arming refuses a mismatch/multiple entities, STOP; don't spawn another Pokémon.
6. In the boss room, **left-click Garchomp with a sword or axe**. Check the action-bar HP message and run `/pridefp boss action status` as needed. Expect `WEAKENED` at 60/300, and further melee attacks should be ignored/cancelled.
7. Try a normal Cobblemon Poké Ball on the registered boss; it **may fail** (no special catch modifier yet). Once a successful native capture event fires:
   ```mcfunction
   /pridefp boss action status
   ```
   Expect `[ACTION BOSS CAPTURED]`. **Do not mistake a failed Poké Ball for completion.**
8. Restart the server (with world/config backed up) and verify the `CAPTURED` state persists. Only then `/pridefp clear` to clean the structure and `/pridefp boss action reset` when block cleanup/journal removal are verified.
9. After testing, set `enable_action_combat=false` to prevent new ACTION boss runs.

**If the test boss vanishes, is defeated by another damage source, fails to capture, or the journal can't be read, DO NOT regenerate over the dungeon or manually delete journals. Stop, preserve logs/world and report results.**

## Next staged milestones

- v0.4.1: inspect realistic boss action damage and reliable source-filtered invulnerability plus ranged combat.
- v0.4.x: attack telegraphs, boss AI, boss bar, verified catch odds, safe alternative FINISH branch.
- v0.5: room objective gates, minibosses, persistent progression, reusable encounter control.
- v0.6+: multiplayer parties, verified wipe/retry, loot and cinematic layer after anti-duplication checks.

## Tests and code safeguards

`bash tools/test-layout.sh` covers the old generators, journal cleanup contracts, Cobblemon capture and battle journals, and the new nonlethal ACTION state machine (including catch/finish replay rejection and corruption). GitHub Actions checks static callback contracts and compiles the Fabric mod.

CI passing is **not** equivalent to in-game runtime testing. v0.4.0 is unsafe for any non-disposable world until the real sword/axe and capture smoke test passes.
