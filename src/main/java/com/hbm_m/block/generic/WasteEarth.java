package com.hbm_m.block.generic;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Порт {@link com.hbm.blocks.generic.WasteEarth} — waste_mycelium и аналоги.
 */
public class WasteEarth extends Block {

    public WasteEarth(Properties properties) {
        super(properties);
    }

    @Override
    public boolean isRandomlyTicking(BlockState state) {
        return true;
    }

    @Override
    public void randomTick(BlockState state, net.minecraft.server.level.ServerLevel level, BlockPos pos, RandomSource random) {
        // Распространение mycelium отключено до GeneralConfig.enableMycelium
    }
}
