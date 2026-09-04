package com.hbm_m.block.gas;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.effect.ModEffects;
import com.hbm_m.extprop.HbmLivingProps;
import com.hbm_m.platform.PlatformHooks;
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
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Плотный радон: сильное заражение + эффект лучевой болезни; оседает вниз,
 * трава под ним выгорает в waste_earth, при испарении оставляет fallout.
 * Порт {@link com.hbm.blocks.gas.BlockGasRadonDense} (1.7.10).
 */
public class BlockGasRadonDense extends BlockGasBase {

    public BlockGasRadonDense() {
        super();
    }

    @Override
    protected void affect(LivingEntity living) {
        if (ArmorRegistry.hasProtection(living, 3, HazardClass.PARTICLE_FINE)) {
            damageWornFilter(living);
        } else {
            // 1.7.10: ContaminationUtil.contaminate RADIATION CREATIVE 0.5F
            ContaminationUtil.contaminate(living, HazardType.RADIATION, ContaminationType.CREATIVE, 0.5F);
            // 1.7.10: PotionEffect(HbmPotion.radiation, 15 * 20, 0)
            PlatformHooks.addEffect(living, ModEffects.RADIATION, 15 * 20, 0);
            HbmLivingProps.incrementAsbestos(living, 5);
        }
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        // 1.7.10: 1/20 — трава снизу → waste_earth
        if (random.nextInt(20) == 0) {
            BlockPos below = pos.below();
            if (level.getBlockState(below).is(Blocks.GRASS_BLOCK)) {
                level.setBlockAndUpdate(below, ModBlocks.WASTE_EARTH.get().defaultBlockState());
            }
        }

        // 1.7.10: 1/30 — испарение; на освободившемся месте остаётся fallout (если выживает)
        if (random.nextInt(30) == 0) {
            BlockState fallout = ModBlocks.NUCLEAR_FALLOUT.get().defaultBlockState();
            if (fallout.canSurvive(level, pos)) {
                level.setBlockAndUpdate(pos, fallout);
            } else {
                level.removeBlock(pos, false);
            }
            return;
        }

        super.tick(state, level, pos, random);
    }

    @Override
    protected void spawnAmbientParticles(BlockState state, Level level, BlockPos pos, RandomSource random) {
        level.addParticle(
                ParticleTypes.MYCELIUM,
                pos.getX() + random.nextDouble(),
                pos.getY() + random.nextDouble(),
                pos.getZ() + random.nextDouble(),
                0.0D, 0.0D, 0.0D
        );
    }

    @Override
    public Direction getFirstDirection(Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(5) == 0) {
            return Direction.UP;
        }
        return Direction.DOWN;
    }

    @Override
    public Direction getSecondDirection(Level level, BlockPos pos, RandomSource random) {
        return randomHorizontal(random);
    }
}
