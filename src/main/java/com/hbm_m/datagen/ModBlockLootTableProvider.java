package com.hbm_m.datagen;
//? if forge {
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.item.ModItems;
import com.hbm_m.item.material.MaterialShape;
import com.hbm_m.item.material.ModMaterialItems;
import com.hbm_m.item.material.ModMaterials;

import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.advancements.critereon.EnchantmentPredicate;
import net.minecraft.advancements.critereon.ItemPredicate;
import net.minecraft.advancements.critereon.MinMaxBounds;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.AlternativesEntry;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.ApplyBonusCount;
import net.minecraft.world.level.storage.loot.functions.CopyNbtFunction;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;
import net.minecraft.world.level.storage.loot.predicates.MatchTool;
import net.minecraft.world.level.storage.loot.providers.nbt.ContextNbtProvider;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;

public class ModBlockLootTableProvider extends BlockLootSubProvider {

    protected ModBlockLootTableProvider() {
        super(Set.of(), FeatureFlags.REGISTRY.allFlags()); // ← Set.of(), а не Collections.emptySet()!
    }
    @Override
    protected void generate() {
        // 1) Базовые лут-таблицы для всех блоков:
        // - если есть обычный BlockItem -> dropSelf
        // - если BlockItem нет (registerBlockWithoutItem) -> пробуем дропнуть item с тем же id
        for (RegistrySupplier<Block> entry : ModBlocks.BLOCKS) {
            Block block = entry.get();
            // noLootTable() → minecraft:empty; datagen must not register loot for those blocks
            if (block.getLootTable() == BuiltInLootTables.EMPTY) {
                continue;
            }
            if (block.asItem() != Items.AIR) {
                this.dropSelf(block);
                continue;
            }

            Item mappedItem = BuiltInRegistries.ITEM.get(entry.getId());
            if (mappedItem != null && mappedItem != Items.AIR) {
                dropMappedItem(block, mappedItem);
            } else {
                // Блок без Item (или без соответствующего id в ITEMS) - явно пустая таблица,
                // чтобы пройти строгую валидацию datagen.
                dropEmptyTable(block);
            }
        }

        // 1.0.0c) R6b: Lampen geben die eingeschaltete (Scheinwerfer) bzw. ausgeschaltete (Tritium) Form
        this.dropOther(ModBlocks.CAGE_LAMP_OFF.get(), ModBlocks.CAGE_LAMP.get());
        // R6e: BlockNTMFlower.damageDropped - die gewachsene Cadmiumweide gibt die junge
        this.dropOther(ModBlocks.PLANT_FLOWER_CD1.get(), ModBlocks.PLANT_FLOWER_CD0.get());
        this.dropOther(ModBlocks.FLOOD_LAMP_OFF.get(), ModBlocks.FLOOD_LAMP.get());
        this.dropOther(ModBlocks.FLUORESCENT_LAMP_OFF.get(), ModBlocks.FLUORESCENT_LAMP.get());
        this.dropOther(ModBlocks.LAMP_TRITIUM_GREEN_ON.get(), ModBlocks.LAMP_TRITIUM_GREEN_OFF.get());
        this.dropOther(ModBlocks.LAMP_TRITIUM_BLUE_ON.get(), ModBlocks.LAMP_TRITIUM_BLUE_OFF.get());

        // 1.0.0b) R6a: Drops 1:1 (BlockOre/BlockCluster/BlockDepthOre/WasteEarth/WasteLog/BlockCap/Glas ...)
        // R6d: BlockPorous - Behutsamkeit liefert glatten Stein (createStackedBlock), sonst Bruchstein
        this.add(ModBlocks.STONE_POROUS.get(), b -> LootTable.lootTable().withPool(net.minecraft.world.level.storage.loot.LootPool.lootPool()
                .setRolls(net.minecraft.world.level.storage.loot.providers.number.ConstantValue.exactly(1.0F))
                .add(net.minecraft.world.level.storage.loot.entries.AlternativesEntry.alternatives(
                        net.minecraft.world.level.storage.loot.entries.LootItem.lootTableItem(net.minecraft.world.level.block.Blocks.STONE).when(HAS_SILK_TOUCH),
                        this.applyExplosionCondition(b, net.minecraft.world.level.storage.loot.entries.LootItem.lootTableItem(net.minecraft.world.level.block.Blocks.COBBLESTONE))))));
        this.add(ModBlocks.ORE_OIL.get(), b -> r6aDrop(b, net.minecraft.core.registries.BuiltInRegistries.ITEM.get(net.minecraft.resources.ResourceLocation.parse("hbm_m:oil_tar_crude")), 1, 1, "nosilk_fortune"));
        this.add(ModBlocks.BLOCK_METEOR_COBBLE.get(), b -> r6aDrop(b, net.minecraft.core.registries.BuiltInRegistries.ITEM.get(net.minecraft.resources.ResourceLocation.parse("hbm_m:fragment_meteorite")), 1, 1, "nofortune"));
        this.add(ModBlocks.BLOCK_METEOR_BROKEN.get(), b -> r6aDrop(b, net.minecraft.core.registries.BuiltInRegistries.ITEM.get(net.minecraft.resources.ResourceLocation.parse("hbm_m:fragment_meteorite")), 1, 3, "nofortune"));
        this.add(ModBlocks.WASTE_PLANKS.get(), b -> r6aDrop(b, net.minecraft.core.registries.BuiltInRegistries.ITEM.get(net.minecraft.resources.ResourceLocation.parse("minecraft:charcoal")), 1, 1, "fortune"));
        this.add(ModBlocks.FROZEN_DIRT.get(), b -> r6aDrop(b, net.minecraft.core.registries.BuiltInRegistries.ITEM.get(net.minecraft.resources.ResourceLocation.parse("minecraft:snowball")), 1, 1, "fortune"));
        this.add(ModBlocks.FROZEN_PLANKS.get(), b -> r6aDrop(b, net.minecraft.core.registries.BuiltInRegistries.ITEM.get(net.minecraft.resources.ResourceLocation.parse("minecraft:snowball")), 1, 1, "fortune"));
        this.add(ModBlocks.ORE_RARE.get(), b -> r6aDrop(b, net.minecraft.core.registries.BuiltInRegistries.ITEM.get(net.minecraft.resources.ResourceLocation.parse("hbm_m:rareground_ore_chunk")), 1, 1, "fortune"));
        this.add(ModBlocks.CLUSTER_IRON.get(), b -> r6aDrop(b, net.minecraft.core.registries.BuiltInRegistries.ITEM.get(net.minecraft.resources.ResourceLocation.parse("hbm_m:crystal_iron")), 1, 1, "plain"));
        this.add(ModBlocks.CLUSTER_TITANIUM.get(), b -> r6aDrop(b, net.minecraft.core.registries.BuiltInRegistries.ITEM.get(net.minecraft.resources.ResourceLocation.parse("hbm_m:crystal_titanium")), 1, 1, "plain"));
        this.add(ModBlocks.CLUSTER_ALUMINIUM.get(), b -> r6aDrop(b, net.minecraft.core.registries.BuiltInRegistries.ITEM.get(net.minecraft.resources.ResourceLocation.parse("hbm_m:crystal_aluminium")), 1, 1, "plain"));
        this.add(ModBlocks.CLUSTER_COPPER.get(), b -> r6aDrop(b, net.minecraft.core.registries.BuiltInRegistries.ITEM.get(net.minecraft.resources.ResourceLocation.parse("hbm_m:crystal_copper")), 1, 1, "plain"));
        this.add(ModBlocks.CLUSTER_DEPTH_IRON.get(), b -> r6aDrop(b, net.minecraft.core.registries.BuiltInRegistries.ITEM.get(net.minecraft.resources.ResourceLocation.parse("hbm_m:crystal_iron")), 1, 1, "depthfortune"));
        this.add(ModBlocks.CLUSTER_DEPTH_TITANIUM.get(), b -> r6aDrop(b, net.minecraft.core.registries.BuiltInRegistries.ITEM.get(net.minecraft.resources.ResourceLocation.parse("hbm_m:crystal_titanium")), 1, 1, "depthfortune"));
        this.add(ModBlocks.CLUSTER_DEPTH_TUNGSTEN.get(), b -> r6aDrop(b, net.minecraft.core.registries.BuiltInRegistries.ITEM.get(net.minecraft.resources.ResourceLocation.parse("hbm_m:crystal_tungsten")), 1, 1, "depthfortune"));
        this.add(ModBlocks.WASTE_LOG.get(), b -> r6aWasteLog(b));
        this.add(ModBlocks.FROZEN_LOG.get(), b -> r6aDrop(b, net.minecraft.core.registries.BuiltInRegistries.ITEM.get(net.minecraft.resources.ResourceLocation.parse("minecraft:snowball")), 2, 4, "plain"));
        this.add(ModBlocks.WASTE_EARTH.get(), b -> r6aDrop(b, net.minecraft.core.registries.BuiltInRegistries.ITEM.get(net.minecraft.resources.ResourceLocation.parse("minecraft:dirt")), 1, 1, "plain"));
        this.add(ModBlocks.FROZEN_GRASS.get(), b -> r6aDrop(b, net.minecraft.core.registries.BuiltInRegistries.ITEM.get(net.minecraft.resources.ResourceLocation.parse("minecraft:snowball")), 1, 1, "plain"));
        this.add(ModBlocks.BURNING_EARTH.get(), b -> r6aDrop(b, net.minecraft.core.registries.BuiltInRegistries.ITEM.get(net.minecraft.resources.ResourceLocation.parse("minecraft:dirt")), 1, 1, "plain"));
        this.add(ModBlocks.IMPACT_DIRT.get(), b -> r6aDrop(b, net.minecraft.core.registries.BuiltInRegistries.ITEM.get(net.minecraft.resources.ResourceLocation.parse("minecraft:dirt")), 1, 1, "plain"));
        this.add(ModBlocks.NTM_DIRT.get(), b -> r6aDrop(b, net.minecraft.core.registries.BuiltInRegistries.ITEM.get(net.minecraft.resources.ResourceLocation.parse("minecraft:dirt")), 1, 1, "plain"));
        this.dropOther(ModBlocks.REINFORCED_LAMP_ON.get(), ModBlocks.REINFORCED_LAMP_OFF.get());
        this.add(ModBlocks.BARRICADE.get(), noDrop());
        this.add(ModBlocks.FLUID_DUCT_BOX.get(), b -> LootTable.lootTable().withPool(net.minecraft.world.level.storage.loot.LootPool.lootPool()
                .setRolls(net.minecraft.world.level.storage.loot.providers.number.ConstantValue.exactly(1))
                .add(net.minecraft.world.level.storage.loot.entries.LootItem.lootTableItem(b)
                        .apply(net.minecraft.world.level.storage.loot.functions.CopyBlockState.copyState(b).copy(com.hbm_m.block.network.BoxDuctBlock.META)))
                .when(net.minecraft.world.level.storage.loot.predicates.ExplosionCondition.survivesExplosion())));
        this.add(ModBlocks.FLUID_DUCT_EXHAUST.get(), b -> LootTable.lootTable().withPool(net.minecraft.world.level.storage.loot.LootPool.lootPool()
                .setRolls(net.minecraft.world.level.storage.loot.providers.number.ConstantValue.exactly(1))
                .add(net.minecraft.world.level.storage.loot.entries.LootItem.lootTableItem(b)
                        .apply(net.minecraft.world.level.storage.loot.functions.CopyBlockState.copyState(b).copy(com.hbm_m.block.network.BoxDuctBlock.META)))
                .when(net.minecraft.world.level.storage.loot.predicates.ExplosionCondition.survivesExplosion())));
        this.add(ModBlocks.RED_CABLE_BOX.get(), b -> LootTable.lootTable().withPool(net.minecraft.world.level.storage.loot.LootPool.lootPool()
                .setRolls(net.minecraft.world.level.storage.loot.providers.number.ConstantValue.exactly(1))
                .add(net.minecraft.world.level.storage.loot.entries.LootItem.lootTableItem(b)
                        .apply(net.minecraft.world.level.storage.loot.functions.CopyBlockState.copyState(b).copy(com.hbm_m.block.network.BoxDuctBlock.META)))
                .when(net.minecraft.world.level.storage.loot.predicates.ExplosionCondition.survivesExplosion())));
        this.add(ModBlocks.GEYSIR_NETHER.get(), noDrop());
        this.add(ModBlocks.GEYSIR_CHLORINE.get(), noDrop());
        this.add(ModBlocks.BRICK_JUNGLE_FRAGILE.get(), noDrop());
        this.add(ModBlocks.BRICK_JUNGLE_GLYPH.get(), b -> LootTable.lootTable().withPool(net.minecraft.world.level.storage.loot.LootPool.lootPool()
                .setRolls(net.minecraft.world.level.storage.loot.providers.number.ConstantValue.exactly(1))
                .add(net.minecraft.world.level.storage.loot.entries.LootItem.lootTableItem(b)
                        .apply(net.minecraft.world.level.storage.loot.functions.CopyBlockState.copyState(b).copy(com.hbm_m.block.generic.JungleBricks.Glyph.GLYPH)))
                .when(net.minecraft.world.level.storage.loot.predicates.ExplosionCondition.survivesExplosion())));
        this.add(ModBlocks.VENT_CHLORINE.get(), noDrop());
        this.add(ModBlocks.VENT_CLOUD.get(), noDrop());
        this.add(ModBlocks.VENT_PINK_CLOUD.get(), noDrop());
        this.add(ModBlocks.PLANT_DEAD.get(), noDrop());
        this.add(ModBlocks.PLANT_DEAD_GRASS.get(), noDrop());
        this.add(ModBlocks.PLANT_DEAD_FLOWER.get(), noDrop());
        this.add(ModBlocks.PLANT_DEAD_BIGFLOWER.get(), noDrop());
        this.add(ModBlocks.PLANT_DEAD_FERN.get(), noDrop());
        this.dropWhenSilkTouch(ModBlocks.GLASS_BORON.get());
        this.dropWhenSilkTouch(ModBlocks.GLASS_LEAD.get());
        this.dropWhenSilkTouch(ModBlocks.GLASS_URANIUM.get());
        this.dropWhenSilkTouch(ModBlocks.GLASS_TRINITITE.get());
        this.dropWhenSilkTouch(ModBlocks.GLASS_POLONIUM.get());
        this.dropWhenSilkTouch(ModBlocks.GLASS_ASH.get());
        this.dropWhenSilkTouch(ModBlocks.GLASS_POLARIZED.get());
        this.add(ModBlocks.BLOCK_CAP_NUKA.get(), b -> r6aDrop(b, net.minecraft.core.registries.BuiltInRegistries.ITEM.get(net.minecraft.resources.ResourceLocation.parse("hbm_m:cap_nuka")), 128, 128, "plain"));
        this.add(ModBlocks.BLOCK_CAP_QUANTUM.get(), b -> r6aDrop(b, net.minecraft.core.registries.BuiltInRegistries.ITEM.get(net.minecraft.resources.ResourceLocation.parse("hbm_m:cap_quantum")), 128, 128, "plain"));
        this.add(ModBlocks.BLOCK_CAP_SPARKLE.get(), b -> r6aDrop(b, net.minecraft.core.registries.BuiltInRegistries.ITEM.get(net.minecraft.resources.ResourceLocation.parse("hbm_m:cap_sparkle")), 128, 128, "plain"));
        this.add(ModBlocks.BLOCK_CAP_RAD.get(), b -> r6aDrop(b, net.minecraft.core.registries.BuiltInRegistries.ITEM.get(net.minecraft.resources.ResourceLocation.parse("hbm_m:cap_rad")), 128, 128, "plain"));
        this.add(ModBlocks.BLOCK_CAP_KORL.get(), b -> r6aDrop(b, net.minecraft.core.registries.BuiltInRegistries.ITEM.get(net.minecraft.resources.ResourceLocation.parse("hbm_m:cap_korl")), 128, 128, "plain"));
        this.add(ModBlocks.BLOCK_CAP_FRITZ.get(), b -> r6aDrop(b, net.minecraft.core.registries.BuiltInRegistries.ITEM.get(net.minecraft.resources.ResourceLocation.parse("hbm_m:cap_fritz")), 128, 128, "plain"));
        this.add(ModBlocks.STONES_SLAB_TILE.get(), b -> createSlabItemTable(b));
        this.add(ModBlocks.STONES_SLAB_BRICKS.get(), b -> createSlabItemTable(b));
        this.add(ModBlocks.FOAM_LAYER.get(), noDrop());
        this.add(ModBlocks.SAND_BORON_LAYER.get(), noDrop());
        this.add(ModBlocks.LEAVES_LAYER.get(), noDrop());

        // 1.0.0a) Foerderbaender (Original getItemDropped = conveyor_wand, damageDropped = Typ)
        this.dropOther(ModBlocks.CONVEYOR.get(), com.hbm_m.item.ModItems.CONVEYOR_WAND_REGULAR.get());
        this.dropOther(ModBlocks.CONVEYOR_LIFT.get(), com.hbm_m.item.ModItems.CONVEYOR_WAND_REGULAR.get());
        this.dropOther(ModBlocks.CONVEYOR_CHUTE.get(), com.hbm_m.item.ModItems.CONVEYOR_WAND_REGULAR.get());
        this.dropOther(ModBlocks.CONVEYOR_EXPRESS.get(), com.hbm_m.item.ModItems.CONVEYOR_WAND_EXPRESS.get());
        this.dropOther(ModBlocks.CONVEYOR_DOUBLE.get(), com.hbm_m.item.ModItems.CONVEYOR_WAND_DOUBLE.get());
        this.dropOther(ModBlocks.CONVEYOR_TRIPLE.get(), com.hbm_m.item.ModItems.CONVEYOR_WAND_TRIPLE.get());

        // 1.0.0) Landminen (Original Landmine.getItemDropped = null): nichts.
        for (var sup : java.util.List.of(ModBlocks.MINE_AP, ModBlocks.MINE_HE, ModBlocks.MINE_SHRAP, ModBlocks.MINE_FAT, ModBlocks.MINE_NAVAL, ModBlocks.NAVAL_MINE)) {
            dropEmptyTable(sup.get());
        }

        // 1.0.1) Basalterze (BlockOreBasalt.getItemDropped): Behutsamkeit gibt den Block, sonst genau ein Stueck.
        for (var sup : java.util.List.of(ModBlocks.ORE_BASALT_SULFUR, ModBlocks.ORE_BASALT_FLUORITE, ModBlocks.ORE_BASALT_ASBESTOS,
                ModBlocks.ORE_BASALT_GEM, ModBlocks.ORE_BASALT_MOLYSITE)) {
            var ore = (com.hbm_m.block.generic.BlockOreBasalt) sup.get();
            this.add(ore, createSilkTouchDispatchTable(ore, net.minecraft.world.level.storage.loot.entries.LootItem.lootTableItem(ore.type.drop.get())));
        }

        // 1.0.2) Meteorbloecke (BlockOre, noFortune, Behutsamkeit gibt immer den Block).
        this.add(ModBlocks.BLOCK_METEOR_COBBLE.get(), b -> createSilkTouchDispatchTable(b,
                LootItem.lootTableItem(ModItems.FRAGMENT_METEORITE.get())));
        this.add(ModBlocks.BLOCK_METEOR_BROKEN.get(), b -> createSilkTouchDispatchTable(b,
                LootItem.lootTableItem(ModItems.FRAGMENT_METEORITE.get()).apply(SetItemCountFunction.setCount(net.minecraft.world.level.storage.loot.providers.number.UniformGenerator.between(1, 3)))));
        this.add(ModBlocks.BLOCK_METEOR_MOLTEN.get(), b -> createSilkTouchOnlyTable(b));
        this.add(ModBlocks.BLOCK_METEOR_TREASURE.get(), b -> meteoriteTreasure(b));

        // 1.1) Батареи должны сохранять заряд/режимы в BlockEntityTag при дропе.
        dropMachineBatteryWithNbt(ModBlocks.MACHINE_BATTERY.get());
        dropMachineBatteryWithNbt(ModBlocks.MACHINE_BATTERY_LITHIUM.get());
        dropMachineBatteryWithNbt(ModBlocks.MACHINE_BATTERY_SCHRABIDIUM.get());
        dropMachineBatteryWithNbt(ModBlocks.MACHINE_BATTERY_DINEUTRONIUM.get());

        // 1.2) Breaking a stacked RBMK panel slab returns both singles it was made of.
        dropDoubleSlab(ModBlocks.DECO_RBMK_PANEL_SLAB4.get(), ModBlocks.DECO_RBMK_PANEL_SLAB2.get());
        dropDoubleSlab(ModBlocks.DECO_RBMK_SMOOTH_PANEL_SLAB4.get(), ModBlocks.DECO_RBMK_SMOOTH_PANEL_SLAB2.get());

        // 2)  ПЕРЕОПРЕДЕЛЯЕМ для ящиков - ПУСТЫЕ таблицы!
        dropEmptyTable(ModBlocks.CRATE_IRON.get());
        dropEmptyTable(ModBlocks.CRATE_STEEL.get());
        dropEmptyTable(ModBlocks.CRATE_DESH.get());
        dropEmptyTable(ModBlocks.CRATE_TUNGSTEN.get());
        dropEmptyTable(ModBlocks.SAFE.get());
        dropEmptyTable(ModBlocks.CRATE_TEMPLATE.get());
        // Фантомные части мультиблока не должны дропаться отдельно.
        dropEmptyTable(ModBlocks.UNIVERSAL_MACHINE_PART.get());

        // 2) ОСОБЫЕ СЛУЧАИ: руды переопределяют свою таблицу

        // deco_loot (груда лута из структур 1.7.10): сам блок ничего не дропает —
        // предметы хранятся в DecoLootBlockEntity и выпадают при ломании/ПКМ
        // (пул хлама перенесён в DecoLootBlockEntity.POOL)
        dropEmptyTable(ModBlocks.DECO_LOOT.get());

        // Тип 1: silk touch -> блок, иначе сырьё с Fortune + explosion decay
        dropOreType1(
                ModBlocks.ALUMINUM_ORE.get(),
                ModBlocks.ALUMINUM_ORE.get(),
                ModItems.ALUMINUM_RAW.get()
        );
        dropOreType1(
                ModBlocks.ALUMINUM_ORE_DEEPSLATE.get(),
                ModBlocks.ALUMINUM_ORE_DEEPSLATE.get(),
                ModItems.ALUMINUM_RAW.get()
        );
        dropOreType1(
                ModBlocks.URANIUM_ORE.get(),
                ModBlocks.URANIUM_ORE.get(),
                ModItems.URANIUM_RAW.get()
        );
        dropOreType1(
                ModBlocks.URANIUM_ORE_DEEPSLATE.get(),
                ModBlocks.URANIUM_ORE_DEEPSLATE.get(),
                ModItems.URANIUM_RAW.get()
        );
        dropOreType1(
                ModBlocks.SCHRABIDIUM_ORE.get(),
                ModBlocks.SCHRABIDIUM_ORE.get(),
                ModMaterialItems.item(ModMaterials.SCHRABIDIUM, MaterialShape.CRYSTAL)
        );
        dropOreType1(
                ModBlocks.SCHRABIDIUM_ORE_NETHER.get(),
                ModBlocks.SCHRABIDIUM_ORE_NETHER.get(),
                ModMaterialItems.item(ModMaterials.SCHRABIDIUM, MaterialShape.CRYSTAL)
        );
        dropOreType1(
                ModBlocks.SCHRABIDIUM_ORE_GNEISS.get(),
                ModBlocks.SCHRABIDIUM_ORE_GNEISS.get(),
                ModMaterialItems.item(ModMaterials.SCHRABIDIUM, MaterialShape.CRYSTAL)
        );
        dropOreType1(
                ModBlocks.COBALT_ORE.get(),
                ModBlocks.COBALT_ORE.get(),
                ModItems.COBALT_RAW.get()
        );
        dropOreType1(
                ModBlocks.COBALT_ORE_DEEPSLATE.get(),
                ModBlocks.COBALT_ORE_DEEPSLATE.get(),
                ModItems.COBALT_RAW.get()
        );
        dropOreType1(
                ModBlocks.TUNGSTEN_ORE.get(),
                ModBlocks.TUNGSTEN_ORE.get(),
                ModItems.TUNGSTEN_RAW.get()
        );
        dropOreType1(
                ModBlocks.TITANIUM_ORE.get(),
                ModBlocks.TITANIUM_ORE.get(),
                ModItems.TITANIUM_RAW.get()
        );
        dropOreType1(
                ModBlocks.TITANIUM_ORE_DEEPSLATE.get(),
                ModBlocks.TITANIUM_ORE_DEEPSLATE.get(),
                ModItems.TITANIUM_RAW.get()
        );
        dropOreType1(
                ModBlocks.THORIUM_ORE.get(),
                ModBlocks.THORIUM_ORE.get(),
                ModItems.THORIUM_RAW.get()
        );
        dropOreType1(
                ModBlocks.THORIUM_ORE_DEEPSLATE.get(),
                ModBlocks.THORIUM_ORE_DEEPSLATE.get(),
                ModItems.THORIUM_RAW.get()
        );
        dropOreType1(
                ModBlocks.BERYLLIUM_ORE.get(),
                ModBlocks.BERYLLIUM_ORE.get(),
                ModItems.BERYLLIUM_RAW.get()
        );
        dropOreType1(
                ModBlocks.BERYLLIUM_ORE_DEEPSLATE.get(),
                ModBlocks.BERYLLIUM_ORE_DEEPSLATE.get(),
                ModItems.BERYLLIUM_RAW.get()
        );
        dropOreType1(
                ModBlocks.LEAD_ORE.get(),
                ModBlocks.LEAD_ORE.get(),
                ModItems.LEAD_RAW.get()
        );
        dropOreType1(
                ModBlocks.LEAD_ORE_DEEPSLATE.get(),
                ModBlocks.LEAD_ORE_DEEPSLATE.get(),
                ModItems.LEAD_RAW.get()
        );




        // Тип 2: silk touch -> блок, иначе сырьё с random count + Fortune + explosion decay

        dropOreType2(
                ModBlocks.WASTE_LOG.get(),
                ModBlocks.WASTE_LOG.get(),
                Items.CHARCOAL,
                1.0f, 3.0f
        );


        dropOreType2(
                ModBlocks.DEPTH_CINNABAR.get(),
                ModBlocks.DEPTH_CINNABAR.get(),
                ModItems.CINNABAR.get(),
                3.0f, 5.0f
        );

        dropOreType2(
                ModBlocks.DEPTH_BORAX.get(),
                ModBlocks.DEPTH_BORAX.get(),
                ModItems.BORAX.get(),
                3.0f, 5.0f
        );

        dropOreType2(
                ModBlocks.DEPTH_TITANIUM.get(),
                ModBlocks.DEPTH_TITANIUM.get(),
                ModItems.TITANIUM_RAW.get(),
                3.0f, 5.0f
        );
        dropOreType2(
                ModBlocks.DEPTH_TUNGSTEN.get(),
                ModBlocks.DEPTH_TUNGSTEN.get(),
                ModItems.TUNGSTEN_RAW.get(),
                3.0f, 5.0f
        );
        dropOreType2(
                ModBlocks.DEPTH_ZIRCONIUM.get(),
                ModBlocks.DEPTH_ZIRCONIUM.get(),
                ModItems.ZIRCONIUM_SHARP.get(),
                3.0f, 5.0f
        );
        dropOreType2(
                ModBlocks.FLUORITE_ORE.get(),
                ModBlocks.FLUORITE_ORE.get(),
                ModItems.FLUORITE.get(),
                1.0f, 3.0f
        );
        dropOreType2(
                ModBlocks.SULFUR_ORE.get(),
                ModBlocks.SULFUR_ORE.get(),
                ModItems.SULFUR.get(),
                1.0f, 3.0f
        );
        dropOreType2(
                ModBlocks.LIGNITE_ORE.get(),
                ModBlocks.LIGNITE_ORE.get(),
                ModItems.LIGNITE.get(),
                1.0f, 3.0f
        );
        dropOreType2(
                ModBlocks.RAREGROUND_ORE.get(),
                ModBlocks.RAREGROUND_ORE.get(),
                ModItems.RAREGROUND_ORE_CHUNK.get(),
                1.0f, 3.0f
        );
        dropOreType2(
                ModBlocks.RAREGROUND_ORE_DEEPSLATE.get(),
                ModBlocks.RAREGROUND_ORE_DEEPSLATE.get(),
                ModItems.RAREGROUND_ORE_CHUNK.get(),
                1.0f, 3.0f
        );
        dropOreType2(
                ModBlocks.STRAWBERRY_BUSH.get(),
                ModBlocks.STRAWBERRY_BUSH.get(),
                ModItems.STRAWBERRY.get(),
                1.0f, 3.0f
        );
        dropOreType2(
                ModBlocks.CINNABAR_ORE.get(),
                ModBlocks.CINNABAR_ORE.get(),
                ModItems.CINNABAR.get(),
                1.0f, 3.0f
        );
        dropOreType2(
                ModBlocks.CINNABAR_ORE_DEEPSLATE.get(),
                ModBlocks.CINNABAR_ORE_DEEPSLATE.get(),
                ModItems.CINNABAR.get(),
                1.0f, 3.0f
        );
        dropOreType2(
                ModBlocks.DEPTH_IRON.get(),
                ModBlocks.DEPTH_IRON.get(),
                Items.RAW_IRON,
                1.0f, 3.0f
        );
        dropOreType2(
                ModBlocks.ASBESTOS_ORE.get(),
                ModBlocks.ASBESTOS_ORE.get(),
                ModMaterialItems.item(ModMaterials.ASBESTOS, MaterialShape.INGOT),
                1.0f, 3.0f
        );
        dropOreType2(
                ModBlocks.RESOURCE_ASBESTOS.get(),
                ModBlocks.RESOURCE_ASBESTOS.get(),
                ModMaterialItems.item(ModMaterials.ASBESTOS, MaterialShape.INGOT),
                2.0f, 5.0f
        );
        dropOreType2(
                ModBlocks.RESOURCE_SULFUR.get(),
                ModBlocks.RESOURCE_SULFUR.get(),
                ModItems.SULFUR.get(),
                2.0f, 5.0f
        );
        dropOreType2(
                ModBlocks.RESOURCE_MALACHITE.get(),
                ModBlocks.RESOURCE_MALACHITE.get(),
                ModItems.MALACHITE_CHUNK.get(),
                1.0f, 3.0f
        );
        dropOreType2(
                ModBlocks.RESOURCE_LIMESTONE.get(),
                ModBlocks.RESOURCE_LIMESTONE.get(),
                ModItems.LIMESTONE.get(),
                1.0f, 3.0f
        );
        dropOreType2(
                ModBlocks.SEQUESTRUM_ORE.get(),
                ModBlocks.SEQUESTRUM_ORE.get(),
                ModItems.SEQUESTRUM.get(),
                1.0f, 3.0f
        );

        // Пропущенные руды (сверялись с 1.7.10 BlockOre/BlockDepthOre/BlockDragonProof):
        // гнейсовые железо/медь/золото/уран/литий/газ и незерские уголь/уран/плутоний/
        // вольфрам/тлеющая руда, а также tikite и australium в оригинале дропают сами
        // себя — оставляем dropSelf (базовый проход).

        // ore_nether_sulfur → sulfur ×2-4
        dropOreType2(
                ModBlocks.NETHER_SULFUR_ORE.get(),
                ModBlocks.NETHER_SULFUR_ORE.get(),
                ModItems.SULFUR.get(),
                2.0f, 4.0f
        );
        // ore_alexandrite → gem_alexandrite ×1
        dropOreType1(
                ModBlocks.ALEXANDRITE_ORE.get(),
                ModBlocks.ALEXANDRITE_ORE.get(),
                ModItems.GEM_ALEXANDRITE.get()
        );
        // ore_coltan → fragment_coltan ×1
        dropOreType1(
                ModBlocks.COLTAN_ORE.get(),
                ModBlocks.COLTAN_ORE.get(),
                ModItems.FRAGMENT_COLTAN.get()
        );
        dropOreType1(
                ModBlocks.COLTAN_ORE_DEEPSLATE.get(),
                ModBlocks.COLTAN_ORE_DEEPSLATE.get(),
                ModItems.FRAGMENT_COLTAN.get()
        );
        // ore_nether_cobalt → fragment_cobalt ×5-12
        dropOreType2(
                ModBlocks.NETHER_COBALT_ORE.get(),
                ModBlocks.NETHER_COBALT_ORE.get(),
                ModItems.FRAGMENT_COBALT.get(),
                5.0f, 12.0f
        );
        // ore_nether_fire → powder_fire (в оригинале ещё 10% ingot_phosphorus —
        // предмета ingot_phosphorus в порту нет)
        dropOreType1(
                ModBlocks.NETHER_FIRE_ORE.get(),
                ModBlocks.NETHER_FIRE_ORE.get(),
                ModItems.FIRE_POWDER.get()
        );
        // ore_gneiss_rare → chunk_ore (порт: rareground_ore_chunk) ×1
        dropOreType1(
                ModBlocks.GNEISS_RARE_ORE.get(),
                ModBlocks.GNEISS_RARE_ORE.get(),
                ModItems.RAREGROUND_ORE_CHUNK.get()
        );
        // ore_gneiss_asbestos → ingot_asbestos ×1
        dropOreType1(
                ModBlocks.GNEISS_ASBESTOS_ORE.get(),
                ModBlocks.GNEISS_ASBESTOS_ORE.get(),
                ModMaterialItems.item(ModMaterials.ASBESTOS, MaterialShape.INGOT)
        );
        // ore_niter → niter ×2-4 (порт: crystal_niter)
        dropOreType2(
                ModBlocks.NITER_ORE.get(),
                ModBlocks.NITER_ORE.get(),
                ModMaterialItems.item(ModMaterials.NITER, MaterialShape.CRYSTAL),
                2.0f, 4.0f
        );
        dropOreType2(
                ModBlocks.NITER_ORE_DEEPSLATE.get(),
                ModBlocks.NITER_ORE_DEEPSLATE.get(),
                ModMaterialItems.item(ModMaterials.NITER, MaterialShape.CRYSTAL),
                2.0f, 4.0f
        );
    }

