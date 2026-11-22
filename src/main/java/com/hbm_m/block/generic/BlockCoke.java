package com.hbm_m.block.generic;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * 1:1 {@link com.hbm.blocks.generic.BlockCoke} ({@code block_coke}, Metadaten COAL/LIGNITE/PETROLEUM als
 * {@code block_coke_coal/_lignite/_petroleum}): brennbar (Entflammbarkeit 5, Ausbreitung 10).
 */
public class BlockCoke extends Block {

    public BlockCoke(Properties properties) {
        super(properties);
    }

    //? if >1.20.1 {
    /*public static final com.mojang.serialization.MapCodec<BlockCoke> CODEC = simpleCodec(BlockCoke::new);

    @Override
    protected com.mojang.serialization.MapCodec<? extends net.minecraft.world.level.block.Block> codec() {
        return CODEC;
    }
    *///?}

    @Override
    public int getFlammability(BlockState state, BlockGetter level, BlockPos pos, @Nullable Direction direction) {
        return 5;
    }

    @Override
    public int getFireSpreadSpeed(BlockState state, BlockGetter level, BlockPos pos, @Nullable Direction direction) {
        return 10;
    }
}
