# Pride Core Module 02 — Story & NPCs — v0.2.0-rc1

**STATUS: ENGINE RELEASE CANDIDATE — STAGING ONLY.** Module 01 is included as a dependency in this *single JAR*. Test Module 01 separately with v0.1.0-rc1 if desired, but **do not install both JARs simultaneously**: both use Fabric mod ID `pride_core`.

## What is implemented

- Validated JSON-driven character dialogue trees, choices and prerequisites.
- Minecraft right-click dialogue for explicitly tagged staging entities using Fabric's `UseEntityCallback` (no blanket interception of installed NPC mods).
- Safe in-process session tracking per player; disconnect, cancel and reload invalidate conversations.
- Module 01 progression service for UUID-persistent story stages, checkpoint facts and atomic changes.
- Non-OP-friendly player dialogue choices; OP4-only NPC simulation and data reload.
- A default **Hearthleaf playable story slice** involving Mom, Professor Sequoia, Technician Theo, Avery, Ranger Mira and Thorn of Verdant Gym.
- `/pridecore story reload` loads and validates a full replacement before swapping; invalid files keep the last working definition.
- Existing starter selection, Theo's actual trainer registration, Avery's actual field kit, FTB Quests, Gym battle rules, LegendaryQuests, economy and player tags are **not changed**.

## Enable only on disposable staging

1. Back up world + config. Keep `pride-core-0.1.0-rc1.jar` archived but **remove it from `mods/`** before installing `pride-core-0.2.0-rc1.jar`.
2. Start server once; `config/pride_core/story.properties` appears with `story_enabled=false`. NPCs are untouched.
3. Stop staging; set `story_enabled=true` in `config/pride_core/story.properties`, then restart. This creates `config/pride_core/story.json` (editable content definitions).
4. Confirm `/pridecore status` and `/pridecore story status`. `/pridecore story reload` applies edited JSON without a server restart.
5. **Use disposable vanilla actors**, not the real professor/gym trainer or live mod NPC, until compatible callbacks are audited. For example summon a peaceful tagged villager in the staging test world (OP command):
   `/summon minecraft:villager ~ ~ ~ {Tags:["pride_npc_mom"],NoAI:1b,Invulnerable:1b,PersistenceRequired:1b}`
   Repeat with `pride_npc_sequoia`, `pride_npc_theo`, `pride_npc_avery`, `pride_npc_ranger`, `pride_npc_thorn`.
6. Right-click Mom, read dialogue, then run `/pridecore story choose begin`. Or use OP4 `/pridecore story admin talk <player> mom` to drive the test harness without tagged villagers.
7. Repeat for `sequoia` → `understood`; `theo` → `next`; `avery` → `journey`; `ranger` → `explore`; `thorn` → `challenge`.
8. At checkpoints run `/pridecore profile`, `/pridecore story status`, `/pridecore admin facts <player>`. Disconnect and restart mid-chain to verify persistent progression.
9. Repeating a completed conversation should produce repeat dialogue, not reissue progression and **never issue any existing Cobblemon/FTB/Impactor rewards**.
10. Use a second non-OP player to verify isolation, and confirm existing League tags/Pokémon inventories remain unchanged.

## Commands

- `/pridecore story status` — report whether module is active and your persisted stage.
- `/pridecore story choose <choice-id>` — select a response from an active dialogue.
- `/pridecore story cancel` — leave dialogue.
- `/pridecore story reload` — OP4, validated config hot reload, resets only ongoing dialogue sessions.
- `/pridecore story admin talk <player> <npc-id>` — OP4 test harness to initiate NPC dialogue.
- Module 01 original commands remain available unchanged.

## Safety and known limits

- **Current NPC integration is limited to explicitly tagged staging entities.** We have not inspected how the live NPC mod intercepts right-clicks. Tagging existing Sequoia/Theo/Avery NPCs could suppress their normal starter/registration/kit interaction, so do not do this until their adapters are verified.
- Chapter tracking is via story stages and flags; no physical route-completion event listener, trainer battle detector or genuine starter ownership detector exists in this release. The textual six-character acceptance slice validates narrative flow, not live reward/badge authority.
- The story engine deliberately does not grant a starter, kit, Pokedex, badge, PokéDollars or LegendaryQuests completion. Those integrations require inspection of the installed providers.
- Dialogue is in ordinary Minecraft messages for now; Module 04 Chat & Identity remains frozen.
- No generated world assets, maps or custom character models.
- Story definitions have bounded sizes and a stable format. Reloading resets ongoing conversations, **not persistent progress**. Revisions and owned facts remain in Module 01's storage.
- Invalid/corrupt profile records fail closed; a maliciously configured story still requires admin write access to its JSON. Do not expose config directory to players.
- Compiled/CI-passing means **code tested**, not verified on the full live modpack.

## QA gate

Successful GitHub build + regression tests + actual non-OP entity interaction + full story dialogue flow + disconnect/restart persistence + reload preservation + existing mods regression. Promote to 'In-Game Verified' only when the staging game test passes.
