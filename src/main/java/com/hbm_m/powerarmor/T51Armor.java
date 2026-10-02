package com.hbm_m.powerarmor;

import com.hbm_m.item.tools_and_armor.ModArmorMaterials;

/** 1:1 {@code com.hbm.items.armor.ArmorT51}: T-51b Powered Armor. */
public class T51Armor extends ModPowerArmorItem {

    public T51Armor(ModArmorMaterials material, Type type, Properties properties, String texture,
              long maxPower, long chargeRate, long consumption, long drain) {
        super(material, type, properties, texture, maxPower, chargeRate, consumption, drain);
    }
}
