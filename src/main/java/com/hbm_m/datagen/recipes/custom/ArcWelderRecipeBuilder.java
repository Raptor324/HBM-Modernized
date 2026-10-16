package com.hbm_m.datagen.recipes.custom;
//? if forge {
import javax.annotation.Nullable;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.hbm_m.recipe.ArcWelderRecipe;

import dev.architectury.fluid.FluidStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;

/**
 * Datagen-билдер {@link ArcWelderRecipe} ({@code hbm_m:arc_welder}).
 *
 * <p>Чистый ванильный 1.20.1 код внутри {@code //? if forge} — датаген только для 1.20.1-forge.</p>
 *
 * <p>JSON-формат (читается {@link ArcWelderRecipe.Serializer#readJson}):</p>
 * <pre>{@code
 * {
 *   "type": "hbm_m:arc_welder",
 *   "ingredients": [ { "ingredient": { ...Ingredient... }, "count": 2 }, ... ],
 *   "fluid": { "fluid": "...", "amount": 1000 },  // optional
 *   "result": { "item": "...", "count": 1 },
 *   "duration": 200,
 *   "consumption": 10000
 * }
 * }</pre>
 */
public class ArcWelderRecipeBuilder extends BaseRecipeBuilder<ArcWelderRecipeBuilder> {

    private final Ingredient[] inputs;
    private final int[] counts;
    @Nullable
    private final FluidStack fluid;
    private final ItemStack output;
    private final int duration;
    private final long consumption;

    private ArcWelderRecipeBuilder(Ingredient[] inputs, int[] counts, @Nullable FluidStack fluid,
                                    ItemStack output, int duration, long consumption) {
        this.inputs = inputs;
        this.counts = counts;
        this.fluid = fluid;
        this.output = output;
        this.duration = duration;
        this.consumption = consumption;
    }

    /** Полная перегрузка: массивы ingredient+count, опциональный fluid, выход, длительность, потребление. */
    public static ArcWelderRecipeBuilder arcWelderRecipe(Ingredient[] inputs, int[] counts,
                                                          @Nullable FluidStack fluid,
                                                          ItemStack output, int duration, long consumption) {
        if (inputs.length != counts.length) {
            throw new IllegalArgumentException("arcWelderRecipe: inputs/counts length mismatch");
        }
        return new ArcWelderRecipeBuilder(inputs, counts, fluid, output, duration, consumption);
    }

    /** Без жидкости — упрощённая перегрузка. */
    public static ArcWelderRecipeBuilder arcWelderRecipe(Ingredient[] inputs, int[] counts,
                                                          ItemStack output, int duration, long consumption) {
        return arcWelderRecipe(inputs, counts, null, output, duration, consumption);
    }

    @Override
    public Item getResult() {
        return this.output.getItem();
    }

    @Override
    protected void serializeRecipeData(JsonObject json) {
        JsonArray arr = new JsonArray();
        for (int i = 0; i < inputs.length; i++) {
            JsonObject entry = new JsonObject();
            entry.add("ingredient", inputs[i].toJson());
            if (counts[i] > 1) entry.addProperty("count", counts[i]);
            arr.add(entry);
        }
        json.add("ingredients", arr);
        if (fluid != null && !fluid.isEmpty() && fluid.getAmount() > 0) {
            json.add("fluid", fluidStackToJson(fluid));
        }
        json.add("result", stackToJson(output));
        json.addProperty("duration", duration);
        json.addProperty("consumption", consumption);
    }

    @Override
    protected RecipeSerializer<?> getType() {
        return ArcWelderRecipe.Serializer.INSTANCE;
    }
}
//?}
