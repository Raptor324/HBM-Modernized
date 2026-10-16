package com.hbm_m.datagen.recipes.custom;
//? if forge {
import java.util.Objects;
import java.util.function.Consumer;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.hbm_m.recipe.AmmoPressRecipe;

import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.CriterionTriggerInstance;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.data.recipes.RecipeBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.ItemLike;

/**
 * Builder for {@link AmmoPressRecipe} data generation - 9 feste, positionsgleiche 3x3-Slots
 * (Index 0-8, GUI-Reihenfolge zeilenweise) statt vanilla Pattern-Key-Syntax.
 */
public class AmmoPressRecipeBuilder implements RecipeBuilder {

    private final ItemStack output;
    private final JsonObject[] slots = new JsonObject[AmmoPressRecipe.GRID_SIZE];
    private final Advancement.Builder advancement = Advancement.Builder.advancement();

    private AmmoPressRecipeBuilder(ItemStack output) {
        this.output = output;
    }

    public static AmmoPressRecipeBuilder ammoPressRecipe(ItemStack output) {
        return new AmmoPressRecipeBuilder(output);
    }

    public AmmoPressRecipeBuilder slot(int index, ItemLike item) {
        slots[index] = itemJson(Objects.requireNonNull(
                BuiltInRegistries.ITEM.getKey(item.asItem()), "Item is not registered"));
        return this;
    }

    public AmmoPressRecipeBuilder slot(int index, Ingredient ingredient) {
        slots[index] = ingredient.toJson().getAsJsonObject();
        return this;
    }

    private static JsonObject itemJson(ResourceLocation id) {
        JsonObject json = new JsonObject();
        json.addProperty("item", id.toString());
        return json;
    }

    @Override
    public RecipeBuilder unlockedBy(@NotNull String pCriterionName, @NotNull CriterionTriggerInstance pCriterionTrigger) {
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
    public void save(@NotNull Consumer<FinishedRecipe> pFinishedRecipeConsumer, @NotNull ResourceLocation pRecipeId) {
        pFinishedRecipeConsumer.accept(new Result(pRecipeId, this));
    }

    /**
     * Overrides the vanilla {@code RecipeBuilder.save(Consumer, String)} default, which resolves a
     * bare path string to the {@code minecraft} namespace (via {@code new ResourceLocation(path)}).
     */
    @Override
    public void save(@NotNull Consumer<FinishedRecipe> pFinishedRecipeConsumer, @NotNull String pPath) {
        save(pFinishedRecipeConsumer, ResourceLocation.fromNamespaceAndPath("hbm_m", pPath));
    }

    private static class Result implements FinishedRecipe {

        private final ResourceLocation id;
        private final AmmoPressRecipeBuilder builder;

        private Result(ResourceLocation id, AmmoPressRecipeBuilder builder) {
            this.id = id;
            this.builder = builder;
        }

        @Override
        public void serializeRecipeData(@NotNull JsonObject pJson) {
            JsonArray jsonIngredients = new JsonArray();
            for (int i = 0; i < AmmoPressRecipe.GRID_SIZE; i++) {
                JsonObject slot = builder.slots[i];
                jsonIngredients.add(slot != null ? slot.deepCopy() : com.google.gson.JsonNull.INSTANCE);
            }
            pJson.add("ingredients", jsonIngredients);

            JsonObject jsonOutput = new JsonObject();
            jsonOutput.addProperty("item", Objects.requireNonNull(
                    BuiltInRegistries.ITEM.getKey(this.builder.output.getItem()),
                    "Output item is not registered").toString());
            if (this.builder.output.getCount() > 1) {
                jsonOutput.addProperty("count", this.builder.output.getCount());
            }
            pJson.add("result", jsonOutput);
        }

        @Override
        public ResourceLocation getId() {
            return this.id;
        }

        @Override
        public RecipeSerializer<?> getType() {
            return AmmoPressRecipe.Serializer.INSTANCE;
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
//?}
