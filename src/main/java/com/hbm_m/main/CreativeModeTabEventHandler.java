package com.hbm_m.main;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import com.hbm_m.api.fluids.HbmFluidRegistry;
import com.hbm_m.api.fluids.ModFluids;
import com.hbm_m.armormod.item.ItemArmorMod;
import com.hbm_m.block.ModBlocks;
import com.hbm_m.client.ClientSetup;
import com.hbm_m.config.ModClothConfig;
import com.hbm_m.item.ModItems;
import com.hbm_m.item.fekal_electric.ModBatteryItem;
import com.hbm_m.item.liquids.FluidBarrelItem;
import com.hbm_m.item.liquids.FluidIdentifierItem;
import com.hbm_m.item.tags_and_tiers.ModIngots;
import com.hbm_m.item.tags_and_tiers.ModPowders;

import dev.architectury.registry.registries.RegistrySupplier;
import dev.architectury.utils.Env;
import dev.architectury.utils.EnvExecutor;
//? if fabric {
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
//?}
//? if forge {
/*import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
*///?}
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;


/**
 * Наполнение креативных вкладок (логика из старого Forge {@code MainRegistry#addCreative}).
 */
public final class CreativeModeTabEventHandler {

    private CreativeModeTabEventHandler() {
    }

    //? if forge {
    /*public static void onBuildCreativeModeTabContents(BuildCreativeModeTabContentsEvent event) {
        MainRegistry.LOGGER.info("Building creative tab contents for: " + event.getTabKey());

        if (event.getTab() == ModCreativeTabs.NTM_WEAPONS_TAB.get() || event.getTabKey() == CreativeModeTabs.SEARCH) {
            populateWeaponsTab((stack, vis) -> event.accept(stack, vis));
        }

        if (event.getTabKey() == CreativeModeTabs.COMBAT) {
            populateCombatTab((stack, vis) -> event.accept(stack, vis));
        }

        if (event.getTab() == ModCreativeTabs.NTM_RESOURCES_TAB.get() || event.getTabKey() == CreativeModeTabs.SEARCH) {
            populateResourceTab((stack, vis) -> event.accept(stack, vis));
        }

        if (event.getTab() == ModCreativeTabs.NTM_CONSUMABLES_TAB.get() || event.getTabKey() == CreativeModeTabs.SEARCH) {
            populateConsumablesTab((stack, vis) -> event.accept(stack, vis));
        }

        if (event.getTab() == ModCreativeTabs.NTM_SPAREPARTS_TAB.get() || event.getTabKey() == CreativeModeTabs.SEARCH) {
            populateSparepartsTab((stack, vis) -> event.accept(stack, vis));
        }

        if (event.getTab() == ModCreativeTabs.NTM_ORES_TAB.get() || event.getTabKey() == CreativeModeTabs.SEARCH) {
            populateOresTab((stack, vis) -> event.accept(stack, vis));
        }

        if (event.getTab() == ModCreativeTabs.NTM_BUILDING_TAB.get() || event.getTabKey() == CreativeModeTabs.SEARCH) {
            populateBuildingTab((stack, vis) -> event.accept(stack, vis));
        }

        if (event.getTab() == ModCreativeTabs.NTM_MACHINES_TAB.get() || event.getTabKey() == CreativeModeTabs.SEARCH) {
            populateMachinesTab((stack, vis) -> event.accept(stack, vis));
        }

        if (event.getTab() == ModCreativeTabs.NTM_FUEL_TAB.get() || event.getTabKey() == CreativeModeTabs.SEARCH) {
            populateFuelTab((stack, vis) -> event.accept(stack, vis));
        }

        if (event.getTab() == ModCreativeTabs.NTM_TEMPLATES_TAB.get() || event.getTabKey() == CreativeModeTabs.SEARCH) {
            populateTemplatesTab((stack, vis) -> event.accept(stack, vis));
        }

    }
    *///?}

    //? if fabric {
    public static void initFabric() {
        ItemGroupEvents.MODIFY_ENTRIES_ALL.register((tabGroup, entries) -> {
            // Кастомные вкладки наполняются через `CreativeModeTab#displayItems` при их регистрации.
            // На Fabric оставляем только точечное добавление в ванильные вкладки.
            if (tabGroup.equals(CreativeModeTabs.COMBAT)) {
                populateCombatTab((stack, vis) -> entries.accept(stack, vis));
            }
        });
    }
    //?}

    public static void populateWeaponsTab(BiConsumer<ItemStack, CreativeModeTab.TabVisibility> acceptor) {
        // Упрощенный Consumer, по умолчанию использующий PARENT_AND_SEARCH_TABS
        Consumer<ItemStack> add = stack -> acceptor.accept(stack, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);

        add.accept(new ItemStack(ModBlocks.BARREL_RED.get()));
        add.accept(new ItemStack(ModBlocks.BARREL_PINK.get()));
        add.accept(new ItemStack(ModBlocks.BARREL_TAINT.get()));
        add.accept(new ItemStack(ModBlocks.BARREL_LOX.get()));
        add.accept(new ItemStack(ModBlocks.BARREL_YELLOW.get()));
        add.accept(new ItemStack(ModBlocks.BARREL_VITRIFIED.get()));
        add.accept(new ItemStack(ModBlocks.BARBED_WIRE_FIRE.get()));
        add.accept(new ItemStack(ModBlocks.BARBED_WIRE_POISON.get()));
        add.accept(new ItemStack(ModBlocks.BARBED_WIRE_RAD.get()));
        add.accept(new ItemStack(ModBlocks.BARBED_WIRE_WITHER.get()));
        add.accept(new ItemStack(ModBlocks.BARBED_WIRE.get()));
        add.accept(new ItemStack(ModItems.DETONATOR.get()));
        add.accept(new ItemStack(ModItems.MULTI_DETONATOR.get()));
        add.accept(new ItemStack(ModItems.RANGE_DETONATOR.get()));
        add.accept(new ItemStack(ModItems.GRENADE.get()));
        add.accept(new ItemStack(ModItems.GRENADEHE.get()));
        add.accept(new ItemStack(ModItems.GRENADEFIRE.get()));
        add.accept(new ItemStack(ModItems.GRENADESMART.get()));
        add.accept(new ItemStack(ModItems.GRENADESLIME.get()));
        add.accept(new ItemStack(ModItems.GRENADE_IF.get()));
        add.accept(new ItemStack(ModItems.GRENADE_IF_HE.get()));
        add.accept(new ItemStack(ModItems.GRENADE_IF_SLIME.get()));
        add.accept(new ItemStack(ModItems.GRENADE_IF_FIRE.get()));
        add.accept(new ItemStack(ModItems.GRENADE_NUC.get()));
        add.accept(new ItemStack(ModBlocks.MINE_AP.get()));
        add.accept(new ItemStack(ModBlocks.MINE_FAT.get()));
        add.accept(new ItemStack(ModBlocks.NUKE_FAT_MAN.get()));
        add.accept(new ItemStack(ModItems.FAT_MAN_EXPLOSIVE.get()));
        add.accept(new ItemStack(ModItems.FAT_MAN_IGNITER.get()));
        add.accept(new ItemStack(ModItems.FAT_MAN_CORE.get()));
        add.accept(new ItemStack(ModBlocks.AIRBOMB.get()));
        add.accept(new ItemStack(ModItems.AIRBOMB_A.get()));
        add.accept(new ItemStack(ModBlocks.BALEBOMB_TEST.get()));
        add.accept(new ItemStack(ModItems.AIRNUKEBOMB_A.get()));
        add.accept(new ItemStack(ModBlocks.DET_MINER.get()));
        add.accept(new ItemStack(ModBlocks.GIGA_DET.get()));
        add.accept(new ItemStack(ModBlocks.WASTE_CHARGE.get()));
        add.accept(new ItemStack(ModBlocks.SMOKE_BOMB.get()));
        add.accept(new ItemStack(ModBlocks.EXPLOSIVE_CHARGE.get()));
        add.accept(new ItemStack(ModBlocks.NUCLEAR_CHARGE.get()));
        add.accept(new ItemStack(ModBlocks.C4.get()));
        add.accept(new ItemStack(ModBlocks.DUD_CONVENTIONAL.get()));
        add.accept(new ItemStack(ModBlocks.DUD_NUKE.get()));
        add.accept(new ItemStack(ModBlocks.DUD_SALTED.get()));
        add.accept(new ItemStack(ModBlocks.LAUNCH_PAD.get()));
        add.accept(new ItemStack(ModBlocks.LAUNCH_PAD_RUSTED.get()));
        add.accept(new ItemStack(ModItems.DESIGNATOR.get()));
        add.accept(new ItemStack(ModItems.DESIGNATOR_RANGE.get()));
        add.accept(new ItemStack(ModItems.DESIGNATOR_MANUAL.get()));
        add.accept(new ItemStack(ModItems.MISSILE_TEST.get()));
    }

