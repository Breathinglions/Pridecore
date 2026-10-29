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

## Later appearances — one Rin identity, no permanent duplicates

The same director now models `RIN_CANYON_REUNION` and `RIN_SUMMIT_REUNION` as separate owner-only **scene instances of the single canonical Rin character**. Each requests a later `RESUME_STORY_DIALOGUE` continuation (never `OPEN_NATIVE_STARTER`) and uses builder-defined logical anchor names (`canyon_ridge`, `canyon_overlook`, `summit_trail`, `summit_overlook`). None is world-spawned or battle-authoritative. The existing verified RCT victories and prerequisite facts remain unchanged.

The coordinator accepts starts only when a trusted server-side adapter decides the correct chapter and location; it must not guess character eligibility from client packets or spawn multiple Rin entities for simultaneous players. The 64-active-scene cap rejects excess starts safely. Automated tests include overflow and release behavior, as well as distinct later-scene handoffs. **These later scenes also still lack the client/private-actor adapter and are not playable world cinematics yet.**

## Operator-only rehearsal (no client mod required, staging only)

When `story_enabled=true` in the isolated staging dimension, OP4 can preview the narrative timing without creating NPCs, opening a starter GUI or changing player state:

```mcfunction
/pridecore story admin preview-rin <player> lab
/pridecore story admin preview-rin <player> canyon
/pridecore story admin preview-rin <player> summit
/pridecore story admin cancel-rin-preview <player>
```

Only the named player sees scene dialogue and temporary descriptive actionbar cues. The rehearsal explicitly labels visual entry/movement/face cues as `[Scene preview]`, and explicitly states that the original starter/dialogue UI **would** be resumed but **is not invoked**. This is an inspection of scripted narrative, not a visual preview of a walking Rin model. Scenes automatically retire when their player disconnects, switches dimensions, or the server stops. The operator can cancel an individual scene without affecting other viewers. Do not enable the story module in production to use these commands.

For future real gameplay, integrate the projector with the native Sequoia service only after testing the original Cobblemon GUI and the per-player private entity/client renderer; delete the preview text path from that production-facing workflow, but keep it available to admins as an explicitly separate test mode.

## Fail-closed native eligibility contract

`StoryPrivateSceneEligibility` supplies a server-side prerequisite gate for an eventual real Cobblemon adapter. The Lab scene requires Mom's story checkpoint and the absence of the original `starter_chosen` evidence. The canyon scene requires `pride:eclipse_contact` and is suppressed after `pride:rival_theory`; the summit scene requires `pride:frozen_warning` plus the ice clue and is suppressed after `pride:rival_ally`. This does **not** replace native service validation or a genuine physical clicked-entity check.

The eventual native integration should call `beginVerified(...)` with server-owned facts and verified legacy tags rather than unrestricted `begin(...)`. Before acting on `OPEN_NATIVE_STARTER`, the adapter must check `nativeStarterContinuationAllowed(...)`, revalidate the exact owner/session/world and **also** ask Cobblemon's actual original starter selector whether a selection is still allowed. The scene cannot mint starter eligibility or issue a starter by itself.

The OP4 `preview-rin` command intentionally bypasses these storyline prerequisites solely to rehearse prose and timing; its continuation never performs a native action. The additional CI suite checks denied starts, missing facts, duplicate scenes, stale starter evidence and later-scene isolation.

## Session-bound native continuation safety

The director now issues a **single-use, 120-server-tick expiration ticket** when the last scene cue requests a native action. The ticket records owner UUID, random scene session UUID, exact scene, intended handoff type and world registry identifier. A future native adapter must `claimContinuation(event, currentServerDimension)` **after** independently re-checking the original native service authorization. Claim consumes the ticket exactly once; repeated, forged, stale, cross-world or cancelled events cannot reopen a selector.

Tickets are also cleared on new scene start, disconnect, world change, cancellation and shutdown. The director limits outstanding continuations to 256 to keep memory bounded; reaching capacity refuses new continuation claims instead of widening authority. OP4 text previews intentionally leave these tokens unclaimed; they expire automatically. The story module still performs no native UI calls, no starter grants and no storyline combat modifications.
