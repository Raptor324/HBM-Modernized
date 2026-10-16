package com.hbm_m.datagen.recipes.custom;
//? if forge {
import javax.annotation.Nullable;

import com.google.gson.JsonObject;
import com.hbm_m.recipe.ArcFurnaceRecipe;

import dev.architectury.fluid.FluidStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;

/**
 * Datagen-билдер {@link ArcFurnaceRecipe} ({@code hbm_m:arc_furnace}).
 *
 * <p>Чистый ванильный 1.20.1 код внутри {@code //? if forge} — датаген только для 1.20.1-forge.</p>
 *
 * <p>JSON-формат (читается {@link ArcFurnaceRecipe.Serializer#readJson}):</p>
 * <pre>{@code
 * {
 *   "type": "hbm_m:arc_furnace",
 *   "ingredient": { ...Ingredient... },
 *   "count": 1,
 *   "result": { "item": "...", "count": 1 },   // optional
 *   "fluid1": { "fluid": "...", "amount": 1000 }, // optional
 *   "fluid2": { "fluid": "...", "amount": 1000 }, // optional
 *   "duration": 200
 * }
 * }</pre>
 */
public class ArcFurnaceRecipeBuilder extends BaseRecipeBuilder<ArcFurnaceRecipeBuilder> {

    private final Ingredient input;
    private final int inputCount;
    @Nullable
    private final ItemStack output;
    @Nullable
    private final FluidStack fluid1;
    @Nullable
    private final FluidStack fluid2;
    private final int duration;

    private ArcFurnaceRecipeBuilder(Ingredient input, int inputCount,
                                     @Nullable ItemStack output,
                                     @Nullable FluidStack fluid1,
                                     @Nullable FluidStack fluid2,
                                     int duration) {
        this.input = input;
        this.inputCount = inputCount;
        this.output = output;
        this.fluid1 = fluid1;
        this.fluid2 = fluid2;
        this.duration = duration;
    }

    /** Полная перегрузка: ингредиент, количество, выход, жидкости, длительность. */
    public static ArcFurnaceRecipeBuilder arcFurnaceRecipe(Ingredient input, int inputCount,
                                                            @Nullable ItemStack output,
                                                            @Nullable FluidStack fluid1,
                                                            @Nullable FluidStack fluid2,
                                                            int duration) {
        return new ArcFurnaceRecipeBuilder(input, inputCount, output, fluid1, fluid2, duration);
    }

    /** Без жидкостей: ингредиент (количество 1), выход, длительность. */
    public static ArcFurnaceRecipeBuilder arcFurnaceRecipe(Ingredient input, ItemStack output, int duration) {
        return arcFurnaceRecipe(input, 1, output, null, null, duration);
    }

    /** Без жидкостей: ингредиент с количеством, выход, длительность. */
    public static ArcFurnaceRecipeBuilder arcFurnaceRecipe(Ingredient input, int inputCount,
                                                            ItemStack output, int duration) {
        return arcFurnaceRecipe(input, inputCount, output, null, null, duration);
    }

    @Override
    public Item getResult() {
        return (output != null && !output.isEmpty()) ? output.getItem() : net.minecraft.world.item.Items.AIR;
    }

    @Override
    protected void serializeRecipeData(JsonObject json) {
        json.add("ingredient", input.toJson());
        if (inputCount > 1) json.addProperty("count", inputCount);
        if (output != null && !output.isEmpty()) {
            json.add("result", stackToJson(output));
        }
        if (fluid1 != null && !fluid1.isEmpty() && fluid1.getAmount() > 0) {
            json.add("fluid1", fluidStackToJson(fluid1));
        }
        if (fluid2 != null && !fluid2.isEmpty() && fluid2.getAmount() > 0) {
            json.add("fluid2", fluidStackToJson(fluid2));
        }
        json.addProperty("duration", duration);
    }

    @Override
    protected RecipeSerializer<?> getType() {
        return ArcFurnaceRecipe.Serializer.INSTANCE;
    }
}
//?}
