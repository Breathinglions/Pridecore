# Resonance finale physical devices (staging builder specification)

The engine uses deliberately placed `minecraft:interaction` entities. Tag each device with **exactly one** marker. The story module rejects devices more than six blocks from the player.

| Device | Entity command tag |
| --- | --- |
| North conduit | `pride_finale_conduit_north` |
| South conduit | `pride_finale_conduit_south` |
| East conduit | `pride_finale_conduit_east` |
| West conduit | `pride_finale_conduit_west` |
| Engine core | `pride_finale_core` |

The operator can inspect a player's status with `/pridecore story admin inspect <player>`. An eligible player can begin naturally by interacting with any conduit after the real Cael victory and engine clue are recorded. On staging, `/pridecore story admin finale-start <player>` remains an optional operator rehearsal command with the same prerequisites. Conduits then advance the player's phase. The core only completes after the three shield-defender victories are verified by the still-unfinished battle-event adapter. Neither the location of the device props nor the boss arena is assumed by this module.

**Implementation state:** Conduits and core are hooked to the server's interaction callback. Shield defender battle launch, shield-victory processing, remain release blockers. Do not present this as a complete boss fight. The in-progress controller remains memory-backed pending durable checkpoint storage. No FTB Quests edits are involved.

Shield battle roster definitions are now authored in `story_battles.json`: Alpha (Steel-themed, levels 61–62), Beta (Electric-themed, levels 62–63), and Gamma (Psychic-themed, levels 63–64). They are **content specifications** only. The native trainer spawn and battle launcher must still be implemented and verified before a normal player can initiate these encounters.

Crash-recovery safeguard: after restoring a checkpoint in the SHIELDS phase, the server reconciles each previously persisted authenticated shield-win fact before allowing further progress. This covers a crash occurring after the player wins but before the separate finale checkpoint save; it does not invent victories or auto-launch a battle.

Live recovery: clicking any tagged finale device now reconciles previously verified shield wins even when the player has not disconnected. This prevents an interrupted checkpoint save from requiring relog. The physical trainer launches and Cobblemon 1.8.1 callback signatures still require verified runtime integration.

## In-world visual effects

Activating a physical conduit now emits server-side electric sparks and end-rod particles from its tagged interaction entity. Successfully stabilizing the core emits a larger end-rod/totem burst at the core marker. This is an initial native visual-effects pass (no additional client particle mod required), **not** the requested fully cinematic multi-phase boss sequence. Particle counts are bounded and only emit after eligible interaction transitions.

After an authenticated shield-defender victory, the player now receives a bounded electric/end-rod visual burst, the verified shields-completed count, and a clear prompt to defeat remaining defenders or stabilize the core once all three are down. This effect is emitted only following trusted battle-victory progression; it is not triggered by arbitrary world interaction.

## Player-facing operation progress

Players can run `/pridecore story finale` to read their saved current phase, conduit count, shield-defender count, and the next objective. The command does not unlock story chapters, spawn battles, award facts, or edit player inventory. Progress is reconciled against already verified shield wins before display. The finale must be enabled in the story configuration.

Core activation now explicitly refuses players who have not reached STABILIZE or COMPLETE; touching the core before defeating all three shield defenders cannot complete the encounter. Already-complete players remain idempotent.

The finale completion particle burst is emitted only on the first successful STABILIZE → COMPLETE core interaction. Reusing the core after completion does not replay the cinematic; the saved completion remains intact.
