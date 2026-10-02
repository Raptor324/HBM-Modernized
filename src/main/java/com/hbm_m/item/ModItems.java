package com.hbm_m.item;

import static com.hbm_m.lib.RefStrings.MODID;
import static com.hbm_m.util.CompletionStatus.*;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.armormod.item.ItemModBattery;
import com.hbm_m.armormod.item.ItemModHealth;
import com.hbm_m.armormod.item.ItemModRadProtection;
// import com.hbm_m.armormod.item.ItemModCladding;
// import com.hbm_m.armormod.item.ItemModExtra;
// import com.hbm_m.armormod.item.ItemModHealth;
// import com.hbm_m.armormod.item.ItemModKevlar;
// import com.hbm_m.armormod.item.ItemModRadProtection;
// import com.hbm_m.armormod.item.ItemModServos;
import com.hbm_m.block.ModBlocks;
import com.hbm_m.block.machines.crates.CrateType;
import com.hbm_m.blockentity.machines.rbmk.IRBMKFluxReceiver.NType;
import com.hbm_m.effect.ModEffects;
import com.hbm_m.entity.ModEntities;
import com.hbm_m.entity.grenades.GrenadeIfType;
import com.hbm_m.entity.grenades.GrenadeType;
import com.hbm_m.item.crates.CrateItem;
import com.hbm_m.item.designator.ItemDesignator;
import com.hbm_m.item.designator.ItemDesignatorManual;
import com.hbm_m.item.designator.ItemDesignatorRange;
import com.hbm_m.item.fekal_electric.EnumBatteryPack;
import com.hbm_m.item.fekal_electric.ItemBatteryPack;
import com.hbm_m.item.fekal_electric.ItemCreativeBattery;
import com.hbm_m.item.fekal_electric.ModBatteryItem;
import com.hbm_m.item.nuclear.WatzPelletItem;
import com.hbm_m.item.nuclear.WatzPelletType;
import com.hbm_m.item.food.ItemConserve;
import com.hbm_m.item.food.ItemEnergyDrink;
import com.hbm_m.item.food.ModFoods;
import com.hbm_m.item.hazmat.HazmatArmorItem;
import com.hbm_m.item.hazmat.HazmatMaskArmorItem;
import com.hbm_m.item.gasmask.ArmorGasMaskItem;
import com.hbm_m.item.gasmask.ArmorGasMaskItem.Variant;
import com.hbm_m.item.gasmask.ItemGasMaskFilter;
import com.hbm_m.item.tools_and_armor.RagMaskItem;
import com.hbm_m.armormod.item.ItemModGasmask;
import com.hbm_m.item.special.ItemCigarette;
import com.hbm_m.item.special.ModConsumables;
import com.hbm_m.item.grenades_and_activators.AirBombItem;
import com.hbm_m.item.grenades_and_activators.AirNukeBombItem;
import com.hbm_m.item.grenades_and_activators.AirstrikeItem;
import com.hbm_m.item.grenades_and_activators.AirstrikeItem.AirstrikeType;
import com.hbm_m.item.grenades_and_activators.DetonatorItem;
import com.hbm_m.item.grenades_and_activators.GrenadeIfItem;
import com.hbm_m.item.grenades_and_activators.GrenadeItem;
import com.hbm_m.item.grenades_and_activators.GrenadeNucItem;
import com.hbm_m.item.grenades_and_activators.MultiDetonatorItem;
import com.hbm_m.item.grenades_and_activators.RangeDetonatorItem;
import com.hbm_m.item.tool.ConfettiTesterItem;
import com.hbm_m.item.tool.RangefinderItem;
import com.hbm_m.item.industrial.EternalFuelItem;
import com.hbm_m.item.industrial.FuelItem;
import com.hbm_m.item.industrial.ItemAssemblyTemplate;
import com.hbm_m.item.industrial.ItemBlades;
import com.hbm_m.item.industrial.ItemBlueprintFolder;
import com.hbm_m.item.industrial.ItemMachineUpgrade;
import com.hbm_m.item.industrial.ItemStamp;
import com.hbm_m.item.industrial.ItemTemplateFolder;
import com.hbm_m.item.industrial.ZirnoxRodItem;
import com.hbm_m.item.material.MaterialShape;
import com.hbm_m.item.material.ModMaterialItems;
import com.hbm_m.item.material.ModMaterials;
import com.hbm_m.item.liquids.FluidBarrelItem;
import com.hbm_m.item.liquids.FluidDuctItem;
import com.hbm_m.item.liquids.FluidIdentifierItem;
import com.hbm_m.item.liquids.InfiniteFluidItem;
import com.hbm_m.item.missile.MissileItem;
import com.hbm_m.item.radiation_meter.ItemDigammaDiagnostic;
import com.hbm_m.item.radiation_meter.ItemDosimeter;
import com.hbm_m.item.radiation_meter.ItemGeigerCounter;
import com.hbm_m.item.rbmk.RBMKLidItem;
import com.hbm_m.item.rbmk.RBMKPelletItem;
import com.hbm_m.item.rbmk.RBMKRodItem;
import com.hbm_m.item.scanners.DepthOresScannerItem;
import com.hbm_m.item.scanners.OilDetectorItem;
import com.hbm_m.item.tags_and_tiers.ItemSimpleConsumable;
import com.hbm_m.item.tools_and_armor.ModArmorMaterials;
import com.hbm_m.item.tools_and_armor.ModArmorMaterialsAccess;
import com.hbm_m.item.tools_and_armor.ModAxeItem;
import com.hbm_m.item.tools_and_armor.ModPickaxeItem;
import com.hbm_m.item.tools_and_armor.ModShovelItem;
import com.hbm_m.item.tools_and_armor.ModSwordItem;
import com.hbm_m.item.tools_and_armor.ModToolTiers;
import com.hbm_m.item.tools_and_armor.ScrewdriverItem;
import com.hbm_m.multiblock.DoorBlockItem;
import com.hbm_m.item.machine.ItemRTGPellet;
import com.hbm_m.multiblock.MultiblockBlockItem;
import com.hbm_m.powerarmor.AJRArmor;
import com.hbm_m.powerarmor.AJROArmor;
import com.hbm_m.powerarmor.BismuthArmor;
import com.hbm_m.powerarmor.DNTArmor;
import com.hbm_m.powerarmor.T51Armor;
import com.hbm_m.sound.ModSounds;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.Rarity;
import com.hbm_m.platform.PlatformHooks;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.level.Level;
import dev.architectury.core.item.ArchitecturySpawnEggItem;


public class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(MODID, Registries.ITEM);

    // --- Standalone Pulver ohne Ingot-Gegenstueck (aus Original-Rezepten portiert, DEV-Tab bis einsortiert) ---
    public static final RegistrySupplier<Item> POWDER_SAWDUST      = registerItem("sawdust_powder", () -> new FuelItem(new Item.Properties(), 100));
    public static final RegistrySupplier<Item> POWDER_YELLOWCAKE   = registerItem("yellowcake_powder", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> POWDER_BALEFIRE     = registerItem("balefire_powder", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> POWDER_THERMITE     = registerItem("thermite_powder", () -> new Item(new Item.Properties()));
    // Энерго-порошок (ориг. 4383 powder_power, ItemCustomLore uncommon, текстура powder_energy_alt).
    public static final RegistrySupplier<Item> POWDER_POWER        = registerItem("powder_power", () -> new Item(new Item.Properties().rarity(Rarity.UNCOMMON)));
    public static final RegistrySupplier<Item> POWDER_FERTILIZER   = registerItem("fertilizer_powder", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> POWDER_FLUX         = registerItem("flux_powder", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> POWDER_MAGIC        = registerItem("magic_powder", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> POWDER_ICE          = registerItem("ice_powder", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> POWDER_SPARK_MIX    = registerItem("spark_mix_powder", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> POWDER_SEMTEX_MIX   = registerItem("semtex_mix_powder", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> POWDER_DESH_READY   = registerItem("desh_ready_powder", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> POWDER_COLTAN       = registerItem("coltan_powder", () -> new Item(new Item.Properties()));

    static {
        // Единый реестр материалов: слитки, порошки, tiny-порошки, наггетсы, биллеты,
        // кристаллы, пластины, провода, лом — по (материал, форма) из ModMaterials.
        // Блоки хранения регистрирует ModBlocks (форма BLOCK).
        ModMaterialItems.registerAll();
        // Развёртка мета-предметов оригинала (dye/crayon/casing/part_*/waste/nuclear_waste_*):
        // по одному предмету на мету — для 1:1 слотов вкладки Parts.
        PartTabMetaItems.registerAll();
    }

    // РЕЕСТР МАТЕРИАЛОВ: доступ через ModMaterialItems.get(ModMaterials.X, MaterialShape.Y)
    
    public static final int SLOT_HELMET = 0;
    public static final int SLOT_CHEST = 1;
    public static final int SLOT_LEGS = 2;
    public static final int SLOT_BOOTS = 3;
    public static final int SLOT_BATTERY = 8;  //  ArmorModificationHelper.battery
    public static final int SLOT_SPECIAL = 7;  //  ArmorModificationHelper.extra
    public static final int SLOT_INSERT = 6;
    public static final int SLOT_CLADDING = 5; //  ArmorModificationHelper.cladding
    public static final int SLOT_SERVOS = 4;   //  ArmorModificationHelper.servos

    public static final int SLOT_HELMET_ONLY = 0;
    public static final int SLOT_PLATE_ONLY = 1;
    public static final int SLOT_LEGS_ONLY = 2;
    public static final int SLOT_BOOTS_ONLY = 3;
    public static final int SLOT_KEVLAR = 6;

    public static final int BATTERY_CAPACITY = 1_000_000;

    public static final RegistrySupplier<Item> STRAWBERRY = registerItem("strawberry", 
            () -> new Item(new Item.Properties().food(ModFoods.STRAWBERRY)));
    public static final RegistrySupplier<Item> CANNED_ASBESTOS = registerItem("canned_asbestos", 
            () -> new ItemConserve(new Item.Properties().food(ModFoods.CANNED_ASBESTOS)));
    public static final RegistrySupplier<Item> CANNED_ASS = registerItem("canned_ass", 
            () -> new ItemConserve(new Item.Properties().food(ModFoods.CANNED_ASS)));
    public static final RegistrySupplier<Item> CANNED_BARK = registerItem("canned_bark", 
            () -> new ItemConserve(new Item.Properties().food(ModFoods.CANNED_BARK)));
    public static final RegistrySupplier<Item> CANNED_BEEF = registerItem("canned_beef", 
            () -> new ItemConserve(new Item.Properties().food(ModFoods.CANNED_BEEF)));
    public static final RegistrySupplier<Item> CANNED_BHOLE = registerItem("canned_bhole", 
            () -> new ItemConserve(new Item.Properties().food(ModFoods.CANNED_BHOLE)));
    public static final RegistrySupplier<Item> CANNED_CHEESE = registerItem("canned_cheese", 
            () -> new ItemConserve(new Item.Properties().food(ModFoods.CANNED_CHEESE)));
    public static final RegistrySupplier<Item> CANNED_CHINESE = registerItem("canned_chinese", 
            () -> new ItemConserve(new Item.Properties().food(ModFoods.CANNED_CHINESE)));
    public static final RegistrySupplier<Item> CANNED_DIESEL = registerItem("canned_diesel", 
            () -> new ItemConserve(new Item.Properties().food(ModFoods.CANNED_DIESEL)));
    public static final RegistrySupplier<Item> CANNED_FIST = registerItem("canned_fist", 
            () -> new ItemConserve(new Item.Properties().food(ModFoods.CANNED_FIST)));
    public static final RegistrySupplier<Item> CANNED_FRIED = registerItem("canned_fried", 
            () -> new ItemConserve(new Item.Properties().food(ModFoods.CANNED_FRIED)));
    public static final RegistrySupplier<Item> CANNED_HOTDOGS = registerItem("canned_hotdogs", 
            () -> new ItemConserve(new Item.Properties().food(ModFoods.CANNED_HOTDOGS)));
    public static final RegistrySupplier<Item> CANNED_JIZZ = registerItem("canned_jizz", 
            () -> new ItemConserve(new Item.Properties().food(ModFoods.CANNED_JIZZ)));
    public static final RegistrySupplier<Item> CANNED_KEROSENE = registerItem("canned_kerosene", 
            () -> new ItemConserve(new Item.Properties().food(ModFoods.CANNED_KEROSENE)));
    public static final RegistrySupplier<Item> CANNED_LEFTOVERS = registerItem("canned_leftovers", 
            () -> new ItemConserve(new Item.Properties().food(ModFoods.CANNED_LEFTOVERS)));
    public static final RegistrySupplier<Item> CANNED_MILK = registerItem("canned_milk", 
            () -> new ItemConserve(new Item.Properties().food(ModFoods.CANNED_MILK)));
    public static final RegistrySupplier<Item> CANNED_MYSTERY = registerItem("canned_mystery", 
            () -> new ItemConserve(new Item.Properties().food(ModFoods.CANNED_MYSTERY)));
    public static final RegistrySupplier<Item> CANNED_NAPALM = registerItem("canned_napalm", 
            () -> new ItemConserve(new Item.Properties().food(ModFoods.CANNED_NAPALM)));
    public static final RegistrySupplier<Item> CANNED_OIL = registerItem("canned_oil", 
            () -> new ItemConserve(new Item.Properties().food(ModFoods.CANNED_OIL)));
    public static final RegistrySupplier<Item> CANNED_PASHTET = registerItem("canned_pashtet", 
            () -> new ItemConserve(new Item.Properties().food(ModFoods.CANNED_PASHTET)));
    public static final RegistrySupplier<Item> CANNED_PIZZA = registerItem("canned_pizza", 
            () -> new ItemConserve(new Item.Properties().food(ModFoods.CANNED_PIZZA)));
    public static final RegistrySupplier<Item> CANNED_RECURSION = registerItem("canned_recursion", 
            () -> new ItemConserve(new Item.Properties().food(ModFoods.CANNED_RECURSION)));
    public static final RegistrySupplier<Item> CANNED_SPAM = registerItem("canned_spam", 
            () -> new ItemConserve(new Item.Properties().food(ModFoods.CANNED_SPAM)));
    public static final RegistrySupplier<Item> CANNED_STEW = registerItem("canned_stew", 
            () -> new ItemConserve(new Item.Properties().food(ModFoods.CANNED_STEW)));
    public static final RegistrySupplier<Item> CANNED_TOMATO = registerItem("canned_tomato", 
            () -> new ItemConserve(new Item.Properties().food(ModFoods.CANNED_TOMATO)));
    public static final RegistrySupplier<Item> CANNED_TUNA = registerItem("canned_tuna", 
            () -> new ItemConserve(new Item.Properties().food(ModFoods.CANNED_TUNA)));
    public static final RegistrySupplier<Item> CANNED_TUBE = registerItem("canned_tube", 
            () -> new ItemConserve(new Item.Properties().food(ModFoods.CANNED_TUBE)));
    public static final RegistrySupplier<Item> CANNED_YOGURT = registerItem("canned_yogurt", 
            () -> new ItemConserve(new Item.Properties().food(ModFoods.CANNED_YOGURT)));


    public static final RegistrySupplier<Item> CAN_BEPIS = registerItem("can_bepis", 
            () -> new ItemEnergyDrink(new Item.Properties().food(ItemEnergyDrink.CAN_BEPIS)));
    public static final RegistrySupplier<Item> CAN_BREEN = registerItem("can_breen", 
            () -> new ItemEnergyDrink(new Item.Properties().food(ItemEnergyDrink.CAN_BREEN)));
    public static final RegistrySupplier<Item> CAN_CREATURE = registerItem("can_creature", 
            () -> new ItemEnergyDrink(new Item.Properties().food(ItemEnergyDrink.CAN_CREATURE)));
    public static final RegistrySupplier<Item> CAN_EMPTY = registerItem("can_empty", 
            () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> CAN_LUNA = registerItem("can_luna", 
            () -> new ItemEnergyDrink(new Item.Properties().food(ItemEnergyDrink.CAN_LUNA)));
    public static final RegistrySupplier<Item> CAN_MRSUGAR = registerItem("can_mrsugar", 
            () -> new ItemEnergyDrink(new Item.Properties().food(ItemEnergyDrink.CAN_MRSUGAR)));
    public static final RegistrySupplier<Item> CAN_MUG = registerItem("can_mug", 
            () -> new ItemEnergyDrink(new Item.Properties().food(ItemEnergyDrink.CAN_MUG)));
    public static final RegistrySupplier<Item> CAN_OVERCHARGE = registerItem("can_overcharge", 
            () -> new ItemEnergyDrink(new Item.Properties().food(ItemEnergyDrink.CAN_OVERCHARGE)));
    public static final RegistrySupplier<Item> CAN_REDBOMB = registerItem("can_redbomb", 
            () -> new ItemEnergyDrink(new Item.Properties().food(ItemEnergyDrink.CAN_REDBOMB)));
    public static final RegistrySupplier<Item> CAN_SMART = registerItem("can_smart", 
            () -> new ItemEnergyDrink(new Item.Properties().food(ItemEnergyDrink.CAN_SMART)));

    public static final RegistrySupplier<Item> STARMETAL_SWORD = registerItem("starmetal_sword", 
            () -> new ModSwordItem(ModToolTiers.STARMETAL, 7, -2, new Item.Properties()));
    public static final RegistrySupplier<Item> STARMETAL_AXE = registerItem("starmetal_axe", 
            () -> new ModAxeItem(ModToolTiers.STARMETAL, 15, 1, new Item.Properties()));
    public static final RegistrySupplier<Item> STARMETAL_PICKAXE = registerItem("starmetal_pickaxe", 
            () -> new ModPickaxeItem(ModToolTiers.STARMETAL, 3, 1, new Item.Properties(), 6, 3, 1, 5));
    public static final RegistrySupplier<Item> STARMETAL_SHOVEL = registerItem("starmetal_shovel", 
            () -> new ModShovelItem(ModToolTiers.STARMETAL, 0, 0, new Item.Properties()));
    public static final RegistrySupplier<Item> STARMETAL_HOE = registerItem("starmetal_hoe", 
            //? if < 1.21.1 {
            () -> new HoeItem(ModToolTiers.STARMETAL, 0, 0f, new Item.Properties()));
            //?} else {
            /*() -> new HoeItem(ModToolTiers.STARMETAL, new Item.Properties()));
            *///?}

    public static final RegistrySupplier<Item> ALLOY_SWORD = registerItem("alloy_sword", 
        () -> new ModSwordItem(ModToolTiers.ALLOY, 5, 2, new Item.Properties()));

    public static final RegistrySupplier<Item> ALLOY_AXE = registerItem("alloy_axe", 
            () -> new ModAxeItem(ModToolTiers.ALLOY, 9, 1, new Item.Properties(), 3, 1));

    public static final RegistrySupplier<Item> ALLOY_PICKAXE = registerItem("alloy_pickaxe", 
            () -> new ModPickaxeItem(ModToolTiers.ALLOY, 2, 1, new Item.Properties(), 3, 0, 0, 0));

    public static final RegistrySupplier<Item> ALLOY_SHOVEL = registerItem("alloy_shovel", 
            () -> new ModShovelItem(ModToolTiers.ALLOY, 0, 0, new Item.Properties(), 3, 0, 2));

    public static final RegistrySupplier<Item> ALLOY_HOE = registerItem("alloy_hoe", 
            //? if < 1.21.1 {
            () -> new HoeItem(ModToolTiers.ALLOY, 0, 0f, new Item.Properties()));
            //?} else {
            /*() -> new HoeItem(ModToolTiers.ALLOY, new Item.Properties()));
            *///?}

    public static final RegistrySupplier<Item> STEEL_SWORD = registerItem("steel_sword", 
            () -> new ModSwordItem(ModToolTiers.STEEL, 4, 2, new Item.Properties()));
    public static final RegistrySupplier<Item> STEEL_AXE = registerItem("steel_axe", 
            () -> new ModAxeItem(ModToolTiers.STEEL, 7, 1, new Item.Properties()));
    public static final RegistrySupplier<Item> STEEL_PICKAXE = registerItem("steel_pickaxe", 
            () -> new ModPickaxeItem(ModToolTiers.STEEL, 1, 1, new Item.Properties()));
    public static final RegistrySupplier<Item> STEEL_SHOVEL = registerItem("steel_shovel", 
            () -> new ModShovelItem(ModToolTiers.STEEL, 0, 0, new Item.Properties()));
    public static final RegistrySupplier<Item> STEEL_HOE = registerItem("steel_hoe", 
            //? if < 1.21.1 {
            () -> new HoeItem(ModToolTiers.STEEL, 0, 0, new Item.Properties()));
            //?} else {
            /*() -> new HoeItem(ModToolTiers.STEEL, new Item.Properties()));
            *///?}

    public static final RegistrySupplier<Item> TITANIUM_SWORD = registerItem("titanium_sword", 
            () -> new ModSwordItem(ModToolTiers.TITANIUM, 2, 3, new Item.Properties()));
    public static final RegistrySupplier<Item> TITANIUM_AXE = registerItem("titanium_axe", 
            () -> new ModAxeItem(ModToolTiers.TITANIUM, 8, 1, new Item.Properties()));

    // Meteorite swords (registered so recipes can produce them)
    public static final RegistrySupplier<Item> METEORITE_SWORD = registerItem("meteorite_sword", 
            () -> new ModSwordItem(ModToolTiers.TITANIUM, 3, -2, new Item.Properties()));
    public static final RegistrySupplier<Item> METEORITE_SWORD_SEARED = registerItem("meteorite_sword_seared", 
            () -> new ModSwordItem(ModToolTiers.TITANIUM, 3, -2, new Item.Properties()));
    // Original chain continues: seared -> reforged -> hardened -> alloyed -> machined -> treated -> etched -> bred -> ...
    // Only hardened/alloyed are added here (needed by the Blast Furnace recipe below); the rest of the chain
    // (Press/Crystallizer/Breeder steps) is tracked separately and not yet wired up.
    public static final RegistrySupplier<Item> METEORITE_SWORD_HARDENED = registerItem("meteorite_sword_hardened", 
            () -> new ModSwordItem(ModToolTiers.TITANIUM, 3, -2, new Item.Properties()));
    public static final RegistrySupplier<Item> METEORITE_SWORD_ALLOYED = registerItem("meteorite_sword_alloyed", 
            () -> new ModSwordItem(ModToolTiers.TITANIUM, 3, -2, new Item.Properties()));
    // Fusions-Brueter-Schritt der Schwertkette (Original: meteorite_sword_irradiated -> _fused,
    // siehe TileEntityFusionBreeder.processSolid).
    public static final RegistrySupplier<Item> METEORITE_SWORD_IRRADIATED = registerItem("meteorite_sword_irradiated", 
            () -> new ModSwordItem(ModToolTiers.TITANIUM, 3, -2, new Item.Properties()));
    public static final RegistrySupplier<Item> METEORITE_SWORD_FUSED = registerItem("meteorite_sword_fused", 
            () -> new ModSwordItem(ModToolTiers.TITANIUM, 3, -2, new Item.Properties()));
    public static final RegistrySupplier<Item> TITANIUM_PICKAXE = registerItem("titanium_pickaxe", 
            () -> new ModPickaxeItem(ModToolTiers.TITANIUM, 1, 1, new Item.Properties()));
    public static final RegistrySupplier<Item> DRILL_TITANIUM = registerItem("drill_titanium", 
            () -> new Item(new Item.Properties().stacksTo(1)));
    public static final RegistrySupplier<Item> TITANIUM_SHOVEL = registerItem("titanium_shovel", 
            () -> new ModShovelItem(ModToolTiers.TITANIUM, 0, 0, new Item.Properties()));
    public static final RegistrySupplier<Item> TITANIUM_HOE = registerItem("titanium_hoe", 
            //? if < 1.21.1 {
            () -> new HoeItem(ModToolTiers.TITANIUM, 0, 0, new Item.Properties()));
            //?} else {
            /*() -> new HoeItem(ModToolTiers.TITANIUM, new Item.Properties()));
            *///?}


    public static final RegistrySupplier<Item> GRENADE = registerItem("grenade", 
        () -> new GrenadeItem(new Item.Properties(), GrenadeType.STANDARD, ModEntities.GRENADE_PROJECTILE));

    public static final RegistrySupplier<Item> GRENADEHE = registerItem("grenadehe", 
        () -> new GrenadeItem(new Item.Properties(), GrenadeType.HE, ModEntities.GRENADEHE_PROJECTILE));

    public static final RegistrySupplier<Item> GRENADEFIRE = registerItem("grenadefire", 
        () -> new GrenadeItem(new Item.Properties(), GrenadeType.FIRE, ModEntities.GRENADEFIRE_PROJECTILE));

    public static final RegistrySupplier<Item> GRENADESLIME = registerItem("grenadeslime", 
        () -> new GrenadeItem(new Item.Properties(), GrenadeType.SLIME, ModEntities.GRENADESLIME_PROJECTILE));

    public static final RegistrySupplier<Item> GRENADESMART = registerItem("grenadesmart", 
        () -> new GrenadeItem(new Item.Properties(), GrenadeType.SMART, ModEntities.GRENADESMART_PROJECTILE));

    public static final RegistrySupplier<Item> GRENADE_IF = registerItem("grenade_if", 
            () -> new GrenadeIfItem(new Item.Properties(), GrenadeIfType.GRENADE_IF, ModEntities.GRENADE_IF_PROJECTILE));

    public static final RegistrySupplier<Item> GRENADE_IF_HE = registerItem("grenade_if_he", 
            () -> new GrenadeIfItem(new Item.Properties(), GrenadeIfType.GRENADE_IF_HE, ModEntities.GRENADE_IF_HE_PROJECTILE));

    public static final RegistrySupplier<Item> GRENADE_IF_SLIME = registerItem("grenade_if_slime", 
            () -> new GrenadeIfItem(new Item.Properties(), GrenadeIfType.GRENADE_IF_SLIME, ModEntities.GRENADE_IF_SLIME_PROJECTILE));

    public static final RegistrySupplier<Item> GRENADE_IF_FIRE = registerItem("grenade_if_fire", 
            () -> new GrenadeIfItem(new Item.Properties(), GrenadeIfType.GRENADE_IF_FIRE, ModEntities.GRENADE_IF_FIRE_PROJECTILE));

    public static final RegistrySupplier<Item> GRENADE_NUC = registerItem("grenade_nuc", 
            () -> new GrenadeNucItem(new Item.Properties(), ModEntities.GRENADE_NUC_PROJECTILE));

    public static final RegistrySupplier<Item> AIRBOMB_A = registerItem("airbomb_a", 
            () -> new AirBombItem(new Item.Properties(), ModEntities.AIRBOMB_PROJECTILE));
    public static final RegistrySupplier<Item> AIRNUKEBOMB_A = registerItem("airnukebomb_a", 
            () -> new AirNukeBombItem(new Item.Properties(), ModEntities.AIRNUKEBOMB_PROJECTILE));
    public static final RegistrySupplier<Item> BOT_PRIME_SPAWN_EGG = registerItem("bot_prime_spawn_egg", 
            () -> new ArchitecturySpawnEggItem(ModEntities.BOT_PRIME_HEAD, 0x3a3f45, 0xc03020, new Item.Properties()));
    public static final RegistrySupplier<Item> UFO_SPAWN_EGG = registerItem("ufo_spawn_egg", 
            () -> new ArchitecturySpawnEggItem(ModEntities.UFO, 0x505a64, 0x30ff90, new Item.Properties()));
    public static final RegistrySupplier<Item> RAD_BEAST_SPAWN_EGG = registerItem("rad_beast_spawn_egg", 
            () -> new ArchitecturySpawnEggItem(ModEntities.RAD_BEAST, 0x1a3d1a, 0x7fff3f, new Item.Properties()));
    public static final RegistrySupplier<Item> MASKMAN_SPAWN_EGG = registerItem("maskman_spawn_egg", 
            () -> new ArchitecturySpawnEggItem(ModEntities.MASKMAN, 0x2b2b2b, 0xa01010, new Item.Properties()));

    public static final RegistrySupplier<Item> NOLO_SPAWN_EGG = registerItem("nolo_spawn_egg", 
            () -> new ArchitecturySpawnEggItem(ModEntities.NOLO, 0x8b5e3c, 0xf0d8b0, new Item.Properties()));

    public static final RegistrySupplier<Item> ENTITY_MOB_TAINTED_CREEPER_SPAWN_EGG = ITEMS.register(
            "entity_mob_tainted_creeper_spawn_egg",
            () -> new ArchitecturySpawnEggItem(ModEntities.ENTITY_MOB_TAINTED_CREEPER, 0x813b9b, 0xd71fdd, new Item.Properties()));

    public static final RegistrySupplier<Item> ENTITY_MOB_VOLATILE_CREEPER_SPAWN_EGG = ITEMS.register(
            "entity_mob_volatile_creeper_spawn_egg",
            () -> new ArchitecturySpawnEggItem(ModEntities.ENTITY_MOB_VOLATILE_CREEPER, 0xC28153, 0x4D382C, new Item.Properties()));

    public static final RegistrySupplier<Item> ENTITY_MOB_PHOSGENE_CREEPER_SPAWN_EGG = ITEMS.register(
            "entity_mob_phosgene_creeper_spawn_egg",
            () -> new ArchitecturySpawnEggItem(ModEntities.ENTITY_MOB_PHOSGENE_CREEPER, 0xE3D398, 0xB8A06B, new Item.Properties()));

    public static final RegistrySupplier<Item> ENTITY_MOB_GOLD_CREEPER_SPAWN_EGG = ITEMS.register(
            "entity_mob_gold_creeper_spawn_egg",
            () -> new ArchitecturySpawnEggItem(ModEntities.ENTITY_MOB_GOLD_CREEPER, 0xECC136, 0x9E8B3E, new Item.Properties()));

    public static final RegistrySupplier<Item> ENTITY_MOB_NUCLEAR_CREEPER_SPAWN_EGG = ITEMS.register(
            "entity_mob_nuclear_creeper_spawn_egg",
            () -> new ArchitecturySpawnEggItem(ModEntities.ENTITY_MOB_NUCLEAR_CREEPER, 0x204131, 0x75CE00, new Item.Properties()));

    // Р‘Р РћРќРЇ Р“РћР РќРЇРљРђ:
    public static final RegistrySupplier<Item> ALLOY_HELMET = registerItem("alloy_helmet", 
            () -> new ArmorItem(ModArmorMaterialsAccess.holder(ModArmorMaterials.ALLOY), ArmorItem.Type.HELMET, ModArmorMaterialsAccess.armorProps(ModArmorMaterials.ALLOY, ArmorItem.Type.HELMET)));
    public static final RegistrySupplier<Item> ALLOY_CHESTPLATE = registerItem("alloy_chestplate", 
            () -> new ArmorItem(ModArmorMaterialsAccess.holder(ModArmorMaterials.ALLOY), ArmorItem.Type.CHESTPLATE, ModArmorMaterialsAccess.armorProps(ModArmorMaterials.ALLOY, ArmorItem.Type.CHESTPLATE)));
    public static final RegistrySupplier<Item> ALLOY_LEGGINGS = registerItem("alloy_leggings", 
            () -> new ArmorItem(ModArmorMaterialsAccess.holder(ModArmorMaterials.ALLOY), ArmorItem.Type.LEGGINGS, ModArmorMaterialsAccess.armorProps(ModArmorMaterials.ALLOY, ArmorItem.Type.LEGGINGS)));
    public static final RegistrySupplier<Item> ALLOY_BOOTS = registerItem("alloy_boots", 
            () -> new ArmorItem(ModArmorMaterialsAccess.holder(ModArmorMaterials.ALLOY), ArmorItem.Type.BOOTS, ModArmorMaterialsAccess.armorProps(ModArmorMaterials.ALLOY, ArmorItem.Type.BOOTS)));

    public static final RegistrySupplier<Item> TITANIUM_HELMET = registerItem("titanium_helmet", 
            () -> new ArmorItem(ModArmorMaterialsAccess.holder(ModArmorMaterials.TITANIUM), ArmorItem.Type.HELMET, ModArmorMaterialsAccess.armorProps(ModArmorMaterials.TITANIUM, ArmorItem.Type.HELMET)));
    public static final RegistrySupplier<Item> TITANIUM_CHESTPLATE = registerItem("titanium_chestplate", 
            () -> new ArmorItem(ModArmorMaterialsAccess.holder(ModArmorMaterials.TITANIUM), ArmorItem.Type.CHESTPLATE, ModArmorMaterialsAccess.armorProps(ModArmorMaterials.TITANIUM, ArmorItem.Type.CHESTPLATE)));
    public static final RegistrySupplier<Item> TITANIUM_LEGGINGS = registerItem("titanium_leggings", 
            () -> new ArmorItem(ModArmorMaterialsAccess.holder(ModArmorMaterials.TITANIUM), ArmorItem.Type.LEGGINGS, ModArmorMaterialsAccess.armorProps(ModArmorMaterials.TITANIUM, ArmorItem.Type.LEGGINGS)));
    public static final RegistrySupplier<Item> TITANIUM_BOOTS = registerItem("titanium_boots", 
            () -> new ArmorItem(ModArmorMaterialsAccess.holder(ModArmorMaterials.TITANIUM), ArmorItem.Type.BOOTS, ModArmorMaterialsAccess.armorProps(ModArmorMaterials.TITANIUM, ArmorItem.Type.BOOTS)));

    public static final RegistrySupplier<Item> STEEL_HELMET = registerItem("steel_helmet", 
            () -> new ArmorItem(ModArmorMaterialsAccess.holder(ModArmorMaterials.STEEL), ArmorItem.Type.HELMET, ModArmorMaterialsAccess.armorProps(ModArmorMaterials.STEEL, ArmorItem.Type.HELMET)));
    public static final RegistrySupplier<Item> STEEL_CHESTPLATE = registerItem("steel_chestplate", WIP,
            () -> new ArmorItem(ModArmorMaterialsAccess.holder(ModArmorMaterials.TITANIUM), ArmorItem.Type.CHESTPLATE, ModArmorMaterialsAccess.armorProps(ModArmorMaterials.TITANIUM, ArmorItem.Type.CHESTPLATE)));
    public static final RegistrySupplier<Item> STEEL_LEGGINGS = registerItem("steel_leggings", 
            () -> new ArmorItem(ModArmorMaterialsAccess.holder(ModArmorMaterials.STEEL), ArmorItem.Type.LEGGINGS, ModArmorMaterialsAccess.armorProps(ModArmorMaterials.STEEL, ArmorItem.Type.LEGGINGS)));
    public static final RegistrySupplier<Item> STEEL_BOOTS = registerItem("steel_boots", 
            () -> new ArmorItem(ModArmorMaterialsAccess.holder(ModArmorMaterials.STEEL), ArmorItem.Type.BOOTS, ModArmorMaterialsAccess.armorProps(ModArmorMaterials.STEEL, ArmorItem.Type.BOOTS)));

    public static final RegistrySupplier<Item> COBALT_HELMET = registerItem("cobalt_helmet", 
            () -> new ArmorItem(ModArmorMaterialsAccess.holder(ModArmorMaterials.COBALT), ArmorItem.Type.HELMET, ModArmorMaterialsAccess.armorProps(ModArmorMaterials.COBALT, ArmorItem.Type.HELMET)));
    public static final RegistrySupplier<Item> COBALT_CHESTPLATE = registerItem("cobalt_chestplate", 
            () -> new ArmorItem(ModArmorMaterialsAccess.holder(ModArmorMaterials.COBALT), ArmorItem.Type.CHESTPLATE, ModArmorMaterialsAccess.armorProps(ModArmorMaterials.COBALT, ArmorItem.Type.CHESTPLATE)));
    public static final RegistrySupplier<Item> COBALT_LEGGINGS = registerItem("cobalt_leggings", 
            () -> new ArmorItem(ModArmorMaterialsAccess.holder(ModArmorMaterials.COBALT), ArmorItem.Type.LEGGINGS, ModArmorMaterialsAccess.armorProps(ModArmorMaterials.COBALT, ArmorItem.Type.LEGGINGS)));
    public static final RegistrySupplier<Item> COBALT_BOOTS = registerItem("cobalt_boots", 
            () -> new ArmorItem(ModArmorMaterialsAccess.holder(ModArmorMaterials.COBALT), ArmorItem.Type.BOOTS, ModArmorMaterialsAccess.armorProps(ModArmorMaterials.COBALT, ArmorItem.Type.BOOTS)));

    public static final RegistrySupplier<Item> SECURITY_HELMET = registerItem("security_helmet", 
            () -> new ArmorItem(ModArmorMaterialsAccess.holder(ModArmorMaterials.SECURITY), ArmorItem.Type.HELMET, ModArmorMaterialsAccess.armorProps(ModArmorMaterials.SECURITY, ArmorItem.Type.HELMET)));
    public static final RegistrySupplier<Item> SECURITY_CHESTPLATE = registerItem("security_chestplate", 
            () -> new ArmorItem(ModArmorMaterialsAccess.holder(ModArmorMaterials.SECURITY), ArmorItem.Type.CHESTPLATE, ModArmorMaterialsAccess.armorProps(ModArmorMaterials.SECURITY, ArmorItem.Type.CHESTPLATE)));
    public static final RegistrySupplier<Item> SECURITY_LEGGINGS = registerItem("security_leggings", 
            () -> new ArmorItem(ModArmorMaterialsAccess.holder(ModArmorMaterials.SECURITY), ArmorItem.Type.LEGGINGS, ModArmorMaterialsAccess.armorProps(ModArmorMaterials.SECURITY, ArmorItem.Type.LEGGINGS)));
    public static final RegistrySupplier<Item> SECURITY_BOOTS = registerItem("security_boots", 
            () -> new ArmorItem(ModArmorMaterialsAccess.holder(ModArmorMaterials.SECURITY), ArmorItem.Type.BOOTS, ModArmorMaterialsAccess.armorProps(ModArmorMaterials.SECURITY, ArmorItem.Type.BOOTS)));

    public static final RegistrySupplier<Item> ASBESTOS_HELMET = registerItem("asbestos_helmet", 
            () -> new ArmorItem(ModArmorMaterialsAccess.holder(ModArmorMaterials.ASBESTOS), ArmorItem.Type.HELMET, ModArmorMaterialsAccess.armorProps(ModArmorMaterials.ASBESTOS, ArmorItem.Type.HELMET)));
    public static final RegistrySupplier<Item> ASBESTOS_CHESTPLATE = registerItem("asbestos_chestplate", 
            () -> new ArmorItem(ModArmorMaterialsAccess.holder(ModArmorMaterials.ASBESTOS), ArmorItem.Type.CHESTPLATE, ModArmorMaterialsAccess.armorProps(ModArmorMaterials.ASBESTOS, ArmorItem.Type.CHESTPLATE)));
    public static final RegistrySupplier<Item> ASBESTOS_LEGGINGS = registerItem("asbestos_leggings", 
            () -> new ArmorItem(ModArmorMaterialsAccess.holder(ModArmorMaterials.ASBESTOS), ArmorItem.Type.LEGGINGS, ModArmorMaterialsAccess.armorProps(ModArmorMaterials.ASBESTOS, ArmorItem.Type.LEGGINGS)));
    public static final RegistrySupplier<Item> ASBESTOS_BOOTS = registerItem("asbestos_boots", 
            () -> new ArmorItem(ModArmorMaterialsAccess.holder(ModArmorMaterials.ASBESTOS), ArmorItem.Type.BOOTS, ModArmorMaterialsAccess.armorProps(ModArmorMaterials.ASBESTOS, ArmorItem.Type.BOOTS)));

    public static final RegistrySupplier<Item> HAZMAT_HELMET = registerItem("hazmat_helmet",
            // В оригинале жёлтый шлем тоже ArmorHazmatMask (IGasMask, фильтр ставится),
            // просто без отдельной модели маски - порт ArmorHazmat (1.7.10).
            () -> new HazmatMaskArmorItem(ModArmorMaterials.HAZMAT, ModArmorMaterialsAccess.armorProps(ModArmorMaterials.HAZMAT, ArmorItem.Type.HELMET), HazmatArmorItem.Variant.YELLOW));
    public static final RegistrySupplier<Item> HAZMAT_CHESTPLATE = registerItem("hazmat_chestplate",
            () -> new HazmatArmorItem(ModArmorMaterials.HAZMAT, ArmorItem.Type.CHESTPLATE, ModArmorMaterialsAccess.armorProps(ModArmorMaterials.HAZMAT, ArmorItem.Type.CHESTPLATE), HazmatArmorItem.Variant.YELLOW));
    public static final RegistrySupplier<Item> HAZMAT_LEGGINGS = registerItem("hazmat_leggings",
            () -> new HazmatArmorItem(ModArmorMaterials.HAZMAT, ArmorItem.Type.LEGGINGS, ModArmorMaterialsAccess.armorProps(ModArmorMaterials.HAZMAT, ArmorItem.Type.LEGGINGS), HazmatArmorItem.Variant.YELLOW));
    public static final RegistrySupplier<Item> HAZMAT_BOOTS = registerItem("hazmat_boots",
            () -> new HazmatArmorItem(ModArmorMaterials.HAZMAT, ArmorItem.Type.BOOTS, ModArmorMaterialsAccess.armorProps(ModArmorMaterials.HAZMAT, ArmorItem.Type.BOOTS), HazmatArmorItem.Variant.YELLOW));

    public static final RegistrySupplier<Item> LIQUIDATOR_HELMET = registerItem("liquidator_helmet", 
            () -> new ArmorItem(ModArmorMaterialsAccess.holder(ModArmorMaterials.LIQUIDATOR), ArmorItem.Type.HELMET, ModArmorMaterialsAccess.armorProps(ModArmorMaterials.LIQUIDATOR, ArmorItem.Type.HELMET)));
    public static final RegistrySupplier<Item> LIQUIDATOR_CHESTPLATE = registerItem("liquidator_chestplate", 
            () -> new ArmorItem(ModArmorMaterialsAccess.holder(ModArmorMaterials.LIQUIDATOR), ArmorItem.Type.CHESTPLATE, ModArmorMaterialsAccess.armorProps(ModArmorMaterials.LIQUIDATOR, ArmorItem.Type.CHESTPLATE)));
    public static final RegistrySupplier<Item> LIQUIDATOR_LEGGINGS = registerItem("liquidator_leggings", 
            () -> new ArmorItem(ModArmorMaterialsAccess.holder(ModArmorMaterials.LIQUIDATOR), ArmorItem.Type.LEGGINGS, ModArmorMaterialsAccess.armorProps(ModArmorMaterials.LIQUIDATOR, ArmorItem.Type.LEGGINGS)));
    public static final RegistrySupplier<Item> LIQUIDATOR_BOOTS = registerItem("liquidator_boots", 
            () -> new ArmorItem(ModArmorMaterialsAccess.holder(ModArmorMaterials.LIQUIDATOR), ArmorItem.Type.BOOTS, ModArmorMaterialsAccess.armorProps(ModArmorMaterials.LIQUIDATOR, ArmorItem.Type.BOOTS)));

    public static final RegistrySupplier<Item> PAA_HELMET = registerItem("paa_helmet", 
            () -> new ArmorItem(ModArmorMaterialsAccess.holder(ModArmorMaterials.PAA), ArmorItem.Type.HELMET, ModArmorMaterialsAccess.armorProps(ModArmorMaterials.PAA, ArmorItem.Type.HELMET)));
    public static final RegistrySupplier<Item> PAA_CHESTPLATE = registerItem("paa_chestplate", 
            () -> new ArmorItem(ModArmorMaterialsAccess.holder(ModArmorMaterials.PAA), ArmorItem.Type.CHESTPLATE, ModArmorMaterialsAccess.armorProps(ModArmorMaterials.PAA, ArmorItem.Type.CHESTPLATE)));
    public static final RegistrySupplier<Item> PAA_LEGGINGS = registerItem("paa_leggings", 
            () -> new ArmorItem(ModArmorMaterialsAccess.holder(ModArmorMaterials.PAA), ArmorItem.Type.LEGGINGS, ModArmorMaterialsAccess.armorProps(ModArmorMaterials.PAA, ArmorItem.Type.LEGGINGS)));
    public static final RegistrySupplier<Item> PAA_BOOTS = registerItem("paa_boots", 
            () -> new ArmorItem(ModArmorMaterialsAccess.holder(ModArmorMaterials.PAA), ArmorItem.Type.BOOTS, ModArmorMaterialsAccess.armorProps(ModArmorMaterials.PAA, ArmorItem.Type.BOOTS)));

    public static final RegistrySupplier<Item> STARMETAL_HELMET = registerItem("starmetal_helmet", 
            () -> new ArmorItem(ModArmorMaterialsAccess.holder(ModArmorMaterials.STARMETAL), ArmorItem.Type.HELMET, ModArmorMaterialsAccess.armorProps(ModArmorMaterials.STARMETAL, ArmorItem.Type.HELMET)));
    public static final RegistrySupplier<Item> STARMETAL_CHESTPLATE = registerItem("starmetal_chestplate", 
            () -> new ArmorItem(ModArmorMaterialsAccess.holder(ModArmorMaterials.STARMETAL), ArmorItem.Type.CHESTPLATE, ModArmorMaterialsAccess.armorProps(ModArmorMaterials.STARMETAL, ArmorItem.Type.CHESTPLATE)));
    public static final RegistrySupplier<Item> STARMETAL_LEGGINGS = registerItem("starmetal_leggings", 
            () -> new ArmorItem(ModArmorMaterialsAccess.holder(ModArmorMaterials.STARMETAL), ArmorItem.Type.LEGGINGS, ModArmorMaterialsAccess.armorProps(ModArmorMaterials.STARMETAL, ArmorItem.Type.LEGGINGS)));
    public static final RegistrySupplier<Item> STARMETAL_BOOTS = registerItem("starmetal_boots", 
            () -> new ArmorItem(ModArmorMaterialsAccess.holder(ModArmorMaterials.STARMETAL), ArmorItem.Type.BOOTS, ModArmorMaterialsAccess.armorProps(ModArmorMaterials.STARMETAL, ArmorItem.Type.BOOTS)));


    //-----------------------POWER ARMOR ----------------------------------

    public static final RegistrySupplier<Item> T51_HELMET = registerItem("t51_helmet", 
            () -> new T51Armor(ModArmorMaterials.TITANIUM, ArmorItem.Type.HELMET, new Item.Properties()));

    public static final RegistrySupplier<Item> T51_CHESTPLATE = registerItem("t51_chestplate", 
            () -> new T51Armor(ModArmorMaterials.TITANIUM, ArmorItem.Type.CHESTPLATE, new Item.Properties()));

    public static final RegistrySupplier<Item> T51_LEGGINGS = registerItem("t51_leggings", 
            () -> new T51Armor(ModArmorMaterials.TITANIUM, ArmorItem.Type.LEGGINGS, new Item.Properties()));

    public static final RegistrySupplier<Item> T51_BOOTS = registerItem("t51_boots", 
            () -> new T51Armor(ModArmorMaterials.TITANIUM, ArmorItem.Type.BOOTS, new Item.Properties()));

    public static final RegistrySupplier<Item> AJR_HELMET = registerItem("ajr_helmet", 
            () -> new AJRArmor(ModArmorMaterials.AJR, ArmorItem.Type.HELMET, new Item.Properties()));

    public static final RegistrySupplier<Item> AJR_CHESTPLATE = registerItem("ajr_chestplate", 
            () -> new AJRArmor(ModArmorMaterials.AJR, ArmorItem.Type.CHESTPLATE, new Item.Properties()));

    public static final RegistrySupplier<Item> AJR_LEGGINGS = registerItem("ajr_leggings", 
            () -> new AJRArmor(ModArmorMaterials.AJR, ArmorItem.Type.LEGGINGS, new Item.Properties()));

    public static final RegistrySupplier<Item> AJR_BOOTS = registerItem("ajr_boots", 
            () -> new AJRArmor(ModArmorMaterials.AJR, ArmorItem.Type.BOOTS, new Item.Properties()));

	public static final RegistrySupplier<Item> AJRO_HELMET = registerItem("ajro_helmet", 
            () -> new AJROArmor(ModArmorMaterials.AJR, ArmorItem.Type.HELMET, new Item.Properties()));

    public static final RegistrySupplier<Item> AJRO_CHESTPLATE = registerItem("ajro_chestplate", 
            () -> new AJROArmor(ModArmorMaterials.AJR, ArmorItem.Type.CHESTPLATE, new Item.Properties()));

    public static final RegistrySupplier<Item> AJRO_LEGGINGS = registerItem("ajro_leggings", 
            () -> new AJROArmor(ModArmorMaterials.AJR, ArmorItem.Type.LEGGINGS, new Item.Properties()));
			
    public static final RegistrySupplier<Item> AJRO_BOOTS = registerItem("ajro_boots", 
            () -> new AJROArmor(ModArmorMaterials.AJR, ArmorItem.Type.BOOTS, new Item.Properties()));

    public static final RegistrySupplier<Item> DNT_HELMET = registerItem("dnt_helmet", 
            () -> new DNTArmor(ModArmorMaterials.STARMETAL, ArmorItem.Type.HELMET, new Item.Properties()));

    public static final RegistrySupplier<Item> DNT_CHESTPLATE = registerItem("dnt_chestplate", 
            () -> new DNTArmor(ModArmorMaterials.STARMETAL, ArmorItem.Type.CHESTPLATE, new Item.Properties()));

    public static final RegistrySupplier<Item> DNT_LEGGINGS = registerItem("dnt_leggings", 
            () -> new DNTArmor(ModArmorMaterials.STARMETAL, ArmorItem.Type.LEGGINGS, new Item.Properties()));

    public static final RegistrySupplier<Item> DNT_BOOTS = registerItem("dnt_boots", 
            () -> new DNTArmor(ModArmorMaterials.STARMETAL, ArmorItem.Type.BOOTS, new Item.Properties()));

    public static final RegistrySupplier<Item> BISMUTH_HELMET = registerItem("bismuth_helmet", 
            () -> new BismuthArmor(ModArmorMaterials.BISMUTH, ArmorItem.Type.HELMET, new Item.Properties()));

    public static final RegistrySupplier<Item> BISMUTH_CHESTPLATE = registerItem("bismuth_chestplate", 
            () -> new BismuthArmor(ModArmorMaterials.BISMUTH, ArmorItem.Type.CHESTPLATE, new Item.Properties()));

    public static final RegistrySupplier<Item> BISMUTH_LEGGINGS = registerItem("bismuth_leggings", 
            () -> new BismuthArmor(ModArmorMaterials.BISMUTH, ArmorItem.Type.LEGGINGS, new Item.Properties()));

    public static final RegistrySupplier<Item> BISMUTH_BOOTS = registerItem("bismuth_boots", 
            () -> new BismuthArmor(ModArmorMaterials.BISMUTH, ArmorItem.Type.BOOTS, new Item.Properties()));

    public static final RegistrySupplier<Item> GEIGER_COUNTER = registerItem("geiger_counter", 
            () -> new ItemGeigerCounter(new Item.Properties().stacksTo(1)));

    public static final RegistrySupplier<Item> DOSIMETER = registerItem("dosimeter", 
            () -> new ItemDosimeter(new Item.Properties().stacksTo(1)));

    public static final RegistrySupplier<Item> DIGAMMA_DIAGNOSTIC = registerItem("digamma_diagnostic", 
            () -> new ItemDigammaDiagnostic(new Item.Properties()));

     public static final RegistrySupplier<Item> MUSIC_DISC_BUNKER = registerItem("music_disc_bunker", 
            () -> PlatformHooks.createRecordItem(
                    1,
                    ModSounds.MUSIC_DISC_BUNKER.get(),
                    new Item.Properties().stacksTo(1).rarity(Rarity.RARE),
                    120,
                    "bunker"
            ));

    public static final RegistrySupplier<Item> MUSIC_DISC_GLASS = registerItem("music_disc_glass", 
            () -> com.hbm_m.platform.PlatformHooks.createRecordItem(
                    2,
                    ModSounds.MUSIC_DISC_GLASS.get(),
                    new Item.Properties().stacksTo(1).rarity(Rarity.RARE),
                    62,
                    "glass"
            ));

    /**
     * Schweizerpsalm - the Swiss national anthem. Length is the source file's 1:24 rounded up; the
     * comparator value continues the mod's own sequence (bunker 1, glass 2).
     */
    public static final RegistrySupplier<Item> MUSIC_DISC_CH = registerItem("music_disc_ch", 
            () -> new FlavouredRecordItem(
                    3,
                    ModSounds.MUSIC_DISC_CH.get(),
                    PlatformHooks.jukeboxProperties(new Item.Properties().stacksTo(1).rarity(Rarity.RARE), "ch"),
                    20 * 85,
                    "item.hbm_m.music_disc_ch.flavour"
            ));




    public static final RegistrySupplier<Item> CRATE_IRON = registerItem("crate_iron", 
            () -> new CrateItem(ModBlocks.CRATE_IRON.get(), new Item.Properties(), CrateType.IRON.getSlotCount()));
    public static final RegistrySupplier<Item> CRATE_STEEL = registerItem("crate_steel", 
            () -> new CrateItem(ModBlocks.CRATE_STEEL.get(), new Item.Properties(), CrateType.STEEL.getSlotCount()));
    public static final RegistrySupplier<Item> CRATE_DESH = registerItem("crate_desh", 
            () -> new CrateItem(ModBlocks.CRATE_DESH.get(), new Item.Properties(), CrateType.DESH.getSlotCount()));
    public static final RegistrySupplier<Item> CRATE_TUNGSTEN = registerItem("crate_tungsten", 
            () -> new CrateItem(ModBlocks.CRATE_TUNGSTEN.get(), new Item.Properties(), CrateType.TUNGSTEN.getSlotCount()));
    public static final RegistrySupplier<Item> CRATE_TEMPLATE = registerItem("crate_template", 
            () -> new CrateItem(ModBlocks.CRATE_TEMPLATE.get(), new Item.Properties(), CrateType.TEMPLATE.getSlotCount()));

    public static final RegistrySupplier<Item> HEART_PIECE = registerItem("heart_piece", 
            () -> new ItemModHealth(
                    new Item.Properties(),
                    SLOT_SPECIAL,
                    5.0
            )
    );
    public static final RegistrySupplier<Item> HEART_CONTAINER = registerItem("heart_container", 
            () -> new ItemModHealth(
                    new Item.Properties(),
                    SLOT_SPECIAL,
                    20.0
            )
    );
    public static final RegistrySupplier<Item> HEART_BOOSTER = registerItem("heart_booster", 
            () -> new ItemModHealth(
                    new Item.Properties(),
                    SLOT_SPECIAL,
                    40.0
            )
    );
    public static final RegistrySupplier<Item> HEART_FAB = registerItem("heart_fab", 
            () -> new ItemModHealth(
                    new Item.Properties(),
                    SLOT_SPECIAL,
                    60.0
            )
    );
    public static final RegistrySupplier<Item> BLACK_DIAMOND = registerItem("black_diamond", 
            () -> new ItemModHealth(
                    new Item.Properties(),
                    SLOT_SPECIAL,
                    40.0
            )
    );

    public static final RegistrySupplier<Item> GHIORSIUM_CLADDING = registerItem("cladding_ghiorsium", 
            () -> new ItemModRadProtection(
                    new Item.Properties(),
                    SLOT_CLADDING,
                    0.5f
            )
    );
    public static final RegistrySupplier<Item> DESH_CLADDING = registerItem("cladding_desh", 
            () -> new ItemModRadProtection(
                    new Item.Properties(),
                    SLOT_CLADDING,
                    0.2f
            )
    );
    public static final RegistrySupplier<Item> LEAD_CLADDING = registerItem("cladding_lead", 
            () -> new ItemModRadProtection(
                    new Item.Properties(),
                    SLOT_CLADDING,
                    0.1f
            )
    );
    public static final RegistrySupplier<Item> RUBBER_CLADDING = registerItem("cladding_rubber", 
            () -> new ItemModRadProtection(
                    new Item.Properties(),
                    SLOT_CLADDING,
                    0.005f
            )
    );
    public static final RegistrySupplier<Item> PAINT_CLADDING = registerItem("cladding_paint", 
            () -> new ItemModRadProtection(
                    new Item.Properties(),
                    SLOT_CLADDING,
                    0.025f
            )
    );

    // РќРѕРІС‹Рµ РјРѕРґРёС„РёРєР°С†РёРё Р±СЂРѕРЅРё
//     public static final RegistrySupplier<Item> ARMOR_MOD_SERVOS = ITEMS.register("armor_mod_servos",
//             () -> new ItemModServos(new Item.Properties())
//     );

//     public static final RegistrySupplier<Item> ARMOR_MOD_CLADDING = ITEMS.register("armor_mod_cladding",
//             () -> new ItemModCladding(new Item.Properties())
//     );

//     public static final RegistrySupplier<Item> ARMOR_MOD_KEVLAR = ITEMS.register("armor_mod_kevlar",
//             () -> new ItemModKevlar(new Item.Properties())
//     );

//     public static final RegistrySupplier<Item> ARMOR_MOD_EXTRA = ITEMS.register("armor_mod_extra",
//             () -> new ItemModExtra(new Item.Properties())
//     );

    // РњРѕРґРёС„РёРєР°С‚РѕСЂС‹ Р±Р°С‚Р°СЂРµРё (СѓРІРµР»РёС‡РёРІР°СЋС‚ Р·Р°СЂСЏРґ Р±СЂРѕРЅРё)
    public static final RegistrySupplier<Item> ARMOR_BATTERY = registerItem("armor_battery", 
            () -> new ItemModBattery(1.25D)
    );

    public static final RegistrySupplier<Item> ARMOR_BATTERY_MK2 = registerItem("armor_battery_mk2", 
            () -> new ItemModBattery(1.5D)
    );

    public static final RegistrySupplier<Item> ARMOR_BATTERY_MK3 = registerItem("armor_battery_mk3", 
            () -> new ItemModBattery(2D)
    );
    public static final RegistrySupplier<Item> CREATIVE_BATTERY = registerItem("battery_creative", 
            () -> new ItemCreativeBattery(
                    new Item.Properties()
            )
    );
    public static final RegistrySupplier<Item> ASSEMBLY_TEMPLATE = registerItem("assembly_template", 
            () -> new ItemAssemblyTemplate(
                    new Item.Properties().stacksTo(1)
            )
    );
    public static final RegistrySupplier<Item> TEMPLATE_FOLDER = registerItem("template_folder", 
            () -> new ItemTemplateFolder(
                    new Item.Properties().stacksTo(1)
            )
    );
    public static final RegistrySupplier<Item> BLUEPRINT_FOLDER = registerItem("blueprint_folder", 
        () -> new ItemBlueprintFolder(
                new Item.Properties().stacksTo(1)
        )
    );

    // в•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђ MACHINE UPGRADES в•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђ

    public static final RegistrySupplier<Item> UPGRADE_SPEED_1 = registerItem("upgrade_speed_1", 
            () -> new ItemMachineUpgrade(new Item.Properties(), ItemMachineUpgrade.UpgradeType.SPEED, 1));
    public static final RegistrySupplier<Item> UPGRADE_SPEED_2 = registerItem("upgrade_speed_2", 
            () -> new ItemMachineUpgrade(new Item.Properties(), ItemMachineUpgrade.UpgradeType.SPEED, 2));
    public static final RegistrySupplier<Item> UPGRADE_SPEED_3 = registerItem("upgrade_speed_3", 
            () -> new ItemMachineUpgrade(new Item.Properties(), ItemMachineUpgrade.UpgradeType.SPEED, 3));
    public static final RegistrySupplier<Item> UPGRADE_STACK_1 = registerItem("upgrade_stack_1", 
            () -> new ItemMachineUpgrade(new Item.Properties(), ItemMachineUpgrade.UpgradeType.STACK, 1));
    public static final RegistrySupplier<Item> UPGRADE_STACK_2 = registerItem("upgrade_stack_2", 
            () -> new ItemMachineUpgrade(new Item.Properties(), ItemMachineUpgrade.UpgradeType.STACK, 2));
    public static final RegistrySupplier<Item> UPGRADE_STACK_3 = registerItem("upgrade_stack_3", 
            () -> new ItemMachineUpgrade(new Item.Properties(), ItemMachineUpgrade.UpgradeType.STACK, 3));
    public static final RegistrySupplier<Item> UPGRADE_EJECTOR_1 = registerItem("upgrade_ejector_1", 
            () -> new ItemMachineUpgrade(new Item.Properties(), ItemMachineUpgrade.UpgradeType.EJECTOR, 1));
    public static final RegistrySupplier<Item> UPGRADE_EJECTOR_2 = registerItem("upgrade_ejector_2", 
            () -> new ItemMachineUpgrade(new Item.Properties(), ItemMachineUpgrade.UpgradeType.EJECTOR, 2));
    public static final RegistrySupplier<Item> UPGRADE_EJECTOR_3 = registerItem("upgrade_ejector_3", 
            () -> new ItemMachineUpgrade(new Item.Properties(), ItemMachineUpgrade.UpgradeType.EJECTOR, 3));

    public static final RegistrySupplier<Item> UPGRADE_EFFECT_1 = registerItem("upgrade_effect_1", 
            () -> new ItemMachineUpgrade(new Item.Properties(), ItemMachineUpgrade.UpgradeType.EFFECT, 1));
    public static final RegistrySupplier<Item> UPGRADE_EFFECT_2 = registerItem("upgrade_effect_2", 
            () -> new ItemMachineUpgrade(new Item.Properties(), ItemMachineUpgrade.UpgradeType.EFFECT, 2));
    public static final RegistrySupplier<Item> UPGRADE_EFFECT_3 = registerItem("upgrade_effect_3", 
            () -> new ItemMachineUpgrade(new Item.Properties(), ItemMachineUpgrade.UpgradeType.EFFECT, 3));

    public static final RegistrySupplier<Item> UPGRADE_POWER_1 = registerItem("upgrade_power_1", 
            () -> new ItemMachineUpgrade(new Item.Properties(), ItemMachineUpgrade.UpgradeType.POWER, 1));
    public static final RegistrySupplier<Item> UPGRADE_POWER_2 = registerItem("upgrade_power_2", 
            () -> new ItemMachineUpgrade(new Item.Properties(), ItemMachineUpgrade.UpgradeType.POWER, 2));
    public static final RegistrySupplier<Item> UPGRADE_POWER_3 = registerItem("upgrade_power_3", 
            () -> new ItemMachineUpgrade(new Item.Properties(), ItemMachineUpgrade.UpgradeType.POWER, 3));

    public static final RegistrySupplier<Item> UPGRADE_FORTUNE_1 = registerItem("upgrade_fortune_1", 
            () -> new ItemMachineUpgrade(new Item.Properties(), ItemMachineUpgrade.UpgradeType.FORTUNE, 1));
    public static final RegistrySupplier<Item> UPGRADE_FORTUNE_2 = registerItem("upgrade_fortune_2", 
            () -> new ItemMachineUpgrade(new Item.Properties(), ItemMachineUpgrade.UpgradeType.FORTUNE, 2));
    public static final RegistrySupplier<Item> UPGRADE_FORTUNE_3 = registerItem("upgrade_fortune_3", 
            () -> new ItemMachineUpgrade(new Item.Properties(), ItemMachineUpgrade.UpgradeType.FORTUNE, 3));

    public static final RegistrySupplier<Item> UPGRADE_AFTERBURN_1 = registerItem("upgrade_afterburn_1", 
            () -> new ItemMachineUpgrade(new Item.Properties(), ItemMachineUpgrade.UpgradeType.AFTERBURN, 1));
    public static final RegistrySupplier<Item> UPGRADE_AFTERBURN_2 = registerItem("upgrade_afterburn_2", 
            () -> new ItemMachineUpgrade(new Item.Properties(), ItemMachineUpgrade.UpgradeType.AFTERBURN, 2));
    public static final RegistrySupplier<Item> UPGRADE_AFTERBURN_3 = registerItem("upgrade_afterburn_3", 
            () -> new ItemMachineUpgrade(new Item.Properties(), ItemMachineUpgrade.UpgradeType.AFTERBURN, 3));

    public static final RegistrySupplier<Item> UPGRADE_OVERDRIVE_1 = registerItem("upgrade_overdrive_1", 
            () -> new ItemMachineUpgrade(new Item.Properties(), ItemMachineUpgrade.UpgradeType.OVERDRIVE, 1));
    public static final RegistrySupplier<Item> UPGRADE_OVERDRIVE_2 = registerItem("upgrade_overdrive_2", 
            () -> new ItemMachineUpgrade(new Item.Properties(), ItemMachineUpgrade.UpgradeType.OVERDRIVE, 2));
    public static final RegistrySupplier<Item> UPGRADE_OVERDRIVE_3 = registerItem("upgrade_overdrive_3", 
            () -> new ItemMachineUpgrade(new Item.Properties(), ItemMachineUpgrade.UpgradeType.OVERDRIVE, 3));

    // Maxwell accepts the full upgrade range in the original (TileEntityTurretMaxwell's
    // getAmmoTypesForDisplay lists seventeen); these last two were never registered here, so the
    // turret was missing two of its ammo types outright. Textures were already in the repo.
    // Kraftfeldaufwertungen. Original: eigene Eintraege mit Stapelgroesse 16, keine Stufen -
    // die Wirkung haengt allein daran, wie viele im Slot liegen.
    public static final RegistrySupplier<Item> UPGRADE_RADIUS = registerItem("upgrade_radius", 
            () -> new ItemMachineUpgrade(new Item.Properties(),
                    ItemMachineUpgrade.UpgradeType.RADIUS, 1, 16));
    public static final RegistrySupplier<Item> UPGRADE_HEALTH = registerItem("upgrade_health", 
            () -> new ItemMachineUpgrade(new Item.Properties(),
                    ItemMachineUpgrade.UpgradeType.HEALTH, 1, 16));

    public static final RegistrySupplier<Item> UPGRADE_5G = registerItem("upgrade_5g", 
            () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> UPGRADE_SCREM = registerItem("upgrade_screm", 
            () -> new Item(new Item.Properties()));

    // ═══════════════════ END MACHINE UPGRADES ═══════════════════
    // в•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђ END MACHINE UPGRADES в•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђ

    public static final RegistrySupplier<Item> RADAWAY = registerItem("radaway", 
            () -> new ItemSimpleConsumable(new Item.Properties(), (player, stack) -> {
                // Р­С‚Рѕ Р»СЏРјР±РґР°-РІС‹СЂР°Р¶РµРЅРёРµ РѕРїСЂРµРґРµР»СЏРµС‚, С‡С‚Рѕ РїСЂРѕРёР·РѕР№РґРµС‚ РїСЂРё РёСЃРїРѕР»СЊР·РѕРІР°РЅРёРё РїСЂРµРґРјРµС‚Р°.
                
                // Р”РµР№СЃС‚РІСѓРµРј С‚РѕР»СЊРєРѕ РЅР° СЃРµСЂРІРµСЂРµ
                if (!player.level().isClientSide()) {
                    // 1. РќР°РєР»Р°РґС‹РІР°РµРј СЌС„С„РµРєС‚ РђРЅС‚РёСЂР°РґРёРЅР°.
                    //    Р”Р»РёС‚РµР»СЊРЅРѕСЃС‚СЊ: 200 С‚РёРєРѕРІ (10 СЃРµРєСѓРЅРґ)
                    //    РЈСЂРѕРІРµРЅСЊ: I (amplifier = 0)
                    com.hbm_m.platform.PlatformHooks.addEffect(player, ModEffects.RADAWAY, 120, 0);

                    // 2. РџСЂРѕРёРіСЂС‹РІР°РµРј Р·РІСѓРє
                    player.level().playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.RADAWAY_USE.get(), player.getSoundSource(), 1.0F, 1.0F);

                    // 3. РЈРјРµРЅСЊС€Р°РµРј РєРѕР»РёС‡РµСЃС‚РІРѕ РїСЂРµРґРјРµС‚РѕРІ РІ СЃС‚Р°РєРµ
                    if (!player.getAbilities().instabuild) { // РЅРµ СѓРјРµРЅСЊС€Р°С‚СЊ РІ РєСЂРµР°С‚РёРІРµ
                        stack.shrink(1);
                    }
                }
            })
    );
    public static final RegistrySupplier<Item> OIL_DETECTOR = registerItem("oil_detector", 
            () -> new OilDetectorItem(new Item.Properties()));

    public static final RegistrySupplier<Item> DEPTH_ORES_SCANNER = registerItem("depth_ores_scanner", 
            () -> new DepthOresScannerItem(new Item.Properties()));

    public static final RegistrySupplier<Item> RANGEFINDER = registerItem("rangefinder", 
            () -> new RangefinderItem(new Item.Properties()));

    public static final RegistrySupplier<Item> CONFETTI_TESTER = registerItem("confetti_tester", 
            ConfettiTesterItem::new);

    public static final RegistrySupplier<Item> RANGE_DETONATOR = registerItem("range_detonator", 
            () -> new RangeDetonatorItem(new Item.Properties()));

    public static final RegistrySupplier<Item> MULTI_DETONATOR = registerItem("multi_detonator", 
            () -> new MultiDetonatorItem(new Item.Properties()));

    public static final RegistrySupplier<Item> DETONATOR = registerItem("detonator", 
            () -> new DetonatorItem(new Item.Properties()));

    public static final RegistrySupplier<Item> BALL_TNT = registerItem("ball_tnt", 
            () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> FAT_MAN_EXPLOSIVE = registerItem("fat_man_explosive", 
            () -> new LoreTooltipItem(List.of(
                    Component.translatable("tooltip.hbm_m.fat_man_explosive.desc1").withStyle(ChatFormatting.GRAY),
                    Component.translatable("tooltip.hbm_m.fat_man_explosive.desc2").withStyle(ChatFormatting.GRAY)),
                    new Item.Properties()));
    public static final RegistrySupplier<Item> FAT_MAN_IGNITER = registerItem("fat_man_igniter", 
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> IGNITER = registerItem("igniter", 
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> FAT_MAN_CORE = registerItem("fat_man_core", 
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> CELL_SAS3 = registerItem("cell_sas3", 
            () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> ROD_QUAD_LEAD = registerItem("rod_quad_lead", 
            () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> ROD_QUAD_NP237 = registerItem("rod_quad_np237", 
            () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> ROD_QUAD_URANIUM = registerItem("rod_quad_uranium", 
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> CROWBAR = registerItem("crowbar", 
            () -> new LoreTooltipItem(List.of(
                    Component.translatable("tooltip.hbm_m.crowbar.line1").withStyle(ChatFormatting.GRAY),
                    Component.translatable("tooltip.hbm_m.crowbar.line2").withStyle(ChatFormatting.GRAY)),
                    new Item.Properties()));


    public static final RegistrySupplier<Item> MALACHITE_CHUNK = registerItem("malachite_chunk", 
            () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> LIMESTONE = registerItem("limestone", 
            () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> SHELL_STEEL = registerItem("shell_steel", 
            () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> SHELL_COPPER = registerItem("shell_copper", 
            () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> SHELL_ALUMINUM = registerItem("shell_aluminum", 
            () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> SHELL_TITANIUM = registerItem("shell_titanium", 
            () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> CAN_KEY = registerItem("can_key", 
            () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> DEFUSER = registerItem("defuser", 
            () -> new LoreTooltipItem(List.of(
                    Component.translatable("tooltip.hbm_m.defuser.line1").withStyle(ChatFormatting.GRAY)),
                    new Item.Properties()));

    public static final RegistrySupplier<Item> GAS_EMPTY = registerItem("gas_empty", 
            () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> DUCTTAPE = registerItem("ducttape", 
            () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> HAZMAT_CLOTH = registerItem("hazmat_cloth", 
            () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> HAZMAT_CLOTH_GREY = registerItem("hazmat_cloth_grey", 
            () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> HAZMAT_CLOTH_RED = registerItem("hazmat_cloth_red", 
            () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> ASBESTOS_CLOTH = registerItem("asbestos_cloth", 
            () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BOLT_STEEL = registerItem("bolt_steel", 
            () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BOLT_LEAD = registerItem("bolt_lead", 
            () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BOLT_TUNGSTEN = registerItem("bolt_tungsten", 
            () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BOLT_HIGHSPEED_STEEL = registerItem("bolt_highspeed_steel", 
            () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> ZIRCONIUM_SHARP = registerItem("zirconium_sharp", 
            () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> COIL_TUNGSTEN = registerItem("coil_tungsten", 
            () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> COIL_GOLD_TORUS = registerItem("coil_gold_torus", 
            () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> COIL_GOLD = registerItem("coil_gold", 
            () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> COIL_MAGNETIZED_TUNGSTEN_TORUS = registerItem("coil_magnetized_tungsten_torus", 
            () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> COIL_MAGNETIZED_TUNGSTEN = registerItem("coil_magnetized_tungsten", 
            () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> COIL_COPPER_TORUS = registerItem("coil_copper_torus", 
            () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> COIL_COPPER = registerItem("coil_copper", 
            () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> COIL_ADVANCED_ALLOY_TORUS = registerItem("coil_advanced_alloy_torus", 
            () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> COIL_ADVANCED_ALLOY = registerItem("coil_advanced_alloy", 
            () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> MOTOR_DESH = registerItem("motor_desh", 
            () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> MOTOR_BISMUTH = registerItem("motor_bismuth", 
            () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> MOTOR = registerItem("motor", 
            () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BORAX = registerItem("borax", 
            () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> DUST = registerItem("dust", 
            () -> new FuelItem(List.of(
                    Component.translatable("tooltip.hbm_m.dust.desc1").withStyle(ChatFormatting.GRAY)),
                    new Item.Properties(), 25));
    public static final RegistrySupplier<Item> DUST_TINY = registerItem("dust_tiny", 
            () -> new Item(new Item.Properties()));
    /** 1.7.10 ModItems.fallout вЂ” РєСѓС‡РєР° РѕСЃР°РґРєРѕРІ. */
    public static final RegistrySupplier<Item> FALLOUT = registerItem("fallout", 
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> NUCLEAR_WASTE_TINY = registerItem("nuclear_waste_tiny", 
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> NUCLEAR_WASTE_LONG_TINY = registerItem("nuclear_waste_long_tiny", 
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> NUCLEAR_WASTE_LONG_DEPLETED_TINY = registerItem("nuclear_waste_long_depleted_tiny", 
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> NUCLEAR_WASTE_SHORT_TINY = registerItem("nuclear_waste_short_tiny", 
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> NUCLEAR_WASTE_SHORT_DEPLETED_TINY = registerItem("nuclear_waste_short_depleted_tiny", 
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> NUCLEAR_WASTE_VITRIFIED_TINY = registerItem("nuclear_waste_vitrified_tiny", 
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> NUGGET_MERCURY_TINY = registerItem("nugget_mercury_tiny", 
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> SILICON_CIRCUIT = registerItem("silicon_circuit", 
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> BISMOID_CIRCUIT = registerItem("bismoid_circuit", 
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> QUANTUM_CHIP = registerItem("quantum_chip", 
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> CAPACITOR_BOARD = registerItem("capacitor_board", 
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> CAPACITOR_TANTALUM = registerItem("capacitor_tantalum", 
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> BISMOID_CHIP = registerItem("bismoid_chip", 
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> CONTROLLER_CHASSIS = registerItem("controller_chassis", 
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> CONTROLLER = registerItem("controller", 
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> CONTROLLER_ADVANCED = registerItem("controller_advanced", 
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> QUANTUM_COMPUTER = registerItem("quantum_computer", 
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> QUANTUM_CIRCUIT = registerItem("quantum_circuit", 
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> ANALOG_CIRCUIT = registerItem("analog_circuit", 
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> INTEGRATED_CIRCUIT = registerItem("integrated_circuit", 
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> ADVANCED_CIRCUIT = registerItem("advanced_circuit", 
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> VACUUM_TUBE = registerItem("vacuum_tube", 
            () -> new Item(new Item.Properties()));

    // EnumCircuitType.NUMITRON from the original's ItemCircuit - needed by the rbmk_numitron recipe.
    public static final RegistrySupplier<Item> CIRCUIT_NUMITRON = registerItem("circuit_numitron", 
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> CAPACITOR = registerItem("capacitor", 
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> CENTRIFUGE_ELEMENT = registerItem("centrifuge_element", 
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> MICROCHIP = registerItem("microchip", 
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> ATOMIC_CLOCK = registerItem("atomic_clock", 
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> PCB = registerItem("pcb", 
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> METAL_ROD = registerItem("metal_rod", 
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> BATTLE_MODULE = registerItem("battle_module", 
            () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BATTLE_GEARS = registerItem("battle_gears", 
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> BATTLE_CASING = registerItem("battle_casing", 
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> BATTLE_SENSOR = registerItem("battle_sensor", 
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> BATTLE_COUNTER = registerItem("battle_counter", 
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> CRT_DISPLAY = registerItem("crt_display", 
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> MAGNETRON = registerItem("magnetron", 
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> TURBINE_TITANIUM = registerItem("turbine_titanium", 
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> PLATE_KEVLAR = registerItem("plate_kevlar", 
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> PLATE_MIXED = registerItem("plate_mixed", 
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> PLATE_PAA = registerItem("plate_paa", 
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> INSULATOR = registerItem("insulator", 
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> PLATE_ARMOR_AJR = registerItem("plate_armor_ajr", 
        () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> PLATE_ARMOR_DNT = registerItem("plate_armor_dnt", 
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> PLATE_ARMOR_DNT_RUSTED = registerItem("plate_armor_dnt_rusted", 
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> PLATE_ARMOR_FAU = registerItem("plate_armor_fau", 
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> PLATE_ARMOR_HEV = registerItem("plate_armor_hev", 
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> PLATE_ARMOR_LUNAR = registerItem("plate_armor_lunar", 
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> PLATE_ARMOR_TITANIUM = registerItem("plate_armor_titanium", 
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> PLATE_CAST = registerItem("plate_cast", 
        () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> PLATE_CAST_ALT = registerItem("plate_cast_alt", 
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> PLATE_CAST_DARK = registerItem("plate_cast_dark", 
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> PLATE_DALEKANIUM = registerItem("plate_dalekanium", 
        () -> new Item(new Item.Properties()));

    // Research-Reactor-Brennstoffplatten - Funktion/Reaktivitaet/Lebensdauer 1:1 aus dem Original
    // (ModItems.java, registerDefaults) uebernommen.
    public static final RegistrySupplier<Item> PLATE_FUEL_MOX = registerItem("plate_fuel_mox", 
        () -> new com.hbm_m.item.industrial.ItemPlateFuel(new Item.Properties().stacksTo(1),
                2_400_000L, com.hbm_m.item.industrial.ItemPlateFuel.FunctionEnum.LOGARITHM, 50));

    public static final RegistrySupplier<Item> PLATE_FUEL_PU238BE = registerItem("plate_fuel_pu238be", 
            () -> new com.hbm_m.item.industrial.ItemPlateFuel(new Item.Properties().stacksTo(1),
                    1_000_000L, com.hbm_m.item.industrial.ItemPlateFuel.FunctionEnum.PASSIVE, 50));

    public static final RegistrySupplier<Item> PLATE_FUEL_PU239 = registerItem("plate_fuel_pu239", 
            () -> new com.hbm_m.item.industrial.ItemPlateFuel(new Item.Properties().stacksTo(1),
                    2_000_000L, com.hbm_m.item.industrial.ItemPlateFuel.FunctionEnum.NEGATIVE_QUADRATIC, 50));

    public static final RegistrySupplier<Item> PLATE_FUEL_RA226BE = registerItem("plate_fuel_ra226be", 
            () -> new com.hbm_m.item.industrial.ItemPlateFuel(new Item.Properties().stacksTo(1),
                    1_300_000L, com.hbm_m.item.industrial.ItemPlateFuel.FunctionEnum.PASSIVE, 30));

    public static final RegistrySupplier<Item> PLATE_FUEL_SA326 = registerItem("plate_fuel_sa326", 
            () -> new com.hbm_m.item.industrial.ItemPlateFuel(new Item.Properties().stacksTo(1),
                    2_000_000L, com.hbm_m.item.industrial.ItemPlateFuel.FunctionEnum.LINEAR, 80));

    public static final RegistrySupplier<Item> PLATE_FUEL_U233 = registerItem("plate_fuel_u233", 
            () -> new com.hbm_m.item.industrial.ItemPlateFuel(new Item.Properties().stacksTo(1),
                    2_200_000L, com.hbm_m.item.industrial.ItemPlateFuel.FunctionEnum.SQUARE_ROOT, 50));

    public static final RegistrySupplier<Item> PLATE_FUEL_U235 = registerItem("plate_fuel_u235", 
            () -> new com.hbm_m.item.industrial.ItemPlateFuel(new Item.Properties().stacksTo(1),
                    2_200_000L, com.hbm_m.item.industrial.ItemPlateFuel.FunctionEnum.SQUARE_ROOT, 40));

    // 1:1 with the original's ItemRBMKRod stat block for rbmk_fuel_drx (items/ModItems.java:3307-3313):
    // yield 10,000,000 / reactivity 1000 / selfRate 10 / QUADRATIC burn / heat 0.1 / melting point
    // 100,000. Was previously registered as a plain flavor-text Item, meaning it could never
    // actually be loaded as reactor fuel (RBMKRodBlock#use gates on `instanceof RBMKRodItem`).
    public static final RegistrySupplier<Item> RBMK_FUEL_DRX = registerItem("rbmk_fuel_drx", 
            () -> new RbmkFuelDrxItem(new Item.Properties())
                    .setYield(100_000_000).setStats(1000, 10).setFunction(RBMKRodItem.EnumBurnFunc.QUADRATIC)
                    .setHeat(0.1).setMeltingPoint(100_000).setTint(0xD77276).setPellet(() -> ModItems.RBMK_PELLET_DRX.get()));

    public static final RegistrySupplier<Item> ROD_ZIRNOX_EMPTY = registerItem("rod_zirnox_empty", 
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> ROD_ZIRNOX_LES_FUEL = registerItem("rod_zirnox_les_fuel", 
            () -> new ZirnoxRodItem(new Item.Properties(), 150_000, 150, false));

    public static final RegistrySupplier<Item> ROD_ZIRNOX_LES_FUEL_DEPLETED = registerItem("rod_zirnox_les_fuel_depleted", 
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> ROD_ZIRNOX_LITHIUM = registerItem("rod_zirnox_lithium", 
            () -> new ZirnoxRodItem(new Item.Properties(), 20_000, 0, true));

    public static final RegistrySupplier<Item> ROD_ZIRNOX_MOX_FUEL = registerItem("rod_zirnox_mox_fuel", 
            () -> new ZirnoxRodItem(new Item.Properties(), 165_000, 75, false));

    public static final RegistrySupplier<Item> ROD_ZIRNOX_MOX_FUEL_DEPLETED = registerItem("rod_zirnox_mox_fuel_depleted", 
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> ROD_ZIRNOX_NATURAL_URANIUM_FUEL = registerItem("rod_zirnox_natural_uranium_fuel", 
            () -> new ZirnoxRodItem(new Item.Properties(), 250_000, 30, false));

    public static final RegistrySupplier<Item> ROD_ZIRNOX_PLUTONIUM_FUEL = registerItem("rod_zirnox_plutonium_fuel", 
            () -> new ZirnoxRodItem(new Item.Properties(), 175_000, 65, false));

    public static final RegistrySupplier<Item> ROD_ZIRNOX_PLUTONIUM_FUEL_DEPLETED = registerItem("rod_zirnox_plutonium_fuel_depleted", 
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> ROD_ZIRNOX_TH232 = registerItem("rod_zirnox_th232", 
            () -> new ZirnoxRodItem(new Item.Properties(), 20_000, 0, true));

    public static final RegistrySupplier<Item> ROD_ZIRNOX_THORIUM_FUEL = registerItem("rod_zirnox_thorium_fuel", 
            () -> new ZirnoxRodItem(new Item.Properties(), 200_000, 40, false));

    public static final RegistrySupplier<Item> ROD_ZIRNOX_THORIUM_FUEL_DEPLETED = registerItem("rod_zirnox_thorium_fuel_depleted", 
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> ROD_ZIRNOX_TRITIUM = registerItem("rod_zirnox_tritium", 
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> ROD_ZIRNOX_U233_FUEL = registerItem("rod_zirnox_u233_fuel", 
            () -> new ZirnoxRodItem(new Item.Properties(), 150_000, 100, false));

    public static final RegistrySupplier<Item> ROD_ZIRNOX_U233_FUEL_DEPLETED = registerItem("rod_zirnox_u233_fuel_depleted", 
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> ROD_ZIRNOX_U235_FUEL = registerItem("rod_zirnox_u235_fuel", 
            () -> new ZirnoxRodItem(new Item.Properties(), 165_000, 85, false));

    public static final RegistrySupplier<Item> ROD_ZIRNOX_U235_FUEL_DEPLETED = registerItem("rod_zirnox_u235_fuel_depleted", 
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> ROD_ZIRNOX_URANIUM_FUEL = registerItem("rod_zirnox_uranium_fuel", 
            () -> new ZirnoxRodItem(new Item.Properties(), 200_000, 50, false));

    public static final RegistrySupplier<Item> ROD_ZIRNOX_URANIUM_FUEL_DEPLETED = registerItem("rod_zirnox_uranium_fuel_depleted", 
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> ROD_ZIRNOX_ZFB_MOX = registerItem("rod_zirnox_zfb_mox", 
            () -> new ZirnoxRodItem(new Item.Properties(), 50_000, 35, false));

    public static final RegistrySupplier<Item> ROD_ZIRNOX_ZFB_MOX_DEPLETED = registerItem("rod_zirnox_zfb_mox_depleted", 
            () -> new Item(new Item.Properties()));

    // RAW METALS

    public static final RegistrySupplier<Item> URANIUM_RAW = registerItem("uranium_raw", 
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> LEAD_RAW = registerItem("lead_raw", 
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> BERYLLIUM_RAW = registerItem("beryllium_raw", 
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> ALUMINUM_RAW = registerItem("aluminum_raw", 
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> TITANIUM_RAW = registerItem("titanium_raw", 
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> THORIUM_RAW = registerItem("thorium_raw", 
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> COBALT_RAW = registerItem("cobalt_raw", 
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> TUNGSTEN_RAW = registerItem("tungsten_raw", 
            () -> new Item(new Item.Properties()));

    // Bedrock-Ore-Veredelung: fehlende Rohmaterialien aus der Original-Rezeptkette
    // (ItemBedrockOreNew.BedrockOreType, siehe CentrifugeRecipes/CrystallizerRecipes des Originals).
    public static final RegistrySupplier<Item> RADIUM_RAW = registerItem("radium_raw", 
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> SALTPETER = registerItem("saltpeter", 
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> CRYOLITE = registerItem("cryolite", 
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> MOLYSITE = registerItem("molysite", 
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> RAREEARTH_RAW = registerItem("rareearth_raw", 
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> POWDER_CHLOROCALCITE = registerItem("powder_chlorocalcite", 
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> POWDER_SODIUM = registerItem("powder_sodium", 
            () -> new Item(new Item.Properties()));



    // РњР°С‚РµСЂРёР°Р»С‹
    public static final RegistrySupplier<Item> SULFUR = registerItem("sulfur", 
            () -> new Item(new Item.Properties()));

    // Kokerer-Ausgabe - im Original ein Metadata-Subtyp von ItemEnumMulti(EnumCokeType) mit
    // COAL/LIGNITE/PETROLEUM; hier nur die fuer den Coker benoetigte PETROLEUM-Variante als
    // eigenstaendiges Item (COAL/LIGNITE gehoeren zu anderen, noch nicht portierten Maschinen).
    public static final RegistrySupplier<Item> COKE_PETROLEUM = registerItem("coke_petroleum", 
            () -> new Item(new Item.Properties()));

    // Ashpit-Ausgabe - im Original ein Metadata-Subtyp von ItemEnumMulti(EnumAshType) mit
    // WOOD/COAL/MISC/FLY/SOOT (+FULLERENE, hier nicht benoetigt); hier als 5 eigenstaendige Items.
    // Burn-Ticks wie im Original-FuelHandler: 100/200/100/200/100.
    public static final RegistrySupplier<Item> ASH_WOOD = registerItem("ash_wood", () -> new FuelItem(new Item.Properties(), 100));
    public static final RegistrySupplier<Item> ASH_COAL = registerItem("ash_coal", () -> new FuelItem(new Item.Properties(), 200));
    public static final RegistrySupplier<Item> ASH_MISC = registerItem("ash_misc", () -> new FuelItem(new Item.Properties(), 100));
    public static final RegistrySupplier<Item> ASH_FLY  = registerItem("ash_fly", () -> new FuelItem(new Item.Properties(), 200));
    public static final RegistrySupplier<Item> ASH_SOOT = registerItem("ash_soot", () -> new FuelItem(new Item.Properties(), 100));

    // Teer - im Original ein ItemEnumMulti(EnumTarType) mit sechs Metadata-Subtypen
    // (CRUDE/CRACK/COAL/WOOD/WAX/PARAFFIN); hier als sechs eigenstaendige Items, analog zur
    // Asche oben. Der RBMK-Outgasser erzeugt COAL-Teer aus Kohle und verarbeitet COAL/WAX weiter;
    // die uebrigen Sorten gehoeren zu Raffinerie/Kristallisator und sind hier nur registriert,
    // damit die Familie vollstaendig ist und jene Rezepte spaeter darauf zeigen koennen.
    public static final RegistrySupplier<Item> OIL_TAR_CRUDE = registerItem("oil_tar_crude", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> OIL_TAR_CRACK = registerItem("oil_tar_crack", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> OIL_TAR_COAL = registerItem("oil_tar_coal", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> OIL_TAR_WOOD = registerItem("oil_tar_wood", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> OIL_TAR_WAX = registerItem("oil_tar_wax", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> OIL_TAR_PARAFFIN = registerItem("oil_tar_paraffin", () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> FLUORITE = registerItem("fluorite", 
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> RAREGROUND_ORE_CHUNK = registerItem("rareground_ore_chunk", 
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> FIRECLAY_BALL = registerItem("fireclay_ball", 
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> WOOD_ASH_POWDER = registerItem("wood_ash_powder", 
            () -> new Item(new Item.Properties()));

    /** РџРѕСЂС‚ {@code powder_desh_mix}. */
    public static final RegistrySupplier<Item> POWDER_DESH_MIX = registerItem("powder_desh_mix", 
            () -> new Item(new Item.Properties()));

    /** РџРѕСЂС‚ {@code powder_nitan_mix}. */
    public static final RegistrySupplier<Item> POWDER_NITAN_MIX = registerItem("powder_nitan_mix", 
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> LIGNITE_POWDER = registerItem("lignite_powder", 
            () -> new FuelItem(new Item.Properties(), 1200));

    public static final RegistrySupplier<Item> FIRE_POWDER = registerItem("fire_powder", 
            () -> new FuelItem(List.of(
                    Component.translatable("tooltip.hbm_m.fire_powder.desc1").withStyle(ChatFormatting.GRAY),
                    Component.translatable("tooltip.hbm_m.fire_powder.desc2").withStyle(ChatFormatting.GRAY)),
                    new Item.Properties(), 6400));

    public static final RegistrySupplier<Item> FIREBRICK = registerItem("firebrick", 
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> LIGNITE = registerItem("lignite", 
            () -> new FuelItem(new Item.Properties(), 1200));

    public static final RegistrySupplier<Item> CINNABAR = registerItem("cinnabar", 
            () -> new Item(new Item.Properties()));



    public static final RegistrySupplier<Item> MACHINE_ASSEMBLER = registerItem("machine_assembler", 
        () -> new MultiblockBlockItem(ModBlocks.MACHINE_ASSEMBLER.get(), new Item.Properties()));
            
    public static final RegistrySupplier<Item> ADVANCED_ASSEMBLY_MACHINE = registerItem("advanced_assembly_machine", 
        () -> new MultiblockBlockItem(ModBlocks.ADVANCED_ASSEMBLY_MACHINE.get(), new Item.Properties()));

    public static final RegistrySupplier<Item> HYDRAULIC_FRACKINING_TOWER = registerItem("hydraulic_frackining_tower", 
        () -> new MultiblockBlockItem(ModBlocks.HYDRAULIC_FRACKINING_TOWER.get(), new Item.Properties()));

    public static final RegistrySupplier<Item> COOLING_TOWER = registerItem("cooling_tower", 
        () -> new MultiblockBlockItem(ModBlocks.COOLING_TOWER.get(), new Item.Properties()));

	public static final RegistrySupplier<Item> TOWER_SMALL = registerItem("tower_small", 
        () -> new MultiblockBlockItem(ModBlocks.TOWER_SMALL.get(), new Item.Properties()));

	public static final RegistrySupplier<Item> CYCLOTRON = registerItem("cyclotron", 
        () -> new MultiblockBlockItem(ModBlocks.CYCLOTRON.get(), new Item.Properties()));

    // ===== Fusionsreaktor: Blockitems der Multiblock-Maschinen =====
    public static final RegistrySupplier<Item> FUSION_TORUS_ITEM = registerItem("torus", 
            () -> new MultiblockBlockItem(ModBlocks.TORUS.get(), new Item.Properties()));
    public static final RegistrySupplier<Item> FUSION_KLYSTRON_ITEM = registerItem("klystron", 
            () -> new MultiblockBlockItem(ModBlocks.KLYSTRON.get(), new Item.Properties()));
    public static final RegistrySupplier<Item> FUSION_KLYSTRON_CREATIVE_ITEM = registerItem("klystron_creative", 
            () -> new MultiblockBlockItem(ModBlocks.KLYSTRON_CREATIVE.get(), new Item.Properties()));
    public static final RegistrySupplier<Item> FUSION_BREEDER_ITEM = registerItem("breeder_fusion", 
            () -> new MultiblockBlockItem(ModBlocks.BREEDER_FUSION.get(), new Item.Properties()));
    public static final RegistrySupplier<Item> FUSION_COLLECTOR_ITEM = registerItem("collector", 
            () -> new MultiblockBlockItem(ModBlocks.COLLECTOR.get(), new Item.Properties()));
    public static final RegistrySupplier<Item> FUSION_COUPLER_ITEM = registerItem("coupler", 
            () -> new MultiblockBlockItem(ModBlocks.COUPLER.get(), new Item.Properties()));
    public static final RegistrySupplier<Item> FUSION_BOILER_ITEM = registerItem("boiler_fusion", 
            () -> new MultiblockBlockItem(ModBlocks.BOILER_FUSION.get(), new Item.Properties()));
    public static final RegistrySupplier<Item> FUSION_MHDT_ITEM = registerItem("mhdt", 
            () -> new MultiblockBlockItem(ModBlocks.MHDT.get(), new Item.Properties()));
    public static final RegistrySupplier<Item> FUSION_PLASMA_FORGE_ITEM = registerItem("plasma_forge", 
            () -> new MultiblockBlockItem(ModBlocks.PLASMA_FORGE.get(), new Item.Properties()));
    public static final RegistrySupplier<Item> HEATING_OVEN = registerItem("heating_oven", 
        () -> new MultiblockBlockItem(ModBlocks.HEATING_OVEN.get(), new Item.Properties()));

    // Тигель и нагреватели — мультиблоки (проверка места ДО установки, подсветка
    // препятствий, пульсирующая рамка футпринта — та же система, что у сборочных машин).
    public static final RegistrySupplier<Item> CRUCIBLE = registerItem("crucible", 
        () -> new MultiblockBlockItem(ModBlocks.CRUCIBLE.get(), new Item.Properties()));

    public static final RegistrySupplier<Item> ELECTRIC_HEATER = registerItem("electric_heater", 
        () -> new MultiblockBlockItem(ModBlocks.ELECTRIC_HEATER.get(), new Item.Properties()));

    public static final RegistrySupplier<Item> FIREBOX = registerItem("firebox", 
        () -> new MultiblockBlockItem(ModBlocks.FIREBOX.get(), new Item.Properties()));

    public static final RegistrySupplier<Item> HEATEX = registerItem("heatex", 
        () -> new MultiblockBlockItem(ModBlocks.HEATEX.get(), new Item.Properties()));

    // Strand Caster — мультиблок: рамка футпринта при удержании + проверка препятствий
    // при установке (порт BlockDummyable 1.7.10, offset 0).
    public static final RegistrySupplier<Item> STRAND_CASTER = registerItem("strand_caster", 
        () -> new MultiblockBlockItem(ModBlocks.STRAND_CASTER.get(), new Item.Properties()));

    public static final RegistrySupplier<Item> OILBURNER = registerItem("oilburner", 
        () -> new MultiblockBlockItem(ModBlocks.OILBURNER.get(), new Item.Properties()));

    public static final RegistrySupplier<Item> OILBURNER_HP = registerItem("oilburner_hp", 
        () -> new MultiblockBlockItem(ModBlocks.OILBURNER_HP.get(), new Item.Properties()));

    public static final RegistrySupplier<Item> PART_LITHIUM    = registerItem("part_lithium", () -> new Item(new Item.Properties().stacksTo(16)));
    /** Beryllium particle вЂ” medium-energy cyclotron projectile. */
    public static final RegistrySupplier<Item> PART_BERYLLIUM  = registerItem("part_beryllium", () -> new Item(new Item.Properties().stacksTo(16)));
    /** Carbon (coal-derived) particle вЂ” low-energy cyclotron projectile. */
    public static final RegistrySupplier<Item> PART_CARBON     = registerItem("part_carbon", () -> new Item(new Item.Properties().stacksTo(16)));
    /** Copper ion вЂ” medium-energy cyclotron projectile. */
    public static final RegistrySupplier<Item> PART_COPPER     = registerItem("part_copper", () -> new Item(new Item.Properties().stacksTo(16)));
    /** Plutonium nucleus вЂ” high-energy cyclotron projectile, produces australium. */
    public static final RegistrySupplier<Item> PART_PLUTONIUM  = registerItem("part_plutonium", () -> new Item(new Item.Properties().stacksTo(16)));

    public static final RegistrySupplier<Item> MOLD_BARREL_HEAVY = registerItem("mold_barrel_heavy", 
            () -> new com.hbm_m.item.material.ItemCastMold(com.hbm_m.item.material.ItemCastMold.MoldType.BARREL_HEAVY, new Item.Properties()));
    public static final RegistrySupplier<Item> MOLD_BARREL_LIGHT = registerItem("mold_barrel_light", 
            () -> new com.hbm_m.item.material.ItemCastMold(com.hbm_m.item.material.ItemCastMold.MoldType.BARREL_LIGHT, new Item.Properties()));
    public static final RegistrySupplier<Item> MOLD_BASE = registerItem("mold_base",
            // Оригинал: mold_base — ПУСТАЯ форма-заготовка (plain Item, не тип формы);
            // заготовка не вставляется в бассейн, а является входом кузнечной резки форм.
            () -> new Item(new Item.Properties().stacksTo(16)));
    public static final RegistrySupplier<Item> MOLD_BILLET = registerItem("mold_billet", 
            () -> new com.hbm_m.item.material.ItemCastMold(com.hbm_m.item.material.ItemCastMold.MoldType.BILLET, new Item.Properties()));
    public static final RegistrySupplier<Item> MOLD_BLADE = registerItem("mold_blade", 
            () -> new com.hbm_m.item.material.ItemCastMold(com.hbm_m.item.material.ItemCastMold.MoldType.BLADE, new Item.Properties()));
    public static final RegistrySupplier<Item> MOLD_BLADES = registerItem("mold_blades", 
            () -> new com.hbm_m.item.material.ItemCastMold(com.hbm_m.item.material.ItemCastMold.MoldType.BLADES, new Item.Properties()));
    public static final RegistrySupplier<Item> MOLD_BLOCK = registerItem("mold_block", 
            () -> new com.hbm_m.item.material.ItemCastMold(com.hbm_m.item.material.ItemCastMold.MoldType.BLOCK, new Item.Properties()));
    public static final RegistrySupplier<Item> MOLD_C357 = registerItem("mold_c357", 
            () -> new com.hbm_m.item.material.ItemCastMold(com.hbm_m.item.material.ItemCastMold.MoldType.C357, new Item.Properties()));
    public static final RegistrySupplier<Item> MOLD_CBUCKSHOT = registerItem("mold_cbuckshot", 
            () -> new com.hbm_m.item.material.ItemCastMold(com.hbm_m.item.material.ItemCastMold.MoldType.CBUCKSHOT, new Item.Properties()));
    public static final RegistrySupplier<Item> MOLD_GEM = registerItem("mold_gem", 
            () -> new com.hbm_m.item.material.ItemCastMold(com.hbm_m.item.material.ItemCastMold.MoldType.GEM, new Item.Properties()));
    public static final RegistrySupplier<Item> MOLD_GRIP = registerItem("mold_grip",
            () -> new com.hbm_m.item.material.ItemCastMold(com.hbm_m.item.material.ItemCastMold.MoldType.GRIP, new Item.Properties()));
    public static final RegistrySupplier<Item> MOLD_INGOT = registerItem("mold_ingot",
            () -> new com.hbm_m.item.material.ItemCastMold(com.hbm_m.item.material.ItemCastMold.MoldType.INGOT, new Item.Properties()));
    public static final RegistrySupplier<Item> MOLD_INGOTS = registerItem("mold_ingots", 
            () -> new com.hbm_m.item.material.ItemCastMold(com.hbm_m.item.material.ItemCastMold.MoldType.INGOTS, new Item.Properties()));
    public static final RegistrySupplier<Item> MOLD_MECHANISM = registerItem("mold_mechanism", 
            () -> new com.hbm_m.item.material.ItemCastMold(com.hbm_m.item.material.ItemCastMold.MoldType.MECHANISM, new Item.Properties()));
    public static final RegistrySupplier<Item> MOLD_NUGGET = registerItem("mold_nugget", 
            () -> new com.hbm_m.item.material.ItemCastMold(com.hbm_m.item.material.ItemCastMold.MoldType.NUGGET, new Item.Properties()));
    public static final RegistrySupplier<Item> MOLD_PIPE = registerItem("mold_pipe", 
            () -> new com.hbm_m.item.material.ItemCastMold(com.hbm_m.item.material.ItemCastMold.MoldType.PIPE, new Item.Properties()));
    public static final RegistrySupplier<Item> MOLD_PIPES = registerItem("mold_pipes", 
            () -> new com.hbm_m.item.material.ItemCastMold(com.hbm_m.item.material.ItemCastMold.MoldType.PIPES, new Item.Properties()));
    public static final RegistrySupplier<Item> MOLD_PLATE = registerItem("mold_plate", 
            () -> new com.hbm_m.item.material.ItemCastMold(com.hbm_m.item.material.ItemCastMold.MoldType.PLATE, new Item.Properties()));
    public static final RegistrySupplier<Item> MOLD_PLATE_CAST = registerItem("mold_plate_cast", 
            () -> new com.hbm_m.item.material.ItemCastMold(com.hbm_m.item.material.ItemCastMold.MoldType.PLATE_CAST, new Item.Properties()));
    public static final RegistrySupplier<Item> MOLD_PLATES = registerItem("mold_plates", 
            () -> new com.hbm_m.item.material.ItemCastMold(com.hbm_m.item.material.ItemCastMold.MoldType.PLATES, new Item.Properties()));
    public static final RegistrySupplier<Item> MOLD_PLATES_CAST = registerItem("mold_plates_cast", 
            () -> new com.hbm_m.item.material.ItemCastMold(com.hbm_m.item.material.ItemCastMold.MoldType.PLATES_CAST, new Item.Properties()));
    public static final RegistrySupplier<Item> MOLD_RECEIVER_HEAVY = registerItem("mold_receiver_heavy", 
            () -> new com.hbm_m.item.material.ItemCastMold(com.hbm_m.item.material.ItemCastMold.MoldType.RECEIVER_HEAVY, new Item.Properties()));
    public static final RegistrySupplier<Item> MOLD_RECEIVER_LIGHT = registerItem("mold_receiver_light", 
            () -> new com.hbm_m.item.material.ItemCastMold(com.hbm_m.item.material.ItemCastMold.MoldType.RECEIVER_LIGHT, new Item.Properties()));
    public static final RegistrySupplier<Item> MOLD_SHELL = registerItem("mold_shell", 
            () -> new com.hbm_m.item.material.ItemCastMold(com.hbm_m.item.material.ItemCastMold.MoldType.SHELL, new Item.Properties()));
    public static final RegistrySupplier<Item> MOLD_STAMP = registerItem("mold_stamp", 
            () -> new com.hbm_m.item.material.ItemCastMold(com.hbm_m.item.material.ItemCastMold.MoldType.STAMP, new Item.Properties()));
    public static final RegistrySupplier<Item> MOLD_STOCK = registerItem("mold_stock", 
            () -> new com.hbm_m.item.material.ItemCastMold(com.hbm_m.item.material.ItemCastMold.MoldType.STOCK, new Item.Properties()));
    public static final RegistrySupplier<Item> MOLD_WIRE = registerItem("mold_wire", 
            () -> new com.hbm_m.item.material.ItemCastMold(com.hbm_m.item.material.ItemCastMold.MoldType.WIRE, new Item.Properties()));
    public static final RegistrySupplier<Item> MOLD_WIRE_DENSE = registerItem("mold_wire_dense", 
            () -> new com.hbm_m.item.material.ItemCastMold(com.hbm_m.item.material.ItemCastMold.MoldType.WIRE_DENSE, new Item.Properties()));
    public static final RegistrySupplier<Item> MOLD_WIRES_DENSE = registerItem("mold_wires_dense", 
            () -> new com.hbm_m.item.material.ItemCastMold(com.hbm_m.item.material.ItemCastMold.MoldType.WIRES_DENSE, new Item.Properties()));

    public static final RegistrySupplier<Item> ZIRNOX = registerItem("zirnox", 
        () -> new MultiblockBlockItem(ModBlocks.ZIRNOX.get(), new Item.Properties()));

	public static final RegistrySupplier<Item> ARC_WELDER = registerItem("arc_welder", 
        () -> new MultiblockBlockItem(ModBlocks.ARC_WELDER.get(), new Item.Properties()));

	public static final RegistrySupplier<Item> SOLDERING_STATION = registerItem("soldering_station", 
        () -> new MultiblockBlockItem(ModBlocks.SOLDERING_STATION.get(), new Item.Properties()));

	public static final RegistrySupplier<Item> MIXER = registerItem("mixer", 
        () -> new MultiblockBlockItem(ModBlocks.MIXER.get(), new Item.Properties()));

	public static final RegistrySupplier<Item> DERRICK = registerItem("derrick", 
        () -> new MultiblockBlockItem(ModBlocks.DERRICK.get(), new Item.Properties()));

	public static final RegistrySupplier<Item> ASHPIT = registerItem("ashpit", 
        () -> new MultiblockBlockItem(ModBlocks.ASHPIT.get(), new Item.Properties()));

	public static final RegistrySupplier<Item> REACTOR_RESEARCH = registerItem("reactor_research", 
        () -> new MultiblockBlockItem(ModBlocks.REACTOR_RESEARCH.get(), new Item.Properties()));

	public static final RegistrySupplier<Item> RBMK_CONSOLE = registerItem("rbmk_console", 
        () -> new MultiblockBlockItem(ModBlocks.RBMK_CONSOLE.get(), new Item.Properties()));

	public static final RegistrySupplier<Item> FLARE_STACK = registerItem("flare_stack", 
        () -> new MultiblockBlockItem(ModBlocks.FLARE_STACK.get(), new Item.Properties()));

	public static final RegistrySupplier<Item> PUMPJACK = registerItem("pumpjack", 
        () -> new MultiblockBlockItem(ModBlocks.PUMPJACK.get(), new Item.Properties()));

	public static final RegistrySupplier<Item> RADAR = registerItem("radar", 
        () -> new MultiblockBlockItem(ModBlocks.RADAR.get(), new Item.Properties()));

	public static final RegistrySupplier<Item> LARGE_RADAR = registerItem("large_radar", 
	    () -> new MultiblockBlockItem(ModBlocks.LARGE_RADAR.get(), new Item.Properties()));

	public static final RegistrySupplier<Item> RADAR_SCREEN = registerItem("radar_screen", 
	    () -> new MultiblockBlockItem(ModBlocks.RADAR_SCREEN.get(), new Item.Properties()));

	public static final RegistrySupplier<Item> CRACKING_TOWER = registerItem("cracking_tower", 
        () -> new MultiblockBlockItem(ModBlocks.CRACKING_TOWER.get(), new Item.Properties()));

	public static final RegistrySupplier<Item> FRACTION_TOWER = registerItem("fraction_tower", 
        () -> new MultiblockBlockItem(ModBlocks.FRACTION_TOWER.get(), new Item.Properties()));

	public static final RegistrySupplier<Item> MINING_DRILL = registerItem("mining_drill", 
        () -> new MultiblockBlockItem(ModBlocks.MINING_DRILL.get(), new Item.Properties()));

	public static final RegistrySupplier<Item> FEL = registerItem("fel", 
        () -> new MultiblockBlockItem(ModBlocks.FEL.get(), new Item.Properties()));

	public static final RegistrySupplier<Item> SILEX = registerItem("silex", 
        () -> new MultiblockBlockItem(ModBlocks.SILEX.get(), new Item.Properties()));

	public static final RegistrySupplier<Item> CHEMICAL_PLANT = registerItem("chemical_plant", 
        () -> new MultiblockBlockItem(ModBlocks.CHEMICAL_PLANT.get(), new Item.Properties()));

	public static final RegistrySupplier<Item> MACHINE_GASCENT = registerItem("machine_gascent", 
        () -> new MultiblockBlockItem(ModBlocks.MACHINE_GASCENT.get(), new Item.Properties()));

	public static final RegistrySupplier<Item> CRYSTALLIZER = registerItem("crystallizer", 
        () -> new MultiblockBlockItem(ModBlocks.CRYSTALLIZER.get(), new Item.Properties()));

	public static final RegistrySupplier<Item> BREEDER = registerItem("breeder", 
        () -> new MultiblockBlockItem(ModBlocks.BREEDER.get(), new Item.Properties()));

	public static final RegistrySupplier<Item> LARGE_PYLON = registerItem("large_pylon", 
        () -> new MultiblockBlockItem(ModBlocks.LARGE_PYLON.get(), new Item.Properties()));

	public static final RegistrySupplier<Item> MACHINE_CENTRIFUGE = registerItem("machine_centrifuge", 
        () -> new MultiblockBlockItem(ModBlocks.MACHINE_CENTRIFUGE.get(), new Item.Properties()));

	public static final RegistrySupplier<Item> FLUID_TANK = registerItem("fluid_tank", 
        () -> new com.hbm_m.multiblock.FluidTankBlockItem(ModBlocks.FLUID_TANK.get(), new Item.Properties()));

	public static final RegistrySupplier<Item> BAT9000 = registerItem("bat9000", 
        () -> new MultiblockBlockItem(ModBlocks.BAT9000.get(), new Item.Properties()));

    public static final RegistrySupplier<Item> MACHINE_BATTERY_SOCKET = registerItem("machine_battery_socket", 
        () -> new MultiblockBlockItem(ModBlocks.MACHINE_BATTERY_SOCKET.get(), new Item.Properties()));

    public static final RegistrySupplier<Item> PRESS = registerItem("press", 
        () -> new MultiblockBlockItem(ModBlocks.PRESS.get(), new Item.Properties()));

    public static final RegistrySupplier<Item> WOOD_BURNER = registerItem("wood_burner", 
        () -> new MultiblockBlockItem(ModBlocks.WOOD_BURNER.get(), new Item.Properties()));

    public static final RegistrySupplier<Item> INDUSTRIAL_BOILER = registerItem("industrial_boiler", 
        () -> new MultiblockBlockItem(ModBlocks.INDUSTRIAL_BOILER.get(), new Item.Properties()));

    public static final RegistrySupplier<Item> SOLAR_BOILER = registerItem("solar_boiler", 
        () -> new MultiblockBlockItem(ModBlocks.SOLAR_BOILER.get(), new Item.Properties()));

    public static final RegistrySupplier<Item> SOLAR_MIRRORS = registerItem("solar_mirrors", 
        () -> new MultiblockBlockItem(ModBlocks.SOLAR_MIRRORS.get(), new Item.Properties()));

    public static final RegistrySupplier<Item> WATZ_POWERPLANT = registerItem("watz_powerplant", 
        () -> new MultiblockBlockItem(ModBlocks.WATZ_POWERPLANT.get(), new Item.Properties()));

    // Watz reactor pellets - see com.hbm_m.item.nuclear.WatzPelletType for the mechanics.
    public static final RegistrySupplier<Item> WATZ_PELLET_SCHRABIDIUM_OXIDE = registerItem("watz_pellet_schrabidium_oxide", 
        () -> new WatzPelletItem(new Item.Properties(), WatzPelletType.SCHRABIDIUM_OXIDE));
    public static final RegistrySupplier<Item> WATZ_PELLET_SCHRABIDIUM_OXIDE_DEPLETED = registerItem("watz_pellet_schrabidium_oxide_depleted", 
        () -> new Item(new Item.Properties().stacksTo(16)));

    public static final RegistrySupplier<Item> WATZ_PELLET_LES_OXIDE = registerItem("watz_pellet_les_oxide", 
        () -> new WatzPelletItem(new Item.Properties(), WatzPelletType.LES_OXIDE));
    public static final RegistrySupplier<Item> WATZ_PELLET_LES_OXIDE_DEPLETED = registerItem("watz_pellet_les_oxide_depleted", 
        () -> new Item(new Item.Properties().stacksTo(16)));

    public static final RegistrySupplier<Item> WATZ_PELLET_NATURAL_URANIUM = registerItem("watz_pellet_natural_uranium", 
        () -> new WatzPelletItem(new Item.Properties(), WatzPelletType.NATURAL_URANIUM));
    public static final RegistrySupplier<Item> WATZ_PELLET_NATURAL_URANIUM_DEPLETED = registerItem("watz_pellet_natural_uranium_depleted", 
        () -> new Item(new Item.Properties().stacksTo(16)));

    public static final RegistrySupplier<Item> WATZ_PELLET_BORON_CARBIDE = registerItem("watz_pellet_boron_carbide", 
        () -> new WatzPelletItem(new Item.Properties(), WatzPelletType.BORON_CARBIDE));
    public static final RegistrySupplier<Item> WATZ_PELLET_BORON_CARBIDE_DEPLETED = registerItem("watz_pellet_boron_carbide_depleted", 
        () -> new Item(new Item.Properties().stacksTo(16)));

    public static final RegistrySupplier<Item> WATZ_PELLET_LEAD_SHIELD = registerItem("watz_pellet_lead_shield", 
        () -> new WatzPelletItem(new Item.Properties(), WatzPelletType.LEAD_SHIELD));
    public static final RegistrySupplier<Item> WATZ_PELLET_LEAD_SHIELD_DEPLETED = registerItem("watz_pellet_lead_shield_depleted", 
        () -> new Item(new Item.Properties().stacksTo(16)));

    public static final RegistrySupplier<Item> HYDROTREATER = registerItem("hydrotreater", 
        () -> new MultiblockBlockItem(ModBlocks.HYDROTREATER.get(), new Item.Properties()));

    public static final RegistrySupplier<Item> CATALYTIC_REFORMER = registerItem("catalytic_reformer", 
        () -> new MultiblockBlockItem(ModBlocks.CATALYTIC_REFORMER.get(), new Item.Properties()));

    public static final RegistrySupplier<Item> DEUTERIUM_TOWER = registerItem("deuterium_tower", 
        () -> new MultiblockBlockItem(ModBlocks.DEUTERIUM_TOWER.get(), new Item.Properties()));

    public static final RegistrySupplier<Item> CHEMICAL_FACTORY = registerItem("chemical_factory", 
        () -> new MultiblockBlockItem(ModBlocks.CHEMICAL_FACTORY.get(), new Item.Properties()));

    public static final RegistrySupplier<Item> STEAM_TURBINE = registerItem("steam_turbine", 
        () -> new MultiblockBlockItem(ModBlocks.STEAM_TURBINE.get(), new Item.Properties()));

    public static final RegistrySupplier<Item> LIQUEFACTOR = registerItem("liquefactor", 
        () -> new MultiblockBlockItem(ModBlocks.LIQUEFACTOR.get(), new Item.Properties()));

    public static final RegistrySupplier<Item> CORE_EMITTER = registerItem("core_emitter", 
        () -> new MultiblockBlockItem(ModBlocks.CORE_EMITTER.get(), new Item.Properties()));

    public static final RegistrySupplier<Item> CORE_INJECTOR = registerItem("core_injector", 
        () -> new MultiblockBlockItem(ModBlocks.CORE_INJECTOR.get(), new Item.Properties()));

    public static final RegistrySupplier<Item> CORE_RECEIVER = registerItem("core_receiver", 
        () -> new MultiblockBlockItem(ModBlocks.CORE_RECEIVER.get(), new Item.Properties()));

    public static final RegistrySupplier<Item> VACUUM_DISTILL = registerItem("vacuum_distill", 
        () -> new MultiblockBlockItem(ModBlocks.VACUUM_DISTILL.get(), new Item.Properties()));

    public static final RegistrySupplier<Item> TURBOFAN = registerItem("turbofan", 
        () -> new MultiblockBlockItem(ModBlocks.TURBOFAN.get(), new Item.Properties()) {
            // Оригинальный тултип предмета: "Fuel efficiency:" / "-Aviation: 100%".
            @Override
            public void appendHbmTooltip(net.minecraft.world.item.ItemStack stack,
                                         net.minecraft.world.level.Level level,
                                         java.util.List<net.minecraft.network.chat.Component> tooltip,
                                         net.minecraft.world.item.TooltipFlag flag) {
                tooltip.add(net.minecraft.network.chat.Component.translatable("desc.hbm_m.turbofan.efficiency"));
                tooltip.add(net.minecraft.network.chat.Component.translatable("desc.hbm_m.turbofan.aviation")
                        .withStyle(net.minecraft.ChatFormatting.GRAY));
            }
        });

    public static final RegistrySupplier<Item> INDUSTRIAL_TURBINE = registerItem("industrial_turbine", 
        () -> new MultiblockBlockItem(ModBlocks.INDUSTRIAL_TURBINE.get(), new Item.Properties()));

    public static final RegistrySupplier<Item> MACHINE_CHUNGUS = registerItem("machine_chungus", 
        () -> new MultiblockBlockItem(ModBlocks.MACHINE_CHUNGUS.get(), new Item.Properties()));

    public static final RegistrySupplier<Item> TURBINE = registerItem("turbine", 
        () -> new MultiblockBlockItem(ModBlocks.TURBINE.get(), new Item.Properties()));

    public static final RegistrySupplier<Item> SUBSTATION = registerItem("substation", 
        () -> new MultiblockBlockItem(ModBlocks.SUBSTATION.get(), new Item.Properties()));

    public static final RegistrySupplier<Item> REFINERY = registerItem("refinery", 
        () -> new MultiblockBlockItem(ModBlocks.REFINERY.get(), new Item.Properties()));
	public static final RegistrySupplier<Item> LAUNCH_PAD = registerItem("launch_pad", 
        () -> new MultiblockBlockItem(ModBlocks.LAUNCH_PAD.get(), new Item.Properties()));

	public static final RegistrySupplier<Item> LAUNCH_PAD_RUSTED = registerItem("launch_pad_rusted", 
        () -> new MultiblockBlockItem(ModBlocks.LAUNCH_PAD_RUSTED.get(), new Item.Properties()));

	public static final RegistrySupplier<Item> NUKE_FAT_MAN = registerItem("nuke_fat_man", 
        () -> new MultiblockBlockItem(ModBlocks.NUKE_FAT_MAN.get(), new Item.Properties()));

    public static final RegistrySupplier<Item> NUKE_PROTOTYPE = registerItem("nuke_prototype", 
        () -> new net.minecraft.world.item.BlockItem(com.hbm_m.block.ModBlocks.NUKE_PROTOTYPE.get(), new Item.Properties()));

    // РџР РћРўРћРўРРџ Р РђРљР•РўР« (TIER 0, MICRO)
    public static final RegistrySupplier<Item> MISSILE_TEST = registerItem("missile_test", 
        () -> new MissileItem(MissileItem.MissileFormFactor.MICRO, MissileItem.MissileTier.TIER0,
                MissileItem.MissileFuel.SOLID));

    public static final RegistrySupplier<Item> MISSILE_ANTI_BALLISTIC = registerItem("missile_anti_ballistic", 
                () -> new MissileItem(MissileItem.MissileFormFactor.ABM, MissileItem.MissileTier.TIER1,
                                MissileItem.MissileFuel.SOLID));

  /** РЎРёРЅРіСѓР»СЏСЂРЅРѕСЃС‚Рё / РѕРїР°СЃРЅС‹Рµ РґСЂРѕРїС‹ (1.7.10 {@code ModItems.black_hole}, {@code pellet_antimatter}, {@code flame_pony}). */
    public static final RegistrySupplier<Item> BLACK_HOLE = registerItem("black_hole", 
            () -> new com.hbm_m.item.special.ItemDrop(new Item.Properties().stacksTo(1)));
    public static final RegistrySupplier<Item> PELLET_ANTIMATTER = registerItem("pellet_antimatter", 
            () -> new com.hbm_m.item.special.ItemDrop(new Item.Properties().stacksTo(1)));
    public static final RegistrySupplier<Item> FLAME_PONY = registerItem("flame_pony", 
            () -> new Item(new Item.Properties()));

    // Tier 0
    public static final RegistrySupplier<Item> MISSILE_MICRO = registerItem("missile_micro", 
            () -> new MissileItem(MissileItem.MissileFormFactor.MICRO, MissileItem.MissileTier.TIER0));
    public static final RegistrySupplier<Item> MISSILE_SCHRABIDIUM = registerItem("missile_schrabidium", 
            () -> new MissileItem(MissileItem.MissileFormFactor.MICRO, MissileItem.MissileTier.TIER0));
    public static final RegistrySupplier<Item> MISSILE_BHOLE = registerItem("missile_bhole", 
            () -> new MissileItem(MissileItem.MissileFormFactor.MICRO, MissileItem.MissileTier.TIER0));
    public static final RegistrySupplier<Item> MISSILE_TAINT = registerItem("missile_taint", 
            () -> new MissileItem(MissileItem.MissileFormFactor.MICRO, MissileItem.MissileTier.TIER0));
    public static final RegistrySupplier<Item> MISSILE_EMP = registerItem("missile_emp", 
            () -> new MissileItem(MissileItem.MissileFormFactor.MICRO, MissileItem.MissileTier.TIER0));

    // Tier 1
    public static final RegistrySupplier<Item> MISSILE_GENERIC = registerItem("missile_generic", 
            () -> new MissileItem(MissileItem.MissileFormFactor.V2, MissileItem.MissileTier.TIER1));
    public static final RegistrySupplier<Item> MISSILE_INCENDIARY = registerItem("missile_incendiary", 
            () -> new MissileItem(MissileItem.MissileFormFactor.V2, MissileItem.MissileTier.TIER1));
    public static final RegistrySupplier<Item> MISSILE_CLUSTER = registerItem("missile_cluster", 
            () -> new MissileItem(MissileItem.MissileFormFactor.V2, MissileItem.MissileTier.TIER1));
    public static final RegistrySupplier<Item> MISSILE_BUSTER = registerItem("missile_buster", 
            () -> new MissileItem(MissileItem.MissileFormFactor.V2, MissileItem.MissileTier.TIER1));
    public static final RegistrySupplier<Item> MISSILE_DECOY = registerItem("missile_decoy", 
            () -> new MissileItem(MissileItem.MissileFormFactor.V2, MissileItem.MissileTier.TIER1));

    public static final RegistrySupplier<Item> MISSILE_STEALTH = registerItem("missile_stealth", 
            () -> new MissileItem(MissileItem.MissileFormFactor.STEALTH, MissileItem.MissileTier.TIER1));

    // Tier 2
    public static final RegistrySupplier<Item> MISSILE_STRONG = registerItem("missile_strong", 
            () -> new MissileItem(MissileItem.MissileFormFactor.STRONG, MissileItem.MissileTier.TIER2));
    public static final RegistrySupplier<Item> MISSILE_INCENDIARY_STRONG = registerItem("missile_incendiary_strong", 
            () -> new MissileItem(MissileItem.MissileFormFactor.STRONG, MissileItem.MissileTier.TIER2));
    public static final RegistrySupplier<Item> MISSILE_CLUSTER_STRONG = registerItem("missile_cluster_strong", 
            () -> new MissileItem(MissileItem.MissileFormFactor.STRONG, MissileItem.MissileTier.TIER2));
    public static final RegistrySupplier<Item> MISSILE_BUSTER_STRONG = registerItem("missile_buster_strong", 
            () -> new MissileItem(MissileItem.MissileFormFactor.STRONG, MissileItem.MissileTier.TIER2));
    public static final RegistrySupplier<Item> MISSILE_EMP_STRONG = registerItem("missile_emp_strong", 
            () -> new MissileItem(MissileItem.MissileFormFactor.STRONG, MissileItem.MissileTier.TIER2));

    // Tier 3
    public static final RegistrySupplier<Item> MISSILE_BURST = registerItem("missile_burst", 
            () -> new MissileItem(MissileItem.MissileFormFactor.HUGE, MissileItem.MissileTier.TIER3));
    public static final RegistrySupplier<Item> MISSILE_INFERNO = registerItem("missile_inferno", 
            () -> new MissileItem(MissileItem.MissileFormFactor.HUGE, MissileItem.MissileTier.TIER3));
    public static final RegistrySupplier<Item> MISSILE_RAIN = registerItem("missile_rain", 
            () -> new MissileItem(MissileItem.MissileFormFactor.HUGE, MissileItem.MissileTier.TIER3));
    public static final RegistrySupplier<Item> MISSILE_DRILL = registerItem("missile_drill", 
            () -> new MissileItem(MissileItem.MissileFormFactor.HUGE, MissileItem.MissileTier.TIER3));
    public static final RegistrySupplier<Item> MISSILE_SHUTTLE = registerItem("missile_shuttle", 
            () -> new MissileItem(MissileItem.MissileFormFactor.OTHER, MissileItem.MissileTier.TIER3,
                    MissileItem.MissileFuel.KEROSENE_PEROXIDE));

    // Soyuz Launcher lander module (the rocket itself reuses ModBlocks.DECO_SOYUZ_ROCKET's item - see SoyuzLauncherBlockEntity.rocketItem())
    public static final RegistrySupplier<Item> MISSILE_SOYUZ_LANDER = registerItem("missile_soyuz_lander", 
            () -> new Item(new Item.Properties()));

    // Tier 4
    public static final RegistrySupplier<Item> MISSILE_NUCLEAR = registerItem("missile_nuclear", 
            () -> new MissileItem(MissileItem.MissileFormFactor.ATLAS, MissileItem.MissileTier.TIER4));
    public static final RegistrySupplier<Item> MISSILE_NUCLEAR_CLUSTER = registerItem("missile_nuclear_cluster", 
            () -> new MissileItem(MissileItem.MissileFormFactor.ATLAS, MissileItem.MissileTier.TIER4));
    public static final RegistrySupplier<Item> MISSILE_VOLCANO = registerItem("missile_volcano", 
            () -> new MissileItem(MissileItem.MissileFormFactor.ATLAS, MissileItem.MissileTier.TIER4));
    public static final RegistrySupplier<Item> MISSILE_DOOMSDAY = registerItem("missile_doomsday", 
            () -> new MissileItem(MissileItem.MissileFormFactor.ATLAS, MissileItem.MissileTier.TIER4));
    public static final RegistrySupplier<Item> MISSILE_DOOMSDAY_RUSTED = registerItem("missile_doomsday_rusted", 
            () -> new MissileItem(MissileItem.MissileFormFactor.ATLAS, MissileItem.MissileTier.TIER4).notLaunchable());

    public static final RegistrySupplier<Item> DESIGNATOR = registerItem("designator", 
        () -> new ItemDesignator(new Item.Properties()));
    public static final RegistrySupplier<Item> DESIGNATOR_RANGE = registerItem("designator_range", 
        () -> new ItemDesignatorRange(new Item.Properties()));
    public static final RegistrySupplier<Item> DESIGNATOR_MANUAL = registerItem("designator_manual", 
        () -> new ItemDesignatorManual(new Item.Properties()));

	// MULTIBLOCK DOORS

    public static final RegistrySupplier<Item> LARGE_VEHICLE_DOOR = registerItem("large_vehicle_door", 
        () -> new DoorBlockItem(ModBlocks.LARGE_VEHICLE_DOOR.get(), new Item.Properties()));

    public static final RegistrySupplier<Item> ROUND_AIRLOCK_DOOR = registerItem("round_airlock_door", 
        () -> new DoorBlockItem(ModBlocks.ROUND_AIRLOCK_DOOR.get(), new Item.Properties()));

    public static final RegistrySupplier<Item> TRANSITION_SEAL = registerItem("transition_seal", 
        () -> new MultiblockBlockItem(ModBlocks.TRANSITION_SEAL.get(), new Item.Properties()));

    public static final RegistrySupplier<Item> SILO_HATCH = registerItem("silo_hatch", 
        () -> new DoorBlockItem(ModBlocks.SILO_HATCH.get(), new Item.Properties()));

    public static final RegistrySupplier<Item> SILO_HATCH_LARGE = registerItem("silo_hatch_large", 
        () -> new DoorBlockItem(ModBlocks.SILO_HATCH_LARGE.get(), new Item.Properties()));

    public static final RegistrySupplier<Item> QE_CONTAINMENT = registerItem("qe_containment_door", 
        () -> new DoorBlockItem(ModBlocks.QE_CONTAINMENT.get(), new Item.Properties()));

    public static final RegistrySupplier<Item> WATER_DOOR = registerItem("water_door", 
        () -> new DoorBlockItem(ModBlocks.WATER_DOOR.get(), new Item.Properties()));

    public static final RegistrySupplier<Item> FIRE_DOOR = registerItem("fire_door", 
        () -> new DoorBlockItem(ModBlocks.FIRE_DOOR.get(), new Item.Properties()));

    public static final RegistrySupplier<Item> SLIDE_DOOR = registerItem("sliding_blast_door", 
        () -> new DoorBlockItem(ModBlocks.SLIDE_DOOR.get(), new Item.Properties()));
        
    public static final RegistrySupplier<Item> SLIDING_SEAL_DOOR = registerItem("sliding_seal_door", 
        () -> new DoorBlockItem(ModBlocks.SLIDING_SEAL_DOOR.get(), new Item.Properties()));

    public static final RegistrySupplier<Item> SECURE_ACCESS_DOOR = registerItem("secure_access_door", 
        () -> new DoorBlockItem(ModBlocks.SECURE_ACCESS_DOOR.get(), new Item.Properties()));

    public static final RegistrySupplier<Item> QE_SLIDING = registerItem("qe_sliding_door", 
        () -> new DoorBlockItem(ModBlocks.QE_SLIDING.get(), new Item.Properties()));

    public static final RegistrySupplier<Item> VAULT_DOOR = registerItem("vault_door", 
        () -> new DoorBlockItem(ModBlocks.VAULT_DOOR.get(), new Item.Properties()));

    public static final RegistrySupplier<Item> CARGO_DOOR = registerItem("cargo_door", 
        () -> new DoorBlockItem(ModBlocks.CARGO_DOOR.get(), new Item.Properties()));



    public static final RegistrySupplier<Item> STAMP_STONE_FLAT = registerItem("stamp_stone_flat", 
            () -> new ItemStamp(new Item.Properties(), 32));
    public static final RegistrySupplier<Item> STAMP_STONE_PLATE = registerItem("stamp_stone_plate", 
            () -> new ItemStamp(new Item.Properties(), 32));
    public static final RegistrySupplier<Item> STAMP_STONE_WIRE = registerItem("stamp_stone_wire", 
            () -> new ItemStamp(new Item.Properties(), 32));
    public static final RegistrySupplier<Item> STAMP_STONE_CIRCUIT = registerItem("stamp_stone_circuit", 
            () -> new ItemStamp(new Item.Properties(), 32));


    public static final RegistrySupplier<Item> BLADE_TEST = registerItem("blade_test", 
            () -> new ItemBlades(new Item.Properties()));

    public static final RegistrySupplier<Item> BLADE_STEEL = registerItem("blade_steel", 
            () -> new ItemBlades(new Item.Properties(), 200));

    public static final RegistrySupplier<Item> BLADE_TITANIUM = registerItem("blade_titanium", 
            () -> new ItemBlades(new Item.Properties(), 350));

    public static final RegistrySupplier<Item> BLADE_ALLOY = registerItem("blade_alloy", 
            () -> new ItemBlades(new Item.Properties(), 700));

    // Р–РµР»РµР·РЅС‹Рµ С€С‚Р°РјРїС‹ (48 РёСЃРїРѕР»СЊР·РѕРІР°РЅРёР№)
    public static final RegistrySupplier<Item> STAMP_IRON_FLAT = registerItem("stamp_iron_flat", 
            () -> new ItemStamp(new Item.Properties(), 48));
    public static final RegistrySupplier<Item> STAMP_IRON_PLATE = registerItem("stamp_iron_plate", 
            () -> new ItemStamp(new Item.Properties(), 48));
    public static final RegistrySupplier<Item> STAMP_IRON_WIRE = registerItem("stamp_iron_wire", 
            () -> new ItemStamp(new Item.Properties(), 48));
    public static final RegistrySupplier<Item> STAMP_IRON_CIRCUIT = registerItem("stamp_iron_circuit", 
            () -> new ItemStamp(new Item.Properties(), 48));
    public static final RegistrySupplier<Item> STAMP_IRON_9 = registerItem("stamp_iron_9", 
            () -> new ItemStamp(new Item.Properties(), 48));
    public static final RegistrySupplier<Item> STAMP_IRON_44 = registerItem("stamp_iron_44", 
            () -> new ItemStamp(new Item.Properties(), 48));
    public static final RegistrySupplier<Item> STAMP_IRON_50 = registerItem("stamp_iron_50", 
            () -> new ItemStamp(new Item.Properties(), 48));
    public static final RegistrySupplier<Item> STAMP_IRON_357 = registerItem("stamp_iron_357", 
            () -> new ItemStamp(new Item.Properties(), 48));

    // РЎС‚Р°Р»СЊРЅС‹Рµ С€С‚Р°РјРїС‹ (64 РёСЃРїРѕР»СЊР·РѕРІР°РЅРёСЏ)
    public static final RegistrySupplier<Item> STAMP_STEEL_FLAT = registerItem("stamp_steel_flat", 
            () -> new ItemStamp(new Item.Properties(), 64));
    public static final RegistrySupplier<Item> STAMP_STEEL_PLATE = registerItem("stamp_steel_plate", 
            () -> new ItemStamp(new Item.Properties(), 64));
    public static final RegistrySupplier<Item> STAMP_STEEL_WIRE = registerItem("stamp_steel_wire", 
            () -> new ItemStamp(new Item.Properties(), 64));
    public static final RegistrySupplier<Item> STAMP_STEEL_CIRCUIT = registerItem("stamp_steel_circuit", 
            () -> new ItemStamp(new Item.Properties(), 64));

    // РўРёС‚Р°РЅРѕРІС‹Рµ С€С‚Р°РјРїС‹ (80 РёСЃРїРѕР»СЊР·РѕРІР°РЅРёР№)
    public static final RegistrySupplier<Item> STAMP_TITANIUM_FLAT = registerItem("stamp_titanium_flat", 
            () -> new ItemStamp(new Item.Properties(), 80));
    public static final RegistrySupplier<Item> STAMP_TITANIUM_PLATE = registerItem("stamp_titanium_plate", 
            () -> new ItemStamp(new Item.Properties(), 80));
    public static final RegistrySupplier<Item> STAMP_TITANIUM_WIRE = registerItem("stamp_titanium_wire", 
            () -> new ItemStamp(new Item.Properties(), 80));
    public static final RegistrySupplier<Item> STAMP_TITANIUM_CIRCUIT = registerItem("stamp_titanium_circuit", 
            () -> new ItemStamp(new Item.Properties(), 80));

    // РћР±СЃРёРґРёР°РЅРѕРІС‹Рµ С€С‚Р°РјРїС‹ (96 РёСЃРїРѕР»СЊР·РѕРІР°РЅРёР№)
    public static final RegistrySupplier<Item> STAMP_OBSIDIAN_FLAT = registerItem("stamp_obsidian_flat", 
            () -> new ItemStamp(new Item.Properties(), 96));
    public static final RegistrySupplier<Item> STAMP_OBSIDIAN_PLATE = registerItem("stamp_obsidian_plate", 
            () -> new ItemStamp(new Item.Properties(), 96));
    public static final RegistrySupplier<Item> STAMP_OBSIDIAN_WIRE = registerItem("stamp_obsidian_wire", 
            () -> new ItemStamp(new Item.Properties(), 96));
    public static final RegistrySupplier<Item> STAMP_OBSIDIAN_CIRCUIT = registerItem("stamp_obsidian_circuit", 
            () -> new ItemStamp(new Item.Properties(), 96));

    // Desh С€С‚Р°РјРїС‹ (Р±РµСЃРєРѕРЅРµС‡РЅР°СЏ РїСЂРѕС‡РЅРѕСЃС‚СЊ)
    public static final RegistrySupplier<Item> STAMP_DESH_FLAT = registerItem("stamp_desh_flat", 
            () -> new ItemStamp(new Item.Properties()));
    public static final RegistrySupplier<Item> STAMP_DESH_PLATE = registerItem("stamp_desh_plate", 
            () -> new ItemStamp(new Item.Properties()));
    public static final RegistrySupplier<Item> STAMP_DESH_WIRE = registerItem("stamp_desh_wire", 
            () -> new ItemStamp(new Item.Properties()));
    public static final RegistrySupplier<Item> STAMP_DESH_CIRCUIT = registerItem("stamp_desh_circuit", 
            () -> new ItemStamp(new Item.Properties()));
    public static final RegistrySupplier<Item> STAMP_DESH_9 = registerItem("stamp_desh_9", 
            () -> new ItemStamp(new Item.Properties()));
    public static final RegistrySupplier<Item> STAMP_DESH_44 = registerItem("stamp_desh_44", 
            () -> new ItemStamp(new Item.Properties()));
    public static final RegistrySupplier<Item> STAMP_DESH_50 = registerItem("stamp_desh_50", 
            () -> new ItemStamp(new Item.Properties()));
    public static final RegistrySupplier<Item> STAMP_DESH_357 = registerItem("stamp_desh_357", 
            () -> new ItemStamp(new Item.Properties()));


    //Р±Р°С‚Р°СЂРµР№РєРё

    public static final RegistrySupplier<Item> BATTERY_SCHRABIDIUM = registerItem("battery_schrabidium", 
            () -> new ModBatteryItem(
                    new Item.Properties(),
                    1000000,
                    5000,
                    5000
            ));

    // ========== РљРђР РўРћР¤Р•Р›Р¬РќРђРЇ Р Р‘РђР—РћР’Р«Р• ==========
    public static final RegistrySupplier<Item> BATTERY_POTATO = registerItem("battery_potato", 
            () -> new ModBatteryItem(
                    new Item.Properties(),
                    1_000,
                    100,
                    100
            ));

    public static final RegistrySupplier<Item> BATTERY = registerItem("battery", 
            () -> new ModBatteryItem(
                    new Item.Properties(),
                    5000,
                    100,
                    100
            ));

    // ========== РљР РђРЎРќР«Р• Р‘РђРўРђР Р•Р™РљР (RED CELL) ==========
    public static final RegistrySupplier<Item> BATTERY_RED_CELL = registerItem("battery_red_cell", 
            () -> new ModBatteryItem(
                    new Item.Properties(),
                    15000,
                    100,
                    100
            ));

    public static final RegistrySupplier<Item> BATTERY_RED_CELL_6 = registerItem("battery_red_cell_6", 
            () -> new ModBatteryItem(
                    new Item.Properties(),
                    90000,
                    100,
                    100
            ));

    public static final RegistrySupplier<Item> BATTERY_RED_CELL_24 = registerItem("battery_red_cell_24", 
            () -> new ModBatteryItem(
                    new Item.Properties(),
                    240000,
                    100,
                    100
            ));

    public static final RegistrySupplier<Item> BATTERY_ADVANCED = registerItem("battery_advanced", 
            () -> new ModBatteryItem(
                    new Item.Properties(),
                    20000,
                    500,
                    500
            ));

    public static final RegistrySupplier<Item> BATTERY_ADVANCED_CELL = registerItem("battery_advanced_cell", 
            () -> new ModBatteryItem(
                    new Item.Properties(),
                    60000,
                    500,
                    500
            ));

    public static final RegistrySupplier<Item> BATTERY_ADVANCED_CELL_4 = registerItem("battery_advanced_cell_4", 
            () -> new ModBatteryItem(
                    new Item.Properties(),
                    240000,
                    500,
                    500
            ));

    public static final RegistrySupplier<Item> BATTERY_ADVANCED_CELL_12 = registerItem("battery_advanced_cell_12", 
            () -> new ModBatteryItem(
                    new Item.Properties(),
                    720000,
                    500,
                    500
            ));

    // ========== Р›РРўРР•Р’Р«Р• Р‘РђРўРђР Р•Р™РљР (LITHIUM) ==========
    public static final RegistrySupplier<Item> BATTERY_LITHIUM = registerItem("battery_lithium", 
            () -> new ModBatteryItem(
                    new Item.Properties(),
                    250000,
                    1000,
                    1000
            ));

    public static final RegistrySupplier<Item> BATTERY_LITHIUM_CELL = registerItem("battery_lithium_cell", 
            () -> new ModBatteryItem(
                    new Item.Properties(),
                    750000,
                    1000,
                    1000
            ));

    public static final RegistrySupplier<Item> BATTERY_LITHIUM_CELL_3 = registerItem("battery_lithium_cell_3", 
            () -> new ModBatteryItem(
                    new Item.Properties(),
                    2250000,
                    1000,
                    1000
            ));

    public static final RegistrySupplier<Item> BATTERY_LITHIUM_CELL_6 = registerItem("battery_lithium_cell_6", 
            () -> new ModBatteryItem(
                    new Item.Properties(),
                    4500000,
                    1000,
                    1000
            ));

// ========== РЁР РђР‘РР”РР•Р’Р«Р• Р‘РђРўРђР Р•Р™РљР (SCHRABIDIUM) - СѓР¶Рµ РµСЃС‚СЊ ==========

    public static final RegistrySupplier<Item> BATTERY_SCHRABIDIUM_CELL = registerItem("battery_schrabidium_cell", 
            () -> new ModBatteryItem(
                    new Item.Properties(),
                    3000000,
                    5000,
                    5000
            ));

    public static final RegistrySupplier<Item> BATTERY_SCHRABIDIUM_CELL_2 = registerItem("battery_schrabidium_cell_2", 
            () -> new ModBatteryItem(
                    new Item.Properties(),
                    6000000,
                    5000,
                    5000
            ));

    public static final RegistrySupplier<Item> BATTERY_SCHRABIDIUM_CELL_4 = registerItem("battery_schrabidium_cell_4", 
            () -> new ModBatteryItem(
                    new Item.Properties(),
                    12000000,
                    5000,
                    5000
            ));

    // ========== РРЎРљР РћР’Р«Р• Р‘РђРўРђР Р•Р™РљР (SPARK) - Р­РљРЎРўР Р•РњРђР›Р¬РќР«Р• ==========
    public static final RegistrySupplier<Item> BATTERY_SPARK = registerItem("battery_spark", 
            () -> new ModBatteryItem(
                    new Item.Properties(),
                    100000000,
                    2000000,
                    2000000
            ));

    public static final RegistrySupplier<Item> BATTERY_TRIXITE = registerItem("battery_trixite", 
            () -> new ModBatteryItem(
                    new Item.Properties(),
                    5000000,
                    40000,
                    200000
            ));

    public static final RegistrySupplier<Item> BATTERY_SPARK_CELL_6 = registerItem("battery_spark_cell_6", 
            () -> new ModBatteryItem(
                    new Item.Properties(),
                    600_000_000L,
                    2000000,
                    2000000
            ));

    public static final RegistrySupplier<Item> BATTERY_SPARK_CELL_25 = registerItem("battery_spark_cell_25", 
            () -> new ModBatteryItem(
                    new Item.Properties(),
                    2_500_000_000L,
                    2000000,
                    2000000
            ));

    public static final RegistrySupplier<Item> BATTERY_SPARK_CELL_100 = registerItem("battery_spark_cell_100", 
            () -> new ModBatteryItem(
                    new Item.Properties(),
                    10_000_000_000L,
                    20000000,
                    2000000
            ));

    public static final RegistrySupplier<Item> BATTERY_SPARK_CELL_1000 = registerItem("battery_spark_cell_1000", 
            () -> new ModBatteryItem(
                    new Item.Properties(),
                    100_000_000_000L,
                    20000000,
                    20000000
            ));

    public static final RegistrySupplier<Item> BATTERY_SPARK_CELL_2500 = registerItem("battery_spark_cell_2500", 
            () -> new ModBatteryItem(
                    new Item.Properties(),
                    250_000_000_000L,
                    20000000,
                    20000000
            ));

    public static final RegistrySupplier<Item> BATTERY_SPARK_CELL_10000 = registerItem("battery_spark_cell_10000", 
            () -> new ModBatteryItem(
                    new Item.Properties(),
                    1_000_000_000_000L,
                    200000000,
                    200000000
            ));

    public static final RegistrySupplier<Item> BATTERY_SPARK_CELL_POWER = registerItem("battery_spark_cell_power",
            () -> new ModBatteryItem(
                    new Item.Properties(),
                    100_000_000_000_000L,
                    200000000,
                    200000000
            ));

    // ========== БОЛЬШИЕ БАТАРЕИ-ПАКИ (бэкпорт оригинала) ==========
    // Вставляются в батарейный сокет и рендерятся там как объёмные тела (Battery/Capacitor).
    public static final RegistrySupplier<Item> BATTERY_PACK_REDSTONE = registerItem("battery_pack_battery_redstone",
            () -> new ItemBatteryPack(new Item.Properties(), EnumBatteryPack.BATTERY_REDSTONE));
    public static final RegistrySupplier<Item> BATTERY_PACK_LEAD = registerItem("battery_pack_battery_lead",
            () -> new ItemBatteryPack(new Item.Properties(), EnumBatteryPack.BATTERY_LEAD));
    public static final RegistrySupplier<Item> BATTERY_PACK_LITHIUM = registerItem("battery_pack_battery_lithium",
            () -> new ItemBatteryPack(new Item.Properties(), EnumBatteryPack.BATTERY_LITHIUM));
    public static final RegistrySupplier<Item> BATTERY_PACK_SODIUM = registerItem("battery_pack_battery_sodium",
            () -> new ItemBatteryPack(new Item.Properties(), EnumBatteryPack.BATTERY_SODIUM));
    public static final RegistrySupplier<Item> BATTERY_PACK_SCHRABIDIUM = registerItem("battery_pack_battery_schrabidium",
            () -> new ItemBatteryPack(new Item.Properties(), EnumBatteryPack.BATTERY_SCHRABIDIUM));
    public static final RegistrySupplier<Item> BATTERY_PACK_QUANTUM = registerItem("battery_pack_battery_quantum",
            () -> new ItemBatteryPack(new Item.Properties(), EnumBatteryPack.BATTERY_QUANTUM));
    public static final RegistrySupplier<Item> BATTERY_PACK_CAPACITOR_COPPER = registerItem("battery_pack_capacitor_copper",
            () -> new ItemBatteryPack(new Item.Properties(), EnumBatteryPack.CAPACITOR_COPPER));
    public static final RegistrySupplier<Item> BATTERY_PACK_CAPACITOR_GOLD = registerItem("battery_pack_capacitor_gold",
            () -> new ItemBatteryPack(new Item.Properties(), EnumBatteryPack.CAPACITOR_GOLD));
    public static final RegistrySupplier<Item> BATTERY_PACK_CAPACITOR_NIOBIUM = registerItem("battery_pack_capacitor_niobium",
            () -> new ItemBatteryPack(new Item.Properties(), EnumBatteryPack.CAPACITOR_NIOBIUM));
    public static final RegistrySupplier<Item> BATTERY_PACK_CAPACITOR_TANTALUM = registerItem("battery_pack_capacitor_tantalum",
            () -> new ItemBatteryPack(new Item.Properties(), EnumBatteryPack.CAPACITOR_TANTALUM));
    public static final RegistrySupplier<Item> BATTERY_PACK_CAPACITOR_BISMUTH = registerItem("battery_pack_capacitor_bismuth",
            () -> new ItemBatteryPack(new Item.Properties(), EnumBatteryPack.CAPACITOR_BISMUTH));
    public static final RegistrySupplier<Item> BATTERY_PACK_CAPACITOR_SPARK = registerItem("battery_pack_capacitor_spark",
            () -> new ItemBatteryPack(new Item.Properties(), EnumBatteryPack.CAPACITOR_SPARK));

    /** Все паки — для циклов рендера/табов/рецептов. */
    public static final List<RegistrySupplier<Item>> BATTERY_PACKS = List.of(
            BATTERY_PACK_REDSTONE, BATTERY_PACK_LEAD, BATTERY_PACK_LITHIUM, BATTERY_PACK_SODIUM,
            BATTERY_PACK_SCHRABIDIUM, BATTERY_PACK_QUANTUM,
            BATTERY_PACK_CAPACITOR_COPPER, BATTERY_PACK_CAPACITOR_GOLD, BATTERY_PACK_CAPACITOR_NIOBIUM,
            BATTERY_PACK_CAPACITOR_TANTALUM, BATTERY_PACK_CAPACITOR_BISMUTH, BATTERY_PACK_CAPACITOR_SPARK);


    public static final RegistrySupplier<Item> AIRSTRIKE_TEST = registerItem("airstrike_test", 
            () -> new AirstrikeItem(new Item.Properties(), AirstrikeType.NORMAL));
    public static final RegistrySupplier<Item> AIRSTRIKE_AGENT= registerItem("airstrike_agent", 
            () -> new AirstrikeItem(new Item.Properties(), AirstrikeType.AGENT));
    public static final RegistrySupplier<Item> AIRSTRIKE_HEAVY = registerItem("airstrike_heavy", 
            () -> new AirstrikeItem(new Item.Properties(), AirstrikeType.HEAVY));
    public static final RegistrySupplier<Item> AIRSTRIKE_NUKE = registerItem("airstrike_nuke", 
            () -> new AirstrikeItem(new Item.Properties(), AirstrikeType.NUKE));
    public static final RegistrySupplier<Item> WIRE_FINE = registerItem("wire_fine", 
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> SAT_HEAD_LASER = registerItem("sat_head_laser", 
            () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> SAT_BASE = registerItem("sat_base", 
            () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> SAT_LASER = registerItem("sat_laser", 
            () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> SAT_HEAD_RADAR = registerItem("sat_head_radar", 
            () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> SAT_RADAR = registerItem("sat_radar", 
            () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> SAT_HEAD_MAPPER = registerItem("sat_head_mapper", 
            () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> SAT_MAPPER = registerItem("sat_mapper", 
            () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> SAT_HEAD_RESONATOR = registerItem("sat_head_resonator", 
            () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> SAT_RESONATOR = registerItem("sat_resonator", 
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> THRUSTER_LARGE         = registerItem("thruster_large", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> FUEL_TANK_LARGE        = registerItem("fuel_tank_large", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> WARHEAD_NUCLEAR        = registerItem("warhead_nuclear", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> MISSILE_ASSEMBLY           = registerItem("missile_assembly", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> INGOT_TUNGSTEN_CARBIDE     = registerItem("ingot_tungsten_carbide", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> INGOT_HIGHSPEED_STEEL      = registerItem("ingot_highspeed_steel", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> NEUTRON_REFLECTOR          = registerItem("neutron_reflector", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> WARHEAD_GENERIC_SMALL      = registerItem("warhead_generic_small", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> WARHEAD_CLUSTER_LARGE      = registerItem("warhead_cluster_large", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> WARHEAD_INCENDIARY_MEDIUM  = registerItem("warhead_incendiary_medium", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> WARHEAD_BUSTER_SMALL       = registerItem("warhead_buster_small", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> THRUSTER_SMALL          = registerItem("thruster_small", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> FUEL_TANK_SMALL         = registerItem("fuel_tank_small", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> WARHEAD_CLUSTER_SMALL      = registerItem("warhead_cluster_small", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> WARHEAD_INCENDIARY_SMALL   = registerItem("warhead_incendiary_small", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> LOW_DENSITY_ELEMENT     = registerItem("low_density_element", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> THRUSTER_MEDIUM         = registerItem("thruster_medium", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> FUEL_TANK_MEDIUM        = registerItem("fuel_tank_medium", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> WARHEAD_GENERIC_MEDIUM  = registerItem("warhead_generic_medium", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> WARHEAD_GENERIC_LARGE   = registerItem("warhead_generic_large", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> WARHEAD_BUSTER_LARGE    = registerItem("warhead_buster_large", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> WARHEAD_MIRV            = registerItem("warhead_mirv", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> WARHEAD_VOLCANO         = registerItem("warhead_volcano", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> WARHEAD_BUSTER_MEDIUM   = registerItem("warhead_buster_medium", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> WARHEAD_CLUSTER_MEDIUM  = registerItem("warhead_cluster_medium", () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> SCREWDRIVER = registerItem("screwdriver", 
            () -> new ScrewdriverItem(new Item.Properties().stacksTo(1)));

	// РњРµРґР»РµРЅРЅС‹Р№ РёСЃС‚РѕС‡РЅРёРє (500 mB/t)
	public static final RegistrySupplier<Item> INFINITE_WATER_500 = registerItem("inf_water", 
					() -> new InfiniteFluidItem(new Item.Properties().stacksTo(1), net.minecraft.world.level.material.Fluids.WATER, 500));

	// Р‘С‹СЃС‚СЂС‹Р№ РёСЃС‚РѕС‡РЅРёРє (5000 mB/t)
	public static final RegistrySupplier<Item> INFINITE_WATER_5000 = registerItem("inf_water_mk2", 
            () -> new InfiniteFluidItem(new Item.Properties().stacksTo(1), net.minecraft.world.level.material.Fluids.WATER, 5000));

    // Fluid Barrel - 16,000 mB capacity portable fluid container
    public static final RegistrySupplier<Item> FLUID_BARREL = registerItem("fluid_barrel", 
            () -> new FluidBarrelItem(new Item.Properties()));

    // Universal infinite fluid source (any fluid type, 1B mB/t - like 1.7.10 fluid_barrel_infinite)
    public static final RegistrySupplier<Item> FLUID_BARREL_INFINITE = registerItem("fluid_barrel_infinite", 
            () -> new InfiniteFluidItem(new Item.Properties().stacksTo(1), 1_000_000_000));


    // Universal fluid identifier - two fluid slots, Shift+RMB opens selection GUI
    public static final RegistrySupplier<Item> FLUID_IDENTIFIER = registerItem("fluid_identifier", 
            () -> new FluidIdentifierItem(new Item.Properties().stacksTo(1)));

    // Mineral Pipes - individual pipe items per mineral, all using pipe.png with color tinting
    public static final RegistrySupplier<Item> PIPE_IRON = registerItem("pipe_iron", 
            () -> new MineralPipeItem(new Item.Properties(), 0xD8D8D8));
    public static final RegistrySupplier<Item> PIPE_COPPER = registerItem("pipe_copper", 
            () -> new MineralPipeItem(new Item.Properties(), 0xE77C56));
    public static final RegistrySupplier<Item> PIPE_GOLD = registerItem("pipe_gold", 
            () -> new MineralPipeItem(new Item.Properties(), 0xFCEE4B));
    public static final RegistrySupplier<Item> PIPE_LEAD = registerItem("pipe_lead", 
            () -> new MineralPipeItem(new Item.Properties(), 0x414166));
    public static final RegistrySupplier<Item> PIPE_STEEL = registerItem("pipe_steel", 
            () -> new MineralPipeItem(new Item.Properties(), 0x767676));
    public static final RegistrySupplier<Item> PIPE_TUNGSTEN = registerItem("pipe_tungsten", 
            () -> new MineralPipeItem(new Item.Properties(), 0x3D3D3D));
    public static final RegistrySupplier<Item> PIPE_TITANIUM = registerItem("pipe_titanium", 
            () -> new MineralPipeItem(new Item.Properties(), 0x8DC5E2));
    public static final RegistrySupplier<Item> PIPE_ALUMINUM = registerItem("pipe_aluminum", 
            () -> new MineralPipeItem(new Item.Properties(), 0xC5C5DE));
    public static final RegistrySupplier<Item> PIPE_DURA_STEEL = registerItem("pipe_dura_steel", 
            () -> new MineralPipeItem(new Item.Properties(), 0x82A59C));

    // Fluid Duct - pipe per fluid type, overlay tinted with fluid color (like fluid barrel)
    public static final RegistrySupplier<Item> FLUID_DUCT = registerItem("fluid_duct", 
            () -> new FluidDuctItem(new Item.Properties(), ModBlocks.FLUID_DUCT,
                    "item.hbm_m.fluid_duct", "item.hbm_m.fluid_duct.empty"));
    public static final RegistrySupplier<Item> FLUID_DUCT_COLORED = registerItem("fluid_duct_colored", 
            () -> new FluidDuctItem(new Item.Properties(), ModBlocks.FLUID_DUCT_COLORED,
                    "item.hbm_m.fluid_duct_colored", "item.hbm_m.fluid_duct_colored.empty"));
    public static final RegistrySupplier<Item> FLUID_DUCT_SILVER = registerItem("fluid_duct_silver", 
            () -> new FluidDuctItem(new Item.Properties(), ModBlocks.FLUID_DUCT_SILVER,
                    "item.hbm_m.fluid_duct_silver", "item.hbm_m.fluid_duct_silver.empty"));

    public static final RegistrySupplier<Item> FLUID_VALVE = registerItem("fluid_valve", 
            () -> new BlockItem(ModBlocks.FLUID_VALVE.get(), new Item.Properties()));
    public static final RegistrySupplier<Item> FLUID_PUMP = registerItem("fluid_pump", 
            () -> new BlockItem(ModBlocks.FLUID_PUMP.get(), new Item.Properties()));
    public static final RegistrySupplier<Item> FLUID_EXHAUST = registerItem("fluid_exhaust", 
            () -> new BlockItem(ModBlocks.FLUID_EXHAUST.get(), new Item.Properties()));

    //=============================== Р’РЃР”Р Рђ Р”Р›РЇ Р–РР”РљРћРЎРўР•Р™ ===============================//

//    public static final RegistrySupplier<Item> CRUDE_OIL_BUCKET = ITEMS.register("bucket_crude_oil",
//            () -> new BucketItem(
//                    () -> ModFluids.CRUDE_OIL.source.get(),
//                    new Item.Properties()
//                            .craftRemainder(Items.BUCKET)
//                            .stacksTo(1)));


    // в”Ђв”Ђв”Ђ RBMK Items в”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђ

    public static final RegistrySupplier<Item> RBMK_LID = registerItem("rbmk_lid", 
            () -> new RBMKLidItem(1, new Item.Properties()));

    public static final RegistrySupplier<Item> RBMK_LID_GLASS = registerItem("rbmk_lid_glass", 
            () -> new RBMKLidItem(2, new Item.Properties()));

    // Pellets - stats mirror the matching rod's, 1:1 with the original's ItemRBMKRod definitions
    // in ModItems.java (see RBMKRodItem's class doc / this port's convention of duplicating stats
    // onto the pellet rather than deriving the rod from it).
    public static final RegistrySupplier<Item> RBMK_PELLET_HEU235 = registerItem("rbmk_pellet_heu235", 
            () -> new RBMKPelletItem(new Item.Properties())
                    .setFullName("High-Enriched Uranium-235").setYield(100_000_000).setReactivity(50)
                    .setMeltingPoint(2865).setTint(0x868D82));

    public static final RegistrySupplier<Item> RBMK_PELLET_LEP = registerItem("rbmk_pellet_lep", 
            () -> new RBMKPelletItem(new Item.Properties())
                    .setFullName("Low-Enriched Plutonium").setYield(100_000_000).setReactivity(35)
                    .setHeat(0.75).setMeltingPoint(2744).setTint(0x656E6B));

    public static final RegistrySupplier<Item> RBMK_PELLET_HEP = registerItem("rbmk_pellet_hep239", 
            () -> new RBMKPelletItem(new Item.Properties())
                    .setFullName("High-Enriched Plutonium-239").setYield(100_000_000).setReactivity(30)
                    .setHeat(1.25).setMeltingPoint(2744).setTint(0x656E6B));

    public static final RegistrySupplier<Item> RBMK_PELLET_MOX = registerItem("rbmk_pellet_mox", 
            () -> new RBMKPelletItem(new Item.Properties())
                    .setFullName("Mixed MEU & LEP Oxide").setYield(100_000_000).setReactivity(40)
                    .setMeltingPoint(2815).setTint(0x868D82));

    // Fuel Rods (assembled from pellets) - 1:1 port of the original's ItemRBMKRod stat blocks.
    public static final RegistrySupplier<Item> RBMK_FUEL_HEU235 = registerItem("rbmk_fuel_heu235", 
            () -> new RBMKRodItem("High-Enriched Uranium-235 Rod", new Item.Properties())
                    .setYield(100_000_000).setStats(50).setFunction(RBMKRodItem.EnumBurnFunc.SQUARE_ROOT)
                    .setMeltingPoint(2865).setTint(0x868D82).setPellet(() -> ModItems.RBMK_PELLET_HEU235.get()));

    public static final RegistrySupplier<Item> RBMK_FUEL_LEP = registerItem("rbmk_fuel_lep", 
            () -> new RBMKRodItem("Low-Enriched Plutonium Rod", new Item.Properties())
                    .setYield(100_000_000).setStats(35).setFunction(RBMKRodItem.EnumBurnFunc.LOG_TEN)
                    .setDepletionFunction(RBMKRodItem.EnumDepleteFunc.RAISING_SLOPE)
                    .setHeat(0.75).setMeltingPoint(2744).setTint(0x656E6B).setPellet(() -> ModItems.RBMK_PELLET_LEP.get()));

    public static final RegistrySupplier<Item> RBMK_FUEL_HEP = registerItem("rbmk_fuel_hep", 
            () -> new RBMKRodItem("High-Enriched Plutonium-239 Rod", new Item.Properties())
                    .setYield(100_000_000).setStats(30).setFunction(RBMKRodItem.EnumBurnFunc.LINEAR)
                    .setHeat(1.25).setMeltingPoint(2744).setTint(0x656E6B).setPellet(() -> ModItems.RBMK_PELLET_HEP.get()));

    public static final RegistrySupplier<Item> RBMK_FUEL_MOX = registerItem("rbmk_fuel_mox", 
            () -> new RBMKRodItem("Mixed Oxide Fuel Rod", new Item.Properties())
                    .setYield(100_000_000).setStats(40).setFunction(RBMKRodItem.EnumBurnFunc.LOG_TEN)
                    .setDepletionFunction(RBMKRodItem.EnumDepleteFunc.RAISING_SLOPE)
                    .setMeltingPoint(2815).setTint(0x868D82).setPellet(() -> ModItems.RBMK_PELLET_MOX.get()));

    public static final RegistrySupplier<Item> RBMK_FUEL_EMPTY = registerItem("rbmk_fuel_empty", 
            () -> new Item(new Item.Properties()));

    // в•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђ
    // DEV: importierte fehlende Items aus dem Original-HBM (zur Sichtung)
    // в•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђв•ђ
    public static final RegistrySupplier<Item> ACETYLENE_TORCH = registerItem("acetylene_torch", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> AJR_LEGS = registerItem("ajr_legs", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> AJR_PLATE = registerItem("ajr_plate", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> AJRO_LEGS = registerItem("ajro_legs", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> AJRO_PLATE = registerItem("ajro_plate", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> ALLOY_LEGS = registerItem("alloy_legs", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> ALLOY_PLATE = registerItem("alloy_plate", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> AMMO_ARTY = registerItem("ammo_arty", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> AMMO_ARTY_CARGO = registerItem("ammo_arty_cargo", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> AMMO_ARTY_CHLORINE = registerItem("ammo_arty_chlorine", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> AMMO_ARTY_CLASSIC = registerItem("ammo_arty_classic", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> AMMO_ARTY_HE = registerItem("ammo_arty_he", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> AMMO_ARTY_MINI_NUKE = registerItem("ammo_arty_mini_nuke", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> AMMO_ARTY_MINI_NUKE_MULTI = registerItem("ammo_arty_mini_nuke_multi", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> AMMO_ARTY_MUSTARD_GAS = registerItem("ammo_arty_mustard_gas", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> AMMO_ARTY_NUKE = registerItem("ammo_arty_nuke", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> AMMO_ARTY_PHOSGENE = registerItem("ammo_arty_phosgene", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> AMMO_ARTY_PHOSPHORUS = registerItem("ammo_arty_phosphorus", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> AMMO_ARTY_PHOSPHORUS_MULTI = registerItem("ammo_arty_phosphorus_multi", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> AMMO_BAG = registerItem("ammo_bag", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> AMMO_BAG_INFINITE = registerItem("ammo_bag_infinite", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> AMMO_CONTAINER = registerItem("ammo_container", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> AMMO_DGK = registerItem("ammo_dgk", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> AMMO_FIREEXT = registerItem("ammo_fireext", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> AMMO_FIREEXT_FOAM = registerItem("ammo_fireext_foam", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> AMMO_FIREEXT_SAND = registerItem("ammo_fireext_sand", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> AMMO_SHELL = registerItem("ammo_shell", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> AMMO_SHELL_APFSDS_DU = registerItem("ammo_shell_apfsds_du", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> AMMO_SHELL_APFSDS_T = registerItem("ammo_shell_apfsds_t", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> AMMO_SHELL_EXPLOSIVE = registerItem("ammo_shell_explosive", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> AMMO_SHELL_W9 = registerItem("ammo_shell_w9", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> AMS_CATALYST_ALUMINIUM = registerItem("ams_catalyst_aluminium", 
            () -> new com.hbm_m.item.machine.ItemAMSCatalyst(new Item.Properties(),
                    0xCCCCCC, 1_000_000L, 1.15F, 0.85F, 1.15F));
    public static final RegistrySupplier<Item> AMS_CATALYST_BERYLLIUM = registerItem("ams_catalyst_beryllium", 
            () -> new com.hbm_m.item.machine.ItemAMSCatalyst(new Item.Properties(),
                    0x97978B, 0L, 1.25F, 0.95F, 1.05F));
    public static final RegistrySupplier<Item> AMS_CATALYST_BLANK = registerItem("ams_catalyst_blank", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> AMS_CATALYST_CAESIUM = registerItem("ams_catalyst_caesium", 
            () -> new com.hbm_m.item.machine.ItemAMSCatalyst(new Item.Properties(),
                    0x6400FF, 2_500_000L, 1.00F, 0.85F, 1.15F));
    public static final RegistrySupplier<Item> AMS_CATALYST_CERIUM = registerItem("ams_catalyst_cerium", 
            () -> new com.hbm_m.item.machine.ItemAMSCatalyst(new Item.Properties(),
                    0x1D3FFF, 1_000_000L, 1.15F, 1.15F, 0.85F));
    public static final RegistrySupplier<Item> AMS_CATALYST_COBALT = registerItem("ams_catalyst_cobalt", 
            () -> new com.hbm_m.item.machine.ItemAMSCatalyst(new Item.Properties(),
                    0x789BBE, 0L, 1.25F, 1.05F, 0.95F));
    public static final RegistrySupplier<Item> AMS_CATALYST_COPPER = registerItem("ams_catalyst_copper", 
            () -> new com.hbm_m.item.machine.ItemAMSCatalyst(new Item.Properties(),
                    0xAADE29, 0L, 1.25F, 1.00F, 1.00F));
    public static final RegistrySupplier<Item> AMS_CATALYST_DINEUTRONIUM = registerItem("ams_catalyst_dineutronium", 
            () -> new com.hbm_m.item.machine.ItemAMSCatalyst(new Item.Properties(),
                    0x334077, 2_500_000L, 1.00F, 1.15F, 0.85F));
    public static final RegistrySupplier<Item> AMS_CATALYST_EUPHEMIUM = registerItem("ams_catalyst_euphemium", 
            () -> new com.hbm_m.item.machine.ItemAMSCatalyst(new Item.Properties(),
                    0xFF9CD2, 2_500_000L, 1.00F, 1.00F, 1.00F));
    public static final RegistrySupplier<Item> AMS_CATALYST_IRON = registerItem("ams_catalyst_iron", 
            () -> new com.hbm_m.item.machine.ItemAMSCatalyst(new Item.Properties(),
                    0xFF7E22, 1_000_000L, 1.15F, 0.95F, 1.05F));
    public static final RegistrySupplier<Item> AMS_CATALYST_LITHIUM = registerItem("ams_catalyst_lithium", 
            () -> new com.hbm_m.item.machine.ItemAMSCatalyst(new Item.Properties(),
                    0xFF2727, 0L, 1.25F, 0.85F, 1.15F));
    public static final RegistrySupplier<Item> AMS_CATALYST_NIOBIUM = registerItem("ams_catalyst_niobium", 
            () -> new com.hbm_m.item.machine.ItemAMSCatalyst(new Item.Properties(),
                    0x3BF1B6, 1_000_000L, 1.15F, 1.05F, 0.95F));
    public static final RegistrySupplier<Item> AMS_CATALYST_SCHRABIDIUM = registerItem("ams_catalyst_schrabidium", 
            () -> new com.hbm_m.item.machine.ItemAMSCatalyst(new Item.Properties(),
                    0x32FFFF, 2_500_000L, 1.00F, 1.05F, 0.95F));
    public static final RegistrySupplier<Item> AMS_CATALYST_STRONTIUM = registerItem("ams_catalyst_strontium", 
            () -> new com.hbm_m.item.machine.ItemAMSCatalyst(new Item.Properties(),
                    0xDD0D35, 1_000_000L, 1.15F, 1.00F, 1.00F));
    public static final RegistrySupplier<Item> AMS_CATALYST_THORIUM = registerItem("ams_catalyst_thorium", 
            () -> new com.hbm_m.item.machine.ItemAMSCatalyst(new Item.Properties(),
                    0x653B22, 2_500_000L, 1.00F, 0.95F, 1.05F));
    public static final RegistrySupplier<Item> AMS_CATALYST_TUNGSTEN = registerItem("ams_catalyst_tungsten", 
            () -> new com.hbm_m.item.machine.ItemAMSCatalyst(new Item.Properties(),
                    0xF5FF48, 0L, 1.25F, 1.15F, 0.85F));
    public static final RegistrySupplier<Item> AMS_CORE_EYEOFHARMONY = registerItem("ams_core_eyeofharmony", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> AMS_CORE_SING = registerItem("ams_core_sing", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> AMS_CORE_THINGY = registerItem("ams_core_thingy", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> AMS_CORE_WORMHOLE = registerItem("ams_core_wormhole", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> AMS_LENS = registerItem("ams_lens", 
            () -> new com.hbm_m.item.machine.ItemAMSLens(new Item.Properties(),
                    com.hbm_m.item.machine.ItemAMSLens.DEFAULT_MAX_DAMAGE));
    public static final RegistrySupplier<Item> ANALYSIS_TOOL = registerItem("analysis_tool", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> ANALYZER = registerItem("analyzer", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> ANCHOR_REMOTE = registerItem("anchor_remote", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> APPLE_EUPHEMIUM = registerItem("apple_euphemium", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> APPLE_LEAD = registerItem("apple_lead", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> APPLE_SCHRABIDIUM = registerItem("apple_schrabidium", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> ARC_ELECTRODE = registerItem("arc_electrode", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> ARMOR_POLISH = registerItem("armor_polish", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> ASBESTOS_LEGS = registerItem("asbestos_legs", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> ASBESTOS_PLATE = registerItem("asbestos_plate", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> ASHGLASSES = registerItem("ashglasses", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> ASSEMBLY_NUKE = registerItem("assembly_nuke", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> ATTACHMENT_MASK = registerItem("attachment_mask", () -> new ItemModGasmask(new Item.Properties(), false));
    public static final RegistrySupplier<Item> ATTACHMENT_MASK_MONO = registerItem("attachment_mask_mono", () -> new ItemModGasmask(new Item.Properties(), true));
    public static final RegistrySupplier<Item> AUSTRALIUM_III = registerItem("australium_iii", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BACK_TESLA = registerItem("back_tesla", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BALEFIRE_AND_HAM = registerItem("balefire_and_ham", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BALEFIRE_AND_STEEL = registerItem("balefire_and_steel", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BALEFIRE_SCRAMBLED = registerItem("balefire_scrambled", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BALL_DYNAMITE = registerItem("ball_dynamite", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BALL_FIRECLAY = registerItem("ball_fireclay", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BALL_RESIN = registerItem("ball_resin", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BALL_TATB = registerItem("ball_tatb", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BALLISTIC_GAUNTLET = registerItem("ballistic_gauntlet", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BALLISTITE = registerItem("ballistite", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BANDAID = registerItem("bandaid", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BATHWATER = registerItem("bathwater", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BATHWATER_MK2 = registerItem("bathwater_mk2", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BDCL = registerItem("bdcl", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BEDROCK_ORE_FRAGMENT = registerItem("bedrock_ore_fragment", () -> new Item(new Item.Properties()));
    // в•ђв•ђв•ђ Bedrock Ore Progression: Rohprodukt + alle Veredelungsstufen (Grade x Type) в•ђв•ђв•ђ
    // Grade-Namen/Traits 1:1 aus ItemBedrockOreNew.BedrockOreGrade (Original-Repo) uebernommen.
    public static final RegistrySupplier<Item> BEDROCK_ORE_BASE = registerItem("bedrock_ore_base", () -> new com.hbm_m.item.industrial.ItemBedrockOreBase(new Item.Properties()));
    public static final RegistrySupplier<Item> BEDROCK_ORE_BASE_LIGHT = registerItem("bedrock_ore_base_light", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.BASE, com.hbm_m.worldgen.BedrockOreDensity.Type.LIGHT));
    public static final RegistrySupplier<Item> BEDROCK_ORE_BASE_HEAVY = registerItem("bedrock_ore_base_heavy", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.BASE, com.hbm_m.worldgen.BedrockOreDensity.Type.HEAVY));
    public static final RegistrySupplier<Item> BEDROCK_ORE_BASE_RARE = registerItem("bedrock_ore_base_rare", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.BASE, com.hbm_m.worldgen.BedrockOreDensity.Type.RARE));
    public static final RegistrySupplier<Item> BEDROCK_ORE_BASE_ACTINIDE = registerItem("bedrock_ore_base_actinide", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.BASE, com.hbm_m.worldgen.BedrockOreDensity.Type.ACTINIDE));
    public static final RegistrySupplier<Item> BEDROCK_ORE_BASE_NONMETAL = registerItem("bedrock_ore_base_nonmetal", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.BASE, com.hbm_m.worldgen.BedrockOreDensity.Type.NONMETAL));
    public static final RegistrySupplier<Item> BEDROCK_ORE_BASE_CRYSTAL = registerItem("bedrock_ore_base_crystal", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.BASE, com.hbm_m.worldgen.BedrockOreDensity.Type.CRYSTAL));
    public static final RegistrySupplier<Item> BEDROCK_ORE_BASE_ROASTED_LIGHT = registerItem("bedrock_ore_base_roasted_light", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.BASE_ROASTED, com.hbm_m.worldgen.BedrockOreDensity.Type.LIGHT));
    public static final RegistrySupplier<Item> BEDROCK_ORE_BASE_ROASTED_HEAVY = registerItem("bedrock_ore_base_roasted_heavy", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.BASE_ROASTED, com.hbm_m.worldgen.BedrockOreDensity.Type.HEAVY));
    public static final RegistrySupplier<Item> BEDROCK_ORE_BASE_ROASTED_RARE = registerItem("bedrock_ore_base_roasted_rare", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.BASE_ROASTED, com.hbm_m.worldgen.BedrockOreDensity.Type.RARE));
    public static final RegistrySupplier<Item> BEDROCK_ORE_BASE_ROASTED_ACTINIDE = registerItem("bedrock_ore_base_roasted_actinide", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.BASE_ROASTED, com.hbm_m.worldgen.BedrockOreDensity.Type.ACTINIDE));
    public static final RegistrySupplier<Item> BEDROCK_ORE_BASE_ROASTED_NONMETAL = registerItem("bedrock_ore_base_roasted_nonmetal", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.BASE_ROASTED, com.hbm_m.worldgen.BedrockOreDensity.Type.NONMETAL));
    public static final RegistrySupplier<Item> BEDROCK_ORE_BASE_ROASTED_CRYSTAL = registerItem("bedrock_ore_base_roasted_crystal", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.BASE_ROASTED, com.hbm_m.worldgen.BedrockOreDensity.Type.CRYSTAL));
    public static final RegistrySupplier<Item> BEDROCK_ORE_BASE_WASHED_LIGHT = registerItem("bedrock_ore_base_washed_light", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.BASE_WASHED, com.hbm_m.worldgen.BedrockOreDensity.Type.LIGHT));
    public static final RegistrySupplier<Item> BEDROCK_ORE_BASE_WASHED_HEAVY = registerItem("bedrock_ore_base_washed_heavy", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.BASE_WASHED, com.hbm_m.worldgen.BedrockOreDensity.Type.HEAVY));
    public static final RegistrySupplier<Item> BEDROCK_ORE_BASE_WASHED_RARE = registerItem("bedrock_ore_base_washed_rare", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.BASE_WASHED, com.hbm_m.worldgen.BedrockOreDensity.Type.RARE));
    public static final RegistrySupplier<Item> BEDROCK_ORE_BASE_WASHED_ACTINIDE = registerItem("bedrock_ore_base_washed_actinide", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.BASE_WASHED, com.hbm_m.worldgen.BedrockOreDensity.Type.ACTINIDE));
    public static final RegistrySupplier<Item> BEDROCK_ORE_BASE_WASHED_NONMETAL = registerItem("bedrock_ore_base_washed_nonmetal", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.BASE_WASHED, com.hbm_m.worldgen.BedrockOreDensity.Type.NONMETAL));
    public static final RegistrySupplier<Item> BEDROCK_ORE_BASE_WASHED_CRYSTAL = registerItem("bedrock_ore_base_washed_crystal", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.BASE_WASHED, com.hbm_m.worldgen.BedrockOreDensity.Type.CRYSTAL));
    public static final RegistrySupplier<Item> BEDROCK_ORE_PRIMARY_LIGHT = registerItem("bedrock_ore_primary_light", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.PRIMARY, com.hbm_m.worldgen.BedrockOreDensity.Type.LIGHT));
    public static final RegistrySupplier<Item> BEDROCK_ORE_PRIMARY_HEAVY = registerItem("bedrock_ore_primary_heavy", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.PRIMARY, com.hbm_m.worldgen.BedrockOreDensity.Type.HEAVY));
    public static final RegistrySupplier<Item> BEDROCK_ORE_PRIMARY_RARE = registerItem("bedrock_ore_primary_rare", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.PRIMARY, com.hbm_m.worldgen.BedrockOreDensity.Type.RARE));
    public static final RegistrySupplier<Item> BEDROCK_ORE_PRIMARY_ACTINIDE = registerItem("bedrock_ore_primary_actinide", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.PRIMARY, com.hbm_m.worldgen.BedrockOreDensity.Type.ACTINIDE));
    public static final RegistrySupplier<Item> BEDROCK_ORE_PRIMARY_NONMETAL = registerItem("bedrock_ore_primary_nonmetal", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.PRIMARY, com.hbm_m.worldgen.BedrockOreDensity.Type.NONMETAL));
    public static final RegistrySupplier<Item> BEDROCK_ORE_PRIMARY_CRYSTAL = registerItem("bedrock_ore_primary_crystal", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.PRIMARY, com.hbm_m.worldgen.BedrockOreDensity.Type.CRYSTAL));
    public static final RegistrySupplier<Item> BEDROCK_ORE_PRIMARY_ROASTED_LIGHT = registerItem("bedrock_ore_primary_roasted_light", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.PRIMARY_ROASTED, com.hbm_m.worldgen.BedrockOreDensity.Type.LIGHT));
    public static final RegistrySupplier<Item> BEDROCK_ORE_PRIMARY_ROASTED_HEAVY = registerItem("bedrock_ore_primary_roasted_heavy", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.PRIMARY_ROASTED, com.hbm_m.worldgen.BedrockOreDensity.Type.HEAVY));
    public static final RegistrySupplier<Item> BEDROCK_ORE_PRIMARY_ROASTED_RARE = registerItem("bedrock_ore_primary_roasted_rare", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.PRIMARY_ROASTED, com.hbm_m.worldgen.BedrockOreDensity.Type.RARE));
    public static final RegistrySupplier<Item> BEDROCK_ORE_PRIMARY_ROASTED_ACTINIDE = registerItem("bedrock_ore_primary_roasted_actinide", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.PRIMARY_ROASTED, com.hbm_m.worldgen.BedrockOreDensity.Type.ACTINIDE));
    public static final RegistrySupplier<Item> BEDROCK_ORE_PRIMARY_ROASTED_NONMETAL = registerItem("bedrock_ore_primary_roasted_nonmetal", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.PRIMARY_ROASTED, com.hbm_m.worldgen.BedrockOreDensity.Type.NONMETAL));
    public static final RegistrySupplier<Item> BEDROCK_ORE_PRIMARY_ROASTED_CRYSTAL = registerItem("bedrock_ore_primary_roasted_crystal", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.PRIMARY_ROASTED, com.hbm_m.worldgen.BedrockOreDensity.Type.CRYSTAL));
    public static final RegistrySupplier<Item> BEDROCK_ORE_PRIMARY_SULFURIC_LIGHT = registerItem("bedrock_ore_primary_sulfuric_light", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.PRIMARY_SULFURIC, com.hbm_m.worldgen.BedrockOreDensity.Type.LIGHT));
    public static final RegistrySupplier<Item> BEDROCK_ORE_PRIMARY_SULFURIC_HEAVY = registerItem("bedrock_ore_primary_sulfuric_heavy", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.PRIMARY_SULFURIC, com.hbm_m.worldgen.BedrockOreDensity.Type.HEAVY));
    public static final RegistrySupplier<Item> BEDROCK_ORE_PRIMARY_SULFURIC_RARE = registerItem("bedrock_ore_primary_sulfuric_rare", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.PRIMARY_SULFURIC, com.hbm_m.worldgen.BedrockOreDensity.Type.RARE));
    public static final RegistrySupplier<Item> BEDROCK_ORE_PRIMARY_SULFURIC_ACTINIDE = registerItem("bedrock_ore_primary_sulfuric_actinide", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.PRIMARY_SULFURIC, com.hbm_m.worldgen.BedrockOreDensity.Type.ACTINIDE));
    public static final RegistrySupplier<Item> BEDROCK_ORE_PRIMARY_SULFURIC_NONMETAL = registerItem("bedrock_ore_primary_sulfuric_nonmetal", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.PRIMARY_SULFURIC, com.hbm_m.worldgen.BedrockOreDensity.Type.NONMETAL));
    public static final RegistrySupplier<Item> BEDROCK_ORE_PRIMARY_SULFURIC_CRYSTAL = registerItem("bedrock_ore_primary_sulfuric_crystal", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.PRIMARY_SULFURIC, com.hbm_m.worldgen.BedrockOreDensity.Type.CRYSTAL));
    public static final RegistrySupplier<Item> BEDROCK_ORE_PRIMARY_NOSULFURIC_LIGHT = registerItem("bedrock_ore_primary_nosulfuric_light", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.PRIMARY_NOSULFURIC, com.hbm_m.worldgen.BedrockOreDensity.Type.LIGHT));
    public static final RegistrySupplier<Item> BEDROCK_ORE_PRIMARY_NOSULFURIC_HEAVY = registerItem("bedrock_ore_primary_nosulfuric_heavy", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.PRIMARY_NOSULFURIC, com.hbm_m.worldgen.BedrockOreDensity.Type.HEAVY));
    public static final RegistrySupplier<Item> BEDROCK_ORE_PRIMARY_NOSULFURIC_RARE = registerItem("bedrock_ore_primary_nosulfuric_rare", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.PRIMARY_NOSULFURIC, com.hbm_m.worldgen.BedrockOreDensity.Type.RARE));
    public static final RegistrySupplier<Item> BEDROCK_ORE_PRIMARY_NOSULFURIC_ACTINIDE = registerItem("bedrock_ore_primary_nosulfuric_actinide", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.PRIMARY_NOSULFURIC, com.hbm_m.worldgen.BedrockOreDensity.Type.ACTINIDE));
    public static final RegistrySupplier<Item> BEDROCK_ORE_PRIMARY_NOSULFURIC_NONMETAL = registerItem("bedrock_ore_primary_nosulfuric_nonmetal", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.PRIMARY_NOSULFURIC, com.hbm_m.worldgen.BedrockOreDensity.Type.NONMETAL));
    public static final RegistrySupplier<Item> BEDROCK_ORE_PRIMARY_NOSULFURIC_CRYSTAL = registerItem("bedrock_ore_primary_nosulfuric_crystal", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.PRIMARY_NOSULFURIC, com.hbm_m.worldgen.BedrockOreDensity.Type.CRYSTAL));
    public static final RegistrySupplier<Item> BEDROCK_ORE_PRIMARY_SOLVENT_LIGHT = registerItem("bedrock_ore_primary_solvent_light", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.PRIMARY_SOLVENT, com.hbm_m.worldgen.BedrockOreDensity.Type.LIGHT));
    public static final RegistrySupplier<Item> BEDROCK_ORE_PRIMARY_SOLVENT_HEAVY = registerItem("bedrock_ore_primary_solvent_heavy", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.PRIMARY_SOLVENT, com.hbm_m.worldgen.BedrockOreDensity.Type.HEAVY));
    public static final RegistrySupplier<Item> BEDROCK_ORE_PRIMARY_SOLVENT_RARE = registerItem("bedrock_ore_primary_solvent_rare", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.PRIMARY_SOLVENT, com.hbm_m.worldgen.BedrockOreDensity.Type.RARE));
    public static final RegistrySupplier<Item> BEDROCK_ORE_PRIMARY_SOLVENT_ACTINIDE = registerItem("bedrock_ore_primary_solvent_actinide", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.PRIMARY_SOLVENT, com.hbm_m.worldgen.BedrockOreDensity.Type.ACTINIDE));
    public static final RegistrySupplier<Item> BEDROCK_ORE_PRIMARY_SOLVENT_NONMETAL = registerItem("bedrock_ore_primary_solvent_nonmetal", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.PRIMARY_SOLVENT, com.hbm_m.worldgen.BedrockOreDensity.Type.NONMETAL));
    public static final RegistrySupplier<Item> BEDROCK_ORE_PRIMARY_SOLVENT_CRYSTAL = registerItem("bedrock_ore_primary_solvent_crystal", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.PRIMARY_SOLVENT, com.hbm_m.worldgen.BedrockOreDensity.Type.CRYSTAL));
    public static final RegistrySupplier<Item> BEDROCK_ORE_PRIMARY_NOSOLVENT_LIGHT = registerItem("bedrock_ore_primary_nosolvent_light", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.PRIMARY_NOSOLVENT, com.hbm_m.worldgen.BedrockOreDensity.Type.LIGHT));
    public static final RegistrySupplier<Item> BEDROCK_ORE_PRIMARY_NOSOLVENT_HEAVY = registerItem("bedrock_ore_primary_nosolvent_heavy", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.PRIMARY_NOSOLVENT, com.hbm_m.worldgen.BedrockOreDensity.Type.HEAVY));
    public static final RegistrySupplier<Item> BEDROCK_ORE_PRIMARY_NOSOLVENT_RARE = registerItem("bedrock_ore_primary_nosolvent_rare", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.PRIMARY_NOSOLVENT, com.hbm_m.worldgen.BedrockOreDensity.Type.RARE));
    public static final RegistrySupplier<Item> BEDROCK_ORE_PRIMARY_NOSOLVENT_ACTINIDE = registerItem("bedrock_ore_primary_nosolvent_actinide", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.PRIMARY_NOSOLVENT, com.hbm_m.worldgen.BedrockOreDensity.Type.ACTINIDE));
    public static final RegistrySupplier<Item> BEDROCK_ORE_PRIMARY_NOSOLVENT_NONMETAL = registerItem("bedrock_ore_primary_nosolvent_nonmetal", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.PRIMARY_NOSOLVENT, com.hbm_m.worldgen.BedrockOreDensity.Type.NONMETAL));
    public static final RegistrySupplier<Item> BEDROCK_ORE_PRIMARY_NOSOLVENT_CRYSTAL = registerItem("bedrock_ore_primary_nosolvent_crystal", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.PRIMARY_NOSOLVENT, com.hbm_m.worldgen.BedrockOreDensity.Type.CRYSTAL));
    public static final RegistrySupplier<Item> BEDROCK_ORE_PRIMARY_RAD_LIGHT = registerItem("bedrock_ore_primary_rad_light", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.PRIMARY_RAD, com.hbm_m.worldgen.BedrockOreDensity.Type.LIGHT));
    public static final RegistrySupplier<Item> BEDROCK_ORE_PRIMARY_RAD_HEAVY = registerItem("bedrock_ore_primary_rad_heavy", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.PRIMARY_RAD, com.hbm_m.worldgen.BedrockOreDensity.Type.HEAVY));
    public static final RegistrySupplier<Item> BEDROCK_ORE_PRIMARY_RAD_RARE = registerItem("bedrock_ore_primary_rad_rare", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.PRIMARY_RAD, com.hbm_m.worldgen.BedrockOreDensity.Type.RARE));
    public static final RegistrySupplier<Item> BEDROCK_ORE_PRIMARY_RAD_ACTINIDE = registerItem("bedrock_ore_primary_rad_actinide", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.PRIMARY_RAD, com.hbm_m.worldgen.BedrockOreDensity.Type.ACTINIDE));
    public static final RegistrySupplier<Item> BEDROCK_ORE_PRIMARY_RAD_NONMETAL = registerItem("bedrock_ore_primary_rad_nonmetal", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.PRIMARY_RAD, com.hbm_m.worldgen.BedrockOreDensity.Type.NONMETAL));
    public static final RegistrySupplier<Item> BEDROCK_ORE_PRIMARY_RAD_CRYSTAL = registerItem("bedrock_ore_primary_rad_crystal", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.PRIMARY_RAD, com.hbm_m.worldgen.BedrockOreDensity.Type.CRYSTAL));
    public static final RegistrySupplier<Item> BEDROCK_ORE_PRIMARY_NORAD_LIGHT = registerItem("bedrock_ore_primary_norad_light", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.PRIMARY_NORAD, com.hbm_m.worldgen.BedrockOreDensity.Type.LIGHT));
    public static final RegistrySupplier<Item> BEDROCK_ORE_PRIMARY_NORAD_HEAVY = registerItem("bedrock_ore_primary_norad_heavy", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.PRIMARY_NORAD, com.hbm_m.worldgen.BedrockOreDensity.Type.HEAVY));
    public static final RegistrySupplier<Item> BEDROCK_ORE_PRIMARY_NORAD_RARE = registerItem("bedrock_ore_primary_norad_rare", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.PRIMARY_NORAD, com.hbm_m.worldgen.BedrockOreDensity.Type.RARE));
    public static final RegistrySupplier<Item> BEDROCK_ORE_PRIMARY_NORAD_ACTINIDE = registerItem("bedrock_ore_primary_norad_actinide", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.PRIMARY_NORAD, com.hbm_m.worldgen.BedrockOreDensity.Type.ACTINIDE));
    public static final RegistrySupplier<Item> BEDROCK_ORE_PRIMARY_NORAD_NONMETAL = registerItem("bedrock_ore_primary_norad_nonmetal", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.PRIMARY_NORAD, com.hbm_m.worldgen.BedrockOreDensity.Type.NONMETAL));
    public static final RegistrySupplier<Item> BEDROCK_ORE_PRIMARY_NORAD_CRYSTAL = registerItem("bedrock_ore_primary_norad_crystal", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.PRIMARY_NORAD, com.hbm_m.worldgen.BedrockOreDensity.Type.CRYSTAL));
    public static final RegistrySupplier<Item> BEDROCK_ORE_PRIMARY_FIRST_LIGHT = registerItem("bedrock_ore_primary_first_light", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.PRIMARY_FIRST, com.hbm_m.worldgen.BedrockOreDensity.Type.LIGHT));
    public static final RegistrySupplier<Item> BEDROCK_ORE_PRIMARY_FIRST_HEAVY = registerItem("bedrock_ore_primary_first_heavy", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.PRIMARY_FIRST, com.hbm_m.worldgen.BedrockOreDensity.Type.HEAVY));
    public static final RegistrySupplier<Item> BEDROCK_ORE_PRIMARY_FIRST_RARE = registerItem("bedrock_ore_primary_first_rare", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.PRIMARY_FIRST, com.hbm_m.worldgen.BedrockOreDensity.Type.RARE));
    public static final RegistrySupplier<Item> BEDROCK_ORE_PRIMARY_FIRST_ACTINIDE = registerItem("bedrock_ore_primary_first_actinide", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.PRIMARY_FIRST, com.hbm_m.worldgen.BedrockOreDensity.Type.ACTINIDE));
    public static final RegistrySupplier<Item> BEDROCK_ORE_PRIMARY_FIRST_NONMETAL = registerItem("bedrock_ore_primary_first_nonmetal", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.PRIMARY_FIRST, com.hbm_m.worldgen.BedrockOreDensity.Type.NONMETAL));
    public static final RegistrySupplier<Item> BEDROCK_ORE_PRIMARY_FIRST_CRYSTAL = registerItem("bedrock_ore_primary_first_crystal", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.PRIMARY_FIRST, com.hbm_m.worldgen.BedrockOreDensity.Type.CRYSTAL));
    public static final RegistrySupplier<Item> BEDROCK_ORE_PRIMARY_SECOND_LIGHT = registerItem("bedrock_ore_primary_second_light", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.PRIMARY_SECOND, com.hbm_m.worldgen.BedrockOreDensity.Type.LIGHT));
    public static final RegistrySupplier<Item> BEDROCK_ORE_PRIMARY_SECOND_HEAVY = registerItem("bedrock_ore_primary_second_heavy", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.PRIMARY_SECOND, com.hbm_m.worldgen.BedrockOreDensity.Type.HEAVY));
    public static final RegistrySupplier<Item> BEDROCK_ORE_PRIMARY_SECOND_RARE = registerItem("bedrock_ore_primary_second_rare", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.PRIMARY_SECOND, com.hbm_m.worldgen.BedrockOreDensity.Type.RARE));
    public static final RegistrySupplier<Item> BEDROCK_ORE_PRIMARY_SECOND_ACTINIDE = registerItem("bedrock_ore_primary_second_actinide", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.PRIMARY_SECOND, com.hbm_m.worldgen.BedrockOreDensity.Type.ACTINIDE));
    public static final RegistrySupplier<Item> BEDROCK_ORE_PRIMARY_SECOND_NONMETAL = registerItem("bedrock_ore_primary_second_nonmetal", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.PRIMARY_SECOND, com.hbm_m.worldgen.BedrockOreDensity.Type.NONMETAL));
    public static final RegistrySupplier<Item> BEDROCK_ORE_PRIMARY_SECOND_CRYSTAL = registerItem("bedrock_ore_primary_second_crystal", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.PRIMARY_SECOND, com.hbm_m.worldgen.BedrockOreDensity.Type.CRYSTAL));
    public static final RegistrySupplier<Item> BEDROCK_ORE_CRUMBS_LIGHT = registerItem("bedrock_ore_crumbs_light", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.CRUMBS, com.hbm_m.worldgen.BedrockOreDensity.Type.LIGHT));
    public static final RegistrySupplier<Item> BEDROCK_ORE_CRUMBS_HEAVY = registerItem("bedrock_ore_crumbs_heavy", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.CRUMBS, com.hbm_m.worldgen.BedrockOreDensity.Type.HEAVY));
    public static final RegistrySupplier<Item> BEDROCK_ORE_CRUMBS_RARE = registerItem("bedrock_ore_crumbs_rare", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.CRUMBS, com.hbm_m.worldgen.BedrockOreDensity.Type.RARE));
    public static final RegistrySupplier<Item> BEDROCK_ORE_CRUMBS_ACTINIDE = registerItem("bedrock_ore_crumbs_actinide", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.CRUMBS, com.hbm_m.worldgen.BedrockOreDensity.Type.ACTINIDE));
    public static final RegistrySupplier<Item> BEDROCK_ORE_CRUMBS_NONMETAL = registerItem("bedrock_ore_crumbs_nonmetal", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.CRUMBS, com.hbm_m.worldgen.BedrockOreDensity.Type.NONMETAL));
    public static final RegistrySupplier<Item> BEDROCK_ORE_CRUMBS_CRYSTAL = registerItem("bedrock_ore_crumbs_crystal", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.CRUMBS, com.hbm_m.worldgen.BedrockOreDensity.Type.CRYSTAL));
    public static final RegistrySupplier<Item> BEDROCK_ORE_SULFURIC_BYPRODUCT_LIGHT = registerItem("bedrock_ore_sulfuric_byproduct_light", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.SULFURIC_BYPRODUCT, com.hbm_m.worldgen.BedrockOreDensity.Type.LIGHT));
    public static final RegistrySupplier<Item> BEDROCK_ORE_SULFURIC_BYPRODUCT_HEAVY = registerItem("bedrock_ore_sulfuric_byproduct_heavy", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.SULFURIC_BYPRODUCT, com.hbm_m.worldgen.BedrockOreDensity.Type.HEAVY));
    public static final RegistrySupplier<Item> BEDROCK_ORE_SULFURIC_BYPRODUCT_RARE = registerItem("bedrock_ore_sulfuric_byproduct_rare", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.SULFURIC_BYPRODUCT, com.hbm_m.worldgen.BedrockOreDensity.Type.RARE));
    public static final RegistrySupplier<Item> BEDROCK_ORE_SULFURIC_BYPRODUCT_ACTINIDE = registerItem("bedrock_ore_sulfuric_byproduct_actinide", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.SULFURIC_BYPRODUCT, com.hbm_m.worldgen.BedrockOreDensity.Type.ACTINIDE));
    public static final RegistrySupplier<Item> BEDROCK_ORE_SULFURIC_BYPRODUCT_NONMETAL = registerItem("bedrock_ore_sulfuric_byproduct_nonmetal", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.SULFURIC_BYPRODUCT, com.hbm_m.worldgen.BedrockOreDensity.Type.NONMETAL));
    public static final RegistrySupplier<Item> BEDROCK_ORE_SULFURIC_BYPRODUCT_CRYSTAL = registerItem("bedrock_ore_sulfuric_byproduct_crystal", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.SULFURIC_BYPRODUCT, com.hbm_m.worldgen.BedrockOreDensity.Type.CRYSTAL));
    public static final RegistrySupplier<Item> BEDROCK_ORE_SULFURIC_ROASTED_LIGHT = registerItem("bedrock_ore_sulfuric_roasted_light", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.SULFURIC_ROASTED, com.hbm_m.worldgen.BedrockOreDensity.Type.LIGHT));
    public static final RegistrySupplier<Item> BEDROCK_ORE_SULFURIC_ROASTED_HEAVY = registerItem("bedrock_ore_sulfuric_roasted_heavy", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.SULFURIC_ROASTED, com.hbm_m.worldgen.BedrockOreDensity.Type.HEAVY));
    public static final RegistrySupplier<Item> BEDROCK_ORE_SULFURIC_ROASTED_RARE = registerItem("bedrock_ore_sulfuric_roasted_rare", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.SULFURIC_ROASTED, com.hbm_m.worldgen.BedrockOreDensity.Type.RARE));
    public static final RegistrySupplier<Item> BEDROCK_ORE_SULFURIC_ROASTED_ACTINIDE = registerItem("bedrock_ore_sulfuric_roasted_actinide", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.SULFURIC_ROASTED, com.hbm_m.worldgen.BedrockOreDensity.Type.ACTINIDE));
    public static final RegistrySupplier<Item> BEDROCK_ORE_SULFURIC_ROASTED_NONMETAL = registerItem("bedrock_ore_sulfuric_roasted_nonmetal", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.SULFURIC_ROASTED, com.hbm_m.worldgen.BedrockOreDensity.Type.NONMETAL));
    public static final RegistrySupplier<Item> BEDROCK_ORE_SULFURIC_ROASTED_CRYSTAL = registerItem("bedrock_ore_sulfuric_roasted_crystal", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.SULFURIC_ROASTED, com.hbm_m.worldgen.BedrockOreDensity.Type.CRYSTAL));
    public static final RegistrySupplier<Item> BEDROCK_ORE_SULFURIC_ARC_LIGHT = registerItem("bedrock_ore_sulfuric_arc_light", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.SULFURIC_ARC, com.hbm_m.worldgen.BedrockOreDensity.Type.LIGHT));
    public static final RegistrySupplier<Item> BEDROCK_ORE_SULFURIC_ARC_HEAVY = registerItem("bedrock_ore_sulfuric_arc_heavy", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.SULFURIC_ARC, com.hbm_m.worldgen.BedrockOreDensity.Type.HEAVY));
    public static final RegistrySupplier<Item> BEDROCK_ORE_SULFURIC_ARC_RARE = registerItem("bedrock_ore_sulfuric_arc_rare", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.SULFURIC_ARC, com.hbm_m.worldgen.BedrockOreDensity.Type.RARE));
    public static final RegistrySupplier<Item> BEDROCK_ORE_SULFURIC_ARC_ACTINIDE = registerItem("bedrock_ore_sulfuric_arc_actinide", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.SULFURIC_ARC, com.hbm_m.worldgen.BedrockOreDensity.Type.ACTINIDE));
    public static final RegistrySupplier<Item> BEDROCK_ORE_SULFURIC_ARC_NONMETAL = registerItem("bedrock_ore_sulfuric_arc_nonmetal", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.SULFURIC_ARC, com.hbm_m.worldgen.BedrockOreDensity.Type.NONMETAL));
    public static final RegistrySupplier<Item> BEDROCK_ORE_SULFURIC_ARC_CRYSTAL = registerItem("bedrock_ore_sulfuric_arc_crystal", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.SULFURIC_ARC, com.hbm_m.worldgen.BedrockOreDensity.Type.CRYSTAL));
    public static final RegistrySupplier<Item> BEDROCK_ORE_SULFURIC_WASHED_LIGHT = registerItem("bedrock_ore_sulfuric_washed_light", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.SULFURIC_WASHED, com.hbm_m.worldgen.BedrockOreDensity.Type.LIGHT));
    public static final RegistrySupplier<Item> BEDROCK_ORE_SULFURIC_WASHED_HEAVY = registerItem("bedrock_ore_sulfuric_washed_heavy", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.SULFURIC_WASHED, com.hbm_m.worldgen.BedrockOreDensity.Type.HEAVY));
    public static final RegistrySupplier<Item> BEDROCK_ORE_SULFURIC_WASHED_RARE = registerItem("bedrock_ore_sulfuric_washed_rare", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.SULFURIC_WASHED, com.hbm_m.worldgen.BedrockOreDensity.Type.RARE));
    public static final RegistrySupplier<Item> BEDROCK_ORE_SULFURIC_WASHED_ACTINIDE = registerItem("bedrock_ore_sulfuric_washed_actinide", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.SULFURIC_WASHED, com.hbm_m.worldgen.BedrockOreDensity.Type.ACTINIDE));
    public static final RegistrySupplier<Item> BEDROCK_ORE_SULFURIC_WASHED_NONMETAL = registerItem("bedrock_ore_sulfuric_washed_nonmetal", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.SULFURIC_WASHED, com.hbm_m.worldgen.BedrockOreDensity.Type.NONMETAL));
    public static final RegistrySupplier<Item> BEDROCK_ORE_SULFURIC_WASHED_CRYSTAL = registerItem("bedrock_ore_sulfuric_washed_crystal", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.SULFURIC_WASHED, com.hbm_m.worldgen.BedrockOreDensity.Type.CRYSTAL));
    public static final RegistrySupplier<Item> BEDROCK_ORE_SOLVENT_BYPRODUCT_LIGHT = registerItem("bedrock_ore_solvent_byproduct_light", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.SOLVENT_BYPRODUCT, com.hbm_m.worldgen.BedrockOreDensity.Type.LIGHT));
    public static final RegistrySupplier<Item> BEDROCK_ORE_SOLVENT_BYPRODUCT_HEAVY = registerItem("bedrock_ore_solvent_byproduct_heavy", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.SOLVENT_BYPRODUCT, com.hbm_m.worldgen.BedrockOreDensity.Type.HEAVY));
    public static final RegistrySupplier<Item> BEDROCK_ORE_SOLVENT_BYPRODUCT_RARE = registerItem("bedrock_ore_solvent_byproduct_rare", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.SOLVENT_BYPRODUCT, com.hbm_m.worldgen.BedrockOreDensity.Type.RARE));
    public static final RegistrySupplier<Item> BEDROCK_ORE_SOLVENT_BYPRODUCT_ACTINIDE = registerItem("bedrock_ore_solvent_byproduct_actinide", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.SOLVENT_BYPRODUCT, com.hbm_m.worldgen.BedrockOreDensity.Type.ACTINIDE));
    public static final RegistrySupplier<Item> BEDROCK_ORE_SOLVENT_BYPRODUCT_NONMETAL = registerItem("bedrock_ore_solvent_byproduct_nonmetal", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.SOLVENT_BYPRODUCT, com.hbm_m.worldgen.BedrockOreDensity.Type.NONMETAL));
    public static final RegistrySupplier<Item> BEDROCK_ORE_SOLVENT_BYPRODUCT_CRYSTAL = registerItem("bedrock_ore_solvent_byproduct_crystal", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.SOLVENT_BYPRODUCT, com.hbm_m.worldgen.BedrockOreDensity.Type.CRYSTAL));
    public static final RegistrySupplier<Item> BEDROCK_ORE_SOLVENT_ROASTED_LIGHT = registerItem("bedrock_ore_solvent_roasted_light", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.SOLVENT_ROASTED, com.hbm_m.worldgen.BedrockOreDensity.Type.LIGHT));
    public static final RegistrySupplier<Item> BEDROCK_ORE_SOLVENT_ROASTED_HEAVY = registerItem("bedrock_ore_solvent_roasted_heavy", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.SOLVENT_ROASTED, com.hbm_m.worldgen.BedrockOreDensity.Type.HEAVY));
    public static final RegistrySupplier<Item> BEDROCK_ORE_SOLVENT_ROASTED_RARE = registerItem("bedrock_ore_solvent_roasted_rare", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.SOLVENT_ROASTED, com.hbm_m.worldgen.BedrockOreDensity.Type.RARE));
    public static final RegistrySupplier<Item> BEDROCK_ORE_SOLVENT_ROASTED_ACTINIDE = registerItem("bedrock_ore_solvent_roasted_actinide", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.SOLVENT_ROASTED, com.hbm_m.worldgen.BedrockOreDensity.Type.ACTINIDE));
    public static final RegistrySupplier<Item> BEDROCK_ORE_SOLVENT_ROASTED_NONMETAL = registerItem("bedrock_ore_solvent_roasted_nonmetal", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.SOLVENT_ROASTED, com.hbm_m.worldgen.BedrockOreDensity.Type.NONMETAL));
    public static final RegistrySupplier<Item> BEDROCK_ORE_SOLVENT_ROASTED_CRYSTAL = registerItem("bedrock_ore_solvent_roasted_crystal", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.SOLVENT_ROASTED, com.hbm_m.worldgen.BedrockOreDensity.Type.CRYSTAL));
    public static final RegistrySupplier<Item> BEDROCK_ORE_SOLVENT_ARC_LIGHT = registerItem("bedrock_ore_solvent_arc_light", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.SOLVENT_ARC, com.hbm_m.worldgen.BedrockOreDensity.Type.LIGHT));
    public static final RegistrySupplier<Item> BEDROCK_ORE_SOLVENT_ARC_HEAVY = registerItem("bedrock_ore_solvent_arc_heavy", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.SOLVENT_ARC, com.hbm_m.worldgen.BedrockOreDensity.Type.HEAVY));
    public static final RegistrySupplier<Item> BEDROCK_ORE_SOLVENT_ARC_RARE = registerItem("bedrock_ore_solvent_arc_rare", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.SOLVENT_ARC, com.hbm_m.worldgen.BedrockOreDensity.Type.RARE));
    public static final RegistrySupplier<Item> BEDROCK_ORE_SOLVENT_ARC_ACTINIDE = registerItem("bedrock_ore_solvent_arc_actinide", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.SOLVENT_ARC, com.hbm_m.worldgen.BedrockOreDensity.Type.ACTINIDE));
    public static final RegistrySupplier<Item> BEDROCK_ORE_SOLVENT_ARC_NONMETAL = registerItem("bedrock_ore_solvent_arc_nonmetal", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.SOLVENT_ARC, com.hbm_m.worldgen.BedrockOreDensity.Type.NONMETAL));
    public static final RegistrySupplier<Item> BEDROCK_ORE_SOLVENT_ARC_CRYSTAL = registerItem("bedrock_ore_solvent_arc_crystal", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.SOLVENT_ARC, com.hbm_m.worldgen.BedrockOreDensity.Type.CRYSTAL));
    public static final RegistrySupplier<Item> BEDROCK_ORE_SOLVENT_WASHED_LIGHT = registerItem("bedrock_ore_solvent_washed_light", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.SOLVENT_WASHED, com.hbm_m.worldgen.BedrockOreDensity.Type.LIGHT));
    public static final RegistrySupplier<Item> BEDROCK_ORE_SOLVENT_WASHED_HEAVY = registerItem("bedrock_ore_solvent_washed_heavy", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.SOLVENT_WASHED, com.hbm_m.worldgen.BedrockOreDensity.Type.HEAVY));
    public static final RegistrySupplier<Item> BEDROCK_ORE_SOLVENT_WASHED_RARE = registerItem("bedrock_ore_solvent_washed_rare", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.SOLVENT_WASHED, com.hbm_m.worldgen.BedrockOreDensity.Type.RARE));
    public static final RegistrySupplier<Item> BEDROCK_ORE_SOLVENT_WASHED_ACTINIDE = registerItem("bedrock_ore_solvent_washed_actinide", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.SOLVENT_WASHED, com.hbm_m.worldgen.BedrockOreDensity.Type.ACTINIDE));
    public static final RegistrySupplier<Item> BEDROCK_ORE_SOLVENT_WASHED_NONMETAL = registerItem("bedrock_ore_solvent_washed_nonmetal", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.SOLVENT_WASHED, com.hbm_m.worldgen.BedrockOreDensity.Type.NONMETAL));
    public static final RegistrySupplier<Item> BEDROCK_ORE_SOLVENT_WASHED_CRYSTAL = registerItem("bedrock_ore_solvent_washed_crystal", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.SOLVENT_WASHED, com.hbm_m.worldgen.BedrockOreDensity.Type.CRYSTAL));
    public static final RegistrySupplier<Item> BEDROCK_ORE_RAD_BYPRODUCT_LIGHT = registerItem("bedrock_ore_rad_byproduct_light", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.RAD_BYPRODUCT, com.hbm_m.worldgen.BedrockOreDensity.Type.LIGHT));
    public static final RegistrySupplier<Item> BEDROCK_ORE_RAD_BYPRODUCT_HEAVY = registerItem("bedrock_ore_rad_byproduct_heavy", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.RAD_BYPRODUCT, com.hbm_m.worldgen.BedrockOreDensity.Type.HEAVY));
    public static final RegistrySupplier<Item> BEDROCK_ORE_RAD_BYPRODUCT_RARE = registerItem("bedrock_ore_rad_byproduct_rare", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.RAD_BYPRODUCT, com.hbm_m.worldgen.BedrockOreDensity.Type.RARE));
    public static final RegistrySupplier<Item> BEDROCK_ORE_RAD_BYPRODUCT_ACTINIDE = registerItem("bedrock_ore_rad_byproduct_actinide", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.RAD_BYPRODUCT, com.hbm_m.worldgen.BedrockOreDensity.Type.ACTINIDE));
    public static final RegistrySupplier<Item> BEDROCK_ORE_RAD_BYPRODUCT_NONMETAL = registerItem("bedrock_ore_rad_byproduct_nonmetal", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.RAD_BYPRODUCT, com.hbm_m.worldgen.BedrockOreDensity.Type.NONMETAL));
    public static final RegistrySupplier<Item> BEDROCK_ORE_RAD_BYPRODUCT_CRYSTAL = registerItem("bedrock_ore_rad_byproduct_crystal", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.RAD_BYPRODUCT, com.hbm_m.worldgen.BedrockOreDensity.Type.CRYSTAL));
    public static final RegistrySupplier<Item> BEDROCK_ORE_RAD_ROASTED_LIGHT = registerItem("bedrock_ore_rad_roasted_light", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.RAD_ROASTED, com.hbm_m.worldgen.BedrockOreDensity.Type.LIGHT));
    public static final RegistrySupplier<Item> BEDROCK_ORE_RAD_ROASTED_HEAVY = registerItem("bedrock_ore_rad_roasted_heavy", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.RAD_ROASTED, com.hbm_m.worldgen.BedrockOreDensity.Type.HEAVY));
    public static final RegistrySupplier<Item> BEDROCK_ORE_RAD_ROASTED_RARE = registerItem("bedrock_ore_rad_roasted_rare", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.RAD_ROASTED, com.hbm_m.worldgen.BedrockOreDensity.Type.RARE));
    public static final RegistrySupplier<Item> BEDROCK_ORE_RAD_ROASTED_ACTINIDE = registerItem("bedrock_ore_rad_roasted_actinide", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.RAD_ROASTED, com.hbm_m.worldgen.BedrockOreDensity.Type.ACTINIDE));
    public static final RegistrySupplier<Item> BEDROCK_ORE_RAD_ROASTED_NONMETAL = registerItem("bedrock_ore_rad_roasted_nonmetal", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.RAD_ROASTED, com.hbm_m.worldgen.BedrockOreDensity.Type.NONMETAL));
    public static final RegistrySupplier<Item> BEDROCK_ORE_RAD_ROASTED_CRYSTAL = registerItem("bedrock_ore_rad_roasted_crystal", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.RAD_ROASTED, com.hbm_m.worldgen.BedrockOreDensity.Type.CRYSTAL));
    public static final RegistrySupplier<Item> BEDROCK_ORE_RAD_ARC_LIGHT = registerItem("bedrock_ore_rad_arc_light", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.RAD_ARC, com.hbm_m.worldgen.BedrockOreDensity.Type.LIGHT));
    public static final RegistrySupplier<Item> BEDROCK_ORE_RAD_ARC_HEAVY = registerItem("bedrock_ore_rad_arc_heavy", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.RAD_ARC, com.hbm_m.worldgen.BedrockOreDensity.Type.HEAVY));
    public static final RegistrySupplier<Item> BEDROCK_ORE_RAD_ARC_RARE = registerItem("bedrock_ore_rad_arc_rare", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.RAD_ARC, com.hbm_m.worldgen.BedrockOreDensity.Type.RARE));
    public static final RegistrySupplier<Item> BEDROCK_ORE_RAD_ARC_ACTINIDE = registerItem("bedrock_ore_rad_arc_actinide", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.RAD_ARC, com.hbm_m.worldgen.BedrockOreDensity.Type.ACTINIDE));
    public static final RegistrySupplier<Item> BEDROCK_ORE_RAD_ARC_NONMETAL = registerItem("bedrock_ore_rad_arc_nonmetal", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.RAD_ARC, com.hbm_m.worldgen.BedrockOreDensity.Type.NONMETAL));
    public static final RegistrySupplier<Item> BEDROCK_ORE_RAD_ARC_CRYSTAL = registerItem("bedrock_ore_rad_arc_crystal", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.RAD_ARC, com.hbm_m.worldgen.BedrockOreDensity.Type.CRYSTAL));
    public static final RegistrySupplier<Item> BEDROCK_ORE_RAD_WASHED_LIGHT = registerItem("bedrock_ore_rad_washed_light", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.RAD_WASHED, com.hbm_m.worldgen.BedrockOreDensity.Type.LIGHT));
    public static final RegistrySupplier<Item> BEDROCK_ORE_RAD_WASHED_HEAVY = registerItem("bedrock_ore_rad_washed_heavy", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.RAD_WASHED, com.hbm_m.worldgen.BedrockOreDensity.Type.HEAVY));
    public static final RegistrySupplier<Item> BEDROCK_ORE_RAD_WASHED_RARE = registerItem("bedrock_ore_rad_washed_rare", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.RAD_WASHED, com.hbm_m.worldgen.BedrockOreDensity.Type.RARE));
    public static final RegistrySupplier<Item> BEDROCK_ORE_RAD_WASHED_ACTINIDE = registerItem("bedrock_ore_rad_washed_actinide", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.RAD_WASHED, com.hbm_m.worldgen.BedrockOreDensity.Type.ACTINIDE));
    public static final RegistrySupplier<Item> BEDROCK_ORE_RAD_WASHED_NONMETAL = registerItem("bedrock_ore_rad_washed_nonmetal", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.RAD_WASHED, com.hbm_m.worldgen.BedrockOreDensity.Type.NONMETAL));
    public static final RegistrySupplier<Item> BEDROCK_ORE_RAD_WASHED_CRYSTAL = registerItem("bedrock_ore_rad_washed_crystal", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.RAD_WASHED, com.hbm_m.worldgen.BedrockOreDensity.Type.CRYSTAL));

    /** Alle 156 Bedrock-Ore-Veredelungsstufen (26 Grades x 6 Types), fuer Datagen/Verarbeitungslogik. */
    public static final java.util.List<RegistrySupplier<Item>> BEDROCK_ORE_ALL_VARIANTS = java.util.List.of(
        BEDROCK_ORE_BASE_LIGHT, BEDROCK_ORE_BASE_HEAVY, BEDROCK_ORE_BASE_RARE, BEDROCK_ORE_BASE_ACTINIDE,
        BEDROCK_ORE_BASE_NONMETAL, BEDROCK_ORE_BASE_CRYSTAL, BEDROCK_ORE_BASE_ROASTED_LIGHT, BEDROCK_ORE_BASE_ROASTED_HEAVY,
        BEDROCK_ORE_BASE_ROASTED_RARE, BEDROCK_ORE_BASE_ROASTED_ACTINIDE, BEDROCK_ORE_BASE_ROASTED_NONMETAL, BEDROCK_ORE_BASE_ROASTED_CRYSTAL,
        BEDROCK_ORE_BASE_WASHED_LIGHT, BEDROCK_ORE_BASE_WASHED_HEAVY, BEDROCK_ORE_BASE_WASHED_RARE, BEDROCK_ORE_BASE_WASHED_ACTINIDE,
        BEDROCK_ORE_BASE_WASHED_NONMETAL, BEDROCK_ORE_BASE_WASHED_CRYSTAL, BEDROCK_ORE_PRIMARY_LIGHT, BEDROCK_ORE_PRIMARY_HEAVY,
        BEDROCK_ORE_PRIMARY_RARE, BEDROCK_ORE_PRIMARY_ACTINIDE, BEDROCK_ORE_PRIMARY_NONMETAL, BEDROCK_ORE_PRIMARY_CRYSTAL,
        BEDROCK_ORE_PRIMARY_ROASTED_LIGHT, BEDROCK_ORE_PRIMARY_ROASTED_HEAVY, BEDROCK_ORE_PRIMARY_ROASTED_RARE, BEDROCK_ORE_PRIMARY_ROASTED_ACTINIDE,
        BEDROCK_ORE_PRIMARY_ROASTED_NONMETAL, BEDROCK_ORE_PRIMARY_ROASTED_CRYSTAL, BEDROCK_ORE_PRIMARY_SULFURIC_LIGHT, BEDROCK_ORE_PRIMARY_SULFURIC_HEAVY,
        BEDROCK_ORE_PRIMARY_SULFURIC_RARE, BEDROCK_ORE_PRIMARY_SULFURIC_ACTINIDE, BEDROCK_ORE_PRIMARY_SULFURIC_NONMETAL, BEDROCK_ORE_PRIMARY_SULFURIC_CRYSTAL,
        BEDROCK_ORE_PRIMARY_NOSULFURIC_LIGHT, BEDROCK_ORE_PRIMARY_NOSULFURIC_HEAVY, BEDROCK_ORE_PRIMARY_NOSULFURIC_RARE, BEDROCK_ORE_PRIMARY_NOSULFURIC_ACTINIDE,
        BEDROCK_ORE_PRIMARY_NOSULFURIC_NONMETAL, BEDROCK_ORE_PRIMARY_NOSULFURIC_CRYSTAL, BEDROCK_ORE_PRIMARY_SOLVENT_LIGHT, BEDROCK_ORE_PRIMARY_SOLVENT_HEAVY,
        BEDROCK_ORE_PRIMARY_SOLVENT_RARE, BEDROCK_ORE_PRIMARY_SOLVENT_ACTINIDE, BEDROCK_ORE_PRIMARY_SOLVENT_NONMETAL, BEDROCK_ORE_PRIMARY_SOLVENT_CRYSTAL,
        BEDROCK_ORE_PRIMARY_NOSOLVENT_LIGHT, BEDROCK_ORE_PRIMARY_NOSOLVENT_HEAVY, BEDROCK_ORE_PRIMARY_NOSOLVENT_RARE, BEDROCK_ORE_PRIMARY_NOSOLVENT_ACTINIDE,
        BEDROCK_ORE_PRIMARY_NOSOLVENT_NONMETAL, BEDROCK_ORE_PRIMARY_NOSOLVENT_CRYSTAL, BEDROCK_ORE_PRIMARY_RAD_LIGHT, BEDROCK_ORE_PRIMARY_RAD_HEAVY,
        BEDROCK_ORE_PRIMARY_RAD_RARE, BEDROCK_ORE_PRIMARY_RAD_ACTINIDE, BEDROCK_ORE_PRIMARY_RAD_NONMETAL, BEDROCK_ORE_PRIMARY_RAD_CRYSTAL,
        BEDROCK_ORE_PRIMARY_NORAD_LIGHT, BEDROCK_ORE_PRIMARY_NORAD_HEAVY, BEDROCK_ORE_PRIMARY_NORAD_RARE, BEDROCK_ORE_PRIMARY_NORAD_ACTINIDE,
        BEDROCK_ORE_PRIMARY_NORAD_NONMETAL, BEDROCK_ORE_PRIMARY_NORAD_CRYSTAL, BEDROCK_ORE_PRIMARY_FIRST_LIGHT, BEDROCK_ORE_PRIMARY_FIRST_HEAVY,
        BEDROCK_ORE_PRIMARY_FIRST_RARE, BEDROCK_ORE_PRIMARY_FIRST_ACTINIDE, BEDROCK_ORE_PRIMARY_FIRST_NONMETAL, BEDROCK_ORE_PRIMARY_FIRST_CRYSTAL,
        BEDROCK_ORE_PRIMARY_SECOND_LIGHT, BEDROCK_ORE_PRIMARY_SECOND_HEAVY, BEDROCK_ORE_PRIMARY_SECOND_RARE, BEDROCK_ORE_PRIMARY_SECOND_ACTINIDE,
        BEDROCK_ORE_PRIMARY_SECOND_NONMETAL, BEDROCK_ORE_PRIMARY_SECOND_CRYSTAL, BEDROCK_ORE_CRUMBS_LIGHT, BEDROCK_ORE_CRUMBS_HEAVY,
        BEDROCK_ORE_CRUMBS_RARE, BEDROCK_ORE_CRUMBS_ACTINIDE, BEDROCK_ORE_CRUMBS_NONMETAL, BEDROCK_ORE_CRUMBS_CRYSTAL,
        BEDROCK_ORE_SULFURIC_BYPRODUCT_LIGHT, BEDROCK_ORE_SULFURIC_BYPRODUCT_HEAVY, BEDROCK_ORE_SULFURIC_BYPRODUCT_RARE, BEDROCK_ORE_SULFURIC_BYPRODUCT_ACTINIDE,
        BEDROCK_ORE_SULFURIC_BYPRODUCT_NONMETAL, BEDROCK_ORE_SULFURIC_BYPRODUCT_CRYSTAL, BEDROCK_ORE_SULFURIC_ROASTED_LIGHT, BEDROCK_ORE_SULFURIC_ROASTED_HEAVY,
        BEDROCK_ORE_SULFURIC_ROASTED_RARE, BEDROCK_ORE_SULFURIC_ROASTED_ACTINIDE, BEDROCK_ORE_SULFURIC_ROASTED_NONMETAL, BEDROCK_ORE_SULFURIC_ROASTED_CRYSTAL,
        BEDROCK_ORE_SULFURIC_ARC_LIGHT, BEDROCK_ORE_SULFURIC_ARC_HEAVY, BEDROCK_ORE_SULFURIC_ARC_RARE, BEDROCK_ORE_SULFURIC_ARC_ACTINIDE,
        BEDROCK_ORE_SULFURIC_ARC_NONMETAL, BEDROCK_ORE_SULFURIC_ARC_CRYSTAL, BEDROCK_ORE_SULFURIC_WASHED_LIGHT, BEDROCK_ORE_SULFURIC_WASHED_HEAVY,
        BEDROCK_ORE_SULFURIC_WASHED_RARE, BEDROCK_ORE_SULFURIC_WASHED_ACTINIDE, BEDROCK_ORE_SULFURIC_WASHED_NONMETAL, BEDROCK_ORE_SULFURIC_WASHED_CRYSTAL,
        BEDROCK_ORE_SOLVENT_BYPRODUCT_LIGHT, BEDROCK_ORE_SOLVENT_BYPRODUCT_HEAVY, BEDROCK_ORE_SOLVENT_BYPRODUCT_RARE, BEDROCK_ORE_SOLVENT_BYPRODUCT_ACTINIDE,
        BEDROCK_ORE_SOLVENT_BYPRODUCT_NONMETAL, BEDROCK_ORE_SOLVENT_BYPRODUCT_CRYSTAL, BEDROCK_ORE_SOLVENT_ROASTED_LIGHT, BEDROCK_ORE_SOLVENT_ROASTED_HEAVY,
        BEDROCK_ORE_SOLVENT_ROASTED_RARE, BEDROCK_ORE_SOLVENT_ROASTED_ACTINIDE, BEDROCK_ORE_SOLVENT_ROASTED_NONMETAL, BEDROCK_ORE_SOLVENT_ROASTED_CRYSTAL,
        BEDROCK_ORE_SOLVENT_ARC_LIGHT, BEDROCK_ORE_SOLVENT_ARC_HEAVY, BEDROCK_ORE_SOLVENT_ARC_RARE, BEDROCK_ORE_SOLVENT_ARC_ACTINIDE,
        BEDROCK_ORE_SOLVENT_ARC_NONMETAL, BEDROCK_ORE_SOLVENT_ARC_CRYSTAL, BEDROCK_ORE_SOLVENT_WASHED_LIGHT, BEDROCK_ORE_SOLVENT_WASHED_HEAVY,
        BEDROCK_ORE_SOLVENT_WASHED_RARE, BEDROCK_ORE_SOLVENT_WASHED_ACTINIDE, BEDROCK_ORE_SOLVENT_WASHED_NONMETAL, BEDROCK_ORE_SOLVENT_WASHED_CRYSTAL,
        BEDROCK_ORE_RAD_BYPRODUCT_LIGHT, BEDROCK_ORE_RAD_BYPRODUCT_HEAVY, BEDROCK_ORE_RAD_BYPRODUCT_RARE, BEDROCK_ORE_RAD_BYPRODUCT_ACTINIDE,
        BEDROCK_ORE_RAD_BYPRODUCT_NONMETAL, BEDROCK_ORE_RAD_BYPRODUCT_CRYSTAL, BEDROCK_ORE_RAD_ROASTED_LIGHT, BEDROCK_ORE_RAD_ROASTED_HEAVY,
        BEDROCK_ORE_RAD_ROASTED_RARE, BEDROCK_ORE_RAD_ROASTED_ACTINIDE, BEDROCK_ORE_RAD_ROASTED_NONMETAL, BEDROCK_ORE_RAD_ROASTED_CRYSTAL,
        BEDROCK_ORE_RAD_ARC_LIGHT, BEDROCK_ORE_RAD_ARC_HEAVY, BEDROCK_ORE_RAD_ARC_RARE, BEDROCK_ORE_RAD_ARC_ACTINIDE,
        BEDROCK_ORE_RAD_ARC_NONMETAL, BEDROCK_ORE_RAD_ARC_CRYSTAL, BEDROCK_ORE_RAD_WASHED_LIGHT, BEDROCK_ORE_RAD_WASHED_HEAVY,
        BEDROCK_ORE_RAD_WASHED_RARE, BEDROCK_ORE_RAD_WASHED_ACTINIDE, BEDROCK_ORE_RAD_WASHED_NONMETAL, BEDROCK_ORE_RAD_WASHED_CRYSTAL
    );

    public static final RegistrySupplier<Item> BETA = registerItem("beta", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BIG_SWORD = registerItem("big_sword", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BIO_WAFER = registerItem("bio_wafer", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BIOMASS = registerItem("biomass", () -> new FuelItem(new Item.Properties(), 400));
    public static final RegistrySupplier<Item> BIOMASS_COMPRESSED = registerItem("biomass_compressed", () -> new FuelItem(new Item.Properties(), 800));
    public static final RegistrySupplier<Item> BISMUTH_AXE = registerItem("bismuth_axe", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BISMUTH_LEGS = registerItem("bismuth_legs", () -> new Item(new Item.Properties()));
    // Кирки-«ломатели глубинной породы» (оригинал: ItemToolAbility.setDepthRockBreaker)
    public static final RegistrySupplier<Item> BISMUTH_PICKAXE = registerItem("bismuth_pickaxe", () -> new com.hbm_m.item.tool.ItemDepthRockBreaker(new Item.Properties()));
    public static final RegistrySupplier<Item> BISMUTH_PLATE = registerItem("bismuth_plate", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BISMUTH_TOOL = registerItem("bismuth_tool", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BJ_BOOTS = registerItem("bj_boots", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BJ_HELMET = registerItem("bj_helmet", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BJ_LEGS = registerItem("bj_legs", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BJ_PLATE = registerItem("bj_plate", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BJ_PLATE_JETPACK = registerItem("bj_plate_jetpack", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BLADE_METEORITE = registerItem("blade_meteorite", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BLADE_TUNGSTEN = registerItem("blade_tungsten", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BLADES_ADVANCED_ALLOY = registerItem("blades_advanced_alloy", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BLADES_DESH = registerItem("blades_desh", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BLADES_STEEL = registerItem("blades_steel", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BLADES_TITANIUM = registerItem("blades_titanium", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BLOWTORCH = registerItem("blowtorch", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BLUEPRINTS = registerItem("blueprints", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BOARD_COPPER = registerItem("board_copper", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BOAT_RUBBER = registerItem("boat_rubber", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BOBMAZON = registerItem("bobmazon", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BOLT_SPIKE = registerItem("bolt_spike", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BOLTGUN = registerItem("boltgun", 
            () -> new com.hbm_m.item.tool.ItemBoltgun(new Item.Properties()));




    public static final RegistrySupplier<Item> BOMB_CALLER = registerItem("bomb_caller", 
            () -> new com.hbm_m.item.tool.ItemBombCaller(com.hbm_m.item.tool.ItemBombCaller.Strike.CARPET, new Item.Properties()));
    public static final RegistrySupplier<Item> BOMB_CALLER_NAPALM = registerItem("bomb_caller_napalm", 
            () -> new com.hbm_m.item.tool.ItemBombCaller(com.hbm_m.item.tool.ItemBombCaller.Strike.NAPALM, new Item.Properties()));
    public static final RegistrySupplier<Item> BOMB_CALLER_CHLORINE = registerItem("bomb_caller_chlorine", 
            () -> new com.hbm_m.item.tool.ItemBombCaller(com.hbm_m.item.tool.ItemBombCaller.Strike.CHLORINE, new Item.Properties()));
    public static final RegistrySupplier<Item> BOMB_CALLER_ATOMIC = registerItem("bomb_caller_atomic", 
            () -> new com.hbm_m.item.tool.ItemBombCaller(com.hbm_m.item.tool.ItemBombCaller.Strike.ATOMIC, new Item.Properties()));
    public static final RegistrySupplier<Item> BOMB_WAFFLE = registerItem("bomb_waffle", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BOOK_GUIDE = registerItem("book_guide", () -> new FuelItem(new Item.Properties(), 200));
    public static final RegistrySupplier<Item> BOOK_LEMEGETON = registerItem("book_lemegeton", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BOOK_OF_ = registerItem("book_of_", () -> new com.hbm_m.item.special.ItemBook(new Item.Properties().stacksTo(1)));
    public static final RegistrySupplier<Item> BOOK_SECRET = registerItem("book_secret", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BOTTLE2_EMPTY = registerItem("bottle2_empty", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BOTTLE2_FRITZ = registerItem("bottle2_fritz", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BOTTLE2_KORL = registerItem("bottle2_korl", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BOTTLE2_SUNSET = registerItem("bottle2_sunset", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BOTTLE_CHERRY = registerItem("bottle_cherry", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BOTTLE_EMPTY = registerItem("bottle_empty", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BOTTLE_MERCURY = registerItem("bottle_mercury", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BOTTLE_NUKA = registerItem("bottle_nuka", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BOTTLE_OPENER = registerItem("bottle_opener", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BOTTLE_QUANTUM = registerItem("bottle_quantum", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BOTTLE_RAD = registerItem("bottle_rad", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BOTTLE_SPARKLE = registerItem("bottle_sparkle", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BOTTLED_CLOUD = registerItem("bottled_cloud", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BOY_BULLET = registerItem("boy_bullet", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BOY_IGNITER = registerItem("boy_igniter", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BOY_KIT = registerItem("boy_kit", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BOY_PROPELLANT = registerItem("boy_propellant", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BOY_SHIELDING = registerItem("boy_shielding", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BOY_TARGET = registerItem("boy_target", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BROKEN_ITEM = registerItem("broken_item", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BUCKET_ACID = registerItem("bucket_acid", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BUCKET_MUD = registerItem("bucket_mud", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BUCKET_SCHRABIDIC_ACID = registerItem("bucket_schrabidic_acid", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BUCKET_SULFURIC_ACID = registerItem("bucket_sulfuric_acid", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BUCKET_TOXIC = registerItem("bucket_toxic", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BURNT_BARK = registerItem("burnt_bark", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> CANISTER_EMPTY = registerItem("canister_empty", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> CANISTER_NAPALM = registerItem("canister_napalm", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> CANNED_SLIME = registerItem("canned_slime", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> CANTEEN_VODKA = registerItem("canteen_vodka", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> CAP_FRITZ = registerItem("cap_fritz", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> CAP_KORL = registerItem("cap_korl", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> CAP_NUKA = registerItem("cap_nuka", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> CAP_QUANTUM = registerItem("cap_quantum", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> CAP_RAD = registerItem("cap_rad", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> CAP_SPARKLE = registerItem("cap_sparkle", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> CAP_STAR = registerItem("cap_star", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> CAP_SUNSET = registerItem("cap_sunset", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> CAPE_GASMASK = registerItem("cape_gasmask", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> CAPE_RADIATION = registerItem("cape_radiation", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> CAPE_SCHRABIDIUM = registerItem("cape_schrabidium", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> CARD_AOS = registerItem("card_aos", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> CARD_QOS = registerItem("card_qos", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> CASING_BAG = registerItem("casing_bag", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> CATALYST_CLAY = registerItem("catalyst_clay", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> CATALYTIC_CONVERTER = registerItem("catalytic_converter", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> CBT_DEVICE = registerItem("cbt_device", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> CELL_ANTI_SCHRABIDIUM = registerItem("cell_anti_schrabidium", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> CELL_ANTIMATTER = registerItem("cell_antimatter", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> CELL_BALEFIRE = registerItem("cell_balefire", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> CELL_DEUTERIUM = registerItem("cell_deuterium", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> CELL_EMPTY = registerItem("cell_empty", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> CELL_PUF6 = registerItem("cell_puf6", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> CELL_TRITIUM = registerItem("cell_tritium", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> CELL_UF6 = registerItem("cell_uf6", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> CENTRI_STICK = registerItem("centri_stick", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> CHAINSAW = registerItem("chainsaw", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> CHEESE = registerItem("cheese", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> CHEMISTRY_SET = registerItem("chemistry_set", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> CHEMISTRY_SET_BORON = registerItem("chemistry_set_boron", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> CHERNOBYLSIGN = registerItem("chernobylsign", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> CHLORINE_PINWHEEL = registerItem("chlorine_pinwheel", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> CHLOROPHYTE_AXE = registerItem("chlorophyte_axe", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> CHLOROPHYTE_PICKAXE = registerItem("chlorophyte_pickaxe", () -> new com.hbm_m.item.tool.ItemDepthRockBreaker(new Item.Properties()));
    public static final RegistrySupplier<Item> CHOCOLATE = registerItem("chocolate", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> CHOCOLATE_MILK = registerItem("chocolate_milk", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> CHOPPER = registerItem("chopper", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> CHOPPER_BLADES = registerItem("chopper_blades", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> CHOPPER_GUN = registerItem("chopper_gun", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> CHOPPER_HEAD = registerItem("chopper_head", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> CHOPPER_TAIL = registerItem("chopper_tail", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> CHOPPER_TORSO = registerItem("chopper_torso", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> CHOPPER_WING = registerItem("chopper_wing", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> CIGARETTE = registerItem("cigarette", 
            () -> new com.hbm_m.item.special.ItemCigarette(false, new Item.Properties()));
    public static final RegistrySupplier<Item> CINNEBAR = registerItem("cinnebar", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> CIRCUIT_STAR = registerItem("circuit_star", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> CLAY_TABLET = registerItem("clay_tablet", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> CMB_AXE = registerItem("cmb_axe", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> CMB_BOOTS = registerItem("cmb_boots", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> CMB_HELMET = registerItem("cmb_helmet", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> CMB_HOE = registerItem("cmb_hoe", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> CMB_LEGS = registerItem("cmb_legs", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> CMB_PLATE = registerItem("cmb_plate", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> CMB_SHOVEL = registerItem("cmb_shovel", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> CMB_SWORD = registerItem("cmb_sword", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> COAL_INFERNAL = registerItem("coal_infernal", () -> new FuelItem(new Item.Properties(), 4800));
    // Ориг. coal_eternal: стек 1, вне креативных вкладок, container item = сам себя — не расходуется.
    public static final RegistrySupplier<Item> COAL_ETERNAL = registerItem("coal_eternal", () -> new EternalFuelItem(new Item.Properties().stacksTo(1), 3200));
    public static final RegistrySupplier<Item> COBALT_AXE = registerItem("cobalt_axe", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> COBALT_DECORATED_AXE = registerItem("cobalt_decorated_axe", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> COBALT_DECORATED_HOE = registerItem("cobalt_decorated_hoe", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> COBALT_DECORATED_PICKAXE = registerItem("cobalt_decorated_pickaxe", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> COBALT_DECORATED_SHOVEL = registerItem("cobalt_decorated_shovel", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> COBALT_DECORATED_SWORD = registerItem("cobalt_decorated_sword", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> COBALT_HOE = registerItem("cobalt_hoe", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> COBALT_LEGS = registerItem("cobalt_legs", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> COBALT_PICKAXE = registerItem("cobalt_pickaxe", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> COBALT_PLATE = registerItem("cobalt_plate", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> COBALT_SHOVEL = registerItem("cobalt_shovel", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> COBALT_SWORD = registerItem("cobalt_sword", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> COFFEE = registerItem("coffee", 
            () -> new com.hbm_m.item.food.ItemCoffee(false, new Item.Properties()));
    public static final RegistrySupplier<Item> COFFEE_RADIUM = registerItem("coffee_radium", 
            () -> new com.hbm_m.item.food.ItemCoffee(true, new Item.Properties()));
    public static final RegistrySupplier<Item> COIN_CREEPER = registerItem("coin_creeper", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> COIN_MASKMAN = registerItem("coin_maskman", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> COIN_RADIATION = registerItem("coin_radiation", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> COIN_TOKEN = registerItem("coin_token", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> COIN_UFO = registerItem("coin_ufo", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> COIN_WORM = registerItem("coin_worm", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> COMPONENT_EMITTER = registerItem("component_emitter", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> COMPONENT_LIMITER = registerItem("component_limiter", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> CONTAINMENT_BOX = registerItem("containment_box", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> CORDITE = registerItem("cordite", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> COTTON_CANDY = registerItem("cotton_candy", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> CRACKPIPE = registerItem("crackpipe", 
            () -> new com.hbm_m.item.special.ItemCigarette(true, new Item.Properties()));
    public static final RegistrySupplier<Item> CRATE_CALLER = registerItem("crate_caller", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> CUBE_POWER = registerItem("cube_power", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> CUSTOM_AMAT = registerItem("custom_amat", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> CUSTOM_DIRTY = registerItem("custom_dirty", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> CUSTOM_FALL = registerItem("custom_fall", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> CUSTOM_HYDRO = registerItem("custom_hydro", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> CUSTOM_KIT = registerItem("custom_kit", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> CUSTOM_NUKE = registerItem("custom_nuke", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> CUSTOM_SCHRAB = registerItem("custom_schrab", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> CUSTOM_TNT = registerItem("custom_tnt", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> DEBRIS_CONCRETE = registerItem("debris_concrete", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> DEBRIS_ELEMENT = registerItem("debris_element", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> DEBRIS_EXCHANGER = registerItem("debris_exchanger", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> DEBRIS_FUEL = registerItem("debris_fuel", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> DEBRIS_GRAPHITE = registerItem("debris_graphite", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> DEBRIS_METAL = registerItem("debris_metal", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> DEBRIS_SHRAPNEL = registerItem("debris_shrapnel", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> DEFINITELYFOOD = registerItem("definitelyfood", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> DEFUSER_GOLD = registerItem("defuser_gold", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> DEMON_CORE_CLOSED = registerItem("demon_core_closed", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> DEMON_CORE_OPEN = registerItem("demon_core_open", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> DESH_AXE = registerItem("desh_axe", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> DESH_HOE = registerItem("desh_hoe", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> DESH_PICKAXE = registerItem("desh_pickaxe", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> DESH_SHOVEL = registerItem("desh_shovel", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> DESH_SWORD = registerItem("desh_sword", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> DESIGNATOR_ARTY_RANGE = registerItem("designator_arty_range", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> DETONATOR_DE = registerItem("detonator_de", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> DETONATOR_DEADMAN = registerItem("detonator_deadman", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> DEUTERIUM_FILTER = registerItem("deuterium_filter", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> DIAMOND_GAVEL = registerItem("diamond_gavel", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> DIESELSUIT_BOOTS = registerItem("dieselsuit_boots", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> DIESELSUIT_HELMET = registerItem("dieselsuit_helmet", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> DIESELSUIT_LEGS = registerItem("dieselsuit_legs", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> DIESELSUIT_PLATE = registerItem("dieselsuit_plate", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> DISPERSER_CANISTER = registerItem("disperser_canister", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> DNS_BOOTS = registerItem("dns_boots", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> DNS_HELMET = registerItem("dns_helmet", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> DNS_LEGS = registerItem("dns_legs", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> DNS_PLATE = registerItem("dns_plate", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> DNT_LEGS = registerItem("dnt_legs", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> DNT_PLATE = registerItem("dnt_plate", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> DNT_SWORD = registerItem("dnt_sword", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> DOOR_METAL = registerItem("door_metal", () -> new Item(new Item.Properties()));
    /** Порт {@code door_red} (1.7.10) — BlockItem двери красной комнаты. */
    public static final RegistrySupplier<Item> DOOR_RED = registerItem("door_red", 
            () -> new BlockItem(ModBlocks.DOOR_RED_BLOCK.get(), new Item.Properties()));

    // ================== Секреты красной комнаты (порт item_secret, 1.7.10) ==================
    public static final RegistrySupplier<Item> ITEM_SECRET_CANISTER = registerItem("item_secret_canister", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> ITEM_SECRET_CONTROLLER = registerItem("item_secret_controller", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> ITEM_SECRET_SELENIUM_STEEL = registerItem("item_secret_selenium_steel", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> ITEM_SECRET_ABERRATOR = registerItem("item_secret_aberrator", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> ITEM_SECRET_FOLLY = registerItem("item_secret_folly", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> DRAX = registerItem("drax", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> DRAX_MK2 = registerItem("drax_mk2", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> DRAX_MK3 = registerItem("drax_mk3", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> DRILLBIT_DESH = registerItem("drillbit_desh", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> DRILLBIT_DESH_DIAMOND = registerItem("drillbit_desh_diamond", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> DRILLBIT_FERRO = registerItem("drillbit_ferro", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> DRILLBIT_FERRO_DIAMOND = registerItem("drillbit_ferro_diamond", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> DRILLBIT_HSS = registerItem("drillbit_hss", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> DRILLBIT_HSS_DIAMOND = registerItem("drillbit_hss_diamond", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> DRILLBIT_STEEL = registerItem("drillbit_steel", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> DRILLBIT_STEEL_DIAMOND = registerItem("drillbit_steel_diamond", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> DRILLBIT_TCALLOY = registerItem("drillbit_tcalloy", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> DRILLBIT_TCALLOY_DIAMOND = registerItem("drillbit_tcalloy_diamond", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> DRONE_LINKER = registerItem("drone_linker", 
            () -> new com.hbm_m.item.tools_and_armor.ItemDroneLinker(new Item.Properties()));
    public static final RegistrySupplier<Item> DRONE_PATROL = registerItem("drone_patrol", 
            () -> new com.hbm_m.item.tools_and_armor.ItemDrone(new Item.Properties(), false, false));
    public static final RegistrySupplier<Item> DRONE_PATROL_CHUNKLOADING = registerItem("drone_patrol_chunkloading", 
            () -> new com.hbm_m.item.tools_and_armor.ItemDrone(new Item.Properties(), false, true));
    public static final RegistrySupplier<Item> DRONE_PATROL_EXPRESS = registerItem("drone_patrol_express", 
            () -> new com.hbm_m.item.tools_and_armor.ItemDrone(new Item.Properties(), true, false));
    public static final RegistrySupplier<Item> DRONE_PATROL_EXPRESS_CHUNKLOADING = registerItem("drone_patrol_express_chunkloading", 
            () -> new com.hbm_m.item.tools_and_armor.ItemDrone(new Item.Properties(), true, true));
    public static final RegistrySupplier<Item> DRONE_REQUEST = registerItem("drone_request", 
            () -> new Item(new Item.Properties().stacksTo(64)));
    public static final RegistrySupplier<Item> DWARVEN_PICKAXE = registerItem("dwarven_pickaxe", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> DYSFUNCTIONAL_REACTOR = registerItem("dysfunctional_reactor", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> EGG_BALEFIRE = registerItem("egg_balefire", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> EXPLOSIVE_LENSES = registerItem("explosive_lenses", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> EGG_BALEFIRE_SHARD = registerItem("egg_balefire_shard", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> EGG_GLYPHID = registerItem("egg_glyphid", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> ELEC_SHOVEL = registerItem("elec_shovel", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> ELEC_SWORD = registerItem("elec_sword", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> ENERGY_CORE = registerItem("energy_core", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> ENTANGLEMENT_KIT = registerItem("entanglement_kit", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> ENVSUIT_BOOTS = registerItem("envsuit_boots", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> ENVSUIT_LEGS = registerItem("envsuit_legs", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> ENVSUIT_PLATE = registerItem("envsuit_plate", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> EUPHEMIUM_BOOTS = registerItem("euphemium_boots", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> EUPHEMIUM_HELMET = registerItem("euphemium_helmet", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> EUPHEMIUM_LEGS = registerItem("euphemium_legs", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> EUPHEMIUM_PLATE = registerItem("euphemium_plate", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> FAU_BOOTS = registerItem("fau_boots", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> FAU_HELMET = registerItem("fau_helmet", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> FAU_LEGS = registerItem("fau_legs", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> FAU_PLATE = registerItem("fau_plate", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> FILTER_COAL = registerItem("filter_coal", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> FINS_BIG_STEEL = registerItem("fins_big_steel", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> FINS_FLAT = registerItem("fins_flat", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> FINS_QUAD_TITANIUM = registerItem("fins_quad_titanium", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> FINS_SMALL_STEEL = registerItem("fins_small_steel", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> FINS_TRI_STEEL = registerItem("fins_tri_steel", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> FLAME_CONSPIRACY = registerItem("flame_conspiracy", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> FLAME_OPINION = registerItem("flame_opinion", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> FLAME_POLITICS = registerItem("flame_politics", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> FLEIJA_CORE = registerItem("fleija_core", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> FLEIJA_IGNITER = registerItem("fleija_igniter", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> FLEIJA_KIT = registerItem("fleija_kit", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> FLEIJA_PROPELLANT = registerItem("fleija_propellant", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> FLUID_IDENTIFIER_MULTI = registerItem("fluid_identifier_multi", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> FLYWHEEL_BERYLLIUM = registerItem("flywheel_beryllium", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> FOODITEM = registerItem("fooditem", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> FRAGMENT_ACTINIUM = registerItem("fragment_actinium", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> FRAGMENT_BORON = registerItem("fragment_boron", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> FRAGMENT_CERIUM = registerItem("fragment_cerium", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> FRAGMENT_COBALT = registerItem("fragment_cobalt", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> FRAGMENT_COLTAN = registerItem("fragment_coltan", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> FRAGMENT_LANTHANIUM = registerItem("fragment_lanthanium", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> FRAGMENT_METEORITE = registerItem("fragment_meteorite", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> FRAGMENT_NEODYMIUM = registerItem("fragment_neodymium", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> FRAGMENT_NIOBIUM = registerItem("fragment_niobium", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> FUSE = registerItem("fuse", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> FUSION_CORE = registerItem("fusion_core", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> FUSION_CORE_INFINITE = registerItem("fusion_core_infinite", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> FUSION_SHIELD_CHLOROPHYTE = registerItem("fusion_shield_chlorophyte", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> FUSION_SHIELD_DESH = registerItem("fusion_shield_desh", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> FUSION_SHIELD_TUNGSTEN = registerItem("fusion_shield_tungsten", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> FUSION_SHIELD_VAPORWAVE = registerItem("fusion_shield_vaporwave", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> GADGET_CORE = registerItem("gadget_core", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> GADGET_EXPLOSIVE = registerItem("gadget_explosive", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> GADGET_KIT = registerItem("gadget_kit", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> GADGET_WIREING = registerItem("gadget_wireing", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> GAS_MASK = registerItem("gas_mask", () -> new ArmorGasMaskItem(Variant.GAS_MASK, new Item.Properties()));
    public static final RegistrySupplier<Item> GAS_MASK_FILTER = registerItem("gas_mask_filter", () -> new ItemGasMaskFilter(new Item.Properties()));
    public static final RegistrySupplier<Item> GAS_MASK_FILTER_COMBO = registerItem("gas_mask_filter_combo", () -> new ItemGasMaskFilter(new Item.Properties()));
    public static final RegistrySupplier<Item> GAS_MASK_FILTER_MONO = registerItem("gas_mask_filter_mono", () -> new ItemGasMaskFilter(new Item.Properties()));
    public static final RegistrySupplier<Item> GAS_MASK_FILTER_PISS = registerItem("gas_mask_filter_piss", () -> new ItemGasMaskFilter(new Item.Properties()));
    public static final RegistrySupplier<Item> GAS_MASK_FILTER_RAG = registerItem("gas_mask_filter_rag", () -> new ItemGasMaskFilter(new Item.Properties()));
    public static final RegistrySupplier<Item> GAS_MASK_M65 = registerItem("gas_mask_m65", () -> new ArmorGasMaskItem(Variant.M65, new Item.Properties()));
    public static final RegistrySupplier<Item> GAS_MASK_MONO = registerItem("gas_mask_mono", () -> new ArmorGasMaskItem(Variant.MONO, new Item.Properties()));
    public static final RegistrySupplier<Item> GAS_MASK_OLDE = registerItem("gas_mask_olde", () -> new ArmorGasMaskItem(Variant.OLDE, new Item.Properties()));
    public static final RegistrySupplier<Item> GAS_TESTER = registerItem("gas_tester", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> GEAR_LARGE = registerItem("gear_large", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> GEM_ALEXANDRITE = registerItem("gem_alexandrite", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> GEM_RAD = registerItem("gem_rad", () -> new Item(new Item.Properties().rarity(net.minecraft.world.item.Rarity.UNCOMMON))); // gem_rad: uncommon в оригинале (setRarity)
    public static final RegistrySupplier<Item> GEM_SODALITE = registerItem("gem_sodalite", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> GEM_TANTALIUM = registerItem("gem_tantalium", 
            () -> new LoreTooltipItem(List.of(
                    Component.translatable("tooltip.hbm_m.tantalum_polycrystal.desc1").withStyle(ChatFormatting.GRAY)),
                    new Item.Properties()));
    // Ориг.: ItemCustomLore().setRarity(EnumRarity.uncommon) — в 1.7.10 uncommon = ЖЁЛТЫЙ
    // (vanilla Rarity.UNCOMMON современного MC тоже жёлтый).
    public static final RegistrySupplier<Item> GEM_VOLCANIC = registerItem("gem_volcanic", 
            () -> new Item(new Item.Properties().rarity(net.minecraft.world.item.Rarity.UNCOMMON)));
    public static final RegistrySupplier<Item> GENERATOR_FRONT = registerItem("generator_front", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> GENERATOR_STEEL = registerItem("generator_steel", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> GLITCH = registerItem("glitch", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> GLOWING_STEW = registerItem("glowing_stew", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> GLYPHID_GLAND = registerItem("glyphid_gland", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> GLYPHID_MEAT = registerItem("glyphid_meat", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> GLYPHID_MEAT_GRILLED = registerItem("glyphid_meat_grilled", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> GOGGLES = registerItem("goggles", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> GRENADE_UNIVERSAL = registerItem("grenade_universal", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> GUN_B92 = registerItem("gun_b92", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> GUN_FIREEXT = registerItem("gun_fireext", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> GUN_KIT_1 = registerItem("gun_kit_1", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> GUN_KIT_2 = registerItem("gun_kit_2", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> GUN_PA_RANGED = registerItem("gun_pa_ranged", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> HAND_DRILL = registerItem("hand_drill", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> HAND_DRILL_DESH = registerItem("hand_drill_desh", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> HAZMAT_BOOTS_GREY = registerItem("hazmat_boots_grey",
            () -> new HazmatArmorItem(ModArmorMaterials.HAZMAT_GREY, ArmorItem.Type.BOOTS, ModArmorMaterialsAccess.armorProps(ModArmorMaterials.HAZMAT_GREY, ArmorItem.Type.BOOTS), HazmatArmorItem.Variant.GREY));
    public static final RegistrySupplier<Item> HAZMAT_BOOTS_RED = registerItem("hazmat_boots_red",
            () -> new HazmatArmorItem(ModArmorMaterials.HAZMAT_RED, ArmorItem.Type.BOOTS, ModArmorMaterialsAccess.armorProps(ModArmorMaterials.HAZMAT_RED, ArmorItem.Type.BOOTS), HazmatArmorItem.Variant.RED));
    public static final RegistrySupplier<Item> HAZMAT_GREY_KIT = registerItem("hazmat_grey_kit", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> HAZMAT_HELMET_GREY = registerItem("hazmat_helmet_grey",
            () -> new HazmatMaskArmorItem(ModArmorMaterials.HAZMAT_GREY, ModArmorMaterialsAccess.armorProps(ModArmorMaterials.HAZMAT_GREY, ArmorItem.Type.HELMET), HazmatArmorItem.Variant.GREY));
    public static final RegistrySupplier<Item> HAZMAT_HELMET_RED = registerItem("hazmat_helmet_red",
            () -> new HazmatMaskArmorItem(ModArmorMaterials.HAZMAT_RED, ModArmorMaterialsAccess.armorProps(ModArmorMaterials.HAZMAT_RED, ArmorItem.Type.HELMET), HazmatArmorItem.Variant.RED));
    public static final RegistrySupplier<Item> HAZMAT_KIT = registerItem("hazmat_kit", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> HAZMAT_LEGS = registerItem("hazmat_legs", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> HAZMAT_LEGS_GREY = registerItem("hazmat_legs_grey",
            () -> new HazmatArmorItem(ModArmorMaterials.HAZMAT_GREY, ArmorItem.Type.LEGGINGS, ModArmorMaterialsAccess.armorProps(ModArmorMaterials.HAZMAT_GREY, ArmorItem.Type.LEGGINGS), HazmatArmorItem.Variant.GREY));
    public static final RegistrySupplier<Item> HAZMAT_LEGS_RED = registerItem("hazmat_legs_red",
            () -> new HazmatArmorItem(ModArmorMaterials.HAZMAT_RED, ArmorItem.Type.LEGGINGS, ModArmorMaterialsAccess.armorProps(ModArmorMaterials.HAZMAT_RED, ArmorItem.Type.LEGGINGS), HazmatArmorItem.Variant.RED));
    public static final RegistrySupplier<Item> HAZMAT_PAA_BOOTS = registerItem("hazmat_paa_boots", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> HAZMAT_PAA_HELMET = registerItem("hazmat_paa_helmet", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> HAZMAT_PAA_LEGS = registerItem("hazmat_paa_legs", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> HAZMAT_PAA_PLATE = registerItem("hazmat_paa_plate", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> HAZMAT_PLATE = registerItem("hazmat_plate", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> HAZMAT_PLATE_GREY = registerItem("hazmat_plate_grey",
            () -> new HazmatArmorItem(ModArmorMaterials.HAZMAT_GREY, ArmorItem.Type.CHESTPLATE, ModArmorMaterialsAccess.armorProps(ModArmorMaterials.HAZMAT_GREY, ArmorItem.Type.CHESTPLATE), HazmatArmorItem.Variant.GREY));
    public static final RegistrySupplier<Item> HAZMAT_PLATE_RED = registerItem("hazmat_plate_red",
            () -> new HazmatArmorItem(ModArmorMaterials.HAZMAT_RED, ArmorItem.Type.CHESTPLATE, ModArmorMaterialsAccess.armorProps(ModArmorMaterials.HAZMAT_RED, ArmorItem.Type.CHESTPLATE), HazmatArmorItem.Variant.RED));
    public static final RegistrySupplier<Item> HAZMAT_RED_KIT = registerItem("hazmat_red_kit", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> HEAVY_COMPONENT = registerItem("heavy_component", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> HEV_BOOTS = registerItem("hev_boots", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> HEV_HELMET = registerItem("hev_helmet", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> HEV_LEGS = registerItem("hev_legs", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> HEV_PLATE = registerItem("hev_plate", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> HOLOTAPE_DAMAGED = registerItem("holotape_damaged", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> HORSESHOE_MAGNET = registerItem("horseshoe_magnet", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> HULL_BIG_ALUMINIUM = registerItem("hull_big_aluminium", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> HULL_BIG_STEEL = registerItem("hull_big_steel", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> HULL_BIG_TITANIUM = registerItem("hull_big_titanium", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> HULL_SMALL_ALUMINIUM = registerItem("hull_small_aluminium", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> HULL_SMALL_STEEL = registerItem("hull_small_steel", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> ICF_PELLET = registerItem("icf_pellet", 
            () -> new com.hbm_m.item.machine.ItemICFPellet(new Item.Properties()));
    public static final RegistrySupplier<Item> ICF_PELLET_DEPLETED = registerItem("icf_pellet_depleted", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> ICF_PELLET_EMPTY = registerItem("icf_pellet_empty", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> INDUSTRIAL_MAGNET = registerItem("industrial_magnet", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> INGOT_ALUMINIUM = registerItem("ingot_aluminium", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> INJECTOR_5HTP = registerItem("injector_5htp", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> INJECTOR_KNIFE = registerItem("injector_knife", 
            () -> new com.hbm_m.armormod.item.ItemModKnife(new Item.Properties()));
    public static final RegistrySupplier<Item> INK = registerItem("ink", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> INSERT_DOXIUM = registerItem("insert_doxium", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> INSERT_DU = registerItem("insert_du", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> INSERT_ERA = registerItem("insert_era", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> INSERT_ESAPI = registerItem("insert_esapi", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> INSERT_GHIORSIUM = registerItem("insert_ghiorsium", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> INSERT_KEVLAR = registerItem("insert_kevlar", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> INSERT_POLONIUM = registerItem("insert_polonium", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> INSERT_SAPI = registerItem("insert_sapi", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> INSERT_STEEL = registerItem("insert_steel", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> INSERT_XSAPI = registerItem("insert_xsapi", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> INSERT_YHARONITE = registerItem("insert_yharonite", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> IV_BLOOD = registerItem("iv_blood", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> IV_EMPTY = registerItem("iv_empty", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> IV_XP = registerItem("iv_xp", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> IV_XP_EMPTY = registerItem("iv_xp_empty", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> JACKT = registerItem("jackt", 
            () -> new ArmorItem(ModArmorMaterialsAccess.holder(ModArmorMaterials.JACKT), ArmorItem.Type.CHESTPLATE, ModArmorMaterialsAccess.armorProps(ModArmorMaterials.JACKT, ArmorItem.Type.CHESTPLATE).stacksTo(1)));
    public static final RegistrySupplier<Item> JACKT2 = registerItem("jackt2", 
            () -> new ArmorItem(ModArmorMaterialsAccess.holder(ModArmorMaterials.JACKT2), ArmorItem.Type.CHESTPLATE, ModArmorMaterialsAccess.armorProps(ModArmorMaterials.JACKT2, ArmorItem.Type.CHESTPLATE).stacksTo(1)));
    public static final RegistrySupplier<Item> JETPACK_BOOST = registerItem("jetpack_boost", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> JETPACK_BREAK = registerItem("jetpack_break", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> JETPACK_FLY = registerItem("jetpack_fly", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> JETPACK_TANK = registerItem("jetpack_tank", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> JETPACK_VECTOR = registerItem("jetpack_vector", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> JOURNAL_BJ = registerItem("journal_bj", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> JOURNAL_PIP = registerItem("journal_pip", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> JOURNAL_SILVER = registerItem("journal_silver", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> KEY = registerItem("key", () -> new com.hbm_m.item.ItemKey(new Item.Properties().stacksTo(1)));
    public static final RegistrySupplier<Item> KEY_RED = registerItem("key_red", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> KEY_RED_CRACKED = registerItem("key_red_cracked", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> LASER_CRYSTAL_BISMUTH = registerItem("laser_crystal_bismuth", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> LASER_CRYSTAL_CMB = registerItem("laser_crystal_cmb", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> LASER_CRYSTAL_CO2 = registerItem("laser_crystal_co2", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> LASER_CRYSTAL_DIGAMMA = registerItem("laser_crystal_digamma", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> LASER_CRYSTAL_DNT = registerItem("laser_crystal_dnt", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> LAUNCH_CODE = registerItem("launch_code", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> LAUNCH_CODE_PIECE = registerItem("launch_code_piece", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> LAUNCH_KEY = registerItem("launch_key", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> LEAD_GAVEL = registerItem("lead_gavel", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> LEMON = registerItem("lemon", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> LINKER = registerItem("linker", () -> new com.hbm_m.item.ItemTeleLink(new Item.Properties().stacksTo(1)));
    public static final RegistrySupplier<Item> LIQUIDATOR_LEGS = registerItem("liquidator_legs", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> LIQUIDATOR_PLATE = registerItem("liquidator_plate", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> LITHIUM = registerItem("lithium", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> LODESTONE = registerItem("lodestone", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> LOOP_STEW = registerItem("loop_stew", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> LOOPS = registerItem("loops", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> LOOT_10 = registerItem("loot_10", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> LOOT_15 = registerItem("loot_15", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> LOOT_MISC = registerItem("loot_misc", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> MAN_KIT = registerItem("man_kit", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> MARSHMALLOW = registerItem("marshmallow", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> MASK_OF_INFAMY = registerItem("mask_of_infamy", () -> new Item(new Item.Properties()));
    // Тряпичные маски: обычная броня без фильтра (порт ModArmor/aMatRags), см. RagMaskItem.
    public static final RegistrySupplier<Item> MASK_PISS = registerItem("mask_piss", () -> new RagMaskItem(true, new Item.Properties()));
    public static final RegistrySupplier<Item> MASK_RAG = registerItem("mask_rag", () -> new RagMaskItem(false, new Item.Properties()));
    public static final RegistrySupplier<Item> MATCHSTICK = registerItem("matchstick", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> MECH_KEY = registerItem("mech_key", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> MED_BAG = registerItem("med_bag", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> MED_IPECAC = registerItem("med_ipecac", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> MED_PTSD = registerItem("med_ptsd", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> MEDAL_LIQUIDATOR = registerItem("medal_liquidator", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> MELTDOWN_TOOL = registerItem("meltdown_tool", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> MEMESPOON = registerItem("memespoon", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> MESE_AXE = registerItem("mese_axe", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> MESE_GAVEL = registerItem("mese_gavel", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> MESE_PICKAXE = registerItem("mese_pickaxe", () -> new com.hbm_m.item.tool.ItemDepthRockBreaker(new Item.Properties()));
    public static final RegistrySupplier<Item> METEOR_CHARM = registerItem("meteor_charm", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> METEOR_REMOTE = registerItem("meteor_remote", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> MIKE_COOLING_UNIT = registerItem("mike_cooling_unit", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> MIKE_CORE = registerItem("mike_core", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> MIKE_DEUT = registerItem("mike_deut", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> MIKE_KIT = registerItem("mike_kit", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> MIRROR_TOOL = registerItem("mirror_tool", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> MISSILE_CARRIER = registerItem("missile_carrier", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> MISSILE_CUSTOM = registerItem("missile_custom", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> MISSILE_ENDO = registerItem("missile_endo", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> MISSILE_EXO = registerItem("missile_exo", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> MISSILE_KIT = registerItem("missile_kit", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> MORNING_GLORY = registerItem("morning_glory", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> MP_C_1 = registerItem("mp_c_1", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> MP_C_2 = registerItem("mp_c_2", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> MP_C_3 = registerItem("mp_c_3", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> MP_C_4 = registerItem("mp_c_4", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> MP_C_5 = registerItem("mp_c_5", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> MUCHO_MANGO = registerItem("mucho_mango", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> MULTI_KIT = registerItem("multi_kit", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> N2_CHARGE = registerItem("n2_charge", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> NEUTRINO_LENS = registerItem("neutrino_lens", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> NIGHT_VISION = registerItem("night_vision", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> NITRA = registerItem("nitra", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> NITRA_SMALL = registerItem("nitra_small", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> NO9 = registerItem("no9", 
            () -> new ArmorItem(ModArmorMaterialsAccess.holder(ModArmorMaterials.STEEL), ArmorItem.Type.HELMET, ModArmorMaterialsAccess.armorProps(ModArmorMaterials.STEEL, ArmorItem.Type.HELMET).stacksTo(1)));
    public static final RegistrySupplier<Item> NOTHING = registerItem("nothing", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> NUCLEAR_WASTE = registerItem("nuclear_waste", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> NUCLEAR_WASTE_LONG = registerItem("nuclear_waste_long", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> NUCLEAR_WASTE_LONG_DEPLETED = registerItem("nuclear_waste_long_depleted", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> NUCLEAR_WASTE_PEARL = registerItem("nuclear_waste_pearl", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> NUCLEAR_WASTE_SHORT = registerItem("nuclear_waste_short", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> NUCLEAR_WASTE_SHORT_DEPLETED = registerItem("nuclear_waste_short_depleted", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> NUCLEAR_WASTE_VITRIFIED = registerItem("nuclear_waste_vitrified", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> NUGGET = registerItem("nugget", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> NUGGET_MERCURY = registerItem("nugget_mercury", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> NUKE_ADVANCED_KIT = registerItem("nuke_advanced_kit", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> NUKE_COMMERCIALLY_KIT = registerItem("nuke_commercially_kit", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> NUKE_ELECTRIC_KIT = registerItem("nuke_electric_kit", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> NUKE_STARTER_KIT = registerItem("nuke_starter_kit", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> ORE_BEDROCK = registerItem("ore_bedrock", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> ORE_CENTRIFUGED = registerItem("ore_centrifuged", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> ORE_CLEANED = registerItem("ore_cleaned", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> ORE_DEEPCLEANED = registerItem("ore_deepcleaned", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> ORE_DENSITY_SCANNER = registerItem("ore_density_scanner", () -> new com.hbm_m.item.tool.ItemOreDensityScanner(new Item.Properties()));
    public static final RegistrySupplier<Item> ORE_ENRICHED = registerItem("ore_enriched", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> ORE_NITRATED = registerItem("ore_nitrated", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> ORE_NITROCRYSTALLINE = registerItem("ore_nitrocrystalline", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> ORE_PURIFIED = registerItem("ore_purified", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> ORE_RADCLEANED = registerItem("ore_radcleaned", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> ORE_SEARED = registerItem("ore_seared", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> ORE_SEPARATED = registerItem("ore_separated", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> OVERFUSE = registerItem("overfuse", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> PAA_LEGS = registerItem("paa_legs", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> PAA_PLATE = registerItem("paa_plate", () -> new Item(new Item.Properties()));
    /** Порт padlock (1.7.10 ItemLock, lockMod 0.1): навешивается на ILockable, код вырезается на кейфордже. */
    public static final RegistrySupplier<Item> PADLOCK = registerItem("padlock", () -> new com.hbm_m.item.ItemLock(new Item.Properties().stacksTo(1), 0.1D));
    public static final RegistrySupplier<Item> PADLOCK_REINFORCED = registerItem("padlock_reinforced", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> PADLOCK_RUSTY = registerItem("padlock_rusty", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> PADLOCK_UNBREAKABLE = registerItem("padlock_unbreakable", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> PADS_RUBBER = registerItem("pads_rubber", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> PADS_SLIME = registerItem("pads_slime", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> PADS_STATIC = registerItem("pads_static", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> PANCAKE = registerItem("pancake", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> PART_BARREL_HEAVY = registerItem("part_barrel_heavy", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> PART_BARREL_LIGHT = registerItem("part_barrel_light", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> PART_GRIP = registerItem("part_grip", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> PART_MECHANISM = registerItem("part_mechanism", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> PART_RECEIVER_HEAVY = registerItem("part_receiver_heavy", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> PART_RECEIVER_LIGHT = registerItem("part_receiver_light", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> PART_STOCK = registerItem("part_stock", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> PARTICLE_AMAT = registerItem("particle_amat", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> PARTICLE_ASCHRAB = registerItem("particle_aschrab", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> PARTICLE_COPPER = registerItem("particle_copper", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> PARTICLE_DARK = registerItem("particle_dark", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> PARTICLE_DIGAMMA = registerItem("particle_digamma", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> PARTICLE_EMPTY = registerItem("particle_empty", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> PARTICLE_HIGGS = registerItem("particle_higgs", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> PARTICLE_HYDROGEN = registerItem("particle_hydrogen", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> PARTICLE_LEAD = registerItem("particle_lead", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> PARTICLE_LUTECE = registerItem("particle_lutece", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> PARTICLE_MUON = registerItem("particle_muon", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> PARTICLE_SPARKTICLE = registerItem("particle_sparkticle", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> PARTICLE_STRANGE = registerItem("particle_strange", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> PARTICLE_TACHYON = registerItem("particle_tachyon", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> PARTS_LEGENDARY = registerItem("parts_legendary", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> PEAS = registerItem("peas", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> PEDESTAL_STEEL = registerItem("pedestal_steel", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> PELLET_CLUSTER = registerItem("pellet_cluster", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> PELLET_GAS = registerItem("pellet_gas", () -> new Item(new Item.Properties()));
    // ── RTG-Pellets ──
    // 1:1 aus {@code ModItems} (1.7.10): Heizleistung, Halbwertszeit und Zerfallsprodukt je Pellet.
    // Die Lebensdauer ist ueberall die Halbwertszeit mal anderthalb, so wie dort.

    /** Die ausgebrannten Pellets - im Original sechs Metadaten eines Gegenstands. */
    public static final RegistrySupplier<Item> PELLET_RTG_DEPLETED_BISMUTH = registerItem("pellet_rtg_depleted_bismuth", 
            () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> PELLET_RTG_DEPLETED_MERCURY = registerItem("pellet_rtg_depleted_mercury", 
            () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> PELLET_RTG_DEPLETED_NEPTUNIUM = registerItem("pellet_rtg_depleted_neptunium", 
            () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> PELLET_RTG_DEPLETED_LEAD = registerItem("pellet_rtg_depleted_lead", 
            () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> PELLET_RTG_DEPLETED_ZIRCONIUM = registerItem("pellet_rtg_depleted_zirconium", 
            () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> PELLET_RTG_DEPLETED_NICKEL = registerItem("pellet_rtg_depleted_nickel", 
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> PELLET_RTG = registerItem("pellet_rtg", 
            () -> new ItemRTGPellet(10, ItemRTGPellet.lifespan15(87.7F, ItemRTGPellet.HalfLifeType.MEDIUM),
                    () -> PELLET_RTG_DEPLETED_LEAD.get(), new Item.Properties()));
    public static final RegistrySupplier<Item> PELLET_RTG_ACTINIUM = registerItem("pellet_rtg_actinium", 
            () -> new ItemRTGPellet(20, ItemRTGPellet.lifespan15(21.8F, ItemRTGPellet.HalfLifeType.MEDIUM),
                    () -> PELLET_RTG_DEPLETED_LEAD.get(), new Item.Properties()));
    public static final RegistrySupplier<Item> PELLET_RTG_AMERICIUM = registerItem("pellet_rtg_americium", 
            () -> new ItemRTGPellet(20, ItemRTGPellet.lifespan15(4.7F, ItemRTGPellet.HalfLifeType.LONG),
                    () -> PELLET_RTG_DEPLETED_NEPTUNIUM.get(), new Item.Properties()));
    /** Im Original nicht vorhanden - dieser Port hat die Textur, aber keine Werte dafuer. */
    public static final RegistrySupplier<Item> PELLET_RTG_BERKELIUM = registerItem("pellet_rtg_berkelium", 
            () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> PELLET_RTG_COBALT = registerItem("pellet_rtg_cobalt", 
            () -> new ItemRTGPellet(15, ItemRTGPellet.lifespan15(5.3F, ItemRTGPellet.HalfLifeType.MEDIUM),
                    () -> PELLET_RTG_DEPLETED_NICKEL.get(), new Item.Properties()));
    /** Original: {@code rtgDecay() ? 200 : 100} - der Zerfall ist hier an, also 200. */
    public static final RegistrySupplier<Item> PELLET_RTG_GOLD = registerItem("pellet_rtg_gold", 
            () -> new ItemRTGPellet(200, ItemRTGPellet.lifespan15(2.7F, ItemRTGPellet.HalfLifeType.SHORT),
                    () -> PELLET_RTG_DEPLETED_MERCURY.get(), new Item.Properties()));
    /** Original: {@code rtgDecay() ? 600 : 200} - der Zerfall ist hier an, also 600. */
    public static final RegistrySupplier<Item> PELLET_RTG_LEAD = registerItem("pellet_rtg_lead", 
            () -> new ItemRTGPellet(600, ItemRTGPellet.lifespan15(0.3F, ItemRTGPellet.HalfLifeType.SHORT),
                    () -> PELLET_RTG_DEPLETED_BISMUTH.get(), new Item.Properties()));
    public static final RegistrySupplier<Item> PELLET_RTG_POLONIUM = registerItem("pellet_rtg_polonium", 
            () -> new ItemRTGPellet(50, ItemRTGPellet.lifespan15(138.0F, ItemRTGPellet.HalfLifeType.SHORT),
                    () -> PELLET_RTG_DEPLETED_LEAD.get(), new Item.Properties()));
    public static final RegistrySupplier<Item> PELLET_RTG_RADIUM = registerItem("pellet_rtg_radium", 
            () -> new ItemRTGPellet(3, ItemRTGPellet.lifespan15(16.0F, ItemRTGPellet.HalfLifeType.LONG),
                    () -> PELLET_RTG_DEPLETED_LEAD.get(), new Item.Properties()));
    public static final RegistrySupplier<Item> PELLET_RTG_STRONTIUM = registerItem("pellet_rtg_strontium", 
            () -> new ItemRTGPellet(15, ItemRTGPellet.lifespan15(29.0F, ItemRTGPellet.HalfLifeType.MEDIUM),
                    () -> PELLET_RTG_DEPLETED_ZIRCONIUM.get(), new Item.Properties()));
    public static final RegistrySupplier<Item> PELLET_RTG_WEAK = registerItem("pellet_rtg_weak", 
            () -> new ItemRTGPellet(5, ItemRTGPellet.lifespan15(1.0F, ItemRTGPellet.HalfLifeType.LONG),
                    () -> PELLET_RTG_DEPLETED_LEAD.get(), new Item.Properties()));
    public static final RegistrySupplier<Item> PHOTO_PANEL = registerItem("photo_panel", () -> new Item(new Item.Properties()));
    // Brennstaebe des Uranmeilers (MK2). Sie stehen neben den alten Staeben oben, weil das
    // Original beide Familien fuehrt - die alten gehoeren zum abgekuendigten Graphitmeiler.
    public static final RegistrySupplier<Item> PILE_ROD_RA226BE = registerItem("pile_rod_mk2_ra226be", 
            () -> new com.hbm_m.item.machine.ItemPileRodMK2(new Item.Properties(),
                    com.hbm_m.item.machine.ItemPileRodMK2.EnumPileRod.RA226BE));
    public static final RegistrySupplier<Item> PILE_ROD_PO210BE = registerItem("pile_rod_mk2_po210be", 
            () -> new com.hbm_m.item.machine.ItemPileRodMK2(new Item.Properties(),
                    com.hbm_m.item.machine.ItemPileRodMK2.EnumPileRod.PO210BE));
    public static final RegistrySupplier<Item> PILE_ROD_ZR = registerItem("pile_rod_mk2_zr", 
            () -> new com.hbm_m.item.machine.ItemPileRodMK2(new Item.Properties(),
                    com.hbm_m.item.machine.ItemPileRodMK2.EnumPileRod.ZR));
    public static final RegistrySupplier<Item> PILE_ROD_NU = registerItem("pile_rod_mk2_nu", 
            () -> new com.hbm_m.item.machine.ItemPileRodMK2(new Item.Properties(),
                    com.hbm_m.item.machine.ItemPileRodMK2.EnumPileRod.NU));
    public static final RegistrySupplier<Item> PILE_ROD_MK2_PU239 = registerItem("pile_rod_mk2_pu239", 
            () -> new com.hbm_m.item.machine.ItemPileRodMK2(new Item.Properties(),
                    com.hbm_m.item.machine.ItemPileRodMK2.EnumPileRod.PU239));
    public static final RegistrySupplier<Item> PILE_ROD_RGP = registerItem("pile_rod_mk2_rgp", 
            () -> new com.hbm_m.item.machine.ItemPileRodMK2(new Item.Properties(),
                    com.hbm_m.item.machine.ItemPileRodMK2.EnumPileRod.RGP));
    public static final RegistrySupplier<Item> PILE_ROD_WASTE = registerItem("pile_rod_mk2_waste", 
            () -> new com.hbm_m.item.machine.ItemPileRodMK2(new Item.Properties(),
                    com.hbm_m.item.machine.ItemPileRodMK2.EnumPileRod.WASTE));

    public static final RegistrySupplier<Item> PILE_ROD_BORON = registerItem("pile_rod_boron", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> PILE_ROD_DETECTOR = registerItem("pile_rod_detector", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> PILE_ROD_LITHIUM = registerItem("pile_rod_lithium", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> PILE_ROD_PLUTONIUM = registerItem("pile_rod_plutonium", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> PILE_ROD_PU239 = registerItem("pile_rod_pu239", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> PILE_ROD_SOURCE = registerItem("pile_rod_source", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> PILE_ROD_URANIUM = registerItem("pile_rod_uranium", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> PILL_HERBAL = registerItem("pill_herbal", () -> new ItemSimpleConsumable(new Item.Properties(), ModConsumables::usePillHerbal));
    public static final RegistrySupplier<Item> PILL_IODINE = registerItem("pill_iodine", () -> new ItemSimpleConsumable(new Item.Properties(), ModConsumables::usePillIodine));
    public static final RegistrySupplier<Item> PILL_RED = registerItem("pill_red", () -> new ItemSimpleConsumable(new Item.Properties(), ModConsumables::usePillRed));
    /** Original: {@code ModItems.radx} - die Textur lag im Port schon vor, das Item fehlte. */
    public static final RegistrySupplier<Item> RADX = registerItem("radx", () -> new ItemSimpleConsumable(new Item.Properties(), ModConsumables::useRadX));
    public static final RegistrySupplier<Item> PIN = registerItem("pin", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> PIPES_STEEL = registerItem("pipes_steel", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> PIPETTE = registerItem("pipette", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> PIPETTE_BORON = registerItem("pipette_boron", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> PIPETTE_LABORATORY = registerItem("pipette_laboratory", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> PISTON_SELENIUM = registerItem("piston_selenium", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> PISTON_SET_DESH = registerItem("piston_set_desh", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> PISTON_SET_DURA = registerItem("piston_set_dura", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> PISTON_SET_STARMETAL = registerItem("piston_set_starmetal", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> PISTON_SET_STEEL = registerItem("piston_set_steel", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> PLAN_C = registerItem("plan_c", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> PLASTIC_BAG = registerItem("plastic_bag", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> PLATE_ALUMINIUM = registerItem("plate_aluminium", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> POLAROID = registerItem("polaroid", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> POLLUTION_DETECTOR = registerItem("pollution_detector", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> POWER_NET_TOOL = registerItem("power_net_tool", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> PROTECTION_CHARM = registerItem("protection_charm", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> PROTOTYPE_KIT = registerItem("prototype_kit", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> PUDDING = registerItem("pudding", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> PWR_PRINTER = registerItem("pwr_printer", 
        () -> new com.hbm_m.item.nuclear.PWRFuelPrinterItem(new Item.Properties().stacksTo(1)));

    // PWR reactor fuel - see com.hbm_m.item.nuclear.PWRFuelType for the mechanics.
    public static final RegistrySupplier<Item> PWR_FUEL_MEU = registerItem("pwr_fuel_meu", 
        () -> new com.hbm_m.item.nuclear.PWRFuelItem(new Item.Properties(), com.hbm_m.item.nuclear.PWRFuelType.MEU));
    public static final RegistrySupplier<Item> PWR_FUEL_MEU_HOT = registerItem("pwr_fuel_meu_hot", 
        () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> PWR_FUEL_HEU233 = registerItem("pwr_fuel_heu233", 
        () -> new com.hbm_m.item.nuclear.PWRFuelItem(new Item.Properties(), com.hbm_m.item.nuclear.PWRFuelType.HEU233));
    public static final RegistrySupplier<Item> PWR_FUEL_HEU233_HOT = registerItem("pwr_fuel_heu233_hot", 
        () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> PWR_FUEL_HEU235 = registerItem("pwr_fuel_heu235", 
        () -> new com.hbm_m.item.nuclear.PWRFuelItem(new Item.Properties(), com.hbm_m.item.nuclear.PWRFuelType.HEU235));
    public static final RegistrySupplier<Item> PWR_FUEL_HEU235_HOT = registerItem("pwr_fuel_heu235_hot", 
        () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> PWR_FUEL_MEN = registerItem("pwr_fuel_men", 
        () -> new com.hbm_m.item.nuclear.PWRFuelItem(new Item.Properties(), com.hbm_m.item.nuclear.PWRFuelType.MEN));
    public static final RegistrySupplier<Item> PWR_FUEL_MEN_HOT = registerItem("pwr_fuel_men_hot", 
        () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> PWR_FUEL_HEN237 = registerItem("pwr_fuel_hen237", 
        () -> new com.hbm_m.item.nuclear.PWRFuelItem(new Item.Properties(), com.hbm_m.item.nuclear.PWRFuelType.HEN237));
    public static final RegistrySupplier<Item> PWR_FUEL_HEN237_HOT = registerItem("pwr_fuel_hen237_hot", 
        () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> PWR_FUEL_MOX = registerItem("pwr_fuel_mox", 
        () -> new com.hbm_m.item.nuclear.PWRFuelItem(new Item.Properties(), com.hbm_m.item.nuclear.PWRFuelType.MOX));
    public static final RegistrySupplier<Item> PWR_FUEL_MOX_HOT = registerItem("pwr_fuel_mox_hot", 
        () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> PWR_FUEL_MEP = registerItem("pwr_fuel_mep", 
        () -> new com.hbm_m.item.nuclear.PWRFuelItem(new Item.Properties(), com.hbm_m.item.nuclear.PWRFuelType.MEP));
    public static final RegistrySupplier<Item> PWR_FUEL_MEP_HOT = registerItem("pwr_fuel_mep_hot", 
        () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> PWR_FUEL_HEP239 = registerItem("pwr_fuel_hep239", 
        () -> new com.hbm_m.item.nuclear.PWRFuelItem(new Item.Properties(), com.hbm_m.item.nuclear.PWRFuelType.HEP239));
    public static final RegistrySupplier<Item> PWR_FUEL_HEP239_HOT = registerItem("pwr_fuel_hep239_hot", 
        () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> PWR_FUEL_HEP241 = registerItem("pwr_fuel_hep241", 
        () -> new com.hbm_m.item.nuclear.PWRFuelItem(new Item.Properties(), com.hbm_m.item.nuclear.PWRFuelType.HEP241));
    public static final RegistrySupplier<Item> PWR_FUEL_HEP241_HOT = registerItem("pwr_fuel_hep241_hot", 
        () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> PWR_FUEL_MEA = registerItem("pwr_fuel_mea", 
        () -> new com.hbm_m.item.nuclear.PWRFuelItem(new Item.Properties(), com.hbm_m.item.nuclear.PWRFuelType.MEA));
    public static final RegistrySupplier<Item> PWR_FUEL_MEA_HOT = registerItem("pwr_fuel_mea_hot", 
        () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> PWR_FUEL_HEA242 = registerItem("pwr_fuel_hea242", 
        () -> new com.hbm_m.item.nuclear.PWRFuelItem(new Item.Properties(), com.hbm_m.item.nuclear.PWRFuelType.HEA242));
    public static final RegistrySupplier<Item> PWR_FUEL_HEA242_HOT = registerItem("pwr_fuel_hea242_hot", 
        () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> PWR_FUEL_HES326 = registerItem("pwr_fuel_hes326", 
        () -> new com.hbm_m.item.nuclear.PWRFuelItem(new Item.Properties(), com.hbm_m.item.nuclear.PWRFuelType.HES326));
    public static final RegistrySupplier<Item> PWR_FUEL_HES326_HOT = registerItem("pwr_fuel_hes326_hot", 
        () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> PWR_FUEL_HES327 = registerItem("pwr_fuel_hes327", 
        () -> new com.hbm_m.item.nuclear.PWRFuelItem(new Item.Properties(), com.hbm_m.item.nuclear.PWRFuelType.HES327));
    public static final RegistrySupplier<Item> PWR_FUEL_HES327_HOT = registerItem("pwr_fuel_hes327_hot", 
        () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> PWR_FUEL_BFB_AM_MIX = registerItem("pwr_fuel_bfb_am_mix", 
        () -> new com.hbm_m.item.nuclear.PWRFuelItem(new Item.Properties(), com.hbm_m.item.nuclear.PWRFuelType.BFB_AM_MIX));
    public static final RegistrySupplier<Item> PWR_FUEL_BFB_AM_MIX_HOT = registerItem("pwr_fuel_bfb_am_mix_hot", 
        () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> PWR_FUEL_BFB_PU241 = registerItem("pwr_fuel_bfb_pu241", 
        () -> new com.hbm_m.item.nuclear.PWRFuelItem(new Item.Properties(), com.hbm_m.item.nuclear.PWRFuelType.BFB_PU241));
    public static final RegistrySupplier<Item> PWR_FUEL_BFB_PU241_HOT = registerItem("pwr_fuel_bfb_pu241_hot", 
        () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> QUARTZ_PLUTONIUM = registerItem("quartz_plutonium", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> RADAR_LINKER = registerItem("radar_linker", () -> new com.hbm_m.item.tool.ItemRadarLinker(new Item.Properties()));
    // Тряпка (ориг. ItemRag, 4541): тултип 1:1 из item.rag.desc оригинала (2 строки, без стиля).
    public static final RegistrySupplier<Item> RAG = registerItem("rag", () -> new LoreTooltipItem(List.of(
            Component.translatable("tooltip.hbm_m.rag.desc1"),
            Component.translatable("tooltip.hbm_m.rag.desc2")),
            new Item.Properties()));
    public static final RegistrySupplier<Item> RAG_DAMP = registerItem("rag_damp", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> RAG_PISS = registerItem("rag_piss", () -> new Item(new Item.Properties()));
    // Remaining RBMK fuel rods/pellets - 1:1 port of the original's ItemRBMKRod stat blocks
    // (com.hbm.items.ModItems, RBMKRodItem class doc explains the burn-curve math).
    public static final RegistrySupplier<Item> RBMK_FUEL_BALEFIRE = registerItem("rbmk_fuel_balefire", 
            () -> new RBMKRodItem("Draconic Flames", new Item.Properties())
                    .setYield(100_000_000).setStats(100, 35).setFunction(RBMKRodItem.EnumBurnFunc.LINEAR)
                    .setXenon(0.0, 50).setHeat(3.0).setMeltingPoint(3652).setTint(0xB2FF1B).setPellet(() -> ModItems.RBMK_PELLET_BALEFIRE.get()));
    public static final RegistrySupplier<Item> RBMK_FUEL_BALEFIRE_GOLD = registerItem("rbmk_fuel_balefire_gold", 
            () -> new RBMKRodItem("Antihydrogen in a Magnetized Gold-198 Lattice", new Item.Properties())
                    .setYield(100_000_000).setStats(50, 10).setFunction(RBMKRodItem.EnumBurnFunc.ARCH)
                    .setDepletionFunction(RBMKRodItem.EnumDepleteFunc.LINEAR).setXenon(0.0, 50)
                    .setMeltingPoint(2000).setTint(0xDC9613).setPellet(() -> ModItems.RBMK_PELLET_BALEFIRE_GOLD.get()));
    public static final RegistrySupplier<Item> RBMK_FUEL_FLASHLEAD = registerItem("rbmk_fuel_flashlead", 
            () -> new RBMKRodItem("Antihydrogen confined by a Magnetized Gold-198 and Lead-209 Lattice", new Item.Properties())
                    .setYield(250_000_000).setStats(40, 50).setFunction(RBMKRodItem.EnumBurnFunc.ARCH)
                    .setDepletionFunction(RBMKRodItem.EnumDepleteFunc.LINEAR).setXenon(0.0, 50)
                    .setMeltingPoint(2050).setTint(0x7B7B87).setPellet(() -> ModItems.RBMK_PELLET_FLASHLEAD.get()));
    public static final RegistrySupplier<Item> RBMK_FUEL_HEA241 = registerItem("rbmk_fuel_hea241", 
            () -> new RBMKRodItem("Highly Enriched Americium-241 Rod", new Item.Properties())
                    .setYield(100_000_000).setStats(65, 15).setFunction(RBMKRodItem.EnumBurnFunc.SQUARE_ROOT)
                    .setHeat(1.85).setMeltingPoint(2386).setNeutronTypes(NType.FAST, NType.FAST).setTint(0xA88A8F).setPellet(() -> ModItems.RBMK_PELLET_HEA241.get()));
    public static final RegistrySupplier<Item> RBMK_FUEL_HEA242 = registerItem("rbmk_fuel_hea242", 
            () -> new RBMKRodItem("Highly Enriched Americium-242 Rod", new Item.Properties())
                    .setYield(100_000_000).setStats(45).setFunction(RBMKRodItem.EnumBurnFunc.LINEAR)
                    .setHeat(2.0).setMeltingPoint(3386).setTint(0xA88A8F).setPellet(() -> ModItems.RBMK_PELLET_HEA242.get()));
    public static final RegistrySupplier<Item> RBMK_FUEL_HEAUS = registerItem("rbmk_fuel_heaus", 
            () -> new RBMKRodItem("Highly Enriched Australium (Ayerite) Rod", new Item.Properties())
                    .setYield(100_000_000).setStats(35).setFunction(RBMKRodItem.EnumBurnFunc.SQUARE_ROOT)
                    .setXenon(0.05, 50).setHeat(2.0).setMeltingPoint(5211).setTint(0xFFEE00).setPellet(() -> ModItems.RBMK_PELLET_HEAUS.get()));
    public static final RegistrySupplier<Item> RBMK_FUEL_HEN = registerItem("rbmk_fuel_hen", 
            () -> new RBMKRodItem("Highly Enriched Neptunium-237 Rod", new Item.Properties())
                    .setYield(100_000_000).setStats(40).setFunction(RBMKRodItem.EnumBurnFunc.SQUARE_ROOT)
                    .setMeltingPoint(2800).setNeutronTypes(NType.FAST, NType.FAST).setTint(0x757E73).setPellet(() -> ModItems.RBMK_PELLET_HEN.get()));
    public static final RegistrySupplier<Item> RBMK_FUEL_HEP241 = registerItem("rbmk_fuel_hep241", 
            () -> new RBMKRodItem("High-Enriched Plutonium-241 Rod", new Item.Properties())
                    .setYield(100_000_000).setStats(40).setFunction(RBMKRodItem.EnumBurnFunc.LINEAR)
                    .setHeat(1.75).setMeltingPoint(2744).setTint(0x656E6B).setPellet(() -> ModItems.RBMK_PELLET_HEP241.get()));
    public static final RegistrySupplier<Item> RBMK_FUEL_HES = registerItem("rbmk_fuel_hes", 
            () -> new RBMKRodItem("Highly Enriched Schrabidium-326 Rod", new Item.Properties())
                    .setYield(100_000_000).setStats(90).setFunction(RBMKRodItem.EnumBurnFunc.LINEAR)
                    .setDepletionFunction(RBMKRodItem.EnumDepleteFunc.LINEAR)
                    .setHeat(1.75).setMeltingPoint(3000).setTint(0x2D9A94).setPellet(() -> ModItems.RBMK_PELLET_HES.get()));
    public static final RegistrySupplier<Item> RBMK_FUEL_HEU233 = registerItem("rbmk_fuel_heu233", 
            () -> new RBMKRodItem("Highly Enriched Uranium-233 Rod", new Item.Properties())
                    .setYield(100_000_000).setStats(27.5).setFunction(RBMKRodItem.EnumBurnFunc.LINEAR)
                    .setHeat(1.25).setMeltingPoint(2865).setTint(0x868D82).setPellet(() -> ModItems.RBMK_PELLET_HEU233.get()));
    public static final RegistrySupplier<Item> RBMK_FUEL_LEA = registerItem("rbmk_fuel_lea", 
            () -> new RBMKRodItem("Low Enriched Americium-242 Rod", new Item.Properties())
                    .setYield(100_000_000).setStats(60, 10).setFunction(RBMKRodItem.EnumBurnFunc.SQUARE_ROOT)
                    .setDepletionFunction(RBMKRodItem.EnumDepleteFunc.RAISING_SLOPE)
                    .setHeat(1.5).setMeltingPoint(2386).setTint(0xA88A8F).setPellet(() -> ModItems.RBMK_PELLET_LEA.get()));
    public static final RegistrySupplier<Item> RBMK_FUEL_LEAUS = registerItem("rbmk_fuel_leaus", 
            () -> new RBMKRodItem("Low Enriched Australium (Tasmanite) Rod", new Item.Properties())
                    .setYield(100_000_000).setStats(30).setFunction(RBMKRodItem.EnumBurnFunc.SIGMOID)
                    .setDepletionFunction(RBMKRodItem.EnumDepleteFunc.LINEAR).setXenon(0.05, 50)
                    .setHeat(1.5).setMeltingPoint(7029).setTint(0xFFEE00).setPellet(() -> ModItems.RBMK_PELLET_LEAUS.get()));
    public static final RegistrySupplier<Item> RBMK_FUEL_LES = registerItem("rbmk_fuel_les", 
            () -> new RBMKRodItem("Low Enriched Schrabidium-326 Rod", new Item.Properties())
                    .setYield(100_000_000).setStats(50).setFunction(RBMKRodItem.EnumBurnFunc.SQUARE_ROOT)
                    .setHeat(1.25).setMeltingPoint(2500).setNeutronTypes(NType.SLOW, NType.SLOW).setTint(0x2D9A94).setPellet(() -> ModItems.RBMK_PELLET_LES.get()));
    public static final RegistrySupplier<Item> RBMK_FUEL_MEA = registerItem("rbmk_fuel_mea", 
            () -> new RBMKRodItem("Medium Enriched Americium-242 Rod", new Item.Properties())
                    .setYield(100_000_000).setStats(35, 20).setFunction(RBMKRodItem.EnumBurnFunc.ARCH)
                    .setHeat(1.75).setMeltingPoint(2386).setTint(0xA88A8F).setPellet(() -> ModItems.RBMK_PELLET_MEA.get()));
    public static final RegistrySupplier<Item> RBMK_FUEL_MEN = registerItem("rbmk_fuel_men", 
            () -> new RBMKRodItem("Medium Enriched Neptunium-237 Rod", new Item.Properties())
                    .setYield(100_000_000).setStats(30).setFunction(RBMKRodItem.EnumBurnFunc.SQUARE_ROOT)
                    .setDepletionFunction(RBMKRodItem.EnumDepleteFunc.RAISING_SLOPE)
                    .setHeat(0.75).setMeltingPoint(2800).setNeutronTypes(NType.ANY, NType.FAST).setTint(0x757E73).setPellet(() -> ModItems.RBMK_PELLET_MEN.get()));
    public static final RegistrySupplier<Item> RBMK_FUEL_MEP = registerItem("rbmk_fuel_mep", 
            () -> new RBMKRodItem("Medium Enriched Plutonium-239 Rod", new Item.Properties())
                    .setYield(100_000_000).setStats(35).setFunction(RBMKRodItem.EnumBurnFunc.SQUARE_ROOT)
                    .setMeltingPoint(2744).setTint(0x656E6B).setPellet(() -> ModItems.RBMK_PELLET_MEP.get()));
    public static final RegistrySupplier<Item> RBMK_FUEL_MES = registerItem("rbmk_fuel_mes", 
            () -> new RBMKRodItem("Medium Enriched Schrabidium-326 Rod", new Item.Properties())
                    .setYield(100_000_000).setStats(75).setFunction(RBMKRodItem.EnumBurnFunc.ARCH)
                    .setHeat(1.5).setMeltingPoint(2750).setTint(0x2D9A94).setPellet(() -> ModItems.RBMK_PELLET_MES.get()));
    public static final RegistrySupplier<Item> RBMK_FUEL_MEU = registerItem("rbmk_fuel_meu", 
            () -> new RBMKRodItem("Medium Enriched Uranium-235 Rod", new Item.Properties())
                    .setYield(100_000_000).setStats(20).setFunction(RBMKRodItem.EnumBurnFunc.LOG_TEN)
                    .setDepletionFunction(RBMKRodItem.EnumDepleteFunc.RAISING_SLOPE)
                    .setHeat(0.65).setMeltingPoint(2865).setTint(0x868D82).setPellet(() -> ModItems.RBMK_PELLET_MEU.get()));
    public static final RegistrySupplier<Item> RBMK_FUEL_PO210BE = registerItem("rbmk_fuel_po210be", 
            () -> new RBMKRodItem("Polonium-210 & Beryllium Neutron Source", new Item.Properties())
                    .setYield(25_000_000).setStats(0, 50).setFunction(RBMKRodItem.EnumBurnFunc.PASSIVE)
                    .setDepletionFunction(RBMKRodItem.EnumDepleteFunc.LINEAR).setXenon(0.0, 50)
                    .setHeat(0.1).setDiffusion(0.05).setMeltingPoint(1287)
                    .setNeutronTypes(NType.SLOW, NType.SLOW).setTint(0x563A26).setPellet(() -> ModItems.RBMK_PELLET_PO210BE.get()));
    public static final RegistrySupplier<Item> RBMK_FUEL_PU238BE = registerItem("rbmk_fuel_pu238be", 
            () -> new RBMKRodItem("Plutonium-238 & Beryllium Neutron Source", new Item.Properties())
                    .setYield(50_000_000).setStats(40, 40).setFunction(RBMKRodItem.EnumBurnFunc.SQUARE_ROOT)
                    .setHeat(0.1).setDiffusion(0.05).setMeltingPoint(1287)
                    .setNeutronTypes(NType.SLOW, NType.SLOW).setTint(0x656E6B).setPellet(() -> ModItems.RBMK_PELLET_PU238BE.get()));
    public static final RegistrySupplier<Item> RBMK_FUEL_RA226BE = registerItem("rbmk_fuel_ra226be", 
            () -> new RBMKRodItem("Radium-226 & Beryllium Neutron Source", new Item.Properties())
                    .setYield(100_000_000).setStats(0, 20).setFunction(RBMKRodItem.EnumBurnFunc.PASSIVE)
                    .setDepletionFunction(RBMKRodItem.EnumDepleteFunc.LINEAR).setXenon(0.0, 50)
                    .setHeat(0.035).setDiffusion(0.5).setMeltingPoint(700)
                    .setNeutronTypes(NType.SLOW, NType.SLOW).setTint(0xB3B6AD).setPellet(() -> ModItems.RBMK_PELLET_RA226BE.get()));
    public static final RegistrySupplier<Item> RBMK_FUEL_THMEU = registerItem("rbmk_fuel_thmeu", 
            () -> new RBMKRodItem("Thorium with MEU Driver Fuel Rod", new Item.Properties())
                    .setYield(100_000_000).setStats(20).setFunction(RBMKRodItem.EnumBurnFunc.PLATEU)
                    .setDepletionFunction(RBMKRodItem.EnumDepleteFunc.BOOSTED_SLOPE)
                    .setHeat(0.65).setMeltingPoint(3350).setTint(0x665448).setPellet(() -> ModItems.RBMK_PELLET_THMEU.get()));
    public static final RegistrySupplier<Item> RBMK_FUEL_UEU = registerItem("rbmk_fuel_ueu", 
            () -> new RBMKRodItem("Unenriched Uranium Rod", new Item.Properties())
                    .setYield(100_000_000).setStats(15).setFunction(RBMKRodItem.EnumBurnFunc.LOG_TEN)
                    .setDepletionFunction(RBMKRodItem.EnumDepleteFunc.RAISING_SLOPE)
                    .setHeat(0.65).setMeltingPoint(2865).setTint(0x868D82).setPellet(() -> ModItems.RBMK_PELLET_UEU.get()));
    public static final RegistrySupplier<Item> RBMK_FUEL_UZH = registerItem("rbmk_fuel_uzh", 
            () -> new RBMKRodItem("Uranium Zirconium Hydride Rod", new Item.Properties())
                    .setYield(50_000_000).setStats(30).setFunction(RBMKRodItem.EnumBurnFunc.LOG_TEN)
                    .setDepletionFunction(RBMKRodItem.EnumDepleteFunc.GENTLE_SLOPE)
                    .setHeat(0.75).setHeatCoeff(1000, 500).setDiffusion(0.1)
                    .setMeltingPoint(1845).setTint(0x7077AF).setPellet(() -> ModItems.RBMK_PELLET_UZH.get()));
    public static final RegistrySupplier<Item> RBMK_FUEL_ZFB_AM_MIX = registerItem("rbmk_fuel_zfb_am_mix", 
            () -> new RBMKRodItem("Zirconium Fast Breeder - HEP-241#MEA Rod", new Item.Properties())
                    .setYield(50_000_000).setStats(20).setFunction(RBMKRodItem.EnumBurnFunc.LINEAR)
                    .setHeat(1.75).setMeltingPoint(2744).setTint(0xAAA36A).setPellet(() -> ModItems.RBMK_PELLET_ZFB_AM_MIX.get()));
    public static final RegistrySupplier<Item> RBMK_FUEL_ZFB_BISMUTH = registerItem("rbmk_fuel_zfb_bismuth", 
            () -> new RBMKRodItem("Zirconium Fast Breeder - LEU/HEP-241#Bi Rod", new Item.Properties())
                    .setYield(50_000_000).setStats(20).setFunction(RBMKRodItem.EnumBurnFunc.SQUARE_ROOT)
                    .setHeat(1.75).setMeltingPoint(2744).setTint(0xAAA36A).setPellet(() -> ModItems.RBMK_PELLET_ZFB_BISMUTH.get()));
    public static final RegistrySupplier<Item> RBMK_FUEL_ZFB_PU241 = registerItem("rbmk_fuel_zfb_pu241", 
            () -> new RBMKRodItem("Zirconium Fast Breeder - HEU-235/HEP-240#Pu-241 Rod", new Item.Properties())
                    .setYield(50_000_000).setStats(20).setFunction(RBMKRodItem.EnumBurnFunc.SQUARE_ROOT)
                    .setMeltingPoint(2865).setTint(0xAAA36A).setPellet(() -> ModItems.RBMK_PELLET_ZFB_PU241.get()));

    // Original ModItems.java:3314 - the debug rod, no pellet (cannot be disassembled).
    public static final RegistrySupplier<Item> RBMK_FUEL_TEST = registerItem("rbmk_fuel_test", 
            () -> new RBMKRodItem("THE VOICES", new Item.Properties())
                    .setYield(1_000_000).setStats(100).setFunction(RBMKRodItem.EnumBurnFunc.EXPERIMENTAL)
                    .setHeat(1.0).setMeltingPoint(100_000));

    public static final RegistrySupplier<Item> RBMK_PELLET_BALEFIRE = registerItem("rbmk_pellet_balefire", 
            () -> new RBMKPelletItem(new Item.Properties()).disableXenon().setFullName("Draconic Flames")
                    .setYield(100_000_000).setReactivity(100).setHeat(3.0).setMeltingPoint(3652).setTint(0xB2FF1B));
    public static final RegistrySupplier<Item> RBMK_PELLET_BALEFIRE_GOLD = registerItem("rbmk_pellet_balefire_gold", 
            () -> new RBMKPelletItem(new Item.Properties()).disableXenon().setFullName("Antihydrogen in a Magnetized Gold-198 Lattice")
                    .setYield(100_000_000).setReactivity(50).setMeltingPoint(2000).setTint(0xDC9613));
    public static final RegistrySupplier<Item> RBMK_PELLET_DRX = registerItem("rbmk_pellet_drx", 
            () -> new RBMKPelletItem(new Item.Properties()).setFullName("can't you hear, can't you hear the thunder?")
                    .setYield(10_000_000).setReactivity(1000).setHeat(0.1).setMeltingPoint(100_000).setTint(0xD77276));
    public static final RegistrySupplier<Item> RBMK_PELLET_FLASHLEAD = registerItem("rbmk_pellet_flashlead", 
            () -> new RBMKPelletItem(new Item.Properties()).disableXenon().setFullName("Antihydrogen confined by a Magnetized Gold-198 and Lead-209 Lattice")
                    .setYield(250_000_000).setReactivity(40).setMeltingPoint(2050).setTint(0x7B7B87));
    public static final RegistrySupplier<Item> RBMK_PELLET_HEA241 = registerItem("rbmk_pellet_hea241", 
            () -> new RBMKPelletItem(new Item.Properties()).setFullName("Highly Enriched Americium-241")
                    .setYield(100_000_000).setReactivity(65).setHeat(1.85).setMeltingPoint(2386)
                    .setNeutronTypes(NType.FAST, NType.FAST).setTint(0xA88A8F));
    public static final RegistrySupplier<Item> RBMK_PELLET_HEA242 = registerItem("rbmk_pellet_hea242", 
            () -> new RBMKPelletItem(new Item.Properties()).setFullName("Highly Enriched Americium-242")
                    .setYield(100_000_000).setReactivity(45).setHeat(2.0).setMeltingPoint(2386).setTint(0xA88A8F));
    public static final RegistrySupplier<Item> RBMK_PELLET_HEAUS = registerItem("rbmk_pellet_heaus", 
            () -> new RBMKPelletItem(new Item.Properties()).setFullName("Highly Enriched Australium (Ayerite)")
                    .setYield(100_000_000).setReactivity(35).setXenon(0.05, 50).setHeat(1.5).setMeltingPoint(5211).setTint(0xFFEE00));
    public static final RegistrySupplier<Item> RBMK_PELLET_HEN = registerItem("rbmk_pellet_hen", 
            () -> new RBMKPelletItem(new Item.Properties()).setFullName("Highly Enriched Neptunium-237")
                    .setYield(100_000_000).setReactivity(40).setMeltingPoint(2800)
                    .setNeutronTypes(NType.FAST, NType.FAST).setTint(0x757E73));
    public static final RegistrySupplier<Item> RBMK_PELLET_HEP241 = registerItem("rbmk_pellet_hep241", 
            () -> new RBMKPelletItem(new Item.Properties()).setFullName("Highly Enriched Plutonium-241")
                    .setYield(100_000_000).setReactivity(40).setHeat(1.75).setMeltingPoint(2744).setTint(0x656E6B));
    public static final RegistrySupplier<Item> RBMK_PELLET_HES = registerItem("rbmk_pellet_hes", 
            () -> new RBMKPelletItem(new Item.Properties()).setFullName("Highly Enriched Schrabidium-326")
                    .setYield(100_000_000).setReactivity(90).setHeat(1.75).setMeltingPoint(3000).setTint(0x2D9A94));
    public static final RegistrySupplier<Item> RBMK_PELLET_HEU233 = registerItem("rbmk_pellet_heu233", 
            () -> new RBMKPelletItem(new Item.Properties()).setFullName("Highly Enriched Uranium-233")
                    .setYield(100_000_000).setReactivity(27.5).setHeat(1.25).setMeltingPoint(2865).setTint(0x868D82));
    public static final RegistrySupplier<Item> RBMK_PELLET_LEA = registerItem("rbmk_pellet_lea", 
            () -> new RBMKPelletItem(new Item.Properties()).setFullName("Low Enriched Americium-242")
                    .setYield(100_000_000).setReactivity(60).setHeat(1.5).setMeltingPoint(2386).setTint(0xA88A8F));
    public static final RegistrySupplier<Item> RBMK_PELLET_LEAUS = registerItem("rbmk_pellet_leaus", 
            () -> new RBMKPelletItem(new Item.Properties()).setFullName("Low Enriched Australium (Tasmanite)")
                    .setYield(100_000_000).setReactivity(30).setXenon(0.05, 50).setHeat(1.5).setMeltingPoint(7029).setTint(0xFFEE00));
    public static final RegistrySupplier<Item> RBMK_PELLET_LES = registerItem("rbmk_pellet_les", 
            () -> new RBMKPelletItem(new Item.Properties()).setFullName("Low Enriched Schrabidium-326")
                    .setYield(100_000_000).setReactivity(50).setHeat(1.25).setMeltingPoint(2500)
                    .setNeutronTypes(NType.SLOW, NType.SLOW).setTint(0x2D9A94));
    public static final RegistrySupplier<Item> RBMK_PELLET_MEA = registerItem("rbmk_pellet_mea", 
            () -> new RBMKPelletItem(new Item.Properties()).setFullName("Medium Enriched Americium-242")
                    .setYield(100_000_000).setReactivity(35).setHeat(1.75).setMeltingPoint(2386).setTint(0xA88A8F));
    public static final RegistrySupplier<Item> RBMK_PELLET_MEN = registerItem("rbmk_pellet_men", 
            () -> new RBMKPelletItem(new Item.Properties()).setFullName("Medium Enriched Neptunium-237")
                    .setYield(100_000_000).setReactivity(30).setHeat(0.75).setMeltingPoint(2800)
                    .setNeutronTypes(NType.ANY, NType.FAST).setTint(0x757E73));
    public static final RegistrySupplier<Item> RBMK_PELLET_MEP = registerItem("rbmk_pellet_mep", 
            () -> new RBMKPelletItem(new Item.Properties()).setFullName("Medium Enriched Plutonium-239")
                    .setYield(100_000_000).setReactivity(35).setMeltingPoint(2744).setTint(0x656E6B));
    public static final RegistrySupplier<Item> RBMK_PELLET_MES = registerItem("rbmk_pellet_mes", 
            () -> new RBMKPelletItem(new Item.Properties()).setFullName("Medium Enriched Schrabidium-326")
                    .setYield(100_000_000).setReactivity(75).setHeat(1.5).setMeltingPoint(2750).setTint(0x2D9A94));
    public static final RegistrySupplier<Item> RBMK_PELLET_MEU = registerItem("rbmk_pellet_meu", 
            () -> new RBMKPelletItem(new Item.Properties()).setFullName("Medium Enriched Uranium-235")
                    .setYield(100_000_000).setReactivity(20).setHeat(0.65).setMeltingPoint(2865).setTint(0x868D82));
    public static final RegistrySupplier<Item> RBMK_PELLET_PO210BE = registerItem("rbmk_pellet_po210be", 
            () -> new RBMKPelletItem(new Item.Properties()).disableXenon().setFullName("Polonium-210 & Beryllium Neutron Source")
                    .setYield(25_000_000).setReactivity(0).setXenon(0.0, 50).setHeat(0.1).setDiffusion(0.05)
                    .setMeltingPoint(1287).setNeutronTypes(NType.SLOW, NType.SLOW).setTint(0x563A26));
    public static final RegistrySupplier<Item> RBMK_PELLET_PU238BE = registerItem("rbmk_pellet_pu238be", 
            () -> new RBMKPelletItem(new Item.Properties()).setFullName("Plutonium-238 & Beryllium Neutron Source")
                    .setYield(50_000_000).setReactivity(40).setHeat(0.1).setDiffusion(0.05)
                    .setMeltingPoint(1287).setNeutronTypes(NType.SLOW, NType.SLOW).setTint(0x656E6B));
    public static final RegistrySupplier<Item> RBMK_PELLET_RA226BE = registerItem("rbmk_pellet_ra226be", 
            () -> new RBMKPelletItem(new Item.Properties()).disableXenon().setFullName("Radium-226 & Beryllium Neutron Source")
                    .setYield(100_000_000).setReactivity(0).setXenon(0.0, 50).setHeat(0.035).setDiffusion(0.5)
                    .setMeltingPoint(700).setNeutronTypes(NType.SLOW, NType.SLOW).setTint(0xB3B6AD));
    public static final RegistrySupplier<Item> RBMK_PELLET_THMEU = registerItem("rbmk_pellet_thmeu", 
            () -> new RBMKPelletItem(new Item.Properties()).setFullName("Thorium with MEU Driver Fuel")
                    .setYield(100_000_000).setReactivity(20).setHeat(0.65).setMeltingPoint(3350).setTint(0x665448));
    public static final RegistrySupplier<Item> RBMK_PELLET_UEU = registerItem("rbmk_pellet_ueu", 
            () -> new RBMKPelletItem(new Item.Properties()).setFullName("Unenriched Uranium")
                    .setYield(100_000_000).setReactivity(15).setHeat(0.65).setMeltingPoint(2865).setTint(0x868D82));
    public static final RegistrySupplier<Item> RBMK_PELLET_UZH = registerItem("rbmk_pellet_uzh", 
            () -> new RBMKPelletItem(new Item.Properties()).setFullName("Uranium Zirconium Hydride")
                    .setYield(50_000_000).setReactivity(30).setHeat(0.75).setDiffusion(0.1).setMeltingPoint(1845).setTint(0x7077AF));
    public static final RegistrySupplier<Item> RBMK_PELLET_ZFB_AM_MIX = registerItem("rbmk_pellet_zfb_am_mix", 
            () -> new RBMKPelletItem(new Item.Properties()).setFullName("Zirconium Fast Breeder - HEP-241#MEA")
                    .setYield(50_000_000).setReactivity(20).setHeat(1.75).setMeltingPoint(2744).setTint(0xAAA36A));
    public static final RegistrySupplier<Item> RBMK_PELLET_ZFB_BISMUTH = registerItem("rbmk_pellet_zfb_bismuth", 
            () -> new RBMKPelletItem(new Item.Properties()).setFullName("Zirconium Fast Breeder - LEU/HEP-241#Bi")
                    .setYield(50_000_000).setReactivity(20).setHeat(1.75).setMeltingPoint(2744).setTint(0xAAA36A));
    public static final RegistrySupplier<Item> RBMK_PELLET_ZFB_PU241 = registerItem("rbmk_pellet_zfb_pu241", 
            () -> new RBMKPelletItem(new Item.Properties()).setFullName("Zirconium Fast Breeder - HEU-235/HEP-240#Pu-241")
                    .setYield(50_000_000).setReactivity(20).setMeltingPoint(2865).setTint(0xAAA36A));
    public static final RegistrySupplier<Item> RBMK_TOOL = registerItem("rbmk_tool", 
            () -> new com.hbm_m.item.rbmk.RBMKToolItem(new Item.Properties().stacksTo(1)));
    public static final RegistrySupplier<Item> REACHER = registerItem("reacher", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> REACTOR_CORE = registerItem("reactor_core", () -> new Item(new Item.Properties()));
    // ── Spulen des Teilchenbeschleunigers (Original: ItemPACoil mit vier Metadaten) ──
    public static final RegistrySupplier<Item> PA_COIL_GOLD = registerItem("pa_coil_gold", 
            () -> new com.hbm_m.item.machine.ItemPACoil(new Item.Properties(),
                    com.hbm_m.item.machine.ItemPACoil.CoilType.GOLD));
    public static final RegistrySupplier<Item> PA_COIL_NIOBIUM = registerItem("pa_coil_niobium", 
            () -> new com.hbm_m.item.machine.ItemPACoil(new Item.Properties(),
                    com.hbm_m.item.machine.ItemPACoil.CoilType.NIOBIUM));
    public static final RegistrySupplier<Item> PA_COIL_BSCCO = registerItem("pa_coil_bscco", 
            () -> new com.hbm_m.item.machine.ItemPACoil(new Item.Properties(),
                    com.hbm_m.item.machine.ItemPACoil.CoilType.BSCCO));
    public static final RegistrySupplier<Item> PA_COIL_CHLOROPHYTE = registerItem("pa_coil_chlorophyte", 
            () -> new com.hbm_m.item.machine.ItemPACoil(new Item.Properties(),
                    com.hbm_m.item.machine.ItemPACoil.CoilType.CHLOROPHYTE));

    /** Original: {@code ItemReactorSensor} - bindet per Rechtsklick einen Forschungsreaktor. */
    public static final RegistrySupplier<Item> REACTOR_SENSOR = registerItem("reactor_sensor", 
            () -> new com.hbm_m.item.machine.ItemReactorSensor(new Item.Properties()));
    public static final RegistrySupplier<Item> REBAR_PLACER = registerItem("rebar_placer", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> REDSTONE_SWORD = registerItem("redstone_sword", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> RING_PULL = registerItem("ring_pull", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> RING_STARMETAL = registerItem("ring_starmetal", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> ROBES_BOOTS = registerItem("robes_boots", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> ROBES_HELMET = registerItem("robes_helmet", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> ROBES_LEGS = registerItem("robes_legs", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> ROBES_PLATE = registerItem("robes_plate", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> ROCKET_FUEL = registerItem("rocket_fuel", () -> new FuelItem(new Item.Properties(), 6400));
    public static final RegistrySupplier<Item> ROD_DUAL_EMPTY = registerItem("rod_dual_empty", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> ROD_EMPTY = registerItem("rod_empty", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> ROD_OF_DISCORD = registerItem("rod_of_discord", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> ROD_QUAD_EMPTY = registerItem("rod_quad_empty", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> RPA_BOOTS = registerItem("rpa_boots", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> RPA_HELMET = registerItem("rpa_helmet", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> RPA_LEGS = registerItem("rpa_legs", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> RPA_PLATE = registerItem("rpa_plate", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> RTG_UNIT = registerItem("rtg_unit", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> RTTY_PAGER = registerItem("rtty_pager", () -> new Item(new Item.Properties()));
    // Runes (Catalyst Matrix): ориг. ItemCustomLore().setEffect().setMaxStackSize(1) —
    // зачарованный блеск (isFoil) и стак в 1 предмет.
    public static final RegistrySupplier<Item> RUNE_BLANK = registerItem("rune_blank", () -> new FoilItem(new Item.Properties().stacksTo(1)));
    public static final RegistrySupplier<Item> RUNE_DAGAZ = registerItem("rune_dagaz", () -> new FoilItem(new Item.Properties().stacksTo(1)));
    public static final RegistrySupplier<Item> RUNE_HAGALAZ = registerItem("rune_hagalaz", () -> new FoilItem(new Item.Properties().stacksTo(1)));
    public static final RegistrySupplier<Item> RUNE_ISA = registerItem("rune_isa", () -> new FoilItem(new Item.Properties().stacksTo(1)));
    public static final RegistrySupplier<Item> RUNE_JERA = registerItem("rune_jera", () -> new FoilItem(new Item.Properties().stacksTo(1)));
    public static final RegistrySupplier<Item> RUNE_THURISAZ = registerItem("rune_thurisaz", () -> new FoilItem(new Item.Properties().stacksTo(1)));
    public static final RegistrySupplier<Item> SAFETY_FUSE = registerItem("safety_fuse", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> SAT_CHIP = registerItem("sat_chip", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> SAT_COORD = registerItem("sat_coord", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> SAT_DESIGNATOR = registerItem("sat_designator", 
            () -> new com.hbm_m.item.designator.ItemSatDesignator(new Item.Properties()));
    public static final RegistrySupplier<Item> SAT_GERALD = registerItem("sat_gerald", 
            () -> new com.hbm_m.item.satellite.ItemSatChip(new Item.Properties()));
    public static final RegistrySupplier<Item> SAT_HEAD_SCANNER = registerItem("sat_head_scanner", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> SAT_INTERFACE = registerItem("sat_interface", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> SAT_LUNAR_MINER = registerItem("sat_lunar_miner", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> SAT_MINER = registerItem("sat_miner", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> SAT_RELAY = registerItem("sat_relay", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> SAWBLADE = registerItem("sawblade", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> SCHNITZEL_VEGAN = registerItem("schnitzel_vegan", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> SCHRABIDIUM_AXE = registerItem("schrabidium_axe", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> SCHRABIDIUM_BOOTS = registerItem("schrabidium_boots", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> SCHRABIDIUM_HAMMER = registerItem("schrabidium_hammer", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> SCHRABIDIUM_HELMET = registerItem("schrabidium_helmet", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> SCHRABIDIUM_HOE = registerItem("schrabidium_hoe", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> SCHRABIDIUM_LEGS = registerItem("schrabidium_legs", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> SCHRABIDIUM_PICKAXE = registerItem("schrabidium_pickaxe", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> SCHRABIDIUM_PLATE = registerItem("schrabidium_plate", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> SCHRABIDIUM_SHOVEL = registerItem("schrabidium_shovel", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> SCHRABIDIUM_SWORD = registerItem("schrabidium_sword", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> SCRAPS = registerItem("scraps", () -> new FuelItem(new Item.Properties(), 50));
    public static final RegistrySupplier<Item> SCREWDRIVER_DESH = registerItem("screwdriver_desh", () -> new ScrewdriverItem(new Item.Properties().stacksTo(1)));
    public static final RegistrySupplier<Item> SCRUMPY = registerItem("scrumpy", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> SECURITY_LEGS = registerItem("security_legs", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> SECURITY_PLATE = registerItem("security_plate", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> SEG_10 = registerItem("seg_10", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> SEG_15 = registerItem("seg_15", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> SEG_20 = registerItem("seg_20", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> SERUM = registerItem("serum", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> SERVO_SET = registerItem("servo_set", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> SERVO_SET_DESH = registerItem("servo_set_desh", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> SETTINGS_TOOL = registerItem("settings_tool", () -> new com.hbm_m.item.tool.ItemSettingsTool(new Item.Properties().stacksTo(1)));
    public static final RegistrySupplier<Item> SHACKLES = registerItem("shackles", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> SHIMMER_AXE = registerItem("shimmer_axe", WIP,
            () -> new com.hbm_m.item.tool.ItemShimmerWeapon(true, new Item.Properties()));
    public static final RegistrySupplier<Item> SHIMMER_AXE_HEAD = registerItem("shimmer_axe_head", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> SHIMMER_HANDLE = registerItem("shimmer_handle", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> SHIMMER_HEAD = registerItem("shimmer_head", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> SHIMMER_SLEDGE = registerItem("shimmer_sledge", WIP,
            () -> new com.hbm_m.item.tool.ItemShimmerWeapon(false, new Item.Properties()));
    public static final RegistrySupplier<Item> SINGULARITY = registerItem("singularity", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> SIOX = registerItem("siox", () -> new ItemSimpleConsumable(new Item.Properties(), ModConsumables::useSiox));
    public static final RegistrySupplier<Item> SIPHON = registerItem("siphon", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> SMASHING_HAMMER = registerItem("smashing_hammer", () -> new Item(new Item.Properties()));
    // Значения горения — оригинальный FuelHandler 1.7.10 (одна операция пресса = 200 тиков)
    public static final RegistrySupplier<Item> SOLID_FUEL = registerItem("solid_fuel", () -> new FuelItem(new Item.Properties(), 3200));
    public static final RegistrySupplier<Item> SOLID_FUEL_BF = registerItem("solid_fuel_bf", () -> new FuelItem(new Item.Properties(), 32000));
    public static final RegistrySupplier<Item> SOLID_FUEL_PRESTO = registerItem("solid_fuel_presto", () -> new FuelItem(new Item.Properties(), 8000));
    public static final RegistrySupplier<Item> SOLID_FUEL_PRESTO_BF = registerItem("solid_fuel_presto_bf", () -> new FuelItem(new Item.Properties(), 80000));
    public static final RegistrySupplier<Item> SOLID_FUEL_PRESTO_TRIPLET = registerItem("solid_fuel_presto_triplet", () -> new FuelItem(new Item.Properties(), 40000));
    public static final RegistrySupplier<Item> SOLID_FUEL_PRESTO_TRIPLET_BF = registerItem("solid_fuel_presto_triplet_bf", () -> new FuelItem(new Item.Properties(), 400000));
    public static final RegistrySupplier<Item> SOLINIUM_CORE = registerItem("solinium_core", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> SOLINIUM_IGNITER = registerItem("solinium_igniter", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> SOLINIUM_KIT = registerItem("solinium_kit", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> SOLINIUM_PROPELLANT = registerItem("solinium_propellant", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> SOPSIGN = registerItem("sopsign", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> SPAWN_DUCK = registerItem("spawn_duck", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> SPAWN_UFO = registerItem("spawn_ufo", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> SPAWN_WORM = registerItem("spawn_worm", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> SPHERE_STEEL = registerItem("sphere_steel", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> SPIDER_MILK = registerItem("spider_milk", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> SPONGEBOB_MACARONI = registerItem("spongebob_macaroni", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> STAMP_357 = registerItem("stamp_357", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> STAMP_44 = registerItem("stamp_44", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> STAMP_50 = registerItem("stamp_50", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> STAMP_9 = registerItem("stamp_9", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> STARMETAL_LEGS = registerItem("starmetal_legs", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> STARMETAL_PLATE = registerItem("starmetal_plate", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> STATIC_SANDWICH = registerItem("static_sandwich", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> STEALTH_BOY = registerItem("stealth_boy", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> STEAMSUIT_BOOTS = registerItem("steamsuit_boots", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> STEAMSUIT_HELMET = registerItem("steamsuit_helmet", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> STEAMSUIT_LEGS = registerItem("steamsuit_legs", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> STEAMSUIT_PLATE = registerItem("steamsuit_plate", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> STEEL_LEGS = registerItem("steel_legs", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> STEEL_PLATE = registerItem("steel_plate", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> STICK_C4 = registerItem("stick_c4", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> STICK_DYNAMITE = registerItem("stick_dynamite", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> STICK_DYNAMITE_FISHING = registerItem("stick_dynamite_fishing", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> STICK_SEMTEX = registerItem("stick_semtex", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> STICK_TNT = registerItem("stick_tnt", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> STOPSIGN = registerItem("stopsign", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> STRUCTURE_CUSTOMMACHINE = registerItem("structure_custommachine", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> SURVEY_SCANNER = registerItem("survey_scanner", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> SYRINGE_ANTIDOTE = registerItem("syringe_antidote", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> SYRINGE_AWESOME = registerItem("syringe_awesome", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> SYRINGE_EMPTY = registerItem("syringe_empty", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> SYRINGE_METAL_EMPTY = registerItem("syringe_metal_empty", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> SYRINGE_METAL_MEDX = registerItem("syringe_metal_medx", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> SYRINGE_METAL_PSYCHO = registerItem("syringe_metal_psycho", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> SYRINGE_METAL_STIMPAK = registerItem("syringe_metal_stimpak", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> SYRINGE_METAL_SUPER = registerItem("syringe_metal_super", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> SYRINGE_MKUNICORN = registerItem("syringe_mkunicorn", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> SYRINGE_POISON = registerItem("syringe_poison", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> SYRINGE_TAINT = registerItem("syringe_taint", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> TANK_STEEL = registerItem("tank_steel", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> TAURUN_BOOTS = registerItem("taurun_boots", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> TAURUN_HELMET = registerItem("taurun_helmet", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> TAURUN_LEGS = registerItem("taurun_legs", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> TAURUN_PLATE = registerItem("taurun_plate", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> TEM_FLAKES = registerItem("tem_flakes", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> THERMO_ELEMENT = registerItem("thermo_element", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> THRUSTER_NUCLEAR = registerItem("thruster_nuclear", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> TITANIUM_FILTER = registerItem("titanium_filter", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> TITANIUM_LEGS = registerItem("titanium_legs", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> TITANIUM_PLATE = registerItem("titanium_plate", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> TRENCHMASTER_BOOTS = registerItem("trenchmaster_boots", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> TRENCHMASTER_HELMET = registerItem("trenchmaster_helmet", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> TRENCHMASTER_LEGS = registerItem("trenchmaster_legs", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> TRENCHMASTER_PLATE = registerItem("trenchmaster_plate", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> TRINITITE = registerItem("trinitite", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> TSAR_CORE = registerItem("tsar_core", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> TSAR_KIT = registerItem("tsar_kit", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> TURBINE_TUNGSTEN = registerItem("turbine_tungsten", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> TURRET_CHIP = registerItem("turret_chip", () -> new Item(new Item.Properties()));
    /** Alte MVP-Platzhaltermunition, wird von keinem Turret mehr direkt verwendet (jeder Typ hat jetzt eigene Munition). */
    public static final RegistrySupplier<Item> TURRET_AMMO = registerItem("turret_ammo", () -> new Item(new Item.Properties()));
    /** 9mm-Pistolenmunition fuer den Sentry-Turret (Original: {@code XFactory9mm}). */
    public static final RegistrySupplier<Item> AMMO_9MM_SP = registerItem("ammo_9mm_sp", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> AMMO_9MM_FMJ = registerItem("ammo_9mm_fmj", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> AMMO_9MM_JHP = registerItem("ammo_9mm_jhp", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> AMMO_9MM_AP = registerItem("ammo_9mm_ap", () -> new Item(new Item.Properties()));
    /** .50 BMG-Munition fuer den Chekhov-Turret (Original: {@code XFactory50}). */
    public static final RegistrySupplier<Item> AMMO_50_SP = registerItem("ammo_50_sp", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> AMMO_50_FMJ = registerItem("ammo_50_fmj", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> AMMO_50_JHP = registerItem("ammo_50_jhp", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> AMMO_50_AP = registerItem("ammo_50_ap", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> AMMO_50_DU = registerItem("ammo_50_du", () -> new Item(new Item.Properties()));
    /** 5.56mm-Munition fuer den Friendly-Turret (Original: {@code XFactory556mm}). */
    public static final RegistrySupplier<Item> AMMO_556_SP = registerItem("ammo_556_sp", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> AMMO_556_FMJ = registerItem("ammo_556_fmj", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> AMMO_556_JHP = registerItem("ammo_556_jhp", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> AMMO_556_AP = registerItem("ammo_556_ap", () -> new Item(new Item.Properties()));
    /** Gelenkte Rakete fuer den Richard-Turret (Original: {@code XFactoryRocket.rocket_ml}). */
    public static final RegistrySupplier<Item> ROCKET_TURRET_STANDARD = registerItem("rocket_turret_standard", () -> new Item(new Item.Properties()));
    /** Gelenkte Raketenvarianten fuer den Himars-Turret (Original: {@code ItemAmmoHIMARS}). */
    public static final RegistrySupplier<Item> ROCKET_HIMARS_STANDARD = registerItem("rocket_himars_standard", () -> new Item(new Item.Properties()));
    // The original's ItemAmmoHIMARS ships eight variants; the port was missing the two
    // large-calibre ones (LARGE / LARGE_TB, "single" and "single_tb").
    public static final RegistrySupplier<Item> ROCKET_HIMARS_SINGLE = registerItem("rocket_himars_single", 
            () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> ROCKET_HIMARS_SINGLE_TB = registerItem("rocket_himars_single_tb", 
            () -> new Item(new Item.Properties()));

    // Richard fires the full five-type ML rocket family in the original
    // (XFactoryRocket.rocket_ml -> HE / HEAT / DEMO / INC / PHOSPHORUS). Only one existed here,
    // so four of the five rocket types simply could not be loaded.
    public static final RegistrySupplier<Item> ROCKET_TURRET_HEAT = registerItem("rocket_turret_heat", 
            () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> ROCKET_TURRET_DEMO = registerItem("rocket_turret_demo", 
            () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> ROCKET_TURRET_INC = registerItem("rocket_turret_inc", 
            () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> ROCKET_TURRET_PHOSPHORUS = registerItem("rocket_turret_phosphorus", 
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> ROCKET_HIMARS_HE = registerItem("rocket_himars_he", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> ROCKET_HIMARS_LAVA = registerItem("rocket_himars_lava", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> ROCKET_HIMARS_MINI_NUKE = registerItem("rocket_himars_mini_nuke", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> ROCKET_HIMARS_WP = registerItem("rocket_himars_wp", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> ROCKET_HIMARS_THERMOBARIC = registerItem("rocket_himars_thermobaric", () -> new Item(new Item.Properties()));
    /** Uran-Munition fuer den Tauon-Turret (Original: {@code XFactoryAccelerator.tau_uranium}). */
    public static final RegistrySupplier<Item> AMMO_TAU_URANIUM = registerItem("ammo_tau_uranium", () -> new Item(new Item.Properties()));
    /** Flammenwerfer-Brennstoff fuer den Fritz-Turret (MVP: Item statt vollem Fluid-Tank, Original: Diesel-Fluid). */
    public static final RegistrySupplier<Item> AMMO_FLAME_DIESEL = registerItem("ammo_flame_diesel", () -> new Item(new Item.Properties()));
    /** Fehlende Missile-Assembly-Teile (Original: {@code ItemCustomMissilePart} mit Typ FUSELAGE/CHIP). */
    public static final RegistrySupplier<Item> MISSILE_FUSELAGE = registerItem("missile_fuselage", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> MISSILE_CHIP = registerItem("missile_chip", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> TWINKIE = registerItem("twinkie", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> ULLAPOOL_CABER = registerItem("ullapool_caber", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> UNDEFINED = registerItem("undefined", () -> new com.hbm_m.item.UndefinedItem(new Item.Properties()));
    public static final RegistrySupplier<Item> VOLCANIC_AXE = registerItem("volcanic_axe", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> VOLCANIC_PICKAXE = registerItem("volcanic_pickaxe", () -> new com.hbm_m.item.tool.ItemDepthRockBreaker(new Item.Properties()));
    public static final RegistrySupplier<Item> WAND_D = registerItem("wand_d", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> WAND_S = registerItem("wand_s", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> WARHEAD_INCENDIARY_LARGE = registerItem("warhead_incendiary_large", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> WASTE_MOX = registerItem("waste_mox", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> WASTE_PLATE_MOX = registerItem("waste_plate_mox", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> WASTE_PLATE_PU238BE = registerItem("waste_plate_pu238be", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> WASTE_PLATE_RA226BE = registerItem("waste_plate_ra226be", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> WASTE_PLATE_SA326 = registerItem("waste_plate_sa326", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> WASTE_PLATE_U233 = registerItem("waste_plate_u233", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> WASTE_PLATE_U235 = registerItem("waste_plate_u235", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> WASTE_PLATE_PU239 = registerItem("waste_plate_pu239", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> WASTE_PLUTONIUM = registerItem("waste_plutonium", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> WASTE_SCHRABIDIUM = registerItem("waste_schrabidium", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> WASTE_THORIUM = registerItem("waste_thorium", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> WASTE_URANIUM = registerItem("waste_uranium", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> WASTE_ZFB_MOX = registerItem("waste_zfb_mox", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> KEY_PIN = registerItem("key_pin", () -> new com.hbm_m.item.ItemKeyPin(new Item.Properties().stacksTo(1)));

    // ==================== Замки-ключкарты (порт TileEntityLockableBase / ItemLock) ====================

    /** Порт key_fake (1.7.10): поддельный ключ, нельзя копировать/перекодировать (canTransfer=false). */
    public static final RegistrySupplier<Item> KEY_FAKE = registerItem("key_fake", 
            () -> new com.hbm_m.item.ItemKey(new Item.Properties().stacksTo(1)) {
                @Override
                public boolean canTransfer() {
                    return false;
                }
            });

    /** Порт key_kit (1.7.10 ItemCounterfeitKeys): набор имитации ключей, выдаёт 2 key_fake с кодом замка. */
    public static final RegistrySupplier<Item> KEY_KIT = registerItem("key_kit", 
            () -> new com.hbm_m.item.ItemCounterfeitKeys(new Item.Properties().stacksTo(1)));
    public static final RegistrySupplier<Item> WATCH = registerItem("watch", () -> new Item(new Item.Properties()));

    // Siren cassettes - simplified from the original's single ItemCassette + metadata TrackType enum
    // to discrete items (matching this project's convention for other multi-variant tools), one per
    // ported alarm track (only the 7 tracks with a .ogg file ported so far get an item).
    public static final RegistrySupplier<Item> CASSETTE_AMS_SIREN = registerItem("cassette_ams_siren", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> CASSETTE_BEEP_SIREN = registerItem("cassette_beep_siren", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> CASSETTE_CLASSIC_SIREN = registerItem("cassette_classic_siren", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> CASSETTE_NOSTROMO_SIREN = registerItem("cassette_nostromo_siren", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> CASSETTE_REGULAR_SIREN = registerItem("cassette_regular_siren", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> CASSETTE_STRIDER_SIREN = registerItem("cassette_strider_siren", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> CASSETTE_SWEEP_SIREN = registerItem("cassette_sweep_siren", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> WD40 = registerItem("wd40", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> WILD_P = registerItem("wild_p", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> WINGS_LIMP = registerItem("wings_limp", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> WINGS_MURK = registerItem("wings_murk", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> WIRING_RED_COPPER = registerItem("wiring_red_copper", () -> new com.hbm_m.item.tool.ItemWiring(new Item.Properties()));
    public static final RegistrySupplier<Item> WOOD_GAVEL = registerItem("wood_gavel", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> WRENCH = registerItem("wrench", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> WRENCH_ARCHINEER = registerItem("wrench_archineer", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> WRENCH_FLIPPED = registerItem("wrench_flipped", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> XANAX = registerItem("xanax", () -> new ItemSimpleConsumable(new Item.Properties(), ModConsumables::useXanax));
    public static final RegistrySupplier<Item> ZIRCONIUM_LEGS = registerItem("zirconium_legs", () -> new Item(new Item.Properties()));

    // ==================== Заглушки диапазона вкладки Parts (id 4098–4500, раздел C отчёта) ====================
    // Простые Item-заглушки предметов, отсутствовавших в порте; registry-имена соответствуют оригиналу 1.7.10.
    // billet_les/nugget_les генерируются материалом LES_FUEL (id "les"); слиток — ручной предмет
    // INGOT_LES (id "ingot_les", как в оригинале).
    public static final RegistrySupplier<Item> INGOT_HES = registerItem("ingot_hes", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> INGOT_LES = registerItem("ingot_les", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> INGOT_REDSTONE = registerItem("ingot_redstone", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> INGOT_BORAX = registerItem("ingot_borax", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> INGOT_SODIUM = registerItem("ingot_sodium", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> INGOT_SLAG = registerItem("ingot_slag", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> COAL_COKE = registerItem("coal_coke", () -> new FuelItem(new Item.Properties(), 3200));
    public static final RegistrySupplier<Item> LIGNITE_COKE = registerItem("lignite_coke", () -> new FuelItem(new Item.Properties(), 3200));
    public static final RegistrySupplier<Item> COAL_BRIQUETTE = registerItem("coal_briquette", () -> new FuelItem(new Item.Properties(), 2000));
    public static final RegistrySupplier<Item> LIGNITE_BRIQUETTE = registerItem("lignite_briquette", () -> new FuelItem(new Item.Properties(), 1600));
    public static final RegistrySupplier<Item> SAWDUST_BRIQUETTE = registerItem("sawdust_briquette", () -> new FuelItem(new Item.Properties(), 400));
    public static final RegistrySupplier<Item> NITER = registerItem("niter", () -> new Item(new Item.Properties()));
    // "coltan_powder" занят ModItems.POWDER_COLTAN (Crushed Coltan); очищенный колтан = "powder_coltan" (как в оригинале).
    public static final RegistrySupplier<Item> POWDER_COLTAN_PURE = registerItem("powder_coltan", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> POWDER_TEKTITE = registerItem("powder_tektite", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> POWDER_IMPURE_OSMIRIDIUM = registerItem("powder_impure_osmiridium", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> POWDER_CHLOROPHYTE = registerItem("powder_chlorophyte", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> POWDER_TCALLOY = registerItem("powder_tcalloy", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> POWDER_POISON = registerItem("powder_poison", 
            () -> new LoreTooltipItem(List.of(
                    Component.translatable("tooltip.hbm_m.poison_powder.desc1").withStyle(ChatFormatting.GRAY),
                    Component.translatable("tooltip.hbm_m.poison_powder.desc2").withStyle(ChatFormatting.GRAY)),
                    new Item.Properties()));
    public static final RegistrySupplier<Item> MOONSTONE = registerItem("moonstone", () -> new Item(new Item.Properties()));
    // Кусок криолита (ориг. 4447/2, chunk_ore.cryolite) — отдельный предмет по образцу malachite/moonstone.
    public static final RegistrySupplier<Item> CRYOLITE_CHUNK = registerItem("cryolite_chunk", () -> new Item(new Item.Properties()));
    // Слиток фосфора (ориг. ingot_phosphorus) — редкий дроп ore_nether_fire (1 из 10).
    public static final RegistrySupplier<Item> INGOT_PHOSPHORUS = registerItem("ingot_phosphorus", () -> new Item(new Item.Properties()));
    // Фуллерен (ориг. 4376/5, powder_ash.fullerene) — в порте ash — отдельные предметы.
    public static final RegistrySupplier<Item> FULLERENE = registerItem("fullerene", () -> new Item(new Item.Properties()));
    // Фрагменты бедрок-руды (в оригинале — мета-предмет на 31 материал; в оригинале одна базовая текстура).
    public static final RegistrySupplier<Item> BEDROCK_ORE_FRAGMENT_COAL = registerItem("bedrock_ore_fragment_coal", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BEDROCK_ORE_FRAGMENT_LIGNITE = registerItem("bedrock_ore_fragment_lignite", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BEDROCK_ORE_FRAGMENT_IRON = registerItem("bedrock_ore_fragment_iron", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BEDROCK_ORE_FRAGMENT_GOLD = registerItem("bedrock_ore_fragment_gold", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BEDROCK_ORE_FRAGMENT_REDSTONE = registerItem("bedrock_ore_fragment_redstone", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BEDROCK_ORE_FRAGMENT_BAUXITE = registerItem("bedrock_ore_fragment_bauxite", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BEDROCK_ORE_FRAGMENT_CRYOLITE = registerItem("bedrock_ore_fragment_cryolite", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BEDROCK_ORE_FRAGMENT_URANIUM = registerItem("bedrock_ore_fragment_uranium", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BEDROCK_ORE_FRAGMENT_U238 = registerItem("bedrock_ore_fragment_u238", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BEDROCK_ORE_FRAGMENT_PO210 = registerItem("bedrock_ore_fragment_po210", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BEDROCK_ORE_FRAGMENT_TC99 = registerItem("bedrock_ore_fragment_tc99", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BEDROCK_ORE_FRAGMENT_TITANIUM = registerItem("bedrock_ore_fragment_titanium", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BEDROCK_ORE_FRAGMENT_TUNGSTEN = registerItem("bedrock_ore_fragment_tungsten", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BEDROCK_ORE_FRAGMENT_ALUMINIUM = registerItem("bedrock_ore_fragment_aluminium", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BEDROCK_ORE_FRAGMENT_BISMUTH = registerItem("bedrock_ore_fragment_bismuth", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BEDROCK_ORE_FRAGMENT_NEODYMIUM = registerItem("bedrock_ore_fragment_neodymium", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BEDROCK_ORE_FRAGMENT_NIOBIUM = registerItem("bedrock_ore_fragment_niobium", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BEDROCK_ORE_FRAGMENT_BERYLLIUM = registerItem("bedrock_ore_fragment_beryllium", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BEDROCK_ORE_FRAGMENT_COBALT = registerItem("bedrock_ore_fragment_cobalt", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BEDROCK_ORE_FRAGMENT_BORON = registerItem("bedrock_ore_fragment_boron", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BEDROCK_ORE_FRAGMENT_BORAX = registerItem("bedrock_ore_fragment_borax", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BEDROCK_ORE_FRAGMENT_ZIRCONIUM = registerItem("bedrock_ore_fragment_zirconium", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BEDROCK_ORE_FRAGMENT_SODIUM = registerItem("bedrock_ore_fragment_sodium", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BEDROCK_ORE_FRAGMENT_STRONTIUM = registerItem("bedrock_ore_fragment_strontium", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BEDROCK_ORE_FRAGMENT_LITHIUM = registerItem("bedrock_ore_fragment_lithium", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BEDROCK_ORE_FRAGMENT_SULFUR = registerItem("bedrock_ore_fragment_sulfur", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BEDROCK_ORE_FRAGMENT_FLUORITE = registerItem("bedrock_ore_fragment_fluorite", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BEDROCK_ORE_FRAGMENT_CHLOROCALCITE = registerItem("bedrock_ore_fragment_chlorocalcite", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BEDROCK_ORE_FRAGMENT_CINNABAR = registerItem("bedrock_ore_fragment_cinnabar", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BEDROCK_ORE_FRAGMENT_SILICON = registerItem("bedrock_ore_fragment_silicon", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BEDROCK_ORE_FRAGMENT_RARE_EARTH = registerItem("bedrock_ore_fragment_rare_earth", () -> new Item(new Item.Properties()));
    // Недостающие 13 фрагментов бедрок-руды (ориг. 4404, ItemAutogen FRAGMENT); текстура —
    // серый шаблон bedrock_ore_fragment.png, тинт solidColorLight материала в ClientSetup.
    public static final RegistrySupplier<Item> BEDROCK_ORE_FRAGMENT_DIAMOND = registerItem("bedrock_ore_fragment_diamond", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BEDROCK_ORE_FRAGMENT_THORIUM = registerItem("bedrock_ore_fragment_thorium", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BEDROCK_ORE_FRAGMENT_RA226 = registerItem("bedrock_ore_fragment_ra226", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BEDROCK_ORE_FRAGMENT_COPPER = registerItem("bedrock_ore_fragment_copper", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BEDROCK_ORE_FRAGMENT_LEAD = registerItem("bedrock_ore_fragment_lead", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BEDROCK_ORE_FRAGMENT_TANTALIUM = registerItem("bedrock_ore_fragment_tantalium", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BEDROCK_ORE_FRAGMENT_EMERALD = registerItem("bedrock_ore_fragment_emerald", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BEDROCK_ORE_FRAGMENT_LANTHANIUM = registerItem("bedrock_ore_fragment_lanthanium", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BEDROCK_ORE_FRAGMENT_SODALITE = registerItem("bedrock_ore_fragment_sodalite", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BEDROCK_ORE_FRAGMENT_KNO = registerItem("bedrock_ore_fragment_kno", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BEDROCK_ORE_FRAGMENT_PHOSPHORUS = registerItem("bedrock_ore_fragment_phosphorus", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BEDROCK_ORE_FRAGMENT_MOLYSITE = registerItem("bedrock_ore_fragment_molysite", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BEDROCK_ORE_FRAGMENT_ASBESTOS = registerItem("bedrock_ore_fragment_asbestos", () -> new Item(new Item.Properties()));
    // ==================== Заглушки хвоста вкладки Parts (id 4502+, оригинальный порядок вызовов registerItem) ====================
    public static final RegistrySupplier<Item> CHEMICAL_DYE = registerItem("chemical_dye", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> CRAYON = registerItem("crayon", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> PART_GENERIC = registerItem("part_generic", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> ITEM_EXPENSIVE = registerItem("item_expensive", () -> new LoreTooltipItem(List.of(
            Component.translatable("tooltip.hbm_m.item_expensive.desc").withStyle(ChatFormatting.RED)),
            new Item.Properties()));
    public static final RegistrySupplier<Item> PLANT_ITEM = registerItem("plant_item", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> CASING = registerItem("casing", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> PELLET_BUCKSHOT = registerItem("pellet_buckshot", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> PELLET_CHARGED = registerItem("pellet_charged", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> UPGRADE_MUFFLER = registerItem("upgrade_muffler", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> UPGRADE_TEMPLATE = registerItem("upgrade_template", () -> new Item(new Item.Properties()));
    // Обеднённое топливо: в оригинале ItemDepletedFuel; у u233/u235/natural отдельной текстуры нет — используется waste_uranium (как в оригинале).
    public static final RegistrySupplier<Item> WASTE_NATURAL_URANIUM = registerItem("waste_natural_uranium", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> WASTE_U233 = registerItem("waste_u233", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> WASTE_U235 = registerItem("waste_u235", () -> new Item(new Item.Properties()));
    // Остатки хвоста Parts, не имевшие заглушек: shell/pipe/bolt (в оригинале ItemAutogen),
    // plate_welded/wire_dense (автоген-формы), circuit (в оригинале ItemCircuit с вариантами).
    public static final RegistrySupplier<Item> SHELL = registerItem("shell", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> PIPE = registerItem("pipe", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BOLT = registerItem("bolt", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> PLATE_WELDED = registerItem("plate_welded", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> WIRE_DENSE = registerItem("wire_dense", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> CIRCUIT = registerItem("circuit", () -> new Item(new Item.Properties()));

    public static void init() {
        ITEMS.register();
    }

    public static RegistrySupplier<Item> registerItem(String name, java.util.function.Supplier<Item> item) {
        return ITEMS.register(name, item);
    }

    public static RegistrySupplier<Item> registerItem(String name, com.hbm_m.util.CompletionStatus status, java.util.function.Supplier<Item> item) {
        return com.hbm_m.util.CompletionTracker.mark(status, ITEMS.register(name, item));
    }
}
