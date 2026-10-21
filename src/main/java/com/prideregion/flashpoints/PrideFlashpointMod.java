package com.prideregion.flashpoints;

import com.mojang.brigadier.arguments.LongArgumentType;
import com.prideregion.flashpoints.layout.DungeonGenerator;
import com.prideregion.flashpoints.layout.DungeonLayout;
import com.prideregion.flashpoints.layout.StandardDungeonGenerator;
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
        JOBS.initialize();
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
            dispatcher.register(CommandManager.literal("pridefp")
                .requires(source -> source.hasPermissionLevel(4))
                // Original syntax and the compact v0.1 generator remain intact.
                .then(CommandManager.literal("preview")
                    .then(CommandManager.argument("seed",LongArgumentType.longArg())
                        .executes(ctx -> preview(ctx.getSource(),LongArgumentType.getLong(ctx,"seed"),false)))
                    .then(CommandManager.literal("standard")
                        .then(CommandManager.argument("seed",LongArgumentType.longArg())
                            .executes(ctx -> preview(ctx.getSource(),LongArgumentType.getLong(ctx,"seed"),true)))))
                .then(CommandManager.literal("generate")
                    .then(CommandManager.argument("seed",LongArgumentType.longArg())
                        .executes(ctx -> generate(ctx.getSource(),LongArgumentType.getLong(ctx,"seed"),false)))
                    .then(CommandManager.literal("standard")
                        .then(CommandManager.argument("seed",LongArgumentType.longArg())
                            .executes(ctx -> generate(ctx.getSource(),LongArgumentType.getLong(ctx,"seed"),true)))))
                .then(CommandManager.literal("status").executes(ctx -> feedback(ctx.getSource(),JOBS.status())))
                .then(CommandManager.literal("cancel").executes(ctx -> feedback(ctx.getSource(),JOBS.cancel())))
                .then(CommandManager.literal("clear")
                    .executes(ctx -> feedback(ctx.getSource(),JOBS.clear(ctx.getSource().getWorld()))))
            ));
        ServerTickEvents.END_SERVER_TICK.register(JOBS::tick);
        LOGGER.info("Pride Flashpoint v0.2 loaded. Journaled, staging-only dungeon generator.");
    }

    private static DungeonLayout layout(long seed,boolean standard) {
        return standard ? StandardDungeonGenerator.generate(seed) : DungeonGenerator.generate(seed);
    }
    private static int preview(ServerCommandSource source,long seed,boolean standard) {
        DungeonLayout d=layout(seed,standard);
        return feedback(source,(standard?"Standard":"Classic")+" seed "+seed+": "
            +d.width()+"x"+d.length()+"x"+d.height()+", "+d.filledBlocks()
            +" blocks. Entry ("+d.entranceX()+","+d.entranceZ()+") boss ("
            +d.bossX()+","+d.bossZ()+"). Nothing placed.");
    }
    private static int generate(ServerCommandSource source,long seed,boolean standard) {
        ServerPlayerEntity player=source.getPlayer();
        if(player==null)return feedback(source,"Run this command as an in-game level-4 operator.");
        try {
            DevConfig config=DevConfig.load();
            DungeonLayout d=layout(seed,standard);
            BlockPos p=player.getBlockPos();
            BlockPos origin=new BlockPos(p.getX()-d.entranceX(),p.getY()+12,p.getZ()-d.entranceZ());
            return feedback(source,JOBS.start(source.getWorld(),player.getUuid(),origin,d,config));
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
