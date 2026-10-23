package com.prideregion.flashpoints.encounter;

import com.prideregion.flashpoints.PrideFlashpointMod;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.MinecraftServer;
import java.lang.reflect.Method;
import java.util.*;
import java.util.function.Consumer;

/**
 * Native Cobblemon 1.8.1 BATTLE_VICTORY listener, restricted to the exact
 * single-player vs single-wild battle shape. No reward or entity side effects.
 */
public final class NativeBattleBridge {
    public record EventData(UUID battleId,boolean wildCapture,
                            List<BattleOutcomeRules.Actor> winners,
                            List<BattleOutcomeRules.Actor> losers) {}
    private static boolean installed;
    private static String state="Not initialized";
    private NativeBattleBridge(){}

    public static boolean active(){return installed;}
    public static String status(){return state;}

    public static void install(MinecraftServer server,Consumer<EventData> listener) {
        if(installed)return;
        String version=FabricLoader.getInstance().getModContainer("cobblemon")
            .map(container->container.getMetadata().getVersion().getFriendlyString())
            .orElse("not installed");
        if(!"1.8.1+1.21.1".equals(version)) {
            state="Native battle OFF: requires Cobblemon 1.8.1+1.21.1, found "+version;
            return;
        }
        try {
            Class<?> events=Class.forName("com.cobblemon.mod.common.api.events.CobblemonEvents");
            Object signal=events.getField("BATTLE_VICTORY").get(null);
            Method subscribe=signal.getClass().getMethod("subscribe",Consumer.class);
            Consumer<Object> callback=event->{
                try {
                    boolean capture=Boolean.TRUE.equals(invoke(event,"getWasWildCapture"));
                    Object battle=invoke(event,"getBattle");
                    Object rawBattleId=invoke(battle,"getBattleId");
                    if(!(rawBattleId instanceof UUID battleId))
                        throw new ReflectiveOperationException("No battle UUID");
                    List<BattleOutcomeRules.Actor> winners=readActors(invoke(event,"getWinners"));
                    List<BattleOutcomeRules.Actor> losers=readActors(invoke(event,"getLosers"));
                    EventData snapshot=new EventData(battleId,capture,winners,losers);
                    server.execute(()->listener.accept(snapshot));
                }catch(ReflectiveOperationException | RuntimeException ex) {
                    PrideFlashpointMod.LOGGER.error("Native Cobblemon battle event could not be parsed; ignored",ex);
                }
            };
            Object token=subscribe.invoke(signal,callback);
            if(token==null)throw new IllegalStateException("Cobblemon returned no subscription token");
            installed=true;
            state="Cobblemon 1.8.1 BATTLE_VICTORY listener installed (single-player wild bosses only)";
            PrideFlashpointMod.LOGGER.info(state);
        }catch(ReflectiveOperationException | RuntimeException | LinkageError ex) {
            state="Native battle listener OFF: runtime API verification failed";
            PrideFlashpointMod.LOGGER.error(state,ex);
        }
    }

    private static List<BattleOutcomeRules.Actor> readActors(Object raw) throws ReflectiveOperationException {
        if(!(raw instanceof Iterable<?> actors))
            throw new ReflectiveOperationException("Actors not iterable");
        ArrayList<BattleOutcomeRules.Actor> values=new ArrayList<>();
        for(Object actor:actors) {
            String type=String.valueOf(invoke(actor,"getType"));
            Set<UUID> pokemon=new HashSet<>(),players=new HashSet<>();
            Object list=invoke(actor,"getPokemonList");
            if(!(list instanceof Iterable<?> entries))
                throw new ReflectiveOperationException("Actor pokemon list unavailable");
            for(Object p:entries) {
                Object original=invoke(p,"getOriginalPokemon");
                Object id=invoke(original,"getUuid");
                if(!(id instanceof UUID uuid))
                    throw new ReflectiveOperationException("Missing Pokemon UUID in battle");
                pokemon.add(uuid);
            }
            Object ids=invoke(actor,"getPlayerUUIDs");
            if(!(ids instanceof Iterable<?> iterable))
                throw new ReflectiveOperationException("Player UUID list unavailable");
            for(Object playerId:iterable) {
                if(!(playerId instanceof UUID uuid))
                    throw new ReflectiveOperationException("Invalid player UUID in battle");
                players.add(uuid);
            }
            values.add(new BattleOutcomeRules.Actor(
                "WILD".equals(type),"PLAYER".equals(type),pokemon,players));
        }
        return List.copyOf(values);
    }
    private static Object invoke(Object object,String method)throws ReflectiveOperationException {
        return object.getClass().getMethod(method).invoke(object);
    }
}
