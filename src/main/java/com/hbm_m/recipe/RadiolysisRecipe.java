package com.hbm_m.recipe;

import javax.annotation.Nullable;

import org.jetbrains.annotations.NotNull;

import com.google.gson.JsonObject;
import com.hbm_m.api.fluids.VanillaFluidEquivalence;
import com.hbm_m.inventory.fluid.ModFluids;
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
import net.minecraft.world.level.material.Fluids;

/**
 * Datapack-facing рецепт радиолиза ({@code hbm_m:radiolysis}).
 *
 * <p>Порт 1.7.10 {@code com.hbm.inventory.recipes.RadiolysisRecipes}. Оригинал содержал ОДИН
 * собственный рецепт — вода → пероксид + водород — а всё остальное делегировал таблице крекинга
 * ({@code CrackingRecipes}). Делегирование сохранено на уровне машины:
 * {@code MachineRadiolysisBlockEntity} сначала ищет {@link RadiolysisRecipe}, затем падает в
 * {@link CrackingTowerRecipe#getRecipe(Level, Fluid)} — ровно как раньше.</p>
 *
 * <p><b>Замена статике:</b> прежний {@code RadiolysisRecipes} хардкодил водный рецепт в Java.
 * Теперь источник правды — {@code RecipeManager} (JSON).</p>
 *
 * <p>JSON-формат:</p>
 * <pre>{@code
 * {
 *   "type": "hbm_m:radiolysis",
 *   "input":    { "fluid": "hbm_m:water",    "amount": 100 },
 *   "output_a": { "fluid": "hbm_m:peroxide", "amount": 80 },
 *   "output_b": { "fluid": "hbm_m:hydrogen", "amount": 20 }
 * }
 * }</pre>
 */
public class RadiolysisRecipe extends PlatformRecipe {

    /** Входной объём одного крэка (mB) — константа поведения машины, выносится в JSON как документация. */
    public static final int INPUT_PER_CRACK_MB = 100;

    private final FluidStack input;
    private final FluidStack outputA;
    @Nullable
    private final FluidStack outputB;

    public RadiolysisRecipe(ResourceLocation id, FluidStack input, FluidStack outputA, @Nullable FluidStack outputB) {
        super(id);
        this.input = input;
        this.outputA = outputA;
        // NONE/пустой второй выход нормализуется в «нет второго выхода».
        this.outputB = (outputB == null || outputB.isEmpty() || outputB.getAmount() <= 0
                || outputB.getFluid() == ModFluids.NONE.getSource()) ? null : outputB;
    }

    public FluidStack getInput() { return input; }
    public Fluid getInputFluid() { return input.getFluid(); }
    public int getInputMb() { return (int) Math.min(Integer.MAX_VALUE, input.getAmount()); }

    public Fluid getOutputA() { return outputA.getFluid(); }
    public int getOutputAMb() { return (int) Math.min(Integer.MAX_VALUE, outputA.getAmount()); }

    /** Второй выход отсутствует, если в JSON его нет (или указан {@code hbm_m:none}). */
    public boolean hasOutputB() { return outputB != null; }
    @Nullable public Fluid getOutputB() { return outputB != null ? outputB.getFluid() : null; }
    public int getOutputBMb() { return outputB != null ? (int) Math.min(Integer.MAX_VALUE, outputB.getAmount()) : 0; }

    /**
     * Совпадение входа по {@link Fluid} из бака машины. Оригинал сравнивал
     * {@code input.isSame(ModFluids.WATER.getSource())}; здесь — эквивалентность вещества
     * ({@link VanillaFluidEquivalence#sameSubstance}), т.е. ванильная и HBM-вода равнозначны.
     */
    public boolean matches(Fluid fluid) {
        if (fluid == null || fluid == Fluids.EMPTY || input.isEmpty()) return false;
        return VanillaFluidEquivalence.sameSubstance(fluid, input.getFluid());
    }

    /** Data-driven поиск собственного рецепта радиолиза (без крэкинг-fallback — он остаётся в машине). */
    @Nullable
    public static RadiolysisRecipe getRecipe(Level level, Fluid input) {
        if (level == null || input == null) return null;
        for (RadiolysisRecipe recipe : RecipeHooks.getAllRecipes(level, RadiolysisRecipe.Type.INSTANCE)) {
            if (recipe.matches(input)) return recipe;
        }
        return null;
    }

    @Override
    public boolean matchesRecipe(@NotNull RecipeInputWrapper container, @NotNull Level level) {
        // Машина работает позиционно по своим бакам — стандартный контейнерный мэтчинг не используется.
        return false;
    }

    @Override
    public ItemStack assembleSafe() { return ItemStack.EMPTY; }

    @Override
    public ItemStack getResultItemSafe() { return ItemStack.EMPTY; }

    @Override
    public RecipeSerializer<?> getSerializer() { return Serializer.INSTANCE; }

    @Override
    public RecipeType<?> getType() { return Type.INSTANCE; }

    public static class Type implements RecipeType<RadiolysisRecipe> {
        public static final Type INSTANCE = new Type();
        public static final String ID = "radiolysis";
    }

    public static class Serializer extends PlatformRecipeSerializer<RadiolysisRecipe> {
        public static final Serializer INSTANCE = new Serializer();
        //? if fabric && < 1.21.1 {
        /*public static final ResourceLocation ID = new ResourceLocation(RefStrings.MODID, "radiolysis");
        *///?} else {
        public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "radiolysis");
        //?}

        @Override
        public RadiolysisRecipe readJson(ResourceLocation recipeId, JsonObject json) {
            FluidStack input = readFluidStack(GsonHelper.getAsJsonObject(json, "input"));
            FluidStack outputA = readFluidStack(GsonHelper.getAsJsonObject(json, "output_a"));
            FluidStack outputB = json.has("output_b") ? readFluidStack(GsonHelper.getAsJsonObject(json, "output_b")) : null;
            return new RadiolysisRecipe(recipeId, input, outputA, outputB);
        }

        @Override
        public RadiolysisRecipe readNetwork(ResourceLocation recipeId, FriendlyByteBuf buf) {
            FluidStack input = RecipeHooks.readFluidStack(buf);
            FluidStack outputA = RecipeHooks.readFluidStack(buf);
            FluidStack outputB = RecipeHooks.readFluidStack(buf);
            return new RadiolysisRecipe(recipeId, input, outputA, outputB);
        }

        @Override
        public void writeNetwork(FriendlyByteBuf buf, RadiolysisRecipe recipe) {
            RecipeHooks.writeFluidStack(buf, recipe.input);
            RecipeHooks.writeFluidStack(buf, recipe.outputA);
            RecipeHooks.writeFluidStack(buf, recipe.outputB != null ? recipe.outputB : FluidStack.empty());
        }

        /** Парсинг {@code { "fluid": <id>, "amount": <mB> }} через {@link RecipeHooks#fluidStackOf}. */
        private static FluidStack readFluidStack(JsonObject json) {
            ResourceLocation id = ResourceLocation.tryParse(GsonHelper.getAsString(json, "fluid"));
            if (id == null) return FluidStack.empty();
            long amount = GsonHelper.getAsLong(json, "amount", 0L);
            return RecipeHooks.fluidStackOf(id, amount);
        }
    }
}
