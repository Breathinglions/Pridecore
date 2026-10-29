package com.prideregion.core.story;

import com.google.gson.*;
import java.io.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import static java.nio.file.StandardOpenOption.CREATE_NEW;

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
        // Closed schema intentionally excludes mixed Minecraft boss mechanics.
        requireExactKeys(root, Set.of("schema", "mode", "authority", "combatMode",
            "battleFormat", "encounters"));
        if (root.get("schema").getAsInt() != 1
                || !"STAGING_ONLY".equals(root.get("mode").getAsString())
                || !StoryCombatPolicy.Mode.POKEMON_ONLY.name().equals(
                    root.get("combatMode").getAsString())
                || !"GEN_9_SINGLES".equals(root.get("battleFormat").getAsString()))
            throw new IllegalArgumentException("Story RCT rosters must be Pokémon-only singles");
        JsonArray entries = root.getAsJsonArray("encounters");
        if (entries == null || entries.size() != StoryBattleSessionRouter.acceptedTrainers().size())
            throw new IllegalArgumentException("Missing or extra story encounters");
        Map<String, Roster> result = new HashMap<>();
        for (JsonElement element : entries) {
            JsonObject o = element.getAsJsonObject();
            requireExactKeys(o, Set.of("id", "actor", "chapter", "minBadges",
                "team", "intro", "win"));
            String id = o.get("id").getAsString(), actor = o.get("actor").getAsString();
            StoryCombatPolicy.requirePokemonOnly(id, root.get("combatMode").getAsString());
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
                requireExactKeys(member, Set.of("species", "level"));
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

    private static void requireExactKeys(JsonObject object, Set<String> allowed) {
        if (!object.keySet().equals(allowed))
            throw new IllegalArgumentException(
                "Unknown or missing Pokémon-only story roster fields: " + object.keySet());
    }


    /**
     * The original schema / authority / encounter identity and badge gates
     * remain immutable. Only teams, intro and win narration may be edited.
     * The snapshot is loaded once at startup: never hot-swap active RCT fights.
     */
    private static volatile StoryBattleRosters configured;

    public static StoryBattleRosters active() throws IOException {
        StoryBattleRosters current = configured;
        if (current == null) throw new IOException("Story roster config not initialized");
        return current;
    }

    public static synchronized void loadConfig(Path configDirectory) throws IOException {
        Files.createDirectories(configDirectory);
        Path file = configDirectory.resolve("story_battles.json");
        if (Files.isSymbolicLink(file)) throw new IOException("Refusing symlinked story battle config");
        byte[] base;
        try (InputStream stream = StoryBattleRosters.class.getResourceAsStream(
                "/story/story_battles.json")) {
            if (stream == null) throw new IOException("Missing bundled story battle template");
            base = stream.readAllBytes();
        }
        if (Files.notExists(file)) {
            try { Files.write(file, base, CREATE_NEW); }
            catch (FileAlreadyExistsException ignored) { }
        }
        if (!Files.isRegularFile(file) || Files.isSymbolicLink(file)
                || Files.size(file) > 65536)
            throw new IOException("Unsafe or oversized story battles file");
        byte[] bytes = Files.readAllBytes(file);
        try {
            JsonObject locked = JsonParser.parseString(new String(base, StandardCharsets.UTF_8))
                .getAsJsonObject();
            JsonObject editable = JsonParser.parseString(new String(bytes, StandardCharsets.UTF_8))
                .getAsJsonObject();
            for (String key : List.of("schema", "mode", "authority",
                    "combatMode", "battleFormat")) {
                if (!Objects.equals(locked.get(key), editable.get(key)))
                    throw new IllegalArgumentException("Cannot change story battle authority: " + key);
            }
            JsonArray original = locked.getAsJsonArray("encounters");
            JsonArray revised = editable.getAsJsonArray("encounters");
            if (revised == null || revised.size() != original.size())
                throw new IllegalArgumentException("Story encounter identities changed");
            Map<String, JsonObject> byId = new HashMap<>();
            for (JsonElement entry : revised) {
                JsonObject obj = entry.getAsJsonObject();
                String id = obj.get("id").getAsString();
                if (byId.putIfAbsent(id, obj) != null)
                    throw new IllegalArgumentException("Duplicate story encounter " + id);
            }
            for (JsonElement entry : original) {
                JsonObject originalEncounter = entry.getAsJsonObject();
                String id = originalEncounter.get("id").getAsString();
                JsonObject changed = byId.get(id);
                if (changed == null) throw new IllegalArgumentException("Missing story encounter " + id);
                for (String lockedKey : List.of("id", "actor", "chapter", "minBadges")) {
                    if (!Objects.equals(originalEncounter.get(lockedKey), changed.get(lockedKey)))
                        throw new IllegalArgumentException(
                            "Story encounter identity/badge gate cannot change: " + id + "." + lockedKey);
                }
            }
            StoryBattleRosters checked = parse(
                new InputStreamReader(new ByteArrayInputStream(bytes), StandardCharsets.UTF_8));
            configured = checked;
        } catch (RuntimeException invalid) {
            configured = null; // No mixed/partially updated encounter state.
            throw new IOException("Invalid story_battles.json; story fails closed", invalid);
        }
    }

    public static synchronized void clearConfig() { configured = null; }

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
