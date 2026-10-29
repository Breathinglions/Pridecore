# Private Rin visual prototype — isolated staging checklist

**Do not deploy to production.** These are operator rehearsals and a client-local **villager placeholder**, not the final Rin animated Cobblemon model. They cannot open native starter UI, select Pokémon or award story facts.

## Architecture

Module 02 remains **server-only**. It registers the presentation-only `pridecore:scene_visual_v1` custom payload, but sends nothing unless `visual_preview_enabled=true` in `config/pride_core/rin_scene_anchors.properties` AND a complete world-specific layout exists AND a selected client explicitly supports the channel. The module calls `ServerPlayNetworking.canSend`; unsupported players retain text-only preview.

The optional `modules/client` Fabric JAR is independent and strictly client-only. Its prototype spawns a transient villager in its own ClientWorld, labels it Rin, interpolates movement and removes it from the local client world. Neither server nor other players ever receive this entity. It does not issue C2S packets or interact with RCT.

## Operator staging procedure

1. Back up production. Use a separate staging server with the exact Fabric 1.21.1 and Cobblemon 1.8.1 modpack. Keep `story_enabled=false` on production.
2. Install the Module 02 staging JAR on the staging server; install the optional `pride-scenes-client-0.1.0-preview1.jar` only in the test client's modpack.
3. Set `story_allowed_dimensions` in staging to the exact intended test dimension. Copy the sample anchor config, replace each scene's dimension and three positions with actual builder-verified coordinates, enable visuals, and restart staging. Do not use guess coordinates.
4. Stand within 96 blocks of the scene's entry anchor; an OP4 operator uses `/pridecore story admin preview-rin <player> lab` (or canyon/summit).
5. Confirm only the named player sees the local villager placeholder, movement and dialogue. The second player standing beside the first must see **no** temporary actor. A player without Pride Scenes Client must still receive text preview and remain connected.
6. Run `/pridecore story admin cancel-rin-preview <player>` mid-scene. Test logout, dimension switch, timeout, two players at once, scene replay and server stop. There must be no leftover local actors or server-spawned villagers.
7. Confirm native Professor Sequoia, original Cobblemon starter screen, existing Gym/RCT trainer fights, FTB, economy and Legacy LegendaryQuests are unaffected. Do not mark cinematic production ready or merge based solely on successful CI.

## Explicit remaining work

A final branded Rin model and animation/controller (not a villager), per-scene authored geometry and camera path, integrated Sequoia native dialogue/starter selector after authenticated one-use handoff, actual two-player staging run and shader/mod compatibility tests. Prototype visual packets must never be treated as storyline battle proof or a progression trigger.
