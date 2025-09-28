package com.hbm_m.block;

// Этот класс реализует блок продвинутой сборочной машины,
// которая является мультиблочной структурой 3x3x3 с центральным контроллером
import com.google.common.collect.ImmutableMap;
import com.hbm_m.block.entity.MachineAdvancedAssemblerBlockEntity;
import com.hbm_m.block.entity.ModBlockEntities;
import com.hbm_m.multiblock.IMultiblockController;
import com.hbm_m.multiblock.MultiblockStructureHelper;
import com.hbm_m.multiblock.PartRole;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
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
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.function.Supplier;

public class MachineAdvancedAssemblerBlock extends BaseEntityBlock implements IMultiblockController {

    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    private static MultiblockStructureHelper STRUCTURE_HELPER;
    
    // A map to cache generated shapes for each direction to improve performance
    private static final Map<Direction, VoxelShape> SHAPE_CACHE = new java.util.EnumMap<>(Direction.class);

    public MachineAdvancedAssemblerBlock(Properties pProperties) {
        super(pProperties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    public PartRole getPartRole(BlockPos localOffset) {
        // This machine is a simple cube with no special connectors.
        return PartRole.DEFAULT;
    }
    
    @Override
    public VoxelShape getShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
        // Use the cached shape if available, otherwise generate and cache it.
        return SHAPE_CACHE.computeIfAbsent(pState.getValue(FACING),
                facing -> getStructureHelper().generateShapeFromParts(facing));
    }
    
    @Override
    public VoxelShape getCollisionShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
        return getShape(pState, pLevel, pPos, pContext);
    }
    
    @Override
    public void setPlacedBy(Level pLevel, BlockPos pPos, BlockState pState, @Nullable LivingEntity pPlacer, ItemStack pStack) {
        super.setPlacedBy(pLevel, pPos, pState, pPlacer, pStack);
        if (!pLevel.isClientSide) {
            getStructureHelper().placeStructure(pLevel, pPos, pState.getValue(FACING), this);
        }
    }

    @Override
    public void onRemove(BlockState pState, Level pLevel, BlockPos pPos, BlockState pNewState, boolean pIsMoving) {
        if (!pState.is(pNewState.getBlock())) {
            // Drop items and other onRemove logic...
            getStructureHelper().destroyStructure(pLevel, pPos, pState.getValue(FACING));
        }
        super.onRemove(pState, pLevel, pPos, pNewState, pIsMoving);
    }

    @Override
    public MultiblockStructureHelper getStructureHelper() {
        if (STRUCTURE_HELPER == null) {
            STRUCTURE_HELPER = new MultiblockStructureHelper(defineStructure(), () -> ModBlocks.UNIVERSAL_MACHINE_PART.get().defaultBlockState());
        }
        return STRUCTURE_HELPER;
    }
    
    private static Map<BlockPos, Supplier<BlockState>> defineStructure() {
        ImmutableMap.Builder<BlockPos, Supplier<BlockState>> builder = ImmutableMap.builder();
        // Define a 3x3x3 cube structure, skipping the center (controller)
        for (int y = 0; y <= 2; y++) {
            for (int x = -1; x <= 1; x++) {
                for (int z = -1; z <= 1; z++) {
                    if (x == 0 && y == 0 && z == 0) continue;
                    builder.put(new BlockPos(x, y, z), () -> ModBlocks.UNIVERSAL_MACHINE_PART.get().defaultBlockState());
                }
            }
        }
        return builder.build();
    }
    
    @Override
public InteractionResult use(BlockState pState, Level pLevel, BlockPos pPos, Player pPlayer, InteractionHand pHand, BlockHitResult pHit) {
    if (!pLevel.isClientSide()) {
        BlockEntity blockEntity = pLevel.getBlockEntity(pPos);
        if (blockEntity instanceof MachineAdvancedAssemblerBlockEntity aamBe) {
            if (pPlayer.isShiftKeyDown()) {
                // --- ДИАГНОСТИКА ---
                aamBe.isCrafting = !aamBe.isCrafting;
                System.out.println("[СЕРВЕР] Статус крафта изменен на: " + aamBe.isCrafting);
                
                // Важно! Нужно уведомить BlockEntity об изменении, чтобы оно сохранилось
                aamBe.setChanged(); 
                
                // Эта строка отправляет пакет с обновлением клиенту.
                // Флаг '3' означает: обновить блок, отправить клиентам и перерисовать на клиенте.
                pLevel.sendBlockUpdated(pPos, pState, pState, 3);
                
                pPlayer.displayClientMessage(Component.literal("Animation toggled: " + aamBe.isCrafting), true);
                return InteractionResult.SUCCESS;
            }
            NetworkHooks.openScreen((ServerPlayer) pPlayer, aamBe, pPos);
        }
    }
    return InteractionResult.sidedSuccess(pLevel.isClientSide());
}

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext pContext) {
        return this.defaultBlockState().setValue(FACING, pContext.getHorizontalDirection().getOpposite());
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> pBuilder) {
        pBuilder.add(FACING);
    }

    @Override
    public RenderShape getRenderShape(BlockState pState) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pPos, BlockState pState) {
        return new MachineAdvancedAssemblerBlockEntity(pPos, pState);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level pLevel, BlockState pState, BlockEntityType<T> pBlockEntityType) {
        return createTickerHelper(pBlockEntityType, ModBlockEntities.ADVANCED_ASSEMBLY_MACHINE.get(), MachineAdvancedAssemblerBlockEntity::tick);
    }
}