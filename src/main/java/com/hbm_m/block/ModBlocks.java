package com.hbm_m.block;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import com.hbm_m.api.energy.ConverterBlock;
import com.hbm_m.api.energy.SwitchBlock;
import com.hbm_m.api.energy.WireBlock;
import com.hbm_m.block.bomb.NukeFatManBlock;
import com.hbm_m.block.decorations.CageLampBlock;
import com.hbm_m.block.decorations.CrtBlock;
import com.hbm_m.block.decorations.DoorBlock;
import com.hbm_m.block.explosives.AirBombBlock;
import com.hbm_m.block.explosives.AirNukeBombBlock;
import com.hbm_m.block.explosives.C4Block;
import com.hbm_m.block.explosives.DetMinerBlock;
import com.hbm_m.block.explosives.DudFugasBlock;
import com.hbm_m.block.explosives.DudNukeBlock;
import com.hbm_m.block.explosives.ExplosiveChargeBlock;
import com.hbm_m.block.explosives.GigaDetBlock;
import com.hbm_m.block.explosives.MineBlock;
import com.hbm_m.block.explosives.MineNukeBlock;
import com.hbm_m.block.explosives.NuclearChargeBlock;
import com.hbm_m.block.explosives.SmokeBombBlock;
import com.hbm_m.block.explosives.WasteChargeBlock;
import com.hbm_m.block.machines.ArmorTableBlock;
import com.hbm_m.block.machines.BlastFurnaceBlock;
import com.hbm_m.block.machines.BlastFurnaceExtensionBlock;
import com.hbm_m.block.machines.FluidDuctBlock;
import com.hbm_m.block.machines.GeigerCounterBlock;
import com.hbm_m.block.machines.HeatingOvenBlock;
import com.hbm_m.block.machines.LaunchPadBlock;
import com.hbm_m.block.machines.LaunchPadRustedBlock;
import com.hbm_m.block.machines.MachineAdvancedAssemblerBlock;
import com.hbm_m.block.machines.MachineArcWelderBlock;
import com.hbm_m.block.machines.MachineAssemblerBlock;
import com.hbm_m.block.machines.MachineBatteryBlock;
import com.hbm_m.block.machines.MachineBatterySocketBlock;
import com.hbm_m.block.machines.MachineBreederBlock;
import com.hbm_m.block.machines.MachineCatalyticReformerBlock;
import com.hbm_m.block.machines.MachineCentrifugeBlock;
import com.hbm_m.block.machines.MachineChemicalFactoryBlock;
import com.hbm_m.block.machines.MachineChemicalPlantBlock;
import com.hbm_m.block.machines.MachineCoolingTowerBlock;
import com.hbm_m.block.machines.MachineFoundryChannelBlock;
import com.hbm_m.block.machines.MachineFoundryBasinBlock;
import com.hbm_m.block.machines.MachineCoreEmitterBlock;
import com.hbm_m.block.machines.MachineCoreInjectorBlock;
import com.hbm_m.block.machines.MachineCoreReceiverBlock;
import com.hbm_m.block.machines.MachineCrackingTowerBlock;
import com.hbm_m.block.machines.MachineCrucibleBlock;
import com.hbm_m.block.machines.MachineCrystallizerBlock;
import com.hbm_m.block.machines.MachineCyclotronBlock;
import com.hbm_m.block.machines.MachineDerrickBlock;
import com.hbm_m.block.machines.MachineDeuteriumTowerBlock;
import com.hbm_m.block.machines.MachineFelBlock;
import com.hbm_m.block.machines.MachineFlareStackBlock;
import com.hbm_m.block.machines.MachineFluidTankBlock;
import com.hbm_m.block.machines.MachineFrackingTowerBlock;
import com.hbm_m.block.machines.MachineFractionTowerBlock;
import com.hbm_m.block.machines.MachineGasCentrifugeBlock;
import com.hbm_m.block.machines.MachineHydrotreaterBlock;
import com.hbm_m.block.machines.MachineIndustrialBoilerBlock;
import com.hbm_m.block.machines.MachineIndustrialTurbineBlock;
import com.hbm_m.block.machines.MachineLargePylonBlock;
import com.hbm_m.block.machines.MachineLiquefactorBlock;
import com.hbm_m.block.machines.MachineMiningDrillBlock;
import com.hbm_m.block.machines.MachineMixerBlock;
import com.hbm_m.block.machines.MachinePressBlock;
import com.hbm_m.block.machines.MachinePumpjackBlock;
import com.hbm_m.block.machines.MachineRadarBlock;
import com.hbm_m.block.machines.MachineRbmkConsoleBlock;
import com.hbm_m.block.machines.MachineRefineryBlock;
import com.hbm_m.block.machines.MachineShredderBlock;
import com.hbm_m.block.machines.MachineSilexBlock;
import com.hbm_m.block.machines.MachineSolarBoilerBlock;
import com.hbm_m.block.machines.MachineSolarMirrorsBlock;
import com.hbm_m.block.machines.MachineSolderingStationBlock;
import com.hbm_m.block.machines.MachineSteamTurbineBlock;
import com.hbm_m.block.machines.MachineSubstationBlock;
import com.hbm_m.block.machines.MachineTowerSmallBlock;
import com.hbm_m.block.machines.MachineTurbineBlock;
import com.hbm_m.block.machines.MachineTurbofanBlock;
import com.hbm_m.block.machines.MachineVacuumDistillBlock;
import com.hbm_m.block.machines.MachineWatzPowerplantBlock;
import com.hbm_m.block.machines.MachineWoodBurnerBlock;
import com.hbm_m.block.machines.MachineZirnoxBlock;
import com.hbm_m.block.machines.UniversalMachinePartBlock;
import com.hbm_m.block.machines.anvils.AnvilBlock;
import com.hbm_m.block.machines.anvils.AnvilTier;
import com.hbm_m.block.machines.crates.DeshCrateBlock;
import com.hbm_m.block.machines.crates.IronCrateBlock;
import com.hbm_m.block.machines.crates.SteelCrateBlock;
import com.hbm_m.block.machines.crates.TemplateCrateBlock;
import com.hbm_m.block.machines.crates.TungstenCrateBlock;
import com.hbm_m.block.nature.DepthOreBlock;
import com.hbm_m.block.nature.GeysirBlock;
import com.hbm_m.block.nature.RadioactiveBlock;
import com.hbm_m.block.weapons.BarbedWireBlock;
import com.hbm_m.block.weapons.BarbedWireFireBlock;
import com.hbm_m.block.weapons.BarbedWirePoisonBlock;
import com.hbm_m.block.weapons.BarbedWireRadBlock;
import com.hbm_m.block.weapons.BarbedWireWitherBlock;
import com.hbm_m.block.weapons.FallingSellafit;
import com.hbm_m.item.ModItems;
import com.hbm_m.item.fekal_electric.MachineBatteryBlockItem;
import com.hbm_m.item.tags_and_tiers.ModIngots;
import com.hbm_m.lib.RefStrings;

import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.material.MapColor;
import dev.architectury.registry.registries.DeferredRegister;
import net.minecraft.core.registries.Registries;
import dev.architectury.registry.registries.RegistrySupplier;

