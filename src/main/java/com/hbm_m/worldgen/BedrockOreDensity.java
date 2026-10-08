package com.hbm_m.worldgen;

import com.hbm_m.inventory.fluid.ModFluids;

import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

/**
 * Portiert {@code ItemBedrockOreBase.getOreLevel}/{@code BedrockOre.getTier}/{@code getBoreFluid}
 * aus dem 1.7.10-Original: die Erzqualitaet an einer Weltposition ist deterministisch (nicht
 * zufaellig), damit der Ore Density Scanner sie vorher anzeigen kann. 1:1 mit den Original-
 * {@code NoiseGeneratorPerlin}s (4 Oktaven, Seeds 2114043 / 2082127 + Typ, Skalierung 0.01) und der
 * Formel {@code |level * type| * 0.05}, geclampt auf [0, 2].
 */
public final class BedrockOreDensity {

    private BedrockOreDensity() {}

    public enum Type {
        LIGHT, HEAVY, RARE, ACTINIDE, NONMETAL, CRYSTAL
    }

    private static final double SCALE = 0.01;
    private static final long LEVEL_SEED = 2114043L;
    private static final long TYPE_SEED_BASE = 2082127L;

    private static final com.hbm_m.world.gen.legacy.NoiseGeneratorPerlin[] ores = new com.hbm_m.world.gen.legacy.NoiseGeneratorPerlin[Type.values().length];
    private static com.hbm_m.world.gen.legacy.NoiseGeneratorPerlin level;

    /** Original {@code ItemBedrockOreBase.getOreLevel}. */
    public static double getDensity(int x, int z, Type type) {

        if (level == null) level = new com.hbm_m.world.gen.legacy.NoiseGeneratorPerlin(LEVEL_SEED, 4);
        if (ores[type.ordinal()] == null) ores[type.ordinal()] = new com.hbm_m.world.gen.legacy.NoiseGeneratorPerlin(TYPE_SEED_BASE + type.ordinal(), 4);

        double scale = SCALE;

        return net.minecraft.util.Mth.clamp(Math.abs(level.getValue(x * scale, z * scale) * ores[type.ordinal()].getValue(x * scale, z * scale)) * 0.05, 0, 2);
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
