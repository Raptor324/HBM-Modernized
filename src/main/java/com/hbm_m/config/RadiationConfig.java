package com.hbm_m.config;

/**
 * Флаги отключения типов опасностей. Порт {@link com.hbm.config.RadiationConfig} (1.7.10).
 *
 * <p>Restport: ueber {@code ConfigSchema} (statische Bindung) einstellbar, Standardwerte des Originals.
 * Die uebrigen Original-Optionen (enableChunkRads, worldRadEffects, cleanupDeadDirt, Pollution) liegen in
 * {@link ModClothConfig}; hellRad heisst dort {@code netherAmbientRad}.
 */
public final class RadiationConfig {

    /** FOG_00_threshold: ab so vielen RAD im Chunk entsteht Strahlungsnebel. */
    public static int fogRad = 100;
    /** FOG_01_threshold: 1:n Chance je Sekunde (Original setDef 20 bei <= 0). */
    public static int fogCh = 20;
    /** RADWORLD_01_amount (im Original nirgends gelesen). */
    public static int worldRad = 10;
    /** RADWORLD_02_minimum (im Original nirgends gelesen). */
    public static int worldRadThreshold = 20;
    /** RADIATION_00_enableContamination: Kontamination des Spielers und Strahlenkrankheit. */
    public static boolean enableContamination = true;

    public static boolean disableAsbestos = false;
    public static boolean disableCoal = false;
    public static boolean disableHot = false;
    public static boolean disableExplosive = false;
    public static boolean disableHydro = false;
    public static boolean disableBlinding = false;
    /** HAZ_06_disableFibrosis (im Original nirgends gelesen). */
    public static boolean disableFibrosis = false;

    /** POL_03_enableSootFog: Smog sichtbar. Russwert per PollutionSyncPacket, Nebel in client/SootFogHandler. */
    public static boolean enableSootFog = true;
    /** POL_06_sootFogThreshold. */
    public static double sootFogThreshold = 35D;
    /** POL_07_sootFogDivisor. */
    public static double sootFogDivisor = 120D;
    /** POL_08_smokeStackSootMult (im Original nirgends gelesen, Rampant nutzt rampantSmokeStackOverride). */
    public static double smokeStackSootMult = 0.8D;

    private RadiationConfig() {
    }
}
