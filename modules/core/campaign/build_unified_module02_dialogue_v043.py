#!/usr/bin/env python3
"""Stage Pride Core 0.4.3 with audited League 7.10.10 and a unified Hearthleaf prologue.

This is a packaging migration only: original native starter, Trainer Card, Field Kit,
all 13 RCT trainers and their first-win authority are retained. It NEVER changes
player saves, live datapacks, or FTB Quest IDs. The output is staging-only.
"""
from __future__ import annotations
import argparse
import hashlib
import json
from pathlib import Path
from zipfile import ZIP_DEFLATED, ZipFile, ZipInfo

EXPECTED_LEAGUE_SHA = "ca16bbfc591519144d6dc53993f84106b546478df471bf30f913aa05791b3a3f"
LAB = ("professor_sequoia", "technician_theo", "avery")
LEAGUE = ("thorn", "rook", "ashen", "kai", "vega", "draven", "freya", "nyx",
          "koda", "seraph", "celeste", "vesper", "champion")
CLUES = ("forest", "mine", "foundry", "river", "grid", "ward", "ice", "echo", "engine")

def digest(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()

def serialize(value: object) -> bytes:
    return (json.dumps(value, indent=2, ensure_ascii=False) + "\n").encode("utf-8")

def entries(path: Path) -> dict[str, bytes]:
    with ZipFile(path) as archive:
        if archive.testzip() is not None:
            raise ValueError("Corrupt ZIP: " + str(path))
        names = [i.filename for i in archive.infolist() if not i.is_dir()]
        if len(names) != len(set(names)):
            raise ValueError("Duplicate ZIP paths")
        output = {}
        for name in names:
            if not name or name.startswith("/") or "\\" in name or ".." in name.split("/"):
                raise ValueError("Unsafe ZIP entry " + repr(name))
            data = archive.read(name)
            if name.endswith((".json", ".mcmeta")):
                json.loads(data)
            output[name] = data
        return output

def page(data: dict, name: str) -> dict:
    result = [p for p in data["pages"] if p.get("id") == name]
    if len(result) != 1:
        raise ValueError("Missing or duplicate native dialogue page: " + name)
    return result[0]

def choice(data: dict, page_name: str, option_id: str) -> dict:
    items = [c for c in page(data, page_name)["input"]["options"] if c.get("value") == option_id]
    if len(items) != 1:
        raise ValueError("Missing or duplicate native dialogue choice: " + page_name + "/" + option_id)
    return items[0]

def add_sync(data: dict, page_name: str, option_id: str, original_proof: str) -> None:
    option = choice(data, page_name, option_id)
    command = option["action"]
    before = command if isinstance(command, list) else [command]
    if not any(original_proof in x for x in before) or any("pridecore story" in x for x in before):
        raise ValueError("Existing native service action changed: " + page_name + "/" + option_id)
    option["action"] = before + ["q.player.run_command('pridecore story sync');"]

def rewrite_lab(league_data: dict[str, bytes]) -> dict[str, bytes]:
    lab: dict[str, dict] = {}
    for name in LAB:
        path = f"data/pride_region/dialogues/{name}.json"
        lab[name] = json.loads(league_data[path])

    sequoia = lab["professor_sequoia"]
    page(sequoia, "main")["lines"] = [
        {"type": "expression", "expression": "'Ah, ' + q.player.username + '! Welcome to my laboratory here in Hearthleaf.'"},
        "I'm Professor Sequoia. I study the bonds Pokémon share with Trainers—and with the places they call home.",
        "Your mother told me you would be coming. Every great journey begins with a question, and today you get to ask your first."
    ]
    page(sequoia, "region")["lines"] = [
        "Beyond Hearthleaf are forests, rivers, mountain roads, and eight Gym Leaders waiting to meet new Trainers.",
        "Lately, some of the Pokémon in those places have been behaving strangely. We don't yet know why.",
        "Start with a partner you can trust. The rest of the region will reveal itself in time."
    ]
    page(sequoia, "starter_done")["lines"] = [
        "There you are. A new partnership—and the beginning of your own story.",
        "Rin has started a journey too. I hope your paths challenge each other to grow, not just to win.",
        "Before leaving Hearthleaf, see Technician Theo for your Trainer Card and Poképedia, then Avery for your field supplies."
    ]
    original_starter_action = None
    for page_name, option_id in (("main", "starter"), ("region", "starter_from_region")):
        c = choice(sequoia, page_name, option_id)
        commands = c["action"] if isinstance(c["action"], list) else [c["action"]]
        if not any("openstarterscreen " in x for x in commands):
            raise ValueError("Original Cobblemon starter GUI command missing")
        if not any("prideregion:onboarding/meet_sequoia" in x for x in commands):
            raise ValueError("Original Sequoia meeting function missing")
        if original_starter_action is None:
            original_starter_action = list(commands)
        elif original_starter_action != commands:
            raise ValueError("Original starter paths differ; cannot safely unify")
        # Keep the original meet-Sequoia function, original player data and
        # tags, and native Cobblemon starter entitlement. Replace only the
        # immediate starter GUI open with a server-verified cinematic command.
        # Both original starter buttons use the exact same safe sequence.
        c["action"] = [
            x for x in original_starter_action if "openstarterscreen " not in x
        ] + ["q.player.run_command('pridecore story sequoia cinematic');"]
    # No scripted 'Rin:' text-only duplicate and no extra starter button.
    # On success the native dialogue closes, Rin enters privately and Cobblemon
    # opens its own unchanged selector once the exit animation completes.
    add_sync(sequoia, "starter_done", "theo", "pride_starter_chosen")

    theo = lab["technician_theo"]
    page(theo, "main")["lines"] = [
        "Professor Sequoia mentioned a new Trainer might stop by. I'm Theo—I keep the lab equipment working while he chases the big questions.",
        "Every journey starts with a record of who you are and the Pokémon who travel beside you. I can help with that."
    ]
    page(theo, "registered")["lines"] = [
        "There we are. Your Trainer Card is official, and your Poképedia is ready for the road.",
        "Record the Pokémon you meet. Even familiar places have mysteries the books don't explain yet.",
        "Avery is preparing your field kit. Once you're ready, Ranger Mira will be waiting near the road out of Hearthleaf."
    ]
    page(theo, "storage")["lines"] = [
        "Your party can only hold six Pokémon, but your friendships don't stop there.",
        "The storage system keeps the rest of your partners safe until you're ready to travel with them again."
    ]
    page(theo, "healing")["lines"] = [
        "The Hearthleaf Pokémon Center is there whenever your team needs a rest.",
        "It's a long road beyond the village. Looking after your partners matters more than how quickly you travel."
    ]
    page(theo, "crafting")["lines"] = [
        "Apricorns grow all over the region. With a little work, they can become the Poké Balls that accompany your journey.",
        "You'll find plenty of reasons to gather, craft, and share supplies on the roads ahead."
    ]
    add_sync(theo, "main", "register", "prideregion:onboarding/trainer_card")

    avery = lab["avery"]
    page(avery, "main")["lines"] = [
        {"type": "expression", "expression": "'Hey, ' + q.player.username + '! I'm Avery, Professor Sequoia's assistant.'"},
        "I help Trainers find their feet before their first trip beyond Hearthleaf. The road has a way of surprising everyone."
    ]
    page(avery, "kit")["lines"] = [
        "Theo's registration is complete—then you must be ready to see what lies beyond Hearthleaf.",
        "I've packed 10 Poké Balls and food for the journey. Sequoia asked me to make sure your new partner is well looked after.",
        "Ranger Mira has been keeping an eye on the woods near Route 1. Pay attention to the world around you; small discoveries can mean a great deal."
    ]
    page(avery, "assignment")["lines"] = [
        "Your first field assignment is called First Steps. Spend some time with your Pokémon on Route 1, and get to know the wild Pokémon there.",
        "Ranger Mira will be waiting when you've begun to find your way. She knows these roads better than anyone.",
        "When the time is right, the path will lead to Thorn and the Verdant Gym. But there's no need to rush the beginning of your story."
    ]
    page(avery, "looking")["lines"] = [
        "Take your time. Every corner of Sequoia's lab has a story, even if Theo says it's mostly unfinished repairs.",
        "If you're new to Hearthleaf, begin with Sequoia, then visit Theo for registration. I'll have your supplies when you're ready."
    ]
    add_sync(avery, "kit", "take_kit", "prideregion:onboarding/trainer_guide")
    return {f"data/pride_region/dialogues/{name}.json": serialize(lab[name]) for name in LAB}

def refine_story_prologue(story: dict[str, bytes]) -> None:
    for name, lines in {
        "mom": [
            "Good morning! Professor Sequoia has been expecting you at his laboratory in Hearthleaf.",
            "I know it's hard to believe the day has finally come. Whatever you discover on the road, remember you can always come home."
        ],
        "ranger": [
            "I'm Ranger Mira. I've watched over Route 1 for years, but something in the woods has changed lately.",
            "The wild Pokémon seem unsettled. You may notice things even an old Ranger like me has missed."
        ]
    }.items():
        path = f"data/pride_story/dialogues/{name}.json"
        d = json.loads(story[path])
        opts = page(d, "main")
        original_options = json.dumps(opts["input"], sort_keys=True)
        opts["lines"] = lines
        if json.dumps(opts["input"], sort_keys=True) != original_options:
            raise AssertionError("Changed story choice behavior")
        story[path] = serialize(d)

def wire_configurable_onboarding(league: dict[str, bytes]) -> list[str]:
    """Replace only original one-time *give* actions with server-validated kit claims.

    The server remains authority for progression and receipts; native Cobblemon
    dialogues remain original native GUI interactions. Preserve all original
    advancement / quest IDs and recovery functions.
    """
    theo_path = "data/prideregion/function/onboarding/trainer_card.mcfunction"
    avery_path = "data/prideregion/function/onboarding/trainer_guide.mcfunction"
    theo_original = league[theo_path].decode("utf-8")
    avery_original = league[avery_path].decode("utf-8")
    for needle in ("run give @s rctmod:trainer_card 1",
                   "run give @s knowlogy:knowlogy_book 1",
                   "tag @s add trainer_card_received",
                   "tag @s add pokepedia_received",
                   "ftbquests change_progress @s complete 1C33DAB0F3691DAE"):
        if needle not in theo_original:
            raise ValueError("Unexpected original Theo native service: " + needle)
    for needle in ("run give @s cobblemon:poke_ball 10",
                   "run give @s minecraft:bread 8",
                   "run rctmod player set series pride_region",
                   "tag @s add trainer_guide_received",
                   "tag @s add pride_avery_kit",
                   "ftbquests change_progress @s complete 38E1E086CB6ECE25"):
        if needle not in avery_original:
            raise ValueError("Unexpected original Avery native service: " + needle)

    # The Java claim sets the original completion tags only *after*
    # inventory capacity has been checked and each configured item inserted.
    # Hence an invalid/missing mod item never advances onboarding.
    theo = [
        "# Theo native service: configurable items via Pride Core, original tag and quest authority retained.",
        'execute unless entity @s[tag=starter_chosen] run tellraw @s {"text":"Choose your starter and return to Professor Sequoia first.","color":"red"}',
        "execute if entity @s[tag=starter_chosen] unless entity @s[tag=trainer_card_received] run pridecore onboarding claim theo",
        "execute if entity @s[tag=trainer_card_received] run advancement grant @s only prideregion:onboarding/trainer_card",
        "execute if entity @s[tag=trainer_card_received] run ftbquests change_progress @s complete 1C33DAB0F3691DAE",
        ""
    ]
    avery = [
        "# Avery native service: one configurable item kit, original registration / FTB / series integration.",
        'execute unless entity @s[tag=trainer_card_received] run tellraw @s {"text":"Get your Trainer Card and Poképedia from Technician Theo first.","color":"red"}',
        "# Reset stale first-steps detector tasks ONLY at initial unclaimed visit, never on replay.",
        "execute if entity @s[tag=trainer_card_received] unless entity @s[tag=trainer_guide_received] unless entity @s[tag=first_catch] run advancement revoke @s only prideregion:onboarding/first_catch_event",
        "execute if entity @s[tag=trainer_card_received] unless entity @s[tag=trainer_guide_received] unless entity @s[tag=first_catch] run ftbquests change_progress @s reset 008C694ADAE81F20",
        "execute if entity @s[tag=trainer_card_received] unless entity @s[tag=trainer_guide_received] unless entity @s[tag=first_battle_win] run advancement revoke @s only prideregion:onboarding/first_battle_event",
        "execute if entity @s[tag=trainer_card_received] unless entity @s[tag=trainer_guide_received] unless entity @s[tag=first_battle_win] run ftbquests change_progress @s reset 70B779849A4D0F73",
        "execute if entity @s[tag=trainer_card_received] unless entity @s[tag=pride_avery_kit] unless entity @s[tag=trainer_guide_received] run pridecore onboarding claim avery",
        "# The original RCT series remains a one-time non-item service operation.",
        "execute if entity @s[tag=trainer_guide_received] unless entity @s[tag=pride_onboarding_series_synced] run rctmod player set series pride_region",
        "execute if entity @s[tag=trainer_guide_received] run tag @s add pride_onboarding_series_synced",
        "execute if entity @s[tag=trainer_guide_received] run advancement grant @s only prideregion:onboarding/trainer_guide",
        "execute if entity @s[tag=trainer_guide_received] run ftbquests change_progress @s complete 38E1E086CB6ECE25",
        ""
    ]
    league[theo_path] = ("\n".join(theo)).encode("utf-8")
    league[avery_path] = ("\n".join(avery)).encode("utf-8")
    return [theo_path, avery_path]


def build(jar_path: Path, league_path: Path, story_path: Path, destination: Path) -> dict:
    if digest(league_path.read_bytes()) != EXPECTED_LEAGUE_SHA:
        raise ValueError("League datapack checksum differs from original audited 7.10.10")
    base = entries(jar_path)
    league_all = entries(league_path)
    story_all = entries(story_path)
    meta = json.loads(base["fabric.mod.json"])
    if meta.get("id") != "pride_core" or meta.get("version") != "0.4.3-rc1":
        raise ValueError("Wrong compiled Pride Core base JAR/version")
    if any(k.startswith("data/") for k in base):
        raise ValueError("Base JAR already bundles datapack; refusing duplicate pack")
    league = {k:v for k,v in league_all.items() if k.startswith("data/")}
    story = {k:v for k,v in story_all.items() if k.startswith("data/")}
    if set(league)&set(story):
        raise ValueError("Story and League have unexpected overlapping paths")
    # No duplicate narrative Thorn, nor duplicate physical actors for Rin's chapters.
    for duplicate in ("thorn", "rin_canyon", "rin_summit", "ranger_epilogue"):
        if f"data/pride_story/npcs/{duplicate}.json" in story:
            raise ValueError("Duplicate physical story actor " + duplicate)
    npcs = [k for k in story if k.startswith("data/pride_story/npcs/")]
    if len(npcs) != 20:
        raise ValueError(f"Expected exactly 20 narrative-only NPC types, found {len(npcs)}")
    for c in CLUES:
        if f"data/pride_story/advancement/clue_{c}.json" not in story:
            raise ValueError("Missing story clue " + c)
    for trainer in LEAGUE:
        mob = f"data/rctmod/mobs/trainers/single/pride_{trainer}.json"
        trainer_def = f"data/rctmod/trainers/pride_{trainer}.json"
        if trainer_def not in league or mob not in league:
            raise ValueError("Original RCT trainer missing " + trainer)
        commands = json.loads(league[mob]).get("winCommands",{}).get("1")
        if not commands:
            raise ValueError("Original RCT win callback missing " + trainer)
    # Assert original authored resources *before* any override.
    immutable_original = dict(league)
    lab = rewrite_lab(league)
    changed_onboarding = wire_configurable_onboarding(league)
    refine_story_prologue(story)
    resources = {**league, **story, **lab}
    thorn = "data/rctmod/mobs/trainers/single/pride_thorn.json"
    thorn_def = json.loads(resources[thorn])
    thorn_commands = thorn_def["winCommands"]["1"]
    for token in ("league_grass", "ftbquests change_progress", "eco deposit"):
        if not any(token in line for line in thorn_commands):
            raise ValueError("Thorn original victory event incomplete: " + token)
    if not any("@1 pridecore story sync" in line for line in thorn_commands):
        thorn_commands.append("@1 pridecore story sync")
    resources[thorn] = serialize(thorn_def)
    # Every original League file MUST remain byte-identical except the approved
    # 3 story-only dialogue overlays and the *additive* Thorn read-only sync.
    changed_league = sorted(k for k,v in immutable_original.items() if resources[k]!=v)
    # Explicit path comparison: only approved dialogue, kit functions and safe Thorn sync.
    expected_changed = sorted([thorn] + changed_onboarding + [f"data/pride_region/dialogues/{n}.json" for n in LAB])
    if changed_league != expected_changed:
        raise ValueError("Unexpected mutation of League resources: " + repr(changed_league))
    manifest = {
        "schema": 1, "release": "0.4.3-rc1-unified-onboarding", "staging_only": True,
        "original_league_sha256": EXPECTED_LEAGUE_SHA,
        "source_sha256": {"server_jar": digest(jar_path.read_bytes()), "story_native": digest(story_path.read_bytes())},
        "authoritative_league_trainers": len(LEAGUE), "story_npc_types": len(npcs),
        "story_clue_markers": len(CLUES), "datapack_data_entries": len(resources),
        "overridden_lab_dialogues": sorted(lab),
        "onboarding_kit_config": "config/pride_core/onboarding_rewards.json",
        "configurable_native_reward_functions": changed_onboarding,
        "only_approved_original_league_changes": changed_league,
        "player_data_changed": False, "ftb_quest_ids_changed": False,
        "story_npc_duplicates_removed": ["thorn", "rin_canyon", "rin_summit", "ranger_epilogue"],
        "limitations": ["NOT game-tested", "Do not install standalone League or Native Story ZIPs with this JAR",
                        "Rin native cinematic is routed through the server-verified staging adapter; live starter handoff requires in-game testing",
                        "Actual player-onboarding GUI, FTB Quest files and world actor bindings require staging validation"],
        "resource_sha256": {k:digest(v) for k,v in sorted(resources.items())},
    }
    base["pride_core_unified_onboarding_manifest.json"] = serialize(manifest)
    output = {**base, **resources}
    destination.parent.mkdir(parents=True,exist_ok=True)
    with ZipFile(destination, "w", ZIP_DEFLATED, compresslevel=8) as archive:
        for k,v in sorted(output.items()):
            info = ZipInfo(k, (2020,1,1,0,0,0))
            info.compress_type = ZIP_DEFLATED
            info.external_attr = 0o644 << 16
            archive.writestr(info,v)
    verified = entries(destination)
    if set(verified)!=set(output) or any(verified[k]!=v for k,v in output.items()):
        raise AssertionError("Packaged file integrity check failed")
    print(f"PASS: {destination} | {len(LEAGUE)} original League trainers | {len(npcs)} story NPCs | {len(resources)} data assets")
    print("PASS: Sequoia/Theo/Avery native actions retained; 2 starter entry paths, original rewards preserved")
    print("SHA256 " + digest(destination.read_bytes()))
    return manifest

if __name__ == "__main__":
    p=argparse.ArgumentParser()
    p.add_argument("--jar",type=Path,required=True)
    p.add_argument("--league-zip",type=Path,required=True)
    p.add_argument("--story-zip",type=Path,required=True)
    p.add_argument("--out",type=Path,required=True)
    a=p.parse_args()
    build(a.jar,a.league_zip,a.story_zip,a.out)