# Pride Core 0.4.3-rc1 — Unified League + Hearthleaf Story Staging

**Staging-only.** This supersedes the non-unified `pride-core-0.4.3-rc1.jar` distributed for the first scene-preview pass. The plain CI-generated JAR contains **no** original League datapack. For full-story staging, use the separately verified `pride-core-0.4.3-rc1-UNIFIED-STAGING.jar` instead.

## Reproducible packaging

Source: `modules/core/campaign/build_unified_module02_dialogue_v043.py`.

Requires the exact original `PokemonLeague7.10.10_FTB_Only_CobbleBadges_Removed_STAGING (2).zip`, SHA-256 `ca16bbfc591519144d6dc53993f84106b546478df471bf30f913aa05791b3a3f`, and the current `PrideStoryNative_0.3.5-rc1.zip` CI asset, plus the freshly compiled `pride-core-0.4.3-rc1.jar`. The audited League ZIP is stored privately by the project owner; it is **not** committed to GitHub or fabricated by the build script.

```bash
python3 modules/core/campaign/build_unified_module02_dialogue_v043.py \
  --jar /path/to/pride-core-0.4.3-rc1.jar \
  --league-zip /path/to/PokemonLeague7.10.10_FTB_Only_CobbleBadges_Removed_STAGING.zip \
  --story-zip /path/to/PrideStoryNative_0.3.5-rc1.zip \
  --out /path/to/pride-core-0.4.3-rc1-UNIFIED-STAGING.jar
```

The verified October 29 staging file SHA-256 is `7c6eed879a46b49d64f66455a005e93e8b92c11b52b38764729dafcbf511da9d`. Packaging inspection verified **287 datapack resources**, **13 original RCT trainers**, **20 standalone narrative-only NPC types**, all **nine clues**, and **412 actual-archive compatibility assertions**.

## Narrative unified without service rewrite

- Mom introduces Professor Sequoia in Hearthleaf and reminds the player they can return home.
- Professor Sequoia explains Pokémon/environment bonds, hints at unsettling changes, introduces **one canonical Rin** through the original native dialogue and retains both `openstarterscreen` paths.
- Theo retains the original Trainer Card and Poképedia granting function and bridges the chapter to Avery.
- Avery retains the original one-time Field Kit service (existing configured 10 Poké Balls + food) and points toward Ranger Mira, Route 1, and eventually Thorn's Verdant Gym.
- Ranger Mira offers the first glimpse of the unsettled woods, without explaining the full Resonance mystery prematurely.
- Each original `isVisible` and native service command is preserved; one read-only `pridecore story sync` is appended after each of the three native service completions. The original first-win RCT Thorn callback retains all commands with an additive read-only sync.
- The original League NPC, team, payout, badge, FTB/Champion Hall-of-Fame definitions stay authoritative. No story-specific physical Thorn is generated. Do not create separate Story Thorn or League copy NPCs.
- Native Rin text introduction is included; the optional **client-only villager projection remains an OP4 staging preview**. Automatic animated Rin handoff to Cobblemon starter GUI is **not yet implemented**.

## Installation rules

Install only **one** `pride_core` mod JAR on an isolated staging server and continue using the existing Fabric/Cobblemon/RCT/FTB/Impactor mods. Because the unified staging JAR bundles both League and native Story data, remove/disable **only** their duplicate standalone ZIPs from the staging world's datapacks; leave unrelated packs unchanged.

Optional `pride-scenes-client-0.1.0-preview1.jar` goes only in the test client's mods. FTB `PrideStoryFTB_0.3.5-rc1.zip` is an external SNBT update kit, **not a Minecraft datapack**, and must be selectively reviewed/merged with backups of the current player quest configuration.

Change staging `config/pride_core/story.properties`: `story_enabled=true` and `story_allowed_dimensions` to the exact test dimension registry ID. Production remains disabled/unmodified.

## Release checks

Run `/pridecore story status`, `/pridecore story validate`, `/pridecore story admin runtime-preflight`, then play actual non-OP onboarding through the original NPCs. Confirm starter service and first-time kit/card occur once. Confirm original RCT Thorn battle and one-time `league_grass`/FTB/₽1,000 and subsequent League trainers. Check a second player, relog, other dimensions, restart and the cinematic preview. No build-only claim of live playability.

Automated test: `python3 modules/core/qa/test_unified_onboarding_dialogue.py`; generated synthetic fixtures verify native original action retention. **Staging gameplay is mandatory before production rollout.**
