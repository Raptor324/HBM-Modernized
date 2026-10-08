package com.hbm_m.datagen.recipes.custom;
//? if forge {
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.hbm_m.recipe.BlastFurnaceRecipe;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.ItemLike;

public class BlastFurnaceRecipeBuilder extends BaseRecipeBuilder<BlastFurnaceRecipeBuilder> {
    private final Ingredient inputA;
    private final Ingredient inputB;
    private final ItemStack output;
    private ItemStack secondaryOutput = ItemStack.EMPTY;
    private int duration = 800;
    private int countA = 1;
    private int countB = 1;
    private boolean nt = false;

    private BlastFurnaceRecipeBuilder(ItemStack output, Ingredient inputA, Ingredient inputB) {
        this.inputA = inputA;
        this.inputB = inputB;
        this.output = output;
    }

    public static BlastFurnaceRecipeBuilder blastFurnaceRecipe(ItemStack output, Ingredient inputA, Ingredient inputB) {
        return new BlastFurnaceRecipeBuilder(output, inputA, inputB);
    }

    public static BlastFurnaceRecipeBuilder blastFurnaceRecipe(ItemStack output, ItemLike inputA, ItemLike inputB) {
        return blastFurnaceRecipe(output, Ingredient.of(inputA), Ingredient.of(inputB));
    }

    /** Длительность плавки в тиках (при скорости 1.0). */
    public BlastFurnaceRecipeBuilder duration(int ticks) {
        this.duration = ticks;
        return this;
    }

    /** Mengen der beiden Zutaten (BlastFurnaceRecipesNT). */
    public BlastFurnaceRecipeBuilder counts(int a, int b) {
        this.countA = a;
        this.countB = b;
        return this;
    }

    /** Rezept des neuen Hochofens (BlastFurnaceRecipesNT) statt des Legierungsofens. */
    public BlastFurnaceRecipeBuilder nt() {
        this.nt = true;
        return this;
    }

    /** Второй выход (шлак). */
    public BlastFurnaceRecipeBuilder secondaryOutput(ItemStack stack) {
        this.secondaryOutput = stack;
        return this;
    }

    @Override
    public Item getResult() {
        return this.output.getItem();
    }

    @Override
    protected void serializeRecipeData(JsonObject json) {
        JsonArray ingredients = new JsonArray();
        ingredients.add(withCount(this.inputA.toJson(), countA));
        ingredients.add(withCount(this.inputB.toJson(), countB));
        json.add("ingredients", ingredients);

        // Унифицированная сериализация ItemStack (через BaseRecipeBuilder.stackToJson).
        json.add("output", stackToJson(this.output));
        if (!this.secondaryOutput.isEmpty()) {
            json.add("secondary_output", stackToJson(this.secondaryOutput));
        }
        json.addProperty("duration", this.duration);
        if (this.nt) json.addProperty("nt", true);
    }

    private static com.google.gson.JsonElement withCount(com.google.gson.JsonElement el, int count) {
        if (count == 1) return el;
        JsonObject obj;
        if (el.isJsonObject()) {
            obj = el.getAsJsonObject();
        } else {
            // Mehrfach-Zutat (Array) -> forge:compound, damit "count" daneben stehen kann
            obj = new JsonObject();
            obj.addProperty("type", "forge:compound");
            obj.add("children", el);
        }
        obj.addProperty("count", count);
        return obj;
    }

    @Override
    protected RecipeSerializer<?> getType() {
        return BlastFurnaceRecipe.Serializer.INSTANCE;
    }
}
//?}
