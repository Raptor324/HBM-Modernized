package com.hbm_m.itempool;

import com.hbm_m.item.ModItems;
import com.hbm_m.item.weapon.grenade.ItemGrenadeExtra.EnumGrenadeExtra;
import com.hbm_m.item.weapon.grenade.ItemGrenadeFilling.EnumGrenadeFilling;
import com.hbm_m.item.weapon.grenade.ItemGrenadeFuze.EnumGrenadeFuze;
import com.hbm_m.item.weapon.grenade.ItemGrenadeShell.EnumGrenadeShell;
import com.hbm_m.item.weapon.grenade.ItemGrenadeUniversal;

/** 1:1 {@code com.hbm.itempool.ItemPoolsComponent}. Eintraege als Registernamen (Port-Umbenennungen beruecksichtigt). */
public final class ItemPoolsComponent {

    private ItemPoolsComponent() { }

    public static final String POOL_MACHINE_PARTS = "POOL_MACHINE_PARTS";
    public static final String POOL_NUKE_FUEL = "POOL_NUKE_FUEL";
    public static final String POOL_SILO = "POOL_SILO";
    public static final String POOL_OFFICE_TRASH = "POOL_OFFICE_TRASH";
    public static final String POOL_FILING_CABINET = "POOL_FILING_CABINET";
    public static final String POOL_SOLID_FUEL = "POOL_SOLID_FUEL";
    public static final String POOL_VAULT_LAB = "POOL_VAULT_LAB";
    public static final String POOL_VAULT_LOCKERS = "POOL_VAULT_LOCKERS";
    public static final String POOL_METEOR_SAFE = "POOL_METEOR_SAFE";
    public static final String POOL_OIL_RIG = "POOL_OIL_RIG";
    public static final String POOL_RTG = "POOL_RTG";
    public static final String POOL_REPAIR_MATERIALS = "POOL_REPAIR_MATERIALS";

