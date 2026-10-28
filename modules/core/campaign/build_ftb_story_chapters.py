#!/usr/bin/env python3
"""Generate additive, reward-free FTB Quests 1.21.1 narrative chapters.

Never edits an existing FTB quest folder or creates global data.snbt.
Produces independent chapters + localization import fragment.
Requires Per Player Quests configured to individually own chapter progress.
"""
from __future__ import annotations
import argparse, hashlib, json, pathlib, re, sys

ID_RE = re.compile(r"^[0-9A-F]{16}$")
GROUPS = (
    ("prologue", "Hearthleaf: The Beginning", "minecraft:compass", 0, 5),
    ("act1", "Act I: Unusual Signals", "minecraft:oak_sapling", 6, 10),
    ("act2", "Act II: Conflicting Stories", "minecraft:redstone", 11, 15),
    ("act3", "Act III: The Buried Truth", "minecraft:amethyst_shard", 16, 20),
    ("act4", "Act IV: Resonance Crisis", "minecraft:nether_star", 21, 24),
    ("league", "Act V: Road to Champion", "minecraft:diamond", 25, 26),
)

def id_for(kind, name):
    value = hashlib.sha256(("pride-story-ftb-v1|" + kind + "|" + name).encode()).hexdigest()[:16].upper()
    assert ID_RE.fullmatch(value)
    return value

