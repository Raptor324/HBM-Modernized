package com.hbm_m.recipe;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.GsonHelper;

import java.util.List;
import java.util.ArrayList;

public class AnvilRecipe implements Recipe<Container> {
    private final ResourceLocation id;
    private final ItemStack inputA;
    private final ItemStack inputB;
    private final ItemStack output;
    private final List<ItemStack> requiredItems;

    public AnvilRecipe(ResourceLocation id, ItemStack inputA, ItemStack inputB, ItemStack output, List<ItemStack> requiredItems) {
        this.id = id;
        this.inputA = inputA;
        this.inputB = inputB;
        this.output = output;
        this.requiredItems = requiredItems;
    }

    public ItemStack getInputA() {
        return inputA;
    }

    public ItemStack getInputB() {
        return inputB;
    }

    public ItemStack getOutput() {
        return output;
    }

    public List<ItemStack> getRequiredItems() {
        return requiredItems;
    }

    @Override
    public boolean matches(Container container, Level level) {
        if (level.isClientSide()) {
            return false;
        }

        ItemStack slotA = container.getItem(0);
        ItemStack slotB = container.getItem(1);

        return ItemStack.isSameItemSameTags(slotA, inputA) && slotA.getCount() >= inputA.getCount() &&
                ItemStack.isSameItemSameTags(slotB, inputB) && slotB.getCount() >= inputB.getCount();
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
        return output;
    }

    @Override
    public ResourceLocation getId() {
        return id;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.ANVIL_RECIPE_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return ModRecipes.ANVIL_RECIPE_TYPE.get();
    }

    // Класс сериализатора
    public static class AnvilRecipeSerializer implements RecipeSerializer<AnvilRecipe> {

        @Override
        public AnvilRecipe fromJson(ResourceLocation recipeId, JsonObject json) {
            ItemStack inputA = itemStackFromJson(GsonHelper.getAsJsonObject(json, "inputA"));
            ItemStack inputB = itemStackFromJson(GsonHelper.getAsJsonObject(json, "inputB"));
            ItemStack output = itemStackFromJson(GsonHelper.getAsJsonObject(json, "output"));

            List<ItemStack> requiredItems = new ArrayList<>();
            if (json.has("required")) {
                JsonArray requiredArray = GsonHelper.getAsJsonArray(json, "required");
                for (int i = 0; i < requiredArray.size(); i++) {
                    requiredItems.add(itemStackFromJson(requiredArray.get(i).getAsJsonObject()));
                }
            }

            return new AnvilRecipe(recipeId, inputA, inputB, output, requiredItems);
        }

        private ItemStack itemStackFromJson(JsonObject json) {
            String itemId = GsonHelper.getAsString(json, "item");
            int count = GsonHelper.getAsInt(json, "count", 1);

            ItemStack stack = new ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.get(new ResourceLocation(itemId)));
            stack.setCount(count);
            return stack;
        }

        @Override
        public AnvilRecipe fromNetwork(ResourceLocation recipeId, FriendlyByteBuf buffer) {
            ItemStack inputA = buffer.readItem();
            ItemStack inputB = buffer.readItem();
            ItemStack output = buffer.readItem();

            int requiredSize = buffer.readInt();
            List<ItemStack> requiredItems = new ArrayList<>();
            for (int i = 0; i < requiredSize; i++) {
                requiredItems.add(buffer.readItem());
            }

            return new AnvilRecipe(recipeId, inputA, inputB, output, requiredItems);
        }

        @Override
        public void toNetwork(FriendlyByteBuf buffer, AnvilRecipe recipe) {
            buffer.writeItem(recipe.inputA);
            buffer.writeItem(recipe.inputB);
            buffer.writeItem(recipe.output);

            buffer.writeInt(recipe.requiredItems.size());
            for (ItemStack item : recipe.requiredItems) {
                buffer.writeItem(item);
            }
        }
    }
}