public class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(RefStrings.MODID, Registries.BLOCK);

    public static final RegistrySupplier<Block> GEIGER_COUNTER_BLOCK = registerBlock("geiger_counter_block",
            () -> new GeigerCounterBlock(Block.Properties.copy(Blocks.IRON_BLOCK).noOcclusion()));

    private static final BlockBehaviour.Properties TABLE_PROPERTIES =
            BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(5.0F, 6.0F).sound(SoundType.METAL).requiresCorrectToolForDrops();
    private static final BlockBehaviour.Properties ANVIL_PROPERTIES =
            BlockBehaviour.Properties.copy(Blocks.ANVIL).sound(SoundType.ANVIL).noOcclusion();

    // Стандартные свойства для блоков слитков

    public static final List<RegistrySupplier<Block>> BATTERY_BLOCKS = new ArrayList<>();

    // Вспомогательный метод для регистрации батареек
    private static RegistrySupplier<Block> registerBattery(String name, long capacity) {
        // 1. Регистрируем БЛОК
        RegistrySupplier<Block> batteryBlock = BLOCKS.register(name,
                () -> new MachineBatteryBlock(Block.Properties.of().strength(5.0f).requiresCorrectToolForDrops(), capacity));

        // 2. Регистрируем ПРЕДМЕТ (MachineBatteryBlockItem)
        ModItems.ITEMS.register(name,
                () -> new MachineBatteryBlockItem(batteryBlock.get(), new Item.Properties(), capacity));

        // 3. Добавляем в список для TileEntity
        BATTERY_BLOCKS.add(batteryBlock);

        return batteryBlock;
    }

    // Регистрируем батарейки
    public static final RegistrySupplier<Block> MACHINE_BATTERY = registerBattery("machine_battery", 1_000_000L);
    public static final RegistrySupplier<Block> MACHINE_BATTERY_LITHIUM = registerBattery("machine_battery_lithium", 50_000_000L);
    public static final RegistrySupplier<Block> MACHINE_BATTERY_SCHRABIDIUM = registerBattery("machine_battery_schrabidium", 25_000_000_000L);
    public static final RegistrySupplier<Block> MACHINE_BATTERY_DINEUTRONIUM = registerBattery("machine_battery_dineutronium", 1_000_000_000_000L);

    // АВТОМАТИЧЕСКАЯ РЕГИСТРАЦИЯ БЛОКОВ СЛИТКОВ
    private static final BlockBehaviour.Properties INGOT_BLOCK_PROPERTIES =
            BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).strength(3.0F, 6.0F).sound(SoundType.METAL).requiresCorrectToolForDrops();

    // 1. СПИСОК РАЗРЕШЕННЫХ БЛОКОВ (Whitelist)
    // Сюда добавляем только те материалы, которым нужны блоки (9 слитков = 1 блок).
    // Скопировано и адаптировано из ModItems, убраны лишние материалы типа еды или топлива, если им не нужен блок.
    // 1. СПИСОК РАЗРЕШЕННЫХ БЛОКОВ (Whitelist)
    // 1. СПИСОК РАЗРЕШЕННЫХ БЛОКОВ (Whitelist)
    public static final Set<String> ENABLED_INGOT_BLOCKS = Set.of(
            "uranium", "plutonium", "thorium", "titanium", "aluminum", "copper",
            "lead", "tungsten", "steel", "advanced_alloy", "schrabidium", "saturnite",
            "beryllium", "bismuth", "desh", "cobalt", "lanthanium",
            "niobium", "zirconium", "actinium", "ferrouranium",
            "u233", "u235", "u238", "pu238", "pu239", "pu240", "pu241",
            "ra226", "neptunium",
            "australium", "dineutronium", "euphemium",
            "combine_steel", "dura_steel", "starmetal", "red_copper",
            "plutonium_fuel", "uranium_fuel", "thorium_fuel", "mox_fuel", "schrabidium_fuel",
            "boron", "tcalloy", "cdalloy", "cadmium"
    );

    // 2. КАРТА БЛОКОВ
    public static final Map<ModIngots, RegistrySupplier<Block>> INGOT_BLOCKS = new EnumMap<>(ModIngots.class);

    // 3. АВТОМАТИЧЕСКАЯ РЕГИСТРАЦИЯ
    static {
        for (ModIngots ingot : ModIngots.values()) {
            String name = ingot.getName();

            // Проверяем, есть ли этот слиток в "белом списке"
            if (ENABLED_INGOT_BLOCKS.contains(name)) {

                String blockName = "block_" + name;

                RegistrySupplier<Block> registeredBlock;

                // Определяем свойства (радиоактивный или обычный)
                if (isRadioactiveIngot(ingot)) {
                    registeredBlock = registerBlock(blockName,
                            () -> new RadioactiveBlock(INGOT_BLOCK_PROPERTIES));
                } else {
                    registeredBlock = registerBlock(blockName,
                            () -> new Block(INGOT_BLOCK_PROPERTIES));
                }

                // Сохраняем в карту
                INGOT_BLOCKS.put(ingot, registeredBlock);
            }
        }
    }

    // Вспомогательный метод получения блока
    public static RegistrySupplier<Block> getIngotBlock(ModIngots ingot) {
        RegistrySupplier<Block> block = INGOT_BLOCKS.get(ingot);
        if (block == null) {
            // Логируем ошибку или возвращаем заглушку, чтобы игра не крашилась при обращении к несуществующему блоку
            throw new NullPointerException("Block for ingot " + ingot.getName() + " is not registered! Check ENABLED_INGOT_BLOCKS.");
        }
        return block;
    }

    // Оставляем логику определения радиоактивности без изменений
    private static boolean isRadioactiveIngot(ModIngots ingot) {
        String name = ingot.getName().toLowerCase();
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

    public static boolean hasIngotBlock(ModIngots ingot) {
        return INGOT_BLOCKS.containsKey(ingot);
    }

    public static final RegistrySupplier<Block> URANIUM_BLOCK = getIngotBlock(ModIngots.URANIUM);
    public static final RegistrySupplier<Block> PLUTONIUM_BLOCK = getIngotBlock(ModIngots.PLUTONIUM);
    public static final RegistrySupplier<Block> PLUTONIUM_FUEL_BLOCK = getIngotBlock(ModIngots.PLUTONIUM_FUEL);

    public static final RegistrySupplier<Block> POLONIUM210_BLOCK = registerBlock("polonium210_block",
            () -> new RadioactiveBlock(INGOT_BLOCK_PROPERTIES));

    public static final RegistrySupplier<Block> WASTE_GRASS = registerBlock("waste_grass",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.DIRT).sound(SoundType.GRAVEL)));

    public static final RegistrySupplier<Block> WASTE_LEAVES = registerBlock("waste_leaves",
            () -> new LeavesBlock(BlockBehaviour.Properties.copy(Blocks.OAK_LEAVES).noOcclusion()));

    public static final RegistrySupplier<Block> WIRE_COATED = registerBlock("wire_coated",
            () -> new WireBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).noOcclusion()));


    //---------------------------<СТАНКИ>-------------------------------------

    public static final RegistrySupplier<Block> ANVIL_IRON = registerAnvil("anvil_iron", AnvilTier.IRON);
    public static final RegistrySupplier<Block> ANVIL_LEAD = registerAnvil("anvil_lead", AnvilTier.IRON);
    public static final RegistrySupplier<Block> ANVIL_STEEL = registerAnvil("anvil_steel", AnvilTier.STEEL);
    public static final RegistrySupplier<Block> ANVIL_DESH = registerAnvil("anvil_desh", AnvilTier.OIL);
    public static final RegistrySupplier<Block> ANVIL_FERROURANIUM = registerAnvil("anvil_ferrouranium", AnvilTier.NUCLEAR);
    public static final RegistrySupplier<Block> ANVIL_SATURNITE = registerAnvil("anvil_saturnite", AnvilTier.RBMK);
    public static final RegistrySupplier<Block> ANVIL_BISMUTH_BRONZE = registerAnvil("anvil_bismuth_bronze", AnvilTier.RBMK);
    public static final RegistrySupplier<Block> ANVIL_ARSENIC_BRONZE = registerAnvil("anvil_arsenic_bronze", AnvilTier.RBMK);
    public static final RegistrySupplier<Block> ANVIL_SCHRABIDATE = registerAnvil("anvil_schrabidate", AnvilTier.FUSION);
    public static final RegistrySupplier<Block> ANVIL_DNT = registerAnvil("anvil_dnt", AnvilTier.PARTICLE);
    public static final RegistrySupplier<Block> ANVIL_OSMIRIDIUM = registerAnvil("anvil_osmiridium", AnvilTier.GERALD);
    public static final RegistrySupplier<Block> ANVIL_MURKY = registerAnvil("anvil_murky", AnvilTier.MURKY);

    public static List<RegistrySupplier<Block>> getAnvilBlocks() {
        return List.of(ANVIL_IRON, ANVIL_LEAD, ANVIL_STEEL, ANVIL_DESH, ANVIL_FERROURANIUM, ANVIL_SATURNITE, ANVIL_BISMUTH_BRONZE, ANVIL_ARSENIC_BRONZE, ANVIL_SCHRABIDATE, ANVIL_DNT, ANVIL_OSMIRIDIUM, ANVIL_MURKY);
    }

    public static final RegistrySupplier<Block> CONVERTER_BLOCK = registerBlock("converter_block",
            () -> new ConverterBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).noOcclusion()));

    public static final RegistrySupplier<Block> BLAST_FURNACE = registerBlock("blast_furnace",
            () -> new BlastFurnaceBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK)
                    .strength(4.0f, 4.0f)
                    .sound(SoundType.STONE)
                    .lightLevel(state -> state.getValue(BlastFurnaceBlock.LIT) ? 15 : 0)));

    public static final RegistrySupplier<Block> BLAST_FURNACE_EXTENSION = registerBlock("blast_furnace_extension",
            () -> new BlastFurnaceExtensionBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK)
                    .strength(3.0f, 4.0f)
                    .sound(SoundType.METAL)
                    .noOcclusion()));
					
	// МУЛЬТИБЛОКИ ----------------------------------------------------------------------------------------------------
	
    public static final RegistrySupplier<Block> PRESS = registerBlockWithoutItem("press",
            () -> new MachinePressBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).strength(4.0f, 4.0f).sound(SoundType.METAL).noOcclusion()));

    public static final RegistrySupplier<Block> WOOD_BURNER = registerBlockWithoutItem("wood_burner",
            () -> new MachineWoodBurnerBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).strength(4.0f, 4.0f).sound(SoundType.METAL).noOcclusion()));

    public static final RegistrySupplier<Block> ARMOR_TABLE = registerBlock("armor_table",
            () -> new ArmorTableBlock(TABLE_PROPERTIES));

    public static final RegistrySupplier<Block> SHREDDER = registerBlock("shredder",
            () -> new MachineShredderBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK)));

    public static final RegistrySupplier<Block> SWITCH = registerBlock("switch",
            () -> new SwitchBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK)));

    public static final RegistrySupplier<Block> MACHINE_ASSEMBLER = registerBlockWithoutItem("machine_assembler",
            () -> new MachineAssemblerBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).strength(2.0f).noOcclusion()));

    public static final RegistrySupplier<Block> ADVANCED_ASSEMBLY_MACHINE = registerBlockWithoutItem("advanced_assembly_machine",
            () -> new MachineAdvancedAssemblerBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).strength(2.0f).noOcclusion()));

    public static final RegistrySupplier<Block> HYDRAULIC_FRACKINING_TOWER = registerBlockWithoutItem("hydraulic_frackining_tower",
            () -> new MachineFrackingTowerBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).strength(4.0f).noOcclusion().isSuffocating((state, world, pos) -> false)));

    public static final RegistrySupplier<Block> COOLING_TOWER = registerBlockWithoutItem("cooling_tower",
            () -> new MachineCoolingTowerBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).strength(4.0f).noOcclusion().isSuffocating((state, world, pos) -> false)));

    public static final RegistrySupplier<Block> TOWER_SMALL = registerBlockWithoutItem("tower_small",
            () -> new MachineTowerSmallBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).strength(4.0f).noOcclusion().isSuffocating((state, world, pos) -> false)));

    public static final RegistrySupplier<Block> CYCLOTRON = registerBlockWithoutItem("cyclotron",
            () -> new MachineCyclotronBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).strength(4.0f).noOcclusion().isSuffocating((state, world, pos) -> false)));

    public static final RegistrySupplier<Block> ZIRNOX = registerBlockWithoutItem("zirnox",
            () -> new MachineZirnoxBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).strength(4.0f).noOcclusion().isSuffocating((state, world, pos) -> false)));

    public static final RegistrySupplier<Block> ARC_WELDER = registerBlockWithoutItem("arc_welder",
            () -> new MachineArcWelderBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).strength(4.0f).noOcclusion().isSuffocating((state, world, pos) -> false)));

    public static final RegistrySupplier<Block> SOLDERING_STATION = registerBlockWithoutItem("soldering_station",
            () -> new MachineSolderingStationBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).strength(4.0f).noOcclusion().isSuffocating((state, world, pos) -> false)));

    public static final RegistrySupplier<Block> MIXER = registerBlockWithoutItem("mixer",
            () -> new MachineMixerBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).strength(4.0f).noOcclusion().isSuffocating((state, world, pos) -> false)));

    public static final RegistrySupplier<Block> DERRICK = registerBlockWithoutItem("derrick",
            () -> new MachineDerrickBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).strength(4.0f).noOcclusion().isSuffocating((state, world, pos) -> false)));

    public static final RegistrySupplier<Block> RBMK_CONSOLE = registerBlockWithoutItem("rbmk_console",
            () -> new MachineRbmkConsoleBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).strength(4.0f).noOcclusion().isSuffocating((state, world, pos) -> false)));

    public static final RegistrySupplier<Block> FLARE_STACK = registerBlockWithoutItem("flare_stack",
            () -> new MachineFlareStackBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).strength(4.0f).noOcclusion().isSuffocating((state, world, pos) -> false)));

    public static final RegistrySupplier<Block> PUMPJACK = registerBlockWithoutItem("pumpjack",
            () -> new MachinePumpjackBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).strength(4.0f).noOcclusion().isSuffocating((state, world, pos) -> false)));

    public static final RegistrySupplier<Block> RADAR = registerBlockWithoutItem("radar",
            () -> new MachineRadarBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).strength(4.0f).noOcclusion().isSuffocating((state, world, pos) -> false)));

    public static final RegistrySupplier<Block> LARGE_RADAR = registerBlockWithoutItem("large_radar",
            () -> new MachineRadarBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).strength(4.0f).noOcclusion().isSuffocating((state, world, pos) -> false)));

    public static final RegistrySupplier<Block> CRACKING_TOWER = registerBlockWithoutItem("cracking_tower",
            () -> new MachineCrackingTowerBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).strength(4.0f).noOcclusion().isSuffocating((state, world, pos) -> false)));

    public static final RegistrySupplier<Block> FRACTION_TOWER = registerBlockWithoutItem("fraction_tower",
            () -> new MachineFractionTowerBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).strength(4.0f).noOcclusion().isSuffocating((state, world, pos) -> false)));

    public static final RegistrySupplier<Block> MINING_DRILL = registerBlockWithoutItem("mining_drill",
            () -> new MachineMiningDrillBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).strength(4.0f).noOcclusion().isSuffocating((state, world, pos) -> false)));

    public static final RegistrySupplier<Block> FEL = registerBlockWithoutItem("fel",
            () -> new MachineFelBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).strength(4.0f).noOcclusion().isSuffocating((state, world, pos) -> false)));

    public static final RegistrySupplier<Block> SILEX = registerBlockWithoutItem("silex",
            () -> new MachineSilexBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).strength(4.0f).noOcclusion().isSuffocating((state, world, pos) -> false)));

    public static final RegistrySupplier<Block> CRYSTALLIZER = registerBlockWithoutItem("crystallizer",
            () -> new MachineCrystallizerBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).noOcclusion().isSuffocating((state, world, pos) -> false)));

    public static final RegistrySupplier<Block> BREEDER = registerBlockWithoutItem("breeder",
            () -> new MachineBreederBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).noOcclusion().isSuffocating((state, world, pos) -> false)));

    public static final RegistrySupplier<Block> LARGE_PYLON = registerBlockWithoutItem("large_pylon",
            () -> new MachineLargePylonBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).noOcclusion().isSuffocating((state, world, pos) -> false)));

    public static final RegistrySupplier<Block> CHEMICAL_PLANT = registerBlockWithoutItem("chemical_plant",
            () -> new MachineChemicalPlantBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).strength(4.0f, 4.0f).sound(SoundType.METAL).noOcclusion().isSuffocating((state, world, pos) -> false)));

    public static final RegistrySupplier<Block> CRUCIBLE = registerBlock("crucible",
            () -> new MachineCrucibleBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).strength(4.0f, 4.0f).sound(SoundType.METAL).noOcclusion().isSuffocating((state, world, pos) -> false)));

    public static final RegistrySupplier<Block> FOUNDRY_BASIN = registerBlock("foundry_basin",
            () -> new MachineFoundryBasinBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).strength(4.0f, 4.0f).sound(SoundType.METAL).noOcclusion().isSuffocating((state, world, pos) -> false)));

    public static final RegistrySupplier<Block> FOUNDRY_CHANNEL = registerBlock("foundry_channel",
            () -> new MachineFoundryChannelBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).strength(2.0f, 2.0f).sound(SoundType.METAL).noOcclusion().isSuffocating((state, world, pos) -> false)));

    public static final RegistrySupplier<Block> GAS_CENTRIFUGE = registerBlockWithoutItem("gas_centrifuge",
            () -> new MachineGasCentrifugeBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).strength(4.0f, 4.0f).sound(SoundType.METAL).noOcclusion().isSuffocating((state, world, pos) -> false)));

    public static final RegistrySupplier<Block> CENTRIFUGE = registerBlockWithoutItem("centrifuge",
            () -> new MachineCentrifugeBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).strength(4.0f, 4.0f).sound(SoundType.METAL).isSuffocating((state, world, pos) -> false)));

    public static final RegistrySupplier<Block> UNIVERSAL_MACHINE_PART = registerBlockWithoutItem("universal_machine_part",
            () -> new UniversalMachinePartBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).strength(5.0f).noOcclusion().isSuffocating((state, world, pos) -> false).noParticlesOnBreak()));

	public static final RegistrySupplier<Block> FLUID_TANK = registerBlockWithoutItem("fluid_tank",
            () -> new MachineFluidTankBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).strength(4.0f).requiresCorrectToolForDrops().noOcclusion().isSuffocating((state, world, pos) -> false)));

	public static final RegistrySupplier<Block> LAUNCH_PAD = registerBlockWithoutItem("launch_pad",
            () -> new LaunchPadBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).noOcclusion()));

    public static final RegistrySupplier<Block> LAUNCH_PAD_RUSTED = registerBlockWithoutItem("launch_pad_rusted",
            () -> new LaunchPadRustedBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).noOcclusion()));

    public static final RegistrySupplier<Block> MACHINE_BATTERY_SOCKET = registerBlockWithoutItem("machine_battery_socket",
            () -> new MachineBatterySocketBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).strength(5.0f).requiresCorrectToolForDrops().noOcclusion().isSuffocating((state, world, pos) -> false)));

    public static final RegistrySupplier<Block> INDUSTRIAL_BOILER = registerBlockWithoutItem("industrial_boiler",
            () -> new MachineIndustrialBoilerBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).strength(4.0f, 4.0f).sound(SoundType.METAL).noOcclusion().isSuffocating((state, world, pos) -> false)));

    public static final RegistrySupplier<Block> SOLAR_BOILER = registerBlockWithoutItem("solar_boiler",
            () -> new MachineSolarBoilerBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).strength(3.0f, 3.0f).sound(SoundType.METAL).noOcclusion().isSuffocating((state, world, pos) -> false)));

    public static final RegistrySupplier<Block> SOLAR_MIRRORS = registerBlockWithoutItem("solar_mirrors",
            () -> new MachineSolarMirrorsBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).strength(2.0f, 2.0f).sound(SoundType.METAL).noOcclusion().isSuffocating((state, world, pos) -> false)));

    public static final RegistrySupplier<Block> WATZ_POWERPLANT = registerBlockWithoutItem("watz_powerplant",
            () -> new MachineWatzPowerplantBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).strength(5.0f, 5.0f).sound(SoundType.METAL).noOcclusion().isSuffocating((state, world, pos) -> false)));

    public static final RegistrySupplier<Block> HYDROTREATER = registerBlockWithoutItem("hydrotreater",
            () -> new MachineHydrotreaterBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).strength(4.0f, 4.0f).sound(SoundType.METAL).noOcclusion().isSuffocating((state, world, pos) -> false)));

    public static final RegistrySupplier<Block> CATALYTIC_REFORMER = registerBlockWithoutItem("catalytic_reformer",
            () -> new MachineCatalyticReformerBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).strength(4.0f, 4.0f).sound(SoundType.METAL).noOcclusion().isSuffocating((state, world, pos) -> false)));

    public static final RegistrySupplier<Block> DEUTERIUM_TOWER = registerBlockWithoutItem("deuterium_tower",
            () -> new MachineDeuteriumTowerBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).strength(4.0f, 4.0f).sound(SoundType.METAL).noOcclusion().isSuffocating((state, world, pos) -> false)));

    public static final RegistrySupplier<Block> CHEMICAL_FACTORY = registerBlockWithoutItem("chemical_factory",
            () -> new MachineChemicalFactoryBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).strength(5.0f, 5.0f).sound(SoundType.METAL).noOcclusion().isSuffocating((state, world, pos) -> false)));

    public static final RegistrySupplier<Block> STEAM_TURBINE = registerBlockWithoutItem("steam_turbine",
            () -> new MachineSteamTurbineBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).strength(4.0f, 4.0f).sound(SoundType.METAL)));

    public static final RegistrySupplier<Block> LIQUEFACTOR = registerBlockWithoutItem("liquefactor",
            () -> new MachineLiquefactorBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).strength(4.0f, 4.0f).sound(SoundType.METAL).noOcclusion().isSuffocating((state, world, pos) -> false)));

    public static final RegistrySupplier<Block> CORE_EMITTER = registerBlockWithoutItem("core_emitter",
            () -> new MachineCoreEmitterBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).strength(4.0f, 4.0f).sound(SoundType.METAL).noOcclusion().isSuffocating((state, world, pos) -> false)));

    public static final RegistrySupplier<Block> CORE_INJECTOR = registerBlockWithoutItem("core_injector",
            () -> new MachineCoreInjectorBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).strength(4.0f, 4.0f).sound(SoundType.METAL).noOcclusion().isSuffocating((state, world, pos) -> false)));

    public static final RegistrySupplier<Block> CORE_RECEIVER = registerBlockWithoutItem("core_receiver",
            () -> new MachineCoreReceiverBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).strength(4.0f, 4.0f).sound(SoundType.METAL).noOcclusion().isSuffocating((state, world, pos) -> false)));

    public static final RegistrySupplier<Block> VACUUM_DISTILL = registerBlockWithoutItem("vacuum_distill",
            () -> new MachineVacuumDistillBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).strength(4.0f, 4.0f).sound(SoundType.METAL).noOcclusion().isSuffocating((state, world, pos) -> false)));

    public static final RegistrySupplier<Block> TURBOFAN = registerBlockWithoutItem("turbofan",
            () -> new MachineTurbofanBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).strength(4.0f, 4.0f).sound(SoundType.METAL).noOcclusion().isSuffocating((state, world, pos) -> false)));

    public static final RegistrySupplier<Block> REFINERY = registerBlockWithoutItem("refinery",
            () -> new MachineRefineryBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).strength(4.0f, 4.0f).sound(SoundType.METAL).noOcclusion().isSuffocating((state, world, pos) -> false)));

    public static final RegistrySupplier<Block> INDUSTRIAL_TURBINE = registerBlockWithoutItem("industrial_turbine",
            () -> new MachineIndustrialTurbineBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).strength(4.0f, 4.0f).sound(SoundType.METAL).noOcclusion().isSuffocating((state, world, pos) -> false)));

    public static final RegistrySupplier<Block> TURBINE = registerBlockWithoutItem("turbine",
            () -> new MachineTurbineBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).strength(4.0f, 4.0f).sound(SoundType.METAL).noOcclusion().isSuffocating((state, world, pos) -> false)));

    public static final RegistrySupplier<Block> SUBSTATION = registerBlockWithoutItem("substation",
            () -> new MachineSubstationBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).strength(4.0f, 4.0f).sound(SoundType.METAL).noOcclusion().isSuffocating((state, world, pos) -> false)));

    public static final RegistrySupplier<Block> FLUID_DUCT = registerBlockWithoutItem("fluid_duct",
            () -> new FluidDuctBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).strength(2.0f).sound(SoundType.METAL).noOcclusion(),
                    com.hbm_m.block.machines.PipeStyle.NEO));
    public static final RegistrySupplier<Block> FLUID_DUCT_COLORED = registerBlockWithoutItem("fluid_duct_colored",
            () -> new FluidDuctBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).strength(2.0f).sound(SoundType.METAL).noOcclusion(),
                    com.hbm_m.block.machines.PipeStyle.COLORED));
    public static final RegistrySupplier<Block> FLUID_DUCT_SILVER = registerBlockWithoutItem("fluid_duct_silver",
            () -> new FluidDuctBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).strength(2.0f).sound(SoundType.METAL).noOcclusion(),
                    com.hbm_m.block.machines.PipeStyle.SILVER));

    public static final RegistrySupplier<Block> FLUID_VALVE = registerBlockWithoutItem("fluid_valve",
            () -> new com.hbm_m.block.machines.FluidValveBlock(
                    BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).strength(2.0f).sound(SoundType.METAL).noOcclusion()));

    public static final RegistrySupplier<Block> FLUID_PUMP = registerBlockWithoutItem("fluid_pump",
            () -> new com.hbm_m.block.machines.FluidPumpBlock(
                    BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).strength(3.0f).sound(SoundType.METAL).noOcclusion()));

    public static final RegistrySupplier<Block> FLUID_EXHAUST = registerBlockWithoutItem("fluid_exhaust",
            () -> new com.hbm_m.block.machines.FluidExhaustBlock(
                    BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).strength(2.0f).sound(SoundType.METAL).noOcclusion()));

    public static final RegistrySupplier<Block> HEATING_OVEN = registerBlock("heating_oven",
            () -> new HeatingOvenBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).strength(4.0f, 4.0f).sound(SoundType.METAL).noOcclusion()));

    //---------------------------<ДВЕРИ>-------------------------------------

    public static final RegistrySupplier<DoorBlock> LARGE_VEHICLE_DOOR = registerBlockWithoutItem("large_vehicle_door",
            () -> new DoorBlock(
                    BlockBehaviour.Properties.of()
                            .strength(10.0F, 1000.0F)
                            .requiresCorrectToolForDrops()
                            .sound(SoundType.METAL)
                            .noOcclusion()
                            .isViewBlocking((state, level, pos) -> false),
                    "large_vehicle_door"
            ));

    public static final RegistrySupplier<DoorBlock> ROUND_AIRLOCK_DOOR = registerBlockWithoutItem("round_airlock_door",
            () -> new DoorBlock(
                    BlockBehaviour.Properties.of()
                            .strength(10.0F, 1000.0F)
                            .requiresCorrectToolForDrops()
                            .sound(SoundType.METAL)
                            .noOcclusion()
                            .isViewBlocking((state, level, pos) -> false),
                    "round_airlock_door"
            ));

    public static final RegistrySupplier<Block> TRANSITION_SEAL = registerBlockWithoutItem("transition_seal",
            () -> new DoorBlock(
                    BlockBehaviour.Properties.of()
                            .strength(10.0F, 1000.0F)
                            .requiresCorrectToolForDrops()
                            .sound(SoundType.METAL)
                            .noOcclusion()
                            .isViewBlocking((state, level, pos) -> false),
                    "transition_seal"
            ));

    public static final RegistrySupplier<Block> FIRE_DOOR = registerBlockWithoutItem("fire_door",
            () -> new DoorBlock(
                    BlockBehaviour.Properties.of()
                            .strength(10.0F, 1000.0F)
                            .requiresCorrectToolForDrops()
                            .sound(SoundType.METAL)
                            .noOcclusion()
                            .isViewBlocking((state, level, pos) -> false),
                    "fire_door"
            ));

    public static final RegistrySupplier<Block> SLIDE_DOOR = registerBlockWithoutItem("sliding_blast_door",
            () -> new DoorBlock(
                    BlockBehaviour.Properties.of()
                            .strength(10.0F, 1000.0F)
                            .requiresCorrectToolForDrops()
                            .sound(SoundType.METAL)
                            .noOcclusion()
                            .isViewBlocking((state, level, pos) -> false),
                    "sliding_blast_door"
            ));

    public static final RegistrySupplier<Block> SLIDING_SEAL_DOOR = registerBlockWithoutItem("sliding_seal_door",
            () -> new DoorBlock(
                    BlockBehaviour.Properties.of()
                            .strength(10.0F, 1000.0F)
                            .requiresCorrectToolForDrops()
                            .sound(SoundType.METAL)
                            .noOcclusion()
                            .isViewBlocking((state, level, pos) -> false),
                    "sliding_seal_door"
            ));

    public static final RegistrySupplier<Block> SECURE_ACCESS_DOOR = registerBlockWithoutItem("secure_access_door",
            () -> new DoorBlock(
                    BlockBehaviour.Properties.of()
                            .strength(10.0F, 1000.0F)
                            .requiresCorrectToolForDrops()
                            .sound(SoundType.METAL)
                            .noOcclusion()
                            .isViewBlocking((state, level, pos) -> false),
                    "secure_access_door"
            ));

    public static final RegistrySupplier<Block> QE_SLIDING = registerBlockWithoutItem("qe_sliding_door",
            () -> new DoorBlock(
                    BlockBehaviour.Properties.of()
                            .strength(10.0F, 1000.0F)
                            .requiresCorrectToolForDrops()
                            .sound(SoundType.METAL)
                            .noOcclusion()
                            .isViewBlocking((state, level, pos) -> false),
                    "qe_sliding_door"
            ));

    public static final RegistrySupplier<Block> QE_CONTAINMENT = registerBlockWithoutItem("qe_containment_door",
            () -> new DoorBlock(
                    BlockBehaviour.Properties.of()
                            .strength(10.0F, 1000.0F)
                            .requiresCorrectToolForDrops()
                            .sound(SoundType.METAL)
                            .noOcclusion()
                            .isViewBlocking((state, level, pos) -> false),
                    "qe_containment_door"
            ));

    public static final RegistrySupplier<Block> WATER_DOOR = registerBlockWithoutItem("water_door",
            () -> new DoorBlock(
                    BlockBehaviour.Properties.of()
                            .strength(10.0F, 1000.0F)
                            .requiresCorrectToolForDrops()
                            .sound(SoundType.METAL)
                            .noOcclusion()
                            .isViewBlocking((state, level, pos) -> false),
                    "water_door"
            ));

    public static final RegistrySupplier<Block> SILO_HATCH = registerBlockWithoutItem("silo_hatch",
            () -> new DoorBlock(
                    BlockBehaviour.Properties.of()
                            .strength(10.0F, 1000.0F)
                            .requiresCorrectToolForDrops()
                            .sound(SoundType.METAL)
                            .noOcclusion()
                            .isViewBlocking((state, level, pos) -> false),
                    "silo_hatch"
            ));

    public static final RegistrySupplier<Block> SILO_HATCH_LARGE = registerBlockWithoutItem("silo_hatch_large",
            () -> new DoorBlock(
                    BlockBehaviour.Properties.of()
                            .strength(10.0F, 1000.0F)
                            .requiresCorrectToolForDrops()
                            .sound(SoundType.METAL)
                            .noOcclusion()
                            .isViewBlocking((state, level, pos) -> false),
                    "silo_hatch_large"
            ));

    public static final RegistrySupplier<DoorBlock> VAULT_DOOR = registerBlockWithoutItem("vault_door",
            () -> new DoorBlock(
                    BlockBehaviour.Properties.of()
                            .strength(10.0F, 1000.0F)
                            .requiresCorrectToolForDrops()
                            .sound(SoundType.METAL)
                            .noOcclusion()
                            .isViewBlocking((state, level, pos) -> false),
                    "vault_door"
            ));


    //---------------------------<БЛОКИ>-------------------------------------
    public static final RegistrySupplier<Block> REINFORCED_STONE = registerBlock("reinforced_stone",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> REINFORCED_GLASS = registerBlock("reinforced_glass",
            () -> new GlassBlock(BlockBehaviour.Properties.copy(Blocks.GLASS).strength(4.0F, 12.0F)));

    public static final RegistrySupplier<Block> CRATE = registerBlock("crate",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.OAK_WOOD).strength(1.0f, 1.0f).requiresCorrectToolForDrops()));
    public static final RegistrySupplier<Block> CRATE_LEAD = registerBlock("crate_lead",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.OAK_WOOD).strength(1.0f, 1.0f).requiresCorrectToolForDrops()));
    public static final RegistrySupplier<Block> CRATE_METAL = registerBlock("crate_metal",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.OAK_WOOD).strength(1.0f, 1.0f).requiresCorrectToolForDrops()));
    public static final RegistrySupplier<Block> CRATE_WEAPON = registerBlock("crate_weapon",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.OAK_WOOD).strength(1.0f, 1.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> CONCRETE_HAZARD = registerBlock("concrete_hazard",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));
    public static final RegistrySupplier<Block> CONCRETE_HAZARD_STAIRS = registerBlock("concrete_hazard_stairs",
            () -> new StairBlock(ModBlocks.CONCRETE_HAZARD.get().defaultBlockState(),
                    BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).sound(SoundType.STONE)));
    public static final RegistrySupplier<Block> CONCRETE_HAZARD_SLAB = registerBlock("concrete_hazard_slab",
            () -> new SlabBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).sound(SoundType.STONE)));

    public static final RegistrySupplier<Block> BRICK_CONCRETE = registerBlock("brick_concrete",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));
    public static final RegistrySupplier<Block> BRICK_CONCRETE_STAIRS = registerBlock("brick_concrete_stairs",
            () -> new StairBlock(ModBlocks.BRICK_CONCRETE.get().defaultBlockState(),
                    BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).sound(SoundType.STONE)));
    public static final RegistrySupplier<Block> BRICK_CONCRETE_SLAB = registerBlock("brick_concrete_slab",
            () -> new SlabBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).sound(SoundType.STONE)));

    public static final RegistrySupplier<Block> CONCRETE_MOSSY = registerBlock("concrete_mossy",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));
    public static final RegistrySupplier<Block> CONCRETE_MOSSY_STAIRS = registerBlock("concrete_mossy_stairs",
            () -> new StairBlock(ModBlocks.CONCRETE_MOSSY.get().defaultBlockState(),
                    BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).sound(SoundType.STONE)));
    public static final RegistrySupplier<Block> CONCRETE_MOSSY_SLAB = registerBlock("concrete_mossy_slab",
            () -> new SlabBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).sound(SoundType.STONE)));

    public static final RegistrySupplier<Block> CONCRETE  = registerBlock("concrete",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));
    public static final RegistrySupplier<Block> CONCRETE_STAIRS = registerBlock("concrete_stairs",
            () -> new StairBlock(ModBlocks.CONCRETE.get().defaultBlockState(),
                    BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).sound(SoundType.STONE)));
    public static final RegistrySupplier<Block> CONCRETE_SLAB = registerBlock("concrete_slab",
            () -> new SlabBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).sound(SoundType.STONE)));

    public static final RegistrySupplier<Block> CONCRETE_CRACKED  = registerBlock("concrete_cracked",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));
    public static final RegistrySupplier<Block> CONCRETE_CRACKED_STAIRS = registerBlock("concrete_cracked_stairs",
            () -> new StairBlock(ModBlocks.CONCRETE_CRACKED.get().defaultBlockState(),
                    BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).sound(SoundType.STONE)));
    public static final RegistrySupplier<Block> CONCRETE_CRACKED_SLAB = registerBlock("concrete_cracked_slab",
            () -> new SlabBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).sound(SoundType.STONE)));

    public static final RegistrySupplier<Block> CONCRETE_VENT  = registerBlock("concrete_vent",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));


    public static final RegistrySupplier<Block> DET_MINER = registerBlock("det_miner",
            () -> new DetMinerBlock(BlockBehaviour.Properties.of()
                    .strength(0.5F, 6.0F)
                    .sound(SoundType.STONE)
                    .requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> GIGA_DET = registerBlock("giga_det",
            () -> new GigaDetBlock(BlockBehaviour.Properties.of()
                    .strength(0.5F, 6.0F)
                    .sound(SoundType.WOOD)
                    .requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> AIRBOMB = registerBlock("airbomb",
            () -> new AirBombBlock(BlockBehaviour.Properties.of()
                    .strength(0.5F, 6.0F)
                    .sound(SoundType.STONE)
                    .requiresCorrectToolForDrops().noOcclusion()));

    public static final RegistrySupplier<Block> BALEBOMB_TEST = registerBlock("balebomb_test",
            () -> new AirNukeBombBlock(BlockBehaviour.Properties.of()
                    .strength(0.5F, 6.0F)
                    .sound(SoundType.STONE)
                    .requiresCorrectToolForDrops().noOcclusion()));

    public static final RegistrySupplier<Block> EXPLOSIVE_CHARGE = registerBlock("explosive_charge",
            () -> new ExplosiveChargeBlock(BlockBehaviour.Properties.of()
                    .strength(0.5F, 6.0F)
                    .sound(SoundType.STONE)
                    .requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> DUD_CONVENTIONAL = registerBlock("dud_conventional",
            () -> new DudFugasBlock(BlockBehaviour.Properties.of()
                    .strength(31F, 6.0F)
                    .sound(SoundType.STONE)
                    .requiresCorrectToolForDrops()
                    .noOcclusion()));

    public static final RegistrySupplier<Block> DUD_NUKE = registerBlock("dud_nuke",
            () -> new DudNukeBlock(BlockBehaviour.Properties.of()
                    .strength(31F, 6.0F)
                    .sound(SoundType.STONE)
                    .requiresCorrectToolForDrops()
                    .noOcclusion()));

    public static final RegistrySupplier<Block> DUD_SALTED = registerBlock("dud_salted",
            () -> new DudNukeBlock(BlockBehaviour.Properties.of()
                    .strength(31F, 6.0F)
                    .sound(SoundType.STONE)
                    .requiresCorrectToolForDrops()
                    .noOcclusion()));

    public static final RegistrySupplier<Block> SMOKE_BOMB = registerBlock("smoke_bomb",
            () -> new SmokeBombBlock(BlockBehaviour.Properties.of()
                    .strength(0.5F, 6.0F)
                    .sound(SoundType.CHERRY_LEAVES)
                    .requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> NUCLEAR_CHARGE = registerBlock("nuclear_charge",
            () -> new NuclearChargeBlock(BlockBehaviour.Properties.of()
                    .strength(0.5F, 6.0F)
                    .sound(SoundType.STONE)
                    .requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> WASTE_CHARGE = registerBlock("waste_charge",
            () -> new WasteChargeBlock(BlockBehaviour.Properties.of()
                    .strength(0.5F, 6.0F)
                    .sound(SoundType.STONE)
                    .requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> CAGE_LAMP = registerBlock("cage_lamp",
            () -> new CageLampBlock(BlockBehaviour.Properties.of()
                    .strength(0.5F, 6.0F)
                    .sound(SoundType.STONE)
                    .requiresCorrectToolForDrops()
                    .noOcclusion()
                    .lightLevel(state -> 15)));

    public static final RegistrySupplier<Block> FLOOD_LAMP = registerBlock("flood_lamp",
            () -> new CageLampBlock(BlockBehaviour.Properties.of()
                    .strength(0.5F, 6.0F)
                    .sound(SoundType.STONE)
                    .requiresCorrectToolForDrops()
                    .noOcclusion()
                    .lightLevel(state -> 15)));

    public static final RegistrySupplier<Block> C4 = registerBlock("c4",
            () -> new C4Block(BlockBehaviour.Properties.of()
                    .strength(0.5F, 6.0F)
                    .sound(SoundType.STONE)
                    .requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> DECO_STEEL = registerBlock("deco_steel",
            () -> new Block(BlockBehaviour.Properties.of()
                    .strength(0.5F, 6.0F)
                    .sound(SoundType.STONE)
                    .requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> DECO_RUSTY_STEEL = registerBlock("deco_rusty_steel",
            () -> new Block(BlockBehaviour.Properties.of()
                    .strength(0.5F, 6.0F)
                    .sound(SoundType.STONE)
                    .requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> DECO_TUNGSTEN = registerBlock("deco_tungsten",
            () -> new Block(BlockBehaviour.Properties.of()
                    .strength(0.5F, 6.0F)
                    .sound(SoundType.STONE)
                    .requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> DECO_RED_COPPER = registerBlock("deco_red_copper",
            () -> new Block(BlockBehaviour.Properties.of()
                    .strength(0.5F, 6.0F)
                    .sound(SoundType.STONE)
                    .requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> DECO_ALUMINUM = registerBlock("deco_aluminum",
            () -> new Block(BlockBehaviour.Properties.of()
                    .strength(0.5F, 6.0F)
                    .sound(SoundType.STONE)
                    .requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> DECO_BERYLLIUM = registerBlock("deco_beryllium",
            () -> new Block(BlockBehaviour.Properties.of()
                    .strength(0.5F, 6.0F)
                    .sound(SoundType.STONE)
                    .requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> DECO_LEAD = registerBlock("deco_lead",
            () -> new Block(BlockBehaviour.Properties.of()
                    .strength(0.5F, 6.0F)
                    .sound(SoundType.STONE)
                    .requiresCorrectToolForDrops()));

    //  ДОБАВЛЕНО: Ядерные осадки (как снег)
    public static final RegistrySupplier<Block> NUCLEAR_FALLOUT = registerBlock("nuclear_fallout",
            () -> new SnowLayerBlock(BlockBehaviour.Properties.copy(Blocks.SNOW)
                    .strength(0.1F)
                    // Тут можно добавить .lightLevel() если он должен светиться,
                    // .emissiveRendering() и так далее
            ));

    public static final RegistrySupplier<Block> DOOR_BUNKER = registerBlock("door_bunker",
            () -> new net.minecraft.world.level.block.DoorBlock(BlockBehaviour.Properties.copy(Blocks.NETHERITE_BLOCK).sound(SoundType.NETHERITE_BLOCK).noOcclusion(), BlockSetType.STONE));

    public static final RegistrySupplier<Block> DOOR_OFFICE = registerBlock("door_office",
            () -> new net.minecraft.world.level.block.DoorBlock(BlockBehaviour.Properties.copy(Blocks.CHERRY_WOOD).sound(SoundType.CHERRY_WOOD).noOcclusion(), BlockSetType.CHERRY));

    public static final RegistrySupplier<Block> METAL_DOOR = registerBlock("metal_door",
            () -> new net.minecraft.world.level.block.DoorBlock(BlockBehaviour.Properties.copy(Blocks.CHAIN).sound(SoundType.CHAIN).noOcclusion(), BlockSetType.BIRCH));


    // ============ ТЕХНИЧЕСКИЕ И ДЕКОРАТИВНЫЕ БЛОКИ ============

    public static final RegistrySupplier<Block> DORNIER = registerBlock("dornier",
            () -> new BarrelBlock(Block.Properties.copy(Blocks.STONE).strength(1.5F, 6.0F).noOcclusion()));

    public static final RegistrySupplier<Block> ORE_OIL = registerBlock("ore_oil",
            () -> new Block(Block.Properties.copy(Blocks.STONE).strength(3.0F, 3.0F).noOcclusion()));

    public static final RegistrySupplier<Block> BEDROCK_OIL = registerBlock("bedrock_oil",
            () -> new Block(Block.Properties.copy(Blocks.STONE).strength(50.0F, 1200.0F).noOcclusion()));

    public static final RegistrySupplier<Block> DEPTH_STONE = registerBlock("depth_stone",
            () -> new DepthOreBlock(Block.Properties.copy(Blocks.DEEPSLATE).strength(4.5F, 6.0F).noOcclusion()));
    public static final RegistrySupplier<Block> DEPTH_BORAX = registerBlock("depth_borax",
            () -> new DepthOreBlock(Block.Properties.copy(Blocks.DEEPSLATE).strength(4.5F, 6.0F).noOcclusion()));
    public static final RegistrySupplier<Block> DEPTH_CINNABAR = registerBlock("depth_cinnabar",
            () -> new DepthOreBlock(Block.Properties.copy(Blocks.DEEPSLATE).strength(4.5F, 6.0F).noOcclusion()));
    public static final RegistrySupplier<Block> DEPTH_IRON = registerBlock("depth_iron",
            () -> new DepthOreBlock(Block.Properties.copy(Blocks.DEEPSLATE).strength(4.5F, 6.0F).noOcclusion()));
    public static final RegistrySupplier<Block> DEPTH_TUNGSTEN = registerBlock("depth_tungsten",
            () -> new DepthOreBlock(Block.Properties.copy(Blocks.DEEPSLATE).strength(4.5F, 6.0F).noOcclusion()));
    public static final RegistrySupplier<Block> DEPTH_TITANIUM = registerBlock("depth_titanium",
            () -> new DepthOreBlock(Block.Properties.copy(Blocks.DEEPSLATE).strength(4.5F, 6.0F).noOcclusion()));
    public static final RegistrySupplier<Block> DEPTH_ZIRCONIUM = registerBlock("depth_zirconium",
            () -> new DepthOreBlock(Block.Properties.copy(Blocks.DEEPSLATE).strength(4.5F, 6.0F).noOcclusion()));

    public static final RegistrySupplier<Block> FILE_CABINET = registerBlock("file_cabinet",
            () -> new CrtBlock(Block.Properties.copy(Blocks.STONE).strength(1.5F, 6.0F).noOcclusion()));

    public static final RegistrySupplier<Block> REBAR = registerBlock("rebar",
            () -> new CrtBlock(Block.Properties.copy(Blocks.IRON_BLOCK).strength(3F, 6.0F).noOcclusion()));
    public static final RegistrySupplier<Block> STEEL_POLE = registerBlock("steel_pole",
            () -> new CrtBlock(Block.Properties.copy(Blocks.IRON_BLOCK).strength(2.5F, 6.0F).noOcclusion()));
    public static final RegistrySupplier<Block> ANTENNA_TOP = registerBlock("antenna_top",
            () -> new CrtBlock(Block.Properties.copy(Blocks.IRON_BLOCK).strength(2.5F, 6.0F).noOcclusion()));
    public static final RegistrySupplier<Block> PUTER = registerBlock("puter",
            () -> new CrtBlock(Block.Properties.copy(Blocks.STONE).strength(1F, 6.0F).noOcclusion()));
    public static final RegistrySupplier<Block> DECO_STEEL_SCAFFOLD = registerBlock("deco_steel_scaffold",
            () -> new CrtBlock(Block.Properties.copy(Blocks.IRON_BLOCK).strength(2.5F, 6.0F).noOcclusion()));

    public static final RegistrySupplier<Block> B29 = registerBlock("b29",
            () -> new BarrelBlock(Block.Properties.copy(Blocks.STONE).strength(1.5F, 6.0F).noOcclusion()));

    public static final RegistrySupplier<Block> MINE_FAT = registerBlock("mine_fat",
            () -> new MineNukeBlock(Block.Properties.copy(Blocks.STONE).strength(1.5F, 6.0F).noOcclusion()));

    public static final RegistrySupplier<Block> NUKE_FAT_MAN = registerBlockWithoutItem("nuke_fat_man",
            () -> new NukeFatManBlock(Block.Properties.copy(Blocks.STONE).strength(1.5F, 6.0F).noOcclusion()));

    public static final RegistrySupplier<Block> MINE_AP = registerBlock("mine_ap",
            () -> new MineBlock(Block.Properties.copy(Blocks.STONE).strength(1.5F, 6.0F).noOcclusion()));

    public static final RegistrySupplier<Block> NAVAL_MINE = registerBlock("naval_mine",
            () -> new CrtBlock(Block.Properties.copy(Blocks.IRON_BLOCK).strength(5, 6.0F).noOcclusion()));

    public static final RegistrySupplier<Block> CRATE_CONSERVE = registerBlock("crate_conserve",
            () -> new CrtBlock(Block.Properties.copy(Blocks.STONE).strength(1.5F, 6.0F).noOcclusion()));
    public static final RegistrySupplier<Block> TAPE_RECORDER = registerBlock("tape_recorder",
            () -> new CrtBlock(Block.Properties.copy(Blocks.STONE).strength(1.5F, 6.0F).noOcclusion()));

    public static final RegistrySupplier<Block> BARREL_LOX = registerBlock("barrel_lox",
            () -> new CrtBlock(Block.Properties.copy(Blocks.STONE).strength(2.0F, 6.0F).noOcclusion()));
    public static final RegistrySupplier<Block> BARREL_CORRODED = registerBlock("barrel_corroded",
            () -> new CrtBlock(Block.Properties.copy(Blocks.STONE).strength(2.0F, 6.0F).noOcclusion()));
    public static final RegistrySupplier<Block> BARREL_IRON = registerBlock("barrel_iron",
            () -> new CrtBlock(Block.Properties.copy(Blocks.STONE).strength(2.0F, 6.0F).noOcclusion()));
    public static final RegistrySupplier<Block> BARREL_PINK = registerBlock("barrel_pink",
            () -> new CrtBlock(Block.Properties.copy(Blocks.STONE).strength(2.0F, 6.0F).noOcclusion()));
    public static final RegistrySupplier<Block> BARREL_PLASTIC = registerBlock("barrel_plastic",
            () -> new CrtBlock(Block.Properties.copy(Blocks.STONE).strength(2.0F, 6.0F).noOcclusion()));
    public static final RegistrySupplier<Block> BARREL_RED = registerBlock("barrel_red",
            () -> new CrtBlock(Block.Properties.copy(Blocks.STONE).strength(2.0F, 6.0F).noOcclusion()));
    public static final RegistrySupplier<Block> BARREL_STEEL = registerBlock("barrel_steel",
            () -> new CrtBlock(Block.Properties.copy(Blocks.STONE).strength(2.0F, 6.0F).noOcclusion()));
    public static final RegistrySupplier<Block> BARREL_TAINT = registerBlock("barrel_taint",
            () -> new CrtBlock(Block.Properties.copy(Blocks.STONE).strength(2.0F, 6.0F).noOcclusion()));
    public static final RegistrySupplier<Block> BARREL_TCALLOY = registerBlock("barrel_tcalloy",
            () -> new CrtBlock(Block.Properties.copy(Blocks.STONE).strength(2.0F, 6.0F).noOcclusion()));
    public static final RegistrySupplier<Block> BARREL_VITRIFIED = registerBlock("barrel_vitrified",
            () -> new CrtBlock(Block.Properties.copy(Blocks.STONE).strength(2.0F, 6.0F).noOcclusion()));
    public static final RegistrySupplier<Block> BARREL_YELLOW = registerBlock("barrel_yellow",
            () -> new CrtBlock(Block.Properties.copy(Blocks.STONE).strength(2.0F, 6.0F).noOcclusion()));

    public static final RegistrySupplier<Block> BARBED_WIRE = registerBlock("barbed_wire",
            () -> new BarbedWireBlock(Block.Properties.copy(Blocks.STONE).strength(1.5F, 6.0F).noOcclusion()));
    public static final RegistrySupplier<Block> BARBED_WIRE_FIRE = registerBlock("barbed_wire_fire",
            () -> new BarbedWireFireBlock(Block.Properties.copy(Blocks.STONE).strength(1.5F, 6.0F).noOcclusion()));
    public static final RegistrySupplier<Block> BARBED_WIRE_WITHER = registerBlock("barbed_wire_wither",
            () -> new BarbedWireWitherBlock(Block.Properties.copy(Blocks.STONE).strength(1.5F, 6.0F).noOcclusion()));
    public static final RegistrySupplier<Block> BARBED_WIRE_POISON = registerBlock("barbed_wire_poison",
            () -> new BarbedWirePoisonBlock(Block.Properties.copy(Blocks.STONE).strength(1.5F, 6.0F).noOcclusion()));
    public static final RegistrySupplier<Block> BARBED_WIRE_RAD = registerBlock("barbed_wire_rad",
            () -> new BarbedWireRadBlock(Block.Properties.copy(Blocks.STONE).strength(1.5F, 6.0F).noOcclusion()));

    public static final RegistrySupplier<Block> TOASTER = registerBlock("toaster",
            () -> new CrtBlock(Block.Properties.copy(Blocks.STONE).strength(1.5F, 6.0F).noOcclusion()));
    public static final RegistrySupplier<Block> CRT_BSOD = registerBlock("crt_bsod",
            () -> new CrtBlock(Block.Properties.copy(Blocks.STONE).strength(1.5F, 6.0F).noOcclusion()));
    public static final RegistrySupplier<Block> CRT_CLEAN = registerBlock("crt_clean",
            () -> new CrtBlock(Block.Properties.copy(Blocks.STONE).strength(1.5F, 6.0F).noOcclusion()));
    public static final RegistrySupplier<Block> CRT_BROKEN = registerBlock("crt_broken",
            () -> new CrtBlock(Block.Properties.copy(Blocks.STONE).strength(1.5F, 6.0F).noOcclusion()));


    // ======================================================================

    public static final RegistrySupplier<Block> DEAD_DIRT  = registerBlock("dead_dirt",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.DIRT).strength(0.5f, 4.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> GEYSIR_DIRT  = registerBlock("geysir_dirt",
            () -> new GeysirBlock(BlockBehaviour.Properties.copy(Blocks.DIRT).strength(0.5f, 4.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> GEYSIR_STONE  = registerBlock("geysir_stone",
            () -> new GeysirBlock(BlockBehaviour.Properties.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));


    public static final RegistrySupplier<Block> SELLAFIELD_SLAKED  = registerBlock("sellafield_slaked",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> SELLAFIELD_SLAKED1  = registerBlock("sellafield_slaked1",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> SELLAFIELD_SLAKED2  = registerBlock("sellafield_slaked2",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> SELLAFIELD_SLAKED3  = registerBlock("sellafield_slaked3",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));

    // ГРАВИТИРУЮЩИЕ ВЕРСИИ СЕЛЛАФИТА (NEW!)

    public static final RegistrySupplier<Block> BURNED_GRASS  = registerBlock("burned_grass",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.GRASS_BLOCK).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> FALLING_SELLAFIT1 = BLOCKS.register("falling_sellafit1",
            () -> new FallingSellafit(SELLAFIELD_SLAKED.get()));

    public static final RegistrySupplier<Block> FALLING_SELLAFIT2 = BLOCKS.register("falling_sellafit2",
            () -> new FallingSellafit(SELLAFIELD_SLAKED1.get()));

    public static final RegistrySupplier<Block> FALLING_SELLAFIT3 = BLOCKS.register("falling_sellafit3",
            () -> new FallingSellafit(SELLAFIELD_SLAKED2.get()));

    public static final RegistrySupplier<Block> FALLING_SELLAFIT4 = BLOCKS.register("falling_sellafit4",
            () -> new FallingSellafit(SELLAFIELD_SLAKED3.get()));

    public static final RegistrySupplier<Block> ASPHALT = registerBlock("asphalt",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> BARRICADE = registerBlock("barricade",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> BASALT_BRICK = registerBlock("basalt_brick",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> BASALT_POLISHED = registerBlock("basalt_polished",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> BRICK_BASE = registerBlock("brick_base",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> BRICK_DUCRETE = registerBlock("brick_ducrete",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> BRICK_FIRE = registerBlock("brick_fire",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> BRICK_LIGHT = registerBlock("brick_light",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> BRICK_OBSIDIAN = registerBlock("brick_obsidian",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> CONCRETE_ASBESTOS = registerBlock("concrete_asbestos",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> CONCRETE_BLACK = registerBlock("concrete_black",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> CONCRETE_BLUE = registerBlock("concrete_blue",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> CONCRETE_BROWN = registerBlock("concrete_brown",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> CONCRETE_COLORED_BRONZE = registerBlock("concrete_colored_bronze",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> CONCRETE_COLORED_INDIGO = registerBlock("concrete_colored_indigo",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> CONCRETE_COLORED_MACHINE = registerBlock("concrete_colored_machine",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> CONCRETE_COLORED_MACHINE_STRIPE = registerBlock("concrete_colored_machine_stripe",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> CONCRETE_COLORED_PINK = registerBlock("concrete_colored_pink",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> CONCRETE_COLORED_PURPLE = registerBlock("concrete_colored_purple",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> CONCRETE_COLORED_SAND = registerBlock("concrete_colored_sand",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> CONCRETE_CYAN = registerBlock("concrete_cyan",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> CONCRETE_GRAY = registerBlock("concrete_gray",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> CONCRETE_GREEN = registerBlock("concrete_green",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> CONCRETE_LIGHT_BLUE = registerBlock("concrete_light_blue",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> CONCRETE_LIME = registerBlock("concrete_lime",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> CONCRETE_MAGENTA = registerBlock("concrete_magenta",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> CONCRETE_MARKED = registerBlock("concrete_marked",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> CONCRETE_ORANGE = registerBlock("concrete_orange",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> CONCRETE_PINK = registerBlock("concrete_pink",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> CONCRETE_PURPLE = registerBlock("concrete_purple",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> CONCRETE_REBAR = registerBlock("concrete_rebar",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> CONCRETE_REBAR_ALT = registerBlock("concrete_rebar_alt",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> CONCRETE_RED = registerBlock("concrete_red",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> CONCRETE_SILVER = registerBlock("concrete_silver",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> CONCRETE_SUPER = registerBlock("concrete_super",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> CONCRETE_SUPER_BROKEN = registerBlock("concrete_super_broken",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> CONCRETE_SUPER_M0 = registerBlock("concrete_super_m0",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> CONCRETE_SUPER_M1 = registerBlock("concrete_super_m1",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> CONCRETE_SUPER_M2 = registerBlock("concrete_super_m2",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> CONCRETE_SUPER_M3 = registerBlock("concrete_super_m3",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> CONCRETE_TILE = registerBlock("concrete_tile",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> CONCRETE_TILE_TREFOIL = registerBlock("concrete_tile_trefoil",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> CONCRETE_WHITE = registerBlock("concrete_white",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> CONCRETE_YELLOW = registerBlock("concrete_yellow",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> CONCRETE_FLAT = registerBlock("concrete_flat",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> DEPTH_BRICK = registerBlock("depth_brick",
            () -> new DepthOreBlock(BlockBehaviour.Properties.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> DEPTH_NETHER_BRICK = registerBlock("depth_nether_brick",
            () -> new DepthOreBlock(BlockBehaviour.Properties.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> DEPTH_NETHER_TILES = registerBlock("depth_nether_tiles",
            () -> new DepthOreBlock(BlockBehaviour.Properties.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> DEPTH_STONE_NETHER = registerBlock("depth_stone_nether",
            () -> new DepthOreBlock(BlockBehaviour.Properties.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> DEPTH_TILES = registerBlock("depth_tiles",
            () -> new DepthOreBlock(BlockBehaviour.Properties.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> GNEISS_BRICK = registerBlock("gneiss_brick",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> GNEISS_CHISELED = registerBlock("gneiss_chiseled",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> GNEISS_STONE = registerBlock("gneiss_stone",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> GNEISS_TILE = registerBlock("gneiss_tile",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> METEOR = registerBlock("meteor",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> METEOR_BRICK = registerBlock("meteor_brick",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> METEOR_BRICK_CHISELED = registerBlock("meteor_brick_chiseled",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> METEOR_BRICK_CRACKED = registerBlock("meteor_brick_cracked",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> METEOR_BRICK_MOSSY = registerBlock("meteor_brick_mossy",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> METEOR_COBBLE = registerBlock("meteor_cobble",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> METEOR_CRUSHED = registerBlock("meteor_crushed",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> METEOR_PILLAR = registerBlock("meteor_pillar",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> METEOR_POLISHED = registerBlock("meteor_polished",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> METEOR_TREASURE = registerBlock("meteor_treasure",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> VINYL_TILE = registerBlock("vinyl_tile",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> VINYL_TILE_SMALL = registerBlock("vinyl_tile_small",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> CONCRETE_PILLAR  = registerBlock("concrete_pillar",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> CONCRETE_ASBESTOS_STAIRS = registerBlock("concrete_asbestos_stairs",
            () -> new StairBlock(CONCRETE_ASBESTOS.get().defaultBlockState(), BlockBehaviour.Properties.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> CONCRETE_BLACK_STAIRS = registerBlock("concrete_black_stairs",
            () -> new StairBlock(CONCRETE_BLACK.get().defaultBlockState(), BlockBehaviour.Properties.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> CONCRETE_BLUE_STAIRS = registerBlock("concrete_blue_stairs",
            () -> new StairBlock(CONCRETE_BLUE.get().defaultBlockState(), BlockBehaviour.Properties.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> CONCRETE_BROWN_STAIRS = registerBlock("concrete_brown_stairs",
            () -> new StairBlock(CONCRETE_BROWN.get().defaultBlockState(), BlockBehaviour.Properties.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> CONCRETE_COLORED_BRONZE_STAIRS = registerBlock("concrete_colored_bronze_stairs",
            () -> new StairBlock(CONCRETE_COLORED_BRONZE.get().defaultBlockState(), BlockBehaviour.Properties.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> CONCRETE_COLORED_INDIGO_STAIRS = registerBlock("concrete_colored_indigo_stairs",
            () -> new StairBlock(CONCRETE_COLORED_INDIGO.get().defaultBlockState(), BlockBehaviour.Properties.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> CONCRETE_COLORED_MACHINE_STAIRS = registerBlock("concrete_colored_machine_stairs",
            () -> new StairBlock(CONCRETE_COLORED_MACHINE.get().defaultBlockState(), BlockBehaviour.Properties.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> CONCRETE_COLORED_PINK_STAIRS = registerBlock("concrete_colored_pink_stairs",
            () -> new StairBlock(CONCRETE_COLORED_PINK.get().defaultBlockState(), BlockBehaviour.Properties.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> CONCRETE_COLORED_PURPLE_STAIRS = registerBlock("concrete_colored_purple_stairs",
            () -> new StairBlock(CONCRETE_COLORED_PURPLE.get().defaultBlockState(), BlockBehaviour.Properties.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> CONCRETE_COLORED_SAND_STAIRS = registerBlock("concrete_colored_sand_stairs",
            () -> new StairBlock(CONCRETE_COLORED_SAND.get().defaultBlockState(), BlockBehaviour.Properties.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> CONCRETE_CYAN_STAIRS = registerBlock("concrete_cyan_stairs",
            () -> new StairBlock(CONCRETE_CYAN.get().defaultBlockState(), BlockBehaviour.Properties.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> CONCRETE_GRAY_STAIRS = registerBlock("concrete_gray_stairs",
            () -> new StairBlock(CONCRETE_GRAY.get().defaultBlockState(), BlockBehaviour.Properties.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> CONCRETE_GREEN_STAIRS = registerBlock("concrete_green_stairs",
            () -> new StairBlock(CONCRETE_GREEN.get().defaultBlockState(), BlockBehaviour.Properties.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> CONCRETE_LIGHT_BLUE_STAIRS = registerBlock("concrete_light_blue_stairs",
            () -> new StairBlock(CONCRETE_LIGHT_BLUE.get().defaultBlockState(), BlockBehaviour.Properties.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> CONCRETE_LIME_STAIRS = registerBlock("concrete_lime_stairs",
            () -> new StairBlock(CONCRETE_LIME.get().defaultBlockState(), BlockBehaviour.Properties.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> CONCRETE_MAGENTA_STAIRS = registerBlock("concrete_magenta_stairs",
            () -> new StairBlock(CONCRETE_MAGENTA.get().defaultBlockState(), BlockBehaviour.Properties.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> CONCRETE_ORANGE_STAIRS = registerBlock("concrete_orange_stairs",
            () -> new StairBlock(CONCRETE_ORANGE.get().defaultBlockState(), BlockBehaviour.Properties.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> CONCRETE_PINK_STAIRS = registerBlock("concrete_pink_stairs",
            () -> new StairBlock(CONCRETE_PINK.get().defaultBlockState(), BlockBehaviour.Properties.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> CONCRETE_PURPLE_STAIRS = registerBlock("concrete_purple_stairs",
            () -> new StairBlock(CONCRETE_PURPLE.get().defaultBlockState(), BlockBehaviour.Properties.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> CONCRETE_RED_STAIRS = registerBlock("concrete_red_stairs",
            () -> new StairBlock(CONCRETE_RED.get().defaultBlockState(), BlockBehaviour.Properties.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> CONCRETE_SILVER_STAIRS = registerBlock("concrete_silver_stairs",
            () -> new StairBlock(CONCRETE_SILVER.get().defaultBlockState(), BlockBehaviour.Properties.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> CONCRETE_WHITE_STAIRS = registerBlock("concrete_white_stairs",
            () -> new StairBlock(CONCRETE_WHITE.get().defaultBlockState(), BlockBehaviour.Properties.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> CONCRETE_YELLOW_STAIRS = registerBlock("concrete_yellow_stairs",
            () -> new StairBlock(CONCRETE_YELLOW.get().defaultBlockState(), BlockBehaviour.Properties.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> CONCRETE_SUPER_STAIRS = registerBlock("concrete_super_stairs",
            () -> new StairBlock(CONCRETE_SUPER.get().defaultBlockState(), BlockBehaviour.Properties.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> CONCRETE_SUPER_M0_STAIRS = registerBlock("concrete_super_m0_stairs",
            () -> new StairBlock(CONCRETE_SUPER_M0.get().defaultBlockState(), BlockBehaviour.Properties.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> CONCRETE_SUPER_M1_STAIRS = registerBlock("concrete_super_m1_stairs",
            () -> new StairBlock(CONCRETE_SUPER_M1.get().defaultBlockState(), BlockBehaviour.Properties.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> CONCRETE_SUPER_M2_STAIRS = registerBlock("concrete_super_m2_stairs",
            () -> new StairBlock(CONCRETE_SUPER_M2.get().defaultBlockState(), BlockBehaviour.Properties.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> CONCRETE_SUPER_M3_STAIRS = registerBlock("concrete_super_m3_stairs",
            () -> new StairBlock(CONCRETE_SUPER_M3.get().defaultBlockState(), BlockBehaviour.Properties.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> CONCRETE_SUPER_BROKEN_STAIRS = registerBlock("concrete_super_broken_stairs",
            () -> new StairBlock(CONCRETE_SUPER_BROKEN.get().defaultBlockState(), BlockBehaviour.Properties.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> CONCRETE_REBAR_STAIRS = registerBlock("concrete_rebar_stairs",
            () -> new StairBlock(CONCRETE_REBAR.get().defaultBlockState(), BlockBehaviour.Properties.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> CONCRETE_FLAT_STAIRS = registerBlock("concrete_flat_stairs",
            () -> new StairBlock(CONCRETE_FLAT.get().defaultBlockState(), BlockBehaviour.Properties.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> CONCRETE_TILE_STAIRS = registerBlock("concrete_tile_stairs",
            () -> new StairBlock(CONCRETE_TILE.get().defaultBlockState(), BlockBehaviour.Properties.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));

	public static final RegistrySupplier<Block> DEPTH_STONE_STAIRS = registerBlock("depth_stone_stairs",
            () -> new StairBlock(DEPTH_BRICK.get().defaultBlockState(), BlockBehaviour.Properties.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> DEPTH_BRICK_STAIRS = registerBlock("depth_brick_stairs",
            () -> new StairBlock(DEPTH_BRICK.get().defaultBlockState(), BlockBehaviour.Properties.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> DEPTH_TILES_STAIRS = registerBlock("depth_tiles_stairs",
            () -> new StairBlock(DEPTH_TILES.get().defaultBlockState(), BlockBehaviour.Properties.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> DEPTH_NETHER_BRICK_STAIRS = registerBlock("depth_nether_brick_stairs",
            () -> new StairBlock(DEPTH_NETHER_BRICK.get().defaultBlockState(), BlockBehaviour.Properties.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> DEPTH_NETHER_TILES_STAIRS = registerBlock("depth_nether_tiles_stairs",
            () -> new StairBlock(DEPTH_NETHER_TILES.get().defaultBlockState(), BlockBehaviour.Properties.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> GNEISS_TILE_STAIRS = registerBlock("gneiss_tile_stairs",
            () -> new StairBlock(GNEISS_TILE.get().defaultBlockState(), BlockBehaviour.Properties.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> GNEISS_BRICK_STAIRS = registerBlock("gneiss_brick_stairs",
            () -> new StairBlock(GNEISS_BRICK.get().defaultBlockState(), BlockBehaviour.Properties.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> BRICK_BASE_STAIRS = registerBlock("brick_base_stairs",
            () -> new StairBlock(BRICK_BASE.get().defaultBlockState(), BlockBehaviour.Properties.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> BRICK_LIGHT_STAIRS = registerBlock("brick_light_stairs",
            () -> new StairBlock(BRICK_LIGHT.get().defaultBlockState(), BlockBehaviour.Properties.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> BRICK_FIRE_STAIRS = registerBlock("brick_fire_stairs",
            () -> new StairBlock(BRICK_FIRE.get().defaultBlockState(), BlockBehaviour.Properties.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> BRICK_OBSIDIAN_STAIRS = registerBlock("brick_obsidian_stairs",
            () -> new StairBlock(BRICK_OBSIDIAN.get().defaultBlockState(), BlockBehaviour.Properties.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> VINYL_TILE_STAIRS = registerBlock("vinyl_tile_stairs",
            () -> new StairBlock(VINYL_TILE.get().defaultBlockState(), BlockBehaviour.Properties.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> VINYL_TILE_SMALL_STAIRS = registerBlock("vinyl_tile_small_stairs",
            () -> new StairBlock(VINYL_TILE_SMALL.get().defaultBlockState(), BlockBehaviour.Properties.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> BRICK_DUCRETE_STAIRS = registerBlock("brick_ducrete_stairs",
            () -> new StairBlock(BRICK_DUCRETE.get().defaultBlockState(), BlockBehaviour.Properties.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> ASPHALT_STAIRS = registerBlock("asphalt_stairs",
            () -> new StairBlock(ASPHALT.get().defaultBlockState(), BlockBehaviour.Properties.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> BASALT_POLISHED_STAIRS = registerBlock("basalt_polished_stairs",
            () -> new StairBlock(BASALT_POLISHED.get().defaultBlockState(), BlockBehaviour.Properties.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> BASALT_BRICK_STAIRS = registerBlock("basalt_brick_stairs",
            () -> new StairBlock(BASALT_BRICK.get().defaultBlockState(), BlockBehaviour.Properties.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> METEOR_POLISHED_STAIRS = registerBlock("meteor_polished_stairs",
            () -> new StairBlock(METEOR_POLISHED.get().defaultBlockState(), BlockBehaviour.Properties.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> METEOR_BRICK_STAIRS = registerBlock("meteor_brick_stairs",
            () -> new StairBlock(METEOR_BRICK.get().defaultBlockState(), BlockBehaviour.Properties.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> METEOR_BRICK_CRACKED_STAIRS = registerBlock("meteor_brick_cracked_stairs",
            () -> new StairBlock(METEOR_BRICK_CRACKED.get().defaultBlockState(), BlockBehaviour.Properties.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> METEOR_BRICK_MOSSY_STAIRS = registerBlock("meteor_brick_mossy_stairs",
            () -> new StairBlock(METEOR_BRICK_MOSSY.get().defaultBlockState(), BlockBehaviour.Properties.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> METEOR_CRUSHED_STAIRS = registerBlock("meteor_crushed_stairs",
            () -> new StairBlock(METEOR_CRUSHED.get().defaultBlockState(), BlockBehaviour.Properties.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));


    public static final RegistrySupplier<Block> DEPTH_STONE_SLAB = registerBlock("depth_stone_slab",
            () -> new SlabBlock(BlockBehaviour.Properties.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));
    public static final RegistrySupplier<Block> ASPHALT_SLAB = registerBlock("asphalt_slab",
            () -> new SlabBlock(BlockBehaviour.Properties.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));
    public static final RegistrySupplier<Block> BASALT_BRICK_SLAB = registerBlock("basalt_brick_slab",
            () -> new SlabBlock(BlockBehaviour.Properties.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));
    public static final RegistrySupplier<Block> BASALT_POLISHED_SLAB = registerBlock("basalt_polished_slab",
            () -> new SlabBlock(BlockBehaviour.Properties.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));
    public static final RegistrySupplier<Block> BRICK_BASE_SLAB = registerBlock("brick_base_slab",
            () -> new SlabBlock(BlockBehaviour.Properties.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));
    public static final RegistrySupplier<Block> BRICK_DUCRETE_SLAB = registerBlock("brick_ducrete_slab",
            () -> new SlabBlock(BlockBehaviour.Properties.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));
    public static final RegistrySupplier<Block> BRICK_FIRE_SLAB = registerBlock("brick_fire_slab",
            () -> new SlabBlock(BlockBehaviour.Properties.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));
    public static final RegistrySupplier<Block> BRICK_LIGHT_SLAB = registerBlock("brick_light_slab",
            () -> new SlabBlock(BlockBehaviour.Properties.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));
    public static final RegistrySupplier<Block> BRICK_OBSIDIAN_SLAB = registerBlock("brick_obsidian_slab",
            () -> new SlabBlock(BlockBehaviour.Properties.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));
    public static final RegistrySupplier<Block> CONCRETE_ASBESTOS_SLAB = registerBlock("concrete_asbestos_slab",
            () -> new SlabBlock(BlockBehaviour.Properties.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));
    public static final RegistrySupplier<Block> CONCRETE_BLACK_SLAB = registerBlock("concrete_black_slab",
            () -> new SlabBlock(BlockBehaviour.Properties.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));
    public static final RegistrySupplier<Block> CONCRETE_BLUE_SLAB = registerBlock("concrete_blue_slab",
            () -> new SlabBlock(BlockBehaviour.Properties.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));
    public static final RegistrySupplier<Block> CONCRETE_BROWN_SLAB = registerBlock("concrete_brown_slab",
            () -> new SlabBlock(BlockBehaviour.Properties.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));
    public static final RegistrySupplier<Block> CONCRETE_COLORED_BRONZE_SLAB = registerBlock("concrete_colored_bronze_slab",
            () -> new SlabBlock(BlockBehaviour.Properties.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));
    public static final RegistrySupplier<Block> CONCRETE_COLORED_INDIGO_SLAB = registerBlock("concrete_colored_indigo_slab",
            () -> new SlabBlock(BlockBehaviour.Properties.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));
    public static final RegistrySupplier<Block> CONCRETE_COLORED_MACHINE_SLAB = registerBlock("concrete_colored_machine_slab",
            () -> new SlabBlock(BlockBehaviour.Properties.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));
    public static final RegistrySupplier<Block> CONCRETE_COLORED_PINK_SLAB = registerBlock("concrete_colored_pink_slab",
            () -> new SlabBlock(BlockBehaviour.Properties.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));
    public static final RegistrySupplier<Block> CONCRETE_COLORED_PURPLE_SLAB = registerBlock("concrete_colored_purple_slab",
            () -> new SlabBlock(BlockBehaviour.Properties.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));
    public static final RegistrySupplier<Block> CONCRETE_COLORED_SAND_SLAB = registerBlock("concrete_colored_sand_slab",
            () -> new SlabBlock(BlockBehaviour.Properties.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));
    public static final RegistrySupplier<Block> CONCRETE_CYAN_SLAB = registerBlock("concrete_cyan_slab",
            () -> new SlabBlock(BlockBehaviour.Properties.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));
    public static final RegistrySupplier<Block> CONCRETE_GRAY_SLAB = registerBlock("concrete_gray_slab",
            () -> new SlabBlock(BlockBehaviour.Properties.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));
    public static final RegistrySupplier<Block> CONCRETE_GREEN_SLAB = registerBlock("concrete_green_slab",
            () -> new SlabBlock(BlockBehaviour.Properties.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));
    public static final RegistrySupplier<Block> CONCRETE_LIGHT_BLUE_SLAB = registerBlock("concrete_light_blue_slab",
            () -> new SlabBlock(BlockBehaviour.Properties.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));
    public static final RegistrySupplier<Block> CONCRETE_LIME_SLAB = registerBlock("concrete_lime_slab",
            () -> new SlabBlock(BlockBehaviour.Properties.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));
    public static final RegistrySupplier<Block> CONCRETE_MAGENTA_SLAB = registerBlock("concrete_magenta_slab",
            () -> new SlabBlock(BlockBehaviour.Properties.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));
    public static final RegistrySupplier<Block> CONCRETE_ORANGE_SLAB = registerBlock("concrete_orange_slab",
            () -> new SlabBlock(BlockBehaviour.Properties.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));
    public static final RegistrySupplier<Block> CONCRETE_PINK_SLAB = registerBlock("concrete_pink_slab",
            () -> new SlabBlock(BlockBehaviour.Properties.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));
    public static final RegistrySupplier<Block> CONCRETE_PURPLE_SLAB = registerBlock("concrete_purple_slab",
            () -> new SlabBlock(BlockBehaviour.Properties.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));
    public static final RegistrySupplier<Block> CONCRETE_REBAR_SLAB = registerBlock("concrete_rebar_slab",
            () -> new SlabBlock(BlockBehaviour.Properties.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));
    public static final RegistrySupplier<Block> CONCRETE_RED_SLAB = registerBlock("concrete_red_slab",
            () -> new SlabBlock(BlockBehaviour.Properties.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));
    public static final RegistrySupplier<Block> CONCRETE_SILVER_SLAB = registerBlock("concrete_silver_slab",
            () -> new SlabBlock(BlockBehaviour.Properties.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));
    public static final RegistrySupplier<Block> CONCRETE_SUPER_SLAB = registerBlock("concrete_super_slab",
            () -> new SlabBlock(BlockBehaviour.Properties.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));
    public static final RegistrySupplier<Block> CONCRETE_SUPER_BROKEN_SLAB = registerBlock("concrete_super_broken_slab",
            () -> new SlabBlock(BlockBehaviour.Properties.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));
    public static final RegistrySupplier<Block> CONCRETE_SUPER_M0_SLAB = registerBlock("concrete_super_m0_slab",
            () -> new SlabBlock(BlockBehaviour.Properties.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));
    public static final RegistrySupplier<Block> CONCRETE_SUPER_M1_SLAB = registerBlock("concrete_super_m1_slab",
            () -> new SlabBlock(BlockBehaviour.Properties.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));
    public static final RegistrySupplier<Block> CONCRETE_SUPER_M2_SLAB = registerBlock("concrete_super_m2_slab",
            () -> new SlabBlock(BlockBehaviour.Properties.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));
    public static final RegistrySupplier<Block> CONCRETE_SUPER_M3_SLAB = registerBlock("concrete_super_m3_slab",
            () -> new SlabBlock(BlockBehaviour.Properties.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));
    public static final RegistrySupplier<Block> CONCRETE_TILE_SLAB = registerBlock("concrete_tile_slab",
            () -> new SlabBlock(BlockBehaviour.Properties.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));
    public static final RegistrySupplier<Block> CONCRETE_WHITE_SLAB = registerBlock("concrete_white_slab",
            () -> new SlabBlock(BlockBehaviour.Properties.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));
    public static final RegistrySupplier<Block> CONCRETE_YELLOW_SLAB = registerBlock("concrete_yellow_slab",
            () -> new SlabBlock(BlockBehaviour.Properties.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));
    public static final RegistrySupplier<Block> CONCRETE_FLAT_SLAB = registerBlock("concrete_flat_slab",
            () -> new SlabBlock(BlockBehaviour.Properties.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));
    public static final RegistrySupplier<Block> DEPTH_BRICK_SLAB = registerBlock("depth_brick_slab",
            () -> new SlabBlock(BlockBehaviour.Properties.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));
    public static final RegistrySupplier<Block> DEPTH_NETHER_BRICK_SLAB = registerBlock("depth_nether_brick_slab",
            () -> new SlabBlock(BlockBehaviour.Properties.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));
    public static final RegistrySupplier<Block> DEPTH_NETHER_TILES_SLAB = registerBlock("depth_nether_tiles_slab",
            () -> new SlabBlock(BlockBehaviour.Properties.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));
    public static final RegistrySupplier<Block> DEPTH_STONE_NETHER_SLAB = registerBlock("depth_stone_nether_slab",
            () -> new SlabBlock(BlockBehaviour.Properties.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));
    public static final RegistrySupplier<Block> DEPTH_TILES_SLAB = registerBlock("depth_tiles_slab",
            () -> new SlabBlock(BlockBehaviour.Properties.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));
    public static final RegistrySupplier<Block> GNEISS_BRICK_SLAB = registerBlock("gneiss_brick_slab",
            () -> new SlabBlock(BlockBehaviour.Properties.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));
    public static final RegistrySupplier<Block> GNEISS_TILE_SLAB = registerBlock("gneiss_tile_slab",
            () -> new SlabBlock(BlockBehaviour.Properties.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));
    public static final RegistrySupplier<Block> METEOR_BRICK_SLAB = registerBlock("meteor_brick_slab",
            () -> new SlabBlock(BlockBehaviour.Properties.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));
    public static final RegistrySupplier<Block> METEOR_BRICK_CRACKED_SLAB = registerBlock("meteor_brick_cracked_slab",
            () -> new SlabBlock(BlockBehaviour.Properties.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));
    public static final RegistrySupplier<Block> METEOR_BRICK_MOSSY_SLAB = registerBlock("meteor_brick_mossy_slab",
            () -> new SlabBlock(BlockBehaviour.Properties.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));
    public static final RegistrySupplier<Block> METEOR_CRUSHED_SLAB = registerBlock("meteor_crushed_slab",
            () -> new SlabBlock(BlockBehaviour.Properties.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));
    public static final RegistrySupplier<Block> METEOR_POLISHED_SLAB = registerBlock("meteor_polished_slab",
            () -> new SlabBlock(BlockBehaviour.Properties.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));
    public static final RegistrySupplier<Block> VINYL_TILE_SLAB = registerBlock("vinyl_tile_slab",
            () -> new SlabBlock(BlockBehaviour.Properties.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));
    public static final RegistrySupplier<Block> VINYL_TILE_SMALL_SLAB = registerBlock("vinyl_tile_small_slab",
            () -> new SlabBlock(BlockBehaviour.Properties.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));



    public static final RegistrySupplier<Block> CONCRETE_FAN  = registerBlock("concrete_fan",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> BRICK_CONCRETE_BROKEN = registerBlock("brick_concrete_broken",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));
    public static final RegistrySupplier<Block> BRICK_CONCRETE_BROKEN_STAIRS = registerBlock("brick_concrete_broken_stairs",
            () -> new StairBlock(ModBlocks.BRICK_CONCRETE_BROKEN.get().defaultBlockState(),
                    BlockBehaviour.Properties.copy(Blocks.STONE).sound(SoundType.STONE)));
    public static final RegistrySupplier<Block> BRICK_CONCRETE_BROKEN_SLAB = registerBlock("brick_concrete_broken_slab",
            () -> new SlabBlock(BlockBehaviour.Properties.copy(Blocks.STONE).sound(SoundType.STONE)));

    public static final RegistrySupplier<Block> BRICK_CONCRETE_CRACKED = registerBlock("brick_concrete_cracked",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));
    public static final RegistrySupplier<Block> BRICK_CONCRETE_CRACKED_STAIRS = registerBlock("brick_concrete_cracked_stairs",
            () -> new StairBlock(ModBlocks.BRICK_CONCRETE_CRACKED.get().defaultBlockState(),
                    BlockBehaviour.Properties.copy(Blocks.STONE).sound(SoundType.STONE)));
    public static final RegistrySupplier<Block> BRICK_CONCRETE_CRACKED_SLAB = registerBlock("brick_concrete_cracked_slab",
            () -> new SlabBlock(BlockBehaviour.Properties.copy(Blocks.STONE).sound(SoundType.STONE)));

    public static final RegistrySupplier<Block> BRICK_CONCRETE_MOSSY = registerBlock("brick_concrete_mossy",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));
    public static final RegistrySupplier<Block> BRICK_CONCRETE_MOSSY_STAIRS = registerBlock("brick_concrete_mossy_stairs",
            () -> new StairBlock(ModBlocks.BRICK_CONCRETE_MOSSY.get().defaultBlockState(),
                    BlockBehaviour.Properties.copy(Blocks.STONE).sound(SoundType.STONE)));
    public static final RegistrySupplier<Block> BRICK_CONCRETE_MOSSY_SLAB = registerBlock("brick_concrete_mossy_slab",
            () -> new SlabBlock(BlockBehaviour.Properties.copy(Blocks.STONE).sound(SoundType.STONE)));

    public static final RegistrySupplier<Block> BRICK_CONCRETE_MARKED = registerBlock("brick_concrete_marked",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> REINFORCED_STONE_STAIRS = registerBlock("reinforced_stone_stairs",
            () -> new StairBlock(ModBlocks.REINFORCED_STONE.get().defaultBlockState(),
                    BlockBehaviour.Properties.copy(Blocks.STONE).sound(SoundType.STONE)));
    public static final RegistrySupplier<Block> REINFORCED_STONE_SLAB = registerBlock("reinforced_stone_slab",
            () -> new SlabBlock(BlockBehaviour.Properties.copy(Blocks.STONE).sound(SoundType.STONE)));


    private static final BlockBehaviour.Properties CRATE_PROPERTIES =
            BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).sound(SoundType.METAL).strength(0.5f, 1f).requiresCorrectToolForDrops();

    public static final RegistrySupplier<Block> CRATE_IRON = registerBlockWithoutItem("crate_iron",
            () -> new IronCrateBlock(CRATE_PROPERTIES));

    public static final RegistrySupplier<Block> CRATE_STEEL = registerBlockWithoutItem("crate_steel",
            () -> new SteelCrateBlock(CRATE_PROPERTIES));

    public static final RegistrySupplier<Block> CRATE_DESH = registerBlockWithoutItem("crate_desh",
            () -> new DeshCrateBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).sound(SoundType.METAL).strength(1.5f, 2f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> CRATE_TUNGSTEN = registerBlockWithoutItem("crate_tungsten",
            () -> new TungstenCrateBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).sound(SoundType.METAL).strength(2.0f, 3f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> CRATE_TEMPLATE = registerBlockWithoutItem("crate_template",
            () -> new TemplateCrateBlock(CRATE_PROPERTIES));

    public static final RegistrySupplier<Block> WASTE_PLANKS = registerBlock("waste_planks",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.OAK_WOOD).strength(3.0f, 3.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> WASTE_LOG = registerBlock("waste_log",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.COAL_BLOCK).strength(3.0f, 3.0f).requiresCorrectToolForDrops()));


    // -----------------------<РАСТЕНИЯ>-----------------------------
    public static final RegistrySupplier<Block> STRAWBERRY_BUSH = registerBlock("strawberry_bush",
            () -> new FlowerBlock(MobEffects.LUCK, 5,
                    BlockBehaviour.Properties.copy(Blocks.ALLIUM).noOcclusion().noCollission()));


    // -----------------------<РУДЫ>-----------------------------


    public static final RegistrySupplier<Block> RESOURCE_ASBESTOS = registerBlock("resource_asbestos",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.STONE).strength(3.0f, 3.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> RESOURCE_BAUXITE = registerBlock("resource_bauxite",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.STONE).strength(3.0f, 3.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> RESOURCE_HEMATITE = registerBlock("resource_hematite",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.STONE).strength(3.0f, 3.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> RESOURCE_LIMESTONE = registerBlock("resource_limestone",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.STONE).strength(3.0f, 3.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> RESOURCE_MALACHITE = registerBlock("resource_malachite",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.STONE).strength(3.0f, 3.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> RESOURCE_SULFUR = registerBlock("resource_sulfur",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.STONE).strength(3.0f, 3.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> SEQUESTRUM_ORE = registerBlock("sequestrum_ore",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.STONE).strength(3.0f, 3.0f).requiresCorrectToolForDrops()));


    public static final RegistrySupplier<Block> LIGNITE_ORE = registerBlock("lignite_ore",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.STONE).strength(3.0f, 3.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> ALUMINUM_ORE = registerBlock("aluminum_ore",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.STONE).strength(3.0f, 3.0f).requiresCorrectToolForDrops()));


	public static final RegistrySupplier<Block> URANIUM_ORE = registerBlock("uranium_ore",
            () -> new DropExperienceBlock(BlockBehaviour.Properties.copy(Blocks.STONE).strength(3.0F, 3.0F).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> LEAD_ORE = registerBlock("lead_ore",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.STONE).strength(3.0f, 3.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> RAREGROUND_ORE = registerBlock("rareground_ore",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.STONE).strength(3.0f, 3.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> FLUORITE_ORE = registerBlock("fluorite_ore",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.STONE).strength(3.0f, 3.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> BERYLLIUM_ORE = registerBlock("beryllium_ore",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.STONE).strength(3.0f, 3.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> ASBESTOS_ORE = registerBlock("asbestos_ore",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.STONE).strength(3.0f, 3.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> CINNABAR_ORE = registerBlock("cinnabar_ore",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.STONE).strength(3.0f, 3.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> COBALT_ORE = registerBlock("cobalt_ore",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.STONE).strength(3.0f, 3.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> TUNGSTEN_ORE = registerBlock("tungsten_ore",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.STONE).strength(3.0f, 3.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> THORIUM_ORE = registerBlock("thorium_ore",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.STONE).strength(3.0f, 3.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> FREAKY_ALIEN_BLOCK = registerBlock("freaky_alien_block",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.STONE).strength(3.0f, 3.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> TITANIUM_ORE = registerBlock("titanium_ore",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.STONE).strength(3.0f, 3.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> SULFUR_ORE = registerBlock("sulfur_ore",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.STONE).strength(3.0f, 3.0f).requiresCorrectToolForDrops()));

    // Дипслейт руды
    public static final RegistrySupplier<Block> URANIUM_ORE_DEEPSLATE = registerBlock("uranium_ore_deepslate",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.DEEPSLATE).strength(5.0f, 5.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> BERYLLIUM_ORE_DEEPSLATE = registerBlock("beryllium_ore_deepslate",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.DEEPSLATE).strength(5.0f, 5.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> TITANIUM_ORE_DEEPSLATE = registerBlock("titanium_ore_deepslate",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.DEEPSLATE).strength(5.0f, 5.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> LEAD_ORE_DEEPSLATE = registerBlock("lead_ore_deepslate",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.DEEPSLATE).strength(5.0f, 5.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> RAREGROUND_ORE_DEEPSLATE = registerBlock("rareground_ore_deepslate",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.DEEPSLATE).strength(5.0f, 5.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> THORIUM_ORE_DEEPSLATE = registerBlock("thorium_ore_deepslate",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.DEEPSLATE).strength(5.0f, 5.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> ALUMINUM_ORE_DEEPSLATE = registerBlock("aluminum_ore_deepslate",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.DEEPSLATE).strength(5.0f, 5.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> COBALT_ORE_DEEPSLATE = registerBlock("cobalt_ore_deepslate",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.DEEPSLATE).strength(5.0f, 5.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> CINNABAR_ORE_DEEPSLATE = registerBlock("cinnabar_ore_deepslate",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.DEEPSLATE).strength(5.0f, 5.0f).requiresCorrectToolForDrops()));

    //======================= ЖИДКОСТИ ==========================================//










    // ==================== Helper Methods ====================

    private static RegistrySupplier<Block> registerAnvil(String name, AnvilTier tier) {
        return registerBlock(name, () -> new AnvilBlock(ANVIL_PROPERTIES, tier));
    }

    private static <T extends Block> RegistrySupplier<T> registerBlock(String name, Supplier<T> block) {
        RegistrySupplier<T> toReturn = BLOCKS.register(name, block);
        registerBlockItem(name, toReturn);
        return toReturn;
    }

    private static <T extends Block> RegistrySupplier<T> registerBlockWithoutItem(String name, Supplier<T> block) {
        return BLOCKS.register(name, block);
    }

    private static <T extends Block> RegistrySupplier<Item> registerBlockItem(String name, RegistrySupplier<T> block) {
        return ModItems.ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
    }

    public static void init() {
        BLOCKS.register();
    }
}