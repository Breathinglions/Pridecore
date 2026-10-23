#!/usr/bin/env python3
"""Source-only checks; not a replacement for a running Minecraft + Cobblemon server."""
from pathlib import Path
src=Path("src/main/java/com/prideregion/flashpoints/StagingJobManager.java").read_text()
mod=Path("src/main/java/com/prideregion/flashpoints/PrideFlashpointMod.java").read_text()
cfg=Path("src/main/java/com/prideregion/flashpoints/DevConfig.java").read_text()
assert "j.phase==Phase.PREFLIGHT || j.phase==Phase.PLACING" in src
assert "j.world.getChunkManager().getChunk(cx,cz,ChunkStatus.FULL,true)" in src
assert "if(requestedChunkThisTick)" in src
assert "if(j.phase!=Phase.PREFLIGHT && material==0)continue;" in src
assert 'props.setProperty("enable_test_encounters","false");' in cfg
assert '!config.testEncounters()' in src
assert 'getChild("spawnpokemonat")==null' in src
assert 'job.spawnedRooms.set(id);' in src
assert 'CommandManager.literal("encounters")' in mod
assert 'CommandManager.literal("spawn")' in mod
assert 'if(job.phase==Phase.PREFLIGHT)' in src and 'job.phase=Phase.ABORTED;' in src
print("PASS: source contracts for automatic chunk readiness and opt-in encounter staging")

# v0.3.1: UUID tracking is operator-only, unambiguous and non-destructive.
assert "public String trackNearbyEncounter(ServerWorld world,int roomId)" in src
assert 'matches.size()!=1' in src, "Ambiguous nearby Pokemon must never be bound"
assert 'Registries.ENTITY_TYPE.getId(entity.getType())' in src
assert 'EncounterLedger.save(entityLedgerPath,next)' in src
assert 'public String forgetEncounter(ServerWorld world,int roomId)' in src
assert 'return "Forgot room "+roomId+" UUID association. No Pokémon was removed or modified."' in src
assert 'entityLedger!=null && !entityLedger.bindings().isEmpty()' in src, "Block re-generation over tracked Pokemon"
assert 'CommandManager.literal("track")' in mod
assert 'CommandManager.literal("tracked")' in mod
assert 'CommandManager.literal("forget")' in mod
print("PASS: explicit UUID association safety, duplicate safeguards and no automatic deletion")


# v0.3.2 manual boss-state simulation — never falsely claim native Cobblemon hooks.
assert "public String simulateBoss(ServerWorld world,UUID operator,BossProgress.Action action)" in src
assert "BossProgressJournal.save(bossJournalPath,next)" in src
assert '"[SIMULATED ONLY] Boss state="' in src
assert 'No Cobblemon battle/capture event was detected;' in src
assert 'if(bossProgress!=null && !bossProgress.key().equals(key))' in src
assert 'if(job!=null || recovery!=null || journalPath!=null && Files.exists(journalPath))' in src
assert 'CommandManager.literal("boss")' in mod
assert 'CommandManager.literal("simulate")' in mod
assert 'CommandManager.literal("reset")' in mod
print("PASS: manual-only boss state transitions guarded by test-world permissions and active journal")


# v0.3.3 exact-UUID native Cobblemon 1.8.1 capture only
bridge=Path("src/main/java/com/prideregion/flashpoints/encounter/NativeCaptureBridge.java").read_text()
native=Path("src/main/java/com/prideregion/flashpoints/encounter/NativeCaptureJournal.java").read_text()
assert 'private static final String SUPPORTED="1.8.1+1.21.1";' in bridge
assert 'getField("POKEMON_CAPTURED")' in bridge
assert 'getMethod("subscribe",Consumer.class)' in bridge
assert 'getMethod("getPokemon")' in bridge
assert 'getMethod("getPlayer")' in bridge
assert 'getMethod("getUuid")' in bridge
assert 'server.execute(()->listener.accept(pokemonUuid,actor))' in bridge
assert 'public void onRealPokemonCaptured(MinecraftServer server,UUID pokemonId,UUID catcher)' in src
assert 'captureRecord.pokemon().equals(pokemonId)' in src
assert 'captureRecord.operator().equals(catcher)' in src
assert 'NativeCaptureJournal.save(captureJournalPath,completed)' in src
assert 'captureRecord.stage()==NativeCaptureJournal.Stage.ARMED' in src
assert 'getMethod("getPokemon")' in src
assert 'CommandManager.literal("native")' in mod
assert 'CommandManager.literal("arm")' in mod
assert 'ServerLifecycleEvents.SERVER_STARTED.register' in mod
assert 'Stage { ARMED, CAPTURED }' in native
print("PASS: 1.8.1 capture bridge event field, exact Pokemon/player identity, durable capture state, staging gate")


