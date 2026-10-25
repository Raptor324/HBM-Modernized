package com.hbm_m.api.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

/** 1:1 {@code api.hbm.block.IBlowable}: Bloecke, die ein Ventilator ({@code MachineFan}) anblaest. */
public interface IBlowable {

    void applyFan(Level world, BlockPos pos, Direction dir, int dist);
}
