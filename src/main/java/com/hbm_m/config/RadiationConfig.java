package com.hbm_m.config;

/**
 * Флаги отключения типов опасностей. Порт {@link com.hbm.config.RadiationConfig} (1.7.10).
 */
public final class RadiationConfig {

    public static boolean disableAsbestos = false;
    public static boolean disableCoal = false;
    public static boolean disableHot = false;
    public static boolean disableExplosive = false;
    public static boolean disableHydro = false;
    public static boolean disableBlinding = false;

    private RadiationConfig() {
    }
}
