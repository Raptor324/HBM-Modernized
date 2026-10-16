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
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;

/**
 * Datapack-facing рецепт солидификатора ({@code hbm_m:solidification}).
 *
 * <p>Порт 1.7.10 статического {@code com.hbm.inventory.recipes.SolidificationRecipes} (в порте —
 * {@code SolidificationRecipes}): жидкость (mB) → предмет. Мэтчинг по точному реестровому
 * {@link Fluid}, как ключ старой {@code Map<Fluid, Recipe>}.</p>
 *
 * <p><b>Замена статике:</b> прежний {@code SolidificationRecipes} хранил {@code Map<Fluid, Recipe>}
 * в Java-статике, включая 27 «авто»-рецептов, вычислявшихся из {@code FT_Flammable}-бренд-энергии
 * жидкости. Теперь источник правды — {@code RecipeManager} (JSON): авто-значения запечены
 * литералами в датагене ({@code SolidificationRecipeGenerator}), поэтому жидкость без
 * flammable-trait просто не имеет рецепта — та же семантика «нет трейта → нет рецепта».</p>
 *
 * <p>JSON-формат:</p>
 * <pre>{@code
 * {
 *   "type": "hbm_m:solidification",
 *   "fluid": { "fluid": "hbm_m:smear", "amount": 21000 },
 *   "result": { "item": "hbm_m:solid_fuel", "count": 1 }
 * }
 * }</pre>
 */
public class SolidificationRecipe extends PlatformRecipe {

    private final FluidStack input;
    private final ItemStack output;

    public SolidificationRecipe(ResourceLocation id, FluidStack input, ItemStack output) {
        super(id);
        this.input = input != null ? input : FluidStack.empty();
        this.output = output;
    }

    /** Жидкостный вход (жидкость + mB). */
    public FluidStack getInput() { return input; }

    /** Требуемое количество жидкости в mB (аналог прежнего {@code Recipe#fillMb()}). */
    public int getFillMb() {
        return input.isEmpty() ? 0 : (int) Math.min(Integer.MAX_VALUE, input.getAmount());
    }

    public ItemStack getOutput() { return output.copy(); }

    /**
     * Мэтчинг по жидкости бака: точное совпадение реестровой жидкости (как ключ старой
     * {@code Map<Fluid, Recipe>}) плюс достаточный уровень заполнения.
     */
    public boolean matchesFluid(Fluid tankFluid) {
        if (input.isEmpty()) return false;
        return tankFluid != null && tankFluid == input.getFluid();
    }

    @Override
    public boolean matchesRecipe(@NotNull RecipeInputWrapper container, @NotNull Level level) {
        // У солидификатора нет предметных входов — стандартный мэтчинг неприменим;
        // машина использует #matchesFluid напрямую (как MixerRecipe).
        return false;
    }

    @Override
    public ItemStack assembleSafe() { return output.copy(); }

    @Override
    public ItemStack getResultItemSafe() { return output.copy(); }

    @Override
    public RecipeSerializer<?> getSerializer() { return Serializer.INSTANCE; }

    @Override
    public RecipeType<?> getType() { return Type.INSTANCE; }

    public static class Type implements RecipeType<SolidificationRecipe> {
        public static final Type INSTANCE = new Type();
        public static final String ID = "solidification";
    }

    public static class Serializer extends PlatformRecipeSerializer<SolidificationRecipe> {
        public static final Serializer INSTANCE = new Serializer();
        //? if fabric && < 1.21.1 {
        /*public static final ResourceLocation ID = new ResourceLocation(RefStrings.MODID, "solidification");
        *///?} else {
        public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "solidification");
        //?}

        @Override
        public SolidificationRecipe readJson(ResourceLocation recipeId, JsonObject json) {
            FluidStack input = readFluidInput(json);
            ItemStack output = RecipeHooks.itemStackFromJson(GsonHelper.getAsJsonObject(json, "result"));
            return new SolidificationRecipe(recipeId, input, output);
        }

        @Override
        public SolidificationRecipe readNetwork(ResourceLocation recipeId, FriendlyByteBuf buf) {
            FluidStack input = RecipeHooks.readFluidStack(buf);
            ItemStack output = RecipeHooks.readItem(buf);
            return new SolidificationRecipe(recipeId, input, output);
        }

        @Override
        public void writeNetwork(FriendlyByteBuf buf, SolidificationRecipe recipe) {
            RecipeHooks.writeFluidStack(buf, recipe.input);
            RecipeHooks.writeItem(buf, recipe.output);
        }

        /** Жидкостный вход: { "fluid": "...", "amount": mB }. */
        private static FluidStack readFluidInput(JsonObject json) {
            JsonObject fluidObj = GsonHelper.getAsJsonObject(json, "fluid");
            ResourceLocation id = ResourceLocation.tryParse(GsonHelper.getAsString(fluidObj, "fluid"));
            long amount = GsonHelper.getAsLong(fluidObj, "amount", 0L);
            if (id == null || amount <= 0) {
                return FluidStack.empty();
            }
            return RecipeHooks.fluidStackOf(id, amount);
        }
    }
}
