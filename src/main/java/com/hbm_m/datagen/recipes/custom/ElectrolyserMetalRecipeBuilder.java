package com.hbm_m.datagen.recipes.custom;
//? if forge {
import javax.annotation.Nullable;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.hbm_m.recipe.ElectrolyserMetalRecipe;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;

/**
 * Datagen-билдер {@link ElectrolyserMetalRecipe} ({@code hbm_m:electrolyser_metal}).
 *
 * <p>Чистый ванильный 1.20.1 код внутри {@code //? if forge} — датаген только для 1.20.1-forge.</p>
 *
 * <p>JSON-формат (читается {@link ElectrolyserMetalRecipe.Serializer#readJson}):</p>
 * <pre>{@code
 * {
 *   "type": "hbm_m:electrolyser_metal",
 *   "ingredient":  { "item": "..." },
 *   "output_a":    { "item": "...", "count": 6 },
 *   "output_b":    { "item": "...", "count": 2 },  // optional
 *   "byproducts": [ { "item": "...", "count": 3 } ],
 *   "duration": 600
 * }
 * }</pre>
 */
public class ElectrolyserMetalRecipeBuilder extends BaseRecipeBuilder<ElectrolyserMetalRecipeBuilder> {

    private final Ingredient input;
    private final ItemStack outputA;
    @Nullable
    private final ItemStack outputB;
    private final ItemStack[] byproducts;
    private final int duration;

    private ElectrolyserMetalRecipeBuilder(Ingredient input, ItemStack outputA, @Nullable ItemStack outputB,
                                            ItemStack[] byproducts, int duration) {
        this.input = input;
        this.outputA = outputA;
        this.outputB = outputB;
        this.byproducts = byproducts;
        this.duration = duration;
    }

    public static ElectrolyserMetalRecipeBuilder electrolyserMetalRecipe(Ingredient input, ItemStack outputA,
                                                                          @Nullable ItemStack outputB,
                                                                          ItemStack[] byproducts, int duration) {
        return new ElectrolyserMetalRecipeBuilder(input, outputA, outputB, byproducts, duration);
    }

    @Override
    public Item getResult() {
        return outputA.getItem();
    }

    @Override
    protected void serializeRecipeData(JsonObject json) {
        json.add("ingredient", input.toJson());
        json.add("output_a", stackToJson(outputA));
        if (outputB != null && !outputB.isEmpty()) {
            json.add("output_b", stackToJson(outputB));
        }
        if (byproducts.length > 0) {
            JsonArray arr = new JsonArray();
            for (ItemStack byproduct : byproducts) {
                if (byproduct.isEmpty()) continue;
                arr.add(stackToJson(byproduct));
            }
            if (arr.size() > 0) json.add("byproducts", arr);
        }
        json.addProperty("duration", duration);
    }

    @Override
    protected RecipeSerializer<?> getType() {
        return ElectrolyserMetalRecipe.Serializer.INSTANCE;
    }
}
//?}