    private void dropEmptyTable(Block block) {
        if (block.getLootTable() == BuiltInLootTables.EMPTY) {
            return;
        }
        LootTable.Builder emptyTable = LootTable.lootTable()
                .withPool(LootPool.lootPool()
                        .setRolls(ConstantValue.exactly(1))
                        .when(LootItemRandomChanceCondition.randomChance(0.0f))); // 0% шанс!

        this.add(block, emptyTable);
    }

    private void dropMappedItem(Block block, Item item) {
        LootTable.Builder tableBuilder = LootTable.lootTable()
                .withPool(
                        LootPool.lootPool()
                                .setRolls(ConstantValue.exactly(1.0f))
                                .setBonusRolls(ConstantValue.exactly(0.0f))
                                .add(this.applyExplosionDecay(block, LootItem.lootTableItem(item)))
                );
        this.add(block, tableBuilder);
    }

    private void dropMachineBatteryWithNbt(Block block) {
        LootTable.Builder tableBuilder = LootTable.lootTable()
                .withPool(
                        LootPool.lootPool()
                                .setRolls(ConstantValue.exactly(1.0f))
                                .setBonusRolls(ConstantValue.exactly(0.0f))
                                .add(this.applyExplosionDecay(
                                        block,
                                        LootItem.lootTableItem(block.asItem())
                                                .apply(
                                                        CopyNbtFunction.copyData(ContextNbtProvider.BLOCK_ENTITY)
                                                                .copy("Energy", "BlockEntityTag.Energy", CopyNbtFunction.MergeStrategy.REPLACE)
                                                                .copy("lastEnergy", "BlockEntityTag.lastEnergy", CopyNbtFunction.MergeStrategy.REPLACE)
                                                                .copy("energyDelta", "BlockEntityTag.energyDelta", CopyNbtFunction.MergeStrategy.REPLACE)
                                                                .copy("modeOnNoSignal", "BlockEntityTag.modeOnNoSignal", CopyNbtFunction.MergeStrategy.REPLACE)
                                                                .copy("modeOnSignal", "BlockEntityTag.modeOnSignal", CopyNbtFunction.MergeStrategy.REPLACE)
                                                                .copy("priority", "BlockEntityTag.priority", CopyNbtFunction.MergeStrategy.REPLACE)
                                                                .copy("Inventory", "BlockEntityTag.Inventory", CopyNbtFunction.MergeStrategy.REPLACE)
                                                )
                                ))
                );
        this.add(block, tableBuilder);
    }
    /**
     * Руда тип 1:
     * - При Silk Touch дропает блок руды.
     * - Иначе дропает сырьё с учетом Fortune и Explosion decay.
     */
    private void dropOreType1(Block block, Block silkTouchDrop, net.minecraft.world.item.Item normalDrop) {
        LootTable.Builder tableBuilder = LootTable.lootTable()
                .withPool(
                        LootPool.lootPool()
                                .setRolls(ConstantValue.exactly(1.0f))
                                .setBonusRolls(ConstantValue.exactly(0.0f))
                                .add(
                                        AlternativesEntry.alternatives(
                                                // Ветка с Silk Touch
                                                LootItem.lootTableItem(silkTouchDrop)
                                                        .when(MatchTool.toolMatches(
                                                                ItemPredicate.Builder.item()
                                                                        .hasEnchantment(new EnchantmentPredicate(
                                                                                Enchantments.SILK_TOUCH,
                                                                                MinMaxBounds.Ints.atLeast(1)
                                                                        ))
                                                        )),
                                                // Ветка без Silk Touch: Fortune + explosion decay
                                                this.applyExplosionDecay(
                                                        block,
                                                        LootItem.lootTableItem(normalDrop)
                                                                .apply(ApplyBonusCount.addOreBonusCount(
                                                                        Enchantments.BLOCK_FORTUNE))
                                                )
                                        )
                                )
                );

        this.add(block, tableBuilder);
    }

