package com.prideregion.core.cinema;

import com.google.gson.*;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.WorldSavePath;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import static java.nio.file.StandardCopyOption.ATOMIC_MOVE;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

/**
 * Server-owned scene authoring and playback. No commands mutate profiles,
 * inventories, NPCs, battles or real player camera positions.
 */
public final class CameraDirectorModule {
    private static final Gson JSON = new GsonBuilder().setPrettyPrinting().create();
    private static Path directory;
    private static MinecraftServer activeServer;
    private record SceneLease(UUID session, long untilTick) {}
    private static final Map<UUID, SceneLease> activeCameras = new HashMap<>();
    private static final String DEFAULT_SCENE = "sequoia_lab";
    private CameraDirectorModule() {}

    public static void register() {
        PayloadTypeRegistry.playS2C().register(CameraDirectorPayload.ID, CameraDirectorPayload.CODEC);
    }

    public static void start(MinecraftServer server) throws IOException {
        activeServer = server;
        directory = server.getSavePath(WorldSavePath.ROOT)
            .resolve("data").resolve("pride_core").resolve("cinematics");
        Files.createDirectories(directory);
        Path sample = scenePath(DEFAULT_SCENE);
        if (!Files.exists(sample)) {
            JsonObject scene = newScene(DEFAULT_SCENE, "*");
            scene.addProperty("trigger", "RIN_LAB_INTRO");
            JsonArray shots = scene.getAsJsonArray("shots");
            // Long-form laboratory scene: follow Rin from the door, settle
            // into the approved west-facing camera and keep it for dialogue.
            JsonObject entrance = shot("rin_entrance", -53.5, 103.62, 3195.5, 90, 0, 88, 0);
            entrance.addProperty("target", "rin");
            entrance.addProperty("mode", "follow");
            entrance.addProperty("offsetX", 1.5);
            entrance.addProperty("offsetY", 1.6);
            entrance.addProperty("offsetZ", 2.0);
            shots.add(entrance);
            shots.add(shot("sequoia_dialogue", -53.5, 103.62, 3195.5, 90, 0, 244, 26));
            save(DEFAULT_SCENE, scene);
        }
    }

    public static void stop() {
        activeCameras.clear();
        activeServer = null;
        directory = null;
    }

    /** Auto-triggers never interrupt an existing cinematic or admin preview. */
    public static boolean cameraBusy(ServerPlayerEntity viewer) {
        SceneLease lease = activeCameras.get(viewer.getUuid());
        return lease != null && activeServer != null
            && activeServer.getTicks() <= lease.untilTick();
    }

    /** Non-OP location triggers call this on the server thread, never through commands. */
    public static boolean playFromLocation(ServerPlayerEntity viewer, String sceneName)
            throws IOException {
        if (cameraBusy(viewer)) return false;
        send(viewer, sceneName, UUID.randomUUID());
        return true;
    }


