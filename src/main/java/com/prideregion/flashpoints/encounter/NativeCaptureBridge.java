package com.prideregion.flashpoints.encounter;

import com.prideregion.flashpoints.PrideFlashpointMod;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import java.lang.reflect.Method;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Optional, version-gated Cobblemon 1.8.1 POKEMON_CAPTURED subscriber.
 *
 * Reflection deliberately avoids requiring the 130 MB Cobblemon mod at Java
 * compilation time. It uses ONLY validated CobblemonEvents/PokemonCapturedEvent
 * public APIs. No scanning or inferred outcomes; exact Pokemon UUID only.
 */
public final class NativeCaptureBridge {
    private static final String SUPPORTED="1.8.1+1.21.1";
    private static boolean installed;
    private static String state="Not initialized";
    private NativeCaptureBridge(){}

    public static void install(MinecraftServer server,BiConsumer<UUID,UUID> listener) {
        if(installed)return;
        String version=FabricLoader.getInstance().getModContainer("cobblemon")
            .map(mod->mod.getMetadata().getVersion().getFriendlyString())
            .orElse("not installed");
        if(!SUPPORTED.equals(version)) {
            state="Native listener OFF: requires Cobblemon "+SUPPORTED+", installed "+version;
            PrideFlashpointMod.LOGGER.warn(state);
            return;
        }
        try {
            Class<?> events=Class.forName("com.cobblemon.mod.common.api.events.CobblemonEvents");
            Object signal=events.getField("POKEMON_CAPTURED").get(null);
            Method subscribe=signal.getClass().getMethod("subscribe",Consumer.class);
            Consumer<Object> handler=event -> {
                try {
                    Object pokemon=event.getClass().getMethod("getPokemon").invoke(event);
                    Object rawPokemonUuid=pokemon.getClass().getMethod("getUuid").invoke(pokemon);
                    Object rawPlayer=event.getClass().getMethod("getPlayer").invoke(event);
                    if(!(rawPokemonUuid instanceof UUID pokemonUuid) ||
                            !(rawPlayer instanceof ServerPlayerEntity player)) {
                        PrideFlashpointMod.LOGGER.warn("Unsupported native captured event arguments");
                        return;
                    }
                    UUID actor=player.getUuid();
                    // Cobblemon event might fire off the world tick; mutate journals
                    // only back on the vanilla Minecraft server main executor.
                    server.execute(()->listener.accept(pokemonUuid,actor));
                } catch(ReflectiveOperationException | RuntimeException ex) {
                    PrideFlashpointMod.LOGGER.error("Native captured-event extraction failed",ex);
                }
            };
            Object token=subscribe.invoke(signal,handler);
            if(token==null)throw new IllegalStateException("No subscription token");
            installed=true;
            state="Native Cobblemon "+SUPPORTED+" capture listener installed (capture only, no defeat/wipe events)";
            PrideFlashpointMod.LOGGER.info(state);
        } catch(ReflectiveOperationException | LinkageError | RuntimeException ex) {
            state="Capture listener OFF: version 1.8.1 API compatibility check failed";
            PrideFlashpointMod.LOGGER.error(state,ex);
        }
    }

    public static boolean active(){return installed;}
    public static String status(){return state;}
}
