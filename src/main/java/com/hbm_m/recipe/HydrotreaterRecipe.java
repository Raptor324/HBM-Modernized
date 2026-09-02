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
 * Datapack-facing рецепт гидроочистки ({@code hbm_m:hydrotreater}).
 *
 * <p>Порт 1.7.10 статического {@code com.hbm.inventory.recipes.HydrotreatingRecipes}: 100mB
 * жидкости (танк 0) за цикл под РАСХОД водорода (танк 1, давление) → десульфурированная жидкость
 * (танк 2) + кислый газ (танк 3).</p>
 *
 * <p><b>Замена статике:</b> прежний {@code HydrotreaterRecipes} хранил {@code Map<Fluid, Recipe>}
 * в Java-статике. Теперь источник правды — {@code RecipeManager}, а
 * {@code MachineHydrotreaterBlockEntity} ищет рецепт через {@link #getRecipe(Level, Fluid)}.</p>
 *
 * <p>JSON-формат:</p>
 * <pre>{@code
 * {
 *   "type": "hbm_m:hydrotreater",
 *   "input":    { "fluid": "hbm_m:oil",     "amount": 100 },
 *   "hydrogen": { "fluid": "hbm_m:hydrogen", "amount": 5 },
 *   "output":   { "fluid": "hbm_m:oil_ds",  "amount": 90 },
 *   "sour_gas": { "fluid": "hbm_m:sourgas", "amount": 15 }
 * }
 * }</pre>
 */
public class HydrotreaterRecipe extends PlatformRecipe {

    /** Входной объём одного цикла (mB) — константа поведения машины, выносится в JSON как документация. */
    public static final int OIL_PER_CYCLE_MB = 100;

    private final FluidStack input;
    private final FluidStack hydrogen;
    private final FluidStack output;
    private final FluidStack sourGas;

    public HydrotreaterRecipe(ResourceLocation id, FluidStack input, FluidStack hydrogen,
                              FluidStack output, FluidStack sourGas) {
        super(id);
        this.input = input;
        this.hydrogen = hydrogen;
        this.output = output;
        this.sourGas = sourGas;
    }

    public FluidStack getInput() { return input; }
    public Fluid getInputFluid() { return input.getFluid(); }
    public int getInputMb() { return (int) Math.min(Integer.MAX_VALUE, input.getAmount()); }

    /** Водород — расходуемый второй вход (жидкость + mB; в оригинале хранился только int). */
    public FluidStack getHydrogen() { return hydrogen; }
    public int getHydrogenMb() { return (int) Math.min(Integer.MAX_VALUE, hydrogen.getAmount()); }

    public Fluid getOutput() { return output.getFluid(); }
    public int getOutputMb() { return (int) Math.min(Integer.MAX_VALUE, output.getAmount()); }

    public Fluid getSourGas() { return sourGas.getFluid(); }
    public int getSourGasMb() { return (int) Math.min(Integer.MAX_VALUE, sourGas.getAmount()); }

    /** Совпадение входа по {@link Fluid} из бака машины (эквивалентность вещества, как в старом Map-lookup). */
    public boolean matches(Fluid fluid) {
        if (fluid == null || fluid == Fluids.EMPTY || input.isEmpty()) return false;
        return VanillaFluidEquivalence.sameSubstance(fluid, input.getFluid());
    }

    /** Data-driven поиск рецепта по входной жидкости (заменяет статический {@code HydrotreaterRecipes.get}). */
    @Nullable
    public static HydrotreaterRecipe getRecipe(Level level, Fluid input) {
        if (level == null || input == null) return null;
        for (HydrotreaterRecipe recipe : RecipeHooks.getAllRecipes(level, HydrotreaterRecipe.Type.INSTANCE)) {
            if (recipe.matches(input)) return recipe;
        }
        return null;
    }

    /** Data-driven аналог {@code HydrotreaterRecipes.has} (валидация входного бака / MK2-подключений). */
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

    public static class Type implements RecipeType<HydrotreaterRecipe> {
        public static final Type INSTANCE = new Type();
        public static final String ID = "hydrotreater";
    }

    public static class Serializer extends PlatformRecipeSerializer<HydrotreaterRecipe> {
        public static final Serializer INSTANCE = new Serializer();
        //? if fabric && < 1.21.1 {
        /*public static final ResourceLocation ID = new ResourceLocation(RefStrings.MODID, "hydrotreater");
        *///?} else {
        public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "hydrotreater");
        //?}

        @Override
        public HydrotreaterRecipe readJson(ResourceLocation recipeId, JsonObject json) {
            FluidStack input = readFluidStack(GsonHelper.getAsJsonObject(json, "input"));
            FluidStack hydrogen = readFluidStack(GsonHelper.getAsJsonObject(json, "hydrogen"));
            FluidStack output = readFluidStack(GsonHelper.getAsJsonObject(json, "output"));
            FluidStack sourGas = readFluidStack(GsonHelper.getAsJsonObject(json, "sour_gas"));
            return new HydrotreaterRecipe(recipeId, input, hydrogen, output, sourGas);
        }

        @Override
        public HydrotreaterRecipe readNetwork(ResourceLocation recipeId, FriendlyByteBuf buf) {
            FluidStack input = RecipeHooks.readFluidStack(buf);
            FluidStack hydrogen = RecipeHooks.readFluidStack(buf);
            FluidStack output = RecipeHooks.readFluidStack(buf);
            FluidStack sourGas = RecipeHooks.readFluidStack(buf);
            return new HydrotreaterRecipe(recipeId, input, hydrogen, output, sourGas);
        }

        @Override
        public void writeNetwork(FriendlyByteBuf buf, HydrotreaterRecipe recipe) {
            RecipeHooks.writeFluidStack(buf, recipe.input);
            RecipeHooks.writeFluidStack(buf, recipe.hydrogen);
            RecipeHooks.writeFluidStack(buf, recipe.output);
            RecipeHooks.writeFluidStack(buf, recipe.sourGas);
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