    /**
     * Руда тип 2:
     * - При Silk Touch дропает блок руды.collections
     * - Иначе дропает сырьё с set_count (от min до max), Fortune и Explosion decay.
     */
    /** BlockMeteoriteTreasure.getDrops: 1-3 Ziehungen aus ItemPoolsSingle.POOL_METEORITE_TREASURE. */
    private LootTable.Builder meteoriteTreasure(Block block) {
        LootPool.Builder pool = LootPool.lootPool()
                .setRolls(net.minecraft.world.level.storage.loot.providers.number.UniformGenerator.between(1, 3))
                .when(HAS_NO_SILK_TOUCH);
        weighted(pool, ModItems.COBALT_PICKAXE.get(), 1, 1, 10);
        weighted(pool, ModMaterialItems.item(ModMaterials.ZIRCONIUM, MaterialShape.INGOT), 1, 16, 10);
        weighted(pool, ModMaterialItems.item(ModMaterials.NIOBIUM, MaterialShape.INGOT), 1, 16, 10);
        weighted(pool, ModMaterialItems.item(ModMaterials.COBALT, MaterialShape.INGOT), 1, 16, 10);
        weighted(pool, ModMaterialItems.item(ModMaterials.BORON, MaterialShape.INGOT), 1, 16, 10);
        weighted(pool, ModMaterialItems.item(ModMaterials.STARMETAL, MaterialShape.INGOT), 1, 1, 5);
        weighted(pool, ModMaterialItems.item(ModMaterials.GOLD, MaterialShape.CRYSTAL), 1, 4, 10);
        weighted(pool, ModItems.VACUUM_TUBE.get(), 4, 8, 10);
        weighted(pool, ModItems.MICROCHIP.get(), 2, 4, 10);
        weighted(pool, ModItems.DEFINITELYFOOD.get(), 16, 32, 25);
        weighted(pool, ModBlocks.CRATE_CAN.get().asItem(), 1, 3, 10);
        weighted(pool, ModItems.PILL_HERBAL.get(), 1, 2, 10);
        weighted(pool, ModItems.SERUM.get(), 1, 1, 5);
        weighted(pool, ModItems.HEART_PIECE.get(), 1, 1, 5);
        weighted(pool, ModItems.SCRUMPY.get(), 1, 1, 5);
        weighted(pool, ModItems.LAUNCH_CODE_PIECE.get(), 1, 1, 5);
        weighted(pool, ModItems.EGG_GLYPHID.get(), 1, 1, 5);
        weighted(pool, ModItems.GEM_ALEXANDRITE.get(), 1, 1, 1);
        weighted(pool, ModItems.BLUEPRINT_FOLDER_DISCOVER.get(), 1, 1, 1);
        return LootTable.lootTable()
                .withPool(LootPool.lootPool().setRolls(ConstantValue.exactly(1.0F)).when(HAS_SILK_TOUCH).add(LootItem.lootTableItem(block)))
                .withPool(pool);
    }