    public static void init() {

        new ItemPool(POOL_MACHINE_PARTS)
                .add("plate_steel", 1, 5, 5)
                .add("shell_steel", 1, 3, 3)
                .add("plate_polymer", 1, 6, 5)
                .add("bolt_steel", 4, 16, 3)
                .add("bolt_tungsten", 4, 16, 3)
                .add("coil_tungsten", 1, 2, 5)
                .add("motor", 1, 2, 4)
                .add("coil_copper", 1, 3, 4)
                .add("coil_copper_torus", 1, 2, 3)
                .add("wire_red_copper", 1, 8, 5)
                .add("piston_selenium", 1, 1, 3)
                .add("battery_pack_battery_lead", 1, 1, 3)
                .add("vacuum_tube", 1, 2, 4)
                .add("pcb", 1, 3, 5)
                .add("capacitor", 1, 1, 3)
                .add("blade_titanium", 1, 8, 1)
                .add("blueprint_folder", 1, 1, 1)
                .build();

        new ItemPool(POOL_NUKE_FUEL)
                .add("billet_uranium", 1, 4, 4)
                .add("billet_th232", 1, 3, 3)
                .add("billet_uranium_fuel", 1, 3, 5)
                .add("billet_mox_fuel", 1, 3, 5)
                .add("billet_thorium_fuel", 1, 3, 3)
                .add("billet_ra226be", 1, 2, 2)
                .add("billet_beryllium", 1, 1, 1)
                .add("nugget_u233", 1, 1, 1)
                .add("nugget_uranium_fuel", 1, 1, 1)
                .add("rod_zirnox_empty", 1, 3, 3)
                .add("graphite_ingot", 1, 4, 3)
                .add("pile_rod_uranium", 2, 5, 3)
                .add("pile_rod_source", 1, 2, 2)
                .add("reacher", 1, 1, 3)
                .add("screwdriver", 1, 1, 2)
                .build();

        new ItemPool(POOL_SILO)
                .add("missile_generic", 1, 1, 4)
                .add("missile_incendiary", 1, 1, 4)
                .add("gas_mask_m65", 0, 1, 1, 5)
                .add("battery_pack_battery_lead", 1, 1, 3)
                .add("designator", 1, 1, 5)
                .add("thruster_small", 1, 1, 5)
                .add("thruster_medium", 1, 1, 4)
                .add("fuel_tank_small", 1, 1, 5)
                .add("fuel_tank_medium", 1, 1, 4)
                .add("bomb_caller", 1, 1, 1)
                .add("bomb_caller_orange", 1, 1, 1)
                .add("bottle_nuka", 1, 3, 10)
                .build();

        new ItemPool(POOL_OFFICE_TRASH)
                .add("minecraft:paper", 1, 12, 10)
                .add("minecraft:book", 1, 3, 4)
                .add("twinkie", 1, 2, 6)
                .add("coffee", 1, 1, 4)
                .add("flame_politics", 1, 1, 2)
                .add("ring_pull", 1, 1, 4)
                .add("can_empty", 1, 1, 2)
                .add("can_creature", 1, 2, 2)
                .add("can_smart", 1, 3, 2)
                .add("can_mrsugar", 1, 2, 2)
                .add("cap_nuka", 1, 16, 2)
                .add(() -> com.hbm_m.item.tool.ItemGuideBook.make(com.hbm_m.item.tool.ItemGuideBook.BookType.STARTER), 1, 1, 1)
                .add("puter", 1, 1, 1)
                .add("blueprint_folder", 1, 1, 1)
                .add("coin_token", 1, 1, 2)
                .build();

        new ItemPool(POOL_FILING_CABINET)
                .add("minecraft:paper", 1, 12, 240)
                .add("minecraft:book", 1, 3, 90)
                .add("minecraft:map", 1, 1, 50)
                .add("minecraft:writable_book", 1, 1, 30)
                .add("cigarette", 1, 16, 20)
                .add("dust", 1, 1, 40)
                .add("dust_tiny", 1, 3, 75)
                .add("ink", 1, 1, 1)
                .add("screwdriver", 1, 1, 10)
                .add("blueprint_folder", 1, 1, 5)
                .add("coin_token", 1, 1, 30)
                .build();

        new ItemPool(POOL_SOLID_FUEL)
                .add("solid_fuel", 1, 5, 1)
                .add("solid_fuel_presto", 1, 2, 2)
                .add("ball_dynamite", 1, 4, 2)
                .add("coke_petroleum", 1, 3, 1)
                .add("minecraft:redstone", 1, 3, 1)
                .add("niter", 1, 3, 1)
                .build();

        new ItemPool(POOL_VAULT_LAB)
                .add(() -> com.hbm_m.item.tool.ItemBlowtorch.getEmptyTool(ModItems.BLOWTORCH.get()), 1, 1, 4)
                .add("chemistry_set", 1, 1, 15)
                .add("screwdriver", 1, 1, 10)
                .add("nugget_mercury", 1, 1, 3)
                .add("morning_glory", 1, 1, 1)
                .add("filter_coal", 1, 1, 5)
                .add("dust", 1, 3, 25)
                .add("minecraft:paper", 1, 2, 15)
                .add("cell_empty", 1, 1, 5)
                .add("minecraft:glass_bottle", 1, 1, 5)
                .add("iodine_powder", 1, 1, 1)
                .add("bromide_powder", 1, 1, 1)
                .add("cobalt_powder", 1, 1, 1)
                .add("neodymium_powder", 1, 1, 1)
                .add("boron_powder", 1, 1, 1)
                .add("blueprint_folder_discover", 1, 1, 1)
                .build();

        new ItemPool(POOL_VAULT_LOCKERS)
                .add("robes_helmet", 1, 1, 1)
                .add("robes_plate", 1, 1, 1)
                .add("robes_legs", 1, 1, 1)
                .add("robes_boots", 1, 1, 1)
                .add("jackt", 1, 1, 1)
                .add("jackt2", 1, 1, 1)
                .add("gas_mask_m65", 0, 1, 1, 2)
                .add("gas_mask_mono", 1, 1, 2)
                .add("goggles", 1, 1, 2)
                .add("gas_mask_filter", 1, 1, 4)
                .add("flame_opinion", 1, 3, 5)
                .add("flame_conspiracy", 1, 3, 5)
                .add("flame_politics", 1, 3, 5)
                .add("definitelyfood", 2, 7, 5)
                .add("cigarette", 1, 8, 5)
                .add("armor_polish", 1, 1, 3)
                .add("gun_kit_1", 1, 1, 3)
                .add("rag", 1, 3, 5)
                .add("minecraft:paper", 1, 6, 7)
                .add("minecraft:clock", 1, 1, 3)
                .add("minecraft:book", 1, 5, 10)
                .add("minecraft:experience_bottle", 1, 3, 1)
                .add("blueprint_folder", 1, 1, 1)
                .add("blueprint_folder_discover", 1, 1, 1)
                .add("ammo_container", 1, 1, 1)
                .add("coin_token", 1, 1, 5)
                .build();

        new ItemPool(POOL_METEOR_SAFE)
                .add("book_of_", 1, 1, 1)
                .add("stamp_book_printing1", 1, 1, 1)
                .add("stamp_book_printing2", 1, 1, 1)
                .add("stamp_book_printing3", 1, 1, 1)
                .add("stamp_book_printing4", 1, 1, 1)
                .add("stamp_book_printing5", 1, 1, 1)
                .add("stamp_book_printing6", 1, 1, 1)
                .add("stamp_book_printing7", 1, 1, 1)
                .add("stamp_book_printing8", 1, 1, 1)
                .build();

        new ItemPool(POOL_OIL_RIG)
                .add("oil_detector", 1, 1, 1)
                .add(() -> com.hbm_m.item.liquids.ItemFluidTank.make(ModItems.CANISTER_FULL.get(), com.hbm_m.inventory.fluid.ModFluids.CRUDE_OIL.getSource(), 1), 1, 4, 5)
                .add("canister_empty", 4, 16, 10)
                .add("analog_circuit", 1, 4, 1)
                .add("capacitor", 1, 1, 3)
                .build();

        new ItemPool(POOL_RTG)
                .add("pellet_rtg_depleted_lead", 1, 1, 40)
                .add("pellet_rtg_weak", 0, 1, 1)
                .build();

        new ItemPool(POOL_REPAIR_MATERIALS)
                .add("ingot_aluminium", 2, 8, 3)
                .add("steel_ingot", 0, 12, 4)
                .add("plate_aluminium", 5, 12, 3)
                .add("plate_iron", 6, 16, 3)
                .add("plate_steel", 2, 12, 2)
                .add("tungsten_ingot", 0, 2, 1)
                .add("deco_aluminum", 12, 24, 4)
                .add("deco_steel", 5, 12, 2)
                .add("block_aluminium", 0, 2, 1)
                .add("block_steel", 0, 1, 1)
                .add("bolt_steel", 4, 16, 3)
                .add("vacuum_tube", 1, 2, 4)
                .add("analog_circuit", 1, 3, 5)
                .add("capacitor", 1, 1, 3)
                .build();
    }
}
