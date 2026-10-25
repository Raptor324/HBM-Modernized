package com.hbm_m.handler;

import java.util.ArrayList;
import java.util.List;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.saveddata.TomSaveData;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.VineBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * 1:1 {@code com.hbm.handler.ImpactWorldHandler}: Nachwirkungen des Tom-Einschlags in der Oberwelt -
 * ohne Licht sterben Pflanzen ab ({@link #die}), solange der Feuersturm tobt brennt die Oberflaeche ({@link #burn}).
 * Die statischen Felder sind die clientseitig synchronisierten Werte (Original: {@code PermaSyncHandler}).
 */
public class ImpactWorldHandler {

    public static void impactEffects(Level world) {

        if (!(world instanceof ServerLevel serv))
            return;

        if (world.dimension() != Level.OVERWORLD) {
            return;
        }

        TomSaveData data = TomSaveData.forWorld(serv);

        if (data.dust <= 0 && data.fire <= 0)
            return;

        List<LevelChunk> list = new ArrayList<>();
        for (ChunkHolder holder : ((com.hbm_m.mixin.ChunkMapAccessor) serv.getChunkSource().chunkMap).invokeGetChunks()) {
            LevelChunk chunk = holder.getTickingChunk();
            if (chunk != null) list.add(chunk);
        }
        int listSize = list.size();

        if (listSize > 0) {
            for (int i = 0; i < 3; i++) {

                LevelChunk chunk = list.get(serv.random.nextInt(listSize));
                int baseX = chunk.getPos().getMinBlockX();
                int baseZ = chunk.getPos().getMinBlockZ();

                for (int x = 0; x < 16; x++) {
                    for (int z = 0; z < 16; z++) {

                        if (world.random.nextBoolean()) continue;

                        int X = baseX + x;
                        int Z = baseZ + z;
                        int height = world.getHeight(Heightmap.Types.MOTION_BLOCKING, X, Z);
                        int Y = height - world.random.nextInt(Math.max(1, height - world.getMinBuildHeight()));

                        if (data.dust > 0) {
                            die(serv, X, Y, Z);
                        }
                        if (data.fire > 0) {
                            burn(serv, X, Y, Z);
                        }
                    }
                }
            }
        }
    }

    /// Plants die without sufficient light.
    public static void die(ServerLevel world, int x, int y, int z) {

        TomSaveData data = TomSaveData.forWorld(world);
        BlockPos pos = new BlockPos(x, y, z);
        int light = Math.max(world.getBrightness(LightLayer.BLOCK, pos.above()), (int) (world.getMaxLocalRawBrightness(pos.above()) * (1 - data.dust)));

        if (light < 4) {
            Block b = world.getBlockState(pos).getBlock();
            if (b == Blocks.GRASS_BLOCK) {
                world.setBlock(pos, Blocks.DIRT.defaultBlockState(), 3);
            } else if (b instanceof BushBlock) {
                world.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
            } else if (b instanceof LeavesBlock) {
                world.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
            } else if (b instanceof VineBlock) {
                world.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
            }
        }
    }

    /// Burn the world.
    public static void burn(ServerLevel world, int x, int y, int z) {

        BlockPos pos = new BlockPos(x, y, z);
        BlockState state = world.getBlockState(pos);
        Block b = state.getBlock();
        int sky = world.getBrightness(LightLayer.SKY, pos.above());

        if (state.isFlammable(world, pos, Direction.UP) && world.getBlockState(pos.above()).isAir() && sky >= 7) {
            if (b instanceof LeavesBlock || b instanceof BushBlock) {
                world.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
            }
            world.setBlock(pos.above(), BaseFireBlock.getState(world, pos.above()), 3);

        } else if ((b == Blocks.GRASS_BLOCK || b == Blocks.MYCELIUM || b == ModBlocks.WASTE_EARTH.get() || b == ModBlocks.FROZEN_GRASS.get() || b == ModBlocks.WASTE_MYCELIUM.get()) &&
                !world.isRainingAt(pos) && sky >= 7) {
            world.setBlock(pos, ModBlocks.BURNING_EARTH.get().defaultBlockState(), 3);

        } else if (b == ModBlocks.FROZEN_DIRT.get() && sky >= 7) {
            world.setBlock(pos, Blocks.DIRT.defaultBlockState(), 3);
        }
    }

    public static Level lastSyncWorld = null;
    public static float fire = 0F;
    public static float dust = 0F;
    public static boolean impact = false;

    public static float getFireForClient(Level world) {
        if (world != lastSyncWorld) return 0F;
        return fire;
    }

    public static float getDustForClient(Level world) {
        if (world != lastSyncWorld) return 0F;
        return dust;
    }

    public static boolean getImpactForClient(Level world) {
        if (world != lastSyncWorld) return false;
        return impact;
    }

    /** Original {@code proxy.getImpactDust}: Server liest die Weltdaten, Client den synchronisierten Wert. */
    public static float getImpactDust(Level world) {
        if (world instanceof ServerLevel sl) return TomSaveData.forWorld(sl).dust;
        return getDustForClient(world);
    }

    /** Original {@code proxy.getImpactFire}. */
    public static float getImpactFire(Level world) {
        if (world instanceof ServerLevel sl) return TomSaveData.forWorld(sl).fire;
        return getFireForClient(world);
    }

    /** Original {@code proxy.getImpact}. */
    public static boolean getImpact(Level world) {
        if (world instanceof ServerLevel sl) return TomSaveData.forWorld(sl).impact;
        return getImpactForClient(world);
    }
}
