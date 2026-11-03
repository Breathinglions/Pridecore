#!/usr/bin/env python3
"""Synthetic/native-dialogue contract checks, no private League archive required."""
from __future__ import annotations
import importlib.util
import json
from pathlib import Path
import sys

root = Path(__file__).resolve().parents[1]
source = root / "campaign" / "build_unified_module02_dialogue_v043.py"
if not source.exists():
    # Allow operator-local verification from the staging source folder.
    source = Path(__file__).with_name("build_unified_module02_dialogue_v043.py")
spec = importlib.util.spec_from_file_location("unified_packager",source)
module = importlib.util.module_from_spec(spec)
spec.loader.exec_module(module)

checks=0
def check(cond,why):
    global checks
    if not cond: raise AssertionError(why)
    checks += 1

def option(value, action, **kwargs):
    return dict(value=value, text="Original action", action=action, **kwargs)
def p(id, options):
    return dict(id=id, speaker="npc", lines=["Original text"], input={"type":"option", "vertical":True,"options":options})

def lab():
    original_starter=["q.run_command('execute as '+q.player.username+' run function prideregion:onboarding/meet_sequoia');", "q.dialogue.close();", "q.run_command('openstarterscreen ' + q.player.username);"]
    def dialogue(pages): return dict(escapeAction="q.dialogue.close();",pages=pages,speakers={"npc":{"name":"Example"}})
    sequoia=dialogue([
        p("main", [option("starter",list(original_starter),isVisible="q.player.has_chosen_starter() == 0"),option("bye","q.dialogue.close();")]),
        p("region", [option("starter_from_region",list(original_starter),isVisible="q.player.has_chosen_starter() == 0")]),
        p("starter_done",[option("theo",["t.data = q.player.data();","q.run_command('tag ' + q.player.username + ' add pride_starter_chosen');","q.dialogue.close();"])])])
    theo=dialogue([p("main",[option("register",["q.run_command('execute as ' + q.player.username + ' run function prideregion:onboarding/trainer_card');","q.player.save_data();"],isVisible="q.player.has_chosen_starter() == 1")]),p("registered",[option("go","q.dialogue.close();")]),p("storage",[option("go","q.dialogue.close();")]),p("healing",[option("go","q.dialogue.close();")]),p("crafting",[option("go","q.dialogue.close();")])])
    avery=dialogue([p("main",[option("sent","q.dialogue.set_page('kit');",isVisible="q.player.has_chosen_starter() == 1")]),p("kit",[option("take_kit",["q.run_command('execute as ' + q.player.username + ' run function prideregion:onboarding/trainer_guide');","q.player.save_data();"])]),p("assignment",[option("go","q.dialogue.close();")]),p("looking",[option("go","q.dialogue.close();")])])
    return {f"data/pride_region/dialogues/{k}.json":module.serialize(v) for k,v in {'professor_sequoia':sequoia,'technician_theo':theo,'avery':avery}.items()}

original=lab(); transformed={k:json.loads(v) for k,v in module.rewrite_lab(original).items()}
for name in module.LAB:
    path=f"data/pride_region/dialogues/{name}.json"
    before=json.loads(original[path]);after=transformed[path]
    check(after['escapeAction']==before['escapeAction'],name+": escape action")
    for old_page in before['pages']:
        new_page=module.page(after,old_page['id'])
        for old_choice in old_page['input']['options']:
            new_choice=module.choice(after,old_page['id'],old_choice['value'])
            check(old_choice.get('isVisible')==new_choice.get('isVisible'),name+": visibility")
            if (name,old_page['id'],old_choice['value']) in (
                ('professor_sequoia','main','starter'),('professor_sequoia','region','starter_from_region')):
                expected=[x for x in old_choice['action'] if 'openstarterscreen ' not in x] + ["q.player.run_command('pridecore story sequoia cinematic');"]
                check(new_choice['action']==expected,'both native starter paths hand off to scene, preserving original meeting action')
                check('openstarterscreen' not in str(new_choice['action']),'no early starter GUI before animation')
            elif (name,old_page['id'],old_choice['value']) == (
                'professor_sequoia','starter_done','theo'):
                recovery = ("q.run_command('execute as ' + q.player.username + "
                            "' run function prideregion:onboarding/sync_player');")
                check(new_choice['action']==old_choice['action']+[recovery,
                    "q.player.run_command('pridecore story sync');"],
                    "Sequoia return must recover only verified native starter advancement before sync")
            elif (name,old_page['id'],old_choice['value']) in (
                ('technician_theo','main','register'),('avery','kit','take_kit')):
                check(new_choice['action']==old_choice['action']+["q.player.run_command('pridecore story sync');"],name+": original service and one sync")
            else:
                check(new_choice['action']==old_choice['action'],name+": other choice unchanged")
    check(any('Hearthleaf' in json.dumps(page) for page in after['pages']),name+": Hearthleaf prologue")
check(len(transformed['data/pride_region/dialogues/professor_sequoia.json']['pages'])==3,
      'no text-only Rin duplicate page; real cinematic triggered from native starter choices')
story_sample={}
for name in ['mom','ranger']:
    path=f'data/pride_story/dialogues/{name}.json'
    fake=dict(pages=[p('main',[option('continue',"q.dialogue.close();")])])
    story_sample[path]=module.serialize(fake)
