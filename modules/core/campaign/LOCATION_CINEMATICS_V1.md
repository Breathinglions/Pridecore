# PrideCore Cinematic Location Triggers — staging release

This server-side extension uses the **same Preview 12 client JAR**. Only the
unified server PrideCore JAR changes. No world reset or recompile for future zones.

## Set up the first entrance (Verdant Gym / Thorn)

1. Author a scene with two or more camera shots by standing at the spots:

    /pridecore cinema capture thorn_gym_tour entry
    /pridecore cinema capture thorn_gym_tour arena
    /pridecore cinema timing thorn_gym_tour entry 100 25
    /pridecore cinema timing thorn_gym_tour arena 140 30
    /pridecore cinema preview thorn_gym_tour

2. Stand at **Thorn's actual gym entrance** and run:

    /pridecore cinema zone create thorn_entrance thorn_gym_tour 6

   Captures the standing position, dimension and six-block horizontal radius.
   The zone starts DISABLED. Walk outside the trigger radius, then run:

    /pridecore cinema zone enable thorn_entrance

3. Walk back within six blocks horizontally, and within four blocks vertically.
   The cinematic plays only for the entering player, then returns the camera to
   the real player's position. No movement/teleport or battle is triggered.

4. To repeat staging tests without resetting your player story:

    /pridecore cinema zone reset thorn_entrance Blions

   Leave and re-enter the zone to see it again.

## Additional controls

    /pridecore cinema zone info thorn_entrance
    /pridecore cinema zone list
    /pridecore cinema zone disable thorn_entrance
    /pridecore cinema zone enable thorn_entrance
    /pridecore cinema zone once thorn_entrance true
    /pridecore cinema zone once thorn_entrance false
    /pridecore cinema zone cooldown thorn_entrance 600
    /pridecore cinema zone remove thorn_entrance

Commands require OP level 4, inherited from /pridecore cinema.
Runtime proximity triggering works for ordinary players.

## Server files

- <world>/data/pride_core/cinema_triggers/zones/thorn_entrance.json
- <world>/data/pride_core/cinema_triggers/completed/thorn_entrance.txt
- <world>/data/pride_core/cinematics/thorn_gym_tour.json

Defaults are enabled=false, oncePerPlayer=true, cooldownTicks=600.
You can edit the zone JSON to set optional requireTags and excludeTags arrays;
these are vanilla Minecraft command tags, NOT badge/FTB quest facts. Zone file
edits require a server restart to reload. Cinematic scene JSON remains live
reload on the next playback. Per-player once completions persist across restarts.

Zones only fire when entering, not every second while standing inside. Enabling
a zone while already inside won't immediately fire; walk outside and return.
Cinematic playback requires a client advertising camera_director_v1 and will
not interrupt another active server camera scene. There is a 128-zone cap,
and the server samples player locations once every five ticks.

This is a *camera-only discovery trigger*. It does not start Pokémon battles,
move NPCs, grant rewards or enforce all story/badge prerequisites. Connect boss
interactions separately when those systems are ready. Configure Thorn's actual
entrance position in-game; no invented world coordinates are included.

Back up server/world and replace only the unified server JAR. Keep the already
working Preview 12 client JAR installed (exactly one copy).
