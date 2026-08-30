package com.hbm_m.recipe;

import org.jetbrains.annotations.NotNull;

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
 * Datapack-facing циклотрон-рецепт ({@code hbm_m:cyclotron}).
 *
 * <p>Порт 1.7.10 {@code com.hbm.inventory.recipes.CyclotronRecipes#registerRecipes()}: циклотрон
 * бомбардирует «таргет»-предмет (target) частицами «реактив» (input) и превращает его в новый
 * предмет, попутно производя антиматерию ({@code amatProduced}). Уникально для циклотрона:
 * две {@link Ingredient}-роли. Мэтчинг по обоим стекам.</p>
 *
 * <p><b>Замена статике:</b> преждний {@code CyclotronRecipes} хранил {@code List<Recipe>} в Java-статике,
 * что блокировало data-driven рецепты. Теперь источник правды — {@code RecipeManager} (JSON), и поиск
 * ведётся через {@code CyclotronRecipes.getOutput(level, target, input)}.</p>
 *
 * <p>JSON-формат:</p>
 * <pre>{@code
 * {
 *   "type": "hbm_m:cyclotron",
 *   "target": { ...Ingredient... },
 *   "input":   { ...Ingredient... },
 *   "result": { "item": "...", "count": 1 },
 *   "amat": 50
 * }
 * }</pre>
 */
public class CyclotronRecipe extends PlatformRecipe {

    private final Ingredient target;
    private final Ingredient input;
    private final ItemStack output;
    private final int amatProduced;

    public CyclotronRecipe(ResourceLocation id, Ingredient target, Ingredient input,
                            ItemStack output, int amatProduced) {
        super(id);
        this.target = target;
        this.input = input;
        this.output = output;
        this.amatProduced = Math.max(0, amatProduced);
    }

    public Ingredient getTarget() { return target; }
    public Ingredient getInput() { return input; }
    public ItemStack getOutput() { return output.copy(); }
    public int getAmatProduced() { return amatProduced; }

    /** Мэтчинг по двум стекам (target и input). */
    public boolean matches(ItemStack targetStack, ItemStack inputStack) {
        if (targetStack == null || inputStack == null) return false;
        return target.test(targetStack) && input.test(inputStack);
    }

    @Override
    public boolean matchesRecipe(@NotNull RecipeInputWrapper container, @NotNull Level level) {
        // Циклотрон двух-слотный: slot 0 = target, slot 1 = input.
        return !level.isClientSide()
                && target.test(container.getItem(0))
                && input.test(container.getItem(1));
    }

    @Override
    public ItemStack assembleSafe() {
        return output.copy();
    }

    @Override
    public ItemStack getResultItemSafe() {
        return output.copy();
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return Serializer.INSTANCE;
    }

    @Override
    public RecipeType<?> getType() {
        return Type.INSTANCE;
    }

    public static class Type implements RecipeType<CyclotronRecipe> {
        public static final Type INSTANCE = new Type();
        public static final String ID = "cyclotron";
    }

    public static class Serializer extends PlatformRecipeSerializer<CyclotronRecipe> {
        public static final Serializer INSTANCE = new Serializer();
        //? if fabric && < 1.21.1 {
        /*public static final ResourceLocation ID = new ResourceLocation(RefStrings.MODID, "cyclotron");
        *///?} else {
        public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "cyclotron");
        //?}

        @Override
        public CyclotronRecipe readJson(ResourceLocation recipeId, JsonObject json) {
            Ingredient target = RecipeHooks.ingredientFromJson(json.get("target"));
            Ingredient input = RecipeHooks.ingredientFromJson(json.get("input"));
            ItemStack output = RecipeHooks.itemStackFromJson(GsonHelper.getAsJsonObject(json, "result"));
            int amat = GsonHelper.getAsInt(json, "amat", 0);
            return new CyclotronRecipe(recipeId, target, input, output, amat);
        }

        @Override
        public CyclotronRecipe readNetwork(ResourceLocation recipeId, FriendlyByteBuf buf) {
            Ingredient target = RecipeHooks.readIngredient(buf);
            Ingredient input = RecipeHooks.readIngredient(buf);
            ItemStack output = RecipeHooks.readItem(buf);
            int amat = buf.readVarInt();
            return new CyclotronRecipe(recipeId, target, input, output, amat);
        }

        @Override
        public void writeNetwork(FriendlyByteBuf buf, CyclotronRecipe recipe) {
            RecipeHooks.writeIngredient(buf, recipe.target);
            RecipeHooks.writeIngredient(buf, recipe.input);
            RecipeHooks.writeItem(buf, recipe.output);
            buf.writeVarInt(recipe.amatProduced);
        }
    }
}
