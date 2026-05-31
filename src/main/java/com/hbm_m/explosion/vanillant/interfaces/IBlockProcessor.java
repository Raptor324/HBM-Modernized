package com.hbm_m.explosion.vanillant.interfaces;

import com.hbm_m.explosion.vanillant.ExplosionVNT;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

import java.util.HashSet;

public interface IBlockProcessor {
    void process(ExplosionVNT explosion, Level level, double x, double y, double z, HashSet<BlockPos> affectedBlocks);
}
