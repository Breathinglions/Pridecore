package com.prideregion.flashpoints;

import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.server.command.CommandManager;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.prideregion.flashpoints.layout.DungeonGenerator;
import com.prideregion.flashpoints.layout.DungeonLayout;
import com.prideregion.flashpoints.layout.StandardDungeonGenerator;
import com.prideregion.flashpoints.encounter.BossProgress;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
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
                .then(CommandManager.literal("encounters")
                    .executes(ctx -> feedback(ctx.getSource(),JOBS.encounters(ctx.getSource().getWorld()))))
                .then(CommandManager.literal("encounter")
                    .then(CommandManager.literal("tracked")
                        .executes(ctx -> feedback(ctx.getSource(),
                            JOBS.trackedEncounters(ctx.getSource().getWorld()))))
                    .then(CommandManager.literal("track")
                        .then(CommandManager.argument("room",IntegerArgumentType.integer(0,15))
                            .executes(ctx -> feedback(ctx.getSource(),
                                JOBS.trackNearbyEncounter(ctx.getSource().getWorld(),
                                    IntegerArgumentType.getInteger(ctx,"room"))))))
                    .then(CommandManager.literal("forget")
                        .then(CommandManager.argument("room",IntegerArgumentType.integer(0,15))
                            .executes(ctx -> feedback(ctx.getSource(),
                                JOBS.forgetEncounter(ctx.getSource().getWorld(),
                                    IntegerArgumentType.getInteger(ctx,"room"))))))
                    .then(CommandManager.argument("room",IntegerArgumentType.integer(0,15))
                        .executes(ctx -> feedback(ctx.getSource(),JOBS.encounter(
                            ctx.getSource().getWorld(),IntegerArgumentType.getInteger(ctx,"room")))))
                    .then(CommandManager.literal("spawn")
                        .then(CommandManager.argument("room",IntegerArgumentType.integer(0,15))
                            .executes(ctx -> feedback(ctx.getSource(),JOBS.testSpawn(
                                ctx.getSource(),IntegerArgumentType.getInteger(ctx,"room")))))))
                .then(bossCommands())
                .then(orphanCommands())
                .then(CommandManager.literal("status").executes(ctx -> feedback(ctx.getSource(),JOBS.status())))
                .then(CommandManager.literal("cancel").executes(ctx -> feedback(ctx.getSource(),JOBS.cancel())))
                .then(CommandManager.literal("clear")
                    .executes(ctx -> feedback(ctx.getSource(),JOBS.clear(ctx.getSource().getWorld()))))
            ));
        ServerTickEvents.END_SERVER_TICK.register(JOBS::tick);
        LOGGER.info("Pride Flashpoint v0.3.2 loaded: persistent boss simulation state machine (no native capture events).");
    }

    private static LiteralArgumentBuilder<ServerCommandSource> bossCommands() {
        LiteralArgumentBuilder<ServerCommandSource> boss=CommandManager.literal("boss");
        boss.then(CommandManager.literal("status")
            .executes(ctx->feedback(ctx.getSource(),
                JOBS.bossStatus(ctx.getSource().getWorld()))));
        LiteralArgumentBuilder<ServerCommandSource> simulation=CommandManager.literal("simulate");
        for(var action: BossProgress.Action.values()) {
            simulation.then(CommandManager.literal(action.name().toLowerCase(java.util.Locale.ROOT))
                .executes(ctx->simulateBoss(ctx.getSource(),action)));
        }
        boss.then(simulation);
        boss.then(CommandManager.literal("reset")
            .executes(ctx->feedback(ctx.getSource(),
                JOBS.resetBoss(ctx.getSource().getWorld()))));
        return boss;
    }

    private static int simulateBoss(ServerCommandSource source,BossProgress.Action action) {
        ServerPlayerEntity player=source.getPlayer();
        if(player==null)return feedback(source,"Boss simulation requires an in-game operator.");
        return feedback(source,JOBS.simulateBoss(source.getWorld(),player.getUuid(),action));
    }

    private static LiteralArgumentBuilder<ServerCommandSource> orphanCommands() {
        RequiredArgumentBuilder<ServerCommandSource,Integer> coordsZ=CommandManager.argument("z", IntegerArgumentType.integer())
            .executes(ctx -> inspectOrphan(ctx.getSource(),
                LongArgumentType.getLong(ctx,"seed"),
                IntegerArgumentType.getInteger(ctx,"x"),
                IntegerArgumentType.getInteger(ctx,"y"),
                IntegerArgumentType.getInteger(ctx,"z")));
        RequiredArgumentBuilder<ServerCommandSource,Integer> coordsY=CommandManager.argument("y", IntegerArgumentType.integer()).then(coordsZ);
        RequiredArgumentBuilder<ServerCommandSource,Integer> coordsX=CommandManager.argument("x", IntegerArgumentType.integer()).then(coordsY);
        RequiredArgumentBuilder<ServerCommandSource,Long> seed=CommandManager.argument("seed",LongArgumentType.longArg()).then(coordsX);
        return CommandManager.literal("orphan")
            .then(CommandManager.literal("inspect")
                .then(CommandManager.literal("standard").then(seed)))
            .then(CommandManager.literal("adopt")
                .executes(ctx -> feedback(ctx.getSource(),
                    JOBS.adoptOrphan(ctx.getSource().getWorld()))));
    }

    private static int inspectOrphan(ServerCommandSource source,long seed,int x,int y,int z) {
        ServerPlayerEntity player=source.getPlayer();
        if(player==null)return feedback(source,"Use an in-game operator in the disposable test dimension.");
        try {
            return feedback(source,JOBS.inspectOrphan(source.getWorld(),player.getUuid(),
                new BlockPos(x,y,z),StandardDungeonGenerator.generate(seed),DevConfig.load()));
        } catch(IOException | RuntimeException ex) {
            LOGGER.error("Orphan inspection refused",ex);
            return feedback(source,"Inspection failed without changing blocks. See server logs.");
        }
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
