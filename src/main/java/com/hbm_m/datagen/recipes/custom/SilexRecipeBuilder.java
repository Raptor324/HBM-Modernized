package com.hbm_m.datagen.recipes.custom;
//? if forge {
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.hbm_m.recipe.SilexRecipe;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;

/**
 * Datagen-билдер {@link SilexRecipe} ({@code hbm_m:silex}).
 *
 * <p>Чистый ванильный 1.20.1 код внутри {@code //? if forge} — датаген только для 1.20.1-forge.</p>
 *
 * <p>JSON-формат (читается {@link SilexRecipe.Serializer#readJson}):</p>
 * <pre>{@code
 * {
 *   "type": "hbm_m:silex",
 *   "ingredient": { "item": "..." },
 *   "peroxide_mb": 100,
 *   "duration": 100,
 *   "outputs": [
 *     { "result": { "item": "...", "count": 1 }, "weight": 1 },
 *     { "result": { "item": "...", "count": 1 }, "weight": 11 }
 *   ]
 * }
 * }</pre>
 */
public class SilexRecipeBuilder extends BaseRecipeBuilder<SilexRecipeBuilder> {

    private final Ingredient input;
    private final int peroxideMb;
    private final int duration;
    private final ItemStack[] outputs;
    private final int[] weights;

    private SilexRecipeBuilder(Ingredient input, int peroxideMb, int duration,
                               ItemStack[] outputs, int[] weights) {
        if (outputs.length != weights.length) {
            throw new IllegalArgumentException("silexRecipe: outputs/weights length mismatch");
        }
        this.input = input;
        this.peroxideMb = peroxideMb;
        this.duration = duration;
        this.outputs = outputs;
        this.weights = weights;
    }

    public static SilexRecipeBuilder silexRecipe(Ingredient input, int peroxideMb, int duration,
                                                 ItemStack[] outputs, int[] weights) {
        return new SilexRecipeBuilder(input, peroxideMb, duration, outputs, weights);
    }

    @Override
    public Item getResult() {
        // Выход случаен; для recipe-идентификации берём первый взвешенный выход.
        return this.outputs.length > 0 ? this.outputs[0].getItem() : net.minecraft.world.item.Items.AIR;
    }

    @Override
    protected void serializeRecipeData(JsonObject json) {
        json.add("ingredient", input.toJson());
        json.addProperty("peroxide_mb", peroxideMb);
        json.addProperty("duration", duration);
        JsonArray arr = new JsonArray();
        for (int i = 0; i < outputs.length; i++) {
            JsonObject entry = new JsonObject();
            entry.add("result", stackToJson(outputs[i]));
            entry.addProperty("weight", weights[i]);
            arr.add(entry);
        }
        json.add("outputs", arr);
    }

    @Override
    protected RecipeSerializer<?> getType() {
        return SilexRecipe.Serializer.INSTANCE;
    }
}
//?}
