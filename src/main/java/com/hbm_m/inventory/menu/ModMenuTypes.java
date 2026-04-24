package com.hbm_m.inventory.menu;

import com.hbm_m.armormod.menu.ArmorTableMenu;
import com.hbm_m.lib.RefStrings;

import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import dev.architectury.registry.registries.DeferredRegister;
import net.minecraft.core.registries.Registries;
import dev.architectury.registry.registries.RegistrySupplier;

public class ModMenuTypes {

    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(RefStrings.MODID, Registries.MENU);
			
    public static final RegistrySupplier<MenuType<MachineCrystallizerMenu>> CRYSTALLIZER_MENU =
            MENUS.register("crystallizer_menu", () -> IForgeMenuType.create(MachineCrystallizerMenu::new));

    public static final RegistrySupplier<MenuType<ArmorTableMenu>> ARMOR_TABLE_MENU =
            MENUS.register("armor_table_menu", () -> IForgeMenuType.create(ArmorTableMenu::new));

    public static final RegistrySupplier<MenuType<MachineAssemblerMenu>> MACHINE_ASSEMBLER_MENU =
            MENUS.register("machine_assembler_menu", () -> IForgeMenuType.create(MachineAssemblerMenu::new));

    public static final RegistrySupplier<MenuType<MachineAdvancedAssemblerMenu>> ADVANCED_ASSEMBLY_MACHINE_MENU =
            MENUS.register("advanced_assembly_machine_menu", () -> IForgeMenuType.create(MachineAdvancedAssemblerMenu::new));

    public static final RegistrySupplier<MenuType<MachineBatteryMenu>> MACHINE_BATTERY_MENU =
            MENUS.register("machine_battery_menu", () -> IForgeMenuType.create(MachineBatteryMenu::new));

    public static final RegistrySupplier<MenuType<BatterySocketMenu>> BATTERY_SOCKET_MENU =
            MENUS.register("battery_socket_menu", () -> IForgeMenuType.create(BatterySocketMenu::new));

    public static final RegistrySupplier<MenuType<BlastFurnaceMenu>> BLAST_FURNACE_MENU =
            MENUS.register("blast_furnace_menu", () -> IForgeMenuType.create(BlastFurnaceMenu::new));

    public static final RegistrySupplier<MenuType<MachinePressMenu>> PRESS_MENU =
            MENUS.register("press_menu", () -> IForgeMenuType.create(MachinePressMenu::new));

    public static final RegistrySupplier<MenuType<AnvilMenu>> ANVIL_MENU =
            MENUS.register("anvil_menu", () -> IForgeMenuType.create(AnvilMenu::new));

    public static final RegistrySupplier<MenuType<MachineWoodBurnerMenu>> WOOD_BURNER_MENU =
            MENUS.register("wood_burner_menu", () -> IForgeMenuType.create(MachineWoodBurnerMenu::new));

    public static final RegistrySupplier<MenuType<MachineShredderMenu>> SHREDDER_MENU =
            MENUS.register("shredder_menu", () -> IForgeMenuType.create(MachineShredderMenu::new));

    public static final RegistrySupplier<MenuType<MachineCentrifugeMenu>> CENTRIFUGE_MENU =
            MENUS.register("centrifuge_menu", () -> IForgeMenuType.create(MachineCentrifugeMenu::new));

    public static final RegistrySupplier<MenuType<IronCrateMenu>> IRON_CRATE_MENU =
            MENUS.register("iron_crate_menu", () -> IForgeMenuType.create(IronCrateMenu::new));

    public static final RegistrySupplier<MenuType<IronCrateMenu>> PORTABLE_IRON_CRATE_MENU =
            MENUS.register("portable_iron_crate_menu", () -> IForgeMenuType.create(IronCrateMenu::new));

    public static final RegistrySupplier<MenuType<SteelCrateMenu>> STEEL_CRATE_MENU =
            MENUS.register("steel_crate_menu", () -> IForgeMenuType.create(SteelCrateMenu::new));

    public static final RegistrySupplier<MenuType<DeshCrateMenu>> DESH_CRATE_MENU =
            MENUS.register("desh_crate_menu", () -> IForgeMenuType.create(DeshCrateMenu::new));

    public static final RegistrySupplier<MenuType<TungstenCrateMenu>> TUNGSTEN_CRATE_MENU =
            MENUS.register("tungsten_crate_menu", () -> IForgeMenuType.create(TungstenCrateMenu::new));

    public static final RegistrySupplier<MenuType<TemplateCrateMenu>> TEMPLATE_CRATE_MENU =
            MENUS.register("template_crate_menu", () -> IForgeMenuType.create(TemplateCrateMenu::new));

    public static final RegistrySupplier<MenuType<MachineFluidTankMenu>> FLUID_TANK_MENU =
            MENUS.register("fluid_tank_menu", () -> IForgeMenuType.create(MachineFluidTankMenu::new));

    public static final RegistrySupplier<MenuType<MachineChemicalPlantMenu>> CHEMICAL_PLANT_MENU =
            MENUS.register("chemical_plant_menu", () -> IForgeMenuType.create(MachineChemicalPlantMenu::new));

	public static final RegistrySupplier<MenuType<MachineFrackingTowerMenu>> FRACTURING_TOWER_MENU =
            MENUS.register("fracking_tower_menu", () -> IForgeMenuType.create(MachineFrackingTowerMenu::new));

    public static final RegistrySupplier<MenuType<LaunchPadLargeMenu>> LAUNCH_PAD_LARGE_MENU =
            MENUS.register("launch_pad_large_menu", () -> IForgeMenuType.create(LaunchPadLargeMenu::new));

    public static final RegistrySupplier<MenuType<LaunchPadRustedMenu>> LAUNCH_PAD_RUSTED_MENU =
            MENUS.register("launch_pad_rusted_menu", () -> IForgeMenuType.create(LaunchPadRustedMenu::new));

    public static final RegistrySupplier<MenuType<NukeFatManMenu>> NUKE_FAT_MAN_MENU =
            MENUS.register("nuke_fat_man_menu", () -> IForgeMenuType.create(NukeFatManMenu::new));

    public static final RegistrySupplier<MenuType<HeatingOvenMenu>> HEATING_OVEN_MENU =
            MENUS.register("heating_oven_menu", () -> IForgeMenuType.create(HeatingOvenMenu::new));

    public static void init() {
        MENUS.register();
    }
}