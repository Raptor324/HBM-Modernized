package com.hbm_m.item;

// Класс для регистрации всех предметов мода.
// Использует DeferredRegister для отложенной регистрации. Здесь так же регистрируются моды для брони.
// Слитки регистрируются автоматически на основе перечисления ModIngots.

import java.util.EnumMap;
import java.util.Map;

import com.hbm_m.armormod.item.ItemModHealth;
import com.hbm_m.armormod.item.ItemModRadProtection;
import com.hbm_m.block.ModBlocks;
import com.hbm_m.effect.ModEffects;
import com.hbm_m.entity.ModEntities;
import com.hbm_m.entity.grenades.GrenadeType;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.main.MainRegistry;
import com.hbm_m.multiblock.MultiblockBlockItem;
import com.hbm_m.sound.ModSounds;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.*;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;


public class ModItems {
    // Создаем отложенный регистратор для предметов.
    // Это стандартный способ регистрации объектов в Forge.
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, RefStrings.MODID);

    // АВТОМАТИЧЕСКАЯ РЕГИСТРАЦИЯ СЛИТКОВ 
    // 1. Создаем карту для хранения всех RegistryObject'ов наших слитков
    public static final Map<ModIngots, RegistryObject<Item>> INGOTS = new EnumMap<>(ModIngots.class);

    // 2. Используем статический блок для заполнения карты
    static {
        for (ModIngots ingot : ModIngots.values()) {
            // Регистрируем предмет. Имя будет, например, "ingot_uranium"
            // Важно: мы стандартизируем имена, добавляя префикс "ingot_"
            RegistryObject<Item> registeredItem;

            // Пример того, как сделать один из слитков особенным
            if (ingot == ModIngots.URANIUM) {
                registeredItem = ITEMS.register(ingot.getName() + "_ingot", // Имя в реестре: uranium_ingot
                        () -> new RadioactiveItem(new Item.Properties()));
            } else {
                registeredItem = ITEMS.register(ingot.getName() + "_ingot", // Имя в реестре: steel_ingot
                        () -> new Item(new Item.Properties()));
            }
            
            // Кладём зарегистрированный объект в нашу карту
            INGOTS.put(ingot, registeredItem);
        }
    }
    
    
    // УДОБНЫЙ МЕТОД ДЛЯ ПОЛУЧЕНИЯ СЛИТКА 
    public static RegistryObject<Item> getIngot(ModIngots ingot) {
        return INGOTS.get(ingot);
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
            () -> new ShovelItem(ModToolTiers.ALLOY, 0, 0, new Item.Properties()));
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

    public static final RegistryObject<Item> GRENADEIF = ITEMS.register("grenadeif",
        () -> new GrenadeItem(new Item.Properties(), GrenadeType.IF, ModEntities.GRENADEIF_PROJECTILE));


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
    public static final RegistryObject<Item> STEEL_COUNTER = ITEMS.register("steel_counter",
            () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> STEEL_CASING = ITEMS.register("steel_casing",
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

    public static final RegistryObject<Item> PLATE_POLYMER = ITEMS.register("plate_polymer",
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


    //штампы для пресса

    public static final RegistryObject<Item> STAMP_STONE_FLAT = ITEMS.register("stamp_stone_flat",
            () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> STAMP_STONE_PLATE = ITEMS.register("stamp_stone_plate",
            () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> STAMP_STONE_WIRE = ITEMS.register("stamp_stone_wire",
            () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> STAMP_STONE_CIRCUIT = ITEMS.register("stamp_stone_circuit",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> STAMP_IRON_FLAT = ITEMS.register("stamp_iron_flat",
            () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> STAMP_IRON_PLATE = ITEMS.register("stamp_iron_plate",
            () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> STAMP_IRON_WIRE = ITEMS.register("stamp_iron_wire",
            () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> STAMP_IRON_CIRCUIT = ITEMS.register("stamp_iron_circuit",
            () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> STAMP_IRON_9 = ITEMS.register("stamp_iron_9",
            () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> STAMP_IRON_44 = ITEMS.register("stamp_iron_44",
            () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> STAMP_IRON_50 = ITEMS.register("stamp_iron_50",
            () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> STAMP_IRON_357 = ITEMS.register("stamp_iron_357",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> STAMP_STEEL_FLAT = ITEMS.register("stamp_steel_flat",
            () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> STAMP_STEEL_PLATE = ITEMS.register("stamp_steel_plate",
            () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> STAMP_STEEL_WIRE = ITEMS.register("stamp_steel_wire",
            () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> STAMP_STEEL_CIRCUIT = ITEMS.register("stamp_steel_circuit",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> STAMP_TITANIUM_FLAT = ITEMS.register("stamp_titanium_flat",
            () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> STAMP_TITANIUM_PLATE = ITEMS.register("stamp_titanium_plate",
            () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> STAMP_TITANIUM_WIRE = ITEMS.register("stamp_titanium_wire",
            () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> STAMP_TITANIUM_CIRCUIT = ITEMS.register("stamp_titanium_circuit",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> STAMP_OBSIDIAN_FLAT = ITEMS.register("stamp_obsidian_flat",
            () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> STAMP_OBSIDIAN_PLATE = ITEMS.register("stamp_obsidian_plate",
            () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> STAMP_OBSIDIAN_WIRE = ITEMS.register("stamp_obsidian_wire",
            () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> STAMP_OBSIDIAN_CIRCUIT = ITEMS.register("stamp_obsidian_circuit",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> STAMP_DESH_FLAT = ITEMS.register("stamp_desh_flat",
            () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> STAMP_DESH_PLATE = ITEMS.register("stamp_desh_plate",
            () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> STAMP_DESH_WIRE = ITEMS.register("stamp_desh_wire",
            () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> STAMP_DESH_CIRCUIT = ITEMS.register("stamp_desh_circuit",
            () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> STAMP_DESH_9 = ITEMS.register("stamp_desh_9",
            () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> STAMP_DESH_44 = ITEMS.register("stamp_desh_44",
            () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> STAMP_DESH_50 = ITEMS.register("stamp_desh_50",
            () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> STAMP_DESH_357 = ITEMS.register("stamp_desh_357",
            () -> new Item(new Item.Properties()));


    public static final RegistryObject<Item> WIRE_RED_COPPER = ITEMS.register("wire_red_copper",
            () -> new Item(new Item.Properties()));


    // Здесь мы регистрируем предмет-блок батареи для машин, поддерживающий хранение энергии через Forge Energy
    public static final RegistryObject<Item> MACHINE_BATTERY = ITEMS.register("machine_battery",
            () -> new MachineBatteryBlockItem(ModBlocks.MACHINE_BATTERY.get(), new Item.Properties(), BATTERY_CAPACITY));

    


    // Метод для регистрации всех предметов, вызывается в основном классе мода.
    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