    public static com.mojang.brigadier.builder.LiteralArgumentBuilder<ServerCommandSource> commands() {
        var root = CommandManager.literal("cinema")
            .requires(s -> s.hasPermissionLevel(4));
        root.then(LocationCinematicTriggerModule.commands());
        root.then(CommandManager.literal("capture")
            .then(CommandManager.argument("scene", StringArgumentType.word())
            .then(CommandManager.argument("shot", StringArgumentType.word())
            .executes(c -> capture(c.getSource(),
                StringArgumentType.getString(c, "scene"),
                StringArgumentType.getString(c, "shot"))))));
        root.then(CommandManager.literal("preview")
            .then(CommandManager.argument("scene", StringArgumentType.word())
            .executes(c -> preview(c.getSource(), StringArgumentType.getString(c, "scene")))));
        root.then(CommandManager.literal("play")
            .then(CommandManager.argument("scene", StringArgumentType.word())
            .then(CommandManager.argument("player", EntityArgumentType.player())
            .executes(c -> play(c.getSource(),
                StringArgumentType.getString(c, "scene"),
                EntityArgumentType.getPlayer(c, "player"))))));
        root.then(CommandManager.literal("bind")
            .then(CommandManager.argument("scene", StringArgumentType.word())
            .then(CommandManager.argument("event", StringArgumentType.word())
            .executes(c -> bind(c.getSource(),
                StringArgumentType.getString(c, "scene"),
                StringArgumentType.getString(c, "event"))))));
        root.then(CommandManager.literal("playback")
            .then(CommandManager.argument("scene", StringArgumentType.word())
            .then(CommandManager.argument("mode", StringArgumentType.word())
            .executes(ctx -> playback(ctx.getSource(),
                StringArgumentType.getString(ctx, "scene"),
                StringArgumentType.getString(ctx, "mode"))))));
        root.then(CommandManager.literal("flight-time")
            .then(CommandManager.argument("scene", StringArgumentType.word())
            .then(CommandManager.argument("ticks", IntegerArgumentType.integer(20, 2400))
            .executes(ctx -> flightTime(ctx.getSource(),
                StringArgumentType.getString(ctx, "scene"),
                IntegerArgumentType.getInteger(ctx, "ticks"))))));
        root.then(CommandManager.literal("list")
            .then(CommandManager.argument("scene", StringArgumentType.word())
            .executes(c -> list(c.getSource(), StringArgumentType.getString(c, "scene")))));
        root.then(CommandManager.literal("timing")
            .then(CommandManager.argument("scene", StringArgumentType.word())
            .then(CommandManager.argument("shot", StringArgumentType.word())
            .then(CommandManager.argument("duration", IntegerArgumentType.integer(1, 1200))
            .then(CommandManager.argument("transition", IntegerArgumentType.integer(0, 120))
            .executes(c -> timing(c.getSource(),
                StringArgumentType.getString(c, "scene"),
                StringArgumentType.getString(c, "shot"),
                IntegerArgumentType.getInteger(c, "duration"),
                IntegerArgumentType.getInteger(c, "transition"))))))));
        root.then(CommandManager.literal("follow")
            .then(CommandManager.argument("scene", StringArgumentType.word())
            .then(CommandManager.argument("shot", StringArgumentType.word())
            .then(CommandManager.argument("target", StringArgumentType.word())
            .executes(c -> follow(c.getSource(),
                StringArgumentType.getString(c, "scene"),
                StringArgumentType.getString(c, "shot"),
                StringArgumentType.getString(c, "target")))))));
        root.then(CommandManager.literal("static")
            .then(CommandManager.argument("scene", StringArgumentType.word())
            .then(CommandManager.argument("shot", StringArgumentType.word())
            .executes(c -> follow(c.getSource(),
                StringArgumentType.getString(c, "scene"),
                StringArgumentType.getString(c, "shot"), "none")))));
        root.then(CommandManager.literal("move-after")
            .then(CommandManager.argument("scene", StringArgumentType.word())
            .then(CommandManager.argument("shot", StringArgumentType.word())
            .then(CommandManager.argument("anchor", StringArgumentType.word())
            .executes(c -> moveShot(c.getSource(),
                StringArgumentType.getString(c, "scene"),
                StringArgumentType.getString(c, "shot"),
                StringArgumentType.getString(c, "anchor")))))));
        root.then(CommandManager.literal("move-first")
            .then(CommandManager.argument("scene", StringArgumentType.word())
            .then(CommandManager.argument("shot", StringArgumentType.word())
            .executes(c -> moveShot(c.getSource(),
                StringArgumentType.getString(c, "scene"),
                StringArgumentType.getString(c, "shot"), null)))));
        root.then(CommandManager.literal("delete-shot")
            .then(CommandManager.argument("scene", StringArgumentType.word())
            .then(CommandManager.argument("shot", StringArgumentType.word())
            .executes(c -> delete(c.getSource(),
                StringArgumentType.getString(c, "scene"),
                StringArgumentType.getString(c, "shot"))))));
        return root;
    }

    /**
     * Bind ANY current trusted story private-scene actor ENTER to an authored
     * JSON cinematic. Existing dialogue, NPC actor and progress remain owned
     * by StoryModule; this is only an optional owner-local camera overlay.
     */
    public static void startBoundScene(ServerPlayerEntity viewer, UUID session,
                                       String storyEvent) {
        if (directory == null || activeServer == null
                || !ServerPlayNetworking.canSend(viewer, CameraDirectorPayload.ID))
            return;
        try (var entries = Files.list(directory)) {
            for (Path file : entries.filter(p -> p.getFileName().toString().endsWith(".json"))
                    .sorted().limit(128).toList()) {
                String name = file.getFileName().toString().replaceFirst("\\.json$", "");
                try {
                    JsonObject scene = load(name);
                    if (!scene.get("trigger").getAsString().equals(storyEvent)) continue;
                    send(viewer, name, session);
                    return; // one scene per verified story event and viewer
                } catch (IOException | RuntimeException ex) {
                    org.slf4j.LoggerFactory.getLogger("PrideCameraDirector")
                        .warn("Cinematic {} not usable for event {}: {}",
                            name, storyEvent, ex.toString());
                }
            }
        } catch (IOException ex) {
            org.slf4j.LoggerFactory.getLogger("PrideCameraDirector")
                .warn("Camera directory not readable: {}", ex.toString());
        }
    }

