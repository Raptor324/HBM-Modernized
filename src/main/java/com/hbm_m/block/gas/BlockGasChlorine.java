package com.hbm_m.block.gas;

import com.hbm_m.handler.ArmorRegistry;
import com.hbm_m.handler.HazardClass;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Хлор: слепота, отравление, иссушение и замедление без защиты органов дыхания.
 * Защищённый фильтр изнашивается.
 * Порт {@link com.hbm.blocks.gas.BlockGasClorine} (1.7.10).
 */
public class BlockGasChlorine extends BlockGasBase {

    public BlockGasChlorine() {
        super();
    }

    @Override
    protected void affect(LivingEntity living) {
        if (ArmorRegistry.hasProtection(living, 3, HazardClass.GAS_LUNG)) {
            damageWornFilter(living);
            return;
        }
        living.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 5 * 20, 0));
        living.addEffect(new MobEffectInstance(MobEffects.POISON, 20 * 20, 2));
        living.addEffect(new MobEffectInstance(MobEffects.WITHER, 1 * 20, 1));
        living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30 * 20, 1));
        living.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 30 * 20, 2));
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (random.nextInt(10) == 0) {
            level.removeBlock(pos, false);
            return;
        }
        super.tick(state, level, pos, random);
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