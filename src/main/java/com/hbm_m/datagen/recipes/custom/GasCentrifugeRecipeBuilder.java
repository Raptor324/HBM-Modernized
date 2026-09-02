package com.hbm_m.datagen.recipes.custom;
//? if forge {
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.hbm_m.recipe.GasCentrifugeRecipe;

import dev.architectury.fluid.FluidStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;

/**
 * Datagen-билдер {@link GasCentrifugeRecipe} ({@code hbm_m:gas_centrifuge}) — JEI-only.
 *
 * <p>Чистый ванильный 1.20.1 код внутри {@code //? if forge} — датаген только для 1.20.1-forge.</p>
 *
 * <p>JSON-формат (читается {@link GasCentrifugeRecipe.Serializer#readJson}):</p>
 * <pre>{@code
 * {
 *   "type": "hbm_m:gas_centrifuge",
 *   "input": { "fluid": "...", "amount": 1200 },
 *   "outputs": [ { "item": "...", "count": 11 }, ... ],
 *   "high_speed": true,
 *   "centrifuge_count": 4
 * }
 * }</pre>
 */
public class GasCentrifugeRecipeBuilder extends BaseRecipeBuilder<GasCentrifugeRecipeBuilder> {

    private final FluidStack input;
    private final ItemStack[] outputs;
    private final boolean highSpeed;
    private final int centrifugeCount;

    private GasCentrifugeRecipeBuilder(FluidStack input, ItemStack[] outputs, boolean highSpeed, int centrifugeCount) {
        this.input = input;
        this.outputs = outputs;
        this.highSpeed = highSpeed;
        this.centrifugeCount = centrifugeCount;
    }

    public static GasCentrifugeRecipeBuilder gasCentrifugeRecipe(FluidStack input, ItemStack[] outputs,
                                                                  boolean highSpeed, int centrifugeCount) {
        return new GasCentrifugeRecipeBuilder(input, outputs, highSpeed, centrifugeCount);
    }

    @Override
    public Item getResult() {
        // JEI-only рецепт без предметного выхода — возвращаем первый выход (для criterion/ID).
        return outputs.length > 0 ? outputs[0].getItem() : net.minecraft.world.item.Items.AIR;
    }

    @Override
    protected void serializeRecipeData(JsonObject json) {
        json.add("input", fluidStackToJson(input));
        JsonArray arr = new JsonArray();
        for (ItemStack out : outputs) arr.add(stackToJson(out));
        json.add("outputs", arr);
        if (highSpeed) json.addProperty("high_speed", true);
        json.addProperty("centrifuge_count", centrifugeCount);
    }

    @Override
    protected RecipeSerializer<?> getType() {
        return GasCentrifugeRecipe.Serializer.INSTANCE;
    }
}
//?}