    public static void endBoundScene(ServerPlayerEntity viewer, UUID session) {
        SceneLease lease = activeCameras.get(viewer.getUuid());
        if (lease != null && lease.session().equals(session))
            activeCameras.remove(viewer.getUuid());
        if (ServerPlayNetworking.canSend(viewer, CameraDirectorPayload.ID))
            ServerPlayNetworking.send(viewer, new CameraDirectorPayload(session, true, ""));
    }

    private static void send(ServerPlayerEntity viewer, String name, UUID session)
            throws IOException {
        if (activeServer == null || !ServerPlayNetworking.canSend(viewer, CameraDirectorPayload.ID))
            throw new IOException("Client Camera Director channel unavailable");
        JsonObject scene = load(name);
        String world = viewer.getWorld().getRegistryKey().getValue().toString();
        if (!"*".equals(scene.get("dimension").getAsString())
                && !world.equals(scene.get("dimension").getAsString()))
            throw new IOException("Scene dimension " + scene.get("dimension").getAsString()
                + " differs from your current world " + world);
        JsonArray shots = scene.getAsJsonArray("shots");
        if (shots.isEmpty()) throw new IOException("Scene has no shots");
        // Require a nearby operator/viewer; no silent distant surveillance camera.
        JsonObject first = shots.get(0).getAsJsonObject();
        double dx = viewer.getX() - first.get("x").getAsDouble();
        double dy = viewer.getEyeY() - first.get("y").getAsDouble();
        double dz = viewer.getZ() - first.get("z").getAsDouble();
        if (dx * dx + dy * dy + dz * dz > 128 * 128)
            throw new IOException("Viewer too far from first authored camera shot");
        ServerPlayNetworking.send(viewer,
            new CameraDirectorPayload(session, false, JSON.toJson(scene)));
        int duration = 0;
        if ("flythrough".equals(playbackMode(scene))) {
            duration = flightTicks(scene);
        } else {
            for (JsonElement e : shots)
                duration += e.getAsJsonObject().get("durationTicks").getAsInt();
        }
        activeCameras.put(viewer.getUuid(),
            new SceneLease(session, activeServer.getTicks() + duration + 10L));
    }

    private static String playbackMode(JsonObject scene) {
        return scene.has("playback") ? scene.get("playback").getAsString() : "shots";
    }
    private static int flightTicks(JsonObject scene) {
        return scene.has("flythroughTicks") ? scene.get("flythroughTicks").getAsInt() : 240;
    }

    /** Scene-wide flythrough is opt-in. Legacy lab/actor shots remain unchanged. */
    private static int playback(ServerCommandSource source, String name, String mode) {
        try {
            if (!mode.equals("shots") && !mode.equals("flythrough"))
                throw new IllegalArgumentException("Playback must be shots or flythrough");
            JsonObject scene = load(name);
            scene.addProperty("playback", mode);
            if (mode.equals("flythrough") && !scene.has("flythroughTicks"))
                scene.addProperty("flythroughTicks", 240);
            save(name, scene);
            return ok(source, name + " playback=" + mode + (mode.equals("flythrough")
                ? " (" + flightTicks(scene)
                    + " ticks for the ENTIRE continuous flight)"
                : " (individual shots and holds)") + ". Preview to test.");
        } catch (Exception ex) { return error(source, ex); }
    }

    /** Total flight duration, not a separate stop/hold at every waypoint. */
    private static int flightTime(ServerCommandSource source, String name, int ticks) {
        try {
            JsonObject scene = load(name);
            scene.addProperty("flythroughTicks", ticks);
            save(name, scene);
            return ok(source, name + " flythrough total=" + ticks
                + " ticks (" + String.format(Locale.ROOT, "%.1f", ticks / 20.0)
                + "s at 20TPS). No waypoint pauses.");
        } catch (Exception ex) { return error(source, ex); }
    }

