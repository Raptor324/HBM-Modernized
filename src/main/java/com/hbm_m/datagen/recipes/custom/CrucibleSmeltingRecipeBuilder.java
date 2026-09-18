package com.hbm_m.datagen.recipes.custom;
//? if forge {
import com.google.gson.JsonObject;
import com.hbm_m.inventory.material.MaterialType;
import com.hbm_m.recipe.CrucibleSmeltingRecipe;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;

/**
 * Datagen-билдер {@link CrucibleSmeltingRecipe} ({@code hbm_m:crucible_smelting}).
 *
 * <p>Чистый ванильный 1.20.1 код внутри {@code //? if forge} — датаген компилируется
 * только на 1.20.1-forge. CrucibleSmelting не имеет предметного выхода (предмет →
 * расплавленный {@link MaterialType} в mB), поэтому {@link #getResult()} возвращает
 * {@link Items#AIR} — ванильный {@code RecipeBuilder} требует реализацию, но для
 * material-рецептов результат не используется.</p>
 *
 * <p>JSON-формат (читается {@link CrucibleSmeltingRecipe.Serializer#readJson}):</p>
 * <pre>{@code
 * {
 *   "type": "hbm_m:crucible_smelting",
 *   "ingredient": { ...Ingredient... },
 *   "material": "iron",     // MaterialType.name (строковый id материала)
 *   "amount": 144           // mB; default MaterialStack.MB_PER_INGOT
 * }
 * }</pre>
 */
public class CrucibleSmeltingRecipeBuilder extends BaseRecipeBuilder<CrucibleSmeltingRecipeBuilder> {

    private final Ingredient input;
    private final MaterialType material;
    private final int amountMb;
    /** Мульти-выход (руды с побочными продуктами, порт MatDistribution); null = одиночный выход. */
    private final java.util.List<com.hbm_m.inventory.material.MaterialStack> multiOutputs;

    private CrucibleSmeltingRecipeBuilder(Ingredient input, MaterialType material, int amountMb) {
        this.input = input;
        this.material = material;
        this.amountMb = amountMb;
        this.multiOutputs = null;
    }

    private CrucibleSmeltingRecipeBuilder(Ingredient input, java.util.List<com.hbm_m.inventory.material.MaterialStack> outputs) {
        this.input = input;
        this.material = outputs.isEmpty() ? MaterialType.IRON : outputs.get(0).type;
        this.amountMb = outputs.isEmpty() ? 1 : outputs.get(0).amount;
        this.multiOutputs = java.util.List.copyOf(outputs);
    }

    public static CrucibleSmeltingRecipeBuilder crucibleSmelting(Ingredient input, MaterialType material, int amountMb) {
        return new CrucibleSmeltingRecipeBuilder(input, material, amountMb);
    }

    /** Мульти-выход: руда, дающая несколько материалов (оригинал registerOre/registerEntry). */
    public static CrucibleSmeltingRecipeBuilder crucibleSmelting(Ingredient input, java.util.List<com.hbm_m.inventory.material.MaterialStack> outputs) {
        return new CrucibleSmeltingRecipeBuilder(input, outputs);
    }

    /** Мульти-выход, Item-перегрузка (ванильные рельсы и пр.). */
    public static CrucibleSmeltingRecipeBuilder crucibleSmelting(Item input, java.util.List<com.hbm_m.inventory.material.MaterialStack> outputs) {
        return crucibleSmelting(Ingredient.of(input), outputs);
    }

    /** Item-перегрузка: {@code input} — одиночный предмет. */
    public static CrucibleSmeltingRecipeBuilder crucibleSmelting(Item input, MaterialType material, int amountMb) {
        return crucibleSmelting(Ingredient.of(input), material, amountMb);
    }

    /** Item-tag перегрузка: {@code input} — forge-тег (строка вида {@code "forge:ingots/iron"}). */
    public static CrucibleSmeltingRecipeBuilder crucibleSmelting(String tagId, MaterialType material, int amountMb) {
        net.minecraft.tags.TagKey<Item> tag = net.minecraft.tags.TagKey.create(
                net.minecraft.core.registries.Registries.ITEM,
                net.minecraft.resources.ResourceLocation.parse(tagId));
        return crucibleSmelting(Ingredient.of(tag), material, amountMb);
    }

    @Override
    public Item getResult() {
        return Items.AIR;
    }

    @Override
    protected void serializeRecipeData(JsonObject json) {
        json.add("ingredient", this.input.toJson());
        if (this.multiOutputs != null) {
            // Формат мульти-выхода: "output": [[mat, mb], ...]
            var out = new com.google.gson.JsonArray();
            for (var ms : this.multiOutputs) {
                var entry = new com.google.gson.JsonArray();
                entry.add(ms.type != null ? ms.type.name : "iron");
                entry.add(ms.amount);
                out.add(entry);
            }
            json.add("output", out);
            return;
        }
        // MaterialType идентифицируется строкой name (см. CrucibleSmeltingRecipe.Serializer.readJson — MaterialType.byName).
        json.addProperty("material", this.material != null ? this.material.name : "iron");
        json.addProperty("amount", this.amountMb);
    }

    @Override
    protected RecipeSerializer<?> getType() {
        return CrucibleSmeltingRecipe.Serializer.INSTANCE;
    }
}
//?}
