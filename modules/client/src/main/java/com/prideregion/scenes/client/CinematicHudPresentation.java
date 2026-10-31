package com.prideregion.scenes.client;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Presentation-only letterbox for the private cinematic. The genuine native
 * Cobblemon NPC skins/models and the storyline are never replaced.
 */
public final class CinematicHudPresentation {
    public record Settings(boolean bars, double barHeight,
                           String dialoguePosition, int dialogueWidth) {}

    private final Settings settings;
    private int activeTicks;

    public CinematicHudPresentation() {
        this.settings = load();
    }

    public Settings settings() { return settings; }

    public void tick(MinecraftClient client) {
        if (client != null && PrideScenesClientMod.cinematicActive(client))
            activeTicks = Math.min(20, activeTicks + 1);
        else activeTicks = 0;
    }

    public void drawBars(DrawContext ctx) {
        if (!settings.bars()) return;
        int width = ctx.getScaledWindowWidth();
        int height = ctx.getScaledWindowHeight();
        int band = Math.max(7, (int) Math.round(height * settings.barHeight()));
        int alpha = (int) (182 * Math.min(1.0, (activeTicks + 1.0) / 11.0));
        int color = (alpha << 24);
        int accent = ((int)(alpha * 0.55) << 24) | 0x005B9DA3;
        ctx.fill(0, 0, width, band, color);
        ctx.fill(0, band - 1, width, band, accent);
        ctx.fill(0, height - band, width, height, color);
        ctx.fill(0, height - band, width, height - band + 1, accent);
    }

    private static Settings load() {
        Path path = FabricLoader.getInstance().getConfigDir()
            .resolve("pride_scenes_client").resolve("hud.properties");
        LinkedHashMap<String, String> defaults = new LinkedHashMap<>();
        defaults.put("letterbox_bars", "true");
        defaults.put("letterbox_height", "0.065");
        defaults.put("dialogue_position", "bottom");
        defaults.put("dialogue_width", "320");

        Properties props = new Properties();
        try {
            Files.createDirectories(path.getParent());
            if (!Files.exists(path)) {
                StringBuilder contents = new StringBuilder(
                    "# Pride Scenes cinematic HUD (client only; restart after editing)\n"
                    + "# dialogue_position: top or bottom\n"
                    + "# letterbox_height: fraction of screen height, 0.0 to 0.14\n");
                defaults.forEach((k,v) -> contents.append(k).append('=').append(v).append('\n'));
                Files.writeString(path, contents);
            } else {
                try (var reader = Files.newInputStream(path)) { props.load(reader); }
                StringBuilder missing = new StringBuilder();
                for (Map.Entry<String, String> entry : defaults.entrySet())
                    if (!props.containsKey(entry.getKey()))
                        missing.append(entry.getKey()).append('=')
                            .append(entry.getValue()).append('\n');
                if (!missing.isEmpty())
                    Files.writeString(path, "\n# New Pride Scenes HUD options\n" + missing,
                        StandardOpenOption.APPEND);
            }
            try (var reader = Files.newInputStream(path)) { props.load(reader); }
        } catch (IOException ex) {
            org.slf4j.LoggerFactory.getLogger("PrideScenesHUD")
                .warn("Cannot load HUD settings; using presentation defaults", ex);
        }

        String position = props.getProperty("dialogue_position", "bottom").trim();
        if (!position.equals("top") && !position.equals("bottom")) position = "bottom";
        return new Settings(
            Boolean.parseBoolean(props.getProperty("letterbox_bars", "true")),
            number(props, "letterbox_height", .065, 0, .14),
            position,
            (int) number(props, "dialogue_width", 320, 200, 450));
    }

    private static double number(Properties props, String name,
                                 double fallback, double minimum, double maximum) {
        try {
            double value = Double.parseDouble(props.getProperty(name,
                Double.toString(fallback)).trim());
            if (!Double.isFinite(value)) return fallback;
            return Math.max(minimum, Math.min(maximum, value));
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }
}
