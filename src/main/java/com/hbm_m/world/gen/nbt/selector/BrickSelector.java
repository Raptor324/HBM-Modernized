package com.hbm_m.world.gen.nbt.selector;

import com.hbm_m.world.gen.nbt.BlockSelector;

import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;

/** 1:1 {@code BrickSelector}. */
public class BrickSelector extends BlockSelector {

    @Override
    public void selectBlocks(RandomSource rand, int x, int y, int z, boolean notInterior) {
        selected = Blocks.BRICKS.defaultBlockState();
    }
}
