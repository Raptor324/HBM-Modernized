package com.hbm_m.block;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import com.hbm_m.platform.BlockProps;
import com.hbm_m.api.energy.ConverterBlock;
import com.hbm_m.api.energy.SwitchBlock;
import com.hbm_m.api.energy.WireBlock;
import com.hbm_m.block.bomb.BlockTaint;
import com.hbm_m.block.generic.BlockAbsorber;
import com.hbm_m.block.generic.BlockOre;
import com.hbm_m.block.generic.BlockSellafieldOre;
import com.hbm_m.block.generic.BlockSellafieldSlaked;
import com.hbm_m.block.generic.BlockSlag;
import com.hbm_m.block.generic.WasteEarth;
import com.hbm_m.block.bomb.NukeFatManBlock;
import com.hbm_m.block.decorations.CageLampBlock;
import com.hbm_m.block.decorations.CrtBlock;
import com.hbm_m.block.decorations.DoorBlock;
import com.hbm_m.block.decorations.SteelWallBlock;
import com.hbm_m.block.explosives.AirBombBlock;
import com.hbm_m.block.explosives.AirNukeBombBlock;
import com.hbm_m.block.explosives.C4Block;
import com.hbm_m.block.explosives.DetMinerBlock;
import com.hbm_m.block.explosives.DudFugasBlock;
import com.hbm_m.block.explosives.DudNukeBlock;
import com.hbm_m.block.explosives.ExplosiveChargeBlock;
import com.hbm_m.block.explosives.GigaDetBlock;
import com.hbm_m.block.bomb.LandmineBlock;
import com.hbm_m.block.explosives.NuclearChargeBlock;
import com.hbm_m.block.explosives.SmokeBombBlock;
import com.hbm_m.block.explosives.WasteChargeBlock;
import com.hbm_m.block.machines.ArmorTableBlock;
import com.hbm_m.block.machines.BlastFurnaceBlock;
import com.hbm_m.block.machines.BlastFurnaceExtensionBlock;
import com.hbm_m.block.machines.CargoElevatorBlock;
import com.hbm_m.block.machines.FluidDuctBlock;
import com.hbm_m.block.machines.MachineElectricFurnaceBlock;
import com.hbm_m.block.machines.MachineFurnaceBrickBlock;
import com.hbm_m.block.machines.MachineFurnaceIronBlock;
import com.hbm_m.block.machines.MachineFurnaceSteelBlock;
import com.hbm_m.block.machines.BlockDecon;
import com.hbm_m.block.machines.GeigerCounterBlock;
import com.hbm_m.block.machines.HeatingOvenBlock;
import com.hbm_m.block.machines.LaunchPadBlock;
import com.hbm_m.block.machines.LaunchPadRustedBlock;
import com.hbm_m.block.machines.MachineAdvancedAssemblerBlock;
import com.hbm_m.block.machines.MachineArcWelderBlock;
import com.hbm_m.block.machines.MachineAssemblerBlock;
import com.hbm_m.block.machines.MachineBat9000Block;
import com.hbm_m.block.machines.MachineBatteryBlock;
import com.hbm_m.block.machines.MachineBatterySocketBlock;
import com.hbm_m.block.machines.MachineBreederBlock;
import com.hbm_m.block.machines.MachineCatalyticReformerBlock;
import com.hbm_m.block.machines.MachineCentrifugeBlock;
import com.hbm_m.block.machines.MachineCombinationOvenBlock;
import com.hbm_m.block.machines.MachineChemicalFactoryBlock;
import com.hbm_m.block.machines.MachineChemicalPlantBlock;
import com.hbm_m.block.machines.MachineTowerLargeBlock;
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
import com.hbm_m.block.machines.MachineOreSlopperBlock;
import com.hbm_m.block.machines.MachinePressBlock;
import com.hbm_m.block.machines.MachinePumpjackBlock;
import com.hbm_m.block.machines.MachineLargeRadarBlock;
import com.hbm_m.block.machines.MachineRadarBlock;
import com.hbm_m.block.machines.TransitionSealBlock;
import com.hbm_m.block.machines.MachineRadarScreenBlock;
import com.hbm_m.block.machines.MachineRbmkConsoleBlock;
import com.hbm_m.block.machines.MachineRefineryBlock;
import com.hbm_m.block.machines.MachineShredderBlock;
import com.hbm_m.block.machines.MachineSilexBlock;
import com.hbm_m.block.machines.MachineSolarBoilerBlock;
import com.hbm_m.block.machines.MachineSolarMirrorsBlock;
import com.hbm_m.block.machines.MachineSolderingStationBlock;
import com.hbm_m.block.machines.MachineSteamTurbineBlock;
import com.hbm_m.block.machines.MachineSteamCondenserBlock;
import com.hbm_m.block.machines.MachineSubstationBlock;
import com.hbm_m.block.machines.MachineTowerSmallBlock;
import com.hbm_m.block.machines.MachineTurbineBlock;
import com.hbm_m.block.machines.MachineTurbofanBlock;
import com.hbm_m.block.machines.MachineVacuumDistillBlock;
import com.hbm_m.block.machines.MachineWatzPowerplantBlock;
import com.hbm_m.block.machines.MachineWoodBurnerBlock;
import com.hbm_m.block.machines.MachineZirnoxBlock;
import com.hbm_m.block.machines.MachineZirnoxDestroyedBlock;
import com.hbm_m.block.machines.rbmk.RBMKRodBlock;
import com.hbm_m.block.machines.rbmk.RBMKControlManualBlock;
import com.hbm_m.block.machines.rbmk.RBMKControlAutoBlock;
import com.hbm_m.block.machines.rbmk.RBMKModeratorBlock;
import com.hbm_m.block.machines.rbmk.RBMKAbsorberBlock;
import com.hbm_m.block.machines.rbmk.RBMKReflectorBlock;
import com.hbm_m.block.machines.rbmk.RBMKCoolerBlock;
import com.hbm_m.block.machines.rbmk.RBMKBoilerBlock;
import com.hbm_m.block.machines.rbmk.RBMKHeaterBlock;
import com.hbm_m.block.machines.rbmk.RBMKOutgasserBlock;
import com.hbm_m.block.machines.rbmk.RBMKStorageBlock;
import com.hbm_m.block.machines.rbmk.RBMKBlankBlock;
import com.hbm_m.block.machines.rbmk.RBMKSteamInletBlock;
import com.hbm_m.block.machines.rbmk.RBMKSteamOutletBlock;
import com.hbm_m.block.machines.rbmk.RBMKLoaderBlock;
import com.hbm_m.block.machines.rbmk.RBMKAutoloaderBlock;
import com.hbm_m.block.machines.rbmk.RBMKCraneConsoleBlock;
import com.hbm_m.block.machines.rbmk.RBMKPanelBlock;
import com.hbm_m.block.machines.anvils.AnvilBlock;
import com.hbm_m.block.machines.anvils.AnvilTier;
import com.hbm_m.block.machines.crates.DeshCrateBlock;
import com.hbm_m.block.machines.crates.IronCrateBlock;
import com.hbm_m.block.machines.crates.SteelCrateBlock;
import com.hbm_m.block.machines.crates.TemplateCrateBlock;
import com.hbm_m.block.machines.crates.TungstenCrateBlock;
import com.hbm_m.block.nature.DepthOreBlock;
import com.hbm_m.block.nature.GeysirBlock;
import com.hbm_m.block.generic.BlockHazard;
import com.hbm_m.block.weapons.BarbedWireBlock;
import com.hbm_m.block.weapons.BarbedWireFireBlock;
import com.hbm_m.block.weapons.BarbedWirePoisonBlock;
import com.hbm_m.block.weapons.BarbedWireRadBlock;
import com.hbm_m.block.weapons.BarbedWireWitherBlock;
import com.hbm_m.block.weapons.FallingSellafit;
import com.hbm_m.item.BlockAbsorberItem;
import com.hbm_m.item.ModItems;
import com.hbm_m.item.fekal_electric.MachineBatteryBlockItem;
import com.hbm_m.item.tags_and_tiers.ModIngots;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.platform.PlatformHooks;

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
            () -> new GeigerCounterBlock(BlockProps.copy(Blocks.IRON_BLOCK).noOcclusion()));

    public static final RegistrySupplier<Block> DECON = registerBlock("decon",
            () -> new BlockDecon(BlockProps.copy(Blocks.IRON_BLOCK)
                    .strength(5.0F, 10.0F)
                    .requiresCorrectToolForDrops()));

    /** Порт {@code rad_absorber} ({@link com.hbm.blocks.generic.BlockAbsorber}). */
    public static final RegistrySupplier<Block> RAD_ABSORBER = registerRadAbsorberBlock("rad_absorber",
            () -> new BlockAbsorber(BlockProps.copy(Blocks.IRON_BLOCK)
                    .strength(5.0F, 10.0F)
                    .requiresCorrectToolForDrops()));

    private static final BlockBehaviour.Properties TABLE_PROPERTIES =
            BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(5.0F, 6.0F).sound(SoundType.METAL).requiresCorrectToolForDrops();
    private static final BlockBehaviour.Properties ANVIL_PROPERTIES =
            BlockProps.copy(Blocks.ANVIL).sound(SoundType.ANVIL).noOcclusion();

    // Стандартные свойства для блоков слитков

    public static final List<RegistrySupplier<Block>> BATTERY_BLOCKS = new ArrayList<>();

    // Вспомогательный метод для регистрации батареек
    private static RegistrySupplier<Block> registerBattery(String name, long capacity) {
        // 1. Регистрируем БЛОК
        RegistrySupplier<Block> batteryBlock = BLOCKS.register(name,
                () -> new MachineBatteryBlock(BlockBehaviour.Properties.of().strength(5.0f).requiresCorrectToolForDrops(), capacity));

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
            BlockProps.copy(Blocks.IRON_BLOCK).strength(3.0F, 6.0F).sound(SoundType.METAL).requiresCorrectToolForDrops();

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
            "schraranium", "schrabidate", "solinium",
            "boron", "tcalloy", "cdalloy", "cadmium"
    );

    // 2. КАРТА БЛОКОВ
    public static final Map<ModIngots, RegistrySupplier<Block>> INGOT_BLOCKS = new EnumMap<>(ModIngots.class);

    /**
     * Слитковые блоки с {@code ExtDisplayEffect.RADFOG} в GIT ({@code BlockHazard#setDisplayEffect}, ModBlocks ~1328–1342).
     * Только для них {@link BlockHazard} получает {@code RADFOG} (частицы townaura).
     */
    private static final Set<String> RADFOG_INGOT_BLOCKS = Set.of(
            "u233", "u235", "neptunium", "plutonium", "pu238", "pu239", "pu240",
            "mox_fuel", "plutonium_fuel");

    /** GIT: {@code ExtDisplayEffect.SCHRAB} на block_schrabidium, block_schraranium, block_schrabidate, block_solinium, block_schrabidium_fuel. */
    private static final Set<String> SCHRABFOG_INGOT_BLOCKS = Set.of(
            "schrabidium", "schraranium", "schrabidate", "solinium", "schrabidium_fuel");

    /** GIT: RADFOG на block_u233, block_u235, block_neptunium, block_plutonium, block_pu*, block_mox_fuel, block_plutonium_fuel. */
    public static boolean hasRadFogParticles(ModIngots ingot) {
        return RADFOG_INGOT_BLOCKS.contains(ingot.getName());
    }

    public static boolean hasSchrabFogParticles(ModIngots ingot) {
        return SCHRABFOG_INGOT_BLOCKS.contains(ingot.getName());
    }

    // 3. АВТОМАТИЧЕСКАЯ РЕГИСТРАЦИЯ
    static {
        for (ModIngots ingot : ModIngots.values()) {
            String name = ingot.getName();

            // Проверяем, есть ли этот слиток в "белом списке"
            if (ENABLED_INGOT_BLOCKS.contains(name)) {

                String blockName = "block_" + name;

                RegistrySupplier<Block> registeredBlock;

                // Display particles: RADFOG / SCHRAB (1.7.10 BlockHazard#setDisplayEffect, ModBlocks ~1326-1373).
                // Все слитковые блоки — это BlockHazard; per-tick эмиттер чанковой радиации (hazard × 0.1/сек)
                // запускается автоматически через scheduled-tick. Различаются только визуальные частицы.
                registeredBlock = registerBlock(blockName,
                        () -> {
                            BlockHazard block = new BlockHazard(INGOT_BLOCK_PROPERTIES).makeBeaconable();
                            if (hasRadFogParticles(ingot)) {
                                block.setDisplayEffect(BlockHazard.ExtDisplayEffect.RADFOG);
                            } else if (hasSchrabFogParticles(ingot)) {
                                block.setDisplayEffect(BlockHazard.ExtDisplayEffect.SCHRAB);
                            }
                            return block;
                        });

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

    public static boolean hasIngotBlock(ModIngots ingot) {
        return INGOT_BLOCKS.containsKey(ingot);
    }

    public static final RegistrySupplier<Block> URANIUM_BLOCK = getIngotBlock(ModIngots.URANIUM);
    public static final RegistrySupplier<Block> PLUTONIUM_BLOCK = getIngotBlock(ModIngots.PLUTONIUM);
    public static final RegistrySupplier<Block> PLUTONIUM_FUEL_BLOCK = getIngotBlock(ModIngots.PLUTONIUM_FUEL);

    public static final RegistrySupplier<Block> POLONIUM210_BLOCK = registerBlock("polonium210_block",
            () -> new BlockHazard(INGOT_BLOCK_PROPERTIES));

    public static final RegistrySupplier<Block> WASTE_GRASS = registerBlock("waste_grass",
            () -> new Block(BlockProps.copy(Blocks.DIRT).sound(SoundType.GRAVEL)));

    public static final RegistrySupplier<Block> WASTE_LEAVES = registerBlock("waste_leaves",
            () -> new com.hbm_m.block.generic.WasteLeaves(BlockProps.copy(Blocks.OAK_LEAVES).noOcclusion()));

    /** Шлак (оригинал {@code ModBlocks.block_slag}) — оболочка volatile creeper и др. */
    public static final RegistrySupplier<Block> BLOCK_SLAG = registerBlock("block_slag",
            () -> new BlockSlag(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.STONE)
                    .sound(SoundType.STONE)
                    .strength(2.0F)
                    .requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> WIRE_COATED = registerBlock("wire_coated",
            () -> new WireBlock(BlockProps.copy(Blocks.IRON_BLOCK).noOcclusion()));


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
            () -> new ConverterBlock(BlockProps.copy(Blocks.IRON_BLOCK).noOcclusion()));

    public static final RegistrySupplier<Block> EMP = registerBlock("emp",
            () -> new Block(BlockProps.copy(Blocks.IRON_BLOCK)
                    .strength(3.0F, 6.0F)
                    .sound(SoundType.METAL)
                    .requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> BLAST_FURNACE = registerBlock("blast_furnace",
            () -> new BlastFurnaceBlock(BlockProps.copy(Blocks.IRON_BLOCK)
                    .strength(4.0f, 4.0f)
                    .sound(SoundType.STONE)
                    .lightLevel(state -> state.getValue(BlastFurnaceBlock.LIT) ? 15 : 0)));

    public static final RegistrySupplier<Block> BLAST_FURNACE_EXTENSION = registerBlock("blast_furnace_extension",
            () -> new BlastFurnaceExtensionBlock(BlockProps.copy(Blocks.IRON_BLOCK)
                    .strength(3.0f, 4.0f)
                    .sound(SoundType.METAL)
                    .noOcclusion()));
					
	// МУЛЬТИБЛОКИ ----------------------------------------------------------------------------------------------------
	
    public static final RegistrySupplier<Block> PRESS = registerBlockWithoutItem("press",
            () -> new MachinePressBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(4.0f, 4.0f).sound(SoundType.METAL).noOcclusion()));

    public static final RegistrySupplier<Block> WOOD_BURNER = registerBlockWithoutItem("wood_burner",
            () -> new MachineWoodBurnerBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(4.0f, 4.0f).sound(SoundType.METAL).noOcclusion()));

    public static final RegistrySupplier<Block> ARMOR_TABLE = registerBlock("armor_table",
            () -> new ArmorTableBlock(TABLE_PROPERTIES));

    public static final RegistrySupplier<Block> SHREDDER = registerBlock("shredder",
            () -> new MachineShredderBlock(BlockProps.copy(Blocks.IRON_BLOCK)));

    public static final RegistrySupplier<Block> SWITCH = registerBlock("switch",
            () -> new SwitchBlock(BlockProps.copy(Blocks.IRON_BLOCK)));

    public static final RegistrySupplier<Block> MACHINE_ASSEMBLER = registerBlockWithoutItem("machine_assembler",
            () -> new MachineAssemblerBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(2.0f).noOcclusion()));

    public static final RegistrySupplier<Block> ADVANCED_ASSEMBLY_MACHINE = registerBlockWithoutItem("advanced_assembly_machine",
            () -> new MachineAdvancedAssemblerBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(2.0f).noOcclusion()));

    public static final RegistrySupplier<Block> HYDRAULIC_FRACKINING_TOWER = registerBlockWithoutItem("hydraulic_frackining_tower",
            () -> new MachineFrackingTowerBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(4.0f).noOcclusion().isSuffocating((state, world, pos) -> false)));

    public static final RegistrySupplier<Block> COOLING_TOWER = registerBlockWithoutItem("cooling_tower",
            () -> new MachineTowerLargeBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(4.0f).noOcclusion().isSuffocating((state, world, pos) -> false)));

    public static final RegistrySupplier<Block> TOWER_SMALL = registerBlockWithoutItem("tower_small",
            () -> new MachineTowerSmallBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(4.0f).noOcclusion().isSuffocating((state, world, pos) -> false)));

    public static final RegistrySupplier<Block> CYCLOTRON = registerBlockWithoutItem("cyclotron",
            () -> new MachineCyclotronBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(4.0f).noOcclusion().isSuffocating((state, world, pos) -> false)));

    public static final RegistrySupplier<Block> ZIRNOX = registerBlockWithoutItem("zirnox",
            () -> new MachineZirnoxBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(4.0f).noOcclusion().isSuffocating((state, world, pos) -> false)));

    public static final RegistrySupplier<Block> ZIRNOX_DESTROYED = registerBlockWithoutItem("zirnox_destroyed",
            () -> new MachineZirnoxDestroyedBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(100.0f, 800.0f).noOcclusion().isSuffocating((state, world, pos) -> false)));

    public static final RegistrySupplier<Block> ZIRNOX_DEB_BLANK     = registerBlockWithoutItem("zirnox_deb_blank",     () -> new Block(BlockBehaviour.Properties.of().strength(-1F, Float.MAX_VALUE).noOcclusion()));
    public static final RegistrySupplier<Block> ZIRNOX_DEB_ELEMENT   = registerBlockWithoutItem("zirnox_deb_element",   () -> new Block(BlockBehaviour.Properties.of().strength(-1F, Float.MAX_VALUE).noOcclusion()));
    public static final RegistrySupplier<Block> ZIRNOX_DEB_SHRAPNEL  = registerBlockWithoutItem("zirnox_deb_shrapnel",  () -> new Block(BlockBehaviour.Properties.of().strength(-1F, Float.MAX_VALUE).noOcclusion()));
    public static final RegistrySupplier<Block> ZIRNOX_DEB_CONCRETE  = registerBlockWithoutItem("zirnox_deb_concrete",  () -> new Block(BlockBehaviour.Properties.of().strength(-1F, Float.MAX_VALUE).noOcclusion()));
    public static final RegistrySupplier<Block> ZIRNOX_DEB_EXCHANGER = registerBlockWithoutItem("zirnox_deb_exchanger", () -> new Block(BlockBehaviour.Properties.of().strength(-1F, Float.MAX_VALUE).noOcclusion()));

    public static final RegistrySupplier<Block> ARC_WELDER = registerBlockWithoutItem("arc_welder",
            () -> new MachineArcWelderBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(4.0f).noOcclusion().isSuffocating((state, world, pos) -> false)));

    public static final RegistrySupplier<Block> SOLDERING_STATION = registerBlockWithoutItem("soldering_station",
            () -> new MachineSolderingStationBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(4.0f).noOcclusion().isSuffocating((state, world, pos) -> false)));

    public static final RegistrySupplier<Block> MIXER = registerBlockWithoutItem("mixer",
            () -> new MachineMixerBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(4.0f).noOcclusion().isSuffocating((state, world, pos) -> false)));

    public static final RegistrySupplier<Block> DERRICK = registerBlockWithoutItem("derrick",
            () -> new MachineDerrickBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(4.0f).noOcclusion().isSuffocating((state, world, pos) -> false)));


    public static final RegistrySupplier<Block> RBMK_CONSOLE = registerBlockWithoutItem("rbmk_console",
            () -> new MachineRbmkConsoleBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(4.0f).noOcclusion().isSuffocating((state, world, pos) -> false)));

    public static final RegistrySupplier<Block> FLARE_STACK = registerBlockWithoutItem("flare_stack",
            () -> new MachineFlareStackBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(4.0f).noOcclusion().isSuffocating((state, world, pos) -> false)));

    public static final RegistrySupplier<Block> PUMPJACK = registerBlockWithoutItem("pumpjack",
            () -> new MachinePumpjackBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(4.0f).noOcclusion().isSuffocating((state, world, pos) -> false)));

    public static final RegistrySupplier<Block> RADAR = registerBlockWithoutItem("radar",
            () -> new MachineRadarBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(4.0f).noOcclusion().isSuffocating((state, world, pos) -> false)));

    public static final RegistrySupplier<Block> LARGE_RADAR = registerBlockWithoutItem("large_radar",
            () -> new MachineLargeRadarBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(4.0f).noOcclusion().isSuffocating((state, world, pos) -> false)));

    public static final RegistrySupplier<Block> RADAR_SCREEN = registerBlockWithoutItem("radar_screen",
            () -> new MachineRadarScreenBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(4.0f).noOcclusion().isSuffocating((state, world, pos) -> false)));

    public static final RegistrySupplier<Block> CRACKING_TOWER = registerBlockWithoutItem("cracking_tower",
            () -> new MachineCrackingTowerBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(4.0f).noOcclusion().isSuffocating((state, world, pos) -> false)));

    /** MVP-Turrets (Einzelblock, kein Multiblock) - eine Java-Klasse fuer alle Varianten, siehe {@link com.hbm_m.block.machines.TurretBlock}. */
    public static final RegistrySupplier<Block> TURRET_SENTRY = registerBlock("turret_sentry",
            () -> new com.hbm_m.block.machines.TurretBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(5.0f, 10.0f).noOcclusion(),
                    () -> com.hbm_m.blockentity.ModBlockEntities.TURRET_SENTRY_BE.get()));
    public static final RegistrySupplier<Block> TURRET_CHEKHOV = registerBlock("turret_chekhov",
            () -> new com.hbm_m.block.machines.TurretBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(5.0f, 10.0f).noOcclusion(),
                    () -> com.hbm_m.blockentity.ModBlockEntities.TURRET_CHEKHOV_BE.get()));
    public static final RegistrySupplier<Block> TURRET_FRIENDLY = registerBlock("turret_friendly",
            () -> new com.hbm_m.block.machines.TurretBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(5.0f, 10.0f).noOcclusion(),
                    () -> com.hbm_m.blockentity.ModBlockEntities.TURRET_FRIENDLY_BE.get()));
    public static final RegistrySupplier<Block> TURRET_JEREMY = registerBlock("turret_jeremy",
            () -> new com.hbm_m.block.machines.TurretBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(5.0f, 10.0f).noOcclusion(),
                    () -> com.hbm_m.blockentity.ModBlockEntities.TURRET_JEREMY_BE.get()));
    public static final RegistrySupplier<Block> TURRET_TAUON = registerBlock("turret_tauon",
            () -> new com.hbm_m.block.machines.TurretBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(5.0f, 10.0f).noOcclusion(),
                    () -> com.hbm_m.blockentity.ModBlockEntities.TURRET_TAUON_BE.get()));
    public static final RegistrySupplier<Block> TURRET_RICHARD = registerBlock("turret_richard",
            () -> new com.hbm_m.block.machines.TurretBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(5.0f, 10.0f).noOcclusion(),
                    () -> com.hbm_m.blockentity.ModBlockEntities.TURRET_RICHARD_BE.get()));
    public static final RegistrySupplier<Block> TURRET_HOWARD = registerBlock("turret_howard",
            () -> new com.hbm_m.block.machines.TurretBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(6.0f, 12.0f).noOcclusion(),
                    () -> com.hbm_m.blockentity.ModBlockEntities.TURRET_HOWARD_BE.get()));
    public static final RegistrySupplier<Block> TURRET_MAXWELL = registerBlock("turret_maxwell",
            () -> new com.hbm_m.block.machines.TurretBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(6.0f, 12.0f).noOcclusion(),
                    () -> com.hbm_m.blockentity.ModBlockEntities.TURRET_MAXWELL_BE.get()));
    public static final RegistrySupplier<Block> TURRET_FRITZ = registerBlock("turret_fritz",
            () -> new com.hbm_m.block.machines.TurretBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(6.0f, 12.0f).noOcclusion(),
                    () -> com.hbm_m.blockentity.ModBlockEntities.TURRET_FRITZ_BE.get()));
    public static final RegistrySupplier<Block> TURRET_ARTY = registerBlock("turret_arty",
            () -> new com.hbm_m.block.machines.TurretBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(8.0f, 20.0f).noOcclusion(),
                    () -> com.hbm_m.blockentity.ModBlockEntities.TURRET_ARTY_BE.get()));
    public static final RegistrySupplier<Block> TURRET_HIMARS = registerBlock("turret_himars",
            () -> new com.hbm_m.block.machines.TurretBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(8.0f, 20.0f).noOcclusion(),
                    () -> com.hbm_m.blockentity.ModBlockEntities.TURRET_HIMARS_BE.get()));

    public static final RegistrySupplier<Block> FRACTION_TOWER = registerBlockWithoutItem("fraction_tower",
            () -> new MachineFractionTowerBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(4.0f).noOcclusion().isSuffocating((state, world, pos) -> false)));

    public static final RegistrySupplier<Block> MINING_DRILL = registerBlockWithoutItem("mining_drill",
            () -> new MachineMiningDrillBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(4.0f).noOcclusion().isSuffocating((state, world, pos) -> false)));

    public static final RegistrySupplier<Block> FEL = registerBlockWithoutItem("fel",
            () -> new MachineFelBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(4.0f).noOcclusion().isSuffocating((state, world, pos) -> false)));

    public static final RegistrySupplier<Block> SILEX = registerBlockWithoutItem("silex",
            () -> new MachineSilexBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(4.0f).noOcclusion().isSuffocating((state, world, pos) -> false)));

    public static final RegistrySupplier<Block> CRYSTALLIZER = registerBlockWithoutItem("crystallizer",
            () -> new MachineCrystallizerBlock(BlockProps.copy(Blocks.IRON_BLOCK).noOcclusion().isSuffocating((state, world, pos) -> false)));

    public static final RegistrySupplier<Block> BREEDER = registerBlockWithoutItem("breeder",
            () -> new MachineBreederBlock(BlockProps.copy(Blocks.IRON_BLOCK).noOcclusion().isSuffocating((state, world, pos) -> false)));

    public static final RegistrySupplier<Block> LARGE_PYLON = registerBlockWithoutItem("large_pylon",
            () -> new MachineLargePylonBlock(BlockProps.copy(Blocks.IRON_BLOCK).noOcclusion().isSuffocating((state, world, pos) -> false)));

    public static final RegistrySupplier<Block> CHEMICAL_PLANT = registerBlockWithoutItem("chemical_plant",
            () -> new MachineChemicalPlantBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(4.0f, 4.0f).sound(SoundType.METAL).noOcclusion().isSuffocating((state, world, pos) -> false)));

    public static final RegistrySupplier<Block> CRUCIBLE = registerBlock("crucible",
            () -> new MachineCrucibleBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(4.0f, 4.0f).sound(SoundType.METAL).noOcclusion().isSuffocating((state, world, pos) -> false)));

    public static final RegistrySupplier<Block> FOUNDRY_BASIN = registerBlock("foundry_basin",
            () -> new MachineFoundryBasinBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(4.0f, 4.0f).sound(SoundType.METAL).noOcclusion().isSuffocating((state, world, pos) -> false)));

    public static final RegistrySupplier<Block> FOUNDRY_CHANNEL = registerBlock("foundry_channel",
            () -> new MachineFoundryChannelBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(2.0f, 2.0f).sound(SoundType.METAL).noOcclusion().isSuffocating((state, world, pos) -> false)));

    public static final RegistrySupplier<Block> FOUNDRY_OUTLET = registerBlock("foundry_outlet",
            () -> new com.hbm_m.block.machines.MachineFoundryOutletBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(3.0f, 3.0f).sound(SoundType.METAL).noOcclusion().isSuffocating((state, world, pos) -> false)));

    // ─── Trophies ─────────────────────────────────────────────────────────────
    public static final RegistrySupplier<Block> SU47_TROPHY = registerBlock("su47_trophy",
            () -> new com.hbm_m.block.machines.SU47TrophyBlock(BlockBehaviour.Properties.of().strength(2f).noOcclusion().isSuffocating((state, world, pos) -> false)));

    public static final RegistrySupplier<Block> JAS39_TROPHY = registerBlock("jas39_trophy",
            () -> new com.hbm_m.block.machines.JAS39TrophyBlock(BlockBehaviour.Properties.of().strength(2f).noOcclusion().isSuffocating((state, world, pos) -> false)));

    public static final RegistrySupplier<Block> GAS_CENTRIFUGE = registerBlockWithoutItem("gas_centrifuge",
            () -> new MachineGasCentrifugeBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(4.0f, 4.0f).sound(SoundType.METAL).noOcclusion().isSuffocating((state, world, pos) -> false)));

    public static final RegistrySupplier<Block> CENTRIFUGE = registerBlockWithoutItem("centrifuge",
            () -> new MachineCentrifugeBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(4.0f, 4.0f).sound(SoundType.METAL).isSuffocating((state, world, pos) -> false)));

    public static final RegistrySupplier<Block> STEAM_CONDENSER = registerBlock("steam_condenser",
            () -> new MachineSteamCondenserBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(3.0f, 4.0f).sound(SoundType.METAL)));

    public static final RegistrySupplier<Block> UNIVERSAL_MACHINE_PART = registerBlockWithoutItem("universal_machine_part",
            //? if < 1.21.1 {
            () -> new UniversalMachinePartBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(5.0f).noOcclusion().isSuffocating((state, world, pos) -> false).noParticlesOnBreak()));
            //?} else {
            /*() -> new UniversalMachinePartBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(5.0f).noOcclusion().isSuffocating((state, world, pos) -> false)));
            *///?}

	public static final RegistrySupplier<Block> FLUID_TANK = registerBlockWithoutItem("fluid_tank",
            () -> new MachineFluidTankBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(4.0f).requiresCorrectToolForDrops().noOcclusion().isSuffocating((state, world, pos) -> false)));

	public static final RegistrySupplier<Block> LAUNCH_PAD = registerBlockWithoutItem("launch_pad",
            () -> new LaunchPadBlock(BlockProps.copy(Blocks.IRON_BLOCK).noOcclusion()));

    public static final RegistrySupplier<Block> LAUNCH_PAD_RUSTED = registerBlockWithoutItem("launch_pad_rusted",
            () -> new LaunchPadRustedBlock(BlockProps.copy(Blocks.IRON_BLOCK).noOcclusion()));

    public static final RegistrySupplier<Block> MACHINE_BATTERY_SOCKET = registerBlockWithoutItem("machine_battery_socket",
            () -> new MachineBatterySocketBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(5.0f).requiresCorrectToolForDrops().noOcclusion().isSuffocating((state, world, pos) -> false)));

    public static final RegistrySupplier<Block> INDUSTRIAL_BOILER = registerBlockWithoutItem("industrial_boiler",
            () -> new MachineIndustrialBoilerBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(4.0f, 4.0f).sound(SoundType.METAL).noOcclusion().isSuffocating((state, world, pos) -> false)));

    public static final RegistrySupplier<Block> SOLAR_BOILER = registerBlockWithoutItem("solar_boiler",
            () -> new MachineSolarBoilerBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(3.0f, 3.0f).sound(SoundType.METAL).noOcclusion().isSuffocating((state, world, pos) -> false)));

    public static final RegistrySupplier<Block> SOLAR_MIRRORS = registerBlockWithoutItem("solar_mirrors",
            () -> new MachineSolarMirrorsBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(2.0f, 2.0f).sound(SoundType.METAL).noOcclusion().isSuffocating((state, world, pos) -> false)));

    public static final RegistrySupplier<Block> WATZ_POWERPLANT = registerBlockWithoutItem("watz_powerplant",
            () -> new MachineWatzPowerplantBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(5.0f, 5.0f).sound(SoundType.METAL).noOcclusion().isSuffocating((state, world, pos) -> false)));

    public static final RegistrySupplier<Block> HYDROTREATER = registerBlockWithoutItem("hydrotreater",
            () -> new MachineHydrotreaterBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(4.0f, 4.0f).sound(SoundType.METAL).noOcclusion().isSuffocating((state, world, pos) -> false)));

    public static final RegistrySupplier<Block> CATALYTIC_REFORMER = registerBlockWithoutItem("catalytic_reformer",
            () -> new MachineCatalyticReformerBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(4.0f, 4.0f).sound(SoundType.METAL).noOcclusion().isSuffocating((state, world, pos) -> false)));

    public static final RegistrySupplier<Block> DEUTERIUM_TOWER = registerBlockWithoutItem("deuterium_tower",
            () -> new MachineDeuteriumTowerBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(4.0f, 4.0f).sound(SoundType.METAL).noOcclusion().isSuffocating((state, world, pos) -> false)));

    public static final RegistrySupplier<Block> CHEMICAL_FACTORY = registerBlockWithoutItem("chemical_factory",
            () -> new MachineChemicalFactoryBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(5.0f, 5.0f).sound(SoundType.METAL).noOcclusion().isSuffocating((state, world, pos) -> false)));

    public static final RegistrySupplier<Block> STEAM_TURBINE = registerBlockWithoutItem("steam_turbine",
            () -> new MachineSteamTurbineBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(4.0f, 4.0f).sound(SoundType.METAL)));

    public static final RegistrySupplier<Block> LIQUEFACTOR = registerBlockWithoutItem("liquefactor",
            () -> new MachineLiquefactorBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(4.0f, 4.0f).sound(SoundType.METAL).noOcclusion().isSuffocating((state, world, pos) -> false)));

    public static final RegistrySupplier<Block> CORE_EMITTER = registerBlockWithoutItem("core_emitter",
            () -> new MachineCoreEmitterBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(4.0f, 4.0f).sound(SoundType.METAL).noOcclusion().isSuffocating((state, world, pos) -> false)));

    public static final RegistrySupplier<Block> CORE_INJECTOR = registerBlockWithoutItem("core_injector",
            () -> new MachineCoreInjectorBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(4.0f, 4.0f).sound(SoundType.METAL).noOcclusion().isSuffocating((state, world, pos) -> false)));

    public static final RegistrySupplier<Block> CORE_RECEIVER = registerBlockWithoutItem("core_receiver",
            () -> new MachineCoreReceiverBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(4.0f, 4.0f).sound(SoundType.METAL).noOcclusion().isSuffocating((state, world, pos) -> false)));

    public static final RegistrySupplier<Block> VACUUM_DISTILL = registerBlockWithoutItem("vacuum_distill",
            () -> new MachineVacuumDistillBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(4.0f, 4.0f).sound(SoundType.METAL).noOcclusion().isSuffocating((state, world, pos) -> false)));

    public static final RegistrySupplier<Block> TURBOFAN = registerBlockWithoutItem("turbofan",
            () -> new MachineTurbofanBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(4.0f, 4.0f).sound(SoundType.METAL).noOcclusion().isSuffocating((state, world, pos) -> false)));

    public static final RegistrySupplier<Block> REFINERY = registerBlockWithoutItem("refinery",
            () -> new MachineRefineryBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(4.0f, 4.0f).sound(SoundType.METAL).noOcclusion().isSuffocating((state, world, pos) -> false)));

    public static final RegistrySupplier<Block> INDUSTRIAL_TURBINE = registerBlockWithoutItem("industrial_turbine",
            () -> new MachineIndustrialTurbineBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(4.0f, 4.0f).sound(SoundType.METAL).noOcclusion().isSuffocating((state, world, pos) -> false)));

    public static final RegistrySupplier<Block> TURBINE = registerBlockWithoutItem("turbine",
            () -> new MachineTurbineBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(4.0f, 4.0f).sound(SoundType.METAL).noOcclusion().isSuffocating((state, world, pos) -> false)));

    public static final RegistrySupplier<Block> SUBSTATION = registerBlockWithoutItem("substation",
            () -> new MachineSubstationBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(4.0f, 4.0f).sound(SoundType.METAL).noOcclusion().isSuffocating((state, world, pos) -> false)));

    public static final RegistrySupplier<Block> FLUID_DUCT = registerBlockWithoutItem("fluid_duct",
            () -> new FluidDuctBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(2.0f).sound(SoundType.METAL).noOcclusion(),
                    com.hbm_m.block.machines.PipeStyle.NEO));
    public static final RegistrySupplier<Block> FLUID_DUCT_COLORED = registerBlockWithoutItem("fluid_duct_colored",
            () -> new FluidDuctBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(2.0f).sound(SoundType.METAL).noOcclusion(),
                    com.hbm_m.block.machines.PipeStyle.COLORED));
    public static final RegistrySupplier<Block> FLUID_DUCT_SILVER = registerBlockWithoutItem("fluid_duct_silver",
            () -> new FluidDuctBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(2.0f).sound(SoundType.METAL).noOcclusion(),
                    com.hbm_m.block.machines.PipeStyle.SILVER));
    public static final RegistrySupplier<Block> OIL_PIPE = registerBlockWithoutItem("oil_pipe",
            () -> new FluidDuctBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(2.0f).sound(SoundType.METAL).noOcclusion(),
                    com.hbm_m.block.machines.PipeStyle.NEO));

    public static final RegistrySupplier<Block> FLUID_VALVE = registerBlockWithoutItem("fluid_valve",
            () -> new com.hbm_m.block.machines.FluidValveBlock(
                    BlockProps.copy(Blocks.IRON_BLOCK).strength(2.0f).sound(SoundType.METAL).noOcclusion()));

    public static final RegistrySupplier<Block> FLUID_PUMP = registerBlockWithoutItem("fluid_pump",
            () -> new com.hbm_m.block.machines.FluidPumpBlock(
                    BlockProps.copy(Blocks.IRON_BLOCK).strength(3.0f).sound(SoundType.METAL).noOcclusion()));

    public static final RegistrySupplier<Block> FLUID_EXHAUST = registerBlockWithoutItem("fluid_exhaust",
            () -> new com.hbm_m.block.machines.FluidExhaustBlock(
                    BlockProps.copy(Blocks.IRON_BLOCK).strength(2.0f).sound(SoundType.METAL).noOcclusion()));

    public static final RegistrySupplier<Block> HEATING_OVEN = registerBlock("heating_oven",
            () -> new HeatingOvenBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(4.0f, 4.0f).sound(SoundType.METAL).noOcclusion()));


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
            () -> new TransitionSealBlock(
                    BlockBehaviour.Properties.of()
                            .strength(10.0F, 1000.0F)
                            .requiresCorrectToolForDrops()
                            .sound(SoundType.METAL)
                            .noOcclusion()
                            .isViewBlocking((state, level, pos) -> false)));

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

    public static final RegistrySupplier<Block> CARGO_DOOR = registerBlockWithoutItem("cargo_door",
            () -> new DoorBlock(
                    BlockBehaviour.Properties.of()
                            .strength(10.0F, 1000.0F)
                            .requiresCorrectToolForDrops()
                            .sound(SoundType.METAL)
                            .noOcclusion()
                            .isViewBlocking((state, level, pos) -> false),
                    "cargo_door"
            ));


    //---------------------------<БЛОКИ>-------------------------------------
    public static final RegistrySupplier<Block> REINFORCED_STONE = registerBlock("reinforced_stone",
            () -> new Block(BlockProps.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> REINFORCED_GLASS = registerBlock("reinforced_glass",
            () -> com.hbm_m.platform.PlatformHooks.createGlassBlock(BlockProps.copy(Blocks.GLASS).strength(4.0F, 12.0F)));

    public static final RegistrySupplier<Block> MACHINE_SIREN = registerBlock("machine_siren",
            () -> new com.hbm_m.block.machines.MachineSirenBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(5.0F, 10.0F).noOcclusion()));

    public static final RegistrySupplier<Block> BROADCASTER = registerBlock("broadcaster",
            () -> new Block(BlockProps.copy(Blocks.IRON_BLOCK).strength(5.0F, 10.0F).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> CRATE = registerBlock("crate",
            () -> new Block(BlockProps.copy(Blocks.OAK_WOOD).strength(1.0f, 1.0f).requiresCorrectToolForDrops()));
    public static final RegistrySupplier<Block> CRATE_LEAD = registerBlock("crate_lead",
            () -> new Block(BlockProps.copy(Blocks.OAK_WOOD).strength(1.0f, 1.0f).requiresCorrectToolForDrops()));
    public static final RegistrySupplier<Block> CRATE_METAL = registerBlock("crate_metal",
            () -> new Block(BlockProps.copy(Blocks.OAK_WOOD).strength(1.0f, 1.0f).requiresCorrectToolForDrops()));
    public static final RegistrySupplier<Block> CRATE_WEAPON = registerBlock("crate_weapon",
            () -> new Block(BlockProps.copy(Blocks.OAK_WOOD).strength(1.0f, 1.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> CONCRETE_HAZARD = registerBlock("concrete_hazard",
            () -> new Block(BlockProps.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));
    public static final RegistrySupplier<Block> CONCRETE_HAZARD_STAIRS = registerBlock("concrete_hazard_stairs",
            () -> new StairBlock(ModBlocks.CONCRETE_HAZARD.get().defaultBlockState(),
                    BlockProps.copy(Blocks.IRON_BLOCK).sound(SoundType.STONE)));
    public static final RegistrySupplier<Block> CONCRETE_HAZARD_SLAB = registerBlock("concrete_hazard_slab",
            () -> new SlabBlock(BlockProps.copy(Blocks.IRON_BLOCK).sound(SoundType.STONE)));

    public static final RegistrySupplier<Block> BRICK_CONCRETE = registerBlock("brick_concrete",
            () -> new Block(BlockProps.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));
    public static final RegistrySupplier<Block> BRICK_CONCRETE_STAIRS = registerBlock("brick_concrete_stairs",
            () -> new StairBlock(ModBlocks.BRICK_CONCRETE.get().defaultBlockState(),
                    BlockProps.copy(Blocks.IRON_BLOCK).sound(SoundType.STONE)));
    public static final RegistrySupplier<Block> BRICK_CONCRETE_SLAB = registerBlock("brick_concrete_slab",
            () -> new SlabBlock(BlockProps.copy(Blocks.IRON_BLOCK).sound(SoundType.STONE)));

    public static final RegistrySupplier<Block> CONCRETE_MOSSY = registerBlock("concrete_mossy",
            () -> new Block(BlockProps.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));
    public static final RegistrySupplier<Block> CONCRETE_MOSSY_STAIRS = registerBlock("concrete_mossy_stairs",
            () -> new StairBlock(ModBlocks.CONCRETE_MOSSY.get().defaultBlockState(),
                    BlockProps.copy(Blocks.IRON_BLOCK).sound(SoundType.STONE)));
    public static final RegistrySupplier<Block> CONCRETE_MOSSY_SLAB = registerBlock("concrete_mossy_slab",
            () -> new SlabBlock(BlockProps.copy(Blocks.IRON_BLOCK).sound(SoundType.STONE)));

    public static final RegistrySupplier<Block> CONCRETE  = registerBlock("concrete",
            () -> new Block(BlockProps.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));
    public static final RegistrySupplier<Block> CONCRETE_STAIRS = registerBlock("concrete_stairs",
            () -> new StairBlock(ModBlocks.CONCRETE.get().defaultBlockState(),
                    BlockProps.copy(Blocks.IRON_BLOCK).sound(SoundType.STONE)));
    public static final RegistrySupplier<Block> CONCRETE_SLAB = registerBlock("concrete_slab",
            () -> new SlabBlock(BlockProps.copy(Blocks.IRON_BLOCK).sound(SoundType.STONE)));

    public static final RegistrySupplier<Block> CONCRETE_CRACKED  = registerBlock("concrete_cracked",
            () -> new Block(BlockProps.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));
    public static final RegistrySupplier<Block> CONCRETE_CRACKED_STAIRS = registerBlock("concrete_cracked_stairs",
            () -> new StairBlock(ModBlocks.CONCRETE_CRACKED.get().defaultBlockState(),
                    BlockProps.copy(Blocks.IRON_BLOCK).sound(SoundType.STONE)));
    public static final RegistrySupplier<Block> CONCRETE_CRACKED_SLAB = registerBlock("concrete_cracked_slab",
            () -> new SlabBlock(BlockProps.copy(Blocks.IRON_BLOCK).sound(SoundType.STONE)));

    public static final RegistrySupplier<Block> CONCRETE_VENT  = registerBlock("concrete_vent",
            () -> new Block(BlockProps.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));


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

    // Ковёр fallout (1.7.10: ModBlocks.fallout / BlockFallout)
    public static final RegistrySupplier<Block> NUCLEAR_FALLOUT = registerBlock("nuclear_fallout",
            () -> new com.hbm_m.block.generic.BlockFallout(BlockProps.copy(Blocks.SAND)
                    .strength(0.1F)
                    .sound(SoundType.GRAVEL)
                    .noOcclusion()));

    // Блок fallout (1.7.10: ModBlocks.block_fallout / BlockHazardFalling)
    public static final RegistrySupplier<Block> BLOCK_FALLOUT = registerBlock("block_fallout",
            () -> new com.hbm_m.block.generic.BlockHazardFalling(BlockProps.copy(Blocks.GRAVEL)
                    .strength(0.2F)
                    .sound(SoundType.GRAVEL)));

    public static final RegistrySupplier<Block> DOOR_BUNKER = registerBlock("door_bunker",
            () -> PlatformHooks.createDoorBlock(BlockProps.copy(Blocks.NETHERITE_BLOCK).sound(SoundType.NETHERITE_BLOCK).noOcclusion(), BlockSetType.STONE));

    public static final RegistrySupplier<Block> DOOR_OFFICE = registerBlock("door_office",
            () -> PlatformHooks.createDoorBlock(BlockProps.copy(Blocks.CHERRY_WOOD).sound(SoundType.CHERRY_WOOD).noOcclusion(), BlockSetType.CHERRY));

    public static final RegistrySupplier<Block> METAL_DOOR = registerBlock("metal_door",
            () -> PlatformHooks.createDoorBlock(BlockProps.copy(Blocks.CHAIN).sound(SoundType.CHAIN).noOcclusion(), BlockSetType.BIRCH));


    // ============ ТЕХНИЧЕСКИЕ И ДЕКОРАТИВНЫЕ БЛОКИ ============

    public static final RegistrySupplier<Block> DORNIER = registerBlock("dornier",
            () -> new BarrelBlock(BlockProps.copy(Blocks.STONE).strength(1.5F, 6.0F).noOcclusion()));

    public static final RegistrySupplier<Block> ORE_OIL = registerBlock("ore_oil",
            () -> new Block(BlockProps.copy(Blocks.STONE).strength(3.0F, 3.0F).noOcclusion()));

    public static final RegistrySupplier<Block> ORE_OIL_EMPTY = registerBlock("ore_oil_empty",
            () -> new Block(BlockProps.copy(Blocks.STONE).strength(3.0F, 3.0F).noOcclusion()));

    public static final RegistrySupplier<Block> BEDROCK_OIL = registerBlock("bedrock_oil",
            () -> new Block(BlockProps.copy(Blocks.BEDROCK).noOcclusion()));

    public static final RegistrySupplier<Block> ORE_BEDROCK_OIL = registerBlock("ore_bedrock_oil",
            () -> new Block(BlockProps.copy(Blocks.BEDROCK)));

    /** Mineralisches Bedrock-Erz (Mining-Drill-Ziel), siehe {@link com.hbm_m.block.nature.OreBedrockBlock}. */
    public static final RegistrySupplier<Block> ORE_BEDROCK = registerBlock("ore_bedrock_mineral",
            () -> new com.hbm_m.block.nature.OreBedrockBlock(BlockProps.copy(Blocks.BEDROCK)));

    public static final RegistrySupplier<Block> DEPTH_STONE = registerBlock("depth_stone",
            () -> new DepthOreBlock(BlockProps.copy(Blocks.DEEPSLATE).strength(4.5F, 6.0F).noOcclusion()));
    public static final RegistrySupplier<Block> DEPTH_BORAX = registerBlock("depth_borax",
            () -> new DepthOreBlock(BlockProps.copy(Blocks.DEEPSLATE).strength(4.5F, 6.0F).noOcclusion()));
    public static final RegistrySupplier<Block> DEPTH_CINNABAR = registerBlock("depth_cinnabar",
            () -> new DepthOreBlock(BlockProps.copy(Blocks.DEEPSLATE).strength(4.5F, 6.0F).noOcclusion()));
    public static final RegistrySupplier<Block> DEPTH_IRON = registerBlock("depth_iron",
            () -> new DepthOreBlock(BlockProps.copy(Blocks.DEEPSLATE).strength(4.5F, 6.0F).noOcclusion()));
    public static final RegistrySupplier<Block> DEPTH_TUNGSTEN = registerBlock("depth_tungsten",
            () -> new DepthOreBlock(BlockProps.copy(Blocks.DEEPSLATE).strength(4.5F, 6.0F).noOcclusion()));
    public static final RegistrySupplier<Block> DEPTH_TITANIUM = registerBlock("depth_titanium",
            () -> new DepthOreBlock(BlockProps.copy(Blocks.DEEPSLATE).strength(4.5F, 6.0F).noOcclusion()));
    public static final RegistrySupplier<Block> DEPTH_ZIRCONIUM = registerBlock("depth_zirconium",
            () -> new DepthOreBlock(BlockProps.copy(Blocks.DEEPSLATE).strength(4.5F, 6.0F).noOcclusion()));

    public static final RegistrySupplier<Block> FILE_CABINET = registerBlock("file_cabinet",
            () -> new CrtBlock(BlockProps.copy(Blocks.STONE).strength(1.5F, 6.0F).noOcclusion()));

    public static final RegistrySupplier<Block> REBAR = registerBlock("rebar",
            () -> new CrtBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(3F, 6.0F).noOcclusion()));
    public static final RegistrySupplier<Block> STEEL_POLE = registerBlock("steel_pole",
            () -> new CrtBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(2.5F, 6.0F).noOcclusion()));
    public static final RegistrySupplier<Block> ANTENNA_TOP = registerBlock("antenna_top",
            () -> new CrtBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(2.5F, 6.0F).noOcclusion()));
    public static final RegistrySupplier<Block> PUTER = registerBlock("puter",
            () -> new CrtBlock(BlockProps.copy(Blocks.STONE).strength(1F, 6.0F).noOcclusion()));
    public static final RegistrySupplier<Block> DECO_STEEL_SCAFFOLD = registerBlock("deco_steel_scaffold",
            () -> new CrtBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(2.5F, 6.0F).noOcclusion()));
    public static final RegistrySupplier<Block> STEEL_WALL = registerBlock("steel_wall",
            () -> new SteelWallBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(2.0F, 6.0F).noOcclusion()));

    public static final RegistrySupplier<Block> B29 = registerBlock("b29",
            () -> new BarrelBlock(BlockProps.copy(Blocks.STONE).strength(1.5F, 6.0F).noOcclusion()));

    public static final RegistrySupplier<Block> MINE_FAT = registerBlock("mine_fat",
            () -> new LandmineBlock(BlockProps.copy(Blocks.STONE).strength(1.0F, 6.0F).noOcclusion(), 2.5D, 1D));

    public static final RegistrySupplier<Block> NUKE_FAT_MAN = registerBlockWithoutItem("nuke_fat_man",
            () -> new NukeFatManBlock(BlockProps.copy(Blocks.STONE).strength(1.5F, 6.0F).noOcclusion()));

    public static final RegistrySupplier<Block> NUKE_PROTOTYPE = registerBlockWithoutItem("nuke_prototype",
            () -> new com.hbm_m.block.bomb.NukePrototypeBlock(BlockProps.copy(Blocks.STONE).strength(1.5F, 6.0F).noOcclusion()));

    public static final RegistrySupplier<Block> NUKE_GADGET = registerBlock("nuke_gadget",
            () -> new com.hbm_m.block.bomb.LargeNukeBlock(com.hbm_m.block.bomb.LargeNukeType.GADGET,
                    BlockProps.copy(Blocks.STONE).strength(1.5F, 6.0F).noOcclusion()));

    public static final RegistrySupplier<Block> NUKE_BOY = registerBlock("nuke_boy",
            () -> new com.hbm_m.block.bomb.LargeNukeBlock(com.hbm_m.block.bomb.LargeNukeType.BOY,
                    BlockProps.copy(Blocks.STONE).strength(1.5F, 6.0F).noOcclusion()));

    public static final RegistrySupplier<Block> NUKE_MIKE = registerBlock("nuke_mike",
            () -> new com.hbm_m.block.bomb.LargeNukeBlock(com.hbm_m.block.bomb.LargeNukeType.MIKE,
                    BlockProps.copy(Blocks.STONE).strength(1.5F, 6.0F).noOcclusion()));

    public static final RegistrySupplier<Block> NUKE_TSAR = registerBlock("nuke_tsar",
            () -> new com.hbm_m.block.bomb.LargeNukeBlock(com.hbm_m.block.bomb.LargeNukeType.TSAR,
                    BlockProps.copy(Blocks.STONE).strength(1.5F, 6.0F).noOcclusion()));

    public static final RegistrySupplier<Block> NUKE_FLEIJA = registerBlock("nuke_fleija",
            () -> new com.hbm_m.block.bomb.NukeFleijaBlock(BlockProps.copy(Blocks.STONE).strength(1.5F, 6.0F).noOcclusion()));

    public static final RegistrySupplier<Block> MINE_AP = registerBlock("mine_ap",
            () -> new LandmineBlock(BlockProps.copy(Blocks.STONE).strength(1.0F, 6.0F).noOcclusion(), 1.5D, 1D));

    public static final RegistrySupplier<Block> NAVAL_MINE = registerBlock("naval_mine",
            () -> new LandmineBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(1.0F, 6.0F).noOcclusion(), 2.5D, 1D));

    public static final RegistrySupplier<Block> CRATE_CONSERVE = registerBlock("crate_conserve",
            () -> new CrtBlock(BlockProps.copy(Blocks.STONE).strength(1.5F, 6.0F).noOcclusion()));
    public static final RegistrySupplier<Block> TAPE_RECORDER = registerBlock("tape_recorder",
            () -> new CrtBlock(BlockProps.copy(Blocks.STONE).strength(1.5F, 6.0F).noOcclusion()));

    public static final RegistrySupplier<Block> BARREL_LOX = registerBlock("barrel_lox",
            () -> new CrtBlock(BlockProps.copy(Blocks.STONE).strength(2.0F, 6.0F).noOcclusion()));
    public static final RegistrySupplier<Block> BARREL_CORRODED = registerBlock("barrel_corroded",
            () -> new com.hbm_m.block.machines.BarrelTankBlock(BlockProps.copy(Blocks.STONE).strength(2.0F, 6.0F).noOcclusion(),
                    com.hbm_m.blockentity.machines.BarrelCorrodedBlockEntity::new,
                    () -> com.hbm_m.blockentity.ModBlockEntities.BARREL_CORRODED_BE.get()));
    public static final RegistrySupplier<Block> BARREL_IRON = registerBlock("barrel_iron",
            () -> new com.hbm_m.block.machines.BarrelTankBlock(BlockProps.copy(Blocks.STONE).strength(2.0F, 6.0F).noOcclusion(),
                    com.hbm_m.blockentity.machines.BarrelIronBlockEntity::new,
                    () -> com.hbm_m.blockentity.ModBlockEntities.BARREL_IRON_BE.get(),
                    new com.hbm_m.block.machines.BarrelTankBlock.TooltipInfo(
                            com.hbm_m.blockentity.machines.BarrelIronBlockEntity.CAPACITY,
                            false, false, false, false)));
    public static final RegistrySupplier<Block> BARREL_PINK = registerBlock("barrel_pink",
            () -> new CrtBlock(BlockProps.copy(Blocks.STONE).strength(2.0F, 6.0F).noOcclusion()));
    public static final RegistrySupplier<Block> BARREL_PLASTIC = registerBlock("barrel_plastic",
            () -> new com.hbm_m.block.machines.BarrelTankBlock(BlockProps.copy(Blocks.STONE).strength(2.0F, 6.0F).noOcclusion(),
                    com.hbm_m.blockentity.machines.BarrelPlasticBlockEntity::new,
                    () -> com.hbm_m.blockentity.ModBlockEntities.BARREL_PLASTIC_BE.get(),
                    new com.hbm_m.block.machines.BarrelTankBlock.TooltipInfo(
                            com.hbm_m.blockentity.machines.BarrelPlasticBlockEntity.CAPACITY,
                            false, false, false, false)));
    public static final RegistrySupplier<Block> BARREL_RED = registerBlock("barrel_red",
            () -> new CrtBlock(BlockProps.copy(Blocks.STONE).strength(2.0F, 6.0F).noOcclusion()));
    public static final RegistrySupplier<Block> BARREL_STEEL = registerBlock("barrel_steel",
            () -> new com.hbm_m.block.machines.BarrelTankBlock(BlockProps.copy(Blocks.STONE).strength(2.0F, 6.0F).noOcclusion(),
                    com.hbm_m.blockentity.machines.BarrelSteelBlockEntity::new,
                    () -> com.hbm_m.blockentity.ModBlockEntities.BARREL_STEEL_BE.get(),
                    new com.hbm_m.block.machines.BarrelTankBlock.TooltipInfo(
                            com.hbm_m.blockentity.machines.BarrelSteelBlockEntity.CAPACITY,
                            true, true, false, false)));
    public static final RegistrySupplier<Block> BARREL_TAINT = registerBlock("barrel_taint",
            () -> new CrtBlock(BlockProps.copy(Blocks.STONE).strength(2.0F, 6.0F).noOcclusion()));

    /** Блок заражения (боеголовка MissileTaint). */
    public static final RegistrySupplier<Block> TAINT = registerBlock("taint",
            () -> new BlockTaint(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_GRAY)
                    .strength(15.0F, 10.0F)
                    .randomTicks()
                    .noLootTable()));
    public static final RegistrySupplier<Block> BARREL_TCALLOY = registerBlock("barrel_tcalloy",
            () -> new com.hbm_m.block.machines.BarrelTankBlock(BlockProps.copy(Blocks.STONE).strength(2.0F, 6.0F).noOcclusion(),
                    com.hbm_m.blockentity.machines.BarrelTcalloyBlockEntity::new,
                    () -> com.hbm_m.blockentity.ModBlockEntities.BARREL_TCALLOY_BE.get(),
                    new com.hbm_m.block.machines.BarrelTankBlock.TooltipInfo(
                            com.hbm_m.blockentity.machines.BarrelTcalloyBlockEntity.CAPACITY,
                            true, true, true, false)));
    public static final RegistrySupplier<Block> BARREL_VITRIFIED = registerBlock("barrel_vitrified",
            () -> new CrtBlock(BlockProps.copy(Blocks.STONE).strength(2.0F, 6.0F).noOcclusion()));
    public static final RegistrySupplier<Block> BARREL_YELLOW = registerBlock("barrel_yellow",
            () -> new CrtBlock(BlockProps.copy(Blocks.STONE).strength(2.0F, 6.0F).noOcclusion()));
    public static final RegistrySupplier<Block> BARREL_ANTIMATTER = registerBlock("barrel_antimatter",
            () -> new com.hbm_m.block.machines.BarrelTankBlock(BlockProps.copy(Blocks.STONE).strength(2.0F, 5.0F).noOcclusion(),
                    com.hbm_m.blockentity.machines.BarrelAntimatterBlockEntity::new,
                    () -> com.hbm_m.blockentity.ModBlockEntities.BARREL_ANTIMATTER_BE.get(),
                    new com.hbm_m.block.machines.BarrelTankBlock.TooltipInfo(
                            com.hbm_m.blockentity.machines.BarrelAntimatterBlockEntity.CAPACITY,
                            true, true, true, true)));

    public static final RegistrySupplier<Block> BARBED_WIRE = registerBlock("barbed_wire",
            () -> new BarbedWireBlock(BlockProps.copy(Blocks.STONE).strength(1.5F, 6.0F).noOcclusion()));
    public static final RegistrySupplier<Block> BARBED_WIRE_FIRE = registerBlock("barbed_wire_fire",
            () -> new BarbedWireFireBlock(BlockProps.copy(Blocks.STONE).strength(1.5F, 6.0F).noOcclusion()));
    public static final RegistrySupplier<Block> BARBED_WIRE_WITHER = registerBlock("barbed_wire_wither",
            () -> new BarbedWireWitherBlock(BlockProps.copy(Blocks.STONE).strength(1.5F, 6.0F).noOcclusion()));
    public static final RegistrySupplier<Block> BARBED_WIRE_POISON = registerBlock("barbed_wire_poison",
            () -> new BarbedWirePoisonBlock(BlockProps.copy(Blocks.STONE).strength(1.5F, 6.0F).noOcclusion()));
    public static final RegistrySupplier<Block> BARBED_WIRE_RAD = registerBlock("barbed_wire_rad",
            () -> new BarbedWireRadBlock(BlockProps.copy(Blocks.STONE).strength(1.5F, 6.0F).noOcclusion()));

    public static final RegistrySupplier<Block> TOASTER = registerBlock("toaster",
            () -> new CrtBlock(BlockProps.copy(Blocks.STONE).strength(1.5F, 6.0F).noOcclusion()));
    public static final RegistrySupplier<Block> CRT_BSOD = registerBlock("crt_bsod",
            () -> new CrtBlock(BlockProps.copy(Blocks.STONE).strength(1.5F, 6.0F).noOcclusion()));
    public static final RegistrySupplier<Block> CRT_CLEAN = registerBlock("crt_clean",
            () -> new CrtBlock(BlockProps.copy(Blocks.STONE).strength(1.5F, 6.0F).noOcclusion()));
    public static final RegistrySupplier<Block> CRT_BROKEN = registerBlock("crt_broken",
            () -> new CrtBlock(BlockProps.copy(Blocks.STONE).strength(1.5F, 6.0F).noOcclusion()));


    // ======================================================================

    public static final RegistrySupplier<Block> DEAD_DIRT  = registerBlock("dead_dirt",
            () -> new Block(BlockProps.copy(Blocks.DIRT).strength(0.5f, 4.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> GEYSIR_DIRT  = registerBlock("geysir_dirt",
            () -> new GeysirBlock(BlockProps.copy(Blocks.DIRT).strength(0.5f, 4.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> GEYSIR_STONE  = registerBlock("geysir_stone",
            () -> new GeysirBlock(BlockProps.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));


    public static final RegistrySupplier<Block> SELLAFIELD_SLAKED  = registerBlock("sellafield_slaked",
            () -> new BlockSellafieldSlaked(BlockProps.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> SELLAFIELD_SLAKED1  = registerBlock("sellafield_slaked1",
            () -> new BlockSellafieldSlaked(BlockProps.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> SELLAFIELD_SLAKED2  = registerBlock("sellafield_slaked2",
            () -> new BlockSellafieldSlaked(BlockProps.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> SELLAFIELD_SLAKED3  = registerBlock("sellafield_slaked3",
            () -> new BlockSellafieldSlaked(BlockProps.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> SELLAFIELD_BEDROCK = registerBlock("sellafield_bedrock",
            () -> new BlockSellafieldSlaked(BlockProps.copy(Blocks.BEDROCK)
                    .strength(-1.0F, 3600000.0F)
                    .isValidSpawn((state, level, pos, type) -> false)));

    public static final RegistrySupplier<Block> ORE_SELLAFIELD_DIAMOND = registerBlock("ore_sellafield_diamond",
            () -> BlockSellafieldOre.diamondOre(BlockProps.copy(Blocks.STONE)
                    .strength(5.0F, 10.0F).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> ORE_SELLAFIELD_EMERALD = registerBlock("ore_sellafield_emerald",
            () -> BlockSellafieldOre.emeraldOre(BlockProps.copy(Blocks.STONE)
                    .strength(5.0F, 10.0F).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> ORE_SELLAFIELD_URANIUM_SCORCHED = registerBlock("ore_sellafield_uranium_scorched",
            () -> BlockSellafieldOre.sellafiteOre(BlockProps.copy(Blocks.STONE)
                    .strength(5.0F, 10.0F).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> ORE_SELLAFIELD_SCHRABIDIUM = registerBlock("ore_sellafield_schrabidium",
            () -> BlockSellafieldOre.sellafiteOre(BlockProps.copy(Blocks.STONE)
                    .strength(5.0F, 10.0F).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> ORE_SELLAFIELD_RADGEM = registerBlock("ore_sellafield_radgem",
            () -> BlockSellafieldOre.radgemOre(BlockProps.copy(Blocks.STONE)
                    .strength(5.0F, 10.0F).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> WASTE_TRINITITE = registerBlock("waste_trinitite",
            () -> new BlockOre(BlockProps.copy(Blocks.SAND).strength(0.5F, 2.5F)));

    public static final RegistrySupplier<Block> WASTE_TRINITITE_RED = registerBlock("waste_trinitite_red",
            () -> new BlockOre(BlockProps.copy(Blocks.SAND).strength(0.5F, 2.5F)));

    public static final RegistrySupplier<Block> WASTE_MYCELIUM = registerBlock("waste_mycelium",
            () -> new WasteEarth(BlockProps.copy(Blocks.MYCELIUM)
                    .strength(0.6F)
                    .lightLevel(state -> 1)
                    .randomTicks()));

    // ГРАВИТИРУЮЩИЕ ВЕРСИИ СЕЛЛАФИТА (NEW!)

    public static final RegistrySupplier<Block> BURNED_GRASS  = registerBlock("burned_grass",
            () -> new Block(BlockProps.copy(Blocks.GRASS_BLOCK).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> FALLING_SELLAFIT1 = BLOCKS.register("falling_sellafit1",
            () -> new FallingSellafit(SELLAFIELD_SLAKED.get()));

    public static final RegistrySupplier<Block> FALLING_SELLAFIT2 = BLOCKS.register("falling_sellafit2",
            () -> new FallingSellafit(SELLAFIELD_SLAKED1.get()));

    public static final RegistrySupplier<Block> FALLING_SELLAFIT3 = BLOCKS.register("falling_sellafit3",
            () -> new FallingSellafit(SELLAFIELD_SLAKED2.get()));

    public static final RegistrySupplier<Block> FALLING_SELLAFIT4 = BLOCKS.register("falling_sellafit4",
            () -> new FallingSellafit(SELLAFIELD_SLAKED3.get()));

    public static final RegistrySupplier<Block> ASPHALT = registerBlock("asphalt",
            () -> new Block(BlockProps.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> BARRICADE = registerBlock("barricade",
            () -> new Block(BlockProps.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> BASALT_BRICK = registerBlock("basalt_brick",
            () -> new Block(BlockProps.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> BASALT_POLISHED = registerBlock("basalt_polished",
            () -> new Block(BlockProps.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> BRICK_BASE = registerBlock("brick_base",
            () -> new Block(BlockProps.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> BRICK_DUCRETE = registerBlock("brick_ducrete",
            () -> new Block(BlockProps.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> BRICK_FIRE = registerBlock("brick_fire",
            () -> new Block(BlockProps.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> BRICK_LIGHT = registerBlock("brick_light",
            () -> new Block(BlockProps.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> BRICK_OBSIDIAN = registerBlock("brick_obsidian",
            () -> new Block(BlockProps.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> CONCRETE_ASBESTOS = registerBlock("concrete_asbestos",
            () -> new Block(BlockProps.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> CONCRETE_BLACK = registerBlock("concrete_black",
            () -> new Block(BlockProps.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> CONCRETE_BLUE = registerBlock("concrete_blue",
            () -> new Block(BlockProps.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> CONCRETE_BROWN = registerBlock("concrete_brown",
            () -> new Block(BlockProps.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> CONCRETE_COLORED_BRONZE = registerBlock("concrete_colored_bronze",
            () -> new Block(BlockProps.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> CONCRETE_COLORED_INDIGO = registerBlock("concrete_colored_indigo",
            () -> new Block(BlockProps.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> CONCRETE_COLORED_MACHINE = registerBlock("concrete_colored_machine",
            () -> new Block(BlockProps.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> CONCRETE_COLORED_MACHINE_STRIPE = registerBlock("concrete_colored_machine_stripe",
            () -> new Block(BlockProps.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> CONCRETE_COLORED_PINK = registerBlock("concrete_colored_pink",
            () -> new Block(BlockProps.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> CONCRETE_COLORED_PURPLE = registerBlock("concrete_colored_purple",
            () -> new Block(BlockProps.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> CONCRETE_COLORED_SAND = registerBlock("concrete_colored_sand",
            () -> new Block(BlockProps.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> CONCRETE_CYAN = registerBlock("concrete_cyan",
            () -> new Block(BlockProps.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> CONCRETE_GRAY = registerBlock("concrete_gray",
            () -> new Block(BlockProps.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> CONCRETE_GREEN = registerBlock("concrete_green",
            () -> new Block(BlockProps.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> CONCRETE_LIGHT_BLUE = registerBlock("concrete_light_blue",
            () -> new Block(BlockProps.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> CONCRETE_LIME = registerBlock("concrete_lime",
            () -> new Block(BlockProps.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> CONCRETE_MAGENTA = registerBlock("concrete_magenta",
            () -> new Block(BlockProps.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> CONCRETE_MARKED = registerBlock("concrete_marked",
            () -> new Block(BlockProps.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> CONCRETE_ORANGE = registerBlock("concrete_orange",
            () -> new Block(BlockProps.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> CONCRETE_PINK = registerBlock("concrete_pink",
            () -> new Block(BlockProps.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> CONCRETE_PURPLE = registerBlock("concrete_purple",
            () -> new Block(BlockProps.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> CONCRETE_REBAR = registerBlock("concrete_rebar",
            () -> new Block(BlockProps.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> CONCRETE_REBAR_ALT = registerBlock("concrete_rebar_alt",
            () -> new Block(BlockProps.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> CONCRETE_RED = registerBlock("concrete_red",
            () -> new Block(BlockProps.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> CONCRETE_SILVER = registerBlock("concrete_silver",
            () -> new Block(BlockProps.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> CONCRETE_SUPER = registerBlock("concrete_super",
            () -> new Block(BlockProps.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> CONCRETE_SUPER_BROKEN = registerBlock("concrete_super_broken",
            () -> new Block(BlockProps.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> CONCRETE_SUPER_M0 = registerBlock("concrete_super_m0",
            () -> new Block(BlockProps.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> CONCRETE_SUPER_M1 = registerBlock("concrete_super_m1",
            () -> new Block(BlockProps.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> CONCRETE_SUPER_M2 = registerBlock("concrete_super_m2",
            () -> new Block(BlockProps.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> CONCRETE_SUPER_M3 = registerBlock("concrete_super_m3",
            () -> new Block(BlockProps.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> CONCRETE_TILE = registerBlock("concrete_tile",
            () -> new Block(BlockProps.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> CONCRETE_TILE_TREFOIL = registerBlock("concrete_tile_trefoil",
            () -> new Block(BlockProps.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> CONCRETE_WHITE = registerBlock("concrete_white",
            () -> new Block(BlockProps.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> CONCRETE_YELLOW = registerBlock("concrete_yellow",
            () -> new Block(BlockProps.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> CONCRETE_FLAT = registerBlock("concrete_flat",
            () -> new Block(BlockProps.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> DEPTH_BRICK = registerBlock("depth_brick",
            () -> new DepthOreBlock(BlockProps.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> DEPTH_NETHER_BRICK = registerBlock("depth_nether_brick",
            () -> new DepthOreBlock(BlockProps.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> DEPTH_NETHER_TILES = registerBlock("depth_nether_tiles",
            () -> new DepthOreBlock(BlockProps.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> DEPTH_STONE_NETHER = registerBlock("depth_stone_nether",
            () -> new DepthOreBlock(BlockProps.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> DEPTH_TILES = registerBlock("depth_tiles",
            () -> new DepthOreBlock(BlockProps.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> GNEISS_BRICK = registerBlock("gneiss_brick",
            () -> new Block(BlockProps.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> GNEISS_CHISELED = registerBlock("gneiss_chiseled",
            () -> new Block(BlockProps.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> GNEISS_STONE = registerBlock("gneiss_stone",
            () -> new Block(BlockProps.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> GNEISS_TILE = registerBlock("gneiss_tile",
            () -> new Block(BlockProps.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> METEOR = registerBlock("meteor",
            () -> new Block(BlockProps.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> METEOR_BRICK = registerBlock("meteor_brick",
            () -> new Block(BlockProps.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> METEOR_BRICK_CHISELED = registerBlock("meteor_brick_chiseled",
            () -> new Block(BlockProps.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> METEOR_BRICK_CRACKED = registerBlock("meteor_brick_cracked",
            () -> new Block(BlockProps.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> METEOR_BRICK_MOSSY = registerBlock("meteor_brick_mossy",
            () -> new Block(BlockProps.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> METEOR_COBBLE = registerBlock("meteor_cobble",
            () -> new Block(BlockProps.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> METEOR_CRUSHED = registerBlock("meteor_crushed",
            () -> new Block(BlockProps.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> METEOR_PILLAR = registerBlock("meteor_pillar",
            () -> new Block(BlockProps.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> METEOR_POLISHED = registerBlock("meteor_polished",
            () -> new Block(BlockProps.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> METEOR_TREASURE = registerBlock("meteor_treasure",
            () -> new Block(BlockProps.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> VINYL_TILE = registerBlock("vinyl_tile",
            () -> new Block(BlockProps.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> VINYL_TILE_SMALL = registerBlock("vinyl_tile_small",
            () -> new Block(BlockProps.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> CONCRETE_PILLAR  = registerBlock("concrete_pillar",
            () -> new Block(BlockProps.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> CONCRETE_ASBESTOS_STAIRS = registerBlock("concrete_asbestos_stairs",
            () -> new StairBlock(CONCRETE_ASBESTOS.get().defaultBlockState(), BlockProps.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> CONCRETE_BLACK_STAIRS = registerBlock("concrete_black_stairs",
            () -> new StairBlock(CONCRETE_BLACK.get().defaultBlockState(), BlockProps.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> CONCRETE_BLUE_STAIRS = registerBlock("concrete_blue_stairs",
            () -> new StairBlock(CONCRETE_BLUE.get().defaultBlockState(), BlockProps.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> CONCRETE_BROWN_STAIRS = registerBlock("concrete_brown_stairs",
            () -> new StairBlock(CONCRETE_BROWN.get().defaultBlockState(), BlockProps.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> CONCRETE_COLORED_BRONZE_STAIRS = registerBlock("concrete_colored_bronze_stairs",
            () -> new StairBlock(CONCRETE_COLORED_BRONZE.get().defaultBlockState(), BlockProps.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> CONCRETE_COLORED_INDIGO_STAIRS = registerBlock("concrete_colored_indigo_stairs",
            () -> new StairBlock(CONCRETE_COLORED_INDIGO.get().defaultBlockState(), BlockProps.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> CONCRETE_COLORED_MACHINE_STAIRS = registerBlock("concrete_colored_machine_stairs",
            () -> new StairBlock(CONCRETE_COLORED_MACHINE.get().defaultBlockState(), BlockProps.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> CONCRETE_COLORED_PINK_STAIRS = registerBlock("concrete_colored_pink_stairs",
            () -> new StairBlock(CONCRETE_COLORED_PINK.get().defaultBlockState(), BlockProps.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> CONCRETE_COLORED_PURPLE_STAIRS = registerBlock("concrete_colored_purple_stairs",
            () -> new StairBlock(CONCRETE_COLORED_PURPLE.get().defaultBlockState(), BlockProps.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> CONCRETE_COLORED_SAND_STAIRS = registerBlock("concrete_colored_sand_stairs",
            () -> new StairBlock(CONCRETE_COLORED_SAND.get().defaultBlockState(), BlockProps.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> CONCRETE_CYAN_STAIRS = registerBlock("concrete_cyan_stairs",
            () -> new StairBlock(CONCRETE_CYAN.get().defaultBlockState(), BlockProps.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> CONCRETE_GRAY_STAIRS = registerBlock("concrete_gray_stairs",
            () -> new StairBlock(CONCRETE_GRAY.get().defaultBlockState(), BlockProps.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> CONCRETE_GREEN_STAIRS = registerBlock("concrete_green_stairs",
            () -> new StairBlock(CONCRETE_GREEN.get().defaultBlockState(), BlockProps.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> CONCRETE_LIGHT_BLUE_STAIRS = registerBlock("concrete_light_blue_stairs",
            () -> new StairBlock(CONCRETE_LIGHT_BLUE.get().defaultBlockState(), BlockProps.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> CONCRETE_LIME_STAIRS = registerBlock("concrete_lime_stairs",
            () -> new StairBlock(CONCRETE_LIME.get().defaultBlockState(), BlockProps.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> CONCRETE_MAGENTA_STAIRS = registerBlock("concrete_magenta_stairs",
            () -> new StairBlock(CONCRETE_MAGENTA.get().defaultBlockState(), BlockProps.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> CONCRETE_ORANGE_STAIRS = registerBlock("concrete_orange_stairs",
            () -> new StairBlock(CONCRETE_ORANGE.get().defaultBlockState(), BlockProps.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> CONCRETE_PINK_STAIRS = registerBlock("concrete_pink_stairs",
            () -> new StairBlock(CONCRETE_PINK.get().defaultBlockState(), BlockProps.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> CONCRETE_PURPLE_STAIRS = registerBlock("concrete_purple_stairs",
            () -> new StairBlock(CONCRETE_PURPLE.get().defaultBlockState(), BlockProps.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> CONCRETE_RED_STAIRS = registerBlock("concrete_red_stairs",
            () -> new StairBlock(CONCRETE_RED.get().defaultBlockState(), BlockProps.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> CONCRETE_SILVER_STAIRS = registerBlock("concrete_silver_stairs",
            () -> new StairBlock(CONCRETE_SILVER.get().defaultBlockState(), BlockProps.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> CONCRETE_WHITE_STAIRS = registerBlock("concrete_white_stairs",
            () -> new StairBlock(CONCRETE_WHITE.get().defaultBlockState(), BlockProps.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> CONCRETE_YELLOW_STAIRS = registerBlock("concrete_yellow_stairs",
            () -> new StairBlock(CONCRETE_YELLOW.get().defaultBlockState(), BlockProps.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> CONCRETE_SUPER_STAIRS = registerBlock("concrete_super_stairs",
            () -> new StairBlock(CONCRETE_SUPER.get().defaultBlockState(), BlockProps.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> CONCRETE_SUPER_M0_STAIRS = registerBlock("concrete_super_m0_stairs",
            () -> new StairBlock(CONCRETE_SUPER_M0.get().defaultBlockState(), BlockProps.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> CONCRETE_SUPER_M1_STAIRS = registerBlock("concrete_super_m1_stairs",
            () -> new StairBlock(CONCRETE_SUPER_M1.get().defaultBlockState(), BlockProps.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> CONCRETE_SUPER_M2_STAIRS = registerBlock("concrete_super_m2_stairs",
            () -> new StairBlock(CONCRETE_SUPER_M2.get().defaultBlockState(), BlockProps.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> CONCRETE_SUPER_M3_STAIRS = registerBlock("concrete_super_m3_stairs",
            () -> new StairBlock(CONCRETE_SUPER_M3.get().defaultBlockState(), BlockProps.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> CONCRETE_SUPER_BROKEN_STAIRS = registerBlock("concrete_super_broken_stairs",
            () -> new StairBlock(CONCRETE_SUPER_BROKEN.get().defaultBlockState(), BlockProps.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> CONCRETE_REBAR_STAIRS = registerBlock("concrete_rebar_stairs",
            () -> new StairBlock(CONCRETE_REBAR.get().defaultBlockState(), BlockProps.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> CONCRETE_FLAT_STAIRS = registerBlock("concrete_flat_stairs",
            () -> new StairBlock(CONCRETE_FLAT.get().defaultBlockState(), BlockProps.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> CONCRETE_TILE_STAIRS = registerBlock("concrete_tile_stairs",
            () -> new StairBlock(CONCRETE_TILE.get().defaultBlockState(), BlockProps.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));

	public static final RegistrySupplier<Block> DEPTH_STONE_STAIRS = registerBlock("depth_stone_stairs",
            () -> new StairBlock(DEPTH_BRICK.get().defaultBlockState(), BlockProps.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> DEPTH_BRICK_STAIRS = registerBlock("depth_brick_stairs",
            () -> new StairBlock(DEPTH_BRICK.get().defaultBlockState(), BlockProps.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> DEPTH_TILES_STAIRS = registerBlock("depth_tiles_stairs",
            () -> new StairBlock(DEPTH_TILES.get().defaultBlockState(), BlockProps.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> DEPTH_NETHER_BRICK_STAIRS = registerBlock("depth_nether_brick_stairs",
            () -> new StairBlock(DEPTH_NETHER_BRICK.get().defaultBlockState(), BlockProps.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> DEPTH_NETHER_TILES_STAIRS = registerBlock("depth_nether_tiles_stairs",
            () -> new StairBlock(DEPTH_NETHER_TILES.get().defaultBlockState(), BlockProps.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> GNEISS_TILE_STAIRS = registerBlock("gneiss_tile_stairs",
            () -> new StairBlock(GNEISS_TILE.get().defaultBlockState(), BlockProps.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> GNEISS_BRICK_STAIRS = registerBlock("gneiss_brick_stairs",
            () -> new StairBlock(GNEISS_BRICK.get().defaultBlockState(), BlockProps.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> BRICK_BASE_STAIRS = registerBlock("brick_base_stairs",
            () -> new StairBlock(BRICK_BASE.get().defaultBlockState(), BlockProps.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> BRICK_LIGHT_STAIRS = registerBlock("brick_light_stairs",
            () -> new StairBlock(BRICK_LIGHT.get().defaultBlockState(), BlockProps.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> BRICK_FIRE_STAIRS = registerBlock("brick_fire_stairs",
            () -> new StairBlock(BRICK_FIRE.get().defaultBlockState(), BlockProps.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> BRICK_OBSIDIAN_STAIRS = registerBlock("brick_obsidian_stairs",
            () -> new StairBlock(BRICK_OBSIDIAN.get().defaultBlockState(), BlockProps.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> VINYL_TILE_STAIRS = registerBlock("vinyl_tile_stairs",
            () -> new StairBlock(VINYL_TILE.get().defaultBlockState(), BlockProps.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> VINYL_TILE_SMALL_STAIRS = registerBlock("vinyl_tile_small_stairs",
            () -> new StairBlock(VINYL_TILE_SMALL.get().defaultBlockState(), BlockProps.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> BRICK_DUCRETE_STAIRS = registerBlock("brick_ducrete_stairs",
            () -> new StairBlock(BRICK_DUCRETE.get().defaultBlockState(), BlockProps.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> ASPHALT_STAIRS = registerBlock("asphalt_stairs",
            () -> new StairBlock(ASPHALT.get().defaultBlockState(), BlockProps.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> BASALT_POLISHED_STAIRS = registerBlock("basalt_polished_stairs",
            () -> new StairBlock(BASALT_POLISHED.get().defaultBlockState(), BlockProps.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> BASALT_BRICK_STAIRS = registerBlock("basalt_brick_stairs",
            () -> new StairBlock(BASALT_BRICK.get().defaultBlockState(), BlockProps.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> METEOR_POLISHED_STAIRS = registerBlock("meteor_polished_stairs",
            () -> new StairBlock(METEOR_POLISHED.get().defaultBlockState(), BlockProps.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> METEOR_BRICK_STAIRS = registerBlock("meteor_brick_stairs",
            () -> new StairBlock(METEOR_BRICK.get().defaultBlockState(), BlockProps.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> METEOR_BRICK_CRACKED_STAIRS = registerBlock("meteor_brick_cracked_stairs",
            () -> new StairBlock(METEOR_BRICK_CRACKED.get().defaultBlockState(), BlockProps.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> METEOR_BRICK_MOSSY_STAIRS = registerBlock("meteor_brick_mossy_stairs",
            () -> new StairBlock(METEOR_BRICK_MOSSY.get().defaultBlockState(), BlockProps.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> METEOR_CRUSHED_STAIRS = registerBlock("meteor_crushed_stairs",
            () -> new StairBlock(METEOR_CRUSHED.get().defaultBlockState(), BlockProps.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));


    public static final RegistrySupplier<Block> DEPTH_STONE_SLAB = registerBlock("depth_stone_slab",
            () -> new SlabBlock(BlockProps.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));
    public static final RegistrySupplier<Block> ASPHALT_SLAB = registerBlock("asphalt_slab",
            () -> new SlabBlock(BlockProps.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));
    public static final RegistrySupplier<Block> BASALT_BRICK_SLAB = registerBlock("basalt_brick_slab",
            () -> new SlabBlock(BlockProps.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));
    public static final RegistrySupplier<Block> BASALT_POLISHED_SLAB = registerBlock("basalt_polished_slab",
            () -> new SlabBlock(BlockProps.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));
    public static final RegistrySupplier<Block> BRICK_BASE_SLAB = registerBlock("brick_base_slab",
            () -> new SlabBlock(BlockProps.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));
    public static final RegistrySupplier<Block> BRICK_DUCRETE_SLAB = registerBlock("brick_ducrete_slab",
            () -> new SlabBlock(BlockProps.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));
    public static final RegistrySupplier<Block> BRICK_FIRE_SLAB = registerBlock("brick_fire_slab",
            () -> new SlabBlock(BlockProps.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));
    public static final RegistrySupplier<Block> BRICK_LIGHT_SLAB = registerBlock("brick_light_slab",
            () -> new SlabBlock(BlockProps.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));
    public static final RegistrySupplier<Block> BRICK_OBSIDIAN_SLAB = registerBlock("brick_obsidian_slab",
            () -> new SlabBlock(BlockProps.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));
    public static final RegistrySupplier<Block> CONCRETE_ASBESTOS_SLAB = registerBlock("concrete_asbestos_slab",
            () -> new SlabBlock(BlockProps.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));
    public static final RegistrySupplier<Block> CONCRETE_BLACK_SLAB = registerBlock("concrete_black_slab",
            () -> new SlabBlock(BlockProps.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));
    public static final RegistrySupplier<Block> CONCRETE_BLUE_SLAB = registerBlock("concrete_blue_slab",
            () -> new SlabBlock(BlockProps.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));
    public static final RegistrySupplier<Block> CONCRETE_BROWN_SLAB = registerBlock("concrete_brown_slab",
            () -> new SlabBlock(BlockProps.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));
    public static final RegistrySupplier<Block> CONCRETE_COLORED_BRONZE_SLAB = registerBlock("concrete_colored_bronze_slab",
            () -> new SlabBlock(BlockProps.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));
    public static final RegistrySupplier<Block> CONCRETE_COLORED_INDIGO_SLAB = registerBlock("concrete_colored_indigo_slab",
            () -> new SlabBlock(BlockProps.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));
    public static final RegistrySupplier<Block> CONCRETE_COLORED_MACHINE_SLAB = registerBlock("concrete_colored_machine_slab",
            () -> new SlabBlock(BlockProps.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));
    public static final RegistrySupplier<Block> CONCRETE_COLORED_PINK_SLAB = registerBlock("concrete_colored_pink_slab",
            () -> new SlabBlock(BlockProps.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));
    public static final RegistrySupplier<Block> CONCRETE_COLORED_PURPLE_SLAB = registerBlock("concrete_colored_purple_slab",
            () -> new SlabBlock(BlockProps.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));
    public static final RegistrySupplier<Block> CONCRETE_COLORED_SAND_SLAB = registerBlock("concrete_colored_sand_slab",
            () -> new SlabBlock(BlockProps.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));
    public static final RegistrySupplier<Block> CONCRETE_CYAN_SLAB = registerBlock("concrete_cyan_slab",
            () -> new SlabBlock(BlockProps.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));
    public static final RegistrySupplier<Block> CONCRETE_GRAY_SLAB = registerBlock("concrete_gray_slab",
            () -> new SlabBlock(BlockProps.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));
    public static final RegistrySupplier<Block> CONCRETE_GREEN_SLAB = registerBlock("concrete_green_slab",
            () -> new SlabBlock(BlockProps.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));
    public static final RegistrySupplier<Block> CONCRETE_LIGHT_BLUE_SLAB = registerBlock("concrete_light_blue_slab",
            () -> new SlabBlock(BlockProps.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));
    public static final RegistrySupplier<Block> CONCRETE_LIME_SLAB = registerBlock("concrete_lime_slab",
            () -> new SlabBlock(BlockProps.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));
    public static final RegistrySupplier<Block> CONCRETE_MAGENTA_SLAB = registerBlock("concrete_magenta_slab",
            () -> new SlabBlock(BlockProps.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));
    public static final RegistrySupplier<Block> CONCRETE_ORANGE_SLAB = registerBlock("concrete_orange_slab",
            () -> new SlabBlock(BlockProps.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));
    public static final RegistrySupplier<Block> CONCRETE_PINK_SLAB = registerBlock("concrete_pink_slab",
            () -> new SlabBlock(BlockProps.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));
    public static final RegistrySupplier<Block> CONCRETE_PURPLE_SLAB = registerBlock("concrete_purple_slab",
            () -> new SlabBlock(BlockProps.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));
    public static final RegistrySupplier<Block> CONCRETE_REBAR_SLAB = registerBlock("concrete_rebar_slab",
            () -> new SlabBlock(BlockProps.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));
    public static final RegistrySupplier<Block> CONCRETE_RED_SLAB = registerBlock("concrete_red_slab",
            () -> new SlabBlock(BlockProps.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));
    public static final RegistrySupplier<Block> CONCRETE_SILVER_SLAB = registerBlock("concrete_silver_slab",
            () -> new SlabBlock(BlockProps.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));
    public static final RegistrySupplier<Block> CONCRETE_SUPER_SLAB = registerBlock("concrete_super_slab",
            () -> new SlabBlock(BlockProps.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));
    public static final RegistrySupplier<Block> CONCRETE_SUPER_BROKEN_SLAB = registerBlock("concrete_super_broken_slab",
            () -> new SlabBlock(BlockProps.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));
    public static final RegistrySupplier<Block> CONCRETE_SUPER_M0_SLAB = registerBlock("concrete_super_m0_slab",
            () -> new SlabBlock(BlockProps.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));
    public static final RegistrySupplier<Block> CONCRETE_SUPER_M1_SLAB = registerBlock("concrete_super_m1_slab",
            () -> new SlabBlock(BlockProps.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));
    public static final RegistrySupplier<Block> CONCRETE_SUPER_M2_SLAB = registerBlock("concrete_super_m2_slab",
            () -> new SlabBlock(BlockProps.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));
    public static final RegistrySupplier<Block> CONCRETE_SUPER_M3_SLAB = registerBlock("concrete_super_m3_slab",
            () -> new SlabBlock(BlockProps.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));
    public static final RegistrySupplier<Block> CONCRETE_TILE_SLAB = registerBlock("concrete_tile_slab",
            () -> new SlabBlock(BlockProps.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));
    public static final RegistrySupplier<Block> CONCRETE_WHITE_SLAB = registerBlock("concrete_white_slab",
            () -> new SlabBlock(BlockProps.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));
    public static final RegistrySupplier<Block> CONCRETE_YELLOW_SLAB = registerBlock("concrete_yellow_slab",
            () -> new SlabBlock(BlockProps.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));
    public static final RegistrySupplier<Block> CONCRETE_FLAT_SLAB = registerBlock("concrete_flat_slab",
            () -> new SlabBlock(BlockProps.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));
    public static final RegistrySupplier<Block> DEPTH_BRICK_SLAB = registerBlock("depth_brick_slab",
            () -> new SlabBlock(BlockProps.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));
    public static final RegistrySupplier<Block> DEPTH_NETHER_BRICK_SLAB = registerBlock("depth_nether_brick_slab",
            () -> new SlabBlock(BlockProps.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));
    public static final RegistrySupplier<Block> DEPTH_NETHER_TILES_SLAB = registerBlock("depth_nether_tiles_slab",
            () -> new SlabBlock(BlockProps.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));
    public static final RegistrySupplier<Block> DEPTH_STONE_NETHER_SLAB = registerBlock("depth_stone_nether_slab",
            () -> new SlabBlock(BlockProps.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));
    public static final RegistrySupplier<Block> DEPTH_TILES_SLAB = registerBlock("depth_tiles_slab",
            () -> new SlabBlock(BlockProps.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));
    public static final RegistrySupplier<Block> GNEISS_BRICK_SLAB = registerBlock("gneiss_brick_slab",
            () -> new SlabBlock(BlockProps.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));
    public static final RegistrySupplier<Block> GNEISS_TILE_SLAB = registerBlock("gneiss_tile_slab",
            () -> new SlabBlock(BlockProps.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));
    public static final RegistrySupplier<Block> METEOR_BRICK_SLAB = registerBlock("meteor_brick_slab",
            () -> new SlabBlock(BlockProps.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));
    public static final RegistrySupplier<Block> METEOR_BRICK_CRACKED_SLAB = registerBlock("meteor_brick_cracked_slab",
            () -> new SlabBlock(BlockProps.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));
    public static final RegistrySupplier<Block> METEOR_BRICK_MOSSY_SLAB = registerBlock("meteor_brick_mossy_slab",
            () -> new SlabBlock(BlockProps.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));
    public static final RegistrySupplier<Block> METEOR_CRUSHED_SLAB = registerBlock("meteor_crushed_slab",
            () -> new SlabBlock(BlockProps.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));
    public static final RegistrySupplier<Block> METEOR_POLISHED_SLAB = registerBlock("meteor_polished_slab",
            () -> new SlabBlock(BlockProps.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));
    public static final RegistrySupplier<Block> VINYL_TILE_SLAB = registerBlock("vinyl_tile_slab",
            () -> new SlabBlock(BlockProps.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));
    public static final RegistrySupplier<Block> VINYL_TILE_SMALL_SLAB = registerBlock("vinyl_tile_small_slab",
            () -> new SlabBlock(BlockProps.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));



    public static final RegistrySupplier<Block> CONCRETE_FAN  = registerBlock("concrete_fan",
            () -> new Block(BlockProps.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> BRICK_CONCRETE_BROKEN = registerBlock("brick_concrete_broken",
            () -> new Block(BlockProps.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));
    public static final RegistrySupplier<Block> BRICK_CONCRETE_BROKEN_STAIRS = registerBlock("brick_concrete_broken_stairs",
            () -> new StairBlock(ModBlocks.BRICK_CONCRETE_BROKEN.get().defaultBlockState(),
                    BlockProps.copy(Blocks.STONE).sound(SoundType.STONE)));
    public static final RegistrySupplier<Block> BRICK_CONCRETE_BROKEN_SLAB = registerBlock("brick_concrete_broken_slab",
            () -> new SlabBlock(BlockProps.copy(Blocks.STONE).sound(SoundType.STONE)));

    public static final RegistrySupplier<Block> BRICK_CONCRETE_CRACKED = registerBlock("brick_concrete_cracked",
            () -> new Block(BlockProps.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));
    public static final RegistrySupplier<Block> BRICK_CONCRETE_CRACKED_STAIRS = registerBlock("brick_concrete_cracked_stairs",
            () -> new StairBlock(ModBlocks.BRICK_CONCRETE_CRACKED.get().defaultBlockState(),
                    BlockProps.copy(Blocks.STONE).sound(SoundType.STONE)));
    public static final RegistrySupplier<Block> BRICK_CONCRETE_CRACKED_SLAB = registerBlock("brick_concrete_cracked_slab",
            () -> new SlabBlock(BlockProps.copy(Blocks.STONE).sound(SoundType.STONE)));

    public static final RegistrySupplier<Block> BRICK_CONCRETE_MOSSY = registerBlock("brick_concrete_mossy",
            () -> new Block(BlockProps.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));
    public static final RegistrySupplier<Block> BRICK_CONCRETE_MOSSY_STAIRS = registerBlock("brick_concrete_mossy_stairs",
            () -> new StairBlock(ModBlocks.BRICK_CONCRETE_MOSSY.get().defaultBlockState(),
                    BlockProps.copy(Blocks.STONE).sound(SoundType.STONE)));
    public static final RegistrySupplier<Block> BRICK_CONCRETE_MOSSY_SLAB = registerBlock("brick_concrete_mossy_slab",
            () -> new SlabBlock(BlockProps.copy(Blocks.STONE).sound(SoundType.STONE)));

    public static final RegistrySupplier<Block> BRICK_CONCRETE_MARKED = registerBlock("brick_concrete_marked",
            () -> new Block(BlockProps.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> REINFORCED_STONE_STAIRS = registerBlock("reinforced_stone_stairs",
            () -> new StairBlock(ModBlocks.REINFORCED_STONE.get().defaultBlockState(),
                    BlockProps.copy(Blocks.STONE).sound(SoundType.STONE)));
    public static final RegistrySupplier<Block> REINFORCED_STONE_SLAB = registerBlock("reinforced_stone_slab",
            () -> new SlabBlock(BlockProps.copy(Blocks.STONE).sound(SoundType.STONE)));


    private static final BlockBehaviour.Properties CRATE_PROPERTIES =
            BlockProps.copy(Blocks.IRON_BLOCK).sound(SoundType.METAL).strength(0.5f, 1f).requiresCorrectToolForDrops();

    public static final RegistrySupplier<Block> CRATE_IRON = registerBlockWithoutItem("crate_iron",
            () -> new IronCrateBlock(CRATE_PROPERTIES));

    public static final RegistrySupplier<Block> CRATE_STEEL = registerBlockWithoutItem("crate_steel",
            () -> new SteelCrateBlock(CRATE_PROPERTIES));

    public static final RegistrySupplier<Block> CRATE_DESH = registerBlockWithoutItem("crate_desh",
            () -> new DeshCrateBlock(BlockProps.copy(Blocks.IRON_BLOCK).sound(SoundType.METAL).strength(1.5f, 2f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> CRATE_TUNGSTEN = registerBlockWithoutItem("crate_tungsten",
            () -> new TungstenCrateBlock(BlockProps.copy(Blocks.IRON_BLOCK).sound(SoundType.METAL).strength(2.0f, 3f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> CRATE_TEMPLATE = registerBlockWithoutItem("crate_template",
            () -> new TemplateCrateBlock(CRATE_PROPERTIES));

    public static final RegistrySupplier<Block> WASTE_PLANKS = registerBlock("waste_planks",
            () -> new Block(BlockProps.copy(Blocks.OAK_WOOD).strength(3.0f, 3.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> WASTE_LOG = registerBlock("waste_log",
            () -> new Block(BlockProps.copy(Blocks.COAL_BLOCK).strength(3.0f, 3.0f).requiresCorrectToolForDrops()));


    // -----------------------<РАСТЕНИЯ>-----------------------------
    public static final RegistrySupplier<Block> STRAWBERRY_BUSH = registerBlock("strawberry_bush",
            () -> PlatformHooks.createFlowerBlock(MobEffects.LUCK, 5,
                    BlockProps.copy(Blocks.ALLIUM).noOcclusion().noCollission()));


    // -----------------------<РУДЫ>-----------------------------


    public static final RegistrySupplier<Block> RESOURCE_ASBESTOS = registerBlock("resource_asbestos",
            () -> new Block(BlockProps.copy(Blocks.STONE).strength(3.0f, 3.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> RESOURCE_BAUXITE = registerBlock("resource_bauxite",
            () -> new Block(BlockProps.copy(Blocks.STONE).strength(3.0f, 3.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> RESOURCE_HEMATITE = registerBlock("resource_hematite",
            () -> new Block(BlockProps.copy(Blocks.STONE).strength(3.0f, 3.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> RESOURCE_LIMESTONE = registerBlock("resource_limestone",
            () -> new Block(BlockProps.copy(Blocks.STONE).strength(3.0f, 3.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> RESOURCE_MALACHITE = registerBlock("resource_malachite",
            () -> new Block(BlockProps.copy(Blocks.STONE).strength(3.0f, 3.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> RESOURCE_SULFUR = registerBlock("resource_sulfur",
            () -> new Block(BlockProps.copy(Blocks.STONE).strength(3.0f, 3.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> SEQUESTRUM_ORE = registerBlock("sequestrum_ore",
            () -> new Block(BlockProps.copy(Blocks.STONE).strength(3.0f, 3.0f).requiresCorrectToolForDrops()));


    public static final RegistrySupplier<Block> LIGNITE_ORE = registerBlock("lignite_ore",
            () -> new Block(BlockProps.copy(Blocks.STONE).strength(3.0f, 3.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> ALUMINUM_ORE = registerBlock("aluminum_ore",
            () -> new Block(BlockProps.copy(Blocks.STONE).strength(3.0f, 3.0f).requiresCorrectToolForDrops()));


	public static final RegistrySupplier<Block> URANIUM_ORE = registerBlock("uranium_ore",
            () -> PlatformHooks.createDropExperienceBlock(BlockProps.copy(Blocks.STONE).strength(3.0F, 3.0F).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> LEAD_ORE = registerBlock("lead_ore",
            () -> new Block(BlockProps.copy(Blocks.STONE).strength(3.0f, 3.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> RAREGROUND_ORE = registerBlock("rareground_ore",
            () -> new Block(BlockProps.copy(Blocks.STONE).strength(3.0f, 3.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> FLUORITE_ORE = registerBlock("fluorite_ore",
            () -> new Block(BlockProps.copy(Blocks.STONE).strength(3.0f, 3.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> BERYLLIUM_ORE = registerBlock("beryllium_ore",
            () -> new Block(BlockProps.copy(Blocks.STONE).strength(3.0f, 3.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> ASBESTOS_ORE = registerBlock("asbestos_ore",
            () -> new Block(BlockProps.copy(Blocks.STONE).strength(3.0f, 3.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> CINNABAR_ORE = registerBlock("cinnabar_ore",
            () -> new Block(BlockProps.copy(Blocks.STONE).strength(3.0f, 3.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> COBALT_ORE = registerBlock("cobalt_ore",
            () -> new Block(BlockProps.copy(Blocks.STONE).strength(3.0f, 3.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> TUNGSTEN_ORE = registerBlock("tungsten_ore",
            () -> new Block(BlockProps.copy(Blocks.STONE).strength(3.0f, 3.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> THORIUM_ORE = registerBlock("thorium_ore",
            () -> new Block(BlockProps.copy(Blocks.STONE).strength(3.0f, 3.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> FREAKY_ALIEN_BLOCK = registerBlock("freaky_alien_block",
            () -> new Block(BlockProps.copy(Blocks.STONE).strength(3.0f, 3.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> TITANIUM_ORE = registerBlock("titanium_ore",
            () -> new Block(BlockProps.copy(Blocks.STONE).strength(3.0f, 3.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> SULFUR_ORE = registerBlock("sulfur_ore",
            () -> new Block(BlockProps.copy(Blocks.STONE).strength(3.0f, 3.0f).requiresCorrectToolForDrops()));

    // Дипслейт руды
    public static final RegistrySupplier<Block> URANIUM_ORE_DEEPSLATE = registerBlock("uranium_ore_deepslate",
            () -> new Block(BlockProps.copy(Blocks.DEEPSLATE).strength(5.0f, 5.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> BERYLLIUM_ORE_DEEPSLATE = registerBlock("beryllium_ore_deepslate",
            () -> new Block(BlockProps.copy(Blocks.DEEPSLATE).strength(5.0f, 5.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> TITANIUM_ORE_DEEPSLATE = registerBlock("titanium_ore_deepslate",
            () -> new Block(BlockProps.copy(Blocks.DEEPSLATE).strength(5.0f, 5.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> LEAD_ORE_DEEPSLATE = registerBlock("lead_ore_deepslate",
            () -> new Block(BlockProps.copy(Blocks.DEEPSLATE).strength(5.0f, 5.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> RAREGROUND_ORE_DEEPSLATE = registerBlock("rareground_ore_deepslate",
            () -> new Block(BlockProps.copy(Blocks.DEEPSLATE).strength(5.0f, 5.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> THORIUM_ORE_DEEPSLATE = registerBlock("thorium_ore_deepslate",
            () -> new Block(BlockProps.copy(Blocks.DEEPSLATE).strength(5.0f, 5.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> ALUMINUM_ORE_DEEPSLATE = registerBlock("aluminum_ore_deepslate",
            () -> new Block(BlockProps.copy(Blocks.DEEPSLATE).strength(5.0f, 5.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> COBALT_ORE_DEEPSLATE = registerBlock("cobalt_ore_deepslate",
            () -> new Block(BlockProps.copy(Blocks.DEEPSLATE).strength(5.0f, 5.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> CINNABAR_ORE_DEEPSLATE = registerBlock("cinnabar_ore_deepslate",
            () -> new Block(BlockProps.copy(Blocks.DEEPSLATE).strength(5.0f, 5.0f).requiresCorrectToolForDrops()));

    /** Порт {@code ore_schrabidium} (GIT ModBlocks). */
    public static final RegistrySupplier<Block> SCHRABIDIUM_ORE = registerBlock("schrabidium_ore",
            () -> PlatformHooks.createDropExperienceBlock(BlockProps.copy(Blocks.STONE)
                    .strength(15.0F, 600.0F).requiresCorrectToolForDrops()));

    /** Порт {@code ore_nether_schrabidium}. */
    public static final RegistrySupplier<Block> SCHRABIDIUM_ORE_NETHER = registerBlock("schrabidium_ore_nether",
            () -> new Block(BlockProps.copy(Blocks.NETHERRACK)
                    .strength(15.0F, 600.0F).requiresCorrectToolForDrops()));

    /** Порт {@code ore_gneiss_schrabidium}. */
    public static final RegistrySupplier<Block> SCHRABIDIUM_ORE_GNEISS = registerBlock("schrabidium_ore_gneiss",
            () -> PlatformHooks.createDropExperienceBlock(BlockProps.copy(Blocks.STONE)
                    .strength(1.5F, 10.0F).requiresCorrectToolForDrops()));

    /** Порт {@code block_schrabidium_cluster} ({@link com.hbm.blocks.generic.BlockRotatablePillar}). */
    public static final RegistrySupplier<Block> BLOCK_SCHRABIDIUM_CLUSTER = registerBlock("block_schrabidium_cluster",
            () -> new RotatedPillarBlock(BlockProps.copy(Blocks.IRON_BLOCK)
                    .strength(5.0F, 60000.0F).requiresCorrectToolForDrops()));

    //======================= ЖИДКОСТИ ==========================================//










    // ==================== Helper Methods ====================

    private static RegistrySupplier<Block> registerAnvil(String name, AnvilTier tier) {
        return registerBlock(name, () -> new AnvilBlock(ANVIL_PROPERTIES, tier));
    }

    @SuppressWarnings("unchecked")
    private static RegistrySupplier<Block> registerRadAbsorberBlock(String name, Supplier<BlockAbsorber> block) {
        RegistrySupplier<BlockAbsorber> toReturn = BLOCKS.register(name, block);
        ModItems.ITEMS.register(name, () -> new BlockAbsorberItem(toReturn.get(), new Item.Properties()));
        return (RegistrySupplier<Block>) (RegistrySupplier<?>) toReturn;
    }

    // ─── RBMK Columns ────────────────────────────────────────────────────────

    private static BlockBehaviour.Properties rbmkProps() {
        return BlockProps.copy(Blocks.IRON_BLOCK).strength(4.0f).noOcclusion()
                .isSuffocating((s, w, p) -> false);
    }

    // ── Fuel Channels ──────────────────────────────────────────────────────────
    public static final RegistrySupplier<Block> RBMK_ROD          = registerBlock("rbmk_element",      () -> new RBMKRodBlock(false, rbmkProps()));
    public static final RegistrySupplier<Block> RBMK_ROD_MOD      = registerBlock("rbmk_element_mod",  () -> new RBMKRodBlock(true,  rbmkProps()));
    /** ReaSim variants: same logic/BlockEntity as the base rod, distinct skin only (matches the rbmk_control_reasim precedent). */
    public static final RegistrySupplier<Block> RBMK_ROD_REASIM       = registerBlock("rbmk_element_reasim",       () -> new RBMKRodBlock(false, rbmkProps()));
    public static final RegistrySupplier<Block> RBMK_ROD_REASIM_MOD   = registerBlock("rbmk_element_reasim_mod",   () -> new RBMKRodBlock(true,  rbmkProps()));

    // ── Control Rods ────────────────────────────────────────────────────────
    public static final RegistrySupplier<Block> RBMK_CONTROL               = registerBlock("rbmk_control",               () -> new RBMKControlManualBlock(false, rbmkProps()));
    public static final RegistrySupplier<Block> RBMK_CONTROL_BLUE          = registerBlock("rbmk_control_blue",          () -> new RBMKControlManualBlock(false, rbmkProps()));
    public static final RegistrySupplier<Block> RBMK_CONTROL_GREEN         = registerBlock("rbmk_control_green",         () -> new RBMKControlManualBlock(false, rbmkProps()));
    public static final RegistrySupplier<Block> RBMK_CONTROL_YELLOW        = registerBlock("rbmk_control_yellow",        () -> new RBMKControlManualBlock(false, rbmkProps()));
    public static final RegistrySupplier<Block> RBMK_CONTROL_PURPLE        = registerBlock("rbmk_control_purple",        () -> new RBMKControlManualBlock(false, rbmkProps()));
    public static final RegistrySupplier<Block> RBMK_CONTROL_MOD           = registerBlock("rbmk_control_mod",           () -> new RBMKControlManualBlock(true,  rbmkProps()));
    public static final RegistrySupplier<Block> RBMK_CONTROL_AUTO          = registerBlock("rbmk_control_auto",          () -> new RBMKControlAutoBlock(false, rbmkProps()));
    public static final RegistrySupplier<Block> RBMK_CONTROL_MOD_AUTO      = registerBlock("rbmk_control_mod_auto",      () -> new RBMKControlAutoBlock(true,  rbmkProps()));
    public static final RegistrySupplier<Block> RBMK_CONTROL_REASIM        = registerBlock("rbmk_control_reasim",        () -> new RBMKControlManualBlock(false, rbmkProps()));
    public static final RegistrySupplier<Block> RBMK_CONTROL_REASIM_AUTO   = registerBlock("rbmk_control_reasim_auto",   () -> new RBMKControlAutoBlock(false,  rbmkProps()));

    // ── Passive Columns ─────────────────────────────────────────────────────
    public static final RegistrySupplier<Block> RBMK_MODERATOR    = registerBlock("rbmk_moderator",    () -> new RBMKModeratorBlock(rbmkProps()));
    public static final RegistrySupplier<Block> RBMK_ABSORBER     = registerBlock("rbmk_absorber",     () -> new RBMKAbsorberBlock(rbmkProps()));
    public static final RegistrySupplier<Block> RBMK_REFLECTOR    = registerBlock("rbmk_reflector",    () -> new RBMKReflectorBlock(rbmkProps()));
    public static final RegistrySupplier<Block> RBMK_COOLER       = registerBlock("rbmk_cooler",       () -> new RBMKCoolerBlock(rbmkProps()));
    public static final RegistrySupplier<Block> RBMK_BOILER       = registerBlock("rbmk_boiler",       () -> new RBMKBoilerBlock(rbmkProps()));
    public static final RegistrySupplier<Block> RBMK_HEATER       = registerBlock("rbmk_heater",       () -> new RBMKHeaterBlock(rbmkProps()));
    public static final RegistrySupplier<Block> RBMK_OUTGASSER    = registerBlock("rbmk_outgasser",    () -> new RBMKOutgasserBlock(rbmkProps()));
    public static final RegistrySupplier<Block> RBMK_STORAGE      = registerBlock("rbmk_storage",      () -> new RBMKStorageBlock(rbmkProps()));
    public static final RegistrySupplier<Block> RBMK_BLANK        = registerBlock("rbmk_blank",        () -> new RBMKBlankBlock(rbmkProps()));

    // ── Fluid Connection ────────────────────────────────────────────────────
    public static final RegistrySupplier<Block> RBMK_STEAM_INLET  = registerBlock("rbmk_steam_inlet",  () -> new RBMKSteamInletBlock(rbmkProps()));
    public static final RegistrySupplier<Block> RBMK_STEAM_OUTLET = registerBlock("rbmk_steam_outlet", () -> new RBMKSteamOutletBlock(rbmkProps()));

    // ── Crane / Loader ──────────────────────────────────────────────────────
    public static final RegistrySupplier<Block> RBMK_LOADER       = registerBlock("rbmk_loader",       () -> new RBMKLoaderBlock(rbmkProps()));
    public static final RegistrySupplier<Block> RBMK_AUTOLOADER   = registerBlock("rbmk_autoloader",   () -> new RBMKAutoloaderBlock(rbmkProps()));
    public static final RegistrySupplier<Block> RBMK_CRANE_CONSOLE= registerBlock("rbmk_crane_console",() -> new RBMKCraneConsoleBlock(rbmkProps()));

    /** Invisible solid filler placed above every column so it has a real 1x3 hitbox (see
     *  {@link com.hbm_m.block.machines.rbmk.RBMKColumnFillerBlock}). Not directly placeable;
     *  breaking it cascades into destroying the real column below (same feel/hardness as the
     *  column itself, matched via {@link #rbmkProps()}). */
    public static final RegistrySupplier<Block> RBMK_COLUMN_FILLER = registerBlockWithoutItem("rbmk_column_filler",
            () -> new com.hbm_m.block.machines.rbmk.RBMKColumnFillerBlock(rbmkProps().noLootTable()));

    // ── Debris ──────────────────────────────────────────────────────────────
    public static final RegistrySupplier<Block> RBMK_DEBRIS            = registerBlock("rbmk_debris",            () -> new Block(BlockProps.copy(Blocks.GRAVEL).strength(0.5f)));
    public static final RegistrySupplier<Block> RBMK_DEBRIS_BURNING    = registerBlock("rbmk_debris_burning",    () -> new Block(BlockProps.copy(Blocks.GRAVEL).strength(0.5f).lightLevel(s -> 10)));
    public static final RegistrySupplier<Block> RBMK_DEBRIS_DIGAMMA    = registerBlock("rbmk_debris_digamma",    () -> new Block(BlockProps.copy(Blocks.GRAVEL).strength(0.5f).lightLevel(s -> 8)));
    public static final RegistrySupplier<Block> RBMK_DEBRIS_RADIATING  = registerBlock("rbmk_debris_radiating",  () -> new Block(BlockProps.copy(Blocks.GRAVEL).strength(0.5f).lightLevel(s -> 4)));

    // ── Corium (molten reactor core, 1:1 with the original's ModBlocks.corium_block) ──────────
    public static final RegistrySupplier<Block> RBMK_CORIUM = registerBlock("rbmk_corium",
            () -> new Block(BlockProps.copy(Blocks.MAGMA_BLOCK).strength(3.0f).lightLevel(s -> 15)));

    // ── Panel / Display Blocks ──────────────────────────────────────────────
    // RBMK_DISPLAY / RBMK_DISPLAY_BLANK: reactor-status link target (like the console/crane
    // targets), not one of the 7 RTTY devices below - stays on the generic no-op panel BE.
    public static final RegistrySupplier<Block> RBMK_DISPLAY   = registerBlock("rbmk_display",   () -> new RBMKPanelBlock(rbmkProps()));
    /** Blank decorative panel, reuses the rbmk_display texture (matches the original, which had no dedicated texture for it either). */
    public static final RegistrySupplier<Block> RBMK_DISPLAY_BLANK = registerBlock("rbmk_display_blank", () -> new RBMKPanelBlock(rbmkProps()));

    // The 7 RTTY-driven panel devices - each wired to its own block entity, config screen and
    // (Lever/KeyPad) primary-click action via the shared RBMKPanelDeviceBlock (see that class).
    public static final RegistrySupplier<Block> RBMK_GAUGE = registerBlock("rbmk_gauge", () ->
            new com.hbm_m.block.machines.rbmk.RBMKPanelDeviceBlock(rbmkProps(),
                    com.hbm_m.blockentity.machines.rbmk.RBMKGaugeBlockEntity::new,
                    () -> com.hbm_m.blockentity.ModBlockEntities.RBMK_GAUGE_BE.get(),
                    "gauge", true, null));

    public static final RegistrySupplier<Block> RBMK_INDICATOR = registerBlock("rbmk_indicator", () ->
            new com.hbm_m.block.machines.rbmk.RBMKPanelDeviceBlock(rbmkProps(),
                    com.hbm_m.blockentity.machines.rbmk.RBMKIndicatorBlockEntity::new,
                    () -> com.hbm_m.blockentity.ModBlockEntities.RBMK_INDICATOR_BE.get(),
                    "indicator", true, null));

    public static final RegistrySupplier<Block> RBMK_NUMITRON = registerBlock("rbmk_numitron", () ->
            new com.hbm_m.block.machines.rbmk.RBMKPanelDeviceBlock(rbmkProps(),
                    com.hbm_m.blockentity.machines.rbmk.RBMKNumitronBlockEntity::new,
                    () -> com.hbm_m.blockentity.ModBlockEntities.RBMK_NUMITRON_BE.get(),
                    "numitron", true, null));

    public static final RegistrySupplier<Block> RBMK_GRAPH = registerBlock("rbmk_graph", () ->
            new com.hbm_m.block.machines.rbmk.RBMKPanelDeviceBlock(rbmkProps(),
                    com.hbm_m.blockentity.machines.rbmk.RBMKGraphBlockEntity::new,
                    () -> com.hbm_m.blockentity.ModBlockEntities.RBMK_GRAPH_BE.get(),
                    "graph", true, null));

    public static final RegistrySupplier<Block> RBMK_LEVER = registerBlock("rbmk_lever", () ->
            new com.hbm_m.block.machines.rbmk.RBMKPanelDeviceBlock(rbmkProps(),
                    com.hbm_m.blockentity.machines.rbmk.RBMKLeverBlockEntity::new,
                    () -> com.hbm_m.blockentity.ModBlockEntities.RBMK_LEVER_BE.get(),
                    "lever", false,
                    (be, level, pos, player, hit) -> {
                        if (be instanceof com.hbm_m.blockentity.machines.rbmk.RBMKLeverBlockEntity lever) {
                            lever.flipLever(level, pos, player,
                                    com.hbm_m.blockentity.machines.rbmk.RBMKLeverBlockEntity.unitFromHit(pos, hit));
                        }
                    }));

    public static final RegistrySupplier<Block> RBMK_KEYPAD = registerBlock("rbmk_keypad", () ->
            new com.hbm_m.block.machines.rbmk.RBMKPanelDeviceBlock(rbmkProps(),
                    com.hbm_m.blockentity.machines.rbmk.RBMKKeyPadBlockEntity::new,
                    () -> com.hbm_m.blockentity.ModBlockEntities.RBMK_KEYPAD_BE.get(),
                    "keypad", false,
                    (be, level, pos, player, hit) -> {
                        if (be instanceof com.hbm_m.blockentity.machines.rbmk.RBMKKeyPadBlockEntity keypad) {
                            keypad.click(level, pos, player,
                                    com.hbm_m.blockentity.machines.rbmk.RBMKKeyPadBlockEntity.unitFromHit(hit));
                        }
                    }));

    public static final RegistrySupplier<Block> RBMK_TERMINAL = registerBlock("rbmk_terminal", () ->
            new com.hbm_m.block.machines.rbmk.RBMKPanelDeviceBlock(rbmkProps(),
                    com.hbm_m.blockentity.machines.rbmk.RBMKTerminalBlockEntity::new,
                    () -> com.hbm_m.blockentity.ModBlockEntities.RBMK_TERMINAL_BE.get(),
                    "terminal", false, null));

    // ══════════════════════════════════════════════════════════════════════
    // DEV: Blöcke aus dem Original-HBM-Mod, die hier noch fehlen (zur Sichtung)
    // Texturen importiert, generische Block-Properties als Platzhalter.
    // ══════════════════════════════════════════════════════════════════════
    public static final RegistrySupplier<Block> ANCIENT_SCRAP = registerBlock("ancient_scrap", () -> new Block(BlockProps.copy(Blocks.STONE)));
    public static final RegistrySupplier<Block> ASH_DIGAMMA = registerBlock("ash_digamma", () -> new Block(BlockProps.copy(Blocks.STONE)));
    public static final RegistrySupplier<Block> ASPHALT_LIGHT = registerBlock("asphalt_light", () -> new Block(BlockProps.copy(Blocks.STONE)));
    public static final RegistrySupplier<Block> BARBED_WIRE_ACID = registerBlock("barbed_wire_acid", () -> new Block(BlockProps.copy(Blocks.STONE)));
    public static final RegistrySupplier<Block> BARBED_WIRE_ULTRADEATH = registerBlock("barbed_wire_ultradeath", () -> new Block(BlockProps.copy(Blocks.STONE)));
    public static final RegistrySupplier<Block> BASALT = registerBlock("basalt", () -> new Block(BlockProps.copy(Blocks.STONE)));
    public static final RegistrySupplier<Block> BASALT_SMOOTH = registerBlock("basalt_smooth", () -> new Block(BlockProps.copy(Blocks.STONE)));
    public static final RegistrySupplier<Block> BASALT_TILES = registerBlock("basalt_tiles", () -> new Block(BlockProps.copy(Blocks.STONE)));
    public static final RegistrySupplier<Block> BATTERY_LITHIUM_BLOCK = registerBlock("battery_lithium_block", () -> new Block(BlockProps.copy(Blocks.STONE)));
    public static final RegistrySupplier<Block> BATTERY_POTATO_BLOCK = registerBlock("battery_potato_block", () -> new Block(BlockProps.copy(Blocks.STONE)));
    public static final RegistrySupplier<Block> BATTERY_SCHRABIDIUM_BLOCK = registerBlock("battery_schrabidium_block", () -> new Block(BlockProps.copy(Blocks.STONE)));
    public static final RegistrySupplier<Block> BLAST_DOOR = registerBlock("blast_door", () -> new Block(BlockProps.copy(Blocks.STONE)));
    public static final RegistrySupplier<Block> BLOCK_ALUMINIUM = registerBlock("block_aluminium", () -> new Block(BlockProps.copy(Blocks.STONE)));
    public static final RegistrySupplier<Block> BOXCAR = registerBlock("boxcar", () -> new Block(BlockProps.copy(Blocks.STONE)));
    public static final RegistrySupplier<Block> BRICK_ASBESTOS = registerBlock("brick_asbestos", () -> new Block(BlockProps.copy(Blocks.STONE)));
    public static final RegistrySupplier<Block> BRICK_COMPOUND = registerBlock("brick_compound", () -> new Block(BlockProps.copy(Blocks.STONE)));
    public static final RegistrySupplier<Block> BRICK_JUNGLE = registerBlock("brick_jungle", () -> new Block(BlockProps.copy(Blocks.STONE)));
    public static final RegistrySupplier<Block> BRICK_JUNGLE_CIRCLE = registerBlock("brick_jungle_circle", () -> new Block(BlockProps.copy(Blocks.STONE)));
    public static final RegistrySupplier<Block> BRICK_JUNGLE_CRACKED = registerBlock("brick_jungle_cracked", () -> new Block(BlockProps.copy(Blocks.STONE)));
    public static final RegistrySupplier<Block> BRICK_JUNGLE_FRAGILE = registerBlock("brick_jungle_fragile", () -> new Block(BlockProps.copy(Blocks.STONE)));
    public static final RegistrySupplier<Block> BRICK_JUNGLE_GLYPH = registerBlock("brick_jungle_glyph", () -> new Block(BlockProps.copy(Blocks.STONE)));
    public static final RegistrySupplier<Block> BRICK_JUNGLE_LAVA = registerBlock("brick_jungle_lava", () -> new Block(BlockProps.copy(Blocks.STONE)));
    public static final RegistrySupplier<Block> BRICK_JUNGLE_MYSTIC = registerBlock("brick_jungle_mystic", () -> new Block(BlockProps.copy(Blocks.STONE)));
    public static final RegistrySupplier<Block> BRICK_JUNGLE_OOZE = registerBlock("brick_jungle_ooze", () -> new Block(BlockProps.copy(Blocks.STONE)));
    public static final RegistrySupplier<Block> BRICK_JUNGLE_TRAP = registerBlock("brick_jungle_trap", () -> new Block(BlockProps.copy(Blocks.STONE)));
    public static final RegistrySupplier<Block> BRICK_RED = registerBlock("brick_red", () -> new Block(BlockProps.copy(Blocks.STONE)));
    public static final RegistrySupplier<Block> BROADCASTER_PC = registerBlock("broadcaster_pc",
            () -> new com.hbm_m.block.machines.BroadcasterPcBlock(BlockProps.copy(Blocks.STONE).noOcclusion()));
    public static final RegistrySupplier<Block> CABLE_DETECTOR = registerBlock("cable_detector", () -> new Block(BlockProps.copy(Blocks.STONE)));
    public static final RegistrySupplier<Block> CABLE_DIODE = registerBlock("cable_diode", () -> new Block(BlockProps.copy(Blocks.STONE)));
    public static final RegistrySupplier<Block> CABLE_SWITCH = registerBlock("cable_switch", () -> new Block(BlockProps.copy(Blocks.STONE)));
    // capacitor_bus/gold/niobium/tantalium/schrabidate are all @Deprecated + hidden from the creative
    // tab in the 1.7.10 original (only capacitor_copper is player-facing); ported for completeness
    // using the shared MachineCapacitorBlock, no bus-chaining mechanic (see MachineCapacitorBlockEntity).
    public static final RegistrySupplier<Block> CAPACITOR_BUS = registerBlock("capacitor_bus",
            () -> new com.hbm_m.block.machines.MachineCapacitorBlock(BlockProps.copy(Blocks.STONE).noOcclusion(), 1_000_000L));
    public static final RegistrySupplier<Block> CAPACITOR_COPPER = registerBlock("capacitor_copper",
            () -> new com.hbm_m.block.machines.MachineCapacitorBlock(BlockProps.copy(Blocks.STONE).noOcclusion(), 1_000_000L));
    public static final RegistrySupplier<Block> CAPACITOR_GOLD = registerBlock("capacitor_gold",
            () -> new com.hbm_m.block.machines.MachineCapacitorBlock(BlockProps.copy(Blocks.STONE).noOcclusion(), 5_000_000L));
    public static final RegistrySupplier<Block> CAPACITOR_NIOBIUM = registerBlock("capacitor_niobium",
            () -> new com.hbm_m.block.machines.MachineCapacitorBlock(BlockProps.copy(Blocks.STONE).noOcclusion(), 25_000_000L));
    public static final RegistrySupplier<Block> CAPACITOR_SCHRABIDATE = registerBlock("capacitor_schrabidate",
            () -> new com.hbm_m.block.machines.MachineCapacitorBlock(BlockProps.copy(Blocks.STONE).noOcclusion(), 50_000_000_000L));
    public static final RegistrySupplier<Block> CAPACITOR_TANTALIUM = registerBlock("capacitor_tantalium",
            () -> new com.hbm_m.block.machines.MachineCapacitorBlock(BlockProps.copy(Blocks.STONE).noOcclusion(), 150_000_000L));
    /** Self-stacking 3x3 elevator shaft; see com.hbm_m.block.machines.CargoElevatorBlock. */
    public static final RegistrySupplier<Block> CARGO_ELEVATOR = registerBlock("cargo_elevator",
            () -> new com.hbm_m.block.machines.CargoElevatorBlock(BlockProps.copy(Blocks.IRON_BLOCK).noOcclusion()));
    public static final RegistrySupplier<Block> CHARGE_C4 = registerBlock("charge_c4", () -> new Block(BlockProps.copy(Blocks.STONE)));
    public static final RegistrySupplier<Block> CHARGE_DYNAMITE = registerBlock("charge_dynamite", () -> new Block(BlockProps.copy(Blocks.STONE)));
    public static final RegistrySupplier<Block> CHARGE_MINER = registerBlock("charge_miner", () -> new Block(BlockProps.copy(Blocks.STONE)));
    public static final RegistrySupplier<Block> CHARGE_SEMTEX = registerBlock("charge_semtex", () -> new Block(BlockProps.copy(Blocks.STONE)));
    public static final RegistrySupplier<Block> CHLORINE_GAS = registerBlock("chlorine_gas", () -> new Block(BlockProps.copy(Blocks.STONE)));
    public static final RegistrySupplier<Block> CLUSTER_ALUMINIUM = registerBlock("cluster_aluminium", () -> new Block(BlockProps.copy(Blocks.STONE)));
    public static final RegistrySupplier<Block> CLUSTER_COPPER = registerBlock("cluster_copper", () -> new Block(BlockProps.copy(Blocks.STONE)));
    public static final RegistrySupplier<Block> CLUSTER_DEPTH_IRON = registerBlock("cluster_depth_iron", () -> new Block(BlockProps.copy(Blocks.STONE)));
    public static final RegistrySupplier<Block> CLUSTER_DEPTH_TITANIUM = registerBlock("cluster_depth_titanium", () -> new Block(BlockProps.copy(Blocks.STONE)));
    public static final RegistrySupplier<Block> CLUSTER_DEPTH_TUNGSTEN = registerBlock("cluster_depth_tungsten", () -> new Block(BlockProps.copy(Blocks.STONE)));
    public static final RegistrySupplier<Block> CLUSTER_IRON = registerBlock("cluster_iron", () -> new Block(BlockProps.copy(Blocks.STONE)));
    public static final RegistrySupplier<Block> CLUSTER_TITANIUM = registerBlock("cluster_titanium", () -> new Block(BlockProps.copy(Blocks.STONE)));
    public static final RegistrySupplier<Block> CM_FLUX = registerBlock("cm_flux", () -> new Block(BlockProps.copy(Blocks.STONE)));
    public static final RegistrySupplier<Block> CM_HEAT = registerBlock("cm_heat", () -> new Block(BlockProps.copy(Blocks.STONE)));
    public static final RegistrySupplier<Block> CMB_BRICK = registerBlock("cmb_brick", () -> new Block(BlockProps.copy(Blocks.STONE)));
    public static final RegistrySupplier<Block> CMB_BRICK_REINFORCED = registerBlock("cmb_brick_reinforced", () -> new Block(BlockProps.copy(Blocks.STONE)));
    public static final RegistrySupplier<Block> COMPACT_LAUNCHER = registerBlock("compact_launcher", () -> new Block(BlockProps.copy(Blocks.STONE)));
    public static final RegistrySupplier<Block> CONCRETE_COLORED_EXT_BRONZE = registerBlock("concrete_colored_ext_bronze", () -> new Block(BlockProps.copy(Blocks.STONE)));
    public static final RegistrySupplier<Block> CONCRETE_COLORED_EXT_HAZARD = registerBlock("concrete_colored_ext_hazard", () -> new Block(BlockProps.copy(Blocks.STONE)));
    public static final RegistrySupplier<Block> CONCRETE_COLORED_EXT_INDIGO = registerBlock("concrete_colored_ext_indigo", () -> new Block(BlockProps.copy(Blocks.STONE)));
    public static final RegistrySupplier<Block> CONCRETE_COLORED_EXT_MACHINE = registerBlock("concrete_colored_ext_machine", () -> new Block(BlockProps.copy(Blocks.STONE)));
    public static final RegistrySupplier<Block> CONCRETE_COLORED_EXT_MACHINE_STRIPE = registerBlock("concrete_colored_ext_machine_stripe", () -> new Block(BlockProps.copy(Blocks.STONE)));
    public static final RegistrySupplier<Block> CONCRETE_COLORED_EXT_PINK = registerBlock("concrete_colored_ext_pink", () -> new Block(BlockProps.copy(Blocks.STONE)));
    public static final RegistrySupplier<Block> CONCRETE_COLORED_EXT_PURPLE = registerBlock("concrete_colored_ext_purple", () -> new Block(BlockProps.copy(Blocks.STONE)));
    public static final RegistrySupplier<Block> CONCRETE_COLORED_EXT_SAND = registerBlock("concrete_colored_ext_sand", () -> new Block(BlockProps.copy(Blocks.STONE)));
    public static final RegistrySupplier<Block> CONVEYOR = registerBlock("conveyor",
            () -> new com.hbm_m.block.network.ConveyorBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(2.0f, 4.0f).sound(SoundType.METAL).noOcclusion()));
    public static final RegistrySupplier<Block> CONVEYOR_DOUBLE = registerBlock("conveyor_double",
            () -> new com.hbm_m.block.network.ConveyorDoubleBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(2.0f, 4.0f).sound(SoundType.METAL).noOcclusion()));
    public static final RegistrySupplier<Block> CONVEYOR_EXPRESS = registerBlock("conveyor_express",
            () -> new com.hbm_m.block.network.ConveyorExpressBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(2.0f, 4.0f).sound(SoundType.METAL).noOcclusion()));
    public static final RegistrySupplier<Block> CONVEYOR_TRIPLE = registerBlock("conveyor_triple",
            () -> new com.hbm_m.block.network.ConveyorTripleBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(2.0f, 4.0f).sound(SoundType.METAL).noOcclusion()));
    public static final RegistrySupplier<Block> CONVEYOR_LIFT = registerBlock("conveyor_lift",
            () -> new com.hbm_m.block.network.ConveyorLiftBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(2.0f, 4.0f).sound(SoundType.METAL).noOcclusion()));
    public static final RegistrySupplier<Block> CONVEYOR_CHUTE = registerBlock("conveyor_chute",
            () -> new com.hbm_m.block.network.ConveyorChuteBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(2.0f, 4.0f).sound(SoundType.METAL).noOcclusion()));
    public static final RegistrySupplier<Block> CRANE_BOXER = registerBlock("crane_boxer",
            () -> new com.hbm_m.block.machines.MachineCraneBoxerBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(4.0f, 4.0f).sound(SoundType.METAL).noOcclusion()));
    public static final RegistrySupplier<Block> CRANE_EXTRACTOR = registerBlock("crane_extractor",
            () -> new com.hbm_m.block.machines.MachineCraneExtractorBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(4.0f, 4.0f).sound(SoundType.METAL).noOcclusion()));
    public static final RegistrySupplier<Block> CRANE_GRABBER = registerBlock("crane_grabber",
            () -> new com.hbm_m.block.machines.MachineCraneGrabberBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(4.0f, 4.0f).sound(SoundType.METAL).noOcclusion()));
    public static final RegistrySupplier<Block> CRANE_INSERTER = registerBlock("crane_inserter",
            () -> new com.hbm_m.block.machines.MachineCraneInserterBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(4.0f, 4.0f).sound(SoundType.METAL).noOcclusion()));
    public static final RegistrySupplier<Block> CRANE_PARTITIONER = registerBlock("crane_partitioner", () -> new Block(BlockProps.copy(Blocks.STONE)));
    public static final RegistrySupplier<Block> CRANE_ROUTER = registerBlock("crane_router",
            () -> new com.hbm_m.block.machines.MachineCraneRouterBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(4.0f, 4.0f).sound(SoundType.METAL).noOcclusion()));
    public static final RegistrySupplier<Block> CRANE_SPLITTER = registerBlock("crane_splitter",
            () -> new com.hbm_m.block.machines.MachineCraneSplitterBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(4.0f, 4.0f).sound(SoundType.METAL).noOcclusion()));
    public static final RegistrySupplier<Block> CRANE_UNBOXER = registerBlock("crane_unboxer",
            () -> new com.hbm_m.block.machines.MachineCraneUnboxerBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(4.0f, 4.0f).sound(SoundType.METAL).noOcclusion()));
    public static final RegistrySupplier<Block> CRATE_AMMO = registerBlock("crate_ammo", () -> new Block(BlockProps.copy(Blocks.STONE)));
    public static final RegistrySupplier<Block> CRATE_CAN = registerBlock("crate_can", () -> new Block(BlockProps.copy(Blocks.STONE)));
    public static final RegistrySupplier<Block> CRATE_JUNGLE = registerBlock("crate_jungle", () -> new Block(BlockProps.copy(Blocks.STONE)));
    public static final RegistrySupplier<Block> CRATE_RED = registerBlock("crate_red", () -> new Block(BlockProps.copy(Blocks.STONE)));
    public static final RegistrySupplier<Block> DECO_ALUMINIUM = registerBlock("deco_aluminium", () -> new Block(BlockProps.copy(Blocks.STONE)));
    public static final RegistrySupplier<Block> DEPTH_DNT = registerBlock("depth_dnt", () -> new Block(BlockProps.copy(Blocks.STONE)));
    public static final RegistrySupplier<Block> DET_CHARGE = registerBlock("det_charge", () -> new Block(BlockProps.copy(Blocks.STONE)));
    public static final RegistrySupplier<Block> DET_CORD = registerBlock("det_cord", () -> new Block(BlockProps.copy(Blocks.STONE)));
    public static final RegistrySupplier<Block> DET_NUKE = registerBlock("det_nuke", () -> new Block(BlockProps.copy(Blocks.STONE)));
    public static final RegistrySupplier<Block> DFC_CORE = registerBlock("dfc_core", () -> new Block(BlockProps.copy(Blocks.STONE)));
    public static final RegistrySupplier<Block> DFC_EMITTER = registerBlock("dfc_emitter", () -> new Block(BlockProps.copy(Blocks.STONE)));
    public static final RegistrySupplier<Block> DFC_INJECTOR = registerBlock("dfc_injector", () -> new Block(BlockProps.copy(Blocks.STONE)));
    public static final RegistrySupplier<Block> DFC_RECEIVER = registerBlock("dfc_receiver", () -> new Block(BlockProps.copy(Blocks.STONE)));
    public static final RegistrySupplier<Block> DFC_STABILIZER = registerBlock("dfc_stabilizer", () -> new Block(BlockProps.copy(Blocks.STONE)));
    public static final RegistrySupplier<Block> DIRT_DEAD = registerBlock("dirt_dead", () -> new Block(BlockProps.copy(Blocks.STONE)));
    public static final RegistrySupplier<Block> DIRT_OILY = registerBlock("dirt_oily", () -> new Block(BlockProps.copy(Blocks.STONE)));
    public static final RegistrySupplier<Block> DRONE_CRATE = registerBlock("drone_crate",
            () -> new com.hbm_m.block.machines.MachineDroneCrateBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(4.0f, 4.0f).sound(SoundType.METAL)));
    public static final RegistrySupplier<Block> DRONE_CRATE_PROVIDER = registerBlock("drone_crate_provider",
            () -> new com.hbm_m.block.machines.MachineDroneProviderBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(4.0f, 4.0f).sound(SoundType.METAL)));
    public static final RegistrySupplier<Block> DRONE_CRATE_REQUESTER = registerBlock("drone_crate_requester",
            () -> new com.hbm_m.block.machines.MachineDroneRequesterBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(4.0f, 4.0f).sound(SoundType.METAL)));
    public static final RegistrySupplier<Block> DRONE_DOCK = registerBlock("drone_dock",
            () -> new com.hbm_m.block.machines.MachineDroneDockBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(4.0f, 4.0f).sound(SoundType.METAL)));
    public static final RegistrySupplier<Block> DRONE_WAYPOINT = registerBlock("drone_waypoint",
            () -> new com.hbm_m.block.machines.MachineDroneWaypointBlock(BlockProps.copy(Blocks.STONE).noOcclusion()));
    public static final RegistrySupplier<Block> DRONE_WAYPOINT_REQUEST = registerBlock("drone_waypoint_request",
            () -> new com.hbm_m.block.machines.MachineDroneWaypointRequestBlock(BlockProps.copy(Blocks.STONE).noOcclusion()));
    // ─── Radio Torch ("RTTY", Redstone Over Radio) family ──────────────────────
    public static final RegistrySupplier<Block> RADIO_TORCH_SENDER = registerBlock("radio_torch_sender",
            () -> new com.hbm_m.block.machines.radio.RadioTorchSenderBlock(BlockProps.copy(Blocks.STONE).noOcclusion()));
    public static final RegistrySupplier<Block> RADIO_TORCH_RECEIVER = registerBlock("radio_torch_receiver",
            () -> new com.hbm_m.block.machines.radio.RadioTorchReceiverBlock(BlockProps.copy(Blocks.STONE).noOcclusion()));
    public static final RegistrySupplier<Block> RADIO_TORCH_LOGIC = registerBlock("radio_torch_logic",
            () -> new com.hbm_m.block.machines.radio.RadioTorchLogicBlock(BlockProps.copy(Blocks.STONE).noOcclusion()));
    public static final RegistrySupplier<Block> RADIO_TORCH_READER = registerBlock("radio_torch_reader",
            () -> new com.hbm_m.block.machines.radio.RadioTorchReaderBlock(BlockProps.copy(Blocks.STONE).noOcclusion()));
    public static final RegistrySupplier<Block> RADIO_TORCH_CONTROLLER = registerBlock("radio_torch_controller",
            () -> new com.hbm_m.block.machines.radio.RadioTorchControllerBlock(BlockProps.copy(Blocks.STONE).noOcclusion()));
    public static final RegistrySupplier<Block> RADIO_TORCH_COUNTER = registerBlock("radio_torch_counter",
            () -> new com.hbm_m.block.machines.radio.RadioTorchCounterBlock(BlockProps.copy(Blocks.STONE).noOcclusion()));

    public static final RegistrySupplier<Block> DUCRETE = registerBlock("ducrete", () -> new Block(BlockProps.copy(Blocks.STONE)));
    public static final RegistrySupplier<Block> DYNAMITE = registerBlock("dynamite", () -> new Block(BlockProps.copy(Blocks.STONE)));
    public static final RegistrySupplier<Block> FACTORY_ADVANCED_HULL = registerBlock("factory_advanced_hull", () -> new Block(BlockProps.copy(Blocks.STONE)));
    public static final RegistrySupplier<Block> FACTORY_TITANIUM_HULL = registerBlock("factory_titanium_hull", () -> new Block(BlockProps.copy(Blocks.STONE)));
    public static final RegistrySupplier<Block> FENCE_METAL = registerBlock("fence_metal", () -> new Block(BlockProps.copy(Blocks.STONE)));
    public static final RegistrySupplier<Block> FENCE_METAL_POST = registerBlock("fence_metal_post", () -> new Block(BlockProps.copy(Blocks.STONE)));
    public static final RegistrySupplier<Block> FIELD_DISTURBER = registerBlock("field_disturber", () -> new Block(BlockProps.copy(Blocks.STONE)));
    public static final RegistrySupplier<Block> FIRE_DIGAMMA = registerBlock("fire_digamma", () -> new Block(BlockProps.copy(Blocks.STONE)));
    public static final RegistrySupplier<Block> FIREWORKS = registerBlock("fireworks", () -> new Block(BlockProps.copy(Blocks.STONE)));
    public static final RegistrySupplier<Block> FISSURE_BOMB = registerBlock("fissure_bomb", () -> new Block(BlockProps.copy(Blocks.STONE)));
    public static final RegistrySupplier<Block> FLAME_WAR = registerBlock("flame_war", () -> new Block(BlockProps.copy(Blocks.STONE)));
    public static final RegistrySupplier<Block> FLUID_COUNTER_VALVE = registerBlock("fluid_counter_valve", () -> new Block(BlockProps.copy(Blocks.STONE)));
    public static final RegistrySupplier<Block> FLUID_DUCT_BOX = registerBlock("fluid_duct_box", () -> new Block(BlockProps.copy(Blocks.STONE)));
    /** Reuses the fluid_duct_box texture, matching the original. */
    public static final RegistrySupplier<Block> FLUID_DUCT_EXHAUST = registerBlock("fluid_duct_exhaust", () -> new Block(BlockProps.copy(Blocks.STONE)));
    public static final RegistrySupplier<Block> FLUID_DUCT_PAINTABLE = registerBlock("fluid_duct_paintable", () -> new Block(BlockProps.copy(Blocks.STONE)));
    /** Reuses the block_steel texture, matching the original. */
    public static final RegistrySupplier<Block> PIPE_ANCHOR = registerBlock("pipe_anchor", () -> new Block(BlockProps.copy(Blocks.STONE)));
    public static final RegistrySupplier<Block> FLUID_SWITCH = registerBlock("fluid_switch", () -> new Block(BlockProps.copy(Blocks.STONE)));
    public static final RegistrySupplier<Block> FOUNDRY_MOLD = registerBlock("foundry_mold",
            () -> new com.hbm_m.block.machines.MachineFoundryMoldBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(4.0f, 4.0f).sound(SoundType.METAL).noOcclusion().isSuffocating((state, world, pos) -> false)));
    public static final RegistrySupplier<Block> FOUNDRY_SLAGTAP = registerBlock("foundry_slagtap",
            () -> new com.hbm_m.block.machines.MachineFoundrySlagtapBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(3.0f, 3.0f).sound(SoundType.METAL).noOcclusion().isSuffocating((state, world, pos) -> false)));
    public static final RegistrySupplier<Block> FOUNDRY_TANK = registerBlock("foundry_tank",
            () -> new com.hbm_m.block.machines.MachineFoundryTankBlock(BlockProps.copy(Blocks.STONE).strength(3.0f, 3.0f).noOcclusion().isSuffocating((state, world, pos) -> false)));
    /**
     * Port of the original's separately-registered dynamic {@code ModBlocks.slag} (molten puddle) -
     * not the same as {@link #BLOCK_SLAG}. No item form: the original has {@code setCreativeTab(null)}
     * (never obtainable as an item, only ever placed by the slagtap).
     */
    public static final RegistrySupplier<Block> SLAG_DYNAMIC = registerBlockWithoutItem("slag",
            () -> new com.hbm_m.block.generic.DynamicSlagBlock(BlockProps.copy(Blocks.STONE).strength(5.0f, 10.0f).noOcclusion()));
    public static final RegistrySupplier<Block> FROZEN_DIRT = registerBlock("frozen_dirt", () -> new Block(BlockProps.copy(Blocks.STONE)));
    public static final RegistrySupplier<Block> FROZEN_GRASS = registerBlock("frozen_grass", () -> new Block(BlockProps.copy(Blocks.STONE)));
    public static final RegistrySupplier<Block> FROZEN_LOG = registerBlock("frozen_log", () -> new Block(BlockProps.copy(Blocks.STONE)));
    public static final RegistrySupplier<Block> FROZEN_PLANKS = registerBlock("frozen_planks", () -> new Block(BlockProps.copy(Blocks.STONE)));
    public static final RegistrySupplier<Block> FUSION_COMPONENT = registerBlock("fusion_component", () -> new Block(BlockProps.copy(Blocks.STONE)));
    public static final RegistrySupplier<Block> FUSION_COMPONENT_BLANKET = registerBlock("fusion_component_blanket", () -> new Block(BlockProps.copy(Blocks.STONE)));
    public static final RegistrySupplier<Block> FUSION_COMPONENT_BSCCO_WELDED = registerBlock("fusion_component_bscco_welded", () -> new Block(BlockProps.copy(Blocks.STONE)));
    public static final RegistrySupplier<Block> FUSION_COMPONENT_MOTOR = registerBlock("fusion_component_motor", () -> new Block(BlockProps.copy(Blocks.STONE)));
    public static final RegistrySupplier<Block> FUSION_HATCH = registerBlock("fusion_hatch", () -> new Block(BlockProps.copy(Blocks.STONE)));
    public static final RegistrySupplier<Block> FUSION_HEATER = registerBlock("fusion_heater", () -> new Block(BlockProps.copy(Blocks.STONE)));
    public static final RegistrySupplier<Block> GAS_ASBESTOS = registerBlock("gas_asbestos", () -> new Block(BlockProps.copy(Blocks.STONE)));
    public static final RegistrySupplier<Block> GAS_COAL = registerBlock("gas_coal", () -> new Block(BlockProps.copy(Blocks.STONE)));
    public static final RegistrySupplier<Block> GAS_EXPLOSIVE = registerBlock("gas_explosive", () -> new Block(BlockProps.copy(Blocks.STONE)));
    public static final RegistrySupplier<Block> GAS_FLAMMABLE = registerBlock("gas_flammable", () -> new Block(BlockProps.copy(Blocks.STONE)));
    public static final RegistrySupplier<Block> GAS_MELTDOWN = registerBlock("gas_meltdown", () -> new Block(BlockProps.copy(Blocks.STONE)));
    public static final RegistrySupplier<Block> GAS_MONOXIDE = registerBlock("gas_monoxide", () -> new Block(BlockProps.copy(Blocks.STONE)));
    public static final RegistrySupplier<Block> GAS_RADON = registerBlock("gas_radon", () -> new Block(BlockProps.copy(Blocks.STONE)));
    public static final RegistrySupplier<Block> GAS_RADON_DENSE = registerBlock("gas_radon_dense", () -> new Block(BlockProps.copy(Blocks.STONE)));
    public static final RegistrySupplier<Block> GAS_RADON_TOMB = registerBlock("gas_radon_tomb", () -> new Block(BlockProps.copy(Blocks.STONE)));
    public static final RegistrySupplier<Block> GLASS_ASH = registerBlock("glass_ash", () -> new Block(BlockProps.copy(Blocks.STONE)));
    public static final RegistrySupplier<Block> GLASS_BORON = registerBlock("glass_boron", () -> new Block(BlockProps.copy(Blocks.STONE)));
    public static final RegistrySupplier<Block> GLASS_LEAD = registerBlock("glass_lead", () -> new Block(BlockProps.copy(Blocks.STONE)));
    public static final RegistrySupplier<Block> GLASS_POLARIZED = registerBlock("glass_polarized", () -> new Block(BlockProps.copy(Blocks.STONE)));
    public static final RegistrySupplier<Block> GLASS_POLONIUM = registerBlock("glass_polonium", () -> new Block(BlockProps.copy(Blocks.STONE)));
    public static final RegistrySupplier<Block> GLASS_QUARTZ = registerBlock("glass_quartz", () -> new Block(BlockProps.copy(Blocks.STONE)));
    public static final RegistrySupplier<Block> GLASS_TRINITITE = registerBlock("glass_trinitite", () -> new Block(BlockProps.copy(Blocks.STONE)));
    public static final RegistrySupplier<Block> GLASS_URANIUM = registerBlock("glass_uranium", () -> new Block(BlockProps.copy(Blocks.STONE)));
    public static final RegistrySupplier<Block> GLYPHID_BASE = registerBlock("glyphid_base", () -> new Block(BlockProps.copy(Blocks.STONE)));
    public static final RegistrySupplier<Block> GRAVEL_DIAMOND = registerBlock("gravel_diamond", () -> new Block(BlockProps.copy(Blocks.STONE)));
    public static final RegistrySupplier<Block> GRAVEL_OBSIDIAN = registerBlock("gravel_obsidian", () -> new Block(BlockProps.copy(Blocks.STONE)));
    public static final RegistrySupplier<Block> HADRON_COIL_ALLOY = registerBlock("hadron_coil_alloy", () -> new Block(BlockProps.copy(Blocks.STONE)));
    public static final RegistrySupplier<Block> HADRON_COIL_CHLOROPHYTE = registerBlock("hadron_coil_chlorophyte", () -> new Block(BlockProps.copy(Blocks.STONE)));
    public static final RegistrySupplier<Block> HADRON_COIL_GOLD = registerBlock("hadron_coil_gold", () -> new Block(BlockProps.copy(Blocks.STONE)));
    public static final RegistrySupplier<Block> HADRON_COIL_MAGTUNG = registerBlock("hadron_coil_magtung", () -> new Block(BlockProps.copy(Blocks.STONE)));
    public static final RegistrySupplier<Block> HADRON_COIL_MESE = registerBlock("hadron_coil_mese", () -> new Block(BlockProps.copy(Blocks.STONE)));
    public static final RegistrySupplier<Block> HADRON_COIL_NEODYMIUM = registerBlock("hadron_coil_neodymium", () -> new Block(BlockProps.copy(Blocks.STONE)));
    public static final RegistrySupplier<Block> HADRON_COIL_SCHRABIDATE = registerBlock("hadron_coil_schrabidate", () -> new Block(BlockProps.copy(Blocks.STONE)));
    public static final RegistrySupplier<Block> HADRON_COIL_SCHRABIDIUM = registerBlock("hadron_coil_schrabidium", () -> new Block(BlockProps.copy(Blocks.STONE)));
    public static final RegistrySupplier<Block> HADRON_COIL_STARMETAL = registerBlock("hadron_coil_starmetal", () -> new Block(BlockProps.copy(Blocks.STONE)));
    public static final RegistrySupplier<Block> HEV_BATTERY = registerBlock("hev_battery",
            () -> new com.hbm_m.block.generic.HevBatteryBlock(BlockProps.copy(Blocks.STONE).noOcclusion().noCollission()));
    public static final RegistrySupplier<Block> ICF_COMPONENT = registerBlock("icf_component", () -> new Block(BlockProps.copy(Blocks.STONE)));
    public static final RegistrySupplier<Block> ICF_COMPONENT_STRUCTURE = registerBlock("icf_component_structure", () -> new Block(BlockProps.copy(Blocks.STONE)));
    public static final RegistrySupplier<Block> ICF_COMPONENT_STRUCTURE_BOLTED = registerBlock("icf_component_structure_bolted", () -> new Block(BlockProps.copy(Blocks.STONE)));
    public static final RegistrySupplier<Block> ICF_COMPONENT_VESSEL = registerBlock("icf_component_vessel", () -> new Block(BlockProps.copy(Blocks.STONE)));
    public static final RegistrySupplier<Block> ICF_COMPONENT_VESSEL_WELDED = registerBlock("icf_component_vessel_welded", () -> new Block(BlockProps.copy(Blocks.STONE)));
    public static final RegistrySupplier<Block> ICF_CONTROLLER = registerBlock("icf_controller", () -> new Block(BlockProps.copy(Blocks.STONE)));
    public static final RegistrySupplier<Block> ITER = registerBlock("iter", () -> new Block(BlockProps.copy(Blocks.STONE)));
    public static final RegistrySupplier<Block> LADDER_ALUMINIUM = registerBlock("ladder_aluminium", () -> new Block(BlockProps.copy(Blocks.STONE)));
    public static final RegistrySupplier<Block> LADDER_COBALT = registerBlock("ladder_cobalt", () -> new Block(BlockProps.copy(Blocks.STONE)));
    public static final RegistrySupplier<Block> LADDER_COPPER = registerBlock("ladder_copper", () -> new Block(BlockProps.copy(Blocks.STONE)));
    public static final RegistrySupplier<Block> LADDER_GOLD = registerBlock("ladder_gold", () -> new Block(BlockProps.copy(Blocks.STONE)));
    public static final RegistrySupplier<Block> LADDER_IRON = registerBlock("ladder_iron", () -> new Block(BlockProps.copy(Blocks.STONE)));
    public static final RegistrySupplier<Block> LADDER_LEAD = registerBlock("ladder_lead", () -> new Block(BlockProps.copy(Blocks.STONE)));
    public static final RegistrySupplier<Block> LADDER_STEEL = registerBlock("ladder_steel", () -> new Block(BlockProps.copy(Blocks.STONE)));
    public static final RegistrySupplier<Block> LADDER_STURDY = registerBlock("ladder_sturdy", () -> new Block(BlockProps.copy(Blocks.STONE)));
    public static final RegistrySupplier<Block> LADDER_TITANIUM = registerBlock("ladder_titanium", () -> new Block(BlockProps.copy(Blocks.STONE)));
    public static final RegistrySupplier<Block> LADDER_TUNGSTEN = registerBlock("ladder_tungsten", () -> new Block(BlockProps.copy(Blocks.STONE)));
    public static final RegistrySupplier<Block> LAMP_DEMON = registerBlock("lamp_demon", () -> new Block(BlockProps.copy(Blocks.STONE)));
    public static final RegistrySupplier<Block> LAMP_TRITIUM_BLUE_OFF = registerBlock("lamp_tritium_blue_off", () -> new Block(BlockProps.copy(Blocks.STONE)));
    public static final RegistrySupplier<Block> LAMP_TRITIUM_BLUE_ON = registerBlock("lamp_tritium_blue_on", () -> new Block(BlockProps.copy(Blocks.STONE)));
    public static final RegistrySupplier<Block> LAMP_TRITIUM_GREEN_OFF = registerBlock("lamp_tritium_green_off", () -> new Block(BlockProps.copy(Blocks.STONE)));
    public static final RegistrySupplier<Block> LAMP_TRITIUM_GREEN_ON = registerBlock("lamp_tritium_green_on", () -> new Block(BlockProps.copy(Blocks.STONE)));
    public static final RegistrySupplier<Block> LIGHTSTONE_BRICKS = registerBlock("lightstone_bricks", () -> new Block(BlockProps.copy(Blocks.STONE)));
    public static final RegistrySupplier<Block> LIGHTSTONE_BRICKS_CHISELED = registerBlock("lightstone_bricks_chiseled", () -> new Block(BlockProps.copy(Blocks.STONE)));
    public static final RegistrySupplier<Block> LIGHTSTONE_CHISELED = registerBlock("lightstone_chiseled", () -> new Block(BlockProps.copy(Blocks.STONE)));
    public static final RegistrySupplier<Block> LIGHTSTONE_TILE = registerBlock("lightstone_tile", () -> new Block(BlockProps.copy(Blocks.STONE)));
    public static final RegistrySupplier<Block> LIGHTSTONE_UNREFINED = registerBlock("lightstone_unrefined", () -> new Block(BlockProps.copy(Blocks.STONE)));
    public static final RegistrySupplier<Block> MACHINE_AUTOCRAFTER = registerBlock("machine_autocrafter", () -> new com.hbm_m.block.machines.MachineAutocrafterBlock(BlockProps.copy(Blocks.STONE)));
    public static final RegistrySupplier<Block> MACHINE_BOILER = registerBlock("machine_boiler", () -> new Block(BlockProps.copy(Blocks.STONE)));
    public static final RegistrySupplier<Block> MACHINE_CENTRIFUGE = registerBlock("machine_centrifuge", () -> new Block(BlockProps.copy(Blocks.STONE)));
    public static final RegistrySupplier<Block> MACHINE_CHUNGUS = registerBlockWithoutItem("machine_chungus",
            () -> new com.hbm_m.block.machines.MachineChungusBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(4.0f, 4.0f).sound(SoundType.METAL).noOcclusion().isSuffocating((state, world, pos) -> false)));
    public static final RegistrySupplier<Block> MACHINE_CONTROLLER = registerBlock("machine_controller", () -> new Block(BlockProps.copy(Blocks.STONE)));
    public static final RegistrySupplier<Block> MACHINE_CONVERTER_HE_RF = registerBlock("machine_converter_he_rf",
            () -> new com.hbm_m.block.machines.MachineConverterHeRfBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(5.0f, 10.0f).noOcclusion()));
    public static final RegistrySupplier<Block> MACHINE_CONVERTER_RF_HE = registerBlock("machine_converter_rf_he",
            () -> new com.hbm_m.block.machines.MachineConverterRfHeBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(5.0f, 10.0f).noOcclusion()));
    public static final RegistrySupplier<Block> MACHINE_CRYSTALLIZER = registerBlock("machine_crystallizer", () -> new Block(BlockProps.copy(Blocks.STONE)));
    public static final RegistrySupplier<Block> MACHINE_DETECTOR = registerBlock("machine_detector", () -> new Block(BlockProps.copy(Blocks.STONE)));
    public static final RegistrySupplier<Block> MACHINE_EPRESS = registerBlock("machine_epress", () -> new Block(BlockProps.copy(Blocks.STONE)));
    public static final RegistrySupplier<Block> MACHINE_FENSU = registerBattery("machine_fensu", Long.MAX_VALUE);
    public static final RegistrySupplier<Block> MACHINE_FLUIDTANK = registerBlock("machine_fluidtank", () -> new Block(BlockProps.copy(Blocks.STONE)));
    public static final RegistrySupplier<Block> MACHINE_FORCEFIELD = registerBlock("machine_forcefield", () -> new Block(BlockProps.copy(Blocks.STONE)));
    public static final RegistrySupplier<Block> MACHINE_FUNNEL = registerBlock("machine_funnel", () -> new com.hbm_m.block.machines.MachineFunnelBlock(BlockProps.copy(Blocks.STONE)));
    public static final RegistrySupplier<Block> MACHINE_GASCENT = registerBlock("machine_gascent", () -> new Block(BlockProps.copy(Blocks.STONE)));
    public static final RegistrySupplier<Block> MACHINE_ICF_PRESS = registerBlock("machine_icf_press", () -> new Block(BlockProps.copy(Blocks.STONE)));
    public static final RegistrySupplier<Block> MACHINE_KEYFORGE = registerBlock("machine_keyforge",
            () -> new com.hbm_m.block.machines.MachineKeyforgeBlock(BlockProps.copy(Blocks.STONE).noOcclusion()));
    public static final RegistrySupplier<Block> MACHINE_LARGE_TURBINE = registerBlock("machine_large_turbine",
            () -> new com.hbm_m.block.machines.MachineLargeTurbineBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(4.0f, 4.0f).sound(SoundType.METAL).noOcclusion()));
    public static final RegistrySupplier<Block> MACHINE_MICROWAVE = registerBlock("machine_microwave", () -> new Block(BlockProps.copy(Blocks.STONE)));
    public static final RegistrySupplier<Block> MACHINE_MINING_LASER = registerBlock("machine_mining_laser", () -> new Block(BlockProps.copy(Blocks.STONE)));
    public static final RegistrySupplier<Block> MACHINE_MISSILE_ASSEMBLY = registerBlock("machine_missile_assembly",
            () -> new com.hbm_m.block.machines.MachineMissileAssemblyBlock(BlockProps.copy(Blocks.STONE)));
    public static final RegistrySupplier<Block> MACHINE_PRESS = registerBlock("machine_press", () -> new Block(BlockProps.copy(Blocks.STONE)));
    /** Rein dekorativ im Original (leerer TE-Stub, keine Fluid-Logik) - siehe {@link #MACHINE_UF6_TANK}. */
    public static final RegistrySupplier<Block> MACHINE_PUF6_TANK = registerBlock("machine_puf6_tank",
            () -> new Block(BlockProps.copy(Blocks.IRON_BLOCK).strength(5.0f, 10.0f).noOcclusion()));
    /** Genuinely missing from the port until now - no RTG (radioisotope thermoelectric generator) existed anywhere. */
    public static final RegistrySupplier<Block> MACHINE_RTG = registerBlock("machine_rtg_grey",
            () -> new com.hbm_m.block.machines.MachineRtgBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(5.0f, 10.0f).sound(SoundType.METAL).noOcclusion()));
    /** Genuinely missing from the port until now - machine_difurnace_off/_extension already exist under
     * blast_furnace/blast_furnace_extension (renamed IDs); only the RTG-heated variant was a real gap. */
    public static final RegistrySupplier<Block> MACHINE_DIFURNACE_RTG = registerBlock("machine_difurnace_rtg_off",
            () -> new com.hbm_m.block.machines.MachineDifurnaceRtgBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(5.0f, 10.0f).sound(SoundType.METAL).noOcclusion()));
    /** Genuinely missing from the port until now. */
    public static final RegistrySupplier<Block> MACHINE_TELEPORTER = registerBlock("machine_teleporter",
            () -> new com.hbm_m.block.machines.MachineTeleporterBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(5.0f, 10.0f).sound(SoundType.METAL).noOcclusion()));
    /** Genuinely missing from the port until now - purely decorative marker, no TileEntity (matches original). */
    public static final RegistrySupplier<Block> TELEANCHOR = registerBlock("teleanchor",
            () -> new Block(BlockProps.copy(Blocks.IRON_BLOCK).strength(5.0f, 10.0f).sound(SoundType.METAL)));
    /** Genuinely missing from the port until now - reuses MachineAdvancedAssemblerBlockEntity wholesale (see class javadoc). */
    public static final RegistrySupplier<Block> MACHINE_PRECASS = registerBlock("machine_precass",
            () -> new com.hbm_m.block.machines.MachinePrecAssBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(5.0f, 30.0f).sound(SoundType.METAL).noOcclusion()));
    /** Genuinely missing from the port until now. */
    public static final RegistrySupplier<Block> MACHINE_DRAIN = registerBlock("machine_drain",
            () -> new com.hbm_m.block.machines.MachineDrainBlock(BlockProps.copy(Blocks.STONE).strength(5.0f, 10.0f).noOcclusion()));
    /** Genuinely missing from the port until now - purely decorative in the original, no TileEntity (see class javadoc). */
    public static final RegistrySupplier<Block> MACHINE_TRANSFORMER = registerBlock("machine_transformer",
            () -> new com.hbm_m.block.generic.MachineTransformerBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(5.0f, 10.0f)));
    /** Genuinely missing from the port until now. */
    public static final RegistrySupplier<Block> MACHINE_FAN = registerBlock("fan",
            () -> new com.hbm_m.block.machines.MachineFanBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(5.0f, 10.0f).sound(SoundType.METAL).noOcclusion()));
    /** Genuinely missing from the port until now. */
    public static final RegistrySupplier<Block> MACHINE_WASTE_DRUM = registerBlock("machine_waste_drum",
            () -> new com.hbm_m.block.machines.MachineWasteDrumBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(5.0f, 10.0f).sound(SoundType.METAL).noOcclusion()));
    public static final RegistrySupplier<Block> MACHINE_RADGEN = registerBlock("machine_radgen",
            () -> new com.hbm_m.block.machines.MachineRadGenBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(5.0f, 10.0f).sound(SoundType.METAL).noOcclusion()));
    public static final RegistrySupplier<Block> MACHINE_REACTOR = registerBlock("machine_reactor", () -> new Block(BlockProps.copy(Blocks.STONE)));
    public static final RegistrySupplier<Block> MACHINE_REACTOR_SMALL = registerBlock("machine_reactor_small", () -> new Block(BlockProps.copy(Blocks.STONE)));
    public static final RegistrySupplier<Block> MACHINE_REFINERY = registerBlock("machine_refinery", () -> new Block(BlockProps.copy(Blocks.STONE)));
    public static final RegistrySupplier<Block> MACHINE_SATLINKER = registerBlock("machine_satlinker",
            () -> new com.hbm_m.block.machines.MachineSatLinkerBlock(BlockProps.copy(Blocks.STONE)));
    public static final RegistrySupplier<Block> MACHINE_SOLAR_BOILER = registerBlock("machine_solar_boiler", () -> new Block(BlockProps.copy(Blocks.STONE)));
    public static final RegistrySupplier<Block> MACHINE_STORAGE_DRUM = registerBlock("machine_storage_drum",
            () -> new com.hbm_m.block.machines.MachineStorageDrumBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(5.0f, 10.0f).noOcclusion()));
    /** Rein dekorativ im Original (leerer TE-Stub, keine Fluid-Logik, custom TESR nur fuer die Wueste-Dungeon-Loot-Raeume). */
    public static final RegistrySupplier<Block> MACHINE_UF6_TANK = registerBlock("machine_uf6_tank",
            () -> new Block(BlockProps.copy(Blocks.IRON_BLOCK).strength(5.0f, 10.0f).noOcclusion()));
    public static final RegistrySupplier<Block> MASS_STORAGE = registerBlock("mass_storage",
            () -> new com.hbm_m.block.machines.MachineMassStorageBlock(BlockProps.copy(Blocks.STONE).noOcclusion()));
    public static final RegistrySupplier<Block> METEOR_SPAWNER = registerBlock("meteor_spawner", () -> new Block(BlockProps.copy(Blocks.STONE)));
    public static final RegistrySupplier<Block> MINE_HE = registerBlock("mine_he", () -> new Block(BlockProps.copy(Blocks.STONE)));
    public static final RegistrySupplier<Block> MINE_NAVAL = registerBlock("mine_naval", () -> new Block(BlockProps.copy(Blocks.STONE)));
    public static final RegistrySupplier<Block> MINE_SHRAP = registerBlock("mine_shrap", () -> new Block(BlockProps.copy(Blocks.STONE)));
    public static final RegistrySupplier<Block> MOON_TURF = registerBlock("moon_turf", () -> new Block(BlockProps.copy(Blocks.STONE)));
    public static final RegistrySupplier<Block> MUSH = registerBlock("mush", () -> new Block(BlockProps.copy(Blocks.STONE)));
    public static final RegistrySupplier<Block> NUKE_FSTBMB = registerBlock("nuke_fstbmb",
            () -> new com.hbm_m.block.bomb.NukeFstbmbBlock(BlockProps.copy(Blocks.STONE).strength(1.5F, 6.0F).noOcclusion()));

    public static final RegistrySupplier<Block> NUKE_CUSTOM = registerBlock("nuke_custom",
            () -> new com.hbm_m.block.bomb.NukeCustomBlock(BlockProps.copy(Blocks.STONE).strength(1.5F, 6.0F).noOcclusion()));

    public static final RegistrySupplier<Block> BOMB_MULTI = registerBlock("bomb_multi",
            () -> new com.hbm_m.block.bomb.BombMultiBlock(BlockProps.copy(Blocks.STONE).strength(1.5F, 6.0F).noOcclusion()));
    public static final RegistrySupplier<Block> NUKE_N2 = registerBlock("nuke_n2",
            () -> new com.hbm_m.block.bomb.NukeN2Block(BlockProps.copy(Blocks.STONE).strength(1.5F, 6.0F).noOcclusion()));
    public static final RegistrySupplier<Block> NUKE_SOLINIUM = registerBlock("nuke_solinium",
            () -> new com.hbm_m.block.bomb.NukeSoliniumBlock(BlockProps.copy(Blocks.STONE).strength(1.5F, 6.0F).noOcclusion()));
    public static final RegistrySupplier<Block> OIL_SPILL = registerBlock("oil_spill", () -> new Block(BlockProps.copy(Blocks.STONE)));
    public static final RegistrySupplier<Block> PEDESTAL = registerBlock("pedestal", () -> new Block(BlockProps.copy(Blocks.STONE)));
    public static final RegistrySupplier<Block> PINK_LOG = registerBlock("pink_log", () -> new Block(BlockProps.copy(Blocks.STONE)));
    public static final RegistrySupplier<Block> PINK_PLANKS = registerBlock("pink_planks", () -> new Block(BlockProps.copy(Blocks.STONE)));
    public static final RegistrySupplier<Block> PLANT_FLOWER_CD0 = registerBlock("plant_flower_cd0", () -> new Block(BlockProps.copy(Blocks.STONE)));
    public static final RegistrySupplier<Block> PLANT_FLOWER_CD1 = registerBlock("plant_flower_cd1", () -> new Block(BlockProps.copy(Blocks.STONE)));
    public static final RegistrySupplier<Block> PLANT_FLOWER_FOXGLOVE = registerBlock("plant_flower_foxglove", () -> new Block(BlockProps.copy(Blocks.STONE)));
    public static final RegistrySupplier<Block> PLANT_FLOWER_NIGHTSHADE = registerBlock("plant_flower_nightshade", () -> new Block(BlockProps.copy(Blocks.STONE)));
    public static final RegistrySupplier<Block> PLANT_FLOWER_TOBACCO = registerBlock("plant_flower_tobacco", () -> new Block(BlockProps.copy(Blocks.STONE)));
    public static final RegistrySupplier<Block> PLANT_FLOWER_WEED = registerBlock("plant_flower_weed", () -> new Block(BlockProps.copy(Blocks.STONE)));
    public static final RegistrySupplier<Block> PLASMA_HEATER = registerBlock("plasma_heater", () -> new Block(BlockProps.copy(Blocks.STONE)));
    public static final RegistrySupplier<Block> PNEUMATIC_TUBE = registerBlock("pneumatic_tube", () -> new Block(BlockProps.copy(Blocks.STONE)));
    public static final RegistrySupplier<Block> PNEUMATIC_TUBE_PAINTABLE = registerBlock("pneumatic_tube_paintable", () -> new Block(BlockProps.copy(Blocks.STONE)));
    public static final RegistrySupplier<Block> PRESS_PREHEATER = registerBlock("press_preheater", () -> new Block(BlockProps.copy(Blocks.STONE)));
    /** Unused now that assembly no longer converts parts into a generic carrier block; see PWRPartBlockEntity. */
    public static final RegistrySupplier<Block> PWR_BLOCK = registerBlock("pwr_block", () -> new Block(BlockProps.copy(Blocks.STONE)));
    public static final RegistrySupplier<Block> PWR_CASING = registerBlock("pwr_casing",
            () -> new com.hbm_m.block.machines.PWRPartBlock(com.hbm_m.blockentity.machines.PWRPartBlockEntity.Kind.CASING, BlockProps.copy(Blocks.IRON_BLOCK)));
    public static final RegistrySupplier<Block> PWR_CHANNEL = registerBlock("pwr_channel",
            () -> new com.hbm_m.block.machines.PWRPartBlock(com.hbm_m.blockentity.machines.PWRPartBlockEntity.Kind.CHANNEL, BlockProps.copy(Blocks.IRON_BLOCK)));
    public static final RegistrySupplier<Block> PWR_CONTROL = registerBlock("pwr_control",
            () -> new com.hbm_m.block.machines.PWRPartBlock(com.hbm_m.blockentity.machines.PWRPartBlockEntity.Kind.CONTROL, BlockProps.copy(Blocks.IRON_BLOCK)));
    /** The reactor's only true machine block (the assembly's controller); see com.hbm_m.blockentity.machines.PWRControllerBlockEntity. */
    public static final RegistrySupplier<Block> PWR_CONTROLLER = registerBlock("pwr_controller",
            () -> new com.hbm_m.block.machines.MachinePWRControllerBlock(BlockProps.copy(Blocks.IRON_BLOCK).noOcclusion()));
    public static final RegistrySupplier<Block> PWR_FUEL = registerBlock("pwr_fuel",
            () -> new com.hbm_m.block.machines.PWRPartBlock(com.hbm_m.blockentity.machines.PWRPartBlockEntity.Kind.FUEL, BlockProps.copy(Blocks.IRON_BLOCK)));
    public static final RegistrySupplier<Block> PWR_HEATEX = registerBlock("pwr_heatex",
            () -> new com.hbm_m.block.machines.PWRPartBlock(com.hbm_m.blockentity.machines.PWRPartBlockEntity.Kind.HEATEX, BlockProps.copy(Blocks.IRON_BLOCK)));
    public static final RegistrySupplier<Block> PWR_HEATSINK = registerBlock("pwr_heatsink",
            () -> new com.hbm_m.block.machines.PWRPartBlock(com.hbm_m.blockentity.machines.PWRPartBlockEntity.Kind.HEATSINK, BlockProps.copy(Blocks.IRON_BLOCK)));
    public static final RegistrySupplier<Block> PWR_NEUTRON_SOURCE = registerBlock("pwr_neutron_source",
            () -> new com.hbm_m.block.machines.PWRPartBlock(com.hbm_m.blockentity.machines.PWRPartBlockEntity.Kind.NEUTRON_SOURCE, BlockProps.copy(Blocks.IRON_BLOCK)));
    public static final RegistrySupplier<Block> PWR_PORT = registerBlock("pwr_port",
            () -> new com.hbm_m.block.machines.PWRPartBlock(com.hbm_m.blockentity.machines.PWRPartBlockEntity.Kind.PORT, BlockProps.copy(Blocks.IRON_BLOCK)));
    public static final RegistrySupplier<Block> PWR_REFLECTOR = registerBlock("pwr_reflector",
            () -> new com.hbm_m.block.machines.PWRPartBlock(com.hbm_m.blockentity.machines.PWRPartBlockEntity.Kind.REFLECTOR, BlockProps.copy(Blocks.IRON_BLOCK)));
    public static final RegistrySupplier<Block> RADIO_TELEX = registerBlock("radio_telex",
            () -> new com.hbm_m.block.network.RadioTelexBlock(BlockProps.copy(Blocks.STONE).noOcclusion()));
    public static final RegistrySupplier<Block> RADIOBOX = registerBlock("radiobox",
            () -> new com.hbm_m.block.machines.RadioboxBlock(BlockProps.copy(Blocks.STONE).noOcclusion()));
    public static final RegistrySupplier<Block> RADIOREC = registerBlock("radiorec",
            () -> new com.hbm_m.block.machines.RadioRecBlock(BlockProps.copy(Blocks.STONE).noOcclusion()));
    public static final RegistrySupplier<Block> RADIO_AUTOCAL = registerBlock("radio_autocal",
            () -> new com.hbm_m.block.network.RadioAutocalBlock(BlockProps.copy(Blocks.STONE).noOcclusion()));
    public static final RegistrySupplier<Block> RAIL_BOOSTER = registerBlock("rail_booster", () -> new Block(BlockProps.copy(Blocks.STONE)));
    public static final RegistrySupplier<Block> RAIL_HIGHSPEED = registerBlock("rail_highspeed", () -> new Block(BlockProps.copy(Blocks.STONE)));
    public static final RegistrySupplier<Block> RAIL_NARROW = registerBlock("rail_narrow", () -> new Block(BlockProps.copy(Blocks.STONE)));
    public static final RegistrySupplier<Block> RAIL_WOOD = registerBlock("rail_wood", () -> new Block(BlockProps.copy(Blocks.STONE)));
    public static final RegistrySupplier<Block> RED_CABLE = registerBlock("red_cable", () -> new Block(BlockProps.copy(Blocks.STONE)));
    public static final RegistrySupplier<Block> RED_CABLE_CLASSIC = registerBlock("red_cable_classic", () -> new Block(BlockProps.copy(Blocks.STONE)));
    /** Reuses the fluid_duct_box texture, matching the original. */
    public static final RegistrySupplier<Block> RED_CABLE_BOX = registerBlock("red_cable_box", () -> new Block(BlockProps.copy(Blocks.STONE)));
    public static final RegistrySupplier<Block> RED_CONNECTOR = registerBlock("red_connector", () -> new Block(BlockProps.copy(Blocks.STONE)));
    /** Reuses the red_connector texture, matching the original (no dedicated texture existed for it either). */
    public static final RegistrySupplier<Block> RED_CONNECTOR_SUPER = registerBlock("red_connector_super", () -> new Block(BlockProps.copy(Blocks.STONE)));
    public static final RegistrySupplier<Block> RED_PYLON = registerBlock("red_pylon", () -> new Block(BlockProps.copy(Blocks.STONE)));
    public static final RegistrySupplier<Block> RED_PYLON_LARGE = registerBlock("red_pylon_large", () -> new Block(BlockProps.copy(Blocks.STONE)));
    /** Both reuse the red_pylon texture, matching the original. */
    public static final RegistrySupplier<Block> RED_PYLON_MEDIUM_WOOD  = registerBlock("red_pylon_medium_wood",  () -> new Block(BlockProps.copy(Blocks.STONE)));
    public static final RegistrySupplier<Block> RED_PYLON_MEDIUM_STEEL = registerBlock("red_pylon_medium_steel", () -> new Block(BlockProps.copy(Blocks.STONE)));
    public static final RegistrySupplier<Block> RED_WIRE_COATED = registerBlock("red_wire_coated", () -> new Block(BlockProps.copy(Blocks.STONE)));
    public static final RegistrySupplier<Block> REINFORCED_BRICK = registerBlock("reinforced_brick", () -> new Block(BlockProps.copy(Blocks.STONE)));
    public static final RegistrySupplier<Block> REINFORCED_DUCRETE = registerBlock("reinforced_ducrete", () -> new Block(BlockProps.copy(Blocks.STONE)));
    public static final RegistrySupplier<Block> REINFORCED_GLASS_PANE = registerBlock("reinforced_glass_pane", () -> new Block(BlockProps.copy(Blocks.STONE)));
    public static final RegistrySupplier<Block> REINFORCED_LAMINATE = registerBlock("reinforced_laminate", () -> new Block(BlockProps.copy(Blocks.STONE)));
    public static final RegistrySupplier<Block> REINFORCED_LAMINATE_PANE = registerBlock("reinforced_laminate_pane", () -> new Block(BlockProps.copy(Blocks.STONE)));
    public static final RegistrySupplier<Block> REINFORCED_LAMP_OFF = registerBlock("reinforced_lamp_off", () -> new Block(BlockProps.copy(Blocks.STONE)));
    public static final RegistrySupplier<Block> REINFORCED_LAMP_ON = registerBlock("reinforced_lamp_on", () -> new Block(BlockProps.copy(Blocks.STONE)));
    public static final RegistrySupplier<Block> REINFORCED_LIGHT = registerBlock("reinforced_light", () -> new Block(BlockProps.copy(Blocks.STONE)));
    public static final RegistrySupplier<Block> REINFORCED_SAND = registerBlock("reinforced_sand", () -> new Block(BlockProps.copy(Blocks.STONE)));
    public static final RegistrySupplier<Block> SAFE = registerBlock("safe", () -> new Block(BlockProps.copy(Blocks.STONE)));
    // sand_mix (orig BlockNTMSand): 1.7.10 metadata-variant falling sand, ported as one block per
    // variant (matching this port's established convention) using vanilla FallingBlock instead of
    // reimplementing the original's fall()/onBlockAdded tick logic (vanilla's is equivalent).
    public static final RegistrySupplier<Block> SAND_BORON = registerBlock("sand_boron", () -> new com.hbm_m.block.generic.BlockHazardFalling(BlockProps.copy(Blocks.SAND)));
    public static final RegistrySupplier<Block> SAND_DIRTY = registerBlock("sand_dirty", () -> new Block(BlockProps.copy(Blocks.STONE)));
    public static final RegistrySupplier<Block> SAND_DIRTY_RED = registerBlock("sand_dirty_red", () -> new Block(BlockProps.copy(Blocks.STONE)));
    public static final RegistrySupplier<Block> SAND_LEAD = registerBlock("sand_lead", () -> new com.hbm_m.block.generic.BlockHazardFalling(BlockProps.copy(Blocks.SAND)));
    public static final RegistrySupplier<Block> SAND_POLONIUM = registerBlock("sand_polonium", () -> new com.hbm_m.block.generic.BlockHazardFalling(BlockProps.copy(Blocks.SAND)));
    public static final RegistrySupplier<Block> SAND_QUARTZ = registerBlock("sand_quartz", () -> new com.hbm_m.block.generic.BlockHazardFalling(BlockProps.copy(Blocks.SAND)));
    public static final RegistrySupplier<Block> SAND_URANIUM = registerBlock("sand_uranium", () -> new com.hbm_m.block.generic.BlockHazardFalling(BlockProps.copy(Blocks.SAND)));
    public static final RegistrySupplier<Block> SANDBAGS = registerBlock("sandbags", () -> new Block(BlockProps.copy(Blocks.STONE)));
    public static final RegistrySupplier<Block> SAT_DOCK = registerBlock("sat_dock", () -> new Block(BlockProps.copy(Blocks.STONE)));
    public static final RegistrySupplier<Block> SAT_FOEQ = registerBlock("sat_foeq", () -> new Block(BlockProps.copy(Blocks.STONE)));
    public static final RegistrySupplier<Block> SAT_SCANNER = registerBlock("sat_scanner", () -> new Block(BlockProps.copy(Blocks.STONE)));
    public static final RegistrySupplier<Block> SEAL_CONTROLLER = registerBlock("seal_controller", () -> new Block(BlockProps.copy(Blocks.STONE)));
    public static final RegistrySupplier<Block> SEAL_FRAME = registerBlock("seal_frame", () -> new Block(BlockProps.copy(Blocks.STONE)));
    public static final RegistrySupplier<Block> SEAL_HATCH = registerBlock("seal_hatch", () -> new Block(BlockProps.copy(Blocks.STONE)));
    public static final RegistrySupplier<Block> SEMTEX = registerBlock("semtex", () -> new Block(BlockProps.copy(Blocks.STONE)));
    public static final RegistrySupplier<Block> SOYUZ_CAPSULE = registerBlock("soyuz_capsule", () -> new Block(BlockProps.copy(Blocks.STONE)));
    /** Dekorative Soyuz-Startrampe (6 OBJ-Teile, siehe models/block/soyuz_launcher.json) - platzierbar, ohne Spiellogik.
     *  Rendert ueber BlockEntityRenderer (SoyuzLauncherRenderer), da die Tuerme ueber 60 Bloecke hoch sind
     *  und damit die 16-Bit-Chunk-Mesh-Grenze eines normalen Block-Modells sprengen wuerden. */
    public static final RegistrySupplier<Block> SOYUZ_LAUNCHER = registerBlock("soyuz_launcher",
            () -> new com.hbm_m.block.decorations.SoyuzLauncherBlock(BlockProps.copy(Blocks.STONE).noOcclusion()));
    /** Dekorative Soyuz-Rakete (soyuz.obj, Multi-Material) - platzierbar, ohne Spiellogik.
     *  Rendert ueber BlockEntityRenderer (SoyuzRocketRenderer), da das Modell ueber 50 Bloecke hoch ist
     *  und damit die 16-Bit-Chunk-Mesh-Grenze eines normalen Block-Modells sprengen wuerde. */
    public static final RegistrySupplier<Block> DECO_SOYUZ_ROCKET = registerBlock("deco_soyuz_rocket",
            () -> new com.hbm_m.block.decorations.SoyuzRocketBlock(BlockProps.copy(Blocks.STONE).noOcclusion()));
    public static final RegistrySupplier<Block> SPIKES = registerBlock("spikes", () -> new Block(BlockProps.copy(Blocks.STONE)));
    public static final RegistrySupplier<Block> STALACTITE_ASBESTOS = registerBlock("stalactite_asbestos", () -> new Block(BlockProps.copy(Blocks.STONE)));
    public static final RegistrySupplier<Block> STALACTITE_SULFUR = registerBlock("stalactite_sulfur", () -> new Block(BlockProps.copy(Blocks.STONE)));
    public static final RegistrySupplier<Block> STALAGMITE_ASBESTOS = registerBlock("stalagmite_asbestos", () -> new Block(BlockProps.copy(Blocks.STONE)));
    public static final RegistrySupplier<Block> STALAGMITE_SULFUR = registerBlock("stalagmite_sulfur", () -> new Block(BlockProps.copy(Blocks.STONE)));
    public static final RegistrySupplier<Block> STEEL_ROOF = registerBlock("steel_roof", () -> new Block(BlockProps.copy(Blocks.STONE)));
    public static final RegistrySupplier<Block> STEEL_SCAFFOLD = registerBlock("steel_scaffold", () -> new Block(BlockProps.copy(Blocks.STONE)));
    public static final RegistrySupplier<Block> STONE_CRACKED = registerBlock("stone_cracked", () -> new Block(BlockProps.copy(Blocks.STONE)));
    public static final RegistrySupplier<Block> STONE_DEPTH = registerBlock("stone_depth", () -> new Block(BlockProps.copy(Blocks.STONE)));
    public static final RegistrySupplier<Block> STONE_DEPTH_NETHER = registerBlock("stone_depth_nether", () -> new Block(BlockProps.copy(Blocks.STONE)));
    public static final RegistrySupplier<Block> STONE_GNEISS = registerBlock("stone_gneiss", () -> new Block(BlockProps.copy(Blocks.STONE)));
    public static final RegistrySupplier<Block> STONE_KEYHOLE = registerBlock("stone_keyhole", () -> new Block(BlockProps.copy(Blocks.STONE)));
    public static final RegistrySupplier<Block> STONE_KEYHOLE_META = registerBlock("stone_keyhole_meta", () -> new Block(BlockProps.copy(Blocks.STONE)));
    public static final RegistrySupplier<Block> STONE_POROUS = registerBlock("stone_porous", () -> new Block(BlockProps.copy(Blocks.STONE)));
    public static final RegistrySupplier<Block> STONE_RESOURCE_ASBESTOS = registerBlock("stone_resource_asbestos", () -> new Block(BlockProps.copy(Blocks.STONE)));
    public static final RegistrySupplier<Block> STONE_RESOURCE_BAUXITE = registerBlock("stone_resource_bauxite", () -> new Block(BlockProps.copy(Blocks.STONE)));
    public static final RegistrySupplier<Block> STONE_RESOURCE_HEMATITE = registerBlock("stone_resource_hematite", () -> new Block(BlockProps.copy(Blocks.STONE)));
    public static final RegistrySupplier<Block> STONE_RESOURCE_LIMESTONE = registerBlock("stone_resource_limestone", () -> new Block(BlockProps.copy(Blocks.STONE)));
    public static final RegistrySupplier<Block> STONE_RESOURCE_MALACHITE = registerBlock("stone_resource_malachite", () -> new Block(BlockProps.copy(Blocks.STONE)));
    public static final RegistrySupplier<Block> STONE_RESOURCE_SULFUR = registerBlock("stone_resource_sulfur", () -> new Block(BlockProps.copy(Blocks.STONE)));
    public static final RegistrySupplier<Block> STRUCT_ICF_CORE = registerBlock("struct_icf_core", () -> new Block(BlockProps.copy(Blocks.STONE)));
    public static final RegistrySupplier<Block> STRUCT_LAUNCHER = registerBlock("struct_launcher", () -> new Block(BlockProps.copy(Blocks.STONE)));
    public static final RegistrySupplier<Block> STRUCT_LAUNCHER_CORE = registerBlock("struct_launcher_core", () -> new Block(BlockProps.copy(Blocks.STONE)));
    public static final RegistrySupplier<Block> STRUCT_LAUNCHER_CORE_LARGE = registerBlock("struct_launcher_core_large", () -> new Block(BlockProps.copy(Blocks.STONE)));
    public static final RegistrySupplier<Block> STRUCT_SCAFFOLD = registerBlock("struct_scaffold", () -> new Block(BlockProps.copy(Blocks.STONE)));
    public static final RegistrySupplier<Block> STRUCT_SOYUZ_CORE = registerBlock("struct_soyuz_core", () -> new Block(BlockProps.copy(Blocks.STONE)));
    public static final RegistrySupplier<Block> STRUCT_TORUS_CORE = registerBlock("struct_torus_core", () -> new Block(BlockProps.copy(Blocks.STONE)));
    public static final RegistrySupplier<Block> STRUCT_WATZ_CORE = registerBlock("struct_watz_core", () -> new Block(BlockProps.copy(Blocks.STONE)));
    /** Decorative casing end-cap; original toggled bolted/unbolted via screwdriver, ported here as two plain block variants. */
    public static final RegistrySupplier<Block> WATZ_END = registerBlock("watz_end", () -> new Block(BlockProps.copy(Blocks.STONE)));
    public static final RegistrySupplier<Block> WATZ_END_BOLTED = registerBlock("watz_end_bolted", () -> new Block(BlockProps.copy(Blocks.STONE)));
    public static final RegistrySupplier<Block> TEKTITE = registerBlock("tektite", () -> new Block(BlockProps.copy(Blocks.STONE)));
    public static final RegistrySupplier<Block> TESLA = registerBlock("tesla", () -> new Block(BlockProps.copy(Blocks.STONE)));
    public static final RegistrySupplier<Block> THERM_ENDO = registerBlock("therm_endo", () -> new Block(BlockProps.copy(Blocks.STONE)));
    public static final RegistrySupplier<Block> THERM_EXO = registerBlock("therm_exo", () -> new Block(BlockProps.copy(Blocks.STONE)));
    public static final RegistrySupplier<Block> TILE_LAB = registerBlock("tile_lab", () -> new Block(BlockProps.copy(Blocks.STONE)));
    public static final RegistrySupplier<Block> TILE_LAB_BROKEN = registerBlock("tile_lab_broken", () -> new Block(BlockProps.copy(Blocks.STONE)));
    public static final RegistrySupplier<Block> TILE_LAB_CRACKED = registerBlock("tile_lab_cracked", () -> new Block(BlockProps.copy(Blocks.STONE)));
    public static final RegistrySupplier<Block> TRAPDOOR_STEEL = registerBlock("trapdoor_steel", () -> new Block(BlockProps.copy(Blocks.STONE)));
    public static final RegistrySupplier<Block> VACUUM = registerBlock("vacuum", () -> new Block(BlockProps.copy(Blocks.STONE)));
    public static final RegistrySupplier<Block> VENT_CHLORINE = registerBlock("vent_chlorine", () -> new Block(BlockProps.copy(Blocks.STONE)));
    public static final RegistrySupplier<Block> VENT_CHLORINE_SEAL = registerBlock("vent_chlorine_seal", () -> new Block(BlockProps.copy(Blocks.STONE)));
    public static final RegistrySupplier<Block> VENT_CLOUD = registerBlock("vent_cloud", () -> new Block(BlockProps.copy(Blocks.STONE)));
    public static final RegistrySupplier<Block> VENT_PINK_CLOUD = registerBlock("vent_pink_cloud", () -> new Block(BlockProps.copy(Blocks.STONE)));
    public static final RegistrySupplier<Block> VINE_PHOSPHOR = registerBlock("vine_phosphor", () -> new Block(BlockProps.copy(Blocks.STONE)));
    public static final RegistrySupplier<Block> VINYL_TILE_LARGE = registerBlock("vinyl_tile_large", () -> new Block(BlockProps.copy(Blocks.STONE)));
    public static final RegistrySupplier<Block> VOLCANO_CORE = registerBlock("volcano_core", () -> new Block(BlockProps.copy(Blocks.STONE)));
    public static final RegistrySupplier<Block> VOLCANO_RAD_CORE = registerBlock("volcano_rad_core", () -> new Block(BlockProps.copy(Blocks.STONE)));
    public static final RegistrySupplier<Block> WAND_AIR = registerBlock("wand_air", () -> new Block(BlockProps.copy(Blocks.STONE)));
    public static final RegistrySupplier<Block> WAND_JIGSAW = registerBlock("wand_jigsaw", () -> new Block(BlockProps.copy(Blocks.STONE)));
    public static final RegistrySupplier<Block> WAND_LOGIC = registerBlock("wand_logic", () -> new Block(BlockProps.copy(Blocks.STONE)));
    public static final RegistrySupplier<Block> WAND_LOOT = registerBlock("wand_loot", () -> new Block(BlockProps.copy(Blocks.STONE)));
    public static final RegistrySupplier<Block> WASTE_EARTH = registerBlock("waste_earth", () -> new Block(BlockProps.copy(Blocks.STONE)));
    public static final RegistrySupplier<Block> WATZ_COOLER = registerBlock("watz_cooler", () -> new Block(BlockProps.copy(Blocks.STONE)));
    public static final RegistrySupplier<Block> WATZ_ELEMENT = registerBlock("watz_element", () -> new Block(BlockProps.copy(Blocks.STONE)));
    public static final RegistrySupplier<Block> WOOD_BARRIER = registerBlock("wood_barrier", () -> new Block(BlockProps.copy(Blocks.STONE)));

    // --- WIP Machines (3D OBJ models) ---
    public static final RegistrySupplier<Block> AMMO_PRESS = registerBlock("ammo_press",
            () -> new com.hbm_m.block.machines.MachineAmmoPressBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(4.0f, 4.0f).sound(SoundType.METAL).noOcclusion()));

    public static final RegistrySupplier<Block> ANNIHILATOR = registerBlock("annihilator",
            () -> new com.hbm_m.block.machines.MachineAnnihilatorBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(4.0f, 4.0f).sound(SoundType.METAL).noOcclusion()));
    public static final RegistrySupplier<Block> ARC_FURNACE = registerBlock("arc_furnace",
            () -> new com.hbm_m.block.machines.MachineArcFurnaceBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(4.0f, 4.0f).sound(SoundType.METAL).noOcclusion()));

    public static final RegistrySupplier<Block> ASSEMBLY_FACTORY = registerBlock("assembly_factory",
            () -> new Block(BlockProps.copy(Blocks.IRON_BLOCK).strength(4.0f, 4.0f).sound(SoundType.METAL).noOcclusion()));

    public static final RegistrySupplier<Block> AUTOSAW = registerBlock("autosaw",
            () -> new com.hbm_m.block.machines.MachineAutosawBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(4.0f, 4.0f).sound(SoundType.METAL).noOcclusion()));
    public static final RegistrySupplier<Block> BAT9000 = registerBlockWithoutItem("bat9000",
            () -> new MachineBat9000Block(BlockProps.copy(Blocks.IRON_BLOCK).strength(4.0f).requiresCorrectToolForDrops().noOcclusion().isSuffocating((state, world, pos) -> false)));

    public static final RegistrySupplier<Block> BEAMLINE = registerBlock("beamline",
            () -> new Block(BlockProps.copy(Blocks.IRON_BLOCK).strength(4.0f, 4.0f).sound(SoundType.METAL).noOcclusion()));

    public static final RegistrySupplier<Block> BOILER = registerBlock("boiler",
            () -> new com.hbm_m.block.machines.MachineBoilerBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(4.0f, 4.0f).sound(SoundType.METAL).noOcclusion()));

    public static final RegistrySupplier<Block> PUMP_STEAM = registerBlock("pump_steam",
            () -> new com.hbm_m.block.machines.MachinePumpBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(4.0f, 4.0f).sound(SoundType.METAL).noOcclusion(), false));

    public static final RegistrySupplier<Block> PUMP_ELECTRIC = registerBlock("pump_electric",
            () -> new com.hbm_m.block.machines.MachinePumpBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(4.0f, 4.0f).sound(SoundType.METAL).noOcclusion(), true));
    public static final RegistrySupplier<Block> BOILER_FUSION = registerBlock("boiler_fusion",
            () -> new Block(BlockProps.copy(Blocks.IRON_BLOCK).strength(4.0f, 4.0f).sound(SoundType.METAL).noOcclusion()));

    public static final RegistrySupplier<Block> BREEDER_FUSION = registerBlock("breeder_fusion",
            () -> new Block(BlockProps.copy(Blocks.IRON_BLOCK).strength(4.0f, 4.0f).sound(SoundType.METAL).noOcclusion()));

    public static final RegistrySupplier<Block> CHIMNEY_BRICK = registerBlock("chimney_brick",
            () -> new com.hbm_m.block.machines.MachineChimneyBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(4.0f, 4.0f).sound(SoundType.METAL).noOcclusion(), 12));

    public static final RegistrySupplier<Block> CHIMNEY_INDUSTRIAL = registerBlock("chimney_industrial",
            () -> new com.hbm_m.block.machines.MachineChimneyBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(4.0f, 4.0f).sound(SoundType.METAL).noOcclusion(), 22));

    public static final RegistrySupplier<Block> COKER = registerBlock("coker",
            () -> new com.hbm_m.block.machines.MachineCokerBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(4.0f, 4.0f).sound(SoundType.METAL).noOcclusion()));
    public static final RegistrySupplier<Block> COLLECTOR = registerBlock("collector",
            () -> new Block(BlockProps.copy(Blocks.IRON_BLOCK).strength(4.0f, 4.0f).sound(SoundType.METAL).noOcclusion()));

    public static final RegistrySupplier<Block> COMBINATION_OVEN = registerBlock("combination_oven",
            () -> new MachineCombinationOvenBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(4.0f, 4.0f).sound(SoundType.METAL).noOcclusion()));

    public static final RegistrySupplier<Block> COMBUSTION_ENGINE = registerBlock("combustion_engine",
            () -> new com.hbm_m.block.machines.MachineCombustionEngineBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(4.0f, 4.0f).sound(SoundType.METAL).noOcclusion()));

    public static final RegistrySupplier<Block> COMPRESSOR = registerBlock("compressor",
            () -> new com.hbm_m.block.machines.MachineCompressorBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(4.0f, 4.0f).sound(SoundType.METAL).noOcclusion()));
    /** Genuinely missing from the port until now - identical logic to {@link #COMPRESSOR}, the original's
     * "compact" variant only differed in multiblock footprint/visuals, which this single-block port already lacks. */
    public static final RegistrySupplier<Block> MACHINE_COMPRESSOR_COMPACT = registerBlock("machine_compressor_compact",
            () -> new com.hbm_m.block.machines.MachineCompressorBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(4.0f, 4.0f).sound(SoundType.METAL).noOcclusion()));

    public static final RegistrySupplier<Block> PUREX = registerBlock("purex",
            () -> new com.hbm_m.block.machines.MachinePUREXBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(4.0f, 4.0f).sound(SoundType.METAL).noOcclusion()));

    public static final RegistrySupplier<Block> INDUSTRIAL_GENERATOR = registerBlock("industrial_generator",
            () -> new com.hbm_m.block.machines.MachineIndustrialGeneratorBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(4.0f, 4.0f).sound(SoundType.METAL).noOcclusion()));

    public static final RegistrySupplier<Block> STEAM_ENGINE = registerBlock("steam_engine",
            () -> new com.hbm_m.block.machines.MachineSteamEngineBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(4.0f, 4.0f).sound(SoundType.METAL).noOcclusion()));

    public static final RegistrySupplier<Block> CONDENSER_POWERED = registerBlock("condenser_powered",
            () -> new com.hbm_m.block.machines.MachineCondenserPoweredBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(4.0f, 4.0f).sound(SoundType.METAL).noOcclusion()));

    public static final RegistrySupplier<Block> LPW2 = registerBlock("lpw2",
            () -> new com.hbm_m.block.machines.MachineLpw2Block(BlockProps.copy(Blocks.IRON_BLOCK).strength(4.0f, 4.0f).sound(SoundType.METAL).noOcclusion()));

    public static final RegistrySupplier<Block> CONVEYOR_PRESS = registerBlock("conveyor_press",
            () -> new com.hbm_m.block.machines.MachineConveyorPressBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(4.0f, 4.0f).sound(SoundType.METAL).noOcclusion()));
    public static final RegistrySupplier<Block> COUPLER = registerBlock("coupler",
            () -> new Block(BlockProps.copy(Blocks.IRON_BLOCK).strength(4.0f, 4.0f).sound(SoundType.METAL).noOcclusion()));

    public static final RegistrySupplier<Block> DETECTOR = registerBlock("detector",
            () -> new Block(BlockProps.copy(Blocks.IRON_BLOCK).strength(4.0f, 4.0f).sound(SoundType.METAL).noOcclusion()));

    public static final RegistrySupplier<Block> DIESELGEN = registerBlock("dieselgen",
            () -> new com.hbm_m.block.machines.MachineDieselGeneratorBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(4.0f, 4.0f).sound(SoundType.METAL).noOcclusion()));
    public static final RegistrySupplier<Block> DIPOLE = registerBlock("dipole",
            () -> new Block(BlockProps.copy(Blocks.IRON_BLOCK).strength(4.0f, 4.0f).sound(SoundType.METAL).noOcclusion()));

    public static final RegistrySupplier<Block> DRONE = registerBlock("drone",
            () -> new Block(BlockProps.copy(Blocks.IRON_BLOCK).strength(4.0f, 4.0f).sound(SoundType.METAL).noOcclusion()));

    public static final RegistrySupplier<Block> ELECTRIC_FURNACE = registerBlock("electric_furnace",
            () -> new MachineElectricFurnaceBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(4.0f, 4.0f).sound(SoundType.METAL).noOcclusion()));

    public static final RegistrySupplier<Block> ELECTRIC_HEATER = registerBlock("electric_heater",
            () -> new com.hbm_m.block.machines.MachineElectricHeaterBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(4.0f, 4.0f).sound(SoundType.METAL).noOcclusion()));

    public static final RegistrySupplier<Block> ELECTROLYSER = registerBlock("electrolyser",
            () -> new com.hbm_m.block.machines.MachineElectrolyserBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(4.0f, 4.0f).sound(SoundType.METAL).noOcclusion()));

    public static final RegistrySupplier<Block> EPRESS = registerBlock("epress",
            () -> new com.hbm_m.block.machines.MachineEPressBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(4.0f, 4.0f).sound(SoundType.METAL).noOcclusion()));

    public static final RegistrySupplier<Block> EXPOSURE_CHAMBER = registerBlock("exposure_chamber",
            () -> new com.hbm_m.block.machines.MachineExposureChamberBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(4.0f, 4.0f).sound(SoundType.METAL).noOcclusion()));
    public static final RegistrySupplier<Block> FENSU = registerBlock("fensu",
            () -> new Block(BlockProps.copy(Blocks.IRON_BLOCK).strength(4.0f, 4.0f).sound(SoundType.METAL).noOcclusion()));

    /** Ursprungs-ID war "fensu2" - entspricht im Original tatsaechlich {@code TileEntityBatteryREDD} (Reddendite-Batterie),
     * nicht einer zweiten FENSU-Stufe (Namensverwechslung im Asset-Datensatz, siehe Recherche). */
    public static final RegistrySupplier<Block> FENSU2 = registerBattery("machine_battery_redd", Long.MAX_VALUE);

    public static final RegistrySupplier<Block> FIREBOX = registerBlock("firebox",
            () -> new com.hbm_m.block.machines.MachineFireboxBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(4.0f, 4.0f).sound(SoundType.METAL).noOcclusion()));
    public static final RegistrySupplier<Block> FRACTION_SPACER = registerBlock("fraction_spacer",
            () -> new Block(BlockProps.copy(Blocks.IRON_BLOCK).strength(4.0f, 4.0f).sound(SoundType.METAL).noOcclusion()));

    public static final RegistrySupplier<Block> FURNACE_BRICK = registerBlock("furnace_brick",
            () -> new MachineFurnaceBrickBlock(BlockProps.copy(Blocks.BRICKS).strength(4.0f, 4.0f).sound(SoundType.STONE).noOcclusion()));

    public static final RegistrySupplier<Block> FURNACE_IRON = registerBlock("furnace_iron",
            () -> new MachineFurnaceIronBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(4.0f, 4.0f).sound(SoundType.METAL).noOcclusion()));

    public static final RegistrySupplier<Block> FURNACE_STEEL = registerBlock("furnace_steel",
            () -> new MachineFurnaceSteelBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(4.0f, 4.0f).sound(SoundType.METAL).noOcclusion()));

    public static final RegistrySupplier<Block> HEATEX = registerBlock("heatex",
            () -> new com.hbm_m.block.machines.MachineHeatexBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(4.0f, 4.0f).sound(SoundType.METAL).noOcclusion()));

    public static final RegistrySupplier<Block> HEPHAESTUS = registerBlock("hephaestus",
            () -> new com.hbm_m.block.machines.MachineHephaestusBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(4.0f, 4.0f).sound(SoundType.METAL).noOcclusion()));
    public static final RegistrySupplier<Block> ICF = registerBlock("icf",
            () -> new Block(BlockProps.copy(Blocks.IRON_BLOCK).strength(4.0f, 4.0f).sound(SoundType.METAL).noOcclusion()));

    public static final RegistrySupplier<Block> INTAKE = registerBlock("intake",
            () -> new Block(BlockProps.copy(Blocks.IRON_BLOCK).strength(4.0f, 4.0f).sound(SoundType.METAL).noOcclusion()));

    public static final RegistrySupplier<Block> KLYSTRON = registerBlock("klystron",
            () -> new Block(BlockProps.copy(Blocks.IRON_BLOCK).strength(4.0f, 4.0f).sound(SoundType.METAL).noOcclusion()));

    public static final RegistrySupplier<Block> MHDT = registerBlock("mhdt",
            () -> new Block(BlockProps.copy(Blocks.IRON_BLOCK).strength(4.0f, 4.0f).sound(SoundType.METAL).noOcclusion()));

    public static final RegistrySupplier<Block> MICROWAVE = registerBlock("microwave",
            () -> new com.hbm_m.block.machines.MachineMicrowaveBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(4.0f, 4.0f).sound(SoundType.METAL).noOcclusion()));

    public static final RegistrySupplier<Block> MINING_LASER = registerBlock("mining_laser",
            () -> new com.hbm_m.block.machines.MachineMiningLaserBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(4.0f, 4.0f).sound(SoundType.METAL).noOcclusion()));

    public static final RegistrySupplier<Block> OILBURNER = registerBlock("oilburner",
            () -> new com.hbm_m.block.machines.MachineOilburnerBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(4.0f, 4.0f).sound(SoundType.METAL).noOcclusion()));

    public static final RegistrySupplier<Block> OILBURNER_HP = registerBlock("oilburner_hp",
            () -> new com.hbm_m.block.machines.MachineOilburnerBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(4.0f, 4.0f).sound(SoundType.METAL).noOcclusion()));

    public static final RegistrySupplier<Block> ORBUS = registerBlock("orbus",
            () -> new com.hbm_m.block.machines.BarrelTankBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(4.0f, 4.0f).sound(SoundType.METAL).noOcclusion(),
                    com.hbm_m.blockentity.machines.OrbusBlockEntity::new,
                    () -> com.hbm_m.blockentity.ModBlockEntities.ORBUS_BE.get()));
    public static final RegistrySupplier<Block> ORE_SLOPPER = registerBlock("ore_slopper",
            () -> new MachineOreSlopperBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(4.0f, 4.0f).sound(SoundType.METAL).noOcclusion()));

    public static final RegistrySupplier<Block> PLASMA_FORGE = registerBlock("plasma_forge",
            () -> new Block(BlockProps.copy(Blocks.IRON_BLOCK).strength(4.0f, 4.0f).sound(SoundType.METAL).noOcclusion()));

    public static final RegistrySupplier<Block> PYROOVEN = registerBlock("pyrooven",
            () -> new com.hbm_m.block.machines.MachinePyroOvenBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(4.0f, 4.0f).sound(SoundType.METAL).noOcclusion()));
    public static final RegistrySupplier<Block> QUADRUPOLE = registerBlock("quadrupole",
            () -> new Block(BlockProps.copy(Blocks.IRON_BLOCK).strength(4.0f, 4.0f).sound(SoundType.METAL).noOcclusion()));

    public static final RegistrySupplier<Block> RADGEN = registerBlock("radgen",
            () -> new Block(BlockProps.copy(Blocks.IRON_BLOCK).strength(4.0f, 4.0f).sound(SoundType.METAL).noOcclusion()));

    public static final RegistrySupplier<Block> RADIOLYSIS = registerBlock("radiolysis",
            () -> new com.hbm_m.block.machines.MachineRadiolysisBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(4.0f, 4.0f).sound(SoundType.METAL).noOcclusion()));
    public static final RegistrySupplier<Block> REACTOR_SMALL = registerBlock("reactor_small",
            () -> new Block(BlockProps.copy(Blocks.IRON_BLOCK).strength(4.0f, 4.0f).sound(SoundType.METAL).noOcclusion()));

    public static final RegistrySupplier<Block> RFC = registerBlock("rfc",
            () -> new Block(BlockProps.copy(Blocks.IRON_BLOCK).strength(4.0f, 4.0f).sound(SoundType.METAL).noOcclusion()));

    public static final RegistrySupplier<Block> ROTARY_FURNACE = registerBlock("rotary_furnace",
            () -> new com.hbm_m.block.machines.MachineRotaryFurnaceBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(4.0f, 4.0f).sound(SoundType.METAL).noOcclusion()));

    public static final RegistrySupplier<Block> SAWMILL = registerBlock("sawmill",
            () -> new com.hbm_m.block.machines.MachineSawmillBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(4.0f, 4.0f).sound(SoundType.METAL).noOcclusion()));

    public static final RegistrySupplier<Block> THRESHER = registerBlock("thresher",
            () -> new com.hbm_m.block.machines.MachineThresherBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(4.0f, 4.0f).sound(SoundType.METAL).noOcclusion()));

    public static final RegistrySupplier<Block> SOLIDIFIER = registerBlock("solidifier",
            () -> new com.hbm_m.block.machines.MachineSolidifierBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(4.0f, 4.0f).sound(SoundType.METAL).noOcclusion()));

    public static final RegistrySupplier<Block> ASHPIT = registerBlockWithoutItem("ashpit",
            () -> new com.hbm_m.block.machines.MachineAshpitBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(4.0f, 4.0f).sound(SoundType.METAL).noOcclusion()));
    public static final RegistrySupplier<Block> SOURCE = registerBlock("source",
            () -> new Block(BlockProps.copy(Blocks.IRON_BLOCK).strength(4.0f, 4.0f).sound(SoundType.METAL).noOcclusion()));

    public static final RegistrySupplier<Block> REACTOR_RESEARCH = registerBlockWithoutItem("reactor_research",
            () -> new com.hbm_m.block.machines.MachineReactorResearchBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(5.0f, 10.0f).sound(SoundType.METAL).noOcclusion()));

    public static final RegistrySupplier<Block> STIRLING = registerBlock("stirling",
            () -> new com.hbm_m.block.machines.MachineStirlingBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(4.0f, 4.0f).sound(SoundType.METAL).noOcclusion()));

    public static final RegistrySupplier<Block> STIRLING_CREATIVE = registerBlock("stirling_creative",
            () -> new com.hbm_m.block.machines.MachineStirlingBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(4.0f, 4.0f).sound(SoundType.METAL).noOcclusion()));

    public static final RegistrySupplier<Block> STIRLING_STEEL = registerBlock("stirling_steel",
            () -> new com.hbm_m.block.machines.MachineStirlingBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(4.0f, 4.0f).sound(SoundType.METAL).noOcclusion()));

    public static final RegistrySupplier<Block> STRAND_CASTER = registerBlock("strand_caster",
            () -> new com.hbm_m.block.machines.MachineStrandCasterBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(4.0f, 4.0f).sound(SoundType.METAL).noOcclusion()));
    public static final RegistrySupplier<Block> TORUS = registerBlock("torus",
            () -> new Block(BlockProps.copy(Blocks.IRON_BLOCK).strength(4.0f, 4.0f).sound(SoundType.METAL).noOcclusion()));

    public static final RegistrySupplier<Block> TURBINEGAS = registerBlock("turbinegas",
            () -> new com.hbm_m.block.machines.MachineTurbineGasBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(4.0f, 4.0f).sound(SoundType.METAL).noOcclusion()));
    public static final RegistrySupplier<Block> WATZ_PUMP = registerBlock("watz_pump",
            () -> new Block(BlockProps.copy(Blocks.IRON_BLOCK).strength(4.0f, 4.0f).sound(SoundType.METAL).noOcclusion()));

    public static final RegistrySupplier<Block> CHUNGUS = registerBlock("chungus",
            () -> new Block(BlockProps.copy(Blocks.IRON_BLOCK).strength(4.0f, 4.0f).sound(SoundType.METAL).noOcclusion()));

    public static final RegistrySupplier<Block> TEST_BLOCK = registerBlock("test_block",
            () -> new TestBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(4.0f, 4.0f).sound(SoundType.METAL).noOcclusion()));

    private static <T extends Block> RegistrySupplier<T> registerBlock(String name, Supplier<T> block) {
        RegistrySupplier<T> toReturn = BLOCKS.register(name, block);
        registerBlockItem(name, toReturn);
        return toReturn;
    }

    private static <T extends Block> RegistrySupplier<T> registerBlockWithoutItem(String name, Supplier<T> block) {
        return BLOCKS.register(name, block);
    }

    private static <T extends Block> RegistrySupplier<Item> registerBlockItem(String name, RegistrySupplier<T> block) {
        return ModItems.ITEMS.register(name, () -> {
            T b = block.get();
            // RBMK column blocks (fuel/moderator/control/console/panels/...) get a custom item
            // that renders through the same block entity renderer used in-world instead of a
            // static baked model - see RBMKColumnItemRenderer for why plain BlockItem doesn't
            // work for them.
            if (b instanceof com.hbm_m.block.machines.rbmk.RBMKColumnBlock
                    || b instanceof com.hbm_m.block.machines.MachineRbmkConsoleBlock) {
                return new com.hbm_m.item.rbmk.RBMKColumnBlockItem(b, new Item.Properties());
            }
            return new BlockItem(b, new Item.Properties());
        });
    }

    public static void init() {
        BLOCKS.register();
    }
}
