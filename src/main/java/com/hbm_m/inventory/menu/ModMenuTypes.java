package com.hbm_m.inventory.menu;

import com.hbm_m.armormod.menu.ArmorTableMenu;
import com.hbm_m.lib.RefStrings;

import dev.architectury.registry.menu.MenuRegistry;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;

public class ModMenuTypes {

    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(RefStrings.MODID, Registries.MENU);

    public static final RegistrySupplier<MenuType<MachineCrystallizerMenu>> CRYSTALLIZER_MENU =
            MENUS.register("crystallizer_menu", () -> MenuRegistry.ofExtended(MachineCrystallizerMenu::new));

    public static final RegistrySupplier<MenuType<MachineLargeTurbineMenu>> LARGE_TURBINE_MENU =
            MENUS.register("large_turbine_menu", () -> MenuRegistry.ofExtended(MachineLargeTurbineMenu::new));

    public static final RegistrySupplier<MenuType<MachineTurbineGasMenu>> TURBINEGAS_MENU =
            MENUS.register("turbinegas_menu", () -> MenuRegistry.ofExtended(MachineTurbineGasMenu::new));

    public static final RegistrySupplier<MenuType<ArmorTableMenu>> ARMOR_TABLE_MENU =
            MENUS.register("armor_table_menu", () -> MenuRegistry.ofExtended(ArmorTableMenu::new));

    public static final RegistrySupplier<MenuType<MachineBreederMenu>> BREEDER_MENU =
            MENUS.register("breeder_menu", () -> MenuRegistry.ofExtended(MachineBreederMenu::new));

    public static final RegistrySupplier<MenuType<MachineLargePylonMenu>> LARGE_PYLON_MENU =
            MENUS.register("large_pylon_menu", () -> MenuRegistry.ofExtended(MachineLargePylonMenu::new));

    public static final RegistrySupplier<MenuType<MachineAssemblerMenu>> MACHINE_ASSEMBLER_MENU =
            MENUS.register("machine_assembler_menu", () -> MenuRegistry.ofExtended(MachineAssemblerMenu::new));

    public static final RegistrySupplier<MenuType<MachineAdvancedAssemblerMenu>> ADVANCED_ASSEMBLY_MACHINE_MENU =
            MENUS.register("advanced_assembly_machine_menu", () -> MenuRegistry.ofExtended(MachineAdvancedAssemblerMenu::new));

    public static final RegistrySupplier<MenuType<MachineDifurnaceRtgMenu>> MACHINE_DIFURNACE_RTG_MENU =
            MENUS.register("machine_difurnace_rtg_menu", () -> MenuRegistry.ofExtended(MachineDifurnaceRtgMenu::new));

    public static final RegistrySupplier<MenuType<MachinePrecAssMenu>> MACHINE_PRECASS_MENU =
            MENUS.register("machine_precass_menu", () -> MenuRegistry.ofExtended(MachinePrecAssMenu::new));

    public static final RegistrySupplier<MenuType<MachineBatteryMenu>> MACHINE_BATTERY_MENU =
            MENUS.register("machine_battery_menu", () -> MenuRegistry.ofExtended(MachineBatteryMenu::new));

    public static final RegistrySupplier<MenuType<BatterySocketMenu>> BATTERY_SOCKET_MENU =
            MENUS.register("battery_socket_menu", () -> MenuRegistry.ofExtended(BatterySocketMenu::new));

    public static final RegistrySupplier<MenuType<BlastFurnaceMenu>> BLAST_FURNACE_MENU =
            MENUS.register("blast_furnace_menu", () -> MenuRegistry.ofExtended(BlastFurnaceMenu::new));

    public static final RegistrySupplier<MenuType<MachinePressMenu>> PRESS_MENU =
            MENUS.register("press_menu", () -> MenuRegistry.ofExtended(MachinePressMenu::new));

    public static final RegistrySupplier<MenuType<AnvilMenu>> ANVIL_MENU =
            MENUS.register("anvil_menu", () -> MenuRegistry.ofExtended(AnvilMenu::new));

