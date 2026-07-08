package com.hbm_m.blockentity.machines;

import com.hbm_m.blockentity.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class SU47TrophyBlockEntity extends BlockEntity {
    public SU47TrophyBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SU47_TROPHY_BE.get(), pos, state);
    }
}
