#!/usr/bin/env python3
"""Unit tests for safe migration of Theo/Avery's native onboarding item grants."""
import importlib.util
import pathlib
import unittest

ROOT = pathlib.Path(__file__).resolve().parents[1]
SCRIPT = ROOT / "campaign" / "build_unified_module02_dialogue_v043.py"
spec = importlib.util.spec_from_file_location("unified_rewards", SCRIPT)
mod = importlib.util.module_from_spec(spec)
spec.loader.exec_module(mod)

THEO = "data/prideregion/function/onboarding/trainer_card.mcfunction"
AVERY = "data/prideregion/function/onboarding/trainer_guide.mcfunction"

THEO_NATIVE = """
execute if entity @s[tag=starter_chosen] unless entity @s[tag=trainer_card_received] run give @s rctmod:trainer_card 1
execute if entity @s[tag=starter_chosen] unless entity @s[tag=pokepedia_received] run give @s knowlogy:knowlogy_book 1
execute if entity @s[tag=starter_chosen] run tag @s add trainer_card_received
execute if entity @s[tag=starter_chosen] run tag @s add pokepedia_received
execute if entity @s[tag=trainer_card_received] run ftbquests change_progress @s complete 1C33DAB0F3691DAE
"""

AVERY_NATIVE = """
execute if entity @s[tag=trainer_card_received] unless entity @s[tag=pride_avery_kit] run give @s cobblemon:poke_ball 10
execute if entity @s[tag=trainer_card_received] unless entity @s[tag=pride_avery_kit] run give @s minecraft:bread 8
execute if entity @s[tag=trainer_card_received] unless entity @s[tag=pride_avery_kit] run rctmod player set series pride_region
execute if entity @s[tag=trainer_card_received] run tag @s add trainer_guide_received
execute if entity @s[tag=trainer_card_received] run tag @s add pride_avery_kit
execute if entity @s[tag=trainer_guide_received] run ftbquests change_progress @s complete 38E1E086CB6ECE25
"""

class ConfigurableNativeServiceTests(unittest.TestCase):
    def data(self):
        return {THEO:THEO_NATIVE.encode(), AVERY:AVERY_NATIVE.encode()}
    def test_native_grants_only_from_protected_server_commands(self):
        data=self.data()
        changed=mod.wire_configurable_onboarding(data)
        self.assertEqual(set(changed), {THEO, AVERY})
        for name,role in ((THEO,"theo"),(AVERY,"avery")):
            text=data[name].decode()
            self.assertIn("pridecore onboarding claim " + role,text)
            self.assertNotIn("run give @s",text)
        self.assertIn("if entity @s[tag=trainer_card_received]",data[THEO].decode())
        self.assertIn("ftbquests change_progress @s complete 1C33DAB0F3691DAE",data[THEO].decode())
        avery=data[AVERY].decode()
        self.assertIn("rctmod player set series pride_region",avery)
        self.assertIn("ftbquests change_progress @s complete 38E1E086CB6ECE25",avery)
        self.assertIn("unless entity @s[tag=trainer_guide_received]",avery)
    def test_no_unknown_native_source_is_silently_migrated(self):
        bad=self.data()
        bad[THEO]=bad[THEO].replace(b"rctmod:trainer_card",b"fake:card")
        with self.assertRaises(ValueError):
            mod.wire_configurable_onboarding(bad)
    def test_java_implementation_restricts_player_identity_and_physical_npc(self):
        source=(ROOT/"src/main/java/com/prideregion/core/OnboardingRewards.java").read_text()
        for needle in ("source.getPlayer()", "pride_service_npc",
                       "pride_npc_technician_theo", "pride_npc_avery",
                       "getCommandTags().contains(claimTag)", "canFit(player, items)",
                       'player.addCommandTag("pride_avery_kit")',
                       "Files.isSymbolicLink(sourceFile)", "Registries.ITEM.containsId",
                       "MAX_CONFIG_BYTES"):
            self.assertIn(needle,source)
        self.assertNotIn("executeWithPrefix",source)

if __name__=="__main__":
    unittest.main()
