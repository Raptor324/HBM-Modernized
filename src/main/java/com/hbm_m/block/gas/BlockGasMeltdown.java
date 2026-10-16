package com.hbm_m.block.gas;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.effect.ModEffects;
import com.hbm_m.extprop.HbmLivingProps;
import com.hbm_m.handler.ArmorRegistry;
import com.hbm_m.handler.HazardClass;
import com.hbm_m.platform.PlatformHooks;
import com.hbm_m.radiation.ChunkRadiationManager;
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
import net.minecraft.world.level.block.state.BlockState;

/**
 * Газ расплава: радиационное заражение и асбестоз лёгких; расползается в
 * gas_radon_dense по соседнему воздуху, при видимом небе качает радиацию в чанк.
 * Порт {@link com.hbm.blocks.gas.BlockGasMeltdown} (1.7.10).
 */
public class BlockGasMeltdown extends BlockGasBase {

    public BlockGasMeltdown() {
        super();
    }

    @Override
    protected void affect(LivingEntity living) {
        // 1.7.10: ContaminationUtil.contaminate RADIATION CREATIVE 0.5F — всегда, в обход маски
        ContaminationUtil.contaminate(living, HazardType.RADIATION, ContaminationType.CREATIVE, 0.5F);
        // 1.7.10: PotionEffect(HbmPotion.radiation, 60 * 20, 2)
        PlatformHooks.addEffect(living, ModEffects.RADIATION, 60 * 20, 2);

        if (ArmorRegistry.hasProtection(living, 3, HazardClass.PARTICLE_FINE)) {
            damageWornFilter(living);
        } else {
            HbmLivingProps.incrementAsbestos(living, 5);
        }
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        // 1.7.10: 1/7 — случайный сосед-воздух становится gas_radon_dense
        Direction dir = Direction.getRandom(random);
        BlockPos neighbor = pos.relative(dir);
        if (random.nextInt(7) == 0 && level.getBlockState(neighbor).isAir()) {
            level.setBlockAndUpdate(neighbor, ModBlocks.GAS_RADON_DENSE.get().defaultBlockState());
        }

        // 1.7.10: при видимом небе — +5 RAD в чанк
        if (level.canSeeSky(pos)) {
            ChunkRadiationManager.incrementRad(level, pos.getX(), pos.getY(), pos.getZ(), 5F);
        }

        // 1/350 рассеивание
        if (random.nextInt(350) == 0) {
            level.removeBlock(pos, false);
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
        if (random.nextInt(2) == 0) {
            return Direction.UP;
        }
        return Direction.DOWN;
    }

    @Override
    public Direction getSecondDirection(Level level, BlockPos pos, RandomSource random) {
        return randomHorizontal(random);
    }
}