    // БРОНЯ И ИНСТРУМЕНТЫ
    public static void populateCombatTab(BiConsumer<ItemStack, CreativeModeTab.TabVisibility> acceptor) {
        // Упрощенный Consumer, по умолчанию использующий PARENT_AND_SEARCH_TABS
        Consumer<ItemStack> add = stack -> acceptor.accept(stack, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);

        add.accept(new ItemStack(ModItems.ALLOY_SWORD.get()));
        add.accept(new ItemStack(ModItems.ALLOY_AXE.get()));
        add.accept(new ItemStack(ModItems.ALLOY_PICKAXE.get()));
        add.accept(new ItemStack(ModItems.ALLOY_HOE.get()));
        add.accept(new ItemStack(ModItems.ALLOY_SHOVEL.get()));
        add.accept(new ItemStack(ModItems.STEEL_SWORD.get()));
        add.accept(new ItemStack(ModItems.STEEL_AXE.get()));
        add.accept(new ItemStack(ModItems.STEEL_PICKAXE.get()));
        add.accept(new ItemStack(ModItems.STEEL_HOE.get()));
        add.accept(new ItemStack(ModItems.STEEL_SHOVEL.get()));
        add.accept(new ItemStack(ModItems.TITANIUM_SWORD.get()));
        add.accept(new ItemStack(ModItems.TITANIUM_AXE.get()));
        add.accept(new ItemStack(ModItems.TITANIUM_PICKAXE.get()));
        add.accept(new ItemStack(ModItems.TITANIUM_HOE.get()));
        add.accept(new ItemStack(ModItems.TITANIUM_SHOVEL.get()));
        add.accept(new ItemStack(ModItems.STARMETAL_SWORD.get()));
        add.accept(new ItemStack(ModItems.STARMETAL_AXE.get()));
        add.accept(new ItemStack(ModItems.STARMETAL_PICKAXE.get()));
        add.accept(new ItemStack(ModItems.STARMETAL_HOE.get()));
        add.accept(new ItemStack(ModItems.STARMETAL_SHOVEL.get()));
        add.accept(new ItemStack(ModItems.METEORITE_SWORD.get()));
        add.accept(new ItemStack(ModItems.METEORITE_SWORD_SEARED.get()));

        // Силовая броня добавляется полностью заряженной
        add.accept(createChargedArmorStack(ModItems.T51_HELMET.get()));
        add.accept(createChargedArmorStack(ModItems.T51_CHESTPLATE.get()));
        add.accept(createChargedArmorStack(ModItems.T51_LEGGINGS.get()));
        add.accept(createChargedArmorStack(ModItems.T51_BOOTS.get()));

        add.accept(createChargedArmorStack(ModItems.AJR_HELMET.get()));
        add.accept(createChargedArmorStack(ModItems.AJR_CHESTPLATE.get()));
        add.accept(createChargedArmorStack(ModItems.AJR_LEGGINGS.get()));
        add.accept(createChargedArmorStack(ModItems.AJR_BOOTS.get()));

        add.accept(createChargedArmorStack(ModItems.AJRO_HELMET.get()));
        add.accept(createChargedArmorStack(ModItems.AJRO_CHESTPLATE.get()));
        add.accept(createChargedArmorStack(ModItems.AJRO_LEGGINGS.get()));
        add.accept(createChargedArmorStack(ModItems.AJRO_BOOTS.get()));

        add.accept(createChargedArmorStack(ModItems.BISMUTH_HELMET.get()));
        add.accept(createChargedArmorStack(ModItems.BISMUTH_CHESTPLATE.get()));
        add.accept(createChargedArmorStack(ModItems.BISMUTH_LEGGINGS.get()));
        add.accept(createChargedArmorStack(ModItems.BISMUTH_BOOTS.get()));

        add.accept(createChargedArmorStack(ModItems.DNT_HELMET.get()));
        add.accept(createChargedArmorStack(ModItems.DNT_CHESTPLATE.get()));
        add.accept(createChargedArmorStack(ModItems.DNT_LEGGINGS.get()));
        add.accept(createChargedArmorStack(ModItems.DNT_BOOTS.get()));

        add.accept(new ItemStack(ModItems.ALLOY_HELMET.get()));
        add.accept(new ItemStack(ModItems.ALLOY_CHESTPLATE.get()));
        add.accept(new ItemStack(ModItems.ALLOY_LEGGINGS.get()));
        add.accept(new ItemStack(ModItems.ALLOY_BOOTS.get()));
        add.accept(new ItemStack(ModItems.COBALT_HELMET.get()));
        add.accept(new ItemStack(ModItems.COBALT_CHESTPLATE.get()));
        add.accept(new ItemStack(ModItems.COBALT_LEGGINGS.get()));
        add.accept(new ItemStack(ModItems.COBALT_BOOTS.get()));
        add.accept(new ItemStack(ModItems.TITANIUM_HELMET.get()));
        add.accept(new ItemStack(ModItems.TITANIUM_CHESTPLATE.get()));
        add.accept(new ItemStack(ModItems.TITANIUM_LEGGINGS.get()));
        add.accept(new ItemStack(ModItems.TITANIUM_BOOTS.get()));
        add.accept(new ItemStack(ModItems.SECURITY_HELMET.get()));
        add.accept(new ItemStack(ModItems.SECURITY_CHESTPLATE.get()));
        add.accept(new ItemStack(ModItems.SECURITY_LEGGINGS.get()));
        add.accept(new ItemStack(ModItems.SECURITY_BOOTS.get()));

        add.accept(new ItemStack(ModItems.STEEL_HELMET.get()));
        add.accept(new ItemStack(ModItems.STEEL_CHESTPLATE.get()));
        add.accept(new ItemStack(ModItems.STEEL_LEGGINGS.get()));
        add.accept(new ItemStack(ModItems.STEEL_BOOTS.get()));
        add.accept(new ItemStack(ModItems.ASBESTOS_HELMET.get()));
        add.accept(new ItemStack(ModItems.ASBESTOS_CHESTPLATE.get()));
        add.accept(new ItemStack(ModItems.ASBESTOS_LEGGINGS.get()));
        add.accept(new ItemStack(ModItems.ASBESTOS_BOOTS.get()));
        add.accept(new ItemStack(ModItems.HAZMAT_HELMET.get()));
        add.accept(new ItemStack(ModItems.HAZMAT_CHESTPLATE.get()));
        add.accept(new ItemStack(ModItems.HAZMAT_LEGGINGS.get()));
        add.accept(new ItemStack(ModItems.HAZMAT_BOOTS.get()));
        add.accept(new ItemStack(ModItems.LIQUIDATOR_HELMET.get()));
        add.accept(new ItemStack(ModItems.LIQUIDATOR_CHESTPLATE.get()));
        add.accept(new ItemStack(ModItems.LIQUIDATOR_LEGGINGS.get()));
        add.accept(new ItemStack(ModItems.LIQUIDATOR_BOOTS.get()));
        add.accept(new ItemStack(ModItems.PAA_HELMET.get()));
        add.accept(new ItemStack(ModItems.PAA_CHESTPLATE.get()));
        add.accept(new ItemStack(ModItems.PAA_LEGGINGS.get()));
        add.accept(new ItemStack(ModItems.PAA_BOOTS.get()));
        add.accept(new ItemStack(ModItems.STARMETAL_HELMET.get()));
        add.accept(new ItemStack(ModItems.STARMETAL_CHESTPLATE.get()));
        add.accept(new ItemStack(ModItems.STARMETAL_LEGGINGS.get()));
        add.accept(new ItemStack(ModItems.STARMETAL_BOOTS.get()));

        // БРОНЯ
        add.accept(new ItemStack(ModItems.TITANIUM_HELMET.get()));
        add.accept(new ItemStack(ModItems.TITANIUM_CHESTPLATE.get()));
        add.accept(new ItemStack(ModItems.TITANIUM_LEGGINGS.get()));
        add.accept(new ItemStack(ModItems.TITANIUM_BOOTS.get()));

        add.accept(new ItemStack(ModItems.COBALT_HELMET.get()));
        add.accept(new ItemStack(ModItems.COBALT_CHESTPLATE.get()));
        add.accept(new ItemStack(ModItems.COBALT_LEGGINGS.get()));
        add.accept(new ItemStack(ModItems.COBALT_BOOTS.get()));

        add.accept(new ItemStack(ModItems.STEEL_HELMET.get()));
        add.accept(new ItemStack(ModItems.STEEL_CHESTPLATE.get()));
        add.accept(new ItemStack(ModItems.STEEL_LEGGINGS.get()));
        add.accept(new ItemStack(ModItems.STEEL_BOOTS.get()));

        add.accept(new ItemStack(ModItems.ALLOY_HELMET.get()));
        add.accept(new ItemStack(ModItems.ALLOY_CHESTPLATE.get()));
        add.accept(new ItemStack(ModItems.ALLOY_LEGGINGS.get()));
        add.accept(new ItemStack(ModItems.ALLOY_BOOTS.get()));

        add.accept(new ItemStack(ModItems.STARMETAL_HELMET.get()));
        add.accept(new ItemStack(ModItems.STARMETAL_CHESTPLATE.get()));
        add.accept(new ItemStack(ModItems.STARMETAL_LEGGINGS.get()));
        add.accept(new ItemStack(ModItems.STARMETAL_BOOTS.get()));

        //СПЕЦ БРОНЯ
        add.accept(new ItemStack(ModItems.SECURITY_HELMET.get()));
        add.accept(new ItemStack(ModItems.SECURITY_CHESTPLATE.get()));
        add.accept(new ItemStack(ModItems.SECURITY_LEGGINGS.get()));
        add.accept(new ItemStack(ModItems.SECURITY_BOOTS.get()));

        add.accept(new ItemStack(ModItems.ASBESTOS_HELMET.get()));
        add.accept(new ItemStack(ModItems.ASBESTOS_CHESTPLATE.get()));
        add.accept(new ItemStack(ModItems.ASBESTOS_LEGGINGS.get()));
        add.accept(new ItemStack(ModItems.ASBESTOS_BOOTS.get()));

        add.accept(new ItemStack(ModItems.HAZMAT_HELMET.get()));
        add.accept(new ItemStack(ModItems.HAZMAT_CHESTPLATE.get()));
        add.accept(new ItemStack(ModItems.HAZMAT_LEGGINGS.get()));
        add.accept(new ItemStack(ModItems.HAZMAT_BOOTS.get()));

        add.accept(new ItemStack(ModItems.PAA_HELMET.get()));
        add.accept(new ItemStack(ModItems.PAA_CHESTPLATE.get()));
        add.accept(new ItemStack(ModItems.PAA_LEGGINGS.get()));
        add.accept(new ItemStack(ModItems.PAA_BOOTS.get()));

        add.accept(new ItemStack(ModItems.LIQUIDATOR_HELMET.get()));
        add.accept(new ItemStack(ModItems.LIQUIDATOR_CHESTPLATE.get()));
        add.accept(new ItemStack(ModItems.LIQUIDATOR_LEGGINGS.get()));
        add.accept(new ItemStack(ModItems.LIQUIDATOR_BOOTS.get()));


        //МЕЧИ
        add.accept(new ItemStack(ModItems.TITANIUM_SWORD.get()));
        add.accept(new ItemStack(ModItems.STEEL_SWORD.get()));
        add.accept(new ItemStack(ModItems.ALLOY_SWORD.get()));
        add.accept(new ItemStack(ModItems.STARMETAL_SWORD.get()));

        //ТОПОРЫ
        add.accept(new ItemStack(ModItems.TITANIUM_AXE.get()));
        add.accept(new ItemStack(ModItems.STEEL_AXE.get()));
        add.accept(new ItemStack(ModItems.ALLOY_AXE.get()));
        add.accept(new ItemStack(ModItems.STARMETAL_AXE.get()));

        //КИРКИ
        add.accept(new ItemStack(ModItems.TITANIUM_PICKAXE.get()));
        add.accept(new ItemStack(ModItems.STEEL_PICKAXE.get()));
        add.accept(new ItemStack(ModItems.ALLOY_PICKAXE.get()));
        add.accept(new ItemStack(ModItems.STARMETAL_PICKAXE.get()));

        //ЛОПАТЫ
        add.accept(new ItemStack(ModItems.TITANIUM_SHOVEL.get()));
        add.accept(new ItemStack(ModItems.STEEL_SHOVEL.get()));
        add.accept(new ItemStack(ModItems.ALLOY_SHOVEL.get()));
        add.accept(new ItemStack(ModItems.STARMETAL_SHOVEL.get()));

        //МОТЫГИ
        add.accept(new ItemStack(ModItems.TITANIUM_HOE.get()));
        add.accept(new ItemStack(ModItems.STEEL_HOE.get()));
        add.accept(new ItemStack(ModItems.ALLOY_HOE.get()));
        add.accept(new ItemStack(ModItems.STARMETAL_HOE.get()));
    }

