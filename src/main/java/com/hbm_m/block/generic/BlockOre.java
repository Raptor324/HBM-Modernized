package com.hbm_m.block.generic;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Минимальный порт {@link com.hbm.blocks.generic.BlockOre} для waste trinitite.
 */
public class BlockOre extends Block {

    public BlockOre(Properties properties) {
        super(properties);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        level.addParticle(ParticleTypes.MYCELIUM,
                pos.getX() + random.nextFloat(),
                pos.getY() + 1.1F,
                pos.getZ() + random.nextFloat(),
                0.0D, 0.0D, 0.0D);
    }
}
