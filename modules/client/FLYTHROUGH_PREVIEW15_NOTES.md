# Pride Scenes Preview 15: complete the gym flythrough

The camera route can end early if its curved path goes through a
wall. Preview 15 tests the route before starting and automatically falls
back to straight lines between saved camera positions when needed.

Both paths travel continuously through the gym. The camera slows only at
the beginning and end of the full tour, not at every waypoint.

If no route is clear, the client shows the two named camera points that
need another waypoint between them, such as "hallway" and "arena".
Stand in the doorway, capture the extra point, and insert it after
the first of those two points.

Only the optional client JAR changes. Keep the Preview 14 server JAR.
Remove Preview 14 client JAR and use Preview 15 client JAR instead.

Commands:

    /pridecore cinema playback thorn_gym_tour flythrough
    /pridecore cinema flight-time thorn_gym_tour 600
    /pridecore cinema preview thorn_gym_tour

If the route is blocked between hallway and arena:

    /pridecore cinema capture thorn_gym_tour doorway
    /pridecore cinema move-after thorn_gym_tour doorway hallway

Advanced: in the saved scene JSON, add a root property
"pathStyle": "precise" to force exact segments. Allowed values are
auto (default), smooth, precise. The older server preserves this
property when saving scenes. Existing story scenes are unaffected.

No story dialogue, NPC appearance, world blocks, player coordinates,
or badge progression is modified by this client renderer update.
