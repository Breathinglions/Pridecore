package com.prideregion.core.npc;

import com.google.gson.*;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.WorldSavePath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;
import static java.nio.file.StandardCopyOption.*;

/**
 * Safe config-to-pack export of PNG NPC appearances.
 *
 * Cobblemon NPCs do NOT render arbitrary server PNGs. This manager produces
 * BOTH the client resource pack and an additive datapack override which changes
 * only Cobblemon NPC "resourceIdentifier". It never mutates player data,
 * quests, battle configuration, in-world NPC entities, or installed packs.
 *
 * A successful operator reload updates Polymer's override_assets/pride_skins
 * namespace and the active world's managed datapack ZIP. Polymer still needs
 * to rebuild and send its pack to players; nothing directly modifies or
 * overwrites the user's existing forced resource pack or other namespaces.
 */
public final class NpcSkinManager {
    private static final Logger LOG = LoggerFactory.getLogger("PrideNpcSkins");
    private static final Gson JSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String NAMESPACE = "pride_skins";
    private static final String PLAYER_MODEL = "pride_skins:player_64.geo";
    private static final String PLAYER_MODEL_FILE =
        "assets/pride_skins/bedrock/npcs/models/player_64.geo.json";
    private static final int MAX_SKINS = 128;
    private static final int MAX_NPCS = 128;
    private static final int MAX_PNG = 1024 * 1024;
    private static final String RESOURCE_ZIP = "PrideNPCSkins_Resources.zip";
    private static final String DATA_ZIP = "PrideNPCSkins_NPCOverrides.zip";

    private record Appearance(String file, String model, String poser) {}
    private record Mapping(Map<String, Appearance> skins, Map<String,String> npcClasses) {}
    private static Path root;
    private static String status = "NOT INITIALIZED";
    private static int lastSkinCount, lastNpcCount;
    private NpcSkinManager() {}

    public static synchronized void start() {
        root = FabricLoader.getInstance().getConfigDir()
            .resolve("pride_core").resolve("npc_skins");
        try {
            Files.createDirectories(root);
            Path mapping = root.resolve("skins.json");
            if (Files.notExists(mapping)) {
                JsonObject config = new JsonObject();
                config.addProperty("schema", 1);
                JsonObject skins = new JsonObject();
                JsonObject ranger = new JsonObject();
                ranger.addProperty("file", "ranger.png");
                ranger.addProperty("model", PLAYER_MODEL);
                ranger.addProperty("poser", "cobblemon:standard");
                skins.add("ranger", ranger);
                config.add("skins", skins);
                JsonObject npcs = new JsonObject();
                npcs.addProperty("pride_region:resource_ranger", "ranger");
                npcs.addProperty("pride_story:ranger", "ranger");
                config.add("npcClasses", npcs);
                // First boot only: never overwrite operator edits.
                Files.writeString(mapping, JSON.toJson(config) + "\n",
                    StandardOpenOption.CREATE_NEW);
            }
            status = "READY: upload PNG files and use /pridecore npc skins reload";
            LOG.info("NPC Skin Manager ready at {}", root);
        } catch (IOException failure) {
            status = "ERROR: " + failure.getMessage();
            LOG.error("NPC Skin Manager could not initialize; no packs generated", failure);
        }
    }

    public static synchronized void stop() {
        root = null;
        status = "NOT INITIALIZED";
        lastSkinCount = 0;
        lastNpcCount = 0;
    }

    public static LiteralArgumentBuilder<ServerCommandSource> commands() {
        var rootCommand = CommandManager.literal("npc")
            .requires(s -> s.hasPermissionLevel(4));
        var skin = CommandManager.literal("skins");
        skin.then(CommandManager.literal("status")
            .executes(c -> feedback(c.getSource(), status
                + " | last export: " + lastSkinCount + " PNG(s), "
                + lastNpcCount + " NPC class override(s)")));
        skin.then(CommandManager.literal("reload")
            .executes(c -> reload(c.getSource())));
        return rootCommand.then(skin);
    }

    private static int feedback(ServerCommandSource source, String message) {
        source.sendFeedback(() -> Text.literal("[Pride Skins] " + message), false);
        return 1;
    }

