package com.hbm_m.item;

// Класс для регистрации всех предметов мода.
// Использует DeferredRegister для отложенной регистрации. Здесь так же регистрируются моды для брони.
// Слитки регистрируются автоматически на основе перечисления ModIngots.

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;


import com.hbm_m.api.fluids.ModFluids;

import com.hbm_m.item.custom.fekal_electric.ItemCreativeBattery;
import com.hbm_m.item.custom.fekal_electric.ModBatteryItem;
import com.hbm_m.item.custom.crates.IronCrateItem;
import com.hbm_m.item.custom.crates.SteelCrateItem;
import com.hbm_m.item.custom.industrial.*;
import com.hbm_m.item.custom.liquids.InfiniteWaterItem;
import com.hbm_m.item.custom.radiation_meter.ItemDosimeter;
import com.hbm_m.item.custom.radiation_meter.ItemGeigerCounter;
import com.hbm_m.item.custom.food.ItemConserve;
import com.hbm_m.item.custom.food.ItemEnergyDrink;
import com.hbm_m.item.custom.food.ModFoods;
import com.hbm_m.item.custom.grenades_and_activators.*;
import com.hbm_m.item.custom.tools_and_armor.*;
import com.hbm_m.item.tags_and_tiers.*;
import com.hbm_m.item.custom.scanners.DepthOresScannerItem;
import com.hbm_m.item.custom.scanners.OilDetectorItem;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import java.util.List;

import com.hbm_m.block.custom.machines.armormod.item.ItemModHealth;
import com.hbm_m.block.custom.machines.armormod.item.ItemModRadProtection;
import com.hbm_m.block.ModBlocks;
import com.hbm_m.effect.ModEffects;
import com.hbm_m.entity.ModEntities;
import com.hbm_m.entity.grenades.GrenadeIfType;
import com.hbm_m.entity.grenades.GrenadeType;
import com.hbm_m.multiblock.MultiblockBlockItem;
import com.hbm_m.sound.ModSounds;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.*;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import static com.hbm_m.lib.RefStrings.MODID;


public class ModItems {
    // Создаем отложенный регистратор для предметов.
    // Это стандартный способ регистрации объектов в Forge.
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, MODID);

    public static final Map<ModIngots, RegistryObject<Item>> INGOTS = new EnumMap<>(ModIngots.class);
    public static final Map<ModPowders, RegistryObject<Item>> POWDERS = new EnumMap<>(ModPowders.class);
    public static final Map<ModIngots, RegistryObject<Item>> INGOT_POWDERS = new EnumMap<>(ModIngots.class);
    public static final Map<ModIngots, RegistryObject<Item>> INGOT_POWDERS_TINY = new EnumMap<>(ModIngots.class);

    private static final Set<String> POWDER_TINY_NAMES = Set.of(
            "actinium", "boron", "cerium", "cobalt", "cs137", "i131",
            "lanthanium", "lithium", "meteorite", "neodymium", "niobium",
            "sr90", "steel", "xe135");
    private static final Map<String, RegistryObject<Item>> POWDER_ITEMS_BY_ID = new HashMap<>();

    private static final Set<String> ENABLED_MODPOWDERS = Set.of("iron", "gold", "coal"); // Только ModPowders!
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
        // 1. СЛИТКИ (ВСЕГДА) ✅ OK
        for (ModIngots ingot : ModIngots.values()) {
            RegistryObject<Item> registeredItem;
            if (ingot == ModIngots.URANIUM) {
                registeredItem = ITEMS.register(ingot.getName() + "_ingot", () -> new RadioactiveItem(new Item.Properties()));
            } else {
                registeredItem = ITEMS.register(ingot.getName() + "_ingot", () -> new Item(new Item.Properties()));
            }
            INGOTS.put(ingot, registeredItem);
        }

        // 2. ModPowders (ТОЛЬКО ИЗ ENABLED_MODPOWDERS) ✅ ИСПРАВЛЕНО!
        for (ModPowders powder : ModPowders.values()) {
            String baseName = powder.name(); // или powder.getName() если есть
            if (ENABLED_MODPOWDERS.contains(baseName)) {
                String powderId = baseName + "_powder";
                RegistryObject<Item> powderItem = ITEMS.register(powderId,
                        () -> powder == ModPowders.IRON ? new RadioactiveItem(new Item.Properties()) : new Item(new Item.Properties()));
                POWDERS.put(powder, powderItem);
                POWDER_ITEMS_BY_ID.put(powderId, powderItem);
            }
        }

        // 3. Порошки из слитков (ТОЛЬКО ИЗ ENABLED_INGOT_POWDERS) ✅ ИСПРАВЛЕНО!
        for (ModIngots ingot : ModIngots.values()) {
            String baseName = ingot.getName();

            // Основной порошок
            if (ENABLED_INGOT_POWDERS.contains(baseName)) {
                String powderId = baseName + "_powder";
                RegistryObject<Item> powderItem = POWDER_ITEMS_BY_ID.get(powderId);
                if (powderItem == null) {
                    powderItem = ITEMS.register(powderId, () -> new Item(new Item.Properties()));
                    POWDER_ITEMS_BY_ID.put(powderId, powderItem);
                }
                INGOT_POWDERS.put(ingot, powderItem);
            }

            // Маленький порошок ✅ OK
            if (POWDER_TINY_NAMES.contains(baseName) && ENABLED_TINY_POWDERS.contains(baseName)) {
                String tinyId = baseName + "_powder_tiny";
                RegistryObject<Item> tinyItem = ITEMS.register(tinyId, () -> new Item(new Item.Properties()));
                INGOT_POWDERS_TINY.put(ingot, tinyItem);
            }
        }
    }

    // УДОБНЫЙ МЕТОД ДЛЯ ПОЛУЧЕНИЯ СЛИТКА
    public static RegistryObject<Item> getIngot(ModIngots ingot) {
        return INGOTS.get(ingot);
    }

    public static RegistryObject<Item> getPowders(ModPowders powders) {return POWDERS.get(powders);}
    public static RegistryObject<Item> getPowder(ModIngots ingot) { return INGOT_POWDERS.get(ingot); }
    public static Optional<RegistryObject<Item>> getTinyPowder(ModIngots ingot) {
        return Optional.ofNullable(INGOT_POWDERS_TINY.get(ingot));
    }
    
    public static final int SLOT_HELMET = 0;
    public static final int SLOT_CHEST = 1;
    public static final int SLOT_LEGS = 2;
    public static final int SLOT_BOOTS = 3;
    public static final int SLOT_BATTERY = 4;
    public static final int SLOT_SPECIAL = 5;
    public static final int SLOT_INSERT = 6;
    public static final int SLOT_CLADDING = 7;
    public static final int SLOT_SERVOS = 8;

    public static final int BATTERY_CAPACITY = 1_000_000;

