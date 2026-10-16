package com.hbm_m.block.machines.crates;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.blockentity.crates.TemplateCrateBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class TemplateCrateBlock extends BaseCrateBlock {

    public TemplateCrateBlock(Properties properties) {
        super(properties);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TemplateCrateBlockEntity(pos, state);
    }

    //? if >1.20.1 {
    /*public static final com.mojang.serialization.MapCodec<TemplateCrateBlock> CODEC = simpleCodec(TemplateCrateBlock::new);

    @Override
    protected com.mojang.serialization.MapCodec<? extends net.minecraft.world.level.block.BaseEntityBlock> codec() {
        return CODEC;
    }
    *///?}
}
