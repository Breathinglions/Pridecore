# RCT 0.19.0 / RCT API 0.16.0 binary integration audit

Inspected user-supplied Fabric JARs with `javap -public` (compiled signatures, not guessed documentation).

- `rctapi` **0.16.0-beta**, `rctmod` **0.19.0-beta**, Minecraft **1.21.1**, Cobblemon requirement **>=1.8**.
- API: `com.gitlab.srcmc.rctapi.api.battle.BattleManager.startBattle(List<Trainer>, List<Trainer>, BattleFormatProvider, BattleRules)` returns **UUID**. It also exposes `startSingle(Trainer, Trainer)` returning boolean; prefer UUID-returning `startBattle` for matching battle victory events.
- API: `TrainerPlayer(ServerPlayerEntity)` implements `Trainer`.
- API: `TrainerNPC(String, Pokemon[], TrainerBag, BattleAI, LivingEntity)` implements `Trainer`. It can wrap a vanilla living NPC (not necessarily a native Cobblemon NPC). `TrainerRegistry.registerNPC(String, TrainerNPC)` and `getById(String, Class<T>)` exist.
- RCT Mod: `TrainerManager.addBattle(PlayerEntity, TrainerMob)`, `getBattle(UUID)`, `removeBattle(UUID)`; its standard trainer entity is `com.gitlab.srcmc.rctmod.world.entities.TrainerMob`. Do not substitute a villager for `TrainerMob` in these methods.
- RCT Mod `TrainerBattle` exposes initiator and trainer-side participants.
- RCT API `EventContext.register(EventType<T>, EventListener<T>)` supports event subscriptions.

## Integration decision

Prefer an RCT API-driven Pride Core battle launcher using `TrainerPlayer` and a named `TrainerNPC` for each authored encounter. The UUID-returning launch path can be cross-checked against Cobblemon's `BATTLE_STARTED_POST` and `BATTLE_VICTORY` events. Keep official Gym and League battles owned by their existing RCT/TBCS configuration; no fabricated League rewards.

Before activating any launcher, inspect concrete `BattleRules` defaults, construct valid authored `Pokemon[]` from roster species and level, establish the exact server-side lifecycle of the RCT battle UUID and Cobblemon battle UUID, and verify the battle participants. A boolean from `startSingle` is not sufficient battle proof. Ensure callbacks do not accept client-supplied UUIDs or generic Cobblemon wins.

**Status:** JAR signatures verified. Runtime launch, event subscriptions, and trainer-team construction are NOT YET implemented or tested. This audit alone does not make encounters playable.

## 2026-10-28 compatibility fix

Verified against the provided `rctapi-fabric-1.21.1-0.16.0-beta.jar` with `javap -public`: `RCTApi.initInstance(String)` and `RCTApi.getInstance(String)` both exist. The story launcher now initializes its dedicated `pridecore` instance when one does not exist, rather than failing on the first attempted battle. `BattleManager.getDefaultRules()`, `startBattle(List,List,BattleFormat,BattleRules)` returning UUID, and `end(UUID)` were also confirmed. This remains subject to server-side runtime validation; it does not prove trainer battles work end-to-end.
