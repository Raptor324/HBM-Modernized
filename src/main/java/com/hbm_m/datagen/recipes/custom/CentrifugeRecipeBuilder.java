package com.hbm_m.datagen.recipes.custom;
//? if forge {
import java.util.Arrays;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.hbm_m.recipe.CentrifugeRecipe;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;

public class CentrifugeRecipeBuilder extends BaseRecipeBuilder<CentrifugeRecipeBuilder> {
    private final Ingredient input;
    private final ItemStack[] outputs;

    private CentrifugeRecipeBuilder(Ingredient input, ItemStack[] outputs) {
        this.input = input;
        this.outputs = outputs;
    }

    public static CentrifugeRecipeBuilder tagRecipe(String tag, ItemStack... outputs) {
        return new CentrifugeRecipeBuilder(Ingredient.of(net.minecraft.tags.TagKey.create(
                net.minecraft.core.registries.Registries.ITEM,
                ResourceLocation.parse(tag))), padOutputs(outputs));
    }

    public static CentrifugeRecipeBuilder itemRecipe(Item item, ItemStack... outputs) {
        return new CentrifugeRecipeBuilder(Ingredient.of(item), padOutputs(outputs));
    }

    private static ItemStack[] padOutputs(ItemStack[] outputs) {
        ItemStack[] padded = new ItemStack[4];
        for (int i = 0; i < 4; i++) {
            padded[i] = i < outputs.length && outputs[i] != null ? outputs[i] : ItemStack.EMPTY;
        }
        return padded;
    }

    @Override
    public Item getResult() {
        return Arrays.stream(outputs)
                .filter(stack -> stack != null && !stack.isEmpty())
                .findFirst()
                .map(ItemStack::getItem)
                .orElse(net.minecraft.world.item.Items.AIR);
    }

    @Override
    protected void serializeRecipeData(JsonObject json) {
        json.add("ingredient", this.input.toJson());
        JsonArray results = new JsonArray();
        for (ItemStack stack : this.outputs) {
            if (stack == null || stack.isEmpty()) continue;
            // Унифицированная сериализация ItemStack (через BaseRecipeBuilder.stackToJson).
            results.add(stackToJson(stack));
        }
        json.add("results", results);
    }

    @Override
    protected RecipeSerializer<?> getType() {
        return CentrifugeRecipe.Serializer.INSTANCE;
    }
}
//?}