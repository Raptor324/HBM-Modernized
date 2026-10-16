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
 * Datapack-facing рецепт вакуумной дистилляции ({@code hbm_m:vacuum_distill}).
 *
 * <p>Порт 1.7.10 статического {@code com.hbm.inventory.recipes.VacuumRefineryRecipes}: 100mB
 * сырой нефти (танк 0) за такт → четыре фракции (тяжёлая вакуумная, реформат, лёгкая вакуумная,
 * кислый газ/реформат-газ). Объёмы фракций фиксированы константами {@link #HEAVY_MB}/
 * {@link #REFORMATE_MB}/{@link #LIGHT_MB}/{@link #SOUR_MB} и выносятся в JSON.</p>
 *
 * <p><b>Замена статике:</b> прежний {@code VacuumDistillRecipes} хранил {@code Map<Fluid, Output>}
 * в Java-статике. Теперь источник правды — {@code RecipeManager}, а
 * {@code MachineVacuumDistillBlockEntity} ищет рецепт через {@link #getRecipe(Level, Fluid)}.</p>
 *
 * <p>JSON-формат:</p>
 * <pre>{@code
 * {
 *   "type": "hbm_m:vacuum_distill",
 *   "input":     { "fluid": "hbm_m:oil",   "amount": 100 },
 *   "heavy":     { "fluid": "hbm_m:heavyoil_vacuum", "amount": 40 },
 *   "reformate": { "fluid": "hbm_m:reformate",       "amount": 25 },
 *   "light":     { "fluid": "hbm_m:lightoil_vacuum", "amount": 20 },
 *   "sour":      { "fluid": "hbm_m:sourgas",         "amount": 15 }
 * }
 * }</pre>
 */
public class VacuumDistillRecipe extends PlatformRecipe {

    /** mB тяжёлой фракции за цикл (танк 1) — как в оригинальном {@code VacuumRefineryRecipes}. */
    public static final int HEAVY_MB = 40;
    /** mB реформата за цикл (танк 2). */
    public static final int REFORMATE_MB = 25;
    /** mB лёгкой фракции за цикл (танк 3). */
    public static final int LIGHT_MB = 20;
    /** mB кислого газа за цикл (танк 4). */
    public static final int SOUR_MB = 15;

    /** Входной объём одного цикла (mB) — константа поведения машины, выносится в JSON как документация. */
    public static final int OIL_PER_CYCLE_MB = 100;

    private final FluidStack input;
    private final FluidStack heavy;
    private final FluidStack reformate;
    private final FluidStack light;
    private final FluidStack sour;

    public VacuumDistillRecipe(ResourceLocation id, FluidStack input,
                               FluidStack heavy, FluidStack reformate, FluidStack light, FluidStack sour) {
        super(id);
        this.input = input;
        this.heavy = heavy;
        this.reformate = reformate;
        this.light = light;
        this.sour = sour;
    }

    public FluidStack getInput() { return input; }
    public Fluid getInputFluid() { return input.getFluid(); }
    public int getInputMb() { return (int) Math.min(Integer.MAX_VALUE, input.getAmount()); }

    public Fluid getHeavy() { return heavy.getFluid(); }
    public int getHeavyMb() { return (int) Math.min(Integer.MAX_VALUE, heavy.getAmount()); }

    public Fluid getReformate() { return reformate.getFluid(); }
    public int getReformateMb() { return (int) Math.min(Integer.MAX_VALUE, reformate.getAmount()); }

    public Fluid getLight() { return light.getFluid(); }
    public int getLightMb() { return (int) Math.min(Integer.MAX_VALUE, light.getAmount()); }

    public Fluid getSour() { return sour.getFluid(); }
    public int getSourMb() { return (int) Math.min(Integer.MAX_VALUE, sour.getAmount()); }

    /** Совпадение входа по {@link Fluid} из бака машины (эквивалентность вещества, как в старом Map-lookup). */
    public boolean matches(Fluid fluid) {
        if (fluid == null || fluid == Fluids.EMPTY || input.isEmpty()) return false;
        return VanillaFluidEquivalence.sameSubstance(fluid, input.getFluid());
    }

    /** Data-driven поиск рецепта по входной жидкости (заменяет статический {@code VacuumDistillRecipes.get}). */
    @Nullable
    public static VacuumDistillRecipe getRecipe(Level level, Fluid input) {
        if (level == null || input == null) return null;
        for (VacuumDistillRecipe recipe : RecipeHooks.getAllRecipes(level, VacuumDistillRecipe.Type.INSTANCE)) {
            if (recipe.matches(input)) return recipe;
        }
        return null;
    }

    /** Data-driven аналог {@code VacuumDistillRecipes.has} (валидация входного бака / MK2-подключений). */
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

    public static class Type implements RecipeType<VacuumDistillRecipe> {
        public static final Type INSTANCE = new Type();
        public static final String ID = "vacuum_distill";
    }

    public static class Serializer extends PlatformRecipeSerializer<VacuumDistillRecipe> {
        public static final Serializer INSTANCE = new Serializer();
        //? if fabric && < 1.21.1 {
        /*public static final ResourceLocation ID = new ResourceLocation(RefStrings.MODID, "vacuum_distill");
        *///?} else {
        public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "vacuum_distill");
        //?}

        @Override
        public VacuumDistillRecipe readJson(ResourceLocation recipeId, JsonObject json) {
            FluidStack input = readFluidStack(GsonHelper.getAsJsonObject(json, "input"));
            FluidStack heavy = readFluidStack(GsonHelper.getAsJsonObject(json, "heavy"));
            FluidStack reformate = readFluidStack(GsonHelper.getAsJsonObject(json, "reformate"));
            FluidStack light = readFluidStack(GsonHelper.getAsJsonObject(json, "light"));
            FluidStack sour = readFluidStack(GsonHelper.getAsJsonObject(json, "sour"));
            return new VacuumDistillRecipe(recipeId, input, heavy, reformate, light, sour);
        }

        @Override
        public VacuumDistillRecipe readNetwork(ResourceLocation recipeId, FriendlyByteBuf buf) {
            FluidStack input = RecipeHooks.readFluidStack(buf);
            FluidStack heavy = RecipeHooks.readFluidStack(buf);
            FluidStack reformate = RecipeHooks.readFluidStack(buf);
            FluidStack light = RecipeHooks.readFluidStack(buf);
            FluidStack sour = RecipeHooks.readFluidStack(buf);
            return new VacuumDistillRecipe(recipeId, input, heavy, reformate, light, sour);
        }

        @Override
        public void writeNetwork(FriendlyByteBuf buf, VacuumDistillRecipe recipe) {
            RecipeHooks.writeFluidStack(buf, recipe.input);
            RecipeHooks.writeFluidStack(buf, recipe.heavy);
            RecipeHooks.writeFluidStack(buf, recipe.reformate);
            RecipeHooks.writeFluidStack(buf, recipe.light);
            RecipeHooks.writeFluidStack(buf, recipe.sour);
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
