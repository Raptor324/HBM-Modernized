package com.hbm_m.block.entity.machines.rbmk;

import com.hbm_m.block.entity.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class RBMKControlManualBlockEntity extends RBMKControlBlockEntity {

    public boolean moderated = false;

    public RBMKControlManualBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.RBMK_CONTROL_BE.get(), pos, state);
    }

    @Override
    public boolean isModerated() { return moderated; }

    public static void tick(Level level, BlockPos pos, BlockState state, RBMKControlManualBlockEntity be) {
        baseTick(level, pos, state, be);
        if (!level.isClientSide) {
            be.lastLevel = be.level;
            be.moveLevelToTarget(level);
        }
    }
}