    // СЛИТКИ И РЕСУРСЫ
    public static void populateResourceTab(BiConsumer<ItemStack, CreativeModeTab.TabVisibility> acceptor) {
        // Упрощенный Consumer, по умолчанию использующий PARENT_AND_SEARCH_TABS
        Consumer<ItemStack> add = stack -> acceptor.accept(stack, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);

        // БАЗОВЫЕ ПРЕДМЕТЫ (все с ItemStack!)
        add.accept(new ItemStack(ModItems.BALL_TNT.get()));
        add.accept(new ItemStack(ModItems.ZIRCONIUM_SHARP.get()));
        add.accept(new ItemStack(ModItems.BORAX.get()));
        add.accept(new ItemStack(ModItems.DUST.get()));
        add.accept(new ItemStack(ModItems.DUST_TINY.get()));
        add.accept(new ItemStack(ModItems.CINNABAR.get()));
        add.accept(new ItemStack(ModItems.FIRECLAY_BALL.get()));
        add.accept(new ItemStack(ModItems.SULFUR.get()));
        add.accept(new ItemStack(ModItems.SEQUESTRUM.get()));
        add.accept(new ItemStack(ModItems.LIGNITE.get()));
        add.accept(new ItemStack(ModItems.FLUORITE.get()));
        add.accept(new ItemStack(ModItems.RAREGROUND_ORE_CHUNK.get()));
        add.accept(new ItemStack(ModItems.FIREBRICK.get()));
        add.accept(new ItemStack(ModItems.WOOD_ASH_POWDER.get()));
        add.accept(new ItemStack(ModItems.SCRAP.get()));
        add.accept(new ItemStack(ModItems.NUGGET_SILICON.get()));
        add.accept(new ItemStack(ModItems.BILLET_SILICON.get()));
        add.accept(new ItemStack(ModItems.BILLET_PLUTONIUM.get()));



        // Crystals (textures/crystall/*.png)
        add.accept(new ItemStack(ModItems.CRYSTAL_ALUMINIUM.get()));
        add.accept(new ItemStack(ModItems.CRYSTAL_BERYLLIUM.get()));
        add.accept(new ItemStack(ModItems.CRYSTAL_CHARRED.get()));
        add.accept(new ItemStack(ModItems.CRYSTAL_CINNEBAR.get()));
        add.accept(new ItemStack(ModItems.CRYSTAL_COAL.get()));
        add.accept(new ItemStack(ModItems.CRYSTAL_COBALT.get()));
        add.accept(new ItemStack(ModItems.CRYSTAL_COPPER.get()));
        add.accept(new ItemStack(ModItems.CRYSTAL_DIAMOND.get()));
        add.accept(new ItemStack(ModItems.CRYSTAL_FLUORITE.get()));
        add.accept(new ItemStack(ModItems.CRYSTAL_GOLD.get()));
        add.accept(new ItemStack(ModItems.CRYSTAL_HARDENED.get()));
        add.accept(new ItemStack(ModItems.CRYSTAL_HORN.get()));
        add.accept(new ItemStack(ModItems.CRYSTAL_IRON.get()));
        add.accept(new ItemStack(ModItems.CRYSTAL_LAPIS.get()));
        add.accept(new ItemStack(ModItems.CRYSTAL_LEAD.get()));
        add.accept(new ItemStack(ModItems.CRYSTAL_LITHIUM.get()));
        add.accept(new ItemStack(ModItems.CRYSTAL_NITER.get()));
        add.accept(new ItemStack(ModItems.CRYSTAL_OSMIRIDIUM.get()));
        add.accept(new ItemStack(ModItems.CRYSTAL_PHOSPHORUS.get()));
        add.accept(new ItemStack(ModItems.CRYSTAL_PLUTONIUM.get()));
        add.accept(new ItemStack(ModItems.CRYSTAL_PULSAR.get()));
        add.accept(new ItemStack(ModItems.CRYSTAL_RARE.get()));
        add.accept(new ItemStack(ModItems.CRYSTAL_REDSTONE.get()));
        add.accept(new ItemStack(ModItems.CRYSTAL_SCHRABIDIUM.get()));
        add.accept(new ItemStack(ModItems.CRYSTAL_SCHRARANIUM.get()));
        add.accept(new ItemStack(ModItems.CRYSTAL_STARMETAL.get()));
        add.accept(new ItemStack(ModItems.CRYSTAL_SULFUR.get()));
        add.accept(new ItemStack(ModItems.CRYSTAL_THORIUM.get()));
        add.accept(new ItemStack(ModItems.CRYSTAL_TITANIUM.get()));
        add.accept(new ItemStack(ModItems.CRYSTAL_TRIXITE.get()));
        add.accept(new ItemStack(ModItems.CRYSTAL_TUNGSTEN.get()));
        add.accept(new ItemStack(ModItems.CRYSTAL_URANIUM.get()));
        add.accept(new ItemStack(ModItems.CRYSTAL_VIRUS.get()));
        add.accept(new ItemStack(ModItems.CRYSTAL_XEN.get()));


        // СЛИТКИ
        for (ModIngots ingot : ModIngots.values()) {
            RegistrySupplier<Item> ingotItem = ModItems.getIngot(ingot);
            if (ingotItem != null && ingotItem.isPresent()) {
                add.accept(new ItemStack(ingotItem.get()));
            }

        }

        // Standalone tiny powders
        add.accept(new ItemStack(ModItems.LITHIUM_POWDER_TINY.get()));
        add.accept(new ItemStack(ModItems.CS137_POWDER_TINY.get()));
        add.accept(new ItemStack(ModItems.I131_POWDER_TINY.get()));
        add.accept(new ItemStack(ModItems.XE135_POWDER_TINY.get()));
        add.accept(new ItemStack(ModItems.PALEOGENITE_POWDER_TINY.get()));
        add.accept(new ItemStack(ModItems.NUCLEAR_WASTE_TINY.get()));
        add.accept(new ItemStack(ModItems.NUCLEAR_WASTE_LONG_TINY.get()));
        add.accept(new ItemStack(ModItems.NUCLEAR_WASTE_LONG_DEPLETED_TINY.get()));
        add.accept(new ItemStack(ModItems.NUCLEAR_WASTE_SHORT_TINY.get()));
        add.accept(new ItemStack(ModItems.NUCLEAR_WASTE_SHORT_DEPLETED_TINY.get()));
        add.accept(new ItemStack(ModItems.NUCLEAR_WASTE_VITRIFIED_TINY.get()));
        add.accept(new ItemStack(ModItems.NUGGET_MERCURY_TINY.get()));
        add.accept(new ItemStack(ModItems.COAL_POWDER_TINY.get()));

        // Standalone powders
        add.accept(new ItemStack(ModItems.COPPER_POWDER.get()));
        add.accept(new ItemStack(ModItems.DIAMOND_POWDER.get()));
        add.accept(new ItemStack(ModItems.EMERALD_POWDER.get()));
        add.accept(new ItemStack(ModItems.LAPIS_POWDER.get()));
        add.accept(new ItemStack(ModItems.QUARTZ_POWDER.get()));
        add.accept(new ItemStack(ModItems.LIGNITE_POWDER.get()));
        add.accept(new ItemStack(ModItems.FIRE_POWDER.get()));
        add.accept(new ItemStack(ModItems.LITHIUM_POWDER.get()));

        // ModPowders
        for (ModPowders powder : ModPowders.values()) {
            RegistrySupplier<Item> powderItem = ModItems.getPowders(powder);
            if (powderItem != null && powderItem.isPresent()) {
                add.accept(new ItemStack(powderItem.get()));
            }
        }

        // ОДИН ЦИКЛ ДЛЯ ВСЕХ ПОРОШКОВ ИЗ СЛИТКОВ (обычные + маленькие)
        for (ModIngots ingot : ModIngots.values()) {
            // Обычный порошок
            RegistrySupplier<Item> powder = ModItems.getPowder(ingot);
            if (powder != null && powder.isPresent()) {
                add.accept(new ItemStack(powder.get()));
            }

            // Маленький порошок
            ModItems.getTinyPowder(ingot).ifPresent(tiny -> {
                if (tiny != null && tiny.isPresent()) {
                    add.accept(new ItemStack(tiny.get()));
                }
            });
        }
    }

