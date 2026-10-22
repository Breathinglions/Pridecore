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
