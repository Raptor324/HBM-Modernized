package com.hbm_m.main;

import com.hbm_m.item.ModItems;
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
                    .icon(() -> new ItemStack(ModItems.ALLOY_SWORD.get())) // Временно используем меч как иконку
                    .build());

    public static final RegistryObject<CreativeModeTab> NTM_FUEL_TAB = CREATIVE_MODE_TABS.register("ntm_fuel_tab",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup." + RefStrings.MODID + ".ntm_fuel_tab"))
                    .icon(() -> new ItemStack(ModItems.ALLOY_SWORD.get())) 
                    .build());

    public static final RegistryObject<CreativeModeTab> NTM_TEMPLATES_TAB = CREATIVE_MODE_TABS.register("ntm_templates_tab",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup." + RefStrings.MODID + ".ntm_templates_tab"))
                    .icon(() -> new ItemStack(ModItems.ALLOY_SWORD.get()))
                    .build());

    public static final RegistryObject<CreativeModeTab> NTM_ORES_TAB = CREATIVE_MODE_TABS.register("ntm_ores_tab",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup." + RefStrings.MODID + ".ntm_ores_tab"))
                    .icon(() -> new ItemStack(ModItems.ALLOY_SWORD.get()))
                    .build());

    public static final RegistryObject<CreativeModeTab> NTM_MACHINES_TAB = CREATIVE_MODE_TABS.register("ntm_machines_tab",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup." + RefStrings.MODID + ".ntm_machines_tab"))
                    .icon(() -> new ItemStack(ModItems.ALLOY_SWORD.get()))
                    .build());

    public static final RegistryObject<CreativeModeTab> NTM_BOMBS_TAB = CREATIVE_MODE_TABS.register("ntm_bombs_tab",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup." + RefStrings.MODID + ".ntm_bombs_tab"))
                    .icon(() -> new ItemStack(ModItems.ALLOY_SWORD.get()))
                    .build());

    public static final RegistryObject<CreativeModeTab> NTM_MISSILES_TAB = CREATIVE_MODE_TABS.register("ntm_missiles_tab",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup." + RefStrings.MODID + ".ntm_missiles_tab"))
                    .icon(() -> new ItemStack(ModItems.ALLOY_SWORD.get()))
                    .build());

    public static final RegistryObject<CreativeModeTab> NTM_WEAPONS_TAB = CREATIVE_MODE_TABS.register("ntm_weapons_tab",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup." + RefStrings.MODID + ".ntm_weapons_tab"))
                    .icon(() -> new ItemStack(ModItems.ALLOY_SWORD.get()))
                    .build());

    public static final RegistryObject<CreativeModeTab> NTM_CONSUMABLES_TAB = CREATIVE_MODE_TABS.register("ntm_consumables_tab",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup." + RefStrings.MODID + ".ntm_consumables_tab"))
                    .icon(() -> new ItemStack(ModItems.ALLOY_SWORD.get()))
                    .build());

    // Метод для регистрации всех вкладок
    public static void register(IEventBus eventBus) {
        CREATIVE_MODE_TABS.register(eventBus);
    }
}