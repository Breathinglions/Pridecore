# Module 02 — Complete Story & NPCs Scope and Content Production Plan
**Pride Region 2.0 · The Resonance Crisis**

**Decision (Oct 25, 2026):** The uploaded `PokemonLeague7.10.10_FTB_Only_CobbleBadges_Removed_STAGING` pack is **existing League/onboarding infrastructure**, not the full game campaign. Pride Core **Module 02 includes all original campaign content and NPC integration**, not merely reformatting League dialogue or supplying six demonstration conversations. The full story must coexist with the current League and LegendaryQuests content.

**Status:** implementation requirements and creative content outline, **not** gameplay delivered or server-side changes. Module 02 v0.2.0-rc1 is an engine demo/staging candidate, not feature-complete.

## The three independent layers

1. **Pride Core Module 02 server engine (Fabric Java)** — authoritative story state through Module 01, conditions, objectives, dialogue orchestration, NPC/quest event adapters, choice routing, event subscriptions, validation, event IDs, cooldowns, failure recovery, QA diagnostics. Later modules may consume read-only story events.
2. **Story content packs (native Cobblemon NPC/datapack)** — native `data/pride_region/npcs` classes, `data/pride_region/dialogues` screens and MoLang decisions, quests, NPC identities/skins, optional branching dialogue, localization, editable chapter data, controlled calls to existing reward functions where verified. Versioned separately from the Fabric JAR, hot-reloadable with safe validation where supported.
3. **World content / game design** — actual towns, routes, ruins, lairs, puzzles, battle arenas, map markers, NPC placement, environmental clues, pathfinding/permissions, ability to recover from missing build areas. Built and playtested as chapters arrive, rather than requiring the whole new map for engine implementation.

The League datapack stays installed during the staging transition. Eventually cut over specific dialogues to dedicated Pride story content without duplicating resource locations, defeating data-pack load order or changing existing Cobblemon NPC services without tests.

## Campaign-owned cast (to be created or explicitly reused)

| Role | Name / identity | Where introduced | NPC/progression interaction |
|---|---|---|---|
| Starter household NPC | **Mom** — NEW native NPC | Hearthleaf player home | Wake-up event, introduction, instructions, repeat visits |
| Professor | **Professor Sequoia** — REUSE | Hearthleaf Pokémon Lab | Existing Cobblemon starter selector; discover actual completion |
| Registrar | **Technician Theo** — REUSE | Lab | Existing trainer registration and Poképedia issuance |
| Field-kit assistant | **Avery** — REUSE | Lab | Existing one-time kit; do not duplicate Poké Balls/food |
| Research guide | **Researcher Mira** — REUSE (distinct from ranger character) | Lab/region | Guidance, journals; retain existing services |
| Rival | **Rin** — NEW *working name* | Hearthleaf/Route 1 and every major arc | Repeat challenge team progression, branching conversations, finale ally |
| Ranger ally | **Captain Mira?** — NEW *name conflict* | Route 1 / Ranger Guild | Must choose different canonical name or explicitly combine with Researcher Mira; do not silently create duplicate Miras |
| Regional research institution | **Meridian researchers**, technicians, inspectors (NEW characters) | early routes, electric city, ice station | Clues, data collection, optional ethical dialogue |
| Villain faction | **Team Eclipse** grunts, commanders, leader **Sable** (working name) | Act I onward | Trainer battles, rescues, ambushes, evidence, villain operations |
| Meridian leadership | **Director Cael** (working name) and senior scientists | midpoint → finale | Evidence, mystery, boss battle, choice aftermath |
| Environmental cast | local rangers, historians, residents, shopkeepers | city and route chapters | Optional side quests and living-world responses |
| Gym leaders | **Thorn, Rook, Ashen, Kai, Vega, Draven, Freya, Nyx** — EXISTING LEAGUE | 8 gyms | Personal chapter scene/dialogue *alongside* existing battle/FTB authoritative mechanisms |
| Elite Four and Champion | **Koda, Seraph, Celeste, Vesper, Magnus** — EXISTING LEAGUE | endgame | Narrative integration only; do not write League wins from story flags |
| Postgame guides | Ranger, operations, social, competitive and expedition liaisons | League completion | Discovery and optional invitations; mechanics owned by later modules |

**Important:** names for NEW characters/factions are provisional until art, NPC model, dialogue and world placement are approved. Name conflicts must be resolved before content generation.

## Narrative implementation by chapter

