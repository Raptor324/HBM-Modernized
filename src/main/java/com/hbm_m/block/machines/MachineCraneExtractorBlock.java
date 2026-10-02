package com.hbm_m.block.machines;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.block.network.CraneBaseBlock;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.network.MachineCraneExtractorBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/** 1:1 {@code CraneExtractor}: zieht aus dem Inventar am Ausgang und legt auf das Band am Eingang. */
public class MachineCraneExtractorBlock extends CraneBaseBlock {

    public MachineCraneExtractorBlock(Properties properties) {
        super(properties);
    }

    @Nullable @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new MachineCraneExtractorBlockEntity(pos, state); }

    @Nullable @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, ModBlockEntities.CRANE_EXTRACTOR_BE.get(), MachineCraneExtractorBlockEntity::tick);
    }

    /** Original: {@code canConnectRedstone} immer wahr. */
    @Override
    public boolean isSignalSource(BlockState state) { return false; }

    //? if forge {
    @Override
    public boolean canConnectRedstone(BlockState state, BlockGetter level, BlockPos pos, @Nullable Direction direction) { return true; }
    //?}

    @Override protected int[] getDropRange() { return new int[] { 9, 20 }; }

    //? if >1.20.1 {
    /*public static final com.mojang.serialization.MapCodec<MachineCraneExtractorBlock> CODEC = simpleCodec(MachineCraneExtractorBlock::new);
    @Override protected com.mojang.serialization.MapCodec<? extends net.minecraft.world.level.block.BaseEntityBlock> codec() { return CODEC; }
    *///?}
}
