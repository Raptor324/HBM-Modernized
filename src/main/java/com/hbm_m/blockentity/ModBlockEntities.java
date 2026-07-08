package com.hbm_m.blockentity;
import com.hbm_m.api.energy.ConverterBlockEntity;
import com.hbm_m.api.energy.SwitchBlockEntity;
import com.hbm_m.api.energy.WireBlockEntity;
import com.hbm_m.block.ModBlocks;
import com.hbm_m.block.entity.doors.DoorBlockEntity;
import com.hbm_m.blockentity.bomb.LandMineBlockEntity;
import com.hbm_m.blockentity.bomb.NukeFatManBlockEntity;
import com.hbm_m.blockentity.crates.DeshCrateBlockEntity;
import com.hbm_m.blockentity.crates.IronCrateBlockEntity;
import com.hbm_m.blockentity.crates.SteelCrateBlockEntity;
import com.hbm_m.blockentity.crates.TemplateCrateBlockEntity;
import com.hbm_m.blockentity.crates.TungstenCrateBlockEntity;
import com.hbm_m.blockentity.machines.AnvilBlockEntity;
import com.hbm_m.blockentity.machines.BatterySocketBlockEntity;
import com.hbm_m.blockentity.machines.BlastFurnaceBlockEntity;
import com.hbm_m.blockentity.machines.DeconBlockEntity;
import com.hbm_m.blockentity.machines.FluidDuctBlockEntity;
import com.hbm_m.blockentity.machines.FluidExhaustBlockEntity;
import com.hbm_m.blockentity.machines.FluidPumpBlockEntity;
import com.hbm_m.blockentity.machines.FluidValveBlockEntity;
import com.hbm_m.blockentity.machines.GeigerCounterBlockEntity;
import com.hbm_m.blockentity.machines.HeatingOvenBlockEntity;
import com.hbm_m.blockentity.machines.LaunchPadBlockEntity;
import com.hbm_m.blockentity.machines.LaunchPadRustedBlockEntity;
import com.hbm_m.blockentity.machines.MachineAdvancedAssemblerBlockEntity;
import com.hbm_m.blockentity.machines.MachineArcWelderBlockEntity;
import com.hbm_m.blockentity.machines.MachineAssemblerBlockEntity;
import com.hbm_m.blockentity.machines.MachineBatteryBlockEntity;
import com.hbm_m.blockentity.machines.MachineBreederBlockEntity;
import com.hbm_m.blockentity.machines.MachineCatalyticReformerBlockEntity;
import com.hbm_m.blockentity.machines.MachineCentrifugeBlockEntity;
import com.hbm_m.blockentity.machines.MachineChemicalFactoryBlockEntity;
import com.hbm_m.blockentity.machines.MachineChemicalPlantBlockEntity;
import com.hbm_m.blockentity.machines.MachineCoolingTowerBlockEntity;
import com.hbm_m.blockentity.machines.MachineCoreEmitterBlockEntity;
import com.hbm_m.blockentity.machines.MachineCoreInjectorBlockEntity;
import com.hbm_m.blockentity.machines.MachineCoreReceiverBlockEntity;
import com.hbm_m.blockentity.machines.MachineCrackingTowerBlockEntity;
import com.hbm_m.blockentity.machines.MachineCrucibleBlockEntity;
import com.hbm_m.blockentity.machines.MachineCrystallizerBlockEntity;
import com.hbm_m.blockentity.machines.MachineCyclotronBlockEntity;
import com.hbm_m.blockentity.machines.MachineDerrickBlockEntity;
import com.hbm_m.blockentity.machines.MachineDeuteriumTowerBlockEntity;
import com.hbm_m.blockentity.machines.MachineFelBlockEntity;
import com.hbm_m.blockentity.machines.MachineFlareStackBlockEntity;
import com.hbm_m.blockentity.machines.MachineFluidTankBlockEntity;
import com.hbm_m.blockentity.machines.MachineFrackingTowerBlockEntity;
import com.hbm_m.blockentity.machines.MachineFractionTowerBlockEntity;
import com.hbm_m.blockentity.machines.MachineGasCentrifugeBlockEntity;
import com.hbm_m.blockentity.machines.MachineHydrotreaterBlockEntity;
import com.hbm_m.blockentity.machines.MachineIndustrialBoilerBlockEntity;
import com.hbm_m.blockentity.machines.MachineIndustrialTurbineBlockEntity;
import com.hbm_m.blockentity.machines.MachineLargePylonBlockEntity;
import com.hbm_m.blockentity.machines.MachineLiquefactorBlockEntity;
import com.hbm_m.blockentity.machines.MachineMiningDrillBlockEntity;
import com.hbm_m.blockentity.machines.MachineMixerBlockEntity;
import com.hbm_m.blockentity.machines.MachinePressBlockEntity;
import com.hbm_m.blockentity.machines.MachinePumpjackBlockEntity;
import com.hbm_m.blockentity.machines.MachineRadarBlockEntity;
import com.hbm_m.blockentity.machines.MachineRbmkConsoleBlockEntity;
import com.hbm_m.blockentity.machines.MachineRefineryBlockEntity;
import com.hbm_m.blockentity.machines.MachineShredderBlockEntity;
import com.hbm_m.blockentity.machines.MachineSilexBlockEntity;
import com.hbm_m.blockentity.machines.MachineSolarBoilerBlockEntity;
import com.hbm_m.blockentity.machines.MachineSolarMirrorsBlockEntity;
import com.hbm_m.blockentity.machines.MachineSolderingStationBlockEntity;
import com.hbm_m.blockentity.machines.MachineSteamCondenserBlockEntity;
import com.hbm_m.blockentity.machines.MachineSteamTurbineBlockEntity;
import com.hbm_m.blockentity.machines.MachineSubstationBlockEntity;
import com.hbm_m.blockentity.machines.MachineTowerSmallBlockEntity;
import com.hbm_m.blockentity.machines.MachineTurbineBlockEntity;
import com.hbm_m.blockentity.machines.MachineTurbofanBlockEntity;
import com.hbm_m.blockentity.machines.MachineVacuumDistillBlockEntity;
import com.hbm_m.blockentity.machines.MachineWatzPowerplantBlockEntity;
import com.hbm_m.blockentity.machines.MachineWoodBurnerBlockEntity;
import com.hbm_m.blockentity.machines.MachineZirnoxBlockEntity;
import com.hbm_m.blockentity.machines.MachineZirnoxDestroyedBlockEntity;
import com.hbm_m.blockentity.machines.UniversalMachinePartBlockEntity;
import com.hbm_m.blockentity.machines.rbmk.RBMKAbsorberBlockEntity;
import com.hbm_m.blockentity.machines.rbmk.RBMKAutoloaderBlockEntity;
import com.hbm_m.blockentity.machines.rbmk.RBMKBlankBlockEntity;
import com.hbm_m.blockentity.machines.rbmk.RBMKBoilerBlockEntity;
import com.hbm_m.blockentity.machines.rbmk.RBMKControlAutoBlockEntity;
import com.hbm_m.blockentity.machines.rbmk.RBMKControlManualBlockEntity;
import com.hbm_m.blockentity.machines.rbmk.RBMKCoolerBlockEntity;
import com.hbm_m.blockentity.machines.rbmk.RBMKCraneConsoleBlockEntity;
import com.hbm_m.blockentity.machines.rbmk.RBMKHeaterBlockEntity;
import com.hbm_m.blockentity.machines.rbmk.RBMKLoaderBlockEntity;
import com.hbm_m.blockentity.machines.rbmk.RBMKModeratorBlockEntity;
import com.hbm_m.blockentity.machines.rbmk.RBMKOutgasserBlockEntity;
import com.hbm_m.blockentity.machines.rbmk.RBMKPanelBlockEntity;
import com.hbm_m.blockentity.machines.rbmk.RBMKReflectorBlockEntity;
import com.hbm_m.blockentity.machines.rbmk.RBMKRodBlockEntity;
import com.hbm_m.blockentity.machines.rbmk.RBMKSteamInletBlockEntity;
import com.hbm_m.blockentity.machines.rbmk.RBMKSteamOutletBlockEntity;
import com.hbm_m.blockentity.machines.rbmk.RBMKStorageBlockEntity;
import com.hbm_m.lib.RefStrings;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;


