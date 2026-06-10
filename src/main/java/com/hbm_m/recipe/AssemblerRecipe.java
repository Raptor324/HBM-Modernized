package com.hbm_m.recipe;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

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
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.level.Level;

public class AssemblerRecipe implements Recipe<SimpleContainer> {
    private final ResourceLocation id;
    private final ItemStack output;
    private final NonNullList<Ingredient> recipeItems;
    /** Logical input slots for JEI / NEI-style display (ingredient + count per JSON entry). */
    private final List<AssemblerInputSlot> inputDisplaySlots;
    private final int duration;
    private final int powerConsumption;
    
    // НОВОЕ: Поддержка blueprint pool
    @Nullable
    private final String blueprintPool;

    public AssemblerRecipe(ResourceLocation id, ItemStack output, NonNullList<Ingredient> recipeItems,
                           int duration, int power) {
        this(id, output, recipeItems, null, duration, power, null);
    }

    public AssemblerRecipe(ResourceLocation id, ItemStack output, NonNullList<Ingredient> recipeItems,
                           int duration, int power, @Nullable String blueprintPool) {
        this(id, output, recipeItems, null, duration, power, blueprintPool);
    }

    public AssemblerRecipe(ResourceLocation id, ItemStack output, NonNullList<Ingredient> recipeItems,
                           @Nullable List<AssemblerInputSlot> inputDisplaySlots, int duration, int power,
                           @Nullable String blueprintPool) {
        this.id = id;
        this.output = output;
        this.recipeItems = recipeItems;
        this.inputDisplaySlots = inputDisplaySlots != null
                ? List.copyOf(inputDisplaySlots)
                : List.of();
        this.duration = duration;
        this.powerConsumption = power;
        this.blueprintPool = blueprintPool;
    }

    public record AssemblerInputSlot(Ingredient ingredient, int count) {
    }

    @Override
    public boolean matches(@NotNull SimpleContainer pContainer, @NotNull Level pLevel) {
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
    public ItemStack assemble(@NotNull SimpleContainer pContainer, @NotNull RegistryAccess pRegistryAccess) {
        return output.copy();
    }

    @Override
    public boolean canCraftInDimensions(int pWidth, int pHeight) {
        return true;
    }

    @Override
    public ItemStack getResultItem(@NotNull RegistryAccess pRegistryAccess) {
        return output.copy();
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        return this.recipeItems;
    }

    public List<AssemblerInputSlot> getInputDisplaySlots() {
        return inputDisplaySlots;
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
        //? if fabric && < 1.21.1 {
        /*public static final ResourceLocation ID = new ResourceLocation(RefStrings.MODID, "assembler");
        *///?} else {
                public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "assembler");
        //?}


        @Override
        public AssemblerRecipe fromJson(@NotNull ResourceLocation pRecipeId, @NotNull JsonObject pSerializedRecipe) {
            ItemStack output = ShapedRecipe.itemStackFromJson(GsonHelper.getAsJsonObject(pSerializedRecipe, "output"));
            JsonArray ingredientsJson = GsonHelper.getAsJsonArray(pSerializedRecipe, "ingredients");
            
            NonNullList<Ingredient> inputs = NonNullList.create();
            List<AssemblerInputSlot> displaySlots = new ArrayList<>();
            for (int i = 0; i < ingredientsJson.size(); i++) {
                JsonObject ingredientObject = ingredientsJson.get(i).getAsJsonObject();
                Ingredient ingredient = Ingredient.fromJson(ingredientObject);
                int count = GsonHelper.getAsInt(ingredientObject, "count", 1);
                for (int j = 0; j < count; j++) {
                    inputs.add(ingredient);
                }
                displaySlots.add(new AssemblerInputSlot(ingredient, count));
            }

            int duration = GsonHelper.getAsInt(pSerializedRecipe, "duration", 100);
            int power = GsonHelper.getAsInt(pSerializedRecipe, "power", 1000);
            
            String blueprintPool = GsonHelper.getAsString(pSerializedRecipe, "blueprint_pool", null);
            
            return new AssemblerRecipe(pRecipeId, output, inputs, displaySlots, duration, power, blueprintPool);
        }

        @Override
        public @Nullable AssemblerRecipe fromNetwork(@NotNull ResourceLocation pRecipeId, @NotNull FriendlyByteBuf pBuffer) {
            int displayCount = pBuffer.readVarInt();
            List<AssemblerInputSlot> displaySlots = new ArrayList<>(displayCount);
            NonNullList<Ingredient> inputs = NonNullList.create();
            for (int i = 0; i < displayCount; i++) {
                Ingredient ingredient = Ingredient.fromNetwork(pBuffer);
                int count = pBuffer.readVarInt();
                displaySlots.add(new AssemblerInputSlot(ingredient, count));
                for (int j = 0; j < count; j++) {
                    inputs.add(ingredient);
                }
            }

            ItemStack output = pBuffer.readItem();
            int duration = pBuffer.readInt();
            int power = pBuffer.readInt();
            
            String blueprintPool = pBuffer.readBoolean() ? pBuffer.readUtf() : null;
            
            return new AssemblerRecipe(pRecipeId, output, inputs, displaySlots, duration, power, blueprintPool);
        }

        @Override
        public void toNetwork(@NotNull FriendlyByteBuf pBuffer, @NotNull AssemblerRecipe pRecipe) {
            pBuffer.writeVarInt(pRecipe.inputDisplaySlots.size());
            for (AssemblerInputSlot slot : pRecipe.inputDisplaySlots) {
                slot.ingredient().toNetwork(pBuffer);
                pBuffer.writeVarInt(slot.count());
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
