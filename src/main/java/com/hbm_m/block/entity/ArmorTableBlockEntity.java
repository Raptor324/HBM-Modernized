package com.hbm_m.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class ArmorTableBlockEntity extends BlockEntity {

    public ArmorTableBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ARMOR_TABLE_BE.get(), pos, state);
    }
}