    private static int capture(ServerCommandSource source, String name, String id) {
        try {
            validateId(name); validateId(id);
            ServerPlayerEntity player = source.getPlayerOrThrow();
            String dimension = player.getWorld().getRegistryKey().getValue().toString();
            JsonObject scene = Files.exists(scenePath(name)) ? load(name) : newScene(name, dimension);
            String boundWorld = scene.get("dimension").getAsString();
            if (!dimension.equals(boundWorld) && !boundWorld.equals("*"))
                throw new IOException("Scene is in a different dimension");
            if (boundWorld.equals("*")) scene.addProperty("dimension", dimension);
            JsonObject captured = shot(id,
                player.getX(), player.getEyeY(), player.getZ(),
                player.getYaw(), player.getPitch(), 60, 20);
            JsonArray shots = scene.getAsJsonArray("shots");
            JsonArray replaced = new JsonArray();
            boolean found = false;
            for (JsonElement s : shots) {
                if (s.getAsJsonObject().get("id").getAsString().equals(id)) {
                    // Re-capturing an angle only updates its position and look.
                    // Preserve timing, transition, tracking and shot order.
                    JsonObject existing = s.getAsJsonObject();
                    for (String key : List.of("durationTicks", "transitionTicks",
                            "mode", "target", "offsetX", "offsetY", "offsetZ"))
                        captured.add(key, existing.get(key).deepCopy());
                    replaced.add(captured); found = true;
                } else replaced.add(s);
            }
            if (!found) replaced.add(captured);
            scene.add("shots", replaced);
            save(name, scene);
            return ok(source, "Captured shot " + name + "/" + id
                + " (" + (found ? "replaced" : "appended") + "). Preview: /pridecore cinema preview " + name);
        } catch (Exception ex) { return error(source, ex); }
    }

    private static int play(ServerCommandSource source, String name,
                            ServerPlayerEntity viewer) {
        try {
            send(viewer, name, UUID.randomUUID());
            return ok(source, "Playing " + name + " privately for "
                + viewer.getName().getString() + " (camera only)");
        } catch (Exception ex) { return error(source, ex); }
    }

    private static int bind(ServerCommandSource source, String name, String event) {
        try {
            if (!event.equals("none") && !Set.of("RIN_LAB_INTRO",
                    "RIN_CANYON_REUNION", "RIN_SUMMIT_REUNION").contains(event))
                throw new IllegalArgumentException("Unknown story event. Use RIN_LAB_INTRO, "
                    + "RIN_CANYON_REUNION, RIN_SUMMIT_REUNION or none");
            JsonObject scene = load(name);
            scene.addProperty("trigger", event);
            save(name, scene);
            return ok(source, "Bound scene " + name + " to verified event " + event);
        } catch (Exception ex) { return error(source, ex); }
    }

    private static int timing(ServerCommandSource source, String name, String id,
                              int duration, int transition) {
        try {
            JsonObject scene = load(name);
            JsonObject s = requireShot(scene, id);
            s.addProperty("durationTicks", duration);
            s.addProperty("transitionTicks", Math.min(transition, duration));
            save(name, scene);
            return ok(source, name + "/" + id + " duration=" + duration
                + " ticks, transition=" + Math.min(transition, duration) + " ticks");
        } catch (Exception ex) { return error(source, ex); }
    }

    private static int follow(ServerCommandSource source, String name, String id, String target) {
        try {
            JsonObject scene = load(name);
            JsonObject s = requireShot(scene, id);
            if (target.length() > 48 || !target.matches("[A-Za-z0-9_:-]+"))
                throw new IllegalArgumentException("Target must be an entity name, tag:... or rin");
            s.addProperty("mode", target.equals("none") ? "static" : "follow");
            s.addProperty("target", target);
            save(name, scene);
            return ok(source, name + "/" + id + " camera target=" + target);
        } catch (Exception ex) { return error(source, ex); }
    }

    /** Move a captured shot without hand-editing array order in JSON. */
    private static int moveShot(ServerCommandSource source, String sceneName,
                                String shotName, String after) {
        try {
            JsonObject scene = load(sceneName);
            validateId(shotName);
            if (after != null) {
                validateId(after);
                if (after.equals(shotName))
                    throw new IllegalArgumentException("A shot cannot follow itself");
            }
            JsonObject selected = requireShot(scene, shotName);
            if (after != null) requireShot(scene, after);
            JsonArray reordered = new JsonArray();
            if (after == null) reordered.add(selected);
            for (JsonElement element : scene.getAsJsonArray("shots")) {
                JsonObject next = element.getAsJsonObject();
                String id = next.get("id").getAsString();
                if (id.equals(shotName)) continue;
                reordered.add(next);
                if (after != null && id.equals(after)) reordered.add(selected);
            }
            scene.add("shots", reordered);
            save(sceneName, scene);
            return ok(source, "Moved " + shotName
                + (after == null ? " to first shot" : " after " + after)
                + " in " + sceneName);
        } catch (Exception ex) { return error(source, ex); }
    }