public class ModBlockEntities {

	public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
			DeferredRegister.create(RefStrings.MODID, Registries.BLOCK_ENTITY_TYPE);

	public static final RegistrySupplier<BlockEntityType<MachineCrystallizerBlockEntity>> CRYSTALLIZER =
		BLOCK_ENTITIES.register("crystallizer", () ->
			BlockEntityType.Builder.of(MachineCrystallizerBlockEntity::new, ModBlocks.CRYSTALLIZER.get())
				.build(null));

	public static final RegistrySupplier<BlockEntityType<MachineFrackingTowerBlockEntity>> HYDRAULIC_FRACKINING_TOWER_BE =
		BLOCK_ENTITIES.register("hydraulic_frackining_tower_be", () ->
            BlockEntityType.Builder.of(MachineFrackingTowerBlockEntity::new, ModBlocks.HYDRAULIC_FRACKINING_TOWER.get())
                .build(null));

    public static final RegistrySupplier<BlockEntityType<MachineBreederBlockEntity>> BREEDER_BE =
            BLOCK_ENTITIES.register("breeder", () ->
                    BlockEntityType.Builder.of(MachineBreederBlockEntity::new, ModBlocks.BREEDER.get()).build(null));

    public static final RegistrySupplier<BlockEntityType<MachineLargePylonBlockEntity>> LARGE_PYLON_BE =
            BLOCK_ENTITIES.register("large_pylon", () ->
                    BlockEntityType.Builder.of(MachineLargePylonBlockEntity::new, ModBlocks.LARGE_PYLON.get()).build(null));

    public static final RegistrySupplier<BlockEntityType<MachineCoolingTowerBlockEntity>> COOLING_TOWER_BE =
            BLOCK_ENTITIES.register("cooling_tower_be", () ->
                    BlockEntityType.Builder.of(MachineCoolingTowerBlockEntity::new, ModBlocks.COOLING_TOWER.get()).build(null));

    public static final RegistrySupplier<BlockEntityType<MachineTowerSmallBlockEntity>> TOWER_SMALL_BE =
            BLOCK_ENTITIES.register("tower_small_be", () ->
                    BlockEntityType.Builder.of(MachineTowerSmallBlockEntity::new, ModBlocks.TOWER_SMALL.get()).build(null));

    public static final RegistrySupplier<BlockEntityType<MachineCyclotronBlockEntity>> CYCLOTRON_BE =
            BLOCK_ENTITIES.register("cyclotron_be", () ->
                    BlockEntityType.Builder.of(MachineCyclotronBlockEntity::new, ModBlocks.CYCLOTRON.get()).build(null));

    public static final RegistrySupplier<BlockEntityType<MachineZirnoxBlockEntity>> ZIRNOX_BE =
            BLOCK_ENTITIES.register("zirnox_be", () ->
                    BlockEntityType.Builder.of(MachineZirnoxBlockEntity::new, ModBlocks.ZIRNOX.get()).build(null));

    public static final RegistrySupplier<BlockEntityType<MachineZirnoxDestroyedBlockEntity>> ZIRNOX_DESTROYED_BE =
            BLOCK_ENTITIES.register("zirnox_destroyed_be", () ->
                    BlockEntityType.Builder.of(MachineZirnoxDestroyedBlockEntity::new, ModBlocks.ZIRNOX_DESTROYED.get()).build(null));

    public static final RegistrySupplier<BlockEntityType<MachineArcWelderBlockEntity>> ARC_WELDER_BE =
            BLOCK_ENTITIES.register("arc_welder_be", () ->
                    BlockEntityType.Builder.of(MachineArcWelderBlockEntity::new, ModBlocks.ARC_WELDER.get()).build(null));

