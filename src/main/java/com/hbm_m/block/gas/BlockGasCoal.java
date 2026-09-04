package com.hbm_m.block.gas;

import com.hbm_m.extprop.HbmLivingProps;
import com.hbm_m.handler.ArmorRegistry;
import com.hbm_m.handler.HazardClass;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Угольная пыль во взвешенном состоянии: +10 black lung за тик контакта.
 * Защищённый фильтр изнашивается. Газ медленно оседает вниз.
 * Порт {@link com.hbm.blocks.gas.BlockGasCoal} (1.7.10).
 */
public class BlockGasCoal extends BlockGasBase {

    public BlockGasCoal() {
        super();
    }

    @Override
    protected void affect(LivingEntity living) {
        if (!ArmorRegistry.hasProtection(living, 3, HazardClass.PARTICLE_COARSE)) {
            HbmLivingProps.incrementBlackLung(living, 10);
        } else {
            damageWornFilter(living);
        }
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (random.nextInt(20) == 0) {
            level.removeBlock(pos, false);
            return;
        }
        super.tick(state, level, pos, random);
    }

    @Override
    protected void spawnAmbientParticles(BlockState state, Level level, BlockPos pos, RandomSource random) {
        level.addParticle(
                ParticleTypes.SMOKE,
                pos.getX() + random.nextDouble(),
                pos.getY() + random.nextDouble(),
                pos.getZ() + random.nextDouble(),
                0.0D, 0.0D, 0.0D
        );
    }

    @Override
    public Direction getFirstDirection(Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(5) == 0) {
            return Direction.DOWN;
        }
        return Direction.getRandom(random);
    }

    @Override
    public Direction getSecondDirection(Level level, BlockPos pos, RandomSource random) {
        return randomHorizontal(random);
    }
}