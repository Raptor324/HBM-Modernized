package com.hbm_m.api.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;

/** 1:1 {@code IDetConnectible}: Bloecke, an die sich die Zuendschnur optisch anschliesst. */
public interface IDetConnectible {

    default boolean canConnectToDetCord(BlockGetter world, BlockPos pos, Direction dir) {
        return true;
    }

    static boolean isConnectible(BlockGetter world, BlockPos pos, Direction dir) {
        return world.getBlockState(pos).getBlock() instanceof IDetConnectible c && c.canConnectToDetCord(world, pos, dir);
    }
}
