package com.hbm_m.block.custom.machines;

import com.hbm_m.block.entity.ModBlockEntities;
import com.hbm_m.api.energy.EnergyNetworkManager;
import com.hbm_m.block.ModBlocks;
import com.hbm_m.block.entity.custom.machines.MachineWoodBurnerBlockEntity;
import com.hbm_m.multiblock.IMultiblockController;
import com.hbm_m.multiblock.MultiblockStructureHelper;
import com.hbm_m.multiblock.PartRole;
import com.google.common.collect.ImmutableMap;
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
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.network.NetworkHooks;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Дровяной генератор энергии (мультиблок 2x2x2).
 * ✅ Корректно интегрирован в энергосеть HBM.
 */
public class MachineWoodBurnerBlock extends BaseEntityBlock implements IMultiblockController {

    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    public static final BooleanProperty LIT = BooleanProperty.create("lit");

    public MachineWoodBurnerBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(LIT, false));
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, LIT);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    // ✅ ИСПРАВЛЕНО: Регистрация ВСЕХ блоков структуры в энергосети
    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        super.onPlace(state, level, pos, oldState, isMoving);

        if (!state.is(oldState.getBlock()) && !level.isClientSide()) {
            MultiblockStructureHelper helper = getStructureHelper();
            Direction facing = state.getValue(FACING);

            // Строим структуру
            helper.placeStructure(level, pos, facing, this);

            // ✅ Регистрируем КОНТРОЛЛЕР в сети (он IEnergyProvider)
            EnergyNetworkManager.get((ServerLevel) level).addNode(pos);

            // ✅ Регистрируем энергетические коннекторы (parts с PartRole.ENERGY_CONNECTOR)
            for (BlockPos localPos : helper.getStructureMap().keySet()) {
                if (getPartRole(localPos) == PartRole.ENERGY_CONNECTOR) {
                    BlockPos worldPos = helper.getRotatedPos(pos, localPos, facing);
                    EnergyNetworkManager.get((ServerLevel) level).addNode(worldPos);
                }
            }
        }
    }

    // ✅ ИСПРАВЛЕНО: Удаление из сети при разрушении
    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (state.getBlock() != newState.getBlock() && !level.isClientSide()) {
            MultiblockStructureHelper helper = getStructureHelper();
            Direction facing = state.getValue(FACING);

            // ✅ Удаляем контроллер из сети
            EnergyNetworkManager.get((ServerLevel) level).removeNode(pos);

            // ✅ Удаляем энергетические коннекторы
            for (BlockPos localPos : helper.getStructureMap().keySet()) {
                if (getPartRole(localPos) == PartRole.ENERGY_CONNECTOR) {
                    BlockPos worldPos = helper.getRotatedPos(pos, localPos, facing);
                    EnergyNetworkManager.get((ServerLevel) level).removeNode(worldPos);
                }
            }

            // Дроп предметов
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof MachineWoodBurnerBlockEntity) {
                blockEntity.getCapability(ForgeCapabilities.ITEM_HANDLER).ifPresent(handler -> {
                    for (int i = 0; i < handler.getSlots(); i++) {
                        Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), handler.getStackInSlot(i));
                    }
                });
            }

            // Разрушаем структуру
            helper.destroyStructure(level, pos, facing);
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MachineWoodBurnerBlockEntity(pos, state);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide()) {
            if (level.getBlockEntity(pos) instanceof MenuProvider provider) {
                NetworkHooks.openScreen((ServerPlayer) player, provider, pos);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ModBlockEntities.WOOD_BURNER_BE.get(), MachineWoodBurnerBlockEntity::tick);
    }

    // --- IMultiblockController implementation ---

    private static MultiblockStructureHelper STRUCTURE_HELPER;

    @Override
    public MultiblockStructureHelper getStructureHelper() {
        if (STRUCTURE_HELPER == null) {
            STRUCTURE_HELPER = new MultiblockStructureHelper(defineStructure(), () -> ModBlocks.UNIVERSAL_MACHINE_PART.get().defaultBlockState());
        }
        return STRUCTURE_HELPER;
    }

    private static Map<BlockPos, Supplier<BlockState>> defineStructure() {
        ImmutableMap.Builder<BlockPos, Supplier<BlockState>> builder = ImmutableMap.builder();
        for (int y = 0; y <= 1; y++)
            for (int x = 0; x <= 1; x++)
                for (int z = 0; z <= 1; z++) {
                    if (x == 0 && y == 0 && z == 0) continue; // Пропускаем контроллер
                    builder.put(new BlockPos(x, y, z), () -> ModBlocks.UNIVERSAL_MACHINE_PART.get().defaultBlockState());
                }
        return builder.build();
    }

    @Override
    public PartRole getPartRole(BlockPos localOffset) {
        // Энергетические коннекторы: нижний ряд (y=0), задняя стенка (z=1)
        return (localOffset.getY() == 0 && localOffset.getZ() == 1 && (localOffset.getX() == 0 || localOffset.getX() == 1))
                ? PartRole.ENERGY_CONNECTOR
                : PartRole.DEFAULT;
    }
}