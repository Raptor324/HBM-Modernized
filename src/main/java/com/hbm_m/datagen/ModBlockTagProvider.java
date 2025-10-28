package com.hbm_m.datagen;
// Провайдер генерации тегов блоков для мода.
// Здесь мы определяем, какими инструментами можно добывать наши блоки и руды,
// а также создаем теги для совместимости с другими модами (например, для систем хранения).
// Используется в классе DataGenerators для регистрации.

import com.hbm_m.block.ModBlocks;
import com.hbm_m.lib.RefStrings;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.Tags;

import net.minecraftforge.common.data.BlockTagsProvider;
import net.minecraftforge.common.data.ExistingFileHelper;

import javax.annotation.Nonnull;
import java.util.concurrent.CompletableFuture;

public class ModBlockTagProvider extends BlockTagsProvider {
    public ModBlockTagProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider, ExistingFileHelper existingFileHelper) {
        super(output, lookupProvider, RefStrings.MODID, existingFileHelper);
    }

    @Override
    protected void addTags(@Nonnull HolderLookup.Provider provider) {
        // Здесь мы указываем, какими инструментами можно добывать наши блоки
        this.tag(BlockTags.MINEABLE_WITH_PICKAXE)
                .add(ModBlocks.URANIUM_ORE.get())
                .add(ModBlocks.URANIUM_BLOCK.get())
                .add(ModBlocks.POLONIUM210_BLOCK.get())
                .add(ModBlocks.PLUTONIUM_BLOCK.get())
                .add(ModBlocks.PLUTONIUM_FUEL_BLOCK.get())
                .add(ModBlocks.MACHINE_BATTERY.get())
                .add(ModBlocks.BLAST_FURNACE.get())
                .add(ModBlocks.PRESS.get())
                .add(ModBlocks.WOOD_BURNER.get())
                .add(ModBlocks.ALUMINUM_ORE.get())
                .add(ModBlocks.ALUMINUM_ORE_DEEPSLATE.get())
                .add(ModBlocks.URANIUM_ORE_H.get())
                .add(ModBlocks.URANIUM_ORE_DEEPSLATE.get())
                .add(ModBlocks.BERYLLIUM_ORE.get())
                .add(ModBlocks.BERYLLIUM_ORE_DEEPSLATE.get())
                .add(ModBlocks.CINNABAR_ORE.get())
                .add(ModBlocks.LIGNITE_ORE.get())
                .add(ModBlocks.ASBESTOS_ORE.get())
                .add(ModBlocks.LEAD_ORE.get())
                .add(ModBlocks.LEAD_ORE_DEEPSLATE.get())
                .add(ModBlocks.FLUORITE_ORE.get())
                .add(ModBlocks.RAREGROUND_ORE.get())
                .add(ModBlocks.RAREGROUND_ORE_DEEPSLATE.get())
                .add(ModBlocks.SULFUR_ORE.get())
                .add(ModBlocks.TITANIUM_ORE.get())
                .add(ModBlocks.TITANIUM_ORE_DEEPSLATE.get())
                .add(ModBlocks.THORIUM_ORE.get())
                .add(ModBlocks.THORIUM_ORE_DEEPSLATE.get())
                .add(ModBlocks.TUNGSTEN_ORE.get())
                .add(ModBlocks.COBALT_ORE.get());


        // Указываем минимальный уровень инструмента (1 = деревянный, 2 = каменный, 3 = железный и т.д.)
        // Например, для урановой руды нужен как минимум железный инструмент
        this.tag(BlockTags.NEEDS_IRON_TOOL)
                .add(ModBlocks.BERYLLIUM_ORE.get())
                .add(ModBlocks.BERYLLIUM_ORE_DEEPSLATE.get())
                .add(ModBlocks.URANIUM_ORE_H.get())
                .add(ModBlocks.URANIUM_ORE_DEEPSLATE.get())
                .add(ModBlocks.CINNABAR_ORE.get())
                .add(ModBlocks.LEAD_ORE.get())
                .add(ModBlocks.LEAD_ORE_DEEPSLATE.get())
                .add(ModBlocks.FLUORITE_ORE.get())
                .add(ModBlocks.RAREGROUND_ORE.get())
                .add(ModBlocks.RAREGROUND_ORE_DEEPSLATE.get())
                .add(ModBlocks.SULFUR_ORE.get())
                .add(ModBlocks.TITANIUM_ORE.get())
                .add(ModBlocks.TUNGSTEN_ORE.get())
                .add(ModBlocks.THORIUM_ORE.get())
                .add(ModBlocks.THORIUM_ORE_DEEPSLATE.get())
                .add(ModBlocks.COBALT_ORE.get())
                .add(ModBlocks.ASBESTOS_ORE.get());

        this.tag(BlockTags.NEEDS_STONE_TOOL)
                .add(ModBlocks.ALUMINUM_ORE_DEEPSLATE.get())
                .add(ModBlocks.LIGNITE_ORE.get())
                .add(ModBlocks.ALUMINUM_ORE.get());

        // ============  ТЕГ ДЛЯ OCCLUSION CULLING ============
        // Блоки, через которые можно видеть (не блокируют рендеринг машин)
        this.tag(BlockTags.create(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "non_occluding")))
            //  Части машин
            .add(ModBlocks.UNIVERSAL_MACHINE_PART.get())
            //  Стекло и прозрачные блоки
            .addTag(Tags.Blocks.GLASS)
            .addTag(Tags.Blocks.GLASS_PANES)

            //  Частичные блоки
            .addTag(BlockTags.FENCES)
            .addTag(BlockTags.FENCE_GATES)
            .addTag(BlockTags.WALLS)
            .addTag(BlockTags.DOORS)
            .addTag(BlockTags.TRAPDOORS)
            .addTag(BlockTags.BUTTONS)
            .addTag(BlockTags.PRESSURE_PLATES)
            .addTag(BlockTags.RAILS)
            .addTag(BlockTags.STAIRS)
            .addTag(BlockTags.SLABS)
            .addTag(BlockTags.CORAL_PLANTS)
            .addTag(BlockTags.LEAVES)
            .addTag(BlockTags.SAPLINGS)
            .addTag(BlockTags.FLOWERS)
            .addTag(BlockTags.SIGNS)
            .addTag(BlockTags.BANNERS)
            .addTag(BlockTags.CANDLES)
            .addTag(BlockTags.CLIMBABLE)
            //  Декоративные
            .add(Blocks.IRON_BARS)
            .add(Blocks.CHAIN)
            .add(Blocks.LANTERN)
            .add(Blocks.SOUL_LANTERN)
            .add(Blocks.TORCH)
            .add(Blocks.SOUL_TORCH)
            .add(Blocks.REDSTONE_TORCH)
            .add(Blocks.BREWING_STAND)
            .add(Blocks.ENCHANTING_TABLE)
            .add(Blocks.END_ROD)
            .add(Blocks.LIGHTNING_ROD)
            .add(Blocks.HOPPER)
            .add(Blocks.COBWEB)
            .add(Blocks.SCAFFOLDING)
            .add(Blocks.LEVER)
            .add(Blocks.TRIPWIRE)
            .add(Blocks.TRIPWIRE_HOOK)
            .add(Blocks.CAMPFIRE);

        // Создаем теги для совместимости с другими модами (например, для систем хранения)
        this.tag(BlockTags.create(ResourceLocation.fromNamespaceAndPath("forge", "storage_blocks/uranium")))
                .add(ModBlocks.URANIUM_BLOCK.get());
        this.tag(BlockTags.create(ResourceLocation.fromNamespaceAndPath("forge", "storage_blocks/plutonium")))
                .add(ModBlocks.PLUTONIUM_BLOCK.get());
        
        // Тег для руд
        this.tag(BlockTags.create(ResourceLocation.fromNamespaceAndPath("forge", "ores/uranium")))
                .add(ModBlocks.URANIUM_ORE.get());
    }
}