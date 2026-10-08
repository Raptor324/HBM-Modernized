package com.hbm_m.world.gen.nbt.selector;

import net.minecraft.util.RandomSource;

/** 1:1 {@code BiomeFillerSelector}: Fuellblock des Bioms. */
public class BiomeFillerSelector extends BiomeBlockSelector {

    @Override
    public void selectBlocks(RandomSource rand, int x, int y, int z, boolean notInterior) {
        selected = nextBiome.fillerBlock();
    }
}
