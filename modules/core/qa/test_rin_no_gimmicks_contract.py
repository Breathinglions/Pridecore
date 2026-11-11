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
# RCTBattleAI resolves an NPC by the Pokemon's registered OT, not by a
# directly constructed TrainerNPC instance. Missing registry enrollment lets
# Mega Showdown's raw canDynamax=true reach the AI unchecked.
for required in (
    'getMethod("getTrainerRegistry")',
    'getMethod("registerNPC", String.class, trainerNpc)',
    'getMethod("getByOT", pokemonType)',
    'getMethod("unregisterById", String.class)',
    'RIN_BATTLE_REGISTRATIONS.put(id, temporaryRegistryId)',
    'RIN_BATTLE_REGISTRATIONS.remove(battleId)',
):
    assert required in source, required
assert source.index('getMethod("registerNPC"') < source.index('getMethod("startBattle"')
module = (root / "src/main/java/com/prideregion/core/story/StoryModule.java").read_text()
assert 'releaseTemporaryRinTrainer(battleId)' in module
assert 'abortOwnedStoryBattles(server, handler.getPlayer().getUuid());' in module
assert 'nativeStoryBattleCancelled(server, battleId);' in module
print("PASS: Route 1 no-gimmicks trainer is registered for RCT AI lookup and safely released")
