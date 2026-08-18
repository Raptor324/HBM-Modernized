package com.hbm_m.datagen.recipes.custom;
//? if forge {
import com.google.gson.JsonObject;
import com.hbm_m.recipe.HydrotreaterRecipe;

import dev.architectury.fluid.FluidStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeSerializer;

/**
 * Datagen-билдер {@link HydrotreaterRecipe} ({@code hbm_m:hydrotreater}).
 *
 * <p>Чистый ванильный 1.20.1 код внутри {@code //? if forge} — датаген только для 1.20.1-forge.
 * Жидкостные стаки сериализуются через общую утилиту {@link BaseRecipeBuilder#fluidStackToJson}.</p>
 *
 * <p>JSON-формат (читается {@link HydrotreaterRecipe.Serializer#readJson}):</p>
 * <pre>{@code
 * {
 *   "type": "hbm_m:hydrotreater",
 *   "input":    { "fluid": "...", "amount": 100 },
 *   "hydrogen": { "fluid": "...", "amount": 5 },
 *   "output":   { "fluid": "...", "amount": 90 },
 *   "sour_gas": { "fluid": "...", "amount": 15 }
 * }
 * }</pre>
 *
 * <p>Предметного выхода нет — {@link #getResult()} возвращает {@link Items#AIR}
 * (ванильный {@code RecipeBuilder} требует реализацию).</p>
 */
public class HydrotreaterRecipeBuilder extends BaseRecipeBuilder<HydrotreaterRecipeBuilder> {

    private final FluidStack input;
    private final FluidStack hydrogen;
    private final FluidStack output;
    private final FluidStack sourGas;

    private HydrotreaterRecipeBuilder(FluidStack input, FluidStack hydrogen,
                                      FluidStack output, FluidStack sourGas) {
        this.input = input;
        this.hydrogen = hydrogen;
        this.output = output;
        this.sourGas = sourGas;
    }

    public static HydrotreaterRecipeBuilder hydrotreaterRecipe(FluidStack input, FluidStack hydrogen,
                                                               FluidStack output, FluidStack sourGas) {
        return new HydrotreaterRecipeBuilder(input, hydrogen, output, sourGas);
    }

    @Override
    public net.minecraft.world.item.Item getResult() {
        return Items.AIR;
    }

    @Override
    protected void serializeRecipeData(JsonObject json) {
        json.add("input", fluidStackToJson(this.input));
        json.add("hydrogen", fluidStackToJson(this.hydrogen));
        json.add("output", fluidStackToJson(this.output));
        json.add("sour_gas", fluidStackToJson(this.sourGas));
    }

    @Override
    protected RecipeSerializer<?> getType() {
        return HydrotreaterRecipe.Serializer.INSTANCE;
    }
}
//?}
