package com.hbm_m.explosion;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

import com.hbm_m.block.ModBlocks;

/**
 * Спираль бейлфайра: всё стирается, камень превращается в слэйтед-селлафилд,
 * на поверхности остаётся огонь.
 */
public class ExplosionBalefire extends ExplosionFleija {

    public ExplosionBalefire(int x, int y, int z, Level level, int rad) {
        super(x, y, z, level, rad, 1.0F, 1.0F);
    }

    @Override
    protected void breakColumn(int x, int z) {
        if (!(level instanceof ServerLevel server)) return;
        int dist = this.radius2 - (x * x + z * z);
        if (dist <= 0) return;
        dist = (int) Math.sqrt(dist);
        BlockPos surface = null;
        for (int y = dist; y > -dist; y--) {
            BlockPos pos = new BlockPos(this.posX + x, this.posY + y, this.posZ + z);
            if (pos.getY() <= level.getMinBuildHeight() || pos.getY() >= level.getMaxBuildHeight()) continue;
            var state = server.getBlockState(pos);
            if (state.isAir()) continue;
            if (state.is(Blocks.STONE)) {
                server.setBlock(pos, ModBlocks.SELLAFIELD_SLAKED.get().defaultBlockState(), 3);
            } else {
                server.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                surface = pos;
            }
        }
        // Оставляем горящий след на уровне земли.
        if (surface != null && server.getBlockState(surface).isAir()) {
            server.setBlock(surface, Blocks.FIRE.defaultBlockState(), 3);
        }
    }
}
