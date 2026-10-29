# Module 02 operator world-binding tools (staging only)

These are **world authoring tools**, not player progression cheats. They require operator level 4 and target loaded entities on the command source's current dimension. In-game operators must be within 32 blocks. They do not place structures, summon Pokémon, initiate battles or grant any story facts.

## Bind existing physical actors

Use the player-facing command prefix `/pridecore story`:

```mcfunction
/pridecore story admin bind battle rin_route1 @e[sort=nearest,limit=1,type=minecraft:villager,distance=..5]
/pridecore story admin bind clue forest @e[sort=nearest,limit=1,type=minecraft:interaction,distance=..5]
/pridecore story admin bind finale north @e[sort=nearest,limit=1,type=minecraft:interaction,distance=..5]
/pridecore story admin bind finale core @e[sort=nearest,limit=1,type=minecraft:interaction,distance=..5]
```

**Trainer example only:** choose an actual living RCT-compatible staging trainer entity instead of a villager when integrating RCT. The sample villager selector demonstrates syntax, not a validated RCT trainer.

Allowed battle IDs are the nine exact IDs defined by StoryBattleSessionRouter (`rin_route1`, `rin_canyon`, `rin_summit`, `eclipse_lieutenant`, `sable`, `director_cael`, `shield_alpha`, `shield_beta`, `shield_gamma`). Allowed clue IDs: `forest`, `mine`, `foundry`, `river`, `grid`, `ward`, `ice`, `echo`, `engine`. Allowed finale IDs: `north`, `south`, `east`, `west`, `core`.

A battle binding requires a living non-player entity. Clue/finale bindings require a separate `minecraft:interaction` entity with a usable hitbox. The bind command refuses unknown IDs and any entity with a conflicting story marker. Other mod tags are preserved. Binding the exact same marker twice is safe.

## Check and correct world setup

```mcfunction
/pridecore story admin audit-markers
/pridecore story admin unbind @e[sort=nearest,limit=1,type=minecraft:interaction,distance=..5]
```

The audit checks loaded entities within a 32-block area of the operator, identifies invalid story marker combinations or wrong entity types, and prints a count. It is **local only** and does not imply that an entire multiworld has been audited. The unbind command strips only Module 02 story battle/clue/finale tags from the selected entity, leaving unrelated tags intact. Always select precisely one entity.

These tools create authenticated *world marker identities*, not proof that the underlying RCT battle launcher works. The next milestone is in-game testing with the actual installed Cobblemon 1.8.1/RCT versions, once the module is ready for testing.
