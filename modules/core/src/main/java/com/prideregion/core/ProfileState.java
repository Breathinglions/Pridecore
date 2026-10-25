package com.prideregion.core;

import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Immutable facts owned by Pride Core only. External League, Cobblemon, FTB or
 * LegendaryQuests achievements MUST NOT be copied in here without a migration.
 */
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
        return new ProfileState(Objects.requireNonNull(playerId), 0, "pride:unstarted", Set.of());
    }

    public ProfileState withStage(String stage) {
        return transition(stage, Set.of(), Set.of());
    }

    public ProfileState withFact(String fact, boolean present) {
        return transition(null, present ? Set.of(fact) : Set.of(), present ? Set.of() : Set.of(fact));
    }

    /**
     * Apply multiple story/fact changes in one single revision.
     * null stage means "unchanged". No-op transitions never advance revision.
     */
    public ProfileState transition(String nextStage, Collection<String> add, Collection<String> remove) {
        Objects.requireNonNull(add, "add");
        Objects.requireNonNull(remove, "remove");
        String stage = nextStage == null ? storyStage : nextStage;
        if (!validKey(stage) || add.stream().anyMatch(k -> !validKey(k))
                || remove.stream().anyMatch(k -> !validKey(k)))
            throw new IllegalArgumentException("Invalid Pride Core progression ID");
        TreeSet<String> grants = new TreeSet<>(add);
        TreeSet<String> revokes = new TreeSet<>(remove);
        if (!Collections.disjoint(grants, revokes))
            throw new IllegalArgumentException("Cannot grant and revoke same fact");
        TreeSet<String> updated = new TreeSet<>(facts);
        updated.removeAll(revokes);
        updated.addAll(grants);
        if (updated.size() > MAX_FACTS) throw new IllegalArgumentException("Fact limit exceeded");
        if (storyStage.equals(stage) && updated.equals(facts)) return this;
        if (revision == Long.MAX_VALUE) throw new IllegalStateException("Revision exhausted");
        return new ProfileState(playerId, revision + 1, stage, updated);
    }
}