module.refine_story_prologue(story_sample)
for name in ['mom','ranger']:
    path=f'data/pride_story/dialogues/{name}.json'
    after=json.loads(story_sample[path])
    check(after['pages'][0]['input']==fake['pages'][0]['input'],name+' actions unchanged')
    check('Hearthleaf' in str(after) if name=='mom' else 'Route 1' in str(after),name+' story focus')
# Two separate checkpoints: real Cobblemon starter selection announces return;
# only Sequoia's later native confirmation advances to Theo.
starter_path = "data/prideregion/advancement/onboarding/starter_chosen.json"
starter_function = "data/prideregion/function/onboarding/starter_chosen.mcfunction"
starter_resources = {
    starter_path: module.serialize({
        "criteria":{"picked":{"trigger":"cobblemon:pick_starter"}},
        "requirements":[["picked"]],
        "rewards":{"function":"prideregion:onboarding/starter_chosen"}
    }),
    starter_function: (
        'execute unless entity @s[tag=starter_chosen] run tellraw @s '
        '[{"text":"Return to Professor Sequoia, then register with Technician Theo."}]\n'
        'tag @s add starter_chosen\n'
        'ftbquests change_progress @s complete 2D7D171369126710\n'
    ).encode()
}
changed = module.wire_starter_selection_timing(starter_resources)
check(set(changed)=={starter_path, starter_function},
    "Only native starter timing assets changed")
criteria = json.loads(starter_resources[starter_path])
check(criteria["requirements"] == [["picked"], ["native_starter_recorded"]],
    "Starter advancement has OR fallback rather than AND gating")
check(criteria["criteria"]["native_starter_recorded"]["conditions"] ==
    {"player":{"advancements":{"cobblemon:root": True}}},
    "Starter selection fallback must use real Cobblemon root proof")
check("Return to Professor Sequoia at the Hearthleaf laboratory." in
    starter_resources[starter_function].decode(),
    "Starter event tells trainer to return to Sequoia, not go to Theo")
check("then register with Technician Theo." not in
    starter_resources[starter_function].decode(),
    "Starter event cannot prematurely tell the player to go to Theo")
check("ftbquests change_progress @s complete 2D7D171369126710" in
    starter_resources[starter_function].decode(),
    "Original FTB starter quest ID preserved")
# The original 24 League service spawn helpers must not retag a nearby
# wrong NPC class when the requested spawn fails or an entity overlaps.
def service_spawn(name):
    return (f"# Pride Region reload-safe NPC placement helper: {name}\n"
        f'function prideregion:npc_services/spawn/_spawn_runtime {{npc:"pride_region:{name}"}}\n'
        + ("execute rotated ~ 0 positioned ^ ^ ^1.5 run tp "
           "@e[type=cobblemon:npc,sort=nearest,limit=1,distance=..0.35] ~ ~ ~ ~ 0\n")
        + ("execute rotated ~ 0 positioned ^ ^ ^1.5 run tag "
           "@e[type=cobblemon:npc,sort=nearest,limit=1,distance=..0.35] add pride_service_npc\n")
        + ("execute rotated ~ 0 positioned ^ ^ ^1.5 run tag "
           "@e[type=cobblemon:npc,sort=nearest,limit=1,distance=..0.35] add pride_npc_" + name + "\n")
        + ("execute rotated ~ 0 positioned ^ ^ ^1.5 run attribute "
           "@e[type=cobblemon:npc,sort=nearest,limit=1,distance=..0.35] "
           "minecraft:generic.knockback_resistance base set 1\n")).encode()
all_services = [
    "aide_nia", "avery", "backpack_outfitter", "battle_mechanics_specialist",
    "battle_tower_receptionist", "card_collector", "daycare_attendant",
    "expedition_guide", "field_scout", "housing_agent", "league_registrar",
    "market_registrar", "move_tutor", "pokemon_center_nurse",
    "pokemon_specialist", "pokemon_work_foreman", "professor_sequoia",
    "raid_researcher", "ranked_official", "researcher_mira",
    "resource_ranger", "technician_theo", "tournament_director",
    "transit_attendant"
]
original_spawns = {
    f"data/prideregion/function/npc_services/spawn/{n}.mcfunction": service_spawn(n)
    for n in all_services
}
guarded_spawns = original_spawns.copy()
changed_spawns = module.harden_original_service_spawn_helpers(guarded_spawns)
check(set(changed_spawns) == set(original_spawns), "All 24 League service spawns hardened")
for n in all_services:
    p = f"data/prideregion/function/npc_services/spawn/{n}.mcfunction"
    text = guarded_spawns[p].decode("utf8")
    check(f'nbt={{NPCClass:"pride_region:{n}"}}' in text,
        n + " must verify original NPCClass")
    check("tag=!pride_service_spawn_existing" in text,
        n + " must exclude existing NPCs")
    check("remove pride_service_spawn_existing" in text,
        n + " must clear temporary tags")
    check(f'{{npc:"pride_region:{n}"}}' in text,
        n + " must preserve original /spawnnpc class")
check(original_spawns != guarded_spawns, "Original unsafe nearest selectors replaced")
try:
    module.harden_original_service_spawn_helpers({
        "data/prideregion/function/npc_services/spawn/only_one.mcfunction":
            service_spawn("only_one")})
    raise AssertionError("Unsafe incomplete helper set was accepted")
except ValueError:
    checks += 1

print(f"PASS: {checks} synthetic unified onboarding dialogue contracts")