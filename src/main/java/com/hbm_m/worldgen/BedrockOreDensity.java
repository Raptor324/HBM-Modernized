package com.hbm_m.worldgen;

import com.hbm_m.inventory.fluid.ModFluids;

import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

/**
 * Portiert {@code ItemBedrockOreBase.getOreLevel}/{@code BedrockOre.getTier}/{@code getBoreFluid}
 * aus dem 1.7.10-Original: die Erzqualitaet an einer Weltposition ist deterministisch (nicht
 * zufaellig), damit der Ore Density Scanner sie vorher anzeigen kann. Statt der vanilla
 * {@code NoiseGeneratorPerlin} (deren Forge-1.20-Aequivalent-API unsicher ist) nutzt dies einen
 * simplen, selbst-enthaltenen Value-Noise mit den gleichen Seeds/Skalierungsfaktor 0.01 und der
 * gleichen Formel {@code |level * type| * 0.05}, geclampt auf [0, 2].
 */
public final class BedrockOreDensity {

    private BedrockOreDensity() {}

    public enum Type {
        LIGHT, HEAVY, RARE, ACTINIDE, NONMETAL, CRYSTAL
    }

    private static final double SCALE = 0.01;
    private static final int OCTAVES = 4;
    private static final long LEVEL_SEED = 2114043L;
    private static final long TYPE_SEED_BASE = 2082127L;

    private static double hash(long seed, int x, int z) {
        long h = seed + (long) x * 374761393L + (long) z * 668265263L;
        h = (h ^ (h >>> 13)) * 1274126177L;
        h = h ^ (h >>> 16);
        return ((h & 0xFFFFFFFFL) / (double) 0xFFFFFFFFL) * 2.0 - 1.0;
    }

    private static double smooth(double t) {
        return t * t * (3 - 2 * t);
    }

    private static double valueNoise(long seed, double x, double z) {
        int x0 = (int) Math.floor(x);
        int z0 = (int) Math.floor(z);
        int x1 = x0 + 1;
        int z1 = z0 + 1;
        double sx = smooth(x - x0);
        double sz = smooth(z - z0);
        double n00 = hash(seed, x0, z0);
        double n10 = hash(seed, x1, z0);
        double n01 = hash(seed, x0, z1);
        double n11 = hash(seed, x1, z1);
        double ix0 = n00 + (n10 - n00) * sx;
        double ix1 = n01 + (n11 - n01) * sx;
        return ix0 + (ix1 - ix0) * sz;
    }

    /**
     * Octave sum matching the original's {@code NoiseGeneratorPerlin(seed, 4)}: each octave halves
     * the frequency and doubles the amplitude ({@code d2 += noise(x * d3, z * d3) / d3; d3 /= 2}).
     *
     * A single octave stays inside [-1, 1], so the original formula |level * type| * 0.05 could
     * never exceed 0.05 - every bedrock ore in the world came out tier 1 with no bore fluid, and
     * tiers 2-4 of the mining drill were unreachable content.
     */
    private static double fractalNoise(long seed, double x, double z) {
        double sum = 0;
        double freq = 1.0;
        for (int octave = 0; octave < OCTAVES; octave++) {
            sum += valueNoise(seed + octave * 6364136223L, x * freq, z * freq) / freq;
            freq /= 2.0;
        }
        return sum;
    }

    public static double getDensity(int x, int z, Type type) {
        double level = fractalNoise(LEVEL_SEED, x * SCALE, z * SCALE);
        double t = fractalNoise(TYPE_SEED_BASE + type.ordinal(), x * SCALE, z * SCALE);
        double raw = Math.abs(level * t) * 0.05;
        return Math.max(0, Math.min(2, raw));
    }

    public static double getTotalDensity(int x, int z) {
        double sum = 0;
        for (Type type : Type.values()) sum += getDensity(x, z, type);
        return sum / Type.values().length;
    }

    public static int getTier(double density) {
        if (density > 1.5) return 4;
        if (density > 1.0) return 3;
        if (density > 0.75) return 2;
        return 1;
    }

    /** {@link Fluids#EMPTY} == kein Fluid noetig (Tier 1). */
    public static Fluid getBoreFluid(double density) {
        if (density > 1.5) return ModFluids.SOLVENT.getSource();
        if (density > 1.0) return ModFluids.SULFURIC_ACID.getSource();
        if (density > 0.75) return ModFluids.WATER.getSource();
        return Fluids.EMPTY;
    }

    public static int getBoreFluidAmountMb(double density) {
        if (density > 1.5) return 2000;
        if (density > 0.75) return 1000;
        return 0;
    }
}
