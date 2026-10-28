#!/usr/bin/env python3
"""Reproducibly bundle original Pokemon League and generated Story into Pride Core.

Never alters existing external player data or FTB quest IDs. Requires the
original audited League archive and a fresh Story overlay generated WITH the
same archive. Fail closed on any duplicate or missing authoritative reward.
"""
import argparse, hashlib, json
from pathlib import Path
from zipfile import ZipFile, ZIP_DEFLATED

LEAGUE_SHA256 = 'ca16bbfc591519144d6dc53993f84106b546478df471bf30f913aa05791b3a3f'
LEADERS = ('thorn','rook','ashen','kai','vega','draven','freya','nyx',
           'koda','seraph','celeste','vesper','champion')
LAB = ('professor_sequoia','technician_theo','avery')
CLUES = ('forest','mine','foundry','river','grid','ward','ice','echo','engine')

def digest(blob): return hashlib.sha256(blob).hexdigest()
def zip_entries(path):
    with ZipFile(path) as z:
        if z.testzip() is not None: raise ValueError('Damaged ZIP: '+str(path))
        names = [n for n in z.namelist() if not n.endswith('/')]
        if len(names) != len(set(names)): raise ValueError('Duplicate ZIP paths')
        data = {}
        for name in names:
            if name.startswith('/') or '\\' in name or '..' in name.split('/'):
                raise ValueError('Unsafe ZIP path: '+name)
            raw = z.read(name)
            if name.endswith('.json') or name.endswith('.mcmeta'):
                json.loads(raw)
            data[name] = raw
        return data

