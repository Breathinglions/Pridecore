# Pride Core Configuration Audit
**Date:** October 29, 2026  
**Target:** Minecraft 1.21.1 · Cobblemon 1.8.1 · Fabric · Pride Core 0.4.3-rc1 unified STAGING build  
**Status:** Implementation plan and verified current-state audit; **does not activate new settings**.

## 1. Governing rule

Put player-facing content and routinely changed balancing/settings outside the JAR. Keep verification, authorization, event listeners, one-time reward enforcement, data integrity, and protocol contracts in Java. For systems already owned by other mods, maintain their official configs or data interfaces instead of creating a conflicting second owner.

This audit covers the actual `pride-core-0.4.3-rc1-UNIFIED-STAGING.jar`, the Pridecore GitHub Module 02 branch and associated project docs. It **does not** imply a source audit of every independent server mod, datapack, merchant, or LegendaryQuests release.

## 2. Confirmed existing runtime configuration

| File | Currently read/created by Pride Core? | Actual purpose |
| --- | --- | --- |
| `config/pride_core.properties` | Yes, generated when absent | `allow_admin_mutations`; default false |
| `config/pride_core/story.properties` | Yes, generated when absent | `story_enabled`, `story_allowed_dimensions`, optional `effects.*` particle IDs |
| `config/pride_core/story.json` | Yes, generated/validated/merged when appropriate | Narrative stage, node text, story choices, prerequisites and story facts |
| `config/pride_core/rin_scene_anchors.properties` | Optional; administrator-created | Opt-in owner-only Rin *preview* coordinates and dimension |
| `config/ftbquests/quests/` | Managed by FTB Quests, not Pride Core | Existing quest structures and tasks; changes require careful merge |
| `config/pride_flashpoint_dev.properties` | Separate Flashpoints module | Staging enablement, dimensions and budgets; not a general server/world config |

**Not presently loaded from external config:** original native Sequoia/Theo/Avery dialogue assets, Theo/Avery grant items, native RCT team files and first-win payouts, and Module 02's nine `story_battles.json` authored teams. Those are bundled data.

## 3. The starting-item audit — highest priority

| Original native NPC/function | Current rewards | Current gate/authority | Proposed editable field |
| --- | --- | --- | --- |
| Professor Sequoia; `prideregion:onboarding/meet_sequoia` plus native starter UI | Genuine Cobblemon starter selection (no ordinary starter item kit) | Native Cobblemon `openstarterscreen` and `starter_chosen`; never bypass | **No configurable starter grant**; narrative text can be separately editable |
| Technician Theo; `prideregion:onboarding/trainer_card` | `rctmod:trainer_card ×1`, `knowlogy:knowlogy_book ×1` | `starter_chosen`, `trainer_card_received` / `pokepedia_received` | `onboarding_rewards.json` → `theo_registration.items` |
| Avery; `prideregion:onboarding/trainer_guide` | `cobblemon:poke_ball ×10`, `minecraft:bread ×8` | `trainer_card_received`, `trainer_guide_received` / `pride_avery_kit` | `onboarding_rewards.json` → `avery_field_kit.items` |
| Mom | No current item reward | `pride:met_mom` story checkpoint | Optional new `mom_welcome` kit, disabled by default |

**Important second grant path:** `prideregion:onboarding/sync_player` can give a missing `knowlogy:knowlogy_book` when `trainer_card_received` exists but `pokepedia_received` is absent. Any Theo reward migration must preserve this recovery behavior or intentionally replace it with a tested equivalent. Do **not** add a second default grant source.

**Non-item side effects to preserve:** Avery's native function sets the player's RCT `pride_region` series, sets original completion tags, and updates the original advancements/FTB tasks. The original kit function also conditionally resets *unearned* First Catch / First Battle detector tasks. Those are gameplay integration rules, **not** configurable item rewards.

### Proposed implementation: a real validated grants service

- Create `config/pride_core/onboarding_rewards.json` on first launch with the current Theo/Avery defaults, so installing an update initially changes no rewards.
- Parse an allowlisted versioned JSON schema. Each item has a registry ID (including optional mod item IDs) and a bounded positive count. No arbitrary server commands or player names in reward config.
- Required core items must resolve in the actual registered mod list; invalid required items prevent that grant from running. Optional extras may be explicitly declared skippable, with a visible log/report. Refuse malformed counts, unknown schema fields and invalid identifiers.
- Bridge only the **existing trusted native NPC completion actions** to the grant service. Keep original prerequisites and once-only tags. Do not expose a free `/kit` or spoofable client packet.
- Record completion per player and reward source with persistent replay protection; handle full inventory safely, interrupted delivery, relog, repeated clicks and two concurrent players. Do not retroactively give additions to already-claimed kits without an intentional, permission-protected migration operation.
- Validate replacement configs before applying; retain the last known-good configuration on a bad reload; allow OP4 `/pridecore config validate` and `/pridecore config reload` once these commands are implemented.
- Do not move item grants until the new service and the original native functions pass the same staging tests. **At present these commands and the grant service do not exist.**

## 4. What should be configurable, and what should not?

**Key:** LIVE = confirmed working configuration input; ADD = should be implemented; MOD = leave ownership with the existing mod; CODE = hard-coded enforced mechanism, intentionally not editable.