    private static void weighted(LootPool.Builder pool, net.minecraft.world.level.ItemLike item, int min, int max, int weight) {
        var entry = LootItem.lootTableItem(item).setWeight(weight);
        if (min != 1 || max != 1)
            entry.apply(SetItemCountFunction.setCount(net.minecraft.world.level.storage.loot.providers.number.UniformGenerator.between(min, max)));
        pool.add(entry);
    }

    private void dropOreType2(Block block, Block silkTouchDrop,
                              net.minecraft.world.item.Item normalDrop,
                              float minCount, float maxCount) {
        LootTable.Builder tableBuilder = LootTable.lootTable()
                .withPool(
                        LootPool.lootPool()
                                .setRolls(ConstantValue.exactly(1.0f))
                                .setBonusRolls(ConstantValue.exactly(0.0f))
                                .add(
                                        AlternativesEntry.alternatives(
                                                // Ветка с Silk Touch
                                                LootItem.lootTableItem(silkTouchDrop)
                                                        .when(MatchTool.toolMatches(
                                                                ItemPredicate.Builder.item()
                                                                        .hasEnchantment(new EnchantmentPredicate(
                                                                                Enchantments.SILK_TOUCH,
                                                                                MinMaxBounds.Ints.atLeast(1)
                                                                        ))
                                                        )),
                                                // Ветка без Silk Touch: random count + Fortune + explosion decay
                                                this.applyExplosionDecay(
                                                        block,
                                                        LootItem.lootTableItem(normalDrop)
                                                                .apply(SetItemCountFunction.setCount(
                                                                        UniformGenerator.between(minCount, maxCount)))
                                                                .apply(ApplyBonusCount.addOreBonusCount(
                                                                        Enchantments.BLOCK_FORTUNE))
                                                )
                                        )
                                )
                );

        this.add(block, tableBuilder);
    }

