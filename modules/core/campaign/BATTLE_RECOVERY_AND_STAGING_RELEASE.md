# Module 02 — battle result recovery and staging release conditions

## What is now implemented

Native Cobblemon victory callbacks are accepted only for an active, exactly matched RCT-owned battle UUID, associated server-side player, and a single winning player actor against a different losing NPC actor. A battle failure, disconnect or invalid callback retires the uncredited ticket.

If a real victory reaches the authenticated story router but **fails to read/write the player's durable ProfileStore data**, the server keeps the in-memory verified ticket and schedules at most five automatic retries, one per 20 server ticks. Repeated callbacks cannot change the credited winner. A successful retry retires the battle ticket and stores one story win; no money, Pokémon or League badge rewards are sent. A repeated already-earned fact is a no-op.

If retries all fail, the queued result is discarded and an error is written to the server logs. **This is not crash-durable:** a server restart, crash or lost player connection can still make an unpersisted result unavailable. Do not claim guaranteed recovery. Operators should inspect the server log and stored campaign facts and use an actual legitimate rematch if necessary. There is deliberately no admin win/credit command.

If native RCT launches successfully but the subsequent story-ticket confirmation fails (including a profile I/O exception), the module now attempts to abort the native RCT battle and releases its temporary reservation. This avoids an orphaned trainer match outside the story ledger. A failure to abort is logged as an error requiring operator investigation.

An in-flight battle whose required narrative fact is revoked (for instance during a staging reset) cannot award a victory; the stale ticket is retired so the player can eventually begin an eligible match again.

## Automated coverage

The CI suite now tests:

- ProfileStore-backed victory evidence, uniqueness and rerun refusals
- Revoked prerequisite during an in-flight battle
- Persistent RCT win metadata and single-player-vs-NPC authorization
- Queue deduplication, a maximum of five retries, expiration and capacity
- **Injected profile I/O failure**, followed by retrying the original ticket and checking exactly one saved win
- All nine authored battle tickets plus the four-conduit, three-shield, core-completion story path
- Recovery of a real shield-win fact after a simulated finale checkpoint interruption
- Staging-only world hitbox props, marker exclusivity and safe cleanup

GitHub CI compiles Java 21 and the Fabric module. It **does not** start Minecraft, simulate actual RCT AI turns, validate multiplayer packet timing, or confirm that a placed interaction entity can be clicked on the full modded server.

## Remaining release blockers

Before production use, the team must verify an isolated Minecraft 1.21.1 Fabric/Cobblemon 1.8.1/RCT 0.19.0-beta staging environment with real physical trainer entities. The test plan must cover wins and losses, disconnects, world switching, duplicates, one-time rewards, mixed-mod compatibility, all eight original badge gates, finale checkpoint recovery, and at least two simultaneous players. The cinematic KO phases currently change presentation, **not opponent Pokémon mechanics**.

Do not install the optional staging datapack or enable `story_enabled` on the production server until these checks are completed.

## Multiplayer authorization and physically bound trainers

The RCT launch ledger now retains the **entire authenticated native start** (battle UUID, player UUID, authored encounter ID and trusted trainer identity), not just a player ownership bit. The only native start accepted by the story router must exactly match that ledger record. This prevents a server-side event for another encounter from borrowing the battle UUID even when it names the correct player.

Physical story launches now require a real tagged living trainer, in the player's same dimension, within six blocks, with only one valid story marker. That validation happens **inside the trusted launch method** as well as the NPC interaction callback. The launch ledger additionally serializes each trainer entity: two players cannot reserve or fight the same *physical* trainer at once. The lock is released on battle completion, cancellation, disconnect and session reset. Different physical trainers can serve players concurrently. A player gets a specific busy message rather than a generic progression denial.

CI now simulates two simultaneous players fighting separate physical trainers, verifies incorrect cross-player victory results cannot award progression, then verifies the trainer lock releases and a legitimate retry can be credited. It also checks independent conduit and finale completion states for two players. These are server-state tests, not a substitute for multiplayer testing inside Minecraft.

## Authored boss-specific presentation

Knockout-driven visual phases now identify the actual boss: Rin, Eclipse Lieutenant, Sable, Director Cael or Alpha/Beta/Gamma defender. Each includes distinctive two-step actionbar cues and configured particle aliases. This remains **presentation only**; no AI decisions, damage, boosts or Pokémon moves are rewritten by the story mod.

Duplicate conduit clicks are idempotent. The server no longer rewrites finale checkpoints or emits duplicate effect bursts for a conduit already charged. The staging-world commands `/pridecore story admin audit-zone hearthleaf`, `audit-zone eclipse`, and `audit-zone finale` validate localized expected marker groups within 96 blocks (loaded entities only). This helps builders but cannot prove the entire region has been fully populated.
