package com.hbm_m.util;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Port of {@code com.hbm.util.Compat} (1.7.10).
 *
 * <p>The original guards every wide-area block-entity lookup with a chunk-loaded check so that
 * explosions and machine scans never drag unloaded chunks in. Direct {@code Level#getBlockEntity}
 * calls generate terrain synchronously on the server thread, which is what this class prevents.
 */
public final class Compat {

    private Compat() {}

    /** Port of {@code Compat.isPositionLoaded}: true only when the chunk is already in memory. */
    public static boolean isPositionLoaded(Level level, int x, int z) {
        return level != null && level.hasChunk(x >> 4, z >> 4);
    }

    public static boolean isPositionLoaded(Level level, BlockPos pos) {
        return pos != null && isPositionLoaded(level, pos.getX(), pos.getZ());
    }

    /** Port of {@code Compat.getTileStandard}: grabs a block entity without loading chunks. */
    public static BlockEntity getTileStandard(Level level, int x, int y, int z) {
        if (!isPositionLoaded(level, x, z)) return null;
        return level.getBlockEntity(new BlockPos(x, y, z));
    }

    public static BlockEntity getTileStandard(Level level, BlockPos pos) {
        if (!isPositionLoaded(level, pos)) return null;
        return level.getBlockEntity(pos);
    }
}
