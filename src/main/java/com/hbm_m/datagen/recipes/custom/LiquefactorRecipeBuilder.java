package com.hbm_m.datagen.recipes.custom;
//? if forge {
import com.google.gson.JsonObject;
import com.hbm_m.recipe.LiquefactorRecipe;

import dev.architectury.fluid.FluidStack;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;

/**
 * Datagen-билдер {@link LiquefactorRecipe} ({@code hbm_m:liquefactor}).
 *
 * <p>Чистый ванильный 1.20.1 код внутри {@code //? if forge} — датаген только для 1.20.1-forge.</p>
 *
 * <p>JSON-формат (читается {@link LiquefactorRecipe.Serializer#readJson}):</p>
 * <pre>{@code
 * {
 *   "type": "hbm_m:liquefactor",
 *   "ingredient": { "item": "minecraft:coal" },
 *   "result": { "fluid": "hbm_m:coaloil", "amount": 100 }
 * }
 * }</pre>
 *
 * <p>Выход — жидкость, поэтому {@link #getResult()} возвращает {@link Items#AIR}
 * (ванильный {@code RecipeBuilder} требует реализацию, но для чисто жидкостных рецептов
 * результат не используется — та же конвенция, что у {@code MixerRecipeBuilder}).</p>
 */
public class LiquefactorRecipeBuilder extends BaseRecipeBuilder<LiquefactorRecipeBuilder> {

    private final Ingredient input;
    private final FluidStack output;

    private LiquefactorRecipeBuilder(Ingredient input, FluidStack output) {
        this.input = input;
        this.output = output;
    }

    public static LiquefactorRecipeBuilder liquefactorRecipe(Ingredient input, FluidStack output) {
        return new LiquefactorRecipeBuilder(input, output);
    }

    @Override
    public net.minecraft.world.item.Item getResult() {
        return Items.AIR; // жидкостный выход — предметного результата нет
    }

    @Override
    protected void serializeRecipeData(JsonObject json) {
        json.add("ingredient", input.toJson());
        JsonObject result = new JsonObject();
        ResourceLocation id = BuiltInRegistries.FLUID.getKey(output.getFluid());
        result.addProperty("fluid", id != null ? id.toString() : "minecraft:empty");
        result.addProperty("amount", output.getAmount());
        json.add("result", result);
    }

    @Override
    protected RecipeSerializer<?> getType() {
        return LiquefactorRecipe.Serializer.INSTANCE;
    }
}
//?}
