#!/usr/bin/env python3
"""Compile-independent fail-closed contract for Preview6's client-only camera."""
from pathlib import Path
import json

root=Path(__file__).resolve().parents[1]
src=root/"src/main/java/com/prideregion/scenes/client"
controller=(src/"RinCinematicCamera.java").read_text()
camera=(src/"mixin/CameraMixin.java").read_text()
hands=(src/"mixin/GameRendererMixin.java").read_text()
mod=(src/"PrideScenesClientMod.java").read_text()
projection=(src/"RinPreviewProjection.java").read_text()
fabric=json.loads((root/"src/main/resources/fabric.mod.json").read_text())
mixins=json.loads((root/"src/main/resources/pride-scenes-client.mixins.json").read_text())

assert fabric["environment"] == "client"
assert fabric["mixins"] == ["pride-scenes-client.mixins.json"]
assert mixins["required"] is True
assert mixins["client"] == ["CameraMixin", "GameRendererMixin"]
assert mixins["injectors"]["defaultRequire"] == 1
assert "@Mixin(Camera.class)" in camera
assert 'method = "update"' in camera
assert 'at = @At("TAIL")' in camera
assert "PrideScenesClientMod.camera().renderPose(" in camera
assert "setPos(pose.position().x, pose.position().y, pose.position().z)" in camera
assert "setRotation(pose.yaw(), pose.pitch())" in camera
assert '@Inject(method = "renderHand"' in hands
assert "PrideScenesClientMod.camera().showingShot()" in hands
for phrase in [
    'resolve("pride_scenes_client").resolve("camera.properties")',
    'camera_enabled=true', 'reduced_motion=false',
    'reducedMotion', 'if (!enabled || reducedMotion) return;',
    'packet.session()', 'projection.hasActor(packet.session())',
    'Story', # placeholder replaced below
]:
    if phrase == 'Story': continue
    assert phrase in controller, phrase
for phrase in [
    "MAX_VIEWER_DISTANCE", "MAX_SCENE_TICKS",
    "RaycastContext.ShapeType.COLLIDER", "HitResult.Type.MISS",
    "getOtherEntities(client.player", "pride_npc_professor_sequoia",
    "return new Pose(blended, blendedYaw, blendedPitch)",
    "public void release()", "public void clear()",
    "client.currentScreen != null",
    "client.world != world", "!client.player.isAlive()",
    "client.player.getPos().squaredDistanceTo(entryPos) > 8 * 8",
]:
    assert phrase in controller, phrase
for forbidden in [
    "setCameraEntity(", "teleport(", "requestTeleport(",
    "sendPosition(", "setPosition(", "setYaw(", "setPitch(",
    "ClientPlayNetworking.send(", "ServerPlayNetworking",
    "openstarterscreen", "starter_chosen", "removeCommandTag(",
]:
    assert forbidden not in controller, forbidden
assert "CAMERA.acceptVisual(context.client(), payload)" in mod
assert "CAMERA.acceptDialogue(payload)" in mod
assert "ClientTickEvents.END_CLIENT_TICK.register(CAMERA::tick)" in mod
assert "CAMERA.clear();" in mod
assert "public Vec3d actorPosition(UUID expectedSession)" in projection
assert "public boolean hasActor(UUID expectedSession)" in projection
print("PASS: Preview6 render-only owner-scoped camera, collision, opt-out, cleanup, no progression")
