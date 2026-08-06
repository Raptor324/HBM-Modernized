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

    //  Примечание: метки времени flash/shake раньше дублировались здесь и в
    //  ModEventHandlerClient (Forge-only). NukeTorex писал в этот «мёртвый»
    //  дубликат, поэтому эффекты не срабатывали. Теперь они обращаются напрямую
    //  к ModEventHandlerClient, а сама тряска камеры — к CameraShakeHandler.
}

