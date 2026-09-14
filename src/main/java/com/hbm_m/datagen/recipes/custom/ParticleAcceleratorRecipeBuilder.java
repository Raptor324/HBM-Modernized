package com.hbm_m.datagen.recipes.custom;
//? if forge {
import com.google.gson.JsonObject;
import com.hbm_m.recipe.ParticleAcceleratorRecipe;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;

/** Datagen-Builder fuer {@link ParticleAcceleratorRecipe} ({@code hbm_m:particle_accelerator}). */
public class ParticleAcceleratorRecipeBuilder extends BaseRecipeBuilder<ParticleAcceleratorRecipeBuilder> {

    private final Ingredient inputA;
    private final Ingredient inputB;
    private final int momentum;
    private final ItemStack outputA;
    private ItemStack outputB;

    private ParticleAcceleratorRecipeBuilder(Ingredient inputA, Ingredient inputB, int momentum, ItemStack outputA) {
        this.inputA = inputA;
        this.inputB = inputB;
        this.momentum = momentum;
        this.outputA = outputA;
    }

    public static ParticleAcceleratorRecipeBuilder paRecipe(Ingredient inputA, Ingredient inputB,
                                                            int momentum, ItemStack outputA) {
        return new ParticleAcceleratorRecipeBuilder(inputA, inputB, momentum, outputA);
    }

    /** Zweites Produkt - im Original haben nur zwei Rezepte eines. */
    public ParticleAcceleratorRecipeBuilder secondResult(ItemStack outputB) {
        this.outputB = outputB;
        return this;
    }

    @Override
    public net.minecraft.world.item.Item getResult() {
        return outputA != null && !outputA.isEmpty() ? outputA.getItem() : Items.AIR;
    }

    @Override
    protected void serializeRecipeData(JsonObject json) {
        json.add("input_a", this.inputA.toJson());
        json.add("input_b", this.inputB.toJson());
        json.addProperty("momentum", this.momentum);
        json.add("result", stackToJson(this.outputA));
        if (this.outputB != null && !this.outputB.isEmpty()) {
            json.add("result_b", stackToJson(this.outputB));
        }
    }

    @Override
    protected RecipeSerializer<?> getType() {
        return ParticleAcceleratorRecipe.Serializer.INSTANCE;
    }
}
//?}
