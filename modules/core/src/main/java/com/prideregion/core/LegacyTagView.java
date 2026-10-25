package com.prideregion.core;

import java.util.Collection;
import java.util.List;

/** Read-only inspection of legacy entity command tags; intentionally no automatic import. */
public final class LegacyTagView {
    private LegacyTagView() {}
    public static List<String> progressionTags(Collection<String> commandTags) {
        return commandTags.stream().filter(t -> t.startsWith("league_")
                || t.startsWith("pride_"))
                .sorted().distinct().toList();
    }
}
