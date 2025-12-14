package com.hbm_m.block.custom.machines;

import com.google.common.collect.ImmutableMap;
import com.hbm_m.block.ModBlocks;
import com.hbm_m.block.entity.ModBlockEntities;
import com.hbm_m.api.energy.EnergyNetworkManager;
import com.hbm_m.block.entity.custom.machines.MachineAssemblerBlockEntity;
import com.hbm_m.multiblock.IMultiblockController;
import com.hbm_m.multiblock.MultiblockStructureHelper;
import com.hbm_m.multiblock.PartRole;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
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
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.Lazy;
import net.minecraftforge.network.NetworkHooks;

import java.util.Map;
import java.util.function.Supplier;

import javax.annotation.Nonnull;

import org.jetbrains.annotations.Nullable;

/**
 * Сборочная машина (мультиблок 3x2x3).
 * ✅ Корректно интегрирована в энергосеть HBM.
 */
public class MachineAssemblerBlock extends BaseEntityBlock implements IMultiblockController {

    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;

    static final Lazy<Map<Direction, VoxelShape>> SHAPES_LAZY = Lazy.of(() ->
            ImmutableMap.<Direction, VoxelShape>builder()
                    .put(Direction.NORTH, buildShapeNorth().move(0.5, 0, 0.5))
                    .put(Direction.SOUTH,  buildShapeSouth().move(0.5, 0, 0.5))
                    .put(Direction.WEST,   buildShapeWest().move(0.5, 0, 0.5))
                    .put(Direction.EAST,   buildShapeEast().move(0.5, 0, 0.5))
                    .build()
    );

    private static VoxelShape buildShapeNorth() {
        return Shapes.or(
                Block.box(-16, 0, -16, 32, 32, 32),
                Block.box(-24, 0, 8, -8, 16, 24),
                Block.box(32, 0, -8, 40, 16, 8),
                Block.box(-2.5, 5.5, -24, 2.5, 10.5, -16),
                Block.box(13.5, 5.5, -24, 18.5, 10.5, -16),
                Block.box(-2.5, 5.5, 32, 2.5, 10.5, 40),
                Block.box(13.5, 5.5, 32, 18.5, 10.5, 40)
        ).optimize();
    }

    private static VoxelShape buildShapeEast() {
        return Shapes.or(
                Block.box(-32, 0, -16, 16, 32, 32),
                Block.box(-24, 0, -24, -8, 16, -8),
                Block.box(-8, 0, 32, 8, 16, 40),
                Block.box(16, 5.5, -2.5, 24, 10.5, 2.5),
                Block.box(16, 5.5, 13.5, 24, 10.5, 18.5),
                Block.box(-40, 5.5, -2.5, -32, 10.5, 2.5),
                Block.box(-40, 5.5, 13.5, -32, 10.5, 18.5)
        ).optimize();
    }

    private static VoxelShape buildShapeSouth() {
        return Shapes.or(
                Block.box(-32, 0, -32, 16, 32, 16),
                Block.box(8, 0, -24, 24, 16, -8),
                Block.box(-40, 0, -8, -32, 16, 8),
                Block.box(-2.5, 5.5, 16, 2.5, 10.5, 24),
                Block.box(-18.5, 5.5, 16, -13.5, 10.5, 24),
                Block.box(-2.5, 5.5, -40, 2.5, 10.5, -32),
                Block.box(-18.5, 5.5, -40, -13.5, 10.5, -32)
        ).optimize();
    }

    private static VoxelShape buildShapeWest() {
        return Shapes.or(
                Block.box(-16, 0, -32, 32, 32, 16),
                Block.box(8, 0, 8, 24, 16, 24),
                Block.box(-8, 0, -40, 8, 16, -32),
                Block.box(-24, 5.5, -2.5, -16, 10.5, 2.5),
                Block.box(-24, 5.5, -18.5, -16, 10.5, -13.5),
                Block.box(32, 5.5, -2.5, 40, 10.5, 2.5),
                Block.box(32, 5.5, -18.5, 40, 10.5, -13.5)
        ).optimize();
    }

    private static MultiblockStructureHelper STRUCTURE_HELPER;

    public MachineAssemblerBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    public PartRole getPartRole(BlockPos localOffset) {
        int x = localOffset.getX();
        int y = localOffset.getY();
        int z = localOffset.getZ();

        // Энергетические коннекторы
        boolean isEnergy = (y == 0) && (x >= 0 && x <= 1) && (z == -1 || z == 2);
        if (isEnergy) {
            return PartRole.ENERGY_CONNECTOR;
        }

        // Выходные конвейеры
        boolean isOutput = (y == 0) && x == -1 && (z == 0 || z == 1);
        if (isOutput) {
            return PartRole.ITEM_OUTPUT;
        }

        // Входные конвейеры
        boolean isInput = (y == 0) && x == 2 && (z == 0 || z == 1);
        if (isInput) {
            return PartRole.ITEM_INPUT;
        }

        return PartRole.DEFAULT;
    }

