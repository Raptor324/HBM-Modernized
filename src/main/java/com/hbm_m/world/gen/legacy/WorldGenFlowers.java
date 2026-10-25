package com.hbm_m.world.gen.legacy;

import java.util.Random;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelAccessor;

/** 1:1 {@code net.minecraft.world.gen.feature.WorldGenFlowers} (1.7.10): 64 Versuche im Umkreis. */
public class WorldGenFlowers {

    private LB flower;
    private int meta;

    public WorldGenFlowers(LB flower) {
        this.flower = flower;
    }

    /** {@code func_150550_a}. */
    public void setFlower(LB flower, int meta) {
        this.flower = flower;
        this.meta = meta;
    }

    public boolean generate(LevelAccessor world, Random rand, int x, int y, int z) {
        for (int l = 0; l < 64; ++l) {
            int i1 = x + rand.nextInt(8) - rand.nextInt(8);
            int j1 = y + rand.nextInt(4) - rand.nextInt(4);
            int k1 = z + rand.nextInt(8) - rand.nextInt(8);

            if (L.isAirBlock(world, i1, j1, k1) && j1 < 255 && L.canWrite(world, i1, k1)
                    && this.flower.state(this.meta).canSurvive(world, new BlockPos(i1, j1, k1))) {
                L.setBlock(world, i1, j1, k1, this.flower, this.meta, 2);
            }
        }

        return true;
    }
}
