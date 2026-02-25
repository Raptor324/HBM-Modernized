package com.hbm_m.block.custom.machines;

import java.util.Map;
import java.util.function.Supplier;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.block.entity.ModBlockEntities;
import com.hbm_m.block.entity.custom.machines.MachineHydraulicFrackiningTowerBlockEntity;
import com.hbm_m.multiblock.IMultiblockController;
import com.hbm_m.multiblock.MultiblockStructureHelper;
import com.hbm_m.multiblock.PartRole;

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
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.network.NetworkHooks;

/**
 * Hydraulic Frackining Tower (Multiblock).
 *
 * Modernized implementation: places a tall 3x3 tower of phantom parts.
 * The exact legacy 1.7.10 shape can be refined later, but this keeps
 * the machine consistent with the current MultiblockStructureHelper system.
 */
public class MachineHydraulicFrackiningTowerBlock extends BaseEntityBlock implements IMultiblockController {

    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;

    private final MultiblockStructureHelper structureHelper;

    public MachineHydraulicFrackiningTowerBlock(Properties pProperties) {
        super(pProperties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
        this.structureHelper = defineStructureNew();
    }

    private static MultiblockStructureHelper defineStructureNew() {
        // - 'A' = DEFAULT (обычная часть структуры)
        // - 'B' = UNIVERSAL_CONNECTOR (универсальный коннектор)
        // - 'L' = LADDER (по нему можно взобраться как по лестнице)
        // - 'C' = CONTROLLER (блок контроллера - ОБЯЗАТЕЛЬНО, ровно 1!)
        // - '.' = пустота (символ не в roleMap, будет игнорирован)
        
        // Слои структуры 3x3x3
        String[] layer0 = {
            "OO...OO",
            "OO...OO",
            ".......",
            "...C...",
            ".......",
            "OO...OO",
            "OO...OO"
        };
        
        String[] layer1 = {
            "OO...OO",
            "OO...OO",
            ".......",
            "...X...",
            ".......",
            "OO...OO",
            "OO...OO"
        };

        String[] layer2 = {
            "OOHHHOO",
            "OOHHHOO",
            "HHHHHHH",
            "HHHXHHH",
            "HHHHHHH",
            "OOHHHOO",
            "OOHHHOO"
        };

        String[] layer3 = {
            "OOOOOOO",
            "OOOOOOO",
            "OOOOOOO",
            "OOOOOOO",
            "OOOOOOO",
            "OOOOOOO",
            "OOOOOOO"
        };
        String[] layer4 = {
            ".......",
            ".NNNNN.",
            ".N...N.",
            ".N.X.N.",
            ".N...N.",
            ".NNNNN.",
            "......."
        };

        String[] layer6 = {
            ".......",
            ".......",
            "..NNN..",
            "..NXN..",
            "..NNN..",
            ".......",
            "......."
        };
        String[] layer9 = {
            ".......",
            ".......",
            "..NNN..",
            "..N.N..",
            "..NNN..",
            ".......",
            "......."
        };

        String[] layer7 = {
            "..OOO..",
            ".NOOON.",
            ".N...N.",
            ".N.X.N.",
            ".N...N.",
            ".NNNNN.",
            "......."
        };

        String[] layer8 = {
            ".......",
            ".......",
            "..OOO..",
            "..OOO..",
            "..OOO..",
            ".......",
            "......."
        };
        
        // === roleMap: программист сам определяет маппинг ===
        // ВАЖНО: роль CONTROLLER ОБЯЗАТЕЛЬНА и должен быть ровно ОДИН контроллер!
        Map<Character, PartRole> roleMap = Map.of(
            'O', PartRole.DEFAULT,              // Обычная часть структуры
            'X', PartRole.LADDER,              // Обычная часть структуры
            'H', PartRole.DEFAULT,              // Обычная часть структуры
            'N', PartRole.DEFAULT,              // Обычная часть структуры
            'C', PartRole.CONTROLLER           // Контроллер (ОБЯЗАТЕЛЬНО!)
        );
        
        // === symbolMap: какой BlockState использовать для каждого символа. Может пригодиться, если вдруг понадобится 
        // назначить кастомные blockstate для каждого блока-части. Например, для освещения. ===
        // Контроллер 'C' НЕ добавляется в symbolMap - он размещается игроком отдельно!
        Map<Character, Supplier<BlockState>> symbolMap = Map.of(
            // 'O', () -> ModBlocks.UNIVERSAL_MACHINE_PART.get().defaultBlockState()
            // 'X', () -> ModBlocks.UNIVERSAL_MACHINE_PART.get().defaultBlockState(),
            // 'L', () -> ModBlocks.UNIVERSAL_MACHINE_PART.get().defaultBlockState()
        );

        // Задаем специфичные формы для символов
        Map<Character, VoxelShape> shapeMap = Map.of(
            'C', Block.box(2, 0, 2, 14, 16, 14),
            'X', Block.box(2, 0, 2, 14, 16, 14), // central pole
            'H', Block.box(0, 8, 0, 16, 16, 16), // upper slab
            'N', Block.box(0, 0, 0, 16, 16, 16)
        );
        Map<Character, VoxelShape> collisionMap = Map.of(
            'C', Block.box(2, 0, 2, 14, 16, 14),
            'X', Block.box(2, 0, 2, 14, 16, 14), // central pole
            'H', Block.box(0, 8, 0, 16, 16, 16), // upper slab
            'O', Block.box(0, 0, 0, 16, 16, 16)
        );
        
        // Используем createFromLayersWithRoles - автоматически найдёт позицию контроллера
        return MultiblockStructureHelper.createFromLayersWithRoles(
            new String[][]{layer0, layer1, layer2, layer3, layer4, layer4, layer4, layer4, layer4, layer4, layer4, layer4, layer4, layer4, layer4, layer7, layer7, layer6, layer6, layer9, layer9, layer9, layer9, layer8},
            symbolMap,
            () -> ModBlocks.UNIVERSAL_MACHINE_PART.get().defaultBlockState(),
            roleMap,
            shapeMap,
            collisionMap
        );
    }

    @Override public MultiblockStructureHelper getStructureHelper() { return this.structureHelper; }

    @Override 
    public PartRole getPartRole(BlockPos localOffset) { 
        // Используем универсальный метод разрешения ролей из хелпера
        if (structureHelper != null) {
            return structureHelper.resolvePartRole(localOffset, this);
        }
        return PartRole.DEFAULT;
    }

    @Override
    public VoxelShape getOcclusionShape(BlockState state, BlockGetter level, BlockPos pos) {
        MultiblockStructureHelper helper = getStructureHelper();
        if (helper != null) {
            // Проверяем, является ли сам контроллер полным блоком
            if (!helper.isFullBlock(helper.getControllerOffset(), state.getValue(FACING))) {
                return Shapes.empty();
            }
        }
        return Shapes.block();
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MachineHydraulicFrackiningTowerBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        // ИСПРАВЛЕНО: Добавляем тикер для работы машины
        return createTickerHelper(type, ModBlockEntities.HYDRAULIC_FRACKINING_TOWER_BE.get(), 
                MachineHydraulicFrackiningTowerBlockEntity::tick);
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        super.onPlace(state, level, pos, oldState, isMoving);
        if (!state.is(oldState.getBlock()) && !level.isClientSide()) {
            Direction facing = state.getValue(FACING);
            getStructureHelper().placeStructure(level, pos, facing, this);
        }
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock()) && !level.isClientSide()) {
            Direction facing = state.getValue(FACING);
            getStructureHelper().destroyStructure(level, pos, facing);
        }
        super.onRemove(state, level, pos, newState, isMoving);
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

    // 1. РАМКА ВЫДЕЛЕНИЯ: Показывает всю структуру целиком
    @Override
    public VoxelShape getShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
        MultiblockStructureHelper helper = getStructureHelper();
        if (helper != null) {
            // Возвращаем объединенную форму всех частей
            return helper.generateShapeFromParts(pState.getValue(FACING));
        }
        return Shapes.block();
    }

    // 2. КОЛЛИЗИЯ: Использует только форму самого блока контроллера из shapeMap
    @Override
    public VoxelShape getCollisionShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
        MultiblockStructureHelper helper = getStructureHelper();
        if (helper != null) {
            // Берём форму ТОЛЬКО для позиции контроллера
            // Она автоматически возьмётся из shapeMap через хелпер
            return helper.getSpecificPartShape(helper.getControllerOffset(), pState.getValue(FACING));
        }
        return Shapes.block();
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }
}