    public static final RegistrySupplier<BlockEntityType<MachineSolderingStationBlockEntity>> SOLDERING_STATION_BE =
            BLOCK_ENTITIES.register("soldering_station_be", () ->
                    BlockEntityType.Builder.of(MachineSolderingStationBlockEntity::new, ModBlocks.SOLDERING_STATION.get()).build(null));

    public static final RegistrySupplier<BlockEntityType<MachineMixerBlockEntity>> MIXER_BE =
            BLOCK_ENTITIES.register("mixer_be", () ->
                    BlockEntityType.Builder.of(MachineMixerBlockEntity::new, ModBlocks.MIXER.get()).build(null));

    public static final RegistrySupplier<BlockEntityType<MachineDerrickBlockEntity>> DERRICK_BE =
            BLOCK_ENTITIES.register("derrick_be", () ->
                    BlockEntityType.Builder.of(MachineDerrickBlockEntity::new, ModBlocks.DERRICK.get()).build(null));

    public static final RegistrySupplier<BlockEntityType<MachineRbmkConsoleBlockEntity>> RBMK_CONSOLE_BE =
            BLOCK_ENTITIES.register("rbmk_console_be", () ->
                    BlockEntityType.Builder.of(MachineRbmkConsoleBlockEntity::new, ModBlocks.RBMK_CONSOLE.get()).build(null));

    public static final RegistrySupplier<BlockEntityType<MachineFlareStackBlockEntity>> FLARE_STACK_BE =
            BLOCK_ENTITIES.register("flare_stack_be", () ->
                    BlockEntityType.Builder.of(MachineFlareStackBlockEntity::new, ModBlocks.FLARE_STACK.get()).build(null));

    public static final RegistrySupplier<BlockEntityType<MachinePumpjackBlockEntity>> PUMPJACK_BE =
            BLOCK_ENTITIES.register("pumpjack_be", () ->
                    BlockEntityType.Builder.of(MachinePumpjackBlockEntity::new, ModBlocks.PUMPJACK.get()).build(null));

    public static final RegistrySupplier<BlockEntityType<MachineRadarBlockEntity>> RADAR_BE =
            BLOCK_ENTITIES.register("radar_be", () ->
                    BlockEntityType.Builder.of(MachineRadarBlockEntity::new, ModBlocks.RADAR.get(), ModBlocks.LARGE_RADAR.get()).build(null));

    public static final RegistrySupplier<BlockEntityType<MachineCrackingTowerBlockEntity>> CRACKING_TOWER_BE =
            BLOCK_ENTITIES.register("cracking_tower_be", () ->
                    BlockEntityType.Builder.of(MachineCrackingTowerBlockEntity::new, ModBlocks.CRACKING_TOWER.get()).build(null));

    public static final RegistrySupplier<BlockEntityType<MachineFractionTowerBlockEntity>> FRACTION_TOWER_BE =
            BLOCK_ENTITIES.register("fraction_tower_be", () ->
                    BlockEntityType.Builder.of(MachineFractionTowerBlockEntity::new, ModBlocks.FRACTION_TOWER.get()).build(null));

    public static final RegistrySupplier<BlockEntityType<MachineMiningDrillBlockEntity>> MINING_DRILL_BE =
            BLOCK_ENTITIES.register("mining_drill_be", () ->
                    BlockEntityType.Builder.of(MachineMiningDrillBlockEntity::new, ModBlocks.MINING_DRILL.get()).build(null));

    public static final RegistrySupplier<BlockEntityType<MachineFelBlockEntity>> FEL_BE =
            BLOCK_ENTITIES.register("fel_be", () ->
                    BlockEntityType.Builder.of(MachineFelBlockEntity::new, ModBlocks.FEL.get()).build(null));

    public static final RegistrySupplier<BlockEntityType<MachineSilexBlockEntity>> SILEX_BE =
            BLOCK_ENTITIES.register("silex_be", () ->
                    BlockEntityType.Builder.of(MachineSilexBlockEntity::new, ModBlocks.SILEX.get()).build(null));

    public static final RegistrySupplier<BlockEntityType<GeigerCounterBlockEntity>> GEIGER_COUNTER_BE =
		BLOCK_ENTITIES.register("geiger_counter_be", () ->
			BlockEntityType.Builder.<GeigerCounterBlockEntity>of(GeigerCounterBlockEntity::new, ModBlocks.GEIGER_COUNTER_BLOCK.get())
				.build(null));

    public static final RegistrySupplier<BlockEntityType<DeconBlockEntity>> DECON_BE =
            BLOCK_ENTITIES.register("decon_be", () ->
                    BlockEntityType.Builder.of(DeconBlockEntity::new, ModBlocks.DECON.get()).build(null));

    public static final RegistrySupplier<BlockEntityType<MachineAssemblerBlockEntity>> MACHINE_ASSEMBLER_BE =
		BLOCK_ENTITIES.register("machine_assembler_be", () ->
			BlockEntityType.Builder.<MachineAssemblerBlockEntity>of(MachineAssemblerBlockEntity::new, ModBlocks.MACHINE_ASSEMBLER.get())
				.build(null));

    public static final RegistrySupplier<BlockEntityType<MachineAdvancedAssemblerBlockEntity>> ADVANCED_ASSEMBLY_MACHINE_BE =
		BLOCK_ENTITIES.register("advanced_assembly_machine_be", () ->
			BlockEntityType.Builder.<MachineAdvancedAssemblerBlockEntity>of(MachineAdvancedAssemblerBlockEntity::new, ModBlocks.ADVANCED_ASSEMBLY_MACHINE.get())
				.build(null));

    public static final RegistrySupplier<BlockEntityType<MachineBatteryBlockEntity>> MACHINE_BATTERY_BE =
            BLOCK_ENTITIES.register("machine_battery_be", () -> {
                // Превращаем список RegistrySupplier в массив Block[]
                Block[] validBlocks = ModBlocks.BATTERY_BLOCKS.stream()
                        .map(RegistrySupplier::get)
                        .toArray(Block[]::new);

                return BlockEntityType.Builder.<MachineBatteryBlockEntity>of(MachineBatteryBlockEntity::new, validBlocks)
                        .build(null);
            });

