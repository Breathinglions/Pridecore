# Module 02 staged encounter presentation (staging)

Pride Core now has a server-tick cinematic cue sequencer for scripted encounters. It schedules short visual and actionbar beats after the **trusted RCT launcher returns an authenticated battle UUID**. It does not simulate a battle victory, change HP, damage enemies, control cameras, or grant any story facts.

## Authored scenes

- Rin Route 1 / Canyon / Summit: three escalating rival challenge beats
- Eclipse Lieutenant: two Eclipse warning beats
- Sable: three commander / shadow-themed beats
- Director Cael: three Meridian / Resonance overload beats
- Shield Alpha / Beta / Gamma: three defender / shield beats each
- Fourth conduit activated: three-burst conduits-online transition
- Third verified shield victory: three-burst shields-offline transition
- First verified core stabilization: three-burst completion transition

Cues run at 0, approximately 1–1.4 seconds, and 2.5–3.25 seconds (depending on scene). Each cue sends a short actionbar text and a bounded server-side particle effect. The opening cue also plays a subdued vanilla beacon audio sting localized to the player (no additional sound mod required). The implementation deliberately avoids camera locks and movement overrides to remain compatible with Cobblemon/RCT battles.

The timeline has a global cap of 64 concurrent scenes. Duplicate scenes for the same player do not stack. The scheduler clears on player disconnect, server shutdown and failed module startup. Phase transitions cancel an older presentation before starting the next phase; completed core interactions cannot replay the transition. Effects use the configurable `StoryEffects` particle registry and safe vanilla fallback.

## Important limitations

This is **cinematic encounter presentation**, not multi-phase Pokémon battle mechanics or a full client cutscene system. No modded camera, custom shader, unique model animation, or synchronization to Pokémon turn phases is claimed. Full combat phase scripting requires confirming RCT/Cobblemon's real battle APIs and testing multiplayer behavior in the staging server with the exact installed jars.

Other mod gameplay and official Gym/League match results remain untouched. This system stays inside the Module 2 development branch until validated.
