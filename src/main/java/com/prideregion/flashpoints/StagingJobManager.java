package com.prideregion.flashpoints;

import com.prideregion.flashpoints.layout.DungeonLayout;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import java.util.BitSet;
import java.util.UUID;

public final class StagingJobManager {
    enum Phase { PREFLIGHT, PLACING, COMPLETE, ABORTED, CLEANING }
    private Job job;
    private static final BlockState[] PALETTE={
        Blocks.AIR.getDefaultState(), Blocks.DEEPSLATE_BRICKS.getDefaultState(),
        Blocks.CRACKED_DEEPSLATE_BRICKS.getDefaultState(),Blocks.POLISHED_DEEPSLATE.getDefaultState(),
        Blocks.DEEPSLATE_TILES.getDefaultState(),Blocks.CHISELED_DEEPSLATE.getDefaultState(),
        Blocks.COBBLED_DEEPSLATE.getDefaultState(),Blocks.SEA_LANTERN.getDefaultState(),
        Blocks.AMETHYST_BLOCK.getDefaultState(),Blocks.CRYING_OBSIDIAN.getDefaultState(),
        Blocks.SMOOTH_BASALT.getDefaultState()
    };

    public String start(ServerWorld world,UUID player,BlockPos origin,DungeonLayout layout,DevConfig config) {
        if(job!=null)return "Clear previous staging job first: /pridefp clear";
        if(!config.enabled())return "Disabled: config/pride_flashpoint_dev.properties enabled=false";
        if(config.dimension().equals("pride:staging_unconfigured"))
            return "Configure allowed_dimension to the exact test world dimension identifier.";
        if(!world.getRegistryKey().getValue().toString().equals(config.dimension()))
            return "Wrong dimension. Allowed: "+config.dimension();
        if(origin.getY()<world.getBottomY()+1 || origin.getY()+layout.height()>=world.getTopY())
            return "Build volume exceeds the world height limit.";
        job=new Job(world,player,origin,layout,config);
        return "Checking "+layout.width()+"x"+layout.length()+"x"+layout.height()
            +" volume at "+coords(origin)+"; placement starts only after full empty-region check.";
    }

    public String status() {
        if(job==null)return "No dungeon tracked in this session.";
        Job j=job;
        return "Seed "+j.layout.seed()+", phase="+j.phase+", cursor="+j.cursor+"/"
            +j.layout.volume()+", placed="+j.placed.cardinality()+", origin="+coords(j.origin);
    }

    public String cancel() {
        if(job==null)return "No dungeon job.";
        if(job.phase==Phase.PREFLIGHT) { job=null; return "Preflight canceled without changes."; }
        if(job.phase==Phase.CLEANING)return "Cleanup cannot be cancelled.";
        if(job.phase==Phase.COMPLETE)return "Completed; use /pridefp clear.";
        job.phase=Phase.ABORTED;
        return "Placement stopped. Use /pridefp clear for session-only cleanup.";
    }

    public String clear(ServerWorld world) {
        if(job==null)return "No tracked dungeon. Restart loses this session's block journal.";
        if(job.world!=world)return "Enter the dimension containing this dungeon first.";
        if(job.phase==Phase.PREFLIGHT) { job=null; return "Preflight removed without changes."; }
        if(job.phase==Phase.CLEANING)return "Cleanup already in progress.";
        job.phase=Phase.CLEANING;job.cursor=0;
        return "Cleaning tracked blocks, preserving any player-modified blocks.";
    }

    public void tick(MinecraftServer server) {
        if(job==null)return;
        Job j=job;
        if(j.phase==Phase.COMPLETE || j.phase==Phase.ABORTED)return;
        int checked=0,writes=0;
        while(j.cursor<j.layout.volume()&&checked<j.config.scanBudget()&&writes<j.config.writeBudget()) {
            int index=j.cursor++;
            checked++;
            int x=j.origin.getX()+j.layout.x(index),y=j.origin.getY()+j.layout.y(index);
            int z=j.origin.getZ()+j.layout.z(index);
            if(!j.world.isChunkLoaded(ChunkPos.toLong(x>>4,z>>4))) {
                abort(server,j,"Chunk not loaded at "+x+","+z);return;
            }
            j.pos.set(x,y,z);
            if(j.phase==Phase.PREFLIGHT) {
                if(!j.world.getBlockState(j.pos).isAir()) {
                    abort(server,j,"Existing block at "+coords(j.pos));return;
                }
            } else if(j.phase==Phase.PLACING) {
                int mat=Byte.toUnsignedInt(j.layout.material(index));
                if(mat==0)continue;
                if(!j.world.getBlockState(j.pos).isAir()) {
                    abort(server,j,"Region changed at "+coords(j.pos));return;
                }
                if(!j.world.setBlockState(j.pos,PALETTE[mat],3)) {
                    abort(server,j,"Write failed at "+coords(j.pos));return;
                }
                j.placed.set(index);
                writes++;
            } else if(j.phase==Phase.CLEANING) {
                if(!j.placed.get(index))continue;
                int mat=Byte.toUnsignedInt(j.layout.material(index));
                if(!j.world.getBlockState(j.pos).equals(PALETTE[mat]))continue;
                if(j.world.setBlockState(j.pos,Blocks.AIR.getDefaultState(),3)) {
                    j.placed.clear(index);writes++;
                }
            }
        }
        if(j.cursor<j.layout.volume())return;
        if(j.phase==Phase.PREFLIGHT) {
            j.phase=Phase.PLACING;j.cursor=0;
            message(server,j.player,"Empty-region preflight passed; staging dungeon blocks.");
        } else if(j.phase==Phase.PLACING) {
            j.phase=Phase.COMPLETE;
            message(server,j.player,"Generated "+j.placed.cardinality()+" blocks. Dungeon entrance: "
                +(j.origin.getX()+j.layout.entranceX())+" "+(j.origin.getY()+1)+" "
                +(j.origin.getZ()+j.layout.entranceZ())+". /pridefp clear removes this session's blocks.");
        } else if(j.phase==Phase.CLEANING) {
            int preserved=j.placed.cardinality();job=null;
            message(server,j.player,"Cleanup completed; "+preserved+" blocks retained (changed or failed writes).");
        }
    }
    private void abort(MinecraftServer server,Job j,String reason) {
        if(j.phase==Phase.PREFLIGHT) {
            job=null;message(server,j.player,"Preflight refused: "+reason+". No blocks placed.");
        } else {
            j.phase=Phase.ABORTED;
            message(server,j.player,"Dungeon halted: "+reason+". Run /pridefp clear.");
        }
    }
    private static String coords(BlockPos pos) { return pos.getX()+","+pos.getY()+","+pos.getZ(); }
    private static void message(MinecraftServer server,UUID uuid,String message) {
        ServerPlayerEntity player=server.getPlayerManager().getPlayer(uuid);
        if(player!=null)player.sendMessage(Text.literal(message),false);
        PrideFlashpointMod.LOGGER.info(message);
    }
    private static final class Job {
        final ServerWorld world;
        final UUID player;
        final BlockPos origin;
        final DungeonLayout layout;
        final DevConfig config;
        final BitSet placed;
        final BlockPos.Mutable pos=new BlockPos.Mutable();
        Phase phase=Phase.PREFLIGHT;
        int cursor;
        Job(ServerWorld world,UUID player,BlockPos origin,DungeonLayout layout,DevConfig config) {
            this.world=world;this.player=player;this.origin=origin.toImmutable();
            this.layout=layout;this.config=config;this.placed=new BitSet(layout.volume());
        }
    }
}
