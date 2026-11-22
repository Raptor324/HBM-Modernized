package com.hbm_m.block.gas;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.effect.ModEffects;
import com.hbm_m.extprop.HbmLivingProps;
import com.hbm_m.handler.ArmorRegistry;
import com.hbm_m.handler.HazardClass;
import com.hbm_m.platform.EffectHooks;
import com.hbm_m.util.ContaminationUtil;
import com.hbm_m.util.ContaminationUtil.ContaminationType;
import com.hbm_m.util.ContaminationUtil.HazardType;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 1:1-Port von {@code BlockGasRadonTomb} (1.7.10): das Gas aus dem Endlager. Es geht durch jeden
 * Schutz hindurch, entfernt heilende Effekte und toetet die Vegetation darunter ab.
 */
public class BlockGasRadonTomb extends BlockGasBase {

    public BlockGasRadonTomb() {
        super(0.1F, 0.3F, 0.1F);
    }

    @Override
    protected void affect(LivingEntity living) {
        ContaminationUtil.contaminate(living, HazardType.RADIATION, ContaminationType.CREATIVE, 0.5F);
        living.addEffect(new MobEffectInstance(EffectHooks.of(ModEffects.RADIATION), 60 * 20, 2));
        living.removeEffect(EffectHooks.of(ModEffects.RADAWAY));
        living.removeEffect(EffectHooks.of(ModEffects.RADX));

        if (ArmorRegistry.hasProtection(living, 3, HazardClass.PARTICLE_FINE)) {
            damageWornFilter(living);
        } else {
            HbmLivingProps.incrementAsbestos(living, 5);
        }
    }

    @Override
    protected boolean onGasTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (random.nextInt(10) == 0) {
            BlockPos below = pos.below();
            BlockState ground = level.getBlockState(below);

            if (ground.is(Blocks.GRASS_BLOCK)) {
                if (random.nextInt(5) == 0) {
                    level.setBlock(below, Blocks.COARSE_DIRT.defaultBlockState(), 3);
                } else {
                    level.setBlock(below, ModBlocks.WASTE_EARTH.get().defaultBlockState(), 3);
                }
            }

            if ((ground.is(BlockTags.LEAVES) || ground.getBlock() instanceof BushBlock)
                    && !ground.isCollisionShapeFullBlock(level, below)) {
                level.setBlock(below, Blocks.AIR.defaultBlockState(), 3);
            }
        }

        if (random.nextInt(50) == 0) {
            level.removeBlock(pos, false);
            return true;
        }

        return false;
    }

    @Override
    public Direction getFirstDirection(Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(3) == 0) return Direction.UP;
        return Direction.DOWN;
    }

    @Override
    public Direction getSecondDirection(Level level, BlockPos pos, RandomSource random) {
        return randomHorizontal(random);
    }
}
