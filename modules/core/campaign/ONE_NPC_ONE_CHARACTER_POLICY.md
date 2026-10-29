# Module 02 — One Character / One NPC Contract

Status: **required design and release gate**, not yet fully implemented or game-tested.

## User-facing requirement

A player interacts with **one physical NPC instance per character at a time**, not separate 'Story Thorn' and 'League Thorn' actors and not a separate "talk to story version" checkpoint.

- **Thorn and other 12 League leaders:** keep original working RCT battle and reward definitions; attach story dialogue and quest state to **that same character/entity**. No parallel TBCS reward authority. Reuse first-win tags and original FTB IDs, team files, badges, Impactor, and Hall of Fame.
- **Sequoia / Theo / Avery:** use each existing native service NPC. Do not spawn a second story NPC alongside it.
- **Rin:** one canonical rival NPC identity across all story chapters. The Lab, canyon, and summit are **scenes/checkpoints of the same Rin**, not three simultaneous 'Rin' NPC copies. Change the character's world position, dialogue and encounter at each stage; never spawn one copy for each scene.
- **Other recurring characters:** apply the same policy (e.g., Captain Rowan, Sable, Cael). Keep NPC scripts and story definitions separate from in-world entity duplication.

## Mandatory rival / starter sequence (v0.4.2 staged integration)

1. Mom sends player to Professor Sequoia.
2. At the existing Sequoia NPC, choosing 'I'm ready to choose my first Pokémon' first shows the **Rin introduction in the Lab**. Both starter UI entry paths display this introduction.
3. The very next button opens the **unchanged Cobblemon native starter screen**. No custom starter grant, no special starter command, no extra 'rival' interaction.
4. Once real starter selection results in verified `starter_chosen`, and `sequoia_met` + Pride-owned Mom/Professor facts are present, on login or `/pridecore story sync` Pride Core marks `pride:met_rin` once. It does not alter the mainline stage, award items, or write legacy tags.
5. Next mainline objective is still **Theo registration**. The story engine/default story now requires `pride:met_rin` before Ranger Mira's orientation; old story.json files are safely backed up and migrated. Older players with the same genuine starter/Professor proof automatically catch up without rerunning the starter UI.
6. Later Rin dialogue is a follow-up and may eventually lead to a **proper rival battle** (not yet developed). It can never reset the player's current Gym or Act stage.
7. If the League has already verified Thorn's Grass victory and a player was previously left at the erroneous `pride:rival_introduced` stage, the story automatically advances to Verdant victory when `pride:met_thorn`, `league_started`, and `league_grass` are all verified.

## Accepted / still outstanding

- [x] Code and automated tests for verified starter-linked rival checkpoint, idempotent reconciliation, and mainline stage safety.
- [x] Source generator for Sequoia's in-dialogue Rin introduction that preserves both original `openstarterscreen` actions.
- [x] Additive backup-based migration for Ranger Mira's new prerequisite.
- [x] Unified **v0.4.2 staging-only** JAR packages original League + Story resources.
- [ ] Game test actual Professor native Cobblemon GUI and starter selection (two entry paths, non-OP and relog).
- [ ] Integrate Rin's actual in-world model/presence at the Lab and stage-based movement. Staged intro is currently delivered as Professor dialogue text (the speaker labels quote Rin), not a cinematic NPC entrance.
- [ ] Move the Rin FTB task to the Prologue / starter stage without replacing quest IDs, losing completed quests, or creating duplicate quests; the current Quest Book layout remains unchanged.
- [ ] Remove duplicate Thorn story actor, integrate story into original RCT actor, then apply to the other 12 League trainers.
- [ ] Design a verified rival battle and story transitions. No rival win or NPC movement claim is made today.
- [ ] Full non-OP, multiplayer, restart, rewards, Hall of Fame and rollback tests.

Do not remove the working original RCT actors or create new win callbacks until one-actor integration is tested. Bundling resource files is not the same as merging active NPC entities.
