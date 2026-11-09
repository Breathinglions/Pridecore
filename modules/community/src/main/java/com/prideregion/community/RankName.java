package com.prideregion.community;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import java.util.UUID;

/** A missing/outdated LuckPerms installation cannot stop quiz rounds. */
final class RankName {
    static Text display(UUID uuid, String name) {
        if (FabricLoader.getInstance().isModLoaded("luckperms")) {
            try { return LuckPermsRankDisplay.styledName(uuid, name); }
            catch (LinkageError | IllegalStateException problem) {
                // Safely degrade to the Minecraft player name.
            }
        }
        return Text.literal(name).styled(s -> s.withColor(TextColor.fromRgb(0x81DDA4)));
    }
}
