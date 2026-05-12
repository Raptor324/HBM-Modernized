package com.hbm_m.block.entity;
import com.hbm_m.api.energy.ConverterBlockEntity;
import com.hbm_m.api.energy.SwitchBlockEntity;
import com.hbm_m.api.energy.WireBlockEntity;
import com.hbm_m.block.ModBlocks;
import com.hbm_m.block.entity.bomb.NukeFatManBlockEntity;
import com.hbm_m.block.entity.crates.DeshCrateBlockEntity;
import com.hbm_m.block.entity.crates.IronCrateBlockEntity;
import com.hbm_m.block.entity.crates.SteelCrateBlockEntity;
import com.hbm_m.block.entity.crates.TemplateCrateBlockEntity;
import com.hbm_m.block.entity.crates.TungstenCrateBlockEntity;
import com.hbm_m.block.entity.doors.DoorBlockEntity;
import com.hbm_m.block.entity.explosives.MineBlockEntity;
import com.hbm_m.block.entity.machines.AnvilBlockEntity;
import com.hbm_m.block.entity.machines.BatterySocketBlockEntity;
import com.hbm_m.block.entity.machines.BlastFurnaceBlockEntity;
import com.hbm_m.block.entity.machines.FluidDuctBlockEntity;
import com.hbm_m.block.entity.machines.FluidExhaustBlockEntity;
import com.hbm_m.block.entity.machines.FluidPumpBlockEntity;
import com.hbm_m.block.entity.machines.FluidValveBlockEntity;
import com.hbm_m.block.entity.machines.GeigerCounterBlockEntity;
import com.hbm_m.block.entity.machines.HeatingOvenBlockEntity;
import com.hbm_m.block.entity.machines.LaunchPadBlockEntity;
import com.hbm_m.block.entity.machines.LaunchPadRustedBlockEntity;
import com.hbm_m.block.entity.machines.MachineAdvancedAssemblerBlockEntity;
import com.hbm_m.block.entity.machines.MachineAssemblerBlockEntity;
import com.hbm_m.block.entity.machines.MachineBatteryBlockEntity;
import com.hbm_m.block.entity.machines.MachineBreederBlockEntity;
import com.hbm_m.block.entity.machines.MachineLargePylonBlockEntity;
import com.hbm_m.block.entity.machines.MachineCentrifugeBlockEntity;
import com.hbm_m.block.entity.machines.MachineChemicalPlantBlockEntity;
import com.hbm_m.block.entity.machines.MachineCoolingTowerBlockEntity;
import com.hbm_m.block.entity.machines.MachineCrystallizerBlockEntity;
import com.hbm_m.block.entity.machines.MachineCyclotronBlockEntity;
import com.hbm_m.block.entity.machines.MachineArcWelderBlockEntity;
import com.hbm_m.block.entity.machines.MachineCrackingTowerBlockEntity;
import com.hbm_m.block.entity.machines.MachineDerrickBlockEntity;
import com.hbm_m.block.entity.machines.MachineFractionTowerBlockEntity;
import com.hbm_m.block.entity.machines.MachineFlareStackBlockEntity;
import com.hbm_m.block.entity.machines.MachineFelBlockEntity;
import com.hbm_m.block.entity.machines.MachineMixerBlockEntity;
import com.hbm_m.block.entity.machines.MachineMiningDrillBlockEntity;
import com.hbm_m.block.entity.machines.MachineSilexBlockEntity;
import com.hbm_m.block.entity.machines.MachineSolderingStationBlockEntity;
import com.hbm_m.block.entity.machines.MachineFluidTankBlockEntity;
import com.hbm_m.block.entity.machines.MachineRbmkConsoleBlockEntity;
import com.hbm_m.block.entity.machines.MachineSubstationBlockEntity;
import com.hbm_m.block.entity.machines.MachineTurbineBlockEntity;
import com.hbm_m.block.entity.machines.MachineZirnoxBlockEntity;
import com.hbm_m.block.entity.machines.MachineCrucibleBlockEntity;
import com.hbm_m.block.entity.machines.MachineGasCentrifugeBlockEntity;
import com.hbm_m.block.entity.machines.MachineHydraulicFrackiningTowerBlockEntity;
import com.hbm_m.block.entity.machines.MachineIndustrialBoilerBlockEntity;
import com.hbm_m.block.entity.machines.MachineSolarBoilerBlockEntity;
import com.hbm_m.block.entity.machines.MachineSolarMirrorsBlockEntity;
import com.hbm_m.block.entity.machines.MachineCoreEmitterBlockEntity;
import com.hbm_m.block.entity.machines.MachineCoreInjectorBlockEntity;
import com.hbm_m.block.entity.machines.MachineCoreReceiverBlockEntity;
import com.hbm_m.block.entity.machines.MachineVacuumDistillBlockEntity;
import com.hbm_m.block.entity.machines.MachineTurbofanBlockEntity;
import com.hbm_m.block.entity.machines.MachineWatzPowerplantBlockEntity;
import com.hbm_m.block.entity.machines.MachineHydrotreaterBlockEntity;
import com.hbm_m.block.entity.machines.MachineCatalyticReformerBlockEntity;
import com.hbm_m.block.entity.machines.MachineDeuteriumTowerBlockEntity;
import com.hbm_m.block.entity.machines.MachineChemicalFactoryBlockEntity;
import com.hbm_m.block.entity.machines.MachineSteamTurbineBlockEntity;
import com.hbm_m.block.entity.machines.MachineLiquefactorBlockEntity;
import com.hbm_m.block.entity.machines.MachineIndustrialTurbineBlockEntity;
import com.hbm_m.block.entity.machines.MachinePressBlockEntity;
import com.hbm_m.block.entity.machines.MachinePumpjackBlockEntity;
import com.hbm_m.block.entity.machines.MachineRadarBlockEntity;
import com.hbm_m.block.entity.machines.MachineRefineryBlockEntity;
import com.hbm_m.block.entity.machines.MachineShredderBlockEntity;
import com.hbm_m.block.entity.machines.MachineTowerSmallBlockEntity;
import com.hbm_m.block.entity.machines.MachineWoodBurnerBlockEntity;
import com.hbm_m.block.entity.machines.UniversalMachinePartBlockEntity;
import com.hbm_m.lib.RefStrings;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;


