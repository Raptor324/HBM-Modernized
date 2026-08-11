package com.hbm_m.datagen.recipes.custom;
//? if forge {
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.hbm_m.recipe.BlastFurnaceRecipe;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.ItemLike;

public class BlastFurnaceRecipeBuilder extends BaseRecipeBuilder<BlastFurnaceRecipeBuilder> {
    private final Ingredient inputA;
    private final Ingredient inputB;
    private final ItemStack output;

    private BlastFurnaceRecipeBuilder(ItemStack output, Ingredient inputA, Ingredient inputB) {
        this.inputA = inputA;
        this.inputB = inputB;
        this.output = output;
    }

    public static BlastFurnaceRecipeBuilder blastFurnaceRecipe(ItemStack output, Ingredient inputA, Ingredient inputB) {
        return new BlastFurnaceRecipeBuilder(output, inputA, inputB);
    }

    public static BlastFurnaceRecipeBuilder blastFurnaceRecipe(ItemStack output, ItemLike inputA, ItemLike inputB) {
        return blastFurnaceRecipe(output, Ingredient.of(inputA), Ingredient.of(inputB));
    }

    @Override
    public Item getResult() {
        return this.output.getItem();
    }

    @Override
    protected void serializeRecipeData(JsonObject json) {
        JsonArray ingredients = new JsonArray();
        ingredients.add(this.inputA.toJson());
        ingredients.add(this.inputB.toJson());
        json.add("ingredients", ingredients);

        // Унифицированная сериализация ItemStack (через BaseRecipeBuilder.stackToJson).
        json.add("output", stackToJson(this.output));
    }

    @Override
    protected RecipeSerializer<?> getType() {
        return BlastFurnaceRecipe.Serializer.INSTANCE;
    }
}
//?}
