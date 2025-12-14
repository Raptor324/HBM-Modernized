package com.hbm_m.datagen.recipes.custom;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.hbm_m.recipe.PressRecipe;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.CriterionTriggerInstance;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.data.recipes.RecipeBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.ItemLike;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Builder for {@link PressRecipe} data generation.
 * Mirrors the JSON structure used by the manual press recipe files but provides
 * the same ergonomic API style as {@link AssemblerRecipeBuilder}.
 */
public class PressRecipeBuilder implements RecipeBuilder {

    private final ItemStack output;
    private final Advancement.Builder advancement = Advancement.Builder.advancement();

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
                ForgeRegistries.ITEMS.getKey(item.asItem()), "Item is not registered"));
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
                ForgeRegistries.ITEMS.getKey(item.asItem()), "Item is not registered"));
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
    public RecipeBuilder unlockedBy(@Nonnull String pCriterionName, @Nonnull CriterionTriggerInstance pCriterionTrigger) {
        this.advancement.addCriterion(pCriterionName, pCriterionTrigger);
        return this;
    }

    @Override
    public RecipeBuilder group(@Nullable String pGroupName) {
        return this;
    }

    @Override
    public Item getResult() {
        return this.output.getItem();
    }

    @Override
    public void save(@Nonnull Consumer<FinishedRecipe> pFinishedRecipeConsumer, @Nonnull ResourceLocation pRecipeId) {
        if (this.stampJson == null) {
            throw new IllegalStateException("Stamp ingredient is not defined for press recipe " + pRecipeId);
        }
        if (this.materialJson == null) {
            throw new IllegalStateException("Material ingredient is not defined for press recipe " + pRecipeId);
        }
        pFinishedRecipeConsumer.accept(new Result(pRecipeId, this));
    }

    private static class Result implements FinishedRecipe {

        private final ResourceLocation id;
        private final PressRecipeBuilder builder;

        private Result(ResourceLocation id, PressRecipeBuilder builder) {
            this.id = id;
            this.builder = builder;
        }

        @Override
        public void serializeRecipeData(@Nonnull JsonObject pJson) {
            JsonArray jsonIngredients = new JsonArray();
            jsonIngredients.add(this.builder.stampJson.deepCopy());
            jsonIngredients.add(this.builder.materialJson.deepCopy());
            pJson.add("ingredients", jsonIngredients);

            JsonObject jsonOutput = new JsonObject();
            jsonOutput.addProperty("item", Objects.requireNonNull(
                    ForgeRegistries.ITEMS.getKey(this.builder.output.getItem()),
                    "Output item is not registered").toString());
            if (this.builder.output.getCount() > 1) {
                jsonOutput.addProperty("count", this.builder.output.getCount());
            }
            pJson.add("output", jsonOutput);
        }

        @Override
        public ResourceLocation getId() {
            return this.id;
        }

        @Override
        public RecipeSerializer<?> getType() {
            return PressRecipe.Serializer.INSTANCE;
        }

        @Nullable
        @Override
        public JsonObject serializeAdvancement() {
            return null;
        }

        @Nullable
        @Override
        public ResourceLocation getAdvancementId() {
            return null;
        }
    }
}

