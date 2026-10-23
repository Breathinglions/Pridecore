# Pride Community v0.2 — Impactor 5.3.5 integration source

**STATUS: SOURCE / DEVELOPMENT PREVIEW, NOT A DEPLOYABLE MINECRAFT JAR.**
The core now contains a version-verified Impactor adapter and a durable reward
coordinator. The adapter compiled against the *actual supplied* `Impactor-Fabric-5.3.5+1.21.1.jar`
using Java 21. A full Fabric Loom/Minecraft 1.21.1 build and live server QA
remain undone because Fabric/Minecraft dependencies are not available in this
runtime. Do not upload this ZIP as a mod to PebbleHost.

## What is implemented
- Original trivia and Pokémon scramble engine; 55 independent question entries.
- First valid correct answer wins; malformed and spammy guesses do not win.
- Four trivia difficulties, distinct configurable money payouts, one daily cap.
- Wallet bridge invokes `EconomyService.instance()`, `currencies().primary()`,
  `account(currency, playerUUID)` and then `account.deposit(BigDecimal)`.
- Account future is handled asynchronously; deposit dispatches to server executor.
- Impactor transaction `successful()` must return true before advertising money.
- Crash-conservative append-only, fsynced reward ledger. A claim is PENDING before
  external deposit, then PAID or MANUAL_REVIEW. PENDING is NEVER automatically retried.
- Optional `/prideannounce` and `/pridequiz` source handlers for server messages.
- CRZQuiz, LuckPerms, v0.3.1 rank sprite pack, FTB Quests, and LegendaryQuests
  are **not** modified by this package.

## Rewards (disabled by default)
`config/pride_community/config.json` has:
```
"economyBackend": "IMPACTOR",
"payoutsEnabled": false,
"dailyCapPokeDollars": 2000
```
Proposed tier payouts in the same file: Scramble 150, Easy 100, Medium 200,
Hard 350, Ultra 500. These are *proposals* to be tuned against your economy.
Do not enable until production-grade multiplayer and crash-recovery tests pass.

## How to build AFTER installing prerequisites
1. Make a separate offline/testing server copy and keep CRZQuiz active until replacement validated.
2. Install Java 21, Gradle 8+, Fabric Loom and Fabric 1.21.1 build dependencies.
3. Copy your original `Impactor-Fabric-5.3.5+1.21.1.jar` to `libs/` without editing it.
4. Run `gradle build` (with internet for dependency resolution).
5. Fix any mappings/event ABI mismatches during real Minecraft compilation and testing.
6. Use a *staging* instance of your live-mod stack for multiplayer, restart, and wallet tests.
7. On eventual release, disable/remove CRZQuiz to avoid simultaneous quizzes.

## Offline QA, already run here
`bash qa/run_core_tests.sh` — **24 core tests passed**.
`bash qa/run_impactor_api_checks.sh /path/to/Impactor-Fabric-5.3.5+1.21.1.jar`
— **17 reward-coordinator tests passed**; the exact Impactor JAR API adapter
compiled successfully with javac 21.

Tests cover duplicate claims, limits, pending/unknown outcomes, timeouts,
restart recovery, winner selection, and API type compatibility. These are NOT
end-to-end live Impactor wallet tests.

## Important limits and deployment warnings
- The APIs are confirmed, but actual Impactor deposit behavior on your Fabric
  server has NOT been tested. Do NOT set `payoutsEnabled` true yet.
- Impactor's transaction logging is asynchronous; this ledger cannot make a
  single atomic transaction with Impactor's separate storage. Crash windows are
  recorded for manual reconciliation, never blindly repaid.
- If the ledger cannot be read or written, fail closed and do not make deposits.
- The current source has no persistent quiz leaderboards and does not subscribe
  to shiny, raid, or legendary APIs. `/prideannounce` is an admin broadcast tool.
- This source does not automatically reformat third-party broadcast messages.
- Player chat formatting and Poké Ball nameplates are still owned by LuckPerms,
  Styled Chat, and your existing resource pack.

## Next QA: checklist
- Compile the actual Fabric mod via Loom, then start it on a fresh 1.21.1
  server with Impactor 5.3.5, Cobblemon 1.8.1, LuckPerms and Styled Chat.
- With payouts disabled, test two normal players, chat listener ordering,
  simultaneous correct answers, 60-sec round, timeout, reload, disconnect.
- On a backed-up staging wallet, enable payouts, pay one known amount, compare
  before/after via Impactor account, verify the PAID ledger.
- Simulate provider failure and crash after deposit but before logging PAID;
  confirm PENDING requires manual review and cannot be claimed twice.
- Check balance under concurrent credits and server shutdown/persistence.
- Add rate-limited event adapters for shiny/legendary/raid announcements when
  their respective public APIs are inspected.
