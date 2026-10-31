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
            elif (name,old_page['id'],old_choice['value']) in (
                ('professor_sequoia','starter_done','theo'),('technician_theo','main','register'),('avery','kit','take_kit')):
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
print(f"PASS: {checks} synthetic unified onboarding dialogue contracts")