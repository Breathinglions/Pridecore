# Module 02 — Native Cobblemon NPC Datapack Audit (7.10.10)

**Input:** user-supplied `PokemonLeague7.10.10_FTB_Only_CobbleBadges_Removed_STAGING (1).zip`, inspected 2026-10-25.  
**Status:** actual pack structure verified offline; NOT confirmation this ZIP is currently loaded in the server. Test `/datapack list enabled` and compare installed pack.  
**Action:** Documentation only; **no modification** to datapack, Pride Core code, existing NPCs or persisted player data.

## Inventory and runtime

- ZIP intact: 209 entries, ~0.19 MB unpacked, no CRC errors, root `pack.mcmeta`.
- Exactly 24 `data/pride_region/npcs/*.json` + 24 `data/pride_region/dialogues/*.json`.
- 85 `data/prideregion/function/**/*.mcfunction` functions.
- 22 `advancement` JSON entries.
- RCT trainer, trainer loot table, series config content exists, including Thorn, all gyms, Elite Four and Champion.
- **NPC mechanism:** native Cobblemon NPC class JSON `interaction: {type: dialogue, dialogue: pride_region:<id>}`. Dialogue JSON defines NPC/player faces, pages, selectable options, `isVisible`, MoLang `q.player.*`, and actions.
- Native npc class sample `data/pride_region/npcs/professor_sequoia.json` uses `resourceIdentifier: prideregion:professor_sequoia`, `dialogue: pride_region:professor_sequoia`, `cobblemon:chats` and `cobblemon:stationary`.
- `Mom` has **no class/dialogue in this pack**. Module 02's Mom is a test-story-only character until added as a new *native* class/dialogue.
- Existing `researcher_mira` != newly proposed `ranger` / `Ranger Mira`. Preserve existing character and avoid a name/entity collision.

## Proven existing interaction/authority map

| Actor | Existing native NPC dialogue file | Current gate/action | Required Pride Core behavior |
|---|---|---|---|
| Professor Sequoia | `data/pride_region/dialogues/professor_sequoia.json` | `q.player.has_chosen_starter()` controls starter options; `openstarterscreen`, `prideregion:onboarding/meet_sequoia`, MoLang `q.player.data()` and command tags | Keep native starter screen and starter ownership in Cobblemon. Story flag must not imply starter was chosen. |
| Technician Theo | `.../technician_theo.json` | Checks native starter, `pride_professor_intro_complete`, `pride_trainer_card_received`; calls `prideregion:onboarding/trainer_card` | Keep one-time `rctmod:trainer_card` and `knowlogy:knowlogy_book` issuance; update story checkpoint only after effective registration success. |
| Avery | `.../avery.json` | Checks starter, registration, per-NPC `kit_received`, `pride_avery_kit_received`; invokes `prideregion:onboarding/trainer_guide` | Keep 10 `cobblemon:poke_ball`, 8 `minecraft:bread` one-time semantics; don't trigger duplicate kit or reset catch/battle progress. |
| Existing Researcher Mira | `.../researcher_mira.json` | Existing research NPC dialogue | Do not replace with proposed Ranger Mira without new class and explicit narrative decision. |
| League | `prideregion:league/*`, `rctmod` trainer content | `league_*` tags authoritative for wins, existing FTB quests present results | Keep League ownership external until Module 03 migration. Never issue badge/FTB win from a mere story conversation. |

## Important code-level facts

1. Sequoia dialogue marks `pride_professor_intro_complete` when the player reports having a starter; starter selection invokes native `openstarterscreen`.
2. Theo `trainer_card.mcfunction` checks tag `starter_chosen` before issuance and records one-time `trainer_card_received`, `pokepedia_received`, corresponding advancement/FTB Quest; do not duplicate those.
3. Avery `trainer_guide.mcfunction` checks `trainer_card_received`; gives 10 Poké Balls and 8 bread conditionally, adds `trainer_guide_received`, `field_kit_received`, `pride_avery_kit`; warns and resets `first_catch` / `first_battle` *projection* if their matching authority tag is absent. Verify catch/battle regression on the actual integration.
4. Onboarding `sync_player.mcfunction` projects legacy tag truth into FTB Quests and advancements. It may infer `starter_chosen` from Cobblemon's `cobblemon:root` advancement. A native NPC GUI marker, command tag, FTB Quest completion and Pride-owned `pride:` fact **are not interchangeable**.
5. League `grass_win.mcfunction` gates on `league_started` and issues a one-time advancement, rewards, sound, and `league_grass` tag. `league/sync_player.mcfunction` projects `league_*` tags into 13 known FTB quest IDs. This pack's README explicitly designates `league_*` tags as persisted League authority despite the new FTB icon-only badge presentation.
6. 7.10.10 removed CobbleBadges physical badge registry/grants but left Gym battle/reward/FTB progress logic, native dialogues and onboarding as they were.

## Integration plan for a *future* 0.2.x Module 02 release

- **Do not** overwrite NPC JSON or retired commands in this uploaded pack while live. Duplicate folder datapack priorities must be audited first.
- Add a native NPC for Mom, then use existing Professor Sequoia, Theo, Avery and service NPC classes. Preserve skins, placement, `q.npc.face()`, `q.player.face()`, `isVisible` and pages.
- Introduce a safe server-side function/API bridge that checks actual Cobblemon/legacy prerequisites and updates Pride-owned story stages only **after** the external action is confirmed. Avoid passing player names as unrestricted command strings; ensure invoked context is the actual authenticated player.
- Add versioned NPC JSON dialogue integration via a staging datapack overlay, or native adapter if an actual compatible Cobblemon 1.8.1 event API is verified. Keep the replacement reversible and avoid double interaction handlers.
- Implement read-only `LegacyOnboardingView`: `has_chosen_starter`, `starter_chosen`, `sequoia_met`, `trainer_card_received`, `trainer_guide_received`, `first_catch`, `first_battle_win`, `league_started`, plus native variable context where accessible. Never silently import these tags into Pride-owned facts.
- Correct Module 02 prototype limitations: `sequoia` test dialogue currently advances without verifying starter; `theo` test conversation can progress without actually issuing registration items; `avery` test dialogue can progress without kit. These are acceptable **for the testing harness only**, not production acceptance.
- Define and run backward-compatibility tests for old players, fresh players, two simultaneous players, disconnect/restart, invalid NBT/player vars, NPC player-specific per-entity flags, idempotent starter/kit claims and all 13 League/FTB Quest milestones.
- The user should test Module 01/02 RC independently; integration is staged only after the current profile test and original NPC test both pass.

## Minimal verification that *this* pack is installed

1. Run `/datapack list enabled` and compare the League pack title/version. Minecraft may display `file/<zip basename>` and the server's actual file may have been renamed.
2. In the staging world's `datapacks` directory check only one active `PokemonLeague7.10.x` ZIP/extract.
3. Use `/spawnnpc pride_region:professor_sequoia 1` only in staging if needed, and confirm the starter/dialogue GUI **without selecting or claiming another starter**.
4. Inspect datapack precedence if another native Lab or NPC pack also declares `pride_region:professor_sequoia`; the highest-priority definition will win and may not be the one from this archive.

**Do not promote Module 02 on the strength of the in-chat test NPC alone.** The final module must pass native Cobblemon GUI and real progression-integration acceptance tests.
