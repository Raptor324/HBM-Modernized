package com.hbm_m.block.gas;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

/**
 * 1:1-Port von {@code BlockGasExplosive} (1.7.10): wie das brennbare Gas, nur dass die Zuendung
 * zusaetzlich eine Explosion der Staerke 3 ausloest.
 */
public class BlockGasExplosive extends BlockGasFlammable {

    public BlockGasExplosive() {
        super(); // Original: erbt Farbe und Verhalten von BlockGasFlammable
    }

    @Override
    protected void combust(ServerLevel level, BlockPos pos) {
        super.combust(level, pos);
        level.explode(null, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D,
                3F, Level.ExplosionInteraction.TNT);
    }
}
