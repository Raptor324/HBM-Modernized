package com.hbm_m.main;

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
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fml.DistExecutor;

import dev.architectury.registry.registries.RegistrySupplier;

import java.util.List;

/**
 * Наполнение креативных вкладок (логика из старого Forge {@code MainRegistry#addCreative}).
 */
public final class CreativeModeTabEventHandler {

    private CreativeModeTabEventHandler() {
    }
    public static void onBuildCreativeModeTabContents(BuildCreativeModeTabContentsEvent event) {
        // Логгирование для отладки
        MainRegistry.LOGGER.info("Building creative tab contents for: " + event.getTabKey());

        if (event.getTab() == ModCreativeTabs.NTM_WEAPONS_TAB.get() || event.getTabKey() == CreativeModeTabs.SEARCH) {
            event.accept(ModBlocks.BARREL_RED.get());
            event.accept(ModBlocks.BARREL_PINK.get());
            event.accept(ModBlocks.BARREL_TAINT.get());
            event.accept(ModBlocks.BARREL_LOX.get());
            event.accept(ModBlocks.BARREL_YELLOW.get());
            event.accept(ModBlocks.BARREL_VITRIFIED.get());
            event.accept(ModBlocks.BARBED_WIRE_FIRE.get());
            event.accept(ModBlocks.BARBED_WIRE_POISON.get());
            event.accept(ModBlocks.BARBED_WIRE_RAD.get());
            event.accept(ModBlocks.BARBED_WIRE_WITHER.get());
            event.accept(ModBlocks.BARBED_WIRE.get());
            event.accept(ModItems.DETONATOR.get());
            event.accept(ModItems.MULTI_DETONATOR.get());
            event.accept(ModItems.RANGE_DETONATOR.get());
            event.accept(ModItems.GRENADE.get());
            event.accept(ModItems.GRENADEHE.get());
            event.accept(ModItems.GRENADEFIRE.get());
            event.accept(ModItems.GRENADESMART.get());
            event.accept(ModItems.GRENADESLIME.get());
            event.accept(ModItems.GRENADE_IF.get());
            event.accept(ModItems.GRENADE_IF_HE.get());
            event.accept(ModItems.GRENADE_IF_SLIME.get());
            event.accept(ModItems.GRENADE_IF_FIRE.get());
            event.accept(ModItems.GRENADE_NUC.get());
            event.accept(ModBlocks.MINE_AP.get());
            event.accept(ModBlocks.MINE_FAT.get());
            event.accept(ModBlocks.NUKE_FAT_MAN.get());
            event.accept(ModItems.FAT_MAN_EXPLOSIVE.get());
            event.accept(ModItems.FAT_MAN_IGNITER.get());
            event.accept(ModItems.FAT_MAN_CORE.get());
            event.accept(ModBlocks.AIRBOMB.get());
            event.accept(ModItems.AIRBOMB_A.get());
            event.accept(ModBlocks.BALEBOMB_TEST.get());
            event.accept(ModItems.AIRNUKEBOMB_A.get());
            event.accept(ModBlocks.DET_MINER.get());
            event.accept(ModBlocks.GIGA_DET.get());
            event.accept(ModBlocks.WASTE_CHARGE.get());
            event.accept(ModBlocks.SMOKE_BOMB.get());
            event.accept(ModBlocks.EXPLOSIVE_CHARGE.get());
            event.accept(ModBlocks.NUCLEAR_CHARGE.get());
            event.accept(ModBlocks.C4.get());
            event.accept(ModBlocks.DUD_CONVENTIONAL.get());
            event.accept(ModBlocks.DUD_NUKE.get());
            event.accept(ModBlocks.DUD_SALTED.get());
            event.accept(ModBlocks.LAUNCH_PAD.get());
            event.accept(ModBlocks.LAUNCH_PAD_RUSTED.get());
            event.accept(ModItems.DESIGNATOR.get());
            event.accept(ModItems.DESIGNATOR_RANGE.get());
            event.accept(ModItems.DESIGNATOR_MANUAL.get());
            event.accept(ModItems.MISSILE_TEST.get());

            if (ModClothConfig.get().enableDebugLogging) {
                MainRegistry.LOGGER.info("Added Alloy Sword to NTM Weapons tab");
            }
        }

        // БРОНЯ И ИНСТРУМЕНТЫ
        if (event.getTabKey() == CreativeModeTabs.COMBAT) {

            event.accept(ModItems.ALLOY_SWORD.get());
            event.accept(ModItems.ALLOY_AXE.get());
            event.accept(ModItems.ALLOY_PICKAXE.get());
            event.accept(ModItems.ALLOY_HOE.get());
            event.accept(ModItems.ALLOY_SHOVEL.get());
            event.accept(ModItems.STEEL_SWORD.get());
            event.accept(ModItems.STEEL_AXE.get());
            event.accept(ModItems.STEEL_PICKAXE.get());
            event.accept(ModItems.STEEL_HOE.get());
            event.accept(ModItems.STEEL_SHOVEL.get());
            event.accept(ModItems.TITANIUM_SWORD.get());
            event.accept(ModItems.TITANIUM_AXE.get());
            event.accept(ModItems.TITANIUM_PICKAXE.get());
            event.accept(ModItems.TITANIUM_HOE.get());
            event.accept(ModItems.TITANIUM_SHOVEL.get());
            event.accept(ModItems.STARMETAL_SWORD.get());
            event.accept(ModItems.STARMETAL_AXE.get());
            event.accept(ModItems.STARMETAL_PICKAXE.get());
            event.accept(ModItems.STARMETAL_HOE.get());
            event.accept(ModItems.STARMETAL_SHOVEL.get());
            event.accept(ModItems.METEORITE_SWORD.get());
            event.accept(ModItems.METEORITE_SWORD_SEARED.get());

            // Силовая броня добавляется полностью заряженной
            event.accept(createChargedArmorStack(ModItems.T51_HELMET.get()));
            event.accept(createChargedArmorStack(ModItems.T51_CHESTPLATE.get()));
            event.accept(createChargedArmorStack(ModItems.T51_LEGGINGS.get()));
            event.accept(createChargedArmorStack(ModItems.T51_BOOTS.get()));

            event.accept(createChargedArmorStack(ModItems.AJR_HELMET.get()));
            event.accept(createChargedArmorStack(ModItems.AJR_CHESTPLATE.get()));
            event.accept(createChargedArmorStack(ModItems.AJR_LEGGINGS.get()));
            event.accept(createChargedArmorStack(ModItems.AJR_BOOTS.get()));

            event.accept(createChargedArmorStack(ModItems.AJRO_HELMET.get()));
            event.accept(createChargedArmorStack(ModItems.AJRO_CHESTPLATE.get()));
            event.accept(createChargedArmorStack(ModItems.AJRO_LEGGINGS.get()));
            event.accept(createChargedArmorStack(ModItems.AJRO_BOOTS.get()));

            event.accept(createChargedArmorStack(ModItems.BISMUTH_HELMET.get()));
            event.accept(createChargedArmorStack(ModItems.BISMUTH_CHESTPLATE.get()));
            event.accept(createChargedArmorStack(ModItems.BISMUTH_LEGGINGS.get()));
            event.accept(createChargedArmorStack(ModItems.BISMUTH_BOOTS.get()));
            
            event.accept(createChargedArmorStack(ModItems.DNT_HELMET.get()));
            event.accept(createChargedArmorStack(ModItems.DNT_CHESTPLATE.get()));
            event.accept(createChargedArmorStack(ModItems.DNT_LEGGINGS.get()));
            event.accept(createChargedArmorStack(ModItems.DNT_BOOTS.get()));
            
            event.accept(ModItems.ALLOY_HELMET.get());
            event.accept(ModItems.ALLOY_CHESTPLATE.get());
            event.accept(ModItems.ALLOY_LEGGINGS.get());
            event.accept(ModItems.ALLOY_BOOTS.get());
            event.accept(ModItems.COBALT_HELMET.get());
            event.accept(ModItems.COBALT_CHESTPLATE.get());
            event.accept(ModItems.COBALT_LEGGINGS.get());
            event.accept(ModItems.COBALT_BOOTS.get());
            event.accept(ModItems.TITANIUM_HELMET.get());
            event.accept(ModItems.TITANIUM_CHESTPLATE.get());
            event.accept(ModItems.TITANIUM_LEGGINGS.get());
            event.accept(ModItems.TITANIUM_BOOTS.get());
            event.accept(ModItems.SECURITY_HELMET.get());
            event.accept(ModItems.SECURITY_CHESTPLATE.get());
            event.accept(ModItems.SECURITY_LEGGINGS.get());
            event.accept(ModItems.SECURITY_BOOTS.get());
        
            event.accept(ModItems.STEEL_HELMET.get());
            event.accept(ModItems.STEEL_CHESTPLATE.get());
            event.accept(ModItems.STEEL_LEGGINGS.get());
            event.accept(ModItems.STEEL_BOOTS.get());
            event.accept(ModItems.ASBESTOS_HELMET.get());
            event.accept(ModItems.ASBESTOS_CHESTPLATE.get());
            event.accept(ModItems.ASBESTOS_LEGGINGS.get());
            event.accept(ModItems.ASBESTOS_BOOTS.get());
            event.accept(ModItems.HAZMAT_HELMET.get());
            event.accept(ModItems.HAZMAT_CHESTPLATE.get());
            event.accept(ModItems.HAZMAT_LEGGINGS.get());
            event.accept(ModItems.HAZMAT_BOOTS.get());
            event.accept(ModItems.LIQUIDATOR_HELMET.get());
            event.accept(ModItems.LIQUIDATOR_CHESTPLATE.get());
            event.accept(ModItems.LIQUIDATOR_LEGGINGS.get());
            event.accept(ModItems.LIQUIDATOR_BOOTS.get());
            event.accept(ModItems.PAA_HELMET.get());
            event.accept(ModItems.PAA_CHESTPLATE.get());
            event.accept(ModItems.PAA_LEGGINGS.get());
            event.accept(ModItems.PAA_BOOTS.get());
            event.accept(ModItems.STARMETAL_HELMET.get());
            event.accept(ModItems.STARMETAL_CHESTPLATE.get());
            event.accept(ModItems.STARMETAL_LEGGINGS.get());
            event.accept(ModItems.STARMETAL_BOOTS.get());

            // БРОНЯ
            event.accept(ModItems.TITANIUM_HELMET.get());
            event.accept(ModItems.TITANIUM_CHESTPLATE.get());
            event.accept(ModItems.TITANIUM_LEGGINGS.get());
            event.accept(ModItems.TITANIUM_BOOTS.get());

            event.accept(ModItems.COBALT_HELMET.get());
            event.accept(ModItems.COBALT_CHESTPLATE.get());
            event.accept(ModItems.COBALT_LEGGINGS.get());
            event.accept(ModItems.COBALT_BOOTS.get());

            event.accept(ModItems.STEEL_HELMET.get());
            event.accept(ModItems.STEEL_CHESTPLATE.get());
            event.accept(ModItems.STEEL_LEGGINGS.get());
            event.accept(ModItems.STEEL_BOOTS.get());

            event.accept(ModItems.ALLOY_HELMET.get());
            event.accept(ModItems.ALLOY_CHESTPLATE.get());
            event.accept(ModItems.ALLOY_LEGGINGS.get());
            event.accept(ModItems.ALLOY_BOOTS.get());

            event.accept(ModItems.STARMETAL_HELMET.get());
            event.accept(ModItems.STARMETAL_CHESTPLATE.get());
            event.accept(ModItems.STARMETAL_LEGGINGS.get());
            event.accept(ModItems.STARMETAL_BOOTS.get());

            //СПЕЦ БРОНЯ
            event.accept(ModItems.SECURITY_HELMET.get());
            event.accept(ModItems.SECURITY_CHESTPLATE.get());
            event.accept(ModItems.SECURITY_LEGGINGS.get());
            event.accept(ModItems.SECURITY_BOOTS.get());

            event.accept(ModItems.ASBESTOS_HELMET.get());
            event.accept(ModItems.ASBESTOS_CHESTPLATE.get());
            event.accept(ModItems.ASBESTOS_LEGGINGS.get());
            event.accept(ModItems.ASBESTOS_BOOTS.get());

            event.accept(ModItems.HAZMAT_HELMET.get());
            event.accept(ModItems.HAZMAT_CHESTPLATE.get());
            event.accept(ModItems.HAZMAT_LEGGINGS.get());
            event.accept(ModItems.HAZMAT_BOOTS.get());

            event.accept(ModItems.PAA_HELMET.get());
            event.accept(ModItems.PAA_CHESTPLATE.get());
            event.accept(ModItems.PAA_LEGGINGS.get());
            event.accept(ModItems.PAA_BOOTS.get());

            event.accept(ModItems.LIQUIDATOR_HELMET.get());
            event.accept(ModItems.LIQUIDATOR_CHESTPLATE.get());
            event.accept(ModItems.LIQUIDATOR_LEGGINGS.get());
            event.accept(ModItems.LIQUIDATOR_BOOTS.get());


            //МЕЧИ
            event.accept(ModItems.TITANIUM_SWORD.get());
            event.accept(ModItems.STEEL_SWORD.get());
            event.accept(ModItems.ALLOY_SWORD.get());
            event.accept(ModItems.STARMETAL_SWORD.get());

            //ТОПОРЫ
            event.accept(ModItems.TITANIUM_AXE.get());
            event.accept(ModItems.STEEL_AXE.get());
            event.accept(ModItems.ALLOY_AXE.get());
            event.accept(ModItems.STARMETAL_AXE.get());

            //КИРКИ
            event.accept(ModItems.TITANIUM_PICKAXE.get());
            event.accept(ModItems.STEEL_PICKAXE.get());
            event.accept(ModItems.ALLOY_PICKAXE.get());
            event.accept(ModItems.STARMETAL_PICKAXE.get());

            //ЛОПАТЫ
            event.accept(ModItems.TITANIUM_SHOVEL.get());
            event.accept(ModItems.STEEL_SHOVEL.get());
            event.accept(ModItems.ALLOY_SHOVEL.get());
            event.accept(ModItems.STARMETAL_SHOVEL.get());

            //МОТЫГИ
            event.accept(ModItems.TITANIUM_HOE.get());
            event.accept(ModItems.STEEL_HOE.get());
            event.accept(ModItems.ALLOY_HOE.get());
            event.accept(ModItems.STARMETAL_HOE.get());

            //СПЕЦ. ИНСТРУМЕНТЫ

            if (ModClothConfig.get().enableDebugLogging) {
                MainRegistry.LOGGER.info("Added Alloy Sword to vanilla Combat tab");
            }
        }

        // СЛИТКИ И РЕСУРСЫ
        if (event.getTab() == ModCreativeTabs.NTM_RESOURCES_TAB.get() || event.getTabKey() == CreativeModeTabs.SEARCH) {

            // БАЗОВЫЕ ПРЕДМЕТЫ (все с ItemStack!)
            event.accept(new ItemStack(ModItems.BALL_TNT.get()));
            event.accept(new ItemStack(ModItems.ZIRCONIUM_SHARP.get()));
            event.accept(new ItemStack(ModItems.BORAX.get()));
            event.accept(new ItemStack(ModItems.DUST.get()));
            event.accept(new ItemStack(ModItems.DUST_TINY.get()));
            event.accept(new ItemStack(ModItems.CINNABAR.get()));
            event.accept(new ItemStack(ModItems.FIRECLAY_BALL.get()));
            event.accept(new ItemStack(ModItems.SULFUR.get()));
            event.accept(new ItemStack(ModItems.SEQUESTRUM.get()));
            event.accept(new ItemStack(ModItems.LIGNITE.get()));
            event.accept(new ItemStack(ModItems.FLUORITE.get()));
            event.accept(new ItemStack(ModItems.RAREGROUND_ORE_CHUNK.get()));
            event.accept(new ItemStack(ModItems.FIREBRICK.get()));
            event.accept(new ItemStack(ModItems.WOOD_ASH_POWDER.get()));
            event.accept(new ItemStack(ModItems.SCRAP.get()));
            event.accept(new ItemStack(ModItems.NUGGET_SILICON.get()));
            event.accept(new ItemStack(ModItems.BILLET_SILICON.get()));
            event.accept(new ItemStack(ModItems.BILLET_PLUTONIUM.get()));



            // Crystals (textures/crystall/*.png)
            event.accept(new ItemStack(ModItems.CRYSTAL_ALUMINIUM.get()));
            event.accept(new ItemStack(ModItems.CRYSTAL_BERYLLIUM.get()));
            event.accept(new ItemStack(ModItems.CRYSTAL_CHARRED.get()));
            event.accept(new ItemStack(ModItems.CRYSTAL_CINNEBAR.get()));
            event.accept(new ItemStack(ModItems.CRYSTAL_COAL.get()));
            event.accept(new ItemStack(ModItems.CRYSTAL_COBALT.get()));
            event.accept(new ItemStack(ModItems.CRYSTAL_COPPER.get()));
            event.accept(new ItemStack(ModItems.CRYSTAL_DIAMOND.get()));
            event.accept(new ItemStack(ModItems.CRYSTAL_FLUORITE.get()));
            event.accept(new ItemStack(ModItems.CRYSTAL_GOLD.get()));
            event.accept(new ItemStack(ModItems.CRYSTAL_HARDENED.get()));
            event.accept(new ItemStack(ModItems.CRYSTAL_HORN.get()));
            event.accept(new ItemStack(ModItems.CRYSTAL_IRON.get()));
            event.accept(new ItemStack(ModItems.CRYSTAL_LAPIS.get()));
            event.accept(new ItemStack(ModItems.CRYSTAL_LEAD.get()));
            event.accept(new ItemStack(ModItems.CRYSTAL_LITHIUM.get()));
            event.accept(new ItemStack(ModItems.CRYSTAL_NITER.get()));
            event.accept(new ItemStack(ModItems.CRYSTAL_OSMIRIDIUM.get()));
            event.accept(new ItemStack(ModItems.CRYSTAL_PHOSPHORUS.get()));
            event.accept(new ItemStack(ModItems.CRYSTAL_PLUTONIUM.get()));
            event.accept(new ItemStack(ModItems.CRYSTAL_PULSAR.get()));
            event.accept(new ItemStack(ModItems.CRYSTAL_RARE.get()));
            event.accept(new ItemStack(ModItems.CRYSTAL_REDSTONE.get()));
            event.accept(new ItemStack(ModItems.CRYSTAL_SCHRABIDIUM.get()));
            event.accept(new ItemStack(ModItems.CRYSTAL_SCHRARANIUM.get()));
            event.accept(new ItemStack(ModItems.CRYSTAL_STARMETAL.get()));
            event.accept(new ItemStack(ModItems.CRYSTAL_SULFUR.get()));
            event.accept(new ItemStack(ModItems.CRYSTAL_THORIUM.get()));
            event.accept(new ItemStack(ModItems.CRYSTAL_TITANIUM.get()));
            event.accept(new ItemStack(ModItems.CRYSTAL_TRIXITE.get()));
            event.accept(new ItemStack(ModItems.CRYSTAL_TUNGSTEN.get()));
            event.accept(new ItemStack(ModItems.CRYSTAL_URANIUM.get()));
            event.accept(new ItemStack(ModItems.CRYSTAL_VIRUS.get()));
            event.accept(new ItemStack(ModItems.CRYSTAL_XEN.get()));
                  

            // СЛИТКИ
            for (ModIngots ingot : ModIngots.values()) {
                RegistrySupplier<Item> ingotItem = ModItems.getIngot(ingot);
                if (ingotItem != null && ingotItem.isPresent()) {
                    event.accept(new ItemStack(ingotItem.get()));
                }
              
            }

            // Standalone tiny powders
            event.accept(new ItemStack(ModItems.LITHIUM_POWDER_TINY.get()));
            event.accept(new ItemStack(ModItems.CS137_POWDER_TINY.get()));
            event.accept(new ItemStack(ModItems.I131_POWDER_TINY.get()));
            event.accept(new ItemStack(ModItems.XE135_POWDER_TINY.get()));
            event.accept(new ItemStack(ModItems.PALEOGENITE_POWDER_TINY.get()));
            event.accept(new ItemStack(ModItems.NUCLEAR_WASTE_TINY.get()));
            event.accept(new ItemStack(ModItems.NUCLEAR_WASTE_LONG_TINY.get()));
            event.accept(new ItemStack(ModItems.NUCLEAR_WASTE_LONG_DEPLETED_TINY.get()));
            event.accept(new ItemStack(ModItems.NUCLEAR_WASTE_SHORT_TINY.get()));
            event.accept(new ItemStack(ModItems.NUCLEAR_WASTE_SHORT_DEPLETED_TINY.get()));
            event.accept(new ItemStack(ModItems.NUCLEAR_WASTE_VITRIFIED_TINY.get()));
            event.accept(new ItemStack(ModItems.NUGGET_MERCURY_TINY.get()));
            event.accept(new ItemStack(ModItems.COAL_POWDER_TINY.get()));

            // Standalone powders
            event.accept(new ItemStack(ModItems.COPPER_POWDER.get()));
            event.accept(new ItemStack(ModItems.DIAMOND_POWDER.get()));
            event.accept(new ItemStack(ModItems.EMERALD_POWDER.get()));
            event.accept(new ItemStack(ModItems.LAPIS_POWDER.get()));
            event.accept(new ItemStack(ModItems.QUARTZ_POWDER.get()));
            event.accept(new ItemStack(ModItems.LIGNITE_POWDER.get()));
            event.accept(new ItemStack(ModItems.FIRE_POWDER.get()));
            event.accept(new ItemStack(ModItems.LITHIUM_POWDER.get()));

            // ModPowders
            for (ModPowders powder : ModPowders.values()) {
                RegistrySupplier<Item> powderItem = ModItems.getPowders(powder);
                if (powderItem != null && powderItem.isPresent()) {
                    event.accept(new ItemStack(powderItem.get()));
                }
            }

            // ОДИН ЦИКЛ ДЛЯ ВСЕХ ПОРОШКОВ ИЗ СЛИТКОВ (обычные + маленькие)
            for (ModIngots ingot : ModIngots.values()) {
                // Обычный порошок
                RegistrySupplier<Item> powder = ModItems.getPowder(ingot);
                if (powder != null && powder.isPresent()) {
                    event.accept(new ItemStack(powder.get()));
                }

                // Маленький порошок
                ModItems.getTinyPowder(ingot).ifPresent(tiny -> {
                    if (tiny != null && tiny.isPresent()) {
                        event.accept(new ItemStack(tiny.get()));
                    }
                });
            }
        }

        // РАСХОДНИКИ И МОДИФИКАТОРЫ
        if (event.getTab() == ModCreativeTabs.NTM_CONSUMABLES_TAB.get() || event.getTabKey() == CreativeModeTabs.SEARCH) {
            event.accept(ModBlocks.ARMOR_TABLE.get());
            // АВТОМАТИЧЕСКОЕ ДОБАВЛЕНИЕ ВСЕХ МОДИФИКАТОРОВ
            // 1. Получаем все зарегистрированные предметы
            for (RegistrySupplier<Item> itemObject : ModItems.ITEMS) {
                if (!itemObject.isPresent()) {
                    continue;
                }
                Item item = itemObject.get();
                if (item instanceof ItemArmorMod) {
                    event.accept(item);
                    if (ModClothConfig.get().enableDebugLogging) {
                        MainRegistry.LOGGER.info("Automatically added Armor Mod [{}] to NTM Consumables tab", itemObject.getId());
                    }
                }
            }
            event.accept(ModItems.RADAWAY.get());
            event.accept(ModItems.CAN_KEY.get());
            event.accept(ModItems.CAN_EMPTY.get());
            event.accept(ModItems.CANNED_ASBESTOS.get());
            event.accept(ModItems.CANNED_ASS.get());
            event.accept(ModItems.CANNED_BARK.get());
            event.accept(ModItems.CANNED_BEEF.get());
            event.accept(ModItems.CANNED_BHOLE.get());
            event.accept(ModItems.CANNED_CHEESE.get());
            event.accept(ModItems.CANNED_CHINESE.get());
            event.accept(ModItems.CANNED_DIESEL.get());
            event.accept(ModItems.CANNED_FIST.get());
            event.accept(ModItems.CANNED_FRIED.get());
            event.accept(ModItems.CANNED_HOTDOGS.get());
            event.accept(ModItems.CANNED_JIZZ.get());
            event.accept(ModItems.CANNED_KEROSENE.get());
            event.accept(ModItems.CANNED_LEFTOVERS.get());
            event.accept(ModItems.CANNED_MILK.get());
            event.accept(ModItems.CANNED_MYSTERY.get());
            event.accept(ModItems.CANNED_NAPALM.get());
            event.accept(ModItems.CANNED_OIL.get());
            event.accept(ModItems.CANNED_PASHTET.get());
            event.accept(ModItems.CANNED_PIZZA.get());
            event.accept(ModItems.CANNED_RECURSION.get());
            event.accept(ModItems.CANNED_SPAM.get());
            event.accept(ModItems.CANNED_STEW.get());
            event.accept(ModItems.CANNED_TOMATO.get());
            event.accept(ModItems.CANNED_TUNA.get());
            event.accept(ModItems.CANNED_TUBE.get());
            event.accept(ModItems.CANNED_YOGURT.get());
            event.accept(ModItems.CAN_BEPIS.get());
            event.accept(ModItems.CAN_BREEN.get());
            event.accept(ModItems.CAN_CREATURE.get());
            event.accept(ModItems.CAN_LUNA.get());
            event.accept(ModItems.CAN_MRSUGAR.get());
            event.accept(ModItems.CAN_MUG.get());
            event.accept(ModItems.CAN_OVERCHARGE.get());
            event.accept(ModItems.CAN_REDBOMB.get());
            event.accept(ModItems.CAN_SMART.get());

            event.accept(ModItems.DEFUSER.get());
            event.accept(ModItems.CROWBAR.get());
            event.accept(ModItems.SCREWDRIVER.get());

            event.accept(ModItems.DOSIMETER.get());
            event.accept(ModItems.GEIGER_COUNTER.get());
            event.accept(ModBlocks.GEIGER_COUNTER_BLOCK.get());

            event.accept(ModItems.OIL_DETECTOR.get());
            event.accept(ModItems.DEPTH_ORES_SCANNER.get());

            event.accept(ModItems.AIRSTRIKE_TEST.get());
            event.accept(ModItems.AIRSTRIKE_HEAVY.get());
            event.accept(ModItems.AIRSTRIKE_AGENT.get());
            event.accept(ModItems.AIRSTRIKE_NUKE.get());
        }

        // ЗАПЧАСТИ
        if (event.getTab() == ModCreativeTabs.NTM_SPAREPARTS_TAB.get() || event.getTabKey() == CreativeModeTabs.SEARCH) {
            event.accept(ModItems.BOLT_STEEL.get());
            event.accept(ModItems.COIL_TUNGSTEN.get());

            event.accept(ModItems.PLATE_IRON.get());
            event.accept(ModItems.PLATE_ALUMINUM.get());
            event.accept(ModItems.PLATE_TITANIUM.get());
            event.accept(ModItems.PLATE_LEAD.get());
            event.accept(ModItems.PLATE_COPPER.get());
            event.accept(ModItems.PLATE_STEEL.get());
            event.accept(ModItems.PLATE_GOLD.get());
            event.accept(ModItems.PLATE_ADVANCED_ALLOY.get());
            event.accept(ModItems.PLATE_GUNMETAL.get());
            event.accept(ModItems.PLATE_GUNSTEEL.get());
            event.accept(ModItems.PLATE_DURA_STEEL.get());
            event.accept(ModItems.PLATE_KEVLAR.get());
            event.accept(ModItems.PLATE_PAA.get());
            event.accept(ModItems.PLATE_SCHRABIDIUM.get());
            event.accept(ModItems.PLATE_SATURNITE.get());
            event.accept(ModItems.PLATE_COMBINE_STEEL.get());
            event.accept(ModItems.PLATE_FUEL_MOX.get());
            event.accept(ModItems.PLATE_FUEL_PU238BE.get());
            event.accept(ModItems.PLATE_FUEL_PU239.get());
            event.accept(ModItems.PLATE_FUEL_RA226BE.get());
            event.accept(ModItems.PLATE_FUEL_SA326.get());
            event.accept(ModItems.PLATE_FUEL_U233.get());
            event.accept(ModItems.PLATE_FUEL_U235.get());

            event.accept(ModItems.WIRE_FINE.get());
            event.accept(ModItems.WIRE_ALUMINIUM.get());
            event.accept(ModItems.WIRE_CARBON.get());
            event.accept(ModItems.WIRE_TUNGSTEN.get());
            event.accept(ModItems.WIRE_GOLD.get());
            event.accept(ModItems.WIRE_COPPER.get());
            event.accept(ModItems.WIRE_RED_COPPER.get());
            event.accept(ModItems.WIRE_ADVANCED_ALLOY.get());
            event.accept(ModItems.WIRE_MAGNETIZED_TUNGSTEN.get());
            event.accept(ModItems.WIRE_SCHRABIDIUM.get());

            event.accept(ModItems.COIL_COPPER.get());
            event.accept(ModItems.COIL_ADVANCED_ALLOY.get());
            event.accept(ModItems.COIL_GOLD.get());
            event.accept(ModItems.COIL_MAGNETIZED_TUNGSTEN.get());
            event.accept(ModItems.COIL_COPPER_TORUS.get());
            event.accept(ModItems.COIL_ADVANCED_ALLOY_TORUS.get());
            event.accept(ModItems.COIL_GOLD_TORUS.get());
            event.accept(ModItems.COIL_MAGNETIZED_TUNGSTEN_TORUS.get());

            // Mineral Pipes
            event.accept(ModItems.PIPE_IRON.get());
            event.accept(ModItems.PIPE_COPPER.get());
            event.accept(ModItems.PIPE_GOLD.get());
            event.accept(ModItems.PIPE_LEAD.get());
            event.accept(ModItems.PIPE_STEEL.get());
            event.accept(ModItems.PIPE_TUNGSTEN.get());
            event.accept(ModItems.PIPE_TITANIUM.get());
            event.accept(ModItems.PIPE_ALUMINUM.get());

            event.accept(ModItems.PLATE_ARMOR_TITANIUM.get());
            event.accept(ModItems.PLATE_ARMOR_AJR.get());
            event.accept(ModItems.PLATE_ARMOR_LUNAR.get());
            event.accept(ModItems.PLATE_ARMOR_HEV.get());
            event.accept(ModItems.PLATE_ARMOR_DNT.get());
            event.accept(ModItems.PLATE_ARMOR_DNT_RUSTED.get());
            event.accept(ModItems.PLATE_ARMOR_FAU.get());

            event.accept(ModItems.PLATE_MIXED.get());
            event.accept(ModItems.PLATE_DALEKANIUM.get());
            event.accept(ModItems.PLATE_DESH.get());
            event.accept(ModItems.PLATE_BISMUTH.get());
            event.accept(ModItems.PLATE_EUPHEMIUM.get());
            event.accept(ModItems.PLATE_DINEUTRONIUM.get());

            event.accept(ModItems.PLATE_CAST.get());
            event.accept(ModItems.PLATE_CAST_ALT.get());
            event.accept(ModItems.PLATE_CAST_BISMUTH.get());
            event.accept(ModItems.PLATE_CAST_DARK.get());

            event.accept(ModItems.MOTOR.get());
            event.accept(ModItems.MOTOR_DESH.get());
            event.accept(ModItems.MOTOR_BISMUTH.get());

            event.accept(ModItems.INSULATOR.get());
            event.accept(ModItems.SILICON_CIRCUIT.get());
            event.accept(ModItems.PCB.get());
            event.accept(ModItems.CRT_DISPLAY.get());
            event.accept(ModItems.VACUUM_TUBE.get());
            event.accept(ModItems.CAPACITOR.get());
            event.accept(ModItems.MICROCHIP.get());
            event.accept(ModItems.ANALOG_CIRCUIT.get());
            event.accept(ModItems.INTEGRATED_CIRCUIT.get());
            event.accept(ModItems.ADVANCED_CIRCUIT.get());
            event.accept(ModItems.CAPACITOR_BOARD.get());

            event.accept(ModItems.CONTROLLER_CHASSIS.get());
            event.accept(ModItems.CONTROLLER.get());
            event.accept(ModItems.CONTROLLER_ADVANCED.get());
            event.accept(ModItems.CAPACITOR_TANTALUM.get());
            event.accept(ModItems.BISMOID_CHIP.get());
            event.accept(ModItems.BISMOID_CIRCUIT.get());
            event.accept(ModItems.ATOMIC_CLOCK.get());
            event.accept(ModItems.QUANTUM_CHIP.get());
            event.accept(ModItems.QUANTUM_CIRCUIT.get());
            event.accept(ModItems.QUANTUM_COMPUTER.get());

            event.accept(ModItems.BATTLE_GEARS.get());
            event.accept(ModItems.BATTLE_SENSOR.get());
            event.accept(ModItems.BATTLE_CASING.get());
            event.accept(ModItems.BATTLE_COUNTER.get());
            event.accept(ModItems.BATTLE_MODULE.get());
            event.accept(ModItems.METAL_ROD.get());
        }
        // РУДЫ
        if (event.getTab() == ModCreativeTabs.NTM_ORES_TAB.get() || event.getTabKey() == CreativeModeTabs.SEARCH) {

            event.accept(ModBlocks.DEPTH_STONE.get());
            event.accept(ModBlocks.DEPTH_STONE_NETHER.get());

            event.accept(ModBlocks.DEPTH_BORAX.get());
            event.accept(ModBlocks.DEPTH_IRON.get());
            event.accept(ModBlocks.DEPTH_TITANIUM.get());
            event.accept(ModBlocks.DEPTH_TUNGSTEN.get());
            event.accept(ModBlocks.DEPTH_CINNABAR.get());
            event.accept(ModBlocks.DEPTH_ZIRCONIUM.get());
            event.accept(ModBlocks.BEDROCK_OIL.get());

            event.accept(ModBlocks.ORE_OIL.get());
            event.accept(ModBlocks.GNEISS_STONE.get());
            event.accept(ModBlocks.FLUORITE_ORE.get());
            event.accept(ModBlocks.LIGNITE_ORE.get());
            event.accept(ModBlocks.TUNGSTEN_ORE.get());
            event.accept(ModBlocks.ASBESTOS_ORE.get());
            event.accept(ModBlocks.SULFUR_ORE.get());
            event.accept(ModBlocks.SEQUESTRUM_ORE.get());

            event.accept(ModBlocks.ALUMINUM_ORE.get());
            event.accept(ModBlocks.ALUMINUM_ORE_DEEPSLATE.get());
            event.accept(ModBlocks.TITANIUM_ORE.get());
            event.accept(ModBlocks.TITANIUM_ORE_DEEPSLATE.get());
            event.accept(ModBlocks.COBALT_ORE.get());
            event.accept(ModBlocks.COBALT_ORE_DEEPSLATE.get());
            event.accept(ModBlocks.THORIUM_ORE.get());
            event.accept(ModBlocks.THORIUM_ORE_DEEPSLATE.get());
            event.accept(ModBlocks.RAREGROUND_ORE.get());
            event.accept(ModBlocks.RAREGROUND_ORE_DEEPSLATE.get());
            event.accept(ModBlocks.BERYLLIUM_ORE.get());
            event.accept(ModBlocks.BERYLLIUM_ORE_DEEPSLATE.get());
            event.accept(ModBlocks.LEAD_ORE.get());
            event.accept(ModBlocks.LEAD_ORE_DEEPSLATE.get());
            event.accept(ModBlocks.CINNABAR_ORE.get());
            event.accept(ModBlocks.CINNABAR_ORE_DEEPSLATE.get());
            event.accept(ModBlocks.URANIUM_ORE.get());
            event.accept(ModBlocks.URANIUM_ORE_DEEPSLATE.get());

            event.accept(ModBlocks.RESOURCE_ASBESTOS.get());
            event.accept(ModBlocks.RESOURCE_BAUXITE.get());
            event.accept(ModBlocks.RESOURCE_HEMATITE.get());
            event.accept(ModBlocks.RESOURCE_LIMESTONE.get());
            event.accept(ModBlocks.RESOURCE_MALACHITE.get());
            event.accept(ModBlocks.RESOURCE_SULFUR.get());

            event.accept(ModItems.ALUMINUM_RAW.get());
            event.accept(ModItems.BERYLLIUM_RAW.get());
            event.accept(ModItems.COBALT_RAW.get());
            event.accept(ModItems.LEAD_RAW.get());
            event.accept(ModItems.THORIUM_RAW.get());
            event.accept(ModItems.TITANIUM_RAW.get());
            event.accept(ModItems.TUNGSTEN_RAW.get());
            event.accept(ModItems.URANIUM_RAW.get());

            event.accept(ModBlocks.METEOR.get());
            event.accept(ModBlocks.METEOR_COBBLE.get());
            event.accept(ModBlocks.METEOR_CRUSHED.get());
            event.accept(ModBlocks.METEOR_TREASURE.get());

            event.accept(ModBlocks.GEYSIR_DIRT.get());
            event.accept(ModBlocks.GEYSIR_STONE.get());

            event.accept(ModBlocks.NUCLEAR_FALLOUT.get());
            event.accept(ModBlocks.SELLAFIELD_SLAKED.get());
            event.accept(ModBlocks.SELLAFIELD_SLAKED1.get());
            event.accept(ModBlocks.SELLAFIELD_SLAKED2.get());
            event.accept(ModBlocks.SELLAFIELD_SLAKED3.get());
            event.accept(ModBlocks.WASTE_LOG.get());
            event.accept(ModBlocks.WASTE_PLANKS.get());
            event.accept(ModBlocks.WASTE_GRASS.get());
            event.accept(ModBlocks.BURNED_GRASS.get());
            event.accept(ModBlocks.DEAD_DIRT.get());
            event.accept(ModBlocks.WASTE_LEAVES.get());

            event.accept(ModItems.STRAWBERRY.get());
            event.accept(ModBlocks.STRAWBERRY_BUSH.get());

            event.accept(ModBlocks.POLONIUM210_BLOCK.get());

            // АВТОМАТИЧЕСКОЕ ДОБАВЛЕНИЕ ВСЕХ БЛОКОВ СЛИТКОВ
            for (ModIngots ingot : ModIngots.values()) {

                if (ModBlocks.hasIngotBlock(ingot)) {

                    RegistrySupplier<Block> ingotBlock = ModBlocks.getIngotBlock(ingot);
                    if (ingotBlock != null) {
                        event.accept(ingotBlock.get());
                        if (ModClothConfig.get().enableDebugLogging) {
                            MainRegistry.LOGGER.info("Added {} block to NTM Ores tab", ingotBlock.getId());
                        }
                    }
                }
            }
            event.accept(ModBlocks.URANIUM_BLOCK.get());
            event.accept(ModBlocks.PLUTONIUM_BLOCK.get());
            event.accept(ModBlocks.PLUTONIUM_FUEL_BLOCK.get());
        }


        // СТРОИТЕЛЬНЫЕ БЛОКИ
        if (event.getTab() == ModCreativeTabs.NTM_BUILDING_TAB.get() || event.getTabKey() == CreativeModeTabs.SEARCH) {

            event.accept(ModBlocks.DECO_STEEL.get());
            event.accept(ModBlocks.CONCRETE.get());
            event.accept(ModBlocks.CONCRETE_ASBESTOS.get());
            event.accept(ModBlocks.CONCRETE_COLORED_SAND.get());
            event.accept(ModBlocks.CONCRETE_BLACK.get());
            event.accept(ModBlocks.CONCRETE_BLUE.get());
            event.accept(ModBlocks.CONCRETE_BROWN.get());
            event.accept(ModBlocks.CONCRETE_COLORED_INDIGO.get());
            event.accept(ModBlocks.CONCRETE_COLORED_PINK.get());
            event.accept(ModBlocks.CONCRETE_COLORED_PURPLE.get());
            event.accept(ModBlocks.CONCRETE_CYAN.get());
            event.accept(ModBlocks.CONCRETE_GRAY.get());
            event.accept(ModBlocks.CONCRETE_GREEN.get());
            event.accept(ModBlocks.CONCRETE_LIGHT_BLUE.get());
            event.accept(ModBlocks.CONCRETE_LIME.get());
            event.accept(ModBlocks.CONCRETE_MAGENTA.get());
            event.accept(ModBlocks.CONCRETE_ORANGE.get());
            event.accept(ModBlocks.CONCRETE_PINK.get());
            event.accept(ModBlocks.CONCRETE_PURPLE.get());
            event.accept(ModBlocks.CONCRETE_RED.get());
            event.accept(ModBlocks.CONCRETE_YELLOW.get());
            event.accept(ModBlocks.CONCRETE_HAZARD.get());
            event.accept(ModBlocks.CONCRETE_SILVER.get());
            event.accept(ModBlocks.CONCRETE_WHITE.get());

            event.accept(ModBlocks.CONCRETE_SUPER.get());
            event.accept(ModBlocks.CONCRETE_SUPER_M0.get());
            event.accept(ModBlocks.CONCRETE_SUPER_M1.get());
            event.accept(ModBlocks.CONCRETE_SUPER_M2.get());
            event.accept(ModBlocks.CONCRETE_SUPER_M3.get());
            event.accept(ModBlocks.CONCRETE_SUPER_BROKEN.get());

            event.accept(ModBlocks.CONCRETE_REBAR.get());
            event.accept(ModBlocks.CONCRETE_REBAR_ALT.get());
            event.accept(ModBlocks.CONCRETE_FLAT.get());
            event.accept(ModBlocks.CONCRETE_TILE.get());
            event.accept(ModBlocks.CONCRETE_VENT.get());
            event.accept(ModBlocks.CONCRETE_FAN.get());
            event.accept(ModBlocks.CONCRETE_TILE_TREFOIL.get());

            event.accept(ModBlocks.CONCRETE_MOSSY.get());
            event.accept(ModBlocks.CONCRETE_CRACKED.get());
            event.accept(ModBlocks.CONCRETE_MARKED.get());
            event.accept(ModBlocks.BRICK_CONCRETE.get());
            event.accept(ModBlocks.BRICK_CONCRETE_MOSSY.get());
            event.accept(ModBlocks.BRICK_CONCRETE_CRACKED.get());
            event.accept(ModBlocks.BRICK_CONCRETE_BROKEN.get());
            event.accept(ModBlocks.BRICK_CONCRETE_MARKED.get());
            event.accept(ModBlocks.CONCRETE_PILLAR.get());
            event.accept(ModBlocks.CONCRETE_COLORED_MACHINE.get());
            event.accept(ModBlocks.CONCRETE_COLORED_MACHINE_STRIPE.get());
            event.accept(ModBlocks.CONCRETE_COLORED_BRONZE.get());


            // Метеоритные блоки
            event.accept(ModBlocks.METEOR_POLISHED.get());
            event.accept(ModBlocks.METEOR_BRICK.get());
            event.accept(ModBlocks.METEOR_BRICK_CRACKED.get());
            event.accept(ModBlocks.METEOR_BRICK_MOSSY.get());
            event.accept(ModBlocks.METEOR_BRICK_CHISELED.get());
            event.accept(ModBlocks.METEOR_PILLAR.get());

            event.accept(ModBlocks.DEPTH_BRICK.get());
            event.accept(ModBlocks.DEPTH_TILES.get());
            event.accept(ModBlocks.DEPTH_NETHER_BRICK.get());
            event.accept(ModBlocks.DEPTH_NETHER_TILES.get());
            event.accept(ModBlocks.GNEISS_TILE.get());
            event.accept(ModBlocks.GNEISS_BRICK.get());
            event.accept(ModBlocks.GNEISS_CHISELED.get());

            event.accept(ModBlocks.BRICK_BASE.get());
            event.accept(ModBlocks.BRICK_LIGHT.get());
            event.accept(ModBlocks.BARRICADE.get());
            event.accept(ModBlocks.BRICK_FIRE.get());
            event.accept(ModBlocks.BRICK_OBSIDIAN.get());

            event.accept(ModBlocks.VINYL_TILE.get());
            event.accept(ModBlocks.VINYL_TILE_SMALL.get());
            event.accept(ModBlocks.REINFORCED_STONE.get());
            event.accept(ModBlocks.BRICK_DUCRETE.get());
            event.accept(ModBlocks.ASPHALT.get());
            event.accept(ModBlocks.BASALT_POLISHED.get());
            event.accept(ModBlocks.BASALT_BRICK.get());

            //ПОЛУБЛОКИ
            event.accept(ModBlocks.CONCRETE_HAZARD_SLAB.get());
            event.accept(ModBlocks.CONCRETE_ASBESTOS_SLAB.get());
            event.accept(ModBlocks.CONCRETE_BLACK_SLAB.get());
            event.accept(ModBlocks.CONCRETE_BLUE_SLAB.get());
            event.accept(ModBlocks.CONCRETE_BROWN_SLAB.get());
            event.accept(ModBlocks.DEPTH_STONE_SLAB.get());
            event.accept(ModBlocks.CONCRETE_COLORED_BRONZE_SLAB.get());
            event.accept(ModBlocks.CONCRETE_COLORED_INDIGO_SLAB.get());
            event.accept(ModBlocks.CONCRETE_COLORED_MACHINE_SLAB.get());
            event.accept(ModBlocks.CONCRETE_COLORED_PINK_SLAB.get());
            event.accept(ModBlocks.CONCRETE_COLORED_PURPLE_SLAB.get());
            event.accept(ModBlocks.CONCRETE_COLORED_SAND_SLAB.get());
            event.accept(ModBlocks.CONCRETE_CYAN_SLAB.get());
            event.accept(ModBlocks.CONCRETE_GRAY_SLAB.get());
            event.accept(ModBlocks.CONCRETE_GREEN_SLAB.get());
            event.accept(ModBlocks.CONCRETE_LIGHT_BLUE_SLAB.get());
            event.accept(ModBlocks.CONCRETE_LIME_SLAB.get());
            event.accept(ModBlocks.CONCRETE_MAGENTA_SLAB.get());
            event.accept(ModBlocks.CONCRETE_ORANGE_SLAB.get());
            event.accept(ModBlocks.CONCRETE_PINK_SLAB.get());
            event.accept(ModBlocks.CONCRETE_PURPLE_SLAB.get());
            event.accept(ModBlocks.CONCRETE_RED_SLAB.get());
            event.accept(ModBlocks.CONCRETE_SILVER_SLAB.get());
            event.accept(ModBlocks.CONCRETE_WHITE_SLAB.get());
            event.accept(ModBlocks.CONCRETE_YELLOW_SLAB.get());
            event.accept(ModBlocks.CONCRETE_SUPER_SLAB.get());
            event.accept(ModBlocks.CONCRETE_SUPER_M0_SLAB.get());
            event.accept(ModBlocks.CONCRETE_SUPER_M1_SLAB.get());
            event.accept(ModBlocks.CONCRETE_SUPER_M2_SLAB.get());
            event.accept(ModBlocks.CONCRETE_SUPER_M3_SLAB.get());
            event.accept(ModBlocks.CONCRETE_SUPER_BROKEN_SLAB.get());
            event.accept(ModBlocks.CONCRETE_REBAR_SLAB.get());
            event.accept(ModBlocks.CONCRETE_FLAT_SLAB.get());
            event.accept(ModBlocks.CONCRETE_TILE_SLAB.get());
            event.accept(ModBlocks.DEPTH_BRICK_SLAB.get());
            event.accept(ModBlocks.DEPTH_TILES_SLAB.get());
            event.accept(ModBlocks.DEPTH_STONE_NETHER_SLAB.get());
            event.accept(ModBlocks.DEPTH_NETHER_BRICK_SLAB.get());
            event.accept(ModBlocks.DEPTH_NETHER_TILES_SLAB.get());
            event.accept(ModBlocks.GNEISS_TILE_SLAB.get());
            event.accept(ModBlocks.GNEISS_BRICK_SLAB.get());
            event.accept(ModBlocks.BRICK_BASE_SLAB.get());
            event.accept(ModBlocks.BRICK_LIGHT_SLAB.get());
            event.accept(ModBlocks.BRICK_FIRE_SLAB.get());
            event.accept(ModBlocks.BRICK_OBSIDIAN_SLAB.get());
            event.accept(ModBlocks.VINYL_TILE_SLAB.get());
            event.accept(ModBlocks.VINYL_TILE_SMALL_SLAB.get());
            event.accept(ModBlocks.BRICK_DUCRETE_SLAB.get());
            event.accept(ModBlocks.ASPHALT_SLAB.get());
            event.accept(ModBlocks.BASALT_POLISHED_SLAB.get());
            event.accept(ModBlocks.BASALT_BRICK_SLAB.get());
            event.accept(ModBlocks.METEOR_POLISHED_SLAB.get());
            event.accept(ModBlocks.METEOR_BRICK_SLAB.get());
            event.accept(ModBlocks.METEOR_BRICK_CRACKED_SLAB.get());
            event.accept(ModBlocks.METEOR_BRICK_MOSSY_SLAB.get());
            event.accept(ModBlocks.METEOR_CRUSHED_SLAB.get());
            event.accept(ModBlocks.REINFORCED_STONE_SLAB.get());
            event.accept(ModBlocks.BRICK_CONCRETE_SLAB.get());
            event.accept(ModBlocks.BRICK_CONCRETE_CRACKED_SLAB.get());
            event.accept(ModBlocks.BRICK_CONCRETE_BROKEN_SLAB.get());
            event.accept(ModBlocks.BRICK_CONCRETE_MOSSY_SLAB.get());
            event.accept(ModBlocks.CONCRETE_SLAB.get());
            event.accept(ModBlocks.CONCRETE_MOSSY_SLAB.get());
            event.accept(ModBlocks.CONCRETE_CRACKED_SLAB.get());

            //СТУПЕНИ
            event.accept(ModBlocks.CONCRETE_STAIRS.get());
            event.accept(ModBlocks.CONCRETE_ASBESTOS_STAIRS.get());
            event.accept(ModBlocks.CONCRETE_MOSSY_STAIRS.get());
            event.accept(ModBlocks.CONCRETE_CRACKED_STAIRS.get());
            event.accept(ModBlocks.CONCRETE_HAZARD_STAIRS.get());
            event.accept(ModBlocks.BRICK_CONCRETE_STAIRS.get());
            event.accept(ModBlocks.BRICK_CONCRETE_MOSSY_STAIRS.get());
            event.accept(ModBlocks.BRICK_CONCRETE_CRACKED_STAIRS.get());
            event.accept(ModBlocks.BRICK_CONCRETE_BROKEN_STAIRS.get());
            event.accept(ModBlocks.CONCRETE_BLACK_STAIRS.get());
            event.accept(ModBlocks.CONCRETE_BLUE_STAIRS.get());
            event.accept(ModBlocks.CONCRETE_BROWN_STAIRS.get());
            event.accept(ModBlocks.CONCRETE_COLORED_BRONZE_STAIRS.get());
            event.accept(ModBlocks.CONCRETE_COLORED_INDIGO_STAIRS.get());
            event.accept(ModBlocks.CONCRETE_COLORED_MACHINE_STAIRS.get());
            event.accept(ModBlocks.CONCRETE_COLORED_PINK_STAIRS.get());
            event.accept(ModBlocks.CONCRETE_COLORED_PURPLE_STAIRS.get());
            event.accept(ModBlocks.CONCRETE_COLORED_SAND_STAIRS.get());
            event.accept(ModBlocks.CONCRETE_CYAN_STAIRS.get());
            event.accept(ModBlocks.CONCRETE_GRAY_STAIRS.get());
            event.accept(ModBlocks.CONCRETE_GREEN_STAIRS.get());
            event.accept(ModBlocks.CONCRETE_LIGHT_BLUE_STAIRS.get());
            event.accept(ModBlocks.CONCRETE_LIME_STAIRS.get());
            event.accept(ModBlocks.CONCRETE_MAGENTA_STAIRS.get());
            event.accept(ModBlocks.CONCRETE_ORANGE_STAIRS.get());
            event.accept(ModBlocks.CONCRETE_PINK_STAIRS.get());
            event.accept(ModBlocks.CONCRETE_PURPLE_STAIRS.get());
            event.accept(ModBlocks.CONCRETE_RED_STAIRS.get());
            event.accept(ModBlocks.CONCRETE_SILVER_STAIRS.get());
            event.accept(ModBlocks.CONCRETE_WHITE_STAIRS.get());
            event.accept(ModBlocks.CONCRETE_YELLOW_STAIRS.get());
            event.accept(ModBlocks.CONCRETE_SUPER_STAIRS.get());
            event.accept(ModBlocks.CONCRETE_SUPER_M0_STAIRS.get());
            event.accept(ModBlocks.CONCRETE_SUPER_M1_STAIRS.get());
            event.accept(ModBlocks.CONCRETE_SUPER_M2_STAIRS.get());
            event.accept(ModBlocks.CONCRETE_SUPER_M3_STAIRS.get());
            event.accept(ModBlocks.CONCRETE_SUPER_BROKEN_STAIRS.get());
            event.accept(ModBlocks.CONCRETE_REBAR_STAIRS.get());
            event.accept(ModBlocks.CONCRETE_FLAT_STAIRS.get());
            event.accept(ModBlocks.CONCRETE_TILE_STAIRS.get());
            event.accept(ModBlocks.DEPTH_BRICK_STAIRS.get());
            event.accept(ModBlocks.DEPTH_STONE_STAIRS.get());
            event.accept(ModBlocks.DEPTH_TILES_STAIRS.get());
            event.accept(ModBlocks.DEPTH_NETHER_BRICK_STAIRS.get());
            event.accept(ModBlocks.DEPTH_NETHER_TILES_STAIRS.get());
            event.accept(ModBlocks.GNEISS_TILE_STAIRS.get());
            event.accept(ModBlocks.GNEISS_BRICK_STAIRS.get());
            event.accept(ModBlocks.BRICK_BASE_STAIRS.get());
            event.accept(ModBlocks.BRICK_LIGHT_STAIRS.get());
            event.accept(ModBlocks.BRICK_FIRE_STAIRS.get());
            event.accept(ModBlocks.BRICK_OBSIDIAN_STAIRS.get());
            event.accept(ModBlocks.VINYL_TILE_STAIRS.get());
            event.accept(ModBlocks.VINYL_TILE_SMALL_STAIRS.get());
            event.accept(ModBlocks.BRICK_DUCRETE_STAIRS.get());
            event.accept(ModBlocks.ASPHALT_STAIRS.get());
            event.accept(ModBlocks.BASALT_POLISHED_STAIRS.get());
            event.accept(ModBlocks.BASALT_BRICK_STAIRS.get());
            event.accept(ModBlocks.METEOR_POLISHED_STAIRS.get());
            event.accept(ModBlocks.METEOR_BRICK_STAIRS.get());
            event.accept(ModBlocks.METEOR_BRICK_CRACKED_STAIRS.get());
            event.accept(ModBlocks.METEOR_BRICK_MOSSY_STAIRS.get());
            event.accept(ModBlocks.METEOR_CRUSHED_STAIRS.get());


            event.accept(ModBlocks.REINFORCED_STONE_STAIRS.get());

            //СТЕКЛО
            event.accept(ModBlocks.REINFORCED_GLASS.get());

            //ЯЩИКИ
            event.accept(ModBlocks.FREAKY_ALIEN_BLOCK.get());
            event.accept(ModBlocks.CRATE.get());
            event.accept(ModBlocks.CRATE_LEAD.get());
            event.accept(ModBlocks.CRATE_METAL.get());
            event.accept(ModBlocks.CRATE_WEAPON.get());
            event.accept(ModBlocks.CRATE_CONSERVE.get());

            //ОСВЕЩЕНИЕ
            event.accept(ModBlocks.CAGE_LAMP.get());
            event.accept(ModBlocks.FLOOD_LAMP.get());

            //OBJ-ДЕКОР
            event.accept(ModBlocks.B29.get());
            event.accept(ModBlocks.DORNIER.get());
            event.accept(ModBlocks.FILE_CABINET.get());
            event.accept(ModBlocks.TAPE_RECORDER.get());
            event.accept(ModBlocks.CRT_BROKEN.get());
            event.accept(ModBlocks.CRT_CLEAN.get());
            event.accept(ModBlocks.CRT_BSOD.get());
            event.accept(ModBlocks.TOASTER.get());

            event.accept(ModBlocks.DOOR_OFFICE.get());
            event.accept(ModBlocks.DOOR_BUNKER.get());
            event.accept(ModBlocks.METAL_DOOR.get());
            event.accept(ModBlocks.LARGE_VEHICLE_DOOR.get());
            event.accept(ModBlocks.ROUND_AIRLOCK_DOOR.get());
            event.accept(ModBlocks.FIRE_DOOR.get());
            event.accept(ModBlocks.SLIDING_SEAL_DOOR.get());
            event.accept(ModBlocks.SECURE_ACCESS_DOOR.get());
            event.accept(ModBlocks.QE_CONTAINMENT.get());
            event.accept(ModBlocks.QE_SLIDING.get());
            event.accept(ModBlocks.WATER_DOOR.get());
            event.accept(ModBlocks.SILO_HATCH.get());
            event.accept(ModBlocks.SILO_HATCH_LARGE.get());
            event.accept(ModBlocks.VAULT_DOOR.get());
            event.accept(ModBlocks.TRANSITION_SEAL.get());
            event.accept(ModBlocks.SLIDE_DOOR.get());
        }

        // СТАНКИ
        if (event.getTab() == ModCreativeTabs.NTM_MACHINES_TAB.get() || event.getTabKey() == CreativeModeTabs.SEARCH) {
            event.accept(ModBlocks.CRATE_IRON.get());
            event.accept(ModBlocks.CRATE_STEEL.get());
            event.accept(ModBlocks.CRATE_TUNGSTEN.get());
            event.accept(ModBlocks.CRATE_DESH.get());
            event.accept(ModBlocks.CRATE_TEMPLATE.get());
            event.accept(ModBlocks.BARREL_CORRODED.get());
            event.accept(ModBlocks.BARREL_IRON.get());
            event.accept(ModBlocks.BARREL_STEEL.get());
            event.accept(ModBlocks.BARREL_TCALLOY.get());
            event.accept(ModBlocks.BARREL_PLASTIC.get());
            event.accept(ModBlocks.ANVIL_IRON.get());
            event.accept(ModBlocks.ANVIL_LEAD.get());
            event.accept(ModBlocks.ANVIL_STEEL.get());
            event.accept(ModBlocks.ANVIL_DESH.get());
            event.accept(ModBlocks.ANVIL_FERROURANIUM.get());
            event.accept(ModBlocks.ANVIL_SATURNITE.get());
            event.accept(ModBlocks.ANVIL_BISMUTH_BRONZE.get());
            event.accept(ModBlocks.ANVIL_ARSENIC_BRONZE.get());
            event.accept(ModBlocks.ANVIL_SCHRABIDATE.get());
            event.accept(ModBlocks.ANVIL_DNT.get());
            event.accept(ModBlocks.ANVIL_OSMIRIDIUM.get());
            event.accept(ModBlocks.ANVIL_MURKY.get());
            event.accept(ModBlocks.PRESS.get());
            event.accept(ModBlocks.BLAST_FURNACE.get());
            event.accept(ModBlocks.BLAST_FURNACE_EXTENSION.get());
            event.accept(ModBlocks.HEATING_OVEN.get());
            event.accept(ModBlocks.SHREDDER.get());
            event.accept(ModBlocks.WOOD_BURNER.get());
            event.accept(ModBlocks.CHEMICAL_PLANT.get());
            event.accept(ModBlocks.CENTRIFUGE.get());
            event.accept(ModBlocks.CRYSTALLIZER.get());
            event.accept(ModBlocks.MACHINE_ASSEMBLER.get());
            event.accept(ModBlocks.ADVANCED_ASSEMBLY_MACHINE.get());
            event.accept(ModBlocks.HYDRAULIC_FRACKINING_TOWER.get());
            event.accept(ModBlocks.FLUID_TANK.get());
            event.accept(ModBlocks.MACHINE_BATTERY_SOCKET.get());
            event.accept(ModBlocks.INDUSTRIAL_BOILER.get());
            event.accept(ModBlocks.INDUSTRIAL_TURBINE.get());
            event.accept(ModBlocks.REFINERY.get());
            event.accept(ModBlocks.MACHINE_BATTERY.get());
            event.accept(ModBlocks.MACHINE_BATTERY_LITHIUM.get());
            event.accept(ModBlocks.MACHINE_BATTERY_SCHRABIDIUM.get());
            event.accept(ModBlocks.MACHINE_BATTERY_DINEUTRONIUM.get());
            event.accept(ModBlocks.CONVERTER_BLOCK.get());
            event.accept(ModBlocks.SWITCH.get());
            event.accept(ModBlocks.WIRE_COATED.get());
        }

        // ТОПЛИВО И ЭЛЕМЕНТЫ МЕХАНИЗМОВ
        if (event.getTab() == ModCreativeTabs.NTM_FUEL_TAB.get() || event.getTabKey() == CreativeModeTabs.SEARCH) {
            event.accept(ModItems.CREATIVE_BATTERY.get());



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
                    event.accept(emptyStack);

                    // Создаем заряженную батарею
                    ItemStack chargedStack = new ItemStack(batteryItem);
                    ModBatteryItem.setEnergy(chargedStack, batteryItem.getCapacity());
                    event.accept(chargedStack);

                    if (ModClothConfig.get().enableDebugLogging) {
                        MainRegistry.LOGGER.debug("Added empty and charged variants of {} to creative tab",
                                batteryRegObj.getId());
                    }
                } else {
                    // На всякий случай, если в списке что-то не ModBatteryItem
                    event.accept(item);
                    MainRegistry.LOGGER.warn("Item {} is not a ModBatteryItem, added as regular item",
                            batteryRegObj.getId());
                }
            }

