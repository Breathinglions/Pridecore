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
      "model": "cobblemon:standard.geo",
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
model/poser/resource resolver warnings. The PNG must match the UV layout of
the actual NPC model; `model` and `poser` fields are editable if your
model uses different bones.

### Real limitations

- This implementation is an **asset + NPC-class overlay generator**, not a
live network PNG streamer. `/pridecore npc skins reload` does not silently
replace an already installed forced resource pack or live client assets.
- Its default `cobblemon:standard.geo` / `cobblemon:standard` template
is intended for the standard Cobblemon NPC player shape. Validate the exact
UV/model behavior with your current Cobblemon 1.8.1 build before rollout.
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
