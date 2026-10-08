package com.hbm_m.world.gen.legacy;

import java.util.Random;

import net.minecraft.util.Mth;
import net.minecraft.world.level.LevelAccessor;

/** 1:1 {@code net.minecraft.world.gen.feature.WorldGenMinable} (1.7.10): spindelfoermige Erzader. */
public class WorldGenMinable {

    private final LB block;
    private final int meta;
    private final int numberOfBlocks;
    private final LB target;

    public WorldGenMinable(LB block, int number) {
        this(block, 0, number, VB.stone);
    }

    public WorldGenMinable(LB block, int number, LB target) {
        this(block, 0, number, target);
    }

    public WorldGenMinable(LB block, int meta, int number, LB target) {
        this.block = block;
        this.meta = meta;
        this.numberOfBlocks = number;
        this.target = target;
    }

    public boolean generate(LevelAccessor world, Random rand, int x, int y, int z) {
        float f = rand.nextFloat() * (float) Math.PI;
        double d0 = (double) ((float) (x + 8) + Mth.sin(f) * (float) this.numberOfBlocks / 8.0F);
        double d1 = (double) ((float) (x + 8) - Mth.sin(f) * (float) this.numberOfBlocks / 8.0F);
        double d2 = (double) ((float) (z + 8) + Mth.cos(f) * (float) this.numberOfBlocks / 8.0F);
        double d3 = (double) ((float) (z + 8) - Mth.cos(f) * (float) this.numberOfBlocks / 8.0F);
        double d4 = (double) (y + rand.nextInt(3) - 2);
        double d5 = (double) (y + rand.nextInt(3) - 2);

        for (int l = 0; l <= this.numberOfBlocks; ++l) {
            double d6 = d0 + (d1 - d0) * (double) l / (double) this.numberOfBlocks;
            double d7 = d4 + (d5 - d4) * (double) l / (double) this.numberOfBlocks;
            double d8 = d2 + (d3 - d2) * (double) l / (double) this.numberOfBlocks;
            double d9 = rand.nextDouble() * (double) this.numberOfBlocks / 16.0D;
            double d10 = (double) (Mth.sin((float) l * (float) Math.PI / (float) this.numberOfBlocks) + 1.0F) * d9 + 1.0D;
            double d11 = (double) (Mth.sin((float) l * (float) Math.PI / (float) this.numberOfBlocks) + 1.0F) * d9 + 1.0D;
            int i1 = Mth.floor(d6 - d10 / 2.0D);
            int j1 = Mth.floor(d7 - d11 / 2.0D);
            int k1 = Mth.floor(d8 - d10 / 2.0D);
            int l1 = Mth.floor(d6 + d10 / 2.0D);
            int i2 = Mth.floor(d7 + d11 / 2.0D);
            int j2 = Mth.floor(d8 + d10 / 2.0D);

            for (int k2 = i1; k2 <= l1; ++k2) {
                double d12 = ((double) k2 + 0.5D - d6) / (d10 / 2.0D);

                if (d12 * d12 < 1.0D) {
                    for (int l2 = j1; l2 <= i2; ++l2) {
                        double d13 = ((double) l2 + 0.5D - d7) / (d11 / 2.0D);

                        if (d12 * d12 + d13 * d13 < 1.0D) {
                            for (int i3 = k1; i3 <= j2; ++i3) {
                                double d14 = ((double) i3 + 0.5D - d8) / (d10 / 2.0D);

                                if (d12 * d12 + d13 * d13 + d14 * d14 < 1.0D && L.isReplaceableOreGen(world, k2, l2, i3, this.target)) {
                                    L.setBlock(world, k2, l2, i3, this.block, this.meta, 2);
                                }
                            }
                        }
                    }
                }
            }
        }

        return true;
    }
}
