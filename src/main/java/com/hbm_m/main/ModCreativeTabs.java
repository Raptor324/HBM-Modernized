package com.hbm_m.main;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.item.ModItems;
import com.hbm_m.item.tags_and_tiers.ModIngots;
import com.hbm_m.lib.RefStrings;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

public class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
        DeferredRegister.create(RefStrings.MODID, Registries.CREATIVE_MODE_TAB);

    private static CreativeModeTab.Builder tabBuilder(int column) {
        return CreativeModeTab.builder(CreativeModeTab.Row.TOP, column);
    }

    // Регистрация всех наших вкладок
    public static final RegistrySupplier<CreativeModeTab> NTM_RESOURCES_TAB = CREATIVE_MODE_TABS.register("ntm_resources_tab",
            () -> tabBuilder(0)
                    .title(Component.translatable("itemGroup." + RefStrings.MODID + ".ntm_resources_tab"))
                    .icon(() -> new ItemStack(ModItems.getIngot(ModIngots.URANIUM).get()))
                    .displayItems((params, output) -> CreativeModeTabEventHandler.populateResourceTab(output::accept))
                    .build());

    public static final RegistrySupplier<CreativeModeTab> NTM_FUEL_TAB = CREATIVE_MODE_TABS.register("ntm_fuel_tab",
            () -> tabBuilder(1)
                    .title(Component.translatable("itemGroup." + RefStrings.MODID + ".ntm_fuel_tab"))
                    .icon(() -> new ItemStack(ModItems.CREATIVE_BATTERY.get()))
                    .displayItems((params, output) -> CreativeModeTabEventHandler.populateFuelTab(output::accept))
                    .build());

    public static final RegistrySupplier<CreativeModeTab> NTM_TEMPLATES_TAB = CREATIVE_MODE_TABS.register("ntm_templates_tab",
            () -> tabBuilder(2)
                    .title(Component.translatable("itemGroup." + RefStrings.MODID + ".ntm_templates_tab"))
                    .icon(() -> new ItemStack(ModItems.ASSEMBLY_TEMPLATE.get()))
                    .displayItems((params, output) -> CreativeModeTabEventHandler.populateTemplatesTab(output::accept))
                    .build());

    public static final RegistrySupplier<CreativeModeTab> NTM_ORES_TAB = CREATIVE_MODE_TABS.register("ntm_ores_tab",
            () -> tabBuilder(3)
                    .title(Component.translatable("itemGroup." + RefStrings.MODID + ".ntm_ores_tab"))
                    .icon(() -> new ItemStack(ModBlocks.URANIUM_ORE.get()))
                    .displayItems((params, output) -> CreativeModeTabEventHandler.populateOresTab(output::accept))
                    .build());

    public static final RegistrySupplier<CreativeModeTab> NTM_BUILDING_TAB = CREATIVE_MODE_TABS.register("ntm_building_tab",
            () -> tabBuilder(4)
                    .title(Component.translatable("itemGroup." + RefStrings.MODID + ".ntm_building_tab"))
                    .icon(() -> new ItemStack(ModBlocks.CONCRETE_HAZARD.get()))
                    .displayItems((params, output) -> CreativeModeTabEventHandler.populateBuildingTab(output::accept))
                    .build());

    public static final RegistrySupplier<CreativeModeTab> NTM_MACHINES_TAB = CREATIVE_MODE_TABS.register("ntm_machines_tab",
            () -> tabBuilder(5)
                    .title(Component.translatable("itemGroup." + RefStrings.MODID + ".ntm_machines_tab"))
                    .icon(() -> new ItemStack(ModBlocks.MACHINE_ASSEMBLER.get()))
                    .displayItems((params, output) -> CreativeModeTabEventHandler.populateMachinesTab(output::accept))
                    .build());

    public static final RegistrySupplier<CreativeModeTab> NTM_WEAPONS_TAB = CREATIVE_MODE_TABS.register("ntm_weapons_tab",
            () -> tabBuilder(6)
                    .title(Component.translatable("itemGroup." + RefStrings.MODID + ".ntm_weapons_tab"))
                    .icon(() -> new ItemStack(ModItems.GRENADE_IF.get()))
                    .displayItems((params, output) -> CreativeModeTabEventHandler.populateWeaponsTab(output::accept))
                    .build());

    public static final RegistrySupplier<CreativeModeTab> NTM_CONSUMABLES_TAB = CREATIVE_MODE_TABS.register("ntm_consumables_tab",
            () -> tabBuilder(7)
                    .title(Component.translatable("itemGroup." + RefStrings.MODID + ".ntm_consumables_tab"))
                    .icon(() -> new ItemStack(ModItems.RADAWAY.get()))
                    .displayItems((params, output) -> CreativeModeTabEventHandler.populateConsumablesTab(output::accept))
                    .build());

    public static final RegistrySupplier<CreativeModeTab> NTM_SPAREPARTS_TAB = CREATIVE_MODE_TABS.register("ntm_spareparts_tab",
            () -> tabBuilder(8)
                    .title(Component.translatable("itemGroup." + RefStrings.MODID + ".ntm_spareparts_tab"))
                    .icon(() -> new ItemStack(ModItems.PLATE_DESH.get()))
                    .displayItems((params, output) -> CreativeModeTabEventHandler.populateSparepartsTab(output::accept))
                    .build());

//     public static final RegistrySupplier<CreativeModeTab> NTM_INSTRUMENTS_TAB = CREATIVE_MODE_TABS.register("ntm_instruments_tab",
//             () -> CreativeModeTab.builder()
//                     .title(Component.translatable("itemGroup." + RefStrings.MODID + ".ntm_instruments_tab"))
//                     .icon(() -> new ItemStack(ModItems.GEIGER_COUNTER.get()))
//                     .build());

    public static void init() {
        CREATIVE_MODE_TABS.register();
    }
}