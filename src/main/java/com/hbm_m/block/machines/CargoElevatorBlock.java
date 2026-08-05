package com.hbm_m.block.machines;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.machines.CargoElevatorBlockEntity;
import com.hbm_m.interfaces.IMultiblockController;
import com.hbm_m.multiblock.MultiblockStructureHelper;
import com.hbm_m.multiblock.PartRole;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
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
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Порт {@code BlockCargoElevator} из 1.7.10 (extends BlockDummyable).
 * <p>
 * 3×3 база-мультиблок с вертикально-выдвижной платформой.
 * ПКМ с блоком лифта → увеличение высоты (добавление слоёв сверху).
 * ПКМ без блока → toggle (выдвинуть/задвинуть платформу).
 * <p>
 * API gap: оригинал использует {@code BlockDummyable} (метаданные для core/dummy).
 * В Modernized — {@link MultiblockStructureHelper} с {@code UNIVERSAL_MACHINE_PART}
 * как filler. Это стандартный паттерн проекта.
 */
public class CargoElevatorBlock extends BaseEntityBlock implements IMultiblockController {

    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;

    private final MultiblockStructureHelper structureHelper;

    public CargoElevatorBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
        this.structureHelper = defineStructure();
    }

    /**
     * Оригинал: {@code getDimensions() = {0, 0, 1, 1, 1, 1}}, {@code getOffset() = 1}.
     * 3×3×1 база (1 блок в каждую сторону по горизонтали).
     */
    private static MultiblockStructureHelper defineStructure() {
        String[] controllerLayer = {
            "OOO",
            "OEO",
            "OOO"
        };
        String[] fillerLayer = {
            "OOO",
            "OOO",
            "OOO"
        };

        Map<Character, PartRole> roleMap = Map.of(
                'O', PartRole.DEFAULT,
                'E', PartRole.CONTROLLER
        );

        Map<Character, Supplier<BlockState>> symbolMap = Map.of();

        return MultiblockStructureHelper.createFromLayersWithRoles(
                new String[][] { controllerLayer, fillerLayer },
                symbolMap,
                () -> ModBlocks.UNIVERSAL_MACHINE_PART.get().defaultBlockState(),
                roleMap,
                null,
                null
        );
    }

    @Override
    public MultiblockStructureHelper getStructureHelper() {
        return this.structureHelper;
    }

    @Override
    public PartRole getPartRole(BlockPos localOffset) {
        return structureHelper.resolvePartRole(localOffset, this);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CargoElevatorBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ModBlockEntities.CARGO_ELEVATOR_BE.get(), CargoElevatorBlockEntity::tick);
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        super.onPlace(state, level, pos, oldState, isMoving);
        if (!state.is(oldState.getBlock()) && !level.isClientSide()) {
            structureHelper.placeStructure(level, pos, state.getValue(FACING), this);
        }
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock()) && !level.isClientSide()) {
            structureHelper.destroyStructure(level, pos, state.getValue(FACING));
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    @Override
    public boolean canSurvive(BlockState state, net.minecraft.world.level.LevelReader level, BlockPos pos) {
        return super.canSurvive(state, level, pos) && canSurviveMultiblockPlacement(state, level, pos);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.sidedSuccess(true);
        if (player.isShiftKeyDown()) return InteractionResult.PASS;

        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof CargoElevatorBlockEntity elevator)) return InteractionResult.PASS;

        ItemStack heldItem = player.getItemInHand(hand);

        // ПКМ с блоком лифта → увеличение высоты
        if (!heldItem.isEmpty() && heldItem.getItem() instanceof BlockItem bi && bi.getBlock() == this) {
            int targetY = pos.getY() + elevator.height + 1;
            boolean replaceable = true;
            for (int x = pos.getX() - 1; x <= pos.getX() + 1; x++) {
                for (int z = pos.getZ() - 1; z <= pos.getZ() + 1; z++) {
                    BlockState targetState = level.getBlockState(new BlockPos(x, targetY, z));
                    if (!targetState.canBeReplaced()) {
                        replaceable = false;
                        break;
                    }
                }
                if (!replaceable) break;
            }

            if (replaceable) {
                for (int x = pos.getX() - 1; x <= pos.getX() + 1; x++) {
                    for (int z = pos.getZ() - 1; z <= pos.getZ() + 1; z++) {
                        level.setBlock(new BlockPos(x, targetY, z), ModBlocks.UNIVERSAL_MACHINE_PART.get().defaultBlockState(), 3);
                    }
                }
                elevator.height++;
                elevator.setChanged();
                level.sendBlockUpdated(pos, state, state, 3);
                if (!player.getAbilities().instabuild) {
                    heldItem.shrink(1);
                }
            }
            return InteractionResult.CONSUME;
        } else {
            // ПКМ без блока → toggle
            elevator.toggleElevator();
            elevator.setChanged();
            level.sendBlockUpdated(pos, state, state, 3);
            return InteractionResult.CONSUME;
        }
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, net.minecraft.world.level.storage.loot.LootParams.Builder builder) {
        // Оригинал: getDrops → (height + 1) предметов
        BlockEntity be = builder.getOptionalParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.BLOCK_ENTITY);
        if (be instanceof CargoElevatorBlockEntity elevator) {
            int toDrop = elevator.height + 1;
            List<ItemStack> drops = new ArrayList<>();
            while (toDrop > 0) {
                int perStack = Math.min(toDrop, 64);
                toDrop -= perStack;
                drops.add(new ItemStack(this, perStack));
            }
            return drops;
        }
        return super.getDrops(state, builder);
    }

    /**
     * Кастомные collision boxes: 4 угловых столба + плита платформы.
     * Оригинал: {@code addCollisionBoxesToList} → {@code getAABBs(elevator, x, y, z)}.
     */
    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof CargoElevatorBlockEntity elevator)) {
            return Shapes.empty();
        }
        return getElevatorCollisionShape(elevator);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.block();
    }

    @Override
    public VoxelShape getOcclusionShape(BlockState state, BlockGetter level, BlockPos pos) {
        return Shapes.empty();
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

    /**
     * 4 угловых столба (0.25×height×0.25) + плита платформы (3×0.25×3 на высоте extension).
     */
    private static VoxelShape getElevatorCollisionShape(CargoElevatorBlockEntity elevator) {
        int h = elevator.height + 1;
        double ext = elevator.extension;

        VoxelShape cornerNW = Block.box(0, 0, 0, 4, h * 16, 4);
        VoxelShape cornerNE = Block.box(12, 0, 0, 16, h * 16, 4);
        VoxelShape cornerSW = Block.box(0, 0, 12, 4, h * 16, 16);
        VoxelShape cornerSE = Block.box(12, 0, 12, 16, h * 16, 16);

        // Плита платформы: 3×0.25×3 на высоте (0.75 + extension)
        double slabY = (0.75 + ext) * 16.0;
        int slabYInt = (int) Math.floor(slabY);
        int slabHeight = (int) Math.ceil((1.0 + ext) * 16.0) - slabYInt;
        if (slabHeight < 1) slabHeight = 1;
        VoxelShape platform = Block.box(0, slabYInt, 0, 16, Math.min(slabYInt + slabHeight, 256), 16);

        VoxelShape result = Shapes.joinUnoptimized(cornerNW, cornerNE, BooleanOp.OR);
        result = Shapes.joinUnoptimized(result, cornerSW, BooleanOp.OR);
        result = Shapes.joinUnoptimized(result, cornerSE, BooleanOp.OR);
        result = Shapes.joinUnoptimized(result, platform, BooleanOp.OR);
        return result;
    }
}