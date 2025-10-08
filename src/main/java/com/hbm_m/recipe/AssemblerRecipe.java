package com.hbm_m.recipe;

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
    
    // НОВОЕ: Поддержка blueprint pool
    @Nullable
    private final String blueprintPool;

    public AssemblerRecipe(ResourceLocation id, ItemStack output, NonNullList<Ingredient> recipeItems, 
                           int duration, int power) {
        this(id, output, recipeItems, duration, power, null);
    }

    // НОВОЕ: Конструктор с blueprint pool
    public AssemblerRecipe(ResourceLocation id, ItemStack output, NonNullList<Ingredient> recipeItems, 
                           int duration, int power, @Nullable String blueprintPool) {
        this.id = id;
        this.output = output;
        this.recipeItems = recipeItems;
        this.duration = duration;
        this.powerConsumption = power;
        this.blueprintPool = blueprintPool;
    }

    @Override
    public boolean matches(@Nonnull SimpleContainer pContainer, @Nonnull Level pLevel) {
        if (pLevel.isClientSide()) {
            return false;
        }

        StackedContents stackedcontents = new StackedContents();
        for (int i = 0; i < pContainer.getContainerSize(); ++i) {
            ItemStack itemstack = pContainer.getItem(i);
            if (!itemstack.isEmpty()) {
                stackedcontents.accountStack(itemstack);
            }
        }
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
    
    // НОВОЕ: Getter для blueprint pool
    @Nullable
    public String getBlueprintPool() { 
        return this.blueprintPool; 
    }
    
    // НОВОЕ: Проверка, требует ли рецепт blueprint
    public boolean requiresBlueprint() {
        return this.blueprintPool != null && !this.blueprintPool.isEmpty();
    }

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
            
            NonNullList<Ingredient> inputs = NonNullList.create();
            for (int i = 0; i < ingredientsJson.size(); i++) {
                JsonObject ingredientObject = ingredientsJson.get(i).getAsJsonObject();
                Ingredient ingredient = Ingredient.fromJson(ingredientObject);
                int count = GsonHelper.getAsInt(ingredientObject, "count", 1);
                for (int j = 0; j < count; j++) {
                    inputs.add(ingredient);
                }
            }

            int duration = GsonHelper.getAsInt(pSerializedRecipe, "duration", 100);
            int power = GsonHelper.getAsInt(pSerializedRecipe, "power", 1000);
            
            // НОВОЕ: Читаем blueprint_pool (опционально)
            String blueprintPool = GsonHelper.getAsString(pSerializedRecipe, "blueprint_pool", null);
            
            return new AssemblerRecipe(pRecipeId, output, inputs, duration, power, blueprintPool);
        }

        @Override
        public @Nullable AssemblerRecipe fromNetwork(@Nonnull ResourceLocation pRecipeId, @Nonnull FriendlyByteBuf pBuffer) {
            int ingredientCount = pBuffer.readVarInt();
            NonNullList<Ingredient> inputs = NonNullList.withSize(ingredientCount, Ingredient.EMPTY);
            for(int i = 0; i < ingredientCount; i++) {
                inputs.set(i, Ingredient.fromNetwork(pBuffer));
            }

            ItemStack output = pBuffer.readItem();
            int duration = pBuffer.readInt();
            int power = pBuffer.readInt();
            
            // НОВОЕ: Читаем blueprint_pool из сети
            String blueprintPool = pBuffer.readBoolean() ? pBuffer.readUtf() : null;
            
            return new AssemblerRecipe(pRecipeId, output, inputs, duration, power, blueprintPool);
        }

        @Override
        public void toNetwork(@Nonnull FriendlyByteBuf pBuffer, @Nonnull AssemblerRecipe pRecipe) {
            pBuffer.writeVarInt(pRecipe.recipeItems.size());
            for (Ingredient ing : pRecipe.recipeItems) {
                ing.toNetwork(pBuffer);
            }

            pBuffer.writeItem(pRecipe.getResultItem(null));
            pBuffer.writeInt(pRecipe.getDuration());
            pBuffer.writeInt(pRecipe.getPowerConsumption());
            
            // НОВОЕ: Записываем blueprint_pool в сеть
            if (pRecipe.blueprintPool != null) {
                pBuffer.writeBoolean(true);
                pBuffer.writeUtf(pRecipe.blueprintPool);
            } else {
                pBuffer.writeBoolean(false);
            }
        }
    }
}
