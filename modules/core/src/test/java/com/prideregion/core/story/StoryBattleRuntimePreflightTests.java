package com.prideregion.core.story;

/** Standalone CI must refuse missing native mods instead of silently starting. */
public final class StoryBattleRuntimePreflightTests {
    public static void main(String[] args) throws Exception {
        ClassLoader withoutMods = new ClassLoader(null) {};
        if (StoryBattleRuntimePreflight.supportsFaintEvents(withoutMods))
            throw new AssertionError("Missing optional faint event was incorrectly accepted");

        try {
            StoryBattleRuntimePreflight.verify(StoryBattleRuntimePreflightTests.class.getClassLoader());
            throw new AssertionError("Preflight accepted absent RCT/Cobblemon runtime");
        } catch (ClassNotFoundException expected) {
            System.out.println("PASS: native RCT/Cobblemon compatibility preflight fails closed");
        }
    }
}
