package com.hbm_m.block;

// Этот класс реализует блок сборочной машины,
// которая является мультиблочной структурой 3x2x3 с центральным контроллером. Может принимать энергию и обрабатывать предметы.
import com.google.common.collect.ImmutableMap;
import com.hbm_m.block.entity.MachineAssemblerBlockEntity;
import com.hbm_m.block.entity.ModBlockEntities;
import com.hbm_m.multiblock.IMultiblockController;
import com.hbm_m.multiblock.MultiblockStructureHelper;
import com.hbm_m.multiblock.PartRole;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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

public class MachineAssemblerBlock extends BaseEntityBlock implements IMultiblockController {

    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;

    // Инициализируем формы напрямую из заранее заготовленных методов, без вращения
    static final Lazy<Map<Direction, VoxelShape>> SHAPES_LAZY = Lazy.of(() ->
        ImmutableMap.<Direction, VoxelShape>builder()
            .put(Direction.NORTH, buildShapeNorth().move(0.5, 0, 0.5))
            .put(Direction.SOUTH,  buildShapeSouth().move(0.5, 0, 0.5))
            .put(Direction.WEST,   buildShapeWest().move(0.5, 0, 0.5))
            .put(Direction.EAST,   buildShapeEast().move(0.5, 0, 0.5))
            .build()
    );

    // --- ЧЕТЫРЕ НЕЗАВИСИМЫХ МЕТОДА ДЛЯ КАЖДОГО НАПРАВЛЕНИЯ ---

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

    private static VoxelShape buildShapeEast() { // Rotation: (x, z) -> (-z, x)
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

    private static VoxelShape buildShapeSouth() { // Rotation: (x, z) -> (-x, -z)
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

    private static VoxelShape buildShapeWest() { // Rotation: (x, z) -> (z, -x)
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

        // Logic moved from MachineAssemblerPartBlockEntity.isEnergyConnector()
        boolean isEnergy = (y == 0) && (x >= 0 && x <= 1) && (z == -1 || z == 2);
        if (isEnergy) {
            return PartRole.ENERGY_CONNECTOR;
        }

        // Logic moved from MachineAssemblerPartBlockEntity.isConveyorConnector()
        boolean isOutput = (y == 0) && x == -1 && (z == 0 || z == 1);
        if (isOutput) {
            return PartRole.ITEM_OUTPUT;
        }
        
        boolean isInput = (y == 0) && x == 2 && (z == 0 || z == 1);
        if (isInput) {
            return PartRole.ITEM_INPUT;
        }

        return PartRole.DEFAULT;
    }

    @Override
    public MultiblockStructureHelper getStructureHelper() {
        if (STRUCTURE_HELPER == null) {
            // The structure definition remains the same, but we tell the helper to use the universal Phantom Block
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
                    if (y == 0 && x == 0 && z == 0) continue;
                    // The supplier here is now just a placeholder, as the helper uses the one from its constructor
                    builder.put(new BlockPos(x, y, z), () -> ModBlocks.UNIVERSAL_MACHINE_PART.get().defaultBlockState());
                }
            }
        }
        return builder.build();
    }

    @Override
    public void onPlace(BlockState pState, Level pLevel, BlockPos pPos, BlockState pOldState, boolean pIsMoving) {
        super.onPlace(pState, pLevel, pPos, pOldState, pIsMoving);
        // Проверяем, что это не просто смена состояния блока, а именно установка нового
        if (!pState.is(pOldState.getBlock())) {
            if (!pLevel.isClientSide) {
                // Вызываем логику постройки структуры
                getStructureHelper().placeStructure(pLevel, pPos, pState.getValue(FACING), this);
            }
        }
    }

    @Override
    public void onRemove(@Nonnull BlockState state, @Nonnull Level level, @Nonnull BlockPos pos, @Nonnull BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof MachineAssemblerBlockEntity assemblerEntity) {
                assemblerEntity.getCapability(ForgeCapabilities.ITEM_HANDLER).ifPresent(handler -> {
                    for (int i = 0; i < handler.getSlots(); i++) {
                        Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), handler.getStackInSlot(i));
                    }
                });
            }
            STRUCTURE_HELPER.destroyStructure(level, pos, state.getValue(FACING));
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
    public VoxelShape getShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
        return getCustomMasterVoxelShape(pState);
    }

    @Override
    public VoxelShape getCollisionShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
        return getCustomMasterVoxelShape(pState);
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