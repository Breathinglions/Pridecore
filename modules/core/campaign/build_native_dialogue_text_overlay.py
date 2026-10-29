#!/usr/bin/env python3
"""Generate a SAFE native Cobblemon NPC text-only datapack from an editable JSON.

This is NOT an in-game runtime mod reload hook: native Cobblemon data is loaded by
Minecraft's datapack manager. Regenerate the higher-priority pack and /reload.
No service actions, starter choices, isVisible gates, rewards or NPC IDs change.
"""
from __future__ import annotations

import argparse
import hashlib
import json
from pathlib import Path
from zipfile import ZipFile, ZIP_DEFLATED

NATIVE = {
    "sequoia": "data/pride_region/dialogues/professor_sequoia.json",
    "theo": "data/pride_region/dialogues/technician_theo.json",
    "avery": "data/pride_region/dialogues/avery.json",
    "mom": "data/pride_story/dialogues/mom.json",
    "ranger_mira": "data/pride_story/dialogues/ranger.json",
}

def encode(obj):
    return (json.dumps(obj, indent=2, ensure_ascii=False) + "\n").encode("utf8")

def string(value):
    if (not isinstance(value, str) or not value.strip() or len(value) > 1000
            or any(ord(c) < 32 for c in value) or "§" in value):
        raise ValueError("Dialogue text must be plain, nonempty, <=1000 chars, with no control codes")
    return value

def template(jar):
    """Include all ordinary lines and choice labels, leave expressions alone."""
    result = {"schema": 1, "dialogues": {}}
    with ZipFile(jar) as z:
        for name,path in NATIVE.items():
            data=json.loads(z.read(path))
            pages={}
            for page in data["pages"]:
                texts={str(i):x for i,x in enumerate(page.get("lines",[]))
                       if isinstance(x,str)}
                options={}
                for option in page.get("input",{}).get("options",[]):
                    if isinstance(option.get("text"),str):
                        options[option["value"]] = option["text"]
                if texts or options:
                    pages[page["id"]]={"lines":texts,"choices":options}
            result["dialogues"][name]={"pages":pages}
    return result

def apply(jar, edited):
    if not isinstance(edited,dict) or set(edited)!={"schema","dialogues"} or edited["schema"]!=1:
        raise ValueError("Unexpected dialogue text schema")
    if not isinstance(edited["dialogues"],dict) or (
        set(edited["dialogues"]) - set(NATIVE)):
        raise ValueError("Unknown native NPC dialogue")
    output={}
    with ZipFile(jar) as z:
        for id, overrides in edited["dialogues"].items():
            path=NATIVE[id]
            original=json.loads(z.read(path))
            if not isinstance(overrides,dict) or set(overrides)!={"pages"} or not isinstance(overrides["pages"],dict):
                raise ValueError("Bad NPC pages for "+id)
            pages={p["id"]:p for p in original["pages"]}
            for page_id, edits in overrides["pages"].items():
                if page_id not in pages or not isinstance(edits,dict) or set(edits)!={"lines","choices"}:
                    raise ValueError("Unrecognized dialogue page "+id+"/"+page_id)
                page=pages[page_id]
                texts=page.get("lines",[])
                if not isinstance(edits["lines"],dict) or not isinstance(edits["choices"],dict):
                    raise ValueError("Page text and choices must be JSON objects")
                for number,txt in edits["lines"].items():
                    if not isinstance(number,str) or not number.isdecimal() or str(int(number))!=number:
                        raise ValueError("Line indexes must be canonical non-negative integers")
                    index=int(number)
                    if index>=len(texts) or not isinstance(texts[index],str):
                        raise ValueError("Cannot override dynamic Cobblemon expression "+id+"/"+page_id)
                    texts[index]=string(txt)
                options={c["value"]:c for c in page.get("input",{}).get("options",[])}
                for option_id,txt in edits["choices"].items():
                    if option_id not in options or not isinstance(options[option_id].get("text"),str):
                        raise ValueError("Cannot override unknown/dynamic native NPC choice")
                    options[option_id]["text"]=string(txt)
            # Semantic check: redact only text fields, and the result must be
            # byte-for-byte equivalent to the original after redaction.
            def strip_text(npc):
                for p in npc["pages"]:
                    if "lines" in p:
                        p["lines"]=[("<TEXT>" if isinstance(v,str) else v) for v in p["lines"]]
                    for opt in p.get("input",{}).get("options",[]):
                        if isinstance(opt.get("text"),str):
                            opt["text"]="<CHOICE>"
                return npc
            import copy
            untouched=json.loads(z.read(path))
            if strip_text(copy.deepcopy(untouched)) != strip_text(copy.deepcopy(original)):
                raise ValueError("Actions, visibility or speaker data changed: "+id)
            output[path]=encode(original)
    return output

def main():
    parser=argparse.ArgumentParser()
    parser.add_argument("--jar",type=Path,required=True)
    parser.add_argument("--config",type=Path,required=True)
    parser.add_argument("--out",type=Path,required=True)
    args=parser.parse_args()
    if args.config.is_symlink(): raise ValueError("Refusing symlinked native dialogue config")
    if not args.config.exists():
        args.config.parent.mkdir(parents=True,exist_ok=True)
        args.config.write_bytes(encode(template(args.jar)))
        print("Created editable native dialogue text config:",args.config)
    if args.config.stat().st_size > 131072:
        raise ValueError("Native dialogue config is too large")
    modified=apply(args.jar,json.loads(args.config.read_text(encoding="utf8")))
    if not modified: raise ValueError("No NPC dialogues requested")
    args.out.parent.mkdir(parents=True,exist_ok=True)
    with ZipFile(args.out,"w",ZIP_DEFLATED) as z:
        z.writestr("pack.mcmeta",encode({
            "pack":{"pack_format":48,"description":"Pride Core native NPC dialogue TEXT ONLY override"}
        }))
        for name,content in sorted(modified.items()):
            z.writestr(name,content)
    with ZipFile(args.out) as z:
        if z.testzip() is not None: raise ValueError("Corrupted NPC text override output")
    print("PASS: native dialogue text-only override created, unmodified native actions protected")
    print("SHA256:",hashlib.sha256(args.out.read_bytes()).hexdigest())

if __name__=="__main__":
    main()