    public static final RegistrySupplier<MenuType<MachineWoodBurnerMenu>> WOOD_BURNER_MENU =
            MENUS.register("wood_burner_menu", () -> MenuRegistry.ofExtended(MachineWoodBurnerMenu::new));

    public static final RegistrySupplier<MenuType<MachineShredderMenu>> SHREDDER_MENU =
            MENUS.register("shredder_menu", () -> MenuRegistry.ofExtended(MachineShredderMenu::new));

    public static final RegistrySupplier<MenuType<MachineOreSlopperMenu>> ORE_SLOPPER_MENU =
            MENUS.register("ore_slopper_menu", () -> MenuRegistry.ofExtended(MachineOreSlopperMenu::new));

    public static final RegistrySupplier<MenuType<MachineCombinationOvenMenu>> COMBINATION_OVEN_MENU =
            MENUS.register("combination_oven_menu", () -> MenuRegistry.ofExtended(MachineCombinationOvenMenu::new));

    public static final RegistrySupplier<MenuType<MachineArcFurnaceMenu>> ARC_FURNACE_MENU =
            MENUS.register("arc_furnace_menu", () -> MenuRegistry.ofExtended(MachineArcFurnaceMenu::new));

    public static final RegistrySupplier<MenuType<com.hbm_m.inventory.menu.MachineAnnihilatorMenu>> ANNIHILATOR_MENU =
            MENUS.register("annihilator_menu", () -> MenuRegistry.ofExtended(com.hbm_m.inventory.menu.MachineAnnihilatorMenu::new));

    public static final RegistrySupplier<MenuType<com.hbm_m.inventory.menu.MachineMiningLaserMenu>> MINING_LASER_MENU =
            MENUS.register("mining_laser_menu", () -> MenuRegistry.ofExtended(com.hbm_m.inventory.menu.MachineMiningLaserMenu::new));

    public static final RegistrySupplier<MenuType<com.hbm_m.inventory.menu.MachineAmmoPressMenu>> AMMO_PRESS_MENU =
            MENUS.register("ammo_press_menu", () -> MenuRegistry.ofExtended(com.hbm_m.inventory.menu.MachineAmmoPressMenu::new));

    public static final RegistrySupplier<MenuType<com.hbm_m.inventory.menu.MachineEPressMenu>> EPRESS_MENU =
            MENUS.register("epress_menu", () -> MenuRegistry.ofExtended(com.hbm_m.inventory.menu.MachineEPressMenu::new));

    public static final RegistrySupplier<MenuType<com.hbm_m.inventory.menu.MachineCombustionEngineMenu>> COMBUSTION_ENGINE_MENU =
            MENUS.register("combustion_engine_menu", () -> MenuRegistry.ofExtended(com.hbm_m.inventory.menu.MachineCombustionEngineMenu::new));

    public static final RegistrySupplier<MenuType<com.hbm_m.inventory.menu.MachineDieselGeneratorMenu>> DIESEL_GENERATOR_MENU =
            MENUS.register("dieselgen_menu", () -> MenuRegistry.ofExtended(com.hbm_m.inventory.menu.MachineDieselGeneratorMenu::new));

    public static final RegistrySupplier<MenuType<com.hbm_m.inventory.menu.MachineIndustrialGeneratorMenu>> INDUSTRIAL_GENERATOR_MENU =
            MENUS.register("industrial_generator_menu", () -> MenuRegistry.ofExtended(com.hbm_m.inventory.menu.MachineIndustrialGeneratorMenu::new));

    public static final RegistrySupplier<MenuType<com.hbm_m.inventory.menu.MachineAutocrafterMenu>> AUTOCRAFTER_MENU =
            MENUS.register("autocrafter_menu", () -> MenuRegistry.ofExtended(com.hbm_m.inventory.menu.MachineAutocrafterMenu::new));

    public static final RegistrySupplier<MenuType<com.hbm_m.inventory.menu.MachineFunnelMenu>> FUNNEL_MENU =
            MENUS.register("funnel_menu", () -> MenuRegistry.ofExtended(com.hbm_m.inventory.menu.MachineFunnelMenu::new));