    public static final RegistrySupplier<BlockEntityType<AnvilBlockEntity>> ANVIL_BE =
        BLOCK_ENTITIES.register("anvil_be", () ->
            BlockEntityType.Builder.<AnvilBlockEntity>of(AnvilBlockEntity::new,
                    ModBlocks.ANVIL_IRON.get(),
                    ModBlocks.ANVIL_LEAD.get(),
                    ModBlocks.ANVIL_STEEL.get(),
                    ModBlocks.ANVIL_DESH.get(),
                    ModBlocks.ANVIL_FERROURANIUM.get(),
                    ModBlocks.ANVIL_SATURNITE.get(),
                    ModBlocks.ANVIL_BISMUTH_BRONZE.get(),
                    ModBlocks.ANVIL_ARSENIC_BRONZE.get(),
                    ModBlocks.ANVIL_SCHRABIDATE.get(),
                    ModBlocks.ANVIL_DNT.get(),
                    ModBlocks.ANVIL_OSMIRIDIUM.get(),
                    ModBlocks.ANVIL_MURKY.get())
                .build(null));

    public static final RegistrySupplier<BlockEntityType<LandMineBlockEntity>> LANDMINE_BE =
            BLOCK_ENTITIES.register("landmine_be", () ->
                    BlockEntityType.Builder.of(LandMineBlockEntity::new,
                            ModBlocks.MINE_AP.get(),
                            ModBlocks.MINE_FAT.get(),
                            ModBlocks.NAVAL_MINE.get())
                            .build(null)
            );

    public static final RegistrySupplier<BlockEntityType<NukeFatManBlockEntity>> NUKE_FAT_MAN_BE =
            BLOCK_ENTITIES.register("nuke_fat_man_be", () ->
                    BlockEntityType.Builder.of(NukeFatManBlockEntity::new, ModBlocks.NUKE_FAT_MAN.get())
                            .build(null));

    public static final RegistrySupplier<BlockEntityType<com.hbm_m.blockentity.bomb.NukePrototypeBlockEntity>> NUKE_PROTOTYPE_BE =
            BLOCK_ENTITIES.register("nuke_prototype_be", () ->
                    BlockEntityType.Builder.of(com.hbm_m.blockentity.bomb.NukePrototypeBlockEntity::new, ModBlocks.NUKE_PROTOTYPE.get())
                            .build(null));

    public static final RegistrySupplier<BlockEntityType<MachineShredderBlockEntity>> SHREDDER =
            BLOCK_ENTITIES.register("shredder", () ->
                    BlockEntityType.Builder.of(MachineShredderBlockEntity::new,
                            ModBlocks.SHREDDER.get()).build(null));

    public static final RegistrySupplier<BlockEntityType<MachineCentrifugeBlockEntity>> CENTRIFUGE_BE =
            BLOCK_ENTITIES.register("centrifuge_be", () ->
                    BlockEntityType.Builder.of(MachineCentrifugeBlockEntity::new,
                            ModBlocks.CENTRIFUGE.get()).build(null));

    public static final RegistrySupplier<BlockEntityType<UniversalMachinePartBlockEntity>> UNIVERSAL_MACHINE_PART_BE =
        BLOCK_ENTITIES.register("universal_machine_part_be", () ->
			BlockEntityType.Builder.<UniversalMachinePartBlockEntity>of(UniversalMachinePartBlockEntity::new, ModBlocks.UNIVERSAL_MACHINE_PART.get())
				.build(null));

	public static final RegistrySupplier<BlockEntityType<WireBlockEntity>> WIRE_BE =
		BLOCK_ENTITIES.register("wire_be", () ->
			BlockEntityType.Builder.<WireBlockEntity>of(WireBlockEntity::new, ModBlocks.WIRE_COATED.get())
				.build(null));

	public static final RegistrySupplier<BlockEntityType<LaunchPadBlockEntity>> LAUNCH_PAD_BE =
		BLOCK_ENTITIES.register("launch_pad_be", () ->
			BlockEntityType.Builder.<LaunchPadBlockEntity>of(LaunchPadBlockEntity::new, ModBlocks.LAUNCH_PAD.get())
				.build(null));

	public static final RegistrySupplier<BlockEntityType<LaunchPadRustedBlockEntity>> LAUNCH_PAD_RUSTED_BE =
		BLOCK_ENTITIES.register("launch_pad_rusted_be", () ->
			BlockEntityType.Builder.<LaunchPadRustedBlockEntity>of(LaunchPadRustedBlockEntity::new, ModBlocks.LAUNCH_PAD_RUSTED.get())
				.build(null));



    public static final RegistrySupplier<BlockEntityType<SwitchBlockEntity>> SWITCH_BE =
            BLOCK_ENTITIES.register("switch_be", () ->
                    BlockEntityType.Builder.of(SwitchBlockEntity::new, ModBlocks.SWITCH.get())
                            .build(null));

	public static final RegistrySupplier<BlockEntityType<BlastFurnaceBlockEntity>> BLAST_FURNACE_BE =
			BLOCK_ENTITIES.register("blast_furnace_be", () ->
					BlockEntityType.Builder.of(BlastFurnaceBlockEntity::new,
							ModBlocks.BLAST_FURNACE.get()).build(null));

	public static final RegistrySupplier<BlockEntityType<MachinePressBlockEntity>> PRESS_BE =
			BLOCK_ENTITIES.register("press_be", () ->
					BlockEntityType.Builder.of(MachinePressBlockEntity::new,
							ModBlocks.PRESS.get()).build(null));

	public static final RegistrySupplier<BlockEntityType<MachineWoodBurnerBlockEntity>> WOOD_BURNER_BE =
			BLOCK_ENTITIES.register("wood_burner_be", () ->
					BlockEntityType.Builder.of(MachineWoodBurnerBlockEntity::new,
							ModBlocks.WOOD_BURNER.get()).build(null));

