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

    public static final RegistrySupplier<MenuType<MachineChemicalPlantMenu>> CHEMICAL_PLANT_MENU =
            MENUS.register("chemical_plant_menu", () -> MenuRegistry.ofExtended(MachineChemicalPlantMenu::new));

    public static final RegistrySupplier<MenuType<MachineCyclotronMenu>> CYCLOTRON_MENU =
            MENUS.register("cyclotron_menu", () -> MenuRegistry.ofExtended(MachineCyclotronMenu::new));

    public static final RegistrySupplier<MenuType<MachineZirnoxMenu>> ZIRNOX_MENU =
            MENUS.register("zirnox_menu", () -> MenuRegistry.ofExtended(MachineZirnoxMenu::new));

    public static final RegistrySupplier<MenuType<MachineArcWelderMenu>> ARC_WELDER_MENU =
            MENUS.register("arc_welder_menu", () -> MenuRegistry.ofExtended(MachineArcWelderMenu::new));

    public static final RegistrySupplier<MenuType<MachineSolderingStationMenu>> SOLDERING_STATION_MENU =
            MENUS.register("soldering_station_menu", () -> MenuRegistry.ofExtended(MachineSolderingStationMenu::new));

    public static final RegistrySupplier<MenuType<MachineMixerMenu>> MIXER_MENU =
            MENUS.register("mixer_menu", () -> MenuRegistry.ofExtended(MachineMixerMenu::new));

    public static final RegistrySupplier<MenuType<MachineDerrickMenu>> DERRICK_MENU =
            MENUS.register("derrick_menu", () -> MenuRegistry.ofExtended(MachineDerrickMenu::new));

    public static final RegistrySupplier<MenuType<MachineRbmkConsoleMenu>> RBMK_CONSOLE_MENU =
            MENUS.register("rbmk_console_menu", () -> MenuRegistry.ofExtended(MachineRbmkConsoleMenu::new));

    public static final RegistrySupplier<MenuType<MachineFlareStackMenu>> FLARE_STACK_MENU =
            MENUS.register("flare_stack_menu", () -> MenuRegistry.ofExtended(MachineFlareStackMenu::new));

    public static final RegistrySupplier<MenuType<MachinePumpjackMenu>> PUMPJACK_MENU =
            MENUS.register("pumpjack_menu", () -> MenuRegistry.ofExtended(MachinePumpjackMenu::new));

    public static final RegistrySupplier<MenuType<MachineRadarMenu>> RADAR_MENU =
            MENUS.register("radar_menu", () -> MenuRegistry.ofExtended(MachineRadarMenu::new));

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

    public static final RegistrySupplier<MenuType<MachineIndustrialTurbineMenu>> INDUSTRIAL_TURBINE_MENU =
            MENUS.register("industrial_turbine_menu", () -> MenuRegistry.ofExtended(MachineIndustrialTurbineMenu::new));

    public static final RegistrySupplier<MenuType<MachineTurbineMenu>> TURBINE_MENU =
            MENUS.register("turbine_menu", () -> MenuRegistry.ofExtended(MachineTurbineMenu::new));

    public static final RegistrySupplier<MenuType<MachineSubstationMenu>> SUBSTATION_MENU =
            MENUS.register("substation_menu", () -> MenuRegistry.ofExtended(MachineSubstationMenu::new));

    public static final RegistrySupplier<MenuType<MachineFrackingTowerMenu>> FRACTURING_TOWER_MENU =
            MENUS.register("fracking_tower_menu", () -> MenuRegistry.ofExtended(MachineFrackingTowerMenu::new));

    public static final RegistrySupplier<MenuType<LaunchPadLargeMenu>> LAUNCH_PAD_LARGE_MENU =
            MENUS.register("launch_pad_large_menu", () -> MenuRegistry.ofExtended(LaunchPadLargeMenu::new));

    public static final RegistrySupplier<MenuType<LaunchPadRustedMenu>> LAUNCH_PAD_RUSTED_MENU =
            MENUS.register("launch_pad_rusted_menu", () -> MenuRegistry.ofExtended(LaunchPadRustedMenu::new));

    public static final RegistrySupplier<MenuType<NukeFatManMenu>> NUKE_FAT_MAN_MENU =
            MENUS.register("nuke_fat_man_menu", () -> MenuRegistry.ofExtended(NukeFatManMenu::new));

    public static final RegistrySupplier<MenuType<HeatingOvenMenu>> HEATING_OVEN_MENU =
            MENUS.register("heating_oven_menu", () -> MenuRegistry.ofExtended(HeatingOvenMenu::new));

    public static void init() {
        MENUS.register();
    }
}