    public static final RegistrySupplier<MenuType<com.hbm_m.inventory.menu.MachinePUREXMenu>> PUREX_MENU =
            MENUS.register("purex_menu", () -> MenuRegistry.ofExtended(com.hbm_m.inventory.menu.MachinePUREXMenu::new));

    public static final RegistrySupplier<MenuType<MachineCentrifugeMenu>> CENTRIFUGE_MENU =
            MENUS.register("centrifuge_menu", () -> MenuRegistry.ofExtended(MachineCentrifugeMenu::new));

    public static final RegistrySupplier<MenuType<MachineCrucibleMenu>> CRUCIBLE_MENU =
            MENUS.register("crucible_menu", () -> MenuRegistry.ofExtended(MachineCrucibleMenu::new));

    public static final RegistrySupplier<MenuType<MachineGasCentrifugeMenu>> GAS_CENTRIFUGE_MENU =
            MENUS.register("gas_centrifuge_menu", () -> MenuRegistry.ofExtended(MachineGasCentrifugeMenu::new));

    public static final RegistrySupplier<MenuType<IronCrateMenu>> IRON_CRATE_MENU =
            MENUS.register("iron_crate_menu", () -> MenuRegistry.ofExtended(IronCrateMenu::new));

    public static final RegistrySupplier<MenuType<IronCrateMenu>> PORTABLE_IRON_CRATE_MENU =
            MENUS.register("portable_iron_crate_menu", () -> MenuRegistry.ofExtended(IronCrateMenu::new));

    public static final RegistrySupplier<MenuType<SteelCrateMenu>> STEEL_CRATE_MENU =
            MENUS.register("steel_crate_menu", () -> MenuRegistry.ofExtended(SteelCrateMenu::new));

    public static final RegistrySupplier<MenuType<DeshCrateMenu>> DESH_CRATE_MENU =
            MENUS.register("desh_crate_menu", () -> MenuRegistry.ofExtended(DeshCrateMenu::new));

    public static final RegistrySupplier<MenuType<TungstenCrateMenu>> TUNGSTEN_CRATE_MENU =
            MENUS.register("tungsten_crate_menu", () -> MenuRegistry.ofExtended(TungstenCrateMenu::new));

    public static final RegistrySupplier<MenuType<TemplateCrateMenu>> TEMPLATE_CRATE_MENU =
            MENUS.register("template_crate_menu", () -> MenuRegistry.ofExtended(TemplateCrateMenu::new));

    public static final RegistrySupplier<MenuType<MachineFluidTankMenu>> FLUID_TANK_MENU =
            MENUS.register("fluid_tank_menu", () -> MenuRegistry.ofExtended(MachineFluidTankMenu::new));

    public static final RegistrySupplier<MenuType<Bat9000Menu>> BAT9000_MENU =
            MENUS.register("bat9000_menu", () -> MenuRegistry.ofExtended(Bat9000Menu::new));

    public static final RegistrySupplier<MenuType<BarrelIronMenu>> BARREL_IRON_MENU =
            MENUS.register("barrel_iron_menu", () -> MenuRegistry.ofExtended(BarrelIronMenu::new));

    public static final RegistrySupplier<MenuType<BarrelSteelMenu>> BARREL_STEEL_MENU =
            MENUS.register("barrel_steel_menu", () -> MenuRegistry.ofExtended(BarrelSteelMenu::new));

    public static final RegistrySupplier<MenuType<MachineChemicalPlantMenu>> CHEMICAL_PLANT_MENU =
            MENUS.register("chemical_plant_menu", () -> MenuRegistry.ofExtended(MachineChemicalPlantMenu::new));

    public static final RegistrySupplier<MenuType<MachineChemicalFactoryMenu>> CHEMICAL_FACTORY_MENU =
            MENUS.register("chemical_factory_menu", () -> MenuRegistry.ofExtended(MachineChemicalFactoryMenu::new));

    public static final RegistrySupplier<MenuType<SoyuzLauncherMenu>> SOYUZ_LAUNCHER_MENU =
            MENUS.register("soyuz_launcher_menu", () -> MenuRegistry.ofExtended(SoyuzLauncherMenu::new));

