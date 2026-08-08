package com.hbm_m.item;

import static com.hbm_m.lib.RefStrings.MODID;

// Класс для регистрации всех предметов мода.
// Использует DeferredRegister для отложенной регистрации. Здесь так же регистрируются моды для брони.
// Слитки регистрируются автоматически на основе перечисления ModIngots.

import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
import com.hbm_m.item.fekal_electric.ItemCreativeBattery;
import com.hbm_m.item.fekal_electric.ModBatteryItem;
import com.hbm_m.item.nuclear.WatzPelletItem;
import com.hbm_m.item.nuclear.WatzPelletType;
import com.hbm_m.item.food.ItemConserve;
import com.hbm_m.item.food.ItemEnergyDrink;
import com.hbm_m.item.food.ModFoods;
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
import com.hbm_m.item.tool.RangefinderItem;
import com.hbm_m.item.industrial.FuelItem;
import com.hbm_m.item.industrial.ItemAssemblyTemplate;
import com.hbm_m.item.industrial.ItemBlades;
import com.hbm_m.item.industrial.ItemBlueprintFolder;
import com.hbm_m.item.industrial.ItemMachineUpgrade;
import com.hbm_m.item.industrial.ItemStamp;
import com.hbm_m.item.industrial.ItemTemplateFolder;
import com.hbm_m.item.industrial.ZirnoxRodItem;
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
import com.hbm_m.item.tags_and_tiers.ModIngots;
import com.hbm_m.item.tags_and_tiers.ModPowders;
import com.hbm_m.item.tags_and_tiers.RadioactiveItem;
import com.hbm_m.item.tools_and_armor.ModArmorMaterials;
import com.hbm_m.item.tools_and_armor.ModAxeItem;
import com.hbm_m.item.tools_and_armor.ModPickaxeItem;
import com.hbm_m.item.tools_and_armor.ModShovelItem;
import com.hbm_m.item.tools_and_armor.ModToolTiers;
import com.hbm_m.item.tools_and_armor.ScrewdriverItem;
import com.hbm_m.multiblock.DoorBlockItem;
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
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.RecordItem;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.ForgeSpawnEggItem;


public class ModItems {
    // Создаем отложенный регистратор для предметов.
    // Это стандартный способ регистрации объектов в Forge.
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(MODID, Registries.ITEM);

