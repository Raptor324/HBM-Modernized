package com.hbm_m.datagen;
//? if forge {
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.item.ModItems;
import com.hbm_m.item.tags_and_tiers.ModIngots;

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
                ModItems.CRYSTAL_SCHRABIDIUM.get()
        );
        dropOreType1(
                ModBlocks.SCHRABIDIUM_ORE_NETHER.get(),
                ModBlocks.SCHRABIDIUM_ORE_NETHER.get(),
                ModItems.CRYSTAL_SCHRABIDIUM.get()
        );
        dropOreType1(
                ModBlocks.SCHRABIDIUM_ORE_GNEISS.get(),
                ModBlocks.SCHRABIDIUM_ORE_GNEISS.get(),
                ModItems.CRYSTAL_SCHRABIDIUM.get()
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
                ModItems.getIngot(ModIngots.ASBESTOS).get(),
                1.0f, 3.0f
        );
        dropOreType2(
                ModBlocks.RESOURCE_ASBESTOS.get(),
                ModBlocks.RESOURCE_ASBESTOS.get(),
                ModItems.getIngot(ModIngots.ASBESTOS).get(),
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
                ModItems.getIngot(ModIngots.ASBESTOS).get()
        );
        // ore_niter → niter ×2-4 (порт: crystal_niter)
        dropOreType2(
                ModBlocks.NITER_ORE.get(),
                ModBlocks.NITER_ORE.get(),
                ModItems.CRYSTAL_NITER.get(),
                2.0f, 4.0f
        );
        dropOreType2(
                ModBlocks.NITER_ORE_DEEPSLATE.get(),
                ModBlocks.NITER_ORE_DEEPSLATE.get(),
                ModItems.CRYSTAL_NITER.get(),
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
