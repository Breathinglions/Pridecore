#!/usr/bin/env python3
"""Build a standalone Cobblemon 1.8.1 native NPC dialogue overlay for Module 02.
This pack augments EXACT League 7.10.10 dialogue without changing existing options
or any original starter/kit/League functions. Offline validation only.
"""
from __future__ import annotations
import argparse, hashlib, json, pathlib, zipfile
from io import BytesIO

def dump(value):
    return (json.dumps(value, indent=2, ensure_ascii=False) + "\n").encode("utf-8")

def script_action(npc, choice):
    return "q.player.run_command('pridecore story native " + npc + " " + choice + "');"

def native_npc(dialogue_id, name):
    return {
        "hitbox":"player",
        "resourceIdentifier":"cobblemon:standard",
        "names":[name],
        "interaction":{"type":"dialogue","dialogue":f"pride_story:{dialogue_id}"},
        "battleConfiguration":{"canChallenge":False},
        "canDespawn":False,
        "hideNameTag":False,
        "isMovable":False,
        "isInvulnerable":True,
        "isLeashable":False,
        "allowProjectileHits":False,
        "behaviours":[{"type":"cobblemon:apply_behaviours",
                     "behaviours":["cobblemon:core", "cobblemon:looks_at_players",
                                   "cobblemon:chats", "cobblemon:stationary"]}]
    }

def dialogue(npc, name, greeting, detail, choice):
    return {
        "escapeAction":"q.dialogue.close();",
        "speakers":{
            "npc":{"name":name,"face":"q.npc.face();",
                   "gibber":{"step":2,"interval":0.05,"allowSkip":True}},
            "player":{"name":{"type":"expression","expression":"q.player.username"},
                      "face":"q.player.face();"}
        },
        "pages":[{"id":"main","speaker":"npc","lines":[greeting,detail],
                  "input":{"type":"option","vertical":True,"options":[
                      {"text":"Continue our journey.", "value":"story",
                       "action":[script_action(npc,choice),"q.dialogue.close();"]},
                      {"text":"Goodbye.","value":"bye",
                       "action":"q.dialogue.close();"}
                  ]}}]
    }

