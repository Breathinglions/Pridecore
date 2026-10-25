package com.prideregion.core;

import java.util.List;
import java.util.Objects;

/** Distinguish Pride-owned facts from legacy external command tags in every query. */
public record ProgressionView(ProfileState owned, List<String> legacyTags) {
    public ProgressionView {
        Objects.requireNonNull(owned, "owned");
        legacyTags = List.copyOf(Objects.requireNonNull(legacyTags, "legacyTags"));
    }

    public boolean hasOwnedFact(String id) { return owned.facts().contains(id); }

    public boolean hasLegacyTag(String tag) { return legacyTags.contains(tag); }

    public String provenance(String id) {
        if (hasOwnedFact(id)) return "pride-owned";
        if (hasLegacyTag(id)) return "legacy-read-only";
        return "absent";
    }
}
