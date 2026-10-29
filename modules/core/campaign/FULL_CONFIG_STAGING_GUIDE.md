# Pride Core — Configuration System & Staging Test Guide

**Build:** Module 02 v0.4.3-rc1, consolidated League + Story, configuration staging revision (October 29, 2026)  
**Compatibility target:** Minecraft 1.21.1 / Fabric / Cobblemon 1.8.1.  
**Status:** Automated build checks and in-game validation are separate. This kit is **NOT for production** until two-player staging acceptance.

## What goes where

| Config file | Loaded by | Edit and apply | What it controls |
|---|---|---|---|
| `config/pride_core/onboarding_rewards.json` | Pride Core | `/pridecore onboarding reload` | Theo/Avery starting items, quantities and modded items |
| `config/pride_core/story.json` | Pride Core | `/pridecore story reload` (after validation) | Narrative dialogue, choices and quest stage content |
| `config/pride_core/story_battles.json` | Pride Core | **Restart staging** | Nine storyline RCT Pokémon teams, levels, intro/win text. Battle authority, minBadges and encounter IDs are locked |
| `config/pride_core/presentation.properties` | Pride Core | **Restart staging** | Cosmetic sound, density, cue pacing, volume, pitch |
| `config/pride_core/story.properties` | Pride Core | **Restart staging** | `story_enabled`, allowed dimensions, simple particle registry overrides |
| `config/pride_core/rin_scene_anchors.properties` | Pride Core | **Restart staging** | Optional private Rin preview positions and world registry IDs (must be actual builder positions) |
| `config/pride_core/native_dialogue_text.json` | Offline generator, **not directly read by Java** | Run `build_native_dialogue_text_overlay.py`, then Minecraft `/reload` | Safe native Cobblemon **text and choice captions only** for Sequoia, Theo, Avery, Mom and Ranger Mira |
| `config/pride_core/league_balance.json` | Offline generator, **not directly read by Java** | Run `build_league_balance_overlay.py`, then Minecraft `/reload` | Official 13 native RCT trainer teams and existing first-win ₽ payout amounts |
| `config/ftbquests/quests/` | FTB Quests | Provider rules | Stable quest IDs/tasks/rewards; never overwrite the current quest book wholesale |
| Other installed mods' settings | Each owning mod | Provider rules | Vendors, Impactor wallets, RCT settings, LuckPerms, Flan, Battle Tower, etc. |

**The original Pokémon League and native Story datapack resources remain embedded in the unified server JAR.** Do not install duplicate standalone League/Story ZIPs. The two optional *text/balance* override ZIPs generated below are different: they intentionally override narrowly selected text or the native RCT data definitions, while leaving everything else bundled. Confirm their enabled datapack priority in the staging world before relying on the changes.

## Setup

