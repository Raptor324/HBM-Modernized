package com.hbm_m.world.gen.nbt.selector;

import com.hbm_m.world.gen.LegacyBiome;
import com.hbm_m.world.gen.nbt.BlockSelector;

/** 1:1 {@code BiomeBlockSelector}: Selector, der das Biom des aktuellen Bauabschnitts kennt. */
public abstract class BiomeBlockSelector extends BlockSelector {

    public LegacyBiome nextBiome;
}
