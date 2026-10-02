package com.hbm_m.powerarmor;

import com.hbm_m.item.tools_and_armor.ModArmorMaterials;

import net.minecraft.world.entity.player.Player;

/**
 * Elektrische FSB-Ruestung mit eigenem OBJ-Modell (Render-Layer im Paket {@code powerarmor.layer}).
 * Entspricht den {@code ArmorFSBPowered}-Unterklassen des Originals mit {@code getArmorModel}.
 */
public class ModPowerArmorItem extends ModArmorFSBPowered {

    public ModPowerArmorItem(ModArmorMaterials material, Type type, Properties properties, String texture,
                             long maxPower, long chargeRate, long consumption, long drain) {
        super(material, type, properties, texture, maxPower, chargeRate, consumption, drain);
    }

    @Override
    public boolean isObjArmor() {
        return true;
    }

    public static boolean hasFSBArmor(Player player) {
        return ModArmorFSB.hasFSBArmor(player);
    }

    public static boolean hasFSBArmorIgnoreCharge(Player player) {
        return ModArmorFSB.hasFSBArmorIgnoreCharge(player);
    }
}
