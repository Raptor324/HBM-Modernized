package com.hbm_m.datagen.recipes.custom;
//? if forge {
import com.google.gson.JsonObject;
import com.hbm_m.recipe.RadGenRecipe;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;

/**
 * Datagen-билдер {@link RadGenRecipe} ({@code hbm_m:radgen}).
 *
 * <p>Чистый ванильный 1.20.1 код внутри {@code //? if forge} — датаген только для 1.20.1-forge.</p>
 *
 * <p>JSON-формат (читается {@link RadGenRecipe.Serializer#readJson}):</p>
 * <pre>{@code
 * {
 *   "type": "hbm_m:radgen",
 *   "ingredient": { "item": "..." },
 *   "power": 1500,
 *   "duration": 36000,
 *   "result": { "item": "...", "count": 1 }   // optional; EMPTY => поле опускается
 * }
 * }</pre>
 */
public class RadGenRecipeBuilder extends BaseRecipeBuilder<RadGenRecipeBuilder> {

    private final Ingredient input;
    private final int power;
    private final int duration;
    private final ItemStack output;

    private RadGenRecipeBuilder(Ingredient input, int power, int duration, ItemStack output) {
        this.input = input;
        this.power = power;
        this.duration = duration;
        this.output = output;
    }

    public static RadGenRecipeBuilder radgenRecipe(Ingredient input, int power, int duration, ItemStack output) {
        return new RadGenRecipeBuilder(input, power, duration, output);
    }

    @Override
    public Item getResult() {
        // EMPTY-выход (scrap) не имеет предмета — возвращаем AIR как нейтральный placeholder.
        return this.output.isEmpty() ? net.minecraft.world.item.Items.AIR : this.output.getItem();
    }

    @Override
    protected void serializeRecipeData(JsonObject json) {
        json.add("ingredient", input.toJson());
        json.addProperty("power", power);
        json.addProperty("duration", duration);
        // result опускается для «scrap»-рецептов без выхода (readJson трактует отсутствие как EMPTY).
        if (!output.isEmpty()) {
            json.add("result", stackToJson(output));
        }
    }

    @Override
    protected RecipeSerializer<?> getType() {
        return RadGenRecipe.Serializer.INSTANCE;
    }
}
//?}