    private static int reload(ServerCommandSource source) {
        try {
            Mapping mapping = parse();
            Map<String, byte[]> resources = new TreeMap<>();
            Map<String, byte[]> overrides = new TreeMap<>();
            // Resource pack format 34 and datapack format 48 for MC 1.21.1.
            resources.put("pack.mcmeta", bytes("{\"pack\":{\"pack_format\":34,"
                + "\"description\":\"PrideCore NPC appearance textures\"}}"));
            overrides.put("pack.mcmeta", bytes("{\"pack\":{\"pack_format\":48,"
                + "\"description\":\"PrideCore NPC appearance mappings\"}}"));

            // Bundle an actual 64x64 player model rather than stretching
            // these skins onto Cobblemon's 64x128 trainer geometry.
            // Inactive catalog entries may be pre-registered before their PNG
            // has been authored. Only an explicitly NPC-mapped skin becomes
            // active and is validated/exported.
            Set<String> activeSkins = Set.copyOf(mapping.npcClasses().values());
            boolean usesPlayer64Model = mapping.skins().entrySet().stream()
                .filter(entry -> activeSkins.contains(entry.getKey()))
                .anyMatch(entry -> PLAYER_MODEL.equals(entry.getValue().model()));
            if (usesPlayer64Model) {
                try (InputStream model = NpcSkinManager.class.getClassLoader()
                        .getResourceAsStream(PLAYER_MODEL_FILE)) {
                    if (model == null)
                        throw new IOException("Bundled Pride player 64x64 geometry missing");
                    resources.put(PLAYER_MODEL_FILE, model.readAllBytes());
                }
            }
            for (var entry : mapping.skins().entrySet()) {
                String id = entry.getKey();
                if (!activeSkins.contains(id)) continue;
                Appearance skin = entry.getValue();
                Path file = root.resolve(skin.file());
                verifySkin(file);
                String pngPath = "assets/" + NAMESPACE + "/textures/npcs/" + id + ".png";
                resources.put(pngPath, Files.readAllBytes(file));

                JsonObject resolver = new JsonObject();
                resolver.addProperty("name", NAMESPACE + ":" + id);
                resolver.addProperty("order", 0);
                JsonArray variations = new JsonArray();
                JsonObject variant = new JsonObject();
                variant.add("aspects", new JsonArray());
                variant.addProperty("model", skin.model());
                variant.addProperty("poser", skin.poser());
                variant.addProperty("texture", NAMESPACE + ":textures/npcs/" + id + ".png");
                variations.add(variant);
                resolver.add("variations", variations);
                resources.put("assets/" + NAMESPACE + "/bedrock/npcs/variations/"
                    + id + "/0_" + id + ".json", bytes(JSON.toJson(resolver)));
            }

            for (var entry : mapping.npcClasses().entrySet()) {
                String npcId = entry.getKey();
                String[] parts = npcId.split(":", 2);
                String path = "data/" + parts[0] + "/npcs/" + parts[1] + ".json";
                // Read the original full NPC definition from the unified JAR;
                // only appearance is replaced. NEVER synthesize dialogue,
                // interaction, battle, flags or NPC IDs.
                InputStream stream = NpcSkinManager.class.getClassLoader()
                    .getResourceAsStream(path);
                if (stream == null)
                    throw new IOException("Cannot safely override " + npcId
                        + ": original NPC class absent in unified JAR");
                JsonObject npc;
                try (InputStream sourceFile = stream) {
                    npc = JsonParser.parseString(new String(
                        sourceFile.readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject();
                }
                if (!npc.has("resourceIdentifier"))
                    throw new IOException("NPC has no resourceIdentifier: " + npcId);
                npc.addProperty("resourceIdentifier",
                    NAMESPACE + ":" + entry.getValue());
                overrides.put(path, bytes(JSON.toJson(npc) + "\n"));
            }

            Path generated = root.resolve("generated");
            Files.createDirectories(generated);
            // Build/validate all entries before replacing EITHER existing output.
            byte[] resZip = zip(resources);
            byte[] dataZip = zip(overrides);
            commitOutput(generated.resolve(RESOURCE_ZIP), resZip);
            commitOutput(generated.resolve(DATA_ZIP), dataZip);
            // Sync ONLY the manager-owned resource namespace. All other
            // Polymer texture providers and the forced resource pack remain.
            syncPolymerAssets(resources);
            // Update the existing fixed datapack filename. A first-time pack
            // may still require /datapack enable; /reload applies edits.
            syncWorldDatapack(source, dataZip);
            lastSkinCount = activeSkins.size();
            lastNpcCount = mapping.npcClasses().size();
            status = "EXPORTED: " + lastSkinCount + " skins, "
                + lastNpcCount + " class mappings | generated/";
            LOG.info("NPC skin pack exported: {} texture(s), {} NPC class(es)",
                lastSkinCount, lastNpcCount);
            return feedback(source, status
                + ". Synced Polymer override_assets and world datapacks. "
                + "Use /reload for NPC class changes; restart Polymer/server "
                + "to rebuild its client pack, then reconnect. "
                + "This does not force-send a client pack.");
        } catch (IOException | RuntimeException e) {
            LOG.warn("NPC skin export rejected: {}", e.toString());
            source.sendError(Text.literal("[Pride Skins] Not applied: "
                + e.getMessage() + ". Prior generated output kept."));
            return 0;
        }
    }


    /**
     * Owned namespace only: no edits to pack.mcmeta, other mods, or existing
     * server.properties pack settings. Source ZIP includes only generated
     * resources; Polymer reads the extracted assets at its next pack build.
     */
    private static void syncPolymerAssets(Map<String, byte[]> resources) throws IOException {
        Path gameDir = FabricLoader.getInstance().getGameDir().toAbsolutePath().normalize();
        Path assets = gameDir.resolve("polymer").resolve("override_assets")
            .resolve("assets").normalize();
        Path target = assets.resolve(NAMESPACE).normalize();
        if (!target.startsWith(assets)) throw new IOException("Invalid Polymer target");
        if (Files.isSymbolicLink(target))
            throw new IOException("Polymer skin asset folder must not be a symlink");
        Files.createDirectories(target);
        for (var entry : resources.entrySet()) {
            if (!entry.getKey().startsWith("assets/" + NAMESPACE + "/")) continue;
            String relative = entry.getKey().substring(("assets/" + NAMESPACE + "/").length());
            Path dest = target.resolve(relative).normalize();
            if (!dest.startsWith(target))
                throw new IOException("Invalid Polymer asset path");
            for (Path part = dest.getParent(); part != null && part.startsWith(target);
                    part = part.getParent()) {
                if (Files.isSymbolicLink(part))
                    throw new IOException("Polymer asset destination is a symlink");
            }
            Files.createDirectories(dest.getParent());
            if (Files.isSymbolicLink(dest))
                throw new IOException("Polymer asset file is a symlink");
            commitBytes(dest, entry.getValue());
        }
    }

    private static void syncWorldDatapack(ServerCommandSource source, byte[] content)
            throws IOException {
        Path world = source.getServer().getSavePath(WorldSavePath.ROOT).normalize();
        Path packs = world.resolve("datapacks");
        Files.createDirectories(packs);
        Path target = packs.resolve(DATA_ZIP);
        if (Files.isSymbolicLink(packs) || Files.isSymbolicLink(target))
            throw new IOException("Refusing symlinked world datapack output");
        commitBytes(target, content);
    }

    private static void commitBytes(Path path, byte[] content) throws IOException {
        Path temp = Files.createTempFile(path.getParent(), "pride-asset-", ".tmp");
        try {
            Files.write(temp, content);
            try { Files.move(temp, path, ATOMIC_MOVE, REPLACE_EXISTING); }
            catch (AtomicMoveNotSupportedException e) {
                Files.move(temp, path, REPLACE_EXISTING);
            }
        } finally { Files.deleteIfExists(temp); }
    }

    private static Mapping parse() throws IOException {
        if (root == null) throw new IOException("NPC Skin Manager not initialized");
        Path path = root.resolve("skins.json");
        if (!Files.isRegularFile(path) || Files.isSymbolicLink(path)
                || Files.size(path) > 65536)
            throw new IOException("Missing/invalid skins.json");
        JsonObject cfg = JsonParser.parseString(Files.readString(path))
            .getAsJsonObject();
        if (!cfg.keySet().equals(Set.of("schema", "skins", "npcClasses"))
                || cfg.get("schema").getAsInt() != 1)
            throw new IOException("Invalid skin mapping schema");
        JsonObject skins = cfg.getAsJsonObject("skins");
        JsonObject npcs = cfg.getAsJsonObject("npcClasses");
        if (skins.size() == 0 || skins.size() > MAX_SKINS
                || npcs.size() == 0 || npcs.size() > MAX_NPCS)
            throw new IOException("Invalid skin/NPC mapping count");
        Map<String, Appearance> validated = new TreeMap<>();
        for (var e : skins.entrySet()) {
            String id = e.getKey();
            validateName(id);
            JsonObject skin = e.getValue().getAsJsonObject();
            if (!skin.keySet().equals(Set.of("file","model","poser")))
                throw new IOException("Skin definition needs file/model/poser: " + id);
            String file = skin.get("file").getAsString();
            if (!file.matches("[a-z0-9_-]{1,48}\\.png"))
                throw new IOException("Use a simple lowercase PNG filename: " + file);
            String model = skin.get("model").getAsString();
            String poser = skin.get("poser").getAsString();
            validateIdentifier(model, true);
            if (model.equals("cobblemon:trainer.geo")
                    || model.equals("cobblemon:standard.geo"))
                throw new IOException("Model " + model
                    + " is incompatible with standard 64x64 Minecraft skins. "
                    + "Use pride_skins:player_64.geo instead.");
            validateIdentifier(poser, false);
            validated.put(id, new Appearance(file, model, poser));
        }
        Map<String,String> classes = new TreeMap<>();
        for (var e : npcs.entrySet()) {
            String npc = e.getKey();
            validateIdentifier(npc, false);
            String skin = e.getValue().getAsString();
            if (!validated.containsKey(skin))
                throw new IOException("Unmapped skin " + skin + " for " + npc);
            // Only Pride-owned NPCs may be modified via this manager.
            if (!npc.startsWith("pride_region:") && !npc.startsWith("pride_story:"))
                throw new IOException("Refusing external NPC replacement: " + npc);
            classes.put(npc, skin);
        }
        return new Mapping(Map.copyOf(validated), Map.copyOf(classes));
    }

    private static void validateName(String name) throws IOException {
        if (!name.matches("[a-z0-9_-]{1,48}"))
            throw new IOException("Invalid skin ID " + name);
    }

    private static void validateIdentifier(String id, boolean model) throws IOException {
        String re = model ? "[a-z0-9_.-]+:[a-z0-9_./-]+"
            : "[a-z0-9_.-]+:[a-z0-9_./-]+";
        if (id.length() > 120 || !id.matches(re) || id.contains(".."))
            throw new IOException("Invalid resource identifier: " + id);
    }

    private static void verifySkin(Path file) throws IOException {
        if (!Files.isRegularFile(file) || Files.isSymbolicLink(file))
            throw new IOException("PNG missing or symlink forbidden: " + file.getFileName());
        long len = Files.size(file);
        if (len < 67 || len > MAX_PNG)
            throw new IOException("PNG size invalid: " + file.getFileName());
        byte[] signature = new byte[8];
        try (var input = Files.newInputStream(file)) {
            if (input.readNBytes(signature,0,8) != 8 ||
                !Arrays.equals(signature, new byte[] {(byte)137,80,78,71,13,10,26,10}))
                throw new IOException("Not a PNG: " + file.getFileName());
        }
        BufferedImage image = ImageIO.read(file.toFile());
        if (image == null || image.getWidth() != 64 || image.getHeight() != 64)
            throw new IOException("NPC skin must be 64x64: " + file.getFileName());
    }

    private static byte[] bytes(String s) {
        return s.getBytes(StandardCharsets.UTF_8);
    }
    private static byte[] zip(Map<String, byte[]> data) throws IOException {
        var buffer = new ByteArrayOutputStream();
        try (var out = new ZipOutputStream(buffer, StandardCharsets.UTF_8)) {
            for (var entry : data.entrySet()) {
                ZipEntry z = new ZipEntry(entry.getKey());
                z.setTime(0);
                out.putNextEntry(z);
                out.write(entry.getValue());
                out.closeEntry();
            }
        }
        return buffer.toByteArray();
    }
    private static void commitOutput(Path path, byte[] content) throws IOException {
        Path temp = Files.createTempFile(path.getParent(), "pride-skin-", ".zip.tmp");
        try {
            Files.write(temp, content);
            try { Files.move(temp,path,ATOMIC_MOVE,REPLACE_EXISTING); }
            catch (AtomicMoveNotSupportedException ex) {
                Files.move(temp,path,REPLACE_EXISTING);
            }
        } finally { Files.deleteIfExists(temp); }
    }
}
