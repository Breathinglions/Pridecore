package com.prideregion.core;

import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.regex.Pattern;

/** Immutable server-owned profile facts. External mods' quest/badge data is NOT mirrored here. */
public record ProfileState(UUID playerId, long revision, String storyStage, Set<String> facts) {
    private static final Pattern KEY = Pattern.compile("pride:[a-z0-9_./-]{1,88}");
    public static final int MAX_FACTS = 512;

    public ProfileState {
        if (playerId == null || revision < 0 || !validKey(storyStage) || facts == null
                || facts.size() > MAX_FACTS || facts.stream().anyMatch(f -> !validKey(f)))
            throw new IllegalArgumentException("Invalid Pride Core profile");
        facts = Collections.unmodifiableSet(new TreeSet<>(facts));
    }

    public static boolean validKey(String key) {
        return key != null && key.length() <= 96 && KEY.matcher(key).matches();
    }

    public static ProfileState empty(UUID playerId) {
        return new ProfileState(playerId, 0, "pride:unstarted", Set.of());
    }

    public ProfileState withStage(String stage) {
        if (!validKey(stage)) throw new IllegalArgumentException("Invalid stage");
        return storyStage.equals(stage) ? this : new ProfileState(playerId, revision + 1, stage, facts);
    }

    public ProfileState withFact(String fact, boolean present) {
        if (!validKey(fact)) throw new IllegalArgumentException("Invalid fact");
        var updated = new TreeSet<>(facts);
        boolean changed = present ? updated.add(fact) : updated.remove(fact);
        return changed ? new ProfileState(playerId, revision + 1, storyStage, updated) : this;
    }
}
