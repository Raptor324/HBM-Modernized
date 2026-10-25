package com.hbm_m.block.generic;

import javax.annotation.Nullable;

import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.deco.VentBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/** 1:1 {@code BlockVent} ({@code vent_chlorine}, {@code vent_cloud}, {@code vent_pink_cloud}): stoesst unter Redstonestrom Gaswolken aus, laesst nichts fallen. */
public class VentBlock extends BaseEntityBlock {

    public VentBlock(Properties p) { super(p); }

    @Override public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }

    @Nullable @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new VentBlockEntity(pos, state); }

    @Nullable @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, ModBlockEntities.VENT.get(), VentBlockEntity::serverTick);
    }

    //? if >1.20.1 {
    /*public static final com.mojang.serialization.MapCodec<VentBlock> CODEC = simpleCodec(VentBlock::new);
    @Override protected com.mojang.serialization.MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }
    *///?}
}
