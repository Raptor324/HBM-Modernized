package com.hbm_m.block.generic;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 1:1 {@link com.hbm.blocks.generic.BlockCoke} ({@code block_coke}, Metadaten COAL/LIGNITE/PETROLEUM als
 * {@code block_coke_coal/_lignite/_petroleum}): brennbar (Entflammbarkeit 5, Ausbreitung 10).
 */
public class BlockCoke extends Block {

    public BlockCoke(Properties properties) {
        super(properties);
    }

    @Override
    public int getFlammability(BlockState state, BlockGetter world, BlockPos pos, Direction face) {
        return 5;
    }

    @Override
    public int getFireSpreadSpeed(BlockState state, BlockGetter world, BlockPos pos, Direction face) {
        return 10;
    }
}
