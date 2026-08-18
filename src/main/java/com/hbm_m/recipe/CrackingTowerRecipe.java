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
 * Datapack-facing рецепт крекинговой башни ({@code hbm_m:cracking_tower}).
 *
 * <p>Порт 1.7.10 статического {@code com.hbm.inventory.recipes.CrackingRecipes}: 100mB входной
 * жидкости + 200mB пара ({@link #STEAM_PER_100_INPUT}) → две более лёгкие фракции + 2mB отработанного
 * пара ({@link #SPENTSTEAM_PRODUCED}). Второй выход может отсутствовать (оригинал:
 * {@code Fluids.NONE} — тогда танк 3 не задействуется).</p>
 *
 * <p><b>Замена статике:</b> прежний {@code CrackingTowerRecipes} хранил {@code Map<Fluid, Crack>}
 * в Java-статике. Теперь источник правды — {@code RecipeManager}, а
 * {@code MachineCrackingTowerBlockEntity} ищет рецепт через {@link #getRecipe(Level, Fluid)}.</p>
 *
 * <p>JSON-формат:</p>
 * <pre>{@code
 * {
 *   "type": "hbm_m:cracking_tower",
 *   "input":    { "fluid": "hbm_m:oil_base", "amount": 100 },
 *   "output_a": { "fluid": "hbm_m:crackoil", "amount": 80 },
 *   "output_b": { "fluid": "hbm_m:petroleum", "amount": 20 }   // необязательно: отсутствие => нет второго выхода
 * }
 * }</pre>
 */
public class CrackingTowerRecipe extends PlatformRecipe {

    /** Пара (mB), расходуемая на 100mB входа — как в оригинальном {@code CrackingRecipes}. */
    public static final int STEAM_PER_100_INPUT = 200;
    /** Отработанный пар (mB), производимый на один крэк — как в оригинале. */
    public static final int SPENTSTEAM_PRODUCED = 2;

    /** Входной объём одного крэка (mB) — константа поведения машины, выносится в JSON как документация. */
    public static final int INPUT_PER_CRACK_MB = 100;

    private final FluidStack input;
    private final FluidStack outputA;
    @Nullable
    private final FluidStack outputB;

    public CrackingTowerRecipe(ResourceLocation id, FluidStack input, FluidStack outputA, @Nullable FluidStack outputB) {
        super(id);
        this.input = input;
        this.outputA = outputA;
        // NONE/пустой второй выход нормализуется в «нет второго выхода» (аналог Fluids.NONE оригинала).
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
     * Совпадение входа по {@link Fluid} из бака машины. Как и в старом Map-lookup, жидкости
     * сравниваются по эквивалентности вещества ({@link VanillaFluidEquivalence#sameSubstance}).
     */
    public boolean matches(Fluid fluid) {
        if (fluid == null || fluid == Fluids.EMPTY || input.isEmpty()) return false;
        return VanillaFluidEquivalence.sameSubstance(fluid, input.getFluid());
    }

    /** Data-driven поиск рецепта по входной жидкости (заменяет статический {@code CrackingTowerRecipes.get}). */
    @Nullable
    public static CrackingTowerRecipe getRecipe(Level level, Fluid input) {
        if (level == null || input == null) return null;
        for (CrackingTowerRecipe recipe : RecipeHooks.getAllRecipes(level, CrackingTowerRecipe.Type.INSTANCE)) {
            if (recipe.matches(input)) return recipe;
        }
        return null;
    }

    /** Data-driven аналог {@code CrackingTowerRecipes.has} (валидация входного бака / MK2-подключений). */
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

    public static class Type implements RecipeType<CrackingTowerRecipe> {
        public static final Type INSTANCE = new Type();
        public static final String ID = "cracking_tower";
    }

    public static class Serializer extends PlatformRecipeSerializer<CrackingTowerRecipe> {
        public static final Serializer INSTANCE = new Serializer();
        //? if fabric && < 1.21.1 {
        /*public static final ResourceLocation ID = new ResourceLocation(RefStrings.MODID, "cracking_tower");
        *///?} else {
        public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "cracking_tower");
        //?}

        @Override
        public CrackingTowerRecipe readJson(ResourceLocation recipeId, JsonObject json) {
            FluidStack input = readFluidStack(GsonHelper.getAsJsonObject(json, "input"));
            FluidStack outputA = readFluidStack(GsonHelper.getAsJsonObject(json, "output_a"));
            FluidStack outputB = json.has("output_b") ? readFluidStack(GsonHelper.getAsJsonObject(json, "output_b")) : null;
            return new CrackingTowerRecipe(recipeId, input, outputA, outputB);
        }

        @Override
        public CrackingTowerRecipe readNetwork(ResourceLocation recipeId, FriendlyByteBuf buf) {
            FluidStack input = RecipeHooks.readFluidStack(buf);
            FluidStack outputA = RecipeHooks.readFluidStack(buf);
            FluidStack outputB = RecipeHooks.readFluidStack(buf);
            return new CrackingTowerRecipe(recipeId, input, outputA, outputB);
        }

        @Override
        public void writeNetwork(FriendlyByteBuf buf, CrackingTowerRecipe recipe) {
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
