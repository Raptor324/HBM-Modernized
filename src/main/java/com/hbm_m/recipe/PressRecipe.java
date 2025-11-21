package com.hbm_m.recipe;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

public class PressRecipe implements Recipe<SimpleContainer> {
    private static final Logger LOGGER = LogUtils.getLogger();

    private final NonNullList<Ingredient> inputItems;
    private final ItemStack output;
    private final ResourceLocation id;

    public PressRecipe(NonNullList<Ingredient> inputItems, ItemStack output, ResourceLocation id) {
        this.inputItems = inputItems;
        this.output = output;
        this.id = id;
    }

    @Override
    public boolean matches(SimpleContainer container, Level level) {
        if(level.isClientSide()) {
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

        // ОТЛАДКА
        // LOGGER.info("=== Press Recipe Check ===");
        // LOGGER.info("Recipe ID: {}", id);
        // LOGGER.info("Stamp in slot: {} ({})", stamp.getItem(), stamp.getDisplayName().getString());
        // LOGGER.info("Material in slot: {} ({})", material.getItem(), material.getDisplayName().getString());
        // LOGGER.info("Expected stamp ingredient: {}", inputItems.get(0).toJson());
        // LOGGER.info("Expected material ingredient: {}", inputItems.get(1).toJson());
        // LOGGER.info("Stamp matches: {}", stampMatches);
        // LOGGER.info("Material matches: {}", materialMatches);
        // LOGGER.info("Overall result: {}", stampMatches && materialMatches);
        // LOGGER.info("========================");

        return stampMatches && materialMatches;
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        return inputItems;
    }

    @Override
    public ItemStack assemble(SimpleContainer container, RegistryAccess registryAccess) {
        return output.copy();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public @NotNull ItemStack getResultItem(RegistryAccess registryAccess) {
        return output.copy();
    }

    @Override
    public ResourceLocation getId() {
        return id;
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

    public static class Serializer implements RecipeSerializer<PressRecipe> {
        public static final Serializer INSTANCE = new Serializer();

        @Override
        public PressRecipe fromJson(ResourceLocation recipeId, JsonObject serializedRecipe) {
            ItemStack output = ShapedRecipe.itemStackFromJson(GsonHelper.getAsJsonObject(serializedRecipe, "output"));

            JsonArray ingredients = GsonHelper.getAsJsonArray(serializedRecipe, "ingredients");
            NonNullList<Ingredient> inputs = NonNullList.withSize(2, Ingredient.EMPTY);

            for(int i = 0; i < inputs.size() && i < ingredients.size(); i++) {
                inputs.set(i, Ingredient.fromJson(ingredients.get(i)));
            }

            return new PressRecipe(inputs, output, recipeId);
        }

        @Override
        public @Nullable PressRecipe fromNetwork(ResourceLocation recipeId, FriendlyByteBuf buffer) {
            NonNullList<Ingredient> inputs = NonNullList.withSize(buffer.readInt(), Ingredient.EMPTY);

            for(int i = 0; i < inputs.size(); i++) {
                inputs.set(i, Ingredient.fromNetwork(buffer));
            }

            ItemStack output = buffer.readItem();
            return new PressRecipe(inputs, output, recipeId);
        }

        @Override
        public void toNetwork(FriendlyByteBuf buffer, PressRecipe recipe) {
            buffer.writeInt(recipe.inputItems.size());

            for (Ingredient ingredient : recipe.getIngredients()) {
                ingredient.toNetwork(buffer);
            }

            buffer.writeItemStack(recipe.getResultItem(null), false);
        }
    }
}