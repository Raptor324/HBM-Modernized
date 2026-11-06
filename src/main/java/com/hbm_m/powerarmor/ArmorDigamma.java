package com.hbm_m.powerarmor;

import com.hbm_m.item.tools_and_armor.ModArmorMaterials;

/** 1:1 {@code com.hbm.items.armor.ArmorDigamma}: Faust-Ruestung (fau_*). */
public class ArmorDigamma extends ModPowerArmorItem {

    public ArmorDigamma(ModArmorMaterials material, Type type, Properties properties, String texture,
              long maxPower, long chargeRate, long consumption, long drain) {
        // Merged ModPowerArmorItem takes PowerArmorSpecs; texture is resolved from the registry id there.
        super(material, type, properties, new PowerArmorSpecs(maxPower, chargeRate, drain, consumption));
    }
}
