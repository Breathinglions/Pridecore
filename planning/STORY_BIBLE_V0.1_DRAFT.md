# Pride Region — Main Story Bible v0.1 (Creative Proposal)

 > **Status:** Main narrative direction approved October 25, 2026; cast names, chapter prose, and exact encounters remain subject to content review. Module 02 implementation underway.  
> **Module:** 02 — Story & NPCs.  
> **Game:** Pride Region | Minecraft 1.21.1 | Fabric | Cobblemon 1.8.1.  
> **Genre direction:** traditional Pokémon coming-of-age journey + layered MMORPG-style political/ecological mystery.  
> **Current story title (working):** **The Resonance Crisis**.

## Core creative promise

A trainer leaves home, meets a rival, chooses a partner, explores a living region, earns eight badges, and defeats the Pokémon League. Simultaneously, disturbances called **Resonance** affect Pokémon behavior and expose a conflict between well-intentioned researchers, unscrupulous extremists, conservationists, and political institutions. The player gains insight through exploration rather than endless exposition.

The player must be allowed to explore freely. Story objectives guide progression but do not turn the region into an invisible-walled corridor. The main League remains the backbone, while the faction mystery rewards curiosity and ultimately leads to a separate regional crisis finale before Victory Road. Advanced MMORPG systems emerge naturally during the Champion/postgame transition.

### Tone pillars

1. Pokémon spirit: uplifting, colorful, adventurous, accessible to newcomers and families.
2. Stronger narrative: secrets, conflicting motives, recurring supporting characters, consequences and sacrifice without excessive violence or grimdark themes.
3. Player agency: choices adjust dialogue, reputation and optional side missions, **not** the ability to complete the League.
4. Environmental storytelling: mystery conveyed by structures, patrols, regional effects, music, props and repeat visits.
5. No loss of legacy work: tested LegendaryQuests remain separate, optional adventures with their existing completion data and encounter logic.

## Existing canon locked for preservation

### Opening characters (existing names)

- **Mom** — emotional anchor and first story step at the player's home in **Hearthleaf**.
- **Professor Sequoia** — starter selection and introduction to trainer life.
- **Technician Theo** — trainer registration, Trainer Card and Poképedia.
- **Avery** — starter field kit (10 Poké Balls, 8 bread and guide, unless later rebalanced explicitly).

### League roster (existing order)

| Position | Leader | Badge if established | Story relevance proposal |
| --- | --- | --- | --- |
| Gym 1, Grass — **Verdant Gym** | **Thorn** | **Verdant** | Disturbed forest habitat; early evidence of Resonance |
| Gym 2, Rock | **Rook** | **Bastion** | Old mining survey, carved symbols and missing geological reports |
| Gym 3, Fire | **Ashen** | **Inferno** | Hotspot near geothermal machinery; sabotage or a leak |
| Gym 4, Water | **Kai** | **Tidal** | Waterway contamination, false accusations and threatened ecosystems |
| Gym 5, Electric | **Vega** | — | Mysterious power fluctuations and missing components |
| Gym 6, Dragon | **Draven** | — | Ancient lore and an old promise to protect the region |
| Gym 7, Ice | **Freya** | — | A frozen research station with the first unambiguous evidence |
| Gym 8, Ghost | **Nyx** | — | Disturbances of dreams/memory and the final clue to the hidden facility |

Elite Four: **Koda, Seraph, Celeste, Vesper**. Champion: **Magnus**. Existing gym-gate order and FTB Quest requirements must not be silently overwritten.

## Working factions and recurring cast — NEW PROPOSALS

These names and character beats are placeholders subject to approval.

- **The Meridian Institute** — admired scientific organization investigating Resonance, providing research facilities and public education. Some staff are sincere; its influential director uses the crisis to justify unacceptable experiments.
- **Team Eclipse** — recognizable villainous field team: thefts, ambushes, intimidation, illicit Pokémon captures, raids on labs. Its public leader believes Meridian has concealed vital evidence and takes reckless action in the name of exposing it.
- **Pride Rangers** — conservation, investigation and disaster-response organization, an ally during route and rescue quests. Rangers distrust both factions while trying to protect people and Pokémon.
- **The Regional League** — respected cultural institution, not secretly evil. Leaders and Champion Magnus have different perspectives on how involved the League should be.
- **Rin (working name), the rival** — starts on the same morning as the player; competes for badges but becomes increasingly involved in the investigation; develops from friendly rivalry to dependable teammate.
- **Director Cael (working name)** — chief architect of Meridian's experimental **Resonance Engine**. Intellectually persuasive but ultimately willing to destabilize wild ecosystems to gain control of the energy.
- **Sable (working name)** — leader of Team Eclipse. A charismatic but dangerous antagonist whose suspicions about Meridian are partly justified; harmful methods remain clearly condemned.
- **Ranger Captain Mira (working name)** — recurring ranger mentor and moral counterweight; local side investigations teach exploration and Pokémon care.