    public static final RegistrySupplier<MenuType<MachineSatLinkerMenu>> MACHINE_SATLINKER_MENU =
            MENUS.register("machine_satlinker_menu", () -> MenuRegistry.ofExtended(MachineSatLinkerMenu::new));

    public static final RegistrySupplier<MenuType<MachineCyclotronMenu>> CYCLOTRON_MENU =
            MENUS.register("cyclotron_menu", () -> MenuRegistry.ofExtended(MachineCyclotronMenu::new));

    public static final RegistrySupplier<MenuType<MachineZirnoxMenu>> ZIRNOX_MENU =
            MENUS.register("zirnox_menu", () -> MenuRegistry.ofExtended(MachineZirnoxMenu::new));

    public static final RegistrySupplier<MenuType<MachineWatzPowerplantMenu>> WATZ_POWERPLANT_MENU =
            MENUS.register("watz_powerplant_menu", () -> MenuRegistry.ofExtended(MachineWatzPowerplantMenu::new));

    public static final RegistrySupplier<MenuType<PWRControllerMenu>> PWR_CONTROLLER_MENU =
            MENUS.register("pwr_controller_menu", () -> MenuRegistry.ofExtended(PWRControllerMenu::new));

    public static final RegistrySupplier<MenuType<MachineArcWelderMenu>> ARC_WELDER_MENU =
            MENUS.register("arc_welder_menu", () -> MenuRegistry.ofExtended(MachineArcWelderMenu::new));

    public static final RegistrySupplier<MenuType<MachineSolderingStationMenu>> SOLDERING_STATION_MENU =
            MENUS.register("soldering_station_menu", () -> MenuRegistry.ofExtended(MachineSolderingStationMenu::new));

    public static final RegistrySupplier<MenuType<MachineMixerMenu>> MIXER_MENU =
            MENUS.register("mixer_menu", () -> MenuRegistry.ofExtended(MachineMixerMenu::new));

    public static final RegistrySupplier<MenuType<MachineDerrickMenu>> DERRICK_MENU =
            MENUS.register("derrick_menu", () -> MenuRegistry.ofExtended(MachineDerrickMenu::new));

    public static final RegistrySupplier<MenuType<MachineOilWellMenu>> MACHINE_WELL_MENU =
            MENUS.register("machine_well_menu", () -> MenuRegistry.ofExtended(MachineOilWellMenu::new));

    public static final RegistrySupplier<MenuType<MachineCokerMenu>> COKER_MENU =
            MENUS.register("coker_menu", () -> MenuRegistry.ofExtended(MachineCokerMenu::new));

    public static final RegistrySupplier<MenuType<MachinePyroOvenMenu>> PYROOVEN_MENU =
            MENUS.register("pyrooven_menu", () -> MenuRegistry.ofExtended(MachinePyroOvenMenu::new));

    public static final RegistrySupplier<MenuType<MachineSolidifierMenu>> SOLIDIFIER_MENU =
            MENUS.register("solidifier_menu", () -> MenuRegistry.ofExtended(MachineSolidifierMenu::new));

    public static final RegistrySupplier<MenuType<MachineAshpitMenu>> ASHPIT_MENU =
            MENUS.register("ashpit_menu", () -> MenuRegistry.ofExtended(MachineAshpitMenu::new));

    public static final RegistrySupplier<MenuType<MachineReactorResearchMenu>> REACTOR_RESEARCH_MENU =
            MENUS.register("reactor_research_menu", () -> MenuRegistry.ofExtended(MachineReactorResearchMenu::new));

    public static final RegistrySupplier<MenuType<MachineRadGenMenu>> RADGEN_MENU =
            MENUS.register("radgen_menu", () -> MenuRegistry.ofExtended(MachineRadGenMenu::new));

    public static final RegistrySupplier<MenuType<MachineCraneInserterMenu>> CRANE_INSERTER_MENU =
            MENUS.register("crane_inserter_menu", () -> MenuRegistry.ofExtended(MachineCraneInserterMenu::new));

