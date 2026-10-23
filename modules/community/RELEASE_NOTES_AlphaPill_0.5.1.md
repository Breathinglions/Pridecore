# Pride Community Alpha-pill resource pack patch (v0.5.1)

The game code remains **v0.6.0-rc1**. No new Minecraft JAR is needed.

New resource pack file: `resourcepack/Pride_Region_Community_Identity_v0.5.1_AlphaPill_1.21.1.zip`.

Pack SHA-1: `958323fb8058fac9dd6fef145e78cbfa8f6b32d3`.

Exactly one resource-pack file differs from the v0.5 pack: `assets/spawn_notification/lang/en_us.json`, key `spawn_notification.alpha` changed from `\\uE11D Alpha` to `\\uE11D`. This prevents the visually duplicated **ALPHA pill Alpha Seviper** announcement. All rank glyphs, actual Cobblemon Poké Ball textures, other category badges and text remain unchanged.

Install by replacing the prior Pride Community identity resource pack in the existing Polymer forced-pack pipeline and updating its URL/filename and SHA-1; then reconnect or reload client resources. **Do not install both packs at the same time.** This only changes Minecraft client translation; it does not change the server-side Cobblemon Spawn Notification mod nor its MC2Discord bridge.

Discord caution: If the user's setup uses `cobblemon_spawn_notification_discord` (its MC2Discord subscription), removing the Spawn Notification backend would remove the event source, and Discord spawn alerts would stop until Pride Core implements an equivalent event->Discord publisher. Keep the existing mods installed until that replacement exists.
