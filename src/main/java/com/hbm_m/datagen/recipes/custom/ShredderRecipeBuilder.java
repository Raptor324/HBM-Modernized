package com.hbm_m.datagen.recipes.custom;
//? if forge {
import com.google.gson.JsonObject;
import com.hbm_m.recipe.ShredderRecipe;

import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;

public class ShredderRecipeBuilder extends BaseRecipeBuilder<ShredderRecipeBuilder> {

    private final Ingredient input;
    private final ItemStack output;

    private ShredderRecipeBuilder(Ingredient input, ItemStack output) {
        this.input = input;
        this.output = output;
    }

    public static ShredderRecipeBuilder shredderRecipe(Ingredient input, ItemStack output) {
        return new ShredderRecipeBuilder(input, output);
    }

    public static ShredderRecipeBuilder shredderRecipe(Item input, ItemStack output) {
        return new ShredderRecipeBuilder(Ingredient.of(input), output);
    }

    public static ShredderRecipeBuilder shredderRecipe(Item input, Item output, int count) {
        return new ShredderRecipeBuilder(Ingredient.of(input), new ItemStack(output, count));
    }

    /**
     * Ленивый вариант: вход и выход передаются как {@link RegistrySupplier} (а не {@code .get()}).
     * {@code .get()} вызывается только здесь, в момент постройки рецепта; в datagen-время регистры
     * уже заполнены, поэтому это безопасно даже на 1.21.1-neoforge (предмет resolвится один раз
     * на стороне билдера, а не остаётся «голым» в коде генератора).
     */
    public static ShredderRecipeBuilder shredderRecipe(RegistrySupplier<Item> input, RegistrySupplier<Item> output, int count) {
        return new ShredderRecipeBuilder(Ingredient.of(input.get()), new ItemStack(output.get(), count));
    }

    /** Ленивый выход: {@code output.get()} resolutions задержан до момента постройки датагеном JSON. */
    public static ShredderRecipeBuilder shredderRecipe(Item input, RegistrySupplier<Item> output, int count) {
        return new ShredderRecipeBuilder(Ingredient.of(input), new ItemStack(output.get(), count));
    }

    /** Ленивый выход с одной единицей (короткая форма для {@code shredder_recipe(item -> 1 powder)}). */
    public static ShredderRecipeBuilder shredderRecipe(Item input, RegistrySupplier<Item> output) {
        return shredderRecipe(input, output, 1);
    }

    @Override
    public Item getResult() {
        return this.output.getItem();
    }

    @Override
    protected void serializeRecipeData(JsonObject json) {
        json.add("ingredient", this.input.toJson());
        // Унифицированная сериализация ItemStack -> { "item", "count"? } (count только если > 1).
        json.add("result", stackToJson(this.output));
    }

    @Override
    protected RecipeSerializer<?> getType() {
        return ShredderRecipe.Serializer.INSTANCE;
    }
}
//?}