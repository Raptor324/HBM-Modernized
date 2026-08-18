package com.hbm_m.block.network;

import com.hbm_m.entity.conveyor.MovingConveyorItemEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

/**
 * Port of {@code api.hbm.conveyor.IEnterableBlock} (1.7.10 Original). Blocks implementing this
 * (e.g. machine input slots) can absorb a {@link MovingConveyorItemEntity} that walks into them.
 * <p>
 * Simplification: the original's separate {@code IConveyorPackage} overload (used by RBMK
 * autoloaders in the original) is not ported since this port has no equivalent package-conveyor
 * payload type - only single-item conveyor entities exist here.
 */
public interface IEnterableBlock {
    void onItemEnter(Level level, BlockPos pos, MovingConveyorItemEntity item);
}
