# Module 02 configurable particle effects

Module 02 reads optional particle overrides from the existing server file `story.properties` in the story config directory. These are presentation-only settings; they do not change RCT ownership, encounter wins, or any quest progression.

Add one or more of the following keys:

```properties
# Leave blank to use the tested vanilla effect.
effects.battle.enchant=
effects.battle.electric=
effects.battle.rin=
effects.battle.sable=
effects.battle.cael=
effects.finale.end_rod=
effects.finale.totem=
effects.finale.conduit=
effects.finale.shield=
```

Use a particle registry ID such as `minecraft:flame`, or a compatible installed Fabric mod's *registered server-side particle ID* such as `examplemod:example_particle` (the latter is an illustrative placeholder, not a real ID). The mod must register a network-serializable particle type on the server, and clients need the required assets/mod to render it. This adapter currently supports simple particle types; particle types that require custom parameters are not suitable for direct overrides. Missing IDs fall back to the original vanilla effect and log a warning. Changes load at server/module startup. Do not install unknown mods directly on production.

Particle distribution, counts, and positions still come from the authored encounter logic. The effect registry is intentionally limited to known presentation keys. The next-stage Pride Client effects system can replace this adapter with richer timed animations without changing story progression.

Rin, Sable, and Director Cael use their own entrance effect keys (falling back to the vanilla enchant particle). The generic electric effect remains an additional burst for selected bosses. Finale conduit and shield keys are reserved for separate future cues and currently have no distinct emit call; configuring them today does not alter the existing effect.