    @Override
    public MultiblockStructureHelper getStructureHelper() {
        if (STRUCTURE_HELPER == null) {
            STRUCTURE_HELPER = new MultiblockStructureHelper(defineStructure(), () -> ModBlocks.UNIVERSAL_MACHINE_PART.get().defaultBlockState());
        }
        return STRUCTURE_HELPER;
    }

    @Override
    public VoxelShape getCustomMasterVoxelShape(BlockState state) {
        return SHAPES_LAZY.get().get(state.getValue(FACING));
    }

    private static Map<BlockPos, Supplier<BlockState>> defineStructure() {
        ImmutableMap.Builder<BlockPos, Supplier<BlockState>> builder = ImmutableMap.builder();
        for (int y = 0; y <= 1; y++) {
            for (int x = -1; x <= 2; x++) {
                for (int z = -1; z <= 2; z++) {
                    if (y == 0 && x == 0 && z == 0) continue; // Пропускаем контроллер
                    builder.put(new BlockPos(x, y, z), () -> ModBlocks.UNIVERSAL_MACHINE_PART.get().defaultBlockState());
                }
            }
        }
        return builder.build();
    }

    // ✅ ДОБАВЛЕНО: Регистрация в энергосети
    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        super.onPlace(state, level, pos, oldState, isMoving);

        if (!state.is(oldState.getBlock()) && !level.isClientSide()) {
            Direction facing = state.getValue(FACING);

            // Строим структуру
            getStructureHelper().placeStructure(level, pos, facing, this);

            // ✅ Регистрируем контроллер (IEnergyReceiver)
            EnergyNetworkManager.get((ServerLevel) level).addNode(pos);

            // ✅ Регистрируем энергетические коннекторы
            for (BlockPos localPos : getStructureHelper().getStructureMap().keySet()) {
                if (getPartRole(localPos) == PartRole.ENERGY_CONNECTOR) {
                    BlockPos worldPos = getStructureHelper().getRotatedPos(pos, localPos, facing);
                    EnergyNetworkManager.get((ServerLevel) level).addNode(worldPos);
                }
            }
        }
    }

    // ✅ ДОБАВЛЕНО: Удаление из энергосети
    @Override
    public void onRemove(@Nonnull BlockState state, @Nonnull Level level, @Nonnull BlockPos pos, @Nonnull BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock()) && !level.isClientSide()) {
            Direction facing = state.getValue(FACING);

            // ✅ Удаляем контроллер из сети
            EnergyNetworkManager.get((ServerLevel) level).removeNode(pos);

            // ✅ Удаляем энергетические коннекторы
            for (BlockPos localPos : getStructureHelper().getStructureMap().keySet()) {
                if (getPartRole(localPos) == PartRole.ENERGY_CONNECTOR) {
                    BlockPos worldPos = getStructureHelper().getRotatedPos(pos, localPos, facing);
                    EnergyNetworkManager.get((ServerLevel) level).removeNode(worldPos);
                }
            }

            // Дроп предметов
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof MachineAssemblerBlockEntity) {
                blockEntity.getCapability(ForgeCapabilities.ITEM_HANDLER).ifPresent(handler -> {
                    for (int i = 0; i < handler.getSlots(); i++) {
                        Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), handler.getStackInSlot(i));
                    }
                });
            }

            // Разрушаем структуру
            STRUCTURE_HELPER.destroyStructure(level, pos, facing);
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    @Override
    public InteractionResult use(@Nonnull BlockState state, @Nonnull Level level, @Nonnull BlockPos pos, @Nonnull Player player, @Nonnull InteractionHand hand, @Nonnull BlockHitResult hit) {
        if (!level.isClientSide) {
            BlockEntity entity = level.getBlockEntity(pos);
            if (entity instanceof MenuProvider) {
                if (player instanceof ServerPlayer serverPlayer) {
                    NetworkHooks.openScreen(serverPlayer, (MenuProvider) entity, pos);
                }
            }
        }
        return InteractionResult.SUCCESS;
    }

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
        return new MachineAssemblerBlockEntity(pos, state);
    }

    @Nullable @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(@Nonnull Level level, @Nonnull BlockState state, @Nonnull BlockEntityType<T> type) {
        return createTickerHelper(type, ModBlockEntities.MACHINE_ASSEMBLER_BE.get(), MachineAssemblerBlockEntity::tick);
    }

    @Nullable @Override
    public BlockState getStateForPlacement(@Nonnull BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected void createBlockStateDefinition(@Nonnull StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }
}