package com.hbm_m.block.machines.rbmk;

import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.machines.rbmk.RBMKCoolerBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class RBMKCoolerBlock extends RBMKColumnBlock {

    public RBMKCoolerBlock(Properties props) { super(props); }

    @Nullable @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new RBMKCoolerBlockEntity(pos, state);
    }

    @Nullable @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ModBlockEntities.RBMK_COOLER_BE.get(), RBMKCoolerBlockEntity::tick);
    }

    //? if >1.20.1 {
    /*public static final com.mojang.serialization.MapCodec<RBMKCoolerBlock> CODEC = simpleCodec(RBMKCoolerBlock::new);

    @Override
    protected com.mojang.serialization.MapCodec<? extends net.minecraft.world.level.block.BaseEntityBlock> codec() {
        return CODEC;
    }
    *///?}
}
