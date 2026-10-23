# Pride Community v0.3 — persistent leaderboard + rank-colored winners

- New command: `/pridequiz leaderboard` (top 10, available to any player).
- Every first winning round records one durable result in `config/pride_community/leaderboard.tsv`.
- Counts trivia, scramble and overall wins, across server restarts.
- A duplicate winning round ID cannot be counted twice.
- Leaderboard errors fail closed; protect the original TSV for recovery.
- Winners inherit their **existing LuckPerms prefix** including custom Pride Region ball/badge glyphs; username colors use rank, with safe fallback if LuckPerms is unavailable.
- The command works for offline winners, although an offline player's live LuckPerms prefix may be unavailable.
- The existing rank sprite resource pack, Styled Chat, League, LegendaryQuests and Impactor remain unchanged.
- Economy rewards are still **disabled by default**. This update does not endorse enabling payouts.
- No automatic shiny/raid/boss listener is implemented yet.

## Minecraft smoke test

1. In a staging server, stop the server and replace **only** the old `pride-community-0.2.0-dev.jar` with `pride-community-0.3.0-dev.jar`. Do not leave two Pride Community JARs.
2. Restart and confirm no error in startup logs.
3. Run `/pridequiz leaderboard`: initially no winners if data is new.
4. Run `/pridequiz force scramble`; have a non-OP player answer.
5. Verify their exact LuckPerms icon+rank prefix, colored name and proper message. Run `/pridequiz leaderboard` and check one victory.
6. Restart; confirm the scoreboard is preserved. Run trivia and confirm both counters.
7. Test two players answering simultaneously and that only one win is recorded; test a player whose rank changes in LuckPerms.

## Developer and CI
- LuckPerms API 5.4 is **compileOnly** via Maven Central, never bundled.
- Existing Impactor 5.3.5 compileOnly wiring is unchanged.
- GitHub Actions runs 24 core checks, 17 reward checks, 15 leaderboard checks, and Fabric JAR packaging.
- This remains a **development build** until verified in your full mod stack.

## Pending audits before release
- Concurrent player/wallet stress tests.
- Restart/crash/payout reconciliation under actual Impactor.
- Visual tests of all six real sprite and prefix combinations on actual player accounts.
- Test LuckPerms absence/outdated API fallback (no crash).
