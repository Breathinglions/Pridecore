# Optional Resonance Engine staging-device template

This folder is an **uninstalled development asset** for Minecraft 1.21.1:

`modules/core/staging_world_datapack/`

It is deliberately outside the mod's normal resources and server `datapacks` directory. Nothing in production is modified by GitHub commits. Copy it only to a disposable staging world's `datapacks` folder when the Module 02 build is ready to test. The pack creates no terrain, buildings, trainers, custom blocks, money or story facts.

## Staging-only installation and placement

Back up the staging world first. Install the folder as a datapack under the correct **staging world**. With an OP in the desired Resonance Engine arena, stand at the intended **core center** and execute:

```mcfunction
/reload
/function pridecore:staging/setup_engine_props
/pridecore story admin audit-finale
```

This puts invisible, 1.5-by-2-block `minecraft:interaction` hitboxes at the core and 12 blocks north/south/east/west. Their one-per-prop tags are `pride_finale_core` and `pride_finale_conduit_<direction>`. Physical arena geometry, decoration, lighting, ambient effects and the **three real living RCT shield defender trainers** still need to be built and bound by operators.

The setup function refuses to add another set of generated devices while any previously generated staging hitbox is within 32 blocks. The world-readiness audit will still report the three missing shield defenders until they are bound to actual staging actors.

When the temporary invisible props need removing, stand back at the arena center and run:

```mcfunction
/function pridecore:staging/cleanup_engine_props
```

Cleanup destroys **only** nearby `minecraft:interaction` entities with the unique `pride_staging_prop` tag. It does not kill the actual trainers, buildings, player Pokémon, or untagged entities. Keep arena builds farther than 64 blocks apart to avoid confusing the local audit, and do not place this pack in production.

## Important constraints

The hitboxes alone do not make the finale playable. Module 02 still requires an authenticated Cael win and engine evidence, all four conduits, three genuine RCT defender victories, and an authenticated core interaction. No scripted battle can be granted through the datapack. These functions use the executing player's current coordinates; running them from the console without a deliberate execution position is unsupported. The included automated test checks the five tags, duplication guard and cleanup selector, but does not prove correct in-game hitbox interaction.

## Optional distributed evidence hitboxes

The same staging-only datapack now includes one placement function per authored investigation clue:

```mcfunction
/function pridecore:staging/place_clue_forest
/function pridecore:staging/place_clue_mine
/function pridecore:staging/place_clue_foundry
/function pridecore:staging/place_clue_river
/function pridecore:staging/place_clue_grid
/function pridecore:staging/place_clue_ward
/function pridecore:staging/place_clue_ice
/function pridecore:staging/place_clue_echo
/function pridecore:staging/place_clue_engine
```

Stand where the actual investigation object will be built and run the appropriate command as a staging operator. Each function places a single invisible `minecraft:interaction` hitbox with the correct `pride_clue_<id>` tag and the unique `pride_staging_clue` cleanup tag. If a matching clue is already within three blocks, another copy is not placed. The associated environmental structure, decorations, and clues' intended story locations are **not invented by the datapack**.

To remove one incorrectly positioned generated clue, stand within four blocks and run:

```mcfunction
/function pridecore:staging/remove_nearest_clue
```

This deletes only the nearest staging-generated clue hitbox. It cannot remove the five finale props or unrelated entities. None of these functions grant or complete evidence; a real eligible player must interact with the device and pass the normal campaign prerequisites.
