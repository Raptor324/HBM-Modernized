package com.hbm_m.block.network;

import com.hbm_m.block.ModBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Port of {@code com.hbm.blocks.network.BlockConveyorChute} (1.7.10 Original) - funnels items
 * downward, speeding them up as they approach another belt/enterable block below (x5) or while
 * still above the funnel throat (x3). Sneak-screwdriver-click swaps back into a base
 * {@link ConveyorBlock}, completing the Conveyor→Lift→Chute→Conveyor cycle.
 */
public class ConveyorChuteBlock extends ConveyorBlockBase {

    private static final VoxelShape SHAPE_FULL = Shapes.block();

    public ConveyorChuteBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE_FULL;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE_FULL;
    }

    @Override
    public VoxelShape getVisualShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE_FULL;
    }

    private boolean isFunnelExit(Level level, BlockPos pos, Vec3 itemPos) {
        var below = level.getBlockState(pos.below()).getBlock();
        return below instanceof IConveyorBelt || below instanceof IEnterableBlock || itemPos.y > pos.getY() + 0.25;
    }

    @Override
    public Vec3 getTravelLocation(Level level, BlockPos pos, Vec3 itemPos, double speed) {
        var below = level.getBlockState(pos.below()).getBlock();
        if (below instanceof IConveyorBelt || below instanceof IEnterableBlock) {
            speed *= 5;
        } else if (itemPos.y > pos.getY() + 0.25) {
            speed *= 3;
        }
        return super.getTravelLocation(level, pos, itemPos, speed);
    }

    @Override
    public Direction getTravelDirection(Level level, BlockPos pos, Vec3 itemPos) {
        if (isFunnelExit(level, pos, itemPos)) return Direction.UP;
        return level.getBlockState(pos).getValue(FACING);
    }

    @Override
    public Vec3 getClosestSnappingPosition(Level level, BlockPos pos, Vec3 itemPos) {
        if (isFunnelExit(level, pos, itemPos)) {
            return new Vec3(pos.getX() + 0.5, itemPos.y, pos.getZ() + 0.5);
        }
        return super.getClosestSnappingPosition(level, pos, itemPos);
    }

    @Override
    protected BlockState onScrewSneak(Level level, BlockPos pos, BlockState state, Player player) {
        level.setBlock(pos, ModBlocks.CONVEYOR.get().defaultBlockState().setValue(FACING, state.getValue(FACING)), 3);
        return state;
    }
}
