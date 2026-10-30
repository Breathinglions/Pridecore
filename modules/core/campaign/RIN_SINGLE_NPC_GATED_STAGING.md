# Rin — One NPC, Three Badge- and Story-Gated Chapters

**Status:** staging candidate; actual RCT trainer battle and native chapter handoff still require in-game verification.

## Player-facing fix

The physical `pride_story:rin` Cobblemon NPC is the **only Rin actor** for all chapters. Her/his native default GUI has two choices: Talk to Rin and Goodbye. The Talk option closes the entry GUI and requests the **server-owned** chapter dialogue for this specific player and physical Rin. The server opens the appropriate native Cobblemon `/opendialogue` from its own fixed resource allowlist after a two-tick delay. The UI does not expose unearned Canyon or Summit choices at all. Neither a client nor an NPC script selects a chapter by submitting an encounter ID.

## Chapter rules

| Chapter | Original Gym badges | Additional prerequisites | Successful outcome |
| --- | ---: | --- | --- |
| Route 1 | 0 | Saved `pride:met_rin` and `pride:met_avery` with no Route 1 victory | `pride:rin_route1_won` |
| Canyon | 2 | Verified Route 1 win + `pride:eclipse_contact`, without Canyon win | `pride:rin_canyon_won` |
| Summit | 7 | Verified Route 1 and Canyon wins + `pride:frozen_warning`, without Summit win | `pride:rin_summit_won` |
| Other phases | — | No currently available Rin encounter | Safe return dialogue; no challenge button |

Badge counts use **only** the eight original League command tags. Old veterans with 2 or 7 badges who reset their Pride Story remain on Route 1 until they complete actual chapters.

## Secure single-NPC challenge

Each unlocked native chapter contains a Challenge Rin button, with no chapter ID in the client command. `pridecore story rin challenge` verifies the caller is a real player, the selected chapter's private pending session is live, that exact tagged `cobblemon:npc` is still within 6 blocks in the same permitted dimension, and the player's **current** stored facts and authoritative original badge tags still satisfy the chapter. It uses the same RCT/Cobblemon battle-launch adapter and authenticated victory callbacks as the original story trainer system, but uses Rin's one `pride_npc_rin` physical actor marker instead of requiring a second battle NPC.

**No separate `pride_battle_rin_route1` marker should be added to the dialogue NPC.** Existing source-only trainer marker checks remain authoritative for other nine boss encounters.

## Smoke test on your alternate staging map

1. Back up staging and replace the prior server JAR with the new verified UNIFIED-RIN-GATED JAR (one Pride Core mod ID only). Do not install duplicate League/Story ZIPs.
2. Reuse your existing **single** Rin NPC spawned by `/function pride_story:spawn/rin`. Keep its native type and `pride_npc_rin` marker.
3. Your current tester has two original Gym badges but was reset into the Route 1 story: verify Rin shows **only** Route 1 lines and Challenge, no Canyon/Summit anywhere.
4. Challenge Rin; the RCT battle must start. Lose deliberately once; no win fact should be granted. Win on second try; check the server-owned `pride:rin_route1_won` checkpoint and next Route 1/Ranger objective.
5. Relog and open Rin again; Route 1 should be a return conversation without replaying the reward/win.
6. On a second staging profile, complete `pride:eclipse_contact` and obtain two real badges with the Route 1 win; Canyon opens. With one badge the Canyon chapter must stay hidden. Summit still hidden.
7. At `pride:frozen_warning`, with seven original badges and verified Canyon win, Summit opens. With six badges or no Canyon win, it must stay hidden.
8. Two players at different stages near the same physical Rin must see their own independent native GUI/chapter. No cross-player dialogue or fights.
9. Validate no native dialog context/portrait errors, no missing Cobblemon `/opendialogue` command, and no RCT reflective failures in server log. These are gameplay acceptance checks, not just CI.

## Important limitations

- If the installed Cobblemon `/opendialogue` command requires an NPC context to show `q.npc.face`, chapter resources currently specify an artificial Pride Rin model portrait to avoid a null NPC expression. In-game validation is required.
- The preview client Rin model/animation and Sequoia's real cinematic handoff are not part of this patch.
- External Kit JSON, RCT League definitions and FTB Quest controls remain unchanged.
- An earlier Maven/Gradle/CI pass is **not** proof that an RCT fight can start. A real test is required before production promotion.

## Troubleshooting

- If the entry overlay closes but no chapter overlay opens, check server console logs for `opendialogue` and resource IDs `pride_story:rin_route1`, `pride_story:rin_canyon`, `pride_story:rin_summit`.
- If Rin refuses a challenge, inspect `/pridecore story admin encounters <YourUsername>` and `/pridecore story status`; check the real story facts, original badges and the same physical Rin within six blocks.
- Do not spawn another Rin or attach conflicting story role tags as a workaround.