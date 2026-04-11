package com.hbm_m.block.machines;

import javax.annotation.Nullable;

import com.hbm_m.block.entity.machines.FluidPumpBlockEntity;
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
 * Жидкостный насос.
 * Принимает жидкость с входной стороны и выталкивает на выходную.
 * Редстоун отключает выход.
 * Fluid identifier задаёт тип жидкости.
 */
public class FluidPumpBlock extends BaseEntityBlock {

    public FluidPumpBlock(Properties props) {
        super(props);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new FluidPumpBlockEntity(pos, state);
    }

    @Override
    public @Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : (lvl, pos, st, be) -> {
            if (be instanceof FluidPumpBlockEntity pump) {
                FluidPumpBlockEntity.tick(lvl, pos, st, pump);
            }
        };
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos,
                                 Player player, InteractionHand hand, BlockHitResult hit) {
        var stack = player.getItemInHand(hand);
        if (!stack.isEmpty() && stack.getItem() instanceof IItemFluidIdentifier idItem) {
            if (!level.isClientSide) {
                Fluid fluid = idItem.getType(level, pos, stack);
                if (fluid != null) {
                    if (fluid == com.hbm_m.api.fluids.ModFluids.NONE.getSource()) fluid = Fluids.EMPTY;
                    BlockEntity be = level.getBlockEntity(pos);
                    if (be instanceof FluidPumpBlockEntity pump) {
                        pump.setFluidType(fluid);
                    }
                }
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        return InteractionResult.PASS;
    }

    /**
     * При размещении задаём facing блока от игрока.
     * Направление «вперёд» = horizontal direction от игрока (куда смотрит).
     */
    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos,
                                Block block, BlockPos fromPos, boolean isMoving) {
        super.neighborChanged(state, level, pos, block, fromPos, isMoving);
        // Насос реагирует на изменение ред.камня в своём tick, не здесь
    }
}
