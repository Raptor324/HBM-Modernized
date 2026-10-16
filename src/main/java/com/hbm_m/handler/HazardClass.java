package com.hbm_m.handler;

/**
 * Классы опасностей, от которых защищают противогазы/фильтры.
 * Порт {@link com.hbm.util.ArmorRegistry.HazardClass} (1.7.10).
 */
public enum HazardClass {

    // Угольная пыль, крупная взвесь
    PARTICLE_COARSE("hazard.particleCoarse"),
    // Асбестовая пыль, мелкодисперсные частицы
    PARTICLE_FINE("hazard.particleFine"),
    GAS_MONOXIDE("hazard.gasMonoxide"),
    GAS_LUNG("hazard.gasLung"),
    GAS_BLISTERING("hazard.gasBlistering"),
    BACTERIA("hazard.bacteria"),
    SAND("hazard.sand"),
    LIGHT("hazard.light");

    public final String translationKey;

    HazardClass(String translationKey) {
        this.translationKey = translationKey;
    }
}