1. Stop and back up a **separate staging server** (including the world's player/FTB and economy data, `mods`, `config`, `world/datapacks`, and all original RCT resources). Keep your production server untouched.
2. Remove the **old Pride Core JAR** from the staging `mods` folder. Install only the latest `UNIFIED-FULL-CONFIG-STAGING.jar` as the one mod with ID `pride_core`. Install the optional Pride Scenes Client JAR only on the test client.
3. Start staging. The mod generates `onboarding_rewards.json`, `story_battles.json`, and `presentation.properties` when absent. It does not overwrite existing versions; keep the original service NPC tags/spawns intact.
4. In `config/pride_core/story.properties`, set `story_enabled=true` and restrict `story_allowed_dimensions` to your actual staging dimension. Restart.
5. Verify `/pridecore story status`, `/pridecore story admin runtime-preflight`, and `/pridecore onboarding config-status`. On a non-OP fresh staging player, test Mom → Sequoia's native starter selector → Theo registration → Avery field kit → Ranger Mira and Thorn.
6. To change Theo or Avery items, edit their `items` array with a **registered** mod item ID and count (1–64 per entry, max 32 unique entries per kit and 512 total items). Use `/pridecore onboarding reload`. The original Trainer Card, Poképedia recovery rules, and FTB progress remain owned by the original native functions.
7. To edit nine storyline trainer rosters, change `story_battles.json` and restart staging. The server rejects edits to encounter identity, badge gating, or Pokémon-only combat mode. Never set badge tags manually.
8. To adjust presentation, edit `presentation.properties` and restart. Valid keys: `enabled`, `sounds`, `particle_scale` (0–2), `cue_time_scale` (0.5–2), `sound_volume` (0–1), `sound_pitch` (0.5–2). Only currently implemented particles/sounds/timed actionbar cues respond to these; there is **no completed camera/model cinematic controller** yet.

## Native dialogue text without rebuilding the JAR

The official Sequoia/Theo/Avery dialogues belong to Cobblemon's native datapack parser. Changing a random `config` file alone cannot change those assets. The supplied generator makes a strictly **text-only** datapack override.

From the **server root** (adapt the filenames/paths for your host):

```bash
python3 build_native_dialogue_text_overlay.py \
  --jar mods/pride-core-0.4.3-rc1-UNIFIED-FULL-CONFIG-STAGING.jar \
  --config config/pride_core/native_dialogue_text.json \
  --out world/datapacks/PrideNativeDialogueText.zip
```

On first use the script generates the complete editable JSON with existing lines and choice captions. Edit it and run the same command again. The script refuses changes to speaker expressions, starter selection, `isVisible`, completion actions, or native reward commands. Run `/reload` and verify the datapack is enabled with correct priority. Do not edit the output ZIP by hand. A configured text override will not automatically trigger the unfinished animated Rin handoff; Rin's introduction remains part of Sequoia's native dialogue.

## Official League teams and payouts without a second reward engine

```bash
python3 build_league_balance_overlay.py \
  --jar mods/pride-core-0.4.3-rc1-UNIFIED-FULL-CONFIG-STAGING.jar \
  --config config/pride_core/league_balance.json \
  --out world/datapacks/PrideLeagueBalance.zip
```

The initial config is generated from the exact **13 original RCT trainer definitions**. You may adjust teams (registered species, levels and moves) and existing numeric first-win ₽ payouts (1–1,000,000). The script preserves each trainer's original RCT handler, eligibility, badge/FTB/Hall-of-Fame commands and win-tag protection. It refuses adding a payout to an originally non-paying trainer, removing an existing payout, or injecting custom commands. RCT/mod compatibility and Pokémon/move registry IDs must still be validated **in game**. Run `/reload`, check priority, and test repeat wins for duplicate payouts. Disable/remove the override pack before rolling back the modified balance.

## Staging acceptance tests

- **Native starter:** both Sequoia selection paths open the original Cobblemon GUI; cannot claim two starters. Rin's text introduction appears, with a separate OP-only visual preview for his unfinished animated actor.
- **Kit safety:** fresh non-OP players receive exactly one configured Theo kit and one Avery kit; failed inventory capacity or invalid item prevents first-time completion; a second player gets only their own kit. Confirm FTB tasks, original `trainer_card_received` / `pokepedia_received` / `trainer_guide_received` tags and Avery's RCT series.
- **One League NPC per character:** use original Thorn/Rook/etc., never a duplicate Story Gym NPC. Test all eight Gym victories, Elite Four and Champion with original RCT battle and reward authority.
- **Story teams:** edit one staging roster, restart, verify the actual RCT battle launches the changed party; attempt changing `minBadges` and confirm server rejects it.
- **Presentation:** set particle density to 0 on staging; restart and confirm visuals are suppressed but real Pokémon-only battles and stage progression continue.
- **Native text:** change one line in Sequoia's dialogue; regenerate and `/reload`; confirm that line changes *without* affecting either starter UI action, Theo/Avery grants, or a Gym reward.
- **League balance:** change one Gym payout, `/reload`, win once on a fresh staging character, verify accurate currency and UI text, and confirm repeat wins do not pay again; revert after test.
- **Persistence:** reconnect, restart, repeat NPC interactions, test two players simultaneously and verify quest/badge state, inventories, server logs and economy entries. Existing progress must not silently reset or get new items after a config update.

## Boundaries and outstanding work

The native dialogue/League balance settings use **regenerated Minecraft datapack overrides**, not automatically hot-loaded JSON. This is intentional: Cobblemon and RCT own those definitions. User-written scripts cannot safely replace server event authorization. The existing M01/M02 server still has outstanding in-game native RCT battle verification, the automated private Rin starter-cinematic handoff, physical world placement, all 13 League single-NPC UX audits, and crash-transaction-safe starter kit delivery. FTB Quests changes require a selective stable-ID merge. Do not call this production-ready on CI/build results alone.