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
 * 1:1-Port von {@code BlockGasCoal} (1.7.10): aufgewirbelter Kohlenstaub, +10 Schwarze Lunge je
 * Tick Kontakt. Loest sich mit 1/20 je Tick auf.
 *
 * <p>Einzige Ergaenzung: das Original nutzt bei Schutz den Filter nicht ab, hier tut es das - wie
 * bei allen anderen Gasen dieser Reihe.</p>
 */
public class BlockGasCoal extends BlockGasBase {

    public BlockGasCoal() {
        super(0.2F, 0.2F, 0.2F);
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
        return 20;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        level.addParticle(ParticleTypes.SMOKE,
                pos.getX() + random.nextFloat(), pos.getY() + random.nextFloat(), pos.getZ() + random.nextFloat(),
                0.0D, 0.0D, 0.0D);
    }
}