def build(jar, league_zip, story_zip, output):
    if digest(league_zip.read_bytes()) != LEAGUE_SHA256:
        raise ValueError('League ZIP differs from audited 7.10.10; REFUSING to overwrite trainer mechanics')
    compiled = zip_entries(jar)
    league = {k:v for k,v in zip_entries(league_zip).items() if k.startswith('data/')}
    story = {k:v for k,v in zip_entries(story_zip).items() if k.startswith('data/')}
    meta = json.loads(compiled['fabric.mod.json'])
    if meta.get('id') != 'pride_core' or meta.get('version') != '0.4.3-rc1':
        raise ValueError('Expected compiled Pride Core v0.4.3-rc1 JAR, not an old repack')
    # No separate Story Thorn: victory comes from the original RCT entity.
    if 'data/pride_story/npcs/thorn.json' in story or 'data/pride_story/dialogues/thorn.json' in story:
        raise ValueError('Duplicate Pride Story Thorn NPC must not be shipped')
    if 'data/pride_story/advancement/thorn.json' not in story:
        raise ValueError('Original Thorn FTB checkpoint advancement must be retained')
    for duplicate in ('rin_canyon','rin_summit','ranger_epilogue'):
        if f'data/pride_story/npcs/{duplicate}.json' in story:
            raise ValueError('Recurring story scene must reuse one canonical NPC: '+duplicate)
        if f'data/pride_story/advancement/{duplicate}.json' not in story:
            raise ValueError('Recurring story FTB checkpoint missing: '+duplicate)
    if len([x for x in story if x.startswith('data/pride_story/npcs/')]) != 20:
        raise ValueError('Unexpected standalone Story NPC count (expected 20 canonical actors)')
    for physical in ('rin','ranger_rowan'):
        if f'data/pride_story/npcs/{physical}.json' not in story:
            raise ValueError('Missing recurring canonical actor '+physical)
    for clue in CLUES:
        if f'data/pride_story/advancement/clue_{clue}.json' not in story:
            raise ValueError('Missing world evidence advancement: '+clue)
    overlaps = set(league)&set(story)
    expected = {f'data/pride_region/dialogues/{n}.json' for n in LAB}
    if overlaps != expected: raise ValueError('Unreviewed conflicting resources: '+repr(overlaps ^ expected))
    for name in LAB:
        path = f'data/pride_region/dialogues/{name}.json'
        if 'pridecore story sync' not in story[path].decode():
            raise ValueError('Story Lab overlay lost safe post-service sync: '+name)
    professor = json.loads(story['data/pride_region/dialogues/professor_sequoia.json'])
    if not any(p.get('id')=='rin_starter_intro' for p in professor['pages']):
        raise ValueError('Professor is missing mandatory Rin introduction')
    resources={**league,**story}
    if set(compiled)&set(resources): raise ValueError('Mod source already has conflicting datapack assets')
    for name in LEADERS:
        trainer = f'data/rctmod/trainers/pride_{name}.json'
        mob = f'data/rctmod/mobs/trainers/single/pride_{name}.json'
        if trainer not in resources or mob not in resources: raise ValueError('Missing RCT: '+name)
        orig=json.loads(league[mob]); dest=json.loads(resources[mob])
        if orig != dest or not dest.get('winCommands',{}).get('1'):
            raise ValueError('Modified original win callback for '+name)
    # Preserve EVERY existing reward operation, add ONLY the harmless
    # read-only post-victory sync to the original working Thorn callback.
    thorn='data/rctmod/mobs/trainers/single/pride_thorn.json'
    trainer=json.loads(resources[thorn]);commands=trainer['winCommands']['1']
    if not any('league_grass' in line for line in commands):
        raise ValueError('Thorn original Grass badge callback missing')
    if not any('ftbquests change_progress' in line for line in commands):
        raise ValueError('Thorn original FTB callback missing')
    if not any('eco deposit' in line for line in commands):
        raise ValueError('Thorn original currency callback missing')
    if '@1 pridecore story sync' not in commands:
        commands.append('@1 pridecore story sync')
    resources[thorn] = (json.dumps(trainer,indent=2,ensure_ascii=False)+'\n').encode()
    manifest={
      'schema':1,'release':'0.4.3-rc1','staging_only':True,
      'league_original_sha256':LEAGUE_SHA256,
      'source_sha256':{'base_jar':digest(jar.read_bytes()),'story_overlay':digest(story_zip.read_bytes())},
      'bundled_files':len(resources),'authoritative_trainers':len(LEADERS),
      'investigation_clues':len(CLUES),'overridden_laboratory_dialogues':sorted(overlaps),
      'single_thorn':True,
      'original_league_payouts_unchanged':True,
      'modifications':['Original RCT Thorn first-win callback appends read-only Pride Core story sync'],
      'limitations':['Rin animated multiplayer cutscene not yet implemented',
                     'World clue interaction anchors require builder placement',
                     'FTB external SNBT updates require migration review',
                     'Story villain/rival combat encounters remain unimplemented',
                     'Existing duplicate Story Thorn entities need operator review'],
      'file_sha256':{name:digest(body) for name,body in sorted(resources.items())}
    }
    compiled['pride_core_unification_manifest.json']=(json.dumps(manifest,indent=2)+'\n').encode()
    output.parent.mkdir(parents=True,exist_ok=True)
    with ZipFile(output,'w',ZIP_DEFLATED,compresslevel=8) as z:
        for name,body in sorted({**compiled,**resources}.items()):z.writestr(name,body)
    check=zip_entries(output)
    assert len(check)==len(compiled)+len(resources)
    assert 'data/pride_story/npcs/thorn.json' not in check
    assert len([n for n in check if n.startswith('data/rctmod/trainers/pride_')])==13
    assert all(check[f'data/pride_story/advancement/clue_{c}.json'] for c in CLUES)
    print('PASS: '+str(output)+' ('+str(len(resources))+' datapack files; 13 RCT trainers, 9 clues, one Thorn)')
    print('SHA256 '+digest(output.read_bytes()))
    return manifest

if __name__=='__main__':
    p=argparse.ArgumentParser()
    p.add_argument('--jar',type=Path,required=True)
    p.add_argument('--league-zip',type=Path,required=True)
    p.add_argument('--story-zip',type=Path,required=True)
    p.add_argument('--out',type=Path,required=True)
    a=p.parse_args()
    build(a.jar,a.league_zip,a.story_zip,a.out)