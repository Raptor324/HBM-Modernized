package com.hbm_m.datagen.recipes.custom;
//? if forge {
import java.util.Objects;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.hbm_m.recipe.PressRecipe;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.ItemLike;

/**
 * Builder for {@link PressRecipe} data generation.
 */
public class PressRecipeBuilder extends BaseRecipeBuilder<PressRecipeBuilder> {

    private final ItemStack output;
    private JsonObject stampJson;
    private JsonObject materialJson;

    private PressRecipeBuilder(ItemStack output) {
        this.output = output;
    }

    public static PressRecipeBuilder pressRecipe(ItemStack output) {
        return new PressRecipeBuilder(output);
    }

    public PressRecipeBuilder stamp(Ingredient ingredient) {
        this.stampJson = ingredient.toJson().getAsJsonObject();
        return this;
    }

    public PressRecipeBuilder stamp(TagKey<Item> tag) {
        this.stampJson = tagJson(tag);
        return this;
    }

    public PressRecipeBuilder stamp(ItemLike item) {
        this.stampJson = itemJson(Objects.requireNonNull(
                BuiltInRegistries.ITEM.getKey(item.asItem()), "Item is not registered"));
        return this;
    }

    public PressRecipeBuilder stamp(ResourceLocation itemId) {
        this.stampJson = itemJson(itemId);
        return this;
    }

    public PressRecipeBuilder material(Ingredient ingredient) {
        this.materialJson = ingredient.toJson().getAsJsonObject();
        return this;
    }

    public PressRecipeBuilder material(TagKey<Item> tag) {
        this.materialJson = tagJson(tag);
        return this;
    }

    public PressRecipeBuilder material(ItemLike item) {
        this.materialJson = itemJson(Objects.requireNonNull(
                BuiltInRegistries.ITEM.getKey(item.asItem()), "Item is not registered"));
        return this;
    }

    public PressRecipeBuilder material(ResourceLocation itemId) {
        this.materialJson = itemJson(itemId);
        return this;
    }

    private static JsonObject tagJson(TagKey<Item> tag) {
        JsonObject json = new JsonObject();
        json.addProperty("tag", tag.location().toString());
        return json;
    }

    private static JsonObject itemJson(ResourceLocation id) {
        JsonObject json = new JsonObject();
        json.addProperty("item", id.toString());
        return json;
    }

    @Override
    public Item getResult() {
        return this.output.getItem();
    }

    @Override
    protected void serializeRecipeData(JsonObject json) {
        if (this.stampJson == null) {
            throw new IllegalStateException("Stamp ingredient is not defined for press recipe");
        }
        if (this.materialJson == null) {
            throw new IllegalStateException("Material ingredient is not defined for press recipe");
        }

        JsonArray jsonIngredients = new JsonArray();
        jsonIngredients.add(this.stampJson.deepCopy());
        jsonIngredients.add(this.materialJson.deepCopy());
        json.add("ingredients", jsonIngredients);

        JsonObject jsonOutput = new JsonObject();
        jsonOutput.addProperty("item", Objects.requireNonNull(
                BuiltInRegistries.ITEM.getKey(this.output.getItem()),
                "Output item is not registered").toString());
        if (this.output.getCount() > 1) {
            jsonOutput.addProperty("count", this.output.getCount());
        }
        json.add("output", jsonOutput);
    }

    @Override
    protected RecipeSerializer<?> getType() {
        return PressRecipe.Serializer.INSTANCE;
    }
}
//?}