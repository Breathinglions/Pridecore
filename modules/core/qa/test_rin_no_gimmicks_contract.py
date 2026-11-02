#!/usr/bin/env python3
"""Route 1 no-transform policy is applied only to the trusted RCT opponent."""
from pathlib import Path
import json
root = Path(__file__).resolve().parents[1]
source = (root / "src/main/java/com/prideregion/core/story/RctStoryBattleLauncher.java").read_text()
roster = json.loads((root / "src/main/resources/story/story_battles.json").read_text())
r = next(x for x in roster["encounters"] if x["id"] == "rin_route1")
assert r["team"] == [{"species":"sprigatito","level":6}]
assert '"rin_route1".equals(encounterId)' in source
assert 'requireNoGimmicksForRoute1(opponent, team, pokemonType, loader);' in source
policy = source.split('private static void requireNoGimmicksForRoute1(',1)[1].split('public static UUID launch(',1)[0]
for contract in (
    'com.gitlab.srcmc.rctapi.api.models.Gimmicks',
    'getConstructor(String.class, boolean.class, boolean.class)',
    '.newInstance(null, false, false)',
    'getMethod("getGimmicks")',
    'getMethod("to", pokemonType, gimmickType)',
    'getMethod("of", pokemonType)',
    'getMethod("tera")',
    'getMethod("dynamax")',
    'getMethod("gmax")',
    'RCT did not accept Rin Route 1',
    'Rin Route 1 gimmicks remain enabled',
):
    assert contract in policy, contract
assert source.index('requireNoGimmicksForRoute1(opponent') < source.index('getMethod("startBattle"')
assert 'setCanDynamax' not in source, "Do not mutate player-facing battle UI"
assert '"rin_canyon".equals(encounterId)' not in source
assert '"rin_summit".equals(encounterId)' not in source
print("PASS: explicit opponent-only Rin Route 1 no-gimmicks guard, no global/RCT Gym modification")
