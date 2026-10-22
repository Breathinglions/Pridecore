#!/usr/bin/env python3
"""Source-level regression guard for known v0.2.1 recovery bug.

Not a replacement for Minecraft-integration tests.
"""
from pathlib import Path

src = Path("src/main/java/com/prideregion/flashpoints/StagingJobManager.java").read_text()
assert "void reloadRecoveryIfNecessary()" in src, "On-disk journal reload helper missing"
assert "public String status() {\n        reloadRecoveryIfNecessary();" in src, "status does not check on-disk recovery"
assert "public String clear(ServerWorld world) {\n        reloadRecoveryIfNecessary();" in src, "clear does not check on-disk recovery"
recovered = src[src.index("if(recovery!=null && job==null)"):]
required = ["new Job(world,recovery.owner()", "job.journaled=true;", "job.phase=Phase.ABORTED;", "recovery=null;"]
locations = [recovered.index(item) for item in required]
assert locations == sorted(locations), "Recovered job must bypass fresh preflight!"
assert "if(job.phase==Phase.PREFLIGHT)" in recovered, "Preflight guard unexpectedly removed"
assert recovered.index("job.phase=Phase.ABORTED;") < recovered.index("if(job.phase==Phase.PREFLIGHT)"), "Critical transition ordering broken"
assert 'if(journalPath==null || !Files.exists(journalPath))' in src, "Missing on-disk safety check"
assert "j.phase=Phase.VERIFYING" in src and "StagingJournal.remove(journalPath)" in src, "Missing cleanup verification path"
print("PASS: source-level checks for journal reload, recovered state transition, cleanup verification")
