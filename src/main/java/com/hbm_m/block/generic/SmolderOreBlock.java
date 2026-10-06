package com.hbm_m.block.generic;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Порт {@code com.hbm.blocks.generic.BlockSmolder} (1.7.10): тлеющая руда.
 * Как в оригинале: брызги лавы и пламя над блоком, когда сверху воздух,
 * и поджигание сущностей, которые встали на руду.
 */
public class SmolderOreBlock extends Block {

    public SmolderOreBlock(Properties props) {
        super(props);
    }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        // Ориг. onEntityWalking: entity.setFire(3) — 3 секунды горения.
        entity.setRemainingFireTicks(Math.max(entity.getRemainingFireTicks(), 60));
        super.stepOn(level, pos, state, entity);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource rand) {
        if (!level.getBlockState(pos.above()).isAir()) return;
        double x = pos.getX() + rand.nextDouble();
        double y = pos.getY() + 1.0 + rand.nextDouble() * 0.2;
        double z = pos.getZ() + rand.nextDouble();
        level.addParticle(ParticleTypes.LAVA, x, y, z, 0.0, 0.0, 0.0);
        level.addParticle(ParticleTypes.FLAME, x, y, z, 0.0, 0.0, 0.0);
    }
}
