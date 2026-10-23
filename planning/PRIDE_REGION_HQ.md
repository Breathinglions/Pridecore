# Pride Region — Development Headquarters
> **Shared planning home · October 22, 2026** | Pride Region MMORPG / Pride Core 

[**Source code and Flashpoint engineering**](https://github.com/Breathinglions/Pridecore) · [**Engineering Issues**](https://github.com/Breathinglions/Pridecore/issues) · [**Notion-ready task database (CSV)**](./NOTION_IMPORT_TASKS.csv)

## Vision
Build **Pride Region** into a coherent, quest-first Cobblemon MMORPG. Keep the working live server, 8-Gym League, LegendaryQuests and economy stable. Extend it with custom Pride Core features, a high-quality rebuilt world, repeatable endgame PvE, unified UI, a thriving player economy, and compelling reasons to return each week.

## Status legend — do not confuse with finished
- **Done · tested**: core flow explicitly reported tested in game (not a comprehensive release audit).
- **Done · reported**: reported installed/working, but comprehensive regression test not recorded.
- **Building**: being designed or developed, not fully shipped.
- **Testing**: staging verification, QA or remediation outstanding.
- **Planned**: not implemented; roadmap only.
- **P0 / P1 / P2**: must-fix or must-build first / near-term / subsequent.

## Where we stand (58 tracked deliverables)
- **Done · reported: 11**
- **Done · tested: 8**
- **Testing: 4**
- **Planned: 29**
- **Building: 6**

**Workstream coverage**

| Workstream | Tracked items |
|---|---:|
| Server & infrastructure | 8 |
| World & exploration | 7 |
| League & progression | 4 |
| Legendaries & story | 8 |
| Economy & minigames | 7 |
| PvP & live events | 3 |
| Pride Core · PvE | 9 |
| UI & identity | 6 |
| Launch & QA | 6 |

## Immediate engineering priorities
- **Flashpoint procedural Standard dungeon foundation v0.3.2** — Building; Complete in-world integration tests; do not deploy to live world ([reference](https://github.com/Breathinglions/Pridecore/blob/main/README.md))
- **Resolve orphaned Flashpoint dungeon geometry / recovery journal** — Testing; Verify cleanup on actual generated blocks and reloads ([reference](https://github.com/Breathinglions/Pridecore/issues/1))
- **Track encounter-owned Pokémon UUID through restarts** — Testing; Test exact owner UUID identity and safe cleanup without touching wild Pokémon ([reference](https://github.com/Breathinglions/Pridecore/issues/2))
- **Replace simulated boss results with native Cobblemon callbacks** — Building; Detect battle win, capture, wipe, despawn and correct party ownership ([reference](https://github.com/Breathinglions/Pridecore/issues/2))
- **Mandatory per-build code and data self-audits** — Planned; Review crash recovery, exploit resistance, concurrency, version pins and tests

## Next development increments
1. **Safety first:** close the Flashpoint geometry/recovery issue with a *real Minecraft test*, then prove UUID ownership and native Cobblemon boss event resolution. Simulation is not gameplay.
2. **Gameplay vertical slice:** one Standard Flashpoint with actual objectives, party ownership, win/fail/reset, safe reward and restart recovery; test for two simultaneous parties.
3. **World rebuild pilot:** block out a metropolis layout and one connecting route in a *new Multiworld dimension*, then validate quest accessibility, navigation and population before discussing overworld replacement.
4. **UI integration:** keep LuckPerms authoritative; prototype rank/nameplate/chat and a Trainer Card hub with native-client fallbacks.
5. **Story cadence:** proceed from the completed Hoenn Regis into Latias/Latios; maintain variety and multiplayer test cases.

## Current completed foundation
- **League:** onboarding NPCs; all eight gyms in sequence, Elite Four and Champion through FTB Quests/tag authority.
- **World:** Pride City and the Transit Center; managed Resource, Nether, End and Aether world policies; BlueMap.
- **Legends:** Kanto five core encounters, all six Johto, Darkrai, and the three Hoenn Regis (the latter is reported done; wider QA remains).
- **Economy & activities:** Impactor economy, casino slots/blackjack/chip wallet flow, 1,253-trainer first-win payouts, manual raid capture testing.
- **PvP:** APS ranked flow and HTTPS service reported working.
- **Modpack:** Pride Region CurseForge pack published/approved earlier; compatibility audit remains recurrent.

## What is explicitly NOT production-ready
- **Flashpoints v0.3.2 is staging only.** Boss progress currently uses simulated admin transitions. Real battle/capture attribution and reward outcomes are not implemented. Read the [technical README](https://github.com/Breathinglions/Pridecore/blob/main/README.md).
- **Orphaned block recovery** remains tracked in [issue #1](https://github.com/Breathinglions/Pridecore/issues/1).
- **Tracked encounter UUID and boss lifecycle** still need integration validation, per [issue #2](https://github.com/Breathinglions/Pridecore/issues/2).
- Operations, World Bosses, GUI hub, replacement overworld and the expanded market systems are planned, *not shipped*.

## Architecture decisions
1. Fabric 1.21.1 + Cobblemon 1.8.1 compatibility is the current pinned target; verify exact APIs before shipping.
2. Pride Core should have shared instance/encounter primitives with modular **Flashpoints**, **Operations**, **World Bosses**, and **UI/Identity** components.
3. Keep **LegendaryQuests** story encounters intact and distinct from repeatable endgame rewards.
4. Keep **FTB Quests/server tags** as story/League authority and **LuckPerms** as rank authority. Avoid double configuration.
5. All new world/dimension and dangerous state changes happen in disposable **staging copies**, never on production first.
6. Require a release self-audit: permissions, concurrency, persistence, crashes, abuse/duplication, load, integration and rollback.
7. A refreshed overworld may ultimately replace the current one *only after* design and migration checks are signed off.

## Collaboration workflow for Manuel and Evan
- **Source of truth:** task database with one owner, status, next action and link per task. This file is the roadmap narrative, not a second task tracker.
- **Review cycle:** at least weekly, both review **P0**, **Testing** and top **P1** items; leave decisions in meeting notes.
- **Moving a task to Done:** attach a video/screenshot/log/build ref, note Minecraft/Cobblemon versions and test OP/non-OP when relevant.
- **Deployment:** branch → compile/CI → local test → staging multiplayer/restart test → rollback preparation → production; no staging JARs on live.
- **Collaboration:** assign each new task to Manuel or Evan only after you agree; existing backlog stays unassigned.

## Use in Notion
1. In Notion, create page **Pride Region — Development HQ** and paste this document as its project briefing.
2. Import [NOTION_IMPORT_TASKS.csv](./NOTION_IMPORT_TASKS.csv) as a database (not pasted text).
3. Add a **Board** view, grouped by **Status**; other recommended views: **P0 / P1 priority**, **By workstream**, **Release QA**.
4. Invite Evan to that page with **Can edit** permission; keep it private unless you want the entire roadmap publicly visible.
5. Store build artifacts, designs, issue links and evidence on the individual task pages.

_Last updated: October 22, 2026. Statuses use user-reported milestones and the current GitHub Pridecore README/issues; they are not independently verified against a live production installation._