    public static final RegistrySupplier<MenuType<MachineCraneExtractorMenu>> CRANE_EXTRACTOR_MENU =
            MENUS.register("crane_extractor_menu", () -> MenuRegistry.ofExtended(MachineCraneExtractorMenu::new));

    public static final RegistrySupplier<MenuType<MachineCraneGrabberMenu>> CRANE_GRABBER_MENU =
            MENUS.register("crane_grabber_menu", () -> MenuRegistry.ofExtended(MachineCraneGrabberMenu::new));

    public static final RegistrySupplier<MenuType<MachineCraneRouterMenu>> CRANE_ROUTER_MENU =
            MENUS.register("crane_router_menu", () -> MenuRegistry.ofExtended(MachineCraneRouterMenu::new));

    public static final RegistrySupplier<MenuType<MachineCraneBoxerMenu>> CRANE_BOXER_MENU =
            MENUS.register("crane_boxer_menu", () -> MenuRegistry.ofExtended(MachineCraneBoxerMenu::new));

    public static final RegistrySupplier<MenuType<MachineCraneUnboxerMenu>> CRANE_UNBOXER_MENU =
            MENUS.register("crane_unboxer_menu", () -> MenuRegistry.ofExtended(MachineCraneUnboxerMenu::new));

    public static final RegistrySupplier<MenuType<MachineDroneCrateMenu>> DRONE_CRATE_MENU =
            MENUS.register("drone_crate_menu", () -> MenuRegistry.ofExtended(MachineDroneCrateMenu::new));

    public static final RegistrySupplier<MenuType<MachineDroneProviderMenu>> DRONE_PROVIDER_MENU =
            MENUS.register("drone_provider_menu", () -> MenuRegistry.ofExtended(MachineDroneProviderMenu::new));

    public static final RegistrySupplier<MenuType<MachineDroneRequesterMenu>> DRONE_REQUESTER_MENU =
            MENUS.register("drone_requester_menu", () -> MenuRegistry.ofExtended(MachineDroneRequesterMenu::new));

    public static final RegistrySupplier<MenuType<MachineDroneDockMenu>> DRONE_DOCK_MENU =
            MENUS.register("drone_dock_menu", () -> MenuRegistry.ofExtended(MachineDroneDockMenu::new));

    public static final RegistrySupplier<MenuType<com.hbm_m.inventory.menu.radio.RadioTorchCounterMenu>> RADIO_TORCH_COUNTER_MENU =
            MENUS.register("radio_torch_counter_menu", () -> MenuRegistry.ofExtended(com.hbm_m.inventory.menu.radio.RadioTorchCounterMenu::new));

    public static final RegistrySupplier<MenuType<MachineStorageDrumMenu>> MACHINE_STORAGE_DRUM_MENU =
            MENUS.register("machine_storage_drum_menu", () -> MenuRegistry.ofExtended(MachineStorageDrumMenu::new));

    public static final RegistrySupplier<MenuType<MachineSirenMenu>> MACHINE_SIREN_MENU =
            MENUS.register("machine_siren_menu", () -> MenuRegistry.ofExtended(MachineSirenMenu::new));

    public static final RegistrySupplier<MenuType<MachineFireboxMenu>> MACHINE_FIREBOX_MENU =
            MENUS.register("machine_firebox_menu", () -> MenuRegistry.ofExtended(MachineFireboxMenu::new));

    public static final RegistrySupplier<MenuType<MachineKeyforgeMenu>> MACHINE_KEYFORGE_MENU =
            MENUS.register("machine_keyforge_menu", () -> MenuRegistry.ofExtended(MachineKeyforgeMenu::new));

    public static final RegistrySupplier<MenuType<MachineMassStorageMenu>> MACHINE_MASS_STORAGE_MENU =
            MENUS.register("mass_storage_menu", () -> MenuRegistry.ofExtended(MachineMassStorageMenu::new));

    public static final RegistrySupplier<MenuType<OrbusMenu>> ORBUS_MENU =
            MENUS.register("orbus_menu", () -> MenuRegistry.ofExtended(OrbusMenu::new));

