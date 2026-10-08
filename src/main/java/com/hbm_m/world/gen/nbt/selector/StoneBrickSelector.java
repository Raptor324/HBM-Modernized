package com.hbm_m.world.gen.nbt.selector;

import com.hbm_m.world.gen.nbt.BlockSelector;

import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;

/** 1:1 {@code StoneBrickSelector}: 20 % rissig, 30 % bemoost, sonst glatt. */
public class StoneBrickSelector extends BlockSelector {

    @Override
    public void selectBlocks(RandomSource rand, int x, int y, int z, boolean notInterior) {
        float f = rand.nextFloat();

        if (f < 0.2F) {
            selected = Blocks.CRACKED_STONE_BRICKS.defaultBlockState();
        } else if (f < 0.5F) {
            selected = Blocks.MOSSY_STONE_BRICKS.defaultBlockState();
        } else {
            selected = Blocks.STONE_BRICKS.defaultBlockState();
        }
    }
}
