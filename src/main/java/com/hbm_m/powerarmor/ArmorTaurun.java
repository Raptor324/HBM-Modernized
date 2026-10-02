package com.hbm_m.powerarmor;

import com.hbm_m.item.tools_and_armor.ModArmorMaterials;

import net.minecraft.world.item.ItemStack;

/** 1:1 {@code com.hbm.items.armor.ArmorTaurun}: FSB-Ruestung mit OBJ-Modell, unzerstoerbar (setMaxDamage(0)). */
public class ArmorTaurun extends ModArmorFSB {

    public ArmorTaurun(ModArmorMaterials material, Type type, Properties properties, String texture) {
        super(material, type, properties, texture);
    }

    @Override
    public boolean isObjArmor() {
        return true;
    }

    //? if !fabric {
    @Override
    public boolean isDamageable(ItemStack stack) {
        return false;
    }

    @Override
    public int getMaxDamage(ItemStack stack) {
        return 0;
    }
    //?}
}
