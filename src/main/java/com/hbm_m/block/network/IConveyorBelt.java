package com.hbm_m.block.network;

import com.hbm_m.entity.conveyor.MovingConveyorItemEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** Port of {@code api.hbm.conveyor.IConveyorBelt} (1.7.10 Original). */
public interface IConveyorBelt {

    /** Returns true if the item should stay on the conveyor, false if the item should drop off. */
    boolean canItemStay(Level level, BlockPos pos, Vec3 itemPos);

    Vec3 getTravelLocation(Level level, BlockPos pos, Vec3 itemPos, double speed);

    Vec3 getClosestSnappingPosition(Level level, BlockPos pos, Vec3 itemPos);

    /** Used by the placing/collision logic to snap a freshly-spawned item onto the belt's lane. */
    default Vec3 snapNewItem(Level level, BlockPos pos, Vec3 itemPos) {
        return getClosestSnappingPosition(level, pos, itemPos);
    }
}
