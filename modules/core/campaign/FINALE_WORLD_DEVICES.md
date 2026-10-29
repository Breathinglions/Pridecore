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
