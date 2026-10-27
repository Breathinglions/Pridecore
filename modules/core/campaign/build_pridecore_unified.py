#!/usr/bin/env python3
"""Bundle exactly two audited Pride Region datapacks inside existing Fabric JAR.

This is a PACKAGING migration, not a reward or battle system rewrite.
For Minecraft 1.21.1 Fabric, resources under mod JAR data/ are always-on
mod data pack resources. Do not keep the separate input world datapacks active.
"""
import argparse, hashlib, json, os, sys
from collections import Counter
from pathlib import Path
from zipfile import ZipFile, ZIP_DEFLATED

SHA = {
 'jar':'dc8aa7ad6e00e4a926f251118806bf743e9a20653cc465e370ef8fda4e9b5b5f',
 'league':'ca16bbfc591519144d6dc53993f84106b546478df471bf30f913aa05791b3a3f',
 'story':'947bcf5ae76246f28f9ad9bd6225065724267f9aeb02ebbc5533a4a70a0d9632',
}
EXPECTED_OVERLAP = {
 'data/pride_region/dialogues/professor_sequoia.json',
 'data/pride_region/dialogues/technician_theo.json',
 'data/pride_region/dialogues/avery.json',
}
REQUIRED = [
 'data/prideregion/function/league/load.mcfunction',
 'data/minecraft/tags/function/load.json',
 'data/pride_story/dialogues/mom.json',
 'data/pride_story/dialogues/thorn.json',
 'data/pride_story/function/league/grass_win.mcfunction',
 'data/pride_region/dialogues/professor_sequoia.json',
 'data/pride_region/dialogues/technician_theo.json',
 'data/pride_region/dialogues/avery.json',
]
TRAINERS = ('thorn','rook','ashen','kai','vega','draven','freya','nyx',
            'koda','seraph','celeste','vesper','champion')

def sha(data):return hashlib.sha256(data).hexdigest()
def verify_file(path,key):
 b=path.read_bytes();actual=sha(b)
 if actual != SHA[key]:raise ValueError(f"Input {key} SHA mismatch; refuse unreviewed content: {actual}")
 return b

def read_data(z):
 seen={}
 for name in z.namelist():
  if not name.startswith('data/') or name.endswith('/'):
   continue
  if '\\' in name or '/..' in name or name.startswith('/') or '..' in name.split('/'):
   raise ValueError('Unsafe datapack path '+name)
  if name in seen:raise ValueError('Duplicate ZIP entry '+name)
  b=z.read(name)
  if name.endswith('.json'):
   json.loads(b.decode('utf-8'))
  seen[name]=b
 return seen

def create(jar_path,league_path,story_path,out):
 verify_file(jar_path,'jar');verify_file(league_path,'league');verify_file(story_path,'story')
 with ZipFile(jar_path) as z:
  originals={name:z.read(name) for name in z.namelist() if not name.endswith('/')}
  assert z.testzip() is None
 with ZipFile(league_path) as z:
  league=read_data(z)
  assert z.testzip() is None
 with ZipFile(story_path) as z:
  story=read_data(z)
  assert z.testzip() is None
 overlaps=set(league)&set(story)
 if overlaps!=EXPECTED_OVERLAP:raise ValueError(f'Unexpected overlaps: {overlaps ^ EXPECTED_OVERLAP}')
 if set(originals)&(set(league)|set(story)):
  raise ValueError('Already existing data in source JAR - refuse to bundle twice')
 content={**league,**story}
 missing=[n for n in REQUIRED if n not in content]
 if missing:raise ValueError('Missing required files '+str(missing))
 for id in TRAINERS:
  path='data/rctmod/trainers/pride_'+id+'.json'
  if path not in content:raise ValueError('Missing authoritative trainer '+path)
  mob_path='data/rctmod/mobs/trainers/single/pride_'+id+'.json'
  assert mob_path in content and b'winCommands' in content[mob_path], ('Missing original winCommands for '+id)
 # The story overlay retains the original service action AND adds story sync.
 for name in EXPECTED_OVERLAP:
  base=json.loads(league[name]);overlay=json.loads(story[name]);
  if base==overlay:raise ValueError('Expected one additive lab dialogue override: '+name)
  if 'pridecore story sync' not in story[name].decode():raise ValueError('No original Lab story sync '+name)
 # The old metadata describes zero bundled content and the 0.3.8 version.
 meta=json.loads(originals['fabric.mod.json'].decode())
 if meta['id']!='pride_core' or meta['version']!='0.3.8-rc1':
  raise ValueError('Unexpected mod jar ID/version; refusing rename')
 meta['version']='0.4.0-rc1'
 meta['description']='Pride Core staging consolidation: foundation, story, and bundled League/Story content. Existing RCT and FTB engines remain authoritative.'
 originals['fabric.mod.json']=(json.dumps(meta,indent=2,ensure_ascii=False)+'\n').encode()
 manifest={
  'schema':1,'release':'0.4.0-rc1','source_code_equivalent':'0.3.8-rc1',
  'mode':'unified_packaged_staging', 'deploy_with_legacy_world_datapacks':False,
  'limitations':['League-first-win still owned by RCT','FTB Quests are external and preserved','existing duplicate Pride Story Thorn NPC must be retired after in-game testing',
                 'LegendaryQuest datapacks outside these two ZIPs are not migrated'],
  'source_sha256':SHA, 'counts':{'league':len(league),'story':len(story),'collisions':len(overlaps),'bundled_data':len(content)},
  'overlay_paths':sorted(overlaps),
  'bundled_files_sha256':{name:sha(b) for name,b in sorted(content.items())},
 }
 originals['pride_core_unification_manifest.json']=(json.dumps(manifest,indent=2)+'\n').encode()
 out.parent.mkdir(parents=True,exist_ok=True)
 with ZipFile(out,'w',ZIP_DEFLATED,compresslevel=8) as z:
  for name,b in sorted(originals.items()):z.writestr(name,b)
  for name,b in sorted(content.items()):z.writestr(name,b)
 with ZipFile(out) as z:
  assert z.testzip() is None
  assert len(z.namelist())==len(set(z.namelist()))
  assert z.read('fabric.mod.json')==originals['fabric.mod.json']
  for name,b in content.items():assert z.read(name)==b
  for name,b in originals.items():assert z.read(name)==b
  assert not any(n=='pack.mcmeta' for n in z.namelist())
 print(f'PASS: unified Pride Core {meta["version"]} JAR: {len(content)} bundled data assets, '
       f'{len(TRAINERS)} retained RCT trainer definitions, {len(overlaps)} intentional dialogue overrides; SHA256={sha(out.read_bytes())}')
 print('WARNING: remove separate League and Pride Story ZIPs from staging world/datapacks before activation; no player data changes')
 return manifest

if __name__=='__main__':
 p=argparse.ArgumentParser()
 p.add_argument('--jar',type=Path,required=True)
 p.add_argument('--league',type=Path,required=True)
 p.add_argument('--story',type=Path,required=True)
 p.add_argument('--out',type=Path,required=True)
 args=p.parse_args();create(args.jar,args.league,args.story,args.out)