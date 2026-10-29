#!/usr/bin/env python3
"""Structural checks for staging-only finale hitboxes. Not an in-game test."""
import json
import pathlib
import re

ROOT = pathlib.Path(__file__).resolve().parents[1]
pack = ROOT / "staging_world_datapack"
metadata = json.loads((pack / "pack.mcmeta").read_text())
assert metadata["pack"]["pack_format"] == 48, "Expected Minecraft 1.21.1 datapack format"
base = pack / "data/pridecore/function/staging"
setup = (base / "setup_engine_props.mcfunction").read_text()
place = (base / "place_engine_props.mcfunction").read_text()
cleanup = (base / "cleanup_engine_props.mcfunction").read_text()
expected = {
    "pride_finale_core",
    "pride_finale_conduit_north",
    "pride_finale_conduit_south",
    "pride_finale_conduit_east",
    "pride_finale_conduit_west",
}
lines = [line for line in place.splitlines()
         if line.strip() and not line.lstrip().startswith("#")]
summons = [line for line in lines if "summon minecraft:interaction" in line]
assert len(summons) == 5, "Must place exactly five interaction props"
observed = set()
for line in summons:
    tags = re.findall(r'"(pride_finale_[a-z_]+)"', line)
    assert len(tags) == 1, "Every summoned prop needs exactly one finale marker"
    observed.update(tags)
    assert '"pride_staging_prop"' in line
    assert "width:1.5f,height:2.0f" in line
assert observed == expected
assert "unless entity" in setup and "pride_staging_prop" in setup
assert "run function pridecore:staging/place_engine_props" in setup
assert "kill @e[type=minecraft:interaction,tag=pride_staging_prop,distance=..32]" in cleanup
assert "kill @e" not in setup and "kill @e" not in place
print("PASS: optional staging world props are bounded, unique and removable")

# Nine hand-placed evidence markers share an exact allowlisted identifier,
# same-dimension local duplication check and a bounded cleanup selector.
clues = {"forest","mine","foundry","river","grid","ward","ice","echo","engine"}
for clue in clues:
    function = (base / f"place_clue_{clue}.mcfunction").read_text()
    marker = f"pride_clue_{clue}"
    assert function.count(f"tag={marker},distance=..3") == 2
    assert function.count("summon minecraft:interaction") == 1
    assert f'Tags:["{marker}","pride_staging_clue"]' in function
    assert "unless entity" in function and "kill @e" not in function
    assert "width:1.0f,height:1.5f" in function
clue_cleanup = (base / "remove_nearest_clue.mcfunction").read_text()
assert "kill @e[type=minecraft:interaction,tag=pride_staging_clue,distance=..4,limit=1,sort=nearest]" in clue_cleanup
assert "kill @a" not in clue_cleanup
print("PASS: nine locally placed investigation clues and nearest-only cleanup validated")
