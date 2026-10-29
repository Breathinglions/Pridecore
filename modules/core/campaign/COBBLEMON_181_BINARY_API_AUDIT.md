# Verified Cobblemon 1.8.1 Fabric binary API inspection

Source artifact: user-supplied `Cobblemon-fabric-1.8.1+1.21.1.jar`; verified from class signatures using `javap` (not inferred from other releases).

## Event hooks
- `com.cobblemon.mod.common.api.events.CobblemonEvents.BATTLE_STARTED_POST` is an `EventObservable<BattleStartedEvent.Post>`.
- `CobblemonEvents.BATTLE_VICTORY` is an `EventObservable<BattleVictoryEvent>`.
- `SimpleObservable.subscribe(java.util.function.Consumer<T>)` returns `ObservableSubscription<T>`; `unsubscribe(subscription)` is available.
- `BattleStartedEvent.Post.getBattle()` returns `PokemonBattle`.
- `BattleVictoryEvent.getBattle()` returns `PokemonBattle`; `getWinners()` and `getLosers()` return lists of `BattleActor`; `getWasWildCapture()` is available.
- `PokemonBattle.getBattleId()` returns UUID and `getPlayerUUIDs()` returns Iterable<UUID>.
- `BattleActor.getUuid()` returns UUID, but use `getPlayerUUIDs()` to identify actual player actors. Do not equate all actor UUIDs with player UUIDs.

## Native trainer launch
`BattleBuilder.INSTANCE.pvn(ServerPlayerEntity, NPCEntity, UUID, BattleFormat, boolean, boolean, PartyStore)` and shorter overloads are present; they return `BattleStartResult`. Their NPC argument is Cobblemon's own `com.cobblemon.mod.common.entity.npc.NPCEntity`, NOT vanilla villager. `BattleStartResult.ifSuccessful(...PokemonBattle...)` and `ifErrored(...)` are exposed.

## Security contract and next implementation
Register event listeners once; only accept battle IDs already armed by the trusted story encounter launcher/authorized native NPC. Validate exact battle ID, expected trainer identity, player actor identity and winner; reject wild captures, other trainers, duplicate/mismatched results. Event start alone is not proof of a story battle. Dispatch progress updates on the Minecraft server thread. Keep League and other plugins isolated.

**Remaining dependency:** RCT/TBCS JAR inspection to determine whether those mods launch via Cobblemon `NPCEntity` and how they identify scripted trainers. This document is binary signature evidence, not a claim that the adapter has been compiled or installed.
