#!/usr/bin/env python3
"""Fail-closed static contract: server-owned, per-viewer camera authoring."""
from pathlib import Path
root=Path(__file__).resolve().parents[3]
server=root/"modules/core/src/main/java/com/prideregion/core"
client=root/"modules/client/src/main/java/com/prideregion/scenes/client"
core=(server/"cinema/CameraDirectorModule.java").read_text()
sp=(server/"cinema/CameraDirectorPayload.java").read_text()
cp=(client/"ClientCameraDirectorPayload.java").read_text()
renderer=(client/"ClientCameraDirector.java").read_text()
bridge=(server/"story/StorySceneVisualBridge.java").read_text()
init=(server/"PrideCoreMod.java").read_text()
mod=(client/"PrideScenesClientMod.java").read_text()

for p in (sp,cp):
    assert 'Identifier.of("pridecore", "camera_director_v1")' in p
    assert 'buf.readUuid(), buf.readBoolean(), buf.readString(24_000)' in p
    assert "buf.writeUuid(session)" in p
    assert "buf.writeBoolean(stop)" in p
    assert "buf.writeString(sceneJson, 24_000)" in p
assert "CameraDirectorModule.register();" in init
assert "CameraDirectorModule.start(server);" in init
assert 'private static int moveShot(' in core
assert 'captured.add(key, existing.get(key).deepCopy())' in core
assert 'out.append(" | lab camera ")' in core
assert "CameraDirectorModule.stop();" in init
assert "CameraDirectorModule.commands()" in init
for literal in ('"capture"','"preview"','"play"','"bind"','"list"','"timing"','"follow"','"static"','"move-after"','"move-first"','"delete-shot"'):
    assert literal in core,literal
assert '.requires(s -> s.hasPermissionLevel(4))' in core
assert 'resolve("data").resolve("pride_core").resolve("cinematics")' in core
assert 'matches("[a-z0-9_-]{1,48}")' in core
assert 'Files.createTempFile' in core
assert 'Files.move(tmp, file, ATOMIC_MOVE, REPLACE_EXISTING)' in core
assert 'JsonParser.parseString(Files.readString(file))' in core
assert 'if (shots == null || shots.size() > 32)' in core
assert 'if (total > 2400)' in core
assert 'ServerPlayNetworking.canSend(viewer, CameraDirectorPayload.ID)' in core
assert 'viewer.getWorld().getRegistryKey().getValue().toString()' in core
assert 'viewer.getX() - first.get("x").getAsDouble()' in core
assert 'new CameraDirectorPayload(session, false, JSON.toJson(scene))' in core
assert 'new CameraDirectorPayload(session, true, "")' in core
assert 'CameraDirectorModule.startBoundScene(viewer, cue.session(), cue.scene().name())' in bridge
assert 'CameraDirectorModule.endBoundScene(viewer, cue.session())' in bridge
assert 'scene.addProperty("trigger", "RIN_LAB_INTRO")' in core
assert 'scene.addProperty("trigger", event)' in core
assert 'JsonObject scene = load(name);' in core
assert 'if (!"*".equals(scene.get("dimension").getAsString())' in core
assert 'if (!requiredWorld.equals("*")' in renderer
assert 'ClientPlayNetworking.registerGlobalReceiver(ClientCameraDirectorPayload.ID' in mod
assert 'PayloadTypeRegistry.playS2C().register(' in mod
assert 'DIRECTOR.clear()' in mod
assert 'ClientTickEvents.END_CLIENT_TICK.register(DIRECTOR::tick)' in mod
assert "cameraPositionClear" not in renderer or 'spaceClear' in renderer
assert 'getCollisionShape(client.world, at).isEmpty()' in renderer
assert 'JsonParser.parseString(packet.sceneJson())' in renderer
assert 'segmentClear(client, currentFrame.pos(), point)' in renderer
assert 'camera.getCameraEntity' not in renderer
for forbidden in ('setCameraEntity(', 'requestTeleport(', 'ClientPlayNetworking.send(',
                  'starter_chosen', 'openstarterscreen', 'ProfileStore', 'OnboardingRewards.claim'):
    assert forbidden not in renderer, forbidden
assert 'client.options.hudHidden = originalHudHidden' in renderer
assert 'originalPlayerPos' in renderer
print("PASS: server-authoritative Cinematic Director captures/preview, validated private wire, return safety")
