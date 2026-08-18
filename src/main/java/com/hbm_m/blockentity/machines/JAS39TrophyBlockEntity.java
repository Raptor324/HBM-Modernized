package com.hbm_m.blockentity.machines;

import com.hbm_m.blockentity.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class JAS39TrophyBlockEntity extends com.hbm_m.blockentity.BaseHbmBlockEntity {
    public JAS39TrophyBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.JAS39_TROPHY_BE.get(), pos, state);
    }
}
