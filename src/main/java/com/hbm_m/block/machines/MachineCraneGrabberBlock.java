package com.hbm_m.block.machines;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.block.network.CraneBaseBlock;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.network.MachineCraneGrabberBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/** 1:1 {@code CraneGrabber}: greift Teile vom Band am Eingang und gibt sie am Ausgang weiter. */
public class MachineCraneGrabberBlock extends CraneBaseBlock {

    public MachineCraneGrabberBlock(Properties properties) {
        super(properties);
    }

    @Nullable @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new MachineCraneGrabberBlockEntity(pos, state); }

    @Nullable @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, ModBlockEntities.CRANE_GRABBER_BE.get(), MachineCraneGrabberBlockEntity::tick);
    }

    @Override protected int[] getDropRange() { return new int[] { 9, 11 }; }

    //? if >1.20.1 {
    /*public static final com.mojang.serialization.MapCodec<MachineCraneGrabberBlock> CODEC = simpleCodec(MachineCraneGrabberBlock::new);
    @Override protected com.mojang.serialization.MapCodec<? extends net.minecraft.world.level.block.BaseEntityBlock> codec() { return CODEC; }
    *///?}
}
