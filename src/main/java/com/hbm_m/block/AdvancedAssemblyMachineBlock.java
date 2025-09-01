package com.hbm_m.block;

import com.google.common.collect.ImmutableMap;
import com.hbm_m.block.entity.AdvancedAssemblyMachineBlockEntity;
import com.hbm_m.block.entity.ModBlockEntities;
import com.hbm_m.multiblock.IMultiblockController;
import com.hbm_m.multiblock.MultiblockStructureHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
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
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.function.Supplier;

public class AdvancedAssemblyMachineBlock extends BaseEntityBlock implements IMultiblockController {

    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    private static MultiblockStructureHelper STRUCTURE_HELPER;

    // --- ЛОГИКА VOXELSHAPE ---
    // Форма ВСЕЙ структуры 3x3x3 относительно этого блока как центра (0,0,0)
    public static final VoxelShape MASTER_SHAPE = Shapes.box(-1, 0, -1, 2, 3, 2);

    public AdvancedAssemblyMachineBlock(Properties pProperties) {
        super(pProperties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    // ВОЗВРАЩАЕМ ПРАВИЛЬНЫЙ SHAPE ДЛЯ ГЛАВНОГО БЛОКА
    @Override
    public VoxelShape getShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
        return MASTER_SHAPE;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
        return MASTER_SHAPE;
    }

    // --- ЛОГИКА МУЛЬТИБЛОКА ---

    @Override
    public void onPlace(BlockState pState, Level pLevel, BlockPos pPos, BlockState pOldState, boolean pIsMoving) {
        super.onPlace(pState, pLevel, pPos, pOldState, pIsMoving);
        if (!pLevel.isClientSide) {
            // ВАЖНО: Убедитесь, что ваш MultiblockStructureHelper передает свойство FACING
            // в устанавливаемые блоки-части.
            getStructureHelper().placeStructure(pLevel, pPos, pState.getValue(FACING));
        }
    }

    @Override
    public void onRemove(BlockState pState, Level pLevel, BlockPos pPos, BlockState pNewState, boolean pIsMoving) {
        if (!pState.is(pNewState.getBlock())) {
            if (pLevel.getBlockEntity(pPos) instanceof AdvancedAssemblyMachineBlockEntity be) {
                be.getCapability(ForgeCapabilities.ITEM_HANDLER).ifPresent(handler -> {
                    for (int i = 0; i < handler.getSlots(); i++) {
                        Containers.dropItemStack(pLevel, pPos.getX(), pPos.getY(), pPos.getZ(), handler.getStackInSlot(i));
                    }
                });
            }
            getStructureHelper().destroyStructure(pLevel, pPos, pState.getValue(FACING));
        }
        super.onRemove(pState, pLevel, pPos, pNewState, pIsMoving);
    }

    @Override
    public MultiblockStructureHelper getStructureHelper() {
        if (STRUCTURE_HELPER == null) {
            STRUCTURE_HELPER = new MultiblockStructureHelper(defineStructure());
        }
        return STRUCTURE_HELPER;
    }

    private static Map<BlockPos, Supplier<BlockState>> defineStructure() {
        ImmutableMap.Builder<BlockPos, Supplier<BlockState>> builder = ImmutableMap.builder();
        for (int y = 0; y <= 2; y++) {
            for (int x = -1; x <= 1; x++) {
                for (int z = -1; z <= 1; z++) {
                    if (x == 0 && y == 0 && z == 0) continue;
                    final int finalX = x;
                    final int finalY = y;
                    final int finalZ = z;
                    builder.put(new BlockPos(x, y, z), () ->
                            ModBlocks.ADVANCED_ASSEMBLY_MACHINE_PART.get().defaultBlockState()
                                    .setValue(AdvancedAssemblyMachinePartBlock.OFFSET_X, finalX + 1)
                                    .setValue(AdvancedAssemblyMachinePartBlock.OFFSET_Y, finalY)
                                    .setValue(AdvancedAssemblyMachinePartBlock.OFFSET_Z, finalZ + 1)
                    );
                }
            }
        }
        return builder.build();
    }
    
    @Override
public InteractionResult use(BlockState pState, Level pLevel, BlockPos pPos, Player pPlayer, InteractionHand pHand, BlockHitResult pHit) {
    if (!pLevel.isClientSide()) {
        BlockEntity blockEntity = pLevel.getBlockEntity(pPos);
        if (blockEntity instanceof AdvancedAssemblyMachineBlockEntity aamBe) {
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
        return new AdvancedAssemblyMachineBlockEntity(pPos, pState);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level pLevel, BlockState pState, BlockEntityType<T> pBlockEntityType) {
        return createTickerHelper(pBlockEntityType, ModBlockEntities.ADVANCED_ASSEMBLY_MACHINE.get(), AdvancedAssemblyMachineBlockEntity::tick);
    }
}