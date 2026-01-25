package com.hbm_m.block.custom.decorations;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;

public class DecoSteelBlock extends Block {

    public static final BooleanProperty CONNECTED_NORTH = BooleanProperty.create("connected_north");
    public static final BooleanProperty CONNECTED_SOUTH = BooleanProperty.create("connected_south");
    public static final BooleanProperty CONNECTED_EAST = BooleanProperty.create("connected_east");
    public static final BooleanProperty CONNECTED_WEST = BooleanProperty.create("connected_west");

    // Свойство для поворота боковой текстуры (направление, которым "смотрит" бок)
    public static final DirectionProperty SIDE_FACING = DirectionProperty.create("side_facing", Direction.Plane.HORIZONTAL);

    public DecoSteelBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(CONNECTED_NORTH, false)
                .setValue(CONNECTED_SOUTH, false)
                .setValue(CONNECTED_EAST, false)
                .setValue(CONNECTED_WEST, false)
                .setValue(SIDE_FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(CONNECTED_NORTH, CONNECTED_SOUTH, CONNECTED_EAST, CONNECTED_WEST, SIDE_FACING);
    }

    private boolean isSameBlock(BlockState other) {
        return other != null && other.getBlock() == this;
    }

    private boolean isSpecialMiddleBlock(BlockState state) {
        // TODO: Ваша логика для проверки блока "3"
        // Пример: return state.getBlock() == ModBlocks.specialBlock && state.get(...) == 3;
        return false;
    }

    private boolean checkMiddleBlock(LevelAccessor world, BlockPos pos1, BlockPos pos2) {
        // Находим середину между pos1 и pos2 для проверки блока "3"
        int mx = (pos1.getX() + pos2.getX()) / 2;
        int my = (pos1.getY() + pos2.getY()) / 2;
        int mz = (pos1.getZ() + pos2.getZ()) / 2;
        BlockPos middlePos = new BlockPos(mx, my, mz);
        BlockState middleState = world.getBlockState(middlePos);
        return middleState != null && isSpecialMiddleBlock(middleState);
    }

    private boolean isConnected(LevelAccessor world, BlockPos currentPos, BlockPos checkPos) {
        BlockState other = world.getBlockState(checkPos);
        if (isSameBlock(other)) return true;
        if (checkMiddleBlock(world, currentPos, checkPos)) {
            return isSameBlock(other);
        }
        return false;
    }

    private Direction determineSideFacing(BlockState state) {
        // Логика выбора направления для поворота боковой текстуры в зависимости от соединений
        // Пример по приоритету: если нет соединений, NORTH; если более 1 соединения — определяем по логике
        int connections = 0;
        if(state.getValue(CONNECTED_NORTH)) connections++;
        if(state.getValue(CONNECTED_EAST)) connections++;
        if(state.getValue(CONNECTED_SOUTH)) connections++;
        if(state.getValue(CONNECTED_WEST)) connections++;

        if (connections <= 1) {
            if(state.getValue(CONNECTED_NORTH)) return Direction.NORTH;
            if(state.getValue(CONNECTED_EAST)) return Direction.EAST;
            if(state.getValue(CONNECTED_SOUTH)) return Direction.SOUTH;
            if(state.getValue(CONNECTED_WEST)) return Direction.WEST;
            return Direction.NORTH; // default
        }

        // Когда соединений >= 2, можно задать логику как поворот к главному направлению
        // Например, при 2 соединениях выбрать вертикальную ось, при 3 — определённое направление

        // Заглушка для упрощения
        if(state.getValue(CONNECTED_NORTH)) return Direction.NORTH;
        return Direction.EAST;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockPos pos = context.getClickedPos();
        LevelAccessor level = context.getLevel();

        BlockState state = this.defaultBlockState()
                .setValue(CONNECTED_NORTH, isConnected(level, pos, pos.north()))
                .setValue(CONNECTED_SOUTH, isConnected(level, pos, pos.south()))
                .setValue(CONNECTED_EAST, isConnected(level, pos, pos.east()))
                .setValue(CONNECTED_WEST, isConnected(level, pos, pos.west()));

        return state.setValue(SIDE_FACING, determineSideFacing(state));
    }

    @Override
    public BlockState updateShape(BlockState state, Direction facing, BlockState facingState,
                                  LevelAccessor world, BlockPos currentPos, BlockPos facingPos) {
        boolean connected = isConnected(world, currentPos, facingPos);
        switch (facing) {
            case NORTH -> state = state.setValue(CONNECTED_NORTH, connected);
            case SOUTH -> state = state.setValue(CONNECTED_SOUTH, connected);
            case EAST -> state = state.setValue(CONNECTED_EAST, connected);
            case WEST -> state = state.setValue(CONNECTED_WEST, connected);
        }
        state = state.setValue(SIDE_FACING, determineSideFacing(state));
        return state;
    }
}
