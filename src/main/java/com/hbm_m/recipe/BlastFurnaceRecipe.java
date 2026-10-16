package com.hbm_m.recipe;

// Рецепт для Плавильной печи - машины, которая сплавляет два предмета в один.
// Обновлённая версия: длительность на рецепт (duration) и второй выход (шлак).

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
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
import org.jetbrains.annotations.NotNull;

public class BlastFurnaceRecipe extends PlatformRecipe {
    private final NonNullList<Ingredient> inputItems;
    private final ItemStack output;
    /** Второй выход (шлак); может быть пустым. */
    private final ItemStack secondaryOutput;
    /** Длительность плавки в тиках при скорости 1.0. */
    private final int duration;

    public BlastFurnaceRecipe(NonNullList<Ingredient> inputItems, ItemStack output, ItemStack secondaryOutput, int duration, ResourceLocation id) {
        super(id);
        this.inputItems = inputItems;
        this.output = output;
        this.secondaryOutput = secondaryOutput;
        this.duration = Math.max(1, duration);
    }

    @Override
    public boolean matchesRecipe(RecipeInputWrapper container, Level level) {
        // Order-independent matching через ванильный StackedContents (как в AssemblerRecipe).
        // Учитываем только слоты 1 и 2 (INPUT_SLOT_1/2); slot 0 — другой слот и не должен влиять.
        StackedContents stacked = new StackedContents();
        for (int i = 1; i <= 2 && i < container.size(); i++) {
            ItemStack stack = container.getItem(i);
            if (!stack.isEmpty()) {
                stacked.accountStack(stack);
            }
        }
        return stacked.canCraft(this, null);
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        return inputItems;
    }

    @Override
    public ItemStack assembleSafe() {
        return output.copy();
    }

    @Override
    public @NotNull ItemStack getResultItemSafe() {
        return output.copy();
    }

    public ItemStack getSecondaryOutputSafe() {
        return secondaryOutput.copy();
    }

    public boolean hasSecondaryOutput() {
        return !secondaryOutput.isEmpty();
    }

    public int getDuration() {
        return duration;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return Serializer.INSTANCE;
    }

    @Override
    public RecipeType<?> getType() {
        return Type.INSTANCE;
    }

    public static class Type implements RecipeType<BlastFurnaceRecipe> {
        public static final Type INSTANCE = new Type();
        public static final String ID = "blast_furnace";
    }

    public static class Serializer extends PlatformRecipeSerializer<BlastFurnaceRecipe> {
        public static final Serializer INSTANCE = new Serializer();


        @Override
        public BlastFurnaceRecipe readJson(ResourceLocation recipeId, JsonObject serializedRecipe) {
            ItemStack output = RecipeHooks.itemStackFromJson(GsonHelper.getAsJsonObject(serializedRecipe, "output"));

            ItemStack secondaryOutput = ItemStack.EMPTY;
            if (serializedRecipe.has("secondary_output") && serializedRecipe.get("secondary_output").isJsonObject()) {
                secondaryOutput = RecipeHooks.itemStackFromJson(serializedRecipe.getAsJsonObject("secondary_output"));
            }

            int duration = GsonHelper.getAsInt(serializedRecipe, "duration", 800);

            JsonArray ingredients = GsonHelper.getAsJsonArray(serializedRecipe, "ingredients");
            NonNullList<Ingredient> inputs = NonNullList.withSize(2, Ingredient.EMPTY);

            for (int i = 0; i < inputs.size() && i < ingredients.size(); i++) {
                inputs.set(i, RecipeHooks.ingredientFromJson(ingredients.get(i)));
            }

            return new BlastFurnaceRecipe(inputs, output, secondaryOutput, duration, recipeId);
        }

        @Override
        public BlastFurnaceRecipe readNetwork(ResourceLocation recipeId, FriendlyByteBuf buffer) {
            NonNullList<Ingredient> inputs = NonNullList.withSize(buffer.readVarInt(), Ingredient.EMPTY);

            for (int i = 0; i < inputs.size(); i++) {
                inputs.set(i, RecipeHooks.readIngredient(buffer));
            }

            ItemStack output = RecipeHooks.readItem(buffer);
            ItemStack secondaryOutput = RecipeHooks.readItem(buffer);
            int duration = buffer.readVarInt();
            return new BlastFurnaceRecipe(inputs, output, secondaryOutput, duration, recipeId);
        }

        @Override
        public void writeNetwork(FriendlyByteBuf buffer, BlastFurnaceRecipe recipe) {
            buffer.writeVarInt(recipe.getIngredients().size());

            for (Ingredient ingredient : recipe.getIngredients()) {
                RecipeHooks.writeIngredient(buffer, ingredient);
            }

            RecipeHooks.writeItem(buffer, recipe.getResultItemSafe());
            RecipeHooks.writeItem(buffer, recipe.getSecondaryOutputSafe());
            buffer.writeVarInt(recipe.getDuration());
        }
    }
}
