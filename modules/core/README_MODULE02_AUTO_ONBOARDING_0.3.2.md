# Pride Core Module 02 — v0.3.2-rc1 Automatic Native Onboarding (STAGING)

**Purpose:** eliminate the temporary "Continue my Pride Region story" option from the three existing Lab NPC dialogues. Existing players and new players continue using original Cobblemon NPC GUI and rewards.

## Included
- Cumulative `pride-core-0.3.2-rc1.jar` containing Module 01 + Module 02.
- `PrideRegion_Story_Native_NPCs_0.3.2-rc1_STAGING.zip`, built **from the exact user-supplied League 7.10.10 ZIP** with three narrowly targeted dialogue overrides.
- Automatic read-only onboarding reconciliation after a successful Lab action and on player JOIN.
- Existing 27 chapter checkpoints mirrored into reward-free vanilla advancements; FTB Quests Per Player may use these as individual quest tasks.
- Existing custom `config/pride_core/story.json` remains intact under the tested additive migration policy. `story_enabled` remains unchanged.

## Native NPC dialogue patch (no extra test button)

| Original NPC | Original GUI choice | Injection order | Milestone consumed |
|---|---|---|---|
| Professor Sequoia | `starter_done/theo` | after normal Cobblemon data/tag writes | `sequoia_met` AND `starter_chosen` |
| Technician Theo | `main/register` | after the existing trainer-card item/service function | `trainer_card_received` |
| Avery | `kit/take_kit` | after the existing one-time kit service | `trainer_guide_received` |

The full original dialogue options, `isVisible` conditions, service functions, and action order are preserved; only one `q.run_command('execute as ' + q.player.username + ' at @s run pridecore story sync')` call is appended. **No original rewards are replaced or repeated.** There is no need to add `pride_npc_*` tags to these three NPCs anymore. New Pride Story NPCs spawned through `pride_story:spawn/*` keep their separate, tagged story interaction identities.

On rejoin, progression facts may be reconciled **only** when the profile already contains `pride:met_mom` and each prior milestone has been verified. The reconciliation does not grant any missing starter, item, FTB quest or League badge, and it never writes legacy tags. A player missing the required markers remains at their previous checkpoint.

## Installation (staging only)

1. Make a full server and `config/pride_core` backup. Record `/pridecore story status`.
2. Stop the staging server. In `mods/`, remove **only** the previous `pride-core-0.3.1-rc1.jar`; insert **only** `pride-core-0.3.2-rc1.jar`. Do not install both.
3. In the staging world's `datapacks/`, **remove/disable** the old `PrideRegion_Story_Native_NPCs_0.3.1-rc1_STAGING.zip`, then add `PrideRegion_Story_Native_NPCs_0.3.2-rc1_STAGING.zip`. Keep the League 7.10.10 pack and all LegendaryQuests datapacks.
4. After starting staging, run `/datapack list enabled`. Ensure only one `PrideRegion_Story_Native_NPCs` version is enabled and it comes **after** the League pack. If needed: `/datapack enable "file/PrideRegion_Story_Native_NPCs_0.3.2-rc1_STAGING.zip" last`. Use the precise returned identifier if renamed.
5. Check `/pridecore status`, `/pridecore story status`, `/pridecore story validate`.
6. For a **fresh non-OP staging tester**, talk normally to Mom, choose starter with Sequoia, select Sequoia's normal next-to-Theo option, click Theo's **Register**, and accept Avery's **Field Kit**. Do **not** use any artificial "Continue my Pride Region story" button or `/tag` command. Verify after each step: `/pridecore story status`.
7. Check non-OP permissions, relog persistence, repeat dialogs do not give duplicate starter/cards/kit, original FTB Per Player onboarding tasks, Pokémon party/PC and Impactor balance.
8. With a returning player who has pre-existing legacy milestones, log in and check that the story reconciles only previously verified checkpoints without rewards or wipes. A legacy veteran who never completed Mom in Pride Core is **not** silently started.
9. Test failure cases: no starter yet, no trainer card yet, no kit yet. Story must not advance until the real action completes.
10. Leave the new 24 narrative NPC placement and all Gym/LegendaryQuests functions unchanged until further content QA.

## Observed source/QA

- Original League 7.10.10 data were inspected offline.
- Exact overlay was generated using that ZIP; the three modified dialogues were diff-checked against their originals. Each retained every original choice and action, appending **exactly one** sync action in the designated successful path.
- GitHub Actions: [v0.3.2-rc1 build](https://github.com/Breathinglions/Pridecore/actions/runs/40270883588) passed 41 Foundation tests, 53 dialogue tests, 19 config migration tests, Fabric compilation and story pack ZIP checks.
- **Not yet verified in Minecraft:** NPC `q.run_command` invoking the newly added sync immediately after the reward action as a normal non-OP player; restarting/server FTB compatibility; all-world quest and boss content.

## Scope exclusions

Still pending before calling Module 02 *complete*: authored and installed FTB Quests Per Player chapter SNBT, species/battle/location/puzzle event adapters, a genuine first-run playthrough, final character models/placement, all Acts I–V encounters/world set pieces, and endgame/League handoff regression. This RC fixes onboarding UX and compatibility only.

**Rollback:** stop staging, remove the 0.3.2 JAR/story overlay, restore the 0.3.1 pair and previous config/world backup if needed. Do not edit player facts or delete League tags.
