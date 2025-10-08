package com.hbm_m.recipe;

// Рецепт для Ассемблера - машины, которая собирает предметы из других предметов по рецептам.
// Отличается от стандартных рецептов Minecraft тем, что позволяет указывать количество каждого ингредиента

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.hbm_m.lib.RefStrings;

import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.StackedContents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;

public class AssemblerRecipe implements Recipe<SimpleContainer> {
    private final ResourceLocation id;
    private final ItemStack output;
    private final NonNullList<Ingredient> recipeItems;
    private final int duration;
    private final int powerConsumption;

    public AssemblerRecipe(ResourceLocation id, ItemStack output, NonNullList<Ingredient> recipeItems, int duration, int power) {
        this.id = id;
        this.output = output;
        this.recipeItems = recipeItems;
        this.duration = duration;
        this.powerConsumption = power;
    }

    @Override
    public boolean matches(@Nonnull SimpleContainer pContainer, @Nonnull Level pLevel) {
        if (pLevel.isClientSide()) {
            return false;
        }

        // Создаем счетчик предметов
        StackedContents stackedcontents = new StackedContents();

        // "Скармливаем" ему все предметы из входных слотов машины.
        // Он сам разберется с количеством предметов в стаках.
        for (int i = 0; i < pContainer.getContainerSize(); ++i) {
            ItemStack itemstack = pContainer.getItem(i);
            if (!itemstack.isEmpty()) {
                stackedcontents.accountStack(itemstack);
            }
        }
        
        // Единственная проверка, которая нам нужна.
        // canCraft сам проверит, можно ли из этого набора предметов
        // удовлетворить все ингредиенты рецепта.
        return stackedcontents.canCraft(this, null);
    }

    @Override
    public ItemStack assemble(@Nonnull SimpleContainer pContainer, @Nonnull RegistryAccess pRegistryAccess) {
        return output.copy();
    }

    @Override
    public boolean canCraftInDimensions(int pWidth, int pHeight) {
        return true;
    }

    @Override
    public ItemStack getResultItem(@Nonnull RegistryAccess pRegistryAccess) {
        return output.copy();
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        return this.recipeItems;
    }

    public int getDuration() { return this.duration; }
    public int getPowerConsumption() { return this.powerConsumption; }

    @Override
    public ResourceLocation getId() {
        return this.id;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return Serializer.INSTANCE;
    }

    @Override
    public RecipeType<?> getType() {
        return Type.INSTANCE;
    }

    // Вложенные классы для регистрации 
    public static class Type implements RecipeType<AssemblerRecipe> {
        public static final Type INSTANCE = new Type();
        public static final String ID = "assembler";
    }

    public static class Serializer implements RecipeSerializer<AssemblerRecipe> {
        public static final Serializer INSTANCE = new Serializer();
        public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "assembler");

        @Override
        public AssemblerRecipe fromJson(@Nonnull ResourceLocation pRecipeId, @Nonnull JsonObject pSerializedRecipe) {
            ItemStack output = ShapedRecipe.itemStackFromJson(GsonHelper.getAsJsonObject(pSerializedRecipe, "output"));
            
            JsonArray ingredientsJson = GsonHelper.getAsJsonArray(pSerializedRecipe, "ingredients");
            // Используем ArrayList, так как мы не знаем итоговый размер списка заранее
            NonNullList<Ingredient> inputs = NonNullList.create(); 

            // Проходимся по каждому объекту в массиве ингредиентов
            for (int i = 0; i < ingredientsJson.size(); i++) {
                JsonObject ingredientObject = ingredientsJson.get(i).getAsJsonObject();
                
                // 1. Читаем сам ингредиент (как и раньше)
                Ingredient ingredient = Ingredient.fromJson(ingredientObject);
                
                // 2. Читаем наше кастомное поле "count". Если его нет, по умолчанию берем 1.
                int count = GsonHelper.getAsInt(ingredientObject, "count", 1);
                
                // 3. Добавляем ингредиент в список 'count' раз.
                for (int j = 0; j < count; j++) {
                    inputs.add(ingredient);
                }
            }

            int duration = GsonHelper.getAsInt(pSerializedRecipe, "duration", 100);
            int power = GsonHelper.getAsInt(pSerializedRecipe, "power", 1000);

            return new AssemblerRecipe(pRecipeId, output, inputs, duration, power);
        }

        @Override
        public @Nullable AssemblerRecipe fromNetwork(@Nonnull ResourceLocation pRecipeId, @Nonnull FriendlyByteBuf pBuffer) {
            // Эта часть тоже должна быть исправлена для консистентности,
            // хотя она меньше используется в синглплеере.
            int ingredientCount = pBuffer.readVarInt();
            NonNullList<Ingredient> inputs = NonNullList.withSize(ingredientCount, Ingredient.EMPTY);
            for(int i = 0; i < ingredientCount; i++) {
                inputs.set(i, Ingredient.fromNetwork(pBuffer));
            }
            ItemStack output = pBuffer.readItem();
            int duration = pBuffer.readInt();
            int power = pBuffer.readInt();
            return new AssemblerRecipe(pRecipeId, output, inputs, duration, power);
        }

        @Override
        public void toNetwork(@Nonnull FriendlyByteBuf pBuffer, @Nonnull AssemblerRecipe pRecipe) {
            // Теперь мы просто записываем размер и сами ингредиенты
            pBuffer.writeVarInt(pRecipe.recipeItems.size());
            for (Ingredient ing : pRecipe.recipeItems) {
                ing.toNetwork(pBuffer);
            }
            pBuffer.writeItem(pRecipe.getResultItem(null));
            pBuffer.writeInt(pRecipe.getDuration());
            pBuffer.writeInt(pRecipe.getPowerConsumption());
        }
    }
}