package com.hbm_m.datagen.recipes.custom;
//? if forge {
import javax.annotation.Nullable;

import com.google.gson.JsonObject;
import com.hbm_m.recipe.PyroOvenRecipe;

import dev.architectury.fluid.FluidStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;

/**
 * Datagen-билдер {@link PyroOvenRecipe} ({@code hbm_m:pyro_oven}).
 *
 * <p>Чистый ванильный 1.20.1 код внутри {@code //? if forge} — датаген только для 1.20.1-forge.</p>
 *
 * <p>JSON-формат (читается {@link PyroOvenRecipe.Serializer#readJson}); все входы/выходы опциональны:</p>
 * <pre>{@code
 * {
 *   "type": "hbm_m:pyro_oven",
 *   "fluid_input":  { "fluid": "...", "amount": 2000 },      // optional
 *   "item_input":   { "ingredient": { "item": "..." }, "count": 1 },  // optional
 *   "item_output":  { "item": "...", "count": 1 },            // optional
 *   "fluid_output": { "fluid": "...", "amount": 1000 },       // optional
 *   "duration": 100
 * }
 * }</pre>
 */
public class PyroOvenRecipeBuilder extends BaseRecipeBuilder<PyroOvenRecipeBuilder> {

    @Nullable
    private final FluidStack inputFluid;
    @Nullable
    private final Ingredient inputItem;
    private final int inputItemCount;
    @Nullable
    private final ItemStack outputItem;
    @Nullable
    private final FluidStack outputFluid;
    private final int duration;

    private PyroOvenRecipeBuilder(@Nullable FluidStack inputFluid, @Nullable Ingredient inputItem, int inputItemCount,
                                   @Nullable ItemStack outputItem, @Nullable FluidStack outputFluid, int duration) {
        this.inputFluid = inputFluid;
        this.inputItem = inputItem;
        this.inputItemCount = inputItemCount;
        this.outputItem = outputItem;
        this.outputFluid = outputFluid;
        this.duration = duration;
    }

    public static PyroOvenRecipeBuilder pyroOvenRecipe(@Nullable FluidStack inputFluid,
                                                        @Nullable Ingredient inputItem, int inputItemCount,
                                                        @Nullable ItemStack outputItem,
                                                        @Nullable FluidStack outputFluid, int duration) {
        return new PyroOvenRecipeBuilder(inputFluid, inputItem, inputItemCount, outputItem, outputFluid, duration);
    }

    @Override
    public Item getResult() {
        // Выход может быть чисто жидкостным — тогда результат не используется (как в MixerRecipeBuilder).
        return outputItem != null && !outputItem.isEmpty() ? outputItem.getItem() : net.minecraft.world.item.Items.AIR;
    }

    @Override
    protected void serializeRecipeData(JsonObject json) {
        if (inputFluid != null && !inputFluid.isEmpty() && inputFluid.getAmount() > 0) {
            json.add("fluid_input", fluidStackToJson(inputFluid));
        }
        if (inputItem != null) {
            JsonObject itemIn = new JsonObject();
            itemIn.add("ingredient", inputItem.toJson());
            if (inputItemCount > 1) itemIn.addProperty("count", inputItemCount);
            json.add("item_input", itemIn);
        }
        if (outputItem != null && !outputItem.isEmpty()) {
            json.add("item_output", stackToJson(outputItem));
        }
        if (outputFluid != null && !outputFluid.isEmpty() && outputFluid.getAmount() > 0) {
            json.add("fluid_output", fluidStackToJson(outputFluid));
        }
        json.addProperty("duration", duration);
    }

    @Override
    protected RecipeSerializer<?> getType() {
        return PyroOvenRecipe.Serializer.INSTANCE;
    }
}
//?}
