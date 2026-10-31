# PrideCore Cinematic Director v1 — staging authoring

Preview 11 client + unified PrideCore server. The server stores editable, replayable
camera scenes in `<world>/data/pride_core/cinematics/`. Each scene is one JSON file.
Files are created automatically; the default `sequoia_lab.json` contains the
Rin entrance-follow shot and the approved lab dialogue camera at
(-53.5, 103.62, 3195.5), yaw=90 (west).

All cinema authoring commands require **OP level 4**. Stand wherever you want
the camera, look in the direction of the shot, and run:

```mcfunction
/pridecore cinema capture sequoia_lab entrance
/pridecore cinema capture sequoia_lab dialogue
/pridecore cinema timing sequoia_lab entrance 88 20
/pridecore cinema follow sequoia_lab entrance rin
/pridecore cinema static sequoia_lab dialogue
/pridecore cinema list sequoia_lab
/pridecore cinema preview sequoia_lab
```

- `capture` APPENDS shots in order or REPLACES the existing named shot in place.
- `timing` is `<duration_ticks> <transition_ticks>`. At 20 TPS, 20 ticks
  normally equal one second. The transition interpolates FROM the previous
  authored shot. Long scene maximum 2400 ticks / 120 seconds.
- `follow` supports `rin` (Rin's private actor), `tag:<entity_tag>`,
  and a single-word NPC name such as `sequoia`. The target must be visible
  on the client's side. If unavailable, the camera uses the captured static
  pose for that shot. Follow offsets can be edited in the scene JSON.
- `static` switches a shot back to its saved camera pose.
- `delete-shot` removes a shot; deleting all shots leaves the file but
  preview is rejected until a new shot is captured.
- `preview` ONLY moves the local render camera. It does not teleport the
  player or cause story stages, starter selection, quests, NPC spawns, or rewards.
- Edit JSON files directly on the server, then preview again. No reboot is
  required: each playback reads the file fresh.
- `sequoia_lab` plays automatically on genuine Rin Lab actor ENTER if both
  the server's scene layout is enabled and the client advertises the new
  `pridecore:camera_director_v1` protocol. The original Rin animation,
  dialogue and starter-GUI continuation are still controlled by the Story module.
- **Dimension binding**: A scene created in the Overworld will not preview in a
  different world. In a Multiworld staging lab, change the JSON `dimension`
  value to the actual in-game dimension ID (or create a fresh scene in that world).

Security: strict alphanumeric IDs, dimension checks, entity/player proximity
checks, per-owner channel, bounded packet size, bounded shot count/duration,
atomic JSON saves, no client->server progression packets. Preview is intended
for development and requires access to the matching staging server.

Deployment: install ONLY ONE server PrideCore JAR, keeping the unified RCT
League/NPC assets from the original staging build. Replace Preview 10 client
with Preview 11 client. Never install the companion visual mod on the server.

## Reusable event binding and external NPC integration

The server automatically creates `sequoia_lab.json` with `"trigger":"RIN_LAB_INTRO"` and `"dimension":"*"` so it works in the built lab's current world. Capturing the first shot converts that wildcard to its actual world ID. Other story scenes can bind their events:

```mcfunction
/pridecore cinema bind rin_canyon RIN_CANYON_REUNION
/pridecore cinema bind rin_summit RIN_SUMMIT_REUNION
/pridecore cinema bind sequoia_lab RIN_LAB_INTRO
/pridecore cinema bind sequoia_lab none
```

To play a scene for a specific online player from a trusted admin command, command block, or server-side NPC integration:

```mcfunction
/pridecore cinema play example_scene Blions
```

This requires OP level 4 and does not alter quests or the NPC story. For a non-operator NPC trigger, configure the server/NPC integration to execute the command with server authority; don't grant players an OP-only command. A trigger for a new future story type may require wiring its original story event, but the camera engine and shot rendering need no redesign.
