package com.prideregion.scenes.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.world.ClientWorld;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Lightweight player-only cinematic subtitle panel. This is a read-only HUD,
 * never a substitute for native Cobblemon dialogue choices or starter UI.
 * Only the server chooses the lines, actor, scene and order.
 */
public final class RinCinematicDialogueOverlay {
    private UUID session;
    private String speaker = "";
    private String line = "";
    private ClientWorld world;
    private int ticksLeft;
    private static final int DIALOGUE_TICKS = 68;

    public void accept(MinecraftClient client, ClientSceneSubtitlePayload packet) {
        if (client.world == null || client.player == null) return;
        if (world != null && world != client.world) clear();
        world = client.world;
        session = packet.session();
        speaker = packet.speaker();
        line = packet.line();
        ticksLeft = DIALOGUE_TICKS;
    }

    public void tick(MinecraftClient client) {
        if (world == null) return;
        if (client.world != world || client.player == null || --ticksLeft <= 0)
            clear();
    }

    public void clear() {
        world = null;
        session = null;
        speaker = "";
        line = "";
        ticksLeft = 0;
    }

    public void clearScene(UUID session) {
        if (session != null && session.equals(this.session)) clear();
    }

    public void draw(MinecraftClient client, DrawContext ctx) {
        if (world == null || client.world != world || ticksLeft <= 0
                || client.options.hudHidden) return;
        TextRenderer font = client.textRenderer;
        int screenWidth = ctx.getScaledWindowWidth();
        int screenHeight = ctx.getScaledWindowHeight();
        int panelWidth = Math.min(screenWidth - 24, 590);
        if (panelWidth < 160) return;

        List<String> lines = wrap(font, line, panelWidth - 34);
        int lineHeight = font.fontHeight + 4;
        int panelHeight = 32 + lines.size() * lineHeight;
        int x = (screenWidth - panelWidth) / 2;
        int y = screenHeight - panelHeight - 38;
        if (y < 8) return;

        // Deep teal Pokemon-style presentation rather than chat spam.
        ctx.fill(x, y, x + panelWidth, y + panelHeight, 0xDC0A1A24);
        ctx.fill(x, y, x + 4, y + panelHeight, 0xFF53D8CA);
        ctx.fill(x + 4, y, x + panelWidth, y + 2, 0xFF297F86);
        ctx.drawTextWithShadow(font, speaker, x + 18, y + 9, 0xFF70EBDD);

        int lineY = y + 25;
        for (String part : lines) {
            ctx.drawTextWithShadow(font, part, x + 18, lineY, 0xFFF2F7F8);
            lineY += lineHeight;
        }
    }

    private static List<String> wrap(TextRenderer font, String text, int maxWidth) {
        List<String> result = new ArrayList<>();
        StringBuilder line = new StringBuilder();
        for (String word : text.split(" ")) {
            String candidate = line.isEmpty() ? word : line + " " + word;
            if (!line.isEmpty() && font.getWidth(candidate) > maxWidth) {
                result.add(line.toString());
                line.setLength(0);
            }
            if (!line.isEmpty()) line.append(' ');
            line.append(word);
        }
        if (!line.isEmpty()) result.add(line.toString());
        if (result.isEmpty()) result.add("");
        return result.size() <= 4 ? result : result.subList(0,4);
    }
}
