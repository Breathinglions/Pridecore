#!/usr/bin/env python3
"""Build a strictly scoped RCT League data override pack from readable balance JSON.

Only native RCT trainer team arrays and existing first-win payout amounts are
editable. The original RCT win callback and all Gym, E4, Champion quest/badge
tags, Hall of Fame hooks and commands remain the sole reward authority.
"""
from __future__ import annotations
import argparse, copy, json, pathlib, re
from zipfile import ZipFile,ZIP_DEFLATED

TRAINERS=("thorn","rook","ashen","kai","vega","draven","freya","nyx",
          "koda","seraph","celeste","vesper","champion")
MONEY=re.compile(r"(?<=run eco deposit )([0-9]+)(?= impactor:dollars @s)")
LABEL=re.compile(r"\+₽[0-9,]+ ")
NAME=re.compile(r"[a-z0-9_.-]+:[a-z0-9_./-]+")
MOVE=re.compile(r"[a-z0-9_.-]{1,40}")

def dump(obj):
    return (json.dumps(obj,ensure_ascii=False,indent=2)+"\n").encode("utf8")
def locations(id):
    return ("data/rctmod/trainers/pride_"+id+".json",
            "data/rctmod/mobs/trainers/single/pride_"+id+".json")
def original(jar):
    result={}
    with ZipFile(jar) as z:
        for id in TRAINERS:
            team_path,mob_path=locations(id)
            trainer=json.loads(z.read(team_path))
            mob=json.loads(z.read(mob_path))
            money=[x for x in mob["winCommands"]["1"] if MONEY.search(x)]
            if len(money)>1:raise ValueError("Unexpected duplicate native payouts for "+id)
            amount=int(MONEY.search(money[0]).group(1)) if money else None
            result[id]={"team":copy.deepcopy(trainer["team"]),"payout":amount}
    return {"schema":1,"trainers":result}
def validate_roster(team):
    if not isinstance(team,list) or not 1 <= len(team) <= 6:
        raise ValueError("RCT team must contain 1-6 Pokémon")
    for pokemon in team:
        if not isinstance(pokemon,dict) or not {"species","level","moveset"}.issubset(pokemon):
            raise ValueError("Trainer Pokémon entry must contain species/level/moveset")
        species=pokemon["species"]
        level=pokemon["level"]
        moves=pokemon["moveset"]
        if not isinstance(species,str) or not NAME.fullmatch(species):
            raise ValueError("Invalid Pokémon registry ID")
        if type(level) is not int or level < 1 or level > 100:
            raise ValueError("Pokémon level must be 1-100")
        if not isinstance(moves,list) or not 1 <= len(moves) <= 4 or any(
                not isinstance(x,str) or not MOVE.fullmatch(x) for x in moves):
            raise ValueError("Invalid Pokémon moveset")

def build(jar, edited):
    base=original(jar)
    if not isinstance(edited,dict) or set(edited)!={"schema","trainers"} or edited["schema"]!=1:
        raise ValueError("Unknown League config schema")
    if not isinstance(edited["trainers"],dict) or set(edited["trainers"])!=set(TRAINERS):
        raise ValueError("Exactly 13 authoritative original League trainers required")
    override={}
    with ZipFile(jar) as z:
        for id in TRAINERS:
            wanted=edited["trainers"][id]
            if not isinstance(wanted,dict) or set(wanted)!={"team","payout"}:
                raise ValueError("League "+id+" has unsupported balance fields")
            validate_roster(wanted["team"])
            old=base["trainers"][id]
            if wanted["payout"] is None and old["payout"] is not None:
                raise ValueError("Disable first-win payout through a separate RCT migration")
            if old["payout"] is None and wanted["payout"] is not None:
                raise ValueError("Cannot add a new reward source to non-paying trainer "+id)
            if old["payout"] is not None and (
                type(wanted["payout"]) is not int or not 1 <= wanted["payout"] <= 1000000):
                raise ValueError("Payout must be within 1-1,000,000")
            team_path,mob_path=locations(id)
            if wanted["team"]!=old["team"]:
                trainer=json.loads(z.read(team_path))
                trainer["team"]=copy.deepcopy(wanted["team"])
                override[team_path]=dump(trainer)
            if wanted["payout"]!=old["payout"]:
                mob=json.loads(z.read(mob_path))
                lines=mob["winCommands"]["1"]
                found=0
                for n,line in enumerate(lines):
                    if MONEY.search(line):
                        updated,num=MONEY.subn(str(wanted["payout"]),line)
                        if num!=1:raise ValueError("Unsafe RCT original money callback")
                        lines[n]=updated
                        found+=1
                    elif "run tellraw" in line and LABEL.search(line):
                        lines[n]=LABEL.sub("+₽"+format(wanted["payout"],",")+" ",line)
                if found!=1:raise ValueError("Missing original RCT money callback")
                # ALL original commands are immutable except original numeric
                # deposit and matching player-facing currency label.
                unchanged=json.loads(z.read(mob_path))
                for old_cmd,new_cmd in zip(unchanged["winCommands"]["1"],lines):
                    if MONEY.search(old_cmd):
                        if MONEY.sub("<AMOUNT>",old_cmd)!=MONEY.sub("<AMOUNT>",new_cmd):
                            raise ValueError("RCT deposit conditions changed")
                    elif LABEL.search(old_cmd):
                        if LABEL.sub("<AMOUNT>",old_cmd)!=LABEL.sub("<AMOUNT>",new_cmd):
                            raise ValueError("RCT win label semantics changed")
                    elif old_cmd!=new_cmd:
                        raise ValueError("RCT callback changed outside numeric payout")
                override[mob_path]=dump(mob)
    return override

def main():
    p=argparse.ArgumentParser()
    p.add_argument("--jar",type=pathlib.Path,required=True)
    p.add_argument("--config",type=pathlib.Path,required=True)
    p.add_argument("--out",type=pathlib.Path,required=True)
    a=p.parse_args()
    if a.config.is_symlink():raise ValueError("Refusing symlinked League config")
    if not a.config.exists():
        a.config.parent.mkdir(parents=True,exist_ok=True)
        a.config.write_bytes(dump(original(a.jar)))
        print("Created 13-trainer editable RCT League balance config:",a.config)
    if a.config.stat().st_size>131072:raise ValueError("League balance JSON oversized")
    overrides=build(a.jar,json.loads(a.config.read_text(encoding="utf8")))
    a.out.parent.mkdir(parents=True,exist_ok=True)
    with ZipFile(a.out,"w",ZIP_DEFLATED) as z:
        z.writestr("pack.mcmeta",dump({"pack":{"pack_format":48,
            "description":"Pride Region original RCT League balance override (no additional reward authority)"}}))
        for name,data in sorted(overrides.items()):z.writestr(name,data)
    print(f"PASS: {len(overrides)} original RCT entries overridden; no extra trainers or callbacks")

if __name__=="__main__":main()
