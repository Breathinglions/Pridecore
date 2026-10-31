# PrideCore continuous flythrough playback — Preview 14

This update changes the **camera renderer only** for scenes whose JSON
explicitly opts into `"playback": "flythrough"`.

Existing scene defaults and Sequoia/Rin onboarding remain `"shots"`,
including all original actor cues, dialogue and starter UI behavior.

## Convert your already-captured Thorn Gym tour

You do NOT have to recapture the current camera coordinates or angles.

```mcfunction
/pridecore cinema playback thorn_gym_tour flythrough
/pridecore cinema flight-time thorn_gym_tour 240
/pridecore cinema list thorn_gym_tour
/pridecore cinema preview thorn_gym_tour
```

- `flythrough` treats every saved static camera shot as a **waypoint** on
  one uninterrupted 3D path.
- The spline passes intermediate camera positions with rounded corners
  instead of holding at them. It interpolates the saved yaw/pitch too.
- Speed stays approximately constant along the actual curve; a gentle
  acceleration/deceleration happens only at the START and END of the tour.
- `flight-time` sets the *entire* tour duration in ticks, not per-shot
  holds. 240 ticks = 12s at 20TPS. Example: 400 = 20s, 160 = 8s.
- Each existing waypoint's `durationTicks` and `transitionTicks` are
  ignored in flythrough mode. Those values are retained for returning to
  normal `shots` playback.
- At least **two waypoints** are required, and all must be `static`
  camera shots. To convert a follow shot:
  `/pridecore cinema static thorn_gym_tour <shot_name>`.
- You can reorder points via
  `/pridecore cinema move-after thorn_gym_tour <shot> <previous_shot>`,
  or use `move-first`.
- To return to the old stop-and-hold style:
  `/pridecore cinema playback thorn_gym_tour shots`.
- The scene stores the new controls in the **same existing JSON**:
  `<world>/data/pride_core/cinematics/thorn_gym_tour.json`

## Important gym geometry and collision safety

A curved camera route may cut corners. **Capture intermediate waypoints
inside each doorway and around each corridor bend**; do not attempt to
travel straight through walls or closed doors. Collision-safe flight
aborts instead of teleporting the camera through solid blocks.

The client preview and configured proximity entrance use the same scene
playback; the once-per-player zone configuration does not need changing.
A flythrough is strictly a camera path and does not teleport characters,
change quests, grant badges or start battles.

## Deployment

Replace the previous *unified* server PrideCore JAR with the new one.
Install the supplied optional client Preview 14 JAR instead of Preview 13.
Do not install the client JAR on the dedicated server. Keep exactly one
PrideCore server JAR and one optional client visual JAR.

Back up the world and `thorn_gym_tour.json` before changing its playback
mode. The build was compiled and QA checked; actual gym geometry must be
tested in-game because the builder's layout isn't in the source repository.
