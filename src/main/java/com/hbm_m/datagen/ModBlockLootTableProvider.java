package com.hbm_m.datagen;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.item.tags_and_tiers.ModIngots;
import com.hbm_m.item.ModItems;
import net.minecraft.advancements.critereon.EnchantmentPredicate;
import net.minecraft.advancements.critereon.ItemPredicate;
import net.minecraft.advancements.critereon.MinMaxBounds;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.AlternativesEntry;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.ApplyBonusCount;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;
import net.minecraft.world.level.storage.loot.predicates.MatchTool;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;
import net.minecraftforge.registries.RegistryObject;

import java.util.Set;

public class ModBlockLootTableProvider extends BlockLootSubProvider {

    protected ModBlockLootTableProvider() {
        super(Set.of(), FeatureFlags.REGISTRY.allFlags()); // ← Set.of(), а не Collections.emptySet()!
    }
    @Override
    protected void generate() {
        // 1) Автоматический dropSelf для ВСЕХ блоков
        for (RegistryObject<Block> entry : ModBlocks.BLOCKS.getEntries()) {
            this.dropSelf(entry.get());
        }

        // 2) ✅ ПЕРЕОПРЕДЕЛЯЕМ для ящиков - ПУСТЫЕ таблицы!
        dropEmptyTable(ModBlocks.CRATE_IRON.get());
        dropEmptyTable(ModBlocks.CRATE_STEEL.get());
        dropEmptyTable(ModBlocks.CRATE_DESH.get());

        // 2) ОСОБЫЕ СЛУЧАИ: руды переопределяют свою таблицу

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


        // Если DEPTH_STONE должен вести себя как обычный блок,
        // отдельный вызов dropSelfType не нужен — его уже обработал цикл выше.
        // Если нужна особая логика — добавь здесь нужный метод.
    }

    private void dropEmptyTable(Block block) {
        LootTable.Builder emptyTable = LootTable.lootTable()
                .withPool(LootPool.lootPool()
                        .setRolls(ConstantValue.exactly(1))
                        .when(LootItemRandomChanceCondition.randomChance(0.0f))); // 0% шанс!

        this.add(block, emptyTable);
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
        return ModBlocks.BLOCKS.getEntries()
                .stream()
                .flatMap(RegistryObject::stream)
                .toList();
    }



}
