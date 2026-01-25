package com.hbm_m.block.custom.machines;

import com.hbm_m.block.entity.ModBlockEntities;
import com.hbm_m.api.energy.EnergyNetworkManager;
// Этот класс реализует блок продвинутой сборочной машины,
// которая является мультиблочной структурой 3x3x3 с центральным контроллером
import com.google.common.collect.ImmutableMap;
import com.hbm_m.block.ModBlocks;
import com.hbm_m.block.entity.custom.machines.MachineAdvancedAssemblerBlockEntity;
import com.hbm_m.multiblock.IFrameSupportable;
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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.network.NetworkHooks;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.function.Supplier;

public class MachineAdvancedAssemblerBlock extends BaseEntityBlock implements IMultiblockController {

    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;

    private final MultiblockStructureHelper structureHelper;

    public MachineAdvancedAssemblerBlock(Properties pProperties) {
        super(pProperties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
        this.structureHelper = new MultiblockStructureHelper(defineStructure(), () -> ModBlocks.UNIVERSAL_MACHINE_PART.get().defaultBlockState());
    }

    // --- Связь с энергосетью HBM ---

    @Override
    public void onPlace(BlockState pState, Level pLevel, BlockPos pPos, BlockState pOldState, boolean pIsMoving) {
        super.onPlace(pState, pLevel, pPos, pOldState, pIsMoving);
        if (!pLevel.isClientSide() && !pState.is(pOldState.getBlock())) {
            MultiblockStructureHelper helper = getStructureHelper();
            Direction facing = pState.getValue(FACING);

            helper.placeStructure(pLevel, pPos, facing, this);

            // ИСПРАВЛЕННАЯ ЛОГИКА
            for (BlockPos localPos : helper.getStructureMap().keySet()) {
                if (getPartRole(localPos) == PartRole.ENERGY_CONNECTOR) {
                    BlockPos worldPos = helper.getRotatedPos(pPos, localPos, facing);
                    EnergyNetworkManager.get((ServerLevel) pLevel).addNode(worldPos);
                }
            }

            if (pLevel.getBlockEntity(pPos) instanceof IFrameSupportable be) {
                be.checkForFrame();
            }
        }
    }


    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            if (!level.isClientSide()) {
                MultiblockStructureHelper helper = getStructureHelper();
                Direction facing = state.getValue(FACING);

                // Удаляем из энергосети (этот код у нас уже правильный)
                for (BlockPos localPos : helper.getStructureMap().keySet()) {
                    if (getPartRole(localPos) == PartRole.ENERGY_CONNECTOR) {
                        BlockPos worldPos = helper.getRotatedPos(pos, localPos, facing);
                        EnergyNetworkManager.get((ServerLevel) level).removeNode(worldPos);
                    }
                }

                // --- ИСПРАВЛЕННАЯ ЛОГИКА ДРОПА ---
                BlockEntity blockEntity = level.getBlockEntity(pos);
                if (blockEntity instanceof MachineAdvancedAssemblerBlockEntity) {
                    // Получаем capability инвентаря
                    blockEntity.getCapability(ForgeCapabilities.ITEM_HANDLER).ifPresent(handler -> {
                        // Проходимся по всем слотам и выбрасываем их содержимое
                        for (int i = 0; i < handler.getSlots(); i++) {
                            Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), handler.getStackInSlot(i));
                        }
                    });
                }
                // ------------------------------------

                helper.destroyStructure(level, pos, facing);
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    // --- Остальной код ---

    @Override public RenderShape getRenderShape(BlockState pState) { return RenderShape.ENTITYBLOCK_ANIMATED; }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> pBuilder) { pBuilder.add(FACING); }
    @Nullable @Override public BlockState getStateForPlacement(BlockPlaceContext pContext) { return this.defaultBlockState().setValue(FACING, pContext.getHorizontalDirection().getOpposite()); }
    @Nullable @Override public BlockEntity newBlockEntity(BlockPos pPos, BlockState pState) { return new MachineAdvancedAssemblerBlockEntity(pPos, pState); }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level pLevel, BlockState pState, BlockEntityType<T> pType) {
        return createTickerHelper(pType, ModBlockEntities.ADVANCED_ASSEMBLY_MACHINE_BE.get(), MachineAdvancedAssemblerBlockEntity::tick);
    }

    @Override
    public InteractionResult use(BlockState pState, Level pLevel, BlockPos pPos, Player pPlayer, InteractionHand pHand, BlockHitResult pHit) {
        if (!pLevel.isClientSide()) {
            if (pLevel.getBlockEntity(pPos) instanceof MenuProvider provider) {
                NetworkHooks.openScreen((ServerPlayer) pPlayer, provider, pPos);
            }
        }
        return InteractionResult.sidedSuccess(pLevel.isClientSide());
    }

    // --- IMultiblockController ---
    @Override public MultiblockStructureHelper getStructureHelper() { return this.structureHelper; }
    private static Map<BlockPos, Supplier<BlockState>> defineStructure() {
        ImmutableMap.Builder<BlockPos, Supplier<BlockState>> builder = ImmutableMap.builder();
        for (int y = 0; y <= 2; y++) for (int x = -1; x <= 1; x++) for (int z = -1; z <= 1; z++) {
            if (x == 0 && y == 0 && z == 0) continue;
            builder.put(new BlockPos(x, y, z), () -> ModBlocks.UNIVERSAL_MACHINE_PART.get().defaultBlockState());
        }
        return builder.build();
    }
    @Override public PartRole getPartRole(BlockPos localOffset) { return localOffset.getY() == 0 ? PartRole.ENERGY_CONNECTOR : PartRole.DEFAULT; }
}