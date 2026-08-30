package com.hbm_m.block.gas;

import com.hbm_m.extprop.HbmLivingProps;
import com.hbm_m.handler.ArmorRegistry;
import com.hbm_m.handler.HazardClass;

import net.minecraft.core.BlockPos;
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
    public void randomTick(BlockState state, net.minecraft.server.level.ServerLevel level, BlockPos pos, RandomSource random) {
        // 20% — рассеивается; иначе 1/5 — оседает вниз (как в оригинале).
        if (random.nextInt(5) == 0) {
            level.removeBlock(pos, false);
        } else if (random.nextInt(5) == 0) {
            BlockPos below = pos.below();
            if (level.getBlockState(below).isAir()) {
                level.removeBlock(pos, false);
                level.setBlock(below, state, 3);
            }
        }
    }
}