| Area | Recommendation | Current state / boundary | Priority |
| --- | --- | --- | --- |
| Theo / Avery items and quantities, new mod extras | **ADD** | Bundled `/onboarding/trainer_card.mcfunction` and `/trainer_guide.mcfunction`; preserve original eligibility, idempotency, special side effects | P0 |
| Optional Mom welcome items | **ADD**, disabled | No kit currently; add only after same grant protection exists | P1 |
| Core narrative chapter text, stage choices and story prerequisites | **LIVE** | `story.json`; validated and additive migration. Some quest gates intentionally fixed for integrity | Existing |
| Sequoia / Theo / Avery native *text* and optional narrative variations | **ADD** | Native dialogue JSON currently inside JAR. Preserve `q.*` commands and `isVisible` conditions; editable text must not rewrite native action logic | P1 |
| Other narrative NPC dialogue and journal text | **ADD** where not already in `story.json` | Native dialogue is bundled; story engine prose already external | P1 |
| Gym / Elite Four / Champion teams, levels, moves | **MOD** with guarded data pack integration | RCT owns native trainer JSON; source assets are bundled in unified JAR. Revisions require a validated RCT resource override, not a JSON file Pride Core ignores | P1/P2 |
| Gym / Elite Four / Champion first-win ₽ amounts and badges | **MOD** / controlled integration | Original RCT first-win callbacks remain authority. Never install a parallel Pride Core payout listener | P1/P2 |
| Nine non-League storyline trainer teams and intro/win lines | **ADD** | `story/story_battles.json` is bundled; keep battle mode **Pokémon-only** and validate badge gates | P1 |
| Storyline clue text, evidence naming and visual hints | **ADD** | Physical markers and stage rules exist in code; configuration should customize presentation only unless validated progression migration is specified | P2 |
| Finale difficulty and presentation parameters | **ADD** bounded values | Four conduit / three authenticated shield wins / one core cannot be bypassed through a config edit | P2 |
| Rin scene anchors | **LIVE**, manual opt-in | `rin_scene_anchors.properties`; coordinates & dimension for client-only preview | Existing |
| Particles | **LIVE**, partial | `story.properties` effect keys; bounded, registered *simple* particle types only | Existing |
| Cinematic timing, sound keys, camera, motion style | **ADD** with strict limits | Mostly authored code/client today; no camera or final model controller integration yet | P2 |
| Move Tutor, PokéBuilder, daycare/shop prices and rules | **MOD** | Native provider owns the actual charge/item/transaction. Optional UI text may read native price. Avoid double billing | Provider config |
| Battle Tower/BP, PvP tournaments, Wonder Trade | **MOD / separate module config** | Not governed by Module 02 story code. Review each owned implementation separately | Separate audit |
| LuckPerms ranks, Flan claims/rents, ChestShop, Impactor economy | **MOD** | These services already have their own authoritative state/config. Pride Core should reference, not duplicate, their permissions or balances | Provider config |
| FTB Quests stages, descriptions and task IDs | **MOD** | FTB SNBT/config is editable externally; use ID-stable migration tooling, not replacement of an entire quest book | Existing |
| LegendaryQuests, dungeon encounters, Flashpoints, Operations, World Bosses | **MOD / separate subsystem config** | LegendaryQuests and endgame systems have different controllers/ownership and combat rules; don't entangle with story kit JSON | Separate audit |
| World structures, NPC location, marker placement, claims | **CODE + world state** | Location is actual saved world entities and builder-verified placements, not fake JSON coordinates. Cosmetic scene coordinates may be config | Not generic config |
| Permission checks, victory authentication, starter entitlement, anti-replay, server-side packet boundaries | **CODE** | Security-critical. Server must not accept arbitrary config commands or client progression claims | Never exposed |
| Saved player facts, League tags, Hall of Fame score, inventory/transactions | **CODE / owner state** | Player-specific persistent data and authoritative mod state, not editable configuration defaults | Never exposed |

## 5. Proposed future folder layout (not yet active)

```text
config/
├── pride_core.properties                # existing foundation setting
├── pride_core/
│   ├── story.properties                 # existing
│   ├── story.json                       # existing
│   ├── rin_scene_anchors.properties     # optional, admin authored
│   ├── onboarding_rewards.json         # PLANNED: Theo/Avery/Mom kits
│   ├── npc_dialogue_text.json           # PLANNED: native text only
│   ├── story_battles.json               # PLANNED: validated story RCT rosters
│   └── presentation.json                # PLANNED: bounded cinematics
└── ftbquests/                           # independent provider-owned config
```

Do not distribute these PLANNED files in a way that suggests the current JAR already reads them. Avoid a single giant `config.json`; separate modules so backups, validation and reloads are predictable.

## 6. Controlled development order

1. **Freeze the October 29 unified staging JAR for tomorrow's integration test**. Existing rewards continue to work exactly as before.
2. Implement onboarding reward JSON loader + strict validator + generated defaults. Test with current core items and extra registered mod items, missing optional/required items, bad JSON and high quantities.
3. Connect the **trusted** native Theo and Avery functions to the new grant service. Preserve existing completion tags, the Poképedia recovery rule, FTB tasks, starter flow, and Avery's RCT series selection. Add a robust claimed/recovery ledger without a second player-triggerable reward path.
4. Stage-test a fresh non-OP player, a second player, a full inventory, repeated clicks, relog/restart, crash/retry, item registration absence and an edited config reload. Verify one-time awards, no user exploits and no lost essential items.
5. Externalize native NPC text separately from protected Cobblemon action scripts. Add validation and fallback before permitting server-side live reload.
6. Expose validated storyline roster and bounded cinematic configs. Work with RCT's existing data architecture for any official League balance changes, after single-NPC acceptance.
7. Audit each independent mod/service for owned config capability before adding any Pride wrapper; update the operator documentation and acceptance checklist.

## 7. Release guardrails

- Never silently alter real players' previously received kits when the config changes.
- Never give a Pokémon starter through a configurable item list.
- Never move RCT reward ownership or original FTB Quest IDs by modifying a text file.
- Unknown modded items cannot silently become vanilla substitutes.
- Keep reward source permissions, item ownership and player UUID checks server-side.
- Only production-promote after clean staging acceptance with a rollback of both JAR and config.