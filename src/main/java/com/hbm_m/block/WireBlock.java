package com.hbm_m.block;

import com.hbm_m.api.energy.IEnergyConnector;
import com.hbm_m.block.entity.WireBlockEntity;
import com.hbm_m.capability.ModCapabilities;
import com.hbm_m.api.energy.EnergyNetworkManager;
import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.common.util.LazyOptional;
import org.slf4j.Logger;

import javax.annotation.Nullable;

public class WireBlock extends BaseEntityBlock {
    private static final Logger LOGGER = LogUtils.getLogger();

    public static final BooleanProperty NORTH = BlockStateProperties.NORTH;
    public static final BooleanProperty EAST = BlockStateProperties.EAST;
    public static final BooleanProperty SOUTH = BlockStateProperties.SOUTH;
    public static final BooleanProperty WEST = BlockStateProperties.WEST;
    public static final BooleanProperty UP = BlockStateProperties.UP;
    public static final BooleanProperty DOWN = BlockStateProperties.DOWN;

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
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.getConnectionState(context.getLevel(), context.getClickedPos());
    }

    @Override
    public BlockState updateShape(BlockState state, Direction facing, BlockState facingState,
                                  LevelAccessor level, BlockPos currentPos, BlockPos facingPos) {
        BooleanProperty property = getProperty(facing);
        boolean canConnect = canVisuallyConnectTo(level, facingPos, facing.getOpposite());
        return state.setValue(property, canConnect);
    }

    private BlockState getConnectionState(LevelAccessor level, BlockPos pos) {
        return this.defaultBlockState()
                .setValue(DOWN,  canVisuallyConnectTo(level, pos.relative(Direction.DOWN),  Direction.UP))
                .setValue(UP,    canVisuallyConnectTo(level, pos.relative(Direction.UP),    Direction.DOWN))
                .setValue(NORTH, canVisuallyConnectTo(level, pos.relative(Direction.NORTH), Direction.SOUTH))
                .setValue(SOUTH, canVisuallyConnectTo(level, pos.relative(Direction.SOUTH), Direction.NORTH))
                .setValue(WEST,  canVisuallyConnectTo(level, pos.relative(Direction.WEST),  Direction.EAST))
                .setValue(EAST,  canVisuallyConnectTo(level, pos.relative(Direction.EAST),  Direction.WEST));
    }

    private boolean canVisuallyConnectTo(LevelAccessor world, BlockPos neighborPos, Direction sideFromNeighbor) {
        BlockEntity be = world.getBlockEntity(neighborPos);
        if (be == null) return false;

        LazyOptional<IEnergyConnector> hbmCap = be.getCapability(ModCapabilities.HBM_ENERGY_CONNECTOR, sideFromNeighbor);
        if (hbmCap.isPresent()) {
            return hbmCap.resolve().map(connector -> connector.canConnectEnergy(sideFromNeighbor)).orElse(false);
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
            // УБИРАЕМ: level.scheduleTick(pos, this, 1);
            // ДОБАВЛЯЕМ СРАЗУ:
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