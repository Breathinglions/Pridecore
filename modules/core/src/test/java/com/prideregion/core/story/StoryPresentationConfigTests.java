package com.prideregion.core.story;

import java.nio.file.*;

public final class StoryPresentationConfigTests {
    private static void check(boolean b) {
        if (!b) throw new AssertionError("Presentation config regression");
    }
    public static void main(String[] args) throws Exception {
        Path folder = Files.createTempDirectory("pride-presentation-test");
        try {
            StoryPresentationConfig.load(folder);
            var original = StoryPresentationConfig.current();
            check(original.enabled() && original.sounds() && original.particles(20) == 20);
            Path file = folder.resolve("presentation.properties");
            Files.writeString(file, """
                enabled=true
                sounds=false
                particle_scale=0.5
                cue_time_scale=1.5
                sound_volume=0.2
                sound_pitch=1.1
                """);
            StoryPresentationConfig.load(folder);
            var changed = StoryPresentationConfig.current();
            check(!changed.sounds() && changed.particles(20) == 10 && changed.tick(20) == 30);
            Files.writeString(file, "enabled=true\nparticle_scale=200.0\n");
            try {
                StoryPresentationConfig.load(folder);
                throw new AssertionError("Invalid presentation accepted");
            } catch (java.io.IOException expected) {
                check(StoryPresentationConfig.current() == changed);
            }
            Files.writeString(file, "enabled=true\nsounds=true\nparticle_scale=0.0\n");
            StoryPresentationConfig.load(folder);
            check(StoryPresentationConfig.current().particles(100) == 0);
            StoryPresentationConfig.reset();
            check(StoryPresentationConfig.current().particles(10) == 10);
            System.out.println("PASS: strictly cosmetic presentation config");
        } finally {
            try (var stream = Files.list(folder)) {
                stream.forEach(p -> { try { Files.delete(p); }
                    catch (java.io.IOException e) { throw new RuntimeException(e); } });
            }
            Files.delete(folder);
        }
    }
}
