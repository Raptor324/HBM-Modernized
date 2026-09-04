package com.hbm_m.block.gas;

import com.hbm_m.extprop.HbmLivingProps;
import com.hbm_m.handler.ArmorRegistry;
import com.hbm_m.handler.HazardClass;
import com.hbm_m.util.ContaminationUtil;
import com.hbm_m.util.ContaminationUtil.ContaminationType;
import com.hbm_m.util.ContaminationUtil.HazardType;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Радон: слабое радиационное заражение; рассеивается 1/50.
 * Порт {@link com.hbm.blocks.gas.BlockGasRadon} (1.7.10).
 */
public class BlockGasRadon extends BlockGasBase {

    public BlockGasRadon() {
        super();
    }

    @Override
    protected void affect(LivingEntity living) {
        if (ArmorRegistry.hasProtection(living, 3, HazardClass.PARTICLE_FINE)) {
            damageWornFilter(living);
        } else {
            // 1.7.10: ContaminationUtil.contaminate RADIATION RAD_BYPASS 0.05F
            // (копит radEnv для Geiger-HUD, игнорирует броню).
            ContaminationUtil.contaminate(living, HazardType.RADIATION, ContaminationType.RAD_BYPASS, 0.05F);
            HbmLivingProps.incrementAsbestos(living, 1);
        }
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (random.nextInt(50) == 0) {
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