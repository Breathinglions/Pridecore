#!/usr/bin/env python3
"""RCT League override must never add a second win callback or modify Gym tags."""
import importlib.util,json,pathlib,tempfile,unittest
from zipfile import ZipFile

ROOT=pathlib.Path(__file__).resolve().parents[1]
module_path=ROOT/"campaign"/"build_league_balance_overlay.py"
spec=importlib.util.spec_from_file_location("league_balance",module_path)
mod=importlib.util.module_from_spec(spec)
spec.loader.exec_module(mod)

class LeagueBalanceTests(unittest.TestCase):
    def setUp(self):
        self.tmp=tempfile.TemporaryDirectory()
        self.jar=pathlib.Path(self.tmp.name)/"mock-unified.jar"
        with ZipFile(self.jar,"w") as z:
            for id in mod.TRAINERS:
                team,mob=mod.locations(id)
                z.writestr(team,json.dumps({
                    "name":id,"battleFormat":"GEN_9_SINGLES",
                    "team":[{"species":"cobblemon:budew","level":12,"moveset":["absorb"]}]
                }))
                commands=["@1 tag @s remove pr_test","@1 execute if entity @s[tag=pr_test] run tag @s add league_grass"]
                if id not in ("koda","seraph","celeste"):
                    commands += [
                        "@1 execute if entity @s[tag=pr_test] run eco deposit 1000 impactor:dollars @s",
                        '@1 execute if entity @s[tag=pr_test] run tellraw @s [{"text":"+₽1,000 "}]'
                    ]
                commands+=["@1 tag @s remove pr_test"]
                z.writestr(mob,json.dumps({"winCommands":{"1":commands}}))
    def tearDown(self):self.tmp.cleanup()
    def test_amount_and_team_only_original_rct_authority(self):
        edited=mod.original(self.jar)
        edited["trainers"]["thorn"]["payout"]=2500
        edited["trainers"]["thorn"]["team"][0]["level"]=15
        files=mod.build(self.jar,edited)
        t,m=mod.locations("thorn")
        self.assertEqual(set(files),{t,m})
        self.assertEqual(json.loads(files[t])["team"][0]["level"],15)
        cmds=json.loads(files[m])["winCommands"]["1"]
        self.assertEqual(sum("run eco deposit" in x for x in cmds),1)
        self.assertIn("run eco deposit 2500 impactor:dollars",str(cmds))
        self.assertIn("₽2,500",str(cmds))
        self.assertIn("league_grass",str(cmds))
    def test_no_new_reward_for_existing_nonpayer(self):
        edited=mod.original(self.jar)
        edited["trainers"]["koda"]["payout"]=999
        with self.assertRaises(ValueError):mod.build(self.jar,edited)
    def test_no_hidden_or_invalid_team_fields(self):
        edited=mod.original(self.jar)
        edited["trainers"]["thorn"]["team"][0]["level"]=101
        with self.assertRaises(ValueError):mod.build(self.jar,edited)
    def test_cannot_change_rct_trainer_identity(self):
        edited=mod.original(self.jar)
        edited["trainers"]["rogue"]={"team":[],"payout":10}
        with self.assertRaises(ValueError):mod.build(self.jar,edited)

if __name__=="__main__":unittest.main()
