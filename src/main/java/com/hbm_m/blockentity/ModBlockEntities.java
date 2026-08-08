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
import com.hbm_m.blockentity.machines.MachineChungusBlockEntity;
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
import com.hbm_m.blockentity.machines.MachineElectricFurnaceBlockEntity;
import com.hbm_m.blockentity.machines.MachineFurnaceBrickBlockEntity;
import com.hbm_m.blockentity.machines.MachineFurnaceIronBlockEntity;
import com.hbm_m.blockentity.machines.MachineFurnaceSteelBlockEntity;
import com.hbm_m.blockentity.machines.MachineGasCentrifugeBlockEntity;
import com.hbm_m.blockentity.machines.MachineHydrotreaterBlockEntity;
import com.hbm_m.blockentity.machines.MachineIndustrialBoilerBlockEntity;
import com.hbm_m.blockentity.machines.MachineIndustrialTurbineBlockEntity;
import com.hbm_m.blockentity.machines.MachineLargePylonBlockEntity;
import com.hbm_m.blockentity.machines.MachineLiquefactorBlockEntity;
import com.hbm_m.blockentity.machines.MachineMiningDrillBlockEntity;
import com.hbm_m.blockentity.machines.MachineMixerBlockEntity;
import com.hbm_m.blockentity.machines.MachineOreSlopperBlockEntity;
import com.hbm_m.blockentity.machines.MachinePressBlockEntity;
import com.hbm_m.blockentity.machines.MachinePumpjackBlockEntity;
import com.hbm_m.blockentity.machines.MachineRadarBlockEntity;
import com.hbm_m.blockentity.machines.MachineRadarScreenBlockEntity;
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

	public static final RegistrySupplier<BlockEntityType<com.hbm_m.blockentity.nature.OreBedrockBlockEntity>> ORE_BEDROCK_BE =
		BLOCK_ENTITIES.register("ore_bedrock_mineral", () ->
			BlockEntityType.Builder.of(com.hbm_m.blockentity.nature.OreBedrockBlockEntity::new, ModBlocks.ORE_BEDROCK.get())
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

    public static final RegistrySupplier<BlockEntityType<com.hbm_m.blockentity.machines.MachineOilWellBlockEntity>> MACHINE_WELL_BE =
            BLOCK_ENTITIES.register("machine_well_be", () ->
                    BlockEntityType.Builder.of(com.hbm_m.blockentity.machines.MachineOilWellBlockEntity::new, ModBlocks.MACHINE_WELL.get()).build(null));

    public static final RegistrySupplier<BlockEntityType<com.hbm_m.blockentity.machines.MachineCokerBlockEntity>> COKER_BE =
            BLOCK_ENTITIES.register("coker_be", () ->
                    BlockEntityType.Builder.of(com.hbm_m.blockentity.machines.MachineCokerBlockEntity::new, ModBlocks.COKER.get()).build(null));

    public static final RegistrySupplier<BlockEntityType<com.hbm_m.blockentity.machines.MachinePyroOvenBlockEntity>> PYROOVEN_BE =
            BLOCK_ENTITIES.register("pyrooven_be", () ->
                    BlockEntityType.Builder.of(com.hbm_m.blockentity.machines.MachinePyroOvenBlockEntity::new, ModBlocks.PYROOVEN.get()).build(null));

    public static final RegistrySupplier<BlockEntityType<com.hbm_m.blockentity.machines.MachineSolidifierBlockEntity>> SOLIDIFIER_BE =
            BLOCK_ENTITIES.register("solidifier_be", () ->
                    BlockEntityType.Builder.of(com.hbm_m.blockentity.machines.MachineSolidifierBlockEntity::new, ModBlocks.SOLIDIFIER.get()).build(null));

    public static final RegistrySupplier<BlockEntityType<com.hbm_m.blockentity.machines.MachineAshpitBlockEntity>> ASHPIT_BE =
            BLOCK_ENTITIES.register("ashpit_be", () ->
                    BlockEntityType.Builder.of(com.hbm_m.blockentity.machines.MachineAshpitBlockEntity::new, ModBlocks.ASHPIT.get()).build(null));

    public static final RegistrySupplier<BlockEntityType<com.hbm_m.blockentity.machines.MachineReactorResearchBlockEntity>> REACTOR_RESEARCH_BE =
            BLOCK_ENTITIES.register("reactor_research_be", () ->
                    BlockEntityType.Builder.of(com.hbm_m.blockentity.machines.MachineReactorResearchBlockEntity::new, ModBlocks.REACTOR_RESEARCH.get()).build(null));

    public static final RegistrySupplier<BlockEntityType<com.hbm_m.blockentity.machines.MachineRadGenBlockEntity>> RADGEN_BE =
            BLOCK_ENTITIES.register("radgen_be", () ->
                    BlockEntityType.Builder.of(com.hbm_m.blockentity.machines.MachineRadGenBlockEntity::new, ModBlocks.MACHINE_RADGEN.get()).build(null));

    public static final RegistrySupplier<BlockEntityType<com.hbm_m.blockentity.network.MachineCraneInserterBlockEntity>> CRANE_INSERTER_BE =
            BLOCK_ENTITIES.register("crane_inserter_be", () ->
                    BlockEntityType.Builder.of(com.hbm_m.blockentity.network.MachineCraneInserterBlockEntity::new, ModBlocks.CRANE_INSERTER.get()).build(null));

    public static final RegistrySupplier<BlockEntityType<com.hbm_m.blockentity.network.MachineCraneSplitterBlockEntity>> CRANE_SPLITTER_BE =
            BLOCK_ENTITIES.register("crane_splitter_be", () ->
                    BlockEntityType.Builder.of(com.hbm_m.blockentity.network.MachineCraneSplitterBlockEntity::new, ModBlocks.CRANE_SPLITTER.get()).build(null));

    public static final RegistrySupplier<BlockEntityType<com.hbm_m.blockentity.network.MachineCraneExtractorBlockEntity>> CRANE_EXTRACTOR_BE =
            BLOCK_ENTITIES.register("crane_extractor_be", () ->
                    BlockEntityType.Builder.of(com.hbm_m.blockentity.network.MachineCraneExtractorBlockEntity::new, ModBlocks.CRANE_EXTRACTOR.get()).build(null));

    public static final RegistrySupplier<BlockEntityType<com.hbm_m.blockentity.network.MachineCraneGrabberBlockEntity>> CRANE_GRABBER_BE =
            BLOCK_ENTITIES.register("crane_grabber_be", () ->
                    BlockEntityType.Builder.of(com.hbm_m.blockentity.network.MachineCraneGrabberBlockEntity::new, ModBlocks.CRANE_GRABBER.get()).build(null));

    public static final RegistrySupplier<BlockEntityType<com.hbm_m.blockentity.network.MachineCraneRouterBlockEntity>> CRANE_ROUTER_BE =
            BLOCK_ENTITIES.register("crane_router_be", () ->
                    BlockEntityType.Builder.of(com.hbm_m.blockentity.network.MachineCraneRouterBlockEntity::new, ModBlocks.CRANE_ROUTER.get()).build(null));

    public static final RegistrySupplier<BlockEntityType<com.hbm_m.blockentity.network.MachineCraneBoxerBlockEntity>> CRANE_BOXER_BE =
            BLOCK_ENTITIES.register("crane_boxer_be", () ->
                    BlockEntityType.Builder.of(com.hbm_m.blockentity.network.MachineCraneBoxerBlockEntity::new, ModBlocks.CRANE_BOXER.get()).build(null));

    public static final RegistrySupplier<BlockEntityType<com.hbm_m.blockentity.network.MachineCraneUnboxerBlockEntity>> CRANE_UNBOXER_BE =
            BLOCK_ENTITIES.register("crane_unboxer_be", () ->
                    BlockEntityType.Builder.of(com.hbm_m.blockentity.network.MachineCraneUnboxerBlockEntity::new, ModBlocks.CRANE_UNBOXER.get()).build(null));

    public static final RegistrySupplier<BlockEntityType<com.hbm_m.blockentity.network.MachineDroneCrateBlockEntity>> DRONE_CRATE_BE =
            BLOCK_ENTITIES.register("drone_crate_be", () ->
                    BlockEntityType.Builder.of(com.hbm_m.blockentity.network.MachineDroneCrateBlockEntity::new, ModBlocks.DRONE_CRATE.get()).build(null));

    public static final RegistrySupplier<BlockEntityType<com.hbm_m.blockentity.network.MachineDroneWaypointBlockEntity>> DRONE_WAYPOINT_BE =
            BLOCK_ENTITIES.register("drone_waypoint_be", () ->
                    BlockEntityType.Builder.of(com.hbm_m.blockentity.network.MachineDroneWaypointBlockEntity::new, ModBlocks.DRONE_WAYPOINT.get()).build(null));

    public static final RegistrySupplier<BlockEntityType<com.hbm_m.blockentity.network.MachineDroneWaypointRequestBlockEntity>> DRONE_WAYPOINT_REQUEST_BE =
            BLOCK_ENTITIES.register("drone_waypoint_request_be", () ->
                    BlockEntityType.Builder.of(com.hbm_m.blockentity.network.MachineDroneWaypointRequestBlockEntity::new, ModBlocks.DRONE_WAYPOINT_REQUEST.get()).build(null));

    public static final RegistrySupplier<BlockEntityType<com.hbm_m.blockentity.network.radio.RadioTorchSenderBlockEntity>> RADIO_TORCH_SENDER_BE =
            BLOCK_ENTITIES.register("radio_torch_sender_be", () ->
                    BlockEntityType.Builder.of(com.hbm_m.blockentity.network.radio.RadioTorchSenderBlockEntity::new, ModBlocks.RADIO_TORCH_SENDER.get()).build(null));

    public static final RegistrySupplier<BlockEntityType<com.hbm_m.blockentity.network.radio.RadioTorchReceiverBlockEntity>> RADIO_TORCH_RECEIVER_BE =
            BLOCK_ENTITIES.register("radio_torch_receiver_be", () ->
                    BlockEntityType.Builder.of(com.hbm_m.blockentity.network.radio.RadioTorchReceiverBlockEntity::new, ModBlocks.RADIO_TORCH_RECEIVER.get()).build(null));

    public static final RegistrySupplier<BlockEntityType<com.hbm_m.blockentity.network.radio.RadioTorchLogicBlockEntity>> RADIO_TORCH_LOGIC_BE =
            BLOCK_ENTITIES.register("radio_torch_logic_be", () ->
                    BlockEntityType.Builder.of(com.hbm_m.blockentity.network.radio.RadioTorchLogicBlockEntity::new, ModBlocks.RADIO_TORCH_LOGIC.get()).build(null));

    public static final RegistrySupplier<BlockEntityType<com.hbm_m.blockentity.network.radio.RadioTorchReaderBlockEntity>> RADIO_TORCH_READER_BE =
            BLOCK_ENTITIES.register("radio_torch_reader_be", () ->
                    BlockEntityType.Builder.of(com.hbm_m.blockentity.network.radio.RadioTorchReaderBlockEntity::new, ModBlocks.RADIO_TORCH_READER.get()).build(null));

    public static final RegistrySupplier<BlockEntityType<com.hbm_m.blockentity.network.radio.RadioTorchControllerBlockEntity>> RADIO_TORCH_CONTROLLER_BE =
            BLOCK_ENTITIES.register("radio_torch_controller_be", () ->
                    BlockEntityType.Builder.of(com.hbm_m.blockentity.network.radio.RadioTorchControllerBlockEntity::new, ModBlocks.RADIO_TORCH_CONTROLLER.get()).build(null));

    public static final RegistrySupplier<BlockEntityType<com.hbm_m.blockentity.network.radio.RadioTorchCounterBlockEntity>> RADIO_TORCH_COUNTER_BE =
            BLOCK_ENTITIES.register("radio_torch_counter_be", () ->
                    BlockEntityType.Builder.of(com.hbm_m.blockentity.network.radio.RadioTorchCounterBlockEntity::new, ModBlocks.RADIO_TORCH_COUNTER.get()).build(null));

    public static final RegistrySupplier<BlockEntityType<com.hbm_m.blockentity.network.MachineDroneProviderBlockEntity>> DRONE_PROVIDER_BE =
            BLOCK_ENTITIES.register("drone_provider_be", () ->
                    BlockEntityType.Builder.of(com.hbm_m.blockentity.network.MachineDroneProviderBlockEntity::new, ModBlocks.DRONE_CRATE_PROVIDER.get()).build(null));

    public static final RegistrySupplier<BlockEntityType<com.hbm_m.blockentity.network.MachineDroneRequesterBlockEntity>> DRONE_REQUESTER_BE =
            BLOCK_ENTITIES.register("drone_requester_be", () ->
                    BlockEntityType.Builder.of(com.hbm_m.blockentity.network.MachineDroneRequesterBlockEntity::new, ModBlocks.DRONE_CRATE_REQUESTER.get()).build(null));

    public static final RegistrySupplier<BlockEntityType<com.hbm_m.blockentity.network.MachineDroneDockBlockEntity>> DRONE_DOCK_BE =
            BLOCK_ENTITIES.register("drone_dock_be", () ->
                    BlockEntityType.Builder.of(com.hbm_m.blockentity.network.MachineDroneDockBlockEntity::new, ModBlocks.DRONE_DOCK.get()).build(null));

    public static final RegistrySupplier<BlockEntityType<com.hbm_m.blockentity.machines.MachineStorageDrumBlockEntity>> MACHINE_STORAGE_DRUM_BE =
            BLOCK_ENTITIES.register("machine_storage_drum_be", () ->
                    BlockEntityType.Builder.of(com.hbm_m.blockentity.machines.MachineStorageDrumBlockEntity::new, ModBlocks.MACHINE_STORAGE_DRUM.get()).build(null));

    public static final RegistrySupplier<BlockEntityType<com.hbm_m.blockentity.machines.MachineSirenBlockEntity>> MACHINE_SIREN_BE =
            BLOCK_ENTITIES.register("machine_siren_be", () ->
                    BlockEntityType.Builder.of(com.hbm_m.blockentity.machines.MachineSirenBlockEntity::new, ModBlocks.MACHINE_SIREN.get()).build(null));

    public static final RegistrySupplier<BlockEntityType<com.hbm_m.blockentity.machines.BroadcasterPcBlockEntity>> BROADCASTER_PC_BE =
            BLOCK_ENTITIES.register("broadcaster_pc_be", () ->
                    BlockEntityType.Builder.of(com.hbm_m.blockentity.machines.BroadcasterPcBlockEntity::new, ModBlocks.BROADCASTER_PC.get()).build(null));

    public static final RegistrySupplier<BlockEntityType<com.hbm_m.blockentity.machines.RadioboxBlockEntity>> RADIOBOX_BE =
            BLOCK_ENTITIES.register("radiobox_be", () ->
                    BlockEntityType.Builder.of(com.hbm_m.blockentity.machines.RadioboxBlockEntity::new, ModBlocks.RADIOBOX.get()).build(null));

    public static final RegistrySupplier<BlockEntityType<com.hbm_m.blockentity.machines.RadioRecBlockEntity>> RADIOREC_BE =
            BLOCK_ENTITIES.register("radiorec_be", () ->
                    BlockEntityType.Builder.of(com.hbm_m.blockentity.machines.RadioRecBlockEntity::new, ModBlocks.RADIOREC.get()).build(null));

    public static final RegistrySupplier<BlockEntityType<com.hbm_m.blockentity.network.RadioTelexBlockEntity>> RADIO_TELEX_BE =
            BLOCK_ENTITIES.register("radio_telex_be", () ->
                    BlockEntityType.Builder.of(com.hbm_m.blockentity.network.RadioTelexBlockEntity::new, ModBlocks.RADIO_TELEX.get()).build(null));

    public static final RegistrySupplier<BlockEntityType<com.hbm_m.blockentity.network.RadioAutocalBlockEntity>> RADIO_AUTOCAL_BE =
            BLOCK_ENTITIES.register("radio_autocal_be", () ->
                    BlockEntityType.Builder.of(com.hbm_m.blockentity.network.RadioAutocalBlockEntity::new, ModBlocks.RADIO_AUTOCAL.get()).build(null));

    public static final RegistrySupplier<BlockEntityType<com.hbm_m.blockentity.machines.MachineHephaestusBlockEntity>> HEPHAESTUS_BE =
            BLOCK_ENTITIES.register("hephaestus_be", () ->
                    BlockEntityType.Builder.of(com.hbm_m.blockentity.machines.MachineHephaestusBlockEntity::new, ModBlocks.HEPHAESTUS.get()).build(null));

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

    public static final RegistrySupplier<BlockEntityType<MachineRadarScreenBlockEntity>> RADAR_SCREEN_BE =
            BLOCK_ENTITIES.register("radar_screen_be", () ->
                    BlockEntityType.Builder.of(MachineRadarScreenBlockEntity::new, ModBlocks.RADAR_SCREEN.get()).build(null));

    public static final RegistrySupplier<BlockEntityType<MachineCrackingTowerBlockEntity>> CRACKING_TOWER_BE =
            BLOCK_ENTITIES.register("cracking_tower_be", () ->
                    BlockEntityType.Builder.of(MachineCrackingTowerBlockEntity::new, ModBlocks.CRACKING_TOWER.get()).build(null));

    public static final RegistrySupplier<BlockEntityType<com.hbm_m.blockentity.machines.TurretBaseBlockEntity>> TURRET_SENTRY_BE =
            BLOCK_ENTITIES.register("turret_sentry_be", () ->
                    BlockEntityType.Builder.of(com.hbm_m.blockentity.machines.TurretBaseBlockEntity::createSentry, ModBlocks.TURRET_SENTRY.get()).build(null));
    public static final RegistrySupplier<BlockEntityType<com.hbm_m.blockentity.machines.TurretBaseBlockEntity>> TURRET_CHEKHOV_BE =
            BLOCK_ENTITIES.register("turret_chekhov_be", () ->
                    BlockEntityType.Builder.of(com.hbm_m.blockentity.machines.TurretBaseBlockEntity::createChekhov, ModBlocks.TURRET_CHEKHOV.get()).build(null));
    public static final RegistrySupplier<BlockEntityType<com.hbm_m.blockentity.machines.TurretBaseBlockEntity>> TURRET_FRIENDLY_BE =
            BLOCK_ENTITIES.register("turret_friendly_be", () ->
                    BlockEntityType.Builder.of(com.hbm_m.blockentity.machines.TurretBaseBlockEntity::createFriendly, ModBlocks.TURRET_FRIENDLY.get()).build(null));
    public static final RegistrySupplier<BlockEntityType<com.hbm_m.blockentity.machines.TurretBaseBlockEntity>> TURRET_JEREMY_BE =
            BLOCK_ENTITIES.register("turret_jeremy_be", () ->
                    BlockEntityType.Builder.of(com.hbm_m.blockentity.machines.TurretBaseBlockEntity::createJeremy, ModBlocks.TURRET_JEREMY.get()).build(null));
    public static final RegistrySupplier<BlockEntityType<com.hbm_m.blockentity.machines.TurretBaseBlockEntity>> TURRET_TAUON_BE =
            BLOCK_ENTITIES.register("turret_tauon_be", () ->
                    BlockEntityType.Builder.of(com.hbm_m.blockentity.machines.TurretBaseBlockEntity::createTauon, ModBlocks.TURRET_TAUON.get()).build(null));
    public static final RegistrySupplier<BlockEntityType<com.hbm_m.blockentity.machines.TurretBaseBlockEntity>> TURRET_RICHARD_BE =
            BLOCK_ENTITIES.register("turret_richard_be", () ->
                    BlockEntityType.Builder.of(com.hbm_m.blockentity.machines.TurretBaseBlockEntity::createRichard, ModBlocks.TURRET_RICHARD.get()).build(null));
    public static final RegistrySupplier<BlockEntityType<com.hbm_m.blockentity.machines.TurretBaseBlockEntity>> TURRET_HOWARD_BE =
            BLOCK_ENTITIES.register("turret_howard_be", () ->
                    BlockEntityType.Builder.of(com.hbm_m.blockentity.machines.TurretBaseBlockEntity::createHoward, ModBlocks.TURRET_HOWARD.get()).build(null));
    public static final RegistrySupplier<BlockEntityType<com.hbm_m.blockentity.machines.TurretBaseBlockEntity>> TURRET_MAXWELL_BE =
            BLOCK_ENTITIES.register("turret_maxwell_be", () ->
                    BlockEntityType.Builder.of(com.hbm_m.blockentity.machines.TurretBaseBlockEntity::createMaxwell, ModBlocks.TURRET_MAXWELL.get()).build(null));
    public static final RegistrySupplier<BlockEntityType<com.hbm_m.blockentity.machines.TurretBaseBlockEntity>> TURRET_FRITZ_BE =
            BLOCK_ENTITIES.register("turret_fritz_be", () ->
                    BlockEntityType.Builder.of(com.hbm_m.blockentity.machines.TurretBaseBlockEntity::createFritz, ModBlocks.TURRET_FRITZ.get()).build(null));
    public static final RegistrySupplier<BlockEntityType<com.hbm_m.blockentity.machines.TurretBaseBlockEntity>> TURRET_ARTY_BE =
            BLOCK_ENTITIES.register("turret_arty_be", () ->
                    BlockEntityType.Builder.of(com.hbm_m.blockentity.machines.TurretBaseBlockEntity::createArty, ModBlocks.TURRET_ARTY.get()).build(null));
    public static final RegistrySupplier<BlockEntityType<com.hbm_m.blockentity.machines.TurretBaseBlockEntity>> TURRET_HIMARS_BE =
            BLOCK_ENTITIES.register("turret_himars_be", () ->
                    BlockEntityType.Builder.of(com.hbm_m.blockentity.machines.TurretBaseBlockEntity::createHimars, ModBlocks.TURRET_HIMARS.get()).build(null));

    public static final RegistrySupplier<BlockEntityType<com.hbm_m.blockentity.machines.MissileAssemblyBlockEntity>> MISSILE_ASSEMBLY_BE =
            BLOCK_ENTITIES.register("missile_assembly_be", () ->
                    BlockEntityType.Builder.of(com.hbm_m.blockentity.machines.MissileAssemblyBlockEntity::new, ModBlocks.MACHINE_MISSILE_ASSEMBLY.get()).build(null));

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

    public static final RegistrySupplier<BlockEntityType<com.hbm_m.blockentity.machines.MachineDifurnaceRtgBlockEntity>> MACHINE_DIFURNACE_RTG_BE =
            BLOCK_ENTITIES.register("machine_difurnace_rtg_be", () ->
                    BlockEntityType.Builder.of(com.hbm_m.blockentity.machines.MachineDifurnaceRtgBlockEntity::new,
                            ModBlocks.MACHINE_DIFURNACE_RTG.get()).build(null));

    public static final RegistrySupplier<BlockEntityType<com.hbm_m.blockentity.machines.MachineTeleporterBlockEntity>> MACHINE_TELEPORTER_BE =
            BLOCK_ENTITIES.register("machine_teleporter_be", () ->
                    BlockEntityType.Builder.of(com.hbm_m.blockentity.machines.MachineTeleporterBlockEntity::new,
                            ModBlocks.MACHINE_TELEPORTER.get()).build(null));

    public static final RegistrySupplier<BlockEntityType<com.hbm_m.blockentity.machines.MachinePrecAssBlockEntity>> MACHINE_PRECASS_BE =
		BLOCK_ENTITIES.register("machine_precass_be", () ->
			BlockEntityType.Builder.<com.hbm_m.blockentity.machines.MachinePrecAssBlockEntity>of(com.hbm_m.blockentity.machines.MachinePrecAssBlockEntity::new, ModBlocks.MACHINE_PRECASS.get())
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

    public static final RegistrySupplier<BlockEntityType<MachineOreSlopperBlockEntity>> ORE_SLOPPER_BE =
            BLOCK_ENTITIES.register("ore_slopper_be", () ->
                    BlockEntityType.Builder.of(MachineOreSlopperBlockEntity::new,
                            ModBlocks.ORE_SLOPPER.get()).build(null));

    public static final RegistrySupplier<BlockEntityType<com.hbm_m.blockentity.machines.MachineCombinationOvenBlockEntity>> COMBINATION_OVEN_BE =
            BLOCK_ENTITIES.register("combination_oven_be", () ->
                    BlockEntityType.Builder.of(com.hbm_m.blockentity.machines.MachineCombinationOvenBlockEntity::new,
                            ModBlocks.COMBINATION_OVEN.get()).build(null));

    public static final RegistrySupplier<BlockEntityType<com.hbm_m.blockentity.machines.MachineArcFurnaceBlockEntity>> ARC_FURNACE_BE =
            BLOCK_ENTITIES.register("arc_furnace_be", () ->
                    BlockEntityType.Builder.of(com.hbm_m.blockentity.machines.MachineArcFurnaceBlockEntity::new,
                            ModBlocks.ARC_FURNACE.get()).build(null));

    public static final RegistrySupplier<BlockEntityType<com.hbm_m.blockentity.machines.MachineAnnihilatorBlockEntity>> ANNIHILATOR_BE =
            BLOCK_ENTITIES.register("annihilator_be", () ->
                    BlockEntityType.Builder.of(com.hbm_m.blockentity.machines.MachineAnnihilatorBlockEntity::new,
                            ModBlocks.ANNIHILATOR.get()).build(null));

    public static final RegistrySupplier<BlockEntityType<com.hbm_m.blockentity.machines.MachineMiningLaserBlockEntity>> MINING_LASER_BE =
            BLOCK_ENTITIES.register("mining_laser_be", () ->
                    BlockEntityType.Builder.of(com.hbm_m.blockentity.machines.MachineMiningLaserBlockEntity::new,
                            ModBlocks.MINING_LASER.get()).build(null));

    public static final RegistrySupplier<BlockEntityType<com.hbm_m.blockentity.machines.MachineAmmoPressBlockEntity>> AMMO_PRESS_BE =
            BLOCK_ENTITIES.register("ammo_press_be", () ->
                    BlockEntityType.Builder.of(com.hbm_m.blockentity.machines.MachineAmmoPressBlockEntity::new,
                            ModBlocks.AMMO_PRESS.get()).build(null));

    public static final RegistrySupplier<BlockEntityType<com.hbm_m.blockentity.machines.MachineEPressBlockEntity>> EPRESS_BE =
            BLOCK_ENTITIES.register("epress_be", () ->
                    BlockEntityType.Builder.of(com.hbm_m.blockentity.machines.MachineEPressBlockEntity::new,
                            ModBlocks.EPRESS.get()).build(null));

    public static final RegistrySupplier<BlockEntityType<com.hbm_m.blockentity.machines.MachineConveyorPressBlockEntity>> CONVEYOR_PRESS_BE =
            BLOCK_ENTITIES.register("conveyor_press_be", () ->
                    BlockEntityType.Builder.of(com.hbm_m.blockentity.machines.MachineConveyorPressBlockEntity::new,
                            ModBlocks.CONVEYOR_PRESS.get()).build(null));

    public static final RegistrySupplier<BlockEntityType<com.hbm_m.blockentity.machines.MachineAutocrafterBlockEntity>> AUTOCRAFTER_BE =
            BLOCK_ENTITIES.register("autocrafter_be", () ->
                    BlockEntityType.Builder.of(com.hbm_m.blockentity.machines.MachineAutocrafterBlockEntity::new,
                            ModBlocks.MACHINE_AUTOCRAFTER.get()).build(null));

    public static final RegistrySupplier<BlockEntityType<com.hbm_m.blockentity.machines.MachineFunnelBlockEntity>> FUNNEL_BE =
            BLOCK_ENTITIES.register("funnel_be", () ->
                    BlockEntityType.Builder.of(com.hbm_m.blockentity.machines.MachineFunnelBlockEntity::new,
                            ModBlocks.MACHINE_FUNNEL.get()).build(null));

    public static final RegistrySupplier<BlockEntityType<com.hbm_m.blockentity.machines.MachinePUREXBlockEntity>> PUREX_BE =
            BLOCK_ENTITIES.register("purex_be", () ->
                    BlockEntityType.Builder.of(com.hbm_m.blockentity.machines.MachinePUREXBlockEntity::new,
                            ModBlocks.PUREX.get()).build(null));

    public static final RegistrySupplier<BlockEntityType<com.hbm_m.blockentity.machines.MachineCombustionEngineBlockEntity>> COMBUSTION_ENGINE_BE =
            BLOCK_ENTITIES.register("combustion_engine_be", () ->
                    BlockEntityType.Builder.of(com.hbm_m.blockentity.machines.MachineCombustionEngineBlockEntity::new,
                            ModBlocks.COMBUSTION_ENGINE.get()).build(null));

    public static final RegistrySupplier<BlockEntityType<com.hbm_m.blockentity.machines.MachineDieselGeneratorBlockEntity>> DIESEL_GENERATOR_BE =
            BLOCK_ENTITIES.register("dieselgen_be", () ->
                    BlockEntityType.Builder.of(com.hbm_m.blockentity.machines.MachineDieselGeneratorBlockEntity::new,
                            ModBlocks.DIESELGEN.get()).build(null));

    public static final RegistrySupplier<BlockEntityType<com.hbm_m.blockentity.machines.MachineChimneyBlockEntity>> CHIMNEY_BE =
            BLOCK_ENTITIES.register("chimney_be", () ->
                    BlockEntityType.Builder.of(com.hbm_m.blockentity.machines.MachineChimneyBlockEntity::new,
                            ModBlocks.CHIMNEY_BRICK.get(), ModBlocks.CHIMNEY_INDUSTRIAL.get()).build(null));

    public static final RegistrySupplier<BlockEntityType<com.hbm_m.blockentity.machines.MachineBoilerBlockEntity>> BOILER_BE =
            BLOCK_ENTITIES.register("boiler_be", () ->
                    BlockEntityType.Builder.of(com.hbm_m.blockentity.machines.MachineBoilerBlockEntity::new,
                            ModBlocks.BOILER.get()).build(null));

    public static final RegistrySupplier<BlockEntityType<com.hbm_m.blockentity.machines.MachineCapacitorBlockEntity>> MACHINE_CAPACITOR_BE =
            BLOCK_ENTITIES.register("machine_capacitor_be", () ->
                    BlockEntityType.Builder.of(com.hbm_m.blockentity.machines.MachineCapacitorBlockEntity::new,
                            ModBlocks.CAPACITOR_BUS.get(), ModBlocks.CAPACITOR_COPPER.get(), ModBlocks.CAPACITOR_GOLD.get(),
                            ModBlocks.CAPACITOR_NIOBIUM.get(), ModBlocks.CAPACITOR_SCHRABIDATE.get(), ModBlocks.CAPACITOR_TANTALIUM.get())
                            .build(null));

    public static final RegistrySupplier<BlockEntityType<com.hbm_m.blockentity.machines.CargoElevatorBlockEntity>> CARGO_ELEVATOR_BE =
            BLOCK_ENTITIES.register("cargo_elevator_be", () ->
                    BlockEntityType.Builder.of(com.hbm_m.blockentity.machines.CargoElevatorBlockEntity::new,
                            ModBlocks.CARGO_ELEVATOR.get()).build(null));

    public static final RegistrySupplier<BlockEntityType<com.hbm_m.blockentity.machines.MachineRtgBlockEntity>> MACHINE_RTG_BE =
            BLOCK_ENTITIES.register("machine_rtg_be", () ->
                    BlockEntityType.Builder.of(com.hbm_m.blockentity.machines.MachineRtgBlockEntity::new,
                            ModBlocks.MACHINE_RTG.get()).build(null));

    public static final RegistrySupplier<BlockEntityType<com.hbm_m.blockentity.machines.MachineDrainBlockEntity>> MACHINE_DRAIN_BE =
            BLOCK_ENTITIES.register("machine_drain_be", () ->
                    BlockEntityType.Builder.of(com.hbm_m.blockentity.machines.MachineDrainBlockEntity::new,
                            ModBlocks.MACHINE_DRAIN.get()).build(null));

    public static final RegistrySupplier<BlockEntityType<com.hbm_m.blockentity.machines.MachineFanBlockEntity>> MACHINE_FAN_BE =
            BLOCK_ENTITIES.register("machine_fan_be", () ->
                    BlockEntityType.Builder.of(com.hbm_m.blockentity.machines.MachineFanBlockEntity::new,
                            ModBlocks.MACHINE_FAN.get()).build(null));

    public static final RegistrySupplier<BlockEntityType<com.hbm_m.blockentity.machines.MachineWasteDrumBlockEntity>> MACHINE_WASTE_DRUM_BE =
            BLOCK_ENTITIES.register("machine_waste_drum_be", () ->
                    BlockEntityType.Builder.of(com.hbm_m.blockentity.machines.MachineWasteDrumBlockEntity::new,
                            ModBlocks.MACHINE_WASTE_DRUM.get()).build(null));

    public static final RegistrySupplier<BlockEntityType<com.hbm_m.blockentity.machines.MachineKeyforgeBlockEntity>> MACHINE_KEYFORGE_BE =
            BLOCK_ENTITIES.register("machine_keyforge_be", () ->
                    BlockEntityType.Builder.of(com.hbm_m.blockentity.machines.MachineKeyforgeBlockEntity::new,
                            ModBlocks.MACHINE_KEYFORGE.get()).build(null));

    public static final RegistrySupplier<BlockEntityType<com.hbm_m.blockentity.machines.MachineMassStorageBlockEntity>> MACHINE_MASS_STORAGE_BE =
            BLOCK_ENTITIES.register("mass_storage_be", () ->
                    BlockEntityType.Builder.of(com.hbm_m.blockentity.machines.MachineMassStorageBlockEntity::new,
                            ModBlocks.MASS_STORAGE.get()).build(null));

    public static final RegistrySupplier<BlockEntityType<com.hbm_m.blockentity.machines.MachineElectricHeaterBlockEntity>> ELECTRIC_HEATER_BE =
            BLOCK_ENTITIES.register("electric_heater_be", () ->
                    BlockEntityType.Builder.of(com.hbm_m.blockentity.machines.MachineElectricHeaterBlockEntity::new,
                            ModBlocks.ELECTRIC_HEATER.get()).build(null));

    public static final RegistrySupplier<BlockEntityType<com.hbm_m.blockentity.machines.MachineFireboxBlockEntity>> FIREBOX_BE =
            BLOCK_ENTITIES.register("firebox_be", () ->
                    BlockEntityType.Builder.of(com.hbm_m.blockentity.machines.MachineFireboxBlockEntity::new,
                            ModBlocks.FIREBOX.get()).build(null));

    public static final RegistrySupplier<BlockEntityType<com.hbm_m.blockentity.machines.MachineOilburnerBlockEntity>> OILBURNER_BE =
            BLOCK_ENTITIES.register("oilburner_be", () ->
                    BlockEntityType.Builder.of(com.hbm_m.blockentity.machines.MachineOilburnerBlockEntity::new,
                            ModBlocks.OILBURNER.get(), ModBlocks.OILBURNER_HP.get()).build(null));

    public static final RegistrySupplier<BlockEntityType<com.hbm_m.blockentity.machines.MachineHeatexBlockEntity>> HEATEX_BE =
            BLOCK_ENTITIES.register("heatex_be", () ->
                    BlockEntityType.Builder.of(com.hbm_m.blockentity.machines.MachineHeatexBlockEntity::new,
                            ModBlocks.HEATEX.get()).build(null));

    public static final RegistrySupplier<BlockEntityType<com.hbm_m.blockentity.machines.MachineSteamEngineBlockEntity>> STEAM_ENGINE_BE =
            BLOCK_ENTITIES.register("steam_engine_be", () ->
                    BlockEntityType.Builder.of(com.hbm_m.blockentity.machines.MachineSteamEngineBlockEntity::new,
                            ModBlocks.STEAM_ENGINE.get()).build(null));

    public static final RegistrySupplier<BlockEntityType<com.hbm_m.blockentity.machines.MachineIndustrialGeneratorBlockEntity>> INDUSTRIAL_GENERATOR_BE =
            BLOCK_ENTITIES.register("industrial_generator_be", () ->
                    BlockEntityType.Builder.of(com.hbm_m.blockentity.machines.MachineIndustrialGeneratorBlockEntity::new,
                            ModBlocks.INDUSTRIAL_GENERATOR.get()).build(null));

    public static final RegistrySupplier<BlockEntityType<com.hbm_m.blockentity.machines.MachineStirlingBlockEntity>> STIRLING_BE =
            BLOCK_ENTITIES.register("stirling_be", () ->
                    BlockEntityType.Builder.of(com.hbm_m.blockentity.machines.MachineStirlingBlockEntity::new,
                            ModBlocks.STIRLING.get(), ModBlocks.STIRLING_STEEL.get(), ModBlocks.STIRLING_CREATIVE.get()).build(null));

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

	public static final RegistrySupplier<BlockEntityType<MachineElectricFurnaceBlockEntity>> ELECTRIC_FURNACE_BE =
			BLOCK_ENTITIES.register("electric_furnace_be", () ->
					BlockEntityType.Builder.of(MachineElectricFurnaceBlockEntity::new,
							ModBlocks.ELECTRIC_FURNACE.get()).build(null));

	public static final RegistrySupplier<BlockEntityType<MachineFurnaceBrickBlockEntity>> FURNACE_BRICK_BE =
			BLOCK_ENTITIES.register("furnace_brick_be", () ->
					BlockEntityType.Builder.of(MachineFurnaceBrickBlockEntity::new,
							ModBlocks.FURNACE_BRICK.get()).build(null));

	public static final RegistrySupplier<BlockEntityType<com.hbm_m.blockentity.machines.MachineMicrowaveBlockEntity>> MICROWAVE_BE =
			BLOCK_ENTITIES.register("microwave_be", () ->
					BlockEntityType.Builder.of(com.hbm_m.blockentity.machines.MachineMicrowaveBlockEntity::new,
							ModBlocks.MICROWAVE.get()).build(null));

	public static final RegistrySupplier<BlockEntityType<com.hbm_m.blockentity.machines.MachineExposureChamberBlockEntity>> EXPOSURE_CHAMBER_BE =
			BLOCK_ENTITIES.register("exposure_chamber_be", () ->
					BlockEntityType.Builder.of(com.hbm_m.blockentity.machines.MachineExposureChamberBlockEntity::new,
							ModBlocks.EXPOSURE_CHAMBER.get()).build(null));

	public static final RegistrySupplier<BlockEntityType<com.hbm_m.blockentity.machines.MachineRadiolysisBlockEntity>> RADIOLYSIS_BE =
			BLOCK_ENTITIES.register("radiolysis_be", () ->
					BlockEntityType.Builder.of(com.hbm_m.blockentity.machines.MachineRadiolysisBlockEntity::new,
							ModBlocks.RADIOLYSIS.get()).build(null));

	public static final RegistrySupplier<BlockEntityType<com.hbm_m.blockentity.machines.MachineElectrolyserBlockEntity>> ELECTROLYSER_BE =
			BLOCK_ENTITIES.register("electrolyser_be", () ->
					BlockEntityType.Builder.of(com.hbm_m.blockentity.machines.MachineElectrolyserBlockEntity::new,
							ModBlocks.ELECTROLYSER.get()).build(null));

	public static final RegistrySupplier<BlockEntityType<com.hbm_m.blockentity.machines.MachineCompressorBlockEntity>> COMPRESSOR_BE =
			BLOCK_ENTITIES.register("compressor_be", () ->
					BlockEntityType.Builder.of(com.hbm_m.blockentity.machines.MachineCompressorBlockEntity::new,
							ModBlocks.COMPRESSOR.get(), ModBlocks.MACHINE_COMPRESSOR_COMPACT.get()).build(null));

	public static final RegistrySupplier<BlockEntityType<com.hbm_m.blockentity.machines.MachineSawmillBlockEntity>> SAWMILL_BE =
			BLOCK_ENTITIES.register("sawmill_be", () ->
					BlockEntityType.Builder.of(com.hbm_m.blockentity.machines.MachineSawmillBlockEntity::new,
							ModBlocks.SAWMILL.get()).build(null));

	public static final RegistrySupplier<BlockEntityType<com.hbm_m.blockentity.machines.MachineAutosawBlockEntity>> AUTOSAW_BE =
			BLOCK_ENTITIES.register("autosaw_be", () ->
					BlockEntityType.Builder.of(com.hbm_m.blockentity.machines.MachineAutosawBlockEntity::new,
							ModBlocks.AUTOSAW.get()).build(null));

	public static final RegistrySupplier<BlockEntityType<com.hbm_m.blockentity.machines.MachineThresherBlockEntity>> THRESHER_BE =
			BLOCK_ENTITIES.register("thresher_be", () ->
					BlockEntityType.Builder.of(com.hbm_m.blockentity.machines.MachineThresherBlockEntity::new,
							ModBlocks.THRESHER.get()).build(null));

	public static final RegistrySupplier<BlockEntityType<MachineFurnaceIronBlockEntity>> FURNACE_IRON_BE =
			BLOCK_ENTITIES.register("furnace_iron_be", () ->
					BlockEntityType.Builder.of(MachineFurnaceIronBlockEntity::new,
							ModBlocks.FURNACE_IRON.get()).build(null));

	public static final RegistrySupplier<BlockEntityType<MachineFurnaceSteelBlockEntity>> FURNACE_STEEL_BE =
			BLOCK_ENTITIES.register("furnace_steel_be", () ->
					BlockEntityType.Builder.of(MachineFurnaceSteelBlockEntity::new,
							ModBlocks.FURNACE_STEEL.get()).build(null));

	public static final RegistrySupplier<BlockEntityType<com.hbm_m.blockentity.machines.MachineRotaryFurnaceBlockEntity>> ROTARY_FURNACE_BE =
			BLOCK_ENTITIES.register("rotary_furnace_be", () ->
					BlockEntityType.Builder.of(com.hbm_m.blockentity.machines.MachineRotaryFurnaceBlockEntity::new,
							ModBlocks.ROTARY_FURNACE.get()).build(null));

	public static final RegistrySupplier<BlockEntityType<com.hbm_m.blockentity.machines.MachineStrandCasterBlockEntity>> STRAND_CASTER_BE =
			BLOCK_ENTITIES.register("strand_caster_be", () ->
					BlockEntityType.Builder.of(com.hbm_m.blockentity.machines.MachineStrandCasterBlockEntity::new,
							ModBlocks.STRAND_CASTER.get()).build(null));

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

    public static final RegistrySupplier<BlockEntityType<com.hbm_m.blockentity.machines.JAS39TrophyBlockEntity>> JAS39_TROPHY_BE =
            BLOCK_ENTITIES.register("jas39_trophy_be", () ->
                    BlockEntityType.Builder.of(com.hbm_m.blockentity.machines.JAS39TrophyBlockEntity::new,
                            ModBlocks.JAS39_TROPHY.get()).build(null));

    public static final RegistrySupplier<BlockEntityType<com.hbm_m.blockentity.machines.MachineFoundryMoldBlockEntity>> FOUNDRY_MOLD_BE =
            BLOCK_ENTITIES.register("foundry_mold_be", () ->
                    BlockEntityType.Builder.of(com.hbm_m.blockentity.machines.MachineFoundryMoldBlockEntity::new, ModBlocks.FOUNDRY_MOLD.get()).build(null));

    public static final RegistrySupplier<BlockEntityType<com.hbm_m.blockentity.machines.MachineFoundrySlagtapBlockEntity>> FOUNDRY_SLAGTAP_BE =
            BLOCK_ENTITIES.register("foundry_slagtap_be", () ->
                    BlockEntityType.Builder.of(com.hbm_m.blockentity.machines.MachineFoundrySlagtapBlockEntity::new, ModBlocks.FOUNDRY_SLAGTAP.get()).build(null));

    public static final RegistrySupplier<BlockEntityType<com.hbm_m.blockentity.machines.MachineFoundryTankBlockEntity>> FOUNDRY_TANK_BE =
            BLOCK_ENTITIES.register("foundry_tank_be", () ->
                    BlockEntityType.Builder.of(com.hbm_m.blockentity.machines.MachineFoundryTankBlockEntity::new, ModBlocks.FOUNDRY_TANK.get()).build(null));

    public static final RegistrySupplier<BlockEntityType<com.hbm_m.blockentity.machines.SlagBlockEntity>> SLAG_BE =
            BLOCK_ENTITIES.register("slag_be", () ->
                    BlockEntityType.Builder.of(com.hbm_m.blockentity.machines.SlagBlockEntity::new, ModBlocks.SLAG_DYNAMIC.get()).build(null));

    public static final RegistrySupplier<BlockEntityType<com.hbm_m.blockentity.machines.MachineFoundryOutletBlockEntity>> FOUNDRY_OUTLET_BE =
            BLOCK_ENTITIES.register("foundry_outlet_be", () ->
                    BlockEntityType.Builder.of(com.hbm_m.blockentity.machines.MachineFoundryOutletBlockEntity::new,
                            ModBlocks.FOUNDRY_OUTLET.get()).build(null));

    public static final RegistrySupplier<BlockEntityType<MachineFluidTankBlockEntity>> FLUID_TANK_BE =
            BLOCK_ENTITIES.register("fluid_tank_be", () ->
                    BlockEntityType.Builder.of(MachineFluidTankBlockEntity::new,
                            ModBlocks.FLUID_TANK.get()).build(null));

    public static final RegistrySupplier<BlockEntityType<com.hbm_m.blockentity.machines.Bat9000BlockEntity>> BAT9000_BE =
            BLOCK_ENTITIES.register("bat9000_be", () ->
                    BlockEntityType.Builder.of(com.hbm_m.blockentity.machines.Bat9000BlockEntity::new,
                            ModBlocks.BAT9000.get()).build(null));

    public static final RegistrySupplier<BlockEntityType<com.hbm_m.blockentity.machines.OrbusBlockEntity>> ORBUS_BE =
            BLOCK_ENTITIES.register("orbus_be", () ->
                    BlockEntityType.Builder.of(com.hbm_m.blockentity.machines.OrbusBlockEntity::new,
                            ModBlocks.ORBUS.get()).build(null));

    public static final RegistrySupplier<BlockEntityType<com.hbm_m.blockentity.machines.BarrelIronBlockEntity>> BARREL_IRON_BE =
            BLOCK_ENTITIES.register("barrel_iron_be", () ->
                    BlockEntityType.Builder.of(com.hbm_m.blockentity.machines.BarrelIronBlockEntity::new,
                            ModBlocks.BARREL_IRON.get()).build(null));

    public static final RegistrySupplier<BlockEntityType<com.hbm_m.blockentity.machines.BarrelSteelBlockEntity>> BARREL_STEEL_BE =
            BLOCK_ENTITIES.register("barrel_steel_be", () ->
                    BlockEntityType.Builder.of(com.hbm_m.blockentity.machines.BarrelSteelBlockEntity::new,
                            ModBlocks.BARREL_STEEL.get()).build(null));

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

    public static final RegistrySupplier<BlockEntityType<com.hbm_m.blockentity.machines.PWRControllerBlockEntity>> PWR_CONTROLLER_BE =
            BLOCK_ENTITIES.register("pwr_controller_be", () ->
                    BlockEntityType.Builder.of(com.hbm_m.blockentity.machines.PWRControllerBlockEntity::new,
                            ModBlocks.PWR_CONTROLLER.get()).build(null));

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

    public static final RegistrySupplier<BlockEntityType<MachineChungusBlockEntity>> MACHINE_CHUNGUS_BE =
            BLOCK_ENTITIES.register("machine_chungus_be", () ->
                    BlockEntityType.Builder.of(MachineChungusBlockEntity::new,
                            ModBlocks.MACHINE_CHUNGUS.get()).build(null));

    public static final RegistrySupplier<BlockEntityType<MachineTurbineBlockEntity>> TURBINE_BE =
            BLOCK_ENTITIES.register("turbine_be", () ->
                    BlockEntityType.Builder.of(MachineTurbineBlockEntity::new,
                            ModBlocks.TURBINE.get()).build(null));

    public static final RegistrySupplier<BlockEntityType<com.hbm_m.blockentity.machines.MachineLargeTurbineBlockEntity>> LARGE_TURBINE_BE =
            BLOCK_ENTITIES.register("large_turbine_be", () ->
                    BlockEntityType.Builder.of(com.hbm_m.blockentity.machines.MachineLargeTurbineBlockEntity::new,
                            ModBlocks.MACHINE_LARGE_TURBINE.get()).build(null));

    public static final RegistrySupplier<BlockEntityType<com.hbm_m.blockentity.machines.MachineTurbineGasBlockEntity>> TURBINEGAS_BE =
            BLOCK_ENTITIES.register("turbinegas_be", () ->
                    BlockEntityType.Builder.of(com.hbm_m.blockentity.machines.MachineTurbineGasBlockEntity::new,
                            ModBlocks.TURBINEGAS.get()).build(null));

    public static final RegistrySupplier<BlockEntityType<com.hbm_m.blockentity.machines.MachineCondenserPoweredBlockEntity>> CONDENSER_POWERED_BE =
            BLOCK_ENTITIES.register("condenser_powered_be", () ->
                    BlockEntityType.Builder.of(com.hbm_m.blockentity.machines.MachineCondenserPoweredBlockEntity::new,
                            ModBlocks.CONDENSER_POWERED.get()).build(null));

    public static final RegistrySupplier<BlockEntityType<com.hbm_m.blockentity.machines.MachineLpw2BlockEntity>> LPW2_BE =
            BLOCK_ENTITIES.register("lpw2_be", () ->
                    BlockEntityType.Builder.of(com.hbm_m.blockentity.machines.MachineLpw2BlockEntity::new,
                            ModBlocks.LPW2.get()).build(null));

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

    public static final RegistrySupplier<BlockEntityType<com.hbm_m.blockentity.machines.MachineConverterHeRfBlockEntity>> MACHINE_CONVERTER_HE_RF_BE =
            BLOCK_ENTITIES.register("machine_converter_he_rf_be",
                    () -> BlockEntityType.Builder.of(com.hbm_m.blockentity.machines.MachineConverterHeRfBlockEntity::new, ModBlocks.MACHINE_CONVERTER_HE_RF.get()).build(null));

    public static final RegistrySupplier<BlockEntityType<com.hbm_m.blockentity.machines.MachineConverterRfHeBlockEntity>> MACHINE_CONVERTER_RF_HE_BE =
            BLOCK_ENTITIES.register("machine_converter_rf_he_be",
                    () -> BlockEntityType.Builder.of(com.hbm_m.blockentity.machines.MachineConverterRfHeBlockEntity::new, ModBlocks.MACHINE_CONVERTER_RF_HE.get()).build(null));

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
                            ModBlocks.RBMK_ROD.get(), ModBlocks.RBMK_ROD_MOD.get(),
                            ModBlocks.RBMK_ROD_REASIM.get(), ModBlocks.RBMK_ROD_REASIM_MOD.get()).build(null));

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
                            ModBlocks.RBMK_TERMINAL.get(), ModBlocks.RBMK_KEYPAD.get(),
                            ModBlocks.RBMK_DISPLAY_BLANK.get()).build(null));

    public static final RegistrySupplier<BlockEntityType<com.hbm_m.blockentity.machines.SoyuzLauncherBlockEntity>> SOYUZ_LAUNCHER_BE =
            BLOCK_ENTITIES.register("soyuz_launcher_be", () ->
                    BlockEntityType.Builder.of(com.hbm_m.blockentity.machines.SoyuzLauncherBlockEntity::new,
                            ModBlocks.SOYUZ_LAUNCHER.get()).build(null));

    public static final RegistrySupplier<BlockEntityType<com.hbm_m.block.entity.decorations.SoyuzRocketBlockEntity>> DECO_SOYUZ_ROCKET_BE =
            BLOCK_ENTITIES.register("deco_soyuz_rocket_be", () ->
                    BlockEntityType.Builder.of(com.hbm_m.block.entity.decorations.SoyuzRocketBlockEntity::new,
                            ModBlocks.DECO_SOYUZ_ROCKET.get()).build(null));

    public static final RegistrySupplier<BlockEntityType<com.hbm_m.blockentity.machines.MachineSatLinkerBlockEntity>> MACHINE_SATLINKER_BE =
            BLOCK_ENTITIES.register("machine_satlinker_be", () ->
                    BlockEntityType.Builder.of(com.hbm_m.blockentity.machines.MachineSatLinkerBlockEntity::new,
                            ModBlocks.MACHINE_SATLINKER.get()).build(null));

    public static void init() {
        BLOCK_ENTITIES.register();
    }
}
