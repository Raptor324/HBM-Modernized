package com.hbm_m.block.machines;

import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.google.common.collect.ImmutableMap;
import com.hbm_m.block.entity.machines.FluidDuctBlockEntity;
import com.hbm_m.item.ModItems;
import com.hbm_m.item.liquids.FluidDuctItem;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.common.capabilities.ForgeCapabilities;

/**
 * Fluid Duct Block - A pipe block that transports a specific fluid type.
 * The fluid type is stored in the BlockEntity.
 * Connects visually and functionally to adjacent ducts (same fluid) and machines with IFluidHandler.
 */
public class FluidDuctBlock extends BaseEntityBlock {

    public static final BooleanProperty NORTH = BlockStateProperties.NORTH;
    public static final BooleanProperty EAST = BlockStateProperties.EAST;
    public static final BooleanProperty SOUTH = BlockStateProperties.SOUTH;
    public static final BooleanProperty WEST = BlockStateProperties.WEST;
    public static final BooleanProperty UP = BlockStateProperties.UP;
    public static final BooleanProperty DOWN = BlockStateProperties.DOWN;

    public static final Map<Direction, BooleanProperty> PROPERTY_BY_DIRECTION = ImmutableMap.of(
            Direction.NORTH, NORTH, Direction.SOUTH, SOUTH,
            Direction.WEST, WEST, Direction.EAST, EAST,
            Direction.UP, UP, Direction.DOWN, DOWN);

    // Core (center piece) + directional arms
    private static final VoxelShape CORE = Block.box(4, 4, 4, 12, 12, 12);
    private static final Map<Direction, VoxelShape> ARM_SHAPES = ImmutableMap.of(
            Direction.NORTH, Block.box(4, 4, 0, 12, 12, 4),
            Direction.SOUTH, Block.box(4, 4, 12, 12, 12, 16),
            Direction.WEST, Block.box(0, 4, 4, 4, 12, 12),
            Direction.EAST, Block.box(12, 4, 4, 16, 12, 12),
            Direction.UP, Block.box(4, 12, 4, 12, 16, 12),
            Direction.DOWN, Block.box(4, 0, 4, 12, 4, 12));

    public FluidDuctBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(NORTH, false).setValue(EAST, false).setValue(SOUTH, false)
                .setValue(WEST, false).setValue(UP, false).setValue(DOWN, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(NORTH, EAST, SOUTH, WEST, UP, DOWN);
    }

    @Override
    public VoxelShape getShape(@Nonnull BlockState state, @Nonnull BlockGetter level, @Nonnull BlockPos pos,
            @Nonnull CollisionContext context) {
        VoxelShape shape = CORE;
        for (Direction dir : Direction.values()) {
            if (state.getValue(PROPERTY_BY_DIRECTION.get(dir))) {
                shape = Shapes.or(shape, ARM_SHAPES.get(dir));
            }
        }
        return shape;
    }

    /** Compute the full connection state for a position whose BlockEntity is already in the world. */
    public BlockState getConnectionState(LevelAccessor level, BlockPos pos) {
        BlockState base = this.defaultBlockState();
        for (Direction dir : Direction.values()) {
            base = base.setValue(PROPERTY_BY_DIRECTION.get(dir),
                    canConnectTo(level, pos, pos.relative(dir), dir));
        }
        return base;
    }

    @Override
    public BlockState updateShape(@Nonnull BlockState state, @Nonnull Direction facing,
            @Nonnull BlockState facingState, @Nonnull LevelAccessor level,
            @Nonnull BlockPos currentPos, @Nonnull BlockPos facingPos) {
        return state.setValue(PROPERTY_BY_DIRECTION.get(facing),
                canConnectTo(level, currentPos, facingPos, facing));
    }

    @Override
    public void neighborChanged(@Nonnull BlockState state, @Nonnull Level level, @Nonnull BlockPos pos,
            @Nonnull Block block, @Nonnull BlockPos fromPos, boolean isMoving) {
        super.neighborChanged(state, level, pos, block, fromPos, isMoving);
        BlockState newState = getConnectionState(level, pos);
        if (!newState.equals(state)) {
            level.setBlock(pos, newState, Block.UPDATE_CLIENTS);
        }
    }

    private boolean canConnectTo(LevelAccessor level, BlockPos myPos, BlockPos neighborPos, Direction direction) {
        BlockState neighborState = level.getBlockState(neighborPos);

        // Connect to other fluid ducts with matching fluid type
        if (neighborState.getBlock() instanceof FluidDuctBlock) {
            BlockEntity myBe = level.getBlockEntity(myPos);
            BlockEntity neighborBe = level.getBlockEntity(neighborPos);
            if (myBe instanceof FluidDuctBlockEntity myDuct && neighborBe instanceof FluidDuctBlockEntity neighborDuct) {
                return myDuct.getFluidType() == neighborDuct.getFluidType();
            }
            // BEs may not be loaded yet during world gen — assume connectable
            return true;
        }

        // Connect to any block exposing IFluidHandler on the facing side
        BlockEntity be = level.getBlockEntity(neighborPos);
        if (be != null) {
            return be.getCapability(ForgeCapabilities.FLUID_HANDLER, direction.getOpposite()).isPresent();
        }
        return false;
    }

    // --- BlockEntity & Ticker ---

    @Nullable
    @Override
    public BlockEntity newBlockEntity(@Nonnull BlockPos pos, @Nonnull BlockState state) {
        return new FluidDuctBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(@Nonnull Level level,
            @Nonnull BlockState state, @Nonnull BlockEntityType<T> type) {
        return level.isClientSide ? null : (lvl, pos, st, be) -> {
            if (be instanceof FluidDuctBlockEntity duct) {
                FluidDuctBlockEntity.tick(lvl, pos, st, duct);
            }
        };
    }

    @Override
    public RenderShape getRenderShape(@Nonnull BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public List<ItemStack> getDrops(@Nonnull BlockState state, LootParams.Builder builder) {
        BlockEntity be = builder.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
        ItemStack drop = new ItemStack(ModItems.FLUID_DUCT.get());
        if (be instanceof FluidDuctBlockEntity ductBe
                && ductBe.getFluidType() != net.minecraft.world.level.material.Fluids.EMPTY) {
            FluidDuctItem.setFluidType(drop, ductBe.getFluidType());
        }
        return List.of(drop);
    }
}
