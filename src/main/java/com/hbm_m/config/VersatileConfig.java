package com.hbm_m.config;

/**
 * 1:1 {@code com.hbm.config.VersatileConfig} (1.7.10): Werte, die von mehreren Schaltern (528, LBSM,
 * MachineConfig) zugleich abhaengen. {@code getTransmutatorItem} fehlt - der Port hat keinen Transmutator.
 */
public final class VersatileConfig {

    static final int minute = 60 * 20;
    static final int hour = 60 * minute;

    private VersatileConfig() { }

    /** Original {@code getSchrabOreChance}: mit LBSM {@code schrabRate}, sonst 1/250. */
    public static int getSchrabOreChance() {
        if (GeneralConfig.lbsm()) return Math.max(1, GeneralConfig.schrabRate);
        return 250;
    }

    /** Original {@code rtgDecay()}. */
    public static boolean rtgDecay() {
        return GeneralConfig.enable528 || MachineConfig.doRTGsDecay;
    }

    /** Original {@code scaleRTGPower()}. */
    public static boolean scaleRTGPower() {
        return GeneralConfig.enable528 || MachineConfig.scaleRTGPower;
    }

    public static int getLongDecayChance() {
        return GeneralConfig.enable528 ? 15 * hour : (GeneralConfig.lbsm() && GeneralConfig.enableLBSMShorterDecay) ? 15 * minute : 3 * hour;
    }

    public static int getShortDecayChance() {
        return GeneralConfig.enable528 ? 3 * hour : (GeneralConfig.lbsm() && GeneralConfig.enableLBSMShorterDecay) ? 3 * minute : 15 * minute;
    }
}
