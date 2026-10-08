package com.hbm_m.powerarmor;

import com.hbm_m.item.tools_and_armor.ModArmorMaterials;
import com.hbm_m.item.weapon.sedna.impl.IPAMelee;
import com.hbm_m.item.weapon.sedna.impl.IPARanged;
import com.hbm_m.item.weapon.sedna.impl.IPAWeaponsProvider;

import net.minecraft.world.entity.player.Player;

/** 1:1 {@code com.hbm.items.armor.ArmorRPA}: Remnant Power Armor mit Nahkampfkomponente fuer gun_pa_melee. */
public class ArmorRPA extends ModPowerArmorItem implements IPAWeaponsProvider {

    public ArmorRPA(ModArmorMaterials material, Type type, Properties properties, String texture,
              long maxPower, long chargeRate, long consumption, long drain) {
        super(material, type, properties, texture, maxPower, chargeRate, consumption, drain);
    }

    public static final ArmorRPAMelee meleeComponent = new ArmorRPAMelee();

    @Override
    public IPAMelee getMeleeComponent(Player entity) {
        if (hasFSBArmorIgnoreCharge(entity)) return meleeComponent;
        return null;
    }

    @Override public IPARanged getRangedComponent(Player entity) { return null; }
}
