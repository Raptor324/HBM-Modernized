package com.hbm_m.api.tile;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

/**
 * 1:1 port of {@code com.hbm.tileentity.IOverpressurable}.
 *
 * <p>Implemented by machines that want to define their own failure mode when an RBMK meltdown
 * sends an overpressure surge down the steam network they are attached to. Anything on the
 * network that does not implement this is simply deleted and replaced with a plain explosion.</p>
 */
public interface IOverpressurable {

    void explode(Level level, BlockPos pos);
}
