package com.hbm_m.datagen.recipes.custom;
//? if forge {
import java.util.Arrays;
import java.util.function.Consumer;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.hbm_m.recipe.CentrifugeRecipe;

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

public class CentrifugeRecipeBuilder implements RecipeBuilder {
    private final Ingredient input;
    private final ItemStack[] outputs;
    private final Advancement.Builder advancement = Advancement.Builder.advancement();

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
        return Arrays.stream(outputs)
                .filter(stack -> stack != null && !stack.isEmpty())
                .findFirst()
                .map(ItemStack::getItem)
                .orElse(net.minecraft.world.item.Items.AIR);
    }

    @Override
    public void save(@NotNull Consumer<FinishedRecipe> pFinishedRecipeConsumer, @NotNull ResourceLocation pRecipeId) {
        pFinishedRecipeConsumer.accept(new Result(pRecipeId, this));
    }

    private static class Result implements FinishedRecipe {
        private final ResourceLocation id;
        private final CentrifugeRecipeBuilder builder;

        Result(ResourceLocation id, CentrifugeRecipeBuilder builder) {
            this.id = id;
            this.builder = builder;
        }

        @Override
        public void serializeRecipeData(@NotNull JsonObject pJson) {
            pJson.add("ingredient", this.builder.input.toJson());
            JsonArray results = new JsonArray();
            for (ItemStack stack : this.builder.outputs) {
                if (stack == null || stack.isEmpty()) {
                    continue;
                }
                JsonObject entry = new JsonObject();
                entry.addProperty("item", BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
                if (stack.getCount() > 1) {
                    entry.addProperty("count", stack.getCount());
                }
                results.add(entry);
            }
            pJson.add("results", results);
        }

        @Override
        public ResourceLocation getId() {
            return this.id;
        }

        @Override
        public RecipeSerializer<?> getType() {
            return CentrifugeRecipe.Serializer.INSTANCE;
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
