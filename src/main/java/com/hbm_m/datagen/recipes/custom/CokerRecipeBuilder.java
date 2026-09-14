package com.hbm_m.datagen.recipes.custom;
//? if forge {
import javax.annotation.Nullable;

import com.google.gson.JsonObject;
import com.hbm_m.recipe.CokerRecipe;

import dev.architectury.fluid.FluidStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;

/**
 * Datagen-билдер {@link CokerRecipe} ({@code hbm_m:coker}).
 *
 * <p>Чистый ванильный 1.20.1 код внутри {@code //? if forge} — датаген только для 1.20.1-forge.</p>
 *
 * <p>JSON-формат (читается {@link CokerRecipe.Serializer#readJson}):</p>
 * <pre>{@code
 * {
 *   "type": "hbm_m:coker",
 *   "input":     { "fluid": "...", "amount": 11000 },
 *   "result":    { "item": "hbm_m:coke_petroleum" },
 *   "byproduct": { "fluid": "...", "amount": 1100 }   // optional
 * }
 * }</pre>
 */
public class CokerRecipeBuilder extends BaseRecipeBuilder<CokerRecipeBuilder> {

    private final FluidStack input;
    private final ItemStack output;
    @Nullable
    private final FluidStack byproduct;

    private CokerRecipeBuilder(FluidStack input, ItemStack output, @Nullable FluidStack byproduct) {
        this.input = input;
        this.output = output;
        this.byproduct = byproduct;
    }

    public static CokerRecipeBuilder cokerRecipe(FluidStack input, ItemStack output, @Nullable FluidStack byproduct) {
        return new CokerRecipeBuilder(input, output, byproduct);
    }

    @Override
    public Item getResult() {
        return output.getItem();
    }

    @Override
    protected void serializeRecipeData(JsonObject json) {
        json.add("input", fluidStackToJson(input));
        json.add("result", stackToJson(output));
        if (byproduct != null && !byproduct.isEmpty() && byproduct.getAmount() > 0) {
            json.add("byproduct", fluidStackToJson(byproduct));
        }
    }

    @Override
    protected RecipeSerializer<?> getType() {
        return CokerRecipe.Serializer.INSTANCE;
    }
}
//?}
