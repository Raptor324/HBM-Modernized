package com.hbm_m.datagen;

import com.hbm_m.item.ModIngots;
import com.hbm_m.item.ModItems;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.function.Consumer;

public class ModRecipeProvider extends RecipeProvider {

    public ModRecipeProvider(PackOutput pOutput) {
        super(pOutput);
    }

    @Override
    protected void buildRecipes(Consumer<FinishedRecipe> pWriter) {
        
        // --- РЕЦЕПТЫ В НОВОМ ФОРМАТЕ ---

        // Рецепт для алмазного меча
        AssemblerRecipeBuilder.assemblerRecipe(
                new ItemStack(Items.DIAMOND_SWORD), 100, 1000)
                .addIngredient(Items.DIAMOND, 2) // <-- Добавляем 2 алмаза
                .addIngredient(Items.STICK, 1)    // <-- Добавляем 1 палку
                .save(pWriter, "diamond_sword_from_assembler");
        
        
        AssemblerRecipeBuilder.assemblerRecipe(
                new ItemStack(ModItems.PLATE_IRON.get(), 2), 30, 500)
                .addIngredient(Items.IRON_INGOT, 3) // <-- Добавляем 3 железных слитка
                .save(pWriter, "plate_iron_from_ingots");

        AssemblerRecipeBuilder.assemblerRecipe(
                new ItemStack(ModItems.PLATE_GOLD.get(), 2), 30, 500)
                .addIngredient(Items.GOLD_INGOT, 3)
                .save(pWriter, "plate_gold_from_ingots");

        AssemblerRecipeBuilder.assemblerRecipe(
                new ItemStack(ModItems.PLATE_STEEL.get(), 2), 30, 500)
                .addIngredient(ModItems.getIngot(ModIngots.STEEL).get(), 3)
                .save(pWriter, "plate_steel_from_ingots");

        AssemblerRecipeBuilder.assemblerRecipe(
                new ItemStack(ModItems.PLATE_SATURNITE.get(), 2), 30, 500)
                .addIngredient(ModItems.getIngot(ModIngots.SATURNITE).get(), 3)
                .save(pWriter, "plate_saturnite_from_ingots");

        AssemblerRecipeBuilder.assemblerRecipe(
                new ItemStack(ModItems.PLATE_SCHRABIDIUM.get(), 2), 30, 500)
                .addIngredient(ModItems.getIngot(ModIngots.SCHRABIDIUM).get(), 3)
                .save(pWriter, "plate_schrabidium_from_ingots");

        AssemblerRecipeBuilder.assemblerRecipe(
                new ItemStack(ModItems.PLATE_GUNMETAL.get(), 2), 30, 500)
                .addIngredient(ModItems.getIngot(ModIngots.GUNMETAL).get(), 3)
                .save(pWriter, "plate_gunmetal_from_ingots");

        AssemblerRecipeBuilder.assemblerRecipe(
                new ItemStack(ModItems.PLATE_GUNSTEEL.get(), 2), 30, 500)
                .addIngredient(ModItems.getIngot(ModIngots.GUNSTEEL).get(), 3)
                .save(pWriter, "plate_gunsteel_from_ingots");
    
        AssemblerRecipeBuilder.assemblerRecipe(
                new ItemStack(ModItems.PLATE_LEAD.get(), 2), 30, 500)
                .addIngredient(ModItems.getIngot(ModIngots.LEAD).get(), 3)
                .save(pWriter, "plate_lead_from_ingots");

    }
}