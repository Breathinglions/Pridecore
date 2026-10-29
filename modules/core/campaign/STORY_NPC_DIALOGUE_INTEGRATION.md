# Module 02 — physical NPC dialogue integration (staging)

The narrative opens from the authored **native Cobblemon dialogue overlay** for each
story NPC (e.g. `pride_story:mom`). Each character must have the matching
Cobblemon NPC type (`pride_story:mom`, etc.) and its `pride_npc_*` marker.
The original native Sequoia/Theo/Avery services and all RCT Gym/League NPCs keep
their respective native interactions. The old clickable-chat prototype remains
only for explicitly marked test villagers; it is not the intended player UI.

## Important native service boundaries

Professor Sequoia, Technician Theo and Avery must continue to use their
existing Cobblemon native interactions for real starter selection, Trainer
Card and Field Kit services. Thorn remains under his official Gym/League
interaction and battle handlers. Pride Core does **not** substitute dialogue
for those original gameplay services. Their native `cobblemon:npc` interactions
are explicitly passed through, and player progression is reconciled from
the existing, verified legacy tags.

All other authored narrative-only Cobblemon NPC types contain an actual native
`interaction: {type: dialogue, dialogue: pride_story:<id>}` resource in the unified
server JAR. Right-click is passed to Cobblemon rather than consumed by a custom
chat transcript. Their choices call `pridecore story native <id> <choice>`, which
checks the physical matching NPC, allowed world and player progression before
recording the next story checkpoint. Dialogue never awards a Pokémon, Gym badge,
or Pokémon-battle victory.

## Recurring actors

- `pride_npc_rin` can speak as early Rin, Canyon Rin or Summit Rin. The game
  selects the latest chapter whose saved story prerequisites are met, preferring
  an unfinished chapter over replaying a previously completed one.
- `pride_npc_ranger_rowan` can speak as Rowan during the river investigation
  or during the Resonance Engine epilogue. It uses the same eligibility rule.
- A locked chapter is never opened just because the player can physically
  see a tagged NPC.

## Security and interaction details

Native Cobblemon NPC choices revalidate the **matching NPC tag, current world,
player identity, six-block proximity and stage prerequisites** on the server.
The separate old prototype *chat* path remains bound to an exact entity UUID
for tagged test villagers; no prototype chat anchor is created for normal native
Cobblemon characters. Player disconnects, story reloads and operator resets
clear temporary sessions. Actual Cobblemon UI open/close behavior remains owned
by Cobblemon.

**Expected UI:** native Cobblemon overlay (including Mom and story characters).
If an NPC still opens chat, inspect whether it is an old test villager or a
legacy/custom Cobblemon NPC that was given a `pride_npc_*` tag without the actual
matching `pride_story:<id>` NPC definition. Merely tagging another type of NPC
does not replace its underlying native dialogue configuration. Do not spawn
a second copy next to the original; repair or replace only the confirmed test
NPC in an isolated staging world after backup.

## Staging checks

1. Use `/pridecore story admin bind npc <id> <entity>` to tag a real
   `cobblemon:npc`, not a villager, then audit its marker using
   `/pridecore story admin audit-markers`.
2. With Module 02 enabled **only** on the isolated staging world, confirm
   right-clicking `pride_story:mom` opens the native Cobblemon dialogue overlay,
   not chat, and that the option advances `pride:visit_sequoia` once.
3. Confirm that Sequoia, Theo, Avery and Thorn preserve native services.
4. Confirm the next Rin/Rowan chapter appears only after prerequisite facts
   and that previously completed chapters remain safe to revisit.
5. Try selecting an old choice after walking away, switching dimensions, or
   opening a different NPC; the old conversation must not advance progress.
6. Verify clickable choices on the exact supported client pack in game.

A successful build verifies the code path and tests, not real mod event ordering
or physical world click behavior. Production remains disabled until staging.
