package com.hbm_m.world.feature;

import com.hbm_m.world.gen.legacy.L;
import com.hbm_m.world.gen.legacy.LB;
import com.hbm_m.world.gen.legacy.NoiseGeneratorPerlin;
import com.hbm_m.world.gen.legacy.VB;

import net.minecraft.world.level.LevelAccessor;

/**
 * 1:1 {@code com.hbm.world.feature.OreLayer3D}: Erzlagen aus drei Rauschfeldern (Haematit, Bauxit, Malachit)
 * zwischen y 6 und 64. Der Original-Fehler (nz liest ebenfalls cacheX) ist beibehalten.
 */
public class OreLayer3D {

    public static int counter = 0;
    public int id;

    private long lastSeed;
    private NoiseGeneratorPerlin noiseX;
    private NoiseGeneratorPerlin noiseY;
    private NoiseGeneratorPerlin noiseZ;

    double scaleH;
    double scaleV;
    double threshold;

    LB block;
    int meta;
    int dim = 0;

    public OreLayer3D(LB block, int meta) {
        this.block = block;
        this.meta = meta;
        this.id = counter;
        counter++;
    }

    public OreLayer3D setDimension(int dim) {
        this.dim = dim;
        return this;
    }

    public OreLayer3D setScaleH(double scale) {
        this.scaleH = scale;
        return this;
    }

    public OreLayer3D setScaleV(double scale) {
        this.scaleV = scale;
        return this;
    }

    public OreLayer3D setThreshold(double threshold) {
        this.threshold = threshold;
        return this;
    }

    public void onDecorate(LevelAccessor world, long seed, int cx, int cz) {

        NoiseGeneratorPerlin noiseX, noiseY;
        synchronized (this) {
            // Caches beim ersten Lauf und bei neuem Weltseed erneuern
            if (this.noiseX == null || seed != lastSeed) {
                this.noiseX = new NoiseGeneratorPerlin(seed + 101 + id, 4);
                this.noiseY = new NoiseGeneratorPerlin(seed + 102 + id, 4);
                this.noiseZ = new NoiseGeneratorPerlin(seed + 103 + id, 4);
                lastSeed = seed;
            }
            noiseX = this.noiseX;
            noiseY = this.noiseY;
        }

        double[][] cacheX = new double[16][65];

        for (int o = 0; o < 16; o++) {
            for (int y = 64; y > 5; y--) {
                cacheX[o][y] = noiseX.getValue(y * scaleV, (cz + 8 + o) * scaleH);
                // cacheZ des Originals wird berechnet, aber nie gelesen
            }
        }

        for (int ox = 0; ox < 16; ox++) {
            int x = cx + 8 + ox;

            for (int oz = 0; oz < 16; oz++) {
                int z = cz + 8 + oz;

                double ny = noiseY.getValue(x * scaleH, z * scaleH);

                for (int y = 64; y > 5; y--) {
                    double nx = cacheX[oz][y];
                    double nz = cacheX[ox][y];

                    if (nx * ny * nz > threshold) {
                        LB target = L.getBlock(world, x, y, z);

                        if (target.isNormalCube() && L.isReplaceableOreGen(world, x, y, z, VB.stone)) {
                            L.setBlock(world, x, y, z, block, meta, 2);
                        }
                    }
                }
            }
        }
    }
}
