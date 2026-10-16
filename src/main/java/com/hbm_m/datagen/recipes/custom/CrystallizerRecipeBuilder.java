package com.hbm_m.datagen.recipes.custom;
//? if forge {
import javax.annotation.Nullable;

import com.google.gson.JsonObject;
import com.hbm_m.recipe.CrystallizerRecipe;

import dev.architectury.fluid.FluidStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;

/**
 * Datagen-билдер {@link CrystallizerRecipe} ({@code hbm_m:crystallizer}).
 *
 * <p>Чистый ванильный 1.20.1 код внутри {@code //? if forge} — датаген компилируется
 * только на 1.20.1-forge. Сериализация стак/жидкость идёт через общие утилиты
 * {@link BaseRecipeBuilder#stackToJson} / {@link BaseRecipeBuilder#fluidStackToJson}.</p>
 *
 * <p>JSON-формат (читается {@link CrystallizerRecipe.Serializer#readJson}):</p>
 * <pre>{@code
 * {
 *   "type": "hbm_m:crystallizer",
 *   "ingredient": { ...Ingredient... },
 *   "count": 1,                    // optional, default 1
 *   "acid": { "fluid": "...", "amount": 500 },  // optional; null => любой бак
 *   "result": { "item": "...", "count": 1 },
 *   "duration": 600,
 *   "productivity": 0.05
 * }
 * }</pre>
 */
public class CrystallizerRecipeBuilder extends BaseRecipeBuilder<CrystallizerRecipeBuilder> {

    private final Ingredient input;
    private final int inputCount;
    @Nullable
    private final FluidStack acid;
    private final ItemStack output;
    private final int duration;
    private final float productivity;

    private CrystallizerRecipeBuilder(Ingredient input, int inputCount, @Nullable FluidStack acid,
                                      ItemStack output, int duration, float productivity) {
        this.input = input;
        this.inputCount = inputCount;
        this.acid = acid;
        this.output = output;
        this.duration = duration;
        this.productivity = productivity;
    }

    public static CrystallizerRecipeBuilder crystallizerRecipe(Ingredient input, int inputCount,
                                                                @Nullable FluidStack acid,
                                                                ItemStack output, int duration, float productivity) {
        return new CrystallizerRecipeBuilder(input, inputCount, acid, output, duration, productivity);
    }

    /** Item-перегрузка: {@code input} одиночным предметом. */
    public static CrystallizerRecipeBuilder crystallizerRecipe(Item input, int inputCount,
                                                               @Nullable FluidStack acid,
                                                               ItemStack output, int duration, float productivity) {
        return crystallizerRecipe(Ingredient.of(input), inputCount, acid, output, duration, productivity);
    }

    /** Item-tag перегрузка: {@code input} через forge-тег (строка вида {@code "forge:ores/iron"}). */
    public static CrystallizerRecipeBuilder crystallizerRecipe(String tagId, int inputCount,
                                                               @Nullable FluidStack acid,
                                                               ItemStack output, int duration, float productivity) {
        net.minecraft.tags.TagKey<Item> tag = net.minecraft.tags.TagKey.create(
                net.minecraft.core.registries.Registries.ITEM,
                net.minecraft.resources.ResourceLocation.parse(tagId));
        return crystallizerRecipe(Ingredient.of(tag), inputCount, acid, output, duration, productivity);
    }

    @Override
    public Item getResult() {
        return this.output.getItem();
    }

    @Override
    protected void serializeRecipeData(JsonObject json) {
        json.add("ingredient", this.input.toJson());
        if (this.inputCount > 1) {
            json.addProperty("count", this.inputCount);
        }
        // acid опционален: null/empty => рецепт работает с любым баком (см. CrystallizerRecipe.matchesAcid).
        if (this.acid != null && !this.acid.isEmpty() && this.acid.getAmount() > 0) {
            json.add("acid", fluidStackToJson(this.acid));
        }
        // Унифицированная сериализация ItemStack (через BaseRecipeBuilder.stackToJson).
        json.add("result", stackToJson(this.output));
        json.addProperty("duration", this.duration);
        json.addProperty("productivity", this.productivity);
    }

    @Override
    protected RecipeSerializer<?> getType() {
        return CrystallizerRecipe.Serializer.INSTANCE;
    }
}
//?}