    /**
     * R6a: Original getItemDropped/quantityDropped. mode: plain = feste Menge lo..hi; fortune = BlockOre-Glueck
     * (Menge * (max(rand(fortune+2)-1,0)+1)); nofortune = ohne Glueck; nosilk_fortune = wie fortune, Behutsamkeit ohne Wirkung;
     * depthfortune = BlockDepthOre (immer Glueck). Mit Behutsamkeit (ausser nosilk_fortune) faellt der Block selbst.
     */
    private LootTable.Builder r6aDrop(Block block, net.minecraft.world.item.Item drop, int lo, int hi, String mode) {
        var entry = net.minecraft.world.level.storage.loot.entries.LootItem.lootTableItem(drop)
                .apply(net.minecraft.world.level.storage.loot.functions.SetItemCountFunction.setCount(
                        lo == hi ? net.minecraft.world.level.storage.loot.providers.number.ConstantValue.exactly(lo)
                                 : net.minecraft.world.level.storage.loot.providers.number.UniformGenerator.between(lo, hi)));
        if (mode.equals("fortune") || mode.equals("nosilk_fortune") || mode.equals("depthfortune")) {
            entry.apply(net.minecraft.world.level.storage.loot.functions.ApplyBonusCount.addUniformBonusCount(net.minecraft.world.item.enchantment.Enchantments.BLOCK_FORTUNE));
        }
        if (mode.equals("nosilk_fortune") || mode.equals("depthfortune")) {
            return LootTable.lootTable().withPool(this.applyExplosionDecay(block, net.minecraft.world.level.storage.loot.LootPool.lootPool()
                    .setRolls(net.minecraft.world.level.storage.loot.providers.number.ConstantValue.exactly(1.0F)).add(entry)));
        }
        return createSilkTouchDispatchTable(block, this.applyExplosionDecay(block, entry));
    }

