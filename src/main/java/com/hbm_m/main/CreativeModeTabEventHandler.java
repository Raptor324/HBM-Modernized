package com.hbm_m.main;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import com.hbm_m.api.fluids.HbmFluidRegistry;
import com.hbm_m.block.ModBlocks;
import com.hbm_m.client.ClientSetup;
import com.hbm_m.config.ModClothConfig;
import com.hbm_m.creativetabs.MissileTab;
import com.hbm_m.creativetabs.NukeTab;
import com.hbm_m.inventory.fluid.ModFluids;
import com.hbm_m.item.ModItems;
import com.hbm_m.item.BlockAbsorberItem;
import com.hbm_m.item.missile.MissilePartItems;
import com.hbm_m.item.nuclear.BreedingRodType;
import com.hbm_m.item.nuclear.PWRFuelType;
import com.hbm_m.item.rbmk.RBMKPelletItem;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.item.fekal_electric.ModBatteryItem;
import com.hbm_m.item.liquids.FluidIdentifierItem;
import com.hbm_m.item.liquids.ItemFluidTank;
import com.hbm_m.item.material.MaterialShape;
import com.hbm_m.item.material.ModMaterialItems;
import com.hbm_m.item.material.ModMaterials;
import com.hbm_m.item.weapon.sedna.WeaponItems;
import com.hbm_m.item.weapon.sedna.factory.GunFactory.EnumAmmo;
import com.hbm_m.item.weapon.sedna.factory.GunFactory.EnumModGeneric;
import com.hbm_m.item.weapon.sedna.factory.GunFactory.EnumModSpecial;
import com.hbm_m.item.weapon.sedna.factory.GunFactory.EnumModCaliber;
import com.hbm_m.item.weapon.grenade.GrenadeItems;
import com.hbm_m.item.weapon.grenade.ItemGrenadeExtra.EnumGrenadeExtra;
import com.hbm_m.item.weapon.grenade.ItemGrenadeFilling.EnumGrenadeFilling;
import com.hbm_m.item.weapon.grenade.ItemGrenadeFuze.EnumGrenadeFuze;
import com.hbm_m.item.weapon.grenade.ItemGrenadeShell.EnumGrenadeShell;
import com.hbm_m.item.weapon.grenade.ItemGrenadeUniversal;

import dev.architectury.utils.Env;
import dev.architectury.utils.EnvExecutor;
import com.hbm_m.platform.PlatformHooks;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.*;
import com.hbm_m.platform.StackNbt;

import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.ItemLike;


/**
 * Наполнение креативных вкладок. Состав и порядок вкладок 1:1 повторяют оригинальный
 * 1.7.10 HBM (порядок {@code setCreativeTab(...)} в {@code ModBlocks}/zo{@code ModItems}).
 * Сгенерировано скриптом tools/creative_reorder/gen_tabs.py; предметы, которых в порте
 * пока нет, пропущены. Dev-вкладка не изменилась.
 */
@SuppressWarnings("UnstableApiUsage")
public final class CreativeModeTabEventHandler {

    /** audit4: Items per ID in einen Reiter legen (Original-Zuordnung), fehlende IDs werden uebersprungen. */
    private static void addOriginalTabIds(BiConsumer<ItemStack, CreativeModeTab.TabVisibility> acceptor, String... ids) {
        for (String id : ids) {
            Item item = BuiltInRegistries.ITEM.get(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, id));
            if (item != Items.AIR) {
                if (item instanceof com.hbm_m.item.rbmk.RBMKPelletItem pellet) {
                    for (int state = 0; state < pellet.stateCount(); state++) {
                        acceptor.accept(pellet.withState(1, state), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
                    }
                } else {
                    acceptor.accept(new ItemStack(item), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
                }
            }
        }
    }


    private CreativeModeTabEventHandler() {
    }

    public static void onBuildCreativeModeTabContents(BuildCreativeModeTabContentsEvent event) {
        MainRegistry.LOGGER.info("Building creative tab contents for: " + event.getTabKey());

        // ВАЖНО: собственные вкладки мода наполняются через .displayItems(...) в ModCreativeTabs —
        // здесь только ванильные вкладки.
        Set<String> seen = new HashSet<>();
        BiConsumer<ItemStack, CreativeModeTab.TabVisibility> acceptor = deduplicatingAcceptor(event, seen);

        if (event.getTabKey() == CreativeModeTabs.COMBAT) {
            populateCombatTab(acceptor);
        }

        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            populateToolsTab(acceptor);
            acceptor.accept(new ItemStack(ModItems.MUSIC_DISC_BUNKER.get()),
                    CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
            event.accept(new ItemStack(ModItems.MUSIC_DISC_CH.get()),
                    CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
            acceptor.accept(new ItemStack(ModItems.RECORD_LC.get()),
                    CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
            acceptor.accept(new ItemStack(ModItems.RECORD_SS.get()),
                    CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
            acceptor.accept(new ItemStack(ModItems.RECORD_VC.get()),
                    CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
        }

        // Original: CreativeTabs.tabTransport
        if (event.getTabKey() == CreativeModeTabs.REDSTONE_BLOCKS) {
            for (var r : java.util.List.of(ModBlocks.RAIL_WOOD, ModBlocks.RAIL_NARROW, ModBlocks.RAIL_HIGHSPEED, ModBlocks.RAIL_BOOSTER))
                acceptor.accept(new ItemStack(r.get()), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
            acceptor.accept(new ItemStack(ModItems.CART_EMPTY_WOOD.get()), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
            acceptor.accept(new ItemStack(ModItems.CART_EMPTY_STEEL.get()), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
            acceptor.accept(new ItemStack(ModItems.CART_EMPTY_PAINTED.get()), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
            acceptor.accept(new ItemStack(ModItems.CART_CRATE.get()), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
            acceptor.accept(new ItemStack(ModItems.CART_DESTROYER_STEEL.get()), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
            acceptor.accept(new ItemStack(ModItems.CART_DESTROYER_PAINTED.get()), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
            acceptor.accept(new ItemStack(ModItems.CART_POWDER_WOOD.get()), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
            acceptor.accept(new ItemStack(ModItems.CART_POWDER_STEEL.get()), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
            acceptor.accept(new ItemStack(ModItems.CART_POWDER_PAINTED.get()), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
            acceptor.accept(new ItemStack(ModItems.CART_SEMTEX_WOOD.get()), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
            acceptor.accept(new ItemStack(ModItems.CART_SEMTEX_STEEL.get()), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
            acceptor.accept(new ItemStack(ModItems.CART_SEMTEX_PAINTED.get()), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
        }

        if (event.getTabKey() == CreativeModeTabs.SPAWN_EGGS) {
            populateSpawnEggs(acceptor);
        }
    }

    /** Обёртка над event.accept, пропускающая повторные добавления одного и того же предмета. */
    private static BiConsumer<ItemStack, CreativeModeTab.TabVisibility> deduplicatingAcceptor(
            BuildCreativeModeTabContentsEvent event, Set<String> seen) {
        BiConsumer<ItemStack, CreativeModeTab.TabVisibility> raw = event::accept;
        return (stack, vis) -> {
            if (stack == null || stack.isEmpty()) {
                return;
            }
            String itemId = String.valueOf(BuiltInRegistries.ITEM.getKey(stack.getItem()));
            CompoundTag itemTag = StackNbt.read(stack);
            String tag = itemTag == null ? "" : itemTag.toString();
            //? if >= 1.21.1 {
            /*net.minecraft.world.item.component.BlockItemStateProperties bs = stack.get(net.minecraft.core.component.DataComponents.BLOCK_STATE);
            if (bs != null && !bs.isEmpty()) {
                tag = tag + "|bs:" + bs.properties().toString();
            }
            *///?}
            if (!seen.add(itemId + "|" + tag)) {
                if (ModClothConfig.get().enableDebugLogging) {
                    MainRegistry.LOGGER.debug("Skipping duplicate creative tab entry {} for tab {}",
                            itemId, event.getTabKey());
                }
                return;
            }
            raw.accept(stack, vis);
        };
    }

    /** Публичный дедупликатор для .displayItems(...) в ModCreativeTabs. */
    public static BiConsumer<ItemStack, CreativeModeTab.TabVisibility> deduplicated(
            BiConsumer<ItemStack, CreativeModeTab.TabVisibility> acceptor) {
        Set<String> seen = new HashSet<>();
        return (stack, vis) -> {
            if (stack == null || stack.isEmpty()) {
                return;
            }
            String itemId = String.valueOf(BuiltInRegistries.ITEM.getKey(stack.getItem()));
            CompoundTag itemTag = StackNbt.read(stack);
            String tag = itemTag == null ? "" : itemTag.toString();
            //? if >= 1.21.1 {
            /*net.minecraft.world.item.component.BlockItemStateProperties bs = stack.get(net.minecraft.core.component.DataComponents.BLOCK_STATE);
            if (bs != null && !bs.isEmpty()) {
                tag = tag + "|bs:" + bs.properties().toString();
            }
            *///?}
            if (!seen.add(itemId + "|" + tag)) {
                if (ModClothConfig.get().enableDebugLogging) {
                    MainRegistry.LOGGER.debug("Skipping duplicate creative tab entry {}", itemId);
                }
                return;
            }
            acceptor.accept(stack, vis);
        };
    }

    /** Яйца призыва (ванильная вкладка). */
    public static void populateSpawnEggs(BiConsumer<ItemStack, CreativeModeTab.TabVisibility> acceptor) {
        Set<String> seen = new HashSet<>();
        Consumer<ItemStack> add = stack -> {
            if (stack == null || stack.isEmpty()) {
                return;
            }
            String itemId = String.valueOf(BuiltInRegistries.ITEM.getKey(stack.getItem()));
            CompoundTag itemTag = StackNbt.read(stack);
            String tag = itemTag == null ? "" : itemTag.toString();
            //? if >= 1.21.1 {
            /*net.minecraft.world.item.component.BlockItemStateProperties bs = stack.get(net.minecraft.core.component.DataComponents.BLOCK_STATE);
            if (bs != null && !bs.isEmpty()) {
                tag = tag + "|bs:" + bs.properties().toString();
            }
            *///?}
            if (!seen.add(itemId + "|" + tag)) {
                return;
            }
            acceptor.accept(stack, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
        };
        add.accept(new ItemStack(ModItems.BOT_PRIME_SPAWN_EGG.get()));
        add.accept(new ItemStack(ModItems.UFO_SPAWN_EGG.get()));
        add.accept(new ItemStack(ModItems.RAD_BEAST_SPAWN_EGG.get()));
        add.accept(new ItemStack(ModItems.MASKMAN_SPAWN_EGG.get()));
        add.accept(new ItemStack(ModItems.HUNTER_CHOPPER_SPAWN_EGG.get()));
        add.accept(new ItemStack(ModItems.DUCK_SPAWN_EGG.get()));
        add.accept(new ItemStack(ModItems.FBI_SPAWN_EGG.get()));
        add.accept(new ItemStack(ModItems.FBI_DRONE_SPAWN_EGG.get()));
        add.accept(new ItemStack(ModItems.QUACKOS_SPAWN_EGG.get()));
        add.accept(new ItemStack(ModItems.PIGEON_SPAWN_EGG.get()));
        add.accept(new ItemStack(ModItems.UNDEAD_SOLDIER_SPAWN_EGG.get()));
        add.accept(new ItemStack(ModItems.PLASTIC_BAG_SPAWN_EGG.get()));
        add.accept(new ItemStack(ModItems.TEST_DUMMY_SPAWN_EGG.get()));
        add.accept(new ItemStack(ModItems.CYBER_CRAB_SPAWN_EGG.get()));
        add.accept(new ItemStack(ModItems.TESLA_CRAB_SPAWN_EGG.get()));
        add.accept(new ItemStack(ModItems.TAINT_CRAB_SPAWN_EGG.get()));
        add.accept(new ItemStack(ModItems.GLYPHID_SPAWN_EGG.get()));
        add.accept(new ItemStack(ModItems.GLYPHID_BRAWLER_SPAWN_EGG.get()));
        add.accept(new ItemStack(ModItems.GLYPHID_BEHEMOTH_SPAWN_EGG.get()));
        add.accept(new ItemStack(ModItems.GLYPHID_BRENDA_SPAWN_EGG.get()));
        add.accept(new ItemStack(ModItems.GLYPHID_BOMBARDIER_SPAWN_EGG.get()));
        add.accept(new ItemStack(ModItems.GLYPHID_BLASTER_SPAWN_EGG.get()));
        add.accept(new ItemStack(ModItems.GLYPHID_SCOUT_SPAWN_EGG.get()));
        add.accept(new ItemStack(ModItems.GLYPHID_NUCLEAR_SPAWN_EGG.get()));
        add.accept(new ItemStack(ModItems.GLYPHID_DIGGER_SPAWN_EGG.get()));
        add.accept(new ItemStack(ModItems.PARASITE_MAGGOT_SPAWN_EGG.get()));
        add.accept(new ItemStack(ModItems.NOLO_SPAWN_EGG.get()));
        add.accept(new ItemStack(ModItems.ENTITY_MOB_TAINTED_CREEPER_SPAWN_EGG.get()));
        add.accept(new ItemStack(ModItems.ENTITY_MOB_GOLD_CREEPER_SPAWN_EGG.get()));
        add.accept(new ItemStack(ModItems.ENTITY_MOB_VOLATILE_CREEPER_SPAWN_EGG.get()));
        add.accept(new ItemStack(ModItems.ENTITY_MOB_PHOSGENE_CREEPER_SPAWN_EGG.get()));
        add.accept(new ItemStack(ModItems.ENTITY_MOB_NUCLEAR_CREEPER_SPAWN_EGG.get()));
    }

    /**
     * Батареи добавляются двумя стеками: пустая и заряженная
     * (сохранено поведение старого populateFuelTab).
     */
    private static void addScrap(Consumer<ItemStack> add, ModMaterials mat) {
        Item scrap = ModMaterialItems.scrapItem(mat);
        if (scrap != null) add.accept(new ItemStack(scrap));
    }

    private static void addBattery(Consumer<ItemStack> add, ItemLike itemLike) {
        Item item = itemLike.asItem();
        if (item instanceof ModBatteryItem batteryItem) {
            ItemStack charged = new ItemStack(batteryItem);
            ModBatteryItem.setEnergy(charged, batteryItem.getCapacity());
            add.accept(charged);
        }
        add.accept(new ItemStack(item));
    }

    private static ItemStack makeChargedBattery(ItemLike itemLike) {
        Item item = itemLike.asItem();
        ItemStack stack = new ItemStack(item);
        if (item instanceof ModBatteryItem batteryItem) {
            ModBatteryItem.setEnergy(stack, batteryItem.getCapacity());
        }
        return stack;
    }

    // ==================== Вкладки мода (порядок оригинала 1.7.10) ====================

        /** populatePartsTab: порядок из оригинального 1.7.10 (ModBlocks/ModItems, вкладка parts); отсутствующие в порте предметы пропущены. */
    public static void populatePartsTab(BiConsumer<ItemStack, CreativeModeTab.TabVisibility> acceptor) {
        Consumer<ItemStack> add = stack -> acceptor.accept(stack, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);

        // === Вкладка Parts: точный порядок оригинала 1.7.10 — scripts/parts_tab_true.tsv (1032 слота = id asc x getSubItems).
        // Все мета-предметы развёрнуты: отдельные предметы на меты (circuit, plate_cast, plate_welded, wire_fine,
        // wire_dense, bedrock_ore, фрагменты, tar/ash/coke/briquette/chunk/ingot_raw и dye/crayon/casing/shell/pipe/
        // bolt/gear_large/part_*/waste_*/nuclear_waste_* — таблица в com.hbm_m.item.PartTabMetaItems). ===

        // --- CHUNK 1: Items 1-100 (worker_m5_parts_c1_r2) ---
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.URANIUM, MaterialShape.INGOT)));            // 4098
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.URANIUM233, MaterialShape.INGOT)));         // 4099
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.URANIUM235, MaterialShape.INGOT)));         // 4100
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.URANIUM238, MaterialShape.INGOT)));         // 4101
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.THORIUM232, MaterialShape.INGOT)));         // 4103
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.PLUTONIUM, MaterialShape.INGOT)));          // 4104
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.PLUTONIUM238, MaterialShape.INGOT)));       // 4105
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.PLUTONIUM239, MaterialShape.INGOT)));       // 4106
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.PLUTONIUM240, MaterialShape.INGOT)));       // 4107
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.PLUTONIUM241, MaterialShape.INGOT)));       // 4108
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.PU_MIX, MaterialShape.INGOT)));             // 4109
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.AM241, MaterialShape.INGOT)));              // 4110
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.AM242, MaterialShape.INGOT)));              // 4111
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.AM_MIX, MaterialShape.INGOT)));             // 4112
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.NEPTUNIUM, MaterialShape.INGOT)));          // 4113
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.POLONIUM, MaterialShape.INGOT)));           // 4114
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.TECHNETIUM, MaterialShape.INGOT)));         // 4115
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.CO60, MaterialShape.INGOT)));               // 4116
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.SR90, MaterialShape.INGOT)));               // 4117
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.AU198, MaterialShape.INGOT)));              // 4118
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.PB209, MaterialShape.INGOT)));              // 4119
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.RA226, MaterialShape.INGOT)));              // 4120
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.TITANIUM, MaterialShape.INGOT)));           // 4121
        add.accept(new ItemStack(Items.COPPER_INGOT));                                                          // 4122 (ванильный)
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.RED_COPPER, MaterialShape.INGOT)));         // 4123
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.TUNGSTEN, MaterialShape.INGOT)));           // 4124
        add.accept(new ItemStack(ModItems.INGOT_TUNGSTEN_CARBIDE.get()));                                       // 4125
        add.accept(new ItemStack(ModItems.INGOT_ALUMINIUM.get()));                                              // 4126
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.STEEL, MaterialShape.INGOT)));              // 4127
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.TCALLOY, MaterialShape.INGOT)));            // 4128
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.CDALLOY, MaterialShape.INGOT)));            // 4129
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.BISMUTH_BRONZE, MaterialShape.INGOT)));     // 4130
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.ARSENIC_BRONZE, MaterialShape.INGOT)));     // 4131
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.BSCCO, MaterialShape.INGOT)));              // 4132
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.LEAD, MaterialShape.INGOT)));               // 4133
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.BISMUTH, MaterialShape.INGOT)));            // 4134
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.ARSENIC, MaterialShape.INGOT)));            // 4135
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.CALCIUM, MaterialShape.INGOT)));            // 4136
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.CADMIUM, MaterialShape.INGOT)));            // 4137
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.TANTALIUM, MaterialShape.INGOT)));          // 4138
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.SILICON, MaterialShape.INGOT)));            // 4139
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.NIOBIUM, MaterialShape.INGOT)));            // 4140
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.BERYLLIUM, MaterialShape.INGOT)));          // 4141
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.COBALT, MaterialShape.INGOT)));             // 4142
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.BORON, MaterialShape.INGOT)));              // 4143
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.GRAPHITE, MaterialShape.INGOT)));           // 4144
        add.accept(new ItemStack(ModItems.FIREBRICK.get()));                                                    // 4145
        add.accept(new ItemStack(ModItems.INGOT_DURA_STEEL.get()));                                        // 4146
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.POLYMER, MaterialShape.INGOT)));            // 4147
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.BAKELITE, MaterialShape.INGOT)));           // 4148
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.BIORUBBER, MaterialShape.INGOT)));          // 4149
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.RUBBER, MaterialShape.INGOT)));             // 4150
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.POLYMER_COMPOSITE, MaterialShape.INGOT)));  // 4151
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.PVC, MaterialShape.INGOT)));                // 4152
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.MUD, MaterialShape.INGOT)));                // 4153
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.CFT, MaterialShape.INGOT)));                // 4154
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.SCHRARANIUM, MaterialShape.INGOT)));        // 4155
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.SCHRABIDIUM, MaterialShape.INGOT)));        // 4156
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.SCHRABIDATE, MaterialShape.INGOT)));        // 4157
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.MAGNETIZED_TUNGSTEN, MaterialShape.INGOT)));// 4158
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.COMBINE_STEEL, MaterialShape.INGOT)));      // 4159
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.SOLINIUM, MaterialShape.INGOT)));           // 4160
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.GH336, MaterialShape.INGOT)));            // 4161
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.URANIUM_FUEL, MaterialShape.INGOT)));       // 4162
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.THORIUM_FUEL, MaterialShape.INGOT)));       // 4163
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.PLUTONIUM_FUEL, MaterialShape.INGOT)));     // 4164
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.NEPTUNIUM_FUEL, MaterialShape.INGOT)));     // 4165
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.MOX_FUEL, MaterialShape.INGOT)));           // 4166
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.AMERICIUM_FUEL, MaterialShape.INGOT)));     // 4167
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.SCHRABIDIUM_FUEL, MaterialShape.INGOT)));   // 4168
        add.accept(new ItemStack(ModItems.INGOT_HES.get()));                                                    // 4169 (stub)
        add.accept(new ItemStack(ModItems.INGOT_LES.get()));                                                    // 4170
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.AUSTRALIUM, MaterialShape.INGOT)));         // 4171
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.LANTHANIUM, MaterialShape.INGOT)));         // 4172
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.ACTINIUM, MaterialShape.INGOT)));           // 4173
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.DESH, MaterialShape.INGOT)));               // 4174
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.FERROURANIUM, MaterialShape.INGOT)));       // 4175
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.STARMETAL, MaterialShape.INGOT)));          // 4176
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.GUNMETAL, MaterialShape.INGOT)));           // 4177
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.WEAPONSTEEL, MaterialShape.INGOT)));        // 4178
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.SATURNITE, MaterialShape.INGOT)));          // 4179
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.EUPHEMIUM, MaterialShape.INGOT)));          // 4180
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.DINEUTRONIUM, MaterialShape.INGOT)));       // 4181
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.ELECTRONIUM, MaterialShape.INGOT)));        // 4182
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.SMORE, MaterialShape.INGOT)));              // 4183
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.OSMIRIDIUM, MaterialShape.INGOT)));         // 4184
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.STEEL_DUSTED, MaterialShape.INGOT)));       // 4185
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.CHAINSSTEEL, MaterialShape.INGOT)));        // 4186
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.METEORITE, MaterialShape.INGOT)));          // 4187
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.METEORITE_FORGED, MaterialShape.INGOT)));   // 4188
        add.accept(new ItemStack(ModItems.BLADE_METEORITE.get()));                                              // 4189
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.PHOSPHORUS, MaterialShape.INGOT)));         // 4190
        add.accept(new ItemStack(ModItems.LITHIUM.get()));                                                      // 4191
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.ZIRCONIUM, MaterialShape.INGOT)));          // 4192
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.SEMTEX, MaterialShape.INGOT)));             // 4193
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.C4, MaterialShape.INGOT)));                 // 4194
        add.accept(new ItemStack(ModItems.OIL_TAR_CRUDE.get()));                                                // 4195/0
        add.accept(new ItemStack(ModItems.OIL_TAR_CRACK.get()));                                                // 4195/1
        add.accept(new ItemStack(ModItems.OIL_TAR_COAL.get()));                                                 // 4195/2
        add.accept(new ItemStack(ModItems.OIL_TAR_WOOD.get()));                                                 // 4195/3

        // --- CHUNK 2: Items 101-200 (worker_m5_parts_c2) ---
        add.accept(new ItemStack(ModItems.OIL_TAR_WAX.get()));                                                  // 4195/4
        add.accept(new ItemStack(ModItems.OIL_TAR_PARAFFIN.get()));                                             // 4195/5
        add.accept(new ItemStack(ModItems.SOLID_FUEL.get()));                                                   // 4196
        add.accept(new ItemStack(ModItems.SOLID_FUEL_PRESTO.get()));                                            // 4197
        add.accept(new ItemStack(ModItems.SOLID_FUEL_PRESTO_TRIPLET.get()));                                    // 4198
        add.accept(new ItemStack(ModItems.SOLID_FUEL_BF.get()));                                                // 4199
        add.accept(new ItemStack(ModItems.SOLID_FUEL_PRESTO_BF.get()));                                         // 4200
        add.accept(new ItemStack(ModItems.SOLID_FUEL_PRESTO_TRIPLET_BF.get()));                                 // 4201
        add.accept(new ItemStack(ModItems.ROCKET_FUEL.get()));                                                  // 4202
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.FIBERGLASS, MaterialShape.INGOT)));         // 4203
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.ASBESTOS, MaterialShape.INGOT)));           // 4204
        add.accept(new ItemStack(ModItems.INGOT_REDSTONE.get()));                                               // 4205/1
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.NEODYMIUM, MaterialShape.INGOT)));          // 4205/6000
        add.accept(new ItemStack(ModItems.INGOT_BORAX.get()));                                                  // 4205/501
        add.accept(new ItemStack(ModItems.INGOT_SODIUM.get()));                                                 // 4205/1100
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.STRONTIUM, MaterialShape.INGOT)));          // 4205/3800
        add.accept(new ItemStack(ModItems.INGOT_SLAG.get()));                                                   // 4205/41
        add.accept(new ItemStack(ModItems.NUGGET_TITANIUM.get()));                                              // 4206/2200
        add.accept(new ItemStack(ModItems.NUGGET_COPPER.get()));                                                // 4206/2900
        add.accept(new ItemStack(ModItems.NUGGET_TUNGSTEN.get()));                                              // 4206/7400
        add.accept(new ItemStack(ModItems.NUGGET_ALUMINIUM.get()));                                             // 4206/1300
        add.accept(new ItemStack(ModItems.NUGGET_BORON.get()));                                                 // 4206/500
        add.accept(new ItemStack(ModItems.NUGGET_SODIUM.get()));                                                // 4206/1100
        add.accept(new ItemStack(ModItems.NUGGET_STRONTIUM.get()));                                             // 4206/3800
        add.accept(new ItemStack(ModItems.NUGGET_LITHIUM.get()));                                               // 4206/300
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.URANIUM, MaterialShape.BILLET)));           // 4207
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.URANIUM233, MaterialShape.BILLET)));        // 4208
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.URANIUM235, MaterialShape.BILLET)));        // 4209
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.URANIUM238, MaterialShape.BILLET)));        // 4210
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.UZH, MaterialShape.BILLET)));               // 4211
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.THORIUM232, MaterialShape.BILLET)));        // 4212
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.PLUTONIUM, MaterialShape.BILLET)));         // 4213
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.PLUTONIUM238, MaterialShape.BILLET)));      // 4214
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.PLUTONIUM239, MaterialShape.BILLET)));      // 4215
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.PLUTONIUM240, MaterialShape.BILLET)));      // 4216
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.PLUTONIUM241, MaterialShape.BILLET)));      // 4217
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.PU_MIX, MaterialShape.BILLET)));            // 4218
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.AM241, MaterialShape.BILLET)));             // 4219
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.AM242, MaterialShape.BILLET)));             // 4220
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.AM_MIX, MaterialShape.BILLET)));            // 4221
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.NEPTUNIUM, MaterialShape.BILLET)));         // 4222
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.POLONIUM, MaterialShape.BILLET)));          // 4223
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.TECHNETIUM, MaterialShape.BILLET)));        // 4224
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.COBALT, MaterialShape.BILLET)));            // 4225
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.CO60, MaterialShape.BILLET)));              // 4226
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.SR90, MaterialShape.BILLET)));              // 4227
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.AU198, MaterialShape.BILLET)));             // 4228
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.PB209, MaterialShape.BILLET)));             // 4229
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.RA226, MaterialShape.BILLET)));             // 4230
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.ACTINIUM, MaterialShape.BILLET)));          // 4231
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.SCHRABIDIUM, MaterialShape.BILLET)));       // 4232
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.SOLINIUM, MaterialShape.BILLET)));          // 4233
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.GH336, MaterialShape.BILLET)));             // 4234
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.AUSTRALIUM, MaterialShape.BILLET)));        // 4235
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.AUSTRALIUM_LESSER, MaterialShape.BILLET))); // 4236
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.AUSTRALIUM_GREATER, MaterialShape.BILLET)));// 4237
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.URANIUM_FUEL, MaterialShape.BILLET)));      // 4238
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.THORIUM_FUEL, MaterialShape.BILLET)));      // 4239
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.PLUTONIUM_FUEL, MaterialShape.BILLET)));    // 4240
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.NEPTUNIUM_FUEL, MaterialShape.BILLET)));    // 4241
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.MOX_FUEL, MaterialShape.BILLET)));          // 4242
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.AMERICIUM_FUEL, MaterialShape.BILLET)));    // 4243
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.LES_FUEL, MaterialShape.BILLET)));          // 4244
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.SCHRABIDIUM_FUEL, MaterialShape.BILLET)));  // 4245
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.HES, MaterialShape.BILLET)));               // 4246
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.PO210BE, MaterialShape.BILLET)));           // 4247
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.RA226BE, MaterialShape.BILLET)));           // 4248
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.PU238BE, MaterialShape.BILLET)));           // 4249
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.BERYLLIUM, MaterialShape.BILLET)));         // 4250
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.BISMUTH, MaterialShape.BILLET)));           // 4251
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.SILICON, MaterialShape.BILLET)));           // 4252
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.ZIRCONIUM, MaterialShape.BILLET)));         // 4253
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.ZFB_BISMUTH, MaterialShape.BILLET)));       // 4254
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.ZFB_PU241, MaterialShape.BILLET)));         // 4255
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.ZFB_AM_MIX, MaterialShape.BILLET)));        // 4256
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.YHARONITE, MaterialShape.BILLET)));         // 4257
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.BALEFIRE_GOLD, MaterialShape.BILLET)));     // 4258
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.FLASHLEAD, MaterialShape.BILLET)));         // 4259
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.NUCLEAR_WASTE, MaterialShape.BILLET)));     // 4260
        add.accept(new ItemStack(ModItems.CINNEBAR.get()));                                                     // 4261
        add.accept(new ItemStack(ModItems.NUGGET_MERCURY_TINY.get()));                                          // 4262
        add.accept(new ItemStack(ModItems.NUGGET_MERCURY.get()));                                               // 4263
        add.accept(new ItemStack(ModItems.BOTTLE_MERCURY.get()));                                               // 4264
        add.accept(new ItemStack(ModItems.COAL_COKE.get()));                                                    // 4265/0
        add.accept(new ItemStack(ModItems.LIGNITE_COKE.get()));                                                 // 4265/1
        add.accept(new ItemStack(ModItems.COKE_PETROLEUM.get()));                                               // 4265/2
        add.accept(new ItemStack(ModItems.LIGNITE.get()));                                                      // 4266
        add.accept(new ItemStack(ModItems.COAL_INFERNAL.get()));                                                // 4267
        add.accept(new ItemStack(ModItems.COAL_BRIQUETTE.get()));                                               // 4269/0
        add.accept(new ItemStack(ModItems.LIGNITE_BRIQUETTE.get()));                                            // 4269/1
        add.accept(new ItemStack(ModItems.SAWDUST_BRIQUETTE.get()));                                            // 4269/2
        add.accept(new ItemStack(ModItems.SULFUR.get()));                                                       // 4270
        add.accept(new ItemStack(ModItems.NITER.get()));                                                        // 4271
        add.accept(new ItemStack(ModItems.NITRA.get()));                                                        // 4272
        add.accept(new ItemStack(ModItems.NITRA_SMALL.get()));                                                  // 4273
        add.accept(new ItemStack(ModItems.FLUORITE.get()));                                                     // 4274
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.COAL, MaterialShape.POWDER)));              // 4275
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.COAL, MaterialShape.POWDER_TINY)));         // 4276
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.IRON, MaterialShape.POWDER)));              // 4277
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.GOLD, MaterialShape.POWDER)));              // 4278

        // --- CHUNK 3: Items 201-300 (worker_m5_parts_c3) ---
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.LAPIS, MaterialShape.POWDER)));             // 4279
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.QUARTZ, MaterialShape.POWDER)));            // 4280
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.DIAMOND, MaterialShape.POWDER)));           // 4281
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.EMERALD, MaterialShape.POWDER)));           // 4282
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.URANIUM, MaterialShape.POWDER)));           // 4283
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.PLUTONIUM, MaterialShape.POWDER)));         // 4284
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.NEPTUNIUM, MaterialShape.POWDER)));         // 4285
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.POLONIUM, MaterialShape.POWDER)));          // 4286
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.CO60, MaterialShape.POWDER)));              // 4287
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.SR90, MaterialShape.POWDER)));              // 4288
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.SR90, MaterialShape.POWDER_TINY)));         // 4289
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.I131, MaterialShape.POWDER)));              // 4290
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.I131, MaterialShape.POWDER_TINY)));         // 4291
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.XE135, MaterialShape.POWDER)));             // 4292
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.XE135, MaterialShape.POWDER_TINY)));        // 4293
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.CS137, MaterialShape.POWDER)));             // 4294
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.CS137, MaterialShape.POWDER_TINY)));        // 4295
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.AU198, MaterialShape.POWDER)));             // 4296
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.RA226, MaterialShape.POWDER)));             // 4297
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.AT209, MaterialShape.POWDER)));             // 4298 (powder_at209)
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.TITANIUM, MaterialShape.POWDER)));          // 4299
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.COPPER, MaterialShape.POWDER)));            // 4300
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.RED_COPPER, MaterialShape.POWDER)));        // 4301
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.TUNGSTEN, MaterialShape.POWDER)));          // 4302
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.ALUMINUM, MaterialShape.POWDER)));          // 4303
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.STEEL, MaterialShape.POWDER)));             // 4304
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.STEEL, MaterialShape.POWDER_TINY)));        // 4305
        add.accept(new ItemStack(ModItems.POWDER_TCALLOY.get()));                                               // 4306 (stub)
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.LEAD, MaterialShape.POWDER)));              // 4307
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.BISMUTH, MaterialShape.POWDER)));           // 4308
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.CALCIUM, MaterialShape.POWDER)));           // 4309
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.CADMIUM, MaterialShape.POWDER)));           // 4310
        add.accept(new ItemStack(ModItems.POWDER_COLTAN.get()));                                                // 4311
        add.accept(new ItemStack(ModItems.POWDER_COLTAN_PURE.get()));                                           // 4312 (stub)
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.TANTALIUM, MaterialShape.POWDER)));         // 4313
        add.accept(new ItemStack(ModItems.POWDER_TEKTITE.get()));                                               // 4314 (stub)
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.PALEOGENITE, MaterialShape.POWDER)));       // 4315
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.PALEOGENITE, MaterialShape.POWDER_TINY)));  // 4316
        add.accept(new ItemStack(ModItems.POWDER_IMPURE_OSMIRIDIUM.get()));                                     // 4317 (stub)
        add.accept(new ItemStack(ModItems.BORAX.get()));                                                        // 4318
        add.accept(new ItemStack(ModItems.POWDER_CHLOROCALCITE.get()));                                         // 4319
        add.accept(new ItemStack(ModItems.MOLYSITE.get()));                                                     // 4320
        add.accept(new ItemStack(ModItems.POWDER_YELLOWCAKE.get()));                                            // 4321
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.BERYLLIUM, MaterialShape.POWDER)));         // 4322
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.DURA_STEEL, MaterialShape.POWDER)));        // 4323
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.POLYMER, MaterialShape.POWDER)));           // 4324
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.BAKELITE, MaterialShape.POWDER)));          // 4325
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.SCHRABIDIUM, MaterialShape.POWDER)));       // 4326
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.SCHRABIDATE, MaterialShape.POWDER)));       // 4327
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.MAGNETIZED_TUNGSTEN, MaterialShape.POWDER)));// 4328
        add.accept(new ItemStack(ModItems.POWDER_CHLOROPHYTE.get()));                                           // 4329 (stub)
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.COMBINE_STEEL, MaterialShape.POWDER)));     // 4330
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.LITHIUM, MaterialShape.POWDER)));           // 4331
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.LITHIUM, MaterialShape.POWDER_TINY)));      // 4332
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.ZIRCONIUM, MaterialShape.POWDER)));         // 4333
        add.accept(new ItemStack(ModItems.POWDER_SODIUM.get()));                                                // 4334
        add.accept(new ItemStack(ModItems.LIGNITE_POWDER.get()));                                               // 4335
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.IODINE, MaterialShape.POWDER)));            // 4336
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.THORIUM, MaterialShape.POWDER)));           // 4337
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.NEODYMIUM, MaterialShape.POWDER)));         // 4338
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.NEODYMIUM, MaterialShape.POWDER_TINY)));    // 4339
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.ASTATINE, MaterialShape.POWDER)));          // 4340 (powder_astatine)
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.CAESIUM, MaterialShape.POWDER)));           // 4341
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.AUSTRALIUM, MaterialShape.POWDER)));        // 4342
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.STRONTIUM, MaterialShape.POWDER)));         // 4343
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.COBALT, MaterialShape.POWDER)));            // 4344
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.COBALT, MaterialShape.POWDER_TINY)));       // 4345
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.BROMINE, MaterialShape.POWDER)));           // 4346
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.NIOBIUM, MaterialShape.POWDER)));           // 4347
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.NIOBIUM, MaterialShape.POWDER_TINY)));      // 4348
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.TENNESSINE, MaterialShape.POWDER)));        // 4349
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.CERIUM, MaterialShape.POWDER)));            // 4350
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.CERIUM, MaterialShape.POWDER_TINY)));       // 4351
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.LANTHANIUM, MaterialShape.POWDER)));        // 4352
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.LANTHANIUM, MaterialShape.POWDER_TINY)));   // 4353
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.ACTINIUM, MaterialShape.POWDER)));          // 4354
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.ACTINIUM, MaterialShape.POWDER_TINY)));     // 4355
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.BORON, MaterialShape.POWDER)));             // 4356
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.BORON, MaterialShape.POWDER_TINY)));        // 4357
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.ASBESTOS, MaterialShape.POWDER)));          // 4358
        add.accept(new ItemStack(ModItems.POWDER_MAGIC.get()));                                                 // 4359
        add.accept(new ItemStack(ModItems.POWDER_SAWDUST.get()));                                               // 4360
        add.accept(new ItemStack(ModItems.POWDER_FLUX.get()));                                                  // 4361
        add.accept(new ItemStack(ModItems.POWDER_FERTILIZER.get()));                                            // 4362
        add.accept(new ItemStack(ModItems.POWDER_BALEFIRE.get()));                                              // 4363
        add.accept(new ItemStack(ModItems.POWDER_SEMTEX_MIX.get()));                                            // 4364
        add.accept(new ItemStack(ModItems.POWDER_DESH_MIX.get()));                                              // 4365
        add.accept(new ItemStack(ModItems.POWDER_DESH_READY.get()));                                            // 4366
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.DESH, MaterialShape.POWDER)));              // 4367
        add.accept(new ItemStack(ModItems.POWDER_NITAN_MIX.get()));                                             // 4368
        add.accept(new ItemStack(ModItems.POWDER_SPARK_MIX.get()));                                             // 4369
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.METEORITE, MaterialShape.POWDER)));         // 4370
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.METEORITE, MaterialShape.POWDER_TINY)));    // 4371
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.EUPHEMIUM, MaterialShape.POWDER)));         // 4372
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.DINEUTRONIUM, MaterialShape.POWDER)));      // 4373
        add.accept(new ItemStack(ModItems.DUST.get()));                                                         // 4374
        add.accept(new ItemStack(ModItems.DUST_TINY.get()));                                                    // 4375
        add.accept(new ItemStack(ModItems.FALLOUT.get()));                                                      // 4376
        add.accept(new ItemStack(ModItems.ASH_WOOD.get()));                                                     // 4377/0
        add.accept(new ItemStack(ModItems.ASH_COAL.get()));                                                     // 4377/1

        // --- CHUNK 4: Items 301-400 (worker_m5_parts_c4) ---
        add.accept(new ItemStack(ModItems.ASH_MISC.get()));                                     // 4377/2
        add.accept(new ItemStack(ModItems.ASH_FLY.get()));                                      // 4377/3
        add.accept(new ItemStack(ModItems.ASH_SOOT.get()));                                     // 4377/4
        add.accept(new ItemStack(ModItems.FULLERENE.get()));                                    // 4377/5 (powder_ash.fullerene)
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.LIMESTONE, MaterialShape.POWDER))); // 4378
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.CEMENT, MaterialShape.POWDER))); // 4379
        add.accept(new ItemStack(ModItems.FIRE_POWDER.get()));                                  // 4380
        add.accept(new ItemStack(ModItems.POWDER_ICE.get()));                                   // 4381
        add.accept(new ItemStack(ModItems.POWDER_POISON.get()));                                // 4382 (stub)
        add.accept(new ItemStack(ModItems.POWDER_THERMITE.get()));                              // 4383
        add.accept(new ItemStack(ModItems.POWDER_POWER.get()));                                 // 4384 (powder_power)
        add.accept(new ItemStack(ModItems.CORDITE.get()));                                      // 4385
        add.accept(new ItemStack(ModItems.BALLISTITE.get()));                                   // 4386
        add.accept(new ItemStack(ModItems.BALL_DYNAMITE.get()));                                // 4387
        add.accept(new ItemStack(ModItems.BALL_TNT.get()));                                     // 4388
        add.accept(new ItemStack(ModItems.BALL_TATB.get()));                                    // 4389
        add.accept(new ItemStack(ModItems.BALL_RESIN.get()));                                   // 4390
        add.accept(new ItemStack(ModItems.BALL_FIRECLAY.get()));                                // 4391
        add.accept(new ItemStack(ModItems.BEDROCK_ORE_BASE.get()));                             // 4403
        add.accept(new ItemStack(ModItems.BEDROCK_ORE_BASE_LIGHT.get()));                       // 4404/0
        add.accept(new ItemStack(ModItems.BEDROCK_ORE_BASE_ROASTED_LIGHT.get()));               // 4404/16
        add.accept(new ItemStack(ModItems.BEDROCK_ORE_BASE_WASHED_LIGHT.get()));                // 4404/32
        add.accept(new ItemStack(ModItems.BEDROCK_ORE_PRIMARY_LIGHT.get()));                    // 4404/48
        add.accept(new ItemStack(ModItems.BEDROCK_ORE_PRIMARY_ROASTED_LIGHT.get()));            // 4404/64
        add.accept(new ItemStack(ModItems.BEDROCK_ORE_PRIMARY_SULFURIC_LIGHT.get()));           // 4404/80
        add.accept(new ItemStack(ModItems.BEDROCK_ORE_PRIMARY_NOSULFURIC_LIGHT.get()));         // 4404/96
        add.accept(new ItemStack(ModItems.BEDROCK_ORE_PRIMARY_SOLVENT_LIGHT.get()));            // 4404/112
        add.accept(new ItemStack(ModItems.BEDROCK_ORE_PRIMARY_NOSOLVENT_LIGHT.get()));          // 4404/128
        add.accept(new ItemStack(ModItems.BEDROCK_ORE_PRIMARY_RAD_LIGHT.get()));                // 4404/144
        add.accept(new ItemStack(ModItems.BEDROCK_ORE_PRIMARY_NORAD_LIGHT.get()));              // 4404/160
        add.accept(new ItemStack(ModItems.BEDROCK_ORE_PRIMARY_FIRST_LIGHT.get()));              // 4404/176
        add.accept(new ItemStack(ModItems.BEDROCK_ORE_PRIMARY_SECOND_LIGHT.get()));             // 4404/192
        add.accept(new ItemStack(ModItems.BEDROCK_ORE_CRUMBS_LIGHT.get()));                     // 4404/208
        add.accept(new ItemStack(ModItems.BEDROCK_ORE_SULFURIC_BYPRODUCT_LIGHT.get()));         // 4404/224
        add.accept(new ItemStack(ModItems.BEDROCK_ORE_SULFURIC_ROASTED_LIGHT.get()));           // 4404/240
        add.accept(new ItemStack(ModItems.BEDROCK_ORE_SULFURIC_ARC_LIGHT.get()));               // 4404/256
        add.accept(new ItemStack(ModItems.BEDROCK_ORE_SULFURIC_WASHED_LIGHT.get()));            // 4404/272
        add.accept(new ItemStack(ModItems.BEDROCK_ORE_SOLVENT_BYPRODUCT_LIGHT.get()));          // 4404/288
        add.accept(new ItemStack(ModItems.BEDROCK_ORE_SOLVENT_ROASTED_LIGHT.get()));            // 4404/304
        add.accept(new ItemStack(ModItems.BEDROCK_ORE_SOLVENT_ARC_LIGHT.get()));                // 4404/320
        add.accept(new ItemStack(ModItems.BEDROCK_ORE_SOLVENT_WASHED_LIGHT.get()));             // 4404/336
        add.accept(new ItemStack(ModItems.BEDROCK_ORE_RAD_BYPRODUCT_LIGHT.get()));              // 4404/352
        add.accept(new ItemStack(ModItems.BEDROCK_ORE_RAD_ROASTED_LIGHT.get()));                // 4404/368
        add.accept(new ItemStack(ModItems.BEDROCK_ORE_RAD_ARC_LIGHT.get()));                    // 4404/384
        add.accept(new ItemStack(ModItems.BEDROCK_ORE_RAD_WASHED_LIGHT.get()));                 // 4404/400
        add.accept(new ItemStack(ModItems.BEDROCK_ORE_BASE_HEAVY.get()));                       // 4404/1
        add.accept(new ItemStack(ModItems.BEDROCK_ORE_BASE_ROASTED_HEAVY.get()));               // 4404/17
        add.accept(new ItemStack(ModItems.BEDROCK_ORE_BASE_WASHED_HEAVY.get()));                // 4404/33
        add.accept(new ItemStack(ModItems.BEDROCK_ORE_PRIMARY_HEAVY.get()));                    // 4404/49
        add.accept(new ItemStack(ModItems.BEDROCK_ORE_PRIMARY_ROASTED_HEAVY.get()));            // 4404/65
        add.accept(new ItemStack(ModItems.BEDROCK_ORE_PRIMARY_SULFURIC_HEAVY.get()));           // 4404/81
        add.accept(new ItemStack(ModItems.BEDROCK_ORE_PRIMARY_NOSULFURIC_HEAVY.get()));         // 4404/97
        add.accept(new ItemStack(ModItems.BEDROCK_ORE_PRIMARY_SOLVENT_HEAVY.get()));            // 4404/113
        add.accept(new ItemStack(ModItems.BEDROCK_ORE_PRIMARY_NOSOLVENT_HEAVY.get()));          // 4404/129
        add.accept(new ItemStack(ModItems.BEDROCK_ORE_PRIMARY_RAD_HEAVY.get()));                // 4404/145
        add.accept(new ItemStack(ModItems.BEDROCK_ORE_PRIMARY_NORAD_HEAVY.get()));              // 4404/161
        add.accept(new ItemStack(ModItems.BEDROCK_ORE_PRIMARY_FIRST_HEAVY.get()));              // 4404/177
        add.accept(new ItemStack(ModItems.BEDROCK_ORE_PRIMARY_SECOND_HEAVY.get()));             // 4404/193
        add.accept(new ItemStack(ModItems.BEDROCK_ORE_CRUMBS_HEAVY.get()));                     // 4404/209
        add.accept(new ItemStack(ModItems.BEDROCK_ORE_SULFURIC_BYPRODUCT_HEAVY.get()));         // 4404/225
        add.accept(new ItemStack(ModItems.BEDROCK_ORE_SULFURIC_ROASTED_HEAVY.get()));           // 4404/241
        add.accept(new ItemStack(ModItems.BEDROCK_ORE_SULFURIC_ARC_HEAVY.get()));               // 4404/257
        add.accept(new ItemStack(ModItems.BEDROCK_ORE_SULFURIC_WASHED_HEAVY.get()));            // 4404/273
        add.accept(new ItemStack(ModItems.BEDROCK_ORE_SOLVENT_BYPRODUCT_HEAVY.get()));          // 4404/289
        add.accept(new ItemStack(ModItems.BEDROCK_ORE_SOLVENT_ROASTED_HEAVY.get()));            // 4404/305
        add.accept(new ItemStack(ModItems.BEDROCK_ORE_SOLVENT_ARC_HEAVY.get()));                // 4404/321
        add.accept(new ItemStack(ModItems.BEDROCK_ORE_SOLVENT_WASHED_HEAVY.get()));             // 4404/337
        add.accept(new ItemStack(ModItems.BEDROCK_ORE_RAD_BYPRODUCT_HEAVY.get()));              // 4404/353
        add.accept(new ItemStack(ModItems.BEDROCK_ORE_RAD_ROASTED_HEAVY.get()));                // 4404/369
        add.accept(new ItemStack(ModItems.BEDROCK_ORE_RAD_ARC_HEAVY.get()));                    // 4404/385
        add.accept(new ItemStack(ModItems.BEDROCK_ORE_RAD_WASHED_HEAVY.get()));                 // 4404/401
        add.accept(new ItemStack(ModItems.BEDROCK_ORE_BASE_RARE.get()));                        // 4404/2
        add.accept(new ItemStack(ModItems.BEDROCK_ORE_BASE_ROASTED_RARE.get()));                // 4404/18
        add.accept(new ItemStack(ModItems.BEDROCK_ORE_BASE_WASHED_RARE.get()));                 // 4404/34
        add.accept(new ItemStack(ModItems.BEDROCK_ORE_PRIMARY_RARE.get()));                     // 4404/50
        add.accept(new ItemStack(ModItems.BEDROCK_ORE_PRIMARY_ROASTED_RARE.get()));             // 4404/66
        add.accept(new ItemStack(ModItems.BEDROCK_ORE_PRIMARY_SULFURIC_RARE.get()));            // 4404/82
        add.accept(new ItemStack(ModItems.BEDROCK_ORE_PRIMARY_NOSULFURIC_RARE.get()));          // 4404/98
        add.accept(new ItemStack(ModItems.BEDROCK_ORE_PRIMARY_SOLVENT_RARE.get()));             // 4404/114
        add.accept(new ItemStack(ModItems.BEDROCK_ORE_PRIMARY_NOSOLVENT_RARE.get()));           // 4404/130
        add.accept(new ItemStack(ModItems.BEDROCK_ORE_PRIMARY_RAD_RARE.get()));                 // 4404/146
        add.accept(new ItemStack(ModItems.BEDROCK_ORE_PRIMARY_NORAD_RARE.get()));               // 4404/162
        add.accept(new ItemStack(ModItems.BEDROCK_ORE_PRIMARY_FIRST_RARE.get()));               // 4404/178
        add.accept(new ItemStack(ModItems.BEDROCK_ORE_PRIMARY_SECOND_RARE.get()));              // 4404/194
        add.accept(new ItemStack(ModItems.BEDROCK_ORE_CRUMBS_RARE.get()));                      // 4404/210
        add.accept(new ItemStack(ModItems.BEDROCK_ORE_SULFURIC_BYPRODUCT_RARE.get()));          // 4404/226
        add.accept(new ItemStack(ModItems.BEDROCK_ORE_SULFURIC_ROASTED_RARE.get()));            // 4404/242
        add.accept(new ItemStack(ModItems.BEDROCK_ORE_SULFURIC_ARC_RARE.get()));                // 4404/258
        add.accept(new ItemStack(ModItems.BEDROCK_ORE_SULFURIC_WASHED_RARE.get()));             // 4404/274
        add.accept(new ItemStack(ModItems.BEDROCK_ORE_SOLVENT_BYPRODUCT_RARE.get()));           // 4404/290
        add.accept(new ItemStack(ModItems.BEDROCK_ORE_SOLVENT_ROASTED_RARE.get()));             // 4404/306
        add.accept(new ItemStack(ModItems.BEDROCK_ORE_SOLVENT_ARC_RARE.get()));                 // 4404/322
        add.accept(new ItemStack(ModItems.BEDROCK_ORE_SOLVENT_WASHED_RARE.get()));              // 4404/338
        add.accept(new ItemStack(ModItems.BEDROCK_ORE_RAD_BYPRODUCT_RARE.get()));               // 4404/354
        add.accept(new ItemStack(ModItems.BEDROCK_ORE_RAD_ROASTED_RARE.get()));                 // 4404/370
        add.accept(new ItemStack(ModItems.BEDROCK_ORE_RAD_ARC_RARE.get()));                     // 4404/386
        add.accept(new ItemStack(ModItems.BEDROCK_ORE_RAD_WASHED_RARE.get()));                  // 4404/402
        add.accept(new ItemStack(ModItems.BEDROCK_ORE_BASE_ACTINIDE.get()));                    // 4404/3
        add.accept(new ItemStack(ModItems.BEDROCK_ORE_BASE_ROASTED_ACTINIDE.get()));            // 4404/19
        add.accept(new ItemStack(ModItems.BEDROCK_ORE_BASE_WASHED_ACTINIDE.get()));             // 4404/35

        // --- CHUNK 5: Items 401-500 (worker_m5_parts_c5) ---
        add.accept(new ItemStack(ModItems.BEDROCK_ORE_PRIMARY_ACTINIDE.get()));                 // 4404/51
        add.accept(new ItemStack(ModItems.BEDROCK_ORE_PRIMARY_ROASTED_ACTINIDE.get()));         // 4404/67
        add.accept(new ItemStack(ModItems.BEDROCK_ORE_PRIMARY_SULFURIC_ACTINIDE.get()));        // 4404/83
        add.accept(new ItemStack(ModItems.BEDROCK_ORE_PRIMARY_NOSULFURIC_ACTINIDE.get()));      // 4404/99
        add.accept(new ItemStack(ModItems.BEDROCK_ORE_PRIMARY_SOLVENT_ACTINIDE.get()));         // 4404/115
        add.accept(new ItemStack(ModItems.BEDROCK_ORE_PRIMARY_NOSOLVENT_ACTINIDE.get()));       // 4404/131
        add.accept(new ItemStack(ModItems.BEDROCK_ORE_PRIMARY_RAD_ACTINIDE.get()));             // 4404/147
        add.accept(new ItemStack(ModItems.BEDROCK_ORE_PRIMARY_NORAD_ACTINIDE.get()));           // 4404/163
        add.accept(new ItemStack(ModItems.BEDROCK_ORE_PRIMARY_FIRST_ACTINIDE.get()));           // 4404/179
        add.accept(new ItemStack(ModItems.BEDROCK_ORE_PRIMARY_SECOND_ACTINIDE.get()));          // 4404/195
        add.accept(new ItemStack(ModItems.BEDROCK_ORE_CRUMBS_ACTINIDE.get()));                  // 4404/211
        add.accept(new ItemStack(ModItems.BEDROCK_ORE_SULFURIC_BYPRODUCT_ACTINIDE.get()));      // 4404/227
        add.accept(new ItemStack(ModItems.BEDROCK_ORE_SULFURIC_ROASTED_ACTINIDE.get()));        // 4404/243
        add.accept(new ItemStack(ModItems.BEDROCK_ORE_SULFURIC_ARC_ACTINIDE.get()));            // 4404/259
        add.accept(new ItemStack(ModItems.BEDROCK_ORE_SULFURIC_WASHED_ACTINIDE.get()));         // 4404/275
        add.accept(new ItemStack(ModItems.BEDROCK_ORE_SOLVENT_BYPRODUCT_ACTINIDE.get()));       // 4404/291
        add.accept(new ItemStack(ModItems.BEDROCK_ORE_SOLVENT_ROASTED_ACTINIDE.get()));         // 4404/307
        add.accept(new ItemStack(ModItems.BEDROCK_ORE_SOLVENT_ARC_ACTINIDE.get()));             // 4404/323
        add.accept(new ItemStack(ModItems.BEDROCK_ORE_SOLVENT_WASHED_ACTINIDE.get()));          // 4404/339
        add.accept(new ItemStack(ModItems.BEDROCK_ORE_RAD_BYPRODUCT_ACTINIDE.get()));           // 4404/355
        add.accept(new ItemStack(ModItems.BEDROCK_ORE_RAD_ROASTED_ACTINIDE.get()));             // 4404/371
        add.accept(new ItemStack(ModItems.BEDROCK_ORE_RAD_ARC_ACTINIDE.get()));                 // 4404/387
        add.accept(new ItemStack(ModItems.BEDROCK_ORE_RAD_WASHED_ACTINIDE.get()));              // 4404/403
        add.accept(new ItemStack(ModItems.BEDROCK_ORE_BASE_NONMETAL.get()));                    // 4404/4
        add.accept(new ItemStack(ModItems.BEDROCK_ORE_BASE_ROASTED_NONMETAL.get()));            // 4404/20
        add.accept(new ItemStack(ModItems.BEDROCK_ORE_BASE_WASHED_NONMETAL.get()));             // 4404/36
        add.accept(new ItemStack(ModItems.BEDROCK_ORE_PRIMARY_NONMETAL.get()));                 // 4404/52
        add.accept(new ItemStack(ModItems.BEDROCK_ORE_PRIMARY_ROASTED_NONMETAL.get()));         // 4404/68
        add.accept(new ItemStack(ModItems.BEDROCK_ORE_PRIMARY_SULFURIC_NONMETAL.get()));        // 4404/84
        add.accept(new ItemStack(ModItems.BEDROCK_ORE_PRIMARY_NOSULFURIC_NONMETAL.get()));      // 4404/100
        add.accept(new ItemStack(ModItems.BEDROCK_ORE_PRIMARY_SOLVENT_NONMETAL.get()));         // 4404/116
        add.accept(new ItemStack(ModItems.BEDROCK_ORE_PRIMARY_NOSOLVENT_NONMETAL.get()));       // 4404/132
        add.accept(new ItemStack(ModItems.BEDROCK_ORE_PRIMARY_RAD_NONMETAL.get()));             // 4404/148
        add.accept(new ItemStack(ModItems.BEDROCK_ORE_PRIMARY_NORAD_NONMETAL.get()));           // 4404/164
        add.accept(new ItemStack(ModItems.BEDROCK_ORE_PRIMARY_FIRST_NONMETAL.get()));           // 4404/180
        add.accept(new ItemStack(ModItems.BEDROCK_ORE_PRIMARY_SECOND_NONMETAL.get()));          // 4404/196
        add.accept(new ItemStack(ModItems.BEDROCK_ORE_CRUMBS_NONMETAL.get()));                  // 4404/212
        add.accept(new ItemStack(ModItems.BEDROCK_ORE_SULFURIC_BYPRODUCT_NONMETAL.get()));      // 4404/228
        add.accept(new ItemStack(ModItems.BEDROCK_ORE_SULFURIC_ROASTED_NONMETAL.get()));        // 4404/244
        add.accept(new ItemStack(ModItems.BEDROCK_ORE_SULFURIC_ARC_NONMETAL.get()));            // 4404/260
        add.accept(new ItemStack(ModItems.BEDROCK_ORE_SULFURIC_WASHED_NONMETAL.get()));         // 4404/276
        add.accept(new ItemStack(ModItems.BEDROCK_ORE_SOLVENT_BYPRODUCT_NONMETAL.get()));       // 4404/292
        add.accept(new ItemStack(ModItems.BEDROCK_ORE_SOLVENT_ROASTED_NONMETAL.get()));         // 4404/308
        add.accept(new ItemStack(ModItems.BEDROCK_ORE_SOLVENT_ARC_NONMETAL.get()));             // 4404/324
        add.accept(new ItemStack(ModItems.BEDROCK_ORE_SOLVENT_WASHED_NONMETAL.get()));          // 4404/340
        add.accept(new ItemStack(ModItems.BEDROCK_ORE_RAD_BYPRODUCT_NONMETAL.get()));           // 4404/356
        add.accept(new ItemStack(ModItems.BEDROCK_ORE_RAD_ROASTED_NONMETAL.get()));             // 4404/372
        add.accept(new ItemStack(ModItems.BEDROCK_ORE_RAD_ARC_NONMETAL.get()));                 // 4404/388
        add.accept(new ItemStack(ModItems.BEDROCK_ORE_RAD_WASHED_NONMETAL.get()));              // 4404/404
        add.accept(new ItemStack(ModItems.BEDROCK_ORE_BASE_CRYSTAL.get()));                     // 4404/5
        add.accept(new ItemStack(ModItems.BEDROCK_ORE_BASE_ROASTED_CRYSTAL.get()));             // 4404/21
        add.accept(new ItemStack(ModItems.BEDROCK_ORE_BASE_WASHED_CRYSTAL.get()));              // 4404/37
        add.accept(new ItemStack(ModItems.BEDROCK_ORE_PRIMARY_CRYSTAL.get()));                  // 4404/53
        add.accept(new ItemStack(ModItems.BEDROCK_ORE_PRIMARY_ROASTED_CRYSTAL.get()));          // 4404/69
        add.accept(new ItemStack(ModItems.BEDROCK_ORE_PRIMARY_SULFURIC_CRYSTAL.get()));         // 4404/85
        add.accept(new ItemStack(ModItems.BEDROCK_ORE_PRIMARY_NOSULFURIC_CRYSTAL.get()));       // 4404/101
        add.accept(new ItemStack(ModItems.BEDROCK_ORE_PRIMARY_SOLVENT_CRYSTAL.get()));          // 4404/117
        add.accept(new ItemStack(ModItems.BEDROCK_ORE_PRIMARY_NOSOLVENT_CRYSTAL.get()));        // 4404/133
        add.accept(new ItemStack(ModItems.BEDROCK_ORE_PRIMARY_RAD_CRYSTAL.get()));              // 4404/149
        add.accept(new ItemStack(ModItems.BEDROCK_ORE_PRIMARY_NORAD_CRYSTAL.get()));            // 4404/165
        add.accept(new ItemStack(ModItems.BEDROCK_ORE_PRIMARY_FIRST_CRYSTAL.get()));            // 4404/181
        add.accept(new ItemStack(ModItems.BEDROCK_ORE_PRIMARY_SECOND_CRYSTAL.get()));           // 4404/197
        add.accept(new ItemStack(ModItems.BEDROCK_ORE_CRUMBS_CRYSTAL.get()));                   // 4404/213
        add.accept(new ItemStack(ModItems.BEDROCK_ORE_SULFURIC_BYPRODUCT_CRYSTAL.get()));       // 4404/229
        add.accept(new ItemStack(ModItems.BEDROCK_ORE_SULFURIC_ROASTED_CRYSTAL.get()));         // 4404/245
        add.accept(new ItemStack(ModItems.BEDROCK_ORE_SULFURIC_ARC_CRYSTAL.get()));             // 4404/261
        add.accept(new ItemStack(ModItems.BEDROCK_ORE_SULFURIC_WASHED_CRYSTAL.get()));          // 4404/277
        add.accept(new ItemStack(ModItems.BEDROCK_ORE_SOLVENT_BYPRODUCT_CRYSTAL.get()));        // 4404/293
        add.accept(new ItemStack(ModItems.BEDROCK_ORE_SOLVENT_ROASTED_CRYSTAL.get()));          // 4404/309
        add.accept(new ItemStack(ModItems.BEDROCK_ORE_SOLVENT_ARC_CRYSTAL.get()));              // 4404/325
        add.accept(new ItemStack(ModItems.BEDROCK_ORE_SOLVENT_WASHED_CRYSTAL.get()));           // 4404/341
        add.accept(new ItemStack(ModItems.BEDROCK_ORE_RAD_BYPRODUCT_CRYSTAL.get()));            // 4404/357
        add.accept(new ItemStack(ModItems.BEDROCK_ORE_RAD_ROASTED_CRYSTAL.get()));              // 4404/373
        add.accept(new ItemStack(ModItems.BEDROCK_ORE_RAD_ARC_CRYSTAL.get()));                  // 4404/389
        add.accept(new ItemStack(ModItems.BEDROCK_ORE_RAD_WASHED_CRYSTAL.get()));               // 4404/405
        add.accept(new ItemStack(ModItems.BEDROCK_ORE_FRAGMENT_COAL.get()));                                  // 4405/600 (COAL)
        add.accept(new ItemStack(ModItems.BEDROCK_ORE_FRAGMENT_LIGNITE.get()));                               // 4405/601 (LIGNITE)
        add.accept(new ItemStack(ModItems.BEDROCK_ORE_FRAGMENT_DIAMOND.get()));                               // 4405/1430 (DIAMOND)
        add.accept(new ItemStack(ModItems.BEDROCK_ORE_FRAGMENT_IRON.get()));                                  // 4405/2600 (IRON)
        add.accept(new ItemStack(ModItems.BEDROCK_ORE_FRAGMENT_GOLD.get()));                                  // 4405/7900 (GOLD)
        add.accept(new ItemStack(ModItems.BEDROCK_ORE_FRAGMENT_REDSTONE.get()));                              // 4405/1 (REDSTONE)
        add.accept(new ItemStack(ModItems.BEDROCK_ORE_FRAGMENT_BAUXITE.get()));                               // 4405/2902 (BAUXITE)
        add.accept(new ItemStack(ModItems.BEDROCK_ORE_FRAGMENT_CRYOLITE.get()));                              // 4405/2903 (CRYOLITE)
        add.accept(new ItemStack(ModItems.BEDROCK_ORE_FRAGMENT_ILMENITE.get()));                            // 4405/2201 (ILMENITE)
        add.accept(new ItemStack(ModItems.BEDROCK_ORE_FRAGMENT_URANIUM.get()));                               // 4405/9200 (URANIUM)
        add.accept(new ItemStack(ModItems.BEDROCK_ORE_FRAGMENT_U238.get()));                                  // 4405/9238 (U238)
        add.accept(new ItemStack(ModItems.BEDROCK_ORE_FRAGMENT_THORIUM.get()));                               // 4405/9032 (THORIUM)
        add.accept(new ItemStack(ModItems.BEDROCK_ORE_FRAGMENT_PO210.get()));                                 // 4405/8410 (POLONIUM)
        add.accept(new ItemStack(ModItems.BEDROCK_ORE_FRAGMENT_TC99.get()));                                  // 4405/4399 (TC99)
        add.accept(new ItemStack(ModItems.BEDROCK_ORE_FRAGMENT_RA226.get()));                                 // 4405/8826 (RADIUM)
        add.accept(new ItemStack(ModItems.BEDROCK_ORE_FRAGMENT_TITANIUM.get()));                              // 4405/2200 (TITANIUM)
        add.accept(new ItemStack(ModItems.BEDROCK_ORE_FRAGMENT_COPPER.get()));                                // 4405/2900 (COPPER)
        add.accept(new ItemStack(ModItems.BEDROCK_ORE_FRAGMENT_TUNGSTEN.get()));                              // 4405/7400 (TUNGSTEN)
        add.accept(new ItemStack(ModItems.BEDROCK_ORE_FRAGMENT_ALUMINIUM.get()));                             // 4405/1300 (ALUMINIUM)
        add.accept(new ItemStack(ModItems.BEDROCK_ORE_FRAGMENT_LEAD.get()));                                  // 4405/8200 (LEAD)
        add.accept(new ItemStack(ModItems.BEDROCK_ORE_FRAGMENT_BISMUTH.get()));                               // 4405/8300 (BISMUTH)
        add.accept(new ItemStack(ModItems.BEDROCK_ORE_FRAGMENT_TANTALIUM.get()));                             // 4405/7300 (TANTALIUM)
        add.accept(new ItemStack(ModItems.BEDROCK_ORE_FRAGMENT_NEODYMIUM.get()));                             // 4405/6000 (NEODYMIUM)
        add.accept(new ItemStack(ModItems.BEDROCK_ORE_FRAGMENT_NIOBIUM.get()));                               // 4405/4100 (NIOBIUM)
        add.accept(new ItemStack(ModItems.BEDROCK_ORE_FRAGMENT_BERYLLIUM.get()));                             // 4405/400 (BERYLLIUM)

        // --- CHUNK 6: Items 501-600 (worker_m5_parts_c6) ---
        add.accept(new ItemStack(ModItems.BEDROCK_ORE_FRAGMENT_EMERALD.get()));                               // 4405/401 (EMERALD)
        add.accept(new ItemStack(ModItems.BEDROCK_ORE_FRAGMENT_COBALT.get()));                                // 4405/2700 (COBALT)
        add.accept(new ItemStack(ModItems.BEDROCK_ORE_FRAGMENT_BORON.get()));                                 // 4405/500 (BORON)
        add.accept(new ItemStack(ModItems.BEDROCK_ORE_FRAGMENT_BORAX.get()));                                 // 4405/501 (BORAX)
        add.accept(new ItemStack(ModItems.BEDROCK_ORE_FRAGMENT_LANTHANIUM.get()));                            // 4405/5700 (LANTHANIUM)
        add.accept(new ItemStack(ModItems.BEDROCK_ORE_FRAGMENT_ZIRCONIUM.get()));                             // 4405/4000 (ZIRCONIUM)
        add.accept(new ItemStack(ModItems.BEDROCK_ORE_FRAGMENT_SODIUM.get()));                                // 4405/1100 (SODIUM)
        add.accept(new ItemStack(ModItems.BEDROCK_ORE_FRAGMENT_SODALITE.get()));                              // 4405/1101 (SODALITE)
        add.accept(new ItemStack(ModItems.BEDROCK_ORE_FRAGMENT_STRONTIUM.get()));                             // 4405/3800 (STRONTIUM)
        add.accept(new ItemStack(ModItems.BEDROCK_ORE_FRAGMENT_LITHIUM.get()));                               // 4405/300 (LITHIUM)
        add.accept(new ItemStack(ModItems.BEDROCK_ORE_FRAGMENT_SULFUR.get()));                                // 4405/1600 (SULFUR)
        add.accept(new ItemStack(ModItems.BEDROCK_ORE_FRAGMENT_KNO.get()));                                   // 4405/700 (KNO)
        add.accept(new ItemStack(ModItems.BEDROCK_ORE_FRAGMENT_FLUORITE.get()));                              // 4405/900 (FLUORITE)
        add.accept(new ItemStack(ModItems.BEDROCK_ORE_FRAGMENT_PHOSPHORUS.get()));                            // 4405/1500 (PHOSPHORUS)
        add.accept(new ItemStack(ModItems.BEDROCK_ORE_FRAGMENT_CHLOROCALCITE.get()));                         // 4405/1701 (CHLOROCALCITE)
        add.accept(new ItemStack(ModItems.BEDROCK_ORE_FRAGMENT_MOLYSITE.get()));                              // 4405/1702 (MOLYSITE)
        add.accept(new ItemStack(ModItems.BEDROCK_ORE_FRAGMENT_CINNABAR.get()));                              // 4405/8001 (CINNABAR)
        add.accept(new ItemStack(ModItems.BEDROCK_ORE_FRAGMENT_SILICON.get()));                               // 4405/1400 (SILICON)
        add.accept(new ItemStack(ModItems.BEDROCK_ORE_FRAGMENT_ASBESTOS.get()));                              // 4405/1401 (ASBESTOS)
        add.accept(new ItemStack(ModItems.BEDROCK_ORE_FRAGMENT_RARE_EARTH.get()));                            // 4405/20000 (RAREEARTH)
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.COAL, MaterialShape.CRYSTAL)));             // 4406
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.IRON, MaterialShape.CRYSTAL)));             // 4407
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.GOLD, MaterialShape.CRYSTAL)));             // 4408
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.REDSTONE, MaterialShape.CRYSTAL)));         // 4409
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.LAPIS, MaterialShape.CRYSTAL)));            // 4410
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.DIAMOND, MaterialShape.CRYSTAL)));          // 4411
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.URANIUM, MaterialShape.CRYSTAL)));          // 4412
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.THORIUM, MaterialShape.CRYSTAL)));          // 4413
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.PLUTONIUM, MaterialShape.CRYSTAL)));        // 4414
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.TITANIUM, MaterialShape.CRYSTAL)));         // 4415
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.SULFUR, MaterialShape.CRYSTAL)));           // 4416
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.NITER, MaterialShape.CRYSTAL)));            // 4417
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.COPPER, MaterialShape.CRYSTAL)));           // 4418
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.TUNGSTEN, MaterialShape.CRYSTAL)));         // 4419
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.ALUMINIUM, MaterialShape.CRYSTAL)));        // 4420
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.FLUORITE, MaterialShape.CRYSTAL)));         // 4421
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.BERYLLIUM, MaterialShape.CRYSTAL)));        // 4422
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.LEAD, MaterialShape.CRYSTAL)));             // 4423
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.SCHRARANIUM, MaterialShape.CRYSTAL)));      // 4424
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.SCHRABIDIUM, MaterialShape.CRYSTAL)));      // 4425
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.RARE, MaterialShape.CRYSTAL)));             // 4426
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.PHOSPHORUS, MaterialShape.CRYSTAL)));       // 4427
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.LITHIUM, MaterialShape.CRYSTAL)));          // 4428
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.COBALT, MaterialShape.CRYSTAL)));           // 4429
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.STARMETAL, MaterialShape.CRYSTAL)));        // 4430
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.CINNEBAR, MaterialShape.CRYSTAL)));         // 4431
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.TRIXITE, MaterialShape.CRYSTAL)));          // 4432
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.OSMIRIDIUM, MaterialShape.CRYSTAL)));       // 4433
        add.accept(new ItemStack(ModItems.GEM_SODALITE.get()));                                                 // 4434
        add.accept(new ItemStack(ModItems.GEM_TANTALIUM.get()));                                                // 4435
        add.accept(new ItemStack(ModItems.GEM_VOLCANIC.get()));                                                 // 4436
        add.accept(new ItemStack(ModItems.GEM_RAD.get()));                                                      // 4437
        add.accept(new ItemStack(ModItems.GEM_ALEXANDRITE.get()));                                              // 4438
        add.accept(new ItemStack(ModItems.FRAGMENT_NEODYMIUM.get()));                                           // 4439
        add.accept(new ItemStack(ModItems.FRAGMENT_COBALT.get()));                                              // 4440
        add.accept(new ItemStack(ModItems.FRAGMENT_NIOBIUM.get()));                                             // 4441
        add.accept(new ItemStack(ModItems.FRAGMENT_CERIUM.get()));                                              // 4442
        add.accept(new ItemStack(ModItems.FRAGMENT_LANTHANIUM.get()));                                          // 4443
        add.accept(new ItemStack(ModItems.FRAGMENT_ACTINIUM.get()));                                            // 4444
        add.accept(new ItemStack(ModItems.FRAGMENT_BORON.get()));                                               // 4445
        add.accept(new ItemStack(ModItems.FRAGMENT_METEORITE.get()));                                           // 4446
        add.accept(new ItemStack(ModItems.FRAGMENT_COLTAN.get()));                                              // 4447
        add.accept(new ItemStack(ModItems.MALACHITE_CHUNK.get()));                                              // 4448/1
        add.accept(new ItemStack(ModItems.CRYOLITE_CHUNK.get()));                                               // 4448/2
        add.accept(new ItemStack(ModItems.ILMENITE_CHUNK.get()));                                              // 4448/4
        add.accept(new ItemStack(ModItems.RAREGROUND_ORE_CHUNK.get()));                                         // 4448/0
        add.accept(new ItemStack(ModItems.MOONSTONE.get()));                                                    // 4448/3
        add.accept(new ItemStack(ModItems.BIOMASS.get()));                                                      // 4449
        add.accept(new ItemStack(ModItems.BIOMASS_COMPRESSED.get()));                                           // 4450
        add.accept(new ItemStack(ModItems.BIO_WAFER.get()));                                                    // 4451
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.URANIUM, MaterialShape.NUGGET)));           // 4452
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.URANIUM233, MaterialShape.NUGGET)));        // 4453
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.URANIUM235, MaterialShape.NUGGET)));        // 4454
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.URANIUM238, MaterialShape.NUGGET)));        // 4455
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.THORIUM232, MaterialShape.NUGGET)));        // 4456
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.PLUTONIUM, MaterialShape.NUGGET)));         // 4457
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.PLUTONIUM238, MaterialShape.NUGGET)));      // 4458
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.PLUTONIUM239, MaterialShape.NUGGET)));      // 4459
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.PLUTONIUM240, MaterialShape.NUGGET)));      // 4460
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.PLUTONIUM241, MaterialShape.NUGGET)));      // 4461
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.PU_MIX, MaterialShape.NUGGET)));            // 4462
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.AM241, MaterialShape.NUGGET)));             // 4463
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.AM242, MaterialShape.NUGGET)));             // 4464
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.AM_MIX, MaterialShape.NUGGET)));            // 4465
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.NEPTUNIUM, MaterialShape.NUGGET)));         // 4466
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.POLONIUM, MaterialShape.NUGGET)));          // 4467
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.COBALT, MaterialShape.NUGGET)));            // 4468
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.CO60, MaterialShape.NUGGET)));              // 4469
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.SR90, MaterialShape.NUGGET)));              // 4470
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.TECHNETIUM, MaterialShape.NUGGET)));        // 4471
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.AU198, MaterialShape.NUGGET)));             // 4472
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.PB209, MaterialShape.NUGGET)));             // 4473
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.RA226, MaterialShape.NUGGET)));             // 4474
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.ACTINIUM, MaterialShape.NUGGET)));          // 4475
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.LEAD, MaterialShape.NUGGET)));              // 4476
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.BISMUTH, MaterialShape.NUGGET)));           // 4477
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.ARSENIC, MaterialShape.NUGGET)));           // 4478
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.TANTALIUM, MaterialShape.NUGGET)));         // 4479
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.SILICON, MaterialShape.NUGGET)));           // 4480
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.NIOBIUM, MaterialShape.NUGGET)));           // 4481

        // --- CHUNK 7: Items 601-700 (worker_m5_parts_c7) ---
        // 601-621: Nuggets
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.BERYLLIUM, MaterialShape.NUGGET)));         // 4482
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.SCHRABIDIUM, MaterialShape.NUGGET)));         // 4483
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.SOLINIUM, MaterialShape.NUGGET)));         // 4484
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.GH336, MaterialShape.NUGGET)));         // 4485
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.URANIUM_FUEL, MaterialShape.NUGGET)));         // 4486
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.THORIUM_FUEL, MaterialShape.NUGGET)));         // 4487
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.PLUTONIUM_FUEL, MaterialShape.NUGGET)));         // 4488
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.NEPTUNIUM_FUEL, MaterialShape.NUGGET)));         // 4489
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.MOX_FUEL, MaterialShape.NUGGET)));         // 4490
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.AMERICIUM_FUEL, MaterialShape.NUGGET)));         // 4491
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.SCHRABIDIUM_FUEL, MaterialShape.NUGGET)));         // 4492
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.HES, MaterialShape.NUGGET)));         // 4493
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.LES_FUEL, MaterialShape.NUGGET)));         // 4494
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.ZIRCONIUM, MaterialShape.NUGGET)));         // 4495
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.AUSTRALIUM, MaterialShape.NUGGET)));         // 4496
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.AUSTRALIUM_LESSER, MaterialShape.NUGGET)));         // 4497
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.AUSTRALIUM_GREATER, MaterialShape.NUGGET)));         // 4498
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.DESH, MaterialShape.NUGGET)));         // 4499
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.EUPHEMIUM, MaterialShape.NUGGET)));         // 4500
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.DINEUTRONIUM, MaterialShape.NUGGET)));         // 4501
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.OSMIRIDIUM, MaterialShape.NUGGET)));         // 4502
        // 622-650: Plates & Armor Plating
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.IRON, MaterialShape.PLATE))); // 4503
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.GOLD, MaterialShape.PLATE))); // 4504
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.TITANIUM, MaterialShape.PLATE))); // 4505
        add.accept(new ItemStack(ModItems.PLATE_ALUMINIUM.get())); // 4506
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.STEEL, MaterialShape.PLATE))); // 4507
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.LEAD, MaterialShape.PLATE))); // 4508
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.COPPER, MaterialShape.PLATE))); // 4509
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.DURA_STEEL, MaterialShape.PLATE))); // 4510
        add.accept(new ItemStack(ModItems.NEUTRON_REFLECTOR.get())); // 4511
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.SCHRABIDIUM, MaterialShape.PLATE))); // 4512
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.COMBINE_STEEL, MaterialShape.PLATE))); // 4513
        add.accept(new ItemStack(ModItems.PLATE_MIXED.get())); // 4514
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.GUNMETAL, MaterialShape.PLATE))); // 4515
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.WEAPONSTEEL, MaterialShape.PLATE))); // 4516
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.SATURNITE, MaterialShape.PLATE))); // 4517
        add.accept(new ItemStack(ModItems.PLATE_PAA.get())); // 4518
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.POLYMER, MaterialShape.PLATE))); // 4519
        add.accept(new ItemStack(ModItems.PLATE_KEVLAR.get())); // 4520
        add.accept(new ItemStack(ModItems.PLATE_DALEKANIUM.get())); // 4521
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.DESH, MaterialShape.PLATE))); // 4522
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.BISMUTH, MaterialShape.PLATE))); // 4523
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.EUPHEMIUM, MaterialShape.PLATE))); // 4524
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.DINEUTRONIUM, MaterialShape.PLATE))); // 4525
        add.accept(new ItemStack(ModItems.PLATE_ARMOR_TITANIUM.get())); // 4526
        add.accept(new ItemStack(ModItems.PLATE_ARMOR_AJR.get())); // 4527
        add.accept(new ItemStack(ModItems.PLATE_ARMOR_HEV.get())); // 4528
        add.accept(new ItemStack(ModItems.PLATE_ARMOR_LUNAR.get())); // 4529
        add.accept(new ItemStack(ModItems.PLATE_ARMOR_FAU.get())); // 4530
        add.accept(new ItemStack(ModItems.PLATE_ARMOR_DNT.get())); // 4531
        // 651-674: Cast Plates (4532)
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.IRON, MaterialShape.PLATE_CAST)));        // 4532/2600 IRON
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.GOLD, MaterialShape.PLATE_CAST)));        // 4532/7900 GOLD
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.SCHRABIDIUM, MaterialShape.PLATE_CAST)));        // 4532/12626 SA326
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.SCHRABIDATE, MaterialShape.PLATE_CAST)));        // 4532/12600 SBD
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.TITANIUM, MaterialShape.PLATE_CAST)));        // 4532/2200 TI
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.COPPER, MaterialShape.PLATE_CAST)));        // 4532/2900 CU
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.TUNGSTEN, MaterialShape.PLATE_CAST)));        // 4532/7400 W
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.ALUMINIUM, MaterialShape.PLATE_CAST)));        // 4532/1300 AL
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.LEAD, MaterialShape.PLATE_CAST)));        // 4532/8200 PB
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.ZIRCONIUM, MaterialShape.PLATE_CAST)));        // 4532/4000 ZR
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.OSMIRIDIUM, MaterialShape.PLATE_CAST)));        // 4532/7699 OSMIRIDIUM
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.STEEL, MaterialShape.PLATE_CAST)));        // 4532/30 STEEL
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.DURA_STEEL, MaterialShape.PLATE_CAST)));        // 4532/33 DURA
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.DESH, MaterialShape.PLATE_CAST)));        // 4532/42 DESH
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.STAR_METAL, MaterialShape.PLATE_CAST)));        // 4532/35 STAR
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.FERROURANIUM, MaterialShape.PLATE_CAST)));        // 4532/37 FERRO
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.TCALLOY, MaterialShape.PLATE_CAST)));        // 4532/36 TCALLOY
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.CDALLOY, MaterialShape.PLATE_CAST)));        // 4532/43 CDALLOY
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.BBRONZE, MaterialShape.PLATE_CAST)));        // 4532/46 BBRONZE
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.ABRONZE, MaterialShape.PLATE_CAST)));        // 4532/47 ABRONZE
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.CMB, MaterialShape.PLATE_CAST)));        // 4532/39 CMB
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.WEAPONSTEEL, MaterialShape.PLATE_CAST)));        // 4532/50 WEAPONSTEEL
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.SATURNITE, MaterialShape.PLATE_CAST)));        // 4532/34 SATURN
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.DEMOMAT, MaterialShape.PLATE_CAST)));          // 674 (4532/20006)
        // 675-686: Welded Plates (4533)
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.IRON, MaterialShape.PLATE_WELDED)));      // 4533/2600 IRON
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.TITANIUM, MaterialShape.PLATE_WELDED)));      // 4533/2200 TI
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.COPPER, MaterialShape.PLATE_WELDED)));      // 4533/2900 CU
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.TUNGSTEN, MaterialShape.PLATE_WELDED)));      // 4533/7400 W
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.ALUMINIUM, MaterialShape.PLATE_WELDED)));      // 4533/1300 AL
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.ZIRCONIUM, MaterialShape.PLATE_WELDED)));      // 4533/4000 ZR
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.OSMIRIDIUM, MaterialShape.PLATE_WELDED)));      // 4533/7699 OSMIRIDIUM
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.STEEL, MaterialShape.PLATE_WELDED)));      // 4533/30 STEEL
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.TCALLOY, MaterialShape.PLATE_WELDED)));      // 4533/36 TCALLOY
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.CDALLOY, MaterialShape.PLATE_WELDED)));      // 4533/43 CDALLOY
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.CMB, MaterialShape.PLATE_WELDED)));      // 4533/39 CMB
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.DEMOMAT, MaterialShape.PLATE_WELDED)));  // 686 (4533/20006)
        // 687-693: Shells (4534)
        add.accept(new ItemStack(ModItems.SHELL_TITANIUM.get()));                                  // 4534/2200 TI
        add.accept(new ItemStack(ModItems.SHELL_COPPER.get()));                                    // 4534/2900 CU
        add.accept(new ItemStack(ModItems.SHELL_ALUMINUM.get()));                                  // 4534/1300 AL
        add.accept(new ItemStack(ModItems.SHELL_STEEL.get()));                                     // 4534/30 STEEL
        addMeta(add, "shell_weaponsteel");// 4534/50 WEAPONSTEEL
        addMeta(add, "shell_saturnite");  // 4534/34 SATURNITE
        addMeta(add, "shell_demomat");    // 693 (4534/20006)
        // 694-700: Pipes (4535)
        add.accept(new ItemStack(ModItems.PIPE_IRON.get()));                                       // 4535/2600 IRON
        add.accept(new ItemStack(ModItems.PIPE_COPPER.get()));                                     // 4535/2900 CU
        add.accept(new ItemStack(ModItems.PIPE_ALUMINUM.get()));                                   // 4535/1300 AL
        add.accept(new ItemStack(ModItems.PIPE_LEAD.get()));                                       // 4535/8200 PB
        add.accept(new ItemStack(ModItems.PIPE_STEEL.get()));                                      // 4535/30 STEEL
        add.accept(new ItemStack(ModItems.PIPE_DURA_STEEL.get()));                                 // 4535/33 DURA
        addMeta(add, "pipe_rubber");      // 4535/20003 RUBBER

        // --- CHUNK 8: Items 701-800 (worker_m5_parts_c8) ---
        addMeta(add, "pipe_demomat");     // 701 (4535/20006)
        // 702-705: Bolts (4536)
        add.accept(new ItemStack(ModItems.BOLT_TUNGSTEN.get()));         // 4536/7400 W
        add.accept(new ItemStack(ModItems.BOLT_LEAD.get()));             // 4536/8200 PB
        add.accept(new ItemStack(ModItems.BOLT_STEEL.get()));            // 4536/30 STEEL
        add.accept(new ItemStack(ModItems.BOLT_DURA_STEEL.get()));  // 4536/33 DURA
        // 706-714: General parts & cloths (4537-4545)
        add.accept(new ItemStack(ModItems.BOLT_SPIKE.get()));            // 4537
        add.accept(new ItemStack(ModItems.HAZMAT_CLOTH.get()));          // 4538
        add.accept(new ItemStack(ModItems.HAZMAT_CLOTH_RED.get()));      // 4539
        add.accept(new ItemStack(ModItems.HAZMAT_CLOTH_GREY.get()));     // 4540
        add.accept(new ItemStack(ModItems.ASBESTOS_CLOTH.get()));        // 4541
        add.accept(new ItemStack(ModItems.RAG.get()));                   // 4542
        add.accept(new ItemStack(ModItems.RAG_DAMP.get()));              // 4543
        add.accept(new ItemStack(ModItems.RAG_PISS.get()));              // 4544
        add.accept(new ItemStack(ModItems.FILTER_COAL.get()));           // 4545
        // 715-725: Fine Wire (4546)
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.CARBON, MaterialShape.WIRE)));            // 4546/699 CARBON
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.GOLD, MaterialShape.WIRE)));              // 4546/7900 GOLD
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.SCHRABIDIUM, MaterialShape.WIRE)));       // 4546/12626 SA326
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.COPPER, MaterialShape.WIRE)));            // 4546/2900 CU
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.TUNGSTEN, MaterialShape.WIRE)));          // 4546/7400 W
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.ALUMINIUM, MaterialShape.WIRE)));         // 4546/1300 AL
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.LEAD, MaterialShape.WIRE)));              // 4546/8200 PB
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.ZIRCONIUM, MaterialShape.WIRE)));         // 4546/4000 ZR
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.STEEL, MaterialShape.WIRE)));             // 4546/30 STEEL
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.RED_COPPER, MaterialShape.WIRE)));        // 4546/31 MINGRADE
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.MAGNETIZED_TUNGSTEN, MaterialShape.WIRE)));// 4546/38 MAGTUNG
        // 726-738: Dense Wire (4547)
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.GOLD, MaterialShape.WIRE_DENSE)));        // 4547/7900 GOLD
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.SCHRABIDIUM, MaterialShape.WIRE_DENSE))); // 4547/12626 SA326
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.SCHRABIDATE, MaterialShape.WIRE_DENSE)));  // 4547/12600 SBD
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.TITANIUM, MaterialShape.WIRE_DENSE)));    // 4547/2200 TI
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.COPPER, MaterialShape.WIRE_DENSE)));      // 4547/2900 CU
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.TUNGSTEN, MaterialShape.WIRE_DENSE)));    // 4547/7400 W
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.NEODYMIUM, MaterialShape.WIRE_DENSE)));   // 4547/6000 ND
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.NIOBIUM, MaterialShape.WIRE_DENSE)));     // 4547/4100 NB
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.RED_COPPER, MaterialShape.WIRE_DENSE)));  // 4547/31 MINGRADE
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.STAR_METAL, MaterialShape.WIRE_DENSE)));  // 4547/35 STAR
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.BSCCO, MaterialShape.WIRE_DENSE)));       // 4547/48 BSCCO
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.MAGNETIZED_TUNGSTEN, MaterialShape.WIRE_DENSE))); // 4547/38 MAGTUNG
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.DNT, MaterialShape.WIRE_DENSE)));         // 4547/45 DNT
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.DEMOMAT, MaterialShape.WIRE_DENSE)));     // 739 (4547/20006)
        // 740-759: Coils, motors & components (4548-4567)
        add.accept(new ItemStack(ModItems.COIL_COPPER.get()));               // 740 (4548)
        add.accept(new ItemStack(ModItems.COIL_COPPER_TORUS.get()));         // 741 (4549)
        add.accept(new ItemStack(ModItems.COIL_GOLD.get()));                 // 742 (4550)
        add.accept(new ItemStack(ModItems.COIL_GOLD_TORUS.get()));           // 743 (4551)
        add.accept(new ItemStack(ModItems.COIL_TUNGSTEN.get()));             // 744 (4552)
        add.accept(new ItemStack(ModItems.COIL_MAGNETIZED_TUNGSTEN.get()));  // 745 (4553)
        add.accept(new ItemStack(ModItems.SAFETY_FUSE.get()));               // 746 (4554)
        add.accept(new ItemStack(ModItems.TANK_STEEL.get()));                // 747 (4555)
        add.accept(new ItemStack(ModItems.MOTOR.get()));                     // 748 (4556)
        add.accept(new ItemStack(ModItems.MOTOR_DESH.get()));                // 749 (4557)
        add.accept(new ItemStack(ModItems.MOTOR_BISMUTH.get()));             // 750 (4558)
        add.accept(new ItemStack(ModItems.CENTRIFUGE_ELEMENT.get()));        // 751 (4559)
        add.accept(new ItemStack(ModItems.REACTOR_CORE.get()));              // 752 (4560)
        add.accept(new ItemStack(ModItems.RTG_UNIT.get()));                  // 753 (4561)
        add.accept(new ItemStack(ModItems.PIPES_STEEL.get()));               // 754 (4562)
        add.accept(new ItemStack(ModItems.DRILL_TITANIUM.get()));            // 755 (4563)
        add.accept(new ItemStack(ModItems.PHOTO_PANEL.get()));               // 756 (4564)
        add.accept(new ItemStack(ModItems.CHLORINE_PINWHEEL.get()));         // 757 (4565)
        add.accept(new ItemStack(ModItems.RING_STARMETAL.get()));            // 758 (4566)
        add.accept(new ItemStack(ModItems.DEUTERIUM_FILTER.get()));          // 759 (4567)
        // 760-775: Chemical Dyes (4568)
        addMeta(add, "chemical_dye_black");     // 760 (4568/0)
        addMeta(add, "chemical_dye_red");       // 761 (4568/1)
        addMeta(add, "chemical_dye_green");     // 762 (4568/2)
        addMeta(add, "chemical_dye_brown");     // 763 (4568/3)
        addMeta(add, "chemical_dye_blue");      // 764 (4568/4)
        addMeta(add, "chemical_dye_purple");    // 765 (4568/5)
        addMeta(add, "chemical_dye_cyan");      // 766 (4568/6)
        addMeta(add, "chemical_dye_silver");    // 767 (4568/7)
        addMeta(add, "chemical_dye_gray");      // 768 (4568/8)
        addMeta(add, "chemical_dye_pink");      // 769 (4568/9)
        addMeta(add, "chemical_dye_lime");      // 770 (4568/10)
        addMeta(add, "chemical_dye_yellow");    // 771 (4568/11)
        addMeta(add, "chemical_dye_lightblue"); // 772 (4568/12)
        addMeta(add, "chemical_dye_magenta");   // 773 (4568/13)
        addMeta(add, "chemical_dye_orange");    // 774 (4568/14)
        addMeta(add, "chemical_dye_white");     // 775 (4568/15)
        // 776-791: Crayons (4569)
        addMeta(add, "crayon_black");     // 776 (4569/0)
        addMeta(add, "crayon_red");       // 777 (4569/1)
        addMeta(add, "crayon_green");     // 778 (4569/2)
        addMeta(add, "crayon_brown");     // 779 (4569/3)
        addMeta(add, "crayon_blue");      // 780 (4569/4)
        addMeta(add, "crayon_purple");    // 781 (4569/5)
        addMeta(add, "crayon_cyan");      // 782 (4569/6)
        addMeta(add, "crayon_silver");    // 783 (4569/7)
        addMeta(add, "crayon_gray");      // 784 (4569/8)
        addMeta(add, "crayon_pink");      // 785 (4569/9)
        addMeta(add, "crayon_lime");      // 786 (4569/10)
        addMeta(add, "crayon_yellow");    // 787 (4569/11)
        addMeta(add, "crayon_lightblue"); // 788 (4569/12)
        addMeta(add, "crayon_magenta");   // 789 (4569/13)
        addMeta(add, "crayon_orange");    // 790 (4569/14)
        addMeta(add, "crayon_white");     // 791 (4569/15)
        // 792-798: Generic Parts (4570)
        addMeta(add, "part_generic_piston_pneumatic"); // 792 (4570/0)
        addMeta(add, "part_generic_piston_hydraulic"); // 793 (4570/1)
        addMeta(add, "part_generic_piston_electric");  // 794 (4570/2)
        addMeta(add, "part_generic_lde");              // 795 (4570/3)
        addMeta(add, "part_generic_hde");              // 796 (4570/4)
        addMeta(add, "part_generic_alclad");           // 797 (4570/6)
        addMeta(add, "part_generic_glass_polarized"); // 798 (4570/5)
        // 799-800: Expensive Items metas 0-1 (4571)
        addMeta(add, "item_expensive_steel_plating"); // 799 (4571/0)
        addMeta(add, "item_expensive_heavy_frame");   // 800 (4571/1)

        // --- CHUNK 9: Items 801-900 (worker_m5_parts_c9) ---
        // 801-809: item_expensive metas 2-10 (4571)
        addMeta(add, "item_expensive_circuit");            // 801 (4571/2)
        addMeta(add, "item_expensive_lead_plating");         // 802 (4571/3)
        addMeta(add, "item_expensive_ferro_plating");        // 803 (4571/4)
        addMeta(add, "item_expensive_computer");             // 804 (4571/5)
        addMeta(add, "item_expensive_bronze_tubes");         // 805 (4571/6)
        addMeta(add, "item_expensive_plastic");              // 806 (4571/7)
        addMeta(add, "item_expensive_gold_dust");            // 807 (4571/8)
        addMeta(add, "item_expensive_degenerate_matter");    // 808 (4571/9)
        addMeta(add, "item_expensive_cmb_tubes");            // 809 (4571/10)
        // 810-812: Legendary Parts (4574)
        addMeta(add, "parts_legendary_tier1");             // 810 (4574/0)
        addMeta(add, "parts_legendary_tier2");             // 811 (4574/1)
        addMeta(add, "parts_legendary_tier3");             // 812 (4574/2)
        // 813-814: Large Gears (4575)
        add.accept(new ItemStack(ModItems.GEAR_LARGE.get()));                                                        // 813 (4575/0)
        addMeta(add, "gear_large_steel");                  // 814 (4575/1)
        // 815: Sawblade (4576)
        add.accept(new ItemStack(ModItems.SAWBLADE.get()));                                                          // 815 (4576/0)
        // 816-825: Light Barrels (4577)
        addMeta(add, "part_barrel_light_steel");            // 816 (4577/30)
        addMeta(add, "part_barrel_light_dura_steel");       // 817 (4577/33)
        addMeta(add, "part_barrel_light_desh");             // 818 (4577/42)
        addMeta(add, "part_barrel_light_tcalloy");          // 819 (4577/36)
        addMeta(add, "part_barrel_light_cdalloy");          // 820 (4577/43)
        addMeta(add, "part_barrel_light_bbronze");          // 821 (4577/46)
        addMeta(add, "part_barrel_light_abronze");          // 822 (4577/47)
        addMeta(add, "part_barrel_light_gunmetal");         // 823 (4577/49)
        addMeta(add, "part_barrel_light_weaponsteel");      // 824 (4577/50)
        addMeta(add, "part_barrel_light_saturnite");        // 825 (4577/34)
        // 826-834: Heavy Barrels (4578)
        addMeta(add, "part_barrel_heavy_steel");            // 826 (4578/30)
        addMeta(add, "part_barrel_heavy_dura_steel");       // 827 (4578/33)
        addMeta(add, "part_barrel_heavy_desh");             // 828 (4578/42)
        addMeta(add, "part_barrel_heavy_ferrouranium");     // 829 (4578/37)
        addMeta(add, "part_barrel_heavy_tcalloy");          // 830 (4578/36)
        addMeta(add, "part_barrel_heavy_cdalloy");          // 831 (4578/43)
        addMeta(add, "part_barrel_heavy_gunmetal");         // 832 (4578/49)
        addMeta(add, "part_barrel_heavy_weaponsteel");      // 833 (4578/50)
        addMeta(add, "part_barrel_heavy_saturnite");        // 834 (4578/34)
        // 835-844: Light Receivers (4579)
        addMeta(add, "part_receiver_light_steel");          // 835 (4579/30)
        addMeta(add, "part_receiver_light_dura_steel");     // 836 (4579/33)
        addMeta(add, "part_receiver_light_desh");           // 837 (4579/42)
        addMeta(add, "part_receiver_light_tcalloy");        // 838 (4579/36)
        addMeta(add, "part_receiver_light_cdalloy");        // 839 (4579/43)
        addMeta(add, "part_receiver_light_bbronze");        // 840 (4579/46)
        addMeta(add, "part_receiver_light_abronze");        // 841 (4579/47)
        addMeta(add, "part_receiver_light_gunmetal");       // 842 (4579/49)
        addMeta(add, "part_receiver_light_weaponsteel");    // 843 (4579/50)
        addMeta(add, "part_receiver_light_saturnite");      // 844 (4579/34)
        // 845-853: Heavy Receivers (4580)
        addMeta(add, "part_receiver_heavy_dura_steel");     // 845 (4580/33)
        addMeta(add, "part_receiver_heavy_ferrouranium");   // 846 (4580/37)
        addMeta(add, "part_receiver_heavy_tcalloy");        // 847 (4580/36)
        addMeta(add, "part_receiver_heavy_cdalloy");        // 848 (4580/43)
        addMeta(add, "part_receiver_heavy_bbronze");        // 849 (4580/46)
        addMeta(add, "part_receiver_heavy_abronze");        // 850 (4580/47)
        addMeta(add, "part_receiver_heavy_gunmetal");       // 851 (4580/49)
        addMeta(add, "part_receiver_heavy_weaponsteel");    // 852 (4580/50)
        addMeta(add, "part_receiver_heavy_saturnite");      // 853 (4580/34)
        // 854-856: Mechanisms (4581)
        addMeta(add, "part_mechanism_gunmetal");            // 854 (4581/49)
        addMeta(add, "part_mechanism_weaponsteel");         // 855 (4581/50)
        addMeta(add, "part_mechanism_saturnite");           // 856 (4581/34)
        // 857-865: Stocks (4582)
        addMeta(add, "part_stock_wood");                    // 857 (4582/3)
        addMeta(add, "part_stock_desh");                    // 858 (4582/42)
        addMeta(add, "part_stock_gunmetal");                // 859 (4582/49)
        addMeta(add, "part_stock_weaponsteel");             // 860 (4582/50)
        addMeta(add, "part_stock_saturnite");               // 861 (4582/34)
        addMeta(add, "part_stock_polymer");                 // 862 (4582/20001)
        addMeta(add, "part_stock_bakelite");                // 863 (4582/20002)
        addMeta(add, "part_stock_pc");                      // 864 (4582/20004)
        addMeta(add, "part_stock_pvc");                     // 865 (4582/20005)
        // 866-878: Grips (4583)
        addMeta(add, "part_grip_wood");                     // 866 (4583/3)
        addMeta(add, "part_grip_ivory");                    // 867 (4583/4)
        addMeta(add, "part_grip_steel");                    // 868 (4583/30)
        addMeta(add, "part_grip_dura_steel");               // 869 (4583/33)
        addMeta(add, "part_grip_desh");                     // 870 (4583/42)
        addMeta(add, "part_grip_gunmetal");                 // 871 (4583/49)
        addMeta(add, "part_grip_weaponsteel");              // 872 (4583/50)
        addMeta(add, "part_grip_saturnite");                // 873 (4583/34)
        addMeta(add, "part_grip_polymer");                  // 874 (4583/20001)
        addMeta(add, "part_grip_bakelite");                 // 875 (4583/20002)
        addMeta(add, "part_grip_rubber");                   // 876 (4583/20003)
        addMeta(add, "part_grip_pc");                       // 877 (4583/20004)
        addMeta(add, "part_grip_pvc");                      // 878 (4583/20005)
        // 879-881: Custom Shape Demos (4584)
        add.accept(new ItemStack(ModItems.DEMOSHAPE_IRON.get()));                                                     // 879 (4584/2600)
        add.accept(new ItemStack(ModItems.DEMOSHAPE_STEEL.get()));                                                    // 880 (4584/30)
        add.accept(new ItemStack(ModItems.DEMOSHAPE_CUSTOM.get()));                                                   // 881 (4584/20006)
        // 882-884: Plant Items (4585)
        addMeta(add, "plant_item_tobacco");                // 882 (4585/0)
        addMeta(add, "plant_item_rope");                   // 883 (4585/1)
        addMeta(add, "plant_item_mustardwillow");           // 884 (4585/2)
        // 885-900: Miscellaneous Mechanism Parts, Fins, Turbines & Catalysts (4586-4601)
        add.accept(new ItemStack(ModItems.ENTANGLEMENT_KIT.get()));                                                   // 885 (4586/0)
        add.accept(new ItemStack(ModItems.FINS_FLAT.get()));                                                          // 886 (4587/0)
        add.accept(new ItemStack(ModItems.FINS_SMALL_STEEL.get()));                                                   // 887 (4588/0)
        add.accept(new ItemStack(ModItems.FINS_BIG_STEEL.get()));                                                     // 888 (4589/0)
        add.accept(new ItemStack(ModItems.FINS_TRI_STEEL.get()));                                                     // 889 (4590/0)
        add.accept(new ItemStack(ModItems.FINS_QUAD_TITANIUM.get()));                                                 // 890 (4591/0)
        add.accept(new ItemStack(ModItems.SPHERE_STEEL.get()));                                                       // 891 (4592/0)
        add.accept(new ItemStack(ModItems.PEDESTAL_STEEL.get()));                                                     // 892 (4593/0)
        add.accept(new ItemStack(ModItems.DYSFUNCTIONAL_REACTOR.get()));                                              // 893 (4594/0)
        add.accept(new ItemStack(ModItems.BLADE_TITANIUM.get()));                                                     // 894 (4595/0)
        add.accept(new ItemStack(ModItems.BLADE_TUNGSTEN.get()));                                                     // 895 (4596/0)
        add.accept(new ItemStack(ModItems.TURBINE_TITANIUM.get()));                                                   // 896 (4597/0)
        add.accept(new ItemStack(ModItems.TURBINE_TUNGSTEN.get()));                                                   // 897 (4598/0)
        add.accept(new ItemStack(ModItems.FLYWHEEL_BERYLLIUM.get()));                                                 // 898 (4599/0)
        add.accept(new ItemStack(ModItems.DUCTTAPE.get()));                                                           // 899 (4600/0)
        add.accept(new ItemStack(ModItems.CATALYST_CLAY.get()));                                                      // 900 (4601/0)

        // --- CHUNK 10: Items 901-1000 (worker_m5_parts_c10) ---
        // 901-926: Missiles, warheads, fuel tanks, thrusters & connectors (4602-4627)
        add.accept(new ItemStack(ModItems.MISSILE_ASSEMBLY.get()));                                                  // 901 (4602/0)
        add.accept(new ItemStack(ModItems.WARHEAD_GENERIC_SMALL.get()));                                             // 902 (4603/0)
        add.accept(new ItemStack(ModItems.WARHEAD_GENERIC_MEDIUM.get()));                                            // 903 (4604/0)
        add.accept(new ItemStack(ModItems.WARHEAD_GENERIC_LARGE.get()));                                             // 904 (4605/0)
        add.accept(new ItemStack(ModItems.WARHEAD_INCENDIARY_SMALL.get()));                                          // 905 (4606/0)
        add.accept(new ItemStack(ModItems.WARHEAD_INCENDIARY_MEDIUM.get()));                                         // 906 (4607/0)
        add.accept(new ItemStack(ModItems.WARHEAD_INCENDIARY_LARGE.get()));                                          // 907 (4608/0)
        add.accept(new ItemStack(ModItems.WARHEAD_CLUSTER_SMALL.get()));                                             // 908 (4609/0)
        add.accept(new ItemStack(ModItems.WARHEAD_CLUSTER_MEDIUM.get()));                                            // 909 (4610/0)
        add.accept(new ItemStack(ModItems.WARHEAD_CLUSTER_LARGE.get()));                                             // 910 (4611/0)
        add.accept(new ItemStack(ModItems.WARHEAD_BUSTER_SMALL.get()));                                              // 911 (4612/0)
        add.accept(new ItemStack(ModItems.WARHEAD_BUSTER_MEDIUM.get()));                                             // 912 (4613/0)
        add.accept(new ItemStack(ModItems.WARHEAD_BUSTER_LARGE.get()));                                              // 913 (4614/0)
        add.accept(new ItemStack(ModItems.WARHEAD_NUCLEAR.get()));                                                   // 914 (4615/0)
        add.accept(new ItemStack(ModItems.WARHEAD_MIRV.get()));                                                      // 915 (4616/0)
        add.accept(new ItemStack(ModItems.WARHEAD_VOLCANO.get()));                                                   // 916 (4617/0)
        add.accept(new ItemStack(ModItems.FUEL_TANK_SMALL.get()));                                                    // 917 (4618/0)
        add.accept(new ItemStack(ModItems.FUEL_TANK_MEDIUM.get()));                                                   // 918 (4619/0)
        add.accept(new ItemStack(ModItems.FUEL_TANK_LARGE.get()));                                                    // 919 (4620/0)
        add.accept(new ItemStack(ModItems.THRUSTER_SMALL.get()));                                                     // 920 (4621/0)
        add.accept(new ItemStack(ModItems.THRUSTER_MEDIUM.get()));                                                    // 921 (4622/0)
        add.accept(new ItemStack(ModItems.THRUSTER_LARGE.get()));                                                     // 922 (4623/0)
        add.accept(new ItemStack(ModItems.THRUSTER_NUCLEAR.get()));                                                   // 923 (4624/0)
        add.accept(new ItemStack(ModItems.SEG_10.get()));                                                             // 924 (4625/0)
        add.accept(new ItemStack(ModItems.SEG_15.get()));                                                             // 925 (4626/0)
        add.accept(new ItemStack(ModItems.SEG_20.get()));                                                             // 926 (4627/0)
        // 927-930: Combine scrap & Shimmer parts (4628-4631)
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.COMBINE_SCRAP, MaterialShape.SCRAP)));          // 927 (4628/0)
        add.accept(new ItemStack(ModItems.SHIMMER_HEAD.get()));                                                      // 928 (4629/0)
        add.accept(new ItemStack(ModItems.SHIMMER_AXE_HEAD.get()));                                                  // 929 (4630/0)
        add.accept(new ItemStack(ModItems.SHIMMER_HANDLE.get()));                                                    // 930 (4631/0)
        // 931-951: Circuits (4632)
        add.accept(new ItemStack(ModItems.VACUUM_TUBE.get()));                                                       // 931 (4632/0)
        add.accept(new ItemStack(ModItems.CIRCUIT_NUMITRON.get()));                                                  // 932 (4632/19)
        add.accept(new ItemStack(ModItems.CAPACITOR.get()));                                                         // 933 (4632/1)
        add.accept(new ItemStack(ModItems.CAPACITOR_TANTALUM.get()));                                                // 934 (4632/2)
        add.accept(new ItemStack(ModItems.ATOMIC_CLOCK.get()));                                                      // 935 (4632/18)
        add.accept(new ItemStack(ModItems.PCB.get()));                                                               // 936 (4632/3)
        add.accept(new ItemStack(ModItems.SILICON_CIRCUIT.get()));                                                   // 937 (4632/4)
        add.accept(new ItemStack(ModItems.MICROCHIP.get()));                                                         // 938 (4632/5)
        add.accept(new ItemStack(ModItems.BISMOID_CHIP.get()));                                                      // 939 (4632/6)
        add.accept(new ItemStack(ModItems.QUANTUM_CHIP.get()));                                                      // 940 (4632/16)
        add.accept(new ItemStack(ModItems.ANALOG_CIRCUIT.get()));                                                    // 941 (4632/7)
        add.accept(new ItemStack(ModItems.INTEGRATED_CIRCUIT.get()));                                                // 942 (4632/8)
        add.accept(new ItemStack(ModItems.ADVANCED_CIRCUIT.get()));                                                  // 943 (4632/9)
        add.accept(new ItemStack(ModItems.CAPACITOR_BOARD.get()));                                                   // 944 (4632/10)
        add.accept(new ItemStack(ModItems.BISMOID_CIRCUIT.get()));                                                   // 945 (4632/11)
        add.accept(new ItemStack(ModItems.QUANTUM_CIRCUIT.get()));                                                   // 946 (4632/15)
        add.accept(new ItemStack(ModItems.CIRCUIT_CRYSTAL.get()));                                                   // 947 (4632/20)
        add.accept(new ItemStack(ModItems.CONTROLLER_CHASSIS.get()));                                                // 948 (4632/12)
        add.accept(new ItemStack(ModItems.CONTROLLER.get()));                                                        // 949 (4632/13)
        add.accept(new ItemStack(ModItems.CONTROLLER_ADVANCED.get()));                                               // 950 (4632/14)
        add.accept(new ItemStack(ModItems.QUANTUM_COMPUTER.get()));                                                  // 951 (4632/17)
        // 952-962: Drives (4633)
        addMeta(add, "drive_flash_empty");                 // 952 (4633/0)
        addMeta(add, "drive_disk_empty");                  // 953 (4633/1)
        addMeta(add, "drive_flash_broken");                // 954 (4633/2)
        addMeta(add, "drive_disk_broken");                 // 955 (4633/3)
        addMeta(add, "drive_flash_flightsim");             // 956 (4633/4)
        addMeta(add, "drive_flash_particlesim");           // 957 (4633/5)
        addMeta(add, "drive_disk_flightdata");             // 958 (4633/6)
        addMeta(add, "drive_disk_flightdata_processed");    // 959 (4633/7)
        addMeta(add, "drive_disk_orbitdata");              // 960 (4633/8)
        addMeta(add, "drive_disk_orbitdata_processed");     // 961 (4633/9)
        addMeta(add, "drive_klaus");                       // 962 (4633/10)
        // 963: Cathode Ray Tube (4634)
        add.accept(new ItemStack(ModItems.CRT_DISPLAY.get()));                                                       // 963 (4634/0)
        // 964-970: Casings (4638)
        addMeta(add, "casing_small");                      // 964 (4638/0)
        addMeta(add, "casing_large");                      // 965 (4638/1)
        addMeta(add, "casing_small_steel");                // 966 (4638/2)
        addMeta(add, "casing_large_steel");                // 967 (4638/3)
        addMeta(add, "casing_shotshell");                  // 968 (4638/4)
        addMeta(add, "casing_buckshot");                   // 969 (4638/5)
        addMeta(add, "casing_buckshot_advanced");          // 970 (4638/6)
        // 971: Mini Nuke Shell (4639)
        add.accept(new ItemStack(ModItems.ASSEMBLY_NUKE.get()));                                                     // 971 (4639/0)
        // 972: Cable Drum (4640)
        add.accept(new ItemStack(ModItems.WIRING_RED_COPPER.get()));                                                 // 972 (4640/0)
        // 973-976: Flame paintings & topics (4641-4644)
        add.accept(new ItemStack(ModItems.FLAME_PONY.get()));                                                         // 973 (4641/0)
        add.accept(new ItemStack(ModItems.FLAME_CONSPIRACY.get()));                                                  // 974 (4642/0)
        add.accept(new ItemStack(ModItems.FLAME_POLITICS.get()));                                                    // 975 (4643/0)
        add.accept(new ItemStack(ModItems.FLAME_OPINION.get()));                                                     // 976 (4644/0)
        // 977-980: Pellets (4656-4659)
        add.accept(new ItemStack(ModItems.PELLET_CLUSTER.get()));                                                     // 977 (4656/0)
        add.accept(new ItemStack(ModItems.PELLET_BUCKSHOT.get()));                                                    // 978 (4657/0)
        add.accept(new ItemStack(ModItems.PELLET_CHARGED.get()));                                                     // 979 (4658/0)
        add.accept(new ItemStack(ModItems.PELLET_GAS.get()));                                                         // 980 (4659/0)
        // 981: Magnetron (4660)
        add.accept(new ItemStack(ModItems.MAGNETRON.get()));                                                          // 981 (4660/0)
        // 982-1000: Foundry Scraps (4767) - first 19 entries
        addScrap(add, ModMaterials.STONE);                                                                           // 982 (4767/0)
        addScrap(add, ModMaterials.CARBON);                                                                          // 983 (4767/699)
        addScrap(add, ModMaterials.IRON);                                                                            // 984 (4767/2600)
        addScrap(add, ModMaterials.GOLD);                                                                            // 985 (4767/7900)
        addScrap(add, ModMaterials.REDSTONE);                                                                        // 986 (4767/1)
        addScrap(add, ModMaterials.OBSIDIAN);                                                                        // 987 (4767/2)
        addScrap(add, ModMaterials.HEMATITE);                                                                        // 988 (4767/2601)
        addScrap(add, ModMaterials.WROUGHT_IRON);                                                                    // 989 (4767/2602)
        addScrap(add, ModMaterials.PIG_IRON);                                                                        // 990 (4767/2603)
        addScrap(add, ModMaterials.METEORITE);                                                                       // 991 (4767/2604)
        addScrap(add, ModMaterials.MALACHITE);                                                                       // 992 (4767/2901)
        addScrap(add, ModMaterials.URANIUM);                                                                         // 993 (4767/9200)
        addScrap(add, ModMaterials.URANIUM233);                                                                      // 994 (4767/9233)
        addScrap(add, ModMaterials.URANIUM235);                                                                      // 995 (4767/9235)
        addScrap(add, ModMaterials.URANIUM238);                                                                      // 996 (4767/9238)
        addScrap(add, ModMaterials.THORIUM232);                                                                      // 997 (4767/9032)
        addScrap(add, ModMaterials.PLUTONIUM);                                                                       // 998 (4767/9400)
        addScrap(add, ModMaterials.PLUTONIUM_RG);                                                                    // 999 (4767/9401)
        addScrap(add, ModMaterials.PLUTONIUM238);                                                                    // 1000 (4767/9438)

        // --- CHUNK 11: Items 1001-1100 (worker_m5_parts_c11) ---
        addScrap(add, ModMaterials.PLUTONIUM239);                                                   // 1001 (4767/9439)
        addScrap(add, ModMaterials.PLUTONIUM240);                                                   // 1002 (4767/9440)
        addScrap(add, ModMaterials.PLUTONIUM241);                                                   // 1003 (4767/9441)
        addScrap(add, ModMaterials.AMERICIUM_RG);                                                   // 1004 (4767/9501)
        addScrap(add, ModMaterials.AM241);                                                          // 1005 (4767/9541)
        addScrap(add, ModMaterials.AM242);                                                          // 1006 (4767/9542)
        addScrap(add, ModMaterials.NEPTUNIUM);                                                      // 1007 (4767/9337)
        addScrap(add, ModMaterials.POLONIUM);                                                       // 1008 (4767/8410)
        addScrap(add, ModMaterials.TECHNETIUM);                                                     // 1009 (4767/4399)
        addScrap(add, ModMaterials.RA226);                                                          // 1010 (4767/8826)
        addScrap(add, ModMaterials.ACTINIUM);                                                       // 1011 (4767/8927)
        addScrap(add, ModMaterials.CO60);                                                           // 1012 (4767/2760)
        addScrap(add, ModMaterials.AU198);                                                          // 1013 (4767/7998)
        addScrap(add, ModMaterials.PB209);                                                          // 1014 (4767/8209)
        addScrap(add, ModMaterials.SCHRABIDIUM);                                                    // 1015 (4767/12626)
        addScrap(add, ModMaterials.SOLINIUM);                                                       // 1016 (4767/12627)
        addScrap(add, ModMaterials.SCHRABIDATE);                                                    // 1017 (4767/12600)
        addScrap(add, ModMaterials.SCHRARANIUM);                                                    // 1018 (4767/12601)
        addScrap(add, ModMaterials.GHIORSIUM);                                                      // 1019 (4767/12836)
        addScrap(add, ModMaterials.TITANIUM);                                                       // 1020 (4767/2200)
        addScrap(add, ModMaterials.COPPER);                                                         // 1021 (4767/2900)
        addScrap(add, ModMaterials.TUNGSTEN);                                                       // 1022 (4767/7400)
        addScrap(add, ModMaterials.ALUMINIUM);                                                      // 1023 (4767/1300)
        addScrap(add, ModMaterials.LEAD);                                                           // 1024 (4767/8200)
        addScrap(add, ModMaterials.BISMUTH);                                                        // 1025 (4767/8300)
        addScrap(add, ModMaterials.ARSENIC);                                                        // 1026 (4767/3300)
        addScrap(add, ModMaterials.TANTALIUM);                                                      // 1027 (4767/7300)
        addScrap(add, ModMaterials.NEODYMIUM);                                                      // 1028 (4767/6000)
        addScrap(add, ModMaterials.NIOBIUM);                                                        // 1029 (4767/4100)
        addScrap(add, ModMaterials.BERYLLIUM);                                                      // 1030 (4767/400)
        addScrap(add, ModMaterials.COBALT);                                                         // 1031 (4767/2700)
        addScrap(add, ModMaterials.BORON);                                                          // 1032 (4767/500)
        addScrap(add, ModMaterials.BORAX);                                                          // 1033 (4767/501)
        addScrap(add, ModMaterials.LANTHANIUM);                                                     // 1034 (4767/5700)
        addScrap(add, ModMaterials.ZIRCONIUM);                                                      // 1035 (4767/4000)
        addScrap(add, ModMaterials.SODIUM);                                                         // 1036 (4767/1100)
        addScrap(add, ModMaterials.STRONTIUM);                                                      // 1037 (4767/3800)
        addScrap(add, ModMaterials.CALCIUM);                                                        // 1038 (4767/2000)
        addScrap(add, ModMaterials.LITHIUM);                                                        // 1039 (4767/300)
        addScrap(add, ModMaterials.CADMIUM);                                                        // 1040 (4767/4800)
        addScrap(add, ModMaterials.SILICON);                                                        // 1041 (4767/1400)
        addScrap(add, ModMaterials.ASBESTOS);                                                       // 1042 (4767/1401)
        addScrap(add, ModMaterials.OSMIRIDIUM);                                                     // 1043 (4767/7699)
        addScrap(add, ModMaterials.STEEL);                                                          // 1044 (4767/30)
        addScrap(add, ModMaterials.RED_COPPER);                                                     // 1045 (4767/31)
        addScrap(add, ModMaterials.DURA_STEEL);                                                     // 1046 (4767/33)
        addScrap(add, ModMaterials.DESH);                                                           // 1047 (4767/42)
        addScrap(add, ModMaterials.STAR_METAL);                                                     // 1048 (4767/35)
        addScrap(add, ModMaterials.FERROURANIUM);                                                   // 1049 (4767/37)
        addScrap(add, ModMaterials.TCALLOY);                                                        // 1050 (4767/36)
        addScrap(add, ModMaterials.CDALLOY);                                                        // 1051 (4767/43)
        addScrap(add, ModMaterials.BBRONZE);                                                        // 1052 (4767/46)
        addScrap(add, ModMaterials.ABRONZE);                                                        // 1053 (4767/47)
        addScrap(add, ModMaterials.BSCCO);                                                          // 1054 (4767/48)
        addScrap(add, ModMaterials.MAGNETIZED_TUNGSTEN);                                            // 1055 (4767/38)
        addScrap(add, ModMaterials.COMBINE_STEEL);                                                  // 1056 (4767/39)
        addScrap(add, ModMaterials.DNT);                                                            // 1057 (4767/45)
        addScrap(add, ModMaterials.FLUX);                                                           // 1058 (4767/40)
        addScrap(add, ModMaterials.SLAG);                                                           // 1059 (4767/41)
        addScrap(add, ModMaterials.MUD);                                                            // 1060 (4767/44)
        addScrap(add, ModMaterials.GUNMETAL);                                                       // 1061 (4767/49)
        addScrap(add, ModMaterials.WEAPONSTEEL);                                                    // 1062 (4767/50)
        addScrap(add, ModMaterials.SATURNITE);                                                      // 1063 (4767/34)
        addScrap(add, ModMaterials.DEMOMAT);                                                        // 1064 (4767/20006)
        add.accept(new ItemStack(ModItems.UPGRADE_MUFFLER.get()));                                  // 1065 (4768/0)
        add.accept(new ItemStack(ModItems.UPGRADE_TEMPLATE.get()));                                 // 1066 (4769/0)
        add.accept(new ItemStack(ModItems.RUNE_BLANK.get()));                                       // 1067 (4826/0)
        add.accept(new ItemStack(ModItems.RUNE_ISA.get()));                                         // 1068 (4827/0)
        add.accept(new ItemStack(ModItems.RUNE_DAGAZ.get()));                                       // 1069 (4828/0)
        add.accept(new ItemStack(ModItems.RUNE_HAGALAZ.get()));                                     // 1070 (4829/0)
        add.accept(new ItemStack(ModItems.RUNE_JERA.get()));                                        // 1071 (4830/0)
        add.accept(new ItemStack(ModItems.RUNE_THURISAZ.get()));                                    // 1072 (4831/0)
        add.accept(new ItemStack(ModItems.WASTE_NATURAL_URANIUM.get()));                            // 1073 (4880/0)
        addMeta(add, "waste_natural_uranium_cooling"); // 1074 (4880/1)
        add.accept(new ItemStack(ModItems.WASTE_URANIUM.get()));                                    // 1075 (4881/0)
        addMeta(add, "waste_uranium_cooling"); // 1076 (4881/1)
        add.accept(new ItemStack(ModItems.WASTE_THORIUM.get()));                                    // 1077 (4882/0)
        addMeta(add, "waste_thorium_cooling"); // 1078 (4882/1)
        add.accept(new ItemStack(ModItems.WASTE_MOX.get()));                                        // 1079 (4883/0)
        addMeta(add, "waste_mox_cooling"); // 1080 (4883/1)
        add.accept(new ItemStack(ModItems.WASTE_PLUTONIUM.get()));                                  // 1081 (4884/0)
        addMeta(add, "waste_plutonium_cooling"); // 1082 (4884/1)
        add.accept(new ItemStack(ModItems.WASTE_U233.get()));                                       // 1083 (4885/0)
        addMeta(add, "waste_u233_cooling"); // 1084 (4885/1)
        add.accept(new ItemStack(ModItems.WASTE_U235.get()));                                       // 1085 (4886/0)
        addMeta(add, "waste_u235_cooling"); // 1086 (4886/1)
        add.accept(new ItemStack(ModItems.WASTE_SCHRABIDIUM.get()));                                // 1087 (4887/0)
        addMeta(add, "waste_schrabidium_cooling"); // 1088 (4887/1)
        add.accept(new ItemStack(ModItems.WASTE_ZFB_MOX.get()));                                    // 1089 (4888/0)
        addMeta(add, "waste_zfb_mox_cooling"); // 1090 (4888/1)
        add.accept(new ItemStack(ModItems.UNDEFINED.get()));                                        // 1091 (4995/0)
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.SCRAP, MaterialShape.SCRAP)));  // 1092 (4997/0)
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.SCRAP_OIL, MaterialShape.SCRAP))); // 1093 (4998/0)
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.SCRAP_NUCLEAR, MaterialShape.SCRAP))); // 1094 (4999/0)
        add.accept(new ItemStack(ModItems.TRINITITE.get()));                                        // 1095 (5000/0)
        addMeta(add, "nuclear_waste_long_u235"); // 1096 (5001/0)
        addMeta(add, "nuclear_waste_long_u233"); // 1097 (5001/1)
        addMeta(add, "nuclear_waste_long_neptunium"); // 1098 (5001/2)
        addMeta(add, "nuclear_waste_long_thorium"); // 1099 (5001/3)
        addMeta(add, "nuclear_waste_long_schrabidium"); // 1100 (5001/4)

        // --- CHUNK 12: Items 1101-1158 (worker_m5_parts_c12) ---
        addMeta(add, "nuclear_waste_long_tiny_u235"); // 1101 (5002/0)
        addMeta(add, "nuclear_waste_long_tiny_u233"); // 1102 (5002/1)
        addMeta(add, "nuclear_waste_long_tiny_neptunium"); // 1103 (5002/2)
        addMeta(add, "nuclear_waste_long_tiny_thorium"); // 1104 (5002/3)
        addMeta(add, "nuclear_waste_long_tiny_schrabidium"); // 1105 (5002/4)
        addMeta(add, "nuclear_waste_short_u235"); // 1106 (5003/0)
        addMeta(add, "nuclear_waste_short_u233"); // 1107 (5003/1)
        addMeta(add, "nuclear_waste_short_neptunium"); // 1108 (5003/2)
        addMeta(add, "nuclear_waste_short_pu239"); // 1109 (5003/3)
        addMeta(add, "nuclear_waste_short_pu240"); // 1110 (5003/4)
        addMeta(add, "nuclear_waste_short_pu241"); // 1111 (5003/5)
        addMeta(add, "nuclear_waste_short_am242"); // 1112 (5003/6)
        addMeta(add, "nuclear_waste_short_schrabidium"); // 1113 (5003/7)
        addMeta(add, "nuclear_waste_short_tiny_u235"); // 1114 (5004/0)
        addMeta(add, "nuclear_waste_short_tiny_u233"); // 1115 (5004/1)
        addMeta(add, "nuclear_waste_short_tiny_neptunium"); // 1116 (5004/2)
        addMeta(add, "nuclear_waste_short_tiny_pu239"); // 1117 (5004/3)
        addMeta(add, "nuclear_waste_short_tiny_pu240"); // 1118 (5004/4)
        addMeta(add, "nuclear_waste_short_tiny_pu241"); // 1119 (5004/5)
        addMeta(add, "nuclear_waste_short_tiny_am242"); // 1120 (5004/6)
        addMeta(add, "nuclear_waste_short_tiny_schrabidium"); // 1121 (5004/7)
        addMeta(add, "nuclear_waste_long_depleted_u235"); // 1122 (5005/0)
        addMeta(add, "nuclear_waste_long_depleted_u233"); // 1123 (5005/1)
        addMeta(add, "nuclear_waste_long_depleted_neptunium"); // 1124 (5005/2)
        addMeta(add, "nuclear_waste_long_depleted_thorium"); // 1125 (5005/3)
        addMeta(add, "nuclear_waste_long_depleted_schrabidium"); // 1126 (5005/4)
        addMeta(add, "nuclear_waste_long_depleted_tiny_u235"); // 1127 (5006/0)
        addMeta(add, "nuclear_waste_long_depleted_tiny_u233"); // 1128 (5006/1)
        addMeta(add, "nuclear_waste_long_depleted_tiny_neptunium"); // 1129 (5006/2)
        addMeta(add, "nuclear_waste_long_depleted_tiny_thorium"); // 1130 (5006/3)
        addMeta(add, "nuclear_waste_long_depleted_tiny_schrabidium"); // 1131 (5006/4)
        addMeta(add, "nuclear_waste_short_depleted_u235"); // 1132 (5007/0)
        addMeta(add, "nuclear_waste_short_depleted_u233"); // 1133 (5007/1)
        addMeta(add, "nuclear_waste_short_depleted_neptunium"); // 1134 (5007/2)
        addMeta(add, "nuclear_waste_short_depleted_pu239"); // 1135 (5007/3)
        addMeta(add, "nuclear_waste_short_depleted_pu240"); // 1136 (5007/4)
        addMeta(add, "nuclear_waste_short_depleted_pu241"); // 1137 (5007/5)
        addMeta(add, "nuclear_waste_short_depleted_am242"); // 1138 (5007/6)
        addMeta(add, "nuclear_waste_short_depleted_schrabidium"); // 1139 (5007/7)
        addMeta(add, "nuclear_waste_short_depleted_tiny_u235"); // 1140 (5008/0)
        addMeta(add, "nuclear_waste_short_depleted_tiny_u233"); // 1141 (5008/1)
        addMeta(add, "nuclear_waste_short_depleted_tiny_neptunium"); // 1142 (5008/2)
        addMeta(add, "nuclear_waste_short_depleted_tiny_pu239"); // 1143 (5008/3)
        addMeta(add, "nuclear_waste_short_depleted_tiny_pu240"); // 1144 (5008/4)
        addMeta(add, "nuclear_waste_short_depleted_tiny_pu241"); // 1145 (5008/5)
        addMeta(add, "nuclear_waste_short_depleted_tiny_am242"); // 1146 (5008/6)
        addMeta(add, "nuclear_waste_short_depleted_tiny_schrabidium"); // 1147 (5008/7)
        add.accept(new ItemStack(ModItems.NUCLEAR_WASTE.get()));                                    // 1148 (5009/0)
        add.accept(new ItemStack(ModItems.NUCLEAR_WASTE_TINY.get()));                               // 1149 (5010/0)
        add.accept(new ItemStack(ModItems.NUCLEAR_WASTE_VITRIFIED.get()));                          // 1150 (5011/0)
        add.accept(new ItemStack(ModItems.NUCLEAR_WASTE_VITRIFIED_TINY.get()));                     // 1151 (5012/0)
        add.accept(new ItemStack(ModItems.LAUNCH_CODE_PIECE.get()));                                // 1152 (5059/0)
        add.accept(new ItemStack(ModItems.LAUNCH_CODE.get()));                                      // 1153 (5060/0)
        add.accept(new ItemStack(ModItems.LAUNCH_KEY.get()));                                       // 1154 (5061/0)
        add.accept(new ItemStack(ModItems.ORBITAL_ASSEMBLY_CRYSTAL_CIRCUIT.get()));                 // 1155 (5230/0)
        add.accept(new ItemStack(ModItems.WRENCH.get()));                                           // 1156 (5405/0)
        add.accept(new ItemStack(ModItems.CRYSTAL_HORN.get()));                                     // 1157 (5806/0)
        add.accept(new ItemStack(ModItems.CRYSTAL_CHARRED.get()));                                  // 1158 (5807/0)

    }

    /** populateControlTab: порядок из оригинального 1.7.10 (ModBlocks/ModItems, вкладка control); отсутствующие в порте предметы пропущены. */
    public static void populateControlTab(BiConsumer<ItemStack, CreativeModeTab.TabVisibility> acceptor) {
        Consumer<ItemStack> add = stack -> acceptor.accept(stack, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);

        // --- CHUNK 1: Items 1-100 (worker_m5_control_c1) ---
        // 1-10: RTG Pellets (Radium-226 .. Lead-209)
        add.accept(new ItemStack(ModItems.PELLET_RTG_RADIUM.get()));                             // 1 (4645/0)
        add.accept(new ItemStack(ModItems.PELLET_RTG_WEAK.get()));                               // 2 (4646/0)
        add.accept(new ItemStack(ModItems.PELLET_RTG.get()));                                    // 3 (4647/0)
        add.accept(new ItemStack(ModItems.PELLET_RTG_STRONTIUM.get()));                          // 4 (4648/0)
        add.accept(new ItemStack(ModItems.PELLET_RTG_COBALT.get()));                             // 5 (4649/0)
        add.accept(new ItemStack(ModItems.PELLET_RTG_ACTINIUM.get()));                           // 6 (4650/0)
        add.accept(new ItemStack(ModItems.PELLET_RTG_POLONIUM.get()));                           // 7 (4651/0)
        add.accept(new ItemStack(ModItems.PELLET_RTG_AMERICIUM.get()));                          // 8 (4652/0)
        add.accept(new ItemStack(ModItems.PELLET_RTG_GOLD.get()));                               // 9 (4653/0)
        add.accept(new ItemStack(ModItems.PELLET_RTG_LEAD.get()));                               // 10 (4654/0)
        // 11-20: Depleted RTG Pellets (11-16), Combustion Engine Piston (17), Piston Sets (18-20)
        add.accept(new ItemStack(ModItems.PELLET_RTG_DEPLETED_BISMUTH.get()));                   // 11 (4655/0)
        add.accept(new ItemStack(ModItems.PELLET_RTG_DEPLETED_MERCURY.get()));                   // 12 (4655/1)
        add.accept(new ItemStack(ModItems.PELLET_RTG_DEPLETED_NEPTUNIUM.get()));                 // 13 (4655/2)
        add.accept(new ItemStack(ModItems.PELLET_RTG_DEPLETED_LEAD.get()));                      // 14 (4655/3)
        add.accept(new ItemStack(ModItems.PELLET_RTG_DEPLETED_ZIRCONIUM.get()));                 // 15 (4655/4)
        add.accept(new ItemStack(ModItems.PELLET_RTG_DEPLETED_NICKEL.get()));                    // 16 (4655/5)
        add.accept(new ItemStack(ModItems.PISTON_SELENIUM.get()));                               // 17 (4661/0)
        add.accept(new ItemStack(ModItems.PISTON_SET_STEEL.get()));                              // 18 (4662/0)
        add.accept(new ItemStack(ModItems.PISTON_SET_DURA.get()));                               // 19 (4662/1)
        add.accept(new ItemStack(ModItems.PISTON_SET_DESH.get()));                               // 20 (4662/2)
        // 21-30: Starmetal Piston Set (21), Drill bits (Steel .. Ferrouranium, 22-30)
        add.accept(new ItemStack(ModItems.PISTON_SET_STARMETAL.get()));                          // 21 (4662/3)
        add.accept(new ItemStack(ModItems.DRILLBIT_STEEL.get()));                                // 22 (4663/0)
        add.accept(new ItemStack(ModItems.DRILLBIT_STEEL_DIAMOND.get()));                        // 23 (4663/1)
        add.accept(new ItemStack(ModItems.DRILLBIT_HSS.get()));                                  // 24 (4663/2)
        add.accept(new ItemStack(ModItems.DRILLBIT_HSS_DIAMOND.get()));                          // 25 (4663/3)
        add.accept(new ItemStack(ModItems.DRILLBIT_DESH.get()));                                 // 26 (4663/4)
        add.accept(new ItemStack(ModItems.DRILLBIT_DESH_DIAMOND.get()));                         // 27 (4663/5)
        add.accept(new ItemStack(ModItems.DRILLBIT_TCALLOY.get()));                              // 28 (4663/6)
        add.accept(new ItemStack(ModItems.DRILLBIT_TCALLOY_DIAMOND.get()));                      // 29 (4663/7)
        add.accept(new ItemStack(ModItems.DRILLBIT_FERRO.get()));                                // 30 (4663/8)
        // 31-40: Diamond Ferrouranium Drill bit (31), Gas/Liquid Cells (Empty .. Gaseous Balefire, 32-40)
        add.accept(new ItemStack(ModItems.DRILLBIT_FERRO_DIAMOND.get()));                        // 31 (4663/9)
        add.accept(new ItemStack(ModItems.CELL_EMPTY.get()));                                    // 32 (4664/0)
        add.accept(new ItemStack(ModItems.CELL_UF6.get()));                                      // 33 (4665/0)
        add.accept(new ItemStack(ModItems.CELL_PUF6.get()));                                     // 34 (4666/0)
        add.accept(new ItemStack(ModItems.CELL_DEUTERIUM.get()));                                // 35 (4667/0)
        add.accept(new ItemStack(ModItems.CELL_TRITIUM.get()));                                  // 36 (4668/0)
        add.accept(new ItemStack(ModItems.CELL_SAS3.get()));                                     // 37 (4669/0)
        add.accept(new ItemStack(ModItems.CELL_ANTIMATTER.get()));                               // 38 (4670/0)
        add.accept(new ItemStack(ModItems.CELL_ANTI_SCHRABIDIUM.get()));                         // 39 (4671/0)
        add.accept(new ItemStack(ModItems.CELL_BALEFIRE.get()));                                 // 40 (4672/0)
        // 41-50: Particle Accelerator Coils (41-44), Particle Capsules (Empty .. Antischrabidium, 45-50)
        add.accept(new ItemStack(ModItems.PA_COIL_GOLD.get()));                                  // 41 (4675/0)
        add.accept(new ItemStack(ModItems.PA_COIL_NIOBIUM.get()));                               // 42 (4675/1)
        add.accept(new ItemStack(ModItems.PA_COIL_BSCCO.get()));                                 // 43 (4675/2)
        add.accept(new ItemStack(ModItems.PA_COIL_CHLOROPHYTE.get()));                           // 44 (4675/3)
        add.accept(new ItemStack(ModItems.PARTICLE_EMPTY.get()));                                // 45 (4676/0)
        add.accept(new ItemStack(ModItems.PARTICLE_HYDROGEN.get()));                             // 46 (4677/0)
        add.accept(new ItemStack(ModItems.PARTICLE_COPPER.get()));                               // 47 (4678/0)
        add.accept(new ItemStack(ModItems.PARTICLE_LEAD.get()));                                 // 48 (4679/0)
        add.accept(new ItemStack(ModItems.PARTICLE_AMAT.get()));                                 // 49 (4680/0)
        add.accept(new ItemStack(ModItems.PARTICLE_ASCHRAB.get()));                              // 50 (4681/0)
        // 51-60: Particle Capsules (Higgs .. Lutece, 51-58), Singularities (Standard & Counter-Resonant, 59-60)
        add.accept(new ItemStack(ModItems.PARTICLE_HIGGS.get()));                                // 51 (4682/0)
        add.accept(new ItemStack(ModItems.PARTICLE_MUON.get()));                                 // 52 (4683/0)
        add.accept(new ItemStack(ModItems.PARTICLE_TACHYON.get()));                              // 53 (4684/0)
        add.accept(new ItemStack(ModItems.PARTICLE_STRANGE.get()));                              // 54 (4685/0)
        add.accept(new ItemStack(ModItems.PARTICLE_DARK.get()));                                 // 55 (4686/0)
        add.accept(new ItemStack(ModItems.PARTICLE_SPARKTICLE.get()));                           // 56 (4687/0)
        add.accept(new ItemStack(ModItems.PARTICLE_DIGAMMA.get()));                              // 57 (4688/0)
        add.accept(new ItemStack(ModItems.PARTICLE_LUTECE.get()));                               // 58 (4689/0)
        add.accept(new ItemStack(ModItems.SINGULARITY.get()));                                   // 59 (4690/0)
        add.accept(new ItemStack(ModItems.SINGULARITY_COUNTER_RESONANT.get()));                  // 60 (4691/0)
        // 61-70: Singularities (61-63), Xen Crystal (64), Antimatter Cluster (65), Infinite Water (66-67), Fuel Additives (68-69), Canister Empty (70)
        add.accept(new ItemStack(ModItems.SINGULARITY_SUPER_HEATED.get()));                      // 61 (4692/0)
        add.accept(new ItemStack(ModItems.BLACK_HOLE.get()));                                    // 62 (4693/0)
        add.accept(new ItemStack(ModItems.SINGULARITY_SPARK.get()));                             // 63 (4694/0)
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.XEN, MaterialShape.CRYSTAL))); // 64 (4695/0)
        add.accept(new ItemStack(ModItems.PELLET_ANTIMATTER.get()));                             // 65 (4696/0)
        add.accept(new ItemStack(ModItems.INFINITE_WATER_500.get()));                            // 66 (4697/0)
        add.accept(new ItemStack(ModItems.INFINITE_WATER_5000.get()));                           // 67 (4698/0)
        add.accept(new ItemStack(ModItems.FUEL_ADDITIVE_ANTIKNOCK.get()));                       // 68 (4699/0)
        add.accept(new ItemStack(ModItems.FUEL_ADDITIVE_DEICER.get()));                          // 69 (4699/1)
        add.accept(new ItemStack(ModItems.CANISTER_EMPTY.get()));                                // 70 (4700/0)
        // 71-80: Canisters (crude_oil .. naphtha_crack, 1-10 of 43)
        add.accept(ItemFluidTank.make(ModItems.CANISTER_FULL.get(), ModFluids.CRUDE_OIL.getSource(), 1)); // 71 (4701/10)
        add.accept(ItemFluidTank.make(ModItems.CANISTER_FULL.get(), ModFluids.OIL_DS.getSource(), 1)); // 72 (4701/133)
        add.accept(ItemFluidTank.make(ModItems.CANISTER_FULL.get(), ModFluids.CRACKOIL.getSource(), 1)); // 73 (4701/61)
        add.accept(ItemFluidTank.make(ModItems.CANISTER_FULL.get(), ModFluids.CRACKOIL_DS.getSource(), 1)); // 74 (4701/135)
        add.accept(ItemFluidTank.make(ModItems.CANISTER_FULL.get(), ModFluids.COALOIL.getSource(), 1)); // 75 (4701/62)
        add.accept(ItemFluidTank.make(ModItems.CANISTER_FULL.get(), ModFluids.HEAVYOIL.getSource(), 1)); // 76 (4701/12)
        add.accept(ItemFluidTank.make(ModItems.CANISTER_FULL.get(), ModFluids.HEAVYOIL_VACUUM.getSource(), 1)); // 77 (4701/90)
        add.accept(ItemFluidTank.make(ModItems.CANISTER_FULL.get(), ModFluids.NAPHTHA.getSource(), 1)); // 78 (4701/19)
        add.accept(ItemFluidTank.make(ModItems.CANISTER_FULL.get(), ModFluids.NAPHTHA_DS.getSource(), 1)); // 79 (4701/137)
        add.accept(ItemFluidTank.make(ModItems.CANISTER_FULL.get(), ModFluids.NAPHTHA_CRACK.getSource(), 1)); // 80 (4701/64)
        // 81-90: Canisters (reformate .. reclaimed, 11-20 of 43)
        add.accept(ItemFluidTank.make(ModItems.CANISTER_FULL.get(), ModFluids.REFORMATE.getSource(), 1)); // 81 (4701/91)
        add.accept(ItemFluidTank.make(ModItems.CANISTER_FULL.get(), ModFluids.LIGHTOIL.getSource(), 1)); // 82 (4701/21)
        add.accept(ItemFluidTank.make(ModItems.CANISTER_FULL.get(), ModFluids.LIGHTOIL_DS.getSource(), 1)); // 83 (4701/138)
        add.accept(ItemFluidTank.make(ModItems.CANISTER_FULL.get(), ModFluids.LIGHTOIL_CRACK.getSource(), 1)); // 84 (4701/65)
        add.accept(ItemFluidTank.make(ModItems.CANISTER_FULL.get(), ModFluids.LIGHTOIL_VACUUM.getSource(), 1)); // 85 (4701/92)
        add.accept(ItemFluidTank.make(ModItems.CANISTER_FULL.get(), ModFluids.BITUMEN.getSource(), 1)); // 86 (4701/13)
        add.accept(ItemFluidTank.make(ModItems.CANISTER_FULL.get(), ModFluids.SMEAR.getSource(), 1)); // 87 (4701/14)
        add.accept(ItemFluidTank.make(ModItems.CANISTER_FULL.get(), ModFluids.HEATINGOIL.getSource(), 1)); // 88 (4701/15)
        add.accept(ItemFluidTank.make(ModItems.CANISTER_FULL.get(), ModFluids.HEATINGOIL_VACUUM.getSource(), 1)); // 89 (4701/95)
        add.accept(ItemFluidTank.make(ModItems.CANISTER_FULL.get(), ModFluids.RECLAIMED.getSource(), 1)); // 90 (4701/16)
        // 91-100: Canisters (lubricant .. petroil_leaded, 21-30 of 43)
        add.accept(ItemFluidTank.make(ModItems.CANISTER_FULL.get(), ModFluids.LUBRICANT.getSource(), 1)); // 91 (4701/18)
        add.accept(ItemFluidTank.make(ModItems.CANISTER_FULL.get(), ModFluids.XYLENE.getSource(), 1)); // 92 (4701/94)
        add.accept(ItemFluidTank.make(ModItems.CANISTER_FULL.get(), ModFluids.DIESEL.getSource(), 1)); // 93 (4701/20)
        add.accept(ItemFluidTank.make(ModItems.CANISTER_FULL.get(), ModFluids.DIESEL_REFORM.getSource(), 1)); // 94 (4701/96)
        add.accept(ItemFluidTank.make(ModItems.CANISTER_FULL.get(), ModFluids.DIESEL_CRACK.getSource(), 1)); // 95 (4701/66)
        add.accept(ItemFluidTank.make(ModItems.CANISTER_FULL.get(), ModFluids.DIESEL_CRACK_REFORM.getSource(), 1)); // 96 (4701/97)
        add.accept(ItemFluidTank.make(ModItems.CANISTER_FULL.get(), ModFluids.KEROSENE.getSource(), 1)); // 97 (4701/22)
        add.accept(ItemFluidTank.make(ModItems.CANISTER_FULL.get(), ModFluids.KEROSENE_REFORM.getSource(), 1)); // 98 (4701/98)
        add.accept(ItemFluidTank.make(ModItems.CANISTER_FULL.get(), ModFluids.PETROIL.getSource(), 1)); // 99 (4701/17)
        add.accept(ItemFluidTank.make(ModItems.CANISTER_FULL.get(), ModFluids.PETROIL_LEADED.getSource(), 1)); // 100 (4701/72)

        // --- CHUNK 2: Items 101-200 (worker_m5_control_c2) ---
        // 101-110: Canisters (Gasoline .. Seeding Slurry, 31-40 of 43)
        add.accept(ItemFluidTank.make(ModItems.CANISTER_FULL.get(), ModFluids.GASOLINE.getSource(), 1)); // 101 (4701/46)
        add.accept(ItemFluidTank.make(ModItems.CANISTER_FULL.get(), ModFluids.GASOLINE_LEADED.getSource(), 1)); // 102 (4701/73)
        add.accept(ItemFluidTank.make(ModItems.CANISTER_FULL.get(), ModFluids.COALGAS.getSource(), 1)); // 103 (4701/47)
        add.accept(ItemFluidTank.make(ModItems.CANISTER_FULL.get(), ModFluids.COALGAS_LEADED.getSource(), 1)); // 104 (4701/74)
        add.accept(ItemFluidTank.make(ModItems.CANISTER_FULL.get(), ModFluids.COALCREOSOTE.getSource(), 1)); // 105 (4701/80)
        add.accept(ItemFluidTank.make(ModItems.CANISTER_FULL.get(), ModFluids.WOODOIL.getSource(), 1)); // 106 (4701/79)
        add.accept(ItemFluidTank.make(ModItems.CANISTER_FULL.get(), ModFluids.BIOFUEL.getSource(), 1)); // 107 (4701/27)
        add.accept(ItemFluidTank.make(ModItems.CANISTER_FULL.get(), ModFluids.ETHANOL.getSource(), 1)); // 108 (4701/59)
        add.accept(ItemFluidTank.make(ModItems.CANISTER_FULL.get(), ModFluids.NITAN.getSource(), 1)); // 109 (4701/28)
        add.accept(ItemFluidTank.make(ModItems.CANISTER_FULL.get(), ModFluids.SEEDSLURRY.getSource(), 1)); // 110 (4701/81)
        // 111-120: Canisters (111-113), Napalm B (114), Empty Gas Tank (115), Gas Tanks (Hydrogen .. Helium-4, 116-120)
        add.accept(ItemFluidTank.make(ModItems.CANISTER_FULL.get(), ModFluids.SOLVENT.getSource(), 1)); // 111 (4701/83)
        add.accept(ItemFluidTank.make(ModItems.CANISTER_FULL.get(), ModFluids.TITANIUM_TETRACHLORIDE.getSource(), 1)); // 112 (4701/156)
        add.accept(ItemFluidTank.make(ModItems.CANISTER_FULL.get(), ModFluids.FRACKSOL.getSource(), 1)); // 113 (4701/49)
        add.accept(new ItemStack(ModItems.CANISTER_NAPALM.get()));                                // 114 (4702/0)
        add.accept(new ItemStack(ModItems.GAS_EMPTY.get()));                                      // 115 (4703/0)
        add.accept(ItemFluidTank.make(ModItems.GAS_FULL.get(), ModFluids.HYDROGEN.getSource(), 1)); // 116 (4704/38)
        add.accept(ItemFluidTank.make(ModItems.GAS_FULL.get(), ModFluids.DEUTERIUM.getSource(), 1)); // 117 (4704/8)
        add.accept(ItemFluidTank.make(ModItems.GAS_FULL.get(), ModFluids.TRITIUM.getSource(), 1)); // 118 (4704/9)
        add.accept(ItemFluidTank.make(ModItems.GAS_FULL.get(), ModFluids.HELIUM3.getSource(), 1)); // 119 (4704/57)
        add.accept(ItemFluidTank.make(ModItems.GAS_FULL.get(), ModFluids.HELIUM4.getSource(), 1)); // 120 (4704/123)
        // 121-130: Gas Tanks (Liquid Oxygen .. Unsaturated Hydrocarbons, 121-130)
        add.accept(ItemFluidTank.make(ModItems.GAS_FULL.get(), ModFluids.OXYGEN.getSource(), 1)); // 121 (4704/39)
        add.accept(ItemFluidTank.make(ModItems.GAS_FULL.get(), ModFluids.XENON.getSource(), 1)); // 122 (4704/40)
        add.accept(ItemFluidTank.make(ModItems.GAS_FULL.get(), ModFluids.CHLORINE.getSource(), 1)); // 123 (4704/89)
        add.accept(ItemFluidTank.make(ModItems.GAS_FULL.get(), ModFluids.FLUE.getSource(), 1)); // 124 (4704/155)
        add.accept(ItemFluidTank.make(ModItems.GAS_FULL.get(), ModFluids.GAS.getSource(), 1)); // 125 (4704/23)
        add.accept(ItemFluidTank.make(ModItems.GAS_FULL.get(), ModFluids.PETROLEUM.getSource(), 1)); // 126 (4704/24)
        add.accept(ItemFluidTank.make(ModItems.GAS_FULL.get(), ModFluids.SOURGAS.getSource(), 1)); // 127 (4704/93)
        add.accept(ItemFluidTank.make(ModItems.GAS_FULL.get(), ModFluids.SYNGAS.getSource(), 1)); // 128 (4704/86)
        add.accept(ItemFluidTank.make(ModItems.GAS_FULL.get(), ModFluids.AROMATICS.getSource(), 1)); // 129 (4704/67)
        add.accept(ItemFluidTank.make(ModItems.GAS_FULL.get(), ModFluids.UNSATURATEDS.getSource(), 1)); // 130 (4704/68)
        // 131-140: Gas Tanks (131-134), Empty Universal Fluid Tank (135), Universal Fluid Tanks (Compressed Air .. Hot Heavy Water, 136-140)
        add.accept(ItemFluidTank.make(ModItems.GAS_FULL.get(), ModFluids.REFORMGAS.getSource(), 1)); // 131 (4704/99)
        add.accept(ItemFluidTank.make(ModItems.GAS_FULL.get(), ModFluids.BIOGAS.getSource(), 1)); // 132 (4704/26)
        add.accept(ItemFluidTank.make(ModItems.GAS_FULL.get(), ModFluids.PHOSGENE.getSource(), 1)); // 133 (4704/101)
        add.accept(ItemFluidTank.make(ModItems.GAS_FULL.get(), ModFluids.MUSTARDGAS.getSource(), 1)); // 134 (4704/102)
        add.accept(new ItemStack(ModItems.FLUID_TANK_EMPTY.get()));                               // 135 (4705/0)
        add.accept(ItemFluidTank.make(ModItems.FLUID_TANK_FULL.get(), ModFluids.AIR.getSource(), 1)); // 136 (4706/151)
        add.accept(ItemFluidTank.make(ModItems.FLUID_TANK_FULL.get(), ModFluids.AIRBLAST.getSource(), 1)); // 137 (4706/154)
        add.accept(ItemFluidTank.make(ModItems.FLUID_TANK_FULL.get(), ModFluids.WATER.getSource(), 1)); // 138 (4706/1)
        add.accept(ItemFluidTank.make(ModItems.FLUID_TANK_FULL.get(), ModFluids.HEAVYWATER.getSource(), 1)); // 139 (4706/60)
        add.accept(ItemFluidTank.make(ModItems.FLUID_TANK_FULL.get(), ModFluids.HEAVYWATER_HOT.getSource(), 1)); // 140 (4706/124)
        // 141-150: Universal Fluid Tanks (Lava .. Cold Perfluoromethyl, 141-150)
        add.accept(ItemFluidTank.make(ModItems.FLUID_TANK_FULL.get(), ModFluids.LAVA.getSource(), 1)); // 141 (4706/7)
        add.accept(ItemFluidTank.make(ModItems.FLUID_TANK_FULL.get(), ModFluids.STEAM.getSource(), 1)); // 142 (4706/2)
        add.accept(ItemFluidTank.make(ModItems.FLUID_TANK_FULL.get(), ModFluids.HOTSTEAM.getSource(), 1)); // 143 (4706/3)
        add.accept(ItemFluidTank.make(ModItems.FLUID_TANK_FULL.get(), ModFluids.SUPERHOTSTEAM.getSource(), 1)); // 144 (4706/4)
        add.accept(ItemFluidTank.make(ModItems.FLUID_TANK_FULL.get(), ModFluids.ULTRAHOTSTEAM.getSource(), 1)); // 145 (4706/5)
        add.accept(ItemFluidTank.make(ModItems.FLUID_TANK_FULL.get(), ModFluids.CARBONDIOXIDE.getSource(), 1)); // 146 (4706/55)
        add.accept(ItemFluidTank.make(ModItems.FLUID_TANK_FULL.get(), ModFluids.COOLANT.getSource(), 1)); // 147 (4706/6)
        add.accept(ItemFluidTank.make(ModItems.FLUID_TANK_FULL.get(), ModFluids.COOLANT_HOT.getSource(), 1)); // 148 (4706/76)
        add.accept(ItemFluidTank.make(ModItems.FLUID_TANK_FULL.get(), ModFluids.PERFLUOROMETHYL.getSource(), 1)); // 149 (4706/144)
        add.accept(ItemFluidTank.make(ModItems.FLUID_TANK_FULL.get(), ModFluids.PERFLUOROMETHYL_COLD.getSource(), 1)); // 150 (4706/145)
        // 151-160: Universal Fluid Tanks (Hot Perfluoromethyl .. Hot Liquid Lead, 151-160)
        add.accept(ItemFluidTank.make(ModItems.FLUID_TANK_FULL.get(), ModFluids.PERFLUOROMETHYL_HOT.getSource(), 1)); // 151 (4706/146)
        add.accept(ItemFluidTank.make(ModItems.FLUID_TANK_FULL.get(), ModFluids.CRYOGEL.getSource(), 1)); // 152 (4706/37)
        add.accept(ItemFluidTank.make(ModItems.FLUID_TANK_FULL.get(), ModFluids.MUG.getSource(), 1)); // 153 (4706/77)
        add.accept(ItemFluidTank.make(ModItems.FLUID_TANK_FULL.get(), ModFluids.MUG_HOT.getSource(), 1)); // 154 (4706/78)
        add.accept(ItemFluidTank.make(ModItems.FLUID_TANK_FULL.get(), ModFluids.BLOOD.getSource(), 1)); // 155 (4706/84)
        add.accept(ItemFluidTank.make(ModItems.FLUID_TANK_FULL.get(), ModFluids.BLOOD_HOT.getSource(), 1)); // 156 (4706/85)
        add.accept(ItemFluidTank.make(ModItems.FLUID_TANK_FULL.get(), ModFluids.SODIUM.getSource(), 1)); // 157 (4706/125)
        add.accept(ItemFluidTank.make(ModItems.FLUID_TANK_FULL.get(), ModFluids.SODIUM_HOT.getSource(), 1)); // 158 (4706/126)
        add.accept(ItemFluidTank.make(ModItems.FLUID_TANK_FULL.get(), ModFluids.LEAD.getSource(), 1)); // 159 (4706/142)
        add.accept(ItemFluidTank.make(ModItems.FLUID_TANK_FULL.get(), ModFluids.LEAD_HOT.getSource(), 1)); // 160 (4706/143)
        // 161-170: Universal Fluid Tanks (Liquid Thorium Salt .. Xenon Gas, 161-170)
        add.accept(ItemFluidTank.make(ModItems.FLUID_TANK_FULL.get(), ModFluids.THORIUM_SALT.getSource(), 1)); // 161 (4706/127)
        add.accept(ItemFluidTank.make(ModItems.FLUID_TANK_FULL.get(), ModFluids.THORIUM_SALT_HOT.getSource(), 1)); // 162 (4706/128)
        add.accept(ItemFluidTank.make(ModItems.FLUID_TANK_FULL.get(), ModFluids.THORIUM_SALT_DEPLETED.getSource(), 1)); // 163 (4706/129)
        add.accept(ItemFluidTank.make(ModItems.FLUID_TANK_FULL.get(), ModFluids.HYDROGEN.getSource(), 1)); // 164 (4706/38)
        add.accept(ItemFluidTank.make(ModItems.FLUID_TANK_FULL.get(), ModFluids.DEUTERIUM.getSource(), 1)); // 165 (4706/8)
        add.accept(ItemFluidTank.make(ModItems.FLUID_TANK_FULL.get(), ModFluids.TRITIUM.getSource(), 1)); // 166 (4706/9)
        add.accept(ItemFluidTank.make(ModItems.FLUID_TANK_FULL.get(), ModFluids.HELIUM3.getSource(), 1)); // 167 (4706/57)
        add.accept(ItemFluidTank.make(ModItems.FLUID_TANK_FULL.get(), ModFluids.HELIUM4.getSource(), 1)); // 168 (4706/123)
        add.accept(ItemFluidTank.make(ModItems.FLUID_TANK_FULL.get(), ModFluids.OXYGEN.getSource(), 1)); // 169 (4706/39)
        add.accept(ItemFluidTank.make(ModItems.FLUID_TANK_FULL.get(), ModFluids.XENON.getSource(), 1)); // 170 (4706/40)
        // 171-180: Universal Fluid Tanks (Chlorine Gas .. Desulfurized Hot Crude Oil, 171-180)
        add.accept(ItemFluidTank.make(ModItems.FLUID_TANK_FULL.get(), ModFluids.CHLORINE.getSource(), 1)); // 171 (4706/89)
        add.accept(ItemFluidTank.make(ModItems.FLUID_TANK_FULL.get(), ModFluids.MERCURY.getSource(), 1)); // 172 (4706/42)
        add.accept(ItemFluidTank.make(ModItems.FLUID_TANK_FULL.get(), ModFluids.CRUDE_OIL.getSource(), 1)); // 173 (4706/10)
        add.accept(ItemFluidTank.make(ModItems.FLUID_TANK_FULL.get(), ModFluids.OIL_DS.getSource(), 1)); // 174 (4706/133)
        add.accept(ItemFluidTank.make(ModItems.FLUID_TANK_FULL.get(), ModFluids.CRACKOIL.getSource(), 1)); // 175 (4706/61)
        add.accept(ItemFluidTank.make(ModItems.FLUID_TANK_FULL.get(), ModFluids.CRACKOIL_DS.getSource(), 1)); // 176 (4706/135)
        add.accept(ItemFluidTank.make(ModItems.FLUID_TANK_FULL.get(), ModFluids.COALOIL.getSource(), 1)); // 177 (4706/62)
        add.accept(ItemFluidTank.make(ModItems.FLUID_TANK_FULL.get(), ModFluids.OIL_COKER.getSource(), 1)); // 178 (4706/104)
        add.accept(ItemFluidTank.make(ModItems.FLUID_TANK_FULL.get(), ModFluids.HOTOIL.getSource(), 1)); // 179 (4706/11)
        add.accept(ItemFluidTank.make(ModItems.FLUID_TANK_FULL.get(), ModFluids.HOTOIL_DS.getSource(), 1)); // 180 (4706/134)
        // 181-190: Universal Fluid Tanks (Hot Cracked Oil .. Light Oil, 181-190)
        add.accept(ItemFluidTank.make(ModItems.FLUID_TANK_FULL.get(), ModFluids.HOTCRACKOIL.getSource(), 1)); // 181 (4706/63)
        add.accept(ItemFluidTank.make(ModItems.FLUID_TANK_FULL.get(), ModFluids.HOTCRACKOIL_DS.getSource(), 1)); // 182 (4706/136)
        add.accept(ItemFluidTank.make(ModItems.FLUID_TANK_FULL.get(), ModFluids.HEAVYOIL.getSource(), 1)); // 183 (4706/12)
        add.accept(ItemFluidTank.make(ModItems.FLUID_TANK_FULL.get(), ModFluids.HEAVYOIL_VACUUM.getSource(), 1)); // 184 (4706/90)
        add.accept(ItemFluidTank.make(ModItems.FLUID_TANK_FULL.get(), ModFluids.NAPHTHA.getSource(), 1)); // 185 (4706/19)
        add.accept(ItemFluidTank.make(ModItems.FLUID_TANK_FULL.get(), ModFluids.NAPHTHA_DS.getSource(), 1)); // 186 (4706/137)
        add.accept(ItemFluidTank.make(ModItems.FLUID_TANK_FULL.get(), ModFluids.NAPHTHA_CRACK.getSource(), 1)); // 187 (4706/64)
        add.accept(ItemFluidTank.make(ModItems.FLUID_TANK_FULL.get(), ModFluids.NAPHTHA_COKER.getSource(), 1)); // 188 (4706/105)
        add.accept(ItemFluidTank.make(ModItems.FLUID_TANK_FULL.get(), ModFluids.REFORMATE.getSource(), 1)); // 189 (4706/91)
        add.accept(ItemFluidTank.make(ModItems.FLUID_TANK_FULL.get(), ModFluids.LIGHTOIL.getSource(), 1)); // 190 (4706/21)
        // 191-200: Universal Fluid Tanks (Desulfurized Light Oil .. Flue Gas, 191-200)
        add.accept(ItemFluidTank.make(ModItems.FLUID_TANK_FULL.get(), ModFluids.LIGHTOIL_DS.getSource(), 1)); // 191 (4706/138)
        add.accept(ItemFluidTank.make(ModItems.FLUID_TANK_FULL.get(), ModFluids.LIGHTOIL_CRACK.getSource(), 1)); // 192 (4706/65)
        add.accept(ItemFluidTank.make(ModItems.FLUID_TANK_FULL.get(), ModFluids.LIGHTOIL_VACUUM.getSource(), 1)); // 193 (4706/92)
        add.accept(ItemFluidTank.make(ModItems.FLUID_TANK_FULL.get(), ModFluids.BITUMEN.getSource(), 1)); // 194 (4706/13)
        add.accept(ItemFluidTank.make(ModItems.FLUID_TANK_FULL.get(), ModFluids.SMEAR.getSource(), 1)); // 195 (4706/14)
        add.accept(ItemFluidTank.make(ModItems.FLUID_TANK_FULL.get(), ModFluids.HEATINGOIL.getSource(), 1)); // 196 (4706/15)
        add.accept(ItemFluidTank.make(ModItems.FLUID_TANK_FULL.get(), ModFluids.HEATINGOIL_VACUUM.getSource(), 1)); // 197 (4706/95)
        add.accept(ItemFluidTank.make(ModItems.FLUID_TANK_FULL.get(), ModFluids.RECLAIMED.getSource(), 1)); // 198 (4706/16)
        add.accept(ItemFluidTank.make(ModItems.FLUID_TANK_FULL.get(), ModFluids.LUBRICANT.getSource(), 1)); // 199 (4706/18)
        add.accept(ItemFluidTank.make(ModItems.FLUID_TANK_FULL.get(), ModFluids.FLUE.getSource(), 1)); // 200 (4706/155)

        // --- CHUNK 3: Items 201-300 (worker_m5_control_c3) ---
        // Batch 21: Items 201-210
        add.accept(ItemFluidTank.make(ModItems.FLUID_TANK_FULL.get(), ModFluids.GAS.getSource(), 1)); // 201 (4706/23)
        add.accept(ItemFluidTank.make(ModItems.FLUID_TANK_FULL.get(), ModFluids.GAS_COKER.getSource(), 1)); // 202 (4706/106)
        add.accept(ItemFluidTank.make(ModItems.FLUID_TANK_FULL.get(), ModFluids.PETROLEUM.getSource(), 1)); // 203 (4706/24)
        add.accept(ItemFluidTank.make(ModItems.FLUID_TANK_FULL.get(), ModFluids.SOURGAS.getSource(), 1)); // 204 (4706/93)
        add.accept(ItemFluidTank.make(ModItems.FLUID_TANK_FULL.get(), ModFluids.LPG.getSource(), 1)); // 205 (4706/25)
        add.accept(ItemFluidTank.make(ModItems.FLUID_TANK_FULL.get(), ModFluids.SYNGAS.getSource(), 1)); // 206 (4706/86)
        add.accept(ItemFluidTank.make(ModItems.FLUID_TANK_FULL.get(), ModFluids.OXYHYDROGEN.getSource(), 1)); // 207 (4706/87)
        add.accept(ItemFluidTank.make(ModItems.FLUID_TANK_FULL.get(), ModFluids.AROMATICS.getSource(), 1)); // 208 (4706/67)
        add.accept(ItemFluidTank.make(ModItems.FLUID_TANK_FULL.get(), ModFluids.UNSATURATEDS.getSource(), 1)); // 209 (4706/68)
        add.accept(ItemFluidTank.make(ModItems.FLUID_TANK_FULL.get(), ModFluids.XYLENE.getSource(), 1)); // 210 (4706/94)
        // Batch 22: Items 211-220
        add.accept(ItemFluidTank.make(ModItems.FLUID_TANK_FULL.get(), ModFluids.REFORMGAS.getSource(), 1)); // 211 (4706/99)
        add.accept(ItemFluidTank.make(ModItems.FLUID_TANK_FULL.get(), ModFluids.DIESEL.getSource(), 1)); // 212 (4706/20)
        add.accept(ItemFluidTank.make(ModItems.FLUID_TANK_FULL.get(), ModFluids.DIESEL_REFORM.getSource(), 1)); // 213 (4706/96)
        add.accept(ItemFluidTank.make(ModItems.FLUID_TANK_FULL.get(), ModFluids.DIESEL_CRACK.getSource(), 1)); // 214 (4706/66)
        add.accept(ItemFluidTank.make(ModItems.FLUID_TANK_FULL.get(), ModFluids.DIESEL_CRACK_REFORM.getSource(), 1)); // 215 (4706/97)
        add.accept(ItemFluidTank.make(ModItems.FLUID_TANK_FULL.get(), ModFluids.KEROSENE.getSource(), 1)); // 216 (4706/22)
        add.accept(ItemFluidTank.make(ModItems.FLUID_TANK_FULL.get(), ModFluids.KEROSENE_REFORM.getSource(), 1)); // 217 (4706/98)
        add.accept(ItemFluidTank.make(ModItems.FLUID_TANK_FULL.get(), ModFluids.PETROIL.getSource(), 1)); // 218 (4706/17)
        add.accept(ItemFluidTank.make(ModItems.FLUID_TANK_FULL.get(), ModFluids.PETROIL_LEADED.getSource(), 1)); // 219 (4706/72)
        add.accept(ItemFluidTank.make(ModItems.FLUID_TANK_FULL.get(), ModFluids.GASOLINE.getSource(), 1)); // 220 (4706/46)
        // Batch 23: Items 221-230
        add.accept(ItemFluidTank.make(ModItems.FLUID_TANK_FULL.get(), ModFluids.GASOLINE_LEADED.getSource(), 1)); // 221 (4706/73)
        add.accept(ItemFluidTank.make(ModItems.FLUID_TANK_FULL.get(), ModFluids.COALGAS.getSource(), 1)); // 222 (4706/47)
        add.accept(ItemFluidTank.make(ModItems.FLUID_TANK_FULL.get(), ModFluids.COALGAS_LEADED.getSource(), 1)); // 223 (4706/74)
        add.accept(ItemFluidTank.make(ModItems.FLUID_TANK_FULL.get(), ModFluids.COALCREOSOTE.getSource(), 1)); // 224 (4706/80)
        add.accept(ItemFluidTank.make(ModItems.FLUID_TANK_FULL.get(), ModFluids.WOODOIL.getSource(), 1)); // 225 (4706/79)
        add.accept(ItemFluidTank.make(ModItems.FLUID_TANK_FULL.get(), ModFluids.BIOGAS.getSource(), 1)); // 226 (4706/26)
        add.accept(ItemFluidTank.make(ModItems.FLUID_TANK_FULL.get(), ModFluids.BIOFUEL.getSource(), 1)); // 227 (4706/27)
        add.accept(ItemFluidTank.make(ModItems.FLUID_TANK_FULL.get(), ModFluids.ETHANOL.getSource(), 1)); // 228 (4706/59)
        add.accept(ItemFluidTank.make(ModItems.FLUID_TANK_FULL.get(), ModFluids.FISHOIL.getSource(), 1)); // 229 (4706/110)
        add.accept(ItemFluidTank.make(ModItems.FLUID_TANK_FULL.get(), ModFluids.SUNFLOWEROIL.getSource(), 1)); // 230 (4706/111)
        // Batch 24: Items 231-240
        add.accept(ItemFluidTank.make(ModItems.FLUID_TANK_FULL.get(), ModFluids.NITAN.getSource(), 1)); // 231 (4706/28)
        add.accept(ItemFluidTank.make(ModItems.FLUID_TANK_FULL.get(), ModFluids.DHC.getSource(), 1)); // 232 (4706/153)
        add.accept(ItemFluidTank.make(ModItems.FLUID_TANK_FULL.get(), ModFluids.BALEFIRE.getSource(), 1)); // 233 (4706/41)
        add.accept(ItemFluidTank.make(ModItems.FLUID_TANK_FULL.get(), ModFluids.SALIENT.getSource(), 1)); // 234 (4706/69)
        // Joke: REN-TV concentrate filled containers (no 1.7.10 analog)
        add.accept(ItemFluidTank.make(ModItems.FLUID_TANK_FULL.get(), ModFluids.UNSTABLE_SANITY.getSource(), 1));
        add.accept(ItemFluidTank.make(ModItems.FLUID_TANK_FULL.get(), ModFluids.SEEDSLURRY.getSource(), 1)); // 235 (4706/81)
        add.accept(ItemFluidTank.make(ModItems.FLUID_TANK_FULL.get(), ModFluids.COLLOID.getSource(), 1)); // 236 (4706/100)
        add.accept(ItemFluidTank.make(ModItems.FLUID_TANK_FULL.get(), ModFluids.VITRIOL.getSource(), 1)); // 237 (4706/140)
        add.accept(ItemFluidTank.make(ModItems.FLUID_TANK_FULL.get(), ModFluids.SLOP.getSource(), 1)); // 238 (4706/141)
        add.accept(ItemFluidTank.make(ModItems.FLUID_TANK_FULL.get(), ModFluids.IONGEL.getSource(), 1)); // 239 (4706/103)
        add.accept(ItemFluidTank.make(ModItems.FLUID_TANK_FULL.get(), ModFluids.PEROXIDE.getSource(), 1)); // 240 (4706/35)
        // Batch 25: Items 241-250
        add.accept(ItemFluidTank.make(ModItems.FLUID_TANK_FULL.get(), ModFluids.SULFURIC_ACID.getSource(), 1)); // 241 (4706/75)
        add.accept(ItemFluidTank.make(ModItems.FLUID_TANK_FULL.get(), ModFluids.NITRIC_ACID.getSource(), 1)); // 242 (4706/82)
        add.accept(ItemFluidTank.make(ModItems.FLUID_TANK_FULL.get(), ModFluids.SOLVENT.getSource(), 1)); // 243 (4706/83)
        add.accept(ItemFluidTank.make(ModItems.FLUID_TANK_FULL.get(), ModFluids.RADIOSOLVENT.getSource(), 1)); // 244 (4706/88)
        add.accept(ItemFluidTank.make(ModItems.FLUID_TANK_FULL.get(), ModFluids.SCHRABIDIC.getSource(), 1)); // 245 (4706/32)
        add.accept(ItemFluidTank.make(ModItems.FLUID_TANK_FULL.get(), ModFluids.UF6.getSource(), 1)); // 246 (4706/29)
        add.accept(ItemFluidTank.make(ModItems.FLUID_TANK_FULL.get(), ModFluids.PUF6.getSource(), 1)); // 247 (4706/30)
        add.accept(ItemFluidTank.make(ModItems.FLUID_TANK_FULL.get(), ModFluids.SAS3.getSource(), 1)); // 248 (4706/31)
        add.accept(ItemFluidTank.make(ModItems.FLUID_TANK_FULL.get(), ModFluids.PAIN.getSource(), 1)); // 249 (4706/43)
        add.accept(ItemFluidTank.make(ModItems.FLUID_TANK_FULL.get(), ModFluids.WATZ.getSource(), 1)); // 250 (4706/36)
        // Batch 26: Items 251-260
        add.accept(ItemFluidTank.make(ModItems.FLUID_TANK_FULL.get(), ModFluids.FULLERENE.getSource(), 1)); // 251 (4706/130)
        add.accept(ItemFluidTank.make(ModItems.FLUID_TANK_FULL.get(), ModFluids.EGG.getSource(), 1)); // 252 (4706/107)
        add.accept(ItemFluidTank.make(ModItems.FLUID_TANK_FULL.get(), ModFluids.CHOLESTEROL.getSource(), 1)); // 253 (4706/108)
        add.accept(ItemFluidTank.make(ModItems.FLUID_TANK_FULL.get(), ModFluids.SODIUM_ALUMINATE.getSource(), 1)); // 254 (4706/148)
        add.accept(ItemFluidTank.make(ModItems.FLUID_TANK_FULL.get(), ModFluids.BAUXITE_SOLUTION.getSource(), 1)); // 255 (4706/149)
        add.accept(ItemFluidTank.make(ModItems.FLUID_TANK_FULL.get(), ModFluids.ALUMINA.getSource(), 1)); // 256 (4706/150)
        add.accept(ItemFluidTank.make(ModItems.FLUID_TANK_FULL.get(), ModFluids.TITANIUM_TETRACHLORIDE.getSource(), 1)); // 257 (4706/156)
        add.accept(ItemFluidTank.make(ModItems.FLUID_TANK_FULL.get(), ModFluids.CONCRETE.getSource(), 1)); // 258 (4706/152)
        add.accept(ItemFluidTank.make(ModItems.FLUID_TANK_FULL.get(), ModFluids.FRACKSOL.getSource(), 1)); // 259 (4706/49)
        add.accept(ItemFluidTank.make(ModItems.FLUID_TANK_FULL.get(), ModFluids.LYE.getSource(), 1)); // 260 (4706/147)
        // Batch 27: Items 261-270
        add.accept(ItemFluidTank.make(ModItems.FLUID_TANK_FULL.get(), ModFluids.PHOSGENE.getSource(), 1)); // 261 (4706/101)
        add.accept(ItemFluidTank.make(ModItems.FLUID_TANK_FULL.get(), ModFluids.MUSTARDGAS.getSource(), 1)); // 262 (4706/102)
        add.accept(ItemFluidTank.make(ModItems.FLUID_TANK_FULL.get(), ModFluids.ESTRADIOL.getSource(), 1)); // 263 (4706/109)
        add.accept(ItemFluidTank.make(ModItems.FLUID_TANK_FULL.get(), ModFluids.NITROGLYCERIN.getSource(), 1)); // 264 (4706/112)
        add.accept(ItemFluidTank.make(ModItems.FLUID_TANK_FULL.get(), ModFluids.AMAT.getSource(), 1)); // 265 (4706/33)
        add.accept(ItemFluidTank.make(ModItems.FLUID_TANK_FULL.get(), ModFluids.ASCHRAB.getSource(), 1)); // 266 (4706/34)
        add.accept(ItemFluidTank.make(ModItems.FLUID_TANK_FULL.get(), ModFluids.XPJUICE.getSource(), 1)); // 267 (4706/70)
        add.accept(ItemFluidTank.make(ModItems.FLUID_TANK_FULL.get(), ModFluids.ENDERJUICE.getSource(), 1)); // 268 (4706/71)
        add.accept(ItemFluidTank.make(ModItems.FLUID_TANK_FULL.get(), ModFluids.STELLAR_FLUX.getSource(), 1)); // 269 (4706/139)
        add.accept(ItemFluidTank.make(ModItems.FLUID_TANK_FULL.get(), ModFluids.PHEROMONE.getSource(), 1)); // 270 (4706/131)
        // Batch 28: Items 271-280
        add.accept(ItemFluidTank.make(ModItems.FLUID_TANK_FULL.get(), ModFluids.PHEROMONE_M.getSource(), 1)); // 271 (4706/132)
        add.accept(ItemFluidTank.make(ModItems.FLUID_TANK_FULL.get(), ModFluids.CUSTOM_DEMO.getSource(), 1)); // 272 (4706/1000)
        add.accept(new ItemStack(ModItems.FLUID_TANK_LEAD_EMPTY.get())); // 273 (4707/0)
        add.accept(ItemFluidTank.make(ModItems.FLUID_TANK_LEAD_FULL.get(), ModFluids.AIR.getSource(), 1)); // 274 (4708/151)
        add.accept(ItemFluidTank.make(ModItems.FLUID_TANK_LEAD_FULL.get(), ModFluids.AIRBLAST.getSource(), 1)); // 275 (4708/154)
        add.accept(ItemFluidTank.make(ModItems.FLUID_TANK_LEAD_FULL.get(), ModFluids.WATER.getSource(), 1)); // 276 (4708/1)
        add.accept(ItemFluidTank.make(ModItems.FLUID_TANK_LEAD_FULL.get(), ModFluids.HEAVYWATER.getSource(), 1)); // 277 (4708/60)
        add.accept(ItemFluidTank.make(ModItems.FLUID_TANK_LEAD_FULL.get(), ModFluids.HEAVYWATER_HOT.getSource(), 1)); // 278 (4708/124)
        add.accept(ItemFluidTank.make(ModItems.FLUID_TANK_LEAD_FULL.get(), ModFluids.LAVA.getSource(), 1)); // 279 (4708/7)
        add.accept(ItemFluidTank.make(ModItems.FLUID_TANK_LEAD_FULL.get(), ModFluids.STEAM.getSource(), 1)); // 280 (4708/2)
        // Batch 29: Items 281-290
        add.accept(ItemFluidTank.make(ModItems.FLUID_TANK_LEAD_FULL.get(), ModFluids.HOTSTEAM.getSource(), 1)); // 281 (4708/3)
        add.accept(ItemFluidTank.make(ModItems.FLUID_TANK_LEAD_FULL.get(), ModFluids.SUPERHOTSTEAM.getSource(), 1)); // 282 (4708/4)
        add.accept(ItemFluidTank.make(ModItems.FLUID_TANK_LEAD_FULL.get(), ModFluids.ULTRAHOTSTEAM.getSource(), 1)); // 283 (4708/5)
        add.accept(ItemFluidTank.make(ModItems.FLUID_TANK_LEAD_FULL.get(), ModFluids.CARBONDIOXIDE.getSource(), 1)); // 284 (4708/55)
        add.accept(ItemFluidTank.make(ModItems.FLUID_TANK_LEAD_FULL.get(), ModFluids.COOLANT.getSource(), 1)); // 285 (4708/6)
        add.accept(ItemFluidTank.make(ModItems.FLUID_TANK_LEAD_FULL.get(), ModFluids.COOLANT_HOT.getSource(), 1)); // 286 (4708/76)
        add.accept(ItemFluidTank.make(ModItems.FLUID_TANK_LEAD_FULL.get(), ModFluids.PERFLUOROMETHYL.getSource(), 1)); // 287 (4708/144)
        add.accept(ItemFluidTank.make(ModItems.FLUID_TANK_LEAD_FULL.get(), ModFluids.PERFLUOROMETHYL_COLD.getSource(), 1)); // 288 (4708/145)
        add.accept(ItemFluidTank.make(ModItems.FLUID_TANK_LEAD_FULL.get(), ModFluids.PERFLUOROMETHYL_HOT.getSource(), 1)); // 289 (4708/146)
        add.accept(ItemFluidTank.make(ModItems.FLUID_TANK_LEAD_FULL.get(), ModFluids.CRYOGEL.getSource(), 1)); // 290 (4708/37)
        // Batch 30: Items 291-300
        add.accept(ItemFluidTank.make(ModItems.FLUID_TANK_LEAD_FULL.get(), ModFluids.MUG.getSource(), 1)); // 291 (4708/77)
        add.accept(ItemFluidTank.make(ModItems.FLUID_TANK_LEAD_FULL.get(), ModFluids.MUG_HOT.getSource(), 1)); // 292 (4708/78)
        add.accept(ItemFluidTank.make(ModItems.FLUID_TANK_LEAD_FULL.get(), ModFluids.BLOOD.getSource(), 1)); // 293 (4708/84)
        add.accept(ItemFluidTank.make(ModItems.FLUID_TANK_LEAD_FULL.get(), ModFluids.BLOOD_HOT.getSource(), 1)); // 294 (4708/85)
        add.accept(ItemFluidTank.make(ModItems.FLUID_TANK_LEAD_FULL.get(), ModFluids.SODIUM.getSource(), 1)); // 295 (4708/125)
        add.accept(ItemFluidTank.make(ModItems.FLUID_TANK_LEAD_FULL.get(), ModFluids.SODIUM_HOT.getSource(), 1)); // 296 (4708/126)
        add.accept(ItemFluidTank.make(ModItems.FLUID_TANK_LEAD_FULL.get(), ModFluids.LEAD.getSource(), 1)); // 297 (4708/142)
        add.accept(ItemFluidTank.make(ModItems.FLUID_TANK_LEAD_FULL.get(), ModFluids.LEAD_HOT.getSource(), 1)); // 298 (4708/143)
        add.accept(ItemFluidTank.make(ModItems.FLUID_TANK_LEAD_FULL.get(), ModFluids.THORIUM_SALT.getSource(), 1)); // 299 (4708/127)
        add.accept(ItemFluidTank.make(ModItems.FLUID_TANK_LEAD_FULL.get(), ModFluids.THORIUM_SALT_HOT.getSource(), 1)); // 300 (4708/128)

        // --- CHUNK 4: Items 301-400 (worker_m5_control_c4) ---
        // Batch 31: Items 301-310
        add.accept(ItemFluidTank.make(ModItems.FLUID_TANK_LEAD_FULL.get(), ModFluids.THORIUM_SALT_DEPLETED.getSource(), 1)); // 301 (4708/129)
        add.accept(ItemFluidTank.make(ModItems.FLUID_TANK_LEAD_FULL.get(), ModFluids.HYDROGEN.getSource(), 1)); // 302 (4708/38)
        add.accept(ItemFluidTank.make(ModItems.FLUID_TANK_LEAD_FULL.get(), ModFluids.DEUTERIUM.getSource(), 1)); // 303 (4708/8)
        add.accept(ItemFluidTank.make(ModItems.FLUID_TANK_LEAD_FULL.get(), ModFluids.TRITIUM.getSource(), 1)); // 304 (4708/9)
        add.accept(ItemFluidTank.make(ModItems.FLUID_TANK_LEAD_FULL.get(), ModFluids.HELIUM3.getSource(), 1)); // 305 (4708/57)
        add.accept(ItemFluidTank.make(ModItems.FLUID_TANK_LEAD_FULL.get(), ModFluids.HELIUM4.getSource(), 1)); // 306 (4708/123)
        add.accept(ItemFluidTank.make(ModItems.FLUID_TANK_LEAD_FULL.get(), ModFluids.OXYGEN.getSource(), 1)); // 307 (4708/39)
        add.accept(ItemFluidTank.make(ModItems.FLUID_TANK_LEAD_FULL.get(), ModFluids.XENON.getSource(), 1)); // 308 (4708/40)
        add.accept(ItemFluidTank.make(ModItems.FLUID_TANK_LEAD_FULL.get(), ModFluids.CHLORINE.getSource(), 1)); // 309 (4708/89)
        add.accept(ItemFluidTank.make(ModItems.FLUID_TANK_LEAD_FULL.get(), ModFluids.MERCURY.getSource(), 1)); // 310 (4708/42)
        // Batch 32: Items 311-320
        add.accept(ItemFluidTank.make(ModItems.FLUID_TANK_LEAD_FULL.get(), ModFluids.CRUDE_OIL.getSource(), 1)); // 311 (4708/10)
        add.accept(ItemFluidTank.make(ModItems.FLUID_TANK_LEAD_FULL.get(), ModFluids.OIL_DS.getSource(), 1)); // 312 (4708/133)
        add.accept(ItemFluidTank.make(ModItems.FLUID_TANK_LEAD_FULL.get(), ModFluids.CRACKOIL.getSource(), 1)); // 313 (4708/61)
        add.accept(ItemFluidTank.make(ModItems.FLUID_TANK_LEAD_FULL.get(), ModFluids.CRACKOIL_DS.getSource(), 1)); // 314 (4708/135)
        add.accept(ItemFluidTank.make(ModItems.FLUID_TANK_LEAD_FULL.get(), ModFluids.COALOIL.getSource(), 1)); // 315 (4708/62)
        add.accept(ItemFluidTank.make(ModItems.FLUID_TANK_LEAD_FULL.get(), ModFluids.OIL_COKER.getSource(), 1)); // 316 (4708/104)
        add.accept(ItemFluidTank.make(ModItems.FLUID_TANK_LEAD_FULL.get(), ModFluids.HOTOIL.getSource(), 1)); // 317 (4708/11)
        add.accept(ItemFluidTank.make(ModItems.FLUID_TANK_LEAD_FULL.get(), ModFluids.HOTOIL_DS.getSource(), 1)); // 318 (4708/134)
        add.accept(ItemFluidTank.make(ModItems.FLUID_TANK_LEAD_FULL.get(), ModFluids.HOTCRACKOIL.getSource(), 1)); // 319 (4708/63)
        add.accept(ItemFluidTank.make(ModItems.FLUID_TANK_LEAD_FULL.get(), ModFluids.HOTCRACKOIL_DS.getSource(), 1)); // 320 (4708/136)
        // Batch 33: Items 321-330
        add.accept(ItemFluidTank.make(ModItems.FLUID_TANK_LEAD_FULL.get(), ModFluids.HEAVYOIL.getSource(), 1)); // 321 (4708/12)
        add.accept(ItemFluidTank.make(ModItems.FLUID_TANK_LEAD_FULL.get(), ModFluids.HEAVYOIL_VACUUM.getSource(), 1)); // 322 (4708/90)
        add.accept(ItemFluidTank.make(ModItems.FLUID_TANK_LEAD_FULL.get(), ModFluids.NAPHTHA.getSource(), 1)); // 323 (4708/19)
        add.accept(ItemFluidTank.make(ModItems.FLUID_TANK_LEAD_FULL.get(), ModFluids.NAPHTHA_DS.getSource(), 1)); // 324 (4708/137)
        add.accept(ItemFluidTank.make(ModItems.FLUID_TANK_LEAD_FULL.get(), ModFluids.NAPHTHA_CRACK.getSource(), 1)); // 325 (4708/64)
        add.accept(ItemFluidTank.make(ModItems.FLUID_TANK_LEAD_FULL.get(), ModFluids.NAPHTHA_COKER.getSource(), 1)); // 326 (4708/105)
        add.accept(ItemFluidTank.make(ModItems.FLUID_TANK_LEAD_FULL.get(), ModFluids.REFORMATE.getSource(), 1)); // 327 (4708/91)
        add.accept(ItemFluidTank.make(ModItems.FLUID_TANK_LEAD_FULL.get(), ModFluids.LIGHTOIL.getSource(), 1)); // 328 (4708/21)
        add.accept(ItemFluidTank.make(ModItems.FLUID_TANK_LEAD_FULL.get(), ModFluids.LIGHTOIL_DS.getSource(), 1)); // 329 (4708/138)
        add.accept(ItemFluidTank.make(ModItems.FLUID_TANK_LEAD_FULL.get(), ModFluids.LIGHTOIL_CRACK.getSource(), 1)); // 330 (4708/65)
        // Batch 34: Items 331-340
        add.accept(ItemFluidTank.make(ModItems.FLUID_TANK_LEAD_FULL.get(), ModFluids.LIGHTOIL_VACUUM.getSource(), 1)); // 331 (4708/92)
        add.accept(ItemFluidTank.make(ModItems.FLUID_TANK_LEAD_FULL.get(), ModFluids.BITUMEN.getSource(), 1)); // 332 (4708/13)
        add.accept(ItemFluidTank.make(ModItems.FLUID_TANK_LEAD_FULL.get(), ModFluids.SMEAR.getSource(), 1)); // 333 (4708/14)
        add.accept(ItemFluidTank.make(ModItems.FLUID_TANK_LEAD_FULL.get(), ModFluids.HEATINGOIL.getSource(), 1)); // 334 (4708/15)
        add.accept(ItemFluidTank.make(ModItems.FLUID_TANK_LEAD_FULL.get(), ModFluids.HEATINGOIL_VACUUM.getSource(), 1)); // 335 (4708/95)
        add.accept(ItemFluidTank.make(ModItems.FLUID_TANK_LEAD_FULL.get(), ModFluids.RECLAIMED.getSource(), 1)); // 336 (4708/16)
        add.accept(ItemFluidTank.make(ModItems.FLUID_TANK_LEAD_FULL.get(), ModFluids.LUBRICANT.getSource(), 1)); // 337 (4708/18)
        add.accept(ItemFluidTank.make(ModItems.FLUID_TANK_LEAD_FULL.get(), ModFluids.FLUE.getSource(), 1)); // 338 (4708/155)
        add.accept(ItemFluidTank.make(ModItems.FLUID_TANK_LEAD_FULL.get(), ModFluids.GAS.getSource(), 1)); // 339 (4708/23)
        add.accept(ItemFluidTank.make(ModItems.FLUID_TANK_LEAD_FULL.get(), ModFluids.GAS_COKER.getSource(), 1)); // 340 (4708/106)
        // Batch 35: Items 341-350
        add.accept(ItemFluidTank.make(ModItems.FLUID_TANK_LEAD_FULL.get(), ModFluids.PETROLEUM.getSource(), 1)); // 341 (4708/24)
        add.accept(ItemFluidTank.make(ModItems.FLUID_TANK_LEAD_FULL.get(), ModFluids.SOURGAS.getSource(), 1)); // 342 (4708/93)
        add.accept(ItemFluidTank.make(ModItems.FLUID_TANK_LEAD_FULL.get(), ModFluids.LPG.getSource(), 1)); // 343 (4708/25)
        add.accept(ItemFluidTank.make(ModItems.FLUID_TANK_LEAD_FULL.get(), ModFluids.SYNGAS.getSource(), 1)); // 344 (4708/86)
        add.accept(ItemFluidTank.make(ModItems.FLUID_TANK_LEAD_FULL.get(), ModFluids.OXYHYDROGEN.getSource(), 1)); // 345 (4708/87)
        add.accept(ItemFluidTank.make(ModItems.FLUID_TANK_LEAD_FULL.get(), ModFluids.AROMATICS.getSource(), 1)); // 346 (4708/67)
        add.accept(ItemFluidTank.make(ModItems.FLUID_TANK_LEAD_FULL.get(), ModFluids.UNSATURATEDS.getSource(), 1)); // 347 (4708/68)
        add.accept(ItemFluidTank.make(ModItems.FLUID_TANK_LEAD_FULL.get(), ModFluids.XYLENE.getSource(), 1)); // 348 (4708/94)
        add.accept(ItemFluidTank.make(ModItems.FLUID_TANK_LEAD_FULL.get(), ModFluids.REFORMGAS.getSource(), 1)); // 349 (4708/99)
        add.accept(ItemFluidTank.make(ModItems.FLUID_TANK_LEAD_FULL.get(), ModFluids.DIESEL.getSource(), 1)); // 350 (4708/20)
        // Batch 36: Items 351-360
        add.accept(ItemFluidTank.make(ModItems.FLUID_TANK_LEAD_FULL.get(), ModFluids.DIESEL_REFORM.getSource(), 1)); // 351 (4708/96)
        add.accept(ItemFluidTank.make(ModItems.FLUID_TANK_LEAD_FULL.get(), ModFluids.DIESEL_CRACK.getSource(), 1)); // 352 (4708/66)
        add.accept(ItemFluidTank.make(ModItems.FLUID_TANK_LEAD_FULL.get(), ModFluids.DIESEL_CRACK_REFORM.getSource(), 1)); // 353 (4708/97)
        add.accept(ItemFluidTank.make(ModItems.FLUID_TANK_LEAD_FULL.get(), ModFluids.KEROSENE.getSource(), 1)); // 354 (4708/22)
        add.accept(ItemFluidTank.make(ModItems.FLUID_TANK_LEAD_FULL.get(), ModFluids.KEROSENE_REFORM.getSource(), 1)); // 355 (4708/98)
        add.accept(ItemFluidTank.make(ModItems.FLUID_TANK_LEAD_FULL.get(), ModFluids.PETROIL.getSource(), 1)); // 356 (4708/17)
        add.accept(ItemFluidTank.make(ModItems.FLUID_TANK_LEAD_FULL.get(), ModFluids.PETROIL_LEADED.getSource(), 1)); // 357 (4708/72)
        add.accept(ItemFluidTank.make(ModItems.FLUID_TANK_LEAD_FULL.get(), ModFluids.GASOLINE.getSource(), 1)); // 358 (4708/46)
        add.accept(ItemFluidTank.make(ModItems.FLUID_TANK_LEAD_FULL.get(), ModFluids.GASOLINE_LEADED.getSource(), 1)); // 359 (4708/73)
        add.accept(ItemFluidTank.make(ModItems.FLUID_TANK_LEAD_FULL.get(), ModFluids.COALGAS.getSource(), 1)); // 360 (4708/47)
        // Batch 37: Items 361-370
        add.accept(ItemFluidTank.make(ModItems.FLUID_TANK_LEAD_FULL.get(), ModFluids.COALGAS_LEADED.getSource(), 1)); // 361 (4708/74)
        add.accept(ItemFluidTank.make(ModItems.FLUID_TANK_LEAD_FULL.get(), ModFluids.COALCREOSOTE.getSource(), 1)); // 362 (4708/80)
        add.accept(ItemFluidTank.make(ModItems.FLUID_TANK_LEAD_FULL.get(), ModFluids.WOODOIL.getSource(), 1)); // 363 (4708/79)
        add.accept(ItemFluidTank.make(ModItems.FLUID_TANK_LEAD_FULL.get(), ModFluids.BIOGAS.getSource(), 1)); // 364 (4708/26)
        add.accept(ItemFluidTank.make(ModItems.FLUID_TANK_LEAD_FULL.get(), ModFluids.BIOFUEL.getSource(), 1)); // 365 (4708/27)
        add.accept(ItemFluidTank.make(ModItems.FLUID_TANK_LEAD_FULL.get(), ModFluids.ETHANOL.getSource(), 1)); // 366 (4708/59)
        add.accept(ItemFluidTank.make(ModItems.FLUID_TANK_LEAD_FULL.get(), ModFluids.FISHOIL.getSource(), 1)); // 367 (4708/110)
        add.accept(ItemFluidTank.make(ModItems.FLUID_TANK_LEAD_FULL.get(), ModFluids.SUNFLOWEROIL.getSource(), 1)); // 368 (4708/111)
        add.accept(ItemFluidTank.make(ModItems.FLUID_TANK_LEAD_FULL.get(), ModFluids.NITAN.getSource(), 1)); // 369 (4708/28)
        add.accept(ItemFluidTank.make(ModItems.FLUID_TANK_LEAD_FULL.get(), ModFluids.DHC.getSource(), 1)); // 370 (4708/153)
        // Batch 38: Items 371-380
        add.accept(ItemFluidTank.make(ModItems.FLUID_TANK_LEAD_FULL.get(), ModFluids.BALEFIRE.getSource(), 1)); // 371 (4708/41)
        add.accept(ItemFluidTank.make(ModItems.FLUID_TANK_LEAD_FULL.get(), ModFluids.SALIENT.getSource(), 1)); // 372 (4708/69)
        add.accept(ItemFluidTank.make(ModItems.FLUID_TANK_LEAD_FULL.get(), ModFluids.UNSTABLE_SANITY.getSource(), 1));
        add.accept(ItemFluidTank.make(ModItems.FLUID_TANK_LEAD_FULL.get(), ModFluids.SEEDSLURRY.getSource(), 1)); // 373 (4708/81)
        add.accept(ItemFluidTank.make(ModItems.FLUID_TANK_LEAD_FULL.get(), ModFluids.COLLOID.getSource(), 1)); // 374 (4708/100)
        add.accept(ItemFluidTank.make(ModItems.FLUID_TANK_LEAD_FULL.get(), ModFluids.VITRIOL.getSource(), 1)); // 375 (4708/140)
        add.accept(ItemFluidTank.make(ModItems.FLUID_TANK_LEAD_FULL.get(), ModFluids.SLOP.getSource(), 1)); // 376 (4708/141)
        add.accept(ItemFluidTank.make(ModItems.FLUID_TANK_LEAD_FULL.get(), ModFluids.IONGEL.getSource(), 1)); // 377 (4708/103)
        add.accept(ItemFluidTank.make(ModItems.FLUID_TANK_LEAD_FULL.get(), ModFluids.PEROXIDE.getSource(), 1)); // 378 (4708/35)
        add.accept(ItemFluidTank.make(ModItems.FLUID_TANK_LEAD_FULL.get(), ModFluids.SULFURIC_ACID.getSource(), 1)); // 379 (4708/75)
        add.accept(ItemFluidTank.make(ModItems.FLUID_TANK_LEAD_FULL.get(), ModFluids.NITRIC_ACID.getSource(), 1)); // 380 (4708/82)
        // Batch 39: Items 381-390
        add.accept(ItemFluidTank.make(ModItems.FLUID_TANK_LEAD_FULL.get(), ModFluids.SOLVENT.getSource(), 1)); // 381 (4708/83)
        add.accept(ItemFluidTank.make(ModItems.FLUID_TANK_LEAD_FULL.get(), ModFluids.RADIOSOLVENT.getSource(), 1)); // 382 (4708/88)
        add.accept(ItemFluidTank.make(ModItems.FLUID_TANK_LEAD_FULL.get(), ModFluids.SCHRABIDIC.getSource(), 1)); // 383 (4708/32)
        add.accept(ItemFluidTank.make(ModItems.FLUID_TANK_LEAD_FULL.get(), ModFluids.UF6.getSource(), 1)); // 384 (4708/29)
        add.accept(ItemFluidTank.make(ModItems.FLUID_TANK_LEAD_FULL.get(), ModFluids.PUF6.getSource(), 1)); // 385 (4708/30)
        add.accept(ItemFluidTank.make(ModItems.FLUID_TANK_LEAD_FULL.get(), ModFluids.SAS3.getSource(), 1)); // 386 (4708/31)
        add.accept(ItemFluidTank.make(ModItems.FLUID_TANK_LEAD_FULL.get(), ModFluids.PAIN.getSource(), 1)); // 387 (4708/43)
        add.accept(ItemFluidTank.make(ModItems.FLUID_TANK_LEAD_FULL.get(), ModFluids.DEATH.getSource(), 1)); // 388 (4708/58)
        add.accept(ItemFluidTank.make(ModItems.FLUID_TANK_LEAD_FULL.get(), ModFluids.WATZ.getSource(), 1)); // 389 (4708/36)
        add.accept(ItemFluidTank.make(ModItems.FLUID_TANK_LEAD_FULL.get(), ModFluids.REDMUD.getSource(), 1)); // 390 (4708/113)
        // Batch 40: Items 391-400
        add.accept(ItemFluidTank.make(ModItems.FLUID_TANK_LEAD_FULL.get(), ModFluids.FULLERENE.getSource(), 1)); // 391 (4708/130)
        add.accept(ItemFluidTank.make(ModItems.FLUID_TANK_LEAD_FULL.get(), ModFluids.EGG.getSource(), 1)); // 392 (4708/107)
        add.accept(ItemFluidTank.make(ModItems.FLUID_TANK_LEAD_FULL.get(), ModFluids.CHOLESTEROL.getSource(), 1)); // 393 (4708/108)
        add.accept(ItemFluidTank.make(ModItems.FLUID_TANK_LEAD_FULL.get(), ModFluids.SODIUM_ALUMINATE.getSource(), 1)); // 394 (4708/148)
        add.accept(ItemFluidTank.make(ModItems.FLUID_TANK_LEAD_FULL.get(), ModFluids.BAUXITE_SOLUTION.getSource(), 1)); // 395 (4708/149)
        add.accept(ItemFluidTank.make(ModItems.FLUID_TANK_LEAD_FULL.get(), ModFluids.ALUMINA.getSource(), 1)); // 396 (4708/150)
        add.accept(ItemFluidTank.make(ModItems.FLUID_TANK_LEAD_FULL.get(), ModFluids.TITANIUM_TETRACHLORIDE.getSource(), 1)); // 397 (4708/156)
        add.accept(ItemFluidTank.make(ModItems.FLUID_TANK_LEAD_FULL.get(), ModFluids.CONCRETE.getSource(), 1)); // 398 (4708/152)
        add.accept(ItemFluidTank.make(ModItems.FLUID_TANK_LEAD_FULL.get(), ModFluids.FRACKSOL.getSource(), 1)); // 399 (4708/49)
        add.accept(ItemFluidTank.make(ModItems.FLUID_TANK_LEAD_FULL.get(), ModFluids.LYE.getSource(), 1)); // 400 (4708/147)

        // --- CHUNK 5: Items 401-500 (worker_m5_control_c5) ---
        // Batch 41: Items 401-410
        add.accept(ItemFluidTank.make(ModItems.FLUID_TANK_LEAD_FULL.get(), ModFluids.PHOSGENE.getSource(), 1)); // 401 (4708/101)
        add.accept(ItemFluidTank.make(ModItems.FLUID_TANK_LEAD_FULL.get(), ModFluids.MUSTARDGAS.getSource(), 1)); // 402 (4708/102)
        add.accept(ItemFluidTank.make(ModItems.FLUID_TANK_LEAD_FULL.get(), ModFluids.ESTRADIOL.getSource(), 1)); // 403 (4708/109)
        add.accept(ItemFluidTank.make(ModItems.FLUID_TANK_LEAD_FULL.get(), ModFluids.NITROGLYCERIN.getSource(), 1)); // 404 (4708/112)
        add.accept(ItemFluidTank.make(ModItems.FLUID_TANK_LEAD_FULL.get(), ModFluids.AMAT.getSource(), 1)); // 405 (4708/33)
        add.accept(ItemFluidTank.make(ModItems.FLUID_TANK_LEAD_FULL.get(), ModFluids.ASCHRAB.getSource(), 1)); // 406 (4708/34)
        add.accept(ItemFluidTank.make(ModItems.FLUID_TANK_LEAD_FULL.get(), ModFluids.XPJUICE.getSource(), 1)); // 407 (4708/70)
        add.accept(ItemFluidTank.make(ModItems.FLUID_TANK_LEAD_FULL.get(), ModFluids.ENDERJUICE.getSource(), 1)); // 408 (4708/71)
        add.accept(ItemFluidTank.make(ModItems.FLUID_TANK_LEAD_FULL.get(), ModFluids.STELLAR_FLUX.getSource(), 1)); // 409 (4708/139)
        add.accept(ItemFluidTank.make(ModItems.FLUID_TANK_LEAD_FULL.get(), ModFluids.PHEROMONE.getSource(), 1)); // 410 (4708/131)
        // Batch 42: Items 411-420
        add.accept(ItemFluidTank.make(ModItems.FLUID_TANK_LEAD_FULL.get(), ModFluids.PHEROMONE_M.getSource(), 1)); // 411 (4708/132)
        add.accept(ItemFluidTank.make(ModItems.FLUID_TANK_LEAD_FULL.get(), ModFluids.CUSTOM_DEMO.getSource(), 1)); // 412 (4708/1000)
        add.accept(new ItemStack(ModItems.FLUID_BARREL_EMPTY.get()));                               // 413 (4709/0)
        add.accept(ItemFluidTank.make(ModItems.FLUID_BARREL_FULL.get(), ModFluids.AIR.getSource(), 1)); // 414 (4710/151)
        add.accept(ItemFluidTank.make(ModItems.FLUID_BARREL_FULL.get(), ModFluids.AIRBLAST.getSource(), 1)); // 415 (4710/154)
        add.accept(ItemFluidTank.make(ModItems.FLUID_BARREL_FULL.get(), ModFluids.WATER.getSource(), 1)); // 416 (4710/1)
        add.accept(ItemFluidTank.make(ModItems.FLUID_BARREL_FULL.get(), ModFluids.HEAVYWATER.getSource(), 1)); // 417 (4710/60)
        add.accept(ItemFluidTank.make(ModItems.FLUID_BARREL_FULL.get(), ModFluids.HEAVYWATER_HOT.getSource(), 1)); // 418 (4710/124)
        add.accept(ItemFluidTank.make(ModItems.FLUID_BARREL_FULL.get(), ModFluids.LAVA.getSource(), 1)); // 419 (4710/7)
        add.accept(ItemFluidTank.make(ModItems.FLUID_BARREL_FULL.get(), ModFluids.STEAM.getSource(), 1)); // 420 (4710/2)
        // Batch 43: Items 421-430
        add.accept(ItemFluidTank.make(ModItems.FLUID_BARREL_FULL.get(), ModFluids.HOTSTEAM.getSource(), 1)); // 421 (4710/3)
        add.accept(ItemFluidTank.make(ModItems.FLUID_BARREL_FULL.get(), ModFluids.SUPERHOTSTEAM.getSource(), 1)); // 422 (4710/4)
        add.accept(ItemFluidTank.make(ModItems.FLUID_BARREL_FULL.get(), ModFluids.ULTRAHOTSTEAM.getSource(), 1)); // 423 (4710/5)
        add.accept(ItemFluidTank.make(ModItems.FLUID_BARREL_FULL.get(), ModFluids.CARBONDIOXIDE.getSource(), 1)); // 424 (4710/55)
        add.accept(ItemFluidTank.make(ModItems.FLUID_BARREL_FULL.get(), ModFluids.COOLANT.getSource(), 1)); // 425 (4710/6)
        add.accept(ItemFluidTank.make(ModItems.FLUID_BARREL_FULL.get(), ModFluids.COOLANT_HOT.getSource(), 1)); // 426 (4710/76)
        add.accept(ItemFluidTank.make(ModItems.FLUID_BARREL_FULL.get(), ModFluids.PERFLUOROMETHYL.getSource(), 1)); // 427 (4710/144)
        add.accept(ItemFluidTank.make(ModItems.FLUID_BARREL_FULL.get(), ModFluids.PERFLUOROMETHYL_COLD.getSource(), 1)); // 428 (4710/145)
        add.accept(ItemFluidTank.make(ModItems.FLUID_BARREL_FULL.get(), ModFluids.PERFLUOROMETHYL_HOT.getSource(), 1)); // 429 (4710/146)
        add.accept(ItemFluidTank.make(ModItems.FLUID_BARREL_FULL.get(), ModFluids.CRYOGEL.getSource(), 1)); // 430 (4710/37)
        // Batch 44: Items 431-440
        add.accept(ItemFluidTank.make(ModItems.FLUID_BARREL_FULL.get(), ModFluids.MUG.getSource(), 1)); // 431 (4710/77)
        add.accept(ItemFluidTank.make(ModItems.FLUID_BARREL_FULL.get(), ModFluids.MUG_HOT.getSource(), 1)); // 432 (4710/78)
        add.accept(ItemFluidTank.make(ModItems.FLUID_BARREL_FULL.get(), ModFluids.BLOOD.getSource(), 1)); // 433 (4710/84)
        add.accept(ItemFluidTank.make(ModItems.FLUID_BARREL_FULL.get(), ModFluids.BLOOD_HOT.getSource(), 1)); // 434 (4710/85)
        add.accept(ItemFluidTank.make(ModItems.FLUID_BARREL_FULL.get(), ModFluids.SODIUM.getSource(), 1)); // 435 (4710/125)
        add.accept(ItemFluidTank.make(ModItems.FLUID_BARREL_FULL.get(), ModFluids.SODIUM_HOT.getSource(), 1)); // 436 (4710/126)
        add.accept(ItemFluidTank.make(ModItems.FLUID_BARREL_FULL.get(), ModFluids.LEAD.getSource(), 1)); // 437 (4710/142)
        add.accept(ItemFluidTank.make(ModItems.FLUID_BARREL_FULL.get(), ModFluids.LEAD_HOT.getSource(), 1)); // 438 (4710/143)
        add.accept(ItemFluidTank.make(ModItems.FLUID_BARREL_FULL.get(), ModFluids.THORIUM_SALT.getSource(), 1)); // 439 (4710/127)
        add.accept(ItemFluidTank.make(ModItems.FLUID_BARREL_FULL.get(), ModFluids.THORIUM_SALT_HOT.getSource(), 1)); // 440 (4710/128)
        // Batch 45: Items 441-450
        add.accept(ItemFluidTank.make(ModItems.FLUID_BARREL_FULL.get(), ModFluids.THORIUM_SALT_DEPLETED.getSource(), 1)); // 441 (4710/129)
        add.accept(ItemFluidTank.make(ModItems.FLUID_BARREL_FULL.get(), ModFluids.HYDROGEN.getSource(), 1)); // 442 (4710/38)
        add.accept(ItemFluidTank.make(ModItems.FLUID_BARREL_FULL.get(), ModFluids.DEUTERIUM.getSource(), 1)); // 443 (4710/8)
        add.accept(ItemFluidTank.make(ModItems.FLUID_BARREL_FULL.get(), ModFluids.TRITIUM.getSource(), 1)); // 444 (4710/9)
        add.accept(ItemFluidTank.make(ModItems.FLUID_BARREL_FULL.get(), ModFluids.HELIUM3.getSource(), 1)); // 445 (4710/57)
        add.accept(ItemFluidTank.make(ModItems.FLUID_BARREL_FULL.get(), ModFluids.HELIUM4.getSource(), 1)); // 446 (4710/123)
        add.accept(ItemFluidTank.make(ModItems.FLUID_BARREL_FULL.get(), ModFluids.OXYGEN.getSource(), 1)); // 447 (4710/39)
        add.accept(ItemFluidTank.make(ModItems.FLUID_BARREL_FULL.get(), ModFluids.XENON.getSource(), 1)); // 448 (4710/40)
        add.accept(ItemFluidTank.make(ModItems.FLUID_BARREL_FULL.get(), ModFluids.CHLORINE.getSource(), 1)); // 449 (4710/89)
        add.accept(ItemFluidTank.make(ModItems.FLUID_BARREL_FULL.get(), ModFluids.MERCURY.getSource(), 1)); // 450 (4710/42)
        // Batch 46: Items 451-460
        add.accept(ItemFluidTank.make(ModItems.FLUID_BARREL_FULL.get(), ModFluids.CRUDE_OIL.getSource(), 1)); // 451 (4710/10)
        add.accept(ItemFluidTank.make(ModItems.FLUID_BARREL_FULL.get(), ModFluids.OIL_DS.getSource(), 1)); // 452 (4710/133)
        add.accept(ItemFluidTank.make(ModItems.FLUID_BARREL_FULL.get(), ModFluids.CRACKOIL.getSource(), 1)); // 453 (4710/61)
        add.accept(ItemFluidTank.make(ModItems.FLUID_BARREL_FULL.get(), ModFluids.CRACKOIL_DS.getSource(), 1)); // 454 (4710/135)
        add.accept(ItemFluidTank.make(ModItems.FLUID_BARREL_FULL.get(), ModFluids.COALOIL.getSource(), 1)); // 455 (4710/62)
        add.accept(ItemFluidTank.make(ModItems.FLUID_BARREL_FULL.get(), ModFluids.OIL_COKER.getSource(), 1)); // 456 (4710/104)
        add.accept(ItemFluidTank.make(ModItems.FLUID_BARREL_FULL.get(), ModFluids.HOTOIL.getSource(), 1)); // 457 (4710/11)
        add.accept(ItemFluidTank.make(ModItems.FLUID_BARREL_FULL.get(), ModFluids.HOTOIL_DS.getSource(), 1)); // 458 (4710/134)
        add.accept(ItemFluidTank.make(ModItems.FLUID_BARREL_FULL.get(), ModFluids.HOTCRACKOIL.getSource(), 1)); // 459 (4710/63)
        add.accept(ItemFluidTank.make(ModItems.FLUID_BARREL_FULL.get(), ModFluids.HOTCRACKOIL_DS.getSource(), 1)); // 460 (4710/136)
        // Batch 47: Items 461-470
        add.accept(ItemFluidTank.make(ModItems.FLUID_BARREL_FULL.get(), ModFluids.HEAVYOIL.getSource(), 1)); // 461 (4710/12)
        add.accept(ItemFluidTank.make(ModItems.FLUID_BARREL_FULL.get(), ModFluids.HEAVYOIL_VACUUM.getSource(), 1)); // 462 (4710/90)
        add.accept(ItemFluidTank.make(ModItems.FLUID_BARREL_FULL.get(), ModFluids.NAPHTHA.getSource(), 1)); // 463 (4710/19)
        add.accept(ItemFluidTank.make(ModItems.FLUID_BARREL_FULL.get(), ModFluids.NAPHTHA_DS.getSource(), 1)); // 464 (4710/137)
        add.accept(ItemFluidTank.make(ModItems.FLUID_BARREL_FULL.get(), ModFluids.NAPHTHA_CRACK.getSource(), 1)); // 465 (4710/64)
        add.accept(ItemFluidTank.make(ModItems.FLUID_BARREL_FULL.get(), ModFluids.NAPHTHA_COKER.getSource(), 1)); // 466 (4710/105)
        add.accept(ItemFluidTank.make(ModItems.FLUID_BARREL_FULL.get(), ModFluids.REFORMATE.getSource(), 1)); // 467 (4710/91)
        add.accept(ItemFluidTank.make(ModItems.FLUID_BARREL_FULL.get(), ModFluids.LIGHTOIL.getSource(), 1)); // 468 (4710/21)
        add.accept(ItemFluidTank.make(ModItems.FLUID_BARREL_FULL.get(), ModFluids.LIGHTOIL_DS.getSource(), 1)); // 469 (4710/138)
        add.accept(ItemFluidTank.make(ModItems.FLUID_BARREL_FULL.get(), ModFluids.LIGHTOIL_CRACK.getSource(), 1)); // 470 (4710/65)
        // Batch 48: Items 471-480
        add.accept(ItemFluidTank.make(ModItems.FLUID_BARREL_FULL.get(), ModFluids.LIGHTOIL_VACUUM.getSource(), 1)); // 471 (4710/92)
        add.accept(ItemFluidTank.make(ModItems.FLUID_BARREL_FULL.get(), ModFluids.BITUMEN.getSource(), 1)); // 472 (4710/13)
        add.accept(ItemFluidTank.make(ModItems.FLUID_BARREL_FULL.get(), ModFluids.SMEAR.getSource(), 1)); // 473 (4710/14)
        add.accept(ItemFluidTank.make(ModItems.FLUID_BARREL_FULL.get(), ModFluids.HEATINGOIL.getSource(), 1)); // 474 (4710/15)
        add.accept(ItemFluidTank.make(ModItems.FLUID_BARREL_FULL.get(), ModFluids.HEATINGOIL_VACUUM.getSource(), 1)); // 475 (4710/95)
        add.accept(ItemFluidTank.make(ModItems.FLUID_BARREL_FULL.get(), ModFluids.RECLAIMED.getSource(), 1)); // 476 (4710/16)
        add.accept(ItemFluidTank.make(ModItems.FLUID_BARREL_FULL.get(), ModFluids.LUBRICANT.getSource(), 1)); // 477 (4710/18)
        add.accept(ItemFluidTank.make(ModItems.FLUID_BARREL_FULL.get(), ModFluids.FLUE.getSource(), 1)); // 478 (4710/155)
        add.accept(ItemFluidTank.make(ModItems.FLUID_BARREL_FULL.get(), ModFluids.GAS.getSource(), 1)); // 479 (4710/23)
        add.accept(ItemFluidTank.make(ModItems.FLUID_BARREL_FULL.get(), ModFluids.GAS_COKER.getSource(), 1)); // 480 (4710/106)
        // Batch 49: Items 481-490
        add.accept(ItemFluidTank.make(ModItems.FLUID_BARREL_FULL.get(), ModFluids.PETROLEUM.getSource(), 1)); // 481 (4710/24)
        add.accept(ItemFluidTank.make(ModItems.FLUID_BARREL_FULL.get(), ModFluids.SOURGAS.getSource(), 1)); // 482 (4710/93)
        add.accept(ItemFluidTank.make(ModItems.FLUID_BARREL_FULL.get(), ModFluids.LPG.getSource(), 1)); // 483 (4710/25)
        add.accept(ItemFluidTank.make(ModItems.FLUID_BARREL_FULL.get(), ModFluids.SYNGAS.getSource(), 1)); // 484 (4710/86)
        add.accept(ItemFluidTank.make(ModItems.FLUID_BARREL_FULL.get(), ModFluids.OXYHYDROGEN.getSource(), 1)); // 485 (4710/87)
        add.accept(ItemFluidTank.make(ModItems.FLUID_BARREL_FULL.get(), ModFluids.AROMATICS.getSource(), 1)); // 486 (4710/67)
        add.accept(ItemFluidTank.make(ModItems.FLUID_BARREL_FULL.get(), ModFluids.UNSATURATEDS.getSource(), 1)); // 487 (4710/68)
        add.accept(ItemFluidTank.make(ModItems.FLUID_BARREL_FULL.get(), ModFluids.XYLENE.getSource(), 1)); // 488 (4710/94)
        add.accept(ItemFluidTank.make(ModItems.FLUID_BARREL_FULL.get(), ModFluids.REFORMGAS.getSource(), 1)); // 489 (4710/99)
        add.accept(ItemFluidTank.make(ModItems.FLUID_BARREL_FULL.get(), ModFluids.DIESEL.getSource(), 1)); // 490 (4710/20)
        // Batch 50: Items 491-500
        add.accept(ItemFluidTank.make(ModItems.FLUID_BARREL_FULL.get(), ModFluids.DIESEL_REFORM.getSource(), 1)); // 491 (4710/96)
        add.accept(ItemFluidTank.make(ModItems.FLUID_BARREL_FULL.get(), ModFluids.DIESEL_CRACK.getSource(), 1)); // 492 (4710/66)
        add.accept(ItemFluidTank.make(ModItems.FLUID_BARREL_FULL.get(), ModFluids.DIESEL_CRACK_REFORM.getSource(), 1)); // 493 (4710/97)
        add.accept(ItemFluidTank.make(ModItems.FLUID_BARREL_FULL.get(), ModFluids.KEROSENE.getSource(), 1)); // 494 (4710/22)
        add.accept(ItemFluidTank.make(ModItems.FLUID_BARREL_FULL.get(), ModFluids.KEROSENE_REFORM.getSource(), 1)); // 495 (4710/98)
        add.accept(ItemFluidTank.make(ModItems.FLUID_BARREL_FULL.get(), ModFluids.PETROIL.getSource(), 1)); // 496 (4710/17)
        add.accept(ItemFluidTank.make(ModItems.FLUID_BARREL_FULL.get(), ModFluids.PETROIL_LEADED.getSource(), 1)); // 497 (4710/72)
        add.accept(ItemFluidTank.make(ModItems.FLUID_BARREL_FULL.get(), ModFluids.GASOLINE.getSource(), 1)); // 498 (4710/46)
        add.accept(ItemFluidTank.make(ModItems.FLUID_BARREL_FULL.get(), ModFluids.GASOLINE_LEADED.getSource(), 1)); // 499 (4710/73)
        add.accept(ItemFluidTank.make(ModItems.FLUID_BARREL_FULL.get(), ModFluids.COALGAS.getSource(), 1)); // 500 (4710/47)

        // --- CHUNK 6: Items 501-600 (worker_m5_control_c6) ---
        // Batch 51: Items 501-510 (Fluid Barrels 88-97 of 137)
        add.accept(ItemFluidTank.make(ModItems.FLUID_BARREL_FULL.get(), ModFluids.COALGAS_LEADED.getSource(), 1)); // 501 (4710/74)
        add.accept(ItemFluidTank.make(ModItems.FLUID_BARREL_FULL.get(), ModFluids.COALCREOSOTE.getSource(), 1)); // 502 (4710/80)
        add.accept(ItemFluidTank.make(ModItems.FLUID_BARREL_FULL.get(), ModFluids.WOODOIL.getSource(), 1)); // 503 (4710/79)
        add.accept(ItemFluidTank.make(ModItems.FLUID_BARREL_FULL.get(), ModFluids.BIOGAS.getSource(), 1)); // 504 (4710/26)
        add.accept(ItemFluidTank.make(ModItems.FLUID_BARREL_FULL.get(), ModFluids.BIOFUEL.getSource(), 1)); // 505 (4710/27)
        add.accept(ItemFluidTank.make(ModItems.FLUID_BARREL_FULL.get(), ModFluids.ETHANOL.getSource(), 1)); // 506 (4710/59)
        add.accept(ItemFluidTank.make(ModItems.FLUID_BARREL_FULL.get(), ModFluids.FISHOIL.getSource(), 1)); // 507 (4710/110)
        add.accept(ItemFluidTank.make(ModItems.FLUID_BARREL_FULL.get(), ModFluids.SUNFLOWEROIL.getSource(), 1)); // 508 (4710/111)
        add.accept(ItemFluidTank.make(ModItems.FLUID_BARREL_FULL.get(), ModFluids.NITAN.getSource(), 1)); // 509 (4710/28)
        add.accept(ItemFluidTank.make(ModItems.FLUID_BARREL_FULL.get(), ModFluids.DHC.getSource(), 1)); // 510 (4710/153)
        // Batch 52: Items 511-520 (Fluid Barrels 98-107 of 137)
        add.accept(ItemFluidTank.make(ModItems.FLUID_BARREL_FULL.get(), ModFluids.BALEFIRE.getSource(), 1)); // 511 (4710/41)
        add.accept(ItemFluidTank.make(ModItems.FLUID_BARREL_FULL.get(), ModFluids.SALIENT.getSource(), 1)); // 512 (4710/69)
        add.accept(ItemFluidTank.make(ModItems.FLUID_BARREL_FULL.get(), ModFluids.UNSTABLE_SANITY.getSource(), 1));
        add.accept(ItemFluidTank.make(ModItems.FLUID_BARREL_FULL.get(), ModFluids.SEEDSLURRY.getSource(), 1)); // 513 (4710/81)
        add.accept(ItemFluidTank.make(ModItems.FLUID_BARREL_FULL.get(), ModFluids.COLLOID.getSource(), 1)); // 514 (4710/100)
        add.accept(ItemFluidTank.make(ModItems.FLUID_BARREL_FULL.get(), ModFluids.VITRIOL.getSource(), 1)); // 515 (4710/140)
        add.accept(ItemFluidTank.make(ModItems.FLUID_BARREL_FULL.get(), ModFluids.SLOP.getSource(), 1)); // 516 (4710/141)
        add.accept(ItemFluidTank.make(ModItems.FLUID_BARREL_FULL.get(), ModFluids.IONGEL.getSource(), 1)); // 517 (4710/103)
        add.accept(ItemFluidTank.make(ModItems.FLUID_BARREL_FULL.get(), ModFluids.PEROXIDE.getSource(), 1)); // 518 (4710/35)
        add.accept(ItemFluidTank.make(ModItems.FLUID_BARREL_FULL.get(), ModFluids.SULFURIC_ACID.getSource(), 1)); // 519 (4710/75)
        add.accept(ItemFluidTank.make(ModItems.FLUID_BARREL_FULL.get(), ModFluids.NITRIC_ACID.getSource(), 1)); // 520 (4710/82)
        // Batch 53: Items 521-530 (Fluid Barrels 108-117 of 137)
        add.accept(ItemFluidTank.make(ModItems.FLUID_BARREL_FULL.get(), ModFluids.SOLVENT.getSource(), 1)); // 521 (4710/83)
        add.accept(ItemFluidTank.make(ModItems.FLUID_BARREL_FULL.get(), ModFluids.RADIOSOLVENT.getSource(), 1)); // 522 (4710/88)
        add.accept(ItemFluidTank.make(ModItems.FLUID_BARREL_FULL.get(), ModFluids.SCHRABIDIC.getSource(), 1)); // 523 (4710/32)
        add.accept(ItemFluidTank.make(ModItems.FLUID_BARREL_FULL.get(), ModFluids.UF6.getSource(), 1)); // 524 (4710/29)
        add.accept(ItemFluidTank.make(ModItems.FLUID_BARREL_FULL.get(), ModFluids.PUF6.getSource(), 1)); // 525 (4710/30)
        add.accept(ItemFluidTank.make(ModItems.FLUID_BARREL_FULL.get(), ModFluids.SAS3.getSource(), 1)); // 526 (4710/31)
        add.accept(ItemFluidTank.make(ModItems.FLUID_BARREL_FULL.get(), ModFluids.PAIN.getSource(), 1)); // 527 (4710/43)
        add.accept(ItemFluidTank.make(ModItems.FLUID_BARREL_FULL.get(), ModFluids.WATZ.getSource(), 1)); // 528 (4710/36)
        add.accept(ItemFluidTank.make(ModItems.FLUID_BARREL_FULL.get(), ModFluids.FULLERENE.getSource(), 1)); // 529 (4710/130)
        add.accept(ItemFluidTank.make(ModItems.FLUID_BARREL_FULL.get(), ModFluids.EGG.getSource(), 1)); // 530 (4710/107)
        // Batch 54: Items 531-540 (Fluid Barrels 118-127 of 137)
        add.accept(ItemFluidTank.make(ModItems.FLUID_BARREL_FULL.get(), ModFluids.CHOLESTEROL.getSource(), 1)); // 531 (4710/108)
        add.accept(ItemFluidTank.make(ModItems.FLUID_BARREL_FULL.get(), ModFluids.SODIUM_ALUMINATE.getSource(), 1)); // 532 (4710/148)
        add.accept(ItemFluidTank.make(ModItems.FLUID_BARREL_FULL.get(), ModFluids.BAUXITE_SOLUTION.getSource(), 1)); // 533 (4710/149)
        add.accept(ItemFluidTank.make(ModItems.FLUID_BARREL_FULL.get(), ModFluids.ALUMINA.getSource(), 1)); // 534 (4710/150)
        add.accept(ItemFluidTank.make(ModItems.FLUID_BARREL_FULL.get(), ModFluids.TITANIUM_TETRACHLORIDE.getSource(), 1)); // 535 (4710/156)
        add.accept(ItemFluidTank.make(ModItems.FLUID_BARREL_FULL.get(), ModFluids.CONCRETE.getSource(), 1)); // 536 (4710/152)
        add.accept(ItemFluidTank.make(ModItems.FLUID_BARREL_FULL.get(), ModFluids.FRACKSOL.getSource(), 1)); // 537 (4710/49)
        add.accept(ItemFluidTank.make(ModItems.FLUID_BARREL_FULL.get(), ModFluids.LYE.getSource(), 1)); // 538 (4710/147)
        add.accept(ItemFluidTank.make(ModItems.FLUID_BARREL_FULL.get(), ModFluids.PHOSGENE.getSource(), 1)); // 539 (4710/101)
        add.accept(ItemFluidTank.make(ModItems.FLUID_BARREL_FULL.get(), ModFluids.MUSTARDGAS.getSource(), 1)); // 540 (4710/102)
        // Batch 55: Items 541-550 (Fluid Barrels 128-136 of 137, Item 550 Custom Demo barrel)
        add.accept(ItemFluidTank.make(ModItems.FLUID_BARREL_FULL.get(), ModFluids.ESTRADIOL.getSource(), 1)); // 541 (4710/109)
        add.accept(ItemFluidTank.make(ModItems.FLUID_BARREL_FULL.get(), ModFluids.NITROGLYCERIN.getSource(), 1)); // 542 (4710/112)
        add.accept(ItemFluidTank.make(ModItems.FLUID_BARREL_FULL.get(), ModFluids.AMAT.getSource(), 1)); // 543 (4710/33)
        add.accept(ItemFluidTank.make(ModItems.FLUID_BARREL_FULL.get(), ModFluids.ASCHRAB.getSource(), 1)); // 544 (4710/34)
        add.accept(ItemFluidTank.make(ModItems.FLUID_BARREL_FULL.get(), ModFluids.XPJUICE.getSource(), 1)); // 545 (4710/70)
        add.accept(ItemFluidTank.make(ModItems.FLUID_BARREL_FULL.get(), ModFluids.ENDERJUICE.getSource(), 1)); // 546 (4710/71)
        add.accept(ItemFluidTank.make(ModItems.FLUID_BARREL_FULL.get(), ModFluids.STELLAR_FLUX.getSource(), 1)); // 547 (4710/139)
        add.accept(ItemFluidTank.make(ModItems.FLUID_BARREL_FULL.get(), ModFluids.PHEROMONE.getSource(), 1)); // 548 (4710/131)
        add.accept(ItemFluidTank.make(ModItems.FLUID_BARREL_FULL.get(), ModFluids.PHEROMONE_M.getSource(), 1)); // 549 (4710/132)
        add.accept(ItemFluidTank.make(ModItems.FLUID_BARREL_FULL.get(), ModFluids.CUSTOM_DEMO.getSource(), 1)); // 550 (4710/1000)
        // Batch 56: Items 551-560 (Item 551 Infinite Barrel, Item 552 Empty Pack, Items 553-560 Packs 1-8 of 137)
        add.accept(new ItemStack(ModItems.FLUID_BARREL_INFINITE.get())); // 551 (4711/0)
        add.accept(new ItemStack(ModItems.FLUID_PACK_EMPTY.get())); // 552 (4712/0)
        add.accept(ItemFluidTank.make(ModItems.FLUID_PACK_FULL.get(), ModFluids.AIR.getSource(), 1)); // 553 (4713/151)
        add.accept(ItemFluidTank.make(ModItems.FLUID_PACK_FULL.get(), ModFluids.AIRBLAST.getSource(), 1)); // 554 (4713/154)
        add.accept(ItemFluidTank.make(ModItems.FLUID_PACK_FULL.get(), ModFluids.WATER.getSource(), 1)); // 555 (4713/1)
        add.accept(ItemFluidTank.make(ModItems.FLUID_PACK_FULL.get(), ModFluids.HEAVYWATER.getSource(), 1)); // 556 (4713/60)
        add.accept(ItemFluidTank.make(ModItems.FLUID_PACK_FULL.get(), ModFluids.HEAVYWATER_HOT.getSource(), 1)); // 557 (4713/124)
        add.accept(ItemFluidTank.make(ModItems.FLUID_PACK_FULL.get(), ModFluids.LAVA.getSource(), 1)); // 558 (4713/7)
        add.accept(ItemFluidTank.make(ModItems.FLUID_PACK_FULL.get(), ModFluids.STEAM.getSource(), 1)); // 559 (4713/2)
        add.accept(ItemFluidTank.make(ModItems.FLUID_PACK_FULL.get(), ModFluids.HOTSTEAM.getSource(), 1)); // 560 (4713/3)
        // Batch 57: Items 561-570 (Fluid Packs 9-18 of 137)
        add.accept(ItemFluidTank.make(ModItems.FLUID_PACK_FULL.get(), ModFluids.SUPERHOTSTEAM.getSource(), 1)); // 561 (4713/4)
        add.accept(ItemFluidTank.make(ModItems.FLUID_PACK_FULL.get(), ModFluids.ULTRAHOTSTEAM.getSource(), 1)); // 562 (4713/5)
        add.accept(ItemFluidTank.make(ModItems.FLUID_PACK_FULL.get(), ModFluids.CARBONDIOXIDE.getSource(), 1)); // 563 (4713/55)
        add.accept(ItemFluidTank.make(ModItems.FLUID_PACK_FULL.get(), ModFluids.COOLANT.getSource(), 1)); // 564 (4713/6)
        add.accept(ItemFluidTank.make(ModItems.FLUID_PACK_FULL.get(), ModFluids.COOLANT_HOT.getSource(), 1)); // 565 (4713/76)
        add.accept(ItemFluidTank.make(ModItems.FLUID_PACK_FULL.get(), ModFluids.PERFLUOROMETHYL.getSource(), 1)); // 566 (4713/144)
        add.accept(ItemFluidTank.make(ModItems.FLUID_PACK_FULL.get(), ModFluids.PERFLUOROMETHYL_COLD.getSource(), 1)); // 567 (4713/145)
        add.accept(ItemFluidTank.make(ModItems.FLUID_PACK_FULL.get(), ModFluids.PERFLUOROMETHYL_HOT.getSource(), 1)); // 568 (4713/146)
        add.accept(ItemFluidTank.make(ModItems.FLUID_PACK_FULL.get(), ModFluids.CRYOGEL.getSource(), 1)); // 569 (4713/37)
        add.accept(ItemFluidTank.make(ModItems.FLUID_PACK_FULL.get(), ModFluids.MUG.getSource(), 1)); // 570 (4713/77)
        // Batch 58: Items 571-580 (Fluid Packs 19-28 of 137)
        add.accept(ItemFluidTank.make(ModItems.FLUID_PACK_FULL.get(), ModFluids.MUG_HOT.getSource(), 1)); // 571 (4713/78)
        add.accept(ItemFluidTank.make(ModItems.FLUID_PACK_FULL.get(), ModFluids.BLOOD.getSource(), 1)); // 572 (4713/84)
        add.accept(ItemFluidTank.make(ModItems.FLUID_PACK_FULL.get(), ModFluids.BLOOD_HOT.getSource(), 1)); // 573 (4713/85)
        add.accept(ItemFluidTank.make(ModItems.FLUID_PACK_FULL.get(), ModFluids.SODIUM.getSource(), 1)); // 574 (4713/125)
        add.accept(ItemFluidTank.make(ModItems.FLUID_PACK_FULL.get(), ModFluids.SODIUM_HOT.getSource(), 1)); // 575 (4713/126)
        add.accept(ItemFluidTank.make(ModItems.FLUID_PACK_FULL.get(), ModFluids.LEAD.getSource(), 1)); // 576 (4713/142)
        add.accept(ItemFluidTank.make(ModItems.FLUID_PACK_FULL.get(), ModFluids.LEAD_HOT.getSource(), 1)); // 577 (4713/143)
        add.accept(ItemFluidTank.make(ModItems.FLUID_PACK_FULL.get(), ModFluids.THORIUM_SALT.getSource(), 1)); // 578 (4713/127)
        add.accept(ItemFluidTank.make(ModItems.FLUID_PACK_FULL.get(), ModFluids.THORIUM_SALT_HOT.getSource(), 1)); // 579 (4713/128)
        add.accept(ItemFluidTank.make(ModItems.FLUID_PACK_FULL.get(), ModFluids.THORIUM_SALT_DEPLETED.getSource(), 1)); // 580 (4713/129)
        // Batch 59: Items 581-590 (Fluid Packs 29-38 of 137)
        add.accept(ItemFluidTank.make(ModItems.FLUID_PACK_FULL.get(), ModFluids.HYDROGEN.getSource(), 1)); // 581 (4713/38)
        add.accept(ItemFluidTank.make(ModItems.FLUID_PACK_FULL.get(), ModFluids.DEUTERIUM.getSource(), 1)); // 582 (4713/8)
        add.accept(ItemFluidTank.make(ModItems.FLUID_PACK_FULL.get(), ModFluids.TRITIUM.getSource(), 1)); // 583 (4713/9)
        add.accept(ItemFluidTank.make(ModItems.FLUID_PACK_FULL.get(), ModFluids.HELIUM3.getSource(), 1)); // 584 (4713/57)
        add.accept(ItemFluidTank.make(ModItems.FLUID_PACK_FULL.get(), ModFluids.HELIUM4.getSource(), 1)); // 585 (4713/123)
        add.accept(ItemFluidTank.make(ModItems.FLUID_PACK_FULL.get(), ModFluids.OXYGEN.getSource(), 1)); // 586 (4713/39)
        add.accept(ItemFluidTank.make(ModItems.FLUID_PACK_FULL.get(), ModFluids.XENON.getSource(), 1)); // 587 (4713/40)
        add.accept(ItemFluidTank.make(ModItems.FLUID_PACK_FULL.get(), ModFluids.CHLORINE.getSource(), 1)); // 588 (4713/89)
        add.accept(ItemFluidTank.make(ModItems.FLUID_PACK_FULL.get(), ModFluids.MERCURY.getSource(), 1)); // 589 (4713/42)
        add.accept(ItemFluidTank.make(ModItems.FLUID_PACK_FULL.get(), ModFluids.CRUDE_OIL.getSource(), 1)); // 590 (4713/10)
        // Batch 60: Items 591-600 (Fluid Packs 39-48 of 137)
        add.accept(ItemFluidTank.make(ModItems.FLUID_PACK_FULL.get(), ModFluids.OIL_DS.getSource(), 1)); // 591 (4713/133)
        add.accept(ItemFluidTank.make(ModItems.FLUID_PACK_FULL.get(), ModFluids.CRACKOIL.getSource(), 1)); // 592 (4713/61)
        add.accept(ItemFluidTank.make(ModItems.FLUID_PACK_FULL.get(), ModFluids.CRACKOIL_DS.getSource(), 1)); // 593 (4713/135)
        add.accept(ItemFluidTank.make(ModItems.FLUID_PACK_FULL.get(), ModFluids.COALOIL.getSource(), 1)); // 594 (4713/62)
        add.accept(ItemFluidTank.make(ModItems.FLUID_PACK_FULL.get(), ModFluids.OIL_COKER.getSource(), 1)); // 595 (4713/104)
        add.accept(ItemFluidTank.make(ModItems.FLUID_PACK_FULL.get(), ModFluids.HOTOIL.getSource(), 1)); // 596 (4713/11)
        add.accept(ItemFluidTank.make(ModItems.FLUID_PACK_FULL.get(), ModFluids.HOTOIL_DS.getSource(), 1)); // 597 (4713/134)
        add.accept(ItemFluidTank.make(ModItems.FLUID_PACK_FULL.get(), ModFluids.HOTCRACKOIL.getSource(), 1)); // 598 (4713/63)
        add.accept(ItemFluidTank.make(ModItems.FLUID_PACK_FULL.get(), ModFluids.HOTCRACKOIL_DS.getSource(), 1)); // 599 (4713/136)
        add.accept(ItemFluidTank.make(ModItems.FLUID_PACK_FULL.get(), ModFluids.HEAVYOIL.getSource(), 1)); // 600 (4713/12)

        // --- CHUNK 7: Items 601-700 (worker_m5_control_c7) ---
        // Batch 61: Items 601-610 (Fluid Packs 49-58 of 137)
        add.accept(ItemFluidTank.make(ModItems.FLUID_PACK_FULL.get(), ModFluids.HEAVYOIL_VACUUM.getSource(), 1)); // 601 (4713/90)
        add.accept(ItemFluidTank.make(ModItems.FLUID_PACK_FULL.get(), ModFluids.NAPHTHA.getSource(), 1)); // 602 (4713/19)
        add.accept(ItemFluidTank.make(ModItems.FLUID_PACK_FULL.get(), ModFluids.NAPHTHA_DS.getSource(), 1)); // 603 (4713/137)
        add.accept(ItemFluidTank.make(ModItems.FLUID_PACK_FULL.get(), ModFluids.NAPHTHA_CRACK.getSource(), 1)); // 604 (4713/64)
        add.accept(ItemFluidTank.make(ModItems.FLUID_PACK_FULL.get(), ModFluids.NAPHTHA_COKER.getSource(), 1)); // 605 (4713/105)
        add.accept(ItemFluidTank.make(ModItems.FLUID_PACK_FULL.get(), ModFluids.REFORMATE.getSource(), 1)); // 606 (4713/91)
        add.accept(ItemFluidTank.make(ModItems.FLUID_PACK_FULL.get(), ModFluids.LIGHTOIL.getSource(), 1)); // 607 (4713/21)
        add.accept(ItemFluidTank.make(ModItems.FLUID_PACK_FULL.get(), ModFluids.LIGHTOIL_DS.getSource(), 1)); // 608 (4713/138)
        add.accept(ItemFluidTank.make(ModItems.FLUID_PACK_FULL.get(), ModFluids.LIGHTOIL_CRACK.getSource(), 1)); // 609 (4713/65)
        add.accept(ItemFluidTank.make(ModItems.FLUID_PACK_FULL.get(), ModFluids.LIGHTOIL_VACUUM.getSource(), 1)); // 610 (4713/92)
        // Batch 62: Items 611-620 (Fluid Packs 59-68 of 137)
        add.accept(ItemFluidTank.make(ModItems.FLUID_PACK_FULL.get(), ModFluids.BITUMEN.getSource(), 1)); // 611 (4713/13)
        add.accept(ItemFluidTank.make(ModItems.FLUID_PACK_FULL.get(), ModFluids.SMEAR.getSource(), 1)); // 612 (4713/14)
        add.accept(ItemFluidTank.make(ModItems.FLUID_PACK_FULL.get(), ModFluids.HEATINGOIL.getSource(), 1)); // 613 (4713/15)
        add.accept(ItemFluidTank.make(ModItems.FLUID_PACK_FULL.get(), ModFluids.HEATINGOIL_VACUUM.getSource(), 1)); // 614 (4713/95)
        add.accept(ItemFluidTank.make(ModItems.FLUID_PACK_FULL.get(), ModFluids.RECLAIMED.getSource(), 1)); // 615 (4713/16)
        add.accept(ItemFluidTank.make(ModItems.FLUID_PACK_FULL.get(), ModFluids.LUBRICANT.getSource(), 1)); // 616 (4713/18)
        add.accept(ItemFluidTank.make(ModItems.FLUID_PACK_FULL.get(), ModFluids.FLUE.getSource(), 1)); // 617 (4713/155)
        add.accept(ItemFluidTank.make(ModItems.FLUID_PACK_FULL.get(), ModFluids.GAS.getSource(), 1)); // 618 (4713/23)
        add.accept(ItemFluidTank.make(ModItems.FLUID_PACK_FULL.get(), ModFluids.GAS_COKER.getSource(), 1)); // 619 (4713/106)
        add.accept(ItemFluidTank.make(ModItems.FLUID_PACK_FULL.get(), ModFluids.PETROLEUM.getSource(), 1)); // 620 (4713/24)
        // Batch 63: Items 621-630 (Fluid Packs 69-78 of 137)
        add.accept(ItemFluidTank.make(ModItems.FLUID_PACK_FULL.get(), ModFluids.SOURGAS.getSource(), 1)); // 621 (4713/93)
        add.accept(ItemFluidTank.make(ModItems.FLUID_PACK_FULL.get(), ModFluids.LPG.getSource(), 1)); // 622 (4713/25)
        add.accept(ItemFluidTank.make(ModItems.FLUID_PACK_FULL.get(), ModFluids.SYNGAS.getSource(), 1)); // 623 (4713/86)
        add.accept(ItemFluidTank.make(ModItems.FLUID_PACK_FULL.get(), ModFluids.OXYHYDROGEN.getSource(), 1)); // 624 (4713/87)
        add.accept(ItemFluidTank.make(ModItems.FLUID_PACK_FULL.get(), ModFluids.AROMATICS.getSource(), 1)); // 625 (4713/67)
        add.accept(ItemFluidTank.make(ModItems.FLUID_PACK_FULL.get(), ModFluids.UNSATURATEDS.getSource(), 1)); // 626 (4713/68)
        add.accept(ItemFluidTank.make(ModItems.FLUID_PACK_FULL.get(), ModFluids.XYLENE.getSource(), 1)); // 627 (4713/94)
        add.accept(ItemFluidTank.make(ModItems.FLUID_PACK_FULL.get(), ModFluids.REFORMGAS.getSource(), 1)); // 628 (4713/99)
        add.accept(ItemFluidTank.make(ModItems.FLUID_PACK_FULL.get(), ModFluids.DIESEL.getSource(), 1)); // 629 (4713/20)
        add.accept(ItemFluidTank.make(ModItems.FLUID_PACK_FULL.get(), ModFluids.DIESEL_REFORM.getSource(), 1)); // 630 (4713/96)
        // Batch 64: Items 631-640 (Fluid Packs 79-88 of 137)
        add.accept(ItemFluidTank.make(ModItems.FLUID_PACK_FULL.get(), ModFluids.DIESEL_CRACK.getSource(), 1)); // 631 (4713/66)
        add.accept(ItemFluidTank.make(ModItems.FLUID_PACK_FULL.get(), ModFluids.DIESEL_CRACK_REFORM.getSource(), 1)); // 632 (4713/97)
        add.accept(ItemFluidTank.make(ModItems.FLUID_PACK_FULL.get(), ModFluids.KEROSENE.getSource(), 1)); // 633 (4713/22)
        add.accept(ItemFluidTank.make(ModItems.FLUID_PACK_FULL.get(), ModFluids.KEROSENE_REFORM.getSource(), 1)); // 634 (4713/98)
        add.accept(ItemFluidTank.make(ModItems.FLUID_PACK_FULL.get(), ModFluids.PETROIL.getSource(), 1)); // 635 (4713/17)
        add.accept(ItemFluidTank.make(ModItems.FLUID_PACK_FULL.get(), ModFluids.PETROIL_LEADED.getSource(), 1)); // 636 (4713/72)
        add.accept(ItemFluidTank.make(ModItems.FLUID_PACK_FULL.get(), ModFluids.GASOLINE.getSource(), 1)); // 637 (4713/46)
        add.accept(ItemFluidTank.make(ModItems.FLUID_PACK_FULL.get(), ModFluids.GASOLINE_LEADED.getSource(), 1)); // 638 (4713/73)
        add.accept(ItemFluidTank.make(ModItems.FLUID_PACK_FULL.get(), ModFluids.COALGAS.getSource(), 1)); // 639 (4713/47)
        add.accept(ItemFluidTank.make(ModItems.FLUID_PACK_FULL.get(), ModFluids.COALGAS_LEADED.getSource(), 1)); // 640 (4713/74)
        // Batch 65: Items 641-650 (Fluid Packs 89-98 of 137)
        add.accept(ItemFluidTank.make(ModItems.FLUID_PACK_FULL.get(), ModFluids.COALCREOSOTE.getSource(), 1)); // 641 (4713/80)
        add.accept(ItemFluidTank.make(ModItems.FLUID_PACK_FULL.get(), ModFluids.WOODOIL.getSource(), 1)); // 642 (4713/79)
        add.accept(ItemFluidTank.make(ModItems.FLUID_PACK_FULL.get(), ModFluids.BIOGAS.getSource(), 1)); // 643 (4713/26)
        add.accept(ItemFluidTank.make(ModItems.FLUID_PACK_FULL.get(), ModFluids.BIOFUEL.getSource(), 1)); // 644 (4713/27)
        add.accept(ItemFluidTank.make(ModItems.FLUID_PACK_FULL.get(), ModFluids.ETHANOL.getSource(), 1)); // 645 (4713/59)
        add.accept(ItemFluidTank.make(ModItems.FLUID_PACK_FULL.get(), ModFluids.FISHOIL.getSource(), 1)); // 646 (4713/110)
        add.accept(ItemFluidTank.make(ModItems.FLUID_PACK_FULL.get(), ModFluids.SUNFLOWEROIL.getSource(), 1)); // 647 (4713/111)
        add.accept(ItemFluidTank.make(ModItems.FLUID_PACK_FULL.get(), ModFluids.NITAN.getSource(), 1)); // 648 (4713/28)
        add.accept(ItemFluidTank.make(ModItems.FLUID_PACK_FULL.get(), ModFluids.DHC.getSource(), 1)); // 649 (4713/153)
        add.accept(ItemFluidTank.make(ModItems.FLUID_PACK_FULL.get(), ModFluids.BALEFIRE.getSource(), 1)); // 650 (4713/41)
        // Batch 66: Items 651-660 (Fluid Packs 99-108 of 137)
        add.accept(ItemFluidTank.make(ModItems.FLUID_PACK_FULL.get(), ModFluids.SALIENT.getSource(), 1)); // 651 (4713/69)
        add.accept(ItemFluidTank.make(ModItems.FLUID_PACK_FULL.get(), ModFluids.SEEDSLURRY.getSource(), 1)); // 652 (4713/81)
        add.accept(ItemFluidTank.make(ModItems.FLUID_PACK_FULL.get(), ModFluids.COLLOID.getSource(), 1)); // 653 (4713/100)
        add.accept(ItemFluidTank.make(ModItems.FLUID_PACK_FULL.get(), ModFluids.VITRIOL.getSource(), 1)); // 654 (4713/140)
        add.accept(ItemFluidTank.make(ModItems.FLUID_PACK_FULL.get(), ModFluids.SLOP.getSource(), 1)); // 655 (4713/141)
        add.accept(ItemFluidTank.make(ModItems.FLUID_PACK_FULL.get(), ModFluids.IONGEL.getSource(), 1)); // 656 (4713/103)
        add.accept(ItemFluidTank.make(ModItems.FLUID_PACK_FULL.get(), ModFluids.PEROXIDE.getSource(), 1)); // 657 (4713/35)
        add.accept(ItemFluidTank.make(ModItems.FLUID_PACK_FULL.get(), ModFluids.SULFURIC_ACID.getSource(), 1)); // 658 (4713/75)
        add.accept(ItemFluidTank.make(ModItems.FLUID_PACK_FULL.get(), ModFluids.NITRIC_ACID.getSource(), 1)); // 659 (4713/82)
        add.accept(ItemFluidTank.make(ModItems.FLUID_PACK_FULL.get(), ModFluids.SOLVENT.getSource(), 1)); // 660 (4713/83)
        // Batch 67: Items 661-670 (Fluid Packs 109-118 of 137)
        add.accept(ItemFluidTank.make(ModItems.FLUID_PACK_FULL.get(), ModFluids.RADIOSOLVENT.getSource(), 1)); // 661 (4713/88)
        add.accept(ItemFluidTank.make(ModItems.FLUID_PACK_FULL.get(), ModFluids.SCHRABIDIC.getSource(), 1)); // 662 (4713/32)
        add.accept(ItemFluidTank.make(ModItems.FLUID_PACK_FULL.get(), ModFluids.UF6.getSource(), 1)); // 663 (4713/29)
        add.accept(ItemFluidTank.make(ModItems.FLUID_PACK_FULL.get(), ModFluids.PUF6.getSource(), 1)); // 664 (4713/30)
        add.accept(ItemFluidTank.make(ModItems.FLUID_PACK_FULL.get(), ModFluids.SAS3.getSource(), 1)); // 665 (4713/31)
        add.accept(ItemFluidTank.make(ModItems.FLUID_PACK_FULL.get(), ModFluids.PAIN.getSource(), 1)); // 666 (4713/43)
        add.accept(ItemFluidTank.make(ModItems.FLUID_PACK_FULL.get(), ModFluids.WATZ.getSource(), 1)); // 667 (4713/36)
        add.accept(ItemFluidTank.make(ModItems.FLUID_PACK_FULL.get(), ModFluids.FULLERENE.getSource(), 1)); // 668 (4713/130)
        add.accept(ItemFluidTank.make(ModItems.FLUID_PACK_FULL.get(), ModFluids.EGG.getSource(), 1)); // 669 (4713/107)
        add.accept(ItemFluidTank.make(ModItems.FLUID_PACK_FULL.get(), ModFluids.CHOLESTEROL.getSource(), 1)); // 670 (4713/108)
        // Batch 68: Items 671-680 (Fluid Packs 119-128 of 137)
        add.accept(ItemFluidTank.make(ModItems.FLUID_PACK_FULL.get(), ModFluids.SODIUM_ALUMINATE.getSource(), 1)); // 671 (4713/148)
        add.accept(ItemFluidTank.make(ModItems.FLUID_PACK_FULL.get(), ModFluids.BAUXITE_SOLUTION.getSource(), 1)); // 672 (4713/149)
        add.accept(ItemFluidTank.make(ModItems.FLUID_PACK_FULL.get(), ModFluids.ALUMINA.getSource(), 1)); // 673 (4713/150)
        add.accept(ItemFluidTank.make(ModItems.FLUID_PACK_FULL.get(), ModFluids.TITANIUM_TETRACHLORIDE.getSource(), 1)); // 674 (4713/156)
        add.accept(ItemFluidTank.make(ModItems.FLUID_PACK_FULL.get(), ModFluids.CONCRETE.getSource(), 1)); // 675 (4713/152)
        add.accept(ItemFluidTank.make(ModItems.FLUID_PACK_FULL.get(), ModFluids.FRACKSOL.getSource(), 1)); // 676 (4713/49)
        add.accept(ItemFluidTank.make(ModItems.FLUID_PACK_FULL.get(), ModFluids.LYE.getSource(), 1)); // 677 (4713/147)
        add.accept(ItemFluidTank.make(ModItems.FLUID_PACK_FULL.get(), ModFluids.PHOSGENE.getSource(), 1)); // 678 (4713/101)
        add.accept(ItemFluidTank.make(ModItems.FLUID_PACK_FULL.get(), ModFluids.MUSTARDGAS.getSource(), 1)); // 679 (4713/102)
        add.accept(ItemFluidTank.make(ModItems.FLUID_PACK_FULL.get(), ModFluids.ESTRADIOL.getSource(), 1)); // 680 (4713/109)
        // Batch 69: Items 681-690 (Fluid Packs 129-136 of 137, Item 689 Custom Demo Pack, Item 690 Pipette)
        add.accept(ItemFluidTank.make(ModItems.FLUID_PACK_FULL.get(), ModFluids.NITROGLYCERIN.getSource(), 1)); // 681 (4713/112)
        add.accept(ItemFluidTank.make(ModItems.FLUID_PACK_FULL.get(), ModFluids.AMAT.getSource(), 1)); // 682 (4713/33)
        add.accept(ItemFluidTank.make(ModItems.FLUID_PACK_FULL.get(), ModFluids.ASCHRAB.getSource(), 1)); // 683 (4713/34)
        add.accept(ItemFluidTank.make(ModItems.FLUID_PACK_FULL.get(), ModFluids.XPJUICE.getSource(), 1)); // 684 (4713/70)
        add.accept(ItemFluidTank.make(ModItems.FLUID_PACK_FULL.get(), ModFluids.ENDERJUICE.getSource(), 1)); // 685 (4713/71)
        add.accept(ItemFluidTank.make(ModItems.FLUID_PACK_FULL.get(), ModFluids.STELLAR_FLUX.getSource(), 1)); // 686 (4713/139)
        add.accept(ItemFluidTank.make(ModItems.FLUID_PACK_FULL.get(), ModFluids.PHEROMONE.getSource(), 1)); // 687 (4713/131)
        add.accept(ItemFluidTank.make(ModItems.FLUID_PACK_FULL.get(), ModFluids.PHEROMONE_M.getSource(), 1)); // 688 (4713/132)
        add.accept(ItemFluidTank.make(ModItems.FLUID_PACK_FULL.get(), ModFluids.CUSTOM_DEMO.getSource(), 1)); // 689 (4713/1000)
        add.accept(new ItemStack(ModItems.PIPETTE.get())); // 690 (4714/0)
        // Batch 70: Items 691-700 (Pipettes, Siphon, Battery Packs 1-7)
        add.accept(new ItemStack(ModItems.PIPETTE_BORON.get())); // 691 (4715/0)
        add.accept(new ItemStack(ModItems.PIPETTE_LABORATORY.get())); // 692 (4716/0)
        add.accept(new ItemStack(ModItems.SIPHON.get())); // 693 (4717/0)
        add.accept(com.hbm_m.item.fekal_electric.ItemBatteryPack.makeEmptyBattery(new ItemStack(ModItems.BATTERY_PACK_BATTERY_REDSTONE.get()))); // 694 (4720/0 empty)
        add.accept(com.hbm_m.item.fekal_electric.ItemBatteryPack.makeFullBattery(new ItemStack(ModItems.BATTERY_PACK_BATTERY_REDSTONE.get()))); // 695 (4720/0 full)
        add.accept(com.hbm_m.item.fekal_electric.ItemBatteryPack.makeEmptyBattery(new ItemStack(ModItems.BATTERY_PACK_BATTERY_LEAD.get()))); // 696 (4720/1 empty)
        add.accept(com.hbm_m.item.fekal_electric.ItemBatteryPack.makeFullBattery(new ItemStack(ModItems.BATTERY_PACK_BATTERY_LEAD.get()))); // 697 (4720/1 full)
        add.accept(com.hbm_m.item.fekal_electric.ItemBatteryPack.makeEmptyBattery(new ItemStack(ModItems.BATTERY_PACK_BATTERY_LITHIUM.get()))); // 698 (4720/2 empty)
        add.accept(com.hbm_m.item.fekal_electric.ItemBatteryPack.makeFullBattery(new ItemStack(ModItems.BATTERY_PACK_BATTERY_LITHIUM.get()))); // 699 (4720/2 full)
        add.accept(com.hbm_m.item.fekal_electric.ItemBatteryPack.makeEmptyBattery(new ItemStack(ModItems.BATTERY_PACK_BATTERY_SODIUM.get()))); // 700 (4720/3 empty)

        // --- CHUNK 8: Items 701-800 (worker_m5_control_c8) ---
        // Batch 71: Items 701-710 (Battery Packs 8-17: Sodium, Schrabidium, Quantum, Copper, Gold, Niobium)
        add.accept(com.hbm_m.item.fekal_electric.ItemBatteryPack.makeFullBattery(new ItemStack(ModItems.BATTERY_PACK_BATTERY_SODIUM.get()))); // 701 (4720/3 full)
        add.accept(com.hbm_m.item.fekal_electric.ItemBatteryPack.makeEmptyBattery(new ItemStack(ModItems.BATTERY_PACK_BATTERY_SCHRABIDIUM.get()))); // 702 (4720/4 empty)
        add.accept(com.hbm_m.item.fekal_electric.ItemBatteryPack.makeFullBattery(new ItemStack(ModItems.BATTERY_PACK_BATTERY_SCHRABIDIUM.get()))); // 703 (4720/4 full)
        add.accept(com.hbm_m.item.fekal_electric.ItemBatteryPack.makeEmptyBattery(new ItemStack(ModItems.BATTERY_PACK_BATTERY_QUANTUM.get()))); // 704 (4720/5 empty)
        add.accept(com.hbm_m.item.fekal_electric.ItemBatteryPack.makeFullBattery(new ItemStack(ModItems.BATTERY_PACK_BATTERY_QUANTUM.get()))); // 705 (4720/5 full)
        add.accept(com.hbm_m.item.fekal_electric.ItemBatteryPack.makeEmptyBattery(new ItemStack(ModItems.BATTERY_PACK_CAPACITOR_COPPER.get()))); // 706 (4720/6 empty)
        add.accept(com.hbm_m.item.fekal_electric.ItemBatteryPack.makeFullBattery(new ItemStack(ModItems.BATTERY_PACK_CAPACITOR_COPPER.get()))); // 707 (4720/6 full)
        add.accept(com.hbm_m.item.fekal_electric.ItemBatteryPack.makeEmptyBattery(new ItemStack(ModItems.BATTERY_PACK_CAPACITOR_GOLD.get()))); // 708 (4720/7 empty)
        add.accept(com.hbm_m.item.fekal_electric.ItemBatteryPack.makeFullBattery(new ItemStack(ModItems.BATTERY_PACK_CAPACITOR_GOLD.get()))); // 709 (4720/7 full)
        add.accept(com.hbm_m.item.fekal_electric.ItemBatteryPack.makeEmptyBattery(new ItemStack(ModItems.BATTERY_PACK_CAPACITOR_NIOBIUM.get()))); // 710 (4720/8 empty)

        // Batch 72: Items 711-720 (Battery Packs 18-24: Niobium, Tantalum, Bismuth, Spark; Self-Charging Batteries 1-3)
        add.accept(com.hbm_m.item.fekal_electric.ItemBatteryPack.makeFullBattery(new ItemStack(ModItems.BATTERY_PACK_CAPACITOR_NIOBIUM.get()))); // 711 (4720/8 full)
        add.accept(com.hbm_m.item.fekal_electric.ItemBatteryPack.makeEmptyBattery(new ItemStack(ModItems.BATTERY_PACK_CAPACITOR_TANTALUM.get()))); // 712 (4720/9 empty)
        add.accept(com.hbm_m.item.fekal_electric.ItemBatteryPack.makeFullBattery(new ItemStack(ModItems.BATTERY_PACK_CAPACITOR_TANTALUM.get()))); // 713 (4720/9 full)
        add.accept(com.hbm_m.item.fekal_electric.ItemBatteryPack.makeEmptyBattery(new ItemStack(ModItems.BATTERY_PACK_CAPACITOR_BISMUTH.get()))); // 714 (4720/10 empty)
        add.accept(com.hbm_m.item.fekal_electric.ItemBatteryPack.makeFullBattery(new ItemStack(ModItems.BATTERY_PACK_CAPACITOR_BISMUTH.get()))); // 715 (4720/10 full)
        add.accept(com.hbm_m.item.fekal_electric.ItemBatteryPack.makeEmptyBattery(new ItemStack(ModItems.BATTERY_PACK_CAPACITOR_SPARK.get()))); // 716 (4720/11 empty)
        add.accept(com.hbm_m.item.fekal_electric.ItemBatteryPack.makeFullBattery(new ItemStack(ModItems.BATTERY_PACK_CAPACITOR_SPARK.get()))); // 717 (4720/11 full)
        add.accept(new ItemStack(ModItems.BATTERY_SC_EMPTY.get())); // 718 (4721/0)
        add.accept(new ItemStack(ModItems.BATTERY_SC_WASTE.get())); // 719 (4721/1)
        add.accept(new ItemStack(ModItems.BATTERY_SC_RA226.get())); // 720 (4721/2)

        // Batch 73: Items 721-730 (Self-Charging Batteries 4-10; Creative Battery; Electronium Cube empty & full)
        add.accept(new ItemStack(ModItems.BATTERY_SC_TC99.get())); // 721 (4721/3)
        add.accept(new ItemStack(ModItems.BATTERY_SC_CO60.get())); // 722 (4721/4)
        add.accept(new ItemStack(ModItems.BATTERY_SC_PU238.get())); // 723 (4721/5)
        add.accept(new ItemStack(ModItems.BATTERY_SC_PO210.get())); // 724 (4721/6)
        add.accept(new ItemStack(ModItems.BATTERY_SC_AU198.get())); // 725 (4721/7)
        add.accept(new ItemStack(ModItems.BATTERY_SC_PB209.get())); // 726 (4721/8)
        add.accept(new ItemStack(ModItems.BATTERY_SC_AM241.get())); // 727 (4721/9)
        add.accept(new ItemStack(ModItems.CREATIVE_BATTERY.get())); // 728 (4722/0)
        add.accept(new ItemStack(ModItems.CUBE_POWER.get())); // 729 (4723/0 empty)
        add.accept(makeChargedBattery(ModItems.CUBE_POWER.get())); // 730 (4723/0 full)

        // Batch 74: Items 731-740 (Potato Battery, PotatOS, Suit Battery, Fusion Core; Stamps 1-6)
        add.accept(makeChargedBattery(ModItems.BATTERY_POTATO.get())); // 731 (4724/0)
        add.accept(makeChargedBattery(ModItems.BATTERY_POTATOS.get())); // 732 (4725/0)
        add.accept(new ItemStack(ModItems.HEV_BATTERY.get())); // 733 (4726/0)
        add.accept(new ItemStack(ModItems.FUSION_CORE.get())); // 734 (4727/0)
        add.accept(new ItemStack(ModItems.STAMP_STONE_FLAT.get())); // 735 (4732/0)
        add.accept(new ItemStack(ModItems.STAMP_STONE_PLATE.get())); // 736 (4733/0)
        add.accept(new ItemStack(ModItems.STAMP_STONE_WIRE.get())); // 737 (4734/0)
        add.accept(new ItemStack(ModItems.STAMP_STONE_CIRCUIT.get())); // 738 (4735/0)
        add.accept(new ItemStack(ModItems.STAMP_IRON_FLAT.get())); // 739 (4736/0)
        add.accept(new ItemStack(ModItems.STAMP_IRON_PLATE.get())); // 740 (4737/0)

        // Batch 75: Items 741-750 (Stamps 7-16: Iron Wire/Circuit; Steel Flat/Plate/Wire/Circuit; Titanium Flat/Plate/Wire/Circuit)
        add.accept(new ItemStack(ModItems.STAMP_IRON_WIRE.get())); // 741 (4738/0)
        add.accept(new ItemStack(ModItems.STAMP_IRON_CIRCUIT.get())); // 742 (4739/0)
        add.accept(new ItemStack(ModItems.STAMP_STEEL_FLAT.get())); // 743 (4740/0)
        add.accept(new ItemStack(ModItems.STAMP_STEEL_PLATE.get())); // 744 (4741/0)
        add.accept(new ItemStack(ModItems.STAMP_STEEL_WIRE.get())); // 745 (4742/0)
        add.accept(new ItemStack(ModItems.STAMP_STEEL_CIRCUIT.get())); // 746 (4743/0)
        add.accept(new ItemStack(ModItems.STAMP_TITANIUM_FLAT.get())); // 747 (4744/0)
        add.accept(new ItemStack(ModItems.STAMP_TITANIUM_PLATE.get())); // 748 (4745/0)
        add.accept(new ItemStack(ModItems.STAMP_TITANIUM_WIRE.get())); // 749 (4746/0)
        add.accept(new ItemStack(ModItems.STAMP_TITANIUM_CIRCUIT.get())); // 750 (4747/0)

        // Batch 76: Items 751-760 (Stamps 17-26: Obsidian Flat/Plate/Wire/Circuit; Desh Flat/Plate/Wire/Circuit; .357/.44 Magnum Stamps)
        add.accept(new ItemStack(ModItems.STAMP_OBSIDIAN_FLAT.get())); // 751 (4748/0)
        add.accept(new ItemStack(ModItems.STAMP_OBSIDIAN_PLATE.get())); // 752 (4749/0)
        add.accept(new ItemStack(ModItems.STAMP_OBSIDIAN_WIRE.get())); // 753 (4750/0)
        add.accept(new ItemStack(ModItems.STAMP_OBSIDIAN_CIRCUIT.get())); // 754 (4751/0)
        add.accept(new ItemStack(ModItems.STAMP_DESH_FLAT.get())); // 755 (4752/0)
        add.accept(new ItemStack(ModItems.STAMP_DESH_PLATE.get())); // 756 (4753/0)
        add.accept(new ItemStack(ModItems.STAMP_DESH_WIRE.get())); // 757 (4754/0)
        add.accept(new ItemStack(ModItems.STAMP_DESH_CIRCUIT.get())); // 758 (4755/0)
        add.accept(new ItemStack(ModItems.STAMP_357.get())); // 759 (4756/0)
        add.accept(new ItemStack(ModItems.STAMP_44.get())); // 760 (4757/0)

        // Batch 77: Items 761-770 (Stamps 27-32: Small/Large Caliber, Desh Caliber Stamps; Blank Mold; Foundry Molds 1-3)
        add.accept(new ItemStack(ModItems.STAMP_9.get())); // 761 (4758/0)
        add.accept(new ItemStack(ModItems.STAMP_50.get())); // 762 (4759/0)
        add.accept(new ItemStack(ModItems.STAMP_DESH_357.get())); // 763 (4760/0)
        add.accept(new ItemStack(ModItems.STAMP_DESH_44.get())); // 764 (4761/0)
        add.accept(new ItemStack(ModItems.STAMP_DESH_9.get())); // 765 (4762/0)
        add.accept(new ItemStack(ModItems.STAMP_DESH_50.get())); // 766 (4763/0)
        add.accept(new ItemStack(ModItems.MOLD_BASE.get())); // 767 (4765/0)
        add.accept(new ItemStack(ModItems.MOLD_NUGGET.get())); // 768 (4766/0)
        add.accept(new ItemStack(ModItems.MOLD_BILLET.get())); // 769 (4766/1)
        add.accept(new ItemStack(ModItems.MOLD_INGOT.get())); // 770 (4766/2)

        // Batch 78: Items 771-780 (Foundry Molds 4-13: Plate, Wire, Cast Plate, Dense Wire, Blade, Blades, Stamp, Shell, Pipe, Ingots)
        add.accept(new ItemStack(ModItems.MOLD_PLATE.get())); // 771 (4766/3)
        add.accept(new ItemStack(ModItems.MOLD_WIRE.get())); // 772 (4766/4)
        add.accept(new ItemStack(ModItems.MOLD_PLATE_CAST.get())); // 773 (4766/19)
        add.accept(new ItemStack(ModItems.MOLD_WIRE_DENSE.get())); // 774 (4766/20)
        add.accept(new ItemStack(ModItems.MOLD_BLADE.get())); // 775 (4766/5)
        add.accept(new ItemStack(ModItems.MOLD_BLADES.get())); // 776 (4766/6)
        add.accept(new ItemStack(ModItems.MOLD_STAMP.get())); // 777 (4766/7)
        add.accept(new ItemStack(ModItems.MOLD_SHELL.get())); // 778 (4766/8)
        add.accept(new ItemStack(ModItems.MOLD_PIPE.get())); // 779 (4766/9)
        add.accept(new ItemStack(ModItems.MOLD_INGOTS.get())); // 780 (4766/10)

        // Batch 79: Items 781-790 (Foundry Molds 14-23: Plates, Cast Plates, Dense Wires, Block, Casing 9mm, Casing .50, Light Barrel, Heavy Barrel, Light Receiver, Heavy Receiver)
        add.accept(new ItemStack(ModItems.MOLD_PLATES.get())); // 781 (4766/11)
        add.accept(new ItemStack(ModItems.MOLD_PLATES_CAST.get())); // 782 (4766/13)
        add.accept(new ItemStack(ModItems.MOLD_WIRES_DENSE.get())); // 783 (4766/21)
        add.accept(new ItemStack(ModItems.MOLD_BLOCK.get())); // 784 (4766/12)
        add.accept(new ItemStack(ModItems.MOLD_C9.get())); // 785 (4766/16)
        add.accept(new ItemStack(ModItems.MOLD_C50.get())); // 786 (4766/17)
        add.accept(new ItemStack(ModItems.MOLD_BARREL_LIGHT.get())); // 787 (4766/22)
        add.accept(new ItemStack(ModItems.MOLD_BARREL_HEAVY.get())); // 788 (4766/23)
        add.accept(new ItemStack(ModItems.MOLD_RECEIVER_LIGHT.get())); // 789 (4766/24)
        add.accept(new ItemStack(ModItems.MOLD_RECEIVER_HEAVY.get())); // 790 (4766/25)

        // Batch 80: Items 791-800 (Foundry Molds 24-27: Mechanism, Stock, Grip, Demoshape; Speed Upgrades Mk.I-III, Effectiveness Upgrades Mk.I-III)
        add.accept(new ItemStack(ModItems.MOLD_MECHANISM.get())); // 791 (4766/26)
        add.accept(new ItemStack(ModItems.MOLD_STOCK.get())); // 792 (4766/27)
        add.accept(new ItemStack(ModItems.MOLD_GRIP.get())); // 793 (4766/28)
        add.accept(new ItemStack(ModItems.MOLD_DEMOSHAPE.get())); // 794 (4766/100)
        add.accept(new ItemStack(ModItems.UPGRADE_SPEED_1.get())); // 795 (4770/0)
        add.accept(new ItemStack(ModItems.UPGRADE_SPEED_2.get())); // 796 (4771/0)
        add.accept(new ItemStack(ModItems.UPGRADE_SPEED_3.get())); // 797 (4772/0)
        add.accept(new ItemStack(ModItems.UPGRADE_EFFECT_1.get())); // 798 (4773/0)
        add.accept(new ItemStack(ModItems.UPGRADE_EFFECT_2.get())); // 799 (4774/0)
        add.accept(new ItemStack(ModItems.UPGRADE_EFFECT_3.get())); // 800 (4775/0)

        // --- CHUNK 9: Items 801-900 (worker_m5_control_c9) ---
        // 801-810: Batch 81: Power Saving Upgrades Mk.I-III, Fortune Upgrades Mk.I-III, Afterburner Upgrades Mk.I-III, Overdrive Upgrade Mk.I
        add.accept(new ItemStack(ModItems.UPGRADE_POWER_1.get())); // 801 (4776/0)
        add.accept(new ItemStack(ModItems.UPGRADE_POWER_2.get())); // 802 (4777/0)
        add.accept(new ItemStack(ModItems.UPGRADE_POWER_3.get())); // 803 (4778/0)
        add.accept(new ItemStack(ModItems.UPGRADE_FORTUNE_1.get())); // 804 (4779/0)
        add.accept(new ItemStack(ModItems.UPGRADE_FORTUNE_2.get())); // 805 (4780/0)
        add.accept(new ItemStack(ModItems.UPGRADE_FORTUNE_3.get())); // 806 (4781/0)
        add.accept(new ItemStack(ModItems.UPGRADE_AFTERBURN_1.get())); // 807 (4782/0)
        add.accept(new ItemStack(ModItems.UPGRADE_AFTERBURN_2.get())); // 808 (4783/0)
        add.accept(new ItemStack(ModItems.UPGRADE_AFTERBURN_3.get())); // 809 (4784/0)
        add.accept(new ItemStack(ModItems.UPGRADE_OVERDRIVE_1.get())); // 810 (4785/0)
        // 811-820: Batch 82: Overdrive Upgrades Mk.II-III, Emitter Upgrades (Radius, Health), Machine Upgrades (Smelter, Shredder, Centrifuge, Crystallizer, Nullifier, Screm)
        add.accept(new ItemStack(ModItems.UPGRADE_OVERDRIVE_2.get())); // 811 (4786/0)
        add.accept(new ItemStack(ModItems.UPGRADE_OVERDRIVE_3.get())); // 812 (4787/0)
        add.accept(new ItemStack(ModItems.UPGRADE_RADIUS.get())); // 813 (4788/0)
        add.accept(new ItemStack(ModItems.UPGRADE_HEALTH.get())); // 814 (4789/0)
        add.accept(new ItemStack(ModItems.UPGRADE_SMELTER.get())); // 815 (4790/0)
        add.accept(new ItemStack(ModItems.UPGRADE_SHREDDER.get())); // 816 (4791/0)
        add.accept(new ItemStack(ModItems.UPGRADE_CENTRIFUGE.get())); // 817 (4792/0)
        add.accept(new ItemStack(ModItems.UPGRADE_CRYSTALLIZER.get())); // 818 (4793/0)
        add.accept(new ItemStack(ModItems.UPGRADE_NULLIFIER.get())); // 819 (4794/0)
        add.accept(new ItemStack(ModItems.UPGRADE_SCREM.get())); // 820 (4795/0)
        // 821-830: Batch 83: Gas Centrifuge, 5G Upgrades, Stack Upgrades Mk.I-III, Ejector Upgrades Mk.I-III, Fuse, Screwdriver
        add.accept(new ItemStack(ModItems.UPGRADE_GC_SPEED.get())); // 821 (4796/0)
        add.accept(new ItemStack(ModItems.UPGRADE_5G.get())); // 822 (4797/0)
        add.accept(new ItemStack(ModItems.UPGRADE_STACK_1.get())); // 823 (4798/0)
        add.accept(new ItemStack(ModItems.UPGRADE_STACK_2.get())); // 824 (4798/1)
        add.accept(new ItemStack(ModItems.UPGRADE_STACK_3.get())); // 825 (4798/2)
        add.accept(new ItemStack(ModItems.UPGRADE_EJECTOR_1.get())); // 826 (4799/0)
        add.accept(new ItemStack(ModItems.UPGRADE_EJECTOR_2.get())); // 827 (4799/1)
        add.accept(new ItemStack(ModItems.UPGRADE_EJECTOR_3.get())); // 828 (4799/2)
        add.accept(new ItemStack(ModItems.FUSE.get())); // 829 (4804/0)
        add.accept(new ItemStack(ModItems.SCREWDRIVER.get())); // 830 (4805/0)
        // 831-840: Batch 84: Tools (Desh Screwdriver, Hand Drills, Glassware, Torches, Rivet Gun), Graphite & Lanthanium Arc Electrodes
        add.accept(new ItemStack(ModItems.SCREWDRIVER_DESH.get())); // 831 (4806/0)
        add.accept(new ItemStack(ModItems.HAND_DRILL.get())); // 832 (4807/0)
        add.accept(new ItemStack(ModItems.HAND_DRILL_DESH.get())); // 833 (4808/0)
        add.accept(new ItemStack(ModItems.CHEMISTRY_SET.get())); // 834 (4809/0)
        add.accept(new ItemStack(ModItems.CHEMISTRY_SET_BORON.get())); // 835 (4810/0)
        add.accept(new ItemStack(ModItems.BLOWTORCH.get())); // 836 (4811/0)
        add.accept(new ItemStack(ModItems.ACETYLENE_TORCH.get())); // 837 (4812/0)
        add.accept(new ItemStack(ModItems.BOLTGUN.get())); // 838 (4813/0)
        add.accept(new ItemStack(ModItems.ARC_ELECTRODE.get())); // 839 (4814/0)
        add.accept(new ItemStack(ModItems.ARC_ELECTRODE_LANTHANIUM.get())); // 840 (4814/1)
        // 841-850: Batch 85: Arc Electrodes (Desh, Saturnite), Molten Arc Electrodes (Graphite, Lanthanium, Desh, Saturnite), Dust Boxes (Lithium, Beryllium, Carbon, Copper)
        add.accept(new ItemStack(ModItems.ARC_ELECTRODE_DESH.get())); // 841 (4814/2)
        add.accept(new ItemStack(ModItems.ARC_ELECTRODE_SATURNITE.get())); // 842 (4814/3)
        add.accept(new ItemStack(ModItems.ARC_ELECTRODE_BURNT_GRAPHITE.get())); // 843 (4815/0)
        add.accept(new ItemStack(ModItems.ARC_ELECTRODE_BURNT_LANTHANIUM.get())); // 844 (4815/1)
        add.accept(new ItemStack(ModItems.ARC_ELECTRODE_BURNT_DESH.get())); // 845 (4815/2)
        add.accept(new ItemStack(ModItems.ARC_ELECTRODE_BURNT_SATURNITE.get())); // 846 (4815/3)
        add.accept(new ItemStack(ModItems.PART_LITHIUM.get())); // 847 (4816/0)
        add.accept(new ItemStack(ModItems.PART_BERYLLIUM.get())); // 848 (4817/0)
        add.accept(new ItemStack(ModItems.PART_CARBON.get())); // 849 (4818/0)
        add.accept(new ItemStack(ModItems.PART_COPPER.get())); // 850 (4819/0)
        // 851-860: Batch 86: Dust Box (Plutonium), Laser Crystals (CO2, Bismuth, CMB, Spark, Digamma), Reaction Catalysts (Blank, Aluminium, Beryllium, Caesium)
        add.accept(new ItemStack(ModItems.PART_PLUTONIUM.get())); // 851 (4820/0)
        add.accept(new ItemStack(ModItems.LASER_CRYSTAL_CO2.get())); // 852 (4821/0)
        add.accept(new ItemStack(ModItems.LASER_CRYSTAL_BISMUTH.get())); // 853 (4822/0)
        add.accept(new ItemStack(ModItems.LASER_CRYSTAL_CMB.get())); // 854 (4823/0)
        add.accept(new ItemStack(ModItems.LASER_CRYSTAL_DNT.get())); // 855 (4824/0)
        add.accept(new ItemStack(ModItems.LASER_CRYSTAL_DIGAMMA.get())); // 856 (4825/0)
        add.accept(new ItemStack(ModItems.AMS_CATALYST_BLANK.get())); // 857 (4832/0)
        add.accept(new ItemStack(ModItems.AMS_CATALYST_ALUMINIUM.get())); // 858 (4833/0)
        add.accept(new ItemStack(ModItems.AMS_CATALYST_BERYLLIUM.get())); // 859 (4834/0)
        add.accept(new ItemStack(ModItems.AMS_CATALYST_CAESIUM.get())); // 860 (4835/0)
        // 861-870: Batch 87: Reaction Catalysts (Cerium, Cobalt, Copper, Euphemium, Dineutronium, Iron, Lithium, Niobium, Schrabidium, Strontium)
        add.accept(new ItemStack(ModItems.AMS_CATALYST_CERIUM.get())); // 861 (4836/0)
        add.accept(new ItemStack(ModItems.AMS_CATALYST_COBALT.get())); // 862 (4837/0)
        add.accept(new ItemStack(ModItems.AMS_CATALYST_COPPER.get())); // 863 (4838/0)
        add.accept(new ItemStack(ModItems.AMS_CATALYST_EUPHEMIUM.get())); // 864 (4839/0)
        add.accept(new ItemStack(ModItems.AMS_CATALYST_DINEUTRONIUM.get())); // 865 (4840/0)
        add.accept(new ItemStack(ModItems.AMS_CATALYST_IRON.get())); // 866 (4841/0)
        add.accept(new ItemStack(ModItems.AMS_CATALYST_LITHIUM.get())); // 867 (4842/0)
        add.accept(new ItemStack(ModItems.AMS_CATALYST_NIOBIUM.get())); // 868 (4843/0)
        add.accept(new ItemStack(ModItems.AMS_CATALYST_SCHRABIDIUM.get())); // 869 (4844/0)
        add.accept(new ItemStack(ModItems.AMS_CATALYST_STRONTIUM.get())); // 870 (4845/0)
        // 871-880: Batch 88: Reaction Catalysts (Thorium, Tungsten), Shredder Blades (Steel, Titanium, Desh), Thermoelectric Element, Catalytic Converter, AMS Lens, AMS Cores (Singularity, Wormhole)
        add.accept(new ItemStack(ModItems.AMS_CATALYST_THORIUM.get())); // 871 (4846/0)
        add.accept(new ItemStack(ModItems.AMS_CATALYST_TUNGSTEN.get())); // 872 (4847/0)
        add.accept(new ItemStack(ModItems.BLADES_STEEL.get())); // 873 (4848/0)
        add.accept(new ItemStack(ModItems.BLADES_TITANIUM.get())); // 874 (4849/0)
        add.accept(new ItemStack(ModItems.BLADES_DESH.get())); // 875 (4850/0)
        add.accept(new ItemStack(ModItems.THERMO_ELEMENT.get())); // 876 (4851/0)
        add.accept(new ItemStack(ModItems.CATALYTIC_CONVERTER.get())); // 877 (4852/0)
        add.accept(new ItemStack(ModItems.AMS_LENS.get())); // 878 (4853/0)
        add.accept(new ItemStack(ModItems.AMS_CORE_SING.get())); // 879 (4854/0)
        add.accept(new ItemStack(ModItems.AMS_CORE_WORMHOLE.get())); // 880 (4855/0)
        // 881-890: Batch 89: AMS Core (Eye of Harmony), Fusion Reactor Blankets (Tungsten, Desh, Chlorophyte, Vaporwave), Empty Rod, Breeding Rods (Lithium, Tritium, Cobalt, Cobalt-60)
        add.accept(new ItemStack(ModItems.AMS_CORE_EYEOFHARMONY.get())); // 881 (4856/0)
        add.accept(new ItemStack(ModItems.FUSION_SHIELD_TUNGSTEN.get())); // 882 (4858/0)
        add.accept(new ItemStack(ModItems.FUSION_SHIELD_DESH.get())); // 883 (4859/0)
        add.accept(new ItemStack(ModItems.FUSION_SHIELD_CHLOROPHYTE.get())); // 884 (4860/0)
        add.accept(new ItemStack(ModItems.FUSION_SHIELD_VAPORWAVE.get())); // 885 (4861/0)
        add.accept(new ItemStack(ModItems.ROD_EMPTY.get())); // 886 (4862/0)
        add.accept(new ItemStack(ModItems.ROD.get(BreedingRodType.LITHIUM).get())); // 887 (4863/0)
        add.accept(new ItemStack(ModItems.ROD.get(BreedingRodType.TRITIUM).get())); // 888 (4863/1)
        add.accept(new ItemStack(ModItems.ROD.get(BreedingRodType.CO).get())); // 889 (4863/2)
        add.accept(new ItemStack(ModItems.ROD.get(BreedingRodType.CO60).get())); // 890 (4863/3)
        // 891-900: Batch 90: Breeding Rods (Thorium-232, Thorium Fuel, Uranium-235, Neptunium-237, Uranium-238, Plutonium-238, Plutonium-239, Reactor-Grade Plutonium, Waste, Lead)
        add.accept(new ItemStack(ModItems.ROD.get(BreedingRodType.TH232).get())); // 891 (4863/4)
        add.accept(new ItemStack(ModItems.ROD.get(BreedingRodType.THF).get())); // 892 (4863/5)
        add.accept(new ItemStack(ModItems.ROD.get(BreedingRodType.U235).get())); // 893 (4863/6)
        add.accept(new ItemStack(ModItems.ROD.get(BreedingRodType.NP237).get())); // 894 (4863/7)
        add.accept(new ItemStack(ModItems.ROD.get(BreedingRodType.U238).get())); // 895 (4863/8)
        add.accept(new ItemStack(ModItems.ROD.get(BreedingRodType.PU238).get())); // 896 (4863/9)
        add.accept(new ItemStack(ModItems.ROD.get(BreedingRodType.PU239).get())); // 897 (4863/10)
        add.accept(new ItemStack(ModItems.ROD.get(BreedingRodType.RGP).get())); // 898 (4863/11)
        add.accept(new ItemStack(ModItems.ROD.get(BreedingRodType.WASTE).get())); // 899 (4863/12)
        add.accept(new ItemStack(ModItems.ROD.get(BreedingRodType.LEAD).get())); // 900 (4863/13)

        // --- CHUNK 10: Items 901-1000 (worker_m5_control_c10) ---
        // Batch 91 (Items 901–910: Single Breeding Rods Uranium, Ra-226, Ac-227, Empty Dual Rod, Dual Rods Li, T, Co, Co-60, Th-232, Th Fuel)
        add.accept(new ItemStack(ModItems.ROD.get(BreedingRodType.URANIUM).get())); // 901 (4863/14)
        add.accept(new ItemStack(ModItems.ROD.get(BreedingRodType.RA226).get())); // 902 (4863/15)
        add.accept(new ItemStack(ModItems.ROD.get(BreedingRodType.AC227).get())); // 903 (4863/16)
        add.accept(new ItemStack(ModItems.ROD_DUAL_EMPTY.get())); // 904 (4864/0)
        add.accept(new ItemStack(ModItems.ROD_DUAL.get(BreedingRodType.LITHIUM).get())); // 905 (4865/0)
        add.accept(new ItemStack(ModItems.ROD_DUAL.get(BreedingRodType.TRITIUM).get())); // 906 (4865/1)
        add.accept(new ItemStack(ModItems.ROD_DUAL.get(BreedingRodType.CO).get())); // 907 (4865/2)
        add.accept(new ItemStack(ModItems.ROD_DUAL.get(BreedingRodType.CO60).get())); // 908 (4865/3)
        add.accept(new ItemStack(ModItems.ROD_DUAL.get(BreedingRodType.TH232).get())); // 909 (4865/4)
        add.accept(new ItemStack(ModItems.ROD_DUAL.get(BreedingRodType.THF).get())); // 910 (4865/5)
        // Batch 92 (Items 911–920: Dual Rods U-235, Np-237, U-238, Pu-238, Pu-239, RGP, Waste, Lead, Uranium, Ra-226)
        add.accept(new ItemStack(ModItems.ROD_DUAL.get(BreedingRodType.U235).get())); // 911 (4865/6)
        add.accept(new ItemStack(ModItems.ROD_DUAL.get(BreedingRodType.NP237).get())); // 912 (4865/7)
        add.accept(new ItemStack(ModItems.ROD_DUAL.get(BreedingRodType.U238).get())); // 913 (4865/8)
        add.accept(new ItemStack(ModItems.ROD_DUAL.get(BreedingRodType.PU238).get())); // 914 (4865/9)
        add.accept(new ItemStack(ModItems.ROD_DUAL.get(BreedingRodType.PU239).get())); // 915 (4865/10)
        add.accept(new ItemStack(ModItems.ROD_DUAL.get(BreedingRodType.RGP).get())); // 916 (4865/11)
        add.accept(new ItemStack(ModItems.ROD_DUAL.get(BreedingRodType.WASTE).get())); // 917 (4865/12)
        add.accept(new ItemStack(ModItems.ROD_DUAL.get(BreedingRodType.LEAD).get())); // 918 (4865/13)
        add.accept(new ItemStack(ModItems.ROD_DUAL.get(BreedingRodType.URANIUM).get())); // 919 (4865/14)
        add.accept(new ItemStack(ModItems.ROD_DUAL.get(BreedingRodType.RA226).get())); // 920 (4865/15)
        // Batch 93 (Items 921–930: Dual Rod Ac-227, Empty Quad Rod, Quad Rods Li, T, Co, Co-60, Th-232, Th Fuel, U-235, Np-237)
        add.accept(new ItemStack(ModItems.ROD_DUAL.get(BreedingRodType.AC227).get())); // 921 (4865/16)
        add.accept(new ItemStack(ModItems.ROD_QUAD_EMPTY.get())); // 922 (4866/0)
        add.accept(new ItemStack(ModItems.ROD_QUAD.get(BreedingRodType.LITHIUM).get())); // 923 (4867/0)
        add.accept(new ItemStack(ModItems.ROD_QUAD.get(BreedingRodType.TRITIUM).get())); // 924 (4867/1)
        add.accept(new ItemStack(ModItems.ROD_QUAD.get(BreedingRodType.CO).get())); // 925 (4867/2)
        add.accept(new ItemStack(ModItems.ROD_QUAD.get(BreedingRodType.CO60).get())); // 926 (4867/3)
        add.accept(new ItemStack(ModItems.ROD_QUAD.get(BreedingRodType.TH232).get())); // 927 (4867/4)
        add.accept(new ItemStack(ModItems.ROD_QUAD.get(BreedingRodType.THF).get())); // 928 (4867/5)
        add.accept(new ItemStack(ModItems.ROD_QUAD.get(BreedingRodType.U235).get())); // 929 (4867/6)
        add.accept(new ItemStack(ModItems.ROD_QUAD.get(BreedingRodType.NP237).get())); // 930 (4867/7)
        // Batch 94 (Items 931–940: Quad Rods U-238, Pu-238, Pu-239, RGP, Waste, Lead, Uranium, Ra-226, Ac-227, Empty ZIRNOX Rod)
        add.accept(new ItemStack(ModItems.ROD_QUAD.get(BreedingRodType.U238).get())); // 931 (4867/8)
        add.accept(new ItemStack(ModItems.ROD_QUAD.get(BreedingRodType.PU238).get())); // 932 (4867/9)
        add.accept(new ItemStack(ModItems.ROD_QUAD.get(BreedingRodType.PU239).get())); // 933 (4867/10)
        add.accept(new ItemStack(ModItems.ROD_QUAD.get(BreedingRodType.RGP).get())); // 934 (4867/11)
        add.accept(new ItemStack(ModItems.ROD_QUAD.get(BreedingRodType.WASTE).get())); // 935 (4867/12)
        add.accept(new ItemStack(ModItems.ROD_QUAD.get(BreedingRodType.LEAD).get())); // 936 (4867/13)
        add.accept(new ItemStack(ModItems.ROD_QUAD.get(BreedingRodType.URANIUM).get())); // 937 (4867/14)
        add.accept(new ItemStack(ModItems.ROD_QUAD.get(BreedingRodType.RA226).get())); // 938 (4867/15)
        add.accept(new ItemStack(ModItems.ROD_QUAD.get(BreedingRodType.AC227).get())); // 939 (4867/16)
        add.accept(new ItemStack(ModItems.ROD_ZIRNOX_EMPTY.get())); // 940 (4868/0)
        // Batch 95 (Items 941–950: ZIRNOX Tritium Rod, ZIRNOX Fuel Rods: Natural Uranium, Uranium, Th-232, Th Fuel, MOX, Pu, U-233, U-235, LES)
        add.accept(new ItemStack(ModItems.ROD_ZIRNOX_TRITIUM.get())); // 941 (4869/0)
        add.accept(new ItemStack(ModItems.ROD_ZIRNOX_NATURAL_URANIUM_FUEL.get())); // 942 (4870/0)
        add.accept(new ItemStack(ModItems.ROD_ZIRNOX_URANIUM_FUEL.get())); // 943 (4870/1)
        add.accept(new ItemStack(ModItems.ROD_ZIRNOX_TH232.get())); // 944 (4870/2)
        add.accept(new ItemStack(ModItems.ROD_ZIRNOX_THORIUM_FUEL.get())); // 945 (4870/3)
        add.accept(new ItemStack(ModItems.ROD_ZIRNOX_MOX_FUEL.get())); // 946 (4870/4)
        add.accept(new ItemStack(ModItems.ROD_ZIRNOX_PLUTONIUM_FUEL.get())); // 947 (4870/5)
        add.accept(new ItemStack(ModItems.ROD_ZIRNOX_U233_FUEL.get())); // 948 (4870/6)
        add.accept(new ItemStack(ModItems.ROD_ZIRNOX_U235_FUEL.get())); // 949 (4870/7)
        add.accept(new ItemStack(ModItems.ROD_ZIRNOX_LES_FUEL.get())); // 950 (4870/8)
        // Batch 96 (Items 951–960: ZIRNOX Rods Li, ZFB MOX, Depleted ZIRNOX Rods: Natural Uranium, Uranium, Thorium, MOX, Pu, U-233, U-235, LES)
        add.accept(new ItemStack(ModItems.ROD_ZIRNOX_LITHIUM.get())); // 951 (4870/9)
        add.accept(new ItemStack(ModItems.ROD_ZIRNOX_ZFB_MOX.get())); // 952 (4870/10)
        add.accept(new ItemStack(ModItems.ROD_ZIRNOX_NATURAL_URANIUM_FUEL_DEPLETED.get())); // 953 (4871/0)
        add.accept(new ItemStack(ModItems.ROD_ZIRNOX_URANIUM_FUEL_DEPLETED.get())); // 954 (4872/0)
        add.accept(new ItemStack(ModItems.ROD_ZIRNOX_THORIUM_FUEL_DEPLETED.get())); // 955 (4873/0)
        add.accept(new ItemStack(ModItems.ROD_ZIRNOX_MOX_FUEL_DEPLETED.get())); // 956 (4874/0)
        add.accept(new ItemStack(ModItems.ROD_ZIRNOX_PLUTONIUM_FUEL_DEPLETED.get())); // 957 (4875/0)
        add.accept(new ItemStack(ModItems.ROD_ZIRNOX_U233_FUEL_DEPLETED.get())); // 958 (4876/0)
        add.accept(new ItemStack(ModItems.ROD_ZIRNOX_U235_FUEL_DEPLETED.get())); // 959 (4877/0)
        add.accept(new ItemStack(ModItems.ROD_ZIRNOX_LES_FUEL_DEPLETED.get())); // 960 (4878/0)
        // Batch 97 (Items 961–970: Depleted ZIRNOX ZFB MOX Rod, Chicago Pile Rods: Ra226Be, Po210Be, Zr, NU, Pu-239, RGP, Waste, Thorium, Thorium Fuel)
        add.accept(new ItemStack(ModItems.ROD_ZIRNOX_ZFB_MOX_DEPLETED.get())); // 961 (4879/0)
        add.accept(new ItemStack(ModItems.PILE_ROD_RA226BE.get())); // 962 (4903/0)
        add.accept(new ItemStack(ModItems.PILE_ROD_PO210BE.get())); // 963 (4903/1)
        add.accept(new ItemStack(ModItems.PILE_ROD_ZR.get())); // 964 (4903/2)
        add.accept(new ItemStack(ModItems.PILE_ROD_NU.get())); // 965 (4903/3)
        add.accept(new ItemStack(ModItems.PILE_ROD_MK2_PU239.get())); // 966 (4903/4)
        add.accept(new ItemStack(ModItems.PILE_ROD_RGP.get())); // 967 (4903/5)
        add.accept(new ItemStack(ModItems.PILE_ROD_WASTE.get())); // 968 (4903/6)
        add.accept(new ItemStack(ModItems.PILE_ROD_THORIUM.get())); // 969 (4903/7)
        add.accept(new ItemStack(ModItems.PILE_ROD_THORIUM_FUEL.get())); // 970 (4903/8)
        // Batch 98 (Items 971–980: PWR Fuel Rods: MEU, HEU-233, HEU-235, MEN, HEN-237, MOX, MEP, HEP-239, HEP-241, MEA)
        add.accept(new ItemStack(ModItems.PWR_FUEL_MEU.get())); // 971 (4911/0)
        add.accept(new ItemStack(ModItems.PWR_FUEL_HEU233.get())); // 972 (4911/1)
        add.accept(new ItemStack(ModItems.PWR_FUEL_HEU235.get())); // 973 (4911/2)
        add.accept(new ItemStack(ModItems.PWR_FUEL_MEN.get())); // 974 (4911/3)
        add.accept(new ItemStack(ModItems.PWR_FUEL_HEN237.get())); // 975 (4911/4)
        add.accept(new ItemStack(ModItems.PWR_FUEL_MOX.get())); // 976 (4911/5)
        add.accept(new ItemStack(ModItems.PWR_FUEL_MEP.get())); // 977 (4911/6)
        add.accept(new ItemStack(ModItems.PWR_FUEL_HEP239.get())); // 978 (4911/7)
        add.accept(new ItemStack(ModItems.PWR_FUEL_HEP241.get())); // 979 (4911/8)
        add.accept(new ItemStack(ModItems.PWR_FUEL_MEA.get())); // 980 (4911/9)
        // Batch 99 (Items 981–990: PWR Fuel Rods: HEA-242, HES-326, HES-327, BFB AM, BFB Pu-241; Hot PWR Fuel Rods: MEU, HEU-233, HEU-235, MEN, HEN-237)
        add.accept(new ItemStack(ModItems.PWR_FUEL_HEA242.get())); // 981 (4911/10)
        add.accept(new ItemStack(ModItems.PWR_FUEL_HES326.get())); // 982 (4911/11)
        add.accept(new ItemStack(ModItems.PWR_FUEL_HES327.get())); // 983 (4911/12)
        add.accept(new ItemStack(ModItems.PWR_FUEL_BFB_AM_MIX.get())); // 984 (4911/13)
        add.accept(new ItemStack(ModItems.PWR_FUEL_BFB_PU241.get())); // 985 (4911/14)
        add.accept(new ItemStack(ModItems.PWR_FUEL_MEU_HOT.get())); // 986 (4912/0)
        add.accept(new ItemStack(ModItems.PWR_FUEL_HEU233_HOT.get())); // 987 (4912/1)
        add.accept(new ItemStack(ModItems.PWR_FUEL_HEU235_HOT.get())); // 988 (4912/2)
        add.accept(new ItemStack(ModItems.PWR_FUEL_MEN_HOT.get())); // 989 (4912/3)
        add.accept(new ItemStack(ModItems.PWR_FUEL_HEN237_HOT.get())); // 990 (4912/4)
        // Batch 100 (Items 991–1000: Hot PWR Fuel Rods: MOX, MEP, HEP-239, HEP-241, MEA, HEA-242, HES-326, HES-327, BFB AM, BFB Pu-241)
        add.accept(new ItemStack(ModItems.PWR_FUEL_MOX_HOT.get())); // 991 (4912/5)
        add.accept(new ItemStack(ModItems.PWR_FUEL_MEP_HOT.get())); // 992 (4912/6)
        add.accept(new ItemStack(ModItems.PWR_FUEL_HEP239_HOT.get())); // 993 (4912/7)
        add.accept(new ItemStack(ModItems.PWR_FUEL_HEP241_HOT.get())); // 994 (4912/8)
        add.accept(new ItemStack(ModItems.PWR_FUEL_MEA_HOT.get())); // 995 (4912/9)
        add.accept(new ItemStack(ModItems.PWR_FUEL_HEA242_HOT.get())); // 996 (4912/10)
        add.accept(new ItemStack(ModItems.PWR_FUEL_HES326_HOT.get())); // 997 (4912/11)
        add.accept(new ItemStack(ModItems.PWR_FUEL_HES327_HOT.get())); // 998 (4912/12)
        add.accept(new ItemStack(ModItems.PWR_FUEL_BFB_AM_MIX_HOT.get())); // 999 (4912/13)
        add.accept(new ItemStack(ModItems.PWR_FUEL_BFB_PU241_HOT.get())); // 1000 (4912/14)

        // --- CHUNK 11: Items 1001-1100 (worker_m5_control_c11) ---
        // Batch 101 (Items 1001–1010: Depleted PWR Fuel MEU through MEA)
        add.accept(new ItemStack(ModItems.PWR_FUEL_DEPLETED.get(PWRFuelType.MEU).get())); // 1001 (4913/0)
        add.accept(new ItemStack(ModItems.PWR_FUEL_DEPLETED.get(PWRFuelType.HEU233).get())); // 1002 (4913/1)
        add.accept(new ItemStack(ModItems.PWR_FUEL_DEPLETED.get(PWRFuelType.HEU235).get())); // 1003 (4913/2)
        add.accept(new ItemStack(ModItems.PWR_FUEL_DEPLETED.get(PWRFuelType.MEN).get())); // 1004 (4913/3)
        add.accept(new ItemStack(ModItems.PWR_FUEL_DEPLETED.get(PWRFuelType.HEN237).get())); // 1005 (4913/4)
        add.accept(new ItemStack(ModItems.PWR_FUEL_DEPLETED.get(PWRFuelType.MOX).get())); // 1006 (4913/5)
        add.accept(new ItemStack(ModItems.PWR_FUEL_DEPLETED.get(PWRFuelType.MEP).get())); // 1007 (4913/6)
        add.accept(new ItemStack(ModItems.PWR_FUEL_DEPLETED.get(PWRFuelType.HEP239).get())); // 1008 (4913/7)
        add.accept(new ItemStack(ModItems.PWR_FUEL_DEPLETED.get(PWRFuelType.HEP241).get())); // 1009 (4913/8)
        add.accept(new ItemStack(ModItems.PWR_FUEL_DEPLETED.get(PWRFuelType.MEA).get())); // 1010 (4913/9)

        // Batch 102 (Items 1011–1020: Depleted PWR Fuel HEA-242 through BFB Pu-241, PWR Printer, RBMK Lid, Lid Glass, Empty Fuel, NU Fuel)
        add.accept(new ItemStack(ModItems.PWR_FUEL_DEPLETED.get(PWRFuelType.HEA242).get())); // 1011 (4913/10)
        add.accept(new ItemStack(ModItems.PWR_FUEL_DEPLETED.get(PWRFuelType.HES326).get())); // 1012 (4913/11)
        add.accept(new ItemStack(ModItems.PWR_FUEL_DEPLETED.get(PWRFuelType.HES327).get())); // 1013 (4913/12)
        add.accept(new ItemStack(ModItems.PWR_FUEL_DEPLETED.get(PWRFuelType.BFB_AM_MIX).get())); // 1014 (4913/13)
        add.accept(new ItemStack(ModItems.PWR_FUEL_DEPLETED.get(PWRFuelType.BFB_PU241).get())); // 1015 (4913/14)
        add.accept(new ItemStack(ModItems.PWR_PRINTER.get())); // 1016 (4914/0)
        add.accept(new ItemStack(ModItems.RBMK_LID.get())); // 1017 (4915/0)
        add.accept(new ItemStack(ModItems.RBMK_LID_GLASS.get())); // 1018 (4916/0)
        add.accept(new ItemStack(ModItems.RBMK_FUEL_EMPTY.get())); // 1019 (4917/0)
        add.accept(new ItemStack(ModItems.RBMK_FUEL_UEU.get())); // 1020 (4918/0)

        // Batch 103 (Items 1021–1030: RBMK Fuel Rods MEU through LEA)
        add.accept(new ItemStack(ModItems.RBMK_FUEL_MEU.get())); // 1021 (4919/0)
        add.accept(new ItemStack(ModItems.RBMK_FUEL_HEU233.get())); // 1022 (4920/0)
        add.accept(new ItemStack(ModItems.RBMK_FUEL_HEU235.get())); // 1023 (4921/0)
        add.accept(new ItemStack(ModItems.RBMK_FUEL_UZH.get())); // 1024 (4922/0)
        add.accept(new ItemStack(ModItems.RBMK_FUEL_THMEU.get())); // 1025 (4923/0)
        add.accept(new ItemStack(ModItems.RBMK_FUEL_LEP.get())); // 1026 (4924/0)
        add.accept(new ItemStack(ModItems.RBMK_FUEL_MEP.get())); // 1027 (4925/0)
        add.accept(new ItemStack(ModItems.RBMK_FUEL_HEP.get())); // 1028 (4926/0)
        add.accept(new ItemStack(ModItems.RBMK_FUEL_HEP241.get())); // 1029 (4927/0)
        add.accept(new ItemStack(ModItems.RBMK_FUEL_LEA.get())); // 1030 (4928/0)

        // Batch 104 (Items 1031–1040: RBMK Fuel Rods MEA through LEAus)
        add.accept(new ItemStack(ModItems.RBMK_FUEL_MEA.get())); // 1031 (4929/0)
        add.accept(new ItemStack(ModItems.RBMK_FUEL_HEA241.get())); // 1032 (4930/0)
        add.accept(new ItemStack(ModItems.RBMK_FUEL_HEA242.get())); // 1033 (4931/0)
        add.accept(new ItemStack(ModItems.RBMK_FUEL_MEN.get())); // 1034 (4932/0)
        add.accept(new ItemStack(ModItems.RBMK_FUEL_HEN.get())); // 1035 (4933/0)
        add.accept(new ItemStack(ModItems.RBMK_FUEL_MOX.get())); // 1036 (4934/0)
        add.accept(new ItemStack(ModItems.RBMK_FUEL_LES.get())); // 1037 (4935/0)
        add.accept(new ItemStack(ModItems.RBMK_FUEL_MES.get())); // 1038 (4936/0)
        add.accept(new ItemStack(ModItems.RBMK_FUEL_HES.get())); // 1039 (4937/0)
        add.accept(new ItemStack(ModItems.RBMK_FUEL_LEAUS.get())); // 1040 (4938/0)

        // Batch 105 (Items 1041–1050: RBMK Fuel Rods HEAus through ZFB Americium)
        add.accept(new ItemStack(ModItems.RBMK_FUEL_HEAUS.get())); // 1041 (4939/0)
        add.accept(new ItemStack(ModItems.RBMK_FUEL_PO210BE.get())); // 1042 (4940/0)
        add.accept(new ItemStack(ModItems.RBMK_FUEL_RA226BE.get())); // 1043 (4941/0)
        add.accept(new ItemStack(ModItems.RBMK_FUEL_PU238BE.get())); // 1044 (4942/0)
        add.accept(new ItemStack(ModItems.RBMK_FUEL_BALEFIRE_GOLD.get())); // 1045 (4943/0)
        add.accept(new ItemStack(ModItems.RBMK_FUEL_FLASHLEAD.get())); // 1046 (4944/0)
        add.accept(new ItemStack(ModItems.RBMK_FUEL_BALEFIRE.get())); // 1047 (4945/0)
        add.accept(new ItemStack(ModItems.RBMK_FUEL_ZFB_BISMUTH.get())); // 1048 (4946/0)
        add.accept(new ItemStack(ModItems.RBMK_FUEL_ZFB_PU241.get())); // 1049 (4947/0)
        add.accept(new ItemStack(ModItems.RBMK_FUEL_ZFB_AM_MIX.get())); // 1050 (4948/0)

        // Batch 106 (Items 1051–1060: Digamma Fuel, Fuel Test, NU Fuel Pellets metas 0–7)
        add.accept(new ItemStack(ModItems.RBMK_FUEL_DRX.get())); // 1051 (4949/0)
        add.accept(new ItemStack(ModItems.RBMK_FUEL_TEST.get())); // 1052 (4950/0)
        add.accept(new ItemStack(ModItems.RBMK_PELLET_UEU.get())); // 1053 (4951/0)
        add.accept(((RBMKPelletItem) ModItems.RBMK_PELLET_UEU.get()).withState(1, 1)); // 1054 (4951/1)
        add.accept(((RBMKPelletItem) ModItems.RBMK_PELLET_UEU.get()).withState(1, 2)); // 1055 (4951/2)
        add.accept(((RBMKPelletItem) ModItems.RBMK_PELLET_UEU.get()).withState(1, 3)); // 1056 (4951/3)
        add.accept(((RBMKPelletItem) ModItems.RBMK_PELLET_UEU.get()).withState(1, 4)); // 1057 (4951/4)
        add.accept(((RBMKPelletItem) ModItems.RBMK_PELLET_UEU.get()).withState(1, 5)); // 1058 (4951/5)
        add.accept(((RBMKPelletItem) ModItems.RBMK_PELLET_UEU.get()).withState(1, 6)); // 1059 (4951/6)
        add.accept(((RBMKPelletItem) ModItems.RBMK_PELLET_UEU.get()).withState(1, 7)); // 1060 (4951/7)

        // Batch 107 (Items 1061–1070: NU Pellets metas 8–9, MEU Fuel Pellets metas 0–7)
        add.accept(((RBMKPelletItem) ModItems.RBMK_PELLET_UEU.get()).withState(1, 8)); // 1061 (4951/8)
        add.accept(((RBMKPelletItem) ModItems.RBMK_PELLET_UEU.get()).withState(1, 9)); // 1062 (4951/9)
        add.accept(new ItemStack(ModItems.RBMK_PELLET_MEU.get())); // 1063 (4952/0)
        add.accept(((RBMKPelletItem) ModItems.RBMK_PELLET_MEU.get()).withState(1, 1)); // 1064 (4952/1)
        add.accept(((RBMKPelletItem) ModItems.RBMK_PELLET_MEU.get()).withState(1, 2)); // 1065 (4952/2)
        add.accept(((RBMKPelletItem) ModItems.RBMK_PELLET_MEU.get()).withState(1, 3)); // 1066 (4952/3)
        add.accept(((RBMKPelletItem) ModItems.RBMK_PELLET_MEU.get()).withState(1, 4)); // 1067 (4952/4)
        add.accept(((RBMKPelletItem) ModItems.RBMK_PELLET_MEU.get()).withState(1, 5)); // 1068 (4952/5)
        add.accept(((RBMKPelletItem) ModItems.RBMK_PELLET_MEU.get()).withState(1, 6)); // 1069 (4952/6)
        add.accept(((RBMKPelletItem) ModItems.RBMK_PELLET_MEU.get()).withState(1, 7)); // 1070 (4952/7)

        // Batch 108 (Items 1071–1080: MEU Pellets metas 8–9, HEU-233 Fuel Pellets metas 0–7)
        add.accept(((RBMKPelletItem) ModItems.RBMK_PELLET_MEU.get()).withState(1, 8)); // 1071 (4952/8)
        add.accept(((RBMKPelletItem) ModItems.RBMK_PELLET_MEU.get()).withState(1, 9)); // 1072 (4952/9)
        add.accept(new ItemStack(ModItems.RBMK_PELLET_HEU233.get())); // 1073 (4953/0)
        add.accept(((RBMKPelletItem) ModItems.RBMK_PELLET_HEU233.get()).withState(1, 1)); // 1074 (4953/1)
        add.accept(((RBMKPelletItem) ModItems.RBMK_PELLET_HEU233.get()).withState(1, 2)); // 1075 (4953/2)
        add.accept(((RBMKPelletItem) ModItems.RBMK_PELLET_HEU233.get()).withState(1, 3)); // 1076 (4953/3)
        add.accept(((RBMKPelletItem) ModItems.RBMK_PELLET_HEU233.get()).withState(1, 4)); // 1077 (4953/4)
        add.accept(((RBMKPelletItem) ModItems.RBMK_PELLET_HEU233.get()).withState(1, 5)); // 1078 (4953/5)
        add.accept(((RBMKPelletItem) ModItems.RBMK_PELLET_HEU233.get()).withState(1, 6)); // 1079 (4953/6)
        add.accept(((RBMKPelletItem) ModItems.RBMK_PELLET_HEU233.get()).withState(1, 7)); // 1080 (4953/7)

        // Batch 109 (Items 1081–1090: HEU-233 Pellets metas 8–9, HEU-235 Fuel Pellets metas 0–7)
        add.accept(((RBMKPelletItem) ModItems.RBMK_PELLET_HEU233.get()).withState(1, 8)); // 1081 (4953/8)
        add.accept(((RBMKPelletItem) ModItems.RBMK_PELLET_HEU233.get()).withState(1, 9)); // 1082 (4953/9)
        add.accept(new ItemStack(ModItems.RBMK_PELLET_HEU235.get())); // 1083 (4954/0)
        add.accept(((RBMKPelletItem) ModItems.RBMK_PELLET_HEU235.get()).withState(1, 1)); // 1084 (4954/1)
        add.accept(((RBMKPelletItem) ModItems.RBMK_PELLET_HEU235.get()).withState(1, 2)); // 1085 (4954/2)
        add.accept(((RBMKPelletItem) ModItems.RBMK_PELLET_HEU235.get()).withState(1, 3)); // 1086 (4954/3)
        add.accept(((RBMKPelletItem) ModItems.RBMK_PELLET_HEU235.get()).withState(1, 4)); // 1087 (4954/4)
        add.accept(((RBMKPelletItem) ModItems.RBMK_PELLET_HEU235.get()).withState(1, 5)); // 1088 (4954/5)
        add.accept(((RBMKPelletItem) ModItems.RBMK_PELLET_HEU235.get()).withState(1, 6)); // 1089 (4954/6)
        add.accept(((RBMKPelletItem) ModItems.RBMK_PELLET_HEU235.get()).withState(1, 7)); // 1090 (4954/7)

        // Batch 110 (Items 1091–1100: HEU-235 Pellets metas 8–9, UZrH Fuel Pellets metas 0–7)
        add.accept(((RBMKPelletItem) ModItems.RBMK_PELLET_HEU235.get()).withState(1, 8)); // 1091 (4954/8)
        add.accept(((RBMKPelletItem) ModItems.RBMK_PELLET_HEU235.get()).withState(1, 9)); // 1092 (4954/9)
        add.accept(new ItemStack(ModItems.RBMK_PELLET_UZH.get())); // 1093 (4955/0)
        add.accept(((RBMKPelletItem) ModItems.RBMK_PELLET_UZH.get()).withState(1, 1)); // 1094 (4955/1)
        add.accept(((RBMKPelletItem) ModItems.RBMK_PELLET_UZH.get()).withState(1, 2)); // 1095 (4955/2)
        add.accept(((RBMKPelletItem) ModItems.RBMK_PELLET_UZH.get()).withState(1, 3)); // 1096 (4955/3)
        add.accept(((RBMKPelletItem) ModItems.RBMK_PELLET_UZH.get()).withState(1, 4)); // 1097 (4955/4)
        add.accept(((RBMKPelletItem) ModItems.RBMK_PELLET_UZH.get()).withState(1, 5)); // 1098 (4955/5)
        add.accept(((RBMKPelletItem) ModItems.RBMK_PELLET_UZH.get()).withState(1, 6)); // 1099 (4955/6)
        add.accept(((RBMKPelletItem) ModItems.RBMK_PELLET_UZH.get()).withState(1, 7)); // 1100 (4955/7)

        // --- CHUNK 12: Items 1101-1200 (worker_m5_control_c12) ---
        // Batch 111 (Items 1101–1110: UZrH Pellets metas 8–9, ThMEU Fuel Pellets metas 0–7)
        add.accept(((RBMKPelletItem) ModItems.RBMK_PELLET_UZH.get()).withState(1, 8)); // 1101 (4955/8)
        add.accept(((RBMKPelletItem) ModItems.RBMK_PELLET_UZH.get()).withState(1, 9)); // 1102 (4955/9)
        add.accept(new ItemStack(ModItems.RBMK_PELLET_THMEU.get())); // 1103 (4956/0)
        add.accept(((RBMKPelletItem) ModItems.RBMK_PELLET_THMEU.get()).withState(1, 1)); // 1104 (4956/1)
        add.accept(((RBMKPelletItem) ModItems.RBMK_PELLET_THMEU.get()).withState(1, 2)); // 1105 (4956/2)
        add.accept(((RBMKPelletItem) ModItems.RBMK_PELLET_THMEU.get()).withState(1, 3)); // 1106 (4956/3)
        add.accept(((RBMKPelletItem) ModItems.RBMK_PELLET_THMEU.get()).withState(1, 4)); // 1107 (4956/4)
        add.accept(((RBMKPelletItem) ModItems.RBMK_PELLET_THMEU.get()).withState(1, 5)); // 1108 (4956/5)
        add.accept(((RBMKPelletItem) ModItems.RBMK_PELLET_THMEU.get()).withState(1, 6)); // 1109 (4956/6)
        add.accept(((RBMKPelletItem) ModItems.RBMK_PELLET_THMEU.get()).withState(1, 7)); // 1110 (4956/7)

        // Batch 112 (Items 1111–1120: ThMEU Pellets metas 8–9, LEP Fuel Pellets metas 0–7)
        add.accept(((RBMKPelletItem) ModItems.RBMK_PELLET_THMEU.get()).withState(1, 8)); // 1111 (4956/8)
        add.accept(((RBMKPelletItem) ModItems.RBMK_PELLET_THMEU.get()).withState(1, 9)); // 1112 (4956/9)
        add.accept(new ItemStack(ModItems.RBMK_PELLET_LEP.get())); // 1113 (4957/0)
        add.accept(((RBMKPelletItem) ModItems.RBMK_PELLET_LEP.get()).withState(1, 1)); // 1114 (4957/1)
        add.accept(((RBMKPelletItem) ModItems.RBMK_PELLET_LEP.get()).withState(1, 2)); // 1115 (4957/2)
        add.accept(((RBMKPelletItem) ModItems.RBMK_PELLET_LEP.get()).withState(1, 3)); // 1116 (4957/3)
        add.accept(((RBMKPelletItem) ModItems.RBMK_PELLET_LEP.get()).withState(1, 4)); // 1117 (4957/4)
        add.accept(((RBMKPelletItem) ModItems.RBMK_PELLET_LEP.get()).withState(1, 5)); // 1118 (4957/5)
        add.accept(((RBMKPelletItem) ModItems.RBMK_PELLET_LEP.get()).withState(1, 6)); // 1119 (4957/6)
        add.accept(((RBMKPelletItem) ModItems.RBMK_PELLET_LEP.get()).withState(1, 7)); // 1120 (4957/7)

        // Batch 113 (Items 1121–1130: LEP Pellets metas 8–9, MEP Fuel Pellets metas 0–7)
        add.accept(((RBMKPelletItem) ModItems.RBMK_PELLET_LEP.get()).withState(1, 8)); // 1121 (4957/8)
        add.accept(((RBMKPelletItem) ModItems.RBMK_PELLET_LEP.get()).withState(1, 9)); // 1122 (4957/9)
        add.accept(new ItemStack(ModItems.RBMK_PELLET_MEP.get())); // 1123 (4958/0)
        add.accept(((RBMKPelletItem) ModItems.RBMK_PELLET_MEP.get()).withState(1, 1)); // 1124 (4958/1)
        add.accept(((RBMKPelletItem) ModItems.RBMK_PELLET_MEP.get()).withState(1, 2)); // 1125 (4958/2)
        add.accept(((RBMKPelletItem) ModItems.RBMK_PELLET_MEP.get()).withState(1, 3)); // 1126 (4958/3)
        add.accept(((RBMKPelletItem) ModItems.RBMK_PELLET_MEP.get()).withState(1, 4)); // 1127 (4958/4)
        add.accept(((RBMKPelletItem) ModItems.RBMK_PELLET_MEP.get()).withState(1, 5)); // 1128 (4958/5)
        add.accept(((RBMKPelletItem) ModItems.RBMK_PELLET_MEP.get()).withState(1, 6)); // 1129 (4958/6)
        add.accept(((RBMKPelletItem) ModItems.RBMK_PELLET_MEP.get()).withState(1, 7)); // 1130 (4958/7)

        // Batch 114 (Items 1131–1140: MEP Pellets metas 8–9, HEP-239 Fuel Pellets metas 0–7)
        add.accept(((RBMKPelletItem) ModItems.RBMK_PELLET_MEP.get()).withState(1, 8)); // 1131 (4958/8)
        add.accept(((RBMKPelletItem) ModItems.RBMK_PELLET_MEP.get()).withState(1, 9)); // 1132 (4958/9)
        add.accept(new ItemStack(ModItems.RBMK_PELLET_HEP.get())); // 1133 (4959/0)
        add.accept(((RBMKPelletItem) ModItems.RBMK_PELLET_HEP.get()).withState(1, 1)); // 1134 (4959/1)
        add.accept(((RBMKPelletItem) ModItems.RBMK_PELLET_HEP.get()).withState(1, 2)); // 1135 (4959/2)
        add.accept(((RBMKPelletItem) ModItems.RBMK_PELLET_HEP.get()).withState(1, 3)); // 1136 (4959/3)
        add.accept(((RBMKPelletItem) ModItems.RBMK_PELLET_HEP.get()).withState(1, 4)); // 1137 (4959/4)
        add.accept(((RBMKPelletItem) ModItems.RBMK_PELLET_HEP.get()).withState(1, 5)); // 1138 (4959/5)
        add.accept(((RBMKPelletItem) ModItems.RBMK_PELLET_HEP.get()).withState(1, 6)); // 1139 (4959/6)
        add.accept(((RBMKPelletItem) ModItems.RBMK_PELLET_HEP.get()).withState(1, 7)); // 1140 (4959/7)

        // Batch 115 (Items 1141–1150: HEP-239 Pellets metas 8–9, HEP-241 Fuel Pellets metas 0–7)
        add.accept(((RBMKPelletItem) ModItems.RBMK_PELLET_HEP.get()).withState(1, 8)); // 1141 (4959/8)
        add.accept(((RBMKPelletItem) ModItems.RBMK_PELLET_HEP.get()).withState(1, 9)); // 1142 (4959/9)
        add.accept(new ItemStack(ModItems.RBMK_PELLET_HEP241.get())); // 1143 (4960/0)
        add.accept(((RBMKPelletItem) ModItems.RBMK_PELLET_HEP241.get()).withState(1, 1)); // 1144 (4960/1)
        add.accept(((RBMKPelletItem) ModItems.RBMK_PELLET_HEP241.get()).withState(1, 2)); // 1145 (4960/2)
        add.accept(((RBMKPelletItem) ModItems.RBMK_PELLET_HEP241.get()).withState(1, 3)); // 1146 (4960/3)
        add.accept(((RBMKPelletItem) ModItems.RBMK_PELLET_HEP241.get()).withState(1, 4)); // 1147 (4960/4)
        add.accept(((RBMKPelletItem) ModItems.RBMK_PELLET_HEP241.get()).withState(1, 5)); // 1148 (4960/5)
        add.accept(((RBMKPelletItem) ModItems.RBMK_PELLET_HEP241.get()).withState(1, 6)); // 1149 (4960/6)
        add.accept(((RBMKPelletItem) ModItems.RBMK_PELLET_HEP241.get()).withState(1, 7)); // 1150 (4960/7)

        // Batch 116 (Items 1151–1160: HEP-241 Pellets metas 8–9, LEA Fuel Pellets metas 0–7)
        add.accept(((RBMKPelletItem) ModItems.RBMK_PELLET_HEP241.get()).withState(1, 8)); // 1151 (4960/8)
        add.accept(((RBMKPelletItem) ModItems.RBMK_PELLET_HEP241.get()).withState(1, 9)); // 1152 (4960/9)
        add.accept(new ItemStack(ModItems.RBMK_PELLET_LEA.get())); // 1153 (4961/0)
        add.accept(((RBMKPelletItem) ModItems.RBMK_PELLET_LEA.get()).withState(1, 1)); // 1154 (4961/1)
        add.accept(((RBMKPelletItem) ModItems.RBMK_PELLET_LEA.get()).withState(1, 2)); // 1155 (4961/2)
        add.accept(((RBMKPelletItem) ModItems.RBMK_PELLET_LEA.get()).withState(1, 3)); // 1156 (4961/3)
        add.accept(((RBMKPelletItem) ModItems.RBMK_PELLET_LEA.get()).withState(1, 4)); // 1157 (4961/4)
        add.accept(((RBMKPelletItem) ModItems.RBMK_PELLET_LEA.get()).withState(1, 5)); // 1158 (4961/5)
        add.accept(((RBMKPelletItem) ModItems.RBMK_PELLET_LEA.get()).withState(1, 6)); // 1159 (4961/6)
        add.accept(((RBMKPelletItem) ModItems.RBMK_PELLET_LEA.get()).withState(1, 7)); // 1160 (4961/7)

        // Batch 117 (Items 1161–1170: LEA Pellets metas 8–9, MEA Fuel Pellets metas 0–7)
        add.accept(((RBMKPelletItem) ModItems.RBMK_PELLET_LEA.get()).withState(1, 8)); // 1161 (4961/8)
        add.accept(((RBMKPelletItem) ModItems.RBMK_PELLET_LEA.get()).withState(1, 9)); // 1162 (4961/9)
        add.accept(new ItemStack(ModItems.RBMK_PELLET_MEA.get())); // 1163 (4962/0)
        add.accept(((RBMKPelletItem) ModItems.RBMK_PELLET_MEA.get()).withState(1, 1)); // 1164 (4962/1)
        add.accept(((RBMKPelletItem) ModItems.RBMK_PELLET_MEA.get()).withState(1, 2)); // 1165 (4962/2)
        add.accept(((RBMKPelletItem) ModItems.RBMK_PELLET_MEA.get()).withState(1, 3)); // 1166 (4962/3)
        add.accept(((RBMKPelletItem) ModItems.RBMK_PELLET_MEA.get()).withState(1, 4)); // 1167 (4962/4)
        add.accept(((RBMKPelletItem) ModItems.RBMK_PELLET_MEA.get()).withState(1, 5)); // 1168 (4962/5)
        add.accept(((RBMKPelletItem) ModItems.RBMK_PELLET_MEA.get()).withState(1, 6)); // 1169 (4962/6)
        add.accept(((RBMKPelletItem) ModItems.RBMK_PELLET_MEA.get()).withState(1, 7)); // 1170 (4962/7)

        // Batch 118 (Items 1171–1180: MEA Pellets metas 8–9, HEA-241 Fuel Pellets metas 0–7)
        add.accept(((RBMKPelletItem) ModItems.RBMK_PELLET_MEA.get()).withState(1, 8)); // 1171 (4962/8)
        add.accept(((RBMKPelletItem) ModItems.RBMK_PELLET_MEA.get()).withState(1, 9)); // 1172 (4962/9)
        add.accept(new ItemStack(ModItems.RBMK_PELLET_HEA241.get())); // 1173 (4963/0)
        add.accept(((RBMKPelletItem) ModItems.RBMK_PELLET_HEA241.get()).withState(1, 1)); // 1174 (4963/1)
        add.accept(((RBMKPelletItem) ModItems.RBMK_PELLET_HEA241.get()).withState(1, 2)); // 1175 (4963/2)
        add.accept(((RBMKPelletItem) ModItems.RBMK_PELLET_HEA241.get()).withState(1, 3)); // 1176 (4963/3)
        add.accept(((RBMKPelletItem) ModItems.RBMK_PELLET_HEA241.get()).withState(1, 4)); // 1177 (4963/4)
        add.accept(((RBMKPelletItem) ModItems.RBMK_PELLET_HEA241.get()).withState(1, 5)); // 1178 (4963/5)
        add.accept(((RBMKPelletItem) ModItems.RBMK_PELLET_HEA241.get()).withState(1, 6)); // 1179 (4963/6)
        add.accept(((RBMKPelletItem) ModItems.RBMK_PELLET_HEA241.get()).withState(1, 7)); // 1180 (4963/7)

        // Batch 119 (Items 1181–1190: HEA-241 Pellets metas 8–9, HEA-242 Fuel Pellets metas 0–7)
        add.accept(((RBMKPelletItem) ModItems.RBMK_PELLET_HEA241.get()).withState(1, 8)); // 1181 (4963/8)
        add.accept(((RBMKPelletItem) ModItems.RBMK_PELLET_HEA241.get()).withState(1, 9)); // 1182 (4963/9)
        add.accept(new ItemStack(ModItems.RBMK_PELLET_HEA242.get())); // 1183 (4964/0)
        add.accept(((RBMKPelletItem) ModItems.RBMK_PELLET_HEA242.get()).withState(1, 1)); // 1184 (4964/1)
        add.accept(((RBMKPelletItem) ModItems.RBMK_PELLET_HEA242.get()).withState(1, 2)); // 1185 (4964/2)
        add.accept(((RBMKPelletItem) ModItems.RBMK_PELLET_HEA242.get()).withState(1, 3)); // 1186 (4964/3)
        add.accept(((RBMKPelletItem) ModItems.RBMK_PELLET_HEA242.get()).withState(1, 4)); // 1187 (4964/4)
        add.accept(((RBMKPelletItem) ModItems.RBMK_PELLET_HEA242.get()).withState(1, 5)); // 1188 (4964/5)
        add.accept(((RBMKPelletItem) ModItems.RBMK_PELLET_HEA242.get()).withState(1, 6)); // 1189 (4964/6)
        add.accept(((RBMKPelletItem) ModItems.RBMK_PELLET_HEA242.get()).withState(1, 7)); // 1190 (4964/7)

        // Batch 120 (Items 1191–1200: HEA-242 Pellets metas 8–9, MEN Fuel Pellets metas 0–7)
        add.accept(((RBMKPelletItem) ModItems.RBMK_PELLET_HEA242.get()).withState(1, 8)); // 1191 (4964/8)
        add.accept(((RBMKPelletItem) ModItems.RBMK_PELLET_HEA242.get()).withState(1, 9)); // 1192 (4964/9)
        add.accept(new ItemStack(ModItems.RBMK_PELLET_MEN.get())); // 1193 (4965/0)
        add.accept(((RBMKPelletItem) ModItems.RBMK_PELLET_MEN.get()).withState(1, 1)); // 1194 (4965/1)
        add.accept(((RBMKPelletItem) ModItems.RBMK_PELLET_MEN.get()).withState(1, 2)); // 1195 (4965/2)
        add.accept(((RBMKPelletItem) ModItems.RBMK_PELLET_MEN.get()).withState(1, 3)); // 1196 (4965/3)
        add.accept(((RBMKPelletItem) ModItems.RBMK_PELLET_MEN.get()).withState(1, 4)); // 1197 (4965/4)
        add.accept(((RBMKPelletItem) ModItems.RBMK_PELLET_MEN.get()).withState(1, 5)); // 1198 (4965/5)
        add.accept(((RBMKPelletItem) ModItems.RBMK_PELLET_MEN.get()).withState(1, 6)); // 1199 (4965/6)
        add.accept(((RBMKPelletItem) ModItems.RBMK_PELLET_MEN.get()).withState(1, 7)); // 1200 (4965/7)

        // --- CHUNK 13: Items 1201-1300 (worker_m5_control_c13) ---
        // Batch 121 (Items 1201–1210: MEN Fuel Pellets metas 8–9, HEN Fuel Pellets metas 0–7)
        add.accept(((RBMKPelletItem) ModItems.RBMK_PELLET_MEN.get()).withState(1, 8)); // 1201 (4965/8)
        add.accept(((RBMKPelletItem) ModItems.RBMK_PELLET_MEN.get()).withState(1, 9)); // 1202 (4965/9)
        add.accept(new ItemStack(ModItems.RBMK_PELLET_HEN.get())); // 1203 (4966/0)
        add.accept(((RBMKPelletItem) ModItems.RBMK_PELLET_HEN.get()).withState(1, 1)); // 1204 (4966/1)
        add.accept(((RBMKPelletItem) ModItems.RBMK_PELLET_HEN.get()).withState(1, 2)); // 1205 (4966/2)
        add.accept(((RBMKPelletItem) ModItems.RBMK_PELLET_HEN.get()).withState(1, 3)); // 1206 (4966/3)
        add.accept(((RBMKPelletItem) ModItems.RBMK_PELLET_HEN.get()).withState(1, 4)); // 1207 (4966/4)
        add.accept(((RBMKPelletItem) ModItems.RBMK_PELLET_HEN.get()).withState(1, 5)); // 1208 (4966/5)
        add.accept(((RBMKPelletItem) ModItems.RBMK_PELLET_HEN.get()).withState(1, 6)); // 1209 (4966/6)
        add.accept(((RBMKPelletItem) ModItems.RBMK_PELLET_HEN.get()).withState(1, 7)); // 1210 (4966/7)
        // Batch 122 (Items 1211–1220: HEN Fuel Pellets metas 8–9, MOX Fuel Pellets metas 0–7)
        add.accept(((RBMKPelletItem) ModItems.RBMK_PELLET_HEN.get()).withState(1, 8)); // 1211 (4966/8)
        add.accept(((RBMKPelletItem) ModItems.RBMK_PELLET_HEN.get()).withState(1, 9)); // 1212 (4966/9)
        add.accept(new ItemStack(ModItems.RBMK_PELLET_MOX.get())); // 1213 (4967/0)
        add.accept(((RBMKPelletItem) ModItems.RBMK_PELLET_MOX.get()).withState(1, 1)); // 1214 (4967/1)
        add.accept(((RBMKPelletItem) ModItems.RBMK_PELLET_MOX.get()).withState(1, 2)); // 1215 (4967/2)
        add.accept(((RBMKPelletItem) ModItems.RBMK_PELLET_MOX.get()).withState(1, 3)); // 1216 (4967/3)
        add.accept(((RBMKPelletItem) ModItems.RBMK_PELLET_MOX.get()).withState(1, 4)); // 1217 (4967/4)
        add.accept(((RBMKPelletItem) ModItems.RBMK_PELLET_MOX.get()).withState(1, 5)); // 1218 (4967/5)
        add.accept(((RBMKPelletItem) ModItems.RBMK_PELLET_MOX.get()).withState(1, 6)); // 1219 (4967/6)
        add.accept(((RBMKPelletItem) ModItems.RBMK_PELLET_MOX.get()).withState(1, 7)); // 1220 (4967/7)
        // Batch 123 (Items 1221–1230: MOX Fuel Pellets metas 8–9, LES Fuel Pellets metas 0–7)
        add.accept(((RBMKPelletItem) ModItems.RBMK_PELLET_MOX.get()).withState(1, 8)); // 1221 (4967/8)
        add.accept(((RBMKPelletItem) ModItems.RBMK_PELLET_MOX.get()).withState(1, 9)); // 1222 (4967/9)
        add.accept(new ItemStack(ModItems.RBMK_PELLET_LES.get())); // 1223 (4968/0)
        add.accept(((RBMKPelletItem) ModItems.RBMK_PELLET_LES.get()).withState(1, 1)); // 1224 (4968/1)
        add.accept(((RBMKPelletItem) ModItems.RBMK_PELLET_LES.get()).withState(1, 2)); // 1225 (4968/2)
        add.accept(((RBMKPelletItem) ModItems.RBMK_PELLET_LES.get()).withState(1, 3)); // 1226 (4968/3)
        add.accept(((RBMKPelletItem) ModItems.RBMK_PELLET_LES.get()).withState(1, 4)); // 1227 (4968/4)
        add.accept(((RBMKPelletItem) ModItems.RBMK_PELLET_LES.get()).withState(1, 5)); // 1228 (4968/5)
        add.accept(((RBMKPelletItem) ModItems.RBMK_PELLET_LES.get()).withState(1, 6)); // 1229 (4968/6)
        add.accept(((RBMKPelletItem) ModItems.RBMK_PELLET_LES.get()).withState(1, 7)); // 1230 (4968/7)
        // Batch 124 (Items 1231–1240: LES Fuel Pellets metas 8–9, MES Fuel Pellets metas 0–7)
        add.accept(((RBMKPelletItem) ModItems.RBMK_PELLET_LES.get()).withState(1, 8)); // 1231 (4968/8)
        add.accept(((RBMKPelletItem) ModItems.RBMK_PELLET_LES.get()).withState(1, 9)); // 1232 (4968/9)
        add.accept(new ItemStack(ModItems.RBMK_PELLET_MES.get())); // 1233 (4969/0)
        add.accept(((RBMKPelletItem) ModItems.RBMK_PELLET_MES.get()).withState(1, 1)); // 1234 (4969/1)
        add.accept(((RBMKPelletItem) ModItems.RBMK_PELLET_MES.get()).withState(1, 2)); // 1235 (4969/2)
        add.accept(((RBMKPelletItem) ModItems.RBMK_PELLET_MES.get()).withState(1, 3)); // 1236 (4969/3)
        add.accept(((RBMKPelletItem) ModItems.RBMK_PELLET_MES.get()).withState(1, 4)); // 1237 (4969/4)
        add.accept(((RBMKPelletItem) ModItems.RBMK_PELLET_MES.get()).withState(1, 5)); // 1238 (4969/5)
        add.accept(((RBMKPelletItem) ModItems.RBMK_PELLET_MES.get()).withState(1, 6)); // 1239 (4969/6)
        add.accept(((RBMKPelletItem) ModItems.RBMK_PELLET_MES.get()).withState(1, 7)); // 1240 (4969/7)
        // Batch 125 (Items 1241–1250: MES Fuel Pellets metas 8–9, HES Fuel Pellets metas 0–7)
        add.accept(((RBMKPelletItem) ModItems.RBMK_PELLET_MES.get()).withState(1, 8)); // 1241 (4969/8)
        add.accept(((RBMKPelletItem) ModItems.RBMK_PELLET_MES.get()).withState(1, 9)); // 1242 (4969/9)
        add.accept(new ItemStack(ModItems.RBMK_PELLET_HES.get())); // 1243 (4970/0)
        add.accept(((RBMKPelletItem) ModItems.RBMK_PELLET_HES.get()).withState(1, 1)); // 1244 (4970/1)
        add.accept(((RBMKPelletItem) ModItems.RBMK_PELLET_HES.get()).withState(1, 2)); // 1245 (4970/2)
        add.accept(((RBMKPelletItem) ModItems.RBMK_PELLET_HES.get()).withState(1, 3)); // 1246 (4970/3)
        add.accept(((RBMKPelletItem) ModItems.RBMK_PELLET_HES.get()).withState(1, 4)); // 1247 (4970/4)
        add.accept(((RBMKPelletItem) ModItems.RBMK_PELLET_HES.get()).withState(1, 5)); // 1248 (4970/5)
        add.accept(((RBMKPelletItem) ModItems.RBMK_PELLET_HES.get()).withState(1, 6)); // 1249 (4970/6)
        add.accept(((RBMKPelletItem) ModItems.RBMK_PELLET_HES.get()).withState(1, 7)); // 1250 (4970/7)
        // Batch 126 (Items 1251–1260: HES Fuel Pellets metas 8–9, LEAus Fuel Pellets metas 0–7)
        add.accept(((RBMKPelletItem) ModItems.RBMK_PELLET_HES.get()).withState(1, 8)); // 1251 (4970/8)
        add.accept(((RBMKPelletItem) ModItems.RBMK_PELLET_HES.get()).withState(1, 9)); // 1252 (4970/9)
        add.accept(new ItemStack(ModItems.RBMK_PELLET_LEAUS.get())); // 1253 (4971/0)
        add.accept(((RBMKPelletItem) ModItems.RBMK_PELLET_LEAUS.get()).withState(1, 1)); // 1254 (4971/1)
        add.accept(((RBMKPelletItem) ModItems.RBMK_PELLET_LEAUS.get()).withState(1, 2)); // 1255 (4971/2)
        add.accept(((RBMKPelletItem) ModItems.RBMK_PELLET_LEAUS.get()).withState(1, 3)); // 1256 (4971/3)
        add.accept(((RBMKPelletItem) ModItems.RBMK_PELLET_LEAUS.get()).withState(1, 4)); // 1257 (4971/4)
        add.accept(((RBMKPelletItem) ModItems.RBMK_PELLET_LEAUS.get()).withState(1, 5)); // 1258 (4971/5)
        add.accept(((RBMKPelletItem) ModItems.RBMK_PELLET_LEAUS.get()).withState(1, 6)); // 1259 (4971/6)
        add.accept(((RBMKPelletItem) ModItems.RBMK_PELLET_LEAUS.get()).withState(1, 7)); // 1260 (4971/7)
        // Batch 127 (Items 1261–1270: LEAus Fuel Pellets metas 8–9, HEAus Fuel Pellets metas 0–7)
        add.accept(((RBMKPelletItem) ModItems.RBMK_PELLET_LEAUS.get()).withState(1, 8)); // 1261 (4971/8)
        add.accept(((RBMKPelletItem) ModItems.RBMK_PELLET_LEAUS.get()).withState(1, 9)); // 1262 (4971/9)
        add.accept(new ItemStack(ModItems.RBMK_PELLET_HEAUS.get())); // 1263 (4972/0)
        add.accept(((RBMKPelletItem) ModItems.RBMK_PELLET_HEAUS.get()).withState(1, 1)); // 1264 (4972/1)
        add.accept(((RBMKPelletItem) ModItems.RBMK_PELLET_HEAUS.get()).withState(1, 2)); // 1265 (4972/2)
        add.accept(((RBMKPelletItem) ModItems.RBMK_PELLET_HEAUS.get()).withState(1, 3)); // 1266 (4972/3)
        add.accept(((RBMKPelletItem) ModItems.RBMK_PELLET_HEAUS.get()).withState(1, 4)); // 1267 (4972/4)
        add.accept(((RBMKPelletItem) ModItems.RBMK_PELLET_HEAUS.get()).withState(1, 5)); // 1268 (4972/5)
        add.accept(((RBMKPelletItem) ModItems.RBMK_PELLET_HEAUS.get()).withState(1, 6)); // 1269 (4972/6)
        add.accept(((RBMKPelletItem) ModItems.RBMK_PELLET_HEAUS.get()).withState(1, 7)); // 1270 (4972/7)
        // Batch 128 (Items 1271–1280: HEAus Fuel Pellets metas 8–9, Po210Be Source Pellets metas 0–4, Ra226Be Source Pellets metas 0–2)
        add.accept(((RBMKPelletItem) ModItems.RBMK_PELLET_HEAUS.get()).withState(1, 8)); // 1271 (4972/8)
        add.accept(((RBMKPelletItem) ModItems.RBMK_PELLET_HEAUS.get()).withState(1, 9)); // 1272 (4972/9)
        add.accept(new ItemStack(ModItems.RBMK_PELLET_PO210BE.get())); // 1273 (4973/0)
        add.accept(((RBMKPelletItem) ModItems.RBMK_PELLET_PO210BE.get()).withState(1, 1)); // 1274 (4973/1)
        add.accept(((RBMKPelletItem) ModItems.RBMK_PELLET_PO210BE.get()).withState(1, 2)); // 1275 (4973/2)
        add.accept(((RBMKPelletItem) ModItems.RBMK_PELLET_PO210BE.get()).withState(1, 3)); // 1276 (4973/3)
        add.accept(((RBMKPelletItem) ModItems.RBMK_PELLET_PO210BE.get()).withState(1, 4)); // 1277 (4973/4)
        add.accept(new ItemStack(ModItems.RBMK_PELLET_RA226BE.get())); // 1278 (4974/0)
        add.accept(((RBMKPelletItem) ModItems.RBMK_PELLET_RA226BE.get()).withState(1, 1)); // 1279 (4974/1)
        add.accept(((RBMKPelletItem) ModItems.RBMK_PELLET_RA226BE.get()).withState(1, 2)); // 1280 (4974/2)
        // Batch 129 (Items 1281–1290: Ra226Be Source Pellets metas 3–4, Pu238Be Source Pellets metas 0–7)
        add.accept(((RBMKPelletItem) ModItems.RBMK_PELLET_RA226BE.get()).withState(1, 3)); // 1281 (4974/3)
        add.accept(((RBMKPelletItem) ModItems.RBMK_PELLET_RA226BE.get()).withState(1, 4)); // 1282 (4974/4)
        add.accept(new ItemStack(ModItems.RBMK_PELLET_PU238BE.get())); // 1283 (4975/0)
        add.accept(((RBMKPelletItem) ModItems.RBMK_PELLET_PU238BE.get()).withState(1, 1)); // 1284 (4975/1)
        add.accept(((RBMKPelletItem) ModItems.RBMK_PELLET_PU238BE.get()).withState(1, 2)); // 1285 (4975/2)
        add.accept(((RBMKPelletItem) ModItems.RBMK_PELLET_PU238BE.get()).withState(1, 3)); // 1286 (4975/3)
        add.accept(((RBMKPelletItem) ModItems.RBMK_PELLET_PU238BE.get()).withState(1, 4)); // 1287 (4975/4)
        add.accept(((RBMKPelletItem) ModItems.RBMK_PELLET_PU238BE.get()).withState(1, 5)); // 1288 (4975/5)
        add.accept(((RBMKPelletItem) ModItems.RBMK_PELLET_PU238BE.get()).withState(1, 6)); // 1289 (4975/6)
        add.accept(((RBMKPelletItem) ModItems.RBMK_PELLET_PU238BE.get()).withState(1, 7)); // 1290 (4975/7)
        // Batch 130 (Items 1291–1300: Pu238Be Source Pellets metas 8–9, Flashgold Fuel Pellets metas 0–4, Flashlead Fuel Pellets metas 0–2)
        add.accept(((RBMKPelletItem) ModItems.RBMK_PELLET_PU238BE.get()).withState(1, 8)); // 1291 (4975/8)
        add.accept(((RBMKPelletItem) ModItems.RBMK_PELLET_PU238BE.get()).withState(1, 9)); // 1292 (4975/9)
        add.accept(new ItemStack(ModItems.RBMK_PELLET_BALEFIRE_GOLD.get())); // 1293 (4976/0)
        add.accept(((RBMKPelletItem) ModItems.RBMK_PELLET_BALEFIRE_GOLD.get()).withState(1, 1)); // 1294 (4976/1)
        add.accept(((RBMKPelletItem) ModItems.RBMK_PELLET_BALEFIRE_GOLD.get()).withState(1, 2)); // 1295 (4976/2)
        add.accept(((RBMKPelletItem) ModItems.RBMK_PELLET_BALEFIRE_GOLD.get()).withState(1, 3)); // 1296 (4976/3)
        add.accept(((RBMKPelletItem) ModItems.RBMK_PELLET_BALEFIRE_GOLD.get()).withState(1, 4)); // 1297 (4976/4)
        add.accept(new ItemStack(ModItems.RBMK_PELLET_FLASHLEAD.get())); // 1298 (4977/0)
        add.accept(((RBMKPelletItem) ModItems.RBMK_PELLET_FLASHLEAD.get()).withState(1, 1)); // 1299 (4977/1)
        add.accept(((RBMKPelletItem) ModItems.RBMK_PELLET_FLASHLEAD.get()).withState(1, 2)); // 1300 (4977/2)

        // --- CHUNK 14: Items 1301-1388 (worker_m5_control_c14) ---
        // Batch 131 (Items 1301–1310: Flashlead Fuel Pellets metas 3–4, Balefire Fuel Pellets metas 0–4, Bismuth ZFB Pellets metas 0–2)
        add.accept(((RBMKPelletItem) ModItems.RBMK_PELLET_FLASHLEAD.get()).withState(1, 3)); // 1301 (4977/3)
        add.accept(((RBMKPelletItem) ModItems.RBMK_PELLET_FLASHLEAD.get()).withState(1, 4)); // 1302 (4977/4)
        add.accept(new ItemStack(ModItems.RBMK_PELLET_BALEFIRE.get())); // 1303 (4978/0)
        add.accept(((RBMKPelletItem) ModItems.RBMK_PELLET_BALEFIRE.get()).withState(1, 1)); // 1304 (4978/1)
        add.accept(((RBMKPelletItem) ModItems.RBMK_PELLET_BALEFIRE.get()).withState(1, 2)); // 1305 (4978/2)
        add.accept(((RBMKPelletItem) ModItems.RBMK_PELLET_BALEFIRE.get()).withState(1, 3)); // 1306 (4978/3)
        add.accept(((RBMKPelletItem) ModItems.RBMK_PELLET_BALEFIRE.get()).withState(1, 4)); // 1307 (4978/4)
        add.accept(new ItemStack(ModItems.RBMK_PELLET_ZFB_BISMUTH.get())); // 1308 (4979/0)
        add.accept(((RBMKPelletItem) ModItems.RBMK_PELLET_ZFB_BISMUTH.get()).withState(1, 1)); // 1309 (4979/1)
        add.accept(((RBMKPelletItem) ModItems.RBMK_PELLET_ZFB_BISMUTH.get()).withState(1, 2)); // 1310 (4979/2)
        // Batch 132 (Items 1311–1320: Bismuth ZFB Pellets metas 3–9, Pu-241 ZFB Pellets metas 0–2)
        add.accept(((RBMKPelletItem) ModItems.RBMK_PELLET_ZFB_BISMUTH.get()).withState(1, 3)); // 1311 (4979/3)
        add.accept(((RBMKPelletItem) ModItems.RBMK_PELLET_ZFB_BISMUTH.get()).withState(1, 4)); // 1312 (4979/4)
        add.accept(((RBMKPelletItem) ModItems.RBMK_PELLET_ZFB_BISMUTH.get()).withState(1, 5)); // 1313 (4979/5)
        add.accept(((RBMKPelletItem) ModItems.RBMK_PELLET_ZFB_BISMUTH.get()).withState(1, 6)); // 1314 (4979/6)
        add.accept(((RBMKPelletItem) ModItems.RBMK_PELLET_ZFB_BISMUTH.get()).withState(1, 7)); // 1315 (4979/7)
        add.accept(((RBMKPelletItem) ModItems.RBMK_PELLET_ZFB_BISMUTH.get()).withState(1, 8)); // 1316 (4979/8)
        add.accept(((RBMKPelletItem) ModItems.RBMK_PELLET_ZFB_BISMUTH.get()).withState(1, 9)); // 1317 (4979/9)
        add.accept(new ItemStack(ModItems.RBMK_PELLET_ZFB_PU241.get())); // 1318 (4980/0)
        add.accept(((RBMKPelletItem) ModItems.RBMK_PELLET_ZFB_PU241.get()).withState(1, 1)); // 1319 (4980/1)
        add.accept(((RBMKPelletItem) ModItems.RBMK_PELLET_ZFB_PU241.get()).withState(1, 2)); // 1320 (4980/2)
        // Batch 133 (Items 1321–1330: Pu-241 ZFB Pellets metas 3–9, Fuel Grade Americium ZFB Pellets metas 0–2)
        add.accept(((RBMKPelletItem) ModItems.RBMK_PELLET_ZFB_PU241.get()).withState(1, 3)); // 1321 (4980/3)
        add.accept(((RBMKPelletItem) ModItems.RBMK_PELLET_ZFB_PU241.get()).withState(1, 4)); // 1322 (4980/4)
        add.accept(((RBMKPelletItem) ModItems.RBMK_PELLET_ZFB_PU241.get()).withState(1, 5)); // 1323 (4980/5)
        add.accept(((RBMKPelletItem) ModItems.RBMK_PELLET_ZFB_PU241.get()).withState(1, 6)); // 1324 (4980/6)
        add.accept(((RBMKPelletItem) ModItems.RBMK_PELLET_ZFB_PU241.get()).withState(1, 7)); // 1325 (4980/7)
        add.accept(((RBMKPelletItem) ModItems.RBMK_PELLET_ZFB_PU241.get()).withState(1, 8)); // 1326 (4980/8)
        add.accept(((RBMKPelletItem) ModItems.RBMK_PELLET_ZFB_PU241.get()).withState(1, 9)); // 1327 (4980/9)
        add.accept(new ItemStack(ModItems.RBMK_PELLET_ZFB_AM_MIX.get())); // 1328 (4981/0)
        add.accept(((RBMKPelletItem) ModItems.RBMK_PELLET_ZFB_AM_MIX.get()).withState(1, 1)); // 1329 (4981/1)
        add.accept(((RBMKPelletItem) ModItems.RBMK_PELLET_ZFB_AM_MIX.get()).withState(1, 2)); // 1330 (4981/2)
        // Batch 134 (Items 1331–1340: Fuel Grade Americium ZFB Pellets metas 3–9, Digamma Fuel Pellets metas 0–2)
        add.accept(((RBMKPelletItem) ModItems.RBMK_PELLET_ZFB_AM_MIX.get()).withState(1, 3)); // 1331 (4981/3)
        add.accept(((RBMKPelletItem) ModItems.RBMK_PELLET_ZFB_AM_MIX.get()).withState(1, 4)); // 1332 (4981/4)
        add.accept(((RBMKPelletItem) ModItems.RBMK_PELLET_ZFB_AM_MIX.get()).withState(1, 5)); // 1333 (4981/5)
        add.accept(((RBMKPelletItem) ModItems.RBMK_PELLET_ZFB_AM_MIX.get()).withState(1, 6)); // 1334 (4981/6)
        add.accept(((RBMKPelletItem) ModItems.RBMK_PELLET_ZFB_AM_MIX.get()).withState(1, 7)); // 1335 (4981/7)
        add.accept(((RBMKPelletItem) ModItems.RBMK_PELLET_ZFB_AM_MIX.get()).withState(1, 8)); // 1336 (4981/8)
        add.accept(((RBMKPelletItem) ModItems.RBMK_PELLET_ZFB_AM_MIX.get()).withState(1, 9)); // 1337 (4981/9)
        add.accept(new ItemStack(ModItems.RBMK_PELLET_DRX.get())); // 1338 (4982/0)
        add.accept(((RBMKPelletItem) ModItems.RBMK_PELLET_DRX.get()).withState(1, 1)); // 1339 (4982/1)
        add.accept(((RBMKPelletItem) ModItems.RBMK_PELLET_DRX.get()).withState(1, 2)); // 1340 (4982/2)
        // Batch 135 (Items 1341–1350: Digamma Fuel Pellets metas 3–9, Watz Pellets metas 0–2)
        add.accept(((RBMKPelletItem) ModItems.RBMK_PELLET_DRX.get()).withState(1, 3)); // 1341 (4982/3)
        add.accept(((RBMKPelletItem) ModItems.RBMK_PELLET_DRX.get()).withState(1, 4)); // 1342 (4982/4)
        add.accept(((RBMKPelletItem) ModItems.RBMK_PELLET_DRX.get()).withState(1, 5)); // 1343 (4982/5)
        add.accept(((RBMKPelletItem) ModItems.RBMK_PELLET_DRX.get()).withState(1, 6)); // 1344 (4982/6)
        add.accept(((RBMKPelletItem) ModItems.RBMK_PELLET_DRX.get()).withState(1, 7)); // 1345 (4982/7)
        add.accept(((RBMKPelletItem) ModItems.RBMK_PELLET_DRX.get()).withState(1, 8)); // 1346 (4982/8)
        add.accept(((RBMKPelletItem) ModItems.RBMK_PELLET_DRX.get()).withState(1, 9)); // 1347 (4982/9)
        add.accept(new ItemStack(ModItems.WATZ_PELLET.get(com.hbm_m.item.nuclear.WatzPelletType.SCHRABIDIUM).get())); // 1348 (4983/0)
        add.accept(new ItemStack(ModItems.WATZ_PELLET.get(com.hbm_m.item.nuclear.WatzPelletType.HES).get())); // 1349 (4983/1)
        add.accept(new ItemStack(ModItems.WATZ_PELLET.get(com.hbm_m.item.nuclear.WatzPelletType.MES).get())); // 1350 (4983/2)
        // Batch 136 (Items 1351–1360: Watz Pellets metas 3–11, Depleted Watz Pellets meta 0)
        add.accept(new ItemStack(ModItems.WATZ_PELLET.get(com.hbm_m.item.nuclear.WatzPelletType.LES).get())); // 1351 (4983/3)
        add.accept(new ItemStack(ModItems.WATZ_PELLET.get(com.hbm_m.item.nuclear.WatzPelletType.HEN).get())); // 1352 (4983/4)
        add.accept(new ItemStack(ModItems.WATZ_PELLET.get(com.hbm_m.item.nuclear.WatzPelletType.MEU).get())); // 1353 (4983/5)
        add.accept(new ItemStack(ModItems.WATZ_PELLET.get(com.hbm_m.item.nuclear.WatzPelletType.MEP).get())); // 1354 (4983/6)
        add.accept(new ItemStack(ModItems.WATZ_PELLET.get(com.hbm_m.item.nuclear.WatzPelletType.LEAD).get())); // 1355 (4983/7)
        add.accept(new ItemStack(ModItems.WATZ_PELLET.get(com.hbm_m.item.nuclear.WatzPelletType.BORON).get())); // 1356 (4983/8)
        add.accept(new ItemStack(ModItems.WATZ_PELLET.get(com.hbm_m.item.nuclear.WatzPelletType.DU).get())); // 1357 (4983/9)
        add.accept(new ItemStack(ModItems.WATZ_PELLET.get(com.hbm_m.item.nuclear.WatzPelletType.NQD).get())); // 1358 (4983/10)
        add.accept(new ItemStack(ModItems.WATZ_PELLET.get(com.hbm_m.item.nuclear.WatzPelletType.NQR).get())); // 1359 (4983/11)
        add.accept(new ItemStack(ModItems.WATZ_PELLET_DEPLETED.get(com.hbm_m.item.nuclear.WatzPelletType.SCHRABIDIUM).get())); // 1360 (4984/0)
        // Batch 137 (Items 1361–1370: Depleted Watz Pellets metas 1–10)
        add.accept(new ItemStack(ModItems.WATZ_PELLET_DEPLETED.get(com.hbm_m.item.nuclear.WatzPelletType.HES).get())); // 1361 (4984/1)
        add.accept(new ItemStack(ModItems.WATZ_PELLET_DEPLETED.get(com.hbm_m.item.nuclear.WatzPelletType.MES).get())); // 1362 (4984/2)
        add.accept(new ItemStack(ModItems.WATZ_PELLET_DEPLETED.get(com.hbm_m.item.nuclear.WatzPelletType.LES).get())); // 1363 (4984/3)
        add.accept(new ItemStack(ModItems.WATZ_PELLET_DEPLETED.get(com.hbm_m.item.nuclear.WatzPelletType.HEN).get())); // 1364 (4984/4)
        add.accept(new ItemStack(ModItems.WATZ_PELLET_DEPLETED.get(com.hbm_m.item.nuclear.WatzPelletType.MEU).get())); // 1365 (4984/5)
        add.accept(new ItemStack(ModItems.WATZ_PELLET_DEPLETED.get(com.hbm_m.item.nuclear.WatzPelletType.MEP).get())); // 1366 (4984/6)
        add.accept(new ItemStack(ModItems.WATZ_PELLET_DEPLETED.get(com.hbm_m.item.nuclear.WatzPelletType.LEAD).get())); // 1367 (4984/7)
        add.accept(new ItemStack(ModItems.WATZ_PELLET_DEPLETED.get(com.hbm_m.item.nuclear.WatzPelletType.BORON).get())); // 1368 (4984/8)
        add.accept(new ItemStack(ModItems.WATZ_PELLET_DEPLETED.get(com.hbm_m.item.nuclear.WatzPelletType.DU).get())); // 1369 (4984/9)
        add.accept(new ItemStack(ModItems.WATZ_PELLET_DEPLETED.get(com.hbm_m.item.nuclear.WatzPelletType.NQD).get())); // 1370 (4984/10)
        // Batch 138 (Items 1371–1380: Depleted Watz Pellet meta 11, Empty ICF Pellet, ICF Pellets, Depleted ICF Pellet, Hot Graphite Chunk, Broken Metal Bar)
        add.accept(new ItemStack(ModItems.WATZ_PELLET_DEPLETED.get(com.hbm_m.item.nuclear.WatzPelletType.NQR).get())); // 1371 (4984/11)
        add.accept(new ItemStack(ModItems.ICF_PELLET_EMPTY.get())); // 1372 (4985/0)
        add.accept(com.hbm_m.item.machine.ItemICFPellet.setup(com.hbm_m.item.machine.ItemICFPellet.EnumICFFuel.DEUTERIUM, com.hbm_m.item.machine.ItemICFPellet.EnumICFFuel.TRITIUM, false)); // 1373 (4986/0)
        add.accept(com.hbm_m.item.machine.ItemICFPellet.setup(com.hbm_m.item.machine.ItemICFPellet.EnumICFFuel.HELIUM3, com.hbm_m.item.machine.ItemICFPellet.EnumICFFuel.HELIUM4, false)); // 1374 (4986/0)
        add.accept(com.hbm_m.item.machine.ItemICFPellet.setup(com.hbm_m.item.machine.ItemICFPellet.EnumICFFuel.LITHIUM, com.hbm_m.item.machine.ItemICFPellet.EnumICFFuel.OXYGEN, false)); // 1375 (4986/0)
        add.accept(com.hbm_m.item.machine.ItemICFPellet.setup(com.hbm_m.item.machine.ItemICFPellet.EnumICFFuel.SODIUM, com.hbm_m.item.machine.ItemICFPellet.EnumICFFuel.CHLORINE, true)); // 1376 (4986/0)
        add.accept(com.hbm_m.item.machine.ItemICFPellet.setup(com.hbm_m.item.machine.ItemICFPellet.EnumICFFuel.BERYLLIUM, com.hbm_m.item.machine.ItemICFPellet.EnumICFFuel.CALCIUM, true)); // 1377 (4986/0)
        add.accept(new ItemStack(ModItems.ICF_PELLET_DEPLETED.get())); // 1378 (4987/0)
        add.accept(new ItemStack(ModItems.DEBRIS_GRAPHITE.get())); // 1379 (4988/0)
        add.accept(new ItemStack(ModItems.DEBRIS_METAL.get())); // 1380 (4989/0)
        // Batch 139 (Items 1381–1388: RBMK Fuel Chunk, Broken Concrete, Heat Exchanger Piece, Walkway Shrapnel, Broken ZIRNOX Fuel Element, Archineer's Wrench, Tungsten Reacher, Dyatlov Meltdown Tool)
        add.accept(new ItemStack(ModItems.DEBRIS_FUEL.get())); // 1381 (4990/0)
        add.accept(new ItemStack(ModItems.DEBRIS_CONCRETE.get())); // 1382 (4991/0)
        add.accept(new ItemStack(ModItems.DEBRIS_EXCHANGER.get())); // 1383 (4992/0)
        add.accept(new ItemStack(ModItems.DEBRIS_SHRAPNEL.get())); // 1384 (4993/0)
        add.accept(new ItemStack(ModItems.DEBRIS_ELEMENT.get())); // 1385 (4994/0)
        add.accept(new ItemStack(ModItems.WRENCH_ARCHINEER.get())); // 1386 (5406/0)
        add.accept(new ItemStack(ModItems.REACHER.get())); // 1387 (5728/0)
        add.accept(new ItemStack(ModItems.MELTDOWN_TOOL.get())); // 1388 (5730/0)
    }

    /** populateTemplatesTab: порядок из оригинального 1.7.10 (ModBlocks/ModItems, вкладка template); отсутствующие в порте предметы пропущены. */
    public static void populateTemplatesTab(BiConsumer<ItemStack, CreativeModeTab.TabVisibility> acceptor) {
        Consumer<ItemStack> add = stack -> acceptor.accept(stack, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);

        // --- CHUNK 1: Items 1-100 (worker_m2_template_c1) ---
        // 1-10: Blueprints (ItemBlueprints - base item & dynamic recipe pools via ClientSetup)
        add.accept(new ItemStack(ModItems.BLUEPRINTS.get()));
        EnvExecutor.runInEnv(Env.CLIENT, () -> () -> {
            ClientSetup.addTemplatesClient(add);
        });

        // 11-20: Blueprint folders (meta 0 & 1) and Siren Tracks (cassettes 1-7)
        add.accept(new ItemStack(ModItems.BLUEPRINT_FOLDER.get()));
        add.accept(new ItemStack(ModItems.BLUEPRINT_FOLDER_DISCOVER.get()));
        add.accept(new ItemStack(ModItems.CASSETTE_HATCH.get()));
        add.accept(new ItemStack(ModItems.CASSETTE_AUTOPILOT.get()));
        add.accept(new ItemStack(ModItems.CASSETTE_AMS_SIREN.get()));
        add.accept(new ItemStack(ModItems.CASSETTE_BLAST_DOOR.get()));
        add.accept(new ItemStack(ModItems.CASSETTE_APC_LOOP.get()));
        add.accept(new ItemStack(ModItems.CASSETTE_KLAXON.get()));
        add.accept(new ItemStack(ModItems.CASSETTE_KLAXON_A.get()));

        // 21-30: Siren Tracks (cassettes 8-17)
        add.accept(new ItemStack(ModItems.CASSETTE_KLAXON_B.get()));
        add.accept(new ItemStack(ModItems.CASSETTE_REGULAR_SIREN.get()));
        add.accept(new ItemStack(ModItems.CASSETTE_CLASSIC_SIREN.get()));
        add.accept(new ItemStack(ModItems.CASSETTE_BANK_ALARM.get()));
        add.accept(new ItemStack(ModItems.CASSETTE_BEEP_SIREN.get()));
        add.accept(new ItemStack(ModItems.CASSETTE_CONTAINER_ALARM.get()));
        add.accept(new ItemStack(ModItems.CASSETTE_SWEEP_SIREN.get()));
        add.accept(new ItemStack(ModItems.CASSETTE_STRIDER_SIREN.get()));
        add.accept(new ItemStack(ModItems.CASSETTE_AIR_RAID.get()));
        add.accept(new ItemStack(ModItems.CASSETTE_NOSTROMO_SIREN.get()));

        // 31-40: Siren Tracks (cassettes 18-20) and Fluid Identifiers (fluids 1-7: air .. steam)
        add.accept(new ItemStack(ModItems.CASSETTE_EAS_ALARM.get()));
        add.accept(new ItemStack(ModItems.CASSETTE_APC_PASS.get()));
        add.accept(new ItemStack(ModItems.CASSETTE_RAZORTRAIN.get()));
        // 34-100: Multi Fluid Identifiers (first 67 fluids in Fluids.metaOrder canonical order)
        // 41-50: fluids 8-17 (hotsteam .. perfluoromethyl_hot)
        // 51-60: fluids 18-27 (cryogel .. thorium_salt)
        // 61-70: fluids 28-37 (thorium_salt_hot .. chlorine)
        // 71-80: fluids 38-47 (mercury .. hotcrackoil)
        // 81-90: fluids 48-57 (hotcrackoil_ds .. lightoil_ds)
        // 91-100: fluids 58-67 (lightoil_crack .. gas)
        List<ModFluids.FluidEntry> orderedFluids = HbmFluidRegistry.getOrderedFluids();
        int chunk1FluidLimit = Math.min(67, orderedFluids.size());
        for (int i = 0; i < chunk1FluidLimit; i++) {
            ModFluids.FluidEntry entry = orderedFluids.get(i);
            if (com.hbm_m.inventory.fluid.FluidType.forFluid(entry.getSource()).hasNoID()) continue;
            ItemStack idStack = new ItemStack(ModItems.FLUID_IDENTIFIER.get());
            FluidIdentifierItem.setType(idStack, HbmFluidRegistry.getFluidName(entry.getSource()), true);
            add.accept(idStack);
        }

        // --- CHUNK 2: Items 101-200 (worker_m2_template_c2) ---
        // 101-110: Remaining fluid identifiers: fluids 68-77 (gas_coker .. reformgas)
        // 111-120: fluids 78-87 (diesel .. gasoline_leaded)
        // 121-130: fluids 88-97 (coalgas .. nitan)
        // 131-140: fluids 98-107 (dhc .. sulfuric_acid)
        // 141-150: fluids 108-117 (nitric_acid .. watz)
        // 151-160: fluids 118-127 (redmud .. calcium_solution)
        // 161-170: fluids 128-137 (sodium_aluminate .. estradiol)
        // 171-180: fluids 138-147 (nitroglycerin .. pheromone_m)
        for (int i = chunk1FluidLimit; i < orderedFluids.size(); i++) {
            ModFluids.FluidEntry entry = orderedFluids.get(i);
            if (com.hbm_m.inventory.fluid.FluidType.forFluid(entry.getSource()).hasNoID()) continue;
            ItemStack idStack = new ItemStack(ModItems.FLUID_IDENTIFIER.get());
            FluidIdentifierItem.setType(idStack, HbmFluidRegistry.getFluidName(entry.getSource()), true);
            add.accept(idStack);
        }
        // 181: fluid 148 CUSTOM_DEMO
        ItemStack demoId = new ItemStack(ModItems.FLUID_IDENTIFIER.get());
        FluidIdentifierItem.setType(demoId, "custom_demo", true);
        add.accept(demoId);

        // 182-190: Fluid Ducts: ducts 1-9 (air .. superhotsteam)
        // 191-200: Fluid Ducts: ducts 10-19 (ultrahotsteam .. mug)
        int chunk2DuctLimit = 19;
        int ductCount = 0;
        for (ModFluids.FluidEntry entry : orderedFluids) {
            if (com.hbm_m.inventory.fluid.FluidType.forFluid(entry.getSource()).hasNoID()) continue;
            add.accept(com.hbm_m.item.liquids.FluidDuctItem.createStack(ModItems.FLUID_DUCT.get(), entry));
            ductCount++;
            if (ductCount >= chunk2DuctLimit) break;
        }

        // --- CHUNK 3: Items 201-300 (worker_m2_template_c3) ---
        // 201-210: Fluid Ducts: ducts 20-29 (mug_hot .. thorium_salt_depleted)
        // 211-220: Fluid Ducts: ducts 30-39 (hydrogen .. crude_oil)
        // 221-230: Fluid Ducts: ducts 40-49 (oil_ds .. heavyoil)
        // 231-240: Fluid Ducts: ducts 50-59 (heavyoil_vacuum .. lightoil_vacuum)
        // 241-250: Fluid Ducts: ducts 60-69 (bitumen .. petroleum)
        // 251-260: Fluid Ducts: ducts 70-79 (sourgas .. diesel_reform)
        // 261-270: Fluid Ducts: ducts 80-89 (diesel_crack .. coalgas_leaded)
        // 271-280: Fluid Ducts: ducts 90-99 (coalcreosote .. balefire)
        // 281-290: Fluid Ducts: ducts 100-109 (salient .. solvent)
        // 291-300: Fluid Ducts: ducts 110-119 (radiosolvent .. fullerene)
        int chunk3DuctLimit = 119;
        int chunk3DuctCount = 0;
        for (ModFluids.FluidEntry entry : orderedFluids) {
            if (com.hbm_m.inventory.fluid.FluidType.forFluid(entry.getSource()).hasNoID()) continue;
            chunk3DuctCount++;
            if (chunk3DuctCount > chunk2DuctLimit && chunk3DuctCount <= chunk3DuctLimit) {
                add.accept(com.hbm_m.item.liquids.FluidDuctItem.createStack(ModItems.FLUID_DUCT.get(), entry));
            }
        }

        // --- CHUNK 4: Items 301-329 (worker_m2_template_c4) ---
        // 301-310: Fluid Ducts: ducts 120-129 (egg .. bauxite_solution)
        // 311-320: Fluid Ducts: ducts 130-139 (alumina .. amat)
        // 321-328: Fluid Ducts: ducts 140-147 (aschrab .. pheromone_m)
        // 329: duct 148 CUSTOM_DEMO: add.accept(com.hbm_m.item.liquids.FluidDuctItem.createStack(ModItems.FLUID_DUCT.get(), ModFluids.CUSTOM_DEMO));
        int chunk4DuctCount = 0;
        for (ModFluids.FluidEntry entry : orderedFluids) {
            if (com.hbm_m.inventory.fluid.FluidType.forFluid(entry.getSource()).hasNoID()) continue;
            chunk4DuctCount++;
            if (chunk4DuctCount > chunk3DuctLimit) {
                add.accept(com.hbm_m.item.liquids.FluidDuctItem.createStack(ModItems.FLUID_DUCT.get(), entry));
            }
        }

        // --- Modernized Extras ---
        // Modernized template & blueprint utility items
        add.accept(new ItemStack(ModItems.ASSEMBLY_TEMPLATE.get()));
        add.accept(new ItemStack(ModItems.TEMPLATE_FOLDER.get()));
        add.accept(new ItemStack(ModItems.BLUEPRINT_FOLDER_SECRET.get()));

        // Modernized fluid duct variants (empty base/colored/silver ducts and filled colored/silver styles)
        add.accept(new ItemStack(ModItems.FLUID_DUCT.get()));
        add.accept(new ItemStack(ModItems.FLUID_DUCT_COLORED.get()));
        add.accept(new ItemStack(ModItems.FLUID_DUCT_SILVER.get()));
        for (ModFluids.FluidEntry entry : orderedFluids) {
            if (com.hbm_m.inventory.fluid.FluidType.forFluid(entry.getSource()).hasNoID()) continue;
            add.accept(com.hbm_m.item.liquids.FluidDuctItem.createStack(ModItems.FLUID_DUCT_COLORED.get(), entry));
            add.accept(com.hbm_m.item.liquids.FluidDuctItem.createStack(ModItems.FLUID_DUCT_SILVER.get(), entry));
        }
    }

    /** populateBlocksTab: порядок из оригинального 1.7.10 (ModBlocks/ModItems, вкладка blocks); отсутствующие в порте предметы пропущены. */
    public static void populateBlocksTab(BiConsumer<ItemStack, CreativeModeTab.TabVisibility> acceptor) {
        Consumer<ItemStack> add = stack -> acceptor.accept(stack, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);

        // --- CHUNK 1: Items 1-100 (worker_m3_b1) ---
        // 1-10: Uranium, Scorched Uranium, Thorium, Titanium, Sulfur, Niter, Copper, Tungsten, Aluminium, Fluorite Ores
        add.accept(new ItemStack(ModBlocks.URANIUM_ORE.get()));
        add.accept(new ItemStack(ModBlocks.ORE_URANIUM_SCORCHED.get()));
        add.accept(new ItemStack(ModBlocks.THORIUM_ORE.get()));
        add.accept(new ItemStack(ModBlocks.TITANIUM_ORE.get()));
        add.accept(new ItemStack(ModBlocks.SULFUR_ORE.get()));
        add.accept(new ItemStack(ModBlocks.NITER_ORE.get()));
        add.accept(new ItemStack(ModBlocks.ORE_COPPER.get()));
        add.accept(new ItemStack(ModBlocks.TUNGSTEN_ORE.get()));
        add.accept(new ItemStack(ModBlocks.ORE_ALUMINIUM.get()));
        add.accept(new ItemStack(ModBlocks.FLUORITE_ORE.get()));

        // 11-20: Beryllium, Lead, Oil Deposits, Tar Sand, Lignite, Asbestos, Schrabidium, Australian, Rare Earth Ores
        add.accept(new ItemStack(ModBlocks.BERYLLIUM_ORE.get()));
        add.accept(new ItemStack(ModBlocks.LEAD_ORE.get()));
        add.accept(new ItemStack(ModBlocks.ORE_OIL.get()));
        add.accept(new ItemStack(ModBlocks.ORE_OIL_EMPTY.get()));
        add.accept(new ItemStack(ModBlocks.ORE_OIL_SAND.get()));
        add.accept(new ItemStack(ModBlocks.LIGNITE_ORE.get()));
        add.accept(new ItemStack(ModBlocks.ASBESTOS_ORE.get()));
        add.accept(new ItemStack(ModBlocks.SCHRABIDIUM_ORE.get()));
        add.accept(new ItemStack(ModBlocks.ORE_AUSTRALIUM.get()));
        add.accept(new ItemStack(ModBlocks.ORE_RARE.get()));

        // 21-30: Cobalt, Cinnabar, Coltan Ores, Clusters (Iron, Titanium, Aluminium, Copper), Bedrock Oil, Nether Coal & Smoldering
        add.accept(new ItemStack(ModBlocks.COBALT_ORE.get()));
        add.accept(new ItemStack(ModBlocks.ORE_CINNEBAR.get()));
        add.accept(new ItemStack(ModBlocks.ORE_COLTAN.get()));
        add.accept(new ItemStack(ModBlocks.CLUSTER_IRON.get()));
        add.accept(new ItemStack(ModBlocks.CLUSTER_TITANIUM.get()));
        add.accept(new ItemStack(ModBlocks.CLUSTER_ALUMINIUM.get()));
        add.accept(new ItemStack(ModBlocks.CLUSTER_COPPER.get()));
        add.accept(new ItemStack(ModBlocks.ORE_BEDROCK_OIL.get()));
        add.accept(new ItemStack(ModBlocks.ORE_NETHER_COAL.get()));
        add.accept(new ItemStack(ModBlocks.ORE_NETHER_SMOLDERING.get()));

        // 31-40: Nether Ores (Uranium, Scorched, Plutonium, Tungsten, Sulfur, Fire, Cobalt, Schrabidium), Meteor Ores (Iron, Copper)
        add.accept(new ItemStack(ModBlocks.ORE_NETHER_URANIUM.get()));
        add.accept(new ItemStack(ModBlocks.ORE_NETHER_URANIUM_SCORCHED.get()));
        add.accept(new ItemStack(ModBlocks.ORE_NETHER_PLUTONIUM.get()));
        add.accept(new ItemStack(ModBlocks.ORE_NETHER_TUNGSTEN.get()));
        add.accept(new ItemStack(ModBlocks.ORE_NETHER_SULFUR.get()));
        add.accept(new ItemStack(ModBlocks.ORE_NETHER_FIRE.get()));
        add.accept(new ItemStack(ModBlocks.ORE_NETHER_COBALT.get()));
        add.accept(new ItemStack(ModBlocks.SCHRABIDIUM_ORE_NETHER.get()));
        add.accept(new ItemStack(ModBlocks.ORE_METEOR_IRON.get()));
        add.accept(new ItemStack(ModBlocks.ORE_METEOR_COPPER.get()));

        // 41-50: Meteor Ores (Aluminium, Rare Earth, Cobalt), Schist Ores (Iron, Gold, Uranium, Scorched, Copper, Asbestos, Lithium)
        add.accept(new ItemStack(ModBlocks.ORE_METEOR_ALUMINIUM.get()));
        add.accept(new ItemStack(ModBlocks.ORE_METEOR_RAREEARTH.get()));
        add.accept(new ItemStack(ModBlocks.ORE_METEOR_COBALT.get()));
        add.accept(new ItemStack(ModBlocks.ORE_GNEISS_IRON.get()));
        add.accept(new ItemStack(ModBlocks.ORE_GNEISS_GOLD.get()));
        add.accept(new ItemStack(ModBlocks.ORE_GNEISS_URANIUM.get()));
        add.accept(new ItemStack(ModBlocks.ORE_GNEISS_URANIUM_SCORCHED.get()));
        add.accept(new ItemStack(ModBlocks.ORE_GNEISS_COPPER.get()));
        add.accept(new ItemStack(ModBlocks.ORE_GNEISS_ASBESTOS.get()));
        add.accept(new ItemStack(ModBlocks.GNEISS_LITHIUM_ORE.get()));

        // 51-60: Schist Ores (Schrabidium, Rare Earth, Gas Shale), Depth Ores (Cinnabar, Zirconium, Borax), Depth Clusters, Alexandrite
        add.accept(new ItemStack(ModBlocks.SCHRABIDIUM_ORE_GNEISS.get()));
        add.accept(new ItemStack(ModBlocks.ORE_GNEISS_RARE.get()));
        add.accept(new ItemStack(ModBlocks.ORE_GNEISS_GAS.get()));
        add.accept(new ItemStack(ModBlocks.ORE_DEPTH_CINNEBAR.get()));
        add.accept(new ItemStack(ModBlocks.ORE_DEPTH_ZIRCONIUM.get()));
        add.accept(new ItemStack(ModBlocks.ORE_DEPTH_BORAX.get()));
        add.accept(new ItemStack(ModBlocks.CLUSTER_DEPTH_IRON.get()));
        add.accept(new ItemStack(ModBlocks.CLUSTER_DEPTH_TITANIUM.get()));
        add.accept(new ItemStack(ModBlocks.CLUSTER_DEPTH_TUNGSTEN.get()));
        add.accept(new ItemStack(ModBlocks.ORE_ALEXANDRITE.get()));

        // 61-70: Nether Depth Neodymium, Basalt Ores (Sulfur, Fluorite, Asbestos, Gem, Molysite), Trixite, Geothermal Vent, Stones (Sulfur, Asbestos)
        add.accept(new ItemStack(ModBlocks.ORE_DEPTH_NETHER_NEODYMIUM.get()));
        add.accept(new ItemStack(ModBlocks.ORE_BASALT_SULFUR.get()));
        add.accept(new ItemStack(ModBlocks.ORE_BASALT_FLUORITE.get()));
        add.accept(new ItemStack(ModBlocks.ORE_BASALT_ASBESTOS.get()));
        add.accept(new ItemStack(ModBlocks.ORE_BASALT_GEM.get()));
        add.accept(new ItemStack(ModBlocks.ORE_BASALT_MOLYSITE.get()));
        add.accept(new ItemStack(ModBlocks.TIKITE_ORE.get()));
        add.accept(new ItemStack(ModBlocks.ORE_VOLCANO.get()));
        add.accept(new ItemStack(ModBlocks.STONE_RESOURCE_SULFUR.get()));
        add.accept(new ItemStack(ModBlocks.STONE_RESOURCE_ASBESTOS.get()));

        // 71-80: Stones (Hematite, Malachite, Limestone, Bauxite), Stalagmites (Sulfur, Asbestos, Ice, Snow, Glyphid 1-2)
        add.accept(new ItemStack(ModBlocks.STONE_RESOURCE_HEMATITE.get()));
        add.accept(new ItemStack(ModBlocks.STONE_RESOURCE_MALACHITE.get()));
        add.accept(new ItemStack(ModBlocks.STONE_RESOURCE_LIMESTONE.get()));
        add.accept(new ItemStack(ModBlocks.STONE_RESOURCE_BAUXITE.get()));
        add.accept(new ItemStack(ModBlocks.STALAGMITE_SULFUR.get()));
        add.accept(new ItemStack(ModBlocks.STALAGMITE_ASBESTOS.get()));
        add.accept(new ItemStack(ModBlocks.STALAGMITE_ICE.get()));
        add.accept(new ItemStack(ModBlocks.STALAGMITE_SNOW.get()));
        add.accept(new ItemStack(ModBlocks.STALAGMITE_GLYPHID1.get()));
        add.accept(new ItemStack(ModBlocks.STALAGMITE_GLYPHID2.get()));

        // 81-90: Stalagmites (Glyphid 3), Stalactites (Sulfur, Asbestos, Ice, Snow, Glyphid 1-3), Desert & Woodland Stones
        add.accept(new ItemStack(ModBlocks.STALAGMITE_GLYPHID3.get()));
        add.accept(new ItemStack(ModBlocks.STALACTITE_SULFUR.get()));
        add.accept(new ItemStack(ModBlocks.STALACTITE_ASBESTOS.get()));
        add.accept(new ItemStack(ModBlocks.STALACTITE_ICE.get()));
        add.accept(new ItemStack(ModBlocks.STALACTITE_SNOW.get()));
        add.accept(new ItemStack(ModBlocks.STALACTITE_GLYPHID1.get()));
        add.accept(new ItemStack(ModBlocks.STALACTITE_GLYPHID2.get()));
        add.accept(new ItemStack(ModBlocks.STALACTITE_GLYPHID3.get()));
        add.accept(new ItemStack(ModBlocks.STONE_BIOME_DESERT.get()));
        add.accept(new ItemStack(ModBlocks.STONE_BIOME_WOODLAND.get()));

        // 91-100: Porous Stone, Graphitic Schist, Schist Bricks/Tiles/Chiseled, Depth Rock/Bricks/Tiles, Nether Depth Rock/Bricks
        add.accept(new ItemStack(ModBlocks.STONE_POROUS.get()));
        add.accept(new ItemStack(ModBlocks.STONE_GNEISS.get()));
        add.accept(new ItemStack(ModBlocks.GNEISS_BRICK.get()));
        add.accept(new ItemStack(ModBlocks.GNEISS_TILE.get()));
        add.accept(new ItemStack(ModBlocks.GNEISS_CHISELED.get()));
        add.accept(new ItemStack(ModBlocks.STONE_DEPTH.get()));
        add.accept(new ItemStack(ModBlocks.DEPTH_BRICK.get()));
        add.accept(new ItemStack(ModBlocks.DEPTH_TILES.get()));
        add.accept(new ItemStack(ModBlocks.STONE_DEPTH_NETHER.get()));
        add.accept(new ItemStack(ModBlocks.DEPTH_NETHER_BRICK.get()));

        // --- CHUNK 2: Items 101-200 (worker_m3_b2) ---
        // 101-110: Nether Depth Tiles, DNT Depth Bricks, Basalts (Plain, Smooth, Brick, Polished, Tiles), Uranium Blocks (Uranium, U-233, U-235)
        add.accept(new ItemStack(ModBlocks.DEPTH_NETHER_TILES.get()));
        add.accept(new ItemStack(ModBlocks.DEPTH_DNT.get()));
        add.accept(new ItemStack(ModBlocks.BASALT.get()));
        add.accept(new ItemStack(ModBlocks.BASALT_SMOOTH.get()));
        add.accept(new ItemStack(ModBlocks.BASALT_BRICK.get()));
        add.accept(new ItemStack(ModBlocks.BASALT_POLISHED.get()));
        add.accept(new ItemStack(ModBlocks.BASALT_TILES.get()));
        add.accept(new ItemStack(ModBlocks.URANIUM_BLOCK.get()));
        add.accept(new ItemStack(ModBlocks.getIngotBlock(ModMaterials.URANIUM233).get()));
        add.accept(new ItemStack(ModBlocks.getIngotBlock(ModMaterials.URANIUM235).get()));

        // 111-120: Uranium Blocks (U-238, Fuel), Neptunium, Polonium-210, MOX Fuel, Plutonium Blocks (Plutonium, Pu-238, Pu-239, Pu-240, Reactor Grade Mix)
        add.accept(new ItemStack(ModBlocks.getIngotBlock(ModMaterials.URANIUM238).get()));
        add.accept(new ItemStack(ModBlocks.getIngotBlock(ModMaterials.URANIUM_FUEL).get()));
        add.accept(new ItemStack(ModBlocks.getIngotBlock(ModMaterials.NEPTUNIUM).get()));
        add.accept(new ItemStack(ModBlocks.getIngotBlock(ModMaterials.POLONIUM).get()));
        add.accept(new ItemStack(ModBlocks.getIngotBlock(ModMaterials.MOX_FUEL).get()));
        add.accept(new ItemStack(ModBlocks.PLUTONIUM_BLOCK.get()));
        add.accept(new ItemStack(ModBlocks.getIngotBlock(ModMaterials.PLUTONIUM238).get()));
        add.accept(new ItemStack(ModBlocks.getIngotBlock(ModMaterials.PLUTONIUM239).get()));
        add.accept(new ItemStack(ModBlocks.getIngotBlock(ModMaterials.PLUTONIUM240).get()));
        add.accept(new ItemStack(ModBlocks.BLOCK_PU_MIX.get()));

        // 121-130: Plutonium Fuel, Thorium-232, Thorium Fuel, Titanium, Sulfur, Niter, Copper, Red Copper, Tungsten, Aluminium
        add.accept(new ItemStack(ModBlocks.PLUTONIUM_FUEL_BLOCK.get()));
        add.accept(new ItemStack(ModBlocks.getIngotBlock(ModMaterials.THORIUM).get()));
        add.accept(new ItemStack(ModBlocks.getIngotBlock(ModMaterials.THORIUM_FUEL).get()));
        add.accept(new ItemStack(ModBlocks.getIngotBlock(ModMaterials.TITANIUM).get()));
        add.accept(new ItemStack(ModBlocks.BLOCK_SULFUR.get()));
        add.accept(new ItemStack(ModBlocks.BLOCK_NITER.get()));
        add.accept(new ItemStack(ModBlocks.BLOCK_COPPER.get()));
        add.accept(new ItemStack(ModBlocks.BLOCK_RED_COPPER.get()));
        add.accept(new ItemStack(ModBlocks.getIngotBlock(ModMaterials.TUNGSTEN).get()));
        add.accept(new ItemStack(ModBlocks.BLOCK_ALUMINIUM.get()));

        // 131-140: Fluorite, Beryllium, Cobalt, Steel, Technetium Steel, Cadmium Steel, Lead, Bismuth, Cadmium, Coltan
        add.accept(new ItemStack(ModBlocks.BLOCK_FLUORITE.get()));
        add.accept(new ItemStack(ModBlocks.getIngotBlock(ModMaterials.BERYLLIUM).get()));
        add.accept(new ItemStack(ModBlocks.getIngotBlock(ModMaterials.COBALT).get()));
        add.accept(new ItemStack(ModBlocks.getIngotBlock(ModMaterials.STEEL).get()));
        add.accept(new ItemStack(ModBlocks.getIngotBlock(ModMaterials.TCALLOY).get()));
        add.accept(new ItemStack(ModBlocks.getIngotBlock(ModMaterials.CDALLOY).get()));
        add.accept(new ItemStack(ModBlocks.getIngotBlock(ModMaterials.LEAD).get()));
        add.accept(new ItemStack(ModBlocks.getIngotBlock(ModMaterials.BISMUTH).get()));
        add.accept(new ItemStack(ModBlocks.getIngotBlock(ModMaterials.CADMIUM).get()));
        add.accept(new ItemStack(ModBlocks.BLOCK_COLTAN.get()));

        // 141-150: Tantalum, Niobium, Lithium, Zirconium, White Phosphorus, Red Phosphorus, Yellowcake, Scrap, Electrical Scrap, Fallout
        add.accept(new ItemStack(ModBlocks.BLOCK_TANTALIUM.get()));
        add.accept(new ItemStack(ModBlocks.getIngotBlock(ModMaterials.NIOBIUM).get()));
        add.accept(new ItemStack(ModBlocks.BLOCK_LITHIUM.get()));
        add.accept(new ItemStack(ModBlocks.getIngotBlock(ModMaterials.ZIRCONIUM).get()));
        add.accept(new ItemStack(ModBlocks.BLOCK_WHITE_PHOSPHORUS.get()));
        add.accept(new ItemStack(ModBlocks.BLOCK_RED_PHOSPHORUS.get()));
        add.accept(new ItemStack(ModBlocks.BLOCK_YELLOWCAKE.get()));
        add.accept(new ItemStack(ModBlocks.BLOCK_SCRAP.get()));
        add.accept(new ItemStack(ModBlocks.BLOCK_ELECTRICAL_SCRAP.get()));
        add.accept(new ItemStack(ModBlocks.BLOCK_FALLOUT.get()));

        // 151-160: Foam, Graphite, Coal Coke, Lignite Coke, Petroleum Coke, Boron, Insulation, Fiberglass, Asbestos, Trinitite
        add.accept(new ItemStack(ModBlocks.BLOCK_FOAM.get()));
        add.accept(new ItemStack(ModBlocks.BLOCK_GRAPHITE.get()));
        add.accept(new ItemStack(ModBlocks.BLOCK_COAL_COKE.get()));
        add.accept(new ItemStack(ModBlocks.BLOCK_LIGNITE_COKE.get()));
        add.accept(new ItemStack(ModBlocks.BLOCK_PETROLEUM_COKE.get()));
        add.accept(new ItemStack(ModBlocks.getIngotBlock(ModMaterials.BORON).get()));
        add.accept(new ItemStack(ModBlocks.BLOCK_INSULATOR.get()));
        add.accept(new ItemStack(ModBlocks.BLOCK_FIBERGLASS.get()));
        add.accept(new ItemStack(ModBlocks.BLOCK_ASBESTOS.get()));
        add.accept(new ItemStack(ModBlocks.BLOCK_TRINITITE.get()));

        // 161-170: Nuclear Waste, Painted Nuclear Waste, Vitrified Nuclear Waste, Ancient Scrap Metal, Corium, Corebblestone, Schraranium, Schrabidium, Ferric Schrabidate, Solinium
        add.accept(new ItemStack(ModBlocks.BLOCK_WASTE.get()));
        add.accept(new ItemStack(ModBlocks.BLOCK_WASTE_PAINTED.get()));
        add.accept(new ItemStack(ModBlocks.BLOCK_WASTE_VITRIFIED.get()));
        add.accept(new ItemStack(ModBlocks.ANCIENT_SCRAP.get()));
        add.accept(new ItemStack(ModBlocks.BLOCK_CORIUM.get()));
        add.accept(new ItemStack(ModBlocks.BLOCK_CORIUM_COBBLE.get()));
        add.accept(new ItemStack(ModBlocks.getIngotBlock(ModMaterials.SCHRARANIUM).get()));
        add.accept(new ItemStack(ModBlocks.getIngotBlock(ModMaterials.SCHRABIDIUM).get()));
        add.accept(new ItemStack(ModBlocks.getIngotBlock(ModMaterials.SCHRABIDATE).get()));
        add.accept(new ItemStack(ModBlocks.getIngotBlock(ModMaterials.SOLINIUM).get()));

        // 171-180: Schrabidium Fuel, Euphemium, Schrabidium Cluster, Euphemium Etched Cluster, Dineutronium, Magnetized Tungsten, CMB Steel, Desh, High-Speed Steel, Starmetal
        add.accept(new ItemStack(ModBlocks.getIngotBlock(ModMaterials.SCHRABIDIUM_FUEL).get()));
        add.accept(new ItemStack(ModBlocks.getIngotBlock(ModMaterials.EUPHEMIUM).get()));
        add.accept(new ItemStack(ModBlocks.BLOCK_SCHRABIDIUM_CLUSTER.get()));
        add.accept(new ItemStack(ModBlocks.BLOCK_EUPHEMIUM_CLUSTER.get()));
        add.accept(new ItemStack(ModBlocks.getIngotBlock(ModMaterials.DINEUTRONIUM).get()));
        add.accept(new ItemStack(ModBlocks.BLOCK_MAGNETIZED_TUNGSTEN.get()));
        add.accept(new ItemStack(ModBlocks.getIngotBlock(ModMaterials.COMBINE_STEEL).get()));
        add.accept(new ItemStack(ModBlocks.getIngotBlock(ModMaterials.DESH).get()));
        add.accept(new ItemStack(ModBlocks.getIngotBlock(ModMaterials.DURA_STEEL).get()));
        add.accept(new ItemStack(ModBlocks.BLOCK_STARMETAL.get()));

        // 181-190: Polymer, Bakelite, Rubber, Australium, Bottle Caps (Nuka, Quantum, S~Cola, S~Cola RAD, Korl, Fritz)
        add.accept(new ItemStack(ModBlocks.BLOCK_POLYMER.get()));
        add.accept(new ItemStack(ModBlocks.BLOCK_BAKELITE.get()));
        add.accept(new ItemStack(ModBlocks.BLOCK_RUBBER.get()));
        add.accept(new ItemStack(ModBlocks.getIngotBlock(ModMaterials.AUSTRALIUM).get()));
        add.accept(new ItemStack(ModBlocks.BLOCK_CAP_NUKA.get()));
        add.accept(new ItemStack(ModBlocks.BLOCK_CAP_QUANTUM.get()));
        add.accept(new ItemStack(ModBlocks.BLOCK_CAP_SPARKLE.get()));
        add.accept(new ItemStack(ModBlocks.BLOCK_CAP_RAD.get()));
        add.accept(new ItemStack(ModBlocks.BLOCK_CAP_KORL.get()));
        add.accept(new ItemStack(ModBlocks.BLOCK_CAP_FRITZ.get()));

        // 191-200: Lanthanium, Radium-226, Actinium, Tritium Cells, Semtex, C-4, S'more, Slag, Titanium Deco Block, Red Copper Deco Block
        add.accept(new ItemStack(ModBlocks.getIngotBlock(ModMaterials.LANTHANIUM).get()));
        add.accept(new ItemStack(ModBlocks.getIngotBlock(ModMaterials.RA226).get()));
        add.accept(new ItemStack(ModBlocks.getIngotBlock(ModMaterials.ACTINIUM).get()));
        add.accept(new ItemStack(ModBlocks.BLOCK_TRITIUM.get()));
        add.accept(new ItemStack(ModBlocks.BLOCK_SEMTEX.get()));
        add.accept(new ItemStack(ModBlocks.BLOCK_C4.get()));
        add.accept(new ItemStack(ModBlocks.BLOCK_SMORE.get()));
        add.accept(new ItemStack(ModBlocks.BLOCK_SLAG.get()));
        add.accept(new ItemStack(ModBlocks.DECO_TITANIUM.get()));
        add.accept(new ItemStack(ModBlocks.DECO_RED_COPPER.get()));

        // --- CHUNK 3: Items 201-300 (worker_m3_b3) ---
        // 201-210: Deco Tungsten, Aluminium, Steel, Rusty Steel, Lead, Beryllium, Platemetal Base to Red
        add.accept(new ItemStack(ModBlocks.DECO_TUNGSTEN.get()));
        add.accept(new ItemStack(ModBlocks.DECO_ALUMINIUM.get()));
        add.accept(new ItemStack(ModBlocks.DECO_STEEL.get()));
        add.accept(new ItemStack(ModBlocks.DECO_RUSTY_STEEL.get()));
        add.accept(new ItemStack(ModBlocks.DECO_LEAD.get()));
        add.accept(new ItemStack(ModBlocks.DECO_BERYLLIUM.get()));
        add.accept(new ItemStack(ModBlocks.PLATEMETAL_BASE.get()));
        add.accept(new ItemStack(ModBlocks.PLATEMETAL_BLACK.get()));
        add.accept(new ItemStack(ModBlocks.PLATEMETAL_WHITE.get()));
        add.accept(new ItemStack(ModBlocks.PLATEMETAL_RED.get()));

        // 211-220: Platemetal Green to Magenta
        add.accept(new ItemStack(ModBlocks.PLATEMETAL_GREEN.get()));
        add.accept(new ItemStack(ModBlocks.PLATEMETAL_LIGHT_GRAY.get()));
        add.accept(new ItemStack(ModBlocks.PLATEMETAL_BLUE.get()));
        add.accept(new ItemStack(ModBlocks.PLATEMETAL_PURPLE.get()));
        add.accept(new ItemStack(ModBlocks.PLATEMETAL_CYAN.get()));
        add.accept(new ItemStack(ModBlocks.PLATEMETAL_PINK.get()));
        add.accept(new ItemStack(ModBlocks.PLATEMETAL_LIME.get()));
        add.accept(new ItemStack(ModBlocks.PLATEMETAL_YELLOW.get()));
        add.accept(new ItemStack(ModBlocks.PLATEMETAL_LIGHT_BLUE.get()));
        add.accept(new ItemStack(ModBlocks.PLATEMETAL_MAGENTA.get()));

        // 221-230: Platemetal Orange, Asbestos Roof, Deco Light Emitter, Deco Particle Emitter, Bobbleheads (Strength to Agility)
        add.accept(new ItemStack(ModBlocks.PLATEMETAL_ORANGE.get()));
        add.accept(new ItemStack(ModBlocks.DECO_ASBESTOS.get()));
        add.accept(new ItemStack(ModBlocks.DECO_EMITTER.get()));
        add.accept(new ItemStack(ModBlocks.PART_EMITTER.get()));
        for (int i = 1; i <= 6; i++)
            add.accept(com.hbm_m.item.TrinketBlockItem.make(ModBlocks.BOBBLEHEAD.get().asItem(), i));

        // 231-240: Bobbleheads (Luck to Dmitry)
        for (int i = 7; i <= 16; i++)
            add.accept(com.hbm_m.item.TrinketBlockItem.make(ModBlocks.BOBBLEHEAD.get().asItem(), i));

        // 241-250: Bobbleheads (Redstone to Schrabidium), Snowglobes (Canyon, City)
        for (int i = 17; i <= 24; i++)
            add.accept(com.hbm_m.item.TrinketBlockItem.make(ModBlocks.BOBBLEHEAD.get().asItem(), i));
        for (int i = 1; i <= 2; i++)
            add.accept(com.hbm_m.item.TrinketBlockItem.make(ModBlocks.SNOWGLOBE.get().asItem(), i));

        // 251-260: Snowglobes (Ruins to Moon), Plushies (Yomi to Dragon), Vending Machines, RBMK Deco Block
        for (int i = 3; i <= 5; i++)
            add.accept(com.hbm_m.item.TrinketBlockItem.make(ModBlocks.SNOWGLOBE.get().asItem(), i));
        for (int i = 1; i <= 4; i++)
            add.accept(com.hbm_m.item.TrinketBlockItem.make(ModBlocks.PLUSHIE.get().asItem(), i));
        add.accept(new ItemStack(ModBlocks.VENDING_MACHINE.get()));
        add.accept(new ItemStack(ModBlocks.VENDING_MACHINE_SNACKS.get()));
        add.accept(new ItemStack(ModBlocks.DECO_RBMK.get()));

        // 261-270: Smooth RBMK Deco, Crushed Obsidian, Diamond Gravel, Tritium Lamps, Demon Core Lamp, Lantern, Spotlights
        add.accept(new ItemStack(ModBlocks.DECO_RBMK_SMOOTH.get()));
        add.accept(new ItemStack(ModBlocks.GRAVEL_OBSIDIAN.get()));
        add.accept(new ItemStack(ModBlocks.GRAVEL_DIAMOND.get()));
        add.accept(new ItemStack(ModBlocks.LAMP_TRITIUM_GREEN_OFF.get()));
        add.accept(new ItemStack(ModBlocks.LAMP_TRITIUM_BLUE_OFF.get()));
        add.accept(new ItemStack(ModBlocks.LAMP_DEMON.get()));
        add.accept(new ItemStack(ModBlocks.LANTERN.get()));
        add.accept(new ItemStack(ModBlocks.CAGE_LAMP.get()));
        add.accept(new ItemStack(ModBlocks.FLUORESCENT_LAMP.get()));
        add.accept(new ItemStack(ModBlocks.FLOOD_LAMP.get()));

        // 271-280: Floodlight, Sandbags, Wooden Barrier, Wooden Structures, Asphalt, Reinforced Stone & Glass
        add.accept(new ItemStack(ModBlocks.FLOODLIGHT.get()));
        add.accept(new ItemStack(ModBlocks.SANDBAGS.get()));
        add.accept(new ItemStack(ModBlocks.WOOD_BARRIER.get()));
        add.accept(new ItemStack(ModBlocks.WOOD_STRUCTURE.get()));
        add.accept(new ItemStack(ModBlocks.WOOD_STRUCTURE_SCAFFOLD.get()));
        add.accept(new ItemStack(ModBlocks.WOOD_STRUCTURE_CEILING.get()));
        add.accept(new ItemStack(ModBlocks.ASPHALT.get()));
        add.accept(new ItemStack(ModBlocks.ASPHALT_LIGHT.get()));
        add.accept(new ItemStack(ModBlocks.REINFORCED_BRICK.get()));
        add.accept(new ItemStack(ModBlocks.REINFORCED_GLASS.get()));

        // 281-290: Reinforced Glass Pane, Glowstone, Sandstone, Lamp, Laminates, Rebar, Dense Stone, Reinforced Ducrete, Concrete
        add.accept(new ItemStack(ModBlocks.REINFORCED_GLASS_PANE.get()));
        add.accept(new ItemStack(ModBlocks.REINFORCED_LIGHT.get()));
        add.accept(new ItemStack(ModBlocks.REINFORCED_SAND.get()));
        add.accept(new ItemStack(ModBlocks.REINFORCED_LAMP_OFF.get()));
        add.accept(new ItemStack(ModBlocks.REINFORCED_LAMINATE.get()));
        add.accept(new ItemStack(ModBlocks.REINFORCED_LAMINATE_PANE.get()));
        add.accept(new ItemStack(ModBlocks.REBAR.get()));
        add.accept(new ItemStack(ModBlocks.REINFORCED_STONE.get()));
        add.accept(new ItemStack(ModBlocks.REINFORCED_DUCRETE.get()));
        add.accept(new ItemStack(ModBlocks.CONCRETE_SMOOTH.get()));

        // 291-300: Colored Concrete (White to Cyan)
        add.accept(new ItemStack(ModBlocks.CONCRETE_WHITE.get()));
        add.accept(new ItemStack(ModBlocks.CONCRETE_ORANGE.get()));
        add.accept(new ItemStack(ModBlocks.CONCRETE_MAGENTA.get()));
        add.accept(new ItemStack(ModBlocks.CONCRETE_LIGHT_BLUE.get()));
        add.accept(new ItemStack(ModBlocks.CONCRETE_YELLOW.get()));
        add.accept(new ItemStack(ModBlocks.CONCRETE_LIME.get()));
        add.accept(new ItemStack(ModBlocks.CONCRETE_PINK.get()));
        add.accept(new ItemStack(ModBlocks.CONCRETE_GRAY.get()));
        add.accept(new ItemStack(ModBlocks.CONCRETE_SILVER.get()));
        add.accept(new ItemStack(ModBlocks.CONCRETE_CYAN.get()));

        // --- CHUNK 4: Items 301-400 (worker_m3_b4) ---
        // 301-310: Colored Concrete (Purple to Black), Builder's Choice Concrete (Machine, Stripe, Indigo, Purple)
        add.accept(new ItemStack(ModBlocks.CONCRETE_PURPLE.get()));
        add.accept(new ItemStack(ModBlocks.CONCRETE_BLUE.get()));
        add.accept(new ItemStack(ModBlocks.CONCRETE_BROWN.get()));
        add.accept(new ItemStack(ModBlocks.CONCRETE_GREEN.get()));
        add.accept(new ItemStack(ModBlocks.CONCRETE_RED.get()));
        add.accept(new ItemStack(ModBlocks.CONCRETE_BLACK.get()));
        add.accept(new ItemStack(ModBlocks.CONCRETE_COLORED_EXT_MACHINE.get()));
        add.accept(new ItemStack(ModBlocks.CONCRETE_COLORED_EXT_MACHINE_STRIPE.get()));
        add.accept(new ItemStack(ModBlocks.CONCRETE_COLORED_EXT_INDIGO.get()));
        add.accept(new ItemStack(ModBlocks.CONCRETE_COLORED_EXT_PURPLE.get()));

        // 311-320: Builder's Choice Concrete (Pink, Hazard, Sand, Bronze), Concrete Tile, Asbestos/Reinforced/Super/Broken Concrete, Ducrete
        add.accept(new ItemStack(ModBlocks.CONCRETE_COLORED_EXT_PINK.get()));
        add.accept(new ItemStack(ModBlocks.CONCRETE_COLORED_EXT_HAZARD.get()));
        add.accept(new ItemStack(ModBlocks.CONCRETE_COLORED_EXT_SAND.get()));
        add.accept(new ItemStack(ModBlocks.CONCRETE_COLORED_EXT_BRONZE.get()));
        add.accept(new ItemStack(ModBlocks.CONCRETE.get()));
        add.accept(new ItemStack(ModBlocks.CONCRETE_ASBESTOS.get()));
        add.accept(new ItemStack(ModBlocks.CONCRETE_REBAR.get()));
        add.accept(new ItemStack(ModBlocks.CONCRETE_SUPER.get()));
        add.accept(new ItemStack(ModBlocks.CONCRETE_SUPER_BROKEN.get()));
        add.accept(new ItemStack(ModBlocks.DUCRETE_SMOOTH.get()));

        // 321-330: Ducrete Tile, Pillar, Bricks (Concrete, Mossy, Cracked, Broken, Marked, Ducrete, Obsidian, Compound)
        add.accept(new ItemStack(ModBlocks.DUCRETE.get()));
        add.accept(new ItemStack(ModBlocks.CONCRETE_PILLAR.get()));
        add.accept(new ItemStack(ModBlocks.BRICK_CONCRETE.get()));
        add.accept(new ItemStack(ModBlocks.BRICK_CONCRETE_MOSSY.get()));
        add.accept(new ItemStack(ModBlocks.BRICK_CONCRETE_CRACKED.get()));
        add.accept(new ItemStack(ModBlocks.BRICK_CONCRETE_BROKEN.get()));
        add.accept(new ItemStack(ModBlocks.BRICK_CONCRETE_MARKED.get()));
        add.accept(new ItemStack(ModBlocks.BRICK_DUCRETE.get()));
        add.accept(new ItemStack(ModBlocks.BRICK_OBSIDIAN.get()));
        add.accept(new ItemStack(ModBlocks.BRICK_COMPOUND.get()));

        // 331-340: Light/Asbestos/Fire Bricks, Lightstone (Raw, Tile, Bricks, Chiseled Bricks, Chiseled), Lightstone Stairs (Tile, Bricks)
        add.accept(new ItemStack(ModBlocks.BRICK_LIGHT.get()));
        add.accept(new ItemStack(ModBlocks.BRICK_ASBESTOS.get()));
        add.accept(new ItemStack(ModBlocks.BRICK_FIRE.get()));
        add.accept(new ItemStack(ModBlocks.LIGHTSTONE_UNREFINED.get()));
        add.accept(new ItemStack(ModBlocks.LIGHTSTONE_TILE.get()));
        add.accept(new ItemStack(ModBlocks.LIGHTSTONE_BRICKS.get()));
        add.accept(new ItemStack(ModBlocks.LIGHTSTONE_BRICKS_CHISELED.get()));
        add.accept(new ItemStack(ModBlocks.LIGHTSTONE_CHISELED.get()));
        add.accept(new ItemStack(ModBlocks.LIGHTSTONE_TILE_STAIRS.get()));
        add.accept(new ItemStack(ModBlocks.LIGHTSTONE_BRICKS_STAIRS.get()));

        // 341-350: Lightstone Slabs, Concrete Slabs (Normal, Tile, Asbestos, Ducrete unported, Ducrete Tile unported, Asphalt), Concrete Brick Slabs (Normal, Mossy)
        add.accept(new ItemStack(ModBlocks.STONES_SLAB_TILE.get()));
        add.accept(new ItemStack(ModBlocks.STONES_SLAB_BRICKS.get()));
        add.accept(new ItemStack(ModBlocks.CONCRETE_SLAB.get()));
        add.accept(new ItemStack(ModBlocks.CONCRETE_TILE_SLAB.get()));
        add.accept(new ItemStack(ModBlocks.CONCRETE_ASBESTOS_SLAB.get()));
        add.accept(new ItemStack(ModBlocks.DUCRETE_SLAB.get()));
        add.accept(new ItemStack(ModBlocks.DUCRETE_TILE_SLAB.get()));
        add.accept(new ItemStack(ModBlocks.ASPHALT_SLAB.get()));
        add.accept(new ItemStack(ModBlocks.BRICK_CONCRETE_SLAB.get()));
        add.accept(new ItemStack(ModBlocks.BRICK_CONCRETE_MOSSY_SLAB.get()));

        // 351-360: Brick Slabs (Cracked, Broken, Ducrete, Dense Stone, Reinforced Stone, Obsidian, Light, Compound, Asbestos, Firebrick)
        add.accept(new ItemStack(ModBlocks.BRICK_CONCRETE_CRACKED_SLAB.get()));
        add.accept(new ItemStack(ModBlocks.BRICK_CONCRETE_BROKEN_SLAB.get()));
        add.accept(new ItemStack(ModBlocks.BRICK_DUCRETE_SLAB.get()));
        add.accept(new ItemStack(ModBlocks.REINFORCED_STONE_SLAB.get()));
        add.accept(new ItemStack(ModBlocks.REINFORCED_BRICK_SLAB.get()));
        add.accept(new ItemStack(ModBlocks.BRICK_OBSIDIAN_SLAB.get()));
        add.accept(new ItemStack(ModBlocks.BRICK_LIGHT_SLAB.get()));
        add.accept(new ItemStack(ModBlocks.BRICK_COMPOUND_SLAB.get()));
        add.accept(new ItemStack(ModBlocks.BRICK_ASBESTOS_SLAB.get()));
        add.accept(new ItemStack(ModBlocks.BRICK_FIRE_SLAB.get()));

        // 361-370: Stairs (Concrete, Concrete Tile, Asbestos Concrete, Ducrete, Concrete Bricks: Normal, Mossy, Cracked, Broken, Ducrete Bricks, Dense Stone)
        add.accept(new ItemStack(ModBlocks.CONCRETE_SMOOTH_STAIRS.get()));
        add.accept(new ItemStack(ModBlocks.CONCRETE_STAIRS.get()));
        add.accept(new ItemStack(ModBlocks.CONCRETE_ASBESTOS_STAIRS.get()));
        add.accept(new ItemStack(ModBlocks.DUCRETE_SMOOTH_STAIRS.get()));
        add.accept(new ItemStack(ModBlocks.BRICK_CONCRETE_STAIRS.get()));
        add.accept(new ItemStack(ModBlocks.BRICK_CONCRETE_MOSSY_STAIRS.get()));
        add.accept(new ItemStack(ModBlocks.BRICK_CONCRETE_CRACKED_STAIRS.get()));
        add.accept(new ItemStack(ModBlocks.BRICK_CONCRETE_BROKEN_STAIRS.get()));
        add.accept(new ItemStack(ModBlocks.BRICK_DUCRETE_STAIRS.get()));
        add.accept(new ItemStack(ModBlocks.REINFORCED_STONE_STAIRS.get()));

        // 371-380: Stairs (Reinforced Stone, Obsidian Bricks, Light Bricks, Compound Mesh, Asbestos Bricks, Firebrick, Ducrete Tile, Asphalt), CMB Tile & Reinforced
        add.accept(new ItemStack(ModBlocks.REINFORCED_BRICK_STAIRS.get()));
        add.accept(new ItemStack(ModBlocks.BRICK_OBSIDIAN_STAIRS.get()));
        add.accept(new ItemStack(ModBlocks.BRICK_LIGHT_STAIRS.get()));
        add.accept(new ItemStack(ModBlocks.BRICK_COMPOUND_STAIRS.get()));
        add.accept(new ItemStack(ModBlocks.BRICK_ASBESTOS_STAIRS.get()));
        add.accept(new ItemStack(ModBlocks.BRICK_FIRE_STAIRS.get()));
        add.accept(new ItemStack(ModBlocks.DUCRETE_STAIRS.get()));
        add.accept(new ItemStack(ModBlocks.ASPHALT_STAIRS.get()));
        add.accept(new ItemStack(ModBlocks.CMB_BRICK.get()));
        add.accept(new ItemStack(ModBlocks.CMB_BRICK_REINFORCED.get()));

        // 381-390: Vinyl Tiles (Large, Small), Lab Tiles (Clean, Cracked, Broken), Barbed Wire (Standard, Flaming, Poisoned, Caustic, Withered)
        add.accept(new ItemStack(ModBlocks.VINYL_TILE.get()));
        add.accept(new ItemStack(ModBlocks.VINYL_TILE_SMALL.get()));
        add.accept(new ItemStack(ModBlocks.TILE_LAB.get()));
        add.accept(new ItemStack(ModBlocks.TILE_LAB_CRACKED.get()));
        add.accept(new ItemStack(ModBlocks.TILE_LAB_BROKEN.get()));
        add.accept(new ItemStack(ModBlocks.BARBED_WIRE.get()));
        add.accept(new ItemStack(ModBlocks.BARBED_WIRE_FIRE.get()));
        add.accept(new ItemStack(ModBlocks.BARBED_WIRE_POISON.get()));
        add.accept(new ItemStack(ModBlocks.BARBED_WIRE_ACID.get()));
        add.accept(new ItemStack(ModBlocks.BARBED_WIRE_WITHER.get()));

        // 391-400: Radioactive Barbed Wire, Spikes, Tesla Coil, Charger, Refueler, Meteor Blocks (Block, Cobble, Broken, Molten, Treasure)
        add.accept(new ItemStack(ModBlocks.BARBED_WIRE_ULTRADEATH.get()));
        add.accept(new ItemStack(ModBlocks.SPIKES.get()));
        add.accept(new ItemStack(ModBlocks.TESLA.get()));
        add.accept(new ItemStack(ModBlocks.CHARGER.get()));
        add.accept(new ItemStack(ModBlocks.REFUELER.get()));
        add.accept(new ItemStack(ModBlocks.BLOCK_METEOR.get()));
        add.accept(new ItemStack(ModBlocks.BLOCK_METEOR_COBBLE.get()));
        add.accept(new ItemStack(ModBlocks.BLOCK_METEOR_BROKEN.get()));
        add.accept(new ItemStack(ModBlocks.BLOCK_METEOR_MOLTEN.get()));
        add.accept(new ItemStack(ModBlocks.BLOCK_METEOR_TREASURE.get()));

        // --- CHUNK 5: Items 401-500 (worker_m3_b5) ---
        // 401-410: Polished Meteor Block, Meteor Bricks & Pillar, Cyber Crab Assembler, Starmetal Battery, Enargite Bricks (Normal, Cracked)
        add.accept(new ItemStack(ModBlocks.METEOR_POLISHED.get()));
        add.accept(new ItemStack(ModBlocks.METEOR_BRICK.get()));
        add.accept(new ItemStack(ModBlocks.METEOR_BRICK_MOSSY.get()));
        add.accept(new ItemStack(ModBlocks.METEOR_BRICK_CRACKED.get()));
        add.accept(new ItemStack(ModBlocks.METEOR_BRICK_CHISELED.get()));
        add.accept(new ItemStack(ModBlocks.METEOR_PILLAR.get()));
        add.accept(new ItemStack(ModBlocks.METEOR_SPAWNER.get()));
        add.accept(new ItemStack(ModBlocks.METEOR_BATTERY.get()));
        add.accept(new ItemStack(ModBlocks.BRICK_JUNGLE.get()));
        add.accept(new ItemStack(ModBlocks.BRICK_JUNGLE_CRACKED.get()));

        // 411-420: Brittle/Magmatic/Radioactive/Arcane Enargite Bricks, Trapped Enargite Bricks (Falling Rocks, Fire, Arrow, Spikes, Mine, Web)
        add.accept(new ItemStack(ModBlocks.BRICK_JUNGLE_FRAGILE.get()));
        add.accept(new ItemStack(ModBlocks.BRICK_JUNGLE_LAVA.get()));
        add.accept(new ItemStack(ModBlocks.BRICK_JUNGLE_OOZE.get()));
        add.accept(new ItemStack(ModBlocks.BRICK_JUNGLE_MYSTIC.get()));
        add.accept(com.hbm_m.block.generic.JungleBricks.stack(ModBlocks.BRICK_JUNGLE_TRAP.get().asItem(), "trap", "falling_rocks"));
        add.accept(com.hbm_m.block.generic.JungleBricks.stack(ModBlocks.BRICK_JUNGLE_TRAP.get().asItem(), "trap", "fire"));
        add.accept(com.hbm_m.block.generic.JungleBricks.stack(ModBlocks.BRICK_JUNGLE_TRAP.get().asItem(), "trap", "arrow"));
        add.accept(com.hbm_m.block.generic.JungleBricks.stack(ModBlocks.BRICK_JUNGLE_TRAP.get().asItem(), "trap", "spikes"));
        add.accept(com.hbm_m.block.generic.JungleBricks.stack(ModBlocks.BRICK_JUNGLE_TRAP.get().asItem(), "trap", "mine"));
        add.accept(com.hbm_m.block.generic.JungleBricks.stack(ModBlocks.BRICK_JUNGLE_TRAP.get().asItem(), "trap", "web"));

        // 421-430: Trapped Enargite Bricks (Flaming Arrow, Pillar, Rad, Magic, Slowness, Weakness, Poison Dart, Zombie, Spiders), Enargite Glyph Brick 0
        add.accept(com.hbm_m.block.generic.JungleBricks.stack(ModBlocks.BRICK_JUNGLE_TRAP.get().asItem(), "trap", "flaming_arrow"));
        add.accept(com.hbm_m.block.generic.JungleBricks.stack(ModBlocks.BRICK_JUNGLE_TRAP.get().asItem(), "trap", "pillar"));
        add.accept(com.hbm_m.block.generic.JungleBricks.stack(ModBlocks.BRICK_JUNGLE_TRAP.get().asItem(), "trap", "rad_conversion"));
        add.accept(com.hbm_m.block.generic.JungleBricks.stack(ModBlocks.BRICK_JUNGLE_TRAP.get().asItem(), "trap", "magic_converstion"));
        add.accept(com.hbm_m.block.generic.JungleBricks.stack(ModBlocks.BRICK_JUNGLE_TRAP.get().asItem(), "trap", "slowness"));
        add.accept(com.hbm_m.block.generic.JungleBricks.stack(ModBlocks.BRICK_JUNGLE_TRAP.get().asItem(), "trap", "weakness"));
        add.accept(com.hbm_m.block.generic.JungleBricks.stack(ModBlocks.BRICK_JUNGLE_TRAP.get().asItem(), "trap", "poison_dart"));
        add.accept(com.hbm_m.block.generic.JungleBricks.stack(ModBlocks.BRICK_JUNGLE_TRAP.get().asItem(), "trap", "zombie"));
        add.accept(com.hbm_m.block.generic.JungleBricks.stack(ModBlocks.BRICK_JUNGLE_TRAP.get().asItem(), "trap", "spiders"));
        add.accept(com.hbm_m.block.generic.JungleBricks.stack(ModBlocks.BRICK_JUNGLE_GLYPH.get().asItem(), "glyph", "0"));

        // 431-440: Enargite Glyph Bricks 1 to 10
        add.accept(com.hbm_m.block.generic.JungleBricks.stack(ModBlocks.BRICK_JUNGLE_GLYPH.get().asItem(), "glyph", "1"));
        add.accept(com.hbm_m.block.generic.JungleBricks.stack(ModBlocks.BRICK_JUNGLE_GLYPH.get().asItem(), "glyph", "2"));
        add.accept(com.hbm_m.block.generic.JungleBricks.stack(ModBlocks.BRICK_JUNGLE_GLYPH.get().asItem(), "glyph", "3"));
        add.accept(com.hbm_m.block.generic.JungleBricks.stack(ModBlocks.BRICK_JUNGLE_GLYPH.get().asItem(), "glyph", "4"));
        add.accept(com.hbm_m.block.generic.JungleBricks.stack(ModBlocks.BRICK_JUNGLE_GLYPH.get().asItem(), "glyph", "5"));
        add.accept(com.hbm_m.block.generic.JungleBricks.stack(ModBlocks.BRICK_JUNGLE_GLYPH.get().asItem(), "glyph", "6"));
        add.accept(com.hbm_m.block.generic.JungleBricks.stack(ModBlocks.BRICK_JUNGLE_GLYPH.get().asItem(), "glyph", "7"));
        add.accept(com.hbm_m.block.generic.JungleBricks.stack(ModBlocks.BRICK_JUNGLE_GLYPH.get().asItem(), "glyph", "8"));
        add.accept(com.hbm_m.block.generic.JungleBricks.stack(ModBlocks.BRICK_JUNGLE_GLYPH.get().asItem(), "glyph", "9"));
        add.accept(com.hbm_m.block.generic.JungleBricks.stack(ModBlocks.BRICK_JUNGLE_GLYPH.get().asItem(), "glyph", "10"));

        // 441-450: Enargite Glyph Bricks 11 to 15, Mechanist Circle, IBM 300PL Computer, CRT Screen (Clean, Broken, Blinking)
        add.accept(com.hbm_m.block.generic.JungleBricks.stack(ModBlocks.BRICK_JUNGLE_GLYPH.get().asItem(), "glyph", "11"));
        add.accept(com.hbm_m.block.generic.JungleBricks.stack(ModBlocks.BRICK_JUNGLE_GLYPH.get().asItem(), "glyph", "12"));
        add.accept(com.hbm_m.block.generic.JungleBricks.stack(ModBlocks.BRICK_JUNGLE_GLYPH.get().asItem(), "glyph", "13"));
        add.accept(com.hbm_m.block.generic.JungleBricks.stack(ModBlocks.BRICK_JUNGLE_GLYPH.get().asItem(), "glyph", "14"));
        add.accept(com.hbm_m.block.generic.JungleBricks.stack(ModBlocks.BRICK_JUNGLE_GLYPH.get().asItem(), "glyph", "15"));
        add.accept(new ItemStack(ModBlocks.BRICK_JUNGLE_CIRCLE.get()));
        add.accept(new ItemStack(ModBlocks.PUTER.get()));
        add.accept(new ItemStack(ModBlocks.CRT_CLEAN.get()));
        add.accept(new ItemStack(ModBlocks.CRT_BROKEN.get()));
        add.accept(new ItemStack(ModBlocks.CRT_BLINKING.get()));

        // 451-460: CRT Screen (BSOD), Toasters (Iron, Steel, Wood), Filing Cabinets (Dusty, Steel), Tape Recorder, Steel Pole, Antenna Top, Satellite Dish
        add.accept(new ItemStack(ModBlocks.CRT_BSOD.get()));
        add.accept(new ItemStack(ModBlocks.TOASTER.get()));
        add.accept(new ItemStack(ModBlocks.TOASTER_STEEL.get()));
        add.accept(new ItemStack(ModBlocks.TOASTER_WOOD.get()));
        add.accept(new ItemStack(ModBlocks.FILE_CABINET.get()));
        add.accept(new ItemStack(ModBlocks.FILE_CABINET_STEEL.get()));
        add.accept(new ItemStack(ModBlocks.TAPE_RECORDER.get()));
        add.accept(new ItemStack(ModBlocks.STEEL_POLE.get()));
        add.accept(new ItemStack(ModBlocks.ANTENNA_TOP.get()));
        add.accept(new ItemStack(ModBlocks.POLE_SATELLITE_RECEIVER.get()));

        // 461-470: Steel Wall, Wall Corner, Roof, Beam, Steel Scaffolds (Steel, Red, White, Yellow), Steel Grate, Item Grate
        add.accept(new ItemStack(ModBlocks.STEEL_WALL.get()));
        add.accept(new ItemStack(ModBlocks.STEEL_CORNER.get()));
        add.accept(new ItemStack(ModBlocks.STEEL_ROOF.get()));
        add.accept(new ItemStack(ModBlocks.STEEL_BEAM.get()));
        add.accept(new ItemStack(ModBlocks.STEEL_SCAFFOLD.get()));
        add.accept(new ItemStack(ModBlocks.STEEL_SCAFFOLD_RED.get()));
        add.accept(new ItemStack(ModBlocks.STEEL_SCAFFOLD_WHITE.get()));
        add.accept(new ItemStack(ModBlocks.STEEL_SCAFFOLD_YELLOW.get()));
        add.accept(new ItemStack(ModBlocks.STEEL_GRATE.get()));
        add.accept(new ItemStack(ModBlocks.STEEL_GRATE_WIDE.get()));

        // 471-480: Deco Pipes (Normal, Rusted, Green, Green Rusted, Red, Gas) and Rimmed Deco Pipes (Normal, Rusted, Green, Green Rusted)
        add.accept(new ItemStack(ModBlocks.DECO_PIPE.get()));
        add.accept(new ItemStack(ModBlocks.DECO_PIPE_RUSTED.get()));
        add.accept(new ItemStack(ModBlocks.DECO_PIPE_GREEN.get()));
        add.accept(new ItemStack(ModBlocks.DECO_PIPE_GREEN_RUSTED.get()));
        add.accept(new ItemStack(ModBlocks.DECO_PIPE_RED.get()));
        add.accept(new ItemStack(ModBlocks.DECO_PIPE_MARKED.get()));
        add.accept(new ItemStack(ModBlocks.DECO_PIPE_RIM.get()));
        add.accept(new ItemStack(ModBlocks.DECO_PIPE_RIM_RUSTED.get()));
        add.accept(new ItemStack(ModBlocks.DECO_PIPE_RIM_GREEN.get()));
        add.accept(new ItemStack(ModBlocks.DECO_PIPE_RIM_GREEN_RUSTED.get()));

        // 481-490: Rimmed Deco Pipes (Red, Gas), Framed Deco Pipes (Normal, Rusted, Green, Green Rusted, Red, Gas), Quad Pipes (Normal, Rusted)
        add.accept(new ItemStack(ModBlocks.DECO_PIPE_RIM_RED.get()));
        add.accept(new ItemStack(ModBlocks.DECO_PIPE_RIM_MARKED.get()));
        add.accept(new ItemStack(ModBlocks.DECO_PIPE_FRAMED.get()));
        add.accept(new ItemStack(ModBlocks.DECO_PIPE_FRAMED_RUSTED.get()));
        add.accept(new ItemStack(ModBlocks.DECO_PIPE_FRAMED_GREEN.get()));
        add.accept(new ItemStack(ModBlocks.DECO_PIPE_FRAMED_GREEN_RUSTED.get()));
        add.accept(new ItemStack(ModBlocks.DECO_PIPE_FRAMED_RED.get()));
        add.accept(new ItemStack(ModBlocks.DECO_PIPE_FRAMED_MARKED.get()));
        add.accept(new ItemStack(ModBlocks.DECO_PIPE_QUAD.get()));
        add.accept(new ItemStack(ModBlocks.DECO_PIPE_QUAD_RUSTED.get()));

        // 491-500: Quad Pipes (Green, Green Rusted, Red, Gas), Flowers (Foxglove, Tobacco, Nightshade, Hemp, Mustard Willow CD0, Mustard Willow CD1)
        add.accept(new ItemStack(ModBlocks.DECO_PIPE_QUAD_GREEN.get()));
        add.accept(new ItemStack(ModBlocks.DECO_PIPE_QUAD_GREEN_RUSTED.get()));
        add.accept(new ItemStack(ModBlocks.DECO_PIPE_QUAD_RED.get()));
        add.accept(new ItemStack(ModBlocks.DECO_PIPE_QUAD_MARKED.get()));
        add.accept(new ItemStack(ModBlocks.PLANT_FLOWER_FOXGLOVE.get()));
        add.accept(new ItemStack(ModBlocks.PLANT_FLOWER_TOBACCO.get()));
        add.accept(new ItemStack(ModBlocks.PLANT_FLOWER_NIGHTSHADE.get()));
        add.accept(new ItemStack(ModBlocks.PLANT_FLOWER_WEED.get()));
        add.accept(new ItemStack(ModBlocks.PLANT_FLOWER_CD0.get()));
        add.accept(new ItemStack(ModBlocks.PLANT_FLOWER_CD1.get()));

        // --- CHUNK 6: Items 501-575 (worker_m3_b6) ---
        // 501-510: Hemp, Mustard Willows, Dead Plants (Generic, Grass, Flower, Big Flower, Fern), Reeds
        add.accept(new ItemStack(ModBlocks.PLANT_TALL_WEED.get()));
        add.accept(new ItemStack(ModBlocks.PLANT_TALL_CD2.get()));
        add.accept(new ItemStack(ModBlocks.PLANT_TALL_CD3.get()));
        add.accept(new ItemStack(ModBlocks.PLANT_TALL_CD4.get()));
        add.accept(new ItemStack(ModBlocks.PLANT_DEAD.get()));
        add.accept(new ItemStack(ModBlocks.PLANT_DEAD_GRASS.get()));
        add.accept(new ItemStack(ModBlocks.PLANT_DEAD_FLOWER.get()));
        add.accept(new ItemStack(ModBlocks.PLANT_DEAD_BIGFLOWER.get()));
        add.accept(new ItemStack(ModBlocks.PLANT_DEAD_FERN.get()));
        add.accept(new ItemStack(ModBlocks.PLANT_REEDS.get()));

        // 511-520: Phosphor Vines, Glowing Mushroom, Glyphid Hive Blocks (Base, Infested unported, Rad unported), Glyphid Hive Spawners (Base, Infested unported, Rad unported), Moon Turf, Dead Grass
        add.accept(new ItemStack(ModBlocks.VINE_PHOSPHOR.get()));
        add.accept(new ItemStack(ModBlocks.MUSH.get()));
        add.accept(new ItemStack(ModBlocks.GLYPHID_BASE.get()));
        add.accept(new ItemStack(ModBlocks.GLYPHID_BASE_INFESTED.get()));
        add.accept(new ItemStack(ModBlocks.GLYPHID_BASE_RADIOACTIVE.get()));
        add.accept(new ItemStack(ModBlocks.GLYPHID_SPAWNER.get()));
        add.accept(new ItemStack(ModBlocks.GLYPHID_SPAWNER_INFESTED.get()));
        add.accept(new ItemStack(ModBlocks.GLYPHID_SPAWNER_RADIOACTIVE.get()));
        add.accept(new ItemStack(ModBlocks.MOON_TURF.get()));
        add.accept(new ItemStack(ModBlocks.WASTE_EARTH.get()));

        // 521-530: Glowing Mycelium, Trinitite Ores (Normal, Red), Charred Wood (Log, Leaves, Planks), Frozen Terrain (Grass, Dirt, Log, Planks)
        add.accept(new ItemStack(ModBlocks.WASTE_MYCELIUM.get()));
        add.accept(new ItemStack(ModBlocks.WASTE_TRINITITE.get()));
        add.accept(new ItemStack(ModBlocks.WASTE_TRINITITE_RED.get()));
        add.accept(new ItemStack(ModBlocks.WASTE_LOG.get()));
        add.accept(new ItemStack(ModBlocks.WASTE_LEAVES.get()));
        add.accept(new ItemStack(ModBlocks.WASTE_PLANKS.get()));
        add.accept(new ItemStack(ModBlocks.FROZEN_GRASS.get()));
        add.accept(new ItemStack(ModBlocks.FROZEN_DIRT.get()));
        add.accept(new ItemStack(ModBlocks.FROZEN_LOG.get()));
        add.accept(new ItemStack(ModBlocks.FROZEN_PLANKS.get()));

        // 531-540: Dead Dirt, Oily Dirt, Oily Sand (Normal, Red), Cracked Stone, Fallout, Foam Layer, Boron Sand Layer, Fallen Leaves, Oil Spill
        add.accept(new ItemStack(ModBlocks.DIRT_DEAD.get()));
        add.accept(new ItemStack(ModBlocks.DIRT_OILY.get()));
        add.accept(new ItemStack(ModBlocks.SAND_DIRTY.get()));
        add.accept(new ItemStack(ModBlocks.SAND_DIRTY_RED.get()));
        add.accept(new ItemStack(ModBlocks.STONE_CRACKED.get()));
        add.accept(new ItemStack(ModBlocks.NUCLEAR_FALLOUT.get()));
        add.accept(new ItemStack(ModBlocks.FOAM_LAYER.get()));
        add.accept(new ItemStack(ModBlocks.SAND_BORON_LAYER.get()));
        add.accept(new ItemStack(ModBlocks.LEAVES_LAYER.get()));
        add.accept(new ItemStack(ModBlocks.OIL_SPILL.get()));

        // 541-550: Burning Grass, Tektite, Osmiridium Tektite, Scorched Dirt, Sellafite (Slaked, Bedrock), Sellafite Ores (Diamond, Emerald, Scorched Uranium, Schrabidium)
        add.accept(new ItemStack(ModBlocks.BURNING_EARTH.get()));
        add.accept(new ItemStack(ModBlocks.TEKTITE.get()));
        add.accept(new ItemStack(ModBlocks.ORE_TEKTITE_OSMIRIDIUM.get()));
        add.accept(new ItemStack(ModBlocks.IMPACT_DIRT.get()));
        add.accept(new ItemStack(ModBlocks.SELLAFIELD_SLAKED.get()));
        add.accept(new ItemStack(ModBlocks.SELLAFIELD_BEDROCK.get()));
        add.accept(new ItemStack(ModBlocks.ORE_SELLAFIELD_DIAMOND.get()));
        add.accept(new ItemStack(ModBlocks.ORE_SELLAFIELD_EMERALD.get()));
        add.accept(new ItemStack(ModBlocks.ORE_SELLAFIELD_URANIUM_SCORCHED.get()));
        add.accept(new ItemStack(ModBlocks.ORE_SELLAFIELD_SCHRABIDIUM.get()));

        // 551-560: Sellafite Radgem Ore, Sellafite Stages 0-5 (Normal, Hot, Boiling, Blazing, Infernal, Corium), Quartz Glass, Boxcar, Boat
        add.accept(new ItemStack(ModBlocks.ORE_SELLAFIELD_RADGEM.get()));
        add.accept(new ItemStack(ModBlocks.SELLAFIELD_0.get()));
        add.accept(new ItemStack(ModBlocks.SELLAFIELD_1.get()));
        add.accept(new ItemStack(ModBlocks.SELLAFIELD_2.get()));
        add.accept(new ItemStack(ModBlocks.SELLAFIELD_3.get()));
        add.accept(new ItemStack(ModBlocks.SELLAFIELD_4.get()));
        add.accept(new ItemStack(ModBlocks.SELLAFIELD_5.get()));
        add.accept(new ItemStack(ModBlocks.GLASS_QUARTZ.get()));
        add.accept(new ItemStack(ModBlocks.BOXCAR.get()));
        add.accept(new ItemStack(ModBlocks.BOAT.get()));

        // 561-570: Metal Chain, Ladders (Sturdy Wood, Gold, Titanium, Copper, Steel), Steel Trapdoor, Buckets (Poisonous Mud, Acid, Toxic Ooze)
        add.accept(new ItemStack(ModBlocks.DUNGEON_CHAIN.get()));
        add.accept(new ItemStack(ModBlocks.LADDER_STURDY.get()));
        add.accept(new ItemStack(ModBlocks.LADDER_GOLD.get()));
        add.accept(new ItemStack(ModBlocks.LADDER_TITANIUM.get()));
        add.accept(new ItemStack(ModBlocks.LADDER_COPPER.get()));
        add.accept(new ItemStack(ModBlocks.LADDER_STEEL.get()));
        add.accept(new ItemStack(ModBlocks.TRAPDOOR_STEEL.get()));
        add.accept(new ItemStack(ModItems.BUCKET_MUD.get()));
        add.accept(new ItemStack(ModItems.BUCKET_ACID.get()));
        add.accept(new ItemStack(ModItems.BUCKET_TOXIC.get()));

        // 571-575: Buckets (Schrabidic Acid, Sulfuric Acid), Doors (Metal, Office, Bunker)
        add.accept(new ItemStack(ModItems.BUCKET_SCHRABIDIC_ACID.get()));
        add.accept(new ItemStack(ModItems.BUCKET_SULFURIC_ACID.get()));
        add.accept(new ItemStack(ModBlocks.METAL_DOOR.get()));
        add.accept(new ItemStack(ModBlocks.DOOR_OFFICE.get()));
        add.accept(new ItemStack(ModBlocks.DOOR_BUNKER.get()));

        // Modernized additions without 1.7.10 counterparts (formerly in addOriginalTabIds)
        add.accept(new ItemStack(ModBlocks.CONCRETE_COLORED_BRONZE_SLAB.get()));
        add.accept(new ItemStack(ModBlocks.CONCRETE_COLORED_BRONZE_STAIRS.get()));
        add.accept(new ItemStack(ModBlocks.BASALT_BRICK_SLAB.get()));
        add.accept(new ItemStack(ModBlocks.DEPTH_BRICK_SLAB.get()));
        add.accept(new ItemStack(ModBlocks.DEPTH_NETHER_BRICK_SLAB.get()));
        add.accept(new ItemStack(ModBlocks.GNEISS_BRICK_SLAB.get()));
        add.accept(new ItemStack(ModBlocks.METEOR_BRICK_SLAB.get()));
    }

    /** populateMachinesTab: точный порядок оригинального 1.7.10 (eclipse/creative_tabs/tabMachine.md, 442 позиции); отсутствующие в порте позиции помечены комментарием. */
    public static void populateMachinesTab(BiConsumer<ItemStack, CreativeModeTab.TabVisibility> acceptor) {
        Consumer<ItemStack> add = stack -> acceptor.accept(stack, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);

        // --- CHUNK 1: Items 1-100 (worker_m3_c1) ---
        // 1-10: Siren, Corrupted Broadcaster, Geiger Counter, HEV Battery, Fences, Digamma Ash, Sands
        add.accept(new ItemStack(ModBlocks.MACHINE_SIREN.get()));
        add.accept(new ItemStack(ModBlocks.BROADCASTER_PC.get()));
        add.accept(new ItemStack(ModBlocks.GEIGER_COUNTER_BLOCK.get()));
        add.accept(new ItemStack(ModBlocks.HEV_BATTERY.get()));
        add.accept(new ItemStack(ModBlocks.FENCE_METAL.get()));
        add.accept(new ItemStack(ModBlocks.FENCE_METAL_POST.get()));
        add.accept(new ItemStack(ModBlocks.ASH_DIGAMMA.get()));
        add.accept(new ItemStack(ModBlocks.SAND_BORON.get()));
        add.accept(new ItemStack(ModBlocks.SAND_LEAD.get()));
        add.accept(new ItemStack(ModBlocks.SAND_URANIUM.get()));

        // 11-20: Sands (cont.), Lead/Uranium/Trinity/Polonium/Ash/Polarized Glasses, Seal Frame
        add.accept(new ItemStack(ModBlocks.SAND_POLONIUM.get()));
        add.accept(new ItemStack(ModBlocks.SAND_QUARTZ.get()));
        add.accept(new ItemStack(ModBlocks.GLASS_BORON.get()));
        add.accept(new ItemStack(ModBlocks.GLASS_LEAD.get()));
        add.accept(new ItemStack(ModBlocks.GLASS_URANIUM.get()));
        add.accept(new ItemStack(ModBlocks.GLASS_TRINITITE.get()));
        add.accept(new ItemStack(ModBlocks.GLASS_POLONIUM.get()));
        add.accept(new ItemStack(ModBlocks.GLASS_ASH.get()));
        add.accept(new ItemStack(ModBlocks.GLASS_POLARIZED.get()));
        add.accept(new ItemStack(ModBlocks.SEAL_FRAME.get()));

        // 21-30: Seal Controller, Cargo Elevator, Doors & Hatches
        add.accept(new ItemStack(ModBlocks.SEAL_CONTROLLER.get()));
        add.accept(new ItemStack(ModBlocks.CARGO_ELEVATOR.get()));
        add.accept(new ItemStack(ModItems.VAULT_DOOR.get()));
        add.accept(new ItemStack(ModBlocks.BLAST_DOOR.get()));
        add.accept(new ItemStack(ModItems.FIRE_DOOR.get()));
        add.accept(new ItemStack(ModItems.TRANSITION_SEAL.get()));
        add.accept(new ItemStack(ModItems.SILO_HATCH.get()));
        add.accept(new ItemStack(ModItems.SILO_HATCH_LARGE.get()));
        add.accept(new ItemStack(ModItems.SLIDE_DOOR.get()));
        add.accept(new ItemStack(ModItems.SECURE_ACCESS_DOOR.get()));

        // 31-40: Doors (cont.), Crates (Iron, Steel, Desh)
        add.accept(new ItemStack(ModItems.LARGE_VEHICLE_DOOR.get()));
        add.accept(new ItemStack(ModItems.QE_CONTAINMENT.get()));
        add.accept(new ItemStack(ModItems.QE_SLIDING.get()));
        add.accept(new ItemStack(ModItems.ROUND_AIRLOCK_DOOR.get()));
        add.accept(new ItemStack(ModItems.SLIDING_SEAL_DOOR.get()));
        add.accept(new ItemStack(ModItems.WATER_DOOR.get()));
        add.accept(new ItemStack(ModItems.CARGO_DOOR.get()));
        add.accept(new ItemStack(ModItems.CRATE_IRON.get()));
        add.accept(new ItemStack(ModItems.CRATE_STEEL.get()));
        add.accept(new ItemStack(ModItems.CRATE_DESH.get()));

        // 41-50: Crates (cont.), Safe, Mass Storages, Autocrafter, Funnel, Anvils (Iron, Lead)
        add.accept(new ItemStack(ModItems.CRATE_TUNGSTEN.get()));
        add.accept(new ItemStack(ModBlocks.SAFE.get()));
        add.accept(new ItemStack(ModBlocks.MASS_STORAGE_WOOD.get()));
        add.accept(new ItemStack(ModBlocks.MASS_STORAGE_IRON.get()));
        add.accept(new ItemStack(ModBlocks.MASS_STORAGE_DESH.get()));
        add.accept(new ItemStack(ModBlocks.MASS_STORAGE.get()));
        add.accept(new ItemStack(ModBlocks.MACHINE_AUTOCRAFTER.get()));
        add.accept(new ItemStack(ModBlocks.MACHINE_FUNNEL.get()));
        add.accept(new ItemStack(ModBlocks.ANVIL_IRON.get()));
        add.accept(new ItemStack(ModBlocks.ANVIL_LEAD.get()));

        // 51-60: Anvils (Steel, Desh, Ferrouranium, Saturnite, Bronzes, Schrabidate, DNT, Osmiridium, Murky)
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

        // 61-70: Press Preheater, Presses, Pumps, Firebox, Heating Oven, Ashpit
        add.accept(new ItemStack(ModBlocks.PRESS_PREHEATER.get()));
        add.accept(new ItemStack(ModBlocks.MACHINE_PRESS.get()));
        add.accept(new ItemStack(ModBlocks.EPRESS.get()));
        add.accept(new ItemStack(ModBlocks.CONVEYOR_PRESS.get()));
        add.accept(new ItemStack(ModBlocks.AMMO_PRESS.get()));
        add.accept(new ItemStack(ModBlocks.PUMP_STEAM.get()));
        add.accept(new ItemStack(ModBlocks.PUMP_ELECTRIC.get()));
        add.accept(new ItemStack(ModItems.FIREBOX.get()));
        add.accept(new ItemStack(ModItems.HEATING_OVEN.get()));
        add.accept(new ItemStack(ModItems.ASHPIT.get()));

        // 71-80: Heaters, Furnaces, Stirling Engines, Sawmill
        add.accept(new ItemStack(ModItems.OILBURNER.get()));
        add.accept(new ItemStack(ModItems.ELECTRIC_HEATER.get()));
        add.accept(new ItemStack(ModItems.HEATEX.get()));
        add.accept(new ItemStack(ModBlocks.FURNACE_IRON.get()));
        add.accept(new ItemStack(ModBlocks.FURNACE_STEEL.get()));
        add.accept(new ItemStack(ModBlocks.COMBINATION_OVEN.get()));
        add.accept(new ItemStack(ModBlocks.STIRLING.get()));
        add.accept(new ItemStack(ModBlocks.STIRLING_STEEL.get()));
        add.accept(new ItemStack(ModBlocks.STIRLING_CREATIVE.get()));
        add.accept(new ItemStack(ModBlocks.SAWMILL.get()));

        // 81-90: Crucible, Strand Caster, Boilers, Foundry components
        add.accept(new ItemStack(ModItems.CRUCIBLE.get()));
        add.accept(new ItemStack(ModItems.STRAND_CASTER.get()));
        add.accept(new ItemStack(ModBlocks.BOILER.get()));
        add.accept(new ItemStack(ModItems.INDUSTRIAL_BOILER.get()));
        add.accept(new ItemStack(ModBlocks.FOUNDRY_MOLD.get()));
        add.accept(new ItemStack(ModBlocks.FOUNDRY_BASIN.get()));
        add.accept(new ItemStack(ModBlocks.FOUNDRY_CHANNEL.get()));
        add.accept(new ItemStack(ModBlocks.FOUNDRY_TANK.get()));
        add.accept(new ItemStack(ModBlocks.FOUNDRY_OUTLET.get()));
        add.accept(new ItemStack(ModBlocks.FOUNDRY_SLAGTAP.get()));

        // 91-100: Bricked/Blast Furnaces, RTG Furnace, Centrifuges, FEL, SILEX, Rotary Furnace
        add.accept(new ItemStack(ModBlocks.FURNACE_BRICK.get()));
        add.accept(new ItemStack(ModBlocks.BLAST_FURNACE.get()));
        add.accept(new ItemStack(ModBlocks.BLAST_FURNACE_EXTENSION.get()));
        add.accept(new ItemStack(ModBlocks.MACHINE_BLAST_FURNACE.get()));
        add.accept(new ItemStack(ModBlocks.MACHINE_DIFURNACE_RTG.get()));
        add.accept(new ItemStack(ModItems.CENTRIFUGE.get()));
        add.accept(new ItemStack(ModItems.MACHINE_GASCENT.get()));
        add.accept(new ItemStack(ModItems.FEL.get()));
        add.accept(new ItemStack(ModItems.SILEX.get()));
        add.accept(new ItemStack(ModBlocks.ROTARY_FURNACE.get()));

        // --- CHUNK 2: Items 101-200 (worker_m3_c2) ---
        // 101-110: Acidizer, Legacy Breeder/Reactors, Wood/Diesel/Combustion/Radgen Generators, Cyclotron
        add.accept(new ItemStack(ModItems.CRYSTALLIZER.get()));
        add.accept(new ItemStack(ModItems.BREEDER.get()));
        add.accept(new ItemStack(ModItems.WOOD_BURNER.get()));
        add.accept(new ItemStack(ModBlocks.DIESELGEN.get()));
        add.accept(new ItemStack(ModBlocks.COMBUSTION_ENGINE.get()));
        add.accept(new ItemStack(ModItems.ZIRNOX.get()));
        add.accept(new ItemStack(ModBlocks.INDUSTRIAL_GENERATOR.get()));
        add.accept(new ItemStack(ModBlocks.MACHINE_RADGEN.get()));
        add.accept(new ItemStack(ModItems.CYCLOTRON.get()));

        // 111-120: Exposure Chamber, RTG, Radiolysis, Hephaestus, Hadron Coils (Alloy-Schrabidate)
        add.accept(new ItemStack(ModBlocks.EXPOSURE_CHAMBER.get()));
        add.accept(new ItemStack(ModBlocks.MACHINE_RTG.get()));
        add.accept(new ItemStack(ModBlocks.RADIOLYSIS.get()));
        add.accept(new ItemStack(ModBlocks.HEPHAESTUS.get()));
        add.accept(new ItemStack(ModBlocks.HADRON_COIL_ALLOY.get()));
        add.accept(new ItemStack(ModBlocks.HADRON_COIL_GOLD.get()));
        add.accept(new ItemStack(ModBlocks.HADRON_COIL_NEODYMIUM.get()));
        add.accept(new ItemStack(ModBlocks.HADRON_COIL_MAGTUNG.get()));
        add.accept(new ItemStack(ModBlocks.HADRON_COIL_SCHRABIDIUM.get()));
        add.accept(new ItemStack(ModBlocks.HADRON_COIL_SCHRABIDATE.get()));

        // 121-130: Hadron Coils (Starmetal-Mese), Particle Accelerator components, RBMK Fuel Channel
        add.accept(new ItemStack(ModBlocks.HADRON_COIL_STARMETAL.get()));
        add.accept(new ItemStack(ModBlocks.HADRON_COIL_CHLOROPHYTE.get()));
        add.accept(new ItemStack(ModBlocks.HADRON_COIL_MESE.get()));
        add.accept(new ItemStack(ModBlocks.PA_SOURCE.get()));
        add.accept(new ItemStack(ModBlocks.BEAMLINE.get()));
        add.accept(new ItemStack(ModBlocks.RFC.get()));
        add.accept(new ItemStack(ModBlocks.QUADRUPOLE.get()));
        add.accept(new ItemStack(ModBlocks.DIPOLE.get()));
        add.accept(new ItemStack(ModBlocks.PA_DETECTOR.get()));
        add.accept(new ItemStack(ModBlocks.RBMK_ROD.get()));

        // 131-140: RBMK Channels (Mod/ReaSim), Control Rods (Manual/Auto/ReaSim), Blank Column, Steam Channel
        add.accept(new ItemStack(ModBlocks.RBMK_ROD_MOD.get()));
        add.accept(new ItemStack(ModBlocks.RBMK_ROD_REASIM.get()));
        add.accept(new ItemStack(ModBlocks.RBMK_ROD_REASIM_MOD.get()));
        add.accept(new ItemStack(ModBlocks.RBMK_CONTROL.get()));
        add.accept(new ItemStack(ModBlocks.RBMK_CONTROL_MOD.get()));
        add.accept(new ItemStack(ModBlocks.RBMK_CONTROL_AUTO.get()));
        add.accept(new ItemStack(ModBlocks.RBMK_CONTROL_REASIM.get()));
        add.accept(new ItemStack(ModBlocks.RBMK_CONTROL_REASIM_AUTO.get()));
        add.accept(new ItemStack(ModBlocks.RBMK_BLANK.get()));
        add.accept(new ItemStack(ModBlocks.RBMK_BOILER.get()));

        // 141-150: RBMK Reflector, Absorber, Moderator, Outgasser, Storage, Cooler, Heater, Consoles, Blank Panel
        add.accept(new ItemStack(ModBlocks.RBMK_REFLECTOR.get()));
        add.accept(new ItemStack(ModBlocks.RBMK_ABSORBER.get()));
        add.accept(new ItemStack(ModBlocks.RBMK_MODERATOR.get()));
        add.accept(new ItemStack(ModBlocks.RBMK_OUTGASSER.get()));
        add.accept(new ItemStack(ModBlocks.RBMK_STORAGE.get()));
        add.accept(new ItemStack(ModBlocks.RBMK_COOLER.get()));
        add.accept(new ItemStack(ModBlocks.RBMK_HEATER.get()));
        add.accept(new ItemStack(ModItems.RBMK_CONSOLE.get()));
        add.accept(new ItemStack(ModBlocks.RBMK_CRANE_CONSOLE.get()));
        add.accept(new ItemStack(ModBlocks.RBMK_DISPLAY_BLANK.get()));

        // 151-160: RBMK Display, RoR Controls (Keypad, Lever, Gauge, Lights, Numitron, Graph, Terminal), Autoloader, Loader
        add.accept(new ItemStack(ModBlocks.RBMK_DISPLAY.get()));
        add.accept(new ItemStack(ModBlocks.RBMK_KEYPAD.get()));
        add.accept(new ItemStack(ModBlocks.RBMK_LEVER.get()));
        add.accept(new ItemStack(ModBlocks.RBMK_GAUGE.get()));
        add.accept(new ItemStack(ModBlocks.RBMK_INDICATOR.get()));
        add.accept(new ItemStack(ModBlocks.RBMK_NUMITRON.get()));
        add.accept(new ItemStack(ModBlocks.RBMK_GRAPH.get()));
        add.accept(new ItemStack(ModBlocks.RBMK_TERMINAL.get()));
        add.accept(new ItemStack(ModBlocks.RBMK_AUTOLOADER.get()));
        add.accept(new ItemStack(ModBlocks.RBMK_LOADER.get()));

        // 161-170: RBMK Steam Connectors, RBMK Debris (Normal/Burning/Radiating/Digamma), Red Copper Cables
        add.accept(new ItemStack(ModBlocks.RBMK_STEAM_INLET.get()));
        add.accept(new ItemStack(ModBlocks.RBMK_STEAM_OUTLET.get()));
        add.accept(new ItemStack(ModBlocks.RBMK_DEBRIS.get()));
        add.accept(new ItemStack(ModBlocks.RBMK_DEBRIS_BURNING.get()));
        add.accept(new ItemStack(ModBlocks.RBMK_DEBRIS_RADIATING.get()));
        add.accept(new ItemStack(ModBlocks.RBMK_DEBRIS_DIGAMMA.get()));
        add.accept(new ItemStack(ModBlocks.RED_CABLE.get()));
        add.accept(new ItemStack(ModBlocks.RED_CABLE_CLASSIC.get()));
        add.accept(new ItemStack(ModBlocks.RED_CABLE_PAINTABLE.get()));
        add.accept(new ItemStack(ModBlocks.RED_CABLE_GAUGE.get()));

        // 171-180: Red Copper Boxcable (0-4), Coated Cable, Electricity Connectors, Small Pylons
        for (int i = 0; i < 5; i++) add.accept(com.hbm_m.block.network.BoxDuctBlock.stack(ModBlocks.RED_CABLE_BOX.get().asItem(), i));
        add.accept(new ItemStack(ModBlocks.RED_WIRE_COATED.get()));
        add.accept(new ItemStack(ModBlocks.RED_CONNECTOR.get()));
        add.accept(new ItemStack(ModBlocks.RED_CONNECTOR_SUPER.get()));
        add.accept(new ItemStack(ModBlocks.RED_PYLON.get()));
        add.accept(new ItemStack(ModBlocks.RED_PYLON_STEEL.get()));

        // 181-190: Medium/Large Pylons, Substation, Switches, Network Separator (unported), Power Detector
        add.accept(new ItemStack(ModBlocks.RED_PYLON_MEDIUM_WOOD.get()));
        add.accept(new ItemStack(ModBlocks.RED_PYLON_MEDIUM_WOOD_TRANSFORMER.get()));
        add.accept(new ItemStack(ModBlocks.RED_PYLON_MEDIUM_STEEL.get()));
        add.accept(new ItemStack(ModBlocks.RED_PYLON_MEDIUM_STEEL_TRANSFORMER.get()));
        add.accept(new ItemStack(ModBlocks.RED_PYLON_LARGE.get()));
        add.accept(new ItemStack(ModItems.SUBSTATION.get()));
        add.accept(new ItemStack(ModBlocks.CABLE_SWITCH.get()));
        add.accept(new ItemStack(ModBlocks.CABLE_DETECTOR.get()));
        add.accept(new ItemStack(ModBlocks.NETWORK_SEPARATOR.get()));
        add.accept(new ItemStack(ModBlocks.DETECTOR.get()));

        // 191-200: Universal Fluid Ducts (Normal/Colored/Silver), Boxducts (0-6; 7-14 in Chunk 3)
        add.accept(new ItemStack(ModItems.FLUID_DUCT.get()));
        add.accept(new ItemStack(ModItems.FLUID_DUCT_COLORED.get()));
        add.accept(new ItemStack(ModItems.FLUID_DUCT_SILVER.get()));
        for (int i = 0; i < 15; i++) add.accept(com.hbm_m.block.network.BoxDuctBlock.stack(ModBlocks.FLUID_DUCT_BOX.get().asItem(), i));

        // --- CHUNK 3: Items 201-300 ---
        // 201-210: Boxducts (7-14; loop at 2176), Exhaust Pipes (0-3)
        for (int i = 0; i < 15; i += 3) add.accept(com.hbm_m.block.network.BoxDuctBlock.stack(ModBlocks.FLUID_DUCT_EXHAUST.get().asItem(), i));

        // 211-220: Exhaust Pipes (6, 9, 12; in loop above), Paintable Ducts, Pipe Anchors, Flow Gauge, Fluid Valve
        add.accept(new ItemStack(ModBlocks.FLUID_DUCT_PAINTABLE_BLOCK_EXHAUST.get()));
        add.accept(new ItemStack(ModBlocks.FLUID_DUCT_PAINTABLE.get()));
        add.accept(new ItemStack(ModBlocks.PIPE_ANCHOR.get()));
        add.accept(new ItemStack(ModBlocks.PIPE_ANCHOR_EXHAUST.get()));
        add.accept(new ItemStack(ModBlocks.PIPE_ANCHOR_PNEUMATIC.get()));
        add.accept(new ItemStack(ModBlocks.FLUID_DUCT_GAUGE.get()));
        add.accept(new ItemStack(ModItems.FLUID_VALVE.get()));

        // 221-230: Counter Valve, Fluid Switch, Flow Control Pump (unported), Drainage Pipe, Redstone-over-Radio
        add.accept(new ItemStack(ModBlocks.FLUID_COUNTER_VALVE.get()));
        add.accept(new ItemStack(ModBlocks.FLUID_SWITCH.get()));
        add.accept(new ItemStack(ModBlocks.FLOW_CONTROL_PUMP.get()));
        add.accept(new ItemStack(ModBlocks.MACHINE_DRAIN.get()));
        add.accept(new ItemStack(ModBlocks.RADIO_TORCH_SENDER.get()));
        add.accept(new ItemStack(ModBlocks.RADIO_TORCH_RECEIVER.get()));
        add.accept(new ItemStack(ModBlocks.RADIO_TORCH_COUNTER.get()));
        add.accept(new ItemStack(ModBlocks.RADIO_TORCH_LOGIC.get()));
        add.accept(new ItemStack(ModBlocks.RADIO_TORCH_READER.get()));
        add.accept(new ItemStack(ModBlocks.RADIO_TORCH_CONTROLLER.get()));

        // 231-240: Telex Machine, AUTOCAL, Conveyor Cranes
        add.accept(new ItemStack(ModBlocks.RADIO_TELEX.get()));
        add.accept(new ItemStack(ModBlocks.RADIO_AUTOCAL.get()));
        add.accept(new ItemStack(ModBlocks.CRANE_EXTRACTOR.get()));
        add.accept(new ItemStack(ModBlocks.CRANE_INSERTER.get()));
        add.accept(new ItemStack(ModBlocks.CRANE_GRABBER.get()));
        add.accept(new ItemStack(ModBlocks.CRANE_ROUTER.get()));
        add.accept(new ItemStack(ModBlocks.CRANE_BOXER.get()));
        add.accept(new ItemStack(ModBlocks.CRANE_UNBOXER.get()));
        add.accept(new ItemStack(ModBlocks.CRANE_SPLITTER.get()));
        add.accept(new ItemStack(ModBlocks.CRANE_PARTITIONER.get()));

        // 241-250: Drone Waypoints, Crate, Dock, Logistics Crates, Pneumatic Tubes, Storage Access/Clutter
        add.accept(new ItemStack(ModBlocks.DRONE_WAYPOINT.get()));
        add.accept(new ItemStack(ModBlocks.DRONE_CRATE.get()));
        add.accept(new ItemStack(ModBlocks.DRONE_WAYPOINT_REQUEST.get()));
        add.accept(new ItemStack(ModBlocks.DRONE_DOCK.get()));
        add.accept(new ItemStack(ModBlocks.DRONE_CRATE_PROVIDER.get()));
        add.accept(new ItemStack(ModBlocks.DRONE_CRATE_REQUESTER.get()));
        add.accept(new ItemStack(ModBlocks.PNEUMATIC_TUBE.get()));
        add.accept(new ItemStack(ModBlocks.PNEUMATIC_TUBE_PAINTABLE.get()));
        add.accept(new ItemStack(ModBlocks.PNEUMATIC_STORAGE_ACCESS.get()));
        add.accept(new ItemStack(ModBlocks.PNEUMATIC_STORAGE_CLUTTER.get()));

        // 251-260: Storage Mono/Importer/Exporter, Fan, Barrels, Battery Socket, FEnSU
        add.accept(new ItemStack(ModBlocks.PNEUMATIC_STORAGE_MONO.get()));
        add.accept(new ItemStack(ModBlocks.PNEUMATIC_STORAGE_IMPORTER.get()));
        add.accept(new ItemStack(ModBlocks.PNEUMATIC_STORAGE_EXPORTER.get()));
        add.accept(new ItemStack(ModBlocks.FAN.get()));
        add.accept(new ItemStack(ModBlocks.BARREL_PLASTIC.get()));
        add.accept(new ItemStack(ModBlocks.BARREL_STEEL.get()));
        add.accept(new ItemStack(ModBlocks.BARREL_TCALLOY.get()));
        add.accept(new ItemStack(ModBlocks.BARREL_ANTIMATTER.get()));
        add.accept(new ItemStack(ModItems.MACHINE_BATTERY_SOCKET.get()));
        add.accept(new ItemStack(ModItems.MACHINE_BATTERY_REDD.get()));

        // 261-270: Old Capacitor, Transformer, Converters (unported), Electric Furnace, Microwave, Rock Mill, Assembly Machine, Assembly Factory, Precision Assembler
        add.accept(new ItemStack(ModBlocks.CAPACITOR_COPPER.get()));
        add.accept(new ItemStack(ModBlocks.MACHINE_TRANSFORMER.get()));
        add.accept(new ItemStack(ModBlocks.MACHINE_CONVERTER_HE_RF.get()));
        add.accept(new ItemStack(ModBlocks.MACHINE_CONVERTER_RF_HE.get()));
        add.accept(new ItemStack(ModBlocks.ELECTRIC_FURNACE.get()));
        add.accept(new ItemStack(ModBlocks.MICROWAVE.get()));
        add.accept(new ItemStack(ModBlocks.MACHINE_ROCKMILL.get()));
        add.accept(new ItemStack(ModItems.ADVANCED_ASSEMBLY_MACHINE.get()));
        add.accept(new ItemStack(ModItems.ASSEMBLY_FACTORY.get()));
        add.accept(new ItemStack(ModBlocks.MACHINE_PRECASS.get()));

        // 271-280: Chemical Plant/Factory, PUREX, Arc Welder, Soldering Station, Arc Furnace, Mixer, Supercomputer, Fluid Tank, BAT9000
        add.accept(new ItemStack(ModItems.CHEMICAL_PLANT.get()));
        add.accept(new ItemStack(ModItems.CHEMICAL_FACTORY.get()));
        add.accept(new ItemStack(ModBlocks.PUREX.get()));
        add.accept(new ItemStack(ModItems.ARC_WELDER.get()));
        add.accept(new ItemStack(ModItems.SOLDERING_STATION.get()));
        add.accept(new ItemStack(ModBlocks.ARC_FURNACE.get()));
        add.accept(new ItemStack(ModItems.MIXER.get()));
        add.accept(new ItemStack(ModBlocks.MACHINE_SUPERCOMPUTER.get()));
        add.accept(new ItemStack(ModItems.FLUID_TANK.get()));
        add.accept(new ItemStack(ModItems.BAT9000.get()));

        // 281-290: Big-Ass Tank, Orbus, Steam Engine, Turbines, Condensers, Cooling Towers
        add.accept(new ItemStack(ModBlocks.MACHINE_BIGASSTANK.get()));
        add.accept(new ItemStack(ModBlocks.ORBUS.get()));
        add.accept(new ItemStack(ModBlocks.STEAM_ENGINE.get()));
        add.accept(new ItemStack(ModBlocks.MACHINE_LARGE_TURBINE.get()));
        add.accept(new ItemStack(ModItems.INDUSTRIAL_TURBINE.get()));
        add.accept(new ItemStack(ModItems.MACHINE_CHUNGUS.get()));
        add.accept(new ItemStack(ModBlocks.STEAM_CONDENSER.get()));
        add.accept(new ItemStack(ModItems.TOWER_SMALL.get()));
        add.accept(new ItemStack(ModItems.COOLING_TOWER.get()));
        add.accept(new ItemStack(ModBlocks.CONDENSER_POWERED.get()));

        // 291-300: Deuterium Extractor/Tower, Liquefactor, Solidifier, Air Intake, Compressors, Electrolyser, Waste/Storage Drums
        add.accept(new ItemStack(ModBlocks.DEUTERIUM_EXTRACTOR.get()));
        add.accept(new ItemStack(ModItems.DEUTERIUM_TOWER.get()));
        add.accept(new ItemStack(ModItems.LIQUEFACTOR.get()));
        add.accept(new ItemStack(ModBlocks.SOLIDIFIER.get()));
        add.accept(new ItemStack(ModBlocks.INTAKE.get()));
        add.accept(new ItemStack(ModBlocks.COMPRESSOR.get()));
        add.accept(new ItemStack(ModBlocks.COMPRESSOR_COMPACT.get()));
        add.accept(new ItemStack(ModBlocks.ELECTROLYSER.get()));
        add.accept(new ItemStack(ModBlocks.MACHINE_WASTE_DRUM.get()));
        add.accept(new ItemStack(ModBlocks.MACHINE_STORAGE_DRUM.get()));

        // --- CHUNK 4: Items 301-400 ---
        // 301-310: Shredder, Oil Derrick, Pumpjack, Fracking Tower, Flare Stack, Chimneys, Refinery, Vacuum Distill, Fraction Tower
        add.accept(new ItemStack(ModBlocks.SHREDDER.get()));
        add.accept(new ItemStack(ModItems.DERRICK.get()));
        add.accept(new ItemStack(ModItems.PUMPJACK.get()));
        add.accept(new ItemStack(ModItems.HYDRAULIC_FRACKINING_TOWER.get()));
        add.accept(new ItemStack(ModItems.FLARE_STACK.get()));
        add.accept(new ItemStack(ModBlocks.CHIMNEY_BRICK.get()));
        add.accept(new ItemStack(ModBlocks.CHIMNEY_INDUSTRIAL.get()));
        add.accept(new ItemStack(ModItems.REFINERY.get()));
        add.accept(new ItemStack(ModItems.VACUUM_DISTILL.get()));
        add.accept(new ItemStack(ModItems.FRACTION_TOWER.get()));

        // 311-320: Fraction Spacer, Cracking Tower, Catalytic Reformer, Hydrotreater, Coker, Pyro Oven, Auto Saw, Thresher, Mining Drill, Ore Slopper
        add.accept(new ItemStack(ModItems.FRACTION_SPACER.get()));
        add.accept(new ItemStack(ModItems.CRACKING_TOWER.get()));
        add.accept(new ItemStack(ModItems.CATALYTIC_REFORMER.get()));
        add.accept(new ItemStack(ModItems.HYDROTREATER.get()));
        add.accept(new ItemStack(ModBlocks.COKER.get()));
        add.accept(new ItemStack(ModBlocks.PYROOVEN.get()));
        add.accept(new ItemStack(ModBlocks.AUTOSAW.get()));
        add.accept(new ItemStack(ModBlocks.THRESHER.get()));
        add.accept(new ItemStack(ModItems.MINING_DRILL.get()));
        add.accept(new ItemStack(ModBlocks.ORE_SLOPPER.get()));

        // 321-330: Annihilator, Mining Laser, Turbofan, Gas Turbine, LPW-2, Teleporter, Tele Anchor, FM Radio, Radio Box, Torus Core
        add.accept(new ItemStack(ModBlocks.ANNIHILATOR.get()));
        add.accept(new ItemStack(ModBlocks.MINING_LASER.get()));
        add.accept(new ItemStack(ModItems.TURBOFAN.get()));
        add.accept(new ItemStack(ModBlocks.TURBINEGAS.get()));
        add.accept(new ItemStack(ModBlocks.LPW2.get()));
        add.accept(new ItemStack(ModBlocks.MACHINE_TELEPORTER.get()));
        add.accept(new ItemStack(ModBlocks.TELEANCHOR.get()));
        add.accept(new ItemStack(ModBlocks.RADIOREC.get()));
        add.accept(new ItemStack(ModBlocks.RADIOBOX.get()));
        add.accept(new ItemStack(ModBlocks.STRUCT_TORUS_CORE.get()));

        // 331-340: Watz Core, ICF Core, Rad Absorbers (Tiers 0-3), Decon, Solar Boiler, Solar Mirror, Custom Machine Controller
        add.accept(new ItemStack(ModBlocks.STRUCT_WATZ_CORE.get()));
        add.accept(new ItemStack(ModBlocks.STRUCT_ICF_CORE.get()));
        for (com.hbm_m.block.generic.BlockAbsorber.EnumAbsorberTier tier : com.hbm_m.block.generic.BlockAbsorber.EnumAbsorberTier.values()) {
            add.accept(com.hbm_m.item.BlockAbsorberItem.forTier(ModBlocks.RAD_ABSORBER.get(), tier));
        }
        add.accept(new ItemStack(ModBlocks.DECON.get()));
        add.accept(new ItemStack(ModItems.SOLAR_BOILER.get()));
        add.accept(new ItemStack(ModBlocks.SOLAR_MIRROR.get()));
        for (var conf : com.hbm_m.config.CustomMachineConfigJSON.niceList) add.accept(com.hbm_m.block.machines.custom.ItemCustomMachine.make(conf.unlocalizedName));

        // 341-350: CM Blocks (Steel/Alloy/Desh/TcAlloy), CM Sheets (Steel/Alloy/Desh/TcAlloy), CM Engines (Standard/Desh; Bismuth in 351-360)
        for (var s : ModBlocks.CM_BLOCK.values()) add.accept(new ItemStack(s.get()));
        for (var s : ModBlocks.CM_SHEET.values()) add.accept(new ItemStack(s.get()));
        for (var s : ModBlocks.CM_ENGINE.values()) add.accept(new ItemStack(s.get()));

        // 351-360: CM Engine (Bismuth in loop above), CM Tanks (Steel/Alloy/Desh/TcAlloy), CM Circuits (Tiers 1-5)
        for (var s : ModBlocks.CM_TANK.values()) add.accept(new ItemStack(s.get()));
        for (var s : ModBlocks.CM_CIRCUIT.values()) add.accept(new ItemStack(s.get()));

        // 361-370: CM Ports (Steel/Alloy/Desh/TcAlloy), CM Flux, CM Heat, CM Anchor, Chicago Pile Bricks, Loader, Vent (Control Rod in 371-380)
        for (var s : ModBlocks.CM_PORT.values()) add.accept(new ItemStack(s.get()));
        add.accept(new ItemStack(ModBlocks.CM_FLUX.get()));
        add.accept(new ItemStack(ModBlocks.CM_HEAT.get()));
        add.accept(new ItemStack(ModBlocks.CM_ANCHOR.get()));
        add.accept(new ItemStack(ModBlocks.PILE_BRICK.get()));
        add.accept(new ItemStack(ModBlocks.PILE_LOADER.get()));
        add.accept(new ItemStack(ModBlocks.PILE_VENT.get()));

        // 371-380: Chicago Pile Control Rod, PWR Fuel, Control, Channel, Heat Exchanger, Heatsink, Neutron Source, Reflector, Casing, Port
        add.accept(new ItemStack(ModBlocks.PILE_CONTROL.get()));
        add.accept(new ItemStack(ModBlocks.PWR_FUEL.get()));
        add.accept(new ItemStack(ModBlocks.PWR_CONTROL.get()));
        add.accept(new ItemStack(ModBlocks.PWR_CHANNEL.get()));
        add.accept(new ItemStack(ModBlocks.PWR_HEATEX.get()));
        add.accept(new ItemStack(ModBlocks.PWR_HEATSINK.get()));
        add.accept(new ItemStack(ModBlocks.PWR_NEUTRON_SOURCE.get()));
        add.accept(new ItemStack(ModBlocks.PWR_REFLECTOR.get()));
        add.accept(new ItemStack(ModBlocks.PWR_CASING.get()));
        add.accept(new ItemStack(ModBlocks.PWR_PORT.get()));

        // 381-390: PWR Controller, Fusion Components (BSCCO, Welded, Blanket, Piping), Torus, Klystrons (Standard/Creative), Fusion Breeder, Collector
        add.accept(new ItemStack(ModBlocks.PWR_CONTROLLER.get()));
        add.accept(new ItemStack(ModBlocks.FUSION_COMPONENT.get()));
        add.accept(new ItemStack(ModBlocks.FUSION_COMPONENT_BSCCO_WELDED.get()));
        add.accept(new ItemStack(ModBlocks.FUSION_COMPONENT_BLANKET.get()));
        add.accept(new ItemStack(ModBlocks.FUSION_COMPONENT_MOTOR.get()));
        add.accept(new ItemStack(ModItems.FUSION_TORUS_ITEM.get()));
        add.accept(new ItemStack(ModItems.FUSION_KLYSTRON_ITEM.get()));
        add.accept(new ItemStack(ModItems.FUSION_KLYSTRON_CREATIVE_ITEM.get()));
        add.accept(new ItemStack(ModItems.FUSION_BREEDER_ITEM.get()));
        add.accept(new ItemStack(ModItems.FUSION_COLLECTOR_ITEM.get()));

        // 391-400: Fusion Boiler, MHDT, Coupler, Plasma Forge, Watz Elements (Chamber, Cooler, End, End Bolted), Watz Powerplant, Watz Pump
        add.accept(new ItemStack(ModItems.FUSION_BOILER_ITEM.get()));
        add.accept(new ItemStack(ModItems.FUSION_MHDT_ITEM.get()));
        add.accept(new ItemStack(ModItems.FUSION_COUPLER_ITEM.get()));
        add.accept(new ItemStack(ModItems.FUSION_PLASMA_FORGE_ITEM.get()));
        add.accept(new ItemStack(ModBlocks.WATZ_ELEMENT.get()));
        add.accept(new ItemStack(ModBlocks.WATZ_COOLER.get()));
        add.accept(new ItemStack(ModBlocks.WATZ_END.get()));
        add.accept(new ItemStack(ModBlocks.WATZ_END_BOLTED.get()));
        add.accept(new ItemStack(ModItems.WATZ_POWERPLANT.get()));
        add.accept(new ItemStack(ModItems.WATZ_PUMP.get()));

        // --- CHUNK 5: Items 401-442 ---
        // 401-410: ICF Fuel Pellet Maker, ICF Laser Components (Casing, Port, Cell, Flash Tube/Emitter, Capacitor, Turbocharger), ICF Controller, ICF Components (Stability, Vessel)
        add.accept(new ItemStack(ModBlocks.MACHINE_ICF_PRESS.get()));
        add.accept(new ItemStack(ModBlocks.ICF_LASER_CASING.get()));
        add.accept(new ItemStack(ModBlocks.ICF_LASER_PORT.get()));
        add.accept(new ItemStack(ModBlocks.ICF_LASER_CELL.get()));
        add.accept(new ItemStack(ModBlocks.ICF_LASER_EMITTER.get()));
        add.accept(new ItemStack(ModBlocks.ICF_LASER_CAPACITOR.get()));
        add.accept(new ItemStack(ModBlocks.ICF_LASER_TURBOCHARGER.get()));
        add.accept(new ItemStack(ModBlocks.ICF_CONTROLLER.get()));
        add.accept(new ItemStack(ModBlocks.ICF_COMPONENT.get()));
        add.accept(new ItemStack(ModBlocks.ICF_COMPONENT_VESSEL.get()));

        // 411-420: ICF Components (Vessel Welded, Casing, Casing Riveted), ICF Reactor, DFC (Emitter, Injector, Receiver, Stabilizer, Core), Chlorine Vent
        add.accept(new ItemStack(ModBlocks.ICF_COMPONENT_VESSEL_WELDED.get()));
        add.accept(new ItemStack(ModBlocks.ICF_COMPONENT_STRUCTURE.get()));
        add.accept(new ItemStack(ModBlocks.ICF_COMPONENT_STRUCTURE_BOLTED.get()));
        add.accept(new ItemStack(ModBlocks.ICF.get()));
        add.accept(new ItemStack(ModItems.CORE_EMITTER.get()));
        add.accept(new ItemStack(ModItems.CORE_INJECTOR.get()));
        add.accept(new ItemStack(ModItems.CORE_RECEIVER.get()));
        add.accept(new ItemStack(ModBlocks.DFC_STABILIZER.get()));
        add.accept(new ItemStack(ModBlocks.DFC_CORE.get()));
        add.accept(new ItemStack(ModBlocks.VENT_CHLORINE.get()));

        // 421-430: Cloud Vent, Pink Cloud Vent, Chlorine Seal, Chlorine Gas, Radon Gas, Dense Radon, Tomb Gas, Meltdown Gas, Carbon Monoxide, Airborne Asbestos
        add.accept(new ItemStack(ModBlocks.VENT_CLOUD.get()));
        add.accept(new ItemStack(ModBlocks.VENT_PINK_CLOUD.get()));
        add.accept(new ItemStack(ModBlocks.VENT_CHLORINE_SEAL.get()));
        add.accept(new ItemStack(ModBlocks.CHLORINE_GAS.get()));
        add.accept(new ItemStack(ModBlocks.GAS_RADON.get()));
        add.accept(new ItemStack(ModBlocks.GAS_RADON_DENSE.get()));
        add.accept(new ItemStack(ModBlocks.GAS_RADON_TOMB.get()));
        add.accept(new ItemStack(ModBlocks.GAS_MELTDOWN.get()));
        add.accept(new ItemStack(ModBlocks.GAS_MONOXIDE.get()));
        add.accept(new ItemStack(ModBlocks.GAS_ASBESTOS.get()));

        // 431-440: Coal Dust, Flammable Gas, Explosive Gas, Drones (Patrol, Chunkloading, Express, Express Chunkloading, Logistics), Conveyor Wands (Regular, Express)
        add.accept(new ItemStack(ModBlocks.GAS_COAL.get()));
        add.accept(new ItemStack(ModBlocks.GAS_FLAMMABLE.get()));
        add.accept(new ItemStack(ModBlocks.GAS_EXPLOSIVE.get()));
        add.accept(new ItemStack(ModItems.DRONE_PATROL.get()));
        add.accept(new ItemStack(ModItems.DRONE_PATROL_CHUNKLOADING.get()));
        add.accept(new ItemStack(ModItems.DRONE_PATROL_EXPRESS.get()));
        add.accept(new ItemStack(ModItems.DRONE_PATROL_EXPRESS_CHUNKLOADING.get()));
        add.accept(new ItemStack(ModItems.DRONE_REQUEST.get()));
        add.accept(new ItemStack(ModItems.CONVEYOR_WAND_REGULAR.get()));
        add.accept(new ItemStack(ModItems.CONVEYOR_WAND_EXPRESS.get()));

        // 441-442: Conveyor Wands (Double-Lane, Triple-Lane)
        add.accept(new ItemStack(ModItems.CONVEYOR_WAND_DOUBLE.get()));
        add.accept(new ItemStack(ModItems.CONVEYOR_WAND_TRIPLE.get()));
    }

    /** populateNukeTab: порядок из оригинального 1.7.10 (ModBlocks/ModItems, вкладка nuke); отсутствующие в порте предметы пропущены. */
    public static void populateNukeTab(BiConsumer<ItemStack, CreativeModeTab.TabVisibility> acceptor) {
        Consumer<ItemStack> add = stack -> acceptor.accept(stack, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);

        // 1-10
        add.accept(new ItemStack(ModBlocks.NUKE_GADGET.get()));
        add.accept(new ItemStack(ModBlocks.NUKE_BOY.get()));
        add.accept(new ItemStack(ModItems.NUKE_FAT_MAN.get()));
        add.accept(new ItemStack(ModBlocks.NUKE_MIKE.get()));
        add.accept(new ItemStack(ModBlocks.NUKE_TSAR.get()));
        add.accept(new ItemStack(ModItems.NUKE_PROTOTYPE.get()));
        add.accept(new ItemStack(ModBlocks.NUKE_FLEIJA.get()));
        add.accept(new ItemStack(ModBlocks.NUKE_SOLINIUM.get()));
        add.accept(new ItemStack(ModBlocks.NUKE_N2.get()));
        add.accept(new ItemStack(ModBlocks.NUKE_FSTBMB.get()));
        // 11-20
        add.accept(new ItemStack(ModBlocks.NUKE_CUSTOM.get()));
        add.accept(new ItemStack(ModBlocks.BOMB_MULTI.get()));
        add.accept(new ItemStack(ModBlocks.DUD_BALEFIRE.get()));
        add.accept(new ItemStack(ModBlocks.DUD_CONVENTIONAL.get()));
        add.accept(new ItemStack(ModBlocks.DUD_NUKE.get()));
        add.accept(new ItemStack(ModBlocks.DUD_SALTED.get()));
        add.accept(new ItemStack(ModBlocks.FIREWORKS.get()));
        add.accept(new ItemStack(ModBlocks.DYNAMITE.get()));
        add.accept(new ItemStack(ModBlocks.TNT_NTM.get()));
        add.accept(new ItemStack(ModBlocks.SEMTEX.get()));

        // 21-30
        add.accept(new ItemStack(ModBlocks.C4.get()));
        add.accept(new ItemStack(ModBlocks.FISSURE_BOMB.get()));
        add.accept(new ItemStack(ModBlocks.CHARGE_DYNAMITE.get()));
        add.accept(new ItemStack(ModBlocks.CHARGE_MINER.get()));
        add.accept(new ItemStack(ModBlocks.CHARGE_C4.get()));
        add.accept(new ItemStack(ModBlocks.CHARGE_SEMTEX.get()));
        add.accept(new ItemStack(ModBlocks.MINE_AP.get()));
        add.accept(new ItemStack(ModBlocks.MINE_SHRAP.get()));
        add.accept(new ItemStack(ModBlocks.MINE_HE.get()));
        add.accept(new ItemStack(ModBlocks.MINE_FAT.get()));

        // 31-40
        add.accept(new ItemStack(ModBlocks.MINE_NAVAL.get()));
        add.accept(new ItemStack(ModBlocks.FLAME_WAR.get()));
        add.accept(new ItemStack(ModBlocks.FLOAT_BOMB.get()));
        add.accept(new ItemStack(ModBlocks.THERM_ENDO.get()));
        add.accept(new ItemStack(ModBlocks.THERM_EXO.get()));
        add.accept(new ItemStack(ModBlocks.EMP_BOMB.get()));
        add.accept(new ItemStack(ModBlocks.DET_CORD.get()));
        add.accept(new ItemStack(ModBlocks.DET_CHARGE.get()));
        add.accept(new ItemStack(ModBlocks.DET_NUKE.get()));
        add.accept(new ItemStack(ModBlocks.DET_MINER.get()));

        // 41-46
        add.accept(new ItemStack(ModBlocks.BARREL_RED.get()));
        add.accept(new ItemStack(ModBlocks.BARREL_PINK.get()));
        add.accept(new ItemStack(ModBlocks.BARREL_LOX.get()));
        add.accept(new ItemStack(ModBlocks.BARREL_TAINT.get()));
        add.accept(new ItemStack(ModBlocks.BARREL_YELLOW.get()));
        add.accept(new ItemStack(ModBlocks.BARREL_VITRIFIED.get()));
        // 47-50
        for (int i = 0; i < 4; i++) add.accept(com.hbm_m.block.bomb.VolcanoBlock.stack(ModBlocks.VOLCANO_CORE.get().asItem(), i));

        // 51-60
        add.accept(com.hbm_m.block.bomb.VolcanoBlock.stack(ModBlocks.VOLCANO_CORE.get().asItem(), 4));
        for (int i = 0; i < 5; i++) add.accept(com.hbm_m.block.bomb.VolcanoBlock.stack(ModBlocks.VOLCANO_RAD_CORE.get().asItem(), i));
        add.accept(new ItemStack(ModBlocks.BOOK_GUIDE.get()));
        add.accept(new ItemStack(ModItems.DEMON_CORE_OPEN.get()));
        add.accept(new ItemStack(ModItems.DEMON_CORE_CLOSED.get()));
        add.accept(makeChargedBattery(ModItems.BATTERY_SPARK.get()));

        // 61-70
        add.accept(makeChargedBattery(ModItems.BATTERY_TRIXITE.get()));
        add.accept(new ItemStack(ModItems.FAT_MAN_EXPLOSIVE.get()));
        add.accept(new ItemStack(ModItems.EXPLOSIVE_LENSES.get()));
        add.accept(new ItemStack(ModItems.GADGET_WIREING.get()));
        add.accept(new ItemStack(ModItems.GADGET_CORE.get()));
        add.accept(new ItemStack(ModItems.BOY_SHIELDING.get()));
        add.accept(new ItemStack(ModItems.BOY_TARGET.get()));
        add.accept(new ItemStack(ModItems.BOY_BULLET.get()));
        add.accept(new ItemStack(ModItems.BOY_PROPELLANT.get()));
        add.accept(new ItemStack(ModItems.BOY_IGNITER.get()));

        // 71-80
        add.accept(new ItemStack(ModItems.FAT_MAN_IGNITER.get()));
        add.accept(new ItemStack(ModItems.FAT_MAN_CORE.get()));
        add.accept(new ItemStack(ModItems.MIKE_CORE.get()));
        add.accept(new ItemStack(ModItems.MIKE_DEUT.get()));
        add.accept(new ItemStack(ModItems.MIKE_COOLING_UNIT.get()));
        add.accept(new ItemStack(ModItems.TSAR_CORE.get()));
        add.accept(new ItemStack(ModItems.FLEIJA_IGNITER.get()));
        add.accept(new ItemStack(ModItems.FLEIJA_PROPELLANT.get()));
        add.accept(new ItemStack(ModItems.FLEIJA_CORE.get()));
        add.accept(new ItemStack(ModItems.SOLINIUM_IGNITER.get()));

        // 81-90
        add.accept(new ItemStack(ModItems.SOLINIUM_PROPELLANT.get()));
        add.accept(new ItemStack(ModItems.SOLINIUM_CORE.get()));
        add.accept(new ItemStack(ModItems.N2_CHARGE.get()));
        add.accept(new ItemStack(ModItems.EGG_BALEFIRE_SHARD.get()));
        add.accept(new ItemStack(ModItems.EGG_BALEFIRE.get()));
        add.accept(new ItemStack(ModItems.CUSTOM_TNT.get()));
        add.accept(new ItemStack(ModItems.CUSTOM_NUKE.get()));
        add.accept(new ItemStack(ModItems.CUSTOM_HYDRO.get()));
        add.accept(new ItemStack(ModItems.CUSTOM_AMAT.get()));
        add.accept(new ItemStack(ModItems.CUSTOM_DIRTY.get()));
        // Joke component for the custom nuke
        add.accept(new ItemStack(ModBlocks.FREAKY_ALIEN_BLOCK.get()));

        // 91-100
        add.accept(new ItemStack(ModItems.CUSTOM_SCHRAB.get()));
        add.accept(new ItemStack(ModItems.CUSTOM_FALL.get()));
        add.accept(new ItemStack(ModItems.IGNITER.get()));
        add.accept(new ItemStack(ModItems.DETONATOR.get()));
        add.accept(new ItemStack(ModItems.MULTI_DETONATOR.get()));
        add.accept(new ItemStack(ModItems.RANGE_DETONATOR.get()));
        add.accept(new ItemStack(ModItems.DETONATOR_DEADMAN.get()));
        add.accept(new ItemStack(ModItems.DETONATOR_DE.get()));
        add.accept(new ItemStack(ModItems.DEFUSER.get()));
        add.accept(new ItemStack(ModItems.GADGET_KIT.get()));

        // 101-109
        add.accept(new ItemStack(ModItems.BOY_KIT.get()));
        add.accept(new ItemStack(ModItems.MAN_KIT.get()));
        add.accept(new ItemStack(ModItems.MIKE_KIT.get()));
        add.accept(new ItemStack(ModItems.TSAR_KIT.get()));
        add.accept(new ItemStack(ModItems.PROTOTYPE_KIT.get()));
        add.accept(new ItemStack(ModItems.FLEIJA_KIT.get()));
        add.accept(new ItemStack(ModItems.SOLINIUM_KIT.get()));
        add.accept(new ItemStack(ModItems.MULTI_KIT.get()));
        add.accept(new ItemStack(ModItems.CUSTOM_KIT.get()));
    }

    /** populateMissilesTab: порядок из оригинального 1.7.10 (ModBlocks/ModItems, вкладка missile); отсутствующие в порте предметы пропущены. */
    public static void populateMissilesTab(BiConsumer<ItemStack, CreativeModeTab.TabVisibility> acceptor) {
        Consumer<ItemStack> add = stack -> acceptor.accept(stack, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);

        // 1-10
        add.accept(new ItemStack(ModBlocks.MACHINE_SATLINKER.get()));
        add.accept(new ItemStack(ModBlocks.MACHINE_SATLINK.get()));
        add.accept(new ItemStack(ModBlocks.MACHINE_TAPE_DRIVE.get()));
        add.accept(new ItemStack(ModBlocks.MACHINE_FORCEFIELD.get()));
        add.accept(new ItemStack(ModBlocks.STRUCT_LAUNCHER.get()));
        add.accept(new ItemStack(ModBlocks.STRUCT_SCAFFOLD.get()));
        add.accept(new ItemStack(ModBlocks.STRUCT_LAUNCHER_CORE.get()));
        add.accept(new ItemStack(ModBlocks.STRUCT_LAUNCHER_CORE_LARGE.get()));
        add.accept(new ItemStack(ModBlocks.STRUCT_SOYUZ_CORE.get()));
        add.accept(new ItemStack(ModBlocks.MACHINE_MISSILE_ASSEMBLY.get()));

        // 11-20
        add.accept(new ItemStack(ModItems.LAUNCH_PAD.get()));
        add.accept(new ItemStack(ModItems.LAUNCH_PAD_RUSTED.get()));
        add.accept(new ItemStack(ModBlocks.LAUNCH_PAD_LARGE.get()));
        add.accept(new ItemStack(ModBlocks.COMPACT_LAUNCHER.get()));
        add.accept(new ItemStack(ModBlocks.LAUNCH_TABLE.get()));
        add.accept(new ItemStack(ModItems.SOYUZ_LAUNCHER.get()));
        add.accept(new ItemStack(ModBlocks.LAUNCHPAD_SOYUZ.get()));
        add.accept(new ItemStack(ModBlocks.LAUNCHPAD_LAMBDA.get()));
        add.accept(new ItemStack(ModBlocks.SAT_DOCK.get()));
        add.accept(new ItemStack(ModBlocks.SOYUZ_CAPSULE.get()));

        // 21-30
        add.accept(new ItemStack(ModBlocks.CRATE_SUPPLY.get()));
        add.accept(new ItemStack(ModItems.RADAR.get()));
        add.accept(new ItemStack(ModItems.LARGE_RADAR.get()));
        add.accept(new ItemStack(ModItems.RADAR_SCREEN.get()));
        add.accept(new ItemStack(ModItems.RANGEFINDER.get()));
        add.accept(new ItemStack(ModItems.DESIGNATOR.get()));
        add.accept(new ItemStack(ModItems.DESIGNATOR_RANGE.get()));
        add.accept(new ItemStack(ModItems.DESIGNATOR_MANUAL.get()));
        add.accept(new ItemStack(ModItems.DESIGNATOR_ARTY_RANGE.get()));
        add.accept(new ItemStack(ModItems.MISSILE_TAINT.get()));

        // 31-40
        add.accept(new ItemStack(ModItems.MISSILE_MICRO.get()));
        add.accept(new ItemStack(ModItems.MISSILE_BHOLE.get()));
        add.accept(new ItemStack(ModItems.MISSILE_SCHRABIDIUM.get()));
        add.accept(new ItemStack(ModItems.MISSILE_EMP.get()));
        add.accept(new ItemStack(ModItems.MISSILE_GENERIC.get()));
        add.accept(new ItemStack(ModItems.MISSILE_DECOY.get()));
        add.accept(new ItemStack(ModItems.MISSILE_INCENDIARY.get()));
        add.accept(new ItemStack(ModItems.MISSILE_CLUSTER.get()));
        add.accept(new ItemStack(ModItems.MISSILE_BUSTER.get()));
        add.accept(new ItemStack(ModItems.MISSILE_STEALTH.get()));

        // 41-50
        add.accept(new ItemStack(ModItems.MISSILE_ANTI_BALLISTIC.get()));
        add.accept(new ItemStack(ModItems.MISSILE_STRONG.get()));
        add.accept(new ItemStack(ModItems.MISSILE_INCENDIARY_STRONG.get()));
        add.accept(new ItemStack(ModItems.MISSILE_CLUSTER_STRONG.get()));
        add.accept(new ItemStack(ModItems.MISSILE_BUSTER_STRONG.get()));
        add.accept(new ItemStack(ModItems.MISSILE_EMP_STRONG.get()));
        add.accept(new ItemStack(ModItems.MISSILE_BURST.get()));
        add.accept(new ItemStack(ModItems.MISSILE_INFERNO.get()));
        add.accept(new ItemStack(ModItems.MISSILE_RAIN.get()));
        add.accept(new ItemStack(ModItems.MISSILE_DRILL.get()));

        // 51-60
        add.accept(new ItemStack(ModItems.MISSILE_SHUTTLE.get()));
        add.accept(new ItemStack(ModItems.MISSILE_NUCLEAR.get()));
        add.accept(new ItemStack(ModItems.MISSILE_NUCLEAR_CLUSTER.get()));
        add.accept(new ItemStack(ModItems.MISSILE_VOLCANO.get()));
        add.accept(new ItemStack(ModItems.MISSILE_DOOMSDAY.get()));
        add.accept(new ItemStack(ModItems.MISSILE_DOOMSDAY_RUSTED.get()));
        add.accept(new ItemStack(ModItems.MISSILE_SOYUZ.get()));
        add.accept(new ItemStack(ModItems.MISSILE_SOYUZ_LUNA.get()));
        add.accept(new ItemStack(ModItems.MISSILE_SOYUZ_POSTWAR.get()));
        add.accept(new ItemStack(ModItems.MISSILE_SOYUZ_LANDER.get()));

        // 61-70
        add.accept(new ItemStack(ModItems.MISSILE_LAMBDA.get()));
        add.accept(new ItemStack(MissilePartItems.get("mp_thruster_10_kerosene").get()));
        add.accept(new ItemStack(MissilePartItems.get("mp_thruster_10_solid").get()));
        add.accept(new ItemStack(MissilePartItems.get("mp_thruster_10_xenon").get()));
        add.accept(new ItemStack(MissilePartItems.get("mp_thruster_15_kerosene").get()));
        add.accept(new ItemStack(MissilePartItems.get("mp_thruster_15_kerosene_dual").get()));
        add.accept(new ItemStack(MissilePartItems.get("mp_thruster_15_kerosene_triple").get()));
        add.accept(new ItemStack(MissilePartItems.get("mp_thruster_15_solid").get()));
        add.accept(new ItemStack(MissilePartItems.get("mp_thruster_15_solid_hexdecuple").get()));
        add.accept(new ItemStack(MissilePartItems.get("mp_thruster_15_hydrogen").get()));

        // 71-80
        add.accept(new ItemStack(MissilePartItems.get("mp_thruster_15_hydrogen_dual").get()));
        add.accept(new ItemStack(MissilePartItems.get("mp_thruster_15_balefire_short").get()));
        add.accept(new ItemStack(MissilePartItems.get("mp_thruster_15_balefire").get()));
        add.accept(new ItemStack(MissilePartItems.get("mp_thruster_15_balefire_large").get()));
        add.accept(new ItemStack(MissilePartItems.get("mp_thruster_15_balefire_large_rad").get()));
        add.accept(new ItemStack(MissilePartItems.get("mp_thruster_20_kerosene").get()));
        add.accept(new ItemStack(MissilePartItems.get("mp_thruster_20_kerosene_dual").get()));
        add.accept(new ItemStack(MissilePartItems.get("mp_thruster_20_kerosene_triple").get()));
        add.accept(new ItemStack(MissilePartItems.get("mp_thruster_20_solid").get()));
        add.accept(new ItemStack(MissilePartItems.get("mp_thruster_20_solid_multi").get()));

        // 81-90
        add.accept(new ItemStack(MissilePartItems.get("mp_thruster_20_solid_multier").get()));
        add.accept(new ItemStack(MissilePartItems.get("mp_stability_10_flat").get()));
        add.accept(new ItemStack(MissilePartItems.get("mp_stability_10_cruise").get()));
        add.accept(new ItemStack(MissilePartItems.get("mp_stability_10_space").get()));
        add.accept(new ItemStack(MissilePartItems.get("mp_stability_15_flat").get()));
        add.accept(new ItemStack(MissilePartItems.get("mp_stability_15_thin").get()));
        add.accept(new ItemStack(MissilePartItems.get("mp_stability_15_soyuz").get()));
        add.accept(new ItemStack(MissilePartItems.get("mp_s_20").get()));
        add.accept(new ItemStack(MissilePartItems.get("mp_fuselage_10_kerosene").get()));
        add.accept(new ItemStack(MissilePartItems.get("mp_fuselage_10_kerosene_camo").get()));

        // 91-100
        add.accept(new ItemStack(MissilePartItems.get("mp_fuselage_10_kerosene_desert").get()));
        add.accept(new ItemStack(MissilePartItems.get("mp_fuselage_10_kerosene_sky").get()));
        add.accept(new ItemStack(MissilePartItems.get("mp_fuselage_10_kerosene_flames").get()));
        add.accept(new ItemStack(MissilePartItems.get("mp_fuselage_10_kerosene_insulation").get()));
        add.accept(new ItemStack(MissilePartItems.get("mp_fuselage_10_kerosene_sleek").get()));
        add.accept(new ItemStack(MissilePartItems.get("mp_fuselage_10_kerosene_metal").get()));
        add.accept(new ItemStack(MissilePartItems.get("mp_fuselage_10_kerosene_taint").get()));
        add.accept(new ItemStack(MissilePartItems.get("mp_fuselage_10_solid").get()));
        add.accept(new ItemStack(MissilePartItems.get("mp_fuselage_10_solid_flames").get()));
        add.accept(new ItemStack(MissilePartItems.get("mp_fuselage_10_solid_insulation").get()));

        // --- CHUNK 2: Items 101-200 (worker_m1_missile_c2) ---
        // 101-110
        add.accept(new ItemStack(MissilePartItems.get("mp_fuselage_10_solid_sleek").get()));
        add.accept(new ItemStack(MissilePartItems.get("mp_fuselage_10_solid_soviet_glory").get()));
        add.accept(new ItemStack(MissilePartItems.get("mp_fuselage_10_solid_cathedral").get()));
        add.accept(new ItemStack(MissilePartItems.get("mp_fuselage_10_solid_moonlit").get()));
        add.accept(new ItemStack(MissilePartItems.get("mp_fuselage_10_solid_battery").get()));
        add.accept(new ItemStack(MissilePartItems.get("mp_fuselage_10_solid_duracell").get()));
        add.accept(new ItemStack(MissilePartItems.get("mp_fuselage_10_xenon").get()));
        add.accept(new ItemStack(MissilePartItems.get("mp_fuselage_10_xenon_bhole").get()));
        add.accept(new ItemStack(MissilePartItems.get("mp_fuselage_10_long_kerosene").get()));
        add.accept(new ItemStack(MissilePartItems.get("mp_fuselage_10_long_kerosene_camo").get()));

        // 111-120
        add.accept(new ItemStack(MissilePartItems.get("mp_fuselage_10_long_kerosene_desert").get()));
        add.accept(new ItemStack(MissilePartItems.get("mp_fuselage_10_long_kerosene_sky").get()));
        add.accept(new ItemStack(MissilePartItems.get("mp_fuselage_10_long_kerosene_flames").get()));
        add.accept(new ItemStack(MissilePartItems.get("mp_fuselage_10_long_kerosene_insulation").get()));
        add.accept(new ItemStack(MissilePartItems.get("mp_fuselage_10_long_kerosene_sleek").get()));
        add.accept(new ItemStack(MissilePartItems.get("mp_fuselage_10_long_kerosene_metal").get()));
        add.accept(new ItemStack(MissilePartItems.get("mp_fuselage_10_long_kerosene_taint").get()));
        add.accept(new ItemStack(MissilePartItems.get("mp_fuselage_10_long_kerosene_vap").get()));
        add.accept(new ItemStack(MissilePartItems.get("mp_fuselage_10_long_solid").get()));
        add.accept(new ItemStack(MissilePartItems.get("mp_fuselage_10_long_solid_flames").get()));

        // 121-130
        add.accept(new ItemStack(MissilePartItems.get("mp_fuselage_10_long_solid_insulation").get()));
        add.accept(new ItemStack(MissilePartItems.get("mp_fuselage_10_long_solid_sleek").get()));
        add.accept(new ItemStack(MissilePartItems.get("mp_fuselage_10_long_solid_soviet_glory").get()));
        add.accept(new ItemStack(MissilePartItems.get("mp_fuselage_10_long_solid_bullet").get()));
        add.accept(new ItemStack(MissilePartItems.get("mp_fuselage_10_long_solid_silvermoonlight").get()));
        add.accept(new ItemStack(MissilePartItems.get("mp_fuselage_10_15_kerosene").get()));
        add.accept(new ItemStack(MissilePartItems.get("mp_fuselage_10_15_solid").get()));
        add.accept(new ItemStack(MissilePartItems.get("mp_fuselage_10_15_hydrogen").get()));
        add.accept(new ItemStack(MissilePartItems.get("mp_fuselage_10_15_balefire").get()));
        add.accept(new ItemStack(MissilePartItems.get("mp_fuselage_15_kerosene").get()));

        // 131-140
        add.accept(new ItemStack(MissilePartItems.get("mp_fuselage_15_kerosene_camo").get()));
        add.accept(new ItemStack(MissilePartItems.get("mp_fuselage_15_kerosene_desert").get()));
        add.accept(new ItemStack(MissilePartItems.get("mp_fuselage_15_kerosene_sky").get()));
        add.accept(new ItemStack(MissilePartItems.get("mp_fuselage_15_kerosene_insulation").get()));
        add.accept(new ItemStack(MissilePartItems.get("mp_fuselage_15_kerosene_metal").get()));
        add.accept(new ItemStack(MissilePartItems.get("mp_fuselage_15_kerosene_decorated").get()));
        add.accept(new ItemStack(MissilePartItems.get("mp_fuselage_15_kerosene_steampunk").get()));
        add.accept(new ItemStack(MissilePartItems.get("mp_fuselage_15_kerosene_polite").get()));
        add.accept(new ItemStack(MissilePartItems.get("mp_fuselage_15_kerosene_blackjack").get()));
        add.accept(new ItemStack(MissilePartItems.get("mp_fuselage_15_kerosene_lambda").get()));

        // 141-150
        add.accept(new ItemStack(MissilePartItems.get("mp_fuselage_15_kerosene_minuteman").get()));
        add.accept(new ItemStack(MissilePartItems.get("mp_fuselage_15_kerosene_taint").get()));
        add.accept(new ItemStack(MissilePartItems.get("mp_fuselage_15_kerosene_yuck").get()));
        add.accept(new ItemStack(MissilePartItems.get("mp_fuselage_15_solid").get()));
        add.accept(new ItemStack(MissilePartItems.get("mp_fuselage_15_solid_insulation").get()));
        add.accept(new ItemStack(MissilePartItems.get("mp_fuselage_15_solid_desh").get()));
        add.accept(new ItemStack(MissilePartItems.get("mp_fuselage_15_solid_soviet_glory").get()));
        add.accept(new ItemStack(MissilePartItems.get("mp_fuselage_15_solid_soviet_stank").get()));
        add.accept(new ItemStack(MissilePartItems.get("mp_fuselage_15_solid_faust").get()));
        add.accept(new ItemStack(MissilePartItems.get("mp_fuselage_15_solid_silvermoonlight").get()));

        // 151-160
        add.accept(new ItemStack(MissilePartItems.get("mp_fuselage_15_solid_snowy").get()));
        add.accept(new ItemStack(MissilePartItems.get("mp_fuselage_15_solid_panorama").get()));
        add.accept(new ItemStack(MissilePartItems.get("mp_fuselage_15_solid_roses").get()));
        add.accept(new ItemStack(MissilePartItems.get("mp_fuselage_15_solid_mimi").get()));
        add.accept(new ItemStack(MissilePartItems.get("mp_fuselage_15_hydrogen").get()));
        add.accept(new ItemStack(MissilePartItems.get("mp_fuselage_15_hydrogen_cathedral").get()));
        add.accept(new ItemStack(MissilePartItems.get("mp_fuselage_15_balefire").get()));
        add.accept(new ItemStack(MissilePartItems.get("mp_fuselage_15_20_kerosene").get()));
        add.accept(new ItemStack(MissilePartItems.get("mp_fuselage_15_20_kerosene_magnusson").get()));
        add.accept(new ItemStack(MissilePartItems.get("mp_fuselage_15_20_solid").get()));

        // 161-170
        add.accept(new ItemStack(MissilePartItems.get("mp_warhead_10_he").get()));
        add.accept(new ItemStack(MissilePartItems.get("mp_warhead_10_incendiary").get()));
        add.accept(new ItemStack(MissilePartItems.get("mp_warhead_10_buster").get()));
        add.accept(new ItemStack(MissilePartItems.get("mp_warhead_10_nuclear").get()));
        add.accept(new ItemStack(MissilePartItems.get("mp_warhead_10_nuclear_large").get()));
        add.accept(new ItemStack(MissilePartItems.get("mp_warhead_10_taint").get()));
        add.accept(new ItemStack(MissilePartItems.get("mp_warhead_10_cloud").get()));
        add.accept(new ItemStack(MissilePartItems.get("mp_warhead_15_he").get()));
        add.accept(new ItemStack(MissilePartItems.get("mp_warhead_15_incendiary").get()));
        add.accept(new ItemStack(MissilePartItems.get("mp_warhead_15_nuclear").get()));

        // 171-180
        add.accept(new ItemStack(MissilePartItems.get("mp_warhead_15_nuclear_shark").get()));
        add.accept(new ItemStack(MissilePartItems.get("mp_warhead_15_nuclear_mimi").get()));
        add.accept(new ItemStack(MissilePartItems.get("mp_warhead_15_boxcar").get()));
        add.accept(new ItemStack(MissilePartItems.get("mp_warhead_15_n2").get()));
        add.accept(new ItemStack(MissilePartItems.get("mp_warhead_15_balefire").get()));
        add.accept(new ItemStack(MissilePartItems.get("mp_warhead_15_turbine").get()));
        add.accept(new ItemStack(MissilePartItems.get("mp_c_1").get()));
        add.accept(new ItemStack(MissilePartItems.get("mp_c_2").get()));
        add.accept(new ItemStack(MissilePartItems.get("mp_c_3").get()));
        add.accept(new ItemStack(MissilePartItems.get("mp_c_4").get()));

        // 181-190
        add.accept(new ItemStack(MissilePartItems.get("mp_c_5").get()));
        add.accept(new ItemStack(ModItems.SATELLITE_SPY.get()));
        add.accept(new ItemStack(ModItems.SATELLITE_SCANNER.get()));
        add.accept(new ItemStack(ModItems.SATELLITE_RADAR.get()));
        add.accept(new ItemStack(ModItems.SATELLITE_MINER_ASTRO.get()));
        add.accept(new ItemStack(ModItems.SATELLITE_MINER_LUNAR.get()));
        add.accept(new ItemStack(ModItems.SATELLITE_PRECISION_LASER.get()));
        add.accept(new ItemStack(ModItems.SATELLITE_DEATH_RAY.get()));
        add.accept(new ItemStack(ModItems.SATELLITE_XENIUM_RESONATOR.get()));
        add.accept(new ItemStack(ModItems.SATELLITE_RELAY.get()));

        // 191-200
        add.accept(new ItemStack(ModItems.SATELLITE_DETECTOR.get()));
        add.accept(new ItemStack(ModItems.SATELLITE_RAY_SCAN.get()));
        add.accept(new ItemStack(ModItems.SATELLITE_SCIENCE.get()));
        add.accept(new ItemStack(ModItems.SATELLITE_SCIENCE_ASSEMBLER.get()));
        add.accept(new ItemStack(ModItems.SATELLITE_SCIENCE_SENSOR.get()));
        add.accept(new ItemStack(ModItems.SAT_GERALD.get()));
        add.accept(new ItemStack(ModItems.SAT_CHIP.get()));
        add.accept(new ItemStack(ModItems.SAT_COORD.get()));
        add.accept(new ItemStack(ModItems.SAT_DESIGNATOR.get()));
        add.accept(new ItemStack(ModItems.SAT_RELAY.get()));

        // --- CHUNK 3: Items 201-213 (worker_m1_missile_c3) ---
        // 201-204: Kits and Loot
        add.accept(new ItemStack(ModItems.MISSILE_KIT.get()));
        add.accept(new ItemStack(ModItems.LOOT_10.get()));
        add.accept(new ItemStack(ModItems.LOOT_15.get()));
        add.accept(new ItemStack(ModItems.LOOT_MISC.get()));

        // 205-213: Custom Missiles (1.7.10 MissileTab.displayAllReleventItems)
        MissileTab.appendExtraItems(add);

        // Modernized extras (not present in canonical 1.7.10 tabMissile)
        add.accept(new ItemStack(ModItems.MOBILE_LAUNCH_PAD.get()));
        add.accept(new ItemStack(ModItems.TOPOL_LAUNCH_PAD.get()));
        add.accept(new ItemStack(ModItems.MISSILE_9M723.get()));
        add.accept(new ItemStack(ModItems.MISSILE_9M723_BUSTER.get()));
        add.accept(new ItemStack(ModItems.MISSILE_TOPOL.get()));
        add.accept(new ItemStack(ModItems.UPGRADE_RADIUS.get()));
        add.accept(new ItemStack(ModItems.UPGRADE_HEALTH.get()));
        add.accept(new ItemStack(ModItems.ORBITAL_ASSEMBLY_CRYSTAL_CIRCUIT.get()));
        add.accept(new ItemStack(ModItems.CIRCUIT_CRYSTAL.get()));
    }

    /** populateWeaponsTab: порядок из оригинального 1.7.10 (ModBlocks/ModItems, вкладка weapon); отсутствующие в порте предметы пропущены. */
    public static void populateWeaponsTab(BiConsumer<ItemStack, CreativeModeTab.TabVisibility> acceptor) {
        Consumer<ItemStack> add = stack -> acceptor.accept(stack, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);

        // --- CHUNK 1: Items 1-100 (worker_m4_c1) ---
        // Batch 1: Items 1-10 (Turrets)
        add.accept(new ItemStack(ModBlocks.TURRET_CHEKHOV.get()));
        add.accept(new ItemStack(ModBlocks.TURRET_FRIENDLY.get()));
        add.accept(new ItemStack(ModBlocks.TURRET_JEREMY.get()));
        add.accept(new ItemStack(ModBlocks.TURRET_TAUON.get()));
        add.accept(new ItemStack(ModBlocks.TURRET_RICHARD.get()));
        add.accept(new ItemStack(ModBlocks.TURRET_HOWARD.get()));
        add.accept(new ItemStack(ModBlocks.TURRET_HOWARD_DAMAGED.get()));
        add.accept(new ItemStack(ModBlocks.TURRET_MAXWELL.get()));
        add.accept(new ItemStack(ModBlocks.TURRET_FRITZ.get()));
        add.accept(new ItemStack(ModBlocks.TURRET_ARTY.get()));

        // Batch 2: Items 11-20 (Turrets, Chip, Pistol, Crucible Sword, Guns)
        add.accept(new ItemStack(ModBlocks.TURRET_HIMARS.get()));
        add.accept(new ItemStack(ModBlocks.TURRET_SENTRY.get()));
        add.accept(new ItemStack(ModBlocks.TURRET_SENTRY_DAMAGED.get()));
        add.accept(new ItemStack(ModItems.TURRET_CHIP.get()));
        add.accept(new ItemStack(ModItems.GUN_B92.get()));
        add.accept(new ItemStack(ModItems.CRUCIBLE_SWORD.get()));
        add.accept(new ItemStack(WeaponItems.gun("gun_pepperbox")));
        add.accept(new ItemStack(WeaponItems.gun("gun_light_revolver")));
        add.accept(new ItemStack(WeaponItems.gun("gun_henry")));
        add.accept(new ItemStack(WeaponItems.gun("gun_greasegun")));

        // Batch 3: Items 21-30 (Guns)
        add.accept(new ItemStack(WeaponItems.gun("gun_maresleg")));
        add.accept(new ItemStack(WeaponItems.gun("gun_flaregun")));
        add.accept(new ItemStack(WeaponItems.gun("gun_heavy_revolver")));
        add.accept(new ItemStack(WeaponItems.gun("gun_carbine")));
        add.accept(new ItemStack(WeaponItems.gun("gun_am180")));
        add.accept(new ItemStack(WeaponItems.gun("gun_liberator")));
        add.accept(new ItemStack(WeaponItems.gun("gun_congolake")));
        add.accept(new ItemStack(WeaponItems.gun("gun_flamer")));
        add.accept(new ItemStack(WeaponItems.gun("gun_uzi")));
        add.accept(new ItemStack(WeaponItems.gun("gun_spas12")));

        // Batch 4: Items 31-40 (Guns)
        add.accept(new ItemStack(WeaponItems.gun("gun_panzerschreck")));
        add.accept(new ItemStack(WeaponItems.gun("gun_star_f")));
        add.accept(new ItemStack(WeaponItems.gun("gun_g3")));
        add.accept(new ItemStack(WeaponItems.gun("gun_mk108")));
        add.accept(new ItemStack(WeaponItems.gun("gun_chemthrower")));
        add.accept(new ItemStack(WeaponItems.gun("gun_amat")));
        add.accept(new ItemStack(WeaponItems.gun("gun_m2")));
        add.accept(new ItemStack(WeaponItems.gun("gun_autoshotgun")));
        add.accept(new ItemStack(WeaponItems.gun("gun_quadro")));
        add.accept(new ItemStack(WeaponItems.gun("gun_lag")));

        // Batch 5: Items 41-50 (Guns)
        add.accept(new ItemStack(WeaponItems.gun("gun_minigun")));
        add.accept(new ItemStack(WeaponItems.gun("gun_missile_launcher")));
        add.accept(new ItemStack(WeaponItems.gun("gun_tesla_cannon")));
        add.accept(new ItemStack(WeaponItems.gun("gun_laser_pistol")));
        add.accept(new ItemStack(WeaponItems.gun("gun_stg77")));
        add.accept(new ItemStack(WeaponItems.gun("gun_tau")));
        add.accept(new ItemStack(WeaponItems.gun("gun_fatman")));
        add.accept(new ItemStack(WeaponItems.gun("gun_lasrifle")));
        add.accept(new ItemStack(WeaponItems.gun("gun_stinger")));
        add.accept(new ItemStack(WeaponItems.gun("gun_fliegerfaust")));

        // Batch 6: Items 51-60 (Guns and Standard Ammo)
        add.accept(new ItemStack(WeaponItems.gun("gun_coilgun")));
        add.accept(new ItemStack(WeaponItems.gun("gun_bolter")));
        add.accept(new ItemStack(WeaponItems.gun("gun_double_barrel")));
        add.accept(new ItemStack(WeaponItems.gun("gun_n_i_4_n_i")));
        add.accept(new ItemStack(WeaponItems.gun("gun_fireext")));
        add.accept(new ItemStack(WeaponItems.gun("gun_charge_thrower")));
        add.accept(new ItemStack(WeaponItems.gun("gun_drill")));
        add.accept(new ItemStack(WeaponItems.gun("gun_pa_melee")));
        add.accept(new ItemStack(WeaponItems.gun("gun_pa_ranged")));
        add.accept(new ItemStack(WeaponItems.ammo(EnumAmmo.STONE)));

        // Batch 7: Items 61-70 (Standard Ammo: Stone, .357, .44)
        add.accept(new ItemStack(WeaponItems.ammo(EnumAmmo.STONE_AP)));
        add.accept(new ItemStack(WeaponItems.ammo(EnumAmmo.STONE_IRON)));
        add.accept(new ItemStack(WeaponItems.ammo(EnumAmmo.STONE_SHOT)));
        add.accept(new ItemStack(WeaponItems.ammo(EnumAmmo.M357_BP)));
        add.accept(new ItemStack(WeaponItems.ammo(EnumAmmo.M357_SP)));
        add.accept(new ItemStack(WeaponItems.ammo(EnumAmmo.M357_FMJ)));
        add.accept(new ItemStack(WeaponItems.ammo(EnumAmmo.M357_JHP)));
        add.accept(new ItemStack(WeaponItems.ammo(EnumAmmo.M357_AP)));
        add.accept(new ItemStack(WeaponItems.ammo(EnumAmmo.M357_EXPRESS)));
        add.accept(new ItemStack(WeaponItems.ammo(EnumAmmo.M44_BP)));

        // Batch 8: Items 71-80 (Standard Ammo: .44, .22 LR, 9mm)
        add.accept(new ItemStack(WeaponItems.ammo(EnumAmmo.M44_SP)));
        add.accept(new ItemStack(WeaponItems.ammo(EnumAmmo.M44_FMJ)));
        add.accept(new ItemStack(WeaponItems.ammo(EnumAmmo.M44_JHP)));
        add.accept(new ItemStack(WeaponItems.ammo(EnumAmmo.M44_AP)));
        add.accept(new ItemStack(WeaponItems.ammo(EnumAmmo.M44_EXPRESS)));
        add.accept(new ItemStack(WeaponItems.ammo(EnumAmmo.P22_SP)));
        add.accept(new ItemStack(WeaponItems.ammo(EnumAmmo.P22_FMJ)));
        add.accept(new ItemStack(WeaponItems.ammo(EnumAmmo.P22_JHP)));
        add.accept(new ItemStack(WeaponItems.ammo(EnumAmmo.P22_AP)));
        add.accept(new ItemStack(WeaponItems.ammo(EnumAmmo.P9_SP)));

        // Batch 9: Items 81-90 (Standard Ammo: 9mm, .45, 5.56mm)
        add.accept(new ItemStack(WeaponItems.ammo(EnumAmmo.P9_FMJ)));
        add.accept(new ItemStack(WeaponItems.ammo(EnumAmmo.P9_JHP)));
        add.accept(new ItemStack(WeaponItems.ammo(EnumAmmo.P9_AP)));
        add.accept(new ItemStack(WeaponItems.ammo(EnumAmmo.P45_SP)));
        add.accept(new ItemStack(WeaponItems.ammo(EnumAmmo.P45_FMJ)));
        add.accept(new ItemStack(WeaponItems.ammo(EnumAmmo.P45_JHP)));
        add.accept(new ItemStack(WeaponItems.ammo(EnumAmmo.P45_AP)));
        add.accept(new ItemStack(WeaponItems.ammo(EnumAmmo.P45_DU)));
        add.accept(new ItemStack(WeaponItems.ammo(EnumAmmo.R556_SP)));
        add.accept(new ItemStack(WeaponItems.ammo(EnumAmmo.R556_FMJ)));

        // Batch 10: Items 91-100 (Standard Ammo: 5.56mm, 7.62mm, .50 BMG)
        add.accept(new ItemStack(WeaponItems.ammo(EnumAmmo.R556_JHP)));
        add.accept(new ItemStack(WeaponItems.ammo(EnumAmmo.R556_AP)));
        add.accept(new ItemStack(WeaponItems.ammo(EnumAmmo.R762_SP)));
        add.accept(new ItemStack(WeaponItems.ammo(EnumAmmo.R762_FMJ)));
        add.accept(new ItemStack(WeaponItems.ammo(EnumAmmo.R762_JHP)));
        add.accept(new ItemStack(WeaponItems.ammo(EnumAmmo.R762_AP)));
        add.accept(new ItemStack(WeaponItems.ammo(EnumAmmo.R762_DU)));
        add.accept(new ItemStack(WeaponItems.ammo(EnumAmmo.R762_HE)));
        add.accept(new ItemStack(WeaponItems.ammo(EnumAmmo.BMG50_SP)));
        add.accept(new ItemStack(WeaponItems.ammo(EnumAmmo.BMG50_FMJ)));

        // --- CHUNK 2: Items 101-200 (worker_m4_c2) ---
        // Batch 11: Items 101-110 (Standard Ammo: .50 BMG, .75 Bolt, 12 Gauge BP)
        add.accept(new ItemStack(WeaponItems.ammo(EnumAmmo.BMG50_JHP)));
        add.accept(new ItemStack(WeaponItems.ammo(EnumAmmo.BMG50_AP)));
        add.accept(new ItemStack(WeaponItems.ammo(EnumAmmo.BMG50_DU)));
        add.accept(new ItemStack(WeaponItems.ammo(EnumAmmo.BMG50_SM)));
        add.accept(new ItemStack(WeaponItems.ammo(EnumAmmo.BMG50_HE)));
        add.accept(new ItemStack(WeaponItems.ammo(EnumAmmo.B75)));
        add.accept(new ItemStack(WeaponItems.ammo(EnumAmmo.B75_INC)));
        add.accept(new ItemStack(WeaponItems.ammo(EnumAmmo.B75_EXP)));
        add.accept(new ItemStack(WeaponItems.ammo(EnumAmmo.G12_BP)));
        add.accept(new ItemStack(WeaponItems.ammo(EnumAmmo.G12_BP_MAGNUM)));

        // Batch 12: Items 111-120 (Standard Ammo: 12 Gauge, 10 Gauge)
        add.accept(new ItemStack(WeaponItems.ammo(EnumAmmo.G12_BP_SLUG)));
        add.accept(new ItemStack(WeaponItems.ammo(EnumAmmo.G12)));
        add.accept(new ItemStack(WeaponItems.ammo(EnumAmmo.G12_SLUG)));
        add.accept(new ItemStack(WeaponItems.ammo(EnumAmmo.G12_FLECHETTE)));
        add.accept(new ItemStack(WeaponItems.ammo(EnumAmmo.G12_MAGNUM)));
        add.accept(new ItemStack(WeaponItems.ammo(EnumAmmo.G12_EXPLOSIVE)));
        add.accept(new ItemStack(WeaponItems.ammo(EnumAmmo.G12_PHOSPHORUS)));
        add.accept(new ItemStack(WeaponItems.ammo(EnumAmmo.G10)));
        add.accept(new ItemStack(WeaponItems.ammo(EnumAmmo.G10_SHRAPNEL)));
        add.accept(new ItemStack(WeaponItems.ammo(EnumAmmo.G10_DU)));

        // Batch 13: Items 121-130 (Standard Ammo: 10 Gauge, 26mm Flare, 40mm Grenade)
        add.accept(new ItemStack(WeaponItems.ammo(EnumAmmo.G10_SLUG)));
        add.accept(new ItemStack(WeaponItems.ammo(EnumAmmo.G10_EXPLOSIVE)));
        add.accept(new ItemStack(WeaponItems.ammo(EnumAmmo.G26_FLARE)));
        add.accept(new ItemStack(WeaponItems.ammo(EnumAmmo.G26_FLARE_SUPPLY)));
        add.accept(new ItemStack(WeaponItems.ammo(EnumAmmo.G26_FLARE_WEAPON)));
        add.accept(new ItemStack(WeaponItems.ammo(EnumAmmo.G40_HE)));
        add.accept(new ItemStack(WeaponItems.ammo(EnumAmmo.G40_HEAT)));
        add.accept(new ItemStack(WeaponItems.ammo(EnumAmmo.G40_DEMO)));
        add.accept(new ItemStack(WeaponItems.ammo(EnumAmmo.G40_INC)));
        add.accept(new ItemStack(WeaponItems.ammo(EnumAmmo.G40_PHOSPHORUS)));

        // Batch 14: Items 131-140 (Standard Ammo: Micro Rocket, Rocket, Flamer Fuel)
        add.accept(new ItemStack(WeaponItems.ammo(EnumAmmo.ROCKET_MICRO)));
        add.accept(new ItemStack(WeaponItems.ammo(EnumAmmo.ROCKET_HE)));
        add.accept(new ItemStack(WeaponItems.ammo(EnumAmmo.ROCKET_HEAT)));
        add.accept(new ItemStack(WeaponItems.ammo(EnumAmmo.ROCKET_DEMO)));
        add.accept(new ItemStack(WeaponItems.ammo(EnumAmmo.ROCKET_INC)));
        add.accept(new ItemStack(WeaponItems.ammo(EnumAmmo.ROCKET_PHOSPHORUS)));
        add.accept(new ItemStack(WeaponItems.ammo(EnumAmmo.FLAME_DIESEL)));
        add.accept(new ItemStack(WeaponItems.ammo(EnumAmmo.FLAME_GAS)));
        add.accept(new ItemStack(WeaponItems.ammo(EnumAmmo.FLAME_NAPALM)));
        add.accept(new ItemStack(WeaponItems.ammo(EnumAmmo.FLAME_BALEFIRE)));

        // Batch 15: Items 141-150 (Standard Ammo: Capacitor, Tau Uranium, Coilgun, Mini Nuke)
        add.accept(new ItemStack(WeaponItems.ammo(EnumAmmo.CAPACITOR)));
        add.accept(new ItemStack(WeaponItems.ammo(EnumAmmo.CAPACITOR_OVERCHARGE)));
        add.accept(new ItemStack(WeaponItems.ammo(EnumAmmo.CAPACITOR_IR)));
        add.accept(new ItemStack(WeaponItems.ammo(EnumAmmo.TAU_URANIUM)));
        add.accept(new ItemStack(WeaponItems.ammo(EnumAmmo.COIL_TUNGSTEN)));
        add.accept(new ItemStack(WeaponItems.ammo(EnumAmmo.COIL_FERROURANIUM)));
        add.accept(new ItemStack(WeaponItems.ammo(EnumAmmo.NUKE_STANDARD)));
        add.accept(new ItemStack(WeaponItems.ammo(EnumAmmo.NUKE_DEMO)));
        add.accept(new ItemStack(WeaponItems.ammo(EnumAmmo.NUKE_HIGH)));
        add.accept(new ItemStack(WeaponItems.ammo(EnumAmmo.NUKE_TOTS)));

        // Batch 16: Items 151-160 (Standard Ammo: Mini Nuke, Mortar & Generic Weapon Mods: Iron, Steel, HSS)
        add.accept(new ItemStack(WeaponItems.ammo(EnumAmmo.NUKE_HIVE)));
        add.accept(new ItemStack(WeaponItems.ammo(EnumAmmo.NUKE_BALEFIRE)));
        add.accept(new ItemStack(WeaponItems.ammo(EnumAmmo.CT_HOOK)));
        add.accept(new ItemStack(WeaponItems.ammo(EnumAmmo.CT_MORTAR)));
        add.accept(new ItemStack(WeaponItems.ammo(EnumAmmo.CT_MORTAR_CHARGE)));
        add.accept(new ItemStack(WeaponItems.modGeneric(EnumModGeneric.IRON_DAMAGE)));
        add.accept(new ItemStack(WeaponItems.modGeneric(EnumModGeneric.IRON_DURA)));
        add.accept(new ItemStack(WeaponItems.modGeneric(EnumModGeneric.STEEL_DAMAGE)));
        add.accept(new ItemStack(WeaponItems.modGeneric(EnumModGeneric.STEEL_DURA)));
        add.accept(new ItemStack(WeaponItems.modGeneric(EnumModGeneric.DURA_DAMAGE)));

        // Batch 17: Items 161-170 (Generic Weapon Mods: HSS, Desh, Weapon Steel, Ferrouranium, Resistant Alloy, Saturnite)
        add.accept(new ItemStack(WeaponItems.modGeneric(EnumModGeneric.DURA_DURA)));
        add.accept(new ItemStack(WeaponItems.modGeneric(EnumModGeneric.DESH_DAMAGE)));
        add.accept(new ItemStack(WeaponItems.modGeneric(EnumModGeneric.DESH_DURA)));
        add.accept(new ItemStack(WeaponItems.modGeneric(EnumModGeneric.WSTEEL_DAMAGE)));
        add.accept(new ItemStack(WeaponItems.modGeneric(EnumModGeneric.WSTEEL_DURA)));
        add.accept(new ItemStack(WeaponItems.modGeneric(EnumModGeneric.FERRO_DAMAGE)));
        add.accept(new ItemStack(WeaponItems.modGeneric(EnumModGeneric.FERRO_DURA)));
        add.accept(new ItemStack(WeaponItems.modGeneric(EnumModGeneric.TCALLOY_DAMAGE)));
        add.accept(new ItemStack(WeaponItems.modGeneric(EnumModGeneric.TCALLOY_DURA)));
        add.accept(new ItemStack(WeaponItems.modGeneric(EnumModGeneric.BIGMT_DAMAGE)));

        // Batch 18: Items 171-180 (Generic Weapon Mods: Saturnite, Bronze & Special Weapon Mods: Silencer, Scope, Hacksaw, Grease Gun, Gear Train, Aux Engine, Choke)
        add.accept(new ItemStack(WeaponItems.modGeneric(EnumModGeneric.BIGMT_DURA)));
        add.accept(new ItemStack(WeaponItems.modGeneric(EnumModGeneric.BRONZE_DAMAGE)));
        add.accept(new ItemStack(WeaponItems.modGeneric(EnumModGeneric.BRONZE_DURA)));
        add.accept(new ItemStack(WeaponItems.modSpecial(EnumModSpecial.SILENCER)));
        add.accept(new ItemStack(WeaponItems.modSpecial(EnumModSpecial.SCOPE)));
        add.accept(new ItemStack(WeaponItems.modSpecial(EnumModSpecial.SAW)));
        add.accept(new ItemStack(WeaponItems.modSpecial(EnumModSpecial.GREASEGUN)));
        add.accept(new ItemStack(WeaponItems.modSpecial(EnumModSpecial.SLOWDOWN)));
        add.accept(new ItemStack(WeaponItems.modSpecial(EnumModSpecial.SPEEDUP)));
        add.accept(new ItemStack(WeaponItems.modSpecial(EnumModSpecial.CHOKE)));

        // Batch 19: Items 181-190 (Special Weapon Mods: Speedloader, Furniture, Bayonet, Double-Stack Mag, Saturnite Skin, Laser Rifle, Nickels)
        add.accept(new ItemStack(WeaponItems.modSpecial(EnumModSpecial.SPEEDLOADER)));
        add.accept(new ItemStack(WeaponItems.modSpecial(EnumModSpecial.FURNITURE_GREEN)));
        add.accept(new ItemStack(WeaponItems.modSpecial(EnumModSpecial.FURNITURE_BLACK)));
        add.accept(new ItemStack(WeaponItems.modSpecial(EnumModSpecial.BAYONET)));
        add.accept(new ItemStack(WeaponItems.modSpecial(EnumModSpecial.STACK_MAG)));
        add.accept(new ItemStack(WeaponItems.modSpecial(EnumModSpecial.SKIN_SATURNITE)));
        add.accept(new ItemStack(WeaponItems.modSpecial(EnumModSpecial.LAS_SHOTGUN)));
        add.accept(new ItemStack(WeaponItems.modSpecial(EnumModSpecial.LAS_CAPACITOR)));
        add.accept(new ItemStack(WeaponItems.modSpecial(EnumModSpecial.LAS_AUTO)));
        add.accept(new ItemStack(WeaponItems.modSpecial(EnumModSpecial.NICKEL)));

        // Batch 20: Items 191-200 (Special Weapon Mods: Doubloons, Drill Bits, Drill Engines, Magnet)
        add.accept(new ItemStack(WeaponItems.modSpecial(EnumModSpecial.DOUBLOONS)));
        add.accept(new ItemStack(WeaponItems.modSpecial(EnumModSpecial.DRILL_HSS)));
        add.accept(new ItemStack(WeaponItems.modSpecial(EnumModSpecial.DRILL_WEAPONSTEEL)));
        add.accept(new ItemStack(WeaponItems.modSpecial(EnumModSpecial.DRILL_TCALLOY)));
        add.accept(new ItemStack(WeaponItems.modSpecial(EnumModSpecial.DRILL_SATURNITE)));
        add.accept(new ItemStack(WeaponItems.modSpecial(EnumModSpecial.ENGINE_DIESEL)));
        add.accept(new ItemStack(WeaponItems.modSpecial(EnumModSpecial.ENGINE_AVIATION)));
        add.accept(new ItemStack(WeaponItems.modSpecial(EnumModSpecial.ENGINE_ELECTRIC)));
        add.accept(new ItemStack(WeaponItems.modSpecial(EnumModSpecial.ENGINE_TURBO)));
        add.accept(new ItemStack(WeaponItems.modSpecial(EnumModSpecial.MAGNET)));

        // --- CHUNK 3: Items 201-300 (worker_m4_c3) ---
        // Batch 21: Items 201-210 (Special Weapon Mods & Caliber Conversion Kits)
        add.accept(new ItemStack(WeaponItems.modSpecial(EnumModSpecial.SIFTER)));
        add.accept(new ItemStack(WeaponItems.modSpecial(EnumModSpecial.CANISTERS)));
        add.accept(new ItemStack(WeaponItems.modCaliber(EnumModCaliber.P9)));
        add.accept(new ItemStack(WeaponItems.modCaliber(EnumModCaliber.P45)));
        add.accept(new ItemStack(WeaponItems.modCaliber(EnumModCaliber.P22)));
        add.accept(new ItemStack(WeaponItems.modCaliber(EnumModCaliber.M357)));
        add.accept(new ItemStack(WeaponItems.modCaliber(EnumModCaliber.M44)));
        add.accept(new ItemStack(WeaponItems.modCaliber(EnumModCaliber.R556)));
        add.accept(new ItemStack(WeaponItems.modCaliber(EnumModCaliber.R762)));
        add.accept(new ItemStack(WeaponItems.modCaliber(EnumModCaliber.BMG50)));

        // Batch 22: Items 211-220 (B92 Energy Cell, Fire Extinguisher Tanks, 240mm Shells, Goalkeeper Belt)
        add.accept(new ItemStack(ModItems.GUN_B92_AMMO.get()));
        add.accept(new ItemStack(ModItems.AMMO_FIREEXT.get()));
        add.accept(new ItemStack(ModItems.AMMO_FIREEXT_FOAM.get()));
        add.accept(new ItemStack(ModItems.AMMO_FIREEXT_SAND.get()));
        add.accept(new ItemStack(ModItems.AMMO_SHELL.get()));
        add.accept(new ItemStack(ModItems.AMMO_SHELL_EXPLOSIVE.get()));
        add.accept(new ItemStack(ModItems.AMMO_SHELL_APFSDS_T.get()));
        add.accept(new ItemStack(ModItems.AMMO_SHELL_APFSDS_DU.get()));
        add.accept(new ItemStack(ModItems.AMMO_SHELL_W9.get()));
        add.accept(new ItemStack(ModItems.AMMO_DGK.get()));

        // Batch 23: Items 221-230 (16" Artillery Shells Part 1)
        add.accept(new ItemStack(ModItems.AMMO_ARTY.get()));
        add.accept(new ItemStack(ModItems.AMMO_ARTY_CLASSIC.get()));
        add.accept(new ItemStack(ModItems.AMMO_ARTY_HE.get()));
        add.accept(new ItemStack(ModItems.AMMO_ARTY_PHOSPHORUS.get()));
        add.accept(new ItemStack(ModItems.AMMO_ARTY_PHOSPHORUS_MULTI.get()));
        add.accept(new ItemStack(ModItems.AMMO_ARTY_MINI_NUKE.get()));
        add.accept(new ItemStack(ModItems.AMMO_ARTY_MINI_NUKE_MULTI.get()));
        add.accept(new ItemStack(ModItems.AMMO_ARTY_NUKE.get()));
        add.accept(new ItemStack(ModItems.AMMO_ARTY_CARGO.get()));
        add.accept(new ItemStack(ModItems.AMMO_ARTY_CHLORINE.get()));

        // Batch 24: Items 231-240 (16" Artillery Shells Part 2 & HIMARS Artillery Rocket Pods)
        add.accept(new ItemStack(ModItems.AMMO_ARTY_PHOSGENE.get()));
        add.accept(new ItemStack(ModItems.AMMO_ARTY_MUSTARD_GAS.get()));
        add.accept(new ItemStack(ModItems.ROCKET_HIMARS_STANDARD.get()));
        add.accept(new ItemStack(ModItems.ROCKET_HIMARS_HE.get()));
        add.accept(new ItemStack(ModItems.ROCKET_HIMARS_WP.get()));
        add.accept(new ItemStack(ModItems.ROCKET_HIMARS_THERMOBARIC.get()));
        add.accept(new ItemStack(ModItems.ROCKET_HIMARS_LAVA.get()));
        add.accept(new ItemStack(ModItems.ROCKET_HIMARS_MINI_NUKE.get()));
        add.accept(new ItemStack(ModItems.ROCKET_HIMARS_SINGLE.get()));
        add.accept(new ItemStack(ModItems.ROCKET_HIMARS_SINGLE_TB.get()));

        // Batch 25: Items 241-250 (Ammo Containers, Explosive Sticks, Grenade Shells Part 1)
        add.accept(new ItemStack(ModItems.AMMO_CONTAINER.get()));
        add.accept(new ItemStack(ModItems.AMMO_CONTAINER_1.get()));
        add.accept(new ItemStack(ModItems.STICK_DYNAMITE.get()));
        add.accept(new ItemStack(ModItems.STICK_DYNAMITE_FISHING.get()));
        add.accept(new ItemStack(ModItems.STICK_TNT.get()));
        add.accept(new ItemStack(ModItems.STICK_SEMTEX.get()));
        add.accept(new ItemStack(ModItems.STICK_C4.get()));
        add.accept(new ItemStack(GrenadeItems.shell(EnumGrenadeShell.FRAG)));
        add.accept(new ItemStack(GrenadeItems.shell(EnumGrenadeShell.STICK)));
        add.accept(new ItemStack(GrenadeItems.shell(EnumGrenadeShell.TECH)));

        // Batch 26: Items 251-260 (Grenade Shell Part 2 & Grenade Fillings Part 1)
        add.accept(new ItemStack(GrenadeItems.shell(EnumGrenadeShell.NUKE)));
        add.accept(new ItemStack(GrenadeItems.filling(EnumGrenadeFilling.POWDER)));
        add.accept(new ItemStack(GrenadeItems.filling(EnumGrenadeFilling.HE)));
        add.accept(new ItemStack(GrenadeItems.filling(EnumGrenadeFilling.DEMO)));
        add.accept(new ItemStack(GrenadeItems.filling(EnumGrenadeFilling.INC)));
        add.accept(new ItemStack(GrenadeItems.filling(EnumGrenadeFilling.WP)));
        add.accept(new ItemStack(GrenadeItems.filling(EnumGrenadeFilling.CLUSTER)));
        add.accept(new ItemStack(GrenadeItems.filling(EnumGrenadeFilling.EMP)));
        add.accept(new ItemStack(GrenadeItems.filling(EnumGrenadeFilling.PLASMA)));
        add.accept(new ItemStack(GrenadeItems.filling(EnumGrenadeFilling.LASER)));

        // Batch 27: Items 261-270 (Grenade Fillings Part 2, Grenade Fuzes, Grenade Extra Part 1)
        add.accept(new ItemStack(GrenadeItems.filling(EnumGrenadeFilling.CLUSTER_HEAVY)));
        add.accept(new ItemStack(GrenadeItems.filling(EnumGrenadeFilling.NUCLEAR)));
        add.accept(new ItemStack(GrenadeItems.filling(EnumGrenadeFilling.NUCLEAR_DEMO)));
        add.accept(new ItemStack(GrenadeItems.filling(EnumGrenadeFilling.SCHRAB)));
        add.accept(new ItemStack(GrenadeItems.fuze(EnumGrenadeFuze.S3)));
        add.accept(new ItemStack(GrenadeItems.fuze(EnumGrenadeFuze.S7)));
        add.accept(new ItemStack(GrenadeItems.fuze(EnumGrenadeFuze.S15)));
        add.accept(new ItemStack(GrenadeItems.fuze(EnumGrenadeFuze.IMPACT)));
        add.accept(new ItemStack(GrenadeItems.fuze(EnumGrenadeFuze.AIRBURST)));
        add.accept(new ItemStack(GrenadeItems.extra(EnumGrenadeExtra.GLUE)));

        // Batch 28: Items 271-280 (Grenade Extras Part 2 & Universal Grenades 1-7)
        add.accept(new ItemStack(GrenadeItems.extra(EnumGrenadeExtra.PROXY_FUZE)));
        add.accept(new ItemStack(GrenadeItems.extra(EnumGrenadeExtra.FRAG_SLEEVE)));
        add.accept(new ItemStack(GrenadeItems.extra(EnumGrenadeExtra.TRIPLEX)));
        java.util.List<ItemStack> universalGrenades = new java.util.ArrayList<>();
        ItemGrenadeUniversal.addSubItems(universalGrenades);
        add.accept(universalGrenades.get(0));
        add.accept(universalGrenades.get(1));
        add.accept(universalGrenades.get(2));
        add.accept(universalGrenades.get(3));
        add.accept(universalGrenades.get(4));
        add.accept(universalGrenades.get(5));
        add.accept(universalGrenades.get(6));

        // Batch 29: Items 281-290 (Universal Grenades 8-17)
        add.accept(universalGrenades.get(7));
        add.accept(universalGrenades.get(8));
        add.accept(universalGrenades.get(9));
        add.accept(universalGrenades.get(10));
        add.accept(universalGrenades.get(11));
        add.accept(universalGrenades.get(12));
        add.accept(universalGrenades.get(13));
        add.accept(universalGrenades.get(14));
        add.accept(universalGrenades.get(15));
        add.accept(universalGrenades.get(16));

        // Batch 30: Items 291-300 (Universal Grenades 18-27)
        add.accept(universalGrenades.get(17));
        add.accept(universalGrenades.get(18));
        add.accept(universalGrenades.get(19));
        add.accept(universalGrenades.get(20));
        add.accept(universalGrenades.get(21));
        add.accept(universalGrenades.get(22));
        add.accept(universalGrenades.get(23));
        add.accept(universalGrenades.get(24));
        add.accept(universalGrenades.get(25));
        add.accept(universalGrenades.get(26));

        // --- CHUNK 4: Items 301-400 (worker_m4_c4) ---
        // Batch 31: Items 301-310 (Universal Grenades 28-37)
        add.accept(universalGrenades.get(27));
        add.accept(universalGrenades.get(28));
        add.accept(universalGrenades.get(29));
        add.accept(universalGrenades.get(30));
        add.accept(universalGrenades.get(31));
        add.accept(universalGrenades.get(32));
        add.accept(universalGrenades.get(33));
        add.accept(universalGrenades.get(34));
        add.accept(universalGrenades.get(35));
        add.accept(universalGrenades.get(36));

        // Batch 32: Items 311-320 (Universal Grenades 38-47)
        add.accept(universalGrenades.get(37));
        add.accept(universalGrenades.get(38));
        add.accept(universalGrenades.get(39));
        add.accept(universalGrenades.get(40));
        add.accept(universalGrenades.get(41));
        add.accept(universalGrenades.get(42));
        add.accept(universalGrenades.get(43));
        add.accept(universalGrenades.get(44));
        add.accept(universalGrenades.get(45));
        add.accept(universalGrenades.get(46));

        // Batch 33: Items 321-330 (Universal Grenades 48-57)
        add.accept(universalGrenades.get(47));
        add.accept(universalGrenades.get(48));
        add.accept(universalGrenades.get(49));
        add.accept(universalGrenades.get(50));
        add.accept(universalGrenades.get(51));
        add.accept(universalGrenades.get(52));
        add.accept(universalGrenades.get(53));
        add.accept(universalGrenades.get(54));
        add.accept(universalGrenades.get(55));
        add.accept(universalGrenades.get(56));

        // Batch 34: Items 331-340 (Universal Grenades 58-67)
        add.accept(universalGrenades.get(57));
        add.accept(universalGrenades.get(58));
        add.accept(universalGrenades.get(59));
        add.accept(universalGrenades.get(60));
        add.accept(universalGrenades.get(61));
        add.accept(universalGrenades.get(62));
        add.accept(universalGrenades.get(63));
        add.accept(universalGrenades.get(64));
        add.accept(universalGrenades.get(65));
        add.accept(universalGrenades.get(66));

        // Batch 35: Items 341-350 (Universal Grenades 68-77)
        add.accept(universalGrenades.get(67));
        add.accept(universalGrenades.get(68));
        add.accept(universalGrenades.get(69));
        add.accept(universalGrenades.get(70));
        add.accept(universalGrenades.get(71));
        add.accept(universalGrenades.get(72));
        add.accept(universalGrenades.get(73));
        add.accept(universalGrenades.get(74));
        add.accept(universalGrenades.get(75));
        add.accept(universalGrenades.get(76));

        // Batch 36: Items 351-360 (Universal Grenades 78-87)
        add.accept(universalGrenades.get(77));
        add.accept(universalGrenades.get(78));
        add.accept(universalGrenades.get(79));
        add.accept(universalGrenades.get(80));
        add.accept(universalGrenades.get(81));
        add.accept(universalGrenades.get(82));
        add.accept(universalGrenades.get(83));
        add.accept(universalGrenades.get(84));
        add.accept(universalGrenades.get(85));
        add.accept(universalGrenades.get(86));

        // Batch 37: Items 361-370 (Universal Grenades 88-97)
        add.accept(universalGrenades.get(87));
        add.accept(universalGrenades.get(88));
        add.accept(universalGrenades.get(89));
        add.accept(universalGrenades.get(90));
        add.accept(universalGrenades.get(91));
        add.accept(universalGrenades.get(92));
        add.accept(universalGrenades.get(93));
        add.accept(universalGrenades.get(94));
        add.accept(universalGrenades.get(95));
        add.accept(universalGrenades.get(96));

        // Batch 38: Items 371-380 (Universal Grenades 98-107)
        add.accept(universalGrenades.get(97));
        add.accept(universalGrenades.get(98));
        add.accept(universalGrenades.get(99));
        add.accept(universalGrenades.get(100));
        add.accept(universalGrenades.get(101));
        add.accept(universalGrenades.get(102));
        add.accept(universalGrenades.get(103));
        add.accept(universalGrenades.get(104));
        add.accept(universalGrenades.get(105));
        add.accept(universalGrenades.get(106));

        // Batch 39: Items 381-390 (Universal Grenades 108-117)
        add.accept(universalGrenades.get(107));
        add.accept(universalGrenades.get(108));
        add.accept(universalGrenades.get(109));
        add.accept(universalGrenades.get(110));
        add.accept(universalGrenades.get(111));
        add.accept(universalGrenades.get(112));
        add.accept(universalGrenades.get(113));
        add.accept(universalGrenades.get(114));
        add.accept(universalGrenades.get(115));
        add.accept(universalGrenades.get(116));

        // Batch 40: Items 391-400 (Universal Grenades 118-127)
        add.accept(universalGrenades.get(117));
        add.accept(universalGrenades.get(118));
        add.accept(universalGrenades.get(119));
        add.accept(universalGrenades.get(120));
        add.accept(universalGrenades.get(121));
        add.accept(universalGrenades.get(122));
        add.accept(universalGrenades.get(123));
        add.accept(universalGrenades.get(124));
        add.accept(universalGrenades.get(125));
        add.accept(universalGrenades.get(126));

        // --- CHUNK 5: Items 401-500 (worker_m4_c5_r2) ---
        // Batch 41: Items 401-410 (Universal Grenades 128-137)
        add.accept(universalGrenades.get(127));
        add.accept(universalGrenades.get(128));
        add.accept(universalGrenades.get(129));
        add.accept(universalGrenades.get(130));
        add.accept(universalGrenades.get(131));
        add.accept(universalGrenades.get(132));
        add.accept(universalGrenades.get(133));
        add.accept(universalGrenades.get(134));
        add.accept(universalGrenades.get(135));
        add.accept(universalGrenades.get(136));

        // Batch 42: Items 411-420 (Universal Grenades 138-147)
        add.accept(universalGrenades.get(137));
        add.accept(universalGrenades.get(138));
        add.accept(universalGrenades.get(139));
        add.accept(universalGrenades.get(140));
        add.accept(universalGrenades.get(141));
        add.accept(universalGrenades.get(142));
        add.accept(universalGrenades.get(143));
        add.accept(universalGrenades.get(144));
        add.accept(universalGrenades.get(145));
        add.accept(universalGrenades.get(146));

        // Batch 43: Items 421-430 (Universal Grenades 148-157)
        add.accept(universalGrenades.get(147));
        add.accept(universalGrenades.get(148));
        add.accept(universalGrenades.get(149));
        add.accept(universalGrenades.get(150));
        add.accept(universalGrenades.get(151));
        add.accept(universalGrenades.get(152));
        add.accept(universalGrenades.get(153));
        add.accept(universalGrenades.get(154));
        add.accept(universalGrenades.get(155));
        add.accept(universalGrenades.get(156));

        // Batch 44: Items 431-440 (Universal Grenades 158-167)
        add.accept(universalGrenades.get(157));
        add.accept(universalGrenades.get(158));
        add.accept(universalGrenades.get(159));
        add.accept(universalGrenades.get(160));
        add.accept(universalGrenades.get(161));
        add.accept(universalGrenades.get(162));
        add.accept(universalGrenades.get(163));
        add.accept(universalGrenades.get(164));
        add.accept(universalGrenades.get(165));
        add.accept(universalGrenades.get(166));

        // Batch 45: Items 441-450 (Universal Grenades 168-177)
        add.accept(universalGrenades.get(167));
        add.accept(universalGrenades.get(168));
        add.accept(universalGrenades.get(169));
        add.accept(universalGrenades.get(170));
        add.accept(universalGrenades.get(171));
        add.accept(universalGrenades.get(172));
        add.accept(universalGrenades.get(173));
        add.accept(universalGrenades.get(174));
        add.accept(universalGrenades.get(175));
        add.accept(universalGrenades.get(176));

        // Batch 46: Items 451-460 (Universal Grenades 178-187)
        add.accept(universalGrenades.get(177));
        add.accept(universalGrenades.get(178));
        add.accept(universalGrenades.get(179));
        add.accept(universalGrenades.get(180));
        add.accept(universalGrenades.get(181));
        add.accept(universalGrenades.get(182));
        add.accept(universalGrenades.get(183));
        add.accept(universalGrenades.get(184));
        add.accept(universalGrenades.get(185));
        add.accept(universalGrenades.get(186));

        // Batch 47: Items 461-470 (Universal Grenades 188-197)
        add.accept(universalGrenades.get(187));
        add.accept(universalGrenades.get(188));
        add.accept(universalGrenades.get(189));
        add.accept(universalGrenades.get(190));
        add.accept(universalGrenades.get(191));
        add.accept(universalGrenades.get(192));
        add.accept(universalGrenades.get(193));
        add.accept(universalGrenades.get(194));
        add.accept(universalGrenades.get(195));
        add.accept(universalGrenades.get(196));

        // Batch 48: Items 471-480 (Universal Grenades 198-207)
        add.accept(universalGrenades.get(197));
        add.accept(universalGrenades.get(198));
        add.accept(universalGrenades.get(199));
        add.accept(universalGrenades.get(200));
        add.accept(universalGrenades.get(201));
        add.accept(universalGrenades.get(202));
        add.accept(universalGrenades.get(203));
        add.accept(universalGrenades.get(204));
        add.accept(universalGrenades.get(205));
        add.accept(universalGrenades.get(206));

        // Batch 49: Items 481-490 (Universal Grenades 208-217)
        add.accept(universalGrenades.get(207));
        add.accept(universalGrenades.get(208));
        add.accept(universalGrenades.get(209));
        add.accept(universalGrenades.get(210));
        add.accept(universalGrenades.get(211));
        add.accept(universalGrenades.get(212));
        add.accept(universalGrenades.get(213));
        add.accept(universalGrenades.get(214));
        add.accept(universalGrenades.get(215));
        add.accept(universalGrenades.get(216));

        // Batch 50: Items 491-500 (Universal Grenades 218-227)
        add.accept(universalGrenades.get(217));
        add.accept(universalGrenades.get(218));
        add.accept(universalGrenades.get(219));
        add.accept(universalGrenades.get(220));
        add.accept(universalGrenades.get(221));
        add.accept(universalGrenades.get(222));
        add.accept(universalGrenades.get(223));
        add.accept(universalGrenades.get(224));
        add.accept(universalGrenades.get(225));
        add.accept(universalGrenades.get(226));

        // --- CHUNK 6: Items 501-600 (worker_m4_c6) ---
        // Batch 51: Items 501-510 (Universal Grenades 228-237)
        add.accept(universalGrenades.get(227));
        add.accept(universalGrenades.get(228));
        add.accept(universalGrenades.get(229));
        add.accept(universalGrenades.get(230));
        add.accept(universalGrenades.get(231));
        add.accept(universalGrenades.get(232));
        add.accept(universalGrenades.get(233));
        add.accept(universalGrenades.get(234));
        add.accept(universalGrenades.get(235));
        add.accept(universalGrenades.get(236));

        // Batch 52: Items 511-520 (Universal Grenades 238-247)
        add.accept(universalGrenades.get(237));
        add.accept(universalGrenades.get(238));
        add.accept(universalGrenades.get(239));
        add.accept(universalGrenades.get(240));
        add.accept(universalGrenades.get(241));
        add.accept(universalGrenades.get(242));
        add.accept(universalGrenades.get(243));
        add.accept(universalGrenades.get(244));
        add.accept(universalGrenades.get(245));
        add.accept(universalGrenades.get(246));

        // Batch 53: Items 521-530 (Universal Grenades 248-257)
        add.accept(universalGrenades.get(247));
        add.accept(universalGrenades.get(248));
        add.accept(universalGrenades.get(249));
        add.accept(universalGrenades.get(250));
        add.accept(universalGrenades.get(251));
        add.accept(universalGrenades.get(252));
        add.accept(universalGrenades.get(253));
        add.accept(universalGrenades.get(254));
        add.accept(universalGrenades.get(255));
        add.accept(universalGrenades.get(256));

        // Batch 54: Items 531-540 (Universal Grenades 258-267)
        add.accept(universalGrenades.get(257));
        add.accept(universalGrenades.get(258));
        add.accept(universalGrenades.get(259));
        add.accept(universalGrenades.get(260));
        add.accept(universalGrenades.get(261));
        add.accept(universalGrenades.get(262));
        add.accept(universalGrenades.get(263));
        add.accept(universalGrenades.get(264));
        add.accept(universalGrenades.get(265));
        add.accept(universalGrenades.get(266));

        // Batch 55: Items 541-550 (Universal Grenades 268-277)
        add.accept(universalGrenades.get(267));
        add.accept(universalGrenades.get(268));
        add.accept(universalGrenades.get(269));
        add.accept(universalGrenades.get(270));
        add.accept(universalGrenades.get(271));
        add.accept(universalGrenades.get(272));
        add.accept(universalGrenades.get(273));
        add.accept(universalGrenades.get(274));
        add.accept(universalGrenades.get(275));
        add.accept(universalGrenades.get(276));

        // Batch 56: Items 551-560 (Universal Grenades 278-287)
        add.accept(universalGrenades.get(277));
        add.accept(universalGrenades.get(278));
        add.accept(universalGrenades.get(279));
        add.accept(universalGrenades.get(280));
        add.accept(universalGrenades.get(281));
        add.accept(universalGrenades.get(282));
        add.accept(universalGrenades.get(283));
        add.accept(universalGrenades.get(284));
        add.accept(universalGrenades.get(285));
        add.accept(universalGrenades.get(286));

        // Batch 57: Items 561-570 (Universal Grenades 288-297)
        add.accept(universalGrenades.get(287));
        add.accept(universalGrenades.get(288));
        add.accept(universalGrenades.get(289));
        add.accept(universalGrenades.get(290));
        add.accept(universalGrenades.get(291));
        add.accept(universalGrenades.get(292));
        add.accept(universalGrenades.get(293));
        add.accept(universalGrenades.get(294));
        add.accept(universalGrenades.get(295));
        add.accept(universalGrenades.get(296));

        // Batch 58: Items 571-580 (Universal Grenades 298-307)
        add.accept(universalGrenades.get(297));
        add.accept(universalGrenades.get(298));
        add.accept(universalGrenades.get(299));
        add.accept(universalGrenades.get(300));
        add.accept(universalGrenades.get(301));
        add.accept(universalGrenades.get(302));
        add.accept(universalGrenades.get(303));
        add.accept(universalGrenades.get(304));
        add.accept(universalGrenades.get(305));
        add.accept(universalGrenades.get(306));

        // Batch 59: Items 581-590 (Universal Grenades 308-317)
        add.accept(universalGrenades.get(307));
        add.accept(universalGrenades.get(308));
        add.accept(universalGrenades.get(309));
        add.accept(universalGrenades.get(310));
        add.accept(universalGrenades.get(311));
        add.accept(universalGrenades.get(312));
        add.accept(universalGrenades.get(313));
        add.accept(universalGrenades.get(314));
        add.accept(universalGrenades.get(315));
        add.accept(universalGrenades.get(316));

        // Batch 60: Items 591-600 (Universal Grenades 318-327)
        add.accept(universalGrenades.get(317));
        add.accept(universalGrenades.get(318));
        add.accept(universalGrenades.get(319));
        add.accept(universalGrenades.get(320));
        add.accept(universalGrenades.get(321));
        add.accept(universalGrenades.get(322));
        add.accept(universalGrenades.get(323));
        add.accept(universalGrenades.get(324));
        add.accept(universalGrenades.get(325));
        add.accept(universalGrenades.get(326));

        // --- CHUNK 7: Items 601-700 (worker_m4_c7) ---
        // Batch 61: Items 601-610 (Universal Grenades 328-337)
        add.accept(universalGrenades.get(327));
        add.accept(universalGrenades.get(328));
        add.accept(universalGrenades.get(329));
        add.accept(universalGrenades.get(330));
        add.accept(universalGrenades.get(331));
        add.accept(universalGrenades.get(332));
        add.accept(universalGrenades.get(333));
        add.accept(universalGrenades.get(334));
        add.accept(universalGrenades.get(335));
        add.accept(universalGrenades.get(336));

        // Batch 62: Items 611-620 (Universal Grenades 338-347)
        add.accept(universalGrenades.get(337));
        add.accept(universalGrenades.get(338));
        add.accept(universalGrenades.get(339));
        add.accept(universalGrenades.get(340));
        add.accept(universalGrenades.get(341));
        add.accept(universalGrenades.get(342));
        add.accept(universalGrenades.get(343));
        add.accept(universalGrenades.get(344));
        add.accept(universalGrenades.get(345));
        add.accept(universalGrenades.get(346));

        // Batch 63: Items 621-630 (Universal Grenades 348-357)
        add.accept(universalGrenades.get(347));
        add.accept(universalGrenades.get(348));
        add.accept(universalGrenades.get(349));
        add.accept(universalGrenades.get(350));
        add.accept(universalGrenades.get(351));
        add.accept(universalGrenades.get(352));
        add.accept(universalGrenades.get(353));
        add.accept(universalGrenades.get(354));
        add.accept(universalGrenades.get(355));
        add.accept(universalGrenades.get(356));

        // Batch 64: Items 631-640 (Universal Grenades 358-367)
        add.accept(universalGrenades.get(357));
        add.accept(universalGrenades.get(358));
        add.accept(universalGrenades.get(359));
        add.accept(universalGrenades.get(360));
        add.accept(universalGrenades.get(361));
        add.accept(universalGrenades.get(362));
        add.accept(universalGrenades.get(363));
        add.accept(universalGrenades.get(364));
        add.accept(universalGrenades.get(365));
        add.accept(universalGrenades.get(366));

        // Batch 65: Items 641-650 (Universal Grenades 368-377)
        add.accept(universalGrenades.get(367));
        add.accept(universalGrenades.get(368));
        add.accept(universalGrenades.get(369));
        add.accept(universalGrenades.get(370));
        add.accept(universalGrenades.get(371));
        add.accept(universalGrenades.get(372));
        add.accept(universalGrenades.get(373));
        add.accept(universalGrenades.get(374));
        add.accept(universalGrenades.get(375));
        add.accept(universalGrenades.get(376));

        // Batch 66: Items 651-660 (Universal Grenades 378-387)
        add.accept(universalGrenades.get(377));
        add.accept(universalGrenades.get(378));
        add.accept(universalGrenades.get(379));
        add.accept(universalGrenades.get(380));
        add.accept(universalGrenades.get(381));
        add.accept(universalGrenades.get(382));
        add.accept(universalGrenades.get(383));
        add.accept(universalGrenades.get(384));
        add.accept(universalGrenades.get(385));
        add.accept(universalGrenades.get(386));

        // Batch 67: Items 661-670 (Universal Grenades 388-397)
        add.accept(universalGrenades.get(387));
        add.accept(universalGrenades.get(388));
        add.accept(universalGrenades.get(389));
        add.accept(universalGrenades.get(390));
        add.accept(universalGrenades.get(391));
        add.accept(universalGrenades.get(392));
        add.accept(universalGrenades.get(393));
        add.accept(universalGrenades.get(394));
        add.accept(universalGrenades.get(395));
        add.accept(universalGrenades.get(396));

        // Batch 68: Items 671-680 (Universal Grenades 398-407)
        add.accept(universalGrenades.get(397));
        add.accept(universalGrenades.get(398));
        add.accept(universalGrenades.get(399));
        add.accept(universalGrenades.get(400));
        add.accept(universalGrenades.get(401));
        add.accept(universalGrenades.get(402));
        add.accept(universalGrenades.get(403));
        add.accept(universalGrenades.get(404));
        add.accept(universalGrenades.get(405));
        add.accept(universalGrenades.get(406));

        // Batch 69: Items 681-690 (Universal Grenades 408-417)
        add.accept(universalGrenades.get(407));
        add.accept(universalGrenades.get(408));
        add.accept(universalGrenades.get(409));
        add.accept(universalGrenades.get(410));
        add.accept(universalGrenades.get(411));
        add.accept(universalGrenades.get(412));
        add.accept(universalGrenades.get(413));
        add.accept(universalGrenades.get(414));
        add.accept(universalGrenades.get(415));
        add.accept(universalGrenades.get(416));

        // Batch 70: Items 691-700 (Universal Grenades 418-427)
        add.accept(universalGrenades.get(417));
        add.accept(universalGrenades.get(418));
        add.accept(universalGrenades.get(419));
        add.accept(universalGrenades.get(420));
        add.accept(universalGrenades.get(421));
        add.accept(universalGrenades.get(422));
        add.accept(universalGrenades.get(423));
        add.accept(universalGrenades.get(424));
        add.accept(universalGrenades.get(425));
        add.accept(universalGrenades.get(426));

        // --- CHUNK 8: Items 701-800 (worker_m4_c8) ---
        // Batch 71: Items 701-710 (Universal Grenades 428-437)
        add.accept(universalGrenades.get(427));
        add.accept(universalGrenades.get(428));
        add.accept(universalGrenades.get(429));
        add.accept(universalGrenades.get(430));
        add.accept(universalGrenades.get(431));
        add.accept(universalGrenades.get(432));
        add.accept(universalGrenades.get(433));
        add.accept(universalGrenades.get(434));
        add.accept(universalGrenades.get(435));
        add.accept(universalGrenades.get(436));

        // Batch 72: Items 711-720 (Universal Grenades 438-447)
        add.accept(universalGrenades.get(437));
        add.accept(universalGrenades.get(438));
        add.accept(universalGrenades.get(439));
        add.accept(universalGrenades.get(440));
        add.accept(universalGrenades.get(441));
        add.accept(universalGrenades.get(442));
        add.accept(universalGrenades.get(443));
        add.accept(universalGrenades.get(444));
        add.accept(universalGrenades.get(445));
        add.accept(universalGrenades.get(446));

        // Batch 73: Items 721-730 (Universal Grenades 448-457)
        add.accept(universalGrenades.get(447));
        add.accept(universalGrenades.get(448));
        add.accept(universalGrenades.get(449));
        add.accept(universalGrenades.get(450));
        add.accept(universalGrenades.get(451));
        add.accept(universalGrenades.get(452));
        add.accept(universalGrenades.get(453));
        add.accept(universalGrenades.get(454));
        add.accept(universalGrenades.get(455));
        add.accept(universalGrenades.get(456));

        // Batch 74: Items 731-740 (Universal Grenades 458-467)
        add.accept(universalGrenades.get(457));
        add.accept(universalGrenades.get(458));
        add.accept(universalGrenades.get(459));
        add.accept(universalGrenades.get(460));
        add.accept(universalGrenades.get(461));
        add.accept(universalGrenades.get(462));
        add.accept(universalGrenades.get(463));
        add.accept(universalGrenades.get(464));
        add.accept(universalGrenades.get(465));
        add.accept(universalGrenades.get(466));

        java.util.List<ItemStack> disperserCanisters = ((com.hbm_m.item.liquids.ItemFluidTank) ModItems.DISPERSER_CANISTER.get()).getSubItems();

        // Batch 75: Items 741-750 (Universal Grenades 468-475, Disperser Canister Empty, Disperser Canister 1)
        add.accept(universalGrenades.get(467));
        add.accept(universalGrenades.get(468));
        add.accept(universalGrenades.get(469));
        add.accept(universalGrenades.get(470));
        add.accept(universalGrenades.get(471));
        add.accept(universalGrenades.get(472));
        add.accept(universalGrenades.get(473));
        add.accept(universalGrenades.get(474));
        add.accept(new ItemStack(ModItems.DISPERSER_CANISTER_EMPTY.get()));
        add.accept(disperserCanisters.get(0));

        // Batch 76: Items 751-760 (Disperser Canisters 2-11)
        add.accept(disperserCanisters.get(1));
        add.accept(disperserCanisters.get(2));
        add.accept(disperserCanisters.get(3));
        add.accept(disperserCanisters.get(4));
        add.accept(disperserCanisters.get(5));
        add.accept(disperserCanisters.get(6));
        add.accept(disperserCanisters.get(7));
        add.accept(disperserCanisters.get(8));
        add.accept(disperserCanisters.get(9));
        add.accept(disperserCanisters.get(10));

        // Batch 77: Items 761-770 (Disperser Canisters 12-21)
        add.accept(disperserCanisters.get(11));
        add.accept(disperserCanisters.get(12));
        add.accept(disperserCanisters.get(13));
        add.accept(disperserCanisters.get(14));
        add.accept(disperserCanisters.get(15));
        add.accept(disperserCanisters.get(16));
        add.accept(disperserCanisters.get(17));
        add.accept(disperserCanisters.get(18));
        add.accept(disperserCanisters.get(19));
        add.accept(disperserCanisters.get(20));

        // Batch 78: Items 771-780 (Disperser Canisters 22-31)
        add.accept(disperserCanisters.get(21));
        add.accept(disperserCanisters.get(22));
        add.accept(disperserCanisters.get(23));
        add.accept(disperserCanisters.get(24));
        add.accept(disperserCanisters.get(25));
        add.accept(disperserCanisters.get(26));
        add.accept(disperserCanisters.get(27));
        add.accept(disperserCanisters.get(28));
        add.accept(disperserCanisters.get(29));
        add.accept(disperserCanisters.get(30));

        // Batch 79: Items 781-790 (Disperser Canisters 32-41)
        add.accept(disperserCanisters.get(31));
        add.accept(disperserCanisters.get(32));
        add.accept(disperserCanisters.get(33));
        add.accept(disperserCanisters.get(34));
        add.accept(disperserCanisters.get(35));
        add.accept(disperserCanisters.get(36));
        add.accept(disperserCanisters.get(37));
        add.accept(disperserCanisters.get(38));
        add.accept(disperserCanisters.get(39));
        add.accept(disperserCanisters.get(40));

        // Batch 80: Items 791-800 (Disperser Canisters 42-51)
        add.accept(disperserCanisters.get(41));
        add.accept(disperserCanisters.get(42));
        add.accept(disperserCanisters.get(43));
        add.accept(disperserCanisters.get(44));
        add.accept(disperserCanisters.get(45));
        add.accept(disperserCanisters.get(46));
        add.accept(disperserCanisters.get(47));
        add.accept(disperserCanisters.get(48));
        add.accept(disperserCanisters.get(49));
        add.accept(disperserCanisters.get(50));

        // --- CHUNK 9: Items 801-838 (worker_m4_c9) ---
        // Batch 81: Items 801-810
        add.accept(disperserCanisters.get(51));
        add.accept(disperserCanisters.get(52));
        add.accept(disperserCanisters.get(53));
        add.accept(disperserCanisters.get(54));
        add.accept(disperserCanisters.get(55));
        add.accept(disperserCanisters.get(56));
        add.accept(disperserCanisters.get(57));
        add.accept(disperserCanisters.get(58));
        add.accept(disperserCanisters.get(59));
        add.accept(disperserCanisters.get(60));

        // Batch 82: Items 811-820
        add.accept(disperserCanisters.get(61));
        add.accept(disperserCanisters.get(62));
        add.accept(disperserCanisters.get(63));
        add.accept(disperserCanisters.get(64));
        add.accept(disperserCanisters.get(65));
        add.accept(disperserCanisters.get(66));
        add.accept(disperserCanisters.get(67));
        add.accept(disperserCanisters.get(68));
        add.accept(disperserCanisters.get(69));
        add.accept(disperserCanisters.get(70));

        // Batch 83: Items 821-830
        add.accept(disperserCanisters.get(71));
        add.accept(disperserCanisters.get(72));
        add.accept(disperserCanisters.get(73));
        add.accept(disperserCanisters.get(74));
        add.accept(disperserCanisters.get(75));
        add.accept(disperserCanisters.get(76));
        add.accept(disperserCanisters.get(77));
        add.accept(disperserCanisters.get(78));
        add.accept(disperserCanisters.get(79));
        add.accept(disperserCanisters.get(80));

        // Batch 84: Items 831-838
        add.accept(disperserCanisters.get(81));
        add.accept(disperserCanisters.get(82));
        add.accept(com.hbm_m.item.liquids.ItemFluidTank.make(ModItems.DISPERSER_CANISTER.get(), ModFluids.CUSTOM_DEMO.getSource(), 1)); // 833
        add.accept(new ItemStack(ModItems.GLYPHID_GLAND_EMPTY.get()));
        java.util.List<ItemStack> glyphidGlands = ((com.hbm_m.item.liquids.ItemFluidTank) ModItems.GLYPHID_GLAND.get()).getSubItems();
        add.accept(glyphidGlands.get(0));
        add.accept(glyphidGlands.get(1));
        add.accept(new ItemStack(ModItems.ULLAPOOL_CABER.get()));
        add.accept(new ItemStack(ModItems.WEAPONIZED_STARBLASTER_CELL.get()));
    }

    /** populateConsumablesTab: порядок из оригинального 1.7.10 (ModBlocks/ModItems, вкладка consumable); отсутствующие в порте предметы пропущены. */
    public static void populateConsumablesTab(BiConsumer<ItemStack, CreativeModeTab.TabVisibility> acceptor) {
        Consumer<ItemStack> add = stack -> acceptor.accept(stack, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);

        // --- CHUNK 1: Items 1-100 (worker_m2_c1) ---
        // 1-10
        add.accept(new ItemStack(ModBlocks.MACHINE_KEYFORGE.get()));
        add.accept(new ItemStack(ModBlocks.ARMOR_TABLE.get()));
        add.accept(new ItemStack(ModBlocks.MACHINE_WEAPON_TABLE.get()));
        add.accept(new ItemStack(ModBlocks.CRATE.get()));
        add.accept(new ItemStack(ModBlocks.CRATE_WEAPON.get()));
        add.accept(new ItemStack(ModBlocks.CRATE_LEAD.get()));
        add.accept(new ItemStack(ModBlocks.CRATE_METAL.get()));
        add.accept(new ItemStack(ModBlocks.CRATE_CAN.get()));
        add.accept(new ItemStack(ModBlocks.CRATE_AMMO.get()));
        add.accept(new ItemStack(ModBlocks.CRATE_JUNGLE.get()));

        // 11-20
        add.accept(new ItemStack(ModItems.BOBMAZON.get()));
        add.accept(new ItemStack(ModItems.CHOPPER.get()));
        add.accept(new ItemStack(ModItems.SPAWN_WORM.get()));
        add.accept(new ItemStack(ModItems.SPAWN_UFO.get()));
        add.accept(new ItemStack(ModItems.SPAWN_DUCK.get()));
        add.accept(new ItemStack(ModItems.LINKER.get()));
        add.accept(new ItemStack(ModItems.REACTOR_SENSOR.get()));
        add.accept(new ItemStack(ModItems.OIL_DETECTOR.get()));
        add.accept(new ItemStack(ModItems.ORE_DENSITY_SCANNER.get()));
        add.accept(new ItemStack(ModItems.SURVEY_SCANNER.get()));

        // 21-30
        add.accept(new ItemStack(ModItems.MIRROR_TOOL.get()));
        add.accept(new ItemStack(ModItems.RBMK_TOOL.get()));
        add.accept(new ItemStack(ModItems.DRONE_LINKER.get()));
        add.accept(new ItemStack(ModItems.RADAR_LINKER.get()));
        add.accept(new ItemStack(ModItems.COLTAN_TOOL.get()));
        add.accept(new ItemStack(ModItems.POWER_NET_TOOL.get()));
        add.accept(new ItemStack(ModItems.ANALYSIS_TOOL.get()));
        add.accept(new ItemStack(ModItems.COUPLING_TOOL.get()));
        add.accept(new ItemStack(ModItems.SETTINGS_TOOL.get()));
        add.accept(new ItemStack(ModItems.RTTY_PAGER.get()));

        // 31-40
        add.accept(new ItemStack(ModItems.RTTY_DESIGNATOR.get()));
        add.accept(new ItemStack(ModItems.DOSIMETER.get()));
        add.accept(new ItemStack(ModItems.GEIGER_COUNTER.get()));
        add.accept(new ItemStack(ModItems.DIGAMMA_DIAGNOSTIC.get()));
        add.accept(new ItemStack(ModItems.POLLUTION_DETECTOR.get()));
        add.accept(new ItemStack(ModItems.CONTAINMENT_BOX.get()));
        add.accept(new ItemStack(ModItems.PLASTIC_BAG.get()));
        add.accept(new ItemStack(ModItems.AMMO_BAG.get()));
        add.accept(new ItemStack(ModItems.AMMO_BAG_INFINITE.get()));
        add.accept(new ItemStack(ModItems.CASING_BAG.get()));

        // 41-50
        add.accept(new ItemStack(ModItems.KEY.get()));
        add.accept(new ItemStack(ModItems.KEY_KIT.get()));
        add.accept(new ItemStack(ModItems.KEY_FAKE.get()));
        add.accept(new ItemStack(ModItems.PIN.get()));
        add.accept(new ItemStack(ModItems.PADLOCK_RUSTY.get()));
        add.accept(new ItemStack(ModItems.PADLOCK.get()));
        add.accept(new ItemStack(ModItems.PADLOCK_REINFORCED.get()));
        add.accept(new ItemStack(ModItems.SYRINGE_EMPTY.get()));
        add.accept(new ItemStack(ModItems.SYRINGE_ANTIDOTE.get()));
        add.accept(new ItemStack(ModItems.SYRINGE_POISON.get()));

        // 51-60
        add.accept(new ItemStack(ModItems.SYRINGE_AWESOME.get()));
        add.accept(new ItemStack(ModItems.SYRINGE_METAL_EMPTY.get()));
        add.accept(new ItemStack(ModItems.SYRINGE_METAL_STIMPAK.get()));
        add.accept(new ItemStack(ModItems.SYRINGE_METAL_MEDX.get()));
        add.accept(new ItemStack(ModItems.SYRINGE_METAL_PSYCHO.get()));
        add.accept(new ItemStack(ModItems.SYRINGE_METAL_SUPER.get()));
        add.accept(new ItemStack(ModItems.SYRINGE_TAINT.get()));
        add.accept(new ItemStack(ModItems.MED_BAG.get()));
        add.accept(new ItemStack(ModItems.IV_EMPTY.get()));
        add.accept(new ItemStack(ModItems.IV_BLOOD.get()));

        // 61-70
        add.accept(new ItemStack(ModItems.IV_XP_EMPTY.get()));
        add.accept(new ItemStack(ModItems.IV_XP.get()));
        add.accept(new ItemStack(ModItems.RADAWAY.get()));
        add.accept(new ItemStack(ModItems.RADAWAY_STRONG.get()));
        add.accept(new ItemStack(ModItems.RADAWAY_FLUSH.get()));
        add.accept(new ItemStack(ModItems.RADX.get()));
        add.accept(new ItemStack(ModItems.SIOX.get()));
        add.accept(new ItemStack(ModItems.PILL_HERBAL.get()));
        add.accept(new ItemStack(ModItems.PILL_IODINE.get()));
        add.accept(new ItemStack(ModItems.XANAX.get()));

        // 71-80
        add.accept(new ItemStack(ModItems.FMN.get()));
        add.accept(new ItemStack(ModItems.FIVE_HTP.get()));
        add.accept(new ItemStack(ModItems.PLAN_C.get()));
        add.accept(new ItemStack(ModItems.PILL_RED.get()));
        add.accept(new ItemStack(ModItems.STEALTH_BOY.get()));
        add.accept(new ItemStack(ModItems.GAS_MASK_FILTER.get()));
        add.accept(new ItemStack(ModItems.GAS_MASK_FILTER_MONO.get()));
        add.accept(new ItemStack(ModItems.GAS_MASK_FILTER_COMBO.get()));
        add.accept(new ItemStack(ModItems.GAS_MASK_FILTER_RAG.get()));
        add.accept(new ItemStack(ModItems.GAS_MASK_FILTER_PISS.get()));

        // 81-90
        add.accept(new ItemStack(ModItems.JETPACK_TANK.get()));
        add.accept(new ItemStack(ModItems.GUN_KIT_1.get()));
        add.accept(new ItemStack(ModItems.GUN_KIT_2.get()));
        add.accept(new ItemStack(ModItems.BOMB_WAFFLE.get()));
        add.accept(new ItemStack(ModItems.SCHNITZEL_VEGAN.get()));
        add.accept(new ItemStack(ModItems.COTTON_CANDY.get()));
        add.accept(new ItemStack(ModItems.APPLE_LEAD.get()));
        add.accept(new ItemStack(ModItems.APPLE_LEAD_1.get()));
        add.accept(new ItemStack(ModItems.APPLE_LEAD_2.get()));
        add.accept(new ItemStack(ModItems.APPLE_SCHRABIDIUM.get()));

        // 91-100
        add.accept(new ItemStack(ModItems.APPLE_SCHRABIDIUM_1.get()));
        add.accept(new ItemStack(ModItems.APPLE_SCHRABIDIUM_2.get()));
        add.accept(new ItemStack(ModItems.TEM_FLAKES.get()));
        add.accept(new ItemStack(ModItems.TEM_FLAKES_1.get()));
        add.accept(new ItemStack(ModItems.TEM_FLAKES_2.get()));
        add.accept(new ItemStack(ModItems.GLOWING_STEW.get()));
        add.accept(new ItemStack(ModItems.BALEFIRE_SCRAMBLED.get()));
        add.accept(new ItemStack(ModItems.BALEFIRE_AND_HAM.get()));
        add.accept(new ItemStack(ModItems.LEMON.get()));
        add.accept(new ItemStack(ModItems.DEFINITELYFOOD.get()));

        // --- CHUNK 2 CURSOR: Items 101-200 (worker_m2_c2) ---
        add.accept(new ItemStack(ModItems.LOOPS.get()));
        add.accept(new ItemStack(ModItems.LOOP_STEW.get()));
        add.accept(new ItemStack(ModItems.SPONGEBOB_MACARONI.get()));
        add.accept(new ItemStack(ModItems.FOODITEM.get()));
        add.accept(new ItemStack(ModItems.TWINKIE.get()));
        add.accept(new ItemStack(ModItems.STATIC_SANDWICH.get()));
        add.accept(new ItemStack(ModItems.PUDDING.get()));
        add.accept(new ItemStack(ModItems.PANCAKE.get()));
        add.accept(new ItemStack(ModItems.NUGGET.get()));
        add.accept(new ItemStack(ModItems.PEAS.get()));
        add.accept(new ItemStack(ModItems.MARSHMALLOW.get()));
        add.accept(new ItemStack(ModItems.MARSHMALLOW_ROASTED.get()));
        add.accept(new ItemStack(ModItems.CHEESE.get()));
        add.accept(new ItemStack(ModItems.CHEESE_QUESADILLA.get()));
        add.accept(new ItemStack(ModItems.GLYPHID_MEAT.get()));
        add.accept(new ItemStack(ModItems.GLYPHID_MEAT_GRILLED.get()));
        add.accept(new ItemStack(ModItems.EGG_GLYPHID.get()));
        add.accept(new ItemStack(ModItems.MED_IPECAC.get()));
        add.accept(new ItemStack(ModItems.MED_PTSD.get()));
        add.accept(new ItemStack(ModItems.CANTEEN_VODKA.get()));
        add.accept(new ItemStack(ModItems.MUCHO_MANGO.get()));
        add.accept(new ItemStack(ModItems.CHOCOLATE.get()));
        add.accept(new ItemStack(ModItems.CAN_EMPTY.get()));
        add.accept(new ItemStack(ModItems.CAN_SMART.get()));
        add.accept(new ItemStack(ModItems.CAN_CREATURE.get()));
        add.accept(new ItemStack(ModItems.CAN_REDBOMB.get()));
        add.accept(new ItemStack(ModItems.CAN_MRSUGAR.get()));
        add.accept(new ItemStack(ModItems.CAN_OVERCHARGE.get()));
        add.accept(new ItemStack(ModItems.CAN_LUNA.get()));
        add.accept(new ItemStack(ModItems.CAN_BEPIS.get()));
        add.accept(new ItemStack(ModItems.CAN_BREEN.get()));
        add.accept(new ItemStack(ModItems.CAN_MUG.get()));
        add.accept(new ItemStack(ModItems.COFFEE.get()));
        add.accept(new ItemStack(ModItems.COFFEE_RADIUM.get()));
        add.accept(new ItemStack(ModItems.BOTTLE_NUKA.get()));
        add.accept(new ItemStack(ModItems.BOTTLE_CHERRY.get()));
        add.accept(new ItemStack(ModItems.BOTTLE_QUANTUM.get()));
        add.accept(new ItemStack(ModItems.BOTTLE_SPARKLE.get()));
        add.accept(new ItemStack(ModItems.BOTTLE_RAD.get()));
        add.accept(new ItemStack(ModItems.BOTTLE2_KORL.get()));
        add.accept(new ItemStack(ModItems.BOTTLE2_FRITZ.get()));
        add.accept(new ItemStack(ModItems.BOTTLE_OPENER.get()));
        add.accept(new ItemStack(ModItems.FLASK_INFUSION.get()));
        add.accept(new ItemStack(ModItems.CANNED_BEEF.get()));
        add.accept(new ItemStack(ModItems.CANNED_TUNA.get()));
        add.accept(new ItemStack(ModItems.CANNED_MYSTERY.get()));
        add.accept(new ItemStack(ModItems.CANNED_PASHTET.get()));
        add.accept(new ItemStack(ModItems.CANNED_CHEESE.get()));
        add.accept(new ItemStack(ModItems.CANNED_SLIME.get()));
        add.accept(new ItemStack(ModItems.CANNED_MILK.get()));
        add.accept(new ItemStack(ModItems.CANNED_ASS.get()));
        add.accept(new ItemStack(ModItems.CANNED_PIZZA.get()));
        add.accept(new ItemStack(ModItems.CANNED_TUBE.get()));
        add.accept(new ItemStack(ModItems.CANNED_TOMATO.get()));
        add.accept(new ItemStack(ModItems.CANNED_ASBESTOS.get()));
        add.accept(new ItemStack(ModItems.CANNED_BHOLE.get()));
        add.accept(new ItemStack(ModItems.CANNED_HOTDOGS.get()));
        add.accept(new ItemStack(ModItems.CANNED_LEFTOVERS.get()));
        add.accept(new ItemStack(ModItems.CANNED_YOGURT.get()));
        add.accept(new ItemStack(ModItems.CANNED_STEW.get()));
        add.accept(new ItemStack(ModItems.CANNED_CHINESE.get()));
        add.accept(new ItemStack(ModItems.CANNED_OIL.get()));
        add.accept(new ItemStack(ModItems.CANNED_FIST.get()));
        add.accept(new ItemStack(ModItems.CANNED_SPAM.get()));
        add.accept(new ItemStack(ModItems.CANNED_FRIED.get()));
        add.accept(new ItemStack(ModItems.CANNED_NAPALM.get()));
        add.accept(new ItemStack(ModItems.CANNED_DIESEL.get()));
        add.accept(new ItemStack(ModItems.CANNED_KEROSENE.get()));
        add.accept(new ItemStack(ModItems.CANNED_RECURSION.get()));
        add.accept(new ItemStack(ModItems.CANNED_BARK.get()));
        add.accept(new ItemStack(ModItems.CAP_NUKA.get()));
        add.accept(new ItemStack(ModItems.CAP_QUANTUM.get()));
        add.accept(new ItemStack(ModItems.CAP_SPARKLE.get()));
        add.accept(new ItemStack(ModItems.CAP_RAD.get()));
        add.accept(new ItemStack(ModItems.CAP_KORL.get()));
        add.accept(new ItemStack(ModItems.CAP_FRITZ.get()));
        add.accept(new ItemStack(ModItems.RING_PULL.get()));
        add.accept(new ItemStack(ModItems.CAN_KEY.get()));
        add.accept(new ItemStack(ModItems.COIN_CREEPER.get()));
        add.accept(new ItemStack(ModItems.COIN_RADIATION.get()));
        add.accept(new ItemStack(ModItems.COIN_MASKMAN.get()));
        add.accept(new ItemStack(ModItems.COIN_WORM.get()));
        add.accept(new ItemStack(ModItems.COIN_UFO.get()));
        add.accept(new ItemStack(ModItems.COIN_TOKEN.get()));
        add.accept(new ItemStack(ModItems.MEDAL_LIQUIDATOR.get()));
        add.accept(new ItemStack(ModItems.BOTTLED_CLOUD.get()));
        add.accept(new ItemStack(ModItems.PROTECTION_CHARM.get()));
        add.accept(new ItemStack(ModItems.METEOR_CHARM.get()));
        add.accept(new ItemStack(ModItems.NEUTRINO_LENS.get()));
        add.accept(new ItemStack(ModItems.GAS_TESTER.get()));
        add.accept(new ItemStack(ModItems.DEFUSER_GOLD.get()));
        add.accept(new ItemStack(ModItems.BALLISTIC_GAUNTLET.get()));
        add.accept(new ItemStack(ModItems.NIGHT_VISION.get()));
        add.accept(new ItemStack(ModItems.AUSTRALIUM_III.get()));
        add.accept(new ItemStack(ModItems.ARMOR_BATTERY.get()));
        add.accept(new ItemStack(ModItems.ARMOR_BATTERY_MK2.get()));
        add.accept(new ItemStack(ModItems.ARMOR_BATTERY_MK3.get()));
        add.accept(new ItemStack(ModItems.CHOCOLATE_MILK.get()));
        add.accept(new ItemStack(ModItems.CIGARETTE.get()));
        add.accept(new ItemStack(ModItems.CRACKPIPE.get()));

        // --- CHUNK 3: Items 201-275 (verified & sorted) ---
        add.accept(new ItemStack(ModItems.BDCL.get()));
        add.accept(new ItemStack(ModItems.ATTACHMENT_MASK.get()));
        add.accept(new ItemStack(ModItems.ATTACHMENT_MASK_MONO.get()));
        add.accept(new ItemStack(ModItems.BACK_TESLA.get()));
        add.accept(new ItemStack(ModItems.SERVO_SET.get()));
        add.accept(new ItemStack(ModItems.SERVO_SET_DESH.get()));
        add.accept(new ItemStack(ModItems.PADS_RUBBER.get()));
        add.accept(new ItemStack(ModItems.PADS_SLIME.get()));
        add.accept(new ItemStack(ModItems.PADS_STATIC.get()));
        add.accept(new ItemStack(ModItems.PAINT_CLADDING.get()));
        add.accept(new ItemStack(ModItems.RUBBER_CLADDING.get()));
        add.accept(new ItemStack(ModItems.LEAD_CLADDING.get()));
        add.accept(new ItemStack(ModItems.DESH_CLADDING.get()));
        add.accept(new ItemStack(ModItems.GHIORSIUM_CLADDING.get()));
        add.accept(new ItemStack(ModItems.CLADDING_IRON.get()));
        add.accept(new ItemStack(ModItems.CLADDING_OBSIDIAN.get()));
        add.accept(new ItemStack(ModItems.INSERT_KEVLAR.get()));
        add.accept(new ItemStack(ModItems.INSERT_SAPI.get()));
        add.accept(new ItemStack(ModItems.INSERT_ESAPI.get()));
        add.accept(new ItemStack(ModItems.INSERT_XSAPI.get()));
        add.accept(new ItemStack(ModItems.INSERT_STEEL.get()));
        add.accept(new ItemStack(ModItems.INSERT_DU.get()));
        add.accept(new ItemStack(ModItems.INSERT_POLONIUM.get()));
        add.accept(new ItemStack(ModItems.INSERT_GHIORSIUM.get()));
        add.accept(new ItemStack(ModItems.INSERT_ERA.get()));
        add.accept(new ItemStack(ModItems.INSERT_YHARONITE.get()));
        add.accept(new ItemStack(ModItems.INSERT_DOXIUM.get()));
        add.accept(new ItemStack(ModItems.ARMOR_POLISH.get()));
        add.accept(new ItemStack(ModItems.BANDAID.get()));
        add.accept(new ItemStack(ModItems.SERUM.get()));
        add.accept(new ItemStack(ModItems.QUARTZ_PLUTONIUM.get()));
        add.accept(new ItemStack(ModItems.MORNING_GLORY.get()));
        add.accept(new ItemStack(ModItems.LODESTONE.get()));
        add.accept(new ItemStack(ModItems.HORSESHOE_MAGNET.get()));
        add.accept(new ItemStack(ModItems.INDUSTRIAL_MAGNET.get()));
        add.accept(new ItemStack(ModItems.BATHWATER.get()));
        add.accept(new ItemStack(ModItems.BATHWATER_MK2.get()));
        add.accept(new ItemStack(ModItems.SPIDER_MILK.get()));
        add.accept(new ItemStack(ModItems.INK.get()));
        add.accept(new ItemStack(ModItems.HEART_PIECE.get()));
        add.accept(new ItemStack(ModItems.HEART_CONTAINER.get()));
        add.accept(new ItemStack(ModItems.HEART_BOOSTER.get()));
        add.accept(new ItemStack(ModItems.HEART_FAB.get()));
        add.accept(new ItemStack(ModItems.BLACK_DIAMOND.get()));
        add.accept(new ItemStack(ModItems.WD40.get()));
        add.accept(new ItemStack(ModItems.SCRUMPY.get()));
        add.accept(new ItemStack(ModItems.WILD_P.get()));
        add.accept(new ItemStack(ModItems.SHACKLES.get()));
        add.accept(new ItemStack(ModItems.INJECTOR_5HTP.get()));
        add.accept(new ItemStack(ModItems.INJECTOR_KNIFE.get()));
        add.accept(new ItemStack(ModItems.BOMB_CALLER.get()));
        add.accept(new ItemStack(ModItems.BOMB_CALLER_NAPALM.get()));
        add.accept(new ItemStack(ModItems.BOMB_CALLER_CHLORINE.get()));
        add.accept(new ItemStack(ModItems.BOMB_CALLER_ORANGE.get()));
        add.accept(new ItemStack(ModItems.BOMB_CALLER_ATOMIC.get()));
        add.accept(new ItemStack(ModItems.METEOR_REMOTE.get()));
        add.accept(new ItemStack(ModItems.ANCHOR_REMOTE.get()));
        add.accept(new ItemStack(ModItems.REBAR_PLACER.get()));
        add.accept(new ItemStack(ModItems.WAND_K.get()));
        add.accept(new ItemStack(ModItems.WAND_S.get()));
        add.accept(new ItemStack(ModItems.WAND_D.get()));
        add.accept(new ItemStack(ModItems.STRUCTURE_CUSTOMMACHINE.get()));
        add.accept(new ItemStack(ModItems.ROD_OF_DISCORD.get()));
        add.accept(new ItemStack(ModItems.POLAROID.get()));
        add.accept(new ItemStack(ModItems.GLITCH.get()));
        add.accept(new ItemStack(ModItems.BOOK_SECRET.get()));
        add.accept(new ItemStack(ModItems.NUKE_STARTER_KIT.get()));
        add.accept(new ItemStack(ModItems.NUKE_ADVANCED_KIT.get()));
        add.accept(new ItemStack(ModItems.NUKE_COMMERCIALLY_KIT.get()));
        add.accept(new ItemStack(ModItems.NUKE_ELECTRIC_KIT.get()));
        add.accept(new ItemStack(ModItems.HAZMAT_KIT.get()));
        add.accept(new ItemStack(ModItems.HAZMAT_RED_KIT.get()));
        add.accept(new ItemStack(ModItems.HAZMAT_GREY_KIT.get()));
        add.accept(new ItemStack(ModItems.TOOLBOX.get()));
        add.accept(com.hbm_m.item.tool.ItemGuideBook.make(com.hbm_m.item.tool.ItemGuideBook.BookType.RBMK));
        add.accept(com.hbm_m.item.tool.ItemGuideBook.make(com.hbm_m.item.tool.ItemGuideBook.BookType.STARTER));

        // --- Modernized Extras ---
        add.accept(new ItemStack(ModItems.MED_SCHIZOPHRENIA.get()));
        add.accept(new ItemStack(ModItems.GAS_MASK.get()));
        add.accept(new ItemStack(ModItems.GAS_MASK_M65.get()));
        add.accept(new ItemStack(ModItems.GAS_MASK_MONO.get()));
        add.accept(new ItemStack(ModItems.GAS_MASK_OLDE.get()));
        add.accept(new ItemStack(ModItems.MASK_RAG.get()));
        add.accept(new ItemStack(ModItems.MASK_PISS.get()));
    }

    // ==================== Ванильная вкладка «Инструменты» ====================

    /** Werkzeuge des Originals (ItemTool/ItemHoe -> Vanilla-Werkzeug-Reiter), Reihenfolge der Registrierung. */
    public static void populateToolsTab(BiConsumer<ItemStack, CreativeModeTab.TabVisibility> acceptor) {
        acceptor.accept(new ItemStack(ModItems.CHAINSAW.get()), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
        acceptor.accept(new ItemStack(ModItems.SCHRABIDIUM_PICKAXE.get()), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
        acceptor.accept(new ItemStack(ModItems.SCHRABIDIUM_AXE.get()), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
        acceptor.accept(new ItemStack(ModItems.SCHRABIDIUM_SHOVEL.get()), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
        acceptor.accept(new ItemStack(ModItems.SCHRABIDIUM_HOE.get()), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
        acceptor.accept(new ItemStack(ModItems.TITANIUM_PICKAXE.get()), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
        acceptor.accept(new ItemStack(ModItems.TITANIUM_AXE.get()), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
        acceptor.accept(new ItemStack(ModItems.TITANIUM_SHOVEL.get()), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
        acceptor.accept(new ItemStack(ModItems.TITANIUM_HOE.get()), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
        acceptor.accept(new ItemStack(ModItems.STEEL_PICKAXE.get()), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
        acceptor.accept(new ItemStack(ModItems.STEEL_AXE.get()), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
        acceptor.accept(new ItemStack(ModItems.STEEL_SHOVEL.get()), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
        acceptor.accept(new ItemStack(ModItems.STEEL_HOE.get()), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
        acceptor.accept(new ItemStack(ModItems.ALLOY_PICKAXE.get()), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
        acceptor.accept(new ItemStack(ModItems.ALLOY_AXE.get()), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
        acceptor.accept(new ItemStack(ModItems.ALLOY_SHOVEL.get()), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
        acceptor.accept(new ItemStack(ModItems.ALLOY_HOE.get()), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
        acceptor.accept(new ItemStack(ModItems.CMB_PICKAXE.get()), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
        acceptor.accept(new ItemStack(ModItems.CMB_AXE.get()), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
        acceptor.accept(new ItemStack(ModItems.CMB_SHOVEL.get()), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
        acceptor.accept(new ItemStack(ModItems.CMB_HOE.get()), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
        acceptor.accept(new ItemStack(ModItems.ELEC_PICKAXE.get()), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
        acceptor.accept(new ItemStack(ModItems.ELEC_AXE.get()), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
        acceptor.accept(new ItemStack(ModItems.ELEC_SHOVEL.get()), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
        acceptor.accept(new ItemStack(ModItems.DESH_PICKAXE.get()), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
        acceptor.accept(new ItemStack(ModItems.DESH_AXE.get()), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
        acceptor.accept(new ItemStack(ModItems.DESH_SHOVEL.get()), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
        acceptor.accept(new ItemStack(ModItems.DESH_HOE.get()), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
        acceptor.accept(new ItemStack(ModItems.COBALT_PICKAXE.get()), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
        acceptor.accept(new ItemStack(ModItems.COBALT_AXE.get()), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
        acceptor.accept(new ItemStack(ModItems.COBALT_SHOVEL.get()), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
        acceptor.accept(new ItemStack(ModItems.COBALT_HOE.get()), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
        acceptor.accept(new ItemStack(ModItems.COBALT_DECORATED_PICKAXE.get()), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
        acceptor.accept(new ItemStack(ModItems.COBALT_DECORATED_AXE.get()), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
        acceptor.accept(new ItemStack(ModItems.COBALT_DECORATED_SHOVEL.get()), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
        acceptor.accept(new ItemStack(ModItems.COBALT_DECORATED_HOE.get()), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
        acceptor.accept(new ItemStack(ModItems.STARMETAL_PICKAXE.get()), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
        acceptor.accept(new ItemStack(ModItems.STARMETAL_AXE.get()), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
        acceptor.accept(new ItemStack(ModItems.STARMETAL_SHOVEL.get()), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
        acceptor.accept(new ItemStack(ModItems.STARMETAL_HOE.get()), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
        acceptor.accept(new ItemStack(ModItems.CENTRI_STICK.get()), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
        acceptor.accept(new ItemStack(ModItems.SMASHING_HAMMER.get()), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
        acceptor.accept(new ItemStack(ModItems.BISMUTH_PICKAXE.get()), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
        acceptor.accept(new ItemStack(ModItems.BISMUTH_AXE.get()), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
        acceptor.accept(new ItemStack(ModItems.VOLCANIC_PICKAXE.get()), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
        acceptor.accept(new ItemStack(ModItems.VOLCANIC_AXE.get()), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
        acceptor.accept(new ItemStack(ModItems.CHLOROPHYTE_PICKAXE.get()), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
        acceptor.accept(new ItemStack(ModItems.CHLOROPHYTE_AXE.get()), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
        acceptor.accept(new ItemStack(ModItems.MESE_PICKAXE.get()), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
        acceptor.accept(new ItemStack(ModItems.MESE_AXE.get()), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
        acceptor.accept(new ItemStack(ModItems.DWARVEN_PICKAXE.get()), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
        acceptor.accept(new ItemStack(ModItems.DRILL_TITANIUM.get()), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
        // matchstick / balefire_and_steel: CreativeTabs.tabTools
        acceptor.accept(new ItemStack(ModItems.MATCHSTICK.get()), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
        acceptor.accept(new ItemStack(ModItems.BALEFIRE_AND_STEEL.get()), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
    }


    // ==================== Ванильная вкладка «Бой» (броня и инструменты) ====================

        public static void populateCombatTab(BiConsumer<ItemStack, CreativeModeTab.TabVisibility> acceptor) {
        // ДОБАВЛЕНА ЗАЩИТА ОТ ВНУТРЕННИХ ДУБЛИКАТОВ (поскольку в коде ниже много повторяющихся шлемов и мечей)
        Set<String> seen = new HashSet<>();
        Consumer<ItemStack> add = stack -> {
            if (stack == null || stack.isEmpty()) return;
            String itemId = String.valueOf(BuiltInRegistries.ITEM.getKey(stack.getItem()));
            CompoundTag itemTag = StackNbt.read(stack);
            String tag = itemTag == null ? "" : itemTag.toString();
            //? if >= 1.21.1 {
            /*net.minecraft.world.item.component.BlockItemStateProperties bs = stack.get(net.minecraft.core.component.DataComponents.BLOCK_STATE);
            if (bs != null && !bs.isEmpty()) {
                tag = tag + "|bs:" + bs.properties().toString();
            }
            *///?}
            if (!seen.add(itemId + "|" + tag)) return; // Игнорируем дубликат, чтобы не крашнуть 1.21.1
            acceptor.accept(stack, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
        };

        // Schwerter des Originals (ItemSword -> Vanilla-Kampf-Reiter), Reihenfolge der Registrierung
        add.accept(new ItemStack(ModItems.REDSTONE_SWORD.get()));
        add.accept(new ItemStack(ModItems.BIG_SWORD.get()));
        add.accept(new ItemStack(ModItems.SCHRABIDIUM_SWORD.get()));
        add.accept(new ItemStack(ModItems.TITANIUM_SWORD.get()));
        add.accept(new ItemStack(ModItems.STEEL_SWORD.get()));
        add.accept(new ItemStack(ModItems.ALLOY_SWORD.get()));
        add.accept(new ItemStack(ModItems.CMB_SWORD.get()));
        add.accept(new ItemStack(ModItems.ELEC_SWORD.get()));
        add.accept(new ItemStack(ModItems.DESH_SWORD.get()));
        add.accept(new ItemStack(ModItems.COBALT_SWORD.get()));
        add.accept(new ItemStack(ModItems.COBALT_DECORATED_SWORD.get()));
        add.accept(new ItemStack(ModItems.STARMETAL_SWORD.get()));
        add.accept(new ItemStack(ModItems.DNT_SWORD.get()));
        add.accept(new ItemStack(ModItems.METEORITE_SWORD.get()));
        add.accept(new ItemStack(ModItems.METEORITE_SWORD_SEARED.get()));
        add.accept(new ItemStack(ModItems.METEORITE_SWORD_REFORGED.get()));
        add.accept(new ItemStack(ModItems.METEORITE_SWORD_HARDENED.get()));
        add.accept(new ItemStack(ModItems.METEORITE_SWORD_ALLOYED.get()));
        add.accept(new ItemStack(ModItems.METEORITE_SWORD_MACHINED.get()));
        add.accept(new ItemStack(ModItems.METEORITE_SWORD_TREATED.get()));
        add.accept(new ItemStack(ModItems.METEORITE_SWORD_ETCHED.get()));
        add.accept(new ItemStack(ModItems.METEORITE_SWORD_BRED.get()));
        add.accept(new ItemStack(ModItems.METEORITE_SWORD_IRRADIATED.get()));
        add.accept(new ItemStack(ModItems.METEORITE_SWORD_FUSED.get()));
        add.accept(new ItemStack(ModItems.METEORITE_SWORD_BALEFUL.get()));
        add.accept(new ItemStack(ModItems.CROWBAR.get()));
        add.accept(new ItemStack(ModItems.MESE_GAVEL.get()));
        add.accept(new ItemStack(ModItems.WEAPON_PIPE_LEAD.get()));
        add.accept(new ItemStack(ModItems.REER_GRAAR.get()));

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
        add.accept(new ItemStack(ModItems.ASBESTOS_PLATE.get()));
        add.accept(new ItemStack(ModItems.ASBESTOS_LEGS.get()));
        add.accept(new ItemStack(ModItems.ASBESTOS_BOOTS.get()));
        add.accept(new ItemStack(ModItems.HAZMAT_HELMET.get()));
        add.accept(new ItemStack(ModItems.HAZMAT_CHESTPLATE.get()));
        add.accept(new ItemStack(ModItems.HAZMAT_LEGGINGS.get()));
        add.accept(new ItemStack(ModItems.HAZMAT_BOOTS.get()));
        add.accept(new ItemStack(ModItems.LIQUIDATOR_HELMET.get()));
        add.accept(new ItemStack(ModItems.LIQUIDATOR_CHESTPLATE.get()));
        add.accept(new ItemStack(ModItems.LIQUIDATOR_LEGGINGS.get()));
        add.accept(new ItemStack(ModItems.LIQUIDATOR_BOOTS.get()));
        add.accept(new ItemStack(ModItems.PAA_CHESTPLATE.get()));
        add.accept(new ItemStack(ModItems.PAA_LEGGINGS.get()));
        add.accept(new ItemStack(ModItems.PAA_BOOTS.get()));
        add.accept(new ItemStack(ModItems.STARMETAL_HELMET.get()));
        add.accept(new ItemStack(ModItems.STARMETAL_CHESTPLATE.get()));
        add.accept(new ItemStack(ModItems.STARMETAL_LEGGINGS.get()));
        add.accept(new ItemStack(ModItems.STARMETAL_BOOTS.get()));

        // Restliche Ruestungen 1:1 (ModItemsArmor)
        add.accept(createChargedArmorStack(ModItems.SCHRABIDIUM_HELMET.get()));
        add.accept(createChargedArmorStack(ModItems.SCHRABIDIUM_PLATE.get()));
        add.accept(createChargedArmorStack(ModItems.SCHRABIDIUM_LEGS.get()));
        add.accept(createChargedArmorStack(ModItems.SCHRABIDIUM_BOOTS.get()));
        add.accept(createChargedArmorStack(ModItems.CMB_HELMET.get()));
        add.accept(createChargedArmorStack(ModItems.CMB_PLATE.get()));
        add.accept(createChargedArmorStack(ModItems.CMB_LEGS.get()));
        add.accept(createChargedArmorStack(ModItems.CMB_BOOTS.get()));
        add.accept(createChargedArmorStack(ModItems.ROBES_HELMET.get()));
        add.accept(createChargedArmorStack(ModItems.ROBES_PLATE.get()));
        add.accept(createChargedArmorStack(ModItems.ROBES_LEGS.get()));
        add.accept(createChargedArmorStack(ModItems.ROBES_BOOTS.get()));
        add.accept(createChargedArmorStack(ModItems.HAZMAT_PAA_HELMET.get()));
        add.accept(createChargedArmorStack(ModItems.HAZMAT_PAA_PLATE.get()));
        add.accept(createChargedArmorStack(ModItems.HAZMAT_PAA_LEGS.get()));
        add.accept(createChargedArmorStack(ModItems.HAZMAT_PAA_BOOTS.get()));
        add.accept(createChargedArmorStack(ModItems.STEAMSUIT_HELMET.get()));
        add.accept(createChargedArmorStack(ModItems.STEAMSUIT_PLATE.get()));
        add.accept(createChargedArmorStack(ModItems.STEAMSUIT_LEGS.get()));
        add.accept(createChargedArmorStack(ModItems.STEAMSUIT_BOOTS.get()));
        add.accept(createChargedArmorStack(ModItems.DIESELSUIT_HELMET.get()));
        add.accept(createChargedArmorStack(ModItems.DIESELSUIT_PLATE.get()));
        add.accept(createChargedArmorStack(ModItems.DIESELSUIT_LEGS.get()));
        add.accept(createChargedArmorStack(ModItems.DIESELSUIT_BOOTS.get()));
        add.accept(createChargedArmorStack(ModItems.RPA_HELMET.get()));
        add.accept(createChargedArmorStack(ModItems.RPA_PLATE.get()));
        add.accept(createChargedArmorStack(ModItems.RPA_LEGS.get()));
        add.accept(createChargedArmorStack(ModItems.RPA_BOOTS.get()));
        add.accept(createChargedArmorStack(ModItems.NCRPA_HELMET.get()));
        add.accept(createChargedArmorStack(ModItems.NCRPA_PLATE.get()));
        add.accept(createChargedArmorStack(ModItems.NCRPA_LEGS.get()));
        add.accept(createChargedArmorStack(ModItems.NCRPA_BOOTS.get()));
        add.accept(createChargedArmorStack(ModItems.BJ_HELMET.get()));
        add.accept(createChargedArmorStack(ModItems.BJ_PLATE.get()));
        add.accept(createChargedArmorStack(ModItems.BJ_PLATE_JETPACK.get()));
        add.accept(createChargedArmorStack(ModItems.BJ_LEGS.get()));
        add.accept(createChargedArmorStack(ModItems.BJ_BOOTS.get()));
        add.accept(createChargedArmorStack(ModItems.ENVSUIT_HELMET.get()));
        add.accept(createChargedArmorStack(ModItems.ENVSUIT_PLATE.get()));
        add.accept(createChargedArmorStack(ModItems.ENVSUIT_LEGS.get()));
        add.accept(createChargedArmorStack(ModItems.ENVSUIT_BOOTS.get()));
        add.accept(createChargedArmorStack(ModItems.HEV_HELMET.get()));
        add.accept(createChargedArmorStack(ModItems.HEV_PLATE.get()));
        add.accept(createChargedArmorStack(ModItems.HEV_LEGS.get()));
        add.accept(createChargedArmorStack(ModItems.HEV_BOOTS.get()));
        add.accept(createChargedArmorStack(ModItems.FAU_HELMET.get()));
        add.accept(createChargedArmorStack(ModItems.FAU_PLATE.get()));
        add.accept(createChargedArmorStack(ModItems.FAU_LEGS.get()));
        add.accept(createChargedArmorStack(ModItems.FAU_BOOTS.get()));
        add.accept(createChargedArmorStack(ModItems.DNS_HELMET.get()));
        add.accept(createChargedArmorStack(ModItems.DNS_PLATE.get()));
        add.accept(createChargedArmorStack(ModItems.DNS_LEGS.get()));
        add.accept(createChargedArmorStack(ModItems.DNS_BOOTS.get()));
        add.accept(createChargedArmorStack(ModItems.TAURUN_HELMET.get()));
        add.accept(createChargedArmorStack(ModItems.TAURUN_PLATE.get()));
        add.accept(createChargedArmorStack(ModItems.TAURUN_LEGS.get()));
        add.accept(createChargedArmorStack(ModItems.TAURUN_BOOTS.get()));
        add.accept(createChargedArmorStack(ModItems.TRENCHMASTER_HELMET.get()));
        add.accept(createChargedArmorStack(ModItems.TRENCHMASTER_PLATE.get()));
        add.accept(createChargedArmorStack(ModItems.TRENCHMASTER_LEGS.get()));
        add.accept(createChargedArmorStack(ModItems.TRENCHMASTER_BOOTS.get()));
        add.accept(new ItemStack(ModItems.HAZMAT_HELMET_RED.get()));
        add.accept(new ItemStack(ModItems.HAZMAT_PLATE_RED.get()));
        add.accept(new ItemStack(ModItems.HAZMAT_LEGS_RED.get()));
        add.accept(new ItemStack(ModItems.HAZMAT_BOOTS_RED.get()));
        add.accept(new ItemStack(ModItems.HAZMAT_HELMET_GREY.get()));
        add.accept(new ItemStack(ModItems.HAZMAT_PLATE_GREY.get()));
        add.accept(new ItemStack(ModItems.HAZMAT_LEGS_GREY.get()));
        add.accept(new ItemStack(ModItems.HAZMAT_BOOTS_GREY.get()));
        add.accept(new ItemStack(ModItems.ZIRCONIUM_LEGS.get()));
        add.accept(new ItemStack(ModItems.JACKT.get()));
        add.accept(new ItemStack(ModItems.JACKT2.get()));
        add.accept(new ItemStack(ModItems.GOGGLES.get()));
        add.accept(new ItemStack(ModItems.ASHGLASSES.get()));
        add.accept(new ItemStack(ModItems.HAT.get()));
        add.accept(new ItemStack(ModItems.NO9.get()));
        add.accept(new ItemStack(ModItems.MASK_OF_INFAMY.get()));
        add.accept(new ItemStack(ModItems.JETPACK_FLY.get()));
        add.accept(new ItemStack(ModItems.JETPACK_BREAK.get()));
        add.accept(new ItemStack(ModItems.JETPACK_VECTOR.get()));
        add.accept(new ItemStack(ModItems.JETPACK_BOOST.get()));
        add.accept(new ItemStack(ModItems.WINGS_LIMP.get()));
        add.accept(new ItemStack(ModItems.WINGS_MURK.get()));



        // audit4: laut Original (setCreativeTab) in diesen Reiter, fehlten im Port (nur Dev-Reiter)
        addOriginalTabIds(acceptor,
                "schrabidium_hammer", "shimmer_sledge", "shimmer_axe", "wrench_flipped", "memespoon", "wood_gavel",
                "lead_gavel", "diamond_gavel", "stopsign", "sopsign", "chernobylsign");
    }

    // СЛИТКИ И РЕСУРСЫ

    // ==================== Dev-вкладка (автонаполнение) ====================

    /**
     * DEV-вкладка: все зарегистрированные предметы мода, сгруппированные по типам.
     * Новые предметы попадают сюда автоматически, вручную ничего добавлять не нужно.
     * Порядок: блоки, броня, оружие, инструменты, еда, остальное (по классу предмета);
     * внутри группы — по registry id.
     */
    public static void populateDevItemsTab(BiConsumer<ItemStack, CreativeModeTab.TabVisibility> acceptor) {
        Consumer<ItemStack> add = stack -> acceptor.accept(stack, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);

        List<Item> items = new ArrayList<>();
        for (Item item : BuiltInRegistries.ITEM) {
            if (RefStrings.MODID.equals(BuiltInRegistries.ITEM.getKey(item).getNamespace())) {
                // vI2: reine Port-Testbloecke ohne Modell/Original-Gegenstueck nicht im Dev-Tab (Icon war Missing-Model)
                String p = BuiltInRegistries.ITEM.getKey(item).getPath();
                if (p.equals("test_block") || p.equals("broadcaster")) continue;
                items.add(item);
            }
        }
        items.sort(Comparator.comparing(item -> BuiltInRegistries.ITEM.getKey(item).toString()));

        // Блоки — отдельной группой в начале вкладки
        List<Item> blockItems = new ArrayList<>();
        List<Item> plainItems = new ArrayList<>();
        for (Item item : items) {
            if (item instanceof BlockItem) {
                blockItems.add(item);
            } else {
                plainItems.add(item);
            }
        }
        for (Item item : blockItems) {
            if (item instanceof BlockItem bi && bi.getBlock() instanceof com.hbm_m.block.network.BoxDuctBlock box) {
                for (int i = 0; i <= box.maxMeta(); i += (box.kind == com.hbm_m.block.network.BoxDuctGeometry.Kind.EXHAUST ? 3 : 1)) {
                    add.accept(com.hbm_m.block.network.BoxDuctBlock.stack(item, i));
                }
            } else {
                add.accept(new ItemStack(item));
            }
        }

        // Остальные предметы — группами по типу
        Map<String, List<Item>> byType = new LinkedHashMap<>();
        for (Item item : plainItems) {
            byType.computeIfAbsent(devTabCategory(item), k -> new ArrayList<>()).add(item);
        }
        List<String> types = new ArrayList<>(byType.keySet());
        Collections.sort(types);
        for (String type : types) {
            for (Item item : byType.get(type)) {
                add.accept(new ItemStack(item));
            }
        }

        // ─── AUTO-PORT: fehlende Original-Bloecke (nur DEV-Tab, ungeprueft) ───
        // add.accept(new ItemStack(ModBlocks.BLOCK_ASBESTOS.get()));
        // add.accept(new ItemStack(ModBlocks.BLOCK_BAKELITE.get()));
        // add.accept(new ItemStack(ModBlocks.BLOCK_C4.get()));
        // add.accept(new ItemStack(ModBlocks.BLOCK_COLTAN.get()));
        // add.accept(new ItemStack(ModBlocks.BLOCK_CORIUM.get()));
        // add.accept(new ItemStack(ModBlocks.BLOCK_CORIUM_COBBLE.get()));
        // add.accept(new ItemStack(ModBlocks.BLOCK_EUPHEMIUM_CLUSTER.get()));
        // add.accept(new ItemStack(ModBlocks.BLOCK_FIBERGLASS.get()));
        // add.accept(new ItemStack(ModBlocks.BLOCK_FLUORITE.get()));
        // add.accept(new ItemStack(ModBlocks.BLOCK_GRAPHITE_DETECTOR.get()));
        // add.accept(new ItemStack(ModBlocks.BLOCK_GRAPHITE_DRILLED.get()));
        // add.accept(new ItemStack(ModBlocks.BLOCK_GRAPHITE_FUEL.get()));
        // add.accept(new ItemStack(ModBlocks.BLOCK_GRAPHITE_LITHIUM.get()));
        // add.accept(new ItemStack(ModBlocks.BLOCK_GRAPHITE_PLUTONIUM.get()));
        // add.accept(new ItemStack(ModBlocks.BLOCK_GRAPHITE_SOURCE.get()));
        // add.accept(new ItemStack(ModBlocks.BLOCK_GRAPHITE_TRITIUM.get()));
        // add.accept(new ItemStack(ModBlocks.BLOCK_INSULATOR.get()));
        // add.accept(new ItemStack(ModBlocks.BLOCK_LITHIUM.get()));
        // add.accept(new ItemStack(ModBlocks.BLOCK_MAGNETIZED_TUNGSTEN.get()));
        // add.accept(new ItemStack(ModBlocks.BLOCK_METEOR.get()));
        // add.accept(new ItemStack(ModBlocks.BLOCK_METEOR_BROKEN.get()));
        // add.accept(new ItemStack(ModBlocks.BLOCK_METEOR_COBBLE.get()));
        // add.accept(new ItemStack(ModBlocks.BLOCK_METEOR_MOLTEN.get()));
        // add.accept(new ItemStack(ModBlocks.BLOCK_METEOR_TREASURE.get()));
        // add.accept(new ItemStack(ModBlocks.BLOCK_NITER.get()));
        // add.accept(new ItemStack(ModBlocks.BLOCK_POLYMER.get()));
        // add.accept(new ItemStack(ModBlocks.BLOCK_PU_MIX.get()));
        // add.accept(new ItemStack(ModBlocks.BLOCK_RED_PHOSPHORUS.get()));
        // add.accept(new ItemStack(ModBlocks.BLOCK_RUBBER.get()));
        // add.accept(new ItemStack(ModBlocks.BLOCK_SCRAP.get()));
        // add.accept(new ItemStack(ModBlocks.BLOCK_SEMTEX.get()));
        // add.accept(new ItemStack(ModBlocks.BLOCK_SMORE.get()));
        // add.accept(new ItemStack(ModBlocks.BLOCK_SULFUR.get()));
        // add.accept(new ItemStack(ModBlocks.BLOCK_TANTALIUM.get()));
        // add.accept(new ItemStack(ModBlocks.BLOCK_TRINITITE.get()));
        // add.accept(new ItemStack(ModBlocks.BLOCK_TRITIUM.get()));
        // add.accept(new ItemStack(ModBlocks.BLOCK_WASTE.get()));
        // add.accept(new ItemStack(ModBlocks.BLOCK_WASTE_VITRIFIED.get()));
        // add.accept(new ItemStack(ModBlocks.BLOCK_WHITE_PHOSPHORUS.get()));
        // add.accept(new ItemStack(ModBlocks.BLOCK_YELLOWCAKE.get()));
        // add.accept(new ItemStack(ModBlocks.BRICK_FORGOTTEN.get()));
        // add.accept(new ItemStack(ModBlocks.CONCRETE_LIQUID.get()));
        // add.accept(new ItemStack(ModBlocks.DIGAMMA_MATTER.get()));
        // add.accept(new ItemStack(ModBlocks.DUNGEON_SPAWNER.get()));
        // add.accept(new ItemStack(ModBlocks.EVENT_TESTER.get()));
        // add.accept(new ItemStack(ModBlocks.FLUID_DUCT_PAINTABLE_BLOCK_EXHAUST.get()));
        // add.accept(new ItemStack(ModBlocks.GEIGER_COUNTER_BLOCK.get()));
        // add.accept(new ItemStack(ModBlocks.GEYSIR_NETHER.get()));
        // add.accept(new ItemStack(ModBlocks.ICF_BLOCK.get()));
        // add.accept(new ItemStack(ModBlocks.LAUNCH_TABLE.get()));
        // add.accept(new ItemStack(ModBlocks.LOGIC_BLOCK.get()));
        // add.accept(new ItemStack(ModBlocks.MUSH_BLOCK_STEM.get()));
        // add.accept(new ItemStack(ModBlocks.ALUMINUM_ORE.get()));
        // add.accept(new ItemStack(ModBlocks.ORE_AUSTRALIUM.get()));
        // add.accept(new ItemStack(ModBlocks.ORE_COPPER.get()));
        // add.accept(new ItemStack(ModBlocks.ORE_GNEISS_URANIUM_SCORCHED.get()));
        // add.accept(new ItemStack(ModBlocks.ORE_NETHER_PLUTONIUM.get()));
        // add.accept(new ItemStack(ModBlocks.ORE_NETHER_TUNGSTEN.get()));
        // add.accept(new ItemStack(ModBlocks.ORE_NETHER_URANIUM_SCORCHED.get()));
        // add.accept(new ItemStack(ModBlocks.ORE_OIL_SAND.get()));
        // add.accept(new ItemStack(ModBlocks.ORE_RARE.get()));
        // add.accept(new ItemStack(ModBlocks.ORE_TEKTITE_OSMIRIDIUM.get()));
        // add.accept(new ItemStack(ModBlocks.ORE_URANIUM_SCORCHED.get()));
        // add.accept(new ItemStack(ModBlocks.PILE_BLOCK.get()));
        // add.accept(new ItemStack(ModBlocks.PILE_BRICK.get()));
        // add.accept(new ItemStack(ModBlocks.PNEUMATIC_STORAGE_ACCESS.get()));
        // add.accept(new ItemStack(ModBlocks.PNEUMATIC_STORAGE_CLUTTER.get()));
        // add.accept(new ItemStack(ModBlocks.PNEUMATIC_STORAGE_EXPORTER.get()));
        // add.accept(new ItemStack(ModBlocks.PNEUMATIC_STORAGE_IMPORTER.get()));
        // add.accept(new ItemStack(ModBlocks.PNEUMATIC_STORAGE_MONO.get()));
        // add.accept(new ItemStack(ModBlocks.STEEL_BEAM.get()));
        // add.accept(new ItemStack(ModBlocks.STRUCTURE_ANCHOR.get()));
        // add.accept(new ItemStack(ModBlocks.WAND_TANDEM.get()));
        // // ─── ENDE AUTO-PORT Bloecke ───

        // add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.IRON, MaterialShape.PLATE_CAST)));
        // add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.STEEL, MaterialShape.PLATE_CAST)));
        // add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.COPPER, MaterialShape.PLATE_CAST)));
        // add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.GOLD, MaterialShape.PLATE_CAST)));
        // add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.TITANIUM, MaterialShape.PLATE_CAST)));
        // add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.ALUMINIUM, MaterialShape.PLATE_CAST)));
        // add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.TUNGSTEN, MaterialShape.PLATE_CAST)));
        // add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.ZIRCONIUM, MaterialShape.PLATE_CAST)));
        // add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.OSMIRIDIUM, MaterialShape.PLATE_CAST)));
        // add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.ALLOY, MaterialShape.PLATE_CAST)));
        // add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.DURA_STEEL, MaterialShape.PLATE_CAST)));
        // add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.DESH, MaterialShape.PLATE_CAST)));
        // add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.STAR_METAL, MaterialShape.PLATE_CAST)));
        // add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.TCALLOY, MaterialShape.PLATE_CAST)));
        // add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.CDALLOY, MaterialShape.PLATE_CAST)));
        // add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.CMB, MaterialShape.PLATE_CAST)));
        // add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.SCHRABIDIUM, MaterialShape.PLATE_CAST)));
        // add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.BBRONZE, MaterialShape.PLATE_CAST)));
        // add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.ABRONZE, MaterialShape.PLATE_CAST)));
        // add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.SATURNITE, MaterialShape.PLATE_CAST)));
        // add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.IRON, MaterialShape.PLATE_WELDED)));
        // add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.STEEL, MaterialShape.PLATE_WELDED)));
        // add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.COPPER, MaterialShape.PLATE_WELDED)));
        // add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.TITANIUM, MaterialShape.PLATE_WELDED)));
        // add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.ALUMINIUM, MaterialShape.PLATE_WELDED)));
        // add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.TUNGSTEN, MaterialShape.PLATE_WELDED)));
        // add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.ZIRCONIUM, MaterialShape.PLATE_WELDED)));
        // add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.OSMIRIDIUM, MaterialShape.PLATE_WELDED)));
        // add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.TCALLOY, MaterialShape.PLATE_WELDED)));
        // add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.CDALLOY, MaterialShape.PLATE_WELDED)));
        // add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.CMB, MaterialShape.PLATE_WELDED)));
        // add.accept(new ItemStack(ModItems.MOLD_BARREL_HEAVY.get()));
        // add.accept(new ItemStack(ModItems.MOLD_BARREL_LIGHT.get()));
        // add.accept(new ItemStack(ModItems.MOLD_BASE.get()));
        // add.accept(new ItemStack(ModItems.MOLD_BILLET.get()));
        // add.accept(new ItemStack(ModItems.MOLD_BLADE.get()));
        // add.accept(new ItemStack(ModItems.MOLD_BLADES.get()));
        // add.accept(new ItemStack(ModItems.MOLD_BLOCK.get()));
        // add.accept(new ItemStack(ModItems.MOLD_C357.get()));
        // add.accept(new ItemStack(ModItems.MOLD_CBUCKSHOT.get()));
        // add.accept(new ItemStack(ModItems.MOLD_GEM.get()));
        // add.accept(new ItemStack(ModItems.MOLD_GRIP.get()));
        // add.accept(new ItemStack(ModItems.MOLD_HULL_BIG.get()));
        // add.accept(new ItemStack(ModItems.MOLD_HULL_SMALL.get()));
        // add.accept(new ItemStack(ModItems.MOLD_INGOT.get()));
        // add.accept(new ItemStack(ModItems.MOLD_INGOTS.get()));
        // add.accept(new ItemStack(ModItems.MOLD_MECHANISM.get()));
        // add.accept(new ItemStack(ModItems.MOLD_MOGUS.get()));
        // add.accept(new ItemStack(ModItems.MOLD_NUGGET.get()));
        // add.accept(new ItemStack(ModItems.MOLD_PIPE.get()));
        // add.accept(new ItemStack(ModItems.MOLD_PIPES.get()));
        // add.accept(new ItemStack(ModItems.MOLD_PLATE.get()));
        // add.accept(new ItemStack(ModItems.MOLD_PLATE_CAST.get()));
        // add.accept(new ItemStack(ModItems.MOLD_PLATES.get()));
        // add.accept(new ItemStack(ModItems.MOLD_PLATES_CAST.get()));
        // add.accept(new ItemStack(ModItems.MOLD_RECEIVER_HEAVY.get()));
        // add.accept(new ItemStack(ModItems.MOLD_RECEIVER_LIGHT.get()));
        // add.accept(new ItemStack(ModItems.MOLD_SHELL.get()));
        // add.accept(new ItemStack(ModItems.MOLD_STAMP.get()));
        // add.accept(new ItemStack(ModItems.MOLD_STEEL_BASE.get()));
        // add.accept(new ItemStack(ModItems.MOLD_STOCK.get()));
        // add.accept(new ItemStack(ModItems.MOLD_WIRE.get()));
        // add.accept(new ItemStack(ModItems.MOLD_WIRE_DENSE.get()));
        // add.accept(new ItemStack(ModItems.MOLD_WIRES_DENSE.get()));
        // add.accept(new ItemStack(ModItems.PART_LITHIUM.get()));
        // add.accept(new ItemStack(ModItems.PART_BERYLLIUM.get()));
        // add.accept(new ItemStack(ModItems.PART_CARBON.get()));
        // add.accept(new ItemStack(ModItems.PART_COPPER.get()));
        // add.accept(new ItemStack(ModItems.PART_PLUTONIUM.get()));
        // add.accept(new ItemStack(ModBlocks.ZIRNOX.get()));
        // // add.accept(new ItemStack(ModBlocks.MIXER.get()));
        // add.accept(new ItemStack(ModBlocks.RBMK_CONSOLE.get()));
        // add.accept(new ItemStack(ModBlocks.FLARE_STACK.get()));
        // add.accept(new ItemStack(ModBlocks.PUMPJACK.get()));
        // add.accept(new ItemStack(ModBlocks.PUMP_STEAM.get()));
        // add.accept(new ItemStack(ModBlocks.PUMP_ELECTRIC.get()));
        // add.accept(new ItemStack(ModBlocks.CRACKING_TOWER.get()));
        // add.accept(new ItemStack(ModBlocks.FEL.get()));
        // add.accept(new ItemStack(ModBlocks.SILEX.get()));
        // add.accept(new ItemStack(ModBlocks.INDUSTRIAL_BOILER.get()));
        // add.accept(new ItemStack(ModBlocks.SOLAR_BOILER.get()));
        // add.accept(new ItemStack(ModBlocks.SOLAR_MIRROR.get()));
        // add.accept(new ItemStack(ModBlocks.WATZ_POWERPLANT.get()));
        // add.accept(new ItemStack(ModBlocks.HYDROTREATER.get()));
        // add.accept(new ItemStack(ModBlocks.CATALYTIC_REFORMER.get()));
        // add.accept(new ItemStack(ModBlocks.DEUTERIUM_TOWER.get()));
        // add.accept(new ItemStack(ModBlocks.CHEMICAL_FACTORY.get()));
        // add.accept(new ItemStack(ModBlocks.STEAM_TURBINE.get()));
        // add.accept(new ItemStack(ModBlocks.LIQUEFACTOR.get()));
        // add.accept(new ItemStack(ModBlocks.CORE_EMITTER.get()));
        // add.accept(new ItemStack(ModBlocks.CORE_INJECTOR.get()));
        // add.accept(new ItemStack(ModBlocks.CORE_RECEIVER.get()));
        // add.accept(new ItemStack(ModBlocks.VACUUM_DISTILL.get()));
        // add.accept(new ItemStack(ModBlocks.TURBOFAN.get()));
        // add.accept(new ItemStack(ModBlocks.INDUSTRIAL_TURBINE.get()));
        // add.accept(new ItemStack(ModBlocks.SUBSTATION.get()));
        // add.accept(new ItemStack(ModBlocks.CONVERTER_BLOCK.get()));
        // add.accept(new ItemStack(ModBlocks.WIRE_COATED.get()));
        // add.accept(new ItemStack(ModBlocks.DECON.get()));
        // add.accept(new ItemStack(ModBlocks.EMP.get()));

        // // --- WIP Machines (3D OBJ models) ---
        // add.accept(new ItemStack(ModBlocks.AMMO_PRESS.get()));
        // add.accept(new ItemStack(ModBlocks.ANNIHILATOR.get()));
        // add.accept(new ItemStack(ModBlocks.ARC_FURNACE.get()));
        // add.accept(new ItemStack(ModBlocks.ASSEMBLY_FACTORY.get()));
        // add.accept(new ItemStack(ModBlocks.AUTOSAW.get()));
        // add.accept(new ItemStack(ModBlocks.BAT9000.get()));
        // add.accept(new ItemStack(ModBlocks.BEAMLINE.get()));
        // add.accept(new ItemStack(ModBlocks.BOILER.get()));
        // add.accept(new ItemStack(ModBlocks.BOILER_FUSION.get()));
        // add.accept(new ItemStack(ModBlocks.BREEDER_FUSION.get()));
        // add.accept(new ItemStack(ModBlocks.CHIMNEY_BRICK.get()));
        // add.accept(new ItemStack(ModBlocks.CHIMNEY_INDUSTRIAL.get()));
        // add.accept(new ItemStack(ModBlocks.COKER.get()));
        // add.accept(new ItemStack(ModBlocks.COLLECTOR.get()));
        // add.accept(new ItemStack(ModBlocks.COMBINATION_OVEN.get()));
        // add.accept(new ItemStack(ModBlocks.COMBUSTION_ENGINE.get()));
        // add.accept(new ItemStack(ModBlocks.COMPRESSOR.get()));
        // add.accept(new ItemStack(ModBlocks.CONDENSER_POWERED.get()));
        // add.accept(new ItemStack(ModBlocks.LPW2.get()));
        // add.accept(new ItemStack(ModBlocks.CONVEYOR_PRESS.get()));
        // add.accept(new ItemStack(ModBlocks.COUPLER.get()));
        // add.accept(new ItemStack(ModBlocks.DETECTOR.get()));
        // add.accept(new ItemStack(ModBlocks.DIESELGEN.get()));
        // add.accept(new ItemStack(ModBlocks.DIPOLE.get()));
        // add.accept(new ItemStack(ModBlocks.DRONE.get()));
        // add.accept(new ItemStack(ModBlocks.ELECTRIC_HEATER.get()));
        // add.accept(new ItemStack(ModBlocks.ELECTROLYSER.get()));
        // add.accept(new ItemStack(ModBlocks.EPRESS.get()));
        // add.accept(new ItemStack(ModBlocks.EXPOSURE_CHAMBER.get()));
        // add.accept(new ItemStack(ModBlocks.FENSU2.get()));
        // add.accept(new ItemStack(ModBlocks.FIREBOX.get()));
        // add.accept(new ItemStack(ModBlocks.FRACTION_SPACER.get()));
        // add.accept(new ItemStack(ModBlocks.FURNACE_IRON.get()));
        // add.accept(new ItemStack(ModBlocks.FURNACE_STEEL.get()));
        // add.accept(new ItemStack(ModBlocks.HEATEX.get()));
        // add.accept(new ItemStack(ModBlocks.HEPHAESTUS.get()));
        // add.accept(new ItemStack(ModBlocks.ICF.get()));
        // add.accept(new ItemStack(ModBlocks.INTAKE.get()));
        // add.accept(new ItemStack(ModBlocks.KLYSTRON.get()));
        // add.accept(new ItemStack(ModBlocks.KLYSTRON_CREATIVE.get()));
        // add.accept(new ItemStack(ModBlocks.MHDT.get()));
        // add.accept(new ItemStack(ModBlocks.MICROWAVE.get()));
        // add.accept(new ItemStack(ModBlocks.MINING_LASER.get()));
        // add.accept(new ItemStack(ModBlocks.OILBURNER.get()));
        // add.accept(new ItemStack(ModBlocks.OILBURNER_HP.get()));
        // add.accept(new ItemStack(ModBlocks.ORBUS.get()));
        // add.accept(new ItemStack(ModBlocks.ORE_SLOPPER.get()));
        // add.accept(new ItemStack(ModBlocks.PLASMA_FORGE.get()));
        // add.accept(new ItemStack(ModBlocks.PYROOVEN.get()));
        // add.accept(new ItemStack(ModBlocks.QUADRUPOLE.get()));
        // add.accept(new ItemStack(ModBlocks.RADIOLYSIS.get()));
        // add.accept(new ItemStack(ModBlocks.REACTOR_SMALL.get()));
        // add.accept(new ItemStack(ModBlocks.RFC.get()));
        // add.accept(new ItemStack(ModBlocks.ROTARY_FURNACE.get()));
        // add.accept(new ItemStack(ModBlocks.SAWMILL.get()));
        // add.accept(new ItemStack(ModBlocks.SOLIDIFIER.get()));
        // add.accept(new ItemStack(ModBlocks.ASHPIT.get()));
        // add.accept(new ItemStack(ModBlocks.REACTOR_RESEARCH.get()));
        // add.accept(new ItemStack(ModBlocks.SOURCE.get()));
        // add.accept(new ItemStack(ModBlocks.INDUSTRIAL_GENERATOR.get()));
        // add.accept(new ItemStack(ModBlocks.STEAM_ENGINE.get()));
        // add.accept(new ItemStack(ModBlocks.STIRLING.get()));
        // add.accept(new ItemStack(ModBlocks.STIRLING_CREATIVE.get()));
        // add.accept(new ItemStack(ModBlocks.STIRLING_STEEL.get()));
        // add.accept(new ItemStack(ModBlocks.STRAND_CASTER.get()));
        // add.accept(new ItemStack(ModBlocks.THRESHER.get()));
        // add.accept(new ItemStack(ModBlocks.TORUS.get()));
        // add.accept(new ItemStack(ModBlocks.TURBINEGAS.get()));
        // add.accept(new ItemStack(ModBlocks.WATZ_PUMP.get()));
        // add.accept(new ItemStack(ModBlocks.CHUNGUS.get()));

        // // ══════════════════════════════════════════════════════════════════════
        // // DEV: importierte fehlende Bloecke aus dem Original-HBM (zur Sichtung)
        // // ══════════════════════════════════════════════════════════════════════
        // add.accept(new ItemStack(ModBlocks.ANCIENT_SCRAP.get()));
        // add.accept(new ItemStack(ModBlocks.ASH_DIGAMMA.get()));
        // add.accept(new ItemStack(ModBlocks.ASPHALT_LIGHT.get()));
        // add.accept(new ItemStack(ModBlocks.BARBED_WIRE_ACID.get()));
        // add.accept(new ItemStack(ModBlocks.BARBED_WIRE_ULTRADEATH.get()));
        // add.accept(new ItemStack(ModBlocks.BASALT.get()));
        // add.accept(new ItemStack(ModBlocks.BASALT_SMOOTH.get()));
        // add.accept(new ItemStack(ModBlocks.BASALT_TILES.get()));
        // add.accept(new ItemStack(ModBlocks.BATTERY_LITHIUM_BLOCK.get()));
        // add.accept(new ItemStack(ModBlocks.BATTERY_POTATO_BLOCK.get()));
        // add.accept(new ItemStack(ModBlocks.BATTERY_SCHRABIDIUM_BLOCK.get()));
        // add.accept(new ItemStack(ModBlocks.BLAST_DOOR.get()));
        // add.accept(new ItemStack(ModBlocks.BLOCK_ALUMINIUM.get()));
        // add.accept(new ItemStack(ModBlocks.BOXCAR.get()));
        // add.accept(new ItemStack(ModBlocks.BRICK_ASBESTOS.get()));
        // add.accept(new ItemStack(ModBlocks.BRICK_COMPOUND.get()));
        // add.accept(new ItemStack(ModBlocks.BRICK_JUNGLE.get()));
        // add.accept(new ItemStack(ModBlocks.BRICK_JUNGLE_CIRCLE.get()));
        // add.accept(new ItemStack(ModBlocks.BRICK_JUNGLE_CRACKED.get()));
        // add.accept(new ItemStack(ModBlocks.BRICK_JUNGLE_FRAGILE.get()));
        // add.accept(new ItemStack(ModBlocks.BRICK_JUNGLE_GLYPH.get()));
        // add.accept(new ItemStack(ModBlocks.BRICK_JUNGLE_LAVA.get()));
        // add.accept(new ItemStack(ModBlocks.BRICK_JUNGLE_MYSTIC.get()));
        // add.accept(new ItemStack(ModBlocks.BRICK_JUNGLE_OOZE.get()));
        // add.accept(new ItemStack(ModBlocks.BRICK_JUNGLE_TRAP.get()));
        // add.accept(new ItemStack(ModBlocks.BRICK_RED.get()));
        // add.accept(new ItemStack(ModBlocks.CABLE_DETECTOR.get()));
        // add.accept(new ItemStack(ModBlocks.CABLE_DIODE.get()));
        // add.accept(new ItemStack(ModBlocks.CABLE_SWITCH.get()));
        // add.accept(new ItemStack(ModBlocks.CAPACITOR_BUS.get()));
        // add.accept(new ItemStack(ModBlocks.CAPACITOR_COPPER.get()));
        // add.accept(new ItemStack(ModBlocks.CAPACITOR_GOLD.get()));
        // add.accept(new ItemStack(ModBlocks.CAPACITOR_NIOBIUM.get()));
        // add.accept(new ItemStack(ModBlocks.CAPACITOR_SCHRABIDATE.get()));
        // add.accept(new ItemStack(ModBlocks.CAPACITOR_TANTALIUM.get()));
        // add.accept(new ItemStack(ModBlocks.CARGO_ELEVATOR.get()));
        // add.accept(new ItemStack(ModBlocks.CHARGE_C4.get()));
        // add.accept(new ItemStack(ModBlocks.CHARGE_DYNAMITE.get()));
        // add.accept(new ItemStack(ModBlocks.CHARGE_MINER.get()));
        // add.accept(new ItemStack(ModBlocks.CHARGE_SEMTEX.get()));
        // add.accept(new ItemStack(ModBlocks.CHLORINE_GAS.get()));
        // add.accept(new ItemStack(ModBlocks.CLUSTER_ALUMINIUM.get()));
        // add.accept(new ItemStack(ModBlocks.CLUSTER_COPPER.get()));
        // add.accept(new ItemStack(ModBlocks.CLUSTER_DEPTH_IRON.get()));
        // add.accept(new ItemStack(ModBlocks.CLUSTER_DEPTH_TITANIUM.get()));
        // add.accept(new ItemStack(ModBlocks.CLUSTER_DEPTH_TUNGSTEN.get()));
        // add.accept(new ItemStack(ModBlocks.CLUSTER_IRON.get()));
        // add.accept(new ItemStack(ModBlocks.CLUSTER_TITANIUM.get()));
        // add.accept(new ItemStack(ModBlocks.CM_FLUX.get()));
        // add.accept(new ItemStack(ModBlocks.CM_HEAT.get()));
        // add.accept(new ItemStack(ModBlocks.CMB_BRICK.get()));
        // add.accept(new ItemStack(ModBlocks.CMB_BRICK_REINFORCED.get()));
        // add.accept(new ItemStack(ModBlocks.COMPACT_LAUNCHER.get()));
        // add.accept(new ItemStack(ModBlocks.CONCRETE_COLORED_EXT_BRONZE.get()));
        // add.accept(new ItemStack(ModBlocks.CONCRETE_COLORED_EXT_HAZARD.get()));
        // add.accept(new ItemStack(ModBlocks.CONCRETE_COLORED_EXT_INDIGO.get()));
        // add.accept(new ItemStack(ModBlocks.CONCRETE_COLORED_EXT_MACHINE.get()));
        // add.accept(new ItemStack(ModBlocks.CONCRETE_COLORED_EXT_MACHINE_STRIPE.get()));
        // add.accept(new ItemStack(ModBlocks.CONCRETE_COLORED_EXT_PINK.get()));
        // add.accept(new ItemStack(ModBlocks.CONCRETE_COLORED_EXT_PURPLE.get()));
        // add.accept(new ItemStack(ModBlocks.CONCRETE_COLORED_EXT_SAND.get()));
        // add.accept(new ItemStack(ModBlocks.CONVEYOR.get()));
        // add.accept(new ItemStack(ModBlocks.CONVEYOR_DOUBLE.get()));
        // add.accept(new ItemStack(ModBlocks.CONVEYOR_EXPRESS.get()));
        // add.accept(new ItemStack(ModBlocks.CONVEYOR_TRIPLE.get()));
        // add.accept(new ItemStack(ModBlocks.CONVEYOR_LIFT.get()));
        // add.accept(new ItemStack(ModBlocks.CONVEYOR_CHUTE.get()));
        // add.accept(new ItemStack(ModBlocks.CRANE_BOXER.get()));
        // add.accept(new ItemStack(ModBlocks.CRANE_INSERTER.get()));
        // add.accept(new ItemStack(ModBlocks.CRANE_EXTRACTOR.get()));
        // add.accept(new ItemStack(ModBlocks.CRANE_GRABBER.get()));
        // add.accept(new ItemStack(ModBlocks.CRANE_PARTITIONER.get()));
        // add.accept(new ItemStack(ModBlocks.CRANE_ROUTER.get()));
        // add.accept(new ItemStack(ModBlocks.CRANE_SPLITTER.get()));
        // add.accept(new ItemStack(ModBlocks.CRANE_UNBOXER.get()));
        // add.accept(new ItemStack(ModBlocks.CRATE_AMMO.get()));
        // add.accept(new ItemStack(ModBlocks.CRATE_CAN.get()));
        // add.accept(new ItemStack(ModBlocks.CRATE_JUNGLE.get()));
        // add.accept(new ItemStack(ModBlocks.CRATE_RED.get()));
        // add.accept(new ItemStack(ModBlocks.DEPTH_DNT.get()));
        // add.accept(new ItemStack(ModBlocks.DET_CHARGE.get()));
        // add.accept(new ItemStack(ModBlocks.DET_CORD.get()));
        // add.accept(new ItemStack(ModBlocks.DET_NUKE.get()));
        // add.accept(new ItemStack(ModBlocks.DFC_CORE.get()));
        // add.accept(new ItemStack(ModBlocks.DFC_STABILIZER.get()));
        // add.accept(new ItemStack(ModBlocks.DIRT_DEAD.get()));
        // add.accept(new ItemStack(ModBlocks.DIRT_OILY.get()));
        // add.accept(new ItemStack(ModBlocks.DRONE_CRATE.get()));
        // add.accept(new ItemStack(ModBlocks.DRONE_CRATE_PROVIDER.get()));
        // add.accept(new ItemStack(ModBlocks.DRONE_CRATE_REQUESTER.get()));
        // add.accept(new ItemStack(ModBlocks.DRONE_DOCK.get()));
        // add.accept(new ItemStack(ModBlocks.DRONE_WAYPOINT.get()));
        // add.accept(new ItemStack(ModBlocks.DRONE_WAYPOINT_REQUEST.get()));
        // add.accept(new ItemStack(ModBlocks.RADIO_TORCH_SENDER.get()));
        // add.accept(new ItemStack(ModBlocks.RADIO_TORCH_RECEIVER.get()));
        // add.accept(new ItemStack(ModBlocks.RADIO_TORCH_LOGIC.get()));
        // add.accept(new ItemStack(ModBlocks.RADIO_TORCH_READER.get()));
        // add.accept(new ItemStack(ModBlocks.RADIO_TORCH_CONTROLLER.get()));
        // add.accept(new ItemStack(ModBlocks.RADIO_TORCH_COUNTER.get()));
        // add.accept(new ItemStack(ModBlocks.DUCRETE.get()));
        // add.accept(new ItemStack(ModBlocks.DYNAMITE.get()));
        // add.accept(new ItemStack(ModBlocks.FACTORY_ADVANCED_HULL.get()));
        // add.accept(new ItemStack(ModBlocks.FACTORY_TITANIUM_HULL.get()));
        // add.accept(new ItemStack(ModBlocks.FENCE_METAL.get()));
        // add.accept(new ItemStack(ModBlocks.FENCE_METAL_POST.get()));
        // add.accept(new ItemStack(ModBlocks.FIELD_DISTURBER.get()));
        // add.accept(new ItemStack(ModBlocks.FIRE_DIGAMMA.get()));
        // add.accept(new ItemStack(ModBlocks.FIREWORKS.get()));
        // add.accept(new ItemStack(ModBlocks.FISSURE_BOMB.get()));
        // add.accept(new ItemStack(ModBlocks.FLAME_WAR.get()));
        // add.accept(new ItemStack(ModBlocks.FLUID_COUNTER_VALVE.get()));
        // add.accept(new ItemStack(ModBlocks.FLUID_DUCT_BOX.get()));
        // add.accept(new ItemStack(ModBlocks.FLUID_DUCT_EXHAUST.get()));
        // add.accept(new ItemStack(ModBlocks.PIPE_ANCHOR.get()));
        // add.accept(new ItemStack(ModBlocks.FLUID_DUCT_PAINTABLE.get()));
        // add.accept(new ItemStack(ModBlocks.FLUID_SWITCH.get()));
        // add.accept(new ItemStack(ModBlocks.FOUNDRY_MOLD.get()));
        // add.accept(new ItemStack(ModBlocks.FOUNDRY_SLAGTAP.get()));
        // add.accept(new ItemStack(ModBlocks.FOUNDRY_TANK.get()));
        // add.accept(new ItemStack(ModBlocks.FROZEN_DIRT.get()));
        // add.accept(new ItemStack(ModBlocks.FROZEN_GRASS.get()));
        // add.accept(new ItemStack(ModBlocks.FROZEN_LOG.get()));
        // add.accept(new ItemStack(ModBlocks.FROZEN_PLANKS.get()));
        // add.accept(new ItemStack(ModBlocks.FUSION_COMPONENT.get()));
        // add.accept(new ItemStack(ModBlocks.FUSION_COMPONENT_BLANKET.get()));
        // add.accept(new ItemStack(ModBlocks.FUSION_COMPONENT_BSCCO_WELDED.get()));
        // add.accept(new ItemStack(ModBlocks.FUSION_COMPONENT_MOTOR.get()));
        // add.accept(new ItemStack(ModBlocks.FUSION_HATCH.get()));
        // add.accept(new ItemStack(ModBlocks.FUSION_HEATER.get()));
        // add.accept(new ItemStack(ModBlocks.GAS_ASBESTOS.get()));
        // add.accept(new ItemStack(ModBlocks.GAS_COAL.get()));
        // add.accept(new ItemStack(ModBlocks.GAS_EXPLOSIVE.get()));
        // add.accept(new ItemStack(ModBlocks.GAS_FLAMMABLE.get()));
        // add.accept(new ItemStack(ModBlocks.GAS_MELTDOWN.get()));
        // add.accept(new ItemStack(ModBlocks.GAS_MONOXIDE.get()));
        // add.accept(new ItemStack(ModBlocks.GAS_RADON.get()));
        // add.accept(new ItemStack(ModBlocks.GAS_RADON_DENSE.get()));
        // add.accept(new ItemStack(ModBlocks.GAS_RADON_TOMB.get()));
        // add.accept(new ItemStack(ModBlocks.GLASS_ASH.get()));
        // add.accept(new ItemStack(ModBlocks.GLASS_BORON.get()));
        // add.accept(new ItemStack(ModBlocks.GLASS_LEAD.get()));
        // add.accept(new ItemStack(ModBlocks.GLASS_POLARIZED.get()));
        // add.accept(new ItemStack(ModBlocks.GLASS_POLONIUM.get()));
        // add.accept(new ItemStack(ModBlocks.GLASS_QUARTZ.get()));
        // add.accept(new ItemStack(ModBlocks.GLASS_TRINITITE.get()));
        // add.accept(new ItemStack(ModBlocks.GLASS_URANIUM.get()));
        add.accept(new ItemStack(ModBlocks.GLYPHID_BASE.get()));
        add.accept(new ItemStack(ModBlocks.GLYPHID_SPAWNER.get()));
        // add.accept(new ItemStack(ModBlocks.GRAVEL_DIAMOND.get()));
        // add.accept(new ItemStack(ModBlocks.GRAVEL_OBSIDIAN.get()));
        // add.accept(new ItemStack(ModBlocks.HADRON_COIL_ALLOY.get()));
        // add.accept(new ItemStack(ModBlocks.HADRON_COIL_CHLOROPHYTE.get()));
        // add.accept(new ItemStack(ModBlocks.HADRON_COIL_GOLD.get()));
        // add.accept(new ItemStack(ModBlocks.HADRON_COIL_MAGTUNG.get()));
        // add.accept(new ItemStack(ModBlocks.HADRON_COIL_MESE.get()));
        // add.accept(new ItemStack(ModBlocks.HADRON_COIL_NEODYMIUM.get()));
        // add.accept(new ItemStack(ModBlocks.HADRON_COIL_SCHRABIDATE.get()));
        // add.accept(new ItemStack(ModBlocks.HADRON_COIL_SCHRABIDIUM.get()));
        // add.accept(new ItemStack(ModBlocks.HADRON_COIL_STARMETAL.get()));
        // add.accept(new ItemStack(ModBlocks.HEV_BATTERY.get()));
        // add.accept(new ItemStack(ModBlocks.ICF_COMPONENT.get()));
        // add.accept(new ItemStack(ModBlocks.ICF_COMPONENT_STRUCTURE.get()));
        // add.accept(new ItemStack(ModBlocks.ICF_COMPONENT_STRUCTURE_BOLTED.get()));
        // add.accept(new ItemStack(ModBlocks.ICF_COMPONENT_VESSEL.get()));
        // add.accept(new ItemStack(ModBlocks.ICF_COMPONENT_VESSEL_WELDED.get()));
        // add.accept(new ItemStack(ModBlocks.ICF_CONTROLLER.get()));
        // add.accept(new ItemStack(ModBlocks.ITER.get()));
        // add.accept(new ItemStack(ModBlocks.LADDER_ALUMINIUM.get()));
        // add.accept(new ItemStack(ModBlocks.LADDER_COBALT.get()));
        // add.accept(new ItemStack(ModBlocks.LADDER_COPPER.get()));
        // add.accept(new ItemStack(ModBlocks.LADDER_GOLD.get()));
        // add.accept(new ItemStack(ModBlocks.LADDER_IRON.get()));
        // add.accept(new ItemStack(ModBlocks.LADDER_LEAD.get()));
        // add.accept(new ItemStack(ModBlocks.LADDER_STEEL.get()));
        // add.accept(new ItemStack(ModBlocks.LADDER_STURDY.get()));
        // add.accept(new ItemStack(ModBlocks.LADDER_TITANIUM.get()));
        // add.accept(new ItemStack(ModBlocks.LADDER_TUNGSTEN.get()));
        // add.accept(new ItemStack(ModBlocks.LAMP_DEMON.get()));
        // add.accept(new ItemStack(ModBlocks.LAMP_TRITIUM_BLUE_OFF.get()));
        // add.accept(new ItemStack(ModBlocks.LAMP_TRITIUM_BLUE_ON.get()));
        // add.accept(new ItemStack(ModBlocks.LAMP_TRITIUM_GREEN_OFF.get()));
        // add.accept(new ItemStack(ModBlocks.LAMP_TRITIUM_GREEN_ON.get()));
        // add.accept(new ItemStack(ModBlocks.LIGHTSTONE_BRICKS.get()));
        // add.accept(new ItemStack(ModBlocks.LIGHTSTONE_BRICKS_CHISELED.get()));
        // add.accept(new ItemStack(ModBlocks.LIGHTSTONE_CHISELED.get()));
        // add.accept(new ItemStack(ModBlocks.LIGHTSTONE_TILE.get()));
        // add.accept(new ItemStack(ModBlocks.LIGHTSTONE_UNREFINED.get()));
        // add.accept(new ItemStack(ModBlocks.MACHINE_AUTOCRAFTER.get()));
        // add.accept(new ItemStack(ModBlocks.MACHINE_CHUNGUS.get()));
        // add.accept(new ItemStack(ModBlocks.MACHINE_CONTROLLER.get()));
        // add.accept(new ItemStack(ModBlocks.MACHINE_FENSU.get()));
        // add.accept(new ItemStack(ModBlocks.MACHINE_FORCEFIELD.get()));
        // add.accept(new ItemStack(ModBlocks.MACHINE_FUNNEL.get()));
        // add.accept(new ItemStack(ModBlocks.MACHINE_ICF_PRESS.get()));
        // add.accept(new ItemStack(ModBlocks.MACHINE_KEYFORGE.get()));
        // add.accept(new ItemStack(ModBlocks.MACHINE_LARGE_TURBINE.get()));
        // add.accept(new ItemStack(ModBlocks.MACHINE_MISSILE_ASSEMBLY.get()));
        // add.accept(new ItemStack(ModBlocks.MACHINE_PUF6_TANK.get()));
        // add.accept(new ItemStack(ModBlocks.MACHINE_DRAIN.get()));
        // add.accept(new ItemStack(ModBlocks.MACHINE_TRANSFORMER.get()));
        // add.accept(new ItemStack(ModBlocks.MACHINE_WASTE_DRUM.get()));
        // add.accept(new ItemStack(ModBlocks.MACHINE_RADGEN.get()));
        // add.accept(new ItemStack(ModBlocks.MACHINE_SATLINKER.get()));
        // add.accept(new ItemStack(ModBlocks.MACHINE_STORAGE_DRUM.get()));
        // add.accept(new ItemStack(ModBlocks.MACHINE_UF6_TANK.get()));
        // add.accept(new ItemStack(ModBlocks.MASS_STORAGE.get()));
        // add.accept(new ItemStack(ModBlocks.METEOR_SPAWNER.get()));
        // add.accept(new ItemStack(ModBlocks.MINE_HE.get()));
        // add.accept(new ItemStack(ModBlocks.MINE_NAVAL.get()));
        // add.accept(new ItemStack(ModBlocks.MINE_SHRAP.get()));
        // add.accept(new ItemStack(ModBlocks.MOON_TURF.get()));
        // add.accept(new ItemStack(ModBlocks.MUSH.get()));
        // add.accept(new ItemStack(ModBlocks.NUKE_FSTBMB.get()));
        // add.accept(new ItemStack(ModBlocks.NUKE_N2.get()));
        // add.accept(new ItemStack(ModBlocks.NUKE_SOLINIUM.get()));
        // add.accept(new ItemStack(ModBlocks.OIL_SPILL.get()));
        // add.accept(new ItemStack(ModBlocks.PEDESTAL.get()));
        // add.accept(new ItemStack(ModBlocks.PINK_LOG.get()));
        // add.accept(new ItemStack(ModBlocks.PINK_PLANKS.get()));
        // add.accept(new ItemStack(ModBlocks.PLANT_FLOWER_CD0.get()));
        // add.accept(new ItemStack(ModBlocks.PLANT_FLOWER_CD1.get()));
        // add.accept(new ItemStack(ModBlocks.PLANT_FLOWER_FOXGLOVE.get()));
        // add.accept(new ItemStack(ModBlocks.PLANT_FLOWER_NIGHTSHADE.get()));
        // add.accept(new ItemStack(ModBlocks.PLANT_FLOWER_TOBACCO.get()));
        // add.accept(new ItemStack(ModBlocks.PLANT_FLOWER_WEED.get()));
        // add.accept(new ItemStack(ModBlocks.PLASMA_HEATER.get()));
        // add.accept(new ItemStack(ModBlocks.PNEUMATIC_TUBE.get()));
        // add.accept(new ItemStack(ModBlocks.PNEUMATIC_TUBE_PAINTABLE.get()));
        // add.accept(new ItemStack(ModBlocks.PRESS_PREHEATER.get()));
        // add.accept(new ItemStack(ModBlocks.PWR_BLOCK.get()));
        // add.accept(new ItemStack(ModBlocks.PWR_CASING.get()));
        // add.accept(new ItemStack(ModBlocks.PWR_CHANNEL.get()));
        // add.accept(new ItemStack(ModBlocks.PWR_CONTROL.get()));
        // add.accept(new ItemStack(ModBlocks.PWR_CONTROLLER.get()));
        // add.accept(new ItemStack(ModBlocks.PWR_FUEL.get()));
        // add.accept(new ItemStack(ModBlocks.PWR_HEATEX.get()));
        // add.accept(new ItemStack(ModBlocks.PWR_HEATSINK.get()));
        // add.accept(new ItemStack(ModBlocks.PWR_NEUTRON_SOURCE.get()));
        // add.accept(new ItemStack(ModBlocks.PWR_PORT.get()));
        // add.accept(new ItemStack(ModBlocks.PWR_REFLECTOR.get()));
        // add.accept(new ItemStack(ModBlocks.RADIO_TELEX.get()));
        // add.accept(new ItemStack(ModBlocks.RADIOBOX.get()));
        // add.accept(new ItemStack(ModBlocks.RADIOREC.get()));
        // add.accept(new ItemStack(ModBlocks.RAIL_BOOSTER.get()));
        // add.accept(new ItemStack(ModBlocks.RAIL_HIGHSPEED.get()));
        // add.accept(new ItemStack(ModBlocks.RAIL_NARROW.get()));
        // add.accept(new ItemStack(ModBlocks.RAIL_WOOD.get()));
        // add.accept(new ItemStack(ModBlocks.RED_CABLE.get()));
        // add.accept(new ItemStack(ModBlocks.RED_CABLE_CLASSIC.get()));
        // add.accept(new ItemStack(ModBlocks.RED_CABLE_PAINTABLE.get()));
        // add.accept(new ItemStack(ModBlocks.RED_CABLE_GAUGE.get()));
        // add.accept(new ItemStack(ModBlocks.RED_CABLE_BOX.get()));
        // add.accept(new ItemStack(ModBlocks.RED_CONNECTOR.get()));
        // add.accept(new ItemStack(ModBlocks.RED_CONNECTOR_SUPER.get()));
        // add.accept(new ItemStack(ModBlocks.RED_PYLON.get()));
        // add.accept(new ItemStack(ModBlocks.RED_PYLON_STEEL.get()));
        // add.accept(new ItemStack(ModBlocks.RED_PYLON_MEDIUM_WOOD.get()));
        // add.accept(new ItemStack(ModBlocks.RED_PYLON_MEDIUM_WOOD_TRANSFORMER.get()));
        // add.accept(new ItemStack(ModBlocks.RED_PYLON_MEDIUM_STEEL.get()));
        // add.accept(new ItemStack(ModBlocks.RED_PYLON_MEDIUM_STEEL_TRANSFORMER.get()));
        // add.accept(new ItemStack(ModBlocks.RED_PYLON_LARGE.get()));
        // add.accept(new ItemStack(ModBlocks.RED_WIRE_COATED.get()));
        // add.accept(new ItemStack(ModBlocks.REINFORCED_BRICK.get()));
        // add.accept(new ItemStack(ModBlocks.REINFORCED_DUCRETE.get()));
        // add.accept(new ItemStack(ModBlocks.REINFORCED_GLASS_PANE.get()));
        // add.accept(new ItemStack(ModBlocks.REINFORCED_LAMINATE.get()));
        // add.accept(new ItemStack(ModBlocks.REINFORCED_LAMINATE_PANE.get()));
        // add.accept(new ItemStack(ModBlocks.REINFORCED_LAMP_OFF.get()));
        // add.accept(new ItemStack(ModBlocks.REINFORCED_LAMP_ON.get()));
        // add.accept(new ItemStack(ModBlocks.REINFORCED_LIGHT.get()));
        // add.accept(new ItemStack(ModBlocks.REINFORCED_SAND.get()));
        // add.accept(new ItemStack(ModBlocks.SAFE.get()));
        // add.accept(new ItemStack(ModBlocks.SAND_BORON.get()));
        // add.accept(new ItemStack(ModBlocks.SAND_DIRTY.get()));
        // add.accept(new ItemStack(ModBlocks.SAND_DIRTY_RED.get()));
        // add.accept(new ItemStack(ModBlocks.SAND_LEAD.get()));
        // add.accept(new ItemStack(ModBlocks.SAND_POLONIUM.get()));
        // add.accept(new ItemStack(ModBlocks.SAND_QUARTZ.get()));
        // add.accept(new ItemStack(ModBlocks.SAND_URANIUM.get()));
        // add.accept(new ItemStack(ModBlocks.SANDBAGS.get()));
        // add.accept(new ItemStack(ModBlocks.SAT_DOCK.get()));
        // add.accept(new ItemStack(ModBlocks.SAT_FOEQ.get()));
        // add.accept(new ItemStack(ModBlocks.SAT_SCANNER.get()));
        // add.accept(new ItemStack(ModBlocks.SEAL_CONTROLLER.get()));
        // add.accept(new ItemStack(ModBlocks.SEAL_FRAME.get()));
        // add.accept(new ItemStack(ModBlocks.SEAL_HATCH.get()));
        // add.accept(new ItemStack(ModBlocks.SEMTEX.get()));
        // add.accept(new ItemStack(ModBlocks.SOYUZ_CAPSULE.get()));
        // add.accept(new ItemStack(ModBlocks.SOYUZ_LAUNCHER.get()));
        // add.accept(new ItemStack(ModBlocks.DECO_SOYUZ_ROCKET.get()));
        // add.accept(new ItemStack(ModBlocks.SPIKES.get()));
        // add.accept(new ItemStack(ModBlocks.STALACTITE_ASBESTOS.get()));
        // add.accept(new ItemStack(ModBlocks.STALACTITE_SULFUR.get()));
        // add.accept(new ItemStack(ModBlocks.STALAGMITE_ASBESTOS.get()));
        // add.accept(new ItemStack(ModBlocks.STALAGMITE_SULFUR.get()));
        // add.accept(new ItemStack(ModBlocks.STEEL_ROOF.get()));
        // add.accept(new ItemStack(ModBlocks.STEEL_SCAFFOLD.get()));
        // add.accept(new ItemStack(ModBlocks.STONE_CRACKED.get()));
        // add.accept(new ItemStack(ModBlocks.STONE_DEPTH.get()));
        // add.accept(new ItemStack(ModBlocks.STONE_DEPTH_NETHER.get()));
        // add.accept(new ItemStack(ModBlocks.STONE_GNEISS.get()));
        // add.accept(new ItemStack(ModBlocks.STONE_KEYHOLE.get()));
        // add.accept(new ItemStack(ModBlocks.STONE_KEYHOLE_META.get()));
        // add.accept(new ItemStack(ModBlocks.STONE_POROUS.get()));
        // add.accept(new ItemStack(ModBlocks.STONE_RESOURCE_ASBESTOS.get()));
        // add.accept(new ItemStack(ModBlocks.STONE_RESOURCE_BAUXITE.get()));
        // add.accept(new ItemStack(ModBlocks.STONE_RESOURCE_HEMATITE.get()));
        // add.accept(new ItemStack(ModBlocks.STONE_RESOURCE_LIMESTONE.get()));
        // add.accept(new ItemStack(ModBlocks.STONE_RESOURCE_MALACHITE.get()));
        // add.accept(new ItemStack(ModBlocks.STONE_RESOURCE_SULFUR.get()));
        // add.accept(new ItemStack(ModBlocks.STRUCT_ICF_CORE.get()));
        // add.accept(new ItemStack(ModBlocks.STRUCT_LAUNCHER.get()));
        // add.accept(new ItemStack(ModBlocks.STRUCT_LAUNCHER_CORE.get()));
        // add.accept(new ItemStack(ModBlocks.STRUCT_LAUNCHER_CORE_LARGE.get()));
        // add.accept(new ItemStack(ModBlocks.STRUCT_SCAFFOLD.get()));
        // add.accept(new ItemStack(ModBlocks.STRUCT_SOYUZ_CORE.get()));
        // add.accept(new ItemStack(ModBlocks.STRUCT_TORUS_CORE.get()));
        // add.accept(new ItemStack(ModBlocks.STRUCT_WATZ_CORE.get()));
        // add.accept(new ItemStack(ModBlocks.WATZ_END.get()));
        // add.accept(new ItemStack(ModBlocks.WATZ_END_BOLTED.get()));
        // add.accept(new ItemStack(ModBlocks.TEKTITE.get()));
        // add.accept(new ItemStack(ModBlocks.TESLA.get()));
        // add.accept(new ItemStack(ModBlocks.THERM_ENDO.get()));
        // add.accept(new ItemStack(ModBlocks.THERM_EXO.get()));
        // add.accept(new ItemStack(ModBlocks.TILE_LAB.get()));
        // add.accept(new ItemStack(ModBlocks.TILE_LAB_BROKEN.get()));
        // add.accept(new ItemStack(ModBlocks.TILE_LAB_CRACKED.get()));
        // add.accept(new ItemStack(ModBlocks.TRAPDOOR_STEEL.get()));
        // add.accept(new ItemStack(ModBlocks.VACUUM.get()));
        // add.accept(new ItemStack(ModBlocks.VENT_CHLORINE.get()));
        // add.accept(new ItemStack(ModBlocks.VENT_CHLORINE_SEAL.get()));
        // add.accept(new ItemStack(ModBlocks.VENT_CLOUD.get()));
        // add.accept(new ItemStack(ModBlocks.VENT_PINK_CLOUD.get()));
        // add.accept(new ItemStack(ModBlocks.VINE_PHOSPHOR.get()));
        // add.accept(new ItemStack(ModBlocks.VOLCANO_CORE.get()));
        // add.accept(new ItemStack(ModBlocks.VOLCANO_RAD_CORE.get()));
        // add.accept(new ItemStack(ModBlocks.WAND_AIR.get()));
        // add.accept(new ItemStack(ModBlocks.WAND_JIGSAW.get()));
        // add.accept(new ItemStack(ModBlocks.WAND_LOGIC.get()));
        // add.accept(new ItemStack(ModBlocks.WAND_LOOT.get()));
        // add.accept(new ItemStack(ModBlocks.WASTE_EARTH.get()));
        // add.accept(new ItemStack(ModBlocks.WASTE_MYCELIUM.get()));
        // add.accept(new ItemStack(ModBlocks.WASTE_TRINITITE.get()));
        // add.accept(new ItemStack(ModBlocks.WASTE_TRINITITE_RED.get()));
        // add.accept(new ItemStack(ModBlocks.WATZ_COOLER.get()));
        // add.accept(new ItemStack(ModBlocks.WATZ_ELEMENT.get()));
        // add.accept(new ItemStack(ModBlocks.WOOD_BARRIER.get()));
        // // ══════════════════════════════════════════════════════════════════════
        // // DEV: importierte fehlende Items aus dem Original-HBM (zur Sichtung)
        // // ══════════════════════════════════════════════════════════════════════
        // add.accept(new ItemStack(ModItems.ACETYLENE_TORCH.get()));
        // add.accept(new ItemStack(ModItems.AJR_LEGGINGS.get()));
        // add.accept(new ItemStack(ModItems.AJR_CHESTPLATE.get()));
        // add.accept(new ItemStack(ModItems.AJRO_LEGGINGS.get()));
        // add.accept(new ItemStack(ModItems.AJRO_CHESTPLATE.get()));
        // add.accept(new ItemStack(ModItems.ALLOY_LEGGINGS.get()));
        // add.accept(new ItemStack(ModItems.ALLOY_CHESTPLATE.get()));
        // add.accept(new ItemStack(ModItems.AMMO_ARTY.get()));
        // add.accept(new ItemStack(ModItems.AMMO_ARTY_CARGO.get()));
        // add.accept(new ItemStack(ModItems.AMMO_ARTY_CHLORINE.get()));
        // add.accept(new ItemStack(ModItems.AMMO_ARTY_CLASSIC.get()));
        // add.accept(new ItemStack(ModItems.AMMO_ARTY_HE.get()));
        // add.accept(new ItemStack(ModItems.AMMO_ARTY_MINI_NUKE.get()));
        // add.accept(new ItemStack(ModItems.AMMO_ARTY_MINI_NUKE_MULTI.get()));
        // add.accept(new ItemStack(ModItems.AMMO_ARTY_MUSTARD_GAS.get()));
        // add.accept(new ItemStack(ModItems.AMMO_ARTY_NUKE.get()));
        // add.accept(new ItemStack(ModItems.AMMO_ARTY_PHOSGENE.get()));
        // add.accept(new ItemStack(ModItems.AMMO_ARTY_PHOSPHORUS.get()));
        // add.accept(new ItemStack(ModItems.AMMO_ARTY_PHOSPHORUS_MULTI.get()));
        // add.accept(new ItemStack(ModItems.AMMO_BAG.get()));
        // add.accept(new ItemStack(ModItems.AMMO_BAG_INFINITE.get()));
        // add.accept(new ItemStack(ModItems.AMMO_CONTAINER.get()));
        // add.accept(new ItemStack(ModItems.AMMO_DGK.get()));
        // add.accept(new ItemStack(ModItems.AMMO_FIREEXT.get()));
        // add.accept(new ItemStack(ModItems.AMMO_FIREEXT_FOAM.get()));
        // add.accept(new ItemStack(ModItems.AMMO_FIREEXT_SAND.get()));
        // add.accept(new ItemStack(ModItems.AMMO_SHELL.get()));
        // add.accept(new ItemStack(ModItems.AMMO_SHELL_APFSDS_DU.get()));
        // add.accept(new ItemStack(ModItems.AMMO_SHELL_APFSDS_T.get()));
        // add.accept(new ItemStack(ModItems.AMMO_SHELL_EXPLOSIVE.get()));
        // add.accept(new ItemStack(ModItems.AMMO_SHELL_W9.get()));
        // add.accept(new ItemStack(ModItems.AMS_CATALYST_ALUMINIUM.get()));
        // add.accept(new ItemStack(ModItems.AMS_CATALYST_BERYLLIUM.get()));
        // add.accept(new ItemStack(ModItems.AMS_CATALYST_BLANK.get()));
        // add.accept(new ItemStack(ModItems.AMS_CATALYST_CAESIUM.get()));
        // add.accept(new ItemStack(ModItems.AMS_CATALYST_CERIUM.get()));
        // add.accept(new ItemStack(ModItems.AMS_CATALYST_COBALT.get()));
        // add.accept(new ItemStack(ModItems.AMS_CATALYST_COPPER.get()));
        // add.accept(new ItemStack(ModItems.AMS_CATALYST_DINEUTRONIUM.get()));
        // add.accept(new ItemStack(ModItems.AMS_CATALYST_EUPHEMIUM.get()));
        // add.accept(new ItemStack(ModItems.AMS_CATALYST_IRON.get()));
        // add.accept(new ItemStack(ModItems.AMS_CATALYST_LITHIUM.get()));
        // add.accept(new ItemStack(ModItems.AMS_CATALYST_NIOBIUM.get()));
        // add.accept(new ItemStack(ModItems.AMS_CATALYST_SCHRABIDIUM.get()));
        // add.accept(new ItemStack(ModItems.AMS_CATALYST_STRONTIUM.get()));
        // add.accept(new ItemStack(ModItems.AMS_CATALYST_THORIUM.get()));
        // add.accept(new ItemStack(ModItems.AMS_CATALYST_TUNGSTEN.get()));
        // add.accept(new ItemStack(ModItems.AMS_CORE_EYEOFHARMONY.get()));
        // add.accept(new ItemStack(ModItems.AMS_CORE_SING.get()));
        // add.accept(new ItemStack(ModItems.AMS_CORE_THINGY.get()));
        // add.accept(new ItemStack(ModItems.AMS_CORE_WORMHOLE.get()));
        // add.accept(new ItemStack(ModItems.AMS_LENS.get()));
        // add.accept(new ItemStack(ModItems.ANALYSIS_TOOL.get()));
        // add.accept(new ItemStack(ModItems.ANALYZER.get()));
        // add.accept(new ItemStack(ModItems.ANCHOR_REMOTE.get()));
        // add.accept(new ItemStack(ModItems.APPLE_EUPHEMIUM.get()));
        // add.accept(new ItemStack(ModItems.APPLE_LEAD.get()));
        // add.accept(new ItemStack(ModItems.APPLE_SCHRABIDIUM.get()));
        // add.accept(new ItemStack(ModItems.ARC_ELECTRODE.get()));
        // add.accept(new ItemStack(ModItems.ARMOR_POLISH.get()));
        // add.accept(new ItemStack(ModItems.ASBESTOS_LEGS.get()));
        // add.accept(new ItemStack(ModItems.ASBESTOS_PLATE.get()));
        // add.accept(new ItemStack(ModItems.ASHGLASSES.get()));
        // add.accept(new ItemStack(ModItems.ASSEMBLY_NUKE.get()));
        // add.accept(new ItemStack(ModItems.ATTACHMENT_MASK.get()));
        // add.accept(new ItemStack(ModItems.ATTACHMENT_MASK_MONO.get()));
        // add.accept(new ItemStack(ModItems.AUSTRALIUM_III.get()));
        // add.accept(new ItemStack(ModItems.BACK_TESLA.get()));
        // add.accept(new ItemStack(ModItems.BALEFIRE_AND_HAM.get()));
        // add.accept(new ItemStack(ModItems.BALEFIRE_AND_STEEL.get()));
        // add.accept(new ItemStack(ModItems.BALEFIRE_SCRAMBLED.get()));
        // add.accept(new ItemStack(ModItems.BALL_DYNAMITE.get()));
        // add.accept(new ItemStack(ModItems.BALL_FIRECLAY.get()));
        // add.accept(new ItemStack(ModItems.BALL_RESIN.get()));
        // add.accept(new ItemStack(ModItems.BALL_TATB.get()));
        // add.accept(new ItemStack(ModItems.BALLISTIC_GAUNTLET.get()));
        // add.accept(new ItemStack(ModItems.BALLISTITE.get()));
        // add.accept(new ItemStack(ModItems.BANDAID.get()));
        // add.accept(new ItemStack(ModItems.BATHWATER.get()));
        // add.accept(new ItemStack(ModItems.BATHWATER_MK2.get()));
        // add.accept(new ItemStack(ModItems.BDCL.get()));
        // add.accept(new ItemStack(ModItems.BEDROCK_ORE_FRAGMENT.get()));
        // add.accept(new ItemStack(ModItems.BETA.get()));
        // add.accept(new ItemStack(ModItems.BIG_SWORD.get()));
        // add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.ACTINIUM, MaterialShape.BILLET)));
        // add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.AM241, MaterialShape.BILLET)));
        // add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.AM242, MaterialShape.BILLET)));
        // add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.AM_MIX, MaterialShape.BILLET)));
        // add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.AMERICIUM_FUEL, MaterialShape.BILLET)));
        // add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.AU198, MaterialShape.BILLET)));
        // add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.AUSTRALIUM, MaterialShape.BILLET)));
        // add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.AUSTRALIUM_GREATER, MaterialShape.BILLET)));
        // add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.AUSTRALIUM_LESSER, MaterialShape.BILLET)));
        // add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.BALEFIRE_GOLD, MaterialShape.BILLET)));
        // add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.BERYLLIUM, MaterialShape.BILLET)));
        // add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.BISMUTH, MaterialShape.BILLET)));
        // add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.CO60, MaterialShape.BILLET)));
        // add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.COBALT, MaterialShape.BILLET)));
        // add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.FLASHLEAD, MaterialShape.BILLET)));
        // add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.GH336, MaterialShape.BILLET)));
        // add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.HES, MaterialShape.BILLET)));
        // add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.LES_FUEL, MaterialShape.BILLET)));
        // add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.MOX_FUEL, MaterialShape.BILLET)));
        // add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.NEPTUNIUM, MaterialShape.BILLET)));
        // add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.NEPTUNIUM_FUEL, MaterialShape.BILLET)));
        // add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.NUCLEAR_WASTE, MaterialShape.BILLET)));
        // add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.PB209, MaterialShape.BILLET)));
        // add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.PLUTONIUM_FUEL, MaterialShape.BILLET)));
        // add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.PO210BE, MaterialShape.BILLET)));
        // add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.POLONIUM, MaterialShape.BILLET)));
        // add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.PLUTONIUM238, MaterialShape.BILLET)));
        // add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.PU238BE, MaterialShape.BILLET)));
        // add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.PLUTONIUM239, MaterialShape.BILLET)));
        // add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.PLUTONIUM240, MaterialShape.BILLET)));
        // add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.PLUTONIUM241, MaterialShape.BILLET)));
        // add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.PU_MIX, MaterialShape.BILLET)));
        // add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.RA226, MaterialShape.BILLET)));
        // add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.RA226BE, MaterialShape.BILLET)));
        // add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.SCHRABIDIUM, MaterialShape.BILLET)));
        // add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.SCHRABIDIUM_FUEL, MaterialShape.BILLET)));
        // add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.SOLINIUM, MaterialShape.BILLET)));
        // add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.SR90, MaterialShape.BILLET)));
        // add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.TECHNETIUM, MaterialShape.BILLET)));
        // add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.THORIUM232, MaterialShape.BILLET)));
        // add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.THORIUM_FUEL, MaterialShape.BILLET)));
        // add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.URANIUM233, MaterialShape.BILLET)));
        // add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.URANIUM235, MaterialShape.BILLET)));
        // add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.URANIUM238, MaterialShape.BILLET)));
        // add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.URANIUM, MaterialShape.BILLET)));
        // add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.URANIUM_FUEL, MaterialShape.BILLET)));
        // add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.UZH, MaterialShape.BILLET)));
        // add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.YHARONITE, MaterialShape.BILLET)));
        // add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.ZFB_AM_MIX, MaterialShape.BILLET)));
        // add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.ZFB_BISMUTH, MaterialShape.BILLET)));
        // add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.ZFB_PU241, MaterialShape.BILLET)));
        // add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.ZIRCONIUM, MaterialShape.BILLET)));
        // add.accept(new ItemStack(ModItems.BIO_WAFER.get()));
        // add.accept(new ItemStack(ModItems.BIOMASS.get()));
        // add.accept(new ItemStack(ModItems.BIOMASS_COMPRESSED.get()));
        // add.accept(new ItemStack(ModItems.BISMUTH_AXE.get()));
        // add.accept(new ItemStack(ModItems.BISMUTH_LEGGINGS.get()));
        // add.accept(new ItemStack(ModItems.BISMUTH_PICKAXE.get()));
        // add.accept(new ItemStack(ModItems.BISMUTH_CHESTPLATE.get()));
        // add.accept(new ItemStack(ModItems.BISMUTH_TOOL.get()));
        // add.accept(new ItemStack(ModItems.BJ_BOOTS.get()));
        // add.accept(new ItemStack(ModItems.BJ_HELMET.get()));
        // add.accept(new ItemStack(ModItems.BJ_LEGS.get()));
        // add.accept(new ItemStack(ModItems.BJ_PLATE.get()));
        // add.accept(new ItemStack(ModItems.BJ_PLATE_JETPACK.get()));
        // add.accept(new ItemStack(ModItems.BLADE_METEORITE.get()));
        // add.accept(new ItemStack(ModItems.BLADE_TUNGSTEN.get()));
        // add.accept(new ItemStack(ModItems.BLADES_ADVANCED_ALLOY.get()));
        // add.accept(new ItemStack(ModItems.BLADES_DESH.get()));
        // add.accept(new ItemStack(ModItems.BLADES_STEEL.get()));
        // add.accept(new ItemStack(ModItems.BLADES_TITANIUM.get()));
        // add.accept(new ItemStack(ModItems.BLOWTORCH.get()));
        // add.accept(new ItemStack(ModItems.BLUEPRINTS.get()));
        // add.accept(new ItemStack(ModItems.BOARD_COPPER.get()));
        // add.accept(new ItemStack(ModItems.BOAT_RUBBER.get()));
        // add.accept(new ItemStack(ModItems.BOBMAZON.get()));
        // add.accept(new ItemStack(ModItems.BOLT_SPIKE.get()));
        // add.accept(new ItemStack(ModItems.BOLTGUN.get()));
        // add.accept(new ItemStack(ModItems.BOMB_CALLER.get()));
        // add.accept(new ItemStack(ModItems.BOMB_CALLER_NAPALM.get()));
        // add.accept(new ItemStack(ModItems.BOMB_CALLER_CHLORINE.get()));
        // add.accept(new ItemStack(ModItems.BOMB_CALLER_ATOMIC.get()));
        // add.accept(new ItemStack(ModItems.BOMB_WAFFLE.get()));
        // add.accept(new ItemStack(ModItems.BOOK_GUIDE.get()));
        // add.accept(new ItemStack(ModItems.BOOK_LEMEGETON.get()));
        // add.accept(new ItemStack(ModItems.BOOK_OF_.get()));
        // add.accept(new ItemStack(ModItems.BOOK_SECRET.get()));
        // add.accept(new ItemStack(ModItems.BOTTLE2_EMPTY.get()));
        // add.accept(new ItemStack(ModItems.BOTTLE2_FRITZ.get()));
        // add.accept(new ItemStack(ModItems.BOTTLE2_KORL.get()));
        // add.accept(new ItemStack(ModItems.BOTTLE2_SUNSET.get()));
        // add.accept(new ItemStack(ModItems.BOTTLE_CHERRY.get()));
        // add.accept(new ItemStack(ModItems.BOTTLE_EMPTY.get()));
        // add.accept(new ItemStack(ModItems.BOTTLE_MERCURY.get()));
        // add.accept(new ItemStack(ModItems.BOTTLE_NUKA.get()));
        // add.accept(new ItemStack(ModItems.BOTTLE_OPENER.get()));
        // add.accept(new ItemStack(ModItems.BOTTLE_QUANTUM.get()));
        // add.accept(new ItemStack(ModItems.BOTTLE_RAD.get()));
        // add.accept(new ItemStack(ModItems.BOTTLE_SPARKLE.get()));
        // add.accept(new ItemStack(ModItems.BOTTLED_CLOUD.get()));
        // add.accept(new ItemStack(ModItems.BOY_BULLET.get()));
        // add.accept(new ItemStack(ModItems.BOY_IGNITER.get()));
        // add.accept(new ItemStack(ModItems.BOY_KIT.get()));
        // add.accept(new ItemStack(ModItems.BOY_PROPELLANT.get()));
        // add.accept(new ItemStack(ModItems.BOY_SHIELDING.get()));
        // add.accept(new ItemStack(ModItems.BOY_TARGET.get()));
        // add.accept(new ItemStack(ModItems.BROKEN_ITEM.get()));
        // add.accept(new ItemStack(ModItems.BUCKET_ACID.get()));
        // add.accept(new ItemStack(ModItems.BUCKET_MUD.get()));
        // add.accept(new ItemStack(ModItems.BUCKET_SCHRABIDIC_ACID.get()));
        // add.accept(new ItemStack(ModItems.BUCKET_SULFURIC_ACID.get()));
        // add.accept(new ItemStack(ModItems.BUCKET_TOXIC.get()));
        // add.accept(new ItemStack(ModItems.BURNT_BARK.get()));
        // add.accept(new ItemStack(ModItems.CANISTER_EMPTY.get()));
        // add.accept(new ItemStack(ModItems.CANISTER_NAPALM.get()));
        // add.accept(new ItemStack(ModItems.CANNED_SLIME.get()));
        // add.accept(new ItemStack(ModItems.CANTEEN_VODKA.get()));
        // add.accept(new ItemStack(ModItems.CAP_FRITZ.get()));
        // add.accept(new ItemStack(ModItems.CAP_KORL.get()));
        // add.accept(new ItemStack(ModItems.CAP_NUKA.get()));
        // add.accept(new ItemStack(ModItems.CAP_QUANTUM.get()));
        // add.accept(new ItemStack(ModItems.CAP_RAD.get()));
        // add.accept(new ItemStack(ModItems.CAP_SPARKLE.get()));
        // add.accept(new ItemStack(ModItems.CAP_STAR.get()));
        // add.accept(new ItemStack(ModItems.CAP_SUNSET.get()));
        // add.accept(new ItemStack(ModItems.CAPE_GASMASK.get()));
        // add.accept(new ItemStack(ModItems.CAPE_RADIATION.get()));
        // add.accept(new ItemStack(ModItems.CAPE_SCHRABIDIUM.get()));
        // add.accept(new ItemStack(ModItems.CARD_AOS.get()));
        // add.accept(new ItemStack(ModItems.CARD_QOS.get()));
        // add.accept(new ItemStack(ModItems.CASING_BAG.get()));
        // add.accept(new ItemStack(ModItems.CATALYST_CLAY.get()));
        // add.accept(new ItemStack(ModItems.CATALYTIC_CONVERTER.get()));
        // add.accept(new ItemStack(ModItems.CBT_DEVICE.get()));
        // add.accept(new ItemStack(ModItems.CELL_ANTI_SCHRABIDIUM.get()));
        // add.accept(new ItemStack(ModItems.CELL_ANTIMATTER.get()));
        // add.accept(new ItemStack(ModItems.CELL_BALEFIRE.get()));
        // add.accept(new ItemStack(ModItems.CELL_DEUTERIUM.get()));
        // add.accept(new ItemStack(ModItems.CELL_EMPTY.get()));
        // add.accept(new ItemStack(ModItems.CELL_PUF6.get()));
        // add.accept(new ItemStack(ModItems.CELL_TRITIUM.get()));
        // add.accept(new ItemStack(ModItems.CELL_UF6.get()));
        // add.accept(new ItemStack(ModItems.CENTRI_STICK.get()));
        // add.accept(new ItemStack(ModItems.CHAINSAW.get()));
        // add.accept(new ItemStack(ModItems.CHEESE.get()));
        // add.accept(new ItemStack(ModItems.CHEMISTRY_SET.get()));
        // add.accept(new ItemStack(ModItems.CHEMISTRY_SET_BORON.get()));
        // add.accept(new ItemStack(ModItems.CHERNOBYLSIGN.get()));
        // add.accept(new ItemStack(ModItems.CHLORINE_PINWHEEL.get()));
        // add.accept(new ItemStack(ModItems.CHLOROPHYTE_AXE.get()));
        // add.accept(new ItemStack(ModItems.CHLOROPHYTE_PICKAXE.get()));
        // add.accept(new ItemStack(ModItems.CHOCOLATE.get()));
        // add.accept(new ItemStack(ModItems.CHOCOLATE_MILK.get()));
        // add.accept(new ItemStack(ModItems.CHOPPER.get()));
        // add.accept(new ItemStack(ModItems.CHOPPER_BLADES.get()));
        // add.accept(new ItemStack(ModItems.CHOPPER_GUN.get()));
        // add.accept(new ItemStack(ModItems.CHOPPER_HEAD.get()));
        // add.accept(new ItemStack(ModItems.CHOPPER_TAIL.get()));
        // add.accept(new ItemStack(ModItems.CHOPPER_TORSO.get()));
        // add.accept(new ItemStack(ModItems.CHOPPER_WING.get()));
        // add.accept(new ItemStack(ModItems.CIGARETTE.get()));
        // add.accept(new ItemStack(ModItems.CINNEBAR.get()));
        // add.accept(new ItemStack(ModItems.CIRCUIT_STAR.get()));
        // add.accept(new ItemStack(ModItems.CLAY_TABLET.get()));
        // add.accept(new ItemStack(ModItems.CMB_AXE.get()));
        // add.accept(new ItemStack(ModItems.CMB_BOOTS.get()));
        // add.accept(new ItemStack(ModItems.CMB_HELMET.get()));
        // add.accept(new ItemStack(ModItems.CMB_HOE.get()));
        // add.accept(new ItemStack(ModItems.CMB_LEGS.get()));
        // add.accept(new ItemStack(ModItems.CMB_PLATE.get()));
        // add.accept(new ItemStack(ModItems.CMB_SHOVEL.get()));
        // add.accept(new ItemStack(ModItems.CMB_SWORD.get()));
        // add.accept(new ItemStack(ModItems.COAL_INFERNAL.get()));
        // add.accept(new ItemStack(ModItems.COBALT_AXE.get()));
        // add.accept(new ItemStack(ModItems.COBALT_DECORATED_AXE.get()));
        // add.accept(new ItemStack(ModItems.COBALT_DECORATED_HOE.get()));
        // add.accept(new ItemStack(ModItems.COBALT_DECORATED_PICKAXE.get()));
        // add.accept(new ItemStack(ModItems.COBALT_DECORATED_SHOVEL.get()));
        // add.accept(new ItemStack(ModItems.COBALT_DECORATED_SWORD.get()));
        // add.accept(new ItemStack(ModItems.COBALT_HOE.get()));
        // add.accept(new ItemStack(ModItems.COBALT_LEGGINGS.get()));
        // add.accept(new ItemStack(ModItems.COBALT_PICKAXE.get()));
        // add.accept(new ItemStack(ModItems.COBALT_CHESTPLATE.get()));
        // add.accept(new ItemStack(ModItems.COBALT_SHOVEL.get()));
        // add.accept(new ItemStack(ModItems.COBALT_SWORD.get()));
        // add.accept(new ItemStack(ModItems.COFFEE.get()));
        // add.accept(new ItemStack(ModItems.COFFEE_RADIUM.get()));
        // add.accept(new ItemStack(ModItems.COIN_CREEPER.get()));
        // add.accept(new ItemStack(ModItems.COIN_MASKMAN.get()));
        // add.accept(new ItemStack(ModItems.COIN_RADIATION.get()));
        // add.accept(new ItemStack(ModItems.COIN_TOKEN.get()));
        // add.accept(new ItemStack(ModItems.COIN_UFO.get()));
        // add.accept(new ItemStack(ModItems.COIN_WORM.get()));
        // add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.COMBINE_SCRAP, MaterialShape.SCRAP)));
        // add.accept(new ItemStack(ModItems.COMPONENT_EMITTER.get()));
        // add.accept(new ItemStack(ModItems.COMPONENT_LIMITER.get()));
        // add.accept(new ItemStack(ModItems.CONTAINMENT_BOX.get()));
        // add.accept(new ItemStack(ModItems.CORDITE.get()));
        // add.accept(new ItemStack(ModItems.COTTON_CANDY.get()));
        // add.accept(new ItemStack(ModItems.CRACKPIPE.get()));
        // add.accept(new ItemStack(ModItems.CRATE_CALLER.get()));
        // add.accept(new ItemStack(ModItems.CRUCIBLE_TEMPLATE.get()));
        // add.accept(new ItemStack(ModItems.CUBE_POWER.get()));
        // add.accept(new ItemStack(ModItems.CUSTOM_AMAT.get()));
        // add.accept(new ItemStack(ModItems.CUSTOM_DIRTY.get()));
        // add.accept(new ItemStack(ModItems.CUSTOM_FALL.get()));
        // add.accept(new ItemStack(ModItems.CUSTOM_HYDRO.get()));
        // add.accept(new ItemStack(ModItems.CUSTOM_KIT.get()));
        // add.accept(new ItemStack(ModItems.CUSTOM_NUKE.get()));
        // add.accept(new ItemStack(ModItems.CUSTOM_SCHRAB.get()));
        // add.accept(new ItemStack(ModItems.CUSTOM_TNT.get()));
        // add.accept(new ItemStack(ModItems.DEBRIS_CONCRETE.get()));
        // add.accept(new ItemStack(ModItems.DEBRIS_ELEMENT.get()));
        // add.accept(new ItemStack(ModItems.DEBRIS_EXCHANGER.get()));
        // add.accept(new ItemStack(ModItems.DEBRIS_FUEL.get()));
        // add.accept(new ItemStack(ModItems.DEBRIS_GRAPHITE.get()));
        // add.accept(new ItemStack(ModItems.DEBRIS_METAL.get()));
        // add.accept(new ItemStack(ModItems.DEBRIS_SHRAPNEL.get()));
        // add.accept(new ItemStack(ModItems.DEFINITELYFOOD.get()));
        // add.accept(new ItemStack(ModItems.DEFUSER_GOLD.get()));
        // add.accept(new ItemStack(ModItems.DEMON_CORE_CLOSED.get()));
        // add.accept(new ItemStack(ModItems.DEMON_CORE_OPEN.get()));
        // add.accept(new ItemStack(ModItems.DESH_AXE.get()));
        // add.accept(new ItemStack(ModItems.DESH_HOE.get()));
        // add.accept(new ItemStack(ModItems.DESH_PICKAXE.get()));
        // add.accept(new ItemStack(ModItems.DESH_SHOVEL.get()));
        // add.accept(new ItemStack(ModItems.DESH_SWORD.get()));
        // add.accept(new ItemStack(ModItems.DESIGNATOR_ARTY_RANGE.get()));
        // add.accept(new ItemStack(ModItems.DETONATOR_DE.get()));
        // add.accept(new ItemStack(ModItems.DETONATOR_DEADMAN.get()));
        // add.accept(new ItemStack(ModItems.DEUTERIUM_FILTER.get()));
        // add.accept(new ItemStack(ModItems.DIAMOND_GAVEL.get()));
        // add.accept(new ItemStack(ModItems.DIESELSUIT_BOOTS.get()));
        // add.accept(new ItemStack(ModItems.DIESELSUIT_HELMET.get()));
        // add.accept(new ItemStack(ModItems.DIESELSUIT_LEGS.get()));
        // add.accept(new ItemStack(ModItems.DIESELSUIT_PLATE.get()));
        // add.accept(new ItemStack(ModItems.DISPERSER_CANISTER.get()));
        // add.accept(new ItemStack(ModItems.DNS_BOOTS.get()));
        // add.accept(new ItemStack(ModItems.DNS_HELMET.get()));
        // add.accept(new ItemStack(ModItems.DNS_LEGS.get()));
        // add.accept(new ItemStack(ModItems.DNS_PLATE.get()));
        // add.accept(new ItemStack(ModItems.DNT_LEGGINGS.get()));
        // add.accept(new ItemStack(ModItems.DNT_CHESTPLATE.get()));
        // add.accept(new ItemStack(ModItems.DNT_SWORD.get()));
        // add.accept(new ItemStack(ModBlocks.METAL_DOOR.get()));
        // add.accept(new ItemStack(ModItems.DOOR_RED.get()));
        // add.accept(new ItemStack(ModItems.DRAX.get()));
        // add.accept(new ItemStack(ModItems.DRAX_MK2.get()));
        // add.accept(new ItemStack(ModItems.DRAX_MK3.get()));
        // add.accept(new ItemStack(ModItems.DRILLBIT_DESH.get()));
        // add.accept(new ItemStack(ModItems.DRILLBIT_DESH_DIAMOND.get()));
        // add.accept(new ItemStack(ModItems.DRILLBIT_FERRO.get()));
        // add.accept(new ItemStack(ModItems.DRILLBIT_FERRO_DIAMOND.get()));
        // add.accept(new ItemStack(ModItems.DRILLBIT_HSS.get()));
        // add.accept(new ItemStack(ModItems.DRILLBIT_HSS_DIAMOND.get()));
        // add.accept(new ItemStack(ModItems.DRILLBIT_STEEL.get()));
        // add.accept(new ItemStack(ModItems.DRILLBIT_STEEL_DIAMOND.get()));
        // add.accept(new ItemStack(ModItems.DRILLBIT_TCALLOY.get()));
        // add.accept(new ItemStack(ModItems.DRILLBIT_TCALLOY_DIAMOND.get()));
        // add.accept(new ItemStack(ModItems.DRONE_LINKER.get()));
        // add.accept(new ItemStack(ModItems.DRONE_PATROL.get()));
        // add.accept(new ItemStack(ModItems.DRONE_PATROL_CHUNKLOADING.get()));
        // add.accept(new ItemStack(ModItems.DRONE_PATROL_EXPRESS.get()));
        // add.accept(new ItemStack(ModItems.DRONE_PATROL_EXPRESS_CHUNKLOADING.get()));
        // add.accept(new ItemStack(ModItems.DRONE_REQUEST.get()));
        // add.accept(new ItemStack(ModItems.DWARVEN_PICKAXE.get()));
        // add.accept(new ItemStack(ModItems.DYSFUNCTIONAL_REACTOR.get()));
        // add.accept(new ItemStack(ModItems.EGG_BALEFIRE.get()));
        // add.accept(new ItemStack(ModItems.EGG_BALEFIRE_SHARD.get()));
        // add.accept(new ItemStack(ModItems.EGG_GLYPHID.get()));
        // add.accept(new ItemStack(ModItems.ELEC_SHOVEL.get()));
        // add.accept(new ItemStack(ModItems.ELEC_SWORD.get()));
        // add.accept(new ItemStack(ModItems.ENERGY_CORE.get()));
        // add.accept(new ItemStack(ModItems.ENTANGLEMENT_KIT.get()));
        // add.accept(new ItemStack(ModItems.ENVSUIT_BOOTS.get()));
        // add.accept(new ItemStack(ModItems.ENVSUIT_LEGS.get()));
        // add.accept(new ItemStack(ModItems.ENVSUIT_PLATE.get()));
        // add.accept(new ItemStack(ModItems.EUPHEMIUM_BOOTS.get()));
        // add.accept(new ItemStack(ModItems.EUPHEMIUM_HELMET.get()));
        // add.accept(new ItemStack(ModItems.EUPHEMIUM_LEGS.get()));
        // add.accept(new ItemStack(ModItems.EUPHEMIUM_PLATE.get()));
        // add.accept(new ItemStack(ModItems.FALLOUT.get()));
        // add.accept(new ItemStack(ModItems.FAU_BOOTS.get()));
        // add.accept(new ItemStack(ModItems.FAU_HELMET.get()));
        // add.accept(new ItemStack(ModItems.FAU_LEGS.get()));
        // add.accept(new ItemStack(ModItems.FAU_PLATE.get()));
        // add.accept(new ItemStack(ModItems.FILTER_COAL.get()));
        // add.accept(new ItemStack(ModItems.FINS_BIG_STEEL.get()));
        // add.accept(new ItemStack(ModItems.FINS_FLAT.get()));
        // add.accept(new ItemStack(ModItems.FINS_QUAD_TITANIUM.get()));
        // add.accept(new ItemStack(ModItems.FINS_SMALL_STEEL.get()));
        // add.accept(new ItemStack(ModItems.FINS_TRI_STEEL.get()));
        // add.accept(new ItemStack(ModItems.FLAME_CONSPIRACY.get()));
        // add.accept(new ItemStack(ModItems.FLAME_OPINION.get()));
        // add.accept(new ItemStack(ModItems.FLAME_POLITICS.get()));
        // add.accept(new ItemStack(ModItems.FLAME_PONY.get()));
        // add.accept(new ItemStack(ModItems.FLEIJA_CORE.get()));
        // add.accept(new ItemStack(ModItems.FLEIJA_IGNITER.get()));
        // add.accept(new ItemStack(ModItems.FLEIJA_KIT.get()));
        // add.accept(new ItemStack(ModItems.FLEIJA_PROPELLANT.get()));
        // add.accept(new ItemStack(ModItems.FLUID_IDENTIFIER_MULTI.get()));
        // add.accept(new ItemStack(ModItems.FLYWHEEL_BERYLLIUM.get()));
        // add.accept(new ItemStack(ModItems.FOODITEM.get()));
        // add.accept(new ItemStack(ModItems.FRAGMENT_ACTINIUM.get()));
        // add.accept(new ItemStack(ModItems.FRAGMENT_BORON.get()));
        // add.accept(new ItemStack(ModItems.FRAGMENT_CERIUM.get()));
        // add.accept(new ItemStack(ModItems.FRAGMENT_COBALT.get()));
        // add.accept(new ItemStack(ModItems.FRAGMENT_COLTAN.get()));
        // add.accept(new ItemStack(ModItems.FRAGMENT_LANTHANIUM.get()));
        // add.accept(new ItemStack(ModItems.FRAGMENT_METEORITE.get()));
        // add.accept(new ItemStack(ModItems.FRAGMENT_NEODYMIUM.get()));
        // add.accept(new ItemStack(ModItems.FRAGMENT_NIOBIUM.get()));
        // add.accept(new ItemStack(ModItems.FUSE.get()));
        // add.accept(new ItemStack(ModItems.FUSION_CORE.get()));
        // add.accept(new ItemStack(ModItems.FUSION_CORE_INFINITE.get()));
        // add.accept(new ItemStack(ModItems.FUSION_SHIELD_CHLOROPHYTE.get()));
        // add.accept(new ItemStack(ModItems.FUSION_SHIELD_DESH.get()));
        // add.accept(new ItemStack(ModItems.FUSION_SHIELD_TUNGSTEN.get()));
        // add.accept(new ItemStack(ModItems.FUSION_SHIELD_VAPORWAVE.get()));
        // add.accept(new ItemStack(ModItems.GADGET_CORE.get()));
        // add.accept(new ItemStack(ModItems.GADGET_KIT.get()));
        // add.accept(new ItemStack(ModItems.GADGET_WIREING.get()));
        // add.accept(new ItemStack(ModItems.GAS_MASK.get()));
        // add.accept(new ItemStack(ModItems.GAS_MASK_FILTER.get()));
        // add.accept(new ItemStack(ModItems.GAS_MASK_FILTER_COMBO.get()));
        // add.accept(new ItemStack(ModItems.GAS_MASK_FILTER_MONO.get()));
        // add.accept(new ItemStack(ModItems.GAS_MASK_FILTER_PISS.get()));
        // add.accept(new ItemStack(ModItems.GAS_MASK_FILTER_RAG.get()));
        // add.accept(new ItemStack(ModItems.GAS_MASK_M65.get()));
        // add.accept(new ItemStack(ModItems.GAS_MASK_MONO.get()));
        // add.accept(new ItemStack(ModItems.GAS_MASK_OLDE.get()));
        // add.accept(new ItemStack(ModItems.GAS_TESTER.get()));
        // add.accept(new ItemStack(ModItems.GEAR_LARGE.get()));
        // add.accept(new ItemStack(ModItems.GEM_ALEXANDRITE.get()));
        // add.accept(new ItemStack(ModItems.GEM_RAD.get()));
        // add.accept(new ItemStack(ModItems.GEM_SODALITE.get()));
        // add.accept(new ItemStack(ModItems.GEM_TANTALIUM.get()));
        // add.accept(new ItemStack(ModItems.GEM_VOLCANIC.get()));
        // add.accept(new ItemStack(ModItems.GENERATOR_FRONT.get()));
        // add.accept(new ItemStack(ModItems.GENERATOR_STEEL.get()));
        // add.accept(new ItemStack(ModItems.GLITCH.get()));
        // add.accept(new ItemStack(ModItems.GLOWING_STEW.get()));
        // add.accept(new ItemStack(ModItems.GLYPHID_GLAND.get()));
        // add.accept(new ItemStack(ModItems.GLYPHID_MEAT.get()));
        // add.accept(new ItemStack(ModItems.GLYPHID_MEAT_GRILLED.get()));
        // add.accept(new ItemStack(ModItems.GOGGLES.get()));
        // add.accept(new ItemStack(com.hbm_m.item.weapon.grenade.GrenadeItems.GRENADE_UNIVERSAL.get()));
        // add.accept(new ItemStack(ModItems.GUN_B92.get()));
        // add.accept(new ItemStack(ModItems.GUN_FIREEXT.get()));
        // add.accept(new ItemStack(ModItems.GUN_KIT_1.get()));
        // add.accept(new ItemStack(ModItems.GUN_KIT_2.get()));
        // add.accept(new ItemStack(ModItems.GUN_PA_RANGED.get()));
        // add.accept(new ItemStack(ModItems.HAND_DRILL.get()));
        // add.accept(new ItemStack(ModItems.HAND_DRILL_DESH.get()));
        // add.accept(new ItemStack(ModItems.HAZMAT_BOOTS_GREY.get()));
        // add.accept(new ItemStack(ModItems.HAZMAT_BOOTS_RED.get()));
        // add.accept(new ItemStack(ModItems.HAZMAT_GREY_KIT.get()));
        // add.accept(new ItemStack(ModItems.HAZMAT_HELMET_GREY.get()));
        // add.accept(new ItemStack(ModItems.HAZMAT_HELMET_RED.get()));
        // add.accept(new ItemStack(ModItems.HAZMAT_KIT.get()));
        // add.accept(new ItemStack(ModItems.HAZMAT_LEGGINGS.get()));
        // add.accept(new ItemStack(ModItems.HAZMAT_LEGS_GREY.get()));
        // add.accept(new ItemStack(ModItems.HAZMAT_LEGS_RED.get()));
        // add.accept(new ItemStack(ModItems.HAZMAT_PAA_BOOTS.get()));
        // add.accept(new ItemStack(ModItems.HAZMAT_PAA_HELMET.get()));
        // add.accept(new ItemStack(ModItems.HAZMAT_PAA_LEGS.get()));
        // add.accept(new ItemStack(ModItems.HAZMAT_PAA_PLATE.get()));
        // add.accept(new ItemStack(ModItems.HAZMAT_CHESTPLATE.get()));
        // add.accept(new ItemStack(ModItems.HAZMAT_PLATE_GREY.get()));
        // add.accept(new ItemStack(ModItems.HAZMAT_PLATE_RED.get()));
        // add.accept(new ItemStack(ModItems.HAZMAT_RED_KIT.get()));
        // add.accept(new ItemStack(ModItems.HEAVY_COMPONENT.get()));
        // add.accept(new ItemStack(ModItems.HEV_BOOTS.get()));
        // add.accept(new ItemStack(ModItems.HEV_HELMET.get()));
        // add.accept(new ItemStack(ModItems.HEV_LEGS.get()));
        // add.accept(new ItemStack(ModItems.HEV_PLATE.get()));
        // add.accept(new ItemStack(ModItems.HOLOTAPE_DAMAGED.get()));
        // add.accept(new ItemStack(ModItems.HORSESHOE_MAGNET.get()));
        // add.accept(new ItemStack(ModItems.HULL_BIG_ALUMINIUM.get()));
        // add.accept(new ItemStack(ModItems.HULL_BIG_STEEL.get()));
        // add.accept(new ItemStack(ModItems.HULL_BIG_TITANIUM.get()));
        // add.accept(new ItemStack(ModItems.HULL_SMALL_ALUMINIUM.get()));
        // add.accept(new ItemStack(ModItems.HULL_SMALL_STEEL.get()));
        // add.accept(new ItemStack(ModItems.ICF_PELLET.get()));
        // add.accept(new ItemStack(ModItems.ICF_PELLET_DEPLETED.get()));
        // add.accept(new ItemStack(ModItems.ICF_PELLET_EMPTY.get()));
        // add.accept(new ItemStack(ModItems.INDUSTRIAL_MAGNET.get()));
        // add.accept(new ItemStack(ModItems.INGOT_ALUMINIUM.get()));
        // add.accept(new ItemStack(ModItems.INJECTOR_5HTP.get()));
        // add.accept(new ItemStack(ModItems.INJECTOR_KNIFE.get()));
        // add.accept(new ItemStack(ModItems.INK.get()));
        // add.accept(new ItemStack(ModItems.INSERT_DOXIUM.get()));
        // add.accept(new ItemStack(ModItems.INSERT_DU.get()));
        // add.accept(new ItemStack(ModItems.INSERT_ERA.get()));
        // add.accept(new ItemStack(ModItems.INSERT_ESAPI.get()));
        // add.accept(new ItemStack(ModItems.INSERT_GHIORSIUM.get()));
        // add.accept(new ItemStack(ModItems.INSERT_KEVLAR.get()));
        // add.accept(new ItemStack(ModItems.INSERT_POLONIUM.get()));
        // add.accept(new ItemStack(ModItems.INSERT_SAPI.get()));
        // add.accept(new ItemStack(ModItems.INSERT_STEEL.get()));
        // add.accept(new ItemStack(ModItems.INSERT_XSAPI.get()));
        // add.accept(new ItemStack(ModItems.INSERT_YHARONITE.get()));
        // add.accept(new ItemStack(ModItems.IV_BLOOD.get()));
        // add.accept(new ItemStack(ModItems.IV_EMPTY.get()));
        // add.accept(new ItemStack(ModItems.IV_XP.get()));
        // add.accept(new ItemStack(ModItems.IV_XP_EMPTY.get()));
        // add.accept(new ItemStack(ModItems.JACKT.get()));
        // add.accept(new ItemStack(ModItems.JACKT2.get()));
        // add.accept(new ItemStack(ModItems.JETPACK_BOOST.get()));
        // add.accept(new ItemStack(ModItems.JETPACK_BREAK.get()));
        // add.accept(new ItemStack(ModItems.JETPACK_FLY.get()));
        // add.accept(new ItemStack(ModItems.JETPACK_TANK.get()));
        // add.accept(new ItemStack(ModItems.JETPACK_VECTOR.get()));
        // add.accept(new ItemStack(ModItems.JOURNAL_BJ.get()));
        // add.accept(new ItemStack(ModItems.JOURNAL_PIP.get()));
        // add.accept(new ItemStack(ModItems.JOURNAL_SILVER.get()));
        // add.accept(new ItemStack(ModItems.KEY.get()));
        // add.accept(new ItemStack(ModItems.KEY_RED.get()));
        // add.accept(new ItemStack(ModItems.KEY_RED_CRACKED.get()));
        // add.accept(new ItemStack(ModItems.LASER_CRYSTAL_BISMUTH.get()));
        // add.accept(new ItemStack(ModItems.LASER_CRYSTAL_CMB.get()));
        // add.accept(new ItemStack(ModItems.LASER_CRYSTAL_CO2.get()));
        // add.accept(new ItemStack(ModItems.LASER_CRYSTAL_DIGAMMA.get()));
        // add.accept(new ItemStack(ModItems.LASER_CRYSTAL_DNT.get()));
        // add.accept(new ItemStack(ModItems.LAUNCH_CODE.get()));
        // add.accept(new ItemStack(ModItems.LAUNCH_CODE_PIECE.get()));
        // add.accept(new ItemStack(ModItems.LAUNCH_KEY.get()));
        // add.accept(new ItemStack(ModItems.LEAD_GAVEL.get()));
        // add.accept(new ItemStack(ModItems.LEMON.get()));
        // add.accept(new ItemStack(ModItems.LINKER.get()));
        // add.accept(new ItemStack(ModItems.LIQUIDATOR_LEGGINGS.get()));
        // add.accept(new ItemStack(ModItems.LIQUIDATOR_CHESTPLATE.get()));
        // add.accept(new ItemStack(ModItems.LITHIUM.get()));
        // add.accept(new ItemStack(ModItems.LODESTONE.get()));
        // add.accept(new ItemStack(ModItems.LOOP_STEW.get()));
        // add.accept(new ItemStack(ModItems.LOOPS.get()));
        // add.accept(new ItemStack(ModItems.LOOT_10.get()));
        // add.accept(new ItemStack(ModItems.LOOT_15.get()));
        // add.accept(new ItemStack(ModItems.LOOT_MISC.get()));
        // add.accept(new ItemStack(ModItems.MAN_KIT.get()));
        // add.accept(new ItemStack(ModItems.MARSHMALLOW.get()));
        // add.accept(new ItemStack(ModItems.MASK_OF_INFAMY.get()));
        // add.accept(new ItemStack(ModItems.MASK_PISS.get()));
        // add.accept(new ItemStack(ModItems.MASK_RAG.get()));
        // add.accept(new ItemStack(ModItems.MATCHSTICK.get()));
        // add.accept(new ItemStack(ModItems.MECH_KEY.get()));
        // add.accept(new ItemStack(ModItems.MED_BAG.get()));
        // add.accept(new ItemStack(ModItems.MED_IPECAC.get()));
        // add.accept(new ItemStack(ModItems.MED_PTSD.get()));
        // add.accept(new ItemStack(ModItems.MEDAL_LIQUIDATOR.get()));
        // add.accept(new ItemStack(ModItems.MELTDOWN_TOOL.get()));
        // add.accept(new ItemStack(ModItems.MEMESPOON.get()));
        // add.accept(new ItemStack(ModItems.MESE_AXE.get()));
        // add.accept(new ItemStack(ModItems.MESE_GAVEL.get()));
        // add.accept(new ItemStack(ModItems.MESE_PICKAXE.get()));
        // add.accept(new ItemStack(ModItems.METEOR_CHARM.get()));
        // add.accept(new ItemStack(ModItems.METEOR_REMOTE.get()));
        // add.accept(new ItemStack(ModItems.MIKE_COOLING_UNIT.get()));
        // add.accept(new ItemStack(ModItems.MIKE_CORE.get()));
        // add.accept(new ItemStack(ModItems.MIKE_DEUT.get()));
        // add.accept(new ItemStack(ModItems.MIKE_KIT.get()));
        // add.accept(new ItemStack(ModItems.MIRROR_TOOL.get()));
        // add.accept(new ItemStack(ModItems.MISSILE_ANTI_BALLISTIC.get()));
        // add.accept(new ItemStack(ModItems.MISSILE_CARRIER.get()));
        // add.accept(new ItemStack(ModItems.MISSILE_CUSTOM.get()));
        // add.accept(new ItemStack(ModItems.MISSILE_ENDO.get()));
        // add.accept(new ItemStack(ModItems.MISSILE_EXO.get()));
        // add.accept(new ItemStack(ModItems.MISSILE_KIT.get()));
        // add.accept(new ItemStack(ModItems.MORNING_GLORY.get()));
        // add.accept(new ItemStack(ModItems.MUCHO_MANGO.get()));
        // add.accept(new ItemStack(ModItems.MULTI_KIT.get()));
        // add.accept(new ItemStack(ModItems.N2_CHARGE.get()));
        // add.accept(new ItemStack(ModItems.NEUTRINO_LENS.get()));
        // add.accept(new ItemStack(ModItems.NIGHT_VISION.get()));
        // add.accept(new ItemStack(ModItems.NITRA.get()));
        // add.accept(new ItemStack(ModItems.NITRA_SMALL.get()));
        // add.accept(new ItemStack(ModItems.NO9.get()));
        // add.accept(new ItemStack(ModItems.NOTHING.get()));
        // add.accept(new ItemStack(ModItems.NUCLEAR_WASTE.get()));
        // add.accept(new ItemStack(ModItems.NUCLEAR_WASTE_LONG.get()));
        // add.accept(new ItemStack(ModItems.NUCLEAR_WASTE_LONG_DEPLETED.get()));
        // add.accept(new ItemStack(ModItems.NUCLEAR_WASTE_PEARL.get()));
        // add.accept(new ItemStack(ModItems.NUCLEAR_WASTE_SHORT.get()));
        // add.accept(new ItemStack(ModItems.NUCLEAR_WASTE_SHORT_DEPLETED.get()));
        // add.accept(new ItemStack(ModItems.NUCLEAR_WASTE_VITRIFIED.get()));
        // add.accept(new ItemStack(ModItems.NUGGET.get()));
        // add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.ACTINIUM, MaterialShape.NUGGET)));
        // add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.AM241, MaterialShape.NUGGET)));
        // add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.AM242, MaterialShape.NUGGET)));
        // add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.AM_MIX, MaterialShape.NUGGET)));
        // add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.AMERICIUM_FUEL, MaterialShape.NUGGET)));
        // add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.ARSENIC, MaterialShape.NUGGET)));
        // add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.AU198, MaterialShape.NUGGET)));
        // add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.AUSTRALIUM, MaterialShape.NUGGET)));
        // add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.AUSTRALIUM_GREATER, MaterialShape.NUGGET)));
        // add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.AUSTRALIUM_LESSER, MaterialShape.NUGGET)));
        // add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.BERYLLIUM, MaterialShape.NUGGET)));
        // add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.BISMUTH, MaterialShape.NUGGET)));
        // add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.CO60, MaterialShape.NUGGET)));
        // add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.COBALT, MaterialShape.NUGGET)));
        // add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.DESH, MaterialShape.NUGGET)));
        // add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.DINEUTRONIUM, MaterialShape.NUGGET)));
        // add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.EUPHEMIUM, MaterialShape.NUGGET)));
        // add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.GH336, MaterialShape.NUGGET)));
        // add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.HES, MaterialShape.NUGGET)));
        // add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.LEAD, MaterialShape.NUGGET)));
        // add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.LES_FUEL, MaterialShape.NUGGET)));
        // add.accept(new ItemStack(ModItems.NUGGET_MERCURY.get()));
        // add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.MOX_FUEL, MaterialShape.NUGGET)));
        // add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.NEPTUNIUM, MaterialShape.NUGGET)));
        // add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.NEPTUNIUM_FUEL, MaterialShape.NUGGET)));
        // add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.NIOBIUM, MaterialShape.NUGGET)));
        // add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.OSMIRIDIUM, MaterialShape.NUGGET)));
        // add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.PB209, MaterialShape.NUGGET)));
        // add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.PLUTONIUM, MaterialShape.NUGGET)));
        // add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.PLUTONIUM_FUEL, MaterialShape.NUGGET)));
        // add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.POLONIUM, MaterialShape.NUGGET)));
        // add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.PLUTONIUM238, MaterialShape.NUGGET)));
        // add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.PLUTONIUM239, MaterialShape.NUGGET)));
        // add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.PLUTONIUM240, MaterialShape.NUGGET)));
        // add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.PLUTONIUM241, MaterialShape.NUGGET)));
        // add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.PU_MIX, MaterialShape.NUGGET)));
        // add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.RA226, MaterialShape.NUGGET)));
        // add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.SCHRABIDIUM, MaterialShape.NUGGET)));
        // add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.SCHRABIDIUM_FUEL, MaterialShape.NUGGET)));
        // add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.SOLINIUM, MaterialShape.NUGGET)));
        // add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.SR90, MaterialShape.NUGGET)));
        // add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.TECHNETIUM, MaterialShape.NUGGET)));
        // add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.THORIUM232, MaterialShape.NUGGET)));
        // add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.THORIUM_FUEL, MaterialShape.NUGGET)));
        // add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.URANIUM233, MaterialShape.NUGGET)));
        // add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.URANIUM235, MaterialShape.NUGGET)));
        // add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.URANIUM238, MaterialShape.NUGGET)));
        // add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.URANIUM, MaterialShape.NUGGET)));
        // add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.URANIUM_FUEL, MaterialShape.NUGGET)));
        // add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.ZIRCONIUM, MaterialShape.NUGGET)));
        // add.accept(new ItemStack(ModItems.NUKE_ADVANCED_KIT.get()));
        // add.accept(new ItemStack(ModItems.NUKE_COMMERCIALLY_KIT.get()));
        // add.accept(new ItemStack(ModItems.NUKE_ELECTRIC_KIT.get()));
        // add.accept(new ItemStack(ModItems.NUKE_STARTER_KIT.get()));
        // add.accept(new ItemStack(ModItems.ORE_BEDROCK.get()));
        // add.accept(new ItemStack(ModItems.ORE_CENTRIFUGED.get()));
        // add.accept(new ItemStack(ModItems.ORE_CLEANED.get()));
        // add.accept(new ItemStack(ModItems.ORE_DEEPCLEANED.get()));
        // add.accept(new ItemStack(ModItems.ORE_DENSITY_SCANNER.get()));
        // add.accept(new ItemStack(ModItems.ORE_ENRICHED.get()));
        // add.accept(new ItemStack(ModItems.ORE_NITRATED.get()));
        // add.accept(new ItemStack(ModItems.ORE_NITROCRYSTALLINE.get()));
        // add.accept(new ItemStack(ModItems.ORE_PURIFIED.get()));
        // add.accept(new ItemStack(ModItems.ORE_RADCLEANED.get()));
        // add.accept(new ItemStack(ModItems.ORE_SEARED.get()));
        // add.accept(new ItemStack(ModItems.ORE_SEPARATED.get()));
        // add.accept(new ItemStack(ModItems.OVERFUSE.get()));
        // add.accept(new ItemStack(ModItems.PAA_LEGGINGS.get()));
        // add.accept(new ItemStack(ModItems.PAA_CHESTPLATE.get()));
        // add.accept(new ItemStack(ModItems.PADLOCK.get()));
        // add.accept(new ItemStack(ModItems.PADLOCK_REINFORCED.get()));
        // add.accept(new ItemStack(ModItems.PADLOCK_RUSTY.get()));
        // add.accept(new ItemStack(ModItems.PADLOCK_UNBREAKABLE.get()));
        // add.accept(new ItemStack(ModItems.PADS_RUBBER.get()));
        // add.accept(new ItemStack(ModItems.PADS_SLIME.get()));
        // add.accept(new ItemStack(ModItems.PADS_STATIC.get()));
        // add.accept(new ItemStack(ModItems.PANCAKE.get()));
        // add.accept(new ItemStack(ModItems.PART_BARREL_HEAVY.get()));
        // add.accept(new ItemStack(ModItems.PART_BARREL_LIGHT.get()));
        // add.accept(new ItemStack(ModItems.PART_GRIP.get()));
        // add.accept(new ItemStack(ModItems.PART_MECHANISM.get()));
        // add.accept(new ItemStack(ModItems.PART_RECEIVER_HEAVY.get()));
        // add.accept(new ItemStack(ModItems.PART_RECEIVER_LIGHT.get()));
        // add.accept(new ItemStack(ModItems.PART_STOCK.get()));
        // add.accept(new ItemStack(ModItems.PARTICLE_AMAT.get()));
        // add.accept(new ItemStack(ModItems.PARTICLE_ASCHRAB.get()));
        // add.accept(new ItemStack(ModItems.PARTICLE_COPPER.get()));
        // add.accept(new ItemStack(ModItems.PARTICLE_DARK.get()));
        // add.accept(new ItemStack(ModItems.PARTICLE_DIGAMMA.get()));
        // add.accept(new ItemStack(ModItems.PARTICLE_EMPTY.get()));
        // add.accept(new ItemStack(ModItems.PARTICLE_HIGGS.get()));
        // add.accept(new ItemStack(ModItems.PARTICLE_HYDROGEN.get()));
        // add.accept(new ItemStack(ModItems.PARTICLE_LEAD.get()));
        // add.accept(new ItemStack(ModItems.PARTICLE_LUTECE.get()));
        // add.accept(new ItemStack(ModItems.PARTICLE_MUON.get()));
        // add.accept(new ItemStack(ModItems.PARTICLE_SPARKTICLE.get()));
        // add.accept(new ItemStack(ModItems.PARTICLE_STRANGE.get()));
        // add.accept(new ItemStack(ModItems.PARTICLE_TACHYON.get()));
        // add.accept(new ItemStack(ModItems.PARTS_LEGENDARY.get()));
        // add.accept(new ItemStack(ModItems.PEAS.get()));
        // add.accept(new ItemStack(ModItems.PEDESTAL_STEEL.get()));
        // add.accept(new ItemStack(ModItems.PELLET_ANTIMATTER.get()));
        // add.accept(new ItemStack(ModItems.PELLET_CLUSTER.get()));
        // add.accept(new ItemStack(ModItems.PELLET_GAS.get()));
        // add.accept(new ItemStack(ModItems.PELLET_RTG.get()));
        // add.accept(new ItemStack(ModItems.PELLET_RTG_ACTINIUM.get()));
        // add.accept(new ItemStack(ModItems.PELLET_RTG_AMERICIUM.get()));
        // add.accept(new ItemStack(ModItems.PELLET_RTG_BERKELIUM.get()));
        // add.accept(new ItemStack(ModItems.PELLET_RTG_COBALT.get()));
        // add.accept(new ItemStack(ModItems.PELLET_RTG_GOLD.get()));
        // add.accept(new ItemStack(ModItems.PELLET_RTG_LEAD.get()));
        // add.accept(new ItemStack(ModItems.PELLET_RTG_POLONIUM.get()));
        // add.accept(new ItemStack(ModItems.PELLET_RTG_RADIUM.get()));
        // add.accept(new ItemStack(ModItems.PELLET_RTG_STRONTIUM.get()));
        // add.accept(new ItemStack(ModItems.PELLET_RTG_WEAK.get()));
        // add.accept(new ItemStack(ModItems.PHOTO_PANEL.get()));
        // add.accept(new ItemStack(ModItems.PILE_ROD_BORON.get()));
        // add.accept(new ItemStack(ModItems.PILE_ROD_DETECTOR.get()));
        // add.accept(new ItemStack(ModItems.PILE_ROD_LITHIUM.get()));
        // add.accept(new ItemStack(ModItems.PILE_ROD_PLUTONIUM.get()));
        // add.accept(new ItemStack(ModItems.PILE_ROD_PU239.get()));
        // add.accept(new ItemStack(ModItems.PILE_ROD_SOURCE.get()));
        // add.accept(new ItemStack(ModItems.PILE_ROD_URANIUM.get()));
        // add.accept(new ItemStack(ModItems.PILL_HERBAL.get()));
        // add.accept(new ItemStack(ModItems.PILL_IODINE.get()));
        // add.accept(new ItemStack(ModItems.PILL_RED.get()));
        // add.accept(new ItemStack(ModItems.PIN.get()));
        // add.accept(new ItemStack(ModItems.PIPES_STEEL.get()));
        // add.accept(new ItemStack(ModItems.PIPETTE.get()));
        // add.accept(new ItemStack(ModItems.PIPETTE_BORON.get()));
        // add.accept(new ItemStack(ModItems.PIPETTE_LABORATORY.get()));
        // add.accept(new ItemStack(ModItems.PISTON_SELENIUM.get()));
        // add.accept(new ItemStack(ModItems.PISTON_SET_DESH.get()));
        // add.accept(new ItemStack(ModItems.PISTON_SET_DURA.get()));
        // add.accept(new ItemStack(ModItems.PISTON_SET_STARMETAL.get()));
        // add.accept(new ItemStack(ModItems.PISTON_SET_STEEL.get()));
        // add.accept(new ItemStack(ModItems.PLAN_C.get()));
        // add.accept(new ItemStack(ModItems.PLASTIC_BAG.get()));
        // add.accept(new ItemStack(ModItems.PLATE_ALUMINIUM.get()));
        // add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.POLYMER, MaterialShape.PLATE)));
        // add.accept(new ItemStack(ModItems.POLAROID.get()));
        // add.accept(new ItemStack(ModItems.POLLUTION_DETECTOR.get()));
        // add.accept(new ItemStack(ModItems.POWER_NET_TOOL.get()));
        // add.accept(new ItemStack(ModItems.PROTECTION_CHARM.get()));
        // add.accept(new ItemStack(ModItems.PROTOTYPE_KIT.get()));
        // add.accept(new ItemStack(ModItems.PUDDING.get()));
        // add.accept(new ItemStack(ModItems.PWR_PRINTER.get()));
        // add.accept(new ItemStack(ModItems.PWR_FUEL_MEU.get()));
        // add.accept(new ItemStack(ModItems.PWR_FUEL_MEU_HOT.get()));
        // add.accept(new ItemStack(ModItems.PWR_FUEL_HEU233.get()));
        // add.accept(new ItemStack(ModItems.PWR_FUEL_HEU233_HOT.get()));
        // add.accept(new ItemStack(ModItems.PWR_FUEL_HEU235.get()));
        // add.accept(new ItemStack(ModItems.PWR_FUEL_HEU235_HOT.get()));
        // add.accept(new ItemStack(ModItems.PWR_FUEL_MEN.get()));
        // add.accept(new ItemStack(ModItems.PWR_FUEL_MEN_HOT.get()));
        // add.accept(new ItemStack(ModItems.PWR_FUEL_HEN237.get()));
        // add.accept(new ItemStack(ModItems.PWR_FUEL_HEN237_HOT.get()));
        // add.accept(new ItemStack(ModItems.PWR_FUEL_MOX.get()));
        // add.accept(new ItemStack(ModItems.PWR_FUEL_MOX_HOT.get()));
        // add.accept(new ItemStack(ModItems.PWR_FUEL_MEP.get()));
        // add.accept(new ItemStack(ModItems.PWR_FUEL_MEP_HOT.get()));
        // add.accept(new ItemStack(ModItems.PWR_FUEL_HEP239.get()));
        // add.accept(new ItemStack(ModItems.PWR_FUEL_HEP239_HOT.get()));
        // add.accept(new ItemStack(ModItems.PWR_FUEL_HEP241.get()));
        // add.accept(new ItemStack(ModItems.PWR_FUEL_HEP241_HOT.get()));
        // add.accept(new ItemStack(ModItems.PWR_FUEL_MEA.get()));
        // add.accept(new ItemStack(ModItems.PWR_FUEL_MEA_HOT.get()));
        // add.accept(new ItemStack(ModItems.PWR_FUEL_HEA242.get()));
        // add.accept(new ItemStack(ModItems.PWR_FUEL_HEA242_HOT.get()));
        // add.accept(new ItemStack(ModItems.PWR_FUEL_HES326.get()));
        // add.accept(new ItemStack(ModItems.PWR_FUEL_HES326_HOT.get()));
        // add.accept(new ItemStack(ModItems.PWR_FUEL_HES327.get()));
        // add.accept(new ItemStack(ModItems.PWR_FUEL_HES327_HOT.get()));
        // add.accept(new ItemStack(ModItems.PWR_FUEL_BFB_AM_MIX.get()));
        // add.accept(new ItemStack(ModItems.PWR_FUEL_BFB_AM_MIX_HOT.get()));
        // add.accept(new ItemStack(ModItems.PWR_FUEL_BFB_PU241.get()));
        // add.accept(new ItemStack(ModItems.PWR_FUEL_BFB_PU241_HOT.get()));
        // add.accept(new ItemStack(ModItems.QUARTZ_PLUTONIUM.get()));
        // add.accept(new ItemStack(ModItems.RAG.get()));
        // add.accept(new ItemStack(ModItems.RAG_DAMP.get()));
        // add.accept(new ItemStack(ModItems.RAG_PISS.get()));
        // add.accept(new ItemStack(ModItems.RBMK_FUEL_BALEFIRE.get()));
        // add.accept(new ItemStack(ModItems.RBMK_FUEL_BALEFIRE_GOLD.get()));
        // add.accept(new ItemStack(ModItems.RBMK_FUEL_FLASHLEAD.get()));
        // add.accept(new ItemStack(ModItems.RBMK_FUEL_HEA241.get()));
        // add.accept(new ItemStack(ModItems.RBMK_FUEL_HEA242.get()));
        // add.accept(new ItemStack(ModItems.RBMK_FUEL_HEAUS.get()));
        // add.accept(new ItemStack(ModItems.RBMK_FUEL_HEN.get()));
        // add.accept(new ItemStack(ModItems.RBMK_FUEL_HEP241.get()));
        // add.accept(new ItemStack(ModItems.RBMK_FUEL_HES.get()));
        // add.accept(new ItemStack(ModItems.RBMK_FUEL_HEU233.get()));
        // add.accept(new ItemStack(ModItems.RBMK_FUEL_LEA.get()));
        // add.accept(new ItemStack(ModItems.RBMK_FUEL_LEAUS.get()));
        // add.accept(new ItemStack(ModItems.RBMK_FUEL_LES.get()));
        // add.accept(new ItemStack(ModItems.RBMK_FUEL_MEA.get()));
        // add.accept(new ItemStack(ModItems.RBMK_FUEL_MEN.get()));
        // add.accept(new ItemStack(ModItems.RBMK_FUEL_MEP.get()));
        // add.accept(new ItemStack(ModItems.RBMK_FUEL_MES.get()));
        // add.accept(new ItemStack(ModItems.RBMK_FUEL_MEU.get()));
        // add.accept(new ItemStack(ModItems.RBMK_FUEL_PO210BE.get()));
        // add.accept(new ItemStack(ModItems.RBMK_FUEL_PU238BE.get()));
        // add.accept(new ItemStack(ModItems.RBMK_FUEL_RA226BE.get()));
        // add.accept(new ItemStack(ModItems.RBMK_FUEL_THMEU.get()));
        // add.accept(new ItemStack(ModItems.RBMK_FUEL_UEU.get()));
        // add.accept(new ItemStack(ModItems.RBMK_FUEL_UZH.get()));
        // add.accept(new ItemStack(ModItems.RBMK_FUEL_ZFB_AM_MIX.get()));
        // add.accept(new ItemStack(ModItems.RBMK_FUEL_ZFB_BISMUTH.get()));
        // add.accept(new ItemStack(ModItems.RBMK_FUEL_ZFB_PU241.get()));
        // add.accept(new ItemStack(ModItems.RBMK_PELLET_BALEFIRE.get()));
        // add.accept(new ItemStack(ModItems.RBMK_PELLET_BALEFIRE_GOLD.get()));
        // add.accept(new ItemStack(ModItems.RBMK_PELLET_DRX.get()));
        // add.accept(new ItemStack(ModItems.RBMK_PELLET_FLASHLEAD.get()));
        // add.accept(new ItemStack(ModItems.RBMK_PELLET_HEA241.get()));
        // add.accept(new ItemStack(ModItems.RBMK_PELLET_HEA242.get()));
        // add.accept(new ItemStack(ModItems.RBMK_PELLET_HEAUS.get()));
        // add.accept(new ItemStack(ModItems.RBMK_PELLET_HEN.get()));
        // add.accept(new ItemStack(ModItems.RBMK_PELLET_HEP241.get()));
        // add.accept(new ItemStack(ModItems.RBMK_PELLET_HES.get()));
        // add.accept(new ItemStack(ModItems.RBMK_PELLET_HEU233.get()));
        // add.accept(new ItemStack(ModItems.RBMK_PELLET_LEA.get()));
        // add.accept(new ItemStack(ModItems.RBMK_PELLET_LEAUS.get()));
        // add.accept(new ItemStack(ModItems.RBMK_PELLET_LES.get()));
        // add.accept(new ItemStack(ModItems.RBMK_PELLET_MEA.get()));
        // add.accept(new ItemStack(ModItems.RBMK_PELLET_MEN.get()));
        // add.accept(new ItemStack(ModItems.RBMK_PELLET_MEP.get()));
        // add.accept(new ItemStack(ModItems.RBMK_PELLET_MES.get()));
        // add.accept(new ItemStack(ModItems.RBMK_PELLET_MEU.get()));
        // add.accept(new ItemStack(ModItems.RBMK_PELLET_PO210BE.get()));
        // add.accept(new ItemStack(ModItems.RBMK_PELLET_PU238BE.get()));
        // add.accept(new ItemStack(ModItems.RBMK_PELLET_RA226BE.get()));
        // add.accept(new ItemStack(ModItems.RBMK_PELLET_THMEU.get()));
        // add.accept(new ItemStack(ModItems.RBMK_PELLET_UEU.get()));
        // add.accept(new ItemStack(ModItems.RBMK_PELLET_UZH.get()));
        // add.accept(new ItemStack(ModItems.RBMK_PELLET_ZFB_AM_MIX.get()));
        // add.accept(new ItemStack(ModItems.RBMK_PELLET_ZFB_BISMUTH.get()));
        // add.accept(new ItemStack(ModItems.RBMK_PELLET_ZFB_PU241.get()));
        // add.accept(new ItemStack(ModItems.RBMK_TOOL.get()));
        // add.accept(new ItemStack(ModItems.REACHER.get()));
        // add.accept(new ItemStack(ModItems.REACTOR_CORE.get()));
        // add.accept(new ItemStack(ModItems.REACTOR_SENSOR.get()));
        // add.accept(new ItemStack(ModItems.REBAR_PLACER.get()));
        // add.accept(new ItemStack(ModItems.REDSTONE_SWORD.get()));
        // add.accept(new ItemStack(ModItems.RING_PULL.get()));
        // add.accept(new ItemStack(ModItems.RING_STARMETAL.get()));
        // add.accept(new ItemStack(ModItems.ROBES_BOOTS.get()));
        // add.accept(new ItemStack(ModItems.ROBES_HELMET.get()));
        // add.accept(new ItemStack(ModItems.ROBES_LEGS.get()));
        // add.accept(new ItemStack(ModItems.ROBES_PLATE.get()));
        // add.accept(new ItemStack(ModItems.ROCKET_FUEL.get()));
        // add.accept(new ItemStack(ModItems.ROD_DUAL_EMPTY.get()));
        // add.accept(new ItemStack(ModItems.ROD_EMPTY.get()));
        // add.accept(new ItemStack(ModItems.ROD_OF_DISCORD.get()));
        // add.accept(new ItemStack(ModItems.ROD_QUAD_EMPTY.get()));
        // add.accept(new ItemStack(ModItems.RPA_BOOTS.get()));
        // add.accept(new ItemStack(ModItems.RPA_HELMET.get()));
        // add.accept(new ItemStack(ModItems.RPA_LEGS.get()));
        // add.accept(new ItemStack(ModItems.RPA_PLATE.get()));
        // add.accept(new ItemStack(ModItems.RTG_UNIT.get()));
        // add.accept(new ItemStack(ModItems.RTTY_PAGER.get()));
        // add.accept(new ItemStack(ModItems.RUNE_BLANK.get()));
        // add.accept(new ItemStack(ModItems.RUNE_DAGAZ.get()));
        // add.accept(new ItemStack(ModItems.RUNE_HAGALAZ.get()));
        // add.accept(new ItemStack(ModItems.RUNE_ISA.get()));
        // add.accept(new ItemStack(ModItems.RUNE_JERA.get()));
        // add.accept(new ItemStack(ModItems.RUNE_THURISAZ.get()));
        // add.accept(new ItemStack(ModItems.SAFETY_FUSE.get()));
        // add.accept(new ItemStack(ModItems.SAT_CHIP.get()));
        // add.accept(new ItemStack(ModItems.SAT_COORD.get()));
        // add.accept(new ItemStack(ModItems.SAT_DESIGNATOR.get()));
        // add.accept(new ItemStack(ModItems.SAT_GERALD.get()));
        // add.accept(new ItemStack(ModItems.SAT_HEAD_SCANNER.get()));
        // add.accept(new ItemStack(ModItems.SAT_INTERFACE.get()));
        // add.accept(new ItemStack(ModItems.SAT_LUNAR_MINER.get()));
        // add.accept(new ItemStack(ModItems.SAT_MINER.get()));
        // add.accept(new ItemStack(ModItems.SAT_RELAY.get()));
        // add.accept(new ItemStack(ModItems.SAWBLADE.get()));
        // add.accept(new ItemStack(ModItems.SCHNITZEL_VEGAN.get()));
        // add.accept(new ItemStack(ModItems.SCHRABIDIUM_AXE.get()));
        // add.accept(new ItemStack(ModItems.SCHRABIDIUM_BOOTS.get()));
        // add.accept(new ItemStack(ModItems.SCHRABIDIUM_HAMMER.get()));
        // add.accept(new ItemStack(ModItems.SCHRABIDIUM_HELMET.get()));
        // add.accept(new ItemStack(ModItems.SCHRABIDIUM_HOE.get()));
        // add.accept(new ItemStack(ModItems.SCHRABIDIUM_LEGS.get()));
        // add.accept(new ItemStack(ModItems.SCHRABIDIUM_PICKAXE.get()));
        // add.accept(new ItemStack(ModItems.SCHRABIDIUM_PLATE.get()));
        // add.accept(new ItemStack(ModItems.SCHRABIDIUM_SHOVEL.get()));
        // add.accept(new ItemStack(ModItems.SCHRABIDIUM_SWORD.get()));
        // add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.SCRAP_NUCLEAR, MaterialShape.SCRAP)));
        // add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.SCRAP_OIL, MaterialShape.SCRAP)));
        // add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.SCRAP_PLASTIC, MaterialShape.SCRAP)));
        // add.accept(new ItemStack(ModItems.SCRAPS.get()));
        // add.accept(new ItemStack(ModItems.SCRUMPY.get()));
        // add.accept(new ItemStack(ModItems.SECURITY_LEGGINGS.get()));
        // add.accept(new ItemStack(ModItems.SECURITY_CHESTPLATE.get()));
        // add.accept(new ItemStack(ModItems.SEG_10.get()));
        // add.accept(new ItemStack(ModItems.SEG_15.get()));
        // add.accept(new ItemStack(ModItems.SEG_20.get()));
        // add.accept(new ItemStack(ModItems.SERUM.get()));
        // add.accept(new ItemStack(ModItems.SERVO_SET.get()));
        // add.accept(new ItemStack(ModItems.SERVO_SET_DESH.get()));
        // add.accept(new ItemStack(ModItems.SETTINGS_TOOL.get()));
        // add.accept(new ItemStack(ModItems.SHACKLES.get()));
        // add.accept(new ItemStack(ModItems.SHIMMER_AXE.get()));
        // add.accept(new ItemStack(ModItems.SHIMMER_AXE_HEAD.get()));
        // add.accept(new ItemStack(ModItems.SHIMMER_HANDLE.get()));
        // add.accept(new ItemStack(ModItems.SHIMMER_HEAD.get()));
        // add.accept(new ItemStack(ModItems.SHIMMER_SLEDGE.get()));
        // add.accept(new ItemStack(ModItems.SINGULARITY.get()));
        // add.accept(new ItemStack(ModItems.SIOX.get()));
        // add.accept(new ItemStack(ModItems.SIPHON.get()));
        // add.accept(new ItemStack(ModItems.SMASHING_HAMMER.get()));
        // add.accept(new ItemStack(ModItems.SOLID_FUEL.get()));
        // add.accept(new ItemStack(ModItems.SOLID_FUEL_BF.get()));
        // add.accept(new ItemStack(ModItems.SOLID_FUEL_PRESTO.get()));
        // add.accept(new ItemStack(ModItems.SOLID_FUEL_PRESTO_BF.get()));
        // add.accept(new ItemStack(ModItems.SOLID_FUEL_PRESTO_TRIPLET.get()));
        // add.accept(new ItemStack(ModItems.SOLID_FUEL_PRESTO_TRIPLET_BF.get()));
        // add.accept(new ItemStack(ModItems.SOLINIUM_CORE.get()));
        // add.accept(new ItemStack(ModItems.SOLINIUM_IGNITER.get()));
        // add.accept(new ItemStack(ModItems.SOLINIUM_KIT.get()));
        // add.accept(new ItemStack(ModItems.SOLINIUM_PROPELLANT.get()));
        // add.accept(new ItemStack(ModItems.SOPSIGN.get()));
        // add.accept(new ItemStack(ModItems.SPAWN_DUCK.get()));
        // add.accept(new ItemStack(ModItems.SPAWN_UFO.get()));
        // add.accept(new ItemStack(ModItems.SPAWN_WORM.get()));
        // add.accept(new ItemStack(ModItems.SPHERE_STEEL.get()));
        // add.accept(new ItemStack(ModItems.SPIDER_MILK.get()));
        // add.accept(new ItemStack(ModItems.SPONGEBOB_MACARONI.get()));
        // add.accept(new ItemStack(ModItems.STAMP_357.get()));
        // add.accept(new ItemStack(ModItems.STAMP_44.get()));
        // add.accept(new ItemStack(ModItems.STAMP_50.get()));
        // add.accept(new ItemStack(ModItems.STAMP_9.get()));
        // add.accept(new ItemStack(ModItems.STARMETAL_LEGGINGS.get()));
        // add.accept(new ItemStack(ModItems.STARMETAL_CHESTPLATE.get()));
        // add.accept(new ItemStack(ModItems.STATIC_SANDWICH.get()));
        // add.accept(new ItemStack(ModItems.STEALTH_BOY.get()));
        // add.accept(new ItemStack(ModItems.STEAMSUIT_BOOTS.get()));
        // add.accept(new ItemStack(ModItems.STEAMSUIT_HELMET.get()));
        // add.accept(new ItemStack(ModItems.STEAMSUIT_LEGS.get()));
        // add.accept(new ItemStack(ModItems.STEAMSUIT_PLATE.get()));
        // add.accept(new ItemStack(ModItems.STEEL_LEGGINGS.get()));
        // add.accept(new ItemStack(ModItems.STEEL_CHESTPLATE.get()));
        // add.accept(new ItemStack(ModItems.STICK_C4.get()));
        // add.accept(new ItemStack(ModItems.STICK_DYNAMITE.get()));
        // add.accept(new ItemStack(ModItems.STICK_DYNAMITE_FISHING.get()));
        // add.accept(new ItemStack(ModItems.STICK_SEMTEX.get()));
        // add.accept(new ItemStack(ModItems.STICK_TNT.get()));
        // add.accept(new ItemStack(ModItems.STOPSIGN.get()));
        // add.accept(new ItemStack(ModItems.STRUCTURE_CUSTOMMACHINE.get()));
        // add.accept(new ItemStack(ModItems.SURVEY_SCANNER.get()));
        // add.accept(new ItemStack(ModItems.SYRINGE_ANTIDOTE.get()));
        // add.accept(new ItemStack(ModItems.SYRINGE_AWESOME.get()));
        // add.accept(new ItemStack(ModItems.SYRINGE_EMPTY.get()));
        // add.accept(new ItemStack(ModItems.SYRINGE_METAL_EMPTY.get()));
        // add.accept(new ItemStack(ModItems.SYRINGE_METAL_MEDX.get()));
        // add.accept(new ItemStack(ModItems.SYRINGE_METAL_PSYCHO.get()));
        // add.accept(new ItemStack(ModItems.SYRINGE_METAL_STIMPAK.get()));
        // add.accept(new ItemStack(ModItems.SYRINGE_METAL_SUPER.get()));
        // add.accept(new ItemStack(ModItems.SYRINGE_MKUNICORN.get()));
        // add.accept(new ItemStack(ModItems.SYRINGE_POISON.get()));
        // add.accept(new ItemStack(ModItems.SYRINGE_TAINT.get()));
        // add.accept(new ItemStack(ModItems.TANK_STEEL.get()));
        // add.accept(new ItemStack(ModItems.TAURUN_BOOTS.get()));
        // add.accept(new ItemStack(ModItems.TAURUN_HELMET.get()));
        // add.accept(new ItemStack(ModItems.TAURUN_LEGS.get()));
        // add.accept(new ItemStack(ModItems.TAURUN_PLATE.get()));
        // add.accept(new ItemStack(ModItems.TEM_FLAKES.get()));
        // add.accept(new ItemStack(ModItems.THERMO_ELEMENT.get()));
        // add.accept(new ItemStack(ModItems.THRUSTER_NUCLEAR.get()));
        // add.accept(new ItemStack(ModItems.TITANIUM_FILTER.get()));
        // add.accept(new ItemStack(ModItems.TITANIUM_LEGGINGS.get()));
        // add.accept(new ItemStack(ModItems.TITANIUM_CHESTPLATE.get()));
        // add.accept(new ItemStack(ModItems.TRENCHMASTER_BOOTS.get()));
        // add.accept(new ItemStack(ModItems.TRENCHMASTER_HELMET.get()));
        // add.accept(new ItemStack(ModItems.TRENCHMASTER_LEGS.get()));
        // add.accept(new ItemStack(ModItems.TRENCHMASTER_PLATE.get()));
        // add.accept(new ItemStack(ModItems.TRINITITE.get()));
        // add.accept(new ItemStack(ModItems.TSAR_CORE.get()));
        // add.accept(new ItemStack(ModItems.TSAR_KIT.get()));
        // add.accept(new ItemStack(ModItems.TURBINE_TUNGSTEN.get()));
        // add.accept(new ItemStack(ModItems.TURRET_CHIP.get()));
        // // Turrets and their ammunition. All of this sat commented out in the combat tab, so
        // // neither the turrets nor a single round were obtainable in creative at all.
        // add.accept(new ItemStack(ModBlocks.TURRET_SENTRY.get()));
        // add.accept(new ItemStack(ModBlocks.TURRET_CHEKHOV.get()));
        // add.accept(new ItemStack(ModBlocks.TURRET_FRIENDLY.get()));
        // add.accept(new ItemStack(ModBlocks.TURRET_JEREMY.get()));
        // add.accept(new ItemStack(ModBlocks.TURRET_TAUON.get()));
        // add.accept(new ItemStack(ModBlocks.TURRET_RICHARD.get()));
        // add.accept(new ItemStack(ModBlocks.TURRET_HOWARD.get()));
        // add.accept(new ItemStack(ModBlocks.TURRET_MAXWELL.get()));
        // add.accept(new ItemStack(ModBlocks.TURRET_FRITZ.get()));
        // add.accept(new ItemStack(ModBlocks.TURRET_ARTY.get()));
        // add.accept(new ItemStack(ModBlocks.TURRET_HIMARS.get()));

        // add.accept(new ItemStack(ModItems.UPGRADE_5G.get()));
        // add.accept(new ItemStack(ModItems.UPGRADE_SCREM.get()));
        // add.accept(new ItemStack(ModItems.AMMO_DGK.get()));
        // add.accept(new ItemStack(ModItems.ROCKET_HIMARS_STANDARD.get()));
        // add.accept(new ItemStack(ModItems.ROCKET_HIMARS_HE.get()));
        // add.accept(new ItemStack(ModItems.ROCKET_HIMARS_LAVA.get()));
        // add.accept(new ItemStack(ModItems.ROCKET_HIMARS_MINI_NUKE.get()));
        // add.accept(new ItemStack(ModItems.ROCKET_HIMARS_WP.get()));
        // add.accept(new ItemStack(ModItems.ROCKET_HIMARS_THERMOBARIC.get()));
        // add.accept(new ItemStack(ModItems.ROCKET_HIMARS_SINGLE.get()));
        // add.accept(new ItemStack(ModItems.ROCKET_HIMARS_SINGLE_TB.get()));
        // add.accept(new ItemStack(ModItems.TWINKIE.get()));
        // add.accept(new ItemStack(ModItems.ULLAPOOL_CABER.get()));
        // add.accept(new ItemStack(ModItems.UNDEFINED.get()));
        // add.accept(new ItemStack(ModItems.VOLCANIC_AXE.get()));
        // add.accept(new ItemStack(ModItems.VOLCANIC_PICKAXE.get()));
        // add.accept(new ItemStack(ModItems.WAND_D.get()));
        // add.accept(new ItemStack(ModItems.WAND_S.get()));
        // add.accept(new ItemStack(ModItems.WARHEAD_INCENDIARY_LARGE.get()));
        // add.accept(new ItemStack(ModItems.WASTE_MOX.get()));
        // add.accept(new ItemStack(ModItems.WASTE_PLATE_MOX.get()));
        // add.accept(new ItemStack(ModItems.WASTE_PLATE_PU238BE.get()));
        // add.accept(new ItemStack(ModItems.WASTE_PLATE_RA226BE.get()));
        // add.accept(new ItemStack(ModItems.WASTE_PLATE_SA326.get()));
        // add.accept(new ItemStack(ModItems.WASTE_PLATE_U233.get()));
        // add.accept(new ItemStack(ModItems.WASTE_PLATE_U235.get()));
        // add.accept(new ItemStack(ModItems.WASTE_PLATE_PU239.get()));
        // add.accept(new ItemStack(ModItems.WASTE_PLUTONIUM.get()));
        // add.accept(new ItemStack(ModItems.WASTE_SCHRABIDIUM.get()));
        // add.accept(new ItemStack(ModItems.WASTE_THORIUM.get()));
        // add.accept(new ItemStack(ModItems.WASTE_URANIUM.get()));
        // add.accept(new ItemStack(ModItems.WASTE_ZFB_MOX.get()));
        // add.accept(new ItemStack(ModItems.WATCH.get()));
        // add.accept(new ItemStack(ModItems.WD40.get()));
        // add.accept(new ItemStack(ModItems.WILD_P.get()));
        // add.accept(new ItemStack(ModItems.WINGS_LIMP.get()));
        // add.accept(new ItemStack(ModItems.WINGS_MURK.get()));
        // add.accept(new ItemStack(ModItems.WIRING_RED_COPPER.get()));
        // add.accept(new ItemStack(ModItems.WOOD_GAVEL.get()));
        // add.accept(new ItemStack(ModItems.WRENCH.get()));
        // add.accept(new ItemStack(ModItems.WRENCH_ARCHINEER.get()));
        // add.accept(new ItemStack(ModItems.WRENCH_FLIPPED.get()));
        // add.accept(new ItemStack(ModItems.XANAX.get()));
        // add.accept(new ItemStack(ModItems.ZIRCONIUM_LEGS.get()));

        // // --- Neu portierte, noch nicht einsortierte Pulver (zur Durchsicht) ---
        // add.accept(new ItemStack(ModItems.POWDER_SAWDUST.get()));
        // add.accept(new ItemStack(ModItems.POWDER_YELLOWCAKE.get()));
        // add.accept(new ItemStack(ModItems.POWDER_BALEFIRE.get()));
        // add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.PALEOGENITE, MaterialShape.POWDER)));
        // add.accept(new ItemStack(ModItems.POWDER_THERMITE.get()));
        // add.accept(new ItemStack(ModItems.POWDER_FERTILIZER.get()));
        // add.accept(new ItemStack(ModItems.POWDER_FLUX.get()));
        // add.accept(new ItemStack(ModItems.POWDER_MAGIC.get()));
        // add.accept(new ItemStack(ModItems.POWDER_ICE.get()));
        // add.accept(new ItemStack(ModItems.POWDER_SPARK_MIX.get()));
        // add.accept(new ItemStack(ModItems.POWDER_SEMTEX_MIX.get()));
        // add.accept(new ItemStack(ModItems.POWDER_DESH_READY.get()));
        // add.accept(new ItemStack(ModItems.POWDER_COLTAN.get()));

        // // RBMK-Teile (2026-08-09, aus Fuel-Tab verschoben, noch nicht vom Entwickler kontrolliert)
        // add.accept(new ItemStack(ModItems.RBMK_FUEL_DRX.get()));
        // add.accept(new ItemStack(ModBlocks.RBMK_ROD.get()));
        // add.accept(new ItemStack(ModBlocks.RBMK_ROD_MOD.get()));
        // add.accept(new ItemStack(ModBlocks.RBMK_ROD_REASIM.get()));
        // add.accept(new ItemStack(ModBlocks.RBMK_ROD_REASIM_MOD.get()));
        // add.accept(new ItemStack(ModBlocks.RBMK_CONTROL.get()));
        // add.accept(new ItemStack(ModBlocks.RBMK_CONTROL_MOD.get()));
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
        // add.accept(new ItemStack(ModBlocks.DECO_RBMK.get()));
        // add.accept(new ItemStack(ModBlocks.DECO_RBMK_SMOOTH.get()));
        // add.accept(new ItemStack(ModBlocks.DECO_RBMK_PANEL.get()));
        // add.accept(new ItemStack(ModBlocks.DECO_RBMK_SMOOTH_PANEL.get()));
        // add.accept(new ItemStack(ModBlocks.DECO_RBMK_PANEL_SLAB2.get()));
        // add.accept(new ItemStack(ModBlocks.DECO_RBMK_SMOOTH_PANEL_SLAB2.get()));
        // add.accept(new ItemStack(ModBlocks.BLOCK_GRAPHITE.get()));
        // add.accept(new ItemStack(ModBlocks.STEEL_GRATE.get()));
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
        // add.accept(new ItemStack(ModBlocks.RBMK_DISPLAY_BLANK.get()));
        // add.accept(new ItemStack(ModBlocks.RBMK_DEBRIS.get()));
        // add.accept(new ItemStack(ModBlocks.RBMK_DEBRIS_BURNING.get()));
        // add.accept(new ItemStack(ModBlocks.RBMK_DEBRIS_DIGAMMA.get()));
        // add.accept(new ItemStack(ModBlocks.RBMK_DEBRIS_RADIATING.get()));
        // add.accept(new ItemStack(ModItems.RBMK_LID.get()));
        // add.accept(new ItemStack(ModItems.RBMK_LID_GLASS.get()));
        // add.accept(new ItemStack(ModItems.RBMK_FUEL_EMPTY.get()));
        // add.accept(new ItemStack(ModItems.RBMK_FUEL_TEST.get()));
        // add.accept(new ItemStack(ModItems.RBMK_FUEL_HEU235.get()));
        // add.accept(new ItemStack(ModItems.RBMK_FUEL_LEP.get()));
        // add.accept(new ItemStack(ModItems.RBMK_FUEL_HEP.get()));
        // add.accept(new ItemStack(ModItems.RBMK_FUEL_MOX.get()));
        // add.accept(new ItemStack(ModItems.RBMK_PELLET_HEU235.get()));
        // add.accept(new ItemStack(ModItems.RBMK_PELLET_LEP.get()));
        // add.accept(new ItemStack(ModItems.RBMK_PELLET_HEP.get()));
        // add.accept(new ItemStack(ModItems.RBMK_PELLET_MOX.get()));
    }

    /**
     * Ключ группы предметов для DEV-вкладки; сортировка по ключу задаёт порядок групп.
     */
    private static String devTabCategory(Item item) {
        if (item instanceof ArmorItem || item instanceof com.hbm_m.powerarmor.ModArmorFSBPowered) {
            return "10_armor";
        }
        String cls = item.getClass().getSimpleName().toLowerCase(java.util.Locale.ROOT);
        if (item instanceof ProjectileWeaponItem || cls.contains("gun")) {
            return "20_weapons";
        }
        if (item instanceof DiggerItem || item instanceof SwordItem || item instanceof TieredItem
                || item instanceof ShearsItem || cls.contains("drill") || cls.contains("saw")) {
            return "30_tools";
        }
        if (com.hbm_m.platform.ItemHooks.isEdible(item)) {
            return "40_food";
        }
        return "90_" + item.getClass().getSimpleName();
    }


    // ТОПЛИВО И ЭЛЕМЕНТЫ МЕХАНИЗМОВ

        private static ItemStack createChargedArmorStack(Item item) {
        ItemStack stack = new ItemStack(item);

        // Проверяем, является ли предмет силовой броней
        if (item instanceof com.hbm_m.powerarmor.ModArmorFSBPowered powerArmor) {
            // Получаем максимальную емкость и устанавливаем полный заряд
            long maxCapacity = powerArmor.getMaxCharge(stack);
            StackNbt.putLong(stack, "charge", maxCapacity);
        }

        return stack;
    }

    private static void addMeta(java.util.function.Consumer<ItemStack> add, String id) {
        net.minecraft.world.item.Item item = com.hbm_m.item.PartTabMetaItems.itemOrNull(id);
        if (item != null) {
            add.accept(new ItemStack(item));
        } else {
            MainRegistry.LOGGER.warn("Skipping missing creative tab PartTabMetaItem: {}", id);
        }
    }
}