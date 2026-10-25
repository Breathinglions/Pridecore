# Module 04 — Chat & Identity integration contract

**State:** planned consolidation; no deployed chat or resource-pack change authorized by this document.

## Why it is a separate module

The tested player-facing rank/chat/nametag appearance is not equivalent to the quiz code on the community development branch. Treat Chat & Identity as a dedicated, first-class Pride Core subsystem, with an independent module acceptance gate.

## Existing source and ownership found

- **Deployed integration:** LuckPerms is the canonical authority for roles, prefixes, permissions and rank selection; Styled Chat currently formats player chat. Do **not** replace or double-format it until the current installation has been audited.
- **Visual assets:** compact Cobblemon Poké Ball sprites and six role badges, rendered using the server-provided Polymer/resource pack. The previously approved v0.3.1 compact design is a preservation requirement. Maintain sprite code points and sizing unless a visual change is approved.
- **Community feature branch:** `feature/pride-community-impactor-ci` contains `modules/community`: quiz and scramble, rank-colored winner display, `/prideannounce`, brand announcement badges, leaderboard, Impactor reward bridge (payments off by default), and multiple resource-pack ZIP variants. Some of these should emit formatted notification events through the Chat & Identity service when eventually integrated; they are not evidence that every part of live player chat or overhead nameplates has been migrated.
- **Main branch:** Flashpoints proof-of-concept remains separate from chat and must not be affected.
- The exact currently deployed chat/nametag JAR and full prior source may differ from the branch. Inventory actual server artifacts and packaging before choosing files to port.

## Scope and requirements

1. LuckPerms-driven six standard ranks and per-user prefix overrides, respecting permissions. Cache safely and respond to rank changes.
2. Existing genuine Cobblemon sprite-based compact rank display, actual glyph placement and overhead nameplates; assess client/resource-pack requirements.
3. Formatted player messages with consistent rank name, prefix, color and chat channel. Ensure compatibility with Styled Chat and avoid two chat-formatting handlers.
4. One notification service for join/leave, server announcements, shiny/legendary/raid/world-boss notices, quiz/scramble winners and future story milestones. Prevent duplicate event subscriptions.
5. Channel-specific formatting, rate limiting, safe escaping of user-authored text, admin authorization, player accessibility/fallback glyphs.
6. Central style-guide and asset versioning. Keep announcements compatible with existing Spawn Notification localization and forced Polymer pack until tested otherwise.
7. Explicit, named adapters for community events. Quiz money payouts remain in Economy module; don't move transaction logic into Chat & Identity.

## Modern chat design requirements — approved direction (2026-10-24)

**Goal:** Pride Region chat must feel like a polished, unified modern multiplayer game interface, rather than default Minecraft chat with noisy prefixes. This is a quality requirement of Module 04, not an optional cosmetic afterthought. Preserve previously approved v0.3.1 compact genuine Poké Ball/rank sprite dimensions and glyph IDs unless user explicitly approves modifications.

### Visual language

- **Compact identity line:** existing Poké Ball/rank sprite + LuckPerms rank presentation + clear player name + restrained separator + naturally readable message. Consistent baseline spacing, glyph offsets, contrast, and line wrapping.
- **Restraint:** primarily light-neutral chat text; colors reserved for rank accents and semantically distinct channels (world news, encounters, competitions, staff announcements). No rainbow gradients, repeated oversized headers, or arbitrary emojis.
- **Branded event notices:** concise category indicator plus one-line message; multiline announcements only for genuinely important events. Honor different event severity and avoid overflowing the ordinary chat channel.
- **Interactive identity when supported:** player-name hover information, optional click-to-message/safe suggestion actions and accessible text alternatives, without relying on trusted identity coming from player-authored text.
- **Polish:** balanced punctuation, consistent capitalization, chat preview examples, minimal join/leave spam, flood control, user-configurable notification categories/muting where technically supported, and predictable fallbacks for missing assets.
- **Announcement channels:** one shared renderer for story milestones, quiz/scramble, raid and world boss activity, special Pokémon spawns and community events. Deduplicate by stable event source/ID, with source-specific cooldown and relevance limits.
- **Do not compromise signed chat:** preserve Minecraft 1.21.1 player message signing/secure-reporting behavior through supported Fabric chat hooks; never impersonate players through synthetic unsigned server messages. Third-party Styled Chat must have exactly one formatting owner.

