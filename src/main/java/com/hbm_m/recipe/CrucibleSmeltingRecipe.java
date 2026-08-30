package com.hbm_m.recipe;

import org.jetbrains.annotations.NotNull;

import com.google.gson.JsonObject;
import com.hbm_m.inventory.material.MaterialStack;
import com.hbm_m.inventory.material.MaterialType;
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
 * Datapack-facing тигель-плавильный рецепт ({@code hbm_m:crucible_smelting}).
 *
 * <p>Порт 1.7.10 {@code com.hbm.inventory.recipes.CrucibleRecipes#getSmeltingRecipes()}
 * (полуподмножество {@code CrucibleSmeltingRecipes.registerDefaults}). Тигель плавит предметный
 * вход (через {@link Ingredient}, по тегу или конкретному предмету) в расплавленный материал
 * заданного {@link MaterialType} и количества в mB. Это «сырьевая» половина тигля: алиирование
 * живёт в {@code MoltenAlloyRecipes}, отливка — в {@code MoldCastingRecipes}.</p>
 *
 * <p><b>Замена статике:</b> прежний {@code CrucibleSmeltingRecipes} хранил
 * {@code List<SmeltingEntry>} в Java-статике, пополнявшуюся из {@code MainRegistry} вручную.
 * Это блокировало data-driven рецепты. Теперь источник правды — {@code RecipeManager}, а
 * {@code CrucibleSmeltingRecipes.smelt(level, stack)} — фасад, читающий JSON-рецепты через
 * {@link RecipeHooks#getAllRecipes(Level, net.minecraft.world.item.crafting.RecipeType)}.</p>
 *
 * <p>JSON-формат:</p>
 * <pre>{@code
 * {
 *   "type": "hbm_m:crucible_smelting",
 *   "ingredient": { ...Ingredient... },
 *   "material": "iron",            // MaterialType.name (строковый id материала)
 *   "amount": 144                 // mB; default MaterialStack.MB_PER_INGOT
 * }
 * }</pre>
 *
 * <p>{@code MaterialType} разрешается через {@link MaterialType#byName(String)} (MaterialType
 * идентифицируется строкой {@code name}, без namespace) — это лёгкая операция, т.к. {@code BY_NAME}
 * статическая {@code Map}, заполненная в статическом инициализаторе {@link MaterialType}.</p>
 */
public class CrucibleSmeltingRecipe extends PlatformRecipe {

    private final Ingredient input;
    private final MaterialType material;
    private final int amountMb;

    public CrucibleSmeltingRecipe(ResourceLocation id, Ingredient input, MaterialType material, int amountMb) {
        super(id);
        this.input = input;
        this.material = material;
        this.amountMb = Math.max(1, amountMb);
    }

    public Ingredient getInput() { return input; }
    public MaterialType getMaterial() { return material; }
    public int getAmountMb() { return amountMb; }
    public String getMaterialName() { return material != null ? material.name : "iron"; }

    /** Проверяет совпадение входа (тигель сам управляет how much to consume). */
    public boolean matchesInput(ItemStack stack) {
        return stack != null && !stack.isEmpty() && input.test(stack);
    }

    /** Превращает рецепт в {@link MaterialStack} (mB × stack-size, как в оригинале: smelt scales w/ count). */
    public MaterialStack toMaterialStack(int stackCount) {
        return new MaterialStack(material, amountMb * Math.max(1, stackCount));
    }

    @Override
    public boolean matchesRecipe(@NotNull RecipeInputWrapper container, @NotNull Level level) {
        return !level.isClientSide() && input.test(container.getItem(0));
    }

    @Override
    public ItemStack assembleSafe() { return ItemStack.EMPTY; }

    @Override
    public ItemStack getResultItemSafe() { return ItemStack.EMPTY; }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return Serializer.INSTANCE;
    }

    @Override
    public RecipeType<?> getType() {
        return Type.INSTANCE;
    }

    public static class Type implements RecipeType<CrucibleSmeltingRecipe> {
        public static final Type INSTANCE = new Type();
        public static final String ID = "crucible_smelting";
    }

    public static class Serializer extends PlatformRecipeSerializer<CrucibleSmeltingRecipe> {
        public static final Serializer INSTANCE = new Serializer();
        //? if fabric && < 1.21.1 {
        /*public static final ResourceLocation ID = new ResourceLocation(RefStrings.MODID, "crucible_smelting");
        *///?} else {
        public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "crucible_smelting");
        //?}

        @Override
        public CrucibleSmeltingRecipe readJson(ResourceLocation recipeId, JsonObject json) {
            Ingredient input = RecipeHooks.ingredientFromJson(json.get("ingredient"));
            // MaterialType идентифицируется строкой name (MaterialType.name), без namespace.
            String matName = GsonHelper.getAsString(json, "material");
            MaterialType mat = MaterialType.byName(matName);
            if (mat == null) {
                throw new IllegalStateException("Unknown material '" + matName
                        + "' in crucible_smelting recipe " + recipeId);
            }
            int amount = GsonHelper.getAsInt(json, "amount", MaterialStack.MB_PER_INGOT);
            return new CrucibleSmeltingRecipe(recipeId, input, mat, amount);
        }

        @Override
        public CrucibleSmeltingRecipe readNetwork(ResourceLocation recipeId, FriendlyByteBuf buf) {
            Ingredient input = RecipeHooks.readIngredient(buf);
            String matName = buf.readUtf();
            MaterialType mat = MaterialType.byName(matName);
            int amount = buf.readVarInt();
            return new CrucibleSmeltingRecipe(recipeId, input, mat != null ? mat : MaterialType.IRON, amount);
        }

        @Override
        public void writeNetwork(FriendlyByteBuf buf, CrucibleSmeltingRecipe recipe) {
            RecipeHooks.writeIngredient(buf, recipe.input);
            // MaterialType.name — строковый ключ, без namespace; сериализуем как UTF.
            buf.writeUtf(recipe.material != null ? recipe.material.name : "iron");
            buf.writeVarInt(recipe.amountMb);
        }
    }
}
