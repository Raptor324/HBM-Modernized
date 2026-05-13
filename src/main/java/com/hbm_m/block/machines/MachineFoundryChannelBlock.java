package com.hbm_m.block.machines;

import com.google.common.collect.ImmutableMap;
import com.hbm_m.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.Map;

public class MachineFoundryChannelBlock extends Block {

    public static final BooleanProperty NORTH = BlockStateProperties.NORTH;
    public static final BooleanProperty EAST = BlockStateProperties.EAST;
    public static final BooleanProperty SOUTH = BlockStateProperties.SOUTH;
    public static final BooleanProperty WEST = BlockStateProperties.WEST;

    private static final Map<Direction, BooleanProperty> PROPERTY_BY_DIRECTION = ImmutableMap.of(
            Direction.NORTH, NORTH,
            Direction.EAST, EAST,
            Direction.SOUTH, SOUTH,
            Direction.WEST, WEST
    );

    private static final VoxelShape CENTER = Block.box(5.0D, 0.0D, 5.0D, 11.0D, 2.0D, 11.0D);
    private static final Map<Direction, VoxelShape> CONNECTED_SHAPES = ImmutableMap.of(
            Direction.EAST, Shapes.or(
                    Block.box(10.0D, 0.0D, 5.0D, 16.0D, 2.0D, 11.0D),
                    Block.box(10.0D, 0.0D, 5.0D, 16.0D, 8.0D, 6.0D),
                    Block.box(10.0D, 0.0D, 10.0D, 16.0D, 8.0D, 11.0D)
            ),
            Direction.WEST, Shapes.or(
                    Block.box(0.0D, 0.0D, 5.0D, 6.0D, 2.0D, 11.0D),
                    Block.box(0.0D, 0.0D, 5.0D, 6.0D, 8.0D, 6.0D),
                    Block.box(0.0D, 0.0D, 10.0D, 6.0D, 8.0D, 11.0D)
            ),
            Direction.SOUTH, Shapes.or(
                    Block.box(5.0D, 0.0D, 10.0D, 11.0D, 2.0D, 16.0D),
                    Block.box(5.0D, 0.0D, 10.0D, 6.0D, 8.0D, 16.0D),
                    Block.box(10.0D, 0.0D, 10.0D, 11.0D, 8.0D, 16.0D)
            ),
            Direction.NORTH, Shapes.or(
                    Block.box(5.0D, 0.0D, 0.0D, 11.0D, 2.0D, 6.0D),
                    Block.box(5.0D, 0.0D, 0.0D, 6.0D, 8.0D, 6.0D),
                    Block.box(10.0D, 0.0D, 0.0D, 11.0D, 8.0D, 6.0D)
            )
    );
    private static final Map<Direction, VoxelShape> CLOSED_SHAPES = ImmutableMap.of(
            Direction.EAST, Block.box(10.0D, 0.0D, 5.0D, 11.0D, 8.0D, 11.0D),
            Direction.WEST, Block.box(5.0D, 0.0D, 5.0D, 6.0D, 8.0D, 11.0D),
            Direction.SOUTH, Block.box(5.0D, 0.0D, 10.0D, 11.0D, 8.0D, 11.0D),
            Direction.NORTH, Block.box(5.0D, 0.0D, 5.0D, 11.0D, 8.0D, 6.0D)
    );

    public MachineFoundryChannelBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(NORTH, false)
                .setValue(EAST, false)
                .setValue(SOUTH, false)
                .setValue(WEST, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(NORTH, EAST, SOUTH, WEST);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return getConnectionState(context.getLevel(), context.getClickedPos());
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
                                  LevelAccessor level, BlockPos currentPos, BlockPos neighborPos) {
        if (direction.getAxis().isHorizontal()) {
            return getConnectionState(level, currentPos);
        }
        return state;
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos,
                                Block block, BlockPos fromPos, boolean isMoving) {
        super.neighborChanged(state, level, pos, block, fromPos, isMoving);
        BlockState next = getConnectionState(level, pos);
        if (!next.equals(state)) {
            level.setBlock(pos, next, Block.UPDATE_CLIENTS);
        }
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return buildShape(state);
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return buildShape(state);
    }

    @Override
    public VoxelShape getOcclusionShape(BlockState state, BlockGetter level, BlockPos pos) {
        return buildShape(state);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    public BlockState getConnectionState(LevelAccessor level, BlockPos pos) {
        return this.defaultBlockState()
                .setValue(NORTH, canConnectTo(level, pos.relative(Direction.NORTH)))
                .setValue(EAST, canConnectTo(level, pos.relative(Direction.EAST)))
                .setValue(SOUTH, canConnectTo(level, pos.relative(Direction.SOUTH)))
                .setValue(WEST, canConnectTo(level, pos.relative(Direction.WEST)));
    }

    private boolean canConnectTo(LevelAccessor level, BlockPos neighborPos) {
        Block neighbor = level.getBlockState(neighborPos).getBlock();
        return neighbor instanceof MachineFoundryChannelBlock
                || neighbor == ModBlocks.FOUNDRY_BASIN.get()
                || neighbor == ModBlocks.CRUCIBLE.get();
    }

    private VoxelShape buildShape(BlockState state) {
        VoxelShape shape = CENTER;
        for (Direction direction : PROPERTY_BY_DIRECTION.keySet()) {
            shape = Shapes.or(shape, state.getValue(PROPERTY_BY_DIRECTION.get(direction))
                    ? CONNECTED_SHAPES.get(direction)
                    : CLOSED_SHAPES.get(direction));
        }
        return shape;
    }
}