package com.hbm_m.block.generic;

import com.hbm_m.block.ModBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 1:1 {@link com.hbm.blocks.generic.BlockNuclearWaste} ({@code block_waste}, {@code block_waste_painted},
 * {@code block_waste_vitrified}): Gefahrenblock, der bei jedem Strahlungstick (alle 20 Ticks) mit 50 % dichtes Radon in ein freies Nachbarfeld abgibt.
 */
public class BlockNuclearWaste extends BlockHazard {

    public BlockNuclearWaste(Properties properties) {
        super(properties);
    }

    @Override
    public void tick(BlockState state, ServerLevel world, BlockPos pos, RandomSource rand) {
        emit(world, pos, rand);
        super.tick(state, world, pos, rand);
    }

    private void emit(ServerLevel world, BlockPos pos, RandomSource rand) {
        Direction dir = Direction.from3DDataValue(rand.nextInt(6));
        if (rand.nextInt(2) == 0 && world.getBlockState(pos.relative(dir)).isAir()) {
            world.setBlock(pos.relative(dir), ModBlocks.GAS_RADON_DENSE.get().defaultBlockState(), 3);
        }
    }
}
