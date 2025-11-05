package com.hbm_m.recipe;

import com.google.gson.JsonObject;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

import java.util.List;

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

    @Override
    public boolean matches(Container container, Level level) {
        if (container.getContainerSize() < 2) return false;
        
        ItemStack slotA = container.getItem(0);
        ItemStack slotB = container.getItem(1);
        
        return ItemStack.isSameItemSameTags(slotA, inputA) &&
               ItemStack.isSameItemSameTags(slotB, inputB) &&
               slotA.getCount() >= inputA.getCount() &&
               slotB.getCount() >= inputB.getCount();
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

    public ItemStack getInputA() {
        return inputA;
    }

    public ItemStack getInputB() {
        return inputB;
    }

    public List<ItemStack> getRequiredItems() {
        return requiredItems;
    }

    public static class Type implements RecipeType<AnvilRecipe> {
        public static final Type INSTANCE = new Type();
        public static final String ID = "anvil";
    }

    public static class Serializer implements RecipeSerializer<AnvilRecipe> {
        public static final Serializer INSTANCE = new Serializer();

        @Override
        public AnvilRecipe fromJson(ResourceLocation id, JsonObject json) {
            // Реализуй десериализацию JSON здесь
            throw new UnsupportedOperationException("JSON serialization not implemented yet");
        }

        @Override
        public AnvilRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buffer) {
            ItemStack inputA = buffer.readItem();
            ItemStack inputB = buffer.readItem();
            ItemStack output = buffer.readItem();
            
            int size = buffer.readInt();
            List<ItemStack> requiredItems = new java.util.ArrayList<>();
            for (int i = 0; i < size; i++) {
                requiredItems.add(buffer.readItem());
            }
            
            return new AnvilRecipe(id, inputA, inputB, output, requiredItems);
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
