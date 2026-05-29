package com.hbm_m.explosion.vanillant.interfaces;

import com.hbm_m.explosion.vanillant.ExplosionVNT;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;

public interface IDropChanceMutator {
    float mutateDropChance(ExplosionVNT explosion, Block block, BlockPos pos, float chance);
}