### What is Resonance?

A poorly understood natural phenomenon connected to ancient sites and rare Pokémon. It causes unusual energy signatures, changes animal behavior, awakens dormant machinery, and provides a thematic bridge for later regional content.

**Important:** Its origin is intentionally left partly unknown. We must not retcon established LegendaryQuests explanations, use Resonance to automatically claim a legendary, or require Darkrai/Mew/Mewtwo/Johto/Hoenn expedition completion for the main story. Legendary expeditions may later feature optional journals or connections, with their current triggers untouched.

## Main campaign by act

### Prologue — The Beginning of a Journey (Hearthleaf → Route 1)

1. Player awakens at home and speaks to Mom, who mentions a promising first journey and news of unusual wild Pokémon.
2. Professor Sequoia handles the existing Cobblemon-compatible starter selection.
3. Theo registers the trainer and introduces the Trainer Card and journal.
4. Avery issues the existing field kit once, without duplicating legacy rewards.
5. The player meets rival Rin, practices a first trainer battle, catches a Pokémon, heals and explores Route 1.
6. A ranger requests help with a distressed wild Pokémon; the player glimpses an unexplained energy pulse, not a full plot dump.

**Module 02 acceptance slice:** Mom → Sequoia → Theo → Avery → Route 1 → first encounter → Verdant Gym eligibility. Do not assume the current installed NPC system exposes a Java callback; implement a safe adapter only after inspecting the deployed mod.

### Act I — Unusual Signals (Verdant Gym and Bastion Gym)

- Thorn explains a recent habitat imbalance and points toward unusual crystalline deposits near a route.
- Rival reappears with a complementary clue but has a different theory.
- Rook identifies old geological records and missing research samples.
- The player witnesses Team Eclipse for the first time, interrupting a Pokémon theft or data theft.
- First recurring mystery clue: Meridian field notes show that the phenomenon is not entirely natural.

**Gameplay:** basic battles, optional ranger rescue, a non-combat puzzle, route exploration, two League milestones.

### Act II — Conflicting Stories (Inferno, Tidal, Electric Gyms)

- Ashen's district experiences unstable heat or machinery during a regional event.
- Kai's community faces a damaged water ecosystem; evidence points at Team Eclipse, but the timeline is inconsistent.
- Vega's power network is the heart of a suspicious energy distribution system.
- Meridian offers the player research assistance, while Rangers want evidence and Eclipse members insist the Institute is the real threat.
- Midpoint reveal: Meridian is building a controllable Resonance Engine. Its stated humanitarian purpose is partly credible; its experiments are putting Pokémon at risk.
- Rival and player may disagree about which institution to trust without becoming permanent enemies.

**Gameplay:** investigations, taggable proof/evidence collection, optional faction reputation, mid-route encounters, existing gym progression, no forced permanent faction lockout.

### Act III — The Buried Truth (Dragon, Ice and Ghost Gyms)

- Draven connects new energy readings to old regional guardianship stories and a sealed research location.
- Freya's frozen station holds evidence that Meridian had warning signs of harm but concealed them.
- Nyx helps the player interpret ghostly memory fragments and navigate a final ritual/puzzle; this must not require Darkrai's independent Nightmare Realm quest.
- Team Eclipse launches an uncontrolled attack to destroy the engine before it activates, which could make the crisis far worse.
- The player and rival must cooperate with Rangers and sympathetic Meridian staff to avert disaster.

**Gameplay:** environments and puzzles with explicit recovery paths, elite field trainer battles, optional investigation variations and faction-specific dialogue.

### Act IV — The Resonance Crisis (after Gym 8; before Victory Road)

- Director Cael refuses to shut down the engine, believing controlled Resonance is the future of the region.
- Sable's sabotage has pushed the machine toward overload.
- The player helps prevent a disaster, defeats the region's principal story antagonists in Cobblemon battles and assists the Rangers in stabilizing the research site.
- Final choice concerns *how to preserve the discovery*: secure the evidence with Rangers, allow independent scientific oversight, or advocate a conservation-first shutdown. These choices affect epilogue dialogue and optional future world events, **not** the eight badges, Champion access or access to key Pokémon.
- The region survives, but Resonance remains a source of open-ended research and exploration for seasonal content.

