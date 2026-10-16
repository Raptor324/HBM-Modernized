package com.hbm_m.block.machines;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.google.common.collect.ImmutableMap;
import java.util.function.Supplier;
import com.hbm_m.block.ModBlocks;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.machines.BlastFurnaceBlockEntity;
import com.hbm_m.interfaces.IMultiblockController;
import com.hbm_m.multiblock.MultiblockStructureHelper;
import com.hbm_m.multiblock.PartRole;

import dev.architectury.registry.menu.MenuRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Доменная печь - мультиблок 3x7x3 (как в обновлённом оригинале).
 * Порты: 4 боковых на уровне ядра (инвентарь + жидкости), 2 фронтальных (жидкости),
 * верхний центр - дымовая труба (жидкости).
 */
public class BlastFurnaceBlock extends BaseEntityBlock implements IMultiblockController {

    public static final BooleanProperty LIT = BlockStateProperties.LIT;
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;

    private static MultiblockStructureHelper STRUCTURE_HELPER;

    public BlastFurnaceBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(LIT, false)
                .setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LIT, FACING);
    }

    // ==================== MULTIBLOCK ====================

    @Override
    public MultiblockStructureHelper getStructureHelper() {
        if (STRUCTURE_HELPER == null) {
            STRUCTURE_HELPER = new MultiblockStructureHelper(defineStructure(),
                    () -> ModBlocks.UNIVERSAL_MACHINE_PART.get().defaultBlockState());
        }
        return STRUCTURE_HELPER;
    }

    private static ImmutableMap<BlockPos, Supplier<BlockState>> defineStructure() {
        ImmutableMap.Builder<BlockPos, Supplier<BlockState>> builder = ImmutableMap.builder();
        for (int y = 0; y <= 6; y++) {
            for (int x = -1; x <= 1; x++) {
                for (int z = -1; z <= 1; z++) {
                    if (y == 0 && x == 0 && z == 0) continue; // Контроллер
                    builder.put(new BlockPos(x, y, z), () -> ModBlocks.UNIVERSAL_MACHINE_PART.get().defaultBlockState());
                }
            }
        }
        return builder.build();
    }

    @Override
    public PartRole getPartRole(BlockPos localOffset) {
        int x = localOffset.getX();
        int y = localOffset.getY();
        int z = localOffset.getZ();

        // Верхний центр - дымовая труба (газы)
        if (y == 6 && x == 0 && z == 0) {
            return PartRole.FLUID_CONNECTOR;
        }

        // Фронтальные порты (воздушное дутьё)
        if (z == 1 && x == 0 && (y == 3 || y == 5)) {
            return PartRole.FLUID_CONNECTOR;
        }

        // Боковые порты на уровне ядра - инвентарь + жидкости
        if (y == 0 && (Math.abs(x) + Math.abs(z) == 1)) {
            return PartRole.UNIVERSAL_CONNECTOR;
        }

        return PartRole.DEFAULT;
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        super.onPlace(state, level, pos, oldState, isMoving);
        if (!state.is(oldState.getBlock()) && !level.isClientSide()) {
            placeMultiblockStructure(level, pos, state);
        }
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            if (!level.isClientSide()) {
                BlockEntity blockEntity = level.getBlockEntity(pos);
                if (blockEntity instanceof BlastFurnaceBlockEntity be) {
                    be.drops();
                }
                getStructureHelper().destroyStructure(level, pos, state.getValue(FACING));
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    // ==================== INTERACTION ====================

    //? if < 1.21.1 {
    @Override
    public InteractionResult use(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos, @NotNull Player player, @NotNull InteractionHand hand, @NotNull BlockHitResult hit) {
        return openMenu(state, level, pos, player);
    }
    //?} else {
    /*@Override
    protected InteractionResult useWithoutItem(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos, @NotNull Player player, @NotNull BlockHitResult hit) {
        return openMenu(state, level, pos, player);
    }
     *///?}

    private InteractionResult openMenu(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos, @NotNull Player player) {
        if (!level.isClientSide()) {
            BlockEntity entity = level.getBlockEntity(pos);
            if (entity instanceof MenuProvider provider && player instanceof ServerPlayer serverPlayer) {
                MenuRegistry.openExtendedMenu(serverPlayer, provider, buf -> buf.writeBlockPos(pos));
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    // ==================== BLOCK ====================

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        // Рамка выделения - вся структура 3x7x3 (генерирует StructureHelper)
        return getStructureHelper().generateShapeFromParts(state.getValue(FACING));
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return getStructureHelper().generateShapeFromParts(state.getValue(FACING));
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(@NotNull BlockPlaceContext context) {
        return this.defaultBlockState()
                .setValue(FACING, context.getHorizontalDirection().getOpposite())
                .setValue(LIT, false);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        // Цельная OBJ-модель рендерится blockstate-моделью ядра (части структуры невидимы)
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
        return new BlastFurnaceBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(@NotNull Level level, @NotNull BlockState state, @NotNull BlockEntityType<T> type) {
        return createTickerHelper(type, ModBlockEntities.BLAST_FURNACE_BE.get(),
                BlastFurnaceBlockEntity::tick);
    }

    //? if >1.20.1 {
    /*public static final com.mojang.serialization.MapCodec<BlastFurnaceBlock> CODEC = simpleCodec(BlastFurnaceBlock::new);

    @Override
    protected com.mojang.serialization.MapCodec<? extends net.minecraft.world.level.block.BaseEntityBlock> codec() {
        return CODEC;
    }
     *///?}
}

