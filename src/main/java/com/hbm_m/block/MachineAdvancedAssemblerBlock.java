package com.hbm_m.block;

// Этот класс реализует блок продвинутой сборочной машины,
// которая является мультиблочной структурой 3x3x3 с центральным контроллером
import com.google.common.collect.ImmutableMap;
import com.hbm_m.block.entity.MachineAdvancedAssemblerBlockEntity;
import com.hbm_m.block.entity.ModBlockEntities;
import com.hbm_m.multiblock.IFrameSupportable;
import com.hbm_m.multiblock.IMultiblockController;
import com.hbm_m.multiblock.MultiblockStructureHelper;
import com.hbm_m.multiblock.PartRole;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
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
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.function.Supplier;

public class MachineAdvancedAssemblerBlock extends BaseEntityBlock implements IMultiblockController {

    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;

    // --- НАЧАЛО ИСПРАВЛЕНИЯ АРХИТЕКТУРЫ ---

    // 1. Определение структуры теперь СТАТИЧЕСКОЕ и НЕИЗМЕНЯЕМОЕ (final). Оно создается один раз при загрузке класса.
    private static final Map<BlockPos, Supplier<BlockState>> STRUCTURE_DEFINITION = defineStructure();

    // 2. Хелпер теперь НЕ static. Он final и создается для КАЖДОГО экземпляра блока в конструкторе.
    private final MultiblockStructureHelper structureHelper;

    // 3. VoxelShape кэш также стал нестатическим. Каждый блок кэширует свои формы.
    private final Map<Direction, VoxelShape> shapeCache = new java.util.EnumMap<>(Direction.class);

    // --- КОНЕЦ ИСПРАВЛЕНИЯ АРХИТЕКТУРЫ ---


    public MachineAdvancedAssemblerBlock(Properties pProperties) {
        super(pProperties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
        // Инициализируем УНИКАЛЬНЫЙ хелпер для этого экземпляра блока
        this.structureHelper = new MultiblockStructureHelper(STRUCTURE_DEFINITION, () -> ModBlocks.UNIVERSAL_MACHINE_PART.get().defaultBlockState());
    }

    @Override
    public MultiblockStructureHelper getStructureHelper() {
        // Просто возвращаем наш уникальный экземпляр хелпера
        return this.structureHelper;
    }

    private static Map<BlockPos, Supplier<BlockState>> defineStructure() {
        ImmutableMap.Builder<BlockPos, Supplier<BlockState>> builder = ImmutableMap.builder();
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
    public PartRole getPartRole(BlockPos localOffset) {
        return PartRole.DEFAULT;
    }


    // --- ЛОГИКА ВЗАИМОДЕЙСТВИЯ И ОБНОВЛЕНИЙ ---

    @Override
    public VoxelShape getShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
        // Используем НЕСТАТИЧЕСКИЙ кэш
        return this.shapeCache.computeIfAbsent(pState.getValue(FACING),
                facing -> getStructureHelper().generateShapeFromParts(facing));
    }

    @Override
    public void onPlace(BlockState pState, Level pLevel, BlockPos pPos, BlockState pOldState, boolean pIsMoving) {
        super.onPlace(pState, pLevel, pPos, pOldState, pIsMoving);
        if (!pLevel.isClientSide() && !pState.is(pOldState.getBlock())) {
            getStructureHelper().placeStructure(pLevel, pPos, pState.getValue(FACING), this);
            
            // Запускаем первую проверку рамки сразу после постройки
            if (pLevel.getBlockEntity(pPos) instanceof IFrameSupportable be) {
                be.checkForFrame();
            }
        }
    }

    @Override
    public void onRemove(BlockState pState, Level pLevel, BlockPos pPos, BlockState pNewState, boolean pIsMoving) {
        if (!pState.is(pNewState.getBlock())) {
            // Логика дропа предметов должна быть здесь, если она нужна
            getStructureHelper().destroyStructure(pLevel, pPos, pState.getValue(FACING));
        }
        super.onRemove(pState, pLevel, pPos, pNewState, pIsMoving);
    }
    
    // ВАЖНО: Метод neighborChanged убран из контроллера.
    // Всю логику теперь обрабатывает `UniversalMachinePartBlock`, что является правильным подходом.
    // Контроллер не должен реагировать на изменения над ним самим, только его части.

    @Override
    public InteractionResult use(BlockState pState, Level pLevel, BlockPos pPos, Player pPlayer, InteractionHand pHand, BlockHitResult pHit) {
        if (pHand == InteractionHand.OFF_HAND) {
            return InteractionResult.PASS;
        }

        if (pPlayer.isShiftKeyDown()) {
            if (!pLevel.isClientSide()) {
                if (pLevel.getBlockEntity(pPos) instanceof IFrameSupportable frameBe) {
                    frameBe.checkForFrame();
                    pPlayer.displayClientMessage(Component.literal("Рамка проверена вручную!"), true);
                }
            }
            return InteractionResult.SUCCESS;
        }

        if (!pLevel.isClientSide()) {
            if (pLevel.getBlockEntity(pPos) instanceof MenuProvider menuProvider) {
                NetworkHooks.openScreen((ServerPlayer) pPlayer, menuProvider, pPos);
            }
        }
        return InteractionResult.CONSUME;
    }


    // --- СТАНДАРТНЫЕ МЕТОДЫ БЛОКА ---

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
        return createTickerHelper(pBlockEntityType, ModBlockEntities.ADVANCED_ASSEMBLY_MACHINE_BE.get(), MachineAdvancedAssemblerBlockEntity::tick);
    }
}