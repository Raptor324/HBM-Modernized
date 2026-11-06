package com.hbm_m.block.generic;

import com.hbm_m.block.ModBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Порт {@code BlockOre#onNeighborBlockChange} (1.7.10) для {@code ore_oil}:
 * нефть стекает вниз — если под залежью опустошённая скважина ({@code ore_oil_empty}),
 * нефть проваливается в неё, оставляя пустоту на своём месте.
 */
public class OilDepositBlock extends Block {

    public OilDepositBlock(Properties properties) {
        super(properties);
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean isMoving) {
        super.neighborChanged(state, level, pos, block, fromPos, isMoving);
        if (level.isClientSide) return;
        if (level.getBlockState(pos.below()).is(ModBlocks.ORE_OIL_EMPTY.get())) {
            level.setBlock(pos, ModBlocks.ORE_OIL_EMPTY.get().defaultBlockState(), 3);
            level.setBlock(pos.below(), ModBlocks.ORE_OIL.get().defaultBlockState(), 3);
        }
    }
}