            public static final RegistrySupplier<BlockEntityType<MachineChemicalPlantBlockEntity>> CHEMICAL_PLANT_BE =
                    BLOCK_ENTITIES.register("chemical_plant_be", () ->
                            BlockEntityType.Builder.of(MachineChemicalPlantBlockEntity::new,
                                    ModBlocks.CHEMICAL_PLANT.get()).build(null));

    public static final RegistrySupplier<BlockEntityType<MachineGasCentrifugeBlockEntity>> GAS_CENTRIFUGE_BE =
            BLOCK_ENTITIES.register("gas_centrifuge_be", () ->
                    BlockEntityType.Builder.of(MachineGasCentrifugeBlockEntity::new,
                            ModBlocks.GAS_CENTRIFUGE.get()).build(null));

    public static final RegistrySupplier<BlockEntityType<MachineCrucibleBlockEntity>> CRUCIBLE_BE =
            BLOCK_ENTITIES.register("crucible_be", () ->
                    BlockEntityType.Builder.of(MachineCrucibleBlockEntity::new,
                            ModBlocks.CRUCIBLE.get()).build(null));

    public static final RegistrySupplier<BlockEntityType<com.hbm_m.blockentity.machines.MachineFoundryBasinBlockEntity>> FOUNDRY_BASIN_BE =
            BLOCK_ENTITIES.register("foundry_basin_be", () ->
                    BlockEntityType.Builder.of(com.hbm_m.blockentity.machines.MachineFoundryBasinBlockEntity::new,
                            ModBlocks.FOUNDRY_BASIN.get()).build(null));

    public static final RegistrySupplier<BlockEntityType<com.hbm_m.blockentity.machines.MachineFoundryChannelBlockEntity>> FOUNDRY_CHANNEL_BE =
            BLOCK_ENTITIES.register("foundry_channel_be", () ->
                    BlockEntityType.Builder.of(com.hbm_m.blockentity.machines.MachineFoundryChannelBlockEntity::new,
                            ModBlocks.FOUNDRY_CHANNEL.get()).build(null));

    public static final RegistrySupplier<BlockEntityType<com.hbm_m.blockentity.machines.SU47TrophyBlockEntity>> SU47_TROPHY_BE =
            BLOCK_ENTITIES.register("su47_trophy_be", () ->
                    BlockEntityType.Builder.of(com.hbm_m.blockentity.machines.SU47TrophyBlockEntity::new,
                            ModBlocks.SU47_TROPHY.get()).build(null));

    public static final RegistrySupplier<BlockEntityType<com.hbm_m.blockentity.machines.MachineFoundryOutletBlockEntity>> FOUNDRY_OUTLET_BE =
            BLOCK_ENTITIES.register("foundry_outlet_be", () ->
                    BlockEntityType.Builder.of(com.hbm_m.blockentity.machines.MachineFoundryOutletBlockEntity::new,
                            ModBlocks.FOUNDRY_OUTLET.get()).build(null));

    public static final RegistrySupplier<BlockEntityType<MachineFluidTankBlockEntity>> FLUID_TANK_BE =
            BLOCK_ENTITIES.register("fluid_tank_be", () ->
                    BlockEntityType.Builder.of(MachineFluidTankBlockEntity::new,
                            ModBlocks.FLUID_TANK.get()).build(null));

    public static final RegistrySupplier<BlockEntityType<BatterySocketBlockEntity>> BATTERY_SOCKET_BE =
            BLOCK_ENTITIES.register("battery_socket_be", () ->
                    BlockEntityType.Builder.of(BatterySocketBlockEntity::new,
                            ModBlocks.MACHINE_BATTERY_SOCKET.get()).build(null));

    public static final RegistrySupplier<BlockEntityType<MachineIndustrialBoilerBlockEntity>> INDUSTRIAL_BOILER_BE =
            BLOCK_ENTITIES.register("industrial_boiler_be", () ->
                    BlockEntityType.Builder.of(MachineIndustrialBoilerBlockEntity::new,
                            ModBlocks.INDUSTRIAL_BOILER.get()).build(null));

    public static final RegistrySupplier<BlockEntityType<MachineSolarBoilerBlockEntity>> SOLAR_BOILER_BE =
            BLOCK_ENTITIES.register("solar_boiler_be", () ->
                    BlockEntityType.Builder.of(MachineSolarBoilerBlockEntity::new,
                            ModBlocks.SOLAR_BOILER.get()).build(null));

    public static final RegistrySupplier<BlockEntityType<MachineSolarMirrorsBlockEntity>> SOLAR_MIRRORS_BE =
            BLOCK_ENTITIES.register("solar_mirrors_be", () ->
                    BlockEntityType.Builder.of(MachineSolarMirrorsBlockEntity::new,
                            ModBlocks.SOLAR_MIRRORS.get()).build(null));

    public static final RegistrySupplier<BlockEntityType<MachineWatzPowerplantBlockEntity>> WATZ_POWERPLANT_BE =
            BLOCK_ENTITIES.register("watz_powerplant_be", () ->
                    BlockEntityType.Builder.of(MachineWatzPowerplantBlockEntity::new,
                            ModBlocks.WATZ_POWERPLANT.get()).build(null));

    public static final RegistrySupplier<BlockEntityType<MachineHydrotreaterBlockEntity>> HYDROTREATER_BE =
            BLOCK_ENTITIES.register("hydrotreater_be", () ->
                    BlockEntityType.Builder.of(MachineHydrotreaterBlockEntity::new,
                            ModBlocks.HYDROTREATER.get()).build(null));

    public static final RegistrySupplier<BlockEntityType<MachineCatalyticReformerBlockEntity>> CATALYTIC_REFORMER_BE =
            BLOCK_ENTITIES.register("catalytic_reformer_be", () ->
                    BlockEntityType.Builder.of(MachineCatalyticReformerBlockEntity::new,
                            ModBlocks.CATALYTIC_REFORMER.get()).build(null));

