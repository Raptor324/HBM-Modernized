package com.hbm_m.block.machines;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.block.entity.machines.FluidValveBlockEntity;
import com.hbm_m.interfaces.IItemFluidIdentifier;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Жидкостный клапан.
 * Открыт по умолчанию; при сигнале редстоуна закрывается (разрывает граф).
 * Тип жидкости задаётся fluid-идентификатором.
 */
public class FluidValveBlock extends BaseEntityBlock {

    public FluidValveBlock(Properties props) {
        super(props);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new FluidValveBlockEntity(pos, state);
    }

    @Override
    public @Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level, BlockState state, BlockEntityType<T> type) {
        return null; // Клапан не тикает — только реагирует на neighborChanged
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos,
                                Block block, BlockPos fromPos, boolean isMoving) {
        super.neighborChanged(state, level, pos, block, fromPos, isMoving);
        if (!level.isClientSide) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof FluidValveBlockEntity valve) {
                valve.updateRedstone(level, pos);
            }
        }
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos,
                                 Player player, InteractionHand hand, BlockHitResult hit) {
        var stack = player.getItemInHand(hand);
        if (!stack.isEmpty() && stack.getItem() instanceof IItemFluidIdentifier idItem) {
            if (!level.isClientSide) {
                Fluid fluid = idItem.getType(level, pos, stack);
                if (fluid != null) {
                    if (fluid == com.hbm_m.inventory.fluid.ModFluids.NONE.getSource()) fluid = Fluids.EMPTY;
                    BlockEntity be = level.getBlockEntity(pos);
                    if (be instanceof FluidValveBlockEntity valve) {
                        valve.setFluidType(fluid);
                    }
                }
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        return InteractionResult.PASS;
    }
}
