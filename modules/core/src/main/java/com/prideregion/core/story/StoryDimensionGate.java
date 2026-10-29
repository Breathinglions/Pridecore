package com.prideregion.core.story;

import java.util.*;
import java.util.regex.Pattern;

/**
 * Optional staging dimension allowlist. Empty setting preserves existing
 * behavior; non-empty settings restrict physical story interactions to named
 * dimension registry IDs without modifying unrelated worlds.
 */
public final class StoryDimensionGate {
    private static final Pattern ID =
        Pattern.compile("[a-z0-9_.-]+:[a-z0-9_./-]+");
    private final Set<String> allowed;

    private StoryDimensionGate(Set<String> allowed) { this.allowed = allowed; }
    public static StoryDimensionGate open() {
        return new StoryDimensionGate(Set.of());
    }
    public static StoryDimensionGate parse(String config) {
        if (config == null || config.isBlank()) return open();
        String[] entries = config.split(",", -1);
        if (entries.length > 16)
            throw new IllegalArgumentException("Too many story dimensions");
        Set<String> ids = new LinkedHashSet<>();
        for (String entry : entries) {
            String id = entry.trim();
            if (!ID.matcher(id).matches() || !ids.add(id))
                throw new IllegalArgumentException("Invalid or duplicate story dimension: " + id);
        }
        return new StoryDimensionGate(Set.copyOf(ids));
    }
    public boolean permits(String dimensionId) {
        return dimensionId != null && (allowed.isEmpty() || allowed.contains(dimensionId));
    }
    public boolean restricted() { return !allowed.isEmpty(); }
    public Set<String> allowed() { return allowed; }
}
