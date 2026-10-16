package com.hbm_m.datagen.recipes.custom;
//? if forge {
import javax.annotation.Nullable;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.hbm_m.recipe.RotaryFurnaceRecipe;

import dev.architectury.fluid.FluidStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;

/**
 * Datagen-билдер {@link RotaryFurnaceRecipe} ({@code hbm_m:rotary_furnace}).
 *
 * <p>Чистый ванильный 1.20.1 код внутри {@code //? if forge} — датаген только для 1.20.1-forge.</p>
 *
 * <p>JSON-формат (читается {@link RotaryFurnaceRecipe.Serializer#readJson}):</p>
 * <pre>{@code
 * {
 *   "type": "hbm_m:rotary_furnace",
 *   "ingredients": [ { "ingredient": { ... }, "count": 1 }, ... ],   // 0..3
 *   "fluid": { "fluid": "...", "amount": 100 },  // optional
 *   "result": { "item": "...", "count": 1 },
 *   "duration": 100
 * }
 * }</pre>
 */
public class RotaryFurnaceRecipeBuilder extends BaseRecipeBuilder<RotaryFurnaceRecipeBuilder> {

    private final Ingredient[] inputs;
    private final int[] counts;
    @Nullable
    private final FluidStack fluid;
    private final ItemStack output;
    private final int duration;

    private RotaryFurnaceRecipeBuilder(Ingredient[] inputs, int[] counts, @Nullable FluidStack fluid,
                                        ItemStack output, int duration) {
        this.inputs = inputs;
        this.counts = counts;
        this.fluid = fluid;
        this.output = output;
        this.duration = duration;
    }

    /** Полная перегрузка: массивы ingredient+count, опциональная жидкость, выход, длительность. */
    public static RotaryFurnaceRecipeBuilder rotaryFurnaceRecipe(Ingredient[] inputs, int[] counts,
                                                                 @Nullable FluidStack fluid,
                                                                 ItemStack output, int duration) {
        if (inputs.length != counts.length) {
            throw new IllegalArgumentException("rotaryFurnaceRecipe: inputs/counts length mismatch");
        }
        return new RotaryFurnaceRecipeBuilder(inputs, counts, fluid, output, duration);
    }

    /** Без жидкости — упрощённая перегрузка. */
    public static RotaryFurnaceRecipeBuilder rotaryFurnaceRecipe(Ingredient[] inputs, int[] counts,
                                                                 ItemStack output, int duration) {
        return rotaryFurnaceRecipe(inputs, counts, null, output, duration);
    }

    @Override
    public Item getResult() {
        return this.output.getItem();
    }

    @Override
    protected void serializeRecipeData(JsonObject json) {
        if (inputs.length > 0) {
            JsonArray arr = new JsonArray();
            for (int i = 0; i < inputs.length; i++) {
                JsonObject entry = new JsonObject();
                entry.add("ingredient", inputs[i].toJson());
                if (counts[i] > 1) entry.addProperty("count", counts[i]);
                arr.add(entry);
            }
            json.add("ingredients", arr);
        }
        if (fluid != null && !fluid.isEmpty() && fluid.getAmount() > 0) {
            json.add("fluid", fluidStackToJson(fluid));
        }
        json.add("result", stackToJson(output));
        json.addProperty("duration", duration);
    }

    @Override
    protected RecipeSerializer<?> getType() {
        return RotaryFurnaceRecipe.Serializer.INSTANCE;
    }
}
//?}
