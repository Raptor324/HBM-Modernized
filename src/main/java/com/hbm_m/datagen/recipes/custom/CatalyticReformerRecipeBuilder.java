package com.hbm_m.datagen.recipes.custom;
//? if forge {
import com.google.gson.JsonObject;
import com.hbm_m.recipe.CatalyticReformerRecipe;

import dev.architectury.fluid.FluidStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeSerializer;

/**
 * Datagen-билдер {@link CatalyticReformerRecipe} ({@code hbm_m:catalytic_reformer}).
 *
 * <p>Чистый ванильный 1.20.1 код внутри {@code //? if forge} — датаген только для 1.20.1-forge.
 * Жидкостные стаки сериализуются через общую утилиту {@link BaseRecipeBuilder#fluidStackToJson}.</p>
 *
 * <p>JSON-формат (читается {@link CatalyticReformerRecipe.Serializer#readJson}):</p>
 * <pre>{@code
 * {
 *   "type": "hbm_m:catalytic_reformer",
 *   "input":    { "fluid": "...", "amount": 100 },
 *   "output_a": { "fluid": "...", "amount": 50 },
 *   "output_b": { "fluid": "...", "amount": 15 },
 *   "output_c": { "fluid": "...", "amount": 10 }
 * }
 * }</pre>
 *
 * <p>Предметного выхода нет — {@link #getResult()} возвращает {@link Items#AIR}
 * (ванильный {@code RecipeBuilder} требует реализацию).</p>
 */
public class CatalyticReformerRecipeBuilder extends BaseRecipeBuilder<CatalyticReformerRecipeBuilder> {

    private final FluidStack input;
    private final FluidStack outputA;
    private final FluidStack outputB;
    private final FluidStack outputC;

    private CatalyticReformerRecipeBuilder(FluidStack input, FluidStack outputA,
                                           FluidStack outputB, FluidStack outputC) {
        this.input = input;
        this.outputA = outputA;
        this.outputB = outputB;
        this.outputC = outputC;
    }

    public static CatalyticReformerRecipeBuilder catalyticReformerRecipe(FluidStack input,
                                                                         FluidStack outputA,
                                                                         FluidStack outputB,
                                                                         FluidStack outputC) {
        return new CatalyticReformerRecipeBuilder(input, outputA, outputB, outputC);
    }

    @Override
    public net.minecraft.world.item.Item getResult() {
        return Items.AIR;
    }

    @Override
    protected void serializeRecipeData(JsonObject json) {
        json.add("input", fluidStackToJson(this.input));
        json.add("output_a", fluidStackToJson(this.outputA));
        json.add("output_b", fluidStackToJson(this.outputB));
        json.add("output_c", fluidStackToJson(this.outputC));
    }

    @Override
    protected RecipeSerializer<?> getType() {
        return CatalyticReformerRecipe.Serializer.INSTANCE;
    }
}
//?}