// ХАВЧИК:
    public static final RegistryObject<Item> STRAWBERRY = ITEMS.register("strawberry",
            () -> new Item(new Item.Properties().food(ModFoods.STRAWBERRY)));
    public static final RegistryObject<Item> CANNED_ASBESTOS = ITEMS.register("canned_asbestos",
            () -> new ItemConserve(new Item.Properties().food(ModFoods.CANNED_ASBESTOS)));
    public static final RegistryObject<Item> CANNED_ASS = ITEMS.register("canned_ass",
            () -> new ItemConserve(new Item.Properties().food(ModFoods.CANNED_ASS)));
    public static final RegistryObject<Item> CANNED_BARK = ITEMS.register("canned_bark",
            () -> new ItemConserve(new Item.Properties().food(ModFoods.CANNED_BARK)));
    public static final RegistryObject<Item> CANNED_BEEF = ITEMS.register("canned_beef",
            () -> new ItemConserve(new Item.Properties().food(ModFoods.CANNED_BEEF)));
    public static final RegistryObject<Item> CANNED_BHOLE = ITEMS.register("canned_bhole",
            () -> new ItemConserve(new Item.Properties().food(ModFoods.CANNED_BHOLE)));
    public static final RegistryObject<Item> CANNED_CHEESE = ITEMS.register("canned_cheese",
            () -> new ItemConserve(new Item.Properties().food(ModFoods.CANNED_CHEESE)));
    public static final RegistryObject<Item> CANNED_CHINESE = ITEMS.register("canned_chinese",
            () -> new ItemConserve(new Item.Properties().food(ModFoods.CANNED_CHINESE)));
    public static final RegistryObject<Item> CANNED_DIESEL = ITEMS.register("canned_diesel",
            () -> new ItemConserve(new Item.Properties().food(ModFoods.CANNED_DIESEL)));
    public static final RegistryObject<Item> CANNED_FIST = ITEMS.register("canned_fist",
            () -> new ItemConserve(new Item.Properties().food(ModFoods.CANNED_FIST)));
    public static final RegistryObject<Item> CANNED_FRIED = ITEMS.register("canned_fried",
            () -> new ItemConserve(new Item.Properties().food(ModFoods.CANNED_FRIED)));
    public static final RegistryObject<Item> CANNED_HOTDOGS = ITEMS.register("canned_hotdogs",
            () -> new ItemConserve(new Item.Properties().food(ModFoods.CANNED_HOTDOGS)));
    public static final RegistryObject<Item> CANNED_JIZZ = ITEMS.register("canned_jizz",
            () -> new ItemConserve(new Item.Properties().food(ModFoods.CANNED_JIZZ)));
    public static final RegistryObject<Item> CANNED_KEROSENE = ITEMS.register("canned_kerosene",
            () -> new ItemConserve(new Item.Properties().food(ModFoods.CANNED_KEROSENE)));
    public static final RegistryObject<Item> CANNED_LEFTOVERS = ITEMS.register("canned_leftovers",
            () -> new ItemConserve(new Item.Properties().food(ModFoods.CANNED_LEFTOVERS)));
    public static final RegistryObject<Item> CANNED_MILK = ITEMS.register("canned_milk",
            () -> new ItemConserve(new Item.Properties().food(ModFoods.CANNED_MILK)));
    public static final RegistryObject<Item> CANNED_MYSTERY = ITEMS.register("canned_mystery",
            () -> new ItemConserve(new Item.Properties().food(ModFoods.CANNED_MYSTERY)));
    public static final RegistryObject<Item> CANNED_NAPALM = ITEMS.register("canned_napalm",
            () -> new ItemConserve(new Item.Properties().food(ModFoods.CANNED_NAPALM)));
    public static final RegistryObject<Item> CANNED_OIL = ITEMS.register("canned_oil",
            () -> new ItemConserve(new Item.Properties().food(ModFoods.CANNED_OIL)));
    public static final RegistryObject<Item> CANNED_PASHTET = ITEMS.register("canned_pashtet",
            () -> new ItemConserve(new Item.Properties().food(ModFoods.CANNED_PASHTET)));
    public static final RegistryObject<Item> CANNED_PIZZA = ITEMS.register("canned_pizza",
            () -> new ItemConserve(new Item.Properties().food(ModFoods.CANNED_PIZZA)));
    public static final RegistryObject<Item> CANNED_RECURSION = ITEMS.register("canned_recursion",
            () -> new ItemConserve(new Item.Properties().food(ModFoods.CANNED_RECURSION)));
    public static final RegistryObject<Item> CANNED_SPAM = ITEMS.register("canned_spam",
            () -> new ItemConserve(new Item.Properties().food(ModFoods.CANNED_SPAM)));
    public static final RegistryObject<Item> CANNED_STEW = ITEMS.register("canned_stew",
            () -> new ItemConserve(new Item.Properties().food(ModFoods.CANNED_STEW)));
    public static final RegistryObject<Item> CANNED_TOMATO = ITEMS.register("canned_tomato",
            () -> new ItemConserve(new Item.Properties().food(ModFoods.CANNED_TOMATO)));
    public static final RegistryObject<Item> CANNED_TUNA = ITEMS.register("canned_tuna",
            () -> new ItemConserve(new Item.Properties().food(ModFoods.CANNED_TUNA)));
    public static final RegistryObject<Item> CANNED_TUBE = ITEMS.register("canned_tube",
            () -> new ItemConserve(new Item.Properties().food(ModFoods.CANNED_TUBE)));
    public static final RegistryObject<Item> CANNED_YOGURT = ITEMS.register("canned_yogurt",
            () -> new ItemConserve(new Item.Properties().food(ModFoods.CANNED_YOGURT)));


    public static final RegistryObject<Item> CAN_BEPIS = ITEMS.register("can_bepis",
            () -> new ItemEnergyDrink(new Item.Properties().food(ItemEnergyDrink.CAN_BEPIS)));
    public static final RegistryObject<Item> CAN_BREEN = ITEMS.register("can_breen",
            () -> new ItemEnergyDrink(new Item.Properties().food(ItemEnergyDrink.CAN_BREEN)));
    public static final RegistryObject<Item> CAN_CREATURE = ITEMS.register("can_creature",
            () -> new ItemEnergyDrink(new Item.Properties().food(ItemEnergyDrink.CAN_CREATURE)));
    public static final RegistryObject<Item> CAN_EMPTY = ITEMS.register("can_empty",
            () -> new Item(new Item.Properties())); // Пустая банка без эффекта
    public static final RegistryObject<Item> CAN_LUNA = ITEMS.register("can_luna",
            () -> new ItemEnergyDrink(new Item.Properties().food(ItemEnergyDrink.CAN_LUNA)));
    public static final RegistryObject<Item> CAN_MRSUGAR = ITEMS.register("can_mrsugar",
            () -> new ItemEnergyDrink(new Item.Properties().food(ItemEnergyDrink.CAN_MRSUGAR)));
    public static final RegistryObject<Item> CAN_MUG = ITEMS.register("can_mug",
            () -> new ItemEnergyDrink(new Item.Properties().food(ItemEnergyDrink.CAN_MUG)));
    public static final RegistryObject<Item> CAN_OVERCHARGE = ITEMS.register("can_overcharge",
            () -> new ItemEnergyDrink(new Item.Properties().food(ItemEnergyDrink.CAN_OVERCHARGE)));
    public static final RegistryObject<Item> CAN_REDBOMB = ITEMS.register("can_redbomb",
            () -> new ItemEnergyDrink(new Item.Properties().food(ItemEnergyDrink.CAN_REDBOMB)));
    public static final RegistryObject<Item> CAN_SMART = ITEMS.register("can_smart",
            () -> new ItemEnergyDrink(new Item.Properties().food(ItemEnergyDrink.CAN_SMART)));



    // ИНСТРУМЕНТЫ ГОРНЯКА:
    public static final RegistryObject<Item> STARMETAL_SWORD = ITEMS.register("starmetal_sword",
            () -> new SwordItem(ModToolTiers.STARMETAL, 7, -2, new Item.Properties()));
    public static final RegistryObject<Item> STARMETAL_AXE = ITEMS.register("starmetal_axe",
            () -> new ModAxeItem(ModToolTiers.STARMETAL, 15, 1, new Item.Properties()));
    public static final RegistryObject<Item> STARMETAL_PICKAXE = ITEMS.register("starmetal_pickaxe",
            () -> new ModPickaxeItem(ModToolTiers.STARMETAL, 3, 1, new Item.Properties(), 6, 3, 1, 5));
    public static final RegistryObject<Item> STARMETAL_SHOVEL = ITEMS.register("starmetal_shovel",
            () -> new ShovelItem(ModToolTiers.STARMETAL, 0, 0, new Item.Properties()));
    public static final RegistryObject<Item> STARMETAL_HOE = ITEMS.register("starmetal_hoe",
            () -> new HoeItem(ModToolTiers.STARMETAL, 0, 0f, new Item.Properties()));

    public static final RegistryObject<Item> ALLOY_SWORD = ITEMS.register("alloy_sword",
        () -> new SwordItem(ModToolTiers.ALLOY, 5, 2, new Item.Properties()));

    public static final RegistryObject<Item> ALLOY_AXE = ITEMS.register("alloy_axe",
            () -> new ModAxeItem(ModToolTiers.ALLOY, 9, 1, new Item.Properties(), 3, 1));

    public static final RegistryObject<Item> ALLOY_PICKAXE = ITEMS.register("alloy_pickaxe",
            () -> new ModPickaxeItem(ModToolTiers.ALLOY, 2, 1, new Item.Properties(), 3, 0, 0, 0));

    public static final RegistryObject<Item> ALLOY_SHOVEL = ITEMS.register("alloy_shovel",
            () -> new ModShovelItem(ModToolTiers.ALLOY, 0, 0, new Item.Properties(), 3, 0, 2));

    public static final RegistryObject<Item> ALLOY_HOE = ITEMS.register("alloy_hoe",
            () -> new HoeItem(ModToolTiers.ALLOY, 0, 0f, new Item.Properties()));

    public static final RegistryObject<Item> STEEL_SWORD = ITEMS.register("steel_sword",
            () -> new SwordItem(ModToolTiers.STEEL, 4, 2, new Item.Properties()));
    public static final RegistryObject<Item> STEEL_AXE = ITEMS.register("steel_axe",
            () -> new AxeItem(ModToolTiers.STEEL, 7, 1, new Item.Properties()));
    public static final RegistryObject<Item> STEEL_PICKAXE = ITEMS.register("steel_pickaxe",
            () -> new PickaxeItem(ModToolTiers.STEEL, 1, 1, new Item.Properties()));
    public static final RegistryObject<Item> STEEL_SHOVEL = ITEMS.register("steel_shovel",
            () -> new ShovelItem(ModToolTiers.STEEL, 0, 0, new Item.Properties()));
    public static final RegistryObject<Item> STEEL_HOE = ITEMS.register("steel_hoe",
            () -> new HoeItem(ModToolTiers.STEEL, 0, 0, new Item.Properties()));

    public static final RegistryObject<Item> TITANIUM_SWORD = ITEMS.register("titanium_sword",
            () -> new SwordItem(ModToolTiers.TITANIUM, 2, 3, new Item.Properties()));
    public static final RegistryObject<Item> TITANIUM_AXE = ITEMS.register("titanium_axe",
            () -> new AxeItem(ModToolTiers.TITANIUM, 8, 1, new Item.Properties()));
    public static final RegistryObject<Item> TITANIUM_PICKAXE = ITEMS.register("titanium_pickaxe",
            () -> new PickaxeItem(ModToolTiers.TITANIUM, 1, 1, new Item.Properties()));
    public static final RegistryObject<Item> TITANIUM_SHOVEL = ITEMS.register("titanium_shovel",
            () -> new ShovelItem(ModToolTiers.TITANIUM, 0, 0, new Item.Properties()));
    public static final RegistryObject<Item> TITANIUM_HOE = ITEMS.register("titanium_hoe",
            () -> new HoeItem(ModToolTiers.TITANIUM, 0, 0, new Item.Properties()));


    public static final RegistryObject<Item> GRENADE = ITEMS.register("grenade",
        () -> new GrenadeItem(new Item.Properties(), GrenadeType.STANDARD, ModEntities.GRENADE_PROJECTILE));

    public static final RegistryObject<Item> GRENADEHE = ITEMS.register("grenadehe",
        () -> new GrenadeItem(new Item.Properties(), GrenadeType.HE, ModEntities.GRENADEHE_PROJECTILE));

    public static final RegistryObject<Item> GRENADEFIRE = ITEMS.register("grenadefire",
        () -> new GrenadeItem(new Item.Properties(), GrenadeType.FIRE, ModEntities.GRENADEFIRE_PROJECTILE));

    public static final RegistryObject<Item> GRENADESLIME = ITEMS.register("grenadeslime",
        () -> new GrenadeItem(new Item.Properties(), GrenadeType.SLIME, ModEntities.GRENADESLIME_PROJECTILE));

    public static final RegistryObject<Item> GRENADESMART = ITEMS.register("grenadesmart",
        () -> new GrenadeItem(new Item.Properties(), GrenadeType.SMART, ModEntities.GRENADESMART_PROJECTILE));

    public static final RegistryObject<Item> GRENADE_IF = ITEMS.register("grenade_if",
            () -> new GrenadeIfItem(new Item.Properties(), GrenadeIfType.GRENADE_IF, ModEntities.GRENADE_IF_PROJECTILE));

    public static final RegistryObject<Item> GRENADE_IF_HE = ITEMS.register("grenade_if_he",
            () -> new GrenadeIfItem(new Item.Properties(), GrenadeIfType.GRENADE_IF_HE, ModEntities.GRENADE_IF_HE_PROJECTILE));

    public static final RegistryObject<Item> GRENADE_IF_SLIME = ITEMS.register("grenade_if_slime",
            () -> new GrenadeIfItem(new Item.Properties(), GrenadeIfType.GRENADE_IF_SLIME, ModEntities.GRENADE_IF_SLIME_PROJECTILE));

    public static final RegistryObject<Item> GRENADE_IF_FIRE = ITEMS.register("grenade_if_fire",
            () -> new GrenadeIfItem(new Item.Properties(), GrenadeIfType.GRENADE_IF_FIRE, ModEntities.GRENADE_IF_FIRE_PROJECTILE));

    public static final RegistryObject<Item> GRENADE_NUC = ITEMS.register("grenade_nuc",
            () -> new GrenadeNucItem(new Item.Properties(), ModEntities.GRENADE_NUC_PROJECTILE));

    public static final RegistryObject<Item> AIRBOMB_A = ITEMS.register("airbomb_a",
            () -> new AirBombItem(new Item.Properties(), ModEntities.AIRBOMB_PROJECTILE));
    public static final RegistryObject<Item> AIRNUKEBOMB_A = ITEMS.register("airnukebomb_a",
            () -> new AirNukeBombItem(new Item.Properties(), ModEntities.AIRNUKEBOMB_PROJECTILE));

    // БРОНЯ ГОРНЯКА:
    public static final RegistryObject<Item> ALLOY_HELMET = ITEMS.register("alloy_helmet",
            () -> new ArmorItem(ModArmorMaterials.ALLOY, ArmorItem.Type.HELMET, new Item.Properties()));
    public static final RegistryObject<Item> ALLOY_CHESTPLATE = ITEMS.register("alloy_chestplate",
            () -> new ArmorItem(ModArmorMaterials.ALLOY, ArmorItem.Type.CHESTPLATE, new Item.Properties()));
    public static final RegistryObject<Item> ALLOY_LEGGINGS = ITEMS.register("alloy_leggings",
            () -> new ArmorItem(ModArmorMaterials.ALLOY, ArmorItem.Type.LEGGINGS, new Item.Properties()));
    public static final RegistryObject<Item> ALLOY_BOOTS = ITEMS.register("alloy_boots",
            () -> new ArmorItem(ModArmorMaterials.ALLOY, ArmorItem.Type.BOOTS, new Item.Properties()));

    public static final RegistryObject<Item> TITANIUM_HELMET = ITEMS.register("titanium_helmet",
            () -> new ModArmorItem(ModArmorMaterials.TITANIUM, ArmorItem.Type.HELMET, new Item.Properties()));
    public static final RegistryObject<Item> TITANIUM_CHESTPLATE = ITEMS.register("titanium_chestplate",
            () -> new ArmorItem(ModArmorMaterials.TITANIUM, ArmorItem.Type.CHESTPLATE, new Item.Properties()));
    public static final RegistryObject<Item> TITANIUM_LEGGINGS = ITEMS.register("titanium_leggings",
            () -> new ArmorItem(ModArmorMaterials.TITANIUM, ArmorItem.Type.LEGGINGS, new Item.Properties()));
    public static final RegistryObject<Item> TITANIUM_BOOTS = ITEMS.register("titanium_boots",
            () -> new ArmorItem(ModArmorMaterials.TITANIUM, ArmorItem.Type.BOOTS, new Item.Properties()));

    public static final RegistryObject<Item> STEEL_HELMET = ITEMS.register("steel_helmet",
            () -> new ArmorItem(ModArmorMaterials.STEEL, ArmorItem.Type.HELMET, new Item.Properties()));
    public static final RegistryObject<Item> STEEL_CHESTPLATE = ITEMS.register("steel_chestplate",
            () -> new ArmorItem(ModArmorMaterials.TITANIUM, ArmorItem.Type.CHESTPLATE, new Item.Properties()));
    public static final RegistryObject<Item> STEEL_LEGGINGS = ITEMS.register("steel_leggings",
            () -> new ArmorItem(ModArmorMaterials.STEEL, ArmorItem.Type.LEGGINGS, new Item.Properties()));
    public static final RegistryObject<Item> STEEL_BOOTS = ITEMS.register("steel_boots",
            () -> new ArmorItem(ModArmorMaterials.STEEL, ArmorItem.Type.BOOTS, new Item.Properties()));

    public static final RegistryObject<Item> COBALT_HELMET = ITEMS.register("cobalt_helmet",
            () -> new ModArmorItem(ModArmorMaterials.COBALT, ArmorItem.Type.HELMET, new Item.Properties()));
    public static final RegistryObject<Item> COBALT_CHESTPLATE = ITEMS.register("cobalt_chestplate",
            () -> new ArmorItem(ModArmorMaterials.COBALT, ArmorItem.Type.CHESTPLATE, new Item.Properties()));
    public static final RegistryObject<Item> COBALT_LEGGINGS = ITEMS.register("cobalt_leggings",
            () -> new ArmorItem(ModArmorMaterials.COBALT, ArmorItem.Type.LEGGINGS, new Item.Properties()));
    public static final RegistryObject<Item> COBALT_BOOTS = ITEMS.register("cobalt_boots",
            () -> new ArmorItem(ModArmorMaterials.COBALT, ArmorItem.Type.BOOTS, new Item.Properties()));

    public static final RegistryObject<Item> SECURITY_HELMET = ITEMS.register("security_helmet",
            () -> new ArmorItem(ModArmorMaterials.SECURITY, ArmorItem.Type.HELMET, new Item.Properties()));
    public static final RegistryObject<Item> SECURITY_CHESTPLATE = ITEMS.register("security_chestplate",
            () -> new ArmorItem(ModArmorMaterials.SECURITY, ArmorItem.Type.CHESTPLATE, new Item.Properties()));
    public static final RegistryObject<Item> SECURITY_LEGGINGS = ITEMS.register("security_leggings",
            () -> new ArmorItem(ModArmorMaterials.SECURITY, ArmorItem.Type.LEGGINGS, new Item.Properties()));
    public static final RegistryObject<Item> SECURITY_BOOTS = ITEMS.register("security_boots",
            () -> new ArmorItem(ModArmorMaterials.SECURITY, ArmorItem.Type.BOOTS, new Item.Properties()));

    public static final RegistryObject<Item> AJR_HELMET = ITEMS.register("ajr_helmet",
            () -> new ModArmorItem(ModArmorMaterials.AJR, ArmorItem.Type.HELMET, new Item.Properties()));
    public static final RegistryObject<Item> AJR_CHESTPLATE = ITEMS.register("ajr_chestplate",
            () -> new ArmorItem(ModArmorMaterials.AJR, ArmorItem.Type.CHESTPLATE, new Item.Properties()));
    public static final RegistryObject<Item> AJR_LEGGINGS = ITEMS.register("ajr_leggings",
            () -> new ArmorItem(ModArmorMaterials.AJR, ArmorItem.Type.LEGGINGS, new Item.Properties()));
    public static final RegistryObject<Item> AJR_BOOTS = ITEMS.register("ajr_boots",
            () -> new ArmorItem(ModArmorMaterials.AJR, ArmorItem.Type.BOOTS, new Item.Properties()));

    public static final RegistryObject<Item> ASBESTOS_HELMET = ITEMS.register("asbestos_helmet",
            () -> new ModArmorItem(ModArmorMaterials.ASBESTOS, ArmorItem.Type.HELMET, new Item.Properties()));
    public static final RegistryObject<Item> ASBESTOS_CHESTPLATE = ITEMS.register("asbestos_chestplate",
            () -> new ArmorItem(ModArmorMaterials.ASBESTOS, ArmorItem.Type.CHESTPLATE, new Item.Properties()));
    public static final RegistryObject<Item> ASBESTOS_LEGGINGS = ITEMS.register("asbestos_leggings",
            () -> new ArmorItem(ModArmorMaterials.ASBESTOS, ArmorItem.Type.LEGGINGS, new Item.Properties()));
    public static final RegistryObject<Item> ASBESTOS_BOOTS = ITEMS.register("asbestos_boots",
            () -> new ArmorItem(ModArmorMaterials.ASBESTOS, ArmorItem.Type.BOOTS, new Item.Properties()));

    public static final RegistryObject<Item> HAZMAT_HELMET = ITEMS.register("hazmat_helmet",
            () -> new ArmorItem(ModArmorMaterials.HAZMAT, ArmorItem.Type.HELMET, new Item.Properties()));
    public static final RegistryObject<Item> HAZMAT_CHESTPLATE = ITEMS.register("hazmat_chestplate",
            () -> new ArmorItem(ModArmorMaterials.HAZMAT, ArmorItem.Type.CHESTPLATE, new Item.Properties()));
    public static final RegistryObject<Item> HAZMAT_LEGGINGS = ITEMS.register("hazmat_leggings",
            () -> new ArmorItem(ModArmorMaterials.HAZMAT, ArmorItem.Type.LEGGINGS, new Item.Properties()));
    public static final RegistryObject<Item> HAZMAT_BOOTS = ITEMS.register("hazmat_boots",
            () -> new ArmorItem(ModArmorMaterials.HAZMAT, ArmorItem.Type.BOOTS, new Item.Properties()));

    public static final RegistryObject<Item> LIQUIDATOR_HELMET = ITEMS.register("liquidator_helmet",
            () -> new ModArmorItem(ModArmorMaterials.LIQUIDATOR, ArmorItem.Type.HELMET, new Item.Properties()));
    public static final RegistryObject<Item> LIQUIDATOR_CHESTPLATE = ITEMS.register("liquidator_chestplate",
            () -> new ArmorItem(ModArmorMaterials.LIQUIDATOR, ArmorItem.Type.CHESTPLATE, new Item.Properties()));
    public static final RegistryObject<Item> LIQUIDATOR_LEGGINGS = ITEMS.register("liquidator_leggings",
            () -> new ArmorItem(ModArmorMaterials.LIQUIDATOR, ArmorItem.Type.LEGGINGS, new Item.Properties()));
    public static final RegistryObject<Item> LIQUIDATOR_BOOTS = ITEMS.register("liquidator_boots",
            () -> new ArmorItem(ModArmorMaterials.LIQUIDATOR, ArmorItem.Type.BOOTS, new Item.Properties()));

    public static final RegistryObject<Item> PAA_HELMET = ITEMS.register("paa_helmet",
            () -> new ArmorItem(ModArmorMaterials.PAA, ArmorItem.Type.HELMET, new Item.Properties()));
    public static final RegistryObject<Item> PAA_CHESTPLATE = ITEMS.register("paa_chestplate",
            () -> new ArmorItem(ModArmorMaterials.PAA, ArmorItem.Type.CHESTPLATE, new Item.Properties()));
    public static final RegistryObject<Item> PAA_LEGGINGS = ITEMS.register("paa_leggings",
            () -> new ArmorItem(ModArmorMaterials.PAA, ArmorItem.Type.LEGGINGS, new Item.Properties()));
    public static final RegistryObject<Item> PAA_BOOTS = ITEMS.register("paa_boots",
            () -> new ArmorItem(ModArmorMaterials.PAA, ArmorItem.Type.BOOTS, new Item.Properties()));

    public static final RegistryObject<Item> STARMETAL_HELMET = ITEMS.register("starmetal_helmet",
            () -> new ArmorItem(ModArmorMaterials.STARMETAL, ArmorItem.Type.HELMET, new Item.Properties()));
    public static final RegistryObject<Item> STARMETAL_CHESTPLATE = ITEMS.register("starmetal_chestplate",
            () -> new ArmorItem(ModArmorMaterials.STARMETAL, ArmorItem.Type.CHESTPLATE, new Item.Properties()));
    public static final RegistryObject<Item> STARMETAL_LEGGINGS = ITEMS.register("starmetal_leggings",
            () -> new ArmorItem(ModArmorMaterials.STARMETAL, ArmorItem.Type.LEGGINGS, new Item.Properties()));
    public static final RegistryObject<Item> STARMETAL_BOOTS = ITEMS.register("starmetal_boots",
            () -> new ArmorItem(ModArmorMaterials.STARMETAL, ArmorItem.Type.BOOTS, new Item.Properties()));

    // Инструменты
    public static final RegistryObject<Item> GEIGER_COUNTER = ITEMS.register("geiger_counter",
            () -> new ItemGeigerCounter(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> DOSIMETER = ITEMS.register("dosimeter",
            () -> new ItemDosimeter(new Item.Properties().stacksTo(1)));




    public static final RegistryObject<Item> CRATE_IRON = ITEMS.register("crate_iron",
            () -> new IronCrateItem(ModBlocks.CRATE_IRON.get(), new Item.Properties()));
    public static final RegistryObject<Item> CRATE_STEEL = ITEMS.register("crate_steel",
            () -> new SteelCrateItem(ModBlocks.CRATE_STEEL.get(), new Item.Properties()));




    // Модификаторы брони
    public static final RegistryObject<Item> HEART_PIECE = ITEMS.register("heart_piece",
            () -> new ItemModHealth(
                    new Item.Properties(),
                    SLOT_SPECIAL,
                    5.0
            )
    );
    public static final RegistryObject<Item> HEART_CONTAINER = ITEMS.register("heart_container",
            () -> new ItemModHealth(
                    new Item.Properties(),
                    SLOT_SPECIAL,
                    20.0
            )
    );
    public static final RegistryObject<Item> HEART_BOOSTER = ITEMS.register("heart_booster",
            () -> new ItemModHealth(
                    new Item.Properties(),
                    SLOT_SPECIAL,
                    40.0
            )
    );
    public static final RegistryObject<Item> HEART_FAB = ITEMS.register("heart_fab",
            () -> new ItemModHealth(
                    new Item.Properties(),
                    SLOT_SPECIAL,
                    60.0
            )
    );
    public static final RegistryObject<Item> BLACK_DIAMOND = ITEMS.register("black_diamond",
            () -> new ItemModHealth(
                    new Item.Properties(),
                    SLOT_SPECIAL,
                    40.0
            )
    );

    public static final RegistryObject<Item> GHIORSIUM_CLADDING = ITEMS.register("cladding_ghiorsium",
            () -> new ItemModRadProtection(
                    new Item.Properties(),
                    SLOT_CLADDING,
                    0.5f
            )
    );
    public static final RegistryObject<Item> DESH_CLADDING = ITEMS.register("cladding_desh",
            () -> new ItemModRadProtection(
                    new Item.Properties(),
                    SLOT_CLADDING,
                    0.2f
            )
    );
    public static final RegistryObject<Item> LEAD_CLADDING = ITEMS.register("cladding_lead",
            () -> new ItemModRadProtection(
                    new Item.Properties(),
                    SLOT_CLADDING,
                    0.1f
            )
    );
    public static final RegistryObject<Item> RUBBER_CLADDING = ITEMS.register("cladding_rubber",
            () -> new ItemModRadProtection(
                    new Item.Properties(),
                    SLOT_CLADDING,
                    0.005f
            )
    );
    public static final RegistryObject<Item> PAINT_CLADDING = ITEMS.register("cladding_paint",
            () -> new ItemModRadProtection(
                    new Item.Properties(),
                    SLOT_CLADDING,
                    0.025f
            )
    );
    public static final RegistryObject<Item> CREATIVE_BATTERY = ITEMS.register("battery_creative",
            () -> new ItemCreativeBattery(
                    new Item.Properties()
            )
    );
    public static final RegistryObject<Item> ASSEMBLY_TEMPLATE = ITEMS.register("assembly_template",
            () -> new ItemAssemblyTemplate(
                    new Item.Properties().stacksTo(1)
            )
    );
    public static final RegistryObject<Item> TEMPLATE_FOLDER = ITEMS.register("template_folder",
            () -> new ItemTemplateFolder(
                    new Item.Properties().stacksTo(1)
            )
    );
    public static final RegistryObject<Item> BLUEPRINT_FOLDER = ITEMS.register("blueprint_folder",
        () -> new ItemBlueprintFolder(
                new Item.Properties().stacksTo(1)
        )
    );


    public static final RegistryObject<Item> RADAWAY = ITEMS.register("radaway",
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
    public static final RegistryObject<Item> OIL_DETECTOR = ITEMS.register("oil_detector",
            () -> new OilDetectorItem(new Item.Properties()));

    public static final RegistryObject<Item> DEPTH_ORES_SCANNER = ITEMS.register("depth_ores_scanner",
            () -> new DepthOresScannerItem(new Item.Properties()));

    public static final RegistryObject<Item> RANGE_DETONATOR = ITEMS.register("range_detonator",
            () -> new RangeDetonatorItem(new Item.Properties()));

    public static final RegistryObject<Item> MULTI_DETONATOR = ITEMS.register("multi_detonator",
            () -> new MultiDetonatorItem(new Item.Properties()));

    public static final RegistryObject<Item> DETONATOR = ITEMS.register("detonator",
            () -> new DetonatorItem(new Item.Properties()));

    public static final RegistryObject<Item> BILLET_PLUTONIUM = ITEMS.register("billet_plutonium",
            () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> BALL_TNT = ITEMS.register("ball_tnt",
            () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> CROWBAR = ITEMS.register("crowbar",
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


    public static final RegistryObject<Item> MALACHITE_CHUNK = ITEMS.register("malachite_chunk",
            () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> LIMESTONE = ITEMS.register("limestone",
            () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> CAN_KEY = ITEMS.register("can_key",
            () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> DEFUSER = ITEMS.register("defuser",
            () -> new Item(new Item.Properties()) {
                @Override
                public void appendHoverText(ItemStack stack, @Nullable Level level,
                                            @Nullable List<Component> tooltip, TooltipFlag flag) {
                    if (tooltip == null) return;

                    tooltip.add(Component.translatable("tooltip.hbm_m.defuser.line1")
                            .withStyle(ChatFormatting.GRAY));
                }
            });
    public static final RegistryObject<Item> POWDER_COAL = ITEMS.register("powder_coal",
            () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> POWDER_COAL_SMALL = ITEMS.register("powder_coal_small",
            () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> BOLT_STEEL = ITEMS.register("bolt_steel",
            () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> ZIRCONIUM_SHARP = ITEMS.register("zirconium_sharp",
            () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> COIL_TUNGSTEN = ITEMS.register("coil_tungsten",
            () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> COIL_GOLD_TORUS = ITEMS.register("coil_gold_torus",
            () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> COIL_GOLD = ITEMS.register("coil_gold",
            () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> COIL_MAGNETIZED_TUNGSTEN_TORUS = ITEMS.register("coil_magnetized_tungsten_torus",
            () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> COIL_MAGNETIZED_TUNGSTEN = ITEMS.register("coil_magnetized_tungsten",
            () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> COIL_COPPER_TORUS = ITEMS.register("coil_copper_torus",
            () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> COIL_COPPER = ITEMS.register("coil_copper",
            () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> COIL_ADVANCED_ALLOY_TORUS = ITEMS.register("coil_advanced_alloy_torus",
            () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> COIL_ADVANCED_ALLOY = ITEMS.register("coil_advanced_alloy",
            () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> MOTOR_DESH = ITEMS.register("motor_desh",
            () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> MOTOR_BISMUTH = ITEMS.register("motor_bismuth",
            () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> MOTOR = ITEMS.register("motor",
            () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> BORAX = ITEMS.register("borax",
            () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> SCRAP = ITEMS.register("scrap",
            () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> DUST = ITEMS.register("dust",
            () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> DUST_TINY = ITEMS.register("dust_tiny",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> NUGGET_SILICON = ITEMS.register("nugget_silicon",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> BILLET_SILICON = ITEMS.register("billet_silicon",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> SILICON_CIRCUIT = ITEMS.register("silicon_circuit",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> BISMOID_CIRCUIT = ITEMS.register("bismoid_circuit",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> QUANTUM_CHIP = ITEMS.register("quantum_chip",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> CAPACITOR_BOARD = ITEMS.register("capacitor_board",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> CAPACITOR_TANTALUM = ITEMS.register("capacitor_tantalum",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> BISMOID_CHIP = ITEMS.register("bismoid_chip",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> CONTROLLER_CHASSIS = ITEMS.register("controller_chassis",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> CONTROLLER = ITEMS.register("controller",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> CONTROLLER_ADVANCED = ITEMS.register("controller_advanced",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> QUANTUM_COMPUTER = ITEMS.register("quantum_computer",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> QUANTUM_CIRCUIT = ITEMS.register("quantum_circuit",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> ANALOG_CIRCUIT = ITEMS.register("analog_circuit",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> INTEGRATED_CIRCUIT = ITEMS.register("integrated_circuit",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> ADVANCED_CIRCUIT = ITEMS.register("advanced_circuit",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> VACUUM_TUBE = ITEMS.register("vacuum_tube",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> CAPACITOR = ITEMS.register("capacitor",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> MICROCHIP = ITEMS.register("microchip",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> ATOMIC_CLOCK = ITEMS.register("atomic_clock",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> PCB = ITEMS.register("pcb",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> METAL_ROD = ITEMS.register("metal_rod",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> BATTLE_MODULE = ITEMS.register("battle_module",
            () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> BATTLE_GEARS = ITEMS.register("battle_gears",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> BATTLE_CASING = ITEMS.register("battle_casing",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> BATTLE_SENSOR = ITEMS.register("battle_sensor",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> BATTLE_COUNTER = ITEMS.register("battle_counter",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> MAN_CORE = ITEMS.register("man_core",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> CRT_DISPLAY = ITEMS.register("crt_display",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> PLATE_IRON = ITEMS.register("plate_iron",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> PLATE_STEEL = ITEMS.register("plate_steel",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> PLATE_GOLD = ITEMS.register("plate_gold",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> PLATE_GUNMETAL = ITEMS.register("plate_gunmetal",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> PLATE_GUNSTEEL = ITEMS.register("plate_gunsteel",
            () -> new Item(new Item.Properties()));
            
    public static final RegistryObject<Item> PLATE_TITANIUM = ITEMS.register("plate_titanium",
            () -> new Item(new Item.Properties())); 

    public static final RegistryObject<Item> PLATE_KEVLAR = ITEMS.register("plate_kevlar",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> PLATE_LEAD = ITEMS.register("plate_lead",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> PLATE_MIXED = ITEMS.register("plate_mixed",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> PLATE_PAA = ITEMS.register("plate_paa",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> INSULATOR = ITEMS.register("insulator",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> PLATE_SATURNITE = ITEMS.register("plate_saturnite",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> PLATE_SCHRABIDIUM = ITEMS.register("plate_schrabidium",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> PLATE_COPPER = ITEMS.register("plate_copper",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> PLATE_ALUMINUM = ITEMS.register("plate_aluminum",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> PLATE_ADVANCED_ALLOY = ITEMS.register("plate_advanced_alloy",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> PLATE_BISMUTH = ITEMS.register("plate_bismuth",
        () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> PLATE_ARMOR_AJR = ITEMS.register("plate_armor_ajr",
        () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> PLATE_ARMOR_DNT = ITEMS.register("plate_armor_dnt",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> PLATE_ARMOR_DNT_RUSTED = ITEMS.register("plate_armor_dnt_rusted",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> PLATE_ARMOR_FAU = ITEMS.register("plate_armor_fau",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> PLATE_ARMOR_HEV = ITEMS.register("plate_armor_hev",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> PLATE_ARMOR_LUNAR = ITEMS.register("plate_armor_lunar",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> PLATE_ARMOR_TITANIUM = ITEMS.register("plate_armor_titanium",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> PLATE_CAST = ITEMS.register("plate_cast",
        () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> PLATE_CAST_ALT = ITEMS.register("plate_cast_alt",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> PLATE_CAST_BISMUTH = ITEMS.register("plate_cast_bismuth",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> PLATE_CAST_DARK = ITEMS.register("plate_cast_dark",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> PLATE_COMBINE_STEEL = ITEMS.register("plate_combine_steel",
        () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> PLATE_DURA_STEEL = ITEMS.register("plate_dura_steel",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> PLATE_DALEKANIUM = ITEMS.register("plate_dalekanium",
        () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> PLATE_DESH = ITEMS.register("plate_desh",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> PLATE_DINEUTRONIUM = ITEMS.register("plate_dineutronium",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> PLATE_EUPHEMIUM = ITEMS.register("plate_euphemium",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> PLATE_FUEL_MOX = ITEMS.register("plate_fuel_mox",
        () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> PLATE_FUEL_PU238BE = ITEMS.register("plate_fuel_pu238be",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> PLATE_FUEL_PU239 = ITEMS.register("plate_fuel_pu239",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> PLATE_FUEL_RA226BE = ITEMS.register("plate_fuel_ra226be",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> PLATE_FUEL_SA326 = ITEMS.register("plate_fuel_sa326",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> PLATE_FUEL_U233 = ITEMS.register("plate_fuel_u233",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> PLATE_FUEL_U235 = ITEMS.register("plate_fuel_u235",
            () -> new Item(new Item.Properties()));

    // RAW METALS

    public static final RegistryObject<Item> URANIUM_RAW = ITEMS.register("uranium_raw",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> LEAD_RAW = ITEMS.register("lead_raw",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> BERYLLIUM_RAW = ITEMS.register("beryllium_raw",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> ALUMINUM_RAW = ITEMS.register("aluminum_raw",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> TITANIUM_RAW = ITEMS.register("titanium_raw",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> THORIUM_RAW = ITEMS.register("thorium_raw",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> COBALT_RAW = ITEMS.register("cobalt_raw",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> TUNGSTEN_RAW = ITEMS.register("tungsten_raw",
            () -> new Item(new Item.Properties()));




    // Материалы
    public static final RegistryObject<Item> SULFUR = ITEMS.register("sulfur",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> SEQUESTRUM = ITEMS.register("sequestrum",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> FLUORITE = ITEMS.register("fluorite",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> RAREGROUND_ORE_CHUNK = ITEMS.register("rareground_ore_chunk",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> FIRECLAY_BALL = ITEMS.register("fireclay_ball",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> WOOD_ASH_POWDER = ITEMS.register("wood_ash_powder",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> FIREBRICK = ITEMS.register("firebrick",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> LIGNITE = ITEMS.register("lignite",
            () -> new FuelItem(new Item.Properties(), 1000));

    public static final RegistryObject<Item> CINNABAR = ITEMS.register("cinnabar",
            () -> new Item(new Item.Properties()));




    // Здесь мы регистрируем мультиблочные структуры для того, чтобы MultiblockBlockItem при установке мог обрабатывать их на наличие препятствующих блоков.

    public static final RegistryObject<Item> MACHINE_ASSEMBLER = ITEMS.register("machine_assembler",
        () -> new MultiblockBlockItem(ModBlocks.MACHINE_ASSEMBLER.get(), new Item.Properties()));
            
    public static final RegistryObject<Item> ADVANCED_ASSEMBLY_MACHINE = ITEMS.register("advanced_assembly_machine",
        () -> new MultiblockBlockItem(ModBlocks.ADVANCED_ASSEMBLY_MACHINE.get(), new Item.Properties()));

    public static final RegistryObject<Item> PRESS = ITEMS.register("press",
        () -> new MultiblockBlockItem(ModBlocks.PRESS.get(), new Item.Properties()));

    public static final RegistryObject<Item> WOOD_BURNER = ITEMS.register("wood_burner",
        () -> new MultiblockBlockItem(ModBlocks.WOOD_BURNER.get(), new Item.Properties()));

    public static final RegistryObject<Item> LARGE_VEHICLE_DOOR = ITEMS.register("large_vehicle_door",
        () -> new MultiblockBlockItem(ModBlocks.LARGE_VEHICLE_DOOR.get(), new Item.Properties()));

    public static final RegistryObject<Item> ROUND_AIRLOCK_DOOR = ITEMS.register("round_airlock_door",
        () -> new MultiblockBlockItem(ModBlocks.ROUND_AIRLOCK_DOOR.get(), new Item.Properties()));

    public static final RegistryObject<Item> TRANSITION_SEAL = ITEMS.register("transition_seal",
        () -> new MultiblockBlockItem(ModBlocks.TRANSITION_SEAL.get(), new Item.Properties()));

    public static final RegistryObject<Item> SILO_HATCH = ITEMS.register("silo_hatch",
        () -> new MultiblockBlockItem(ModBlocks.SILO_HATCH.get(), new Item.Properties()));

    public static final RegistryObject<Item> SILO_HATCH_LARGE = ITEMS.register("silo_hatch_large",
        () -> new MultiblockBlockItem(ModBlocks.SILO_HATCH_LARGE.get(), new Item.Properties()));

    public static final RegistryObject<Item> QE_CONTAINMENT = ITEMS.register("qe_containment_door",
        () -> new MultiblockBlockItem(ModBlocks.QE_CONTAINMENT.get(), new Item.Properties()));

    public static final RegistryObject<Item> WATER_DOOR = ITEMS.register("water_door",
        () -> new MultiblockBlockItem(ModBlocks.WATER_DOOR.get(), new Item.Properties()));

    public static final RegistryObject<Item> FIRE_DOOR = ITEMS.register("fire_door",
        () -> new MultiblockBlockItem(ModBlocks.FIRE_DOOR.get(), new Item.Properties()));

    public static final RegistryObject<Item> SLIDE_DOOR = ITEMS.register("sliding_blast_door",
        () -> new MultiblockBlockItem(ModBlocks.SLIDE_DOOR.get(), new Item.Properties()));
        
    public static final RegistryObject<Item> SLIDING_SEAL_DOOR = ITEMS.register("sliding_seal_door",
        () -> new MultiblockBlockItem(ModBlocks.SLIDING_SEAL_DOOR.get(), new Item.Properties()));

    public static final RegistryObject<Item> SECURE_ACCESS_DOOR = ITEMS.register("secure_access_door",
        () -> new MultiblockBlockItem(ModBlocks.SECURE_ACCESS_DOOR.get(), new Item.Properties()));

    public static final RegistryObject<Item> QE_SLIDING = ITEMS.register("qe_sliding_door",
        () -> new MultiblockBlockItem(ModBlocks.QE_SLIDING.get(), new Item.Properties()));

    public static final RegistryObject<Item> STAMP_STONE_FLAT = ITEMS.register("stamp_stone_flat",
            () -> new ItemStamp(new Item.Properties(), 32));
    public static final RegistryObject<Item> STAMP_STONE_PLATE = ITEMS.register("stamp_stone_plate",
            () -> new ItemStamp(new Item.Properties(), 32));
    public static final RegistryObject<Item> STAMP_STONE_WIRE = ITEMS.register("stamp_stone_wire",
            () -> new ItemStamp(new Item.Properties(), 32));
    public static final RegistryObject<Item> STAMP_STONE_CIRCUIT = ITEMS.register("stamp_stone_circuit",
            () -> new ItemStamp(new Item.Properties(), 32));


    public static final RegistryObject<Item> BLADE_TEST = ITEMS.register("blade_test",
            () -> new ItemBlades(new Item.Properties()));

    public static final RegistryObject<Item> BLADE_STEEL = ITEMS.register("blade_steel",
            () -> new ItemBlades(new Item.Properties(), 200));

    public static final RegistryObject<Item> BLADE_TITANIUM = ITEMS.register("blade_titanium",
            () -> new ItemBlades(new Item.Properties(), 350));

    public static final RegistryObject<Item> BLADE_ALLOY = ITEMS.register("blade_alloy",
            () -> new ItemBlades(new Item.Properties(), 700));

    // Железные штампы (48 использований)
    public static final RegistryObject<Item> STAMP_IRON_FLAT = ITEMS.register("stamp_iron_flat",
            () -> new ItemStamp(new Item.Properties(), 48));
    public static final RegistryObject<Item> STAMP_IRON_PLATE = ITEMS.register("stamp_iron_plate",
            () -> new ItemStamp(new Item.Properties(), 48));
    public static final RegistryObject<Item> STAMP_IRON_WIRE = ITEMS.register("stamp_iron_wire",
            () -> new ItemStamp(new Item.Properties(), 48));
    public static final RegistryObject<Item> STAMP_IRON_CIRCUIT = ITEMS.register("stamp_iron_circuit",
            () -> new ItemStamp(new Item.Properties(), 48));
    public static final RegistryObject<Item> STAMP_IRON_9 = ITEMS.register("stamp_iron_9",
            () -> new ItemStamp(new Item.Properties(), 48));
    public static final RegistryObject<Item> STAMP_IRON_44 = ITEMS.register("stamp_iron_44",
            () -> new ItemStamp(new Item.Properties(), 48));
    public static final RegistryObject<Item> STAMP_IRON_50 = ITEMS.register("stamp_iron_50",
            () -> new ItemStamp(new Item.Properties(), 48));
    public static final RegistryObject<Item> STAMP_IRON_357 = ITEMS.register("stamp_iron_357",
            () -> new ItemStamp(new Item.Properties(), 48));

    // Стальные штампы (64 использования)
    public static final RegistryObject<Item> STAMP_STEEL_FLAT = ITEMS.register("stamp_steel_flat",
            () -> new ItemStamp(new Item.Properties(), 64));
    public static final RegistryObject<Item> STAMP_STEEL_PLATE = ITEMS.register("stamp_steel_plate",
            () -> new ItemStamp(new Item.Properties(), 64));
    public static final RegistryObject<Item> STAMP_STEEL_WIRE = ITEMS.register("stamp_steel_wire",
            () -> new ItemStamp(new Item.Properties(), 64));
    public static final RegistryObject<Item> STAMP_STEEL_CIRCUIT = ITEMS.register("stamp_steel_circuit",
            () -> new ItemStamp(new Item.Properties(), 64));

    // Титановые штампы (80 использований)
    public static final RegistryObject<Item> STAMP_TITANIUM_FLAT = ITEMS.register("stamp_titanium_flat",
            () -> new ItemStamp(new Item.Properties(), 80));
    public static final RegistryObject<Item> STAMP_TITANIUM_PLATE = ITEMS.register("stamp_titanium_plate",
            () -> new ItemStamp(new Item.Properties(), 80));
    public static final RegistryObject<Item> STAMP_TITANIUM_WIRE = ITEMS.register("stamp_titanium_wire",
            () -> new ItemStamp(new Item.Properties(), 80));
    public static final RegistryObject<Item> STAMP_TITANIUM_CIRCUIT = ITEMS.register("stamp_titanium_circuit",
            () -> new ItemStamp(new Item.Properties(), 80));

    // Обсидиановые штампы (96 использований)
    public static final RegistryObject<Item> STAMP_OBSIDIAN_FLAT = ITEMS.register("stamp_obsidian_flat",
            () -> new ItemStamp(new Item.Properties(), 96));
    public static final RegistryObject<Item> STAMP_OBSIDIAN_PLATE = ITEMS.register("stamp_obsidian_plate",
            () -> new ItemStamp(new Item.Properties(), 96));
    public static final RegistryObject<Item> STAMP_OBSIDIAN_WIRE = ITEMS.register("stamp_obsidian_wire",
            () -> new ItemStamp(new Item.Properties(), 96));
    public static final RegistryObject<Item> STAMP_OBSIDIAN_CIRCUIT = ITEMS.register("stamp_obsidian_circuit",
            () -> new ItemStamp(new Item.Properties(), 96));

    // Desh штампы (бесконечная прочность)
    public static final RegistryObject<Item> STAMP_DESH_FLAT = ITEMS.register("stamp_desh_flat",
            () -> new ItemStamp(new Item.Properties()));
    public static final RegistryObject<Item> STAMP_DESH_PLATE = ITEMS.register("stamp_desh_plate",
            () -> new ItemStamp(new Item.Properties()));
    public static final RegistryObject<Item> STAMP_DESH_WIRE = ITEMS.register("stamp_desh_wire",
            () -> new ItemStamp(new Item.Properties()));
    public static final RegistryObject<Item> STAMP_DESH_CIRCUIT = ITEMS.register("stamp_desh_circuit",
            () -> new ItemStamp(new Item.Properties()));
    public static final RegistryObject<Item> STAMP_DESH_9 = ITEMS.register("stamp_desh_9",
            () -> new ItemStamp(new Item.Properties()));
    public static final RegistryObject<Item> STAMP_DESH_44 = ITEMS.register("stamp_desh_44",
            () -> new ItemStamp(new Item.Properties()));
    public static final RegistryObject<Item> STAMP_DESH_50 = ITEMS.register("stamp_desh_50",
            () -> new ItemStamp(new Item.Properties()));
    public static final RegistryObject<Item> STAMP_DESH_357 = ITEMS.register("stamp_desh_357",
            () -> new ItemStamp(new Item.Properties()));


    //батарейки

    public static final RegistryObject<Item> BATTERY_SCHRABIDIUM = ITEMS.register("battery_schrabidium",
            () -> new ModBatteryItem(
                    new Item.Properties(),
                    1000000,
                    5000,
                    5000
            ));

    // ========== КАРТОФЕЛЬНАЯ И БАЗОВЫЕ ==========
    public static final RegistryObject<Item> BATTERY_POTATO = ITEMS.register("battery_potato",
            () -> new ModBatteryItem(
                    new Item.Properties(),
                    1_000,
                    100,
                    100
            ));

    public static final RegistryObject<Item> BATTERY = ITEMS.register("battery",
            () -> new ModBatteryItem(
                    new Item.Properties(),
                    5000,
                    100,
                    100
            ));

    // ========== КРАСНЫЕ БАТАРЕЙКИ (RED CELL) ==========
    public static final RegistryObject<Item> BATTERY_RED_CELL = ITEMS.register("battery_red_cell",
            () -> new ModBatteryItem(
                    new Item.Properties(),
                    15000,
                    100,
                    100
            ));

    public static final RegistryObject<Item> BATTERY_RED_CELL_6 = ITEMS.register("battery_red_cell_6",
            () -> new ModBatteryItem(
                    new Item.Properties(),
                    90000,
                    100,
                    100
            ));

    public static final RegistryObject<Item> BATTERY_RED_CELL_24 = ITEMS.register("battery_red_cell_24",
            () -> new ModBatteryItem(
                    new Item.Properties(),
                    240000,
                    100,
                    100
            ));

    // ========== ПРОДВИНУТЫЕ БАТАРЕЙКИ (ADVANCED) ==========
    public static final RegistryObject<Item> BATTERY_ADVANCED = ITEMS.register("battery_advanced",
            () -> new ModBatteryItem(
                    new Item.Properties(),
                    20000,
                    500,
                    500
            ));

    public static final RegistryObject<Item> BATTERY_ADVANCED_CELL = ITEMS.register("battery_advanced_cell",
            () -> new ModBatteryItem(
                    new Item.Properties(),
                    60000,
                    500,
                    500
            ));

    public static final RegistryObject<Item> BATTERY_ADVANCED_CELL_4 = ITEMS.register("battery_advanced_cell_4",
            () -> new ModBatteryItem(
                    new Item.Properties(),
                    240000,
                    500,
                    500
            ));

    public static final RegistryObject<Item> BATTERY_ADVANCED_CELL_12 = ITEMS.register("battery_advanced_cell_12",
            () -> new ModBatteryItem(
                    new Item.Properties(),
                    720000,
                    500,
                    500
            ));

    // ========== ЛИТИЕВЫЕ БАТАРЕЙКИ (LITHIUM) ==========
    public static final RegistryObject<Item> BATTERY_LITHIUM = ITEMS.register("battery_lithium",
            () -> new ModBatteryItem(
                    new Item.Properties(),
                    250000,
                    1000,
                    1000
            ));

    public static final RegistryObject<Item> BATTERY_LITHIUM_CELL = ITEMS.register("battery_lithium_cell",
            () -> new ModBatteryItem(
                    new Item.Properties(),
                    750000,
                    1000,
                    1000
            ));

    public static final RegistryObject<Item> BATTERY_LITHIUM_CELL_3 = ITEMS.register("battery_lithium_cell_3",
            () -> new ModBatteryItem(
                    new Item.Properties(),
                    2250000,
                    1000,
                    1000
            ));

    public static final RegistryObject<Item> BATTERY_LITHIUM_CELL_6 = ITEMS.register("battery_lithium_cell_6",
            () -> new ModBatteryItem(
                    new Item.Properties(),
                    4500000,
                    1000,
                    1000
            ));

// ========== ШРАБИДИЕВЫЕ БАТАРЕЙКИ (SCHRABIDIUM) - уже есть ==========

    public static final RegistryObject<Item> BATTERY_SCHRABIDIUM_CELL = ITEMS.register("battery_schrabidium_cell",
            () -> new ModBatteryItem(
                    new Item.Properties(),
                    3000000,
                    5000,
                    5000
            ));

    public static final RegistryObject<Item> BATTERY_SCHRABIDIUM_CELL_2 = ITEMS.register("battery_schrabidium_cell_2",
            () -> new ModBatteryItem(
                    new Item.Properties(),
                    6000000,
                    5000,
                    5000
            ));

    public static final RegistryObject<Item> BATTERY_SCHRABIDIUM_CELL_4 = ITEMS.register("battery_schrabidium_cell_4",
            () -> new ModBatteryItem(
                    new Item.Properties(),
                    12000000,
                    5000,
                    5000
            ));

    // ========== ИСКРОВЫЕ БАТАРЕЙКИ (SPARK) - ЭКСТРЕМАЛЬНЫЕ ==========
    public static final RegistryObject<Item> BATTERY_SPARK = ITEMS.register("battery_spark",
            () -> new ModBatteryItem(
                    new Item.Properties(),
                    100000000,
                    2000000,
                    2000000
            ));

    public static final RegistryObject<Item> BATTERY_TRIXITE = ITEMS.register("battery_trixite",
            () -> new ModBatteryItem(
                    new Item.Properties(),
                    5000000,
                    40000,
                    200000
            ));

    public static final RegistryObject<Item> BATTERY_SPARK_CELL_6 = ITEMS.register("battery_spark_cell_6",
            () -> new ModBatteryItem(
                    new Item.Properties(),
                    600_000_000L,
                    2000000,
                    2000000
            ));

    public static final RegistryObject<Item> BATTERY_SPARK_CELL_25 = ITEMS.register("battery_spark_cell_25",
            () -> new ModBatteryItem(
                    new Item.Properties(),
                    2_500_000_000L,
                    2000000,
                    2000000
            ));

    public static final RegistryObject<Item> BATTERY_SPARK_CELL_100 = ITEMS.register("battery_spark_cell_100",
            () -> new ModBatteryItem(
                    new Item.Properties(),
                    10_000_000_000L,
                    20000000,
                    2000000
            ));

    public static final RegistryObject<Item> BATTERY_SPARK_CELL_1000 = ITEMS.register("battery_spark_cell_1000",
            () -> new ModBatteryItem(
                    new Item.Properties(),
                    100_000_000_000L,
                    20000000,
                    20000000
            ));

    public static final RegistryObject<Item> BATTERY_SPARK_CELL_2500 = ITEMS.register("battery_spark_cell_2500",
            () -> new ModBatteryItem(
                    new Item.Properties(),
                    250_000_000_000L,
                    20000000,
                    20000000
            ));

    public static final RegistryObject<Item> BATTERY_SPARK_CELL_10000 = ITEMS.register("battery_spark_cell_10000",
            () -> new ModBatteryItem(
                    new Item.Properties(),
                    1_000_000_000_000L,
                    200000000,
                    200000000
            ));

    public static final RegistryObject<Item> BATTERY_SPARK_CELL_POWER = ITEMS.register("battery_spark_cell_power",
            () -> new ModBatteryItem(
                    new Item.Properties(),
                    100_000_000_000_000L,
                    200000000,
                    200000000
            ));


    public static final RegistryObject<Item> AIRSTRIKE_TEST = ITEMS.register("airstrike_test",
            () -> new AirstrikeItem(new Item.Properties()));
    public static final RegistryObject<Item> AIRSTRIKE_AGENT= ITEMS.register("airstrike_agent",
            () -> new AirstrikeAgentItem(new Item.Properties()));
    public static final RegistryObject<Item> AIRSTRIKE_HEAVY = ITEMS.register("airstrike_heavy",
            () -> new AirstrikeHeavyItem(new Item.Properties()));
    public static final RegistryObject<Item> AIRSTRIKE_NUKE = ITEMS.register("airstrike_nuke",
            () -> new AirstrikeNukeItem(new Item.Properties()));
    public static final RegistryObject<Item> WIRE_RED_COPPER = ITEMS.register("wire_red_copper",
            () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> WIRE_ADVANCED_ALLOY = ITEMS.register("wire_advanced_alloy",
            () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> WIRE_ALUMINIUM = ITEMS.register("wire_aluminium",
            () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> WIRE_COPPER = ITEMS.register("wire_copper",
            () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> WIRE_CARBON = ITEMS.register("wire_carbon",
            () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> WIRE_FINE = ITEMS.register("wire_fine",
            () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> WIRE_GOLD = ITEMS.register("wire_gold",
            () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> WIRE_MAGNETIZED_TUNGSTEN = ITEMS.register("wire_magnetized_tungsten",
            () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> WIRE_SCHRABIDIUM = ITEMS.register("wire_schrabidium",
            () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> WIRE_TUNGSTEN = ITEMS.register("wire_tungsten",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> SCREWDRIVER = ITEMS.register("screwdriver",
            () -> new Item(new Item.Properties().stacksTo(1))); // В стаке только 1 штука


        // Медленный источник (500 mB/t)
        public static final RegistryObject<Item> INFINITE_WATER_500 = ITEMS.register("inf_water",
                () -> new InfiniteWaterItem(new Item.Properties().stacksTo(1), 500));

        // Быстрый источник (5000 mB/t)
        public static final RegistryObject<Item> INFINITE_WATER_5000 = ITEMS.register("inf_water_mk2",
                () -> new InfiniteWaterItem(new Item.Properties().stacksTo(1), 5000));



    //=============================== ВЁДРА ДЛЯ ЖИДКОСТЕЙ ===============================//

    public static final RegistryObject<Item> CRUDE_OIL_BUCKET = ITEMS.register("bucket_crude_oil",
            () -> new BucketItem(ModFluids.CRUDE_OIL_SOURCE,
                    new Item.Properties()
                            .craftRemainder(Items.BUCKET) // Возвращает пустое ведро при крафте
                            .stacksTo(1))); // Ведра не стакаются

    


    // Метод для регистрации всех предметов, вызывается в основном классе мода.
    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
