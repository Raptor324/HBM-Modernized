package com.hbm_m.recipe;

import javax.annotation.Nullable;

import org.jetbrains.annotations.NotNull;

import com.google.gson.JsonObject;
import com.hbm_m.api.fluids.VanillaFluidEquivalence;
import com.hbm_m.inventory.fluid.tank.FluidTank;
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
 * Datapack-рецепт кокера ({@code hbm_m:coker}).
 *
 * <p>Порт статического {@code CokerRecipes}: входная жидкость (mB, бак 0 машины) → предметный выход
 * (слот 1) + опциональная побочная жидкость (mB, бак 1). Длительность фиксирована машиной
 * ({@code PROCESS_TIME = 20_000}), поэтому в JSON не хранится.</p>
 *
 * <p><b>Замена статике:</b> большинство исходных рецептов рассчитывались автоматически из
 * {@code FT_Flammable}/{@code FT_Combustible}-энергии жидкости ({@code registerAuto}: 820.000 TU =
 * 1 бонусный кокс; формула: {@code mB = tuPerSF * 1000 / tuPerBucket} с округлением вниз до
 * 1000/100/10 mB, побочный выход {@code max(10, mB / 10)}). В датагене эти значения запечены
 * литералами 1:1 (см. {@code CokerRecipeGenerator}), повторное вычисление не выполняется.</p>
 *
 * <p>JSON-формат:</p>
 * <pre>{@code
 * {
 *   "type": "hbm_m:coker",
 *   "input":     { "fluid": "hbm_m:heavyoil", "amount": 11000 },
 *   "result":    { "item": "hbm_m:coke_petroleum" },
 *   "byproduct": { "fluid": "hbm_m:oil_coker", "amount": 1100 }   // optional
 * }
 * }</pre>
 */
public class CokerRecipe extends PlatformRecipe {

    private final FluidStack input;
    private final ItemStack output;
    @Nullable
    private final FluidStack byproduct;

    public CokerRecipe(ResourceLocation id, FluidStack input, ItemStack output, @Nullable FluidStack byproduct) {
        super(id);
        this.input = input;
        this.output = output;
        this.byproduct = byproduct != null && !byproduct.isEmpty() && byproduct.getAmount() > 0 ? byproduct : null;
    }

    public FluidStack getInput() { return input; }
    public Fluid getInputFluid() { return input.getFluid(); }
    public int getInputMb() { return (int) Math.min(Integer.MAX_VALUE, input.getAmount()); }
    public ItemStack getOutput() { return output.copy(); }
    @Nullable public FluidStack getByproduct() { return byproduct; }
    @Nullable public Fluid getByproductFluid() { return byproduct != null ? byproduct.getFluid() : null; }
    public int getByproductMb() { return byproduct != null ? (int) Math.min(Integer.MAX_VALUE, byproduct.getAmount()) : 0; }

    /** Совпадение входного бака по типу жидкости (без проверки заполнения) — для поиска рецепта машиной. */
    public boolean matchesFluidType(Fluid fluid) {
        return VanillaFluidEquivalence.sameSubstance(fluid, input.getFluid());
    }

    @Override
    public boolean matchesRecipe(@NotNull RecipeInputWrapper container, @NotNull Level level) {
        // Кокер работает по баку (Heat-driven), предметных входов нет.
        return !level.isClientSide();
    }

    @Override
    public ItemStack assembleSafe() {
        return getResultItemSafe();
    }

    @Override
    public ItemStack getResultItemSafe() {
        return output.copy();
    }

    @Override
    public RecipeSerializer<?> getSerializer() { return Serializer.INSTANCE; }

    @Override
    public RecipeType<?> getType() { return Type.INSTANCE; }

    public static class Type implements RecipeType<CokerRecipe> {
        public static final Type INSTANCE = new Type();
        public static final String ID = "coker";
    }

    public static class Serializer extends PlatformRecipeSerializer<CokerRecipe> {
        public static final Serializer INSTANCE = new Serializer();
        //? if fabric && < 1.21.1 {
        /*public static final ResourceLocation ID = new ResourceLocation(RefStrings.MODID, "coker");
        *///?} else {
        public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "coker");
        //?}

        @Override
        public CokerRecipe readJson(ResourceLocation recipeId, JsonObject json) {
            FluidStack input = readFluid(GsonHelper.getAsJsonObject(json, "input"));
            ItemStack output = RecipeHooks.itemStackFromJson(GsonHelper.getAsJsonObject(json, "result"));
            FluidStack byproduct = json.has("byproduct") ? readFluid(GsonHelper.getAsJsonObject(json, "byproduct")) : null;
            return new CokerRecipe(recipeId, input, output, byproduct);
        }

        @Override
        public CokerRecipe readNetwork(ResourceLocation recipeId, FriendlyByteBuf buf) {
            FluidStack input = RecipeHooks.readFluidStack(buf);
            ItemStack output = RecipeHooks.readItem(buf);
            FluidStack byproduct = buf.readBoolean() ? RecipeHooks.readFluidStack(buf) : null;
            return new CokerRecipe(recipeId, input, output, byproduct);
        }

        @Override
        public void writeNetwork(FriendlyByteBuf buf, CokerRecipe recipe) {
            RecipeHooks.writeFluidStack(buf, recipe.input);
            RecipeHooks.writeItem(buf, recipe.output);
            buf.writeBoolean(recipe.byproduct != null);
            if (recipe.byproduct != null) RecipeHooks.writeFluidStack(buf, recipe.byproduct);
        }

        /** { "fluid": <id>, "amount": <mB> } — единый формат мода (см. ChemicalPlantRecipe). */
        private static FluidStack readFluid(JsonObject obj) {
            ResourceLocation id = ResourceLocation.tryParse(GsonHelper.getAsString(obj, "fluid"));
            long amount = GsonHelper.getAsLong(obj, "amount", 0L);
            return RecipeHooks.fluidStackOf(id, amount);
        }
    }
}
