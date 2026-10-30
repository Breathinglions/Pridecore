# Pride Scenes Client — staging projection prototype

Minecraft **1.21.1**, Fabric **0.16.10+**, Java **21**. This separate, **client-only** JAR receives `pridecore:scene_visual_v1` from the server's Module 02 and renders a temporary private **Minecraft player-shaped Rin actor using the supplied 64×64 Rin PNG skin**. It is **not** the final branded Cobblemon NPC model; it uses the bundled custom player skin, cannot be interacted with, and never enters the authoritative server world.

## Safety and scope

- No client-to-server packets, starter grants, story progression, trainer wins or hybrid boss mechanics.
- The server sends visual packets only when an explicit anchors config is enabled, the player is in the matching dimension/area, and the client reports this protocol as supported.
- An absent client mod falls back to the existing text-only staging scene.
- Client virtual NPC clears on scene end, logout, world change, unexpected entity loss or a 220-tick silence timeout.
- Different players run distinct scenes without shared actors. Only the local client sees its own projection.
- The skin is bundled at `assets/pride_scenes_client/textures/entity/rin.png`; it does not require fetching a player profile or changing a server datapack.\n- The source and optional JAR are **development only** until native Sequoia GUI compatibility, models, positioning and two-player staging gameplay are verified.

Build with `gradle --no-daemon clean build` from `modules/client`. The output is `build/libs/pride-scenes-client-0.1.0-preview2.jar` and must only be tested in a disposable staging modpack.
