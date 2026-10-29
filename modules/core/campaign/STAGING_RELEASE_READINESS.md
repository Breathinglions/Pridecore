# Module 02 — development-to-staging readiness checklist

**Branch:** `feature/pridecore-module02-story-npcs`  
**Production rollout:** blocked until a complete staging multiplayer walkthrough.

This document separates Java/build readiness from actual Cobblemon/RCT functionality. A successful GitHub Actions build does **not** prove a playable server.

## Implemented in development

- Authored Hearthleaf onboarding and 27 story dialogue identities.
- Physical right-click story conversations on tagged narrative Cobblemon NPCs, clickable chat choices, recurring-character chapter selection and stale-choice replay protection; existing Sequoia/Theo/Avery/Thorn native services are preserved.
- Nine non-League scripted trainer fights with authored rosters and original eight-Gym badge requirements.
- Exact native RCT launch ledger authentication; verified player/NPC victory handling; per-player and per-physical-trainer locks.
- Failure-safe battle reservation cleanup and bounded retry of already-authenticated victory profile writes.
- Environmental clue interactions with nine atmospheric discovery/revisit passages and per-player persistent Resonance finale checkpoints.
- Four conduit devices, three verified shield defender wins, final engine stabilization and completion fact.
- Authored cinematic introductions and encounter-specific knockout-reactive visual phases.
- Operator-only physical actor bindings and local Hearthleaf, Eclipse, finale and arena audits.
- Optional per-dimension world isolation; optional five-device staging datapack.
- Player-facing encounter checklist and exact remaining finale objectives.
- Standalone tests for battle ownership, revoked prerequisites, malformed victory metadata, checkpoint recovery, temporary I/O faults, and concurrent players.

## Still unverified; do not ship

- Actual physical Cobblemon NPC click-event ordering, in-game clickable chat UX, native trainer entity spawning and Cobblemon 1.8.1/RCT 0.19.0-beta battle initialization.
- Real battle turns, faint event ordering, player win/loss handling, simultaneous players and relogs.
- The physical world geometry, decorations, NPC models, trainer positioning and marker placement for the full region.
- Native player UX of the scripted dialogue and its integration with FTB Quests and the real client pack.
- Full cinematic visual and audio quality on the exact installed shaders and resource pack.
- Gameplay balance, difficulty pacing and Pokémon-only RCT battle strategies; cinematic faint-triggered phases are **presentation**, not damage/AI changes. Story boss fights must never use Minecraft combat.
- Dedicated staging server soak test, world claims, unloaded chunks, player permissions and cross-world exploits.

## Staging procedure after the 90% code-completion target

1. Back up the production server. Create a separate isolated staging world/server and disposable test profiles. Keep `story_enabled=false` in production.
2. Configure `story_allowed_dimensions` to exactly the test world's registry ID. Enable Module 02 **only in staging**.
3. Run the operator API preflight and local physical story audits. Bind all 27 narrative actors and nine battle trainer entities; place the nine clues and five finale click props.
4. Complete onboarding, Route 1, Gyms, Eclipse, Cael, and the finale in order with a fresh disposable player profile. Verify no stage can be skipped.
5. Verify narrative NPC click dialogue (including Rin/Rowan's chapter transitions), preserved native Sequoia/Theo/Avery/Thorn functionality, player movement and old chat button rejection.
6. Repeat key fights with two staging players: one winning, one losing, simultaneous matches against separate trainers, and sequential access to the same trainer.
7. Test disconnect, restart, checkpoint recovery, repeated clicks, expired retries, missing modded particles and server-side logs.
8. Compare saved player facts, FTB Quest markers, economy, League results, and player inventories before and after; confirm no unintended side effects.

Do not interpret the percentage development estimate as gameplay readiness or guarantee of production stability. Testing may reveal more code work.

## Combat separation — mandatory staging acceptance

The storyline bosses are **exclusively Pokémon battles**. Verify on a disposable
staging player that each `pride_battle_*` actor triggers Cobblemon/RCT combat
from interaction, cannot be beaten by hitting it with a sword, bow, fist or
other Minecraft damage, and awards no story credit from entity deaths.
Normal unrelated mobs and the original League NPCs must **not** gain damage
immunity. Verify that turning `story_enabled=false` leaves unrelated gameplay
untouched. See `COMBAT_BOUNDARIES.md`.

Pride Core Flashpoints, Operations, Raids and World Bosses will own separate
**hybrid Minecraft + Pokémon** combat systems. Module 02 does not implement,
simulate or take over those endgame encounter systems.
