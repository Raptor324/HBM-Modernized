package com.hbm_m.api.energy;

import java.util.Map;

import javax.annotation.Nullable;

import org.slf4j.Logger;

import com.google.common.collect.ImmutableMap;
import com.hbm_m.capability.ModCapabilities;
import com.mojang.logging.LogUtils;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.context.BlockPlaceContext;
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
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.common.util.LazyOptional;

public class WireBlock extends BaseEntityBlock {
    private static final Logger LOGGER = LogUtils.getLogger();

    public static final BooleanProperty NORTH = BlockStateProperties.NORTH;
    public static final BooleanProperty EAST = BlockStateProperties.EAST;
    public static final BooleanProperty SOUTH = BlockStateProperties.SOUTH;
    public static final BooleanProperty WEST = BlockStateProperties.WEST;
    public static final BooleanProperty UP = BlockStateProperties.UP;
    public static final BooleanProperty DOWN = BlockStateProperties.DOWN;

    public static final Map<Direction, BooleanProperty> PROPERTIES_MAP =
            ImmutableMap.of(
                    Direction.NORTH, NORTH, Direction.SOUTH, SOUTH,
                    Direction.WEST, WEST, Direction.EAST, EAST,
                    Direction.UP, UP, Direction.DOWN, DOWN
            );

    private static final VoxelShape CORE_SHAPE = Block.box(5.5, 5.5, 5.5, 10.5, 10.5, 10.5);
    private static final Map<Direction, VoxelShape> ARM_SHAPES =
            ImmutableMap.of(
                    Direction.NORTH, Block.box(5.5, 5.5, 0, 10.5, 10.5, 5.5),
                    Direction.SOUTH, Block.box(5.5, 5.5, 10.5, 10.5, 10.5, 16),
                    Direction.WEST, Block.box(0, 5.5, 5.5, 5.5, 10.5, 10.5),
                    Direction.EAST, Block.box(10.5, 5.5, 5.5, 16, 10.5, 10.5),
                    Direction.UP, Block.box(5.5, 10.5, 5.5, 10.5, 16, 10.5),
                    Direction.DOWN, Block.box(5.5, 0, 5.5, 10.5, 5.5, 10.5)
            );

    public WireBlock(Properties properties) {
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
    public VoxelShape getShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
        VoxelShape shape = CORE_SHAPE;
        // Мы используем PROPERTIES_MAP, который добавили выше
        for (Direction dir : Direction.values()) {
            if (pState.getValue(PROPERTIES_MAP.get(dir))) {
                shape = Shapes.or(shape, ARM_SHAPES.get(dir));
            }
        }
        return shape;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {

        return this.getConnectionState(context.getLevel(), context.getClickedPos());
    }

    @Override
    public BlockState updateShape(BlockState state, Direction facing, BlockState facingState,
                                  LevelAccessor level, BlockPos currentPos, BlockPos facingPos) {
        BooleanProperty property = getProperty(facing);
        boolean canConnect = canVisuallyConnectTo(level, facingPos, facing.getOpposite(), facingState);
        return state.setValue(property, canConnect);
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean isMoving) {
        super.neighborChanged(state, level, pos, block, fromPos, isMoving);
        // Сосед изменился (например, часть мультиблока получила роль коннектора).
        // Пересчитываем соединения и обновляем визуал.
        BlockState newState = getConnectionState(level, pos);
        if (!newState.equals(state)) {
            level.setBlock(pos, newState, Block.UPDATE_CLIENTS);
        }
    }

    private BlockState getConnectionState(LevelAccessor level, BlockPos pos) {
        return this.defaultBlockState()
                .setValue(DOWN,  canVisuallyConnectTo(level, pos.relative(Direction.DOWN),  Direction.UP,    level.getBlockState(pos.relative(Direction.DOWN))))
                .setValue(UP,    canVisuallyConnectTo(level, pos.relative(Direction.UP),    Direction.DOWN,  level.getBlockState(pos.relative(Direction.UP))))
                .setValue(NORTH, canVisuallyConnectTo(level, pos.relative(Direction.NORTH), Direction.SOUTH, level.getBlockState(pos.relative(Direction.NORTH))))
                .setValue(SOUTH, canVisuallyConnectTo(level, pos.relative(Direction.SOUTH), Direction.NORTH, level.getBlockState(pos.relative(Direction.SOUTH))))
                .setValue(WEST,  canVisuallyConnectTo(level, pos.relative(Direction.WEST),  Direction.EAST,  level.getBlockState(pos.relative(Direction.WEST))))
                .setValue(EAST,  canVisuallyConnectTo(level, pos.relative(Direction.EAST),  Direction.WEST,  level.getBlockState(pos.relative(Direction.EAST))));
    }

    private boolean canVisuallyConnectTo(LevelAccessor world, BlockPos neighborPos, Direction sideFromNeighbor, BlockState neighborState) {

        if (neighborState.is(this)) {
            return true;
        }

        Block block = neighborState.getBlock();
        if (block instanceof SwitchBlock || block instanceof MachineBatteryBlock) {
            return true;
        }

        BlockEntity be = world.getBlockEntity(neighborPos);
        if (be == null) {
            return false;
        }

        LazyOptional<IEnergyConnector> hbmCap = be.getCapability(ModCapabilities.HBM_ENERGY_CONNECTOR, sideFromNeighbor);
        if (hbmCap.isPresent()) {
            return hbmCap.resolve().map(c -> c.canConnectEnergy(sideFromNeighbor)).orElse(false);
        }

        if (be.getCapability(ModCapabilities.HBM_ENERGY_PROVIDER, sideFromNeighbor).isPresent()) {
            return true;
        }

        if (be.getCapability(ModCapabilities.HBM_ENERGY_RECEIVER, sideFromNeighbor).isPresent()) {
            return true;
        }

        return be.getCapability(net.minecraftforge.common.capabilities.ForgeCapabilities.ENERGY, sideFromNeighbor).isPresent();
    }

    public static BooleanProperty getProperty(Direction direction) {
        return switch (direction) {
            case NORTH -> NORTH;
            case EAST -> EAST;
            case SOUTH -> SOUTH;
            case WEST -> WEST;
            case UP -> UP;
            case DOWN -> DOWN;
        };
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        if (!level.isClientSide && !oldState.is(this)) {
            LOGGER.info("[WIRE] Block placed at {}, adding to network immediately", pos);
            EnergyNetworkManager.get((ServerLevel) level).addNode(pos);
        }
        super.onPlace(state, level, pos, oldState, isMoving);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!level.isClientSide && !state.is(newState.getBlock())) {
            LOGGER.info("[WIRE] Block removed at {}, removing from network", pos);
            EnergyNetworkManager.get((ServerLevel) level).removeNode(pos);
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }



    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : (lvl, pos, st, be) -> {
            if (be instanceof WireBlockEntity wire) {
                WireBlockEntity.tick(lvl, pos, st, wire);
            }
        };
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new WireBlockEntity(pos, state);
    }
}