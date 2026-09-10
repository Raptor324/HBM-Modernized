package com.hbm_m.block.gas;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.extprop.HbmLivingProps;
import com.hbm_m.handler.ArmorRegistry;
import com.hbm_m.handler.HazardClass;
import com.hbm_m.util.ContaminationUtil;
import com.hbm_m.util.ContaminationUtil.ContaminationType;
import com.hbm_m.util.ContaminationUtil.HazardType;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 1:1-Port von {@code BlockGasRadonDense} (1.7.10): dichtes Radon aus einer Kernschmelze.
 * Verwandelt Gras darunter in verseuchte Erde und hinterlaesst beim Zerfall Fallout.
 *
 * <p><b>Abweichung:</b> Das Original setzt zusaetzlich den Trank {@code HbmPotion.radiation};
 * dieser Effekt ist im Port nicht vorhanden (nur Radaway und Taint sind portiert), die
 * Strahlungsdosis selbst wird aber wie im Original gesetzt.</p>
 */
public class BlockGasRadonDense extends BlockGasBase {

    public BlockGasRadonDense() {
        super(0.1F, 0.5F, 0.1F);
    }

    @Override
    protected void affect(LivingEntity living) {
        if (ArmorRegistry.hasProtection(living, 3, HazardClass.PARTICLE_FINE)) {
            damageWornFilter(living);
        } else {
            ContaminationUtil.contaminate(living, HazardType.RADIATION, ContaminationType.CREATIVE, 0.5F);
            HbmLivingProps.incrementAsbestos(living, 5);
        }
    }

    @Override
    protected boolean onGasTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {

        if (random.nextInt(20) == 0) {
            BlockPos below = pos.below();
            if (level.getBlockState(below).is(Blocks.GRASS_BLOCK)) {
                level.setBlock(below, ModBlocks.WASTE_EARTH.get().defaultBlockState(), 3);
            }
        }

        if (random.nextInt(30) == 0) {
            level.removeBlock(pos, false);
            BlockState fallout = ModBlocks.NUCLEAR_FALLOUT.get().defaultBlockState();
            if (fallout.canSurvive(level, pos)) {
                level.setBlock(pos, fallout, 3);
            }
            return true;
        }

        return false;
    }

    @Override
    public Direction getFirstDirection(ServerLevel level, BlockPos pos, RandomSource random) {
        if (random.nextInt(5) == 0) return Direction.UP;
        return Direction.DOWN;
    }

    @Override
    public Direction getSecondDirection(ServerLevel level, BlockPos pos, RandomSource random) {
        return randomHorizontal(random);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        level.addParticle(ParticleTypes.MYCELIUM,
                pos.getX() + random.nextFloat(), pos.getY() + random.nextFloat(), pos.getZ() + random.nextFloat(),
                0.0D, 0.0D, 0.0D);
    }
}
