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
import com.hbm_m.lib.RefStrings;
import com.hbm_m.multiblock.MultiblockBlockItem;
import com.hbm_m.sound.ModSounds;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.Item;
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

    // Регистрируем наш меч.
    // RegistryObject<Item> будет содержать ссылку на зарегистрированный предмет.
    public static final RegistryObject<Item> ALLOY_SWORD = ITEMS.register("alloy_sword",
            () -> new AlloySword()); // Имя файла меча будет "alloy_sword"
            

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
                    new Item.Properties()
            )
    );
    public static final RegistryObject<Item> TEMPLATE_FOLDER = ITEMS.register("template_folder",
            () -> new ItemTemplateFolder(
                    new Item.Properties()
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

    public static final RegistryObject<Item> FIREBRICK = ITEMS.register("firebrick",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> LIGNITE = ITEMS.register("lignite",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> CINNABAR = ITEMS.register("cinnabar",
            () -> new Item(new Item.Properties()));





    public static final RegistryObject<Item> MACHINE_ASSEMBLER = ITEMS.register("machine_assembler",
            () -> new MultiblockBlockItem(ModBlocks.MACHINE_ASSEMBLER.get(), new Item.Properties()));
            
    public static final RegistryObject<Item> ADVANCED_ASSEMBLY_MACHINE = ITEMS.register("advanced_assembly_machine",
        () -> new MultiblockBlockItem(ModBlocks.ADVANCED_ASSEMBLY_MACHINE.get(), new Item.Properties()));

    public static final RegistryObject<Item> MACHINE_BATTERY = ITEMS.register("machine_battery",
            () -> new MachineBatteryBlockItem(ModBlocks.MACHINE_BATTERY.get(), new Item.Properties(), BATTERY_CAPACITY));

    


    // Метод для регистрации всех предметов, вызывается в основном классе мода.
    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
