package com.hbm_m.creativetabs;

import java.util.function.Consumer;

import com.hbm_m.item.weapon.sedna.ItemGunBaseNT;
import com.hbm_m.item.weapon.sedna.WeaponItems;
import com.hbm_m.item.weapon.sedna.factory.GunFactory.EnumAmmo;

import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Port von {@code com.hbm.creativetabs.WeaponTab}: Symbol und der SEDNA-Block des Waffen-Reiters.
 * Reihenfolge = Original-Registrierungsreihenfolge in {@code ModItems.registerItem()} (1.7.10 sortiert nach Item-ID).
 */
public final class WeaponTab {

    private WeaponTab() { }

    /** Original {@code getTabIconItem}: gun_greasegun, sonst Eisenspitzhacke. */
    public static Item getTabIconItem() {
        RegistrySupplier<Item> grease = WeaponItems.GUNS.get("gun_greasegun");
        if (grease != null && grease.isPresent()) return grease.get();
        return Items.IRON_PICKAXE;
    }

    /** Registrierungsreihenfolge der Waffen in {@code ModItems.registerItem()} (nicht die Init-Reihenfolge der Fabriken). */
    private static final String[] GUN_ORDER = {
            "gun_debug",
            "gun_pepperbox", "gun_light_revolver", "gun_light_revolver_atlas", "gun_light_revolver_dani",
            "gun_henry", "gun_henry_lincoln", "gun_greasegun", "gun_maresleg", "gun_maresleg_akimbo", "gun_maresleg_broken",
            "gun_flaregun", "gun_heavy_revolver", "gun_heavy_revolver_lilmac", "gun_heavy_revolver_protege",
            "gun_carbine", "gun_am180", "gun_liberator", "gun_congolake",
            "gun_flamer", "gun_flamer_topaz", "gun_flamer_daybreaker",
            "gun_uzi", "gun_uzi_akimbo", "gun_spas12", "gun_panzerschreck", "gun_star_f", "gun_star_f_akimbo",
            "gun_g3", "gun_g3_zebra", "gun_mk108", "gun_chemthrower",
            "gun_amat", "gun_amat_subtlety", "gun_amat_penance", "gun_m2",
            "gun_autoshotgun", "gun_autoshotgun_shredder", "gun_autoshotgun_sexy", "gun_autoshotgun_heretic",
            "gun_quadro", "gun_lag", "gun_minigun", "gun_minigun_dual", "gun_minigun_lacunae", "gun_missile_launcher",
            "gun_tesla_cannon", "gun_laser_pistol", "gun_laser_pistol_pew_pew", "gun_laser_pistol_morning_glory",
            "gun_stg77", "gun_tau", "gun_fatman", "gun_lasrifle", "gun_stinger", "gun_coilgun", "gun_hangman", "gun_mas36",
            "gun_bolter", "gun_folly", "gun_aberrator", "gun_aberrator_eott", "gun_double_barrel",
            "gun_double_barrel_sacred_dragon", "gun_n_i_4_n_i",
            "gun_fireext", "gun_charge_thrower", "gun_drill", "gun_pa_melee", "gun_pa_ranged"
    };

    /**
     * SEDNA-Eintraege des Waffen-Reiters: Waffen der Qualitaet A_SIDE/SPECIAL/UTILITY (Original-Konstruktor setzt nur
     * dann {@code weaponTab}), dann ammo_standard in {@code EnumAmmo.order}, dann weapon_mod_generic/special/caliber.
     * ammo_debug, ammo_secret und weapon_mod_test haben im Original keinen Reiter.
     */
    public static void appendSednaItems(Consumer<ItemStack> output) {
        for (String name : GUN_ORDER) {
            RegistrySupplier<Item> sup = WeaponItems.GUNS.get(name);
            if (sup == null || !sup.isPresent()) continue;
            if (sup.get() instanceof ItemGunBaseNT gun && gun.isInCreativeTab()) output.accept(new ItemStack(gun));
        }
        for (EnumAmmo a : EnumAmmo.order) output.accept(new ItemStack(WeaponItems.ammo(a)));
        for (RegistrySupplier<Item> sup : WeaponItems.WEAPON_MOD_GENERIC.values()) output.accept(new ItemStack(sup.get()));
        for (RegistrySupplier<Item> sup : WeaponItems.WEAPON_MOD_SPECIAL.values()) output.accept(new ItemStack(sup.get()));
        for (RegistrySupplier<Item> sup : WeaponItems.WEAPON_MOD_CALIBER.values()) output.accept(new ItemStack(sup.get()));
    }
}
