package com.hbm_m.block.network;

import com.hbm_m.block.ModBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.Vec3;

/**
 * Port of {@code com.hbm.blocks.network.BlockConveyorLift} (1.7.10 Original) - vertical conveyor
 * segment. Direction is computed dynamically from neighboring blocks each call rather than stored:
 * the bottom of a lift stack (nothing conveyor-like below) and any tile blocked from continuing
 * upward both resolve to travel=DOWN, which - combined with an unchanged-Y snapping target - is
 * the original's trick to produce a net *upward* nudge via the shared {@code getTravelLocation}
 * dest-offset formula (see {@link ConveyorBlockBase#getTravelLocation}); ported literally rather
 * than "simplified" since the exact behavior depends on that formula's sign flip.
 */
public class ConveyorLiftBlock extends ConveyorBlockBase {

    public enum LiftSegment implements StringRepresentable {
        BOTTOM("bottom"),
        MIDDLE("middle"),
        TOP("top");

        private final String name;

        LiftSegment(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return name;
        }
    }

    public static final EnumProperty<LiftSegment> SEGMENT = EnumProperty.create("segment", LiftSegment.class);

    private static final VoxelShape SHAPE_FULL = net.minecraft.world.level.block.Block.box(0, 0, 0, 16, 16, 16);
    private static final VoxelShape SHAPE_HALF = net.minecraft.world.level.block.Block.box(0, 0, 0, 16, 8, 16);

    public ConveyorLiftBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.defaultBlockState().setValue(SEGMENT, LiftSegment.MIDDLE));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(SEGMENT);
    }

    public static LiftSegment computeSegment(BlockGetter level, BlockPos pos) {
        boolean bottom = !(level.getBlockState(pos.below()).getBlock() instanceof IConveyorBelt);
        if (bottom) return LiftSegment.BOTTOM;
        boolean top = !(level.getBlockState(pos.above()).getBlock() instanceof IConveyorBelt)
                && !(level.getBlockState(pos.above()).getBlock() instanceof IEnterableBlock);
        return top ? LiftSegment.TOP : LiftSegment.MIDDLE;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState base = super.getStateForPlacement(context);
        if (base == null) return null;
        return base.setValue(SEGMENT, computeSegment(context.getLevel(), context.getClickedPos()));
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        super.onPlace(state, level, pos, oldState, isMoving);
        if (!level.isClientSide() && !state.is(oldState.getBlock())) {
            LiftSegment desired = computeSegment(level, pos);
            if (state.getValue(SEGMENT) != desired) {
                level.setBlock(pos, state.setValue(SEGMENT, desired), 3);
            }
        }
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
            LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        if (direction == Direction.UP || direction == Direction.DOWN) {
            return state.setValue(SEGMENT, computeSegment(level, pos));
        }
        return state;
    }

    private boolean isTop(BlockGetter level, BlockPos pos) {
        return computeSegment(level, pos) == LiftSegment.TOP;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return isTop(level, pos) ? SHAPE_HALF : SHAPE_FULL;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return getShape(state, level, pos, context);
    }

    @Override
    public VoxelShape getVisualShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return getShape(state, level, pos, context);
    }

    @Override
    public Direction getInputDirection(BlockState state) {
        return Direction.DOWN;
    }

    @Override
    public Direction getOutputDirection(BlockState state) {
        return Direction.UP;
    }

    @Override
    public Direction getTravelDirection(Level level, BlockPos pos, Vec3 itemPos) {
        if (!isTop(level, pos)) return Direction.DOWN;
        return level.getBlockState(pos).getValue(FACING);
    }

    @Override
    public Vec3 getClosestSnappingPosition(Level level, BlockPos pos, Vec3 itemPos) {
        if (!isTop(level, pos)) {
            return new Vec3(pos.getX() + 0.5, itemPos.y, pos.getZ() + 0.5);
        }
        return super.getClosestSnappingPosition(level, pos, itemPos);
    }

    @Override
    protected BlockState onScrewSneak(Level level, BlockPos pos, BlockState state, Player player) {
        return ModBlocks.CONVEYOR_CHUTE.get().defaultBlockState().setValue(FACING, state.getValue(FACING));
    }
}
