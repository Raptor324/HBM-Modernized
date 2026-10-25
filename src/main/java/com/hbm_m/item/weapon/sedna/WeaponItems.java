package com.hbm_m.item.weapon.sedna;

import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

import com.hbm_m.item.ModItems;
import com.hbm_m.item.weapon.sedna.factory.GunFactory;
import com.hbm_m.item.weapon.sedna.factory.GunFactory.EnumAmmo;
import com.hbm_m.item.weapon.sedna.factory.GunFactory.EnumAmmoSecret;
import com.hbm_m.item.weapon.sedna.factory.GunFactory.EnumModCaliber;
import com.hbm_m.item.weapon.sedna.factory.GunFactory.EnumModGeneric;
import com.hbm_m.item.weapon.sedna.factory.GunFactory.EnumModSpecial;
import com.hbm_m.item.weapon.sedna.factory.GunFactory.EnumModTest;

import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.world.item.Item;

/**
 * Registrierung des SEDNA-Waffensystems. Jede Original-Meta ist ein eigener Gegenstand:
 * {@code ammo_standard_<typ>}, {@code ammo_secret_<typ>}, {@code weapon_mod_<art>_<typ>}. Munition wird VOR den Waffen
 * registriert; die Waffen selbst entstehen beim ersten Registry-Zugriff gesammelt in {@link GunFactory#init()} (feste
 * Reihenfolge, weil die IDs der Geschosskonfigurationen im Magazin-NBT stehen) und werden hier nur noch abgeholt.
 */
public final class WeaponItems {

    private WeaponItems() { }

    public static final Map<EnumAmmo, RegistrySupplier<Item>> AMMO_STANDARD = new EnumMap<>(EnumAmmo.class);
    public static final Map<EnumAmmoSecret, RegistrySupplier<Item>> AMMO_SECRET = new EnumMap<>(EnumAmmoSecret.class);
    public static final Map<EnumModTest, RegistrySupplier<Item>> WEAPON_MOD_TEST = new EnumMap<>(EnumModTest.class);
    public static final Map<EnumModGeneric, RegistrySupplier<Item>> WEAPON_MOD_GENERIC = new EnumMap<>(EnumModGeneric.class);
    public static final Map<EnumModSpecial, RegistrySupplier<Item>> WEAPON_MOD_SPECIAL = new EnumMap<>(EnumModSpecial.class);
    public static final Map<EnumModCaliber, RegistrySupplier<Item>> WEAPON_MOD_CALIBER = new EnumMap<>(EnumModCaliber.class);
    public static final Map<String, RegistrySupplier<Item>> GUNS = new LinkedHashMap<>();
    public static RegistrySupplier<Item> AMMO_DEBUG;

    /** Alle Waffen-IDs in Original-Registrierungsreihenfolge. */
    public static final String[] GUN_NAMES = {
            "gun_debug",
            "gun_pepperbox",
            "gun_light_revolver",
            "gun_light_revolver_atlas",
            "gun_light_revolver_dani",
            "gun_henry",
            "gun_henry_lincoln",
            "gun_heavy_revolver",
            "gun_heavy_revolver_lilmac",
            "gun_heavy_revolver_protege",
            "gun_hangman",
            "gun_greasegun",
            "gun_lag",
            "gun_uzi",
            "gun_uzi_akimbo",
            "gun_maresleg",
            "gun_maresleg_akimbo",
            "gun_maresleg_broken",
            "gun_liberator",
            "gun_spas12",
            "gun_autoshotgun",
            "gun_autoshotgun_shredder",
            "gun_autoshotgun_sexy",
            "gun_flaregun",
            "gun_congolake",
            "gun_mk108",
            "gun_carbine",
            "gun_minigun",
            "gun_minigun_lacunae",
            "gun_minigun_dual",
            "gun_mas36",
            "gun_am180",
            "gun_star_f",
            "gun_star_f_akimbo",
            "gun_flamer",
            "gun_flamer_topaz",
            "gun_flamer_daybreaker",
            "gun_chemthrower",
            "gun_panzerschreck",
            "gun_stinger",
            "gun_quadro",
            "gun_missile_launcher",
            "gun_g3",
            "gun_g3_zebra",
            "gun_stg77",
            "gun_amat",
            "gun_amat_subtlety",
            "gun_amat_penance",
            "gun_m2",
            "gun_tesla_cannon",
            "gun_laser_pistol",
            "gun_laser_pistol_pew_pew",
            "gun_laser_pistol_morning_glory",
            "gun_lasrifle",
            "gun_tau",
            "gun_coilgun",
            "gun_n_i_4_n_i",
            "gun_fatman",
            "gun_bolter",
            "gun_folly",
            "gun_double_barrel",
            "gun_double_barrel_sacred_dragon",
            "gun_autoshotgun_heretic",
            "gun_aberrator",
            "gun_aberrator_eott",
            "gun_fireext",
            "gun_charge_thrower",
            "gun_drill",
            "gun_pa_melee",
            "gun_pa_ranged"
    };

    private static String id(Enum<?> e) {
        return e.name().toLowerCase(Locale.US);
    }

    /** Von {@code ModItems} aufgerufen (nach den allgemeinen Gegenstaenden). */
    public static void registerAll() {
        AMMO_DEBUG = ModItems.ITEMS.register("ammo_debug", () -> new Item(new Item.Properties()));
        for (EnumAmmo a : EnumAmmo.values()) AMMO_STANDARD.put(a, ModItems.ITEMS.register("ammo_standard_" + id(a), () -> new Item(new Item.Properties())));
        for (EnumAmmoSecret a : EnumAmmoSecret.values()) AMMO_SECRET.put(a, ModItems.ITEMS.register("ammo_secret_" + id(a), () -> new Item(new Item.Properties())));
        for (EnumModTest a : EnumModTest.values()) WEAPON_MOD_TEST.put(a, ModItems.ITEMS.register("weapon_mod_test_" + id(a), () -> new Item(new Item.Properties().stacksTo(1))));
        for (EnumModGeneric a : EnumModGeneric.values()) WEAPON_MOD_GENERIC.put(a, ModItems.ITEMS.register("weapon_mod_generic_" + id(a), () -> new Item(new Item.Properties().stacksTo(1))));
        for (EnumModSpecial a : EnumModSpecial.values()) WEAPON_MOD_SPECIAL.put(a, ModItems.ITEMS.register("weapon_mod_special_" + id(a), () -> new Item(new Item.Properties().stacksTo(1))));
        for (EnumModCaliber a : EnumModCaliber.values()) WEAPON_MOD_CALIBER.put(a, ModItems.ITEMS.register("weapon_mod_caliber_" + id(a), () -> new Item(new Item.Properties().stacksTo(1))));
        for (String name : GUN_NAMES) GUNS.put(name, ModItems.ITEMS.register(name, () -> GunFactory.take(name)));
    }

    public static Item ammo(EnumAmmo a) { return AMMO_STANDARD.get(a).get(); }
    public static Item ammo(EnumAmmoSecret a) { return AMMO_SECRET.get(a).get(); }

    /** Original {@code ModItems.gun_x} - nach der Registrierung. */
    public static Item gun(String name) {
        RegistrySupplier<Item> s = GUNS.get(name);
        if (s == null) throw new IllegalArgumentException("Unknown gun " + name);
        return s.get();
    }
}
