package com.hbm_m.powerarmor;

import com.hbm_m.item.tools_and_armor.ModArmorMaterials;

/** 1:1 {@code com.hbm.items.armor.ArmorBismuth}: FSB-Ruestung (ohne Akku) mit OBJ-Modell. */
public class BismuthArmor extends ModArmorFSB {

    public BismuthArmor(ModArmorMaterials material, Type type, Properties properties, String texture) {
        super(material, type, properties, texture);
    }

    @Override
    public boolean isObjArmor() {
        return true;
    }
}
