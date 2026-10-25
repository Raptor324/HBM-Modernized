package com.hbm_m.world.gen.legacy;

import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.synth.SimplexNoise;

/**
 * 1:1 {@code net.minecraft.world.gen.NoiseGeneratorPerlin} (1.7.10): Summe mehrerer 2D-Simplex-Oktaven
 * ({@code NoiseGeneratorSimplex} = 1.20 {@link SimplexNoise}, gleicher Algorithmus). Port: statt
 * {@code new NoiseGeneratorPerlin(new Random(seed), n)} wird der Seed uebergeben - {@link LegacyRandomSource} ist
 * derselbe Java-LCG, die Oktaven sind damit identisch.
 */
public class NoiseGeneratorPerlin {

    private final SimplexNoise[] octaves;
    private final int count;

    public NoiseGeneratorPerlin(long seed, int octaves) {
        this.count = octaves;
        this.octaves = new SimplexNoise[octaves];
        LegacyRandomSource src = new LegacyRandomSource(seed);
        for (int j = 0; j < octaves; ++j) {
            this.octaves[j] = new SimplexNoise(src);
        }
    }

    /** {@code func_151601_a}. */
    public double getValue(double x, double z) {
        double d2 = 0.0D;
        double d3 = 1.0D;

        for (int i = 0; i < this.count; ++i) {
            d2 += this.octaves[i].getValue(x * d3, z * d3) / d3;
            d3 /= 2.0D;
        }

        return d2;
    }
}