    // РАСХОДНИКИ И МОДИФИКАТОРЫ
    public static void populateConsumablesTab(BiConsumer<ItemStack, CreativeModeTab.TabVisibility> acceptor) {
        // Упрощенный Consumer, по умолчанию использующий PARENT_AND_SEARCH_TABS
        Consumer<ItemStack> add = stack -> acceptor.accept(stack, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
        add.accept(new ItemStack(ModBlocks.ARMOR_TABLE.get()));
        // АВТОМАТИЧЕСКОЕ ДОБАВЛЕНИЕ ВСЕХ МОДИФИКАТОРОВ
        // 1. Получаем все зарегистрированные предметы
        for (RegistrySupplier<Item> itemObject : ModItems.ITEMS) {
            if (!itemObject.isPresent()) {
                continue;
            }
            Item item = itemObject.get();
            if (item instanceof ItemArmorMod) {
                add.accept(new ItemStack(item));
                if (ModClothConfig.get().enableDebugLogging) {
                    MainRegistry.LOGGER.info("Automatically added Armor Mod [{}] to NTM Consumables tab", itemObject.getId());
                }
            }
        }
        add.accept(new ItemStack(ModItems.RADAWAY.get()));
        add.accept(new ItemStack(ModItems.CAN_KEY.get()));
        add.accept(new ItemStack(ModItems.CAN_EMPTY.get()));
        add.accept(new ItemStack(ModItems.CANNED_ASBESTOS.get()));
        add.accept(new ItemStack(ModItems.CANNED_ASS.get()));
        add.accept(new ItemStack(ModItems.CANNED_BARK.get()));
        add.accept(new ItemStack(ModItems.CANNED_BEEF.get()));
        add.accept(new ItemStack(ModItems.CANNED_BHOLE.get()));
        add.accept(new ItemStack(ModItems.CANNED_CHEESE.get()));
        add.accept(new ItemStack(ModItems.CANNED_CHINESE.get()));
        add.accept(new ItemStack(ModItems.CANNED_DIESEL.get()));
        add.accept(new ItemStack(ModItems.CANNED_FIST.get()));
        add.accept(new ItemStack(ModItems.CANNED_FRIED.get()));
        add.accept(new ItemStack(ModItems.CANNED_HOTDOGS.get()));
        add.accept(new ItemStack(ModItems.CANNED_JIZZ.get()));
        add.accept(new ItemStack(ModItems.CANNED_KEROSENE.get()));
        add.accept(new ItemStack(ModItems.CANNED_LEFTOVERS.get()));
        add.accept(new ItemStack(ModItems.CANNED_MILK.get()));
        add.accept(new ItemStack(ModItems.CANNED_MYSTERY.get()));
        add.accept(new ItemStack(ModItems.CANNED_NAPALM.get()));
        add.accept(new ItemStack(ModItems.CANNED_OIL.get()));
        add.accept(new ItemStack(ModItems.CANNED_PASHTET.get()));
        add.accept(new ItemStack(ModItems.CANNED_PIZZA.get()));
        add.accept(new ItemStack(ModItems.CANNED_RECURSION.get()));
        add.accept(new ItemStack(ModItems.CANNED_SPAM.get()));
        add.accept(new ItemStack(ModItems.CANNED_STEW.get()));
        add.accept(new ItemStack(ModItems.CANNED_TOMATO.get()));
        add.accept(new ItemStack(ModItems.CANNED_TUNA.get()));
        add.accept(new ItemStack(ModItems.CANNED_TUBE.get()));
        add.accept(new ItemStack(ModItems.CANNED_YOGURT.get()));
        add.accept(new ItemStack(ModItems.CAN_BEPIS.get()));
        add.accept(new ItemStack(ModItems.CAN_BREEN.get()));
        add.accept(new ItemStack(ModItems.CAN_CREATURE.get()));
        add.accept(new ItemStack(ModItems.CAN_LUNA.get()));
        add.accept(new ItemStack(ModItems.CAN_MRSUGAR.get()));
        add.accept(new ItemStack(ModItems.CAN_MUG.get()));
        add.accept(new ItemStack(ModItems.CAN_OVERCHARGE.get()));
        add.accept(new ItemStack(ModItems.CAN_REDBOMB.get()));
        add.accept(new ItemStack(ModItems.CAN_SMART.get()));

        add.accept(new ItemStack(ModItems.DEFUSER.get()));
        add.accept(new ItemStack(ModItems.CROWBAR.get()));
        add.accept(new ItemStack(ModItems.SCREWDRIVER.get()));

        add.accept(new ItemStack(ModItems.DOSIMETER.get()));
        add.accept(new ItemStack(ModItems.GEIGER_COUNTER.get()));
        add.accept(new ItemStack(ModBlocks.GEIGER_COUNTER_BLOCK.get()));

        add.accept(new ItemStack(ModItems.OIL_DETECTOR.get()));
        add.accept(new ItemStack(ModItems.DEPTH_ORES_SCANNER.get()));

        add.accept(new ItemStack(ModItems.AIRSTRIKE_TEST.get()));
        add.accept(new ItemStack(ModItems.AIRSTRIKE_HEAVY.get()));
        add.accept(new ItemStack(ModItems.AIRSTRIKE_AGENT.get()));
        add.accept(new ItemStack(ModItems.AIRSTRIKE_NUKE.get()));
    }

    // ЗАПЧАСТИ
    public static void populateSparepartsTab(BiConsumer<ItemStack, CreativeModeTab.TabVisibility> acceptor) {
        // Упрощенный Consumer, по умолчанию использующий PARENT_AND_SEARCH_TABS
        Consumer<ItemStack> add = stack -> acceptor.accept(stack, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
        add.accept(new ItemStack(ModItems.BOLT_STEEL.get()));
        add.accept(new ItemStack(ModItems.COIL_TUNGSTEN.get()));

        add.accept(new ItemStack(ModItems.PLATE_IRON.get()));
        add.accept(new ItemStack(ModItems.PLATE_ALUMINUM.get()));
        add.accept(new ItemStack(ModItems.PLATE_TITANIUM.get()));
        add.accept(new ItemStack(ModItems.PLATE_LEAD.get()));
        add.accept(new ItemStack(ModItems.PLATE_COPPER.get()));
        add.accept(new ItemStack(ModItems.PLATE_STEEL.get()));
        add.accept(new ItemStack(ModItems.PLATE_GOLD.get()));
        add.accept(new ItemStack(ModItems.PLATE_ADVANCED_ALLOY.get()));
        add.accept(new ItemStack(ModItems.PLATE_GUNMETAL.get()));
        add.accept(new ItemStack(ModItems.PLATE_GUNSTEEL.get()));
        add.accept(new ItemStack(ModItems.PLATE_DURA_STEEL.get()));
        add.accept(new ItemStack(ModItems.PLATE_KEVLAR.get()));
        add.accept(new ItemStack(ModItems.PLATE_PAA.get()));
        add.accept(new ItemStack(ModItems.PLATE_SCHRABIDIUM.get()));
        add.accept(new ItemStack(ModItems.PLATE_SATURNITE.get()));
        add.accept(new ItemStack(ModItems.PLATE_COMBINE_STEEL.get()));
        add.accept(new ItemStack(ModItems.PLATE_FUEL_MOX.get()));
        add.accept(new ItemStack(ModItems.PLATE_FUEL_PU238BE.get()));
        add.accept(new ItemStack(ModItems.PLATE_FUEL_PU239.get()));
        add.accept(new ItemStack(ModItems.PLATE_FUEL_RA226BE.get()));
        add.accept(new ItemStack(ModItems.PLATE_FUEL_SA326.get()));
        add.accept(new ItemStack(ModItems.PLATE_FUEL_U233.get()));
        add.accept(new ItemStack(ModItems.PLATE_FUEL_U235.get()));

        add.accept(new ItemStack(ModItems.WIRE_FINE.get()));
        add.accept(new ItemStack(ModItems.WIRE_ALUMINIUM.get()));
        add.accept(new ItemStack(ModItems.WIRE_CARBON.get()));
        add.accept(new ItemStack(ModItems.WIRE_TUNGSTEN.get()));
        add.accept(new ItemStack(ModItems.WIRE_GOLD.get()));
        add.accept(new ItemStack(ModItems.WIRE_COPPER.get()));
        add.accept(new ItemStack(ModItems.WIRE_RED_COPPER.get()));
        add.accept(new ItemStack(ModItems.WIRE_ADVANCED_ALLOY.get()));
        add.accept(new ItemStack(ModItems.WIRE_MAGNETIZED_TUNGSTEN.get()));
        add.accept(new ItemStack(ModItems.WIRE_SCHRABIDIUM.get()));

        add.accept(new ItemStack(ModItems.COIL_COPPER.get()));
        add.accept(new ItemStack(ModItems.COIL_ADVANCED_ALLOY.get()));
        add.accept(new ItemStack(ModItems.COIL_GOLD.get()));
        add.accept(new ItemStack(ModItems.COIL_MAGNETIZED_TUNGSTEN.get()));
        add.accept(new ItemStack(ModItems.COIL_COPPER_TORUS.get()));
        add.accept(new ItemStack(ModItems.COIL_ADVANCED_ALLOY_TORUS.get()));
        add.accept(new ItemStack(ModItems.COIL_GOLD_TORUS.get()));
        add.accept(new ItemStack(ModItems.COIL_MAGNETIZED_TUNGSTEN_TORUS.get()));

        // Mineral Pipes
        add.accept(new ItemStack(ModItems.PIPE_IRON.get()));
        add.accept(new ItemStack(ModItems.PIPE_COPPER.get()));
        add.accept(new ItemStack(ModItems.PIPE_GOLD.get()));
        add.accept(new ItemStack(ModItems.PIPE_LEAD.get()));
        add.accept(new ItemStack(ModItems.PIPE_STEEL.get()));
        add.accept(new ItemStack(ModItems.PIPE_TUNGSTEN.get()));
        add.accept(new ItemStack(ModItems.PIPE_TITANIUM.get()));
        add.accept(new ItemStack(ModItems.PIPE_ALUMINUM.get()));

        add.accept(new ItemStack(ModItems.PLATE_ARMOR_TITANIUM.get()));
        add.accept(new ItemStack(ModItems.PLATE_ARMOR_AJR.get()));
        add.accept(new ItemStack(ModItems.PLATE_ARMOR_LUNAR.get()));
        add.accept(new ItemStack(ModItems.PLATE_ARMOR_HEV.get()));
        add.accept(new ItemStack(ModItems.PLATE_ARMOR_DNT.get()));
        add.accept(new ItemStack(ModItems.PLATE_ARMOR_DNT_RUSTED.get()));
        add.accept(new ItemStack(ModItems.PLATE_ARMOR_FAU.get()));

        add.accept(new ItemStack(ModItems.PLATE_MIXED.get()));
        add.accept(new ItemStack(ModItems.PLATE_DALEKANIUM.get()));
        add.accept(new ItemStack(ModItems.PLATE_DESH.get()));
        add.accept(new ItemStack(ModItems.PLATE_BISMUTH.get()));
        add.accept(new ItemStack(ModItems.PLATE_EUPHEMIUM.get()));
        add.accept(new ItemStack(ModItems.PLATE_DINEUTRONIUM.get()));

        add.accept(new ItemStack(ModItems.PLATE_CAST.get()));
        add.accept(new ItemStack(ModItems.PLATE_CAST_ALT.get()));
        add.accept(new ItemStack(ModItems.PLATE_CAST_BISMUTH.get()));
        add.accept(new ItemStack(ModItems.PLATE_CAST_DARK.get()));

        add.accept(new ItemStack(ModItems.MOTOR.get()));
        add.accept(new ItemStack(ModItems.MOTOR_DESH.get()));
        add.accept(new ItemStack(ModItems.MOTOR_BISMUTH.get()));

        add.accept(new ItemStack(ModItems.INSULATOR.get()));
        add.accept(new ItemStack(ModItems.SILICON_CIRCUIT.get()));
        add.accept(new ItemStack(ModItems.PCB.get()));
        add.accept(new ItemStack(ModItems.CRT_DISPLAY.get()));
        add.accept(new ItemStack(ModItems.VACUUM_TUBE.get()));
        add.accept(new ItemStack(ModItems.CAPACITOR.get()));
        add.accept(new ItemStack(ModItems.MICROCHIP.get()));
        add.accept(new ItemStack(ModItems.ANALOG_CIRCUIT.get()));
        add.accept(new ItemStack(ModItems.INTEGRATED_CIRCUIT.get()));
        add.accept(new ItemStack(ModItems.ADVANCED_CIRCUIT.get()));
        add.accept(new ItemStack(ModItems.CAPACITOR_BOARD.get()));

        add.accept(new ItemStack(ModItems.CONTROLLER_CHASSIS.get()));
        add.accept(new ItemStack(ModItems.CONTROLLER.get()));
        add.accept(new ItemStack(ModItems.CONTROLLER_ADVANCED.get()));
        add.accept(new ItemStack(ModItems.CAPACITOR_TANTALUM.get()));
        add.accept(new ItemStack(ModItems.BISMOID_CHIP.get()));
        add.accept(new ItemStack(ModItems.BISMOID_CIRCUIT.get()));
        add.accept(new ItemStack(ModItems.ATOMIC_CLOCK.get()));
        add.accept(new ItemStack(ModItems.QUANTUM_CHIP.get()));
        add.accept(new ItemStack(ModItems.QUANTUM_CIRCUIT.get()));
        add.accept(new ItemStack(ModItems.QUANTUM_COMPUTER.get()));

        add.accept(new ItemStack(ModItems.BATTLE_GEARS.get()));
        add.accept(new ItemStack(ModItems.BATTLE_SENSOR.get()));
        add.accept(new ItemStack(ModItems.BATTLE_CASING.get()));
        add.accept(new ItemStack(ModItems.BATTLE_COUNTER.get()));
        add.accept(new ItemStack(ModItems.BATTLE_MODULE.get()));
        add.accept(new ItemStack(ModItems.METAL_ROD.get()));
    }
    // РУДЫ
    public static void populateOresTab(BiConsumer<ItemStack, CreativeModeTab.TabVisibility> acceptor) {
        // Упрощенный Consumer, по умолчанию использующий PARENT_AND_SEARCH_TABS
        Consumer<ItemStack> add = stack -> acceptor.accept(stack, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);

        add.accept(new ItemStack(ModBlocks.DEPTH_STONE.get()));
        add.accept(new ItemStack(ModBlocks.DEPTH_STONE_NETHER.get()));

        add.accept(new ItemStack(ModBlocks.DEPTH_BORAX.get()));
        add.accept(new ItemStack(ModBlocks.DEPTH_IRON.get()));
        add.accept(new ItemStack(ModBlocks.DEPTH_TITANIUM.get()));
        add.accept(new ItemStack(ModBlocks.DEPTH_TUNGSTEN.get()));
        add.accept(new ItemStack(ModBlocks.DEPTH_CINNABAR.get()));
        add.accept(new ItemStack(ModBlocks.DEPTH_ZIRCONIUM.get()));
        add.accept(new ItemStack(ModBlocks.BEDROCK_OIL.get()));

        add.accept(new ItemStack(ModBlocks.ORE_OIL.get()));
        add.accept(new ItemStack(ModBlocks.GNEISS_STONE.get()));
        add.accept(new ItemStack(ModBlocks.FLUORITE_ORE.get()));
        add.accept(new ItemStack(ModBlocks.LIGNITE_ORE.get()));
        add.accept(new ItemStack(ModBlocks.TUNGSTEN_ORE.get()));
        add.accept(new ItemStack(ModBlocks.ASBESTOS_ORE.get()));
        add.accept(new ItemStack(ModBlocks.SULFUR_ORE.get()));
        add.accept(new ItemStack(ModBlocks.SEQUESTRUM_ORE.get()));

        add.accept(new ItemStack(ModBlocks.ALUMINUM_ORE.get()));
        add.accept(new ItemStack(ModBlocks.ALUMINUM_ORE_DEEPSLATE.get()));
        add.accept(new ItemStack(ModBlocks.TITANIUM_ORE.get()));
        add.accept(new ItemStack(ModBlocks.TITANIUM_ORE_DEEPSLATE.get()));
        add.accept(new ItemStack(ModBlocks.COBALT_ORE.get()));
        add.accept(new ItemStack(ModBlocks.COBALT_ORE_DEEPSLATE.get()));
        add.accept(new ItemStack(ModBlocks.THORIUM_ORE.get()));
        add.accept(new ItemStack(ModBlocks.THORIUM_ORE_DEEPSLATE.get()));
        add.accept(new ItemStack(ModBlocks.RAREGROUND_ORE.get()));
        add.accept(new ItemStack(ModBlocks.RAREGROUND_ORE_DEEPSLATE.get()));
        add.accept(new ItemStack(ModBlocks.BERYLLIUM_ORE.get()));
        add.accept(new ItemStack(ModBlocks.BERYLLIUM_ORE_DEEPSLATE.get()));
        add.accept(new ItemStack(ModBlocks.LEAD_ORE.get()));
        add.accept(new ItemStack(ModBlocks.LEAD_ORE_DEEPSLATE.get()));
        add.accept(new ItemStack(ModBlocks.CINNABAR_ORE.get()));
        add.accept(new ItemStack(ModBlocks.CINNABAR_ORE_DEEPSLATE.get()));
        add.accept(new ItemStack(ModBlocks.URANIUM_ORE.get()));
        add.accept(new ItemStack(ModBlocks.URANIUM_ORE_DEEPSLATE.get()));

        add.accept(new ItemStack(ModBlocks.RESOURCE_ASBESTOS.get()));
        add.accept(new ItemStack(ModBlocks.RESOURCE_BAUXITE.get()));
        add.accept(new ItemStack(ModBlocks.RESOURCE_HEMATITE.get()));
        add.accept(new ItemStack(ModBlocks.RESOURCE_LIMESTONE.get()));
        add.accept(new ItemStack(ModBlocks.RESOURCE_MALACHITE.get()));
        add.accept(new ItemStack(ModBlocks.RESOURCE_SULFUR.get()));

        add.accept(new ItemStack(ModItems.ALUMINUM_RAW.get()));
        add.accept(new ItemStack(ModItems.BERYLLIUM_RAW.get()));
        add.accept(new ItemStack(ModItems.COBALT_RAW.get()));
        add.accept(new ItemStack(ModItems.LEAD_RAW.get()));
        add.accept(new ItemStack(ModItems.THORIUM_RAW.get()));
        add.accept(new ItemStack(ModItems.TITANIUM_RAW.get()));
        add.accept(new ItemStack(ModItems.TUNGSTEN_RAW.get()));
        add.accept(new ItemStack(ModItems.URANIUM_RAW.get()));

        add.accept(new ItemStack(ModBlocks.METEOR.get()));
        add.accept(new ItemStack(ModBlocks.METEOR_COBBLE.get()));
        add.accept(new ItemStack(ModBlocks.METEOR_CRUSHED.get()));
        add.accept(new ItemStack(ModBlocks.METEOR_TREASURE.get()));

        add.accept(new ItemStack(ModBlocks.GEYSIR_DIRT.get()));
        add.accept(new ItemStack(ModBlocks.GEYSIR_STONE.get()));

        add.accept(new ItemStack(ModBlocks.NUCLEAR_FALLOUT.get()));
        add.accept(new ItemStack(ModBlocks.SELLAFIELD_SLAKED.get()));
        add.accept(new ItemStack(ModBlocks.SELLAFIELD_SLAKED1.get()));
        add.accept(new ItemStack(ModBlocks.SELLAFIELD_SLAKED2.get()));
        add.accept(new ItemStack(ModBlocks.SELLAFIELD_SLAKED3.get()));
        add.accept(new ItemStack(ModBlocks.WASTE_LOG.get()));
        add.accept(new ItemStack(ModBlocks.WASTE_PLANKS.get()));
        add.accept(new ItemStack(ModBlocks.WASTE_GRASS.get()));
        add.accept(new ItemStack(ModBlocks.BURNED_GRASS.get()));
        add.accept(new ItemStack(ModBlocks.DEAD_DIRT.get()));
        add.accept(new ItemStack(ModBlocks.WASTE_LEAVES.get()));

        add.accept(new ItemStack(ModItems.STRAWBERRY.get()));
        add.accept(new ItemStack(ModBlocks.STRAWBERRY_BUSH.get()));

        add.accept(new ItemStack(ModBlocks.POLONIUM210_BLOCK.get()));

        // АВТОМАТИЧЕСКОЕ ДОБАВЛЕНИЕ ВСЕХ БЛОКОВ СЛИТКОВ
        for (ModIngots ingot : ModIngots.values()) {

            if (ModBlocks.hasIngotBlock(ingot)) {

                RegistrySupplier<Block> ingotBlock = ModBlocks.getIngotBlock(ingot);
                if (ingotBlock != null) {
                    add.accept(new ItemStack(ingotBlock.get()));
                    if (ModClothConfig.get().enableDebugLogging) {
                        MainRegistry.LOGGER.info("Added {} block to NTM Ores tab", ingotBlock.getId());
                    }
                }
            }
        }
        // add.accept(new ItemStack(ModBlocks.URANIUM_BLOCK.get()));
        // add.accept(new ItemStack(ModBlocks.PLUTONIUM_BLOCK.get()));
        // add.accept(new ItemStack(ModBlocks.PLUTONIUM_FUEL_BLOCK.get()));
    }


    // СТРОИТЕЛЬНЫЕ БЛОКИ
    public static void populateBuildingTab(BiConsumer<ItemStack, CreativeModeTab.TabVisibility> acceptor) {
        // Упрощенный Consumer, по умолчанию использующий PARENT_AND_SEARCH_TABS
        Consumer<ItemStack> add = stack -> acceptor.accept(stack, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);

        add.accept(new ItemStack(ModBlocks.DECO_STEEL.get()));
        add.accept(new ItemStack(ModBlocks.CONCRETE.get()));
        add.accept(new ItemStack(ModBlocks.CONCRETE_ASBESTOS.get()));
        add.accept(new ItemStack(ModBlocks.CONCRETE_COLORED_SAND.get()));
        add.accept(new ItemStack(ModBlocks.CONCRETE_BLACK.get()));
        add.accept(new ItemStack(ModBlocks.CONCRETE_BLUE.get()));
        add.accept(new ItemStack(ModBlocks.CONCRETE_BROWN.get()));
        add.accept(new ItemStack(ModBlocks.CONCRETE_COLORED_INDIGO.get()));
        add.accept(new ItemStack(ModBlocks.CONCRETE_COLORED_PINK.get()));
        add.accept(new ItemStack(ModBlocks.CONCRETE_COLORED_PURPLE.get()));
        add.accept(new ItemStack(ModBlocks.CONCRETE_CYAN.get()));
        add.accept(new ItemStack(ModBlocks.CONCRETE_GRAY.get()));
        add.accept(new ItemStack(ModBlocks.CONCRETE_GREEN.get()));
        add.accept(new ItemStack(ModBlocks.CONCRETE_LIGHT_BLUE.get()));
        add.accept(new ItemStack(ModBlocks.CONCRETE_LIME.get()));
        add.accept(new ItemStack(ModBlocks.CONCRETE_MAGENTA.get()));
        add.accept(new ItemStack(ModBlocks.CONCRETE_ORANGE.get()));
        add.accept(new ItemStack(ModBlocks.CONCRETE_PINK.get()));
        add.accept(new ItemStack(ModBlocks.CONCRETE_PURPLE.get()));
        add.accept(new ItemStack(ModBlocks.CONCRETE_RED.get()));
        add.accept(new ItemStack(ModBlocks.CONCRETE_YELLOW.get()));
        add.accept(new ItemStack(ModBlocks.CONCRETE_HAZARD.get()));
        add.accept(new ItemStack(ModBlocks.CONCRETE_SILVER.get()));
        add.accept(new ItemStack(ModBlocks.CONCRETE_WHITE.get()));

        add.accept(new ItemStack(ModBlocks.CONCRETE_SUPER.get()));
        add.accept(new ItemStack(ModBlocks.CONCRETE_SUPER_M0.get()));
        add.accept(new ItemStack(ModBlocks.CONCRETE_SUPER_M1.get()));
        add.accept(new ItemStack(ModBlocks.CONCRETE_SUPER_M2.get()));
        add.accept(new ItemStack(ModBlocks.CONCRETE_SUPER_M3.get()));
        add.accept(new ItemStack(ModBlocks.CONCRETE_SUPER_BROKEN.get()));

        add.accept(new ItemStack(ModBlocks.CONCRETE_REBAR.get()));
        add.accept(new ItemStack(ModBlocks.CONCRETE_REBAR_ALT.get()));
        add.accept(new ItemStack(ModBlocks.CONCRETE_FLAT.get()));
        add.accept(new ItemStack(ModBlocks.CONCRETE_TILE.get()));
        add.accept(new ItemStack(ModBlocks.CONCRETE_VENT.get()));
        add.accept(new ItemStack(ModBlocks.CONCRETE_FAN.get()));
        add.accept(new ItemStack(ModBlocks.CONCRETE_TILE_TREFOIL.get()));

        add.accept(new ItemStack(ModBlocks.CONCRETE_MOSSY.get()));
        add.accept(new ItemStack(ModBlocks.CONCRETE_CRACKED.get()));
        add.accept(new ItemStack(ModBlocks.CONCRETE_MARKED.get()));
        add.accept(new ItemStack(ModBlocks.BRICK_CONCRETE.get()));
        add.accept(new ItemStack(ModBlocks.BRICK_CONCRETE_MOSSY.get()));
        add.accept(new ItemStack(ModBlocks.BRICK_CONCRETE_CRACKED.get()));
        add.accept(new ItemStack(ModBlocks.BRICK_CONCRETE_BROKEN.get()));
        add.accept(new ItemStack(ModBlocks.BRICK_CONCRETE_MARKED.get()));
        add.accept(new ItemStack(ModBlocks.CONCRETE_PILLAR.get()));
        add.accept(new ItemStack(ModBlocks.CONCRETE_COLORED_MACHINE.get()));
        add.accept(new ItemStack(ModBlocks.CONCRETE_COLORED_MACHINE_STRIPE.get()));
        add.accept(new ItemStack(ModBlocks.CONCRETE_COLORED_BRONZE.get()));


        // Метеоритные блоки
        add.accept(new ItemStack(ModBlocks.METEOR_POLISHED.get()));
        add.accept(new ItemStack(ModBlocks.METEOR_BRICK.get()));
        add.accept(new ItemStack(ModBlocks.METEOR_BRICK_CRACKED.get()));
        add.accept(new ItemStack(ModBlocks.METEOR_BRICK_MOSSY.get()));
        add.accept(new ItemStack(ModBlocks.METEOR_BRICK_CHISELED.get()));
        add.accept(new ItemStack(ModBlocks.METEOR_PILLAR.get()));

        add.accept(new ItemStack(ModBlocks.DEPTH_BRICK.get()));
        add.accept(new ItemStack(ModBlocks.DEPTH_TILES.get()));
        add.accept(new ItemStack(ModBlocks.DEPTH_NETHER_BRICK.get()));
        add.accept(new ItemStack(ModBlocks.DEPTH_NETHER_TILES.get()));
        add.accept(new ItemStack(ModBlocks.GNEISS_TILE.get()));
        add.accept(new ItemStack(ModBlocks.GNEISS_BRICK.get()));
        add.accept(new ItemStack(ModBlocks.GNEISS_CHISELED.get()));

        add.accept(new ItemStack(ModBlocks.BRICK_BASE.get()));
        add.accept(new ItemStack(ModBlocks.BRICK_LIGHT.get()));
        add.accept(new ItemStack(ModBlocks.BARRICADE.get()));
        add.accept(new ItemStack(ModBlocks.BRICK_FIRE.get()));
        add.accept(new ItemStack(ModBlocks.BRICK_OBSIDIAN.get()));

        add.accept(new ItemStack(ModBlocks.VINYL_TILE.get()));
        add.accept(new ItemStack(ModBlocks.VINYL_TILE_SMALL.get()));
        add.accept(new ItemStack(ModBlocks.REINFORCED_STONE.get()));
        add.accept(new ItemStack(ModBlocks.BRICK_DUCRETE.get()));
        add.accept(new ItemStack(ModBlocks.ASPHALT.get()));
        add.accept(new ItemStack(ModBlocks.BASALT_POLISHED.get()));
        add.accept(new ItemStack(ModBlocks.BASALT_BRICK.get()));

        //ПОЛУБЛОКИ
        add.accept(new ItemStack(ModBlocks.CONCRETE_HAZARD_SLAB.get()));
        add.accept(new ItemStack(ModBlocks.CONCRETE_ASBESTOS_SLAB.get()));
        add.accept(new ItemStack(ModBlocks.CONCRETE_BLACK_SLAB.get()));
        add.accept(new ItemStack(ModBlocks.CONCRETE_BLUE_SLAB.get()));
        add.accept(new ItemStack(ModBlocks.CONCRETE_BROWN_SLAB.get()));
        add.accept(new ItemStack(ModBlocks.DEPTH_STONE_SLAB.get()));
        add.accept(new ItemStack(ModBlocks.CONCRETE_COLORED_BRONZE_SLAB.get()));
        add.accept(new ItemStack(ModBlocks.CONCRETE_COLORED_INDIGO_SLAB.get()));
        add.accept(new ItemStack(ModBlocks.CONCRETE_COLORED_MACHINE_SLAB.get()));
        add.accept(new ItemStack(ModBlocks.CONCRETE_COLORED_PINK_SLAB.get()));
        add.accept(new ItemStack(ModBlocks.CONCRETE_COLORED_PURPLE_SLAB.get()));
        add.accept(new ItemStack(ModBlocks.CONCRETE_COLORED_SAND_SLAB.get()));
        add.accept(new ItemStack(ModBlocks.CONCRETE_CYAN_SLAB.get()));
        add.accept(new ItemStack(ModBlocks.CONCRETE_GRAY_SLAB.get()));
        add.accept(new ItemStack(ModBlocks.CONCRETE_GREEN_SLAB.get()));
        add.accept(new ItemStack(ModBlocks.CONCRETE_LIGHT_BLUE_SLAB.get()));
        add.accept(new ItemStack(ModBlocks.CONCRETE_LIME_SLAB.get()));
        add.accept(new ItemStack(ModBlocks.CONCRETE_MAGENTA_SLAB.get()));
        add.accept(new ItemStack(ModBlocks.CONCRETE_ORANGE_SLAB.get()));
        add.accept(new ItemStack(ModBlocks.CONCRETE_PINK_SLAB.get()));
        add.accept(new ItemStack(ModBlocks.CONCRETE_PURPLE_SLAB.get()));
        add.accept(new ItemStack(ModBlocks.CONCRETE_RED_SLAB.get()));
        add.accept(new ItemStack(ModBlocks.CONCRETE_SILVER_SLAB.get()));
        add.accept(new ItemStack(ModBlocks.CONCRETE_WHITE_SLAB.get()));
        add.accept(new ItemStack(ModBlocks.CONCRETE_YELLOW_SLAB.get()));
        add.accept(new ItemStack(ModBlocks.CONCRETE_SUPER_SLAB.get()));
        add.accept(new ItemStack(ModBlocks.CONCRETE_SUPER_M0_SLAB.get()));
        add.accept(new ItemStack(ModBlocks.CONCRETE_SUPER_M1_SLAB.get()));
        add.accept(new ItemStack(ModBlocks.CONCRETE_SUPER_M2_SLAB.get()));
        add.accept(new ItemStack(ModBlocks.CONCRETE_SUPER_M3_SLAB.get()));
        add.accept(new ItemStack(ModBlocks.CONCRETE_SUPER_BROKEN_SLAB.get()));
        add.accept(new ItemStack(ModBlocks.CONCRETE_REBAR_SLAB.get()));
        add.accept(new ItemStack(ModBlocks.CONCRETE_FLAT_SLAB.get()));
        add.accept(new ItemStack(ModBlocks.CONCRETE_TILE_SLAB.get()));
        add.accept(new ItemStack(ModBlocks.DEPTH_BRICK_SLAB.get()));
        add.accept(new ItemStack(ModBlocks.DEPTH_TILES_SLAB.get()));
        add.accept(new ItemStack(ModBlocks.DEPTH_STONE_NETHER_SLAB.get()));
        add.accept(new ItemStack(ModBlocks.DEPTH_NETHER_BRICK_SLAB.get()));
        add.accept(new ItemStack(ModBlocks.DEPTH_NETHER_TILES_SLAB.get()));
        add.accept(new ItemStack(ModBlocks.GNEISS_TILE_SLAB.get()));
        add.accept(new ItemStack(ModBlocks.GNEISS_BRICK_SLAB.get()));
        add.accept(new ItemStack(ModBlocks.BRICK_BASE_SLAB.get()));
        add.accept(new ItemStack(ModBlocks.BRICK_LIGHT_SLAB.get()));
        add.accept(new ItemStack(ModBlocks.BRICK_FIRE_SLAB.get()));
        add.accept(new ItemStack(ModBlocks.BRICK_OBSIDIAN_SLAB.get()));
        add.accept(new ItemStack(ModBlocks.VINYL_TILE_SLAB.get()));
        add.accept(new ItemStack(ModBlocks.VINYL_TILE_SMALL_SLAB.get()));
        add.accept(new ItemStack(ModBlocks.BRICK_DUCRETE_SLAB.get()));
        add.accept(new ItemStack(ModBlocks.ASPHALT_SLAB.get()));
        add.accept(new ItemStack(ModBlocks.BASALT_POLISHED_SLAB.get()));
        add.accept(new ItemStack(ModBlocks.BASALT_BRICK_SLAB.get()));
        add.accept(new ItemStack(ModBlocks.METEOR_POLISHED_SLAB.get()));
        add.accept(new ItemStack(ModBlocks.METEOR_BRICK_SLAB.get()));
        add.accept(new ItemStack(ModBlocks.METEOR_BRICK_CRACKED_SLAB.get()));
        add.accept(new ItemStack(ModBlocks.METEOR_BRICK_MOSSY_SLAB.get()));
        add.accept(new ItemStack(ModBlocks.METEOR_CRUSHED_SLAB.get()));
        add.accept(new ItemStack(ModBlocks.REINFORCED_STONE_SLAB.get()));
        add.accept(new ItemStack(ModBlocks.BRICK_CONCRETE_SLAB.get()));
        add.accept(new ItemStack(ModBlocks.BRICK_CONCRETE_CRACKED_SLAB.get()));
        add.accept(new ItemStack(ModBlocks.BRICK_CONCRETE_BROKEN_SLAB.get()));
        add.accept(new ItemStack(ModBlocks.BRICK_CONCRETE_MOSSY_SLAB.get()));
        add.accept(new ItemStack(ModBlocks.CONCRETE_SLAB.get()));
        add.accept(new ItemStack(ModBlocks.CONCRETE_MOSSY_SLAB.get()));
        add.accept(new ItemStack(ModBlocks.CONCRETE_CRACKED_SLAB.get()));

        //СТУПЕНИ
        add.accept(new ItemStack(ModBlocks.CONCRETE_STAIRS.get()));
        add.accept(new ItemStack(ModBlocks.CONCRETE_ASBESTOS_STAIRS.get()));
        add.accept(new ItemStack(ModBlocks.CONCRETE_MOSSY_STAIRS.get()));
        add.accept(new ItemStack(ModBlocks.CONCRETE_CRACKED_STAIRS.get()));
        add.accept(new ItemStack(ModBlocks.CONCRETE_HAZARD_STAIRS.get()));
        add.accept(new ItemStack(ModBlocks.BRICK_CONCRETE_STAIRS.get()));
        add.accept(new ItemStack(ModBlocks.BRICK_CONCRETE_MOSSY_STAIRS.get()));
        add.accept(new ItemStack(ModBlocks.BRICK_CONCRETE_CRACKED_STAIRS.get()));
        add.accept(new ItemStack(ModBlocks.BRICK_CONCRETE_BROKEN_STAIRS.get()));
        add.accept(new ItemStack(ModBlocks.CONCRETE_BLACK_STAIRS.get()));
        add.accept(new ItemStack(ModBlocks.CONCRETE_BLUE_STAIRS.get()));
        add.accept(new ItemStack(ModBlocks.CONCRETE_BROWN_STAIRS.get()));
        add.accept(new ItemStack(ModBlocks.CONCRETE_COLORED_BRONZE_STAIRS.get()));
        add.accept(new ItemStack(ModBlocks.CONCRETE_COLORED_INDIGO_STAIRS.get()));
        add.accept(new ItemStack(ModBlocks.CONCRETE_COLORED_MACHINE_STAIRS.get()));
        add.accept(new ItemStack(ModBlocks.CONCRETE_COLORED_PINK_STAIRS.get()));
        add.accept(new ItemStack(ModBlocks.CONCRETE_COLORED_PURPLE_STAIRS.get()));
        add.accept(new ItemStack(ModBlocks.CONCRETE_COLORED_SAND_STAIRS.get()));
        add.accept(new ItemStack(ModBlocks.CONCRETE_CYAN_STAIRS.get()));
        add.accept(new ItemStack(ModBlocks.CONCRETE_GRAY_STAIRS.get()));
        add.accept(new ItemStack(ModBlocks.CONCRETE_GREEN_STAIRS.get()));
        add.accept(new ItemStack(ModBlocks.CONCRETE_LIGHT_BLUE_STAIRS.get()));
        add.accept(new ItemStack(ModBlocks.CONCRETE_LIME_STAIRS.get()));
        add.accept(new ItemStack(ModBlocks.CONCRETE_MAGENTA_STAIRS.get()));
        add.accept(new ItemStack(ModBlocks.CONCRETE_ORANGE_STAIRS.get()));
        add.accept(new ItemStack(ModBlocks.CONCRETE_PINK_STAIRS.get()));
        add.accept(new ItemStack(ModBlocks.CONCRETE_PURPLE_STAIRS.get()));
        add.accept(new ItemStack(ModBlocks.CONCRETE_RED_STAIRS.get()));
        add.accept(new ItemStack(ModBlocks.CONCRETE_SILVER_STAIRS.get()));
        add.accept(new ItemStack(ModBlocks.CONCRETE_WHITE_STAIRS.get()));
        add.accept(new ItemStack(ModBlocks.CONCRETE_YELLOW_STAIRS.get()));
        add.accept(new ItemStack(ModBlocks.CONCRETE_SUPER_STAIRS.get()));
        add.accept(new ItemStack(ModBlocks.CONCRETE_SUPER_M0_STAIRS.get()));
        add.accept(new ItemStack(ModBlocks.CONCRETE_SUPER_M1_STAIRS.get()));
        add.accept(new ItemStack(ModBlocks.CONCRETE_SUPER_M2_STAIRS.get()));
        add.accept(new ItemStack(ModBlocks.CONCRETE_SUPER_M3_STAIRS.get()));
        add.accept(new ItemStack(ModBlocks.CONCRETE_SUPER_BROKEN_STAIRS.get()));
        add.accept(new ItemStack(ModBlocks.CONCRETE_REBAR_STAIRS.get()));
        add.accept(new ItemStack(ModBlocks.CONCRETE_FLAT_STAIRS.get()));
        add.accept(new ItemStack(ModBlocks.CONCRETE_TILE_STAIRS.get()));
        add.accept(new ItemStack(ModBlocks.DEPTH_BRICK_STAIRS.get()));
        add.accept(new ItemStack(ModBlocks.DEPTH_STONE_STAIRS.get()));
        add.accept(new ItemStack(ModBlocks.DEPTH_TILES_STAIRS.get()));
        add.accept(new ItemStack(ModBlocks.DEPTH_NETHER_BRICK_STAIRS.get()));
        add.accept(new ItemStack(ModBlocks.DEPTH_NETHER_TILES_STAIRS.get()));
        add.accept(new ItemStack(ModBlocks.GNEISS_TILE_STAIRS.get()));
        add.accept(new ItemStack(ModBlocks.GNEISS_BRICK_STAIRS.get()));
        add.accept(new ItemStack(ModBlocks.BRICK_BASE_STAIRS.get()));
        add.accept(new ItemStack(ModBlocks.BRICK_LIGHT_STAIRS.get()));
        add.accept(new ItemStack(ModBlocks.BRICK_FIRE_STAIRS.get()));
        add.accept(new ItemStack(ModBlocks.BRICK_OBSIDIAN_STAIRS.get()));
        add.accept(new ItemStack(ModBlocks.VINYL_TILE_STAIRS.get()));
        add.accept(new ItemStack(ModBlocks.VINYL_TILE_SMALL_STAIRS.get()));
        add.accept(new ItemStack(ModBlocks.BRICK_DUCRETE_STAIRS.get()));
        add.accept(new ItemStack(ModBlocks.ASPHALT_STAIRS.get()));
        add.accept(new ItemStack(ModBlocks.BASALT_POLISHED_STAIRS.get()));
        add.accept(new ItemStack(ModBlocks.BASALT_BRICK_STAIRS.get()));
        add.accept(new ItemStack(ModBlocks.METEOR_POLISHED_STAIRS.get()));
        add.accept(new ItemStack(ModBlocks.METEOR_BRICK_STAIRS.get()));
        add.accept(new ItemStack(ModBlocks.METEOR_BRICK_CRACKED_STAIRS.get()));
        add.accept(new ItemStack(ModBlocks.METEOR_BRICK_MOSSY_STAIRS.get()));
        add.accept(new ItemStack(ModBlocks.METEOR_CRUSHED_STAIRS.get()));


        add.accept(new ItemStack(ModBlocks.REINFORCED_STONE_STAIRS.get()));

        //СТЕКЛО
        add.accept(new ItemStack(ModBlocks.REINFORCED_GLASS.get()));

        //ЯЩИКИ
        add.accept(new ItemStack(ModBlocks.FREAKY_ALIEN_BLOCK.get()));
        add.accept(new ItemStack(ModBlocks.CRATE.get()));
        add.accept(new ItemStack(ModBlocks.CRATE_LEAD.get()));
        add.accept(new ItemStack(ModBlocks.CRATE_METAL.get()));
        add.accept(new ItemStack(ModBlocks.CRATE_WEAPON.get()));
        add.accept(new ItemStack(ModBlocks.CRATE_CONSERVE.get()));

        //ОСВЕЩЕНИЕ
        add.accept(new ItemStack(ModBlocks.CAGE_LAMP.get()));
        add.accept(new ItemStack(ModBlocks.FLOOD_LAMP.get()));

        //OBJ-ДЕКОР
        add.accept(new ItemStack(ModBlocks.B29.get()));
        add.accept(new ItemStack(ModBlocks.DORNIER.get()));
        add.accept(new ItemStack(ModBlocks.FILE_CABINET.get()));
        add.accept(new ItemStack(ModBlocks.TAPE_RECORDER.get()));
        add.accept(new ItemStack(ModBlocks.CRT_BROKEN.get()));
        add.accept(new ItemStack(ModBlocks.CRT_CLEAN.get()));
        add.accept(new ItemStack(ModBlocks.CRT_BSOD.get()));
        add.accept(new ItemStack(ModBlocks.TOASTER.get()));

        add.accept(new ItemStack(ModBlocks.DOOR_OFFICE.get()));
        add.accept(new ItemStack(ModBlocks.DOOR_BUNKER.get()));
        add.accept(new ItemStack(ModBlocks.METAL_DOOR.get()));
        add.accept(new ItemStack(ModBlocks.LARGE_VEHICLE_DOOR.get()));
        add.accept(new ItemStack(ModBlocks.ROUND_AIRLOCK_DOOR.get()));
        add.accept(new ItemStack(ModBlocks.FIRE_DOOR.get()));
        add.accept(new ItemStack(ModBlocks.SLIDING_SEAL_DOOR.get()));
        add.accept(new ItemStack(ModBlocks.SECURE_ACCESS_DOOR.get()));
        add.accept(new ItemStack(ModBlocks.QE_CONTAINMENT.get()));
        add.accept(new ItemStack(ModBlocks.QE_SLIDING.get()));
        add.accept(new ItemStack(ModBlocks.WATER_DOOR.get()));
        add.accept(new ItemStack(ModBlocks.SILO_HATCH.get()));
        add.accept(new ItemStack(ModBlocks.SILO_HATCH_LARGE.get()));
        add.accept(new ItemStack(ModBlocks.VAULT_DOOR.get()));
        add.accept(new ItemStack(ModBlocks.TRANSITION_SEAL.get()));
        add.accept(new ItemStack(ModBlocks.SLIDE_DOOR.get()));
    }

    // СТАНКИ
    public static void populateMachinesTab(BiConsumer<ItemStack, CreativeModeTab.TabVisibility> acceptor) {
        // Упрощенный Consumer, по умолчанию использующий PARENT_AND_SEARCH_TABS
        Consumer<ItemStack> add = stack -> acceptor.accept(stack, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
        add.accept(new ItemStack(ModBlocks.CRATE_IRON.get()));
        add.accept(new ItemStack(ModBlocks.CRATE_STEEL.get()));
        add.accept(new ItemStack(ModBlocks.CRATE_TUNGSTEN.get()));
        add.accept(new ItemStack(ModBlocks.CRATE_DESH.get()));
        add.accept(new ItemStack(ModBlocks.CRATE_TEMPLATE.get()));
        add.accept(new ItemStack(ModBlocks.BARREL_CORRODED.get()));
        add.accept(new ItemStack(ModBlocks.BARREL_IRON.get()));
        add.accept(new ItemStack(ModBlocks.BARREL_STEEL.get()));
        add.accept(new ItemStack(ModBlocks.BARREL_TCALLOY.get()));
        add.accept(new ItemStack(ModBlocks.BARREL_PLASTIC.get()));
        add.accept(new ItemStack(ModBlocks.ANVIL_IRON.get()));
        add.accept(new ItemStack(ModBlocks.ANVIL_LEAD.get()));
        add.accept(new ItemStack(ModBlocks.ANVIL_STEEL.get()));
        add.accept(new ItemStack(ModBlocks.ANVIL_DESH.get()));
        add.accept(new ItemStack(ModBlocks.ANVIL_FERROURANIUM.get()));
        add.accept(new ItemStack(ModBlocks.ANVIL_SATURNITE.get()));
        add.accept(new ItemStack(ModBlocks.ANVIL_BISMUTH_BRONZE.get()));
        add.accept(new ItemStack(ModBlocks.ANVIL_ARSENIC_BRONZE.get()));
        add.accept(new ItemStack(ModBlocks.ANVIL_SCHRABIDATE.get()));
        add.accept(new ItemStack(ModBlocks.ANVIL_DNT.get()));
        add.accept(new ItemStack(ModBlocks.ANVIL_OSMIRIDIUM.get()));
        add.accept(new ItemStack(ModBlocks.ANVIL_MURKY.get()));
        add.accept(new ItemStack(ModBlocks.PRESS.get()));
        add.accept(new ItemStack(ModBlocks.BLAST_FURNACE.get()));
        add.accept(new ItemStack(ModBlocks.BLAST_FURNACE_EXTENSION.get()));
        add.accept(new ItemStack(ModBlocks.HEATING_OVEN.get()));
        add.accept(new ItemStack(ModBlocks.SHREDDER.get()));
        add.accept(new ItemStack(ModBlocks.WOOD_BURNER.get()));
        add.accept(new ItemStack(ModBlocks.CHEMICAL_PLANT.get()));
        add.accept(new ItemStack(ModBlocks.CENTRIFUGE.get()));
        add.accept(new ItemStack(ModBlocks.CRYSTALLIZER.get()));
        add.accept(new ItemStack(ModBlocks.MACHINE_ASSEMBLER.get()));
        add.accept(new ItemStack(ModBlocks.ADVANCED_ASSEMBLY_MACHINE.get()));
        add.accept(new ItemStack(ModBlocks.HYDRAULIC_FRACKINING_TOWER.get()));
        add.accept(new ItemStack(ModBlocks.FLUID_TANK.get()));
        add.accept(new ItemStack(ModBlocks.MACHINE_BATTERY_SOCKET.get()));
        add.accept(new ItemStack(ModBlocks.INDUSTRIAL_BOILER.get()));
        add.accept(new ItemStack(ModBlocks.INDUSTRIAL_TURBINE.get()));
        add.accept(new ItemStack(ModBlocks.REFINERY.get()));
        add.accept(new ItemStack(ModBlocks.MACHINE_BATTERY.get()));
        add.accept(new ItemStack(ModBlocks.MACHINE_BATTERY_LITHIUM.get()));
        add.accept(new ItemStack(ModBlocks.MACHINE_BATTERY_SCHRABIDIUM.get()));
        add.accept(new ItemStack(ModBlocks.MACHINE_BATTERY_DINEUTRONIUM.get()));
        add.accept(new ItemStack(ModBlocks.CONVERTER_BLOCK.get()));
        add.accept(new ItemStack(ModBlocks.SWITCH.get()));
        add.accept(new ItemStack(ModBlocks.WIRE_COATED.get()));
    }

    // ТОПЛИВО И ЭЛЕМЕНТЫ МЕХАНИЗМОВ
    public static void populateFuelTab(BiConsumer<ItemStack, CreativeModeTab.TabVisibility> acceptor) {
        // Упрощенный Consumer, по умолчанию использующий PARENT_AND_SEARCH_TABS
        Consumer<ItemStack> add = stack -> acceptor.accept(stack, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
        add.accept(new ItemStack(ModItems.CREATIVE_BATTERY.get()));



// 1. Создаем список всех батареек
        List<RegistrySupplier<Item>> batteriesToAdd = List.of(
                ModItems.BATTERY_POTATO,
                ModItems.BATTERY,
                ModItems.BATTERY_RED_CELL,
                ModItems.BATTERY_RED_CELL_6,
                ModItems.BATTERY_RED_CELL_24,
                ModItems.BATTERY_ADVANCED,
                ModItems.BATTERY_ADVANCED_CELL,
                ModItems.BATTERY_ADVANCED_CELL_4,
                ModItems.BATTERY_ADVANCED_CELL_12,
                ModItems.BATTERY_LITHIUM,
                ModItems.BATTERY_LITHIUM_CELL,
                ModItems.BATTERY_LITHIUM_CELL_3,
                ModItems.BATTERY_LITHIUM_CELL_6,
                ModItems.BATTERY_SCHRABIDIUM,
                ModItems.BATTERY_SCHRABIDIUM_CELL,
                ModItems.BATTERY_SCHRABIDIUM_CELL_2,
                ModItems.BATTERY_SCHRABIDIUM_CELL_4,
                ModItems.BATTERY_SPARK,
                ModItems.BATTERY_TRIXITE,
                ModItems.BATTERY_SPARK_CELL_6,
                ModItems.BATTERY_SPARK_CELL_25,
                ModItems.BATTERY_SPARK_CELL_100,
                ModItems.BATTERY_SPARK_CELL_1000,
                ModItems.BATTERY_SPARK_CELL_2500,
                ModItems.BATTERY_SPARK_CELL_10000,
                ModItems.BATTERY_SPARK_CELL_POWER
        );

// 2. Проходимся по списку и добавляем 2 версии каждой
        for (RegistrySupplier<Item> batteryRegObj : batteriesToAdd) {
            Item item = batteryRegObj.get();

            // Проверка, что это ModBatteryItem
            if (item instanceof ModBatteryItem batteryItem) {
                // Добавляем пустую батарею
                ItemStack emptyStack = new ItemStack(batteryItem);
                add.accept(emptyStack);

                // Создаем заряженную батарею
                ItemStack chargedStack = new ItemStack(batteryItem);
                ModBatteryItem.setEnergy(chargedStack, batteryItem.getCapacity());
                add.accept(chargedStack);

                if (ModClothConfig.get().enableDebugLogging) {
                    MainRegistry.LOGGER.debug("Added empty and charged variants of {} to creative tab",
                            batteryRegObj.getId());
                }
            } else {
                // На всякий случай, если в списке что-то не ModBatteryItem
                add.accept(new ItemStack(item));
                MainRegistry.LOGGER.warn("Item {} is not a ModBatteryItem, added as regular item",
                        batteryRegObj.getId());
            }
        }

        if (ModClothConfig.get().enableDebugLogging) {
            MainRegistry.LOGGER.info("Added {} battery variants to NTM Fuel tab", batteriesToAdd.size() * 2);
        }

        add.accept(new ItemStack(ModItems.FLUID_BARREL.get()));
        for (ModFluids.FluidEntry entry : HbmFluidRegistry.getOrderedFluids()) {
            ItemStack filledBarrel = new ItemStack(ModItems.FLUID_BARREL.get());
            dev.architectury.fluid.FluidStack archFluidStack = dev.architectury.fluid.FluidStack.create(entry.getSource(), FluidBarrelItem.CAPACITY);
            FluidBarrelItem.setFluid(filledBarrel, archFluidStack);
            add.accept(filledBarrel);
        }
        // Fluid Ducts - one per fluid type (neo / colored / silver styles)
        for (ModFluids.FluidEntry entry : HbmFluidRegistry.getOrderedFluids()) {
            add.accept(com.hbm_m.item.liquids.FluidDuctItem.createStack(ModItems.FLUID_DUCT.get(), entry));
            add.accept(com.hbm_m.item.liquids.FluidDuctItem.createStack(ModItems.FLUID_DUCT_COLORED.get(), entry));
            add.accept(com.hbm_m.item.liquids.FluidDuctItem.createStack(ModItems.FLUID_DUCT_SILVER.get(), entry));
        }
        add.accept(new ItemStack(ModItems.FLUID_VALVE.get()));
        add.accept(new ItemStack(ModItems.FLUID_PUMP.get()));
        add.accept(new ItemStack(ModItems.FLUID_EXHAUST.get()));
//        add.accept(new ItemStack(ModItems.CRUDE_OIL_BUCKET.get()));
        add.accept(new ItemStack(ModItems.INFINITE_WATER_500.get()));
        add.accept(new ItemStack(ModItems.INFINITE_WATER_5000.get()));
        add.accept(new ItemStack(ModItems.FLUID_BARREL_INFINITE.get()));
    }

    public static void populateTemplatesTab(BiConsumer<ItemStack, CreativeModeTab.TabVisibility> acceptor) {
        // Упрощенный Consumer, по умолчанию использующий PARENT_AND_SEARCH_TABS.
        // В 1.20.1 игра падает, если один и тот же ItemStack (item+tag) добавить в вкладку дважды.
        Set<String> seen = new HashSet<>();
        Consumer<ItemStack> add = stack -> {
            if (stack == null || stack.isEmpty()) return;
            String itemId = String.valueOf(BuiltInRegistries.ITEM.getKey(stack.getItem()));
            String tag = stack.getTag() == null ? "" : stack.getTag().toString();
            if (!seen.add(itemId + "|" + tag)) return;
            acceptor.accept(stack, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
        };

        add.accept(new ItemStack(ModItems.BLADE_STEEL.get()));
        add.accept(new ItemStack(ModItems.BLADE_TITANIUM.get()));
        add.accept(new ItemStack(ModItems.BLADE_ALLOY.get()));
        add.accept(new ItemStack(ModItems.BLADE_TEST.get()));
        add.accept(new ItemStack(ModItems.STAMP_STONE_FLAT.get()));
        add.accept(new ItemStack(ModItems.STAMP_STONE_PLATE.get()));
        add.accept(new ItemStack(ModItems.STAMP_STONE_WIRE.get()));
        add.accept(new ItemStack(ModItems.STAMP_STONE_CIRCUIT.get()));
        add.accept(new ItemStack(ModItems.STAMP_IRON_FLAT.get()));
        add.accept(new ItemStack(ModItems.STAMP_IRON_PLATE.get()));
        add.accept(new ItemStack(ModItems.STAMP_IRON_WIRE.get()));
        add.accept(new ItemStack(ModItems.STAMP_IRON_CIRCUIT.get()));
        add.accept(new ItemStack(ModItems.STAMP_IRON_9.get()));
        add.accept(new ItemStack(ModItems.STAMP_IRON_44.get()));
        add.accept(new ItemStack(ModItems.STAMP_IRON_50.get()));
        add.accept(new ItemStack(ModItems.STAMP_IRON_357.get()));
        add.accept(new ItemStack(ModItems.STAMP_STEEL_FLAT.get()));
        add.accept(new ItemStack(ModItems.STAMP_STEEL_PLATE.get()));
        add.accept(new ItemStack(ModItems.STAMP_STEEL_WIRE.get()));
        add.accept(new ItemStack(ModItems.STAMP_STEEL_CIRCUIT.get()));
        add.accept(new ItemStack(ModItems.STAMP_TITANIUM_FLAT.get()));
        add.accept(new ItemStack(ModItems.STAMP_TITANIUM_PLATE.get()));
        add.accept(new ItemStack(ModItems.STAMP_TITANIUM_WIRE.get()));
        add.accept(new ItemStack(ModItems.STAMP_TITANIUM_CIRCUIT.get()));
        add.accept(new ItemStack(ModItems.STAMP_OBSIDIAN_FLAT.get()));
        add.accept(new ItemStack(ModItems.STAMP_OBSIDIAN_PLATE.get()));
        add.accept(new ItemStack(ModItems.STAMP_OBSIDIAN_WIRE.get()));
        add.accept(new ItemStack(ModItems.STAMP_OBSIDIAN_CIRCUIT.get()));
        add.accept(new ItemStack(ModItems.STAMP_DESH_FLAT.get()));
        add.accept(new ItemStack(ModItems.STAMP_DESH_PLATE.get()));
        add.accept(new ItemStack(ModItems.STAMP_DESH_WIRE.get()));
        add.accept(new ItemStack(ModItems.STAMP_DESH_CIRCUIT.get()));
        add.accept(new ItemStack(ModItems.STAMP_DESH_9.get()));
        add.accept(new ItemStack(ModItems.STAMP_DESH_44.get()));
        add.accept(new ItemStack(ModItems.STAMP_DESH_50.get()));
        add.accept(new ItemStack(ModItems.STAMP_DESH_357.get()));

        add.accept(new ItemStack(ModItems.TEMPLATE_FOLDER.get()));

        EnvExecutor.runInEnv(Env.CLIENT, () -> () -> {
            ClientSetup.addTemplatesClient(add);
        });

        for (ModFluids.FluidEntry entry : HbmFluidRegistry.getOrderedFluids()) {
            ItemStack idStack = new ItemStack(ModItems.FLUID_IDENTIFIER.get());
            FluidIdentifierItem.setType(idStack, HbmFluidRegistry.getFluidName(entry.getSource()), true);
            add.accept(idStack);
        }
    }

    /**
     * Создает ItemStack с максимальным зарядом для силовой брони
     */
    private static ItemStack createChargedArmorStack(Item item) {
        ItemStack stack = new ItemStack(item);

        // Проверяем, является ли предмет силовой броней
        if (item instanceof com.hbm_m.powerarmor.ModArmorFSBPowered powerArmor) {
            // Получаем максимальную емкость и устанавливаем полный заряд
            long maxCapacity = powerArmor.getMaxCharge(stack);
            stack.getOrCreateTag().putLong("charge", maxCapacity);
        }

        return stack;
    }

}

