package com.hbm_m.explosion.vanillant.standard;

import com.hbm_m.explosion.vanillant.ExplosionVNT;
import com.hbm_m.explosion.vanillant.interfaces.IDropChanceMutator;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;

/** 1:1 {@code DropChanceMutatorStandard}: feste Dropchance. */
public class DropChanceMutatorStandard implements IDropChanceMutator {

    private float chance;

    public DropChanceMutatorStandard(float chance) {
        this.chance = chance;
    }

    @Override
    public float mutateDropChance(ExplosionVNT explosion, Block block, BlockPos pos, float chance) {
        return this.chance;
    }
}
