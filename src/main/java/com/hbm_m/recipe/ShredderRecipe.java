package com.hbm_m.recipe;

import com.google.gson.JsonObject;
import com.hbm_m.lib.RefStrings;

import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraftforge.items.wrapper.RecipeWrapper;
import org.jetbrains.annotations.Nullable;

public class ShredderRecipe implements Recipe<Container> {

    private final ResourceLocation id;
    private final Ingredient input;
    private final ItemStack output;

    public ShredderRecipe(ResourceLocation id, Ingredient input, ItemStack output) {
        this.id = id;
        this.input = input;
        this.output = output;
    }

    @Override
    public boolean matches(Container container, Level level) {
        if (level.isClientSide()) {
            return false;
        }
        return input.test(container.getItem(0));
    }

    @Override
    public ItemStack assemble(Container container, RegistryAccess registryAccess) {
        return output.copy();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public ItemStack getResultItem(RegistryAccess registryAccess) {
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

    public static class Serializer implements RecipeSerializer<ShredderRecipe> {
        public static final Serializer INSTANCE = new Serializer();
        public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "shredding");

        @Override
        public ShredderRecipe fromJson(ResourceLocation recipeId, JsonObject json) {
            Ingredient input = Ingredient.fromJson(GsonHelper.getAsJsonObject(json, "ingredient"));
            JsonObject result = GsonHelper.getAsJsonObject(json, "result");
            ItemStack output = new ItemStack(
                    GsonHelper.getAsItem(result, "item"),
                    GsonHelper.getAsInt(result, "count", 1)
            );

            return new ShredderRecipe(recipeId, input, output);
        }

        @Override
        public @Nullable ShredderRecipe fromNetwork(ResourceLocation recipeId, FriendlyByteBuf buffer) {
            Ingredient input = Ingredient.fromNetwork(buffer);
            ItemStack output = buffer.readItem();
            return new ShredderRecipe(recipeId, input, output);
        }

        @Override
        public void toNetwork(FriendlyByteBuf buffer, ShredderRecipe recipe) {
            recipe.input.toNetwork(buffer);
            buffer.writeItem(recipe.output);
        }
    }
}
