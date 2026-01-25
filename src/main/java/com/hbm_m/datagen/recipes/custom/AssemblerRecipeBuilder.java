package com.hbm_m.datagen.recipes.custom;

// Билдер рецептов для AssemblerRecipe с поддержкой количества ингредиентов.
// Позволяет легко создавать рецепты с несколькими ингредиентами, каждый из которых имеет свое количество.
// Используется в классе генерации данных ModRecipeProvider.
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.hbm_m.recipe.AssemblerRecipe;
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

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import javax.annotation.Nonnull;

public class AssemblerRecipeBuilder implements RecipeBuilder {
    private final ItemStack output;
    private final int duration;
    private final int power;
    private final List<CountableIngredient> ingredients = new ArrayList<>();
    private final Advancement.Builder advancement = Advancement.Builder.advancement();

    @Nullable
    private String blueprintPool = null;

    private AssemblerRecipeBuilder(ItemStack output, int duration, int power) {
        this.output = output;
        this.duration = duration;
        this.power = power;
    }

    public static AssemblerRecipeBuilder assemblerRecipe(ItemStack output, int duration, int power) {
        return new AssemblerRecipeBuilder(output, duration, power);
    }

    
    /**
     * Добавляет ингредиент с указанным количеством.
     */
    public AssemblerRecipeBuilder addIngredient(Ingredient ingredient, int count) {
        this.ingredients.add(new CountableIngredient(ingredient, count));
        return this;
    }
    
    /**
     * Удобный метод для добавления ванильных предметов.
     */
    public AssemblerRecipeBuilder addIngredient(Item item, int count) {
        return addIngredient(Ingredient.of(item), count);
    }

    public AssemblerRecipeBuilder withBlueprintPool(String pool) {
        this.blueprintPool = pool;
        return this;
    }
    
    // Внутренний record для хранения пары "Ингредиент-Количество"
    private record CountableIngredient(Ingredient ingredient, int count) {}


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
        private final AssemblerRecipeBuilder builder;

        public Result(ResourceLocation id, AssemblerRecipeBuilder builder) {
            this.id = id;
            this.builder = builder;
        }
        

        @Override
        public void serializeRecipeData(@Nonnull JsonObject pJson) {
            JsonArray jsonIngredients = new JsonArray();

            for (CountableIngredient countableIng : this.builder.ingredients) {
                // Преобразуем наш Ingredient в JSON
                JsonObject ingredientJson = countableIng.ingredient().toJson().getAsJsonObject();
                // Добавляем к нему поле "count"
                ingredientJson.addProperty("count", countableIng.count());
                jsonIngredients.add(ingredientJson);
            }
            pJson.add("ingredients", jsonIngredients);

            JsonObject jsonOutput = new JsonObject();
            jsonOutput.addProperty("item", ForgeRegistries.ITEMS.getKey(this.builder.output.getItem()).toString());
            if (this.builder.output.getCount() > 1) {
                jsonOutput.addProperty("count", this.builder.output.getCount());
            }
            pJson.add("output", jsonOutput);

            pJson.addProperty("duration", this.builder.duration);
            pJson.addProperty("power", this.builder.power);

            if (this.builder.blueprintPool != null) {
                pJson.addProperty("blueprint_pool", this.builder.blueprintPool);
            }
        }

        @Override
        public ResourceLocation getId() {
            return this.id;
        }

        @Override
        public RecipeSerializer<?> getType() {
            return AssemblerRecipe.Serializer.INSTANCE;
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