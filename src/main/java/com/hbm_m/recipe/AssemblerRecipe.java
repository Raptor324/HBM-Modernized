package com.hbm_m.recipe;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.platform.recipe.PlatformRecipe;
import com.hbm_m.platform.recipe.PlatformRecipeSerializer;
import com.hbm_m.platform.recipe.RecipeHooks;
import com.hbm_m.platform.recipe.RecipeInputWrapper;

import net.minecraft.core.NonNullList;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.entity.player.StackedContents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

public class AssemblerRecipe extends PlatformRecipe {
    private final ItemStack output;
    private final NonNullList<Ingredient> recipeItems;
    private final List<AssemblerInputSlot> inputDisplaySlots;
    private final int duration;
    private final int powerConsumption;
    
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
        super(id);
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
    public boolean matchesRecipe(RecipeInputWrapper pContainer, Level pLevel) {
        if (pLevel.isClientSide()) {
            return false;
        }

        StackedContents stackedcontents = new StackedContents();
        for (int i = 0; i < pContainer.size(); ++i) {
            ItemStack itemstack = pContainer.getItem(i);
            if (!itemstack.isEmpty()) {
                stackedcontents.accountStack(itemstack);
            }
        }
        return stackedcontents.canCraft(this, null);
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
    public NonNullList<Ingredient> getIngredients() {
        return this.recipeItems;
    }

    public List<AssemblerInputSlot> getInputDisplaySlots() { return inputDisplaySlots; }
    public int getDuration() { return this.duration; }
    public int getPowerConsumption() { return this.powerConsumption; }
    @Nullable public String getBlueprintPool() { return this.blueprintPool; }
    
    public boolean requiresBlueprint() {
        return this.blueprintPool != null && !this.blueprintPool.isEmpty();
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

    public static class Serializer extends PlatformRecipeSerializer<AssemblerRecipe> {
        public static final Serializer INSTANCE = new Serializer();
        public static final ResourceLocation ID = ResourceLocation.tryParse(RefStrings.MODID + ":assembler");

        @Override
        public AssemblerRecipe readJson(ResourceLocation pRecipeId, JsonObject pSerializedRecipe) {
            ItemStack output = RecipeHooks.itemStackFromJson(GsonHelper.getAsJsonObject(pSerializedRecipe, "output"));
            JsonArray ingredientsJson = GsonHelper.getAsJsonArray(pSerializedRecipe, "ingredients");
            
            NonNullList<Ingredient> inputs = NonNullList.create();
            List<AssemblerInputSlot> displaySlots = new ArrayList<>();
            for (int i = 0; i < ingredientsJson.size(); i++) {
                AssemblerInputSlot slot = fromCountedIngredientJson(ingredientsJson.get(i).getAsJsonObject());
                for (int j = 0; j < slot.count(); j++) {
                    inputs.add(slot.ingredient());
                }
                displaySlots.add(slot);
            }

            int duration = GsonHelper.getAsInt(pSerializedRecipe, "duration", 100);
            int power = GsonHelper.getAsInt(pSerializedRecipe, "power", 1000);
            String blueprintPool = GsonHelper.getAsString(pSerializedRecipe, "blueprint_pool", null);
            
            return new AssemblerRecipe(pRecipeId, output, inputs, displaySlots, duration, power, blueprintPool);
        }

        @Override
        public AssemblerRecipe readNetwork(ResourceLocation pRecipeId, FriendlyByteBuf pBuffer) {
            int displayCount = pBuffer.readVarInt();
            List<AssemblerInputSlot> displaySlots = new ArrayList<>(displayCount);
            NonNullList<Ingredient> inputs = NonNullList.create();
            for (int i = 0; i < displayCount; i++) {
                Ingredient ingredient = RecipeHooks.readIngredient(pBuffer);
                int count = pBuffer.readVarInt();
                displaySlots.add(new AssemblerInputSlot(ingredient, count));
                for (int j = 0; j < count; j++) {
                    inputs.add(ingredient);
                }
            }

            ItemStack output = RecipeHooks.readItem(pBuffer);
            int duration = pBuffer.readInt();
            int power = pBuffer.readInt();
            
            String blueprintPool = pBuffer.readBoolean() ? pBuffer.readUtf() : null;
            
            return new AssemblerRecipe(pRecipeId, output, inputs, displaySlots, duration, power, blueprintPool);
        }

        @Override
        public void writeNetwork(FriendlyByteBuf pBuffer, AssemblerRecipe pRecipe) {
            pBuffer.writeVarInt(pRecipe.inputDisplaySlots.size());
            for (AssemblerInputSlot slot : pRecipe.inputDisplaySlots) {
                RecipeHooks.writeIngredient(pBuffer, slot.ingredient());
                pBuffer.writeVarInt(slot.count());
            }

            RecipeHooks.writeItem(pBuffer, pRecipe.getResultItemSafe());
            pBuffer.writeInt(pRecipe.getDuration());
            pBuffer.writeInt(pRecipe.getPowerConsumption());
            
            if (pRecipe.blueprintPool != null) {
                pBuffer.writeBoolean(true);
                pBuffer.writeUtf(pRecipe.blueprintPool);
            } else {
                pBuffer.writeBoolean(false);
            }
        }
    }

    public static JsonObject toCountedIngredientJson(Ingredient ingredient, int count) {
        JsonElement element = RecipeHooks.ingredientToJson(ingredient);
        JsonObject result;
        if (element.isJsonArray()) {
            result = new JsonObject();
            result.add("items", element);
        } else {
            result = element.getAsJsonObject().deepCopy();
        }
        result.addProperty("count", count);
        return result;
    }

    private static AssemblerInputSlot fromCountedIngredientJson(JsonObject ingredientObject) {
        int count = GsonHelper.getAsInt(ingredientObject, "count", 1);
        Ingredient ingredient;
        if (ingredientObject.has("items")) {
            ingredient = RecipeHooks.ingredientFromJson(ingredientObject.get("items"));
        } else {
            JsonObject clone = ingredientObject.deepCopy();
            clone.remove("count");
            ingredient = RecipeHooks.ingredientFromJson(clone);
        }
        return new AssemblerInputSlot(ingredient, count);
    }
}