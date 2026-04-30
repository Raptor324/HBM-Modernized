package com.hbm_m.inventory.menu;

import com.hbm_m.armormod.menu.ArmorTableMenu;
import com.hbm_m.lib.RefStrings;

import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModMenuTypes {

    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(ForgeRegistries.MENU_TYPES, RefStrings.MODID);
			
    public static final RegistryObject<MenuType<MachineCrystallizerMenu>> CRYSTALLIZER_MENU =
            MENUS.register("crystallizer_menu", () -> IForgeMenuType.create(MachineCrystallizerMenu::new));

    public static final RegistryObject<MenuType<MachineBreederMenu>> BREEDER_MENU =
            MENUS.register("breeder_menu", () -> IForgeMenuType.create(MachineBreederMenu::new));

    public static final RegistryObject<MenuType<MachineLargePylonMenu>> LARGE_PYLON_MENU =
            MENUS.register("large_pylon_menu", () -> IForgeMenuType.create(MachineLargePylonMenu::new));

    public static final RegistryObject<MenuType<ArmorTableMenu>> ARMOR_TABLE_MENU =
            MENUS.register("armor_table_menu", () -> IForgeMenuType.create(ArmorTableMenu::new));

    public static final RegistryObject<MenuType<MachineAssemblerMenu>> MACHINE_ASSEMBLER_MENU =
            MENUS.register("machine_assembler_menu", () -> IForgeMenuType.create(MachineAssemblerMenu::new));

    public static final RegistryObject<MenuType<MachineAdvancedAssemblerMenu>> ADVANCED_ASSEMBLY_MACHINE_MENU =
            MENUS.register("advanced_assembly_machine_menu", () -> IForgeMenuType.create(MachineAdvancedAssemblerMenu::new));

    public static final RegistryObject<MenuType<MachineBatteryMenu>> MACHINE_BATTERY_MENU =
            MENUS.register("machine_battery_menu", () -> IForgeMenuType.create(MachineBatteryMenu::new));

    public static final RegistryObject<MenuType<BatterySocketMenu>> BATTERY_SOCKET_MENU =
            MENUS.register("battery_socket_menu", () -> IForgeMenuType.create(BatterySocketMenu::new));

    public static final RegistryObject<MenuType<BlastFurnaceMenu>> BLAST_FURNACE_MENU =
            MENUS.register("blast_furnace_menu", () -> IForgeMenuType.create(BlastFurnaceMenu::new));

    public static final RegistryObject<MenuType<MachinePressMenu>> PRESS_MENU =
            MENUS.register("press_menu", () -> IForgeMenuType.create(MachinePressMenu::new));

    public static final RegistryObject<MenuType<AnvilMenu>> ANVIL_MENU =
            MENUS.register("anvil_menu", () -> IForgeMenuType.create(AnvilMenu::new));

    public static final RegistryObject<MenuType<MachineWoodBurnerMenu>> WOOD_BURNER_MENU =
            MENUS.register("wood_burner_menu", () -> IForgeMenuType.create(MachineWoodBurnerMenu::new));

    public static final RegistryObject<MenuType<MachineShredderMenu>> SHREDDER_MENU =
            MENUS.register("shredder_menu", () -> IForgeMenuType.create(MachineShredderMenu::new));

    public static final RegistryObject<MenuType<MachineCentrifugeMenu>> CENTRIFUGE_MENU =
            MENUS.register("centrifuge_menu", () -> IForgeMenuType.create(MachineCentrifugeMenu::new));

    public static final RegistryObject<MenuType<MachineCrucibleMenu>> CRUCIBLE_MENU =
            MENUS.register("crucible_menu", () -> IForgeMenuType.create(MachineCrucibleMenu::new));

    public static final RegistryObject<MenuType<MachineGasCentrifugeMenu>> GAS_CENTRIFUGE_MENU =
            MENUS.register("gas_centrifuge_menu", () -> IForgeMenuType.create(MachineGasCentrifugeMenu::new));

    public static final RegistryObject<MenuType<LaunchPadLargeMenu>> LAUNCH_PAD_LARGE_MENU =
            MENUS.register("launch_pad_large_menu", () -> IForgeMenuType.create(LaunchPadLargeMenu::new));

    public static final RegistryObject<MenuType<LaunchPadRustedMenu>> LAUNCH_PAD_RUSTED_MENU =
            MENUS.register("launch_pad_rusted_menu", () -> IForgeMenuType.create(LaunchPadRustedMenu::new));

    public static final RegistryObject<MenuType<NukeFatManMenu>> NUKE_FAT_MAN_MENU =
            MENUS.register("nuke_fat_man_menu", () -> IForgeMenuType.create(NukeFatManMenu::new));

    public static final RegistryObject<MenuType<IronCrateMenu>> IRON_CRATE_MENU =
            MENUS.register("iron_crate_menu", () -> IForgeMenuType.create(IronCrateMenu::new));

    public static final RegistryObject<MenuType<IronCrateMenu>> PORTABLE_IRON_CRATE_MENU =
            MENUS.register("portable_iron_crate_menu", () -> IForgeMenuType.create(IronCrateMenu::new));

    public static final RegistryObject<MenuType<SteelCrateMenu>> STEEL_CRATE_MENU =
            MENUS.register("steel_crate_menu", () -> IForgeMenuType.create(SteelCrateMenu::new));

    public static final RegistryObject<MenuType<DeshCrateMenu>> DESH_CRATE_MENU =
            MENUS.register("desh_crate_menu", () -> IForgeMenuType.create(DeshCrateMenu::new));

    public static final RegistryObject<MenuType<TungstenCrateMenu>> TUNGSTEN_CRATE_MENU =
            MENUS.register("tungsten_crate_menu", () -> IForgeMenuType.create(TungstenCrateMenu::new));

    public static final RegistryObject<MenuType<TemplateCrateMenu>> TEMPLATE_CRATE_MENU =
            MENUS.register("template_crate_menu", () -> IForgeMenuType.create(TemplateCrateMenu::new));

    public static final RegistryObject<MenuType<MachineFluidTankMenu>> FLUID_TANK_MENU =
            MENUS.register("fluid_tank_menu", () -> IForgeMenuType.create(MachineFluidTankMenu::new));

    public static final RegistryObject<MenuType<MachineChemicalPlantMenu>> CHEMICAL_PLANT_MENU =
            MENUS.register("chemical_plant_menu", () -> IForgeMenuType.create(MachineChemicalPlantMenu::new));

    public static final RegistryObject<MenuType<MachineCyclotronMenu>> CYCLOTRON_MENU =
            MENUS.register("cyclotron_menu", () -> IForgeMenuType.create(MachineCyclotronMenu::new));

    public static final RegistryObject<MenuType<MachineZirnoxMenu>> ZIRNOX_MENU =
            MENUS.register("zirnox_menu", () -> IForgeMenuType.create(MachineZirnoxMenu::new));

    public static final RegistryObject<MenuType<MachineArcWelderMenu>> ARC_WELDER_MENU =
            MENUS.register("arc_welder_menu", () -> IForgeMenuType.create(MachineArcWelderMenu::new));

    public static final RegistryObject<MenuType<MachineSolderingStationMenu>> SOLDERING_STATION_MENU =
            MENUS.register("soldering_station_menu", () -> IForgeMenuType.create(MachineSolderingStationMenu::new));

    public static final RegistryObject<MenuType<MachineMixerMenu>> MIXER_MENU =
            MENUS.register("mixer_menu", () -> IForgeMenuType.create(MachineMixerMenu::new));

    public static final RegistryObject<MenuType<MachineDerrickMenu>> DERRICK_MENU =
            MENUS.register("derrick_menu", () -> IForgeMenuType.create(MachineDerrickMenu::new));

    public static final RegistryObject<MenuType<MachineRbmkConsoleMenu>> RBMK_CONSOLE_MENU =
            MENUS.register("rbmk_console_menu", () -> IForgeMenuType.create(MachineRbmkConsoleMenu::new));

    public static final RegistryObject<MenuType<MachineFlareStackMenu>> FLARE_STACK_MENU =
            MENUS.register("flare_stack_menu", () -> IForgeMenuType.create(MachineFlareStackMenu::new));

    public static final RegistryObject<MenuType<MachinePumpjackMenu>> PUMPJACK_MENU =
            MENUS.register("pumpjack_menu", () -> IForgeMenuType.create(MachinePumpjackMenu::new));

    public static final RegistryObject<MenuType<MachineRadarMenu>> RADAR_MENU =
            MENUS.register("radar_menu", () -> IForgeMenuType.create(MachineRadarMenu::new));

    public static final RegistryObject<MenuType<MachineCrackingTowerMenu>> CRACKING_TOWER_MENU =
            MENUS.register("cracking_tower_menu", () -> IForgeMenuType.create(MachineCrackingTowerMenu::new));

    public static final RegistryObject<MenuType<MachineFractionTowerMenu>> FRACTION_TOWER_MENU =
            MENUS.register("fraction_tower_menu", () -> IForgeMenuType.create(MachineFractionTowerMenu::new));

    public static final RegistryObject<MenuType<MachineMiningDrillMenu>> MINING_DRILL_MENU =
            MENUS.register("mining_drill_menu", () -> IForgeMenuType.create(MachineMiningDrillMenu::new));

    public static final RegistryObject<MenuType<MachineFelMenu>> FEL_MENU =
            MENUS.register("fel_menu", () -> IForgeMenuType.create(MachineFelMenu::new));

    public static final RegistryObject<MenuType<MachineSilexMenu>> SILEX_MENU =
            MENUS.register("silex_menu", () -> IForgeMenuType.create(MachineSilexMenu::new));

    public static final RegistryObject<MenuType<MachineIndustrialTurbineMenu>> INDUSTRIAL_TURBINE_MENU =
            MENUS.register("industrial_turbine_menu", () -> IForgeMenuType.create(MachineIndustrialTurbineMenu::new));

    public static final RegistryObject<MenuType<MachineTurbineMenu>> TURBINE_MENU =
            MENUS.register("turbine_menu", () -> IForgeMenuType.create(MachineTurbineMenu::new));

    public static final RegistryObject<MenuType<MachineSubstationMenu>> SUBSTATION_MENU =
            MENUS.register("substation_menu", () -> IForgeMenuType.create(MachineSubstationMenu::new));

	public static final RegistryObject<MenuType<MachineFrackingTowerMenu>> FRACTURING_TOWER_MENU =
            MENUS.register("fracking_tower_menu", () -> IForgeMenuType.create(MachineFrackingTowerMenu::new));

    public static final RegistryObject<MenuType<HeatingOvenMenu>> HEATING_OVEN_MENU =
            MENUS.register("heating_oven_menu", () -> IForgeMenuType.create(HeatingOvenMenu::new));

    public static void register(IEventBus eventBus) {
        MENUS.register(eventBus);
    }
}