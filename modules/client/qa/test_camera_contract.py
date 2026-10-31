#!/usr/bin/env python3
"""Compile-independent fail-closed contract for Preview11's client-only camera."""
from pathlib import Path
import json

root=Path(__file__).resolve().parents[1]
src=root/"src/main/java/com/prideregion/scenes/client"
controller=(src/"RinCinematicCamera.java").read_text()
settings=(src/"RinCameraSettings.java").read_text()
camera=(src/"mixin/CameraMixin.java").read_text()
hands=(src/"mixin/GameRendererMixin.java").read_text()
hud=(src/"mixin/CinematicHudMixin.java").read_text()
held=(src/"mixin/HeldItemRendererMixin.java").read_text()
chat=(src/"mixin/ChatHudMixin.java").read_text()
overlay=(src/"RinCinematicDialogueOverlay.java").read_text()
mod=(src/"PrideScenesClientMod.java").read_text()
projection=(src/"RinPreviewProjection.java").read_text()
fabric=json.loads((root/"src/main/resources/fabric.mod.json").read_text())
mixins=json.loads((root/"src/main/resources/pride-scenes-client.mixins.json").read_text())

assert fabric["environment"] == "client"
assert fabric["mixins"] == ["pride-scenes-client.mixins.json"]
assert mixins["required"] is True
assert mixins["client"] == ["CameraMixin", "GameRendererMixin", "CinematicHudMixin", "HeldItemRendererMixin", "ChatHudMixin"]
assert mixins["injectors"]["defaultRequire"] == 1
assert "@Mixin(Camera.class)" in camera
assert 'method = "update"' in camera
assert 'at = @At("TAIL")' in camera
assert "PrideScenesClientMod.camera().renderPose(" in camera
assert "setPos(pose.position().x, pose.position().y, pose.position().z)" in camera
assert "setRotation(pose.yaw(), pose.pitch())" in camera
assert '@Inject(method = "renderHand"' in hands
assert "PrideScenesClientMod.cinematicActive(MinecraftClient.getInstance())" in hands
assert "@Mixin(InGameHud.class)" in hud
assert '@Inject(method = "render", at = @At("HEAD"), cancellable = true)' in hud
assert "PrideScenesClientMod.cinematicActive(client)" in hud
assert "PrideScenesClientMod.renderCinematicDialogue(client, context)" in hud
assert "ci.cancel();" in hud
assert "DIALOGUE.draw(client, context, true)" in mod
assert "DIALOGUE_TICKS = 112" in overlay
presentation=(src/"CinematicHudPresentation.java").read_text()
assert 'resolve("pride_scenes_client").resolve("hud.properties")' in presentation
assert 'defaults.put("letterbox_bars", "true")' in presentation
assert 'defaults.put("dialogue_position", "bottom")' in presentation
assert 'HUD.drawBars(context)' in mod
assert 'ClientTickEvents.END_CLIENT_TICK.register(HUD::tick)' in mod
assert 'if (rinPortrait)' in overlay
assert 'drawPortrait(ctx, portraitX, portraitY)' in overlay
assert 'drawCenteredTextWithShadow(font,' not in overlay
assert 'PrideScenesClientMod.hudPresentation().settings()' in overlay
assert "client.options.hudHidden && !cinematic" in overlay
assert "MAX_SCENE_TICKS = 400" in controller
director=(src/"ClientCameraDirector.java").read_text()
packet=(src/"ClientCameraDirectorPayload.java").read_text()
assert 'Identifier.of("pridecore", "camera_director_v1")' in packet
assert 'DIRECTOR.accept(context.client(), payload)' in mod
assert 'ClientTickEvents.END_CLIENT_TICK.register(DIRECTOR::tick)' in mod
assert 'PrideScenesClientMod.directorPose(' in camera
assert 'DIRECTOR.active(client) || CAMERA.suppressHud(client)' in mod
assert 'cameraPositionClear' in controller or 'cameraSpaceClear' in controller
assert 'segmentClear(client, currentFrame.pos(), point)' in director
assert 'JsonParser.parseString(packet.sceneJson())' in director
assert 'client.options.hudHidden = originalHudHidden' in director
assert "LAB_CAMERA_BLOCK = new BlockPos(-54, 102, 3195)" in controller
assert "LAB_CAMERA = new Vec3d(-53.5, 103.62, 3195.5)" in controller
assert "LAB_YAW_WEST = 90.0F" in controller
assert "labFixedShot = labSettings.labEnabled()" in controller
assert "labSettings = RinCameraSettings.load()" in controller
assert "labSettings.followEnabled()" in controller
assert "return new Pose(position, yaw, MathHelper.clamp(pitch, -70.0F, 70.0F))" in controller
assert "tickLabTracking(client, rin)" in controller
assert "followCandidate(client, rin)" in controller
assert "settleFromPos.lerp(labCameraPosition, t)" in controller
assert "segmentClear(client, currentPos, next)" in controller
assert "labFinalLook()" in controller
assert "Rin's position" in controller
assert "if (releaseTicks >= 0 || labCameraPosition == null) return null;" in controller
assert "findSafeLabCamera(client)" in controller
assert "cameraSpaceClear(client, candidate)" in controller
assert "getCollisionShape(client.world, block).isEmpty()" in controller
assert "cameraPositionClear(client, point)" in controller
assert "point, ahead" in controller
assert "labCameraPosition = null" in controller
assert "vanilla.getPos().lerp(LAB_CAMERA, weight)" not in controller
assert 'resolve("pride_scenes_client").resolve("camera.properties")' in settings
assert '"lab_follow_enabled", "true"' in settings
assert '"lab_follow_ticks", "88"' in settings
assert '"lab_settle_ticks", "26"' in settings
assert '"lab_camera_y", "102"' in settings
assert 'StandardOpenOption.APPEND' in settings
assert 'Double.isFinite(n)' in settings
assert 'Files.newInputStream(path)' in settings
assert "originalHudHidden = client.options.hudHidden" in controller
assert "client.options.hudHidden = true" in controller
assert "client.options.hudHidden = originalHudHidden" in controller
assert "@Mixin(HeldItemRenderer.class)" in held
assert "renderItem(FLnet/minecraft/client/util/math/MatrixStack;" in held
assert "@Mixin(ChatHud.class)" in chat
assert "pride$hideChatDuringScene" in chat
assert "ci.cancel();" in held and "ci.cancel();" in chat
assert "professor.getRotationVec(1.0F)" in controller
assert "professorFrontShot(client, professor, focus, playerEye)" in controller
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
# The client and server must agree on the longer Lab sequence; otherwise
# the private actor exits and the native starter UI opens too early.
director=(root.parent/"core/src/main/java/com/prideregion/core/story/StoryPrivateSceneDirector.java").read_text()
assert "MAX_TICKS = 390" in director
assert 'new Cue(290, Kind.ACTOR_MOVE, "Rin", "", "lab_exit")' in director
assert 'new Cue(332, Kind.OPEN_NATIVE_STARTER' in director
print("PASS: Preview11 scene Director, client-owned safe camera, legacy follow-Rin, HUD")
