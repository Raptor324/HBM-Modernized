package com.hbm_m.datagen.recipes.custom;
//? if forge {
import javax.annotation.Nullable;

import com.google.gson.JsonObject;
import com.hbm_m.recipe.CrackingTowerRecipe;

import dev.architectury.fluid.FluidStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeSerializer;

/**
 * Datagen-билдер {@link CrackingTowerRecipe} ({@code hbm_m:cracking_tower}).
 *
 * <p>Чистый ванильный 1.20.1 код внутри {@code //? if forge} — датаген только для 1.20.1-forge.
 * Жидкостные стаки сериализуются через общую утилиту {@link BaseRecipeBuilder#fluidStackToJson}.</p>
 *
 * <p>JSON-формат (читается {@link CrackingTowerRecipe.Serializer#readJson}):</p>
 * <pre>{@code
 * {
 *   "type": "hbm_m:cracking_tower",
 *   "input":    { "fluid": "...", "amount": 100 },
 *   "output_a": { "fluid": "...", "amount": 80 },
 *   "output_b": { "fluid": "...", "amount": 20 }   // null => ключ опускается => нет второго выхода
 * }
 * }</pre>
 *
 * <p>Предметного выхода нет — {@link #getResult()} возвращает {@link Items#AIR}
 * (ванильный {@code RecipeBuilder} требует реализацию).</p>
 */
public class CrackingTowerRecipeBuilder extends BaseRecipeBuilder<CrackingTowerRecipeBuilder> {

    private final FluidStack input;
    private final FluidStack outputA;
    @Nullable
    private final FluidStack outputB;

    private CrackingTowerRecipeBuilder(FluidStack input, FluidStack outputA, @Nullable FluidStack outputB) {
        this.input = input;
        this.outputA = outputA;
        this.outputB = outputB;
    }

    public static CrackingTowerRecipeBuilder crackingTowerRecipe(FluidStack input, FluidStack outputA,
                                                                 @Nullable FluidStack outputB) {
        return new CrackingTowerRecipeBuilder(input, outputA, outputB);
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
        return CrackingTowerRecipe.Serializer.INSTANCE;
    }
}
//?}
