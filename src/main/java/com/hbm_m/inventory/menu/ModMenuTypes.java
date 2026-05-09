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