    public static final Map<ModIngots, RegistrySupplier<Item>> INGOTS = new EnumMap<>(ModIngots.class);
    public static final Map<ModPowders, RegistrySupplier<Item>> POWDERS = new EnumMap<>(ModPowders.class);
    public static final Map<ModIngots, RegistrySupplier<Item>> INGOT_POWDERS = new EnumMap<>(ModIngots.class);
    public static final Map<ModIngots, RegistrySupplier<Item>> INGOT_POWDERS_TINY = new EnumMap<>(ModIngots.class);
    public static final RegistrySupplier<Item> WIRE_DENSE_IRON          = ITEMS.register("wire_dense_iron",          () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> WIRE_DENSE_ALUMINIUM     = ITEMS.register("wire_dense_aluminium",     () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> WIRE_DENSE_TITANIUM      = ITEMS.register("wire_dense_titanium",      () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> WIRE_DENSE_LEAD          = ITEMS.register("wire_dense_lead",          () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> WIRE_DENSE_COPPER        = ITEMS.register("wire_dense_copper",        () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> WIRE_DENSE_STEEL         = ITEMS.register("wire_dense_steel",         () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> WIRE_DENSE_GOLD          = ITEMS.register("wire_dense_gold",          () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> WIRE_DENSE_ADVANCED_ALLOY= ITEMS.register("wire_dense_advanced_alloy",() -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> WIRE_DENSE_SCHRABIDIUM   = ITEMS.register("wire_dense_schrabidium",   () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> WIRE_DENSE_SATURNITE     = ITEMS.register("wire_dense_saturnite",     () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> WIRE_DENSE_COMBINE_STEEL = ITEMS.register("wire_dense_combine_steel", () -> new Item(new Item.Properties()));

    // --- Standalone Pulver ohne Ingot-Gegenstueck (aus Original-Rezepten portiert, DEV-Tab bis einsortiert) ---
    public static final RegistrySupplier<Item> POWDER_SAWDUST      = ITEMS.register("sawdust_powder",      () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> POWDER_YELLOWCAKE   = ITEMS.register("yellowcake_powder",   () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> POWDER_BALEFIRE     = ITEMS.register("balefire_powder",     () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> POWDER_PALEOGENITE  = ITEMS.register("paleogenite_powder",  () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> POWDER_THERMITE     = ITEMS.register("thermite_powder",     () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> POWDER_FERTILIZER   = ITEMS.register("fertilizer_powder",   () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> POWDER_FLUX         = ITEMS.register("flux_powder",         () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> POWDER_MAGIC        = ITEMS.register("magic_powder",        () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> POWDER_ICE          = ITEMS.register("ice_powder",          () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> POWDER_SPARK_MIX    = ITEMS.register("spark_mix_powder",    () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> POWDER_SEMTEX_MIX   = ITEMS.register("semtex_mix_powder",   () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> POWDER_DESH_READY   = ITEMS.register("desh_ready_powder",   () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> POWDER_COLTAN       = ITEMS.register("coltan_powder",       () -> new Item(new Item.Properties()));

    private static final Set<String> POWDER_TINY_NAMES = Set.of(
            "actinium", "boron", "cerium", "cobalt", "cs137", "i131",
            "lanthanium", "lithium", "meteorite", "neodymium", "niobium",
            "sr90", "steel", "xe135");
    private static final Map<String, RegistrySupplier<Item>> POWDER_ITEMS_BY_ID = new HashMap<>();

        private static final Set<String> ENABLED_MODPOWDERS = Set.of("iron", "gold", "coal", "cement", "aluminum", "limestone"); // Только ModPowders!
    private static final Set<String> ENABLED_INGOT_POWDERS = Set.of(
            "uranium", "plutonium",
            "actinium", "steel", "advanced_alloy", "aluminum", "schrabidium", "lead",
            "red_copper", "asbestos", "titanium", "cobalt", "tungsten",
            "beryllium", "bismuth", "polymer", "bakelite", "desh", "les",
            "magnetized_tungsten", "combine_steel", "dura_steel",
            "euphemium", "dineutronium", "australium", "tantalium",
            "meteorite", "lanthanium", "neodymium", "niobium", "cerium", "cadmium",
            "caesium", "strontium", "tennessine", "bromide", "zirconium", "iodine",
            "astatine", "neptunium", "polonium", "boron", "schrabidate",
            "au198", "ra226", "thorium", "selenium", "co60",
            "sr90", "calcium", "ferrouranium"
    );

    private static final Set<String> ENABLED_TINY_POWDERS = Set.of(
            "actinium", "boron", "cerium", "cobalt", "cs137", "i131", "lanthanium", "lithium",
            "meteorite", "neodymium", "niobium", "sr90", "steel", "xe135"
    );

    static {
        // 1. СЛИТКИ (ВСЕГДА)  OK
        for (ModIngots ingot : ModIngots.values()) {
            RegistrySupplier<Item> registeredItem;
            if (ingot == ModIngots.URANIUM) {
                registeredItem = ITEMS.register(ingot.getName() + "_ingot", () -> new RadioactiveItem(new Item.Properties()));
            } else {
                registeredItem = ITEMS.register(ingot.getName() + "_ingot", () -> new Item(new Item.Properties()));
            }
            INGOTS.put(ingot, registeredItem);
        }

        // 2. ModPowders (ТОЛЬКО ИЗ ENABLED_MODPOWDERS)  ИСПРАВЛЕНО!
        for (ModPowders powder : ModPowders.values()) {
            String baseName = powder.getName(); // use getName() to get lowercase name
            if (ENABLED_MODPOWDERS.contains(baseName)) {
                String powderId = baseName + "_powder";
                RegistrySupplier<Item> powderItem = ITEMS.register(powderId,
                        () -> powder == ModPowders.IRON ? new RadioactiveItem(new Item.Properties()) : new Item(new Item.Properties()));
                POWDERS.put(powder, powderItem);
                POWDER_ITEMS_BY_ID.put(powderId, powderItem);
            }
        }

        // 3. Порошки из слитков (ТОЛЬКО ИЗ ENABLED_INGOT_POWDERS)  ИСПРАВЛЕНО!
        for (ModIngots ingot : ModIngots.values()) {
            String baseName = ingot.getName();

            // Основной порошок
            if (ENABLED_INGOT_POWDERS.contains(baseName)) {
                String powderId = baseName + "_powder";
                RegistrySupplier<Item> powderItem = POWDER_ITEMS_BY_ID.get(powderId);
                if (powderItem == null) {
                    powderItem = ITEMS.register(powderId, () -> new Item(new Item.Properties()));
                    POWDER_ITEMS_BY_ID.put(powderId, powderItem);
                }
                INGOT_POWDERS.put(ingot, powderItem);
            }

            // Маленький порошок  OK
            if (POWDER_TINY_NAMES.contains(baseName) && ENABLED_TINY_POWDERS.contains(baseName)) {
                String tinyId = baseName + "_powder_tiny";
                RegistrySupplier<Item> tinyItem = ITEMS.register(tinyId, () -> new Item(new Item.Properties()));
                INGOT_POWDERS_TINY.put(ingot, tinyItem);
            }
        }
    }

    // УДОБНЫЙ МЕТОД ДЛЯ ПОЛУЧЕНИЯ СЛИТКА
    public static RegistrySupplier<Item> getIngot(ModIngots ingot) {
        return INGOTS.get(ingot);
    }

    public static RegistrySupplier<Item> getPowders(ModPowders powders) {return POWDERS.get(powders);}
    public static RegistrySupplier<Item> getPowder(ModIngots ingot) { return INGOT_POWDERS.get(ingot); }
    public static Optional<RegistrySupplier<Item>> getTinyPowder(ModIngots ingot) {
        return Optional.ofNullable(INGOT_POWDERS_TINY.get(ingot));
    }
    
    public static final int SLOT_HELMET = 0;
    public static final int SLOT_CHEST = 1;
    public static final int SLOT_LEGS = 2;
    public static final int SLOT_BOOTS = 3;
    public static final int SLOT_BATTERY = 8;  // Изменено согласно ArmorModificationHelper.battery
    public static final int SLOT_SPECIAL = 7;  // Изменено согласно ArmorModificationHelper.extra
    public static final int SLOT_INSERT = 6;
    public static final int SLOT_CLADDING = 5; // Изменено согласно ArmorModificationHelper.cladding
    public static final int SLOT_SERVOS = 4;   // Изменено согласно ArmorModificationHelper.servos

    // Дополнительные константы для совместимости
    public static final int SLOT_HELMET_ONLY = 0;
    public static final int SLOT_PLATE_ONLY = 1;
    public static final int SLOT_LEGS_ONLY = 2;
    public static final int SLOT_BOOTS_ONLY = 3;
    public static final int SLOT_KEVLAR = 6;

    public static final int BATTERY_CAPACITY = 1_000_000;

// ХАВЧИК:
    public static final RegistrySupplier<Item> STRAWBERRY = ITEMS.register("strawberry",
            () -> new Item(new Item.Properties().food(ModFoods.STRAWBERRY)));
    public static final RegistrySupplier<Item> CANNED_ASBESTOS = ITEMS.register("canned_asbestos",
            () -> new ItemConserve(new Item.Properties().food(ModFoods.CANNED_ASBESTOS)));
    public static final RegistrySupplier<Item> CANNED_ASS = ITEMS.register("canned_ass",
            () -> new ItemConserve(new Item.Properties().food(ModFoods.CANNED_ASS)));
    public static final RegistrySupplier<Item> CANNED_BARK = ITEMS.register("canned_bark",
            () -> new ItemConserve(new Item.Properties().food(ModFoods.CANNED_BARK)));
    public static final RegistrySupplier<Item> CANNED_BEEF = ITEMS.register("canned_beef",
            () -> new ItemConserve(new Item.Properties().food(ModFoods.CANNED_BEEF)));
    public static final RegistrySupplier<Item> CANNED_BHOLE = ITEMS.register("canned_bhole",
            () -> new ItemConserve(new Item.Properties().food(ModFoods.CANNED_BHOLE)));
    public static final RegistrySupplier<Item> CANNED_CHEESE = ITEMS.register("canned_cheese",
            () -> new ItemConserve(new Item.Properties().food(ModFoods.CANNED_CHEESE)));
    public static final RegistrySupplier<Item> CANNED_CHINESE = ITEMS.register("canned_chinese",
            () -> new ItemConserve(new Item.Properties().food(ModFoods.CANNED_CHINESE)));
    public static final RegistrySupplier<Item> CANNED_DIESEL = ITEMS.register("canned_diesel",
            () -> new ItemConserve(new Item.Properties().food(ModFoods.CANNED_DIESEL)));
    public static final RegistrySupplier<Item> CANNED_FIST = ITEMS.register("canned_fist",
            () -> new ItemConserve(new Item.Properties().food(ModFoods.CANNED_FIST)));
    public static final RegistrySupplier<Item> CANNED_FRIED = ITEMS.register("canned_fried",
            () -> new ItemConserve(new Item.Properties().food(ModFoods.CANNED_FRIED)));
    public static final RegistrySupplier<Item> CANNED_HOTDOGS = ITEMS.register("canned_hotdogs",
            () -> new ItemConserve(new Item.Properties().food(ModFoods.CANNED_HOTDOGS)));
    public static final RegistrySupplier<Item> CANNED_JIZZ = ITEMS.register("canned_jizz",
            () -> new ItemConserve(new Item.Properties().food(ModFoods.CANNED_JIZZ)));
    public static final RegistrySupplier<Item> CANNED_KEROSENE = ITEMS.register("canned_kerosene",
            () -> new ItemConserve(new Item.Properties().food(ModFoods.CANNED_KEROSENE)));
    public static final RegistrySupplier<Item> CANNED_LEFTOVERS = ITEMS.register("canned_leftovers",
            () -> new ItemConserve(new Item.Properties().food(ModFoods.CANNED_LEFTOVERS)));
    public static final RegistrySupplier<Item> CANNED_MILK = ITEMS.register("canned_milk",
            () -> new ItemConserve(new Item.Properties().food(ModFoods.CANNED_MILK)));
    public static final RegistrySupplier<Item> CANNED_MYSTERY = ITEMS.register("canned_mystery",
            () -> new ItemConserve(new Item.Properties().food(ModFoods.CANNED_MYSTERY)));
    public static final RegistrySupplier<Item> CANNED_NAPALM = ITEMS.register("canned_napalm",
            () -> new ItemConserve(new Item.Properties().food(ModFoods.CANNED_NAPALM)));
    public static final RegistrySupplier<Item> CANNED_OIL = ITEMS.register("canned_oil",
            () -> new ItemConserve(new Item.Properties().food(ModFoods.CANNED_OIL)));
    public static final RegistrySupplier<Item> CANNED_PASHTET = ITEMS.register("canned_pashtet",
            () -> new ItemConserve(new Item.Properties().food(ModFoods.CANNED_PASHTET)));
    public static final RegistrySupplier<Item> CANNED_PIZZA = ITEMS.register("canned_pizza",
            () -> new ItemConserve(new Item.Properties().food(ModFoods.CANNED_PIZZA)));
    public static final RegistrySupplier<Item> CANNED_RECURSION = ITEMS.register("canned_recursion",
            () -> new ItemConserve(new Item.Properties().food(ModFoods.CANNED_RECURSION)));
    public static final RegistrySupplier<Item> CANNED_SPAM = ITEMS.register("canned_spam",
            () -> new ItemConserve(new Item.Properties().food(ModFoods.CANNED_SPAM)));
    public static final RegistrySupplier<Item> CANNED_STEW = ITEMS.register("canned_stew",
            () -> new ItemConserve(new Item.Properties().food(ModFoods.CANNED_STEW)));
    public static final RegistrySupplier<Item> CANNED_TOMATO = ITEMS.register("canned_tomato",
            () -> new ItemConserve(new Item.Properties().food(ModFoods.CANNED_TOMATO)));
    public static final RegistrySupplier<Item> CANNED_TUNA = ITEMS.register("canned_tuna",
            () -> new ItemConserve(new Item.Properties().food(ModFoods.CANNED_TUNA)));
    public static final RegistrySupplier<Item> CANNED_TUBE = ITEMS.register("canned_tube",
            () -> new ItemConserve(new Item.Properties().food(ModFoods.CANNED_TUBE)));
    public static final RegistrySupplier<Item> CANNED_YOGURT = ITEMS.register("canned_yogurt",
            () -> new ItemConserve(new Item.Properties().food(ModFoods.CANNED_YOGURT)));


    public static final RegistrySupplier<Item> CAN_BEPIS = ITEMS.register("can_bepis",
            () -> new ItemEnergyDrink(new Item.Properties().food(ItemEnergyDrink.CAN_BEPIS)));
    public static final RegistrySupplier<Item> CAN_BREEN = ITEMS.register("can_breen",
            () -> new ItemEnergyDrink(new Item.Properties().food(ItemEnergyDrink.CAN_BREEN)));
    public static final RegistrySupplier<Item> CAN_CREATURE = ITEMS.register("can_creature",
            () -> new ItemEnergyDrink(new Item.Properties().food(ItemEnergyDrink.CAN_CREATURE)));
    public static final RegistrySupplier<Item> CAN_EMPTY = ITEMS.register("can_empty",
            () -> new Item(new Item.Properties())); // Пустая банка без эффекта
    public static final RegistrySupplier<Item> CAN_LUNA = ITEMS.register("can_luna",
            () -> new ItemEnergyDrink(new Item.Properties().food(ItemEnergyDrink.CAN_LUNA)));
    public static final RegistrySupplier<Item> CAN_MRSUGAR = ITEMS.register("can_mrsugar",
            () -> new ItemEnergyDrink(new Item.Properties().food(ItemEnergyDrink.CAN_MRSUGAR)));
    public static final RegistrySupplier<Item> CAN_MUG = ITEMS.register("can_mug",
            () -> new ItemEnergyDrink(new Item.Properties().food(ItemEnergyDrink.CAN_MUG)));
    public static final RegistrySupplier<Item> CAN_OVERCHARGE = ITEMS.register("can_overcharge",
            () -> new ItemEnergyDrink(new Item.Properties().food(ItemEnergyDrink.CAN_OVERCHARGE)));
    public static final RegistrySupplier<Item> CAN_REDBOMB = ITEMS.register("can_redbomb",
            () -> new ItemEnergyDrink(new Item.Properties().food(ItemEnergyDrink.CAN_REDBOMB)));
    public static final RegistrySupplier<Item> CAN_SMART = ITEMS.register("can_smart",
            () -> new ItemEnergyDrink(new Item.Properties().food(ItemEnergyDrink.CAN_SMART)));



    // ИНСТРУМЕНТЫ ГОРНЯКА:
    public static final RegistrySupplier<Item> STARMETAL_SWORD = ITEMS.register("starmetal_sword",
            () -> new SwordItem(ModToolTiers.STARMETAL, 7, -2, new Item.Properties()));
    public static final RegistrySupplier<Item> STARMETAL_AXE = ITEMS.register("starmetal_axe",
            () -> new ModAxeItem(ModToolTiers.STARMETAL, 15, 1, new Item.Properties()));
    public static final RegistrySupplier<Item> STARMETAL_PICKAXE = ITEMS.register("starmetal_pickaxe",
            () -> new ModPickaxeItem(ModToolTiers.STARMETAL, 3, 1, new Item.Properties(), 6, 3, 1, 5));
    public static final RegistrySupplier<Item> STARMETAL_SHOVEL = ITEMS.register("starmetal_shovel",
            () -> new ShovelItem(ModToolTiers.STARMETAL, 0, 0, new Item.Properties()));
    public static final RegistrySupplier<Item> STARMETAL_HOE = ITEMS.register("starmetal_hoe",
            () -> new HoeItem(ModToolTiers.STARMETAL, 0, 0f, new Item.Properties()));

    public static final RegistrySupplier<Item> ALLOY_SWORD = ITEMS.register("alloy_sword",
        () -> new SwordItem(ModToolTiers.ALLOY, 5, 2, new Item.Properties()));

    public static final RegistrySupplier<Item> ALLOY_AXE = ITEMS.register("alloy_axe",
            () -> new ModAxeItem(ModToolTiers.ALLOY, 9, 1, new Item.Properties(), 3, 1));

    public static final RegistrySupplier<Item> ALLOY_PICKAXE = ITEMS.register("alloy_pickaxe",
            () -> new ModPickaxeItem(ModToolTiers.ALLOY, 2, 1, new Item.Properties(), 3, 0, 0, 0));

    public static final RegistrySupplier<Item> ALLOY_SHOVEL = ITEMS.register("alloy_shovel",
            () -> new ModShovelItem(ModToolTiers.ALLOY, 0, 0, new Item.Properties(), 3, 0, 2));

    public static final RegistrySupplier<Item> ALLOY_HOE = ITEMS.register("alloy_hoe",
            () -> new HoeItem(ModToolTiers.ALLOY, 0, 0f, new Item.Properties()));

    public static final RegistrySupplier<Item> STEEL_SWORD = ITEMS.register("steel_sword",
            () -> new SwordItem(ModToolTiers.STEEL, 4, 2, new Item.Properties()));
    public static final RegistrySupplier<Item> STEEL_AXE = ITEMS.register("steel_axe",
            () -> new AxeItem(ModToolTiers.STEEL, 7, 1, new Item.Properties()));
    public static final RegistrySupplier<Item> STEEL_PICKAXE = ITEMS.register("steel_pickaxe",
            () -> new PickaxeItem(ModToolTiers.STEEL, 1, 1, new Item.Properties()));
    public static final RegistrySupplier<Item> STEEL_SHOVEL = ITEMS.register("steel_shovel",
            () -> new ShovelItem(ModToolTiers.STEEL, 0, 0, new Item.Properties()));
    public static final RegistrySupplier<Item> STEEL_HOE = ITEMS.register("steel_hoe",
            () -> new HoeItem(ModToolTiers.STEEL, 0, 0, new Item.Properties()));

    public static final RegistrySupplier<Item> TITANIUM_SWORD = ITEMS.register("titanium_sword",
            () -> new SwordItem(ModToolTiers.TITANIUM, 2, 3, new Item.Properties()));
    public static final RegistrySupplier<Item> TITANIUM_AXE = ITEMS.register("titanium_axe",
            () -> new AxeItem(ModToolTiers.TITANIUM, 8, 1, new Item.Properties()));

    // Meteorite swords (registered so recipes can produce them)
    public static final RegistrySupplier<Item> METEORITE_SWORD = ITEMS.register("meteorite_sword",
            () -> new SwordItem(ModToolTiers.TITANIUM, 3, -2, new Item.Properties()));
    public static final RegistrySupplier<Item> METEORITE_SWORD_SEARED = ITEMS.register("meteorite_sword_seared",
            () -> new SwordItem(ModToolTiers.TITANIUM, 3, -2, new Item.Properties()));
    // Original chain continues: seared -> reforged -> hardened -> alloyed -> machined -> treated -> etched -> bred -> ...
    // Only hardened/alloyed are added here (needed by the Blast Furnace recipe below); the rest of the chain
    // (Press/Crystallizer/Breeder steps) is tracked separately and not yet wired up.
    public static final RegistrySupplier<Item> METEORITE_SWORD_HARDENED = ITEMS.register("meteorite_sword_hardened",
            () -> new SwordItem(ModToolTiers.TITANIUM, 3, -2, new Item.Properties()));
    public static final RegistrySupplier<Item> METEORITE_SWORD_ALLOYED = ITEMS.register("meteorite_sword_alloyed",
            () -> new SwordItem(ModToolTiers.TITANIUM, 3, -2, new Item.Properties()));
    public static final RegistrySupplier<Item> TITANIUM_PICKAXE = ITEMS.register("titanium_pickaxe",
            () -> new PickaxeItem(ModToolTiers.TITANIUM, 1, 1, new Item.Properties()));
    public static final RegistrySupplier<Item> DRILL_TITANIUM = ITEMS.register("drill_titanium",
            () -> new Item(new Item.Properties().stacksTo(1)));
    public static final RegistrySupplier<Item> TITANIUM_SHOVEL = ITEMS.register("titanium_shovel",
            () -> new ShovelItem(ModToolTiers.TITANIUM, 0, 0, new Item.Properties()));
    public static final RegistrySupplier<Item> TITANIUM_HOE = ITEMS.register("titanium_hoe",
            () -> new HoeItem(ModToolTiers.TITANIUM, 0, 0, new Item.Properties()));


    public static final RegistrySupplier<Item> GRENADE = ITEMS.register("grenade",
        () -> new GrenadeItem(new Item.Properties(), GrenadeType.STANDARD, ModEntities.GRENADE_PROJECTILE));

    public static final RegistrySupplier<Item> GRENADEHE = ITEMS.register("grenadehe",
        () -> new GrenadeItem(new Item.Properties(), GrenadeType.HE, ModEntities.GRENADEHE_PROJECTILE));

    public static final RegistrySupplier<Item> GRENADEFIRE = ITEMS.register("grenadefire",
        () -> new GrenadeItem(new Item.Properties(), GrenadeType.FIRE, ModEntities.GRENADEFIRE_PROJECTILE));

    public static final RegistrySupplier<Item> GRENADESLIME = ITEMS.register("grenadeslime",
        () -> new GrenadeItem(new Item.Properties(), GrenadeType.SLIME, ModEntities.GRENADESLIME_PROJECTILE));

    public static final RegistrySupplier<Item> GRENADESMART = ITEMS.register("grenadesmart",
        () -> new GrenadeItem(new Item.Properties(), GrenadeType.SMART, ModEntities.GRENADESMART_PROJECTILE));

    public static final RegistrySupplier<Item> GRENADE_IF = ITEMS.register("grenade_if",
            () -> new GrenadeIfItem(new Item.Properties(), GrenadeIfType.GRENADE_IF, ModEntities.GRENADE_IF_PROJECTILE));

    public static final RegistrySupplier<Item> GRENADE_IF_HE = ITEMS.register("grenade_if_he",
            () -> new GrenadeIfItem(new Item.Properties(), GrenadeIfType.GRENADE_IF_HE, ModEntities.GRENADE_IF_HE_PROJECTILE));

    public static final RegistrySupplier<Item> GRENADE_IF_SLIME = ITEMS.register("grenade_if_slime",
            () -> new GrenadeIfItem(new Item.Properties(), GrenadeIfType.GRENADE_IF_SLIME, ModEntities.GRENADE_IF_SLIME_PROJECTILE));

    public static final RegistrySupplier<Item> GRENADE_IF_FIRE = ITEMS.register("grenade_if_fire",
            () -> new GrenadeIfItem(new Item.Properties(), GrenadeIfType.GRENADE_IF_FIRE, ModEntities.GRENADE_IF_FIRE_PROJECTILE));

    public static final RegistrySupplier<Item> GRENADE_NUC = ITEMS.register("grenade_nuc",
            () -> new GrenadeNucItem(new Item.Properties(), ModEntities.GRENADE_NUC_PROJECTILE));

    public static final RegistrySupplier<Item> AIRBOMB_A = ITEMS.register("airbomb_a",
            () -> new AirBombItem(new Item.Properties(), ModEntities.AIRBOMB_PROJECTILE));
    public static final RegistrySupplier<Item> AIRNUKEBOMB_A = ITEMS.register("airnukebomb_a",
            () -> new AirNukeBombItem(new Item.Properties(), ModEntities.AIRNUKEBOMB_PROJECTILE));
    public static final RegistrySupplier<Item> NOLO_SPAWN_EGG = ITEMS.register("nolo_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.NOLO, 0x8b5e3c, 0xf0d8b0, new Item.Properties()));

    public static final RegistrySupplier<Item> ENTITY_MOB_TAINTED_CREEPER_SPAWN_EGG = ITEMS.register(
            "entity_mob_tainted_creeper_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.ENTITY_MOB_TAINTED_CREEPER, 0x813b9b, 0xd71fdd, new Item.Properties()));

    public static final RegistrySupplier<Item> ENTITY_MOB_VOLATILE_CREEPER_SPAWN_EGG = ITEMS.register(
            "entity_mob_volatile_creeper_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.ENTITY_MOB_VOLATILE_CREEPER, 0xC28153, 0x4D382C, new Item.Properties()));

    public static final RegistrySupplier<Item> ENTITY_MOB_PHOSGENE_CREEPER_SPAWN_EGG = ITEMS.register(
            "entity_mob_phosgene_creeper_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.ENTITY_MOB_PHOSGENE_CREEPER, 0xE3D398, 0xB8A06B, new Item.Properties()));

    public static final RegistrySupplier<Item> ENTITY_MOB_GOLD_CREEPER_SPAWN_EGG = ITEMS.register(
            "entity_mob_gold_creeper_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.ENTITY_MOB_GOLD_CREEPER, 0xECC136, 0x9E8B3E, new Item.Properties()));

    public static final RegistrySupplier<Item> ENTITY_MOB_NUCLEAR_CREEPER_SPAWN_EGG = ITEMS.register(
            "entity_mob_nuclear_creeper_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.ENTITY_MOB_NUCLEAR_CREEPER, 0x204131, 0x75CE00, new Item.Properties()));

    // БРОНЯ ГОРНЯКА:
    public static final RegistrySupplier<Item> ALLOY_HELMET = ITEMS.register("alloy_helmet",
            () -> new ArmorItem(ModArmorMaterials.ALLOY, ArmorItem.Type.HELMET, new Item.Properties()));
    public static final RegistrySupplier<Item> ALLOY_CHESTPLATE = ITEMS.register("alloy_chestplate",
            () -> new ArmorItem(ModArmorMaterials.ALLOY, ArmorItem.Type.CHESTPLATE, new Item.Properties()));
    public static final RegistrySupplier<Item> ALLOY_LEGGINGS = ITEMS.register("alloy_leggings",
            () -> new ArmorItem(ModArmorMaterials.ALLOY, ArmorItem.Type.LEGGINGS, new Item.Properties()));
    public static final RegistrySupplier<Item> ALLOY_BOOTS = ITEMS.register("alloy_boots",
            () -> new ArmorItem(ModArmorMaterials.ALLOY, ArmorItem.Type.BOOTS, new Item.Properties()));

    public static final RegistrySupplier<Item> TITANIUM_HELMET = ITEMS.register("titanium_helmet",
            () -> new ArmorItem(ModArmorMaterials.TITANIUM, ArmorItem.Type.HELMET, new Item.Properties()));
    public static final RegistrySupplier<Item> TITANIUM_CHESTPLATE = ITEMS.register("titanium_chestplate",
            () -> new ArmorItem(ModArmorMaterials.TITANIUM, ArmorItem.Type.CHESTPLATE, new Item.Properties()));
    public static final RegistrySupplier<Item> TITANIUM_LEGGINGS = ITEMS.register("titanium_leggings",
            () -> new ArmorItem(ModArmorMaterials.TITANIUM, ArmorItem.Type.LEGGINGS, new Item.Properties()));
    public static final RegistrySupplier<Item> TITANIUM_BOOTS = ITEMS.register("titanium_boots",
            () -> new ArmorItem(ModArmorMaterials.TITANIUM, ArmorItem.Type.BOOTS, new Item.Properties()));

    public static final RegistrySupplier<Item> STEEL_HELMET = ITEMS.register("steel_helmet",
            () -> new ArmorItem(ModArmorMaterials.STEEL, ArmorItem.Type.HELMET, new Item.Properties()));
    public static final RegistrySupplier<Item> STEEL_CHESTPLATE = ITEMS.register("steel_chestplate",
            () -> new ArmorItem(ModArmorMaterials.TITANIUM, ArmorItem.Type.CHESTPLATE, new Item.Properties()));
    public static final RegistrySupplier<Item> STEEL_LEGGINGS = ITEMS.register("steel_leggings",
            () -> new ArmorItem(ModArmorMaterials.STEEL, ArmorItem.Type.LEGGINGS, new Item.Properties()));
    public static final RegistrySupplier<Item> STEEL_BOOTS = ITEMS.register("steel_boots",
            () -> new ArmorItem(ModArmorMaterials.STEEL, ArmorItem.Type.BOOTS, new Item.Properties()));

    public static final RegistrySupplier<Item> COBALT_HELMET = ITEMS.register("cobalt_helmet",
            () -> new ArmorItem(ModArmorMaterials.COBALT, ArmorItem.Type.HELMET, new Item.Properties()));
    public static final RegistrySupplier<Item> COBALT_CHESTPLATE = ITEMS.register("cobalt_chestplate",
            () -> new ArmorItem(ModArmorMaterials.COBALT, ArmorItem.Type.CHESTPLATE, new Item.Properties()));
    public static final RegistrySupplier<Item> COBALT_LEGGINGS = ITEMS.register("cobalt_leggings",
            () -> new ArmorItem(ModArmorMaterials.COBALT, ArmorItem.Type.LEGGINGS, new Item.Properties()));
    public static final RegistrySupplier<Item> COBALT_BOOTS = ITEMS.register("cobalt_boots",
            () -> new ArmorItem(ModArmorMaterials.COBALT, ArmorItem.Type.BOOTS, new Item.Properties()));

    public static final RegistrySupplier<Item> SECURITY_HELMET = ITEMS.register("security_helmet",
            () -> new ArmorItem(ModArmorMaterials.SECURITY, ArmorItem.Type.HELMET, new Item.Properties()));
    public static final RegistrySupplier<Item> SECURITY_CHESTPLATE = ITEMS.register("security_chestplate",
            () -> new ArmorItem(ModArmorMaterials.SECURITY, ArmorItem.Type.CHESTPLATE, new Item.Properties()));
    public static final RegistrySupplier<Item> SECURITY_LEGGINGS = ITEMS.register("security_leggings",
            () -> new ArmorItem(ModArmorMaterials.SECURITY, ArmorItem.Type.LEGGINGS, new Item.Properties()));
    public static final RegistrySupplier<Item> SECURITY_BOOTS = ITEMS.register("security_boots",
            () -> new ArmorItem(ModArmorMaterials.SECURITY, ArmorItem.Type.BOOTS, new Item.Properties()));

    public static final RegistrySupplier<Item> ASBESTOS_HELMET = ITEMS.register("asbestos_helmet",
            () -> new ArmorItem(ModArmorMaterials.ASBESTOS, ArmorItem.Type.HELMET, new Item.Properties()));
    public static final RegistrySupplier<Item> ASBESTOS_CHESTPLATE = ITEMS.register("asbestos_chestplate",
            () -> new ArmorItem(ModArmorMaterials.ASBESTOS, ArmorItem.Type.CHESTPLATE, new Item.Properties()));
    public static final RegistrySupplier<Item> ASBESTOS_LEGGINGS = ITEMS.register("asbestos_leggings",
            () -> new ArmorItem(ModArmorMaterials.ASBESTOS, ArmorItem.Type.LEGGINGS, new Item.Properties()));
    public static final RegistrySupplier<Item> ASBESTOS_BOOTS = ITEMS.register("asbestos_boots",
            () -> new ArmorItem(ModArmorMaterials.ASBESTOS, ArmorItem.Type.BOOTS, new Item.Properties()));

    public static final RegistrySupplier<Item> HAZMAT_HELMET = ITEMS.register("hazmat_helmet",
            () -> new ArmorItem(ModArmorMaterials.HAZMAT, ArmorItem.Type.HELMET, new Item.Properties()));
    public static final RegistrySupplier<Item> HAZMAT_CHESTPLATE = ITEMS.register("hazmat_chestplate",
            () -> new ArmorItem(ModArmorMaterials.HAZMAT, ArmorItem.Type.CHESTPLATE, new Item.Properties()));
    public static final RegistrySupplier<Item> HAZMAT_LEGGINGS = ITEMS.register("hazmat_leggings",
            () -> new ArmorItem(ModArmorMaterials.HAZMAT, ArmorItem.Type.LEGGINGS, new Item.Properties()));
    public static final RegistrySupplier<Item> HAZMAT_BOOTS = ITEMS.register("hazmat_boots",
            () -> new ArmorItem(ModArmorMaterials.HAZMAT, ArmorItem.Type.BOOTS, new Item.Properties()));

    public static final RegistrySupplier<Item> LIQUIDATOR_HELMET = ITEMS.register("liquidator_helmet",
            () -> new ArmorItem(ModArmorMaterials.LIQUIDATOR, ArmorItem.Type.HELMET, new Item.Properties()));
    public static final RegistrySupplier<Item> LIQUIDATOR_CHESTPLATE = ITEMS.register("liquidator_chestplate",
            () -> new ArmorItem(ModArmorMaterials.LIQUIDATOR, ArmorItem.Type.CHESTPLATE, new Item.Properties()));
    public static final RegistrySupplier<Item> LIQUIDATOR_LEGGINGS = ITEMS.register("liquidator_leggings",
            () -> new ArmorItem(ModArmorMaterials.LIQUIDATOR, ArmorItem.Type.LEGGINGS, new Item.Properties()));
    public static final RegistrySupplier<Item> LIQUIDATOR_BOOTS = ITEMS.register("liquidator_boots",
            () -> new ArmorItem(ModArmorMaterials.LIQUIDATOR, ArmorItem.Type.BOOTS, new Item.Properties()));

    public static final RegistrySupplier<Item> PAA_HELMET = ITEMS.register("paa_helmet",
            () -> new ArmorItem(ModArmorMaterials.PAA, ArmorItem.Type.HELMET, new Item.Properties()));
    public static final RegistrySupplier<Item> PAA_CHESTPLATE = ITEMS.register("paa_chestplate",
            () -> new ArmorItem(ModArmorMaterials.PAA, ArmorItem.Type.CHESTPLATE, new Item.Properties()));
    public static final RegistrySupplier<Item> PAA_LEGGINGS = ITEMS.register("paa_leggings",
            () -> new ArmorItem(ModArmorMaterials.PAA, ArmorItem.Type.LEGGINGS, new Item.Properties()));
    public static final RegistrySupplier<Item> PAA_BOOTS = ITEMS.register("paa_boots",
            () -> new ArmorItem(ModArmorMaterials.PAA, ArmorItem.Type.BOOTS, new Item.Properties()));

    public static final RegistrySupplier<Item> STARMETAL_HELMET = ITEMS.register("starmetal_helmet",
            () -> new ArmorItem(ModArmorMaterials.STARMETAL, ArmorItem.Type.HELMET, new Item.Properties()));
    public static final RegistrySupplier<Item> STARMETAL_CHESTPLATE = ITEMS.register("starmetal_chestplate",
            () -> new ArmorItem(ModArmorMaterials.STARMETAL, ArmorItem.Type.CHESTPLATE, new Item.Properties()));
    public static final RegistrySupplier<Item> STARMETAL_LEGGINGS = ITEMS.register("starmetal_leggings",
            () -> new ArmorItem(ModArmorMaterials.STARMETAL, ArmorItem.Type.LEGGINGS, new Item.Properties()));
    public static final RegistrySupplier<Item> STARMETAL_BOOTS = ITEMS.register("starmetal_boots",
            () -> new ArmorItem(ModArmorMaterials.STARMETAL, ArmorItem.Type.BOOTS, new Item.Properties()));


    //-----------------------POWER ARMOR ----------------------------------

    public static final RegistrySupplier<Item> T51_HELMET = ITEMS.register("t51_helmet",
            () -> new T51Armor(ModArmorMaterials.TITANIUM, ArmorItem.Type.HELMET, new Item.Properties()));

    public static final RegistrySupplier<Item> T51_CHESTPLATE = ITEMS.register("t51_chestplate",
            () -> new T51Armor(ModArmorMaterials.TITANIUM, ArmorItem.Type.CHESTPLATE, new Item.Properties()));

    public static final RegistrySupplier<Item> T51_LEGGINGS = ITEMS.register("t51_leggings",
            () -> new T51Armor(ModArmorMaterials.TITANIUM, ArmorItem.Type.LEGGINGS, new Item.Properties()));

    public static final RegistrySupplier<Item> T51_BOOTS = ITEMS.register("t51_boots",
            () -> new T51Armor(ModArmorMaterials.TITANIUM, ArmorItem.Type.BOOTS, new Item.Properties()));

    public static final RegistrySupplier<Item> AJR_HELMET = ITEMS.register("ajr_helmet",
            () -> new AJRArmor(ModArmorMaterials.AJR, ArmorItem.Type.HELMET, new Item.Properties()));

    public static final RegistrySupplier<Item> AJR_CHESTPLATE = ITEMS.register("ajr_chestplate",
            () -> new AJRArmor(ModArmorMaterials.AJR, ArmorItem.Type.CHESTPLATE, new Item.Properties()));

    public static final RegistrySupplier<Item> AJR_LEGGINGS = ITEMS.register("ajr_leggings",
            () -> new AJRArmor(ModArmorMaterials.AJR, ArmorItem.Type.LEGGINGS, new Item.Properties()));

    public static final RegistrySupplier<Item> AJR_BOOTS = ITEMS.register("ajr_boots",
            () -> new AJRArmor(ModArmorMaterials.AJR, ArmorItem.Type.BOOTS, new Item.Properties()));

	public static final RegistrySupplier<Item> AJRO_HELMET = ITEMS.register("ajro_helmet",
            () -> new AJROArmor(ModArmorMaterials.AJR, ArmorItem.Type.HELMET, new Item.Properties()));

    public static final RegistrySupplier<Item> AJRO_CHESTPLATE = ITEMS.register("ajro_chestplate",
            () -> new AJROArmor(ModArmorMaterials.AJR, ArmorItem.Type.CHESTPLATE, new Item.Properties()));

    public static final RegistrySupplier<Item> AJRO_LEGGINGS = ITEMS.register("ajro_leggings",
            () -> new AJROArmor(ModArmorMaterials.AJR, ArmorItem.Type.LEGGINGS, new Item.Properties()));
			
    public static final RegistrySupplier<Item> AJRO_BOOTS = ITEMS.register("ajro_boots",
            () -> new AJROArmor(ModArmorMaterials.AJR, ArmorItem.Type.BOOTS, new Item.Properties()));

    public static final RegistrySupplier<Item> DNT_HELMET = ITEMS.register("dnt_helmet",
            () -> new DNTArmor(ModArmorMaterials.STARMETAL, ArmorItem.Type.HELMET, new Item.Properties()));

    public static final RegistrySupplier<Item> DNT_CHESTPLATE = ITEMS.register("dnt_chestplate",
            () -> new DNTArmor(ModArmorMaterials.STARMETAL, ArmorItem.Type.CHESTPLATE, new Item.Properties()));

    public static final RegistrySupplier<Item> DNT_LEGGINGS = ITEMS.register("dnt_leggings",
            () -> new DNTArmor(ModArmorMaterials.STARMETAL, ArmorItem.Type.LEGGINGS, new Item.Properties()));

    public static final RegistrySupplier<Item> DNT_BOOTS = ITEMS.register("dnt_boots",
            () -> new DNTArmor(ModArmorMaterials.STARMETAL, ArmorItem.Type.BOOTS, new Item.Properties()));

    public static final RegistrySupplier<Item> BISMUTH_HELMET = ITEMS.register("bismuth_helmet",
            () -> new BismuthArmor(ModArmorMaterials.BISMUTH, ArmorItem.Type.HELMET, new Item.Properties()));

    public static final RegistrySupplier<Item> BISMUTH_CHESTPLATE = ITEMS.register("bismuth_chestplate",
            () -> new BismuthArmor(ModArmorMaterials.BISMUTH, ArmorItem.Type.CHESTPLATE, new Item.Properties()));

    public static final RegistrySupplier<Item> BISMUTH_LEGGINGS = ITEMS.register("bismuth_leggings",
            () -> new BismuthArmor(ModArmorMaterials.BISMUTH, ArmorItem.Type.LEGGINGS, new Item.Properties()));

    public static final RegistrySupplier<Item> BISMUTH_BOOTS = ITEMS.register("bismuth_boots",
            () -> new BismuthArmor(ModArmorMaterials.BISMUTH, ArmorItem.Type.BOOTS, new Item.Properties()));



    // Инструменты
    public static final RegistrySupplier<Item> GEIGER_COUNTER = ITEMS.register("geiger_counter",
            () -> new ItemGeigerCounter(new Item.Properties().stacksTo(1)));

    public static final RegistrySupplier<Item> DOSIMETER = ITEMS.register("dosimeter",
            () -> new ItemDosimeter(new Item.Properties().stacksTo(1)));

    public static final RegistrySupplier<Item> DIGAMMA_DIAGNOSTIC = ITEMS.register("digamma_diagnostic",
            () -> new ItemDigammaDiagnostic(new Item.Properties()));

    public static final RegistrySupplier<Item> MUSIC_DISC_BUNKER = ITEMS.register("music_disc_bunker",
            () -> new RecordItem(
                    1,
                    ModSounds.MUSIC_DISC_BUNKER.get(),
                    new Item.Properties().stacksTo(1).rarity(Rarity.RARE),
                    20 * 120
            ));




    public static final RegistrySupplier<Item> CRATE_IRON = ITEMS.register("crate_iron",
            () -> new CrateItem(ModBlocks.CRATE_IRON.get(), new Item.Properties(), CrateType.IRON.getSlotCount()));
    public static final RegistrySupplier<Item> CRATE_STEEL = ITEMS.register("crate_steel",
            () -> new CrateItem(ModBlocks.CRATE_STEEL.get(), new Item.Properties(), CrateType.STEEL.getSlotCount()));
    public static final RegistrySupplier<Item> CRATE_DESH = ITEMS.register("crate_desh",
            () -> new CrateItem(ModBlocks.CRATE_DESH.get(), new Item.Properties(), CrateType.DESH.getSlotCount()));
    public static final RegistrySupplier<Item> CRATE_TUNGSTEN = ITEMS.register("crate_tungsten",
            () -> new CrateItem(ModBlocks.CRATE_TUNGSTEN.get(), new Item.Properties(), CrateType.TUNGSTEN.getSlotCount()));
    public static final RegistrySupplier<Item> CRATE_TEMPLATE = ITEMS.register("crate_template",
            () -> new CrateItem(ModBlocks.CRATE_TEMPLATE.get(), new Item.Properties(), CrateType.TEMPLATE.getSlotCount()));




    // Модификаторы брони
    public static final RegistrySupplier<Item> HEART_PIECE = ITEMS.register("heart_piece",
            () -> new ItemModHealth(
                    new Item.Properties(),
                    SLOT_SPECIAL,
                    5.0
            )
    );
    public static final RegistrySupplier<Item> HEART_CONTAINER = ITEMS.register("heart_container",
            () -> new ItemModHealth(
                    new Item.Properties(),
                    SLOT_SPECIAL,
                    20.0
            )
    );
    public static final RegistrySupplier<Item> HEART_BOOSTER = ITEMS.register("heart_booster",
            () -> new ItemModHealth(
                    new Item.Properties(),
                    SLOT_SPECIAL,
                    40.0
            )
    );
    public static final RegistrySupplier<Item> HEART_FAB = ITEMS.register("heart_fab",
            () -> new ItemModHealth(
                    new Item.Properties(),
                    SLOT_SPECIAL,
                    60.0
            )
    );
    public static final RegistrySupplier<Item> BLACK_DIAMOND = ITEMS.register("black_diamond",
            () -> new ItemModHealth(
                    new Item.Properties(),
                    SLOT_SPECIAL,
                    40.0
            )
    );

    public static final RegistrySupplier<Item> GHIORSIUM_CLADDING = ITEMS.register("cladding_ghiorsium",
            () -> new ItemModRadProtection(
                    new Item.Properties(),
                    SLOT_CLADDING,
                    0.5f
            )
    );
    public static final RegistrySupplier<Item> DESH_CLADDING = ITEMS.register("cladding_desh",
            () -> new ItemModRadProtection(
                    new Item.Properties(),
                    SLOT_CLADDING,
                    0.2f
            )
    );
    public static final RegistrySupplier<Item> LEAD_CLADDING = ITEMS.register("cladding_lead",
            () -> new ItemModRadProtection(
                    new Item.Properties(),
                    SLOT_CLADDING,
                    0.1f
            )
    );
    public static final RegistrySupplier<Item> RUBBER_CLADDING = ITEMS.register("cladding_rubber",
            () -> new ItemModRadProtection(
                    new Item.Properties(),
                    SLOT_CLADDING,
                    0.005f
            )
    );
    public static final RegistrySupplier<Item> PAINT_CLADDING = ITEMS.register("cladding_paint",
            () -> new ItemModRadProtection(
                    new Item.Properties(),
                    SLOT_CLADDING,
                    0.025f
            )
    );

    // Новые модификации брони
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

    // Модификаторы батареи (увеличивают заряд брони)
    public static final RegistrySupplier<Item> ARMOR_BATTERY = ITEMS.register("armor_battery",
            () -> new ItemModBattery(1.25D)
    );

    public static final RegistrySupplier<Item> ARMOR_BATTERY_MK2 = ITEMS.register("armor_battery_mk2",
            () -> new ItemModBattery(1.5D)
    );

    public static final RegistrySupplier<Item> ARMOR_BATTERY_MK3 = ITEMS.register("armor_battery_mk3",
            () -> new ItemModBattery(2D)
    );
    public static final RegistrySupplier<Item> CREATIVE_BATTERY = ITEMS.register("battery_creative",
            () -> new ItemCreativeBattery(
                    new Item.Properties()
            )
    );
    public static final RegistrySupplier<Item> ASSEMBLY_TEMPLATE = ITEMS.register("assembly_template",
            () -> new ItemAssemblyTemplate(
                    new Item.Properties().stacksTo(1)
            )
    );
    public static final RegistrySupplier<Item> TEMPLATE_FOLDER = ITEMS.register("template_folder",
            () -> new ItemTemplateFolder(
                    new Item.Properties().stacksTo(1)
            )
    );
    public static final RegistrySupplier<Item> BLUEPRINT_FOLDER = ITEMS.register("blueprint_folder",
        () -> new ItemBlueprintFolder(
                new Item.Properties().stacksTo(1)
        )
    );

    // ═══════════════════ MACHINE UPGRADES ═══════════════════

    public static final RegistrySupplier<Item> UPGRADE_SPEED_1 = ITEMS.register("upgrade_speed_1",
            () -> new ItemMachineUpgrade(new Item.Properties(), ItemMachineUpgrade.UpgradeType.SPEED, 1));
    public static final RegistrySupplier<Item> UPGRADE_SPEED_2 = ITEMS.register("upgrade_speed_2",
            () -> new ItemMachineUpgrade(new Item.Properties(), ItemMachineUpgrade.UpgradeType.SPEED, 2));
    public static final RegistrySupplier<Item> UPGRADE_SPEED_3 = ITEMS.register("upgrade_speed_3",
            () -> new ItemMachineUpgrade(new Item.Properties(), ItemMachineUpgrade.UpgradeType.SPEED, 3));
    public static final RegistrySupplier<Item> UPGRADE_STACK_1 = ITEMS.register("upgrade_stack_1",
            () -> new ItemMachineUpgrade(new Item.Properties(), ItemMachineUpgrade.UpgradeType.STACK, 1));
    public static final RegistrySupplier<Item> UPGRADE_STACK_2 = ITEMS.register("upgrade_stack_2",
            () -> new ItemMachineUpgrade(new Item.Properties(), ItemMachineUpgrade.UpgradeType.STACK, 2));
    public static final RegistrySupplier<Item> UPGRADE_STACK_3 = ITEMS.register("upgrade_stack_3",
            () -> new ItemMachineUpgrade(new Item.Properties(), ItemMachineUpgrade.UpgradeType.STACK, 3));
    public static final RegistrySupplier<Item> UPGRADE_EJECTOR_1 = ITEMS.register("upgrade_ejector_1",
            () -> new ItemMachineUpgrade(new Item.Properties(), ItemMachineUpgrade.UpgradeType.EJECTOR, 1));
    public static final RegistrySupplier<Item> UPGRADE_EJECTOR_2 = ITEMS.register("upgrade_ejector_2",
            () -> new ItemMachineUpgrade(new Item.Properties(), ItemMachineUpgrade.UpgradeType.EJECTOR, 2));
    public static final RegistrySupplier<Item> UPGRADE_EJECTOR_3 = ITEMS.register("upgrade_ejector_3",
            () -> new ItemMachineUpgrade(new Item.Properties(), ItemMachineUpgrade.UpgradeType.EJECTOR, 3));

    public static final RegistrySupplier<Item> UPGRADE_EFFECT_1 = ITEMS.register("upgrade_effect_1",
            () -> new ItemMachineUpgrade(new Item.Properties(), ItemMachineUpgrade.UpgradeType.EFFECT, 1));
    public static final RegistrySupplier<Item> UPGRADE_EFFECT_2 = ITEMS.register("upgrade_effect_2",
            () -> new ItemMachineUpgrade(new Item.Properties(), ItemMachineUpgrade.UpgradeType.EFFECT, 2));
    public static final RegistrySupplier<Item> UPGRADE_EFFECT_3 = ITEMS.register("upgrade_effect_3",
            () -> new ItemMachineUpgrade(new Item.Properties(), ItemMachineUpgrade.UpgradeType.EFFECT, 3));

    public static final RegistrySupplier<Item> UPGRADE_POWER_1 = ITEMS.register("upgrade_power_1",
            () -> new ItemMachineUpgrade(new Item.Properties(), ItemMachineUpgrade.UpgradeType.POWER, 1));
    public static final RegistrySupplier<Item> UPGRADE_POWER_2 = ITEMS.register("upgrade_power_2",
            () -> new ItemMachineUpgrade(new Item.Properties(), ItemMachineUpgrade.UpgradeType.POWER, 2));
    public static final RegistrySupplier<Item> UPGRADE_POWER_3 = ITEMS.register("upgrade_power_3",
            () -> new ItemMachineUpgrade(new Item.Properties(), ItemMachineUpgrade.UpgradeType.POWER, 3));

    public static final RegistrySupplier<Item> UPGRADE_FORTUNE_1 = ITEMS.register("upgrade_fortune_1",
            () -> new ItemMachineUpgrade(new Item.Properties(), ItemMachineUpgrade.UpgradeType.FORTUNE, 1));
    public static final RegistrySupplier<Item> UPGRADE_FORTUNE_2 = ITEMS.register("upgrade_fortune_2",
            () -> new ItemMachineUpgrade(new Item.Properties(), ItemMachineUpgrade.UpgradeType.FORTUNE, 2));
    public static final RegistrySupplier<Item> UPGRADE_FORTUNE_3 = ITEMS.register("upgrade_fortune_3",
            () -> new ItemMachineUpgrade(new Item.Properties(), ItemMachineUpgrade.UpgradeType.FORTUNE, 3));

    public static final RegistrySupplier<Item> UPGRADE_AFTERBURN_1 = ITEMS.register("upgrade_afterburn_1",
            () -> new ItemMachineUpgrade(new Item.Properties(), ItemMachineUpgrade.UpgradeType.AFTERBURN, 1));
    public static final RegistrySupplier<Item> UPGRADE_AFTERBURN_2 = ITEMS.register("upgrade_afterburn_2",
            () -> new ItemMachineUpgrade(new Item.Properties(), ItemMachineUpgrade.UpgradeType.AFTERBURN, 2));
    public static final RegistrySupplier<Item> UPGRADE_AFTERBURN_3 = ITEMS.register("upgrade_afterburn_3",
            () -> new ItemMachineUpgrade(new Item.Properties(), ItemMachineUpgrade.UpgradeType.AFTERBURN, 3));

    public static final RegistrySupplier<Item> UPGRADE_OVERDRIVE_1 = ITEMS.register("upgrade_overdrive_1",
            () -> new ItemMachineUpgrade(new Item.Properties(), ItemMachineUpgrade.UpgradeType.OVERDRIVE, 1));
    public static final RegistrySupplier<Item> UPGRADE_OVERDRIVE_2 = ITEMS.register("upgrade_overdrive_2",
            () -> new ItemMachineUpgrade(new Item.Properties(), ItemMachineUpgrade.UpgradeType.OVERDRIVE, 2));
    public static final RegistrySupplier<Item> UPGRADE_OVERDRIVE_3 = ITEMS.register("upgrade_overdrive_3",
            () -> new ItemMachineUpgrade(new Item.Properties(), ItemMachineUpgrade.UpgradeType.OVERDRIVE, 3));

    // ═══════════════════ END MACHINE UPGRADES ═══════════════════

    public static final RegistrySupplier<Item> RADAWAY = ITEMS.register("radaway",
            () -> new ItemSimpleConsumable(new Item.Properties(), (player, stack) -> {
                // Это лямбда-выражение определяет, что произойдет при использовании предмета.
                
                // Действуем только на сервере
                if (!player.level().isClientSide()) {
                    // 1. Накладываем эффект Антирадина.
                    //    Длительность: 200 тиков (10 секунд)
                    //    Уровень: I (amplifier = 0)
                    player.addEffect(new MobEffectInstance(ModEffects.RADAWAY.get(), 120, 0));

                    // 2. Проигрываем звук
                    player.level().playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.RADAWAY_USE.get(), player.getSoundSource(), 1.0F, 1.0F);

                    // 3. Уменьшаем количество предметов в стаке
                    if (!player.getAbilities().instabuild) { // не уменьшать в креативе
                        stack.shrink(1);
                    }
                }
            })
    );
    public static final RegistrySupplier<Item> OIL_DETECTOR = ITEMS.register("oil_detector",
            () -> new OilDetectorItem(new Item.Properties()));

    public static final RegistrySupplier<Item> DEPTH_ORES_SCANNER = ITEMS.register("depth_ores_scanner",
            () -> new DepthOresScannerItem(new Item.Properties()));

    public static final RegistrySupplier<Item> RANGEFINDER = ITEMS.register("rangefinder",
            () -> new RangefinderItem(new Item.Properties()));

    public static final RegistrySupplier<Item> RANGE_DETONATOR = ITEMS.register("range_detonator",
            () -> new RangeDetonatorItem(new Item.Properties()));

    public static final RegistrySupplier<Item> MULTI_DETONATOR = ITEMS.register("multi_detonator",
            () -> new MultiDetonatorItem(new Item.Properties()));

    public static final RegistrySupplier<Item> DETONATOR = ITEMS.register("detonator",
            () -> new DetonatorItem(new Item.Properties()));

    public static final RegistrySupplier<Item> BILLET_PLUTONIUM = ITEMS.register("billet_plutonium",
            () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BALL_TNT = ITEMS.register("ball_tnt",
            () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> FAT_MAN_EXPLOSIVE = ITEMS.register("fat_man_explosive",
            () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> FAT_MAN_IGNITER = ITEMS.register("fat_man_igniter",
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> IGNITER = ITEMS.register("igniter",
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> FAT_MAN_CORE = ITEMS.register("fat_man_core",
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> CELL_SAS3 = ITEMS.register("cell_sas3",
            () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> ROD_QUAD_LEAD = ITEMS.register("rod_quad_lead",
            () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> ROD_QUAD_NP237 = ITEMS.register("rod_quad_np237",
            () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> ROD_QUAD_URANIUM = ITEMS.register("rod_quad_uranium",
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> CROWBAR = ITEMS.register("crowbar",
            () -> new Item(new Item.Properties()) {
                @Override
                public void appendHoverText(ItemStack stack, @Nullable Level level,
                                            @Nullable List<Component> tooltip, TooltipFlag flag) {
                    if (tooltip == null) return;

                    tooltip.add(Component.translatable("tooltip.hbm_m.crowbar.line1")
                            .withStyle(ChatFormatting.GRAY));
                    tooltip.add(Component.translatable("tooltip.hbm_m.crowbar.line2")
                            .withStyle(ChatFormatting.GRAY));
                }
            });


    public static final RegistrySupplier<Item> MALACHITE_CHUNK = ITEMS.register("malachite_chunk",
            () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> LIMESTONE = ITEMS.register("limestone",
            () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> SHELL_STEEL = ITEMS.register("shell_steel",
            () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> SHELL_COPPER = ITEMS.register("shell_copper",
            () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> SHELL_ALUMINUM = ITEMS.register("shell_aluminum",
            () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> SHELL_TITANIUM = ITEMS.register("shell_titanium",
            () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> CAN_KEY = ITEMS.register("can_key",
            () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> DEFUSER = ITEMS.register("defuser",
            () -> new Item(new Item.Properties()) {
                @Override
                public void appendHoverText(ItemStack stack, @Nullable Level level,
                                            @Nullable List<Component> tooltip, TooltipFlag flag) {
                    if (tooltip == null) return;

                    tooltip.add(Component.translatable("tooltip.hbm_m.defuser.line1")
                            .withStyle(ChatFormatting.GRAY));
                }
            });

    public static final RegistrySupplier<Item> GAS_EMPTY = ITEMS.register("gas_empty",
            () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> DUCTTAPE = ITEMS.register("ducttape",
            () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> HAZMAT_CLOTH = ITEMS.register("hazmat_cloth",
            () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> HAZMAT_CLOTH_GREY = ITEMS.register("hazmat_cloth_grey",
            () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> HAZMAT_CLOTH_RED = ITEMS.register("hazmat_cloth_red",
            () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> ASBESTOS_CLOTH = ITEMS.register("asbestos_cloth",
            () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BOLT_STEEL = ITEMS.register("bolt_steel",
            () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BOLT_LEAD = ITEMS.register("bolt_lead",
            () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BOLT_TUNGSTEN = ITEMS.register("bolt_tungsten",
            () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BOLT_HIGHSPEED_STEEL = ITEMS.register("bolt_highspeed_steel",
            () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> ZIRCONIUM_SHARP = ITEMS.register("zirconium_sharp",
            () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> COIL_TUNGSTEN = ITEMS.register("coil_tungsten",
            () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> COIL_GOLD_TORUS = ITEMS.register("coil_gold_torus",
            () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> COIL_GOLD = ITEMS.register("coil_gold",
            () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> COIL_MAGNETIZED_TUNGSTEN_TORUS = ITEMS.register("coil_magnetized_tungsten_torus",
            () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> COIL_MAGNETIZED_TUNGSTEN = ITEMS.register("coil_magnetized_tungsten",
            () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> COIL_COPPER_TORUS = ITEMS.register("coil_copper_torus",
            () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> COIL_COPPER = ITEMS.register("coil_copper",
            () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> COIL_ADVANCED_ALLOY_TORUS = ITEMS.register("coil_advanced_alloy_torus",
            () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> COIL_ADVANCED_ALLOY = ITEMS.register("coil_advanced_alloy",
            () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> MOTOR_DESH = ITEMS.register("motor_desh",
            () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> MOTOR_BISMUTH = ITEMS.register("motor_bismuth",
            () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> MOTOR = ITEMS.register("motor",
            () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BORAX = ITEMS.register("borax",
            () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> SCRAP = ITEMS.register("scrap",
            () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> DUST = ITEMS.register("dust",
            () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> DUST_TINY = ITEMS.register("dust_tiny",
            () -> new Item(new Item.Properties()));
    /** 1.7.10 ModItems.fallout — кучка осадков. */
    public static final RegistrySupplier<Item> FALLOUT = ITEMS.register("fallout",
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> LITHIUM_POWDER_TINY = ITEMS.register("lithium_powder_tiny",
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> CS137_POWDER_TINY = ITEMS.register("cs137_powder_tiny",
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> I131_POWDER_TINY = ITEMS.register("i131_powder_tiny",
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> XE135_POWDER_TINY = ITEMS.register("xe135_powder_tiny",
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> COAL_POWDER_TINY = ITEMS.register("coal_powder_tiny",
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> PALEOGENITE_POWDER_TINY = ITEMS.register("paleogenite_powder_tiny",
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> NUCLEAR_WASTE_TINY = ITEMS.register("nuclear_waste_tiny",
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> NUCLEAR_WASTE_LONG_TINY = ITEMS.register("nuclear_waste_long_tiny",
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> NUCLEAR_WASTE_LONG_DEPLETED_TINY = ITEMS.register("nuclear_waste_long_depleted_tiny",
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> NUCLEAR_WASTE_SHORT_TINY = ITEMS.register("nuclear_waste_short_tiny",
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> NUCLEAR_WASTE_SHORT_DEPLETED_TINY = ITEMS.register("nuclear_waste_short_depleted_tiny",
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> NUCLEAR_WASTE_VITRIFIED_TINY = ITEMS.register("nuclear_waste_vitrified_tiny",
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> NUGGET_MERCURY_TINY = ITEMS.register("nugget_mercury_tiny",
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> NUGGET_SILICON = ITEMS.register("nugget_silicon",
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> NUGGET_TANTALIUM = ITEMS.register("nugget_tantalium",
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> BILLET_SILICON = ITEMS.register("billet_silicon",
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> SILICON_CIRCUIT = ITEMS.register("silicon_circuit",
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> BISMOID_CIRCUIT = ITEMS.register("bismoid_circuit",
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> QUANTUM_CHIP = ITEMS.register("quantum_chip",
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> CAPACITOR_BOARD = ITEMS.register("capacitor_board",
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> CAPACITOR_TANTALUM = ITEMS.register("capacitor_tantalum",
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> BISMOID_CHIP = ITEMS.register("bismoid_chip",
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> CONTROLLER_CHASSIS = ITEMS.register("controller_chassis",
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> CONTROLLER = ITEMS.register("controller",
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> CONTROLLER_ADVANCED = ITEMS.register("controller_advanced",
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> QUANTUM_COMPUTER = ITEMS.register("quantum_computer",
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> QUANTUM_CIRCUIT = ITEMS.register("quantum_circuit",
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> ANALOG_CIRCUIT = ITEMS.register("analog_circuit",
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> INTEGRATED_CIRCUIT = ITEMS.register("integrated_circuit",
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> ADVANCED_CIRCUIT = ITEMS.register("advanced_circuit",
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> VACUUM_TUBE = ITEMS.register("vacuum_tube",
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> CAPACITOR = ITEMS.register("capacitor",
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> CENTRIFUGE_ELEMENT = ITEMS.register("centrifuge_element",
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> MICROCHIP = ITEMS.register("microchip",
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> ATOMIC_CLOCK = ITEMS.register("atomic_clock",
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> PCB = ITEMS.register("pcb",
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> METAL_ROD = ITEMS.register("metal_rod",
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> BATTLE_MODULE = ITEMS.register("battle_module",
            () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BATTLE_GEARS = ITEMS.register("battle_gears",
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> BATTLE_CASING = ITEMS.register("battle_casing",
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> BATTLE_SENSOR = ITEMS.register("battle_sensor",
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> BATTLE_COUNTER = ITEMS.register("battle_counter",
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> CRT_DISPLAY = ITEMS.register("crt_display",
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> MAGNETRON = ITEMS.register("magnetron",
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> TURBINE_TITANIUM = ITEMS.register("turbine_titanium",
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> PLATE_IRON = ITEMS.register("plate_iron",
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> PLATE_STEEL = ITEMS.register("plate_steel",
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> PLATE_GOLD = ITEMS.register("plate_gold",
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> PLATE_GUNMETAL = ITEMS.register("plate_gunmetal",
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> PLATE_GUNSTEEL = ITEMS.register("plate_gunsteel",
            () -> new Item(new Item.Properties()));
            
    public static final RegistrySupplier<Item> PLATE_TITANIUM = ITEMS.register("plate_titanium",
            () -> new Item(new Item.Properties())); 

    public static final RegistrySupplier<Item> PLATE_KEVLAR = ITEMS.register("plate_kevlar",
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> PLATE_LEAD = ITEMS.register("plate_lead",
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> PLATE_MIXED = ITEMS.register("plate_mixed",
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> PLATE_PAA = ITEMS.register("plate_paa",
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> INSULATOR = ITEMS.register("insulator",
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> PLATE_SATURNITE = ITEMS.register("plate_saturnite",
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> PLATE_SCHRABIDIUM = ITEMS.register("plate_schrabidium",
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> PLATE_COPPER = ITEMS.register("plate_copper",
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> PLATE_ALUMINUM = ITEMS.register("plate_aluminum",
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> PLATE_ADVANCED_ALLOY = ITEMS.register("plate_advanced_alloy",
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> PLATE_BISMUTH = ITEMS.register("plate_bismuth",
        () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> PLATE_ARMOR_AJR = ITEMS.register("plate_armor_ajr",
        () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> PLATE_ARMOR_DNT = ITEMS.register("plate_armor_dnt",
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> PLATE_ARMOR_DNT_RUSTED = ITEMS.register("plate_armor_dnt_rusted",
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> PLATE_ARMOR_FAU = ITEMS.register("plate_armor_fau",
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> PLATE_ARMOR_HEV = ITEMS.register("plate_armor_hev",
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> PLATE_ARMOR_LUNAR = ITEMS.register("plate_armor_lunar",
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> PLATE_ARMOR_TITANIUM = ITEMS.register("plate_armor_titanium",
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> PLATE_CAST = ITEMS.register("plate_cast",
        () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> PLATE_CAST_ALT = ITEMS.register("plate_cast_alt",
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> PLATE_CAST_BISMUTH = ITEMS.register("plate_cast_bismuth",
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> PLATE_CAST_DARK = ITEMS.register("plate_cast_dark",
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> PLATE_COMBINE_STEEL = ITEMS.register("plate_combine_steel",
        () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> PLATE_DURA_STEEL = ITEMS.register("plate_dura_steel",
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> PLATE_DALEKANIUM = ITEMS.register("plate_dalekanium",
        () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> PLATE_DESH = ITEMS.register("plate_desh",
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> PLATE_DINEUTRONIUM = ITEMS.register("plate_dineutronium",
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> PLATE_EUPHEMIUM = ITEMS.register("plate_euphemium",
            () -> new Item(new Item.Properties()));

    // Research-Reactor-Brennstoffplatten - Funktion/Reaktivitaet/Lebensdauer 1:1 aus dem Original
    // (ModItems.java, registerDefaults) uebernommen.
    public static final RegistrySupplier<Item> PLATE_FUEL_MOX = ITEMS.register("plate_fuel_mox",
        () -> new com.hbm_m.item.industrial.ItemPlateFuel(new Item.Properties().stacksTo(1),
                2_400_000L, com.hbm_m.item.industrial.ItemPlateFuel.FunctionEnum.LOGARITHM, 50));

    public static final RegistrySupplier<Item> PLATE_FUEL_PU238BE = ITEMS.register("plate_fuel_pu238be",
            () -> new com.hbm_m.item.industrial.ItemPlateFuel(new Item.Properties().stacksTo(1),
                    1_000_000L, com.hbm_m.item.industrial.ItemPlateFuel.FunctionEnum.PASSIVE, 50));

    public static final RegistrySupplier<Item> PLATE_FUEL_PU239 = ITEMS.register("plate_fuel_pu239",
            () -> new com.hbm_m.item.industrial.ItemPlateFuel(new Item.Properties().stacksTo(1),
                    2_000_000L, com.hbm_m.item.industrial.ItemPlateFuel.FunctionEnum.NEGATIVE_QUADRATIC, 50));

    public static final RegistrySupplier<Item> PLATE_FUEL_RA226BE = ITEMS.register("plate_fuel_ra226be",
            () -> new com.hbm_m.item.industrial.ItemPlateFuel(new Item.Properties().stacksTo(1),
                    1_300_000L, com.hbm_m.item.industrial.ItemPlateFuel.FunctionEnum.PASSIVE, 30));

    public static final RegistrySupplier<Item> PLATE_FUEL_SA326 = ITEMS.register("plate_fuel_sa326",
            () -> new com.hbm_m.item.industrial.ItemPlateFuel(new Item.Properties().stacksTo(1),
                    2_000_000L, com.hbm_m.item.industrial.ItemPlateFuel.FunctionEnum.LINEAR, 80));

    public static final RegistrySupplier<Item> PLATE_FUEL_U233 = ITEMS.register("plate_fuel_u233",
            () -> new com.hbm_m.item.industrial.ItemPlateFuel(new Item.Properties().stacksTo(1),
                    2_200_000L, com.hbm_m.item.industrial.ItemPlateFuel.FunctionEnum.SQUARE_ROOT, 50));

    public static final RegistrySupplier<Item> PLATE_FUEL_U235 = ITEMS.register("plate_fuel_u235",
            () -> new com.hbm_m.item.industrial.ItemPlateFuel(new Item.Properties().stacksTo(1),
                    2_200_000L, com.hbm_m.item.industrial.ItemPlateFuel.FunctionEnum.SQUARE_ROOT, 40));

    public static final RegistrySupplier<Item> RBMK_FUEL_DRX = ITEMS.register("rbmk_fuel_drx",
            () -> new RbmkFuelDrxItem(new Item.Properties()));

    public static final RegistrySupplier<Item> ROD_ZIRNOX_EMPTY = ITEMS.register("rod_zirnox_empty",
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> ROD_ZIRNOX_LES_FUEL = ITEMS.register("rod_zirnox_les_fuel",
            () -> new ZirnoxRodItem(new Item.Properties(), 150_000, 150, false));

    public static final RegistrySupplier<Item> ROD_ZIRNOX_LES_FUEL_DEPLETED = ITEMS.register("rod_zirnox_les_fuel_depleted",
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> ROD_ZIRNOX_LITHIUM = ITEMS.register("rod_zirnox_lithium",
            () -> new ZirnoxRodItem(new Item.Properties(), 20_000, 0, true));

    public static final RegistrySupplier<Item> ROD_ZIRNOX_MOX_FUEL = ITEMS.register("rod_zirnox_mox_fuel",
            () -> new ZirnoxRodItem(new Item.Properties(), 165_000, 75, false));

    public static final RegistrySupplier<Item> ROD_ZIRNOX_MOX_FUEL_DEPLETED = ITEMS.register("rod_zirnox_mox_fuel_depleted",
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> ROD_ZIRNOX_NATURAL_URANIUM_FUEL = ITEMS.register("rod_zirnox_natural_uranium_fuel",
            () -> new ZirnoxRodItem(new Item.Properties(), 250_000, 30, false));

    public static final RegistrySupplier<Item> ROD_ZIRNOX_PLUTONIUM_FUEL = ITEMS.register("rod_zirnox_plutonium_fuel",
            () -> new ZirnoxRodItem(new Item.Properties(), 175_000, 65, false));

    public static final RegistrySupplier<Item> ROD_ZIRNOX_PLUTONIUM_FUEL_DEPLETED = ITEMS.register("rod_zirnox_plutonium_fuel_depleted",
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> ROD_ZIRNOX_TH232 = ITEMS.register("rod_zirnox_th232",
            () -> new ZirnoxRodItem(new Item.Properties(), 20_000, 0, true));

    public static final RegistrySupplier<Item> ROD_ZIRNOX_THORIUM_FUEL = ITEMS.register("rod_zirnox_thorium_fuel",
            () -> new ZirnoxRodItem(new Item.Properties(), 200_000, 40, false));

    public static final RegistrySupplier<Item> ROD_ZIRNOX_THORIUM_FUEL_DEPLETED = ITEMS.register("rod_zirnox_thorium_fuel_depleted",
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> ROD_ZIRNOX_TRITIUM = ITEMS.register("rod_zirnox_tritium",
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> ROD_ZIRNOX_U233_FUEL = ITEMS.register("rod_zirnox_u233_fuel",
            () -> new ZirnoxRodItem(new Item.Properties(), 150_000, 100, false));

    public static final RegistrySupplier<Item> ROD_ZIRNOX_U233_FUEL_DEPLETED = ITEMS.register("rod_zirnox_u233_fuel_depleted",
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> ROD_ZIRNOX_U235_FUEL = ITEMS.register("rod_zirnox_u235_fuel",
            () -> new ZirnoxRodItem(new Item.Properties(), 165_000, 85, false));

    public static final RegistrySupplier<Item> ROD_ZIRNOX_U235_FUEL_DEPLETED = ITEMS.register("rod_zirnox_u235_fuel_depleted",
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> ROD_ZIRNOX_URANIUM_FUEL = ITEMS.register("rod_zirnox_uranium_fuel",
            () -> new ZirnoxRodItem(new Item.Properties(), 200_000, 50, false));

    public static final RegistrySupplier<Item> ROD_ZIRNOX_URANIUM_FUEL_DEPLETED = ITEMS.register("rod_zirnox_uranium_fuel_depleted",
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> ROD_ZIRNOX_ZFB_MOX = ITEMS.register("rod_zirnox_zfb_mox",
            () -> new ZirnoxRodItem(new Item.Properties(), 50_000, 35, false));

    public static final RegistrySupplier<Item> ROD_ZIRNOX_ZFB_MOX_DEPLETED = ITEMS.register("rod_zirnox_zfb_mox_depleted",
            () -> new Item(new Item.Properties()));

    // RAW METALS

    public static final RegistrySupplier<Item> URANIUM_RAW = ITEMS.register("uranium_raw",
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> LEAD_RAW = ITEMS.register("lead_raw",
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> BERYLLIUM_RAW = ITEMS.register("beryllium_raw",
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> ALUMINUM_RAW = ITEMS.register("aluminum_raw",
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> TITANIUM_RAW = ITEMS.register("titanium_raw",
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> THORIUM_RAW = ITEMS.register("thorium_raw",
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> COBALT_RAW = ITEMS.register("cobalt_raw",
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> TUNGSTEN_RAW = ITEMS.register("tungsten_raw",
            () -> new Item(new Item.Properties()));

    // Bedrock-Ore-Veredelung: fehlende Rohmaterialien aus der Original-Rezeptkette
    // (ItemBedrockOreNew.BedrockOreType, siehe CentrifugeRecipes/CrystallizerRecipes des Originals).
    public static final RegistrySupplier<Item> RADIUM_RAW = ITEMS.register("radium_raw",
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> SALTPETER = ITEMS.register("saltpeter",
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> CRYOLITE = ITEMS.register("cryolite",
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> MOLYSITE = ITEMS.register("molysite",
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> RAREEARTH_RAW = ITEMS.register("rareearth_raw",
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> POWDER_CHLOROCALCITE = ITEMS.register("powder_chlorocalcite",
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> POWDER_SODIUM = ITEMS.register("powder_sodium",
            () -> new Item(new Item.Properties()));



    // Материалы
    public static final RegistrySupplier<Item> SULFUR = ITEMS.register("sulfur",
            () -> new Item(new Item.Properties()));

    // Kokerer-Ausgabe - im Original ein Metadata-Subtyp von ItemEnumMulti(EnumCokeType) mit
    // COAL/LIGNITE/PETROLEUM; hier nur die fuer den Coker benoetigte PETROLEUM-Variante als
    // eigenstaendiges Item (COAL/LIGNITE gehoeren zu anderen, noch nicht portierten Maschinen).
    public static final RegistrySupplier<Item> COKE_PETROLEUM = ITEMS.register("coke_petroleum",
            () -> new Item(new Item.Properties()));

    // Ashpit-Ausgabe - im Original ein Metadata-Subtyp von ItemEnumMulti(EnumAshType) mit
    // WOOD/COAL/MISC/FLY/SOOT (+FULLERENE, hier nicht benoetigt); hier als 5 eigenstaendige Items.
    public static final RegistrySupplier<Item> ASH_WOOD = ITEMS.register("ash_wood", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> ASH_COAL = ITEMS.register("ash_coal", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> ASH_MISC = ITEMS.register("ash_misc", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> ASH_FLY  = ITEMS.register("ash_fly",  () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> ASH_SOOT = ITEMS.register("ash_soot", () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> SEQUESTRUM = ITEMS.register("sequestrum",
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> FLUORITE = ITEMS.register("fluorite",
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> RAREGROUND_ORE_CHUNK = ITEMS.register("rareground_ore_chunk",
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> FIRECLAY_BALL = ITEMS.register("fireclay_ball",
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> WOOD_ASH_POWDER = ITEMS.register("wood_ash_powder",
            () -> new Item(new Item.Properties()));

    /** Порт {@code powder_desh_mix}. */
    public static final RegistrySupplier<Item> POWDER_DESH_MIX = ITEMS.register("powder_desh_mix",
            () -> new Item(new Item.Properties()));

    /** Порт {@code powder_nitan_mix}. */
    public static final RegistrySupplier<Item> POWDER_NITAN_MIX = ITEMS.register("powder_nitan_mix",
            () -> new Item(new Item.Properties()));

    // Additional standalone powders (not from ModIngots)
    public static final RegistrySupplier<Item> COPPER_POWDER = ITEMS.register("copper_powder",
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> DIAMOND_POWDER = ITEMS.register("diamond_powder",
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> EMERALD_POWDER = ITEMS.register("emerald_powder",
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> LAPIS_POWDER = ITEMS.register("lapis_powder",
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> QUARTZ_POWDER = ITEMS.register("quartz_powder",
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> LIGNITE_POWDER = ITEMS.register("lignite_powder",
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> FIRE_POWDER = ITEMS.register("fire_powder",
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> LITHIUM_POWDER = ITEMS.register("lithium_powder",
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> FIREBRICK = ITEMS.register("firebrick",
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> LIGNITE = ITEMS.register("lignite",
            () -> new FuelItem(new Item.Properties(), 1000));

    public static final RegistrySupplier<Item> CINNABAR = ITEMS.register("cinnabar",
            () -> new Item(new Item.Properties()));



    // Crystals (auto-generated from textures/crystall/*.png)
    public static final RegistrySupplier<Item> CRYSTAL_ALUMINIUM = ITEMS.register("crystal_aluminium",
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> CRYSTAL_BERYLLIUM = ITEMS.register("crystal_beryllium",
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> CRYSTAL_CHARRED = ITEMS.register("crystal_charred",
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> CRYSTAL_CINNEBAR = ITEMS.register("crystal_cinnebar",
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> CRYSTAL_COAL = ITEMS.register("crystal_coal",
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> CRYSTAL_COBALT = ITEMS.register("crystal_cobalt",
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> CRYSTAL_COPPER = ITEMS.register("crystal_copper",
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> CRYSTAL_DIAMOND = ITEMS.register("crystal_diamond",
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> CRYSTAL_FLUORITE = ITEMS.register("crystal_fluorite",
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> CRYSTAL_GOLD = ITEMS.register("crystal_gold",
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> CRYSTAL_HARDENED = ITEMS.register("crystal_hardened",
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> CRYSTAL_HORN = ITEMS.register("crystal_horn",
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> CRYSTAL_IRON = ITEMS.register("crystal_iron",
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> CRYSTAL_LAPIS = ITEMS.register("crystal_lapis",
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> CRYSTAL_LEAD = ITEMS.register("crystal_lead",
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> CRYSTAL_LITHIUM = ITEMS.register("crystal_lithium",
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> CRYSTAL_NITER = ITEMS.register("crystal_niter",
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> CRYSTAL_OSMIRIDIUM = ITEMS.register("crystal_osmiridium",
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> CRYSTAL_PHOSPHORUS = ITEMS.register("crystal_phosphorus",
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> CRYSTAL_PLUTONIUM = ITEMS.register("crystal_plutonium",
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> CRYSTAL_PULSAR = ITEMS.register("crystal_pulsar",
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> CRYSTAL_RARE = ITEMS.register("crystal_rare",
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> CRYSTAL_REDSTONE = ITEMS.register("crystal_redstone",
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> CRYSTAL_SCHRABIDIUM = ITEMS.register("crystal_schrabidium",
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> CRYSTAL_SCHRARANIUM = ITEMS.register("crystal_schraranium",
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> CRYSTAL_STARMETAL = ITEMS.register("crystal_starmetal",
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> CRYSTAL_SULFUR = ITEMS.register("crystal_sulfur",
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> CRYSTAL_THORIUM = ITEMS.register("crystal_thorium",
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> CRYSTAL_TITANIUM = ITEMS.register("crystal_titanium",
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> CRYSTAL_TRIXITE = ITEMS.register("crystal_trixite",
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> CRYSTAL_TUNGSTEN = ITEMS.register("crystal_tungsten",
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> CRYSTAL_URANIUM = ITEMS.register("crystal_uranium",
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> CRYSTAL_VIRUS = ITEMS.register("crystal_virus",
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> CRYSTAL_XEN = ITEMS.register("crystal_xen",
            () -> new Item(new Item.Properties()));




    // Здесь мы регистрируем мультиблочные структуры для того, чтобы MultiblockBlockItem при установке мог обрабатывать их на наличие препятствующих блоков.

    public static final RegistrySupplier<Item> MACHINE_ASSEMBLER = ITEMS.register("machine_assembler",
        () -> new MultiblockBlockItem(ModBlocks.MACHINE_ASSEMBLER.get(), new Item.Properties()));
            
    public static final RegistrySupplier<Item> ADVANCED_ASSEMBLY_MACHINE = ITEMS.register("advanced_assembly_machine",
        () -> new MultiblockBlockItem(ModBlocks.ADVANCED_ASSEMBLY_MACHINE.get(), new Item.Properties()));

    public static final RegistrySupplier<Item> HYDRAULIC_FRACKINING_TOWER = ITEMS.register("hydraulic_frackining_tower",
        () -> new MultiblockBlockItem(ModBlocks.HYDRAULIC_FRACKINING_TOWER.get(), new Item.Properties()));

    public static final RegistrySupplier<Item> COOLING_TOWER = ITEMS.register("cooling_tower",
        () -> new MultiblockBlockItem(ModBlocks.COOLING_TOWER.get(), new Item.Properties()));

	public static final RegistrySupplier<Item> TOWER_SMALL = ITEMS.register("tower_small",
        () -> new MultiblockBlockItem(ModBlocks.TOWER_SMALL.get(), new Item.Properties()));

	public static final RegistrySupplier<Item> CYCLOTRON = ITEMS.register("cyclotron",
        () -> new MultiblockBlockItem(ModBlocks.CYCLOTRON.get(), new Item.Properties()));

    // ─── Cyclotron particle parts ─────────────────────────────────────────────
    /** Lithium ion — accelerated in the cyclotron as a low-energy particle. */
    public static final RegistrySupplier<Item> PART_LITHIUM    = ITEMS.register("part_lithium",    () -> new Item(new Item.Properties().stacksTo(16)));
    /** Beryllium particle — medium-energy cyclotron projectile. */
    public static final RegistrySupplier<Item> PART_BERYLLIUM  = ITEMS.register("part_beryllium",  () -> new Item(new Item.Properties().stacksTo(16)));
    /** Carbon (coal-derived) particle — low-energy cyclotron projectile. */
    public static final RegistrySupplier<Item> PART_CARBON     = ITEMS.register("part_carbon",     () -> new Item(new Item.Properties().stacksTo(16)));
    /** Copper ion — medium-energy cyclotron projectile. */
    public static final RegistrySupplier<Item> PART_COPPER     = ITEMS.register("part_copper",     () -> new Item(new Item.Properties().stacksTo(16)));
    /** Plutonium nucleus — high-energy cyclotron projectile, produces australium. */
    public static final RegistrySupplier<Item> PART_PLUTONIUM  = ITEMS.register("part_plutonium",  () -> new Item(new Item.Properties().stacksTo(16)));

    // ─── Cast Molds ───────────────────────────────────────────────────────────
    public static final RegistrySupplier<Item> MOLD_BARREL_HEAVY = ITEMS.register("mold_barrel_heavy",
            () -> new com.hbm_m.item.material.ItemCastMold(com.hbm_m.item.material.ItemCastMold.MoldType.BARREL_HEAVY, new Item.Properties()));
    public static final RegistrySupplier<Item> MOLD_BARREL_LIGHT = ITEMS.register("mold_barrel_light",
            () -> new com.hbm_m.item.material.ItemCastMold(com.hbm_m.item.material.ItemCastMold.MoldType.BARREL_LIGHT, new Item.Properties()));
    public static final RegistrySupplier<Item> MOLD_BASE = ITEMS.register("mold_base",
            () -> new com.hbm_m.item.material.ItemCastMold(com.hbm_m.item.material.ItemCastMold.MoldType.BASE, new Item.Properties()));
    public static final RegistrySupplier<Item> MOLD_BILLET = ITEMS.register("mold_billet",
            () -> new com.hbm_m.item.material.ItemCastMold(com.hbm_m.item.material.ItemCastMold.MoldType.BILLET, new Item.Properties()));
    public static final RegistrySupplier<Item> MOLD_BLADE = ITEMS.register("mold_blade",
            () -> new com.hbm_m.item.material.ItemCastMold(com.hbm_m.item.material.ItemCastMold.MoldType.BLADE, new Item.Properties()));
    public static final RegistrySupplier<Item> MOLD_BLADES = ITEMS.register("mold_blades",
            () -> new com.hbm_m.item.material.ItemCastMold(com.hbm_m.item.material.ItemCastMold.MoldType.BLADES, new Item.Properties()));
    public static final RegistrySupplier<Item> MOLD_BLOCK = ITEMS.register("mold_block",
            () -> new com.hbm_m.item.material.ItemCastMold(com.hbm_m.item.material.ItemCastMold.MoldType.BLOCK, new Item.Properties()));
    public static final RegistrySupplier<Item> MOLD_C357 = ITEMS.register("mold_c357",
            () -> new com.hbm_m.item.material.ItemCastMold(com.hbm_m.item.material.ItemCastMold.MoldType.C357, new Item.Properties()));
    public static final RegistrySupplier<Item> MOLD_CBUCKSHOT = ITEMS.register("mold_cbuckshot",
            () -> new com.hbm_m.item.material.ItemCastMold(com.hbm_m.item.material.ItemCastMold.MoldType.CBUCKSHOT, new Item.Properties()));
    public static final RegistrySupplier<Item> MOLD_GEM = ITEMS.register("mold_gem",
            () -> new com.hbm_m.item.material.ItemCastMold(com.hbm_m.item.material.ItemCastMold.MoldType.GEM, new Item.Properties()));
    public static final RegistrySupplier<Item> MOLD_GRIP = ITEMS.register("mold_grip",
            () -> new com.hbm_m.item.material.ItemCastMold(com.hbm_m.item.material.ItemCastMold.MoldType.GRIP, new Item.Properties()));
    public static final RegistrySupplier<Item> MOLD_HULL_BIG = ITEMS.register("mold_hull_big",
            () -> new com.hbm_m.item.material.ItemCastMold(com.hbm_m.item.material.ItemCastMold.MoldType.HULL_BIG, new Item.Properties()));
    public static final RegistrySupplier<Item> MOLD_HULL_SMALL = ITEMS.register("mold_hull_small",
            () -> new com.hbm_m.item.material.ItemCastMold(com.hbm_m.item.material.ItemCastMold.MoldType.HULL_SMALL, new Item.Properties()));
    public static final RegistrySupplier<Item> MOLD_INGOT = ITEMS.register("mold_ingot",
            () -> new com.hbm_m.item.material.ItemCastMold(com.hbm_m.item.material.ItemCastMold.MoldType.INGOT, new Item.Properties()));
    public static final RegistrySupplier<Item> MOLD_INGOTS = ITEMS.register("mold_ingots",
            () -> new com.hbm_m.item.material.ItemCastMold(com.hbm_m.item.material.ItemCastMold.MoldType.INGOTS, new Item.Properties()));
    public static final RegistrySupplier<Item> MOLD_MECHANISM = ITEMS.register("mold_mechanism",
            () -> new com.hbm_m.item.material.ItemCastMold(com.hbm_m.item.material.ItemCastMold.MoldType.MECHANISM, new Item.Properties()));
    public static final RegistrySupplier<Item> MOLD_MOGUS = ITEMS.register("mold_mogus",
            () -> new com.hbm_m.item.material.ItemCastMold(com.hbm_m.item.material.ItemCastMold.MoldType.MOGUS, new Item.Properties()));
    public static final RegistrySupplier<Item> MOLD_NUGGET = ITEMS.register("mold_nugget",
            () -> new com.hbm_m.item.material.ItemCastMold(com.hbm_m.item.material.ItemCastMold.MoldType.NUGGET, new Item.Properties()));
    public static final RegistrySupplier<Item> MOLD_PIPE = ITEMS.register("mold_pipe",
            () -> new com.hbm_m.item.material.ItemCastMold(com.hbm_m.item.material.ItemCastMold.MoldType.PIPE, new Item.Properties()));
    public static final RegistrySupplier<Item> MOLD_PIPES = ITEMS.register("mold_pipes",
            () -> new com.hbm_m.item.material.ItemCastMold(com.hbm_m.item.material.ItemCastMold.MoldType.PIPES, new Item.Properties()));
    public static final RegistrySupplier<Item> MOLD_PLATE = ITEMS.register("mold_plate",
            () -> new com.hbm_m.item.material.ItemCastMold(com.hbm_m.item.material.ItemCastMold.MoldType.PLATE, new Item.Properties()));
    public static final RegistrySupplier<Item> MOLD_PLATE_CAST = ITEMS.register("mold_plate_cast",
            () -> new com.hbm_m.item.material.ItemCastMold(com.hbm_m.item.material.ItemCastMold.MoldType.PLATE_CAST, new Item.Properties()));
    public static final RegistrySupplier<Item> MOLD_PLATES = ITEMS.register("mold_plates",
            () -> new com.hbm_m.item.material.ItemCastMold(com.hbm_m.item.material.ItemCastMold.MoldType.PLATES, new Item.Properties()));
    public static final RegistrySupplier<Item> MOLD_PLATES_CAST = ITEMS.register("mold_plates_cast",
            () -> new com.hbm_m.item.material.ItemCastMold(com.hbm_m.item.material.ItemCastMold.MoldType.PLATES_CAST, new Item.Properties()));
    public static final RegistrySupplier<Item> MOLD_RECEIVER_HEAVY = ITEMS.register("mold_receiver_heavy",
            () -> new com.hbm_m.item.material.ItemCastMold(com.hbm_m.item.material.ItemCastMold.MoldType.RECEIVER_HEAVY, new Item.Properties()));
    public static final RegistrySupplier<Item> MOLD_RECEIVER_LIGHT = ITEMS.register("mold_receiver_light",
            () -> new com.hbm_m.item.material.ItemCastMold(com.hbm_m.item.material.ItemCastMold.MoldType.RECEIVER_LIGHT, new Item.Properties()));
    public static final RegistrySupplier<Item> MOLD_SHELL = ITEMS.register("mold_shell",
            () -> new com.hbm_m.item.material.ItemCastMold(com.hbm_m.item.material.ItemCastMold.MoldType.SHELL, new Item.Properties()));
    public static final RegistrySupplier<Item> MOLD_STAMP = ITEMS.register("mold_stamp",
            () -> new com.hbm_m.item.material.ItemCastMold(com.hbm_m.item.material.ItemCastMold.MoldType.STAMP, new Item.Properties()));
    public static final RegistrySupplier<Item> MOLD_STEEL_BASE = ITEMS.register("mold_steel_base",
            () -> new com.hbm_m.item.material.ItemCastMold(com.hbm_m.item.material.ItemCastMold.MoldType.STEEL_BASE, new Item.Properties()));
    public static final RegistrySupplier<Item> MOLD_STOCK = ITEMS.register("mold_stock",
            () -> new com.hbm_m.item.material.ItemCastMold(com.hbm_m.item.material.ItemCastMold.MoldType.STOCK, new Item.Properties()));
    public static final RegistrySupplier<Item> MOLD_WIRE = ITEMS.register("mold_wire",
            () -> new com.hbm_m.item.material.ItemCastMold(com.hbm_m.item.material.ItemCastMold.MoldType.WIRE, new Item.Properties()));
    public static final RegistrySupplier<Item> MOLD_WIRE_DENSE = ITEMS.register("mold_wire_dense",
            () -> new com.hbm_m.item.material.ItemCastMold(com.hbm_m.item.material.ItemCastMold.MoldType.WIRE_DENSE, new Item.Properties()));
    public static final RegistrySupplier<Item> MOLD_WIRES_DENSE = ITEMS.register("mold_wires_dense",
            () -> new com.hbm_m.item.material.ItemCastMold(com.hbm_m.item.material.ItemCastMold.MoldType.WIRES_DENSE, new Item.Properties()));

    // ─── Cast Plates (plate_cast_<material>) ─────────────────────────────────
    // Port of the original metadata-based plate_cast item. Each material is a
    // separate registration in the modern system.
    public static final RegistrySupplier<Item> PLATE_CAST_IRON        = ITEMS.register("plate_cast_iron",        () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> PLATE_CAST_STEEL       = ITEMS.register("plate_cast_steel",       () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> PLATE_CAST_COPPER      = ITEMS.register("plate_cast_copper",      () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> PLATE_CAST_GOLD        = ITEMS.register("plate_cast_gold",        () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> PLATE_CAST_TITANIUM    = ITEMS.register("plate_cast_titanium",    () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> PLATE_CAST_ALUMINIUM   = ITEMS.register("plate_cast_aluminium",   () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> PLATE_CAST_TUNGSTEN    = ITEMS.register("plate_cast_tungsten",    () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> PLATE_CAST_ZIRCONIUM   = ITEMS.register("plate_cast_zirconium",   () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> PLATE_CAST_OSMIRIDIUM  = ITEMS.register("plate_cast_osmiridium",  () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> PLATE_CAST_ALLOY       = ITEMS.register("plate_cast_alloy",       () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> PLATE_CAST_DURA_STEEL  = ITEMS.register("plate_cast_dura_steel",  () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> PLATE_CAST_DESH        = ITEMS.register("plate_cast_desh",        () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> PLATE_CAST_STAR_METAL  = ITEMS.register("plate_cast_star_metal",  () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> PLATE_CAST_TCALLOY     = ITEMS.register("plate_cast_tcalloy",     () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> PLATE_CAST_CDALLOY     = ITEMS.register("plate_cast_cdalloy",     () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> PLATE_CAST_CMB         = ITEMS.register("plate_cast_cmb",         () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> PLATE_CAST_SCHRABIDIUM = ITEMS.register("plate_cast_schrabidium", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> PLATE_CAST_BBRONZE     = ITEMS.register("plate_cast_bbronze",     () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> PLATE_CAST_ABRONZE     = ITEMS.register("plate_cast_abronze",     () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> PLATE_CAST_SATURNITE   = ITEMS.register("plate_cast_saturnite",   () -> new Item(new Item.Properties()));

    // ─── Welded Plates (plate_welded_<material>) ─────────────────────────────
    // Produced by the Arc Welder from 2× cast plates of the same material.
    public static final RegistrySupplier<Item> PLATE_WELDED_IRON       = ITEMS.register("plate_welded_iron",       () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> PLATE_WELDED_STEEL      = ITEMS.register("plate_welded_steel",      () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> PLATE_WELDED_COPPER     = ITEMS.register("plate_welded_copper",     () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> PLATE_WELDED_TITANIUM   = ITEMS.register("plate_welded_titanium",   () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> PLATE_WELDED_ALUMINIUM  = ITEMS.register("plate_welded_aluminium",  () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> PLATE_WELDED_TUNGSTEN   = ITEMS.register("plate_welded_tungsten",   () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> PLATE_WELDED_ZIRCONIUM  = ITEMS.register("plate_welded_zirconium",  () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> PLATE_WELDED_OSMIRIDIUM = ITEMS.register("plate_welded_osmiridium", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> PLATE_WELDED_TCALLOY    = ITEMS.register("plate_welded_tcalloy",    () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> PLATE_WELDED_CDALLOY    = ITEMS.register("plate_welded_cdalloy",    () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> PLATE_WELDED_CMB        = ITEMS.register("plate_welded_cmb",        () -> new Item(new Item.Properties()));

	public static final RegistrySupplier<Item> ZIRNOX = ITEMS.register("zirnox",
        () -> new MultiblockBlockItem(ModBlocks.ZIRNOX.get(), new Item.Properties()));

	public static final RegistrySupplier<Item> ARC_WELDER = ITEMS.register("arc_welder",
        () -> new MultiblockBlockItem(ModBlocks.ARC_WELDER.get(), new Item.Properties()));

	public static final RegistrySupplier<Item> SOLDERING_STATION = ITEMS.register("soldering_station",
        () -> new MultiblockBlockItem(ModBlocks.SOLDERING_STATION.get(), new Item.Properties()));

	public static final RegistrySupplier<Item> MIXER = ITEMS.register("mixer",
        () -> new MultiblockBlockItem(ModBlocks.MIXER.get(), new Item.Properties()));

	public static final RegistrySupplier<Item> DERRICK = ITEMS.register("derrick",
        () -> new MultiblockBlockItem(ModBlocks.DERRICK.get(), new Item.Properties()));

	public static final RegistrySupplier<Item> MACHINE_WELL = ITEMS.register("machine_well",
        () -> new MultiblockBlockItem(ModBlocks.MACHINE_WELL.get(), new Item.Properties()));

	public static final RegistrySupplier<Item> ASHPIT = ITEMS.register("ashpit",
        () -> new MultiblockBlockItem(ModBlocks.ASHPIT.get(), new Item.Properties()));

	public static final RegistrySupplier<Item> REACTOR_RESEARCH = ITEMS.register("reactor_research",
        () -> new MultiblockBlockItem(ModBlocks.REACTOR_RESEARCH.get(), new Item.Properties()));

	public static final RegistrySupplier<Item> RBMK_CONSOLE = ITEMS.register("rbmk_console",
        () -> new MultiblockBlockItem(ModBlocks.RBMK_CONSOLE.get(), new Item.Properties()));

	public static final RegistrySupplier<Item> FLARE_STACK = ITEMS.register("flare_stack",
        () -> new MultiblockBlockItem(ModBlocks.FLARE_STACK.get(), new Item.Properties()));

	public static final RegistrySupplier<Item> PUMPJACK = ITEMS.register("pumpjack",
        () -> new MultiblockBlockItem(ModBlocks.PUMPJACK.get(), new Item.Properties()));

	public static final RegistrySupplier<Item> RADAR = ITEMS.register("radar",
        () -> new MultiblockBlockItem(ModBlocks.RADAR.get(), new Item.Properties()));

	public static final RegistrySupplier<Item> LARGE_RADAR = ITEMS.register("large_radar",
	    () -> new MultiblockBlockItem(ModBlocks.LARGE_RADAR.get(), new Item.Properties()));

	public static final RegistrySupplier<Item> RADAR_SCREEN = ITEMS.register("radar_screen",
	    () -> new MultiblockBlockItem(ModBlocks.RADAR_SCREEN.get(), new Item.Properties()));

	public static final RegistrySupplier<Item> CRACKING_TOWER = ITEMS.register("cracking_tower",
        () -> new MultiblockBlockItem(ModBlocks.CRACKING_TOWER.get(), new Item.Properties()));

	public static final RegistrySupplier<Item> FRACTION_TOWER = ITEMS.register("fraction_tower",
        () -> new MultiblockBlockItem(ModBlocks.FRACTION_TOWER.get(), new Item.Properties()));

	public static final RegistrySupplier<Item> MINING_DRILL = ITEMS.register("mining_drill",
        () -> new MultiblockBlockItem(ModBlocks.MINING_DRILL.get(), new Item.Properties()));

	public static final RegistrySupplier<Item> FEL = ITEMS.register("fel",
        () -> new MultiblockBlockItem(ModBlocks.FEL.get(), new Item.Properties()));

	public static final RegistrySupplier<Item> SILEX = ITEMS.register("silex",
        () -> new MultiblockBlockItem(ModBlocks.SILEX.get(), new Item.Properties()));

	public static final RegistrySupplier<Item> CHEMICAL_PLANT = ITEMS.register("chemical_plant",
        () -> new MultiblockBlockItem(ModBlocks.CHEMICAL_PLANT.get(), new Item.Properties()));

	public static final RegistrySupplier<Item> GAS_CENTRIFUGE = ITEMS.register("gas_centrifuge",
        () -> new MultiblockBlockItem(ModBlocks.GAS_CENTRIFUGE.get(), new Item.Properties()));

	public static final RegistrySupplier<Item> CRYSTALLIZER = ITEMS.register("crystallizer",
        () -> new MultiblockBlockItem(ModBlocks.CRYSTALLIZER.get(), new Item.Properties()));

	public static final RegistrySupplier<Item> BREEDER = ITEMS.register("breeder",
        () -> new MultiblockBlockItem(ModBlocks.BREEDER.get(), new Item.Properties()));

	public static final RegistrySupplier<Item> LARGE_PYLON = ITEMS.register("large_pylon",
        () -> new MultiblockBlockItem(ModBlocks.LARGE_PYLON.get(), new Item.Properties()));

	public static final RegistrySupplier<Item> CENTRIFUGE = ITEMS.register("centrifuge",
        () -> new MultiblockBlockItem(ModBlocks.CENTRIFUGE.get(), new Item.Properties()));

	public static final RegistrySupplier<Item> FLUID_TANK = ITEMS.register("fluid_tank",
        () -> new MultiblockBlockItem(ModBlocks.FLUID_TANK.get(), new Item.Properties()));

	public static final RegistrySupplier<Item> BAT9000 = ITEMS.register("bat9000",
        () -> new MultiblockBlockItem(ModBlocks.BAT9000.get(), new Item.Properties()));

    public static final RegistrySupplier<Item> MACHINE_BATTERY_SOCKET = ITEMS.register("machine_battery_socket",
        () -> new MultiblockBlockItem(ModBlocks.MACHINE_BATTERY_SOCKET.get(), new Item.Properties()));

    public static final RegistrySupplier<Item> PRESS = ITEMS.register("press",
        () -> new MultiblockBlockItem(ModBlocks.PRESS.get(), new Item.Properties()));

    public static final RegistrySupplier<Item> WOOD_BURNER = ITEMS.register("wood_burner",
        () -> new MultiblockBlockItem(ModBlocks.WOOD_BURNER.get(), new Item.Properties()));

    public static final RegistrySupplier<Item> INDUSTRIAL_BOILER = ITEMS.register("industrial_boiler",
        () -> new MultiblockBlockItem(ModBlocks.INDUSTRIAL_BOILER.get(), new Item.Properties()));

    public static final RegistrySupplier<Item> SOLAR_BOILER = ITEMS.register("solar_boiler",
        () -> new MultiblockBlockItem(ModBlocks.SOLAR_BOILER.get(), new Item.Properties()));

    public static final RegistrySupplier<Item> SOLAR_MIRRORS = ITEMS.register("solar_mirrors",
        () -> new MultiblockBlockItem(ModBlocks.SOLAR_MIRRORS.get(), new Item.Properties()));

    public static final RegistrySupplier<Item> WATZ_POWERPLANT = ITEMS.register("watz_powerplant",
        () -> new MultiblockBlockItem(ModBlocks.WATZ_POWERPLANT.get(), new Item.Properties()));

    // Watz reactor pellets - see com.hbm_m.item.nuclear.WatzPelletType for the mechanics.
    public static final RegistrySupplier<Item> WATZ_PELLET_SCHRABIDIUM_OXIDE = ITEMS.register("watz_pellet_schrabidium_oxide",
        () -> new WatzPelletItem(new Item.Properties(), WatzPelletType.SCHRABIDIUM_OXIDE));
    public static final RegistrySupplier<Item> WATZ_PELLET_SCHRABIDIUM_OXIDE_DEPLETED = ITEMS.register("watz_pellet_schrabidium_oxide_depleted",
        () -> new Item(new Item.Properties().stacksTo(16)));

    public static final RegistrySupplier<Item> WATZ_PELLET_LES_OXIDE = ITEMS.register("watz_pellet_les_oxide",
        () -> new WatzPelletItem(new Item.Properties(), WatzPelletType.LES_OXIDE));
    public static final RegistrySupplier<Item> WATZ_PELLET_LES_OXIDE_DEPLETED = ITEMS.register("watz_pellet_les_oxide_depleted",
        () -> new Item(new Item.Properties().stacksTo(16)));

    public static final RegistrySupplier<Item> WATZ_PELLET_NATURAL_URANIUM = ITEMS.register("watz_pellet_natural_uranium",
        () -> new WatzPelletItem(new Item.Properties(), WatzPelletType.NATURAL_URANIUM));
    public static final RegistrySupplier<Item> WATZ_PELLET_NATURAL_URANIUM_DEPLETED = ITEMS.register("watz_pellet_natural_uranium_depleted",
        () -> new Item(new Item.Properties().stacksTo(16)));

    public static final RegistrySupplier<Item> WATZ_PELLET_BORON_CARBIDE = ITEMS.register("watz_pellet_boron_carbide",
        () -> new WatzPelletItem(new Item.Properties(), WatzPelletType.BORON_CARBIDE));
    public static final RegistrySupplier<Item> WATZ_PELLET_BORON_CARBIDE_DEPLETED = ITEMS.register("watz_pellet_boron_carbide_depleted",
        () -> new Item(new Item.Properties().stacksTo(16)));

    public static final RegistrySupplier<Item> WATZ_PELLET_LEAD_SHIELD = ITEMS.register("watz_pellet_lead_shield",
        () -> new WatzPelletItem(new Item.Properties(), WatzPelletType.LEAD_SHIELD));
    public static final RegistrySupplier<Item> WATZ_PELLET_LEAD_SHIELD_DEPLETED = ITEMS.register("watz_pellet_lead_shield_depleted",
        () -> new Item(new Item.Properties().stacksTo(16)));

    public static final RegistrySupplier<Item> HYDROTREATER = ITEMS.register("hydrotreater",
        () -> new MultiblockBlockItem(ModBlocks.HYDROTREATER.get(), new Item.Properties()));

    public static final RegistrySupplier<Item> CATALYTIC_REFORMER = ITEMS.register("catalytic_reformer",
        () -> new MultiblockBlockItem(ModBlocks.CATALYTIC_REFORMER.get(), new Item.Properties()));

    public static final RegistrySupplier<Item> DEUTERIUM_TOWER = ITEMS.register("deuterium_tower",
        () -> new MultiblockBlockItem(ModBlocks.DEUTERIUM_TOWER.get(), new Item.Properties()));

    public static final RegistrySupplier<Item> CHEMICAL_FACTORY = ITEMS.register("chemical_factory",
        () -> new MultiblockBlockItem(ModBlocks.CHEMICAL_FACTORY.get(), new Item.Properties()));

    public static final RegistrySupplier<Item> STEAM_TURBINE = ITEMS.register("steam_turbine",
        () -> new MultiblockBlockItem(ModBlocks.STEAM_TURBINE.get(), new Item.Properties()));

    public static final RegistrySupplier<Item> LIQUEFACTOR = ITEMS.register("liquefactor",
        () -> new MultiblockBlockItem(ModBlocks.LIQUEFACTOR.get(), new Item.Properties()));

    public static final RegistrySupplier<Item> CORE_EMITTER = ITEMS.register("core_emitter",
        () -> new MultiblockBlockItem(ModBlocks.CORE_EMITTER.get(), new Item.Properties()));

    public static final RegistrySupplier<Item> CORE_INJECTOR = ITEMS.register("core_injector",
        () -> new MultiblockBlockItem(ModBlocks.CORE_INJECTOR.get(), new Item.Properties()));

    public static final RegistrySupplier<Item> CORE_RECEIVER = ITEMS.register("core_receiver",
        () -> new MultiblockBlockItem(ModBlocks.CORE_RECEIVER.get(), new Item.Properties()));

    public static final RegistrySupplier<Item> VACUUM_DISTILL = ITEMS.register("vacuum_distill",
        () -> new MultiblockBlockItem(ModBlocks.VACUUM_DISTILL.get(), new Item.Properties()));

    public static final RegistrySupplier<Item> TURBOFAN = ITEMS.register("turbofan",
        () -> new MultiblockBlockItem(ModBlocks.TURBOFAN.get(), new Item.Properties()));

    public static final RegistrySupplier<Item> INDUSTRIAL_TURBINE = ITEMS.register("industrial_turbine",
        () -> new MultiblockBlockItem(ModBlocks.INDUSTRIAL_TURBINE.get(), new Item.Properties()));

    public static final RegistrySupplier<Item> MACHINE_CHUNGUS = ITEMS.register("machine_chungus",
        () -> new MultiblockBlockItem(ModBlocks.MACHINE_CHUNGUS.get(), new Item.Properties()));

    public static final RegistrySupplier<Item> TURBINE = ITEMS.register("turbine",
        () -> new MultiblockBlockItem(ModBlocks.TURBINE.get(), new Item.Properties()));

    public static final RegistrySupplier<Item> SUBSTATION = ITEMS.register("substation",
        () -> new MultiblockBlockItem(ModBlocks.SUBSTATION.get(), new Item.Properties()));

    public static final RegistrySupplier<Item> REFINERY = ITEMS.register("refinery",
        () -> new MultiblockBlockItem(ModBlocks.REFINERY.get(), new Item.Properties()));
	public static final RegistrySupplier<Item> LAUNCH_PAD = ITEMS.register("launch_pad",
        () -> new MultiblockBlockItem(ModBlocks.LAUNCH_PAD.get(), new Item.Properties()));

	public static final RegistrySupplier<Item> LAUNCH_PAD_RUSTED = ITEMS.register("launch_pad_rusted",
        () -> new MultiblockBlockItem(ModBlocks.LAUNCH_PAD_RUSTED.get(), new Item.Properties()));

	public static final RegistrySupplier<Item> NUKE_FAT_MAN = ITEMS.register("nuke_fat_man",
        () -> new MultiblockBlockItem(ModBlocks.NUKE_FAT_MAN.get(), new Item.Properties()));

    public static final RegistrySupplier<Item> NUKE_PROTOTYPE = ITEMS.register("nuke_prototype",
        () -> new net.minecraft.world.item.BlockItem(com.hbm_m.block.ModBlocks.NUKE_PROTOTYPE.get(), new Item.Properties()));

    // ПРОТОТИП РАКЕТЫ (TIER 0, MICRO)
    public static final RegistrySupplier<Item> MISSILE_TEST = ITEMS.register("missile_test",
        () -> new MissileItem(MissileItem.MissileFormFactor.MICRO, MissileItem.MissileTier.TIER0,
                MissileItem.MissileFuel.SOLID));

    public static final RegistrySupplier<Item> MISSILE_ABM = ITEMS.register("missile_abm",
                () -> new MissileItem(MissileItem.MissileFormFactor.ABM, MissileItem.MissileTier.TIER1,
                                MissileItem.MissileFuel.SOLID));

  /** Сингулярности / опасные дропы (1.7.10 {@code ModItems.black_hole}, {@code pellet_antimatter}, {@code flame_pony}). */
    public static final RegistrySupplier<Item> BLACK_HOLE = ITEMS.register("black_hole",
            () -> new com.hbm_m.item.special.ItemDrop(new Item.Properties().stacksTo(1)));
    public static final RegistrySupplier<Item> PELLET_ANTIMATTER = ITEMS.register("pellet_antimatter",
            () -> new com.hbm_m.item.special.ItemDrop(new Item.Properties().stacksTo(1)));
    public static final RegistrySupplier<Item> FLAME_PONY = ITEMS.register("flame_pony",
            () -> new Item(new Item.Properties()));

    // Tier 0
    public static final RegistrySupplier<Item> MISSILE_MICRO = ITEMS.register("missile_micro",
            () -> new MissileItem(MissileItem.MissileFormFactor.MICRO, MissileItem.MissileTier.TIER0));
    public static final RegistrySupplier<Item> MISSILE_SCHRABIDIUM = ITEMS.register("missile_schrabidium",
            () -> new MissileItem(MissileItem.MissileFormFactor.MICRO, MissileItem.MissileTier.TIER0));
    public static final RegistrySupplier<Item> MISSILE_BHOLE = ITEMS.register("missile_bhole",
            () -> new MissileItem(MissileItem.MissileFormFactor.MICRO, MissileItem.MissileTier.TIER0));
    public static final RegistrySupplier<Item> MISSILE_TAINT = ITEMS.register("missile_taint",
            () -> new MissileItem(MissileItem.MissileFormFactor.MICRO, MissileItem.MissileTier.TIER0));
    public static final RegistrySupplier<Item> MISSILE_EMP = ITEMS.register("missile_emp",
            () -> new MissileItem(MissileItem.MissileFormFactor.MICRO, MissileItem.MissileTier.TIER0));

    // Tier 1
    public static final RegistrySupplier<Item> MISSILE_GENERIC = ITEMS.register("missile_generic",
            () -> new MissileItem(MissileItem.MissileFormFactor.V2, MissileItem.MissileTier.TIER1));
    public static final RegistrySupplier<Item> MISSILE_INCENDIARY = ITEMS.register("missile_incendiary",
            () -> new MissileItem(MissileItem.MissileFormFactor.V2, MissileItem.MissileTier.TIER1));
    public static final RegistrySupplier<Item> MISSILE_CLUSTER = ITEMS.register("missile_cluster",
            () -> new MissileItem(MissileItem.MissileFormFactor.V2, MissileItem.MissileTier.TIER1));
    public static final RegistrySupplier<Item> MISSILE_BUSTER = ITEMS.register("missile_buster",
            () -> new MissileItem(MissileItem.MissileFormFactor.V2, MissileItem.MissileTier.TIER1));
    public static final RegistrySupplier<Item> MISSILE_DECOY = ITEMS.register("missile_decoy",
            () -> new MissileItem(MissileItem.MissileFormFactor.V2, MissileItem.MissileTier.TIER1));

    public static final RegistrySupplier<Item> MISSILE_STEALTH = ITEMS.register("missile_stealth",
            () -> new MissileItem(MissileItem.MissileFormFactor.STEALTH, MissileItem.MissileTier.TIER1));

    // Tier 2
    public static final RegistrySupplier<Item> MISSILE_STRONG = ITEMS.register("missile_strong",
            () -> new MissileItem(MissileItem.MissileFormFactor.STRONG, MissileItem.MissileTier.TIER2));
    public static final RegistrySupplier<Item> MISSILE_INCENDIARY_STRONG = ITEMS.register("missile_incendiary_strong",
            () -> new MissileItem(MissileItem.MissileFormFactor.STRONG, MissileItem.MissileTier.TIER2));
    public static final RegistrySupplier<Item> MISSILE_CLUSTER_STRONG = ITEMS.register("missile_cluster_strong",
            () -> new MissileItem(MissileItem.MissileFormFactor.STRONG, MissileItem.MissileTier.TIER2));
    public static final RegistrySupplier<Item> MISSILE_BUSTER_STRONG = ITEMS.register("missile_buster_strong",
            () -> new MissileItem(MissileItem.MissileFormFactor.STRONG, MissileItem.MissileTier.TIER2));
    public static final RegistrySupplier<Item> MISSILE_EMP_STRONG = ITEMS.register("missile_emp_strong",
            () -> new MissileItem(MissileItem.MissileFormFactor.STRONG, MissileItem.MissileTier.TIER2));

    // Tier 3
    public static final RegistrySupplier<Item> MISSILE_BURST = ITEMS.register("missile_burst",
            () -> new MissileItem(MissileItem.MissileFormFactor.HUGE, MissileItem.MissileTier.TIER3));
    public static final RegistrySupplier<Item> MISSILE_INFERNO = ITEMS.register("missile_inferno",
            () -> new MissileItem(MissileItem.MissileFormFactor.HUGE, MissileItem.MissileTier.TIER3));
    public static final RegistrySupplier<Item> MISSILE_RAIN = ITEMS.register("missile_rain",
            () -> new MissileItem(MissileItem.MissileFormFactor.HUGE, MissileItem.MissileTier.TIER3));
    public static final RegistrySupplier<Item> MISSILE_DRILL = ITEMS.register("missile_drill",
            () -> new MissileItem(MissileItem.MissileFormFactor.HUGE, MissileItem.MissileTier.TIER3));
    public static final RegistrySupplier<Item> MISSILE_SHUTTLE = ITEMS.register("missile_shuttle",
            () -> new MissileItem(MissileItem.MissileFormFactor.OTHER, MissileItem.MissileTier.TIER3,
                    MissileItem.MissileFuel.KEROSENE_PEROXIDE));

    // Soyuz Launcher lander module (the rocket itself reuses ModBlocks.DECO_SOYUZ_ROCKET's item - see SoyuzLauncherBlockEntity.rocketItem())
    public static final RegistrySupplier<Item> MISSILE_SOYUZ_LANDER = ITEMS.register("missile_soyuz_lander",
            () -> new Item(new Item.Properties()));

    // Tier 4
    public static final RegistrySupplier<Item> MISSILE_NUCLEAR = ITEMS.register("missile_nuclear",
            () -> new MissileItem(MissileItem.MissileFormFactor.ATLAS, MissileItem.MissileTier.TIER4));
    public static final RegistrySupplier<Item> MISSILE_NUCLEAR_CLUSTER = ITEMS.register("missile_nuclear_cluster",
            () -> new MissileItem(MissileItem.MissileFormFactor.ATLAS, MissileItem.MissileTier.TIER4));
    public static final RegistrySupplier<Item> MISSILE_VOLCANO = ITEMS.register("missile_volcano",
            () -> new MissileItem(MissileItem.MissileFormFactor.ATLAS, MissileItem.MissileTier.TIER4));
    public static final RegistrySupplier<Item> MISSILE_DOOMSDAY = ITEMS.register("missile_doomsday",
            () -> new MissileItem(MissileItem.MissileFormFactor.ATLAS, MissileItem.MissileTier.TIER4));
    public static final RegistrySupplier<Item> MISSILE_DOOMSDAY_RUSTED = ITEMS.register("missile_doomsday_rusted",
            () -> new MissileItem(MissileItem.MissileFormFactor.ATLAS, MissileItem.MissileTier.TIER4).notLaunchable());

    public static final RegistrySupplier<Item> DESIGNATOR = ITEMS.register("designator",
        () -> new ItemDesignator(new Item.Properties()));
    public static final RegistrySupplier<Item> DESIGNATOR_RANGE = ITEMS.register("designator_range",
        () -> new ItemDesignatorRange(new Item.Properties()));
    public static final RegistrySupplier<Item> DESIGNATOR_MANUAL = ITEMS.register("designator_manual",
        () -> new ItemDesignatorManual(new Item.Properties()));

	// MULTIBLOCK DOORS

    public static final RegistrySupplier<Item> LARGE_VEHICLE_DOOR = ITEMS.register("large_vehicle_door",
        () -> new DoorBlockItem(ModBlocks.LARGE_VEHICLE_DOOR.get(), new Item.Properties()));

    public static final RegistrySupplier<Item> ROUND_AIRLOCK_DOOR = ITEMS.register("round_airlock_door",
        () -> new DoorBlockItem(ModBlocks.ROUND_AIRLOCK_DOOR.get(), new Item.Properties()));

    public static final RegistrySupplier<Item> TRANSITION_SEAL = ITEMS.register("transition_seal",
        () -> new DoorBlockItem(ModBlocks.TRANSITION_SEAL.get(), new Item.Properties()));

    public static final RegistrySupplier<Item> SILO_HATCH = ITEMS.register("silo_hatch",
        () -> new DoorBlockItem(ModBlocks.SILO_HATCH.get(), new Item.Properties()));

    public static final RegistrySupplier<Item> SILO_HATCH_LARGE = ITEMS.register("silo_hatch_large",
        () -> new DoorBlockItem(ModBlocks.SILO_HATCH_LARGE.get(), new Item.Properties()));

    public static final RegistrySupplier<Item> QE_CONTAINMENT = ITEMS.register("qe_containment_door",
        () -> new DoorBlockItem(ModBlocks.QE_CONTAINMENT.get(), new Item.Properties()));

    public static final RegistrySupplier<Item> WATER_DOOR = ITEMS.register("water_door",
        () -> new DoorBlockItem(ModBlocks.WATER_DOOR.get(), new Item.Properties()));

    public static final RegistrySupplier<Item> FIRE_DOOR = ITEMS.register("fire_door",
        () -> new DoorBlockItem(ModBlocks.FIRE_DOOR.get(), new Item.Properties()));

    public static final RegistrySupplier<Item> SLIDE_DOOR = ITEMS.register("sliding_blast_door",
        () -> new DoorBlockItem(ModBlocks.SLIDE_DOOR.get(), new Item.Properties()));
        
    public static final RegistrySupplier<Item> SLIDING_SEAL_DOOR = ITEMS.register("sliding_seal_door",
        () -> new DoorBlockItem(ModBlocks.SLIDING_SEAL_DOOR.get(), new Item.Properties()));

    public static final RegistrySupplier<Item> SECURE_ACCESS_DOOR = ITEMS.register("secure_access_door",
        () -> new DoorBlockItem(ModBlocks.SECURE_ACCESS_DOOR.get(), new Item.Properties()));

    public static final RegistrySupplier<Item> QE_SLIDING = ITEMS.register("qe_sliding_door",
        () -> new DoorBlockItem(ModBlocks.QE_SLIDING.get(), new Item.Properties()));

    public static final RegistrySupplier<Item> VAULT_DOOR = ITEMS.register("vault_door",
        () -> new DoorBlockItem(ModBlocks.VAULT_DOOR.get(), new Item.Properties()));



    public static final RegistrySupplier<Item> STAMP_STONE_FLAT = ITEMS.register("stamp_stone_flat",
            () -> new ItemStamp(new Item.Properties(), 32));
    public static final RegistrySupplier<Item> STAMP_STONE_PLATE = ITEMS.register("stamp_stone_plate",
            () -> new ItemStamp(new Item.Properties(), 32));
    public static final RegistrySupplier<Item> STAMP_STONE_WIRE = ITEMS.register("stamp_stone_wire",
            () -> new ItemStamp(new Item.Properties(), 32));
    public static final RegistrySupplier<Item> STAMP_STONE_CIRCUIT = ITEMS.register("stamp_stone_circuit",
            () -> new ItemStamp(new Item.Properties(), 32));


    public static final RegistrySupplier<Item> BLADE_TEST = ITEMS.register("blade_test",
            () -> new ItemBlades(new Item.Properties()));

    public static final RegistrySupplier<Item> BLADE_STEEL = ITEMS.register("blade_steel",
            () -> new ItemBlades(new Item.Properties(), 200));

    public static final RegistrySupplier<Item> BLADE_TITANIUM = ITEMS.register("blade_titanium",
            () -> new ItemBlades(new Item.Properties(), 350));

    public static final RegistrySupplier<Item> BLADE_ALLOY = ITEMS.register("blade_alloy",
            () -> new ItemBlades(new Item.Properties(), 700));

    // Железные штампы (48 использований)
    public static final RegistrySupplier<Item> STAMP_IRON_FLAT = ITEMS.register("stamp_iron_flat",
            () -> new ItemStamp(new Item.Properties(), 48));
    public static final RegistrySupplier<Item> STAMP_IRON_PLATE = ITEMS.register("stamp_iron_plate",
            () -> new ItemStamp(new Item.Properties(), 48));
    public static final RegistrySupplier<Item> STAMP_IRON_WIRE = ITEMS.register("stamp_iron_wire",
            () -> new ItemStamp(new Item.Properties(), 48));
    public static final RegistrySupplier<Item> STAMP_IRON_CIRCUIT = ITEMS.register("stamp_iron_circuit",
            () -> new ItemStamp(new Item.Properties(), 48));
    public static final RegistrySupplier<Item> STAMP_IRON_9 = ITEMS.register("stamp_iron_9",
            () -> new ItemStamp(new Item.Properties(), 48));
    public static final RegistrySupplier<Item> STAMP_IRON_44 = ITEMS.register("stamp_iron_44",
            () -> new ItemStamp(new Item.Properties(), 48));
    public static final RegistrySupplier<Item> STAMP_IRON_50 = ITEMS.register("stamp_iron_50",
            () -> new ItemStamp(new Item.Properties(), 48));
    public static final RegistrySupplier<Item> STAMP_IRON_357 = ITEMS.register("stamp_iron_357",
            () -> new ItemStamp(new Item.Properties(), 48));

    // Стальные штампы (64 использования)
    public static final RegistrySupplier<Item> STAMP_STEEL_FLAT = ITEMS.register("stamp_steel_flat",
            () -> new ItemStamp(new Item.Properties(), 64));
    public static final RegistrySupplier<Item> STAMP_STEEL_PLATE = ITEMS.register("stamp_steel_plate",
            () -> new ItemStamp(new Item.Properties(), 64));
    public static final RegistrySupplier<Item> STAMP_STEEL_WIRE = ITEMS.register("stamp_steel_wire",
            () -> new ItemStamp(new Item.Properties(), 64));
    public static final RegistrySupplier<Item> STAMP_STEEL_CIRCUIT = ITEMS.register("stamp_steel_circuit",
            () -> new ItemStamp(new Item.Properties(), 64));

    // Титановые штампы (80 использований)
    public static final RegistrySupplier<Item> STAMP_TITANIUM_FLAT = ITEMS.register("stamp_titanium_flat",
            () -> new ItemStamp(new Item.Properties(), 80));
    public static final RegistrySupplier<Item> STAMP_TITANIUM_PLATE = ITEMS.register("stamp_titanium_plate",
            () -> new ItemStamp(new Item.Properties(), 80));
    public static final RegistrySupplier<Item> STAMP_TITANIUM_WIRE = ITEMS.register("stamp_titanium_wire",
            () -> new ItemStamp(new Item.Properties(), 80));
    public static final RegistrySupplier<Item> STAMP_TITANIUM_CIRCUIT = ITEMS.register("stamp_titanium_circuit",
            () -> new ItemStamp(new Item.Properties(), 80));

    // Обсидиановые штампы (96 использований)
    public static final RegistrySupplier<Item> STAMP_OBSIDIAN_FLAT = ITEMS.register("stamp_obsidian_flat",
            () -> new ItemStamp(new Item.Properties(), 96));
    public static final RegistrySupplier<Item> STAMP_OBSIDIAN_PLATE = ITEMS.register("stamp_obsidian_plate",
            () -> new ItemStamp(new Item.Properties(), 96));
    public static final RegistrySupplier<Item> STAMP_OBSIDIAN_WIRE = ITEMS.register("stamp_obsidian_wire",
            () -> new ItemStamp(new Item.Properties(), 96));
    public static final RegistrySupplier<Item> STAMP_OBSIDIAN_CIRCUIT = ITEMS.register("stamp_obsidian_circuit",
            () -> new ItemStamp(new Item.Properties(), 96));

    // Desh штампы (бесконечная прочность)
    public static final RegistrySupplier<Item> STAMP_DESH_FLAT = ITEMS.register("stamp_desh_flat",
            () -> new ItemStamp(new Item.Properties()));
    public static final RegistrySupplier<Item> STAMP_DESH_PLATE = ITEMS.register("stamp_desh_plate",
            () -> new ItemStamp(new Item.Properties()));
    public static final RegistrySupplier<Item> STAMP_DESH_WIRE = ITEMS.register("stamp_desh_wire",
            () -> new ItemStamp(new Item.Properties()));
    public static final RegistrySupplier<Item> STAMP_DESH_CIRCUIT = ITEMS.register("stamp_desh_circuit",
            () -> new ItemStamp(new Item.Properties()));
    public static final RegistrySupplier<Item> STAMP_DESH_9 = ITEMS.register("stamp_desh_9",
            () -> new ItemStamp(new Item.Properties()));
    public static final RegistrySupplier<Item> STAMP_DESH_44 = ITEMS.register("stamp_desh_44",
            () -> new ItemStamp(new Item.Properties()));
    public static final RegistrySupplier<Item> STAMP_DESH_50 = ITEMS.register("stamp_desh_50",
            () -> new ItemStamp(new Item.Properties()));
    public static final RegistrySupplier<Item> STAMP_DESH_357 = ITEMS.register("stamp_desh_357",
            () -> new ItemStamp(new Item.Properties()));


    //батарейки

    public static final RegistrySupplier<Item> BATTERY_SCHRABIDIUM = ITEMS.register("battery_schrabidium",
            () -> new ModBatteryItem(
                    new Item.Properties(),
                    1000000,
                    5000,
                    5000
            ));

    // ========== КАРТОФЕЛЬНАЯ И БАЗОВЫЕ ==========
    public static final RegistrySupplier<Item> BATTERY_POTATO = ITEMS.register("battery_potato",
            () -> new ModBatteryItem(
                    new Item.Properties(),
                    1_000,
                    100,
                    100
            ));

    public static final RegistrySupplier<Item> BATTERY = ITEMS.register("battery",
            () -> new ModBatteryItem(
                    new Item.Properties(),
                    5000,
                    100,
                    100
            ));

    // ========== КРАСНЫЕ БАТАРЕЙКИ (RED CELL) ==========
    public static final RegistrySupplier<Item> BATTERY_RED_CELL = ITEMS.register("battery_red_cell",
            () -> new ModBatteryItem(
                    new Item.Properties(),
                    15000,
                    100,
                    100
            ));

    public static final RegistrySupplier<Item> BATTERY_RED_CELL_6 = ITEMS.register("battery_red_cell_6",
            () -> new ModBatteryItem(
                    new Item.Properties(),
                    90000,
                    100,
                    100
            ));

    public static final RegistrySupplier<Item> BATTERY_RED_CELL_24 = ITEMS.register("battery_red_cell_24",
            () -> new ModBatteryItem(
                    new Item.Properties(),
                    240000,
                    100,
                    100
            ));

    // ========== ПРОДВИНУТЫЕ БАТАРЕЙКИ (ADVANCED) ==========
    public static final RegistrySupplier<Item> BATTERY_ADVANCED = ITEMS.register("battery_advanced",
            () -> new ModBatteryItem(
                    new Item.Properties(),
                    20000,
                    500,
                    500
            ));

    public static final RegistrySupplier<Item> BATTERY_ADVANCED_CELL = ITEMS.register("battery_advanced_cell",
            () -> new ModBatteryItem(
                    new Item.Properties(),
                    60000,
                    500,
                    500
            ));

    public static final RegistrySupplier<Item> BATTERY_ADVANCED_CELL_4 = ITEMS.register("battery_advanced_cell_4",
            () -> new ModBatteryItem(
                    new Item.Properties(),
                    240000,
                    500,
                    500
            ));

    public static final RegistrySupplier<Item> BATTERY_ADVANCED_CELL_12 = ITEMS.register("battery_advanced_cell_12",
            () -> new ModBatteryItem(
                    new Item.Properties(),
                    720000,
                    500,
                    500
            ));

    // ========== ЛИТИЕВЫЕ БАТАРЕЙКИ (LITHIUM) ==========
    public static final RegistrySupplier<Item> BATTERY_LITHIUM = ITEMS.register("battery_lithium",
            () -> new ModBatteryItem(
                    new Item.Properties(),
                    250000,
                    1000,
                    1000
            ));

    public static final RegistrySupplier<Item> BATTERY_LITHIUM_CELL = ITEMS.register("battery_lithium_cell",
            () -> new ModBatteryItem(
                    new Item.Properties(),
                    750000,
                    1000,
                    1000
            ));

    public static final RegistrySupplier<Item> BATTERY_LITHIUM_CELL_3 = ITEMS.register("battery_lithium_cell_3",
            () -> new ModBatteryItem(
                    new Item.Properties(),
                    2250000,
                    1000,
                    1000
            ));

    public static final RegistrySupplier<Item> BATTERY_LITHIUM_CELL_6 = ITEMS.register("battery_lithium_cell_6",
            () -> new ModBatteryItem(
                    new Item.Properties(),
                    4500000,
                    1000,
                    1000
            ));

// ========== ШРАБИДИЕВЫЕ БАТАРЕЙКИ (SCHRABIDIUM) - уже есть ==========

    public static final RegistrySupplier<Item> BATTERY_SCHRABIDIUM_CELL = ITEMS.register("battery_schrabidium_cell",
            () -> new ModBatteryItem(
                    new Item.Properties(),
                    3000000,
                    5000,
                    5000
            ));

    public static final RegistrySupplier<Item> BATTERY_SCHRABIDIUM_CELL_2 = ITEMS.register("battery_schrabidium_cell_2",
            () -> new ModBatteryItem(
                    new Item.Properties(),
                    6000000,
                    5000,
                    5000
            ));

    public static final RegistrySupplier<Item> BATTERY_SCHRABIDIUM_CELL_4 = ITEMS.register("battery_schrabidium_cell_4",
            () -> new ModBatteryItem(
                    new Item.Properties(),
                    12000000,
                    5000,
                    5000
            ));

    // ========== ИСКРОВЫЕ БАТАРЕЙКИ (SPARK) - ЭКСТРЕМАЛЬНЫЕ ==========
    public static final RegistrySupplier<Item> BATTERY_SPARK = ITEMS.register("battery_spark",
            () -> new ModBatteryItem(
                    new Item.Properties(),
                    100000000,
                    2000000,
                    2000000
            ));

    public static final RegistrySupplier<Item> BATTERY_TRIXITE = ITEMS.register("battery_trixite",
            () -> new ModBatteryItem(
                    new Item.Properties(),
                    5000000,
                    40000,
                    200000
            ));

    public static final RegistrySupplier<Item> BATTERY_SPARK_CELL_6 = ITEMS.register("battery_spark_cell_6",
            () -> new ModBatteryItem(
                    new Item.Properties(),
                    600_000_000L,
                    2000000,
                    2000000
            ));

    public static final RegistrySupplier<Item> BATTERY_SPARK_CELL_25 = ITEMS.register("battery_spark_cell_25",
            () -> new ModBatteryItem(
                    new Item.Properties(),
                    2_500_000_000L,
                    2000000,
                    2000000
            ));

    public static final RegistrySupplier<Item> BATTERY_SPARK_CELL_100 = ITEMS.register("battery_spark_cell_100",
            () -> new ModBatteryItem(
                    new Item.Properties(),
                    10_000_000_000L,
                    20000000,
                    2000000
            ));

    public static final RegistrySupplier<Item> BATTERY_SPARK_CELL_1000 = ITEMS.register("battery_spark_cell_1000",
            () -> new ModBatteryItem(
                    new Item.Properties(),
                    100_000_000_000L,
                    20000000,
                    20000000
            ));

    public static final RegistrySupplier<Item> BATTERY_SPARK_CELL_2500 = ITEMS.register("battery_spark_cell_2500",
            () -> new ModBatteryItem(
                    new Item.Properties(),
                    250_000_000_000L,
                    20000000,
                    20000000
            ));

    public static final RegistrySupplier<Item> BATTERY_SPARK_CELL_10000 = ITEMS.register("battery_spark_cell_10000",
            () -> new ModBatteryItem(
                    new Item.Properties(),
                    1_000_000_000_000L,
                    200000000,
                    200000000
            ));

    public static final RegistrySupplier<Item> BATTERY_SPARK_CELL_POWER = ITEMS.register("battery_spark_cell_power",
            () -> new ModBatteryItem(
                    new Item.Properties(),
                    100_000_000_000_000L,
                    200000000,
                    200000000
            ));


    public static final RegistrySupplier<Item> AIRSTRIKE_TEST = ITEMS.register("airstrike_test",
            () -> new AirstrikeItem(new Item.Properties(), AirstrikeType.NORMAL));
    public static final RegistrySupplier<Item> AIRSTRIKE_AGENT= ITEMS.register("airstrike_agent",
            () -> new AirstrikeItem(new Item.Properties(), AirstrikeType.AGENT));
    public static final RegistrySupplier<Item> AIRSTRIKE_HEAVY = ITEMS.register("airstrike_heavy",
            () -> new AirstrikeItem(new Item.Properties(), AirstrikeType.HEAVY));
    public static final RegistrySupplier<Item> AIRSTRIKE_NUKE = ITEMS.register("airstrike_nuke",
            () -> new AirstrikeItem(new Item.Properties(), AirstrikeType.NUKE));
    public static final RegistrySupplier<Item> WIRE_RED_COPPER = ITEMS.register("wire_red_copper",
            () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> WIRE_ADVANCED_ALLOY = ITEMS.register("wire_advanced_alloy",
            () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> WIRE_ALUMINIUM = ITEMS.register("wire_aluminium",
            () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> WIRE_COPPER = ITEMS.register("wire_copper",
            () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> WIRE_CARBON = ITEMS.register("wire_carbon",
            () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> WIRE_FINE = ITEMS.register("wire_fine",
            () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> WIRE_GOLD = ITEMS.register("wire_gold",
            () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> WIRE_MAGNETIZED_TUNGSTEN = ITEMS.register("wire_magnetized_tungsten",
            () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> WIRE_SCHRABIDIUM = ITEMS.register("wire_schrabidium",
            () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> WIRE_TUNGSTEN = ITEMS.register("wire_tungsten",
            () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> WIRE_IRON = ITEMS.register("wire_iron",
            () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> WIRE_STEEL = ITEMS.register("wire_steel",
            () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> WIRE_TITANIUM = ITEMS.register("wire_titanium",
            () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> WIRE_SATURNITE = ITEMS.register("wire_saturnite",
            () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> WIRE_COMBINE_STEEL = ITEMS.register("wire_combine_steel",
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> SAT_HEAD_LASER = ITEMS.register("sat_head_laser",
            () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> SAT_BASE = ITEMS.register("sat_base",
            () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> SAT_LASER = ITEMS.register("sat_laser",
            () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> SAT_HEAD_RADAR = ITEMS.register("sat_head_radar",
            () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> SAT_RADAR = ITEMS.register("sat_radar",
            () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> SAT_HEAD_MAPPER = ITEMS.register("sat_head_mapper",
            () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> SAT_MAPPER = ITEMS.register("sat_mapper",
            () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> SAT_HEAD_RESONATOR = ITEMS.register("sat_head_resonator",
            () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> SAT_RESONATOR = ITEMS.register("sat_resonator",
            () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> THRUSTER_LARGE         = ITEMS.register("thruster_large",         () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> FUEL_TANK_LARGE        = ITEMS.register("fuel_tank_large",        () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> WARHEAD_NUCLEAR        = ITEMS.register("warhead_nuclear",        () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> MISSILE_ASSEMBLY           = ITEMS.register("missile_assembly",           () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> INGOT_TUNGSTEN_CARBIDE     = ITEMS.register("ingot_tungsten_carbide",     () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> INGOT_HIGHSPEED_STEEL      = ITEMS.register("ingot_highspeed_steel",      () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> NEUTRON_REFLECTOR          = ITEMS.register("neutron_reflector",          () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> WARHEAD_GENERIC_SMALL      = ITEMS.register("warhead_generic_small",      () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> WARHEAD_CLUSTER_LARGE      = ITEMS.register("warhead_cluster_large",      () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> WARHEAD_INCENDIARY_MEDIUM  = ITEMS.register("warhead_incendiary_medium",  () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> WARHEAD_BUSTER_SMALL       = ITEMS.register("warhead_buster_small",       () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> THRUSTER_SMALL          = ITEMS.register("thruster_small",          () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> FUEL_TANK_SMALL         = ITEMS.register("fuel_tank_small",         () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> WARHEAD_CLUSTER_SMALL      = ITEMS.register("warhead_cluster_small",      () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> WARHEAD_INCENDIARY_SMALL   = ITEMS.register("warhead_incendiary_small",   () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> LOW_DENSITY_ELEMENT     = ITEMS.register("low_density_element",     () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> THRUSTER_MEDIUM         = ITEMS.register("thruster_medium",         () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> FUEL_TANK_MEDIUM        = ITEMS.register("fuel_tank_medium",        () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> WARHEAD_GENERIC_MEDIUM  = ITEMS.register("warhead_generic_medium",  () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> WARHEAD_GENERIC_LARGE   = ITEMS.register("warhead_generic_large",   () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> WARHEAD_BUSTER_LARGE    = ITEMS.register("warhead_buster_large",    () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> WARHEAD_MIRV            = ITEMS.register("warhead_mirv",            () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> WARHEAD_VOLCANO         = ITEMS.register("warhead_volcano",         () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> WARHEAD_BUSTER_MEDIUM   = ITEMS.register("warhead_buster_medium",   () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> WARHEAD_CLUSTER_MEDIUM  = ITEMS.register("warhead_cluster_medium",  () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> SCREWDRIVER = ITEMS.register("screwdriver",
            () -> new ScrewdriverItem(new Item.Properties().stacksTo(1)));

	// Медленный источник (500 mB/t)
	public static final RegistrySupplier<Item> INFINITE_WATER_500 = ITEMS.register("inf_water",
					() -> new InfiniteFluidItem(new Item.Properties().stacksTo(1), net.minecraft.world.level.material.Fluids.WATER, 500));

	// Быстрый источник (5000 mB/t)
	public static final RegistrySupplier<Item> INFINITE_WATER_5000 = ITEMS.register("inf_water_mk2",
            () -> new InfiniteFluidItem(new Item.Properties().stacksTo(1), net.minecraft.world.level.material.Fluids.WATER, 5000));

    // Fluid Barrel - 16,000 mB capacity portable fluid container
    public static final RegistrySupplier<Item> FLUID_BARREL = ITEMS.register("fluid_barrel",
            () -> new FluidBarrelItem(new Item.Properties()));

    // Universal infinite fluid source (any fluid type, 1B mB/t - like 1.7.10 fluid_barrel_infinite)
    public static final RegistrySupplier<Item> FLUID_BARREL_INFINITE = ITEMS.register("fluid_barrel_infinite",
            () -> new InfiniteFluidItem(new Item.Properties().stacksTo(1), 1_000_000_000));


    // Universal fluid identifier - two fluid slots, Shift+RMB opens selection GUI
    public static final RegistrySupplier<Item> FLUID_IDENTIFIER = ITEMS.register("fluid_identifier",
            () -> new FluidIdentifierItem(new Item.Properties().stacksTo(1)));

    // Mineral Pipes - individual pipe items per mineral, all using pipe.png with color tinting
    public static final RegistrySupplier<Item> PIPE_IRON = ITEMS.register("pipe_iron",
            () -> new MineralPipeItem(new Item.Properties(), 0xD8D8D8));
    public static final RegistrySupplier<Item> PIPE_COPPER = ITEMS.register("pipe_copper",
            () -> new MineralPipeItem(new Item.Properties(), 0xE77C56));
    public static final RegistrySupplier<Item> PIPE_GOLD = ITEMS.register("pipe_gold",
            () -> new MineralPipeItem(new Item.Properties(), 0xFCEE4B));
    public static final RegistrySupplier<Item> PIPE_LEAD = ITEMS.register("pipe_lead",
            () -> new MineralPipeItem(new Item.Properties(), 0x414166));
    public static final RegistrySupplier<Item> PIPE_STEEL = ITEMS.register("pipe_steel",
            () -> new MineralPipeItem(new Item.Properties(), 0x767676));
    public static final RegistrySupplier<Item> PIPE_TUNGSTEN = ITEMS.register("pipe_tungsten",
            () -> new MineralPipeItem(new Item.Properties(), 0x3D3D3D));
    public static final RegistrySupplier<Item> PIPE_TITANIUM = ITEMS.register("pipe_titanium",
            () -> new MineralPipeItem(new Item.Properties(), 0x8DC5E2));
    public static final RegistrySupplier<Item> PIPE_ALUMINUM = ITEMS.register("pipe_aluminum",
            () -> new MineralPipeItem(new Item.Properties(), 0xC5C5DE));
    public static final RegistrySupplier<Item> PIPE_DURA_STEEL = ITEMS.register("pipe_dura_steel",
            () -> new MineralPipeItem(new Item.Properties(), 0x82A59C));

    // Fluid Duct - pipe per fluid type, overlay tinted with fluid color (like fluid barrel)
    public static final RegistrySupplier<Item> FLUID_DUCT = ITEMS.register("fluid_duct",
            () -> new FluidDuctItem(new Item.Properties(), ModBlocks.FLUID_DUCT,
                    "item.hbm_m.fluid_duct", "item.hbm_m.fluid_duct.empty"));
    public static final RegistrySupplier<Item> FLUID_DUCT_COLORED = ITEMS.register("fluid_duct_colored",
            () -> new FluidDuctItem(new Item.Properties(), ModBlocks.FLUID_DUCT_COLORED,
                    "item.hbm_m.fluid_duct_colored", "item.hbm_m.fluid_duct_colored.empty"));
    public static final RegistrySupplier<Item> FLUID_DUCT_SILVER = ITEMS.register("fluid_duct_silver",
            () -> new FluidDuctItem(new Item.Properties(), ModBlocks.FLUID_DUCT_SILVER,
                    "item.hbm_m.fluid_duct_silver", "item.hbm_m.fluid_duct_silver.empty"));

    public static final RegistrySupplier<Item> FLUID_VALVE = ITEMS.register("fluid_valve",
            () -> new BlockItem(ModBlocks.FLUID_VALVE.get(), new Item.Properties()));
    public static final RegistrySupplier<Item> FLUID_PUMP = ITEMS.register("fluid_pump",
            () -> new BlockItem(ModBlocks.FLUID_PUMP.get(), new Item.Properties()));
    public static final RegistrySupplier<Item> FLUID_EXHAUST = ITEMS.register("fluid_exhaust",
            () -> new BlockItem(ModBlocks.FLUID_EXHAUST.get(), new Item.Properties()));

    //=============================== ВЁДРА ДЛЯ ЖИДКОСТЕЙ ===============================//

//    public static final RegistrySupplier<Item> CRUDE_OIL_BUCKET = ITEMS.register("bucket_crude_oil",
//            () -> new BucketItem(
//                    () -> ModFluids.CRUDE_OIL.source.get(),
//                    new Item.Properties()
//                            .craftRemainder(Items.BUCKET)
//                            .stacksTo(1)));


    // ─── RBMK Items ──────────────────────────────────────────────────────────

    public static final RegistrySupplier<Item> RBMK_LID = ITEMS.register("rbmk_lid",
            () -> new RBMKLidItem(1, new Item.Properties()));

    public static final RegistrySupplier<Item> RBMK_LID_GLASS = ITEMS.register("rbmk_lid_glass",
            () -> new RBMKLidItem(2, new Item.Properties()));

    // Pellets
    public static final RegistrySupplier<Item> RBMK_PELLET_LEU235 = ITEMS.register("rbmk_pellet_leu235",
            () -> new RBMKPelletItem(new Item.Properties())
                    .setFullName("Low-Enriched Uranium-235").setYield(250_000).setReactivity(80)
                    .setHeat(1.8).setMeltingPoint(1000).setTint(0x4a6a1a));

    public static final RegistrySupplier<Item> RBMK_PELLET_HEU235 = ITEMS.register("rbmk_pellet_heu235",
            () -> new RBMKPelletItem(new Item.Properties())
                    .setFullName("High-Enriched Uranium-235").setYield(125_000).setReactivity(200)
                    .setHeat(2.0).setMeltingPoint(1000).setTint(0x6e901e));

    public static final RegistrySupplier<Item> RBMK_PELLET_LEP = ITEMS.register("rbmk_pellet_lep",
            () -> new RBMKPelletItem(new Item.Properties())
                    .setFullName("Low-Enriched Plutonium").setYield(225_000).setReactivity(100)
                    .setHeat(2.2).setMeltingPoint(975).setTint(0x7c2a12)
                    .setNeutronTypes(NType.FAST, NType.FAST));

    public static final RegistrySupplier<Item> RBMK_PELLET_HEP = ITEMS.register("rbmk_pellet_hep239",
            () -> new RBMKPelletItem(new Item.Properties())
                    .setFullName("High-Enriched Plutonium-239").setYield(100_000).setReactivity(225)
                    .setHeat(2.5).setMeltingPoint(975).setTint(0xa03318)
                    .setNeutronTypes(NType.FAST, NType.FAST));

    public static final RegistrySupplier<Item> RBMK_PELLET_MOX = ITEMS.register("rbmk_pellet_mox",
            () -> new RBMKPelletItem(new Item.Properties())
                    .setFullName("Mixed Oxide Fuel").setYield(200_000).setReactivity(130)
                    .setHeat(2.1).setMeltingPoint(1000).setTint(0x604018)
                    .setNeutronTypes(NType.ANY, NType.FAST));

    // Fuel Rods (assembled from pellets)
    public static final RegistrySupplier<Item> RBMK_FUEL_LEU235 = ITEMS.register("rbmk_fuel_leu235",
            () -> new RBMKRodItem("Low-Enriched Uranium-235 Rod", new Item.Properties())
                    .setYield(250_000).setStats(80).setHeat(1.8).setMeltingPoint(1000)
                    .setDiffusion(0.02).setTint(0x4a6a1a));

    public static final RegistrySupplier<Item> RBMK_FUEL_HEU235 = ITEMS.register("rbmk_fuel_heu235",
            () -> new RBMKRodItem("High-Enriched Uranium-235 Rod", new Item.Properties())
                    .setYield(125_000).setStats(200).setHeat(2.0).setMeltingPoint(1100)
                    .setDiffusion(0.025).setTint(0x6e901e));

    public static final RegistrySupplier<Item> RBMK_FUEL_LEP = ITEMS.register("rbmk_fuel_lep",
            () -> new RBMKRodItem("Low-Enriched Plutonium Rod", new Item.Properties())
                    .setYield(225_000).setStats(100).setHeat(2.2).setMeltingPoint(975)
                    .setDiffusion(0.02).setTint(0x7c2a12).setNeutronTypes(NType.FAST, NType.FAST));

    public static final RegistrySupplier<Item> RBMK_FUEL_HEP = ITEMS.register("rbmk_fuel_hep239",
            () -> new RBMKRodItem("High-Enriched Plutonium-239 Rod", new Item.Properties())
                    .setYield(100_000).setStats(225).setHeat(2.5).setMeltingPoint(975)
                    .setDiffusion(0.025).setTint(0xa03318).setNeutronTypes(NType.FAST, NType.FAST));

    public static final RegistrySupplier<Item> RBMK_FUEL_MOX = ITEMS.register("rbmk_fuel_mox",
            () -> new RBMKRodItem("Mixed Oxide Fuel Rod", new Item.Properties())
                    .setYield(200_000).setStats(130).setHeat(2.1).setMeltingPoint(1000)
                    .setDiffusion(0.02).setTint(0x604018).setNeutronTypes(NType.ANY, NType.FAST));

    public static final RegistrySupplier<Item> RBMK_FUEL_EMPTY = ITEMS.register("rbmk_fuel_empty",
            () -> new Item(new Item.Properties()));

    // ══════════════════════════════════════════════════════════════════════
    // DEV: importierte fehlende Items aus dem Original-HBM (zur Sichtung)
    // ══════════════════════════════════════════════════════════════════════
    public static final RegistrySupplier<Item> ACETYLENE_TORCH = ITEMS.register("acetylene_torch", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> AJR_LEGS = ITEMS.register("ajr_legs", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> AJR_PLATE = ITEMS.register("ajr_plate", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> AJRO_LEGS = ITEMS.register("ajro_legs", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> AJRO_PLATE = ITEMS.register("ajro_plate", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> ALLOY_LEGS = ITEMS.register("alloy_legs", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> ALLOY_PLATE = ITEMS.register("alloy_plate", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> AMMO_ARTY = ITEMS.register("ammo_arty", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> AMMO_ARTY_CARGO = ITEMS.register("ammo_arty_cargo", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> AMMO_ARTY_CHLORINE = ITEMS.register("ammo_arty_chlorine", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> AMMO_ARTY_CLASSIC = ITEMS.register("ammo_arty_classic", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> AMMO_ARTY_HE = ITEMS.register("ammo_arty_he", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> AMMO_ARTY_MINI_NUKE = ITEMS.register("ammo_arty_mini_nuke", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> AMMO_ARTY_MINI_NUKE_MULTI = ITEMS.register("ammo_arty_mini_nuke_multi", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> AMMO_ARTY_MUSTARD_GAS = ITEMS.register("ammo_arty_mustard_gas", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> AMMO_ARTY_NUKE = ITEMS.register("ammo_arty_nuke", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> AMMO_ARTY_PHOSGENE = ITEMS.register("ammo_arty_phosgene", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> AMMO_ARTY_PHOSPHORUS = ITEMS.register("ammo_arty_phosphorus", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> AMMO_ARTY_PHOSPHORUS_MULTI = ITEMS.register("ammo_arty_phosphorus_multi", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> AMMO_BAG = ITEMS.register("ammo_bag", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> AMMO_BAG_INFINITE = ITEMS.register("ammo_bag_infinite", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> AMMO_CONTAINER = ITEMS.register("ammo_container", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> AMMO_DGK = ITEMS.register("ammo_dgk", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> AMMO_FIREEXT = ITEMS.register("ammo_fireext", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> AMMO_FIREEXT_FOAM = ITEMS.register("ammo_fireext_foam", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> AMMO_FIREEXT_SAND = ITEMS.register("ammo_fireext_sand", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> AMMO_SHELL = ITEMS.register("ammo_shell", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> AMMO_SHELL_APFSDS_DU = ITEMS.register("ammo_shell_apfsds_du", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> AMMO_SHELL_APFSDS_T = ITEMS.register("ammo_shell_apfsds_t", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> AMMO_SHELL_EXPLOSIVE = ITEMS.register("ammo_shell_explosive", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> AMMO_SHELL_W9 = ITEMS.register("ammo_shell_w9", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> AMS_CATALYST_ALUMINIUM = ITEMS.register("ams_catalyst_aluminium", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> AMS_CATALYST_BERYLLIUM = ITEMS.register("ams_catalyst_beryllium", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> AMS_CATALYST_BLANK = ITEMS.register("ams_catalyst_blank", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> AMS_CATALYST_CAESIUM = ITEMS.register("ams_catalyst_caesium", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> AMS_CATALYST_CERIUM = ITEMS.register("ams_catalyst_cerium", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> AMS_CATALYST_COBALT = ITEMS.register("ams_catalyst_cobalt", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> AMS_CATALYST_COPPER = ITEMS.register("ams_catalyst_copper", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> AMS_CATALYST_DINEUTRONIUM = ITEMS.register("ams_catalyst_dineutronium", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> AMS_CATALYST_EUPHEMIUM = ITEMS.register("ams_catalyst_euphemium", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> AMS_CATALYST_IRON = ITEMS.register("ams_catalyst_iron", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> AMS_CATALYST_LITHIUM = ITEMS.register("ams_catalyst_lithium", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> AMS_CATALYST_NIOBIUM = ITEMS.register("ams_catalyst_niobium", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> AMS_CATALYST_SCHRABIDIUM = ITEMS.register("ams_catalyst_schrabidium", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> AMS_CATALYST_STRONTIUM = ITEMS.register("ams_catalyst_strontium", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> AMS_CATALYST_THORIUM = ITEMS.register("ams_catalyst_thorium", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> AMS_CATALYST_TUNGSTEN = ITEMS.register("ams_catalyst_tungsten", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> AMS_CORE_EYEOFHARMONY = ITEMS.register("ams_core_eyeofharmony", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> AMS_CORE_SING = ITEMS.register("ams_core_sing", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> AMS_CORE_THINGY = ITEMS.register("ams_core_thingy", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> AMS_CORE_WORMHOLE = ITEMS.register("ams_core_wormhole", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> AMS_LENS = ITEMS.register("ams_lens", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> ANALYSIS_TOOL = ITEMS.register("analysis_tool", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> ANALYZER = ITEMS.register("analyzer", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> ANCHOR_REMOTE = ITEMS.register("anchor_remote", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> APPLE_EUPHEMIUM = ITEMS.register("apple_euphemium", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> APPLE_LEAD = ITEMS.register("apple_lead", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> APPLE_SCHRABIDIUM = ITEMS.register("apple_schrabidium", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> ARC_ELECTRODE = ITEMS.register("arc_electrode", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> ARMOR_POLISH = ITEMS.register("armor_polish", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> ASBESTOS_LEGS = ITEMS.register("asbestos_legs", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> ASBESTOS_PLATE = ITEMS.register("asbestos_plate", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> ASHGLASSES = ITEMS.register("ashglasses", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> ASSEMBLY_NUKE = ITEMS.register("assembly_nuke", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> ATTACHMENT_MASK = ITEMS.register("attachment_mask", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> ATTACHMENT_MASK_MONO = ITEMS.register("attachment_mask_mono", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> AUSTRALIUM_III = ITEMS.register("australium_iii", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BACK_TESLA = ITEMS.register("back_tesla", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BALEFIRE_AND_HAM = ITEMS.register("balefire_and_ham", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BALEFIRE_AND_STEEL = ITEMS.register("balefire_and_steel", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BALEFIRE_SCRAMBLED = ITEMS.register("balefire_scrambled", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BALL_DYNAMITE = ITEMS.register("ball_dynamite", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BALL_FIRECLAY = ITEMS.register("ball_fireclay", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BALL_RESIN = ITEMS.register("ball_resin", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BALL_TATB = ITEMS.register("ball_tatb", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BALLISTIC_GAUNTLET = ITEMS.register("ballistic_gauntlet", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BALLISTITE = ITEMS.register("ballistite", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BANDAID = ITEMS.register("bandaid", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BATHWATER = ITEMS.register("bathwater", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BATHWATER_MK2 = ITEMS.register("bathwater_mk2", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BDCL = ITEMS.register("bdcl", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BEDROCK_ORE_FRAGMENT = ITEMS.register("bedrock_ore_fragment", () -> new Item(new Item.Properties()));
    // ═══ Bedrock Ore Progression: Rohprodukt + alle Veredelungsstufen (Grade x Type) ═══
    // Grade-Namen/Traits 1:1 aus ItemBedrockOreNew.BedrockOreGrade (Original-Repo) uebernommen.
    public static final RegistrySupplier<Item> BEDROCK_ORE_BASE = ITEMS.register("bedrock_ore_base", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BEDROCK_ORE_BASE_LIGHT = ITEMS.register("bedrock_ore_base_light", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.BASE, com.hbm_m.worldgen.BedrockOreDensity.Type.LIGHT));
    public static final RegistrySupplier<Item> BEDROCK_ORE_BASE_HEAVY = ITEMS.register("bedrock_ore_base_heavy", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.BASE, com.hbm_m.worldgen.BedrockOreDensity.Type.HEAVY));
    public static final RegistrySupplier<Item> BEDROCK_ORE_BASE_RARE = ITEMS.register("bedrock_ore_base_rare", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.BASE, com.hbm_m.worldgen.BedrockOreDensity.Type.RARE));
    public static final RegistrySupplier<Item> BEDROCK_ORE_BASE_ACTINIDE = ITEMS.register("bedrock_ore_base_actinide", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.BASE, com.hbm_m.worldgen.BedrockOreDensity.Type.ACTINIDE));
    public static final RegistrySupplier<Item> BEDROCK_ORE_BASE_NONMETAL = ITEMS.register("bedrock_ore_base_nonmetal", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.BASE, com.hbm_m.worldgen.BedrockOreDensity.Type.NONMETAL));
    public static final RegistrySupplier<Item> BEDROCK_ORE_BASE_CRYSTAL = ITEMS.register("bedrock_ore_base_crystal", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.BASE, com.hbm_m.worldgen.BedrockOreDensity.Type.CRYSTAL));
    public static final RegistrySupplier<Item> BEDROCK_ORE_BASE_ROASTED_LIGHT = ITEMS.register("bedrock_ore_base_roasted_light", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.BASE_ROASTED, com.hbm_m.worldgen.BedrockOreDensity.Type.LIGHT));
    public static final RegistrySupplier<Item> BEDROCK_ORE_BASE_ROASTED_HEAVY = ITEMS.register("bedrock_ore_base_roasted_heavy", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.BASE_ROASTED, com.hbm_m.worldgen.BedrockOreDensity.Type.HEAVY));
    public static final RegistrySupplier<Item> BEDROCK_ORE_BASE_ROASTED_RARE = ITEMS.register("bedrock_ore_base_roasted_rare", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.BASE_ROASTED, com.hbm_m.worldgen.BedrockOreDensity.Type.RARE));
    public static final RegistrySupplier<Item> BEDROCK_ORE_BASE_ROASTED_ACTINIDE = ITEMS.register("bedrock_ore_base_roasted_actinide", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.BASE_ROASTED, com.hbm_m.worldgen.BedrockOreDensity.Type.ACTINIDE));
    public static final RegistrySupplier<Item> BEDROCK_ORE_BASE_ROASTED_NONMETAL = ITEMS.register("bedrock_ore_base_roasted_nonmetal", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.BASE_ROASTED, com.hbm_m.worldgen.BedrockOreDensity.Type.NONMETAL));
    public static final RegistrySupplier<Item> BEDROCK_ORE_BASE_ROASTED_CRYSTAL = ITEMS.register("bedrock_ore_base_roasted_crystal", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.BASE_ROASTED, com.hbm_m.worldgen.BedrockOreDensity.Type.CRYSTAL));
    public static final RegistrySupplier<Item> BEDROCK_ORE_BASE_WASHED_LIGHT = ITEMS.register("bedrock_ore_base_washed_light", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.BASE_WASHED, com.hbm_m.worldgen.BedrockOreDensity.Type.LIGHT));
    public static final RegistrySupplier<Item> BEDROCK_ORE_BASE_WASHED_HEAVY = ITEMS.register("bedrock_ore_base_washed_heavy", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.BASE_WASHED, com.hbm_m.worldgen.BedrockOreDensity.Type.HEAVY));
    public static final RegistrySupplier<Item> BEDROCK_ORE_BASE_WASHED_RARE = ITEMS.register("bedrock_ore_base_washed_rare", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.BASE_WASHED, com.hbm_m.worldgen.BedrockOreDensity.Type.RARE));
    public static final RegistrySupplier<Item> BEDROCK_ORE_BASE_WASHED_ACTINIDE = ITEMS.register("bedrock_ore_base_washed_actinide", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.BASE_WASHED, com.hbm_m.worldgen.BedrockOreDensity.Type.ACTINIDE));
    public static final RegistrySupplier<Item> BEDROCK_ORE_BASE_WASHED_NONMETAL = ITEMS.register("bedrock_ore_base_washed_nonmetal", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.BASE_WASHED, com.hbm_m.worldgen.BedrockOreDensity.Type.NONMETAL));
    public static final RegistrySupplier<Item> BEDROCK_ORE_BASE_WASHED_CRYSTAL = ITEMS.register("bedrock_ore_base_washed_crystal", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.BASE_WASHED, com.hbm_m.worldgen.BedrockOreDensity.Type.CRYSTAL));
    public static final RegistrySupplier<Item> BEDROCK_ORE_PRIMARY_LIGHT = ITEMS.register("bedrock_ore_primary_light", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.PRIMARY, com.hbm_m.worldgen.BedrockOreDensity.Type.LIGHT));
    public static final RegistrySupplier<Item> BEDROCK_ORE_PRIMARY_HEAVY = ITEMS.register("bedrock_ore_primary_heavy", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.PRIMARY, com.hbm_m.worldgen.BedrockOreDensity.Type.HEAVY));
    public static final RegistrySupplier<Item> BEDROCK_ORE_PRIMARY_RARE = ITEMS.register("bedrock_ore_primary_rare", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.PRIMARY, com.hbm_m.worldgen.BedrockOreDensity.Type.RARE));
    public static final RegistrySupplier<Item> BEDROCK_ORE_PRIMARY_ACTINIDE = ITEMS.register("bedrock_ore_primary_actinide", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.PRIMARY, com.hbm_m.worldgen.BedrockOreDensity.Type.ACTINIDE));
    public static final RegistrySupplier<Item> BEDROCK_ORE_PRIMARY_NONMETAL = ITEMS.register("bedrock_ore_primary_nonmetal", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.PRIMARY, com.hbm_m.worldgen.BedrockOreDensity.Type.NONMETAL));
    public static final RegistrySupplier<Item> BEDROCK_ORE_PRIMARY_CRYSTAL = ITEMS.register("bedrock_ore_primary_crystal", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.PRIMARY, com.hbm_m.worldgen.BedrockOreDensity.Type.CRYSTAL));
    public static final RegistrySupplier<Item> BEDROCK_ORE_PRIMARY_ROASTED_LIGHT = ITEMS.register("bedrock_ore_primary_roasted_light", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.PRIMARY_ROASTED, com.hbm_m.worldgen.BedrockOreDensity.Type.LIGHT));
    public static final RegistrySupplier<Item> BEDROCK_ORE_PRIMARY_ROASTED_HEAVY = ITEMS.register("bedrock_ore_primary_roasted_heavy", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.PRIMARY_ROASTED, com.hbm_m.worldgen.BedrockOreDensity.Type.HEAVY));
    public static final RegistrySupplier<Item> BEDROCK_ORE_PRIMARY_ROASTED_RARE = ITEMS.register("bedrock_ore_primary_roasted_rare", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.PRIMARY_ROASTED, com.hbm_m.worldgen.BedrockOreDensity.Type.RARE));
    public static final RegistrySupplier<Item> BEDROCK_ORE_PRIMARY_ROASTED_ACTINIDE = ITEMS.register("bedrock_ore_primary_roasted_actinide", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.PRIMARY_ROASTED, com.hbm_m.worldgen.BedrockOreDensity.Type.ACTINIDE));
    public static final RegistrySupplier<Item> BEDROCK_ORE_PRIMARY_ROASTED_NONMETAL = ITEMS.register("bedrock_ore_primary_roasted_nonmetal", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.PRIMARY_ROASTED, com.hbm_m.worldgen.BedrockOreDensity.Type.NONMETAL));
    public static final RegistrySupplier<Item> BEDROCK_ORE_PRIMARY_ROASTED_CRYSTAL = ITEMS.register("bedrock_ore_primary_roasted_crystal", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.PRIMARY_ROASTED, com.hbm_m.worldgen.BedrockOreDensity.Type.CRYSTAL));
    public static final RegistrySupplier<Item> BEDROCK_ORE_PRIMARY_SULFURIC_LIGHT = ITEMS.register("bedrock_ore_primary_sulfuric_light", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.PRIMARY_SULFURIC, com.hbm_m.worldgen.BedrockOreDensity.Type.LIGHT));
    public static final RegistrySupplier<Item> BEDROCK_ORE_PRIMARY_SULFURIC_HEAVY = ITEMS.register("bedrock_ore_primary_sulfuric_heavy", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.PRIMARY_SULFURIC, com.hbm_m.worldgen.BedrockOreDensity.Type.HEAVY));
    public static final RegistrySupplier<Item> BEDROCK_ORE_PRIMARY_SULFURIC_RARE = ITEMS.register("bedrock_ore_primary_sulfuric_rare", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.PRIMARY_SULFURIC, com.hbm_m.worldgen.BedrockOreDensity.Type.RARE));
    public static final RegistrySupplier<Item> BEDROCK_ORE_PRIMARY_SULFURIC_ACTINIDE = ITEMS.register("bedrock_ore_primary_sulfuric_actinide", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.PRIMARY_SULFURIC, com.hbm_m.worldgen.BedrockOreDensity.Type.ACTINIDE));
    public static final RegistrySupplier<Item> BEDROCK_ORE_PRIMARY_SULFURIC_NONMETAL = ITEMS.register("bedrock_ore_primary_sulfuric_nonmetal", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.PRIMARY_SULFURIC, com.hbm_m.worldgen.BedrockOreDensity.Type.NONMETAL));
    public static final RegistrySupplier<Item> BEDROCK_ORE_PRIMARY_SULFURIC_CRYSTAL = ITEMS.register("bedrock_ore_primary_sulfuric_crystal", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.PRIMARY_SULFURIC, com.hbm_m.worldgen.BedrockOreDensity.Type.CRYSTAL));
    public static final RegistrySupplier<Item> BEDROCK_ORE_PRIMARY_NOSULFURIC_LIGHT = ITEMS.register("bedrock_ore_primary_nosulfuric_light", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.PRIMARY_NOSULFURIC, com.hbm_m.worldgen.BedrockOreDensity.Type.LIGHT));
    public static final RegistrySupplier<Item> BEDROCK_ORE_PRIMARY_NOSULFURIC_HEAVY = ITEMS.register("bedrock_ore_primary_nosulfuric_heavy", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.PRIMARY_NOSULFURIC, com.hbm_m.worldgen.BedrockOreDensity.Type.HEAVY));
    public static final RegistrySupplier<Item> BEDROCK_ORE_PRIMARY_NOSULFURIC_RARE = ITEMS.register("bedrock_ore_primary_nosulfuric_rare", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.PRIMARY_NOSULFURIC, com.hbm_m.worldgen.BedrockOreDensity.Type.RARE));
    public static final RegistrySupplier<Item> BEDROCK_ORE_PRIMARY_NOSULFURIC_ACTINIDE = ITEMS.register("bedrock_ore_primary_nosulfuric_actinide", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.PRIMARY_NOSULFURIC, com.hbm_m.worldgen.BedrockOreDensity.Type.ACTINIDE));
    public static final RegistrySupplier<Item> BEDROCK_ORE_PRIMARY_NOSULFURIC_NONMETAL = ITEMS.register("bedrock_ore_primary_nosulfuric_nonmetal", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.PRIMARY_NOSULFURIC, com.hbm_m.worldgen.BedrockOreDensity.Type.NONMETAL));
    public static final RegistrySupplier<Item> BEDROCK_ORE_PRIMARY_NOSULFURIC_CRYSTAL = ITEMS.register("bedrock_ore_primary_nosulfuric_crystal", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.PRIMARY_NOSULFURIC, com.hbm_m.worldgen.BedrockOreDensity.Type.CRYSTAL));
    public static final RegistrySupplier<Item> BEDROCK_ORE_PRIMARY_SOLVENT_LIGHT = ITEMS.register("bedrock_ore_primary_solvent_light", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.PRIMARY_SOLVENT, com.hbm_m.worldgen.BedrockOreDensity.Type.LIGHT));
    public static final RegistrySupplier<Item> BEDROCK_ORE_PRIMARY_SOLVENT_HEAVY = ITEMS.register("bedrock_ore_primary_solvent_heavy", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.PRIMARY_SOLVENT, com.hbm_m.worldgen.BedrockOreDensity.Type.HEAVY));
    public static final RegistrySupplier<Item> BEDROCK_ORE_PRIMARY_SOLVENT_RARE = ITEMS.register("bedrock_ore_primary_solvent_rare", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.PRIMARY_SOLVENT, com.hbm_m.worldgen.BedrockOreDensity.Type.RARE));
    public static final RegistrySupplier<Item> BEDROCK_ORE_PRIMARY_SOLVENT_ACTINIDE = ITEMS.register("bedrock_ore_primary_solvent_actinide", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.PRIMARY_SOLVENT, com.hbm_m.worldgen.BedrockOreDensity.Type.ACTINIDE));
    public static final RegistrySupplier<Item> BEDROCK_ORE_PRIMARY_SOLVENT_NONMETAL = ITEMS.register("bedrock_ore_primary_solvent_nonmetal", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.PRIMARY_SOLVENT, com.hbm_m.worldgen.BedrockOreDensity.Type.NONMETAL));
    public static final RegistrySupplier<Item> BEDROCK_ORE_PRIMARY_SOLVENT_CRYSTAL = ITEMS.register("bedrock_ore_primary_solvent_crystal", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.PRIMARY_SOLVENT, com.hbm_m.worldgen.BedrockOreDensity.Type.CRYSTAL));
    public static final RegistrySupplier<Item> BEDROCK_ORE_PRIMARY_NOSOLVENT_LIGHT = ITEMS.register("bedrock_ore_primary_nosolvent_light", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.PRIMARY_NOSOLVENT, com.hbm_m.worldgen.BedrockOreDensity.Type.LIGHT));
    public static final RegistrySupplier<Item> BEDROCK_ORE_PRIMARY_NOSOLVENT_HEAVY = ITEMS.register("bedrock_ore_primary_nosolvent_heavy", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.PRIMARY_NOSOLVENT, com.hbm_m.worldgen.BedrockOreDensity.Type.HEAVY));
    public static final RegistrySupplier<Item> BEDROCK_ORE_PRIMARY_NOSOLVENT_RARE = ITEMS.register("bedrock_ore_primary_nosolvent_rare", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.PRIMARY_NOSOLVENT, com.hbm_m.worldgen.BedrockOreDensity.Type.RARE));
    public static final RegistrySupplier<Item> BEDROCK_ORE_PRIMARY_NOSOLVENT_ACTINIDE = ITEMS.register("bedrock_ore_primary_nosolvent_actinide", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.PRIMARY_NOSOLVENT, com.hbm_m.worldgen.BedrockOreDensity.Type.ACTINIDE));
    public static final RegistrySupplier<Item> BEDROCK_ORE_PRIMARY_NOSOLVENT_NONMETAL = ITEMS.register("bedrock_ore_primary_nosolvent_nonmetal", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.PRIMARY_NOSOLVENT, com.hbm_m.worldgen.BedrockOreDensity.Type.NONMETAL));
    public static final RegistrySupplier<Item> BEDROCK_ORE_PRIMARY_NOSOLVENT_CRYSTAL = ITEMS.register("bedrock_ore_primary_nosolvent_crystal", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.PRIMARY_NOSOLVENT, com.hbm_m.worldgen.BedrockOreDensity.Type.CRYSTAL));
    public static final RegistrySupplier<Item> BEDROCK_ORE_PRIMARY_RAD_LIGHT = ITEMS.register("bedrock_ore_primary_rad_light", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.PRIMARY_RAD, com.hbm_m.worldgen.BedrockOreDensity.Type.LIGHT));
    public static final RegistrySupplier<Item> BEDROCK_ORE_PRIMARY_RAD_HEAVY = ITEMS.register("bedrock_ore_primary_rad_heavy", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.PRIMARY_RAD, com.hbm_m.worldgen.BedrockOreDensity.Type.HEAVY));
    public static final RegistrySupplier<Item> BEDROCK_ORE_PRIMARY_RAD_RARE = ITEMS.register("bedrock_ore_primary_rad_rare", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.PRIMARY_RAD, com.hbm_m.worldgen.BedrockOreDensity.Type.RARE));
    public static final RegistrySupplier<Item> BEDROCK_ORE_PRIMARY_RAD_ACTINIDE = ITEMS.register("bedrock_ore_primary_rad_actinide", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.PRIMARY_RAD, com.hbm_m.worldgen.BedrockOreDensity.Type.ACTINIDE));
    public static final RegistrySupplier<Item> BEDROCK_ORE_PRIMARY_RAD_NONMETAL = ITEMS.register("bedrock_ore_primary_rad_nonmetal", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.PRIMARY_RAD, com.hbm_m.worldgen.BedrockOreDensity.Type.NONMETAL));
    public static final RegistrySupplier<Item> BEDROCK_ORE_PRIMARY_RAD_CRYSTAL = ITEMS.register("bedrock_ore_primary_rad_crystal", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.PRIMARY_RAD, com.hbm_m.worldgen.BedrockOreDensity.Type.CRYSTAL));
    public static final RegistrySupplier<Item> BEDROCK_ORE_PRIMARY_NORAD_LIGHT = ITEMS.register("bedrock_ore_primary_norad_light", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.PRIMARY_NORAD, com.hbm_m.worldgen.BedrockOreDensity.Type.LIGHT));
    public static final RegistrySupplier<Item> BEDROCK_ORE_PRIMARY_NORAD_HEAVY = ITEMS.register("bedrock_ore_primary_norad_heavy", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.PRIMARY_NORAD, com.hbm_m.worldgen.BedrockOreDensity.Type.HEAVY));
    public static final RegistrySupplier<Item> BEDROCK_ORE_PRIMARY_NORAD_RARE = ITEMS.register("bedrock_ore_primary_norad_rare", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.PRIMARY_NORAD, com.hbm_m.worldgen.BedrockOreDensity.Type.RARE));
    public static final RegistrySupplier<Item> BEDROCK_ORE_PRIMARY_NORAD_ACTINIDE = ITEMS.register("bedrock_ore_primary_norad_actinide", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.PRIMARY_NORAD, com.hbm_m.worldgen.BedrockOreDensity.Type.ACTINIDE));
    public static final RegistrySupplier<Item> BEDROCK_ORE_PRIMARY_NORAD_NONMETAL = ITEMS.register("bedrock_ore_primary_norad_nonmetal", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.PRIMARY_NORAD, com.hbm_m.worldgen.BedrockOreDensity.Type.NONMETAL));
    public static final RegistrySupplier<Item> BEDROCK_ORE_PRIMARY_NORAD_CRYSTAL = ITEMS.register("bedrock_ore_primary_norad_crystal", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.PRIMARY_NORAD, com.hbm_m.worldgen.BedrockOreDensity.Type.CRYSTAL));
    public static final RegistrySupplier<Item> BEDROCK_ORE_PRIMARY_FIRST_LIGHT = ITEMS.register("bedrock_ore_primary_first_light", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.PRIMARY_FIRST, com.hbm_m.worldgen.BedrockOreDensity.Type.LIGHT));
    public static final RegistrySupplier<Item> BEDROCK_ORE_PRIMARY_FIRST_HEAVY = ITEMS.register("bedrock_ore_primary_first_heavy", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.PRIMARY_FIRST, com.hbm_m.worldgen.BedrockOreDensity.Type.HEAVY));
    public static final RegistrySupplier<Item> BEDROCK_ORE_PRIMARY_FIRST_RARE = ITEMS.register("bedrock_ore_primary_first_rare", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.PRIMARY_FIRST, com.hbm_m.worldgen.BedrockOreDensity.Type.RARE));
    public static final RegistrySupplier<Item> BEDROCK_ORE_PRIMARY_FIRST_ACTINIDE = ITEMS.register("bedrock_ore_primary_first_actinide", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.PRIMARY_FIRST, com.hbm_m.worldgen.BedrockOreDensity.Type.ACTINIDE));
    public static final RegistrySupplier<Item> BEDROCK_ORE_PRIMARY_FIRST_NONMETAL = ITEMS.register("bedrock_ore_primary_first_nonmetal", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.PRIMARY_FIRST, com.hbm_m.worldgen.BedrockOreDensity.Type.NONMETAL));
    public static final RegistrySupplier<Item> BEDROCK_ORE_PRIMARY_FIRST_CRYSTAL = ITEMS.register("bedrock_ore_primary_first_crystal", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.PRIMARY_FIRST, com.hbm_m.worldgen.BedrockOreDensity.Type.CRYSTAL));
    public static final RegistrySupplier<Item> BEDROCK_ORE_PRIMARY_SECOND_LIGHT = ITEMS.register("bedrock_ore_primary_second_light", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.PRIMARY_SECOND, com.hbm_m.worldgen.BedrockOreDensity.Type.LIGHT));
    public static final RegistrySupplier<Item> BEDROCK_ORE_PRIMARY_SECOND_HEAVY = ITEMS.register("bedrock_ore_primary_second_heavy", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.PRIMARY_SECOND, com.hbm_m.worldgen.BedrockOreDensity.Type.HEAVY));
    public static final RegistrySupplier<Item> BEDROCK_ORE_PRIMARY_SECOND_RARE = ITEMS.register("bedrock_ore_primary_second_rare", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.PRIMARY_SECOND, com.hbm_m.worldgen.BedrockOreDensity.Type.RARE));
    public static final RegistrySupplier<Item> BEDROCK_ORE_PRIMARY_SECOND_ACTINIDE = ITEMS.register("bedrock_ore_primary_second_actinide", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.PRIMARY_SECOND, com.hbm_m.worldgen.BedrockOreDensity.Type.ACTINIDE));
    public static final RegistrySupplier<Item> BEDROCK_ORE_PRIMARY_SECOND_NONMETAL = ITEMS.register("bedrock_ore_primary_second_nonmetal", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.PRIMARY_SECOND, com.hbm_m.worldgen.BedrockOreDensity.Type.NONMETAL));
    public static final RegistrySupplier<Item> BEDROCK_ORE_PRIMARY_SECOND_CRYSTAL = ITEMS.register("bedrock_ore_primary_second_crystal", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.PRIMARY_SECOND, com.hbm_m.worldgen.BedrockOreDensity.Type.CRYSTAL));
    public static final RegistrySupplier<Item> BEDROCK_ORE_CRUMBS_LIGHT = ITEMS.register("bedrock_ore_crumbs_light", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.CRUMBS, com.hbm_m.worldgen.BedrockOreDensity.Type.LIGHT));
    public static final RegistrySupplier<Item> BEDROCK_ORE_CRUMBS_HEAVY = ITEMS.register("bedrock_ore_crumbs_heavy", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.CRUMBS, com.hbm_m.worldgen.BedrockOreDensity.Type.HEAVY));
    public static final RegistrySupplier<Item> BEDROCK_ORE_CRUMBS_RARE = ITEMS.register("bedrock_ore_crumbs_rare", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.CRUMBS, com.hbm_m.worldgen.BedrockOreDensity.Type.RARE));
    public static final RegistrySupplier<Item> BEDROCK_ORE_CRUMBS_ACTINIDE = ITEMS.register("bedrock_ore_crumbs_actinide", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.CRUMBS, com.hbm_m.worldgen.BedrockOreDensity.Type.ACTINIDE));
    public static final RegistrySupplier<Item> BEDROCK_ORE_CRUMBS_NONMETAL = ITEMS.register("bedrock_ore_crumbs_nonmetal", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.CRUMBS, com.hbm_m.worldgen.BedrockOreDensity.Type.NONMETAL));
    public static final RegistrySupplier<Item> BEDROCK_ORE_CRUMBS_CRYSTAL = ITEMS.register("bedrock_ore_crumbs_crystal", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.CRUMBS, com.hbm_m.worldgen.BedrockOreDensity.Type.CRYSTAL));
    public static final RegistrySupplier<Item> BEDROCK_ORE_SULFURIC_BYPRODUCT_LIGHT = ITEMS.register("bedrock_ore_sulfuric_byproduct_light", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.SULFURIC_BYPRODUCT, com.hbm_m.worldgen.BedrockOreDensity.Type.LIGHT));
    public static final RegistrySupplier<Item> BEDROCK_ORE_SULFURIC_BYPRODUCT_HEAVY = ITEMS.register("bedrock_ore_sulfuric_byproduct_heavy", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.SULFURIC_BYPRODUCT, com.hbm_m.worldgen.BedrockOreDensity.Type.HEAVY));
    public static final RegistrySupplier<Item> BEDROCK_ORE_SULFURIC_BYPRODUCT_RARE = ITEMS.register("bedrock_ore_sulfuric_byproduct_rare", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.SULFURIC_BYPRODUCT, com.hbm_m.worldgen.BedrockOreDensity.Type.RARE));
    public static final RegistrySupplier<Item> BEDROCK_ORE_SULFURIC_BYPRODUCT_ACTINIDE = ITEMS.register("bedrock_ore_sulfuric_byproduct_actinide", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.SULFURIC_BYPRODUCT, com.hbm_m.worldgen.BedrockOreDensity.Type.ACTINIDE));
    public static final RegistrySupplier<Item> BEDROCK_ORE_SULFURIC_BYPRODUCT_NONMETAL = ITEMS.register("bedrock_ore_sulfuric_byproduct_nonmetal", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.SULFURIC_BYPRODUCT, com.hbm_m.worldgen.BedrockOreDensity.Type.NONMETAL));
    public static final RegistrySupplier<Item> BEDROCK_ORE_SULFURIC_BYPRODUCT_CRYSTAL = ITEMS.register("bedrock_ore_sulfuric_byproduct_crystal", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.SULFURIC_BYPRODUCT, com.hbm_m.worldgen.BedrockOreDensity.Type.CRYSTAL));
    public static final RegistrySupplier<Item> BEDROCK_ORE_SULFURIC_ROASTED_LIGHT = ITEMS.register("bedrock_ore_sulfuric_roasted_light", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.SULFURIC_ROASTED, com.hbm_m.worldgen.BedrockOreDensity.Type.LIGHT));
    public static final RegistrySupplier<Item> BEDROCK_ORE_SULFURIC_ROASTED_HEAVY = ITEMS.register("bedrock_ore_sulfuric_roasted_heavy", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.SULFURIC_ROASTED, com.hbm_m.worldgen.BedrockOreDensity.Type.HEAVY));
    public static final RegistrySupplier<Item> BEDROCK_ORE_SULFURIC_ROASTED_RARE = ITEMS.register("bedrock_ore_sulfuric_roasted_rare", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.SULFURIC_ROASTED, com.hbm_m.worldgen.BedrockOreDensity.Type.RARE));
    public static final RegistrySupplier<Item> BEDROCK_ORE_SULFURIC_ROASTED_ACTINIDE = ITEMS.register("bedrock_ore_sulfuric_roasted_actinide", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.SULFURIC_ROASTED, com.hbm_m.worldgen.BedrockOreDensity.Type.ACTINIDE));
    public static final RegistrySupplier<Item> BEDROCK_ORE_SULFURIC_ROASTED_NONMETAL = ITEMS.register("bedrock_ore_sulfuric_roasted_nonmetal", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.SULFURIC_ROASTED, com.hbm_m.worldgen.BedrockOreDensity.Type.NONMETAL));
    public static final RegistrySupplier<Item> BEDROCK_ORE_SULFURIC_ROASTED_CRYSTAL = ITEMS.register("bedrock_ore_sulfuric_roasted_crystal", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.SULFURIC_ROASTED, com.hbm_m.worldgen.BedrockOreDensity.Type.CRYSTAL));
    public static final RegistrySupplier<Item> BEDROCK_ORE_SULFURIC_ARC_LIGHT = ITEMS.register("bedrock_ore_sulfuric_arc_light", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.SULFURIC_ARC, com.hbm_m.worldgen.BedrockOreDensity.Type.LIGHT));
    public static final RegistrySupplier<Item> BEDROCK_ORE_SULFURIC_ARC_HEAVY = ITEMS.register("bedrock_ore_sulfuric_arc_heavy", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.SULFURIC_ARC, com.hbm_m.worldgen.BedrockOreDensity.Type.HEAVY));
    public static final RegistrySupplier<Item> BEDROCK_ORE_SULFURIC_ARC_RARE = ITEMS.register("bedrock_ore_sulfuric_arc_rare", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.SULFURIC_ARC, com.hbm_m.worldgen.BedrockOreDensity.Type.RARE));
    public static final RegistrySupplier<Item> BEDROCK_ORE_SULFURIC_ARC_ACTINIDE = ITEMS.register("bedrock_ore_sulfuric_arc_actinide", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.SULFURIC_ARC, com.hbm_m.worldgen.BedrockOreDensity.Type.ACTINIDE));
    public static final RegistrySupplier<Item> BEDROCK_ORE_SULFURIC_ARC_NONMETAL = ITEMS.register("bedrock_ore_sulfuric_arc_nonmetal", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.SULFURIC_ARC, com.hbm_m.worldgen.BedrockOreDensity.Type.NONMETAL));
    public static final RegistrySupplier<Item> BEDROCK_ORE_SULFURIC_ARC_CRYSTAL = ITEMS.register("bedrock_ore_sulfuric_arc_crystal", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.SULFURIC_ARC, com.hbm_m.worldgen.BedrockOreDensity.Type.CRYSTAL));
    public static final RegistrySupplier<Item> BEDROCK_ORE_SULFURIC_WASHED_LIGHT = ITEMS.register("bedrock_ore_sulfuric_washed_light", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.SULFURIC_WASHED, com.hbm_m.worldgen.BedrockOreDensity.Type.LIGHT));
    public static final RegistrySupplier<Item> BEDROCK_ORE_SULFURIC_WASHED_HEAVY = ITEMS.register("bedrock_ore_sulfuric_washed_heavy", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.SULFURIC_WASHED, com.hbm_m.worldgen.BedrockOreDensity.Type.HEAVY));
    public static final RegistrySupplier<Item> BEDROCK_ORE_SULFURIC_WASHED_RARE = ITEMS.register("bedrock_ore_sulfuric_washed_rare", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.SULFURIC_WASHED, com.hbm_m.worldgen.BedrockOreDensity.Type.RARE));
    public static final RegistrySupplier<Item> BEDROCK_ORE_SULFURIC_WASHED_ACTINIDE = ITEMS.register("bedrock_ore_sulfuric_washed_actinide", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.SULFURIC_WASHED, com.hbm_m.worldgen.BedrockOreDensity.Type.ACTINIDE));
    public static final RegistrySupplier<Item> BEDROCK_ORE_SULFURIC_WASHED_NONMETAL = ITEMS.register("bedrock_ore_sulfuric_washed_nonmetal", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.SULFURIC_WASHED, com.hbm_m.worldgen.BedrockOreDensity.Type.NONMETAL));
    public static final RegistrySupplier<Item> BEDROCK_ORE_SULFURIC_WASHED_CRYSTAL = ITEMS.register("bedrock_ore_sulfuric_washed_crystal", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.SULFURIC_WASHED, com.hbm_m.worldgen.BedrockOreDensity.Type.CRYSTAL));
    public static final RegistrySupplier<Item> BEDROCK_ORE_SOLVENT_BYPRODUCT_LIGHT = ITEMS.register("bedrock_ore_solvent_byproduct_light", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.SOLVENT_BYPRODUCT, com.hbm_m.worldgen.BedrockOreDensity.Type.LIGHT));
    public static final RegistrySupplier<Item> BEDROCK_ORE_SOLVENT_BYPRODUCT_HEAVY = ITEMS.register("bedrock_ore_solvent_byproduct_heavy", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.SOLVENT_BYPRODUCT, com.hbm_m.worldgen.BedrockOreDensity.Type.HEAVY));
    public static final RegistrySupplier<Item> BEDROCK_ORE_SOLVENT_BYPRODUCT_RARE = ITEMS.register("bedrock_ore_solvent_byproduct_rare", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.SOLVENT_BYPRODUCT, com.hbm_m.worldgen.BedrockOreDensity.Type.RARE));
    public static final RegistrySupplier<Item> BEDROCK_ORE_SOLVENT_BYPRODUCT_ACTINIDE = ITEMS.register("bedrock_ore_solvent_byproduct_actinide", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.SOLVENT_BYPRODUCT, com.hbm_m.worldgen.BedrockOreDensity.Type.ACTINIDE));
    public static final RegistrySupplier<Item> BEDROCK_ORE_SOLVENT_BYPRODUCT_NONMETAL = ITEMS.register("bedrock_ore_solvent_byproduct_nonmetal", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.SOLVENT_BYPRODUCT, com.hbm_m.worldgen.BedrockOreDensity.Type.NONMETAL));
    public static final RegistrySupplier<Item> BEDROCK_ORE_SOLVENT_BYPRODUCT_CRYSTAL = ITEMS.register("bedrock_ore_solvent_byproduct_crystal", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.SOLVENT_BYPRODUCT, com.hbm_m.worldgen.BedrockOreDensity.Type.CRYSTAL));
    public static final RegistrySupplier<Item> BEDROCK_ORE_SOLVENT_ROASTED_LIGHT = ITEMS.register("bedrock_ore_solvent_roasted_light", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.SOLVENT_ROASTED, com.hbm_m.worldgen.BedrockOreDensity.Type.LIGHT));
    public static final RegistrySupplier<Item> BEDROCK_ORE_SOLVENT_ROASTED_HEAVY = ITEMS.register("bedrock_ore_solvent_roasted_heavy", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.SOLVENT_ROASTED, com.hbm_m.worldgen.BedrockOreDensity.Type.HEAVY));
    public static final RegistrySupplier<Item> BEDROCK_ORE_SOLVENT_ROASTED_RARE = ITEMS.register("bedrock_ore_solvent_roasted_rare", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.SOLVENT_ROASTED, com.hbm_m.worldgen.BedrockOreDensity.Type.RARE));
    public static final RegistrySupplier<Item> BEDROCK_ORE_SOLVENT_ROASTED_ACTINIDE = ITEMS.register("bedrock_ore_solvent_roasted_actinide", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.SOLVENT_ROASTED, com.hbm_m.worldgen.BedrockOreDensity.Type.ACTINIDE));
    public static final RegistrySupplier<Item> BEDROCK_ORE_SOLVENT_ROASTED_NONMETAL = ITEMS.register("bedrock_ore_solvent_roasted_nonmetal", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.SOLVENT_ROASTED, com.hbm_m.worldgen.BedrockOreDensity.Type.NONMETAL));
    public static final RegistrySupplier<Item> BEDROCK_ORE_SOLVENT_ROASTED_CRYSTAL = ITEMS.register("bedrock_ore_solvent_roasted_crystal", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.SOLVENT_ROASTED, com.hbm_m.worldgen.BedrockOreDensity.Type.CRYSTAL));
    public static final RegistrySupplier<Item> BEDROCK_ORE_SOLVENT_ARC_LIGHT = ITEMS.register("bedrock_ore_solvent_arc_light", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.SOLVENT_ARC, com.hbm_m.worldgen.BedrockOreDensity.Type.LIGHT));
    public static final RegistrySupplier<Item> BEDROCK_ORE_SOLVENT_ARC_HEAVY = ITEMS.register("bedrock_ore_solvent_arc_heavy", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.SOLVENT_ARC, com.hbm_m.worldgen.BedrockOreDensity.Type.HEAVY));
    public static final RegistrySupplier<Item> BEDROCK_ORE_SOLVENT_ARC_RARE = ITEMS.register("bedrock_ore_solvent_arc_rare", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.SOLVENT_ARC, com.hbm_m.worldgen.BedrockOreDensity.Type.RARE));
    public static final RegistrySupplier<Item> BEDROCK_ORE_SOLVENT_ARC_ACTINIDE = ITEMS.register("bedrock_ore_solvent_arc_actinide", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.SOLVENT_ARC, com.hbm_m.worldgen.BedrockOreDensity.Type.ACTINIDE));
    public static final RegistrySupplier<Item> BEDROCK_ORE_SOLVENT_ARC_NONMETAL = ITEMS.register("bedrock_ore_solvent_arc_nonmetal", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.SOLVENT_ARC, com.hbm_m.worldgen.BedrockOreDensity.Type.NONMETAL));
    public static final RegistrySupplier<Item> BEDROCK_ORE_SOLVENT_ARC_CRYSTAL = ITEMS.register("bedrock_ore_solvent_arc_crystal", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.SOLVENT_ARC, com.hbm_m.worldgen.BedrockOreDensity.Type.CRYSTAL));
    public static final RegistrySupplier<Item> BEDROCK_ORE_SOLVENT_WASHED_LIGHT = ITEMS.register("bedrock_ore_solvent_washed_light", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.SOLVENT_WASHED, com.hbm_m.worldgen.BedrockOreDensity.Type.LIGHT));
    public static final RegistrySupplier<Item> BEDROCK_ORE_SOLVENT_WASHED_HEAVY = ITEMS.register("bedrock_ore_solvent_washed_heavy", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.SOLVENT_WASHED, com.hbm_m.worldgen.BedrockOreDensity.Type.HEAVY));
    public static final RegistrySupplier<Item> BEDROCK_ORE_SOLVENT_WASHED_RARE = ITEMS.register("bedrock_ore_solvent_washed_rare", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.SOLVENT_WASHED, com.hbm_m.worldgen.BedrockOreDensity.Type.RARE));
    public static final RegistrySupplier<Item> BEDROCK_ORE_SOLVENT_WASHED_ACTINIDE = ITEMS.register("bedrock_ore_solvent_washed_actinide", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.SOLVENT_WASHED, com.hbm_m.worldgen.BedrockOreDensity.Type.ACTINIDE));
    public static final RegistrySupplier<Item> BEDROCK_ORE_SOLVENT_WASHED_NONMETAL = ITEMS.register("bedrock_ore_solvent_washed_nonmetal", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.SOLVENT_WASHED, com.hbm_m.worldgen.BedrockOreDensity.Type.NONMETAL));
    public static final RegistrySupplier<Item> BEDROCK_ORE_SOLVENT_WASHED_CRYSTAL = ITEMS.register("bedrock_ore_solvent_washed_crystal", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.SOLVENT_WASHED, com.hbm_m.worldgen.BedrockOreDensity.Type.CRYSTAL));
    public static final RegistrySupplier<Item> BEDROCK_ORE_RAD_BYPRODUCT_LIGHT = ITEMS.register("bedrock_ore_rad_byproduct_light", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.RAD_BYPRODUCT, com.hbm_m.worldgen.BedrockOreDensity.Type.LIGHT));
    public static final RegistrySupplier<Item> BEDROCK_ORE_RAD_BYPRODUCT_HEAVY = ITEMS.register("bedrock_ore_rad_byproduct_heavy", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.RAD_BYPRODUCT, com.hbm_m.worldgen.BedrockOreDensity.Type.HEAVY));
    public static final RegistrySupplier<Item> BEDROCK_ORE_RAD_BYPRODUCT_RARE = ITEMS.register("bedrock_ore_rad_byproduct_rare", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.RAD_BYPRODUCT, com.hbm_m.worldgen.BedrockOreDensity.Type.RARE));
    public static final RegistrySupplier<Item> BEDROCK_ORE_RAD_BYPRODUCT_ACTINIDE = ITEMS.register("bedrock_ore_rad_byproduct_actinide", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.RAD_BYPRODUCT, com.hbm_m.worldgen.BedrockOreDensity.Type.ACTINIDE));
    public static final RegistrySupplier<Item> BEDROCK_ORE_RAD_BYPRODUCT_NONMETAL = ITEMS.register("bedrock_ore_rad_byproduct_nonmetal", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.RAD_BYPRODUCT, com.hbm_m.worldgen.BedrockOreDensity.Type.NONMETAL));
    public static final RegistrySupplier<Item> BEDROCK_ORE_RAD_BYPRODUCT_CRYSTAL = ITEMS.register("bedrock_ore_rad_byproduct_crystal", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.RAD_BYPRODUCT, com.hbm_m.worldgen.BedrockOreDensity.Type.CRYSTAL));
    public static final RegistrySupplier<Item> BEDROCK_ORE_RAD_ROASTED_LIGHT = ITEMS.register("bedrock_ore_rad_roasted_light", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.RAD_ROASTED, com.hbm_m.worldgen.BedrockOreDensity.Type.LIGHT));
    public static final RegistrySupplier<Item> BEDROCK_ORE_RAD_ROASTED_HEAVY = ITEMS.register("bedrock_ore_rad_roasted_heavy", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.RAD_ROASTED, com.hbm_m.worldgen.BedrockOreDensity.Type.HEAVY));
    public static final RegistrySupplier<Item> BEDROCK_ORE_RAD_ROASTED_RARE = ITEMS.register("bedrock_ore_rad_roasted_rare", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.RAD_ROASTED, com.hbm_m.worldgen.BedrockOreDensity.Type.RARE));
    public static final RegistrySupplier<Item> BEDROCK_ORE_RAD_ROASTED_ACTINIDE = ITEMS.register("bedrock_ore_rad_roasted_actinide", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.RAD_ROASTED, com.hbm_m.worldgen.BedrockOreDensity.Type.ACTINIDE));
    public static final RegistrySupplier<Item> BEDROCK_ORE_RAD_ROASTED_NONMETAL = ITEMS.register("bedrock_ore_rad_roasted_nonmetal", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.RAD_ROASTED, com.hbm_m.worldgen.BedrockOreDensity.Type.NONMETAL));
    public static final RegistrySupplier<Item> BEDROCK_ORE_RAD_ROASTED_CRYSTAL = ITEMS.register("bedrock_ore_rad_roasted_crystal", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.RAD_ROASTED, com.hbm_m.worldgen.BedrockOreDensity.Type.CRYSTAL));
    public static final RegistrySupplier<Item> BEDROCK_ORE_RAD_ARC_LIGHT = ITEMS.register("bedrock_ore_rad_arc_light", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.RAD_ARC, com.hbm_m.worldgen.BedrockOreDensity.Type.LIGHT));
    public static final RegistrySupplier<Item> BEDROCK_ORE_RAD_ARC_HEAVY = ITEMS.register("bedrock_ore_rad_arc_heavy", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.RAD_ARC, com.hbm_m.worldgen.BedrockOreDensity.Type.HEAVY));
    public static final RegistrySupplier<Item> BEDROCK_ORE_RAD_ARC_RARE = ITEMS.register("bedrock_ore_rad_arc_rare", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.RAD_ARC, com.hbm_m.worldgen.BedrockOreDensity.Type.RARE));
    public static final RegistrySupplier<Item> BEDROCK_ORE_RAD_ARC_ACTINIDE = ITEMS.register("bedrock_ore_rad_arc_actinide", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.RAD_ARC, com.hbm_m.worldgen.BedrockOreDensity.Type.ACTINIDE));
    public static final RegistrySupplier<Item> BEDROCK_ORE_RAD_ARC_NONMETAL = ITEMS.register("bedrock_ore_rad_arc_nonmetal", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.RAD_ARC, com.hbm_m.worldgen.BedrockOreDensity.Type.NONMETAL));
    public static final RegistrySupplier<Item> BEDROCK_ORE_RAD_ARC_CRYSTAL = ITEMS.register("bedrock_ore_rad_arc_crystal", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.RAD_ARC, com.hbm_m.worldgen.BedrockOreDensity.Type.CRYSTAL));
    public static final RegistrySupplier<Item> BEDROCK_ORE_RAD_WASHED_LIGHT = ITEMS.register("bedrock_ore_rad_washed_light", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.RAD_WASHED, com.hbm_m.worldgen.BedrockOreDensity.Type.LIGHT));
    public static final RegistrySupplier<Item> BEDROCK_ORE_RAD_WASHED_HEAVY = ITEMS.register("bedrock_ore_rad_washed_heavy", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.RAD_WASHED, com.hbm_m.worldgen.BedrockOreDensity.Type.HEAVY));
    public static final RegistrySupplier<Item> BEDROCK_ORE_RAD_WASHED_RARE = ITEMS.register("bedrock_ore_rad_washed_rare", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.RAD_WASHED, com.hbm_m.worldgen.BedrockOreDensity.Type.RARE));
    public static final RegistrySupplier<Item> BEDROCK_ORE_RAD_WASHED_ACTINIDE = ITEMS.register("bedrock_ore_rad_washed_actinide", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.RAD_WASHED, com.hbm_m.worldgen.BedrockOreDensity.Type.ACTINIDE));
    public static final RegistrySupplier<Item> BEDROCK_ORE_RAD_WASHED_NONMETAL = ITEMS.register("bedrock_ore_rad_washed_nonmetal", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.RAD_WASHED, com.hbm_m.worldgen.BedrockOreDensity.Type.NONMETAL));
    public static final RegistrySupplier<Item> BEDROCK_ORE_RAD_WASHED_CRYSTAL = ITEMS.register("bedrock_ore_rad_washed_crystal", () -> new com.hbm_m.item.industrial.ItemBedrockOreGraded(new Item.Properties(), com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.RAD_WASHED, com.hbm_m.worldgen.BedrockOreDensity.Type.CRYSTAL));

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

    public static final RegistrySupplier<Item> BETA = ITEMS.register("beta", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BIG_SWORD = ITEMS.register("big_sword", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BILLET_ACTINIUM = ITEMS.register("billet_actinium", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BILLET_AM241 = ITEMS.register("billet_am241", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BILLET_AM242 = ITEMS.register("billet_am242", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BILLET_AM_MIX = ITEMS.register("billet_am_mix", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BILLET_AMERICIUM_FUEL = ITEMS.register("billet_americium_fuel", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BILLET_AU198 = ITEMS.register("billet_au198", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BILLET_AUSTRALIUM = ITEMS.register("billet_australium", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BILLET_AUSTRALIUM_GREATER = ITEMS.register("billet_australium_greater", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BILLET_AUSTRALIUM_LESSER = ITEMS.register("billet_australium_lesser", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BILLET_BALEFIRE_GOLD = ITEMS.register("billet_balefire_gold", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BILLET_BERYLLIUM = ITEMS.register("billet_beryllium", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BILLET_BISMUTH = ITEMS.register("billet_bismuth", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BILLET_CO60 = ITEMS.register("billet_co60", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BILLET_COBALT = ITEMS.register("billet_cobalt", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BILLET_FLASHLEAD = ITEMS.register("billet_flashlead", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BILLET_GH336 = ITEMS.register("billet_gh336", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BILLET_HES = ITEMS.register("billet_hes", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BILLET_LES = ITEMS.register("billet_les", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BILLET_MOX_FUEL = ITEMS.register("billet_mox_fuel", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BILLET_NEPTUNIUM = ITEMS.register("billet_neptunium", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BILLET_NEPTUNIUM_FUEL = ITEMS.register("billet_neptunium_fuel", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BILLET_NUCLEAR_WASTE = ITEMS.register("billet_nuclear_waste", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BILLET_PB209 = ITEMS.register("billet_pb209", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BILLET_PLUTONIUM_FUEL = ITEMS.register("billet_plutonium_fuel", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BILLET_PO210BE = ITEMS.register("billet_po210be", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BILLET_POLONIUM = ITEMS.register("billet_polonium", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BILLET_PU238 = ITEMS.register("billet_pu238", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BILLET_PU238BE = ITEMS.register("billet_pu238be", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BILLET_PU239 = ITEMS.register("billet_pu239", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BILLET_PU240 = ITEMS.register("billet_pu240", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BILLET_PU241 = ITEMS.register("billet_pu241", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BILLET_PU_MIX = ITEMS.register("billet_pu_mix", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BILLET_RA226 = ITEMS.register("billet_ra226", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BILLET_RA226BE = ITEMS.register("billet_ra226be", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BILLET_SCHRABIDIUM = ITEMS.register("billet_schrabidium", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BILLET_SCHRABIDIUM_FUEL = ITEMS.register("billet_schrabidium_fuel", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BILLET_SOLINIUM = ITEMS.register("billet_solinium", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BILLET_SR90 = ITEMS.register("billet_sr90", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BILLET_TECHNETIUM = ITEMS.register("billet_technetium", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BILLET_TH232 = ITEMS.register("billet_th232", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BILLET_THORIUM_FUEL = ITEMS.register("billet_thorium_fuel", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BILLET_U233 = ITEMS.register("billet_u233", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BILLET_U235 = ITEMS.register("billet_u235", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BILLET_U238 = ITEMS.register("billet_u238", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BILLET_URANIUM = ITEMS.register("billet_uranium", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BILLET_URANIUM_FUEL = ITEMS.register("billet_uranium_fuel", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BILLET_UZH = ITEMS.register("billet_uzh", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BILLET_YHARONITE = ITEMS.register("billet_yharonite", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BILLET_ZFB_AM_MIX = ITEMS.register("billet_zfb_am_mix", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BILLET_ZFB_BISMUTH = ITEMS.register("billet_zfb_bismuth", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BILLET_ZFB_PU241 = ITEMS.register("billet_zfb_pu241", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BILLET_ZIRCONIUM = ITEMS.register("billet_zirconium", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BIO_WAFER = ITEMS.register("bio_wafer", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BIOMASS = ITEMS.register("biomass", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BIOMASS_COMPRESSED = ITEMS.register("biomass_compressed", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BISMUTH_AXE = ITEMS.register("bismuth_axe", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BISMUTH_LEGS = ITEMS.register("bismuth_legs", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BISMUTH_PICKAXE = ITEMS.register("bismuth_pickaxe", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BISMUTH_PLATE = ITEMS.register("bismuth_plate", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BISMUTH_TOOL = ITEMS.register("bismuth_tool", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BJ_BOOTS = ITEMS.register("bj_boots", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BJ_HELMET = ITEMS.register("bj_helmet", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BJ_LEGS = ITEMS.register("bj_legs", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BJ_PLATE = ITEMS.register("bj_plate", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BJ_PLATE_JETPACK = ITEMS.register("bj_plate_jetpack", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BLADE_METEORITE = ITEMS.register("blade_meteorite", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BLADE_TUNGSTEN = ITEMS.register("blade_tungsten", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BLADES_ADVANCED_ALLOY = ITEMS.register("blades_advanced_alloy", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BLADES_DESH = ITEMS.register("blades_desh", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BLADES_STEEL = ITEMS.register("blades_steel", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BLADES_TITANIUM = ITEMS.register("blades_titanium", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BLOWTORCH = ITEMS.register("blowtorch", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BLUEPRINTS = ITEMS.register("blueprints", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BOARD_COPPER = ITEMS.register("board_copper", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BOAT_RUBBER = ITEMS.register("boat_rubber", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BOBMAZON = ITEMS.register("bobmazon", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BOLT_SPIKE = ITEMS.register("bolt_spike", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BOLTGUN = ITEMS.register("boltgun", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BOMB_CALLER = ITEMS.register("bomb_caller", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BOMB_WAFFLE = ITEMS.register("bomb_waffle", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BOOK_GUIDE = ITEMS.register("book_guide", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BOOK_LEMEGETON = ITEMS.register("book_lemegeton", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BOOK_OF_ = ITEMS.register("book_of_", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BOOK_SECRET = ITEMS.register("book_secret", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BOTTLE2_EMPTY = ITEMS.register("bottle2_empty", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BOTTLE2_FRITZ = ITEMS.register("bottle2_fritz", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BOTTLE2_KORL = ITEMS.register("bottle2_korl", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BOTTLE2_SUNSET = ITEMS.register("bottle2_sunset", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BOTTLE_CHERRY = ITEMS.register("bottle_cherry", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BOTTLE_EMPTY = ITEMS.register("bottle_empty", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BOTTLE_MERCURY = ITEMS.register("bottle_mercury", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BOTTLE_NUKA = ITEMS.register("bottle_nuka", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BOTTLE_OPENER = ITEMS.register("bottle_opener", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BOTTLE_QUANTUM = ITEMS.register("bottle_quantum", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BOTTLE_RAD = ITEMS.register("bottle_rad", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BOTTLE_SPARKLE = ITEMS.register("bottle_sparkle", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BOTTLED_CLOUD = ITEMS.register("bottled_cloud", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BOY_BULLET = ITEMS.register("boy_bullet", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BOY_IGNITER = ITEMS.register("boy_igniter", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BOY_KIT = ITEMS.register("boy_kit", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BOY_PROPELLANT = ITEMS.register("boy_propellant", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BOY_SHIELDING = ITEMS.register("boy_shielding", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BOY_TARGET = ITEMS.register("boy_target", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BROKEN_ITEM = ITEMS.register("broken_item", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BUCKET_ACID = ITEMS.register("bucket_acid", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BUCKET_MUD = ITEMS.register("bucket_mud", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BUCKET_SCHRABIDIC_ACID = ITEMS.register("bucket_schrabidic_acid", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BUCKET_SULFURIC_ACID = ITEMS.register("bucket_sulfuric_acid", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BUCKET_TOXIC = ITEMS.register("bucket_toxic", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BURNT_BARK = ITEMS.register("burnt_bark", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> CANISTER_EMPTY = ITEMS.register("canister_empty", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> CANISTER_NAPALM = ITEMS.register("canister_napalm", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> CANNED_SLIME = ITEMS.register("canned_slime", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> CANTEEN_VODKA = ITEMS.register("canteen_vodka", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> CAP_FRITZ = ITEMS.register("cap_fritz", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> CAP_KORL = ITEMS.register("cap_korl", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> CAP_NUKA = ITEMS.register("cap_nuka", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> CAP_QUANTUM = ITEMS.register("cap_quantum", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> CAP_RAD = ITEMS.register("cap_rad", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> CAP_SPARKLE = ITEMS.register("cap_sparkle", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> CAP_STAR = ITEMS.register("cap_star", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> CAP_SUNSET = ITEMS.register("cap_sunset", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> CAPE_GASMASK = ITEMS.register("cape_gasmask", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> CAPE_RADIATION = ITEMS.register("cape_radiation", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> CAPE_SCHRABIDIUM = ITEMS.register("cape_schrabidium", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> CARD_AOS = ITEMS.register("card_aos", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> CARD_QOS = ITEMS.register("card_qos", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> CASING_BAG = ITEMS.register("casing_bag", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> CATALYST_CLAY = ITEMS.register("catalyst_clay", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> CATALYTIC_CONVERTER = ITEMS.register("catalytic_converter", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> CBT_DEVICE = ITEMS.register("cbt_device", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> CELL_ANTI_SCHRABIDIUM = ITEMS.register("cell_anti_schrabidium", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> CELL_ANTIMATTER = ITEMS.register("cell_antimatter", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> CELL_BALEFIRE = ITEMS.register("cell_balefire", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> CELL_DEUTERIUM = ITEMS.register("cell_deuterium", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> CELL_EMPTY = ITEMS.register("cell_empty", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> CELL_PUF6 = ITEMS.register("cell_puf6", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> CELL_TRITIUM = ITEMS.register("cell_tritium", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> CELL_UF6 = ITEMS.register("cell_uf6", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> CENTRI_STICK = ITEMS.register("centri_stick", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> CHAINSAW = ITEMS.register("chainsaw", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> CHEESE = ITEMS.register("cheese", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> CHEMISTRY_SET = ITEMS.register("chemistry_set", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> CHEMISTRY_SET_BORON = ITEMS.register("chemistry_set_boron", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> CHERNOBYLSIGN = ITEMS.register("chernobylsign", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> CHLORINE_PINWHEEL = ITEMS.register("chlorine_pinwheel", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> CHLOROPHYTE_AXE = ITEMS.register("chlorophyte_axe", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> CHLOROPHYTE_PICKAXE = ITEMS.register("chlorophyte_pickaxe", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> CHOCOLATE = ITEMS.register("chocolate", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> CHOCOLATE_MILK = ITEMS.register("chocolate_milk", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> CHOPPER = ITEMS.register("chopper", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> CHOPPER_BLADES = ITEMS.register("chopper_blades", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> CHOPPER_GUN = ITEMS.register("chopper_gun", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> CHOPPER_HEAD = ITEMS.register("chopper_head", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> CHOPPER_TAIL = ITEMS.register("chopper_tail", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> CHOPPER_TORSO = ITEMS.register("chopper_torso", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> CHOPPER_WING = ITEMS.register("chopper_wing", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> CIGARETTE = ITEMS.register("cigarette", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> CINNEBAR = ITEMS.register("cinnebar", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> CIRCUIT_STAR = ITEMS.register("circuit_star", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> CLAY_TABLET = ITEMS.register("clay_tablet", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> CMB_AXE = ITEMS.register("cmb_axe", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> CMB_BOOTS = ITEMS.register("cmb_boots", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> CMB_HELMET = ITEMS.register("cmb_helmet", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> CMB_HOE = ITEMS.register("cmb_hoe", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> CMB_LEGS = ITEMS.register("cmb_legs", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> CMB_PLATE = ITEMS.register("cmb_plate", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> CMB_SHOVEL = ITEMS.register("cmb_shovel", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> CMB_SWORD = ITEMS.register("cmb_sword", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> COAL_INFERNAL = ITEMS.register("coal_infernal", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> COBALT_AXE = ITEMS.register("cobalt_axe", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> COBALT_DECORATED_AXE = ITEMS.register("cobalt_decorated_axe", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> COBALT_DECORATED_HOE = ITEMS.register("cobalt_decorated_hoe", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> COBALT_DECORATED_PICKAXE = ITEMS.register("cobalt_decorated_pickaxe", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> COBALT_DECORATED_SHOVEL = ITEMS.register("cobalt_decorated_shovel", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> COBALT_DECORATED_SWORD = ITEMS.register("cobalt_decorated_sword", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> COBALT_HOE = ITEMS.register("cobalt_hoe", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> COBALT_LEGS = ITEMS.register("cobalt_legs", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> COBALT_PICKAXE = ITEMS.register("cobalt_pickaxe", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> COBALT_PLATE = ITEMS.register("cobalt_plate", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> COBALT_SHOVEL = ITEMS.register("cobalt_shovel", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> COBALT_SWORD = ITEMS.register("cobalt_sword", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> COFFEE = ITEMS.register("coffee", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> COFFEE_RADIUM = ITEMS.register("coffee_radium", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> COIN_CREEPER = ITEMS.register("coin_creeper", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> COIN_MASKMAN = ITEMS.register("coin_maskman", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> COIN_RADIATION = ITEMS.register("coin_radiation", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> COIN_TOKEN = ITEMS.register("coin_token", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> COIN_UFO = ITEMS.register("coin_ufo", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> COIN_WORM = ITEMS.register("coin_worm", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> COMBINE_SCRAP = ITEMS.register("combine_scrap", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> COMPONENT_EMITTER = ITEMS.register("component_emitter", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> COMPONENT_LIMITER = ITEMS.register("component_limiter", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> CONTAINMENT_BOX = ITEMS.register("containment_box", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> CORDITE = ITEMS.register("cordite", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> COTTON_CANDY = ITEMS.register("cotton_candy", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> CRACKPIPE = ITEMS.register("crackpipe", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> CRATE_CALLER = ITEMS.register("crate_caller", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> CRUCIBLE_TEMPLATE = ITEMS.register("crucible_template", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> CUBE_POWER = ITEMS.register("cube_power", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> CUSTOM_AMAT = ITEMS.register("custom_amat", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> CUSTOM_DIRTY = ITEMS.register("custom_dirty", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> CUSTOM_FALL = ITEMS.register("custom_fall", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> CUSTOM_HYDRO = ITEMS.register("custom_hydro", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> CUSTOM_KIT = ITEMS.register("custom_kit", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> CUSTOM_NUKE = ITEMS.register("custom_nuke", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> CUSTOM_SCHRAB = ITEMS.register("custom_schrab", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> CUSTOM_TNT = ITEMS.register("custom_tnt", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> DEBRIS_CONCRETE = ITEMS.register("debris_concrete", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> DEBRIS_ELEMENT = ITEMS.register("debris_element", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> DEBRIS_EXCHANGER = ITEMS.register("debris_exchanger", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> DEBRIS_FUEL = ITEMS.register("debris_fuel", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> DEBRIS_GRAPHITE = ITEMS.register("debris_graphite", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> DEBRIS_METAL = ITEMS.register("debris_metal", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> DEBRIS_SHRAPNEL = ITEMS.register("debris_shrapnel", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> DEFINITELYFOOD = ITEMS.register("definitelyfood", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> DEFUSER_GOLD = ITEMS.register("defuser_gold", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> DEMON_CORE_CLOSED = ITEMS.register("demon_core_closed", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> DEMON_CORE_OPEN = ITEMS.register("demon_core_open", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> DESH_AXE = ITEMS.register("desh_axe", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> DESH_HOE = ITEMS.register("desh_hoe", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> DESH_PICKAXE = ITEMS.register("desh_pickaxe", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> DESH_SHOVEL = ITEMS.register("desh_shovel", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> DESH_SWORD = ITEMS.register("desh_sword", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> DESIGNATOR_ARTY_RANGE = ITEMS.register("designator_arty_range", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> DETONATOR_DE = ITEMS.register("detonator_de", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> DETONATOR_DEADMAN = ITEMS.register("detonator_deadman", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> DETONATOR_LASER = ITEMS.register("detonator_laser", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> DETONATOR_MULTI = ITEMS.register("detonator_multi", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> DEUTERIUM_FILTER = ITEMS.register("deuterium_filter", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> DIAMOND_GAVEL = ITEMS.register("diamond_gavel", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> DIESELSUIT_BOOTS = ITEMS.register("dieselsuit_boots", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> DIESELSUIT_HELMET = ITEMS.register("dieselsuit_helmet", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> DIESELSUIT_LEGS = ITEMS.register("dieselsuit_legs", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> DIESELSUIT_PLATE = ITEMS.register("dieselsuit_plate", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> DISPERSER_CANISTER = ITEMS.register("disperser_canister", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> DNS_BOOTS = ITEMS.register("dns_boots", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> DNS_HELMET = ITEMS.register("dns_helmet", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> DNS_LEGS = ITEMS.register("dns_legs", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> DNS_PLATE = ITEMS.register("dns_plate", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> DNT_LEGS = ITEMS.register("dnt_legs", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> DNT_PLATE = ITEMS.register("dnt_plate", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> DNT_SWORD = ITEMS.register("dnt_sword", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> DOOR_METAL = ITEMS.register("door_metal", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> DOOR_RED = ITEMS.register("door_red", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> DRAX = ITEMS.register("drax", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> DRAX_MK2 = ITEMS.register("drax_mk2", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> DRAX_MK3 = ITEMS.register("drax_mk3", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> DRILLBIT_DESH = ITEMS.register("drillbit_desh", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> DRILLBIT_DESH_DIAMOND = ITEMS.register("drillbit_desh_diamond", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> DRILLBIT_FERRO = ITEMS.register("drillbit_ferro", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> DRILLBIT_FERRO_DIAMOND = ITEMS.register("drillbit_ferro_diamond", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> DRILLBIT_HSS = ITEMS.register("drillbit_hss", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> DRILLBIT_HSS_DIAMOND = ITEMS.register("drillbit_hss_diamond", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> DRILLBIT_STEEL = ITEMS.register("drillbit_steel", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> DRILLBIT_STEEL_DIAMOND = ITEMS.register("drillbit_steel_diamond", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> DRILLBIT_TCALLOY = ITEMS.register("drillbit_tcalloy", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> DRILLBIT_TCALLOY_DIAMOND = ITEMS.register("drillbit_tcalloy_diamond", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> DRONE_LINKER = ITEMS.register("drone_linker",
            () -> new com.hbm_m.item.tools_and_armor.ItemDroneLinker(new Item.Properties()));
    public static final RegistrySupplier<Item> DRONE_PATROL = ITEMS.register("drone_patrol",
            () -> new com.hbm_m.item.tools_and_armor.ItemDrone(new Item.Properties(), false, false));
    public static final RegistrySupplier<Item> DRONE_PATROL_CHUNKLOADING = ITEMS.register("drone_patrol_chunkloading",
            () -> new com.hbm_m.item.tools_and_armor.ItemDrone(new Item.Properties(), false, true));
    public static final RegistrySupplier<Item> DRONE_PATROL_EXPRESS = ITEMS.register("drone_patrol_express",
            () -> new com.hbm_m.item.tools_and_armor.ItemDrone(new Item.Properties(), true, false));
    public static final RegistrySupplier<Item> DRONE_PATROL_EXPRESS_CHUNKLOADING = ITEMS.register("drone_patrol_express_chunkloading",
            () -> new com.hbm_m.item.tools_and_armor.ItemDrone(new Item.Properties(), true, true));
    public static final RegistrySupplier<Item> DRONE_REQUEST = ITEMS.register("drone_request",
            () -> new Item(new Item.Properties().stacksTo(64)));
    public static final RegistrySupplier<Item> DWARVEN_PICKAXE = ITEMS.register("dwarven_pickaxe", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> DYSFUNCTIONAL_REACTOR = ITEMS.register("dysfunctional_reactor", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> EGG_BALEFIRE = ITEMS.register("egg_balefire", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> EGG_BALEFIRE_SHARD = ITEMS.register("egg_balefire_shard", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> EGG_GLYPHID = ITEMS.register("egg_glyphid", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> ELEC_SHOVEL = ITEMS.register("elec_shovel", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> ELEC_SWORD = ITEMS.register("elec_sword", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> ENERGY_CORE = ITEMS.register("energy_core", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> ENTANGLEMENT_KIT = ITEMS.register("entanglement_kit", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> ENVSUIT_BOOTS = ITEMS.register("envsuit_boots", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> ENVSUIT_LEGS = ITEMS.register("envsuit_legs", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> ENVSUIT_PLATE = ITEMS.register("envsuit_plate", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> EUPHEMIUM_BOOTS = ITEMS.register("euphemium_boots", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> EUPHEMIUM_HELMET = ITEMS.register("euphemium_helmet", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> EUPHEMIUM_LEGS = ITEMS.register("euphemium_legs", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> EUPHEMIUM_PLATE = ITEMS.register("euphemium_plate", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> FAU_BOOTS = ITEMS.register("fau_boots", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> FAU_HELMET = ITEMS.register("fau_helmet", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> FAU_LEGS = ITEMS.register("fau_legs", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> FAU_PLATE = ITEMS.register("fau_plate", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> FILTER_COAL = ITEMS.register("filter_coal", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> FINS_BIG_STEEL = ITEMS.register("fins_big_steel", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> FINS_FLAT = ITEMS.register("fins_flat", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> FINS_QUAD_TITANIUM = ITEMS.register("fins_quad_titanium", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> FINS_SMALL_STEEL = ITEMS.register("fins_small_steel", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> FINS_TRI_STEEL = ITEMS.register("fins_tri_steel", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> FLAME_CONSPIRACY = ITEMS.register("flame_conspiracy", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> FLAME_OPINION = ITEMS.register("flame_opinion", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> FLAME_POLITICS = ITEMS.register("flame_politics", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> FLEIJA_CORE = ITEMS.register("fleija_core", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> FLEIJA_IGNITER = ITEMS.register("fleija_igniter", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> FLEIJA_KIT = ITEMS.register("fleija_kit", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> FLEIJA_PROPELLANT = ITEMS.register("fleija_propellant", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> FLUID_IDENTIFIER_MULTI = ITEMS.register("fluid_identifier_multi", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> FLYWHEEL_BERYLLIUM = ITEMS.register("flywheel_beryllium", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> FOODITEM = ITEMS.register("fooditem", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> FRAGMENT_ACTINIUM = ITEMS.register("fragment_actinium", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> FRAGMENT_BORON = ITEMS.register("fragment_boron", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> FRAGMENT_CERIUM = ITEMS.register("fragment_cerium", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> FRAGMENT_COBALT = ITEMS.register("fragment_cobalt", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> FRAGMENT_COLTAN = ITEMS.register("fragment_coltan", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> FRAGMENT_LANTHANIUM = ITEMS.register("fragment_lanthanium", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> FRAGMENT_METEORITE = ITEMS.register("fragment_meteorite", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> FRAGMENT_NEODYMIUM = ITEMS.register("fragment_neodymium", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> FRAGMENT_NIOBIUM = ITEMS.register("fragment_niobium", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> FUSE = ITEMS.register("fuse", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> FUSION_CORE = ITEMS.register("fusion_core", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> FUSION_CORE_INFINITE = ITEMS.register("fusion_core_infinite", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> FUSION_SHIELD_CHLOROPHYTE = ITEMS.register("fusion_shield_chlorophyte", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> FUSION_SHIELD_DESH = ITEMS.register("fusion_shield_desh", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> FUSION_SHIELD_TUNGSTEN = ITEMS.register("fusion_shield_tungsten", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> FUSION_SHIELD_VAPORWAVE = ITEMS.register("fusion_shield_vaporwave", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> GADGET_CORE = ITEMS.register("gadget_core", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> GADGET_EXPLOSIVE = ITEMS.register("gadget_explosive", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> GADGET_KIT = ITEMS.register("gadget_kit", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> GADGET_WIREING = ITEMS.register("gadget_wireing", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> GAS_MASK = ITEMS.register("gas_mask", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> GAS_MASK_FILTER = ITEMS.register("gas_mask_filter", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> GAS_MASK_FILTER_COMBO = ITEMS.register("gas_mask_filter_combo", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> GAS_MASK_FILTER_MONO = ITEMS.register("gas_mask_filter_mono", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> GAS_MASK_FILTER_PISS = ITEMS.register("gas_mask_filter_piss", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> GAS_MASK_FILTER_RAG = ITEMS.register("gas_mask_filter_rag", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> GAS_MASK_M65 = ITEMS.register("gas_mask_m65", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> GAS_MASK_MONO = ITEMS.register("gas_mask_mono", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> GAS_MASK_OLDE = ITEMS.register("gas_mask_olde", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> GAS_TESTER = ITEMS.register("gas_tester", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> GEAR_LARGE = ITEMS.register("gear_large", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> GEM_ALEXANDRITE = ITEMS.register("gem_alexandrite", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> GEM_RAD = ITEMS.register("gem_rad", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> GEM_SODALITE = ITEMS.register("gem_sodalite", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> GEM_TANTALIUM = ITEMS.register("gem_tantalium", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> GEM_VOLCANIC = ITEMS.register("gem_volcanic", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> GENERATOR_FRONT = ITEMS.register("generator_front", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> GENERATOR_STEEL = ITEMS.register("generator_steel", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> GLITCH = ITEMS.register("glitch", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> GLOWING_STEW = ITEMS.register("glowing_stew", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> GLYPHID_GLAND = ITEMS.register("glyphid_gland", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> GLYPHID_MEAT = ITEMS.register("glyphid_meat", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> GLYPHID_MEAT_GRILLED = ITEMS.register("glyphid_meat_grilled", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> GOGGLES = ITEMS.register("goggles", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> GRENADE_UNIVERSAL = ITEMS.register("grenade_universal", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> GUN_B92 = ITEMS.register("gun_b92", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> GUN_FIREEXT = ITEMS.register("gun_fireext", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> GUN_KIT_1 = ITEMS.register("gun_kit_1", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> GUN_KIT_2 = ITEMS.register("gun_kit_2", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> GUN_PA_RANGED = ITEMS.register("gun_pa_ranged", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> HAND_DRILL = ITEMS.register("hand_drill", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> HAND_DRILL_DESH = ITEMS.register("hand_drill_desh", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> HAZMAT_BOOTS_GREY = ITEMS.register("hazmat_boots_grey", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> HAZMAT_BOOTS_RED = ITEMS.register("hazmat_boots_red", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> HAZMAT_GREY_KIT = ITEMS.register("hazmat_grey_kit", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> HAZMAT_HELMET_GREY = ITEMS.register("hazmat_helmet_grey", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> HAZMAT_HELMET_RED = ITEMS.register("hazmat_helmet_red", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> HAZMAT_KIT = ITEMS.register("hazmat_kit", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> HAZMAT_LEGS = ITEMS.register("hazmat_legs", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> HAZMAT_LEGS_GREY = ITEMS.register("hazmat_legs_grey", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> HAZMAT_LEGS_RED = ITEMS.register("hazmat_legs_red", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> HAZMAT_PAA_BOOTS = ITEMS.register("hazmat_paa_boots", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> HAZMAT_PAA_HELMET = ITEMS.register("hazmat_paa_helmet", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> HAZMAT_PAA_LEGS = ITEMS.register("hazmat_paa_legs", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> HAZMAT_PAA_PLATE = ITEMS.register("hazmat_paa_plate", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> HAZMAT_PLATE = ITEMS.register("hazmat_plate", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> HAZMAT_PLATE_GREY = ITEMS.register("hazmat_plate_grey", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> HAZMAT_PLATE_RED = ITEMS.register("hazmat_plate_red", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> HAZMAT_RED_KIT = ITEMS.register("hazmat_red_kit", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> HEAVY_COMPONENT = ITEMS.register("heavy_component", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> HEV_BOOTS = ITEMS.register("hev_boots", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> HEV_HELMET = ITEMS.register("hev_helmet", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> HEV_LEGS = ITEMS.register("hev_legs", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> HEV_PLATE = ITEMS.register("hev_plate", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> HOLOTAPE_DAMAGED = ITEMS.register("holotape_damaged", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> HORSESHOE_MAGNET = ITEMS.register("horseshoe_magnet", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> HULL_BIG_ALUMINIUM = ITEMS.register("hull_big_aluminium", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> HULL_BIG_STEEL = ITEMS.register("hull_big_steel", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> HULL_BIG_TITANIUM = ITEMS.register("hull_big_titanium", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> HULL_SMALL_ALUMINIUM = ITEMS.register("hull_small_aluminium", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> HULL_SMALL_STEEL = ITEMS.register("hull_small_steel", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> ICF_PELLET = ITEMS.register("icf_pellet", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> ICF_PELLET_DEPLETED = ITEMS.register("icf_pellet_depleted", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> ICF_PELLET_EMPTY = ITEMS.register("icf_pellet_empty", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> INDUSTRIAL_MAGNET = ITEMS.register("industrial_magnet", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> INGOT_ALUMINIUM = ITEMS.register("ingot_aluminium", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> INJECTOR_5HTP = ITEMS.register("injector_5htp", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> INJECTOR_KNIFE = ITEMS.register("injector_knife", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> INK = ITEMS.register("ink", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> INSERT_DOXIUM = ITEMS.register("insert_doxium", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> INSERT_DU = ITEMS.register("insert_du", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> INSERT_ERA = ITEMS.register("insert_era", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> INSERT_ESAPI = ITEMS.register("insert_esapi", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> INSERT_GHIORSIUM = ITEMS.register("insert_ghiorsium", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> INSERT_KEVLAR = ITEMS.register("insert_kevlar", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> INSERT_POLONIUM = ITEMS.register("insert_polonium", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> INSERT_SAPI = ITEMS.register("insert_sapi", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> INSERT_STEEL = ITEMS.register("insert_steel", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> INSERT_XSAPI = ITEMS.register("insert_xsapi", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> INSERT_YHARONITE = ITEMS.register("insert_yharonite", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> IV_BLOOD = ITEMS.register("iv_blood", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> IV_EMPTY = ITEMS.register("iv_empty", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> IV_XP = ITEMS.register("iv_xp", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> IV_XP_EMPTY = ITEMS.register("iv_xp_empty", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> JACKT = ITEMS.register("jackt", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> JACKT2 = ITEMS.register("jackt2", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> JETPACK_BOOST = ITEMS.register("jetpack_boost", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> JETPACK_BREAK = ITEMS.register("jetpack_break", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> JETPACK_FLY = ITEMS.register("jetpack_fly", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> JETPACK_TANK = ITEMS.register("jetpack_tank", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> JETPACK_VECTOR = ITEMS.register("jetpack_vector", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> JOURNAL_BJ = ITEMS.register("journal_bj", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> JOURNAL_PIP = ITEMS.register("journal_pip", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> JOURNAL_SILVER = ITEMS.register("journal_silver", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> KEY = ITEMS.register("key", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> KEY_RED = ITEMS.register("key_red", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> KEY_RED_CRACKED = ITEMS.register("key_red_cracked", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> LASER_CRYSTAL_BISMUTH = ITEMS.register("laser_crystal_bismuth", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> LASER_CRYSTAL_CMB = ITEMS.register("laser_crystal_cmb", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> LASER_CRYSTAL_CO2 = ITEMS.register("laser_crystal_co2", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> LASER_CRYSTAL_DIGAMMA = ITEMS.register("laser_crystal_digamma", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> LASER_CRYSTAL_DNT = ITEMS.register("laser_crystal_dnt", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> LAUNCH_CODE = ITEMS.register("launch_code", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> LAUNCH_CODE_PIECE = ITEMS.register("launch_code_piece", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> LAUNCH_KEY = ITEMS.register("launch_key", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> LEAD_GAVEL = ITEMS.register("lead_gavel", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> LEMON = ITEMS.register("lemon", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> LINKER = ITEMS.register("linker", () -> new com.hbm_m.item.ItemTeleLink(new Item.Properties().stacksTo(1)));
    public static final RegistrySupplier<Item> LIQUIDATOR_LEGS = ITEMS.register("liquidator_legs", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> LIQUIDATOR_PLATE = ITEMS.register("liquidator_plate", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> LITHIUM = ITEMS.register("lithium", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> LODESTONE = ITEMS.register("lodestone", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> LOOP_STEW = ITEMS.register("loop_stew", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> LOOPS = ITEMS.register("loops", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> LOOT_10 = ITEMS.register("loot_10", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> LOOT_15 = ITEMS.register("loot_15", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> LOOT_MISC = ITEMS.register("loot_misc", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> MAN_CORE = ITEMS.register("man_core", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> MAN_IGNITER = ITEMS.register("man_igniter", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> MAN_KIT = ITEMS.register("man_kit", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> MARSHMALLOW = ITEMS.register("marshmallow", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> MASK_OF_INFAMY = ITEMS.register("mask_of_infamy", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> MASK_PISS = ITEMS.register("mask_piss", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> MASK_RAG = ITEMS.register("mask_rag", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> MATCHSTICK = ITEMS.register("matchstick", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> MECH_KEY = ITEMS.register("mech_key", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> MED_BAG = ITEMS.register("med_bag", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> MED_IPECAC = ITEMS.register("med_ipecac", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> MED_PTSD = ITEMS.register("med_ptsd", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> MEDAL_LIQUIDATOR = ITEMS.register("medal_liquidator", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> MELTDOWN_TOOL = ITEMS.register("meltdown_tool", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> MEMESPOON = ITEMS.register("memespoon", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> MESE_AXE = ITEMS.register("mese_axe", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> MESE_GAVEL = ITEMS.register("mese_gavel", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> MESE_PICKAXE = ITEMS.register("mese_pickaxe", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> METEOR_CHARM = ITEMS.register("meteor_charm", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> METEOR_REMOTE = ITEMS.register("meteor_remote", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> MIKE_COOLING_UNIT = ITEMS.register("mike_cooling_unit", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> MIKE_CORE = ITEMS.register("mike_core", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> MIKE_DEUT = ITEMS.register("mike_deut", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> MIKE_KIT = ITEMS.register("mike_kit", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> MIRROR_TOOL = ITEMS.register("mirror_tool", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> MISSILE_ANTI_BALLISTIC = ITEMS.register("missile_anti_ballistic", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> MISSILE_CARRIER = ITEMS.register("missile_carrier", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> MISSILE_CUSTOM = ITEMS.register("missile_custom", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> MISSILE_ENDO = ITEMS.register("missile_endo", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> MISSILE_EXO = ITEMS.register("missile_exo", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> MISSILE_KIT = ITEMS.register("missile_kit", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> MORNING_GLORY = ITEMS.register("morning_glory", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> MP_C_1 = ITEMS.register("mp_c_1", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> MP_C_2 = ITEMS.register("mp_c_2", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> MP_C_3 = ITEMS.register("mp_c_3", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> MP_C_4 = ITEMS.register("mp_c_4", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> MP_C_5 = ITEMS.register("mp_c_5", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> MUCHO_MANGO = ITEMS.register("mucho_mango", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> MULTI_KIT = ITEMS.register("multi_kit", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> N2_CHARGE = ITEMS.register("n2_charge", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> NEUTRINO_LENS = ITEMS.register("neutrino_lens", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> NIGHT_VISION = ITEMS.register("night_vision", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> NITRA = ITEMS.register("nitra", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> NITRA_SMALL = ITEMS.register("nitra_small", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> NO9 = ITEMS.register("no9", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> NOTHING = ITEMS.register("nothing", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> NUCLEAR_WASTE = ITEMS.register("nuclear_waste", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> NUCLEAR_WASTE_LONG = ITEMS.register("nuclear_waste_long", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> NUCLEAR_WASTE_LONG_DEPLETED = ITEMS.register("nuclear_waste_long_depleted", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> NUCLEAR_WASTE_PEARL = ITEMS.register("nuclear_waste_pearl", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> NUCLEAR_WASTE_SHORT = ITEMS.register("nuclear_waste_short", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> NUCLEAR_WASTE_SHORT_DEPLETED = ITEMS.register("nuclear_waste_short_depleted", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> NUCLEAR_WASTE_VITRIFIED = ITEMS.register("nuclear_waste_vitrified", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> NUGGET = ITEMS.register("nugget", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> NUGGET_ACTINIUM = ITEMS.register("nugget_actinium", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> NUGGET_AM241 = ITEMS.register("nugget_am241", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> NUGGET_AM242 = ITEMS.register("nugget_am242", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> NUGGET_AM_MIX = ITEMS.register("nugget_am_mix", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> NUGGET_AMERICIUM_FUEL = ITEMS.register("nugget_americium_fuel", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> NUGGET_ARSENIC = ITEMS.register("nugget_arsenic", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> NUGGET_AU198 = ITEMS.register("nugget_au198", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> NUGGET_AUSTRALIUM = ITEMS.register("nugget_australium", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> NUGGET_AUSTRALIUM_GREATER = ITEMS.register("nugget_australium_greater", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> NUGGET_AUSTRALIUM_LESSER = ITEMS.register("nugget_australium_lesser", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> NUGGET_BERYLLIUM = ITEMS.register("nugget_beryllium", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> NUGGET_BISMUTH = ITEMS.register("nugget_bismuth", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> NUGGET_CO60 = ITEMS.register("nugget_co60", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> NUGGET_COBALT = ITEMS.register("nugget_cobalt", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> NUGGET_DESH = ITEMS.register("nugget_desh", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> NUGGET_DINEUTRONIUM = ITEMS.register("nugget_dineutronium", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> NUGGET_EUPHEMIUM = ITEMS.register("nugget_euphemium", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> NUGGET_GH336 = ITEMS.register("nugget_gh336", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> NUGGET_HES = ITEMS.register("nugget_hes", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> NUGGET_LEAD = ITEMS.register("nugget_lead", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> NUGGET_LES = ITEMS.register("nugget_les", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> NUGGET_MERCURY = ITEMS.register("nugget_mercury", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> NUGGET_MOX_FUEL = ITEMS.register("nugget_mox_fuel", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> NUGGET_NEPTUNIUM = ITEMS.register("nugget_neptunium", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> NUGGET_NEPTUNIUM_FUEL = ITEMS.register("nugget_neptunium_fuel", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> NUGGET_NIOBIUM = ITEMS.register("nugget_niobium", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> NUGGET_OSMIRIDIUM = ITEMS.register("nugget_osmiridium", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> NUGGET_PB209 = ITEMS.register("nugget_pb209", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> NUGGET_PLUTONIUM = ITEMS.register("nugget_plutonium", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> NUGGET_PLUTONIUM_FUEL = ITEMS.register("nugget_plutonium_fuel", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> NUGGET_POLONIUM = ITEMS.register("nugget_polonium", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> NUGGET_PU238 = ITEMS.register("nugget_pu238", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> NUGGET_PU239 = ITEMS.register("nugget_pu239", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> NUGGET_PU240 = ITEMS.register("nugget_pu240", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> NUGGET_PU241 = ITEMS.register("nugget_pu241", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> NUGGET_PU_MIX = ITEMS.register("nugget_pu_mix", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> NUGGET_RA226 = ITEMS.register("nugget_ra226", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> NUGGET_SCHRABIDIUM = ITEMS.register("nugget_schrabidium", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> NUGGET_SCHRABIDIUM_FUEL = ITEMS.register("nugget_schrabidium_fuel", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> NUGGET_SOLINIUM = ITEMS.register("nugget_solinium", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> NUGGET_SR90 = ITEMS.register("nugget_sr90", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> NUGGET_TECHNETIUM = ITEMS.register("nugget_technetium", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> NUGGET_TH232 = ITEMS.register("nugget_th232", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> NUGGET_THORIUM_FUEL = ITEMS.register("nugget_thorium_fuel", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> NUGGET_U233 = ITEMS.register("nugget_u233", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> NUGGET_U235 = ITEMS.register("nugget_u235", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> NUGGET_U238 = ITEMS.register("nugget_u238", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> NUGGET_URANIUM = ITEMS.register("nugget_uranium", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> NUGGET_URANIUM_FUEL = ITEMS.register("nugget_uranium_fuel", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> NUGGET_ZIRCONIUM = ITEMS.register("nugget_zirconium", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> NUKE_ADVANCED_KIT = ITEMS.register("nuke_advanced_kit", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> NUKE_COMMERCIALLY_KIT = ITEMS.register("nuke_commercially_kit", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> NUKE_ELECTRIC_KIT = ITEMS.register("nuke_electric_kit", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> NUKE_STARTER_KIT = ITEMS.register("nuke_starter_kit", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> ORE_BEDROCK = ITEMS.register("ore_bedrock", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> ORE_CENTRIFUGED = ITEMS.register("ore_centrifuged", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> ORE_CLEANED = ITEMS.register("ore_cleaned", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> ORE_DEEPCLEANED = ITEMS.register("ore_deepcleaned", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> ORE_DENSITY_SCANNER = ITEMS.register("ore_density_scanner", () -> new com.hbm_m.item.tool.ItemOreDensityScanner(new Item.Properties()));
    public static final RegistrySupplier<Item> ORE_ENRICHED = ITEMS.register("ore_enriched", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> ORE_NITRATED = ITEMS.register("ore_nitrated", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> ORE_NITROCRYSTALLINE = ITEMS.register("ore_nitrocrystalline", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> ORE_PURIFIED = ITEMS.register("ore_purified", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> ORE_RADCLEANED = ITEMS.register("ore_radcleaned", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> ORE_SEARED = ITEMS.register("ore_seared", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> ORE_SEPARATED = ITEMS.register("ore_separated", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> OVERFUSE = ITEMS.register("overfuse", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> PAA_LEGS = ITEMS.register("paa_legs", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> PAA_PLATE = ITEMS.register("paa_plate", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> PADLOCK = ITEMS.register("padlock", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> PADLOCK_REINFORCED = ITEMS.register("padlock_reinforced", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> PADLOCK_RUSTY = ITEMS.register("padlock_rusty", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> PADLOCK_UNBREAKABLE = ITEMS.register("padlock_unbreakable", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> PADS_RUBBER = ITEMS.register("pads_rubber", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> PADS_SLIME = ITEMS.register("pads_slime", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> PADS_STATIC = ITEMS.register("pads_static", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> PANCAKE = ITEMS.register("pancake", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> PART_BARREL_HEAVY = ITEMS.register("part_barrel_heavy", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> PART_BARREL_LIGHT = ITEMS.register("part_barrel_light", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> PART_GRIP = ITEMS.register("part_grip", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> PART_MECHANISM = ITEMS.register("part_mechanism", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> PART_RECEIVER_HEAVY = ITEMS.register("part_receiver_heavy", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> PART_RECEIVER_LIGHT = ITEMS.register("part_receiver_light", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> PART_STOCK = ITEMS.register("part_stock", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> PARTICLE_AMAT = ITEMS.register("particle_amat", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> PARTICLE_ASCHRAB = ITEMS.register("particle_aschrab", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> PARTICLE_COPPER = ITEMS.register("particle_copper", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> PARTICLE_DARK = ITEMS.register("particle_dark", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> PARTICLE_DIGAMMA = ITEMS.register("particle_digamma", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> PARTICLE_EMPTY = ITEMS.register("particle_empty", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> PARTICLE_HIGGS = ITEMS.register("particle_higgs", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> PARTICLE_HYDROGEN = ITEMS.register("particle_hydrogen", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> PARTICLE_LEAD = ITEMS.register("particle_lead", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> PARTICLE_LUTECE = ITEMS.register("particle_lutece", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> PARTICLE_MUON = ITEMS.register("particle_muon", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> PARTICLE_SPARKTICLE = ITEMS.register("particle_sparkticle", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> PARTICLE_STRANGE = ITEMS.register("particle_strange", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> PARTICLE_TACHYON = ITEMS.register("particle_tachyon", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> PARTS_LEGENDARY = ITEMS.register("parts_legendary", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> PEAS = ITEMS.register("peas", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> PEDESTAL_STEEL = ITEMS.register("pedestal_steel", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> PELLET_CLUSTER = ITEMS.register("pellet_cluster", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> PELLET_GAS = ITEMS.register("pellet_gas", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> PELLET_RTG = ITEMS.register("pellet_rtg", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> PELLET_RTG_ACTINIUM = ITEMS.register("pellet_rtg_actinium", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> PELLET_RTG_AMERICIUM = ITEMS.register("pellet_rtg_americium", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> PELLET_RTG_BERKELIUM = ITEMS.register("pellet_rtg_berkelium", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> PELLET_RTG_COBALT = ITEMS.register("pellet_rtg_cobalt", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> PELLET_RTG_GOLD = ITEMS.register("pellet_rtg_gold", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> PELLET_RTG_LEAD = ITEMS.register("pellet_rtg_lead", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> PELLET_RTG_POLONIUM = ITEMS.register("pellet_rtg_polonium", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> PELLET_RTG_RADIUM = ITEMS.register("pellet_rtg_radium", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> PELLET_RTG_STRONTIUM = ITEMS.register("pellet_rtg_strontium", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> PELLET_RTG_WEAK = ITEMS.register("pellet_rtg_weak", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> PHOTO_PANEL = ITEMS.register("photo_panel", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> PILE_ROD_BORON = ITEMS.register("pile_rod_boron", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> PILE_ROD_DETECTOR = ITEMS.register("pile_rod_detector", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> PILE_ROD_LITHIUM = ITEMS.register("pile_rod_lithium", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> PILE_ROD_PLUTONIUM = ITEMS.register("pile_rod_plutonium", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> PILE_ROD_PU239 = ITEMS.register("pile_rod_pu239", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> PILE_ROD_SOURCE = ITEMS.register("pile_rod_source", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> PILE_ROD_URANIUM = ITEMS.register("pile_rod_uranium", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> PILL_HERBAL = ITEMS.register("pill_herbal", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> PILL_IODINE = ITEMS.register("pill_iodine", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> PILL_RED = ITEMS.register("pill_red", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> PIN = ITEMS.register("pin", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> PIPES_STEEL = ITEMS.register("pipes_steel", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> PIPETTE = ITEMS.register("pipette", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> PIPETTE_BORON = ITEMS.register("pipette_boron", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> PIPETTE_LABORATORY = ITEMS.register("pipette_laboratory", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> PISTON_SELENIUM = ITEMS.register("piston_selenium", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> PISTON_SET_DESH = ITEMS.register("piston_set_desh", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> PISTON_SET_DURA = ITEMS.register("piston_set_dura", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> PISTON_SET_STARMETAL = ITEMS.register("piston_set_starmetal", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> PISTON_SET_STEEL = ITEMS.register("piston_set_steel", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> PLAN_C = ITEMS.register("plan_c", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> PLASTIC_BAG = ITEMS.register("plastic_bag", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> PLATE_ALUMINIUM = ITEMS.register("plate_aluminium", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> PLATE_POLYMER = ITEMS.register("plate_polymer", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> POLAROID = ITEMS.register("polaroid", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> POLLUTION_DETECTOR = ITEMS.register("pollution_detector", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> POWER_NET_TOOL = ITEMS.register("power_net_tool", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> PROTECTION_CHARM = ITEMS.register("protection_charm", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> PROTOTYPE_KIT = ITEMS.register("prototype_kit", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> PUDDING = ITEMS.register("pudding", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> PWR_PRINTER = ITEMS.register("pwr_printer", () -> new Item(new Item.Properties()));

    // PWR reactor fuel - see com.hbm_m.item.nuclear.PWRFuelType for the mechanics.
    public static final RegistrySupplier<Item> PWR_FUEL_MEU = ITEMS.register("pwr_fuel_meu",
        () -> new com.hbm_m.item.nuclear.PWRFuelItem(new Item.Properties(), com.hbm_m.item.nuclear.PWRFuelType.MEU));
    public static final RegistrySupplier<Item> PWR_FUEL_MEU_HOT = ITEMS.register("pwr_fuel_meu_hot",
        () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> PWR_FUEL_HEU = ITEMS.register("pwr_fuel_heu",
        () -> new com.hbm_m.item.nuclear.PWRFuelItem(new Item.Properties(), com.hbm_m.item.nuclear.PWRFuelType.HEU));
    public static final RegistrySupplier<Item> PWR_FUEL_HEU_HOT = ITEMS.register("pwr_fuel_heu_hot",
        () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> PWR_FUEL_MOX = ITEMS.register("pwr_fuel_mox",
        () -> new com.hbm_m.item.nuclear.PWRFuelItem(new Item.Properties(), com.hbm_m.item.nuclear.PWRFuelType.MOX));
    public static final RegistrySupplier<Item> PWR_FUEL_MOX_HOT = ITEMS.register("pwr_fuel_mox_hot",
        () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> PWR_FUEL_HEP = ITEMS.register("pwr_fuel_hep",
        () -> new com.hbm_m.item.nuclear.PWRFuelItem(new Item.Properties(), com.hbm_m.item.nuclear.PWRFuelType.HEP));
    public static final RegistrySupplier<Item> PWR_FUEL_HEP_HOT = ITEMS.register("pwr_fuel_hep_hot",
        () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> PWR_FUEL_SCHRABIDIUM = ITEMS.register("pwr_fuel_schrabidium",
        () -> new com.hbm_m.item.nuclear.PWRFuelItem(new Item.Properties(), com.hbm_m.item.nuclear.PWRFuelType.SCHRABIDIUM));
    public static final RegistrySupplier<Item> PWR_FUEL_SCHRABIDIUM_HOT = ITEMS.register("pwr_fuel_schrabidium_hot",
        () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> QUARTZ_PLUTONIUM = ITEMS.register("quartz_plutonium", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> RADAR_LINKER = ITEMS.register("radar_linker", () -> new com.hbm_m.item.tool.ItemRadarLinker(new Item.Properties()));
    public static final RegistrySupplier<Item> RAG = ITEMS.register("rag", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> RAG_DAMP = ITEMS.register("rag_damp", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> RAG_PISS = ITEMS.register("rag_piss", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> RBMK_FUEL_BALEFIRE = ITEMS.register("rbmk_fuel_balefire", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> RBMK_FUEL_BALEFIRE_GOLD = ITEMS.register("rbmk_fuel_balefire_gold", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> RBMK_FUEL_FLASHLEAD = ITEMS.register("rbmk_fuel_flashlead", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> RBMK_FUEL_HEA241 = ITEMS.register("rbmk_fuel_hea241", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> RBMK_FUEL_HEA242 = ITEMS.register("rbmk_fuel_hea242", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> RBMK_FUEL_HEAUS = ITEMS.register("rbmk_fuel_heaus", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> RBMK_FUEL_HEN = ITEMS.register("rbmk_fuel_hen", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> RBMK_FUEL_HEP_ALT = ITEMS.register("rbmk_fuel_hep", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> RBMK_FUEL_HEP241 = ITEMS.register("rbmk_fuel_hep241", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> RBMK_FUEL_HES = ITEMS.register("rbmk_fuel_hes", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> RBMK_FUEL_HEU233 = ITEMS.register("rbmk_fuel_heu233", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> RBMK_FUEL_LEA = ITEMS.register("rbmk_fuel_lea", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> RBMK_FUEL_LEAUS = ITEMS.register("rbmk_fuel_leaus", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> RBMK_FUEL_LES = ITEMS.register("rbmk_fuel_les", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> RBMK_FUEL_MEA = ITEMS.register("rbmk_fuel_mea", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> RBMK_FUEL_MEN = ITEMS.register("rbmk_fuel_men", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> RBMK_FUEL_MEP = ITEMS.register("rbmk_fuel_mep", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> RBMK_FUEL_MES = ITEMS.register("rbmk_fuel_mes", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> RBMK_FUEL_MEU = ITEMS.register("rbmk_fuel_meu", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> RBMK_FUEL_PO210BE = ITEMS.register("rbmk_fuel_po210be", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> RBMK_FUEL_PU238BE = ITEMS.register("rbmk_fuel_pu238be", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> RBMK_FUEL_RA226BE = ITEMS.register("rbmk_fuel_ra226be", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> RBMK_FUEL_THMEU = ITEMS.register("rbmk_fuel_thmeu", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> RBMK_FUEL_UEU = ITEMS.register("rbmk_fuel_ueu", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> RBMK_FUEL_UZH = ITEMS.register("rbmk_fuel_uzh", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> RBMK_FUEL_ZFB_AM_MIX = ITEMS.register("rbmk_fuel_zfb_am_mix", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> RBMK_FUEL_ZFB_BISMUTH = ITEMS.register("rbmk_fuel_zfb_bismuth", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> RBMK_FUEL_ZFB_PU241 = ITEMS.register("rbmk_fuel_zfb_pu241", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> RBMK_PELLET_BALEFIRE = ITEMS.register("rbmk_pellet_balefire", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> RBMK_PELLET_BALEFIRE_GOLD = ITEMS.register("rbmk_pellet_balefire_gold", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> RBMK_PELLET_DRX = ITEMS.register("rbmk_pellet_drx", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> RBMK_PELLET_FLASHLEAD = ITEMS.register("rbmk_pellet_flashlead", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> RBMK_PELLET_HEA241 = ITEMS.register("rbmk_pellet_hea241", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> RBMK_PELLET_HEA242 = ITEMS.register("rbmk_pellet_hea242", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> RBMK_PELLET_HEAUS = ITEMS.register("rbmk_pellet_heaus", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> RBMK_PELLET_HEN = ITEMS.register("rbmk_pellet_hen", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> RBMK_PELLET_HEP241 = ITEMS.register("rbmk_pellet_hep241", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> RBMK_PELLET_HES = ITEMS.register("rbmk_pellet_hes", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> RBMK_PELLET_HEU233 = ITEMS.register("rbmk_pellet_heu233", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> RBMK_PELLET_LEA = ITEMS.register("rbmk_pellet_lea", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> RBMK_PELLET_LEAUS = ITEMS.register("rbmk_pellet_leaus", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> RBMK_PELLET_LES = ITEMS.register("rbmk_pellet_les", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> RBMK_PELLET_MEA = ITEMS.register("rbmk_pellet_mea", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> RBMK_PELLET_MEN = ITEMS.register("rbmk_pellet_men", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> RBMK_PELLET_MEP = ITEMS.register("rbmk_pellet_mep", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> RBMK_PELLET_MES = ITEMS.register("rbmk_pellet_mes", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> RBMK_PELLET_MEU = ITEMS.register("rbmk_pellet_meu", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> RBMK_PELLET_PO210BE = ITEMS.register("rbmk_pellet_po210be", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> RBMK_PELLET_PU238BE = ITEMS.register("rbmk_pellet_pu238be", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> RBMK_PELLET_RA226BE = ITEMS.register("rbmk_pellet_ra226be", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> RBMK_PELLET_THMEU = ITEMS.register("rbmk_pellet_thmeu", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> RBMK_PELLET_UEU = ITEMS.register("rbmk_pellet_ueu", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> RBMK_PELLET_UZH = ITEMS.register("rbmk_pellet_uzh", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> RBMK_PELLET_ZFB_AM_MIX = ITEMS.register("rbmk_pellet_zfb_am_mix", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> RBMK_PELLET_ZFB_BISMUTH = ITEMS.register("rbmk_pellet_zfb_bismuth", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> RBMK_PELLET_ZFB_PU241 = ITEMS.register("rbmk_pellet_zfb_pu241", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> RBMK_TOOL = ITEMS.register("rbmk_tool", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> REACHER = ITEMS.register("reacher", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> REACTOR_CORE = ITEMS.register("reactor_core", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> REACTOR_SENSOR = ITEMS.register("reactor_sensor", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> REBAR_PLACER = ITEMS.register("rebar_placer", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> REDSTONE_SWORD = ITEMS.register("redstone_sword", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> RING_PULL = ITEMS.register("ring_pull", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> RING_STARMETAL = ITEMS.register("ring_starmetal", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> ROBES_BOOTS = ITEMS.register("robes_boots", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> ROBES_HELMET = ITEMS.register("robes_helmet", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> ROBES_LEGS = ITEMS.register("robes_legs", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> ROBES_PLATE = ITEMS.register("robes_plate", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> ROCKET_FUEL = ITEMS.register("rocket_fuel", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> ROD_DUAL_EMPTY = ITEMS.register("rod_dual_empty", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> ROD_EMPTY = ITEMS.register("rod_empty", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> ROD_OF_DISCORD = ITEMS.register("rod_of_discord", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> ROD_QUAD_EMPTY = ITEMS.register("rod_quad_empty", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> RPA_BOOTS = ITEMS.register("rpa_boots", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> RPA_HELMET = ITEMS.register("rpa_helmet", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> RPA_LEGS = ITEMS.register("rpa_legs", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> RPA_PLATE = ITEMS.register("rpa_plate", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> RTG_UNIT = ITEMS.register("rtg_unit", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> RTTY_PAGER = ITEMS.register("rtty_pager", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> RUNE_BLANK = ITEMS.register("rune_blank", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> RUNE_DAGAZ = ITEMS.register("rune_dagaz", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> RUNE_HAGALAZ = ITEMS.register("rune_hagalaz", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> RUNE_ISA = ITEMS.register("rune_isa", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> RUNE_JERA = ITEMS.register("rune_jera", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> RUNE_THURISAZ = ITEMS.register("rune_thurisaz", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> SAFETY_FUSE = ITEMS.register("safety_fuse", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> SAT_CHIP = ITEMS.register("sat_chip", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> SAT_COORD = ITEMS.register("sat_coord", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> SAT_DESIGNATOR = ITEMS.register("sat_designator",
            () -> new com.hbm_m.item.designator.ItemSatDesignator(new Item.Properties()));
    public static final RegistrySupplier<Item> SAT_GERALD = ITEMS.register("sat_gerald",
            () -> new com.hbm_m.item.satellite.ItemSatChip(new Item.Properties()));
    public static final RegistrySupplier<Item> SAT_HEAD_SCANNER = ITEMS.register("sat_head_scanner", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> SAT_INTERFACE = ITEMS.register("sat_interface", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> SAT_LUNAR_MINER = ITEMS.register("sat_lunar_miner", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> SAT_MINER = ITEMS.register("sat_miner", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> SAT_RELAY = ITEMS.register("sat_relay", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> SAWBLADE = ITEMS.register("sawblade", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> SCHNITZEL_VEGAN = ITEMS.register("schnitzel_vegan", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> SCHRABIDIUM_AXE = ITEMS.register("schrabidium_axe", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> SCHRABIDIUM_BOOTS = ITEMS.register("schrabidium_boots", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> SCHRABIDIUM_HAMMER = ITEMS.register("schrabidium_hammer", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> SCHRABIDIUM_HELMET = ITEMS.register("schrabidium_helmet", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> SCHRABIDIUM_HOE = ITEMS.register("schrabidium_hoe", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> SCHRABIDIUM_LEGS = ITEMS.register("schrabidium_legs", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> SCHRABIDIUM_PICKAXE = ITEMS.register("schrabidium_pickaxe", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> SCHRABIDIUM_PLATE = ITEMS.register("schrabidium_plate", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> SCHRABIDIUM_SHOVEL = ITEMS.register("schrabidium_shovel", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> SCHRABIDIUM_SWORD = ITEMS.register("schrabidium_sword", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> SCRAP_NUCLEAR = ITEMS.register("scrap_nuclear", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> SCRAP_OIL = ITEMS.register("scrap_oil", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> SCRAP_PLASTIC = ITEMS.register("scrap_plastic", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> SCRAPS = ITEMS.register("scraps", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> SCREWDRIVER_DESH = ITEMS.register("screwdriver_desh", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> SCRUMPY = ITEMS.register("scrumpy", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> SECURITY_LEGS = ITEMS.register("security_legs", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> SECURITY_PLATE = ITEMS.register("security_plate", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> SEG_10 = ITEMS.register("seg_10", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> SEG_15 = ITEMS.register("seg_15", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> SEG_20 = ITEMS.register("seg_20", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> SERUM = ITEMS.register("serum", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> SERVO_SET = ITEMS.register("servo_set", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> SERVO_SET_DESH = ITEMS.register("servo_set_desh", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> SETTINGS_TOOL = ITEMS.register("settings_tool", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> SHACKLES = ITEMS.register("shackles", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> SHIMMER_AXE = ITEMS.register("shimmer_axe", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> SHIMMER_AXE_HEAD = ITEMS.register("shimmer_axe_head", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> SHIMMER_HANDLE = ITEMS.register("shimmer_handle", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> SHIMMER_HEAD = ITEMS.register("shimmer_head", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> SHIMMER_SLEDGE = ITEMS.register("shimmer_sledge", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> SINGULARITY = ITEMS.register("singularity", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> SIOX = ITEMS.register("siox", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> SIPHON = ITEMS.register("siphon", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> SMASHING_HAMMER = ITEMS.register("smashing_hammer", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> SOLID_FUEL = ITEMS.register("solid_fuel", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> SOLID_FUEL_BF = ITEMS.register("solid_fuel_bf", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> SOLID_FUEL_PRESTO = ITEMS.register("solid_fuel_presto", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> SOLID_FUEL_PRESTO_BF = ITEMS.register("solid_fuel_presto_bf", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> SOLID_FUEL_PRESTO_TRIPLET = ITEMS.register("solid_fuel_presto_triplet", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> SOLID_FUEL_PRESTO_TRIPLET_BF = ITEMS.register("solid_fuel_presto_triplet_bf", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> SOLINIUM_CORE = ITEMS.register("solinium_core", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> SOLINIUM_IGNITER = ITEMS.register("solinium_igniter", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> SOLINIUM_KIT = ITEMS.register("solinium_kit", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> SOLINIUM_PROPELLANT = ITEMS.register("solinium_propellant", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> SOPSIGN = ITEMS.register("sopsign", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> SPAWN_DUCK = ITEMS.register("spawn_duck", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> SPAWN_UFO = ITEMS.register("spawn_ufo", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> SPAWN_WORM = ITEMS.register("spawn_worm", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> SPHERE_STEEL = ITEMS.register("sphere_steel", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> SPIDER_MILK = ITEMS.register("spider_milk", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> SPONGEBOB_MACARONI = ITEMS.register("spongebob_macaroni", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> STAMP_357 = ITEMS.register("stamp_357", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> STAMP_44 = ITEMS.register("stamp_44", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> STAMP_50 = ITEMS.register("stamp_50", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> STAMP_9 = ITEMS.register("stamp_9", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> STARMETAL_LEGS = ITEMS.register("starmetal_legs", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> STARMETAL_PLATE = ITEMS.register("starmetal_plate", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> STATIC_SANDWICH = ITEMS.register("static_sandwich", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> STEALTH_BOY = ITEMS.register("stealth_boy", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> STEAMSUIT_BOOTS = ITEMS.register("steamsuit_boots", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> STEAMSUIT_HELMET = ITEMS.register("steamsuit_helmet", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> STEAMSUIT_LEGS = ITEMS.register("steamsuit_legs", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> STEAMSUIT_PLATE = ITEMS.register("steamsuit_plate", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> STEEL_LEGS = ITEMS.register("steel_legs", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> STEEL_PLATE = ITEMS.register("steel_plate", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> STICK_C4 = ITEMS.register("stick_c4", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> STICK_DYNAMITE = ITEMS.register("stick_dynamite", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> STICK_DYNAMITE_FISHING = ITEMS.register("stick_dynamite_fishing", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> STICK_SEMTEX = ITEMS.register("stick_semtex", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> STICK_TNT = ITEMS.register("stick_tnt", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> STOPSIGN = ITEMS.register("stopsign", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> STRUCTURE_CUSTOMMACHINE = ITEMS.register("structure_custommachine", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> SURVEY_SCANNER = ITEMS.register("survey_scanner", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> SYRINGE_ANTIDOTE = ITEMS.register("syringe_antidote", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> SYRINGE_AWESOME = ITEMS.register("syringe_awesome", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> SYRINGE_EMPTY = ITEMS.register("syringe_empty", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> SYRINGE_METAL_EMPTY = ITEMS.register("syringe_metal_empty", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> SYRINGE_METAL_MEDX = ITEMS.register("syringe_metal_medx", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> SYRINGE_METAL_PSYCHO = ITEMS.register("syringe_metal_psycho", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> SYRINGE_METAL_STIMPAK = ITEMS.register("syringe_metal_stimpak", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> SYRINGE_METAL_SUPER = ITEMS.register("syringe_metal_super", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> SYRINGE_MKUNICORN = ITEMS.register("syringe_mkunicorn", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> SYRINGE_POISON = ITEMS.register("syringe_poison", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> SYRINGE_TAINT = ITEMS.register("syringe_taint", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> TANK_STEEL = ITEMS.register("tank_steel", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> TAURUN_BOOTS = ITEMS.register("taurun_boots", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> TAURUN_HELMET = ITEMS.register("taurun_helmet", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> TAURUN_LEGS = ITEMS.register("taurun_legs", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> TAURUN_PLATE = ITEMS.register("taurun_plate", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> TEM_FLAKES = ITEMS.register("tem_flakes", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> THERMO_ELEMENT = ITEMS.register("thermo_element", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> THRUSTER_NUCLEAR = ITEMS.register("thruster_nuclear", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> TITANIUM_FILTER = ITEMS.register("titanium_filter", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> TITANIUM_LEGS = ITEMS.register("titanium_legs", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> TITANIUM_PLATE = ITEMS.register("titanium_plate", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> TRENCHMASTER_BOOTS = ITEMS.register("trenchmaster_boots", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> TRENCHMASTER_HELMET = ITEMS.register("trenchmaster_helmet", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> TRENCHMASTER_LEGS = ITEMS.register("trenchmaster_legs", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> TRENCHMASTER_PLATE = ITEMS.register("trenchmaster_plate", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> TRINITITE = ITEMS.register("trinitite", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> TSAR_CORE = ITEMS.register("tsar_core", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> TSAR_KIT = ITEMS.register("tsar_kit", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> TURBINE_TUNGSTEN = ITEMS.register("turbine_tungsten", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> TURRET_CHIP = ITEMS.register("turret_chip", () -> new Item(new Item.Properties()));
    /** Alte MVP-Platzhaltermunition, wird von keinem Turret mehr direkt verwendet (jeder Typ hat jetzt eigene Munition). */
    public static final RegistrySupplier<Item> TURRET_AMMO = ITEMS.register("turret_ammo", () -> new Item(new Item.Properties()));
    /** 9mm-Pistolenmunition fuer den Sentry-Turret (Original: {@code XFactory9mm}). */
    public static final RegistrySupplier<Item> AMMO_9MM_SP = ITEMS.register("ammo_9mm_sp", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> AMMO_9MM_FMJ = ITEMS.register("ammo_9mm_fmj", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> AMMO_9MM_JHP = ITEMS.register("ammo_9mm_jhp", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> AMMO_9MM_AP = ITEMS.register("ammo_9mm_ap", () -> new Item(new Item.Properties()));
    /** .50 BMG-Munition fuer den Chekhov-Turret (Original: {@code XFactory50}). */
    public static final RegistrySupplier<Item> AMMO_50_SP = ITEMS.register("ammo_50_sp", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> AMMO_50_FMJ = ITEMS.register("ammo_50_fmj", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> AMMO_50_JHP = ITEMS.register("ammo_50_jhp", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> AMMO_50_AP = ITEMS.register("ammo_50_ap", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> AMMO_50_DU = ITEMS.register("ammo_50_du", () -> new Item(new Item.Properties()));
    /** 5.56mm-Munition fuer den Friendly-Turret (Original: {@code XFactory556mm}). */
    public static final RegistrySupplier<Item> AMMO_556_SP = ITEMS.register("ammo_556_sp", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> AMMO_556_FMJ = ITEMS.register("ammo_556_fmj", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> AMMO_556_JHP = ITEMS.register("ammo_556_jhp", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> AMMO_556_AP = ITEMS.register("ammo_556_ap", () -> new Item(new Item.Properties()));
    /** Gelenkte Rakete fuer den Richard-Turret (Original: {@code XFactoryRocket.rocket_ml}). */
    public static final RegistrySupplier<Item> ROCKET_TURRET_STANDARD = ITEMS.register("rocket_turret_standard", () -> new Item(new Item.Properties()));
    /** Gelenkte Raketenvarianten fuer den Himars-Turret (Original: {@code ItemAmmoHIMARS}). */
    public static final RegistrySupplier<Item> ROCKET_HIMARS_STANDARD = ITEMS.register("rocket_himars_standard", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> ROCKET_HIMARS_HE = ITEMS.register("rocket_himars_he", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> ROCKET_HIMARS_LAVA = ITEMS.register("rocket_himars_lava", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> ROCKET_HIMARS_MINI_NUKE = ITEMS.register("rocket_himars_mini_nuke", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> ROCKET_HIMARS_WP = ITEMS.register("rocket_himars_wp", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> ROCKET_HIMARS_THERMOBARIC = ITEMS.register("rocket_himars_thermobaric", () -> new Item(new Item.Properties()));
    /** Uran-Munition fuer den Tauon-Turret (Original: {@code XFactoryAccelerator.tau_uranium}). */
    public static final RegistrySupplier<Item> AMMO_TAU_URANIUM = ITEMS.register("ammo_tau_uranium", () -> new Item(new Item.Properties()));
    /** Flammenwerfer-Brennstoff fuer den Fritz-Turret (MVP: Item statt vollem Fluid-Tank, Original: Diesel-Fluid). */
    public static final RegistrySupplier<Item> AMMO_FLAME_DIESEL = ITEMS.register("ammo_flame_diesel", () -> new Item(new Item.Properties()));
    /** Fehlende Missile-Assembly-Teile (Original: {@code ItemCustomMissilePart} mit Typ FUSELAGE/CHIP). */
    public static final RegistrySupplier<Item> MISSILE_FUSELAGE = ITEMS.register("missile_fuselage", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> MISSILE_CHIP = ITEMS.register("missile_chip", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> TWINKIE = ITEMS.register("twinkie", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> ULLAPOOL_CABER = ITEMS.register("ullapool_caber", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> UNDEFINED = ITEMS.register("undefined", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> VOLCANIC_AXE = ITEMS.register("volcanic_axe", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> VOLCANIC_PICKAXE = ITEMS.register("volcanic_pickaxe", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> WAND_D = ITEMS.register("wand_d", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> WAND_S = ITEMS.register("wand_s", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> WARHEAD_INCENDIARY_LARGE = ITEMS.register("warhead_incendiary_large", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> WASTE_MOX = ITEMS.register("waste_mox", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> WASTE_PLATE_MOX = ITEMS.register("waste_plate_mox", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> WASTE_PLATE_PU238BE = ITEMS.register("waste_plate_pu238be", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> WASTE_PLATE_RA226BE = ITEMS.register("waste_plate_ra226be", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> WASTE_PLATE_SA326 = ITEMS.register("waste_plate_sa326", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> WASTE_PLATE_U233 = ITEMS.register("waste_plate_u233", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> WASTE_PLATE_U235 = ITEMS.register("waste_plate_u235", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> WASTE_PLATE_PU239 = ITEMS.register("waste_plate_pu239", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> WASTE_PLUTONIUM = ITEMS.register("waste_plutonium", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> WASTE_SCHRABIDIUM = ITEMS.register("waste_schrabidium", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> WASTE_THORIUM = ITEMS.register("waste_thorium", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> WASTE_URANIUM = ITEMS.register("waste_uranium", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> WASTE_ZFB_MOX = ITEMS.register("waste_zfb_mox", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> KEY_PIN = ITEMS.register("key_pin", () -> new com.hbm_m.item.ItemKeyPin(new Item.Properties().stacksTo(1)));
    public static final RegistrySupplier<Item> WATCH = ITEMS.register("watch", () -> new Item(new Item.Properties()));

    // Siren cassettes - simplified from the original's single ItemCassette + metadata TrackType enum
    // to discrete items (matching this project's convention for other multi-variant tools), one per
    // ported alarm track (only the 7 tracks with a .ogg file ported so far get an item).
    public static final RegistrySupplier<Item> CASSETTE_AMS_SIREN = ITEMS.register("cassette_ams_siren", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> CASSETTE_BEEP_SIREN = ITEMS.register("cassette_beep_siren", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> CASSETTE_CLASSIC_SIREN = ITEMS.register("cassette_classic_siren", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> CASSETTE_NOSTROMO_SIREN = ITEMS.register("cassette_nostromo_siren", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> CASSETTE_REGULAR_SIREN = ITEMS.register("cassette_regular_siren", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> CASSETTE_STRIDER_SIREN = ITEMS.register("cassette_strider_siren", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> CASSETTE_SWEEP_SIREN = ITEMS.register("cassette_sweep_siren", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> WD40 = ITEMS.register("wd40", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> WILD_P = ITEMS.register("wild_p", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> WINGS_LIMP = ITEMS.register("wings_limp", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> WINGS_MURK = ITEMS.register("wings_murk", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> WIRING_RED_COPPER = ITEMS.register("wiring_red_copper", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> WOOD_GAVEL = ITEMS.register("wood_gavel", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> WRENCH = ITEMS.register("wrench", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> WRENCH_ARCHINEER = ITEMS.register("wrench_archineer", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> WRENCH_FLIPPED = ITEMS.register("wrench_flipped", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> XANAX = ITEMS.register("xanax", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> ZIRCONIUM_LEGS = ITEMS.register("zirconium_legs", () -> new Item(new Item.Properties()));

    public static void init() {
        ITEMS.register();
    }
}
