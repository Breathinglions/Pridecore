# Module 02 story battle NPC markers (staging)

A builder may tag a deliberately placed **living** NPC/trainer with exactly one
`pride_battle_<encounter-id>` scoreboard/entity tag. The server's player
interaction callback rejects more than one story battle tag, unknown encounters,
and interactions beyond six blocks. No unrelated trainers are intercepted.

Allowed marker examples:
- `pride_battle_rin_route1`
- `pride_battle_rin_canyon`
- `pride_battle_rin_summit`
- `pride_battle_eclipse_lieutenant`
- `pride_battle_sable`
- `pride_battle_director_cael`
- `pride_battle_shield_alpha`
- `pride_battle_shield_beta`
- `pride_battle_shield_gamma`

Server-owned story fact prerequisites, original Gym badge requirements from the authored roster, and existing victory records determine whether the battle may start. Shield encounters also require the live finale
SHIELDS phase. The NPC's entity UUID is bound to the launch reservation.

**Not yet release-ready:** The reflective RCT launcher and Cobblemon event
handler must pass a full server runtime integration test. In particular,
RCT initialization and default BattleRules / BattleFormat contracts have been inspected in the supplied binaries, but require a real server launch and battle verification. These marker instructions describe
the current staging adapter and do not imply battles are already working.
Official Gym and League opponents remain outside this system.


## Authoritative badge progression

The live launch reservation checks `minBadges` from the exact bundled `story_battles.json` roster against only the original eight official `league_grass` through `league_ghost` command tags. The Champion tag, onboarding tags, and arbitrary player tags are not counted. Requirements are currently 0 for Route 1 Rin; 2 for Canyon Rin; 7 for Summit Rin; and 8 for the Eclipse Lieutenant, Sable, Director Cael, and all shield defenders. The player-facing encounter checklist shows the missing-badge count when applicable. Pride Core does **not** award or forge official badges.

The campaign and runtime roster copies are checked for exact JSON parity in CI, preventing development-only badge thresholds from silently drifting from the JAR. This is a source/CI safeguard, not a claim that all real League tag events are validated in a running server.
