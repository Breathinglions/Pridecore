package com.prideregion.core.story;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import static java.nio.file.StandardOpenOption.CREATE_NEW;

/**
 * Strict optional cosmetics-only tuning. No Pokémon battle mechanics,
 * encounter requirements, starter authorization or rewards can be changed.
 */
public record StoryPresentationConfig(boolean enabled, boolean sounds,
        double particleScale, double timeScale, float soundVolume, float soundPitch) {
    private static volatile StoryPresentationConfig current = defaults();

    public StoryPresentationConfig {
        if (!Double.isFinite(particleScale) || particleScale < 0 || particleScale > 2
                || !Double.isFinite(timeScale) || timeScale < 0.5 || timeScale > 2.0
                || !Float.isFinite(soundVolume) || soundVolume < 0 || soundVolume > 1
                || !Float.isFinite(soundPitch) || soundPitch < 0.5 || soundPitch > 2)
            throw new IllegalArgumentException("Invalid cosmetic scene settings");
    }
    public static StoryPresentationConfig defaults() {
        return new StoryPresentationConfig(true, true, 1.0, 1.0, 0.55f, 0.85f);
    }
    public static StoryPresentationConfig current() { return current; }
    public static void reset() { current = defaults(); }

    public static synchronized void load(Path dir) throws IOException {
        Files.createDirectories(dir);
        Path file = dir.resolve("presentation.properties");
        if (Files.isSymbolicLink(file)) throw new IOException("Symlinked presentation config");
        if (Files.notExists(file)) {
            String example = """
                # Changes apply after a staging server restart.
                # Cosmetic only: cannot change Pokémon battles, quests or rewards.
                enabled=true
                sounds=true
                particle_scale=1.0
                cue_time_scale=1.0
                sound_volume=0.55
                sound_pitch=0.85
                """;
            try { Files.writeString(file, example, CREATE_NEW); }
            catch (FileAlreadyExistsException ignored) { }
        }
        if (!Files.isRegularFile(file) || Files.isSymbolicLink(file)
                || Files.size(file) > 4096) throw new IOException("Unsafe presentation properties");
        Properties p = new Properties();
        try (InputStream input = Files.newInputStream(file)) { p.load(input); }
        Set<String> allowed = Set.of("enabled", "sounds", "particle_scale",
            "cue_time_scale", "sound_volume", "sound_pitch");
        for (String key : p.stringPropertyNames()) {
            if (!allowed.contains(key))
                throw new IOException("Unknown cosmetic setting: " + key);
        }
        try {
            StoryPresentationConfig next = new StoryPresentationConfig(
                strictBoolean(p, "enabled", true),
                strictBoolean(p, "sounds", true),
                Double.parseDouble(p.getProperty("particle_scale", "1.0")),
                Double.parseDouble(p.getProperty("cue_time_scale", "1.0")),
                Float.parseFloat(p.getProperty("sound_volume", "0.55")),
                Float.parseFloat(p.getProperty("sound_pitch", "0.85")));
            current = next; // atomic snapshot only after full validation
        } catch (RuntimeException invalid) {
            throw new IOException("Invalid presentation settings", invalid);
        }
    }

    private static boolean strictBoolean(Properties p, String key, boolean fallback) {
        String raw = p.getProperty(key, String.valueOf(fallback));
        if ("true".equalsIgnoreCase(raw)) return true;
        if ("false".equalsIgnoreCase(raw)) return false;
        throw new IllegalArgumentException("Expected boolean for " + key);
    }
    public int particles(int original) {
        return enabled ? Math.max(0, Math.min(256, (int)Math.round(original * particleScale))) : 0;
    }
    public int tick(int original) {
        return Math.max(0, Math.min(200, (int)Math.round(original * timeScale)));
    }
}