    private static int delete(ServerCommandSource source, String name, String id) {
        try {
            JsonObject scene = load(name);
            JsonArray replaced = new JsonArray();
            for (JsonElement s : scene.getAsJsonArray("shots"))
                if (!s.getAsJsonObject().get("id").getAsString().equals(id))
                    replaced.add(s);
            if (replaced.size() == scene.getAsJsonArray("shots").size())
                throw new IllegalArgumentException("Unknown shot " + id);
            scene.add("shots", replaced);
            save(name, scene);
            return ok(source, "Removed " + id + " from " + name);
        } catch (Exception ex) { return error(source, ex); }
    }

    private static int list(ServerCommandSource source, String name) {
        try {
            JsonObject scene = load(name);
            StringBuilder out = new StringBuilder(name + " ["
                + scene.get("dimension").getAsString() + "] playback="
                + playbackMode(scene)
                + ("flythrough".equals(playbackMode(scene))
                    ? "(" + flightTicks(scene) + "t total)" : "")
                + ": ");
            for (JsonElement shot : scene.getAsJsonArray("shots")) {
                JsonObject s = shot.getAsJsonObject();
                out.append(s.get("id").getAsString()).append("(")
                   .append(s.get("durationTicks").getAsInt()).append("t,")
                   .append(s.get("mode").getAsString()).append(") ");
            }
            if ("sequoia_lab".equals(name)) {
                int total = 0;
                for (JsonElement shot : scene.getAsJsonArray("shots"))
                    total += shot.getAsJsonObject().get("durationTicks").getAsInt();
                out.append(" | lab camera ").append(total).append("/332 ticks");
                if (total > 332) out.append(" (WARNING: live Rin scene ends at 332)");
            }
            return ok(source, out.toString());
        } catch (Exception ex) { return error(source, ex); }
    }

    private static int preview(ServerCommandSource source, String name) {
        try {
            ServerPlayerEntity viewer = source.getPlayerOrThrow();
            send(viewer, name, UUID.randomUUID());
            return ok(source, "Previewing " + name + " privately. No progression/rewards modified.");
        } catch (Exception ex) { return error(source, ex); }
    }

    private static JsonObject newScene(String name, String dimension) {
        JsonObject scene = new JsonObject();
        scene.addProperty("schema", 1);
        scene.addProperty("name", name);
        scene.addProperty("dimension", dimension);
        scene.addProperty("trigger", "none");
        scene.addProperty("playback", "shots"); // backward-compatible lab and NPC scenes
        scene.add("shots", new JsonArray());
        return scene;
    }

    private static JsonObject shot(String id, double x, double y, double z,
                                   double yaw, double pitch, int duration, int transition) {
        JsonObject s = new JsonObject();
        s.addProperty("id", id);
        s.addProperty("x", x);
        s.addProperty("y", y);
        s.addProperty("z", z);
        s.addProperty("yaw", yaw);
        s.addProperty("pitch", pitch);
        s.addProperty("durationTicks", duration);
        s.addProperty("transitionTicks", transition);
        s.addProperty("mode", "static");
        s.addProperty("target", "none");
        s.addProperty("offsetX", 1.5);
        s.addProperty("offsetY", 1.6);
        s.addProperty("offsetZ", 2.0);
        return s;
    }

    private static JsonObject requireShot(JsonObject scene, String id) {
        validateId(id);
        for (JsonElement s : scene.getAsJsonArray("shots"))
            if (s.getAsJsonObject().get("id").getAsString().equals(id))
                return s.getAsJsonObject();
        throw new IllegalArgumentException("Unknown shot " + id);
    }

    private static Path scenePath(String name) {
        validateId(name);
        if (directory == null) throw new IllegalStateException("Camera Director not started");
        return directory.resolve(name + ".json");
    }

    private static void validateId(String name) {
        if (name == null || !name.matches("[a-z0-9_-]{1,48}"))
            throw new IllegalArgumentException("Use lowercase a-z, 0-9, _ or -, maximum 48 characters");
    }

