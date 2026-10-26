package com.prideregion.core.story;

import com.google.gson.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
/** End-to-end config migration test with actual bundled JSON. Runs in Gradle CI. */
public final class StoryConfigMigrationTests {
    private static int checks;
    private static void check(boolean passes, String label) {
        checks++;
        if (!passes) throw new AssertionError(label);
    }
    private static InputStream bundled() {
        InputStream input = StoryConfigMigrationTests.class.getResourceAsStream("/story/default.json");
        if (input == null) throw new IllegalStateException("No packaged default story");
        return input;
    }
    private static JsonObject defaultJson() throws Exception {
        try (InputStream in = bundled(); Reader r = new InputStreamReader(in, StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(r).getAsJsonObject();
        }
    }
    private static JsonObject find(JsonArray npcs, String id) {
        for (var x : npcs)
            if (x.getAsJsonObject().get("id").getAsString().equals(id)) return x.getAsJsonObject();
        throw new AssertionError("Missing NPC " + id);
    }
    private static void denied(RunnableWithException runnable) throws Exception {
        checks++;
        try { runnable.run(); throw new AssertionError("Expected refusal"); }
        catch (IOException expected) { }
    }
    @FunctionalInterface private interface RunnableWithException { void run() throws Exception; }
    public static void main(String[] args) throws Exception {
        Path folder = Files.createTempDirectory("pride-migrate-");
        Path file = folder.resolve("story.json");
        var initial = defaultJson();
        // Recreate the real v0.2.0 on-disk state: only its original six NPCs.
        JsonArray oldSix = new JsonArray();
        for (int i = 0; i < 6; i++)
            oldSix.add(initial.getAsJsonArray("npcs").get(i).deepCopy());
        initial.add("npcs", oldSix);
        JsonArray npcs = initial.getAsJsonArray("npcs");
        // Emulate previous 0.2.0 copied config: no requiresLegacy fields.
        for (var npc : npcs) for (var node : npc.getAsJsonObject().getAsJsonArray("nodes"))
            for (var option : node.getAsJsonObject().getAsJsonArray("options"))
                option.getAsJsonObject().remove("requiresLegacy");
        JsonObject mom = find(npcs, "mom");
        mom.addProperty("name", "Custom Mom");
        mom.getAsJsonArray("nodes").get(0).getAsJsonObject()
            .addProperty("text", "Our customized original welcome");
        JsonObject custom = mom.deepCopy();
        custom.addProperty("id", "customguide");
        custom.addProperty("name", "My existing custom Guide");
        custom.addProperty("completedFact", "pride:custom_guide_done");
        npcs.add(custom);
        byte[] oldBytes = new GsonBuilder().setPrettyPrinting().create()
            .toJson(initial).getBytes(StandardCharsets.UTF_8);
        Files.write(file, oldBytes);
        Path props = folder.resolve("story.properties");
        Files.writeString(props, "story_enabled=true\n# custom property kept\n");
        String previousProps = Files.readString(props);

        StoryConfigManager.Result updated;
        try (InputStream data = bundled()) { updated = StoryConfigManager.upgrade(file,data); }
        check(updated.change() == StoryConfigManager.Change.UPGRADED, "Old config upgraded");
        check(updated.backup()!=null && Files.exists(updated.backup()), "Backup written");
        check(Arrays.equals(oldBytes,Files.readAllBytes(updated.backup())),
            "Original config backed up without changes");
        check(updated.newGates()==6, "All six new legacy gates injected");
        JsonObject newDoc = JsonParser.parseString(Files.readString(file)).getAsJsonObject();
        StoryJson.parse(newDoc);
        JsonArray all = newDoc.getAsJsonArray("npcs");
        check(all.size() == 28, "Full campaign and custom NPC retained");
        check(find(all,"customguide").get("name").getAsString().equals("My existing custom Guide"),
            "Custom NPC retained");
        check(find(all,"mom").get("name").getAsString().equals("Custom Mom"),
            "Custom NPC name retained");
        check(find(all,"mom").getAsJsonArray("nodes").get(0).getAsJsonObject()
            .get("text").getAsString().equals("Our customized original welcome"),
            "Custom dialogue retained");
        check(Files.readString(props).equals(previousProps),
            "Story enabled property and custom config preserved");
        byte[] snapshot=Files.readAllBytes(file);
        StoryConfigManager.Result again;
        try (InputStream data = bundled()) { again=StoryConfigManager.upgrade(file,data); }
        check(again.change()==StoryConfigManager.Change.UNCHANGED,
            "Idempotent second server startup");
        check(Arrays.equals(snapshot,Files.readAllBytes(file)),
            "No rewrite on same version");
        check(again.backup()==null, "No duplicate backups");

        Path broken = folder.resolve("broken.json");
        Files.writeString(broken,"{not valid JSON");
        byte[] brokenBytes = Files.readAllBytes(broken);
        denied(() -> { try (InputStream data = bundled()) { StoryConfigManager.upgrade(broken,data); }});
        check(Arrays.equals(brokenBytes,Files.readAllBytes(broken)),
            "Malformed JSON remains untouched");
        Path fresh=folder.resolve("brandnew.json");
        StoryConfigManager.Result created;
        try (InputStream data = bundled()) { created = StoryConfigManager.upgrade(fresh,data); }
        check(created.change()==StoryConfigManager.Change.CREATED,"Missing story created");
        check(created.backup()==null, "New file requires no backup");
        check(StoryJson.load(fresh).npcs().size()==27,"Fresh file has full bundled campaign NPCs");

        Path unusual = folder.resolve("unusual.json");
        var withForeign = defaultJson();
        var customNpc = find(withForeign.getAsJsonArray("npcs"),"sequoia");
        customNpc.getAsJsonArray("nodes").get(0).getAsJsonObject()
            .getAsJsonArray("options").get(0).getAsJsonObject()
            .add("requiresLegacy", JsonParser.parseString("[\"admin\"]"));
        Files.writeString(unusual,withForeign.toString());
        byte[] unusualOriginal=Files.readAllBytes(unusual);
        denied(() -> { try (InputStream data = bundled()) { StoryConfigManager.upgrade(unusual,data); }});
        check(Arrays.equals(unusualOriginal,Files.readAllBytes(unusual)),
            "Unsupported custom prerequisite fails closed");

        System.out.println("PASS: " + checks +
            " story config tests — backup, merge, custom content, idempotence, rejection");
    }
}
