package com.hbm_m.block.custom.machines;

import com.google.common.collect.ImmutableMap;
import com.hbm_m.block.ModBlocks;
import com.hbm_m.block.entity.ModBlockEntities;
import com.hbm_m.block.entity.custom.machines.HydraulicFrackiningTowerBlockEntity;
import com.hbm_m.multiblock.IMultiblockController;
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
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.function.Supplier;

/**
 * Hydraulic Frackining Tower (Multiblock).
 *
 * Modernized implementation: places a tall 3x3 tower of phantom parts.
 * The exact legacy 1.7.10 shape can be refined later, but this keeps
 * the machine consistent with the current MultiblockStructureHelper system.
 */
public class HydraulicFrackiningTowerBlock extends BaseEntityBlock implements IMultiblockController {

    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;

    private static MultiblockStructureHelper STRUCTURE_HELPER;

    public HydraulicFrackiningTowerBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    // Simple placeholder structure: 3x3 footprint, 16 blocks tall (controller at bottom center).
    // local coordinates: x,z in [-1..1], y in [0..15]
    private static Map<BlockPos, Supplier<BlockState>> defineStructure() {
        ImmutableMap.Builder<BlockPos, Supplier<BlockState>> builder = ImmutableMap.builder();
        Supplier<BlockState> phantom = () -> ModBlocks.UNIVERSAL_MACHINE_PART.get().defaultBlockState();

        for (int y = 0; y <= 15; y++) {
            for (int x = -1; x <= 1; x++) {
                for (int z = -1; z <= 1; z++) {
                    if (y == 0 && x == 0 && z == 0) continue; // controller
                    builder.put(new BlockPos(x, y, z), phantom);
                }
            }
        }

        return builder.build();
    }

    @Override
    public MultiblockStructureHelper getStructureHelper() {
        if (STRUCTURE_HELPER == null) {
            STRUCTURE_HELPER = new MultiblockStructureHelper(defineStructure(),
                    () -> ModBlocks.UNIVERSAL_MACHINE_PART.get().defaultBlockState());
        }
        return STRUCTURE_HELPER;
    }

    @Override
    public PartRole getPartRole(BlockPos localOffset) {
        int x = localOffset.getX();
        int y = localOffset.getY();
        int z = localOffset.getZ();

        // Energy connectors: 4 sides at ground level
        if (y == 0 && ((x == 0 && (z == -1 || z == 1)) || (z == 0 && (x == -1 || x == 1)))) {
            return PartRole.ENERGY_CONNECTOR;
        }

        // Fluid connectors: 4 sides near the top
        if (y == 15 && ((x == 0 && (z == -1 || z == 1)) || (z == 0 && (x == -1 || x == 1)))) {
            return PartRole.FLUID_CONNECTOR;
        }

        return PartRole.DEFAULT;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new HydraulicFrackiningTowerBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return null;
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
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        // No GUI yet; placeholder.
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.block();
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
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