    public static final RegistrySupplier<BlockEntityType<MachineDeuteriumTowerBlockEntity>> DEUTERIUM_TOWER_BE =
            BLOCK_ENTITIES.register("deuterium_tower_be", () ->
                    BlockEntityType.Builder.of(MachineDeuteriumTowerBlockEntity::new,
                            ModBlocks.DEUTERIUM_TOWER.get()).build(null));

    public static final RegistrySupplier<BlockEntityType<MachineChemicalFactoryBlockEntity>> CHEMICAL_FACTORY_BE =
            BLOCK_ENTITIES.register("chemical_factory_be", () ->
                    BlockEntityType.Builder.of(MachineChemicalFactoryBlockEntity::new,
                            ModBlocks.CHEMICAL_FACTORY.get()).build(null));

    public static final RegistrySupplier<BlockEntityType<MachineSteamTurbineBlockEntity>> STEAM_TURBINE_BE =
            BLOCK_ENTITIES.register("steam_turbine_be", () ->
                    BlockEntityType.Builder.of(MachineSteamTurbineBlockEntity::new,
                            ModBlocks.STEAM_TURBINE.get()).build(null));

    public static final RegistrySupplier<BlockEntityType<MachineSteamCondenserBlockEntity>> STEAM_CONDENSER_BE =
            BLOCK_ENTITIES.register("steam_condenser_be", () ->
                    BlockEntityType.Builder.of(MachineSteamCondenserBlockEntity::new,
                            ModBlocks.STEAM_CONDENSER.get()).build(null));

    public static final RegistrySupplier<BlockEntityType<MachineLiquefactorBlockEntity>> LIQUEFACTOR_BE =
            BLOCK_ENTITIES.register("liquefactor_be", () ->
                    BlockEntityType.Builder.of(MachineLiquefactorBlockEntity::new,
                            ModBlocks.LIQUEFACTOR.get()).build(null));

    public static final RegistrySupplier<BlockEntityType<MachineCoreEmitterBlockEntity>> CORE_EMITTER_BE =
            BLOCK_ENTITIES.register("core_emitter_be", () ->
                    BlockEntityType.Builder.of(MachineCoreEmitterBlockEntity::new,
                            ModBlocks.CORE_EMITTER.get()).build(null));

    public static final RegistrySupplier<BlockEntityType<MachineCoreInjectorBlockEntity>> CORE_INJECTOR_BE =
            BLOCK_ENTITIES.register("core_injector_be", () ->
                    BlockEntityType.Builder.of(MachineCoreInjectorBlockEntity::new,
                            ModBlocks.CORE_INJECTOR.get()).build(null));

    public static final RegistrySupplier<BlockEntityType<MachineCoreReceiverBlockEntity>> CORE_RECEIVER_BE =
            BLOCK_ENTITIES.register("core_receiver_be", () ->
                    BlockEntityType.Builder.of(MachineCoreReceiverBlockEntity::new,
                            ModBlocks.CORE_RECEIVER.get()).build(null));

    public static final RegistrySupplier<BlockEntityType<MachineVacuumDistillBlockEntity>> VACUUM_DISTILL_BE =
            BLOCK_ENTITIES.register("vacuum_distill_be", () ->
                    BlockEntityType.Builder.of(MachineVacuumDistillBlockEntity::new,
                            ModBlocks.VACUUM_DISTILL.get()).build(null));

    public static final RegistrySupplier<BlockEntityType<MachineTurbofanBlockEntity>> TURBOFAN_BE =
            BLOCK_ENTITIES.register("turbofan_be", () ->
                    BlockEntityType.Builder.of(MachineTurbofanBlockEntity::new,
                            ModBlocks.TURBOFAN.get()).build(null));

    public static final RegistrySupplier<BlockEntityType<MachineRefineryBlockEntity>> REFINERY_BE =
            BLOCK_ENTITIES.register("refinery_be", () ->
                    BlockEntityType.Builder.of(MachineRefineryBlockEntity::new,
                            ModBlocks.REFINERY.get()).build(null));

    public static final RegistrySupplier<BlockEntityType<MachineIndustrialTurbineBlockEntity>> INDUSTRIAL_TURBINE_BE =
            BLOCK_ENTITIES.register("industrial_turbine_be", () ->
                    BlockEntityType.Builder.of(MachineIndustrialTurbineBlockEntity::new,
                            ModBlocks.INDUSTRIAL_TURBINE.get()).build(null));

    public static final RegistrySupplier<BlockEntityType<MachineTurbineBlockEntity>> TURBINE_BE =
            BLOCK_ENTITIES.register("turbine_be", () ->
                    BlockEntityType.Builder.of(MachineTurbineBlockEntity::new,
                            ModBlocks.TURBINE.get()).build(null));

    public static final RegistrySupplier<BlockEntityType<MachineSubstationBlockEntity>> SUBSTATION_BE =
            BLOCK_ENTITIES.register("substation_be", () ->
                    BlockEntityType.Builder.of(MachineSubstationBlockEntity::new,
                            ModBlocks.SUBSTATION.get()).build(null));

    // ДВЕРИ

    public static final RegistrySupplier<BlockEntityType<DoorBlockEntity>> DOOR_ENTITY =
        BLOCK_ENTITIES.register("door", () -> 
                BlockEntityType.Builder.of(DoorBlockEntity::new,
                // Все блоки дверей, которые используют этот BlockEntity
                        ModBlocks.LARGE_VEHICLE_DOOR.get(),
                        ModBlocks.ROUND_AIRLOCK_DOOR.get(),
                        ModBlocks.TRANSITION_SEAL.get(),
                        ModBlocks.FIRE_DOOR.get(),
                        ModBlocks.SLIDE_DOOR.get(),
                        ModBlocks.SLIDING_SEAL_DOOR.get(),
                        ModBlocks.SECURE_ACCESS_DOOR.get(),
                        ModBlocks.QE_SLIDING.get(),
                        ModBlocks.QE_CONTAINMENT.get(),
                        ModBlocks.WATER_DOOR.get(),
                        ModBlocks.SILO_HATCH.get(),
                        ModBlocks.SILO_HATCH_LARGE.get(),
                        ModBlocks.VAULT_DOOR.get())
                    .build(null));

