package com.hbm_m.block.machines;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.block.entity.ModBlockEntities;
import com.hbm_m.block.entity.machines.FluidPumpBlockEntity;
import com.hbm_m.interfaces.IItemFluidIdentifier;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
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

    /** Направление «вперёд» (к выходу); совпадает с {@link FluidPumpBlockEntity#getFacing()}. */
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    public FluidPumpBlock(Properties props) {
        super(props);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
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
        return level.isClientSide ? null
                : createTickerHelper(type, ModBlockEntities.FLUID_PUMP_BE.get(), FluidPumpBlockEntity::tick);
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
                    if (be instanceof FluidPumpBlockEntity pump) {
                        pump.setFluidType(fluid);
                    }
                }
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        return InteractionResult.PASS;
    }
}
