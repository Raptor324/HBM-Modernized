package com.hbm_m.datagen.recipes.custom;
//? if forge {
import com.google.gson.JsonObject;
import com.hbm_m.recipe.SolidificationRecipe;

import dev.architectury.fluid.FluidStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;

/**
 * Datagen-билдер {@link SolidificationRecipe} ({@code hbm_m:solidification}).
 *
 * <p>Чистый ванильный 1.20.1 код внутри {@code //? if forge} — датаген только для 1.20.1-forge.</p>
 *
 * <p>JSON-формат (читается {@link SolidificationRecipe.Serializer#readJson}):</p>
 * <pre>{@code
 * {
 *   "type": "hbm_m:solidification",
 *   "fluid": { "fluid": "hbm_m:smear", "amount": 21000 },
 *   "result": { "item": "hbm_m:solid_fuel", "count": 1 }
 * }
 * }</pre>
 */
public class SolidificationRecipeBuilder extends BaseRecipeBuilder<SolidificationRecipeBuilder> {

    private final FluidStack input;
    private final ItemStack output;

    private SolidificationRecipeBuilder(FluidStack input, ItemStack output) {
        this.input = input;
        this.output = output;
    }

    public static SolidificationRecipeBuilder solidificationRecipe(FluidStack input, ItemStack output) {
        return new SolidificationRecipeBuilder(input, output);
    }

    @Override
    public Item getResult() {
        return this.output.getItem();
    }

    @Override
    protected void serializeRecipeData(JsonObject json) {
        json.add("fluid", fluidStackToJson(input));
        json.add("result", stackToJson(output));
    }

    @Override
    protected RecipeSerializer<?> getType() {
        return SolidificationRecipe.Serializer.INSTANCE;
    }
}
//?}
