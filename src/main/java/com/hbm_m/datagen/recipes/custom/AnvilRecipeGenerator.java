package com.hbm_m.datagen.recipes.custom;
//? if forge {
import java.util.function.Consumer;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.item.ModItems;
import com.hbm_m.item.material.MaterialShape;
import com.hbm_m.item.material.ModMaterialItems;
import com.hbm_m.item.material.ModMaterials;
import com.hbm_m.recipe.AnvilRecipe.OverlayType;

import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

/**
 * Amboss: die Original-Rezepte stehen 1:1 in {@link AnvilConstructionRecipeGenerator} (aus {@code AnvilRecipes}
 * uebersetzt). Hier bleiben nur Port-Notbehelfe fuer Bloecke, deren Original-Weg (noch) fehlt oder die es nur im
 * Port gibt - sonst waeren sie im Survival nicht herstellbar. Sortiert hinter allen Original-Rezepten.
 */
public final class AnvilRecipeGenerator {
    private AnvilRecipeGenerator() { }

    public static void generate(Consumer<FinishedRecipe> writer) {
        AnvilConstructionRecipeGenerator.generate(writer);
        registerStopgaps(writer);
    }

    private static void registerStopgaps(Consumer<FinishedRecipe> writer) {
        int sort = 10_000;

        // Port-eigener alter Montageautomat (das Original kennt nur machine_assembly_machine)
        AnvilRecipeBuilder.construction(2, sort++, OverlayType.CONSTRUCTION)
                .input(Ingredient.of(ModMaterialItems.item(ModMaterials.STEEL, MaterialShape.INGOT)), 8)
                .input(Ingredient.of(ModMaterialItems.item(ModMaterials.COPPER, MaterialShape.PLATE)), 4)
                .input(Ingredient.of(ModItems.MOTOR.get()), 2)
                .input(Ingredient.of(ModItems.VACUUM_TUBE.get()), 4)
                .output(new ItemStack(ModBlocks.MACHINE_ASSEMBLER.get()))
                .save(writer, "anvil/port/machine_assembler");

        // Port-eigener Hochdruckbrenner: Brenner, verstaerkt um gegossene Stahlplatten
        AnvilRecipeBuilder.construction(2, sort++, OverlayType.CONSTRUCTION)
                .input(Ingredient.of(ModBlocks.OILBURNER.get()), 1)
                .input(Ingredient.of(ModMaterialItems.item(ModMaterials.STEEL, MaterialShape.PLATE_CAST)), 8)
                .input(Ingredient.of(ModItems.PIPE_STEEL.get()), 4)
                .output(new ItemStack(ModBlocks.OILBURNER_HP.get()))
                .save(writer, "anvil/port/oilburner_hp");

        // furnace_brick / electric_furnace: Werkbankrezepte des Originals jetzt in OrigCraftingRecipeGenerator
    }
}
//?}
