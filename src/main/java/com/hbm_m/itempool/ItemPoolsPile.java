package com.hbm_m.itempool;

import com.hbm_m.item.ModItems;
import com.hbm_m.item.weapon.grenade.ItemGrenadeExtra.EnumGrenadeExtra;
import com.hbm_m.item.weapon.grenade.ItemGrenadeFilling.EnumGrenadeFilling;
import com.hbm_m.item.weapon.grenade.ItemGrenadeFuze.EnumGrenadeFuze;
import com.hbm_m.item.weapon.grenade.ItemGrenadeShell.EnumGrenadeShell;
import com.hbm_m.item.weapon.grenade.ItemGrenadeUniversal;

/** 1:1 {@code com.hbm.itempool.ItemPoolsPile}. Eintraege als Registernamen (Port-Umbenennungen beruecksichtigt). */
public final class ItemPoolsPile {

    private ItemPoolsPile() { }

    public static final String POOL_PILE_HIVE = "POOL_PILE_HIVE";
    public static final String POOL_PILE_BONES = "POOL_PILE_BONES";
    public static final String POOL_PILE_CAPS = "POOL_PILE_CAPS";
    public static final String POOL_PILE_MED_SYRINGE = "POOL_PILE_MED_SYRINGE";
    public static final String POOL_PILE_MED_PILLS = "POOL_PILE_MED_PILLS";
    public static final String POOL_PILE_MAKESHIFT_GUN = "POOL_PILE_MAKESHIFT_GUN";
    public static final String POOL_PILE_MAKESHIFT_WRENCH = "POOL_PILE_MAKESHIFT_WRENCH";
    public static final String POOL_PILE_MAKESHIFT_PLATES = "POOL_PILE_MAKESHIFT_PLATES";
    public static final String POOL_PILE_MAKESHIFT_WIRE = "POOL_PILE_MAKESHIFT_WIRE";
    public static final String POOL_PILE_NUKE_STORAGE = "POOL_PILE_NUKE_STORAGE";
    public static final String POOL_PILE_OF_GARBAGE = "POOL_PILE_OF_GARBAGE";
    public static final String POOL_PILE_MECHANICAL = "POOL_PILE_MECHANICAL";
    public static final String POOL_PILE_GEAR = "POOL_PILE_GEAR";
    public static final String POOL_PILE_SUPPLIES = "POOL_PILE_SUPPLIES";

