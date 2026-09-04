package com.hbm_m.block.gas;

import com.hbm_m.damagesource.ModDamageSources;
import com.hbm_m.handler.ArmorRegistry;
import com.hbm_m.handler.HazardClass;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Угарный газ: урон без защиты, износ фильтра у защищённых. Рассеивается редко (1/100).
 * Порт {@link com.hbm.blocks.gas.BlockGasMonoxide} (1.7.10).
 */
public class BlockGasMonoxide extends BlockGasBase {

    public BlockGasMonoxide() {
        super();
    }

    @Override
    protected void affect(LivingEntity living) {
        if (ArmorRegistry.hasProtection(living, 3, HazardClass.GAS_MONOXIDE)) {
            damageWornFilter(living);
        } else {
            living.hurt(ModDamageSources.monoxide(living.level()), 1.0F);
        }
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (random.nextInt(100) == 0) {
            level.removeBlock(pos, false);
            return;
        }
        super.tick(state, level, pos, random);
    }

    @Override
    public Direction getFirstDirection(Level level, BlockPos pos, RandomSource random) {
        return Direction.DOWN;
    }

    @Override
    public Direction getSecondDirection(Level level, BlockPos pos, RandomSource random) {
        return randomHorizontal(random);
    }
}