| Stage | Primary location/anchor | Essential story objective | Gameplay requirements and conditions | Reusable content |
|---|---|---|---|---|
| Prologue | Hearthleaf home, Professor Lab | Meet Mom → choose starter → register → collect kit → rival introduction | Detect *real* starter choice, real one-time registration/kit, safe skip for migrated players | native NPCs, party state adapter, mentor tutorial, journal |
| Route 1 | route and ranger camp | First catches/trainer encounter; unusual signal discovered | Wild capture/battle verification adapter, narrative clue marker, recoverable exploration | rival NPC, ranger NPC, world marker |
| Act I-A / Gym 1 | Forest → Verdant | Investigate habitats, challenge Thorn | League badge read-only signal from current system, ranger side quest | clue, wild Pokémon, gym intro dialogue |
| Act I-B / Gym 2 | Rocky route, mine, Rook | Recover missing geological survey, defeat thieves | puzzle/item evidence verification, one-time mission markers; Bastion badge remains League-owned | Team Eclipse grunt, archivist |
| Act II-A / Gym 3 | Fire district | Investigate machine/energy disturbance with Ashen | rescue/defend event, battle evidence, existing Inferno badge owner | technicians, optional route quest |
| Act II-B / Gym 4 | Water district | Examine endangered ecosystem, uncover false accusation with Kai | investigation clues and faction dialog branching; Tidal badge owner | researcher, ranger environmental quest |
| Act II-C / Gym 5 | Power station, Vega | Find who built Resonance Engine; midpoint reveal | multi-objective puzzle, verified Electric Gym checkpoint | Meridian researchers, facility actors |
| Act III-A / Gym 6 | Dragon lore sites | Trace ancient guardianship records with Draven | ruin exploration, discovery markers and puzzle validation | historian/guardian NPC |
| Act III-B / Gym 7 | Frozen research station | Recover Meridian's hidden warning records with Freya | interior checkpoint and optional research branches | logs, scientists, opposing teams |
| Act III-C / Gym 8 | Ghost / abandoned lab | Investigate memories, confront consequences with Nyx | staged boss encounter, clues, existing Ghost gym win | recurring rival, spectral story |
| Regional Finale | Resonance Engine facility | Stop dangerous activation, face Eclipse/Meridian leadership, resolve crisis | boss and environmental state, instancing/arena protections, choices converge safely | story finale, epilogue |
| League Finale | Victory Road, E4, Champion Magnus | Become Champion | All 8 gym + E4 + Champion progression **read** from Module 03/old League | E4/Champion narrative beats |
| Postgame Bridge | League HQ and Guild | Introduce expansion activities | Non-destructive postgame tutorial, no override of legendary gates | future modules' optional quest hooks |

Every stage needs dialogue definitions, at least one grounded encounter/quest, explicit prerequisites, real completion verification, non-OP testing, repeat dialogue, restart/rejoin test, and content QA. Narrative pacing should not make players grind pointless talks just to unlock gyms.

## Required quest/objective types

- **Talk** to NPC / choose dialogue; persistent one-time interaction marker.
- **Explore** area / marker with correct dimension and world, not just coordinate check.
- **Capture** Pokémon via Cobblemon's authoritative events; species/quantity conditions and rollback semantics.
- **Win trainer/Pokémon battle** through verified event or read-only provider, not chat messages or arbitrary commands.
- **Collect/deliver** quest item(s); idempotent handoff, safe inventory and item authenticity checks.
- **Solve** interactable puzzle, activate switches in order; checkpointed per player or party.
- **Investigate** evidence, optional lore discovery; accommodate alternate order.
- **Choice/reputation**: reversible or reconverging side branches, no permanent League lockout.
- **Rescue/protect**: escort or combat objectives with controlled reset/timeout.
- **Defeat story boss**: existing trainer battles as default; combat-mode exceptions explicitly planned and tested.
- **Read-only external achievement**: native starter, FTB League, LegendaryQuests/realm access; never duplicate reward grants.
- **Group involvement**: each participant receives only their earned state; avoid global cross-player completion.

These are **requirements** for the final engine, NOT capabilities fully present in v0.2.0-rc1. At present the RC can handle owned fact prerequisites, dialogue options and stage updates, but not actual item, battle, location, starter or quest-provider events.

## Player-facing gameplay requirements