    public static void init() {

        new ItemPool(POOL_PILE_HIVE)
                //Materials
                .add("minecraft:iron_ingot", 1, 3, 10)
                .add("steel_ingot", 1, 2, 10)
                .add("ingot_aluminium", 1, 2, 10)
                .add("scrap", 3, 6, 10)
                //Armor
                .add("gas_mask_m65", 0, 1, 1, 10)
                .add("steel_plate", 1, 1, 5)
                .add("steel_legs", 1, 1, 5)
                //Gear
                .add("steel_pickaxe", 1, 1, 5)
                .add("steel_shovel", 1, 1, 5)
                //Weapons
                .add("gun_maresleg", 1, 1, 5)
                .add("gun_light_revolver", 1, 1, 1)
                .add(() -> ItemGrenadeUniversal.make(EnumGrenadeShell.FRAG, EnumGrenadeFilling.HE, EnumGrenadeFuze.S3, EnumGrenadeExtra.FRAG_SLEEVE), 1, 2, 5)
                .add(() -> ItemGrenadeUniversal.make(EnumGrenadeShell.STICK, EnumGrenadeFilling.DEMO, EnumGrenadeFuze.IMPACT), 1, 2, 3)
                .add("ammo_standard_g12", 4, 4, 10)
                .add("ammo_standard_m357_sp", 6, 12, 10)
                .add("ammo_standard_g40_he", 1, 1, 2)
                //Consumables
                .add("bottle_nuka", 1, 2, 20)
                .add("bottle_quantum", 1, 2, 1)
                .add("definitelyfood", 5, 12, 20)
                .add("egg_glyphid", 1, 3, 30)
                .add("syringe_metal_stimpak", 1, 1, 5)
                .add("iv_blood", 1, 1, 10)
                .add("minecraft:experience_bottle", 1, 3, 5)
                .build();

        new ItemPool(POOL_PILE_BONES)
                .add("minecraft:bone", 1, 1, 10)
                .add("minecraft:rotten_flesh", 1, 1, 5)
                .add("biomass", 1, 1, 2)
                .build();

        new ItemPool(POOL_PILE_CAPS)
                .add("cap_nuka", 4, 4, 20)
                .add("cap_quantum", 4, 4, 3)
                .add("cap_sparkle", 4, 4, 1)
                .build();

        new ItemPool(POOL_PILE_MED_SYRINGE)
                .add("syringe_metal_stimpak", 1, 1, 10)
                .add("syringe_metal_medx", 1, 1, 5)
                .add("syringe_metal_psycho", 1, 1, 5)
                .build();

        new ItemPool(POOL_PILE_MED_PILLS)
                .add("radaway", 1, 1, 10)
                .add("radx", 1, 1, 10)
                .add("iv_blood", 1, 1, 15)
                .add("siox", 1, 1, 5)
                .build();

        new ItemPool(POOL_PILE_MAKESHIFT_GUN)
                .add("gun_maresleg", 1, 1, 10)
                .build();

        new ItemPool(POOL_PILE_MAKESHIFT_WRENCH)
                .add("wrench", 1, 1, 10)
                .build();

        new ItemPool(POOL_PILE_MAKESHIFT_PLATES)
                .add("plate_steel", 1, 1, 10)
                .build();

        new ItemPool(POOL_PILE_MAKESHIFT_WIRE)
                .add("wire_aluminium", 1, 1, 10)
                .build();

        new ItemPool(POOL_PILE_NUKE_STORAGE)
                .add("ammo_standard_nuke_standard", 1, 1, 50)
                .add("ammo_standard_nuke_high", 1, 1, 10)
                .add("ammo_standard_nuke_tots", 1, 1, 10)
                .build();

        new ItemPool(POOL_PILE_OF_GARBAGE)
                .add("pipe_iron", 0, 2, 20)
                .add("scrap", 1, 5, 20)
                .add("dust", 1, 3, 40)
                .add("dust_tiny", 1, 7, 40)
                .add("cement_powder", 1, 6, 40)
                .add("nugget_lead", 0, 3, 20)
                .add("wire_lead", 1, 2, 20)
                .add("ash_wood", 0, 1, 15)
                .add("plate_lead", 0, 1, 15)
                .add("minecraft:string", 0, 1, 15)
                .add("bolt_lead", 0, 2, 15)
                .add("pin", 0, 2, 15)
                .add("cap_nuka", 0, 8, 15)
                .add("plate_iron", 0, 2, 15)
                .add("fallout", 0, 2, 15)
                .add("coil_tungsten", 0, 2, 15)
                .add("can_empty", 0, 1, 15)
                .add("asbestos_ingot", 0, 1, 15)
                .add("syringe_metal_empty", 0, 1, 15)
                .add("syringe_empty", 0, 1, 15)
                .add("pipe_lead", 0, 1, 5)
                .add("motor", 0, 1, 5)
                .add("canned_mystery", 0, 1, 5)
                .build();

        new ItemPool(POOL_PILE_MECHANICAL)
                .add("defuser", 1, 1, 30)
                .add("screwdriver", 1, 1, 30)
                .add("wire_copper", 8, 12, 120)
                .add("plate_steel", 3, 8, 40)
                .add("plate_copper", 2, 5, 40)
                .add("coil_copper", 2, 5, 40)
                .add("coil_tungsten", 2, 5, 40)
                .build();

        new ItemPool(POOL_PILE_GEAR)
                .add("defuser", 1, 1, 40)
                .add("screwdriver", 1, 1, 30)
                .add("canteen_vodka", 1, 1, 40)
                .add("casing_small_steel", 1, 4, 30)
                .add("casing_small", 3, 8, 40)
                .add("casing_buckshot", 3, 8, 40)
                .add("canned_beef", 2, 5, 40)
                .add("taurun_helmet", 1, 1, 20)
                .add("taurun_plate", 1, 1, 20)
                .add("taurun_legs", 1, 1, 20)
                .add("taurun_boots", 1, 1, 20)
                .build();

        new ItemPool(POOL_PILE_SUPPLIES)
                .add(() -> ItemGrenadeUniversal.make(EnumGrenadeShell.FRAG, EnumGrenadeFilling.HE, EnumGrenadeFuze.S3, EnumGrenadeExtra.FRAG_SLEEVE), 3, 5, 10)
                .add(() -> ItemGrenadeUniversal.make(EnumGrenadeShell.FRAG, EnumGrenadeFilling.HE, EnumGrenadeFuze.S3, EnumGrenadeExtra.FRAG_SLEEVE), 3, 5, 10)
                .add(() -> ItemGrenadeUniversal.make(EnumGrenadeShell.FRAG, EnumGrenadeFilling.HE, EnumGrenadeFuze.S3, EnumGrenadeExtra.FRAG_SLEEVE), 3, 5, 10)
                .add("syringe_metal_stimpak", 3, 5, 30)
                .add("syringe_metal_psycho", 3, 5, 30)
                .add("syringe_antidote", 1, 2, 30)
                .add("ammo_container", 2, 3, 40)
                .build();
    }
}
