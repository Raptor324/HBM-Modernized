package com.hbm_m.block.machines;

import javax.annotation.Nullable;

import com.hbm_m.block.entity.machines.FluidExhaustBlockEntity;
import com.hbm_m.block.entity.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Выхлопная труба: поддерживает три типа дыма одновременно.
 * Всегда открыта, тип жидкости не настраивается — только smoke/smoke_leaded/smoke_poison.
 */
public class FluidExhaustBlock extends BaseEntityBlock {

    public FluidExhaustBlock(Properties props) {
        super(props);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new FluidExhaustBlockEntity(pos, state);
    }

    @Override
    public @Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : (lvl, pos, st, be) -> {
            if (be instanceof FluidExhaustBlockEntity exhaust) {
                FluidExhaustBlockEntity.tick(lvl, pos, st, exhaust);
            }
        };
    }
}
