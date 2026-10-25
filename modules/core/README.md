# Pride Core 0.1.0-alpha.1 — Progression Foundation

**STAGING ONLY — Minecraft 1.21.1, Fabric API 0.116.17, Java 21.**
This is a new independent module of the existing Pridecore repository. It deliberately does **not** alter `pride-flashpoint` on main, `pride-community` on the community development branch, or existing live datapacks.

## Implemented
- Per-player UUID-based immutable profile state with stage, owned facts and monotonically increasing revision.
- CRC32 integrity check and atomic revision-checked per-profile disk commits under `<world>/data/pride_core/players/`.
- Server-side data validation; corrupt/unrecognized records cannot be silently overwritten.
- Read-only diagnostic view of *existing* `league_*` / `pride_*` command tags; no import or sync occurs.
- Read-only by default: `config/pride_core.properties` contains `allow_admin_mutations=false`.
- Admin-only write test commands require permission level 4 **and** an explicit flag.
- No auto rewards, badge replacement, currency changes, new NPC spawns, or global tag changes.

## Operator commands
- `/pridecore profile` — inspect your own Pride Core profile
- `/pridecore status` — OP4: mod status/write mode
- `/pridecore admin inspect <online-player>` — OP4: profile summary
- `/pridecore admin legacy <online-player>` — OP4: legacy command tags (read-only)
- `/pridecore admin stage <online-player> pride:met_mom` — OP4: *requires enabled staging mutations*
- `/pridecore admin grant <online-player> pride:intro_done` — OP4: *requires enabled staging mutations*
- `/pridecore admin revoke <online-player> pride:intro_done` — OP4: *requires enabled staging mutations*

Story stages and facts must use `pride:` IDs. Do **not** grant synthetic gym badges or legendary completions: those remain controlled by the existing mods/datapacks.

## Building
From `modules/core`, run `gradle --no-daemon clean build` using Gradle 8.12.1 and Java 21.
Pure Java regression tests: `bash qa/test-core.sh`.
The accompanying GitHub Action builds and uploads the remapped Fabric JAR.

## Staging smoke test
1. Back up your server's entire world and config directories; use a disposable staging copy.
2. Install **only the new `pride-core-0.1.0-alpha.1.jar`** in addition to (not instead of) existing mods.
3. Restart; verify `/pridecore status` says writes=false and `/pridecore profile` loads.
4. Inspect an OP and non-OP with legacy tags; confirm those tags have **not changed**.
5. On disposable staging **only**, stop server and edit `config/pride_core.properties` to `allow_admin_mutations=true`, then restart.
6. Stage test commands above, repeat grant, verify no revision increase, restart, verify persistence.
7. Deliberately disconnect/reconnect; check other player's state stayed independent.
8. Restore read-only mode and restart. Do not deploy to production until in-game tests pass.

## Architecture boundaries
Pride Core currently owns **only** `pride:` profile facts/stages. It does not own FTB League achievements, existing player tags, Impactor currency, Cobblemon party data, or LegendaryQuests. No automatic mapping from legacy tags occurs. A future reviewed adapter will translate external signals to read-only view models before selective migrations.

## Known limitations
- OP-only diagnostic/admin tools, not a player-facing Story Director yet.
- No source verification from Minecraft runtime until CI build + staging walkthrough are complete.
- Atomic rename requires filesystem support. Unavailable storage yields an error (no non-atomic fallback).
- Only online players supported by admin commands.
- No background threads or asynchronous IO. A future batch migration will need bounded, scheduled processing.
