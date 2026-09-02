package com.hbm_m.recipe;

import javax.annotation.Nullable;

import org.jetbrains.annotations.NotNull;

import com.google.gson.JsonObject;
import com.hbm_m.api.fluids.VanillaFluidEquivalence;
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
 * Datapack-facing рецепт каталитического риформера ({@code hbm_m:catalytic_reformer}).
 *
 * <p>Порт 1.7.10 статического {@code com.hbm.inventory.recipes.ReformingRecipes}: 100mB жидкости
 * (танк 0) за цикл → ТРИ выходные жидкости (танки 1-3; в отличие от гидроочистки водород здесь
 * ПРОИЗВОДИТСЯ, а не расходуется).</p>
 *
 * <p><b>Замена статике:</b> прежний {@code CatalyticReformerRecipes} хранил {@code Map<Fluid, Triple>}
 * в Java-статике. Теперь источник правды — {@code RecipeManager}, а
 * {@code MachineCatalyticReformerBlockEntity} ищет рецепт через {@link #getRecipe(Level, Fluid)}.</p>
 *
 * <p>JSON-формат:</p>
 * <pre>{@code
 * {
 *   "type": "hbm_m:catalytic_reformer",
 *   "input":    { "fluid": "hbm_m:heatingoil", "amount": 100 },
 *   "output_a": { "fluid": "hbm_m:naphtha",    "amount": 50 },
 *   "output_b": { "fluid": "hbm_m:petroleum",  "amount": 15 },
 *   "output_c": { "fluid": "hbm_m:hydrogen",   "amount": 10 }
 * }
 * }</pre>
 */
public class CatalyticReformerRecipe extends PlatformRecipe {

    /** Входной объём одного цикла (mB) — константа поведения машины, выносится в JSON как документация. */
    public static final int INPUT_PER_CYCLE_MB = 100;

    private final FluidStack input;
    private final FluidStack outputA;
    private final FluidStack outputB;
    private final FluidStack outputC;

    public CatalyticReformerRecipe(ResourceLocation id, FluidStack input,
                                    FluidStack outputA, FluidStack outputB, FluidStack outputC) {
        super(id);
        this.input = input;
        this.outputA = outputA;
        this.outputB = outputB;
        this.outputC = outputC;
    }

    public FluidStack getInput() { return input; }
    public Fluid getInputFluid() { return input.getFluid(); }
    public int getInputMb() { return (int) Math.min(Integer.MAX_VALUE, input.getAmount()); }

    public Fluid getOutputA() { return outputA.getFluid(); }
    public int getOutputAMb() { return (int) Math.min(Integer.MAX_VALUE, outputA.getAmount()); }

    public Fluid getOutputB() { return outputB.getFluid(); }
    public int getOutputBMb() { return (int) Math.min(Integer.MAX_VALUE, outputB.getAmount()); }

    public Fluid getOutputC() { return outputC.getFluid(); }
    public int getOutputCMb() { return (int) Math.min(Integer.MAX_VALUE, outputC.getAmount()); }

    /** Совпадение входа по {@link Fluid} из бака машины (эквивалентность вещества, как в старом Map-lookup). */
    public boolean matches(Fluid fluid) {
        if (fluid == null || fluid == Fluids.EMPTY || input.isEmpty()) return false;
        return VanillaFluidEquivalence.sameSubstance(fluid, input.getFluid());
    }

    /** Data-driven поиск рецепта по входной жидкости (заменяет статический {@code CatalyticReformerRecipes.get}). */
    @Nullable
    public static CatalyticReformerRecipe getRecipe(Level level, Fluid input) {
        if (level == null || input == null) return null;
        for (CatalyticReformerRecipe recipe : RecipeHooks.getAllRecipes(level, CatalyticReformerRecipe.Type.INSTANCE)) {
            if (recipe.matches(input)) return recipe;
        }
        return null;
    }

    /** Data-driven аналог {@code CatalyticReformerRecipes.has} (валидация входного бака / MK2-подключений). */
    public static boolean hasRecipe(Level level, Fluid input) {
        return getRecipe(level, input) != null;
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

    public static class Type implements RecipeType<CatalyticReformerRecipe> {
        public static final Type INSTANCE = new Type();
        public static final String ID = "catalytic_reformer";
    }

    public static class Serializer extends PlatformRecipeSerializer<CatalyticReformerRecipe> {
        public static final Serializer INSTANCE = new Serializer();
        //? if fabric && < 1.21.1 {
        /*public static final ResourceLocation ID = new ResourceLocation(RefStrings.MODID, "catalytic_reformer");
        *///?} else {
        public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "catalytic_reformer");
        //?}

        @Override
        public CatalyticReformerRecipe readJson(ResourceLocation recipeId, JsonObject json) {
            FluidStack input = readFluidStack(GsonHelper.getAsJsonObject(json, "input"));
            FluidStack outputA = readFluidStack(GsonHelper.getAsJsonObject(json, "output_a"));
            FluidStack outputB = readFluidStack(GsonHelper.getAsJsonObject(json, "output_b"));
            FluidStack outputC = readFluidStack(GsonHelper.getAsJsonObject(json, "output_c"));
            return new CatalyticReformerRecipe(recipeId, input, outputA, outputB, outputC);
        }

        @Override
        public CatalyticReformerRecipe readNetwork(ResourceLocation recipeId, FriendlyByteBuf buf) {
            FluidStack input = RecipeHooks.readFluidStack(buf);
            FluidStack outputA = RecipeHooks.readFluidStack(buf);
            FluidStack outputB = RecipeHooks.readFluidStack(buf);
            FluidStack outputC = RecipeHooks.readFluidStack(buf);
            return new CatalyticReformerRecipe(recipeId, input, outputA, outputB, outputC);
        }

        @Override
        public void writeNetwork(FriendlyByteBuf buf, CatalyticReformerRecipe recipe) {
            RecipeHooks.writeFluidStack(buf, recipe.input);
            RecipeHooks.writeFluidStack(buf, recipe.outputA);
            RecipeHooks.writeFluidStack(buf, recipe.outputB);
            RecipeHooks.writeFluidStack(buf, recipe.outputC);
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
