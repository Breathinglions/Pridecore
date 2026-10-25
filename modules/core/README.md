# Pride Core — Module 01: Foundation & Player Progression

**Module completeness target: Release Candidate, staging-only.**
Minecraft 1.21.1 / Fabric API 0.116.17 / Java 21 / Cobblemon 1.8.1 baseline.

## Scope (included in this module)

- UUID-keyed immutable player progression with a namespaced Pride-owned story stage and up to 512 owned facts.
- One transaction gateway: `ProgressionService` returns persisted changes and supports atomic multi-field transitions within one revision.
- CRC-checked, atomic-write per-profile storage under `<world>/data/pride_core/players/<uuid>.bin`.
- Fail-closed load/commit behavior on corrupt or unknown records; revision conflict detection; replay/no-op idempotency.
- Read-only legacy command tag inspection; **no automatic conversion, migration or writes** to FTB, League, LegendaryQuests, Cobblemon, economy, player tags, world protection or rank records.
- Named provenance in `ProgressionView` distinguishes `pride:` owned facts from `league_*` / `pride_*` legacy tags.
- Central in-process service accessible to later modules through `PrideCoreMod.progression(server)`. Other modules must not construct competing stores.
- OP4 diagnostics and staging-only admin mutation commands. Server installs with mutation flag **false** by default.
- Pure Java regression suite and Fabric CI build. **Tests passing does not demonstrate in-game compatibility.**

## Commands

| Command | Permission | Meaning |
| --- | --- | --- |
| `/pridecore profile` | Player | Read your own owned stage and legacy-tag count. |
| `/pridecore status` | OP4 | Version, service readiness and mutation mode. |
| `/pridecore admin inspect <player>` | OP4 | Snapshot of profile counts/revision. |
| `/pridecore admin facts <player>` | OP4 | First 16 Pride-owned fact names. |
| `/pridecore admin legacy <player>` | OP4 | First 16 read-only external tags. |
| `/pridecore admin stage <player> pride:met_mom` | OP4 + staging write flag | Test an owned story stage. |
| `/pridecore admin grant <player> pride:intro_done` | OP4 + staging write flag | Test an owned fact. |
| `/pridecore admin revoke <player> pride:intro_done` | OP4 + staging write flag | Revoke a test fact. |

No badge or legendary completion should be faked or copied using these commands. They DO NOT award money, run NPC commands or unlock existing dimension gates.

## Testing and deployment

Run `bash qa/test-core.sh` and `gradle --no-daemon clean build` from `modules/core`. GitHub Actions uploads the Fabric JAR when both pass.

1. Obtain an isolated **staging backup** containing world + config. Keep the live server unchanged.
2. Install the new JAR with existing server mods in staging; remove any duplicate `pride_core` mod IDs. Do not remove existing Flashpoint or Community code.
3. Start and check `/pridecore status` is active and admin mutations disabled.
4. Snapshot before/after original tags, FTB Quests progress, LegendaryQuests progression, Impactor wallet and LuckPerms role. No changes should occur.
5. Test `/pridecore profile`, `/pridecore admin inspect`, `/pridecore admin legacy` as OP and normal player; OP commands must be denied to normal players.
6. Stop staging, change `config/pride_core.properties` to `allow_admin_mutations=true`, restart.
7. Change a **test account's Pride-owned** stage and facts; test repeat grant, revoke, reconnect and restart. Confirm unchanged revision after repeated grant.
8. Stop and revert flag to `false` before any rollout. Confirm a normal player cannot edit profile progression.
9. Corruption testing belongs in a disposable duplicate copy ONLY; never deliberately corrupt real player files.

## Known limitations and boundaries

- **Release Candidate, NOT production-approved:** in-game staging acceptance must pass.
- Per-file CRC is accidental-corruption detection, not tamper-proof cryptographic integrity.
- Atomic moves require filesystem support. Journal directory power-loss durability is not independently guaranteed.
- All operations are synchronous; call via server main-thread, not asynchronous callbacks. Not safe for multiple server JVMs writing the same world.
- `ProgressionService` provides transactional **owned fact updates**, not exactly-once cross-mod economy payouts. Implement durable transactional outbox/reconciliation when bridging external rewards.
- Future Story Director, League bridge, NPCs, UI and chat are explicitly outside Module 01 and remain unchanged.
- Only online players can be targeted by commands. Programmatic UUID reading is supported.
- Configuration changes to mutation permissions require restart. Do not expose writable config to untrusted users.

## Module 01 acceptance gate

CI compile, regression tests, second-player isolation, read-only legacy tag behavior, restart persistence, permission denial, corrupt-storage refusal, resource/world compatibility, backup restore, and no live data mutations. Only after that gate can Module 01 be called **in-game verified**.
