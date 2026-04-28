package com.hbm_m.block.machines;

import java.util.Map;
import java.util.function.Supplier;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.block.entity.ModBlockEntities;
import com.hbm_m.block.entity.machines.MachineFluidTankBlockEntity;
import com.hbm_m.interfaces.IMultiblockController;
import com.hbm_m.multiblock.MultiblockSideTuples;
import com.hbm_m.multiblock.MultiblockStructureHelper;
import com.hbm_m.multiblock.PartRole;

import dev.architectury.registry.menu.MenuRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
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
//? if forge {
/*import net.minecraftforge.common.capabilities.ForgeCapabilities;
*///?}


public class MachineFluidTankBlock extends BaseEntityBlock implements IMultiblockController {

    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    private final MultiblockStructureHelper structureHelper;

    public MachineFluidTankBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
        this.structureHelper = defineStructure();
    }

    /**
     * Определяем структуру 5x3x3 через слои.
     * 5 в ширину, 3 в высоту, 3 в глубину.
     * Контроллер 'C' находится в центре переднего ряда нижнего слоя.
     */
    private MultiblockStructureHelper defineStructure() {
        // Строка 0 - передняя (ближняя к игроку), Строка 2 - задняя.

        // Направления лестницы: MultiblockSideTuples.ladder(north, south, west, east) - локально к схеме до поворота FACING.
        String[] layer0 = {
            "LFCFA",
            "LEEEA",
            "AFAFA"
        };

        String[] layer1 = {
            "LAAAA",
            "LEEEA",
            "AAAAA"
        };

        String[] layer2 = {
            "LAAAA",
            "LAAAA",
            "AAAAA"
        };

        Map<Character, PartRole> roleMap = Map.of(
            'A', PartRole.DEFAULT,
            'C', PartRole.CONTROLLER,
            'L', PartRole.LADDER,
            'F', PartRole.FLUID_CONNECTOR
        );

        Map<Character, Supplier<BlockState>> symbolMap = Map.of(
        );

        Map<Character, boolean[]> ladderSideMap = Map.of(
            'L', MultiblockSideTuples.ladder(false, false, true, false)
        );

        return MultiblockStructureHelper.createFromLayersWithRolesAndSides(
            new String[][]{layer0, layer1, layer2},
            symbolMap,
            () -> ModBlocks.UNIVERSAL_MACHINE_PART.get().defaultBlockState(),
            roleMap,
            ladderSideMap,
            null
        );
    }

    // --- ЛОГИКА МУЛЬТИБЛОКА ---

    @Override
    public MultiblockStructureHelper getStructureHelper() {
        return this.structureHelper;
    }

    @Override
    public PartRole getPartRole(BlockPos localOffset) {
        // Используем хелпер для автоматического определения ролей из схемы
        return structureHelper.resolvePartRole(localOffset, this);
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        super.onPlace(state, level, pos, oldState, isMoving);
        if (!state.is(oldState.getBlock()) && !level.isClientSide()) {
            structureHelper.placeStructure(level, pos, state.getValue(FACING), this);
        }
    }

    @Override
    public void onRemove(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos, @NotNull BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock()) && !level.isClientSide()) {
            Direction facing = state.getValue(FACING);

            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof com.hbm_m.block.entity.BaseMachineBlockEntity be) {
                be.dropInventoryContents();
            }
            structureHelper.destroyStructure(level, pos, facing);
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    // 1. РАМКА ВЫДЕЛЕНИЯ: Показывает всю структуру целиком (3x3x3)
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
            // Берём форму ТОЛЬКО для позиции контроллера (С)
            // Она автоматически возьмётся из вашей shapeMap через хелпер
            return helper.getSpecificPartShape(helper.getControllerOffset(), pState.getValue(FACING));
        }
        return Shapes.block();
    }

    @Override
    public InteractionResult use(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos, @NotNull Player player, @NotNull InteractionHand hand, @NotNull BlockHitResult hit) {
        if (level.isClientSide) {
            return InteractionResult.sidedSuccess(true);
        }

        BlockEntity entity = level.getBlockEntity(pos);
        if (!(entity instanceof MachineFluidTankBlockEntity tank)) {
            return InteractionResult.PASS;
        }

        MenuRegistry.openExtendedMenu((ServerPlayer) player, tank, buf -> buf.writeBlockPos(pos));
        return InteractionResult.sidedSuccess(false);
    }

    @Override
    public RenderShape getRenderShape(@NotNull BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable @Override
    public BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
        return new MachineFluidTankBlockEntity(pos, state);
    }

    @Nullable @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(@NotNull Level level, @NotNull BlockState state, @NotNull BlockEntityType<T> type) {
        return createTickerHelper(type, ModBlockEntities.FLUID_TANK_BE.get(), MachineFluidTankBlockEntity::tick);
    }

    @Nullable @Override
    public BlockState getStateForPlacement(@NotNull BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected void createBlockStateDefinition(@NotNull StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }
}