    public static final RegistrySupplier<MenuType<MachineRtgMenu>> MACHINE_RTG_MENU =
            MENUS.register("machine_rtg_menu", () -> MenuRegistry.ofExtended(MachineRtgMenu::new));

    public static final RegistrySupplier<MenuType<MachineWasteDrumMenu>> MACHINE_WASTE_DRUM_MENU =
            MENUS.register("machine_waste_drum_menu", () -> MenuRegistry.ofExtended(MachineWasteDrumMenu::new));

    public static final RegistrySupplier<MenuType<MachineRbmkConsoleMenu>> RBMK_CONSOLE_MENU =
            MENUS.register("rbmk_console_menu", () -> MenuRegistry.ofExtended(MachineRbmkConsoleMenu::new));

    public static final RegistrySupplier<MenuType<MachineFlareStackMenu>> FLARE_STACK_MENU =
            MENUS.register("flare_stack_menu", () -> MenuRegistry.ofExtended(MachineFlareStackMenu::new));

    public static final RegistrySupplier<MenuType<MachineOilburnerMenu>> OILBURNER_MENU =
            MENUS.register("oilburner_menu", () -> MenuRegistry.ofExtended(MachineOilburnerMenu::new));

    public static final RegistrySupplier<MenuType<MachineTurbofanMenu>> TURBOFAN_MENU =
            MENUS.register("turbofan_menu", () -> MenuRegistry.ofExtended(MachineTurbofanMenu::new));

    public static final RegistrySupplier<MenuType<MachineHeatexMenu>> HEATEX_MENU =
            MENUS.register("heatex_menu", () -> MenuRegistry.ofExtended(MachineHeatexMenu::new));

    public static final RegistrySupplier<MenuType<MachinePumpjackMenu>> PUMPJACK_MENU =
            MENUS.register("pumpjack_menu", () -> MenuRegistry.ofExtended(MachinePumpjackMenu::new));

    public static final RegistrySupplier<MenuType<MachineRadarMenu>> RADAR_MENU =
            MENUS.register("radar_menu", () -> MenuRegistry.ofExtended(MachineRadarMenu::new));

    public static final RegistrySupplier<MenuType<MachineRadarSlotsMenu>> RADAR_SLOTS_MENU =
            MENUS.register("radar_slots_menu", () -> MenuRegistry.ofExtended(MachineRadarSlotsMenu::new));

    public static final RegistrySupplier<MenuType<MachineCrackingTowerMenu>> CRACKING_TOWER_MENU =
            MENUS.register("cracking_tower_menu", () -> MenuRegistry.ofExtended(MachineCrackingTowerMenu::new));

    public static final RegistrySupplier<MenuType<MachineFractionTowerMenu>> FRACTION_TOWER_MENU =
            MENUS.register("fraction_tower_menu", () -> MenuRegistry.ofExtended(MachineFractionTowerMenu::new));

    public static final RegistrySupplier<MenuType<MachineMiningDrillMenu>> MINING_DRILL_MENU =
            MENUS.register("mining_drill_menu", () -> MenuRegistry.ofExtended(MachineMiningDrillMenu::new));

    public static final RegistrySupplier<MenuType<MachineFelMenu>> FEL_MENU =
            MENUS.register("fel_menu", () -> MenuRegistry.ofExtended(MachineFelMenu::new));

    public static final RegistrySupplier<MenuType<MachineSilexMenu>> SILEX_MENU =
            MENUS.register("silex_menu", () -> MenuRegistry.ofExtended(MachineSilexMenu::new));

    public static final RegistrySupplier<MenuType<MachineTurbineMenu>> TURBINE_MENU =
            MENUS.register("turbine_menu", () -> MenuRegistry.ofExtended(MachineTurbineMenu::new));

    public static final RegistrySupplier<MenuType<MachineSteamTurbineMenu>> STEAM_TURBINE_MENU =
            MENUS.register("steam_turbine_menu", () -> MenuRegistry.ofExtended(MachineSteamTurbineMenu::new));

