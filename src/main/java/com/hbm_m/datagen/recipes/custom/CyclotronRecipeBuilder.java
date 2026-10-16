package com.hbm_m.datagen.recipes.custom;
//? if forge {
import com.google.gson.JsonObject;
import com.hbm_m.recipe.CyclotronRecipe;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;

/**
 * Datagen-билдер {@link CyclotronRecipe} ({@code hbm_m:cyclotron}).
 *
 * <p>Чистый ванильный 1.20.1 код внутри {@code //? if forge} — датаген компилируется
 * только на 1.20.1-forge. Предметный выход сериализуется через общую утилиту
 * {@link BaseRecipeBuilder#stackToJson}.</p>
 *
 * <p>JSON-формат (читается {@link CyclotronRecipe.Serializer#readJson}):</p>
 * <pre>{@code
 * {
 *   "type": "hbm_m:cyclotron",
 *   "target": { ...Ingredient... },
 *   "input":   { ...Ingredient... },
 *   "result": { "item": "...", "count": 1 },
 *   "amat": 50
 * }
 * }</pre>
 */
public class CyclotronRecipeBuilder extends BaseRecipeBuilder<CyclotronRecipeBuilder> {

    private final Ingredient target;
    private final Ingredient input;
    private final ItemStack output;
    private final int amat;

    private CyclotronRecipeBuilder(Ingredient target, Ingredient input, ItemStack output, int amat) {
        this.target = target;
        this.input = input;
        this.output = output;
        this.amat = amat;
    }

    public static CyclotronRecipeBuilder cyclotronRecipe(Ingredient target, Ingredient input,
                                                         ItemStack output, int amat) {
        return new CyclotronRecipeBuilder(target, input, output, amat);
    }

    /** Item-перегрузка: target/input — одиночные предметы. */
    public static CyclotronRecipeBuilder cyclotronRecipe(Item target, Item input, ItemStack output, int amat) {
        return cyclotronRecipe(Ingredient.of(target), Ingredient.of(input), output, amat);
    }

    /** Item-tag перегрузка для input: target — предмет, input — forge-тег (строка вида {@code "forge:powders/lithium"}). */
    public static CyclotronRecipeBuilder cyclotronRecipe(Item target, String inputTagId,
                                                         ItemStack output, int amat) {
        net.minecraft.tags.TagKey<Item> tag = net.minecraft.tags.TagKey.create(
                net.minecraft.core.registries.Registries.ITEM,
                net.minecraft.resources.ResourceLocation.parse(inputTagId));
        return cyclotronRecipe(Ingredient.of(target), Ingredient.of(tag), output, amat);
    }

    @Override
    public Item getResult() {
        return this.output.getItem();
    }

    @Override
    protected void serializeRecipeData(JsonObject json) {
        json.add("target", this.target.toJson());
        json.add("input", this.input.toJson());
        // Унифицированная сериализация ItemStack (через BaseRecipeBuilder.stackToJson).
        json.add("result", stackToJson(this.output));
        json.addProperty("amat", this.amat);
    }

    @Override
    protected RecipeSerializer<?> getType() {
        return CyclotronRecipe.Serializer.INSTANCE;
    }
}
//?}
