package com.hbm_m.api.energy;

import java.util.Map;

import org.jetbrains.annotations.Nullable;

import org.slf4j.Logger;

import com.google.common.collect.ImmutableMap;
import com.hbm_m.block.machines.MachineBatteryBlock;
import com.hbm_m.block.machines.MachineBatterySocketBlock;
import com.hbm_m.capability.ModCapabilities;
import com.hbm_m.interfaces.IEnergyConnector;
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
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
//? if forge {
/*import com.hbm_m.capability.ModCapabilities;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
*///?}

//? if fabric {
import com.hbm_m.capability.ModCapabilities;
//?}

public class WireBlock extends BaseEntityBlock {
    private static final Logger LOGGER = LogUtils.getLogger();

    public static final BooleanProperty NORTH = BlockStateProperties.NORTH;
    public static final BooleanProperty EAST = BlockStateProperties.EAST;
    public static final BooleanProperty SOUTH = BlockStateProperties.SOUTH;
    public static final BooleanProperty WEST = BlockStateProperties.WEST;
    public static final BooleanProperty UP = BlockStateProperties.UP;
    public static final BooleanProperty DOWN = BlockStateProperties.DOWN;

    public static final EnumProperty<WireCenterVisual> CENTER =
            EnumProperty.create("center", WireCenterVisual.class);

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
                .setValue(WEST, false).setValue(UP, false).setValue(DOWN, false)
                .setValue(CENTER, WireCenterVisual.JUNCTION));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(NORTH, EAST, SOUTH, WEST, UP, DOWN, CENTER);
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
        // Пересчитываем все грани и center (прямой сегмент vs контакт), иначе center устаревает.
        return getConnectionState(level, currentPos);
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
        boolean down = canVisuallyConnectTo(level, pos.relative(Direction.DOWN), Direction.UP, level.getBlockState(pos.relative(Direction.DOWN)));
        boolean up = canVisuallyConnectTo(level, pos.relative(Direction.UP), Direction.DOWN, level.getBlockState(pos.relative(Direction.UP)));
        boolean north = canVisuallyConnectTo(level, pos.relative(Direction.NORTH), Direction.SOUTH, level.getBlockState(pos.relative(Direction.NORTH)));
        boolean south = canVisuallyConnectTo(level, pos.relative(Direction.SOUTH), Direction.NORTH, level.getBlockState(pos.relative(Direction.SOUTH)));
        boolean west = canVisuallyConnectTo(level, pos.relative(Direction.WEST), Direction.EAST, level.getBlockState(pos.relative(Direction.WEST)));
        boolean east = canVisuallyConnectTo(level, pos.relative(Direction.EAST), Direction.WEST, level.getBlockState(pos.relative(Direction.EAST)));
        WireCenterVisual center = computeCenterVisual(north, south, east, west, up, down);
        return this.defaultBlockState()
                .setValue(DOWN, down)
                .setValue(UP, up)
                .setValue(NORTH, north)
                .setValue(SOUTH, south)
                .setValue(WEST, west)
                .setValue(EAST, east)
                .setValue(CENTER, center);
    }

    private static WireCenterVisual computeCenterVisual(boolean north, boolean south, boolean east, boolean west, boolean up, boolean down) {
        if (north && south && !east && !west && !up && !down) {
            return WireCenterVisual.STRAIGHT_Z;
        }
        if (east && west && !north && !south && !up && !down) {
            return WireCenterVisual.STRAIGHT_X;
        }
        if (up && down && !north && !south && !east && !west) {
            return WireCenterVisual.STRAIGHT_Y;
        }
        return WireCenterVisual.JUNCTION;
    }

    private boolean canVisuallyConnectTo(LevelAccessor world, BlockPos neighborPos, Direction sideFromNeighbor, BlockState neighborState) {

        if (neighborState.is(this)) {
            return true;
        }

        Block block = neighborState.getBlock();
        if (block instanceof SwitchBlock || block instanceof MachineBatteryBlock || block instanceof MachineBatterySocketBlock) {
            return true;
        }

        BlockEntity be = world.getBlockEntity(neighborPos);
        if (be == null) {
            return false;
        }

        if (be instanceof IEnergyConnector connector) {
            return connector.canConnectEnergy(sideFromNeighbor);
        }

        //? if forge {
        /*// Forge: дополнительная проверка через Capability для совместимости со сторонними модами
        if (be.getCapability(ModCapabilities.HBM_ENERGY_CONNECTOR, sideFromNeighbor).isPresent()) return true;
        if (be.getCapability(ModCapabilities.HBM_ENERGY_PROVIDER, sideFromNeighbor).isPresent()) return true;
        if (be.getCapability(ModCapabilities.HBM_ENERGY_RECEIVER, sideFromNeighbor).isPresent()) return true;
        return be.getCapability(ForgeCapabilities.ENERGY, sideFromNeighbor).isPresent();
        *///?}

        //? if fabric {
        // Fabric: проверяем через cardinal-components
        return ModCapabilities.hasEnergyComponent(be);
        //?}
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