# Pride Core 0.4.0-rc1 — Unified League + Story Packaging (STAGING ONLY)

**This is an integration prototype, not a live server-approved release.** It packages the exact existing Pride Core v0.3.8 Java classes with the original League 7.10.10 and v0.3.5 Pride Story server-data content. It is **not** a rewrite of the Java handlers. Do not claim that a TBCS duplicate Thorn reward path is fixed by this packaging step. The original RCT trainers are authoritative.

## Exactly what changed

- One Fabric mod JAR with mod ID `pride_core`, metadata version `0.4.0-rc1`.
- Contains **291 data entries**: 191 unchanged League resources + 103 Story resources - 3 intentionally overlapping Lab dialogues. Of the overlaps, the Pride Story variant is included because it appends reward-free story sync to the original service choices.
- Retains all **13 RCT trainer/team and original win-command definitions**; no teams, moves, level caps, tags, items or payouts changed.
- Retains 24 Cobblemon native story NPC definitions, 27 hidden advancement definitions, dialogue and NPC spawn functions.
- Retains 0.3.8 story/command Java code, including the separate test-era Pride Story Thorn encounter. **Do not use the Pride Story Thorn as the final battle authority**; continue to use the original RCT Thorn while validating consolidation.
- The bundled `/data` resources are always-on mod resources on Fabric 1.21.1. In `/datapack list` they appear under the `fabric` mod resource pack, not two named standalone pack entries.

## Staging migration (STOP SERVER FIRST)

1. **Full backup:** world folder(s) including player profiles, tags/advancements, `world/data/pride_core/`, `mods/`, existing world's `datapacks/`, `config/pride_core/`, `config/ftbquests/quests/`, and permissions/economy data. Never wipe those. Keep a copy of original League 7.10.10 and current Pride Story 0.3.5 ZIPs.
2. On **staging**, move aside the old `pride-core-0.3.8-rc1.jar` (or any other Pride Core JAR). **Only one JAR with mod ID `pride_core`** can be installed.
3. Move aside **only** the standalone Pokémon League 7.10.10 ZIP and Pride Story Native NPC ZIP from the ACTIVE world's `datapacks/` folder. Do not delete or disable Legendary Expeditions, worldgen, economy, unrelated packs, or FTB quest folders. If there is a second Pride datapack copy or an overriding world pack, audit priority before going further.
4. Put `pride-core-0.4.0-rc1-UNIFIED-STAGING.jar` in `mods/` and start staging. Keep Fabric API, Cobblemon, RCT, TBCS, Impactor, FTB Quests and all current independent prerequisite mods.
5. Confirm **one Pride Core mod** loaded. Run `/datapack list enabled`. Expect to see Fabric mods resources; old standalone League/Story ZIPs should not be listed. **Don't use `/datapack disable fabric`.**
6. Confirm function *autocomplete* exists for `prideregion:league/load` and `pride_story:spawn/mom`. **Do not execute** the win-reward functions manually. Check Sequoia/Thorn NPC definitions, RCT team registration and logs. Avoid spawning duplicate NPCs.
7. Use a non-OP staging account or temporarily de-OP your account. Confirm Mom → existing Sequoia starter → Theo → Avery → Ranger, no second starter/kit. Verify single NPC click and FTB quest completion.
8. With existing original RCT Thorn: verify the Grass battle and `league_grass`, FTB Gym 1, reward (once), and `pride:verdant_victory` after `/pridecore story sync`. Use original RCT trainer as the authoritative battle. Verify no payout on simple dialogue and no duplicate payout after repeating.
9. Verify Rook → Nyx, four Elite Four and Magnus in a controlled test with original trainer definitions. Hall of Fame and champion callback must work exactly as before. Test multiplayer independently before production. Test load/restart to confirm persisted stage and one-time grants.

## Rollback if anything breaks

Stop staging. Remove the unified JAR. Restore the previous Pride Core v0.3.8 JAR and both ORIGINAL standalone League and Story datapack ZIPs to their former locations, including enable state and priority. Restart. Do not touch player progression files or FTB Quest IDs. Backups matter if players earned new rewards during testing.

## Still outstanding before 'one system' is production-ready

- Story events for Acts I–IV must be authored and connected to actual investigations, puzzles and bosses.
- Remove the duplicate Pride Story Thorn encounter and consolidate the existing RCT NPC into the single player-facing story/battle experience. Repeat for all 13; do not create new reward handlers until the originals are fully validated.
- Review *all* other Pride-specific datapacks / LegendaryQuests / custom gameplay systems; only League 7.10.10 and Pride Story 0.3.5 were merged here.
- User-editable configs: `config/pride_core/story.json` exists, but source RCT teams are inside the JAR. A later release should ship generated external editable content and implement validated loading/overrides, not force JAR recompilation for changes. FTB Quests SNBT files already remain editable outside the JAR.
- In-game launch verification for bundled RCT, Cobblemon NPC and Minecraft function/advancement loaders is not performed in this offline packaging pass.

## Integrity

Expected input hashes are pinned by `build_pridecore_unified.py`. Jar includes `pride_core_unification_manifest.json` with source SHA-256, counts and each packaged resource hash.