package com.hbm_m.powerarmor;

/**
 * Общий (multiloader) клиентский стейт для режимов силовой брони.
 *
 * Важно: здесь нет никаких client-only импортов (Minecraft, RenderSystem и т.п.),
 * поэтому класс безопасно существует во всех таргетах. Реальные рендер/ивенты
 * могут быть Forge/Fabric-специфичными, но они читают состояние отсюда.
 */
public final class PowerArmorClientState {
    private PowerArmorClientState() {}

    // VATS
    private static boolean vatsActive = false;

    public static boolean isVATSActive() {
        return vatsActive;
    }

    public static void activateVATS() {
        vatsActive = true;
    }

    public static void deactivateVATS() {
        vatsActive = false;
    }

    // Thermal vision
    private static boolean thermalActive = false;

    public static boolean isThermalActive() {
        return thermalActive;
    }

    public static void activateThermal() {
        thermalActive = true;
    }

    public static void deactivateThermal() {
        thermalActive = false;
    }

    // Nuclear flash / screen shake timestamps (used by particles / overlays)
    public static final int FLASH_DURATION = 5_000;
    public static long flashTimestamp;
    public static final int SHAKE_DURATION = 1_500;
    public static long shakeTimestamp;

    public static void triggerNuclearFlash() {
        flashTimestamp = System.currentTimeMillis();
    }
}

