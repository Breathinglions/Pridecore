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
