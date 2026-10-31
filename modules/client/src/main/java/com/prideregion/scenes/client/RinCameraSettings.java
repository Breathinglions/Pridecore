package com.prideregion.scenes.client;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.util.math.MathHelper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Editable CLIENT-side staging camera authoring, reloaded for each scene.
 * The server still owns the story timeline and native starter handoff.
 * Malformed values use safe defaults; authored positions cannot teleport players.
 */
public record RinCameraSettings(
    boolean labEnabled, double cameraX, double cameraY, double cameraZ,
    double eyeHeight, float yaw, float pitch, boolean followEnabled,
    int followTicks, int settleTicks, double followOffsetX,
    double followOffsetY, double followOffsetZ, double followSmooth
) {
    private static final org.slf4j.Logger LOG =
        org.slf4j.LoggerFactory.getLogger("PrideScenesCamera");

    private static final LinkedHashMap<String, String> DEFAULTS = new LinkedHashMap<>();
    static {
        DEFAULTS.put("camera_enabled", "true");
        DEFAULTS.put("reduced_motion", "false");
        DEFAULTS.put("lab_enabled", "true");
        DEFAULTS.put("lab_camera_x", "-54");
        DEFAULTS.put("lab_camera_y", "102");
        DEFAULTS.put("lab_camera_z", "3195");
        DEFAULTS.put("lab_eye_height", "1.62");
        DEFAULTS.put("lab_yaw", "90");
        DEFAULTS.put("lab_pitch", "0");
        DEFAULTS.put("lab_follow_enabled", "true");
        DEFAULTS.put("lab_follow_ticks", "88");
        DEFAULTS.put("lab_settle_ticks", "26");
        DEFAULTS.put("lab_follow_offset_x", "1.5");
        DEFAULTS.put("lab_follow_offset_y", "1.6");
        DEFAULTS.put("lab_follow_offset_z", "2.0");
        DEFAULTS.put("lab_follow_smoothing", "0.20");
    }

    public static Path file() {
        return FabricLoader.getInstance().getConfigDir()
            .resolve("pride_scenes_client").resolve("camera.properties");
    }

    public static RinCameraSettings load() {
        Properties p = new Properties();
        Path path = file();
        try {
            Files.createDirectories(path.getParent());
            if (!Files.exists(path)) {
                StringBuilder config = new StringBuilder(
                    "# Pride Scenes CLIENT camera configuration (Fabric 1.21.1)\n"
                    + "# Edit and replay the cutscene; settings reload each scene.\n"
                    + "# LAB camera x/y/z are STANDING BLOCK coordinates, not eye coordinates.\n"
                    + "# yaw: 90=west, -90=east, 0=south, 180=north.\n"
                    + "# follow offsets are relative to Rin's actor coordinates.\n"
                    + "# If follow path is obstructed, the camera holds or uses the safe fixed shot.\n");
                for (Map.Entry<String, String> e : DEFAULTS.entrySet())
                    config.append(e.getKey()).append('=').append(e.getValue()).append('\n');
                Files.writeString(path, config.toString());
            } else {
                try (var in = Files.newInputStream(path)) { p.load(in); }
                StringBuilder missing = new StringBuilder();
                for (Map.Entry<String, String> e : DEFAULTS.entrySet())
                    if (!p.containsKey(e.getKey()))
                        missing.append(e.getKey()).append('=').append(e.getValue()).append('\n');
                if (!missing.isEmpty())
                    Files.writeString(path,
                        "\n# Added by Pride Scenes Preview 10; edit as needed.\n" + missing,
                        StandardOpenOption.APPEND);
            }
            // Re-read after writing/appending, so every setting takes effect.
            try (var in = Files.newInputStream(path)) { p.load(in); }
        } catch (IOException ex) {
            LOG.warn("Could not load client camera.properties; using safe defaults", ex);
        }
        return new RinCameraSettings(
            Boolean.parseBoolean(read(p, "lab_enabled")),
            number(p, "lab_camera_x", -54, -29_000_000, 29_000_000),
            number(p, "lab_camera_y", 102, -64, 320),
            number(p, "lab_camera_z", 3195, -29_000_000, 29_000_000),
            number(p, "lab_eye_height", 1.62, 0.6, 2.4),
            (float) number(p, "lab_yaw", 90, -180, 180),
            (float) number(p, "lab_pitch", 0, -45, 45),
            Boolean.parseBoolean(read(p, "lab_follow_enabled")),
            (int) number(p, "lab_follow_ticks", 88, 12, 150),
            (int) number(p, "lab_settle_ticks", 26, 1, 70),
            number(p, "lab_follow_offset_x", 1.5, -6, 6),
            number(p, "lab_follow_offset_y", 1.6, 0.8, 2.6),
            number(p, "lab_follow_offset_z", 2.0, -6, 6),
            number(p, "lab_follow_smoothing", 0.20, 0.05, 0.7)
        );
    }

    private static String read(Properties p, String key) {
        return p.getProperty(key, DEFAULTS.get(key)).trim();
    }

    private static double number(Properties p, String key,
                                 double fallback, double min, double max) {
        try {
            double n = Double.parseDouble(read(p, key));
            return Double.isFinite(n) ? MathHelper.clamp(n, min, max) : fallback;
        } catch (NumberFormatException ignored) {
            LOG.warn("Invalid camera setting {}, using {}", key, fallback);
            return fallback;
        }
    }
}
