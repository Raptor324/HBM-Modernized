package com.hbm_m.block.custom.decorations;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.block.entity.ModBlockEntities;
import com.hbm_m.block.entity.custom.doors.DoorBlockEntity;
import com.hbm_m.block.entity.custom.doors.DoorDecl;
import com.hbm_m.block.entity.custom.doors.DoorDeclRegistry;
import com.hbm_m.multiblock.IMultiblockController;
import com.hbm_m.multiblock.MultiblockStructureHelper;
import com.hbm_m.multiblock.PartRole;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class DoorBlock extends BaseEntityBlock implements IMultiblockController {

    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final EnumProperty<PartRole> PART_ROLE = EnumProperty.create("part_role", PartRole.class);
    /** Дверь движется (открывается или закрывается). Используется для переключения рендера при Iris/Oculus. */
    public static final BooleanProperty DOOR_MOVING = BooleanProperty.create("door_moving");
    /** Дверь полностью открыта. Используется для baked-геометрии (створка в правильной позиции). */
    public static final BooleanProperty OPEN = BooleanProperty.create("open");
    private final Map<Direction, VoxelShape> shapeCache = new java.util.EnumMap<>(Direction.class);

    private final String doorDeclId;
    private final MultiblockStructureHelper structureHelper;

    public DoorBlock(Properties properties, String doorDeclId) {
        super(properties);
        this.doorDeclId = doorDeclId;
        
        DoorDecl decl = DoorDeclRegistry.getById(doorDeclId);
        
        Supplier<BlockState> phantomSupplier = () -> ModBlocks.UNIVERSAL_MACHINE_PART.get().defaultBlockState();
        
        if (decl != null && decl.getStructureDefinition() != null) {
            DoorDecl.DoorStructureDefinition def = decl.getStructureDefinition();
            Map<BlockPos, Supplier<BlockState>> structureMap = new HashMap<>();
            
            // Заполняем карту блоков на основе закрытых форм
            for (BlockPos localPos : def.getClosedShapes().keySet()) {
                if (localPos.equals(BlockPos.ZERO)) continue; // Пропускаем контроллер
                structureMap.put(localPos, phantomSupplier);
            }
            
            // ВАЖНО: 
            // 1. Передаем specificPartShapes (def.getClosedShapes())
            // 2. Передаем specificCollisionShapes (def.getClosedShapes())
            // 3. Offset ставим в ZERO, так как координаты в def уже относительны контроллера (С=0,0,0)
            this.structureHelper = new MultiblockStructureHelper(
                structureMap, 
                phantomSupplier,
                null, // symbolRoleMap не нужен, роли уже распарсены
                null, // positionSymbolMap не нужен
                def.getClosedShapes(), // Формы для Outline
                def.getClosedShapes(), // Формы для Physics
                BlockPos.ZERO          // Смещение контроллера
            );
            
       } else {
           // === СТАРАЯ ЛОГИКА (FALLBACK) ===
           Map<BlockPos, Supplier<BlockState>> structureMap = createStructureForDoor(doorDeclId);
           this.structureHelper = new MultiblockStructureHelper(structureMap, phantomSupplier);
       }
       
       registerDefaultState(stateDefinition.any()
           .setValue(FACING, Direction.NORTH)
           .setValue(PART_ROLE, PartRole.DEFAULT)
           .setValue(DOOR_MOVING, false)
           .setValue(OPEN, false));
   }

    private static Map<BlockPos, Supplier<BlockState>> createStructureForDoor(String doorDeclId) {
        Map<BlockPos, Supplier<BlockState>> structureMap = new HashMap<>();
        Supplier<BlockState> phantomSupplier = () -> ModBlocks.UNIVERSAL_MACHINE_PART.get().defaultBlockState();

        // Получаем размеры двери на основе типа
        int[] dimensions = getDoorDimensions(doorDeclId);
        
        // dimensions = [offsetX, offsetY, offsetZ, sizeX, sizeY, sizeZ]
        int offsetX = dimensions[0];
        int offsetY = dimensions[1]; 
        int offsetZ = dimensions[2];
        int sizeX = dimensions[3];
        int sizeY = dimensions[4];
        int sizeZ = dimensions[5];

        // Генерируем структуру на основе размеров
        for (int x = offsetX; x <= offsetX + sizeX; x++) {
            for (int y = offsetY; y <= offsetY + sizeY; y++) {
                for (int z = offsetZ; z <= offsetZ + sizeZ; z++) {
                    if (x == 0 && y == 0 && z == 0) {
                        continue;
                    }
                    structureMap.put(new BlockPos(x, y, z), phantomSupplier);
                }
            }
        }

        return structureMap;
    }

    @Override
    public boolean isCollisionShapeFullBlock(BlockState state, BlockGetter world, BlockPos pos) {
        return false; // Это отключает логику "выталкивания" игрока изнутри блока
    }

    @Override
    public boolean isPathfindable(BlockState state, BlockGetter world, BlockPos pos, net.minecraft.world.level.pathfinder.PathComputationType type) {
        return false; // Помогает ИИ понимать, что здесь стена (когда закрыто)
    }

    /**
     * Получение размеров дверей для генерации структуры
     * Размеры берутся статически из switch, чтобы избежать проблем инициализации
     * (DoorDeclRegistry может быть недоступен во время генерации данных)
     */
    // public static int[] getDoorDimensions(String doorDeclId) {
    //     return switch (doorDeclId) {
    //         case "large_vehicle_door" -> new int[] { -3, 0, 0, 6, 5, 0 };
    //         case "round_airlock_door" -> new int[] { -1, 0, 0, 3, 3, 0 };
    //         case "transition_seal" -> new int[] { -12, 0, 0, 25, 23, 0 };
    //         case "fire_door" -> new int[] { -1, 0, 0, 3, 2, 0 };
    //         case "sliding_blast_door" -> new int[] { -3, 0, 0, 6, 3, 0 };
    //         case "sliding_seal_door" -> new int[] { 0, 0, 0, 0, 1, 0 };
    //         case "secure_access_door" -> new int[] { -2, 0, 0, 4, 4, 0 };
    //         case "qe_sliding_door" -> new int[] { 0, 0, 0, 1, 1, 0 };
    //         case "qe_containment_door" -> new int[] { -1, 0, 0, 2, 2, 0 };
    //         case "water_door" -> new int[] { -1, 0, 0, 2, 2, 0 };
    //         case "silo_hatch" -> new int[] { -2, 0, -2, 4, 0, 4 };
    //         case "silo_hatch_large" -> new int[] { -3, 0, -3, 6, 0, 6 };
    //         default -> new int[] { 0, 0, 0, 0, 1, 0 };
    //     };
    // }    

    public static int[] getDoorDimensions(String doorDeclId) {
        DoorDecl decl = DoorDeclRegistry.getById(doorDeclId);
        if (decl != null) return decl.getDimensions();
        return new int[] { 0,0,0,0,1,0 };
    }

    @Override
    public MultiblockStructureHelper getStructureHelper() {
        return structureHelper;
    }

    @Override
    public PartRole getPartRole(BlockPos localOffset) {
        DoorDecl decl = DoorDeclRegistry.getById(doorDeclId);
        if (decl != null && decl.getStructureDefinition() != null) {
            PartRole role = decl.getStructureDefinition().getRoles().get(localOffset);
            if (role != null) return role;
        }
        return PartRole.DEFAULT;
    }

    public String getDoorDeclId() {
        return doorDeclId;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new DoorBlockEntity(pos, state, doorDeclId);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide) {
            return null;
        }
        return createTickerHelper(type, ModBlockEntities.DOOR_ENTITY.get(), 
                (world, pos, blockState, blockEntity) -> DoorBlockEntity.serverTick(world, pos, blockState, (DoorBlockEntity) blockEntity));
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof DoorBlockEntity doorBE) {
            // Самого себя (контроллер) дергаем напрямую
            if (doorBE.isLocked()) {
                player.displayClientMessage(Component.translatable("door.locked"), true);
                return InteractionResult.FAIL;
            }
            if (doorBE.isMoving()) return InteractionResult.CONSUME;
            
            doorBE.toggle();
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        // 1. Получаем TE, но НЕ делаем жесткой зависимости от него
        BlockEntity be = level.getBlockEntity(pos);
        boolean isOpen = false;

        // Проверяем состояние только если TE существует
        if (be instanceof DoorBlockEntity doorBE) {
            isOpen = doorBE.getState() != 0; // 0 = закрыто, остальное = открыто/движется
        }

        // 2. Если дверь открыта (или открывается)
        if (isOpen) {
            DoorDecl decl = DoorDeclRegistry.getById(doorDeclId);
            if (decl != null && decl.getStructureDefinition() != null) {
                // Пытаемся взять форму открытого состояния из схемы
                // Для контроллера позиция всегда ZERO
                VoxelShape openShape = decl.getStructureDefinition().getOpenShapes().get(BlockPos.ZERO);
                if (openShape != null && !openShape.isEmpty()) {
                    return MultiblockStructureHelper.rotateShape(openShape, state.getValue(FACING));
                }
            }
            // Если в схеме пробел или формы нет -> проход свободен
            return Shapes.empty();
        }

        // 3. Если закрыта (ИЛИ если TE == null) -> возвращаем корректную тонкую форму!
        // Важно: мы используем structureHelper, который уже знает форму из конструктора.
        // Больше никакого Shapes.block() в конце!
        return structureHelper.getSpecificCollisionShape(BlockPos.ZERO, state.getValue(FACING));
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        DoorDecl decl = DoorDeclRegistry.getById(doorDeclId);
        
        // Если это люк, строим общую динамическую рамку
        if (decl != null && decl.isDynamicShape()) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof DoorBlockEntity doorBE) {
                // Передаем true в метод генерации, чтобы он учитывал открытость/закрытость
                return generateDynamicFullShape(state, level, pos, doorBE);
            }
        }

        // Стандартная полная рамка для обычных дверей
        return this.shapeCache.computeIfAbsent(state.getValue(FACING),
                facing -> getStructureHelper().generateShapeFromParts(facing));
    }

    private VoxelShape generateDynamicFullShape(BlockState state, BlockGetter level, BlockPos pos, DoorBlockEntity doorBE) {
        Direction facing = state.getValue(FACING);
        DoorDecl decl = doorBE.getDoorDecl();
        if (decl == null || decl.getStructureDefinition() == null) return Shapes.empty();
    
        VoxelShape combined = Shapes.empty();
        boolean isOpen = doorBE.getState() != 0;
        Map<BlockPos, VoxelShape> currentMap = isOpen ? decl.getStructureDefinition().getOpenShapes() : decl.getStructureDefinition().getClosedShapes();
    
        for (Map.Entry<BlockPos, VoxelShape> entry : currentMap.entrySet()) {
            VoxelShape partShape = entry.getValue();
            if (partShape.isEmpty()) continue;
    
            BlockPos relativePos = entry.getKey();
            // Вращаем позицию блока
            BlockPos rotatedPos = MultiblockStructureHelper.rotate(relativePos, facing);
            // Вращаем саму форму блока
            VoxelShape rotatedShape = MultiblockStructureHelper.rotateShape(partShape, facing);
            
            combined = Shapes.or(combined, rotatedShape.move(rotatedPos.getX(), rotatedPos.getY(), rotatedPos.getZ()));
        }
        return combined.optimize();
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock, BlockPos neighborPos, boolean isMoving) {
        super.neighborChanged(state, level, pos, neighborBlock, neighborPos, isMoving);
        if (level.isClientSide) return;

        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof DoorBlockEntity doorBE) {
            // Запускаем агрегированную проверку сигнала
            doorBE.checkRedstonePower();
        }
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        // MODEL — BakedModel запекается в чанк (нужно для Iris/Oculus).
        // ENTITYBLOCK_ANIMATED не использует BakedModel для world render.
        return RenderShape.MODEL;
    }


    @Override
    public VoxelShape getCustomMasterVoxelShape(BlockState state) {
        return Shapes.empty(); 
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState()
                .setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, PART_ROLE, DOOR_MOVING, OPEN);
    }


    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        if (!level.isClientSide && !oldState.is(this)) {
            Direction facing = state.getValue(FACING);
            structureHelper.placeStructure(level, pos, facing, this);
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof DoorBlockEntity doorBE) {
                doorBE.setControllerPos(pos);
                doorBE.onStructureFormed();
            }
        }
        super.onPlace(state, level, pos, oldState, isMoving);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock()) && !level.isClientSide) {
            Direction facing = state.getValue(FACING);
            structureHelper.destroyStructure(level, pos, facing);
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    @Override
    public int getLightBlock(BlockState pState, BlockGetter pLevel, BlockPos pPos) {
        return 0; // Полная прозрачность для движка света
    }

    @Override
    public float getShadeBrightness(BlockState pState, BlockGetter pLevel, BlockPos pPos) {
        return 1.0F; // Убирает тени под блоком и Ambient Occlusion
    }

    public boolean propagatesSkylightDown(BlockState pState, BlockGetter pLevel, BlockPos pPos) {
        return true;
    }

    // ---------------- КАМЕРА И ФИЗИКА ----------------

    @Override
    public VoxelShape getVisualShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
        return Shapes.empty();
    }

    @Override
    public VoxelShape getOcclusionShape(BlockState pState, BlockGetter pLevel, BlockPos pPos) {
        return Shapes.empty();
    }

    @Override
    public VoxelShape getBlockSupportShape(BlockState pState, BlockGetter pLevel, BlockPos pPos) {
        return Shapes.empty();
    }
}
