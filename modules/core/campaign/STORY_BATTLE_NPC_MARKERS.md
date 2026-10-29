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

Server-owned story fact prerequisites and existing victory records determine
whether the battle may start. Shield encounters also require the live finale
SHIELDS phase. The NPC's entity UUID is bound to the launch reservation.

**Not yet release-ready:** The reflective RCT launcher and Cobblemon event
handler must pass a full server runtime integration test. In particular,
RCT initialization and default BattleRules / BattleFormat contracts need
verification against the supplied binaries. These marker instructions describe
the current staging adapter and do not imply battles are already working.
Official Gym and League opponents remain outside this system.
