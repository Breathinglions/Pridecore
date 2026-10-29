package com.prideregion.core.story;

import com.google.gson.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Strict authored encounter roster, loaded by the future native RCT launcher. */
public final class StoryBattleRosters {
    public record Member(String species, int level) {}
    public record Roster(String id, String actor, int minBadges, List<Member> team,
                         String intro, String win) {}
    private final Map<String, Roster> rosters;
    private StoryBattleRosters(Map<String, Roster> rosters) {
        this.rosters = Map.copyOf(rosters);
    }
    public Optional<Roster> find(String id) { return Optional.ofNullable(rosters.get(id)); }
    public Collection<Roster> all() { return rosters.values(); }

    public static StoryBattleRosters parse(Reader reader) {
        JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
        if (root.get("schema").getAsInt() != 1
                || !"STAGING_ONLY".equals(root.get("mode").getAsString()))
            throw new IllegalArgumentException("Unsupported battle roster schema");
        JsonArray entries = root.getAsJsonArray("encounters");
        if (entries == null || entries.size() != StoryBattleSessionRouter.acceptedTrainers().size())
            throw new IllegalArgumentException("Missing or extra story encounters");
        Map<String, Roster> result = new HashMap<>();
        for (JsonElement element : entries) {
            JsonObject o = element.getAsJsonObject();
            String id = o.get("id").getAsString(), actor = o.get("actor").getAsString();
            if (!StoryBattleSessionRouter.acceptedTrainers().containsKey(id)
                    || !StoryDefinition.validId(actor))
                throw new IllegalArgumentException("Unknown encounter or actor");
            int minBadges = o.get("minBadges").getAsInt();
            if (minBadges < 0 || minBadges > 8) throw new IllegalArgumentException("Invalid badge gate");
            JsonArray team = o.getAsJsonArray("team");
            if (team == null || team.size() == 0 || team.size() > 6)
                throw new IllegalArgumentException("Invalid team size");
            List<Member> members = new ArrayList<>();
            for (JsonElement pokemon : team) {
                JsonObject member = pokemon.getAsJsonObject();
                String species = member.get("species").getAsString();
                int level = member.get("level").getAsInt();
                if (!species.matches("[a-z0-9_-]+") || level < 1 || level > 100)
                    throw new IllegalArgumentException("Invalid Pokémon species or level");
                members.add(new Member(species, level));
            }
            String intro = o.get("intro").getAsString(), win = o.get("win").getAsString();
            if (intro.isBlank() || win.isBlank()) throw new IllegalArgumentException("Missing battle dialogue");
            if (result.putIfAbsent(id, new Roster(id, actor, minBadges,
                    List.copyOf(members), intro, win)) != null)
                throw new IllegalArgumentException("Repeated encounter");
        }
        return new StoryBattleRosters(result);
    }

    public static StoryBattleRosters bundled() throws IOException {
        try (InputStream source = StoryBattleRosters.class.getResourceAsStream("/story/story_battles.json")) {
            if (source == null) throw new IOException("Bundled battle rosters missing");
            try (Reader reader = new InputStreamReader(source, StandardCharsets.UTF_8)) {
                try { return parse(reader); }
                catch (RuntimeException ex) { throw new IOException("Invalid bundled battle rosters", ex); }
            }
        }
    }
}
