package com.hbm_m.main;

import com.hbm_m.item.tags_and_tiers.ModIngots;
import com.hbm_m.item.ModItems;
import com.hbm_m.block.ModBlocks;
import com.hbm_m.lib.RefStrings;
import dev.architectury.registry.CreativeTabRegistry;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

public class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
        DeferredRegister.create(RefStrings.MODID, Registries.CREATIVE_MODE_TAB);

    // Регистрация всех наших вкладок
    public static final RegistrySupplier<CreativeModeTab> NTM_RESOURCES_TAB = CREATIVE_MODE_TABS.register("ntm_resources_tab",
            () -> CreativeTabRegistry.create(
                    Component.translatable("itemGroup." + RefStrings.MODID + ".ntm_resources_tab"),
                    () -> new ItemStack(ModItems.getIngot(ModIngots.URANIUM).get())));

    public static final RegistrySupplier<CreativeModeTab> NTM_FUEL_TAB = CREATIVE_MODE_TABS.register("ntm_fuel_tab",
            () -> CreativeTabRegistry.create(
                    Component.translatable("itemGroup." + RefStrings.MODID + ".ntm_fuel_tab"),
                    () -> new ItemStack(ModItems.CREATIVE_BATTERY.get())));

    public static final RegistrySupplier<CreativeModeTab> NTM_TEMPLATES_TAB = CREATIVE_MODE_TABS.register("ntm_templates_tab",
            () -> CreativeTabRegistry.create(
                    Component.translatable("itemGroup." + RefStrings.MODID + ".ntm_templates_tab"),
                    () -> new ItemStack(ModItems.ASSEMBLY_TEMPLATE.get())));

    public static final RegistrySupplier<CreativeModeTab> NTM_ORES_TAB = CREATIVE_MODE_TABS.register("ntm_ores_tab",
            () -> CreativeTabRegistry.create(
                    Component.translatable("itemGroup." + RefStrings.MODID + ".ntm_ores_tab"),
                    () -> new ItemStack(ModBlocks.URANIUM_ORE.get())));

    public static final RegistrySupplier<CreativeModeTab> NTM_BUILDING_TAB = CREATIVE_MODE_TABS.register("ntm_building_tab",
            () -> CreativeTabRegistry.create(
                    Component.translatable("itemGroup." + RefStrings.MODID + ".ntm_building_tab"),
                    () -> new ItemStack(ModBlocks.CONCRETE_HAZARD.get())));

    public static final RegistrySupplier<CreativeModeTab> NTM_MACHINES_TAB = CREATIVE_MODE_TABS.register("ntm_machines_tab",
            () -> CreativeTabRegistry.create(
                    Component.translatable("itemGroup." + RefStrings.MODID + ".ntm_machines_tab"),
                    () -> new ItemStack(ModBlocks.MACHINE_ASSEMBLER.get())));

    public static final RegistrySupplier<CreativeModeTab> NTM_WEAPONS_TAB = CREATIVE_MODE_TABS.register("ntm_weapons_tab",
            () -> CreativeTabRegistry.create(
                    Component.translatable("itemGroup." + RefStrings.MODID + ".ntm_weapons_tab"),
                    () -> new ItemStack(ModItems.GRENADE_IF.get())));

    public static final RegistrySupplier<CreativeModeTab> NTM_CONSUMABLES_TAB = CREATIVE_MODE_TABS.register("ntm_consumables_tab",
            () -> CreativeTabRegistry.create(
                    Component.translatable("itemGroup." + RefStrings.MODID + ".ntm_consumables_tab"),
                    () -> new ItemStack(ModItems.RADAWAY.get())));

    public static final RegistrySupplier<CreativeModeTab> NTM_SPAREPARTS_TAB = CREATIVE_MODE_TABS.register("ntm_spareparts_tab",
            () -> CreativeTabRegistry.create(
                    Component.translatable("itemGroup." + RefStrings.MODID + ".ntm_spareparts_tab"),
                    () -> new ItemStack(ModItems.PLATE_DESH.get())));

//     public static final RegistryObject<CreativeModeTab> NTM_INSTRUMENTS_TAB = CREATIVE_MODE_TABS.register("ntm_instruments_tab",
//             () -> CreativeModeTab.builder()
//                     .title(Component.translatable("itemGroup." + RefStrings.MODID + ".ntm_instruments_tab"))
//                     .icon(() -> new ItemStack(ModItems.GEIGER_COUNTER.get()))
//                     .build());

    public static void init() {
        CREATIVE_MODE_TABS.register();
    }
}