package com.hbm_m.powerarmor;

import com.hbm_m.item.tools_and_armor.ModArmorMaterials;

/**
 * 1:1 {@code com.hbm.items.armor.ArmorHEV}: der HEV-Anzug. Sein HUD (Gesundheit, Ruestungsladung,
 * Strahlungsbalken, RAD/s) ersetzt die Vanilla-Leisten, siehe {@code overlay.HEVHudOverlay}.
 */
public class ArmorHEV extends ModPowerArmorItem {

    public ArmorHEV(ModArmorMaterials material, Type type, Properties properties, String texture,
                    long maxPower, long chargeRate, long consumption, long drain) {
        super(material, type, properties, texture, maxPower, chargeRate, consumption, drain);
    }
}
