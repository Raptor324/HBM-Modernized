package com.hbm_m.datagen.recipes.custom;
//? if forge {
import javax.annotation.Nullable;

import com.google.gson.JsonObject;
import com.hbm_m.recipe.RadiolysisRecipe;

import dev.architectury.fluid.FluidStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeSerializer;

/**
 * Datagen-билдер {@link RadiolysisRecipe} ({@code hbm_m:radiolysis}).
 *
 * <p>Чистый ванильный 1.20.1 код внутри {@code //? if forge} — датаген только для 1.20.1-forge.
 * Жидкостные стаки сериализуются через общую утилиту {@link BaseRecipeBuilder#fluidStackToJson}.</p>
 *
 * <p>JSON-формат (читается {@link RadiolysisRecipe.Serializer#readJson}):</p>
 * <pre>{@code
 * {
 *   "type": "hbm_m:radiolysis",
 *   "input":    { "fluid": "...", "amount": 100 },
 *   "output_a": { "fluid": "...", "amount": 80 },
 *   "output_b": { "fluid": "...", "amount": 20 }   // null => ключ опускается => нет второго выхода
 * }
 * }</pre>
 *
 * <p>Предметного выхода нет — {@link #getResult()} возвращает {@link Items#AIR}
 * (ванильный {@code RecipeBuilder} требует реализацию).</p>
 */
public class RadiolysisRecipeBuilder extends BaseRecipeBuilder<RadiolysisRecipeBuilder> {

    private final FluidStack input;
    private final FluidStack outputA;
    @Nullable
    private final FluidStack outputB;

    private RadiolysisRecipeBuilder(FluidStack input, FluidStack outputA, @Nullable FluidStack outputB) {
        this.input = input;
        this.outputA = outputA;
        this.outputB = outputB;
    }

    public static RadiolysisRecipeBuilder radiolysisRecipe(FluidStack input, FluidStack outputA,
                                                           @Nullable FluidStack outputB) {
        return new RadiolysisRecipeBuilder(input, outputA, outputB);
    }

    @Override
    public net.minecraft.world.item.Item getResult() {
        return Items.AIR;
    }

    @Override
    protected void serializeRecipeData(JsonObject json) {
        json.add("input", fluidStackToJson(this.input));
        json.add("output_a", fluidStackToJson(this.outputA));
        if (this.outputB != null && !this.outputB.isEmpty() && this.outputB.getAmount() > 0) {
            json.add("output_b", fluidStackToJson(this.outputB));
        }
    }

    @Override
    protected RecipeSerializer<?> getType() {
        return RadiolysisRecipe.Serializer.INSTANCE;
    }
}
//?}
