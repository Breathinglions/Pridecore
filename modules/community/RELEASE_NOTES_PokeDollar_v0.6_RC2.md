# Pride Community v0.6.0-rc2 — PokéDollar symbol + configurable rewards

This is a **code and Polymer resource-pack pair**. Both are needed when using the default custom currency icon.

- Server JAR: `pride-community-0.6.0-rc2.jar` (Fabric 1.21.1, Cobblemon 1.8.1)
- Player pack: `Pride_Region_Community_Identity_v0.6_RC2_PokeDollar_1.21.1.zip`
- Pack SHA-1: `6d1178df990cc8c8e14fbf95f806f80d87eed175`
- Resource-pack addition: U+E122 (a compact golden PokéDollar bitmap glyph). All original Pride Region OWNER/other ranks, genuine Cobblemon Poké Ball icon mappings and ALPHA pill-only translations are preserved.

## Config — on PebbleHost
`config/pride_community/config.json`:

```json
"rewards": {
  "economyBackend": "IMPACTOR",
  "payoutsEnabled": false,
  "dailyCapPokeDollars": 2000,
  "currencySymbol": "\\uE122",
  "tiers": {
    "EASY": 100,
    "MEDIUM": 200,
    "HARD": 350,
    "ULTRA": 500,
    "SCRAMBLE": 150
  }
}
```

- Change the FIVE `tiers` numbers to configure **actual amounts deposited** into the existing Impactor primary wallet (not just cosmetic numbers).
- `dailyCapPokeDollars` controls the maximum per-player reserved/paid amount per UTC day.
- `payoutsEnabled: false` means **no deposit**, even though the prize announcement states the hypothetical award with **TEST: no payout**.
- Only switch `payoutsEnabled` to true on a backed-up staging server for controlled live-wallet QA. Cold restart required; `/pridequiz reload` cannot safely arm deposits.
- `currencySymbol` changes ONLY how the amount is shown. The default uses private-use character U+E122, requiring the matching resource pack. You may set a vanilla-compatible `"₽"` or `"₱"` if needed.
- If your existing config.json lacks `currencySymbol`, the JAR automatically falls back to the new PokéDollar glyph.

## What changed
- Both **question prize prompts** and **successful deposit notifications** now use the PokéDollar symbol.
- **Staff /pridequiz audit** daily cap and unresolved-transaction amounts show the same symbol.
- All three use one shared money formatter with thousands separators.
- No change to wallet API, payout atomicity, claim ledger, leaderboard, CRZQuiz, Spawn Notification or Discord.
- Existing configuration settings and default-disabled payout behavior preserved.

## Staging test
1. Stop the server, back up your v0.6 RC1 JAR and current pack.
2. Replace only the old Pride Community JAR; install one `pride-community-0.6.0-rc2.jar`.
3. Replace the old Pride Identity/Community pack in Polymer with v0.6 RC2, update its URL/filename and SHA-1, reconnect.
4. Run `/pridequiz force scramble` or `/pridequiz force trivia` and `/pridequiz audit`. Confirm custom PokéDollar symbol renders in prize amount and audit without missing glyph squares.
5. Keep test mode and check that Impactor wallet does **not** increase.
6. On backed-up staging, use a 1-unit test tier/cap with `payoutsEnabled=true`, cold restart, have a non-OP player answer; verify exact deposit and `rewards.tsv`. Turn payouts back off afterward.

**Production wallet QA remains required**. Passing GitHub Actions only establishes compilation and unit checks, not a real Impactor wallet transaction.
