package com.hbm_m.datagen.recipes.custom;
//? if forge {
import com.google.gson.JsonObject;
import com.hbm_m.inventory.material.MaterialType;
import com.hbm_m.item.material.ItemCastMold;
import com.hbm_m.recipe.MoldCastingRecipe;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;

/**
 * Datagen-билдер {@link MoldCastingRecipe} ({@code hbm_m:mold_casting}).
 *
 * <p>Чистый ванильный 1.20.1 код внутри {@code //? if forge}. Поддерживает два формата
 * {@code output}:
 * <ul>
 *   <li><b>одиночный предмет</b> — {@code output = Ingredient.of(stack)};
 *       JSON: {@code {"item":"hbm_m:plate_iron","count":1}}</li>
 *   <li><b>forge-тег</b> — {@code output = Ingredient.of(tagKey)};
 *       JSON: {@code {"tag":"forge:ingots/iron","count":1}}</li>
 * </ul>
 * Это позволяет {@code MoldCastingRecipeGenerator}'у эмитить рецепты для ванильных слитков
 * /самородков/блоков (через теги), даже когда реестр-теги не загружены на стадии датагена
 * — рецепт резолвится в runtime при загрузке датапака.</p>
 *
 * <p>{@link #getResult()} возвращает первый предмет ingredient'а (требование ванильного
 * {@code RecipeBuilder}); но реальный выходной стак вычисляется {@link MoldCastingRecipe#getOutput}
 * в runtime из {@code outputIngredient.getItems()[0]} × {@code count}.</p>
 */
public class MoldCastingRecipeBuilder extends BaseRecipeBuilder<MoldCastingRecipeBuilder> {

    private final ItemCastMold.MoldType mold;
    private final MaterialType material;
    private final Ingredient output;
    private final int count;

    public MoldCastingRecipeBuilder(ItemCastMold.MoldType mold, MaterialType material,
                                    Ingredient output, int count) {
        this.mold = mold;
        this.material = material;
        this.output = output;
        this.count = count;
    }

    /** Item-перегрузка: выход — одиночный {@link ItemStack} (запишется как {@code {"item":...}}). */
    public MoldCastingRecipeBuilder(ItemCastMold.MoldType mold, MaterialType material, ItemStack output) {
        this(mold, material, Ingredient.of(output), output.getCount());
    }

    @Override
    public Item getResult() {
        ItemStack[] items = output.getItems();
        return (items.length == 0 || items[0].isEmpty()) ? net.minecraft.world.item.Items.AIR
                : items[0].getItem();
    }

    @Override
    protected void serializeRecipeData(JsonObject json) {
        json.addProperty("mold", mold.name());
        json.addProperty("material", material != null ? material.name : "iron");
        JsonObject outJson = new JsonObject();
        // Теговый ингредиент — пишем {"tag":..., "count":...}; одиночный предмет — {"item":..., "count":...}.
        // В Ingredient 1.20.1 нет публичного API, чтобы узнать, это тег или предмет. Используем
        // toJson() (формат vanilla ingredient) и хитрый хак: проверяем наличие "tag" в сериализованном.
        net.minecraft.resources.ResourceLocation tagId = tagIdOf(output);
        if (tagId != null) {
            outJson.addProperty("tag", tagId.toString());
        } else {
            ItemStack first = output.getItems().length > 0 ? output.getItems()[0] : ItemStack.EMPTY;
            outJson.addProperty("item",
                    net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(first.getItem()).toString());
        }
        if (count > 1) outJson.addProperty("count", count);
        json.add("output", outJson);
    }

    /**
     * Хак для определения, является ли {@link Ingredient} теговым. В 1.20.1 Ingredient
     * {@code toJson()} эмитит {@code {"tag":"..."}}, а одиночный предмет — {@code {"item":"..."}}.
     */
    private static net.minecraft.resources.ResourceLocation tagIdOf(Ingredient ingredient) {
        try {
            com.google.gson.JsonElement el = ingredient.toJson();
            if (el.isJsonObject()) {
                JsonObject obj = el.getAsJsonObject();
                if (obj.has("tag")) {
                    return ResourceLocation.parse(net.minecraft.util.GsonHelper.getAsString(obj, "tag"));
                }
            }
        } catch (Throwable ignored) { }
        return null;
    }

    @Override
    protected RecipeSerializer<?> getType() {
        return MoldCastingRecipe.Serializer.INSTANCE;
    }
}
//?}
