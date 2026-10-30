#!/usr/bin/env python3
"""Compare the two isolated Fabric 1.21.1 scene-wire implementations.

This is a static compatibility guard, NOT an in-game networking test.
"""
import pathlib
import re

ROOT = pathlib.Path(__file__).resolve().parents[3]
SERVER = ROOT / "modules/core/src/main/java/com/prideregion/core/story/StorySceneVisualPayload.java"
CLIENT = ROOT / "modules/client/src/main/java/com/prideregion/scenes/client/ClientSceneVisualPayload.java"
SENDER = ROOT / "modules/core/src/main/java/com/prideregion/core/story/StorySceneVisualBridge.java"
RECEIVER = ROOT / "modules/client/src/main/java/com/prideregion/scenes/client/PrideScenesClientMod.java"
PROJECTION = ROOT / "modules/client/src/main/java/com/prideregion/scenes/client/RinPreviewProjection.java"

server, client = SERVER.read_text(), CLIENT.read_text()
def actions(content):
    m = re.search(r"enum Action\s*\{([^{}]+)\}", content)
    assert m, "Missing wire action enum"
    return tuple(s.strip() for s in m.group(1).split(","))
assert actions(server) == actions(client) == ("ENTER", "MOVE", "FACE", "REMOVE")
for content in (server, client):
    assert 'Identifier.of("pridecore", "scene_visual_v1")' in content
    write = content.split("private void write(RegistryByteBuf buf)",1)[1].split("}",1)[0]
    assert tuple(re.findall(r"buf\.write(\w+)\(", write)) == ("Uuid","Byte","Double","Double","Double")
    read = content.split("private "+("StorySceneVisualPayload" if content == server else "ClientSceneVisualPayload")+"(RegistryByteBuf buf)",1)[1].split("}",1)[0]
    assert tuple(re.findall(r"buf\.read(\w+)\(", read)) == ("Uuid","UnsignedByte","Double","Double","Double")
assert "ServerPlayNetworking.canSend(viewer, StorySceneVisualPayload.ID)" in SENDER.read_text()
assert "ServerPlayNetworking.send(viewer," in SENDER.read_text()
receiver = RECEIVER.read_text()
assert "ClientPlayNetworking.registerGlobalReceiver(" in receiver
assert "PayloadTypeRegistry.playS2C().register(" in receiver
assert "ClientPlayNetworking.send(" not in receiver
projection = PROJECTION.read_text()
assert "current.addEntity(created)" in projection and "world.removeEntity(" in projection
assert "MAX_SILENCE_TICKS" in projection and "client.world != world" in projection
# A client-only actor must drive its own gait when moving; changing
# position alone renders a sliding villager with frozen legs. Its head,
# body and root should agree, and FACE must wait until walking has ended.
assert "actor.limbAnimator.updateLimbs(" in projection
assert "horizontal * 4.0D" in projection
assert "actor.setBodyYaw(smooth)" in projection
assert "actor.setHeadYaw(smooth)" in projection
assert "MathHelper.wrapDegrees(" in projection
assert "if (faceTarget != null)" in projection
assert "if (destination != null && steps > 0)" in projection
assert projection.index("if (destination != null && steps > 0)") < projection.index("if (faceTarget != null)")
assert "faceTarget = null;" in projection
for source in (receiver, projection):
    assert "ServerPlayNetworking.send" not in source
print("PASS: private scene protocol, channel, receiver, lifecycle and no-C2S boundaries")