- Native Cobblemon NPC appearance/portrait/dialogue (no mandatory chat-command menu in released adventure).
- Visible **current objective, next contact and location hint** through a minimal server compatible journal/quest presentation; Module 05 later provides the polished Trainer Card UI.
- Cinematic beats where feasible, non-disruptive sounds/particle/camera transitions (only with supported mods/client functionality).
- NPC chatter should change with badge count, rival meetings, faction discoveries and chapter status.
- Free-roam by default: players can explore cities/routes early, while *specific* main story interactions/bosses have chapter gates.
- Recovery from missing NPCs, teleport mishaps, item loss, side-quest abandonment or disconnect.
- Player language/localization as content data; gameplay texts aren't hardcoded in Java.
- Optional and repeatable content with controlled cooldowns and a separate reward ledger; no infinite payout from reusing a dialogue.
- Distinct test/production configs and resource-pack compatibility; only approved server-side assets distributed.

## Safe native NPC / datapack cutover

**Current League datapack v7.10.10 is not solely a League scoreboard:** it contains 24 native NPC classes and dialogues plus onboarding and services. Preserve those. Do not remove it wholesale.

1. Audit in-use world/datapack versions and actual NPC class IDs, dialogue IDs, actions and specific legacy tags.
2. Define owned/external data authority. Module 01 owns only `pride:` facts; current League/FTB, Cobblemon, LegendaryQuests and Impactor retain their data.
3. Build **bridge** to existing native Cobblemon dialogue choices (safe authenticated player and validated actions), confirm non-OP context. No blanketing the entity callback or overriding unrelated mods' clicks.
4. Add one *new* Mom native NPC and a small first-chapter overlay. Verify conflict-free resource paths and rollback without changing live original pack.
5. Connect real Sequoia starter, Theo registration, and Avery kit completion **only after** confirmation from their existing data. Test returning players with prior completion and fresh players.
6. Expand narrative chapters using versioned content bundles. Each bundle can update validated dialogue/quest definitions without Java recompilation when safe. New event adapter types require new JAR and staging.
7. Replace only the old dialogue/function bits that are now covered and whose one-time rewards/data migration have passed regression checks. Do **not** delete the entire League datapack while it owns badge/FTB trainer logic.
8. Maintain backups, prior known-good pack, and documented rollback at every cutover.

## What “Module 02 complete” actually means

**Engineering-complete:** story engine, objective types actually used by the main plot, native NPC GUI callbacks, authenticated actions, safe persistence/recovery, data-driven authoring/reloads, diagnostics and automated tests are complete.

**Campaign content-complete:** full chapter sequence from Mom to regional crisis to Magnus exists, NPCs are defined, dialogues and interaction conditions written, objectives/encounters scripted, side quest hooks placed, and all prerequisites/rewards have explicit authoritative owners.

**World-ready:** all essential chapter locations and story markers exist in the playable region; no permanent blockers even if optional art/decoration is incomplete.

**In-game verified:** full beginning-to-Champion playthrough using normal permissions, second-player isolation, repeat/restart/corruption tests, migration from legacy players, Cobblemon interactions, no duplicated starter/kit/badge rewards, no affected LegendaryQuests, and practical rollback.

We can implement and QA engine components while world building is underway, but **do not label the module fully done merely because the six-NPC RC flow passed**.

## Release packaging

- **One Pride Core server JAR** containing Module 01+02 (do not install separate 0.1 and 0.2 mod IDs simultaneously).
- **One versioned Pride story content datapack/overlay** compatible with existing League NPC resources; prefer separate namespace/IDs for new assets and narrowly-targeted overrides of existing dialogues after verification.
- Optional resource pack assets (skins, icons, sounds) and world schematic placement manifest only when needed.
- Each release candidate includes a matrix of which story chapters are playable and which remain stubs.

## Immediate next engineering milestones

- M02.1: native Cobblemon 1.8.1 NPC JSON, UI and MoLang interaction API bridge with a verified non-OP test on a new Mom NPC.
- M02.2: real starter/registration/field-kit adapter, legacy player reconciliation, and idempotent Prologue end-to-end test.
- M02.3: objective system (battle/catch/location/evidence/item), journal/diagnostics and data reload safety.
- M02.4: rival, Ranger/Researcher identity resolution, Route 1 and Verdant story slice with actual battle/quest conditions.
- M02.5: Act I → III chapter content, reusable faction and environmental systems, world/quest QA.
- M02.6: Crisis finale, League handoff, postgame bridge, regression/multiplayer acceptance.

**Dependency note:** Integration with the eight existing gyms in story can read legacy FTB/League data for now; Module 03 will own final League migration. Do not block Module 02's campaign content production on rewriting the League.
