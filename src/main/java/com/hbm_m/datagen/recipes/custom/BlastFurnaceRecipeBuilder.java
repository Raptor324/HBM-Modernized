package com.hbm_m.datagen.recipes.custom;

// Билдер рецептов для BlastFurnaceRecipe.
// Позволяет быстро описывать двухкомпонентные рецепты доменной печи и генерирует корректный JSON.

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.hbm_m.recipe.BlastFurnaceRecipe;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.CriterionTriggerInstance;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.data.recipes.RecipeBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.ItemLike;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import java.util.function.Consumer;

public class BlastFurnaceRecipeBuilder implements RecipeBuilder {

    private final Ingredient inputA;
    private final Ingredient inputB;
    private final ItemStack output;
    private final Advancement.Builder advancement = Advancement.Builder.advancement();

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
    public RecipeBuilder unlockedBy(@Nonnull String criterionName, @Nonnull CriterionTriggerInstance trigger) {
        this.advancement.addCriterion(criterionName, trigger);
        return this;
    }

    @Override
    public RecipeBuilder group(@Nullable String groupName) {
        return this;
    }

    @Override
    public Item getResult() {
        return this.output.getItem();
    }

    @Override
    public void save(@Nonnull Consumer<FinishedRecipe> consumer, @Nonnull ResourceLocation recipeId) {
        consumer.accept(new Result(recipeId, this));
    }

    private static class Result implements FinishedRecipe {

        private final ResourceLocation id;
        private final BlastFurnaceRecipeBuilder builder;

        private Result(ResourceLocation id, BlastFurnaceRecipeBuilder builder) {
            this.id = id;
            this.builder = builder;
        }

        @Override
        public void serializeRecipeData(@Nonnull JsonObject json) {
            JsonArray ingredients = new JsonArray();
            ingredients.add(builder.inputA.toJson());
            ingredients.add(builder.inputB.toJson());
            json.add("ingredients", ingredients);

            JsonObject outputObject = new JsonObject();
            outputObject.addProperty("item", ForgeRegistries.ITEMS.getKey(builder.output.getItem()).toString());
            if (builder.output.getCount() > 1) {
                outputObject.addProperty("count", builder.output.getCount());
            }
            json.add("output", outputObject);
        }

        @Override
        public ResourceLocation getId() {
            return id;
        }

        @Override
        public RecipeSerializer<?> getType() {
            return BlastFurnaceRecipe.Serializer.INSTANCE;
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

