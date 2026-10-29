# Verified battle-phase API surface, locally supplied binaries

Inspection performed against these supplied **unmodified binary JARs** (not a running game):
- `Cobblemon-fabric-1.8.1+1.21.1.jar`
- `rctapi-fabric-1.21.1-0.16.0-beta.jar`
- `rctmod-fabric-1.21.1-0.19.0-beta.jar`

The runtime player/boss handoff still requires a staged multiplayer playthrough with the real server and client pack.

## RCT confirmation

The inspected `RCTApi` class exposes `getInstance(String)`, `initInstance(String)`, and `getBattleManager()`. `BattleManager` exposes `startBattle(List, List, BattleFormat, BattleRules): UUID`, `getState(UUID)`, `getDefaultRules()`, and `end(UUID)`. `BattleState` exposes `getBattle()`, `getParticipants1()`, and `getParticipants2()`. `TrainerNPC` has a five-argument constructor accepting a string, Pokémon array, trainer bag, BattleAI, and living entity. `TrainerPlayer` takes a server player, while `RCTBattleAI` implements Cobblemon's `BattleAI` with a public zero-argument constructor.

## Cobblemon confirmation

`CobblemonEvents` exposes both `BATTLE_VICTORY` and `BATTLE_FAINTED`. `BattleVictoryEvent` exposes `getBattle()`, `getWinners()`, `getLosers()`, and `getWasWildCapture()`. `BattleFaintedEvent` exposes `getBattle()` and `getKilled()`; `BattlePokemon` exposes `getActor()` and `getUuid()`. `PlayerBattleActor.getPlayerUUIDs()` returns a set containing its own actor UUID; default `BattleActor.getPlayerUUIDs()` returns an empty collection. This distinction allows the listener to exclude player Pokémon and count opponent faints for *presentation-only* phases.

`SimpleObservable` supports `subscribe(java.util.function.Consumer)` and `unsubscribe(ObservableSubscription)`.

## What is NOT established by the binary audit

Matching method signatures do not prove successful player-versus-NPC trainer launch, battle AI turns, KO event ordering, loss handling, custom client particle rendering, multiplayer permissions, or compatibility with the server's other installed mods. The native battle adapter remains a staging integration requiring runtime verification.

Module 2's fail-closed preflight now checks the APIs needed for both the win and the faint listener. Fainted Pokémon IDs are deduplicated per authenticated battle. Battle result authorization and reward ownership are unchanged.
