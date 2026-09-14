package com.hbm_m.datagen.recipes.custom;
//? if forge {
import com.google.gson.JsonObject;
import com.hbm_m.recipe.FractionTowerRecipe;

import dev.architectury.fluid.FluidStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeSerializer;

/**
 * Datagen-билдер {@link FractionTowerRecipe} ({@code hbm_m:fraction_tower}).
 *
 * <p>Чистый ванильный 1.20.1 код внутри {@code //? if forge} — датаген только для 1.20.1-forge.
 * Жидкостные стаки сериализуются через общую утилиту {@link BaseRecipeBuilder#fluidStackToJson}.</p>
 *
 * <p>JSON-формат (читается {@link FractionTowerRecipe.Serializer#readJson}):</p>
 * <pre>{@code
 * {
 *   "type": "hbm_m:fraction_tower",
 *   "input":    { "fluid": "...", "amount": 100 },
 *   "output_a": { "fluid": "...", "amount": 30 },
 *   "output_b": { "fluid": "...", "amount": 70 }
 * }
 * }</pre>
 *
 * <p>Предметного выхода нет — {@link #getResult()} возвращает {@link Items#AIR}
 * (ванильный {@code RecipeBuilder} требует реализацию).</p>
 */
public class FractionTowerRecipeBuilder extends BaseRecipeBuilder<FractionTowerRecipeBuilder> {

    private final FluidStack input;
    private final FluidStack outputA;
    private final FluidStack outputB;

    private FractionTowerRecipeBuilder(FluidStack input, FluidStack outputA, FluidStack outputB) {
        this.input = input;
        this.outputA = outputA;
        this.outputB = outputB;
    }

    public static FractionTowerRecipeBuilder fractionTowerRecipe(FluidStack input, FluidStack outputA, FluidStack outputB) {
        return new FractionTowerRecipeBuilder(input, outputA, outputB);
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
    }

    @Override
    protected RecipeSerializer<?> getType() {
        return FractionTowerRecipe.Serializer.INSTANCE;
    }
}
//?}