public class ModBlockEntities {

	public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
			DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, RefStrings.MODID);

	public static final RegistryObject<BlockEntityType<MachineCrystallizerBlockEntity>> CRYSTALLIZER =
		BLOCK_ENTITIES.register("crystallizer", () ->
			BlockEntityType.Builder.of(MachineCrystallizerBlockEntity::new, ModBlocks.CRYSTALLIZER.get())
				.build(null));

	public static final RegistryObject<BlockEntityType<MachineBreederBlockEntity>> BREEDER =
		BLOCK_ENTITIES.register("breeder", () ->
			BlockEntityType.Builder.of(MachineBreederBlockEntity::new, ModBlocks.BREEDER.get())
				.build(null));

	public static final RegistryObject<BlockEntityType<MachineLargePylonBlockEntity>> LARGE_PYLON =
		BLOCK_ENTITIES.register("large_pylon", () ->
			BlockEntityType.Builder.of(MachineLargePylonBlockEntity::new, ModBlocks.LARGE_PYLON.get())
				.build(null));

	public static final RegistryObject<BlockEntityType<MachineHydraulicFrackiningTowerBlockEntity>> HYDRAULIC_FRACKINING_TOWER_BE =
		BLOCK_ENTITIES.register("hydraulic_frackining_tower_be", () ->
            BlockEntityType.Builder.of(MachineHydraulicFrackiningTowerBlockEntity::new, ModBlocks.HYDRAULIC_FRACKINING_TOWER.get())
                .build(null));

	public static final RegistryObject<BlockEntityType<MachineCoolingTowerBlockEntity>> COOLING_TOWER_BE =
		BLOCK_ENTITIES.register("cooling_tower_be", () ->
            BlockEntityType.Builder.of(MachineCoolingTowerBlockEntity::new, ModBlocks.COOLING_TOWER.get())
                .build(null));

        public static final RegistryObject<BlockEntityType<MachineTowerSmallBlockEntity>> TOWER_SMALL_BE =
                BLOCK_ENTITIES.register("tower_small_be", () ->
            BlockEntityType.Builder.of(MachineTowerSmallBlockEntity::new, ModBlocks.TOWER_SMALL.get())
                .build(null));

        public static final RegistryObject<BlockEntityType<MachineCyclotronBlockEntity>> CYCLOTRON_BE =
                BLOCK_ENTITIES.register("cyclotron_be", () ->
            BlockEntityType.Builder.of(MachineCyclotronBlockEntity::new, ModBlocks.CYCLOTRON.get())
                .build(null));

        public static final RegistryObject<BlockEntityType<MachineZirnoxBlockEntity>> ZIRNOX_BE =
                BLOCK_ENTITIES.register("zirnox_be", () ->
            BlockEntityType.Builder.of(MachineZirnoxBlockEntity::new, ModBlocks.ZIRNOX.get())
                .build(null));

                public static final RegistryObject<BlockEntityType<MachineArcWelderBlockEntity>> ARC_WELDER_BE =
                                BLOCK_ENTITIES.register("arc_welder_be", () ->
                        BlockEntityType.Builder.of(MachineArcWelderBlockEntity::new, ModBlocks.ARC_WELDER.get())
                                .build(null));

                public static final RegistryObject<BlockEntityType<MachineSolderingStationBlockEntity>> SOLDERING_STATION_BE =
                                BLOCK_ENTITIES.register("soldering_station_be", () ->
                        BlockEntityType.Builder.of(MachineSolderingStationBlockEntity::new, ModBlocks.SOLDERING_STATION.get())
                                .build(null));

                public static final RegistryObject<BlockEntityType<MachineMixerBlockEntity>> MIXER_BE =
                                BLOCK_ENTITIES.register("mixer_be", () ->
                        BlockEntityType.Builder.of(MachineMixerBlockEntity::new, ModBlocks.MIXER.get())
                                .build(null));

                public static final RegistryObject<BlockEntityType<MachineDerrickBlockEntity>> DERRICK_BE =
                                BLOCK_ENTITIES.register("derrick_be", () ->
                        BlockEntityType.Builder.of(MachineDerrickBlockEntity::new, ModBlocks.DERRICK.get())
                                .build(null));

                public static final RegistryObject<BlockEntityType<MachineRbmkConsoleBlockEntity>> RBMK_CONSOLE_BE =
                                BLOCK_ENTITIES.register("rbmk_console_be", () ->
                        BlockEntityType.Builder.of(MachineRbmkConsoleBlockEntity::new, ModBlocks.RBMK_CONSOLE.get())
                                .build(null));

                public static final RegistryObject<BlockEntityType<MachineFlareStackBlockEntity>> FLARE_STACK_BE =
                                BLOCK_ENTITIES.register("flare_stack_be", () ->
                        BlockEntityType.Builder.of(MachineFlareStackBlockEntity::new, ModBlocks.FLARE_STACK.get())
                                .build(null));

                public static final RegistryObject<BlockEntityType<MachinePumpjackBlockEntity>> PUMPJACK_BE =
                                BLOCK_ENTITIES.register("pumpjack_be", () ->
                        BlockEntityType.Builder.of(MachinePumpjackBlockEntity::new, ModBlocks.PUMPJACK.get())
                                .build(null));

                public static final RegistryObject<BlockEntityType<MachineRadarBlockEntity>> RADAR_BE =
                                BLOCK_ENTITIES.register("radar_be", () ->
                        BlockEntityType.Builder.of(MachineRadarBlockEntity::new, ModBlocks.RADAR.get(), ModBlocks.LARGE_RADAR.get())
                                .build(null));

                public static final RegistryObject<BlockEntityType<MachineCrackingTowerBlockEntity>> CRACKING_TOWER_BE =
                                BLOCK_ENTITIES.register("cracking_tower_be", () ->
                        BlockEntityType.Builder.of(MachineCrackingTowerBlockEntity::new, ModBlocks.CRACKING_TOWER.get())
                                .build(null));

                public static final RegistryObject<BlockEntityType<MachineFractionTowerBlockEntity>> FRACTION_TOWER_BE =
                                BLOCK_ENTITIES.register("fraction_tower_be", () ->
                        BlockEntityType.Builder.of(MachineFractionTowerBlockEntity::new, ModBlocks.FRACTION_TOWER.get())
                                .build(null));

                public static final RegistryObject<BlockEntityType<MachineMiningDrillBlockEntity>> MINING_DRILL_BE =
                                BLOCK_ENTITIES.register("mining_drill_be", () ->
                        BlockEntityType.Builder.of(MachineMiningDrillBlockEntity::new, ModBlocks.MINING_DRILL.get())
                                .build(null));

                public static final RegistryObject<BlockEntityType<MachineFelBlockEntity>> FEL_BE =
                                BLOCK_ENTITIES.register("fel_be", () ->
                        BlockEntityType.Builder.of(MachineFelBlockEntity::new, ModBlocks.FEL.get())
                                .build(null));

                public static final RegistryObject<BlockEntityType<MachineSilexBlockEntity>> SILEX_BE =
                                BLOCK_ENTITIES.register("silex_be", () ->
                        BlockEntityType.Builder.of(MachineSilexBlockEntity::new, ModBlocks.SILEX.get())
                                .build(null));

    public static final RegistryObject<BlockEntityType<GeigerCounterBlockEntity>> GEIGER_COUNTER_BE =
		BLOCK_ENTITIES.register("geiger_counter_be", () ->
			BlockEntityType.Builder.<GeigerCounterBlockEntity>of(GeigerCounterBlockEntity::new, ModBlocks.GEIGER_COUNTER_BLOCK.get())
				.build(null));

    public static final RegistryObject<BlockEntityType<MachineAssemblerBlockEntity>> MACHINE_ASSEMBLER_BE =
		BLOCK_ENTITIES.register("machine_assembler_be", () ->
			BlockEntityType.Builder.<MachineAssemblerBlockEntity>of(MachineAssemblerBlockEntity::new, ModBlocks.MACHINE_ASSEMBLER.get())
				.build(null));

    public static final RegistryObject<BlockEntityType<MachineAdvancedAssemblerBlockEntity>> ADVANCED_ASSEMBLY_MACHINE_BE =
		BLOCK_ENTITIES.register("advanced_assembly_machine_be", () ->
			BlockEntityType.Builder.<MachineAdvancedAssemblerBlockEntity>of(MachineAdvancedAssemblerBlockEntity::new, ModBlocks.ADVANCED_ASSEMBLY_MACHINE.get())
				.build(null));

    public static final RegistryObject<BlockEntityType<MachineBatteryBlockEntity>> MACHINE_BATTERY_BE =
            BLOCK_ENTITIES.register("machine_battery_be", () -> {
                // Превращаем список RegistryObject в массив Block[]
                Block[] validBlocks = ModBlocks.BATTERY_BLOCKS.stream()
                        .map(RegistryObject::get)
                        .toArray(Block[]::new);

                return BlockEntityType.Builder.<MachineBatteryBlockEntity>of(MachineBatteryBlockEntity::new, validBlocks)
                        .build(null);
            });

    public static final RegistryObject<BlockEntityType<AnvilBlockEntity>> ANVIL_BE =
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

    public static final RegistryObject<BlockEntityType<MineBlockEntity>> MINE_BLOCK_ENTITY =
            BLOCK_ENTITIES.register("mine_block_entity", () ->
                    BlockEntityType.Builder.of(MineBlockEntity::new, ModBlocks.MINE_AP.get())
                            .build(null)
            );

    public static final RegistryObject<BlockEntityType<MineBlockEntity>> MINE_NUKE_BLOCK_ENTITY =
            BLOCK_ENTITIES.register("mine_nuke_block_entity", () ->
                    BlockEntityType.Builder.of(MineBlockEntity::new, ModBlocks.MINE_FAT.get())
                            .build(null)
            );

    public static final RegistryObject<BlockEntityType<NukeFatManBlockEntity>> NUKE_FAT_MAN_BE =
            BLOCK_ENTITIES.register("nuke_fat_man_be", () ->
                    BlockEntityType.Builder.of(NukeFatManBlockEntity::new, ModBlocks.NUKE_FAT_MAN.get())
                            .build(null));

    public static final RegistryObject<BlockEntityType<MachineShredderBlockEntity>> SHREDDER =
            BLOCK_ENTITIES.register("shredder", () ->
                    BlockEntityType.Builder.of(MachineShredderBlockEntity::new,
                            ModBlocks.SHREDDER.get()).build(null));

    public static final RegistryObject<BlockEntityType<MachineCentrifugeBlockEntity>> CENTRIFUGE_BE =
            BLOCK_ENTITIES.register("centrifuge_be", () ->
                    BlockEntityType.Builder.of(MachineCentrifugeBlockEntity::new,
                            ModBlocks.CENTRIFUGE.get()).build(null));

    public static final RegistryObject<BlockEntityType<UniversalMachinePartBlockEntity>> UNIVERSAL_MACHINE_PART_BE =
        BLOCK_ENTITIES.register("universal_machine_part_be", () ->
			BlockEntityType.Builder.<UniversalMachinePartBlockEntity>of(UniversalMachinePartBlockEntity::new, ModBlocks.UNIVERSAL_MACHINE_PART.get())
				.build(null));

	public static final RegistryObject<BlockEntityType<WireBlockEntity>> WIRE_BE =
		BLOCK_ENTITIES.register("wire_be", () ->
			BlockEntityType.Builder.<WireBlockEntity>of(WireBlockEntity::new, ModBlocks.WIRE_COATED.get())
				.build(null));

	public static final RegistryObject<BlockEntityType<LaunchPadBlockEntity>> LAUNCH_PAD_BE =
		BLOCK_ENTITIES.register("launch_pad_be", () ->
			BlockEntityType.Builder.<LaunchPadBlockEntity>of(LaunchPadBlockEntity::new, ModBlocks.LAUNCH_PAD.get())
				.build(null));

	public static final RegistryObject<BlockEntityType<LaunchPadRustedBlockEntity>> LAUNCH_PAD_RUSTED_BE =
		BLOCK_ENTITIES.register("launch_pad_rusted_be", () ->
			BlockEntityType.Builder.<LaunchPadRustedBlockEntity>of(LaunchPadRustedBlockEntity::new, ModBlocks.LAUNCH_PAD_RUSTED.get())
				.build(null));



    public static final RegistryObject<BlockEntityType<SwitchBlockEntity>> SWITCH_BE =
            BLOCK_ENTITIES.register("switch_be", () ->
                    BlockEntityType.Builder.of(SwitchBlockEntity::new, ModBlocks.SWITCH.get())
                            .build(null));

	public static final RegistryObject<BlockEntityType<BlastFurnaceBlockEntity>> BLAST_FURNACE_BE =
			BLOCK_ENTITIES.register("blast_furnace_be", () ->
					BlockEntityType.Builder.of(BlastFurnaceBlockEntity::new,
							ModBlocks.BLAST_FURNACE.get()).build(null));

	public static final RegistryObject<BlockEntityType<MachinePressBlockEntity>> PRESS_BE =
			BLOCK_ENTITIES.register("press_be", () ->
					BlockEntityType.Builder.of(MachinePressBlockEntity::new,
							ModBlocks.PRESS.get()).build(null));

	public static final RegistryObject<BlockEntityType<MachineWoodBurnerBlockEntity>> WOOD_BURNER_BE =
			BLOCK_ENTITIES.register("wood_burner_be", () ->
					BlockEntityType.Builder.of(MachineWoodBurnerBlockEntity::new,
							ModBlocks.WOOD_BURNER.get()).build(null));

            public static final RegistryObject<BlockEntityType<MachineChemicalPlantBlockEntity>> CHEMICAL_PLANT_BE =
                    BLOCK_ENTITIES.register("chemical_plant_be", () ->
                            BlockEntityType.Builder.of(MachineChemicalPlantBlockEntity::new,
                                    ModBlocks.CHEMICAL_PLANT.get()).build(null));

    public static final RegistryObject<BlockEntityType<MachineGasCentrifugeBlockEntity>> GAS_CENTRIFUGE_BE =
            BLOCK_ENTITIES.register("gas_centrifuge_be", () ->
                    BlockEntityType.Builder.of(MachineGasCentrifugeBlockEntity::new,
                            ModBlocks.GAS_CENTRIFUGE.get()).build(null));

    public static final RegistryObject<BlockEntityType<MachineFluidTankBlockEntity>> FLUID_TANK_BE =
            BLOCK_ENTITIES.register("fluid_tank_be", () ->
                    BlockEntityType.Builder.of(MachineFluidTankBlockEntity::new,
                            ModBlocks.FLUID_TANK.get()).build(null));

    public static final RegistryObject<BlockEntityType<BatterySocketBlockEntity>> BATTERY_SOCKET_BE =
            BLOCK_ENTITIES.register("battery_socket_be", () ->
                    BlockEntityType.Builder.of(BatterySocketBlockEntity::new,
                            ModBlocks.MACHINE_BATTERY_SOCKET.get()).build(null));

    public static final RegistryObject<BlockEntityType<MachineIndustrialBoilerBlockEntity>> INDUSTRIAL_BOILER_BE =
            BLOCK_ENTITIES.register("industrial_boiler_be", () ->
                    BlockEntityType.Builder.of(MachineIndustrialBoilerBlockEntity::new,
                            ModBlocks.INDUSTRIAL_BOILER.get()).build(null));

    public static final RegistryObject<BlockEntityType<MachineSolarBoilerBlockEntity>> SOLAR_BOILER_BE =
            BLOCK_ENTITIES.register("solar_boiler_be", () ->
                    BlockEntityType.Builder.of(MachineSolarBoilerBlockEntity::new,
                            ModBlocks.SOLAR_BOILER.get()).build(null));

    public static final RegistryObject<BlockEntityType<MachineSolarMirrorsBlockEntity>> SOLAR_MIRRORS_BE =
            BLOCK_ENTITIES.register("solar_mirrors_be", () ->
                    BlockEntityType.Builder.of(MachineSolarMirrorsBlockEntity::new,
                            ModBlocks.SOLAR_MIRRORS.get()).build(null));

    public static final RegistryObject<BlockEntityType<MachineWatzPowerplantBlockEntity>> WATZ_POWERPLANT_BE =
            BLOCK_ENTITIES.register("watz_powerplant_be", () ->
                    BlockEntityType.Builder.of(MachineWatzPowerplantBlockEntity::new,
                            ModBlocks.WATZ_POWERPLANT.get()).build(null));

    public static final RegistryObject<BlockEntityType<MachineHydrotreaterBlockEntity>> HYDROTREATER_BE =
            BLOCK_ENTITIES.register("hydrotreater_be", () ->
                    BlockEntityType.Builder.of(MachineHydrotreaterBlockEntity::new,
                            ModBlocks.HYDROTREATER.get()).build(null));

    public static final RegistryObject<BlockEntityType<MachineCatalyticReformerBlockEntity>> CATALYTIC_REFORMER_BE =
            BLOCK_ENTITIES.register("catalytic_reformer_be", () ->
                    BlockEntityType.Builder.of(MachineCatalyticReformerBlockEntity::new,
                            ModBlocks.CATALYTIC_REFORMER.get()).build(null));

    public static final RegistryObject<BlockEntityType<MachineDeuteriumTowerBlockEntity>> DEUTERIUM_TOWER_BE =
            BLOCK_ENTITIES.register("deuterium_tower_be", () ->
                    BlockEntityType.Builder.of(MachineDeuteriumTowerBlockEntity::new,
                            ModBlocks.DEUTERIUM_TOWER.get()).build(null));

    public static final RegistryObject<BlockEntityType<MachineChemicalFactoryBlockEntity>> CHEMICAL_FACTORY_BE =
            BLOCK_ENTITIES.register("chemical_factory_be", () ->
                    BlockEntityType.Builder.of(MachineChemicalFactoryBlockEntity::new,
                            ModBlocks.CHEMICAL_FACTORY.get()).build(null));

    public static final RegistryObject<BlockEntityType<MachineSteamTurbineBlockEntity>> STEAM_TURBINE_BE =
            BLOCK_ENTITIES.register("steam_turbine_be", () ->
                    BlockEntityType.Builder.of(MachineSteamTurbineBlockEntity::new,
                            ModBlocks.STEAM_TURBINE.get()).build(null));

    public static final RegistryObject<BlockEntityType<MachineLiquefactorBlockEntity>> LIQUEFACTOR_BE =
            BLOCK_ENTITIES.register("liquefactor_be", () ->
                    BlockEntityType.Builder.of(MachineLiquefactorBlockEntity::new,
                            ModBlocks.LIQUEFACTOR.get()).build(null));

    public static final RegistryObject<BlockEntityType<MachineCoreEmitterBlockEntity>> CORE_EMITTER_BE =
            BLOCK_ENTITIES.register("core_emitter_be", () ->
                    BlockEntityType.Builder.of(MachineCoreEmitterBlockEntity::new,
                            ModBlocks.CORE_EMITTER.get()).build(null));

    public static final RegistryObject<BlockEntityType<MachineCoreInjectorBlockEntity>> CORE_INJECTOR_BE =
            BLOCK_ENTITIES.register("core_injector_be", () ->
                    BlockEntityType.Builder.of(MachineCoreInjectorBlockEntity::new,
                            ModBlocks.CORE_INJECTOR.get()).build(null));

    public static final RegistryObject<BlockEntityType<MachineCoreReceiverBlockEntity>> CORE_RECEIVER_BE =
            BLOCK_ENTITIES.register("core_receiver_be", () ->
                    BlockEntityType.Builder.of(MachineCoreReceiverBlockEntity::new,
                            ModBlocks.CORE_RECEIVER.get()).build(null));

    public static final RegistryObject<BlockEntityType<MachineVacuumDistillBlockEntity>> VACUUM_DISTILL_BE =
            BLOCK_ENTITIES.register("vacuum_distill_be", () ->
                    BlockEntityType.Builder.of(MachineVacuumDistillBlockEntity::new,
                            ModBlocks.VACUUM_DISTILL.get()).build(null));

    public static final RegistryObject<BlockEntityType<MachineTurbofanBlockEntity>> TURBOFAN_BE =
            BLOCK_ENTITIES.register("turbofan_be", () ->
                    BlockEntityType.Builder.of(MachineTurbofanBlockEntity::new,
                            ModBlocks.TURBOFAN.get()).build(null));

    public static final RegistryObject<BlockEntityType<MachineRefineryBlockEntity>> REFINERY_BE =
            BLOCK_ENTITIES.register("refinery_be", () ->
                    BlockEntityType.Builder.of(MachineRefineryBlockEntity::new,
                            ModBlocks.REFINERY.get()).build(null));

    public static final RegistryObject<BlockEntityType<MachineIndustrialTurbineBlockEntity>> INDUSTRIAL_TURBINE_BE =
            BLOCK_ENTITIES.register("industrial_turbine_be", () ->
                    BlockEntityType.Builder.of(MachineIndustrialTurbineBlockEntity::new,
                            ModBlocks.INDUSTRIAL_TURBINE.get()).build(null));

    public static final RegistryObject<BlockEntityType<MachineTurbineBlockEntity>> TURBINE_BE =
            BLOCK_ENTITIES.register("turbine_be", () ->
                    BlockEntityType.Builder.of(MachineTurbineBlockEntity::new,
                            ModBlocks.TURBINE.get()).build(null));

    public static final RegistryObject<BlockEntityType<MachineSubstationBlockEntity>> SUBSTATION_BE =
            BLOCK_ENTITIES.register("substation_be", () ->
                    BlockEntityType.Builder.of(MachineSubstationBlockEntity::new,
                            ModBlocks.SUBSTATION.get()).build(null));

    // ДВЕРИ

    public static final RegistryObject<BlockEntityType<DoorBlockEntity>> DOOR_ENTITY =
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

    public static final RegistryObject<BlockEntityType<IronCrateBlockEntity>> IRON_CRATE_BE =
            BLOCK_ENTITIES.register("iron_crate_be", () ->
                    BlockEntityType.Builder.<IronCrateBlockEntity>of(
                            IronCrateBlockEntity::new,
                            ModBlocks.CRATE_IRON.get()
                    ).build(null));

    public static final RegistryObject<BlockEntityType<SteelCrateBlockEntity>> STEEL_CRATE_BE =
            BLOCK_ENTITIES.register("steel_crate_be", () ->
                    BlockEntityType.Builder.<SteelCrateBlockEntity>of(
                            SteelCrateBlockEntity::new,
                            ModBlocks.CRATE_STEEL.get()
                    ).build(null));
    public static final RegistryObject<BlockEntityType<DeshCrateBlockEntity>> DESH_CRATE_BE =
            BLOCK_ENTITIES.register("desh_crate_be", () ->
                    BlockEntityType.Builder.<DeshCrateBlockEntity>of(
                            DeshCrateBlockEntity::new,
                            ModBlocks.CRATE_DESH.get()
                    ).build(null));

    public static final RegistryObject<BlockEntityType<TungstenCrateBlockEntity>> TUNGSTEN_CRATE_BE =
            BLOCK_ENTITIES.register("tungsten_crate_be", () ->
                    BlockEntityType.Builder.<TungstenCrateBlockEntity>of(
                            TungstenCrateBlockEntity::new,
                            ModBlocks.CRATE_TUNGSTEN.get()
                    ).build(null));

    public static final RegistryObject<BlockEntityType<TemplateCrateBlockEntity>> TEMPLATE_CRATE_BE =
            BLOCK_ENTITIES.register("template_crate_be", () ->
                    BlockEntityType.Builder.<TemplateCrateBlockEntity>of(
                            TemplateCrateBlockEntity::new,
                            ModBlocks.CRATE_TEMPLATE.get()
                    ).build(null));

    public static final RegistryObject<BlockEntityType<ConverterBlockEntity>> CONVERTER_BE =
            BLOCK_ENTITIES.register("converter_be",
                    () -> BlockEntityType.Builder.of(ConverterBlockEntity::new, ModBlocks.CONVERTER_BLOCK.get()).build(null));

    public static final RegistryObject<BlockEntityType<HeatingOvenBlockEntity>> HEATING_OVEN_BE =
            BLOCK_ENTITIES.register("heating_oven_be", () ->
                    BlockEntityType.Builder.of(HeatingOvenBlockEntity::new,
                            ModBlocks.HEATING_OVEN.get()).build(null));

    public static final RegistryObject<BlockEntityType<FluidDuctBlockEntity>> FLUID_DUCT_BE =
            BLOCK_ENTITIES.register("fluid_duct_be", () ->
                    BlockEntityType.Builder.of(FluidDuctBlockEntity::new,
                            ModBlocks.FLUID_DUCT.get(),
                            ModBlocks.FLUID_DUCT_COLORED.get(),
                            ModBlocks.FLUID_DUCT_SILVER.get()).build(null));

    public static final RegistryObject<BlockEntityType<FluidValveBlockEntity>> FLUID_VALVE_BE =
            BLOCK_ENTITIES.register("fluid_valve_be", () ->
                    BlockEntityType.Builder.of(FluidValveBlockEntity::new,
                            ModBlocks.FLUID_VALVE.get()).build(null));

    public static final RegistryObject<BlockEntityType<FluidPumpBlockEntity>> FLUID_PUMP_BE =
            BLOCK_ENTITIES.register("fluid_pump_be", () ->
                    BlockEntityType.Builder.of(FluidPumpBlockEntity::new,
                            ModBlocks.FLUID_PUMP.get()).build(null));

    public static final RegistryObject<BlockEntityType<FluidExhaustBlockEntity>> FLUID_EXHAUST_BE =
            BLOCK_ENTITIES.register("fluid_exhaust_be", () ->
                    BlockEntityType.Builder.of(FluidExhaustBlockEntity::new,
                            ModBlocks.FLUID_EXHAUST.get()).build(null));

    public static final RegistryObject<BlockEntityType<MachineCrucibleBlockEntity>> CRUCIBLE_BE =
            BLOCK_ENTITIES.register("crucible_be", () ->
                    BlockEntityType.Builder.of(MachineCrucibleBlockEntity::new,
                            ModBlocks.CRUCIBLE.get()).build(null));

    public static void register(IEventBus eventBus) {
        BLOCK_ENTITIES.register(eventBus);
    }
}
