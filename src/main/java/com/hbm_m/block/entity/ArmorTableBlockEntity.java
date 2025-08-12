package com.hbm_m.block.entity;

import java.util.function.Supplier;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class ArmorTableBlockEntity extends BlockEntity {

    public static Supplier<BlockEntityType<ArmorTableBlockEntity>> TYPE_SUPPLIER;

    public ArmorTableBlockEntity(BlockPos pos, BlockState state) {
        super(TYPE_SUPPLIER.get(), pos, state);
    }
}