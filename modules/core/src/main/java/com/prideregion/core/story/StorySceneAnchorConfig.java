package com.prideregion.core.story;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.util.*;

/**
 * Builder-authored coordinates for temporary scene projections. No location
 * is guessed. An absent/disabled config means no client visual packets.
 */
public final class StorySceneAnchorConfig {
    public record Point(double x, double y, double z) {
        public Point {
            if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)
                    || Math.abs(x) > 29_999_984 || Math.abs(z) > 29_999_984
                    || y < -64 || y > 1024)
                throw new IllegalArgumentException("Unsafe scene coordinate");
        }
        public double distanceSquared(Point other) {
            double dx = x - other.x, dy = y - other.y, dz = z - other.z;
            return dx * dx + dy * dy + dz * dz;
        }
    }
    public record Layout(String dimension, Point entry, Point move, Point face) {
        public Layout {
            Objects.requireNonNull(dimension);
            Objects.requireNonNull(entry);
            Objects.requireNonNull(move);
            Objects.requireNonNull(face);
            if (!dimension.matches("[a-z0-9_.-]+:[a-z0-9_./-]+")
                    || entry.distanceSquared(move) > 64.0D * 64.0D
                    || entry.distanceSquared(face) > 64.0D * 64.0D)
                throw new IllegalArgumentException("Invalid scene layout");
        }
    }

    private static final Map<StoryPrivateSceneDirector.SceneId, String> NAMES = Map.of(
        StoryPrivateSceneDirector.SceneId.RIN_LAB_INTRO, "lab",
        StoryPrivateSceneDirector.SceneId.RIN_CANYON_REUNION, "canyon",
        StoryPrivateSceneDirector.SceneId.RIN_SUMMIT_REUNION, "summit"
    );
    private final Map<StoryPrivateSceneDirector.SceneId, Layout> layouts;

    private StorySceneAnchorConfig(Map<StoryPrivateSceneDirector.SceneId, Layout> layouts) {
        this.layouts = Map.copyOf(layouts);
    }
    public static StorySceneAnchorConfig disabled() {
        return new StorySceneAnchorConfig(Map.of());
    }
    public static StorySceneAnchorConfig load(Path path) throws IOException {
        Objects.requireNonNull(path);
        if (Files.notExists(path)) return disabled();
        if (Files.isSymbolicLink(path) || !Files.isRegularFile(path) || Files.size(path) > 8192)
            throw new IOException("Unsafe or oversized scene anchors file");
        Properties p = new Properties();
        try (InputStream in = Files.newInputStream(path)) { p.load(in); }
        try {
            return parse(p);
        } catch (IllegalArgumentException ex) {
            throw new IOException("Invalid scene anchor layout; visuals disabled", ex);
        }
    }
    public static StorySceneAnchorConfig parse(Properties p) {
        Objects.requireNonNull(p);
        if (!"true".equals(p.getProperty("visual_preview_enabled", "false").trim()))
            return disabled();
        Map<StoryPrivateSceneDirector.SceneId, Layout> found = new EnumMap<>(
            StoryPrivateSceneDirector.SceneId.class);
        for (var entry : NAMES.entrySet()) {
            String prefix = "scene." + entry.getValue() + ".";
            String dimension = p.getProperty(prefix + "dimension", "").trim();
            String start = p.getProperty(prefix + "entry", "").trim();
            String move = p.getProperty(prefix + "move", "").trim();
            String face = p.getProperty(prefix + "face", "").trim();
            if (dimension.isEmpty() && start.isEmpty() && move.isEmpty() && face.isEmpty())
                continue;
            if (dimension.isEmpty() || start.isEmpty() || move.isEmpty() || face.isEmpty())
                throw new IllegalArgumentException("Incomplete scene: " + entry.getValue());
            found.put(entry.getKey(), new Layout(dimension, point(start), point(move), point(face)));
        }
        if (found.isEmpty())
            throw new IllegalArgumentException("Visual mode requires at least one complete layout");
        return new StorySceneAnchorConfig(found);
    }
    private static Point point(String text) {
        String[] parts = text.split(",", -1);
        if (parts.length != 3) throw new IllegalArgumentException("Expected x,y,z");
        return new Point(Double.parseDouble(parts[0].trim()),
            Double.parseDouble(parts[1].trim()), Double.parseDouble(parts[2].trim()));
    }
    public Optional<Layout> forScene(StoryPrivateSceneDirector.SceneId scene,
                                     String worldDimension, Point viewer) {
        Layout layout = layouts.get(scene);
        if (layout == null || !layout.dimension().equals(worldDimension)
                || viewer.distanceSquared(layout.entry()) > 96.0D * 96.0D)
            return Optional.empty();
        return Optional.of(layout);
    }
    public int count() { return layouts.size(); }
}
