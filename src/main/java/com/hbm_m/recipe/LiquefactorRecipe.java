package com.hbm_m.recipe;

import org.jetbrains.annotations.NotNull;

import com.google.gson.JsonObject;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.platform.recipe.PlatformRecipe;
import com.hbm_m.platform.recipe.PlatformRecipeSerializer;
import com.hbm_m.platform.recipe.RecipeHooks;
import com.hbm_m.platform.recipe.RecipeInputWrapper;

import dev.architectury.fluid.FluidStack;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

/**
 * Datapack-facing рецепт ликвейфактора ({@code hbm_m:liquefactor}).
 *
 * <p>Порт 1.7.10 статического {@code com.hbm.inventory.recipes.LiquefactionRecipes} (в порте —
 * {@code LiquefactorRecipes}): один предмет → жидкость ({@link FluidStack}, mB). Размер стека
 * игнорируется — 1 предмет за цикл, как в оригинале.</p>
 *
 * <p><b>Замена статике:</b> прежний {@code LiquefactorRecipes} хранил {@code Map<Item, Output>}
 * в Java-статике. Теперь источник правды — {@code RecipeManager}, а
 * {@code MachineLiquefactorBlockEntity} ищет рецепт через
 * {@link RecipeHooks#getAllRecipes(Level, RecipeType)}.</p>
 *
 * <p>JSON-формат:</p>
 * <pre>{@code
 * {
 *   "type": "hbm_m:liquefactor",
 *   "ingredient": { "item": "minecraft:coal" },
 *   "result": { "fluid": "hbm_m:coaloil", "amount": 100 }
 * }
 * }</pre>
 */
public class LiquefactorRecipe extends PlatformRecipe {

    private final Ingredient input;
    private final FluidStack output;

    public LiquefactorRecipe(ResourceLocation id, Ingredient input, FluidStack output) {
        super(id);
        this.input = input;
        this.output = output != null ? output : FluidStack.empty();
    }

    public Ingredient getInput() { return input; }
    public FluidStack getOutput() { return output; }

    /** Количество выходной жидкости в mB. */
    public int getOutputAmountMb() {
        return output.isEmpty() ? 0 : (int) Math.min(Integer.MAX_VALUE, output.getAmount());
    }

    /** Мэтчинг по стёку входного слота (1 предмет за цикл, размер стека не важен — как оригинал). */
    public boolean matchesInput(ItemStack stack) {
        return stack != null && !stack.isEmpty() && input.test(stack);
    }

    @Override
    public boolean matchesRecipe(@NotNull RecipeInputWrapper container, @NotNull Level level) {
        // Ликвейфактор однослотный: slot 0 = вход.
        return !level.isClientSide() && matchesInput(container.getItem(0));
    }

    @Override
    public ItemStack assembleSafe() { return ItemStack.EMPTY; }

    @Override
    public ItemStack getResultItemSafe() { return ItemStack.EMPTY; }

    @Override
    public RecipeSerializer<?> getSerializer() { return Serializer.INSTANCE; }

    @Override
    public RecipeType<?> getType() { return Type.INSTANCE; }

    public static class Type implements RecipeType<LiquefactorRecipe> {
        public static final Type INSTANCE = new Type();
        public static final String ID = "liquefactor";
    }

    public static class Serializer extends PlatformRecipeSerializer<LiquefactorRecipe> {
        public static final Serializer INSTANCE = new Serializer();
        //? if fabric && < 1.21.1 {
        /*public static final ResourceLocation ID = new ResourceLocation(RefStrings.MODID, "liquefactor");
        *///?} else {
        public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "liquefactor");
        //?}

        @Override
        public LiquefactorRecipe readJson(ResourceLocation recipeId, JsonObject json) {
            Ingredient input = RecipeHooks.ingredientFromJson(json.get("ingredient"));
            FluidStack output = readFluidOutput(json);
            return new LiquefactorRecipe(recipeId, input, output);
        }

        @Override
        public LiquefactorRecipe readNetwork(ResourceLocation recipeId, FriendlyByteBuf buf) {
            Ingredient input = RecipeHooks.readIngredient(buf);
            FluidStack output = RecipeHooks.readFluidStack(buf);
            return new LiquefactorRecipe(recipeId, input, output);
        }

        @Override
        public void writeNetwork(FriendlyByteBuf buf, LiquefactorRecipe recipe) {
            RecipeHooks.writeIngredient(buf, recipe.input);
            RecipeHooks.writeFluidStack(buf, recipe.output);
        }

        /** Жидкостный выход: { "fluid": "...", "amount": mB }. */
        private static FluidStack readFluidOutput(JsonObject json) {
            JsonObject fluidObj = GsonHelper.getAsJsonObject(json, "result");
            ResourceLocation id = ResourceLocation.tryParse(GsonHelper.getAsString(fluidObj, "fluid"));
            long amount = GsonHelper.getAsLong(fluidObj, "amount", 0L);
            if (id == null || amount <= 0) {
                return FluidStack.empty();
            }
            return RecipeHooks.fluidStackOf(id, amount);
        }
    }
}
