package com.hbm_m.block.gas;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.Level.ExplosionInteraction;

/**
 * Взрывоопасный газ: как горючий, но при воспламенении взрывается.
 * Порт {@link com.hbm.blocks.gas.BlockGasExplosive} (1.7.10).
 */
public class BlockGasExplosive extends BlockGasFlammable {

    public BlockGasExplosive() {
        super();
    }

    @Override
    protected void combust(Level level, BlockPos pos) {
        super.combust(level, pos);
        level.explode(null, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, 3.0F, ExplosionInteraction.TNT);
    }
}