def q(value):
    return json.dumps(value, ensure_ascii=True)

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--story", default="modules/core/src/main/resources/story/default.json")
    parser.add_argument("--out", required=True)
    args = parser.parse_args()
    source = json.loads(pathlib.Path(args.story).read_text(encoding="utf-8"))
    npcs = source["npcs"]
    if len(npcs) != 27:
        raise SystemExit("Refusing unexpected story NPC count")
    names = [entry["id"] for entry in npcs]
    if len(set(names)) != len(names):
        raise SystemExit("Duplicate NPC ids")
    output = pathlib.Path(args.out)
    folder = output / "chapters"
    folder.mkdir(parents=True, exist_ok=True)
    locale = []
    manifest = {"schema":1, "chapters":[], "quests":{}, "no_rewards":True,
                "requires":["ftbquests","per_player_quests"],"existing_files_untouched":True}
    prev = None
    seen = set()
    for group, title, icon, begin, end in GROUPS:
        chapter = id_for("chapter", group)
        if chapter in seen: raise SystemExit("ID collision")
        seen.add(chapter)
        locale.append(f"\tchapter.{chapter}.title: {q(title)}")
        quest_nodes = []
        for offset,index in enumerate(range(begin,end+1)):
            npc = npcs[index]
            name = npc["id"]
            quest = id_for("quest", name)
            task = id_for("task", name)
            if quest in seen or task in seen: raise SystemExit("ID collision")
            seen.update((quest,task))
            label = npc["name"]
            subtitle = "Pride Region storyline"
            info = npc["nodes"][0]["text"]
            # Only the existing first-catch and first-battle advancements are
            # independently verified in the legacy onboarding datapack.
            extras = []
            if name == "ranger":
                for event in ("first_catch_event","first_battle_event"):
                    event_id = id_for("task", "legacy_"+event)
                    if event_id in seen: raise SystemExit("ID collision")
                    seen.add(event_id)
                    extras.append((event_id,f"prideregion:onboarding/{event}"))
                    locale.append(f"\ttask.{event_id}.title: {q('Complete '+event.replace('_',' '))}")
            # These actual gym and Champion wins remain League-owned.
            legacy_battles = {
                "forest_warden":"grass", "miner_lyra":"rock",
                "heat_technician":"fire", "waterkeeper":"water",
                "vega_engineer":"electric", "draven_historian":"dragon",
                "ice_archivist":"ice", "ghost_medium":"ghost",
                "hall_of_fame_guide":"champion"
            }
            if name in legacy_battles:
                badge = legacy_battles[name]
                event_id = id_for("task", "legacy_badge_"+badge)
                if event_id in seen: raise SystemExit("ID collision")
                seen.add(event_id)
                extras.append((event_id,"prideregion:league/"+badge))
                locale.append(f"\\ttask.{event_id}.title: {q('Win the official '+badge+' League challenge')}")
            # These nine tasks are physical world interactions with
            # authenticated minecraft:interaction clue markers; they are not
            # auto-completed by talking to the preceding NPC.
            world_evidence = {
                "miner_lyra":"forest", "eclipse_scout":"mine",
                "meridian_scientist":"foundry", "ranger_rowan":"river",
                "cael_aide":"grid", "ice_archivist":"ward",
                "rin_summit":"ice", "eclipse_operative":"echo",
                "ranger_epilogue":"engine"
            }
            if name in world_evidence:
                clue = world_evidence[name]
                clue_task = id_for("task", "world_clue_"+clue)
                if clue_task in seen: raise SystemExit("World clue task collision")
                seen.add(clue_task)
                extras.append((clue_task,"pride_story:clue_"+clue))
                locale.append(f"\ttask.{clue_task}.title: {q('Investigate the '+clue+' evidence')}")
            tasks = [f'\t\t\t{{ id: "{task}", type: "advancement", advancement: "pride_story:{name}" }}']
            tasks += [f'\t\t\t{{ id: "{t}", type: "advancement", advancement: "{adv}" }}'
                      for t,adv in extras]
            fields = [f'\t\t\tid: "{quest}"',
                      f'\t\t\tx: {offset*3}.0d', '\t\t\ty: 0.0d',
                      f'\t\t\ticon: {{ id: "{icon}" }}']
            if prev: fields.append(f'\t\t\tdependencies: ["{prev}"]')
            fields += ['\t\t\ttasks: [', *tasks, '\t\t\t]', '\t\t\trewards: [ ]']
            quest_nodes.append('\t\t{\n'+'\n'.join(fields)+'\n\t\t}')
            locale.append(f'\tquest.{quest}.title: {q(label)}')
            locale.append(f'\tquest.{quest}.quest_subtitle: {q(subtitle)}')
            locale.append(f'\tquest.{quest}.quest_desc: [{q(info)}]')
            locale.append(f'\ttask.{task}.title: {q("Finish the story checkpoint with "+label)}')
            manifest["quests"][name]={"id":quest,"task":task,"advancement":f"pride_story:{name}",
                                     "chapter":chapter,"extra_tasks":extras}
            prev = quest
        data = ('{\n' + f'\tfilename: "{chapter}"\n\tid: "{chapter}"\n'
                + f'\torder_index: {len(manifest["chapters"])}\n'
                + f'\ticon: {{ id: "{icon}" }}\n'
                + '\tquests: [\n' + '\n'.join(quest_nodes)
                + '\n\t]\n}\n')
        (folder / (chapter+'.snbt')).write_text(data,encoding="utf-8")
        manifest["chapters"].append({"name":group,"id":chapter,"title":title,
                                     "filename":chapter+".snbt","count":end-begin+1})
    (output/"PRIDE_STORY_EN_US_IMPORT.snbtfrag").write_text(
        "# Insert the following key-value entries inside the existing lang/en_us.snbt root object.\n"
        + "\n".join(locale) + "\n",encoding="utf-8")
    (output/"manifest.json").write_text(
        json.dumps(manifest,indent=2)+"\n",encoding="utf-8")
    (output/"README.txt").write_text(
        "PRIDE REGION FTB QUEST CHAPTERS — STAGING ONLY\n"
        "Generated from Pride Core 27-NPC story.json. Additive only; six new SNBT chapter files.\n"
        "1. Back up config/ftbquests/quests and server player quest data.\n"
        "2. Verify no chapter filenames/quest IDs collide with your existing FTB pack.\n"
        "3. Copy the six files in chapters/ to config/ftbquests/quests/chapters/ (no overwrite).\n"
        "4. Merge entries from PRIDE_STORY_EN_US_IMPORT.snbtfrag into the existing\n"
        "   config/ftbquests/quests/lang/en_us.snbt INSIDE its braces, preserving old keys.\n"
        "5. Do not replace data.snbt, chapter_groups.snbt, or existing League chapters.\n"
        "6. Set per-player quest progression in the installed Per Player Quests settings.\n"
        "7. Run /ftbquests reload on staging and inspect GUI/quest dependencies.\n"
        "8. Tasks use Pride Story advancements, no rewards. Route1 also requires\n"
        "   existing first_catch_event and first_battle_event advancements.\n"
        "9. These are narrative checkpoint chapters, NOT proof that all future world\n"
        "   puzzles, Gym fights, evidence pickup or crisis boss encounters work.\n"
        "10. Rollback: remove only the six generated chapter files and imported lang entries.\n",
        encoding="utf-8")
    print(f"PASS: {len(manifest['chapters'])} additive FTB chapters, "+
          f"{len(manifest['quests'])} advancement-gated quests; no rewards or existing config edits")

if __name__ == "__main__":
    main()
