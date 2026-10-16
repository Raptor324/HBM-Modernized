package com.hbm_m.datagen.recipes.custom;
//? if forge {
import javax.annotation.Nullable;

import com.google.gson.JsonObject;
import com.hbm_m.recipe.CombinationOvenRecipe;

import dev.architectury.fluid.FluidStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;

/**
 * Datagen-билдер {@link CombinationOvenRecipe} ({@code hbm_m:combination_oven}).
 *
 * <p>Чистый ванильный 1.20.1 код внутри {@code //? if forge} — датаген только для 1.20.1-forge.</p>
 *
 * <p>JSON-формат (читается {@link CombinationOvenRecipe.Serializer#readJson}):</p>
 * <pre>{@code
 * {
 *   "type": "hbm_m:combination_oven",
 *   "ingredient": { ...Ingredient... },
 *   "count": 1,
 *   "fluid": { "fluid": "...", "amount": 50 },  // optional
 *   "result": { "item": "...", "count": 1 },
 *   "duration": 200
 * }
 * }</pre>
 */
public class CombinationOvenRecipeBuilder extends BaseRecipeBuilder<CombinationOvenRecipeBuilder> {

    private final Ingredient input;
    private final int inputCount;
    @Nullable
    private final FluidStack fluidInput;
    private final ItemStack output;
    private final int duration;

    private CombinationOvenRecipeBuilder(Ingredient input, int inputCount,
                                          @Nullable FluidStack fluidInput,
                                          ItemStack output, int duration) {
        this.input = input;
        this.inputCount = inputCount;
        this.fluidInput = fluidInput;
        this.output = output;
        this.duration = duration;
    }

    /** Полная перегрузка с жидкостным входом. */
    public static CombinationOvenRecipeBuilder combinationOvenRecipe(Ingredient input, int inputCount,
                                                                      @Nullable FluidStack fluidInput,
                                                                      ItemStack output, int duration) {
        return new CombinationOvenRecipeBuilder(input, inputCount, fluidInput, output, duration);
    }

    /** Без жидкостного входа (inputCount = 1). */
    public static CombinationOvenRecipeBuilder combinationOvenRecipe(Ingredient input, ItemStack output, int duration) {
        return combinationOvenRecipe(input, 1, null, output, duration);
    }

    @Override
    public Item getResult() {
        return output.isEmpty() ? net.minecraft.world.item.Items.AIR : output.getItem();
    }

    @Override
    protected void serializeRecipeData(JsonObject json) {
        json.add("ingredient", input.toJson());
        if (inputCount > 1) json.addProperty("count", inputCount);
        if (fluidInput != null && !fluidInput.isEmpty() && fluidInput.getAmount() > 0) {
            json.add("fluid", fluidStackToJson(fluidInput));
        }
        json.add("result", stackToJson(output));
        json.addProperty("duration", duration);
    }

    @Override
    protected RecipeSerializer<?> getType() {
        return CombinationOvenRecipe.Serializer.INSTANCE;
    }
}
//?}
