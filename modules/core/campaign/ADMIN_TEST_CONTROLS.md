# Module 02 — Operator Test Controls (staging only)

The following commands are available only to server operators at permission level 4 when the Pride Story module is active. They **do not** replace native Cobblemon victories.

| Command | Purpose |
| --- | --- |
| `/pridecore story admin inspect <player>` | Show player stage, revision, current finale phase and temporary checkpoint counts. |
| `/pridecore story admin reset-session <player>` | Clear active dialogue, in-flight scripted battle, and unfinished finale sessions; **preserves all saved story progress**, badges, and economy. |
| `/pridecore story admin finale-start <player>` | Rehearse the engine phase controller **only after** authentic Cael victory and engine clue facts exist. |
| `/pridecore story admin talk <player> <npc>` | Existing NPC conversation preview; normal progression gates still apply. |
| `/pridecore story validate` | Existing story-content validation. |

Safeguards: no cheat win command is supplied; arbitrary player fact grants and stage skips are intentionally prohibited. A session reset will **not** reset a character to the beginning of the story. For a true clean-room test use a fresh test account/profile on the staging server or a properly backed-up disposable staging world. Never delete live player data.

The finale controller is an internal, nonpersistent stage machine awaiting actual world-marker and defender hooks. Inspect/rehearsal is provided to assist operator testing during development and does not constitute a playable boss instance or proof of runtime validation. Use only a staging copy with `story_enabled=true` and a tested build.