    public static final RegistrySupplier<MenuType<MachineSubstationMenu>> SUBSTATION_MENU =
            MENUS.register("substation_menu", () -> MenuRegistry.ofExtended(MachineSubstationMenu::new));

    public static final RegistrySupplier<MenuType<MachineFrackingTowerMenu>> FRACTURING_TOWER_MENU =
            MENUS.register("fracking_tower_menu", () -> MenuRegistry.ofExtended(MachineFrackingTowerMenu::new));

    public static final RegistrySupplier<MenuType<MachineRefineryMenu>> REFINERY_MENU =
            MENUS.register("refinery_menu", () -> MenuRegistry.ofExtended(MachineRefineryMenu::new));

    public static final RegistrySupplier<MenuType<LaunchPadLargeMenu>> LAUNCH_PAD_LARGE_MENU =
            MENUS.register("launch_pad_large_menu", () -> MenuRegistry.ofExtended(LaunchPadLargeMenu::new));

    public static final RegistrySupplier<MenuType<LaunchPadRustedMenu>> LAUNCH_PAD_RUSTED_MENU =
            MENUS.register("launch_pad_rusted_menu", () -> MenuRegistry.ofExtended(LaunchPadRustedMenu::new));

    public static final RegistrySupplier<MenuType<NukeFatManMenu>> NUKE_FAT_MAN_MENU =
            MENUS.register("nuke_fat_man_menu", () -> MenuRegistry.ofExtended(NukeFatManMenu::new));

    public static final RegistrySupplier<MenuType<NukePrototypeMenu>> NUKE_PROTOTYPE_MENU =
            MENUS.register("nuke_prototype_menu", () -> MenuRegistry.ofExtended(NukePrototypeMenu::new));

    public static final RegistrySupplier<MenuType<HeatingOvenMenu>> HEATING_OVEN_MENU =
            MENUS.register("heating_oven_menu", () -> MenuRegistry.ofExtended(HeatingOvenMenu::new));

    // ─── RBMK Column Menus ───────────────────────────────────────────────────

    public static final RegistrySupplier<MenuType<RBMKRodMenu>> RBMK_ROD_MENU =
            MENUS.register("rbmk_rod_menu", () -> MenuRegistry.ofExtended(RBMKRodMenu::new));

    public static final RegistrySupplier<MenuType<RBMKControlMenu>> RBMK_CONTROL_MENU =
            MENUS.register("rbmk_control_menu", () -> MenuRegistry.ofExtended(RBMKControlMenu::new));

    public static final RegistrySupplier<MenuType<RBMKBoilerMenu>> RBMK_BOILER_MENU =
            MENUS.register("rbmk_boiler_menu", () -> MenuRegistry.ofExtended(RBMKBoilerMenu::new));

    public static final RegistrySupplier<MenuType<RBMKStorageMenu>> RBMK_STORAGE_MENU =
            MENUS.register("rbmk_storage_menu", () -> MenuRegistry.ofExtended(RBMKStorageMenu::new));

    public static final RegistrySupplier<MenuType<RBMKOutgasserMenu>> RBMK_OUTGASSER_MENU =
            MENUS.register("rbmk_outgasser_menu", () -> MenuRegistry.ofExtended(RBMKOutgasserMenu::new));

    /** Ein gemeinsamer MenuType fuer alle Turret-Varianten - siehe {@link TurretMenu}. */
    public static final RegistrySupplier<MenuType<TurretMenu>> TURRET_MENU =
            MENUS.register("turret_menu", () -> MenuRegistry.ofExtended(TurretMenu::new));

    public static final RegistrySupplier<MenuType<MissileAssemblyMenu>> MISSILE_ASSEMBLY_MENU =
            MENUS.register("missile_assembly_menu", () -> MenuRegistry.ofExtended(MissileAssemblyMenu::new));

    public static final RegistrySupplier<MenuType<MachineVacuumDistillMenu>> VACUUM_DISTILL_MENU =
            MENUS.register("vacuum_distill_menu", () -> MenuRegistry.ofExtended(MachineVacuumDistillMenu::new));

