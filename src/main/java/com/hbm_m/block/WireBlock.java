package com.hbm_m.block;

// Блок провода для передачи энергии между машинами и устройствами.
// Провод может соединяться с другими проводами, с машинами и с блоками, у которых есть энергия (FE) capability.
// Форма провода динамически меняется в зависимости от того, с чем он соединён.
// Логика соединения реализована в методе canConnectTo().
import com.google.common.collect.ImmutableMap;
import com.hbm_m.block.entity.ModBlockEntities;
import com.hbm_m.block.entity.UniversalMachinePartBlockEntity;
import com.hbm_m.block.entity.WireBlockEntity;
import com.hbm_m.multiblock.PartRole;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.common.capabilities.ForgeCapabilities;

import java.util.Map;

import javax.annotation.Nonnull;

import org.jetbrains.annotations.Nullable;

public class WireBlock extends BaseEntityBlock {

    public static final BooleanProperty NORTH = BooleanProperty.create("north");
    public static final BooleanProperty SOUTH = BooleanProperty.create("south");
    public static final BooleanProperty WEST = BooleanProperty.create("west");
    public static final BooleanProperty EAST = BooleanProperty.create("east");
    public static final BooleanProperty UP = BooleanProperty.create("up");
    public static final BooleanProperty DOWN = BooleanProperty.create("down");

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
    
    public WireBlock(Properties pProperties) {
        super(pProperties);
        BlockState defaultState = this.stateDefinition.any();
        for (BooleanProperty property : PROPERTIES_MAP.values()) {
            defaultState = defaultState.setValue(property, false);
        }
        this.registerDefaultState(defaultState);
    }

    @Override
    public VoxelShape getShape(@Nonnull BlockState pState, @Nonnull BlockGetter pLevel, @Nonnull BlockPos pPos, @Nonnull CollisionContext pContext) {
        VoxelShape shape = CORE_SHAPE;
        for (Direction dir : Direction.values()) {
            if (pState.getValue(PROPERTIES_MAP.get(dir))) {
                shape = Shapes.or(shape, ARM_SHAPES.get(dir));
            }
        }
        return shape;
    }
    
    @Override
    public BlockState getStateForPlacement(@Nonnull BlockPlaceContext pContext) {
        BlockGetter level = pContext.getLevel();
        BlockPos pos = pContext.getClickedPos();
        BlockState state = this.defaultBlockState();
        for (Direction dir : Direction.values()) {
            state = state.setValue(PROPERTIES_MAP.get(dir), this.canConnectTo(level, pos, dir));
        }
        return state;
    }

    @Override
    public BlockState updateShape(@Nonnull BlockState pState, @Nonnull Direction pFacing, @Nonnull BlockState pFacingState, @Nonnull LevelAccessor pLevel, @Nonnull BlockPos pCurrentPos, @Nonnull BlockPos pFacingPos) {
        if (pLevel.isClientSide() && pFacingState.getBlock() instanceof UniversalMachinePartBlock) {
            if (pLevel.getBlockEntity(pCurrentPos) instanceof WireBlockEntity wire) {
                wire.scheduleRecheck();
            }
        }

        // на сервере уведомляем менеджер о том, что провод поменялся,
        // чтобы он инвалидировал/пересчитал соответствующие кэши.
        if (pLevel instanceof Level lvl && !lvl.isClientSide()) {
            com.hbm_m.energy.WireNetworkManager.get().onWireChanged(lvl, pCurrentPos);
        }

        boolean canConnect = this.canConnectTo(pLevel, pCurrentPos, pFacing);
        return pState.setValue(PROPERTIES_MAP.get(pFacing), canConnect);
    }
    
    /**
     * Логика проверки соединения.
     */
    public boolean canConnectTo(BlockGetter level, BlockPos pos, Direction direction) {
        BlockPos neighborPos = pos.relative(direction);
        BlockState neighborState = level.getBlockState(neighborPos);

        // 1. Подключаемся к другим проводам
        if (neighborState.is(this)) {
            return true;
        }

        BlockEntity be = level.getBlockEntity(neighborPos);
        if (be == null) {
            return false;
        }

        // если это наша фантомная часть — прочитать её роль напрямую (работает и на клиенте)
        if (be instanceof UniversalMachinePartBlockEntity partBe) {
            if (partBe.getPartRole() == PartRole.ENERGY_CONNECTOR) {
                return true;
            }
        }
        
        // 2. Для ВСЕ остальных блоков используем проверку через Forge Capabilities.
        return be.getCapability(ForgeCapabilities.ENERGY, direction.getOpposite()).isPresent();
    }

    @Override
    protected void createBlockStateDefinition(@Nonnull StateDefinition.Builder<Block, BlockState> pBuilder) {
        pBuilder.add(NORTH, SOUTH, WEST, EAST, UP, DOWN);
    }

    @Override
    public RenderShape getRenderShape(@Nonnull BlockState pState) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(@Nonnull BlockPos pPos, @Nonnull BlockState pState) {
        return new WireBlockEntity(pPos, pState);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(@Nonnull Level pLevel, @Nonnull BlockState pState, @Nonnull BlockEntityType<T> pBlockEntityType) {
        if (pLevel.isClientSide()) {
            return createTickerHelper(pBlockEntityType, ModBlockEntities.WIRE_BE.get(), WireBlockEntity::clientTick);
        }
        return null;
    }
}