package com.prideregion.core.story;

import java.util.Set;

/** Pure-Java staging world marker invariants, independent of the game runtime. */
public final class StoryWorldMarkerCatalogTests {
    private static int checks;
    private static void check(boolean ok) {
        checks++;
        if (!ok) throw new AssertionError("World marker test failed at " + checks);
    }
    private static void invalid(String kind, String id) {
        boolean rejected = false;
        try { StoryWorldMarkerCatalog.marker(kind, id); }
        catch (IllegalArgumentException expected) { rejected = true; }
        check(rejected);
    }
    public static void main(String[] args) {
        for (String id : StoryBattleSessionRouter.acceptedTrainers().keySet()) {
            String tag = StoryWorldMarkerCatalog.marker("battle", id);
            check(tag.equals("pride_battle_" + id));
            check(StoryWorldMarkerCatalog.canBind(Set.of("custom_tag"), tag));
            check(StoryWorldMarkerCatalog.canBind(Set.of(tag), tag));
            check(!StoryWorldMarkerCatalog.canBind(Set.of("pride_battle_other"), tag));
            check(!StoryWorldMarkerCatalog.canBind(Set.of("pride_clue_forest"), tag));
        }
        for (var clue : StoryEvidenceEngine.definitions()) {
            String tag = StoryWorldMarkerCatalog.marker("clue", clue.id());
            check(tag.equals(clue.markerTag()));
            check(StoryWorldMarkerCatalog.canBind(Set.of(), tag));
            check(!StoryWorldMarkerCatalog.canBind(Set.of("pride_finale_core"), tag));
        }
        for (String id : StoryWorldMarkerCatalog.finaleIds()) {
            String tag = StoryWorldMarkerCatalog.marker("finale", id);
            check(tag.equals(id.equals("core") ? "pride_finale_core"
                : "pride_finale_conduit_" + id));
            check(StoryWorldMarkerCatalog.canBind(Set.of(), tag));
        }
        invalid("battle", "fake");
        invalid("clue", "fake");
        invalid("finale", "fake");
        invalid("unknown", "core");
        invalid("battle", "rin_route1 extra");
        check(!StoryWorldMarkerCatalog.isStoryTag("other_mod_tag"));
        check(StoryWorldMarkerCatalog.isStoryTag("pride_finale_typo"));
        check(!StoryWorldMarkerCatalog.canBind(Set.of("pride_finale_typo"),
            StoryWorldMarkerCatalog.marker("finale", "core")));
        System.out.println("PASS: " + checks + " authored world marker invariants");
    }
}