            if (ModClothConfig.get().enableDebugLogging) {
                MainRegistry.LOGGER.info("Added {} battery variants to NTM Fuel tab", batteriesToAdd.size() * 2);
            }

            event.accept(new ItemStack(ModItems.FLUID_BARREL.get()));
            for (ModFluids.FluidEntry entry : HbmFluidRegistry.getOrderedFluids()) {
                ItemStack filledBarrel = new ItemStack(ModItems.FLUID_BARREL.get());
                FluidBarrelItem.setFluid(filledBarrel, new FluidStack(entry.getSource(), FluidBarrelItem.CAPACITY));
                event.accept(filledBarrel, net.minecraft.world.item.CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
            }
            // Fluid Ducts - one per fluid type (neo / colored / silver styles)
            for (ModFluids.FluidEntry entry : HbmFluidRegistry.getOrderedFluids()) {
                event.accept(com.hbm_m.item.liquids.FluidDuctItem.createStack(ModItems.FLUID_DUCT.get(), entry),
                        net.minecraft.world.item.CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
                event.accept(com.hbm_m.item.liquids.FluidDuctItem.createStack(ModItems.FLUID_DUCT_COLORED.get(), entry),
                        net.minecraft.world.item.CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
                event.accept(com.hbm_m.item.liquids.FluidDuctItem.createStack(ModItems.FLUID_DUCT_SILVER.get(), entry),
                        net.minecraft.world.item.CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
            }
            event.accept(new ItemStack(ModItems.FLUID_VALVE.get()));
            event.accept(new ItemStack(ModItems.FLUID_PUMP.get()));
            event.accept(new ItemStack(ModItems.FLUID_EXHAUST.get()));
            event.accept(new ItemStack(ModItems.CRUDE_OIL_BUCKET.get()));
            event.accept(new ItemStack(ModItems.INFINITE_WATER_500.get()));
            event.accept(new ItemStack(ModItems.INFINITE_WATER_5000.get()));
            event.accept(new ItemStack(ModItems.FLUID_BARREL_INFINITE.get()));
        }

        if (event.getTab() == ModCreativeTabs.NTM_TEMPLATES_TAB.get() || event.getTabKey() == CreativeModeTabs.SEARCH) {

            event.accept(ModItems.BLADE_STEEL.get());
            event.accept(ModItems.BLADE_TITANIUM.get());
            event.accept(ModItems.BLADE_ALLOY.get());
            event.accept(ModItems.BLADE_TEST.get());
            event.accept(ModItems.STAMP_STONE_FLAT.get());
            event.accept(ModItems.STAMP_STONE_PLATE.get());
            event.accept(ModItems.STAMP_STONE_WIRE.get());
            event.accept(ModItems.STAMP_STONE_CIRCUIT.get());
            event.accept(ModItems.STAMP_IRON_FLAT.get());
            event.accept(ModItems.STAMP_IRON_PLATE.get());
            event.accept(ModItems.STAMP_IRON_WIRE.get());
            event.accept(ModItems.STAMP_IRON_CIRCUIT.get());
            event.accept(ModItems.STAMP_IRON_9.get());
            event.accept(ModItems.STAMP_IRON_44.get());
            event.accept(ModItems.STAMP_IRON_50.get());
            event.accept(ModItems.STAMP_IRON_357.get());
            event.accept(ModItems.STAMP_STEEL_FLAT.get());
            event.accept(ModItems.STAMP_STEEL_PLATE.get());
            event.accept(ModItems.STAMP_STEEL_WIRE.get());
            event.accept(ModItems.STAMP_STEEL_CIRCUIT.get());
            event.accept(ModItems.STAMP_TITANIUM_FLAT.get());
            event.accept(ModItems.STAMP_TITANIUM_PLATE.get());
            event.accept(ModItems.STAMP_TITANIUM_WIRE.get());
            event.accept(ModItems.STAMP_TITANIUM_FLAT.get());
            event.accept(ModItems.STAMP_TITANIUM_PLATE.get());
            event.accept(ModItems.STAMP_TITANIUM_WIRE.get());
            event.accept(ModItems.STAMP_TITANIUM_CIRCUIT.get());
            event.accept(ModItems.STAMP_OBSIDIAN_FLAT.get());
            event.accept(ModItems.STAMP_OBSIDIAN_PLATE.get());
            event.accept(ModItems.STAMP_OBSIDIAN_WIRE.get());
            event.accept(ModItems.STAMP_OBSIDIAN_CIRCUIT.get());
            event.accept(ModItems.STAMP_DESH_FLAT.get());
            event.accept(ModItems.STAMP_DESH_PLATE.get());
            event.accept(ModItems.STAMP_DESH_WIRE.get());
            event.accept(ModItems.STAMP_DESH_CIRCUIT.get());
            event.accept(ModItems.STAMP_DESH_9.get());
            event.accept(ModItems.STAMP_DESH_44.get());
            event.accept(ModItems.STAMP_DESH_50.get());
            event.accept(ModItems.STAMP_DESH_357.get());

            event.accept(ModItems.TEMPLATE_FOLDER.get());

            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                ClientSetup.addTemplatesClient(event);
            });

            for (ModFluids.FluidEntry entry : HbmFluidRegistry.getOrderedFluids()) {
                ItemStack idStack = new ItemStack(ModItems.FLUID_IDENTIFIER.get());
                FluidIdentifierItem.setType(idStack, HbmFluidRegistry.getFluidName(entry.getSource()), true);
                event.accept(idStack);
            }
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

