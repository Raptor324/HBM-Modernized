package com.hbm_m.util.explosions.nuclear;

/**
 * Дополнительные переключатели финальной стадии {@link CraterGenerator}.
 */
public final class CraterGenerationFlags {

    public static final CraterGenerationFlags DEFAULT = new CraterGenerationFlags(true, true);

    private final boolean applyBiomes;
    private final boolean applyEntityDamage;

    public CraterGenerationFlags(boolean applyBiomes, boolean applyEntityDamage) {
        this.applyBiomes = applyBiomes;
        this.applyEntityDamage = applyEntityDamage;
    }

    public boolean applyBiomes() {
        return applyBiomes;
    }

    public boolean applyEntityDamage() {
        return applyEntityDamage;
    }
}
