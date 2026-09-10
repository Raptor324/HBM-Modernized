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
 * 1:1-Port von {@code BlockGasAsbestos} (1.7.10): Asbeststaub in der Luft, +1 Asbest je Tick
 * Kontakt. Sinkt bevorzugt nach unten, loest sich mit 1/50 je Tick auf.
 */
public class BlockGasAsbestos extends BlockGasBase {

    public BlockGasAsbestos() {
        super(0.6F, 0.6F, 0.5F);
    }

    @Override
    protected void affect(LivingEntity living) {
        if (!ArmorRegistry.hasProtection(living, 3, HazardClass.PARTICLE_FINE)) {
            HbmLivingProps.incrementAsbestos(living, 1);
        } else {
            damageWornFilter(living);
        }
    }

    @Override
    public Direction getFirstDirection(ServerLevel level, BlockPos pos, RandomSource random) {
        if (random.nextInt(5) == 0) return Direction.DOWN;
        return randomAny(random);
    }

    @Override
    public Direction getSecondDirection(ServerLevel level, BlockPos pos, RandomSource random) {
        return randomHorizontal(random);
    }

    @Override
    protected int getDecayChance() {
        return 50;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(5) == 0) {
            level.addParticle(ParticleTypes.MYCELIUM,
                    pos.getX() + random.nextFloat(), pos.getY() + random.nextFloat(), pos.getZ() + random.nextFloat(),
                    0.0D, 0.0D, 0.0D);
        }
    }
}
