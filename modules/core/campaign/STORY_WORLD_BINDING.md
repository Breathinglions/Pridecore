# Module 02 operator world-binding tools (staging only)

These are **world authoring tools**, not player progression cheats. They require operator level 4 and target loaded entities on the command source's current dimension. In-game operators must be within 32 blocks. They do not place structures, summon Pokémon, initiate battles or grant any story facts.

## Bind existing physical actors

Use the player-facing command prefix `/pridecore story`:

```mcfunction
/pridecore story admin bind npc rin @e[sort=nearest,limit=1,type=cobblemon:npc,distance=..5]
/pridecore story admin bind battle rin_route1 @e[sort=nearest,limit=1,type=minecraft:villager,distance=..5]
/pridecore story admin bind clue forest @e[sort=nearest,limit=1,type=minecraft:interaction,distance=..5]
/pridecore story admin bind finale north @e[sort=nearest,limit=1,type=minecraft:interaction,distance=..5]
/pridecore story admin bind finale core @e[sort=nearest,limit=1,type=minecraft:interaction,distance=..5]
```

**Two different story actor roles:** `npc` requires a real `cobblemon:npc` entity. It binds one of the 27 authored dialogue identities from `story/default.json`. In contrast, `battle` requires a living non-player entity representing the scripted opponent. Choose an actual RCT-compatible staging trainer entity instead of a villager for live integration; the villager selector above demonstrates syntax only, not a validated RCT trainer.

**Do not combine dialogue and battle markers on the same physical entity.** Module 2 currently routes a marked battle actor to the battle launcher before dialogue, so separate physical actors are needed for dialogue and battle. The catalog rejects competing story marker tags to make this limitation explicit. `rin_canyon` and `rin_summit` intentionally map to the same existing Cobblemon dialogue marker `pride_npc_rin`; the story prerequisites decide which conversation is eligible. The existing `ranger_epilogue` marker likewise reuses `pride_npc_ranger_rowan`.

Allowed battle IDs are the nine exact IDs defined by StoryBattleSessionRouter (`rin_route1`, `rin_canyon`, `rin_summit`, `eclipse_lieutenant`, `sable`, `director_cael`, `shield_alpha`, `shield_beta`, `shield_gamma`). Allowed clue IDs: `forest`, `mine`, `foundry`, `river`, `grid`, `ward`, `ice`, `echo`, `engine`. Allowed finale IDs: `north`, `south`, `east`, `west`, `core`.

An NPC binding requires a Cobblemon NPC of type `cobblemon:npc`. A battle binding requires a living non-player entity. Clue/finale bindings require a separate `minecraft:interaction` entity with a usable hitbox. The bind command refuses unknown IDs and any entity with a conflicting story marker. Other mod tags are preserved. Binding the exact same marker twice is safe.

## Check and correct world setup

```mcfunction
/pridecore story admin audit-markers
/pridecore story admin audit-finale
/pridecore story admin unbind @e[sort=nearest,limit=1,type=minecraft:interaction,distance=..5]
```

The audit checks loaded entities within a 32-block area of the operator, identifies invalid story marker combinations or wrong entity types, and prints a count. It is **local only** and does not imply that an entire multiworld has been audited. The unbind command strips only Module 02 story NPC/battle/clue/finale tags from the selected entity, leaving unrelated tags intact. Always select precisely one entity.

These tools create authenticated *world marker identities*, not proof that the underlying RCT battle launcher works. The next milestone is in-game testing with the actual installed Cobblemon 1.8.1/RCT versions, once the module is ready for testing.


## Finale arena placement audit

Stand near the constructed Resonance Engine and run `/pridecore story admin audit-finale`. The command checks a **64-block local area** of loaded entities for all eight required unique physical markers: four cardinal conduits, the core, and the three Alpha/Beta/Gamma shield battle actors. It reports missing markers, duplicated devices, and markers attached to the wrong entity type. It never changes the world or player records.

A "MARKER SET COMPLETE" message confirms only local prop setup—not RCT compatibility, NPC AI, battle launches, correct quest prerequisites, cinematic timing, or readiness for production. If the arena extends more than 64 blocks from the operator, reposition and audit from its center. The audit does not scan unloaded chunks or other dimensions.

## Restrict story interactions to a test world

The existing `config/pride_core/story.properties` can optionally isolate Module 2 to one or more named Minecraft dimensions. This is particularly useful while developing a separate Multiworld test region.

```properties
story_enabled=false
# Replace the illustrative registry ID below with your actual staging dimension ID.
story_allowed_dimensions=example:staging_world
```

A blank `story_allowed_dimensions=` preserves compatibility by allowing story interactions in every dimension. A comma-separated list permits up to 16 explicitly named registry IDs. Invalid or duplicate IDs fail the story module's startup configuration check. The setting is read at server startup and does not modify any underlying Multiworld definitions, chunks, world borders or claims.

When restricted, physical story NPC interactions, evidence clues, finale devices, RCT story launches, and player dialogue choices cannot advance the campaign outside those dimensions; pending cinematic effects are cancelled when a player leaves the allowed area. Operator world-binding tools still work as preparation utilities. Official Gym, League, NPC and other-mod behavior is unaffected. The story module remains disabled by default; this is documentation for staging, not authorization to turn it on in production.
