# Module 02 — World Evidence and Actor Placement (STAGING / for world builders)

The nine new investigations are ACTUAL server-side player-owned evidence interactions. Builders decide coordinates in the finished Pride Region world; **Pride Core never invents or teleports players to locations**. World markers are intentionally invisible interaction entities over visible decor built by the world team.

## Place 9 investigation props

At each finished clue prop's exact center, an operator can issue the relevant command. The value after `Tags:` is the server-validated marker; do not change names. Each marker is **world-local and visible as an interactable area, not a player inventory item**. It only gives a one-time owned fact when the player has finished preceding chapter dialogue and (if required) the official Gym.

| Clue id | World anchor to build | Must have finished | Tag |
| --- | --- | --- | --- |
| forest | Verdant Forest glowing residue and Ranger survey | Warden Elowen; Grass Gym | `pride_clue_forest` |
| mine | Rock Mine missing survey page | Miner Lyra; Rock Gym | `pride_clue_mine` |
| foundry | Fire Foundry anomalous heat gauge | Technician Sol; Fire Gym | `pride_clue_foundry` |
| river | Tidal waterway pulse recording | Keeper Neris; Water Gym | `pride_clue_river` |
| grid | Vega's hidden auxiliary circuit | Engineer Quill; Electric Gym | `pride_clue_grid` |
| ward | Dragon ruins ancient Resonance seal | Historian Toren; Dragon Gym | `pride_clue_ward` |
| ice | Frozen Research Station hidden safety report | Archivist Sera; Ice Gym | `pride_clue_ice` |
| echo | Ghost District abandoned lab memory trace | Medium Ori; Ghost Gym | `pride_clue_echo` |
| engine | Meridian's Resonance Engine diagnostic station | Director Cael confronted | `pride_clue_engine` |

Example — operator stands in the right location and places a target area:

```mcfunction
/summon minecraft:interaction ~ ~ ~ {width:1.4f,height:1.4f,response:1b,Tags:["pride_clue_forest"]}
```

For other sites, change only `pride_clue_forest` to the right tag. Verify the entity is centered on the *visible* object and can be clicked by a non-OP player with the intended interaction. The entity is invisible, so builders should add a sign, amethyst model, particles, or appropriate prop separately. Don't use `/kill @e[type=minecraft:interaction]` globally; this could destroy other mods' or builders' interactions. On staging, select one specific marker tag when repairing a placement.

## Player testing

- `/pridecore story evidence` shows the player's 0/9–9/9 list without mutation.
- Right-click the installed clue marker after the required preceding character dialogue and League win. The player gets the first-discovery message and `pride:clue_<id>` in their Pride-owned profile.
- Their per-player vanilla `pride_story:clue_<id>` advancement is granted automatically. FTB narrative quests can include that advancement task when migrated. No currency, badges, starter Pokémon or League tags are awarded by clues.
- Repeat the click: no new fact, no reward, no revision.
- A second player in the same place receives **only their own** discovery if they meet the prerequisites.
- Explore without the prerequisite and confirm refusal without any progress mutation.

## Existing players and FTB Quest Book

The FTB quest generator adds nine evidence tasks to the **same 27 story quest IDs**. It does not migrate your already installed and customized quest chapter SNBT in place. Before deployment, back up `config/ftbquests/quests/`, compare the generated six chapters against the customized Quest Book, and merge the new task objects by stable ID. Do not overwrite custom labels, quest positions or data.snbt. Original Gym 1–8, E4 and Champion FTB quests are owned by the old League system and must remain unchanged.

## Single-character world placement

- **Thorn:** use the original working RCT Gym Thorn only. The generated story datapack no longer includes the second `pride_story:thorn` NPC. Older worlds may still contain one manually spawned Story Thorn; after backups, locate and remove **only that exact old world entity**. Preserve the RCT leader.
- **Rin:** `pride_story:rin` is the only canonical persistent Rin class. Canyon/summit are **dialogue checkpoints** on that actor, not additional spawned Rin classes. Future scene-based actor movement remains a separate gameplay implementation blocker.
- **Captain Rowan:** use `pride_story:ranger_rowan` for field case and finale briefing. The `ranger_epilogue` action is included in the same NPC's dialogue. Route 1 **Ranger Mira** is a separate character.
- **Professor Sequoia, Technician Theo, Avery:** keep existing original service NPCs, not duplicates. Sequoia's two original starter paths are preserved and include Rin's introduction.

## Release blocker

This is a *world preparation / gameplay checkpoint reference*. It is **NOT a statement that the new region's buildings, clickable clue anchors, rival battles, private animated Rin entrance, or final boss fights exist in Minecraft yet**. Do not promote the module based on generated assets alone.
