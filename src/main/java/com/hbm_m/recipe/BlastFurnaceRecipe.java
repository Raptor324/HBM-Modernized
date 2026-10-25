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
import org.jetbrains.annotations.Nullable;

public class BlastFurnaceRecipe extends PlatformRecipe {
    private final NonNullList<Ingredient> inputItems;
    private final ItemStack output;
    /** Второй выход (шлак); может быть пустым. */
    private final ItemStack secondaryOutput;
    /** Длительность плавки в тиках при скорости 1.0. */
    private final int duration;
    /** Mengen je Zutat (BlastFurnaceRecipesNT: z. B. 2 Eisenbarren + 1 Sand). */
    private int[] counts = {1, 1};
    /**
     * {@code true}: Rezept des neuen Hochofens (BlastFurnaceRecipesNT, mit Schlacke), {@code false}: Rezept des
     * Legierungsofens (BlastFurnaceRecipes). Im Original sind das zwei getrennte Rezeptlisten.
     */
    private boolean nt = false;

    public BlastFurnaceRecipe(NonNullList<Ingredient> inputItems, ItemStack output, ItemStack secondaryOutput, int duration, ResourceLocation id) {
        super(id);
        this.inputItems = inputItems;
        this.output = output;
        this.secondaryOutput = secondaryOutput;
        this.duration = Math.max(1, duration);
    }

    public BlastFurnaceRecipe withCounts(int[] counts, boolean nt) {
        this.counts = counts;
        this.nt = nt;
        return this;
    }

    public boolean isNT() { return nt; }

    public int getCount(int index) { return index < counts.length ? counts[index] : 1; }

    /**
     * Wie viel aus Slot 1 und 2 verbraucht wird (Zutaten in beliebiger Reihenfolge, wie getRecipe des
     * Originals); {@code null}, wenn das Rezept nicht passt.
     */
    @Nullable
    public int[] getConsumption(ItemStack s1, ItemStack s2) {
        if (fits(0, s1) && fits(1, s2)) return new int[] { inputItems.get(0).isEmpty() ? 0 : getCount(0), inputItems.get(1).isEmpty() ? 0 : getCount(1) };
        if (fits(1, s1) && fits(0, s2)) return new int[] { inputItems.get(1).isEmpty() ? 0 : getCount(1), inputItems.get(0).isEmpty() ? 0 : getCount(0) };
        return null;
    }

    private boolean fits(int index, ItemStack stack) {
        Ingredient ing = inputItems.get(index);
        if (ing.isEmpty()) return stack.isEmpty();
        return !stack.isEmpty() && ing.test(stack) && stack.getCount() >= getCount(index);
    }

    @Override
    public boolean matchesRecipe(RecipeInputWrapper container, Level level) {
        // Slots 1 und 2 (INPUT_SLOT_1/2), Zutaten in beliebiger Reihenfolge, mit Mengen.
        if (container.size() < 3) return false;
        return getConsumption(container.getItem(1), container.getItem(2)) != null;
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

            int[] counts = {1, 1};
            for (int i = 0; i < inputs.size() && i < ingredients.size(); i++) {
                com.google.gson.JsonElement el = ingredients.get(i);
                if (el.isJsonObject() && el.getAsJsonObject().has("count")) {
                    JsonObject copy = el.getAsJsonObject().deepCopy();
                    counts[i] = GsonHelper.getAsInt(copy, "count", 1);
                    copy.remove("count");
                    el = copy;
                }
                inputs.set(i, RecipeHooks.ingredientFromJson(el));
            }

            return new BlastFurnaceRecipe(inputs, output, secondaryOutput, duration, recipeId)
                    .withCounts(counts, GsonHelper.getAsBoolean(serializedRecipe, "nt", false));
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
            int[] counts = { buffer.readVarInt(), buffer.readVarInt() };
            return new BlastFurnaceRecipe(inputs, output, secondaryOutput, duration, recipeId).withCounts(counts, buffer.readBoolean());
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
            buffer.writeVarInt(recipe.getCount(0));
            buffer.writeVarInt(recipe.getCount(1));
            buffer.writeBoolean(recipe.isNT());
        }
    }
}
