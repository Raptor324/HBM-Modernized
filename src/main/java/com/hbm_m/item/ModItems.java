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

import com.hbm_m.block.ModBlocks;
import com.hbm_m.item.custom.crates.CrateItem;
import com.hbm_m.item.custom.food.ItemConserve;
import com.hbm_m.item.custom.food.ItemEnergyDrink;
import com.hbm_m.item.custom.food.ModFoods;
import com.hbm_m.item.custom.tools_and_armor.ScrewdriverItem;
import com.hbm_m.item.tags_and_tiers.ModIngots;
import com.hbm_m.item.tags_and_tiers.ModPowders;
import com.hbm_m.item.tags_and_tiers.RadioactiveItem;
import com.hbm_m.multiblock.DoorBlockItem;
import com.hbm_m.multiblock.MultiblockBlockItem;
import com.hbm_m.sound.ModSounds;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;


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
        // 1. СЛИТКИ (ВСЕГДА)  OK
        for (ModIngots ingot : ModIngots.values()) {
            RegistryObject<Item> registeredItem;
            if (ingot == ModIngots.URANIUM) {
                registeredItem = ITEMS.register(ingot.getName() + "_ingot", () -> new RadioactiveItem(new Item.Properties()));
            } else {
                registeredItem = ITEMS.register(ingot.getName() + "_ingot", () -> new Item(new Item.Properties()));
            }
            INGOTS.put(ingot, registeredItem);
        }

        // 2. ModPowders (ТОЛЬКО ИЗ ENABLED_MODPOWDERS)  ИСПРАВЛЕНО!
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

        // 3. Порошки из слитков (ТОЛЬКО ИЗ ENABLED_INGOT_POWDERS)  ИСПРАВЛЕНО!
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

            // Маленький порошок  OK
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

    public static final RegistryObject<Item> CINNABAR = ITEMS.register("cinnabar",
            () -> new Item(new Item.Properties()));



    // Crystals (auto-generated from textures/crystall/*.png)
    public static final RegistryObject<Item> CRYSTAL_ALUMINIUM = ITEMS.register("crystal_aluminium",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> CRYSTAL_BERYLLIUM = ITEMS.register("crystal_beryllium",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> CRYSTAL_CHARRED = ITEMS.register("crystal_charred",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> CRYSTAL_CINNEBAR = ITEMS.register("crystal_cinnebar",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> CRYSTAL_COAL = ITEMS.register("crystal_coal",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> CRYSTAL_COBALT = ITEMS.register("crystal_cobalt",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> CRYSTAL_COPPER = ITEMS.register("crystal_copper",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> CRYSTAL_DIAMOND = ITEMS.register("crystal_diamond",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> CRYSTAL_FLUORITE = ITEMS.register("crystal_fluorite",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> CRYSTAL_GOLD = ITEMS.register("crystal_gold",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> CRYSTAL_HARDENED = ITEMS.register("crystal_hardened",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> CRYSTAL_HORN = ITEMS.register("crystal_horn",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> CRYSTAL_IRON = ITEMS.register("crystal_iron",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> CRYSTAL_LAPIS = ITEMS.register("crystal_lapis",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> CRYSTAL_LEAD = ITEMS.register("crystal_lead",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> CRYSTAL_LITHIUM = ITEMS.register("crystal_lithium",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> CRYSTAL_NITER = ITEMS.register("crystal_niter",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> CRYSTAL_OSMIRIDIUM = ITEMS.register("crystal_osmiridium",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> CRYSTAL_PHOSPHORUS = ITEMS.register("crystal_phosphorus",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> CRYSTAL_PLUTONIUM = ITEMS.register("crystal_plutonium",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> CRYSTAL_PULSAR = ITEMS.register("crystal_pulsar",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> CRYSTAL_RARE = ITEMS.register("crystal_rare",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> CRYSTAL_REDSTONE = ITEMS.register("crystal_redstone",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> CRYSTAL_SCHRABIDIUM = ITEMS.register("crystal_schrabidium",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> CRYSTAL_SCHRARANIUM = ITEMS.register("crystal_schraranium",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> CRYSTAL_STARMETAL = ITEMS.register("crystal_starmetal",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> CRYSTAL_SULFUR = ITEMS.register("crystal_sulfur",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> CRYSTAL_THORIUM = ITEMS.register("crystal_thorium",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> CRYSTAL_TITANIUM = ITEMS.register("crystal_titanium",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> CRYSTAL_TRIXITE = ITEMS.register("crystal_trixite",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> CRYSTAL_TUNGSTEN = ITEMS.register("crystal_tungsten",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> CRYSTAL_URANIUM = ITEMS.register("crystal_uranium",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> CRYSTAL_VIRUS = ITEMS.register("crystal_virus",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> CRYSTAL_XEN = ITEMS.register("crystal_xen",
            () -> new Item(new Item.Properties()));




    // Здесь мы регистрируем мультиблочные структуры для того, чтобы MultiblockBlockItem при установке мог обрабатывать их на наличие препятствующих блоков.


    public static final RegistryObject<Item> LARGE_VEHICLE_DOOR = ITEMS.register("large_vehicle_door",
        () -> new DoorBlockItem(ModBlocks.LARGE_VEHICLE_DOOR.get(), new Item.Properties()));

    public static final RegistryObject<Item> ROUND_AIRLOCK_DOOR = ITEMS.register("round_airlock_door",
        () -> new DoorBlockItem(ModBlocks.ROUND_AIRLOCK_DOOR.get(), new Item.Properties()));

    public static final RegistryObject<Item> TRANSITION_SEAL = ITEMS.register("transition_seal",
        () -> new DoorBlockItem(ModBlocks.TRANSITION_SEAL.get(), new Item.Properties()));

    public static final RegistryObject<Item> SILO_HATCH = ITEMS.register("silo_hatch",
        () -> new DoorBlockItem(ModBlocks.SILO_HATCH.get(), new Item.Properties()));

    public static final RegistryObject<Item> SILO_HATCH_LARGE = ITEMS.register("silo_hatch_large",
        () -> new DoorBlockItem(ModBlocks.SILO_HATCH_LARGE.get(), new Item.Properties()));

    public static final RegistryObject<Item> QE_CONTAINMENT = ITEMS.register("qe_containment_door",
        () -> new DoorBlockItem(ModBlocks.QE_CONTAINMENT.get(), new Item.Properties()));

    public static final RegistryObject<Item> WATER_DOOR = ITEMS.register("water_door",
        () -> new DoorBlockItem(ModBlocks.WATER_DOOR.get(), new Item.Properties()));

    public static final RegistryObject<Item> FIRE_DOOR = ITEMS.register("fire_door",
        () -> new DoorBlockItem(ModBlocks.FIRE_DOOR.get(), new Item.Properties()));

    public static final RegistryObject<Item> SLIDE_DOOR = ITEMS.register("sliding_blast_door",
        () -> new DoorBlockItem(ModBlocks.SLIDE_DOOR.get(), new Item.Properties()));
        
    public static final RegistryObject<Item> SLIDING_SEAL_DOOR = ITEMS.register("sliding_seal_door",
        () -> new DoorBlockItem(ModBlocks.SLIDING_SEAL_DOOR.get(), new Item.Properties()));

    public static final RegistryObject<Item> SECURE_ACCESS_DOOR = ITEMS.register("secure_access_door",
        () -> new DoorBlockItem(ModBlocks.SECURE_ACCESS_DOOR.get(), new Item.Properties()));

    public static final RegistryObject<Item> QE_SLIDING = ITEMS.register("qe_sliding_door",
        () -> new DoorBlockItem(ModBlocks.QE_SLIDING.get(), new Item.Properties()));

    public static final RegistryObject<Item> VAULT_DOOR = ITEMS.register("vault_door",
        () -> new DoorBlockItem(ModBlocks.VAULT_DOOR.get(), new Item.Properties()));


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
            () -> new ScrewdriverItem(new Item.Properties().stacksTo(1)));



    //=============================== ВЁДРА ДЛЯ ЖИДКОСТЕЙ ===============================//


    // Метод для регистрации всех предметов, вызывается в основном классе мода.
    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
