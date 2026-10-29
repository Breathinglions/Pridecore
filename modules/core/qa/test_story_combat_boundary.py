#!/usr/bin/env python3
"""Keep Module 02 story bosses exclusively inside Cobblemon/RCT combat.

Static guardrails are a complement to Java policy and runtime tests; they do
not substitute for actual Minecraft gameplay verification.
"""
from pathlib import Path
import json
import re

ROOT = Path(__file__).resolve().parents[1]
source = ROOT / "src/main/java/com/prideregion/core/story"
rosters = json.loads((ROOT / "src/main/resources/story/story_battles.json").read_text())
assert rosters["combatMode"] == "POKEMON_ONLY"
assert rosters["battleFormat"] == "GEN_9_SINGLES"
assert len(rosters["encounters"]) == 9
assert all(1 <= len(e["team"]) <= 6 for e in rosters["encounters"])

module = (source / "StoryModule.java").read_text()
launcher = (source / "RctStoryBattleLauncher.java").read_text()
roster_factory = (source / "CobblemonRosterFactory.java").read_text()
policy = (source / "StoryCombatPolicy.java").read_text()
timeline = (source / "StoryCinematicTimeline.java").read_text()

# The sole storyline damage hook CANCELS physical damage to authenticated
# story trainer actors. It must not damage players, Pokémon or unrelated mobs.
assert "ServerLivingEntityEvents.ALLOW_DAMAGE.register" in module
assert "AttackEntityCallback.EVENT.register" in module
assert "ActionResult.FAIL : ActionResult.PASS" in module
assert "StoryCombatPolicy.protectTrainer(" in module
assert "return !protectedTrainer;" in module
assert "StoryCombatPolicy.requiredMode" not in module or "Mode.HYBRID" not in module
assert 'System.STORY ? Mode.POKEMON_ONLY : Mode.HYBRID' in policy

for filename, text in [
    ("StoryModule", module), ("RctStoryBattleLauncher", launcher),
    ("CobblemonRosterFactory", roster_factory), ("StoryCinematicTimeline", timeline)
]:
    for pattern in [
        r"\.damage\s*\(", r"\.attack\s*\(", r"\.setHealth\s*\(",
        r"\.setOnFire\s*\(", r"\.spawnEntity\s*\(",
        r"\.applyDamage\s*\(", r"\.hurt\s*\(",
        r"\.addStatusEffect\s*\(", r"\.setVelocity\s*\("
    ]:
        assert not re.search(pattern, text), f"{filename}: forbidden physical boss combat call {pattern}"

assert '"GEN_9_SINGLES"' in launcher
assert "startBattle" in launcher
assert "CobblemonRosterFactory.create" in launcher
assert 'PokemonProperties' in roster_factory
assert "new Cue(" in timeline
assert "ParticleTypes" in module
print("PASS: Module 02 contains RCT/Cobblemon Pokémon-only bosses, no hybrid Minecraft combat calls")
