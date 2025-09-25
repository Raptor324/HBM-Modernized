package com.hbm_m.datagen;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.lib.RefStrings;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.tags.BlockTags;
import net.minecraft.resources.ResourceLocation;
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