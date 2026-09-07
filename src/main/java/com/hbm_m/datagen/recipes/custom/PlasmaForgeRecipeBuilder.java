package com.hbm_m.datagen.recipes.custom;
//? if forge {
import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.hbm_m.recipe.AssemblerRecipe;
import com.hbm_m.recipe.PlasmaForgeRecipe;

import dev.architectury.fluid.FluidStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.material.Fluid;

/**
 * Datagen-Builder fuer {@link PlasmaForgeRecipe} ({@code hbm_m:plasma_forge}).
 * Entspricht dem 1.7.10-Aufruf
 * {@code new PlasmaForgeRecipe(name).setInputEnergy(..).setup(duration, power).outputItems(..).inputItems(..)}.
 */
public class PlasmaForgeRecipeBuilder extends BaseRecipeBuilder<PlasmaForgeRecipeBuilder> {

    private record CountedIngredient(Ingredient ingredient, int count) {}
    private record FluidAmount(Fluid fluid, int amount) {}

    private final ItemStack output;
    private final int duration;
    private final long power;
    private long ignitionTemp;

    private final List<CountedIngredient> itemInputs = new ArrayList<>();
    private final List<FluidAmount> fluidInputs = new ArrayList<>();

    @Nullable
    private String blueprintPool;

    private PlasmaForgeRecipeBuilder(ItemStack output, int duration, long power) {
        this.output = output;
        this.duration = duration;
        this.power = power;
    }

    public static PlasmaForgeRecipeBuilder plasmaForgeRecipe(ItemStack output, int duration, long power) {
        return new PlasmaForgeRecipeBuilder(output, duration, power);
    }

    /** Original: {@code setInputEnergy} - minimale Plasmaleistung des Torus. */
    public PlasmaForgeRecipeBuilder inputEnergy(long ignitionTemp) {
        this.ignitionTemp = ignitionTemp;
        return this;
    }

    public PlasmaForgeRecipeBuilder addIngredient(Ingredient ingredient, int count) {
        this.itemInputs.add(new CountedIngredient(ingredient, count));
        return this;
    }

    public PlasmaForgeRecipeBuilder addIngredient(Item item, int count) {
        return addIngredient(Ingredient.of(item), count);
    }

    public PlasmaForgeRecipeBuilder addFluidInput(Fluid fluid, int amount) {
        this.fluidInputs.add(new FluidAmount(fluid, amount));
        return this;
    }

    public PlasmaForgeRecipeBuilder blueprintPool(String pool) {
        this.blueprintPool = pool;
        return this;
    }

    @Override
    public Item getResult() {
        return this.output.getItem();
    }

    @Override
    protected void serializeRecipeData(JsonObject json) {
        json.addProperty("duration", duration);
        json.addProperty("power", power);
        json.addProperty("ignition_temp", ignitionTemp);
        if (blueprintPool != null) json.addProperty("blueprint_pool", blueprintPool);

        JsonArray itemInputsJson = new JsonArray();
        for (CountedIngredient ci : itemInputs) {
            itemInputsJson.add(AssemblerRecipe.toCountedIngredientJson(ci.ingredient(), ci.count()));
        }
        json.add("item_inputs", itemInputsJson);

        JsonArray fluidInputsJson = new JsonArray();
        for (FluidAmount fa : fluidInputs) {
            if (fa.fluid() == null) continue;
            fluidInputsJson.add(fluidStackToJson(FluidStack.create(fa.fluid(), fa.amount())));
        }
        json.add("fluid_inputs", fluidInputsJson);

        json.add("result", stackToJson(output));
    }

    @Override
    protected RecipeSerializer<?> getType() {
        return PlasmaForgeRecipe.Serializer.INSTANCE;
    }
}
//?}