    public static final RegistrySupplier<BlockEntityType<IronCrateBlockEntity>> IRON_CRATE_BE =
            BLOCK_ENTITIES.register("iron_crate_be", () ->
                    BlockEntityType.Builder.<IronCrateBlockEntity>of(
                            IronCrateBlockEntity::new,
                            ModBlocks.CRATE_IRON.get()
                    ).build(null));

    public static final RegistrySupplier<BlockEntityType<SteelCrateBlockEntity>> STEEL_CRATE_BE =
            BLOCK_ENTITIES.register("steel_crate_be", () ->
                    BlockEntityType.Builder.<SteelCrateBlockEntity>of(
                            SteelCrateBlockEntity::new,
                            ModBlocks.CRATE_STEEL.get()
                    ).build(null));
    public static final RegistrySupplier<BlockEntityType<DeshCrateBlockEntity>> DESH_CRATE_BE =
            BLOCK_ENTITIES.register("desh_crate_be", () ->
                    BlockEntityType.Builder.<DeshCrateBlockEntity>of(
                            DeshCrateBlockEntity::new,
                            ModBlocks.CRATE_DESH.get()
                    ).build(null));

    public static final RegistrySupplier<BlockEntityType<TungstenCrateBlockEntity>> TUNGSTEN_CRATE_BE =
            BLOCK_ENTITIES.register("tungsten_crate_be", () ->
                    BlockEntityType.Builder.<TungstenCrateBlockEntity>of(
                            TungstenCrateBlockEntity::new,
                            ModBlocks.CRATE_TUNGSTEN.get()
                    ).build(null));

    public static final RegistrySupplier<BlockEntityType<TemplateCrateBlockEntity>> TEMPLATE_CRATE_BE =
            BLOCK_ENTITIES.register("template_crate_be", () ->
                    BlockEntityType.Builder.<TemplateCrateBlockEntity>of(
                            TemplateCrateBlockEntity::new,
                            ModBlocks.CRATE_TEMPLATE.get()
                    ).build(null));

    public static final RegistrySupplier<BlockEntityType<ConverterBlockEntity>> CONVERTER_BE =
            BLOCK_ENTITIES.register("converter_be",
                    () -> BlockEntityType.Builder.of(ConverterBlockEntity::new, ModBlocks.CONVERTER_BLOCK.get()).build(null));

    public static final RegistrySupplier<BlockEntityType<HeatingOvenBlockEntity>> HEATING_OVEN_BE =
            BLOCK_ENTITIES.register("heating_oven_be", () ->
                    BlockEntityType.Builder.of(HeatingOvenBlockEntity::new,
                            ModBlocks.HEATING_OVEN.get()).build(null));

    public static final RegistrySupplier<BlockEntityType<FluidDuctBlockEntity>> FLUID_DUCT_BE =
            BLOCK_ENTITIES.register("fluid_duct_be", () ->
                    BlockEntityType.Builder.of(FluidDuctBlockEntity::new,
                            ModBlocks.FLUID_DUCT.get(),
                            ModBlocks.FLUID_DUCT_COLORED.get(),
                            ModBlocks.FLUID_DUCT_SILVER.get(),
                            ModBlocks.OIL_PIPE.get()).build(null));

    public static final RegistrySupplier<BlockEntityType<FluidValveBlockEntity>> FLUID_VALVE_BE =
            BLOCK_ENTITIES.register("fluid_valve_be", () ->
                    BlockEntityType.Builder.of(FluidValveBlockEntity::new,
                            ModBlocks.FLUID_VALVE.get()).build(null));

    public static final RegistrySupplier<BlockEntityType<FluidPumpBlockEntity>> FLUID_PUMP_BE =
            BLOCK_ENTITIES.register("fluid_pump_be", () ->
                    BlockEntityType.Builder.of(FluidPumpBlockEntity::new,
                            ModBlocks.FLUID_PUMP.get()).build(null));

    public static final RegistrySupplier<BlockEntityType<FluidExhaustBlockEntity>> FLUID_EXHAUST_BE =
            BLOCK_ENTITIES.register("fluid_exhaust_be", () ->
                    BlockEntityType.Builder.of(FluidExhaustBlockEntity::new,
                            ModBlocks.FLUID_EXHAUST.get()).build(null));

    // ─── RBMK Columns ────────────────────────────────────────────────────────

    public static final RegistrySupplier<BlockEntityType<RBMKRodBlockEntity>> RBMK_ROD_BE =
            BLOCK_ENTITIES.register("rbmk_rod_be", () ->
                    BlockEntityType.Builder.of(RBMKRodBlockEntity::new,
                            ModBlocks.RBMK_ROD.get(), ModBlocks.RBMK_ROD_MOD.get()).build(null));

    public static final RegistrySupplier<BlockEntityType<RBMKControlManualBlockEntity>> RBMK_CONTROL_BE =
            BLOCK_ENTITIES.register("rbmk_control_be", () ->
                    BlockEntityType.Builder.of(RBMKControlManualBlockEntity::new,
                            ModBlocks.RBMK_CONTROL.get(), ModBlocks.RBMK_CONTROL_BLUE.get(),
                            ModBlocks.RBMK_CONTROL_GREEN.get(), ModBlocks.RBMK_CONTROL_YELLOW.get(),
                            ModBlocks.RBMK_CONTROL_PURPLE.get(), ModBlocks.RBMK_CONTROL_MOD.get(),
                            ModBlocks.RBMK_CONTROL_REASIM.get()).build(null));

    public static final RegistrySupplier<BlockEntityType<RBMKControlAutoBlockEntity>> RBMK_CONTROL_AUTO_BE =
            BLOCK_ENTITIES.register("rbmk_control_auto_be", () ->
                    BlockEntityType.Builder.of(RBMKControlAutoBlockEntity::new,
                            ModBlocks.RBMK_CONTROL_AUTO.get(), ModBlocks.RBMK_CONTROL_MOD_AUTO.get(),
                            ModBlocks.RBMK_CONTROL_REASIM_AUTO.get()).build(null));

