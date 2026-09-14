package com.hbm_m.datagen.recipes.custom;
//? if forge {
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.hbm_m.inventory.material.MaterialStack;
import com.hbm_m.inventory.material.MaterialType;
import com.hbm_m.recipe.MoltenAlloyRecipe;

import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeSerializer;

/**
 * Datagen-билдер {@link MoltenAlloyRecipe} ({@code hbm_m:molten_alloy}).
 *
 * <p>Чистый ванильный 1.20.1 код внутри {@code //? if forge} — датаген компилируется
 * только на 1.20.1-forge. Рецепт material-based (вход/выход — {@link MaterialStack}[], не
 * предметы/жидкости), поэтому {@link #getResult()} возвращает {@link Items#AIR} — ванильный
 * {@code RecipeBuilder} требует реализацию, но для material-рецептов результат не используется.</p>
 *
 * <p>JSON-формат (читается {@link MoltenAlloyRecipe.Serializer#readJson}):</p>
 * <pre>{@code
 * {
 *   "type": "hbm_m:molten_alloy",
 *   "frequency": 20,
 *   "inputs":  [ { "material": "iron",   "amount": 288 }, ... ],
 *   "outputs": [ { "material": "steel",  "amount": 288 }, ... ]
 * }
 * }</pre>
 */
public class MoltenAlloyRecipeBuilder extends BaseRecipeBuilder<MoltenAlloyRecipeBuilder> {

    private final MaterialStack[] inputs;
    private final MaterialStack[] outputs;
    private final int frequency;

    public MoltenAlloyRecipeBuilder(MaterialStack[] inputs, MaterialStack[] outputs, int frequency) {
        this.inputs = inputs;
        this.outputs = outputs;
        this.frequency = frequency;
    }

    @Override
    public net.minecraft.world.item.Item getResult() {
        return Items.AIR;
    }

    @Override
    protected void serializeRecipeData(JsonObject json) {
        json.addProperty("frequency", frequency);
        json.add("inputs",  materialArrayToJson(inputs));
        json.add("outputs", materialArrayToJson(outputs));
    }

    private static JsonArray materialArrayToJson(MaterialStack[] arr) {
        JsonArray out = new JsonArray();
        for (MaterialStack ms : arr) {
            JsonObject el = new JsonObject();
            el.addProperty("material", ms.type != null ? ms.type.name : "iron");
            el.addProperty("amount",   ms.amount);
            out.add(el);
        }
        return out;
    }

    @Override
    protected RecipeSerializer<?> getType() {
        return MoltenAlloyRecipe.Serializer.INSTANCE;
    }
}
//?}
