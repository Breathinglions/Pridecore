#!/usr/bin/env python3
"""Regression contract for safe, persistent, per-player proximity cinematics."""
from pathlib import Path
root=Path(__file__).resolve().parents[3]
code=(root/"modules/core/src/main/java/com/prideregion/core/cinema/LocationCinematicTriggerModule.java").read_text()
director=(root/"modules/core/src/main/java/com/prideregion/core/cinema/CameraDirectorModule.java").read_text()
entry=(root/"modules/core/src/main/java/com/prideregion/core/PrideCoreMod.java").read_text()

for s in (
    'ServerTickEvents.END_SERVER_TICK.register(LocationCinematicTriggerModule::tick)',
    'LocationCinematicTriggerModule.registerHooks();',
    'LocationCinematicTriggerModule.start(server);',
    'LocationCinematicTriggerModule.stop();',
):
    assert s in code+entry, s
assert 'root.then(LocationCinematicTriggerModule.commands());' in director
for token in ('"create"', '"enable"', '"disable"', '"once"', '"cooldown"',
              '"reset"', '"remove"', '"info"', '"list"'):
    assert token in code, token
assert "false, true, DEFAULT_COOLDOWN" in code
assert 'Math.abs(player.getY() - y) <= 4.0' in code
assert 'dx * dx + dz * dz <= radius * radius' in code
assert 'if (wasInside || !zone.enabled() || !zone.eligible(player)) continue;' in code
assert 'inside.add(zone.id());' in code and 'inside.remove(zone.id());' in code
assert 'CameraDirectorModule.cameraBusy(player)' in code
assert 'CameraDirectorModule.playFromLocation(player, zone.scene())' in code
assert 'activeCameras.put(viewer.getUuid()' in director
assert 'if (cameraBusy(viewer)) return false;' in director
assert 'ServerPlayNetworking.canSend(viewer, CameraDirectorPayload.ID)' in director
assert 'Set<UUID>' in code and 'UUID.fromString' in code
assert 'writeCompleted(zone.id(), seen);' in code
assert 'Files.move(tmp, file, ATOMIC_MOVE, REPLACE_EXISTING)' in code
assert 'readCompleted(zone.id())' in code
assert '"requireTags"' in code and '"excludeTags"' in code
assert 'getCommandTags()' in code
assert 'new Zone(id, scene' in code
assert 'readZone(file)' in code
assert 'if (!Files.isRegularFile(sceneFile))' in code
assert 'zone.contains(player)' in code
assert 'player.isSpectator()' in code
assert 'player.getWorld().getRegistryKey().getValue().toString()' in code
assert 'resolve("data").resolve("pride_core").resolve("cinema_triggers")' in code
assert 'source.sendError(Text.literal("Cinematic Zone:' in code
for forbidden in ('setCameraEntity(', 'requestTeleport(', 'setYaw(', 'setPitch(',
                  'OnboardingRewards.claim(', 'ProfileStore', 'starter_chosen',
                  'grantBadge(', 'awardBattle('):
    assert forbidden not in code, forbidden
print("PASS: location triggers are enter-only, dimension-safe, once-per-player, persistent and profile-isolated")
