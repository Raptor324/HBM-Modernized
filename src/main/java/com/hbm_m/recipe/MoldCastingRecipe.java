package com.hbm_m.recipe;

import org.jetbrains.annotations.NotNull;

import com.google.gson.JsonObject;
import com.hbm_m.inventory.material.MaterialType;
import com.hbm_m.item.material.ItemCastMold;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.platform.recipe.PlatformRecipe;
import com.hbm_m.platform.recipe.PlatformRecipeSerializer;
import com.hbm_m.platform.recipe.RecipeHooks;
import com.hbm_m.platform.recipe.RecipeInputWrapper;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

/**
 * Отливка в форме ({@code hbm_m:mold_casting}): пара {@code (mold, material)} → {@link ItemStack}.
 *
 * <p>Порт 1.7.10 {@code Mold.getOutput(material)} (контейнер форм). Каждый рецепт описывает
 * одну пару «тип формы × материал → выход». Раньше {@code MoldCastingRecipes.getOutput} разрешал
 * выход динамически по forge-тегам/строковым id; теперь каждая пара становится отдельным
 * JSON-рецептом. {@code output} может быть конкретным предметом ({@code "item"}) или forge-тегом
 * ({@code "tag"}) — для пар, где выход — это ванильный слиток/самородок/блок (например
 * {@code forge:ingots/iron}), тег резолвится в runtime при загрузке датапака.</p>
 *
 * <p>JSON-формат (читается {@link Serializer#readJson}):</p>
 * <pre>{@code
 * {
 *   "type": "hbm_m:mold_casting",
 *   "mold":     "plate",                 // ItemCastMold.MoldType.name()
 *   "material": "iron",                  // MaterialType.name
 *   "output":   { "item": "hbm_m:plate_iron", "count": 1 }   // конкретный предмет
 *   // —или—
 *   "output":   { "tag": "forge:ingots/iron", "count": 1 }   // forge-тег (резолвится в runtime)
 * }
 * }</pre>
 */
public class MoldCastingRecipe extends PlatformRecipe {

    private final ItemCastMold.MoldType mold;
    private final MaterialType material;
    private final Ingredient outputIngredient;
    private final int outputCount;

    public MoldCastingRecipe(ResourceLocation id, ItemCastMold.MoldType mold,
                             MaterialType material, Ingredient outputIngredient, int outputCount) {
        super(id);
        this.mold = mold;
        this.material = material;
        this.outputIngredient = outputIngredient;
        this.outputCount = Math.max(1, outputCount);
    }

    public ItemCastMold.MoldType getMold()      { return mold; }
    public MaterialType getMaterial()            { return material; }
    public Ingredient getOutputIngredient()      { return outputIngredient; }

    /** {@link ItemStack} — первый {@link Ingredient#getItems()} × {@link #outputCount}. */
    public ItemStack getOutput() {
        ItemStack[] items = outputIngredient.getItems();
        if (items.length == 0 || items[0].isEmpty()) return ItemStack.EMPTY;
        ItemStack out = items[0].copy();
        out.setCount(outputCount);
        return out;
    }

    /** Совпадение по паре {@code (mold, material)} — для {@code MachineFoundryBasinBlockEntity}. */
    public boolean matches(ItemCastMold.MoldType moldType, MaterialType mat) {
        return this.mold == moldType && this.material == mat;
    }

    @Override
    public boolean matchesRecipe(@NotNull RecipeInputWrapper container, @NotNull Level level) {
        return !level.isClientSide();
    }

    @Override
    public ItemStack assembleSafe() { return getOutput(); }

    @Override
    public ItemStack getResultItemSafe() {
        ItemStack out = getOutput();
        return out.isEmpty() ? ItemStack.EMPTY : out;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return Serializer.INSTANCE;
    }

    @Override
    public RecipeType<?> getType() {
        return Type.INSTANCE;
    }

    public static class Type implements RecipeType<MoldCastingRecipe> {
        public static final Type INSTANCE = new Type();
        public static final String ID = "mold_casting";
    }

    public static class Serializer extends PlatformRecipeSerializer<MoldCastingRecipe> {
        public static final Serializer INSTANCE = new Serializer();
        //? if fabric && < 1.21.1 {
        /*public static final ResourceLocation ID = new ResourceLocation(RefStrings.MODID, "mold_casting");
        *///?} else {
        public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "mold_casting");
        //?}

        @Override
        public MoldCastingRecipe readJson(ResourceLocation recipeId, JsonObject json) {
            String moldName = GsonHelper.getAsString(json, "mold");
            ItemCastMold.MoldType mold;
            try {
                mold = ItemCastMold.MoldType.valueOf(moldName);
            } catch (IllegalArgumentException e) {
                throw new IllegalStateException("Unknown mold type '" + moldName
                        + "' in mold_casting recipe " + recipeId, e);
            }
            String matName = GsonHelper.getAsString(json, "material");
            MaterialType mat = MaterialType.byName(matName);
            if (mat == null) {
                throw new IllegalStateException("Unknown material '" + matName
                        + "' in mold_casting recipe " + recipeId);
            }
            JsonObject outJson = GsonHelper.getAsJsonObject(json, "output");
            int count = GsonHelper.getAsInt(outJson, "count", 1);

            Ingredient outputIngredient;
            if (outJson.has("tag")) {
                String tagId = GsonHelper.getAsString(outJson, "tag");
                TagKey<net.minecraft.world.item.Item> tag = TagKey.create(Registries.ITEM,
                        ResourceLocation.parse(tagId));
                outputIngredient = Ingredient.of(tag);
            } else {
                // Поддержка как одиночного предмета, так и полного ingredient-json-формата.
                ItemStack stack = RecipeHooks.itemStackFromJson(outJson);
                outputIngredient = Ingredient.of(stack);
            }
            return new MoldCastingRecipe(recipeId, mold, mat, outputIngredient, count);
        }

        @Override
        public MoldCastingRecipe readNetwork(ResourceLocation recipeId, FriendlyByteBuf buf) {
            ItemCastMold.MoldType mold = buf.readEnum(ItemCastMold.MoldType.class);
            MaterialType mat = MaterialType.byName(buf.readUtf());
            Ingredient outputIngredient = RecipeHooks.readIngredient(buf);
            int count = buf.readVarInt();
            return new MoldCastingRecipe(recipeId, mold,
                    mat != null ? mat : MaterialType.IRON, outputIngredient, count);
        }

        @Override
        public void writeNetwork(FriendlyByteBuf buf, MoldCastingRecipe recipe) {
            buf.writeEnum(recipe.mold);
            buf.writeUtf(recipe.material != null ? recipe.material.name : "iron");
            RecipeHooks.writeIngredient(buf, recipe.outputIngredient);
            buf.writeVarInt(recipe.outputCount);
        }
    }
}