    public static final RegistrySupplier<MenuType<MachineHydrotreaterMenu>> HYDROTREATER_MENU =
            MENUS.register("hydrotreater_menu", () -> MenuRegistry.ofExtended(MachineHydrotreaterMenu::new));

    public static final RegistrySupplier<MenuType<MachineCatalyticReformerMenu>> CATALYTIC_REFORMER_MENU =
            MENUS.register("catalytic_reformer_menu", () -> MenuRegistry.ofExtended(MachineCatalyticReformerMenu::new));

    public static final RegistrySupplier<MenuType<MachineLiquefactorMenu>> LIQUEFACTOR_MENU =
            MENUS.register("liquefactor_menu", () -> MenuRegistry.ofExtended(MachineLiquefactorMenu::new));

    public static final RegistrySupplier<MenuType<MachineElectricFurnaceMenu>> ELECTRIC_FURNACE_MENU =
            MENUS.register("electric_furnace_menu", () -> MenuRegistry.ofExtended(MachineElectricFurnaceMenu::new));

    public static final RegistrySupplier<MenuType<MachineFurnaceBrickMenu>> FURNACE_BRICK_MENU =
            MENUS.register("furnace_brick_menu", () -> MenuRegistry.ofExtended(MachineFurnaceBrickMenu::new));

    public static final RegistrySupplier<MenuType<MachineFurnaceIronMenu>> FURNACE_IRON_MENU =
            MENUS.register("furnace_iron_menu", () -> MenuRegistry.ofExtended(MachineFurnaceIronMenu::new));

    public static final RegistrySupplier<MenuType<MachineFurnaceSteelMenu>> FURNACE_STEEL_MENU =
            MENUS.register("furnace_steel_menu", () -> MenuRegistry.ofExtended(MachineFurnaceSteelMenu::new));

    public static final RegistrySupplier<MenuType<com.hbm_m.inventory.menu.MachineRotaryFurnaceMenu>> ROTARY_FURNACE_MENU =
            MENUS.register("rotary_furnace_menu", () -> MenuRegistry.ofExtended(com.hbm_m.inventory.menu.MachineRotaryFurnaceMenu::new));

    public static final RegistrySupplier<MenuType<com.hbm_m.inventory.menu.MachineStrandCasterMenu>> STRAND_CASTER_MENU =
            MENUS.register("strand_caster_menu", () -> MenuRegistry.ofExtended(com.hbm_m.inventory.menu.MachineStrandCasterMenu::new));

    public static final RegistrySupplier<MenuType<com.hbm_m.inventory.menu.MachineMicrowaveMenu>> MICROWAVE_MENU =
            MENUS.register("microwave_menu", () -> MenuRegistry.ofExtended(com.hbm_m.inventory.menu.MachineMicrowaveMenu::new));

    public static final RegistrySupplier<MenuType<com.hbm_m.inventory.menu.MachineExposureChamberMenu>> EXPOSURE_CHAMBER_MENU =
            MENUS.register("exposure_chamber_menu", () -> MenuRegistry.ofExtended(com.hbm_m.inventory.menu.MachineExposureChamberMenu::new));

    public static final RegistrySupplier<MenuType<com.hbm_m.inventory.menu.MachineRadiolysisMenu>> RADIOLYSIS_MENU =
            MENUS.register("radiolysis_menu", () -> MenuRegistry.ofExtended(com.hbm_m.inventory.menu.MachineRadiolysisMenu::new));

    public static final RegistrySupplier<MenuType<com.hbm_m.inventory.menu.MachineElectrolyserMenu>> ELECTROLYSER_MENU =
            MENUS.register("electrolyser_menu", () -> MenuRegistry.ofExtended(com.hbm_m.inventory.menu.MachineElectrolyserMenu::new));

    public static final RegistrySupplier<MenuType<com.hbm_m.inventory.menu.MachineCompressorMenu>> COMPRESSOR_MENU =
            MENUS.register("compressor_menu", () -> MenuRegistry.ofExtended(com.hbm_m.inventory.menu.MachineCompressorMenu::new));

    public static void init() {
        MENUS.register();
    }
}
