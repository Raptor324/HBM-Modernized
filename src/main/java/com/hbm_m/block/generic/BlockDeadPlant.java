package com.hbm_m.block.generic;

import com.hbm_m.block.ModBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 1:1 {@link com.hbm.blocks.generic.BlockDeadPlant} ({@code plant_dead}, Metadaten GENERIC/GRASS/FLOWER/BIGFLOWER/FERN
 * als eigene Bloecke {@code plant_dead} = GENERIC und {@code plant_dead_<art>}): kreuzfoermige tote Pflanze ohne
 * Kollision, nur auf Gras, Erde, Oedland-Erde, oeliger und toter Erde, faellt ohne Drop ab.
 */
public class BlockDeadPlant extends BushBlock {

    public BlockDeadPlant(Properties properties) {
        super(properties);
    }

    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter world, BlockPos pos) {
        Block block = state.getBlock();
        return block == Blocks.GRASS_BLOCK || block == Blocks.DIRT || block == ModBlocks.WASTE_EARTH.get()
                || block == ModBlocks.DIRT_OILY.get() || block == ModBlocks.DIRT_DEAD.get();
    }
}
