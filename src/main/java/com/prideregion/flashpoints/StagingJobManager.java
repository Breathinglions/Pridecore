package com.prideregion.flashpoints;

import com.prideregion.flashpoints.layout.DungeonLayout;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import java.io.IOException;
import java.nio.file.*;
import java.util.BitSet;
import java.util.UUID;

public final class StagingJobManager {
    enum Phase { PREFLIGHT, PLACING, COMPLETE, ABORTED, CLEANING }

    private Job job;
    private StagingJournal.Snapshot recovery;
    private Path journalPath;
    private String journalError;

    private static final BlockState[] PALETTE={
        Blocks.AIR.getDefaultState(), Blocks.DEEPSLATE_BRICKS.getDefaultState(),
        Blocks.CRACKED_DEEPSLATE_BRICKS.getDefaultState(),Blocks.POLISHED_DEEPSLATE.getDefaultState(),
        Blocks.DEEPSLATE_TILES.getDefaultState(),Blocks.CHISELED_DEEPSLATE.getDefaultState(),
        Blocks.COBBLED_DEEPSLATE.getDefaultState(),Blocks.SEA_LANTERN.getDefaultState(),
        Blocks.AMETHYST_BLOCK.getDefaultState(),Blocks.CRYING_OBSIDIAN.getDefaultState(),
        Blocks.SMOOTH_BASALT.getDefaultState()
    };

    /** Read the persistent intent. Never automatically overwrite or remove on server startup. */
    public void initialize() {
        journalPath=FabricLoader.getInstance().getConfigDir().resolve("pride_flashpoint_active.bin");
        if(!Files.exists(journalPath))return;
        try {
            recovery=StagingJournal.load(journalPath);
            PrideFlashpointMod.LOGGER.warn("Unfinished Flashpoint journal found for {}. Enter that dimension and run /pridefp clear.",
                recovery.dimension());
        } catch(IOException ex) {
            journalError=ex.getMessage();
            PrideFlashpointMod.LOGGER.error("Cannot read Flashpoint journal. Refusing new builds. Preserve {} for recovery.",
                journalPath,ex);
        }
    }

    public String start(ServerWorld world,UUID player,BlockPos origin,DungeonLayout layout,DevConfig config) {
        if(journalPath==null)return "Generator not initialized.";
        if(journalError!=null)return "Invalid recovery journal; refusing new generation. Check logs.";
        if(recovery!=null || Files.exists(journalPath))
            return "A persistent dungeon is pending cleanup. Enter its world and run /pridefp clear.";
        if(job!=null)return "Complete or clear previous staging job first: /pridefp clear";
        if(!config.enabled())return "Disabled: config/pride_flashpoint_dev.properties enabled=false";
        if(config.dimension().equals("pride:staging_unconfigured"))
            return "Configure allowed_dimension to the exact test world dimension identifier.";
        String dimension=world.getRegistryKey().getValue().toString();
        if(dimension.equals("minecraft:overworld") || dimension.equals("minecraft:the_nether")
            || dimension.equals("minecraft:the_end"))
            return "Staging refuses vanilla dimensions. Use an isolated Multiworld dimension.";
        if(!dimension.equals(config.dimension()))
            return "Wrong dimension. Allowed: "+config.dimension();
        if(origin.getY()<world.getBottomY()+1 || origin.getY()+layout.height()>=world.getTopY())
            return "Build volume exceeds the world height limit.";
        job=new Job(world,player,origin,layout,config);
        return "Checking empty "+layout.width()+"x"+layout.length()+"x"+layout.height()
            +" volume at "+coords(origin)+". A recovery journal will be saved BEFORE block placement.";
    }

    public String status() {
        if(journalError!=null)return "CORRUPT persistent journal: "+journalError+". New builds disabled.";
        if(recovery!=null)
            return "RECOVERED pending dungeon: seed "+recovery.layout().seed()+" in "+recovery.dimension()
                +" at "+recovery.x()+","+recovery.y()+","+recovery.z()
                +". Enter that dimension and run /pridefp clear. Do not generate over it.";
        if(job==null)return "No dungeon tracked; no recovery journal.";
        Job j=job;
        return "Seed "+j.layout.seed()+", phase="+j.phase+", cursor="+j.cursor+"/"
            +j.layout.volume()+", placed this session="+j.placed.cardinality()
            +", persistent journal="+j.journaled+", origin="+coords(j.origin);
    }

    public String cancel() {
        if(recovery!=null)return "Recovered dungeon needs /pridefp clear.";
        if(job==null)return "No active dungeon job.";
        if(job.phase==Phase.PREFLIGHT) {
            job=null;return "Preflight canceled without changes.";
        }
        if(job.phase==Phase.CLEANING)return "Cleanup cannot be cancelled.";
        if(job.phase==Phase.COMPLETE)return "Completed; use /pridefp clear.";
        job.phase=Phase.ABORTED;
        return "Placement stopped. Persistent cleanup remains available via /pridefp clear.";
    }

