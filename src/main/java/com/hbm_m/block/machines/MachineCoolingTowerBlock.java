package com.hbm_m.block.machines;

import java.util.Map;
import java.util.function.Supplier;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.block.entity.ModBlockEntities;
import com.hbm_m.block.entity.machines.MachineCoolingTowerBlockEntity;
import com.hbm_m.interfaces.IMultiblockController;
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

/**
 * Cooling Tower (Multiblock, WIP).
 * Large cylindrical cooling tower for nuclear reactors.
 */
public class MachineCoolingTowerBlock extends BaseEntityBlock implements IMultiblockController {

    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;

    private final MultiblockStructureHelper structureHelper;

    public MachineCoolingTowerBlock(Properties pProperties) {
        super(pProperties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
        this.structureHelper = defineStructure();
    }

    private static MultiblockStructureHelper defineStructure() {
        // Cooling Tower: tall 5x5 circular/square structure, 13 layers
        // 'C' = CONTROLLER (Pflicht, genau 1!)
        // 'O' = DEFAULT (normale Strukturteile)
        // 'X' = LADDER (zentraler Kern)
        // '.' = leer

        String[] layer0 = {
            "..OOO..",
            ".OOOOO.",
            "OO.C.OO",
            "OOOOOOO",
            "OO...OO",
            ".OOOOO.",
            "..OOO.."
        };

        String[] layer1 = {
            "..OOO..",
            ".OOOOO.",
            "OO.X.OO",
            "OOOOOOO",
            "OO...OO",
            ".OOOOO.",
            "..OOO.."
        };

        String[] layerMid = {
            "..OOO..",
            ".O...O.",
            "O.....O",
            "O..X..O",
            "O.....O",
            ".O...O.",
            "..OOO.."
        };

        String[] layerTop = {
            "..OOO..",
            ".OOOOO.",
            "OOOOOOO",
            "OOOOOOO",
            "OOOOOOO",
            ".OOOOO.",
            "..OOO.."
        };

        Map<Character, PartRole> roleMap = Map.of(
            'O', PartRole.DEFAULT,
            'X', PartRole.LADDER,
            'C', PartRole.CONTROLLER
        );

        Map<Character, Supplier<BlockState>> symbolMap = Map.of();

        Map<Character, VoxelShape> shapeMap = Map.of(
            'C', Block.box(0, 0, 0, 16, 16, 16),
            'O', Block.box(0, 0, 0, 16, 16, 16),
            'X', Block.box(4, 0, 4, 12, 16, 12)
        );

        Map<Character, VoxelShape> collisionMap = Map.of(
            'C', Block.box(0, 0, 0, 16, 16, 16),
            'O', Block.box(0, 0, 0, 16, 16, 16),
            'X', Block.box(4, 0, 4, 12, 16, 12)
        );

        return MultiblockStructureHelper.createFromLayersWithRoles(
            new String[][]{
                layer0, layer1,
                layerMid, layerMid, layerMid, layerMid, layerMid,
                layerMid, layerMid, layerMid, layerMid, layerMid,
                layerTop
            },
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
        if (structureHelper != null) {
            return structureHelper.resolvePartRole(localOffset, this);
        }
        return PartRole.DEFAULT;
    }

    @Override
    public VoxelShape getOcclusionShape(BlockState state, BlockGetter level, BlockPos pos) {
        MultiblockStructureHelper helper = getStructureHelper();
        if (helper != null && !helper.isFullBlock(helper.getControllerOffset(), state.getValue(FACING))) {
            return Shapes.empty();
        }
        return Shapes.block();
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MachineCoolingTowerBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ModBlockEntities.COOLING_TOWER_BE.get(),
                MachineCoolingTowerBlockEntity::tick);
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
        // WIP - keine GUI vorhanden
        return InteractionResult.sidedSuccess(pLevel.isClientSide());
    }

    @Override
    public VoxelShape getShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
        MultiblockStructureHelper helper = getStructureHelper();
        if (helper != null) {
            return helper.generateShapeFromParts(pState.getValue(FACING));
        }
        return Shapes.block();
    }

    @Override
    public VoxelShape getCollisionShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
        MultiblockStructureHelper helper = getStructureHelper();
        if (helper != null) {
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
