# Pride Scenes: Rin Lab camera (Preview 10 staging)

The private client-side camera is rendered only for the local viewer. It does **not** teleport the actual player, affect other players, or change starter/quest progress. Native starter handoff remains server-owned.

1. Install just `pride-scenes-client-0.1.0-preview10.jar` instead of Preview 9; keep the current unified server JAR.
2. Start Minecraft once. Edit `config/pride_scenes_client/camera.properties` in your Minecraft instance (the file is automatically created/expanded).
3. Save changes and replay the cinematic with `/pridecore story admin test-onboarding Blions`. Camera values reload on every scene start. You do not need to restart Minecraft.
4. To turn off the follow shot, set `lab_follow_enabled=false`. To disable Lab camera altogether use `lab_enabled=false`. Existing `camera_enabled` and `reduced_motion` continue to work but are read at game startup.

## Camera coordinates and directions

The fixed final view defaults to the established **standing block** X=-54, Y=102, Z=3195. The render eye is (X+0.5, Y+1.62, Z+0.5) = (-53.5, 103.62, 3195.5). Minecraft yaw 90 means west. Pitch 0 is level. If the spot is blocked by room furniture, the camera probes limited safe nearby positions; if there is no clear view, it falls back to the normal player camera.

## Storyboard and configurable tuning

- At entrance, the local camera aims at Rin from `lab_follow_offset_x/y/z` blocks relative to his moving projection. The camera follows with `lab_follow_smoothing` until `lab_follow_ticks`, default 88 ticks (~4.4 s at 20 TPS).
- The camera eases to the approved west-facing Lab shot over `lab_settle_ticks`, default 26 ticks (~1.3 s).
- It remains on the final shot through dialogue, then restores the player's normal camera when the scene ends.
- Solid-block collision checks protect the camera against clipping while moving. Where there is no safe transition, it uses the safe static camera instead of flying through a wall.
- The dialogue and Enter-to-advance behavior are left untouched. The existing HUD/hand/chat suppression remains active while the camera is on.

Configuration is **client-side**, not pushed by the server yet; each viewer can customize their own. Camera geometry should be verified in the actual built lab with shaders enabled. No camera setting changes Rin's NPC stage anchors or server timeline.
