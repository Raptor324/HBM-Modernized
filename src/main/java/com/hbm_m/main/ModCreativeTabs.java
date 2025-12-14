package com.hbm_m.main;

import com.hbm_m.item.tags_and_tiers.ModIngots;
import com.hbm_m.item.ModItems;
import com.hbm_m.block.ModBlocks;
import com.hbm_m.lib.RefStrings;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = 
        DeferredRegister.create(Registries.CREATIVE_MODE_TAB, RefStrings.MODID);

    // Регистрация всех наших вкладок
    public static final RegistryObject<CreativeModeTab> NTM_RESOURCES_TAB = CREATIVE_MODE_TABS.register("ntm_resources_tab",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup." + RefStrings.MODID + ".ntm_resources_tab"))
                    .icon(() -> new ItemStack(ModItems.getIngot(ModIngots.URANIUM).get()))
                    .build());

    public static final RegistryObject<CreativeModeTab> NTM_FUEL_TAB = CREATIVE_MODE_TABS.register("ntm_fuel_tab",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup." + RefStrings.MODID + ".ntm_fuel_tab"))
                    .icon(() -> new ItemStack(ModItems.CREATIVE_BATTERY.get())) 
                    .build());

    public static final RegistryObject<CreativeModeTab> NTM_TEMPLATES_TAB = CREATIVE_MODE_TABS.register("ntm_templates_tab",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup." + RefStrings.MODID + ".ntm_templates_tab"))
                    .icon(() -> new ItemStack(ModItems.ASSEMBLY_TEMPLATE.get()))
                    .build());

    public static final RegistryObject<CreativeModeTab> NTM_ORES_TAB = CREATIVE_MODE_TABS.register("ntm_ores_tab",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup." + RefStrings.MODID + ".ntm_ores_tab"))
                    .icon(() -> new ItemStack(ModBlocks.URANIUM_ORE.get()))
                    .build());

    public static final RegistryObject<CreativeModeTab> NTM_BUILDING_TAB = CREATIVE_MODE_TABS.register("ntm_building_tab",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup." + RefStrings.MODID + ".ntm_building_tab"))
                    .icon(() -> new ItemStack(ModBlocks.CONCRETE_HAZARD.get()))
                    .build());

    public static final RegistryObject<CreativeModeTab> NTM_MACHINES_TAB = CREATIVE_MODE_TABS.register("ntm_machines_tab",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup." + RefStrings.MODID + ".ntm_machines_tab"))
                    .icon(() -> new ItemStack(ModBlocks.MACHINE_ASSEMBLER.get()))
                    .build());

    public static final RegistryObject<CreativeModeTab> NTM_WEAPONS_TAB = CREATIVE_MODE_TABS.register("ntm_weapons_tab",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup." + RefStrings.MODID + ".ntm_weapons_tab"))
                    .icon(() -> new ItemStack(ModItems.GRENADE_IF.get()))
                    .build());

    public static final RegistryObject<CreativeModeTab> NTM_CONSUMABLES_TAB = CREATIVE_MODE_TABS.register("ntm_consumables_tab",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup." + RefStrings.MODID + ".ntm_consumables_tab"))
                    .icon(() -> new ItemStack(ModItems.RADAWAY.get()))
                    .build());

    public static final RegistryObject<CreativeModeTab> NTM_SPAREPARTS_TAB = CREATIVE_MODE_TABS.register("ntm_spareparts_tab",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup." + RefStrings.MODID + ".ntm_spareparts_tab"))
                    .icon(() -> new ItemStack(ModItems.PLATE_DESH.get()))
                    .build());

    public static final RegistryObject<CreativeModeTab> NTM_INSTRUMENTS_TAB = CREATIVE_MODE_TABS.register("ntm_instruments_tab",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup." + RefStrings.MODID + ".ntm_instruments_tab"))
                    .icon(() -> new ItemStack(ModItems.GEIGER_COUNTER.get()))
                    .build());

    // Метод для регистрации всех вкладок
    public static void register(IEventBus eventBus) {
        CREATIVE_MODE_TABS.register(eventBus);
    }
}