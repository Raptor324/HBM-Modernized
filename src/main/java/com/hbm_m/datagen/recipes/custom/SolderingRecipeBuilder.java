package com.hbm_m.datagen.recipes.custom;
//? if forge {
import javax.annotation.Nullable;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.hbm_m.recipe.SolderingRecipe;

import dev.architectury.fluid.FluidStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;

/**
 * Datagen-билдер {@link SolderingRecipe} ({@code hbm_m:soldering_station}).
 *
 * <p>Чистый ванильный 1.20.1 код внутри {@code //? if forge} — датаген только для 1.20.1-forge.</p>
 *
 * <p>JSON-формат (читается {@link SolderingRecipe.Serializer#readJson}):</p>
 * <pre>{@code
 * {
 *   "type": "hbm_m:soldering_station",
 *   "toppings": [ { "ingredient": { ... }, "count": 3 }, ... ],   // 0..3
 *   "pcb":      [ { "ingredient": { ... }, "count": 4 } ],         // 0..2
 *   "solder":   [ { "ingredient": { ... }, "count": 4 } ],       // 0..1
 *   "fluid": { "fluid": "...", "amount": 1000 },  // optional
 *   "result": { "item": "...", "count": 1 },
 *   "duration": 100,
 *   "consumption": 100
 * }
 * }</pre>
 */
public class SolderingRecipeBuilder extends BaseRecipeBuilder<SolderingRecipeBuilder> {

    private final Ingredient[] toppings;
    private final int[] toppingCounts;
    private final Ingredient[] pcb;
    private final int[] pcbCounts;
    private final Ingredient[] solder;
    private final int[] solderCounts;
    @Nullable
    private final FluidStack fluid;
    private final ItemStack output;
    private final int duration;
    private final long consumption;

    private SolderingRecipeBuilder(Ingredient[] toppings, int[] toppingCounts,
                                    Ingredient[] pcb, int[] pcbCounts,
                                    Ingredient[] solder, int[] solderCounts,
                                    @Nullable FluidStack fluid, ItemStack output, int duration, long consumption) {
        this.toppings = toppings; this.toppingCounts = toppingCounts;
        this.pcb = pcb; this.pcbCounts = pcbCounts;
        this.solder = solder; this.solderCounts = solderCounts;
        this.fluid = fluid;
        this.output = output;
        this.duration = duration;
        this.consumption = consumption;
    }

    public static SolderingRecipeBuilder solderingRecipe(Ingredient[] toppings, int[] toppingCounts,
                                                          Ingredient[] pcb, int[] pcbCounts,
                                                          Ingredient[] solder, int[] solderCounts,
                                                          @Nullable FluidStack fluid, ItemStack output,
                                                          int duration, long consumption) {
        return new SolderingRecipeBuilder(toppings, toppingCounts, pcb, pcbCounts, solder, solderCounts,
                fluid, output, duration, consumption);
    }

    @Override
    public Item getResult() {
        return this.output.getItem();
    }

    private static JsonArray groupToJson(Ingredient[] group, int[] counts) {
        JsonArray arr = new JsonArray();
        for (int i = 0; i < group.length; i++) {
            JsonObject entry = new JsonObject();
            entry.add("ingredient", group[i].toJson());
            if (counts[i] > 1) entry.addProperty("count", counts[i]);
            arr.add(entry);
        }
        return arr;
    }

    @Override
    protected void serializeRecipeData(JsonObject json) {
        if (toppings.length > 0) json.add("toppings", groupToJson(toppings, toppingCounts));
        if (pcb.length > 0)      json.add("pcb", groupToJson(pcb, pcbCounts));
        if (solder.length > 0)   json.add("solder", groupToJson(solder, solderCounts));
        if (fluid != null && !fluid.isEmpty() && fluid.getAmount() > 0) {
            json.add("fluid", fluidStackToJson(fluid));
        }
        json.add("result", stackToJson(output));
        json.addProperty("duration", duration);
        json.addProperty("consumption", consumption);
    }

    @Override
    protected RecipeSerializer<?> getType() {
        return SolderingRecipe.Serializer.INSTANCE;
    }
}
//?}
