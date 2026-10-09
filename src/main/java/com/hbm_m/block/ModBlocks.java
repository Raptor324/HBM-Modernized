package com.hbm_m.block;

import net.minecraft.world.level.block.SoundType;

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
import com.hbm_m.block.decorations.CrateCanBlock;
import com.hbm_m.block.decorations.CrtBlock;
import com.hbm_m.block.decorations.DecorShapeBlock;
import com.hbm_m.block.decorations.DoorBlock;
import com.hbm_m.block.decorations.GrateBlock;
import com.hbm_m.block.decorations.KeyholeBlock;
import com.hbm_m.block.decorations.OilSpillBlock;
import com.hbm_m.block.decorations.PedestalBlock;
import com.hbm_m.block.decorations.SteelBeamBlock;
import com.hbm_m.block.decorations.SteelScaffoldBlock;
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
import com.hbm_m.block.machines.MachineBlastFurnaceBlock;
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
import com.hbm_m.block.machines.fusion.MachineFusionTorusBlock;
import com.hbm_m.block.machines.fusion.MachineFusionKlystronBlock;
import com.hbm_m.block.machines.fusion.MachineFusionKlystronCreativeBlock;
import com.hbm_m.block.machines.fusion.MachineFusionBreederBlock;
import com.hbm_m.block.machines.fusion.MachineFusionCollectorBlock;
import com.hbm_m.block.machines.fusion.MachineFusionCouplerBlock;
import com.hbm_m.block.machines.fusion.MachineFusionBoilerBlock;
import com.hbm_m.block.machines.fusion.MachineFusionMhdtBlock;
import com.hbm_m.block.machines.fusion.FusionComponentBlock;
import com.hbm_m.block.machines.fusion.MachineFusionPlasmaForgeBlock;
import com.hbm_m.block.machines.fusion.StructTorusCoreBlock;
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
import com.hbm_m.block.machines.MachineSolderingStationBlock;
import com.hbm_m.block.machines.MachineSteamTurbineBlock;
import com.hbm_m.block.machines.MachineSteamCondenserBlock;
import com.hbm_m.block.machines.MachineSubstationBlock;
import com.hbm_m.block.machines.MachineTowerSmallBlock;
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
import com.hbm_m.block.machines.rbmk.RBMKDisplayBlock;
import com.hbm_m.block.machines.rbmk.RBMKPanelBlock;
import com.hbm_m.block.machines.anvils.AnvilBlock;
import com.hbm_m.block.machines.anvils.AnvilTier;
import com.hbm_m.block.machines.crates.DeshCrateBlock;
import com.hbm_m.block.machines.crates.IronCrateBlock;
import com.hbm_m.block.machines.crates.SteelCrateBlock;
import com.hbm_m.block.machines.crates.TemplateCrateBlock;
import com.hbm_m.block.machines.crates.TungstenCrateBlock;
import com.hbm_m.block.nature.DepthOreBlock;
import com.hbm_m.block.network.PylonDummyBlock;
import com.hbm_m.block.network.RedCableGaugeBlock;
import com.hbm_m.block.network.RedCablePaintableBlock;
import com.hbm_m.block.network.RedConnectorBlock;
import com.hbm_m.block.network.RedPylonBlock;
import com.hbm_m.block.network.RedPylonLargeBlock;
import com.hbm_m.block.network.RedPylonMediumBlock;
import com.hbm_m.block.network.RedWireCoatedBlock;
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
import com.hbm_m.item.material.MaterialShape;
import com.hbm_m.item.material.ModMaterials;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.platform.PlatformHooks;

import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.shapes.Shapes;
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
                    .requiresCorrectToolForDrops().strength(5.0F, 6.0F).sound(SoundType.STONE)));

    /** Порт {@code rad_absorber} ({@link com.hbm.blocks.generic.BlockAbsorber}). */
    public static final RegistrySupplier<Block> RAD_ABSORBER = registerRadAbsorberBlock("rad_absorber",
            () -> new BlockAbsorber(BlockProps.copy(Blocks.IRON_BLOCK)
                    .requiresCorrectToolForDrops().strength(5.0F, 6.0F).sound(SoundType.STONE)));

    private static final BlockBehaviour.Properties TABLE_PROPERTIES =
            BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(5.0F, 6.0F).sound(SoundType.STONE).requiresCorrectToolForDrops();
    private static final BlockBehaviour.Properties ANVIL_PROPERTIES =
            BlockProps.copy(Blocks.ANVIL).strength(5.0F, 60.0F).sound(SoundType.ANVIL).noOcclusion();

    // Стандартные свойства для блоков слитков

    public static final List<RegistrySupplier<Block>> BATTERY_BLOCKS = new ArrayList<>();

    // Вспомогательный метод для регистрации батареек
    private static RegistrySupplier<Block> registerBattery(String name, long capacity) {
        // 1. Регистрируем БЛОК
        RegistrySupplier<Block> batteryBlock = BLOCKS.register(name,
                () -> new MachineBatteryBlock(BlockBehaviour.Properties.of().strength(5.0F, 6.0F).requiresCorrectToolForDrops(), capacity));

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

    /**
     * Originalwerte der Speicherbloecke (1.7.10 ModBlocks: setHardness/setResistance/setLightLevel/setStepSound).
     * Werte: Haerte, Explosionswiderstand (setResistance*3/5), Lichtwert, 1 = Steinklang (sonst Metall).
     */
    private static final Map<String, float[]> INGOT_BLOCK_VALUES = Map.ofEntries(
            Map.entry("block_actinium", new float[]{5.0F, 6.0F, 0, 0}),
            Map.entry("block_aluminium", new float[]{5.0F, 12.0F, 0, 0}),
            Map.entry("block_australium", new float[]{5.0F, 6.0F, 0, 1}),
            Map.entry("block_beryllium", new float[]{5.0F, 12.0F, 0, 0}),
            Map.entry("block_bismuth", new float[]{5.0F, 54.0F, 0, 0}),
            Map.entry("block_boron", new float[]{5.0F, 6.0F, 0, 0}),
            Map.entry("block_cadmium", new float[]{5.0F, 54.0F, 0, 0}),
            Map.entry("block_cdalloy", new float[]{5.0F, 42.0F, 0, 0}),
            Map.entry("block_cobalt", new float[]{5.0F, 30.0F, 0, 1}),
            Map.entry("block_combine_steel", new float[]{5.0F, 360.0F, 0, 0}),
            Map.entry("block_desh", new float[]{5.0F, 180.0F, 0, 0}),
            Map.entry("block_dineutronium", new float[]{5.0F, 36000.0F, 0, 0}),
            Map.entry("block_dura_steel", new float[]{5.0F, 120.0F, 0, 0}),
            Map.entry("block_euphemium", new float[]{5.0F, 36000.0F, 0, 0}),
            Map.entry("block_lanthanium", new float[]{5.0F, 6.0F, 0, 0}),
            Map.entry("block_lead", new float[]{5.0F, 30.0F, 0, 0}),
            Map.entry("block_mox_fuel", new float[]{5.0F, 30.0F, 0, 0}),
            Map.entry("block_neptunium", new float[]{5.0F, 36.0F, 0, 0}),
            Map.entry("block_niobium", new float[]{5.0F, 30.0F, 0, 0}),
            Map.entry("block_plutonium", new float[]{5.0F, 30.0F, 0, 0}),
            Map.entry("block_plutonium_fuel", new float[]{5.0F, 30.0F, 0, 0}),
            Map.entry("block_pu238", new float[]{5.0F, 30.0F, 5, 0}),
            Map.entry("block_pu239", new float[]{5.0F, 30.0F, 0, 0}),
            Map.entry("block_pu240", new float[]{5.0F, 30.0F, 0, 0}),
            Map.entry("block_ra226", new float[]{5.0F, 6.0F, 0, 0}),
            Map.entry("block_red_copper", new float[]{5.0F, 15.0F, 0, 0}),
            Map.entry("block_schrabidate", new float[]{5.0F, 360.0F, 0, 0}),
            Map.entry("block_schrabidium", new float[]{5.0F, 360.0F, 0, 0}),
            Map.entry("block_schrabidium_fuel", new float[]{5.0F, 360.0F, 0, 0}),
            Map.entry("block_schraranium", new float[]{5.0F, 150.0F, 0, 0}),
            Map.entry("block_solinium", new float[]{5.0F, 360.0F, 0, 0}),
            Map.entry("block_starmetal", new float[]{5.0F, 240.0F, 0, 0}),
            Map.entry("block_steel", new float[]{5.0F, 30.0F, 0, 0}),
            Map.entry("block_tcalloy", new float[]{5.0F, 42.0F, 0, 0}),
            Map.entry("block_thorium", new float[]{5.0F, 30.0F, 0, 0}),
            Map.entry("block_thorium_fuel", new float[]{5.0F, 30.0F, 0, 0}),
            Map.entry("block_titanium", new float[]{5.0F, 30.0F, 0, 0}),
            Map.entry("block_tungsten", new float[]{5.0F, 12.0F, 0, 0}),
            Map.entry("block_u233", new float[]{5.0F, 30.0F, 0, 0}),
            Map.entry("block_u235", new float[]{5.0F, 30.0F, 0, 0}),
            Map.entry("block_u238", new float[]{5.0F, 30.0F, 0, 0}),
            Map.entry("block_uranium", new float[]{5.0F, 30.0F, 0, 0}),
            Map.entry("block_uranium_fuel", new float[]{5.0F, 30.0F, 0, 0}),
            Map.entry("block_zirconium", new float[]{5.0F, 18.0F, 0, 0})
    );

    private static BlockBehaviour.Properties ingotBlockProps(String id) {
        float[] v = INGOT_BLOCK_VALUES.get(id);
        if (v == null) return INGOT_BLOCK_PROPERTIES;
        BlockBehaviour.Properties p = BlockProps.copy(Blocks.IRON_BLOCK).strength(v[0], v[1])
                .sound(v[3] > 0 ? SoundType.STONE : SoundType.METAL).requiresCorrectToolForDrops();
        if (v[2] > 0) { int l = (int) v[2]; p.lightLevel(st -> l); }
        return p;
    }

    // Блоки хранения задаются формой BLOCK в реестре ModMaterials (единый источник истины).

    // 2. КАРТА БЛОКОВ
    public static final Map<ModMaterials, RegistrySupplier<Block>> INGOT_BLOCKS = new EnumMap<>(ModMaterials.class);

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
    public static boolean hasRadFogParticles(ModMaterials mat) {
        return RADFOG_INGOT_BLOCKS.contains(mat.getId());
    }

    public static boolean hasSchrabFogParticles(ModMaterials mat) {
        return SCHRABFOG_INGOT_BLOCKS.contains(mat.getId());
    }

    // 3. АВТОМАТИЧЕСКАЯ РЕГИСТРАЦИЯ
    static {
        for (ModMaterials mat : ModMaterials.values()) {
            // Проверяем, задан ли материалу блок хранения формой BLOCK
            if (!mat.has(MaterialShape.BLOCK)) continue;

            String blockName = MaterialShape.BLOCK.itemId(mat);

            // Display particles: RADFOG / SCHRAB (1.7.10 BlockHazard#setDisplayEffect, ModBlocks ~1326-1373).
            // Все слитковые блоки — это BlockHazard; per-tick эмиттер чанковой радиации (hazard × 0.1/сек)
            // запускается автоматически через scheduled-tick. Различаются только визуальные частицы.
            RegistrySupplier<Block> registeredBlock = registerBlock(blockName,
                    () -> {
                        BlockHazard block = new BlockHazard(ingotBlockProps(blockName)).makeBeaconable();
                        if (hasRadFogParticles(mat)) {
                            block.setDisplayEffect(BlockHazard.ExtDisplayEffect.RADFOG);
                        } else if (hasSchrabFogParticles(mat)) {
                            block.setDisplayEffect(BlockHazard.ExtDisplayEffect.SCHRAB);
                        }
                        return block;
                    });

            // Сохраняем в карту
            INGOT_BLOCKS.put(mat, registeredBlock);
        }
    }

    // Вспомогательный метод получения блока
    public static RegistrySupplier<Block> getIngotBlock(ModMaterials mat) {
        RegistrySupplier<Block> block = INGOT_BLOCKS.get(mat);
        if (block == null) {
            // Логируем ошибку или возвращаем заглушку, чтобы игра не крашилась при обращении к несуществующему блоку
            throw new NullPointerException("Block for material " + mat.getId() + " is not registered! Нет формы BLOCK в ModMaterials.");
        }
        return block;
    }

    public static boolean hasIngotBlock(ModMaterials mat) {
        return INGOT_BLOCKS.containsKey(mat);
    }

    public static final RegistrySupplier<Block> URANIUM_BLOCK = getIngotBlock(ModMaterials.URANIUM);
    public static final RegistrySupplier<Block> PLUTONIUM_BLOCK = getIngotBlock(ModMaterials.PLUTONIUM);
    public static final RegistrySupplier<Block> PLUTONIUM_FUEL_BLOCK = getIngotBlock(ModMaterials.PLUTONIUM_FUEL);

    public static final RegistrySupplier<Block> POLONIUM210_BLOCK = registerBlock("polonium210_block",
            () -> new BlockHazard(BlockProps.copy(Blocks.IRON_BLOCK).strength(5.0F, 30.0F).sound(SoundType.METAL).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> WASTE_GRASS = registerBlock("waste_grass",
            () -> new Block(BlockProps.copy(Blocks.DIRT).sound(SoundType.GRAVEL)));

    public static final RegistrySupplier<Block> WASTE_LEAVES = registerBlock("waste_leaves",
            () -> new com.hbm_m.block.generic.WasteLeaves(BlockProps.copy(Blocks.OAK_LEAVES).noOcclusion().strength(0.1F, 0.1F)));

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

    /** Legacy одноблочная доменная печь (старые миры, ориг. MachineDifurnace). */
    public static final RegistrySupplier<Block> BLAST_FURNACE = registerBlock("blast_furnace",
            () -> new BlastFurnaceBlock(BlockProps.copy(Blocks.IRON_BLOCK)
                    .strength(4.0f, 4.0f)
                    .sound(SoundType.STONE)
                    .lightLevel(state -> state.getValue(BlastFurnaceBlock.LIT) ? 15 : 0)));

    /** Обновлённая мультиблочная доменная печь 3x7x3 (ориг. MachineBlastFurnace). */
    public static final RegistrySupplier<Block> MACHINE_BLAST_FURNACE = registerBlock("machine_blast_furnace",
            () -> new MachineBlastFurnaceBlock(BlockProps.copy(Blocks.IRON_BLOCK)
                    .sound(SoundType.STONE)
                    .lightLevel(state -> state.getValue(MachineBlastFurnaceBlock.LIT) ? 15 : 0).strength(5.0F, 6.0F)));

    public static final RegistrySupplier<Block> BLAST_FURNACE_EXTENSION = registerBlock("blast_furnace_extension",
            () -> new BlastFurnaceExtensionBlock(BlockProps.copy(Blocks.IRON_BLOCK)
                    .strength(3.0f, 4.0f)
                    .sound(SoundType.METAL)
                    .noOcclusion()));
					
	// МУЛЬТИБЛОКИ ----------------------------------------------------------------------------------------------------
	
    public static final RegistrySupplier<Block> PRESS = registerBlockWithoutItem("press",
            () -> new MachinePressBlock(BlockProps.copy(Blocks.IRON_BLOCK).noOcclusion().strength(5.0F, 6.0F).sound(SoundType.STONE)));

    public static final RegistrySupplier<Block> WOOD_BURNER = registerBlockWithoutItem("wood_burner",
            () -> new MachineWoodBurnerBlock(BlockProps.copy(Blocks.IRON_BLOCK).noOcclusion().strength(5.0F, 6.0F).sound(SoundType.STONE)));

    public static final RegistrySupplier<Block> ARMOR_TABLE = registerBlock("armor_table",
            () -> new ArmorTableBlock(TABLE_PROPERTIES));

    /** SEDNA: Original BlockWeaponTable (machine_weapon_table, Material.iron, Haerte 5, Widerstand 10 -> 6). */
    public static final RegistrySupplier<Block> MACHINE_WEAPON_TABLE = registerBlock("machine_weapon_table",
            () -> new com.hbm_m.block.machines.WeaponTableBlock(TABLE_PROPERTIES));

    public static final RegistrySupplier<Block> SHREDDER = registerBlock("shredder",
            () -> new MachineShredderBlock(BlockProps.copy(Blocks.IRON_BLOCK).sound(SoundType.STONE)));

    public static final RegistrySupplier<Block> SWITCH = registerBlock("switch",
            () -> new SwitchBlock(BlockProps.copy(Blocks.IRON_BLOCK)));

    public static final RegistrySupplier<Block> MACHINE_ASSEMBLER = registerBlockWithoutItem("machine_assembler",
            () -> new MachineAssemblerBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(2.0f).noOcclusion()));

    public static final RegistrySupplier<Block> ADVANCED_ASSEMBLY_MACHINE = registerBlockWithoutItem("advanced_assembly_machine",
            () -> new MachineAdvancedAssemblerBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(2.0f).noOcclusion()));

    public static final RegistrySupplier<Block> HYDRAULIC_FRACKINING_TOWER = registerBlockWithoutItem("hydraulic_frackining_tower",
            () -> new MachineFrackingTowerBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(4.0f).noOcclusion().isSuffocating((state, world, pos) -> false)));

    public static final RegistrySupplier<Block> COOLING_TOWER = registerBlockWithoutItem("cooling_tower",
            () -> new MachineTowerLargeBlock(BlockProps.copy(Blocks.IRON_BLOCK).noOcclusion().isSuffocating((state, world, pos) -> false).strength(5.0F, 6.0F).sound(SoundType.STONE))); // Original: Haerte 5, Widerstand 10

    public static final RegistrySupplier<Block> TOWER_SMALL = registerBlockWithoutItem("tower_small",
            () -> new MachineTowerSmallBlock(BlockProps.copy(Blocks.IRON_BLOCK).noOcclusion().isSuffocating((state, world, pos) -> false).strength(5.0F, 6.0F).sound(SoundType.STONE)));

    public static final RegistrySupplier<Block> CYCLOTRON = registerBlockWithoutItem("cyclotron",
            () -> new MachineCyclotronBlock(BlockProps.copy(Blocks.IRON_BLOCK).noOcclusion().isSuffocating((state, world, pos) -> false).strength(5.0F, 6.0F).sound(SoundType.STONE)));

    public static final RegistrySupplier<Block> ZIRNOX = registerBlockWithoutItem("zirnox",
            () -> new MachineZirnoxBlock(BlockProps.copy(Blocks.IRON_BLOCK).noOcclusion().isSuffocating((state, world, pos) -> false).strength(5.0F, 60.0F).sound(SoundType.STONE)));

    public static final RegistrySupplier<Block> ZIRNOX_DESTROYED = registerBlockWithoutItem("zirnox_destroyed",
            () -> new MachineZirnoxDestroyedBlock(BlockProps.copy(Blocks.IRON_BLOCK).noOcclusion().isSuffocating((state, world, pos) -> false).strength(100.0F, 480.0F).sound(SoundType.STONE)));

    public static final RegistrySupplier<Block> ZIRNOX_DEB_BLANK     = registerBlockWithoutItem("zirnox_deb_blank",     () -> new Block(BlockBehaviour.Properties.of().strength(-1F, Float.MAX_VALUE).noOcclusion()));
    public static final RegistrySupplier<Block> ZIRNOX_DEB_ELEMENT   = registerBlockWithoutItem("zirnox_deb_element",   () -> new Block(BlockBehaviour.Properties.of().strength(-1F, Float.MAX_VALUE).noOcclusion()));
    public static final RegistrySupplier<Block> ZIRNOX_DEB_SHRAPNEL  = registerBlockWithoutItem("zirnox_deb_shrapnel",  () -> new Block(BlockBehaviour.Properties.of().strength(-1F, Float.MAX_VALUE).noOcclusion()));
    public static final RegistrySupplier<Block> ZIRNOX_DEB_CONCRETE  = registerBlockWithoutItem("zirnox_deb_concrete",  () -> new Block(BlockBehaviour.Properties.of().strength(-1F, Float.MAX_VALUE).noOcclusion()));
    public static final RegistrySupplier<Block> ZIRNOX_DEB_EXCHANGER = registerBlockWithoutItem("zirnox_deb_exchanger", () -> new Block(BlockBehaviour.Properties.of().strength(-1F, Float.MAX_VALUE).noOcclusion()));

    public static final RegistrySupplier<Block> ARC_WELDER = registerBlockWithoutItem("arc_welder",
            () -> new MachineArcWelderBlock(BlockProps.copy(Blocks.IRON_BLOCK).noOcclusion().isSuffocating((state, world, pos) -> false).strength(5.0F, 18.0F).sound(SoundType.STONE)));

    public static final RegistrySupplier<Block> SOLDERING_STATION = registerBlockWithoutItem("soldering_station",
            () -> new MachineSolderingStationBlock(BlockProps.copy(Blocks.IRON_BLOCK).noOcclusion().isSuffocating((state, world, pos) -> false).strength(5.0F, 18.0F).sound(SoundType.STONE)));

    public static final RegistrySupplier<Block> MIXER = registerBlockWithoutItem("mixer",
            () -> new MachineMixerBlock(BlockProps.copy(Blocks.IRON_BLOCK).noOcclusion().isSuffocating((state, world, pos) -> false).strength(5.0F, 18.0F).sound(SoundType.STONE)));

    public static final RegistrySupplier<Block> DERRICK = registerBlockWithoutItem("derrick",
            () -> new MachineDerrickBlock(BlockProps.copy(Blocks.IRON_BLOCK).noOcclusion().isSuffocating((state, world, pos) -> false).strength(5.0F, 12.0F).sound(SoundType.STONE)));


    public static final RegistrySupplier<Block> RBMK_CONSOLE = registerBlockWithoutItem("rbmk_console",
            () -> new MachineRbmkConsoleBlock(BlockProps.copy(Blocks.IRON_BLOCK).noOcclusion().isSuffocating((state, world, pos) -> false).strength(3.0F, 18.0F).sound(SoundType.STONE)));

    public static final RegistrySupplier<Block> FLARE_STACK = registerBlockWithoutItem("flare_stack",
            () -> new MachineFlareStackBlock(BlockProps.copy(Blocks.IRON_BLOCK).noOcclusion().isSuffocating((state, world, pos) -> false).strength(5.0F, 60.0F).sound(SoundType.STONE)));

    public static final RegistrySupplier<Block> PUMPJACK = registerBlockWithoutItem("pumpjack",
            () -> new MachinePumpjackBlock(BlockProps.copy(Blocks.IRON_BLOCK).noOcclusion().isSuffocating((state, world, pos) -> false).strength(5.0F, 12.0F).sound(SoundType.STONE)));

    public static final RegistrySupplier<Block> RADAR = registerBlockWithoutItem("radar",
            () -> new MachineRadarBlock(BlockProps.copy(Blocks.IRON_BLOCK).noOcclusion().isSuffocating((state, world, pos) -> false).strength(5.0F, 6.0F).sound(SoundType.STONE)));

    public static final RegistrySupplier<Block> LARGE_RADAR = registerBlockWithoutItem("large_radar",
            () -> new MachineLargeRadarBlock(BlockProps.copy(Blocks.IRON_BLOCK).noOcclusion().isSuffocating((state, world, pos) -> false).strength(5.0F, 6.0F).sound(SoundType.STONE)));

    public static final RegistrySupplier<Block> RADAR_SCREEN = registerBlockWithoutItem("radar_screen",
            () -> new MachineRadarScreenBlock(BlockProps.copy(Blocks.IRON_BLOCK).noOcclusion().isSuffocating((state, world, pos) -> false).strength(5.0F, 6.0F).sound(SoundType.STONE)));

    public static final RegistrySupplier<Block> CRACKING_TOWER = registerBlockWithoutItem("cracking_tower",
            () -> new MachineCrackingTowerBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(4.0f).noOcclusion().isSuffocating((state, world, pos) -> false)));

    /** Turrets: Sentry 1x1 ({@link com.hbm_m.block.machines.TurretBlock}), alle uebrigen wie im Original BlockDummyable ({@link com.hbm_m.block.machines.TurretMultiblockBlock}). */
    public static final RegistrySupplier<Block> TURRET_SENTRY = registerBlock("turret_sentry",
            () -> new com.hbm_m.block.machines.TurretBlock(BlockProps.copy(Blocks.IRON_BLOCK).noOcclusion().strength(5.0F, 3.0F).sound(SoundType.STONE),
                    () -> com.hbm_m.blockentity.ModBlockEntities.TURRET_SENTRY_BE.get()));
    public static final RegistrySupplier<Block> TURRET_CHEKHOV = registerBlock("turret_chekhov",
            () -> new com.hbm_m.block.machines.TurretMultiblockBlock.Small(BlockProps.copy(Blocks.IRON_BLOCK).noOcclusion().strength(5.0F, 6.0F).sound(SoundType.STONE),
                    () -> com.hbm_m.blockentity.ModBlockEntities.TURRET_CHEKHOV_BE.get()));
    public static final RegistrySupplier<Block> TURRET_FRIENDLY = registerBlock("turret_friendly",
            () -> new com.hbm_m.block.machines.TurretMultiblockBlock.Small(BlockProps.copy(Blocks.IRON_BLOCK).noOcclusion().strength(5.0F, 6.0F).sound(SoundType.STONE),
                    () -> com.hbm_m.blockentity.ModBlockEntities.TURRET_FRIENDLY_BE.get()));
    public static final RegistrySupplier<Block> TURRET_JEREMY = registerBlock("turret_jeremy",
            () -> new com.hbm_m.block.machines.TurretMultiblockBlock.Small(BlockProps.copy(Blocks.IRON_BLOCK).noOcclusion().strength(5.0F, 360.0F).sound(SoundType.STONE),
                    () -> com.hbm_m.blockentity.ModBlockEntities.TURRET_JEREMY_BE.get()));
    public static final RegistrySupplier<Block> TURRET_TAUON = registerBlock("turret_tauon",
            () -> new com.hbm_m.block.machines.TurretMultiblockBlock.Small(BlockProps.copy(Blocks.IRON_BLOCK).noOcclusion().strength(5.0F, 36.0F).sound(SoundType.STONE),
                    () -> com.hbm_m.blockentity.ModBlockEntities.TURRET_TAUON_BE.get()));
    public static final RegistrySupplier<Block> TURRET_RICHARD = registerBlock("turret_richard",
            () -> new com.hbm_m.block.machines.TurretMultiblockBlock.Small(BlockProps.copy(Blocks.IRON_BLOCK).noOcclusion().strength(5.0F, 360.0F).sound(SoundType.STONE),
                    () -> com.hbm_m.blockentity.ModBlockEntities.TURRET_RICHARD_BE.get()));
    public static final RegistrySupplier<Block> TURRET_HOWARD = registerBlock("turret_howard",
            () -> new com.hbm_m.block.machines.TurretMultiblockBlock.Small(BlockProps.copy(Blocks.IRON_BLOCK).noOcclusion().strength(5.0F, 36.0F).sound(SoundType.STONE),
                    () -> com.hbm_m.blockentity.ModBlockEntities.TURRET_HOWARD_BE.get()));
    public static final RegistrySupplier<Block> TURRET_MAXWELL = registerBlock("turret_maxwell",
            () -> new com.hbm_m.block.machines.TurretMultiblockBlock.Small(BlockProps.copy(Blocks.IRON_BLOCK).noOcclusion().strength(5.0F, 36.0F).sound(SoundType.STONE),
                    () -> com.hbm_m.blockentity.ModBlockEntities.TURRET_MAXWELL_BE.get()));
    public static final RegistrySupplier<Block> TURRET_FRITZ = registerBlock("turret_fritz",
            () -> new com.hbm_m.block.machines.TurretMultiblockBlock.Small(BlockProps.copy(Blocks.IRON_BLOCK).noOcclusion().strength(5.0F, 6.0F).sound(SoundType.STONE),
                    () -> com.hbm_m.blockentity.ModBlockEntities.TURRET_FRITZ_BE.get()));
    public static final RegistrySupplier<Block> TURRET_ARTY = registerBlock("turret_arty",
            () -> new com.hbm_m.block.machines.TurretMultiblockBlock.Large(BlockProps.copy(Blocks.IRON_BLOCK).noOcclusion().strength(5.0F, 360.0F).sound(SoundType.STONE),
                    () -> com.hbm_m.blockentity.ModBlockEntities.TURRET_ARTY_BE.get()));
    public static final RegistrySupplier<Block> TURRET_HIMARS = registerBlock("turret_himars",
            () -> new com.hbm_m.block.machines.TurretMultiblockBlock.Large(BlockProps.copy(Blocks.IRON_BLOCK).noOcclusion().strength(5.0F, 360.0F).sound(SoundType.STONE),
                    () -> com.hbm_m.blockentity.ModBlockEntities.TURRET_HIMARS_BE.get()));
    /** Original turret_sentry_damaged / turret_howard_damaged (Bunker-Deko ohne GUI, ohne Drop). */
    public static final RegistrySupplier<Block> TURRET_SENTRY_DAMAGED = registerBlock("turret_sentry_damaged",
            () -> new com.hbm_m.block.machines.TurretDamagedBlock(BlockProps.copy(Blocks.IRON_BLOCK).noOcclusion().strength(5.0F, 3.0F).sound(SoundType.STONE),
                    () -> com.hbm_m.blockentity.ModBlockEntities.TURRET_SENTRY_DAMAGED_BE.get()));
    public static final RegistrySupplier<Block> TURRET_HOWARD_DAMAGED = registerBlock("turret_howard_damaged",
            () -> new com.hbm_m.block.machines.TurretMultiblockBlock.SmallDamaged(BlockProps.copy(Blocks.IRON_BLOCK).noOcclusion().strength(5.0F, 360.0F).sound(SoundType.STONE),
                    () -> com.hbm_m.blockentity.ModBlockEntities.TURRET_HOWARD_DAMAGED_BE.get()));

    public static final RegistrySupplier<Block> FRACTION_TOWER = registerBlockWithoutItem("fraction_tower",
            () -> new MachineFractionTowerBlock(BlockProps.copy(Blocks.IRON_BLOCK).noOcclusion().isSuffocating((state, world, pos) -> false).strength(5.0F, 6.0F).sound(SoundType.STONE)));

    public static final RegistrySupplier<Block> MINING_DRILL = registerBlockWithoutItem("mining_drill",
            () -> new MachineMiningDrillBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(4.0f).noOcclusion().isSuffocating((state, world, pos) -> false)));

    public static final RegistrySupplier<Block> FEL = registerBlockWithoutItem("fel",
            () -> new MachineFelBlock(BlockProps.copy(Blocks.IRON_BLOCK).noOcclusion().isSuffocating((state, world, pos) -> false).strength(5.0F, 6.0F).sound(SoundType.STONE)));

    public static final RegistrySupplier<Block> SILEX = registerBlockWithoutItem("silex",
            () -> new MachineSilexBlock(BlockProps.copy(Blocks.IRON_BLOCK).noOcclusion().isSuffocating((state, world, pos) -> false).strength(5.0F, 6.0F).sound(SoundType.STONE)));

    public static final RegistrySupplier<Block> CRYSTALLIZER = registerBlockWithoutItem("crystallizer",
            () -> new MachineCrystallizerBlock(BlockProps.copy(Blocks.IRON_BLOCK).noOcclusion().isSuffocating((state, world, pos) -> false).sound(SoundType.STONE)));

    public static final RegistrySupplier<Block> BREEDER = registerBlockWithoutItem("breeder",
            () -> new MachineBreederBlock(BlockProps.copy(Blocks.IRON_BLOCK).noOcclusion().isSuffocating((state, world, pos) -> false)));

    public static final RegistrySupplier<Block> LARGE_PYLON = registerBlockWithoutItem("large_pylon",
            () -> new MachineLargePylonBlock(BlockProps.copy(Blocks.IRON_BLOCK).noOcclusion().isSuffocating((state, world, pos) -> false)));

    public static final RegistrySupplier<Block> CHEMICAL_PLANT = registerBlockWithoutItem("chemical_plant",
            () -> new MachineChemicalPlantBlock(BlockProps.copy(Blocks.IRON_BLOCK).noOcclusion().isSuffocating((state, world, pos) -> false).strength(5.0F, 18.0F).sound(SoundType.STONE)));

    public static final RegistrySupplier<Block> CRUCIBLE = registerBlock("crucible",
            () -> new MachineCrucibleBlock(BlockProps.copy(Blocks.IRON_BLOCK).noOcclusion().isSuffocating((state, world, pos) -> false).strength(5.0F, 6.0F).sound(SoundType.STONE)));

    public static final RegistrySupplier<Block> FOUNDRY_BASIN = registerBlock("foundry_basin",
            () -> new MachineFoundryBasinBlock(BlockProps.copy(Blocks.IRON_BLOCK).noOcclusion().isSuffocating((state, world, pos) -> false).strength(5.0F, 6.0F).sound(SoundType.STONE)));

    public static final RegistrySupplier<Block> FOUNDRY_CHANNEL = registerBlock("foundry_channel",
            () -> new MachineFoundryChannelBlock(BlockProps.copy(Blocks.IRON_BLOCK).noOcclusion().isSuffocating((state, world, pos) -> false).strength(5.0F, 6.0F).sound(SoundType.STONE)));

    public static final RegistrySupplier<Block> FOUNDRY_OUTLET = registerBlock("foundry_outlet",
            () -> new com.hbm_m.block.machines.MachineFoundryOutletBlock(BlockProps.copy(Blocks.IRON_BLOCK).noOcclusion().isSuffocating((state, world, pos) -> false).strength(5.0F, 6.0F).sound(SoundType.STONE)));

    // ─── Trophies ─────────────────────────────────────────────────────────────
    public static final RegistrySupplier<Block> GAS_CENTRIFUGE = registerBlockWithoutItem("gas_centrifuge",
            () -> new MachineGasCentrifugeBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(4.0f, 4.0f).sound(SoundType.METAL).noOcclusion().isSuffocating((state, world, pos) -> false)));

    public static final RegistrySupplier<Block> CENTRIFUGE = registerBlockWithoutItem("centrifuge",
            () -> new MachineCentrifugeBlock(BlockProps.copy(Blocks.IRON_BLOCK).isSuffocating((state, world, pos) -> false).strength(5.0F, 6.0F).sound(SoundType.STONE)));

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
            () -> new LaunchPadBlock(BlockProps.copy(Blocks.IRON_BLOCK).noOcclusion().sound(SoundType.STONE)));

    /** 1:1 LaunchPadLarge (grosse Startrampe 9x9 mit Aufrichter) - Gegenstand automatisch MultiblockBlockItem. */
    public static final RegistrySupplier<Block> LAUNCH_PAD_LARGE = registerBlock("launch_pad_large",
            () -> new com.hbm_m.block.machines.LaunchPadLargeBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(5.0F, 10.0F).noOcclusion().sound(SoundType.METAL)));

    /** Mobile Startrampe (Iskander-M TEL), 15x5-Grundriss - siehe MobileLaunchPadBlock. */
    public static final RegistrySupplier<Block> MOBILE_LAUNCH_PAD = registerBlockWithoutItem("mobile_launch_pad",
            () -> new com.hbm_m.block.machines.MobileLaunchPadBlock(BlockProps.copy(Blocks.IRON_BLOCK).noOcclusion()));

    /** Topol-M-Werfer (MZKT-79221), 25x5-Grundriss mit Aufrichter - siehe TopolLaunchPadBlock. */
    public static final RegistrySupplier<Block> TOPOL_LAUNCH_PAD = registerBlockWithoutItem("topol_launch_pad",
            () -> new com.hbm_m.block.machines.TopolLaunchPadBlock(BlockProps.copy(Blocks.IRON_BLOCK).noOcclusion()));

    public static final RegistrySupplier<Block> LAUNCH_PAD_RUSTED = registerBlockWithoutItem("launch_pad_rusted",
            () -> new LaunchPadRustedBlock(BlockProps.copy(Blocks.IRON_BLOCK).noOcclusion().sound(SoundType.STONE)));

    public static final RegistrySupplier<Block> MACHINE_BATTERY_SOCKET = registerBlockWithoutItem("machine_battery_socket",
            () -> new MachineBatterySocketBlock(BlockProps.copy(Blocks.IRON_BLOCK).requiresCorrectToolForDrops().noOcclusion().isSuffocating((state, world, pos) -> false).strength(5.0F, 6.0F).sound(SoundType.STONE)));

    public static final RegistrySupplier<Block> INDUSTRIAL_BOILER = registerBlockWithoutItem("industrial_boiler",
            () -> new MachineIndustrialBoilerBlock(BlockProps.copy(Blocks.IRON_BLOCK).noOcclusion().isSuffocating((state, world, pos) -> false).strength(5.0F, 6.0F).sound(SoundType.STONE)));

    public static final RegistrySupplier<Block> SOLAR_BOILER = registerBlockWithoutItem("solar_boiler",
            () -> new MachineSolarBoilerBlock(BlockProps.copy(Blocks.IRON_BLOCK).noOcclusion().isSuffocating((state, world, pos) -> false).strength(5.0F, 6.0F).sound(SoundType.STONE)));

    // 1:1 SolarMirror (Haerte 5, Widerstand 10)
    public static final RegistrySupplier<Block> SOLAR_MIRROR = registerBlock("solar_mirror",
            () -> new com.hbm_m.block.machines.SolarMirrorBlock(BlockProps.copy(Blocks.IRON_BLOCK).noOcclusion().strength(5.0F, 6.0F).sound(SoundType.STONE)));

    public static final RegistrySupplier<Block> WATZ_POWERPLANT = registerBlockWithoutItem("watz_powerplant",
            () -> new MachineWatzPowerplantBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(5.0f, 6.0f).sound(SoundType.METAL).noOcclusion().noLootTable().isSuffocating((state, world, pos) -> false)));

    public static final RegistrySupplier<Block> HYDROTREATER = registerBlockWithoutItem("hydrotreater",
            () -> new MachineHydrotreaterBlock(BlockProps.copy(Blocks.IRON_BLOCK).noOcclusion().isSuffocating((state, world, pos) -> false).strength(5.0F, 6.0F).sound(SoundType.STONE)));

    public static final RegistrySupplier<Block> CATALYTIC_REFORMER = registerBlockWithoutItem("catalytic_reformer",
            () -> new MachineCatalyticReformerBlock(BlockProps.copy(Blocks.IRON_BLOCK).noOcclusion().isSuffocating((state, world, pos) -> false).strength(5.0F, 6.0F).sound(SoundType.STONE)));

    public static final RegistrySupplier<Block> DEUTERIUM_TOWER = registerBlockWithoutItem("deuterium_tower",
            () -> new MachineDeuteriumTowerBlock(BlockProps.copy(Blocks.IRON_BLOCK).noOcclusion().isSuffocating((state, world, pos) -> false).strength(10.0F, 12.0F).sound(SoundType.STONE)));

    public static final RegistrySupplier<Block> CHEMICAL_FACTORY = registerBlockWithoutItem("chemical_factory",
            () -> new MachineChemicalFactoryBlock(BlockProps.copy(Blocks.IRON_BLOCK).noOcclusion().isSuffocating((state, world, pos) -> false).strength(5.0F, 18.0F).sound(SoundType.STONE)));

    public static final RegistrySupplier<Block> STEAM_TURBINE = registerBlockWithoutItem("steam_turbine",
            () -> new MachineSteamTurbineBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(4.0f, 4.0f).sound(SoundType.METAL)));

    public static final RegistrySupplier<Block> LIQUEFACTOR = registerBlockWithoutItem("liquefactor",
            () -> new MachineLiquefactorBlock(BlockProps.copy(Blocks.IRON_BLOCK).noOcclusion().isSuffocating((state, world, pos) -> false).strength(10.0F, 12.0F).sound(SoundType.STONE)));

    public static final RegistrySupplier<Block> CORE_EMITTER = registerBlockWithoutItem("core_emitter",
            () -> new MachineCoreEmitterBlock(BlockProps.copy(Blocks.IRON_BLOCK).noOcclusion().isSuffocating((state, world, pos) -> false).strength(5.0F, 6.0F).sound(SoundType.STONE)));

    public static final RegistrySupplier<Block> CORE_INJECTOR = registerBlockWithoutItem("core_injector",
            () -> new MachineCoreInjectorBlock(BlockProps.copy(Blocks.IRON_BLOCK).noOcclusion().isSuffocating((state, world, pos) -> false).strength(5.0F, 6.0F).sound(SoundType.STONE)));

    public static final RegistrySupplier<Block> CORE_RECEIVER = registerBlockWithoutItem("core_receiver",
            () -> new MachineCoreReceiverBlock(BlockProps.copy(Blocks.IRON_BLOCK).noOcclusion().isSuffocating((state, world, pos) -> false).strength(5.0F, 6.0F).sound(SoundType.STONE)));

    public static final RegistrySupplier<Block> VACUUM_DISTILL = registerBlockWithoutItem("vacuum_distill",
            () -> new MachineVacuumDistillBlock(BlockProps.copy(Blocks.IRON_BLOCK).noOcclusion().isSuffocating((state, world, pos) -> false).strength(5.0F, 12.0F).sound(SoundType.STONE)));

    public static final RegistrySupplier<Block> TURBOFAN = registerBlockWithoutItem("turbofan",
            () -> new MachineTurbofanBlock(BlockProps.copy(Blocks.IRON_BLOCK).noOcclusion().isSuffocating((state, world, pos) -> false).strength(5.0F, 6.0F).sound(SoundType.STONE)));

    public static final RegistrySupplier<Block> REFINERY = registerBlockWithoutItem("refinery",
            () -> new MachineRefineryBlock(BlockProps.copy(Blocks.IRON_BLOCK).noOcclusion().isSuffocating((state, world, pos) -> false).strength(5.0F, 12.0F).sound(SoundType.STONE)));

    public static final RegistrySupplier<Block> INDUSTRIAL_TURBINE = registerBlockWithoutItem("industrial_turbine",
            () -> new MachineIndustrialTurbineBlock(BlockProps.copy(Blocks.IRON_BLOCK).noOcclusion().isSuffocating((state, world, pos) -> false).strength(5.0F, 6.0F).sound(SoundType.STONE)));

    // "turbine" (WIP-Duplikat von machine_large_turbine) entfernt: alte IDs remappt ForgeMainEvents.LEGACY_IDS

    public static final RegistrySupplier<Block> SUBSTATION = registerBlockWithoutItem("substation",
            () -> new MachineSubstationBlock(BlockProps.copy(Blocks.IRON_BLOCK).noOcclusion().isSuffocating((state, world, pos) -> false).strength(5.0F, 6.0F).sound(SoundType.STONE)));

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
            () -> new FluidDuctBlock(BlockProps.copy(Blocks.IRON_BLOCK).noOcclusion().strength(5.0F, 6.0F).sound(SoundType.STONE),
                    com.hbm_m.block.machines.PipeStyle.NEO));

    /** R7g: 1:1 FluidValve (von Hand, meta 1 = offen). */
    public static final RegistrySupplier<Block> FLUID_VALVE = registerBlockWithoutItem("fluid_valve",
            () -> new com.hbm_m.block.machines.FluidValveBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(5.0F, 6.0F).requiresCorrectToolForDrops().sound(SoundType.STONE), com.hbm_m.block.machines.FluidValveBlock.Mode.VALVE));

    public static final RegistrySupplier<Block> FLUID_PUMP = registerBlockWithoutItem("fluid_pump",
            () -> new com.hbm_m.block.machines.FluidPumpBlock(
                    BlockProps.copy(Blocks.IRON_BLOCK).noOcclusion().strength(5.0F, 6.0F).sound(SoundType.STONE)));

    public static final RegistrySupplier<Block> FLUID_EXHAUST = registerBlockWithoutItem("fluid_exhaust",
            () -> new com.hbm_m.block.machines.FluidExhaustBlock(
                    BlockProps.copy(Blocks.IRON_BLOCK).strength(2.0f).sound(SoundType.METAL).noOcclusion()));

    public static final RegistrySupplier<Block> HEATING_OVEN = registerBlock("heating_oven",
            () -> new HeatingOvenBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(4.0f, 4.0f).sound(SoundType.METAL).noOcclusion()));


    //---------------------------<ДВЕРИ>-------------------------------------

    public static final RegistrySupplier<DoorBlock> LARGE_VEHICLE_DOOR = registerBlockWithoutItem("large_vehicle_door",
            () -> new DoorBlock(
                    BlockBehaviour.Properties.of()
                            .requiresCorrectToolForDrops()
                            .noOcclusion()
                            .isViewBlocking((state, level, pos) -> false).strength(10.0F, 600.0F).sound(SoundType.STONE),
                    "large_vehicle_door"
            ));

    public static final RegistrySupplier<DoorBlock> ROUND_AIRLOCK_DOOR = registerBlockWithoutItem("round_airlock_door",
            () -> new DoorBlock(
                    BlockBehaviour.Properties.of()
                            .requiresCorrectToolForDrops()
                            .noOcclusion()
                            .isViewBlocking((state, level, pos) -> false).strength(10.0F, 600.0F).sound(SoundType.STONE),
                    "round_airlock_door"
            ));

    public static final RegistrySupplier<Block> TRANSITION_SEAL = registerBlockWithoutItem("transition_seal",
            () -> new TransitionSealBlock(
                    BlockBehaviour.Properties.of()
                            .requiresCorrectToolForDrops()
                            .noOcclusion()
                            .isViewBlocking((state, level, pos) -> false).strength(10.0F, 600.0F).sound(SoundType.STONE)));

    public static final RegistrySupplier<Block> FIRE_DOOR = registerBlockWithoutItem("fire_door",
            () -> new DoorBlock(
                    BlockBehaviour.Properties.of()
                            .requiresCorrectToolForDrops()
                            .noOcclusion()
                            .isViewBlocking((state, level, pos) -> false).strength(10.0F, 600.0F).sound(SoundType.STONE),
                    "fire_door"
            ));

    public static final RegistrySupplier<Block> SLIDE_DOOR = registerBlockWithoutItem("sliding_blast_door",
            () -> new DoorBlock(
                    BlockBehaviour.Properties.of()
                            .requiresCorrectToolForDrops()
                            .noOcclusion()
                            .isViewBlocking((state, level, pos) -> false).strength(10.0F, 450.0F).sound(SoundType.STONE),
                    "sliding_blast_door"
            ));

    public static final RegistrySupplier<Block> SLIDING_SEAL_DOOR = registerBlockWithoutItem("sliding_seal_door",
            () -> new DoorBlock(
                    BlockBehaviour.Properties.of()
                            .requiresCorrectToolForDrops()
                            .noOcclusion()
                            .isViewBlocking((state, level, pos) -> false).strength(10.0F, 600.0F).sound(SoundType.STONE),
                    "sliding_seal_door"
            ));

    public static final RegistrySupplier<Block> SECURE_ACCESS_DOOR = registerBlockWithoutItem("secure_access_door",
            () -> new DoorBlock(
                    BlockBehaviour.Properties.of()
                            .requiresCorrectToolForDrops()
                            .noOcclusion()
                            .isViewBlocking((state, level, pos) -> false).strength(20.0F, 1200.0F).sound(SoundType.STONE),
                    "secure_access_door"
            ));

    public static final RegistrySupplier<Block> QE_SLIDING = registerBlockWithoutItem("qe_sliding_door",
            () -> new DoorBlock(
                    BlockBehaviour.Properties.of()
                            .requiresCorrectToolForDrops()
                            .noOcclusion()
                            .isViewBlocking((state, level, pos) -> false).strength(10.0F, 600.0F).sound(SoundType.STONE),
                    "qe_sliding_door"
            ));

    public static final RegistrySupplier<Block> QE_CONTAINMENT = registerBlockWithoutItem("qe_containment_door",
            () -> new DoorBlock(
                    BlockBehaviour.Properties.of()
                            .requiresCorrectToolForDrops()
                            .noOcclusion()
                            .isViewBlocking((state, level, pos) -> false).strength(10.0F, 600.0F).sound(SoundType.STONE),
                    "qe_containment_door"
            ));

    public static final RegistrySupplier<Block> WATER_DOOR = registerBlockWithoutItem("water_door",
            () -> new DoorBlock(
                    BlockBehaviour.Properties.of()
                            .requiresCorrectToolForDrops()
                            .noOcclusion()
                            .isViewBlocking((state, level, pos) -> false).strength(5.0F, 30.0F).sound(SoundType.STONE),
                    "water_door"
            ));

    public static final RegistrySupplier<Block> SILO_HATCH = registerBlockWithoutItem("silo_hatch",
            () -> new DoorBlock(
                    BlockBehaviour.Properties.of()
                            .requiresCorrectToolForDrops()
                            .noOcclusion()
                            .isViewBlocking((state, level, pos) -> false).strength(10.0F, 60.0F).sound(SoundType.STONE),
                    "silo_hatch"
            ));

    public static final RegistrySupplier<Block> SILO_HATCH_LARGE = registerBlockWithoutItem("silo_hatch_large",
            () -> new DoorBlock(
                    BlockBehaviour.Properties.of()
                            .requiresCorrectToolForDrops()
                            .noOcclusion()
                            .isViewBlocking((state, level, pos) -> false).strength(10.0F, 60.0F).sound(SoundType.STONE),
                    "silo_hatch_large"
            ));

    public static final RegistrySupplier<DoorBlock> VAULT_DOOR = registerBlockWithoutItem("vault_door",
            () -> new DoorBlock(
                    BlockBehaviour.Properties.of()
                            .requiresCorrectToolForDrops()
                            .noOcclusion()
                            .isViewBlocking((state, level, pos) -> false).strength(10.0F, 600.0F).sound(SoundType.STONE),
                    "vault_door"
            ));

    public static final RegistrySupplier<Block> CARGO_DOOR = registerBlockWithoutItem("cargo_door",
            () -> new DoorBlock(
                    BlockBehaviour.Properties.of()
                            .requiresCorrectToolForDrops()
                            .noOcclusion()
                            .isViewBlocking((state, level, pos) -> false).strength(5.0F, 30.0F).sound(SoundType.STONE),
                    "cargo_door"
            ));


    //---------------------------<БЛОКИ>-------------------------------------
    public static final RegistrySupplier<Block> REINFORCED_STONE = registerBlock("reinforced_stone",
            () -> new Block(BlockProps.copy(Blocks.STONE).requiresCorrectToolForDrops().strength(15.0F, 60.0F)));

    public static final RegistrySupplier<Block> REINFORCED_GLASS = registerBlock("reinforced_glass",
            () -> com.hbm_m.platform.PlatformHooks.createGlassBlock(BlockProps.copy(Blocks.GLASS).requiresCorrectToolForDrops().strength(2.0F, 15.0F).sound(SoundType.STONE)));

    public static final RegistrySupplier<Block> MACHINE_SIREN = registerBlock("machine_siren",
            () -> new com.hbm_m.block.machines.MachineSirenBlock(BlockProps.copy(Blocks.IRON_BLOCK).noOcclusion().strength(5.0F, 6.0F).sound(SoundType.STONE)));

    public static final RegistrySupplier<Block> BROADCASTER = registerBlock("broadcaster",
            () -> new Block(BlockProps.copy(Blocks.IRON_BLOCK).strength(5.0F, 10.0F).requiresCorrectToolForDrops()));

    /** R6d: 1:1 BlockCrate. */
    public static final RegistrySupplier<Block> CRATE = registerBlock("crate",
            () -> new com.hbm_m.block.generic.CrateBlocks.Supply(BlockProps.copy(Blocks.OAK_PLANKS).strength(5.0F, 6.0F).sound(SoundType.WOOD), com.hbm_m.block.generic.CrateBlocks.Supply.Kind.SUPPLY));
    public static final RegistrySupplier<Block> CRATE_LEAD = registerBlock("crate_lead",
            () -> new com.hbm_m.block.generic.CrateBlocks.Supply(BlockProps.copy(Blocks.IRON_BLOCK).strength(5.0F, 6.0F).sound(SoundType.METAL).requiresCorrectToolForDrops(), com.hbm_m.block.generic.CrateBlocks.Supply.Kind.LEAD));
    public static final RegistrySupplier<Block> CRATE_METAL = registerBlock("crate_metal",
            () -> new com.hbm_m.block.generic.CrateBlocks.Supply(BlockProps.copy(Blocks.IRON_BLOCK).strength(5.0F, 6.0F).sound(SoundType.METAL).requiresCorrectToolForDrops(), com.hbm_m.block.generic.CrateBlocks.Supply.Kind.METAL));
    public static final RegistrySupplier<Block> CRATE_WEAPON = registerBlock("crate_weapon",
            () -> new com.hbm_m.block.generic.CrateBlocks.Supply(BlockProps.copy(Blocks.OAK_PLANKS).strength(5.0F, 6.0F).sound(SoundType.WOOD), com.hbm_m.block.generic.CrateBlocks.Supply.Kind.WEAPON));

    public static final RegistrySupplier<Block> CONCRETE_COLORED_EXT_HAZARD = registerBlock("concrete_colored_ext_hazard",
            () -> new Block(BlockProps.copy(Blocks.STONE).isValidSpawn((st, lvl, pos, type) -> false).strength(15F, 84.0F).sound(SoundType.STONE).requiresCorrectToolForDrops()));
    /** concrete_hazard: doppelte Port-ID von concrete_colored_ext@HAZARD (Umleitung in ForgeMainEvents.LEGACY_IDS). */
    public static final RegistrySupplier<Block> CONCRETE_HAZARD = CONCRETE_COLORED_EXT_HAZARD;
    public static final RegistrySupplier<Block> CONCRETE_HAZARD_STAIRS = registerBlock("concrete_hazard_stairs",
            () -> new StairBlock(ModBlocks.CONCRETE_HAZARD.get().defaultBlockState(),
                    BlockProps.copy(Blocks.IRON_BLOCK).sound(SoundType.STONE)));
    public static final RegistrySupplier<Block> CONCRETE_HAZARD_SLAB = registerBlock("concrete_hazard_slab",
            () -> new SlabBlock(BlockProps.copy(Blocks.IRON_BLOCK).sound(SoundType.STONE)));

    public static final RegistrySupplier<Block> BRICK_CONCRETE = registerBlock("brick_concrete",
            () -> new Block(BlockProps.copy(Blocks.STONE).isValidSpawn((st, lvl, pos, type) -> false).requiresCorrectToolForDrops().strength(15.0F, 96.0F)));
    public static final RegistrySupplier<Block> BRICK_CONCRETE_STAIRS = registerBlock("brick_concrete_stairs",
            () -> new StairBlock(ModBlocks.BRICK_CONCRETE.get().defaultBlockState(),
                    BlockProps.copy(Blocks.IRON_BLOCK).sound(SoundType.STONE).strength(15.0F, 96.0F)));
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
            () -> new Block(BlockProps.copy(Blocks.STONE).isValidSpawn((st, lvl, pos, type) -> false).requiresCorrectToolForDrops().strength(15.0F, 84.0F)));
    public static final RegistrySupplier<Block> CONCRETE_STAIRS = registerBlock("concrete_stairs",
            () -> new StairBlock(ModBlocks.CONCRETE.get().defaultBlockState(),
                    BlockProps.copy(Blocks.IRON_BLOCK).sound(SoundType.STONE).strength(15.0F, 84.0F)));
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
                    .sound(SoundType.STONE)
                    .requiresCorrectToolForDrops().strength(0.1F, 0.0F)));

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

    /** R6c: 1:1 BlockCrashedBomb (crashed_bomb), Untertypen als Bloecke. */
    public static final RegistrySupplier<Block> DUD_BALEFIRE = registerBlock("dud_balefire",
            () -> new com.hbm_m.block.bomb.CrashedBombBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(-1.0F, 3600.0F).sound(SoundType.STONE).noOcclusion(), com.hbm_m.block.bomb.CrashedBombBlock.DudType.BALEFIRE));
    public static final RegistrySupplier<Block> DUD_CONVENTIONAL = registerBlock("dud_conventional",
            () -> new com.hbm_m.block.bomb.CrashedBombBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(-1.0F, 3600.0F).sound(SoundType.STONE).noOcclusion(), com.hbm_m.block.bomb.CrashedBombBlock.DudType.CONVENTIONAL));
    public static final RegistrySupplier<Block> DUD_NUKE = registerBlock("dud_nuke",
            () -> new com.hbm_m.block.bomb.CrashedBombBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(-1.0F, 3600.0F).sound(SoundType.STONE).noOcclusion(), com.hbm_m.block.bomb.CrashedBombBlock.DudType.NUKE));
    public static final RegistrySupplier<Block> DUD_SALTED = registerBlock("dud_salted",
            () -> new com.hbm_m.block.bomb.CrashedBombBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(-1.0F, 3600.0F).sound(SoundType.STONE).noOcclusion(), com.hbm_m.block.bomb.CrashedBombBlock.DudType.SALTED));



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
            () -> new com.hbm_m.block.decorations.SpotlightBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(0.5F, 0.5F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion(), 2, com.hbm_m.block.decorations.SpotlightBlock.LightType.INCANDESCENT, true, () -> ModBlocks.CAGE_LAMP_OFF.get(), () -> ModBlocks.CAGE_LAMP.get()));

    public static final RegistrySupplier<Block> FLOOD_LAMP = registerBlock("flood_lamp",
            () -> new com.hbm_m.block.decorations.SpotlightBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(0.5F, 0.5F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion(), 32, com.hbm_m.block.decorations.SpotlightBlock.LightType.HALOGEN, true, () -> ModBlocks.FLOOD_LAMP_OFF.get(), () -> ModBlocks.FLOOD_LAMP.get()));

    public static final RegistrySupplier<Block> C4 = registerBlock("c4",
            // audit10: 1:1 BlockC4 (BlockTNTBase, Explosion 15) statt der Eigenbau-C4Block-Krater-Logik
            () -> new com.hbm_m.block.bomb.TNTBlocks.C4(BlockBehaviour.Properties.of().strength(0.0F, 0.0F).sound(SoundType.GRASS)));

    public static final RegistrySupplier<Block> DECO_STEEL = registerBlock("deco_steel",
            () -> new Block(BlockBehaviour.Properties.of()
                    .sound(SoundType.STONE)
                    .requiresCorrectToolForDrops().strength(5.0F, 6.0F)));

    public static final RegistrySupplier<Block> DECO_RUSTY_STEEL = registerBlock("deco_rusty_steel",
            () -> new Block(BlockBehaviour.Properties.of()
                    .sound(SoundType.STONE)
                    .requiresCorrectToolForDrops().strength(5.0F, 6.0F)));

    public static final RegistrySupplier<Block> DECO_TUNGSTEN = registerBlock("deco_tungsten",
            () -> new Block(BlockBehaviour.Properties.of()
                    .sound(SoundType.STONE)
                    .requiresCorrectToolForDrops().strength(5.0F, 6.0F)));

    public static final RegistrySupplier<Block> DECO_RED_COPPER = registerBlock("deco_red_copper",
            () -> new Block(BlockBehaviour.Properties.of()
                    .sound(SoundType.STONE)
                    .requiresCorrectToolForDrops().strength(5.0F, 6.0F)));

    public static final RegistrySupplier<Block> DECO_ALUMINUM = registerBlock("deco_aluminum",
            () -> new Block(BlockBehaviour.Properties.of()
                    .sound(SoundType.STONE)
                    .requiresCorrectToolForDrops().strength(5.0F, 6.0F)));

    public static final RegistrySupplier<Block> DECO_BERYLLIUM = registerBlock("deco_beryllium",
            () -> new Block(BlockBehaviour.Properties.of()
                    .sound(SoundType.STONE)
                    .requiresCorrectToolForDrops().strength(5.0F, 6.0F)));

    public static final RegistrySupplier<Block> DECO_LEAD = registerBlock("deco_lead",
            () -> new Block(BlockBehaviour.Properties.of()
                    .sound(SoundType.STONE)
                    .requiresCorrectToolForDrops().strength(5.0F, 6.0F)));

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
            () -> new com.hbm_m.block.generic.BlockModDoor(BlockProps.copy(Blocks.IRON_DOOR).requiresCorrectToolForDrops().noOcclusion().strength(10.0F, 60.0F).sound(SoundType.STONE)));

    public static final RegistrySupplier<Block> DOOR_OFFICE = registerBlock("door_office",
            () -> new com.hbm_m.block.generic.BlockModDoor(BlockProps.copy(Blocks.IRON_DOOR).requiresCorrectToolForDrops().noOcclusion().strength(10.0F, 6.0F).sound(SoundType.STONE)));

    /** Original {@code door_metal} (Port-ID war metal_door). */
    public static final RegistrySupplier<Block> METAL_DOOR = registerBlock("door_metal",
            () -> new com.hbm_m.block.generic.BlockModDoor(BlockProps.copy(Blocks.IRON_DOOR).requiresCorrectToolForDrops().noOcclusion().strength(5.0F, 3.0F).sound(SoundType.STONE)));

    /** Порт {@code door_red} (1.7.10) — дверь скрытой красной комнаты. */
    public static final RegistrySupplier<Block> DOOR_RED_BLOCK = registerBlockWithoutItem("door_red",
            () -> new com.hbm_m.block.generic.BlockModDoor(BlockProps.copy(Blocks.IRON_DOOR).requiresCorrectToolForDrops().noOcclusion().strength(10.0F, 60.0F).sound(SoundType.STONE)));


    // ============ ТЕХНИЧЕСКИЕ И ДЕКОРАТИВНЫЕ БЛОКИ ============

    public static final RegistrySupplier<Block> DORNIER = registerBlock("dornier",
            () -> new BarrelBlock(BlockProps.copy(Blocks.STONE).strength(1.5F, 6.0F).noOcclusion()));

    public static final RegistrySupplier<Block> ORE_OIL = registerBlock("ore_oil",
            () -> new com.hbm_m.block.generic.BlockOre(BlockProps.copy(Blocks.STONE).strength(5.0F, 6.0F).sound(SoundType.STONE).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> ORE_OIL_EMPTY = registerBlock("ore_oil_empty",
            () -> new Block(BlockProps.copy(Blocks.STONE).noOcclusion().strength(5.0F, 6.0F)));

    // ================== ДЕКОР СТРУКТУР (порт декоративных блоков 1.7.10) ==================
    // Блоки, используемые в структурах из оригинального 1.7.10; конвертер структур
    // (tools/structure_converter/convert.py) ссылается на эти реестровые имена.
    // Порт по мотивам: block_meteor*, block_copper, deco_pipe* и пр. из com.hbm.blocks.ModBlocks.

    private static final BlockBehaviour.Properties STRUCTURE_DECOR_STONE =
            BlockProps.copy(Blocks.STONE).strength(3.0F, 9.0F).requiresCorrectToolForDrops();
    private static final BlockBehaviour.Properties STRUCTURE_DECOR_METAL =
            BlockProps.copy(Blocks.IRON_BLOCK).strength(5.0F, 10.0F).requiresCorrectToolForDrops();

    public static final RegistrySupplier<Block> BLOCK_METEOR = registerBlock("block_meteor",
            () -> new com.hbm_m.block.generic.BlockOre(BlockProps.copy(Blocks.STONE).strength(15.0F, 216.0F).sound(SoundType.STONE).requiresCorrectToolForDrops()));
    public static final RegistrySupplier<Block> BLOCK_METEOR_COBBLE = registerBlock("block_meteor_cobble",
            () -> new com.hbm_m.block.generic.BlockOre(BlockProps.copy(Blocks.STONE).strength(15.0F, 216.0F).sound(SoundType.STONE).requiresCorrectToolForDrops()));
    // NOTE: "block_red_copper"/"block_starmetal" уже регистрируются автоциклом слитковых блоков
    // (форма BLOCK в ModMaterials, static {} выше) — здесь только алиасы, иначе дубликат имени
    // не биндится и runData падает ("Registry Object not present").
    public static final RegistrySupplier<Block> BLOCK_COPPER = registerBlock("block_copper",
            () -> new Block(BlockProps.copy(Blocks.IRON_BLOCK).strength(5.0F, 12.0F).sound(SoundType.METAL).requiresCorrectToolForDrops()));
    public static final RegistrySupplier<Block> BLOCK_RED_COPPER = getIngotBlock(ModMaterials.RED_COPPER);
    public static final RegistrySupplier<Block> BLOCK_SCRAP = registerBlock("block_scrap",
            () -> new net.minecraft.world.level.block.FallingBlock(BlockProps.copy(Blocks.SAND).strength(2.5F, 3.0F).sound(SoundType.GRAVEL)));
    public static final RegistrySupplier<Block> BLOCK_ELECTRICAL_SCRAP = registerBlock("block_electrical_scrap",
            () -> new net.minecraft.world.level.block.FallingBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(2.5F, 3.0F).sound(SoundType.METAL).requiresCorrectToolForDrops()));
    public static final RegistrySupplier<Block> BLOCK_STARMETAL = getIngotBlock(ModMaterials.STARMETAL);

    public static final RegistrySupplier<Block> DECO_TITANIUM = registerBlock("deco_titanium",
            () -> new com.hbm_m.block.generic.BlockOre(BlockProps.copy(Blocks.IRON_BLOCK).strength(5.0F, 6.0F).requiresCorrectToolForDrops().sound(SoundType.STONE)));
    /** R6d: 1:1 BlockWoodStructure (Dach/Geruest/Decke). */
    public static final RegistrySupplier<Block> WOOD_STRUCTURE = registerBlock("wood_structure",
            () -> new com.hbm_m.block.generic.R6dBlocks.WoodStructure(BlockProps.copy(Blocks.OAK_PLANKS).strength(5.0F, 9.0F).sound(SoundType.WOOD).noOcclusion(), com.hbm_m.block.generic.R6dBlocks.WoodStructure.Type.ROOF));
    public static final RegistrySupplier<Block> WOOD_STRUCTURE_SCAFFOLD = registerBlock("wood_structure_scaffold",
            () -> new com.hbm_m.block.generic.R6dBlocks.WoodStructure(BlockProps.copy(Blocks.OAK_PLANKS).strength(5.0F, 9.0F).sound(SoundType.WOOD).noOcclusion(), com.hbm_m.block.generic.R6dBlocks.WoodStructure.Type.SCAFFOLD));
    public static final RegistrySupplier<Block> WOOD_STRUCTURE_CEILING = registerBlock("wood_structure_ceiling",
            () -> new com.hbm_m.block.generic.R6dBlocks.WoodStructure(BlockProps.copy(Blocks.OAK_PLANKS).strength(5.0F, 9.0F).sound(SoundType.WOOD).noOcclusion(), com.hbm_m.block.generic.R6dBlocks.WoodStructure.Type.CEILING));

    /** R6b: 1:1 DecoBlock steel_beam (beam.obj, ohne Drehung). */
    public static final RegistrySupplier<Block> STEEL_BEAM = registerBlock("steel_beam",
            () -> new com.hbm_m.block.decorations.DecoObjBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(5F, 9.0F).sound(SoundType.STONE).requiresCorrectToolForDrops().noOcclusion(), com.hbm_m.block.decorations.DecoObjBlock.Placement.NONE, d -> Shapes.box(7 / 16.0, 0, 7 / 16.0, 9 / 16.0, 1, 9 / 16.0)));
    /** R6b: 1:1 BlockChain (dungeon_chain). */
    public static final RegistrySupplier<Block> DUNGEON_CHAIN = registerBlock("dungeon_chain",
            () -> new com.hbm_m.block.decorations.DungeonChainBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(0.25F, 1.2F).sound(SoundType.STONE).requiresCorrectToolForDrops().noOcclusion().noCollission()));
    public static final RegistrySupplier<Block> STEEL_GRATE = registerBlock("steel_grate",
            () -> new GrateBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(2F, 3.0F).sound(com.hbm_m.sound.ModSoundTypes.GRATE).requiresCorrectToolForDrops().noOcclusion(), false));
    public static final RegistrySupplier<Block> STEEL_GRATE_WIDE = registerBlock("steel_grate_wide",
            () -> new GrateBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(2F, 3.0F).sound(com.hbm_m.sound.ModSoundTypes.GRATE).requiresCorrectToolForDrops().noOcclusion(), true));

    /** R6b: 1:1 DecoBlock steel_corner. */
    public static final RegistrySupplier<Block> STEEL_CORNER = registerBlock("steel_corner",
            () -> new com.hbm_m.block.decorations.SteelCornerBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(15F, 9.0F).sound(SoundType.STONE).requiresCorrectToolForDrops().noOcclusion()));

    /** R6c: 1:1 Balefire (BlockFire), kein Kreativ-Reiter. */
    public static final RegistrySupplier<Block> BALEFIRE = registerBlock("balefire",
            () -> new com.hbm_m.block.bomb.BalefireBlock(BlockBehaviour.Properties.copy(Blocks.FIRE).lightLevel(s -> 15).noLootTable().sound(SoundType.STONE)));

    public static final RegistrySupplier<Block> PLANT_DEAD = registerBlock("plant_dead",
            () -> new com.hbm_m.block.generic.BlockDeadPlant(BlockProps.copy(Blocks.GRASS).strength(0.0F, 0.0F).sound(SoundType.GRASS).noCollission().instabreak().noOcclusion()));

    /** R6b: 1:1 DecoPoleSatelliteReceiver. */
    public static final RegistrySupplier<Block> POLE_SATELLITE_RECEIVER = registerBlock("pole_satellite_receiver",
            () -> new com.hbm_m.block.decorations.DecoObjBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(5F, 9.0F).sound(SoundType.STONE).requiresCorrectToolForDrops().noOcclusion(), com.hbm_m.block.decorations.DecoObjBlock.Placement.OPPOSITE));

    /** Порт {@code deco_loot} (BlockLoot из 1.7.10) — невидимая груда лута: хитбокс-«коврик»,
     *  предметы лежат внутри (BER), при ломании/ПКМ выпадают. */
    public static final RegistrySupplier<Block> DECO_LOOT = registerBlock("deco_loot",
            () -> new com.hbm_m.block.decorations.DecoLootBlock(BlockProps.copy(Blocks.GRAVEL).requiresCorrectToolForDrops().noOcclusion().strength(0.0F, 0.0F).sound(SoundType.STONE)));

    /** R6b: Deko-Rohre 1:1 (BlockPipe): Material.iron, Haerte 2, Widerstand 5, Klang grate. */
    private static RegistrySupplier<Block> pipePillar(String name) {
        return registerBlock(name, () -> new com.hbm_m.block.decorations.DecoPipeBlock(BlockProps.copy(Blocks.IRON_BLOCK)
                .strength(2.0F, 3.0F).sound(com.hbm_m.sound.ModSoundTypes.GRATE).requiresCorrectToolForDrops().noOcclusion()));
    }
    public static final RegistrySupplier<Block> DECO_PIPE                     = pipePillar("deco_pipe");
    public static final RegistrySupplier<Block> DECO_PIPE_RUSTED              = pipePillar("deco_pipe_rusted");
    public static final RegistrySupplier<Block> DECO_PIPE_GREEN               = pipePillar("deco_pipe_green");
    public static final RegistrySupplier<Block> DECO_PIPE_GREEN_RUSTED        = pipePillar("deco_pipe_green_rusted");
    public static final RegistrySupplier<Block> DECO_PIPE_RED                 = pipePillar("deco_pipe_red");
    public static final RegistrySupplier<Block> DECO_PIPE_MARKED              = pipePillar("deco_pipe_marked");
    public static final RegistrySupplier<Block> DECO_PIPE_RIM                 = pipePillar("deco_pipe_rim");
    public static final RegistrySupplier<Block> DECO_PIPE_RIM_RUSTED          = pipePillar("deco_pipe_rim_rusted");
    public static final RegistrySupplier<Block> DECO_PIPE_RIM_GREEN           = pipePillar("deco_pipe_rim_green");
    public static final RegistrySupplier<Block> DECO_PIPE_RIM_GREEN_RUSTED    = pipePillar("deco_pipe_rim_green_rusted");
    public static final RegistrySupplier<Block> DECO_PIPE_RIM_RED             = pipePillar("deco_pipe_rim_red");
    public static final RegistrySupplier<Block> DECO_PIPE_RIM_MARKED          = pipePillar("deco_pipe_rim_marked");
    public static final RegistrySupplier<Block> DECO_PIPE_FRAMED              = pipePillar("deco_pipe_framed");
    public static final RegistrySupplier<Block> DECO_PIPE_FRAMED_RUSTED       = pipePillar("deco_pipe_framed_rusted");
    public static final RegistrySupplier<Block> DECO_PIPE_FRAMED_GREEN        = pipePillar("deco_pipe_framed_green");
    public static final RegistrySupplier<Block> DECO_PIPE_FRAMED_GREEN_RUSTED = pipePillar("deco_pipe_framed_green_rusted");
    public static final RegistrySupplier<Block> DECO_PIPE_FRAMED_RED          = pipePillar("deco_pipe_framed_red");
    public static final RegistrySupplier<Block> DECO_PIPE_FRAMED_MARKED       = pipePillar("deco_pipe_framed_marked");
    public static final RegistrySupplier<Block> DECO_PIPE_QUAD                = pipePillar("deco_pipe_quad");
    public static final RegistrySupplier<Block> DECO_PIPE_QUAD_RUSTED         = pipePillar("deco_pipe_quad_rusted");
    public static final RegistrySupplier<Block> DECO_PIPE_QUAD_GREEN          = pipePillar("deco_pipe_quad_green");
    public static final RegistrySupplier<Block> DECO_PIPE_QUAD_GREEN_RUSTED   = pipePillar("deco_pipe_quad_green_rusted");
    public static final RegistrySupplier<Block> DECO_PIPE_QUAD_RED            = pipePillar("deco_pipe_quad_red");
    public static final RegistrySupplier<Block> DECO_PIPE_QUAD_MARKED         = pipePillar("deco_pipe_quad_marked");

    // ================== КОНЕЦ ДЕКОРА СТРУКТУР ==================

    public static final RegistrySupplier<Block> BEDROCK_OIL = registerBlock("bedrock_oil",
            () -> new Block(BlockProps.copy(Blocks.BEDROCK).noOcclusion()));

    public static final RegistrySupplier<Block> ORE_BEDROCK_OIL = registerBlock("ore_bedrock_oil",
            () -> new Block(BlockProps.copy(Blocks.BEDROCK).isValidSpawn((st, lvl, pos, type) -> false).requiresCorrectToolForDrops()));

    /** Mineralisches Bedrock-Erz (Mining-Drill-Ziel), siehe {@link com.hbm_m.block.nature.OreBedrockBlock}. */
    public static final RegistrySupplier<Block> ORE_BEDROCK = registerBlock("ore_bedrock_mineral",
            () -> new com.hbm_m.block.nature.OreBedrockBlock(BlockProps.copy(Blocks.BEDROCK)));

    public static final RegistrySupplier<Block> DEPTH_STONE = registerBlock("depth_stone",
            () -> new DepthOreBlock(BlockProps.copy(Blocks.DEEPSLATE).strength(-1.0F, 6.0F).noOcclusion()));
    public static final RegistrySupplier<Block> DEPTH_BORAX = registerBlock("depth_borax",
            () -> new DepthOreBlock(BlockProps.copy(Blocks.DEEPSLATE).strength(-1.0F, 6.0F).noOcclusion()));
    public static final RegistrySupplier<Block> DEPTH_CINNABAR = registerBlock("depth_cinnabar",
            () -> new DepthOreBlock(BlockProps.copy(Blocks.DEEPSLATE).strength(-1.0F, 6.0F).noOcclusion()));
    public static final RegistrySupplier<Block> DEPTH_IRON = registerBlock("depth_iron",
            () -> new DepthOreBlock(BlockProps.copy(Blocks.DEEPSLATE).strength(-1.0F, 6.0F).noOcclusion()));
    public static final RegistrySupplier<Block> DEPTH_TUNGSTEN = registerBlock("depth_tungsten",
            () -> new DepthOreBlock(BlockProps.copy(Blocks.DEEPSLATE).strength(-1.0F, 6.0F).noOcclusion()));
    public static final RegistrySupplier<Block> DEPTH_TITANIUM = registerBlock("depth_titanium",
            () -> new DepthOreBlock(BlockProps.copy(Blocks.DEEPSLATE).strength(-1.0F, 6.0F).noOcclusion()));
    public static final RegistrySupplier<Block> DEPTH_ZIRCONIUM = registerBlock("depth_zirconium",
            () -> new DepthOreBlock(BlockProps.copy(Blocks.DEEPSLATE).strength(-1.0F, 6.0F).noOcclusion()));

    /** R6b: 1:1 filing_cabinet (BlockDecoContainer, DecoCabinetEnum GREEN/STEEL). */
    public static final RegistrySupplier<Block> FILE_CABINET = registerBlock("file_cabinet",
            () -> new com.hbm_m.block.decorations.FileCabinetBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(10F, 9.0F).sound(SoundType.STONE).requiresCorrectToolForDrops().noOcclusion(), false));
    public static final RegistrySupplier<Block> FILE_CABINET_STEEL = registerBlock("file_cabinet_steel",
            () -> new com.hbm_m.block.decorations.FileCabinetBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(10F, 9.0F).sound(SoundType.STONE).requiresCorrectToolForDrops().noOcclusion(), true));

    public static final RegistrySupplier<Block> REBAR = registerBlock("rebar",
            () -> new com.hbm_m.block.generic.BlockRebar(BlockProps.copy(Blocks.IRON_BLOCK).strength(15.0F, 12.0F).requiresCorrectToolForDrops().noOcclusion().sound(SoundType.STONE)));
    /** R6b: 1:1 steel_poles (DecoSteelPoles). */
    public static final RegistrySupplier<Block> STEEL_POLE = registerBlock("steel_pole",
            () -> new com.hbm_m.block.decorations.DecoObjBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(5F, 9.0F).sound(SoundType.STONE).requiresCorrectToolForDrops().noOcclusion(), com.hbm_m.block.decorations.DecoObjBlock.Placement.OPPOSITE));
    /** 1:1 statue_elb_f (DecoBlockAlt): unzerstoerbar, leuchtet (1.0F), nur per rotem Schluessel abbaubar; kein Kreativreiter. */
    public static final RegistrySupplier<Block> STATUE_ELB_F = registerBlock("statue_elb_f",
            () -> new com.hbm_m.block.decorations.StatueBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(-1.0F, 3600000.0F).lightLevel(s -> 15).noOcclusion()));
    /** R6b: 1:1 pole_top (DecoPoleTop). */
    public static final RegistrySupplier<Block> ANTENNA_TOP = registerBlock("antenna_top",
            () -> new com.hbm_m.block.decorations.DecoObjBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(5F, 9.0F).sound(SoundType.STONE).requiresCorrectToolForDrops().noOcclusion(), com.hbm_m.block.decorations.DecoObjBlock.Placement.NONE));
    /** R6b: 1:1 deco_computer (BlockDecoModel, IBM 300PL). */
    public static final RegistrySupplier<Block> PUTER = registerBlock("puter",
            () -> new com.hbm_m.block.decorations.DecoObjBlock(BlockProps.copy(Blocks.IRON_BLOCK).sound(SoundType.STONE).requiresCorrectToolForDrops().noOcclusion().strength(5.0F, 6.0F), com.hbm_m.block.decorations.DecoObjBlock.Placement.OPPOSITE, com.hbm_m.block.decorations.DecoObjBlock.decoModelBounds(0.125, 0, 0, 0.875, 0.875, 0.625)));
    /** R6b: 1:1 DecoBlock steel_wall. */
    public static final RegistrySupplier<Block> STEEL_WALL = registerBlock("steel_wall",
            () -> new SteelWallBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(5F, 9.0F).sound(SoundType.STONE).requiresCorrectToolForDrops().noOcclusion()));

    public static final RegistrySupplier<Block> B29 = registerBlock("b29",
            () -> new BarrelBlock(BlockProps.copy(Blocks.STONE).strength(1.5F, 6.0F).noOcclusion()));

    public static final RegistrySupplier<Block> MINE_FAT = registerBlock("mine_fat",
            () -> new LandmineBlock(BlockProps.copy(Blocks.STONE).noOcclusion().strength(1.0F, 1.0F), 2.5D, 1D));

    public static final RegistrySupplier<Block> NUKE_FAT_MAN = registerBlockWithoutItem("nuke_fat_man",
            () -> new NukeFatManBlock(BlockProps.copy(Blocks.STONE).strength(1.5F, 6.0F).noOcclusion()));

    public static final RegistrySupplier<Block> NUKE_PROTOTYPE = registerBlockWithoutItem("nuke_prototype",
            () -> new com.hbm_m.block.bomb.NukePrototypeBlock(BlockProps.copy(Blocks.STONE).noOcclusion().strength(5.0F, 120.0F)));

    public static final RegistrySupplier<Block> NUKE_GADGET = registerBlock("nuke_gadget",
            () -> new com.hbm_m.block.bomb.LargeNukeBlock(com.hbm_m.block.bomb.LargeNukeType.GADGET,
                    BlockProps.copy(Blocks.STONE).noOcclusion().strength(5.0F, 120.0F)));

    public static final RegistrySupplier<Block> NUKE_BOY = registerBlock("nuke_boy",
            () -> new com.hbm_m.block.bomb.LargeNukeBlock(com.hbm_m.block.bomb.LargeNukeType.BOY,
                    BlockProps.copy(Blocks.STONE).noOcclusion().strength(5.0F, 120.0F)));

    public static final RegistrySupplier<Block> NUKE_MIKE = registerBlock("nuke_mike",
            () -> new com.hbm_m.block.bomb.LargeNukeBlock(com.hbm_m.block.bomb.LargeNukeType.MIKE,
                    BlockProps.copy(Blocks.STONE).noOcclusion().strength(5.0F, 120.0F)));

    public static final RegistrySupplier<Block> NUKE_TSAR = registerBlock("nuke_tsar",
            () -> new com.hbm_m.block.bomb.LargeNukeBlock(com.hbm_m.block.bomb.LargeNukeType.TSAR,
                    BlockProps.copy(Blocks.STONE).noOcclusion().strength(5.0F, 120.0F)));

    public static final RegistrySupplier<Block> NUKE_FLEIJA = registerBlock("nuke_fleija",
            () -> new com.hbm_m.block.bomb.NukeFleijaBlock(BlockProps.copy(Blocks.STONE).noOcclusion().strength(5.0F, 120.0F)));

    public static final RegistrySupplier<Block> MINE_AP = registerBlock("mine_ap",
            () -> new com.hbm_m.block.bomb.LandmineAPBlock(BlockProps.copy(Blocks.STONE).noOcclusion().strength(1.0F, 1.0F), 1.5D, 1D));

    public static final RegistrySupplier<Block> CRATE_CONSERVE = registerBlock("crate_conserve",
            () -> new CrtBlock(BlockProps.copy(Blocks.STONE).strength(1.5F, 6.0F).noOcclusion()));
    /** R6b: 1:1 DecoTapeRecorder. */
    public static final RegistrySupplier<Block> TAPE_RECORDER = registerBlock("tape_recorder",
            () -> new com.hbm_m.block.decorations.DecoObjBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(5F, 9.0F).sound(SoundType.STONE).requiresCorrectToolForDrops().noOcclusion(), com.hbm_m.block.decorations.DecoObjBlock.Placement.OPPOSITE));

    public static final RegistrySupplier<Block> BARREL_LOX = registerBlock("barrel_lox",
            () -> new com.hbm_m.block.generic.RedBarrelBlock(BlockProps.copy(Blocks.IRON_BLOCK).noOcclusion().strength(0.5F, 1.5F).sound(SoundType.STONE), false));
    public static final RegistrySupplier<Block> BARREL_CORRODED = registerBlock("barrel_corroded",
            () -> new com.hbm_m.block.machines.BarrelTankBlock(BlockProps.copy(Blocks.STONE).noOcclusion().strength(2.0F, 3.0F).sound(SoundType.METAL),
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
            () -> new com.hbm_m.block.generic.RedBarrelBlock(BlockProps.copy(Blocks.IRON_BLOCK).noOcclusion().strength(0.5F, 1.5F).sound(SoundType.STONE), true));
    public static final RegistrySupplier<Block> BARREL_PLASTIC = registerBlock("barrel_plastic",
            () -> new com.hbm_m.block.machines.BarrelTankBlock(BlockProps.copy(Blocks.STONE).noOcclusion().strength(2.0F, 3.0F),
                    com.hbm_m.blockentity.machines.BarrelPlasticBlockEntity::new,
                    () -> com.hbm_m.blockentity.ModBlockEntities.BARREL_PLASTIC_BE.get(),
                    new com.hbm_m.block.machines.BarrelTankBlock.TooltipInfo(
                            com.hbm_m.blockentity.machines.BarrelPlasticBlockEntity.CAPACITY,
                            false, false, false, false)));
    public static final RegistrySupplier<Block> BARREL_RED = registerBlock("barrel_red",
            () -> new com.hbm_m.block.generic.RedBarrelBlock(BlockProps.copy(Blocks.IRON_BLOCK).noOcclusion().strength(0.5F, 1.5F).sound(SoundType.STONE), true));
    public static final RegistrySupplier<Block> BARREL_STEEL = registerBlock("barrel_steel",
            () -> new com.hbm_m.block.machines.BarrelTankBlock(BlockProps.copy(Blocks.STONE).noOcclusion().strength(2.0F, 3.0F).sound(SoundType.METAL),
                    com.hbm_m.blockentity.machines.BarrelSteelBlockEntity::new,
                    () -> com.hbm_m.blockentity.ModBlockEntities.BARREL_STEEL_BE.get(),
                    new com.hbm_m.block.machines.BarrelTankBlock.TooltipInfo(
                            com.hbm_m.blockentity.machines.BarrelSteelBlockEntity.CAPACITY,
                            true, true, false, false)));
    public static final RegistrySupplier<Block> BARREL_TAINT = registerBlock("barrel_taint",
            () -> new com.hbm_m.block.generic.RedBarrelBlock(BlockProps.copy(Blocks.IRON_BLOCK).noOcclusion().strength(0.5F, 1.5F).sound(SoundType.STONE), false));

    /** Блок заражения (боеголовка MissileTaint). */
    public static final RegistrySupplier<Block> TAINT = registerBlock("taint",
            () -> new BlockTaint(BlockBehaviour.Properties.of().requiresCorrectToolForDrops()
                    .mapColor(MapColor.COLOR_GRAY)
                    .randomTicks()
                    .noLootTable().strength(15.0F, 6.0F)));
    public static final RegistrySupplier<Block> BARREL_TCALLOY = registerBlock("barrel_tcalloy",
            () -> new com.hbm_m.block.machines.BarrelTankBlock(BlockProps.copy(Blocks.STONE).noOcclusion().strength(2.0F, 3.0F).sound(SoundType.METAL),
                    com.hbm_m.blockentity.machines.BarrelTcalloyBlockEntity::new,
                    () -> com.hbm_m.blockentity.ModBlockEntities.BARREL_TCALLOY_BE.get(),
                    new com.hbm_m.block.machines.BarrelTankBlock.TooltipInfo(
                            com.hbm_m.blockentity.machines.BarrelTcalloyBlockEntity.CAPACITY,
                            true, true, true, false)));
    public static final RegistrySupplier<Block> BARREL_VITRIFIED = registerBlock("vitrified_barrel",
            () -> new com.hbm_m.block.generic.BarrelYellowBlock(BlockProps.copy(Blocks.IRON_BLOCK).noOcclusion().strength(0.5F, 1.5F).sound(SoundType.STONE)));
    /** Original yellow_barrel (YellowBarrel). Fruehere Port-ID "barrel_yellow" wird in ForgeMainEvents umgeleitet. */
    public static final RegistrySupplier<Block> BARREL_YELLOW = registerBlock("yellow_barrel",
            () -> new com.hbm_m.block.generic.BarrelYellowBlock(BlockProps.copy(Blocks.IRON_BLOCK).noOcclusion().strength(0.5F, 1.5F).sound(SoundType.STONE)));
    public static final RegistrySupplier<Block> BARREL_ANTIMATTER = registerBlock("barrel_antimatter",
            () -> new com.hbm_m.block.machines.BarrelTankBlock(BlockProps.copy(Blocks.STONE).noOcclusion().strength(2.0F, 3.0F).sound(SoundType.METAL),
                    com.hbm_m.blockentity.machines.BarrelAntimatterBlockEntity::new,
                    () -> com.hbm_m.blockentity.ModBlockEntities.BARREL_ANTIMATTER_BE.get(),
                    new com.hbm_m.block.machines.BarrelTankBlock.TooltipInfo(
                            com.hbm_m.blockentity.machines.BarrelAntimatterBlockEntity.CAPACITY,
                            true, true, true, true)));

    public static final RegistrySupplier<Block> BARBED_WIRE = registerBlock("barbed_wire",
            () -> new com.hbm_m.block.weapons.BarbedWireBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(5.0F, 6.0F).requiresCorrectToolForDrops().noOcclusion().noCollission().sound(SoundType.STONE)));
    public static final RegistrySupplier<Block> BARBED_WIRE_FIRE = registerBlock("barbed_wire_fire",
            () -> new com.hbm_m.block.weapons.BarbedWireBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(5.0F, 6.0F).requiresCorrectToolForDrops().noOcclusion().noCollission().sound(SoundType.STONE)));
    public static final RegistrySupplier<Block> BARBED_WIRE_WITHER = registerBlock("barbed_wire_wither",
            () -> new com.hbm_m.block.weapons.BarbedWireBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(5.0F, 6.0F).requiresCorrectToolForDrops().noOcclusion().noCollission().sound(SoundType.STONE)));
    public static final RegistrySupplier<Block> BARBED_WIRE_POISON = registerBlock("barbed_wire_poison",
            () -> new com.hbm_m.block.weapons.BarbedWireBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(5.0F, 6.0F).requiresCorrectToolForDrops().noOcclusion().noCollission().sound(SoundType.STONE)));
    public static final RegistrySupplier<Block> BARBED_WIRE_RAD = registerBlock("barbed_wire_rad",
            () -> new BarbedWireRadBlock(BlockProps.copy(Blocks.STONE).strength(1.5F, 6.0F).noOcclusion()));

    /** R6b: 1:1 deco_toaster (BlockDecoToaster): Eisen, Stahl, Holz. */
    public static final RegistrySupplier<Block> TOASTER = registerBlock("toaster",
            () -> new com.hbm_m.block.decorations.DecoObjBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(5F, 6.0F).sound(SoundType.STONE).requiresCorrectToolForDrops().noOcclusion(), com.hbm_m.block.decorations.DecoObjBlock.Placement.LOOK, com.hbm_m.block.decorations.DecoObjBlock::toasterBounds));
    public static final RegistrySupplier<Block> TOASTER_STEEL = registerBlock("toaster_steel",
            () -> new com.hbm_m.block.decorations.DecoObjBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(5F, 6.0F).sound(SoundType.STONE).requiresCorrectToolForDrops().noOcclusion(), com.hbm_m.block.decorations.DecoObjBlock.Placement.LOOK, com.hbm_m.block.decorations.DecoObjBlock::toasterBounds));
    public static final RegistrySupplier<Block> TOASTER_WOOD = registerBlock("toaster_wood",
            () -> new com.hbm_m.block.decorations.DecoObjBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(5F, 6.0F).sound(SoundType.STONE).requiresCorrectToolForDrops().noOcclusion(), com.hbm_m.block.decorations.DecoObjBlock.Placement.LOOK, com.hbm_m.block.decorations.DecoObjBlock::toasterBounds));
    /** R6b: 1:1 deco_crt (BlockDecoCRT), je Untertyp ein Block; blinking/bsod mit leuchtendem Schirm. */
    public static final RegistrySupplier<Block> CRT_CLEAN = registerBlock("crt_clean",
            () -> new com.hbm_m.block.decorations.DecoObjBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(5F, 6.0F).sound(SoundType.STONE).requiresCorrectToolForDrops().noOcclusion(), com.hbm_m.block.decorations.DecoObjBlock.Placement.LOOK));
    public static final RegistrySupplier<Block> CRT_BROKEN = registerBlock("crt_broken",
            () -> new com.hbm_m.block.decorations.DecoObjBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(5F, 6.0F).sound(SoundType.STONE).requiresCorrectToolForDrops().noOcclusion(), com.hbm_m.block.decorations.DecoObjBlock.Placement.LOOK));
    public static final RegistrySupplier<Block> CRT_BLINKING = registerBlock("crt_blinking",
            () -> new com.hbm_m.block.decorations.DecoObjBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(5F, 6.0F).sound(SoundType.STONE).requiresCorrectToolForDrops().noOcclusion(), com.hbm_m.block.decorations.DecoObjBlock.Placement.LOOK));
    public static final RegistrySupplier<Block> CRT_BSOD = registerBlock("crt_bsod",
            () -> new com.hbm_m.block.decorations.DecoObjBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(5F, 6.0F).sound(SoundType.STONE).requiresCorrectToolForDrops().noOcclusion(), com.hbm_m.block.decorations.DecoObjBlock.Placement.LOOK));


    // ======================================================================

    public static final RegistrySupplier<Block> DEAD_DIRT  = registerBlock("dead_dirt",
            () -> new Block(BlockProps.copy(Blocks.DIRT).strength(0.5f, 4.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> GEYSIR_DIRT  = registerBlock("geysir_dirt",
            () -> new GeysirBlock(BlockProps.copy(Blocks.DIRT).strength(0.5f, 4.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> GEYSIR_STONE  = registerBlock("geysir_stone",
            () -> new GeysirBlock(BlockProps.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));


    public static final RegistrySupplier<Block> SELLAFIELD_SLAKED  = registerBlock("sellafield_slaked",
            () -> new BlockSellafieldSlaked(BlockProps.copy(Blocks.STONE).isValidSpawn((st, lvl, pos, type) -> false).requiresCorrectToolForDrops().strength(5.0F, 5.0F)));

    public static final RegistrySupplier<Block> SELLAFIELD_SLAKED1  = registerBlock("sellafield_slaked1",
            () -> new BlockSellafieldSlaked(BlockProps.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> SELLAFIELD_SLAKED2  = registerBlock("sellafield_slaked2",
            () -> new BlockSellafieldSlaked(BlockProps.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> SELLAFIELD_SLAKED3  = registerBlock("sellafield_slaked3",
            () -> new BlockSellafieldSlaked(BlockProps.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));

    /** Original "sellafield" (BlockSellafield) Meta 0-5: heisses Sellafit, kuehlt stufenweise ab. */
    public static final RegistrySupplier<Block> SELLAFIELD_0 = registerBlock("sellafield_0",
            () -> new com.hbm_m.block.generic.BlockSellafield(BlockProps.copy(Blocks.STONE).strength(5.0F).sound(SoundType.STONE).requiresCorrectToolForDrops(), 0, null));
    public static final RegistrySupplier<Block> SELLAFIELD_1 = registerBlock("sellafield_1",
            () -> new com.hbm_m.block.generic.BlockSellafield(BlockProps.copy(Blocks.STONE).strength(5.0F).sound(SoundType.STONE).requiresCorrectToolForDrops(), 1, () -> ModBlocks.SELLAFIELD_0.get()));
    public static final RegistrySupplier<Block> SELLAFIELD_2 = registerBlock("sellafield_2",
            () -> new com.hbm_m.block.generic.BlockSellafield(BlockProps.copy(Blocks.STONE).strength(5.0F).sound(SoundType.STONE).requiresCorrectToolForDrops(), 2, () -> ModBlocks.SELLAFIELD_1.get()));
    public static final RegistrySupplier<Block> SELLAFIELD_3 = registerBlock("sellafield_3",
            () -> new com.hbm_m.block.generic.BlockSellafield(BlockProps.copy(Blocks.STONE).strength(5.0F).sound(SoundType.STONE).requiresCorrectToolForDrops(), 3, () -> ModBlocks.SELLAFIELD_2.get()));
    public static final RegistrySupplier<Block> SELLAFIELD_4 = registerBlock("sellafield_4",
            () -> new com.hbm_m.block.generic.BlockSellafield(BlockProps.copy(Blocks.STONE).strength(5.0F).sound(SoundType.STONE).requiresCorrectToolForDrops(), 4, () -> ModBlocks.SELLAFIELD_3.get()));
    public static final RegistrySupplier<Block> SELLAFIELD_5 = registerBlock("sellafield_5",
            () -> new com.hbm_m.block.generic.BlockSellafield(BlockProps.copy(Blocks.STONE).strength(5.0F).sound(SoundType.STONE).requiresCorrectToolForDrops(), 5, () -> ModBlocks.SELLAFIELD_4.get()));

    public static final RegistrySupplier<Block> SELLAFIELD_BEDROCK = registerBlock("sellafield_bedrock",
            () -> new BlockSellafieldSlaked(BlockProps.copy(Blocks.BEDROCK).requiresCorrectToolForDrops()
                    .strength(-1.0F, 3600000.0F)
                    .isValidSpawn((state, level, pos, type) -> false)));

    public static final RegistrySupplier<Block> ORE_SELLAFIELD_DIAMOND = registerBlock("ore_sellafield_diamond",
            () -> BlockSellafieldOre.diamondOre(BlockProps.copy(Blocks.STONE).isValidSpawn((st, lvl, pos, type) -> false).requiresCorrectToolForDrops().strength(5.0F, 5.0F)));

    public static final RegistrySupplier<Block> ORE_SELLAFIELD_EMERALD = registerBlock("ore_sellafield_emerald",
            () -> BlockSellafieldOre.emeraldOre(BlockProps.copy(Blocks.STONE).isValidSpawn((st, lvl, pos, type) -> false).requiresCorrectToolForDrops().strength(5.0F, 5.0F)));

    public static final RegistrySupplier<Block> ORE_SELLAFIELD_URANIUM_SCORCHED = registerBlock("ore_sellafield_uranium_scorched",
            () -> BlockSellafieldOre.sellafiteOre(BlockProps.copy(Blocks.STONE).isValidSpawn((st, lvl, pos, type) -> false).requiresCorrectToolForDrops().strength(5.0F, 5.0F)));

    public static final RegistrySupplier<Block> ORE_SELLAFIELD_SCHRABIDIUM = registerBlock("ore_sellafield_schrabidium",
            () -> BlockSellafieldOre.sellafiteOre(BlockProps.copy(Blocks.STONE).isValidSpawn((st, lvl, pos, type) -> false).requiresCorrectToolForDrops().strength(5.0F, 5.0F)));

    public static final RegistrySupplier<Block> ORE_SELLAFIELD_RADGEM = registerBlock("ore_sellafield_radgem",
            () -> BlockSellafieldOre.radgemOre(BlockProps.copy(Blocks.STONE).isValidSpawn((st, lvl, pos, type) -> false).requiresCorrectToolForDrops().strength(5.0F, 5.0F)));

    public static final RegistrySupplier<Block> WASTE_TRINITITE = registerBlock("waste_trinitite",
            () -> new BlockOre(BlockProps.copy(Blocks.SAND).strength(0.5F, 1.5F)));

    public static final RegistrySupplier<Block> WASTE_TRINITITE_RED = registerBlock("waste_trinitite_red",
            () -> new BlockOre(BlockProps.copy(Blocks.SAND).strength(0.5F, 1.5F)));

    public static final RegistrySupplier<Block> WASTE_MYCELIUM = registerBlock("waste_mycelium",
            () -> new WasteEarth(BlockProps.copy(Blocks.MYCELIUM)
                    .strength(0.6F)
                    .randomTicks().lightLevel(s -> 15)));

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
            () -> new com.hbm_m.block.generic.BlockSpeedy(BlockProps.copy(Blocks.STONE).strength(15.0F, 72.0F).sound(SoundType.STONE).requiresCorrectToolForDrops(), 1.5));

    /** R6d: 1:1 BlockNoDrop (barricade, kein Drop). */
    public static final RegistrySupplier<Block> BARRICADE = registerBlock("barricade",
            () -> new Block(BlockProps.copy(Blocks.SAND).strength(1.0F, 1.5F).sound(SoundType.STONE)));

    public static final RegistrySupplier<Block> BASALT_BRICK = registerBlock("basalt_brick",
            () -> new Block(BlockProps.copy(Blocks.STONE).requiresCorrectToolForDrops().strength(5.0F, 6.0F)));

    public static final RegistrySupplier<Block> BASALT_POLISHED = registerBlock("basalt_polished",
            () -> new Block(BlockProps.copy(Blocks.STONE).requiresCorrectToolForDrops().strength(5.0F, 6.0F)));

    public static final RegistrySupplier<Block> BRICK_BASE = registerBlock("brick_base",
            () -> new Block(BlockProps.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));

    /** R6e: 1:1 BlockNoSpawn. */
    public static final RegistrySupplier<Block> BRICK_DUCRETE = registerBlock("brick_ducrete",
            () -> new com.hbm_m.block.generic.NTMPlants.NoSpawn(BlockProps.copy(Blocks.STONE).strength(15F, 450.0F).sound(SoundType.STONE).requiresCorrectToolForDrops()));

    // Original brick_fire: Haerte 5, Widerstand 35 (= 21 in 1.20)
    public static final RegistrySupplier<Block> BRICK_FIRE = registerBlock("brick_fire",
            () -> new Block(BlockProps.copy(Blocks.STONE).strength(5.0f, 21.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> BRICK_LIGHT = registerBlock("brick_light",
            () -> new Block(BlockProps.copy(Blocks.STONE).requiresCorrectToolForDrops().strength(5.0F, 12.0F)));

    public static final RegistrySupplier<Block> BRICK_OBSIDIAN = registerBlock("brick_obsidian",
            () -> new Block(BlockProps.copy(Blocks.STONE).requiresCorrectToolForDrops().strength(15.0F, 72.0F)));

    public static final RegistrySupplier<Block> CONCRETE_ASBESTOS = registerBlock("concrete_asbestos",
            () -> new Block(BlockProps.copy(Blocks.STONE).isValidSpawn((st, lvl, pos, type) -> false).requiresCorrectToolForDrops().strength(15.0F, 90.0F)));

    public static final RegistrySupplier<Block> CONCRETE_BLACK = registerBlock("concrete_black",
            () -> new Block(BlockProps.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> CONCRETE_BLUE = registerBlock("concrete_blue",
            () -> new Block(BlockProps.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> CONCRETE_BROWN = registerBlock("concrete_brown",
            () -> new Block(BlockProps.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> CONCRETE_COLORED_EXT_BRONZE = registerBlock("concrete_colored_ext_bronze",
            () -> new Block(BlockProps.copy(Blocks.STONE).isValidSpawn((st, lvl, pos, type) -> false).strength(15F, 84.0F).sound(SoundType.STONE).requiresCorrectToolForDrops()));
    /** concrete_colored_bronze: doppelte Port-ID von concrete_colored_ext@BRONZE (Umleitung in ForgeMainEvents.LEGACY_IDS). */
    public static final RegistrySupplier<Block> CONCRETE_COLORED_BRONZE = CONCRETE_COLORED_EXT_BRONZE;

    public static final RegistrySupplier<Block> CONCRETE_COLORED_EXT_INDIGO = registerBlock("concrete_colored_ext_indigo",
            () -> new Block(BlockProps.copy(Blocks.STONE).isValidSpawn((st, lvl, pos, type) -> false).strength(15F, 84.0F).sound(SoundType.STONE).requiresCorrectToolForDrops()));
    /** concrete_colored_indigo: doppelte Port-ID von concrete_colored_ext@INDIGO (Umleitung in ForgeMainEvents.LEGACY_IDS). */
    public static final RegistrySupplier<Block> CONCRETE_COLORED_INDIGO = CONCRETE_COLORED_EXT_INDIGO;

    public static final RegistrySupplier<Block> CONCRETE_COLORED_EXT_MACHINE = registerBlock("concrete_colored_ext_machine",
            () -> new Block(BlockProps.copy(Blocks.STONE).isValidSpawn((st, lvl, pos, type) -> false).strength(15F, 84.0F).sound(SoundType.STONE).requiresCorrectToolForDrops()));
    /** concrete_colored_machine: doppelte Port-ID von concrete_colored_ext@MACHINE (Umleitung in ForgeMainEvents.LEGACY_IDS). */
    public static final RegistrySupplier<Block> CONCRETE_COLORED_MACHINE = CONCRETE_COLORED_EXT_MACHINE;

    public static final RegistrySupplier<Block> CONCRETE_COLORED_EXT_MACHINE_STRIPE = registerBlock("concrete_colored_ext_machine_stripe",
            () -> new Block(BlockProps.copy(Blocks.STONE).strength(15F, 84.0F).sound(SoundType.STONE).requiresCorrectToolForDrops()));
    /** concrete_colored_machine_stripe: doppelte Port-ID von concrete_colored_ext@MACHINE_STRIPE (Umleitung in ForgeMainEvents.LEGACY_IDS). */
    public static final RegistrySupplier<Block> CONCRETE_COLORED_MACHINE_STRIPE = CONCRETE_COLORED_EXT_MACHINE_STRIPE;

    public static final RegistrySupplier<Block> CONCRETE_COLORED_EXT_PINK = registerBlock("concrete_colored_ext_pink",
            () -> new Block(BlockProps.copy(Blocks.STONE).strength(15F, 84.0F).sound(SoundType.STONE).requiresCorrectToolForDrops()));
    /** concrete_colored_pink: doppelte Port-ID von concrete_colored_ext@PINK (Umleitung in ForgeMainEvents.LEGACY_IDS). */
    public static final RegistrySupplier<Block> CONCRETE_COLORED_PINK = CONCRETE_COLORED_EXT_PINK;

    public static final RegistrySupplier<Block> CONCRETE_COLORED_EXT_PURPLE = registerBlock("concrete_colored_ext_purple",
            () -> new Block(BlockProps.copy(Blocks.STONE).strength(15F, 84.0F).sound(SoundType.STONE).requiresCorrectToolForDrops()));
    /** concrete_colored_purple: doppelte Port-ID von concrete_colored_ext@PURPLE (Umleitung in ForgeMainEvents.LEGACY_IDS). */
    public static final RegistrySupplier<Block> CONCRETE_COLORED_PURPLE = CONCRETE_COLORED_EXT_PURPLE;

    public static final RegistrySupplier<Block> CONCRETE_COLORED_EXT_SAND = registerBlock("concrete_colored_ext_sand",
            () -> new Block(BlockProps.copy(Blocks.STONE).strength(15F, 84.0F).sound(SoundType.STONE).requiresCorrectToolForDrops()));
    /** concrete_colored_sand: doppelte Port-ID von concrete_colored_ext@SAND (Umleitung in ForgeMainEvents.LEGACY_IDS). */
    public static final RegistrySupplier<Block> CONCRETE_COLORED_SAND = CONCRETE_COLORED_EXT_SAND;

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
            () -> new Block(BlockProps.copy(Blocks.STONE).isValidSpawn((st, lvl, pos, type) -> false).requiresCorrectToolForDrops().strength(50.0F, 144.0F)));

    public static final RegistrySupplier<Block> CONCRETE_REBAR_ALT = registerBlock("concrete_rebar_alt",
            () -> new Block(BlockProps.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> CONCRETE_RED = registerBlock("concrete_red",
            () -> new Block(BlockProps.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> CONCRETE_SILVER = registerBlock("concrete_silver",
            () -> new Block(BlockProps.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> CONCRETE_SUPER = registerBlock("concrete_super",
            () -> new com.hbm_m.block.generic.BlockUberConcrete(BlockProps.copy(Blocks.STONE).strength(150.0F, 600.0F).sound(SoundType.STONE).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> CONCRETE_SUPER_BROKEN = registerBlock("concrete_super_broken",
            () -> new net.minecraft.world.level.block.FallingBlock(BlockProps.copy(Blocks.STONE).strength(10.0F, 12.0F).sound(SoundType.STONE).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> CONCRETE_SUPER_M0 = registerBlock("concrete_super_m0",
            () -> new Block(BlockProps.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> CONCRETE_SUPER_M1 = registerBlock("concrete_super_m1",
            () -> new Block(BlockProps.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> CONCRETE_SUPER_M2 = registerBlock("concrete_super_m2",
            () -> new Block(BlockProps.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> CONCRETE_SUPER_M3 = registerBlock("concrete_super_m3",
            () -> new Block(BlockProps.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));

    // Original "concrete" (Concrete Tile): BlockNoSpawn, Haerte 15, Widerstand 140 (= 84 in 1.20)
    public static final RegistrySupplier<Block> CONCRETE_TILE = registerBlock("concrete_tile",
            () -> new com.hbm_m.block.generic.BlockNoSpawn(BlockProps.copy(Blocks.STONE).strength(15.0F, 84.0F).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> CONCRETE_TILE_TREFOIL = registerBlock("concrete_tile_trefoil",
            () -> new Block(BlockProps.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> CONCRETE_WHITE = registerBlock("concrete_white",
            () -> new Block(BlockProps.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> CONCRETE_YELLOW = registerBlock("concrete_yellow",
            () -> new Block(BlockProps.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> CONCRETE_FLAT = registerBlock("concrete_flat",
            () -> new Block(BlockProps.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> DEPTH_BRICK = registerBlock("depth_brick",
            () -> new DepthOreBlock(BlockProps.copy(Blocks.STONE).requiresCorrectToolForDrops().strength(-1.0F, 6.0F)));

    public static final RegistrySupplier<Block> DEPTH_NETHER_BRICK = registerBlock("depth_nether_brick",
            () -> new DepthOreBlock(BlockProps.copy(Blocks.STONE).requiresCorrectToolForDrops().strength(-1.0F, 6.0F)));

    public static final RegistrySupplier<Block> DEPTH_NETHER_TILES = registerBlock("depth_nether_tiles",
            () -> new DepthOreBlock(BlockProps.copy(Blocks.STONE).requiresCorrectToolForDrops().strength(-1.0F, 6.0F)));

    public static final RegistrySupplier<Block> DEPTH_STONE_NETHER = registerBlock("depth_stone_nether",
            () -> new DepthOreBlock(BlockProps.copy(Blocks.STONE).strength(-1.0F, 6.0F).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> DEPTH_TILES = registerBlock("depth_tiles",
            () -> new DepthOreBlock(BlockProps.copy(Blocks.STONE).requiresCorrectToolForDrops().strength(-1.0F, 6.0F)));

    public static final RegistrySupplier<Block> GNEISS_BRICK = registerBlock("gneiss_brick",
            () -> new Block(BlockProps.copy(Blocks.STONE).requiresCorrectToolForDrops().strength(1.5F, 6.0F)));

    public static final RegistrySupplier<Block> GNEISS_CHISELED = registerBlock("gneiss_chiseled",
            () -> new Block(BlockProps.copy(Blocks.STONE).strength(1.5F, 6.0F).sound(SoundType.STONE).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> GNEISS_STONE = registerBlock("gneiss_stone",
            () -> new Block(BlockProps.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> GNEISS_TILE = registerBlock("gneiss_tile",
            () -> new Block(BlockProps.copy(Blocks.STONE).requiresCorrectToolForDrops().strength(1.5F, 6.0F)));

    public static final RegistrySupplier<Block> METEOR = registerBlock("meteor",
            () -> new Block(BlockProps.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> METEOR_BRICK = registerBlock("meteor_brick",
            () -> new Block(BlockProps.copy(Blocks.STONE).requiresCorrectToolForDrops().strength(15.0F, 216.0F)));

    public static final RegistrySupplier<Block> METEOR_BRICK_CHISELED = registerBlock("meteor_brick_chiseled",
            () -> new Block(BlockProps.copy(Blocks.STONE).requiresCorrectToolForDrops().strength(15.0F, 216.0F)));

    public static final RegistrySupplier<Block> METEOR_BRICK_CRACKED = registerBlock("meteor_brick_cracked",
            () -> new Block(BlockProps.copy(Blocks.STONE).requiresCorrectToolForDrops().strength(15.0F, 216.0F)));

    public static final RegistrySupplier<Block> METEOR_BRICK_MOSSY = registerBlock("meteor_brick_mossy",
            () -> new Block(BlockProps.copy(Blocks.STONE).requiresCorrectToolForDrops().strength(15.0F, 216.0F)));

    public static final RegistrySupplier<Block> METEOR_COBBLE = registerBlock("meteor_cobble",
            () -> new Block(BlockProps.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> METEOR_CRUSHED = registerBlock("meteor_crushed",
            () -> new Block(BlockProps.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> METEOR_PILLAR = registerBlock("meteor_pillar",
            () -> new net.minecraft.world.level.block.RotatedPillarBlock(BlockProps.copy(Blocks.STONE).strength(15.0F, 216.0F).sound(SoundType.STONE).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> METEOR_POLISHED = registerBlock("meteor_polished",
            () -> new Block(BlockProps.copy(Blocks.STONE).requiresCorrectToolForDrops().strength(15.0F, 216.0F)));

    public static final RegistrySupplier<Block> METEOR_TREASURE = registerBlock("meteor_treasure",
            () -> new Block(BlockProps.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));

    /** R6d: vinyl_tile Meta 0 (LARGE), Werte 1:1. */
    public static final RegistrySupplier<Block> VINYL_TILE = registerBlock("vinyl_tile",
            () -> new Block(BlockProps.copy(Blocks.STONE).strength(10.0F, 36.0F).sound(SoundType.GLASS).requiresCorrectToolForDrops()));

    /** R6d: vinyl_tile Meta 1 (SMALL). */
    public static final RegistrySupplier<Block> VINYL_TILE_SMALL = registerBlock("vinyl_tile_small",
            () -> new Block(BlockProps.copy(Blocks.STONE).strength(10.0F, 36.0F).sound(SoundType.GLASS).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> CONCRETE_PILLAR  = registerBlock("concrete_pillar",
            () -> new net.minecraft.world.level.block.RotatedPillarBlock(BlockProps.copy(Blocks.STONE).strength(15.0F, 108.0F).sound(SoundType.STONE).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> CONCRETE_ASBESTOS_STAIRS = registerBlock("concrete_asbestos_stairs",
            () -> new StairBlock(CONCRETE_ASBESTOS.get().defaultBlockState(), BlockProps.copy(Blocks.STONE).requiresCorrectToolForDrops().strength(15.0F, 90.0F)));

    public static final RegistrySupplier<Block> CONCRETE_BLACK_STAIRS = registerBlock("concrete_black_stairs",
            () -> new StairBlock(CONCRETE_BLACK.get().defaultBlockState(), BlockProps.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> CONCRETE_BLUE_STAIRS = registerBlock("concrete_blue_stairs",
            () -> new StairBlock(CONCRETE_BLUE.get().defaultBlockState(), BlockProps.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> CONCRETE_BROWN_STAIRS = registerBlock("concrete_brown_stairs",
            () -> new StairBlock(CONCRETE_BROWN.get().defaultBlockState(), BlockProps.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> CONCRETE_COLORED_BRONZE_STAIRS = registerBlock("concrete_colored_bronze_stairs",
            () -> new StairBlock(CONCRETE_COLORED_BRONZE.get().defaultBlockState(), BlockProps.copy(Blocks.STONE).requiresCorrectToolForDrops().strength(15.0F, 84.0F)));

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
            () -> new StairBlock(BRICK_LIGHT.get().defaultBlockState(), BlockProps.copy(Blocks.STONE).requiresCorrectToolForDrops().strength(5.0F, 12.0F)));

    public static final RegistrySupplier<Block> BRICK_FIRE_STAIRS = registerBlock("brick_fire_stairs",
            () -> new StairBlock(BRICK_FIRE.get().defaultBlockState(), BlockProps.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> BRICK_OBSIDIAN_STAIRS = registerBlock("brick_obsidian_stairs",
            () -> new StairBlock(BRICK_OBSIDIAN.get().defaultBlockState(), BlockProps.copy(Blocks.STONE).requiresCorrectToolForDrops().strength(15.0F, 72.0F)));

    public static final RegistrySupplier<Block> VINYL_TILE_STAIRS = registerBlock("vinyl_tile_stairs",
            () -> new StairBlock(VINYL_TILE.get().defaultBlockState(), BlockProps.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> VINYL_TILE_SMALL_STAIRS = registerBlock("vinyl_tile_small_stairs",
            () -> new StairBlock(VINYL_TILE_SMALL.get().defaultBlockState(), BlockProps.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> BRICK_DUCRETE_STAIRS = registerBlock("brick_ducrete_stairs",
            () -> new StairBlock(BRICK_DUCRETE.get().defaultBlockState(), BlockProps.copy(Blocks.STONE).requiresCorrectToolForDrops().strength(15.0F, 450.0F)));

    public static final RegistrySupplier<Block> ASPHALT_STAIRS = registerBlock("asphalt_stairs",
            () -> new StairBlock(ASPHALT.get().defaultBlockState(), BlockProps.copy(Blocks.STONE).requiresCorrectToolForDrops().strength(15.0F, 72.0F)));

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
            () -> new SlabBlock(BlockProps.copy(Blocks.STONE).requiresCorrectToolForDrops().strength(15.0F, 84.0F)));
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
            () -> new Block(BlockProps.copy(Blocks.STONE).requiresCorrectToolForDrops().strength(15.0F, 27.0F)));
    public static final RegistrySupplier<Block> BRICK_CONCRETE_BROKEN_STAIRS = registerBlock("brick_concrete_broken_stairs",
            () -> new StairBlock(ModBlocks.BRICK_CONCRETE_BROKEN.get().defaultBlockState(),
                    BlockProps.copy(Blocks.STONE).sound(SoundType.STONE).strength(15.0F, 27.0F)));
    public static final RegistrySupplier<Block> BRICK_CONCRETE_BROKEN_SLAB = registerBlock("brick_concrete_broken_slab",
            () -> new SlabBlock(BlockProps.copy(Blocks.STONE).sound(SoundType.STONE)));

    public static final RegistrySupplier<Block> BRICK_CONCRETE_CRACKED = registerBlock("brick_concrete_cracked",
            () -> new Block(BlockProps.copy(Blocks.STONE).requiresCorrectToolForDrops().strength(15.0F, 36.0F)));
    public static final RegistrySupplier<Block> BRICK_CONCRETE_CRACKED_STAIRS = registerBlock("brick_concrete_cracked_stairs",
            () -> new StairBlock(ModBlocks.BRICK_CONCRETE_CRACKED.get().defaultBlockState(),
                    BlockProps.copy(Blocks.STONE).sound(SoundType.STONE).strength(15.0F, 36.0F)));
    public static final RegistrySupplier<Block> BRICK_CONCRETE_CRACKED_SLAB = registerBlock("brick_concrete_cracked_slab",
            () -> new SlabBlock(BlockProps.copy(Blocks.STONE).sound(SoundType.STONE)));

    public static final RegistrySupplier<Block> BRICK_CONCRETE_MOSSY = registerBlock("brick_concrete_mossy",
            () -> new Block(BlockProps.copy(Blocks.STONE).requiresCorrectToolForDrops().strength(15.0F, 96.0F)));
    public static final RegistrySupplier<Block> BRICK_CONCRETE_MOSSY_STAIRS = registerBlock("brick_concrete_mossy_stairs",
            () -> new StairBlock(ModBlocks.BRICK_CONCRETE_MOSSY.get().defaultBlockState(),
                    BlockProps.copy(Blocks.STONE).sound(SoundType.STONE).strength(15.0F, 96.0F)));
    public static final RegistrySupplier<Block> BRICK_CONCRETE_MOSSY_SLAB = registerBlock("brick_concrete_mossy_slab",
            () -> new SlabBlock(BlockProps.copy(Blocks.STONE).sound(SoundType.STONE)));

    public static final RegistrySupplier<Block> BRICK_CONCRETE_MARKED = registerBlock("brick_concrete_marked",
            () -> new com.hbm_m.block.generic.BlockWriting(BlockProps.copy(Blocks.STONE).strength(15.0F, 96.0F).sound(SoundType.STONE).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> REINFORCED_STONE_STAIRS = registerBlock("reinforced_stone_stairs",
            () -> new StairBlock(ModBlocks.REINFORCED_STONE.get().defaultBlockState(),
                    BlockProps.copy(Blocks.STONE).sound(SoundType.STONE).strength(15.0F, 60.0F)));
    public static final RegistrySupplier<Block> REINFORCED_STONE_SLAB = registerBlock("reinforced_stone_slab",
            () -> new SlabBlock(BlockProps.copy(Blocks.STONE).sound(SoundType.STONE)));


    private static final BlockBehaviour.Properties CRATE_PROPERTIES =
            BlockProps.copy(Blocks.IRON_BLOCK).sound(SoundType.METAL).strength(5.0F, 6.0F).requiresCorrectToolForDrops();

    public static final RegistrySupplier<Block> CRATE_IRON = registerBlockWithoutItem("crate_iron",
            () -> new IronCrateBlock(CRATE_PROPERTIES));

    public static final RegistrySupplier<Block> CRATE_STEEL = registerBlockWithoutItem("crate_steel",
            () -> new SteelCrateBlock(CRATE_PROPERTIES));

    public static final RegistrySupplier<Block> CRATE_DESH = registerBlockWithoutItem("crate_desh",
            () -> new DeshCrateBlock(BlockProps.copy(Blocks.IRON_BLOCK).sound(SoundType.METAL).requiresCorrectToolForDrops().strength(5.0F, 6.0F)));

    public static final RegistrySupplier<Block> CRATE_TUNGSTEN = registerBlockWithoutItem("crate_tungsten",
            () -> new TungstenCrateBlock(BlockProps.copy(Blocks.IRON_BLOCK).sound(SoundType.METAL).requiresCorrectToolForDrops().strength(7.5F, 180.0F)));

    public static final RegistrySupplier<Block> CRATE_TEMPLATE = registerBlockWithoutItem("crate_template",
            () -> new TemplateCrateBlock(BlockProps.copy(Blocks.IRON_BLOCK).sound(SoundType.METAL).strength(0.5f, 1f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> WASTE_PLANKS = registerBlock("waste_planks",
            () -> new com.hbm_m.block.generic.BlockOre(BlockProps.copy(Blocks.OAK_PLANKS).strength(0.5F, 1.5F).sound(SoundType.WOOD)));

    public static final RegistrySupplier<Block> WASTE_LOG = registerBlock("waste_log",
            () -> new Block(BlockProps.copy(Blocks.OAK_PLANKS).strength(5.0F, 1.5F).sound(SoundType.WOOD)));


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


    public static final RegistrySupplier<Block> LIGNITE_ORE = registerBlock("lignite_ore", // 1:1 BlockOre (Original-Klasse; Drops in ModBlockLootTableProvider)
            () -> new BlockOre(BlockProps.copy(Blocks.STONE).requiresCorrectToolForDrops().strength(5.0F, 9.0F)));

    /** Original-ID ore_aluminium (frueher Port-ID aluminum_ore, Umleitung in ForgeMainEvents.LEGACY_IDS). */
    public static final RegistrySupplier<Block> ALUMINUM_ORE = registerBlock("ore_aluminium",
            () -> new Block(BlockProps.copy(Blocks.STONE).requiresCorrectToolForDrops().strength(5.0F, 6.0F)));


	/** Original: {@code ore_uranium} ist ein {@code BlockOutgas} und gast Radon aus. */
    public static final RegistrySupplier<Block> URANIUM_ORE = registerBlock("uranium_ore",
            () -> new com.hbm_m.block.gas.OutgasBlock(BlockProps.copy(Blocks.STONE).requiresCorrectToolForDrops().strength(5.0F, 6.0F),
                    () -> ModBlocks.GAS_RADON.get(), true, true));

    public static final RegistrySupplier<Block> LEAD_ORE = registerBlock("lead_ore",
            () -> new Block(BlockProps.copy(Blocks.STONE).requiresCorrectToolForDrops().strength(5.0F, 6.0F)));

    public static final RegistrySupplier<Block> RAREGROUND_ORE = registerBlock("rareground_ore",
            () -> new Block(BlockProps.copy(Blocks.STONE).strength(3.0f, 3.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> FLUORITE_ORE = registerBlock("fluorite_ore", // 1:1 BlockOre (Original-Klasse; Drops in ModBlockLootTableProvider)
            () -> new BlockOre(BlockProps.copy(Blocks.STONE).requiresCorrectToolForDrops().strength(5.0F, 6.0F)));

    public static final RegistrySupplier<Block> BERYLLIUM_ORE = registerBlock("beryllium_ore",
            () -> new Block(BlockProps.copy(Blocks.STONE).requiresCorrectToolForDrops().strength(5.0F, 9.0F)));

    /** Original: {@code ore_asbestos} ist ein {@code BlockOutgas} und wirbelt Asbestfasern auf. */
    public static final RegistrySupplier<Block> ASBESTOS_ORE = registerBlock("asbestos_ore",
            () -> new com.hbm_m.block.gas.OutgasBlock(BlockProps.copy(Blocks.STONE).requiresCorrectToolForDrops().strength(5.0F, 9.0F),
                    () -> ModBlocks.GAS_ASBESTOS.get(), true, true, false, false, true));

    public static final RegistrySupplier<Block> CINNABAR_ORE = registerBlock("cinnabar_ore", // 1:1 BlockOre (Original-Klasse; Drops in ModBlockLootTableProvider)
            () -> new BlockOre(BlockProps.copy(Blocks.STONE).requiresCorrectToolForDrops().strength(5.0F, 6.0F)));

    public static final RegistrySupplier<Block> COBALT_ORE = registerBlock("cobalt_ore", // 1:1 BlockOre (Original-Klasse; Drops in ModBlockLootTableProvider)
            () -> new BlockOre(BlockProps.copy(Blocks.STONE).requiresCorrectToolForDrops().strength(5.0F, 6.0F)));

    public static final RegistrySupplier<Block> TUNGSTEN_ORE = registerBlock("tungsten_ore",
            () -> new Block(BlockProps.copy(Blocks.STONE).requiresCorrectToolForDrops().strength(5.0F, 6.0F)));

    public static final RegistrySupplier<Block> THORIUM_ORE = registerBlock("thorium_ore",
            () -> new Block(BlockProps.copy(Blocks.STONE).requiresCorrectToolForDrops().strength(5.0F, 6.0F)));

    public static final RegistrySupplier<Block> FREAKY_ALIEN_BLOCK = registerBlock("freaky_alien_block",
            () -> new Block(BlockProps.copy(Blocks.STONE).strength(3.0f, 3.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> TITANIUM_ORE = registerBlock("titanium_ore",
            () -> new Block(BlockProps.copy(Blocks.STONE).requiresCorrectToolForDrops().strength(5.0F, 6.0F)));

    public static final RegistrySupplier<Block> SULFUR_ORE = registerBlock("sulfur_ore", // 1:1 BlockOre (Original-Klasse; Drops in ModBlockLootTableProvider)
            () -> new BlockOre(BlockProps.copy(Blocks.STONE).requiresCorrectToolForDrops().strength(5.0F, 6.0F)));

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
            // audit10: 1:1 BlockOre(Material.rock, 0.1F, 0.5F) - Chunkstrahlung alle 20 Ticks
            () -> new com.hbm_m.block.generic.BlockOreRad(BlockProps.copy(Blocks.STONE).requiresCorrectToolForDrops().strength(15.0F, 360.0F), 0.1F, 0.5F));

    /** Порт {@code ore_nether_schrabidium}. */
    public static final RegistrySupplier<Block> SCHRABIDIUM_ORE_NETHER = registerBlock("schrabidium_ore_nether",
            () -> new Block(BlockProps.copy(Blocks.NETHERRACK).requiresCorrectToolForDrops().strength(15.0F, 360.0F).sound(SoundType.STONE)));

    /** Порт {@code ore_gneiss_schrabidium}. */
    public static final RegistrySupplier<Block> SCHRABIDIUM_ORE_GNEISS = registerBlock("schrabidium_ore_gneiss", // 1:1 BlockOre (Original-Klasse; Drops in ModBlockLootTableProvider)
            () -> new BlockOre(BlockProps.copy(Blocks.STONE).requiresCorrectToolForDrops().strength(1.5F, 6.0F)));

    // ==== Руды, добавленные для полного паритета генерации с 1.7.10 (HbmWorldGen) ====
    // Медную руду не портируем — в 1.18+ есть ванильная.

    public static final RegistrySupplier<Block> NITER_ORE = registerBlock("niter_ore", // 1:1 BlockOre (Original-Klasse; Drops in ModBlockLootTableProvider)
            () -> new BlockOre(BlockProps.copy(Blocks.STONE).requiresCorrectToolForDrops().strength(5.0F, 6.0F)));

    public static final RegistrySupplier<Block> NITER_ORE_DEEPSLATE = registerBlock("niter_ore_deepslate",
            () -> new Block(BlockProps.copy(Blocks.DEEPSLATE).strength(5.0f, 5.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> LITHIUM_ORE = registerBlock("lithium_ore",
            () -> new Block(BlockProps.copy(Blocks.STONE).strength(3.0f, 3.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> LITHIUM_ORE_DEEPSLATE = registerBlock("lithium_ore_deepslate",
            () -> new Block(BlockProps.copy(Blocks.DEEPSLATE).strength(5.0f, 5.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> ALEXANDRITE_ORE = registerBlock("alexandrite_ore",
            () -> new Block(BlockProps.copy(Blocks.STONE).requiresCorrectToolForDrops().strength(-1.0F, 6.0F)));

    public static final RegistrySupplier<Block> COLTAN_ORE = registerBlock("coltan_ore", // 1:1 BlockOre (Original-Klasse; Drops in ModBlockLootTableProvider)
            () -> new BlockOre(BlockProps.copy(Blocks.STONE).requiresCorrectToolForDrops().strength(15.0F, 6.0F)));

    public static final RegistrySupplier<Block> COLTAN_ORE_DEEPSLATE = registerBlock("coltan_ore_deepslate",
            () -> new Block(BlockProps.copy(Blocks.DEEPSLATE).strength(5.0f, 5.0f).requiresCorrectToolForDrops()));

    // Deepslate-версии существующих руд (в 1.7.10 их не было)
    public static final RegistrySupplier<Block> SULFUR_ORE_DEEPSLATE = registerBlock("sulfur_ore_deepslate",
            () -> new Block(BlockProps.copy(Blocks.DEEPSLATE).strength(5.0f, 5.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> TUNGSTEN_ORE_DEEPSLATE = registerBlock("tungsten_ore_deepslate",
            () -> new Block(BlockProps.copy(Blocks.DEEPSLATE).strength(5.0f, 5.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> ASBESTOS_ORE_DEEPSLATE = registerBlock("asbestos_ore_deepslate",
            () -> new Block(BlockProps.copy(Blocks.DEEPSLATE).strength(5.0f, 5.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> FLUORITE_ORE_DEEPSLATE = registerBlock("fluorite_ore_deepslate",
            () -> new Block(BlockProps.copy(Blocks.DEEPSLATE).strength(5.0f, 5.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> LIGNITE_ORE_DEEPSLATE = registerBlock("lignite_ore_deepslate",
            () -> new Block(BlockProps.copy(Blocks.DEEPSLATE).strength(5.0f, 5.0f).requiresCorrectToolForDrops()));

    // Незерские руды (ore_nether_* оригинала)
    /** Original: {@code ore_nether_uranium} ist ein {@code BlockOutgas} und gast Radon aus. */
    public static final RegistrySupplier<Block> NETHER_URANIUM_ORE = registerBlock("nether_uranium_ore",
            () -> new com.hbm_m.block.gas.OutgasBlock(BlockProps.copy(Blocks.NETHERRACK).requiresCorrectToolForDrops().strength(0.4F, 6.0F).sound(SoundType.STONE),
                    () -> ModBlocks.GAS_RADON.get(), true, true));

    public static final RegistrySupplier<Block> NETHER_TUNGSTEN_ORE = registerBlock("nether_tungsten_ore",
            () -> new Block(BlockProps.copy(Blocks.NETHERRACK).strength(3.0f, 3.0f).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> NETHER_SULFUR_ORE = registerBlock("nether_sulfur_ore", // 1:1 BlockOre (Original-Klasse; Drops in ModBlockLootTableProvider)
            () -> new BlockOre(BlockProps.copy(Blocks.NETHERRACK).requiresCorrectToolForDrops().strength(0.4F, 6.0F).sound(SoundType.STONE)));

    /** Порт {@code ore_nether_fire} (фосфорит). */
    public static final RegistrySupplier<Block> NETHER_FIRE_ORE = registerBlock("nether_fire_ore", // 1:1 BlockOre (Original-Klasse; Drops in ModBlockLootTableProvider)
            () -> new BlockOre(BlockProps.copy(Blocks.NETHERRACK).requiresCorrectToolForDrops().strength(0.4F, 6.0F).sound(SoundType.STONE)));

    public static final RegistrySupplier<Block> NETHER_COAL_ORE = registerBlock("nether_coal_ore",
            // audit10: 1:1 BlockNetherCoal (BlockOutgas(rock, false, 5, true) mit gas_monoxide)
            () -> new com.hbm_m.block.generic.BlockNetherCoal(BlockProps.copy(Blocks.NETHERRACK).requiresCorrectToolForDrops().strength(0.4F, 6.0F).sound(SoundType.STONE).lightLevel(s -> 10),
                    () -> ModBlocks.GAS_MONOXIDE.get(), false, true));

    public static final RegistrySupplier<Block> NETHER_COBALT_ORE = registerBlock("nether_cobalt_ore", // 1:1 BlockOre (Original-Klasse; Drops in ModBlockLootTableProvider)
            () -> new BlockOre(BlockProps.copy(Blocks.NETHERRACK).requiresCorrectToolForDrops().strength(0.4F, 6.0F).sound(SoundType.STONE)));

    public static final RegistrySupplier<Block> NETHER_PLUTONIUM_ORE = registerBlock("nether_plutonium_ore",
            () -> new Block(BlockProps.copy(Blocks.NETHERRACK).strength(3.0f, 3.0f).requiresCorrectToolForDrops()));

    /** Порт {@code ore_nether_smoldering} (дымящийся аутунит, поверхностная руда). */
    public static final RegistrySupplier<Block> NETHER_SMOLDERING_ORE = registerBlock("nether_smoldering_ore",
            // audit10: 1:1 BlockSmolder
            () -> new com.hbm_m.block.generic.BlockSmolder(BlockProps.copy(Blocks.NETHERRACK).requiresCorrectToolForDrops().strength(0.4F, 6.0F).sound(SoundType.STONE).lightLevel(s -> 15)));

    /** Порт {@code ore_depth_nether_neodymium} (глубинные залежи у дна/потолка ада). */
    public static final RegistrySupplier<Block> DEPTH_NETHER_NEODYMIUM = registerBlock("depth_nether_neodymium",
            () -> new Block(BlockProps.copy(Blocks.NETHERRACK).strength(3.0f, 3.0f).requiresCorrectToolForDrops()));

    /** Порт {@code ore_australium} (секретная руда). */
    public static final RegistrySupplier<Block> AUSTRALIUM_ORE = registerBlock("australium_ore",
            () -> new Block(BlockProps.copy(Blocks.STONE).strength(3.0f, 3.0f).requiresCorrectToolForDrops()));

    // Гнейсовые руды (ore_gneiss_* оригинала; генерируются внутри гнейсовых пластов)
    public static final RegistrySupplier<Block> GNEISS_IRON_ORE = registerBlock("gneiss_iron_ore", // 1:1 BlockOre (Original-Klasse; Drops in ModBlockLootTableProvider)
            () -> new BlockOre(BlockProps.copy(Blocks.STONE).requiresCorrectToolForDrops().strength(1.5F, 6.0F)));

    public static final RegistrySupplier<Block> GNEISS_GOLD_ORE = registerBlock("gneiss_gold_ore", // 1:1 BlockOre (Original-Klasse; Drops in ModBlockLootTableProvider)
            () -> new BlockOre(BlockProps.copy(Blocks.STONE).requiresCorrectToolForDrops().strength(1.5F, 6.0F)));

    /** Original: {@code ore_gneiss_uranium} ist ein {@code BlockOutgas} und gast Radon aus. */
    public static final RegistrySupplier<Block> GNEISS_URANIUM_ORE = registerBlock("gneiss_uranium_ore",
            () -> new com.hbm_m.block.gas.OutgasBlock(BlockProps.copy(Blocks.STONE).requiresCorrectToolForDrops().strength(1.5F, 6.0F),
                    () -> ModBlocks.GAS_RADON.get(), true, true));

    public static final RegistrySupplier<Block> GNEISS_COPPER_ORE = registerBlock("gneiss_copper_ore", // 1:1 BlockOre (Original-Klasse; Drops in ModBlockLootTableProvider)
            () -> new BlockOre(BlockProps.copy(Blocks.STONE).requiresCorrectToolForDrops().strength(1.5F, 6.0F)));

    /** Original: {@code ore_gneiss_asbestos} ist ein {@code BlockOutgas}. */
    public static final RegistrySupplier<Block> GNEISS_ASBESTOS_ORE = registerBlock("gneiss_asbestos_ore",
            () -> new com.hbm_m.block.gas.OutgasBlock(BlockProps.copy(Blocks.STONE).requiresCorrectToolForDrops().strength(1.5F, 6.0F),
                    () -> ModBlocks.GAS_ASBESTOS.get(), true, true, false, false, true));

    public static final RegistrySupplier<Block> GNEISS_LITHIUM_ORE = registerBlock("gneiss_lithium_ore", // 1:1 BlockOre (Original-Klasse; Drops in ModBlockLootTableProvider)
            () -> new BlockOre(BlockProps.copy(Blocks.STONE).requiresCorrectToolForDrops().strength(1.5F, 6.0F)));

    public static final RegistrySupplier<Block> GNEISS_RARE_ORE = registerBlock("gneiss_rare_ore", // 1:1 BlockOre (Original-Klasse; Drops in ModBlockLootTableProvider)
            () -> new BlockOre(BlockProps.copy(Blocks.STONE).requiresCorrectToolForDrops().strength(1.5F, 6.0F)));

    public static final RegistrySupplier<Block> GNEISS_GAS_ORE = registerBlock("gneiss_gas_ore", // 1:1 BlockOre (Original-Klasse; Drops in ModBlockLootTableProvider)
            () -> new BlockOre(BlockProps.copy(Blocks.STONE).requiresCorrectToolForDrops().strength(1.5F, 6.0F)));

    /** Порт {@code ore_tikite} (BlockDragonProof; дракон-стойкость опущена). */
    public static final RegistrySupplier<Block> TIKITE_ORE = registerBlock("tikite_ore",
            () -> new Block(BlockProps.copy(Blocks.END_STONE).requiresCorrectToolForDrops().strength(5.0F, 6.0F)));

    /** Порт {@code ore_oil_sand} (песчаные нефтяные месторождения в пустынях). */
    public static final RegistrySupplier<Block> ORE_OIL_SAND = registerBlock("ore_oil_sand",
            () -> new net.minecraft.world.level.block.FallingBlock(BlockProps.copy(Blocks.SAND).strength(0.5F, 0.6F).sound(SoundType.SAND)));




    /** Порт {@code block_schrabidium_cluster} ({@link com.hbm.blocks.generic.BlockRotatablePillar}). */
    public static final RegistrySupplier<Block> BLOCK_SCHRABIDIUM_CLUSTER = registerBlock("block_schrabidium_cluster",
            () -> new RotatedPillarBlock(BlockProps.copy(Blocks.IRON_BLOCK).requiresCorrectToolForDrops().strength(5.0F, 36000.0F).sound(SoundType.STONE)));

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
    public static final RegistrySupplier<Block> RBMK_ROD_MOD      = registerBlock("rbmk_element_mod",  () -> new RBMKRodBlock(true,  rbmkProps().strength(3.0F, 18.0F).sound(SoundType.STONE)));
    /** ReaSim variants: same logic/BlockEntity as the base rod, distinct skin only (matches the rbmk_control_reasim precedent). */
    public static final RegistrySupplier<Block> RBMK_ROD_REASIM       = registerBlock("rbmk_element_reasim",       () -> new RBMKRodBlock(false, true, rbmkProps()));
    public static final RegistrySupplier<Block> RBMK_ROD_REASIM_MOD   = registerBlock("rbmk_element_reasim_mod",   () -> new RBMKRodBlock(true,  true, rbmkProps().strength(3.0F, 18.0F).sound(SoundType.STONE)));

    // ── Control Rods ────────────────────────────────────────────────────────
    public static final RegistrySupplier<Block> RBMK_CONTROL               = registerBlock("rbmk_control",               () -> new RBMKControlManualBlock(false, rbmkProps().strength(3.0F, 18.0F).sound(SoundType.STONE)));
    public static final RegistrySupplier<Block> RBMK_CONTROL_MOD           = registerBlock("rbmk_control_mod",           () -> new RBMKControlManualBlock(true,  rbmkProps().strength(3.0F, 18.0F).sound(SoundType.STONE)));
    public static final RegistrySupplier<Block> RBMK_CONTROL_AUTO          = registerBlock("rbmk_control_auto",          () -> new RBMKControlAutoBlock(false, rbmkProps().strength(3.0F, 18.0F).sound(SoundType.STONE)));
    public static final RegistrySupplier<Block> RBMK_CONTROL_REASIM        = registerBlock("rbmk_control_reasim",        () -> new RBMKControlManualBlock(false, "rbmk_control_reasim", rbmkProps().strength(3.0F, 18.0F).sound(SoundType.STONE)));
    public static final RegistrySupplier<Block> RBMK_CONTROL_REASIM_AUTO   = registerBlock("rbmk_control_reasim_auto",   () -> new RBMKControlAutoBlock(false, "rbmk_control_reasim_auto", rbmkProps().strength(3.0F, 18.0F).sound(SoundType.STONE)));

    // ── Passive Columns ─────────────────────────────────────────────────────
    public static final RegistrySupplier<Block> RBMK_MODERATOR    = registerBlock("rbmk_moderator",    () -> new RBMKModeratorBlock(rbmkProps().strength(3.0F, 18.0F).sound(SoundType.STONE)));
    public static final RegistrySupplier<Block> RBMK_ABSORBER     = registerBlock("rbmk_absorber",     () -> new RBMKAbsorberBlock(rbmkProps().strength(3.0F, 18.0F).sound(SoundType.STONE)));
    public static final RegistrySupplier<Block> RBMK_REFLECTOR    = registerBlock("rbmk_reflector",    () -> new RBMKReflectorBlock(rbmkProps().strength(3.0F, 18.0F).sound(SoundType.STONE)));
    public static final RegistrySupplier<Block> RBMK_COOLER       = registerBlock("rbmk_cooler",       () -> new RBMKCoolerBlock(rbmkProps().strength(3.0F, 18.0F).sound(SoundType.STONE)));
    public static final RegistrySupplier<Block> RBMK_BOILER       = registerBlock("rbmk_boiler",       () -> new RBMKBoilerBlock(rbmkProps().strength(3.0F, 18.0F).sound(SoundType.STONE)));
    public static final RegistrySupplier<Block> RBMK_HEATER       = registerBlock("rbmk_heater",       () -> new RBMKHeaterBlock(rbmkProps().strength(3.0F, 18.0F).sound(SoundType.STONE)));
    public static final RegistrySupplier<Block> RBMK_OUTGASSER    = registerBlock("rbmk_outgasser",    () -> new RBMKOutgasserBlock(rbmkProps().strength(3.0F, 18.0F).sound(SoundType.STONE)));
    public static final RegistrySupplier<Block> RBMK_STORAGE      = registerBlock("rbmk_storage",      () -> new RBMKStorageBlock(rbmkProps().strength(3.0F, 18.0F).sound(SoundType.STONE)));
    public static final RegistrySupplier<Block> RBMK_BLANK        = registerBlock("rbmk_blank",        () -> new RBMKBlankBlock(rbmkProps().strength(3.0F, 18.0F).sound(SoundType.STONE)));

    // ── Fluid Connection ────────────────────────────────────────────────────
    public static final RegistrySupplier<Block> RBMK_STEAM_INLET  = registerBlock("rbmk_steam_inlet",  () -> new RBMKSteamInletBlock(rbmkProps().strength(50.0F, 36.0F).sound(SoundType.STONE)));
    public static final RegistrySupplier<Block> RBMK_STEAM_OUTLET = registerBlock("rbmk_steam_outlet", () -> new RBMKSteamOutletBlock(rbmkProps().strength(50.0F, 36.0F).sound(SoundType.STONE)));

    // ── Crane / Loader ──────────────────────────────────────────────────────
    public static final RegistrySupplier<Block> RBMK_LOADER       = registerBlock("rbmk_loader",       () -> new RBMKLoaderBlock(rbmkProps().strength(50.0F, 36.0F).sound(SoundType.STONE)));
    public static final RegistrySupplier<Block> RBMK_AUTOLOADER   = registerBlock("rbmk_autoloader",   () -> new RBMKAutoloaderBlock(rbmkProps().strength(50.0F, 36.0F).sound(SoundType.STONE)));
    public static final RegistrySupplier<Block> RBMK_CRANE_CONSOLE= registerBlock("rbmk_crane_console",() -> new RBMKCraneConsoleBlock(rbmkProps().strength(3.0F, 18.0F).sound(SoundType.STONE)));

    /** Invisible solid filler placed above every column so it has a real 1x3 hitbox (see
     *  {@link com.hbm_m.block.machines.rbmk.RBMKColumnFillerBlock}). Not directly placeable;
     *  breaking it cascades into destroying the real column below (same feel/hardness as the
     *  column itself, matched via {@link #rbmkProps()}). */
    // dynamicShape() is load-bearing, not decoration: RBMKColumnFillerBlock returns a
    // position-dependent collision box (the topmost filler of a lidded column is 1.25 blocks tall).
    // Without it Minecraft builds a BlockState shape cache from
    // getCollisionShape(state, EmptyBlockGetter, BlockPos.ZERO), which resolves to a plain full
    // block - so hasLargeCollisionShape() reports false, and BlockCollisions then SKIPS this block
    // whenever it is only reached through the one-block ring around the entity's box. Standing on a
    // lid put the player exactly in that case: the real 1.25 shape was never queried and they sank
    // a quarter block into the lid.
    public static final RegistrySupplier<Block> RBMK_COLUMN_FILLER = registerBlockWithoutItem("rbmk_column_filler",
            () -> new com.hbm_m.block.machines.rbmk.RBMKColumnFillerBlock(rbmkProps().noLootTable().dynamicShape()));

    // ── Decorative / support blocks used by the original's RBMK recipes ──────
    // com.hbm.blocks.ModBlocks:1436-1437 - plain deco blocks reusing the rbmk column textures.
    public static final RegistrySupplier<Block> DECO_RBMK        = registerBlock("deco_rbmk",        () -> new Block(BlockProps.copy(Blocks.IRON_BLOCK).strength(5.0F, 60.0F).sound(SoundType.STONE)));
    public static final RegistrySupplier<Block> DECO_RBMK_SMOOTH = registerBlock("deco_rbmk_smooth", () -> new Block(BlockProps.copy(Blocks.IRON_BLOCK).strength(5.0F, 60.0F).sound(SoundType.STONE)));
    /**
     * CE ships four RBMK decoration blocks, not two: the plain and smooth casings plus a panelled
     * version of each ({@code deco_rbmk_panel} / {@code deco_rbmk_smooth_panel}). The panelled pair
     * was missing entirely.
     */
    public static final RegistrySupplier<Block> DECO_RBMK_PANEL        = registerBlock("deco_rbmk_panel",        () -> new Block(BlockProps.copy(Blocks.IRON_BLOCK).strength(5.0f, 100.0f)));
    public static final RegistrySupplier<Block> DECO_RBMK_SMOOTH_PANEL = registerBlock("deco_rbmk_smooth_panel", () -> new Block(BlockProps.copy(Blocks.IRON_BLOCK).strength(5.0f, 100.0f)));

    /**
     * CE's thin RBMK panel slabs. Each pair is a 2px single you craft and carry plus a 4px double
     * that only ever appears by stacking two singles - see {@link com.hbm_m.block.generic.RBMKSlabBlock}.
     * The doubles are deliberately kept out of the creative menu, exactly as CE does
     * ({@code setCreativeTab(null)}), and drop two singles when broken.
     */
    private static BlockBehaviour.Properties rbmkSlabProps() {
        return BlockProps.copy(Blocks.IRON_BLOCK).strength(5.0f, 10.0f).noOcclusion();
    }

    public static final RegistrySupplier<Block> DECO_RBMK_PANEL_SLAB4 = registerBlockWithoutItem("deco_rbmk_panel_slab4",
            () -> new com.hbm_m.block.generic.RBMKSlabBlock(true, rbmkSlabProps()));
    public static final RegistrySupplier<Block> DECO_RBMK_PANEL_SLAB2 = registerBlock("deco_rbmk_panel_slab2",
            () -> new com.hbm_m.block.generic.RBMKSlabBlock(false, rbmkSlabProps()));

    public static final RegistrySupplier<Block> DECO_RBMK_SMOOTH_PANEL_SLAB4 = registerBlockWithoutItem("deco_rbmk_smooth_panel_slab4",
            () -> new com.hbm_m.block.generic.RBMKSlabBlock(true, rbmkSlabProps()));
    public static final RegistrySupplier<Block> DECO_RBMK_SMOOTH_PANEL_SLAB2 = registerBlock("deco_rbmk_smooth_panel_slab2",
            () -> new com.hbm_m.block.generic.RBMKSlabBlock(false, rbmkSlabProps()));
    /** R7a: 1:1 BlockGraphite (brennbar 30/5). */
    public static final RegistrySupplier<Block> BLOCK_GRAPHITE = registerBlock("block_graphite",
            () -> new com.hbm_m.block.bomb.BlockFlammable(BlockProps.copy(Blocks.IRON_BLOCK).strength(5.0F, 6.0F).sound(SoundType.METAL).requiresCorrectToolForDrops(), 30, 5));

    // ── Debris ──────────────────────────────────────────────────────────────
    public static final RegistrySupplier<Block> RBMK_DEBRIS            = registerBlock("rbmk_debris",            () -> new Block(BlockProps.copy(Blocks.GRAVEL).strength(0.5f)));
    public static final RegistrySupplier<Block> RBMK_DEBRIS_BURNING    = registerBlock("rbmk_debris_burning",    () -> new com.hbm_m.block.machines.rbmk.RBMKDebrisBurningBlock(BlockProps.copy(Blocks.GRAVEL).requiresCorrectToolForDrops().randomTicks().strength(50.0F, 360.0F).sound(SoundType.STONE)));
    public static final RegistrySupplier<Block> RBMK_DEBRIS_DIGAMMA    = registerBlock("rbmk_debris_digamma",    () -> new com.hbm_m.block.machines.rbmk.RBMKDebrisDigammaBlock(BlockProps.copy(Blocks.GRAVEL).requiresCorrectToolForDrops().randomTicks().strength(50.0F, 360.0F).sound(SoundType.STONE)));
    public static final RegistrySupplier<Block> RBMK_DEBRIS_RADIATING  = registerBlock("rbmk_debris_radiating",  () -> new com.hbm_m.block.machines.rbmk.RBMKDebrisRadiatingBlock(BlockProps.copy(Blocks.GRAVEL).requiresCorrectToolForDrops().randomTicks().strength(50.0F, 360.0F).sound(SoundType.STONE)));

    // ── Corium (molten reactor core, 1:1 with the original's ModBlocks.corium_block) ──────────
    public static final RegistrySupplier<Block> RBMK_CORIUM = registerBlock("rbmk_corium",
            () -> new Block(BlockProps.copy(Blocks.MAGMA_BLOCK).strength(3.0f).lightLevel(s -> 15)));

    // ── Panel / Display Blocks ──────────────────────────────────────────────
    // RBMK_DISPLAY / RBMK_DISPLAY_BLANK: reactor-status link target (like the console/crane
    // targets), not one of the 7 RTTY devices below - stays on the generic no-op panel BE.
    public static final RegistrySupplier<Block> RBMK_DISPLAY   = registerBlock("rbmk_display",   () -> new RBMKDisplayBlock(rbmkProps().strength(3.0F, 18.0F).sound(SoundType.STONE)));
    /** Blank decorative panel, reuses the rbmk_display texture (matches the original, which had no dedicated texture for it either). */
    public static final RegistrySupplier<Block> RBMK_DISPLAY_BLANK = registerBlock("rbmk_display_blank", () -> new RBMKPanelBlock(rbmkProps().strength(3.0F, 18.0F).sound(SoundType.STONE)));

    // The 7 RTTY-driven panel devices - each wired to its own block entity, config screen and
    // (Lever/KeyPad) primary-click action via the shared RBMKPanelDeviceBlock (see that class).
    public static final RegistrySupplier<Block> RBMK_GAUGE = registerBlock("rbmk_gauge", () ->
            new com.hbm_m.block.machines.rbmk.RBMKPanelDeviceBlock(rbmkProps().strength(3.0F, 18.0F).sound(SoundType.STONE),
                    com.hbm_m.blockentity.machines.rbmk.RBMKGaugeBlockEntity::new,
                    () -> com.hbm_m.blockentity.ModBlockEntities.RBMK_GAUGE_BE.get(),
                    "gauge", true, null));

    public static final RegistrySupplier<Block> RBMK_INDICATOR = registerBlock("rbmk_indicator", () ->
            new com.hbm_m.block.machines.rbmk.RBMKPanelDeviceBlock(rbmkProps().strength(3.0F, 18.0F).sound(SoundType.STONE),
                    com.hbm_m.blockentity.machines.rbmk.RBMKIndicatorBlockEntity::new,
                    () -> com.hbm_m.blockentity.ModBlockEntities.RBMK_INDICATOR_BE.get(),
                    "indicator", true, null));

    public static final RegistrySupplier<Block> RBMK_NUMITRON = registerBlock("rbmk_numitron", () ->
            new com.hbm_m.block.machines.rbmk.RBMKPanelDeviceBlock(rbmkProps().strength(3.0F, 18.0F).sound(SoundType.STONE),
                    com.hbm_m.blockentity.machines.rbmk.RBMKNumitronBlockEntity::new,
                    () -> com.hbm_m.blockentity.ModBlockEntities.RBMK_NUMITRON_BE.get(),
                    "numitron", true, null));

    public static final RegistrySupplier<Block> RBMK_GRAPH = registerBlock("rbmk_graph", () ->
            new com.hbm_m.block.machines.rbmk.RBMKPanelDeviceBlock(rbmkProps().strength(3.0F, 18.0F).sound(SoundType.STONE),
                    com.hbm_m.blockentity.machines.rbmk.RBMKGraphBlockEntity::new,
                    () -> com.hbm_m.blockentity.ModBlockEntities.RBMK_GRAPH_BE.get(),
                    "graph", true, null));

    public static final RegistrySupplier<Block> RBMK_LEVER = registerBlock("rbmk_lever", () ->
            new com.hbm_m.block.machines.rbmk.RBMKPanelDeviceBlock(rbmkProps().strength(3.0F, 18.0F).sound(SoundType.STONE),
                    com.hbm_m.blockentity.machines.rbmk.RBMKLeverBlockEntity::new,
                    () -> com.hbm_m.blockentity.ModBlockEntities.RBMK_LEVER_BE.get(),
                    "lever", false,
                    (be, level, pos, player, hit) -> {
                        if (be instanceof com.hbm_m.blockentity.machines.rbmk.RBMKLeverBlockEntity lever) {
                            lever.flipLever(level, pos, player,
                                    com.hbm_m.blockentity.machines.rbmk.RBMKLeverBlockEntity.unitFromHit(pos, hit));
                        }
                    }));

    public static final RegistrySupplier<Block> RBMK_KEYPAD = registerBlock("rbmk_key_pad", () ->
            new com.hbm_m.block.machines.rbmk.RBMKPanelDeviceBlock(rbmkProps().strength(3.0F, 18.0F).sound(SoundType.STONE),
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
            new com.hbm_m.block.machines.rbmk.RBMKPanelDeviceBlock(rbmkProps().strength(3.0F, 18.0F).sound(SoundType.STONE),
                    com.hbm_m.blockentity.machines.rbmk.RBMKTerminalBlockEntity::new,
                    () -> com.hbm_m.blockentity.ModBlockEntities.RBMK_TERMINAL_BE.get(),
                    "terminal", false, null));

    // ══════════════════════════════════════════════════════════════════════
    // DEV: Blöcke aus dem Original-HBM-Mod, die hier noch fehlen (zur Sichtung)
    // Texturen importiert, generische Block-Properties als Platzhalter.
    // ══════════════════════════════════════════════════════════════════════
    /**
     * Original: {@code ancient_scrap} ist ein {@code BlockOutgas} mit {@code onNeighbour} und dem
     * Sonderfall in {@code breakBlock} - beim Abbau entsteht eine 5x5x5-Wolke Grabgas.
     */
    public static final RegistrySupplier<Block> ANCIENT_SCRAP = registerBlock("ancient_scrap",
            () -> new com.hbm_m.block.gas.OutgasBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(100.0F, 3600.0F).sound(SoundType.STONE),
                    () -> ModBlocks.GAS_RADON_TOMB.get(), true, true, true, true, false));
    public static final RegistrySupplier<Block> ASH_DIGAMMA = registerBlock("ash_digamma",
            () -> new com.hbm_m.block.generic.BlockAshes(BlockProps.copy(Blocks.SAND).strength(0.5F, 90.0F).sound(SoundType.SAND)));
    public static final RegistrySupplier<Block> ASPHALT_LIGHT = registerBlock("asphalt_light",
            () -> new com.hbm_m.block.generic.BlockSpeedy(BlockProps.copy(Blocks.STONE).strength(15.0F, 72.0F).sound(SoundType.STONE).requiresCorrectToolForDrops().lightLevel(s -> 15), 1.5));
    public static final RegistrySupplier<Block> BARBED_WIRE_ACID = registerBlock("barbed_wire_acid",
            () -> new com.hbm_m.block.weapons.BarbedWireBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(5.0F, 6.0F).requiresCorrectToolForDrops().noOcclusion().noCollission().sound(SoundType.STONE)));
    public static final RegistrySupplier<Block> BARBED_WIRE_ULTRADEATH = registerBlock("barbed_wire_ultradeath",
            () -> new com.hbm_m.block.weapons.BarbedWireBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(5.0F, 6.0F).requiresCorrectToolForDrops().noOcclusion().noCollission().sound(SoundType.STONE)));
    public static final RegistrySupplier<Block> BASALT = registerBlock("basalt",
            () -> new Block(BlockProps.copy(Blocks.STONE).strength(5.0F, 6.0F).sound(SoundType.STONE).requiresCorrectToolForDrops()));
    public static final RegistrySupplier<Block> BASALT_SMOOTH = registerBlock("basalt_smooth",
            () -> new Block(BlockProps.copy(Blocks.STONE).strength(5F, 6.0F).sound(SoundType.STONE).requiresCorrectToolForDrops()));
    public static final RegistrySupplier<Block> BASALT_TILES = registerBlock("basalt_tiles",
            () -> new Block(BlockProps.copy(Blocks.STONE).strength(5F, 6.0F).sound(SoundType.STONE).requiresCorrectToolForDrops()));
    public static final RegistrySupplier<Block> BATTERY_LITHIUM_BLOCK = registerBlock("battery_lithium_block", () -> new Block(BlockProps.copy(Blocks.STONE)));
    public static final RegistrySupplier<Block> BATTERY_POTATO_BLOCK = registerBlock("battery_potato_block", () -> new Block(BlockProps.copy(Blocks.STONE)));
    public static final RegistrySupplier<Block> BATTERY_SCHRABIDIUM_BLOCK = registerBlock("battery_schrabidium_block", () -> new Block(BlockProps.copy(Blocks.STONE)));
    /** R7n: 1:1 BlastDoor. */
    public static final RegistrySupplier<Block> BLAST_DOOR = registerBlock("blast_door",
            () -> new com.hbm_m.block.machines.BlastDoorBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(10.0F, 600.0F).requiresCorrectToolForDrops().noOcclusion().sound(SoundType.STONE)));
    /** R7n: 1:1 DummyBlockBlast (Rahmen des Explosionsschutztors). */
    public static final RegistrySupplier<Block> DUMMY_BLOCK_BLAST = registerBlockWithoutItem("dummy_block_blast",
            () -> new com.hbm_m.block.machines.BlastDoorBlock.Dummy(BlockProps.copy(Blocks.IRON_BLOCK).strength(10.0F, 6000.0F).requiresCorrectToolForDrops().noLootTable().sound(SoundType.STONE)));
    /** Original block_aluminium = Materialblock ALUMINUM (Registry-ID block_aluminium, siehe MaterialShape.itemId). */
    public static final RegistrySupplier<Block> BLOCK_ALUMINIUM = INGOT_BLOCKS.get(com.hbm_m.item.material.ModMaterials.ALUMINUM);
    /** R6b: 1:1 BlockBobble / BlockSnowglobe / BlockPlushie (Typ im Blockentity, Gegenstand mit NBT "type"). */
    public static final RegistrySupplier<Block> BOBBLEHEAD = registerBlock("bobblehead",
            () -> new com.hbm_m.block.decorations.TrinketBlock(BlockProps.copy(Blocks.IRON_BLOCK).instabreak().sound(SoundType.STONE).noOcclusion().noLootTable(), com.hbm_m.block.decorations.TrinketBlock.Kind.BOBBLE));
    public static final RegistrySupplier<Block> SNOWGLOBE = registerBlock("snowglobe",
            () -> new com.hbm_m.block.decorations.TrinketBlock(BlockProps.copy(Blocks.GLASS).instabreak().sound(SoundType.STONE).noOcclusion().noLootTable(), com.hbm_m.block.decorations.TrinketBlock.Kind.SNOWGLOBE));
    public static final RegistrySupplier<Block> PLUSHIE = registerBlock("plushie",
            () -> new com.hbm_m.block.decorations.TrinketBlock(BlockProps.copy(Blocks.WHITE_WOOL).instabreak().sound(SoundType.WOOL).noOcclusion().noLootTable(), com.hbm_m.block.decorations.TrinketBlock.Kind.PLUSHIE));
    /** R6b: 1:1 BlockSkeletonHolder (kein Kreativ-Reiter). */
    public static final RegistrySupplier<Block> SKELETON_HOLDER = registerBlock("skeleton_holder",
            () -> new com.hbm_m.block.decorations.SkeletonHolderBlock(BlockProps.copy(Blocks.STONE).strength(2.0F, 6.0F).sound(SoundType.STONE).requiresCorrectToolForDrops().noOcclusion()));
    /** R6b: 1:1 BlockVendingMachine, Untertyp 0 (Limonade) und 1 (Snacks). */
    public static final RegistrySupplier<Block> VENDING_MACHINE = registerBlock("vending_machine",
            () -> new com.hbm_m.block.decorations.VendingMachineBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(5F, 6.0F).sound(SoundType.STONE).requiresCorrectToolForDrops().noOcclusion(), false));
    public static final RegistrySupplier<Block> VENDING_MACHINE_SNACKS = registerBlock("vending_machine_snacks",
            () -> new com.hbm_m.block.decorations.VendingMachineBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(5F, 6.0F).sound(SoundType.STONE).requiresCorrectToolForDrops().noOcclusion(), true));
    /** R6b: 1:1 BlockEmitter. */
    public static final RegistrySupplier<Block> DECO_EMITTER = registerBlock("deco_emitter",
            () -> new com.hbm_m.block.decorations.EmitterBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(5F, 12.0F).sound(SoundType.STONE).requiresCorrectToolForDrops().noOcclusion()));
    /** R6b: 1:1 PartEmitter. */
    public static final RegistrySupplier<Block> PART_EMITTER = registerBlock("part_emitter",
            () -> new com.hbm_m.block.decorations.PartEmitterBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(5F, 12.0F).sound(SoundType.STONE).requiresCorrectToolForDrops().noOcclusion()));
    /** R6b: 1:1 DecoBlock boxcar / boat mit TileEntityDecoBlock. */
    public static final RegistrySupplier<Block> BOXCAR = registerBlock("boxcar",
            () -> new com.hbm_m.block.decorations.DecoModelTEBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(10F, 6.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion(), com.hbm_m.block.decorations.DecoModelTEBlock.Kind.BOXCAR));
    public static final RegistrySupplier<Block> BOAT = registerBlock("boat",
            () -> new com.hbm_m.block.decorations.DecoModelTEBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(10F, 6.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion(), com.hbm_m.block.decorations.DecoModelTEBlock.Kind.BOAT));
    /** Original: {@code brick_asbestos} ist ein {@code BlockOutgas} (Haerte 5, Widerstand 1000). */
    public static final RegistrySupplier<Block> BRICK_ASBESTOS = registerBlock("brick_asbestos",
            () -> new com.hbm_m.block.gas.OutgasBlock(BlockProps.copy(Blocks.STONE).strength(5.0F, 600.0F),
                    () -> ModBlocks.GAS_ASBESTOS.get(), true, true, false, false, true));
    public static final RegistrySupplier<Block> BRICK_COMPOUND = registerBlock("brick_compound",
            () -> new Block(BlockProps.copy(Blocks.STONE).strength(15F, 240.0F).sound(SoundType.STONE).requiresCorrectToolForDrops()));
    /** R6d: Werte 1:1 (BlockGeneric, Haerte 15, Widerstand 360). */
    public static final RegistrySupplier<Block> BRICK_JUNGLE = registerBlock("brick_jungle",
            () -> new Block(BlockProps.copy(Blocks.STONE).strength(15.0F, 216.0F).sound(SoundType.STONE).requiresCorrectToolForDrops()));
    /** R6d: 1:1 BlockBallsSpawner. */
    public static final RegistrySupplier<Block> BRICK_JUNGLE_CIRCLE = registerBlock("brick_jungle_circle",
            () -> new com.hbm_m.block.generic.JungleBricks.Circle(BlockProps.copy(Blocks.STONE).strength(15.0F, 216.0F).sound(SoundType.STONE).requiresCorrectToolForDrops()));
    public static final RegistrySupplier<Block> BRICK_JUNGLE_CRACKED = registerBlock("brick_jungle_cracked",
            () -> new Block(BlockProps.copy(Blocks.STONE).strength(15.0F, 216.0F).sound(SoundType.STONE).requiresCorrectToolForDrops()));
    /** R6d: 1:1 FragileBrick. */
    public static final RegistrySupplier<Block> BRICK_JUNGLE_FRAGILE = registerBlock("brick_jungle_fragile",
            () -> new com.hbm_m.block.generic.JungleBricks.Fragile(BlockProps.copy(Blocks.STONE).strength(15.0F, 216.0F).sound(SoundType.STONE).requiresCorrectToolForDrops()));
    /** R6d: 1:1 BlockGlyph. */
    public static final RegistrySupplier<Block> BRICK_JUNGLE_GLYPH = registerBlock("brick_jungle_glyph",
            () -> new com.hbm_m.block.generic.JungleBricks.Glyph(BlockProps.copy(Blocks.STONE).strength(15.0F, 216.0F).sound(SoundType.STONE).requiresCorrectToolForDrops()));
    public static final RegistrySupplier<Block> BRICK_JUNGLE_LAVA = registerBlock("brick_jungle_lava",
            () -> new Block(BlockProps.copy(Blocks.STONE).strength(15.0F, 216.0F).sound(SoundType.STONE).requiresCorrectToolForDrops().lightLevel(s -> 5)));
    public static final RegistrySupplier<Block> BRICK_JUNGLE_MYSTIC = registerBlock("brick_jungle_mystic",
            () -> new com.hbm_m.block.generic.BlockOre(BlockProps.copy(Blocks.STONE).strength(15.0F, 216.0F).sound(SoundType.STONE).requiresCorrectToolForDrops().lightLevel(s -> 5)));
    public static final RegistrySupplier<Block> BRICK_JUNGLE_OOZE = registerBlock("brick_jungle_ooze",
            () -> new com.hbm_m.block.generic.BlockOre(BlockProps.copy(Blocks.STONE).strength(15.0F, 216.0F).sound(SoundType.STONE).requiresCorrectToolForDrops().lightLevel(s -> 5)));
    /** R6d: 1:1 TrappedBrick. */
    public static final RegistrySupplier<Block> BRICK_JUNGLE_TRAP = registerBlock("brick_jungle_trap",
            () -> new com.hbm_m.block.generic.JungleBricks.Trapped(BlockProps.copy(Blocks.STONE).strength(15.0F, 216.0F).sound(SoundType.STONE).requiresCorrectToolForDrops()));
    public static final RegistrySupplier<Block> BRICK_RED = registerBlock("brick_red", () -> new RedBrickBlock(BlockProps.copy(Blocks.STONE).strength(0.0F, 6000.0F)));
    public static final RegistrySupplier<Block> BROADCASTER_PC = registerBlock("broadcaster_pc",
            () -> new com.hbm_m.block.machines.BroadcasterPcBlock(BlockProps.copy(Blocks.STONE).noOcclusion().strength(5.0F, 9.0F)));
    /** R7e: 1:1 CableDetector. */
    public static final RegistrySupplier<Block> CABLE_DETECTOR = registerBlock("cable_detector",
            () -> new com.hbm_m.api.energy.CableSwitchBlocks.Detector(BlockProps.copy(Blocks.IRON_BLOCK).strength(5.0F, 6.0F).requiresCorrectToolForDrops().sound(SoundType.STONE)));
    /** R7e: 1:1 CableDiode. */
    public static final RegistrySupplier<Block> CABLE_DIODE = registerBlock("cable_diode",
            () -> new com.hbm_m.api.energy.CableDiodeBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(5.0F, 6.0F).requiresCorrectToolForDrops().noOcclusion().sound(SoundType.STONE)));
    /** R7e: 1:1 CableSwitch. */
    public static final RegistrySupplier<Block> CABLE_SWITCH = registerBlock("cable_switch",
            () -> new com.hbm_m.api.energy.CableSwitchBlocks.Switch(BlockProps.copy(Blocks.IRON_BLOCK).strength(5.0F, 6.0F).requiresCorrectToolForDrops().sound(SoundType.STONE)));
    // capacitor_bus/gold/niobium/tantalium/schrabidate are all @Deprecated + hidden from the creative
    // tab in the 1.7.10 original (only capacitor_copper is player-facing); ported for completeness
    // using the shared MachineCapacitorBlock, no bus-chaining mechanic (see MachineCapacitorBlockEntity).
    public static final RegistrySupplier<Block> CAPACITOR_BUS = registerBlock("capacitor_bus",
            () -> new com.hbm_m.block.machines.MachineCapacitorBlock(BlockProps.copy(Blocks.STONE).noOcclusion().strength(5.0F, 6.0F), 1_000_000L));
    public static final RegistrySupplier<Block> CAPACITOR_COPPER = registerBlock("capacitor_copper",
            () -> new com.hbm_m.block.machines.MachineCapacitorBlock(BlockProps.copy(Blocks.STONE).noOcclusion().strength(5.0F, 6.0F), 1_000_000L));
    public static final RegistrySupplier<Block> CAPACITOR_GOLD = registerBlock("capacitor_gold",
            () -> new com.hbm_m.block.machines.MachineCapacitorBlock(BlockProps.copy(Blocks.STONE).noOcclusion().strength(5.0F, 6.0F), 5_000_000L));
    public static final RegistrySupplier<Block> CAPACITOR_NIOBIUM = registerBlock("capacitor_niobium",
            () -> new com.hbm_m.block.machines.MachineCapacitorBlock(BlockProps.copy(Blocks.STONE).noOcclusion().strength(5.0F, 6.0F), 25_000_000L));
    public static final RegistrySupplier<Block> CAPACITOR_SCHRABIDATE = registerBlock("capacitor_schrabidate",
            () -> new com.hbm_m.block.machines.MachineCapacitorBlock(BlockProps.copy(Blocks.STONE).noOcclusion().strength(5.0F, 6.0F), 50_000_000_000L));
    public static final RegistrySupplier<Block> CAPACITOR_TANTALIUM = registerBlock("capacitor_tantalium",
            () -> new com.hbm_m.block.machines.MachineCapacitorBlock(BlockProps.copy(Blocks.STONE).noOcclusion().strength(5.0F, 6.0F), 150_000_000L));
    /** Self-stacking 3x3 elevator shaft; see com.hbm_m.block.machines.CargoElevatorBlock. */
    public static final RegistrySupplier<Block> CARGO_ELEVATOR = registerBlock("cargo_elevator",
            () -> new com.hbm_m.block.machines.CargoElevatorBlock(BlockProps.copy(Blocks.IRON_BLOCK).noOcclusion().sound(SoundType.STONE)));
    /** R6c: 1:1 BlockChargeC4. */
    public static final RegistrySupplier<Block> CHARGE_C4 = registerBlock("charge_c4",
            () -> new com.hbm_m.block.bomb.ChargeBlocks.C4(BlockProps.copy(Blocks.TNT).instabreak().strength(0.0F, 0.6F).sound(SoundType.STONE).noOcclusion().noLootTable().noCollission()));
    /** R6c: 1:1 BlockChargeDynamite. */
    public static final RegistrySupplier<Block> CHARGE_DYNAMITE = registerBlock("charge_dynamite",
            () -> new com.hbm_m.block.bomb.ChargeBlocks.Dynamite(BlockProps.copy(Blocks.TNT).instabreak().strength(0.0F, 0.6F).sound(SoundType.STONE).noOcclusion().noLootTable().noCollission()));
    /** R6c: 1:1 BlockChargeMiner. */
    public static final RegistrySupplier<Block> CHARGE_MINER = registerBlock("charge_miner",
            () -> new com.hbm_m.block.bomb.ChargeBlocks.Miner(BlockProps.copy(Blocks.TNT).instabreak().strength(0.0F, 0.6F).sound(SoundType.STONE).noOcclusion().noLootTable().noCollission()));
    /** R6c: 1:1 BlockChargeSemtex. */
    public static final RegistrySupplier<Block> CHARGE_SEMTEX = registerBlock("charge_semtex",
            () -> new com.hbm_m.block.bomb.ChargeBlocks.Semtex(BlockProps.copy(Blocks.TNT).instabreak().strength(0.0F, 0.6F).sound(SoundType.STONE).noOcclusion().noLootTable().noCollission()));
    /**
     * Original: {@code BlockGasClorine} - der einzige Gasblock, der nicht dem {@code gas_*}-Schema
     * folgt, und der einzige, der ueberhaupt sichtbar ist.
     */
    public static final RegistrySupplier<Block> CHLORINE_GAS = registerBlock("chlorine_gas", com.hbm_m.block.gas.BlockGasChlorine::new);
    public static final RegistrySupplier<Block> CLUSTER_ALUMINIUM = registerBlock("cluster_aluminium",
            () -> new Block(BlockProps.copy(Blocks.STONE).strength(5.0F, 9.0F).sound(SoundType.STONE).requiresCorrectToolForDrops()));
    public static final RegistrySupplier<Block> CLUSTER_COPPER = registerBlock("cluster_copper",
            () -> new Block(BlockProps.copy(Blocks.STONE).strength(5.0F, 9.0F).sound(SoundType.STONE).requiresCorrectToolForDrops()));
    public static final RegistrySupplier<Block> CLUSTER_DEPTH_IRON = registerBlock("cluster_depth_iron",
            () -> new com.hbm_m.block.nature.DepthOreBlock(BlockProps.copy(Blocks.STONE).sound(SoundType.STONE).strength(-1.0F, 6.0F)));
    public static final RegistrySupplier<Block> CLUSTER_DEPTH_TITANIUM = registerBlock("cluster_depth_titanium",
            () -> new com.hbm_m.block.nature.DepthOreBlock(BlockProps.copy(Blocks.STONE).sound(SoundType.STONE).strength(-1.0F, 6.0F)));
    public static final RegistrySupplier<Block> CLUSTER_DEPTH_TUNGSTEN = registerBlock("cluster_depth_tungsten",
            () -> new com.hbm_m.block.nature.DepthOreBlock(BlockProps.copy(Blocks.STONE).sound(SoundType.STONE).strength(-1.0F, 6.0F)));
    public static final RegistrySupplier<Block> CLUSTER_IRON = registerBlock("cluster_iron",
            () -> new Block(BlockProps.copy(Blocks.STONE).strength(5.0F, 9.0F).sound(SoundType.STONE).requiresCorrectToolForDrops()));
    public static final RegistrySupplier<Block> CLUSTER_TITANIUM = registerBlock("cluster_titanium",
            () -> new Block(BlockProps.copy(Blocks.STONE).strength(5.0F, 9.0F).sound(SoundType.STONE).requiresCorrectToolForDrops()));
    /** BlockCMFlux/BlockCMHeat = BlockPillar (Achse wie Baumstamm), Haerte 5 / Widerstand 10. */
    public static final RegistrySupplier<Block> CM_FLUX = registerBlock("cm_flux",
            () -> new net.minecraft.world.level.block.RotatedPillarBlock(BlockProps.copy(Blocks.IRON_BLOCK).requiresCorrectToolForDrops().strength(5.0F, 6.0F).sound(SoundType.STONE)));
    public static final RegistrySupplier<Block> CM_HEAT = registerBlock("cm_heat",
            () -> new net.minecraft.world.level.block.RotatedPillarBlock(BlockProps.copy(Blocks.IRON_BLOCK).requiresCorrectToolForDrops().strength(5.0F, 6.0F).sound(SoundType.STONE)));

    // 1:1 Custom-Machine-System (BlockCM*, je Meta ein Block cm_<art>_<enum>)
    public static final Map<com.hbm_m.block.machines.custom.CMBlocks.CMMaterial, RegistrySupplier<Block>> CM_BLOCK = new EnumMap<>(com.hbm_m.block.machines.custom.CMBlocks.CMMaterial.class);
    public static final Map<com.hbm_m.block.machines.custom.CMBlocks.CMMaterial, RegistrySupplier<Block>> CM_SHEET = new EnumMap<>(com.hbm_m.block.machines.custom.CMBlocks.CMMaterial.class);
    public static final Map<com.hbm_m.block.machines.custom.CMBlocks.CMMaterial, RegistrySupplier<Block>> CM_TANK = new EnumMap<>(com.hbm_m.block.machines.custom.CMBlocks.CMMaterial.class);
    public static final Map<com.hbm_m.block.machines.custom.CMBlocks.CMMaterial, RegistrySupplier<Block>> CM_PORT = new EnumMap<>(com.hbm_m.block.machines.custom.CMBlocks.CMMaterial.class);
    public static final Map<com.hbm_m.block.machines.custom.CMBlocks.CMEngine, RegistrySupplier<Block>> CM_ENGINE = new EnumMap<>(com.hbm_m.block.machines.custom.CMBlocks.CMEngine.class);
    public static final Map<com.hbm_m.block.machines.custom.CMBlocks.CMCircuit, RegistrySupplier<Block>> CM_CIRCUIT = new EnumMap<>(com.hbm_m.block.machines.custom.CMBlocks.CMCircuit.class);
    static {
        for (var m : com.hbm_m.block.machines.custom.CMBlocks.CMMaterial.values()) {
            CM_BLOCK.put(m, registerBlock("cm_block_" + m.id(), () -> new Block(cmProps())));
            CM_SHEET.put(m, registerBlock("cm_sheet_" + m.id(), () -> new Block(cmProps())));
            CM_TANK.put(m, registerBlock("cm_tank_" + m.id(), () -> new com.hbm_m.block.machines.custom.CMBlocks.Glass(cmProps())));
            CM_PORT.put(m, registerBlock("cm_port_" + m.id(), () -> new com.hbm_m.block.machines.custom.CMBlocks.Port(cmProps())));
        }
        for (var m : com.hbm_m.block.machines.custom.CMBlocks.CMEngine.values()) CM_ENGINE.put(m, registerBlock("cm_engine_" + m.id(), () -> new Block(cmProps())));
        for (var m : com.hbm_m.block.machines.custom.CMBlocks.CMCircuit.values()) CM_CIRCUIT.put(m, registerBlock("cm_circuit_" + m.id(), () -> new Block(cmProps())));
    }
    private static net.minecraft.world.level.block.state.BlockBehaviour.Properties cmProps() {
        return BlockProps.copy(Blocks.IRON_BLOCK).strength(5.0F, 10.0F).sound(SoundType.METAL).requiresCorrectToolForDrops();
    }
    /** 1:1 BlockCustomMachine (Gegenstand = ModItems.CUSTOM_MACHINE mit machineType-NBT), Licht wie setLightLevel(1F). */
    public static final RegistrySupplier<Block> CUSTOM_MACHINE = registerBlockWithoutItem("custom_machine",
            () -> new com.hbm_m.block.machines.custom.CustomMachineBlock(BlockProps.copy(Blocks.IRON_BLOCK).lightLevel(s -> 15).strength(5.0F, 6.0F).sound(SoundType.STONE)));
    public static final RegistrySupplier<Block> CM_ANCHOR = registerBlock("custom_machine_anchor",
            () -> new com.hbm_m.block.machines.custom.CMBlocks.Anchor(BlockProps.copy(Blocks.IRON_BLOCK).strength(5.0F, 6.0F).sound(SoundType.STONE)));
    public static final RegistrySupplier<Block> CMB_BRICK = registerBlock("cmb_brick",
            () -> new Block(BlockProps.copy(Blocks.STONE).strength(25F, 3000.0F).sound(SoundType.STONE).requiresCorrectToolForDrops()));
    public static final RegistrySupplier<Block> CMB_BRICK_REINFORCED = registerBlock("cmb_brick_reinforced",
            () -> new Block(BlockProps.copy(Blocks.STONE).strength(25F, 30000.0F).sound(SoundType.STONE).requiresCorrectToolForDrops()));
    /** 1:1 CompactLauncher: 3x3-Werfer fuer Baukastenraketen (1.0er-Spitze). */
    public static final RegistrySupplier<Block> COMPACT_LAUNCHER = registerBlock("compact_launcher",
            () -> new com.hbm_m.block.machines.CompactLauncherBlock(BlockProps.copy(Blocks.IRON_BLOCK).noOcclusion().strength(5.0F, 6.0F).sound(SoundType.STONE)));
    public static final RegistrySupplier<Block> CONVEYOR = registerBlockWithoutItem("conveyor",
            () -> new com.hbm_m.block.network.ConveyorBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(2.0F, 1.2F).noOcclusion().sound(SoundType.STONE)));
    public static final RegistrySupplier<Block> CONVEYOR_DOUBLE = registerBlockWithoutItem("conveyor_double",
            () -> new com.hbm_m.block.network.ConveyorDoubleBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(2.0F, 1.2F).noOcclusion().sound(SoundType.STONE)));
    public static final RegistrySupplier<Block> CONVEYOR_EXPRESS = registerBlockWithoutItem("conveyor_express",
            () -> new com.hbm_m.block.network.ConveyorExpressBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(2.0F, 1.2F).noOcclusion().sound(SoundType.STONE)));
    public static final RegistrySupplier<Block> CONVEYOR_TRIPLE = registerBlockWithoutItem("conveyor_triple",
            () -> new com.hbm_m.block.network.ConveyorTripleBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(2.0F, 1.2F).noOcclusion().sound(SoundType.STONE)));
    public static final RegistrySupplier<Block> CONVEYOR_LIFT = registerBlockWithoutItem("conveyor_lift",
            () -> new com.hbm_m.block.network.ConveyorLiftBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(2.0F, 1.2F).noOcclusion().sound(SoundType.STONE)));
    public static final RegistrySupplier<Block> CONVEYOR_CHUTE = registerBlockWithoutItem("conveyor_chute",
            () -> new com.hbm_m.block.network.ConveyorChuteBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(2.0F, 1.2F).noOcclusion().sound(SoundType.STONE)));
    public static final RegistrySupplier<Block> CRANE_BOXER = registerBlock("crane_boxer",
            () -> new com.hbm_m.block.machines.MachineCraneBoxerBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(5.0F, 6.0F).requiresCorrectToolForDrops().sound(SoundType.STONE)));
    public static final RegistrySupplier<Block> CRANE_EXTRACTOR = registerBlock("crane_extractor",
            () -> new com.hbm_m.block.machines.MachineCraneExtractorBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(5.0F, 6.0F).requiresCorrectToolForDrops().sound(SoundType.STONE)));
    public static final RegistrySupplier<Block> CRANE_GRABBER = registerBlock("crane_grabber",
            () -> new com.hbm_m.block.machines.MachineCraneGrabberBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(5.0F, 6.0F).requiresCorrectToolForDrops().sound(SoundType.STONE)));
    public static final RegistrySupplier<Block> CRANE_INSERTER = registerBlock("crane_inserter",
            () -> new com.hbm_m.block.machines.MachineCraneInserterBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(5.0F, 6.0F).requiresCorrectToolForDrops().sound(SoundType.STONE)));
    /** R7k: 1:1 CranePartitioner. */
    public static final RegistrySupplier<Block> CRANE_PARTITIONER = registerBlock("crane_partitioner",
            () -> new com.hbm_m.block.network.CranePartitionerBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(5.0F, 6.0F).requiresCorrectToolForDrops().noOcclusion().sound(SoundType.STONE)));
    public static final RegistrySupplier<Block> CRANE_ROUTER = registerBlock("crane_router",
            () -> new com.hbm_m.block.machines.MachineCraneRouterBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(5.0F, 6.0F).requiresCorrectToolForDrops().sound(SoundType.STONE)));
    /** R7q: 1:1 CraneSplitter (2 Haelften). */
    public static final RegistrySupplier<Block> CRANE_SPLITTER = registerBlock("crane_splitter",
            () -> new com.hbm_m.block.machines.MachineCraneSplitterBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(5.0F, 6.0F).requiresCorrectToolForDrops().noOcclusion().sound(SoundType.STONE)));
    public static final RegistrySupplier<Block> CRANE_UNBOXER = registerBlock("crane_unboxer",
            () -> new com.hbm_m.block.machines.MachineCraneUnboxerBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(5.0F, 6.0F).requiresCorrectToolForDrops().sound(SoundType.STONE)));
    /** R6d: 1:1 BlockAmmoCrate. */
    public static final RegistrySupplier<Block> CRATE_AMMO = registerBlock("crate_ammo",
            () -> new com.hbm_m.block.generic.CrateBlocks.Ammo(BlockProps.copy(Blocks.IRON_BLOCK).strength(1.0F, 1.5F).sound(SoundType.METAL).requiresCorrectToolForDrops()));
    /** Original crate_supply (BlockSupplyCrate): Holz, Haerte 1, Widerstand 2,5, Aussehen wie crate_can. */
    public static final RegistrySupplier<Block> CRATE_SUPPLY = registerBlock("crate_supply",
            () -> new com.hbm_m.block.generic.SupplyCrateBlock(BlockProps.copy(Blocks.OAK_PLANKS).sound(SoundType.WOOD).strength(1.0F, 1.5F)));
    public static final RegistrySupplier<Block> CRATE_CAN = registerBlock("crate_can", () -> new CrateCanBlock(BlockBehaviour.Properties.of().mapColor(MapColor.STONE).sound(SoundType.WOOD).strength(1.0F, 1.5F)));
    /** R6d: 1:1 BlockJungleCrate. */
    public static final RegistrySupplier<Block> CRATE_JUNGLE = registerBlock("crate_jungle",
            () -> new com.hbm_m.block.generic.CrateBlocks.Jungle(BlockProps.copy(Blocks.STONE).strength(1.0F, 1.5F).sound(SoundType.STONE).requiresCorrectToolForDrops()));
    public static final RegistrySupplier<Block> CRATE_RED = registerBlock("crate_red",
            () -> new com.hbm_m.block.generic.CrateBlocks.Supply(BlockProps.copy(Blocks.IRON_BLOCK).strength(5.0F, 6.0F).sound(SoundType.METAL).requiresCorrectToolForDrops(), com.hbm_m.block.generic.CrateBlocks.Supply.Kind.RED));
    public static final RegistrySupplier<Block> DEPTH_DNT = registerBlock("depth_dnt",
            () -> new com.hbm_m.block.nature.DepthOreBlock(BlockProps.copy(Blocks.STONE).sound(SoundType.STONE).strength(-1.0F, 36000.0F)));
    /** R6c: 1:1 ExplosiveCharge. */
    public static final RegistrySupplier<Block> DET_CHARGE = registerBlock("det_charge",
            () -> new com.hbm_m.block.bomb.ExplosiveCharge(BlockProps.copy(Blocks.IRON_BLOCK).strength(0.1F, 0.0F).sound(SoundType.STONE).requiresCorrectToolForDrops(), false));
    /** R6c: 1:1 DetCord. */
    public static final RegistrySupplier<Block> DET_CORD = registerBlock("det_cord",
            () -> new com.hbm_m.block.bomb.DetCordBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(0.1F, 0.0F).sound(SoundType.STONE).requiresCorrectToolForDrops().noOcclusion()));
    public static final RegistrySupplier<Block> DET_NUKE = registerBlock("det_nuke",
            () -> new com.hbm_m.block.bomb.ExplosiveCharge(BlockProps.copy(Blocks.IRON_BLOCK).strength(0.1F, 0.0F).sound(SoundType.STONE).requiresCorrectToolForDrops(), true));
    public static final RegistrySupplier<Block> DFC_CORE = registerBlock("dfc_core",
            () -> new com.hbm_m.block.machines.dfc.DFCCoreBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(5.0F, 6.0F).sound(SoundType.STONE)));
    public static final RegistrySupplier<Block> DFC_STABILIZER = registerBlock("dfc_stabilizer",
            () -> new com.hbm_m.block.machines.dfc.DFCStabilizerBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(5.0F, 6.0F).sound(SoundType.STONE)));
    public static final RegistrySupplier<Block> DIRT_DEAD = registerBlock("dirt_dead",
            () -> new net.minecraft.world.level.block.FallingBlock(BlockProps.copy(Blocks.DIRT).strength(0.5F, 0.5F).sound(SoundType.GRAVEL)));
    public static final RegistrySupplier<Block> DIRT_OILY = registerBlock("dirt_oily",
            () -> new net.minecraft.world.level.block.FallingBlock(BlockProps.copy(Blocks.DIRT).strength(0.5F, 0.5F).sound(SoundType.GRAVEL)));
    public static final RegistrySupplier<Block> DRONE_CRATE = registerBlock("drone_crate",
            () -> new com.hbm_m.block.machines.MachineDroneCrateBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(0.1F, 6.0F).sound(SoundType.STONE)));
    public static final RegistrySupplier<Block> DRONE_CRATE_PROVIDER = registerBlock("drone_crate_provider",
            () -> new com.hbm_m.block.machines.MachineDroneProviderBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(0.1F, 6.0F).sound(SoundType.STONE)));
    public static final RegistrySupplier<Block> DRONE_CRATE_REQUESTER = registerBlock("drone_crate_requester",
            () -> new com.hbm_m.block.machines.MachineDroneRequesterBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(0.1F, 6.0F).sound(SoundType.STONE)));
    public static final RegistrySupplier<Block> DRONE_DOCK = registerBlock("drone_dock",
            () -> new com.hbm_m.block.machines.MachineDroneDockBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(0.1F, 6.0F).sound(SoundType.STONE)));
    public static final RegistrySupplier<Block> DRONE_WAYPOINT = registerBlock("drone_waypoint",
            () -> new com.hbm_m.block.machines.MachineDroneWaypointBlock(BlockBehaviour.Properties.of().mapColor(MapColor.STONE).noOcclusion().strength(0.1F, 6.0F)));
    public static final RegistrySupplier<Block> DRONE_WAYPOINT_REQUEST = registerBlock("drone_waypoint_request",
            () -> new com.hbm_m.block.machines.MachineDroneWaypointRequestBlock(BlockBehaviour.Properties.of().mapColor(MapColor.STONE).noOcclusion().strength(0.1F, 6.0F)));
    // ─── Radio Torch ("RTTY", Redstone Over Radio) family ──────────────────────
    public static final RegistrySupplier<Block> RADIO_TORCH_SENDER = registerBlock("radio_torch_sender",
            () -> new com.hbm_m.block.machines.radio.RadioTorchSenderBlock(BlockBehaviour.Properties.of().mapColor(MapColor.STONE).noOcclusion().strength(0.1F, 6.0F)));
    public static final RegistrySupplier<Block> RADIO_TORCH_RECEIVER = registerBlock("radio_torch_receiver",
            () -> new com.hbm_m.block.machines.radio.RadioTorchReceiverBlock(BlockBehaviour.Properties.of().mapColor(MapColor.STONE).noOcclusion().strength(0.1F, 6.0F)));
    public static final RegistrySupplier<Block> RADIO_TORCH_LOGIC = registerBlock("radio_torch_logic",
            () -> new com.hbm_m.block.machines.radio.RadioTorchLogicBlock(BlockBehaviour.Properties.of().mapColor(MapColor.STONE).noOcclusion().strength(0.1F, 6.0F)));
    public static final RegistrySupplier<Block> RADIO_TORCH_READER = registerBlock("radio_torch_reader",
            () -> new com.hbm_m.block.machines.radio.RadioTorchReaderBlock(BlockBehaviour.Properties.of().mapColor(MapColor.STONE).noOcclusion().strength(0.1F, 6.0F)));
    public static final RegistrySupplier<Block> RADIO_TORCH_CONTROLLER = registerBlock("radio_torch_controller",
            () -> new com.hbm_m.block.machines.radio.RadioTorchControllerBlock(BlockBehaviour.Properties.of().mapColor(MapColor.STONE).noOcclusion().strength(0.1F, 6.0F)));
    public static final RegistrySupplier<Block> RADIO_TORCH_COUNTER = registerBlock("radio_torch_counter",
            () -> new com.hbm_m.block.machines.radio.RadioTorchCounterBlock(BlockBehaviour.Properties.of().mapColor(MapColor.STONE).noOcclusion().strength(0.1F, 6.0F)));

    /** R6e: 1:1 BlockNoSpawn. */
    public static final RegistrySupplier<Block> DUCRETE = registerBlock("ducrete",
            () -> new com.hbm_m.block.generic.NTMPlants.NoSpawn(BlockProps.copy(Blocks.STONE).strength(20F, 300.0F).sound(SoundType.STONE).requiresCorrectToolForDrops()));
    /** R6c: 1:1 BlockDynamite (BlockTNTBase). */
    public static final RegistrySupplier<Block> DYNAMITE = registerBlock("dynamite",
            () -> new com.hbm_m.block.bomb.TNTBlocks.Dynamite(BlockProps.copy(Blocks.TNT).instabreak().sound(SoundType.GRASS)));
    public static final RegistrySupplier<Block> FACTORY_ADVANCED_HULL = registerBlock("factory_advanced_hull", () -> new Block(BlockProps.copy(Blocks.STONE)));
    public static final RegistrySupplier<Block> FACTORY_TITANIUM_HULL = registerBlock("factory_titanium_hull", () -> new Block(BlockProps.copy(Blocks.STONE)));
    public static final RegistrySupplier<Block> FENCE_METAL = registerBlock("fence_metal", () -> new net.minecraft.world.level.block.FenceBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(15.0F, 0.15F).sound(SoundType.STONE)));
    public static final RegistrySupplier<Block> FENCE_METAL_POST = registerBlock("fence_metal_post", () -> new DecorShapeBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(5.0F, 6.0F),
            Shapes.box(0.375, 0, 0.375, 0.625, 1.0, 0.625), false, false));
    /** R7d: 1:1 MachineFieldDisturber. */
    public static final RegistrySupplier<Block> FIELD_DISTURBER = registerBlock("field_disturber",
            () -> new com.hbm_m.block.machines.FieldDisturberBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(5.0F, 120.0F).requiresCorrectToolForDrops().sound(SoundType.STONE)));
    /** R6c: 1:1 DigammaFlame. */
    public static final RegistrySupplier<Block> FIRE_DIGAMMA = registerBlock("fire_digamma",
            () -> new com.hbm_m.block.bomb.DigammaBlocks.Flame(BlockBehaviour.Properties.copy(Blocks.FIRE).instabreak().lightLevel(s -> 15).noLootTable().noOcclusion().noCollission().sound(SoundType.STONE)));
    /** R6c: 1:1 BlockFireworks. */
    public static final RegistrySupplier<Block> FIREWORKS = registerBlock("fireworks",
            () -> new com.hbm_m.block.bomb.FireworksBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(0.0F, 3.0F).sound(SoundType.STONE)));
    /** R6c: 1:1 BlockFissureBomb. */
    public static final RegistrySupplier<Block> FISSURE_BOMB = registerBlock("fissure_bomb",
            () -> new com.hbm_m.block.bomb.TNTBlocks.FissureBomb(BlockProps.copy(Blocks.TNT).instabreak().sound(SoundType.GRASS)));
    /** R6c: 1:1 BlockFissure ("Geothermal Vent"). */
    public static final RegistrySupplier<Block> ORE_VOLCANO = registerBlock("ore_volcano",
            () -> new com.hbm_m.block.generic.FissureBlock(BlockProps.copy(Blocks.BEDROCK).requiresCorrectToolForDrops().strength(-1.0F, 600000.0F).lightLevel(s -> 15).randomTicks()));
    /** R6c: 1:1 BombFlameWar. */
    public static final RegistrySupplier<Block> FLAME_WAR = registerBlock("flame_war",
            () -> new com.hbm_m.block.bomb.SimpleBombs.FlameWar(BlockProps.copy(Blocks.IRON_BLOCK).strength(5.0F, 120.0F).sound(SoundType.STONE).requiresCorrectToolForDrops()));
    /** R7g: 1:1 FluidCounterValve. */
    public static final RegistrySupplier<Block> FLUID_COUNTER_VALVE = registerBlock("fluid_counter_valve",
            () -> new com.hbm_m.block.machines.FluidValveBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(5.0F, 6.0F).requiresCorrectToolForDrops().sound(SoundType.STONE), com.hbm_m.block.machines.FluidValveBlock.Mode.COUNTER));
    /** R7i: 1:1 FluidDuctBox. */
    public static final RegistrySupplier<Block> FLUID_DUCT_BOX = registerBlock("fluid_duct_box",
            () -> new com.hbm_m.block.network.BoxDuctBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(5.0F, 6.0F).requiresCorrectToolForDrops().noOcclusion().sound(com.hbm_m.sound.ModSoundTypes.PIPE), com.hbm_m.block.network.BoxDuctGeometry.Kind.FLUID));
    /** R7i: 1:1 FluidDuctBoxExhaust. */
    public static final RegistrySupplier<Block> FLUID_DUCT_EXHAUST = registerBlock("fluid_duct_exhaust",
            () -> new com.hbm_m.block.network.BoxDuctBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(5.0F, 6.0F).requiresCorrectToolForDrops().noOcclusion().sound(com.hbm_m.sound.ModSoundTypes.PIPE), com.hbm_m.block.network.BoxDuctGeometry.Kind.EXHAUST));
    /** R7h: 1:1 FluidDuctPaintable. */
    public static final RegistrySupplier<Block> FLUID_DUCT_PAINTABLE = registerBlock("fluid_duct_paintable",
            () -> new com.hbm_m.block.network.PaintableDuctBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(5.0F, 6.0F).requiresCorrectToolForDrops().sound(SoundType.STONE), false));
    /** Original fluid_duct_gauge (FluidDuctGauge): Haerte 5, Widerstand 10. */
    public static final RegistrySupplier<Block> FLUID_DUCT_GAUGE = registerBlock("fluid_duct_gauge",
            () -> new com.hbm_m.block.network.FluidDuctGaugeBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(5.0F, 6.0F).sound(SoundType.STONE)));
    /** R7j: 1:1 FluidPipeAnchor. */
    public static final RegistrySupplier<Block> PIPE_ANCHOR = registerBlock("pipe_anchor",
            () -> new com.hbm_m.block.network.PipeAnchorBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(5.0F, 6.0F).requiresCorrectToolForDrops().noOcclusion().sound(com.hbm_m.sound.ModSoundTypes.PIPE)));
    /** 1:1 ExhaustPipeAnchor (Abgas-Rohranker). */
    public static final RegistrySupplier<Block> PIPE_ANCHOR_EXHAUST = registerBlock("pipe_anchor_exhaust",
            () -> new com.hbm_m.block.network.ExhaustPipeAnchorBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(5.0F, 6.0F).requiresCorrectToolForDrops().noOcclusion().sound(com.hbm_m.sound.ModSoundTypes.PIPE)));
    /** 1:1 PneumoPipeAnchor (pneumatischer Rohranker). */
    public static final RegistrySupplier<Block> PIPE_ANCHOR_PNEUMATIC = registerBlock("pipe_anchor_pneumatic",
            () -> new com.hbm_m.block.network.PneumaticPipeAnchorBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(5.0F, 6.0F).requiresCorrectToolForDrops().noOcclusion().sound(com.hbm_m.sound.ModSoundTypes.PIPE)));
    /** R7g: 1:1 FluidSwitch. */
    public static final RegistrySupplier<Block> FLUID_SWITCH = registerBlock("fluid_switch",
            () -> new com.hbm_m.block.machines.FluidValveBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(5.0F, 6.0F).requiresCorrectToolForDrops().sound(SoundType.STONE), com.hbm_m.block.machines.FluidValveBlock.Mode.SWITCH));
    public static final RegistrySupplier<Block> FOUNDRY_MOLD = registerBlock("foundry_mold",
            () -> new com.hbm_m.block.machines.MachineFoundryMoldBlock(BlockProps.copy(Blocks.IRON_BLOCK).noOcclusion().isSuffocating((state, world, pos) -> false).strength(5.0F, 6.0F).sound(SoundType.STONE)));
    public static final RegistrySupplier<Block> FOUNDRY_SLAGTAP = registerBlock("foundry_slagtap",
            () -> new com.hbm_m.block.machines.MachineFoundrySlagtapBlock(BlockProps.copy(Blocks.IRON_BLOCK).noOcclusion().isSuffocating((state, world, pos) -> false).strength(5.0F, 6.0F).sound(SoundType.STONE)));
    public static final RegistrySupplier<Block> FOUNDRY_TANK = registerBlock("foundry_tank",
            () -> new com.hbm_m.block.machines.MachineFoundryTankBlock(BlockProps.copy(Blocks.STONE).noOcclusion().isSuffocating((state, world, pos) -> false).strength(5.0F, 6.0F)));
    /**
     * Port of the original's separately-registered dynamic {@code ModBlocks.slag} (molten puddle) -
     * not the same as {@link #BLOCK_SLAG}. No item form: the original has {@code setCreativeTab(null)}
     * (never obtainable as an item, only ever placed by the slagtap).
     */
    public static final RegistrySupplier<Block> SLAG_DYNAMIC = registerBlockWithoutItem("slag",
            () -> new com.hbm_m.block.generic.DynamicSlagBlock(BlockProps.copy(Blocks.STONE).noOcclusion().strength(5.0F, 6.0F)));
    public static final RegistrySupplier<Block> FROZEN_DIRT = registerBlock("frozen_dirt",
            () -> new com.hbm_m.block.generic.BlockOre(BlockProps.copy(Blocks.DIRT).strength(0.5F, 1.5F).sound(SoundType.GLASS)));
    public static final RegistrySupplier<Block> FROZEN_GRASS = registerBlock("frozen_grass",
            () -> new com.hbm_m.block.generic.WasteEarth(BlockProps.copy(Blocks.DIRT).strength(0.5F, 1.5F).sound(SoundType.GLASS), false));
    public static final RegistrySupplier<Block> FROZEN_LOG = registerBlock("frozen_log",
            () -> new Block(BlockProps.copy(Blocks.OAK_PLANKS).strength(0.5F, 1.5F).sound(SoundType.GLASS)));
    public static final RegistrySupplier<Block> FROZEN_PLANKS = registerBlock("frozen_planks",
            () -> new com.hbm_m.block.generic.BlockOre(BlockProps.copy(Blocks.OAK_PLANKS).strength(0.5F, 1.5F).sound(SoundType.GLASS)));
    /** 1:1-Port von {@code BlockFusionComponent} (1.7.10): mit dem Schneidbrenner zur geschweissten Spule. */
    public static final RegistrySupplier<Block> FUSION_COMPONENT = registerBlock("fusion_component",
            () -> new FusionComponentBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(5.0F, 18.0F).sound(SoundType.STONE)));
    // Im Original sind das die Metadaten 1-3 desselben Blocks wie FUSION_COMPONENT und teilen sich
    // daher zwangslaeufig dessen Werte: Material.iron, setHardness(5.0F), setResistance(30.0F).
    // Als Platzhalter standen sie auf copy(Blocks.STONE) - also Sprengfestigkeit 6 statt 30 an einem
    // Fusionsreaktor, und Steinabbau statt Metall.
    public static final RegistrySupplier<Block> FUSION_COMPONENT_BLANKET = registerBlock("fusion_component_blanket",
            () -> new Block(BlockProps.copy(Blocks.IRON_BLOCK).strength(5.0f, 30.0f)));
    public static final RegistrySupplier<Block> FUSION_COMPONENT_BSCCO_WELDED = registerBlock("fusion_component_bscco_welded",
            () -> new Block(BlockProps.copy(Blocks.IRON_BLOCK).strength(5.0f, 30.0f)));
    public static final RegistrySupplier<Block> FUSION_COMPONENT_MOTOR = registerBlock("fusion_component_motor",
            () -> new Block(BlockProps.copy(Blocks.IRON_BLOCK).strength(5.0f, 30.0f)));
    /** 1:1-Port von {@code FusionHatch} (1.7.10): Material.iron, Haerte 5, Widerstand 10, mit Blickrichtung. */
    public static final RegistrySupplier<Block> FUSION_HATCH = registerBlock("fusion_hatch",
            () -> new com.hbm_m.block.machines.fusion.FusionHatchBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(5.0F, 6.0F).sound(SoundType.STONE)));
    /**
     * Original: {@code new BlockPillar(Material.iron, ":fusion_heater_top")}, Haerte 5, Widerstand 10.
     * Ein Pillar richtet seine Achse nach der Seite aus, auf die man ihn setzt - im Port lag der
     * Deckel bisher fest auf oben/unten, weil es ein gewoehnlicher Wuerfel war.
     */
    public static final RegistrySupplier<Block> FUSION_HEATER = registerBlock("fusion_heater",
            () -> new net.minecraft.world.level.block.RotatedPillarBlock(
                    BlockProps.copy(Blocks.IRON_BLOCK).strength(5.0F, 6.0F).sound(SoundType.STONE)));
    public static final RegistrySupplier<Block> GAS_ASBESTOS = registerBlock("gas_asbestos", com.hbm_m.block.gas.BlockGasAsbestos::new);
    public static final RegistrySupplier<Block> GAS_COAL = registerBlock("gas_coal", com.hbm_m.block.gas.BlockGasCoal::new);
    public static final RegistrySupplier<Block> GAS_EXPLOSIVE = registerBlock("gas_explosive", com.hbm_m.block.gas.BlockGasExplosive::new);
    public static final RegistrySupplier<Block> GAS_FLAMMABLE = registerBlock("gas_flammable", com.hbm_m.block.gas.BlockGasFlammable::new);
    public static final RegistrySupplier<Block> GAS_MELTDOWN = registerBlock("gas_meltdown", com.hbm_m.block.gas.BlockGasMeltdown::new);
    public static final RegistrySupplier<Block> GAS_MONOXIDE = registerBlock("gas_monoxide", com.hbm_m.block.gas.BlockGasMonoxide::new);
    public static final RegistrySupplier<Block> GAS_RADON = registerBlock("gas_radon", com.hbm_m.block.gas.BlockGasRadon::new);
    public static final RegistrySupplier<Block> GAS_RADON_DENSE = registerBlock("gas_radon_dense", com.hbm_m.block.gas.BlockGasRadonDense::new);
    public static final RegistrySupplier<Block> GAS_RADON_TOMB = registerBlock("gas_radon_tomb", com.hbm_m.block.gas.BlockGasRadonTomb::new);
    public static final RegistrySupplier<Block> GLASS_ASH = registerBlock("glass_ash",
            () -> new com.hbm_m.block.generic.BlockNTMGlass(BlockProps.copy(Blocks.GLASS).strength(3.0F, 3.0F).sound(SoundType.GLASS)));
    public static final RegistrySupplier<Block> GLASS_BORON = registerBlock("glass_boron",
            () -> new com.hbm_m.block.generic.BlockNTMGlass(BlockProps.copy(Blocks.GLASS).strength(0.3F, 0.3F).sound(SoundType.GLASS)));
    public static final RegistrySupplier<Block> GLASS_LEAD = registerBlock("glass_lead",
            () -> new com.hbm_m.block.generic.BlockNTMGlass(BlockProps.copy(Blocks.GLASS).strength(0.3F, 0.3F).sound(SoundType.GLASS)));
    public static final RegistrySupplier<Block> GLASS_POLARIZED = registerBlock("glass_polarized",
            () -> new com.hbm_m.block.generic.BlockNTMGlass(BlockProps.copy(Blocks.GLASS).strength(0.3F, 0.3F).sound(SoundType.GLASS)));
    public static final RegistrySupplier<Block> GLASS_POLONIUM = registerBlock("glass_polonium",
            () -> new com.hbm_m.block.generic.BlockNTMGlass(BlockProps.copy(Blocks.GLASS).strength(0.3F, 0.3F).sound(SoundType.GLASS).lightLevel(s -> 5)));
    public static final RegistrySupplier<Block> GLASS_QUARTZ = registerBlock("glass_quartz",
            () -> new com.hbm_m.block.generic.BlockNTMGlass(BlockProps.copy(Blocks.PACKED_ICE).strength(1.0F, 24.0F).sound(SoundType.GLASS)));
    public static final RegistrySupplier<Block> GLASS_TRINITITE = registerBlock("glass_trinitite",
            () -> new com.hbm_m.block.generic.BlockNTMGlass(BlockProps.copy(Blocks.GLASS).strength(0.3F, 0.3F).sound(SoundType.GLASS).lightLevel(s -> 5)));
    public static final RegistrySupplier<Block> GLASS_URANIUM = registerBlock("glass_uranium",
            () -> new com.hbm_m.block.generic.BlockNTMGlass(BlockProps.copy(Blocks.GLASS).strength(0.3F, 0.3F).sound(SoundType.GLASS).lightLevel(s -> 5)));
    public static final RegistrySupplier<Block> GLYPHID_BASE = registerBlock("glyphid_base",
            () -> new com.hbm_m.block.generic.GlyphidBlock(BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(0.5F).sound(com.hbm_m.sound.ModSoundTypes.FLESH)));
    public static final RegistrySupplier<Block> GLYPHID_SPAWNER = registerBlock("glyphid_spawner",
            () -> new com.hbm_m.block.generic.GlyphidSpawnerBlock(BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(0.5F).sound(com.hbm_m.sound.ModSoundTypes.FLESH)));
    public static final RegistrySupplier<Block> GRAVEL_DIAMOND = registerBlock("gravel_diamond",
            () -> new net.minecraft.world.level.block.FallingBlock(BlockProps.copy(Blocks.SAND).strength(0.6F, 0.6F).sound(SoundType.GRAVEL)));
    public static final RegistrySupplier<Block> GRAVEL_OBSIDIAN = registerBlock("gravel_obsidian",
            () -> new net.minecraft.world.level.block.FallingBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(5.0F, 144.0F).sound(SoundType.GRAVEL).requiresCorrectToolForDrops()));
    /** R7b: 1:1 BlockHadronCoil. */
    public static final RegistrySupplier<Block> HADRON_COIL_ALLOY = registerBlock("hadron_coil_alloy",
            () -> new com.hbm_m.block.machines.HadronCoilBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(5.0F, 6.0F).sound(SoundType.METAL).requiresCorrectToolForDrops(), 10));
    public static final RegistrySupplier<Block> HADRON_COIL_CHLOROPHYTE = registerBlock("hadron_coil_chlorophyte",
            () -> new com.hbm_m.block.machines.HadronCoilBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(5.0F, 6.0F).sound(SoundType.METAL).requiresCorrectToolForDrops(), 2500));
    public static final RegistrySupplier<Block> HADRON_COIL_GOLD = registerBlock("hadron_coil_gold",
            () -> new com.hbm_m.block.machines.HadronCoilBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(5.0F, 6.0F).sound(SoundType.METAL).requiresCorrectToolForDrops(), 25));
    public static final RegistrySupplier<Block> HADRON_COIL_MAGTUNG = registerBlock("hadron_coil_magtung",
            () -> new com.hbm_m.block.machines.HadronCoilBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(5.0F, 6.0F).sound(SoundType.METAL).requiresCorrectToolForDrops(), 100));
    public static final RegistrySupplier<Block> HADRON_COIL_MESE = registerBlock("hadron_coil_mese",
            () -> new com.hbm_m.block.machines.HadronCoilBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(5.0F, 6.0F).sound(SoundType.METAL).requiresCorrectToolForDrops(), 10000));
    public static final RegistrySupplier<Block> HADRON_COIL_NEODYMIUM = registerBlock("hadron_coil_neodymium",
            () -> new com.hbm_m.block.machines.HadronCoilBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(5.0F, 6.0F).sound(SoundType.METAL).requiresCorrectToolForDrops(), 50));
    public static final RegistrySupplier<Block> HADRON_COIL_SCHRABIDATE = registerBlock("hadron_coil_schrabidate",
            () -> new com.hbm_m.block.machines.HadronCoilBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(5.0F, 6.0F).sound(SoundType.METAL).requiresCorrectToolForDrops(), 500));
    public static final RegistrySupplier<Block> HADRON_COIL_SCHRABIDIUM = registerBlock("hadron_coil_schrabidium",
            () -> new com.hbm_m.block.machines.HadronCoilBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(5.0F, 6.0F).sound(SoundType.METAL).requiresCorrectToolForDrops(), 250));
    public static final RegistrySupplier<Block> HADRON_COIL_STARMETAL = registerBlock("hadron_coil_starmetal",
            () -> new com.hbm_m.block.machines.HadronCoilBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(5.0F, 6.0F).sound(SoundType.METAL).requiresCorrectToolForDrops(), 1000));
    public static final RegistrySupplier<Block> HEV_BATTERY = registerBlock("hev_battery",
            () -> new com.hbm_m.block.generic.HevBatteryBlock(BlockProps.copy(Blocks.STONE).noOcclusion().noCollission().strength(0.5F, 0.15F).lightLevel(s -> 10)));
    public static final RegistrySupplier<Block> ICF_COMPONENT = registerBlock("icf_component",
            () -> new com.hbm_m.block.machines.icf.ICFComponentBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(5.0F, 36.0F).sound(SoundType.STONE)));
    public static final RegistrySupplier<Block> ICF_COMPONENT_STRUCTURE = registerBlock("icf_component_structure",
            () -> new com.hbm_m.block.machines.icf.ICFComponentBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(5.0f, 60.0f).sound(SoundType.METAL),
                    com.hbm_m.block.machines.icf.ICFComponentBlock.Tool.BOLT, () -> ModBlocks.ICF_COMPONENT_STRUCTURE_BOLTED.get()));
    public static final RegistrySupplier<Block> ICF_COMPONENT_STRUCTURE_BOLTED = registerBlock("icf_component_structure_bolted",
            () -> new com.hbm_m.block.machines.icf.ICFComponentBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(5.0f, 60.0f).sound(SoundType.METAL)));
    public static final RegistrySupplier<Block> ICF_COMPONENT_VESSEL = registerBlock("icf_component_vessel",
            () -> new com.hbm_m.block.machines.icf.ICFComponentBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(5.0f, 60.0f).sound(SoundType.METAL),
                    com.hbm_m.block.machines.icf.ICFComponentBlock.Tool.TORCH, () -> ModBlocks.ICF_COMPONENT_VESSEL_WELDED.get()));
    public static final RegistrySupplier<Block> ICF_COMPONENT_VESSEL_WELDED = registerBlock("icf_component_vessel_welded",
            () -> new com.hbm_m.block.machines.icf.ICFComponentBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(5.0f, 60.0f).sound(SoundType.METAL)));
    public static final RegistrySupplier<Block> ICF_CONTROLLER = registerBlock("icf_controller",
            () -> new com.hbm_m.block.machines.icf.ICFControllerBlock(
                    BlockProps.copy(Blocks.IRON_BLOCK).strength(5.0F, 36.0F).sound(SoundType.STONE)));
    public static final RegistrySupplier<Block> ITER = registerBlock("iter", () -> new Block(BlockProps.copy(Blocks.STONE)));
    public static final RegistrySupplier<Block> LADDER_ALUMINIUM = registerBlock("ladder_aluminium", () -> new net.minecraft.world.level.block.LadderBlock(BlockProps.copy(Blocks.LADDER)));
    public static final RegistrySupplier<Block> LADDER_COBALT = registerBlock("ladder_cobalt", () -> new net.minecraft.world.level.block.LadderBlock(BlockProps.copy(Blocks.LADDER)));
    public static final RegistrySupplier<Block> LADDER_COPPER = registerBlock("ladder_copper", () -> new net.minecraft.world.level.block.LadderBlock(BlockProps.copy(Blocks.LADDER).strength(0.25F, 1.2F).sound(SoundType.STONE)));
    public static final RegistrySupplier<Block> LADDER_GOLD = registerBlock("ladder_gold", () -> new net.minecraft.world.level.block.LadderBlock(BlockProps.copy(Blocks.LADDER).strength(0.25F, 1.2F).sound(SoundType.STONE)));
    public static final RegistrySupplier<Block> LADDER_IRON = registerBlock("ladder_iron", () -> new net.minecraft.world.level.block.LadderBlock(BlockProps.copy(Blocks.LADDER)));
    public static final RegistrySupplier<Block> LADDER_LEAD = registerBlock("ladder_lead", () -> new net.minecraft.world.level.block.LadderBlock(BlockProps.copy(Blocks.LADDER)));
    public static final RegistrySupplier<Block> LADDER_STEEL = registerBlock("ladder_steel", () -> new net.minecraft.world.level.block.LadderBlock(BlockProps.copy(Blocks.LADDER).strength(0.25F, 1.2F).sound(SoundType.STONE)));
    public static final RegistrySupplier<Block> LADDER_STURDY = registerBlock("ladder_sturdy", () -> new net.minecraft.world.level.block.LadderBlock(BlockProps.copy(Blocks.LADDER).strength(0.25F, 1.2F).sound(SoundType.STONE)));
    public static final RegistrySupplier<Block> LADDER_TITANIUM = registerBlock("ladder_titanium", () -> new net.minecraft.world.level.block.LadderBlock(BlockProps.copy(Blocks.LADDER).strength(0.25F, 1.2F).sound(SoundType.STONE)));
    public static final RegistrySupplier<Block> LADDER_TUNGSTEN = registerBlock("ladder_tungsten", () -> new net.minecraft.world.level.block.LadderBlock(BlockProps.copy(Blocks.LADDER)));
    /** R6d: 1:1 DemonLamp. */
    public static final RegistrySupplier<Block> LAMP_DEMON = registerBlock("lamp_demon",
            () -> new com.hbm_m.block.machines.DemonLampBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(3.0F, 3.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().lightLevel(s -> 15).noOcclusion()));
    public static final RegistrySupplier<Block> LAMP_TRITIUM_BLUE_OFF = registerBlock("lamp_tritium_blue_off",
            () -> new com.hbm_m.block.decorations.TritiumLampBlock(BlockBehaviour.Properties.of().mapColor(net.minecraft.world.level.material.MapColor.NONE).strength(3.0F, 3.0F).sound(SoundType.GLASS), false, 2, () -> ModBlocks.LAMP_TRITIUM_BLUE_OFF.get(), () -> ModBlocks.LAMP_TRITIUM_BLUE_ON.get()));
    public static final RegistrySupplier<Block> LAMP_TRITIUM_BLUE_ON = registerBlock("lamp_tritium_blue_on",
            () -> new com.hbm_m.block.decorations.TritiumLampBlock(BlockBehaviour.Properties.of().mapColor(net.minecraft.world.level.material.MapColor.NONE).strength(3.0F, 3.0F).sound(SoundType.GLASS), true, 2, () -> ModBlocks.LAMP_TRITIUM_BLUE_OFF.get(), () -> ModBlocks.LAMP_TRITIUM_BLUE_ON.get()));
    public static final RegistrySupplier<Block> LAMP_TRITIUM_GREEN_OFF = registerBlock("lamp_tritium_green_off",
            () -> new com.hbm_m.block.decorations.TritiumLampBlock(BlockBehaviour.Properties.of().mapColor(net.minecraft.world.level.material.MapColor.NONE).strength(3.0F, 3.0F).sound(SoundType.GLASS), false, 1, () -> ModBlocks.LAMP_TRITIUM_GREEN_OFF.get(), () -> ModBlocks.LAMP_TRITIUM_GREEN_ON.get()));
    public static final RegistrySupplier<Block> LAMP_TRITIUM_GREEN_ON = registerBlock("lamp_tritium_green_on",
            () -> new com.hbm_m.block.decorations.TritiumLampBlock(BlockBehaviour.Properties.of().mapColor(net.minecraft.world.level.material.MapColor.NONE).strength(3.0F, 3.0F).sound(SoundType.GLASS), true, 1, () -> ModBlocks.LAMP_TRITIUM_GREEN_OFF.get(), () -> ModBlocks.LAMP_TRITIUM_GREEN_ON.get()));
    public static final RegistrySupplier<Block> LIGHTSTONE_BRICKS = registerBlock("lightstone_bricks",
            () -> new Block(BlockProps.copy(Blocks.STONE).strength(2F, 9.0F).sound(SoundType.STONE).requiresCorrectToolForDrops()));
    public static final RegistrySupplier<Block> LIGHTSTONE_BRICKS_CHISELED = registerBlock("lightstone_bricks_chiseled",
            () -> new Block(BlockProps.copy(Blocks.STONE).strength(2F, 9.0F).sound(SoundType.STONE).requiresCorrectToolForDrops()));
    public static final RegistrySupplier<Block> LIGHTSTONE_CHISELED = registerBlock("lightstone_chiseled",
            () -> new Block(BlockProps.copy(Blocks.STONE).strength(2F, 9.0F).sound(SoundType.STONE).requiresCorrectToolForDrops()));
    public static final RegistrySupplier<Block> LIGHTSTONE_TILE = registerBlock("lightstone_tile",
            () -> new Block(BlockProps.copy(Blocks.STONE).strength(2F, 9.0F).sound(SoundType.STONE).requiresCorrectToolForDrops()));
    public static final RegistrySupplier<Block> LIGHTSTONE_UNREFINED = registerBlock("lightstone_unrefined",
            () -> new Block(BlockProps.copy(Blocks.STONE).strength(2F, 9.0F).sound(SoundType.STONE).requiresCorrectToolForDrops()));
    public static final RegistrySupplier<Block> MACHINE_AUTOCRAFTER = registerBlock("machine_autocrafter", () -> new com.hbm_m.block.machines.MachineAutocrafterBlock(BlockProps.copy(Blocks.STONE).strength(10.0F, 12.0F)));
    public static final RegistrySupplier<Block> MACHINE_CHUNGUS = registerBlockWithoutItem("machine_chungus",
            () -> new com.hbm_m.block.machines.MachineChungusBlock(BlockProps.copy(Blocks.IRON_BLOCK).noOcclusion().isSuffocating((state, world, pos) -> false).strength(5.0F, 6.0F).sound(SoundType.STONE)));
    /**
     * Original: {@code machine_controller} - das Reaktorsteuerpult, Haerte 5 / Widerstand 10.
     * Halbhohe Hitbox wie im Original, siehe {@code MachineReactorControlBlock}.
     */
    public static final RegistrySupplier<Block> MACHINE_CONTROLLER = registerBlock("machine_controller",
            () -> new com.hbm_m.block.machines.MachineReactorControlBlock(
                    BlockProps.copy(Blocks.IRON_BLOCK).requiresCorrectToolForDrops().noOcclusion().strength(5.0F, 6.0F).sound(SoundType.STONE)));
    public static final RegistrySupplier<Block> MACHINE_CONVERTER_HE_RF = registerBlock("machine_converter_he_rf",
            () -> new com.hbm_m.block.machines.MachineConverterHeRfBlock(BlockProps.copy(Blocks.IRON_BLOCK).noOcclusion().strength(5.0F, 6.0F).sound(SoundType.STONE)));
    public static final RegistrySupplier<Block> MACHINE_CONVERTER_RF_HE = registerBlock("machine_converter_rf_he",
            () -> new com.hbm_m.block.machines.MachineConverterRfHeBlock(BlockProps.copy(Blocks.IRON_BLOCK).noOcclusion().strength(5.0F, 6.0F).sound(SoundType.STONE)));
    /** 1:1 {@code MachineFENSU} (LEGACY): Multiblock 5x5x3, Haerte 5 / Widerstand 10, kein Kreativ-Tab. */
    public static final RegistrySupplier<Block> MACHINE_FENSU = registerBlock("machine_fensu",
            () -> new com.hbm_m.block.machines.MachineFENSUBlock(BlockProps.copy(Blocks.IRON_BLOCK).requiresCorrectToolForDrops().noOcclusion().strength(5.0F, 6.0F).sound(SoundType.STONE)));
    public static final RegistrySupplier<Block> MACHINE_FORCEFIELD = registerBlock("machine_forcefield",
            () -> new com.hbm_m.block.machines.ForceFieldBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(5.0F, 60.0F).sound(SoundType.STONE)));
    public static final RegistrySupplier<Block> MACHINE_FUNNEL = registerBlock("machine_funnel", () -> new com.hbm_m.block.machines.MachineFunnelBlock(BlockProps.copy(Blocks.STONE).strength(10.0F, 12.0F)));
    public static final RegistrySupplier<Block> MACHINE_ICF_PRESS = registerBlock("machine_icf_press",
            () -> new com.hbm_m.block.machines.icf.MachineICFPressBlock(
                    BlockProps.copy(Blocks.IRON_BLOCK).strength(5.0F, 36.0F).sound(SoundType.STONE)));
    public static final RegistrySupplier<Block> MACHINE_KEYFORGE = registerBlock("machine_keyforge",
            () -> new com.hbm_m.block.machines.MachineKeyforgeBlock(BlockProps.copy(Blocks.STONE).noOcclusion().strength(5.0F, 6.0F)));
    public static final RegistrySupplier<Block> MACHINE_LARGE_TURBINE = registerBlock("machine_large_turbine",
            () -> new com.hbm_m.block.machines.MachineLargeTurbineBlock(BlockProps.copy(Blocks.IRON_BLOCK).noOcclusion().strength(5.0F, 6.0F).sound(SoundType.STONE)));
    public static final RegistrySupplier<Block> MACHINE_MISSILE_ASSEMBLY = registerBlock("machine_missile_assembly",
            () -> new com.hbm_m.block.machines.MachineMissileAssemblyBlock(BlockProps.copy(Blocks.STONE).strength(5.0F, 6.0F)));
    public static final RegistrySupplier<Block> MACHINE_PUF6_TANK = registerBlock("machine_puf6_tank",
            () -> new com.hbm_m.block.decorations.DecoObjBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(5.0F, 6.0F).requiresCorrectToolForDrops().noOcclusion().noLootTable().sound(SoundType.STONE), com.hbm_m.block.decorations.DecoObjBlock.Placement.OPPOSITE));
    /** Genuinely missing from the port until now - machine_difurnace_off/_extension already exist under
     * blast_furnace/blast_furnace_extension (renamed IDs); only the RTG-heated variant was a real gap. */
    public static final RegistrySupplier<Block> MACHINE_DIFURNACE_RTG = registerBlock("machine_difurnace_rtg_off",
            () -> new com.hbm_m.block.machines.MachineDifurnaceRtgBlock(BlockProps.copy(Blocks.IRON_BLOCK).noOcclusion().strength(5.0F, 6.0F).sound(SoundType.STONE)));
    /** 1:1 {@code machine_rtg_furnace_off/_on} (MachineRtgFurnace, im Original deprecated): an/aus ueber LIT. */
    public static final RegistrySupplier<Block> MACHINE_RTG_FURNACE = registerBlock("machine_rtg_furnace_off",
            () -> new com.hbm_m.block.machines.MachineRtgFurnaceBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(5.0F, 6.0F).sound(SoundType.STONE)));
    /** Genuinely missing from the port until now. */
    public static final RegistrySupplier<Block> MACHINE_TELEPORTER = registerBlock("machine_teleporter",
            () -> new com.hbm_m.block.machines.MachineTeleporterBlock(BlockProps.copy(Blocks.IRON_BLOCK).noOcclusion().strength(5.0F, 6.0F).sound(SoundType.STONE)));
    /** R7d: MachineTeleanchor (nur Texturen; Funktion in ItemAnchorRemote). */
    public static final RegistrySupplier<Block> TELEANCHOR = registerBlock("teleanchor",
            () -> new Block(BlockProps.copy(Blocks.IRON_BLOCK).strength(5.0F, 6.0F).requiresCorrectToolForDrops().sound(SoundType.STONE)));
    /** Genuinely missing from the port until now. */
    public static final RegistrySupplier<Block> MACHINE_DRAIN = registerBlock("machine_drain",
            () -> new com.hbm_m.block.machines.MachineDrainBlock(BlockProps.copy(Blocks.STONE).noOcclusion().strength(5.0F, 6.0F)));
    /** Genuinely missing from the port until now - purely decorative in the original, no TileEntity (see class javadoc). */
    public static final RegistrySupplier<Block> MACHINE_TRANSFORMER = registerBlock("machine_transformer",
            () -> new com.hbm_m.block.generic.MachineTransformerBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(5.0F, 6.0F).sound(SoundType.STONE)));
    /** Genuinely missing from the port until now. */
    public static final RegistrySupplier<Block> MACHINE_WASTE_DRUM = registerBlock("machine_waste_drum",
            () -> new com.hbm_m.block.machines.MachineWasteDrumBlock(BlockProps.copy(Blocks.IRON_BLOCK).noOcclusion().strength(5.0F, 6.0F).sound(SoundType.STONE)));
    public static final RegistrySupplier<Block> MACHINE_RADGEN = registerBlock("machine_radgen",
            () -> new com.hbm_m.block.machines.MachineRadGenBlock(BlockProps.copy(Blocks.IRON_BLOCK).noOcclusion().strength(5.0F, 6.0F).sound(SoundType.STONE)));
    /**
     * Original: {@code machine_rtg} (Anzeigename "machine_rtg_grey"), Haerte 5 / Widerstand 10.
     * Radioisotopengenerator - wandelt die Waerme von RTG-Pellets unmittelbar in Energie.
     */
    /**
     * Original: {@code machine_bigasstank} - Grosstank mit 16 Millionen mB, Textur block_steel.
     */
    public static final RegistrySupplier<Block> MACHINE_BIGASSTANK = registerBlock("machine_bigasstank",
            () -> new com.hbm_m.block.machines.BarrelTankBlock(
                    BlockProps.copy(Blocks.IRON_BLOCK).requiresCorrectToolForDrops().noOcclusion().strength(5.0F, 6.0F).sound(SoundType.STONE),
                    com.hbm_m.blockentity.machines.MachineBigAssTankBlockEntity::new,
                    () -> com.hbm_m.blockentity.ModBlockEntities.BIGASSTANK_BE.get(),
                    new com.hbm_m.block.machines.BarrelTankBlock.TooltipInfo(
                            com.hbm_m.blockentity.machines.MachineBigAssTankBlockEntity.CAPACITY,
                            true, true, false, false),
                    // w16b: 1:1 MachineBigAssTank.getAllDimensions/fillSpace (getOffset 6) - vorher Einzelblock ohne Hitbox
                    () -> com.hbm_m.multiblock.DummyableStructureBuilder.create()
                            .box(5, 0, 4, 4, 4, 4)
                            .box(4, 0, 5, -4, 2, 2)
                            .box(4, 0, -4, 5, 2, 2)
                            .box(4, 0, 2, 2, 5, -4)
                            .box(4, 0, 2, 2, -4, 5)
                            .box(3, 0, 6, -5, 0, 0)
                            .box(3, 0, -5, 6, 0, 0)
                            .extra(6, 0, 0)
                            .extra(-6, 0, 0)
                            .placementOffset(6)
                            .build(() -> ModBlocks.UNIVERSAL_MACHINE_PART.get().defaultBlockState())));

    public static final RegistrySupplier<Block> MACHINE_RTG = registerBlock("machine_rtg",
            () -> new com.hbm_m.block.machines.MachineRTGBlock(
                    BlockProps.copy(Blocks.IRON_BLOCK).requiresCorrectToolForDrops().strength(5.0F, 6.0F).sound(SoundType.STONE).noOcclusion()));

    /** Original machine_satlink (Satelliten-Bodenstation), Haerte 5, Widerstand 10. */
    public static final RegistrySupplier<Block> MACHINE_SATLINK = registerBlock("machine_satlink",
            () -> new com.hbm_m.block.machines.MachineSatLinkBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(5.0F, 6.0F).requiresCorrectToolForDrops().noOcclusion().sound(SoundType.STONE)));
    public static final RegistrySupplier<Block> MACHINE_SATLINKER = registerBlock("machine_satlinker",
            () -> new com.hbm_m.block.machines.MachineSatLinkerBlock(BlockProps.copy(Blocks.STONE).strength(5.0F, 6.0F)));
    public static final RegistrySupplier<Block> MACHINE_STORAGE_DRUM = registerBlock("machine_storage_drum",
            () -> new com.hbm_m.block.machines.MachineStorageDrumBlock(BlockProps.copy(Blocks.IRON_BLOCK).noOcclusion().strength(5.0F, 6.0F).sound(SoundType.STONE)));
    /** R7f: 1:1 MachineUF6Tank/MachinePuF6Tank (Legacy, laesst nichts fallen). */
    public static final RegistrySupplier<Block> MACHINE_UF6_TANK = registerBlock("machine_uf6_tank",
            () -> new com.hbm_m.block.decorations.DecoObjBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(5.0F, 6.0F).requiresCorrectToolForDrops().noOcclusion().noLootTable().sound(SoundType.STONE), com.hbm_m.block.decorations.DecoObjBlock.Placement.OPPOSITE));
    public static final RegistrySupplier<Block> MASS_STORAGE = registerBlock("mass_storage",
            () -> new com.hbm_m.block.machines.MachineMassStorageBlock(BlockProps.copy(Blocks.STONE).noOcclusion().strength(5.0F, 6.0F).sound(SoundType.METAL)));

    /** 1:1 aus {@code BlockMassStorage}: Holz fasst hundert Stueck. */
    public static final RegistrySupplier<Block> MASS_STORAGE_WOOD = registerBlock("mass_storage_wood",
            () -> new com.hbm_m.block.machines.MachineMassStorageBlock(
                    BlockProps.copy(Blocks.OAK_PLANKS).strength(2.0f, 3.0f).sound(SoundType.WOOD),
                    com.hbm_m.block.machines.MachineMassStorageBlock.Tier.WOOD));

    /** 1:1: Eisen fasst zehntausend. */
    public static final RegistrySupplier<Block> MASS_STORAGE_IRON = registerBlock("mass_storage_iron",
            () -> new com.hbm_m.block.machines.MachineMassStorageBlock(
                    BlockProps.copy(Blocks.IRON_BLOCK).strength(4.0f, 4.0f).sound(SoundType.METAL),
                    com.hbm_m.block.machines.MachineMassStorageBlock.Tier.IRON));

    /** 1:1: Desh fasst hunderttausend. */
    public static final RegistrySupplier<Block> MASS_STORAGE_DESH = registerBlock("mass_storage_desh",
            () -> new com.hbm_m.block.machines.MachineMassStorageBlock(
                    BlockProps.copy(Blocks.IRON_BLOCK).strength(5.0f, 6.0f).sound(SoundType.METAL),
                    com.hbm_m.block.machines.MachineMassStorageBlock.Tier.DESH));
    public static final RegistrySupplier<Block> METEOR_SPAWNER = registerBlock("meteor_spawner", () -> new com.hbm_m.block.machines.CybercrabSpawnerBlock(BlockProps.copy(Blocks.STONE).noLootTable().strength(15.0F, 216.0F)));
    public static final RegistrySupplier<Block> MINE_HE = registerBlock("mine_he", () -> new com.hbm_m.block.bomb.LandmineBlock(BlockProps.copy(Blocks.STONE).noOcclusion().strength(1.0F, 1.0F), 2D, 5D));
    public static final RegistrySupplier<Block> MINE_NAVAL = registerBlock("mine_naval", () -> new com.hbm_m.block.bomb.LandmineBlock(BlockProps.copy(Blocks.IRON_BLOCK).noOcclusion().strength(1.0F, 1.0F).sound(SoundType.STONE), 2.5D, 1D));
    public static final RegistrySupplier<Block> MINE_SHRAP = registerBlock("mine_shrap", () -> new com.hbm_m.block.bomb.LandmineBlock(BlockProps.copy(Blocks.STONE).noOcclusion().strength(1.0F, 1.0F), 1.5D, 1D));
    public static final RegistrySupplier<Block> MOON_TURF = registerBlock("moon_turf",
            () -> new net.minecraft.world.level.block.FallingBlock(BlockProps.copy(Blocks.SAND).strength(0.5F, 0.5F).sound(SoundType.SAND)));
    /** R6d: 1:1 BlockMush / BlockMushHuge. */
    public static final RegistrySupplier<Block> MUSH = registerBlock("mush",
            () -> new com.hbm_m.block.generic.R6dPlants.Mush(BlockProps.copy(Blocks.BROWN_MUSHROOM).instabreak().sound(SoundType.GRASS).lightLevel(s -> 7).randomTicks().noOcclusion().noCollission()));
    public static final RegistrySupplier<Block> MUSH_BLOCK = registerBlock("mush_block",
            () -> new com.hbm_m.block.generic.R6dPlants.MushHuge(BlockProps.copy(Blocks.BROWN_MUSHROOM_BLOCK).strength(0.2F).sound(SoundType.GRASS).lightLevel(s -> 15)));
    public static final RegistrySupplier<Block> NUKE_FSTBMB = registerBlock("nuke_fstbmb",
            () -> new com.hbm_m.block.bomb.NukeFstbmbBlock(BlockProps.copy(Blocks.STONE).noOcclusion().strength(5.0F, 120.0F)));

    public static final RegistrySupplier<Block> NUKE_CUSTOM = registerBlock("nuke_custom",
            () -> new com.hbm_m.block.bomb.NukeCustomBlock(BlockProps.copy(Blocks.STONE).noOcclusion().strength(5.0F, 120.0F)));

    public static final RegistrySupplier<Block> BOMB_MULTI = registerBlock("bomb_multi",
            () -> new com.hbm_m.block.bomb.BombMultiBlock(BlockProps.copy(Blocks.STONE).noOcclusion().strength(0.0F, 120.0F)));
    public static final RegistrySupplier<Block> NUKE_N2 = registerBlock("nuke_n2",
            () -> new com.hbm_m.block.bomb.NukeN2Block(BlockProps.copy(Blocks.STONE).noOcclusion().strength(5.0F, 120.0F)));
    public static final RegistrySupplier<Block> NUKE_SOLINIUM = registerBlock("nuke_solinium",
            () -> new com.hbm_m.block.bomb.NukeSoliniumBlock(BlockProps.copy(Blocks.STONE).noOcclusion().strength(5.0F, 120.0F)));
    public static final RegistrySupplier<Block> OIL_SPILL = registerBlock("oil_spill", () -> new OilSpillBlock(BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(0.1F).noOcclusion().sound(SoundType.SNOW)));
    /** Порт {@code BlockPedestal} (1.7.10) — постамент с парящим предметом. */
    public static final RegistrySupplier<Block> PEDESTAL = registerBlock("pedestal",
            () -> new PedestalBlock(BlockProps.copy(Blocks.STONE).noOcclusion().strength(2.0F, 6.0F)));
    /** R6d: 1:1 BlockPinkLog (Eichenrinde, rosa Ende). */
    public static final RegistrySupplier<Block> PINK_LOG = registerBlock("pink_log",
            () -> new RotatedPillarBlock(BlockProps.copy(Blocks.OAK_LOG).strength(0.5F).sound(SoundType.WOOD)));
    public static final RegistrySupplier<Block> PINK_PLANKS = registerBlock("pink_planks",
            () -> new Block(BlockProps.copy(Blocks.OAK_PLANKS).instabreak().sound(SoundType.WOOD)));
    /** R6e: 1:1 BlockNTMFlower. */
    public static final RegistrySupplier<Block> PLANT_FLOWER_CD0 = registerBlock("plant_flower_cd0",
            () -> new com.hbm_m.block.generic.NTMPlants.Flower(BlockProps.copy(Blocks.POPPY).strength(0.0F).sound(SoundType.GRASS).noCollission().instabreak().noOcclusion(), com.hbm_m.block.generic.NTMPlants.FlowerType.CD0));
    /** R6e: 1:1 BlockNTMFlower. */
    public static final RegistrySupplier<Block> PLANT_FLOWER_CD1 = registerBlock("plant_flower_cd1",
            () -> new com.hbm_m.block.generic.NTMPlants.Flower(BlockProps.copy(Blocks.POPPY).strength(0.0F).sound(SoundType.GRASS).noCollission().instabreak().noOcclusion(), com.hbm_m.block.generic.NTMPlants.FlowerType.CD1));
    /** R6e: 1:1 BlockNTMFlower. */
    public static final RegistrySupplier<Block> PLANT_FLOWER_FOXGLOVE = registerBlock("plant_flower_foxglove",
            () -> new com.hbm_m.block.generic.NTMPlants.Flower(BlockProps.copy(Blocks.POPPY).strength(0.0F).sound(SoundType.GRASS).noCollission().instabreak().noOcclusion(), com.hbm_m.block.generic.NTMPlants.FlowerType.FOXGLOVE));
    /** R6e: 1:1 BlockNTMFlower. */
    public static final RegistrySupplier<Block> PLANT_FLOWER_NIGHTSHADE = registerBlock("plant_flower_nightshade",
            () -> new com.hbm_m.block.generic.NTMPlants.Flower(BlockProps.copy(Blocks.POPPY).strength(0.0F).sound(SoundType.GRASS).noCollission().instabreak().noOcclusion(), com.hbm_m.block.generic.NTMPlants.FlowerType.NIGHTSHADE));
    /** R6e: 1:1 BlockNTMFlower. */
    public static final RegistrySupplier<Block> PLANT_FLOWER_TOBACCO = registerBlock("plant_flower_tobacco",
            () -> new com.hbm_m.block.generic.NTMPlants.Flower(BlockProps.copy(Blocks.POPPY).strength(0.0F).sound(SoundType.GRASS).noCollission().instabreak().noOcclusion(), com.hbm_m.block.generic.NTMPlants.FlowerType.TOBACCO));
    /** R6e: 1:1 BlockNTMFlower. */
    public static final RegistrySupplier<Block> PLANT_FLOWER_WEED = registerBlock("plant_flower_weed",
            () -> new com.hbm_m.block.generic.NTMPlants.Flower(BlockProps.copy(Blocks.POPPY).strength(0.0F).sound(SoundType.GRASS).noCollission().instabreak().noOcclusion(), com.hbm_m.block.generic.NTMPlants.FlowerType.WEED));
    public static final RegistrySupplier<Block> PLASMA_HEATER = registerBlock("plasma_heater", () -> new Block(BlockProps.copy(Blocks.STONE)));
    public static final RegistrySupplier<Block> PNEUMATIC_TUBE = registerBlock("pneumatic_tube",
            () -> new com.hbm_m.block.network.PneumoTubeBlock(
                    BlockProps.copy(Blocks.IRON_BLOCK).noOcclusion().strength(2.0F, 6.0F).sound(com.hbm_m.sound.ModSoundTypes.PIPE)));
    public static final RegistrySupplier<Block> PNEUMATIC_TUBE_PAINTABLE = registerBlock("pneumatic_tube_paintable",
            () -> new com.hbm_m.block.network.PneumoTubePaintableBlock(
                    BlockProps.copy(Blocks.IRON_BLOCK).noOcclusion().sound(SoundType.STONE)));
    public static final RegistrySupplier<Block> PRESS_PREHEATER = registerBlock("press_preheater",
            () -> new Block(BlockProps.copy(Blocks.IRON_BLOCK).strength(5F, 6.0F).requiresCorrectToolForDrops().sound(SoundType.STONE)));
    /** R7r: 1:1 BlockPWR (Traeger des zusammengebauten Reaktors). */
    public static final RegistrySupplier<Block> PWR_BLOCK = registerBlockWithoutItem("pwr_block",
            () -> new com.hbm_m.block.machines.PWRBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(15.0F, 6.0F).requiresCorrectToolForDrops().noLootTable().sound(SoundType.STONE)));
    public static final RegistrySupplier<Block> PWR_CASING = registerBlock("pwr_casing",
            () -> new com.hbm_m.block.machines.PWRPartBlock(com.hbm_m.block.machines.PWRPart.Kind.CASING, BlockProps.copy(Blocks.IRON_BLOCK).strength(5.0F, 6.0F).requiresCorrectToolForDrops().sound(SoundType.STONE)));
    public static final RegistrySupplier<Block> PWR_CHANNEL = registerBlock("pwr_channel",
            () -> new com.hbm_m.block.machines.PWRPartBlock.Pillar(com.hbm_m.block.machines.PWRPart.Kind.CHANNEL, BlockProps.copy(Blocks.IRON_BLOCK).strength(5.0F, 6.0F).requiresCorrectToolForDrops().sound(SoundType.STONE)));
    public static final RegistrySupplier<Block> PWR_CONTROL = registerBlock("pwr_control",
            () -> new com.hbm_m.block.machines.PWRPartBlock.Pillar(com.hbm_m.block.machines.PWRPart.Kind.CONTROL, BlockProps.copy(Blocks.IRON_BLOCK).strength(5.0F, 6.0F).requiresCorrectToolForDrops().sound(SoundType.STONE)));
    /** R7r: 1:1 MachinePWRController. */
    public static final RegistrySupplier<Block> PWR_CONTROLLER = registerBlock("pwr_controller",
            () -> new com.hbm_m.block.machines.MachinePWRControllerBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(5.0F, 6.0F).requiresCorrectToolForDrops().sound(SoundType.STONE)));
    public static final RegistrySupplier<Block> PWR_FUEL = registerBlock("pwr_fuel",
            () -> new com.hbm_m.block.machines.PWRPartBlock.Pillar(com.hbm_m.block.machines.PWRPart.Kind.FUEL, BlockProps.copy(Blocks.IRON_BLOCK).strength(5.0F, 6.0F).requiresCorrectToolForDrops().sound(SoundType.STONE)));
    public static final RegistrySupplier<Block> PWR_HEATEX = registerBlock("pwr_heatex",
            () -> new com.hbm_m.block.machines.PWRPartBlock(com.hbm_m.block.machines.PWRPart.Kind.HEATEX, BlockProps.copy(Blocks.IRON_BLOCK).strength(5.0F, 6.0F).requiresCorrectToolForDrops().sound(SoundType.STONE)));
    public static final RegistrySupplier<Block> PWR_HEATSINK = registerBlock("pwr_heatsink",
            () -> new com.hbm_m.block.machines.PWRPartBlock(com.hbm_m.block.machines.PWRPart.Kind.HEATSINK, BlockProps.copy(Blocks.IRON_BLOCK).strength(5.0F, 6.0F).requiresCorrectToolForDrops().sound(SoundType.STONE)));
    public static final RegistrySupplier<Block> PWR_NEUTRON_SOURCE = registerBlock("pwr_neutron_source",
            () -> new com.hbm_m.block.machines.PWRPartBlock(com.hbm_m.block.machines.PWRPart.Kind.NEUTRON_SOURCE, BlockProps.copy(Blocks.IRON_BLOCK).strength(5.0F, 6.0F).requiresCorrectToolForDrops().sound(SoundType.STONE)));
    public static final RegistrySupplier<Block> PWR_PORT = registerBlock("pwr_port",
            () -> new com.hbm_m.block.machines.PWRPartBlock(com.hbm_m.block.machines.PWRPart.Kind.PORT, BlockProps.copy(Blocks.IRON_BLOCK).strength(5.0F, 6.0F).requiresCorrectToolForDrops().sound(SoundType.STONE)));
    public static final RegistrySupplier<Block> PWR_REFLECTOR = registerBlock("pwr_reflector",
            () -> new com.hbm_m.block.machines.PWRPartBlock(com.hbm_m.block.machines.PWRPart.Kind.REFLECTOR, BlockProps.copy(Blocks.IRON_BLOCK).strength(5.0F, 6.0F).requiresCorrectToolForDrops().sound(SoundType.STONE)));
    public static final RegistrySupplier<Block> RADIO_TELEX = registerBlock("radio_telex",
            () -> new com.hbm_m.block.network.RadioTelexBlock(BlockBehaviour.Properties.of().mapColor(MapColor.STONE).noOcclusion().strength(3.0F, 6.0F)));
    /** Original refueler (BlockRefueler): Haerte 5, Widerstand 10. */
    public static final RegistrySupplier<Block> REFUELER = registerBlock("refueler",
            () -> new com.hbm_m.block.machines.RefuelerBlock(BlockProps.copy(Blocks.IRON_BLOCK).noOcclusion().strength(5.0F, 6.0F).sound(SoundType.STONE)));
    /** Original piston_inserter (PistonInserter): Haerte 5, Widerstand 10, ohne Creative-Tab und Rezept. */
    public static final RegistrySupplier<Block> PISTON_INSERTER = registerBlock("piston_inserter",
            () -> new com.hbm_m.block.machines.PistonInserterBlock(BlockProps.copy(Blocks.IRON_BLOCK).noOcclusion().strength(5.0F, 6.0F).sound(SoundType.STONE)));
    /** Original machine_deuterium_extractor (MachineDeuteriumExtractor): Haerte 5, Widerstand 10. */
    public static final RegistrySupplier<Block> DEUTERIUM_EXTRACTOR = registerBlock("machine_deuterium_extractor",
            () -> new com.hbm_m.block.machines.DeuteriumExtractorBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(5.0F, 6.0F).sound(SoundType.STONE)));
    public static final RegistrySupplier<Block> FAN = registerBlock("fan",
            () -> new com.hbm_m.block.machines.MachineFanBlock(BlockProps.copy(Blocks.IRON_BLOCK).noOcclusion().strength(5.0F, 6.0F).sound(SoundType.STONE)));
    /** Original lantern_behemoth: Signallaterne (Weltgenerierung), laesst nichts fallen, kein Kreativ-Reiter. */
    public static final RegistrySupplier<Block> LANTERN_BEHEMOTH = registerBlock("lantern_behemoth",
            () -> new com.hbm_m.block.decorations.LanternBehemothBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(3.0F, 3.0F).sound(SoundType.METAL).noOcclusion().noLootTable()));
    public static final RegistrySupplier<Block> CRYSTAL_VIRUS = registerBlock("crystal_virus",
            () -> new com.hbm_m.block.bomb.CrystalVirusBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(15.0F, 15.0F).sound(SoundType.STONE)));
    public static final RegistrySupplier<Block> CRYSTAL_HARDENED = registerBlock("crystal_hardened",
            () -> new Block(BlockProps.copy(Blocks.IRON_BLOCK).strength(15.0F, 15.0F).sound(SoundType.STONE)));
    public static final RegistrySupplier<Block> CRYSTAL_PULSAR = registerBlock("crystal_pulsar",
            () -> new com.hbm_m.block.bomb.CrystalPulsarBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(15.0F, 15.0F).sound(SoundType.STONE)));
    public static final RegistrySupplier<Block> RADIOBOX = registerBlock("radiobox",
            () -> new com.hbm_m.block.machines.RadioboxBlock(BlockProps.copy(Blocks.STONE).noOcclusion().strength(5.0F, 6.0F)));
    public static final RegistrySupplier<Block> RADIOREC = registerBlock("radiorec",
            () -> new com.hbm_m.block.machines.RadioRecBlock(BlockProps.copy(Blocks.STONE).noOcclusion().strength(5.0F, 6.0F)));
    public static final RegistrySupplier<Block> RADIO_AUTOCAL = registerBlock("radio_autocal",
            () -> new com.hbm_m.block.network.RadioAutocalBlock(BlockProps.copy(Blocks.STONE).noOcclusion().strength(3.0F, 6.0F)));
    public static final RegistrySupplier<Block> RAIL_BOOSTER = registerBlock("rail_booster",
            () -> new com.hbm_m.block.machines.RailGenericBlock(BlockProps.copy(Blocks.RAIL).strength(5.0F, 6.0F).sound(SoundType.STONE), 1.0F, false, true));
    public static final RegistrySupplier<Block> RAIL_HIGHSPEED = registerBlock("rail_highspeed",
            () -> new com.hbm_m.block.machines.RailGenericBlock(BlockProps.copy(Blocks.RAIL).strength(5.0F, 6.0F).sound(SoundType.STONE), 1.0F, false, false));
    public static final RegistrySupplier<Block> RAIL_NARROW = registerBlock("rail_narrow",
            () -> new com.hbm_m.block.machines.RailGenericBlock(BlockProps.copy(Blocks.RAIL).strength(5.0F, 6.0F).sound(SoundType.STONE), 0.4F, true, false));
    /** R7d: 1:1 RailGeneric/RailBooster. */
    public static final RegistrySupplier<Block> RAIL_WOOD = registerBlock("rail_wood",
            () -> new com.hbm_m.block.machines.RailGenericBlock(BlockProps.copy(Blocks.RAIL).strength(5.0F, 6.0F).sound(SoundType.STONE), 0.2F, true, false));
    // ─── Zugsystem: Original rail_narrow_* / rail_large_* (IRailNTM, BlockDummyable; Material.iron, 5/10) ───
    public static final RegistrySupplier<Block> RAIL_NARROW_STRAIGHT = registerBlock("rail_narrow_straight",
            () -> new com.hbm_m.block.rail.RailNarrowStraight(railProps().sound(SoundType.STONE)));
    public static final RegistrySupplier<Block> RAIL_NARROW_CURVE = registerBlock("rail_narrow_curve",
            () -> new com.hbm_m.block.rail.RailNarrowCurve(railProps().sound(SoundType.STONE)));
    public static final RegistrySupplier<Block> RAIL_LARGE_STRAIGHT = registerBlock("rail_large_straight",
            () -> new com.hbm_m.block.rail.RailStandardStraight(railProps().sound(SoundType.STONE)));
    public static final RegistrySupplier<Block> RAIL_LARGE_STRAIGHT_SHORT = registerBlock("rail_large_straight_short",
            () -> new com.hbm_m.block.rail.RailStandardStraightShort(railProps().sound(SoundType.STONE)));
    public static final RegistrySupplier<Block> RAIL_LARGE_CURVE = registerBlock("rail_large_curve",
            () -> new com.hbm_m.block.rail.RailStandardCurveBase(railProps().sound(SoundType.STONE)));
    public static final RegistrySupplier<Block> RAIL_LARGE_CURVE_7 = registerBlock("rail_large_curve_7",
            () -> new com.hbm_m.block.rail.RailStandardCurveWide7(railProps().sound(SoundType.STONE)));
    public static final RegistrySupplier<Block> RAIL_LARGE_CURVE_9 = registerBlock("rail_large_curve_9",
            () -> new com.hbm_m.block.rail.RailStandardCurveWide9(railProps().sound(SoundType.STONE)));
    public static final RegistrySupplier<Block> RAIL_LARGE_RAMP = registerBlock("rail_large_ramp",
            () -> new com.hbm_m.block.rail.RailStandardRamp(railProps().sound(SoundType.STONE)));
    public static final RegistrySupplier<Block> RAIL_LARGE_BUFFER = registerBlock("rail_large_buffer",
            () -> new com.hbm_m.block.rail.RailStandardBuffer(railProps().sound(SoundType.STONE)));
    public static final RegistrySupplier<Block> RAIL_LARGE_SWITCH = registerBlock("rail_large_switch",
            () -> new com.hbm_m.block.rail.RailStandardSwitch(railProps().sound(SoundType.STONE)));
    public static final RegistrySupplier<Block> RAIL_LARGE_SWITCH_FLIPPED = registerBlock("rail_large_switch_flipped",
            () -> new com.hbm_m.block.rail.RailStandardSwitchFlipped(railProps().sound(SoundType.STONE)));

    private static BlockBehaviour.Properties railProps() {
        return BlockProps.copy(Blocks.IRON_BLOCK).strength(5.0F, 6.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion();
    }
    /** R7c: Original-Hauptkabel (BlockCable) wie red_cable_classic. */
    public static final RegistrySupplier<Block> RED_CABLE = registerBlock("red_cable",
            () -> new WireBlock(BlockProps.copy(Blocks.IRON_BLOCK).noOcclusion().strength(5.0F, 6.0F).sound(SoundType.STONE)));
    public static final RegistrySupplier<Block> RED_CABLE_CLASSIC = registerBlock("red_cable_classic", () -> new WireBlock(BlockProps.copy(Blocks.IRON_BLOCK).noOcclusion().strength(5.0F, 6.0F).sound(SoundType.STONE)));
    /** Порт BlockCablePaintable (1.7.10): цельноблочный кабель-камуфляж. */
    public static final RegistrySupplier<Block> RED_CABLE_PAINTABLE = registerBlock("red_cable_paintable",
            () -> new RedCablePaintableBlock(BlockProps.copy(Blocks.IRON_BLOCK).noOcclusion().strength(5.0F, 6.0F).sound(SoundType.STONE)));
    /** Порт BlockCableGauge (1.7.10): цельноблочный кабель с датчиком энергии. */
    public static final RegistrySupplier<Block> RED_CABLE_GAUGE = registerBlock("red_cable_gauge",
            () -> new RedCableGaugeBlock(BlockProps.copy(Blocks.IRON_BLOCK).noOcclusion().strength(5.0F, 6.0F).sound(SoundType.STONE)));
    /** R7i: 1:1 PowerCableBox. */
    public static final RegistrySupplier<Block> RED_CABLE_BOX = registerBlock("red_cable_box",
            () -> new com.hbm_m.block.network.BoxDuctBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(5.0F, 6.0F).requiresCorrectToolForDrops().noOcclusion().sound(SoundType.STONE), com.hbm_m.block.network.BoxDuctGeometry.Kind.CABLE));
    /** Порты сети длинной ЛЭП (1.7.10: ConnectorRedWire / ConnectorRedWireSuper / PylonRedWire / PylonMedium / PylonLarge / WireCoated). */
    public static final RegistrySupplier<Block> RED_CONNECTOR = registerBlock("red_connector",
            () -> new RedConnectorBlock(BlockProps.copy(Blocks.IRON_BLOCK).noOcclusion().strength(5.0F, 6.0F).sound(SoundType.STONE),
                    com.hbm_m.blockentity.ModBlockEntities.RED_CONNECTOR_BE, 10));
    public static final RegistrySupplier<Block> RED_CONNECTOR_SUPER = registerBlock("red_connector_super",
            () -> new RedConnectorBlock(BlockProps.copy(Blocks.IRON_BLOCK).noOcclusion().strength(5.0F, 6.0F).sound(SoundType.STONE),
                    com.hbm_m.blockentity.ModBlockEntities.RED_CONNECTOR_SUPER_BE, 100));
    public static final RegistrySupplier<Block> RED_PYLON = registerBlock("red_pylon",
            () -> new RedPylonBlock(BlockProps.copy(Blocks.IRON_BLOCK).noOcclusion().strength(5.0F, 6.0F).sound(SoundType.STONE)));
    public static final RegistrySupplier<Block> RED_PYLON_STEEL = registerBlock("red_pylon_steel",
            () -> new RedPylonBlock(BlockProps.copy(Blocks.IRON_BLOCK).noOcclusion().strength(5.0F, 6.0F).sound(SoundType.STONE)));
    public static final RegistrySupplier<Block> RED_PYLON_LARGE = registerBlock("red_pylon_large",
            () -> new RedPylonLargeBlock(BlockProps.copy(Blocks.IRON_BLOCK).noOcclusion().strength(5.0F, 6.0F).sound(SoundType.STONE)));
    public static final RegistrySupplier<Block> RED_PYLON_MEDIUM_WOOD  = registerBlock("red_pylon_medium_wood",
            () -> new RedPylonMediumBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).noOcclusion().strength(5.0F, 6.0F).sound(SoundType.STONE), false));
    public static final RegistrySupplier<Block> RED_PYLON_MEDIUM_WOOD_TRANSFORMER = registerBlock("red_pylon_medium_wood_transformer",
            () -> new RedPylonMediumBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).noOcclusion().strength(5.0F, 6.0F).sound(SoundType.STONE), true));
    public static final RegistrySupplier<Block> RED_PYLON_MEDIUM_STEEL = registerBlock("red_pylon_medium_steel",
            () -> new RedPylonMediumBlock(BlockProps.copy(Blocks.IRON_BLOCK).noOcclusion().strength(5.0F, 6.0F).sound(SoundType.STONE), false));
    public static final RegistrySupplier<Block> RED_PYLON_MEDIUM_STEEL_TRANSFORMER = registerBlock("red_pylon_medium_steel_transformer",
            () -> new RedPylonMediumBlock(BlockProps.copy(Blocks.IRON_BLOCK).noOcclusion().strength(5.0F, 6.0F).sound(SoundType.STONE), true));
    /** Невидимая часть корпуса пилона (аналог dummy-блоков BlockDummyable): без предмета и лута, как в оригинале. */
    public static final RegistrySupplier<Block> PYLON_DUMMY = registerBlockWithoutItem("pylon_dummy",
            () -> new PylonDummyBlock(BlockProps.copy(Blocks.IRON_BLOCK).noOcclusion().strength(5.0F, 10.0F).noLootTable()));
    public static final RegistrySupplier<Block> RED_WIRE_COATED = registerBlock("red_wire_coated",
            () -> new RedWireCoatedBlock(BlockProps.copy(Blocks.IRON_BLOCK).noOcclusion().strength(5.0F, 6.0F).sound(SoundType.STONE)));
    public static final RegistrySupplier<Block> REINFORCED_BRICK = registerBlock("reinforced_brick",
            () -> new Block(BlockProps.copy(Blocks.STONE).strength(15F, 180.0F).sound(SoundType.STONE).requiresCorrectToolForDrops()));
    /** R6e: 1:1 BlockNoSpawn. */
    public static final RegistrySupplier<Block> REINFORCED_DUCRETE = registerBlock("reinforced_ducrete",
            () -> new com.hbm_m.block.generic.NTMPlants.NoSpawn(BlockProps.copy(Blocks.STONE).strength(20F, 600.0F).sound(SoundType.STONE).requiresCorrectToolForDrops()));
    public static final RegistrySupplier<Block> REINFORCED_GLASS_PANE = registerBlock("reinforced_glass_pane", () -> new net.minecraft.world.level.block.IronBarsBlock(BlockProps.copy(Blocks.STONE).noOcclusion().strength(2.0F, 15.0F).sound(SoundType.STONE)));
    public static final RegistrySupplier<Block> REINFORCED_LAMINATE = registerBlock("reinforced_laminate",
            () -> new com.hbm_m.block.generic.BlockNTMGlass(BlockProps.copy(Blocks.STONE).strength(15.0F, 180.0F).requiresCorrectToolForDrops().sound(SoundType.STONE)));
    /** R6d: 1:1 BlockNTMGlassPane. */
    public static final RegistrySupplier<Block> REINFORCED_LAMINATE_PANE = registerBlock("reinforced_laminate_pane",
            () -> new com.hbm_m.block.generic.R6dBlocks.GlassPane(BlockProps.copy(Blocks.GLASS_PANE).strength(15.0F, 180.0F).requiresCorrectToolForDrops().noOcclusion().sound(SoundType.STONE)));
    public static final RegistrySupplier<Block> REINFORCED_LAMP_OFF = registerBlock("reinforced_lamp_off",
            () -> new com.hbm_m.block.generic.ReinforcedLamp(BlockProps.copy(Blocks.STONE).strength(15.0F, 48.0F).sound(SoundType.STONE).requiresCorrectToolForDrops(), false, () -> ModBlocks.REINFORCED_LAMP_OFF.get(), () -> ModBlocks.REINFORCED_LAMP_ON.get()));
    public static final RegistrySupplier<Block> REINFORCED_LAMP_ON = registerBlock("reinforced_lamp_on",
            () -> new com.hbm_m.block.generic.ReinforcedLamp(BlockProps.copy(Blocks.STONE).strength(15.0F, 48.0F).sound(SoundType.STONE).requiresCorrectToolForDrops(), true, () -> ModBlocks.REINFORCED_LAMP_OFF.get(), () -> ModBlocks.REINFORCED_LAMP_ON.get()));
    public static final RegistrySupplier<Block> REINFORCED_LIGHT = registerBlock("reinforced_light",
            () -> new Block(BlockProps.copy(Blocks.STONE).strength(15.0F, 48.0F).sound(SoundType.STONE).lightLevel(s -> 15).requiresCorrectToolForDrops()));
    public static final RegistrySupplier<Block> REINFORCED_SAND = registerBlock("reinforced_sand",
            () -> new Block(BlockProps.copy(Blocks.STONE).strength(15F, 24.0F).sound(SoundType.STONE).requiresCorrectToolForDrops()));
    /** R6d: 1:1 BlockStorageCrate (safe). */
    public static final RegistrySupplier<Block> SAFE = registerBlockWithoutItem("safe",
            () -> new com.hbm_m.block.machines.crates.SafeBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(7.5F, 6000.0F).sound(SoundType.METAL).requiresCorrectToolForDrops()));
    // sand_mix (orig BlockNTMSand): 1.7.10 metadata-variant falling sand, ported as one block per
    // variant (matching this port's established convention) using vanilla FallingBlock instead of
    // reimplementing the original's fall()/onBlockAdded tick logic (vanilla's is equivalent).
    public static final RegistrySupplier<Block> SAND_BORON = registerBlock("sand_boron", () -> new com.hbm_m.block.generic.BlockHazardFalling(BlockProps.copy(Blocks.SAND)));
    public static final RegistrySupplier<Block> SAND_DIRTY = registerBlock("sand_dirty",
            () -> new net.minecraft.world.level.block.FallingBlock(BlockProps.copy(Blocks.SAND).strength(0.5F, 0.5F).sound(SoundType.SAND)));
    public static final RegistrySupplier<Block> SAND_DIRTY_RED = registerBlock("sand_dirty_red",
            () -> new net.minecraft.world.level.block.FallingBlock(BlockProps.copy(Blocks.SAND).strength(0.5F, 0.5F).sound(SoundType.SAND)));
    public static final RegistrySupplier<Block> SAND_LEAD = registerBlock("sand_lead", () -> new com.hbm_m.block.generic.BlockHazardFalling(BlockProps.copy(Blocks.SAND)));
    public static final RegistrySupplier<Block> SAND_POLONIUM = registerBlock("sand_polonium", () -> new com.hbm_m.block.generic.BlockHazardFalling(BlockProps.copy(Blocks.SAND)));
    public static final RegistrySupplier<Block> SAND_QUARTZ = registerBlock("sand_quartz", () -> new com.hbm_m.block.generic.BlockHazardFalling(BlockProps.copy(Blocks.SAND)));
    public static final RegistrySupplier<Block> SAND_URANIUM = registerBlock("sand_uranium", () -> new com.hbm_m.block.generic.BlockHazardFalling(BlockProps.copy(Blocks.SAND)));
    /** R6d: 1:1 BlockSandbags. */
    public static final RegistrySupplier<Block> SANDBAGS = registerBlock("sandbags",
            () -> new com.hbm_m.block.generic.R6dBlocks.Sandbags(BlockProps.copy(Blocks.DIRT).strength(5.0F, 18.0F).sound(SoundType.STONE).noOcclusion()));
    public static final RegistrySupplier<Block> SAT_DOCK = registerBlock("sat_dock", () -> new com.hbm_m.block.machines.MachineSatDockBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(5.0F, 6.0F).requiresCorrectToolForDrops().noOcclusion().sound(SoundType.STONE)));
    public static final RegistrySupplier<Block> SAT_FOEQ = registerBlock("sat_foeq_deco", () -> new Block(BlockProps.copy(Blocks.STONE)));
    public static final RegistrySupplier<Block> SAT_SCANNER = registerBlock("sat_scanner_deco", () -> new Block(BlockProps.copy(Blocks.STONE)));
    /** R7o: 1:1 BlockSeal. */
    public static final RegistrySupplier<Block> SEAL_CONTROLLER = registerBlock("seal_controller",
            () -> new com.hbm_m.block.machines.SealBlocks.Controller(BlockProps.copy(Blocks.IRON_BLOCK).strength(10.0F, 60.0F).requiresCorrectToolForDrops().sound(SoundType.STONE)));
    public static final RegistrySupplier<Block> SEAL_FRAME = registerBlock("seal_frame",
            () -> new Block(BlockProps.copy(Blocks.IRON_BLOCK).strength(10.0F, 60.0F).requiresCorrectToolForDrops().sound(SoundType.STONE)));
    /** R7o: 1:1 BlockHatch. */
    public static final RegistrySupplier<Block> SEAL_HATCH = registerBlock("seal_hatch",
            () -> new com.hbm_m.block.machines.SealBlocks.Hatch(BlockProps.copy(Blocks.BEDROCK).requiresCorrectToolForDrops().noLootTable().strength(0.0F, 0.0F).sound(SoundType.STONE)));
    /** R6c: 1:1 BlockSemtex (BlockTNTBase). */
    public static final RegistrySupplier<Block> SEMTEX = registerBlock("semtex",
            () -> new com.hbm_m.block.bomb.TNTBlocks.Semtex(BlockProps.copy(Blocks.TNT).instabreak().sound(SoundType.GRASS)));
    /** R6c: 1:1 BlockTNT ("Actual TNT"). */
    public static final RegistrySupplier<Block> TNT_NTM = registerBlock("tnt_ntm",
            () -> new com.hbm_m.block.bomb.TNTBlocks.TNT(BlockProps.copy(Blocks.TNT).instabreak().sound(SoundType.GRASS)));
    /** Original soyuz_capsule (SoyuzCapsule): Eisen, Haerte 5, Widerstand 10. */
    public static final RegistrySupplier<Block> SOYUZ_CAPSULE = registerBlock("soyuz_capsule",
            () -> new com.hbm_m.block.machines.SoyuzCapsuleBlock(BlockProps.copy(Blocks.IRON_BLOCK).noOcclusion().strength(5.0F, 6.0F).sound(SoundType.STONE)));
    /** Dekorative Soyuz-Startrampe (6 OBJ-Teile, siehe models/block/soyuz_launcher.json) - platzierbar, ohne Spiellogik.
     *  Rendert ueber BlockEntityRenderer (SoyuzLauncherRenderer), da die Tuerme ueber 60 Bloecke hoch sind
     *  und damit die 16-Bit-Chunk-Mesh-Grenze eines normalen Block-Modells sprengen wuerden. */
    public static final RegistrySupplier<Block> SOYUZ_LAUNCHER = registerBlockWithoutItem("soyuz_launcher",
            () -> new com.hbm_m.block.decorations.SoyuzLauncherBlock(BlockProps.copy(Blocks.IRON_BLOCK).noOcclusion().noLootTable().isSuffocating((st, w, p) -> false).strength(5.0F, 6.0F).sound(SoundType.STONE)));
    /** Dekorative Soyuz-Rakete (soyuz.obj, Multi-Material) - platzierbar, ohne Spiellogik.
     *  Rendert ueber BlockEntityRenderer (SoyuzRocketRenderer), da das Modell ueber 50 Bloecke hoch ist
     *  und damit die 16-Bit-Chunk-Mesh-Grenze eines normalen Block-Modells sprengen wuerde. */
    public static final RegistrySupplier<Block> DECO_SOYUZ_ROCKET = registerBlock("deco_soyuz_rocket",
            () -> new com.hbm_m.block.decorations.SoyuzRocketBlock(BlockProps.copy(Blocks.STONE).noOcclusion()));
    /** R6d: 1:1 Spikes. */
    public static final RegistrySupplier<Block> SPIKES = registerBlock("spikes",
            () -> new com.hbm_m.block.generic.R6dBlocks.Spikes(BlockProps.copy(Blocks.IRON_BLOCK).strength(2.5F, 3.0F).sound(SoundType.STONE).requiresCorrectToolForDrops().noOcclusion().noCollission()));
    /** R6e: 1:1 BlockStalagmite. */
    public static final RegistrySupplier<Block> STALACTITE_ASBESTOS = registerBlock("stalactite_asbestos",
            () -> new com.hbm_m.block.generic.NTMPlants.Stalagmite(BlockProps.copy(Blocks.STONE).strength(0.5F, 1.2F).sound(SoundType.STONE).noCollission().noOcclusion().requiresCorrectToolForDrops(), com.hbm_m.block.generic.NTMPlants.StalagmiteType.ASBESTOS, true));
    /** R6e: 1:1 BlockStalagmite. */
    public static final RegistrySupplier<Block> STALACTITE_SULFUR = registerBlock("stalactite_sulfur",
            () -> new com.hbm_m.block.generic.NTMPlants.Stalagmite(BlockProps.copy(Blocks.STONE).strength(0.5F, 1.2F).sound(SoundType.STONE).noCollission().noOcclusion().requiresCorrectToolForDrops(), com.hbm_m.block.generic.NTMPlants.StalagmiteType.SULFUR, true));
    /** R6e: 1:1 BlockStalagmite. */
    public static final RegistrySupplier<Block> STALAGMITE_ASBESTOS = registerBlock("stalagmite_asbestos",
            () -> new com.hbm_m.block.generic.NTMPlants.Stalagmite(BlockProps.copy(Blocks.STONE).strength(0.5F, 1.2F).sound(SoundType.STONE).noCollission().noOcclusion().requiresCorrectToolForDrops(), com.hbm_m.block.generic.NTMPlants.StalagmiteType.ASBESTOS, false));
    /** R6e: 1:1 BlockStalagmite. */
    public static final RegistrySupplier<Block> STALAGMITE_SULFUR = registerBlock("stalagmite_sulfur",
            () -> new com.hbm_m.block.generic.NTMPlants.Stalagmite(BlockProps.copy(Blocks.STONE).strength(0.5F, 1.2F).sound(SoundType.STONE).noCollission().noOcclusion().requiresCorrectToolForDrops(), com.hbm_m.block.generic.NTMPlants.StalagmiteType.SULFUR, false));
    /** R6b: 1:1 DecoBlock steel_roof (1px Platte). */
    public static final RegistrySupplier<Block> STEEL_ROOF = registerBlock("steel_roof", () -> new DecorShapeBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(5F, 9.0F).sound(SoundType.STONE).requiresCorrectToolForDrops().noOcclusion(),
            Shapes.box(0, 0, 0, 1, 0.0625, 1), false, false));
    /** R6b: 1:1 BlockScaffold (Farben grau/rot/weiss/gelb als eigene Bloecke). */
    public static final RegistrySupplier<Block> STEEL_SCAFFOLD = registerBlock("steel_scaffold",
            () -> new com.hbm_m.block.decorations.SteelScaffoldBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(5F, 9.0F).sound(SoundType.STONE).requiresCorrectToolForDrops().noOcclusion()));
    public static final RegistrySupplier<Block> STEEL_SCAFFOLD_RED = registerBlock("steel_scaffold_red",
            () -> new com.hbm_m.block.decorations.SteelScaffoldBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(5F, 9.0F).sound(SoundType.STONE).requiresCorrectToolForDrops().noOcclusion()));
    public static final RegistrySupplier<Block> STEEL_SCAFFOLD_WHITE = registerBlock("steel_scaffold_white",
            () -> new com.hbm_m.block.decorations.SteelScaffoldBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(5F, 9.0F).sound(SoundType.STONE).requiresCorrectToolForDrops().noOcclusion()));
    public static final RegistrySupplier<Block> STEEL_SCAFFOLD_YELLOW = registerBlock("steel_scaffold_yellow",
            () -> new com.hbm_m.block.decorations.SteelScaffoldBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(5F, 9.0F).sound(SoundType.STONE).requiresCorrectToolForDrops().noOcclusion()));
    public static final RegistrySupplier<Block> STONE_CRACKED = registerBlock("stone_cracked",
            () -> new net.minecraft.world.level.block.FallingBlock(BlockProps.copy(Blocks.STONE).strength(5.0F, 5.0F).sound(SoundType.STONE).requiresCorrectToolForDrops()));
    public static final RegistrySupplier<Block> STONE_DEPTH = registerBlock("stone_depth",
            () -> new com.hbm_m.block.nature.DepthOreBlock(BlockProps.copy(Blocks.STONE).sound(SoundType.STONE).strength(-1.0F, 6.0F)));
    public static final RegistrySupplier<Block> STONE_DEPTH_NETHER = registerBlock("stone_depth_nether",
            () -> new com.hbm_m.block.nature.DepthOreBlock(BlockProps.copy(Blocks.STONE).sound(SoundType.STONE).strength(-1.0F, 6.0F)));
    public static final RegistrySupplier<Block> STONE_GNEISS = registerBlock("stone_gneiss",
            () -> new Block(BlockProps.copy(Blocks.STONE).strength(1.5F, 6.0F).sound(SoundType.STONE).requiresCorrectToolForDrops()));
    /** Порт {@code BlockKeyhole} (1.7.10) — скважина-ключ, маскируется под камень, ведёт в красную комнату. */
    public static final RegistrySupplier<Block> STONE_KEYHOLE = registerBlock("stone_keyhole",
            () -> new KeyholeBlock(BlockProps.copy(Blocks.STONE).strength(0.0F, 0.0F), false));
    /** Порт {@code BlockRedBrickKeyhole} (1.7.10) — скважина-ключ в красном кирпиче, ведёт в чёрную комнату. */
    public static final RegistrySupplier<Block> STONE_KEYHOLE_META = registerBlock("stone_keyhole_meta",
            () -> new KeyholeBlock(BlockProps.copy(Blocks.STONE).strength(0.0F, 6000.0F), true));
    /** R6d: 1:1 BlockPorous. */
    public static final RegistrySupplier<Block> STONE_POROUS = registerBlock("stone_porous",
            () -> new com.hbm_m.block.generic.R6dBlocks.Porous(BlockProps.copy(Blocks.STONE).strength(1.5F, 18.0F).requiresCorrectToolForDrops()));
    /** R6e: 1:1 BlockResourceStone. */
    public static final RegistrySupplier<Block> STONE_RESOURCE_ASBESTOS = registerBlock("stone_resource_asbestos",
            () -> new com.hbm_m.block.generic.NTMPlants.ResourceStone(BlockProps.copy(Blocks.STONE).strength(5.0F, 6.0F).sound(SoundType.STONE).requiresCorrectToolForDrops(), com.hbm_m.block.generic.NTMPlants.StoneType.ASBESTOS));
    /** R6e: 1:1 BlockResourceStone. */
    public static final RegistrySupplier<Block> STONE_RESOURCE_BAUXITE = registerBlock("stone_resource_bauxite",
            () -> new com.hbm_m.block.generic.NTMPlants.ResourceStone(BlockProps.copy(Blocks.STONE).strength(5.0F, 6.0F).sound(SoundType.STONE).requiresCorrectToolForDrops(), com.hbm_m.block.generic.NTMPlants.StoneType.BAUXITE));
    /** R6e: 1:1 BlockResourceStone. */
    public static final RegistrySupplier<Block> STONE_RESOURCE_HEMATITE = registerBlock("stone_resource_hematite",
            () -> new com.hbm_m.block.generic.NTMPlants.ResourceStone(BlockProps.copy(Blocks.STONE).strength(5.0F, 6.0F).sound(SoundType.STONE).requiresCorrectToolForDrops(), com.hbm_m.block.generic.NTMPlants.StoneType.HEMATITE));
    /** R6e: 1:1 BlockResourceStone. */
    public static final RegistrySupplier<Block> STONE_RESOURCE_LIMESTONE = registerBlock("stone_resource_limestone",
            () -> new com.hbm_m.block.generic.NTMPlants.ResourceStone(BlockProps.copy(Blocks.STONE).strength(5.0F, 6.0F).sound(SoundType.STONE).requiresCorrectToolForDrops(), com.hbm_m.block.generic.NTMPlants.StoneType.LIMESTONE));
    /** R6e: 1:1 BlockResourceStone. */
    public static final RegistrySupplier<Block> STONE_RESOURCE_MALACHITE = registerBlock("stone_resource_malachite",
            () -> new com.hbm_m.block.generic.NTMPlants.ResourceStone(BlockProps.copy(Blocks.STONE).strength(5.0F, 6.0F).sound(SoundType.STONE).requiresCorrectToolForDrops(), com.hbm_m.block.generic.NTMPlants.StoneType.MALACHITE));
    /** R6e: 1:1 BlockResourceStone. */
    public static final RegistrySupplier<Block> STONE_RESOURCE_SULFUR = registerBlock("stone_resource_sulfur",
            () -> new com.hbm_m.block.generic.NTMPlants.ResourceStone(BlockProps.copy(Blocks.STONE).strength(5.0F, 6.0F).sound(SoundType.STONE).requiresCorrectToolForDrops(), com.hbm_m.block.generic.NTMPlants.StoneType.SULFUR));
    public static final RegistrySupplier<Block> STRUCT_ICF_CORE = registerBlock("struct_icf_core",
            () -> new com.hbm_m.block.machines.icf.ICFStructBlock(BlockProps.copy(Blocks.IRON_BLOCK).lightLevel(s -> 15).strength(5.0F, 6.0F).sound(SoundType.STONE)));
    public static final RegistrySupplier<Block> STRUCT_LAUNCHER = registerBlock("struct_launcher",
            () -> new Block(BlockProps.copy(Blocks.IRON_BLOCK).strength(5F, 6.0F).requiresCorrectToolForDrops().sound(SoundType.STONE)));
    /** 1:1 BlockStruct + TileEntityMultiblock: Bauplankern Kompaktwerfer / Starttisch. */
    public static final RegistrySupplier<Block> STRUCT_LAUNCHER_CORE = registerBlock("struct_launcher_core",
            () -> new com.hbm_m.block.machines.StructLauncherCoreBlock(BlockProps.copy(Blocks.IRON_BLOCK).noOcclusion().strength(5.0F, 6.0F).sound(SoundType.STONE), false));
    public static final RegistrySupplier<Block> STRUCT_LAUNCHER_CORE_LARGE = registerBlock("struct_launcher_core_large",
            () -> new com.hbm_m.block.machines.StructLauncherCoreBlock(BlockProps.copy(Blocks.IRON_BLOCK).noOcclusion().strength(5.0F, 6.0F).sound(SoundType.STONE), true));
    public static final RegistrySupplier<Block> STRUCT_SCAFFOLD = registerBlock("struct_scaffold",
            () -> new Block(BlockProps.copy(Blocks.IRON_BLOCK).strength(5F, 6.0F).requiresCorrectToolForDrops().sound(SoundType.STONE)));
    /** 1:1 BlockSoyuzStruct: errichtet die Startrampe, sobald der Bauplan steht. */
    public static final RegistrySupplier<Block> STRUCT_SOYUZ_CORE = registerBlock("struct_soyuz_core",
            () -> new com.hbm_m.block.machines.SoyuzStructBlock(BlockProps.copy(Blocks.IRON_BLOCK).noOcclusion().strength(5.0F, 6.0F).sound(SoundType.STONE)));
    /** 1:1-Port von {@code BlockFusionTorusStruct} (1.7.10): wird zum Torus, sobald die Form steht. */
    public static final RegistrySupplier<Block> STRUCT_TORUS_CORE = registerBlock("struct_torus_core",
            () -> new StructTorusCoreBlock(BlockProps.copy(Blocks.IRON_BLOCK).lightLevel(state -> 15).noOcclusion().strength(5.0F, 6.0F).sound(SoundType.STONE)));
    /** R7p: 1:1 BlockWatzStruct. */
    public static final RegistrySupplier<Block> STRUCT_WATZ_CORE = registerBlock("struct_watz_core",
            () -> new com.hbm_m.block.machines.WatzBlocks.StructCore(BlockProps.copy(Blocks.IRON_BLOCK).strength(5.0F, 6.0F).lightLevel(s -> 15).noOcclusion().sound(SoundType.STONE)));
    /** Decorative casing end-cap; original toggled bolted/unbolted via screwdriver, ported here as two plain block variants. */
    public static final RegistrySupplier<Block> WATZ_END = registerBlock("watz_end", () -> new com.hbm_m.block.generic.WatzEndBlock(BlockProps.copy(Blocks.STONE).strength(5.0F, 6.0F)));
    public static final RegistrySupplier<Block> WATZ_END_BOLTED = registerBlock("watz_end_bolted", () -> new Block(BlockProps.copy(Blocks.STONE)));
    public static final RegistrySupplier<Block> TEKTITE = registerBlock("tektite",
            () -> new Block(BlockProps.copy(Blocks.SAND).sound(SoundType.SAND).strength(0.5F, 0.5F)));
    // Original YellowBarrel (yellow_barrel): siehe BARREL_YELLOW (BarrelYellowBlock), die doppelte Variante entfiel.

    /** Original: {@code Charger} - laedt die Ausruestung der darauf stehenden Spieler. */
    public static final RegistrySupplier<Block> CHARGER = registerBlock("charger",
            () -> new com.hbm_m.block.machines.ChargerBlock(
                    BlockProps.copy(Blocks.IRON_BLOCK).requiresCorrectToolForDrops().noOcclusion().strength(5.0F, 6.0F).sound(SoundType.STONE)));

    /** Original: {@code MachineTesla} - setzt alles Lebende im Umkreis unter Strom. */
    public static final RegistrySupplier<Block> TESLA = registerBlock("tesla",
            () -> new com.hbm_m.block.machines.TeslaBlock(
                    BlockProps.copy(Blocks.IRON_BLOCK).requiresCorrectToolForDrops().noOcclusion().strength(5.0F, 6.0F).sound(SoundType.STONE)));
    /** R6c: 1:1 BombThermo / BombFloat. */
    public static final RegistrySupplier<Block> THERM_ENDO = registerBlock("therm_endo",
            () -> new com.hbm_m.block.bomb.SimpleBombs.Thermo(BlockProps.copy(Blocks.IRON_BLOCK).strength(5.0F, 120.0F).sound(SoundType.STONE).requiresCorrectToolForDrops(), false));
    public static final RegistrySupplier<Block> THERM_EXO = registerBlock("therm_exo",
            () -> new com.hbm_m.block.bomb.SimpleBombs.Thermo(BlockProps.copy(Blocks.IRON_BLOCK).strength(5.0F, 120.0F).sound(SoundType.STONE).requiresCorrectToolForDrops(), true));
    public static final RegistrySupplier<Block> FLOAT_BOMB = registerBlock("float_bomb",
            () -> new com.hbm_m.block.bomb.SimpleBombs.Float(BlockProps.copy(Blocks.IRON_BLOCK).strength(5.0F, 120.0F).sound(SoundType.STONE).requiresCorrectToolForDrops(), false));
    public static final RegistrySupplier<Block> EMP_BOMB = registerBlock("emp_bomb",
            () -> new com.hbm_m.block.bomb.SimpleBombs.Float(BlockProps.copy(Blocks.IRON_BLOCK).strength(5.0F, 120.0F).sound(SoundType.STONE).requiresCorrectToolForDrops(), true));
    /** Original: {@code tile_lab} ist ein {@code BlockOutgas} (Asbest, Glasklang, Haerte 1/20). */
    public static final RegistrySupplier<Block> TILE_LAB = registerBlock("tile_lab",
            () -> new com.hbm_m.block.gas.OutgasBlock(BlockProps.copy(Blocks.GLASS).requiresCorrectToolForDrops().strength(1.0F, 12.0F),
                    () -> ModBlocks.GAS_ASBESTOS.get(), false, true, false, false, true));
    /** Original: {@code tile_lab_broken} ist ein {@code BlockOutgas} (Asbest, Glasklang, Haerte 1/20). */
    public static final RegistrySupplier<Block> TILE_LAB_BROKEN = registerBlock("tile_lab_broken",
            () -> new com.hbm_m.block.gas.OutgasBlock(BlockProps.copy(Blocks.GLASS).requiresCorrectToolForDrops().strength(1.0F, 12.0F),
                    () -> ModBlocks.GAS_ASBESTOS.get(), true, true, false, false, true));
    /** Original: {@code tile_lab_cracked} ist ein {@code BlockOutgas} (Asbest, Glasklang, Haerte 1/20). */
    public static final RegistrySupplier<Block> TILE_LAB_CRACKED = registerBlock("tile_lab_cracked",
            () -> new com.hbm_m.block.gas.OutgasBlock(BlockProps.copy(Blocks.GLASS).requiresCorrectToolForDrops().strength(1.0F, 12.0F),
                    () -> ModBlocks.GAS_ASBESTOS.get(), false, true, false, false, true));
    /** R6d: 1:1 BlockNTMTrapdoor. */
    public static final RegistrySupplier<Block> TRAPDOOR_STEEL = registerBlock("trapdoor_steel",
            () -> new com.hbm_m.block.generic.R6dBlocks.SteelTrapdoor(BlockProps.copy(Blocks.IRON_TRAPDOOR).strength(3.0F, 4.8F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion()));
    public static final RegistrySupplier<Block> VACUUM = registerBlock("vacuum", () -> new Block(BlockProps.copy(Blocks.STONE)));
    /** R6d: 1:1 BlockVent. */
    public static final RegistrySupplier<Block> VENT_CHLORINE = registerBlock("vent_chlorine",
            () -> new com.hbm_m.block.generic.VentBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(5.0F, 6.0F).sound(SoundType.STONE).requiresCorrectToolForDrops()));
    /** Original: {@code BlockClorineSeal} - fuellt unter Redstonestrom den Raum mit Chlorgas. */
    public static final RegistrySupplier<Block> VENT_CHLORINE_SEAL = registerBlock("vent_chlorine_seal", com.hbm_m.block.machines.ChlorineSealBlock::new);
    public static final RegistrySupplier<Block> VENT_CLOUD = registerBlock("vent_cloud",
            () -> new com.hbm_m.block.generic.VentBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(5.0F, 6.0F).sound(SoundType.STONE).requiresCorrectToolForDrops()));
    public static final RegistrySupplier<Block> VENT_PINK_CLOUD = registerBlock("vent_pink_cloud",
            () -> new com.hbm_m.block.generic.VentBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(5.0F, 6.0F).sound(SoundType.STONE).requiresCorrectToolForDrops()));
    /** R6d: 1:1 BlockHangingVine. */
    public static final RegistrySupplier<Block> VINE_PHOSPHOR = registerBlock("vine_phosphor",
            () -> new com.hbm_m.block.generic.R6dPlants.HangingVine(BlockProps.copy(Blocks.VINE).strength(0.5F).sound(SoundType.GRASS).noOcclusion().noCollission()));
    /** R6c: 1:1 BlockVolcano. */
    public static final RegistrySupplier<Block> VOLCANO_CORE = registerBlock("volcano_core",
            () -> new com.hbm_m.block.bomb.VolcanoBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(-1.0F, 6000.0F).sound(SoundType.STONE), false));
    public static final RegistrySupplier<Block> VOLCANO_RAD_CORE = registerBlock("volcano_rad_core",
            () -> new com.hbm_m.block.bomb.VolcanoBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(-1.0F, 6000.0F).sound(SoundType.STONE), true));
    /** 1:1 BlockWand (wand_air): exportiert als Luft. */
    public static final RegistrySupplier<Block> WAND_AIR = registerBlock("wand_air",
            () -> new com.hbm_m.block.generic.BlockWand(BlockProps.copy(Blocks.GLASS).strength(0.0F).noOcclusion().noCollission().sound(SoundType.STONE), () -> Blocks.AIR));
    /** 1:1 BlockWandJigsaw. */
    public static final RegistrySupplier<Block> WAND_JIGSAW = registerBlock("wand_jigsaw",
            () -> new com.hbm_m.block.generic.BlockWandJigsaw(BlockProps.copy(Blocks.IRON_BLOCK).strength(0.0F).sound(SoundType.STONE)));
    /** 1:1 BlockWandLogic. */
    public static final RegistrySupplier<Block> WAND_LOGIC = registerBlock("wand_logic",
            () -> new com.hbm_m.block.generic.BlockWandLogic(BlockProps.copy(Blocks.IRON_BLOCK).strength(0.0F).sound(SoundType.STONE)));
    /** 1:1 BlockWandStructure, Meta 0 (Speichern) und Meta 1 (Laden). */
    public static final RegistrySupplier<Block> WAND_STRUCTURE_SAVE = registerBlock("wand_structure_save",
            () -> new com.hbm_m.block.generic.BlockWandStructure(BlockProps.copy(Blocks.IRON_BLOCK).strength(0.0F).sound(SoundType.METAL), false));
    public static final RegistrySupplier<Block> WAND_STRUCTURE_LOAD = registerBlock("wand_structure_load",
            () -> new com.hbm_m.block.generic.BlockWandStructure(BlockProps.copy(Blocks.IRON_BLOCK).strength(0.0F).sound(SoundType.METAL), true));
    /** 1:1 BlockWandLoot. */
    public static final RegistrySupplier<Block> WAND_LOOT = registerBlock("wand_loot",
            () -> new com.hbm_m.block.generic.BlockWandLoot(BlockProps.copy(Blocks.IRON_BLOCK).strength(0.0F).sound(SoundType.STONE)));
    public static final RegistrySupplier<Block> WASTE_EARTH = registerBlock("waste_earth",
            () -> new com.hbm_m.block.generic.WasteEarth(BlockProps.copy(Blocks.DIRT).strength(0.6F, 0.6F).sound(SoundType.GRASS), true));
    public static final RegistrySupplier<Block> WATZ_COOLER = registerBlock("watz_cooler",
            () -> new Block(BlockProps.copy(Blocks.IRON_BLOCK).strength(5.0F, 6.0F).requiresCorrectToolForDrops().sound(SoundType.STONE)));
    public static final RegistrySupplier<Block> WATZ_ELEMENT = registerBlock("watz_element",
            () -> new Block(BlockProps.copy(Blocks.IRON_BLOCK).strength(5.0F, 6.0F).requiresCorrectToolForDrops().sound(SoundType.STONE)));
    /** R6e: 1:1 BlockTallPlant. */
    public static final RegistrySupplier<Block> PLANT_TALL_WEED = registerBlock("plant_tall_weed",
            () -> new com.hbm_m.block.generic.NTMPlants.TallPlant(BlockProps.copy(Blocks.POPPY).strength(0.0F).sound(SoundType.GRASS).noCollission().instabreak().noOcclusion(), com.hbm_m.block.generic.NTMPlants.TallType.WEED));
    /** R6e: 1:1 BlockTallPlant. */
    public static final RegistrySupplier<Block> PLANT_TALL_CD2 = registerBlock("plant_tall_cd2",
            () -> new com.hbm_m.block.generic.NTMPlants.TallPlant(BlockProps.copy(Blocks.POPPY).strength(0.0F).sound(SoundType.GRASS).noCollission().instabreak().noOcclusion(), com.hbm_m.block.generic.NTMPlants.TallType.CD2));
    /** R6e: 1:1 BlockTallPlant. */
    public static final RegistrySupplier<Block> PLANT_TALL_CD3 = registerBlock("plant_tall_cd3",
            () -> new com.hbm_m.block.generic.NTMPlants.TallPlant(BlockProps.copy(Blocks.POPPY).strength(0.0F).sound(SoundType.GRASS).noCollission().instabreak().noOcclusion(), com.hbm_m.block.generic.NTMPlants.TallType.CD3));
    /** R6e: 1:1 BlockTallPlant. */
    public static final RegistrySupplier<Block> PLANT_TALL_CD4 = registerBlock("plant_tall_cd4",
            () -> new com.hbm_m.block.generic.NTMPlants.TallPlant(BlockProps.copy(Blocks.POPPY).strength(0.0F).sound(SoundType.GRASS).noCollission().instabreak().noOcclusion(), com.hbm_m.block.generic.NTMPlants.TallType.CD4));
    /** R6e: 1:1 BlockReeds (Saeule bis zum Grund: ReedsBakedModel). */
    public static final RegistrySupplier<Block> PLANT_REEDS = registerBlock("plant_reeds",
            () -> new com.hbm_m.block.generic.NTMPlants.Reeds(BlockProps.copy(Blocks.POPPY).strength(0.0F).sound(SoundType.GRASS).noCollission().instabreak().noOcclusion()));
    /** R6e: 1:1 BlockStalagmite. */
    public static final RegistrySupplier<Block> STALAGMITE_ICE = registerBlock("stalagmite_ice",
            () -> new com.hbm_m.block.generic.NTMPlants.Stalagmite(BlockProps.copy(Blocks.STONE).strength(0.5F, 1.2F).sound(SoundType.STONE).noCollission().noOcclusion().requiresCorrectToolForDrops(), com.hbm_m.block.generic.NTMPlants.StalagmiteType.ICE, false));
    /** R6e: 1:1 BlockStalagmite. */
    public static final RegistrySupplier<Block> STALAGMITE_SNOW = registerBlock("stalagmite_snow",
            () -> new com.hbm_m.block.generic.NTMPlants.Stalagmite(BlockProps.copy(Blocks.STONE).strength(0.5F, 1.2F).sound(SoundType.STONE).noCollission().noOcclusion().requiresCorrectToolForDrops(), com.hbm_m.block.generic.NTMPlants.StalagmiteType.SNOW, false));
    /** R6e: 1:1 BlockStalagmite. */
    public static final RegistrySupplier<Block> STALAGMITE_GLYPHID1 = registerBlock("stalagmite_glyphid1",
            () -> new com.hbm_m.block.generic.NTMPlants.Stalagmite(BlockProps.copy(Blocks.STONE).strength(0.5F, 1.2F).sound(SoundType.STONE).noCollission().noOcclusion().requiresCorrectToolForDrops(), com.hbm_m.block.generic.NTMPlants.StalagmiteType.GLYPHID1, false));
    /** R6e: 1:1 BlockStalagmite. */
    public static final RegistrySupplier<Block> STALAGMITE_GLYPHID2 = registerBlock("stalagmite_glyphid2",
            () -> new com.hbm_m.block.generic.NTMPlants.Stalagmite(BlockProps.copy(Blocks.STONE).strength(0.5F, 1.2F).sound(SoundType.STONE).noCollission().noOcclusion().requiresCorrectToolForDrops(), com.hbm_m.block.generic.NTMPlants.StalagmiteType.GLYPHID2, false));
    /** R6e: 1:1 BlockStalagmite. */
    public static final RegistrySupplier<Block> STALAGMITE_GLYPHID3 = registerBlock("stalagmite_glyphid3",
            () -> new com.hbm_m.block.generic.NTMPlants.Stalagmite(BlockProps.copy(Blocks.STONE).strength(0.5F, 1.2F).sound(SoundType.STONE).noCollission().noOcclusion().requiresCorrectToolForDrops(), com.hbm_m.block.generic.NTMPlants.StalagmiteType.GLYPHID3, false));
    /** R6e: 1:1 BlockStalagmite. */
    public static final RegistrySupplier<Block> STALACTITE_ICE = registerBlock("stalactite_ice",
            () -> new com.hbm_m.block.generic.NTMPlants.Stalagmite(BlockProps.copy(Blocks.STONE).strength(0.5F, 1.2F).sound(SoundType.STONE).noCollission().noOcclusion().requiresCorrectToolForDrops(), com.hbm_m.block.generic.NTMPlants.StalagmiteType.ICE, true));
    /** R6e: 1:1 BlockStalagmite. */
    public static final RegistrySupplier<Block> STALACTITE_SNOW = registerBlock("stalactite_snow",
            () -> new com.hbm_m.block.generic.NTMPlants.Stalagmite(BlockProps.copy(Blocks.STONE).strength(0.5F, 1.2F).sound(SoundType.STONE).noCollission().noOcclusion().requiresCorrectToolForDrops(), com.hbm_m.block.generic.NTMPlants.StalagmiteType.SNOW, true));
    /** R6e: 1:1 BlockStalagmite. */
    public static final RegistrySupplier<Block> STALACTITE_GLYPHID1 = registerBlock("stalactite_glyphid1",
            () -> new com.hbm_m.block.generic.NTMPlants.Stalagmite(BlockProps.copy(Blocks.STONE).strength(0.5F, 1.2F).sound(SoundType.STONE).noCollission().noOcclusion().requiresCorrectToolForDrops(), com.hbm_m.block.generic.NTMPlants.StalagmiteType.GLYPHID1, true));
    /** R6e: 1:1 BlockStalagmite. */
    public static final RegistrySupplier<Block> STALACTITE_GLYPHID2 = registerBlock("stalactite_glyphid2",
            () -> new com.hbm_m.block.generic.NTMPlants.Stalagmite(BlockProps.copy(Blocks.STONE).strength(0.5F, 1.2F).sound(SoundType.STONE).noCollission().noOcclusion().requiresCorrectToolForDrops(), com.hbm_m.block.generic.NTMPlants.StalagmiteType.GLYPHID2, true));
    /** R6e: 1:1 BlockStalagmite. */
    public static final RegistrySupplier<Block> STALACTITE_GLYPHID3 = registerBlock("stalactite_glyphid3",
            () -> new com.hbm_m.block.generic.NTMPlants.Stalagmite(BlockProps.copy(Blocks.STONE).strength(0.5F, 1.2F).sound(SoundType.STONE).noCollission().noOcclusion().requiresCorrectToolForDrops(), com.hbm_m.block.generic.NTMPlants.StalagmiteType.GLYPHID3, true));
    /** R6d: 1:1 BlockBarrier. */
    public static final RegistrySupplier<Block> WOOD_BARRIER = registerBlock("wood_barrier",
            () -> new com.hbm_m.block.generic.R6dBlocks.Barrier(BlockProps.copy(Blocks.OAK_PLANKS).strength(5.0F, 9.0F).sound(SoundType.WOOD).noOcclusion()));

    // --- WIP Machines (3D OBJ models) ---
    public static final RegistrySupplier<Block> AMMO_PRESS = registerBlock("ammo_press",
            () -> new com.hbm_m.block.machines.MachineAmmoPressBlock(BlockProps.copy(Blocks.IRON_BLOCK).noOcclusion().strength(5.0F, 6.0F).sound(SoundType.STONE)));

    public static final RegistrySupplier<Block> ANNIHILATOR = registerBlock("annihilator",
            () -> new com.hbm_m.block.machines.MachineAnnihilatorBlock(BlockProps.copy(Blocks.IRON_BLOCK).noOcclusion().strength(5.0F, 6.0F).sound(SoundType.STONE)));
    public static final RegistrySupplier<Block> ARC_FURNACE = registerBlock("arc_furnace",
            () -> new com.hbm_m.block.machines.MachineArcFurnaceBlock(BlockProps.copy(Blocks.IRON_BLOCK).noOcclusion().strength(5.0F, 6.0F).sound(SoundType.STONE)));

    /** 1:1 {@code MachineAssemblyFactory} (Haerte 5, Widerstand 30), Item: {@code ModItems.ASSEMBLY_FACTORY}. */
    public static final RegistrySupplier<Block> ASSEMBLY_FACTORY = registerBlockWithoutItem("assembly_factory",
            () -> new com.hbm_m.block.machines.MachineAssemblyFactoryBlock(BlockProps.copy(Blocks.IRON_BLOCK).noOcclusion().isSuffocating((state, world, pos) -> false).strength(5.0F, 18.0F).sound(SoundType.STONE)));

    public static final RegistrySupplier<Block> AUTOSAW = registerBlock("autosaw",
            () -> new com.hbm_m.block.machines.MachineAutosawBlock(BlockProps.copy(Blocks.IRON_BLOCK).noOcclusion().strength(5.0F, 6.0F).sound(SoundType.STONE)));
    public static final RegistrySupplier<Block> BAT9000 = registerBlockWithoutItem("bat9000",
            () -> new MachineBat9000Block(BlockProps.copy(Blocks.IRON_BLOCK).requiresCorrectToolForDrops().noOcclusion().isSuffocating((state, world, pos) -> false).strength(5.0F, 6.0F).sound(SoundType.STONE)));

    /** Die Teilchenquelle - Kopf des Beschleunigers. */
    public static final RegistrySupplier<Block> PA_SOURCE = registerBlock("pa_source",
            () -> new com.hbm_m.block.machines.albion.PASourceBlock(BlockProps.copy(Blocks.IRON_BLOCK).sound(SoundType.METAL).noOcclusion().strength(5.0F, 6.0F)));

    /** Der Detektor - hier wird das Rezept abgerechnet. */
    public static final RegistrySupplier<Block> PA_DETECTOR = registerBlock("pa_detector",
            () -> new com.hbm_m.block.machines.albion.PADetectorBlock(BlockProps.copy(Blocks.IRON_BLOCK).sound(SoundType.METAL).noOcclusion().strength(5.0F, 6.0F)));

    /** Strahlrohr des Teilchenbeschleunigers. */
    public static final RegistrySupplier<Block> BEAMLINE = registerBlock("beamline",
            () -> new com.hbm_m.block.machines.albion.PABeamlineBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(4.0f, 4.0f).sound(SoundType.METAL).noOcclusion()));

    public static final RegistrySupplier<Block> BOILER = registerBlock("boiler",
            () -> new com.hbm_m.block.machines.MachineBoilerBlock(BlockProps.copy(Blocks.IRON_BLOCK).noOcclusion().strength(5.0F, 6.0F).sound(SoundType.STONE)));

    public static final RegistrySupplier<Block> PUMP_STEAM = registerBlock("pump_steam",
            () -> new com.hbm_m.block.machines.MachinePumpBlock(BlockProps.copy(Blocks.IRON_BLOCK).noOcclusion().strength(5.0F, 6.0F).sound(SoundType.STONE), false));

    public static final RegistrySupplier<Block> PUMP_ELECTRIC = registerBlock("pump_electric",
            () -> new com.hbm_m.block.machines.MachinePumpBlock(BlockProps.copy(Blocks.IRON_BLOCK).noOcclusion().strength(5.0F, 6.0F).sound(SoundType.STONE), true));
    /** 1:1-Port von {@code MachineFusionBoiler} (1.7.10, dort {@code fusion_boiler}). */
    public static final RegistrySupplier<Block> BOILER_FUSION = registerBlockWithoutItem("boiler_fusion",
            () -> new MachineFusionBoilerBlock(BlockProps.copy(Blocks.IRON_BLOCK).noOcclusion().isSuffocating((state, world, pos) -> false).strength(5.0F, 36.0F).sound(SoundType.STONE)));

    /** 1:1-Port von {@code MachineFusionBreeder} (1.7.10, dort {@code fusion_breeder}). */
    public static final RegistrySupplier<Block> BREEDER_FUSION = registerBlockWithoutItem("breeder_fusion",
            () -> new MachineFusionBreederBlock(BlockProps.copy(Blocks.IRON_BLOCK).noOcclusion().isSuffocating((state, world, pos) -> false).strength(5.0F, 36.0F).sound(SoundType.STONE)));

    public static final RegistrySupplier<Block> CHIMNEY_BRICK = registerBlock("chimney_brick",
            () -> new com.hbm_m.block.machines.MachineChimneyBlock(BlockProps.copy(Blocks.IRON_BLOCK).noOcclusion().strength(5.0F, 60.0F).sound(SoundType.STONE), 12));

    public static final RegistrySupplier<Block> CHIMNEY_INDUSTRIAL = registerBlock("chimney_industrial",
            () -> new com.hbm_m.block.machines.MachineChimneyBlock(BlockProps.copy(Blocks.IRON_BLOCK).noOcclusion().strength(5.0F, 60.0F).sound(SoundType.STONE), 22));

    public static final RegistrySupplier<Block> COKER = registerBlock("coker",
            () -> new com.hbm_m.block.machines.MachineCokerBlock(BlockProps.copy(Blocks.IRON_BLOCK).noOcclusion().strength(5.0F, 6.0F).sound(SoundType.STONE)));
    /** 1:1-Port von {@code MachineFusionCollector} (1.7.10, dort {@code fusion_collector}). */
    public static final RegistrySupplier<Block> COLLECTOR = registerBlockWithoutItem("collector",
            () -> new MachineFusionCollectorBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(5.0f, 60.0f).sound(SoundType.METAL).noOcclusion().isSuffocating((state, world, pos) -> false)));

    public static final RegistrySupplier<Block> COMBINATION_OVEN = registerBlock("combination_oven",
            () -> new MachineCombinationOvenBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(4.0f, 4.0f).sound(SoundType.METAL).noOcclusion()));

    public static final RegistrySupplier<Block> COMBUSTION_ENGINE = registerBlock("combustion_engine",
            () -> new com.hbm_m.block.machines.MachineCombustionEngineBlock(BlockProps.copy(Blocks.IRON_BLOCK).noOcclusion().strength(5.0F, 6.0F).sound(SoundType.STONE)));

    public static final RegistrySupplier<Block> COMPRESSOR = registerBlock("compressor",
            () -> new com.hbm_m.block.machines.MachineCompressorBlock(BlockProps.copy(Blocks.IRON_BLOCK).noOcclusion().strength(10.0F, 12.0F).sound(SoundType.STONE)));

    /** Original: {@code machine_compressor_compact}. */
    public static final RegistrySupplier<Block> COMPRESSOR_COMPACT = registerBlock("compressor_compact",
            () -> com.hbm_m.block.machines.MachineCompressorBlock.createCompact(BlockProps.copy(Blocks.IRON_BLOCK).noOcclusion().strength(10.0F, 12.0F).sound(SoundType.STONE)));

    /** Original machine_precass (MachinePrecAss): Haerte 5, Widerstand 30. */
    public static final RegistrySupplier<Block> MACHINE_PRECASS = registerBlock("machine_precass",
            () -> new com.hbm_m.block.machines.MachinePrecAssBlock(BlockProps.copy(Blocks.IRON_BLOCK).noOcclusion().isSuffocating((st, w, p) -> false).strength(5.0F, 18.0F).sound(SoundType.STONE)));
    public static final RegistrySupplier<Block> PUREX = registerBlock("purex",
            () -> new com.hbm_m.block.machines.MachinePUREXBlock(BlockProps.copy(Blocks.IRON_BLOCK).noOcclusion().strength(5.0F, 18.0F).sound(SoundType.STONE)));
    /** 1:1 MachineSuperComputer (Original: Haerte 5, Widerstand 30). */
    public static final RegistrySupplier<Block> MACHINE_SUPERCOMPUTER = registerBlock("machine_supercomputer",
            () -> new com.hbm_m.block.machines.MachineSuperComputerBlock(BlockProps.copy(Blocks.IRON_BLOCK).noOcclusion().isSuffocating((st, w, p) -> false).strength(5.0F, 30.0F).sound(SoundType.METAL)));
    /** 1:1 LaunchpadLambda (Original: Haerte 5, Widerstand 10). */
    public static final RegistrySupplier<Block> LAUNCHPAD_LAMBDA = registerBlock("launchpad_lambda",
            () -> new com.hbm_m.block.machines.LaunchpadLambdaBlock(BlockProps.copy(Blocks.IRON_BLOCK).noOcclusion().isSuffocating((st, w, p) -> false).strength(5.0F, 10.0F).sound(SoundType.METAL)));
    /** 1:1 MachineTapeDrive (Original: Haerte 5, Widerstand 10). */
    public static final RegistrySupplier<Block> MACHINE_TAPE_DRIVE = registerBlock("machine_tape_drive",
            () -> new com.hbm_m.block.machines.MachineTapeDriveBlock(BlockProps.copy(Blocks.IRON_BLOCK).noOcclusion().strength(5.0F, 10.0F).sound(SoundType.METAL)));
    /** 1:1 MachineRockMill (Original: Haerte 5, Widerstand 100). */
    public static final RegistrySupplier<Block> MACHINE_ROCKMILL = registerBlock("machine_rockmill",
            () -> new com.hbm_m.block.machines.MachineRockMillBlock(BlockProps.copy(Blocks.IRON_BLOCK).noOcclusion().isSuffocating((st, w, p) -> false).strength(5.0F, 100.0F).sound(SoundType.STONE)));

    public static final RegistrySupplier<Block> INDUSTRIAL_GENERATOR = registerBlock("industrial_generator",
            () -> new com.hbm_m.block.machines.MachineIndustrialGeneratorBlock(BlockProps.copy(Blocks.IRON_BLOCK).noOcclusion().strength(5.0F, 6.0F).sound(SoundType.STONE)));

    public static final RegistrySupplier<Block> STEAM_ENGINE = registerBlock("steam_engine",
            () -> new com.hbm_m.block.machines.MachineSteamEngineBlock(BlockProps.copy(Blocks.IRON_BLOCK).noOcclusion().strength(5.0F, 6.0F).sound(SoundType.STONE)));

    public static final RegistrySupplier<Block> CONDENSER_POWERED = registerBlock("condenser_powered",
            () -> new com.hbm_m.block.machines.MachineCondenserPoweredBlock(BlockProps.copy(Blocks.IRON_BLOCK).noOcclusion().strength(5.0F, 6.0F).sound(SoundType.STONE)));

    public static final RegistrySupplier<Block> LPW2 = registerBlock("lpw2",
            () -> new com.hbm_m.block.machines.MachineLpw2Block(BlockProps.copy(Blocks.IRON_BLOCK).noOcclusion().strength(5.0F, 60.0F).sound(SoundType.STONE)));

    public static final RegistrySupplier<Block> CONVEYOR_PRESS = registerBlock("conveyor_press",
            () -> new com.hbm_m.block.machines.MachineConveyorPressBlock(BlockProps.copy(Blocks.IRON_BLOCK).noOcclusion().strength(5.0F, 6.0F).sound(SoundType.STONE)));
    /** 1:1-Port von {@code MachineFusionCoupler} (1.7.10, dort {@code fusion_coupler}). */
    public static final RegistrySupplier<Block> COUPLER = registerBlockWithoutItem("coupler",
            () -> new MachineFusionCouplerBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(5.0f, 60.0f).sound(SoundType.METAL).noOcclusion().isSuffocating((state, world, pos) -> false)));

    public static final RegistrySupplier<Block> DETECTOR = registerBlock("detector",
            // audit10: 1:1 PowerDetector (TileEntityMachineDetector) statt Platzhalterblock
            () -> new com.hbm_m.block.machines.PowerDetectorBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(5.0F, 6.0F).sound(SoundType.STONE)));

    public static final RegistrySupplier<Block> DIESELGEN = registerBlock("dieselgen",
            () -> new com.hbm_m.block.machines.MachineDieselGeneratorBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(4.0f, 4.0f).sound(SoundType.METAL).noOcclusion()));
    /** Ablenkmagnet - die Weiche des Rings. */
    public static final RegistrySupplier<Block> DIPOLE = registerBlock("dipole",
            () -> new com.hbm_m.block.machines.albion.PADipoleBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(4.0f, 4.0f).sound(SoundType.METAL).noOcclusion()));

    public static final RegistrySupplier<Block> DRONE = registerBlock("drone",
            () -> new Block(BlockProps.copy(Blocks.IRON_BLOCK).strength(4.0f, 4.0f).sound(SoundType.METAL).noOcclusion()));

    public static final RegistrySupplier<Block> ELECTRIC_FURNACE = registerBlock("electric_furnace",
            // Original machine_electric_furnace_off/_on: Haerte 5, Widerstand 10, an = Lichtstaerke 1.0
            () -> new MachineElectricFurnaceBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(5.0F, 6.0F).sound(SoundType.STONE)
                    .lightLevel(s -> s.getValue(MachineElectricFurnaceBlock.LIT) ? 15 : 0)));

    public static final RegistrySupplier<Block> ELECTRIC_HEATER = registerBlock("electric_heater",
            () -> new com.hbm_m.block.machines.MachineElectricHeaterBlock(BlockProps.copy(Blocks.IRON_BLOCK).noOcclusion().strength(5.0F, 6.0F).sound(SoundType.STONE)));

    public static final RegistrySupplier<Block> ELECTROLYSER = registerBlock("electrolyser",
            () -> new com.hbm_m.block.machines.MachineElectrolyserBlock(BlockProps.copy(Blocks.IRON_BLOCK).noOcclusion().strength(10.0F, 12.0F).sound(SoundType.STONE)));

    public static final RegistrySupplier<Block> EPRESS = registerBlock("epress",
            () -> new com.hbm_m.block.machines.MachineEPressBlock(BlockProps.copy(Blocks.IRON_BLOCK).noOcclusion().strength(5.0F, 6.0F).sound(SoundType.STONE)));

    public static final RegistrySupplier<Block> EXPOSURE_CHAMBER = registerBlock("exposure_chamber",
            () -> new com.hbm_m.block.machines.MachineExposureChamberBlock(BlockProps.copy(Blocks.IRON_BLOCK).noOcclusion().strength(5.0F, 6.0F).sound(SoundType.STONE)));

    /** Ursprungs-ID war "fensu2" - entspricht im Original tatsaechlich {@code TileEntityBatteryREDD} (Reddendite-Batterie),
     * nicht einer zweiten FENSU-Stufe (Namensverwechslung im Asset-Datensatz, siehe Recherche). */
    public static final RegistrySupplier<Block> FENSU2 = registerBlockWithoutItem("machine_battery_redd",
            () -> new com.hbm_m.block.machines.MachineBatteryREDDBlock(BlockProps.copy(Blocks.IRON_BLOCK).noOcclusion().isSuffocating((s, w, p) -> false).strength(5.0F, 6.0F).sound(SoundType.STONE)));

    public static final RegistrySupplier<Block> FIREBOX = registerBlock("firebox",
            () -> new com.hbm_m.block.machines.MachineFireboxBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(4.0f, 4.0f).sound(SoundType.METAL).noOcclusion()));
    /** R7l: 1:1 FractionSpacer. */
    public static final RegistrySupplier<Block> FRACTION_SPACER = registerBlockWithoutItem("fraction_spacer",
            () -> new com.hbm_m.block.machines.FractionSpacerBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(5.0F, 6.0F).requiresCorrectToolForDrops().noOcclusion().isSuffocating((s, w, p) -> false).sound(SoundType.STONE)));

    public static final RegistrySupplier<Block> FURNACE_BRICK = registerBlock("furnace_brick",
            // Original machine_furnace_brick_off/_on: Haerte 5, Widerstand 10, an = Lichtstaerke 1.0
            () -> new MachineFurnaceBrickBlock(BlockProps.copy(Blocks.BRICKS).strength(5.0F, 6.0F).sound(SoundType.STONE)
                    .lightLevel(s -> s.getValue(MachineFurnaceBrickBlock.LIT) ? 15 : 0)));

    public static final RegistrySupplier<Block> FURNACE_IRON = registerBlock("furnace_iron",
            () -> new MachineFurnaceIronBlock(BlockProps.copy(Blocks.IRON_BLOCK).noOcclusion().strength(5.0F, 6.0F).sound(SoundType.STONE)));

    public static final RegistrySupplier<Block> FURNACE_STEEL = registerBlock("furnace_steel",
            () -> new MachineFurnaceSteelBlock(BlockProps.copy(Blocks.IRON_BLOCK).noOcclusion().strength(5.0F, 6.0F).sound(SoundType.STONE)));

    public static final RegistrySupplier<Block> HEATEX = registerBlock("heatex",
            () -> new com.hbm_m.block.machines.MachineHeatexBlock(BlockProps.copy(Blocks.IRON_BLOCK).noOcclusion().strength(5.0F, 6.0F).sound(SoundType.STONE)));

    public static final RegistrySupplier<Block> HEPHAESTUS = registerBlock("hephaestus",
            () -> new com.hbm_m.block.machines.MachineHephaestusBlock(BlockProps.copy(Blocks.IRON_BLOCK).noOcclusion().strength(10.0F, 6.0F).sound(SoundType.STONE)));
    public static final RegistrySupplier<Block> ICF = registerBlock("icf",
            () -> new com.hbm_m.block.machines.icf.MachineICFBlock(
                    BlockProps.copy(Blocks.IRON_BLOCK).noOcclusion().strength(5.0F, 36.0F).sound(SoundType.STONE)));

    // Der ICF-Laser besteht aus sechs Bauteilen. Original: ein Block "icf_laser_component" mit
    // sechs Metadaten - die gibt es in 1.20 nicht mehr, darum sechs eigene Bloecke.
    public static final RegistrySupplier<Block> ICF_LASER_CASING = registerBlock("icf_laser_casing",
            () -> new com.hbm_m.block.machines.icf.ICFLaserComponentBlock(
                    BlockProps.copy(Blocks.IRON_BLOCK).strength(5.0f, 60.0f).sound(SoundType.METAL),
                    com.hbm_m.block.machines.icf.ICFLaserPart.CASING));
    public static final RegistrySupplier<Block> ICF_LASER_PORT = registerBlock("icf_laser_port",
            () -> new com.hbm_m.block.machines.icf.ICFLaserComponentBlock(
                    BlockProps.copy(Blocks.IRON_BLOCK).strength(5.0f, 60.0f).sound(SoundType.METAL),
                    com.hbm_m.block.machines.icf.ICFLaserPart.PORT));
    public static final RegistrySupplier<Block> ICF_LASER_CELL = registerBlock("icf_laser_cell",
            () -> new com.hbm_m.block.machines.icf.ICFLaserComponentBlock(
                    BlockProps.copy(Blocks.IRON_BLOCK).strength(5.0f, 60.0f).sound(SoundType.METAL),
                    com.hbm_m.block.machines.icf.ICFLaserPart.CELL));
    public static final RegistrySupplier<Block> ICF_LASER_EMITTER = registerBlock("icf_laser_emitter",
            () -> new com.hbm_m.block.machines.icf.ICFLaserComponentBlock(
                    BlockProps.copy(Blocks.IRON_BLOCK).strength(5.0f, 60.0f).sound(SoundType.METAL),
                    com.hbm_m.block.machines.icf.ICFLaserPart.EMITTER));
    public static final RegistrySupplier<Block> ICF_LASER_CAPACITOR = registerBlock("icf_laser_capacitor",
            () -> new com.hbm_m.block.machines.icf.ICFLaserComponentBlock(
                    BlockProps.copy(Blocks.IRON_BLOCK).strength(5.0f, 60.0f).sound(SoundType.METAL),
                    com.hbm_m.block.machines.icf.ICFLaserPart.CAPACITOR));
    public static final RegistrySupplier<Block> ICF_LASER_TURBOCHARGER = registerBlock("icf_laser_turbocharger",
            () -> new com.hbm_m.block.machines.icf.ICFLaserComponentBlock(
                    BlockProps.copy(Blocks.IRON_BLOCK).strength(5.0f, 60.0f).sound(SoundType.METAL),
                    com.hbm_m.block.machines.icf.ICFLaserPart.TURBO));

    /** Der Block zu einem Laserbauteil - der Platzhalter braucht ihn zum Zuruecksetzen. */
    public static Block icfLaserPart(com.hbm_m.block.machines.icf.ICFLaserPart part) {
        return switch (part) {
            case CASING    -> ICF_LASER_CASING.get();
            case PORT      -> ICF_LASER_PORT.get();
            case CELL      -> ICF_LASER_CELL.get();
            case EMITTER   -> ICF_LASER_EMITTER.get();
            case CAPACITOR -> ICF_LASER_CAPACITOR.get();
            case TURBO     -> ICF_LASER_TURBOCHARGER.get();
        };
    }


    /** 1:1 {@code MachineIntake} (Haerte 10, Widerstand 20), Item: {@code ModItems.INTAKE}. */
    public static final RegistrySupplier<Block> INTAKE = registerBlockWithoutItem("intake",
            () -> new com.hbm_m.block.machines.MachineIntakeBlock(BlockProps.copy(Blocks.IRON_BLOCK).noOcclusion().isSuffocating((state, world, pos) -> false).strength(10.0F, 12.0F).sound(SoundType.STONE)));

    /** 1:1-Port von {@code MachineFusionKlystron} (1.7.10, dort {@code fusion_klystron}). */
    public static final RegistrySupplier<Block> KLYSTRON = registerBlockWithoutItem("klystron",
            () -> new MachineFusionKlystronBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(5.0f, 60.0f).sound(SoundType.METAL).noOcclusion().isSuffocating((state, world, pos) -> false)));

    /** 1:1-Port von {@code MachineFusionKlystronCreative} (1.7.10, dort {@code fusion_klystron_creative}). */
    public static final RegistrySupplier<Block> KLYSTRON_CREATIVE = registerBlockWithoutItem("klystron_creative",
            () -> new MachineFusionKlystronCreativeBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(5.0f, 60.0f).sound(SoundType.METAL).noOcclusion().isSuffocating((state, world, pos) -> false)));

    /** 1:1-Port von {@code MachineFusionMHDT} (1.7.10, dort {@code fusion_mhdt}). */
    public static final RegistrySupplier<Block> MHDT = registerBlockWithoutItem("mhdt",
            () -> new MachineFusionMhdtBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(5.0f, 60.0f).sound(SoundType.METAL).noOcclusion().isSuffocating((state, world, pos) -> false)));

    public static final RegistrySupplier<Block> MICROWAVE = registerBlock("microwave",
            () -> new com.hbm_m.block.machines.MachineMicrowaveBlock(BlockProps.copy(Blocks.IRON_BLOCK).noOcclusion().strength(5.0F, 6.0F).sound(SoundType.STONE)));

    public static final RegistrySupplier<Block> MINING_LASER = registerBlock("mining_laser",
            () -> new com.hbm_m.block.machines.MachineMiningLaserBlock(BlockProps.copy(Blocks.IRON_BLOCK).noOcclusion().strength(5.0F, 60.0F).sound(SoundType.STONE)));

    public static final RegistrySupplier<Block> OILBURNER = registerBlock("oilburner",
            () -> new com.hbm_m.block.machines.MachineOilburnerBlock(BlockProps.copy(Blocks.IRON_BLOCK).noOcclusion().strength(5.0F, 6.0F).sound(SoundType.STONE)));

    public static final RegistrySupplier<Block> OILBURNER_HP = registerBlock("oilburner_hp",
            () -> new com.hbm_m.block.machines.MachineOilburnerBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(4.0f, 4.0f).sound(SoundType.METAL).noOcclusion()));

    /**
     * Der Orbus ist im Original ein Multiblock ({@code getDimensions {4,0,2,1,2,1}}, Setzversatz 1):
     * fuenf Felder hoch, drei mal drei breit, mit je vier Anschlusszellen ganz unten und ganz oben.
     */
    public static final RegistrySupplier<Block> ORBUS = registerBlock("orbus",
            () -> new com.hbm_m.block.machines.BarrelTankBlock(BlockProps.copy(Blocks.IRON_BLOCK).noOcclusion().strength(5.0F, 6.0F).sound(SoundType.STONE),
                    com.hbm_m.blockentity.machines.OrbusBlockEntity::new,
                    () -> com.hbm_m.blockentity.ModBlockEntities.ORBUS_BE.get(),
                    null,
                    () -> com.hbm_m.multiblock.DummyableStructureBuilder.create()
                            .box(4, 0, 2, 1, 2, 1)
                            // Original: je vier Zellen auf Hoehe 0 und 4, hinten und zur Seite.
                            .extra(-1, 0, 0)
                            .extra(0, 0, 1)
                            .extra(-1, 0, 1)
                            .extra(0, 4, 0)
                            .extra(-1, 4, 0)
                            .extra(0, 4, 1)
                            .extra(-1, 4, 1)
                            .placementOffset(1)
                            .build(() -> UNIVERSAL_MACHINE_PART.get().defaultBlockState())));
                    
    public static final RegistrySupplier<Block> ORE_SLOPPER = registerBlock("ore_slopper",
            () -> new MachineOreSlopperBlock(BlockProps.copy(Blocks.IRON_BLOCK).noOcclusion().strength(5.0F, 6.0F).sound(SoundType.STONE)));

    /** 1:1-Port von {@code MachineFusionPlasmaForge} (1.7.10, dort {@code fusion_plasma_forge}). */
    public static final RegistrySupplier<Block> PLASMA_FORGE = registerBlockWithoutItem("plasma_forge",
            () -> new MachineFusionPlasmaForgeBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(5.0f, 60.0f).sound(SoundType.METAL).noOcclusion().isSuffocating((state, world, pos) -> false)));

    public static final RegistrySupplier<Block> PYROOVEN = registerBlock("pyrooven",
            () -> new com.hbm_m.block.machines.MachinePyroOvenBlock(BlockProps.copy(Blocks.IRON_BLOCK).noOcclusion().strength(5.0F, 6.0F).sound(SoundType.STONE)));
    /** Quadrupolmagnet - buendelt den Strahl. */
    public static final RegistrySupplier<Block> QUADRUPOLE = registerBlock("quadrupole",
            () -> new com.hbm_m.block.machines.albion.PAQuadrupoleBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(4.0f, 4.0f).sound(SoundType.METAL).noOcclusion()));

    public static final RegistrySupplier<Block> RADGEN = registerBlock("radgen",
            () -> new Block(BlockProps.copy(Blocks.IRON_BLOCK).strength(4.0f, 4.0f).sound(SoundType.METAL).noOcclusion()));

    public static final RegistrySupplier<Block> RADIOLYSIS = registerBlock("radiolysis",
            () -> new com.hbm_m.block.machines.MachineRadiolysisBlock(BlockProps.copy(Blocks.IRON_BLOCK).noOcclusion().strength(10.0F, 6.0F).sound(SoundType.STONE)));
    public static final RegistrySupplier<Block> REACTOR_SMALL = registerBlock("reactor_small",
            () -> new Block(BlockProps.copy(Blocks.IRON_BLOCK).noOcclusion().strength(5.0F, 6.0F).sound(SoundType.STONE)));

    /** Beschleunigerzelle - gibt dem Teilchen Impuls. */
    public static final RegistrySupplier<Block> RFC = registerBlock("rfc",
            () -> new com.hbm_m.block.machines.albion.PARFCBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(4.0f, 4.0f).sound(SoundType.METAL).noOcclusion()));

    public static final RegistrySupplier<Block> ROTARY_FURNACE = registerBlock("rotary_furnace",
            () -> new com.hbm_m.block.machines.MachineRotaryFurnaceBlock(BlockProps.copy(Blocks.IRON_BLOCK).noOcclusion().strength(5.0F, 6.0F).sound(SoundType.STONE)));

    public static final RegistrySupplier<Block> SAWMILL = registerBlock("sawmill",
            () -> new com.hbm_m.block.machines.MachineSawmillBlock(BlockProps.copy(Blocks.IRON_BLOCK).noOcclusion().strength(5.0F, 6.0F).sound(SoundType.STONE)));

    public static final RegistrySupplier<Block> THRESHER = registerBlock("thresher",
            () -> new com.hbm_m.block.machines.MachineThresherBlock(BlockProps.copy(Blocks.IRON_BLOCK).noOcclusion().strength(5.0F, 6.0F).sound(SoundType.STONE)));

    public static final RegistrySupplier<Block> SOLIDIFIER = registerBlock("solidifier",
            () -> new com.hbm_m.block.machines.MachineSolidifierBlock(BlockProps.copy(Blocks.IRON_BLOCK).noOcclusion().strength(10.0F, 12.0F).sound(SoundType.STONE)));

    public static final RegistrySupplier<Block> ASHPIT = registerBlockWithoutItem("ashpit",
            () -> new com.hbm_m.block.machines.MachineAshpitBlock(BlockProps.copy(Blocks.IRON_BLOCK).noOcclusion().strength(5.0F, 6.0F).sound(SoundType.STONE)));
    public static final RegistrySupplier<Block> SOURCE = registerBlock("source",
            () -> new Block(BlockProps.copy(Blocks.IRON_BLOCK).strength(4.0f, 4.0f).sound(SoundType.METAL).noOcclusion()));

    public static final RegistrySupplier<Block> REACTOR_RESEARCH = registerBlockWithoutItem("reactor_research",
            () -> new com.hbm_m.block.machines.MachineReactorResearchBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(5.0f, 10.0f).sound(SoundType.METAL).noOcclusion()));

    public static final RegistrySupplier<Block> STIRLING = registerBlock("stirling",
            () -> new com.hbm_m.block.machines.MachineStirlingBlock(BlockProps.copy(Blocks.IRON_BLOCK).noOcclusion().strength(5.0F, 6.0F).sound(SoundType.STONE)));

    public static final RegistrySupplier<Block> STIRLING_CREATIVE = registerBlock("stirling_creative",
            () -> new com.hbm_m.block.machines.MachineStirlingBlock(BlockProps.copy(Blocks.IRON_BLOCK).noOcclusion().strength(5.0F, 6.0F).sound(SoundType.STONE)));

    public static final RegistrySupplier<Block> STIRLING_STEEL = registerBlock("stirling_steel",
            () -> new com.hbm_m.block.machines.MachineStirlingBlock(BlockProps.copy(Blocks.IRON_BLOCK).noOcclusion().strength(5.0F, 6.0F).sound(SoundType.STONE)));

    public static final RegistrySupplier<Block> STRAND_CASTER = registerBlock("strand_caster",
            () -> new com.hbm_m.block.machines.MachineStrandCasterBlock(BlockProps.copy(Blocks.IRON_BLOCK).noOcclusion().strength(5.0F, 6.0F).sound(SoundType.STONE)));
    /** 1:1-Port von {@code MachineFusionTorus} (1.7.10, dort {@code fusion_torus}). */
    public static final RegistrySupplier<Block> TORUS = registerBlockWithoutItem("torus",
            () -> new MachineFusionTorusBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(5.0f, 60.0f).sound(SoundType.METAL).noOcclusion().isSuffocating((state, world, pos) -> false)));

    public static final RegistrySupplier<Block> TURBINEGAS = registerBlock("turbinegas",
            () -> new com.hbm_m.block.machines.MachineTurbineGasBlock(BlockProps.copy(Blocks.IRON_BLOCK).noOcclusion().strength(5.0F, 6.0F).sound(SoundType.STONE)));
    /** R7p: 1:1 WatzPump (2 hoch, Item: ModItems.WATZ_PUMP). */
    public static final RegistrySupplier<Block> WATZ_PUMP = registerBlockWithoutItem("watz_pump",
            () -> new com.hbm_m.block.machines.WatzBlocks.Pump(BlockProps.copy(Blocks.IRON_BLOCK).strength(5.0F, 6.0F).noOcclusion().sound(SoundType.STONE))); // audit13: Original WatzPump droppt sich selbst

    public static final RegistrySupplier<Block> CHUNGUS = registerBlock("chungus",
            () -> new Block(BlockProps.copy(Blocks.IRON_BLOCK).strength(4.0f, 4.0f).sound(SoundType.METAL).noOcclusion()));

    // ─── AUTO-PORT: fehlende Original-Bloecke (nur DEV-Tab, ungeprueft) ───
    // Automatisch aus dem 1.7.10-Original uebernommen: Bloecke, deren Registry-Name im
    // Port fehlte und fuer die das Original eine Cube-Textur mitbringt. Reine
    // Platzhalter mit Stein-Eigenschaften - Varianten-, Deko- und OBJ-Modell-Bloecke
    // sind bewusst ausgelassen. Sichtbar NUR im Dev-Tab.

    /** Original: {@code block_asbestos} ist ein {@code BlockOutgas} (Material Wolle, Haerte 5/15). */
    public static final RegistrySupplier<Block> BLOCK_ASBESTOS = registerBlock("block_asbestos",
            () -> new com.hbm_m.block.gas.OutgasBlock(BlockProps.copy(Blocks.WHITE_WOOL).strength(5.0F, 9.0F),
                    () -> ModBlocks.GAS_ASBESTOS.get(), true, true, false, false, true));
    public static final RegistrySupplier<Block> BLOCK_BAKELITE = registerBlock("block_bakelite",
            () -> new Block(BlockProps.copy(Blocks.STONE).strength(3.0F, 3.0F).sound(SoundType.STONE).requiresCorrectToolForDrops()));
    /** R6c: 1:1 BlockPlasticExplosive. */
    public static final RegistrySupplier<Block> BLOCK_C4 = registerBlock("block_c4",
            () -> new com.hbm_m.block.bomb.PlasticExplosiveBlock(BlockProps.copy(Blocks.TNT).strength(2.0F, 1.2F).sound(SoundType.METAL)));
    public static final RegistrySupplier<Block> BLOCK_COLTAN = registerBlock("block_coltan",
            () -> new Block(BlockProps.copy(Blocks.IRON_BLOCK).sound(SoundType.METAL).requiresCorrectToolForDrops().strength(5.0F, 30.0F)));

    // ── Welt-Fluessigkeiten (Original: ModBlocks.*_block + *_fluid, kein Item) ────────────────
    private static BlockBehaviour.Properties fluidProps(boolean lava) {
        return BlockBehaviour.Properties.copy(lava ? Blocks.LAVA : Blocks.WATER).strength(100F, 500F).noLootTable();
    }
    public static final RegistrySupplier<LiquidBlock> MUD_BLOCK = BLOCKS.register("mud_block",
            () -> new com.hbm_m.block.fluid.MudBlock(() -> com.hbm_m.inventory.fluid.WorldFluids.MUD.getSource(), fluidProps(false).lightLevel(s -> 5)));
    public static final RegistrySupplier<LiquidBlock> ACID_BLOCK = BLOCKS.register("acid_block",
            () -> new com.hbm_m.block.fluid.AcidBlock(() -> com.hbm_m.inventory.fluid.WorldFluids.ACID.getSource(), fluidProps(false).lightLevel(s -> 5)));
    public static final RegistrySupplier<LiquidBlock> TOXIC_BLOCK = BLOCKS.register("toxic_block",
            () -> new com.hbm_m.block.fluid.RadioactiveFluidBlock(() -> com.hbm_m.inventory.fluid.WorldFluids.TOXIC.getSource(), fluidProps(false).lightLevel(s -> 15), false));
    public static final RegistrySupplier<LiquidBlock> SCHRABIDIC_BLOCK = BLOCKS.register("schrabidic_block",
            () -> new com.hbm_m.block.fluid.RadioactiveFluidBlock(() -> com.hbm_m.inventory.fluid.WorldFluids.SCHRABIDIC.getSource(), fluidProps(false), true));
    public static final RegistrySupplier<LiquidBlock> VOLCANIC_LAVA_BLOCK = BLOCKS.register("volcanic_lava_block",
            () -> new com.hbm_m.block.fluid.VolcanicBlock(() -> com.hbm_m.inventory.fluid.WorldFluids.VOLCANIC_LAVA.getSource(), fluidProps(true).lightLevel(s -> 15).randomTicks()));
    public static final RegistrySupplier<LiquidBlock> RAD_LAVA_BLOCK = BLOCKS.register("rad_lava_block",
            () -> new com.hbm_m.block.fluid.RadLavaBlock(() -> com.hbm_m.inventory.fluid.WorldFluids.RAD_LAVA.getSource(), fluidProps(true).lightLevel(s -> 15).randomTicks()));
    public static final RegistrySupplier<LiquidBlock> SULFURIC_ACID_BLOCK = BLOCKS.register("sulfuric_acid_block",
            () -> new com.hbm_m.block.fluid.GenericFluidBlock(() -> com.hbm_m.inventory.fluid.WorldFluids.SULFURIC_ACID.getSource(), fluidProps(false))
                    .setDamage((level, e) -> com.hbm_m.damagesource.ModDamageSources.acid(level), 5F));
    /** Original {@code corium_block} = {@code CoriumFinite} (5 Quanten, Tickrate 30). */
    public static final RegistrySupplier<Block> CORIUM_BLOCK = BLOCKS.register("corium_block",
            () -> new com.hbm_m.block.fluid.CoriumFiniteBlock(BlockBehaviour.Properties.copy(Blocks.LAVA).strength(100F, 500F).lightLevel(s -> 10).noOcclusion().noLootTable().randomTicks()));
    /** Original {@code concrete_liquid} = {@code GenericFiniteFluid} mit 4 Quanten, Material Stein. */
    public static final RegistrySupplier<Block> CONCRETE_LIQUID = BLOCKS.register("concrete_liquid",
            () -> new com.hbm_m.block.fluid.FiniteFluidBlock(BlockBehaviour.Properties.copy(Blocks.STONE).strength(100F, 500F).noOcclusion().noLootTable(), 4, 20));

    // ── Basalterze (Original: ore_basalt, BlockOreBasalt mit EnumBasaltOreType) ─────────────
    public static final RegistrySupplier<Block> ORE_BASALT_SULFUR = registerBlock("ore_basalt_sulfur",
            () -> new com.hbm_m.block.generic.BlockOreBasalt(BlockProps.copy(Blocks.STONE).requiresCorrectToolForDrops().strength(5.0F, 6.0F), com.hbm_m.block.generic.BlockOreBasalt.Type.SULFUR));
    public static final RegistrySupplier<Block> ORE_BASALT_FLUORITE = registerBlock("ore_basalt_fluorite",
            () -> new com.hbm_m.block.generic.BlockOreBasalt(BlockProps.copy(Blocks.STONE).requiresCorrectToolForDrops().strength(5.0F, 6.0F), com.hbm_m.block.generic.BlockOreBasalt.Type.FLUORITE));
    public static final RegistrySupplier<Block> ORE_BASALT_ASBESTOS = registerBlock("ore_basalt_asbestos",
            () -> new com.hbm_m.block.generic.BlockOreBasalt(BlockProps.copy(Blocks.STONE).requiresCorrectToolForDrops().strength(5.0F, 6.0F), com.hbm_m.block.generic.BlockOreBasalt.Type.ASBESTOS));
    public static final RegistrySupplier<Block> ORE_BASALT_GEM = registerBlock("ore_basalt_gem",
            () -> new com.hbm_m.block.generic.BlockOreBasalt(BlockProps.copy(Blocks.STONE).requiresCorrectToolForDrops().strength(5.0F, 6.0F), com.hbm_m.block.generic.BlockOreBasalt.Type.GEM));
    public static final RegistrySupplier<Block> ORE_BASALT_MOLYSITE = registerBlock("ore_basalt_molysite",
            () -> new com.hbm_m.block.generic.BlockOreBasalt(BlockProps.copy(Blocks.STONE).requiresCorrectToolForDrops().strength(5.0F, 6.0F), com.hbm_m.block.generic.BlockOreBasalt.Type.MOLYSITE));

    public static final RegistrySupplier<Block> BLOCK_CORIUM = registerBlock("block_corium",
            () -> new com.hbm_m.block.generic.BlockHazard(BlockProps.copy(Blocks.IRON_BLOCK).strength(100.0F, 3600.0F).requiresCorrectToolForDrops().sound(SoundType.STONE)));
    public static final RegistrySupplier<Block> BLOCK_CORIUM_COBBLE = registerBlock("block_corium_cobble",
            () -> new com.hbm_m.block.gas.OutgasBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(100.0F, 3600.0F).requiresCorrectToolForDrops().sound(SoundType.STONE), () -> ModBlocks.GAS_RADON.get(), true, true, true, false, false));
    public static final RegistrySupplier<Block> BLOCK_EUPHEMIUM_CLUSTER = registerBlock("block_euphemium_cluster",
            () -> new net.minecraft.world.level.block.RotatedPillarBlock(BlockProps.copy(Blocks.STONE).strength(5.0F, 36000.0F).sound(SoundType.STONE).requiresCorrectToolForDrops()));
    public static final RegistrySupplier<Block> BLOCK_FIBERGLASS = registerBlock("block_fiberglass",
            () -> new net.minecraft.world.level.block.RotatedPillarBlock(BlockProps.copy(Blocks.WHITE_WOOL).strength(5.0F, 9.0F).sound(SoundType.WOOL)));
    public static final RegistrySupplier<Block> BLOCK_FLUORITE = registerBlock("block_fluorite",
            () -> new Block(BlockProps.copy(Blocks.IRON_BLOCK).strength(5.0F, 6.0F).requiresCorrectToolForDrops().sound(SoundType.STONE)));
    public static final RegistrySupplier<Block> BLOCK_GRAPHITE_DETECTOR = registerBlock("block_graphite_detector",
            () -> new com.hbm_m.block.machines.pile.GraphitePileBlocks.Detector(BlockProps.copy(Blocks.IRON_BLOCK).strength(5.0F, 6.0F).sound(SoundType.METAL).requiresCorrectToolForDrops()));
    public static final RegistrySupplier<Block> BLOCK_GRAPHITE_ROD = registerBlock("block_graphite_rod",
            () -> new com.hbm_m.block.machines.pile.GraphitePileBlocks.Rod(BlockProps.copy(Blocks.IRON_BLOCK).strength(5.0F, 6.0F).sound(SoundType.METAL).requiresCorrectToolForDrops()));
    /** R7a: 1:1 Chicago Pile MK1. */
    public static final RegistrySupplier<Block> BLOCK_GRAPHITE_DRILLED = registerBlock("block_graphite_drilled",
            () -> new com.hbm_m.block.machines.pile.GraphitePileBlocks.Drilled(BlockProps.copy(Blocks.IRON_BLOCK).strength(5.0F, 6.0F).sound(SoundType.METAL).requiresCorrectToolForDrops()));
    public static final RegistrySupplier<Block> BLOCK_GRAPHITE_FUEL = registerBlock("block_graphite_fuel",
            () -> new com.hbm_m.block.machines.pile.GraphitePileBlocks.Fuel(BlockProps.copy(Blocks.IRON_BLOCK).strength(5.0F, 6.0F).sound(SoundType.METAL).requiresCorrectToolForDrops()));
    public static final RegistrySupplier<Block> BLOCK_GRAPHITE_LITHIUM = registerBlock("block_graphite_lithium",
            () -> new com.hbm_m.block.machines.pile.GraphitePileBlocks.BreedingFuel(BlockProps.copy(Blocks.IRON_BLOCK).strength(5.0F, 6.0F).sound(SoundType.METAL).requiresCorrectToolForDrops()));
    public static final RegistrySupplier<Block> BLOCK_GRAPHITE_PLUTONIUM = registerBlock("block_graphite_plutonium",
            () -> new com.hbm_m.block.machines.pile.GraphitePileBlocks.Source(BlockProps.copy(Blocks.IRON_BLOCK).strength(5.0F, 6.0F).sound(SoundType.METAL).requiresCorrectToolForDrops(), true));
    public static final RegistrySupplier<Block> BLOCK_GRAPHITE_SOURCE = registerBlock("block_graphite_source",
            () -> new com.hbm_m.block.machines.pile.GraphitePileBlocks.Source(BlockProps.copy(Blocks.IRON_BLOCK).strength(5.0F, 6.0F).sound(SoundType.METAL).requiresCorrectToolForDrops(), false));
    public static final RegistrySupplier<Block> BLOCK_GRAPHITE_TRITIUM = registerBlock("block_graphite_tritium",
            () -> new com.hbm_m.block.machines.pile.GraphitePileBlocks.BreedingProduct(BlockProps.copy(Blocks.IRON_BLOCK).strength(5.0F, 6.0F).sound(SoundType.METAL).requiresCorrectToolForDrops()));
    public static final RegistrySupplier<Block> BLOCK_INSULATOR = registerBlock("block_insulator",
            () -> new net.minecraft.world.level.block.RotatedPillarBlock(BlockProps.copy(Blocks.WHITE_WOOL).strength(5.0F, 6.0F).sound(SoundType.WOOL)));
    public static final RegistrySupplier<Block> BLOCK_LITHIUM = registerBlock("block_lithium",
            () -> new com.hbm_m.block.generic.BlockLithium(BlockProps.copy(Blocks.IRON_BLOCK).strength(5.0F, 6.0F).sound(SoundType.METAL).requiresCorrectToolForDrops()));
    public static final RegistrySupplier<Block> BLOCK_MAGNETIZED_TUNGSTEN = registerBlock("block_magnetized_tungsten",
            () -> new Block(BlockProps.copy(Blocks.IRON_BLOCK).sound(SoundType.METAL).requiresCorrectToolForDrops().strength(5.0F, 45.0F)));
    public static final RegistrySupplier<Block> BLOCK_METEOR_BROKEN = registerBlock("block_meteor_broken",
            () -> new com.hbm_m.block.generic.BlockOre(BlockProps.copy(Blocks.STONE).strength(15.0F, 216.0F).sound(SoundType.STONE).requiresCorrectToolForDrops()));
    public static final RegistrySupplier<Block> BLOCK_METEOR_MOLTEN = registerBlock("block_meteor_molten",
            () -> new com.hbm_m.block.generic.BlockMeteorMolten(BlockProps.copy(Blocks.STONE).strength(15.0F, 216.0F).lightLevel(st -> 11).requiresCorrectToolForDrops()));
    public static final RegistrySupplier<Block> BLOCK_METEOR_TREASURE = registerBlock("block_meteor_treasure",
            () -> new Block(BlockProps.copy(Blocks.STONE).strength(15.0F, 216.0F).requiresCorrectToolForDrops()));
    /** Original ore_meteor (BlockMeteorOre, EnumMeteorType IRON..COBALT), Haerte 5, Widerstand 10. */
    public static final RegistrySupplier<Block> ORE_METEOR_IRON = registerBlock("ore_meteor_iron",
            () -> new Block(BlockProps.copy(Blocks.STONE).strength(5.0F, 6.0F).requiresCorrectToolForDrops()));
    public static final RegistrySupplier<Block> ORE_METEOR_COPPER = registerBlock("ore_meteor_copper",
            () -> new Block(BlockProps.copy(Blocks.STONE).strength(5.0F, 6.0F).requiresCorrectToolForDrops()));
    public static final RegistrySupplier<Block> ORE_METEOR_ALUMINIUM = registerBlock("ore_meteor_aluminium",
            () -> new Block(BlockProps.copy(Blocks.STONE).strength(5.0F, 6.0F).requiresCorrectToolForDrops()));
    public static final RegistrySupplier<Block> ORE_METEOR_RAREEARTH = registerBlock("ore_meteor_rareearth",
            () -> new Block(BlockProps.copy(Blocks.STONE).strength(5.0F, 6.0F).requiresCorrectToolForDrops()));
    public static final RegistrySupplier<Block> ORE_METEOR_COBALT = registerBlock("ore_meteor_cobalt",
            () -> new Block(BlockProps.copy(Blocks.STONE).strength(5.0F, 6.0F).requiresCorrectToolForDrops()));
    public static final RegistrySupplier<Block> BLOCK_NITER = registerBlock("block_niter",
            () -> new Block(BlockProps.copy(Blocks.IRON_BLOCK).strength(5.0F, 6.0F).requiresCorrectToolForDrops().sound(SoundType.STONE)));
    public static final RegistrySupplier<Block> BLOCK_POLYMER = registerBlock("block_polymer",
            () -> new Block(BlockProps.copy(Blocks.STONE).sound(SoundType.STONE).requiresCorrectToolForDrops().strength(3.0F, 6.0F)));
    public static final RegistrySupplier<Block> BLOCK_PU_MIX = registerBlock("block_pu_mix",
            () -> ((com.hbm_m.block.generic.BlockHazard) new com.hbm_m.block.generic.BlockHazard(BlockProps.copy(Blocks.IRON_BLOCK).strength(5.0F, 30.0F).sound(SoundType.METAL).requiresCorrectToolForDrops()).makeBeaconable()).setDisplayEffect(com.hbm_m.block.generic.BlockHazard.ExtDisplayEffect.RADFOG));
    public static final RegistrySupplier<Block> BLOCK_RED_PHOSPHORUS = registerBlock("block_red_phosphorus",
            () -> new com.hbm_m.block.generic.BlockHazardFalling(BlockProps.copy(Blocks.SAND).strength(5.0F, 6.0F).sound(SoundType.SAND)));
    public static final RegistrySupplier<Block> BLOCK_RUBBER = registerBlock("block_rubber",
            () -> new Block(BlockProps.copy(Blocks.STONE).sound(SoundType.STONE).requiresCorrectToolForDrops().strength(3.0F, 9.0F)));
    /** R6c: 1:1 BlockPlasticExplosive. */
    public static final RegistrySupplier<Block> BLOCK_SEMTEX = registerBlock("block_semtex",
            () -> new com.hbm_m.block.bomb.PlasticExplosiveBlock(BlockProps.copy(Blocks.TNT).strength(2.0F, 1.2F).sound(SoundType.METAL)));
    public static final RegistrySupplier<Block> BLOCK_SMORE = registerBlock("block_smore",
            () -> new Block(BlockProps.copy(Blocks.STONE).strength(15.0F, 360.0F).sound(SoundType.STONE).requiresCorrectToolForDrops()));
    public static final RegistrySupplier<Block> BLOCK_SULFUR = registerBlock("block_sulfur",
            () -> new Block(BlockProps.copy(Blocks.IRON_BLOCK).strength(5.0F, 6.0F).requiresCorrectToolForDrops().sound(SoundType.STONE)));
    public static final RegistrySupplier<Block> BLOCK_TANTALIUM = registerBlock("block_tantalium",
            () -> new Block(BlockProps.copy(Blocks.IRON_BLOCK).sound(SoundType.METAL).requiresCorrectToolForDrops().strength(5.0F, 30.0F)));
    public static final RegistrySupplier<Block> BLOCK_TRINITITE = registerBlock("block_trinitite",
            () -> new com.hbm_m.block.generic.BlockHazard(BlockProps.copy(Blocks.IRON_BLOCK).strength(5.0F, 6.0F).requiresCorrectToolForDrops().sound(SoundType.STONE)).makeBeaconable());
    public static final RegistrySupplier<Block> BLOCK_TRITIUM = registerBlock("block_tritium",
            () -> new net.minecraft.world.level.block.RotatedPillarBlock(BlockProps.copy(Blocks.GLASS).strength(3.0F, 1.2F).sound(SoundType.GLASS)));
    public static final RegistrySupplier<Block> BLOCK_WASTE = registerBlock("block_waste",
            () -> ((com.hbm_m.block.generic.BlockHazard) new com.hbm_m.block.generic.BlockNuclearWaste(BlockProps.copy(Blocks.IRON_BLOCK).strength(5.0F, 6.0F).requiresCorrectToolForDrops().sound(SoundType.STONE)).makeBeaconable()).setDisplayEffect(com.hbm_m.block.generic.BlockHazard.ExtDisplayEffect.RADFOG));
    public static final RegistrySupplier<Block> BLOCK_WASTE_VITRIFIED = registerBlock("block_waste_vitrified",
            () -> ((com.hbm_m.block.generic.BlockHazard) new com.hbm_m.block.generic.BlockNuclearWaste(BlockProps.copy(Blocks.IRON_BLOCK).strength(5.0F, 6.0F).requiresCorrectToolForDrops().sound(SoundType.STONE)).makeBeaconable()).setDisplayEffect(com.hbm_m.block.generic.BlockHazard.ExtDisplayEffect.RADFOG));
    public static final RegistrySupplier<Block> BLOCK_WHITE_PHOSPHORUS = registerBlock("block_white_phosphorus",
            () -> new com.hbm_m.block.generic.BlockHazard(BlockProps.copy(Blocks.STONE).strength(5.0F, 6.0F).sound(SoundType.STONE).requiresCorrectToolForDrops()).makeBeaconable());
    public static final RegistrySupplier<Block> BLOCK_YELLOWCAKE = registerBlock("block_yellowcake",
            () -> new com.hbm_m.block.generic.BlockHazardFalling(BlockProps.copy(Blocks.SAND).strength(5.0F, 6.0F).sound(SoundType.SAND)));
    /** R6d: 1:1 BlockForgottenBrick. */
    public static final RegistrySupplier<Block> BRICK_FORGOTTEN = registerBlock("brick_forgotten",
            () -> new com.hbm_m.block.generic.ForgottenBricks.Brick(BlockProps.copy(Blocks.STONE).strength(-1.0F, 400000.0F).sound(SoundType.STONE).noLootTable()));
    /** R6d: 1:1 BlockForgottenLock. */
    public static final RegistrySupplier<Block> BRICK_FORGOTTEN_LOCK = registerBlock("brick_forgotten_lock",
            () -> new com.hbm_m.block.generic.ForgottenBricks.Lock(BlockProps.copy(Blocks.STONE).strength(-1.0F, 400000.0F).sound(SoundType.STONE).noLootTable()));
    /** R6c: 1:1 DigammaMatter. */
    public static final RegistrySupplier<Block> DIGAMMA_MATTER = registerBlock("digamma_matter",
            () -> new com.hbm_m.block.bomb.DigammaBlocks.Matter(BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(-1.0F, 10800000.0F).noOcclusion().noCollission().noLootTable()));
    /** 1:1 DungeonSpawner (unzerstoerbar, Widerstand 500000). */
    public static final RegistrySupplier<Block> DUNGEON_SPAWNER = registerBlock("dungeon_spawner",
            () -> new com.hbm_m.block.generic.DungeonSpawnerBlock(BlockProps.copy(Blocks.STONE).strength(-1.0F, 500000.0F).noLootTable()));
    public static final RegistrySupplier<Block> EVENT_TESTER = registerBlock("event_tester",
            () -> new Block(BlockProps.copy(Blocks.STONE).strength(2.5F, 0.0F)));
    /** R7h: 1:1 FluidDuctPaintableBlockExhaust. */
    public static final RegistrySupplier<Block> FLUID_DUCT_PAINTABLE_BLOCK_EXHAUST = registerBlock("fluid_duct_paintable_block_exhaust",
            () -> new com.hbm_m.block.network.PaintableDuctBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(5.0F, 6.0F).requiresCorrectToolForDrops().sound(SoundType.STONE), true));
    /** R6d: 1:1 BlockGeysir. */
    public static final RegistrySupplier<Block> GEYSIR_NETHER = registerBlock("geysir_nether",
            () -> new com.hbm_m.block.generic.BlockGeysir(BlockProps.copy(Blocks.STONE).strength(2.0F, 2.0F).sound(SoundType.STONE).requiresCorrectToolForDrops().lightLevel(s -> 15), true));
    public static final RegistrySupplier<Block> GEYSIR_CHLORINE = registerBlock("geysir_chlorine",
            () -> new com.hbm_m.block.generic.BlockGeysir(BlockProps.copy(Blocks.STONE).strength(5.0F, 5.0F).sound(SoundType.STONE).requiresCorrectToolForDrops(), false));
    // Der Platzhalter des zusammengebauten Lasers. Ohne Gegenstand - er faellt nie.
    public static final RegistrySupplier<Block> ICF_BLOCK = registerBlockWithoutItem("icf_block",
            () -> new com.hbm_m.block.machines.icf.ICFPhantomBlock(
                    BlockProps.copy(Blocks.IRON_BLOCK).strength(5.0F, 6.0F).sound(SoundType.STONE)));
    /** 1:1 LaunchTable: 9x9-Starttisch fuer Baukastenraketen aller Groessen. */
    public static final RegistrySupplier<Block> LAUNCH_TABLE = registerBlock("launch_table",
            () -> new com.hbm_m.block.machines.LaunchTableBlock(BlockProps.copy(Blocks.IRON_BLOCK).noOcclusion().strength(5.0F, 6.0F).sound(SoundType.STONE)));
    /** 1:1 LogicBlock / LogicBlockInvis (ohne Haerte wie im Original). */
    public static final RegistrySupplier<Block> LOGIC_BLOCK = registerBlock("logic_block",
            () -> new com.hbm_m.block.generic.LogicBlock(BlockProps.copy(Blocks.STONE).strength(0.0F).noOcclusion()));
    public static final RegistrySupplier<Block> LOGIC_BLOCK_INVIS = registerBlock("logic_block_invis",
            () -> new com.hbm_m.block.generic.LogicBlockInvis(BlockProps.copy(Blocks.STONE).strength(0.0F).noOcclusion()));
    public static final RegistrySupplier<Block> MUSH_BLOCK_STEM = registerBlock("mush_block_stem",
            () -> new com.hbm_m.block.generic.R6dPlants.MushHuge(BlockProps.copy(Blocks.MUSHROOM_STEM).strength(0.2F).sound(SoundType.GRASS).lightLevel(s -> 15)));
    /** Original ore_aluminium = ALUMINUM_ORE (Registry-ID ore_aluminium). */
    public static final RegistrySupplier<Block> ORE_ALUMINIUM = ALUMINUM_ORE;
    public static final RegistrySupplier<Block> ORE_AUSTRALIUM = registerBlock("ore_australium",
            () -> new Block(BlockProps.copy(Blocks.STONE).strength(5.0F, 6.0F)));
    public static final RegistrySupplier<Block> ORE_COPPER = registerBlock("ore_copper",
            () -> new Block(BlockProps.copy(Blocks.STONE).strength(5.0F, 6.0F)));
    public static final RegistrySupplier<Block> ORE_GNEISS_URANIUM_SCORCHED = registerBlock("ore_gneiss_uranium_scorched",
            () -> new com.hbm_m.block.gas.OutgasBlock(BlockProps.copy(Blocks.STONE).sound(SoundType.STONE).requiresCorrectToolForDrops().strength(1.5F, 6.0F), () -> ModBlocks.GAS_RADON.get(), true, true));
    public static final RegistrySupplier<Block> ORE_NETHER_PLUTONIUM = registerBlock("ore_nether_plutonium",
            () -> new Block(BlockProps.copy(Blocks.STONE).strength(0.4F, 6.0F)));
    public static final RegistrySupplier<Block> ORE_NETHER_TUNGSTEN = registerBlock("ore_nether_tungsten",
            () -> new Block(BlockProps.copy(Blocks.STONE).strength(0.4F, 6.0F)));
    public static final RegistrySupplier<Block> ORE_NETHER_URANIUM_SCORCHED = registerBlock("ore_nether_uranium_scorched",
            () -> new com.hbm_m.block.gas.OutgasBlock(BlockProps.copy(Blocks.STONE).sound(SoundType.STONE).requiresCorrectToolForDrops().strength(0.4F, 6.0F), () -> ModBlocks.GAS_RADON.get(), true, true));
    public static final RegistrySupplier<Block> ORE_RARE = registerBlock("ore_rare",
            () -> new com.hbm_m.block.generic.BlockOre(BlockProps.copy(Blocks.STONE).strength(5.0F, 6.0F).sound(SoundType.STONE).requiresCorrectToolForDrops()));
    public static final RegistrySupplier<Block> ORE_TEKTITE_OSMIRIDIUM = registerBlock("ore_tektite_osmiridium",
            () -> new Block(BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(0.5F, 0.5F).sound(SoundType.SAND)));
    public static final RegistrySupplier<Block> ORE_URANIUM_SCORCHED = registerBlock("ore_uranium_scorched",
            () -> new com.hbm_m.block.gas.OutgasBlock(BlockProps.copy(Blocks.STONE).strength(5.0F, 6.0F).sound(SoundType.STONE).requiresCorrectToolForDrops(), () -> ModBlocks.GAS_RADON.get(), true, true));
    // Uranmeiler (Chicago Pile). Der Ziegel wird gemauert, der Bohrer macht daraus den Meiler.
    public static final RegistrySupplier<Block> PILE_BLOCK = registerBlockWithoutItem("pile_block",
            () -> new com.hbm_m.block.machines.pile.PileBlock(
                    BlockProps.copy(Blocks.IRON_BLOCK).strength(15.0F, 6.0F)));
    public static final RegistrySupplier<Block> PILE_BRICK = registerBlock("pile_brick",
            () -> new com.hbm_m.block.machines.pile.PileBrickBlock(
                    BlockProps.copy(Blocks.STONE).strength(5.0F, 6.0F).sound(SoundType.METAL)));
    public static final RegistrySupplier<Block> PILE_LOADER = registerBlock("pile_loader",
            () -> new com.hbm_m.block.machines.pile.PileLoaderBlock(
                    BlockProps.copy(Blocks.IRON_BLOCK).noOcclusion()));
    public static final RegistrySupplier<Block> PILE_VENT = registerBlock("pile_vent",
            () -> new com.hbm_m.block.machines.pile.PileVentBlock(
                    BlockProps.copy(Blocks.IRON_BLOCK).noOcclusion()));
    public static final RegistrySupplier<Block> PILE_CONTROL = registerBlock("pile_control",
            () -> new com.hbm_m.block.machines.pile.PileControlBlock(
                    BlockProps.copy(Blocks.IRON_BLOCK).noOcclusion()));
    public static final RegistrySupplier<Block> PNEUMATIC_STORAGE_ACCESS = registerBlock("pneumatic_storage_access",
            () -> new com.hbm_m.block.network.pneumatic.PneumoStorageAccessBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(5.0F, 6.0F).sound(com.hbm_m.sound.ModSoundTypes.PIPE)));
    public static final RegistrySupplier<Block> PNEUMATIC_STORAGE_CLUTTER = registerBlock("pneumatic_storage_clutter",
            () -> new com.hbm_m.block.network.pneumatic.PneumoStorageClutterBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(5.0F, 6.0F).sound(com.hbm_m.sound.ModSoundTypes.PIPE)));
    public static final RegistrySupplier<Block> PNEUMATIC_STORAGE_EXPORTER = registerBlock("pneumatic_storage_exporter",
            () -> new com.hbm_m.block.network.pneumatic.PneumoStorageExporterBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(5.0F, 6.0F).sound(com.hbm_m.sound.ModSoundTypes.PIPE)));
    public static final RegistrySupplier<Block> PNEUMATIC_STORAGE_IMPORTER = registerBlock("pneumatic_storage_importer",
            () -> new com.hbm_m.block.network.pneumatic.PneumoStorageImporterBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(5.0F, 6.0F).sound(com.hbm_m.sound.ModSoundTypes.PIPE)));
    public static final RegistrySupplier<Block> PNEUMATIC_STORAGE_MONO = registerBlock("pneumatic_storage_mono",
            () -> new com.hbm_m.block.network.pneumatic.PneumoStorageMonoBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(5.0F, 6.0F).sound(com.hbm_m.sound.ModSoundTypes.PIPE)));
    public static final RegistrySupplier<Block> STRUCTURE_ANCHOR = registerBlock("structure_anchor",
            () -> new Block(BlockProps.copy(Blocks.STONE).strength(2.5F, 6.0F)));
    /** 1:1 BlockWandTandem. */
    public static final RegistrySupplier<Block> WAND_TANDEM = registerBlock("wand_tandem",
            () -> new com.hbm_m.block.generic.BlockWandTandem(BlockProps.copy(Blocks.IRON_BLOCK).strength(0.0F).sound(SoundType.STONE)));
    // ─── ENDE AUTO-PORT Bloecke ───

    public static final RegistrySupplier<Block> TEST_BLOCK = registerBlock("test_block",
            () -> new TestBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(4.0f, 4.0f).sound(SoundType.METAL).noOcclusion()));

    /** ДИАГНОСТИКА: столб частиц вверх (ваниль + NT) — тест рендер-пайплайнов. */
    public static final RegistrySupplier<Block> PARTICLE_TEST_BLOCK = registerBlock("particle_test_block",
            () -> new ParticleTestBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(4.0f, 4.0f).sound(SoundType.METAL)));

    public static final RegistrySupplier<Block> BRICK_COMPOUND_STAIRS = registerBlock("brick_compound_stairs",
            () -> new StairBlock(BRICK_COMPOUND.get().defaultBlockState(), BlockProps.copy(Blocks.STONE).requiresCorrectToolForDrops().strength(15.0F, 240.0F)));
    public static final RegistrySupplier<Block> REINFORCED_BRICK_STAIRS = registerBlock("reinforced_brick_stairs",
            () -> new StairBlock(REINFORCED_BRICK.get().defaultBlockState(), BlockProps.copy(Blocks.STONE).requiresCorrectToolForDrops().strength(15.0F, 180.0F)));
    public static final RegistrySupplier<Block> LIGHTSTONE_BRICKS_STAIRS = registerBlock("lightstone_bricks_stairs",
            () -> new StairBlock(LIGHTSTONE_BRICKS.get().defaultBlockState(), BlockProps.copy(Blocks.STONE).strength(2.0f, 15.0f).requiresCorrectToolForDrops()));
    public static final RegistrySupplier<Block> REINFORCED_BRICK_SLAB = registerBlock("reinforced_brick_slab",
            () -> new SlabBlock(BlockProps.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));
    public static final RegistrySupplier<Block> BRICK_COMPOUND_SLAB = registerBlock("brick_compound_slab",
            () -> new SlabBlock(BlockProps.copy(Blocks.STONE).strength(5.0f, 4.0f).requiresCorrectToolForDrops()));
    private static <T extends Block> RegistrySupplier<T> registerBlock(String name, Supplier<T> block) {
        RegistrySupplier<T> toReturn = BLOCKS.register(name, block);
        registerBlockItem(name, toReturn);
        return toReturn;
    }

    // ---- R6b: Scheinwerfer 1:1 (Spotlight/SpotlightModular/SpotlightBeam) ----
    public static final RegistrySupplier<Block> CAGE_LAMP_OFF = registerBlock("cage_lamp_off",
            () -> new com.hbm_m.block.decorations.SpotlightBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(0.5F, 0.5F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion(), 2, com.hbm_m.block.decorations.SpotlightBlock.LightType.INCANDESCENT, false, () -> ModBlocks.CAGE_LAMP_OFF.get(), () -> ModBlocks.CAGE_LAMP.get()));
    public static final RegistrySupplier<Block> FLOOD_LAMP_OFF = registerBlock("flood_lamp_off",
            () -> new com.hbm_m.block.decorations.SpotlightBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(0.5F, 0.5F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion(), 32, com.hbm_m.block.decorations.SpotlightBlock.LightType.HALOGEN, false, () -> ModBlocks.FLOOD_LAMP_OFF.get(), () -> ModBlocks.FLOOD_LAMP.get()));
    public static final RegistrySupplier<Block> FLUORESCENT_LAMP = registerBlock("fluorescent_lamp",
            () -> new com.hbm_m.block.decorations.SpotlightBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(0.5F, 0.5F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion(), 8, com.hbm_m.block.decorations.SpotlightBlock.LightType.FLUORESCENT, true, () -> ModBlocks.FLUORESCENT_LAMP_OFF.get(), () -> ModBlocks.FLUORESCENT_LAMP.get()));
    public static final RegistrySupplier<Block> FLUORESCENT_LAMP_OFF = registerBlock("fluorescent_lamp_off",
            () -> new com.hbm_m.block.decorations.SpotlightBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(0.5F, 0.5F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion(), 8, com.hbm_m.block.decorations.SpotlightBlock.LightType.FLUORESCENT, false, () -> ModBlocks.FLUORESCENT_LAMP_OFF.get(), () -> ModBlocks.FLUORESCENT_LAMP.get()));
    public static final RegistrySupplier<Block> SPOTLIGHT_BEAM = registerBlockWithoutItem("spotlight_beam",
            () -> new com.hbm_m.block.decorations.SpotlightBeamBlock(BlockBehaviour.Properties.of().air().noCollission().noLootTable().replaceable().lightLevel(s -> 15).strength(-1.0F, 600000.0F)));

    public static final RegistrySupplier<Block> FLOODLIGHT = registerBlock("floodlight",
            () -> new com.hbm_m.block.decorations.FloodlightBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(5.0F, 6.0F).requiresCorrectToolForDrops().noOcclusion().sound(SoundType.STONE)));
    public static final RegistrySupplier<Block> FLOODLIGHT_BEAM = registerBlockWithoutItem("floodlight_beam",
            () -> new com.hbm_m.block.decorations.FloodlightBeamBlock(BlockBehaviour.Properties.of().air().noCollission().noLootTable().replaceable().lightLevel(s -> 15).strength(-1.0F, 600000.0F)));
    public static final RegistrySupplier<Block> LANTERN = registerBlock("lantern",
            () -> new com.hbm_m.block.decorations.LanternBlock(BlockProps.copy(Blocks.IRON_BLOCK).strength(3.0F, 3.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion().lightLevel(s -> 15)));
    // ---- R6a: fehlende einfache Bloecke (1:1 aus ModBlocks 1.7.10) ----
    public static final RegistrySupplier<Block> METEOR_BATTERY = registerBlock("meteor_battery",
            () -> new Block(BlockProps.copy(Blocks.STONE).strength(15.0F, 216.0F).sound(SoundType.STONE).requiresCorrectToolForDrops()));
    public static final RegistrySupplier<Block> BLOCK_WASTE_PAINTED = registerBlock("block_waste_painted",
            () -> ((com.hbm_m.block.generic.BlockHazard) new com.hbm_m.block.generic.BlockNuclearWaste(BlockProps.copy(Blocks.IRON_BLOCK).strength(5.0F, 6.0F).requiresCorrectToolForDrops().sound(SoundType.STONE)).makeBeaconable()).setDisplayEffect(com.hbm_m.block.generic.BlockHazard.ExtDisplayEffect.RADFOG));
    public static final RegistrySupplier<Block> DECO_ASBESTOS = registerBlock("deco_asbestos",
            () -> new com.hbm_m.block.gas.OutgasBlock(BlockProps.copy(Blocks.WHITE_WOOL).strength(5.0F, 6.0F).sound(SoundType.STONE), () -> ModBlocks.GAS_ASBESTOS.get(), true, true));
    public static final RegistrySupplier<Block> BURNING_EARTH = registerBlock("burning_earth",
            () -> new com.hbm_m.block.generic.WasteEarth(BlockProps.copy(Blocks.DIRT).strength(0.6F, 0.6F).sound(SoundType.GRASS), true));
    public static final RegistrySupplier<Block> IMPACT_DIRT = registerBlock("impact_dirt",
            () -> new com.hbm_m.block.generic.BlockDirtHbm(BlockProps.copy(Blocks.DIRT).strength(0.5F, 0.5F).sound(SoundType.GRAVEL), true, false));
    public static final RegistrySupplier<Block> NTM_DIRT = registerBlock("ntm_dirt",
            () -> new com.hbm_m.block.generic.BlockDirtHbm(BlockProps.copy(Blocks.DIRT).strength(0.5F, 0.5F).sound(SoundType.GRAVEL), false, true));
    public static final RegistrySupplier<Block> PLANT_DEAD_GRASS = registerBlock("plant_dead_grass",
            () -> new com.hbm_m.block.generic.BlockDeadPlant(BlockProps.copy(Blocks.GRASS).strength(0.0F, 0.0F).sound(SoundType.GRASS).noCollission().instabreak().noOcclusion()));
    public static final RegistrySupplier<Block> PLANT_DEAD_FLOWER = registerBlock("plant_dead_flower",
            () -> new com.hbm_m.block.generic.BlockDeadPlant(BlockProps.copy(Blocks.GRASS).strength(0.0F, 0.0F).sound(SoundType.GRASS).noCollission().instabreak().noOcclusion()));
    public static final RegistrySupplier<Block> PLANT_DEAD_BIGFLOWER = registerBlock("plant_dead_bigflower",
            () -> new com.hbm_m.block.generic.BlockDeadPlant(BlockProps.copy(Blocks.GRASS).strength(0.0F, 0.0F).sound(SoundType.GRASS).noCollission().instabreak().noOcclusion()));
    public static final RegistrySupplier<Block> PLANT_DEAD_FERN = registerBlock("plant_dead_fern",
            () -> new com.hbm_m.block.generic.BlockDeadPlant(BlockProps.copy(Blocks.GRASS).strength(0.0F, 0.0F).sound(SoundType.GRASS).noCollission().instabreak().noOcclusion()));
    public static final RegistrySupplier<Block> STONE_BIOME_DESERT = registerBlock("stone_biome_desert",
            () -> new com.hbm_m.block.generic.BlockBiomeStone(BlockProps.copy(Blocks.STONE).strength(5.0F, 6.0F).sound(SoundType.STONE).requiresCorrectToolForDrops()));
    public static final RegistrySupplier<Block> STONE_BIOME_WOODLAND = registerBlock("stone_biome_woodland",
            () -> new com.hbm_m.block.generic.BlockBiomeStone(BlockProps.copy(Blocks.STONE).strength(5.0F, 6.0F).sound(SoundType.STONE).requiresCorrectToolForDrops()));
    public static final RegistrySupplier<Block> BLOCK_COKE_COAL = registerBlock("block_coke_coal",
            () -> new com.hbm_m.block.generic.BlockCoke(BlockProps.copy(Blocks.IRON_BLOCK).strength(5.0F, 6.0F).sound(SoundType.METAL).requiresCorrectToolForDrops()));
    public static final RegistrySupplier<Block> BLOCK_COKE_LIGNITE = registerBlock("block_coke_lignite",
            () -> new com.hbm_m.block.generic.BlockCoke(BlockProps.copy(Blocks.IRON_BLOCK).strength(5.0F, 6.0F).sound(SoundType.METAL).requiresCorrectToolForDrops()));
    public static final RegistrySupplier<Block> BLOCK_COKE_PETROLEUM = registerBlock("block_coke_petroleum",
            () -> new com.hbm_m.block.generic.BlockCoke(BlockProps.copy(Blocks.IRON_BLOCK).strength(5.0F, 6.0F).sound(SoundType.METAL).requiresCorrectToolForDrops()));
    public static final RegistrySupplier<Block> BLOCK_CAP_NUKA = registerBlock("block_cap_nuka",
            () -> new Block(BlockProps.copy(Blocks.IRON_BLOCK).strength(5.0F, 6.0F).requiresCorrectToolForDrops().sound(SoundType.STONE)));
    public static final RegistrySupplier<Block> BLOCK_CAP_QUANTUM = registerBlock("block_cap_quantum",
            () -> new Block(BlockProps.copy(Blocks.IRON_BLOCK).strength(5.0F, 6.0F).requiresCorrectToolForDrops().sound(SoundType.STONE)));
    public static final RegistrySupplier<Block> BLOCK_CAP_SPARKLE = registerBlock("block_cap_sparkle",
            () -> new Block(BlockProps.copy(Blocks.IRON_BLOCK).strength(5.0F, 6.0F).sound(SoundType.METAL).requiresCorrectToolForDrops()));
    public static final RegistrySupplier<Block> BLOCK_CAP_RAD = registerBlock("block_cap_rad",
            () -> new Block(BlockProps.copy(Blocks.IRON_BLOCK).strength(5.0F, 6.0F).requiresCorrectToolForDrops().sound(SoundType.STONE)));
    public static final RegistrySupplier<Block> BLOCK_CAP_KORL = registerBlock("block_cap_korl",
            () -> new Block(BlockProps.copy(Blocks.IRON_BLOCK).strength(5.0F, 6.0F).requiresCorrectToolForDrops().sound(SoundType.STONE)));
    public static final RegistrySupplier<Block> BLOCK_CAP_FRITZ = registerBlock("block_cap_fritz",
            () -> new Block(BlockProps.copy(Blocks.IRON_BLOCK).strength(5.0F, 6.0F).requiresCorrectToolForDrops().sound(SoundType.STONE)));
    public static final RegistrySupplier<Block> PLATEMETAL_BASE = registerBlock("platemetal_base",
            () -> new Block(BlockProps.copy(Blocks.IRON_BLOCK).strength(5.0F, 6.0F).requiresCorrectToolForDrops().sound(com.hbm_m.sound.ModSoundTypes.PLATEMETAL)));
    public static final RegistrySupplier<Block> PLATEMETAL_BLACK = registerBlock("platemetal_black",
            () -> new Block(BlockProps.copy(Blocks.IRON_BLOCK).strength(5.0F, 6.0F).requiresCorrectToolForDrops().sound(com.hbm_m.sound.ModSoundTypes.PLATEMETAL)));
    public static final RegistrySupplier<Block> PLATEMETAL_WHITE = registerBlock("platemetal_white",
            () -> new Block(BlockProps.copy(Blocks.IRON_BLOCK).strength(5.0F, 6.0F).sound(SoundType.METAL).requiresCorrectToolForDrops()));
    public static final RegistrySupplier<Block> PLATEMETAL_RED = registerBlock("platemetal_red",
            () -> new Block(BlockProps.copy(Blocks.IRON_BLOCK).strength(5.0F, 6.0F).sound(SoundType.METAL).requiresCorrectToolForDrops()));
    public static final RegistrySupplier<Block> PLATEMETAL_GREEN = registerBlock("platemetal_green",
            () -> new Block(BlockProps.copy(Blocks.IRON_BLOCK).strength(5.0F, 6.0F).requiresCorrectToolForDrops().sound(com.hbm_m.sound.ModSoundTypes.PLATEMETAL)));
    public static final RegistrySupplier<Block> PLATEMETAL_LIGHT_GRAY = registerBlock("platemetal_light_gray",
            () -> new Block(BlockProps.copy(Blocks.IRON_BLOCK).strength(5.0F, 6.0F).sound(SoundType.METAL).requiresCorrectToolForDrops()));
    public static final RegistrySupplier<Block> PLATEMETAL_BLUE = registerBlock("platemetal_blue",
            () -> new Block(BlockProps.copy(Blocks.IRON_BLOCK).strength(5.0F, 6.0F).requiresCorrectToolForDrops().sound(com.hbm_m.sound.ModSoundTypes.PLATEMETAL)));
    public static final RegistrySupplier<Block> PLATEMETAL_PURPLE = registerBlock("platemetal_purple",
            () -> new Block(BlockProps.copy(Blocks.IRON_BLOCK).strength(5.0F, 6.0F).sound(SoundType.METAL).requiresCorrectToolForDrops()));
    public static final RegistrySupplier<Block> PLATEMETAL_CYAN = registerBlock("platemetal_cyan",
            () -> new Block(BlockProps.copy(Blocks.IRON_BLOCK).strength(5.0F, 6.0F).requiresCorrectToolForDrops().sound(com.hbm_m.sound.ModSoundTypes.PLATEMETAL)));
    public static final RegistrySupplier<Block> PLATEMETAL_PINK = registerBlock("platemetal_pink",
            () -> new Block(BlockProps.copy(Blocks.IRON_BLOCK).strength(5.0F, 6.0F).sound(SoundType.METAL).requiresCorrectToolForDrops()));
    public static final RegistrySupplier<Block> PLATEMETAL_LIME = registerBlock("platemetal_lime",
            () -> new Block(BlockProps.copy(Blocks.IRON_BLOCK).strength(5.0F, 6.0F).sound(SoundType.METAL).requiresCorrectToolForDrops()));
    public static final RegistrySupplier<Block> PLATEMETAL_YELLOW = registerBlock("platemetal_yellow",
            () -> new Block(BlockProps.copy(Blocks.IRON_BLOCK).strength(5.0F, 6.0F).sound(SoundType.METAL).requiresCorrectToolForDrops()));
    public static final RegistrySupplier<Block> PLATEMETAL_LIGHT_BLUE = registerBlock("platemetal_light_blue",
            () -> new Block(BlockProps.copy(Blocks.IRON_BLOCK).strength(5.0F, 6.0F).sound(SoundType.METAL).requiresCorrectToolForDrops()));
    public static final RegistrySupplier<Block> PLATEMETAL_MAGENTA = registerBlock("platemetal_magenta",
            () -> new Block(BlockProps.copy(Blocks.IRON_BLOCK).strength(5.0F, 6.0F).sound(SoundType.METAL).requiresCorrectToolForDrops()));
    public static final RegistrySupplier<Block> PLATEMETAL_ORANGE = registerBlock("platemetal_orange",
            () -> new Block(BlockProps.copy(Blocks.IRON_BLOCK).strength(5.0F, 6.0F).sound(SoundType.METAL).requiresCorrectToolForDrops()));
    public static final RegistrySupplier<Block> CONCRETE_SMOOTH = registerBlock("concrete_smooth",
            () -> new com.hbm_m.block.generic.BlockNoSpawn(BlockProps.copy(Blocks.STONE).strength(15.0F, 84.0F).sound(SoundType.STONE).requiresCorrectToolForDrops()));
    public static final RegistrySupplier<Block> DUCRETE_SMOOTH = registerBlock("ducrete_smooth",
            () -> new com.hbm_m.block.generic.BlockNoSpawn(BlockProps.copy(Blocks.STONE).strength(20.0F, 300.0F).sound(SoundType.STONE).requiresCorrectToolForDrops()));
    public static final RegistrySupplier<Block> CONCRETE_SMOOTH_STAIRS = registerBlock("concrete_smooth_stairs",
            () -> new net.minecraft.world.level.block.StairBlock(() -> ModBlocks.CONCRETE_SMOOTH.get().defaultBlockState(), BlockBehaviour.Properties.copy(ModBlocks.CONCRETE_SMOOTH.get())));
    public static final RegistrySupplier<Block> DUCRETE_SMOOTH_STAIRS = registerBlock("ducrete_smooth_stairs",
            () -> new net.minecraft.world.level.block.StairBlock(() -> ModBlocks.DUCRETE_SMOOTH.get().defaultBlockState(), BlockBehaviour.Properties.copy(ModBlocks.DUCRETE_SMOOTH.get())));
    public static final RegistrySupplier<Block> BRICK_ASBESTOS_STAIRS = registerBlock("brick_asbestos_stairs",
            () -> new net.minecraft.world.level.block.StairBlock(() -> ModBlocks.BRICK_ASBESTOS.get().defaultBlockState(), BlockBehaviour.Properties.copy(ModBlocks.BRICK_ASBESTOS.get()).strength(5.0F, 600.0F)));
    /** Original ducrete_stairs (BlockGenericStairs(ducrete)). */
    public static final RegistrySupplier<Block> DUCRETE_STAIRS = registerBlock("ducrete_stairs",
            () -> new net.minecraft.world.level.block.StairBlock(() -> ModBlocks.DUCRETE.get().defaultBlockState(), BlockBehaviour.Properties.copy(ModBlocks.DUCRETE.get())));
    /** Original brick_slab Meta 5 (brick_asbestos) - die uebrigen Metas sind die *_slab-Bloecke. */
    public static final RegistrySupplier<Block> BRICK_ASBESTOS_SLAB = registerBlock("brick_asbestos_slab",
            () -> new net.minecraft.world.level.block.SlabBlock(BlockBehaviour.Properties.copy(ModBlocks.BRICK_ASBESTOS.get())));
    /** Original pink_slab/pink_double_slab (BlockPinkSlab) und pink_stairs; Original ohne Creative-Tab. */
    public static final RegistrySupplier<Block> PINK_SLAB = registerBlock("pink_slab",
            () -> new net.minecraft.world.level.block.SlabBlock(BlockBehaviour.Properties.copy(ModBlocks.PINK_PLANKS.get())));
    /** Original obj_tester (TestObjTester): Testblock ohne Creative-Tab, Hardness 2.5 / Resistance 10. */
    public static final RegistrySupplier<Block> OBJ_TESTER = registerBlock("obj_tester",
            () -> new com.hbm_m.block.generic.TestObjTesterBlock(BlockProps.copy(Blocks.IRON_BLOCK).noOcclusion().strength(2.5F, 6.0F).sound(SoundType.STONE)));
    public static final RegistrySupplier<Block> PINK_STAIRS = registerBlock("pink_stairs",
            () -> new net.minecraft.world.level.block.StairBlock(() -> ModBlocks.PINK_PLANKS.get().defaultBlockState(), BlockBehaviour.Properties.copy(ModBlocks.PINK_PLANKS.get())));
    public static final RegistrySupplier<Block> LIGHTSTONE_TILE_STAIRS = registerBlock("lightstone_tile_stairs",
            () -> new net.minecraft.world.level.block.StairBlock(() -> ModBlocks.LIGHTSTONE_TILE.get().defaultBlockState(), BlockBehaviour.Properties.copy(ModBlocks.LIGHTSTONE_TILE.get())));
    public static final RegistrySupplier<Block> STONES_SLAB_TILE = registerBlock("stones_slab_tile",
            () -> new net.minecraft.world.level.block.SlabBlock(BlockBehaviour.Properties.copy(ModBlocks.LIGHTSTONE_TILE.get())));
    public static final RegistrySupplier<Block> STONES_SLAB_BRICKS = registerBlock("stones_slab_bricks",
            () -> new net.minecraft.world.level.block.SlabBlock(BlockBehaviour.Properties.copy(ModBlocks.LIGHTSTONE_BRICKS.get())));
    public static final RegistrySupplier<Block> BLOCK_FOAM = registerBlock("block_foam",
            () -> new Block(BlockProps.copy(Blocks.SNOW_BLOCK).strength(0.5F, 0.0F).sound(SoundType.SNOW)));
    public static final RegistrySupplier<Block> FOAM_LAYER = registerBlock("foam_layer",
            () -> new com.hbm_m.block.generic.BlockLayering(BlockProps.copy(Blocks.SNOW_BLOCK).strength(0.1F, 0.1F).sound(SoundType.SNOW).noOcclusion()));
    public static final RegistrySupplier<Block> SAND_BORON_LAYER = registerBlock("sand_boron_layer",
            () -> new com.hbm_m.block.generic.BlockLayering(BlockProps.copy(Blocks.SAND).strength(0.1F, 0.1F).sound(SoundType.SAND).noOcclusion()));
    public static final RegistrySupplier<Block> LEAVES_LAYER = registerBlock("leaves_layer",
            () -> new com.hbm_m.block.generic.BlockLayering(BlockProps.copy(Blocks.OAK_LEAVES).strength(0.1F, 0.1F).sound(SoundType.GRASS).noOcclusion()));

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
            // hev_battery: Original-Item (ItemFusionCore 150000, Stapel 4) und Block teilen sich im Port die ID.
            if (b instanceof com.hbm_m.block.generic.HevBatteryBlock) {
                return new com.hbm_m.item.tool.ItemHevBattery(b, new Item.Properties().stacksTo(4));
            }
            // Sammelfiguren: Untertyp als NBT, Darstellung ueber den Figuren-Renderer.
            if (b instanceof com.hbm_m.block.decorations.TrinketBlock) {
                return new com.hbm_m.item.TrinketBlockItem(b, new Item.Properties());
            }
            // A panel slab's item has to be able to merge two singles into the double block.
            if (b instanceof com.hbm_m.block.generic.RBMKSlabBlock slab && !slab.isDouble) {
                return new com.hbm_m.block.generic.RBMKSlabItem(slab, new Item.Properties());
            }
            // Multiblocks: ihr Gegenstand muss vor dem Setzen pruefen, ob die ganze Struktur Platz
            // hat, und danach die Dummyzellen fuellen.
            //
            // FusionMultiblockBlock gehoerte hier von Anfang an dazu, stand aber nicht in der Liste
            // und bekam deshalb ein blankes BlockItem. Folge: MultiblockBlockItem.shiftContextToCore
            // lief nie, also landete der Kern genau auf dem angeklickten Block statt - wie
            // BlockDummyable.getOffset() im Original - um den Platzierungsoffset nach hinten
            // versetzt. Die Maschine wuchs damit immer um mehrere Bloecke verschoben aus dem
            // Klickpunkt heraus, und checkPlacement lief ebenfalls nie: die Struktur hat alles
            // ueberschrieben, was im Weg stand, statt die Platzierung abzulehnen.
            if (b instanceof com.hbm_m.block.machines.albion.PAMultiblockBlock
                    || b instanceof com.hbm_m.block.machines.DummyableMachineBlock
                    || b instanceof com.hbm_m.block.machines.fusion.FusionMultiblockBlock) {
                return new com.hbm_m.multiblock.MultiblockBlockItem(b, new Item.Properties());
            }
            // Der Orbus ist ein Fass mit Struktur - die uebrigen Faesser sind Einzelbloecke.
            if (b instanceof com.hbm_m.block.machines.BarrelTankBlock barrel
                    && barrel.getStructureHelper() != null) {
                return new com.hbm_m.multiblock.MultiblockBlockItem(b, new Item.Properties());
            }
            // Original ItemModDoor: Stapel 1, nur auf Oberseite, Scharnier wie placeDoorBlock
            if (b instanceof com.hbm_m.block.generic.BlockModDoor) {
                return new com.hbm_m.item.tool.ItemModDoor(b, new Item.Properties());
            }
            // Audit 7: Original-Itemrenderer mit Animation (builtin/entity-Itemmodell)
            if (com.hbm_m.item.AnimatedBlockItem.NAMES.contains(name)) {
                return new com.hbm_m.item.AnimatedBlockItem(b, new Item.Properties());
            }
            // Original ItemBlockLore.getRarity (Diamantkies selten, Euphemium episch)
            net.minecraft.world.item.Rarity loreRarity = com.hbm_m.item.block.ItemBlockLore.getRarity(name);
            if (loreRarity != null) return new BlockItem(b, new Item.Properties().rarity(loreRarity));
            return new BlockItem(b, new Item.Properties());
        });
    }

    public static void init() {
        BLOCKS.register();
    }

    /**
     * Links each panel slab to its counterpart. Cannot be done in the field initialisers because
     * the two halves of a pair reference each other, so it runs once after registration.
     */
    public static void linkSlabPairs() {
        pairSlabs(DECO_RBMK_PANEL_SLAB2, DECO_RBMK_PANEL_SLAB4);
        pairSlabs(DECO_RBMK_SMOOTH_PANEL_SLAB2, DECO_RBMK_SMOOTH_PANEL_SLAB4);
    }

    private static void pairSlabs(RegistrySupplier<Block> single, RegistrySupplier<Block> dbl) {
        if (single.get() instanceof com.hbm_m.block.generic.RBMKSlabBlock s) s.setCounterpart(dbl::get);
        if (dbl.get() instanceof com.hbm_m.block.generic.RBMKSlabBlock d) d.setCounterpart(single::get);
    }
}
