# Pride Scenes Client — staging projection prototype

Minecraft **1.21.1**, Fabric **0.16.10+**, Java **21**. This separate, **client-only** JAR receives `pridecore:scene_visual_v1` from the server's Module 02 and renders a temporary private **Minecraft player-shaped Rin actor using the supplied 64×64 Rin PNG skin**. It is **not** the final branded Cobblemon NPC model; it uses the bundled custom player skin, cannot be interacted with, and never enters the authoritative server world.

The new automatic Sequoia onboarding uses an owner-only read-only dialogue subtitle overlay and original Cobblemon starter selector. No cinematic button can grant a starter. This is staging-only and camera choreography is not yet implemented.\n\nPreview5 dialogue redesign: Cobblemon-style dark gray pixel frame, white dialogue card, left-side speaker portrait, real Rin skin head and dark text; screen-safe top positioning. This remains a read-only renderer and uses a temporary placeholder initial for Sequoia until his portrait is available.\n\nPreview4 HUD polish: cinematic speaker dialogue now appears at the upper-left rather than behind vanilla chat, including at large Minecraft GUI scales. No server changes, starter resets, or actor changes required.\n\n### Preview6 optional cinematic camera

Client-only render-camera framing adds entry, walking, Rin dialogue,
Sequoia dialogue and exit shots. The original Minecraft player is never
teleported or rotated. A vanilla Camera.update runs before every frame's
temporary view change, so the camera automatically returns when the scene
ends. Client disconnect, world switch, player movement away from the lab,
unexpected actor loss, scene timeout and missing scene packets reset it.
Walls are checked with a collision raycast and viewpoints stay within
nine blocks of the player.

Camera configuration (created on first client launch):
`config/pride_scenes_client/camera.properties`

```properties
camera_enabled=true
reduced_motion=false
```

Set `camera_enabled=false` or `reduced_motion=true` and restart the
client for the original Preview5-like fixed player camera. The dialogue,
custom Rin model and animation still work with the camera disabled.

**Staging warning:** Camera mixins may interact with other shader/camera
mods; verify in-game with your actual Fabric modpack before production.
Camera framing may vary by Sequoia position and furnishings; this version
does not pause game input or move server entities. No starter/battle/League
logic has changed.

## Safety and scope

- No client-to-server packets, starter grants, story progression, trainer wins or hybrid boss mechanics.
- The server sends visual packets only when an explicit anchors config is enabled, the player is in the matching dimension/area, and the client reports this protocol as supported.
- An absent client mod falls back to the existing text-only staging scene.
- Client virtual NPC clears on scene end, logout, world change, unexpected entity loss or a 220-tick silence timeout.
- Different players run distinct scenes without shared actors. Only the local client sees its own projection.
- The skin is bundled at `assets/pride_scenes_client/textures/entity/rin.png`; it does not require fetching a player profile or changing a server datapack.\n- The source and optional JAR are **development only** until native Sequoia GUI compatibility, models, positioning and two-player staging gameplay are verified.

Build with `gradle --no-daemon clean build` from `modules/client`. The output is `build/libs/pride-scenes-client-0.1.0-preview6.jar` and must only be tested in a disposable staging modpack.
