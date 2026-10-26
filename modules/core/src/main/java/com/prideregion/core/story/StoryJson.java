package com.prideregion.core.story;

import com.google.gson.*;
import java.util.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.io.*;

/** Strict bounded loader; constructs a complete validated graph before swapping. */
public final class StoryJson {
    private StoryJson() {}
    public static StoryDefinition load(Path path) throws IOException {
        if (Files.size(path) > 512_000L) throw new IOException("Story JSON too large");
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            return parse(JsonParser.parseReader(reader));
        } catch (RuntimeException ex) {
            throw new IOException("Invalid story JSON: " + ex.getMessage(), ex);
        }
    }

    public static StoryDefinition parse(JsonElement root) {
        JsonObject doc = root.getAsJsonObject();
        if (doc.get("version").getAsInt() != 1) throw new IllegalArgumentException("Story schema version must be 1");
        Map<String, StoryDefinition.Npc> npcs = new LinkedHashMap<>();
        for (JsonElement element : doc.getAsJsonArray("npcs")) {
            JsonObject item = element.getAsJsonObject();
            String id = val(item, "id");
            Map<String, StoryDefinition.Node> nodes = new LinkedHashMap<>();
            for (JsonElement e : item.getAsJsonArray("nodes")) {
                JsonObject node = e.getAsJsonObject();
                String key = val(node, "id");
                List<StoryDefinition.Option> choices = new ArrayList<>();
                for (JsonElement c : node.getAsJsonArray("options")) {
                    JsonObject choice = c.getAsJsonObject();
                    choices.add(new StoryDefinition.Option(val(choice, "id"),
                            val(choice, "text"), optional(choice, "next"), optional(choice, "stage"),
                            ids(choice, "grant"), ids(choice, "revoke"), ids(choice, "requires"),
                            ids(choice, "requiresLegacy")));
                }
                var parsed = new StoryDefinition.Node(key, val(node, "text"), choices);
                if (nodes.putIfAbsent(key, parsed) != null) throw new IllegalArgumentException("Duplicate node " + key);
            }
            var npc = new StoryDefinition.Npc(id, val(item, "name"), ids(item, "requires"),
                    val(item, "completedFact"), val(item, "start"), val(item, "repeat"), nodes);
            if (npcs.putIfAbsent(id, npc) != null) throw new IllegalArgumentException("Duplicate NPC " + id);
        }
        return new StoryDefinition(npcs);
    }

    private static String val(JsonObject obj, String name) {
        return obj.get(name).getAsString();
    }
    private static String optional(JsonObject obj, String name) {
        return !obj.has(name) || obj.get(name).isJsonNull() ? null : obj.get(name).getAsString();
    }
    private static Set<String> ids(JsonObject obj, String name) {
        if (!obj.has(name)) return Set.of();
        Set<String> set = new LinkedHashSet<>();
        for (JsonElement element : obj.getAsJsonArray(name)) {
            if (!set.add(element.getAsString()))
                throw new IllegalArgumentException("Duplicate identifier in " + name);
        }
        return set;
    }
}
