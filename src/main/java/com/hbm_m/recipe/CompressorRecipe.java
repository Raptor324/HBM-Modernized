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
 * Datapack-facing спец-рецепт компрессора ({@code hbm_m:compressor}).
 *
 * <p>Порт 1.7.10 статического {@code com.hbm.inventory.recipes.CompressorRecipes}: ключ —
 * пара (жидкость, давление входного бака). Поведение машины по умолчанию (рецепт не найден) —
 * генерическое сжатие 1000mB той же жидкости с повышением давления на 1; только эти спец-пары
 * переопределяют умолчание. Длительность переносится в JSON для полноты (машина этого порта
 * использует фиксированный базовый таймер).</p>
 *
 * <p><b>Замена статике:</b> прежний {@code CompressorRecipes} хранил {@code Map<Key, Recipe>}
 * в Java-статике. Теперь источник правды — {@code RecipeManager}, а
 * {@code MachineCompressorBlockEntity} ищет рецепт через {@link #getRecipe(Level, Fluid, int)}.</p>
 *
 * <p>JSON-формат:</p>
 * <pre>{@code
 * {
 *   "type": "hbm_m:compressor",
 *   "input":          { "fluid": "hbm_m:petroleum", "amount": 2000 },
 *   "input_pressure": 0,
 *   "output":         { "fluid": "hbm_m:lpg", "amount": 1000 },
 *   "output_pressure": 1,
 *   "duration": 20
 * }
 * }</pre>
 */
public class CompressorRecipe extends PlatformRecipe {

    private final FluidStack input;
    private final int inputPressure;
    private final FluidStack output;
    private final int outputPressure;
    private final int duration;

    public CompressorRecipe(ResourceLocation id, FluidStack input, int inputPressure,
                            FluidStack output, int outputPressure, int duration) {
        super(id);
        this.input = input;
        this.inputPressure = Math.max(0, inputPressure);
        this.output = output;
        this.outputPressure = Math.max(0, outputPressure);
        this.duration = Math.max(1, duration);
    }

    public FluidStack getInput() { return input; }
    public Fluid getInputFluid() { return input.getFluid(); }
    public int getInputMb() { return (int) Math.min(Integer.MAX_VALUE, input.getAmount()); }

    /** Давление входного бака, при котором рецепт активен (ключ оригинальной Map). */
    public int getInputPressure() { return inputPressure; }

    public Fluid getOutputFluid() { return output.getFluid(); }
    public int getOutputMb() { return (int) Math.min(Integer.MAX_VALUE, output.getAmount()); }

    /** Давление, которое выставляется выходному баку при этом рецепте. */
    public int getOutputPressure() { return outputPressure; }

    /** Длительность процесса (тики) — переносится 1:1 из оригинала. */
    public int getDuration() { return duration; }

    /** Совпадение входа по {@link Fluid} из бака машины (без учёта давления — см. {@link #matches(Fluid, int)}). */
    public boolean matches(Fluid fluid) {
        if (fluid == null || fluid == Fluids.EMPTY || input.isEmpty()) return false;
        return VanillaFluidEquivalence.sameSubstance(fluid, input.getFluid());
    }

    /** Полный ключ оригинальной Map: жидкость + давление входного бака. */
    public boolean matches(Fluid fluid, int pressure) {
        return matches(fluid) && inputPressure == pressure;
    }

    /** Data-driven поиск спец-рецепта по паре (жидкость, давление) — заменяет {@code CompressorRecipes.get}. */
    @Nullable
    public static CompressorRecipe getRecipe(Level level, Fluid input, int pressure) {
        if (level == null || input == null) return null;
        for (CompressorRecipe recipe : RecipeHooks.getAllRecipes(level, CompressorRecipe.Type.INSTANCE)) {
            if (recipe.matches(input, pressure)) return recipe;
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

    public static class Type implements RecipeType<CompressorRecipe> {
        public static final Type INSTANCE = new Type();
        public static final String ID = "compressor";
    }

    public static class Serializer extends PlatformRecipeSerializer<CompressorRecipe> {
        public static final Serializer INSTANCE = new Serializer();
        //? if fabric && < 1.21.1 {
        /*public static final ResourceLocation ID = new ResourceLocation(RefStrings.MODID, "compressor");
        *///?} else {
        public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "compressor");
        //?}

        @Override
        public CompressorRecipe readJson(ResourceLocation recipeId, JsonObject json) {
            FluidStack input = readFluidStack(GsonHelper.getAsJsonObject(json, "input"));
            int inputPressure = GsonHelper.getAsInt(json, "input_pressure", 0);
            FluidStack output = readFluidStack(GsonHelper.getAsJsonObject(json, "output"));
            int outputPressure = GsonHelper.getAsInt(json, "output_pressure", 0);
            int duration = GsonHelper.getAsInt(json, "duration", 100);
            return new CompressorRecipe(recipeId, input, inputPressure, output, outputPressure, duration);
        }

        @Override
        public CompressorRecipe readNetwork(ResourceLocation recipeId, FriendlyByteBuf buf) {
            FluidStack input = RecipeHooks.readFluidStack(buf);
            int inputPressure = buf.readVarInt();
            FluidStack output = RecipeHooks.readFluidStack(buf);
            int outputPressure = buf.readVarInt();
            int duration = buf.readVarInt();
            return new CompressorRecipe(recipeId, input, inputPressure, output, outputPressure, duration);
        }

        @Override
        public void writeNetwork(FriendlyByteBuf buf, CompressorRecipe recipe) {
            RecipeHooks.writeFluidStack(buf, recipe.input);
            buf.writeVarInt(recipe.inputPressure);
            RecipeHooks.writeFluidStack(buf, recipe.output);
            buf.writeVarInt(recipe.outputPressure);
            buf.writeVarInt(recipe.duration);
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
