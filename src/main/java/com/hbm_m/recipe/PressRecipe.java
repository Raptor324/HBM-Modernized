package com.hbm_m.recipe;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import com.hbm_m.platform.recipe.PlatformRecipe;
import com.hbm_m.platform.recipe.PlatformRecipeSerializer;
import com.hbm_m.platform.recipe.RecipeHooks;
import com.hbm_m.platform.recipe.RecipeInputWrapper;

import net.minecraft.core.NonNullList;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

public class PressRecipe extends PlatformRecipe {
    private static final Logger LOGGER = LogUtils.getLogger();

    private final NonNullList<Ingredient> inputItems;
    private final ItemStack output;

    public PressRecipe(NonNullList<Ingredient> inputItems, ItemStack output, ResourceLocation id) {
        super(id);
        this.inputItems = inputItems;
        this.output = output;
    }

    @Override
    public boolean matchesRecipe(RecipeInputWrapper container, Level level) {
        if (level.isClientSide()) {
            return false;
        }

        ItemStack stamp = container.getItem(1); // STAMP_SLOT
        ItemStack material = container.getItem(2); // MATERIAL_SLOT

        if (stamp.isEmpty() || material.isEmpty()) {
            return false;
        }

        // Проверяем что штамп и материал соответствуют рецепту
        boolean stampMatches = inputItems.get(0).test(stamp);
        boolean materialMatches = inputItems.get(1).test(material);

        return stampMatches && materialMatches;
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        return inputItems;
    }

    @Override
    public ItemStack assembleSafe() {
        return output.copy();
    }

    @Override
    public ItemStack getResultItemSafe() {
        return output.copy();
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return Serializer.INSTANCE;
    }

    @Override
    public RecipeType<?> getType() {
        return Type.INSTANCE;
    }

    public static class Type implements RecipeType<PressRecipe> {
        public static final Type INSTANCE = new Type();
        public static final String ID = "press";
    }

    public static class Serializer extends PlatformRecipeSerializer<PressRecipe> {
        public static final Serializer INSTANCE = new Serializer();

        @Override
        public PressRecipe readJson(ResourceLocation recipeId, JsonObject serializedRecipe) {
            ItemStack output = RecipeHooks.itemStackFromJson(GsonHelper.getAsJsonObject(serializedRecipe, "output"));

            JsonArray ingredients = GsonHelper.getAsJsonArray(serializedRecipe, "ingredients");
            NonNullList<Ingredient> inputs = NonNullList.withSize(2, Ingredient.EMPTY);

            for (int i = 0; i < inputs.size() && i < ingredients.size(); i++) {
                inputs.set(i, RecipeHooks.ingredientFromJson(ingredients.get(i)));
            }

            return new PressRecipe(inputs, output, recipeId);
        }

        @Override
        public PressRecipe readNetwork(ResourceLocation recipeId, FriendlyByteBuf buffer) {
            NonNullList<Ingredient> inputs = NonNullList.withSize(buffer.readVarInt(), Ingredient.EMPTY);

            for (int i = 0; i < inputs.size(); i++) {
                inputs.set(i, RecipeHooks.readIngredient(buffer));
            }

            ItemStack output = RecipeHooks.readItem(buffer);
            return new PressRecipe(inputs, output, recipeId);
        }

        @Override
        public void writeNetwork(FriendlyByteBuf buffer, PressRecipe recipe) {
            buffer.writeVarInt(recipe.getIngredients().size());

            for (Ingredient ingredient : recipe.getIngredients()) {
                RecipeHooks.writeIngredient(buffer, ingredient);
            }

            RecipeHooks.writeItem(buffer, recipe.getResultItemSafe());
        }
    }
}