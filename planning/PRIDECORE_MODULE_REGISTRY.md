# Pride Core — Module Registry (authoritative project vocabulary)

**Purpose:** All development work is assigned to a named module. The user can say "work on Chat & Identity", "work on Story & NPCs", or "work on Module 04", and the scope below remains stable.

**Target:** Minecraft 1.21.1, Fabric, Java 21, Cobblemon 1.8.1. Maintain the existing server and all verified content. Build modules internally; package validated modules into Pride Core server JAR. Pride PvE and future advanced-client functionality may remain separate deployables when justified.

## Stable module names and IDs

| ID | Module | Responsibility | Initial status / integration boundary |
|----|--------|----------------|----------------------------------------|
| 01 | **Foundation & Player Progression** (`foundation`) | UUID profiles, authoritative Pride-owned story facts, persistence, event plumbing, compatibility views, diagnostics. | v0.1.0-rc1 code-complete foundation in `modules/core`; GitHub CI and regression tests passed. **Staging in-game acceptance still required.** No tag import. |
| 02 | **Story & NPCs** (`story_npc`) | Dialogue, player choices, chapter objectives, NPC interaction adapters, content reload, main campaign. | Planned. Existing NPC actors and datapacks retained until audited. |
| 03 | **League Integration** (`league`) | Eight gyms, badge display and requirements, Elite Four/Champion, FTB Quests read adapter. | Existing League/FTB Quests remain authoritative. |
| 04 | **Chat & Identity** (`chat_identity`) | LuckPerms-driven ranks, compact existing Poké Ball sprites, overhead nameplates, chat formatting, join/leave messages, announcements and notification presentation. | **Required consolidation module.** Preserve deployed LuckPerms + Styled Chat + Polymer resource pack and previously approved compact sprites. Migration pending source/runtime audit; *not* complete. |
| 05 | **Trainer Card & Interface** (`trainer_ui`) | Profile hub, quest journal, objectives, badge/pokedex summaries and screen/menu integration. | Planned. Client extension optional if advanced UI needs it. |
| 06 | **World & Travel** (`world`) | World discovery, region access, fast travel, dimension gates, region protection compatibility. | Existing Multiworld, Flan and tag-based gates retained. |
| 07 | **Economy & Rewards** (`economy`) | Impactor adapter, payout coordination, idempotency, transaction diagnostics and reward balance. | Existing economy and 1,253 trainer first-win datapack retained. No double payments. |
| 08 | **Community & Social** (`social`) | Parties, clans, social permissions, community events, quizzes and scrambles. | `modules/community` on `feature/pride-community-impactor-ci` is existing source to assess for migration (quiz/announcements/economy). Do not confuse this with the entire Chat & Identity module. |
| 09 | **Pokémon Activities** (`pokemon`) | Pokédex milestones, hunting, raid-dens bridge, daycare/GTS bridges, daily activities. | Use Cobblemon and verified existing mods as authorities. |
| 10 | **Endgame PvE** (`pve`) | Flashpoints, Operations, World Bosses, encounter/instance infrastructure. | Flashpoint prototype currently on `main`; staging-only. Preserve until independently integrated. |

Operations / QA / release management are **cross-cutting release gates**, not a gameplay module.

## Module completion contract

1. **Design:** requirements, dependencies, data ownership, commands, data definitions, acceptance journey, migration plan.
2. **Implementation:** complete internal module, not individual tiny JAR fragments.
3. **Automated audit:** compile, unit/property tests, negative/edge cases, persistence, multiplayer-state correctness.
4. **Staging RC:** full modpack load, non-OP and OP walkthroughs, restart/restore, visual consistency.
5. **Production approval:** release notes, known limitations, rollback, explicit promotion. Only then call the module "done."

Build code in units, test code automatically throughout, and **batch in-game testing around complete, functioning modules**. Content-only edits should use a validated reload path wherever safe. No assertion of in-game verification based solely on CI passing.

## Rules preventing legacy breakage

- **One authoritative owner per gameplay fact.** LuckPerms owns ranks; Impactor owns wallet balances; Cobblemon owns Pokémon storage; existing League/FTB and LegendaryQuests continue owning current completion until an explicit tested migration.
- Existing `league_*` and `pride_*` tags must not be deleted or auto-imported just because Pride Core can read them.
- No two simultaneous chat formatters or event announcers. Route or disable duplicates only after staging verification.
- Keep existing v0.3.1-approved genuine Cobblemon Poké Ball sprite sizes/formatting, six rank badge identities, and Polymer-compatible resource-pack rendering unless the user explicitly approves a change.
- Preserve tested Flashpoints and Pride Community branches; don't blindly merge Java classes, mod IDs, resources or config paths into another JAR.
- Player-facing new content is data-driven where practical, while server authority and persistence stay in Java.
- All new work must say which module ID and release candidate it belongs to.

## How to request work

- `Pride Core — Module 04 (Chat & Identity): polish overhead nameplates`
- `Pride Core — Module 02 (Story & NPCs): Professor Sequoia dialogue`
- `Pride Core — Module 07 (Economy & Rewards): audit trainer payouts`

Within a module: provide the complete module acceptance test after code and automated QA; do not force a new server restart per small subfeature.

## Suggested dependency order

Foundation (01) → Story/NPC (02) + League bridge (03) → Trainer UI (05) and World (06).
Chat/Identity (04) may be developed **in parallel after the cross-module notification interface is defined**; it is not blocked by completion of the entire story. Economy (07) enables social (08), activities (09), and PvE (10) reward flows. Existing modules stay operational during migration.

**Document status:** A canonical plan and naming contract, not proof that pending modules have been coded or shipped.
