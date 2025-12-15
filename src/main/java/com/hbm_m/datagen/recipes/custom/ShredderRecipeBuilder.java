package com.hbm_m.datagen.recipes.custom;

// Билдер рецептов для ShredderRecipe.
// Позволяет легко создавать рецепты для шреддера (один вход -> один выход).
// Используется в классе генерации данных ModRecipeProvider.
import com.google.gson.JsonObject;
import com.hbm_m.recipe.ShredderRecipe;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.CriterionTriggerInstance;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.data.recipes.RecipeBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import java.util.function.Consumer;

public class ShredderRecipeBuilder implements RecipeBuilder {
    private final Ingredient input;
    private final ItemStack output;
    private final Advancement.Builder advancement = Advancement.Builder.advancement();

    private ShredderRecipeBuilder(Ingredient input, ItemStack output) {
        this.input = input;
        this.output = output;
    }

    /**
     * Создает новый билдер рецепта для шреддера.
     * @param input Ингредиент (вход)
     * @param output Результат (выход)
     */
    public static ShredderRecipeBuilder shredderRecipe(Ingredient input, ItemStack output) {
        return new ShredderRecipeBuilder(input, output);
    }

    /**
     * Удобный метод для создания рецепта с одним предметом на входе.
     */
    public static ShredderRecipeBuilder shredderRecipe(Item input, ItemStack output) {
        return new ShredderRecipeBuilder(Ingredient.of(input), output);
    }

    /**
     * Удобный метод для создания рецепта с одним предметом на входе и выходе.
     */
    public static ShredderRecipeBuilder shredderRecipe(Item input, Item output, int count) {
        return new ShredderRecipeBuilder(Ingredient.of(input), new ItemStack(output, count));
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
        pFinishedRecipeConsumer.accept(new Result(pRecipeId, this));
    }

    // Внутренний класс для сериализации
    private static class Result implements FinishedRecipe {

        private final ResourceLocation id;
        private final ShredderRecipeBuilder builder;

        public Result(ResourceLocation id, ShredderRecipeBuilder builder) {
            this.id = id;
            this.builder = builder;
        }

        @Override
        public void serializeRecipeData(@Nonnull JsonObject pJson) {
            // Сериализуем входной ингредиент
            pJson.add("ingredient", this.builder.input.toJson());

            // Сериализуем выходной предмет
            JsonObject resultJson = new JsonObject();
            resultJson.addProperty("item", ForgeRegistries.ITEMS.getKey(this.builder.output.getItem()).toString());
            if (this.builder.output.getCount() > 1) {
                resultJson.addProperty("count", this.builder.output.getCount());
            }
            pJson.add("result", resultJson);
        }

        @Override
        public ResourceLocation getId() {
            return this.id;
        }

        @Override
        public RecipeSerializer<?> getType() {
            return ShredderRecipe.Serializer.INSTANCE;
        }

        @Nullable
        @Override
        public JsonObject serializeAdvancement() {
            // Всегда возвращаем null, так как мы не хотим генерировать ачивку
            return null;
        }

        @Nullable
        @Override
        public ResourceLocation getAdvancementId() {
            // Всегда возвращаем null
            return null;
        }
    }
}

