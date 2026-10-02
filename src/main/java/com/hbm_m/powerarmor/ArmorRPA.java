package com.hbm_m.powerarmor;

import com.hbm_m.item.tools_and_armor.ModArmorMaterials;

/** 1:1 {@code com.hbm.items.armor.ArmorRPA}: Remnant Power Armor (Nahkampfkomponente folgt mit dem Sedna-Waffensystem). */
public class ArmorRPA extends ModPowerArmorItem {

    public ArmorRPA(ModArmorMaterials material, Type type, Properties properties, String texture,
              long maxPower, long chargeRate, long consumption, long drain) {
        super(material, type, properties, texture, maxPower, chargeRate, consumption, drain);
    }
}
