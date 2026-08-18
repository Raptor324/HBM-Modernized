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


        // --- Weitere Rezepte aus dem Original BlastFurnaceRecipesNT.java (1.7.10) ---
        // Original hat zusaetzlich einen Schlacke-Nebenprodukt-Output (ingot_raw:MAT_SLAG) auf den
        // Erz-Rezepten - dieser Port unterstuetzt nur einen einzigen Output pro Blast-Furnace-Rezept,
        // das Nebenprodukt entfaellt daher (gleiche Vereinfachung wie an anderer Stelle diese Session).

        // CU (dust/dust) + REDSTONE dust -> red_copper x2 (blast.mingradeDust)
        BlastFurnaceRecipeBuilder.blastFurnaceRecipe(
                new ItemStack(ModItems.getIngot(ModIngots.RED_COPPER).get(), 2),
                Ingredient.of(ModItems.COPPER_POWDER.get()),
                Ingredient.of(Items.REDSTONE)
                ).save(writer, "blast_furnace/red_copper_dust");


        // GOLD + plate_mixed -> plate_paa (blast.paa)
        BlastFurnaceRecipeBuilder.blastFurnaceRecipe(
                new ItemStack(ModItems.PLATE_PAA.get()),
                Ingredient.of(Items.GOLD_INGOT),
                Ingredient.of(ModItems.PLATE_MIXED.get())
                ).save(writer, "blast_furnace/paa");


        // ALUMINUM powder + 7x clay_ball -> firebrick x8 (blast.firebrick)
        BlastFurnaceRecipeBuilder.blastFurnaceRecipe(
                new ItemStack(ModItems.FIREBRICK.get(), 8),
                Ingredient.of(ModItems.getPowder(ModIngots.ALUMINUM).get()),
                Ingredient.of(Items.CLAY_BALL)
                ).save(writer, "blast_furnace/firebrick");


        // LIMESTONE + clay_ball -> firebrick x8 (blast.firebrickLimestone)
        BlastFurnaceRecipeBuilder.blastFurnaceRecipe(
                new ItemStack(ModItems.FIREBRICK.get(), 8),
                Ingredient.of(ModItems.LIMESTONE.get()),
                Ingredient.of(Items.CLAY_BALL)
                ).save(writer, "blast_furnace/firebrick_limestone");

        // --- Previously flagged as "skipped" - all three actually portable, the required items just live
        // under this port's generic per-ingot naming (ModIngots.METEORITE/STARMETAL/SATURNITE + getPowder())
        // rather than individually declared ModItems fields, so an earlier pass missed them. ---

        // Cobalt + meteorite powder -> meteorite ingot (blast.meteor)
        BlastFurnaceRecipeBuilder.blastFurnaceRecipe(
                new ItemStack(ModItems.getIngot(ModIngots.METEORITE).get()),
                Ingredient.of(ModItems.getIngot(ModIngots.COBALT).get()),
                Ingredient.of(ModItems.getPowder(ModIngots.METEORITE).get())
                ).save(writer, "blast_furnace/meteorite_ingot");


        // Saturnite (BIGMT) + meteorite ingot -> starmetal ingot x2 (blast.starmetal)
        BlastFurnaceRecipeBuilder.blastFurnaceRecipe(
                new ItemStack(ModItems.getIngot(ModIngots.STARMETAL).get(), 2),
                Ingredient.of(ModItems.getIngot(ModIngots.SATURNITE).get()),
                Ingredient.of(ModItems.getIngot(ModIngots.METEORITE).get())
                ).save(writer, "blast_furnace/starmetal_ingot");


        // Meteorite Sword (Hardened) + Cobalt -> Meteorite Sword (Alloyed) (blast.meteorSword)
        // Note: meteorite_sword_hardened itself has no crafting path yet in this port (original obtains it via
        // a Press-stamped "reforged" precursor, which isn't ported) - added here so the Blast Furnace link in
        // the chain isn't silently dropped, but the item is currently only reachable via creative/JEI.
        BlastFurnaceRecipeBuilder.blastFurnaceRecipe(
                new ItemStack(ModItems.METEORITE_SWORD_ALLOYED.get()),
                Ingredient.of(ModItems.METEORITE_SWORD_HARDENED.get()),
                Ingredient.of(ModItems.getIngot(ModIngots.COBALT).get())
                ).save(writer, "blast_furnace/meteorite_sword_alloyed");
    }
}
//?}
