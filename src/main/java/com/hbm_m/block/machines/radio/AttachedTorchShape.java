package com.hbm_m.block.machines.radio;

import java.util.EnumMap;
import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Gemeinsame 1:1-Logik von {@code RadioTorchBase} und {@code DroneWaypoint}: die Auswahlbox reicht von der Haltewand
 * bis zur Mitte ({@code collisionRayTrace}), keine Kollision, und der Block faellt ab, wenn der Halt verschwindet
 * ({@code canBlockStay}). Im Port zeigt {@code FACING} in den Halteblock - die Original-Richtung ist das Gegenteil.
 */
public final class AttachedTorchShape {

    private static final Map<Direction, VoxelShape> SHAPES = new EnumMap<>(Direction.class);

    static {
        for (Direction facing : Direction.values()) {
            Direction dir = facing.getOpposite();
            SHAPES.put(facing, Block.box(
                    dir.getStepX() == 1 ? 0 : 6,
                    dir.getStepY() == 1 ? 0 : 6,
                    dir.getStepZ() == 1 ? 0 : 6,
                    dir.getStepX() == -1 ? 16 : 10,
                    dir.getStepY() == -1 ? 16 : 10,
                    dir.getStepZ() == -1 ? 16 : 10));
        }
    }

    private AttachedTorchShape() { }

    public static VoxelShape shape(Direction facing) {
        return SHAPES.get(facing);
    }

    /** Original {@code RadioTorchBase.canBlockStay}: feste Seite, Komparator-/Redstonequelle oder ein voller Block. */
    public static boolean canStay(LevelReader level, BlockPos pos, Direction facing) {
        BlockPos supportPos = pos.relative(facing);
        BlockState b = level.getBlockState(supportPos);
        return canStayWaypoint(level, pos, facing) || b.hasAnalogOutputSignal() || b.isSignalSource();
    }

    /** Original {@code DroneWaypoint}/{@code DroneWaypointRequest}: feste Seite oder ein voller Block. */
    public static boolean canStayWaypoint(LevelReader level, BlockPos pos, Direction facing) {
        BlockPos supportPos = pos.relative(facing);
        BlockState b = level.getBlockState(supportPos);
        Direction dir = facing.getOpposite();
        return b.isFaceSturdy(level, supportPos, dir) || (b.isCollisionShapeFullBlock(level, supportPos) && !b.isAir());
    }

    public static boolean isSupportSide(BlockGetter level, Direction facing, Direction updateDir) {
        return updateDir == facing;
    }
}