    private static JsonObject load(String name) throws IOException {
        Path file = scenePath(name);
        if (!Files.isRegularFile(file)) throw new IOException("Unknown scene " + name);
        JsonObject scene;
        try { scene = JsonParser.parseString(Files.readString(file)).getAsJsonObject(); }
        catch (RuntimeException ex) { throw new IOException("Invalid scene JSON", ex); }
        validate(name, scene);
        return scene;
    }

    private static void save(String name, JsonObject scene) throws IOException {
        validate(name, scene);
        Path file = scenePath(name);
        Path tmp = Files.createTempFile(directory, name + "-", ".tmp");
        try {
            Files.writeString(tmp, JSON.toJson(scene));
            try { Files.move(tmp, file, ATOMIC_MOVE, REPLACE_EXISTING); }
            catch (AtomicMoveNotSupportedException ex) { Files.move(tmp, file, REPLACE_EXISTING); }
        } finally { Files.deleteIfExists(tmp); }
    }

    private static void validate(String name, JsonObject scene) {
        if (!name.equals(scene.get("name").getAsString()))
            throw new IllegalArgumentException("Scene name does not match file");
        String dim = scene.get("dimension").getAsString();
        if ((!dim.equals("*") && !dim.matches("[a-z0-9_./-]+:[a-z0-9_./-]+"))
                || dim.length() > 100)
            throw new IllegalArgumentException("Invalid scene dimension");
        String trigger = scene.has("trigger") ? scene.get("trigger").getAsString() : "none";
        if (!Set.of("none", "RIN_LAB_INTRO", "RIN_CANYON_REUNION",
                "RIN_SUMMIT_REUNION").contains(trigger))
            throw new IllegalArgumentException("Invalid automatic story event binding");
        String playback = playbackMode(scene);
        if (!Set.of("shots", "flythrough").contains(playback))
            throw new IllegalArgumentException("Invalid playback; use shots or flythrough");
        if (scene.has("flythroughTicks")) {
            int flightTicks = scene.get("flythroughTicks").getAsInt();
            if (flightTicks < 20 || flightTicks > 2400)
                throw new IllegalArgumentException("Flythrough duration must be 20-2400 ticks");
        }
        JsonArray shots = scene.getAsJsonArray("shots");
        if (shots == null || shots.size() > 32)
            throw new IllegalArgumentException("Maximum 32 camera shots");
        if (playback.equals("flythrough") && shots.size() < 2)
            throw new IllegalArgumentException("Flythrough needs at least two captured waypoints");
        HashSet<String> ids = new HashSet<>();
        int total = 0;
        for (JsonElement item : shots) {
            JsonObject s = item.getAsJsonObject();
            validateId(s.get("id").getAsString());
            if (!ids.add(s.get("id").getAsString()))
                throw new IllegalArgumentException("Duplicate camera shot");
            for (String field : List.of("x", "y", "z", "yaw", "pitch", "offsetX", "offsetY", "offsetZ"))
                if (!Double.isFinite(s.get(field).getAsDouble()))
                    throw new IllegalArgumentException("Non-finite camera field " + field);
            if (Math.abs(s.get("x").getAsDouble()) > 29_999_984
                || Math.abs(s.get("z").getAsDouble()) > 29_999_984
                || s.get("y").getAsDouble() < -64 || s.get("y").getAsDouble() > 1024)
                throw new IllegalArgumentException("Camera coordinates outside world");
            int duration = s.get("durationTicks").getAsInt();
            int transition = s.get("transitionTicks").getAsInt();
            if (duration < 1 || duration > 1200 || transition < 0 || transition > duration)
                throw new IllegalArgumentException("Invalid shot timing");
            total += duration;
            if (total > 2400) throw new IllegalArgumentException("Maximum scene duration 120 seconds");
            String mode = s.get("mode").getAsString();
            if (!mode.equals("follow") && !mode.equals("static"))
                throw new IllegalArgumentException("Camera mode must be static or follow");
            if (playback.equals("flythrough") && !mode.equals("static"))
                throw new IllegalArgumentException("Flythrough waypoints must be static; "
                    + "run /pridecore cinema static <scene> <shot> first");
            if (s.get("target").getAsString().length() > 48)
                throw new IllegalArgumentException("Camera target too long");
        }
        if (JSON.toJson(scene).length() > 24_000)
            throw new IllegalArgumentException("Scene file too large");
    }

    private static int ok(ServerCommandSource source, String text) {
        source.sendFeedback(() -> Text.literal(text), false); return 1;
    }
    private static int error(ServerCommandSource source, Exception ex) {
        source.sendError(Text.literal("Cinematic Director: " + ex.getMessage())); return 0;
    }
}
