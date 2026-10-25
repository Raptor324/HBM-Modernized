package com.hbm_m.block.bomb;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/** 1:1 {@code BlockFlammable}: Feuerausbreitung/Brennbarkeit und "brennt daneben Feuer?". */
public class BlockFlammable extends Block {

    public final int encouragement;
    public final int flammability;

    public BlockFlammable(Properties properties, int en, int flam) {
        super(properties);
        this.encouragement = en;
        this.flammability = flam;
    }

    @Override
    public int getFlammability(BlockState state, BlockGetter level, BlockPos pos, Direction face) {
        return flammability;
    }

    @Override
    public int getFireSpreadSpeed(BlockState state, BlockGetter level, BlockPos pos, Direction face) {
        return encouragement;
    }

    public boolean shouldIgnite(Level world, BlockPos pos) {
        if (flammability == 0) return false;
        for (Direction dir : Direction.values()) {
            if (world.getBlockState(pos.relative(dir)).is(Blocks.FIRE)) return true;
        }
        return false;
    }
}
