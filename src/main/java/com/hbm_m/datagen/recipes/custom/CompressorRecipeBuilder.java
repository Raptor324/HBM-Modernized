package com.hbm_m.datagen.recipes.custom;
//? if forge {
import com.google.gson.JsonObject;
import com.hbm_m.recipe.CompressorRecipe;

import dev.architectury.fluid.FluidStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeSerializer;

/**
 * Datagen-билдер {@link CompressorRecipe} ({@code hbm_m:compressor}).
 *
 * <p>Чистый ванильный 1.20.1 код внутри {@code //? if forge} — датаген только для 1.20.1-forge.
 * Жидкостные стаки сериализуются через общую утилиту {@link BaseRecipeBuilder#fluidStackToJson}.</p>
 *
 * <p>JSON-формат (читается {@link CompressorRecipe.Serializer#readJson}):</p>
 * <pre>{@code
 * {
 *   "type": "hbm_m:compressor",
 *   "input":          { "fluid": "...", "amount": 2000 },
 *   "input_pressure": 0,
 *   "output":         { "fluid": "...", "amount": 1000 },
 *   "output_pressure": 1,
 *   "duration": 20
 * }
 * }</pre>
 *
 * <p>Предметного выхода нет — {@link #getResult()} возвращает {@link Items#AIR}
 * (ванильный {@code RecipeBuilder} требует реализацию).</p>
 */
public class CompressorRecipeBuilder extends BaseRecipeBuilder<CompressorRecipeBuilder> {

    private final FluidStack input;
    private final int inputPressure;
    private final FluidStack output;
    private final int outputPressure;
    private final int duration;

    private CompressorRecipeBuilder(FluidStack input, int inputPressure,
                                    FluidStack output, int outputPressure, int duration) {
        this.input = input;
        this.inputPressure = inputPressure;
        this.output = output;
        this.outputPressure = outputPressure;
        this.duration = duration;
    }

    public static CompressorRecipeBuilder compressorRecipe(FluidStack input, int inputPressure,
                                                           FluidStack output, int outputPressure, int duration) {
        return new CompressorRecipeBuilder(input, inputPressure, output, outputPressure, duration);
    }

    @Override
    public net.minecraft.world.item.Item getResult() {
        return Items.AIR;
    }

    @Override
    protected void serializeRecipeData(JsonObject json) {
        json.add("input", fluidStackToJson(this.input));
        json.addProperty("input_pressure", this.inputPressure);
        json.add("output", fluidStackToJson(this.output));
        json.addProperty("output_pressure", this.outputPressure);
        json.addProperty("duration", this.duration);
    }

    @Override
    protected RecipeSerializer<?> getType() {
        return CompressorRecipe.Serializer.INSTANCE;
    }
}
//?}
