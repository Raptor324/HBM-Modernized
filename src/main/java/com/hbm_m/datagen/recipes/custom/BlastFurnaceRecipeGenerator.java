package com.hbm_m.datagen.recipes.custom;
//? if forge {
import com.hbm_m.item.ModItems;
import com.hbm_m.item.tags_and_tiers.ModIngots;
import com.hbm_m.item.tags_and_tiers.ModPowders;

import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.common.Tags;

import java.util.function.Consumer;

/**
 * Handles Blast Furnace recipe generation to keep {@code ModRecipeProvider} focused on orchestration.
 * Recipes based on original HBM BlastFurnaceRecipes.
 *
 * <p>Использует {@code save(writer, "id")} из {@link BaseRecipeBuilder} — Stonecutter-блоки
 * с {@code ResourceLocation} больше не нужны.</p>
 */
public final class BlastFurnaceRecipeGenerator {

    private BlastFurnaceRecipeGenerator() {}

    public static void generate(Consumer<FinishedRecipe> writer) {
        // IRON + COAL -> steel x1
        BlastFurnaceRecipeBuilder.blastFurnaceRecipe(
                new ItemStack(ModItems.getIngot(ModIngots.STEEL).get()),
                Ingredient.of(Items.IRON_INGOT),
                Ingredient.of(ItemTags.COALS)
        ).save(writer, "blast_furnace/steel_from_ingot");

        // IRON.ore() + COAL -> steel x2
        BlastFurnaceRecipeBuilder.blastFurnaceRecipe(
                new ItemStack(ModItems.getIngot(ModIngots.STEEL).get(), 2),
                Ingredient.of(Tags.Items.ORES_IRON),
                Ingredient.of(ItemTags.COALS)
        ).save(writer, "blast_furnace/steel_from_ore");

        // IRON.ore() + COAL_BLOCK -> steel x3 (coal block burns hotter, like coke)
        BlastFurnaceRecipeBuilder.blastFurnaceRecipe(
                new ItemStack(ModItems.getIngot(ModIngots.STEEL).get(), 3),
                Ingredient.of(Tags.Items.ORES_IRON),
                Ingredient.of(Items.COAL_BLOCK)
        ).save(writer, "blast_furnace/steel_from_ore_coal_block");

        // IRON.ore() + coal powder -> steel x3 (flux-like)
        BlastFurnaceRecipeBuilder.blastFurnaceRecipe(
                new ItemStack(ModItems.getIngot(ModIngots.STEEL).get(), 3),
                Ingredient.of(Tags.Items.ORES_IRON),
                Ingredient.of(ModItems.getPowders(ModPowders.COAL).get())
        ).save(writer, "blast_furnace/steel_from_ore_powder");

        // CU + REDSTONE -> red_copper x2
        BlastFurnaceRecipeBuilder.blastFurnaceRecipe(
                new ItemStack(ModItems.getIngot(ModIngots.RED_COPPER).get(), 2),
                Ingredient.of(Items.COPPER_INGOT),
                Ingredient.of(Items.REDSTONE)
        ).save(writer, "blast_furnace/red_copper");

        // STEEL + RED_COPPER (MINGRADE analogue) -> advanced_alloy x2
        BlastFurnaceRecipeBuilder.blastFurnaceRecipe(
                new ItemStack(ModItems.getIngot(ModIngots.ADVANCED_ALLOY).get(), 2),
                Ingredient.of(ModItems.getIngot(ModIngots.STEEL).get()),
                Ingredient.of(ModItems.getIngot(ModIngots.RED_COPPER).get())
        ).save(writer, "blast_furnace/advanced_alloy");
    }
}
//?}