# v0.3.4 native Cobblemon battle victory — exact matching and no reward paths.
bridge=Path("src/main/java/com/prideregion/flashpoints/encounter/NativeBattleBridge.java").read_text()
rules=Path("src/main/java/com/prideregion/flashpoints/encounter/BattleOutcomeRules.java").read_text()
assert 'getField("BATTLE_VICTORY")' in bridge
assert 'getMethod("subscribe",Consumer.class)' in bridge
assert 'getWasWildCapture' in bridge
assert 'getOriginalPokemon' in bridge
assert 'getPlayerUUIDs' in bridge
assert 'server.execute(()->listener.accept(snapshot))' in bridge
assert 'if(wildCapture || winners.size()!=1 || losers.size()!=1)' in rules
assert 'a.pokemon().size()==1 && a.pokemon().contains(uuid)' in rules
assert 'a.playerIds().size()==1' in rules
assert 'public String armNativeBattle(ServerCommandSource source)' in src
assert 'NativeBattleJournal.save(battleJournalPath,resolved)' in src
assert 'if(battleRecord!=null && battleRecord.stage()==NativeBattleProgress.Stage.ARMED)' in src
assert 'CommandManager.literal("battle")' in mod
assert 'NativeBattleBridge.install(server,event -> JOBS.onNativeBattleVictory(server,event))' in mod
print("PASS: native battle-victory exact boss+player validation, wild-capture exclusion and durable outcome")


# v0.4.0 ACTION melee prototype: staging opt-in + real capture closure only.
action=Path("src/main/java/com/prideregion/flashpoints/encounter/ActionBossState.java").read_text()
journal=Path("src/main/java/com/prideregion/flashpoints/encounter/ActionBossJournal.java").read_text()
config=Path("src/main/java/com/prideregion/flashpoints/DevConfig.java").read_text()
assert 'props.setProperty("enable_action_combat","false");' in config
assert 'public static boolean actionCombatEnabled()' in config
assert 'if(!DevConfig.actionCombatEnabled())' in src
assert 'AttackEntityCallback.EVENT.register' in mod
assert 'JOBS.consumeActionMelee(serverPlayer,target)' in mod
assert 'ActionResult.SUCCESS : ActionResult.PASS' in mod
assert 'public boolean consumeActionMelee(ServerPlayerEntity player,Entity target)' in src
assert 'a.pokemon().equals(pokemonId)' in src
assert 'a.operator().equals(player.getUuid())' in src
assert 'item instanceof AxeItem?12:item instanceof SwordItem?9:0' in src
assert 'ActionBossJournal.save(actionJournalPath,next)' in src
assert 'Math.max(threshold(maxHp),hp-damage)' in action
assert 'phase!=Phase.WEAKENED' in action
assert 'onActionBossCaptured(MinecraftServer server,UUID pokemon,UUID player)' in src
assert 'JOBS.onActionBossCaptured(server,pokemonId,playerId)' in mod
assert 'if(actionRecord!=null && actionRecord.phase()!=ActionBossState.Phase.CAPTURED)' in src
assert 'CRC32' in journal and 'StandardCopyOption.ATOMIC_MOVE' in journal
assert 'CommandManager.literal("action")' in mod
print("PASS: staging-only exact-UUID melee interception, nonlethal 20% gate and native capture journaling")
