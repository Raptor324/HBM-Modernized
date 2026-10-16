package com.hbm_m.datagen.recipes.custom;
//? if forge {
import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.hbm_m.recipe.AssemblerRecipe;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;

public class AssemblerRecipeBuilder extends BaseRecipeBuilder<AssemblerRecipeBuilder> {

    private final ItemStack output;
    private final int duration;
    private final int power;
    private final List<CountableIngredient> ingredients = new ArrayList<>();

    @Nullable
    private String blueprintPool = null;

    private AssemblerRecipeBuilder(ItemStack output, int duration, int power) {
        this.output = output;
        this.duration = duration;
        this.power = power;
    }

    public static AssemblerRecipeBuilder assemblerRecipe(ItemStack output, int duration, int power) {
        return new AssemblerRecipeBuilder(output, duration, power);
    }

    public AssemblerRecipeBuilder addIngredient(Ingredient ingredient, int count) {
        this.ingredients.add(new CountableIngredient(ingredient, count));
        return this;
    }

    public AssemblerRecipeBuilder addIngredient(Item item, int count) {
        return addIngredient(Ingredient.of(item), count);
    }

    public AssemblerRecipeBuilder withBlueprintPool(String pool) {
        this.blueprintPool = pool;
        return this;
    }

    private record CountableIngredient(Ingredient ingredient, int count) {}

    @Override
    public Item getResult() {
        return this.output.getItem();
    }

    @Override
    protected void serializeRecipeData(JsonObject json) {
        JsonArray jsonIngredients = new JsonArray();

        for (CountableIngredient countableIng : this.ingredients) {
            jsonIngredients.add(AssemblerRecipe.toCountedIngredientJson(
                    countableIng.ingredient(), countableIng.count()));
        }
        json.add("ingredients", jsonIngredients);

        JsonObject jsonOutput = new JsonObject();
        jsonOutput.addProperty("item", BuiltInRegistries.ITEM.getKey(this.output.getItem()).toString());
        if (this.output.getCount() > 1) {
            jsonOutput.addProperty("count", this.output.getCount());
        }
        json.add("output", jsonOutput);

        json.addProperty("duration", this.duration);
        json.addProperty("power", this.power);

        if (this.blueprintPool != null) {
            json.addProperty("blueprint_pool", this.blueprintPool);
        }
    }

    @Override
    protected RecipeSerializer<?> getType() {
        return AssemblerRecipe.Serializer.INSTANCE;
    }
}
//?}