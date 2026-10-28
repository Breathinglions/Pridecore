package com.prideregion.core.story;

import com.prideregion.core.ProgressionService;
import com.prideregion.core.ProfileState;
import java.io.IOException;
import java.util.*;

/**
 * Module 02 per-player environmental evidence. Players interact with a
 * deliberately placed `minecraft:interaction` entity bearing exactly one
 * `pride_clue_<id>` command tag. The native server verifies the entity,
 * distance, current prerequisite and League marker before calling complete.
 *
 * The proof is the authenticated interaction; clients cannot supply arbitrary
 * completion identifiers as commands. This service never pays, spawns battles,
 * grants League tags, or auto-completes any FTB quest.
 */
public final class StoryEvidenceEngine {
    public record Clue(String id, String title, String requiresFact,
                       String requiresLeague, String earnedFact) {
        public Clue {
            if (!StoryDefinition.validId(id) || !ProfileState.validKey(requiresFact)
                    || !ProfileState.validKey(earnedFact)
                    || (requiresLeague != null && !LegacyOnboardingGate.allowed(requiresLeague)))
                throw new IllegalArgumentException("Invalid clue definition");
        }
        public String markerTag() { return "pride_clue_" + id; }
    }
    public record Result(Clue clue, boolean firstDiscovery) {}
    private static final List<Clue> CLUES = List.of(
        new Clue("forest", "Resonance traces in Verdant Forest",
            "pride:forest_evidence", "league_grass", "pride:clue_forest"),
        new Clue("mine", "Missing survey page at the Rock Mine",
            "pride:mine_records", "league_rock", "pride:clue_mine"),
        new Clue("foundry", "Residual heat from the Foundry",
            "pride:heat_anomaly", "league_fire", "pride:clue_foundry"),
        new Clue("river", "Unnatural pulse beside the waterway",
            "pride:water_evidence", "league_water", "pride:clue_river"),
        new Clue("grid", "Hidden circuit in the electric power grid",
            "pride:engine_plan", "league_electric", "pride:clue_grid"),
        new Clue("ward", "Ancient Resonance ward",
            "pride:ancient_ward", "league_dragon", "pride:clue_ward"),
        new Clue("ice", "Concealed Meridian warning archive",
            "pride:frozen_warning", "league_ice", "pride:clue_ice"),
        new Clue("echo", "Memory echo from the abandoned laboratory",
            "pride:memory_truth", "league_ghost", "pride:clue_echo"),
        new Clue("engine", "Engine stabilizer diagnostics",
            "pride:cael_confronted", null, "pride:clue_engine")
    );
    private static final Map<String, Clue> BY_ID;
    static {
        Map<String, Clue> byId = new LinkedHashMap<>();
        for (Clue clue : CLUES)
            if (byId.putIfAbsent(clue.id(), clue) != null)
                throw new IllegalStateException("Duplicate story clue " + clue.id());
        BY_ID = Map.copyOf(byId);
    }

    private final ProgressionService progression;
    private final java.util.function.Function<UUID, Set<String>> externalTags;

    public StoryEvidenceEngine(ProgressionService progression,
                               java.util.function.Function<UUID, Set<String>> externalTags) {
        this.progression = Objects.requireNonNull(progression);
        this.externalTags = Objects.requireNonNull(externalTags);
    }
    public static List<Clue> definitions() { return CLUES; }
    public static Clue definition(String id) { return BY_ID.get(id); }

    public synchronized Result complete(UUID playerId, String id) throws IOException {
        Clue clue = BY_ID.get(id);
        if (clue == null) throw new IllegalArgumentException("Unknown evidence marker");
        ProfileState player = progression.get(playerId);
        if (player.facts().contains(clue.earnedFact())) return new Result(clue, false);
        if (!player.facts().contains(clue.requiresFact()))
            throw new IllegalStateException("Investigate the preceding story chapter first");
        if (clue.requiresLeague() != null && !LegacyOnboardingGate.filter(
                Objects.requireNonNull(externalTags.apply(playerId))).contains(clue.requiresLeague()))
            throw new IllegalStateException("Earn the corresponding official League badge first");
        // Do not change stage: the current objective must never regress when
        // a player discovers a clue late or alongside another player.
        return new Result(clue, progression.grant(playerId, clue.earnedFact()).changed());
    }

    public static Set<String> discovered(ProfileState profile) {
        Set<String> found = new HashSet<>();
        for (Clue clue : CLUES)
            if (profile.facts().contains(clue.earnedFact())) found.add(clue.id());
        return Set.copyOf(found);
    }
}
