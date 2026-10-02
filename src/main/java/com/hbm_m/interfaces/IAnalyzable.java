package com.hbm_m.interfaces;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

/** 1:1 {@code com.hbm.blocks.IAnalyzable}: Debug-Zeilen fuer das Analysewerkzeug. */
public interface IAnalyzable {

    List<String> getDebugInfo(Level world, BlockPos pos);
}
