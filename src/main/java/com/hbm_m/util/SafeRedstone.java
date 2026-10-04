package com.hbm_m.util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

/**
 * Redstone reads that must never synchronously load chunks.
 *
 * <p>{@code Level.hasNeighborSignal} walks neighbouring blocks and every {@code getBlockState}
 * on an unloaded position triggers a synchronous chunk load. Machines ticked in still-loaded
 * chunks kept yanking their neighbours back out of the unload pipeline during unload waves
 * (gametest worlds, explosion craters, world exit): the queue never drained and the server
 * spent whole ticks in save/unload churn - thread dumps showed
 * {@code MachineBatteryBlockEntity.getCurrentMode} reloading chunks from inside
 * {@code PowerNet.update}.
 *
 * <p>The safe variant answers "unpowered" while any chunk of the 3x3 chunk neighbourhood is
 * not currently loaded (covers the direct neighbours plus the one-block redstone-wire fanout);
 * the read simply falls back to the unpowered default until the chunk comes back.
 */
public final class SafeRedstone {

    private SafeRedstone() {}

    /** {@code level.hasNeighborSignal(pos)} without the risk of a synchronous chunk load. */
    public static boolean hasNeighborSignal(Level level, BlockPos pos) {
        int cx = pos.getX() >> 4;
        int cz = pos.getZ() >> 4;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (!level.hasChunkAt(cx + dx, cz + dz)) {
                    return false;
                }
            }
        }
        return level.hasNeighborSignal(pos);
    }
}
