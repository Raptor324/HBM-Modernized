package com.hbm_m.block;

import com.hbm_m.api.energy.ConverterBlock;
import com.hbm_m.block.machine.MachineAdvancedAssemblerBlock;
import com.hbm_m.block.machine.MachineAssemblerBlock;
import com.hbm_m.block.machine.MachineBatteryBlock;
import com.hbm_m.block.machine.MachinePressBlock;
import com.hbm_m.block.machine.MachineShredderBlock;
import com.hbm_m.block.machine.MachineWoodBurnerBlock;
import com.hbm_m.block.machine.UniversalMachinePartBlock;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.item.ModItems;
import com.hbm_m.item.ModIngots;

import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.minecraft.util.valueproviders.UniformInt;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, RefStrings.MODID);

    public static final RegistryObject<Block> GEIGER_COUNTER_BLOCK = registerBlock("geiger_counter_block",
            () -> new GeigerCounterBlock(Block.Properties.copy(Blocks.IRON_BLOCK).noOcclusion()));
            
    private static final BlockBehaviour.Properties TABLE_PROPERTIES =
                BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(5.0F, 6.0F).sound(SoundType.METAL).requiresCorrectToolForDrops();
    private static final BlockBehaviour.Properties ANVIL_PROPERTIES =
            BlockBehaviour.Properties.copy(Blocks.ANVIL).sound(SoundType.ANVIL).noOcclusion();
    
    // Стандартные свойства для блоков слитков
    private static final BlockBehaviour.Properties INGOT_BLOCK_PROPERTIES = 
            BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).strength(5.0F, 6.0F).sound(SoundType.METAL).requiresCorrectToolForDrops();

    public static final List<RegistryObject<Block>> BATTERY_BLOCKS = new ArrayList<>();

    // Вспомогательный метод для регистрации батареек
    private static RegistryObject<Block> registerBattery(String name, long capacity) {
        // 1. Регистрируем БЛОК
        RegistryObject<Block> batteryBlock = BLOCKS.register(name,
                () -> new MachineBatteryBlock(Block.Properties.of().strength(5.0f).requiresCorrectToolForDrops(), capacity));

        // 2. Регистрируем ПРЕДМЕТ (MachineBatteryBlockItem)
        // Обращаемся к ModItems.ITEMS напрямую
        ModItems.ITEMS.register(name,
                () -> new com.hbm_m.item.MachineBatteryBlockItem(batteryBlock.get(), new Item.Properties(), capacity));

        // 3. Добавляем в список для TileEntity
        BATTERY_BLOCKS.add(batteryBlock);

        return batteryBlock;
    }

    // Регистрируем батарейки
    public static final RegistryObject<Block> MACHINE_BATTERY = registerBattery("machine_battery", 1_000_000L);
    public static final RegistryObject<Block> MACHINE_BATTERY_LITHIUM = registerBattery("machine_battery_lithium", 50_000_000L);
    public static final RegistryObject<Block> MACHINE_BATTERY_SCHRABIDIUM = registerBattery("machine_battery_schrabidium", 25_000_000_000L);
    public static final RegistryObject<Block> MACHINE_BATTERY_DINEUTRONIUM = registerBattery("machine_battery_dineutronium", 1_000_000_000_000L);
    // АВТОМАТИЧЕСКАЯ РЕГИСТРАЦИЯ БЛОКОВ СЛИТКОВ
    // Карта для хранения всех RegistryObject'ов блоков слитков
    public static final Map<ModIngots, RegistryObject<Block>> INGOT_BLOCKS = new EnumMap<>(ModIngots.class);
    
    // Статический блок для автоматической регистрации блоков слитков
    static {
        for (ModIngots ingot : ModIngots.values()) {
            String blockName = ingot.getName() + "_block";
            RegistryObject<Block> registeredBlock;
            
            // Определяем, должен ли блок быть радиоактивным
            if (isRadioactiveIngot(ingot)) {
                registeredBlock = registerBlock(blockName,
                        () -> new RadioactiveBlock(INGOT_BLOCK_PROPERTIES));
            } else {
                registeredBlock = registerBlock(blockName,
                        () -> new Block(INGOT_BLOCK_PROPERTIES));
            }
            
            // Кладём зарегистрированный блок в карту
            INGOT_BLOCKS.put(ingot, registeredBlock);
        }
    }
    
    /**
     * Определяет, должен ли слиток быть радиоактивным блоком
     */
    private static boolean isRadioactiveIngot(ModIngots ingot) {
        String name = ingot.getName().toLowerCase();
        // Проверяем по названию, является ли слиток радиоактивным
        return name.contains("uranium") || 
               name.contains("plutonium") || 
               name.contains("thorium") || 
               name.contains("actinium") || 
               name.contains("polonium") || 
               name.contains("neptunium") || 
               name.contains("americium") || 
               name.contains("curium") ||
               name.contains("berkelium") ||
               name.contains("californium") ||
               name.contains("einsteinium") ||
               name.contains("fermium") ||
               name.contains("mendelevium") ||
               name.contains("nobelium") ||
               name.contains("lawrencium") ||
               name.contains("radium") ||
               name.contains("radon") ||
               name.contains("francium") ||
               name.contains("ra226") ||
               name.contains("co60") ||
               name.contains("sr90") ||
               name.contains("am241") ||
               name.contains("am242") ||
               name.contains("u233") ||
               name.contains("u235") ||
               name.contains("u238") ||
               name.contains("th232") ||
               name.contains("pu238") ||
               name.contains("pu239") ||
               name.contains("pu240") ||
               name.contains("pu241");
    }
    
    // Удобный метод для получения блока слитка
    public static RegistryObject<Block> getIngotBlock(ModIngots ingot) {
        return INGOT_BLOCKS.get(ingot);
    }
    
    // Старые блоки оставлены для обратной совместимости, но теперь они ссылаются на автоматически созданные
    public static final RegistryObject<Block> URANIUM_BLOCK = getIngotBlock(ModIngots.URANIUM);
    public static final RegistryObject<Block> PLUTONIUM_BLOCK = getIngotBlock(ModIngots.PLUTONIUM);
    public static final RegistryObject<Block> PLUTONIUM_FUEL_BLOCK = getIngotBlock(ModIngots.PLUTONIUM_FUEL);
    
    // Специальные блоки, которые не являются стандартными блоками слитков
    public static final RegistryObject<Block> POLONIUM210_BLOCK = registerBlock("polonium210_block",
            () -> new RadioactiveBlock(INGOT_BLOCK_PROPERTIES));

    public static final RegistryObject<Block> URANIUM_ORE = registerBlock("uranium_ore",
            () -> new DropExperienceBlock(BlockBehaviour.Properties.copy(Blocks.STONE)
                    .strength(3.0F, 3.0F).requiresCorrectToolForDrops(),
                    UniformInt.of(2, 5))); // Будет выпадать 2-5 единиц опыта

    public static final RegistryObject<Block> WASTE_GRASS = registerBlock("waste_grass",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.DIRT).sound(SoundType.GRAVEL)));
            
    public static final RegistryObject<Block> WASTE_LEAVES = registerBlock("waste_leaves",
            () -> new LeavesBlock(BlockBehaviour.Properties.copy(Blocks.OAK_LEAVES).noOcclusion()));

    public static final RegistryObject<Block> WIRE_COATED = registerBlock("wire_coated",
            () -> new WireBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).noOcclusion()));


    //---------------------------<СТАНКИ>-------------------------------------

    public static final RegistryObject<Block> ANVIL_IRON = registerAnvil("anvil_iron", AnvilTier.IRON); // 1 уровень
    public static final RegistryObject<Block> ANVIL_LEAD = registerAnvil("anvil_lead", AnvilTier.IRON); // 1 уровень
    public static final RegistryObject<Block> ANVIL_STEEL = registerAnvil("anvil_steel", AnvilTier.STEEL); // 2 уровень
    public static final RegistryObject<Block> ANVIL_DESH = registerAnvil("anvil_desh", AnvilTier.OIL); // 3 уровень
    public static final RegistryObject<Block> ANVIL_FERROURANIUM = registerAnvil("anvil_ferrouranium", AnvilTier.NUCLEAR); // 4 уровень
    public static final RegistryObject<Block> ANVIL_SATURNITE = registerAnvil("anvil_saturnite", AnvilTier.RBMK); // 5 уровень
    public static final RegistryObject<Block> ANVIL_BISMUTH_BRONZE = registerAnvil("anvil_bismuth_bronze", AnvilTier.RBMK); // 5 уровень
    public static final RegistryObject<Block> ANVIL_ARSENIC_BRONZE = registerAnvil("anvil_arsenic_bronze", AnvilTier.RBMK); // 5 уровень
    public static final RegistryObject<Block> ANVIL_SCHRABIDATE = registerAnvil("anvil_schrabidate", AnvilTier.FUSION); // 6 уровень
    public static final RegistryObject<Block> ANVIL_DNT = registerAnvil("anvil_dnt", AnvilTier.PARTICLE); // 7 уровень
    public static final RegistryObject<Block> ANVIL_OSMIRIDIUM = registerAnvil("anvil_osmiridium", AnvilTier.GERALD); // 8 уровень
    public static final RegistryObject<Block> ANVIL_MURKY = registerAnvil("anvil_murky", AnvilTier.MURKY); // 1916169 уровень

    public static List<RegistryObject<Block>> getAnvilBlocks() {
        return List.of(
                ANVIL_IRON,
                ANVIL_LEAD,
                ANVIL_STEEL,
                ANVIL_DESH,
                ANVIL_FERROURANIUM,
                ANVIL_SATURNITE,
                ANVIL_BISMUTH_BRONZE,
                ANVIL_ARSENIC_BRONZE,
                ANVIL_SCHRABIDATE,
                ANVIL_DNT,
                ANVIL_OSMIRIDIUM,
                ANVIL_MURKY
        );
    }

    public static final RegistryObject<Block> CONVERTER_BLOCK = registerBlock("converter_block",
            () -> new ConverterBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).noOcclusion()));

    public static final RegistryObject<Block> BLAST_FURNACE = registerBlock("blast_furnace",
            () -> new BlastFurnaceBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK)
                    .strength(4.0f, 4.0f)
                    .sound(SoundType.STONE)
                    .lightLevel(state -> state.getValue(BlastFurnaceBlock.LIT) ? 15 : 0)));

    public static final RegistryObject<Block> BLAST_FURNACE_EXTENSION = registerBlock("blast_furnace_extension",
            () -> new BlastFurnaceExtensionBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK)
                    .strength(3.0f, 4.0f)
                    .sound(SoundType.METAL)
                    .noOcclusion()));

    public static final RegistryObject<Block> PRESS = registerBlockWithoutItem("press",
            () -> new MachinePressBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).strength(4.0f, 4.0f).sound(SoundType.METAL).noOcclusion()));

    public static final RegistryObject<Block> WOOD_BURNER = registerBlockWithoutItem("wood_burner",
            () -> new MachineWoodBurnerBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).strength(4.0f, 4.0f).sound(SoundType.METAL).noOcclusion()));

	public static final RegistryObject<Block> ARMOR_TABLE = registerBlock("armor_table",
            () -> new ArmorTableBlock(TABLE_PROPERTIES));

    public static final RegistryObject<Block> SHREDDER = registerBlock("shredder",
            () -> new MachineShredderBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).noOcclusion()));

    public static final RegistryObject<Block> SWITCH = registerBlock("switch",
            () -> new SwitchBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).noOcclusion()));


    public static final RegistryObject<Block> MACHINE_ASSEMBLER = registerBlockWithoutItem("machine_assembler",
        () -> new MachineAssemblerBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).strength(2.0f).noOcclusion()));

    public static final RegistryObject<Block> ADVANCED_ASSEMBLY_MACHINE = registerBlockWithoutItem("advanced_assembly_machine",
        () -> new MachineAdvancedAssemblerBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).strength(2.0f).noOcclusion()));

    public static final RegistryObject<Block> UNIVERSAL_MACHINE_PART = registerBlockWithoutItem("universal_machine_part",
        () -> new UniversalMachinePartBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).strength(5.0f).noOcclusion().noParticlesOnBreak()));


    // public static final RegistryObject<Block> FLUID_TANK = registerBlockWithoutItem("fluid_tank",
    //     () -> new Block(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).strength(4.0f, 4.0f).sound(SoundType.METAL).noOcclusion()));

    //---------------------------<БЛОКИ>-------------------------------------
    public static final RegistryObject<Block> REINFORCED_STONE = registerBlock("reinforced_stone",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));

    public static final RegistryObject<Block> REINFORCED_GLASS = registerBlock("reinforced_glass",
            () -> new GlassBlock(BlockBehaviour.Properties.copy(Blocks.GLASS).strength(4.0F, 12.0F)));

    public static final RegistryObject<Block> CRATE = registerBlock("crate",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.OAK_WOOD).strength(1.0f, 1.0f).requiresCorrectToolForDrops()));
    public static final RegistryObject<Block> CRATE_LEAD = registerBlock("crate_lead",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.OAK_WOOD).strength(1.0f, 1.0f).requiresCorrectToolForDrops()));
    public static final RegistryObject<Block> CRATE_METAL = registerBlock("crate_metal",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.OAK_WOOD).strength(1.0f, 1.0f).requiresCorrectToolForDrops()));
    public static final RegistryObject<Block> CRATE_WEAPON = registerBlock("crate_weapon",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.OAK_WOOD).strength(1.0f, 1.0f).requiresCorrectToolForDrops()));

    public static final RegistryObject<Block> CONCRETE_HAZARD = registerBlock("concrete_hazard",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));
    public static final RegistryObject<Block> CONCRETE_HAZARD_STAIRS = registerBlock("concrete_hazard_stairs",
            () -> new StairBlock(() -> ModBlocks.CONCRETE_HAZARD.get().defaultBlockState(),
                    BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).sound(SoundType.STONE)));
    public static final RegistryObject<Block> CONCRETE_HAZARD_SLAB = registerBlock("concrete_hazard_slab",
            () -> new SlabBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).sound(SoundType.STONE)));

    public static final RegistryObject<Block> BRICK_CONCRETE = registerBlock("brick_concrete",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));
    public static final RegistryObject<Block> BRICK_CONCRETE_STAIRS = registerBlock("brick_concrete_stairs",
            () -> new StairBlock(() -> ModBlocks.BRICK_CONCRETE.get().defaultBlockState(),
                    BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).sound(SoundType.STONE)));
    public static final RegistryObject<Block> BRICK_CONCRETE_SLAB = registerBlock("brick_concrete_slab",
            () -> new SlabBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).sound(SoundType.STONE)));

    public static final RegistryObject<Block> CONCRETE_MOSSY = registerBlock("concrete_mossy",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));
    public static final RegistryObject<Block> CONCRETE_MOSSY_STAIRS = registerBlock("concrete_mossy_stairs",
            () -> new StairBlock(() -> ModBlocks.CONCRETE_MOSSY.get().defaultBlockState(),
                    BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).sound(SoundType.STONE)));
    public static final RegistryObject<Block> CONCRETE_MOSSY_SLAB = registerBlock("concrete_mossy_slab",
            () -> new SlabBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).sound(SoundType.STONE)));

    public static final RegistryObject<Block> CONCRETE  = registerBlock("concrete",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));
    public static final RegistryObject<Block> CONCRETE_STAIRS = registerBlock("concrete_stairs",
            () -> new StairBlock(() -> ModBlocks.CONCRETE.get().defaultBlockState(),
                    BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).sound(SoundType.STONE)));
    public static final RegistryObject<Block> CONCRETE_SLAB = registerBlock("concrete_slab",
            () -> new SlabBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).sound(SoundType.STONE)));

    public static final RegistryObject<Block> CONCRETE_CRACKED  = registerBlock("concrete_cracked",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));
    public static final RegistryObject<Block> CONCRETE_CRACKED_STAIRS = registerBlock("concrete_cracked_stairs",
            () -> new StairBlock(() -> ModBlocks.CONCRETE_CRACKED.get().defaultBlockState(),
                    BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).sound(SoundType.STONE)));
    public static final RegistryObject<Block> CONCRETE_CRACKED_SLAB = registerBlock("concrete_cracked_slab",
            () -> new SlabBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).sound(SoundType.STONE)));

    public static final RegistryObject<Block> CONCRETE_VENT  = registerBlock("concrete_vent",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));


    public static final RegistryObject<Block> DET_MINER = registerBlock("det_miner",
            () -> new DetMinerBlock(BlockBehaviour.Properties.of()
                    .strength(0.5F, 6.0F) // Прочность блока (как у камня или земли)
                    .sound(SoundType.STONE) // Звук при разрушении
                    .requiresCorrectToolForDrops() // Требует правильного инструмента для лута (как руды)
                    // .noOcclusion() // <--- ЭТО ОЧЕНЬ ВАЖНО! НЕ ИСПОЛЬЗУЙТЕ ЭТО ДЛЯ БЛОКОВ, КОТОРЫЕ ДОЛЖНЫ РЕАГИРОВАТЬ НА РЕДСТОУН
                    //                    // noOcclusion делает блок "неполным" или "прозрачным" для редстоуна.
                    //                    // Если ваш блок должен быть твердым и проводить редстоун, то эти свойства должны быть по умолчанию.
                    // .isRedstoneConductor((state, level, pos) -> true) // Можно явно указать, что блок проводит редстоун
                    // .isViewBlocking((state, level, pos) -> true) // Блок блокирует обзор
            ));




    public static final RegistryObject<Block> GIGA_DET = registerBlock("giga_det",
            () -> new GigaDetBlock(BlockBehaviour.Properties.of()
                    .strength(0.5F, 6.0F) // Прочность блока (как у камня или земли)
                    .sound(SoundType.WOOD) // Звук при разрушении
                    .requiresCorrectToolForDrops() // Требует правильного инструмента для лута (как руды)
                    // .noOcclusion() // <--- ЭТО ОЧЕНЬ ВАЖНО! НЕ ИСПОЛЬЗУЙТЕ ЭТО ДЛЯ БЛОКОВ, КОТОРЫЕ ДОЛЖНЫ РЕАГИРОВАТЬ НА РЕДСТОУН
                    //                    // noOcclusion делает блок "неполным" или "прозрачным" для редстоуна.
                    //                    // Если ваш блок должен быть твердым и проводить редстоун, то эти свойства должны быть по умолчанию.
                    // .isRedstoneConductor((state, level, pos) -> true) // Можно явно указать, что блок проводит редстоун
                    // .isViewBlocking((state, level, pos) -> true) // Блок блокирует обзор
            ));


    public static final RegistryObject<Block> EXPLOSIVE_CHARGE = registerBlock("explosive_charge",
            () -> new ExplosiveChargeBlock(BlockBehaviour.Properties.of()
                    .strength(0.5F, 6.0F) // Прочность блока (как у камня или земли)
                    .sound(SoundType.STONE) // Звук при разрушении
                    .requiresCorrectToolForDrops() // Требует правильного инструмента для лута (как руды)
                    // .noOcclusion() // <--- ЭТО ОЧЕНЬ ВАЖНО! НЕ ИСПОЛЬЗУЙТЕ ЭТО ДЛЯ БЛОКОВ, КОТОРЫЕ ДОЛЖНЫ РЕАГИРОВАТЬ НА РЕДСТОУН
                    //                    // noOcclusion делает блок "неполным" или "прозрачным" для редстоуна.
                    //                    // Если ваш блок должен быть твердым и проводить редстоун, то эти свойства должны быть по умолчанию.
                    // .isRedstoneConductor((state, level, pos) -> true) // Можно явно указать, что блок проводит редстоун
                    // .isViewBlocking((state, level, pos) -> true) // Блок блокирует обзор
            ));

    public static final RegistryObject<Block> DUD_FUGAS_TONG = registerBlock("dud_fugas_tong",
            () -> new DudFugasBlock(BlockBehaviour.Properties.of()
                    .strength(31F, 6.0F) // Прочность блока (как у камня или земли)
                    .sound(SoundType.STONE) // Звук при разрушении
                    .requiresCorrectToolForDrops() // Требует правильного инструмента для лута (как руды)
                    .noOcclusion()));

    public static final RegistryObject<Block> DUD_NUKE = registerBlock("dud_nuke",
            () -> new DudNukeBlock(BlockBehaviour.Properties.of()
                    .strength(31F, 6.0F) // Прочность блока (как у камня или земли)
                    .sound(SoundType.STONE) // Звук при разрушении
                    .requiresCorrectToolForDrops() // Требует правильного инструмента для лута (как руды)
                    .noOcclusion()));

    public static final RegistryObject<Block> DUD_SALTED = registerBlock("dud_salted",
            () -> new DudNukeBlock(BlockBehaviour.Properties.of()
                    .strength(31F, 6.0F) // Прочность блока (как у камня или земли)
                    .sound(SoundType.STONE) // Звук при разрушении
                    .requiresCorrectToolForDrops() // Требует правильного инструмента для лута (как руды)
                    .noOcclusion()));

    public static final RegistryObject<Block> SMOKE_BOMB = registerBlock("smoke_bomb",
            () -> new SmokeBombBlock(BlockBehaviour.Properties.of()
                    .strength(0.5F, 6.0F) // Прочность блока (как у камня или земли)
                    .sound(SoundType.CHERRY_LEAVES) // Звук при разрушении
                    .requiresCorrectToolForDrops() // Требует правильного инструмента для лута (как руды)
                    // .noOcclusion() // <--- ЭТО ОЧЕНЬ ВАЖНО! НЕ ИСПОЛЬЗУЙТЕ ЭТО ДЛЯ БЛОКОВ, КОТОРЫЕ ДОЛЖНЫ РЕАГИРОВАТЬ НА РЕДСТОУН
                    //                    // noOcclusion делает блок "неполным" или "прозрачным" для редстоуна.
                    //                    // Если ваш блок должен быть твердым и проводить редстоун, то эти свойства должны быть по умолчанию.
                    // .isRedstoneConductor((state, level, pos) -> true) // Можно явно указать, что блок проводит редстоун
                    // .isViewBlocking((state, level, pos) -> true) // Блок блокирует обзор
            ));

    public static final RegistryObject<Block> NUCLEAR_CHARGE = registerBlock("nuclear_charge",
            () -> new NuclearChargeBlock(BlockBehaviour.Properties.of()
                    .strength(0.5F, 6.0F) // Прочность блока (как у камня или земли)
                    .sound(SoundType.STONE) // Звук при разрушении
                    .requiresCorrectToolForDrops() // Требует правильного инструмента для лута (как руды)
                    // .noOcclusion() // <--- ЭТО ОЧЕНЬ ВАЖНО! НЕ ИСПОЛЬЗУЙТЕ ЭТО ДЛЯ БЛОКОВ, КОТОРЫЕ ДОЛЖНЫ РЕАГИРОВАТЬ НА РЕДСТОУН
                    //                    // noOcclusion делает блок "неполным" или "прозрачным" для редстоуна.
                    //                    // Если ваш блок должен быть твердым и проводить редстоун, то эти свойства должны быть по умолчанию.
                    // .isRedstoneConductor((state, level, pos) -> true) // Можно явно указать, что блок проводит редстоун
                    // .isViewBlocking((state, level, pos) -> true) // Блок блокирует обзор
            ));

    public static final RegistryObject<Block> WASTE_CHARGE = registerBlock("waste_charge",
            () -> new WasteChargeBlock(BlockBehaviour.Properties.of()
                    .strength(0.5F, 6.0F) // Прочность блока (как у камня или земли)
                    .sound(SoundType.STONE) // Звук при разрушении
                    .requiresCorrectToolForDrops() // Требует правильного инструмента для лута (как руды)
                    // .noOcclusion() // <--- ЭТО ОЧЕНЬ ВАЖНО! НЕ ИСПОЛЬЗУЙТЕ ЭТО ДЛЯ БЛОКОВ, КОТОРЫЕ ДОЛЖНЫ РЕАГИРОВАТЬ НА РЕДСТОУН
                    //                    // noOcclusion делает блок "неполным" или "прозрачным" для редстоуна.
                    //                    // Если ваш блок должен быть твердым и проводить редстоун, то эти свойства должны быть по умолчанию.
                    // .isRedstoneConductor((state, level, pos) -> true) // Можно явно указать, что блок проводит редстоун
                    // .isViewBlocking((state, level, pos) -> true) // Блок блокирует обзор
            ));

    public static final RegistryObject<Block> CAGE_LAMP = registerBlock("cage_lamp",
            () -> new CageLampBlock(BlockBehaviour.Properties.of()
                    .strength(0.5F, 6.0F) // Прочность блока (как у камня или земли)
                    .sound(SoundType.STONE) // Звук при разрушении
                    .requiresCorrectToolForDrops() // Требует правильного инструмента для лута (как руды)
                    .noOcclusion() // <--- ЭТО ОЧЕНЬ ВАЖНО! НЕ ИСПОЛЬЗУЙТЕ ЭТО ДЛЯ БЛОКОВ, КОТОРЫЕ ДОЛЖНЫ РЕАГИРОВАТЬ НА РЕДСТОУН
                    .lightLevel(state -> 15)  // свет 15 (максимум)
            ));

    public static final RegistryObject<Block> FLOOD_LAMP = registerBlock("flood_lamp",
            () -> new CageLampBlock(BlockBehaviour.Properties.of()
                    .strength(0.5F, 6.0F) // Прочность блока (как у камня или земли)
                    .sound(SoundType.STONE) // Звук при разрушении
                    .requiresCorrectToolForDrops() // Требует правильного инструмента для лута (как руды)
                    .noOcclusion() // <--- ЭТО ОЧЕНЬ ВАЖНО! НЕ ИСПОЛЬЗУЙТЕ ЭТО ДЛЯ БЛОКОВ, КОТОРЫЕ ДОЛЖНЫ РЕАГИРОВАТЬ НА РЕДСТОУН
                    .lightLevel(state -> 15)  // свет 15 (максимум)
            ));

    public static final RegistryObject<Block> FLUORESCENT_LAMP = registerBlock("fluorescent_lamp",
            () -> new CageLampBlock(BlockBehaviour.Properties.of()
                    .strength(0.5F, 6.0F) // Прочность блока (как у камня или земли)
                    .sound(SoundType.STONE) // Звук при разрушении
                    .requiresCorrectToolForDrops() // Требует правильного инструмента для лута (как руды)
                    .noOcclusion() // <--- ЭТО ОЧЕНЬ ВАЖНО! НЕ ИСПОЛЬЗУЙТЕ ЭТО ДЛЯ БЛОКОВ, КОТОРЫЕ ДОЛЖНЫ РЕАГИРОВАТЬ НА РЕДСТОУН
                    .lightLevel(state -> 15)  // свет 15 (максимум)
            ));

    public static final RegistryObject<Block> C4 = registerBlock("c4",
            () -> new C4Block(BlockBehaviour.Properties.of()
                    .strength(0.5F, 6.0F) // Прочность блока (как у камня или земли)
                    .sound(SoundType.STONE) // Звук при разрушении
                    .requiresCorrectToolForDrops() // Требует правильного инструмента для лута (как руды)
                    // .noOcclusion() // <--- ЭТО ОЧЕНЬ ВАЖНО! НЕ ИСПОЛЬЗУЙТЕ ЭТО ДЛЯ БЛОКОВ, КОТОРЫЕ ДОЛЖНЫ РЕАГИРОВАТЬ НА РЕДСТОУН
                    //                    // noOcclusion делает блок "неполным" или "прозрачным" для редстоуна.
                    //                    // Если ваш блок должен быть твердым и проводить редстоун, то эти свойства должны быть по умолчанию.
                    // .isRedstoneConductor((state, level, pos) -> true) // Можно явно указать, что блок проводит редстоун
                    // .isViewBlocking((state, level, pos) -> true) // Блок блокирует обзор
            ));

    public static final RegistryObject<Block> DECO_STEEL = registerBlock("deco_steel",
            () -> new DecoSteelBlock(BlockBehaviour.Properties.of()
                    .strength(0.5F, 6.0F) // Прочность блока (как у камня или земли)
                    .sound(SoundType.STONE) // Звук при разрушении
                    .requiresCorrectToolForDrops() // Требует правильного инструмента для лута (как руды)
                    // .noOcclusion() // <--- ЭТО ОЧЕНЬ ВАЖНО! НЕ ИСПОЛЬЗУЙТЕ ЭТО ДЛЯ БЛОКОВ, КОТОРЫЕ ДОЛЖНЫ РЕАГИРОВАТЬ НА РЕДСТОУН
                    //                    // noOcclusion делает блок "неполным" или "прозрачным" для редстоуна.
                    //                    // Если ваш блок должен быть твердым и проводить редстоун, то эти свойства должны быть по умолчанию.
                    // .isRedstoneConductor((state, level, pos) -> true) // Можно явно указать, что блок проводит редстоун
                    // .isViewBlocking((state, level, pos) -> true) // Блок блокирует обзор
            ));

    //public static final RegistryObject<Block> DEMON_LAMP  = registerBlock("demon_lamp",
           // () -> new Block(BlockBehaviour.Properties.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));
    public static final RegistryObject<Block> DOOR_BUNKER = registerBlock("door_bunker",
            () -> new net.minecraft.world.level.block.DoorBlock(BlockBehaviour.Properties.copy(Blocks.NETHERITE_BLOCK).sound(SoundType.NETHERITE_BLOCK).noOcclusion(), BlockSetType.STONE));

    public static final RegistryObject<Block> DOOR_OFFICE = registerBlock("door_office",
            () -> new net.minecraft.world.level.block.DoorBlock(BlockBehaviour.Properties.copy(Blocks.CHERRY_WOOD).sound(SoundType.CHERRY_WOOD).noOcclusion(), BlockSetType.CHERRY));

    public static final RegistryObject<Block> METAL_DOOR = registerBlock("metal_door",
            () -> new net.minecraft.world.level.block.DoorBlock(BlockBehaviour.Properties.copy(Blocks.CHAIN).sound(SoundType.CHAIN).noOcclusion(), BlockSetType.BIRCH));

    //public static final RegistryObject<Block> EXPLOSIVE_CHARGE = BLOCKS.register("explosive_charge",
     //   () -> new ExplosiveChargeBlock(BlockBehaviour.Properties.of()
        //    .strength(0.5f)
             //    .sound(SoundType.METAL)));


    public static final RegistryObject<Block> DORNIER = registerBlock("dornier",
            () -> new BarrelBlock(Block.Properties.copy(Blocks.STONE).noOcclusion()));


    public static final RegistryObject<Block> ORE_OIL = registerBlock("ore_oil",
            () -> new Block(Block.Properties.copy(Blocks.STONE).noOcclusion()));
    public static final RegistryObject<Block> BEDROCK_OIL = registerBlock("bedrock_oil",
            () -> new Block(Block.Properties.copy(Blocks.STONE).noOcclusion()));

    public static final RegistryObject<Block> DEPTH_STONE = registerBlock("depth_stone",
            () -> new DepthOreBlock(Block.Properties.copy(Blocks.STONE).noOcclusion()));
    public static final RegistryObject<Block> DEPTH_BORAX = registerBlock("depth_borax",
            () -> new DepthOreBlock(Block.Properties.copy(Blocks.STONE).noOcclusion()));
    public static final RegistryObject<Block> DEPTH_CINNABAR = registerBlock("depth_cinnabar",
            () -> new DepthOreBlock(Block.Properties.copy(Blocks.STONE).noOcclusion()));
    public static final RegistryObject<Block> DEPTH_IRON = registerBlock("depth_iron",
            () -> new DepthOreBlock(Block.Properties.copy(Blocks.STONE).noOcclusion()));
    public static final RegistryObject<Block> DEPTH_TUNGSTEN = registerBlock("depth_tungsten",
            () -> new DepthOreBlock(Block.Properties.copy(Blocks.STONE).noOcclusion()));
    public static final RegistryObject<Block> DEPTH_TITANIUM = registerBlock("depth_titanium",
            () -> new DepthOreBlock(Block.Properties.copy(Blocks.STONE).noOcclusion()));
    public static final RegistryObject<Block> DEPTH_ZIRCONIUM = registerBlock("depth_zirconium",
            () -> new DepthOreBlock(Block.Properties.copy(Blocks.STONE).noOcclusion()));




    public static final RegistryObject<Block> FILE_CABINET = registerBlock("file_cabinet",
            () -> new CrtBlock(Block.Properties.copy(Blocks.STONE).noOcclusion()));

    public static final RegistryObject<Block> B29 = registerBlock("b29",
            () -> new BarrelBlock(Block.Properties.copy(Blocks.STONE).noOcclusion()));

    public static final RegistryObject<Block> MINE_FAT = registerBlock("mine_fat",
            () -> new MineNukeBlock(Block.Properties.copy(Blocks.STONE).noOcclusion()));

    public static final RegistryObject<Block> MINE_AP = registerBlock("mine_ap",
            () -> new MineBlock(Block.Properties.copy(Blocks.STONE).noOcclusion()));

    public static final RegistryObject<Block> CRATE_CONSERVE = registerBlock("crate_conserve",
            () -> new CrtBlock(Block.Properties.copy(Blocks.STONE).noOcclusion()));
    public static final RegistryObject<Block> TAPE_RECORDER = registerBlock("tape_recorder",
            () -> new CrtBlock(Block.Properties.copy(Blocks.STONE).noOcclusion()));

    public static final RegistryObject<Block> BARREL_LOX = registerBlock("barrel_lox",
            () -> new CrtBlock(Block.Properties.copy(Blocks.STONE).noOcclusion()));
    public static final RegistryObject<Block> BARREL_CORRODED = registerBlock("barrel_corroded",
            () -> new CrtBlock(Block.Properties.copy(Blocks.STONE).noOcclusion()));

    public static final RegistryObject<Block> BARREL_IRON = registerBlock("barrel_iron",
            () -> new CrtBlock(Block.Properties.copy(Blocks.STONE).noOcclusion()));

    public static final RegistryObject<Block> BARREL_PINK = registerBlock("barrel_pink",
            () -> new CrtBlock(Block.Properties.copy(Blocks.STONE).noOcclusion()));

    public static final RegistryObject<Block> BARREL_PLASTIC = registerBlock("barrel_plastic",
            () -> new CrtBlock(Block.Properties.copy(Blocks.STONE).noOcclusion()));

    public static final RegistryObject<Block> BARREL_RED = registerBlock("barrel_red",
            () -> new CrtBlock(Block.Properties.copy(Blocks.STONE).noOcclusion()));

    public static final RegistryObject<Block> BARREL_STEEL = registerBlock("barrel_steel",
            () -> new CrtBlock(Block.Properties.copy(Blocks.STONE).noOcclusion()));

    public static final RegistryObject<Block> BARREL_TAINT = registerBlock("barrel_taint",
            () -> new CrtBlock(Block.Properties.copy(Blocks.STONE).noOcclusion()));

    public static final RegistryObject<Block> BARREL_TCALLOY = registerBlock("barrel_tcalloy",
            () -> new CrtBlock(Block.Properties.copy(Blocks.STONE).noOcclusion()));

    public static final RegistryObject<Block> BARREL_VITRIFIED = registerBlock("barrel_vitrified",
            () -> new CrtBlock(Block.Properties.copy(Blocks.STONE).noOcclusion()));

    public static final RegistryObject<Block> BARREL_YELLOW = registerBlock("barrel_yellow",
            () -> new CrtBlock(Block.Properties.copy(Blocks.STONE).noOcclusion()));

    public static final RegistryObject<Block> BARBED_WIRE = registerBlock("barbed_wire",
            () -> new BarbedWireBlock(Block.Properties.copy(Blocks.STONE).noOcclusion()));
    public static final RegistryObject<Block> BARBED_WIRE_FIRE = registerBlock("barbed_wire_fire",
            () -> new BarbedWireFireBlock(Block.Properties.copy(Blocks.STONE).noOcclusion()));
    public static final RegistryObject<Block> BARBED_WIRE_WITHER = registerBlock("barbed_wire_wither",
            () -> new BarbedWireWitherBlock(Block.Properties.copy(Blocks.STONE).noOcclusion()));
    public static final RegistryObject<Block> BARBED_WIRE_POISON = registerBlock("barbed_wire_poison",
            () -> new BarbedWirePoisonBlock(Block.Properties.copy(Blocks.STONE).noOcclusion()));
    public static final RegistryObject<Block> BARBED_WIRE_RAD = registerBlock("barbed_wire_rad",
            () -> new BarbedWireRadBlock(Block.Properties.copy(Blocks.STONE).noOcclusion()));
    public static final RegistryObject<Block> TOASTER = registerBlock("toaster",
            () -> new CrtBlock(Block.Properties.copy(Blocks.STONE).noOcclusion()));
    public static final RegistryObject<Block> CRT_BSOD = registerBlock("crt_bsod",
            () -> new CrtBlock(Block.Properties.copy(Blocks.STONE).noOcclusion()));
    public static final RegistryObject<Block> CRT_CLEAN = registerBlock("crt_clean",
            () -> new CrtBlock(Block.Properties.copy(Blocks.STONE).noOcclusion()));
    public static final RegistryObject<Block> CRT_BROKEN = registerBlock("crt_broken",
            () -> new CrtBlock(Block.Properties.copy(Blocks.STONE).noOcclusion()));


    public static final RegistryObject<Block> SELLAFIELD_SLAKED  = registerBlock("sellafield_slaked",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));

    public static final RegistryObject<Block> SELLAFIELD_SLAKED1  = registerBlock("sellafield_slaked1",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));

    public static final RegistryObject<Block> SELLAFIELD_SLAKED2  = registerBlock("sellafield_slaked2",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));

    public static final RegistryObject<Block> SELLAFIELD_SLAKED3  = registerBlock("sellafield_slaked3",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));

    // ГРАВИТИРУЮЩИЕ ВЕРСИИ СЕЛЛАФИТА (NEW!)

    public static final RegistryObject<Block> BURNED_GRASS  = registerBlock("burned_grass",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.GRASS_BLOCK).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));

    public static final RegistryObject<Block> FALLING_SELLAFIT1 = BLOCKS.register("falling_sellafit1",
            () -> new FallingSellafit(SELLAFIELD_SLAKED.get()));

    public static final RegistryObject<Block> FALLING_SELLAFIT2 = BLOCKS.register("falling_sellafit2",
            () -> new FallingSellafit(SELLAFIELD_SLAKED1.get()));

    public static final RegistryObject<Block> FALLING_SELLAFIT3 = BLOCKS.register("falling_sellafit3",
            () -> new FallingSellafit(SELLAFIELD_SLAKED2.get()));

    public static final RegistryObject<Block> FALLING_SELLAFIT4 = BLOCKS.register("falling_sellafit4",
            () -> new FallingSellafit(SELLAFIELD_SLAKED3.get()));

    public static final RegistryObject<Block> CONCRETE_FAN  = registerBlock("concrete_fan",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));

    public static final RegistryObject<Block> CONCRETE_MARKED  = registerBlock("concrete_marked",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));

    public static final RegistryObject<Block> BRICK_CONCRETE_BROKEN = registerBlock("brick_concrete_broken",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));
    public static final RegistryObject<Block> BRICK_CONCRETE_BROKEN_STAIRS = registerBlock("brick_concrete_broken_stairs",
            () -> new StairBlock(() -> ModBlocks.BRICK_CONCRETE_BROKEN.get().defaultBlockState(),
                    BlockBehaviour.Properties.copy(Blocks.STONE).sound(SoundType.STONE)));
    public static final RegistryObject<Block> BRICK_CONCRETE_BROKEN_SLAB = registerBlock("brick_concrete_broken_slab",
            () -> new SlabBlock(BlockBehaviour.Properties.copy(Blocks.STONE).sound(SoundType.STONE)));

    public static final RegistryObject<Block> BRICK_CONCRETE_CRACKED = registerBlock("brick_concrete_cracked",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));
    public static final RegistryObject<Block> BRICK_CONCRETE_CRACKED_STAIRS = registerBlock("brick_concrete_cracked_stairs",
            () -> new StairBlock(() -> ModBlocks.BRICK_CONCRETE_CRACKED.get().defaultBlockState(),
                    BlockBehaviour.Properties.copy(Blocks.STONE).sound(SoundType.STONE)));
    public static final RegistryObject<Block> BRICK_CONCRETE_CRACKED_SLAB = registerBlock("brick_concrete_cracked_slab",
            () -> new SlabBlock(BlockBehaviour.Properties.copy(Blocks.STONE).sound(SoundType.STONE)));

    public static final RegistryObject<Block> BRICK_CONCRETE_MOSSY = registerBlock("brick_concrete_mossy",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));
    public static final RegistryObject<Block> BRICK_CONCRETE_MOSSY_STAIRS = registerBlock("brick_concrete_mossy_stairs",
            () -> new StairBlock(() -> ModBlocks.BRICK_CONCRETE_MOSSY.get().defaultBlockState(),
                    BlockBehaviour.Properties.copy(Blocks.STONE).sound(SoundType.STONE)));
    public static final RegistryObject<Block> BRICK_CONCRETE_MOSSY_SLAB = registerBlock("brick_concrete_mossy_slab",
            () -> new SlabBlock(BlockBehaviour.Properties.copy(Blocks.STONE).sound(SoundType.STONE)));

    public static final RegistryObject<Block> BRICK_CONCRETE_MARKED = registerBlock("brick_concrete_marked",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));

    public static final RegistryObject<Block> REINFORCED_STONE_STAIRS = registerBlock("reinforced_stone_stairs",
            () -> new StairBlock(() -> ModBlocks.REINFORCED_STONE.get().defaultBlockState(),
                    BlockBehaviour.Properties.copy(Blocks.STONE).sound(SoundType.STONE)));
    public static final RegistryObject<Block> REINFORCED_STONE_SLAB = registerBlock("reinforced_stone_slab",
            () -> new SlabBlock(BlockBehaviour.Properties.copy(Blocks.STONE).sound(SoundType.STONE)));
    public static final RegistryObject<Block> CRATE_IRON = registerBlock("crate_iron",
            () -> new IronCrateBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).sound(SoundType.METAL).strength(0.5f, 1f).requiresCorrectToolForDrops()));
    public static final RegistryObject<Block> CRATE_STEEL = registerBlock("crate_steel",
            () -> new SteelCrateBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).sound(SoundType.METAL).strength(1f, 1.5f).requiresCorrectToolForDrops()));
    public static final RegistryObject<Block> CRATE_DESH = registerBlock("crate_desh",
            () -> new DeshCrateBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).sound(SoundType.METAL).strength(1.5f, 2f).requiresCorrectToolForDrops()));

    public static final RegistryObject<Block> WASTE_PLANKS = registerBlock("waste_planks",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.OAK_WOOD).strength(3.0f, 3.0f).requiresCorrectToolForDrops()));

    public static final RegistryObject<Block> WASTE_LOG = registerBlock("waste_log",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.COAL_BLOCK).strength(3.0f, 3.0f).requiresCorrectToolForDrops()));


    // -----------------------<РАСТЕНИЯ>-----------------------------
    public static final RegistryObject<Block> STRAWBERRY_BUSH = registerBlock("strawberry_bush",
            () -> new FlowerBlock(() -> MobEffects.LUCK, 5,
                    BlockBehaviour.Properties.copy(Blocks.ALLIUM).noOcclusion().noCollission()));


    // -----------------------<РУДЫ>-----------------------------


    public static final RegistryObject<Block> RESOURCE_ASBESTOS = registerBlock("resource_asbestos",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.STONE).strength(3.0f, 3.0f).requiresCorrectToolForDrops()));

    public static final RegistryObject<Block> RESOURCE_BAUXITE = registerBlock("resource_bauxite",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.STONE).strength(3.0f, 3.0f).requiresCorrectToolForDrops()));

    public static final RegistryObject<Block> RESOURCE_HEMATITE = registerBlock("resource_hematite",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.STONE).strength(3.0f, 3.0f).requiresCorrectToolForDrops()));

    public static final RegistryObject<Block> RESOURCE_LIMESTONE = registerBlock("resource_limestone",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.STONE).strength(3.0f, 3.0f).requiresCorrectToolForDrops()));

    public static final RegistryObject<Block> RESOURCE_MALACHITE = registerBlock("resource_malachite",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.STONE).strength(3.0f, 3.0f).requiresCorrectToolForDrops()));

    public static final RegistryObject<Block> RESOURCE_SULFUR = registerBlock("resource_sulfur",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.STONE).strength(3.0f, 3.0f).requiresCorrectToolForDrops()));


    public static final RegistryObject<Block> LIGNITE_ORE = registerBlock("lignite_ore",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.STONE).strength(3.0f, 3.0f).requiresCorrectToolForDrops()));

    public static final RegistryObject<Block> ALUMINUM_ORE = registerBlock("aluminum_ore",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.STONE).strength(3.0f, 3.0f).requiresCorrectToolForDrops()));

    public static final RegistryObject<Block> URANIUM_ORE_H = registerBlock("uranium_ore_h",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.STONE).strength(3.0f, 3.0f).requiresCorrectToolForDrops()));

    public static final RegistryObject<Block> LEAD_ORE = registerBlock("lead_ore",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.STONE).strength(3.0f, 3.0f).requiresCorrectToolForDrops()));

    public static final RegistryObject<Block> RAREGROUND_ORE = registerBlock("rareground_ore",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.STONE).strength(3.0f, 3.0f).requiresCorrectToolForDrops()));

    public static final RegistryObject<Block> FLUORITE_ORE = registerBlock("fluorite_ore",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.STONE).strength(3.0f, 3.0f).requiresCorrectToolForDrops()));

    public static final RegistryObject<Block> BERYLLIUM_ORE = registerBlock("beryllium_ore",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.STONE).strength(3.0f, 3.0f).requiresCorrectToolForDrops()));

    public static final RegistryObject<Block> ASBESTOS_ORE = registerBlock("asbestos_ore",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.STONE).strength(3.0f, 3.0f).requiresCorrectToolForDrops()));

    public static final RegistryObject<Block> CINNABAR_ORE = registerBlock("cinnabar_ore",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.STONE).strength(3.0f, 3.0f).requiresCorrectToolForDrops()));

    public static final RegistryObject<Block> COBALT_ORE = registerBlock("cobalt_ore",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.STONE).strength(3.0f, 3.0f).requiresCorrectToolForDrops()));

    public static final RegistryObject<Block> TUNGSTEN_ORE = registerBlock("tungsten_ore",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.STONE).strength(3.0f, 3.0f).requiresCorrectToolForDrops()));

    public static final RegistryObject<Block> THORIUM_ORE = registerBlock("thorium_ore",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.STONE).strength(3.0f, 3.0f).requiresCorrectToolForDrops()));

    public static final RegistryObject<Block> FREAKY_ALIEN_BLOCK = registerBlock("freaky_alien_block",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.STONE).strength(3.0f, 3.0f).requiresCorrectToolForDrops()));

    public static final RegistryObject<Block> TITANIUM_ORE = registerBlock("titanium_ore",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.STONE).strength(3.0f, 3.0f).requiresCorrectToolForDrops()));

    public static final RegistryObject<Block> SULFUR_ORE = registerBlock("sulfur_ore",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.STONE).strength(3.0f, 3.0f).requiresCorrectToolForDrops()));

    // Дипслейт руды
    public static final RegistryObject<Block> URANIUM_ORE_DEEPSLATE = registerBlock("uranium_ore_deepslate",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.DEEPSLATE).strength(5.0f, 5.0f).requiresCorrectToolForDrops()));

    public static final RegistryObject<Block> BERYLLIUM_ORE_DEEPSLATE = registerBlock("beryllium_ore_deepslate",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.DEEPSLATE).strength(5.0f, 5.0f).requiresCorrectToolForDrops()));

    public static final RegistryObject<Block> TITANIUM_ORE_DEEPSLATE = registerBlock("titanium_ore_deepslate",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.DEEPSLATE).strength(5.0f, 5.0f).requiresCorrectToolForDrops()));

    public static final RegistryObject<Block> LEAD_ORE_DEEPSLATE = registerBlock("lead_ore_deepslate",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.DEEPSLATE).strength(5.0f, 5.0f).requiresCorrectToolForDrops()));

    public static final RegistryObject<Block> RAREGROUND_ORE_DEEPSLATE = registerBlock("rareground_ore_deepslate",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.DEEPSLATE).strength(5.0f, 5.0f).requiresCorrectToolForDrops()));

    public static final RegistryObject<Block> THORIUM_ORE_DEEPSLATE = registerBlock("thorium_ore_deepslate",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.DEEPSLATE).strength(5.0f, 5.0f).requiresCorrectToolForDrops()));

    public static final RegistryObject<Block> ALUMINUM_ORE_DEEPSLATE = registerBlock("aluminum_ore_deepslate",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.DEEPSLATE).strength(5.0f, 5.0f).requiresCorrectToolForDrops()));

    public static final RegistryObject<Block> COBALT_ORE_DEEPSLATE = registerBlock("cobalt_ore_deepslate",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.DEEPSLATE).strength(5.0f, 5.0f).requiresCorrectToolForDrops()));

    public static final RegistryObject<Block> CINNABAR_ORE_DEEPSLATE = registerBlock("cinnabar_ore_deepslate",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.DEEPSLATE).strength(5.0f, 5.0f).requiresCorrectToolForDrops()));


    // ДВЕРИ
    
    public static final RegistryObject<DoorBlock> LARGE_VEHICLE_DOOR = registerBlockWithoutItem("large_vehicle_door", 
        () -> new DoorBlock(
            BlockBehaviour.Properties.of()
                .strength(10.0F, 1000.0F)
                .requiresCorrectToolForDrops()
                .sound(SoundType.METAL)
                .noOcclusion(),
            "large_vehicle_door"
        ));

    public static final RegistryObject<DoorBlock> ROUND_AIRLOCK_DOOR = registerBlockWithoutItem("round_airlock_door",
        () -> new DoorBlock(
            BlockBehaviour.Properties.of()
                .strength(10.0F, 1000.0F)
                .requiresCorrectToolForDrops()
                .sound(SoundType.METAL)
                .noOcclusion(),
            "round_airlock_door"
        ));

        public static final RegistryObject<Block> TRANSITION_SEAL = registerBlockWithoutItem("transition_seal",
        () -> new DoorBlock(
            BlockBehaviour.Properties.of()
                .strength(10.0F, 1000.0F)
                .requiresCorrectToolForDrops()
                .sound(SoundType.METAL)
                .noOcclusion(),
            "transition_seal"
        ));
    
    public static final RegistryObject<Block> FIRE_DOOR = registerBlockWithoutItem("fire_door",
        () -> new DoorBlock(
            BlockBehaviour.Properties.of()
                .strength(10.0F, 1000.0F)
                .requiresCorrectToolForDrops()
                .sound(SoundType.METAL)
                .noOcclusion(),
            "fire_door"
        ));
    
    public static final RegistryObject<Block> SLIDE_DOOR = registerBlockWithoutItem("sliding_blast_door",
        () -> new DoorBlock(
            BlockBehaviour.Properties.of()
                .strength(10.0F, 1000.0F)
                .requiresCorrectToolForDrops()
                .sound(SoundType.METAL)
                .noOcclusion(),
            "sliding_blast_door"
        ));
    
    public static final RegistryObject<Block> SLIDING_SEAL_DOOR = registerBlockWithoutItem("sliding_seal_door",
        () -> new DoorBlock(
            BlockBehaviour.Properties.of()
                .strength(10.0F, 1000.0F)
                .requiresCorrectToolForDrops()
                .sound(SoundType.METAL)
                .noOcclusion(),
            "sliding_seal_door"
        ));
    
    public static final RegistryObject<Block> SECURE_ACCESS_DOOR = registerBlockWithoutItem("secure_access_door",
        () -> new DoorBlock(
            BlockBehaviour.Properties.of()
                .strength(10.0F, 1000.0F)
                .requiresCorrectToolForDrops()
                .sound(SoundType.METAL)
                .noOcclusion(),
            "secure_access_door"
        ));
    
    public static final RegistryObject<Block> QE_SLIDING = registerBlockWithoutItem("qe_sliding_door",
        () -> new DoorBlock(
            BlockBehaviour.Properties.of()
                .strength(10.0F, 1000.0F)
                .requiresCorrectToolForDrops()
                .sound(SoundType.METAL)   
                .noOcclusion(),
            "qe_sliding_door"
        ));
    
    public static final RegistryObject<Block> QE_CONTAINMENT = registerBlockWithoutItem("qe_containment_door",
        () -> new DoorBlock(
            BlockBehaviour.Properties.of()
                .strength(10.0F, 1000.0F)
                .requiresCorrectToolForDrops()
                .sound(SoundType.METAL)
                .noOcclusion(),
            "qe_containment_door"
        ));
    
    public static final RegistryObject<Block> WATER_DOOR = registerBlockWithoutItem("water_door",
        () -> new DoorBlock(
            BlockBehaviour.Properties.of()
                .strength(10.0F, 1000.0F)
                .requiresCorrectToolForDrops()
                .sound(SoundType.METAL)
                .noOcclusion(),
            "water_door"
        ));
    
    public static final RegistryObject<Block> SILO_HATCH = registerBlockWithoutItem("silo_hatch",
        () -> new DoorBlock(
            BlockBehaviour.Properties.of()
                .strength(10.0F, 1000.0F)
                .requiresCorrectToolForDrops()
                .sound(SoundType.METAL)
                .noOcclusion(),
            "silo_hatch"
        ));
    
    public static final RegistryObject<Block> SILO_HATCH_LARGE = registerBlockWithoutItem("silo_hatch_large",
        () -> new DoorBlock(
            BlockBehaviour.Properties.of()
                .strength(10.0F, 1000.0F)
                .requiresCorrectToolForDrops()
                .sound(SoundType.METAL)
                .noOcclusion(),
            "silo_hatch_large"
        ));
    
    // ==================== Helper Methods ====================

    private static RegistryObject<Block> registerAnvil(String name, AnvilTier tier) {
        return registerBlock(name, () -> new AnvilBlock(ANVIL_PROPERTIES, tier));
    }

    private static <T extends Block> RegistryObject<T> registerBlock(String name, Supplier<T> block) {
        RegistryObject<T> toReturn = BLOCKS.register(name, block);
        registerBlockItem(name, toReturn);
        return toReturn;
    }
    
    private static <T extends Block> RegistryObject<T> registerBlockWithoutItem(String name, Supplier<T> block) {
        return BLOCKS.register(name, block);
    }
    
    private static <T extends Block> RegistryObject<Item> registerBlockItem(String name, RegistryObject<T> block) {
        return ModItems.ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
    }
}