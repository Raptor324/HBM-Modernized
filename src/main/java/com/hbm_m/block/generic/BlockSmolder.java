package com.hbm_m.block.generic;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 1:1 {@code BlockSmolder} ({@code ore_nether_smoldering}): wer darueber laeuft, brennt 3 s; ist oben Luft,
 * steigen Lava- und Flammenpartikel auf.
 */
public class BlockSmolder extends Block {

    public BlockSmolder(Properties props) {
        super(props);
    }

    @Override
    public void animateTick(BlockState state, Level world, BlockPos pos, RandomSource rand) {
        super.animateTick(state, world, pos, rand);

        if (world.getBlockState(pos.above()).isAir()) {
            int x = pos.getX(), y = pos.getY(), z = pos.getZ();
            world.addParticle(ParticleTypes.LAVA, x + 0.25 + rand.nextDouble() * 0.5, y + 1.1, z + 0.25 + rand.nextDouble() * 0.5, 0.0, 0.0, 0.0);
            world.addParticle(ParticleTypes.FLAME, x + 0.25 + rand.nextDouble() * 0.5, y + 1.1, z + 0.25 + rand.nextDouble() * 0.5, 0.0, 0.0, 0.0);
        }
    }

    @Override
    public void stepOn(Level world, BlockPos pos, BlockState state, Entity entity) {
        entity.setSecondsOnFire(3);
    }
}
