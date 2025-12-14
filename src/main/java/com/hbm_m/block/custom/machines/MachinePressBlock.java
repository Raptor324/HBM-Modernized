package com.hbm_m.block.custom.machines;

import com.hbm_m.block.ModBlocks;
// Этот класс реализует блок пресса, который является контроллером мультиблочной структуры.
// Пресс занимает 1x1x3 блока и использует вспомогательный класс MultiblockStructureHelper для управления своей структурой.
// Контроллер отвечает за построение и разрушение структуры, а также за взаимодействие с игроком.
import com.hbm_m.block.entity.ModBlockEntities;
import com.hbm_m.block.entity.custom.machines.MachinePressBlockEntity;
import com.hbm_m.multiblock.IMultiblockController;
import com.hbm_m.multiblock.MultiblockStructureHelper;
import com.hbm_m.multiblock.PartRole;
import com.google.common.collect.ImmutableMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.function.Supplier;

public class MachinePressBlock extends BaseEntityBlock implements IMultiblockController {
    // 3 блока в высоту, 1 блок в ширину и глубину
    public static final VoxelShape SHAPE = Shapes.box(0.0, 0.0, 0.0, 1.0, 3.0, 1.0);

    // Добавляем ориентацию для совместимости с MultiblockBlockItem и helper'ом
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;

    public MachinePressBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext pContext) {
        return this.defaultBlockState().setValue(FACING, pContext.getHorizontalDirection().getOpposite());
    }

    @Override
    public void onPlace(BlockState pState, Level pLevel, BlockPos pPos, BlockState pOldState, boolean pIsMoving) {
        super.onPlace(pState, pLevel, pPos, pOldState, pIsMoving);
        if (!pState.is(pOldState.getBlock())) {
            if (!pLevel.isClientSide) {
                // Построить структуру частей для пресса
                getStructureHelper().placeStructure(pLevel, pPos, pState.getValue(FACING), this);
            }
        }
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
        if (state.getBlock() != newState.getBlock()) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof MachinePressBlockEntity) {
                ((MachinePressBlockEntity) blockEntity).drops();
            }
            // Уничтожаем структуру при удалении контроллера
            this.getStructureHelper().destroyStructure(level, pos, state.getValue(FACING));
        }
        super.onRemove(state, level, pos, newState, moved);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide()) {
            BlockEntity entity = level.getBlockEntity(pos);
            if(entity instanceof MachinePressBlockEntity) {
                NetworkHooks.openScreen(((ServerPlayer) player), (MachinePressBlockEntity) entity, pos);
            } else {
                throw new IllegalStateException("Our Container provider is missing!");
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MachinePressBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        return createTickerHelper(blockEntityType, ModBlockEntities.PRESS_BE.get(),
            MachinePressBlockEntity::tick);
    }

    // --- IMultiblockController implementation ---

    private static MultiblockStructureHelper STRUCTURE_HELPER;

    private static Map<BlockPos, Supplier<BlockState>> defineStructure() {
        ImmutableMap.Builder<BlockPos, Supplier<BlockState>> builder = ImmutableMap.builder();
        // Two blocks above the controller to make 1x1x3
        builder.put(new BlockPos(0, 1, 0), () -> ModBlocks.UNIVERSAL_MACHINE_PART.get().defaultBlockState());
        builder.put(new BlockPos(0, 2, 0), () -> ModBlocks.UNIVERSAL_MACHINE_PART.get().defaultBlockState());
        return builder.build();
    }

    @Override
    public MultiblockStructureHelper getStructureHelper() {
        if (STRUCTURE_HELPER == null) {
            STRUCTURE_HELPER = new MultiblockStructureHelper(defineStructure(), () -> ModBlocks.UNIVERSAL_MACHINE_PART.get().defaultBlockState());
        }
        return STRUCTURE_HELPER;
    }

    @Override
    public PartRole getPartRole(BlockPos localOffset) {
        // Для пресса все части — чистая структура
        return PartRole.DEFAULT;
    }
}