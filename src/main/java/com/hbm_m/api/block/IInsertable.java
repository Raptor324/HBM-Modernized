package com.hbm_m.api.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** 1:1 {@code api.hbm.block.IInsertable}: Bloecke, in die der Kolben-Einschieber Gegenstaende druecken kann. */
public interface IInsertable {

    /** @param dir Richtung, in die geschoben wird (vom Einschieber weg). */
    boolean insertItem(Level world, BlockPos pos, Direction dir, ItemStack stack);
}
