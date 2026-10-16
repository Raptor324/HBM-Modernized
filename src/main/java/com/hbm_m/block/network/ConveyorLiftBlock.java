package com.hbm_m.block.network;

import com.hbm_m.block.ModBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
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

    private static final VoxelShape SHAPE_FULL = net.minecraft.world.level.block.Block.box(0, 0, 0, 16, 16, 16);
    private static final VoxelShape SHAPE_HALF = net.minecraft.world.level.block.Block.box(0, 0, 0, 16, 8, 16);

    public ConveyorLiftBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    private boolean isTop(BlockGetter level, BlockPos pos) {
        boolean bottom = !(level.getBlockState(pos.below()).getBlock() instanceof IConveyorBelt);
        return !(level.getBlockState(pos.above()).getBlock() instanceof IConveyorBelt) && !bottom
                && !(level.getBlockState(pos.above()).getBlock() instanceof IEnterableBlock);
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
        level.setBlock(pos, ModBlocks.CONVEYOR_CHUTE.get().defaultBlockState().setValue(FACING, state.getValue(FACING)), 3);
        return state;
    }
}
