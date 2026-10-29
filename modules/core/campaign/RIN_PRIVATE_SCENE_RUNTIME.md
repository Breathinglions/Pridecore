# Module 02 — Rin's private Lab entrance (server scene foundation)

**Status: implemented as a pure Java coordinator; native Minecraft/Cobblemon integration and staging validation remain outstanding.**

The `StoryPrivateSceneDirector` class provides a per-player, session-tokenized event timeline for Rin's introduction in Professor Sequoia's Lab. It does not summon entities, move Minecraft players, intercept existing native NPC services, choose a starter, grant any facts/rewards or connect a client renderer. It must not be treated as a completed in-game cinematic.

The intended sequence is Sequoia's introduction; owner-visible Rin entering at `lab_entry`, walking toward `sequoia_desk`, facing the player, greeting them and declaring their ambition; Sequoia's closing line; removal of the temporary private projection; then requesting the **existing native starter-selection GUI**, not replacing or duplicating Cobblemon's native starter action.

An authorized native-Sequoia integration will need to:

1. Verify the real player's eligibility with the existing native starter service and start the scene only once per viewer. No parallel permanent Rin NPCs and no second Sequoia.
2. Feed `tick()` exactly once per Minecraft server tick; route every event only to its `owner` UUID and its exact `session` token. Render logical movement anchors without broadcasting, adding an entity to world persistence, or colliding with other players. Resolve those anchors only after builder setup.
3. Before honoring `OPEN_NATIVE_STARTER`, re-check Cobblemon's real eligibility and resume the **original** starter-selection action. Do not grant or fake a Pokémon via Pride Core.
4. On logout, dimension switch, death, GUI cancellation or server shutdown, call `cancel()`, `checkDimension()` or `clear()` and clean up owner-only visual actors. Cancellation must never automatically give a starter or advance the story.
5. Offer `skip(player, session)` as a safe reduced-motion/text-only fallback: it returns all narrative lines, removes the projected actor and requests native starter UI at most once.
6. Continue to mark `pride:met_rin` only from existing verified `starter_chosen` and `sequoia_met` plus Mom/Professor facts. The scene itself is not progress proof.

The coordinator enforces one active scene per player, a 64-viewer cap, deterministic cue order and a finite duration. It supports independent concurrent players and one-use handoff tokens. A client or packet adapter is still required for the actual walking Rin projection, camera treatment and physical world presentation. Scene events are not bound to the main `StoryModule` yet, to avoid intercepting Sequoia's currently working native GUI without a verified compatibility adapter.

**Java test coverage:** dual-player session isolation, repeated starts, stale token refusal, skip/normal single handoff, scene completion, dimension invalidation, disconnect/shutdown cleanup. Run through `bash modules/core/qa/test-story.sh`.

**Acceptance remains blocked** until the native Cobblemon 1.8.1 GUI path and 1.21.1 Fabric private projection are proven on the disposable staging server with two players. Storyline Pokémon battles remain strictly Cobblemon/RCT; hybrid Minecraft combat belongs only to Flashpoints, Operations, raids and World Bosses.
