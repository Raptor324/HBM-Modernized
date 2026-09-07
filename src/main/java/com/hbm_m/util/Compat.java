package com.hbm_m.util;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;

/**
 * Port of {@code com.hbm.util.Compat} (1.7.10).
 *
 * <p>The original guards every wide-area block-entity lookup with a chunk-loaded check so that
 * explosions and machine scans never drag unloaded chunks in. Direct {@code Level#getBlockEntity}
 * calls generate terrain synchronously on the server thread, which is what this class prevents.
 *
 * <p>Both methods go through {@code getChunkNow} rather than {@code Level#hasChunk}: it resolves the
 * chunk once instead of twice on hot paths, and {@code ClientLevel#hasChunk} unconditionally returns
 * true, which would make the guard useless on the client.
 */
public final class Compat {

    private Compat() {}

    /** Port of {@code Compat.isPositionLoaded}: true only when the chunk is already in memory. */
    public static boolean isPositionLoaded(Level level, int x, int z) {
        return getLoadedChunk(level, x, z) != null;
    }

    public static boolean isPositionLoaded(Level level, BlockPos pos) {
        return pos != null && isPositionLoaded(level, pos.getX(), pos.getZ());
    }

    /** Port of {@code Compat.getTileStandard}: grabs a block entity without loading chunks. */
    public static BlockEntity getTileStandard(Level level, int x, int y, int z) {
        return getTileStandard(level, new BlockPos(x, y, z));
    }

    public static BlockEntity getTileStandard(Level level, BlockPos pos) {
        if (pos == null || level == null || level.isOutsideBuildHeight(pos)) return null;
        LevelChunk chunk = getLoadedChunk(level, pos.getX(), pos.getZ());
        if (chunk == null) return null;
        // Same creation type Level#getBlockEntity uses, so behaviour matches an unguarded lookup.
        return chunk.getBlockEntity(pos, LevelChunk.EntityCreationType.IMMEDIATE);
    }

    /** Already-resident chunk for a block position, or null. Never triggers loading or generation. */
    public static LevelChunk getLoadedChunk(Level level, int x, int z) {
        if (level == null) return null;
        return level.getChunkSource().getChunkNow(x >> 4, z >> 4);
    }
}
