# Module 02 — Operator Test Controls (staging only)

The following commands are available only to server operators at permission level 4 when the Pride Story module is active. They **do not** replace native Cobblemon victories.

| Command | Purpose |
| --- | --- |
| `/pridecore story admin encounters <player>` | Read-only list of scripted encounters showing LOCKED, READY, BUSY, or COMPLETE; no battle is started. |
| `/pridecore story admin reset-preview <player>` | Preview permanent Module 02 story facts that would be cleared; no mutation. |
| `/pridecore story admin reset-playthrough <player> confirm` | Reset owned Module 02 campaign facts and return story stage to Mom; keeps non-story facts and external achievements intact. |
| `/pridecore story admin inspect <player>` | Show player stage, revision, current finale phase and temporary checkpoint counts. |
| `/pridecore story admin reset-session <player>` | Clear active dialogue, in-flight scripted battle, and unfinished finale sessions; **preserves all saved story progress**, badges, and economy. |
| `/pridecore story admin finale-start <player>` | Rehearse the engine phase controller **only after** authentic Cael victory and engine clue facts exist. |
| `/pridecore story admin talk <player> <npc>` | Existing NPC conversation preview; normal progression gates still apply. |
| `/pridecore story validate` | Existing story-content validation. |

Safeguards: no cheat win command is supplied; arbitrary player fact grants and stage skips are intentionally prohibited. A session reset will **not** reset a character to the beginning of the story. For a true clean-room test use a fresh test account/profile on the staging server or a properly backed-up disposable staging world. Never delete live player data.

The finale controller has durable per-player checkpoint storage and server-side physical conduit/core and defender battle adapters. Inspect/rehearsal remains useful for staging but does not prove a playable boss instance until the exact installed RCT/Cobblemon stack passes in-game verification. Use only a staging copy with `story_enabled=true` and a tested build.
\n**Important for full onboarding replay:** This reset does not erase your existing Cobblemon starter, League badges, external Minecraft command tags, FTB quest data, or inventory. Reconciliation may recognize previously earned external milestones. For a genuine first-player experience, use an isolated staging player/profile with a backup; do not clear external production data.\n
## Player-facing encounter checklist

Players can run `/pridecore story encounters` to see all nine authenticated story battles marked `LOCKED`, `READY`, or `COMPLETE`, with their completion count. It only reads persisted story facts; it does not attempt to start an RCT match or modify badges, money, quests, Pokémon, or rewards. Players can also run `/pridecore story finale` for the Resonance Engine phase and next objective.

## Operator staging readiness checks

Run `/pridecore story admin runtime-preflight` to verify that the server exposes the expected RCT/Cobblemon classes and methods and that all nine authored battle rosters can be parsed. A PASS means only the **binary preflight and authored data** are present; it is not evidence of a functioning native Pokémon battle, correct visuals, or production readiness. A failure is logged with stack trace and the command fails closed.

Run `/pridecore story admin audit-markers` while standing near your staged NPCs and props. The local 32-block scan reports invalid marker combinations and wrong entity types. See `STORY_WORLD_BINDING.md` for the safe bind/unbind commands.

Both tools are read-only with respect to player progression. They are operator level 4; neither should be exposed to normal players.

## Additional safeguards in this development branch

The encounter inspector and player checklist now reflect actual `minBadges` from the bundled story rosters, with badge counts derived only from the eight original League Gym tags. Use `/pridecore story admin audit-finale` at the staged finale arena to report missing, duplicated or wrongly typed core/conduit/defender entities within 64 blocks.

The `story_allowed_dimensions` setting in `config/pride_core/story.properties` can confine Module 2 interactions, battles, synchronization and dialogue choices to your selected staging-world dimension IDs (see `STORY_WORLD_BINDING.md`). This does not modify the Multiworld configuration or affect unrelated mods. It loads at startup. Blank means all dimensions for backward compatibility; `story_enabled=false` remains the installation default.

Knockout-driven boss effects are optional. If the Cobblemon faint-event API cannot be subscribed, verified battle-victory routing remains mandatory and the cinematic-only listener is skipped. This fallback is not an in-game test substitute.
