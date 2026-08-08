package com.hbm_m.block.machines;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.blockentity.machines.PWRPartBlockEntity;
import com.hbm_m.blockentity.machines.PWRPartBlockEntity.Kind;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Shared block class for every non-controller PWR structure block (fuel/control/channel/heatex/
 * heatsink/neutron_source/casing/reflector/port), parametrized by {@link Kind} - matches this
 * port's established "one shared block entity class per block family" convention (e.g.
 * {@code RBMKPanelBlock}). See {@link PWRPartBlockEntity} for why this differs from the
 * original's single generic {@code pwr_block} carrier.
 */
public class PWRPartBlock extends BaseEntityBlock {

    private final Kind kind;

    public PWRPartBlock(Kind kind, Properties properties) {
        super(properties);
        this.kind = kind;
    }

    public Kind getKind() {
        return kind;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PWRPartBlockEntity(pos, state, kind);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock()) && !level.isClientSide) {
            if (level.getBlockEntity(pos) instanceof PWRPartBlockEntity part) {
                part.notifyRemoved();
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }
}
