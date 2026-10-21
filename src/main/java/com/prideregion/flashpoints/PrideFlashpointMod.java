package com.prideregion.flashpoints;

import com.mojang.brigadier.arguments.LongArgumentType;
import com.prideregion.flashpoints.layout.DungeonGenerator;
import com.prideregion.flashpoints.layout.DungeonLayout;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;

public final class PrideFlashpointMod implements ModInitializer {
    public static final Logger LOGGER=LoggerFactory.getLogger("PrideFlashpoint");
    private static final StagingJobManager JOBS=new StagingJobManager();
    @Override public void onInitialize() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
            dispatcher.register(CommandManager.literal("pridefp")
                .requires(source -> source.hasPermissionLevel(4))
                .then(CommandManager.literal("preview")
                    .then(CommandManager.argument("seed",LongArgumentType.longArg())
                        .executes(ctx -> preview(ctx.getSource(),LongArgumentType.getLong(ctx,"seed")))))
                .then(CommandManager.literal("generate")
                    .then(CommandManager.argument("seed",LongArgumentType.longArg())
                        .executes(ctx -> generate(ctx.getSource(),LongArgumentType.getLong(ctx,"seed")))))
                .then(CommandManager.literal("status").executes(ctx -> feedback(ctx.getSource(),JOBS.status())))
                .then(CommandManager.literal("cancel").executes(ctx -> feedback(ctx.getSource(),JOBS.cancel())))
                .then(CommandManager.literal("clear")
                    .executes(ctx -> feedback(ctx.getSource(),JOBS.clear(ctx.getSource().getWorld()))))
            ));
        ServerTickEvents.END_SERVER_TICK.register(JOBS::tick);
        LOGGER.info("Pride Flashpoint procedural generator initialized (disabled until staging opt-in).");
    }
    private static int preview(ServerCommandSource source,long seed) {
        DungeonLayout d=DungeonGenerator.generate(seed);
        return feedback(source,"Seed "+seed+": "+d.width()+"x"+d.length()+"x"+d.height()
            +", "+d.filledBlocks()+" blocks. Entry ("+d.entranceX()+","+d.entranceZ()
            +") boss ("+d.bossX()+","+d.bossZ()+"). Nothing placed.");
    }
    private static int generate(ServerCommandSource source,long seed) {
        ServerPlayerEntity player=source.getPlayer();
        if(player==null)return feedback(source,"Run this command as an in-game level-4 operator.");
        try {
            DevConfig config=DevConfig.load();
            DungeonLayout layout=DungeonGenerator.generate(seed);
            BlockPos p=player.getBlockPos();
            BlockPos origin=new BlockPos(p.getX()-layout.entranceX(),p.getY()+12,p.getZ()-layout.entranceZ());
            return feedback(source,JOBS.start(source.getWorld(),player.getUuid(),origin,layout,config));
        } catch(IOException | RuntimeException ex) {
            LOGGER.error("Staging generation refused",ex);
            return feedback(source,"Generator error. See server logs; no new job started.");
        }
    }
    private static int feedback(ServerCommandSource source,String message) {
        source.sendFeedback(() -> Text.literal(message),false);
        return 1;
    }
}
