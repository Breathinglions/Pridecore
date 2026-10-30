package com.prideregion.core.story;

import java.util.*;

public final class StorySceneAnchorConfigTests {
    private static int checks;
    private static void ok(boolean value) {
        if (!value) throw new AssertionError("Scene anchor invariant " + (++checks));
        checks++;
    }
    private static Properties anchors() {
        Properties p = new Properties();
        p.setProperty("visual_preview_enabled", "true");
        p.setProperty("scene.lab.dimension", "pride:test");
        p.setProperty("scene.lab.entry", "10,75,-10");
        p.setProperty("scene.lab.move", "13,75,-10");
        p.setProperty("scene.lab.face", "13,75,-11");
        return p;
    }
    public static void main(String[] args) {
        var scene = StoryPrivateSceneDirector.SceneId.RIN_LAB_INTRO;
        var p = anchors();
        var config = StorySceneAnchorConfig.parse(p);
        ok(config.count() == 1);
        ok(config.forScene(scene, "pride:test",
            new StorySceneAnchorConfig.Point(10,75,-10)).isPresent());
        ok(config.forScene(scene, "pride:wrong",
            new StorySceneAnchorConfig.Point(10,75,-10)).isEmpty());
        ok(config.forScene(scene, "pride:test",
            new StorySceneAnchorConfig.Point(500,75,-10)).isEmpty());
        // A cleanup packet must remain eligible after walking away from the
        // original staging anchor, so no actor persists until the TTL.
        ok(config.forDimension(scene, "pride:test").isPresent());
        ok(config.forDimension(scene, "pride:wrong").isEmpty());
        // The Lab's final heading may be an explicit Minecraft cardinal
        // direction. North must point toward negative Z, not toward Rin's
        // final walking location. Departure uses an independent exit.
        Properties lab = anchors();
        lab.setProperty("scene.lab.dimension", "multiworld:prideregiontest");
        lab.setProperty("scene.lab.entry", "-53,102,3197");
        lab.setProperty("scene.lab.move", "-57,102,3197");
        lab.setProperty("scene.lab.face", "north");
        lab.setProperty("scene.lab.exit", "-51,102,3197");
        var authored = StorySceneAnchorConfig.parse(lab)
            .forDimension(scene, "multiworld:prideregiontest").orElseThrow();
        ok(authored.face().x() == -57 && authored.face().z() == 3196);
        ok(authored.exit().x() == -51 && authored.exit().y() == 102
            && authored.exit().z() == 3197);
        ok(authored.move().x() == -57);
        lab.remove("scene.lab.exit");
        ok(StorySceneAnchorConfig.parse(lab)
            .forDimension(scene, "multiworld:prideregiontest")
            .orElseThrow().exit().equals(authored.entry()));
        lab.setProperty("scene.lab.exit", "500,102,3197");
        boolean invalidExit = false;
        try { StorySceneAnchorConfig.parse(lab); }
        catch (IllegalArgumentException expected) { invalidExit = true; }
        ok(invalidExit);

        p.setProperty("visual_preview_enabled", "false");
        ok(StorySceneAnchorConfig.parse(p).count() == 0);
        p.setProperty("visual_preview_enabled", "true");
        p.remove("scene.lab.face");
        boolean bad = false;
        try { StorySceneAnchorConfig.parse(p); } catch (IllegalArgumentException expected) { bad = true; }
        ok(bad);
        p.setProperty("scene.lab.face", "13,75,-11");
        p.setProperty("scene.lab.entry", "NaN,75,-10");
        bad = false;
        try { StorySceneAnchorConfig.parse(p); } catch (IllegalArgumentException expected) { bad = true; }
        ok(bad);
        p.setProperty("scene.lab.entry", "10,75,-10");
        p.setProperty("scene.lab.move", "900,75,-10");
        bad = false;
        try { StorySceneAnchorConfig.parse(p); } catch (IllegalArgumentException expected) { bad = true; }
        ok(bad);
        p.setProperty("scene.lab.move", "13,75,-10");
        p.setProperty("scene.lab.dimension", "invalid");
        bad = false;
        try { StorySceneAnchorConfig.parse(p); } catch (IllegalArgumentException expected) { bad = true; }
        ok(bad);
        System.out.println("PASS: " + checks + " private scene world-anchor checks");
    }
}
