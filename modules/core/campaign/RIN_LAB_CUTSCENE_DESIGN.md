# Module 02 — Rin's First Entrance at Professor Sequoia's Lab

**Status: APPROVED CONCEPT / NOT IMPLEMENTED / REQUIRES STAGING QA**  
Platform: Minecraft 1.21.1 / Cobblemon 1.8.1 / Fabric / Pride Core.  
Related: `ONE_NPC_ONE_CHARACTER_POLICY.md`, starter-linked mandatory Rin checkpoint in source v0.4.2.

## Player-facing script

Trigger at the single existing Sequoia NPC when a player with Mom completed chooses the first-starter conversation. Before opening the **unchanged** native Cobblemon starter selector:

1. Sequoia: "Before we begin, there's someone I'd like you to meet."
2. Close or suspend the dialogue UI. Keep player in the Lab.
3. Door opens / soft footsteps; Rin appears at the **Lab entry anchor**.
4. Rin *walks*, following defined waypoints to Sequoia's desk, turns to player.
5. Rin: "Oh! You're choosing your first Pokémon today too? I'm Rin!"
6. Rin: "I'm going to be Champion someday. Guess we'll see who gets there first!"
7. Sequoia: "Competition is healthy when you look after your partners. Now, both of you—choose wisely."
8. Return to the SAME Sequoia dialogue context, then invoke the **original Cobblemon starter-selection action**. Do not mint an extra Pokémon or skip the original starter eligibility checks.
9. **Only after** validated `starter_chosen` and `sequoia_met`, add the one-time owned `pride:met_rin` checkpoint via Pride Core; the FTB mirror must have one quest ID, no duplicate.

## Per-player scene instance, not a shared server entity

A single **logical** Rin character with data identity `rin`, multiple plot appearances. The Lab cutscene actor is a **temporary viewer-specific projection/actor**, NOT a second persistent Rin in the world.

- The server allocates `StorySceneSession(playerUUID, sceneId, currentStep, startedAt, actorId)`, checked on the main server thread, with at most **one running scene per player**.
- Each scene's temporary Rin actor is visible **only to its owner** (per-player entity visibility or client-side model rendering). Actors are never saved with the world or selectable as a normal Quest NPC.
- Players entering together do not share actor movement, dialogue, stage updates or camera. Nonparticipants continue normal Lab use.
- Scene uses fixed, admin-configured Lab anchors and a short walk path; all effects and dialogue are explicitly addressed to initiating player.
- Opening a second Sequoia conversation during a scene must not start another scene; rapid clicking is idempotent.
- On logout, dimension change, death, player cancellation, server shutdown, or timeout, clean up private actors, movement/camera control and sessions. Return to a safe Sequoia dialogue state on next interaction; never silently grant a starter, money or Rin completion.
- Persist a **separate** `pride:rin_intro_seen` fact only after the successful completed cutscene if replay suppression is desired, without making that cosmetic fact a replacement for verified starter ownership. A reconnect can retry the scene if not seen. Existing veteran players with already-verified starter can get one clear introduction/recap without re-opening starter selection.
- Preserve `pride:met_rin` and `pride:verdant_victory` on prior accounts. Never set stage `pride:rival_introduced` over Theo/Gym/Act stages.
- A skippable reduced-motion route or client fallback may bypass camera/movement, but still shows Rin's lines and triggers only the original starter action. No hard progression softlocks if optional visuals fail.

## Engineering choices to prototype

**Preferred long term:** Pride Core server owns authoritative scene state, and a small Pride Client renderer/packet layer draws the private actor, interpolated walk, per-player camera/audio effects, and event sequencing. This yields strong simultaneous-player isolation and future cutscene reuse for LegendaryQuests / Resonance Crisis. Verify version compatibility and security before shipping.

**Alternative server-only prototype:** server-sent per-viewer entity spawn/metadata/movement packets with correct Cobblemon renderer identifiers. This is feasible in concept but requires verifying packet behavior for Fabric 1.21.1 Cobblemon 1.8.1; do not assume ordinary Minecraft `/summon` is isolated. Do not use Fabric FakePlayer as a simple visual actor; FakePlayer is a real simulated server player and not equivalent to a per-viewer client-only NPC.

**External reference:** Journey documents owner-only visibility, NPC walking paths and cutscene timelines for Cobblemon Fabric; it is an architecture reference only. Do not install Journey or Ceremony automatically or make this module depend on them without user approval and compatibility test.

## Suggested data file, not implemented yet

`config/pride_core/scenes/rin_starter_intro.json`:

- `scene_id`: `pride:rin_starter_intro`
- `trigger`: `sequoia.starter_dialogue` (only Mom + no starter + not playing)
- `npc`: `rin` (canonical character identity)
- `owner_visibility`: `OWNER_ONLY`
- `entry_anchor`, `waypoints`, `stop_anchor`, `facing_target`: admin-configurable world positions
- `dialogue_script`: the Sequoia/Rin lines above
- `on_complete`: reopen original starter-selection action
- `on_cancel`: cleanup / safe resume, no reward mutation
- `timeout_ticks`: finite, server-configured

## QA acceptance

- [ ] One new non-OP player: Sequoia -> private Rin walks in -> starter screen -> `starter_chosen` -> `pride:met_rin` once -> Theo objective.
- [ ] Two new players concurrently standing beside each other: **each sees only their own Rin scene**, no shared teleport/push/collision, no mixed dialogue, and each progresses separately.
- [ ] One returning player beside new player: returning player sees ordinary Lab, can talk to Sequoia; no cutscene retrigger from someone else's start.
- [ ] Cancel/escape, starter-screen cancelled, disconnect during entrance, server restart: no duplicate actors and no false checkpoint.
- [ ] One player can rerun ordinary Sequoia service as allowed by Cobblemon without replaying scene or granting multiple starters.
- [ ] Existing old profiles are retroactively reconciled without resetting Prologue, FTB Quest IDs, tags, paid rewards or `pride:verdant_victory`.
- [ ] Actors, scripts, camera state and events don't leak across worlds or player sessions.
- [ ] Optional client renderer missing or broken -> safe non-cinematic dialogue fallback, not a progression dead end.

**Release gate:** Do not claim this is deployed by the v0.4.2 content-only Rin dialogue introduction. That version provides Professor text and server-side starter-linked progress, **not** an animated entry or multiplayer instance runtime.
