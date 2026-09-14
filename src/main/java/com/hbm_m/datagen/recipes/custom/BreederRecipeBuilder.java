package com.hbm_m.datagen.recipes.custom;
//? if forge {
import com.google.gson.JsonObject;
import com.hbm_m.recipe.BreederRecipe;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;

/**
 * Datagen-билдер {@link BreederRecipe} ({@code hbm_m:breeder}).
 *
 * <p>Чистый ванильный 1.20.1 код внутри {@code //? if forge} — датаген только для 1.20.1-forge.</p>
 *
 * <p>JSON-формат (читается {@link BreederRecipe.Serializer#readJson}):</p>
 * <pre>{@code
 * {
 *   "type": "hbm_m:breeder",
 *   "ingredient": { "item": "..." },
 *   "result": { "item": "...", "count": 1 },
 *   "energy_per_tick": 100
 * }
 * }</pre>
 */
public class BreederRecipeBuilder extends BaseRecipeBuilder<BreederRecipeBuilder> {

    private final Ingredient input;
    private final ItemStack output;
    private final int energyPerTick;

    private BreederRecipeBuilder(Ingredient input, ItemStack output, int energyPerTick) {
        this.input = input;
        this.output = output;
        this.energyPerTick = energyPerTick;
    }

    public static BreederRecipeBuilder breederRecipe(Ingredient input, ItemStack output, int energyPerTick) {
        return new BreederRecipeBuilder(input, output, energyPerTick);
    }

    @Override
    public Item getResult() {
        return this.output.getItem();
    }

    @Override
    protected void serializeRecipeData(JsonObject json) {
        json.add("ingredient", input.toJson());
        json.add("result", stackToJson(output));
        json.addProperty("energy_per_tick", energyPerTick);
    }

    @Override
    protected RecipeSerializer<?> getType() {
        return BreederRecipe.Serializer.INSTANCE;
    }
}
//?}