def main():
    p=argparse.ArgumentParser()
    p.add_argument("--league-zip", default=None)
    p.add_argument("--campaign", default="modules/core/campaign/acts.json")
    p.add_argument("--out", required=True)
    args=p.parse_args()
    campaign=json.loads(pathlib.Path(args.campaign).read_text(encoding="utf-8"))
    assert len(campaign["npc"]) == 21, "Unexpected campaign NPC count"
    generated={}
    generated["pack.mcmeta"]=dump({"pack":{"pack_format":48,
       "description":"Pride Region Story v0.3.2-rc1: native Cobblemon NPC overlay for League v7.10.10"}})
    original_hashes={}
    if args.league_zip is not None:
        with zipfile.ZipFile(args.league_zip) as original:
            # Keep every original option, condition and service action. Add an
            # idempotent, reward-free sync *after* existing successful handoffs.
            target_options = [
                ("professor_sequoia", "starter_done", "theo", "pride_starter_chosen"),
                ("technician_theo", "main", "register", "prideregion:onboarding/trainer_card"),
                ("avery", "kit", "take_kit", "prideregion:onboarding/trainer_guide"),
            ]
            for filename, page_id, choice_id, required_original in target_options:
                entry = f"data/pride_region/dialogues/{filename}.json"
                source = original.read(entry)
                original_hashes[entry] = hashlib.sha256(source).hexdigest()
                data = json.loads(source)
                selected = [page for page in data["pages"] if page["id"] == page_id]
                if len(selected) != 1:
                    raise ValueError(f"Expected one page {page_id} in {filename}")
                options = [o for o in selected[0]["input"]["options"]
                           if o.get("value") == choice_id]
                if len(options) != 1:
                    raise ValueError(f"Expected one existing choice {choice_id} in {filename}")
                option = options[0]
                action = option["action"]
                original_actions = list(action) if isinstance(action, list) else [action]
                if not any(required_original in item for item in original_actions):
                    raise ValueError(f"Original service action changed for {filename}/{choice_id}")
                sync_action = "q.player.run_command('pridecore story sync');"
                if any("pridecore story" in item for item in original_actions):
                    raise ValueError(f"Unexpected preexisting Pride Core command in {filename}")
                # Append rather than prepend, since rewards and trusted legacy
                # completion tags MUST be established by the existing system first.
                option["action"] = original_actions + [sync_action]
                generated[entry] = dump(data)


    opening=[
      ("mom","Mom",
       "Good morning! Professor Sequoia is expecting you at his lab.",
       "When you're ready, promise you'll look after your partner and explore every road.",
       "begin"),
      ("ranger","Ranger Captain Rowan",
       "You've made it to Route 1. Something has the wild Pokémon uneasy.",
       "Catch your first Pokémon and win your first battle, then report back for field guidance.",
       "explore"),
      ("thorn","Thorn — Verdant Gym",
       "A Grass-type Trainer must respect the land as well as the Pokémon living on it.",
       "Your League win is awarded only through the official Gym battle. This conversation introduces your journey through Verdant.",
       "challenge")
    ]
    for c in campaign["npc"]:
        opening.append((c["id"],c["name"],c["dialogue"],c["followup"],"continue"))
    ids=set()
    for npc,name,greeting,detail,choice in opening:
        if npc in ids: raise ValueError("Duplicate story NPC "+npc)
        ids.add(npc)
        generated[f"data/pride_story/npcs/{npc}.json"]=dump(native_npc(npc,name))
        generated[f"data/pride_story/dialogues/{npc}.json"]=dump(
            dialogue(npc,name,greeting,detail,choice))
        # Compatible with existing League spawn runtime function.
        generated[f"data/pride_story/function/spawn/{npc}.mcfunction"] = (
            f"# Pride Story native NPC: {name}. Stand at placement position and face direction.\n"
            f"function prideregion:npc_services/spawn/_spawn_runtime {{npc:\"pride_story:{npc}\"}}\n"
            f"execute positioned ^ ^ ^1.5 run tag @e[type=cobblemon:npc,sort=nearest,limit=1,distance=..0.35] add pride_npc_{npc}\n"
            f"execute positioned ^ ^ ^1.5 run attribute @e[type=cobblemon:npc,sort=nearest,limit=1,distance=..0.35] minecraft:generic.knockback_resistance base set 1\n"
        ).encode("utf-8")
    # No physical items or commands are awarded by these checkpoints.
    # FTB Quests Per Player can consume each as an ordinary advancement task.
    for npc in ["sequoia", "theo", "avery"] + sorted(ids):
        generated[f"data/pride_story/advancement/{npc}.json"] = dump({
            "criteria":{"done":{"trigger":"minecraft:impossible"}},
            "requirements":[["done"]]
        })
    generated["FTB_QUEST_ADVANCEMENT_MAPPING.txt"]=(
        "# Create an FTB Quests Advancement task with the right-hand id.\\n" +
        "# Personal FTB Quests progression stays with Per Player Quests.\\n" +
        "\\n".join(f"{id} -> pride_story:{id}" for id in
                  ["sequoia", "theo", "avery"] + sorted(ids)) + "\\n"
    ).encode("utf-8")
    generated["PRIDE_STORY_README.txt"]=(
       "Module 02 staging story overlay for exact PokemonLeague7.10.10.\n"
       "Requires Pride Core v0.3.2-rc1 and existing v7.10.10 League datapack.\n"
       "Install SECOND with higher datapack priority on staging only.\n"
       "This pack overrides only the 3 lab NPC dialogues by adding one story option.\n"
       "It does not change starter choices, trainer cards, kits, badge functions or other NPCs.\n"
       "Use /function pride_story:spawn/mom while standing at a chosen world location.\n"
       "Repeat for /function pride_story:spawn/ranger and new NPC cast after world placement.\n"
       "Do not spawn additional Thorn at the Gym boss position without choosing separate story anchor.\n"
       "FTB Quests Per Player chapters and actual battle/world objectives must still be configured.\n"
    ).encode("utf-8")
    generated["PRIDE_STORY_MANIFEST.json"]=dump({
        "version":"0.3.2-rc1",
        "requires":{"minecraft":"1.21.1","cobblemon":"1.8.1",
                    "league_datapack":"7.10.10","pride_core":"0.3.2-rc1"},
        "native_new_npcs":len(opening),
        "overridden_league_dialogues":original_hashes,
        "preexisting_services_untouched":True,
        "ftb_per_player_quests_modified":False,
        "authoritative_battles_modified":False
    })
    for entry,content in generated.items():
        if entry.endswith(".json") or entry.endswith(".mcmeta"):
            json.loads(content)  # avoid packaging malformed JSON
    dest=pathlib.Path(args.out)
    dest.parent.mkdir(parents=True,exist_ok=True)
    with zipfile.ZipFile(dest,"w",zipfile.ZIP_DEFLATED) as z:
        for name,body in sorted(generated.items()):
            z.writestr(name,body)
    with zipfile.ZipFile(dest) as z:
        assert z.testzip() is None
        assert len(z.namelist()) == len(generated)
        assert len([x for x in z.namelist() if x.startswith("data/pride_story/npcs/")]) == 24
        assert len([x for x in z.namelist() if x.startswith("data/pride_story/advancement/")]) == 27
        if args.league_zip:
            assert all(x in z.namelist() for x in
                 ["data/pride_region/dialogues/professor_sequoia.json",
                  "data/pride_region/dialogues/technician_theo.json",
                  "data/pride_region/dialogues/avery.json"])
    print(f"PASS: native Cobblemon story overlay: {len(opening)} new NPC classes, "
          f"{len(original_hashes)} preserved lab dialogues, {len(generated)} files -> {dest}")

if __name__ == "__main__":
    main()
