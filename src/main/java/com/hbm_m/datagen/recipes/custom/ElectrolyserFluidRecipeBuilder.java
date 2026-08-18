package com.hbm_m.datagen.recipes.custom;
//? if forge {
import javax.annotation.Nullable;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.hbm_m.recipe.ElectrolyserFluidRecipe;

import dev.architectury.fluid.FluidStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;

/**
 * Datagen-билдер {@link ElectrolyserFluidRecipe} ({@code hbm_m:electrolyser_fluid}).
 *
 * <p>Чистый ванильный 1.20.1 код внутри {@code //? if forge} — датаген только для 1.20.1-forge.</p>
 *
 * <p>JSON-формат (читается {@link ElectrolyserFluidRecipe.Serializer#readJson}):</p>
 * <pre>{@code
 * {
 *   "type": "hbm_m:electrolyser_fluid",
 *   "input":    { "fluid": "...", "amount": 2000 },
 *   "output_a": { "fluid": "...", "amount": 200 },   // optional
 *   "output_b": { "fluid": "...", "amount": 200 },   // optional
 *   "byproducts": [ { "item": "...", "count": 1 } ]  // optional
 * }
 * }</pre>
 */
public class ElectrolyserFluidRecipeBuilder extends BaseRecipeBuilder<ElectrolyserFluidRecipeBuilder> {

    private final FluidStack input;
    @Nullable
    private final FluidStack outputA;
    @Nullable
    private final FluidStack outputB;
    private final ItemStack[] byproducts;

    private ElectrolyserFluidRecipeBuilder(FluidStack input, @Nullable FluidStack outputA,
                                            @Nullable FluidStack outputB, ItemStack[] byproducts) {
        this.input = input;
        this.outputA = outputA;
        this.outputB = outputB;
        this.byproducts = byproducts;
    }

    public static ElectrolyserFluidRecipeBuilder electrolyserFluidRecipe(FluidStack input,
                                                                          @Nullable FluidStack outputA,
                                                                          @Nullable FluidStack outputB,
                                                                          ItemStack... byproducts) {
        return new ElectrolyserFluidRecipeBuilder(input, outputA, outputB, byproducts);
    }

    @Override
    public Item getResult() {
        // Основной выход Fluid-режима — жидкости; ванильный RecipeBuilder требует реализацию,
        // но для рецептов без предметного выхода результат не используется (как в MixerRecipeBuilder).
        return net.minecraft.world.item.Items.AIR;
    }

    @Override
    protected void serializeRecipeData(JsonObject json) {
        json.add("input", fluidStackToJson(input));
        if (outputA != null && !outputA.isEmpty() && outputA.getAmount() > 0) {
            json.add("output_a", fluidStackToJson(outputA));
        }
        if (outputB != null && !outputB.isEmpty() && outputB.getAmount() > 0) {
            json.add("output_b", fluidStackToJson(outputB));
        }
        if (byproducts.length > 0) {
            JsonArray arr = new JsonArray();
            for (ItemStack byproduct : byproducts) {
                if (byproduct.isEmpty()) continue;
                arr.add(stackToJson(byproduct));
            }
            if (arr.size() > 0) json.add("byproducts", arr);
        }
    }

    @Override
    protected RecipeSerializer<?> getType() {
        return ElectrolyserFluidRecipe.Serializer.INSTANCE;
    }
}
//?}
