package com.hbm_m.recipe;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.hbm_m.lib.RefStrings;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * Centrifuge processing recipe.
 * One input -> up to 4 output stacks (fixed output slots in the machine).
 */
public class CentrifugeRecipe implements Recipe<Container> {

    /** Matches the machine input slot index in {@code MachineCentrifugeBlockEntity}. */
    private static final int INPUT_SLOT_INDEX = 1;

    private final ResourceLocation id;
    private final Ingredient input;
    private final NonNullList<ItemStack> outputs;

    public CentrifugeRecipe(ResourceLocation id, Ingredient input, NonNullList<ItemStack> outputs) {
        this.id = id;
        this.input = input;
        this.outputs = outputs;
    }

    @Override
    public boolean matches(Container container, Level level) {
        if (level.isClientSide()) {
            return false;
        }
        return input.test(container.getItem(INPUT_SLOT_INDEX));
    }

    @Override
    public ItemStack assemble(Container container, RegistryAccess registryAccess) {
        // Not used by our machine; JEI uses getResultItem as a representative output.
        return getResultItem(registryAccess);
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public ItemStack getResultItem(RegistryAccess registryAccess) {
        for (ItemStack out : outputs) {
            if (!out.isEmpty()) {
                return out.copy();
            }
        }
        return ItemStack.EMPTY;
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        NonNullList<Ingredient> list = NonNullList.create();
        list.add(input);
        return list;
    }

    public Ingredient getInput() {
        return input;
    }

    public NonNullList<ItemStack> getOutputs() {
        NonNullList<ItemStack> out = NonNullList.withSize(outputs.size(), ItemStack.EMPTY);
        for (int i = 0; i < outputs.size(); i++) {
            out.set(i, outputs.get(i).copy());
        }
        return out;
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

    public static class Type implements RecipeType<CentrifugeRecipe> {
        public static final Type INSTANCE = new Type();
        public static final String ID = "centrifuge";
    }

    public static class Serializer implements RecipeSerializer<CentrifugeRecipe> {
        public static final Serializer INSTANCE = new Serializer();
        public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "centrifuge");

        private static final int MAX_OUTPUTS = 4;

        @Override
        public CentrifugeRecipe fromJson(ResourceLocation recipeId, JsonObject json) {
            Ingredient input = Ingredient.fromJson(GsonHelper.getAsJsonObject(json, "ingredient"));

            NonNullList<ItemStack> outputs = NonNullList.withSize(MAX_OUTPUTS, ItemStack.EMPTY);
            JsonArray resultsJson = GsonHelper.getAsJsonArray(json, "results");
            int limit = Math.min(resultsJson.size(), MAX_OUTPUTS);
            for (int i = 0; i < limit; i++) {
                JsonElement element = resultsJson.get(i);
                if (!element.isJsonObject()) continue;
                outputs.set(i, ShapedRecipe.itemStackFromJson(element.getAsJsonObject()));
            }

            return new CentrifugeRecipe(recipeId, input, outputs);
        }

        @Override
        public @Nullable CentrifugeRecipe fromNetwork(ResourceLocation recipeId, FriendlyByteBuf buffer) {
            Ingredient input = Ingredient.fromNetwork(buffer);
            int size = buffer.readVarInt();
            NonNullList<ItemStack> outputs = NonNullList.withSize(size, ItemStack.EMPTY);
            for (int i = 0; i < size; i++) {
                outputs.set(i, buffer.readItem());
            }
            return new CentrifugeRecipe(recipeId, input, outputs);
        }

        @Override
        public void toNetwork(FriendlyByteBuf buffer, CentrifugeRecipe recipe) {
            recipe.input.toNetwork(buffer);

            buffer.writeVarInt(recipe.outputs.size());
            for (ItemStack out : recipe.outputs) {
                buffer.writeItem(out);
            }
        }
    }
}
