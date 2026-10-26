package com.prideregion.core.story;

import com.google.gson.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.channels.FileChannel;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import static java.nio.file.StandardOpenOption.*;

/**
 * Updates bundled story content on install without overwriting customized nodes,
 * options or text. Unknown content is left intact. Safe additive changes only.
 *
 * Each migrated file is validated then backed up before an atomic replace.
 * This operates on config/pride_core/story.json, never on player profiles.
 */
public final class StoryConfigManager {
    private static final int MAX_BYTES = 512_000;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting()
            .disableHtmlEscaping().create();

    public enum Change { CREATED, UPGRADED, UNCHANGED }
    public record Result(Change change, Path backup, int newNpcs, int newGates) {}

    private StoryConfigManager() {}

    public static Result upgrade(Path file, InputStream bundled) throws IOException {
        Objects.requireNonNull(file);
        Objects.requireNonNull(bundled);
        byte[] defaults = bundled.readNBytes(MAX_BYTES + 1);
        if (defaults.length > MAX_BYTES) throw new IOException("Bundled story too large");
        final JsonObject template;
        try {
            template = JsonParser.parseString(new String(defaults, StandardCharsets.UTF_8))
                    .getAsJsonObject();
            StoryJson.parse(template);
        } catch (RuntimeException ex) {
            throw new IOException("Bundled story fails validation", ex);
        }

        Path folder = file.toAbsolutePath().getParent();
        Files.createDirectories(folder);
        if (Files.isSymbolicLink(file))
            throw new IOException("Refusing to edit a symlinked story file");
        if (Files.notExists(file)) {
            // Validate before writing, preserve built-in UTF-8 template as originally shipped.
            atomicWrite(file, defaults);
            return new Result(Change.CREATED, null, template.getAsJsonArray("npcs").size(), 0);
        }
        byte[] original = Files.readAllBytes(file);
        if (original.length > MAX_BYTES)
            throw new IOException("Existing story config exceeds size cap: " + file);
        JsonObject current;
        try {
            current = JsonParser.parseString(new String(original, StandardCharsets.UTF_8))
                    .getAsJsonObject();
            StoryJson.parse(current);
        } catch (RuntimeException ex) {
            throw new IOException("Existing story config invalid; preserved without changes", ex);
        }
        JsonObject merged = current.deepCopy();
        int addedNpcs = 0;
        int addedGates = 0;
        JsonArray existingNpcs = merged.getAsJsonArray("npcs");
        for (JsonElement entry : template.getAsJsonArray("npcs")) {
            JsonObject newerNpc = entry.getAsJsonObject();
            String npcId = newerNpc.get("id").getAsString();
            JsonObject existingNpc = byId(existingNpcs, npcId);
            if (existingNpc == null) {
                existingNpcs.add(newerNpc.deepCopy());
                addedNpcs++;
                continue;
            }
            JsonArray oldNodes = existingNpc.getAsJsonArray("nodes");
            for (JsonElement node : newerNpc.getAsJsonArray("nodes")) {
                JsonObject proposed = node.getAsJsonObject();
                JsonObject originalNode = byId(oldNodes, proposed.get("id").getAsString());
                if (originalNode == null) {
                    // New dialogue nodes can be added without editing existing ones.
                    oldNodes.add(proposed.deepCopy());
                    continue;
                }
                JsonArray oldOptions = originalNode.getAsJsonArray("options");
                for (JsonElement opt : proposed.getAsJsonArray("options")) {
                    JsonObject newerOption = opt.getAsJsonObject();
                    JsonObject previousOption = byId(oldOptions, newerOption.get("id").getAsString());
                    if (previousOption == null) {
                        // New choices are add-only; no existing choice is rewritten.
                        oldOptions.add(newerOption.deepCopy());
                        continue;
                    }
                    if (!newerOption.has("requiresLegacy")) continue;
                    TreeSet<String> allowed = new TreeSet<>();
                    if (previousOption.has("requiresLegacy"))
                        for (JsonElement p : previousOption.getAsJsonArray("requiresLegacy"))
                            allowed.add(p.getAsString());
                    for (JsonElement r : newerOption.getAsJsonArray("requiresLegacy")) {
                        String tag = r.getAsString();
                        if (!LegacyOnboardingGate.allowed(tag))
                            throw new IOException("Bundled legacy marker not allowed: " + tag);
                        if (allowed.add(tag)) addedGates++;
                    }
                    JsonArray gates = new JsonArray();
                    for (String g : allowed) gates.add(g);
                    if (!allowed.isEmpty()) previousOption.add("requiresLegacy", gates);
                }
            }
        }
        // Reject incompatible user-defined graphs before touching anything.
        try { StoryJson.parse(merged); }
        catch (RuntimeException ex) {
            throw new IOException("Merged story would be invalid; original retained", ex);
        }
        if (merged.equals(current))
            return new Result(Change.UNCHANGED, null, 0, 0);

        byte[] output = GSON.toJson(merged).getBytes(StandardCharsets.UTF_8);
        if (output.length > MAX_BYTES)
            throw new IOException("Upgraded story exceeds file-size cap; original retained");
        Path backup = file.resolveSibling(file.getFileName() +
                ".backup-" + sha256(original).substring(0, 16) + ".json");
        // Create-only backup, never overwrite an existing backup, verify contents if present.
        try (FileChannel channel = FileChannel.open(backup, CREATE_NEW, WRITE)) {
            java.nio.ByteBuffer payload = java.nio.ByteBuffer.wrap(original);
            while (payload.hasRemaining()) channel.write(payload);
            channel.force(true);
        } catch (FileAlreadyExistsException previous) {
            if (!Arrays.equals(Files.readAllBytes(backup), original))
                throw new IOException("Conflicting story backup exists; original untouched");
        }
        atomicWrite(file, output);
        return new Result(Change.UPGRADED, backup, addedNpcs, addedGates);
    }

    private static JsonObject byId(JsonArray collection, String id) {
        for (JsonElement element : collection) {
            JsonObject item = element.getAsJsonObject();
            if (id.equals(item.get("id").getAsString())) return item;
        }
        return null;
    }

    private static String sha256(byte[] input) throws IOException {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(input);
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException ex) {
            throw new IOException("SHA-256 unavailable", ex);
        }
    }

    private static void atomicWrite(Path path, byte[] contents) throws IOException {
        Path tmp = Files.createTempFile(path.getParent(), ".pride-story-", ".tmp");
        try {
            try (FileChannel channel = FileChannel.open(tmp, WRITE, TRUNCATE_EXISTING)) {
                var data = java.nio.ByteBuffer.wrap(contents);
                while (data.hasRemaining()) channel.write(data);
                channel.force(true);
            }
            Files.move(tmp, path, StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING);
        } finally {
            Files.deleteIfExists(tmp);
        }
    }
}
