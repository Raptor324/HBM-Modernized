package com.hbm_m.recipe;

import javax.annotation.Nullable;

import org.jetbrains.annotations.NotNull;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.platform.recipe.PlatformRecipe;
import com.hbm_m.platform.recipe.PlatformRecipeSerializer;
import com.hbm_m.platform.recipe.RecipeHooks;
import com.hbm_m.platform.recipe.RecipeInputWrapper;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

/**
 * Datapack-рецепт электролизёра, Metal-режим ({@code hbm_m:electrolyser_metal}).
 *
 * <p>Порт статического {@code ElectrolyserRecipes} (Metal-часть): кристалл (1 шт., слот 5 машины) →
 * два предметных выхода (слоты 6/7) + до трёх побочных предметов (слоты 8..10), фиксированная
 * длительность {@code duration} (у всех исходных рецептов 600 тиков).</p>
 *
 * <p><b>Замена статике:</b> прежний {@code ElectrolyserRecipes} держал {@code Map<Item, MetalRecipe>}
 * в Java-статике. Теперь источник правды — {@code RecipeManager}, а
 * {@code MachineElectrolyserBlockEntity.tick} ищет рецепт через
 * {@link RecipeHooks#getAllRecipes(Level, RecipeType)}.</p>
 *
 * <p>JSON-формат:</p>
 * <pre>{@code
 * {
 *   "type": "hbm_m:electrolyser_metal",
 *   "ingredient":  { "item": "hbm_m:crystal_iron" },
 *   "output_a":    { "item": "hbm_m:ingot_steel", "count": 6 },
 *   "output_b":    { "item": "hbm_m:ingot_titanium", "count": 2 },  // optional
 *   "byproducts": [ { "item": "...", "count": 3 } ],                // optional, до 3
 *   "duration": 600
 * }
 * }</pre>
 */
public class ElectrolyserMetalRecipe extends PlatformRecipe {

    private final Ingredient input;
    private final ItemStack outputA;
    @Nullable
    private final ItemStack outputB;
    private final ItemStack[] byproducts;
    private final int duration;

    public ElectrolyserMetalRecipe(ResourceLocation id, Ingredient input, ItemStack outputA,
                                    @Nullable ItemStack outputB, ItemStack[] byproducts, int duration) {
        super(id);
        this.input = input;
        this.outputA = outputA;
        this.outputB = outputB != null && !outputB.isEmpty() ? outputB : null;
        this.byproducts = byproducts != null ? byproducts : new ItemStack[0];
        this.duration = Math.max(1, duration);
    }

    public Ingredient getInput() { return input; }
    public ItemStack getOutputA() { return outputA.copy(); }
    @Nullable public ItemStack getOutputB() { return outputB != null ? outputB.copy() : ItemStack.EMPTY; }
    public ItemStack[] getByproducts() { return byproducts; }
    public int getDuration() { return duration; }

    /** Совпадение слота кристалла: предмет подходит под ингредиент (оригинал — точный Item). */
    public boolean matchesInput(ItemStack crystal) {
        return !crystal.isEmpty() && input.test(crystal);
    }

    @Override
    public boolean matchesRecipe(@NotNull RecipeInputWrapper container, @NotNull Level level) {
        // Metal-режим: единственный предметный вход — слот 0 обёртки.
        return !level.isClientSide() && matchesInput(container.getItem(0));
    }

    @Override
    public ItemStack assembleSafe() {
        return getResultItemSafe();
    }

    @Override
    public ItemStack getResultItemSafe() {
        return outputA.copy();
    }

    @Override
    public RecipeSerializer<?> getSerializer() { return Serializer.INSTANCE; }

    @Override
    public RecipeType<?> getType() { return Type.INSTANCE; }

    public static class Type implements RecipeType<ElectrolyserMetalRecipe> {
        public static final Type INSTANCE = new Type();
        public static final String ID = "electrolyser_metal";
    }

    public static class Serializer extends PlatformRecipeSerializer<ElectrolyserMetalRecipe> {
        public static final Serializer INSTANCE = new Serializer();
        //? if fabric && < 1.21.1 {
        /*public static final ResourceLocation ID = new ResourceLocation(RefStrings.MODID, "electrolyser_metal");
        *///?} else {
        public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "electrolyser_metal");
        //?}

        @Override
        public ElectrolyserMetalRecipe readJson(ResourceLocation recipeId, JsonObject json) {
            Ingredient input = RecipeHooks.ingredientFromJson(json.get("ingredient"));
            ItemStack outputA = RecipeHooks.itemStackFromJson(GsonHelper.getAsJsonObject(json, "output_a"));
            ItemStack outputB = json.has("output_b") ? RecipeHooks.itemStackFromJson(GsonHelper.getAsJsonObject(json, "output_b")) : null;

            ItemStack[] byproducts = new ItemStack[0];
            if (json.has("byproducts")) {
                JsonArray arr = GsonHelper.getAsJsonArray(json, "byproducts");
                byproducts = new ItemStack[arr.size()];
                for (int i = 0; i < arr.size(); i++) {
                    byproducts[i] = RecipeHooks.itemStackFromJson(arr.get(i).getAsJsonObject());
                }
            }
            int duration = GsonHelper.getAsInt(json, "duration", 600);
            return new ElectrolyserMetalRecipe(recipeId, input, outputA, outputB, byproducts, duration);
        }

        @Override
        public ElectrolyserMetalRecipe readNetwork(ResourceLocation recipeId, FriendlyByteBuf buf) {
            Ingredient input = RecipeHooks.readIngredient(buf);
            ItemStack outputA = RecipeHooks.readItem(buf);
            ItemStack outputB = buf.readBoolean() ? RecipeHooks.readItem(buf) : ItemStack.EMPTY;
            int n = buf.readVarInt();
            ItemStack[] byproducts = new ItemStack[n];
            for (int i = 0; i < n; i++) {
                byproducts[i] = RecipeHooks.readItem(buf);
            }
            int duration = buf.readVarInt();
            return new ElectrolyserMetalRecipe(recipeId, input, outputA, outputB, byproducts, duration);
        }

        @Override
        public void writeNetwork(FriendlyByteBuf buf, ElectrolyserMetalRecipe recipe) {
            RecipeHooks.writeIngredient(buf, recipe.input);
            RecipeHooks.writeItem(buf, recipe.outputA);
            buf.writeBoolean(recipe.outputB != null);
            if (recipe.outputB != null) RecipeHooks.writeItem(buf, recipe.outputB);
            buf.writeVarInt(recipe.byproducts.length);
            for (ItemStack byproduct : recipe.byproducts) {
                RecipeHooks.writeItem(buf, byproduct);
            }
            buf.writeVarInt(recipe.duration);
        }
    }
}
