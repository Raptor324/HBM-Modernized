package com.hbm_m.recipe;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.platform.recipe.PlatformRecipe;
import com.hbm_m.platform.recipe.PlatformRecipeSerializer;
import com.hbm_m.platform.recipe.RecipeHooks;
import com.hbm_m.platform.recipe.RecipeInputWrapper;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

/**
 * Datapack-facing centrifuge recipe shape ({@code hbm_m:centrifuge}).
 * Runtime machine logic still uses {@link CentrifugeRecipes}.
 */
public class CentrifugeRecipe extends PlatformRecipe {

    private final Ingredient input;
    private final ItemStack[] outputs;

    public CentrifugeRecipe(ResourceLocation id, Ingredient input, ItemStack[] outputs) {
        super(id);
        this.input = input;
        this.outputs = outputs;
    }

    @Override
    public boolean matchesRecipe(RecipeInputWrapper container, Level level) {
        return !level.isClientSide() && input.test(container.getItem(0));
    }

    @Override
    public ItemStack assembleSafe() {
        return outputs.length > 0 && outputs[0] != null ? outputs[0].copy() : ItemStack.EMPTY;
    }

    @Override
    public ItemStack getResultItemSafe() {
        return assembleSafe();
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return Serializer.INSTANCE;
    }

    @Override
    public RecipeType<?> getType() {
        return Type.INSTANCE;
    }

    public Ingredient getInput() {
        return input;
    }

    public ItemStack[] getOutputs() {
        ItemStack[] copy = new ItemStack[outputs.length];
        for (int i = 0; i < outputs.length; i++) {
            copy[i] = outputs[i] != null ? outputs[i].copy() : ItemStack.EMPTY;
        }
        return copy;
    }

    public static class Type implements RecipeType<CentrifugeRecipe> {
        public static final Type INSTANCE = new Type();
        public static final String ID = "centrifuge";
    }

    public static class Serializer extends PlatformRecipeSerializer<CentrifugeRecipe> {
        public static final Serializer INSTANCE = new Serializer();
        //? if fabric && < 1.21.1 {
        /*public static final ResourceLocation ID = new ResourceLocation(RefStrings.MODID, "centrifuge");
        *///?} else {
        public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "centrifuge");
        //?}

        @Override
        public CentrifugeRecipe readJson(ResourceLocation recipeId, JsonObject json) {
            Ingredient input = RecipeHooks.ingredientFromJson(GsonHelper.getAsJsonObject(json, "ingredient"));
            JsonArray results = GsonHelper.getAsJsonArray(json, "results");
            ItemStack[] outputs = new ItemStack[4];
            for (int i = 0; i < Math.min(results.size(), 4); i++) {
                JsonObject entry = results.get(i).getAsJsonObject();
                outputs[i] = new ItemStack(
                        GsonHelper.getAsItem(entry, "item"),
                        GsonHelper.getAsInt(entry, "count", 1)
                );
            }
            return new CentrifugeRecipe(recipeId, input, outputs);
        }

        @Override
        public CentrifugeRecipe readNetwork(ResourceLocation recipeId, FriendlyByteBuf buffer) {
            Ingredient input = RecipeHooks.readIngredient(buffer);
            ItemStack[] outputs = new ItemStack[4];
            for (int i = 0; i < 4; i++) {
                outputs[i] = RecipeHooks.readItem(buffer);
            }
            return new CentrifugeRecipe(recipeId, input, outputs);
        }

        @Override
        public void writeNetwork(FriendlyByteBuf buffer, CentrifugeRecipe recipe) {
            RecipeHooks.writeIngredient(buffer, recipe.input);
            for (ItemStack stack : recipe.outputs) {
                RecipeHooks.writeItem(buffer, stack != null ? stack : ItemStack.EMPTY);
            }
        }
    }
}
