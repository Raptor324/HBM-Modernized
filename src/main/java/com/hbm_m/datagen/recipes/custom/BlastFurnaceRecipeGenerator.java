package com.hbm_m.datagen.recipes.custom;

import com.hbm_m.datagen.recipes.ModRecipeProvider;
import com.hbm_m.item.tags_and_tiers.ModIngots;
import com.hbm_m.item.ModItems;
import com.hbm_m.lib.RefStrings;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.common.Tags;

import java.util.function.Consumer;

/**
 * Handles Blast Furnace recipe generation to keep {@link ModRecipeProvider} focused on orchestration.
 */
public final class BlastFurnaceRecipeGenerator {

    private BlastFurnaceRecipeGenerator() {
    }

    public static void generate(Consumer<FinishedRecipe> writer) {
        BlastFurnaceRecipeBuilder.blastFurnaceRecipe(
                new ItemStack(ModItems.getIngot(ModIngots.STEEL).get()),
                Ingredient.of(Items.IRON_INGOT),
                Ingredient.of(ItemTags.COALS)
        ).save(writer, ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "blast_furnace/steel_from_ingot"));

        BlastFurnaceRecipeBuilder.blastFurnaceRecipe(
                new ItemStack(ModItems.getIngot(ModIngots.STEEL).get(), 2),
                Ingredient.of(Tags.Items.ORES_IRON),
                Ingredient.of(ItemTags.COALS)
        ).save(writer, ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "blast_furnace/steel_from_ore"));

        BlastFurnaceRecipeBuilder.blastFurnaceRecipe(
                new ItemStack(ModItems.getIngot(ModIngots.RED_COPPER).get(), 2),
                Ingredient.of(Items.COPPER_INGOT),
                Ingredient.of(Items.REDSTONE)
        ).save(writer, ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "blast_furnace/red_copper"));

        BlastFurnaceRecipeBuilder.blastFurnaceRecipe(
                new ItemStack(ModItems.getIngot(ModIngots.ADVANCED_ALLOY).get(), 2),
                Ingredient.of(ModItems.getIngot(ModIngots.RED_COPPER).get()),
                Ingredient.of(ModItems.getIngot(ModIngots.STEEL).get())
        ).save(writer, ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "blast_furnace/advanced_alloy"));
    }
}