### Two rendering tiers

1. **Server-only baseline (first release):** styled Minecraft Text components, existing sprite resource pack, LuckPerms rank data, sensible event hierarchy and consistent naming. Cannot rely on rich custom chat backgrounds or floating chat bubbles solely from ordinary server components.
2. **Enhanced client HUD (later, opt-in companion where feasible):** improved message grouping, timestamps/muting controls, refined chat panel layout, opacity/animation, better wrapping and custom icon arrangement. Requires a real client-side mod and modpack compatibility tests, and must gracefully fall back to server-only formatting for players without it.

### Representative layouts (text prototypes, NOT actual sprites)

```text
[existing sprite] Trainer Alex  ›  Anyone doing raids?
[existing sprite] VIP     Nova  ›  Meet at Pride City!
[RAID] A tier-five den is active near Thunder Mountain.
[PRIDE] The weekly tournament registration has opened.
```

The above placeholders must be replaced by existing approved sprites rather than new plain-text rank tags; use compact consistent widths. The identity line should not redundantly display the rank twice if LuckPerms already supplies a full glyph prefix.

### New module acceptance criteria

- Before implementing, provide a style specification with mockups of ordinary chat, private messages, join/leave, six ranks, shiny/legendary/raid notices, quiz winner, server announcements and story objectives.
- Compare in-game screenshots at 100% and 75% chat scales and on narrow client resolutions; contrast, wrapping and glyph alignments must remain legible.
- Confirm single-message rendering with Styled Chat and other notification mods, with no duplicate join/leave or spawn alerts.
- Confirm no permission/rank mismatch after LuckPerms updates, login/reconnect and restarts.
- Test modpack/resource-pack disabled or outdated: legible fallback instead of placeholder boxes. Keep sprite asset pack SHA1/version control.
- Verify server-only baseline with an unmodified client and optional enhanced HUD with the matching client mod; no forced client requirement until explicitly approved.
- Document which screenshots are design mockups, which are runtime evidence, and which claims remain untested.

## Proposed internal API boundaries (not yet implemented)

- `IdentityView`: read-only UUID → rank/prefix/display components.
- `ChatPresentation`: renders chat messages without owning player permission records.
- `NotificationBus`: typed events + event IDs; exactly one presentation per routed source.
- `NameplateRenderer`: overhead nameplates and client fallback behavior.
- `ResourcePackContract`: version/codepoint compatibility checks and SHA1 validation.

## Module acceptance journey

- A non-OP Trainer, VIP, Builder, Moderator, Admin and Owner each displays correct rank/sprite/name **without changing LuckPerms permission assignments**.
- Chat appears once, with correct prefix and no duplicate Styled Chat output, after a LuckPerms rank change and after restart.
- Overhead nameplates are visible and correct for another player; no ghost tags or duplicated names.
- One join, one leave, one raid/shiny test notice, one quiz/scramble win, one admin announcement — each emits **exactly one** correctly branded result.
- Existing resource pack still renders icons (and graceful fallback when missing). Client-only assets remain legal/licensed for distribution; original upstream mods untouched.
- Two players, no OP access, full persistence and restart tests. Correct formatting in all supported contexts.
- Existing LegendaryQuests, FTB Quests, Impactor, APS, Flashpoints and main chat remain unchanged outside explicitly tested adapters.

## Safe migration sequence

1. Archive deployed chat JAR(s), Styled Chat config, LuckPerms meta/prefix conventions, Polymer pack ZIP+SHA1, and take screenshots.
2. Compare sources against `modules/community` and note duplication points (quiz, announcements, third-party spawn notifications, nameplates).
3. Implement module and tests in a new branch while keeping resource assets/callback API stable.
4. Stage behind disabled flags alongside existing integrations. Validate no double broadcast and no nameplate stacking.
5. Replace legacy handler **only after** full QA and verified rollout, with a tested rollback option.

**Not part of module:** alternate currency, reward payouts, starter selection, gyms, storyline or Flashpoint combat.
