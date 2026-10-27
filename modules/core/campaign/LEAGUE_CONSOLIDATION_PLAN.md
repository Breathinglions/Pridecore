# Pride Region — League Trainer Consolidation Plan (13 encounters)

Status: **Inventory and design only — not deployed.** Source audited: `PokemonLeague7.10.10_FTB_Only_CobbleBadges_Removed_STAGING (2).zip`, SHA-256 `ca16bbfc591519144d6dc53993f84106b546478df471bf30f913aa05791b3a3f`.

The exact 13-trainer roster, existing win tags, prerequisites, RCT trainer/team file paths, FTB quest IDs, team sizes/levels, and first-win payouts are in [league_consolidation_manifest.json](league_consolidation_manifest.json). Treat that JSON as the acceptance baseline, **not a runtime replacement**.

## Confirmed from the original League pack

- Eight Gym RCT trainers, four Elite Four RCT trainers, and Champion Magnus are already defined under `data/rctmod/trainers/` and `data/rctmod/mobs/trainers/single/`.
- **All 13 RCT mob definitions have built-in `winCommands.1`** conditioned on the previous `league_*` tag and protected from repeat first-win payouts.
- Their existing win handlers own all 13 corresponding **original FTB quest IDs** and `league_*` tag milestones. Seven? No: all eight Gym definitions have explicit currency payouts; E4 Koda, Seraph, Celeste have no currency payout in the RCT callback; E4 Vesper pays 40,000; Champion Magnus pays 25,000. Do not invent missing payments.
- The Champion callback has additional Hall of Fame tags, broadcast, scoreboard, crest and announcement logic. These are **not optional** in a future replacement.
- The **Pride Story** Thorn is a *second* Cobblemon dialogue entity, created by `pride_story:thorn`. The existing RCT Gym Thorn awards first-win tags and rewards correctly in the user's in-game test. Pride Core currently reads `league_grass` for story advancement.
- Current TBCS commands can attach an RCT trainer to a generic entity and start a battle, but a TBCS battle does **not automatically imply that RCT's own mob `winCommands` ran**. For that reason, do not presume a successful fight awarded the official League victory. Use only a single authoritative winner callback.

## Desired architecture

1. **One visible encounter per Gym/E4/Champion.** Retain the original RCT trainer ID and Pokémon team. Unify story dialogue and battle access at the same character, with permission-safe player interaction. Prefer using the **existing RCT entity** as the visible actor since it already works. Verify first that the RCT entity can support our proposed story-interaction hook; do not automatically remove the working actor until that is demonstrated.
2. **RCT remains battle/reward authority initially.** Pride Core stays a read-only subscriber to verified legacy win tags and advancements, and provides quest direction. This avoids a second payout and avoids changing original success callbacks.
3. **Single source of player progression truth** for League wins while migrating: `league_grass`, `league_rock`, ... `league_champion`. Preserve existing tag semantics and all current FTB quest IDs. The Pride Core advancement mirror remains *supplementary*.
4. **After parity proven**, move each original win command's logic to a tested Pride Core battle-completion adapter (with server-side native Cobblemon/RCT/TBCS event verification, not arbitrary player commands). **Only then** retire the corresponding legacy callback, atomically: never leave two active payout authorities.
5. Keep trainer teams, rules, dialogue, models and balanced payouts in readable config/data resources—not 13 hard-coded Java methods. Keep multiplayer rewards, permissions, level handling, player identity and anti-replay isolated.

## Incremental validation order and scope

- **Step A: Thorn reference acceptance (blocking).** Test original RCT Thorn encounter as non-OP. Confirm `league_grass`, FTB quest `456DCDE440B68A94`, ₽1,000 exactly once, Pride Core reaches `pride:verdant_victory`, Warden Elowen opens Act I, relog and second battle do not pay twice. Check whether Pride Story Thorn can be retired without losing narrative.
- **Step B: Other seven Gyms.** Follow manifest order Rook → Ashen → Kai → Vega → Draven → Freya → Nyx. Each next Gym requires the previous `league_*` tag. For every one, verify story chapter, RCT team and moves, FTB ID, first-time payout, idempotent repeat, no-op dialogue.
- **Step C: Elite Four.** Sequentially verify Koda → Seraph → Celeste → Vesper (`league_e4_1..4`); preserve their actual win callbacks. Don't assume all E4 have standalone monetary rewards.
- **Step D: Champion Magnus.** Verify battle, `league_champion`, FTB Champion task, Hall of Fame scoreboard/title/broadcast, player Champion title/crest, one-time payout and repeat protection.
- **Step E: Retire source datapack** ONLY after inventory outside the League trainer files confirms that the rest of League 7.10.10 (Sequoia, Theo, Avery, all non-League NPCs, starter functions, services, sounds and advancements) has been replaced or safely extracted. Do not remove it as a monolith during Module 02.

## Important warnings

- Do **not** place both a Pride Story NPC and a legacy RCT trainer at the same Gym as the final UX. One visible trainer is the acceptance target.
- Do **not** try to force battle completion by simply adding `league_*` tags. Official battle outcomes and first-win callbacks are the authority.
- Do **not** give regular players OP permissions or accept arbitrary player names, trainer ids or command strings in a battle API.
- Do **not** reset existing players' FTB progress, awards or world tags while consolidating.
- Verify non-OP and two-player isolation before advancing from staging. Existing tests prove code compiles; they do not substitute for actual Cobblemon battle completion.
- Keep the currently working League 7.10.10 datapack enabled until *all* dependencies above have replacements.

**Module 02 scope:** make story NPCs trigger/acknowledge the authoritative League battles and unlock story chapters. **League migration scope:** replacing RCT reward ownership and deleting the legacy datapack should be a later isolated module/release with its own rollback plan.
