package com.hbm_m.menu;

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

    public static final RegistryObject<MenuType<ArmorTableMenu>> ARMOR_TABLE_MENU =
            MENUS.register("armor_table_menu", () -> IForgeMenuType.create(ArmorTableMenu::new));

    public static final RegistryObject<MenuType<MachineAssemblerMenu>> MACHINE_ASSEMBLER_MENU =
            MENUS.register("machine_assembler_menu", () -> IForgeMenuType.create(MachineAssemblerMenu::new));

            
    public static void register(IEventBus eventBus) {
        MENUS.register(eventBus);
    }
}