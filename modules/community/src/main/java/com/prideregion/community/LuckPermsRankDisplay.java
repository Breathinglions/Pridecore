package com.prideregion.community;

import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.Formatting;
import java.util.Locale;
import java.util.UUID;

/** Loaded only when LuckPerms is present; never modifies LP data or rank glyph assets. */
final class LuckPermsRankDisplay {
    static Text styledName(UUID player, String fallbackName) {
        User user = LuckPermsProvider.get().getUserManager().getUser(player);
        if (user == null) return Text.literal(fallbackName).styled(s -> s.withColor(TextColor.fromRgb(0x81DDA4)));
        var meta = user.getCachedData().getMetaData(user.getQueryOptions());
        String prefix = meta.getPrefix();
        String group = user.getPrimaryGroup().toLowerCase(Locale.ROOT);
        // Actual Pride Region icons take precedence, including user-level LP prefix overrides.
        if (prefix != null) {
            if (prefix.indexOf('\uE145') >= 0) group = "owner";
            else if (prefix.indexOf('\uE144') >= 0) group = "admin";
            else if (prefix.indexOf('\uE143') >= 0) group = "moderator";
            else if (prefix.indexOf('\uE142') >= 0) group = "builder";
            else if (prefix.indexOf('\uE141') >= 0) group = "vip";
            else if (prefix.indexOf('\uE140') >= 0) group = "trainer";
        }
        int color = switch (group) {
            case "owner" -> 0xD6B0FF;
            case "admin" -> 0xF3A4A4;
            case "moderator", "mod" -> 0xB2B8FF;
            case "builder" -> 0x83D7EF;
            case "vip" -> 0xEDCC84;
            default -> 0x81DDA4;
        };
        Text name = Text.literal(fallbackName).styled(s -> s.withColor(TextColor.fromRgb(color)));
        // Keep the bitmap sprite colors intact; do not tint the custom glyph prefix.
        if (prefix == null || prefix.isBlank()) return name;
        return Text.empty().append(Text.literal(prefix).formatted(Formatting.WHITE)).append(name);
    }
}
