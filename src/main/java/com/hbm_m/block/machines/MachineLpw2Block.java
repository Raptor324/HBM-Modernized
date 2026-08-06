package com.hbm_m.block.machines;

import java.util.Map;
import java.util.function.Supplier;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.blockentity.machines.MachineLpw2BlockEntity;
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
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * LPW2 - Port von {@code MachineLPW2} (1.7.10 Original, dort {@code BlockDummyable}), auf diesem
 * Repo-eigenen {@link IMultiblockController}-Framework. Footprint: 5 breit x 5 tief x 3 hoch,
 * Controller mittig unten - vereinfacht-aber-proportionaler Ersatz fuer das riesige
 * Original-Dimension-Array {@code {6,0,3,3,9,10}} (20 breit x 7 tief x 7 hoch), analog zu
 * {@link MachineLargeTurbineBlock}. Rein dekorativ (siehe Klassenkommentar in
 * {@link MachineLpw2BlockEntity}) - kein Ticker, kein GUI, Rechtsklick tut nichts.
 */
public class MachineLpw2Block extends BaseEntityBlock implements IMultiblockController {

    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    private final MultiblockStructureHelper structureHelper;

    public MachineLpw2Block(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
        this.structureHelper = defineStructure();
    }

    private static MultiblockStructureHelper defineStructure() {
        String[] bottom = { "OOOOO", "OOOOO", "OOCOO", "OOOOO", "OOOOO" };
        String[] mid    = { "OOOOO", "OOOOO", "OOOOO", "OOOOO", "OOOOO" };
        String[] top    = { "OOOOO", "OOOOO", "OOOOO", "OOOOO", "OOOOO" };

        Map<Character, PartRole> roleMap = Map.of(
                'O', PartRole.DEFAULT,
                'C', PartRole.CONTROLLER
        );

        Map<Character, Supplier<BlockState>> symbolMap = Map.of();

        return MultiblockStructureHelper.createFromLayersWithRoles(
                new String[][] { bottom, mid, top },
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

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return structureHelper.generateShapeFromParts(state.getValue(FACING));
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return structureHelper.getSpecificPartShape(structureHelper.getControllerOffset(), state.getValue(FACING));
    }

    @Override
    public VoxelShape getOcclusionShape(BlockState state, BlockGetter level, BlockPos pos) {
        if (!structureHelper.isFullBlock(structureHelper.getControllerOffset(), state.getValue(FACING))) {
            return Shapes.empty();
        }
        return Shapes.block();
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MachineLpw2BlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level,
            BlockState state,
            BlockEntityType<T> type
    ) {
        return null; // Rein dekorativ, kein Ticker.
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        super.onPlace(state, level, pos, oldState, isMoving);
        if (!state.is(oldState.getBlock()) && !level.isClientSide()) {
            structureHelper.placeStructure(level, pos, state.getValue(FACING), this);
        }
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos,
                                  Player player, InteractionHand hand, BlockHitResult hit) {
        return InteractionResult.PASS; // Kein GUI im Original.
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos,
                          BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            if (!level.isClientSide()) {
                structureHelper.destroyStructure(level, pos, state.getValue(FACING));
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }
}
