package com.hbm_m.datagen.recipes.custom;
//? if forge {
import com.google.gson.JsonObject;
import com.hbm_m.recipe.ExposureChamberRecipe;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;

/**
 * Datagen-билдер {@link ExposureChamberRecipe} ({@code hbm_m:exposure_chamber}).
 *
 * <p>Чистый ванильный 1.20.1 код внутри {@code //? if forge} — датаген только для 1.20.1-forge.</p>
 *
 * <p>JSON-формат (читается {@link ExposureChamberRecipe.Serializer#readJson}):</p>
 * <pre>{@code
 * {
 *   "type": "hbm_m:exposure_chamber",
 *   "particle":   { "item": "..." },
 *   "ingredient": { "item": "..." },
 *   "result":     { "item": "...", "count": 1 }
 * }
 * }</pre>
 */
public class ExposureChamberRecipeBuilder extends BaseRecipeBuilder<ExposureChamberRecipeBuilder> {

    private final ItemStack particle;
    private final Ingredient ingredient;
    private final ItemStack output;

    private ExposureChamberRecipeBuilder(ItemStack particle, Ingredient ingredient, ItemStack output) {
        this.particle = particle;
        this.ingredient = ingredient;
        this.output = output;
    }

    public static ExposureChamberRecipeBuilder exposureChamberRecipe(ItemStack particle,
                                                                     Ingredient ingredient, ItemStack output) {
        return new ExposureChamberRecipeBuilder(particle, ingredient, output);
    }

    @Override
    public Item getResult() {
        return this.output.getItem();
    }

    @Override
    protected void serializeRecipeData(JsonObject json) {
        json.add("particle", stackToJson(particle));
        json.add("ingredient", ingredient.toJson());
        json.add("result", stackToJson(output));
    }

    @Override
    protected RecipeSerializer<?> getType() {
        return ExposureChamberRecipe.Serializer.INSTANCE;
    }
}
//?}