    /** R6a WasteLog: 1/1000 verbrannte Rinde, sonst 2-4 Holzkohle (Meta 1). */
    private LootTable.Builder r6aWasteLog(Block block) {
        var pool = net.minecraft.world.level.storage.loot.LootPool.lootPool().setRolls(net.minecraft.world.level.storage.loot.providers.number.ConstantValue.exactly(1.0F))
                .add(net.minecraft.world.level.storage.loot.entries.LootItem.lootTableItem(net.minecraft.core.registries.BuiltInRegistries.ITEM.get(net.minecraft.resources.ResourceLocation.parse("hbm_m:burnt_bark"))).setWeight(1))
                .add(net.minecraft.world.level.storage.loot.entries.LootItem.lootTableItem(net.minecraft.world.item.Items.CHARCOAL).setWeight(999)
                        .apply(net.minecraft.world.level.storage.loot.functions.SetItemCountFunction.setCount(net.minecraft.world.level.storage.loot.providers.number.UniformGenerator.between(2, 4))));
        return LootTable.lootTable().withPool(pool);
    }

    @Override
    protected Iterable<Block> getKnownBlocks() {
        // Генерируем для всех зарегистрированных блоков мода
        List<Block> list = new ArrayList<>();
        for (RegistrySupplier<Block> entry : ModBlocks.BLOCKS) {
            list.add(entry.get());
        }
        return list;
    }

    /** CE's {@code quantityDropped}: a double slab yields two of its single form. */
    private void dropDoubleSlab(Block doubleSlab, Block singleSlab) {
        this.add(doubleSlab, LootTable.lootTable()
                .withPool(this.applyExplosionDecay(doubleSlab,
                        net.minecraft.world.level.storage.loot.LootPool.lootPool()
                                .setRolls(net.minecraft.world.level.storage.loot.providers.number.ConstantValue.exactly(1.0F))
                                .add(net.minecraft.world.level.storage.loot.entries.LootItem.lootTableItem(singleSlab)
                                        .apply(net.minecraft.world.level.storage.loot.functions.SetItemCountFunction
                                                .setCount(net.minecraft.world.level.storage.loot.providers.number.ConstantValue.exactly(2.0F)))))));
    }

}
//?}
