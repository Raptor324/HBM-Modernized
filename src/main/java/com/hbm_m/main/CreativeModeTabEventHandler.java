package com.hbm_m.main;

import java.util.HashSet;
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
import com.hbm_m.item.fekal_electric.ModBatteryItem;
import com.hbm_m.item.liquids.FluidBarrelItem;
import com.hbm_m.item.liquids.FluidIdentifierItem;
import com.hbm_m.item.material.MaterialShape;
import com.hbm_m.item.material.ModMaterialItems;
import com.hbm_m.item.material.ModMaterials;

import dev.architectury.utils.Env;
import dev.architectury.utils.EnvExecutor;
import com.hbm_m.platform.PlatformHooks;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.*;
//? if forge {
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
//?} elif neoforge {
/*import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
*///?}
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
            acceptor.accept(new ItemStack(ModItems.MUSIC_DISC_BUNKER.get()),
                    CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
            event.accept(new ItemStack(ModItems.MUSIC_DISC_CH.get()),
                    CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
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
            CompoundTag itemTag = PlatformHooks.getItemTag(stack);
            String tag = itemTag == null ? "" : itemTag.toString();
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
            CompoundTag itemTag = PlatformHooks.getItemTag(stack);
            String tag = itemTag == null ? "" : itemTag.toString();
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
            CompoundTag itemTag = PlatformHooks.getItemTag(stack);
            String tag = itemTag == null ? "" : itemTag.toString();
            if (!seen.add(itemId + "|" + tag)) {
                return;
            }
            acceptor.accept(stack, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
        };
        add.accept(new ItemStack(ModItems.BOT_PRIME_SPAWN_EGG.get()));
        add.accept(new ItemStack(ModItems.UFO_SPAWN_EGG.get()));
        add.accept(new ItemStack(ModItems.RAD_BEAST_SPAWN_EGG.get()));
        add.accept(new ItemStack(ModItems.MASKMAN_SPAWN_EGG.get()));
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

    // ==================== Вкладки мода (порядок оригинала 1.7.10) ====================

        /** populatePartsTab: порядок из оригинального 1.7.10 (ModBlocks/ModItems, вкладка parts); отсутствующие в порте предметы пропущены. */
    public static void populatePartsTab(BiConsumer<ItemStack, CreativeModeTab.TabVisibility> acceptor) {
        Consumer<ItemStack> add = stack -> acceptor.accept(stack, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);

        // === Вкладка Parts: точный порядок оригинала 1.7.10 — scripts/parts_tab_true.tsv (1032 слота = id asc x getSubItems).
        // Все мета-предметы развёрнуты: отдельные предметы на меты (circuit, plate_cast, plate_welded, wire_fine,
        // wire_dense, bedrock_ore, фрагменты, tar/ash/coke/briquette/chunk/ingot_raw и dye/crayon/casing/shell/pipe/
        // bolt/gear_large/part_*/waste_*/nuclear_waste_* — таблица в com.hbm_m.item.PartTabMetaItems). ===

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
        add.accept(new ItemStack(ModItems.INGOT_HIGHSPEED_STEEL.get()));                                        // 4146
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
        add.accept(new ItemStack(ModItems.INGOT_REDSTONE.get()));                                               // 4205/1 (stub)
        add.accept(new ItemStack(ModItems.INGOT_SLAG.get()));                                                   // 4205/41 (stub)
        add.accept(new ItemStack(ModItems.INGOT_BORAX.get()));                                                  // 4205/501 (stub)
        add.accept(new ItemStack(ModItems.INGOT_SODIUM.get()));                                                 // 4205/1100 (stub)
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.STRONTIUM, MaterialShape.INGOT)));          // 4205/3800
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.NEODYMIUM, MaterialShape.INGOT)));          // 4205/6000
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.URANIUM, MaterialShape.BILLET)));           // 4206
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.URANIUM233, MaterialShape.BILLET)));        // 4207
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.URANIUM235, MaterialShape.BILLET)));        // 4208
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.URANIUM238, MaterialShape.BILLET)));        // 4209
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.THORIUM232, MaterialShape.BILLET)));        // 4211
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.PLUTONIUM, MaterialShape.BILLET)));         // 4212
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.PLUTONIUM238, MaterialShape.BILLET)));      // 4213
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.PLUTONIUM239, MaterialShape.BILLET)));      // 4214
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.PLUTONIUM240, MaterialShape.BILLET)));      // 4215
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.PLUTONIUM241, MaterialShape.BILLET)));      // 4216
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.PU_MIX, MaterialShape.BILLET)));            // 4217
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.AM241, MaterialShape.BILLET)));             // 4218
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.AM242, MaterialShape.BILLET)));             // 4219
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.AM_MIX, MaterialShape.BILLET)));            // 4220
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.NEPTUNIUM, MaterialShape.BILLET)));         // 4221
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.POLONIUM, MaterialShape.BILLET)));          // 4222
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.TECHNETIUM, MaterialShape.BILLET)));        // 4223
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.COBALT, MaterialShape.BILLET)));            // 4224
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.CO60, MaterialShape.BILLET)));              // 4225
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.SR90, MaterialShape.BILLET)));              // 4226
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.AU198, MaterialShape.BILLET)));             // 4227
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.PB209, MaterialShape.BILLET)));             // 4228
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.RA226, MaterialShape.BILLET)));             // 4229
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.ACTINIUM, MaterialShape.BILLET)));          // 4230
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.SCHRABIDIUM, MaterialShape.BILLET)));       // 4231
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.SOLINIUM, MaterialShape.BILLET)));          // 4232
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.GH336, MaterialShape.BILLET)));             // 4233
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.AUSTRALIUM, MaterialShape.BILLET)));        // 4234
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.AUSTRALIUM_LESSER, MaterialShape.BILLET))); // 4235
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.AUSTRALIUM_GREATER, MaterialShape.BILLET)));// 4236
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.URANIUM_FUEL, MaterialShape.BILLET)));      // 4237
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.THORIUM_FUEL, MaterialShape.BILLET)));      // 4238
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.PLUTONIUM_FUEL, MaterialShape.BILLET)));    // 4239
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.NEPTUNIUM_FUEL, MaterialShape.BILLET)));    // 4240
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.MOX_FUEL, MaterialShape.BILLET)));          // 4241
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.AMERICIUM_FUEL, MaterialShape.BILLET)));    // 4242
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.LES_FUEL, MaterialShape.BILLET)));          // 4243
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.SCHRABIDIUM_FUEL, MaterialShape.BILLET)));  // 4244
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.HES, MaterialShape.BILLET)));               // 4245
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.PO210BE, MaterialShape.BILLET)));           // 4246
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.RA226BE, MaterialShape.BILLET)));           // 4247
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.PU238BE, MaterialShape.BILLET)));           // 4248
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.BERYLLIUM, MaterialShape.BILLET)));         // 4249
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.BISMUTH, MaterialShape.BILLET)));           // 4250
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.SILICON, MaterialShape.BILLET)));           // 4251
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.ZIRCONIUM, MaterialShape.BILLET)));         // 4252
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.ZFB_BISMUTH, MaterialShape.BILLET)));       // 4253
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.ZFB_PU241, MaterialShape.BILLET)));         // 4254
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.ZFB_AM_MIX, MaterialShape.BILLET)));        // 4255
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.YHARONITE, MaterialShape.BILLET)));         // 4256
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.BALEFIRE_GOLD, MaterialShape.BILLET)));     // 4257
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.FLASHLEAD, MaterialShape.BILLET)));         // 4258
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.NUCLEAR_WASTE, MaterialShape.BILLET)));     // 4259
        add.accept(new ItemStack(ModItems.CINNEBAR.get()));                                                     // 4260
        add.accept(new ItemStack(ModItems.NUGGET_MERCURY_TINY.get()));                                          // 4261
        add.accept(new ItemStack(ModItems.NUGGET_MERCURY.get()));                                               // 4262
        add.accept(new ItemStack(ModItems.BOTTLE_MERCURY.get()));                                               // 4263
        add.accept(new ItemStack(ModItems.COAL_COKE.get()));                                                    // 4264/0 (stub)
        add.accept(new ItemStack(ModItems.LIGNITE_COKE.get()));                                                 // 4264/1 (stub)
        add.accept(new ItemStack(ModItems.COKE_PETROLEUM.get()));                                               // 4264/2
        add.accept(new ItemStack(ModItems.LIGNITE.get()));                                                      // 4265
        add.accept(new ItemStack(ModItems.COAL_INFERNAL.get()));                                                // 4266
        add.accept(new ItemStack(ModItems.COAL_BRIQUETTE.get()));                                               // 4268/0 (stub)
        add.accept(new ItemStack(ModItems.LIGNITE_BRIQUETTE.get()));                                            // 4268/1 (stub)
        add.accept(new ItemStack(ModItems.SAWDUST_BRIQUETTE.get()));                                            // 4268/2 (stub)
        add.accept(new ItemStack(ModItems.SULFUR.get()));                                                       // 4269
        add.accept(new ItemStack(ModItems.NITER.get()));                                                        // 4270 (stub)
        add.accept(new ItemStack(ModItems.NITRA.get()));                                                        // 4271
        add.accept(new ItemStack(ModItems.NITRA_SMALL.get()));                                                  // 4272
        add.accept(new ItemStack(ModItems.FLUORITE.get()));                                                     // 4273
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.COAL, MaterialShape.POWDER)));              // 4274
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.COAL, MaterialShape.POWDER_TINY)));         // 4275
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.IRON, MaterialShape.POWDER)));              // 4276
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.GOLD, MaterialShape.POWDER)));              // 4277
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.LAPIS, MaterialShape.POWDER)));             // 4278
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.QUARTZ, MaterialShape.POWDER)));            // 4279
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.DIAMOND, MaterialShape.POWDER)));           // 4280
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.EMERALD, MaterialShape.POWDER)));           // 4281
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.URANIUM, MaterialShape.POWDER)));           // 4282
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.PLUTONIUM, MaterialShape.POWDER)));         // 4283
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.NEPTUNIUM, MaterialShape.POWDER)));         // 4284
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.POLONIUM, MaterialShape.POWDER)));          // 4285
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.CO60, MaterialShape.POWDER)));              // 4286
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.SR90, MaterialShape.POWDER)));              // 4287
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.SR90, MaterialShape.POWDER_TINY)));         // 4288
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.I131, MaterialShape.POWDER)));              // 4289
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.I131, MaterialShape.POWDER_TINY)));         // 4290
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.XE135, MaterialShape.POWDER)));             // 4291
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.XE135, MaterialShape.POWDER_TINY)));        // 4292
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.CS137, MaterialShape.POWDER)));             // 4293
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.CS137, MaterialShape.POWDER_TINY)));        // 4294
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.AU198, MaterialShape.POWDER)));             // 4295
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.RA226, MaterialShape.POWDER)));             // 4296
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.AT209, MaterialShape.POWDER)));             // 4297 (powder_at209)
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.TITANIUM, MaterialShape.POWDER)));          // 4298
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.COPPER, MaterialShape.POWDER)));            // 4299
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.RED_COPPER, MaterialShape.POWDER)));        // 4300
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.TUNGSTEN, MaterialShape.POWDER)));          // 4301
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.ALUMINUM, MaterialShape.POWDER)));          // 4302
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.STEEL, MaterialShape.POWDER)));             // 4303
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.STEEL, MaterialShape.POWDER_TINY)));        // 4304
        add.accept(new ItemStack(ModItems.POWDER_TCALLOY.get()));                                               // 4305 (stub)
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.LEAD, MaterialShape.POWDER)));              // 4306
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.BISMUTH, MaterialShape.POWDER)));           // 4307
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.CALCIUM, MaterialShape.POWDER)));           // 4308
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.CADMIUM, MaterialShape.POWDER)));           // 4309
        add.accept(new ItemStack(ModItems.POWDER_COLTAN.get()));                                                // 4310
        add.accept(new ItemStack(ModItems.POWDER_COLTAN_PURE.get()));                                           // 4311 (stub)
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.TANTALIUM, MaterialShape.POWDER)));         // 4312
        add.accept(new ItemStack(ModItems.POWDER_TEKTITE.get()));                                               // 4313 (stub)
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.PALEOGENITE, MaterialShape.POWDER)));       // 4314
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.PALEOGENITE, MaterialShape.POWDER_TINY)));  // 4315
        add.accept(new ItemStack(ModItems.POWDER_IMPURE_OSMIRIDIUM.get()));                                     // 4316 (stub)
        add.accept(new ItemStack(ModItems.BORAX.get()));                                                        // 4317
        add.accept(new ItemStack(ModItems.POWDER_CHLOROCALCITE.get()));                                         // 4318
        add.accept(new ItemStack(ModItems.MOLYSITE.get()));                                                     // 4319
        add.accept(new ItemStack(ModItems.POWDER_YELLOWCAKE.get()));                                            // 4320
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.BERYLLIUM, MaterialShape.POWDER)));         // 4321
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.DURA_STEEL, MaterialShape.POWDER)));        // 4322
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.POLYMER, MaterialShape.POWDER)));           // 4323
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.BAKELITE, MaterialShape.POWDER)));          // 4324
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.SCHRABIDIUM, MaterialShape.POWDER)));       // 4325
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.SCHRABIDATE, MaterialShape.POWDER)));       // 4326
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.MAGNETIZED_TUNGSTEN, MaterialShape.POWDER)));// 4327
        add.accept(new ItemStack(ModItems.POWDER_CHLOROPHYTE.get()));                                           // 4328 (stub)
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.COMBINE_STEEL, MaterialShape.POWDER)));     // 4329
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.LITHIUM, MaterialShape.POWDER)));           // 4330
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.LITHIUM, MaterialShape.POWDER_TINY)));      // 4331
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.ZIRCONIUM, MaterialShape.POWDER)));         // 4332
        add.accept(new ItemStack(ModItems.POWDER_SODIUM.get()));                                                // 4333
        add.accept(new ItemStack(ModItems.LIGNITE_POWDER.get()));                                               // 4334
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.IODINE, MaterialShape.POWDER)));            // 4335
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.THORIUM, MaterialShape.POWDER)));           // 4336
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.NEODYMIUM, MaterialShape.POWDER)));         // 4337
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.NEODYMIUM, MaterialShape.POWDER_TINY)));    // 4338
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.ASTATINE, MaterialShape.POWDER)));          // 4339 (powder_astatine)
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.CAESIUM, MaterialShape.POWDER)));           // 4340
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.AUSTRALIUM, MaterialShape.POWDER)));        // 4341
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.STRONTIUM, MaterialShape.POWDER)));         // 4342
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.COBALT, MaterialShape.POWDER)));            // 4343
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.COBALT, MaterialShape.POWDER_TINY)));       // 4344
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.BROMINE, MaterialShape.POWDER)));           // 4345
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.NIOBIUM, MaterialShape.POWDER)));           // 4346
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.NIOBIUM, MaterialShape.POWDER_TINY)));      // 4347
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.TENNESSINE, MaterialShape.POWDER)));        // 4348
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.CERIUM, MaterialShape.POWDER)));            // 4349
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.CERIUM, MaterialShape.POWDER_TINY)));       // 4350
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.LANTHANIUM, MaterialShape.POWDER)));        // 4351
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.LANTHANIUM, MaterialShape.POWDER_TINY)));   // 4352
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.ACTINIUM, MaterialShape.POWDER)));          // 4353
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.ACTINIUM, MaterialShape.POWDER_TINY)));     // 4354
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.BORON, MaterialShape.POWDER)));             // 4355
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.BORON, MaterialShape.POWDER_TINY)));        // 4356
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.ASBESTOS, MaterialShape.POWDER)));          // 4357
        add.accept(new ItemStack(ModItems.POWDER_MAGIC.get()));                                                 // 4358
        add.accept(new ItemStack(ModItems.POWDER_SAWDUST.get()));                                               // 4359
        add.accept(new ItemStack(ModItems.POWDER_FLUX.get()));                                                  // 4360
        add.accept(new ItemStack(ModItems.POWDER_FERTILIZER.get()));                                            // 4361
        add.accept(new ItemStack(ModItems.POWDER_BALEFIRE.get()));                                              // 4362
        add.accept(new ItemStack(ModItems.POWDER_SEMTEX_MIX.get()));                                            // 4363
        add.accept(new ItemStack(ModItems.POWDER_DESH_MIX.get()));                                              // 4364
        add.accept(new ItemStack(ModItems.POWDER_DESH_READY.get()));                                            // 4365
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.DESH, MaterialShape.POWDER)));              // 4366
        add.accept(new ItemStack(ModItems.POWDER_NITAN_MIX.get()));                                             // 4367
        add.accept(new ItemStack(ModItems.POWDER_SPARK_MIX.get()));                                             // 4368
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.METEORITE, MaterialShape.POWDER)));         // 4369
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.METEORITE, MaterialShape.POWDER_TINY)));    // 4370
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.EUPHEMIUM, MaterialShape.POWDER)));         // 4371
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.DINEUTRONIUM, MaterialShape.POWDER)));      // 4372
        add.accept(new ItemStack(ModItems.DUST.get()));                                                         // 4373
        add.accept(new ItemStack(ModItems.DUST_TINY.get()));                                                    // 4374
        add.accept(new ItemStack(ModItems.FALLOUT.get()));                                                      // 4375
        add.accept(new ItemStack(ModItems.ASH_WOOD.get()));                                                     // 4376/0
        add.accept(new ItemStack(ModItems.ASH_COAL.get()));                                                     // 4376/1
        add.accept(new ItemStack(ModItems.ASH_MISC.get()));                                                     // 4376/2
        add.accept(new ItemStack(ModItems.ASH_FLY.get()));                                                      // 4376/3
        add.accept(new ItemStack(ModItems.ASH_SOOT.get()));                                                   // 4376/4
        add.accept(new ItemStack(ModItems.FULLERENE.get()));                                                    // 4376/5 (powder_ash.fullerene)
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.LIMESTONE, MaterialShape.POWDER)));         // 4377
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.CEMENT, MaterialShape.POWDER)));            // 4378
        add.accept(new ItemStack(ModItems.FIRE_POWDER.get()));                                                  // 4379
        add.accept(new ItemStack(ModItems.POWDER_ICE.get()));                                                   // 4380
        add.accept(new ItemStack(ModItems.POWDER_POISON.get()));                                                // 4381 (stub)
        add.accept(new ItemStack(ModItems.POWDER_THERMITE.get()));                                              // 4382
        add.accept(new ItemStack(ModItems.POWDER_POWER.get()));                                                 // 4383 (powder_power)
        add.accept(new ItemStack(ModItems.CORDITE.get()));                                                      // 4384
        add.accept(new ItemStack(ModItems.BALLISTITE.get()));                                                   // 4385
        add.accept(new ItemStack(ModItems.BALL_DYNAMITE.get()));                                                // 4386
        add.accept(new ItemStack(ModItems.BALL_TNT.get()));                                                     // 4387
        add.accept(new ItemStack(ModItems.BALL_TATB.get()));                                                    // 4388
        add.accept(new ItemStack(ModItems.BALL_RESIN.get()));                                                   // 4389
        add.accept(new ItemStack(ModItems.BALL_FIRECLAY.get()));                                                // 4390
        add.accept(new ItemStack(ModItems.BEDROCK_ORE_BASE.get()));                                             // 4402
        java.util.Map<String, net.minecraft.world.item.Item> bedrockVariants = new java.util.HashMap<>();
        for (var supplier : ModItems.BEDROCK_ORE_ALL_VARIANTS) {
            com.hbm_m.item.industrial.ItemBedrockOreGraded item =
                    (com.hbm_m.item.industrial.ItemBedrockOreGraded) supplier.get();
            bedrockVariants.put(item.getGrade().key + "|" + item.getOreType(), item);
        }
        for (com.hbm_m.worldgen.BedrockOreDensity.Type oreType : com.hbm_m.worldgen.BedrockOreDensity.Type.values()) {
            for (com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade grade : com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade.values()) {
                net.minecraft.world.item.Item variant = bedrockVariants.get(grade.key + "|" + oreType);
                if (variant != null) add.accept(new ItemStack(variant));
            }
        }
        add.accept(new ItemStack(ModItems.BEDROCK_ORE_FRAGMENT_COAL.get()));                                  // 4404/600 (COAL)
        add.accept(new ItemStack(ModItems.BEDROCK_ORE_FRAGMENT_LIGNITE.get()));                               // 4404/601 (LIGNITE)
        add.accept(new ItemStack(ModItems.BEDROCK_ORE_FRAGMENT_DIAMOND.get()));                               // 4404/1430 (DIAMOND)
        add.accept(new ItemStack(ModItems.BEDROCK_ORE_FRAGMENT_IRON.get()));                                  // 4404/2600 (IRON)
        add.accept(new ItemStack(ModItems.BEDROCK_ORE_FRAGMENT_GOLD.get()));                                  // 4404/7900 (GOLD)
        add.accept(new ItemStack(ModItems.BEDROCK_ORE_FRAGMENT_REDSTONE.get()));                              // 4404/1 (REDSTONE)
        add.accept(new ItemStack(ModItems.BEDROCK_ORE_FRAGMENT_BAUXITE.get()));                               // 4404/2902 (BAUXITE)
        add.accept(new ItemStack(ModItems.BEDROCK_ORE_FRAGMENT_CRYOLITE.get()));                              // 4404/2903 (CRYOLITE)
        add.accept(new ItemStack(ModItems.BEDROCK_ORE_FRAGMENT_URANIUM.get()));                               // 4404/9200 (URANIUM)
        add.accept(new ItemStack(ModItems.BEDROCK_ORE_FRAGMENT_U238.get()));                                  // 4404/9238 (U238)
        add.accept(new ItemStack(ModItems.BEDROCK_ORE_FRAGMENT_THORIUM.get()));                               // 4404/9032 (THORIUM)
        add.accept(new ItemStack(ModItems.BEDROCK_ORE_FRAGMENT_PO210.get()));                                 // 4404/8410 (POLONIUM)
        add.accept(new ItemStack(ModItems.BEDROCK_ORE_FRAGMENT_TC99.get()));                                  // 4404/4399 (TC99)
        add.accept(new ItemStack(ModItems.BEDROCK_ORE_FRAGMENT_RA226.get()));                                 // 4404/8826 (RADIUM)
        add.accept(new ItemStack(ModItems.BEDROCK_ORE_FRAGMENT_TITANIUM.get()));                              // 4404/2200 (TITANIUM)
        add.accept(new ItemStack(ModItems.BEDROCK_ORE_FRAGMENT_COPPER.get()));                                // 4404/2900 (COPPER)
        add.accept(new ItemStack(ModItems.BEDROCK_ORE_FRAGMENT_TUNGSTEN.get()));                              // 4404/7400 (TUNGSTEN)
        add.accept(new ItemStack(ModItems.BEDROCK_ORE_FRAGMENT_ALUMINIUM.get()));                             // 4404/1300 (ALUMINIUM)
        add.accept(new ItemStack(ModItems.BEDROCK_ORE_FRAGMENT_LEAD.get()));                                  // 4404/8200 (LEAD)
        add.accept(new ItemStack(ModItems.BEDROCK_ORE_FRAGMENT_BISMUTH.get()));                               // 4404/8300 (BISMUTH)
        add.accept(new ItemStack(ModItems.BEDROCK_ORE_FRAGMENT_TANTALIUM.get()));                             // 4404/7300 (TANTALIUM)
        add.accept(new ItemStack(ModItems.BEDROCK_ORE_FRAGMENT_NEODYMIUM.get()));                             // 4404/6000 (NEODYMIUM)
        add.accept(new ItemStack(ModItems.BEDROCK_ORE_FRAGMENT_NIOBIUM.get()));                               // 4404/4100 (NIOBIUM)
        add.accept(new ItemStack(ModItems.BEDROCK_ORE_FRAGMENT_BERYLLIUM.get()));                             // 4404/400 (BERYLLIUM)
        add.accept(new ItemStack(ModItems.BEDROCK_ORE_FRAGMENT_EMERALD.get()));                               // 4404/401 (EMERALD)
        add.accept(new ItemStack(ModItems.BEDROCK_ORE_FRAGMENT_COBALT.get()));                                // 4404/2700 (COBALT)
        add.accept(new ItemStack(ModItems.BEDROCK_ORE_FRAGMENT_BORON.get()));                                 // 4404/500 (BORON)
        add.accept(new ItemStack(ModItems.BEDROCK_ORE_FRAGMENT_BORAX.get()));                                 // 4404/501 (BORAX)
        add.accept(new ItemStack(ModItems.BEDROCK_ORE_FRAGMENT_LANTHANIUM.get()));                            // 4404/5700 (LANTHANIUM)
        add.accept(new ItemStack(ModItems.BEDROCK_ORE_FRAGMENT_ZIRCONIUM.get()));                             // 4404/4000 (ZIRCONIUM)
        add.accept(new ItemStack(ModItems.BEDROCK_ORE_FRAGMENT_SODIUM.get()));                                // 4404/1100 (SODIUM)
        add.accept(new ItemStack(ModItems.BEDROCK_ORE_FRAGMENT_SODALITE.get()));                              // 4404/1101 (SODALITE)
        add.accept(new ItemStack(ModItems.BEDROCK_ORE_FRAGMENT_STRONTIUM.get()));                             // 4404/3800 (STRONTIUM)
        add.accept(new ItemStack(ModItems.BEDROCK_ORE_FRAGMENT_LITHIUM.get()));                               // 4404/300 (LITHIUM)
        add.accept(new ItemStack(ModItems.BEDROCK_ORE_FRAGMENT_SULFUR.get()));                                // 4404/1600 (SULFUR)
        add.accept(new ItemStack(ModItems.BEDROCK_ORE_FRAGMENT_KNO.get()));                                   // 4404/700 (KNO)
        add.accept(new ItemStack(ModItems.BEDROCK_ORE_FRAGMENT_FLUORITE.get()));                              // 4404/900 (FLUORITE)
        add.accept(new ItemStack(ModItems.BEDROCK_ORE_FRAGMENT_PHOSPHORUS.get()));                            // 4404/1500 (PHOSPHORUS)
        add.accept(new ItemStack(ModItems.BEDROCK_ORE_FRAGMENT_CHLOROCALCITE.get()));                         // 4404/1701 (CHLOROCALCITE)
        add.accept(new ItemStack(ModItems.BEDROCK_ORE_FRAGMENT_MOLYSITE.get()));                              // 4404/1702 (MOLYSITE)
        add.accept(new ItemStack(ModItems.BEDROCK_ORE_FRAGMENT_CINNABAR.get()));                              // 4404/8001 (CINNABAR)
        add.accept(new ItemStack(ModItems.BEDROCK_ORE_FRAGMENT_SILICON.get()));                               // 4404/1400 (SILICON)
        add.accept(new ItemStack(ModItems.BEDROCK_ORE_FRAGMENT_ASBESTOS.get()));                              // 4404/1401 (ASBESTOS)
        add.accept(new ItemStack(ModItems.BEDROCK_ORE_FRAGMENT_RARE_EARTH.get()));                            // 4404/20000 (RAREEARTH)
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.COAL, MaterialShape.CRYSTAL)));             // 4405
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.IRON, MaterialShape.CRYSTAL)));             // 4406
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.GOLD, MaterialShape.CRYSTAL)));             // 4407
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.REDSTONE, MaterialShape.CRYSTAL)));         // 4408
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.LAPIS, MaterialShape.CRYSTAL)));            // 4409
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.DIAMOND, MaterialShape.CRYSTAL)));          // 4410
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.URANIUM, MaterialShape.CRYSTAL)));          // 4411
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.THORIUM, MaterialShape.CRYSTAL)));          // 4412
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.PLUTONIUM, MaterialShape.CRYSTAL)));        // 4413
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.TITANIUM, MaterialShape.CRYSTAL)));         // 4414
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.SULFUR, MaterialShape.CRYSTAL)));           // 4415
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.NITER, MaterialShape.CRYSTAL)));            // 4416
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.COPPER, MaterialShape.CRYSTAL)));           // 4417
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.TUNGSTEN, MaterialShape.CRYSTAL)));         // 4418
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.ALUMINIUM, MaterialShape.CRYSTAL)));        // 4419
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.FLUORITE, MaterialShape.CRYSTAL)));         // 4420
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.BERYLLIUM, MaterialShape.CRYSTAL)));        // 4421
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.LEAD, MaterialShape.CRYSTAL)));             // 4422
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.SCHRARANIUM, MaterialShape.CRYSTAL)));      // 4423
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.SCHRABIDIUM, MaterialShape.CRYSTAL)));      // 4424
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.RARE, MaterialShape.CRYSTAL)));             // 4425
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.PHOSPHORUS, MaterialShape.CRYSTAL)));       // 4426
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.LITHIUM, MaterialShape.CRYSTAL)));          // 4427
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.COBALT, MaterialShape.CRYSTAL)));           // 4428
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.STARMETAL, MaterialShape.CRYSTAL)));        // 4429
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.CINNEBAR, MaterialShape.CRYSTAL)));         // 4430
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.TRIXITE, MaterialShape.CRYSTAL)));          // 4431
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.OSMIRIDIUM, MaterialShape.CRYSTAL)));       // 4432
        add.accept(new ItemStack(ModItems.GEM_SODALITE.get()));                                                 // 4433
        add.accept(new ItemStack(ModItems.GEM_TANTALIUM.get()));                                                // 4434
        add.accept(new ItemStack(ModItems.GEM_VOLCANIC.get()));                                                 // 4435
        add.accept(new ItemStack(ModItems.GEM_RAD.get()));                                                      // 4436
        add.accept(new ItemStack(ModItems.GEM_ALEXANDRITE.get()));                                              // 4437
        add.accept(new ItemStack(ModItems.FRAGMENT_NEODYMIUM.get()));                                           // 4438
        add.accept(new ItemStack(ModItems.FRAGMENT_COBALT.get()));                                              // 4439
        add.accept(new ItemStack(ModItems.FRAGMENT_NIOBIUM.get()));                                             // 4440
        add.accept(new ItemStack(ModItems.FRAGMENT_CERIUM.get()));                                              // 4441
        add.accept(new ItemStack(ModItems.FRAGMENT_LANTHANIUM.get()));                                          // 4442
        add.accept(new ItemStack(ModItems.FRAGMENT_ACTINIUM.get()));                                            // 4443
        add.accept(new ItemStack(ModItems.FRAGMENT_BORON.get()));                                               // 4444
        add.accept(new ItemStack(ModItems.FRAGMENT_METEORITE.get()));                                           // 4445
        add.accept(new ItemStack(ModItems.FRAGMENT_COLTAN.get()));                                              // 4446
        add.accept(new ItemStack(ModItems.RAREGROUND_ORE_CHUNK.get()));                                         // 4447/0
        add.accept(new ItemStack(ModItems.MALACHITE_CHUNK.get()));                                              // 4447/1
        add.accept(new ItemStack(ModItems.CRYOLITE_CHUNK.get()));                                               // 4447/2 (chunk_ore.cryolite)
        add.accept(new ItemStack(ModItems.MOONSTONE.get()));                                                    // 4447/3 (stub)
        add.accept(new ItemStack(ModItems.BIOMASS.get()));                                                      // 4448
        add.accept(new ItemStack(ModItems.BIOMASS_COMPRESSED.get()));                                           // 4449
        add.accept(new ItemStack(ModItems.BIO_WAFER.get()));                                                    // 4450
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.URANIUM, MaterialShape.NUGGET)));           // 4451
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.URANIUM233, MaterialShape.NUGGET)));        // 4452
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.URANIUM235, MaterialShape.NUGGET)));        // 4453
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.URANIUM238, MaterialShape.NUGGET)));        // 4454
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.THORIUM232, MaterialShape.NUGGET)));        // 4455
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.PLUTONIUM, MaterialShape.NUGGET)));         // 4456
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.PLUTONIUM238, MaterialShape.NUGGET)));      // 4457
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.PLUTONIUM239, MaterialShape.NUGGET)));      // 4458
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.PLUTONIUM240, MaterialShape.NUGGET)));      // 4459
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.PLUTONIUM241, MaterialShape.NUGGET)));      // 4460
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.PU_MIX, MaterialShape.NUGGET)));            // 4461
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.AM241, MaterialShape.NUGGET)));             // 4462
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.AM242, MaterialShape.NUGGET)));             // 4463
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.AM_MIX, MaterialShape.NUGGET)));            // 4464
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.NEPTUNIUM, MaterialShape.NUGGET)));         // 4465
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.POLONIUM, MaterialShape.NUGGET)));          // 4466
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.COBALT, MaterialShape.NUGGET)));            // 4467
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.CO60, MaterialShape.NUGGET)));              // 4468
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.SR90, MaterialShape.NUGGET)));              // 4469
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.TECHNETIUM, MaterialShape.NUGGET)));        // 4470
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.AU198, MaterialShape.NUGGET)));             // 4471
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.PB209, MaterialShape.NUGGET)));             // 4472
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.RA226, MaterialShape.NUGGET)));             // 4473
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.ACTINIUM, MaterialShape.NUGGET)));          // 4474
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.LEAD, MaterialShape.NUGGET)));              // 4475
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.BISMUTH, MaterialShape.NUGGET)));           // 4476
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.ARSENIC, MaterialShape.NUGGET)));           // 4477
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.TANTALIUM, MaterialShape.NUGGET)));         // 4478
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.SILICON, MaterialShape.NUGGET)));           // 4479
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.NIOBIUM, MaterialShape.NUGGET)));           // 4480
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.BERYLLIUM, MaterialShape.NUGGET)));         // 4481
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.SCHRABIDIUM, MaterialShape.NUGGET)));       // 4482
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.SOLINIUM, MaterialShape.NUGGET)));          // 4483
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.GH336, MaterialShape.NUGGET)));             // 4484
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.URANIUM_FUEL, MaterialShape.NUGGET)));      // 4485
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.THORIUM_FUEL, MaterialShape.NUGGET)));      // 4486
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.PLUTONIUM_FUEL, MaterialShape.NUGGET)));    // 4487
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.NEPTUNIUM_FUEL, MaterialShape.NUGGET)));    // 4488
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.MOX_FUEL, MaterialShape.NUGGET)));          // 4489
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.AMERICIUM_FUEL, MaterialShape.NUGGET)));    // 4490
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.SCHRABIDIUM_FUEL, MaterialShape.NUGGET)));  // 4491
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.HES, MaterialShape.NUGGET)));               // 4492
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.LES_FUEL, MaterialShape.NUGGET)));          // 4493
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.ZIRCONIUM, MaterialShape.NUGGET)));         // 4494
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.AUSTRALIUM, MaterialShape.NUGGET)));        // 4495
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.AUSTRALIUM_LESSER, MaterialShape.NUGGET))); // 4496
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.AUSTRALIUM_GREATER, MaterialShape.NUGGET)));// 4497
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.DESH, MaterialShape.NUGGET)));              // 4498
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.EUPHEMIUM, MaterialShape.NUGGET)));         // 4499
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.DINEUTRONIUM, MaterialShape.NUGGET)));      // 4500
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.OSMIRIDIUM, MaterialShape.NUGGET)));        // 4501
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.IRON, MaterialShape.PLATE))); // 4502
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.GOLD, MaterialShape.PLATE))); // 4503
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.TITANIUM, MaterialShape.PLATE))); // 4504
        add.accept(new ItemStack(ModItems.PLATE_ALUMINIUM.get())); // 4505
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.STEEL, MaterialShape.PLATE))); // 4506
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.LEAD, MaterialShape.PLATE))); // 4507
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.COPPER, MaterialShape.PLATE))); // 4508
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.DURA_STEEL, MaterialShape.PLATE))); // 4509
        add.accept(new ItemStack(ModItems.NEUTRON_REFLECTOR.get())); // 4510
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.SCHRABIDIUM, MaterialShape.PLATE))); // 4511
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.COMBINE_STEEL, MaterialShape.PLATE))); // 4512
        add.accept(new ItemStack(ModItems.PLATE_MIXED.get())); // 4513
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.GUNMETAL, MaterialShape.PLATE))); // 4514
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.WEAPONSTEEL, MaterialShape.PLATE))); // 4515
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.SATURNITE, MaterialShape.PLATE))); // 4516
        add.accept(new ItemStack(ModItems.PLATE_PAA.get())); // 4517
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.POLYMER, MaterialShape.PLATE))); // 4518
        add.accept(new ItemStack(ModItems.PLATE_KEVLAR.get())); // 4519
        add.accept(new ItemStack(ModItems.PLATE_DALEKANIUM.get())); // 4520
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.DESH, MaterialShape.PLATE))); // 4521
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.BISMUTH, MaterialShape.PLATE))); // 4522
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.EUPHEMIUM, MaterialShape.PLATE))); // 4523
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.DINEUTRONIUM, MaterialShape.PLATE))); // 4524
        add.accept(new ItemStack(ModItems.PLATE_ARMOR_TITANIUM.get())); // 4525
        add.accept(new ItemStack(ModItems.PLATE_ARMOR_AJR.get())); // 4526
        add.accept(new ItemStack(ModItems.PLATE_ARMOR_HEV.get())); // 4527
        add.accept(new ItemStack(ModItems.PLATE_ARMOR_LUNAR.get())); // 4528
        add.accept(new ItemStack(ModItems.PLATE_ARMOR_FAU.get())); // 4529
        add.accept(new ItemStack(ModItems.PLATE_ARMOR_DNT.get())); // 4530
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.IRON, MaterialShape.PLATE_CAST)));        // 4531/2600 IRON
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.GOLD, MaterialShape.PLATE_CAST)));        // 4531/7900 GOLD
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.SCHRABIDIUM, MaterialShape.PLATE_CAST))); // 4531/12626 SA326
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.SCHRABIDATE, MaterialShape.PLATE_CAST))); // 4531/12600 SBD
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.TITANIUM, MaterialShape.PLATE_CAST)));    // 4531/2200 TI
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.COPPER, MaterialShape.PLATE_CAST)));      // 4531/2900 CU
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.TUNGSTEN, MaterialShape.PLATE_CAST)));    // 4531/7400 W
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.ALUMINIUM, MaterialShape.PLATE_CAST)));   // 4531/1300 AL
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.LEAD, MaterialShape.PLATE_CAST)));        // 4531/8200 PB
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.ZIRCONIUM, MaterialShape.PLATE_CAST)));   // 4531/4000 ZR
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.OSMIRIDIUM, MaterialShape.PLATE_CAST)));   // 4531/7699 OSMIRIDIUM
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.STEEL, MaterialShape.PLATE_CAST)));       // 4531/30 STEEL
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.DURA_STEEL, MaterialShape.PLATE_CAST)));   // 4531/33 DURA
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.DESH, MaterialShape.PLATE_CAST)));        // 4531/42 DESH
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.STAR_METAL, MaterialShape.PLATE_CAST)));   // 4531/35 STAR
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.FERROURANIUM, MaterialShape.PLATE_CAST))); // 4531/37 FERRO
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.TCALLOY, MaterialShape.PLATE_CAST)));     // 4531/36 TCALLOY
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.CDALLOY, MaterialShape.PLATE_CAST)));     // 4531/43 CDALLOY
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.BBRONZE, MaterialShape.PLATE_CAST)));     // 4531/46 BBRONZE
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.ABRONZE, MaterialShape.PLATE_CAST)));     // 4531/47 ABRONZE
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.CMB, MaterialShape.PLATE_CAST)));         // 4531/39 CMB
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.WEAPONSTEEL, MaterialShape.PLATE_CAST))); // 4531/50 WEAPONSTEEL
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.SATURNITE, MaterialShape.PLATE_CAST)));   // 4531/34 SATURN
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.IRON, MaterialShape.PLATE_WELDED)));      // 4532/2600 IRON
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.TITANIUM, MaterialShape.PLATE_WELDED)));   // 4532/2200 TI
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.COPPER, MaterialShape.PLATE_WELDED)));    // 4532/2900 CU
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.TUNGSTEN, MaterialShape.PLATE_WELDED)));   // 4532/7400 W
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.ALUMINIUM, MaterialShape.PLATE_WELDED)));   // 4532/1300 AL
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.ZIRCONIUM, MaterialShape.PLATE_WELDED)));   // 4532/4000 ZR
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.OSMIRIDIUM, MaterialShape.PLATE_WELDED)));   // 4532/7699 OSMIRIDIUM
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.STEEL, MaterialShape.PLATE_WELDED)));     // 4532/30 STEEL
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.TCALLOY, MaterialShape.PLATE_WELDED)));   // 4532/36 TCALLOY
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.CDALLOY, MaterialShape.PLATE_WELDED)));   // 4532/43 CDALLOY
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.CMB, MaterialShape.PLATE_WELDED)));       // 4532/39 CMB
        // 4533 shell (ItemAutogen SHELL, 6 мет: TI/CU/AL/STEEL/WEAPONSTEEL/BIGMT)
        add.accept(new ItemStack(ModItems.SHELL_TITANIUM.get()));   // 4533/2200 TI
        add.accept(new ItemStack(ModItems.SHELL_COPPER.get()));     // 4533/2900 CU
        add.accept(new ItemStack(ModItems.SHELL_ALUMINUM.get()));   // 4533/1300 AL
        add.accept(new ItemStack(ModItems.SHELL_STEEL.get()));      // 4533/30 STEEL
        for (Item sh : com.hbm_m.item.PartTabMetaItems.group("shell")) { // 4533 WEAPONSTEEL/BIGMT
            add.accept(new ItemStack(sh));
        }
        // 4534 pipe (ItemAutogen PIPE, 7 мет: IRON/CU/AL/PB/STEEL/DURA/RUBBER)
        add.accept(new ItemStack(ModItems.PIPE_IRON.get()));        // 4534/2600 IRON
        add.accept(new ItemStack(ModItems.PIPE_COPPER.get()));      // 4534/2900 CU
        add.accept(new ItemStack(ModItems.PIPE_ALUMINUM.get()));    // 4534/1300 AL
        add.accept(new ItemStack(ModItems.PIPE_LEAD.get()));        // 4534/8200 PB
        add.accept(new ItemStack(ModItems.PIPE_STEEL.get()));       // 4534/30 STEEL
        add.accept(new ItemStack(ModItems.PIPE_DURA_STEEL.get()));  // 4534/33 DURA
        for (Item pi : com.hbm_m.item.PartTabMetaItems.group("pipe")) { // 4534/20003 RUBBER
            add.accept(new ItemStack(pi));
        }
        // 4535 bolt (ItemAutogen BOLT, 4 меты: W/PB/STEEL/DURA)
        add.accept(new ItemStack(ModItems.BOLT_TUNGSTEN.get()));         // 4535/7400 W
        add.accept(new ItemStack(ModItems.BOLT_LEAD.get()));             // 4535/8200 PB
        add.accept(new ItemStack(ModItems.BOLT_STEEL.get()));            // 4535/30 STEEL
        add.accept(new ItemStack(ModItems.BOLT_HIGHSPEED_STEEL.get()));  // 4535/33 DURA
        add.accept(new ItemStack(ModItems.BOLT_SPIKE.get())); // 4536
        add.accept(new ItemStack(ModItems.HAZMAT_CLOTH.get())); // 4537
        add.accept(new ItemStack(ModItems.HAZMAT_CLOTH_RED.get())); // 4538
        add.accept(new ItemStack(ModItems.HAZMAT_CLOTH_GREY.get())); // 4539
        add.accept(new ItemStack(ModItems.ASBESTOS_CLOTH.get())); // 4540
        add.accept(new ItemStack(ModItems.RAG.get())); // 4541
        add.accept(new ItemStack(ModItems.RAG_DAMP.get())); // 4542
        add.accept(new ItemStack(ModItems.RAG_PISS.get())); // 4543
        add.accept(new ItemStack(ModItems.FILTER_COAL.get())); // 4544
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.CARBON, MaterialShape.WIRE)));            // 4545/699 CARBON
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.GOLD, MaterialShape.WIRE)));              // 4545/7900 GOLD
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.SCHRABIDIUM, MaterialShape.WIRE)));       // 4545/12626 SA326
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.COPPER, MaterialShape.WIRE)));            // 4545/2900 CU
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.TUNGSTEN, MaterialShape.WIRE)));          // 4545/7400 W
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.ALUMINIUM, MaterialShape.WIRE)));         // 4545/1300 AL
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.LEAD, MaterialShape.WIRE)));              // 4545/8200 PB
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.ZIRCONIUM, MaterialShape.WIRE)));         // 4545/4000 ZR
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.STEEL, MaterialShape.WIRE)));             // 4545/30 STEEL
        // 4545/31 MINGRADE ("Minecraft Grade Copper Wire"): MAT_MINGRADE (_AS+1 = 31) идёт
        // сразу после MAT_STEEL в Mats.orderedList — tsv-дамп её не содержал, но getSubItems
        // оригинала перебирает orderedList, где она между STEEL и MAGTUNG.
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.RED_COPPER, MaterialShape.WIRE)));   // 4545/31 MINGRADE
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.MAGNETIZED_TUNGSTEN, MaterialShape.WIRE)));   // 4545/38 MAGTUNG
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.GOLD, MaterialShape.WIRE_DENSE)));        // 4546/7900 GOLD
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.SCHRABIDIUM, MaterialShape.WIRE_DENSE)));   // 4546/12626 SA326
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.SCHRABIDATE, MaterialShape.WIRE_DENSE))); // 4546/12600 SBD
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.TITANIUM, MaterialShape.WIRE_DENSE)));    // 4546/2200 TI
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.COPPER, MaterialShape.WIRE_DENSE)));      // 4546/2900 CU
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.TUNGSTEN, MaterialShape.WIRE_DENSE)));    // 4546/7400 W
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.NEODYMIUM, MaterialShape.WIRE_DENSE)));   // 4546/6000 ND
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.NIOBIUM, MaterialShape.WIRE_DENSE)));     // 4546/4100 NB
        // 4546/31 MINGRADE ("Dense Minecraft Grade Copper Wire"): в Mats.orderedList между
        // NIOBIUM и STAR (_AS-группа: STEEL(30), MINGRADE(31), ..., STAR(35)).
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.RED_COPPER, MaterialShape.WIRE_DENSE)));  // 4546/31 MINGRADE
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.STAR_METAL, MaterialShape.WIRE_DENSE)));  // 4546/35 STAR
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.BSCCO, MaterialShape.WIRE_DENSE)));       // 4546/48 BSCCO
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.MAGNETIZED_TUNGSTEN, MaterialShape.WIRE_DENSE))); // 4546/38 MAGTUNG
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.DNT, MaterialShape.WIRE_DENSE)));         // 4546/45 DNT
        add.accept(new ItemStack(ModItems.COIL_COPPER.get())); // 4547
        add.accept(new ItemStack(ModItems.COIL_COPPER_TORUS.get())); // 4548
        add.accept(new ItemStack(ModItems.COIL_GOLD.get())); // 4549
        add.accept(new ItemStack(ModItems.COIL_GOLD_TORUS.get())); // 4550
        add.accept(new ItemStack(ModItems.COIL_TUNGSTEN.get())); // 4551
        add.accept(new ItemStack(ModItems.COIL_MAGNETIZED_TUNGSTEN.get())); // 4552
        add.accept(new ItemStack(ModItems.SAFETY_FUSE.get())); // 4553
        add.accept(new ItemStack(ModItems.TANK_STEEL.get())); // 4554
        add.accept(new ItemStack(ModItems.MOTOR.get())); // 4555
        add.accept(new ItemStack(ModItems.MOTOR_DESH.get())); // 4556
        add.accept(new ItemStack(ModItems.MOTOR_BISMUTH.get())); // 4557
        add.accept(new ItemStack(ModItems.CENTRIFUGE_ELEMENT.get())); // 4558
        add.accept(new ItemStack(ModItems.REACTOR_CORE.get())); // 4559
        add.accept(new ItemStack(ModItems.RTG_UNIT.get())); // 4560
        add.accept(new ItemStack(ModItems.PIPES_STEEL.get())); // 4561
        add.accept(new ItemStack(ModItems.DRILL_TITANIUM.get())); // 4562
        add.accept(new ItemStack(ModItems.PHOTO_PANEL.get())); // 4563
        add.accept(new ItemStack(ModItems.CHLORINE_PINWHEEL.get())); // 4564
        add.accept(new ItemStack(ModItems.RING_STARMETAL.get())); // 4565
        add.accept(new ItemStack(ModItems.DEUTERIUM_FILTER.get())); // 4566
        // 4567 chemical_dye (ItemChemicalDye, 16 цветов в порядке EnumChemDye)
        for (Item dye : com.hbm_m.item.PartTabMetaItems.group("dye")) {
            add.accept(new ItemStack(dye));
        }
        // 4568 crayon (ItemCrayon, 16 цветов)
        for (Item crayon : com.hbm_m.item.PartTabMetaItems.group("crayon")) {
            add.accept(new ItemStack(crayon));
        }
        // 4569 part_generic (ItemGenericPart, 6 мет в порядке EnumPartType)
        for (Item pg : com.hbm_m.item.PartTabMetaItems.group("part_generic")) {
            add.accept(new ItemStack(pg));
        }
        add.accept(new ItemStack(ModItems.ITEM_EXPENSIVE.get())); // 4570
        // 4573 parts_legendary (ItemEnumMulti EnumLegendaryType, 3 меты)
        for (Item leg : com.hbm_m.item.PartTabMetaItems.group("legendary")) {
            add.accept(new ItemStack(leg));
        }
        // 4574 gear_large (ItemGear, 2 меты: gear_large / gear_large_steel)
        add.accept(new ItemStack(ModItems.GEAR_LARGE.get())); // 4574 meta0
        for (Item gear : com.hbm_m.item.PartTabMetaItems.group("gear")) { // 4574 meta1
            add.accept(new ItemStack(gear));
        }
        add.accept(new ItemStack(ModItems.SAWBLADE.get())); // 4575
        // 4576..4582 ItemAutogen-семейство (порядок мет — по parts_tab_true.tsv)
        for (Item it : com.hbm_m.item.PartTabMetaItems.group("barrel_light")) {    // 4576
            add.accept(new ItemStack(it));
        }
        for (Item it : com.hbm_m.item.PartTabMetaItems.group("barrel_heavy")) {    // 4577
            add.accept(new ItemStack(it));
        }
        for (Item it : com.hbm_m.item.PartTabMetaItems.group("receiver_light")) {  // 4578
            add.accept(new ItemStack(it));
        }
        for (Item it : com.hbm_m.item.PartTabMetaItems.group("receiver_heavy")) {  // 4579
            add.accept(new ItemStack(it));
        }
        for (Item it : com.hbm_m.item.PartTabMetaItems.group("mechanism")) {       // 4580
            add.accept(new ItemStack(it));
        }
        for (Item it : com.hbm_m.item.PartTabMetaItems.group("stock")) {           // 4581
            add.accept(new ItemStack(it));
        }
        for (Item it : com.hbm_m.item.PartTabMetaItems.group("grip")) {            // 4582
            add.accept(new ItemStack(it));
        }
        // 4583 plant_item (ItemEnumMulti EnumPlantType: TOBACCO/ROPE/MUSTARDWILLOW)
        for (Item pl : com.hbm_m.item.PartTabMetaItems.group("plant")) {
            add.accept(new ItemStack(pl));
        }
        add.accept(new ItemStack(ModItems.ENTANGLEMENT_KIT.get())); // 4584
        add.accept(new ItemStack(ModItems.FINS_FLAT.get())); // 4585
        add.accept(new ItemStack(ModItems.FINS_SMALL_STEEL.get())); // 4586
        add.accept(new ItemStack(ModItems.FINS_BIG_STEEL.get())); // 4587
        add.accept(new ItemStack(ModItems.FINS_TRI_STEEL.get())); // 4588
        add.accept(new ItemStack(ModItems.FINS_QUAD_TITANIUM.get())); // 4589
        add.accept(new ItemStack(ModItems.SPHERE_STEEL.get())); // 4590
        add.accept(new ItemStack(ModItems.PEDESTAL_STEEL.get())); // 4591
        add.accept(new ItemStack(ModItems.DYSFUNCTIONAL_REACTOR.get())); // 4592
        add.accept(new ItemStack(ModItems.BLADE_TITANIUM.get())); // 4593
        add.accept(new ItemStack(ModItems.BLADE_TUNGSTEN.get())); // 4594
        add.accept(new ItemStack(ModItems.TURBINE_TITANIUM.get())); // 4595
        add.accept(new ItemStack(ModItems.TURBINE_TUNGSTEN.get())); // 4596
        add.accept(new ItemStack(ModItems.FLYWHEEL_BERYLLIUM.get())); // 4597
        add.accept(new ItemStack(ModItems.DUCTTAPE.get())); // 4598
        add.accept(new ItemStack(ModItems.CATALYST_CLAY.get())); // 4599
        add.accept(new ItemStack(ModItems.MISSILE_ASSEMBLY.get())); // 4600
        add.accept(new ItemStack(ModItems.WARHEAD_GENERIC_SMALL.get())); // 4601
        add.accept(new ItemStack(ModItems.WARHEAD_GENERIC_MEDIUM.get())); // 4602
        add.accept(new ItemStack(ModItems.WARHEAD_GENERIC_LARGE.get())); // 4603
        add.accept(new ItemStack(ModItems.WARHEAD_INCENDIARY_SMALL.get())); // 4604
        add.accept(new ItemStack(ModItems.WARHEAD_INCENDIARY_MEDIUM.get())); // 4605
        add.accept(new ItemStack(ModItems.WARHEAD_INCENDIARY_LARGE.get())); // 4606
        add.accept(new ItemStack(ModItems.WARHEAD_CLUSTER_SMALL.get())); // 4607
        add.accept(new ItemStack(ModItems.WARHEAD_CLUSTER_MEDIUM.get())); // 4608
        add.accept(new ItemStack(ModItems.WARHEAD_CLUSTER_LARGE.get())); // 4609
        add.accept(new ItemStack(ModItems.WARHEAD_BUSTER_SMALL.get())); // 4610
        add.accept(new ItemStack(ModItems.WARHEAD_BUSTER_MEDIUM.get())); // 4611
        add.accept(new ItemStack(ModItems.WARHEAD_BUSTER_LARGE.get())); // 4612
        add.accept(new ItemStack(ModItems.WARHEAD_NUCLEAR.get())); // 4613
        add.accept(new ItemStack(ModItems.WARHEAD_MIRV.get())); // 4614
        add.accept(new ItemStack(ModItems.WARHEAD_VOLCANO.get())); // 4615
        add.accept(new ItemStack(ModItems.FUEL_TANK_SMALL.get())); // 4616
        add.accept(new ItemStack(ModItems.FUEL_TANK_MEDIUM.get())); // 4617
        add.accept(new ItemStack(ModItems.FUEL_TANK_LARGE.get())); // 4618
        add.accept(new ItemStack(ModItems.THRUSTER_SMALL.get())); // 4619
        add.accept(new ItemStack(ModItems.THRUSTER_MEDIUM.get())); // 4620
        add.accept(new ItemStack(ModItems.THRUSTER_LARGE.get())); // 4621
        add.accept(new ItemStack(ModItems.THRUSTER_NUCLEAR.get())); // 4622
        add.accept(new ItemStack(ModItems.SEG_10.get())); // 4623
        add.accept(new ItemStack(ModItems.SEG_15.get())); // 4624
        add.accept(new ItemStack(ModItems.SEG_20.get())); // 4625
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.COMBINE_SCRAP, MaterialShape.SCRAP))); // 4626
        add.accept(new ItemStack(ModItems.SHIMMER_HEAD.get())); // 4627
        add.accept(new ItemStack(ModItems.SHIMMER_AXE_HEAD.get())); // 4628
        add.accept(new ItemStack(ModItems.SHIMMER_HANDLE.get())); // 4629
        add.accept(new ItemStack(ModItems.VACUUM_TUBE.get()));                                                // 4630/0
        add.accept(new ItemStack(ModItems.CIRCUIT_NUMITRON.get()));                                           // 4630/19
        add.accept(new ItemStack(ModItems.CAPACITOR.get()));                                                  // 4630/1
        add.accept(new ItemStack(ModItems.CAPACITOR_TANTALUM.get()));                                         // 4630/2
        add.accept(new ItemStack(ModItems.ATOMIC_CLOCK.get()));                                               // 4630/18
        add.accept(new ItemStack(ModItems.PCB.get()));                                                        // 4630/3
        add.accept(new ItemStack(ModItems.SILICON_CIRCUIT.get()));                                            // 4630/4
        add.accept(new ItemStack(ModItems.MICROCHIP.get()));                                                  // 4630/5
        add.accept(new ItemStack(ModItems.BISMOID_CHIP.get()));                                               // 4630/6
        add.accept(new ItemStack(ModItems.QUANTUM_CHIP.get()));                                               // 4630/16
        add.accept(new ItemStack(ModItems.ANALOG_CIRCUIT.get()));                                             // 4630/7
        add.accept(new ItemStack(ModItems.INTEGRATED_CIRCUIT.get()));                                         // 4630/8
        add.accept(new ItemStack(ModItems.ADVANCED_CIRCUIT.get()));                                           // 4630/9
        add.accept(new ItemStack(ModItems.CAPACITOR_BOARD.get()));                                            // 4630/10
        add.accept(new ItemStack(ModItems.BISMOID_CIRCUIT.get()));                                            // 4630/11
        add.accept(new ItemStack(ModItems.QUANTUM_CIRCUIT.get()));                                            // 4630/15
        add.accept(new ItemStack(ModItems.CONTROLLER_CHASSIS.get()));                                         // 4630/12
        add.accept(new ItemStack(ModItems.CONTROLLER.get()));                                                 // 4630/13
        add.accept(new ItemStack(ModItems.CONTROLLER_ADVANCED.get()));                                        // 4630/14
        add.accept(new ItemStack(ModItems.QUANTUM_COMPUTER.get()));                                           // 4630/17
        add.accept(new ItemStack(ModItems.CRT_DISPLAY.get())); // 4632
        // 4636 casing (ItemEnumMulti EnumCasingType, 7 мет в порядке enum)
        for (Item casing : com.hbm_m.item.PartTabMetaItems.group("casing")) {
            add.accept(new ItemStack(casing));
        }
        add.accept(new ItemStack(ModItems.ASSEMBLY_NUKE.get())); // 4637
        add.accept(new ItemStack(ModItems.WIRING_RED_COPPER.get())); // 4638
        add.accept(new ItemStack(ModItems.FLAME_PONY.get())); // 4639
        add.accept(new ItemStack(ModItems.FLAME_CONSPIRACY.get())); // 4640
        add.accept(new ItemStack(ModItems.FLAME_POLITICS.get())); // 4641
        add.accept(new ItemStack(ModItems.FLAME_OPINION.get())); // 4642
        add.accept(new ItemStack(ModItems.PELLET_CLUSTER.get())); // 4654
        add.accept(new ItemStack(ModItems.PELLET_BUCKSHOT.get())); // 4655
        add.accept(new ItemStack(ModItems.PELLET_CHARGED.get())); // 4656
        add.accept(new ItemStack(ModItems.PELLET_GAS.get())); // 4657
        add.accept(new ItemStack(ModItems.MAGNETRON.get())); // 4658
        // 4765 (ItemScraps): 82 суб-айтема в порядке Mats.orderedList оригинала
        // (SMELTABLE/ADDITIVE) — единый источник порядка с ModMaterialItems.FOUNDRY_SCRAPS.
        for (ModMaterialItems.ScrapEntry scrap : ModMaterialItems.FOUNDRY_SCRAPS) {
            addScrap(add, scrap.mat());
        }
        add.accept(new ItemStack(ModItems.UPGRADE_MUFFLER.get())); // 4766
        add.accept(new ItemStack(ModItems.UPGRADE_TEMPLATE.get())); // 4767
        add.accept(new ItemStack(ModItems.RUNE_BLANK.get())); // 4824
        add.accept(new ItemStack(ModItems.RUNE_ISA.get())); // 4825
        add.accept(new ItemStack(ModItems.RUNE_DAGAZ.get())); // 4826
        add.accept(new ItemStack(ModItems.RUNE_HAGALAZ.get())); // 4827
        add.accept(new ItemStack(ModItems.RUNE_JERA.get())); // 4828
        add.accept(new ItemStack(ModItems.RUNE_THURISAZ.get())); // 4829
        // 4878..4886 waste_* (ItemDepletedFuel): мета0 свежее + мета1 охлаждающееся (тинт)
        add.accept(new ItemStack(ModItems.WASTE_NATURAL_URANIUM.get())); // 4878/0
        add.accept(new ItemStack(com.hbm_m.item.PartTabMetaItems.itemOrNull("waste_natural_uranium_cooling"))); // 4878/1
        add.accept(new ItemStack(ModItems.WASTE_URANIUM.get())); // 4879/0
        add.accept(new ItemStack(com.hbm_m.item.PartTabMetaItems.itemOrNull("waste_uranium_cooling"))); // 4879/1
        add.accept(new ItemStack(ModItems.WASTE_THORIUM.get())); // 4880/0
        add.accept(new ItemStack(com.hbm_m.item.PartTabMetaItems.itemOrNull("waste_thorium_cooling"))); // 4880/1
        add.accept(new ItemStack(ModItems.WASTE_MOX.get())); // 4881/0
        add.accept(new ItemStack(com.hbm_m.item.PartTabMetaItems.itemOrNull("waste_mox_cooling"))); // 4881/1
        add.accept(new ItemStack(ModItems.WASTE_PLUTONIUM.get())); // 4882/0
        add.accept(new ItemStack(com.hbm_m.item.PartTabMetaItems.itemOrNull("waste_plutonium_cooling"))); // 4882/1
        add.accept(new ItemStack(ModItems.WASTE_U233.get())); // 4883/0
        add.accept(new ItemStack(com.hbm_m.item.PartTabMetaItems.itemOrNull("waste_u233_cooling"))); // 4883/1
        add.accept(new ItemStack(ModItems.WASTE_U235.get())); // 4884/0
        add.accept(new ItemStack(com.hbm_m.item.PartTabMetaItems.itemOrNull("waste_u235_cooling"))); // 4884/1
        add.accept(new ItemStack(ModItems.WASTE_SCHRABIDIUM.get())); // 4885/0
        add.accept(new ItemStack(com.hbm_m.item.PartTabMetaItems.itemOrNull("waste_schrabidium_cooling"))); // 4885/1
        add.accept(new ItemStack(ModItems.WASTE_ZFB_MOX.get())); // 4886/0
        add.accept(new ItemStack(com.hbm_m.item.PartTabMetaItems.itemOrNull("waste_zfb_mox_cooling"))); // 4886/1
        add.accept(new ItemStack(ModItems.UNDEFINED.get())); // 4993
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.SCRAP, MaterialShape.SCRAP))); // 4995
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.SCRAP_OIL, MaterialShape.SCRAP))); // 4996
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.SCRAP_NUCLEAR, MaterialShape.SCRAP))); // 4997
        add.accept(new ItemStack(ModItems.TRINITITE.get())); // 4998
        // 4999..5006 nuclear_waste_long/short (ItemWasteLong/ItemWasteShort):
        // каждая мета (изотоп) = отдельный слот, лор — италик-имя изотопа.
        for (Item it : com.hbm_m.item.PartTabMetaItems.group("nw_long")) {            // 4999
            add.accept(new ItemStack(it));
        }
        for (Item it : com.hbm_m.item.PartTabMetaItems.group("nw_long_tiny")) {       // 5000
            add.accept(new ItemStack(it));
        }
        for (Item it : com.hbm_m.item.PartTabMetaItems.group("nw_short")) {           // 5001
            add.accept(new ItemStack(it));
        }
        for (Item it : com.hbm_m.item.PartTabMetaItems.group("nw_short_tiny")) {      // 5002
            add.accept(new ItemStack(it));
        }
        for (Item it : com.hbm_m.item.PartTabMetaItems.group("nw_long_dep")) {        // 5003
            add.accept(new ItemStack(it));
        }
        for (Item it : com.hbm_m.item.PartTabMetaItems.group("nw_long_dep_tiny")) {   // 5004
            add.accept(new ItemStack(it));
        }
        for (Item it : com.hbm_m.item.PartTabMetaItems.group("nw_short_dep")) {       // 5005
            add.accept(new ItemStack(it));
        }
        for (Item it : com.hbm_m.item.PartTabMetaItems.group("nw_short_dep_tiny")) {  // 5006
            add.accept(new ItemStack(it));
        }
        add.accept(new ItemStack(ModItems.NUCLEAR_WASTE.get())); // 5007
        add.accept(new ItemStack(ModItems.NUCLEAR_WASTE_TINY.get())); // 5008
        add.accept(new ItemStack(ModItems.NUCLEAR_WASTE_VITRIFIED.get())); // 5009
        add.accept(new ItemStack(ModItems.NUCLEAR_WASTE_VITRIFIED_TINY.get())); // 5010
        add.accept(new ItemStack(ModItems.LAUNCH_CODE_PIECE.get())); // 5056
        add.accept(new ItemStack(ModItems.LAUNCH_CODE.get())); // 5057
        add.accept(new ItemStack(ModItems.LAUNCH_KEY.get())); // 5058
        add.accept(new ItemStack(ModItems.WRENCH.get())); // 5399
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.HORN, MaterialShape.CRYSTAL))); // 5800
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.CHARRED, MaterialShape.CRYSTAL))); // 5801
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.UZH, MaterialShape.BILLET)));               // 4210
        add.accept(new ItemStack(ModItems.INGOT_TUNGSTEN_CARBIDE.get()));                                       // 4125

        for (Item it : com.hbm_m.item.PartTabMetaItems.group("drive")) {
            add.accept(new ItemStack(it));
        }

    }

    /** populateControlTab: порядок из оригинального 1.7.10 (ModBlocks/ModItems, вкладка control); отсутствующие в порте предметы пропущены. */
    public static void populateControlTab(BiConsumer<ItemStack, CreativeModeTab.TabVisibility> acceptor) {
        Consumer<ItemStack> add = stack -> acceptor.accept(stack, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);

        add.accept(new ItemStack(ModItems.PELLET_RTG_RADIUM.get()));
        add.accept(new ItemStack(ModItems.PELLET_RTG_WEAK.get()));
        add.accept(new ItemStack(ModItems.PELLET_RTG.get()));
        add.accept(new ItemStack(ModItems.PELLET_RTG_STRONTIUM.get()));
        add.accept(new ItemStack(ModItems.PELLET_RTG_COBALT.get()));
        add.accept(new ItemStack(ModItems.PELLET_RTG_ACTINIUM.get()));
        add.accept(new ItemStack(ModItems.PELLET_RTG_AMERICIUM.get()));
        add.accept(new ItemStack(ModItems.PELLET_RTG_POLONIUM.get()));
        add.accept(new ItemStack(ModItems.PELLET_RTG_GOLD.get()));
        add.accept(new ItemStack(ModItems.PELLET_RTG_LEAD.get()));
        add.accept(new ItemStack(ModItems.PISTON_SELENIUM.get()));
        add.accept(new ItemStack(ModItems.AMS_CATALYST_BLANK.get()));
        add.accept(new ItemStack(ModItems.AMS_CATALYST_ALUMINIUM.get()));
        add.accept(new ItemStack(ModItems.AMS_CATALYST_BERYLLIUM.get()));
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
        add.accept(new ItemStack(ModItems.CELL_EMPTY.get()));
        add.accept(new ItemStack(ModItems.CELL_UF6.get()));
        add.accept(new ItemStack(ModItems.CELL_PUF6.get()));
        add.accept(new ItemStack(ModItems.CELL_ANTIMATTER.get()));
        add.accept(new ItemStack(ModItems.CELL_DEUTERIUM.get()));
        add.accept(new ItemStack(ModItems.CELL_TRITIUM.get()));
        add.accept(new ItemStack(ModItems.CELL_SAS3.get()));
        add.accept(new ItemStack(ModItems.CELL_ANTI_SCHRABIDIUM.get()));
        add.accept(new ItemStack(ModItems.CELL_BALEFIRE.get()));
        add.accept(new ItemStack(ModItems.PARTICLE_EMPTY.get()));
        add.accept(new ItemStack(ModItems.PARTICLE_HYDROGEN.get()));
        add.accept(new ItemStack(ModItems.PARTICLE_COPPER.get()));
        add.accept(new ItemStack(ModItems.PARTICLE_LEAD.get()));
        add.accept(new ItemStack(ModItems.PARTICLE_AMAT.get()));
        add.accept(new ItemStack(ModItems.PARTICLE_ASCHRAB.get()));
        add.accept(new ItemStack(ModItems.PARTICLE_HIGGS.get()));
        add.accept(new ItemStack(ModItems.PARTICLE_MUON.get()));
        add.accept(new ItemStack(ModItems.PARTICLE_TACHYON.get()));
        add.accept(new ItemStack(ModItems.PARTICLE_STRANGE.get()));
        add.accept(new ItemStack(ModItems.PARTICLE_DARK.get()));
        add.accept(new ItemStack(ModItems.PARTICLE_SPARKTICLE.get()));
        add.accept(new ItemStack(ModItems.PARTICLE_DIGAMMA.get()));
        add.accept(new ItemStack(ModItems.PARTICLE_LUTECE.get()));
        add.accept(new ItemStack(ModItems.SINGULARITY.get()));
        add.accept(new ItemStack(ModItems.BLACK_HOLE.get()));
        add.accept(new ItemStack(ModItems.PELLET_ANTIMATTER.get()));
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.XEN, MaterialShape.CRYSTAL)));
        add.accept(new ItemStack(ModItems.STAMP_STONE_FLAT.get()));
        add.accept(new ItemStack(ModItems.STAMP_STONE_PLATE.get()));
        add.accept(new ItemStack(ModItems.STAMP_STONE_WIRE.get()));
        add.accept(new ItemStack(ModItems.STAMP_STONE_CIRCUIT.get()));
        add.accept(new ItemStack(ModItems.STAMP_IRON_FLAT.get()));
        add.accept(new ItemStack(ModItems.STAMP_IRON_PLATE.get()));
        add.accept(new ItemStack(ModItems.STAMP_IRON_WIRE.get()));
        add.accept(new ItemStack(ModItems.STAMP_IRON_CIRCUIT.get()));
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
        add.accept(new ItemStack(ModItems.STAMP_357.get()));
        add.accept(new ItemStack(ModItems.STAMP_44.get()));
        add.accept(new ItemStack(ModItems.STAMP_9.get()));
        add.accept(new ItemStack(ModItems.STAMP_50.get()));
        add.accept(new ItemStack(ModItems.STAMP_DESH_357.get()));
        add.accept(new ItemStack(ModItems.STAMP_DESH_44.get()));
        add.accept(new ItemStack(ModItems.STAMP_DESH_9.get()));
        add.accept(new ItemStack(ModItems.STAMP_DESH_50.get()));
        add.accept(new ItemStack(ModItems.BLADES_STEEL.get()));
        add.accept(new ItemStack(ModItems.BLADES_TITANIUM.get()));
        add.accept(new ItemStack(ModItems.BLADES_DESH.get()));
        add.accept(new ItemStack(ModItems.MOLD_BASE.get()));
        add.accept(new ItemStack(ModItems.PART_LITHIUM.get()));
        add.accept(new ItemStack(ModItems.PART_BERYLLIUM.get()));
        add.accept(new ItemStack(ModItems.PART_CARBON.get()));
        add.accept(new ItemStack(ModItems.PART_COPPER.get()));
        add.accept(new ItemStack(ModItems.PART_PLUTONIUM.get()));
        add.accept(new ItemStack(ModItems.LASER_CRYSTAL_CO2.get()));
        add.accept(new ItemStack(ModItems.LASER_CRYSTAL_BISMUTH.get()));
        add.accept(new ItemStack(ModItems.LASER_CRYSTAL_CMB.get()));
        add.accept(new ItemStack(ModItems.LASER_CRYSTAL_DNT.get()));
        add.accept(new ItemStack(ModItems.LASER_CRYSTAL_DIGAMMA.get()));
        add.accept(new ItemStack(ModItems.THERMO_ELEMENT.get()));
        add.accept(new ItemStack(ModItems.CATALYTIC_CONVERTER.get()));
        add.accept(new ItemStack(ModItems.CANISTER_EMPTY.get()));
        add.accept(new ItemStack(ModItems.CANISTER_NAPALM.get()));
        add.accept(new ItemStack(ModItems.GAS_EMPTY.get()));
        add.accept(new ItemStack(ModItems.ROD_EMPTY.get()));
        add.accept(new ItemStack(ModItems.ROD_DUAL_EMPTY.get()));
        add.accept(new ItemStack(ModItems.ROD_QUAD_EMPTY.get()));
        add.accept(new ItemStack(ModItems.ROD_ZIRNOX_EMPTY.get()));
        add.accept(new ItemStack(ModItems.ROD_ZIRNOX_TRITIUM.get()));
        add.accept(new ItemStack(ModItems.ROD_ZIRNOX_URANIUM_FUEL_DEPLETED.get()));
        add.accept(new ItemStack(ModItems.ROD_ZIRNOX_THORIUM_FUEL_DEPLETED.get()));
        add.accept(new ItemStack(ModItems.ROD_ZIRNOX_MOX_FUEL_DEPLETED.get()));
        add.accept(new ItemStack(ModItems.ROD_ZIRNOX_PLUTONIUM_FUEL_DEPLETED.get()));
        add.accept(new ItemStack(ModItems.ROD_ZIRNOX_U233_FUEL_DEPLETED.get()));
        add.accept(new ItemStack(ModItems.ROD_ZIRNOX_U235_FUEL_DEPLETED.get()));
        add.accept(new ItemStack(ModItems.ROD_ZIRNOX_LES_FUEL_DEPLETED.get()));
        add.accept(new ItemStack(ModItems.ROD_ZIRNOX_ZFB_MOX_DEPLETED.get()));
        add.accept(new ItemStack(ModBlocks.PWR_FUEL.get()));
        add.accept(new ItemStack(ModItems.PWR_PRINTER.get()));
        add.accept(new ItemStack(ModItems.RBMK_LID.get()));
        add.accept(new ItemStack(ModItems.RBMK_LID_GLASS.get()));
        add.accept(new ItemStack(ModItems.RBMK_FUEL_EMPTY.get()));
        add.accept(new ItemStack(ModItems.ICF_PELLET_EMPTY.get()));
        add.accept(new ItemStack(ModItems.ICF_PELLET.get()));
        add.accept(new ItemStack(ModItems.ICF_PELLET_DEPLETED.get()));
        add.accept(new ItemStack(ModItems.DEBRIS_GRAPHITE.get()));
        add.accept(new ItemStack(ModItems.DEBRIS_METAL.get()));
        add.accept(new ItemStack(ModItems.DEBRIS_FUEL.get()));
        add.accept(new ItemStack(ModItems.DEBRIS_CONCRETE.get()));
        add.accept(new ItemStack(ModItems.DEBRIS_EXCHANGER.get()));
        add.accept(new ItemStack(ModItems.DEBRIS_SHRAPNEL.get()));
        add.accept(new ItemStack(ModItems.DEBRIS_ELEMENT.get()));
        add.accept(new ItemStack(ModItems.REACHER.get()));
        add.accept(new ItemStack(ModItems.MELTDOWN_TOOL.get()));
        addBattery(add, ModItems.CREATIVE_BATTERY.get());
        add.accept(new ItemStack(ModItems.CUBE_POWER.get()));
        addBattery(add, ModItems.BATTERY_SCHRABIDIUM.get());
        addBattery(add, ModItems.BATTERY_POTATO.get());
        addBattery(add, ModBlocks.HEV_BATTERY.get());
        add.accept(new ItemStack(ModItems.FUSION_CORE.get()));
        add.accept(new ItemStack(ModItems.FUSE.get()));
        add.accept(new ItemStack(ModItems.ARC_ELECTRODE.get()));
        add.accept(new ItemStack(ModItems.AMS_LENS.get()));
        add.accept(new ItemStack(ModItems.AMS_CORE_SING.get()));
        add.accept(new ItemStack(ModItems.AMS_CORE_WORMHOLE.get()));
        add.accept(new ItemStack(ModItems.AMS_CORE_EYEOFHARMONY.get()));
        add.accept(new ItemStack(ModItems.FUSION_SHIELD_TUNGSTEN.get()));
        add.accept(new ItemStack(ModItems.FUSION_SHIELD_DESH.get()));
        add.accept(new ItemStack(ModItems.FUSION_SHIELD_CHLOROPHYTE.get()));
        add.accept(new ItemStack(ModItems.FUSION_SHIELD_VAPORWAVE.get()));
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
        add.accept(new ItemStack(ModItems.UPGRADE_SCREM.get()));
        add.accept(new ItemStack(ModItems.UPGRADE_5G.get()));
        add.accept(new ItemStack(ModItems.UPGRADE_STACK_1.get()));
        add.accept(new ItemStack(ModItems.UPGRADE_EJECTOR_1.get()));
        add.accept(new ItemStack(ModItems.FLUID_TANK.get()));
        add.accept(new ItemStack(ModItems.FLUID_BARREL.get()));
        add.accept(new ItemStack(ModItems.FLUID_BARREL_INFINITE.get()));
        add.accept(new ItemStack(ModItems.PIPETTE.get()));
        add.accept(new ItemStack(ModItems.PIPETTE_BORON.get()));
        add.accept(new ItemStack(ModItems.PIPETTE_LABORATORY.get()));
        add.accept(new ItemStack(ModItems.SIPHON.get()));
        add.accept(new ItemStack(ModItems.INFINITE_WATER_500.get()));
        add.accept(new ItemStack(ModItems.INFINITE_WATER_5000.get()));

        // Заполненные жидкостные бочки (по одной на каждую жидкость) — восстановлено вручную
        // из старого populateFuelTab (в 1.7.10 это предметы с NBT, в генераторе не представимы).
        for (ModFluids.FluidEntry entry : HbmFluidRegistry.getOrderedFluids()) {
            ItemStack filledBarrel = new ItemStack(ModItems.FLUID_BARREL.get());
            dev.architectury.fluid.FluidStack archFluidStack = dev.architectury.fluid.FluidStack.create(entry.getSource(), FluidBarrelItem.getPlatformCapacity());
            FluidBarrelItem.setFluid(filledBarrel, archFluidStack);
            add.accept(filledBarrel);
        }

        // Жидкостные трубы: пустые + заполненные (по флюиду), три стиля — восстановлено вручную
        add.accept(new ItemStack(ModItems.FLUID_DUCT.get()));
        add.accept(new ItemStack(ModItems.FLUID_DUCT_COLORED.get()));
        add.accept(new ItemStack(ModItems.FLUID_DUCT_SILVER.get()));
        for (ModFluids.FluidEntry entry : HbmFluidRegistry.getOrderedFluids()) {
            add.accept(com.hbm_m.item.liquids.FluidDuctItem.createStack(ModItems.FLUID_DUCT.get(), entry));
            add.accept(com.hbm_m.item.liquids.FluidDuctItem.createStack(ModItems.FLUID_DUCT_COLORED.get(), entry));
            add.accept(com.hbm_m.item.liquids.FluidDuctItem.createStack(ModItems.FLUID_DUCT_SILVER.get(), entry));
        }
    }

    /** populateTemplatesTab: порядок из оригинального 1.7.10 (ModBlocks/ModItems, вкладка template); отсутствующие в порте предметы пропущены. */
    public static void populateTemplatesTab(BiConsumer<ItemStack, CreativeModeTab.TabVisibility> acceptor) {
        Consumer<ItemStack> add = stack -> acceptor.accept(stack, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);

        add.accept(new ItemStack(ModItems.BLUEPRINTS.get()));
        add.accept(new ItemStack(ModItems.BLUEPRINT_FOLDER.get()));
        add.accept(new ItemStack(ModItems.FLUID_IDENTIFIER_MULTI.get()));
        add.accept(new ItemStack(ModItems.FLUID_DUCT.get()));

        // Чертежи/шаблоны сборки (папки чертежей по пулам рецептов) — восстановлено вручную
        // из старого populateTemplatesTab; требует доступа к RecipeManager, т.е. только клиент.
        EnvExecutor.runInEnv(Env.CLIENT, () -> () -> {
            ClientSetup.addTemplatesClient(add);
        });

        // Идентификаторы жидкостей (по одному на каждую жидкость) — восстановлено вручную
        for (ModFluids.FluidEntry entry : HbmFluidRegistry.getOrderedFluids()) {
            ItemStack idStack = new ItemStack(ModItems.FLUID_IDENTIFIER.get());
            FluidIdentifierItem.setType(idStack, HbmFluidRegistry.getFluidName(entry.getSource()), true);
            add.accept(idStack);
        }
    }

    /** populateBlocksTab: порядок из оригинального 1.7.10 (ModBlocks/ModItems, вкладка blocks); отсутствующие в порте предметы пропущены. */
    public static void populateBlocksTab(BiConsumer<ItemStack, CreativeModeTab.TabVisibility> acceptor) {
        Consumer<ItemStack> add = stack -> acceptor.accept(stack, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);

        add.accept(new ItemStack(ModBlocks.URANIUM_ORE.get()));
        add.accept(new ItemStack(ModBlocks.ORE_URANIUM_SCORCHED.get()));
        add.accept(new ItemStack(ModBlocks.TITANIUM_ORE.get()));
        add.accept(new ItemStack(ModBlocks.SULFUR_ORE.get()));
        add.accept(new ItemStack(ModBlocks.THORIUM_ORE.get()));
        add.accept(new ItemStack(ModBlocks.NITER_ORE.get()));
        add.accept(new ItemStack(ModBlocks.ORE_COPPER.get()));
        add.accept(new ItemStack(ModBlocks.TUNGSTEN_ORE.get()));
        add.accept(new ItemStack(ModBlocks.ORE_ALUMINIUM.get()));
        add.accept(new ItemStack(ModBlocks.FLUORITE_ORE.get()));
        add.accept(new ItemStack(ModBlocks.LEAD_ORE.get()));
        add.accept(new ItemStack(ModBlocks.SCHRABIDIUM_ORE.get()));
        add.accept(new ItemStack(ModBlocks.BERYLLIUM_ORE.get()));
        add.accept(new ItemStack(ModBlocks.LIGNITE_ORE.get()));
        add.accept(new ItemStack(ModBlocks.ASBESTOS_ORE.get()));
        add.accept(new ItemStack(ModBlocks.CLUSTER_IRON.get()));
        add.accept(new ItemStack(ModBlocks.CLUSTER_TITANIUM.get()));
        add.accept(new ItemStack(ModBlocks.CLUSTER_ALUMINIUM.get()));
        add.accept(new ItemStack(ModBlocks.CLUSTER_COPPER.get()));
        add.accept(new ItemStack(ModBlocks.ORE_NETHER_COAL.get()));
        add.accept(new ItemStack(ModBlocks.ORE_NETHER_SMOLDERING.get()));
        add.accept(new ItemStack(ModBlocks.ORE_NETHER_URANIUM.get()));
        add.accept(new ItemStack(ModBlocks.ORE_NETHER_URANIUM_SCORCHED.get()));
        add.accept(new ItemStack(ModBlocks.ORE_NETHER_PLUTONIUM.get()));
        add.accept(new ItemStack(ModBlocks.ORE_NETHER_TUNGSTEN.get()));
        add.accept(new ItemStack(ModBlocks.ORE_NETHER_SULFUR.get()));
        add.accept(new ItemStack(ModBlocks.ORE_NETHER_FIRE.get()));
        add.accept(new ItemStack(ModBlocks.ORE_NETHER_COBALT.get()));
        add.accept(new ItemStack(ModBlocks.SCHRABIDIUM_ORE_NETHER.get()));
        add.accept(new ItemStack(ModBlocks.STONE_GNEISS.get()));
        add.accept(new ItemStack(ModBlocks.ORE_GNEISS_IRON.get()));
        add.accept(new ItemStack(ModBlocks.ORE_GNEISS_GOLD.get()));
        add.accept(new ItemStack(ModBlocks.ORE_GNEISS_URANIUM.get()));
        add.accept(new ItemStack(ModBlocks.ORE_GNEISS_URANIUM_SCORCHED.get()));
        add.accept(new ItemStack(ModBlocks.ORE_GNEISS_COPPER.get()));
        add.accept(new ItemStack(ModBlocks.ORE_GNEISS_ASBESTOS.get()));
        add.accept(new ItemStack(ModBlocks.ORE_GNEISS_LITHIUM.get()));
        add.accept(new ItemStack(ModBlocks.SCHRABIDIUM_ORE_GNEISS.get()));
        add.accept(new ItemStack(ModBlocks.ORE_GNEISS_RARE.get()));
        add.accept(new ItemStack(ModBlocks.ORE_GNEISS_GAS.get()));
        add.accept(new ItemStack(ModBlocks.GNEISS_BRICK.get()));
        add.accept(new ItemStack(ModBlocks.GNEISS_TILE.get()));
        add.accept(new ItemStack(ModBlocks.GNEISS_CHISELED.get()));
        add.accept(new ItemStack(ModBlocks.STONE_DEPTH.get()));
        add.accept(new ItemStack(ModBlocks.ORE_DEPTH_CINNEBAR.get()));
        add.accept(new ItemStack(ModBlocks.ORE_DEPTH_ZIRCONIUM.get()));
        add.accept(new ItemStack(ModBlocks.ORE_DEPTH_BORAX.get()));
        add.accept(new ItemStack(ModBlocks.CLUSTER_DEPTH_IRON.get()));
        add.accept(new ItemStack(ModBlocks.CLUSTER_DEPTH_TITANIUM.get()));
        add.accept(new ItemStack(ModBlocks.CLUSTER_DEPTH_TUNGSTEN.get()));
        add.accept(new ItemStack(ModBlocks.ORE_ALEXANDRITE.get()));
        add.accept(new ItemStack(ModBlocks.DEPTH_BRICK.get()));
        add.accept(new ItemStack(ModBlocks.DEPTH_TILES.get()));
        add.accept(new ItemStack(ModBlocks.DEPTH_NETHER_BRICK.get()));
        add.accept(new ItemStack(ModBlocks.DEPTH_NETHER_TILES.get()));
        add.accept(new ItemStack(ModBlocks.DEPTH_DNT.get()));
        add.accept(new ItemStack(ModBlocks.STONE_DEPTH_NETHER.get()));
        add.accept(new ItemStack(ModBlocks.ORE_DEPTH_NETHER_NEODYMIUM.get()));
        add.accept(new ItemStack(ModBlocks.STONE_POROUS.get()));
        add.accept(new ItemStack(ModBlocks.BASALT.get()));
        add.accept(new ItemStack(ModBlocks.BASALT_SMOOTH.get()));
        add.accept(new ItemStack(ModBlocks.BASALT_BRICK.get()));
        add.accept(new ItemStack(ModBlocks.BASALT_POLISHED.get()));
        add.accept(new ItemStack(ModBlocks.BASALT_TILES.get()));
        add.accept(new ItemStack(ModBlocks.ORE_AUSTRALIUM.get()));
        add.accept(new ItemStack(ModBlocks.ORE_RARE.get()));
        add.accept(new ItemStack(ModBlocks.COBALT_ORE.get()));
        add.accept(new ItemStack(ModBlocks.ORE_CINNEBAR.get()));
        add.accept(new ItemStack(ModBlocks.ORE_COLTAN.get()));
        add.accept(new ItemStack(ModBlocks.ORE_OIL.get()));
        add.accept(new ItemStack(ModBlocks.ORE_OIL_EMPTY.get()));
        add.accept(new ItemStack(ModBlocks.ORE_OIL_SAND.get()));
        add.accept(new ItemStack(ModBlocks.ORE_BEDROCK_OIL.get()));
        add.accept(new ItemStack(ModBlocks.ORE_TIKITE.get()));
        add.accept(new ItemStack(ModBlocks.BLOCK_PU_MIX.get()));
        add.accept(new ItemStack(ModBlocks.BLOCK_SULFUR.get()));
        add.accept(new ItemStack(ModBlocks.BLOCK_NITER.get()));
        add.accept(new ItemStack(ModBlocks.BLOCK_COPPER.get()));
        add.accept(new ItemStack(ModBlocks.BLOCK_ALUMINIUM.get()));
        add.accept(new ItemStack(ModBlocks.BLOCK_FLUORITE.get()));
        add.accept(new ItemStack(ModBlocks.BLOCK_COLTAN.get()));
        add.accept(new ItemStack(ModBlocks.BLOCK_TANTALIUM.get()));
        add.accept(new ItemStack(ModBlocks.BLOCK_TRINITITE.get()));
        add.accept(new ItemStack(ModBlocks.BLOCK_WASTE.get()));
        add.accept(new ItemStack(ModBlocks.BLOCK_WASTE_VITRIFIED.get()));
        add.accept(new ItemStack(ModBlocks.ANCIENT_SCRAP.get()));
        add.accept(new ItemStack(ModBlocks.BLOCK_CORIUM.get()));
        add.accept(new ItemStack(ModBlocks.BLOCK_CORIUM_COBBLE.get()));
        add.accept(new ItemStack(ModBlocks.BLOCK_SCRAP.get()));
        add.accept(new ItemStack(ModBlocks.BLOCK_ELECTRICAL_SCRAP.get()));
        add.accept(new ItemStack(ModBlocks.BLOCK_SCHRABIDIUM_CLUSTER.get()));
        add.accept(new ItemStack(ModBlocks.BLOCK_EUPHEMIUM_CLUSTER.get()));
        add.accept(new ItemStack(ModBlocks.BLOCK_MAGNETIZED_TUNGSTEN.get()));
        add.accept(new ItemStack(ModBlocks.BLOCK_POLYMER.get()));
        add.accept(new ItemStack(ModBlocks.BLOCK_BAKELITE.get()));
        add.accept(new ItemStack(ModBlocks.BLOCK_RUBBER.get()));
        add.accept(new ItemStack(ModBlocks.BLOCK_YELLOWCAKE.get()));
        add.accept(new ItemStack(ModBlocks.BLOCK_INSULATOR.get()));
        add.accept(new ItemStack(ModBlocks.BLOCK_FIBERGLASS.get()));
        add.accept(new ItemStack(ModBlocks.BLOCK_ASBESTOS.get()));
        add.accept(new ItemStack(ModBlocks.BLOCK_LITHIUM.get()));
        add.accept(new ItemStack(ModBlocks.BLOCK_WHITE_PHOSPHORUS.get()));
        add.accept(new ItemStack(ModBlocks.BLOCK_RED_PHOSPHORUS.get()));
        add.accept(new ItemStack(ModBlocks.BLOCK_FALLOUT.get()));
        add.accept(new ItemStack(ModBlocks.BLOCK_GRAPHITE.get()));
        add.accept(new ItemStack(ModBlocks.BLOCK_TRITIUM.get()));
        add.accept(new ItemStack(ModBlocks.BLOCK_SEMTEX.get()));
        add.accept(new ItemStack(ModBlocks.BLOCK_C4.get()));
        add.accept(new ItemStack(ModBlocks.BLOCK_SMORE.get()));
        add.accept(new ItemStack(ModBlocks.BLOCK_SLAG.get()));
        add.accept(new ItemStack(ModBlocks.DECO_TITANIUM.get()));
        add.accept(new ItemStack(ModBlocks.DECO_RED_COPPER.get()));
        add.accept(new ItemStack(ModBlocks.DECO_TUNGSTEN.get()));
        add.accept(new ItemStack(ModBlocks.DECO_ALUMINIUM.get()));
        add.accept(new ItemStack(ModBlocks.DECO_STEEL.get()));
        add.accept(new ItemStack(ModBlocks.DECO_RUSTY_STEEL.get()));
        add.accept(new ItemStack(ModBlocks.DECO_LEAD.get()));
        add.accept(new ItemStack(ModBlocks.DECO_BERYLLIUM.get()));
        add.accept(new ItemStack(ModBlocks.DECO_RBMK.get()));
        add.accept(new ItemStack(ModBlocks.DECO_RBMK_SMOOTH.get()));
        add.accept(new ItemStack(ModBlocks.GRAVEL_OBSIDIAN.get()));
        add.accept(new ItemStack(ModBlocks.GRAVEL_DIAMOND.get()));
        add.accept(new ItemStack(ModBlocks.ASPHALT.get()));
        add.accept(new ItemStack(ModBlocks.ASPHALT_LIGHT.get()));
        add.accept(new ItemStack(ModBlocks.SANDBAGS.get()));
        add.accept(new ItemStack(ModBlocks.WOOD_BARRIER.get()));
        add.accept(new ItemStack(ModBlocks.WOOD_STRUCTURE.get()));
        add.accept(new ItemStack(ModBlocks.REINFORCED_BRICK.get()));
        add.accept(new ItemStack(ModBlocks.REINFORCED_GLASS.get()));
        add.accept(new ItemStack(ModBlocks.REINFORCED_GLASS_PANE.get()));
        add.accept(new ItemStack(ModBlocks.REINFORCED_LIGHT.get()));
        add.accept(new ItemStack(ModBlocks.REINFORCED_SAND.get()));
        add.accept(new ItemStack(ModBlocks.REINFORCED_LAMP_OFF.get()));
        add.accept(new ItemStack(ModBlocks.REINFORCED_LAMINATE.get()));
        add.accept(new ItemStack(ModBlocks.REINFORCED_LAMINATE_PANE.get()));
        add.accept(new ItemStack(ModBlocks.LAMP_TRITIUM_GREEN_OFF.get()));
        add.accept(new ItemStack(ModBlocks.LAMP_TRITIUM_BLUE_OFF.get()));
        add.accept(new ItemStack(ModBlocks.LAMP_DEMON.get()));
        add.accept(new ItemStack(ModBlocks.REBAR.get()));
        add.accept(new ItemStack(ModBlocks.REINFORCED_STONE.get()));
        add.accept(new ItemStack(ModBlocks.CONCRETE.get()));
        add.accept(new ItemStack(ModBlocks.CONCRETE_ASBESTOS.get()));
        add.accept(new ItemStack(ModBlocks.CONCRETE_REBAR.get()));
        add.accept(new ItemStack(ModBlocks.CONCRETE_SUPER.get()));
        add.accept(new ItemStack(ModBlocks.CONCRETE_SUPER_BROKEN.get()));
        add.accept(new ItemStack(ModBlocks.CONCRETE_PILLAR.get()));
        add.accept(new ItemStack(ModBlocks.BRICK_CONCRETE.get()));
        add.accept(new ItemStack(ModBlocks.BRICK_CONCRETE_MOSSY.get()));
        add.accept(new ItemStack(ModBlocks.BRICK_CONCRETE_CRACKED.get()));
        add.accept(new ItemStack(ModBlocks.BRICK_CONCRETE_BROKEN.get()));
        add.accept(new ItemStack(ModBlocks.BRICK_CONCRETE_MARKED.get()));
        add.accept(new ItemStack(ModBlocks.BRICK_OBSIDIAN.get()));
        add.accept(new ItemStack(ModBlocks.BRICK_LIGHT.get()));
        add.accept(new ItemStack(ModBlocks.BRICK_COMPOUND.get()));
        add.accept(new ItemStack(ModBlocks.CMB_BRICK.get()));
        add.accept(new ItemStack(ModBlocks.CMB_BRICK_REINFORCED.get()));
        add.accept(new ItemStack(ModBlocks.BRICK_ASBESTOS.get()));
        add.accept(new ItemStack(ModBlocks.BRICK_FIRE.get()));
        add.accept(new ItemStack(ModBlocks.DUCRETE.get()));
        add.accept(new ItemStack(ModBlocks.BRICK_DUCRETE.get()));
        add.accept(new ItemStack(ModBlocks.REINFORCED_DUCRETE.get()));
        add.accept(new ItemStack(ModBlocks.CONCRETE_SLAB.get()));
        add.accept(new ItemStack(ModBlocks.CONCRETE_STAIRS.get()));
        add.accept(new ItemStack(ModBlocks.CONCRETE_ASBESTOS_STAIRS.get()));
        add.accept(new ItemStack(ModBlocks.BRICK_CONCRETE_STAIRS.get()));
        add.accept(new ItemStack(ModBlocks.BRICK_CONCRETE_MOSSY_STAIRS.get()));
        add.accept(new ItemStack(ModBlocks.BRICK_CONCRETE_CRACKED_STAIRS.get()));
        add.accept(new ItemStack(ModBlocks.BRICK_CONCRETE_BROKEN_STAIRS.get()));
        add.accept(new ItemStack(ModBlocks.BRICK_DUCRETE_STAIRS.get()));
        add.accept(new ItemStack(ModBlocks.REINFORCED_STONE_STAIRS.get()));
        add.accept(new ItemStack(ModBlocks.REINFORCED_BRICK_STAIRS.get()));
        add.accept(new ItemStack(ModBlocks.BRICK_OBSIDIAN_STAIRS.get()));
        add.accept(new ItemStack(ModBlocks.BRICK_LIGHT_STAIRS.get()));
        add.accept(new ItemStack(ModBlocks.BRICK_COMPOUND_STAIRS.get()));
        add.accept(new ItemStack(ModBlocks.BRICK_FIRE_STAIRS.get()));
        add.accept(new ItemStack(ModBlocks.ASPHALT_STAIRS.get()));
        add.accept(new ItemStack(ModBlocks.LIGHTSTONE_BRICKS_STAIRS.get()));
        add.accept(new ItemStack(ModBlocks.VINYL_TILE.get()));
        add.accept(new ItemStack(ModBlocks.TILE_LAB.get()));
        add.accept(new ItemStack(ModBlocks.TILE_LAB_CRACKED.get()));
        add.accept(new ItemStack(ModBlocks.TILE_LAB_BROKEN.get()));
        add.accept(new ItemStack(ModBlocks.BLOCK_METEOR.get()));
        add.accept(new ItemStack(ModBlocks.BLOCK_METEOR_COBBLE.get()));
        add.accept(new ItemStack(ModBlocks.BLOCK_METEOR_BROKEN.get()));
        add.accept(new ItemStack(ModBlocks.BLOCK_METEOR_MOLTEN.get()));
        add.accept(new ItemStack(ModBlocks.BLOCK_METEOR_TREASURE.get()));
        add.accept(new ItemStack(ModBlocks.METEOR_POLISHED.get()));
        add.accept(new ItemStack(ModBlocks.METEOR_BRICK.get()));
        add.accept(new ItemStack(ModBlocks.METEOR_BRICK_MOSSY.get()));
        add.accept(new ItemStack(ModBlocks.METEOR_BRICK_CRACKED.get()));
        add.accept(new ItemStack(ModBlocks.METEOR_BRICK_CHISELED.get()));
        add.accept(new ItemStack(ModBlocks.METEOR_PILLAR.get()));
        add.accept(new ItemStack(ModBlocks.METEOR_SPAWNER.get()));
        add.accept(new ItemStack(ModBlocks.MOON_TURF.get()));
        add.accept(new ItemStack(ModBlocks.BRICK_JUNGLE.get()));
        add.accept(new ItemStack(ModBlocks.BRICK_JUNGLE_CRACKED.get()));
        add.accept(new ItemStack(ModBlocks.BRICK_JUNGLE_FRAGILE.get()));
        add.accept(new ItemStack(ModBlocks.BRICK_JUNGLE_LAVA.get()));
        add.accept(new ItemStack(ModBlocks.BRICK_JUNGLE_OOZE.get()));
        add.accept(new ItemStack(ModBlocks.BRICK_JUNGLE_MYSTIC.get()));
        add.accept(new ItemStack(ModBlocks.BRICK_JUNGLE_TRAP.get()));
        add.accept(new ItemStack(ModBlocks.BRICK_JUNGLE_GLYPH.get()));
        add.accept(new ItemStack(ModBlocks.BRICK_JUNGLE_CIRCLE.get()));
        add.accept(new ItemStack(ModBlocks.TOASTER.get()));
        add.accept(new ItemStack(ModBlocks.TAPE_RECORDER.get()));
        add.accept(new ItemStack(ModBlocks.POLE_TOP.get()));
        add.accept(new ItemStack(ModBlocks.POLE_SATELLITE_RECEIVER.get()));
        add.accept(new ItemStack(ModBlocks.STEEL_WALL.get()));
        add.accept(new ItemStack(ModBlocks.STEEL_CORNER.get()));
        add.accept(new ItemStack(ModBlocks.STEEL_ROOF.get()));
        add.accept(new ItemStack(ModBlocks.STEEL_BEAM.get()));
        add.accept(new ItemStack(ModBlocks.STEEL_SCAFFOLD.get()));
        add.accept(new ItemStack(ModBlocks.STEEL_GRATE.get()));
        add.accept(new ItemStack(ModBlocks.STEEL_GRATE_WIDE.get()));
        add.accept(new ItemStack(ModBlocks.GLASS_QUARTZ.get()));
        add.accept(new ItemStack(ModBlocks.MUSH.get()));
        add.accept(new ItemStack(ModBlocks.PLANT_DEAD.get()));
        add.accept(new ItemStack(ModBlocks.VINE_PHOSPHOR.get()));
        add.accept(new ItemStack(ModBlocks.WASTE_EARTH.get()));
        add.accept(new ItemStack(ModBlocks.WASTE_MYCELIUM.get()));
        add.accept(new ItemStack(ModBlocks.WASTE_TRINITITE.get()));
        add.accept(new ItemStack(ModBlocks.WASTE_TRINITITE_RED.get()));
        add.accept(new ItemStack(ModBlocks.WASTE_LOG.get()));
        add.accept(new ItemStack(ModBlocks.WASTE_LEAVES.get()));
        add.accept(new ItemStack(ModBlocks.WASTE_PLANKS.get()));
        add.accept(new ItemStack(ModBlocks.FROZEN_DIRT.get()));
        add.accept(new ItemStack(ModBlocks.FROZEN_GRASS.get()));
        add.accept(new ItemStack(ModBlocks.FROZEN_LOG.get()));
        add.accept(new ItemStack(ModBlocks.FROZEN_PLANKS.get()));
        add.accept(new ItemStack(ModItems.FALLOUT.get()));
        add.accept(new ItemStack(ModBlocks.OIL_SPILL.get()));
        add.accept(new ItemStack(ModBlocks.TEKTITE.get()));
        add.accept(new ItemStack(ModBlocks.ORE_TEKTITE_OSMIRIDIUM.get()));
        add.accept(new ItemStack(ModBlocks.DIRT_DEAD.get()));
        add.accept(new ItemStack(ModBlocks.DIRT_OILY.get()));
        add.accept(new ItemStack(ModBlocks.SAND_DIRTY.get()));
        add.accept(new ItemStack(ModBlocks.SAND_DIRTY_RED.get()));
        add.accept(new ItemStack(ModBlocks.STONE_CRACKED.get()));
        add.accept(new ItemStack(ModBlocks.SELLAFIELD_SLAKED.get()));
        add.accept(new ItemStack(ModBlocks.SELLAFIELD_BEDROCK.get()));
        add.accept(new ItemStack(ModBlocks.ORE_SELLAFIELD_DIAMOND.get()));
        add.accept(new ItemStack(ModBlocks.ORE_SELLAFIELD_EMERALD.get()));
        add.accept(new ItemStack(ModBlocks.ORE_SELLAFIELD_URANIUM_SCORCHED.get()));
        add.accept(new ItemStack(ModBlocks.ORE_SELLAFIELD_SCHRABIDIUM.get()));
        add.accept(new ItemStack(ModBlocks.ORE_SELLAFIELD_RADGEM.get()));
        add.accept(new ItemStack(ModBlocks.LADDER_STURDY.get()));
        add.accept(new ItemStack(ModBlocks.LADDER_GOLD.get()));
        add.accept(new ItemStack(ModBlocks.LADDER_COPPER.get()));
        add.accept(new ItemStack(ModBlocks.LADDER_TITANIUM.get()));
        add.accept(new ItemStack(ModBlocks.LADDER_STEEL.get()));
        add.accept(new ItemStack(ModBlocks.TRAPDOOR_STEEL.get()));
        add.accept(new ItemStack(ModBlocks.BARBED_WIRE.get()));
        add.accept(new ItemStack(ModBlocks.BARBED_WIRE_FIRE.get()));
        add.accept(new ItemStack(ModBlocks.BARBED_WIRE_POISON.get()));
        add.accept(new ItemStack(ModBlocks.BARBED_WIRE_ACID.get()));
        add.accept(new ItemStack(ModBlocks.BARBED_WIRE_WITHER.get()));
        add.accept(new ItemStack(ModBlocks.BARBED_WIRE_ULTRADEATH.get()));
        add.accept(new ItemStack(ModBlocks.SPIKES.get()));
        add.accept(new ItemStack(ModBlocks.TESLA.get()));
        add.accept(new ItemStack(ModBlocks.BOXCAR.get()));
        add.accept(new ItemStack(ModItems.BUCKET_MUD.get()));
        add.accept(new ItemStack(ModItems.BUCKET_ACID.get()));
        add.accept(new ItemStack(ModItems.BUCKET_TOXIC.get()));
        add.accept(new ItemStack(ModItems.BUCKET_SCHRABIDIC_ACID.get()));
        add.accept(new ItemStack(ModItems.BUCKET_SULFURIC_ACID.get()));
        add.accept(new ItemStack(ModItems.DOOR_METAL.get()));
        add.accept(new ItemStack(ModBlocks.DOOR_OFFICE.get()));
        add.accept(new ItemStack(ModBlocks.DOOR_BUNKER.get()));
    }

    /** populateMachinesTab: порядок из оригинального 1.7.10 (ModBlocks/ModItems, вкладка machine); отсутствующие в порте предметы пропущены. */
    public static void populateMachinesTab(BiConsumer<ItemStack, CreativeModeTab.TabVisibility> acceptor) {
        Consumer<ItemStack> add = stack -> acceptor.accept(stack, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);

        add.accept(new ItemStack(ModBlocks.BROADCASTER_PC.get()));
        add.accept(new ItemStack(ModBlocks.GEIGER_COUNTER_BLOCK.get()));
        addBattery(add, ModBlocks.HEV_BATTERY.get());
        add.accept(new ItemStack(ModBlocks.FENCE_METAL.get()));
        add.accept(new ItemStack(ModBlocks.ASH_DIGAMMA.get()));
        add.accept(new ItemStack(ModBlocks.GLASS_BORON.get()));
        add.accept(new ItemStack(ModBlocks.GLASS_LEAD.get()));
        add.accept(new ItemStack(ModBlocks.GLASS_URANIUM.get()));
        add.accept(new ItemStack(ModBlocks.GLASS_TRINITITE.get()));
        add.accept(new ItemStack(ModBlocks.GLASS_POLONIUM.get()));
        add.accept(new ItemStack(ModBlocks.GLASS_ASH.get()));
        add.accept(new ItemStack(ModBlocks.GLASS_POLARIZED.get()));
        add.accept(new ItemStack(ModBlocks.PUMP_STEAM.get()));
        add.accept(new ItemStack(ModBlocks.PUMP_ELECTRIC.get()));
        add.accept(new ItemStack(ModBlocks.ELECTRIC_HEATER.get()));
        add.accept(new ItemStack(ModItems.ASHPIT.get()));
        add.accept(new ItemStack(ModBlocks.FURNACE_IRON.get()));
        add.accept(new ItemStack(ModBlocks.FURNACE_STEEL.get()));
        add.accept(new ItemStack(ModBlocks.STIRLING.get()));
        add.accept(new ItemStack(ModBlocks.STIRLING_STEEL.get()));
        add.accept(new ItemStack(ModBlocks.STIRLING_CREATIVE.get()));
        add.accept(new ItemStack(ModBlocks.SAWMILL.get()));
        add.accept(new ItemStack(ModBlocks.STRAND_CASTER.get()));
        add.accept(new ItemStack(ModBlocks.CRUCIBLE.get()));
        add.accept(new ItemStack(ModBlocks.MACHINE_BOILER.get()));
        add.accept(new ItemStack(ModItems.INDUSTRIAL_BOILER.get()));
        add.accept(new ItemStack(ModBlocks.FOUNDRY_MOLD.get()));
        add.accept(new ItemStack(ModBlocks.FOUNDRY_BASIN.get()));
        add.accept(new ItemStack(ModBlocks.FOUNDRY_CHANNEL.get()));
        add.accept(new ItemStack(ModBlocks.FOUNDRY_TANK.get()));
        add.accept(new ItemStack(ModBlocks.FOUNDRY_OUTLET.get()));
        add.accept(new ItemStack(ModBlocks.FOUNDRY_SLAGTAP.get()));
        add.accept(new ItemStack(ModBlocks.MACHINE_DIFURNACE_RTG.get()));
        add.accept(new ItemStack(ModBlocks.BLAST_FURNACE.get()));
        add.accept(new ItemStack(ModBlocks.BLAST_FURNACE_EXTENSION.get()));
        add.accept(new ItemStack(ModBlocks.MACHINE_BLAST_FURNACE.get()));
        add.accept(new ItemStack(ModBlocks.MACHINE_CENTRIFUGE.get()));
        add.accept(new ItemStack(ModBlocks.MACHINE_GASCENT.get()));
        add.accept(new ItemStack(ModItems.FEL.get()));
        add.accept(new ItemStack(ModItems.SILEX.get()));
        add.accept(new ItemStack(ModBlocks.ROTARY_FURNACE.get()));
        add.accept(new ItemStack(ModBlocks.MACHINE_CRYSTALLIZER.get()));
        add.accept(new ItemStack(ModBlocks.MACHINE_REACTOR.get()));
        add.accept(new ItemStack(ModBlocks.FURNACE_BRICK.get()));
        add.accept(new ItemStack(ModBlocks.INDUSTRIAL_GENERATOR.get()));
        add.accept(new ItemStack(ModItems.CYCLOTRON.get()));
        add.accept(new ItemStack(ModBlocks.EXPOSURE_CHAMBER.get()));
        add.accept(new ItemStack(ModBlocks.MACHINE_RADGEN.get()));
        add.accept(new ItemStack(ModBlocks.HADRON_COIL_ALLOY.get()));
        add.accept(new ItemStack(ModBlocks.HADRON_COIL_GOLD.get()));
        add.accept(new ItemStack(ModBlocks.HADRON_COIL_NEODYMIUM.get()));
        add.accept(new ItemStack(ModBlocks.HADRON_COIL_MAGTUNG.get()));
        add.accept(new ItemStack(ModBlocks.HADRON_COIL_SCHRABIDIUM.get()));
        add.accept(new ItemStack(ModBlocks.HADRON_COIL_SCHRABIDATE.get()));
        add.accept(new ItemStack(ModBlocks.HADRON_COIL_STARMETAL.get()));
        add.accept(new ItemStack(ModBlocks.HADRON_COIL_CHLOROPHYTE.get()));
        add.accept(new ItemStack(ModBlocks.HADRON_COIL_MESE.get()));
        add.accept(new ItemStack(ModBlocks.ELECTRIC_FURNACE.get()));
        add.accept(new ItemStack(ModBlocks.ARC_FURNACE.get()));
        add.accept(new ItemStack(ModBlocks.MACHINE_MICROWAVE.get()));
        addBattery(add, ModItems.MACHINE_BATTERY_SOCKET.get());
        add.accept(new ItemStack(ModBlocks.FENSU2.get()));
        add.accept(new ItemStack(ModBlocks.CAPACITOR_COPPER.get()));
        add.accept(new ItemStack(ModItems.WOOD_BURNER.get()));
        add.accept(new ItemStack(ModBlocks.COMBUSTION_ENGINE.get()));
        add.accept(new ItemStack(ModBlocks.SHREDDER.get()));
        add.accept(new ItemStack(ModBlocks.MACHINE_TELEPORTER.get()));
        add.accept(new ItemStack(ModBlocks.TELEANCHOR.get()));
        add.accept(new ItemStack(ModBlocks.RADIOLYSIS.get()));
        add.accept(new ItemStack(ModBlocks.HEPHAESTUS.get()));
        add.accept(new ItemStack(ModBlocks.RED_WIRE_COATED.get()));
        add.accept(new ItemStack(ModBlocks.RED_CABLE.get()));
        add.accept(new ItemStack(ModBlocks.RED_CABLE_CLASSIC.get()));
        add.accept(new ItemStack(ModBlocks.RED_CABLE_PAINTABLE.get()));
        add.accept(new ItemStack(ModBlocks.RED_CABLE_GAUGE.get()));
        add.accept(new ItemStack(ModBlocks.RED_CABLE_BOX.get()));
        add.accept(new ItemStack(ModBlocks.RED_CONNECTOR.get()));
        add.accept(new ItemStack(ModBlocks.RED_CONNECTOR_SUPER.get()));
        add.accept(new ItemStack(ModBlocks.RED_PYLON.get()));
        add.accept(new ItemStack(ModBlocks.RED_PYLON_STEEL.get()));
        add.accept(new ItemStack(ModBlocks.RED_PYLON_MEDIUM_WOOD.get()));
        add.accept(new ItemStack(ModBlocks.RED_PYLON_MEDIUM_WOOD_TRANSFORMER.get()));
        add.accept(new ItemStack(ModBlocks.RED_PYLON_MEDIUM_STEEL.get()));
        add.accept(new ItemStack(ModBlocks.RED_PYLON_MEDIUM_STEEL_TRANSFORMER.get()));
        add.accept(new ItemStack(ModBlocks.RED_PYLON_LARGE.get()));
        add.accept(new ItemStack(ModItems.SUBSTATION.get()));
        add.accept(new ItemStack(ModBlocks.CABLE_SWITCH.get()));
        add.accept(new ItemStack(ModBlocks.CABLE_DETECTOR.get()));
        add.accept(new ItemStack(ModBlocks.CABLE_DIODE.get()));
        add.accept(new ItemStack(ModBlocks.MACHINE_DETECTOR.get()));
        add.accept(new ItemStack(ModBlocks.FLUID_DUCT_BOX.get()));
        add.accept(new ItemStack(ModBlocks.FLUID_DUCT_EXHAUST.get()));
        add.accept(new ItemStack(ModBlocks.FLUID_DUCT_PAINTABLE_BLOCK_EXHAUST.get()));
        add.accept(new ItemStack(ModBlocks.PIPE_ANCHOR.get()));
        add.accept(new ItemStack(ModBlocks.FLUID_DUCT_PAINTABLE.get()));
        add.accept(new ItemStack(ModItems.FLUID_VALVE.get()));
        add.accept(new ItemStack(ModBlocks.FLUID_SWITCH.get()));
        add.accept(new ItemStack(ModBlocks.FLUID_COUNTER_VALVE.get()));
        add.accept(new ItemStack(ModItems.FLUID_PUMP.get()));
        add.accept(new ItemStack(ModBlocks.MACHINE_DRAIN.get()));
        add.accept(new ItemStack(ModBlocks.RADIO_TORCH_SENDER.get()));
        add.accept(new ItemStack(ModBlocks.RADIO_TORCH_RECEIVER.get()));
        add.accept(new ItemStack(ModBlocks.RADIO_TORCH_COUNTER.get()));
        add.accept(new ItemStack(ModBlocks.RADIO_TORCH_LOGIC.get()));
        add.accept(new ItemStack(ModBlocks.RADIO_TORCH_READER.get()));
        add.accept(new ItemStack(ModBlocks.RADIO_TORCH_CONTROLLER.get()));
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
        add.accept(new ItemStack(ModBlocks.PNEUMATIC_STORAGE_MONO.get()));
        add.accept(new ItemStack(ModBlocks.PNEUMATIC_STORAGE_IMPORTER.get()));
        add.accept(new ItemStack(ModBlocks.PNEUMATIC_STORAGE_EXPORTER.get()));
        add.accept(new ItemStack(ModBlocks.BARREL_PLASTIC.get()));
        add.accept(new ItemStack(ModBlocks.BARREL_STEEL.get()));
        add.accept(new ItemStack(ModBlocks.BARREL_TCALLOY.get()));
        add.accept(new ItemStack(ModBlocks.BARREL_ANTIMATTER.get()));
        add.accept(new ItemStack(ModBlocks.MACHINE_TRANSFORMER.get()));
        add.accept(new ItemStack(ModBlocks.MACHINE_SOLAR_BOILER.get()));
        add.accept(new ItemStack(ModBlocks.SOLAR_MIRROR.get()));
        add.accept(new ItemStack(ModBlocks.STRUCT_TORUS_CORE.get()));
        add.accept(new ItemStack(ModBlocks.STRUCT_WATZ_CORE.get()));
        add.accept(new ItemStack(ModBlocks.STRUCT_ICF_CORE.get()));
        add.accept(new ItemStack(ModBlocks.CM_FLUX.get()));
        add.accept(new ItemStack(ModBlocks.CM_HEAT.get()));
        add.accept(new ItemStack(ModBlocks.PILE_BRICK.get()));
        add.accept(new ItemStack(ModBlocks.PILE_BLOCK.get()));
        add.accept(new ItemStack(ModBlocks.PWR_FUEL.get()));
        add.accept(new ItemStack(ModBlocks.PWR_CONTROL.get()));
        add.accept(new ItemStack(ModBlocks.PWR_CHANNEL.get()));
        add.accept(new ItemStack(ModBlocks.PWR_HEATEX.get()));
        add.accept(new ItemStack(ModBlocks.PWR_HEATSINK.get()));
        add.accept(new ItemStack(ModBlocks.PWR_NEUTRON_SOURCE.get()));
        add.accept(new ItemStack(ModBlocks.PWR_REFLECTOR.get()));
        add.accept(new ItemStack(ModBlocks.PWR_CASING.get()));
        add.accept(new ItemStack(ModBlocks.PWR_PORT.get()));
        add.accept(new ItemStack(ModBlocks.PWR_CONTROLLER.get()));
        add.accept(new ItemStack(ModBlocks.FUSION_COMPONENT.get()));
        add.accept(new ItemStack(ModBlocks.BREEDER_FUSION.get()));
        add.accept(new ItemStack(ModBlocks.BOILER_FUSION.get()));
        add.accept(new ItemStack(ModBlocks.MACHINE_ICF_PRESS.get()));
        add.accept(new ItemStack(ModBlocks.ICF.get()));
        add.accept(new ItemStack(ModBlocks.ICF_COMPONENT.get()));
        add.accept(new ItemStack(ModBlocks.ICF_CONTROLLER.get()));
        add.accept(new ItemStack(ModBlocks.WATZ_ELEMENT.get()));
        add.accept(new ItemStack(ModBlocks.WATZ_COOLER.get()));
        add.accept(new ItemStack(ModBlocks.WATZ_END.get()));
        add.accept(new ItemStack(ModBlocks.WATZ_PUMP.get()));
        add.accept(new ItemStack(ModBlocks.MACHINE_CONVERTER_HE_RF.get()));
        add.accept(new ItemStack(ModBlocks.MACHINE_CONVERTER_RF_HE.get()));
        add.accept(new ItemStack(ModBlocks.DFC_EMITTER.get()));
        add.accept(new ItemStack(ModBlocks.DFC_INJECTOR.get()));
        add.accept(new ItemStack(ModBlocks.DFC_RECEIVER.get()));
        add.accept(new ItemStack(ModBlocks.DFC_STABILIZER.get()));
        add.accept(new ItemStack(ModBlocks.DFC_CORE.get()));
        add.accept(new ItemStack(ModBlocks.SEAL_FRAME.get()));
        add.accept(new ItemStack(ModBlocks.SEAL_CONTROLLER.get()));
        add.accept(new ItemStack(ModBlocks.CARGO_ELEVATOR.get()));
        add.accept(new ItemStack(ModItems.VAULT_DOOR.get()));
        add.accept(new ItemStack(ModBlocks.BLAST_DOOR.get()));
        add.accept(new ItemStack(ModItems.SLIDE_DOOR.get()));
        add.accept(new ItemStack(ModItems.FIRE_DOOR.get()));
        add.accept(new ItemStack(ModItems.TRANSITION_SEAL.get()));
        add.accept(new ItemStack(ModItems.SILO_HATCH.get()));
        add.accept(new ItemStack(ModItems.SILO_HATCH_LARGE.get()));
        add.accept(new ItemStack(ModItems.SECURE_ACCESS_DOOR.get()));
        add.accept(new ItemStack(ModItems.LARGE_VEHICLE_DOOR.get()));
        add.accept(new ItemStack(ModItems.QE_SLIDING.get()));
        add.accept(new ItemStack(ModItems.ROUND_AIRLOCK_DOOR.get()));
        add.accept(new ItemStack(ModItems.SLIDING_SEAL_DOOR.get()));
        add.accept(new ItemStack(ModItems.WATER_DOOR.get()));
        add.accept(new ItemStack(ModItems.CARGO_DOOR.get()));
        add.accept(new ItemStack(ModBlocks.RBMK_ROD.get()));
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
        add.accept(new ItemStack(ModBlocks.RBMK_DISPLAY.get()));
        add.accept(new ItemStack(ModBlocks.RBMK_KEYPAD.get()));
        add.accept(new ItemStack(ModBlocks.RBMK_GAUGE.get()));
        add.accept(new ItemStack(ModBlocks.RBMK_NUMITRON.get()));
        add.accept(new ItemStack(ModBlocks.RBMK_GRAPH.get()));
        add.accept(new ItemStack(ModBlocks.RBMK_LEVER.get()));
        add.accept(new ItemStack(ModBlocks.RBMK_INDICATOR.get()));
        add.accept(new ItemStack(ModBlocks.RBMK_TERMINAL.get()));
        add.accept(new ItemStack(ModBlocks.RBMK_AUTOLOADER.get()));
        add.accept(new ItemStack(ModBlocks.RBMK_LOADER.get()));
        add.accept(new ItemStack(ModBlocks.RBMK_STEAM_INLET.get()));
        add.accept(new ItemStack(ModBlocks.RBMK_STEAM_OUTLET.get()));
        add.accept(new ItemStack(ModItems.CRATE_IRON.get()));
        add.accept(new ItemStack(ModItems.CRATE_STEEL.get()));
        add.accept(new ItemStack(ModItems.CRATE_DESH.get()));
        add.accept(new ItemStack(ModItems.CRATE_TUNGSTEN.get()));
        add.accept(new ItemStack(ModBlocks.SAFE.get()));
        add.accept(new ItemStack(ModBlocks.MASS_STORAGE.get()));
        add.accept(new ItemStack(ModItems.PUMPJACK.get()));
        add.accept(new ItemStack(ModBlocks.CHIMNEY_BRICK.get()));
        add.accept(new ItemStack(ModBlocks.CHIMNEY_INDUSTRIAL.get()));
        add.accept(new ItemStack(ModBlocks.MACHINE_REFINERY.get()));
        add.accept(new ItemStack(ModItems.VACUUM_DISTILL.get()));
        add.accept(new ItemStack(ModItems.FRACTION_TOWER.get()));
        add.accept(new ItemStack(ModBlocks.FRACTION_SPACER.get()));
        add.accept(new ItemStack(ModItems.CATALYTIC_REFORMER.get()));
        add.accept(new ItemStack(ModItems.HYDROTREATER.get()));
        add.accept(new ItemStack(ModBlocks.COKER.get()));
        add.accept(new ItemStack(ModBlocks.PYROOVEN.get()));
        add.accept(new ItemStack(ModBlocks.AUTOSAW.get()));
        add.accept(new ItemStack(ModBlocks.THRESHER.get()));
        add.accept(new ItemStack(ModBlocks.ORE_SLOPPER.get()));
        add.accept(new ItemStack(ModBlocks.ANNIHILATOR.get()));
        add.accept(new ItemStack(ModBlocks.MACHINE_MINING_LASER.get()));
        add.accept(new ItemStack(ModBlocks.ASSEMBLY_FACTORY.get()));
        add.accept(new ItemStack(ModItems.ARC_WELDER.get()));
        add.accept(new ItemStack(ModItems.SOLDERING_STATION.get()));
        add.accept(new ItemStack(ModItems.CHEMICAL_PLANT.get()));
        add.accept(new ItemStack(ModItems.CHEMICAL_FACTORY.get()));
        add.accept(new ItemStack(ModItems.ADVANCED_ASSEMBLY_MACHINE.get()));
        add.accept(new ItemStack(ModItems.MACHINE_ASSEMBLER.get()));
        add.accept(new ItemStack(ModBlocks.PUREX.get()));
        add.accept(new ItemStack(ModItems.MIXER.get()));
        add.accept(new ItemStack(ModBlocks.MACHINE_FLUIDTANK.get()));
        add.accept(new ItemStack(ModItems.BAT9000.get()));
        add.accept(new ItemStack(ModBlocks.ORBUS.get()));
        add.accept(new ItemStack(ModItems.TURBOFAN.get()));
        add.accept(new ItemStack(ModBlocks.TURBINEGAS.get()));
        add.accept(new ItemStack(ModBlocks.LPW2.get()));
        add.accept(new ItemStack(ModBlocks.PRESS_PREHEATER.get()));
        add.accept(new ItemStack(ModBlocks.MACHINE_PRESS.get()));
        add.accept(new ItemStack(ModBlocks.MACHINE_EPRESS.get()));
        add.accept(new ItemStack(ModBlocks.CONVEYOR_PRESS.get()));
        add.accept(new ItemStack(ModBlocks.AMMO_PRESS.get()));
        add.accept(new ItemStack(ModBlocks.MACHINE_REACTOR_SMALL.get()));
        add.accept(new ItemStack(ModItems.ZIRNOX.get()));
        add.accept(new ItemStack(ModBlocks.STEAM_ENGINE.get()));
        add.accept(new ItemStack(ModItems.TURBINE.get()));
        add.accept(new ItemStack(ModBlocks.MACHINE_LARGE_TURBINE.get()));
        add.accept(new ItemStack(ModItems.INDUSTRIAL_TURBINE.get()));
        add.accept(new ItemStack(ModItems.MACHINE_CHUNGUS.get()));
        add.accept(new ItemStack(ModItems.TOWER_SMALL.get()));
        add.accept(new ItemStack(ModBlocks.CONDENSER_POWERED.get()));
        add.accept(new ItemStack(ModItems.DEUTERIUM_TOWER.get()));
        add.accept(new ItemStack(ModItems.LIQUEFACTOR.get()));
        add.accept(new ItemStack(ModBlocks.SOLIDIFIER.get()));
        add.accept(new ItemStack(ModBlocks.INTAKE.get()));
        add.accept(new ItemStack(ModBlocks.COMPRESSOR.get()));
        add.accept(new ItemStack(ModBlocks.ELECTROLYSER.get()));
        add.accept(new ItemStack(ModBlocks.MACHINE_AUTOCRAFTER.get()));
        add.accept(new ItemStack(ModBlocks.MACHINE_FUNNEL.get()));
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
        add.accept(new ItemStack(ModBlocks.MACHINE_WASTE_DRUM.get()));
        add.accept(new ItemStack(ModBlocks.MACHINE_STORAGE_DRUM.get()));
        add.accept(new ItemStack(ModBlocks.MACHINE_SIREN.get()));
        add.accept(new ItemStack(ModBlocks.RADIOBOX.get()));
        add.accept(new ItemStack(ModBlocks.RADIOREC.get()));
        add.accept(new ItemStack(ModBlocks.VENT_CHLORINE.get()));
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
        add.accept(new ItemStack(ModBlocks.GAS_COAL.get()));
        add.accept(new ItemStack(ModBlocks.GAS_FLAMMABLE.get()));
        add.accept(new ItemStack(ModBlocks.GAS_EXPLOSIVE.get()));
        add.accept(new ItemStack(ModBlocks.RAD_ABSORBER.get()));
        add.accept(new ItemStack(ModBlocks.DECON.get()));
    }

    /** populateNukeTab: порядок из оригинального 1.7.10 (ModBlocks/ModItems, вкладка nuke); отсутствующие в порте предметы пропущены. */
    public static void populateNukeTab(BiConsumer<ItemStack, CreativeModeTab.TabVisibility> acceptor) {
        Consumer<ItemStack> add = stack -> acceptor.accept(stack, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);

        add.accept(new ItemStack(ModBlocks.NUKE_GADGET.get()));
        add.accept(new ItemStack(ModBlocks.NUKE_BOY.get()));
        add.accept(new ItemStack(ModItems.NUKE_FAT_MAN.get()));
        add.accept(new ItemStack(ModBlocks.NUKE_MIKE.get()));
        add.accept(new ItemStack(ModBlocks.NUKE_TSAR.get()));
        add.accept(new ItemStack(ModBlocks.NUKE_FLEIJA.get()));
        add.accept(new ItemStack(ModItems.NUKE_PROTOTYPE.get()));
        add.accept(new ItemStack(ModBlocks.NUKE_CUSTOM.get()));
        add.accept(new ItemStack(ModBlocks.NUKE_SOLINIUM.get()));
        add.accept(new ItemStack(ModBlocks.NUKE_N2.get()));
        add.accept(new ItemStack(ModBlocks.NUKE_FSTBMB.get()));
        add.accept(new ItemStack(ModBlocks.BOMB_MULTI.get()));
        add.accept(new ItemStack(ModBlocks.FLAME_WAR.get()));
        add.accept(new ItemStack(ModBlocks.THERM_ENDO.get()));
        add.accept(new ItemStack(ModBlocks.THERM_EXO.get()));
        add.accept(new ItemStack(ModBlocks.DET_CORD.get()));
        add.accept(new ItemStack(ModBlocks.DET_CHARGE.get()));
        add.accept(new ItemStack(ModBlocks.DET_NUKE.get()));
        add.accept(new ItemStack(ModBlocks.DET_MINER.get()));
        add.accept(new ItemStack(ModBlocks.BARREL_RED.get()));
        add.accept(new ItemStack(ModBlocks.BARREL_PINK.get()));
        add.accept(new ItemStack(ModBlocks.BARREL_YELLOW.get()));
        add.accept(new ItemStack(ModBlocks.BARREL_VITRIFIED.get()));
        add.accept(new ItemStack(ModBlocks.BARREL_LOX.get()));
        add.accept(new ItemStack(ModBlocks.BARREL_TAINT.get()));
        add.accept(new ItemStack(ModBlocks.FIREWORKS.get()));
        add.accept(new ItemStack(ModBlocks.CHARGE_DYNAMITE.get()));
        add.accept(new ItemStack(ModBlocks.CHARGE_MINER.get()));
        add.accept(new ItemStack(ModBlocks.CHARGE_C4.get()));
        add.accept(new ItemStack(ModBlocks.CHARGE_SEMTEX.get()));
        add.accept(new ItemStack(ModBlocks.MINE_AP.get()));
        add.accept(new ItemStack(ModBlocks.MINE_HE.get()));
        add.accept(new ItemStack(ModBlocks.MINE_SHRAP.get()));
        add.accept(new ItemStack(ModBlocks.MINE_FAT.get()));
        add.accept(new ItemStack(ModBlocks.MINE_NAVAL.get()));
        add.accept(new ItemStack(ModBlocks.DYNAMITE.get()));
        add.accept(new ItemStack(ModBlocks.SEMTEX.get()));
        add.accept(new ItemStack(ModBlocks.C4.get()));
        add.accept(new ItemStack(ModBlocks.FISSURE_BOMB.get()));
        add.accept(new ItemStack(ModItems.BOOK_GUIDE.get()));
        add.accept(new ItemStack(ModBlocks.VOLCANO_CORE.get()));
        add.accept(new ItemStack(ModBlocks.VOLCANO_RAD_CORE.get()));
        add.accept(new ItemStack(ModItems.DEMON_CORE_OPEN.get()));
        add.accept(new ItemStack(ModItems.DEMON_CORE_CLOSED.get()));
        add.accept(new ItemStack(ModItems.DEFUSER.get()));
        add.accept(new ItemStack(ModItems.FAT_MAN_EXPLOSIVE.get()));
        add.accept(new ItemStack(ModItems.EXPLOSIVE_LENSES.get()));
        add.accept(new ItemStack(ModItems.GADGET_WIREING.get()));
        add.accept(new ItemStack(ModItems.GADGET_CORE.get()));
        add.accept(new ItemStack(ModItems.BOY_IGNITER.get()));
        add.accept(new ItemStack(ModItems.BOY_PROPELLANT.get()));
        add.accept(new ItemStack(ModItems.BOY_BULLET.get()));
        add.accept(new ItemStack(ModItems.BOY_TARGET.get()));
        add.accept(new ItemStack(ModItems.BOY_SHIELDING.get()));
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
        add.accept(new ItemStack(ModItems.CUSTOM_SCHRAB.get()));
        add.accept(new ItemStack(ModItems.CUSTOM_FALL.get()));
        addBattery(add, ModItems.BATTERY_SPARK.get());
        addBattery(add, ModItems.BATTERY_TRIXITE.get());
        add.accept(new ItemStack(ModItems.GADGET_KIT.get()));
        add.accept(new ItemStack(ModItems.BOY_KIT.get()));
        add.accept(new ItemStack(ModItems.MAN_KIT.get()));
        add.accept(new ItemStack(ModItems.MIKE_KIT.get()));
        add.accept(new ItemStack(ModItems.TSAR_KIT.get()));
        add.accept(new ItemStack(ModItems.MULTI_KIT.get()));
        add.accept(new ItemStack(ModItems.CUSTOM_KIT.get()));
        add.accept(new ItemStack(ModItems.FLEIJA_KIT.get()));
        add.accept(new ItemStack(ModItems.PROTOTYPE_KIT.get()));
        add.accept(new ItemStack(ModItems.SOLINIUM_KIT.get()));
        add.accept(new ItemStack(ModItems.IGNITER.get()));
        add.accept(new ItemStack(ModItems.DETONATOR.get()));
        add.accept(new ItemStack(ModItems.MULTI_DETONATOR.get()));
        add.accept(new ItemStack(ModItems.RANGE_DETONATOR.get()));
        add.accept(new ItemStack(ModItems.DETONATOR_DEADMAN.get()));
        add.accept(new ItemStack(ModItems.DETONATOR_DE.get()));
    }

    /** populateMissilesTab: порядок из оригинального 1.7.10 (ModBlocks/ModItems, вкладка missile); отсутствующие в порте предметы пропущены. */
    public static void populateMissilesTab(BiConsumer<ItemStack, CreativeModeTab.TabVisibility> acceptor) {
        Consumer<ItemStack> add = stack -> acceptor.accept(stack, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);

        add.accept(new ItemStack(ModBlocks.MACHINE_SATLINKER.get()));
        add.accept(new ItemStack(ModBlocks.STRUCT_LAUNCHER.get()));
        add.accept(new ItemStack(ModBlocks.STRUCT_SCAFFOLD.get()));
        add.accept(new ItemStack(ModBlocks.STRUCT_LAUNCHER_CORE.get()));
        add.accept(new ItemStack(ModBlocks.STRUCT_LAUNCHER_CORE_LARGE.get()));
        add.accept(new ItemStack(ModBlocks.STRUCT_SOYUZ_CORE.get()));
        add.accept(new ItemStack(ModItems.LAUNCH_PAD.get()));
        add.accept(new ItemStack(ModItems.LAUNCH_PAD_RUSTED.get()));
        add.accept(new ItemStack(ModBlocks.MACHINE_RADAR.get()));
        add.accept(new ItemStack(ModItems.RADAR_SCREEN.get()));
        add.accept(new ItemStack(ModBlocks.MACHINE_MISSILE_ASSEMBLY.get()));
        add.accept(new ItemStack(ModBlocks.COMPACT_LAUNCHER.get()));
        add.accept(new ItemStack(ModBlocks.LAUNCH_TABLE.get()));
        add.accept(new ItemStack(ModBlocks.SOYUZ_LAUNCHER.get()));
        add.accept(new ItemStack(ModBlocks.SAT_DOCK.get()));
        add.accept(new ItemStack(ModBlocks.SOYUZ_CAPSULE.get()));
        add.accept(new ItemStack(ModBlocks.MACHINE_FORCEFIELD.get()));
        add.accept(new ItemStack(ModItems.RANGEFINDER.get()));
        add.accept(new ItemStack(ModItems.DESIGNATOR.get()));
        add.accept(new ItemStack(ModItems.DESIGNATOR_RANGE.get()));
        add.accept(new ItemStack(ModItems.DESIGNATOR_MANUAL.get()));
        add.accept(new ItemStack(ModItems.DESIGNATOR_ARTY_RANGE.get()));
        add.accept(new ItemStack(ModItems.MISSILE_GENERIC.get()));
        add.accept(new ItemStack(ModItems.MISSILE_ANTI_BALLISTIC.get()));
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
        add.accept(new ItemStack(ModItems.MISSILE_SOYUZ_LANDER.get()));
        add.accept(new ItemStack(ModItems.SAT_GERALD.get()));
        add.accept(new ItemStack(ModItems.SAT_CHIP.get()));
        add.accept(new ItemStack(ModItems.SAT_COORD.get()));
        add.accept(new ItemStack(ModItems.SAT_DESIGNATOR.get()));
        add.accept(new ItemStack(ModItems.SAT_RELAY.get()));
        add.accept(new ItemStack(ModItems.MP_C_1.get()));
        add.accept(new ItemStack(ModItems.MP_C_2.get()));
        add.accept(new ItemStack(ModItems.MP_C_3.get()));
        add.accept(new ItemStack(ModItems.MP_C_4.get()));
        add.accept(new ItemStack(ModItems.MP_C_5.get()));
        add.accept(new ItemStack(ModItems.MISSILE_KIT.get()));
        add.accept(new ItemStack(ModItems.LOOT_10.get()));
        add.accept(new ItemStack(ModItems.LOOT_15.get()));
        add.accept(new ItemStack(ModItems.LOOT_MISC.get()));

        MissileTab.appendExtraItems(add);
    }

    /** populateWeaponsTab: порядок из оригинального 1.7.10 (ModBlocks/ModItems, вкладка weapon); отсутствующие в порте предметы пропущены. */
    public static void populateWeaponsTab(BiConsumer<ItemStack, CreativeModeTab.TabVisibility> acceptor) {
        Consumer<ItemStack> add = stack -> acceptor.accept(stack, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);

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
        add.accept(new ItemStack(ModBlocks.TURRET_SENTRY.get()));
        add.accept(new ItemStack(ModItems.AMMO_SHELL.get()));
        add.accept(new ItemStack(ModItems.AMMO_DGK.get()));
        add.accept(new ItemStack(ModItems.AMMO_FIREEXT.get()));
        add.accept(new ItemStack(ModItems.GUN_B92.get()));
        add.accept(new ItemStack(ModBlocks.CRUCIBLE.get()));
        add.accept(new ItemStack(ModItems.STICK_DYNAMITE.get()));
        add.accept(new ItemStack(ModItems.STICK_DYNAMITE_FISHING.get()));
        add.accept(new ItemStack(ModItems.STICK_TNT.get()));
        add.accept(new ItemStack(ModItems.STICK_SEMTEX.get()));
        add.accept(new ItemStack(ModItems.STICK_C4.get()));
        add.accept(new ItemStack(ModItems.GRENADE_UNIVERSAL.get()));
        add.accept(new ItemStack(ModItems.ULLAPOOL_CABER.get()));
        add.accept(new ItemStack(ModItems.AMMO_CONTAINER.get()));
        add.accept(new ItemStack(ModItems.TURRET_CHIP.get()));
        add.accept(new ItemStack(ModItems.DISPERSER_CANISTER.get()));
        add.accept(new ItemStack(ModItems.GLYPHID_GLAND.get()));
    }

    /** populateConsumablesTab: порядок из оригинального 1.7.10 (ModBlocks/ModItems, вкладка consumable); отсутствующие в порте предметы пропущены. */
    public static void populateConsumablesTab(BiConsumer<ItemStack, CreativeModeTab.TabVisibility> acceptor) {
        Consumer<ItemStack> add = stack -> acceptor.accept(stack, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);

        add.accept(new ItemStack(ModBlocks.MACHINE_KEYFORGE.get()));
        add.accept(new ItemStack(ModBlocks.ARMOR_TABLE.get()));
        add.accept(new ItemStack(ModBlocks.CRATE.get()));
        add.accept(new ItemStack(ModBlocks.CRATE_WEAPON.get()));
        add.accept(new ItemStack(ModBlocks.CRATE_LEAD.get()));
        add.accept(new ItemStack(ModBlocks.CRATE_METAL.get()));
        add.accept(new ItemStack(ModBlocks.CRATE_CAN.get()));
        add.accept(new ItemStack(ModBlocks.CRATE_AMMO.get()));
        add.accept(new ItemStack(ModBlocks.CRATE_JUNGLE.get()));
        add.accept(new ItemStack(ModItems.SYRINGE_EMPTY.get()));
        add.accept(new ItemStack(ModItems.SYRINGE_METAL_EMPTY.get()));
        add.accept(new ItemStack(ModItems.SYRINGE_METAL_STIMPAK.get()));
        add.accept(new ItemStack(ModItems.SYRINGE_METAL_MEDX.get()));
        add.accept(new ItemStack(ModItems.SYRINGE_METAL_PSYCHO.get()));
        add.accept(new ItemStack(ModItems.SYRINGE_METAL_SUPER.get()));
        add.accept(new ItemStack(ModItems.SYRINGE_TAINT.get()));
        add.accept(new ItemStack(ModItems.MED_BAG.get()));
        add.accept(new ItemStack(ModItems.SIOX.get()));
        add.accept(new ItemStack(ModItems.PILL_HERBAL.get()));
        add.accept(new ItemStack(ModItems.XANAX.get()));
        add.accept(new ItemStack(ModItems.PILL_IODINE.get()));
        add.accept(new ItemStack(ModItems.PLAN_C.get()));
        add.accept(new ItemStack(ModItems.PILL_RED.get()));
        add.accept(new ItemStack(ModItems.STEALTH_BOY.get()));
        add.accept(new ItemStack(ModItems.JETPACK_TANK.get()));
        add.accept(new ItemStack(ModItems.GUN_KIT_1.get()));
        add.accept(new ItemStack(ModItems.GUN_KIT_2.get()));
        add.accept(new ItemStack(ModItems.CIGARETTE.get()));
        add.accept(new ItemStack(ModItems.CRACKPIPE.get()));
        add.accept(new ItemStack(ModItems.BDCL.get()));
        add.accept(new ItemStack(ModItems.CAP_NUKA.get()));
        add.accept(new ItemStack(ModItems.CAP_QUANTUM.get()));
        add.accept(new ItemStack(ModItems.CAP_SPARKLE.get()));
        add.accept(new ItemStack(ModItems.CAP_RAD.get()));
        add.accept(new ItemStack(ModItems.CAP_KORL.get()));
        add.accept(new ItemStack(ModItems.CAP_FRITZ.get()));
        add.accept(new ItemStack(ModItems.RING_PULL.get()));
        add.accept(new ItemStack(ModItems.CAN_EMPTY.get()));
        add.accept(new ItemStack(ModItems.CHOCOLATE.get()));
        add.accept(new ItemStack(ModItems.CAN_KEY.get()));
        add.accept(new ItemStack(ModItems.COIN_CREEPER.get()));
        add.accept(new ItemStack(ModItems.COIN_RADIATION.get()));
        add.accept(new ItemStack(ModItems.COIN_MASKMAN.get()));
        add.accept(new ItemStack(ModItems.COIN_WORM.get()));
        add.accept(new ItemStack(ModItems.COIN_UFO.get()));
        add.accept(new ItemStack(ModItems.COIN_TOKEN.get()));
        add.accept(new ItemStack(ModItems.CONTAINMENT_BOX.get()));
        add.accept(new ItemStack(ModItems.PLASTIC_BAG.get()));
        add.accept(new ItemStack(ModItems.AMMO_BAG.get()));
        add.accept(new ItemStack(ModItems.AMMO_BAG_INFINITE.get()));
        add.accept(new ItemStack(ModItems.CASING_BAG.get()));
        add.accept(new ItemStack(ModItems.BOMB_WAFFLE.get()));
        add.accept(new ItemStack(ModItems.SCHNITZEL_VEGAN.get()));
        add.accept(new ItemStack(ModItems.COTTON_CANDY.get()));
        add.accept(new ItemStack(ModItems.APPLE_LEAD.get()));
        add.accept(new ItemStack(ModItems.APPLE_SCHRABIDIUM.get()));
        add.accept(new ItemStack(ModItems.TEM_FLAKES.get()));
        add.accept(new ItemStack(ModItems.GLOWING_STEW.get()));
        add.accept(new ItemStack(ModItems.BALEFIRE_SCRAMBLED.get()));
        add.accept(new ItemStack(ModItems.BALEFIRE_AND_HAM.get()));
        add.accept(new ItemStack(ModItems.LEMON.get()));
        add.accept(new ItemStack(ModItems.DEFINITELYFOOD.get()));
        add.accept(new ItemStack(ModItems.MED_IPECAC.get()));
        add.accept(new ItemStack(ModItems.MED_PTSD.get()));
        add.accept(new ItemStack(ModItems.LOOPS.get()));
        add.accept(new ItemStack(ModItems.LOOP_STEW.get()));
        add.accept(new ItemStack(ModItems.SPONGEBOB_MACARONI.get()));
        add.accept(new ItemStack(ModItems.FOODITEM.get()));
        add.accept(new ItemStack(ModItems.TWINKIE.get()));
        add.accept(new ItemStack(ModItems.STATIC_SANDWICH.get()));
        add.accept(new ItemStack(ModItems.PUDDING.get()));
        add.accept(new ItemStack(ModItems.CANTEEN_VODKA.get()));
        add.accept(new ItemStack(ModItems.PANCAKE.get()));
        add.accept(new ItemStack(ModItems.NUGGET.get()));
        add.accept(new ItemStack(ModItems.PEAS.get()));
        add.accept(new ItemStack(ModItems.MARSHMALLOW.get()));
        add.accept(new ItemStack(ModItems.CHEESE.get()));
        add.accept(new ItemStack(ModItems.MUCHO_MANGO.get()));
        add.accept(new ItemStack(ModItems.GLYPHID_MEAT.get()));
        add.accept(new ItemStack(ModItems.GLYPHID_MEAT_GRILLED.get()));
        add.accept(new ItemStack(ModItems.EGG_GLYPHID.get()));
        add.accept(new ItemStack(ModItems.REBAR_PLACER.get()));
        add.accept(new ItemStack(ModItems.WAND_S.get()));
        add.accept(new ItemStack(ModItems.WAND_D.get()));
        add.accept(new ItemStack(ModItems.STRUCTURE_CUSTOMMACHINE.get()));
        add.accept(new ItemStack(ModItems.ROD_OF_DISCORD.get()));
        add.accept(new ItemStack(ModItems.NUKE_STARTER_KIT.get()));
        add.accept(new ItemStack(ModItems.NUKE_ADVANCED_KIT.get()));
        add.accept(new ItemStack(ModItems.NUKE_COMMERCIALLY_KIT.get()));
        add.accept(new ItemStack(ModItems.NUKE_ELECTRIC_KIT.get()));
        add.accept(new ItemStack(ModItems.HAZMAT_KIT.get()));
        add.accept(new ItemStack(ModItems.HAZMAT_RED_KIT.get()));
        add.accept(new ItemStack(ModItems.HAZMAT_GREY_KIT.get()));
        add.accept(new ItemStack(ModItems.BOMB_CALLER.get()));
        add.accept(new ItemStack(ModItems.METEOR_REMOTE.get()));
        add.accept(new ItemStack(ModItems.ANCHOR_REMOTE.get()));
        add.accept(new ItemStack(ModItems.CHOPPER.get()));
        add.accept(new ItemStack(ModItems.SPAWN_WORM.get()));
        add.accept(new ItemStack(ModItems.SPAWN_UFO.get()));
        add.accept(new ItemStack(ModItems.SPAWN_DUCK.get()));
        add.accept(new ItemStack(ModItems.LINKER.get()));
        add.accept(new ItemStack(ModItems.REACTOR_SENSOR.get()));
        add.accept(new ItemStack(ModItems.OIL_DETECTOR.get()));
        add.accept(new ItemStack(ModItems.DOSIMETER.get()));
        add.accept(new ItemStack(ModItems.GEIGER_COUNTER.get()));
        add.accept(new ItemStack(ModItems.DIGAMMA_DIAGNOSTIC.get()));
        add.accept(new ItemStack(ModItems.POLLUTION_DETECTOR.get()));
        add.accept(new ItemStack(ModItems.ORE_DENSITY_SCANNER.get()));
        add.accept(new ItemStack(ModItems.SURVEY_SCANNER.get()));
        add.accept(new ItemStack(ModItems.MIRROR_TOOL.get()));
        add.accept(new ItemStack(ModItems.RBMK_TOOL.get()));
        add.accept(new ItemStack(ModItems.POWER_NET_TOOL.get()));
        add.accept(new ItemStack(ModItems.ANALYSIS_TOOL.get()));
        add.accept(new ItemStack(ModItems.DRONE_LINKER.get()));
        add.accept(new ItemStack(ModItems.RADAR_LINKER.get()));
        add.accept(new ItemStack(ModItems.SETTINGS_TOOL.get()));
        add.accept(new ItemStack(ModItems.RTTY_PAGER.get()));
        add.accept(new ItemStack(ModItems.KEY.get()));
        add.accept(new ItemStack(ModItems.PIN.get()));
        add.accept(new ItemStack(ModItems.PADLOCK_RUSTY.get()));
        add.accept(new ItemStack(ModItems.PADLOCK.get()));
        add.accept(new ItemStack(ModItems.PADLOCK_REINFORCED.get()));
        add.accept(new ItemStack(ModItems.BOBMAZON.get()));
        add.accept(new ItemStack(ModItems.BOTTLE_OPENER.get()));
        add.accept(new ItemStack(ModItems.BOOK_GUIDE.get()));
        add.accept(new ItemStack(ModItems.POLAROID.get()));
        add.accept(new ItemStack(ModItems.GLITCH.get()));
        add.accept(new ItemStack(ModItems.GAS_MASK.get()));  // no creative tab in original - added manually
        add.accept(new ItemStack(ModItems.GAS_MASK_M65.get()));  // no creative tab in original - added manually
        add.accept(new ItemStack(ModItems.GAS_MASK_MONO.get()));  // no creative tab in original - added manually
        add.accept(new ItemStack(ModItems.GAS_MASK_OLDE.get()));  // no creative tab in original - added manually
        add.accept(new ItemStack(ModItems.MASK_RAG.get()));  // no creative tab in original - added manually
        add.accept(new ItemStack(ModItems.MASK_PISS.get()));  // no creative tab in original - added manually
        add.accept(new ItemStack(ModItems.GAS_MASK_FILTER.get()));  // no creative tab in original - added manually
        add.accept(new ItemStack(ModItems.GAS_MASK_FILTER_MONO.get()));  // no creative tab in original - added manually
        add.accept(new ItemStack(ModItems.GAS_MASK_FILTER_COMBO.get()));  // no creative tab in original - added manually
        add.accept(new ItemStack(ModItems.GAS_MASK_FILTER_RAG.get()));  // no creative tab in original - added manually
        add.accept(new ItemStack(ModItems.GAS_MASK_FILTER_PISS.get()));  // no creative tab in original - added manually
    }


    // ==================== Ванильная вкладка «Бой» (броня и инструменты) ====================

        public static void populateCombatTab(BiConsumer<ItemStack, CreativeModeTab.TabVisibility> acceptor) {
        // ДОБАВЛЕНА ЗАЩИТА ОТ ВНУТРЕННИХ ДУБЛИКАТОВ (поскольку в коде ниже много повторяющихся шлемов и мечей)
        Set<String> seen = new HashSet<>();
        Consumer<ItemStack> add = stack -> {
            if (stack == null || stack.isEmpty()) return;
            String itemId = String.valueOf(BuiltInRegistries.ITEM.getKey(stack.getItem()));
            CompoundTag itemTag = PlatformHooks.getItemTag(stack);
            String tag = itemTag == null ? "" : itemTag.toString();
            if (!seen.add(itemId + "|" + tag)) return; // Игнорируем дубликат, чтобы не крашнуть 1.21.1
            acceptor.accept(stack, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
        };

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
        add.accept(new ItemStack(ModItems.METEORITE_SWORD_HARDENED.get()));
        add.accept(new ItemStack(ModItems.METEORITE_SWORD_ALLOYED.get()));
        add.accept(new ItemStack(ModItems.CROWBAR.get()));

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

    // ==================== Dev-вкладка (без изменений) ====================

        public static void populateDevItemsTab(BiConsumer<ItemStack, CreativeModeTab.TabVisibility> acceptor) {
        Consumer<ItemStack> add = stack -> acceptor.accept(stack, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);





        // ─── AUTO-PORT: fehlende Original-Bloecke (nur DEV-Tab, ungeprueft) ───
        add.accept(new ItemStack(ModBlocks.BLOCK_ASBESTOS.get()));
        add.accept(new ItemStack(ModBlocks.BLOCK_BAKELITE.get()));
        add.accept(new ItemStack(ModBlocks.BLOCK_C4.get()));
        add.accept(new ItemStack(ModBlocks.BLOCK_COLTAN.get()));
        add.accept(new ItemStack(ModBlocks.BLOCK_CORIUM.get()));
        add.accept(new ItemStack(ModBlocks.BLOCK_CORIUM_COBBLE.get()));
        add.accept(new ItemStack(ModBlocks.BLOCK_EUPHEMIUM_CLUSTER.get()));
        add.accept(new ItemStack(ModBlocks.BLOCK_FIBERGLASS.get()));
        add.accept(new ItemStack(ModBlocks.BLOCK_FLUORITE.get()));
        add.accept(new ItemStack(ModBlocks.BLOCK_GRAPHITE_DETECTOR.get()));
        add.accept(new ItemStack(ModBlocks.BLOCK_GRAPHITE_DRILLED.get()));
        add.accept(new ItemStack(ModBlocks.BLOCK_GRAPHITE_FUEL.get()));
        add.accept(new ItemStack(ModBlocks.BLOCK_GRAPHITE_LITHIUM.get()));
        add.accept(new ItemStack(ModBlocks.BLOCK_GRAPHITE_PLUTONIUM.get()));
        add.accept(new ItemStack(ModBlocks.BLOCK_GRAPHITE_SOURCE.get()));
        add.accept(new ItemStack(ModBlocks.BLOCK_GRAPHITE_TRITIUM.get()));
        add.accept(new ItemStack(ModBlocks.BLOCK_INSULATOR.get()));
        add.accept(new ItemStack(ModBlocks.BLOCK_LITHIUM.get()));
        add.accept(new ItemStack(ModBlocks.BLOCK_MAGNETIZED_TUNGSTEN.get()));
        add.accept(new ItemStack(ModBlocks.BLOCK_METEOR.get()));
        add.accept(new ItemStack(ModBlocks.BLOCK_METEOR_BROKEN.get()));
        add.accept(new ItemStack(ModBlocks.BLOCK_METEOR_COBBLE.get()));
        add.accept(new ItemStack(ModBlocks.BLOCK_METEOR_MOLTEN.get()));
        add.accept(new ItemStack(ModBlocks.BLOCK_METEOR_TREASURE.get()));
        add.accept(new ItemStack(ModBlocks.BLOCK_NITER.get()));
        add.accept(new ItemStack(ModBlocks.BLOCK_POLYMER.get()));
        add.accept(new ItemStack(ModBlocks.BLOCK_PU_MIX.get()));
        add.accept(new ItemStack(ModBlocks.BLOCK_RED_PHOSPHORUS.get()));
        add.accept(new ItemStack(ModBlocks.BLOCK_RUBBER.get()));
        add.accept(new ItemStack(ModBlocks.BLOCK_SCRAP.get()));
        add.accept(new ItemStack(ModBlocks.BLOCK_SEMTEX.get()));
        add.accept(new ItemStack(ModBlocks.BLOCK_SMORE.get()));
        add.accept(new ItemStack(ModBlocks.BLOCK_SULFUR.get()));
        add.accept(new ItemStack(ModBlocks.BLOCK_TANTALIUM.get()));
        add.accept(new ItemStack(ModBlocks.BLOCK_TRINITITE.get()));
        add.accept(new ItemStack(ModBlocks.BLOCK_TRITIUM.get()));
        add.accept(new ItemStack(ModBlocks.BLOCK_WASTE.get()));
        add.accept(new ItemStack(ModBlocks.BLOCK_WASTE_VITRIFIED.get()));
        add.accept(new ItemStack(ModBlocks.BLOCK_WHITE_PHOSPHORUS.get()));
        add.accept(new ItemStack(ModBlocks.BLOCK_YELLOWCAKE.get()));
        add.accept(new ItemStack(ModBlocks.BRICK_FORGOTTEN.get()));
        add.accept(new ItemStack(ModBlocks.CONCRETE_LIQUID.get()));
        add.accept(new ItemStack(ModBlocks.DIGAMMA_MATTER.get()));
        add.accept(new ItemStack(ModBlocks.DUNGEON_SPAWNER.get()));
        add.accept(new ItemStack(ModBlocks.EVENT_TESTER.get()));
        add.accept(new ItemStack(ModBlocks.FLUID_DUCT_PAINTABLE_BLOCK_EXHAUST.get()));
        add.accept(new ItemStack(ModBlocks.GEIGER_COUNTER_BLOCK.get()));
        add.accept(new ItemStack(ModBlocks.GEYSIR_NETHER.get()));
        add.accept(new ItemStack(ModBlocks.ICF_BLOCK.get()));
        add.accept(new ItemStack(ModBlocks.LAUNCH_TABLE.get()));
        add.accept(new ItemStack(ModBlocks.LOGIC_BLOCK.get()));
        add.accept(new ItemStack(ModBlocks.MACHINE_RADAR.get()));
        add.accept(new ItemStack(ModBlocks.MUSH_BLOCK_STEM.get()));
        add.accept(new ItemStack(ModBlocks.ORE_ALEXANDRITE.get()));
        add.accept(new ItemStack(ModBlocks.ORE_ALUMINIUM.get()));
        add.accept(new ItemStack(ModBlocks.ORE_AUSTRALIUM.get()));
        add.accept(new ItemStack(ModBlocks.ORE_CINNEBAR.get()));
        add.accept(new ItemStack(ModBlocks.ORE_COLTAN.get()));
        add.accept(new ItemStack(ModBlocks.ORE_COPPER.get()));
        add.accept(new ItemStack(ModBlocks.ORE_DEPTH_BORAX.get()));
        add.accept(new ItemStack(ModBlocks.ORE_DEPTH_CINNEBAR.get()));
        add.accept(new ItemStack(ModBlocks.ORE_DEPTH_NETHER_NEODYMIUM.get()));
        add.accept(new ItemStack(ModBlocks.ORE_DEPTH_ZIRCONIUM.get()));
        add.accept(new ItemStack(ModBlocks.ORE_GNEISS_ASBESTOS.get()));
        add.accept(new ItemStack(ModBlocks.ORE_GNEISS_COPPER.get()));
        add.accept(new ItemStack(ModBlocks.ORE_GNEISS_GAS.get()));
        add.accept(new ItemStack(ModBlocks.ORE_GNEISS_GOLD.get()));
        add.accept(new ItemStack(ModBlocks.ORE_GNEISS_IRON.get()));
        add.accept(new ItemStack(ModBlocks.ORE_GNEISS_LITHIUM.get()));
        add.accept(new ItemStack(ModBlocks.ORE_GNEISS_RARE.get()));
        add.accept(new ItemStack(ModBlocks.ORE_GNEISS_URANIUM.get()));
        add.accept(new ItemStack(ModBlocks.ORE_GNEISS_URANIUM_SCORCHED.get()));
        add.accept(new ItemStack(ModBlocks.ORE_NETHER_COAL.get()));
        add.accept(new ItemStack(ModBlocks.ORE_NETHER_COBALT.get()));
        add.accept(new ItemStack(ModBlocks.ORE_NETHER_FIRE.get()));
        add.accept(new ItemStack(ModBlocks.ORE_NETHER_PLUTONIUM.get()));
        add.accept(new ItemStack(ModBlocks.ORE_NETHER_SMOLDERING.get()));
        add.accept(new ItemStack(ModBlocks.ORE_NETHER_SULFUR.get()));
        add.accept(new ItemStack(ModBlocks.ORE_NETHER_TUNGSTEN.get()));
        add.accept(new ItemStack(ModBlocks.ORE_NETHER_URANIUM.get()));
        add.accept(new ItemStack(ModBlocks.ORE_NETHER_URANIUM_SCORCHED.get()));
        add.accept(new ItemStack(ModBlocks.ORE_OIL_SAND.get()));
        add.accept(new ItemStack(ModBlocks.ORE_RARE.get()));
        add.accept(new ItemStack(ModBlocks.ORE_TEKTITE_OSMIRIDIUM.get()));
        add.accept(new ItemStack(ModBlocks.ORE_TIKITE.get()));
        add.accept(new ItemStack(ModBlocks.ORE_URANIUM_SCORCHED.get()));
        add.accept(new ItemStack(ModBlocks.PILE_BLOCK.get()));
        add.accept(new ItemStack(ModBlocks.PILE_BRICK.get()));
        add.accept(new ItemStack(ModBlocks.PNEUMATIC_STORAGE_ACCESS.get()));
        add.accept(new ItemStack(ModBlocks.PNEUMATIC_STORAGE_CLUTTER.get()));
        add.accept(new ItemStack(ModBlocks.PNEUMATIC_STORAGE_EXPORTER.get()));
        add.accept(new ItemStack(ModBlocks.PNEUMATIC_STORAGE_IMPORTER.get()));
        add.accept(new ItemStack(ModBlocks.PNEUMATIC_STORAGE_MONO.get()));
        add.accept(new ItemStack(ModBlocks.SOLAR_MIRROR.get()));
        add.accept(new ItemStack(ModBlocks.STEEL_BEAM.get()));
        add.accept(new ItemStack(ModBlocks.STRUCTURE_ANCHOR.get()));
        add.accept(new ItemStack(ModBlocks.WAND_TANDEM.get()));
        // ─── ENDE AUTO-PORT Bloecke ───

        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.IRON, MaterialShape.PLATE_CAST)));
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.STEEL, MaterialShape.PLATE_CAST)));
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.COPPER, MaterialShape.PLATE_CAST)));
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.GOLD, MaterialShape.PLATE_CAST)));
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.TITANIUM, MaterialShape.PLATE_CAST)));
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.ALUMINIUM, MaterialShape.PLATE_CAST)));
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.TUNGSTEN, MaterialShape.PLATE_CAST)));
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.ZIRCONIUM, MaterialShape.PLATE_CAST)));
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.OSMIRIDIUM, MaterialShape.PLATE_CAST)));
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.ALLOY, MaterialShape.PLATE_CAST)));
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.DURA_STEEL, MaterialShape.PLATE_CAST)));
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.DESH, MaterialShape.PLATE_CAST)));
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.STAR_METAL, MaterialShape.PLATE_CAST)));
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.TCALLOY, MaterialShape.PLATE_CAST)));
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.CDALLOY, MaterialShape.PLATE_CAST)));
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.CMB, MaterialShape.PLATE_CAST)));
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.SCHRABIDIUM, MaterialShape.PLATE_CAST)));
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.BBRONZE, MaterialShape.PLATE_CAST)));
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.ABRONZE, MaterialShape.PLATE_CAST)));
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.SATURNITE, MaterialShape.PLATE_CAST)));
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.IRON, MaterialShape.PLATE_WELDED)));
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.STEEL, MaterialShape.PLATE_WELDED)));
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.COPPER, MaterialShape.PLATE_WELDED)));
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.TITANIUM, MaterialShape.PLATE_WELDED)));
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.ALUMINIUM, MaterialShape.PLATE_WELDED)));
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.TUNGSTEN, MaterialShape.PLATE_WELDED)));
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.ZIRCONIUM, MaterialShape.PLATE_WELDED)));
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.OSMIRIDIUM, MaterialShape.PLATE_WELDED)));
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.TCALLOY, MaterialShape.PLATE_WELDED)));
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.CDALLOY, MaterialShape.PLATE_WELDED)));
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.CMB, MaterialShape.PLATE_WELDED)));
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
        // add.accept(new ItemStack(ModBlocks.MIXER.get()));
        add.accept(new ItemStack(ModBlocks.RBMK_CONSOLE.get()));
        add.accept(new ItemStack(ModBlocks.FLARE_STACK.get()));
        add.accept(new ItemStack(ModBlocks.PUMPJACK.get()));
        add.accept(new ItemStack(ModBlocks.PUMP_STEAM.get()));
        add.accept(new ItemStack(ModBlocks.PUMP_ELECTRIC.get()));
        add.accept(new ItemStack(ModBlocks.CRACKING_TOWER.get()));
        add.accept(new ItemStack(ModBlocks.FEL.get()));
        add.accept(new ItemStack(ModBlocks.SILEX.get()));
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
        add.accept(new ItemStack(ModBlocks.CONVERTER_BLOCK.get()));
        add.accept(new ItemStack(ModBlocks.WIRE_COATED.get()));
        add.accept(new ItemStack(ModBlocks.DECON.get()));
        add.accept(new ItemStack(ModBlocks.EMP.get()));

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
        add.accept(new ItemStack(ModBlocks.LPW2.get()));
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
        add.accept(new ItemStack(ModBlocks.ASHPIT.get()));
        add.accept(new ItemStack(ModBlocks.REACTOR_RESEARCH.get()));
        add.accept(new ItemStack(ModBlocks.SOURCE.get()));
        add.accept(new ItemStack(ModBlocks.INDUSTRIAL_GENERATOR.get()));
        add.accept(new ItemStack(ModBlocks.STEAM_ENGINE.get()));
        add.accept(new ItemStack(ModBlocks.STIRLING.get()));
        add.accept(new ItemStack(ModBlocks.STIRLING_CREATIVE.get()));
        add.accept(new ItemStack(ModBlocks.STIRLING_STEEL.get()));
        add.accept(new ItemStack(ModBlocks.STRAND_CASTER.get()));
        add.accept(new ItemStack(ModBlocks.THRESHER.get()));
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
        add.accept(new ItemStack(ModBlocks.CABLE_DETECTOR.get()));
        add.accept(new ItemStack(ModBlocks.CABLE_DIODE.get()));
        add.accept(new ItemStack(ModBlocks.CABLE_SWITCH.get()));
        add.accept(new ItemStack(ModBlocks.CAPACITOR_BUS.get()));
        add.accept(new ItemStack(ModBlocks.CAPACITOR_COPPER.get()));
        add.accept(new ItemStack(ModBlocks.CAPACITOR_GOLD.get()));
        add.accept(new ItemStack(ModBlocks.CAPACITOR_NIOBIUM.get()));
        add.accept(new ItemStack(ModBlocks.CAPACITOR_SCHRABIDATE.get()));
        add.accept(new ItemStack(ModBlocks.CAPACITOR_TANTALIUM.get()));
        add.accept(new ItemStack(ModBlocks.CARGO_ELEVATOR.get()));
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
        add.accept(new ItemStack(ModBlocks.CONVEYOR_LIFT.get()));
        add.accept(new ItemStack(ModBlocks.CONVEYOR_CHUTE.get()));
        add.accept(new ItemStack(ModBlocks.CRANE_BOXER.get()));
        add.accept(new ItemStack(ModBlocks.CRANE_INSERTER.get()));
        add.accept(new ItemStack(ModBlocks.CRANE_EXTRACTOR.get()));
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
        add.accept(new ItemStack(ModBlocks.RADIO_TORCH_SENDER.get()));
        add.accept(new ItemStack(ModBlocks.RADIO_TORCH_RECEIVER.get()));
        add.accept(new ItemStack(ModBlocks.RADIO_TORCH_LOGIC.get()));
        add.accept(new ItemStack(ModBlocks.RADIO_TORCH_READER.get()));
        add.accept(new ItemStack(ModBlocks.RADIO_TORCH_CONTROLLER.get()));
        add.accept(new ItemStack(ModBlocks.RADIO_TORCH_COUNTER.get()));
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
        add.accept(new ItemStack(ModBlocks.FLUID_DUCT_EXHAUST.get()));
        add.accept(new ItemStack(ModBlocks.PIPE_ANCHOR.get()));
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
        add.accept(new ItemStack(ModBlocks.MACHINE_DRAIN.get()));
        add.accept(new ItemStack(ModBlocks.MACHINE_TRANSFORMER.get()));
        add.accept(new ItemStack(ModBlocks.MACHINE_WASTE_DRUM.get()));
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
        add.accept(new ItemStack(ModBlocks.RED_CABLE_PAINTABLE.get()));
        add.accept(new ItemStack(ModBlocks.RED_CABLE_GAUGE.get()));
        add.accept(new ItemStack(ModBlocks.RED_CABLE_BOX.get()));
        add.accept(new ItemStack(ModBlocks.RED_CONNECTOR.get()));
        add.accept(new ItemStack(ModBlocks.RED_CONNECTOR_SUPER.get()));
        add.accept(new ItemStack(ModBlocks.RED_PYLON.get()));
        add.accept(new ItemStack(ModBlocks.RED_PYLON_STEEL.get()));
        add.accept(new ItemStack(ModBlocks.RED_PYLON_MEDIUM_WOOD.get()));
        add.accept(new ItemStack(ModBlocks.RED_PYLON_MEDIUM_WOOD_TRANSFORMER.get()));
        add.accept(new ItemStack(ModBlocks.RED_PYLON_MEDIUM_STEEL.get()));
        add.accept(new ItemStack(ModBlocks.RED_PYLON_MEDIUM_STEEL_TRANSFORMER.get()));
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
        add.accept(new ItemStack(ModBlocks.WATZ_END.get()));
        add.accept(new ItemStack(ModBlocks.WATZ_END_BOLTED.get()));
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
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.ACTINIUM, MaterialShape.BILLET)));
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.AM241, MaterialShape.BILLET)));
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.AM242, MaterialShape.BILLET)));
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.AM_MIX, MaterialShape.BILLET)));
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.AMERICIUM_FUEL, MaterialShape.BILLET)));
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.AU198, MaterialShape.BILLET)));
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.AUSTRALIUM, MaterialShape.BILLET)));
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.AUSTRALIUM_GREATER, MaterialShape.BILLET)));
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.AUSTRALIUM_LESSER, MaterialShape.BILLET)));
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.BALEFIRE_GOLD, MaterialShape.BILLET)));
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.BERYLLIUM, MaterialShape.BILLET)));
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.BISMUTH, MaterialShape.BILLET)));
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.CO60, MaterialShape.BILLET)));
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.COBALT, MaterialShape.BILLET)));
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.FLASHLEAD, MaterialShape.BILLET)));
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.GH336, MaterialShape.BILLET)));
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.HES, MaterialShape.BILLET)));
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.LES_FUEL, MaterialShape.BILLET)));
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.MOX_FUEL, MaterialShape.BILLET)));
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.NEPTUNIUM, MaterialShape.BILLET)));
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.NEPTUNIUM_FUEL, MaterialShape.BILLET)));
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.NUCLEAR_WASTE, MaterialShape.BILLET)));
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.PB209, MaterialShape.BILLET)));
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.PLUTONIUM_FUEL, MaterialShape.BILLET)));
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.PO210BE, MaterialShape.BILLET)));
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.POLONIUM, MaterialShape.BILLET)));
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.PLUTONIUM238, MaterialShape.BILLET)));
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.PU238BE, MaterialShape.BILLET)));
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.PLUTONIUM239, MaterialShape.BILLET)));
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.PLUTONIUM240, MaterialShape.BILLET)));
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.PLUTONIUM241, MaterialShape.BILLET)));
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.PU_MIX, MaterialShape.BILLET)));
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.RA226, MaterialShape.BILLET)));
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.RA226BE, MaterialShape.BILLET)));
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.SCHRABIDIUM, MaterialShape.BILLET)));
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.SCHRABIDIUM_FUEL, MaterialShape.BILLET)));
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.SOLINIUM, MaterialShape.BILLET)));
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.SR90, MaterialShape.BILLET)));
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.TECHNETIUM, MaterialShape.BILLET)));
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.THORIUM232, MaterialShape.BILLET)));
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.THORIUM_FUEL, MaterialShape.BILLET)));
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.URANIUM233, MaterialShape.BILLET)));
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.URANIUM235, MaterialShape.BILLET)));
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.URANIUM238, MaterialShape.BILLET)));
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.URANIUM, MaterialShape.BILLET)));
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.URANIUM_FUEL, MaterialShape.BILLET)));
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.UZH, MaterialShape.BILLET)));
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.YHARONITE, MaterialShape.BILLET)));
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.ZFB_AM_MIX, MaterialShape.BILLET)));
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.ZFB_BISMUTH, MaterialShape.BILLET)));
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.ZFB_PU241, MaterialShape.BILLET)));
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.ZIRCONIUM, MaterialShape.BILLET)));
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
        add.accept(new ItemStack(ModItems.BOMB_CALLER_NAPALM.get()));
        add.accept(new ItemStack(ModItems.BOMB_CALLER_CHLORINE.get()));
        add.accept(new ItemStack(ModItems.BOMB_CALLER_ATOMIC.get()));
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
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.COMBINE_SCRAP, MaterialShape.SCRAP)));
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
        add.accept(new ItemStack(ModItems.DRONE_PATROL.get()));
        add.accept(new ItemStack(ModItems.DRONE_PATROL_CHUNKLOADING.get()));
        add.accept(new ItemStack(ModItems.DRONE_PATROL_EXPRESS.get()));
        add.accept(new ItemStack(ModItems.DRONE_PATROL_EXPRESS_CHUNKLOADING.get()));
        add.accept(new ItemStack(ModItems.DRONE_REQUEST.get()));
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
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.ACTINIUM, MaterialShape.NUGGET)));
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.AM241, MaterialShape.NUGGET)));
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.AM242, MaterialShape.NUGGET)));
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.AM_MIX, MaterialShape.NUGGET)));
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.AMERICIUM_FUEL, MaterialShape.NUGGET)));
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.ARSENIC, MaterialShape.NUGGET)));
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.AU198, MaterialShape.NUGGET)));
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.AUSTRALIUM, MaterialShape.NUGGET)));
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.AUSTRALIUM_GREATER, MaterialShape.NUGGET)));
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.AUSTRALIUM_LESSER, MaterialShape.NUGGET)));
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.BERYLLIUM, MaterialShape.NUGGET)));
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.BISMUTH, MaterialShape.NUGGET)));
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.CO60, MaterialShape.NUGGET)));
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.COBALT, MaterialShape.NUGGET)));
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.DESH, MaterialShape.NUGGET)));
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.DINEUTRONIUM, MaterialShape.NUGGET)));
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.EUPHEMIUM, MaterialShape.NUGGET)));
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.GH336, MaterialShape.NUGGET)));
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.HES, MaterialShape.NUGGET)));
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.LEAD, MaterialShape.NUGGET)));
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.LES_FUEL, MaterialShape.NUGGET)));
        add.accept(new ItemStack(ModItems.NUGGET_MERCURY.get()));
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.MOX_FUEL, MaterialShape.NUGGET)));
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.NEPTUNIUM, MaterialShape.NUGGET)));
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.NEPTUNIUM_FUEL, MaterialShape.NUGGET)));
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.NIOBIUM, MaterialShape.NUGGET)));
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.OSMIRIDIUM, MaterialShape.NUGGET)));
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.PB209, MaterialShape.NUGGET)));
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.PLUTONIUM, MaterialShape.NUGGET)));
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.PLUTONIUM_FUEL, MaterialShape.NUGGET)));
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.POLONIUM, MaterialShape.NUGGET)));
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.PLUTONIUM238, MaterialShape.NUGGET)));
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.PLUTONIUM239, MaterialShape.NUGGET)));
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.PLUTONIUM240, MaterialShape.NUGGET)));
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.PLUTONIUM241, MaterialShape.NUGGET)));
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.PU_MIX, MaterialShape.NUGGET)));
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.RA226, MaterialShape.NUGGET)));
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.SCHRABIDIUM, MaterialShape.NUGGET)));
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.SCHRABIDIUM_FUEL, MaterialShape.NUGGET)));
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.SOLINIUM, MaterialShape.NUGGET)));
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.SR90, MaterialShape.NUGGET)));
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.TECHNETIUM, MaterialShape.NUGGET)));
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.THORIUM232, MaterialShape.NUGGET)));
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.THORIUM_FUEL, MaterialShape.NUGGET)));
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.URANIUM233, MaterialShape.NUGGET)));
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.URANIUM235, MaterialShape.NUGGET)));
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.URANIUM238, MaterialShape.NUGGET)));
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.URANIUM, MaterialShape.NUGGET)));
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.URANIUM_FUEL, MaterialShape.NUGGET)));
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.ZIRCONIUM, MaterialShape.NUGGET)));
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
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.POLYMER, MaterialShape.PLATE)));
        add.accept(new ItemStack(ModItems.POLAROID.get()));
        add.accept(new ItemStack(ModItems.POLLUTION_DETECTOR.get()));
        add.accept(new ItemStack(ModItems.POWER_NET_TOOL.get()));
        add.accept(new ItemStack(ModItems.PROTECTION_CHARM.get()));
        add.accept(new ItemStack(ModItems.PROTOTYPE_KIT.get()));
        add.accept(new ItemStack(ModItems.PUDDING.get()));
        add.accept(new ItemStack(ModItems.PWR_PRINTER.get()));
        add.accept(new ItemStack(ModItems.PWR_FUEL_MEU.get()));
        add.accept(new ItemStack(ModItems.PWR_FUEL_MEU_HOT.get()));
        add.accept(new ItemStack(ModItems.PWR_FUEL_HEU233.get()));
        add.accept(new ItemStack(ModItems.PWR_FUEL_HEU233_HOT.get()));
        add.accept(new ItemStack(ModItems.PWR_FUEL_HEU235.get()));
        add.accept(new ItemStack(ModItems.PWR_FUEL_HEU235_HOT.get()));
        add.accept(new ItemStack(ModItems.PWR_FUEL_MEN.get()));
        add.accept(new ItemStack(ModItems.PWR_FUEL_MEN_HOT.get()));
        add.accept(new ItemStack(ModItems.PWR_FUEL_HEN237.get()));
        add.accept(new ItemStack(ModItems.PWR_FUEL_HEN237_HOT.get()));
        add.accept(new ItemStack(ModItems.PWR_FUEL_MOX.get()));
        add.accept(new ItemStack(ModItems.PWR_FUEL_MOX_HOT.get()));
        add.accept(new ItemStack(ModItems.PWR_FUEL_MEP.get()));
        add.accept(new ItemStack(ModItems.PWR_FUEL_MEP_HOT.get()));
        add.accept(new ItemStack(ModItems.PWR_FUEL_HEP239.get()));
        add.accept(new ItemStack(ModItems.PWR_FUEL_HEP239_HOT.get()));
        add.accept(new ItemStack(ModItems.PWR_FUEL_HEP241.get()));
        add.accept(new ItemStack(ModItems.PWR_FUEL_HEP241_HOT.get()));
        add.accept(new ItemStack(ModItems.PWR_FUEL_MEA.get()));
        add.accept(new ItemStack(ModItems.PWR_FUEL_MEA_HOT.get()));
        add.accept(new ItemStack(ModItems.PWR_FUEL_HEA242.get()));
        add.accept(new ItemStack(ModItems.PWR_FUEL_HEA242_HOT.get()));
        add.accept(new ItemStack(ModItems.PWR_FUEL_HES326.get()));
        add.accept(new ItemStack(ModItems.PWR_FUEL_HES326_HOT.get()));
        add.accept(new ItemStack(ModItems.PWR_FUEL_HES327.get()));
        add.accept(new ItemStack(ModItems.PWR_FUEL_HES327_HOT.get()));
        add.accept(new ItemStack(ModItems.PWR_FUEL_BFB_AM_MIX.get()));
        add.accept(new ItemStack(ModItems.PWR_FUEL_BFB_AM_MIX_HOT.get()));
        add.accept(new ItemStack(ModItems.PWR_FUEL_BFB_PU241.get()));
        add.accept(new ItemStack(ModItems.PWR_FUEL_BFB_PU241_HOT.get()));
        add.accept(new ItemStack(ModItems.QUARTZ_PLUTONIUM.get()));
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
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.SCRAP_NUCLEAR, MaterialShape.SCRAP)));
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.SCRAP_OIL, MaterialShape.SCRAP)));
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.SCRAP_PLASTIC, MaterialShape.SCRAP)));
        add.accept(new ItemStack(ModItems.SCRAPS.get()));
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
        // Turrets and their ammunition. All of this sat commented out in the combat tab, so
        // neither the turrets nor a single round were obtainable in creative at all.
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
        add.accept(new ItemStack(ModItems.AMMO_TAU_URANIUM.get()));
        add.accept(new ItemStack(ModItems.UPGRADE_5G.get()));
        add.accept(new ItemStack(ModItems.UPGRADE_SCREM.get()));
        add.accept(new ItemStack(ModItems.AMMO_FLAME_DIESEL.get()));
        add.accept(new ItemStack(ModItems.AMMO_DGK.get()));
        add.accept(new ItemStack(ModItems.ROCKET_TURRET_STANDARD.get()));
        add.accept(new ItemStack(ModItems.ROCKET_TURRET_HEAT.get()));
        add.accept(new ItemStack(ModItems.ROCKET_TURRET_DEMO.get()));
        add.accept(new ItemStack(ModItems.ROCKET_TURRET_INC.get()));
        add.accept(new ItemStack(ModItems.ROCKET_TURRET_PHOSPHORUS.get()));
        add.accept(new ItemStack(ModItems.ROCKET_HIMARS_STANDARD.get()));
        add.accept(new ItemStack(ModItems.ROCKET_HIMARS_HE.get()));
        add.accept(new ItemStack(ModItems.ROCKET_HIMARS_LAVA.get()));
        add.accept(new ItemStack(ModItems.ROCKET_HIMARS_MINI_NUKE.get()));
        add.accept(new ItemStack(ModItems.ROCKET_HIMARS_WP.get()));
        add.accept(new ItemStack(ModItems.ROCKET_HIMARS_THERMOBARIC.get()));
        add.accept(new ItemStack(ModItems.ROCKET_HIMARS_SINGLE.get()));
        add.accept(new ItemStack(ModItems.ROCKET_HIMARS_SINGLE_TB.get()));
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
        add.accept(new ItemStack(ModItems.WASTE_PLATE_U233.get()));
        add.accept(new ItemStack(ModItems.WASTE_PLATE_U235.get()));
        add.accept(new ItemStack(ModItems.WASTE_PLATE_PU239.get()));
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
        add.accept(new ItemStack(ModMaterialItems.item(ModMaterials.PALEOGENITE, MaterialShape.POWDER)));
        add.accept(new ItemStack(ModItems.POWDER_THERMITE.get()));
        add.accept(new ItemStack(ModItems.POWDER_FERTILIZER.get()));
        add.accept(new ItemStack(ModItems.POWDER_FLUX.get()));
        add.accept(new ItemStack(ModItems.POWDER_MAGIC.get()));
        add.accept(new ItemStack(ModItems.POWDER_ICE.get()));
        add.accept(new ItemStack(ModItems.POWDER_SPARK_MIX.get()));
        add.accept(new ItemStack(ModItems.POWDER_SEMTEX_MIX.get()));
        add.accept(new ItemStack(ModItems.POWDER_DESH_READY.get()));
        add.accept(new ItemStack(ModItems.POWDER_COLTAN.get()));

        // RBMK-Teile (2026-08-09, aus Fuel-Tab verschoben, noch nicht vom Entwickler kontrolliert)
        add.accept(new ItemStack(ModItems.RBMK_FUEL_DRX.get()));
        add.accept(new ItemStack(ModBlocks.RBMK_ROD.get()));
        add.accept(new ItemStack(ModBlocks.RBMK_ROD_MOD.get()));
        add.accept(new ItemStack(ModBlocks.RBMK_ROD_REASIM.get()));
        add.accept(new ItemStack(ModBlocks.RBMK_ROD_REASIM_MOD.get()));
        add.accept(new ItemStack(ModBlocks.RBMK_CONTROL.get()));
        add.accept(new ItemStack(ModBlocks.RBMK_CONTROL_MOD.get()));
        add.accept(new ItemStack(ModBlocks.RBMK_CONTROL_AUTO.get()));
        add.accept(new ItemStack(ModBlocks.RBMK_CONTROL_REASIM.get()));
        add.accept(new ItemStack(ModBlocks.RBMK_CONTROL_REASIM_AUTO.get()));
        add.accept(new ItemStack(ModBlocks.RBMK_MODERATOR.get()));
        add.accept(new ItemStack(ModBlocks.RBMK_ABSORBER.get()));
        add.accept(new ItemStack(ModBlocks.RBMK_REFLECTOR.get()));
        add.accept(new ItemStack(ModBlocks.RBMK_COOLER.get()));
        add.accept(new ItemStack(ModBlocks.RBMK_BOILER.get()));
        add.accept(new ItemStack(ModBlocks.RBMK_HEATER.get()));
        add.accept(new ItemStack(ModBlocks.RBMK_OUTGASSER.get()));
        add.accept(new ItemStack(ModBlocks.RBMK_STORAGE.get()));
        add.accept(new ItemStack(ModBlocks.RBMK_BLANK.get()));
        add.accept(new ItemStack(ModBlocks.DECO_RBMK.get()));
        add.accept(new ItemStack(ModBlocks.DECO_RBMK_SMOOTH.get()));
        add.accept(new ItemStack(ModBlocks.DECO_RBMK_PANEL.get()));
        add.accept(new ItemStack(ModBlocks.DECO_RBMK_SMOOTH_PANEL.get()));
        add.accept(new ItemStack(ModBlocks.DECO_RBMK_PANEL_SLAB2.get()));
        add.accept(new ItemStack(ModBlocks.DECO_RBMK_SMOOTH_PANEL_SLAB2.get()));
        add.accept(new ItemStack(ModBlocks.BLOCK_GRAPHITE.get()));
        add.accept(new ItemStack(ModBlocks.STEEL_GRATE.get()));
        add.accept(new ItemStack(ModBlocks.RBMK_STEAM_INLET.get()));
        add.accept(new ItemStack(ModBlocks.RBMK_STEAM_OUTLET.get()));
        add.accept(new ItemStack(ModBlocks.RBMK_LOADER.get()));
        add.accept(new ItemStack(ModBlocks.RBMK_AUTOLOADER.get()));
        add.accept(new ItemStack(ModBlocks.RBMK_CRANE_CONSOLE.get()));
        add.accept(new ItemStack(ModBlocks.RBMK_DISPLAY.get()));
        add.accept(new ItemStack(ModBlocks.RBMK_GAUGE.get()));
        add.accept(new ItemStack(ModBlocks.RBMK_INDICATOR.get()));
        add.accept(new ItemStack(ModBlocks.RBMK_LEVER.get()));
        add.accept(new ItemStack(ModBlocks.RBMK_NUMITRON.get()));
        add.accept(new ItemStack(ModBlocks.RBMK_GRAPH.get()));
        add.accept(new ItemStack(ModBlocks.RBMK_TERMINAL.get()));
        add.accept(new ItemStack(ModBlocks.RBMK_KEYPAD.get()));
        add.accept(new ItemStack(ModBlocks.RBMK_DISPLAY_BLANK.get()));
        add.accept(new ItemStack(ModBlocks.RBMK_DEBRIS.get()));
        add.accept(new ItemStack(ModBlocks.RBMK_DEBRIS_BURNING.get()));
        add.accept(new ItemStack(ModBlocks.RBMK_DEBRIS_DIGAMMA.get()));
        add.accept(new ItemStack(ModBlocks.RBMK_DEBRIS_RADIATING.get()));
        add.accept(new ItemStack(ModItems.RBMK_LID.get()));
        add.accept(new ItemStack(ModItems.RBMK_LID_GLASS.get()));
        add.accept(new ItemStack(ModItems.RBMK_FUEL_EMPTY.get()));
        add.accept(new ItemStack(ModItems.RBMK_FUEL_TEST.get()));
        add.accept(new ItemStack(ModItems.RBMK_FUEL_HEU235.get()));
        add.accept(new ItemStack(ModItems.RBMK_FUEL_LEP.get()));
        add.accept(new ItemStack(ModItems.RBMK_FUEL_HEP.get()));
        add.accept(new ItemStack(ModItems.RBMK_FUEL_MOX.get()));
        add.accept(new ItemStack(ModItems.RBMK_PELLET_HEU235.get()));
        add.accept(new ItemStack(ModItems.RBMK_PELLET_LEP.get()));
        add.accept(new ItemStack(ModItems.RBMK_PELLET_HEP.get()));
        add.accept(new ItemStack(ModItems.RBMK_PELLET_MOX.get()));
    }

    // ТОПЛИВО И ЭЛЕМЕНТЫ МЕХАНИЗМОВ

        private static ItemStack createChargedArmorStack(Item item) {
        ItemStack stack = new ItemStack(item);

        // Проверяем, является ли предмет силовой броней
        if (item instanceof com.hbm_m.powerarmor.ModArmorFSBPowered powerArmor) {
            // Получаем максимальную емкость и устанавливаем полный заряд
            long maxCapacity = powerArmor.getMaxCharge(stack);
            PlatformHooks.putLong(stack, "charge", maxCapacity);
        }

        return stack;
    }
}