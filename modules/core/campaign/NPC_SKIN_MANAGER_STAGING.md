# PrideCore NPC Skin Manager — Staging v1 (Fabric 1.21.1)

**Goal:** Upload 64×64 Minecraft player-skin PNGs to the dedicated server
and regenerate *appearance assets* without recompiling PrideCore, changing
NPC quest scripts or resetting players.

## Server directory

After starting the updated unified PrideCore JAR once:

```text
config/pride_core/npc_skins/
├── skins.json
├── ranger.png                      ← YOU upload this PNG
└── generated/                     ← written by /pridecore npc skins reload
    ├── PrideNPCSkins_Resources.zip    client assets
    └── PrideNPCSkins_NPCOverrides.zip world datapack
```

`skins.json` (generated on first boot; existing changes are never overwritten):

```json
{
  "schema": 1,
  "skins": {
    "ranger": {
      "file": "ranger.png",
      "model": "pride_skins:player_64.geo",
      "poser": "cobblemon:standard"
    }
  },
  "npcClasses": {
    "pride_region:resource_ranger": "ranger",
    "pride_story:ranger": "ranger"
  }
}
```

To add an NPC class, add a skin and a mapping. Use the **exact namespace
and class ID of a known PrideCore NPC**, such as `pride_story:rin`.
Do not map `cobblemon:standard` globally: that would recolor unrelated
NPCs. By default Sequoia, Rin, and all other existing characters are NOT
modified. All LegendayQuests ranger NPC classes are separate and NOT
implicitly modified by these mappings.

## Reload and deploy

1. Upload the supplied `ranger.png` into the config directory.
2. In game, OP level 4:

```mcfunction
/pridecore npc skins status
/pridecore npc skins reload
```

3. The command validates PNG signatures, dimensions and schema; reads original
NPC class JSON from the running unified PrideCore JAR; changes ONLY the
appearance `resourceIdentifier` in the exported override; and produces
two ZIPs. A failed validation leaves prior exported ZIPs intact.
4. Copy `PrideNPCSkins_NPCOverrides.zip` to the staging world's
`datapacks/` directory, enable at a higher priority than the bundled
PrideCore/League datapack, then run `/reload`.
5. The **resources ZIP must reach every player's Minecraft client**. Merge
`PrideNPCSkins_Resources.zip` into your already-forced server resource pack,
re-publish the combined ZIP at your existing HTTPS pack URL, update its
SHA-1 in `server.properties`, and have clients reload/rejoin. An empty
URL, or simply putting an `assets/` ZIP on the dedicated server, will
not deliver textures to clients.
6. Newly spawned NPCs should pick up the new appearance resource identifier.
**Back up and test before replacing existing persistent NPCs**. They may
retain their cached old class appearance until safely re-created by your
existing NPC spawn workflow. Do not mass-delete story NPCs.
7. Confirm appearance in-game and check `latest.log` for Cobblemon
model/poser/resource resolver warnings. Use `pride_skins:player_64.geo` for 64×64 Minecraft skins. The exported resources ZIP bundles matching geometry and the stock NPC poser uses compatible bone names. Visual verification of each skin remains required.

### Real limitations

- This implementation is an **asset + NPC-class overlay generator**, not a
live network PNG streamer. `/pridecore npc skins reload` does not silently
replace an already installed forced resource pack or live client assets.
- Its default model is `pride_skins:player_64.geo`, built around the 64×64 Minecraft player UV and preserving Cobblemon trainer poser bones. Previous `cobblemon:trainer.geo` is a 64×128 geometry and MUST NOT be used with unconverted 64×64 skins. Existing `skins.json` files must be updated manually to this model; the generator will refuse the known incompatible models to prevent invisible or reversed textures. Validate visually in Cobblemon 1.8.1.
- It changes two PrideCore NPC **classes**, not every independently configured
LegendaryQuests Ranger automatically. LegendaryQuests classes require their
own compatible resourceIdentifier or provider-native skin setting.
- NPC names, dialogue, quest triggers, RCT battle rosters, authorizations
and physical positions are left untouched.

## Security and rollback

The command is OP4, reads regular non-symlink files, limits input size,
validates 64×64 PNG and namespaces, and never executes arbitrary config
commands. It generates new ZIPs atomically rather than replacing worlds or
mods. Back up server and world; rollback by removing the generated skin
datapack from `world/datapacks/`, restoring your previous resource pack,
and reloading/rejoining. Player progress remains unchanged.

## Broken Ranger back/front geometry recovery (2026-11-01)

If Mira looks partially invisible or the back texture renders on the front,
this is caused by our earlier model selection, which paired 64×64 skins with
Cobblemon's 64×128 `cobblemon:trainer.geo` (and sometimes a nonexistent
`cobblemon:standard.geo`). Use the new resource pack containing
`assets/pride_skins/bedrock/npcs/models/player_64.geo.json`; switch each
mapped 64×64 character to `"model": "pride_skins:player_64.geo"`.
Run the skin export again after installing the updated module. Remove the
older Ranger ZIP from Polymer includes to avoid conflicting resolver files.
Rebuild Polymer, restart, reconnect and confirm the skin from both sides.
No NPC deletion or quest reset is needed.


## Master skin catalog + automatic Polymer deployment (November 2026)

A complete master `skins.json` can predeclare unused *skin slots* without
requiring corresponding PNGs until those skins are assigned to an NPC via
`npcClasses`. Only active, mapped skins are validated/exported. Thus it is
safe to prepare 44 currently bundled PrideCore NPC identities plus reserved
League appearance names without overwriting already-working NPCs.

After uploading/editing PNGs and `skins.json`, run
`/pridecore npc skins reload`. The updated exporter:
- Generates the two preview ZIPs in `config/pride_core/npc_skins/generated/`.
- Writes **only** the manager-owned `assets/pride_skins/` files into
  `polymer/override_assets/assets/pride_skins/`. It does not replace other
  Polymer pack assets. **One-time setup:** remove the former Pride-only ZIP(s)
  from `config/polymer/resource-pack.json` `include_zips` to prevent duplicates.
- Writes the managed NPCOverrides ZIP to the active world's `datapacks/` folder.
  Existing pack must be enabled with sufficient priority; if first installed,
  operator may need to run `/datapack enable`.
- Run `/reload` after changing NPC mappings and restart/rebuild Polymer's
  client pack at your convenient maintenance window; reconnect clients to get
  the new textures. This is **not** live client asset streaming.

**Caution:** The exporter only knows Cobblemon NPC JSON classes physically
bundled in the running unified PrideCore JAR. Existing RCT League trainers
and independently shipped LegendaryQuests NPCs are outside this scope.
Their names may be reserved as *inactive skin slots* in the master catalog,
but mapping them to a fictitious `pride_story:` class is invalid. They need a
provider-compatible integration before they can use this manager.

This sync takes effect only in the updated server JAR after replacing the old
one; the existing user-provided static resource-pack ZIP cannot by itself
perform the sync. Existing files under `polymer/override_assets/assets/pride_skins`
with matching names will be replaced by PrideCore; other namespaces are untouched.
Back up world/Polymer before deployment.
