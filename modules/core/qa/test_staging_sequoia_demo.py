#!/usr/bin/env python3
"""Staging-only Sequoia demo cannot mutate profile or invoke the starter GUI.
This is a contract regression, not a substitute for in-game testing.
"""
from pathlib import Path

root=Path(__file__).resolve().parents[1]
code=(root/"src/main/java/com/prideregion/core/story/StoryModule.java").read_text()
definition=code.split("private static int testSequoiaOnboarding(",1)[1].split("\n    /**",1)[0]
assert "source.getServer().isOnThread()" in definition
assert "STORY_WORLDS.permits(dimension)" in definition
assert "physicalSequoiaNear(target)" in definition
assert "StorySceneVisualBridge.canRender(target, lab)" in definition
assert "PRIVATE_SCENES.active(target.getUuid()).isPresent()" in definition
assert "PRIVATE_SCENES.begin(target.getUuid(), dimension, lab)" in definition
assert "STAGING_LAB_TEST_SCENES.put(target.getUuid(), started.session())" in definition
for forbidden in ["openOriginalStarter(", "openstarterscreen",
    "resetService(", "StoryPlaythroughReset", "getCommandManager()",
    "player.addCommandTag(", "removeCommandTag(", "pokemonrestart",
    "starter_chosen", "sync(", "PRIVATE_SCENES.beginVerified("]:
    assert forbidden not in definition, f"Demo must not mutate state: {forbidden}"
assert 'literal("test-onboarding")' in code
assert '.requires(source -> source.hasPermissionLevel(4))' in code
assert "StorySceneSubtitleBridge.emit(viewer, cue)" in code
assert "hasCinematicLab(cue.owner())" in code
final=code.split("case OPEN_NATIVE_STARTER -> {",1)[1].split("case RESUME_STORY_DIALOGUE",1)[0]
assert 'STAGING_LAB_TEST_SCENES.remove(cue.owner())' in final
assert "PRIVATE_SCENES.claimContinuation(cue," in final
assert 'completeVerifiedLabScene(server, viewer, cue)' in final
assert "openOriginalStarter(" not in final
assert 'No NPC, starter or story facts changed' in code
print("PASS: OP4 Sequoia test uses real private cinematic and subtitles but never resets or grants a starter")
