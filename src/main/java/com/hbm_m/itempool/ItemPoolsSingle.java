package com.hbm_m.itempool;

import com.hbm_m.item.ModItems;
import com.hbm_m.item.weapon.grenade.ItemGrenadeExtra.EnumGrenadeExtra;
import com.hbm_m.item.weapon.grenade.ItemGrenadeFilling.EnumGrenadeFilling;
import com.hbm_m.item.weapon.grenade.ItemGrenadeFuze.EnumGrenadeFuze;
import com.hbm_m.item.weapon.grenade.ItemGrenadeShell.EnumGrenadeShell;
import com.hbm_m.item.weapon.grenade.ItemGrenadeUniversal;

/** 1:1 {@code com.hbm.itempool.ItemPoolsSingle}. Eintraege als Registernamen (Port-Umbenennungen beruecksichtigt). */
public final class ItemPoolsSingle {

    private ItemPoolsSingle() { }

    public static final String POOL_VAULT_RUSTY = "POOL_VAULT_RUSTY";
    public static final String POOL_VAULT_STANDARD = "POOL_VAULT_STANDARD";
    public static final String POOL_VAULT_REINFORCED = "POOL_VAULT_REINFORCED";
    public static final String POOL_VAULT_UNBREAKABLE = "POOL_VAULT_UNBREAKABLE";
    public static final String POOL_METEORITE_TREASURE = "POOL_METEORITE_TREASURE";
    public static final String POOL_BLUEPRINTS = "POOL_BLUEPRINTS";

    public static void init() {

        new ItemPool(POOL_VAULT_RUSTY)
                .add("minecraft:gold_ingot", 3, 14, 1)
                .add("gun_heavy_revolver", 1, 1, 2)
                .add("pin", 8, 8, 1)
                .add("gun_am180", 1, 1, 1)
                .add("bottle_quantum", 1, 3, 1)
                .add("cobalt_ingot", 4, 12, 1)
                .add("ammo_standard_bmg50_fmj", 24, 48, 1)
                .add("ammo_standard_p9_jhp", 48, 64, 2)
                .add("microchip", 3, 6, 1)
                .add("gas_mask_m65", 0, 1, 1, 1)
                .add(() -> ItemGrenadeUniversal.make(EnumGrenadeShell.FRAG, EnumGrenadeFilling.HE, EnumGrenadeFuze.S3), 1, 1, 1)
                .add(() -> ItemGrenadeUniversal.make(EnumGrenadeShell.FRAG, EnumGrenadeFilling.INC, EnumGrenadeFuze.S3), 1, 1, 1)
                .add("minecraft:diamond", 1, 2, 1)
                .build();

        new ItemPool(POOL_VAULT_STANDARD)
                .add("desh_ingot", 2, 6, 1)
                .add("powder_desh_mix", 1, 5, 1)
                .add("minecraft:diamond", 3, 6, 1)
                .add("ammo_standard_nuke_standard", 1, 1, 1)
                .add("ammo_container", 1, 1, 1)
                .add(() -> ItemGrenadeUniversal.make(EnumGrenadeShell.NUKE, EnumGrenadeFilling.NUCLEAR, EnumGrenadeFuze.S7), 1, 1, 1)
                .add(() -> ItemGrenadeUniversal.make(EnumGrenadeShell.TECH, EnumGrenadeFilling.EMP, EnumGrenadeFuze.S3), 1, 6, 1)
                .add("yellowcake_powder", 16, 24, 1)
                .add("gun_uzi", 1, 1, 1)
                .add("vacuum_tube", 12, 16, 1)
                .add("microchip", 2, 6, 1)
                .build();

        new ItemPool(POOL_VAULT_REINFORCED)
                .add("desh_ingot", 6, 16, 1)
                .add("powder_power", 1, 5, 1)
                .add("sat_chip", 1, 1, 1)
                .add("minecraft:diamond", 5, 9, 1)
                .add("ammo_standard_nuke_standard", 1, 3, 1)
                .add("ammo_container", 1, 4, 1)
                .add(() -> ItemGrenadeUniversal.make(EnumGrenadeShell.NUKE, EnumGrenadeFilling.NUCLEAR, EnumGrenadeFuze.S7), 1, 2, 1)
                .add(() -> ItemGrenadeUniversal.make(EnumGrenadeShell.STICK, EnumGrenadeFilling.HE, EnumGrenadeFuze.IMPACT, EnumGrenadeExtra.TRIPLEX), 1, 1, 1)
                .add("yellowcake_powder", 26, 42, 1)
                .add("gun_heavy_revolver", 1, 1, 1)
                .add("microchip", 18, 32, 1)
                .add("integrated_circuit", 6, 12, 1)
                .build();

        new ItemPool(POOL_VAULT_UNBREAKABLE)
                .add("ammo_container", 3, 6, 1)
                .add("ammo_standard_nuke_demo", 2, 3, 1)
                .add("gun_carbine", 1, 1, 1)
                .add("ammo_standard_r762_du", 16, 32, 1)
                .add("gun_congolake", 1, 1, 1)
                .add("advanced_circuit", 6, 12, 1)
                .build();

        new ItemPool(POOL_METEORITE_TREASURE)
                .add("cobalt_pickaxe", 1, 1, 10)
                .add("zirconium_ingot", 1, 16, 10)
                .add("niobium_ingot", 1, 16, 10)
                .add("cobalt_ingot", 1, 16, 10)
                .add("boron_ingot", 1, 16, 10)
                .add("starmetal_ingot", 1, 1, 5)
                .add("crystal_gold", 1, 4, 10)
                .add("vacuum_tube", 4, 8, 10)
                .add("microchip", 2, 4, 10)
                .add("definitelyfood", 16, 32, 25)
                .add("crate_can", 1, 3, 10)
                .add("pill_herbal", 1, 2, 10)
                .add("serum", 1, 1, 5)
                .add("heart_piece", 1, 1, 5)
                .add("scrumpy", 1, 1, 5)
                .add("launch_code_piece", 1, 1, 5)
                .add("egg_glyphid", 1, 1, 5)
                .add("gem_alexandrite", 1, 1, 1)
                .add("blueprint_folder_discover", 1, 1, 1)
                .build();

        new ItemPool(POOL_BLUEPRINTS)
                .add("blueprint_folder", 1, 1, 10)
                .add("blueprint_folder_discover", 1, 1, 5)
                .add("blueprint_folder", 1, 1, 1)
                .build();

        new ItemPool(POOL_BLUEPRINTS)
                .add("blueprint_folder", 1, 1, 10)
                .add("blueprint_folder_discover", 1, 1, 5)
                .add("blueprint_folder", 1, 1, 1)
                .build();
    }
}
