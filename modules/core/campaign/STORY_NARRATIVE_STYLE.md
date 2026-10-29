# Pride Region narrative direction — story first

Module 02 is a **story campaign**, not a combat tutorial or game-system
walkthrough. The player should encounter memorable characters, mysteries, moral
choices, and consequences that unfold naturally across Hearthleaf, the League,
Team Eclipse, Meridian Institute, and the Resonance Engine.

## What players experience

- Mom, Professor Sequoia, Theo and Avery speak as people in Hearthleaf. They
  never say they are dialogue systems or warn players that a quest option does
  not issue items.
- Rin is a recurring childhood rival who grows alongside the player. Their
  meetings are part of the story; narration does not call them "scripted boss
  battles" or tell players to defeat Rin to unlock an NPC.
- Team Eclipse and Sable are written around why they distrust Meridian and what
  their actions have cost the region. Cael's story is about the consequences of
  attempting to control Resonance.
- The engine's old seals and clues reveal a mystery. The finale is presented
  through chamber changes, character reactions, and atmospheric discoveries
  rather than imperatives such as "defeat the remaining defenders".
- Cinematic beats are character thoughts, narrative stakes, and environmental
  events; no boss phase labels, move orders, attack tips, or combat tutorials.
- The quest journal may indicate where someone was last seen and what has
  changed, but must never expose internal IDs, flags, prerequisite logic,
  RCT launch details, or debugging instructions in an NPC's voice.

## Hidden implementation rules

All nine storyline trainer encounters are still **normal Pokémon battles**.
The trusted RCT/Cobblemon battle, original League badge requirements, verified
wins, player-specific story facts, and multiplayer safeguards remain entirely
behind the scenes. Nothing in the narrative rewrite grants badges, progression,
money, items, Pokémon, or battle results.

**Minecraft + Pokémon hybrid combat** remains reserved for the separately
planned Flashpoints, Operations, raids, and World Bosses. It must never appear
inside Module 02 storyline boss encounters.

## Upgrading older staged installations

When a staging install has the original *unedited* first-party NPC dialogue,
`StoryConfigManager` compares every node and choice to the exact archived
`legacy_stock_dialogue_v1.json` before applying the new story text. Only a
perfect match on an individual original text field can be upgraded. Customized
lines or choice labels are preserved. The manager validates the merged story,
creates an atomic backup, and leaves existing milestone/stage definitions
unchanged. It does not edit player profiles or production server files.

The accompanying CI migration test ensures old stock lines are replaced,
custom text remains untouched, and repeat upgrades do not rewrite content.
`test_story_narrative.py` checks the 27-NPC story graph is unchanged and
prevents explicit combat instructions or technical dialogue from resurfacing.

## Status of validation

Static tests and GitHub builds are **not** an in-game performance or content
review. The final staging playthrough will verify that conversations flow
naturally, Pokémon battles launch at the appropriate dramatic moments, and
characters react in the intended order without exposing internal mechanics.
