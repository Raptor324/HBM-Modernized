package com.hbm_m.world.gen.nbt.selector;

import net.minecraft.util.RandomSource;

/** 1:1 {@code BiomeTopSelector}: Oberflaechenblock des Bioms. */
public class BiomeTopSelector extends BiomeBlockSelector {

    @Override
    public void selectBlocks(RandomSource rand, int x, int y, int z, boolean notInterior) {
        selected = nextBiome.topBlock();
    }
}
