# Module 02 v0.2.2-rc1 — Automatic Story Configuration Upgrade

**Status:** Staging release candidate, NOT a finished campaign or native NPC replacement.

### What changes when you replace the JAR

Install only `pride-core-0.2.2-rc1.jar` in `mods/` after making a full staging backup and removing `pride-core-0.2.0-rc1.jar`. Both builds share mod ID `pride_core`; never run them together.

On the FIRST STARTUP with the new JAR:

1. The mod loads **its bundled current** `story/default.json` from inside the JAR.
2. If `config/pride_core/story.properties` is missing, it creates it with `story_enabled=false`. If it exists, its contents and enabled value stay unchanged.
3. If `config/pride_core/story.json` is missing, the mod installs the bundled default.
4. If `story.json` exists, the mod validates it, then **adds** missing new NPC definitions, nodes/options, and required `requiresLegacy` onboarding checks from the bundled template; it never replaces existing NPC dialogue text, IDs, existing choices, story stages or other custom entries.
5. If anything is changed, an exact previous-byte backup is written beside the original:
   `story.json.backup-<sha256-prefix>.json`
6. The modified JSON is validated in memory and replaced by an atomic same-directory write. An invalid/malformed config remains untouched and the story module will refuse to start, leaving Module 01 and legacy systems unaffected.
7. Rebooting with the same content makes **no further changes**. If new bundled story content appears in a future JAR, it will be merged in the same way.

You **do not need to delete `story.json` or copy JSON by hand** to receive the new prerequisites.

### Existing NPC progression checks included in this build

| Story choice | Must already have server-side legacy marker |
|---|---|
| Sequoia → `understood` | `starter_chosen` |
| Theo → `next` | `trainer_card_received` |
| Avery → `journey` | `trainer_guide_received` |
| Ranger → `explore` | `first_catch` AND `first_battle_win` |
| Thorn → `challenge` | `league_started` |

These read-only markers come from the currently installed League/onboarding datapack. They are checked per player. **No item, starter, badge, tag or FTB Quest is awarded/modified by this feature**. FTB Quests Per Player remains responsible for quest completion and journal.

The new check only applies to attempts to select that dialogue option. Previously completed `pride:` facts from v0.2.0 are preserved and are **not retroactively proof** of genuine starter/registration/kit completion; a clean test player or manual reconciliation is needed for a realistic new-player playthrough.

### Staging verification

1. Save a backup of world + `config/pride_core` and document your current `/pridecore story status`.
2. Replace only the JAR; **do not** install it beside v0.2.0.
3. Start staging. Verify `/pridecore story status` shows `config=upgraded` (if old file changed), `config=created` (if missing) or `config=unchanged`. `story_enabled` retains its existing value.
4. As OP4 run `/pridecore story validate`. It should say how many NPCs were loaded.
5. Look under `config/pride_core/` for `story.json.backup-....json` (only if migration changed anything).
6. Confirm your original customized NPC dialogue survived.
7. With a genuinely fresh test account, try Sequoia's `understood` option **before** receiving starter through the real professor. It must not advance. After the old datapack's `starter_chosen` tag becomes valid, retry and check stage advances.
8. Repeat for Theo (trainer card), Avery (field kit) and the first catch/battle, using original services. Repeating the interaction must not grant a second kit/card/starter.
9. Reboot again and verify `config=unchanged` and previously saved stages persist. Compare FTB Quests Per Player, Pokémon party, legacy tags and Impactor balance before/after.
10. If anything breaks, stop staging, restore the backup JSON and revert to your previous JAR. The migration itself doesn't change player profile data, but newer story checkpoints may need special care when downgrading.

### Not finished in this release

- Native Cobblemon NPC dialogue GUI ↔ authenticated Pride Core story action bridge.
- FTB Quests Per Player objective event synchronization (no duplicate task updates).
- Actual Pokémon capture/battle event listeners vs only legacy completion tags.
- Full Resonance Crisis characters/chapters/world placement and League story handoff.
- Module 04 chat, badges, starter item changes, or extra Potions are NOT modified.

**This upgrade fixes configuration/version drift while we continue developing the complete Module 02.**
