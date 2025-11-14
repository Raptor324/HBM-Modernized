package com.hbm_m.block;

import com.hbm_m.block.entity.DoorBlockEntity;
import com.hbm_m.block.entity.DoorDeclRegistry;
import com.hbm_m.block.entity.ModBlockEntities;
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
import net.minecraft.world.level.block.*;
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
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class DoorBlock extends BaseEntityBlock implements IMultiblockController {

    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final EnumProperty<PartRole> PART_ROLE = EnumProperty.create("part_role", PartRole.class);
    public static final BooleanProperty OPEN = BooleanProperty.create("open");
    private final Map<Direction, VoxelShape> shapeCache = new java.util.EnumMap<>(Direction.class);

    private final String doorDeclId;
    private final MultiblockStructureHelper structureHelper;

    public DoorBlock(Properties properties, String doorDeclId) {
        super(properties);
        this.doorDeclId = doorDeclId;
        
        // Создаём структуру ТОЛЬКО один раз и переиспользуем
        Map<BlockPos, Supplier<BlockState>> structureMap = createStructureForDoor(doorDeclId);
        
        this.structureHelper = new MultiblockStructureHelper(structureMap, 
            () -> ModBlocks.UNIVERSAL_MACHINE_PART.get().defaultBlockState());
        
        registerDefaultState(stateDefinition.any()
            .setValue(FACING, Direction.NORTH)
            .setValue(PART_ROLE, PartRole.DEFAULT)
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
                    structureMap.put(new BlockPos(x, y, z), phantomSupplier);
                }
            }
        }

        return structureMap;
    }

    /**
     * Получение размеров дверей для генерации структуры
     * Размеры берутся из DoorDecl, но статически, чтобы избежать проблем инициализации
     */
    
     public static int[] getDoorDimensions(String doorDeclId) {
        return DoorDeclRegistry.getById(doorDeclId).getDimensions();
    }    

    @Override
    public MultiblockStructureHelper getStructureHelper() {
        return structureHelper;
    }

    @Override
    public PartRole getPartRole(BlockPos localOffset) {
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
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof DoorBlockEntity doorBE) {
            DoorBlockEntity controller = doorBE.getController();
            if (controller != null) {
                // ИСПРАВЛЕНО: Получаем DoorDecl ТОЛЬКО на клиенте
                if (controller.isLocked()) {
                    // Для сервера используем простое сообщение
                    player.displayClientMessage(Component.translatable("door.locked"), true);
                    return InteractionResult.FAIL;
                }
                
                if (controller.isMoving()) {
                    return InteractionResult.CONSUME;
                }
                
                controller.toggle();
                return InteractionResult.SUCCESS;
            }
        }
        return InteractionResult.PASS;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    public VoxelShape getShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
        // Используем НЕСТАТИЧЕСКИЙ кэш
        return this.shapeCache.computeIfAbsent(pState.getValue(FACING),
                facing -> getStructureHelper().generateShapeFromParts(facing));
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState()
                .setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, PART_ROLE, OPEN);
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

    
}
