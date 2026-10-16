package com.hbm_m.recipe;

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

public class ShredderRecipe extends PlatformRecipe {

    private final Ingredient input;
    private final ItemStack output;

    public ShredderRecipe(ResourceLocation id, Ingredient input, ItemStack output) {
        super(id);
        this.input = input;
        this.output = output;
    }

    @Override
    public boolean matchesRecipe(RecipeInputWrapper container, Level level) {
        if (level.isClientSide()) {
            return false;
        }
        return input.test(container.getItem(0));
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

    public Ingredient getInput() {
        return input;
    }

    public ItemStack getOutput() {
        return output.copy();
    }

    public static class Type implements RecipeType<ShredderRecipe> {
        public static final Type INSTANCE = new Type();
        public static final String ID = "shredding";
    }

    public static class Serializer extends PlatformRecipeSerializer<ShredderRecipe> {
        public static final Serializer INSTANCE = new Serializer();
        //? if fabric && < 1.21.1 {
        /*public static final ResourceLocation ID = new ResourceLocation(RefStrings.MODID, "shredding");
        *///?} else {
        public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "shredding");
        //?}

        @Override
        public ShredderRecipe readJson(ResourceLocation recipeId, JsonObject json) {
            Ingredient input = RecipeHooks.ingredientFromJson(GsonHelper.getAsJsonObject(json, "ingredient"));
            JsonObject result = GsonHelper.getAsJsonObject(json, "result");
            ItemStack output = new ItemStack(
                    GsonHelper.getAsItem(result, "item"),
                    GsonHelper.getAsInt(result, "count", 1)
            );

            return new ShredderRecipe(recipeId, input, output);
        }

        @Override
        public ShredderRecipe readNetwork(ResourceLocation recipeId, FriendlyByteBuf buffer) {
            Ingredient input = RecipeHooks.readIngredient(buffer);
            ItemStack output = RecipeHooks.readItem(buffer);
            return new ShredderRecipe(recipeId, input, output);
        }

        @Override
        public void writeNetwork(FriendlyByteBuf buffer, ShredderRecipe recipe) {
            RecipeHooks.writeIngredient(buffer, recipe.input);
            RecipeHooks.writeItem(buffer, recipe.output);
        }
    }
}
