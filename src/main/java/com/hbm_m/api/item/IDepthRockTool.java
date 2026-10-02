package com.hbm_m.api.item;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;

/** 1:1 {@code api.hbm.item.IDepthRockTool}. */
public interface IDepthRockTool {

    /**
     * Whether our item can break depthrock, has a couple of params so we can restrict mining for certain blocks, dimensions or positions
     */
    boolean canBreakRock(BlockGetter world, Player player, ItemStack tool, BlockState block, BlockPos pos);
}
