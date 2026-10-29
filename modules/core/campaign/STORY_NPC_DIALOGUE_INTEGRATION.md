# Module 02 — physical NPC dialogue integration (staging)

The narrative now opens directly from **tagged Cobblemon NPCs**, not only
temporary demonstration villagers. In staging, a builder must attach the
corresponding authored `pride_npc_*` marker to the specific `cobblemon:npc`
entity. Untagged NPCs are not intercepted. All 27 authored dialogue identities
are available; story-only actors display clickable chat choices in the player's
current language of the story rather than requiring typed commands.

## Important native service boundaries

Professor Sequoia, Technician Theo and Avery must continue to use their
existing Cobblemon native interactions for real starter selection, Trainer
Card and Field Kit services. Thorn remains under his official Gym/League
interaction and battle handlers. Pride Core does **not** substitute dialogue
for those original gameplay services. Their native `cobblemon:npc` interactions
are explicitly passed through, and player progression is reconciled from
the existing, verified legacy tags.

All other authored narrative-only actors can show the story conversation on
physical right-click when Module 02 is enabled. Dialogue never awards a
Pokémon, Gym badge, or Pokémon-battle victory. Every choice still passes
server-owned stage, fact and existing legacy prerequisites.

## Recurring actors

- `pride_npc_rin` can speak as early Rin, Canyon Rin or Summit Rin. The game
  selects the latest chapter whose saved story prerequisites are met, preferring
  an unfinished chapter over replaying a previously completed one.
- `pride_npc_ranger_rowan` can speak as Rowan during the river investigation
  or during the Resonance Engine epilogue. It uses the same eligibility rule.
- A locked chapter is never opened just because the player can physically
  see a tagged NPC.

## Security and interaction details

The story conversation is associated with the **exact physical NPC UUID,
marker and dimension**. Clicking a dialogue option is handled by the
player-owned server command only while they remain within six blocks of that
entity. A second NPC with the same tag cannot impersonate the first. Moving
away, changing dimension or losing the entity ends the active conversation.
Player disconnects, command cancellation, story reloads and operator session
resets clear pending NPC anchors.

NPC text and choices are displayed in clickable chat lines; players do not
need to type a command. Current dialogue UI uses vanilla chat and is not a
custom GUI or client-only phone interface yet.

## Staging checks

1. Use `/pridecore story admin bind npc <id> <entity>` to tag a real
   `cobblemon:npc`, not a villager, then audit its marker using
   `/pridecore story admin audit-markers`.
2. With Module 02 enabled **only** on the isolated staging world, confirm
   right-click opens an eligible narrative-only character.
3. Confirm that Sequoia, Theo, Avery and Thorn preserve native services.
4. Confirm the next Rin/Rowan chapter appears only after prerequisite facts
   and that previously completed chapters remain safe to revisit.
5. Try selecting an old choice after walking away, switching dimensions, or
   opening a different NPC; the old conversation must not advance progress.
6. Verify clickable choices on the exact supported client pack in game.

A successful build verifies the code path and tests, not real mod event ordering
or physical world click behavior. Production remains disabled until staging.
