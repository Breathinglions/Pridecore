package com.prideregion.core;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

/** Read-only inspection of legacy player command tags; never writes or imports. */
public final class LegacyTagView {
    private LegacyTagView() {}

    public static List<String> progressionTags(Collection<String> commandTags) {
        Objects.requireNonNull(commandTags, "commandTags");
        return commandTags.stream()
            .filter(Objects::nonNull)
            .filter(t -> t.startsWith("league_") || t.startsWith("pride_"))
            .sorted().distinct().toList();
    }
}