    public String clear(ServerWorld world) {
        if(journalError!=null)return "Corrupted recovery journal: manual operator inspection required. No destructive action taken.";
        if(recovery!=null && job==null) {
            if(!world.getRegistryKey().getValue().toString().equals(recovery.dimension()))
                return "Wrong world; recovered dungeon belongs to "+recovery.dimension();
            job=new Job(world,recovery.owner(),new BlockPos(recovery.x(),recovery.y(),recovery.z()),
                recovery.layout(),new DevConfig(false,recovery.dimension(),3000,250));
            job.journaled=true;
            recovery=null;
        }
        if(job==null)return "No tracked dungeon.";
        if(job.world!=world)return "Enter the dimension containing this dungeon first.";
        if(job.phase==Phase.PREFLIGHT) {
            job=null;return "Preflight cancelled without changes.";
        }
        if(job.phase==Phase.CLEANING)return "Cleanup already in progress.";
        job.phase=Phase.CLEANING;job.cursor=0;
        return "Cleaning persistent dungeon journal. Matching generated blocks only; modified blocks preserved.";
    }

    public void tick(MinecraftServer server) {
        if(job==null)return;
        Job j=job;
        if(j.phase==Phase.COMPLETE || j.phase==Phase.ABORTED)return;
        int checked=0,writes=0;
        while(j.cursor<j.layout.volume() && checked<j.config.scanBudget()
                && writes<j.config.writeBudget()) {
            int index=j.cursor++;
            checked++;
            int x=j.origin.getX()+j.layout.x(index),y=j.origin.getY()+j.layout.y(index);
            int z=j.origin.getZ()+j.layout.z(index);
            if(!j.world.isChunkLoaded(ChunkPos.toLong(x>>4,z>>4))) {
                abort(server,j,"Chunk not loaded at "+x+","+z+". Load area and retry cleanup.");return;
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
                int mat=Byte.toUnsignedInt(j.layout.material(index));
                if(mat==0)continue;
                BlockState current=j.world.getBlockState(j.pos);
                if(current.equals(PALETTE[mat])) {
                    if(!j.world.setBlockState(j.pos,Blocks.AIR.getDefaultState(),3)) {
                        abort(server,j,"Cleanup write failed at "+coords(j.pos));return;
                    }
                    writes++;
                } else if(!current.isAir())j.preserved++;
            }
        }
        if(j.cursor<j.layout.volume())return;
        if(j.phase==Phase.PREFLIGHT) {
            // Durably save the complete intended layout before the very first block write.
            try {
                StagingJournal.save(journalPath,new StagingJournal.Snapshot(
                    j.world.getRegistryKey().getValue().toString(),j.player,
                    j.origin.getX(),j.origin.getY(),j.origin.getZ(),j.layout));
                j.journaled=true;
            } catch(IOException ex) {
                PrideFlashpointMod.LOGGER.error("Cannot persist dungeon journal; refusing placement",ex);
                job=null;
                message(server,j.player,"Preflight passed, but recovery journal could not be saved. NO blocks placed.");
                return;
            }
            j.phase=Phase.PLACING;j.cursor=0;
            message(server,j.player,"Preflight passed. Recovery journal saved; generating dungeon.");
        } else if(j.phase==Phase.PLACING) {
            j.phase=Phase.COMPLETE;
            message(server,j.player,"Generated "+j.placed.cardinality()+" blocks. Dungeon entrance: "
                +(j.origin.getX()+j.layout.entranceX())+" "+(j.origin.getY()+1)+" "
                +(j.origin.getZ()+j.layout.entranceZ())
                +". Use /pridefp clear even after restart to clean it.");
        } else if(j.phase==Phase.CLEANING) {
            try {
                // The journal is retained until the entire scan finishes successfully.
                StagingJournal.remove(journalPath);
                job=null;
                message(server,j.player,"Cleanup complete; "+j.preserved+
                    " modified blocks preserved. Recovery journal removed.");
            } catch(IOException ex) {
                j.phase=Phase.ABORTED;
                PrideFlashpointMod.LOGGER.error("Cleanup finished but journal delete failed",ex);
                message(server,j.player,"Cleanup scanned but journal removal failed. Review logs, retry /pridefp clear.");
            }
        }
    }

    private void abort(MinecraftServer server,Job j,String reason) {
        if(j.phase==Phase.PREFLIGHT) {
            job=null;
            message(server,j.player,"Preflight refused: "+reason+". No blocks placed.");
        } else {
            j.phase=Phase.ABORTED;
            message(server,j.player,"Dungeon job halted: "+reason+" Persistent journal retained. /pridefp clear to retry.");
        }
    }

    private static String coords(BlockPos pos) {
        return pos.getX()+","+pos.getY()+","+pos.getZ();
    }
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
        boolean journaled;
        int cursor;
        int preserved;
        Job(ServerWorld world,UUID player,BlockPos origin,DungeonLayout layout,DevConfig config) {
            this.world=world;this.player=player;this.origin=origin.toImmutable();
            this.layout=layout;this.config=config;this.placed=new BitSet(layout.volume());
        }
    }
}
