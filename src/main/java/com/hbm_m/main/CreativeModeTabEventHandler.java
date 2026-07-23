package com.hbm_m.main;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import com.hbm_m.api.fluids.HbmFluidRegistry;
import com.hbm_m.armormod.item.ItemArmorMod;
import com.hbm_m.block.ModBlocks;
import com.hbm_m.block.generic.BlockAbsorber;
import com.hbm_m.client.ClientSetup;
import com.hbm_m.config.ModClothConfig;
import com.hbm_m.creativetabs.MissileTab;
import com.hbm_m.inventory.fluid.ModFluids;
import com.hbm_m.item.BlockAbsorberItem;
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
/*import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
*///?}
//? if forge {
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
//?}
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;


/**
 * Наполнение креативных вкладок (логика из старого Forge {@code MainRegistry#addCreative}).
 */
@SuppressWarnings("UnstableApiUsage")
public final class CreativeModeTabEventHandler {

    private CreativeModeTabEventHandler() {
    }

    //? if forge {
    public static void onBuildCreativeModeTabContents(BuildCreativeModeTabContentsEvent event) {
        MainRegistry.LOGGER.info("Building creative tab contents for: " + event.getTabKey());
        
        if (event.getTab() == ModCreativeTabs.NTM_RESOURCES_TAB.get() || event.getTabKey() == CreativeModeTabs.SEARCH) {
            populateResourceTab((stack, vis) -> event.accept(stack, vis));
        }

        if (event.getTab() == ModCreativeTabs.NTM_FUEL_TAB.get() || event.getTabKey() == CreativeModeTabs.SEARCH) {
            populateFuelTab((stack, vis) -> event.accept(stack, vis));
        }

        if (event.getTab() == ModCreativeTabs.NTM_TEMPLATES_TAB.get() || event.getTabKey() == CreativeModeTabs.SEARCH) {
            populateTemplatesTab((stack, vis) -> event.accept(stack, vis));
        }

        if (event.getTab() == ModCreativeTabs.NTM_WEAPONS_TAB.get() || event.getTabKey() == CreativeModeTabs.SEARCH) {
            populateWeaponsTab((stack, vis) -> event.accept(stack, vis));
        }

        if (event.getTabKey() == CreativeModeTabs.COMBAT) {
            populateCombatTab((stack, vis) -> event.accept(stack, vis));
        }

        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(new ItemStack(ModItems.MUSIC_DISC_BUNKER.get()),
                    CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
        }

        if (event.getTabKey() == CreativeModeTabs.SPAWN_EGGS || event.getTabKey() == CreativeModeTabs.SEARCH) {
            populateSpawnEggs((stack, vis) -> event.accept(stack, vis));
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

        if (event.getTab() == ModCreativeTabs.NTM_BOMBS_TAB.get() || event.getTabKey() == CreativeModeTabs.SEARCH) {
            populateNukeTab((stack, vis) -> event.accept(stack, vis));
        }

        if (event.getTab() == ModCreativeTabs.NTM_MISSILES_TAB.get() || event.getTabKey() == CreativeModeTabs.SEARCH) {
            populateMissilesTab((stack, vis) -> event.accept(stack, vis));
        }

        if (event.getTab() == ModCreativeTabs.NTM_DEV_TAB.get() || event.getTabKey() == CreativeModeTabs.SEARCH) {
            populateDevItemsTab((stack, vis) -> event.accept(stack, vis));
        }

    }
    //?}

    //? if fabric {
    /*public static void initFabric() {
        // Кастомные вкладки наполняются через `CreativeModeTab#displayItems` при их регистрации.
        // На Fabric добавляем только в ванильные вкладки через точечные хуки по ключу вкладки,
        // чтобы не зависеть от типов аргументов колбэка (tab key vs tab instance).
        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.COMBAT).register(entries ->
                populateCombatTab((stack, vis) -> entries.accept(stack, vis)));

        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(entries ->
                entries.accept(new ItemStack(ModItems.MUSIC_DISC_BUNKER.get()),
                        CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS));

        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.SPAWN_EGGS).register(entries ->
                populateSpawnEggs((stack, vis) -> entries.accept(stack, vis)));

        // На Forge мы добавляем почти всё в SEARCH для удобства поиска.
        // На Fabric ванильный SEARCH не подхватывает наши кастомные вкладки автоматически,
        // поэтому зеркалим поведение.
        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.SEARCH).register(entries -> {
            populateWeaponsTab((stack, vis) -> entries.accept(stack, vis));
            populateCombatTab((stack, vis) -> entries.accept(stack, vis));

            entries.accept(new ItemStack(ModItems.MUSIC_DISC_BUNKER.get()),
                    CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);

            populateSpawnEggs((stack, vis) -> entries.accept(stack, vis));

            populateResourceTab((stack, vis) -> entries.accept(stack, vis));
            populateConsumablesTab((stack, vis) -> entries.accept(stack, vis));
            populateSparepartsTab((stack, vis) -> entries.accept(stack, vis));
            populateOresTab((stack, vis) -> entries.accept(stack, vis));
            populateBuildingTab((stack, vis) -> entries.accept(stack, vis));
            populateMachinesTab((stack, vis) -> entries.accept(stack, vis));
            populateFuelTab((stack, vis) -> entries.accept(stack, vis));
            populateTemplatesTab((stack, vis) -> entries.accept(stack, vis));
            populateNukeTab((stack, vis) -> entries.accept(stack, vis));
            populateMissilesTab((stack, vis) -> entries.accept(stack, vis));
            populateDevItemsTab((stack, vis) -> entries.accept(stack, vis));
        });
    }
    *///?}

    /** Яйца призыва и связанное (ванильная вкладка + поиск на Fabric). */
    public static void populateSpawnEggs(BiConsumer<ItemStack, CreativeModeTab.TabVisibility> acceptor) {
        Consumer<ItemStack> add = stack -> acceptor.accept(stack, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
        add.accept(new ItemStack(ModItems.NOLO_SPAWN_EGG.get()));
        add.accept(new ItemStack(ModItems.ENTITY_MOB_TAINTED_CREEPER_SPAWN_EGG.get()));
        add.accept(new ItemStack(ModItems.ENTITY_MOB_GOLD_CREEPER_SPAWN_EGG.get()));
        add.accept(new ItemStack(ModItems.ENTITY_MOB_VOLATILE_CREEPER_SPAWN_EGG.get()));
        add.accept(new ItemStack(ModItems.ENTITY_MOB_PHOSGENE_CREEPER_SPAWN_EGG.get()));
        add.accept(new ItemStack(ModItems.ENTITY_MOB_NUCLEAR_CREEPER_SPAWN_EGG.get()));
    }

    /** Ракеты и спутники (порядок из GIT {@code MainRegistry.missileTab}). */
    public static void populateMissilesTab(BiConsumer<ItemStack, CreativeModeTab.TabVisibility> acceptor) {
        Consumer<ItemStack> add = stack -> acceptor.accept(stack, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);

        add.accept(new ItemStack(ModBlocks.LAUNCH_PAD.get()));
        add.accept(new ItemStack(ModBlocks.LAUNCH_PAD_RUSTED.get()));
        add.accept(new ItemStack(ModBlocks.CRATE_CONSERVE.get()));
        add.accept(new ItemStack(ModBlocks.RADAR.get()));
        add.accept(new ItemStack(ModBlocks.LARGE_RADAR.get()));
        add.accept(new ItemStack(ModItems.RANGEFINDER.get()));
        add.accept(new ItemStack(ModItems.DESIGNATOR.get()));
        add.accept(new ItemStack(ModItems.DESIGNATOR_RANGE.get()));
        add.accept(new ItemStack(ModItems.DESIGNATOR_MANUAL.get()));
        add.accept(new ItemStack(ModItems.MISSILE_GENERIC.get()));
        add.accept(new ItemStack(ModItems.MISSILE_ABM.get()));
        add.accept(new ItemStack(ModItems.MISSILE_INCENDIARY.get()));
        add.accept(new ItemStack(ModItems.MISSILE_CLUSTER.get()));
        add.accept(new ItemStack(ModItems.MISSILE_BUSTER.get()));
        add.accept(new ItemStack(ModItems.MISSILE_DECOY.get()));
        add.accept(new ItemStack(ModItems.MISSILE_STRONG.get()));
        add.accept(new ItemStack(ModItems.MISSILE_INCENDIARY_STRONG.get()));
        add.accept(new ItemStack(ModItems.MISSILE_CLUSTER_STRONG.get()));
        add.accept(new ItemStack(ModItems.MISSILE_BUSTER_STRONG.get()));
        add.accept(new ItemStack(ModItems.MISSILE_EMP_STRONG.get()));
        add.accept(new ItemStack(ModItems.MISSILE_BURST.get()));
        add.accept(new ItemStack(ModItems.MISSILE_INFERNO.get()));
        add.accept(new ItemStack(ModItems.MISSILE_RAIN.get()));
        add.accept(new ItemStack(ModItems.MISSILE_DRILL.get()));
        add.accept(new ItemStack(ModItems.MISSILE_NUCLEAR.get()));
        add.accept(new ItemStack(ModItems.MISSILE_NUCLEAR_CLUSTER.get()));
        add.accept(new ItemStack(ModItems.MISSILE_VOLCANO.get()));
        add.accept(new ItemStack(ModItems.MISSILE_DOOMSDAY.get()));
        add.accept(new ItemStack(ModItems.MISSILE_DOOMSDAY_RUSTED.get()));
        add.accept(new ItemStack(ModItems.MISSILE_TAINT.get()));
        add.accept(new ItemStack(ModItems.MISSILE_MICRO.get()));
        add.accept(new ItemStack(ModItems.MISSILE_BHOLE.get()));
        add.accept(new ItemStack(ModItems.MISSILE_SCHRABIDIUM.get()));
        add.accept(new ItemStack(ModItems.MISSILE_EMP.get()));
        add.accept(new ItemStack(ModItems.MISSILE_SHUTTLE.get()));
        add.accept(new ItemStack(ModItems.MISSILE_STEALTH.get()));

        MissileTab.appendExtraItems(add);
    }

    /** Бомбы (порядок из GIT {@code MainRegistry.nukeTab}). */
    public static void populateNukeTab(BiConsumer<ItemStack, CreativeModeTab.TabVisibility> acceptor) {
        Set<String> seen = new HashSet<>();
        Consumer<ItemStack> add = stack -> {
            if (stack == null || stack.isEmpty()) {
                return;
            }
            String itemId = String.valueOf(BuiltInRegistries.ITEM.getKey(stack.getItem()));
            String tag = stack.getTag() == null ? "" : stack.getTag().toString();
            if (!seen.add(itemId + "|" + tag)) {
                return;
            }
            acceptor.accept(stack, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
        };

        add.accept(new ItemStack(ModBlocks.NUKE_FAT_MAN.get()));
        add.accept(new ItemStack(ModBlocks.NUKE_PROTOTYPE.get()));
        add.accept(new ItemStack(ModBlocks.DUD_CONVENTIONAL.get()));
        add.accept(new ItemStack(ModBlocks.DUD_NUKE.get()));
        add.accept(new ItemStack(ModBlocks.DUD_SALTED.get()));
        add.accept(new ItemStack(ModBlocks.C4.get()));
        add.accept(new ItemStack(ModBlocks.MINE_AP.get()));
        add.accept(new ItemStack(ModBlocks.MINE_FAT.get()));
        add.accept(new ItemStack(ModBlocks.NAVAL_MINE.get()));
        add.accept(new ItemStack(ModBlocks.EXPLOSIVE_CHARGE.get()));
        add.accept(new ItemStack(ModBlocks.NUCLEAR_CHARGE.get()));
        add.accept(new ItemStack(ModBlocks.DET_MINER.get()));
        add.accept(new ItemStack(ModBlocks.BARREL_RED.get()));
        add.accept(new ItemStack(ModBlocks.BARREL_PINK.get()));
        add.accept(new ItemStack(ModBlocks.BARREL_LOX.get()));
        add.accept(new ItemStack(ModBlocks.BARREL_VITRIFIED.get()));
        add.accept(new ItemStack(ModBlocks.BARREL_TAINT.get()));
        add.accept(new ItemStack(ModBlocks.BARREL_YELLOW.get()));
        
        List<RegistrySupplier<Item>> batteriesToAdd = List.of(
            ModItems.BATTERY_SPARK,
            ModItems.BATTERY_TRIXITE
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
            MainRegistry.LOGGER.info("Added {} battery variants to NTM Bomb tab", batteriesToAdd.size() * 2);
        }

        add.accept(new ItemStack(ModItems.FAT_MAN_EXPLOSIVE.get()));

        add.accept(new ItemStack(ModItems.FAT_MAN_IGNITER.get()));
        add.accept(new ItemStack(ModItems.FAT_MAN_CORE.get()));
        
        // add.accept(new ItemStack(ModItems.IGNITER.get()));
        add.accept(new ItemStack(ModItems.DETONATOR.get()));
        add.accept(new ItemStack(ModItems.MULTI_DETONATOR.get()));
        add.accept(new ItemStack(ModItems.RANGE_DETONATOR.get()));
        add.accept(new ItemStack(ModItems.DEFUSER.get()));
    }

    public static void populateWeaponsTab(BiConsumer<ItemStack, CreativeModeTab.TabVisibility> acceptor) {
        // Упрощенный Consumer, по умолчанию использующий PARENT_AND_SEARCH_TABS
        Consumer<ItemStack> add = stack -> acceptor.accept(stack, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);

        add.accept(new ItemStack(ModBlocks.BARBED_WIRE_FIRE.get()));
        add.accept(new ItemStack(ModBlocks.BARBED_WIRE_POISON.get()));
        add.accept(new ItemStack(ModBlocks.BARBED_WIRE_RAD.get()));
        add.accept(new ItemStack(ModBlocks.BARBED_WIRE_WITHER.get()));
        add.accept(new ItemStack(ModBlocks.BARBED_WIRE.get()));
        add.accept(new ItemStack(ModBlocks.TURRET_SENTRY.get()));
        add.accept(new ItemStack(ModBlocks.TURRET_CHEKHOV.get()));
        add.accept(new ItemStack(ModBlocks.TURRET_FRIENDLY.get()));
        add.accept(new ItemStack(ModBlocks.TURRET_JEREMY.get()));
        add.accept(new ItemStack(ModBlocks.TURRET_TAUON.get()));
        add.accept(new ItemStack(ModBlocks.TURRET_RICHARD.get()));
        add.accept(new ItemStack(ModBlocks.TURRET_HOWARD.get()));
        add.accept(new ItemStack(ModBlocks.TURRET_MAXWELL.get()));
        add.accept(new ItemStack(ModBlocks.TURRET_FRITZ.get()));
        add.accept(new ItemStack(ModBlocks.TURRET_ARTY.get()));
        add.accept(new ItemStack(ModBlocks.TURRET_HIMARS.get()));
        add.accept(new ItemStack(ModItems.TURRET_AMMO.get()));
        add.accept(new ItemStack(ModItems.AMMO_9MM_SP.get()));
        add.accept(new ItemStack(ModItems.AMMO_9MM_FMJ.get()));
        add.accept(new ItemStack(ModItems.AMMO_9MM_JHP.get()));
        add.accept(new ItemStack(ModItems.AMMO_9MM_AP.get()));
        add.accept(new ItemStack(ModItems.AMMO_50_SP.get()));
        add.accept(new ItemStack(ModItems.AMMO_50_FMJ.get()));
        add.accept(new ItemStack(ModItems.AMMO_50_JHP.get()));
        add.accept(new ItemStack(ModItems.AMMO_50_AP.get()));
        add.accept(new ItemStack(ModItems.AMMO_50_DU.get()));
        add.accept(new ItemStack(ModItems.AMMO_556_SP.get()));
        add.accept(new ItemStack(ModItems.AMMO_556_FMJ.get()));
        add.accept(new ItemStack(ModItems.AMMO_556_JHP.get()));
        add.accept(new ItemStack(ModItems.AMMO_556_AP.get()));
        add.accept(new ItemStack(ModItems.ROCKET_TURRET_STANDARD.get()));
        add.accept(new ItemStack(ModItems.ROCKET_HIMARS_STANDARD.get()));
        add.accept(new ItemStack(ModItems.ROCKET_HIMARS_HE.get()));
        add.accept(new ItemStack(ModItems.ROCKET_HIMARS_LAVA.get()));
        add.accept(new ItemStack(ModItems.ROCKET_HIMARS_MINI_NUKE.get()));
        add.accept(new ItemStack(ModItems.ROCKET_HIMARS_WP.get()));
        add.accept(new ItemStack(ModItems.ROCKET_HIMARS_THERMOBARIC.get()));
        add.accept(new ItemStack(ModItems.AMMO_TAU_URANIUM.get()));
        add.accept(new ItemStack(ModItems.AMMO_FLAME_DIESEL.get()));
        add.accept(new ItemStack(ModBlocks.MACHINE_MISSILE_ASSEMBLY.get()));
        add.accept(new ItemStack(ModItems.MISSILE_FUSELAGE.get()));
        add.accept(new ItemStack(ModItems.MISSILE_CHIP.get()));
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
        // add.accept(new ItemStack(ModItems.CELL_SAS3.get()));
        // add.accept(new ItemStack(ModItems.ROD_QUAD_LEAD.get()));
        // add.accept(new ItemStack(ModItems.ROD_QUAD_NP237.get()));
        // add.accept(new ItemStack(ModItems.ROD_QUAD_URANIUM.get()));
        add.accept(new ItemStack(ModBlocks.AIRBOMB.get()));
        add.accept(new ItemStack(ModItems.AIRBOMB_A.get()));
        add.accept(new ItemStack(ModBlocks.BALEBOMB_TEST.get()));
        add.accept(new ItemStack(ModItems.AIRNUKEBOMB_A.get()));
        add.accept(new ItemStack(ModBlocks.GIGA_DET.get()));
        add.accept(new ItemStack(ModBlocks.WASTE_CHARGE.get()));
        add.accept(new ItemStack(ModBlocks.SMOKE_BOMB.get()));
        add.accept(new ItemStack(ModBlocks.EXPLOSIVE_CHARGE.get()));
        add.accept(new ItemStack(ModBlocks.NUCLEAR_CHARGE.get()));
        add.accept(new ItemStack(ModBlocks.DUD_CONVENTIONAL.get()));
        add.accept(new ItemStack(ModBlocks.DUD_NUKE.get()));
        add.accept(new ItemStack(ModBlocks.DUD_SALTED.get()));
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
        add.accept(new ItemStack(ModItems.DRILL_TITANIUM.get()));
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
        add.accept(new ItemStack(ModItems.FALLOUT.get()));
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
        add.accept(new ItemStack(ModItems.NUGGET_TANTALIUM.get()));
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
        add.accept(new ItemStack(ModItems.POWDER_DESH_MIX.get()));
        add.accept(new ItemStack(ModItems.POWDER_NITAN_MIX.get()));

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

        add.accept(new ItemStack(ModItems.CROWBAR.get()));
        add.accept(new ItemStack(ModItems.SCREWDRIVER.get()));

        add.accept(new ItemStack(ModItems.DOSIMETER.get()));
        add.accept(new ItemStack(ModItems.DIGAMMA_DIAGNOSTIC.get()));
        add.accept(new ItemStack(ModItems.GEIGER_COUNTER.get()));
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
        add.accept(new ItemStack(ModItems.SHELL_STEEL.get()));
        add.accept(new ItemStack(ModItems.SHELL_COPPER.get()));
        add.accept(new ItemStack(ModItems.SHELL_ALUMINUM.get()));
        add.accept(new ItemStack(ModItems.SHELL_TITANIUM.get()));

        add.accept(new ItemStack(ModItems.BOLT_STEEL.get()));
        add.accept(new ItemStack(ModItems.BOLT_LEAD.get()));
        add.accept(new ItemStack(ModItems.BOLT_TUNGSTEN.get()));
        add.accept(new ItemStack(ModItems.BOLT_HIGHSPEED_STEEL.get()));
        add.accept(new ItemStack(ModItems.COIL_TUNGSTEN.get()));
        add.accept(new ItemStack(ModItems.CENTRIFUGE_ELEMENT.get()));
        add.accept(new ItemStack(ModItems.GAS_EMPTY.get()));
        add.accept(new ItemStack(ModItems.DUCTTAPE.get()));
        add.accept(new ItemStack(ModItems.HAZMAT_CLOTH.get()));
        add.accept(new ItemStack(ModItems.HAZMAT_CLOTH_GREY.get()));
        add.accept(new ItemStack(ModItems.HAZMAT_CLOTH_RED.get()));
        add.accept(new ItemStack(ModItems.ASBESTOS_CLOTH.get()));

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
        add.accept(new ItemStack(ModItems.WIRE_IRON.get()));
        add.accept(new ItemStack(ModItems.WIRE_STEEL.get()));
        add.accept(new ItemStack(ModItems.WIRE_TITANIUM.get()));
        add.accept(new ItemStack(ModItems.WIRE_SATURNITE.get()));
        add.accept(new ItemStack(ModItems.WIRE_COMBINE_STEEL.get()));

        // Dense Wires
        add.accept(new ItemStack(ModItems.WIRE_DENSE_IRON.get()));
        add.accept(new ItemStack(ModItems.WIRE_DENSE_ALUMINIUM.get()));
        add.accept(new ItemStack(ModItems.WIRE_DENSE_TITANIUM.get()));
        add.accept(new ItemStack(ModItems.WIRE_DENSE_LEAD.get()));
        add.accept(new ItemStack(ModItems.WIRE_DENSE_COPPER.get()));
        add.accept(new ItemStack(ModItems.WIRE_DENSE_STEEL.get()));
        add.accept(new ItemStack(ModItems.WIRE_DENSE_GOLD.get()));
        add.accept(new ItemStack(ModItems.WIRE_DENSE_ADVANCED_ALLOY.get()));
        add.accept(new ItemStack(ModItems.WIRE_DENSE_SCHRABIDIUM.get()));
        add.accept(new ItemStack(ModItems.WIRE_DENSE_SATURNITE.get()));
        add.accept(new ItemStack(ModItems.WIRE_DENSE_COMBINE_STEEL.get()));

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
        add.accept(new ItemStack(ModItems.PIPE_DURA_STEEL.get()));

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
        add.accept(new ItemStack(ModItems.MAGNETRON.get()));
        add.accept(new ItemStack(ModItems.TURBINE_TITANIUM.get()));
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

        // Satellite Parts
        // add.accept(new ItemStack(ModItems.SAT_BASE.get()));
        // add.accept(new ItemStack(ModItems.SAT_HEAD_LASER.get()));
        // add.accept(new ItemStack(ModItems.SAT_LASER.get()));
        // add.accept(new ItemStack(ModItems.SAT_HEAD_RADAR.get()));
        // add.accept(new ItemStack(ModItems.SAT_RADAR.get()));
        // add.accept(new ItemStack(ModItems.SAT_HEAD_MAPPER.get()));
        // add.accept(new ItemStack(ModItems.SAT_MAPPER.get()));
        // add.accept(new ItemStack(ModItems.SAT_HEAD_RESONATOR.get()));
        // add.accept(new ItemStack(ModItems.SAT_RESONATOR.get()));

        // add.accept(new ItemStack(ModItems.LOW_DENSITY_ELEMENT.get()));

        // add.accept(new ItemStack(ModItems.INGOT_TUNGSTEN_CARBIDE.get()));
        // add.accept(new ItemStack(ModItems.INGOT_HIGHSPEED_STEEL.get()));
        add.accept(new ItemStack(ModItems.NEUTRON_REFLECTOR.get()));
        add.accept(new ItemStack(ModItems.WARHEAD_GENERIC_SMALL.get()));
        add.accept(new ItemStack(ModItems.WARHEAD_CLUSTER_LARGE.get()));
        add.accept(new ItemStack(ModItems.WARHEAD_INCENDIARY_MEDIUM.get()));
        add.accept(new ItemStack(ModItems.WARHEAD_BUSTER_SMALL.get()));

        // Missile Parts
        add.accept(new ItemStack(ModItems.MISSILE_ASSEMBLY.get()));
        add.accept(new ItemStack(ModItems.THRUSTER_SMALL.get()));
        add.accept(new ItemStack(ModItems.FUEL_TANK_SMALL.get()));
        add.accept(new ItemStack(ModItems.WARHEAD_CLUSTER_SMALL.get()));
        add.accept(new ItemStack(ModItems.WARHEAD_INCENDIARY_SMALL.get()));
        add.accept(new ItemStack(ModItems.THRUSTER_MEDIUM.get()));
        add.accept(new ItemStack(ModItems.FUEL_TANK_MEDIUM.get()));
        add.accept(new ItemStack(ModItems.WARHEAD_GENERIC_MEDIUM.get()));
        add.accept(new ItemStack(ModItems.WARHEAD_GENERIC_LARGE.get()));
        add.accept(new ItemStack(ModItems.WARHEAD_BUSTER_LARGE.get()));
        add.accept(new ItemStack(ModItems.WARHEAD_MIRV.get()));
        add.accept(new ItemStack(ModItems.WARHEAD_VOLCANO.get()));
        add.accept(new ItemStack(ModItems.WARHEAD_BUSTER_MEDIUM.get()));
        add.accept(new ItemStack(ModItems.WARHEAD_CLUSTER_MEDIUM.get()));
        add.accept(new ItemStack(ModItems.THRUSTER_LARGE.get()));
        add.accept(new ItemStack(ModItems.FUEL_TANK_LARGE.get()));
        add.accept(new ItemStack(ModItems.WARHEAD_NUCLEAR.get()));
        add.accept(new ItemStack(ModItems.THRUSTER_NUCLEAR.get()));
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
        add.accept(new ItemStack(ModBlocks.SCHRABIDIUM_ORE.get()));
        add.accept(new ItemStack(ModBlocks.SCHRABIDIUM_ORE_NETHER.get()));
        add.accept(new ItemStack(ModBlocks.SCHRABIDIUM_ORE_GNEISS.get()));
        add.accept(new ItemStack(ModBlocks.BLOCK_SCHRABIDIUM_CLUSTER.get()));

        // Bedrock-Ore-Progression (Mining Drill): Rohprodukt + alle 156 Veredelungsstufen.
        add.accept(new ItemStack(ModItems.BEDROCK_ORE_FRAGMENT.get()));
        add.accept(new ItemStack(ModItems.BEDROCK_ORE_BASE.get()));
        for (RegistrySupplier<Item> variant : ModItems.BEDROCK_ORE_ALL_VARIANTS) {
            add.accept(new ItemStack(variant.get()));
        }

        add.accept(new ItemStack(ModBlocks.RESOURCE_ASBESTOS.get()));
        add.accept(new ItemStack(ModBlocks.RESOURCE_BAUXITE.get()));
        add.accept(new ItemStack(ModBlocks.RESOURCE_HEMATITE.get()));
        add.accept(new ItemStack(ModBlocks.RESOURCE_LIMESTONE.get()));
        add.accept(new ItemStack(ModItems.LIMESTONE.get()));
        add.accept(new ItemStack(ModBlocks.RESOURCE_MALACHITE.get()));
        add.accept(new ItemStack(ModItems.MALACHITE_CHUNK.get()));
        add.accept(new ItemStack(ModBlocks.RESOURCE_SULFUR.get()));

        add.accept(new ItemStack(ModItems.ALUMINUM_RAW.get()));
        add.accept(new ItemStack(ModItems.BERYLLIUM_RAW.get()));
        add.accept(new ItemStack(ModItems.COBALT_RAW.get()));
        add.accept(new ItemStack(ModItems.LEAD_RAW.get()));
        add.accept(new ItemStack(ModItems.THORIUM_RAW.get()));
        add.accept(new ItemStack(ModItems.TITANIUM_RAW.get()));
        add.accept(new ItemStack(ModItems.TUNGSTEN_RAW.get()));
        add.accept(new ItemStack(ModItems.URANIUM_RAW.get()));
        add.accept(new ItemStack(ModItems.RADIUM_RAW.get()));
        add.accept(new ItemStack(ModItems.SALTPETER.get()));
        add.accept(new ItemStack(ModItems.CRYOLITE.get()));
        add.accept(new ItemStack(ModItems.MOLYSITE.get()));
        add.accept(new ItemStack(ModItems.RAREEARTH_RAW.get()));
        add.accept(new ItemStack(ModItems.POWDER_CHLOROCALCITE.get()));
        add.accept(new ItemStack(ModItems.POWDER_SODIUM.get()));

        add.accept(new ItemStack(ModBlocks.METEOR.get()));
        add.accept(new ItemStack(ModBlocks.METEOR_COBBLE.get()));
        add.accept(new ItemStack(ModBlocks.METEOR_CRUSHED.get()));
        add.accept(new ItemStack(ModBlocks.METEOR_TREASURE.get()));

        add.accept(new ItemStack(ModBlocks.GEYSIR_DIRT.get()));
        add.accept(new ItemStack(ModBlocks.GEYSIR_STONE.get()));

        add.accept(new ItemStack(ModBlocks.NUCLEAR_FALLOUT.get()));
        add.accept(new ItemStack(ModBlocks.BLOCK_FALLOUT.get()));
        add.accept(new ItemStack(ModBlocks.SELLAFIELD_SLAKED.get()));
        add.accept(new ItemStack(ModBlocks.SELLAFIELD_SLAKED1.get()));
        add.accept(new ItemStack(ModBlocks.SELLAFIELD_SLAKED2.get()));
        add.accept(new ItemStack(ModBlocks.SELLAFIELD_SLAKED3.get()));
        add.accept(new ItemStack(ModBlocks.SELLAFIELD_BEDROCK.get()));
        add.accept(new ItemStack(ModBlocks.ORE_SELLAFIELD_DIAMOND.get()));
        add.accept(new ItemStack(ModBlocks.ORE_SELLAFIELD_EMERALD.get()));
        add.accept(new ItemStack(ModBlocks.ORE_SELLAFIELD_URANIUM_SCORCHED.get()));
        add.accept(new ItemStack(ModBlocks.ORE_SELLAFIELD_SCHRABIDIUM.get()));
        add.accept(new ItemStack(ModBlocks.ORE_SELLAFIELD_RADGEM.get()));
        add.accept(new ItemStack(ModBlocks.WASTE_TRINITITE.get()));
        add.accept(new ItemStack(ModBlocks.WASTE_TRINITITE_RED.get()));
        add.accept(new ItemStack(ModBlocks.WASTE_MYCELIUM.get()));
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
        add.accept(new ItemStack(ModBlocks.URANIUM_BLOCK.get()));
        add.accept(new ItemStack(ModBlocks.PLUTONIUM_BLOCK.get()));
        add.accept(new ItemStack(ModBlocks.PLUTONIUM_FUEL_BLOCK.get()));
    }


    // СТРОИТЕЛЬНЫЕ БЛОКИ
    public static void populateBuildingTab(BiConsumer<ItemStack, CreativeModeTab.TabVisibility> acceptor) {
        // Упрощенный Consumer, по умолчанию использующий PARENT_AND_SEARCH_TABS
        Consumer<ItemStack> add = stack -> acceptor.accept(stack, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);

        add.accept(new ItemStack(ModBlocks.DECO_STEEL.get()));
        add.accept(new ItemStack(ModBlocks.DECO_RUSTY_STEEL.get()));
        add.accept(new ItemStack(ModBlocks.DECO_TUNGSTEN.get()));
        add.accept(new ItemStack(ModBlocks.DECO_RED_COPPER.get()));
        add.accept(new ItemStack(ModBlocks.DECO_BERYLLIUM.get()));
        add.accept(new ItemStack(ModBlocks.DECO_ALUMINUM.get()));
        add.accept(new ItemStack(ModBlocks.DECO_LEAD.get()));
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
        add.accept(new ItemStack(ModBlocks.STEEL_POLE.get()));
        add.accept(new ItemStack(ModBlocks.STEEL_WALL.get()));
        add.accept(new ItemStack(ModBlocks.DECO_STEEL_SCAFFOLD.get()));
        add.accept(new ItemStack(ModBlocks.ANTENNA_TOP.get()));
        add.accept(new ItemStack(ModBlocks.PUTER.get()));

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
        // add.accept(new ItemStack(ModBlocks.TRANSITION_SEAL.get()));
        // add.accept(new ItemStack(ModBlocks.SLIDE_DOOR.get()));
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
        add.accept(new ItemStack(ModBlocks.STEAM_CONDENSER.get()));
        add.accept(new ItemStack(ModBlocks.SHREDDER.get()));
        add.accept(new ItemStack(ModBlocks.WOOD_BURNER.get()));
        add.accept(new ItemStack(ModBlocks.MACHINE_SIREN.get()));
        add.accept(new ItemStack(ModBlocks.CHEMICAL_PLANT.get()));
        add.accept(new ItemStack(ModBlocks.CRUCIBLE.get()));
        add.accept(new ItemStack(ModBlocks.FOUNDRY_BASIN.get()));
        add.accept(new ItemStack(ModBlocks.FOUNDRY_CHANNEL.get()));
        add.accept(new ItemStack(ModBlocks.FOUNDRY_OUTLET.get()));
        add.accept(new ItemStack(ModBlocks.FOUNDRY_CHANNEL.get()));
        add.accept(new ItemStack(ModBlocks.GAS_CENTRIFUGE.get()));
        add.accept(new ItemStack(ModBlocks.CENTRIFUGE.get()));
        add.accept(new ItemStack(ModBlocks.CRYSTALLIZER.get()));
        add.accept(new ItemStack(ModBlocks.BREEDER.get()));
        add.accept(new ItemStack(ModBlocks.LARGE_PYLON.get()));
        add.accept(new ItemStack(ModBlocks.MACHINE_ASSEMBLER.get()));
        add.accept(new ItemStack(ModBlocks.ADVANCED_ASSEMBLY_MACHINE.get()));
        add.accept(new ItemStack(ModBlocks.HYDRAULIC_FRACKINING_TOWER.get()));
        add.accept(new ItemStack(ModBlocks.COOLING_TOWER.get()));
        add.accept(new ItemStack(ModBlocks.TOWER_SMALL.get()));
        add.accept(new ItemStack(ModBlocks.CYCLOTRON.get()));
        add.accept(new ItemStack(ModItems.PLATE_CAST_IRON.get()));
        add.accept(new ItemStack(ModItems.PLATE_CAST_STEEL.get()));
        add.accept(new ItemStack(ModItems.PLATE_CAST_COPPER.get()));
        add.accept(new ItemStack(ModItems.PLATE_CAST_GOLD.get()));
        add.accept(new ItemStack(ModItems.PLATE_CAST_TITANIUM.get()));
        add.accept(new ItemStack(ModItems.PLATE_CAST_ALUMINIUM.get()));
        add.accept(new ItemStack(ModItems.PLATE_CAST_TUNGSTEN.get()));
        add.accept(new ItemStack(ModItems.PLATE_CAST_ZIRCONIUM.get()));
        add.accept(new ItemStack(ModItems.PLATE_CAST_OSMIRIDIUM.get()));
        add.accept(new ItemStack(ModItems.PLATE_CAST_ALLOY.get()));
        add.accept(new ItemStack(ModItems.PLATE_CAST_DURA_STEEL.get()));
        add.accept(new ItemStack(ModItems.PLATE_CAST_DESH.get()));
        add.accept(new ItemStack(ModItems.PLATE_CAST_STAR_METAL.get()));
        add.accept(new ItemStack(ModItems.PLATE_CAST_TCALLOY.get()));
        add.accept(new ItemStack(ModItems.PLATE_CAST_CDALLOY.get()));
        add.accept(new ItemStack(ModItems.PLATE_CAST_CMB.get()));
        add.accept(new ItemStack(ModItems.PLATE_CAST_SCHRABIDIUM.get()));
        add.accept(new ItemStack(ModItems.PLATE_CAST_BBRONZE.get()));
        add.accept(new ItemStack(ModItems.PLATE_CAST_ABRONZE.get()));
        add.accept(new ItemStack(ModItems.PLATE_CAST_SATURNITE.get()));
        add.accept(new ItemStack(ModItems.PLATE_WELDED_IRON.get()));
        add.accept(new ItemStack(ModItems.PLATE_WELDED_STEEL.get()));
        add.accept(new ItemStack(ModItems.PLATE_WELDED_COPPER.get()));
        add.accept(new ItemStack(ModItems.PLATE_WELDED_TITANIUM.get()));
        add.accept(new ItemStack(ModItems.PLATE_WELDED_ALUMINIUM.get()));
        add.accept(new ItemStack(ModItems.PLATE_WELDED_TUNGSTEN.get()));
        add.accept(new ItemStack(ModItems.PLATE_WELDED_ZIRCONIUM.get()));
        add.accept(new ItemStack(ModItems.PLATE_WELDED_OSMIRIDIUM.get()));
        add.accept(new ItemStack(ModItems.PLATE_WELDED_TCALLOY.get()));
        add.accept(new ItemStack(ModItems.PLATE_WELDED_CDALLOY.get()));
        add.accept(new ItemStack(ModItems.PLATE_WELDED_CMB.get()));
        add.accept(new ItemStack(ModItems.MOLD_BARREL_HEAVY.get()));
        add.accept(new ItemStack(ModItems.MOLD_BARREL_LIGHT.get()));
        add.accept(new ItemStack(ModItems.MOLD_BASE.get()));
        add.accept(new ItemStack(ModItems.MOLD_BILLET.get()));
        add.accept(new ItemStack(ModItems.MOLD_BLADE.get()));
        add.accept(new ItemStack(ModItems.MOLD_BLADES.get()));
        add.accept(new ItemStack(ModItems.MOLD_BLOCK.get()));
        add.accept(new ItemStack(ModItems.MOLD_C357.get()));
        add.accept(new ItemStack(ModItems.MOLD_CBUCKSHOT.get()));
        add.accept(new ItemStack(ModItems.MOLD_GEM.get()));
        add.accept(new ItemStack(ModItems.MOLD_GRIP.get()));
        add.accept(new ItemStack(ModItems.MOLD_HULL_BIG.get()));
        add.accept(new ItemStack(ModItems.MOLD_HULL_SMALL.get()));
        add.accept(new ItemStack(ModItems.MOLD_INGOT.get()));
        add.accept(new ItemStack(ModItems.MOLD_INGOTS.get()));
        add.accept(new ItemStack(ModItems.MOLD_MECHANISM.get()));
        add.accept(new ItemStack(ModItems.MOLD_MOGUS.get()));
        add.accept(new ItemStack(ModItems.MOLD_NUGGET.get()));
        add.accept(new ItemStack(ModItems.MOLD_PIPE.get()));
        add.accept(new ItemStack(ModItems.MOLD_PIPES.get()));
        add.accept(new ItemStack(ModItems.MOLD_PLATE.get()));
        add.accept(new ItemStack(ModItems.MOLD_PLATE_CAST.get()));
        add.accept(new ItemStack(ModItems.MOLD_PLATES.get()));
        add.accept(new ItemStack(ModItems.MOLD_PLATES_CAST.get()));
        add.accept(new ItemStack(ModItems.MOLD_RECEIVER_HEAVY.get()));
        add.accept(new ItemStack(ModItems.MOLD_RECEIVER_LIGHT.get()));
        add.accept(new ItemStack(ModItems.MOLD_SHELL.get()));
        add.accept(new ItemStack(ModItems.MOLD_STAMP.get()));
        add.accept(new ItemStack(ModItems.MOLD_STEEL_BASE.get()));
        add.accept(new ItemStack(ModItems.MOLD_STOCK.get()));
        add.accept(new ItemStack(ModItems.MOLD_WIRE.get()));
        add.accept(new ItemStack(ModItems.MOLD_WIRE_DENSE.get()));
        add.accept(new ItemStack(ModItems.MOLD_WIRES_DENSE.get()));
        add.accept(new ItemStack(ModItems.PART_LITHIUM.get()));
        add.accept(new ItemStack(ModItems.PART_BERYLLIUM.get()));
        add.accept(new ItemStack(ModItems.PART_CARBON.get()));
        add.accept(new ItemStack(ModItems.PART_COPPER.get()));
        add.accept(new ItemStack(ModItems.PART_PLUTONIUM.get()));
        add.accept(new ItemStack(ModBlocks.ZIRNOX.get()));
        add.accept(new ItemStack(ModBlocks.ARC_WELDER.get()));
        add.accept(new ItemStack(ModBlocks.SOLDERING_STATION.get()));
        // add.accept(new ItemStack(ModBlocks.MIXER.get()));
        add.accept(new ItemStack(ModBlocks.DERRICK.get()));
        add.accept(new ItemStack(ModBlocks.RBMK_CONSOLE.get()));
        add.accept(new ItemStack(ModBlocks.FLARE_STACK.get()));
        add.accept(new ItemStack(ModBlocks.PUMPJACK.get()));
        add.accept(new ItemStack(ModBlocks.RADAR.get()));
        add.accept(new ItemStack(ModBlocks.LARGE_RADAR.get()));
        add.accept(new ItemStack(ModBlocks.CRACKING_TOWER.get()));
        add.accept(new ItemStack(ModBlocks.FRACTION_TOWER.get()));
        add.accept(new ItemStack(ModBlocks.MINING_DRILL.get()));
        add.accept(new ItemStack(ModBlocks.FEL.get()));
        add.accept(new ItemStack(ModBlocks.SILEX.get()));
        add.accept(new ItemStack(ModBlocks.FLUID_TANK.get()));
        add.accept(new ItemStack(ModBlocks.MACHINE_BATTERY_SOCKET.get()));
        add.accept(new ItemStack(ModBlocks.INDUSTRIAL_BOILER.get()));
        add.accept(new ItemStack(ModBlocks.SOLAR_BOILER.get()));
        add.accept(new ItemStack(ModBlocks.SOLAR_MIRRORS.get()));
        add.accept(new ItemStack(ModBlocks.WATZ_POWERPLANT.get()));
        add.accept(new ItemStack(ModBlocks.HYDROTREATER.get()));
        add.accept(new ItemStack(ModBlocks.CATALYTIC_REFORMER.get()));
        add.accept(new ItemStack(ModBlocks.DEUTERIUM_TOWER.get()));
        add.accept(new ItemStack(ModBlocks.CHEMICAL_FACTORY.get()));
        add.accept(new ItemStack(ModBlocks.STEAM_TURBINE.get()));
        add.accept(new ItemStack(ModBlocks.LIQUEFACTOR.get()));
        add.accept(new ItemStack(ModBlocks.CORE_EMITTER.get()));
        add.accept(new ItemStack(ModBlocks.CORE_INJECTOR.get()));
        add.accept(new ItemStack(ModBlocks.CORE_RECEIVER.get()));
        add.accept(new ItemStack(ModBlocks.VACUUM_DISTILL.get()));
        add.accept(new ItemStack(ModBlocks.TURBOFAN.get()));
        add.accept(new ItemStack(ModBlocks.INDUSTRIAL_TURBINE.get()));
        add.accept(new ItemStack(ModBlocks.TURBINE.get()));
        add.accept(new ItemStack(ModBlocks.SUBSTATION.get()));
        add.accept(new ItemStack(ModBlocks.REFINERY.get()));
        add.accept(new ItemStack(ModBlocks.MACHINE_BATTERY.get()));
        add.accept(new ItemStack(ModBlocks.MACHINE_BATTERY_LITHIUM.get()));
        add.accept(new ItemStack(ModBlocks.MACHINE_BATTERY_SCHRABIDIUM.get()));
        add.accept(new ItemStack(ModBlocks.MACHINE_BATTERY_DINEUTRONIUM.get()));
        add.accept(new ItemStack(ModBlocks.CONVERTER_BLOCK.get()));
        add.accept(new ItemStack(ModBlocks.SWITCH.get()));
        add.accept(new ItemStack(ModBlocks.WIRE_COATED.get()));
        add.accept(new ItemStack(ModBlocks.GEIGER_COUNTER_BLOCK.get()));
        add.accept(new ItemStack(ModBlocks.DECON.get()));
        add.accept(new ItemStack(ModBlocks.EMP.get()));
        for (BlockAbsorber.EnumAbsorberTier tier : BlockAbsorber.EnumAbsorberTier.values()) {
            add.accept(BlockAbsorberItem.forTier(ModBlocks.RAD_ABSORBER.get(), tier));
        }

        // --- WIP Machines (3D OBJ models) ---
        add.accept(new ItemStack(ModBlocks.AMMO_PRESS.get()));
        add.accept(new ItemStack(ModBlocks.ANNIHILATOR.get()));
        add.accept(new ItemStack(ModBlocks.ARC_FURNACE.get()));
        add.accept(new ItemStack(ModBlocks.ASSEMBLY_FACTORY.get()));
        add.accept(new ItemStack(ModBlocks.AUTOSAW.get()));
        add.accept(new ItemStack(ModBlocks.BAT9000.get()));
        add.accept(new ItemStack(ModBlocks.BEAMLINE.get()));
        add.accept(new ItemStack(ModBlocks.BOILER.get()));
        add.accept(new ItemStack(ModBlocks.BOILER_FUSION.get()));
        add.accept(new ItemStack(ModBlocks.BREEDER_FUSION.get()));
        add.accept(new ItemStack(ModBlocks.CHIMNEY_BRICK.get()));
        add.accept(new ItemStack(ModBlocks.CHIMNEY_INDUSTRIAL.get()));
        add.accept(new ItemStack(ModBlocks.COKER.get()));
        add.accept(new ItemStack(ModBlocks.COLLECTOR.get()));
        add.accept(new ItemStack(ModBlocks.COMBINATION_OVEN.get()));
        add.accept(new ItemStack(ModBlocks.COMBUSTION_ENGINE.get()));
        add.accept(new ItemStack(ModBlocks.COMPRESSOR.get()));
        add.accept(new ItemStack(ModBlocks.CONDENSER_POWERED.get()));
        add.accept(new ItemStack(ModBlocks.CONVEYOR_PRESS.get()));
        add.accept(new ItemStack(ModBlocks.COUPLER.get()));
        add.accept(new ItemStack(ModBlocks.DETECTOR.get()));
        add.accept(new ItemStack(ModBlocks.DIESELGEN.get()));
        add.accept(new ItemStack(ModBlocks.DIPOLE.get()));
        add.accept(new ItemStack(ModBlocks.DRONE.get()));
        add.accept(new ItemStack(ModBlocks.ELECTRIC_HEATER.get()));
        add.accept(new ItemStack(ModBlocks.ELECTROLYSER.get()));
        add.accept(new ItemStack(ModBlocks.EPRESS.get()));
        add.accept(new ItemStack(ModBlocks.EXPOSURE_CHAMBER.get()));
        add.accept(new ItemStack(ModBlocks.FENSU.get()));
        add.accept(new ItemStack(ModBlocks.FENSU2.get()));
        add.accept(new ItemStack(ModBlocks.FIREBOX.get()));
        add.accept(new ItemStack(ModBlocks.FRACTION_SPACER.get()));
        add.accept(new ItemStack(ModBlocks.FURNACE_IRON.get()));
        add.accept(new ItemStack(ModBlocks.FURNACE_STEEL.get()));
        add.accept(new ItemStack(ModBlocks.HEATEX.get()));
        add.accept(new ItemStack(ModBlocks.HEPHAESTUS.get()));
        add.accept(new ItemStack(ModBlocks.ICF.get()));
        add.accept(new ItemStack(ModBlocks.INTAKE.get()));
        add.accept(new ItemStack(ModBlocks.KLYSTRON.get()));
        add.accept(new ItemStack(ModBlocks.MHDT.get()));
        add.accept(new ItemStack(ModBlocks.MICROWAVE.get()));
        add.accept(new ItemStack(ModBlocks.MINING_LASER.get()));
        add.accept(new ItemStack(ModBlocks.OILBURNER.get()));
        add.accept(new ItemStack(ModBlocks.OILBURNER_HP.get()));
        add.accept(new ItemStack(ModBlocks.ORBUS.get()));
        add.accept(new ItemStack(ModBlocks.ORE_SLOPPER.get()));
        add.accept(new ItemStack(ModBlocks.PLASMA_FORGE.get()));
        add.accept(new ItemStack(ModBlocks.PYROOVEN.get()));
        add.accept(new ItemStack(ModBlocks.QUADRUPOLE.get()));
        add.accept(new ItemStack(ModBlocks.RADGEN.get()));
        add.accept(new ItemStack(ModBlocks.RADIOLYSIS.get()));
        add.accept(new ItemStack(ModBlocks.REACTOR_SMALL.get()));
        add.accept(new ItemStack(ModBlocks.RFC.get()));
        add.accept(new ItemStack(ModBlocks.ROTARY_FURNACE.get()));
        add.accept(new ItemStack(ModBlocks.SAWMILL.get()));
        add.accept(new ItemStack(ModBlocks.SOLIDIFIER.get()));
        add.accept(new ItemStack(ModBlocks.SOURCE.get()));
        add.accept(new ItemStack(ModBlocks.STIRLING.get()));
        add.accept(new ItemStack(ModBlocks.STIRLING_CREATIVE.get()));
        add.accept(new ItemStack(ModBlocks.STIRLING_STEEL.get()));
        add.accept(new ItemStack(ModBlocks.STRAND_CASTER.get()));
        add.accept(new ItemStack(ModBlocks.TORUS.get()));
        add.accept(new ItemStack(ModBlocks.TURBINEGAS.get()));
        add.accept(new ItemStack(ModBlocks.WATZ_PUMP.get()));
        add.accept(new ItemStack(ModBlocks.CHUNGUS.get()));
    }

    /** Временная вкладка для новых, ещё не отсортированных предметов/блоков. */
    public static void populateDevItemsTab(BiConsumer<ItemStack, CreativeModeTab.TabVisibility> acceptor) {
        Consumer<ItemStack> add = stack -> acceptor.accept(stack, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
        add.accept(new ItemStack(ModBlocks.MACHINE_SIREN.get()));
        add.accept(new ItemStack(ModBlocks.BROADCASTER.get()));

        // --- WIP Machines (3D OBJ models) ---
        add.accept(new ItemStack(ModBlocks.AMMO_PRESS.get()));
        add.accept(new ItemStack(ModBlocks.ANNIHILATOR.get()));
        add.accept(new ItemStack(ModBlocks.ARC_FURNACE.get()));
        add.accept(new ItemStack(ModBlocks.ASSEMBLY_FACTORY.get()));
        add.accept(new ItemStack(ModBlocks.AUTOSAW.get()));
        add.accept(new ItemStack(ModBlocks.BAT9000.get()));
        add.accept(new ItemStack(ModBlocks.BEAMLINE.get()));
        add.accept(new ItemStack(ModBlocks.BOILER.get()));
        add.accept(new ItemStack(ModBlocks.BOILER_FUSION.get()));
        add.accept(new ItemStack(ModBlocks.BREEDER_FUSION.get()));
        add.accept(new ItemStack(ModBlocks.CHIMNEY_BRICK.get()));
        add.accept(new ItemStack(ModBlocks.CHIMNEY_INDUSTRIAL.get()));
        add.accept(new ItemStack(ModBlocks.COKER.get()));
        add.accept(new ItemStack(ModBlocks.COLLECTOR.get()));
        add.accept(new ItemStack(ModBlocks.COMBINATION_OVEN.get()));
        add.accept(new ItemStack(ModBlocks.COMBUSTION_ENGINE.get()));
        add.accept(new ItemStack(ModBlocks.COMPRESSOR.get()));
        add.accept(new ItemStack(ModBlocks.CONDENSER_POWERED.get()));
        add.accept(new ItemStack(ModBlocks.CONVEYOR_PRESS.get()));
        add.accept(new ItemStack(ModBlocks.COUPLER.get()));
        add.accept(new ItemStack(ModBlocks.DETECTOR.get()));
        add.accept(new ItemStack(ModBlocks.DIESELGEN.get()));
        add.accept(new ItemStack(ModBlocks.DIPOLE.get()));
        add.accept(new ItemStack(ModBlocks.DRONE.get()));
        add.accept(new ItemStack(ModBlocks.ELECTRIC_HEATER.get()));
        add.accept(new ItemStack(ModBlocks.ELECTROLYSER.get()));
        add.accept(new ItemStack(ModBlocks.EPRESS.get()));
        add.accept(new ItemStack(ModBlocks.EXPOSURE_CHAMBER.get()));
        add.accept(new ItemStack(ModBlocks.FENSU.get()));
        add.accept(new ItemStack(ModBlocks.FENSU2.get()));
        add.accept(new ItemStack(ModBlocks.FIREBOX.get()));
        add.accept(new ItemStack(ModBlocks.FRACTION_SPACER.get()));
        add.accept(new ItemStack(ModBlocks.FURNACE_IRON.get()));
        add.accept(new ItemStack(ModBlocks.FURNACE_STEEL.get()));
        add.accept(new ItemStack(ModBlocks.HEATEX.get()));
        add.accept(new ItemStack(ModBlocks.HEPHAESTUS.get()));
        add.accept(new ItemStack(ModBlocks.ICF.get()));
        add.accept(new ItemStack(ModBlocks.INTAKE.get()));
        add.accept(new ItemStack(ModBlocks.KLYSTRON.get()));
        add.accept(new ItemStack(ModBlocks.MHDT.get()));
        add.accept(new ItemStack(ModBlocks.MICROWAVE.get()));
        add.accept(new ItemStack(ModBlocks.MINING_LASER.get()));
        add.accept(new ItemStack(ModBlocks.OILBURNER.get()));
        add.accept(new ItemStack(ModBlocks.OILBURNER_HP.get()));
        add.accept(new ItemStack(ModBlocks.ORBUS.get()));
        add.accept(new ItemStack(ModBlocks.ORE_SLOPPER.get()));
        add.accept(new ItemStack(ModBlocks.PLASMA_FORGE.get()));
        add.accept(new ItemStack(ModBlocks.PYROOVEN.get()));
        add.accept(new ItemStack(ModBlocks.QUADRUPOLE.get()));
        add.accept(new ItemStack(ModBlocks.RADGEN.get()));
        add.accept(new ItemStack(ModBlocks.RADIOLYSIS.get()));
        add.accept(new ItemStack(ModBlocks.REACTOR_SMALL.get()));
        add.accept(new ItemStack(ModBlocks.RFC.get()));
        add.accept(new ItemStack(ModBlocks.ROTARY_FURNACE.get()));
        add.accept(new ItemStack(ModBlocks.SAWMILL.get()));
        add.accept(new ItemStack(ModBlocks.SOLIDIFIER.get()));
        add.accept(new ItemStack(ModBlocks.SOURCE.get()));
        add.accept(new ItemStack(ModBlocks.STIRLING.get()));
        add.accept(new ItemStack(ModBlocks.STIRLING_CREATIVE.get()));
        add.accept(new ItemStack(ModBlocks.STIRLING_STEEL.get()));
        add.accept(new ItemStack(ModBlocks.STRAND_CASTER.get()));
        add.accept(new ItemStack(ModBlocks.TORUS.get()));
        add.accept(new ItemStack(ModBlocks.TURBINEGAS.get()));
        add.accept(new ItemStack(ModBlocks.WATZ_PUMP.get()));
        add.accept(new ItemStack(ModBlocks.CHUNGUS.get()));

        // ══════════════════════════════════════════════════════════════════════
        // DEV: importierte fehlende Bloecke aus dem Original-HBM (zur Sichtung)
        // ══════════════════════════════════════════════════════════════════════
        add.accept(new ItemStack(ModBlocks.ANCIENT_SCRAP.get()));
        add.accept(new ItemStack(ModBlocks.ASH_DIGAMMA.get()));
        add.accept(new ItemStack(ModBlocks.ASPHALT_LIGHT.get()));
        add.accept(new ItemStack(ModBlocks.BARBED_WIRE_ACID.get()));
        add.accept(new ItemStack(ModBlocks.BARBED_WIRE_ULTRADEATH.get()));
        add.accept(new ItemStack(ModBlocks.BASALT.get()));
        add.accept(new ItemStack(ModBlocks.BASALT_SMOOTH.get()));
        add.accept(new ItemStack(ModBlocks.BASALT_TILES.get()));
        add.accept(new ItemStack(ModBlocks.BATTERY_LITHIUM_BLOCK.get()));
        add.accept(new ItemStack(ModBlocks.BATTERY_POTATO_BLOCK.get()));
        add.accept(new ItemStack(ModBlocks.BATTERY_SCHRABIDIUM_BLOCK.get()));
        add.accept(new ItemStack(ModBlocks.BLAST_DOOR.get()));
        add.accept(new ItemStack(ModBlocks.BLOCK_ALUMINIUM.get()));
        add.accept(new ItemStack(ModBlocks.BOXCAR.get()));
        add.accept(new ItemStack(ModBlocks.BRICK_ASBESTOS.get()));
        add.accept(new ItemStack(ModBlocks.BRICK_COMPOUND.get()));
        add.accept(new ItemStack(ModBlocks.BRICK_JUNGLE.get()));
        add.accept(new ItemStack(ModBlocks.BRICK_JUNGLE_CIRCLE.get()));
        add.accept(new ItemStack(ModBlocks.BRICK_JUNGLE_CRACKED.get()));
        add.accept(new ItemStack(ModBlocks.BRICK_JUNGLE_FRAGILE.get()));
        add.accept(new ItemStack(ModBlocks.BRICK_JUNGLE_GLYPH.get()));
        add.accept(new ItemStack(ModBlocks.BRICK_JUNGLE_LAVA.get()));
        add.accept(new ItemStack(ModBlocks.BRICK_JUNGLE_MYSTIC.get()));
        add.accept(new ItemStack(ModBlocks.BRICK_JUNGLE_OOZE.get()));
        add.accept(new ItemStack(ModBlocks.BRICK_JUNGLE_TRAP.get()));
        add.accept(new ItemStack(ModBlocks.BRICK_RED.get()));
        add.accept(new ItemStack(ModBlocks.BROADCASTER_PC.get()));
        add.accept(new ItemStack(ModBlocks.CABLE_DETECTOR.get()));
        add.accept(new ItemStack(ModBlocks.CABLE_DIODE.get()));
        add.accept(new ItemStack(ModBlocks.CABLE_SWITCH.get()));
        add.accept(new ItemStack(ModBlocks.CAPACITOR_BUS.get()));
        add.accept(new ItemStack(ModBlocks.CAPACITOR_COPPER.get()));
        add.accept(new ItemStack(ModBlocks.CAPACITOR_GOLD.get()));
        add.accept(new ItemStack(ModBlocks.CAPACITOR_NIOBIUM.get()));
        add.accept(new ItemStack(ModBlocks.CAPACITOR_SCHRABIDATE.get()));
        add.accept(new ItemStack(ModBlocks.CAPACITOR_TANTALIUM.get()));
        add.accept(new ItemStack(ModBlocks.CHARGE_C4.get()));
        add.accept(new ItemStack(ModBlocks.CHARGE_DYNAMITE.get()));
        add.accept(new ItemStack(ModBlocks.CHARGE_MINER.get()));
        add.accept(new ItemStack(ModBlocks.CHARGE_SEMTEX.get()));
        add.accept(new ItemStack(ModBlocks.CHLORINE_GAS.get()));
        add.accept(new ItemStack(ModBlocks.CLUSTER_ALUMINIUM.get()));
        add.accept(new ItemStack(ModBlocks.CLUSTER_COPPER.get()));
        add.accept(new ItemStack(ModBlocks.CLUSTER_DEPTH_IRON.get()));
        add.accept(new ItemStack(ModBlocks.CLUSTER_DEPTH_TITANIUM.get()));
        add.accept(new ItemStack(ModBlocks.CLUSTER_DEPTH_TUNGSTEN.get()));
        add.accept(new ItemStack(ModBlocks.CLUSTER_IRON.get()));
        add.accept(new ItemStack(ModBlocks.CLUSTER_TITANIUM.get()));
        add.accept(new ItemStack(ModBlocks.CM_FLUX.get()));
        add.accept(new ItemStack(ModBlocks.CM_HEAT.get()));
        add.accept(new ItemStack(ModBlocks.CMB_BRICK.get()));
        add.accept(new ItemStack(ModBlocks.CMB_BRICK_REINFORCED.get()));
        add.accept(new ItemStack(ModBlocks.COMPACT_LAUNCHER.get()));
        add.accept(new ItemStack(ModBlocks.CONCRETE_COLORED_EXT_BRONZE.get()));
        add.accept(new ItemStack(ModBlocks.CONCRETE_COLORED_EXT_HAZARD.get()));
        add.accept(new ItemStack(ModBlocks.CONCRETE_COLORED_EXT_INDIGO.get()));
        add.accept(new ItemStack(ModBlocks.CONCRETE_COLORED_EXT_MACHINE.get()));
        add.accept(new ItemStack(ModBlocks.CONCRETE_COLORED_EXT_MACHINE_STRIPE.get()));
        add.accept(new ItemStack(ModBlocks.CONCRETE_COLORED_EXT_PINK.get()));
        add.accept(new ItemStack(ModBlocks.CONCRETE_COLORED_EXT_PURPLE.get()));
        add.accept(new ItemStack(ModBlocks.CONCRETE_COLORED_EXT_SAND.get()));
        add.accept(new ItemStack(ModBlocks.CONVEYOR.get()));
        add.accept(new ItemStack(ModBlocks.CONVEYOR_DOUBLE.get()));
        add.accept(new ItemStack(ModBlocks.CONVEYOR_EXPRESS.get()));
        add.accept(new ItemStack(ModBlocks.CONVEYOR_TRIPLE.get()));
        add.accept(new ItemStack(ModBlocks.CRANE_BOXER.get()));
        add.accept(new ItemStack(ModBlocks.CRANE_GRABBER.get()));
        add.accept(new ItemStack(ModBlocks.CRANE_PARTITIONER.get()));
        add.accept(new ItemStack(ModBlocks.CRANE_ROUTER.get()));
        add.accept(new ItemStack(ModBlocks.CRANE_SPLITTER.get()));
        add.accept(new ItemStack(ModBlocks.CRANE_UNBOXER.get()));
        add.accept(new ItemStack(ModBlocks.CRATE_AMMO.get()));
        add.accept(new ItemStack(ModBlocks.CRATE_CAN.get()));
        add.accept(new ItemStack(ModBlocks.CRATE_JUNGLE.get()));
        add.accept(new ItemStack(ModBlocks.CRATE_RED.get()));
        add.accept(new ItemStack(ModBlocks.DECO_ALUMINIUM.get()));
        add.accept(new ItemStack(ModBlocks.DEPTH_DNT.get()));
        add.accept(new ItemStack(ModBlocks.DET_CHARGE.get()));
        add.accept(new ItemStack(ModBlocks.DET_CORD.get()));
        add.accept(new ItemStack(ModBlocks.DET_NUKE.get()));
        add.accept(new ItemStack(ModBlocks.DFC_CORE.get()));
        add.accept(new ItemStack(ModBlocks.DFC_EMITTER.get()));
        add.accept(new ItemStack(ModBlocks.DFC_INJECTOR.get()));
        add.accept(new ItemStack(ModBlocks.DFC_RECEIVER.get()));
        add.accept(new ItemStack(ModBlocks.DFC_STABILIZER.get()));
        add.accept(new ItemStack(ModBlocks.DIRT_DEAD.get()));
        add.accept(new ItemStack(ModBlocks.DIRT_OILY.get()));
        add.accept(new ItemStack(ModBlocks.DRONE_CRATE.get()));
        add.accept(new ItemStack(ModBlocks.DRONE_CRATE_PROVIDER.get()));
        add.accept(new ItemStack(ModBlocks.DRONE_CRATE_REQUESTER.get()));
        add.accept(new ItemStack(ModBlocks.DRONE_DOCK.get()));
        add.accept(new ItemStack(ModBlocks.DRONE_WAYPOINT.get()));
        add.accept(new ItemStack(ModBlocks.DRONE_WAYPOINT_REQUEST.get()));
        add.accept(new ItemStack(ModBlocks.DUCRETE.get()));
        add.accept(new ItemStack(ModBlocks.DYNAMITE.get()));
        add.accept(new ItemStack(ModBlocks.FACTORY_ADVANCED_HULL.get()));
        add.accept(new ItemStack(ModBlocks.FACTORY_TITANIUM_HULL.get()));
        add.accept(new ItemStack(ModBlocks.FENCE_METAL.get()));
        add.accept(new ItemStack(ModBlocks.FENCE_METAL_POST.get()));
        add.accept(new ItemStack(ModBlocks.FIELD_DISTURBER.get()));
        add.accept(new ItemStack(ModBlocks.FIRE_DIGAMMA.get()));
        add.accept(new ItemStack(ModBlocks.FIREWORKS.get()));
        add.accept(new ItemStack(ModBlocks.FISSURE_BOMB.get()));
        add.accept(new ItemStack(ModBlocks.FLAME_WAR.get()));
        add.accept(new ItemStack(ModBlocks.FLUID_COUNTER_VALVE.get()));
        add.accept(new ItemStack(ModBlocks.FLUID_DUCT_BOX.get()));
        add.accept(new ItemStack(ModBlocks.FLUID_DUCT_PAINTABLE.get()));
        add.accept(new ItemStack(ModBlocks.FLUID_SWITCH.get()));
        add.accept(new ItemStack(ModBlocks.FOUNDRY_MOLD.get()));
        add.accept(new ItemStack(ModBlocks.FOUNDRY_SLAGTAP.get()));
        add.accept(new ItemStack(ModBlocks.FOUNDRY_TANK.get()));
        add.accept(new ItemStack(ModBlocks.FROZEN_DIRT.get()));
        add.accept(new ItemStack(ModBlocks.FROZEN_GRASS.get()));
        add.accept(new ItemStack(ModBlocks.FROZEN_LOG.get()));
        add.accept(new ItemStack(ModBlocks.FROZEN_PLANKS.get()));
        add.accept(new ItemStack(ModBlocks.FUSION_COMPONENT.get()));
        add.accept(new ItemStack(ModBlocks.FUSION_COMPONENT_BLANKET.get()));
        add.accept(new ItemStack(ModBlocks.FUSION_COMPONENT_BSCCO_WELDED.get()));
        add.accept(new ItemStack(ModBlocks.FUSION_COMPONENT_MOTOR.get()));
        add.accept(new ItemStack(ModBlocks.FUSION_HATCH.get()));
        add.accept(new ItemStack(ModBlocks.FUSION_HEATER.get()));
        add.accept(new ItemStack(ModBlocks.GAS_ASBESTOS.get()));
        add.accept(new ItemStack(ModBlocks.GAS_COAL.get()));
        add.accept(new ItemStack(ModBlocks.GAS_EXPLOSIVE.get()));
        add.accept(new ItemStack(ModBlocks.GAS_FLAMMABLE.get()));
        add.accept(new ItemStack(ModBlocks.GAS_MELTDOWN.get()));
        add.accept(new ItemStack(ModBlocks.GAS_MONOXIDE.get()));
        add.accept(new ItemStack(ModBlocks.GAS_RADON.get()));
        add.accept(new ItemStack(ModBlocks.GAS_RADON_DENSE.get()));
        add.accept(new ItemStack(ModBlocks.GAS_RADON_TOMB.get()));
        add.accept(new ItemStack(ModBlocks.GLASS_ASH.get()));
        add.accept(new ItemStack(ModBlocks.GLASS_BORON.get()));
        add.accept(new ItemStack(ModBlocks.GLASS_LEAD.get()));
        add.accept(new ItemStack(ModBlocks.GLASS_POLARIZED.get()));
        add.accept(new ItemStack(ModBlocks.GLASS_POLONIUM.get()));
        add.accept(new ItemStack(ModBlocks.GLASS_QUARTZ.get()));
        add.accept(new ItemStack(ModBlocks.GLASS_TRINITITE.get()));
        add.accept(new ItemStack(ModBlocks.GLASS_URANIUM.get()));
        add.accept(new ItemStack(ModBlocks.GLYPHID_BASE.get()));
        add.accept(new ItemStack(ModBlocks.GRAVEL_DIAMOND.get()));
        add.accept(new ItemStack(ModBlocks.GRAVEL_OBSIDIAN.get()));
        add.accept(new ItemStack(ModBlocks.HADRON_COIL_ALLOY.get()));
        add.accept(new ItemStack(ModBlocks.HADRON_COIL_CHLOROPHYTE.get()));
        add.accept(new ItemStack(ModBlocks.HADRON_COIL_GOLD.get()));
        add.accept(new ItemStack(ModBlocks.HADRON_COIL_MAGTUNG.get()));
        add.accept(new ItemStack(ModBlocks.HADRON_COIL_MESE.get()));
        add.accept(new ItemStack(ModBlocks.HADRON_COIL_NEODYMIUM.get()));
        add.accept(new ItemStack(ModBlocks.HADRON_COIL_SCHRABIDATE.get()));
        add.accept(new ItemStack(ModBlocks.HADRON_COIL_SCHRABIDIUM.get()));
        add.accept(new ItemStack(ModBlocks.HADRON_COIL_STARMETAL.get()));
        add.accept(new ItemStack(ModBlocks.HEV_BATTERY.get()));
        add.accept(new ItemStack(ModBlocks.ICF_COMPONENT.get()));
        add.accept(new ItemStack(ModBlocks.ICF_COMPONENT_STRUCTURE.get()));
        add.accept(new ItemStack(ModBlocks.ICF_COMPONENT_STRUCTURE_BOLTED.get()));
        add.accept(new ItemStack(ModBlocks.ICF_COMPONENT_VESSEL.get()));
        add.accept(new ItemStack(ModBlocks.ICF_COMPONENT_VESSEL_WELDED.get()));
        add.accept(new ItemStack(ModBlocks.ICF_CONTROLLER.get()));
        add.accept(new ItemStack(ModBlocks.ITER.get()));
        add.accept(new ItemStack(ModBlocks.LADDER_ALUMINIUM.get()));
        add.accept(new ItemStack(ModBlocks.LADDER_COBALT.get()));
        add.accept(new ItemStack(ModBlocks.LADDER_COPPER.get()));
        add.accept(new ItemStack(ModBlocks.LADDER_GOLD.get()));
        add.accept(new ItemStack(ModBlocks.LADDER_IRON.get()));
        add.accept(new ItemStack(ModBlocks.LADDER_LEAD.get()));
        add.accept(new ItemStack(ModBlocks.LADDER_STEEL.get()));
        add.accept(new ItemStack(ModBlocks.LADDER_STURDY.get()));
        add.accept(new ItemStack(ModBlocks.LADDER_TITANIUM.get()));
        add.accept(new ItemStack(ModBlocks.LADDER_TUNGSTEN.get()));
        add.accept(new ItemStack(ModBlocks.LAMP_DEMON.get()));
        add.accept(new ItemStack(ModBlocks.LAMP_TRITIUM_BLUE_OFF.get()));
        add.accept(new ItemStack(ModBlocks.LAMP_TRITIUM_BLUE_ON.get()));
        add.accept(new ItemStack(ModBlocks.LAMP_TRITIUM_GREEN_OFF.get()));
        add.accept(new ItemStack(ModBlocks.LAMP_TRITIUM_GREEN_ON.get()));
        add.accept(new ItemStack(ModBlocks.LIGHTSTONE_BRICKS.get()));
        add.accept(new ItemStack(ModBlocks.LIGHTSTONE_BRICKS_CHISELED.get()));
        add.accept(new ItemStack(ModBlocks.LIGHTSTONE_CHISELED.get()));
        add.accept(new ItemStack(ModBlocks.LIGHTSTONE_TILE.get()));
        add.accept(new ItemStack(ModBlocks.LIGHTSTONE_UNREFINED.get()));
        add.accept(new ItemStack(ModBlocks.MACHINE_AUTOCRAFTER.get()));
        add.accept(new ItemStack(ModBlocks.MACHINE_BOILER.get()));
        add.accept(new ItemStack(ModBlocks.MACHINE_CENTRIFUGE.get()));
        add.accept(new ItemStack(ModBlocks.MACHINE_CHUNGUS.get()));
        add.accept(new ItemStack(ModBlocks.MACHINE_CONTROLLER.get()));
        add.accept(new ItemStack(ModBlocks.MACHINE_CONVERTER_HE_RF.get()));
        add.accept(new ItemStack(ModBlocks.MACHINE_CONVERTER_RF_HE.get()));
        add.accept(new ItemStack(ModBlocks.MACHINE_CRYSTALLIZER.get()));
        add.accept(new ItemStack(ModBlocks.MACHINE_DETECTOR.get()));
        add.accept(new ItemStack(ModBlocks.MACHINE_EPRESS.get()));
        add.accept(new ItemStack(ModBlocks.MACHINE_FENSU.get()));
        add.accept(new ItemStack(ModBlocks.MACHINE_FLUIDTANK.get()));
        add.accept(new ItemStack(ModBlocks.MACHINE_FORCEFIELD.get()));
        add.accept(new ItemStack(ModBlocks.MACHINE_FUNNEL.get()));
        add.accept(new ItemStack(ModBlocks.MACHINE_GASCENT.get()));
        add.accept(new ItemStack(ModBlocks.MACHINE_ICF_PRESS.get()));
        add.accept(new ItemStack(ModBlocks.MACHINE_KEYFORGE.get()));
        add.accept(new ItemStack(ModBlocks.MACHINE_LARGE_TURBINE.get()));
        add.accept(new ItemStack(ModBlocks.MACHINE_MICROWAVE.get()));
        add.accept(new ItemStack(ModBlocks.MACHINE_MINING_LASER.get()));
        add.accept(new ItemStack(ModBlocks.MACHINE_MISSILE_ASSEMBLY.get()));
        add.accept(new ItemStack(ModBlocks.MACHINE_PRESS.get()));
        add.accept(new ItemStack(ModBlocks.MACHINE_PUF6_TANK.get()));
        add.accept(new ItemStack(ModBlocks.MACHINE_RADAR.get()));
        add.accept(new ItemStack(ModBlocks.MACHINE_RADGEN.get()));
        add.accept(new ItemStack(ModBlocks.MACHINE_REACTOR.get()));
        add.accept(new ItemStack(ModBlocks.MACHINE_REACTOR_SMALL.get()));
        add.accept(new ItemStack(ModBlocks.MACHINE_REFINERY.get()));
        add.accept(new ItemStack(ModBlocks.MACHINE_SATLINKER.get()));
        add.accept(new ItemStack(ModBlocks.MACHINE_SOLAR_BOILER.get()));
        add.accept(new ItemStack(ModBlocks.MACHINE_STORAGE_DRUM.get()));
        add.accept(new ItemStack(ModBlocks.MACHINE_UF6_TANK.get()));
        add.accept(new ItemStack(ModBlocks.MASS_STORAGE.get()));
        add.accept(new ItemStack(ModBlocks.METEOR_SPAWNER.get()));
        add.accept(new ItemStack(ModBlocks.MINE_HE.get()));
        add.accept(new ItemStack(ModBlocks.MINE_NAVAL.get()));
        add.accept(new ItemStack(ModBlocks.MINE_SHRAP.get()));
        add.accept(new ItemStack(ModBlocks.MOON_TURF.get()));
        add.accept(new ItemStack(ModBlocks.MUSH.get()));
        add.accept(new ItemStack(ModBlocks.NUKE_FSTBMB.get()));
        add.accept(new ItemStack(ModBlocks.NUKE_N2.get()));
        add.accept(new ItemStack(ModBlocks.NUKE_SOLINIUM.get()));
        add.accept(new ItemStack(ModBlocks.OIL_SPILL.get()));
        add.accept(new ItemStack(ModBlocks.PEDESTAL.get()));
        add.accept(new ItemStack(ModBlocks.PINK_LOG.get()));
        add.accept(new ItemStack(ModBlocks.PINK_PLANKS.get()));
        add.accept(new ItemStack(ModBlocks.PLANT_FLOWER_CD0.get()));
        add.accept(new ItemStack(ModBlocks.PLANT_FLOWER_CD1.get()));
        add.accept(new ItemStack(ModBlocks.PLANT_FLOWER_FOXGLOVE.get()));
        add.accept(new ItemStack(ModBlocks.PLANT_FLOWER_NIGHTSHADE.get()));
        add.accept(new ItemStack(ModBlocks.PLANT_FLOWER_TOBACCO.get()));
        add.accept(new ItemStack(ModBlocks.PLANT_FLOWER_WEED.get()));
        add.accept(new ItemStack(ModBlocks.PLASMA_HEATER.get()));
        add.accept(new ItemStack(ModBlocks.PNEUMATIC_TUBE.get()));
        add.accept(new ItemStack(ModBlocks.PNEUMATIC_TUBE_PAINTABLE.get()));
        add.accept(new ItemStack(ModBlocks.PRESS_PREHEATER.get()));
        add.accept(new ItemStack(ModBlocks.PWR_BLOCK.get()));
        add.accept(new ItemStack(ModBlocks.PWR_CASING.get()));
        add.accept(new ItemStack(ModBlocks.PWR_CHANNEL.get()));
        add.accept(new ItemStack(ModBlocks.PWR_CONTROL.get()));
        add.accept(new ItemStack(ModBlocks.PWR_CONTROLLER.get()));
        add.accept(new ItemStack(ModBlocks.PWR_FUEL.get()));
        add.accept(new ItemStack(ModBlocks.PWR_HEATEX.get()));
        add.accept(new ItemStack(ModBlocks.PWR_HEATSINK.get()));
        add.accept(new ItemStack(ModBlocks.PWR_NEUTRON_SOURCE.get()));
        add.accept(new ItemStack(ModBlocks.PWR_PORT.get()));
        add.accept(new ItemStack(ModBlocks.PWR_REFLECTOR.get()));
        add.accept(new ItemStack(ModBlocks.RADIO_TELEX.get()));
        add.accept(new ItemStack(ModBlocks.RADIOBOX.get()));
        add.accept(new ItemStack(ModBlocks.RADIOREC.get()));
        add.accept(new ItemStack(ModBlocks.RAIL_BOOSTER.get()));
        add.accept(new ItemStack(ModBlocks.RAIL_HIGHSPEED.get()));
        add.accept(new ItemStack(ModBlocks.RAIL_NARROW.get()));
        add.accept(new ItemStack(ModBlocks.RAIL_WOOD.get()));
        add.accept(new ItemStack(ModBlocks.RED_CABLE.get()));
        add.accept(new ItemStack(ModBlocks.RED_CABLE_CLASSIC.get()));
        add.accept(new ItemStack(ModBlocks.RED_CONNECTOR.get()));
        add.accept(new ItemStack(ModBlocks.RED_PYLON.get()));
        add.accept(new ItemStack(ModBlocks.RED_PYLON_LARGE.get()));
        add.accept(new ItemStack(ModBlocks.RED_WIRE_COATED.get()));
        add.accept(new ItemStack(ModBlocks.REINFORCED_BRICK.get()));
        add.accept(new ItemStack(ModBlocks.REINFORCED_DUCRETE.get()));
        add.accept(new ItemStack(ModBlocks.REINFORCED_GLASS_PANE.get()));
        add.accept(new ItemStack(ModBlocks.REINFORCED_LAMINATE.get()));
        add.accept(new ItemStack(ModBlocks.REINFORCED_LAMINATE_PANE.get()));
        add.accept(new ItemStack(ModBlocks.REINFORCED_LAMP_OFF.get()));
        add.accept(new ItemStack(ModBlocks.REINFORCED_LAMP_ON.get()));
        add.accept(new ItemStack(ModBlocks.REINFORCED_LIGHT.get()));
        add.accept(new ItemStack(ModBlocks.REINFORCED_SAND.get()));
        add.accept(new ItemStack(ModBlocks.SAFE.get()));
        add.accept(new ItemStack(ModBlocks.SAND_BORON.get()));
        add.accept(new ItemStack(ModBlocks.SAND_DIRTY.get()));
        add.accept(new ItemStack(ModBlocks.SAND_DIRTY_RED.get()));
        add.accept(new ItemStack(ModBlocks.SAND_LEAD.get()));
        add.accept(new ItemStack(ModBlocks.SAND_POLONIUM.get()));
        add.accept(new ItemStack(ModBlocks.SAND_QUARTZ.get()));
        add.accept(new ItemStack(ModBlocks.SAND_URANIUM.get()));
        add.accept(new ItemStack(ModBlocks.SANDBAGS.get()));
        add.accept(new ItemStack(ModBlocks.SAT_DOCK.get()));
        add.accept(new ItemStack(ModBlocks.SAT_FOEQ.get()));
        add.accept(new ItemStack(ModBlocks.SAT_SCANNER.get()));
        add.accept(new ItemStack(ModBlocks.SEAL_CONTROLLER.get()));
        add.accept(new ItemStack(ModBlocks.SEAL_FRAME.get()));
        add.accept(new ItemStack(ModBlocks.SEAL_HATCH.get()));
        add.accept(new ItemStack(ModBlocks.SEMTEX.get()));
        add.accept(new ItemStack(ModBlocks.SOYUZ_CAPSULE.get()));
        add.accept(new ItemStack(ModBlocks.SOYUZ_LAUNCHER.get()));
        add.accept(new ItemStack(ModBlocks.DECO_SOYUZ_ROCKET.get()));
        add.accept(new ItemStack(ModBlocks.SPIKES.get()));
        add.accept(new ItemStack(ModBlocks.STALACTITE_ASBESTOS.get()));
        add.accept(new ItemStack(ModBlocks.STALACTITE_SULFUR.get()));
        add.accept(new ItemStack(ModBlocks.STALAGMITE_ASBESTOS.get()));
        add.accept(new ItemStack(ModBlocks.STALAGMITE_SULFUR.get()));
        add.accept(new ItemStack(ModBlocks.STEEL_ROOF.get()));
        add.accept(new ItemStack(ModBlocks.STEEL_SCAFFOLD.get()));
        add.accept(new ItemStack(ModBlocks.STONE_CRACKED.get()));
        add.accept(new ItemStack(ModBlocks.STONE_DEPTH.get()));
        add.accept(new ItemStack(ModBlocks.STONE_DEPTH_NETHER.get()));
        add.accept(new ItemStack(ModBlocks.STONE_GNEISS.get()));
        add.accept(new ItemStack(ModBlocks.STONE_KEYHOLE.get()));
        add.accept(new ItemStack(ModBlocks.STONE_KEYHOLE_META.get()));
        add.accept(new ItemStack(ModBlocks.STONE_POROUS.get()));
        add.accept(new ItemStack(ModBlocks.STONE_RESOURCE_ASBESTOS.get()));
        add.accept(new ItemStack(ModBlocks.STONE_RESOURCE_BAUXITE.get()));
        add.accept(new ItemStack(ModBlocks.STONE_RESOURCE_HEMATITE.get()));
        add.accept(new ItemStack(ModBlocks.STONE_RESOURCE_LIMESTONE.get()));
        add.accept(new ItemStack(ModBlocks.STONE_RESOURCE_MALACHITE.get()));
        add.accept(new ItemStack(ModBlocks.STONE_RESOURCE_SULFUR.get()));
        add.accept(new ItemStack(ModBlocks.STRUCT_ICF_CORE.get()));
        add.accept(new ItemStack(ModBlocks.STRUCT_LAUNCHER.get()));
        add.accept(new ItemStack(ModBlocks.STRUCT_LAUNCHER_CORE.get()));
        add.accept(new ItemStack(ModBlocks.STRUCT_LAUNCHER_CORE_LARGE.get()));
        add.accept(new ItemStack(ModBlocks.STRUCT_SCAFFOLD.get()));
        add.accept(new ItemStack(ModBlocks.STRUCT_SOYUZ_CORE.get()));
        add.accept(new ItemStack(ModBlocks.STRUCT_TORUS_CORE.get()));
        add.accept(new ItemStack(ModBlocks.STRUCT_WATZ_CORE.get()));
        add.accept(new ItemStack(ModBlocks.TEKTITE.get()));
        add.accept(new ItemStack(ModBlocks.TESLA.get()));
        add.accept(new ItemStack(ModBlocks.THERM_ENDO.get()));
        add.accept(new ItemStack(ModBlocks.THERM_EXO.get()));
        add.accept(new ItemStack(ModBlocks.TILE_LAB.get()));
        add.accept(new ItemStack(ModBlocks.TILE_LAB_BROKEN.get()));
        add.accept(new ItemStack(ModBlocks.TILE_LAB_CRACKED.get()));
        add.accept(new ItemStack(ModBlocks.TRAPDOOR_STEEL.get()));
        add.accept(new ItemStack(ModBlocks.VACUUM.get()));
        add.accept(new ItemStack(ModBlocks.VENT_CHLORINE.get()));
        add.accept(new ItemStack(ModBlocks.VENT_CHLORINE_SEAL.get()));
        add.accept(new ItemStack(ModBlocks.VENT_CLOUD.get()));
        add.accept(new ItemStack(ModBlocks.VENT_PINK_CLOUD.get()));
        add.accept(new ItemStack(ModBlocks.VINE_PHOSPHOR.get()));
        add.accept(new ItemStack(ModBlocks.VINYL_TILE_LARGE.get()));
        add.accept(new ItemStack(ModBlocks.VOLCANO_CORE.get()));
        add.accept(new ItemStack(ModBlocks.VOLCANO_RAD_CORE.get()));
        add.accept(new ItemStack(ModBlocks.WAND_AIR.get()));
        add.accept(new ItemStack(ModBlocks.WAND_JIGSAW.get()));
        add.accept(new ItemStack(ModBlocks.WAND_LOGIC.get()));
        add.accept(new ItemStack(ModBlocks.WAND_LOOT.get()));
        add.accept(new ItemStack(ModBlocks.WASTE_EARTH.get()));
        add.accept(new ItemStack(ModBlocks.WASTE_MYCELIUM.get()));
        add.accept(new ItemStack(ModBlocks.WASTE_TRINITITE.get()));
        add.accept(new ItemStack(ModBlocks.WASTE_TRINITITE_RED.get()));
        add.accept(new ItemStack(ModBlocks.WATZ_COOLER.get()));
        add.accept(new ItemStack(ModBlocks.WATZ_ELEMENT.get()));
        add.accept(new ItemStack(ModBlocks.WOOD_BARRIER.get()));
        // ══════════════════════════════════════════════════════════════════════
        // DEV: importierte fehlende Items aus dem Original-HBM (zur Sichtung)
        // ══════════════════════════════════════════════════════════════════════
        add.accept(new ItemStack(ModItems.ACETYLENE_TORCH.get()));
        add.accept(new ItemStack(ModItems.AJR_LEGS.get()));
        add.accept(new ItemStack(ModItems.AJR_PLATE.get()));
        add.accept(new ItemStack(ModItems.AJRO_LEGS.get()));
        add.accept(new ItemStack(ModItems.AJRO_PLATE.get()));
        add.accept(new ItemStack(ModItems.ALLOY_LEGS.get()));
        add.accept(new ItemStack(ModItems.ALLOY_PLATE.get()));
        add.accept(new ItemStack(ModItems.AMMO_ARTY.get()));
        add.accept(new ItemStack(ModItems.AMMO_ARTY_CARGO.get()));
        add.accept(new ItemStack(ModItems.AMMO_ARTY_CHLORINE.get()));
        add.accept(new ItemStack(ModItems.AMMO_ARTY_CLASSIC.get()));
        add.accept(new ItemStack(ModItems.AMMO_ARTY_HE.get()));
        add.accept(new ItemStack(ModItems.AMMO_ARTY_MINI_NUKE.get()));
        add.accept(new ItemStack(ModItems.AMMO_ARTY_MINI_NUKE_MULTI.get()));
        add.accept(new ItemStack(ModItems.AMMO_ARTY_MUSTARD_GAS.get()));
        add.accept(new ItemStack(ModItems.AMMO_ARTY_NUKE.get()));
        add.accept(new ItemStack(ModItems.AMMO_ARTY_PHOSGENE.get()));
        add.accept(new ItemStack(ModItems.AMMO_ARTY_PHOSPHORUS.get()));
        add.accept(new ItemStack(ModItems.AMMO_ARTY_PHOSPHORUS_MULTI.get()));
        add.accept(new ItemStack(ModItems.AMMO_BAG.get()));
        add.accept(new ItemStack(ModItems.AMMO_BAG_INFINITE.get()));
        add.accept(new ItemStack(ModItems.AMMO_CONTAINER.get()));
        add.accept(new ItemStack(ModItems.AMMO_DGK.get()));
        add.accept(new ItemStack(ModItems.AMMO_FIREEXT.get()));
        add.accept(new ItemStack(ModItems.AMMO_FIREEXT_FOAM.get()));
        add.accept(new ItemStack(ModItems.AMMO_FIREEXT_SAND.get()));
        add.accept(new ItemStack(ModItems.AMMO_SHELL.get()));
        add.accept(new ItemStack(ModItems.AMMO_SHELL_APFSDS_DU.get()));
        add.accept(new ItemStack(ModItems.AMMO_SHELL_APFSDS_T.get()));
        add.accept(new ItemStack(ModItems.AMMO_SHELL_EXPLOSIVE.get()));
        add.accept(new ItemStack(ModItems.AMMO_SHELL_W9.get()));
        add.accept(new ItemStack(ModItems.AMS_CATALYST_ALUMINIUM.get()));
        add.accept(new ItemStack(ModItems.AMS_CATALYST_BERYLLIUM.get()));
        add.accept(new ItemStack(ModItems.AMS_CATALYST_BLANK.get()));
        add.accept(new ItemStack(ModItems.AMS_CATALYST_CAESIUM.get()));
        add.accept(new ItemStack(ModItems.AMS_CATALYST_CERIUM.get()));
        add.accept(new ItemStack(ModItems.AMS_CATALYST_COBALT.get()));
        add.accept(new ItemStack(ModItems.AMS_CATALYST_COPPER.get()));
        add.accept(new ItemStack(ModItems.AMS_CATALYST_DINEUTRONIUM.get()));
        add.accept(new ItemStack(ModItems.AMS_CATALYST_EUPHEMIUM.get()));
        add.accept(new ItemStack(ModItems.AMS_CATALYST_IRON.get()));
        add.accept(new ItemStack(ModItems.AMS_CATALYST_LITHIUM.get()));
        add.accept(new ItemStack(ModItems.AMS_CATALYST_NIOBIUM.get()));
        add.accept(new ItemStack(ModItems.AMS_CATALYST_SCHRABIDIUM.get()));
        add.accept(new ItemStack(ModItems.AMS_CATALYST_STRONTIUM.get()));
        add.accept(new ItemStack(ModItems.AMS_CATALYST_THORIUM.get()));
        add.accept(new ItemStack(ModItems.AMS_CATALYST_TUNGSTEN.get()));
        add.accept(new ItemStack(ModItems.AMS_CORE_EYEOFHARMONY.get()));
        add.accept(new ItemStack(ModItems.AMS_CORE_SING.get()));
        add.accept(new ItemStack(ModItems.AMS_CORE_THINGY.get()));
        add.accept(new ItemStack(ModItems.AMS_CORE_WORMHOLE.get()));
        add.accept(new ItemStack(ModItems.AMS_LENS.get()));
        add.accept(new ItemStack(ModItems.ANALYSIS_TOOL.get()));
        add.accept(new ItemStack(ModItems.ANALYZER.get()));
        add.accept(new ItemStack(ModItems.ANCHOR_REMOTE.get()));
        add.accept(new ItemStack(ModItems.APPLE_EUPHEMIUM.get()));
        add.accept(new ItemStack(ModItems.APPLE_LEAD.get()));
        add.accept(new ItemStack(ModItems.APPLE_SCHRABIDIUM.get()));
        add.accept(new ItemStack(ModItems.ARC_ELECTRODE.get()));
        add.accept(new ItemStack(ModItems.ARMOR_POLISH.get()));
        add.accept(new ItemStack(ModItems.ASBESTOS_LEGS.get()));
        add.accept(new ItemStack(ModItems.ASBESTOS_PLATE.get()));
        add.accept(new ItemStack(ModItems.ASHGLASSES.get()));
        add.accept(new ItemStack(ModItems.ASSEMBLY_NUKE.get()));
        add.accept(new ItemStack(ModItems.ATTACHMENT_MASK.get()));
        add.accept(new ItemStack(ModItems.ATTACHMENT_MASK_MONO.get()));
        add.accept(new ItemStack(ModItems.AUSTRALIUM_III.get()));
        add.accept(new ItemStack(ModItems.BACK_TESLA.get()));
        add.accept(new ItemStack(ModItems.BALEFIRE_AND_HAM.get()));
        add.accept(new ItemStack(ModItems.BALEFIRE_AND_STEEL.get()));
        add.accept(new ItemStack(ModItems.BALEFIRE_SCRAMBLED.get()));
        add.accept(new ItemStack(ModItems.BALL_DYNAMITE.get()));
        add.accept(new ItemStack(ModItems.BALL_FIRECLAY.get()));
        add.accept(new ItemStack(ModItems.BALL_RESIN.get()));
        add.accept(new ItemStack(ModItems.BALL_TATB.get()));
        add.accept(new ItemStack(ModItems.BALLISTIC_GAUNTLET.get()));
        add.accept(new ItemStack(ModItems.BALLISTITE.get()));
        add.accept(new ItemStack(ModItems.BANDAID.get()));
        add.accept(new ItemStack(ModItems.BATHWATER.get()));
        add.accept(new ItemStack(ModItems.BATHWATER_MK2.get()));
        add.accept(new ItemStack(ModItems.BDCL.get()));
        add.accept(new ItemStack(ModItems.BEDROCK_ORE_FRAGMENT.get()));
        add.accept(new ItemStack(ModItems.BETA.get()));
        add.accept(new ItemStack(ModItems.BIG_SWORD.get()));
        add.accept(new ItemStack(ModItems.BILLET_ACTINIUM.get()));
        add.accept(new ItemStack(ModItems.BILLET_AM241.get()));
        add.accept(new ItemStack(ModItems.BILLET_AM242.get()));
        add.accept(new ItemStack(ModItems.BILLET_AM_MIX.get()));
        add.accept(new ItemStack(ModItems.BILLET_AMERICIUM_FUEL.get()));
        add.accept(new ItemStack(ModItems.BILLET_AU198.get()));
        add.accept(new ItemStack(ModItems.BILLET_AUSTRALIUM.get()));
        add.accept(new ItemStack(ModItems.BILLET_AUSTRALIUM_GREATER.get()));
        add.accept(new ItemStack(ModItems.BILLET_AUSTRALIUM_LESSER.get()));
        add.accept(new ItemStack(ModItems.BILLET_BALEFIRE_GOLD.get()));
        add.accept(new ItemStack(ModItems.BILLET_BERYLLIUM.get()));
        add.accept(new ItemStack(ModItems.BILLET_BISMUTH.get()));
        add.accept(new ItemStack(ModItems.BILLET_CO60.get()));
        add.accept(new ItemStack(ModItems.BILLET_COBALT.get()));
        add.accept(new ItemStack(ModItems.BILLET_FLASHLEAD.get()));
        add.accept(new ItemStack(ModItems.BILLET_GH336.get()));
        add.accept(new ItemStack(ModItems.BILLET_HES.get()));
        add.accept(new ItemStack(ModItems.BILLET_LES.get()));
        add.accept(new ItemStack(ModItems.BILLET_MOX_FUEL.get()));
        add.accept(new ItemStack(ModItems.BILLET_NEPTUNIUM.get()));
        add.accept(new ItemStack(ModItems.BILLET_NEPTUNIUM_FUEL.get()));
        add.accept(new ItemStack(ModItems.BILLET_NUCLEAR_WASTE.get()));
        add.accept(new ItemStack(ModItems.BILLET_PB209.get()));
        add.accept(new ItemStack(ModItems.BILLET_PLUTONIUM_FUEL.get()));
        add.accept(new ItemStack(ModItems.BILLET_PO210BE.get()));
        add.accept(new ItemStack(ModItems.BILLET_POLONIUM.get()));
        add.accept(new ItemStack(ModItems.BILLET_PU238.get()));
        add.accept(new ItemStack(ModItems.BILLET_PU238BE.get()));
        add.accept(new ItemStack(ModItems.BILLET_PU239.get()));
        add.accept(new ItemStack(ModItems.BILLET_PU240.get()));
        add.accept(new ItemStack(ModItems.BILLET_PU241.get()));
        add.accept(new ItemStack(ModItems.BILLET_PU_MIX.get()));
        add.accept(new ItemStack(ModItems.BILLET_RA226.get()));
        add.accept(new ItemStack(ModItems.BILLET_RA226BE.get()));
        add.accept(new ItemStack(ModItems.BILLET_SCHRABIDIUM.get()));
        add.accept(new ItemStack(ModItems.BILLET_SCHRABIDIUM_FUEL.get()));
        add.accept(new ItemStack(ModItems.BILLET_SOLINIUM.get()));
        add.accept(new ItemStack(ModItems.BILLET_SR90.get()));
        add.accept(new ItemStack(ModItems.BILLET_TECHNETIUM.get()));
        add.accept(new ItemStack(ModItems.BILLET_TH232.get()));
        add.accept(new ItemStack(ModItems.BILLET_THORIUM_FUEL.get()));
        add.accept(new ItemStack(ModItems.BILLET_U233.get()));
        add.accept(new ItemStack(ModItems.BILLET_U235.get()));
        add.accept(new ItemStack(ModItems.BILLET_U238.get()));
        add.accept(new ItemStack(ModItems.BILLET_URANIUM.get()));
        add.accept(new ItemStack(ModItems.BILLET_URANIUM_FUEL.get()));
        add.accept(new ItemStack(ModItems.BILLET_UZH.get()));
        add.accept(new ItemStack(ModItems.BILLET_YHARONITE.get()));
        add.accept(new ItemStack(ModItems.BILLET_ZFB_AM_MIX.get()));
        add.accept(new ItemStack(ModItems.BILLET_ZFB_BISMUTH.get()));
        add.accept(new ItemStack(ModItems.BILLET_ZFB_PU241.get()));
        add.accept(new ItemStack(ModItems.BILLET_ZIRCONIUM.get()));
        add.accept(new ItemStack(ModItems.BIO_WAFER.get()));
        add.accept(new ItemStack(ModItems.BIOMASS.get()));
        add.accept(new ItemStack(ModItems.BIOMASS_COMPRESSED.get()));
        add.accept(new ItemStack(ModItems.BISMUTH_AXE.get()));
        add.accept(new ItemStack(ModItems.BISMUTH_LEGS.get()));
        add.accept(new ItemStack(ModItems.BISMUTH_PICKAXE.get()));
        add.accept(new ItemStack(ModItems.BISMUTH_PLATE.get()));
        add.accept(new ItemStack(ModItems.BISMUTH_TOOL.get()));
        add.accept(new ItemStack(ModItems.BJ_BOOTS.get()));
        add.accept(new ItemStack(ModItems.BJ_HELMET.get()));
        add.accept(new ItemStack(ModItems.BJ_LEGS.get()));
        add.accept(new ItemStack(ModItems.BJ_PLATE.get()));
        add.accept(new ItemStack(ModItems.BJ_PLATE_JETPACK.get()));
        add.accept(new ItemStack(ModItems.BLADE_METEORITE.get()));
        add.accept(new ItemStack(ModItems.BLADE_TUNGSTEN.get()));
        add.accept(new ItemStack(ModItems.BLADES_ADVANCED_ALLOY.get()));
        add.accept(new ItemStack(ModItems.BLADES_DESH.get()));
        add.accept(new ItemStack(ModItems.BLADES_STEEL.get()));
        add.accept(new ItemStack(ModItems.BLADES_TITANIUM.get()));
        add.accept(new ItemStack(ModItems.BLOWTORCH.get()));
        add.accept(new ItemStack(ModItems.BLUEPRINTS.get()));
        add.accept(new ItemStack(ModItems.BOARD_COPPER.get()));
        add.accept(new ItemStack(ModItems.BOAT_RUBBER.get()));
        add.accept(new ItemStack(ModItems.BOBMAZON.get()));
        add.accept(new ItemStack(ModItems.BOLT_SPIKE.get()));
        add.accept(new ItemStack(ModItems.BOLTGUN.get()));
        add.accept(new ItemStack(ModItems.BOMB_CALLER.get()));
        add.accept(new ItemStack(ModItems.BOMB_WAFFLE.get()));
        add.accept(new ItemStack(ModItems.BOOK_GUIDE.get()));
        add.accept(new ItemStack(ModItems.BOOK_LEMEGETON.get()));
        add.accept(new ItemStack(ModItems.BOOK_OF_.get()));
        add.accept(new ItemStack(ModItems.BOOK_SECRET.get()));
        add.accept(new ItemStack(ModItems.BOTTLE2_EMPTY.get()));
        add.accept(new ItemStack(ModItems.BOTTLE2_FRITZ.get()));
        add.accept(new ItemStack(ModItems.BOTTLE2_KORL.get()));
        add.accept(new ItemStack(ModItems.BOTTLE2_SUNSET.get()));
        add.accept(new ItemStack(ModItems.BOTTLE_CHERRY.get()));
        add.accept(new ItemStack(ModItems.BOTTLE_EMPTY.get()));
        add.accept(new ItemStack(ModItems.BOTTLE_MERCURY.get()));
        add.accept(new ItemStack(ModItems.BOTTLE_NUKA.get()));
        add.accept(new ItemStack(ModItems.BOTTLE_OPENER.get()));
        add.accept(new ItemStack(ModItems.BOTTLE_QUANTUM.get()));
        add.accept(new ItemStack(ModItems.BOTTLE_RAD.get()));
        add.accept(new ItemStack(ModItems.BOTTLE_SPARKLE.get()));
        add.accept(new ItemStack(ModItems.BOTTLED_CLOUD.get()));
        add.accept(new ItemStack(ModItems.BOY_BULLET.get()));
        add.accept(new ItemStack(ModItems.BOY_IGNITER.get()));
        add.accept(new ItemStack(ModItems.BOY_KIT.get()));
        add.accept(new ItemStack(ModItems.BOY_PROPELLANT.get()));
        add.accept(new ItemStack(ModItems.BOY_SHIELDING.get()));
        add.accept(new ItemStack(ModItems.BOY_TARGET.get()));
        add.accept(new ItemStack(ModItems.BROKEN_ITEM.get()));
        add.accept(new ItemStack(ModItems.BUCKET_ACID.get()));
        add.accept(new ItemStack(ModItems.BUCKET_MUD.get()));
        add.accept(new ItemStack(ModItems.BUCKET_SCHRABIDIC_ACID.get()));
        add.accept(new ItemStack(ModItems.BUCKET_SULFURIC_ACID.get()));
        add.accept(new ItemStack(ModItems.BUCKET_TOXIC.get()));
        add.accept(new ItemStack(ModItems.BURNT_BARK.get()));
        add.accept(new ItemStack(ModItems.CANISTER_EMPTY.get()));
        add.accept(new ItemStack(ModItems.CANISTER_NAPALM.get()));
        add.accept(new ItemStack(ModItems.CANNED_SLIME.get()));
        add.accept(new ItemStack(ModItems.CANTEEN_VODKA.get()));
        add.accept(new ItemStack(ModItems.CAP_FRITZ.get()));
        add.accept(new ItemStack(ModItems.CAP_KORL.get()));
        add.accept(new ItemStack(ModItems.CAP_NUKA.get()));
        add.accept(new ItemStack(ModItems.CAP_QUANTUM.get()));
        add.accept(new ItemStack(ModItems.CAP_RAD.get()));
        add.accept(new ItemStack(ModItems.CAP_SPARKLE.get()));
        add.accept(new ItemStack(ModItems.CAP_STAR.get()));
        add.accept(new ItemStack(ModItems.CAP_SUNSET.get()));
        add.accept(new ItemStack(ModItems.CAPE_GASMASK.get()));
        add.accept(new ItemStack(ModItems.CAPE_RADIATION.get()));
        add.accept(new ItemStack(ModItems.CAPE_SCHRABIDIUM.get()));
        add.accept(new ItemStack(ModItems.CARD_AOS.get()));
        add.accept(new ItemStack(ModItems.CARD_QOS.get()));
        add.accept(new ItemStack(ModItems.CASING_BAG.get()));
        add.accept(new ItemStack(ModItems.CATALYST_CLAY.get()));
        add.accept(new ItemStack(ModItems.CATALYTIC_CONVERTER.get()));
        add.accept(new ItemStack(ModItems.CBT_DEVICE.get()));
        add.accept(new ItemStack(ModItems.CELL_ANTI_SCHRABIDIUM.get()));
        add.accept(new ItemStack(ModItems.CELL_ANTIMATTER.get()));
        add.accept(new ItemStack(ModItems.CELL_BALEFIRE.get()));
        add.accept(new ItemStack(ModItems.CELL_DEUTERIUM.get()));
        add.accept(new ItemStack(ModItems.CELL_EMPTY.get()));
        add.accept(new ItemStack(ModItems.CELL_PUF6.get()));
        add.accept(new ItemStack(ModItems.CELL_TRITIUM.get()));
        add.accept(new ItemStack(ModItems.CELL_UF6.get()));
        add.accept(new ItemStack(ModItems.CENTRI_STICK.get()));
        add.accept(new ItemStack(ModItems.CHAINSAW.get()));
        add.accept(new ItemStack(ModItems.CHEESE.get()));
        add.accept(new ItemStack(ModItems.CHEMISTRY_SET.get()));
        add.accept(new ItemStack(ModItems.CHEMISTRY_SET_BORON.get()));
        add.accept(new ItemStack(ModItems.CHERNOBYLSIGN.get()));
        add.accept(new ItemStack(ModItems.CHLORINE_PINWHEEL.get()));
        add.accept(new ItemStack(ModItems.CHLOROPHYTE_AXE.get()));
        add.accept(new ItemStack(ModItems.CHLOROPHYTE_PICKAXE.get()));
        add.accept(new ItemStack(ModItems.CHOCOLATE.get()));
        add.accept(new ItemStack(ModItems.CHOCOLATE_MILK.get()));
        add.accept(new ItemStack(ModItems.CHOPPER.get()));
        add.accept(new ItemStack(ModItems.CHOPPER_BLADES.get()));
        add.accept(new ItemStack(ModItems.CHOPPER_GUN.get()));
        add.accept(new ItemStack(ModItems.CHOPPER_HEAD.get()));
        add.accept(new ItemStack(ModItems.CHOPPER_TAIL.get()));
        add.accept(new ItemStack(ModItems.CHOPPER_TORSO.get()));
        add.accept(new ItemStack(ModItems.CHOPPER_WING.get()));
        add.accept(new ItemStack(ModItems.CIGARETTE.get()));
        add.accept(new ItemStack(ModItems.CINNEBAR.get()));
        add.accept(new ItemStack(ModItems.CIRCUIT_STAR.get()));
        add.accept(new ItemStack(ModItems.CLAY_TABLET.get()));
        add.accept(new ItemStack(ModItems.CMB_AXE.get()));
        add.accept(new ItemStack(ModItems.CMB_BOOTS.get()));
        add.accept(new ItemStack(ModItems.CMB_HELMET.get()));
        add.accept(new ItemStack(ModItems.CMB_HOE.get()));
        add.accept(new ItemStack(ModItems.CMB_LEGS.get()));
        add.accept(new ItemStack(ModItems.CMB_PLATE.get()));
        add.accept(new ItemStack(ModItems.CMB_SHOVEL.get()));
        add.accept(new ItemStack(ModItems.CMB_SWORD.get()));
        add.accept(new ItemStack(ModItems.COAL_INFERNAL.get()));
        add.accept(new ItemStack(ModItems.COBALT_AXE.get()));
        add.accept(new ItemStack(ModItems.COBALT_DECORATED_AXE.get()));
        add.accept(new ItemStack(ModItems.COBALT_DECORATED_HOE.get()));
        add.accept(new ItemStack(ModItems.COBALT_DECORATED_PICKAXE.get()));
        add.accept(new ItemStack(ModItems.COBALT_DECORATED_SHOVEL.get()));
        add.accept(new ItemStack(ModItems.COBALT_DECORATED_SWORD.get()));
        add.accept(new ItemStack(ModItems.COBALT_HOE.get()));
        add.accept(new ItemStack(ModItems.COBALT_LEGS.get()));
        add.accept(new ItemStack(ModItems.COBALT_PICKAXE.get()));
        add.accept(new ItemStack(ModItems.COBALT_PLATE.get()));
        add.accept(new ItemStack(ModItems.COBALT_SHOVEL.get()));
        add.accept(new ItemStack(ModItems.COBALT_SWORD.get()));
        add.accept(new ItemStack(ModItems.COFFEE.get()));
        add.accept(new ItemStack(ModItems.COFFEE_RADIUM.get()));
        add.accept(new ItemStack(ModItems.COIN_CREEPER.get()));
        add.accept(new ItemStack(ModItems.COIN_MASKMAN.get()));
        add.accept(new ItemStack(ModItems.COIN_RADIATION.get()));
        add.accept(new ItemStack(ModItems.COIN_TOKEN.get()));
        add.accept(new ItemStack(ModItems.COIN_UFO.get()));
        add.accept(new ItemStack(ModItems.COIN_WORM.get()));
        add.accept(new ItemStack(ModItems.COMBINE_SCRAP.get()));
        add.accept(new ItemStack(ModItems.COMPONENT_EMITTER.get()));
        add.accept(new ItemStack(ModItems.COMPONENT_LIMITER.get()));
        add.accept(new ItemStack(ModItems.CONTAINMENT_BOX.get()));
        add.accept(new ItemStack(ModItems.CORDITE.get()));
        add.accept(new ItemStack(ModItems.COTTON_CANDY.get()));
        add.accept(new ItemStack(ModItems.CRACKPIPE.get()));
        add.accept(new ItemStack(ModItems.CRATE_CALLER.get()));
        add.accept(new ItemStack(ModItems.CRUCIBLE_TEMPLATE.get()));
        add.accept(new ItemStack(ModItems.CUBE_POWER.get()));
        add.accept(new ItemStack(ModItems.CUSTOM_AMAT.get()));
        add.accept(new ItemStack(ModItems.CUSTOM_DIRTY.get()));
        add.accept(new ItemStack(ModItems.CUSTOM_FALL.get()));
        add.accept(new ItemStack(ModItems.CUSTOM_HYDRO.get()));
        add.accept(new ItemStack(ModItems.CUSTOM_KIT.get()));
        add.accept(new ItemStack(ModItems.CUSTOM_NUKE.get()));
        add.accept(new ItemStack(ModItems.CUSTOM_SCHRAB.get()));
        add.accept(new ItemStack(ModItems.CUSTOM_TNT.get()));
        add.accept(new ItemStack(ModItems.DEBRIS_CONCRETE.get()));
        add.accept(new ItemStack(ModItems.DEBRIS_ELEMENT.get()));
        add.accept(new ItemStack(ModItems.DEBRIS_EXCHANGER.get()));
        add.accept(new ItemStack(ModItems.DEBRIS_FUEL.get()));
        add.accept(new ItemStack(ModItems.DEBRIS_GRAPHITE.get()));
        add.accept(new ItemStack(ModItems.DEBRIS_METAL.get()));
        add.accept(new ItemStack(ModItems.DEBRIS_SHRAPNEL.get()));
        add.accept(new ItemStack(ModItems.DEFINITELYFOOD.get()));
        add.accept(new ItemStack(ModItems.DEFUSER_GOLD.get()));
        add.accept(new ItemStack(ModItems.DEMON_CORE_CLOSED.get()));
        add.accept(new ItemStack(ModItems.DEMON_CORE_OPEN.get()));
        add.accept(new ItemStack(ModItems.DESH_AXE.get()));
        add.accept(new ItemStack(ModItems.DESH_HOE.get()));
        add.accept(new ItemStack(ModItems.DESH_PICKAXE.get()));
        add.accept(new ItemStack(ModItems.DESH_SHOVEL.get()));
        add.accept(new ItemStack(ModItems.DESH_SWORD.get()));
        add.accept(new ItemStack(ModItems.DESIGNATOR_ARTY_RANGE.get()));
        add.accept(new ItemStack(ModItems.DETONATOR_DE.get()));
        add.accept(new ItemStack(ModItems.DETONATOR_DEADMAN.get()));
        add.accept(new ItemStack(ModItems.DETONATOR_LASER.get()));
        add.accept(new ItemStack(ModItems.DETONATOR_MULTI.get()));
        add.accept(new ItemStack(ModItems.DEUTERIUM_FILTER.get()));
        add.accept(new ItemStack(ModItems.DIAMOND_GAVEL.get()));
        add.accept(new ItemStack(ModItems.DIESELSUIT_BOOTS.get()));
        add.accept(new ItemStack(ModItems.DIESELSUIT_HELMET.get()));
        add.accept(new ItemStack(ModItems.DIESELSUIT_LEGS.get()));
        add.accept(new ItemStack(ModItems.DIESELSUIT_PLATE.get()));
        add.accept(new ItemStack(ModItems.DISPERSER_CANISTER.get()));
        add.accept(new ItemStack(ModItems.DNS_BOOTS.get()));
        add.accept(new ItemStack(ModItems.DNS_HELMET.get()));
        add.accept(new ItemStack(ModItems.DNS_LEGS.get()));
        add.accept(new ItemStack(ModItems.DNS_PLATE.get()));
        add.accept(new ItemStack(ModItems.DNT_LEGS.get()));
        add.accept(new ItemStack(ModItems.DNT_PLATE.get()));
        add.accept(new ItemStack(ModItems.DNT_SWORD.get()));
        add.accept(new ItemStack(ModItems.DOOR_METAL.get()));
        add.accept(new ItemStack(ModItems.DOOR_RED.get()));
        add.accept(new ItemStack(ModItems.DRAX.get()));
        add.accept(new ItemStack(ModItems.DRAX_MK2.get()));
        add.accept(new ItemStack(ModItems.DRAX_MK3.get()));
        add.accept(new ItemStack(ModItems.DRILLBIT_DESH.get()));
        add.accept(new ItemStack(ModItems.DRILLBIT_DESH_DIAMOND.get()));
        add.accept(new ItemStack(ModItems.DRILLBIT_FERRO.get()));
        add.accept(new ItemStack(ModItems.DRILLBIT_FERRO_DIAMOND.get()));
        add.accept(new ItemStack(ModItems.DRILLBIT_HSS.get()));
        add.accept(new ItemStack(ModItems.DRILLBIT_HSS_DIAMOND.get()));
        add.accept(new ItemStack(ModItems.DRILLBIT_STEEL.get()));
        add.accept(new ItemStack(ModItems.DRILLBIT_STEEL_DIAMOND.get()));
        add.accept(new ItemStack(ModItems.DRILLBIT_TCALLOY.get()));
        add.accept(new ItemStack(ModItems.DRILLBIT_TCALLOY_DIAMOND.get()));
        add.accept(new ItemStack(ModItems.DRONE_LINKER.get()));
        add.accept(new ItemStack(ModItems.DWARVEN_PICKAXE.get()));
        add.accept(new ItemStack(ModItems.DYSFUNCTIONAL_REACTOR.get()));
        add.accept(new ItemStack(ModItems.EGG_BALEFIRE.get()));
        add.accept(new ItemStack(ModItems.EGG_BALEFIRE_SHARD.get()));
        add.accept(new ItemStack(ModItems.EGG_GLYPHID.get()));
        add.accept(new ItemStack(ModItems.ELEC_SHOVEL.get()));
        add.accept(new ItemStack(ModItems.ELEC_SWORD.get()));
        add.accept(new ItemStack(ModItems.ENERGY_CORE.get()));
        add.accept(new ItemStack(ModItems.ENTANGLEMENT_KIT.get()));
        add.accept(new ItemStack(ModItems.ENVSUIT_BOOTS.get()));
        add.accept(new ItemStack(ModItems.ENVSUIT_LEGS.get()));
        add.accept(new ItemStack(ModItems.ENVSUIT_PLATE.get()));
        add.accept(new ItemStack(ModItems.EUPHEMIUM_BOOTS.get()));
        add.accept(new ItemStack(ModItems.EUPHEMIUM_HELMET.get()));
        add.accept(new ItemStack(ModItems.EUPHEMIUM_LEGS.get()));
        add.accept(new ItemStack(ModItems.EUPHEMIUM_PLATE.get()));
        add.accept(new ItemStack(ModItems.FALLOUT.get()));
        add.accept(new ItemStack(ModItems.FAU_BOOTS.get()));
        add.accept(new ItemStack(ModItems.FAU_HELMET.get()));
        add.accept(new ItemStack(ModItems.FAU_LEGS.get()));
        add.accept(new ItemStack(ModItems.FAU_PLATE.get()));
        add.accept(new ItemStack(ModItems.FILTER_COAL.get()));
        add.accept(new ItemStack(ModItems.FINS_BIG_STEEL.get()));
        add.accept(new ItemStack(ModItems.FINS_FLAT.get()));
        add.accept(new ItemStack(ModItems.FINS_QUAD_TITANIUM.get()));
        add.accept(new ItemStack(ModItems.FINS_SMALL_STEEL.get()));
        add.accept(new ItemStack(ModItems.FINS_TRI_STEEL.get()));
        add.accept(new ItemStack(ModItems.FLAME_CONSPIRACY.get()));
        add.accept(new ItemStack(ModItems.FLAME_OPINION.get()));
        add.accept(new ItemStack(ModItems.FLAME_POLITICS.get()));
        add.accept(new ItemStack(ModItems.FLAME_PONY.get()));
        add.accept(new ItemStack(ModItems.FLEIJA_CORE.get()));
        add.accept(new ItemStack(ModItems.FLEIJA_IGNITER.get()));
        add.accept(new ItemStack(ModItems.FLEIJA_KIT.get()));
        add.accept(new ItemStack(ModItems.FLEIJA_PROPELLANT.get()));
        add.accept(new ItemStack(ModItems.FLUID_IDENTIFIER_MULTI.get()));
        add.accept(new ItemStack(ModItems.FLYWHEEL_BERYLLIUM.get()));
        add.accept(new ItemStack(ModItems.FOODITEM.get()));
        add.accept(new ItemStack(ModItems.FRAGMENT_ACTINIUM.get()));
        add.accept(new ItemStack(ModItems.FRAGMENT_BORON.get()));
        add.accept(new ItemStack(ModItems.FRAGMENT_CERIUM.get()));
        add.accept(new ItemStack(ModItems.FRAGMENT_COBALT.get()));
        add.accept(new ItemStack(ModItems.FRAGMENT_COLTAN.get()));
        add.accept(new ItemStack(ModItems.FRAGMENT_LANTHANIUM.get()));
        add.accept(new ItemStack(ModItems.FRAGMENT_METEORITE.get()));
        add.accept(new ItemStack(ModItems.FRAGMENT_NEODYMIUM.get()));
        add.accept(new ItemStack(ModItems.FRAGMENT_NIOBIUM.get()));
        add.accept(new ItemStack(ModItems.FUSE.get()));
        add.accept(new ItemStack(ModItems.FUSION_CORE.get()));
        add.accept(new ItemStack(ModItems.FUSION_CORE_INFINITE.get()));
        add.accept(new ItemStack(ModItems.FUSION_SHIELD_CHLOROPHYTE.get()));
        add.accept(new ItemStack(ModItems.FUSION_SHIELD_DESH.get()));
        add.accept(new ItemStack(ModItems.FUSION_SHIELD_TUNGSTEN.get()));
        add.accept(new ItemStack(ModItems.FUSION_SHIELD_VAPORWAVE.get()));
        add.accept(new ItemStack(ModItems.GADGET_CORE.get()));
        add.accept(new ItemStack(ModItems.GADGET_EXPLOSIVE.get()));
        add.accept(new ItemStack(ModItems.GADGET_KIT.get()));
        add.accept(new ItemStack(ModItems.GADGET_WIREING.get()));
        add.accept(new ItemStack(ModItems.GAS_MASK.get()));
        add.accept(new ItemStack(ModItems.GAS_MASK_FILTER.get()));
        add.accept(new ItemStack(ModItems.GAS_MASK_FILTER_COMBO.get()));
        add.accept(new ItemStack(ModItems.GAS_MASK_FILTER_MONO.get()));
        add.accept(new ItemStack(ModItems.GAS_MASK_FILTER_PISS.get()));
        add.accept(new ItemStack(ModItems.GAS_MASK_FILTER_RAG.get()));
        add.accept(new ItemStack(ModItems.GAS_MASK_M65.get()));
        add.accept(new ItemStack(ModItems.GAS_MASK_MONO.get()));
        add.accept(new ItemStack(ModItems.GAS_MASK_OLDE.get()));
        add.accept(new ItemStack(ModItems.GAS_TESTER.get()));
        add.accept(new ItemStack(ModItems.GEAR_LARGE.get()));
        add.accept(new ItemStack(ModItems.GEM_ALEXANDRITE.get()));
        add.accept(new ItemStack(ModItems.GEM_RAD.get()));
        add.accept(new ItemStack(ModItems.GEM_SODALITE.get()));
        add.accept(new ItemStack(ModItems.GEM_TANTALIUM.get()));
        add.accept(new ItemStack(ModItems.GEM_VOLCANIC.get()));
        add.accept(new ItemStack(ModItems.GENERATOR_FRONT.get()));
        add.accept(new ItemStack(ModItems.GENERATOR_STEEL.get()));
        add.accept(new ItemStack(ModItems.GLITCH.get()));
        add.accept(new ItemStack(ModItems.GLOWING_STEW.get()));
        add.accept(new ItemStack(ModItems.GLYPHID_GLAND.get()));
        add.accept(new ItemStack(ModItems.GLYPHID_MEAT.get()));
        add.accept(new ItemStack(ModItems.GLYPHID_MEAT_GRILLED.get()));
        add.accept(new ItemStack(ModItems.GOGGLES.get()));
        add.accept(new ItemStack(ModItems.GRENADE_UNIVERSAL.get()));
        add.accept(new ItemStack(ModItems.GUN_B92.get()));
        add.accept(new ItemStack(ModItems.GUN_FIREEXT.get()));
        add.accept(new ItemStack(ModItems.GUN_KIT_1.get()));
        add.accept(new ItemStack(ModItems.GUN_KIT_2.get()));
        add.accept(new ItemStack(ModItems.GUN_PA_RANGED.get()));
        add.accept(new ItemStack(ModItems.HAND_DRILL.get()));
        add.accept(new ItemStack(ModItems.HAND_DRILL_DESH.get()));
        add.accept(new ItemStack(ModItems.HAZMAT_BOOTS_GREY.get()));
        add.accept(new ItemStack(ModItems.HAZMAT_BOOTS_RED.get()));
        add.accept(new ItemStack(ModItems.HAZMAT_GREY_KIT.get()));
        add.accept(new ItemStack(ModItems.HAZMAT_HELMET_GREY.get()));
        add.accept(new ItemStack(ModItems.HAZMAT_HELMET_RED.get()));
        add.accept(new ItemStack(ModItems.HAZMAT_KIT.get()));
        add.accept(new ItemStack(ModItems.HAZMAT_LEGS.get()));
        add.accept(new ItemStack(ModItems.HAZMAT_LEGS_GREY.get()));
        add.accept(new ItemStack(ModItems.HAZMAT_LEGS_RED.get()));
        add.accept(new ItemStack(ModItems.HAZMAT_PAA_BOOTS.get()));
        add.accept(new ItemStack(ModItems.HAZMAT_PAA_HELMET.get()));
        add.accept(new ItemStack(ModItems.HAZMAT_PAA_LEGS.get()));
        add.accept(new ItemStack(ModItems.HAZMAT_PAA_PLATE.get()));
        add.accept(new ItemStack(ModItems.HAZMAT_PLATE.get()));
        add.accept(new ItemStack(ModItems.HAZMAT_PLATE_GREY.get()));
        add.accept(new ItemStack(ModItems.HAZMAT_PLATE_RED.get()));
        add.accept(new ItemStack(ModItems.HAZMAT_RED_KIT.get()));
        add.accept(new ItemStack(ModItems.HEAVY_COMPONENT.get()));
        add.accept(new ItemStack(ModItems.HEV_BOOTS.get()));
        add.accept(new ItemStack(ModItems.HEV_HELMET.get()));
        add.accept(new ItemStack(ModItems.HEV_LEGS.get()));
        add.accept(new ItemStack(ModItems.HEV_PLATE.get()));
        add.accept(new ItemStack(ModItems.HOLOTAPE_DAMAGED.get()));
        add.accept(new ItemStack(ModItems.HORSESHOE_MAGNET.get()));
        add.accept(new ItemStack(ModItems.HULL_BIG_ALUMINIUM.get()));
        add.accept(new ItemStack(ModItems.HULL_BIG_STEEL.get()));
        add.accept(new ItemStack(ModItems.HULL_BIG_TITANIUM.get()));
        add.accept(new ItemStack(ModItems.HULL_SMALL_ALUMINIUM.get()));
        add.accept(new ItemStack(ModItems.HULL_SMALL_STEEL.get()));
        add.accept(new ItemStack(ModItems.ICF_PELLET.get()));
        add.accept(new ItemStack(ModItems.ICF_PELLET_DEPLETED.get()));
        add.accept(new ItemStack(ModItems.ICF_PELLET_EMPTY.get()));
        add.accept(new ItemStack(ModItems.INDUSTRIAL_MAGNET.get()));
        add.accept(new ItemStack(ModItems.INGOT_ALUMINIUM.get()));
        add.accept(new ItemStack(ModItems.INJECTOR_5HTP.get()));
        add.accept(new ItemStack(ModItems.INJECTOR_KNIFE.get()));
        add.accept(new ItemStack(ModItems.INK.get()));
        add.accept(new ItemStack(ModItems.INSERT_DOXIUM.get()));
        add.accept(new ItemStack(ModItems.INSERT_DU.get()));
        add.accept(new ItemStack(ModItems.INSERT_ERA.get()));
        add.accept(new ItemStack(ModItems.INSERT_ESAPI.get()));
        add.accept(new ItemStack(ModItems.INSERT_GHIORSIUM.get()));
        add.accept(new ItemStack(ModItems.INSERT_KEVLAR.get()));
        add.accept(new ItemStack(ModItems.INSERT_POLONIUM.get()));
        add.accept(new ItemStack(ModItems.INSERT_SAPI.get()));
        add.accept(new ItemStack(ModItems.INSERT_STEEL.get()));
        add.accept(new ItemStack(ModItems.INSERT_XSAPI.get()));
        add.accept(new ItemStack(ModItems.INSERT_YHARONITE.get()));
        add.accept(new ItemStack(ModItems.IV_BLOOD.get()));
        add.accept(new ItemStack(ModItems.IV_EMPTY.get()));
        add.accept(new ItemStack(ModItems.IV_XP.get()));
        add.accept(new ItemStack(ModItems.IV_XP_EMPTY.get()));
        add.accept(new ItemStack(ModItems.JACKT.get()));
        add.accept(new ItemStack(ModItems.JACKT2.get()));
        add.accept(new ItemStack(ModItems.JETPACK_BOOST.get()));
        add.accept(new ItemStack(ModItems.JETPACK_BREAK.get()));
        add.accept(new ItemStack(ModItems.JETPACK_FLY.get()));
        add.accept(new ItemStack(ModItems.JETPACK_TANK.get()));
        add.accept(new ItemStack(ModItems.JETPACK_VECTOR.get()));
        add.accept(new ItemStack(ModItems.JOURNAL_BJ.get()));
        add.accept(new ItemStack(ModItems.JOURNAL_PIP.get()));
        add.accept(new ItemStack(ModItems.JOURNAL_SILVER.get()));
        add.accept(new ItemStack(ModItems.KEY.get()));
        add.accept(new ItemStack(ModItems.KEY_RED.get()));
        add.accept(new ItemStack(ModItems.KEY_RED_CRACKED.get()));
        add.accept(new ItemStack(ModItems.LASER_CRYSTAL_BISMUTH.get()));
        add.accept(new ItemStack(ModItems.LASER_CRYSTAL_CMB.get()));
        add.accept(new ItemStack(ModItems.LASER_CRYSTAL_CO2.get()));
        add.accept(new ItemStack(ModItems.LASER_CRYSTAL_DIGAMMA.get()));
        add.accept(new ItemStack(ModItems.LASER_CRYSTAL_DNT.get()));
        add.accept(new ItemStack(ModItems.LAUNCH_CODE.get()));
        add.accept(new ItemStack(ModItems.LAUNCH_CODE_PIECE.get()));
        add.accept(new ItemStack(ModItems.LAUNCH_KEY.get()));
        add.accept(new ItemStack(ModItems.LEAD_GAVEL.get()));
        add.accept(new ItemStack(ModItems.LEMON.get()));
        add.accept(new ItemStack(ModItems.LINKER.get()));
        add.accept(new ItemStack(ModItems.LIQUIDATOR_LEGS.get()));
        add.accept(new ItemStack(ModItems.LIQUIDATOR_PLATE.get()));
        add.accept(new ItemStack(ModItems.LITHIUM.get()));
        add.accept(new ItemStack(ModItems.LODESTONE.get()));
        add.accept(new ItemStack(ModItems.LOOP_STEW.get()));
        add.accept(new ItemStack(ModItems.LOOPS.get()));
        add.accept(new ItemStack(ModItems.LOOT_10.get()));
        add.accept(new ItemStack(ModItems.LOOT_15.get()));
        add.accept(new ItemStack(ModItems.LOOT_MISC.get()));
        add.accept(new ItemStack(ModItems.MAN_CORE.get()));
        add.accept(new ItemStack(ModItems.MAN_IGNITER.get()));
        add.accept(new ItemStack(ModItems.MAN_KIT.get()));
        add.accept(new ItemStack(ModItems.MARSHMALLOW.get()));
        add.accept(new ItemStack(ModItems.MASK_OF_INFAMY.get()));
        add.accept(new ItemStack(ModItems.MASK_PISS.get()));
        add.accept(new ItemStack(ModItems.MASK_RAG.get()));
        add.accept(new ItemStack(ModItems.MATCHSTICK.get()));
        add.accept(new ItemStack(ModItems.MECH_KEY.get()));
        add.accept(new ItemStack(ModItems.MED_BAG.get()));
        add.accept(new ItemStack(ModItems.MED_IPECAC.get()));
        add.accept(new ItemStack(ModItems.MED_PTSD.get()));
        add.accept(new ItemStack(ModItems.MEDAL_LIQUIDATOR.get()));
        add.accept(new ItemStack(ModItems.MELTDOWN_TOOL.get()));
        add.accept(new ItemStack(ModItems.MEMESPOON.get()));
        add.accept(new ItemStack(ModItems.MESE_AXE.get()));
        add.accept(new ItemStack(ModItems.MESE_GAVEL.get()));
        add.accept(new ItemStack(ModItems.MESE_PICKAXE.get()));
        add.accept(new ItemStack(ModItems.METEOR_CHARM.get()));
        add.accept(new ItemStack(ModItems.METEOR_REMOTE.get()));
        add.accept(new ItemStack(ModItems.MIKE_COOLING_UNIT.get()));
        add.accept(new ItemStack(ModItems.MIKE_CORE.get()));
        add.accept(new ItemStack(ModItems.MIKE_DEUT.get()));
        add.accept(new ItemStack(ModItems.MIKE_KIT.get()));
        add.accept(new ItemStack(ModItems.MIRROR_TOOL.get()));
        add.accept(new ItemStack(ModItems.MISSILE_ANTI_BALLISTIC.get()));
        add.accept(new ItemStack(ModItems.MISSILE_CARRIER.get()));
        add.accept(new ItemStack(ModItems.MISSILE_CUSTOM.get()));
        add.accept(new ItemStack(ModItems.MISSILE_ENDO.get()));
        add.accept(new ItemStack(ModItems.MISSILE_EXO.get()));
        add.accept(new ItemStack(ModItems.MISSILE_KIT.get()));
        add.accept(new ItemStack(ModItems.MORNING_GLORY.get()));
        add.accept(new ItemStack(ModItems.MP_C_1.get()));
        add.accept(new ItemStack(ModItems.MP_C_2.get()));
        add.accept(new ItemStack(ModItems.MP_C_3.get()));
        add.accept(new ItemStack(ModItems.MP_C_4.get()));
        add.accept(new ItemStack(ModItems.MP_C_5.get()));
        add.accept(new ItemStack(ModItems.MUCHO_MANGO.get()));
        add.accept(new ItemStack(ModItems.MULTI_KIT.get()));
        add.accept(new ItemStack(ModItems.N2_CHARGE.get()));
        add.accept(new ItemStack(ModItems.NEUTRINO_LENS.get()));
        add.accept(new ItemStack(ModItems.NIGHT_VISION.get()));
        add.accept(new ItemStack(ModItems.NITRA.get()));
        add.accept(new ItemStack(ModItems.NITRA_SMALL.get()));
        add.accept(new ItemStack(ModItems.NO9.get()));
        add.accept(new ItemStack(ModItems.NOTHING.get()));
        add.accept(new ItemStack(ModItems.NUCLEAR_WASTE.get()));
        add.accept(new ItemStack(ModItems.NUCLEAR_WASTE_LONG.get()));
        add.accept(new ItemStack(ModItems.NUCLEAR_WASTE_LONG_DEPLETED.get()));
        add.accept(new ItemStack(ModItems.NUCLEAR_WASTE_PEARL.get()));
        add.accept(new ItemStack(ModItems.NUCLEAR_WASTE_SHORT.get()));
        add.accept(new ItemStack(ModItems.NUCLEAR_WASTE_SHORT_DEPLETED.get()));
        add.accept(new ItemStack(ModItems.NUCLEAR_WASTE_VITRIFIED.get()));
        add.accept(new ItemStack(ModItems.NUGGET.get()));
        add.accept(new ItemStack(ModItems.NUGGET_ACTINIUM.get()));
        add.accept(new ItemStack(ModItems.NUGGET_AM241.get()));
        add.accept(new ItemStack(ModItems.NUGGET_AM242.get()));
        add.accept(new ItemStack(ModItems.NUGGET_AM_MIX.get()));
        add.accept(new ItemStack(ModItems.NUGGET_AMERICIUM_FUEL.get()));
        add.accept(new ItemStack(ModItems.NUGGET_ARSENIC.get()));
        add.accept(new ItemStack(ModItems.NUGGET_AU198.get()));
        add.accept(new ItemStack(ModItems.NUGGET_AUSTRALIUM.get()));
        add.accept(new ItemStack(ModItems.NUGGET_AUSTRALIUM_GREATER.get()));
        add.accept(new ItemStack(ModItems.NUGGET_AUSTRALIUM_LESSER.get()));
        add.accept(new ItemStack(ModItems.NUGGET_BERYLLIUM.get()));
        add.accept(new ItemStack(ModItems.NUGGET_BISMUTH.get()));
        add.accept(new ItemStack(ModItems.NUGGET_CO60.get()));
        add.accept(new ItemStack(ModItems.NUGGET_COBALT.get()));
        add.accept(new ItemStack(ModItems.NUGGET_DESH.get()));
        add.accept(new ItemStack(ModItems.NUGGET_DINEUTRONIUM.get()));
        add.accept(new ItemStack(ModItems.NUGGET_EUPHEMIUM.get()));
        add.accept(new ItemStack(ModItems.NUGGET_GH336.get()));
        add.accept(new ItemStack(ModItems.NUGGET_HES.get()));
        add.accept(new ItemStack(ModItems.NUGGET_LEAD.get()));
        add.accept(new ItemStack(ModItems.NUGGET_LES.get()));
        add.accept(new ItemStack(ModItems.NUGGET_MERCURY.get()));
        add.accept(new ItemStack(ModItems.NUGGET_MOX_FUEL.get()));
        add.accept(new ItemStack(ModItems.NUGGET_NEPTUNIUM.get()));
        add.accept(new ItemStack(ModItems.NUGGET_NEPTUNIUM_FUEL.get()));
        add.accept(new ItemStack(ModItems.NUGGET_NIOBIUM.get()));
        add.accept(new ItemStack(ModItems.NUGGET_OSMIRIDIUM.get()));
        add.accept(new ItemStack(ModItems.NUGGET_PB209.get()));
        add.accept(new ItemStack(ModItems.NUGGET_PLUTONIUM.get()));
        add.accept(new ItemStack(ModItems.NUGGET_PLUTONIUM_FUEL.get()));
        add.accept(new ItemStack(ModItems.NUGGET_POLONIUM.get()));
        add.accept(new ItemStack(ModItems.NUGGET_PU238.get()));
        add.accept(new ItemStack(ModItems.NUGGET_PU239.get()));
        add.accept(new ItemStack(ModItems.NUGGET_PU240.get()));
        add.accept(new ItemStack(ModItems.NUGGET_PU241.get()));
        add.accept(new ItemStack(ModItems.NUGGET_PU_MIX.get()));
        add.accept(new ItemStack(ModItems.NUGGET_RA226.get()));
        add.accept(new ItemStack(ModItems.NUGGET_SCHRABIDIUM.get()));
        add.accept(new ItemStack(ModItems.NUGGET_SCHRABIDIUM_FUEL.get()));
        add.accept(new ItemStack(ModItems.NUGGET_SOLINIUM.get()));
        add.accept(new ItemStack(ModItems.NUGGET_SR90.get()));
        add.accept(new ItemStack(ModItems.NUGGET_TECHNETIUM.get()));
        add.accept(new ItemStack(ModItems.NUGGET_TH232.get()));
        add.accept(new ItemStack(ModItems.NUGGET_THORIUM_FUEL.get()));
        add.accept(new ItemStack(ModItems.NUGGET_U233.get()));
        add.accept(new ItemStack(ModItems.NUGGET_U235.get()));
        add.accept(new ItemStack(ModItems.NUGGET_U238.get()));
        add.accept(new ItemStack(ModItems.NUGGET_URANIUM.get()));
        add.accept(new ItemStack(ModItems.NUGGET_URANIUM_FUEL.get()));
        add.accept(new ItemStack(ModItems.NUGGET_ZIRCONIUM.get()));
        add.accept(new ItemStack(ModItems.NUKE_ADVANCED_KIT.get()));
        add.accept(new ItemStack(ModItems.NUKE_COMMERCIALLY_KIT.get()));
        add.accept(new ItemStack(ModItems.NUKE_ELECTRIC_KIT.get()));
        add.accept(new ItemStack(ModItems.NUKE_STARTER_KIT.get()));
        add.accept(new ItemStack(ModItems.ORE_BEDROCK.get()));
        add.accept(new ItemStack(ModItems.ORE_CENTRIFUGED.get()));
        add.accept(new ItemStack(ModItems.ORE_CLEANED.get()));
        add.accept(new ItemStack(ModItems.ORE_DEEPCLEANED.get()));
        add.accept(new ItemStack(ModItems.ORE_DENSITY_SCANNER.get()));
        add.accept(new ItemStack(ModItems.ORE_ENRICHED.get()));
        add.accept(new ItemStack(ModItems.ORE_NITRATED.get()));
        add.accept(new ItemStack(ModItems.ORE_NITROCRYSTALLINE.get()));
        add.accept(new ItemStack(ModItems.ORE_PURIFIED.get()));
        add.accept(new ItemStack(ModItems.ORE_RADCLEANED.get()));
        add.accept(new ItemStack(ModItems.ORE_SEARED.get()));
        add.accept(new ItemStack(ModItems.ORE_SEPARATED.get()));
        add.accept(new ItemStack(ModItems.OVERFUSE.get()));
        add.accept(new ItemStack(ModItems.PAA_LEGS.get()));
        add.accept(new ItemStack(ModItems.PAA_PLATE.get()));
        add.accept(new ItemStack(ModItems.PADLOCK.get()));
        add.accept(new ItemStack(ModItems.PADLOCK_REINFORCED.get()));
        add.accept(new ItemStack(ModItems.PADLOCK_RUSTY.get()));
        add.accept(new ItemStack(ModItems.PADLOCK_UNBREAKABLE.get()));
        add.accept(new ItemStack(ModItems.PADS_RUBBER.get()));
        add.accept(new ItemStack(ModItems.PADS_SLIME.get()));
        add.accept(new ItemStack(ModItems.PADS_STATIC.get()));
        add.accept(new ItemStack(ModItems.PANCAKE.get()));
        add.accept(new ItemStack(ModItems.PART_BARREL_HEAVY.get()));
        add.accept(new ItemStack(ModItems.PART_BARREL_LIGHT.get()));
        add.accept(new ItemStack(ModItems.PART_GRIP.get()));
        add.accept(new ItemStack(ModItems.PART_MECHANISM.get()));
        add.accept(new ItemStack(ModItems.PART_RECEIVER_HEAVY.get()));
        add.accept(new ItemStack(ModItems.PART_RECEIVER_LIGHT.get()));
        add.accept(new ItemStack(ModItems.PART_STOCK.get()));
        add.accept(new ItemStack(ModItems.PARTICLE_AMAT.get()));
        add.accept(new ItemStack(ModItems.PARTICLE_ASCHRAB.get()));
        add.accept(new ItemStack(ModItems.PARTICLE_COPPER.get()));
        add.accept(new ItemStack(ModItems.PARTICLE_DARK.get()));
        add.accept(new ItemStack(ModItems.PARTICLE_DIGAMMA.get()));
        add.accept(new ItemStack(ModItems.PARTICLE_EMPTY.get()));
        add.accept(new ItemStack(ModItems.PARTICLE_HIGGS.get()));
        add.accept(new ItemStack(ModItems.PARTICLE_HYDROGEN.get()));
        add.accept(new ItemStack(ModItems.PARTICLE_LEAD.get()));
        add.accept(new ItemStack(ModItems.PARTICLE_LUTECE.get()));
        add.accept(new ItemStack(ModItems.PARTICLE_MUON.get()));
        add.accept(new ItemStack(ModItems.PARTICLE_SPARKTICLE.get()));
        add.accept(new ItemStack(ModItems.PARTICLE_STRANGE.get()));
        add.accept(new ItemStack(ModItems.PARTICLE_TACHYON.get()));
        add.accept(new ItemStack(ModItems.PARTS_LEGENDARY.get()));
        add.accept(new ItemStack(ModItems.PEAS.get()));
        add.accept(new ItemStack(ModItems.PEDESTAL_STEEL.get()));
        add.accept(new ItemStack(ModItems.PELLET_ANTIMATTER.get()));
        add.accept(new ItemStack(ModItems.PELLET_CLUSTER.get()));
        add.accept(new ItemStack(ModItems.PELLET_GAS.get()));
        add.accept(new ItemStack(ModItems.PELLET_RTG.get()));
        add.accept(new ItemStack(ModItems.PELLET_RTG_ACTINIUM.get()));
        add.accept(new ItemStack(ModItems.PELLET_RTG_AMERICIUM.get()));
        add.accept(new ItemStack(ModItems.PELLET_RTG_BERKELIUM.get()));
        add.accept(new ItemStack(ModItems.PELLET_RTG_COBALT.get()));
        add.accept(new ItemStack(ModItems.PELLET_RTG_GOLD.get()));
        add.accept(new ItemStack(ModItems.PELLET_RTG_LEAD.get()));
        add.accept(new ItemStack(ModItems.PELLET_RTG_POLONIUM.get()));
        add.accept(new ItemStack(ModItems.PELLET_RTG_RADIUM.get()));
        add.accept(new ItemStack(ModItems.PELLET_RTG_STRONTIUM.get()));
        add.accept(new ItemStack(ModItems.PELLET_RTG_WEAK.get()));
        add.accept(new ItemStack(ModItems.PHOTO_PANEL.get()));
        add.accept(new ItemStack(ModItems.PILE_ROD_BORON.get()));
        add.accept(new ItemStack(ModItems.PILE_ROD_DETECTOR.get()));
        add.accept(new ItemStack(ModItems.PILE_ROD_LITHIUM.get()));
        add.accept(new ItemStack(ModItems.PILE_ROD_PLUTONIUM.get()));
        add.accept(new ItemStack(ModItems.PILE_ROD_PU239.get()));
        add.accept(new ItemStack(ModItems.PILE_ROD_SOURCE.get()));
        add.accept(new ItemStack(ModItems.PILE_ROD_URANIUM.get()));
        add.accept(new ItemStack(ModItems.PILL_HERBAL.get()));
        add.accept(new ItemStack(ModItems.PILL_IODINE.get()));
        add.accept(new ItemStack(ModItems.PILL_RED.get()));
        add.accept(new ItemStack(ModItems.PIN.get()));
        add.accept(new ItemStack(ModItems.PIPES_STEEL.get()));
        add.accept(new ItemStack(ModItems.PIPETTE.get()));
        add.accept(new ItemStack(ModItems.PIPETTE_BORON.get()));
        add.accept(new ItemStack(ModItems.PIPETTE_LABORATORY.get()));
        add.accept(new ItemStack(ModItems.PISTON_SELENIUM.get()));
        add.accept(new ItemStack(ModItems.PISTON_SET_DESH.get()));
        add.accept(new ItemStack(ModItems.PISTON_SET_DURA.get()));
        add.accept(new ItemStack(ModItems.PISTON_SET_STARMETAL.get()));
        add.accept(new ItemStack(ModItems.PISTON_SET_STEEL.get()));
        add.accept(new ItemStack(ModItems.PLAN_C.get()));
        add.accept(new ItemStack(ModItems.PLASTIC_BAG.get()));
        add.accept(new ItemStack(ModItems.PLATE_ALUMINIUM.get()));
        add.accept(new ItemStack(ModItems.PLATE_POLYMER.get()));
        add.accept(new ItemStack(ModItems.POLAROID.get()));
        add.accept(new ItemStack(ModItems.POLLUTION_DETECTOR.get()));
        add.accept(new ItemStack(ModItems.POWER_NET_TOOL.get()));
        add.accept(new ItemStack(ModItems.PROTECTION_CHARM.get()));
        add.accept(new ItemStack(ModItems.PROTOTYPE_KIT.get()));
        add.accept(new ItemStack(ModItems.PUDDING.get()));
        add.accept(new ItemStack(ModItems.PWR_PRINTER.get()));
        add.accept(new ItemStack(ModItems.QUARTZ_PLUTONIUM.get()));
        add.accept(new ItemStack(ModItems.RADAR_LINKER.get()));
        add.accept(new ItemStack(ModItems.RAG.get()));
        add.accept(new ItemStack(ModItems.RAG_DAMP.get()));
        add.accept(new ItemStack(ModItems.RAG_PISS.get()));
        add.accept(new ItemStack(ModItems.RBMK_FUEL_BALEFIRE.get()));
        add.accept(new ItemStack(ModItems.RBMK_FUEL_BALEFIRE_GOLD.get()));
        add.accept(new ItemStack(ModItems.RBMK_FUEL_FLASHLEAD.get()));
        add.accept(new ItemStack(ModItems.RBMK_FUEL_HEA241.get()));
        add.accept(new ItemStack(ModItems.RBMK_FUEL_HEA242.get()));
        add.accept(new ItemStack(ModItems.RBMK_FUEL_HEAUS.get()));
        add.accept(new ItemStack(ModItems.RBMK_FUEL_HEN.get()));
        add.accept(new ItemStack(ModItems.RBMK_FUEL_HEP_ALT.get()));
        add.accept(new ItemStack(ModItems.RBMK_FUEL_HEP241.get()));
        add.accept(new ItemStack(ModItems.RBMK_FUEL_HES.get()));
        add.accept(new ItemStack(ModItems.RBMK_FUEL_HEU233.get()));
        add.accept(new ItemStack(ModItems.RBMK_FUEL_LEA.get()));
        add.accept(new ItemStack(ModItems.RBMK_FUEL_LEAUS.get()));
        add.accept(new ItemStack(ModItems.RBMK_FUEL_LES.get()));
        add.accept(new ItemStack(ModItems.RBMK_FUEL_MEA.get()));
        add.accept(new ItemStack(ModItems.RBMK_FUEL_MEN.get()));
        add.accept(new ItemStack(ModItems.RBMK_FUEL_MEP.get()));
        add.accept(new ItemStack(ModItems.RBMK_FUEL_MES.get()));
        add.accept(new ItemStack(ModItems.RBMK_FUEL_MEU.get()));
        add.accept(new ItemStack(ModItems.RBMK_FUEL_PO210BE.get()));
        add.accept(new ItemStack(ModItems.RBMK_FUEL_PU238BE.get()));
        add.accept(new ItemStack(ModItems.RBMK_FUEL_RA226BE.get()));
        add.accept(new ItemStack(ModItems.RBMK_FUEL_THMEU.get()));
        add.accept(new ItemStack(ModItems.RBMK_FUEL_UEU.get()));
        add.accept(new ItemStack(ModItems.RBMK_FUEL_UZH.get()));
        add.accept(new ItemStack(ModItems.RBMK_FUEL_ZFB_AM_MIX.get()));
        add.accept(new ItemStack(ModItems.RBMK_FUEL_ZFB_BISMUTH.get()));
        add.accept(new ItemStack(ModItems.RBMK_FUEL_ZFB_PU241.get()));
        add.accept(new ItemStack(ModItems.RBMK_PELLET_BALEFIRE.get()));
        add.accept(new ItemStack(ModItems.RBMK_PELLET_BALEFIRE_GOLD.get()));
        add.accept(new ItemStack(ModItems.RBMK_PELLET_DRX.get()));
        add.accept(new ItemStack(ModItems.RBMK_PELLET_FLASHLEAD.get()));
        add.accept(new ItemStack(ModItems.RBMK_PELLET_HEA241.get()));
        add.accept(new ItemStack(ModItems.RBMK_PELLET_HEA242.get()));
        add.accept(new ItemStack(ModItems.RBMK_PELLET_HEAUS.get()));
        add.accept(new ItemStack(ModItems.RBMK_PELLET_HEN.get()));
        add.accept(new ItemStack(ModItems.RBMK_PELLET_HEP241.get()));
        add.accept(new ItemStack(ModItems.RBMK_PELLET_HES.get()));
        add.accept(new ItemStack(ModItems.RBMK_PELLET_HEU233.get()));
        add.accept(new ItemStack(ModItems.RBMK_PELLET_LEA.get()));
        add.accept(new ItemStack(ModItems.RBMK_PELLET_LEAUS.get()));
        add.accept(new ItemStack(ModItems.RBMK_PELLET_LES.get()));
        add.accept(new ItemStack(ModItems.RBMK_PELLET_MEA.get()));
        add.accept(new ItemStack(ModItems.RBMK_PELLET_MEN.get()));
        add.accept(new ItemStack(ModItems.RBMK_PELLET_MEP.get()));
        add.accept(new ItemStack(ModItems.RBMK_PELLET_MES.get()));
        add.accept(new ItemStack(ModItems.RBMK_PELLET_MEU.get()));
        add.accept(new ItemStack(ModItems.RBMK_PELLET_PO210BE.get()));
        add.accept(new ItemStack(ModItems.RBMK_PELLET_PU238BE.get()));
        add.accept(new ItemStack(ModItems.RBMK_PELLET_RA226BE.get()));
        add.accept(new ItemStack(ModItems.RBMK_PELLET_THMEU.get()));
        add.accept(new ItemStack(ModItems.RBMK_PELLET_UEU.get()));
        add.accept(new ItemStack(ModItems.RBMK_PELLET_UZH.get()));
        add.accept(new ItemStack(ModItems.RBMK_PELLET_ZFB_AM_MIX.get()));
        add.accept(new ItemStack(ModItems.RBMK_PELLET_ZFB_BISMUTH.get()));
        add.accept(new ItemStack(ModItems.RBMK_PELLET_ZFB_PU241.get()));
        add.accept(new ItemStack(ModItems.RBMK_TOOL.get()));
        add.accept(new ItemStack(ModItems.REACHER.get()));
        add.accept(new ItemStack(ModItems.REACTOR_CORE.get()));
        add.accept(new ItemStack(ModItems.REACTOR_SENSOR.get()));
        add.accept(new ItemStack(ModItems.REBAR_PLACER.get()));
        add.accept(new ItemStack(ModItems.REDSTONE_SWORD.get()));
        add.accept(new ItemStack(ModItems.RING_PULL.get()));
        add.accept(new ItemStack(ModItems.RING_STARMETAL.get()));
        add.accept(new ItemStack(ModItems.ROBES_BOOTS.get()));
        add.accept(new ItemStack(ModItems.ROBES_HELMET.get()));
        add.accept(new ItemStack(ModItems.ROBES_LEGS.get()));
        add.accept(new ItemStack(ModItems.ROBES_PLATE.get()));
        add.accept(new ItemStack(ModItems.ROCKET_FUEL.get()));
        add.accept(new ItemStack(ModItems.ROD_DUAL_EMPTY.get()));
        add.accept(new ItemStack(ModItems.ROD_EMPTY.get()));
        add.accept(new ItemStack(ModItems.ROD_OF_DISCORD.get()));
        add.accept(new ItemStack(ModItems.ROD_QUAD_EMPTY.get()));
        add.accept(new ItemStack(ModItems.RPA_BOOTS.get()));
        add.accept(new ItemStack(ModItems.RPA_HELMET.get()));
        add.accept(new ItemStack(ModItems.RPA_LEGS.get()));
        add.accept(new ItemStack(ModItems.RPA_PLATE.get()));
        add.accept(new ItemStack(ModItems.RTG_UNIT.get()));
        add.accept(new ItemStack(ModItems.RTTY_PAGER.get()));
        add.accept(new ItemStack(ModItems.RUNE_BLANK.get()));
        add.accept(new ItemStack(ModItems.RUNE_DAGAZ.get()));
        add.accept(new ItemStack(ModItems.RUNE_HAGALAZ.get()));
        add.accept(new ItemStack(ModItems.RUNE_ISA.get()));
        add.accept(new ItemStack(ModItems.RUNE_JERA.get()));
        add.accept(new ItemStack(ModItems.RUNE_THURISAZ.get()));
        add.accept(new ItemStack(ModItems.SAFETY_FUSE.get()));
        add.accept(new ItemStack(ModItems.SAT_CHIP.get()));
        add.accept(new ItemStack(ModItems.SAT_COORD.get()));
        add.accept(new ItemStack(ModItems.SAT_DESIGNATOR.get()));
        add.accept(new ItemStack(ModItems.SAT_GERALD.get()));
        add.accept(new ItemStack(ModItems.SAT_HEAD_SCANNER.get()));
        add.accept(new ItemStack(ModItems.SAT_INTERFACE.get()));
        add.accept(new ItemStack(ModItems.SAT_LUNAR_MINER.get()));
        add.accept(new ItemStack(ModItems.SAT_MINER.get()));
        add.accept(new ItemStack(ModItems.SAT_RELAY.get()));
        add.accept(new ItemStack(ModItems.SAWBLADE.get()));
        add.accept(new ItemStack(ModItems.SCHNITZEL_VEGAN.get()));
        add.accept(new ItemStack(ModItems.SCHRABIDIUM_AXE.get()));
        add.accept(new ItemStack(ModItems.SCHRABIDIUM_BOOTS.get()));
        add.accept(new ItemStack(ModItems.SCHRABIDIUM_HAMMER.get()));
        add.accept(new ItemStack(ModItems.SCHRABIDIUM_HELMET.get()));
        add.accept(new ItemStack(ModItems.SCHRABIDIUM_HOE.get()));
        add.accept(new ItemStack(ModItems.SCHRABIDIUM_LEGS.get()));
        add.accept(new ItemStack(ModItems.SCHRABIDIUM_PICKAXE.get()));
        add.accept(new ItemStack(ModItems.SCHRABIDIUM_PLATE.get()));
        add.accept(new ItemStack(ModItems.SCHRABIDIUM_SHOVEL.get()));
        add.accept(new ItemStack(ModItems.SCHRABIDIUM_SWORD.get()));
        add.accept(new ItemStack(ModItems.SCRAP_NUCLEAR.get()));
        add.accept(new ItemStack(ModItems.SCRAP_OIL.get()));
        add.accept(new ItemStack(ModItems.SCRAP_PLASTIC.get()));
        add.accept(new ItemStack(ModItems.SCRAPS.get()));
        add.accept(new ItemStack(ModItems.SCREWDRIVER_DESH.get()));
        add.accept(new ItemStack(ModItems.SCRUMPY.get()));
        add.accept(new ItemStack(ModItems.SECURITY_LEGS.get()));
        add.accept(new ItemStack(ModItems.SECURITY_PLATE.get()));
        add.accept(new ItemStack(ModItems.SEG_10.get()));
        add.accept(new ItemStack(ModItems.SEG_15.get()));
        add.accept(new ItemStack(ModItems.SEG_20.get()));
        add.accept(new ItemStack(ModItems.SERUM.get()));
        add.accept(new ItemStack(ModItems.SERVO_SET.get()));
        add.accept(new ItemStack(ModItems.SERVO_SET_DESH.get()));
        add.accept(new ItemStack(ModItems.SETTINGS_TOOL.get()));
        add.accept(new ItemStack(ModItems.SHACKLES.get()));
        add.accept(new ItemStack(ModItems.SHIMMER_AXE.get()));
        add.accept(new ItemStack(ModItems.SHIMMER_AXE_HEAD.get()));
        add.accept(new ItemStack(ModItems.SHIMMER_HANDLE.get()));
        add.accept(new ItemStack(ModItems.SHIMMER_HEAD.get()));
        add.accept(new ItemStack(ModItems.SHIMMER_SLEDGE.get()));
        add.accept(new ItemStack(ModItems.SINGULARITY.get()));
        add.accept(new ItemStack(ModItems.SIOX.get()));
        add.accept(new ItemStack(ModItems.SIPHON.get()));
        add.accept(new ItemStack(ModItems.SMASHING_HAMMER.get()));
        add.accept(new ItemStack(ModItems.SOLID_FUEL.get()));
        add.accept(new ItemStack(ModItems.SOLID_FUEL_BF.get()));
        add.accept(new ItemStack(ModItems.SOLID_FUEL_PRESTO.get()));
        add.accept(new ItemStack(ModItems.SOLID_FUEL_PRESTO_BF.get()));
        add.accept(new ItemStack(ModItems.SOLID_FUEL_PRESTO_TRIPLET.get()));
        add.accept(new ItemStack(ModItems.SOLID_FUEL_PRESTO_TRIPLET_BF.get()));
        add.accept(new ItemStack(ModItems.SOLINIUM_CORE.get()));
        add.accept(new ItemStack(ModItems.SOLINIUM_IGNITER.get()));
        add.accept(new ItemStack(ModItems.SOLINIUM_KIT.get()));
        add.accept(new ItemStack(ModItems.SOLINIUM_PROPELLANT.get()));
        add.accept(new ItemStack(ModItems.SOPSIGN.get()));
        add.accept(new ItemStack(ModItems.SPAWN_DUCK.get()));
        add.accept(new ItemStack(ModItems.SPAWN_UFO.get()));
        add.accept(new ItemStack(ModItems.SPAWN_WORM.get()));
        add.accept(new ItemStack(ModItems.SPHERE_STEEL.get()));
        add.accept(new ItemStack(ModItems.SPIDER_MILK.get()));
        add.accept(new ItemStack(ModItems.SPONGEBOB_MACARONI.get()));
        add.accept(new ItemStack(ModItems.STAMP_357.get()));
        add.accept(new ItemStack(ModItems.STAMP_44.get()));
        add.accept(new ItemStack(ModItems.STAMP_50.get()));
        add.accept(new ItemStack(ModItems.STAMP_9.get()));
        add.accept(new ItemStack(ModItems.STARMETAL_LEGS.get()));
        add.accept(new ItemStack(ModItems.STARMETAL_PLATE.get()));
        add.accept(new ItemStack(ModItems.STATIC_SANDWICH.get()));
        add.accept(new ItemStack(ModItems.STEALTH_BOY.get()));
        add.accept(new ItemStack(ModItems.STEAMSUIT_BOOTS.get()));
        add.accept(new ItemStack(ModItems.STEAMSUIT_HELMET.get()));
        add.accept(new ItemStack(ModItems.STEAMSUIT_LEGS.get()));
        add.accept(new ItemStack(ModItems.STEAMSUIT_PLATE.get()));
        add.accept(new ItemStack(ModItems.STEEL_LEGS.get()));
        add.accept(new ItemStack(ModItems.STEEL_PLATE.get()));
        add.accept(new ItemStack(ModItems.STICK_C4.get()));
        add.accept(new ItemStack(ModItems.STICK_DYNAMITE.get()));
        add.accept(new ItemStack(ModItems.STICK_DYNAMITE_FISHING.get()));
        add.accept(new ItemStack(ModItems.STICK_SEMTEX.get()));
        add.accept(new ItemStack(ModItems.STICK_TNT.get()));
        add.accept(new ItemStack(ModItems.STOPSIGN.get()));
        add.accept(new ItemStack(ModItems.STRUCTURE_CUSTOMMACHINE.get()));
        add.accept(new ItemStack(ModItems.SURVEY_SCANNER.get()));
        add.accept(new ItemStack(ModItems.SYRINGE_ANTIDOTE.get()));
        add.accept(new ItemStack(ModItems.SYRINGE_AWESOME.get()));
        add.accept(new ItemStack(ModItems.SYRINGE_EMPTY.get()));
        add.accept(new ItemStack(ModItems.SYRINGE_METAL_EMPTY.get()));
        add.accept(new ItemStack(ModItems.SYRINGE_METAL_MEDX.get()));
        add.accept(new ItemStack(ModItems.SYRINGE_METAL_PSYCHO.get()));
        add.accept(new ItemStack(ModItems.SYRINGE_METAL_STIMPAK.get()));
        add.accept(new ItemStack(ModItems.SYRINGE_METAL_SUPER.get()));
        add.accept(new ItemStack(ModItems.SYRINGE_MKUNICORN.get()));
        add.accept(new ItemStack(ModItems.SYRINGE_POISON.get()));
        add.accept(new ItemStack(ModItems.SYRINGE_TAINT.get()));
        add.accept(new ItemStack(ModItems.TANK_STEEL.get()));
        add.accept(new ItemStack(ModItems.TAURUN_BOOTS.get()));
        add.accept(new ItemStack(ModItems.TAURUN_HELMET.get()));
        add.accept(new ItemStack(ModItems.TAURUN_LEGS.get()));
        add.accept(new ItemStack(ModItems.TAURUN_PLATE.get()));
        add.accept(new ItemStack(ModItems.TEM_FLAKES.get()));
        add.accept(new ItemStack(ModItems.THERMO_ELEMENT.get()));
        add.accept(new ItemStack(ModItems.THRUSTER_NUCLEAR.get()));
        add.accept(new ItemStack(ModItems.TITANIUM_FILTER.get()));
        add.accept(new ItemStack(ModItems.TITANIUM_LEGS.get()));
        add.accept(new ItemStack(ModItems.TITANIUM_PLATE.get()));
        add.accept(new ItemStack(ModItems.TRENCHMASTER_BOOTS.get()));
        add.accept(new ItemStack(ModItems.TRENCHMASTER_HELMET.get()));
        add.accept(new ItemStack(ModItems.TRENCHMASTER_LEGS.get()));
        add.accept(new ItemStack(ModItems.TRENCHMASTER_PLATE.get()));
        add.accept(new ItemStack(ModItems.TRINITITE.get()));
        add.accept(new ItemStack(ModItems.TSAR_CORE.get()));
        add.accept(new ItemStack(ModItems.TSAR_KIT.get()));
        add.accept(new ItemStack(ModItems.TURBINE_TUNGSTEN.get()));
        add.accept(new ItemStack(ModItems.TURRET_CHIP.get()));
        add.accept(new ItemStack(ModItems.TWINKIE.get()));
        add.accept(new ItemStack(ModItems.ULLAPOOL_CABER.get()));
        add.accept(new ItemStack(ModItems.UNDEFINED.get()));
        add.accept(new ItemStack(ModItems.VOLCANIC_AXE.get()));
        add.accept(new ItemStack(ModItems.VOLCANIC_PICKAXE.get()));
        add.accept(new ItemStack(ModItems.WAND_D.get()));
        add.accept(new ItemStack(ModItems.WAND_S.get()));
        add.accept(new ItemStack(ModItems.WARHEAD_INCENDIARY_LARGE.get()));
        add.accept(new ItemStack(ModItems.WASTE_MOX.get()));
        add.accept(new ItemStack(ModItems.WASTE_PLATE_MOX.get()));
        add.accept(new ItemStack(ModItems.WASTE_PLATE_PU238BE.get()));
        add.accept(new ItemStack(ModItems.WASTE_PLATE_RA226BE.get()));
        add.accept(new ItemStack(ModItems.WASTE_PLATE_SA326.get()));
        add.accept(new ItemStack(ModItems.WASTE_PLUTONIUM.get()));
        add.accept(new ItemStack(ModItems.WASTE_SCHRABIDIUM.get()));
        add.accept(new ItemStack(ModItems.WASTE_THORIUM.get()));
        add.accept(new ItemStack(ModItems.WASTE_URANIUM.get()));
        add.accept(new ItemStack(ModItems.WASTE_ZFB_MOX.get()));
        add.accept(new ItemStack(ModItems.WATCH.get()));
        add.accept(new ItemStack(ModItems.WD40.get()));
        add.accept(new ItemStack(ModItems.WILD_P.get()));
        add.accept(new ItemStack(ModItems.WINGS_LIMP.get()));
        add.accept(new ItemStack(ModItems.WINGS_MURK.get()));
        add.accept(new ItemStack(ModItems.WIRING_RED_COPPER.get()));
        add.accept(new ItemStack(ModItems.WOOD_GAVEL.get()));
        add.accept(new ItemStack(ModItems.WRENCH.get()));
        add.accept(new ItemStack(ModItems.WRENCH_ARCHINEER.get()));
        add.accept(new ItemStack(ModItems.WRENCH_FLIPPED.get()));
        add.accept(new ItemStack(ModItems.XANAX.get()));
        add.accept(new ItemStack(ModItems.ZIRCONIUM_LEGS.get()));

        // --- Neu portierte, noch nicht einsortierte Pulver (zur Durchsicht) ---
        add.accept(new ItemStack(ModItems.POWDER_SAWDUST.get()));
        add.accept(new ItemStack(ModItems.POWDER_YELLOWCAKE.get()));
        add.accept(new ItemStack(ModItems.POWDER_BALEFIRE.get()));
        add.accept(new ItemStack(ModItems.POWDER_PALEOGENITE.get()));
        add.accept(new ItemStack(ModItems.POWDER_THERMITE.get()));
        add.accept(new ItemStack(ModItems.POWDER_FERTILIZER.get()));
        add.accept(new ItemStack(ModItems.POWDER_FLUX.get()));
        add.accept(new ItemStack(ModItems.POWDER_MAGIC.get()));
        add.accept(new ItemStack(ModItems.POWDER_ICE.get()));
        add.accept(new ItemStack(ModItems.POWDER_SPARK_MIX.get()));
        add.accept(new ItemStack(ModItems.POWDER_SEMTEX_MIX.get()));
        add.accept(new ItemStack(ModItems.POWDER_DESH_READY.get()));
        add.accept(new ItemStack(ModItems.POWDER_COLTAN.get()));
    }

    // ТОПЛИВО И ЭЛЕМЕНТЫ МЕХАНИЗМОВ
    public static void populateFuelTab(BiConsumer<ItemStack, CreativeModeTab.TabVisibility> acceptor) {
        // ДОБАВЛЕНА ЗАЩИТА ОТ ДУБЛИКАТОВ, как в TemplatesTab и NukeTab:
        Set<String> seen = new HashSet<>();
        Consumer<ItemStack> add = stack -> {
            if (stack == null || stack.isEmpty()) return;
            String itemId = String.valueOf(BuiltInRegistries.ITEM.getKey(stack.getItem()));
            String tag = stack.getTag() == null ? "" : stack.getTag().toString();
            // Если такой предмет с таким же NBT уже добавлялся, пропускаем его, чтобы не сломать Поиск
            if (!seen.add(itemId + "|" + tag)) return;
            acceptor.accept(stack, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
        };
        add.accept(new ItemStack(ModItems.BLACK_HOLE.get()));
        add.accept(new ItemStack(ModItems.PELLET_ANTIMATTER.get()));
        add.accept(new ItemStack(ModItems.FLAME_PONY.get()));
        add.accept(new ItemStack(ModItems.CREATIVE_BATTERY.get()));
        add.accept(new ItemStack(ModItems.RBMK_FUEL_DRX.get()));

        // RBMK blocks
        // add.accept(new ItemStack(ModBlocks.RBMK_ROD.get()));
        // add.accept(new ItemStack(ModBlocks.RBMK_ROD_MOD.get()));
        // add.accept(new ItemStack(ModBlocks.RBMK_CONTROL.get()));
        // add.accept(new ItemStack(ModBlocks.RBMK_CONTROL_BLUE.get()));
        // add.accept(new ItemStack(ModBlocks.RBMK_CONTROL_GREEN.get()));
        // add.accept(new ItemStack(ModBlocks.RBMK_CONTROL_YELLOW.get()));
        // add.accept(new ItemStack(ModBlocks.RBMK_CONTROL_PURPLE.get()));
        // add.accept(new ItemStack(ModBlocks.RBMK_CONTROL_MOD.get()));
        // add.accept(new ItemStack(ModBlocks.RBMK_CONTROL_MOD_AUTO.get()));
        // add.accept(new ItemStack(ModBlocks.RBMK_CONTROL_AUTO.get()));
        // add.accept(new ItemStack(ModBlocks.RBMK_CONTROL_REASIM.get()));
        // add.accept(new ItemStack(ModBlocks.RBMK_CONTROL_REASIM_AUTO.get()));
        // add.accept(new ItemStack(ModBlocks.RBMK_MODERATOR.get()));
        // add.accept(new ItemStack(ModBlocks.RBMK_ABSORBER.get()));
        // add.accept(new ItemStack(ModBlocks.RBMK_REFLECTOR.get()));
        // add.accept(new ItemStack(ModBlocks.RBMK_COOLER.get()));
        // add.accept(new ItemStack(ModBlocks.RBMK_BOILER.get()));
        // add.accept(new ItemStack(ModBlocks.RBMK_HEATER.get()));
        // add.accept(new ItemStack(ModBlocks.RBMK_OUTGASSER.get()));
        // add.accept(new ItemStack(ModBlocks.RBMK_STORAGE.get()));
        // add.accept(new ItemStack(ModBlocks.RBMK_BLANK.get()));
        // add.accept(new ItemStack(ModBlocks.RBMK_STEAM_INLET.get()));
        // add.accept(new ItemStack(ModBlocks.RBMK_STEAM_OUTLET.get()));
        // add.accept(new ItemStack(ModBlocks.RBMK_LOADER.get()));
        // add.accept(new ItemStack(ModBlocks.RBMK_AUTOLOADER.get()));
        // add.accept(new ItemStack(ModBlocks.RBMK_CRANE_CONSOLE.get()));
        // add.accept(new ItemStack(ModBlocks.RBMK_DISPLAY.get()));
        // add.accept(new ItemStack(ModBlocks.RBMK_GAUGE.get()));
        // add.accept(new ItemStack(ModBlocks.RBMK_INDICATOR.get()));
        // add.accept(new ItemStack(ModBlocks.RBMK_LEVER.get()));
        // add.accept(new ItemStack(ModBlocks.RBMK_NUMITRON.get()));
        // add.accept(new ItemStack(ModBlocks.RBMK_GRAPH.get()));
        // add.accept(new ItemStack(ModBlocks.RBMK_TERMINAL.get()));
        // add.accept(new ItemStack(ModBlocks.RBMK_KEYPAD.get()));
        // add.accept(new ItemStack(ModBlocks.RBMK_DEBRIS.get()));
        // add.accept(new ItemStack(ModBlocks.RBMK_DEBRIS_BURNING.get()));
        // add.accept(new ItemStack(ModBlocks.RBMK_DEBRIS_DIGAMMA.get()));
        // add.accept(new ItemStack(ModBlocks.RBMK_DEBRIS_RADIATING.get()));

        // // RBMK items
        // add.accept(new ItemStack(ModItems.RBMK_LID.get()));
        // add.accept(new ItemStack(ModItems.RBMK_LID_GLASS.get()));
        // add.accept(new ItemStack(ModItems.RBMK_FUEL_EMPTY.get()));
        // add.accept(new ItemStack(ModItems.RBMK_FUEL_LEU235.get()));
        // add.accept(new ItemStack(ModItems.RBMK_FUEL_HEU235.get()));
        // add.accept(new ItemStack(ModItems.RBMK_FUEL_LEP.get()));
        // add.accept(new ItemStack(ModItems.RBMK_FUEL_HEP.get()));
        // add.accept(new ItemStack(ModItems.RBMK_FUEL_MOX.get()));
        // add.accept(new ItemStack(ModItems.RBMK_PELLET_LEU235.get()));
        // add.accept(new ItemStack(ModItems.RBMK_PELLET_HEU235.get()));
        // add.accept(new ItemStack(ModItems.RBMK_PELLET_LEP.get()));
        // add.accept(new ItemStack(ModItems.RBMK_PELLET_HEP.get()));
        // add.accept(new ItemStack(ModItems.RBMK_PELLET_MOX.get()));



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

        add.accept(new ItemStack(ModItems.ROD_ZIRNOX_EMPTY.get()));
        add.accept(new ItemStack(ModItems.ROD_ZIRNOX_LITHIUM.get()));
        add.accept(new ItemStack(ModItems.ROD_ZIRNOX_TRITIUM.get()));
        add.accept(new ItemStack(ModItems.ROD_ZIRNOX_TH232.get()));
        add.accept(new ItemStack(ModItems.ROD_ZIRNOX_LES_FUEL.get()));
        add.accept(new ItemStack(ModItems.ROD_ZIRNOX_LES_FUEL_DEPLETED.get()));
        add.accept(new ItemStack(ModItems.ROD_ZIRNOX_MOX_FUEL.get()));
        add.accept(new ItemStack(ModItems.ROD_ZIRNOX_MOX_FUEL_DEPLETED.get()));
        add.accept(new ItemStack(ModItems.ROD_ZIRNOX_NATURAL_URANIUM_FUEL.get()));
        add.accept(new ItemStack(ModItems.ROD_ZIRNOX_PLUTONIUM_FUEL.get()));
        add.accept(new ItemStack(ModItems.ROD_ZIRNOX_PLUTONIUM_FUEL_DEPLETED.get()));
        add.accept(new ItemStack(ModItems.ROD_ZIRNOX_THORIUM_FUEL.get()));
        add.accept(new ItemStack(ModItems.ROD_ZIRNOX_THORIUM_FUEL_DEPLETED.get()));
        add.accept(new ItemStack(ModItems.ROD_ZIRNOX_U233_FUEL.get()));
        add.accept(new ItemStack(ModItems.ROD_ZIRNOX_U233_FUEL_DEPLETED.get()));
        add.accept(new ItemStack(ModItems.ROD_ZIRNOX_U235_FUEL.get()));
        add.accept(new ItemStack(ModItems.ROD_ZIRNOX_U235_FUEL_DEPLETED.get()));
        add.accept(new ItemStack(ModItems.ROD_ZIRNOX_URANIUM_FUEL.get()));
        add.accept(new ItemStack(ModItems.ROD_ZIRNOX_URANIUM_FUEL_DEPLETED.get()));
        add.accept(new ItemStack(ModItems.ROD_ZIRNOX_ZFB_MOX.get()));
        add.accept(new ItemStack(ModItems.ROD_ZIRNOX_ZFB_MOX_DEPLETED.get()));

        add.accept(new ItemStack(ModItems.FLUID_BARREL.get()));
        for (ModFluids.FluidEntry entry : HbmFluidRegistry.getOrderedFluids()) {
            ItemStack filledBarrel = new ItemStack(ModItems.FLUID_BARREL.get());
            dev.architectury.fluid.FluidStack archFluidStack = dev.architectury.fluid.FluidStack.create(entry.getSource(), FluidBarrelItem.getPlatformCapacity());
            FluidBarrelItem.setFluid(filledBarrel, archFluidStack);
            add.accept(filledBarrel);
        }
        // Fluid Ducts - one per fluid type (neo / colored / silver styles)
        add.accept(new ItemStack(ModItems.FLUID_DUCT.get()));
        add.accept(new ItemStack(ModItems.FLUID_DUCT_COLORED.get()));
        add.accept(new ItemStack(ModItems.FLUID_DUCT_SILVER.get()));
        for (ModFluids.FluidEntry entry : HbmFluidRegistry.getOrderedFluids()) {
            add.accept(com.hbm_m.item.liquids.FluidDuctItem.createStack(ModItems.FLUID_DUCT.get(), entry));
            add.accept(com.hbm_m.item.liquids.FluidDuctItem.createStack(ModItems.FLUID_DUCT_COLORED.get(), entry));
            add.accept(com.hbm_m.item.liquids.FluidDuctItem.createStack(ModItems.FLUID_DUCT_SILVER.get(), entry));
        }
        // add.accept(new ItemStack(ModItems.FLUID_VALVE.get()));
        // add.accept(new ItemStack(ModItems.FLUID_PUMP.get()));
        // add.accept(new ItemStack(ModItems.FLUID_EXHAUST.get()));
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
        add.accept(new ItemStack(ModItems.ASSEMBLY_TEMPLATE.get()));
        add.accept(new ItemStack(ModItems.BLUEPRINT_FOLDER.get()));

        // Machine Upgrades
        add.accept(new ItemStack(ModItems.UPGRADE_SPEED_1.get()));
        add.accept(new ItemStack(ModItems.UPGRADE_SPEED_2.get()));
        add.accept(new ItemStack(ModItems.UPGRADE_SPEED_3.get()));
        add.accept(new ItemStack(ModItems.UPGRADE_EFFECT_1.get()));
        add.accept(new ItemStack(ModItems.UPGRADE_EFFECT_2.get()));
        add.accept(new ItemStack(ModItems.UPGRADE_EFFECT_3.get()));
        add.accept(new ItemStack(ModItems.UPGRADE_POWER_1.get()));
        add.accept(new ItemStack(ModItems.UPGRADE_POWER_2.get()));
        add.accept(new ItemStack(ModItems.UPGRADE_POWER_3.get()));
        add.accept(new ItemStack(ModItems.UPGRADE_FORTUNE_1.get()));
        add.accept(new ItemStack(ModItems.UPGRADE_FORTUNE_2.get()));
        add.accept(new ItemStack(ModItems.UPGRADE_FORTUNE_3.get()));
        add.accept(new ItemStack(ModItems.UPGRADE_AFTERBURN_1.get()));
        add.accept(new ItemStack(ModItems.UPGRADE_AFTERBURN_2.get()));
        add.accept(new ItemStack(ModItems.UPGRADE_AFTERBURN_3.get()));
        add.accept(new ItemStack(ModItems.UPGRADE_OVERDRIVE_1.get()));
        add.accept(new ItemStack(ModItems.UPGRADE_OVERDRIVE_2.get()));
        add.accept(new ItemStack(ModItems.UPGRADE_OVERDRIVE_3.get()));

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
