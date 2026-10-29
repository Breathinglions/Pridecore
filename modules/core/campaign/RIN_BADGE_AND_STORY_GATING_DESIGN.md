# Rin — dual badge + narrative gating for one physical NPC

**Status:** next staging patch design, not yet implemented by this document. Current native Rin dialog still exposes premature Canyon/Summit buttons; battle eligibility checks are implemented separately.

## Canonical requirements

| Encounter | Required original Gym badges | Required story fact | Verified battle result |
| --- | ---: | --- | --- |
| Route 1 Rin | 0 | `pride:met_rin` and initial onboarding continuity | `pride:rin_route1_won` |
| Canyon Rin | 2 | `pride:eclipse_contact` and earlier authentic story progression | `pride:rin_canyon_won` |
| Summit Rin | 7 | `pride:frozen_warning` and earlier authentic story progression | `pride:rin_summit_won` |

Use only `StoryLeagueBadgeGate` counting `league_grass`, `league_rock`, `league_fire`, `league_water`, `league_electric`, `league_dragon`, `league_ice`, `league_ghost`. Never treat a badge count alone as permission to skip missing story chapters. Preserve locked IDs, immutable badge minima and Pokémon-only RCT battle authority from the validated storyline roster config.

## Required player experience

- **ONE** physical `pride_npc_rin` NPC with Cobblemon native dialogue overlay, not several simultaneously spawned Rins.
- Per-player, server-authoritative chapter selection at interaction time. Route 1 players see only the Route 1 introduction and a contextual challenge option; Canyon/Summit choices are completely hidden.
- At the actual Canyon stage and two badges, Rin uses the Canyon text and challenge; at the actual Summit stage and seven badges, Rin uses Summit text and challenge.
- If a returning trainer has seven badges but has restarted the story at Route 1, they still see only Route 1. Story prerequisites are mandatory.
- A completed chapter supplies appropriate repeat dialogue, never duplicate wins or a previous chapter reset.
- Dialogues should use natural story language rather than raw internal IDs and spoiler-heavy future options.

## Technical architecture and safety

- Cobblemon native JSON offers `isVisible`/`isSelectable` MoLang expressions. Verify an **actually supported** player-phase query in 1.8.1 before choosing that implementation. Do not invent `q.player.has_tag` or assume player-writable `q.player.data` can enforce progression.
- The server controls eligibility and battle launch regardless of what is hidden in the UI; clicking a stale choice or manually submitting a native action cannot bypass badge requirements, narrative prerequisites, range, dimension or original RCT win authentication.
- The existing native Rin handler does not currently launch Pokémon battles from dialogue. Implement a native `Challenge Rin` handoff into the real authenticated story RCT launcher using **that same physical Rin NPC**. Do not add a duplicate battle actor, a generic villager or simulated wins.
- Genuine victory unlocks the next checkpoint only through the existing battle callback. Loss, cancellation and disconnect cannot award a win. Storyline combat remains Pokémon-only.

## Acceptance tests

1. New player: 0 badges + Route 1 prerequisites -> Route 1 dialogue/challenge only.
2. Veteran with two or seven badges but reset to Route 1 -> still only Route 1 dialogue, no future spoilers.
3. Eclipse story fact + one badge -> Canyon stays locked.
4. Eclipse story fact + two actual Gym badges + authentic continuity -> Canyon dialogue and encounter; Summit hidden.
5. Seven badges without `pride:frozen_warning` -> Summit remains locked.
6. Seven badges plus frozen warning and continuity -> Summit dialogue and encounter.
7. Manually attempting an out-of-order native command is rejected on server; zero fake wins.
8. Two users on distinct chapters viewing same Rin see only their own correct content.
9. Restart, relog, battle loss and story reset retain correct visibility and eligibility.
10. All existing League and original onboarding native dialogs and rewards continue working.

**Stage-gating design only: no replacement server JAR is produced by this document.**