**Avoid:** mandatory one-off capture of an already quest-gated legendary or requiring experimental Minecraft action combat for the League storyline.

### Act V — Road to Champion

- The public recognizes the player's contributions, but the player's original childhood ambition remains unfinished.
- Victory Road, Elite Four **Koda, Seraph, Celeste, Vesper**, Champion **Magnus**.
- Magnus recognizes the strength required both to protect Pokémon and to become Champion.
- Region credits / celebratory unlocks and a curated set of **postgame activities**.
- Existing LegendaryQuests access rules remain authoritative; some expeditions can be introduced in postgame as recommendations without changing their unlock gates.

## Side-content branches

- **Ranger Expeditions:** rescue/capture/observe objectives with habitat-focused rewards.
- **Research Cases:** evidence collection, Pokédex milestones and optional lore entries.
- **Eclipse Investigations:** criminal cases, stolen Pokémon rescue, hideout infiltration.
- **City Stories:** local residents, businesses, NPC relationships, trainer challenges and reputation.
- **Legendary Journal:** existing LegendaryQuests discoveries can be referenced in text *read-only* when their APIs/tags are verified; no quest duplication.
- **Competitive and social activities:** appear as trainer-community invitations, not hard requirements for the story.

## Design rules for player choices

- Choose between short-term trust/reputation outcomes; do not break the main storyline or globally lock out gyms.
- Every branch converges before a compulsory story boss or vital next chapter.
- Never reward a destructive choice with exclusive competitive power.
- NPC conversations react to chapters, badges, prior conversations and optional relationships only when each signal has an authoritative owner.
- Existing player profiles need a safe entry/reconciliation path; no silent resets.

## Module 02 implementation specification (high-level)

Data-driven definitions (validated JSON/datapack resources) for:
- NPC identity to interaction binding and optional dialogue-tree IDs.
- Dialogue nodes, options and localized text, conditions and safe actions.
- Chapters, objectives, start/advance/completion hooks and retry/recovery.
- Non-destructive objective conditions: own facts, view of external facts, location/interaction/trainer-battle evidence when exposed by compatible APIs.
- Strictly single-owner progression writes through Module 01 `ProgressionService`; no unreviewed tag creation/deletion.
- Stable event IDs and persisted checkpoints for idempotency; rewards must use a separate appropriately authoritative payment adapter and ledger.
- Config staging and validation; reject corrupt/inconsistent content without destroying active definitions or save data.
- Integration to the **actual deployed** NPC mod after source/API discovery. If absent, provide a clearly marked admin-operated interaction harness for early engine tests, *not* falsely claim natural NPC interaction.

### End-to-end acceptance requirements

1. A genuinely fresh test player can follow Mom → Professor → Theo → Avery → Route 1 → first Gym progression after corresponding adapters are implemented.
2. Repeating any completed conversation does not repeat one-time rewards or advance a chapter incorrectly.
3. Player A and B independently progress through chapters; spectator/non-OP interaction security intact.
4. Logout/relogin and server restart preserve the precise objective and dialogue stage.
5. Changing reloadable story content cannot retroactively corrupt or silently skip active progress.
6. Players with existing League or LegendaryQuests progression can resume without wipes or duplicated unlocks.
7. Invalid config, missing NPC binding, crashed objective or unknown legacy fact gives actionable diagnostics and recoverable failure.
8. In-game performance and UI presentation are reviewed separately from compilation/logic test passes.

## World-build dependencies (can be placeholders during Module 02 development)

- Hearthleaf home and Professor's lab.
- Theo's trainer registration workstation and Avery's nearby field-kit handoff.
- Route 1 training area and accessible Verdant Gym.
- Later: ranger camps, Meridian facility, Eclipse outposts, ancient ruins and final Resonance Engine.
- Story positions and paths must come from editable per-world locations; hardcoded coordinates not allowed.

## Creative decisions still awaiting approval

- Region-wide phenomenon name **Resonance**, working title **The Resonance Crisis**.
- Faction names **The Meridian Institute**, **Team Eclipse**, **Pride Rangers**.
- New cast names and personalities: **Rin**, **Cael**, **Sable**, **Mira**.
- Whether major faction choices adjust reputation only or meaningfully branch optional quests.
- How explicit (versus subtle) the optional link to existing legendary lore should be.

**This is the approved narrative direction; the engine implements only the first dialogue slice at this stage. Later acts and final world/NPC integration are not yet implemented.**
