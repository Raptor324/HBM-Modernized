package com.hbm_m.block.decorations;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.block.entity.doors.DoorBlockEntity;
import com.hbm_m.block.entity.doors.DoorDecl;
import com.hbm_m.block.entity.doors.DoorDeclRegistry;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.interfaces.IMultiblockController;
import com.hbm_m.item.ModItems;
import com.hbm_m.multiblock.MultiblockStructureHelper;
import com.hbm_m.multiblock.PartRole;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
import net.minecraft.world.level.pathfinder.PathComputationType;
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
            
            for (BlockPos localPos : def.getClosedShapes().keySet()) {
                if (localPos.equals(BlockPos.ZERO)) continue; // Пропускаем контроллер
                structureMap.put(localPos, phantomSupplier);
            }
            
            this.structureHelper = new MultiblockStructureHelper(
                structureMap, 
                phantomSupplier,
                null, 
                null, 
                def.getClosedShapes(), 
                def.getClosedShapes(), 
                BlockPos.ZERO          
            );
            
       } else {
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

        int[] dimensions = getDoorDimensions(doorDeclId);
        
        int offsetX = dimensions[0];
        int offsetY = dimensions[1]; 
        int offsetZ = dimensions[2];
        int sizeX = dimensions[3];
        int sizeY = dimensions[4];
        int sizeZ = dimensions[5];

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
        return false;
    }

    @Override
    public boolean isPathfindable(BlockState state, BlockGetter level, BlockPos pos, PathComputationType type) {
        return switch (type) {
            case LAND, AIR -> state.getValue(OPEN);
            default -> false;
        };
    }

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
        if (hasScrewdriver(player)) {
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        if (!level.isClientSide) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof DoorBlockEntity doorBE) {
                doorBE.toggle();
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    private static boolean hasScrewdriver(Player player) {
        return player.getItemInHand(InteractionHand.MAIN_HAND).getItem() == ModItems.SCREWDRIVER.get()
                || player.getItemInHand(InteractionHand.OFF_HAND).getItem() == ModItems.SCREWDRIVER.get()
                || player.getItemInHand(InteractionHand.MAIN_HAND).getItem() == ModItems.SCREWDRIVER_DESH.get()
                || player.getItemInHand(InteractionHand.OFF_HAND).getItem() == ModItems.SCREWDRIVER_DESH.get();
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        // Контрапшен: DoorBlock полностью берет на себя объединенную коллизию, чтобы 
        // игроку не пришлось выделять все фантомные блоки суперклеем
        if (level instanceof Level lvl && com.hbm_m.compat.ContraptionDoorState.isContraptionWorld(lvl)) {
            VoxelShape cached = com.hbm_m.compat.ContraptionDoorState.getShape(lvl, pos);
            if (cached != null) return cached;
        }
        
        BlockEntity be = level.getBlockEntity(pos);
        boolean isOpen;
        if (be instanceof DoorBlockEntity doorBE) {
            isOpen = doorBE.getState() != 0; 
        } else {
            isOpen = state.getValue(OPEN);
        }

        if (isOpen) {
            DoorDecl decl = DoorDeclRegistry.getById(doorDeclId);
            if (decl != null && decl.getStructureDefinition() != null) {
                VoxelShape openShape = decl.getStructureDefinition().getOpenShapes().get(BlockPos.ZERO);
                if (openShape != null && !openShape.isEmpty()) {
                    return MultiblockStructureHelper.rotateShape(openShape, state.getValue(FACING));
                }
            }
            return Shapes.empty();
        }

        return structureHelper.getSpecificCollisionShape(BlockPos.ZERO, state.getValue(FACING));
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (level instanceof Level lvl && com.hbm_m.compat.ContraptionDoorState.isContraptionWorld(lvl)) {
            VoxelShape cached = com.hbm_m.compat.ContraptionDoorState.getShape(lvl, pos);
            if (cached != null) return cached;
        }
        
        DoorDecl decl = DoorDeclRegistry.getById(doorDeclId);
        
        if (decl != null && decl.isDynamicShape()) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof DoorBlockEntity doorBE) {
                return generateDynamicFullShape(state, level, pos, doorBE);
            }
        }

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
            BlockPos rotatedPos = MultiblockStructureHelper.rotate(relativePos, facing);
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
            doorBE.checkRedstonePower();
        }
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        // По умолчанию RenderShape.MODEL —DoorBakedModel skip-ает world-quads через
        // shouldSkipWorldRendering, а DoorRenderer рисует анимированную геометрию через
        // BlockEntityRenderer (VBO/instanced). Это рабочий паттерн, который НЕ ломает
        // контрапшены Create: при разборке поезда Create возвращает блоки из
        // contraption.getBlocks() в мир через level.setBlock, и с RenderShape.MODEL
        // блок появляется в chunk-render сразу, без задержки/дублирования.
        //
        // ENTITYBLOCK_ANIMATED отключает chunk-bake полностью, оставляя только BER — это
        // было нужно для DAE-дверей (getColladaAnimationSource() != null), чей
        // DaeBakedModel иначе отдаёт в chunk «кривую» статичную геометрию без поворота по
        // FACING и без анимации (Sliding Blast Door: 2 пайплайна OBJ + DAE).
        //
        // НО: ENTITYBLOCK_ANIMATED ломает разборку составных contraption-поездов Create
        // — реальный блок-дверь (состояние из StructureBlockInfo) перестаёт появляться в
        // мире синхронно с удалением сущности-конрапшена; остается «двойной рендер»
        // (сущность + реальная копия), часть склеенных блоков бесследно пропадает.
        // Поэтому: только DAE-двери используют ENTITYBLOCK_ANIMATED, остальные — MODEL.
        DoorDecl decl = DoorDeclRegistry.getById(doorDeclId);
        if (decl != null && decl.getColladaAnimationSource() != null) {
            return RenderShape.ENTITYBLOCK_ANIMATED;
        }
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
            BlockPos core = placeMultiblockStructure(level, pos, state);
            if (core == null) {
                return;
            }
            BlockEntity be = level.getBlockEntity(core);
            if (be instanceof DoorBlockEntity doorBE) {
                doorBE.setControllerPos(core);
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
        return 0;
    }

    @Override
    public float getShadeBrightness(BlockState pState, BlockGetter pLevel, BlockPos pPos) {
        return 1.0F; 
    }

    public boolean propagatesSkylightDown(BlockState pState, BlockGetter pLevel, BlockPos pPos) {
        return true;
    }

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

    //? if >1.20.1 {
    /*public static final com.mojang.serialization.MapCodec<DoorBlock> CODEC = simpleCodec(DoorBlock::new);

    @Override
    protected com.mojang.serialization.MapCodec<? extends net.minecraft.world.level.block.BaseEntityBlock> codec() {
        return CODEC;
    }
    *///?}
}