    public static final RegistrySupplier<BlockEntityType<RBMKModeratorBlockEntity>> RBMK_MODERATOR_BE =
            BLOCK_ENTITIES.register("rbmk_moderator_be", () ->
                    BlockEntityType.Builder.of(RBMKModeratorBlockEntity::new,
                            ModBlocks.RBMK_MODERATOR.get()).build(null));

    public static final RegistrySupplier<BlockEntityType<RBMKAbsorberBlockEntity>> RBMK_ABSORBER_BE =
            BLOCK_ENTITIES.register("rbmk_absorber_be", () ->
                    BlockEntityType.Builder.of(RBMKAbsorberBlockEntity::new,
                            ModBlocks.RBMK_ABSORBER.get()).build(null));

    public static final RegistrySupplier<BlockEntityType<RBMKReflectorBlockEntity>> RBMK_REFLECTOR_BE =
            BLOCK_ENTITIES.register("rbmk_reflector_be", () ->
                    BlockEntityType.Builder.of(RBMKReflectorBlockEntity::new,
                            ModBlocks.RBMK_REFLECTOR.get()).build(null));

    public static final RegistrySupplier<BlockEntityType<RBMKCoolerBlockEntity>> RBMK_COOLER_BE =
            BLOCK_ENTITIES.register("rbmk_cooler_be", () ->
                    BlockEntityType.Builder.of(RBMKCoolerBlockEntity::new,
                            ModBlocks.RBMK_COOLER.get()).build(null));

    public static final RegistrySupplier<BlockEntityType<RBMKBoilerBlockEntity>> RBMK_BOILER_BE =
            BLOCK_ENTITIES.register("rbmk_boiler_be", () ->
                    BlockEntityType.Builder.of(RBMKBoilerBlockEntity::new,
                            ModBlocks.RBMK_BOILER.get()).build(null));

    public static final RegistrySupplier<BlockEntityType<RBMKHeaterBlockEntity>> RBMK_HEATER_BE =
            BLOCK_ENTITIES.register("rbmk_heater_be", () ->
                    BlockEntityType.Builder.of(RBMKHeaterBlockEntity::new,
                            ModBlocks.RBMK_HEATER.get()).build(null));

    public static final RegistrySupplier<BlockEntityType<RBMKOutgasserBlockEntity>> RBMK_OUTGASSER_BE =
            BLOCK_ENTITIES.register("rbmk_outgasser_be", () ->
                    BlockEntityType.Builder.of(RBMKOutgasserBlockEntity::new,
                            ModBlocks.RBMK_OUTGASSER.get()).build(null));

    public static final RegistrySupplier<BlockEntityType<RBMKStorageBlockEntity>> RBMK_STORAGE_BE =
            BLOCK_ENTITIES.register("rbmk_storage_be", () ->
                    BlockEntityType.Builder.of(RBMKStorageBlockEntity::new,
                            ModBlocks.RBMK_STORAGE.get()).build(null));

    public static final RegistrySupplier<BlockEntityType<RBMKBlankBlockEntity>> RBMK_BLANK_BE =
            BLOCK_ENTITIES.register("rbmk_blank_be", () ->
                    BlockEntityType.Builder.of(RBMKBlankBlockEntity::new,
                            ModBlocks.RBMK_BLANK.get()).build(null));

    public static final RegistrySupplier<BlockEntityType<RBMKSteamInletBlockEntity>> RBMK_STEAM_INLET_BE =
            BLOCK_ENTITIES.register("rbmk_steam_inlet_be", () ->
                    BlockEntityType.Builder.of(RBMKSteamInletBlockEntity::new,
                            ModBlocks.RBMK_STEAM_INLET.get()).build(null));

    public static final RegistrySupplier<BlockEntityType<RBMKSteamOutletBlockEntity>> RBMK_STEAM_OUTLET_BE =
            BLOCK_ENTITIES.register("rbmk_steam_outlet_be", () ->
                    BlockEntityType.Builder.of(RBMKSteamOutletBlockEntity::new,
                            ModBlocks.RBMK_STEAM_OUTLET.get()).build(null));

    public static final RegistrySupplier<BlockEntityType<RBMKLoaderBlockEntity>> RBMK_LOADER_BE =
            BLOCK_ENTITIES.register("rbmk_loader_be", () ->
                    BlockEntityType.Builder.of(RBMKLoaderBlockEntity::new,
                            ModBlocks.RBMK_LOADER.get()).build(null));

    public static final RegistrySupplier<BlockEntityType<RBMKAutoloaderBlockEntity>> RBMK_AUTOLOADER_BE =
            BLOCK_ENTITIES.register("rbmk_autoloader_be", () ->
                    BlockEntityType.Builder.of(RBMKAutoloaderBlockEntity::new,
                            ModBlocks.RBMK_AUTOLOADER.get()).build(null));

    public static final RegistrySupplier<BlockEntityType<RBMKCraneConsoleBlockEntity>> RBMK_CRANE_CONSOLE_BE =
            BLOCK_ENTITIES.register("rbmk_crane_console_be", () ->
                    BlockEntityType.Builder.of(RBMKCraneConsoleBlockEntity::new,
                            ModBlocks.RBMK_CRANE_CONSOLE.get()).build(null));

    public static final RegistrySupplier<BlockEntityType<RBMKPanelBlockEntity>> RBMK_PANEL_BE =
            BLOCK_ENTITIES.register("rbmk_panel_be", () ->
                    BlockEntityType.Builder.of(RBMKPanelBlockEntity::new,
                            ModBlocks.RBMK_DISPLAY.get(), ModBlocks.RBMK_GAUGE.get(),
                            ModBlocks.RBMK_INDICATOR.get(), ModBlocks.RBMK_LEVER.get(),
                            ModBlocks.RBMK_NUMITRON.get(), ModBlocks.RBMK_GRAPH.get(),
                            ModBlocks.RBMK_TERMINAL.get(), ModBlocks.RBMK_KEYPAD.get()).build(null));

    public static void init() {
        BLOCK_ENTITIES.register();
    }
}
