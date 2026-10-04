package com.hbm_m.datagen.recipes.custom;
//? if forge {
import java.util.ArrayList;
import java.util.List;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.hbm_m.recipe.SilexRecipe;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.material.Fluid;

/**
 * Datagen-Builder fuer {@link SilexRecipe} ({@code hbm_m:silex}), 1:1 {@code new SILEXRecipe(produced, consumed, laser).addOut(...)}.
 *
 * <pre>{@code
 * { "type": "hbm_m:silex", "ingredient": {...}, "pellet_state": 3, "fluids": ["hbm_m:uf6"],
 *   "fluid_produced": 900, "fluid_consumed": 100, "laser": 1,
 *   "outputs": [ { "result": {...}, "weight": 1 } ] }
 * }</pre>
 */
public class SilexRecipeBuilder extends BaseRecipeBuilder<SilexRecipeBuilder> {

    private final int produced;
    private final int consumed;
    private final int laser;
    private Ingredient input = Ingredient.EMPTY;
    private int pelletState = -1;
    private final List<Fluid> fluids = new ArrayList<>();
    private final List<ItemStack> outputs = new ArrayList<>();
    private final List<Integer> weights = new ArrayList<>();

    private SilexRecipeBuilder(int produced, int consumed, int laser) {
        this.produced = produced;
        this.consumed = consumed;
        this.laser = laser;
    }

    public static SilexRecipeBuilder silex(int produced, int consumed, int laser) {
        return new SilexRecipeBuilder(produced, consumed, laser);
    }

    public SilexRecipeBuilder input(Ingredient input) { this.input = input; return this; }
    public SilexRecipeBuilder pellet(int state) { this.pelletState = state; return this; }
    public SilexRecipeBuilder fluid(Fluid fluid) { this.fluids.add(fluid); return this; }

    public SilexRecipeBuilder out(ItemStack stack, int weight) {
        outputs.add(stack);
        weights.add(weight);
        return this;
    }

    @Override
    public Item getResult() {
        return this.outputs.isEmpty() ? net.minecraft.world.item.Items.AIR : this.outputs.get(0).getItem();
    }

    @Override
    protected void serializeRecipeData(JsonObject json) {
        if (!input.isEmpty()) json.add("ingredient", input.toJson());
        if (pelletState >= 0) json.addProperty("pellet_state", pelletState);
        if (!fluids.isEmpty()) {
            JsonArray fa = new JsonArray();
            for (Fluid f : fluids) fa.add(BuiltInRegistries.FLUID.getKey(f).toString());
            json.add("fluids", fa);
        }
        json.addProperty("fluid_produced", produced);
        json.addProperty("fluid_consumed", consumed);
        json.addProperty("laser", laser);
        JsonArray arr = new JsonArray();
        for (int i = 0; i < outputs.size(); i++) {
            JsonObject entry = new JsonObject();
            entry.add("result", stackToJson(outputs.get(i)));
            entry.addProperty("weight", weights.get(i));
            arr.add(entry);
        }
        json.add("outputs", arr);
    }

    @Override
    protected RecipeSerializer<?> getType() {
        return SilexRecipe.Serializer.INSTANCE;
    }
}
//?}
