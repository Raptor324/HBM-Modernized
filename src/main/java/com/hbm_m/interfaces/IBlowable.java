package com.hbm_m.interfaces;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

/**
 * Verbatim port of NEXT {@code com.hbm.interfaces.IBlowable}: anything a machine fan can blow
 * through. SCAFFOLD ONLY - no Modernized block implements the fan side yet (machine_fan is
 * unported), so {@link #applyFan} currently has no caller. Accepted deviation, as with the
 * blast_door report.
 */
public interface IBlowable {

    void applyFan(Level level, BlockPos pos, Direction dir, int dist);
}
