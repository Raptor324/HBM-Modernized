package com.hbm_m.block.custom.machines;

import com.google.common.collect.ImmutableMap;
import com.hbm_m.block.ModBlocks;
import com.hbm_m.block.entity.ModBlockEntities;
import com.hbm_m.block.entity.custom.machines.MachineFluidTankBlockEntity;
import com.hbm_m.multiblock.IMultiblockController;
import com.hbm_m.multiblock.MultiblockStructureHelper;
import com.hbm_m.multiblock.PartRole;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.Lazy;
import net.minecraftforge.network.NetworkHooks;

import java.util.Map;
import java.util.function.Supplier;

import javax.annotation.Nonnull;
import org.jetbrains.annotations.Nullable;

/**
 * Мультиблочная цистерна 5 (ширина) x 3 (высота) x 3 (глубина).
 * Контроллер (основной блок) находится по центру нижнего ряда передней стороны.
 */
public class MachineFluidTankBlock extends BaseEntityBlock implements IMultiblockController {

    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    private static MultiblockStructureHelper STRUCTURE_HELPER;

    // --- КОЛЛИЗИЯ (VoxelShapes) ---
    static final Lazy<Map<Direction, VoxelShape>> SHAPES_LAZY = Lazy.of(() ->
            ImmutableMap.<Direction, VoxelShape>builder()
                    // NORTH (Z+): Широкая сторона смотрит на нас. X широкий, Z короткий.
                    .put(Direction.NORTH, Block.box(-32, 0, 0, 48, 48, 48))

                    // SOUTH (Z-): Аналогично, но сдвиг по Z назад.
                    .put(Direction.SOUTH, Block.box(-32, 0, -32, 48, 48, 16))

                    // WEST (X+): Теперь глубина становится шириной.
                    .put(Direction.WEST,  Block.box(0, 0, -32, 48, 48, 48))

                    // EAST (X-): Аналогично WEST, но сдвиг.
                    .put(Direction.EAST,  Block.box(-32, 0, -32, 16, 48, 48))
                    .build()
    );

    @Override
    public VoxelShape getCustomMasterVoxelShape(BlockState state) {
        return SHAPES_LAZY.get().get(state.getValue(FACING));
    }

    public MachineFluidTankBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    // --- ЛОГИКА МУЛЬТИБЛОКА ---

    @Override
    public PartRole getPartRole(BlockPos localOffset) {
        // Здесь можно настроить, где входы/выходы, если нужно. Пока всё DEFAULT.
        return PartRole.DEFAULT;
    }

    @Override
    public MultiblockStructureHelper getStructureHelper() {
        if (STRUCTURE_HELPER == null) {
            STRUCTURE_HELPER = new MultiblockStructureHelper(defineStructure(), () -> ModBlocks.UNIVERSAL_MACHINE_PART.get().defaultBlockState());
        }
        return STRUCTURE_HELPER;
    }

    /**
     * Определяем структуру 5x3x3.
     * Контроллер в (0, 0, 0).
     */
    private static Map<BlockPos, Supplier<BlockState>> defineStructure() {
        ImmutableMap.Builder<BlockPos, Supplier<BlockState>> builder = ImmutableMap.builder();

        // X: от -2 до 2 (5 блоков в ширину). 0 - это центр.
        // Y: от 0 до 2 (3 блока в высоту).
        // Z: от 0 до 2 (3 блока в глубину, растет назад от контроллера).

        for (int x = -2; x <= 2; x++) {
            for (int y = 0; y <= 2; y++) {
                for (int z = 0; z <= 2; z++) {
                    // Пропускаем (0,0,0), так как это сам блок контроллера
                    if (x == 0 && y == 0 && z == 0) continue;

                    builder.put(new BlockPos(x, y, z), () -> ModBlocks.UNIVERSAL_MACHINE_PART.get().defaultBlockState());
                }
            }
        }
        return builder.build();
    }

    // --- СОБЫТИЯ ---

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        super.onPlace(state, level, pos, oldState, isMoving);

        if (!state.is(oldState.getBlock()) && !level.isClientSide()) {
            Direction facing = state.getValue(FACING);
            // Используем геттер для безопасности
            getStructureHelper().placeStructure(level, pos, facing, this);
        }
    }

    @Override
    public void onRemove(@Nonnull BlockState state, @Nonnull Level level, @Nonnull BlockPos pos, @Nonnull BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock()) && !level.isClientSide()) {
            Direction facing = state.getValue(FACING);

            // Дроп предметов
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof MachineFluidTankBlockEntity) {
                blockEntity.getCapability(ForgeCapabilities.ITEM_HANDLER).ifPresent(handler -> {
                    for (int i = 0; i < handler.getSlots(); i++) {
                        Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), handler.getStackInSlot(i));
                    }
                });
            }

            // Разрушаем структуру (ИСПРАВЛЕНО: вызов через геттер)
            getStructureHelper().destroyStructure(level, pos, facing);
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    @Override
    public InteractionResult use(@Nonnull BlockState state, @Nonnull Level level, @Nonnull BlockPos pos, @Nonnull Player player, @Nonnull InteractionHand hand, @Nonnull BlockHitResult hit) {
        if (!level.isClientSide) {
            BlockEntity entity = level.getBlockEntity(pos);
            if (entity instanceof MachineFluidTankBlockEntity tank) {
                NetworkHooks.openScreen((ServerPlayer) player, tank, pos);
            }
        }
        return InteractionResult.SUCCESS;
    }

    // --- СТАНДАРТНЫЕ МЕТОДЫ ---

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return getCustomMasterVoxelShape(state);
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return getCustomMasterVoxelShape(state);
    }

    @Override
    public RenderShape getRenderShape(@Nonnull BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable @Override
    public BlockEntity newBlockEntity(@Nonnull BlockPos pos, @Nonnull BlockState state) {
        return ModBlockEntities.FLUID_TANK_BE.get().create(pos, state);
    }

    @Nullable @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(@Nonnull Level level, @Nonnull BlockState state, @Nonnull BlockEntityType<T> type) {
        return createTickerHelper(type, ModBlockEntities.FLUID_TANK_BE.get(), MachineFluidTankBlockEntity::tick);
    }

    @Nullable @Override
    public BlockState getStateForPlacement(@Nonnull BlockPlaceContext context) {
        // Ставим лицом к игроку
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected void createBlockStateDefinition(@Nonnull StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }
}