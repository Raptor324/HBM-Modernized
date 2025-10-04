package com.hbm_m.item;

import com.hbm_m.main.MainRegistry;
import com.hbm_m.block.ModBlocks;
import com.hbm_m.item.ModItems;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.ItemTagsProvider;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.common.data.ExistingFileHelper;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

public class ModItemTagGenerator extends ItemTagsProvider {
    public ModItemTagGenerator(PackOutput p_275343_, CompletableFuture<HolderLookup.Provider> p_275729_,
                               CompletableFuture<TagLookup<Block>> p_275322_, @Nullable ExistingFileHelper existingFileHelper) {
        super(p_275343_, p_275729_, p_275322_, MainRegistry.MOD_ID, existingFileHelper);
    }

    @Override
    protected void addTags(HolderLookup.Provider pProvider) {
        this.tag(ItemTags.TRIMMABLE_ARMOR)
                .add(ModItems.ALLOY_HELMET.get(),
                        ModItems.ALLOY_CHESTPLATE.get(),
                        ModItems.ALLOY_LEGGINGS.get(),
                        ModItems.TITANIUM_HELMET.get(),
                        ModItems.TITANIUM_CHESTPLATE.get(),
                        ModItems.TITANIUM_LEGGINGS.get(),
                        ModItems.TITANIUM_BOOTS.get(),
                        ModItems.STEEL_HELMET.get(),
                        ModItems.STEEL_CHESTPLATE.get(),
                        ModItems.STEEL_LEGGINGS.get(),
                        ModItems.STEEL_BOOTS.get(),
                        ModItems.HAZMAT_HELMET.get(),
                        ModItems.HAZMAT_CHESTPLATE.get(),
                        ModItems.HAZMAT_LEGGINGS.get(),
                        ModItems.HAZMAT_BOOTS.get(),
                        ModItems.SECURITY_HELMET.get(),
                        ModItems.SECURITY_CHESTPLATE.get(),
                        ModItems.SECURITY_LEGGINGS.get(),
                        ModItems.SECURITY_BOOTS.get(),
                        ModItems.AJR_HELMET.get(),
                        ModItems.AJR_CHESTPLATE.get(),
                        ModItems.AJR_LEGGINGS.get(),
                        ModItems.AJR_BOOTS.get(),
                        ModItems.STARMETAL_HELMET.get(),
                        ModItems.STARMETAL_CHESTPLATE.get(),
                        ModItems.STARMETAL_LEGGINGS.get(),
                        ModItems.STARMETAL_BOOTS.get(),
                        ModItems.ASBESTOS_HELMET.get(),
                        ModItems.ASBESTOS_CHESTPLATE.get(),
                        ModItems.ASBESTOS_LEGGINGS.get(),
                        ModItems.ASBESTOS_BOOTS.get(),
                        ModItems.COBALT_HELMET.get(),
                        ModItems.COBALT_CHESTPLATE.get(),
                        ModItems.COBALT_LEGGINGS.get(),
                        ModItems.COBALT_BOOTS.get(),
                        ModItems.ALLOY_BOOTS.get());

    }
}
