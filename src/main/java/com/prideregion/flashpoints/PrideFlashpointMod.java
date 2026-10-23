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
import com.prideregion.flashpoints.encounter.NativeCaptureBridge;
import com.prideregion.flashpoints.encounter.NativeBattleBridge;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.util.ActionResult;
import net.minecraft.server.world.ServerWorld;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
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
                .then(CommandManager.literal("compat")
                    .executes(ctx->compatibility(ctx.getSource())))
                .then(bossCommands())
                .then(orphanCommands())
                .then(CommandManager.literal("status").executes(ctx -> feedback(ctx.getSource(),JOBS.status())))
                .then(CommandManager.literal("cancel").executes(ctx -> feedback(ctx.getSource(),JOBS.cancel())))
                .then(CommandManager.literal("clear")
                    .executes(ctx -> feedback(ctx.getSource(),JOBS.clear(ctx.getSource().getWorld()))))
            ));
        ServerTickEvents.END_SERVER_TICK.register(JOBS::tick);
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            NativeCaptureBridge.install(server,(pokemonId,playerId) -> {
                JOBS.onRealPokemonCaptured(server,pokemonId,playerId);
                JOBS.onActionBossCaptured(server,pokemonId,playerId);
            });
            NativeBattleBridge.install(server,event -> JOBS.onNativeBattleVictory(server,event));
        });
        AttackEntityCallback.EVENT.register((player,world,hand,target,hit) -> {
            if(!(world instanceof ServerWorld) || !(player instanceof ServerPlayerEntity serverPlayer))
                return ActionResult.PASS;
            return JOBS.consumeActionMelee(serverPlayer,target)
                ? ActionResult.SUCCESS : ActionResult.PASS;
        });
        LOGGER.info("Pride Flashpoint v0.4.0 loaded: opt-in nonlethal sword/axe boss action meter; capture and battle integrations preserved.");
    }

    /** Inspect the actual installed Cobblemon API version without linking its classes. */
    private static int compatibility(ServerCommandSource source) {
        String version=FabricLoader.getInstance().getModContainer("cobblemon")
            .map(mod -> mod.getMetadata().getVersion().getFriendlyString())
            .orElse("NOT INSTALLED");
        return feedback(source,"Cobblemon: "+version+
            ". Capture: "+NativeCaptureBridge.status()+
            ". Battle: "+NativeBattleBridge.status()+
            ". Both require explicit arm; rewards are off.");
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
        boss.then(CommandManager.literal("action")
            .then(CommandManager.literal("arm")
                .executes(ctx->feedback(ctx.getSource(),JOBS.armActionBoss(ctx.getSource()))))
            .then(CommandManager.literal("status")
                .executes(ctx->feedback(ctx.getSource(),
                    JOBS.actionBossStatus(ctx.getSource().getWorld()))))
            .then(CommandManager.literal("reset")
                .executes(ctx->feedback(ctx.getSource(),
                    JOBS.resetActionBoss(ctx.getSource().getWorld())))));
        boss.then(CommandManager.literal("battle")
            .then(CommandManager.literal("status")
                .executes(ctx->feedback(ctx.getSource(),
                    JOBS.nativeBattleStatus(ctx.getSource().getWorld()))))
            .then(CommandManager.literal("arm")
                .executes(ctx->feedback(ctx.getSource(),
                    JOBS.armNativeBattle(ctx.getSource()))))
            .then(CommandManager.literal("reset")
                .executes(ctx->feedback(ctx.getSource(),
                    JOBS.resetNativeBattle(ctx.getSource().getWorld())))));
        boss.then(CommandManager.literal("native")
            .then(CommandManager.literal("status")
                .executes(ctx->feedback(ctx.getSource(),
                    JOBS.nativeCaptureStatus(ctx.getSource().getWorld()))))
            .then(CommandManager.literal("arm")
                .executes(ctx->feedback(ctx.getSource(),
                    JOBS.armNativeCapture(ctx.getSource()))))
            .then(CommandManager.literal("reset")
                .executes(ctx->feedback(ctx.getSource(),
                    JOBS.resetNativeCapture(ctx.getSource().getWorld())))));
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
