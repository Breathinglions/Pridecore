# Pride Community v0.5 — combined reward-aware chat build

## Update scope
- JAR: prize amount plus `First correct answer wins` and 30-second countdown announced in every round.
- Payout status: [TEST: no payout] is explicit when rewards are off. +$ is sent only after Impactor confirms successful deposit.
- Imactor 5.3.5 integration, duplicate-safe durable ledger, daily cap and persistent leaderboard stay in place.
- Changing reward configuration via reload does NOT activate deposits. Restart required.
- Pill-style badges for Quiz, Scramble, Shiny, Legendary, Mythical, Ultra Beast, Paradox, Alpha, Max IV, Rare, Ultra Rare, Raid, World Boss, Event, Pride.
- Alpha and Max IV now use new pixel sprites inside the existing Spawn Notification English translations.
- `RARE` and `ULTRA RARE` are assets/manual announcement channels only. Spawn Notification itself does NOT define a universal ultra-rare tier: no automatic detection is claimed.
- The v0.4 rank glyphs and real Cobblemon ball glyphs are preserved, with no new client JAR.

## Deploy to backed-up staging before live
1. Keep one `pride-community-*.jar`: remove v0.4, install `pride-community-0.5.0-dev.jar` from GitHub Actions.
2. Replace the Polymer forced pack with `resourcepack/Pride_Region_Community_Identity_v0.5_1.21.1.zip`; update the resource-pack SHA-1 and filename.
3. Reconnect clients; use `/pridequiz status` to verify Impactor payouts=false.
4. Test `/pridequiz force trivia`, `/pridequiz force scramble`, `/pridequiz leaderboard`, `/prideannounce alpha Test`, `/prideannounce ultra-rare Test`.
5. Verify two non-OP players, only one winner, no payout when disabled, exact prefix glyphs, and scoreboard retained across restart.
6. Trigger real special Pokémon spawns to verify Spawn Notification text and Alpha/Max IV badges. Its server mod stays installed.
7. For real-wallet testing ONLY on staging, back up Impactor data, choose small reward amounts, verify wallet before/after, rewards.tsv, and error/restart behavior. Set payoutsEnabled=false afterward.

**Do not enable real payouts on live PebbleHost until in-game economy and failure-path QA passes.** Automated CI compilation is not proof of payout correctness.

### Resource pack SHA1
`035dba9becaad4e8bd1a225ca15b2dba8b483e8c`
