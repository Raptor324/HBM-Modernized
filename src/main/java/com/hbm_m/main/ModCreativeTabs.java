package com.hbm_m.main;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.creativetabs.MissileTab;
import com.hbm_m.creativetabs.NukeTab;
import com.hbm_m.item.ModItems;
import com.hbm_m.item.tags_and_tiers.ModIngots;
import com.hbm_m.lib.RefStrings;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.resources.ResourceLocation;

/**
 * Вкладки повторяют оригинальный 1.7.10 (MainRegistry: parts/control/template/blocks/
 * machine/nuke/missile/weapon/consumable) — и состав, и порядок вкладок.
 */
public class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
        DeferredRegister.create(RefStrings.MODID, Registries.CREATIVE_MODE_TAB);

    private static CreativeModeTab.Builder tabBuilder() {
        //? if forge || neoforge {
        return CreativeModeTab.builder();
        //?} else {
        /*return CreativeModeTab.builder(CreativeModeTab.Row.TOP, 0);
        *///?}
    }

    /**
     * Forge/NeoForge: {@code withTabsBefore} — порядок вкладок (не по registry ID).
     */
    private static CreativeModeTab.Builder tabBuilderAfter(String previousTabId) {
        //? if forge || neoforge {
        return CreativeModeTab.builder()
                .withTabsBefore(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, previousTabId));
        //?} else {
        /*return CreativeModeTab.builder(CreativeModeTab.Row.TOP, 0);
        *///?}
    }

    // Оригинал: PartsTab (иконка — ingot_uranium)
    public static final RegistrySupplier<CreativeModeTab> NTM_PARTS_TAB = CREATIVE_MODE_TABS.register("ntm_parts_tab",
            () -> tabBuilder()
                    .title(Component.translatable("itemGroup." + RefStrings.MODID + ".ntm_parts_tab"))
                    .icon(() -> new ItemStack(ModItems.getIngot(ModIngots.URANIUM).get()))
                    .displayItems((params, output) -> CreativeModeTabEventHandler.populatePartsTab(CreativeModeTabEventHandler.deduplicated(output::accept)))
                    .build());

    // Оригинал: ControlTab (иконка — pellet_rtg)
    public static final RegistrySupplier<CreativeModeTab> NTM_CONTROL_TAB = CREATIVE_MODE_TABS.register("ntm_control_tab",
            () -> tabBuilderAfter("ntm_parts_tab")
                    .title(Component.translatable("itemGroup." + RefStrings.MODID + ".ntm_control_tab"))
                    .icon(() -> new ItemStack(ModItems.PELLET_RTG.get()))
                    .displayItems((params, output) -> CreativeModeTabEventHandler.populateControlTab(CreativeModeTabEventHandler.deduplicated(output::accept)))
                    .build());

    // Оригинал: TemplateTab (иконка — blueprints)
    public static final RegistrySupplier<CreativeModeTab> NTM_TEMPLATE_TAB = CREATIVE_MODE_TABS.register("ntm_template_tab",
            () -> tabBuilderAfter("ntm_control_tab")
                    .title(Component.translatable("itemGroup." + RefStrings.MODID + ".ntm_template_tab"))
                    .icon(() -> new ItemStack(ModItems.ASSEMBLY_TEMPLATE.get()))
                    .displayItems((params, output) -> CreativeModeTabEventHandler.populateTemplatesTab(CreativeModeTabEventHandler.deduplicated(output::accept)))
                    .build());

    // Оригинал: BlockTab (иконка — ore_uranium)
    public static final RegistrySupplier<CreativeModeTab> NTM_BLOCKS_TAB = CREATIVE_MODE_TABS.register("ntm_blocks_tab",
            () -> tabBuilderAfter("ntm_template_tab")
                    .title(Component.translatable("itemGroup." + RefStrings.MODID + ".ntm_blocks_tab"))
                    .icon(() -> new ItemStack(ModBlocks.URANIUM_ORE.get()))
                    .displayItems((params, output) -> CreativeModeTabEventHandler.populateBlocksTab(CreativeModeTabEventHandler.deduplicated(output::accept)))
                    .build());

    // Оригинал: MachineTab (иконка — pwr_controller)
    public static final RegistrySupplier<CreativeModeTab> NTM_MACHINE_TAB = CREATIVE_MODE_TABS.register("ntm_machine_tab",
            () -> tabBuilderAfter("ntm_blocks_tab")
                    .title(Component.translatable("itemGroup." + RefStrings.MODID + ".ntm_machine_tab"))
                    .icon(() -> new ItemStack(ModBlocks.PWR_CONTROLLER.get()))
                    .displayItems((params, output) -> CreativeModeTabEventHandler.populateMachinesTab(CreativeModeTabEventHandler.deduplicated(output::accept)))
                    .build());

    // Оригинал: NukeTab (иконка — nuke_man)
    public static final RegistrySupplier<CreativeModeTab> NTM_NUKE_TAB = CREATIVE_MODE_TABS.register("ntm_nuke_tab",
            () -> {
                CreativeModeTab.Builder builder = tabBuilderAfter("ntm_machine_tab")
                        .title(Component.translatable("itemGroup." + RefStrings.MODID + ".ntm_nuke_tab"))
                        .icon(() -> new ItemStack(ModBlocks.NUKE_FAT_MAN.get()))
                        .displayItems((params, output) -> CreativeModeTabEventHandler.populateNukeTab(CreativeModeTabEventHandler.deduplicated(output::accept)));
                NukeTab.applyBackgroundTexture(builder);
                return builder.build();
            });

    // Оригинал: MissileTab (иконка — missile_nuclear)
    public static final RegistrySupplier<CreativeModeTab> NTM_MISSILE_TAB = CREATIVE_MODE_TABS.register("ntm_missile_tab",
            () -> tabBuilderAfter("ntm_nuke_tab")
                    .title(Component.translatable("itemGroup." + RefStrings.MODID + ".ntm_missile_tab"))
                    .icon(() -> new ItemStack(ModItems.MISSILE_NUCLEAR.get()))
                    .displayItems((params, output) -> CreativeModeTabEventHandler.populateMissilesTab(CreativeModeTabEventHandler.deduplicated(output::accept)))
                    .build());

    // Оригинал: WeaponTab (иконка — gun_greasegun; у нас его пока нет, берём ближайшее оружие)
    public static final RegistrySupplier<CreativeModeTab> NTM_WEAPON_TAB = CREATIVE_MODE_TABS.register("ntm_weapon_tab",
            () -> tabBuilderAfter("ntm_missile_tab")
                    .title(Component.translatable("itemGroup." + RefStrings.MODID + ".ntm_weapon_tab"))
                    .icon(() -> new ItemStack(ModItems.GUN_B92.get()))
                    .displayItems((params, output) -> CreativeModeTabEventHandler.populateWeaponsTab(CreativeModeTabEventHandler.deduplicated(output::accept)))
                    .build());

    // Оригинал: ConsumableTab (иконка — bottle_nuka)
    public static final RegistrySupplier<CreativeModeTab> NTM_CONSUMABLE_TAB = CREATIVE_MODE_TABS.register("ntm_consumable_tab",
            () -> tabBuilderAfter("ntm_weapon_tab")
                    .title(Component.translatable("itemGroup." + RefStrings.MODID + ".ntm_consumable_tab"))
                    .icon(() -> new ItemStack(ModItems.BOTTLE_NUKA.get()))
                    .displayItems((params, output) -> CreativeModeTabEventHandler.populateConsumablesTab(CreativeModeTabEventHandler.deduplicated(output::accept)))
                    .build());



    /**
     * Временная вкладка для новых, ещё не отсортированных по основным вкладкам предметов/блоков.
     */
//     public static final RegistrySupplier<CreativeModeTab> NTM_DEV_TAB = CREATIVE_MODE_TABS.register("ntm_dev_tab",

//             () -> tabBuilderAfter("ntm_consumables_tab", 11)

//                     .title(Component.translatable("itemGroup." + RefStrings.MODID + ".ntm_dev_tab"))

//                     .icon(() -> new ItemStack(ModBlocks.BROADCASTER.get()))

//                     .displayItems((params, output) -> CreativeModeTabEventHandler.populateDevItemsTab(CreativeModeTabEventHandler.deduplicated(output::accept)))

//                     .build());



//     public static final RegistrySupplier<CreativeModeTab> NTM_INSTRUMENTS_TAB = CREATIVE_MODE_TABS.register("ntm_instruments_tab",

//             () -> CreativeModeTab.builder()

//                     .title(Component.translatable("itemGroup." + RefStrings.MODID + ".ntm_instruments_tab"))

//                     .icon(() -> new ItemStack(ModItems.GEIGER_COUNTER.get()))

//                     .build());



    public static void init() {
        CREATIVE_MODE_TABS.register();
    }
}
