package com.hbm_m.menu;

import com.hbm_m.armormod.menu.ArmorTableMenu;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.main.MainRegistry;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModMenuTypes {
    public static final DeferredRegister<MenuType<?>> MENUS =
		DeferredRegister.create(ForgeRegistries.MENU_TYPES, RefStrings.MODID);

    public static final RegistryObject<MenuType<ArmorTableMenu>> ARMOR_TABLE_MENU =
		MENUS.register("armor_table_menu", () -> IForgeMenuType.create(ArmorTableMenu::new));

    public static final RegistryObject<MenuType<MachineAssemblerMenu>> MACHINE_ASSEMBLER_MENU =
		MENUS.register("machine_assembler_menu", () -> IForgeMenuType.create(MachineAssemblerMenu::new));

	public static final RegistryObject<MenuType<MachineAdvancedAssemblerMenu>> ADVANCED_ASSEMBLY_MACHINE_MENU =
        MENUS.register("advanced_assembly_machine_menu", () -> IForgeMenuType.create(MachineAdvancedAssemblerMenu::new));

    public static final RegistryObject<MenuType<MachineBatteryMenu>> MACHINE_BATTERY_MENU =
        MENUS.register("machine_battery_menu", () -> IForgeMenuType.create(MachineBatteryMenu::new));

    public static final RegistryObject<MenuType<BlastFurnaceMenu>> BLAST_FURNACE_MENU =
            MENUS.register("blast_furnace_menu", () -> IForgeMenuType.create(BlastFurnaceMenu::new));

    public static final RegistryObject<MenuType<MachinePressMenu>> PRESS_MENU =
            MENUS.register("press_menu", () -> IForgeMenuType.create(MachinePressMenu::new));

    public static final RegistryObject<MenuType<AnvilMenu>> ANVIL_MENU =
            MENUS.register("anvil_menu", () ->
                    IForgeMenuType.create(AnvilMenu::new));

    public static final RegistryObject<MenuType<MachineWoodBurnerMenu>> WOOD_BURNER_MENU =
            MENUS.register("wood_burner_menu", () -> IForgeMenuType.create(MachineWoodBurnerMenu::new));

    public static final RegistryObject<MenuType<ShredderMenu>> SHREDDER_MENU =
            MENUS.register("shredder_menu", () -> IForgeMenuType.create(ShredderMenu::new));


    public static void register(IEventBus eventBus) {
        MENUS.register(eventBus);
    }
}