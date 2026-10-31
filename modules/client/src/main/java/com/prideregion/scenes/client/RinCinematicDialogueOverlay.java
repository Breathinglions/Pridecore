package com.prideregion.scenes.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Read-only Cobblemon-style cinematic dialogue, deliberately NOT a second
 * dialogue authority. Only the server chooses speaker, page text and timing.
 *
 * The pixel-frame, white page and left-side portrait mirror the native NPC
 * dialogue visual language without replacing Cobblemon's actual GUI screen
 * or intercepting its real starter-service choices.
 */
public final class RinCinematicDialogueOverlay {
    private static final Identifier RIN_SKIN =
        Identifier.of("pride_scenes_client", "textures/entity/rin.png");

    // Native Cobblemon dialogue uses a pale dialogue card, charcoal frame,
    // detachable speaker portrait and dark text. The values here are drawn
    // locally so users' server-side Cobblemon dialogue data is left alone.
    private static final int FRAME_SHADOW = 0xA0000000;
    private static final int FRAME_EDGE = 0xFF26282B;
    private static final int FRAME_GRAY = 0xFF787C81;
    private static final int FRAME_HIGHLIGHT = 0xFFD6D9DB;
    private static final int PANEL_WHITE = 0xFFF6F7F8;
    private static final int TEXT_DARK = 0xFF282B2D;
    private static final int TEXT_SECONDARY = 0xFF44484C;
    private static final int PORTRAIT_TEAL = 0xFF70C6C7;

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

    public void clearScene(UUID sceneSession) {
        if (sceneSession != null && sceneSession.equals(this.session)) clear();
    }

    public void draw(MinecraftClient client, DrawContext ctx) {
        if (world == null || client.world != world || ticksLeft <= 0
                || client.options.hudHidden) return;

        TextRenderer font = client.textRenderer;
        int screenWidth = ctx.getScaledWindowWidth();
        int screenHeight = ctx.getScaledWindowHeight();

        // Never fill the entire screen at GUI scale 4. Keep this anchored in
        // the upper portion, clear of Minecraft chat and the bottom party HUD.
        int width = Math.min(292, screenWidth - 74);
        if (width < 154) return;
        int x = Math.max(55, (screenWidth - width) / 2);
        int y = 22;

        List<String> wrapped = wrap(font, line, width - 23);
        int rowHeight = font.fontHeight + 4;
        int height = Math.max(55, 33 + rowHeight * wrapped.size());
        if (y + height + 3 > screenHeight - 12) return;

        // The Cobblemon-like page: dark keyline, gray bevel, solid white card.
        rect(ctx, x + 3, y + 4, width, height, FRAME_SHADOW);
        rect(ctx, x, y, width, height, FRAME_EDGE);
        rect(ctx, x + 2, y + 2, width - 4, height - 4, FRAME_GRAY);
        rect(ctx, x + 4, y + 4, width - 8, height - 8, PANEL_WHITE);
        rect(ctx, x + 4, y + 4, width - 8, 1, FRAME_HIGHLIGHT);

        // Portrait projects beyond the left edge like the Cobblemon NPC face
        // container. It never obscures the text or consumes click events.
        int portrait = 39;
        int portraitX = x - portrait + 2;
        int portraitY = y - 6;
        rect(ctx, portraitX + 2, portraitY + 3,
            portrait, portrait, FRAME_SHADOW);
        rect(ctx, portraitX, portraitY,
            portrait, portrait, FRAME_EDGE);
        rect(ctx, portraitX + 2, portraitY + 2,
            portrait - 4, portrait - 4, FRAME_GRAY);
        rect(ctx, portraitX + 4, portraitY + 4,
            portrait - 8, portrait - 8, PORTRAIT_TEAL);
        drawPortrait(ctx, font, portraitX, portraitY, portrait, speaker);

        // Tiny interlocking pointer joins the portrait to the dialogue page.
        rect(ctx, x - 1, y + 13, 8, 11, FRAME_EDGE);
        rect(ctx, x + 1, y + 15, 6, 7, FRAME_GRAY);
        rect(ctx, x + 3, y + 17, 3, 3, PANEL_WHITE);

        String name = speaker.length() > 32 ? speaker.substring(0, 32) : speaker;
        ctx.drawTextWithShadow(font, name, x + 12, y + 9, TEXT_SECONDARY);
        int rowY = y + 24;
        for (String part : wrapped) {
            ctx.drawText(font, part, x + 12, rowY, TEXT_DARK, false);
            rowY += rowHeight;
        }

        // Native-like unobtrusive continue glyph; auto-advance is still
        // driven by the server script, not by clicking this visual overlay.
        if ((ticksLeft / 12) % 2 == 0)
            ctx.drawText(font, "▼", x + width - 17,
                y + height - 15, FRAME_GRAY, false);
    }

    private static void drawPortrait(DrawContext ctx, TextRenderer font,
                                     int x, int y, int size, String speakerName) {
        if ("Rin".equalsIgnoreCase(speakerName)) {
            // Real 64x64 skin head + outer hat layer at 4x pixel scale.
            ctx.getMatrices().push();
            ctx.getMatrices().translate(x + 4, y + 4, 0);
            ctx.getMatrices().scale(3.875F, 3.875F, 1.0F);
            ctx.drawTexture(RIN_SKIN, 0, 0, 8, 8, 8, 8, 64, 64);
            ctx.drawTexture(RIN_SKIN, 0, 0, 40, 8, 8, 8, 64, 64);
            ctx.getMatrices().pop();
        } else {
            // Native NPC head rendering needs a Cobblemon client model
            // binding. Avoid fabricating Sequoia's likeness: a clear initial
            // identifies the speaker until a real portrait is supplied.
            ctx.drawCenteredTextWithShadow(font,
                "S", x + size / 2, y + size / 2 - 4, TEXT_DARK);
        }
    }

    private static void rect(DrawContext ctx, int x, int y,
                             int width, int height, int color) {
        ctx.fill(x, y, x + width, y + height, color);
    }

    private static List<String> wrap(TextRenderer font, String text, int maxWidth) {
        List<String> result = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (String word : text.split(" ")) {
            String proposal = current.isEmpty() ? word : current + " " + word;
            if (!current.isEmpty() && font.getWidth(proposal) > maxWidth) {
                result.add(current.toString());
                current.setLength(0);
            }
            if (!current.isEmpty()) current.append(' ');
            current.append(word);
        }
        if (!current.isEmpty()) result.add(current.toString());
        if (result.isEmpty()) result.add("");
        return result;
    }
}
