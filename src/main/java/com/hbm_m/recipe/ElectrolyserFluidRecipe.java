package com.hbm_m.recipe;

import javax.annotation.Nullable;

import org.jetbrains.annotations.NotNull;

import com.google.gson.JsonArray;
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
 * Datapack-рецепт электролизёра, Fluid-режим ({@code hbm_m:electrolyser_fluid}).
 *
 * <p>Порт статического {@code ElectrolyserRecipes} (Fluid-часть): входная жидкость (mB) → до двух
 * выходных жидкостей (mB, баки 1/2 машины) + до трёх побочных предметов (слоты 2..4).</p>
 *
 * <p><b>Замена статике:</b> прежний {@code ElectrolyserRecipes} держал {@code Map<Fluid, FluidRecipe>}
 * в Java-статике. Теперь источник правды — {@code RecipeManager}, а
 * {@code MachineElectrolyserBlockEntity.tick} ищет рецепт через
 * {@link RecipeHooks#getAllRecipes(Level, RecipeType)}.</p>
 *
 * <p>JSON-формат:</p>
 * <pre>{@code
 * {
 *   "type": "hbm_m:electrolyser_fluid",
 *   "input":    { "fluid": "hbm_m:water", "amount": 2000 },
 *   "output_a": { "fluid": "hbm_m:hydrogen", "amount": 200 },   // optional
 *   "output_b": { "fluid": "hbm_m:oxygen",   "amount": 200 },   // optional
 *   "byproducts": [ { "item": "...", "count": 1 } ]             // optional, до 3
 * }
 * }</pre>
 */
public class ElectrolyserFluidRecipe extends PlatformRecipe {

    private final FluidStack input;
    @Nullable
    private final FluidStack outputA;
    @Nullable
    private final FluidStack outputB;
    private final ItemStack[] byproducts;

    public ElectrolyserFluidRecipe(ResourceLocation id, FluidStack input,
                                    @Nullable FluidStack outputA, @Nullable FluidStack outputB,
                                    ItemStack[] byproducts) {
        super(id);
        this.input = input;
        this.outputA = outputA != null && !outputA.isEmpty() && outputA.getAmount() > 0 ? outputA : null;
        this.outputB = outputB != null && !outputB.isEmpty() && outputB.getAmount() > 0 ? outputB : null;
        this.byproducts = byproducts != null ? byproducts : new ItemStack[0];
    }

    public FluidStack getInput() { return input; }
    public Fluid getInputFluid() { return input.getFluid(); }
    public int getInputAmount() { return (int) Math.min(Integer.MAX_VALUE, input.getAmount()); }
    @Nullable public FluidStack getOutputA() { return outputA; }
    @Nullable public FluidStack getOutputB() { return outputB; }
    public ItemStack[] getByproducts() { return byproducts; }

    /** Совпадение входного бака по типу жидкости и достаточному количеству (как оригинальный map-lookup). */
    public boolean matchesInput(FluidTank tank) {
        if (tank == null) return false;
        if (!VanillaFluidEquivalence.sameSubstance(tank.getTankType(), input.getFluid())) return false;
        return tank.getFill() >= getInputAmount();
    }

    /** Совпадение только по типу жидкости (без проверки заполнения) — для поиска рецепта машиной. */
    public boolean matchesFluidType(Fluid fluid) {
        return VanillaFluidEquivalence.sameSubstance(fluid, input.getFluid());
    }

    @Override
    public boolean matchesRecipe(@NotNull RecipeInputWrapper container, @NotNull Level level) {
        // Fluid-режим не имеет предметных входов — совпадение определяется баком машины.
        return !level.isClientSide();
    }

    @Override
    public ItemStack assembleSafe() {
        return getResultItemSafe();
    }

    @Override
    public ItemStack getResultItemSafe() {
        for (ItemStack byproduct : byproducts) {
            if (!byproduct.isEmpty()) return byproduct.copy();
        }
        return ItemStack.EMPTY;
    }

    @Override
    public RecipeSerializer<?> getSerializer() { return Serializer.INSTANCE; }

    @Override
    public RecipeType<?> getType() { return Type.INSTANCE; }

    public static class Type implements RecipeType<ElectrolyserFluidRecipe> {
        public static final Type INSTANCE = new Type();
        public static final String ID = "electrolyser_fluid";
    }

    public static class Serializer extends PlatformRecipeSerializer<ElectrolyserFluidRecipe> {
        public static final Serializer INSTANCE = new Serializer();
        //? if fabric && < 1.21.1 {
        /*public static final ResourceLocation ID = new ResourceLocation(RefStrings.MODID, "electrolyser_fluid");
        *///?} else {
        public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "electrolyser_fluid");
        //?}

        @Override
        public ElectrolyserFluidRecipe readJson(ResourceLocation recipeId, JsonObject json) {
            FluidStack input = readFluid(GsonHelper.getAsJsonObject(json, "input"));
            FluidStack outputA = json.has("output_a") ? readFluid(GsonHelper.getAsJsonObject(json, "output_a")) : null;
            FluidStack outputB = json.has("output_b") ? readFluid(GsonHelper.getAsJsonObject(json, "output_b")) : null;

            ItemStack[] byproducts = new ItemStack[0];
            if (json.has("byproducts")) {
                JsonArray arr = GsonHelper.getAsJsonArray(json, "byproducts");
                byproducts = new ItemStack[arr.size()];
                for (int i = 0; i < arr.size(); i++) {
                    byproducts[i] = RecipeHooks.itemStackFromJson(arr.get(i).getAsJsonObject());
                }
            }
            return new ElectrolyserFluidRecipe(recipeId, input, outputA, outputB, byproducts);
        }

        @Override
        public ElectrolyserFluidRecipe readNetwork(ResourceLocation recipeId, FriendlyByteBuf buf) {
            FluidStack input = RecipeHooks.readFluidStack(buf);
            FluidStack outputA = buf.readBoolean() ? RecipeHooks.readFluidStack(buf) : null;
            FluidStack outputB = buf.readBoolean() ? RecipeHooks.readFluidStack(buf) : null;
            int n = buf.readVarInt();
            ItemStack[] byproducts = new ItemStack[n];
            for (int i = 0; i < n; i++) {
                byproducts[i] = RecipeHooks.readItem(buf);
            }
            return new ElectrolyserFluidRecipe(recipeId, input, outputA, outputB, byproducts);
        }

        @Override
        public void writeNetwork(FriendlyByteBuf buf, ElectrolyserFluidRecipe recipe) {
            RecipeHooks.writeFluidStack(buf, recipe.input);
            buf.writeBoolean(recipe.outputA != null);
            if (recipe.outputA != null) RecipeHooks.writeFluidStack(buf, recipe.outputA);
            buf.writeBoolean(recipe.outputB != null);
            if (recipe.outputB != null) RecipeHooks.writeFluidStack(buf, recipe.outputB);
            buf.writeVarInt(recipe.byproducts.length);
            for (ItemStack byproduct : recipe.byproducts) {
                RecipeHooks.writeItem(buf, byproduct);
            }
        }

        /** { "fluid": <id>, "amount": <mB> } — единый формат мода (см. ChemicalPlantRecipe). */
        private static FluidStack readFluid(JsonObject obj) {
            ResourceLocation id = ResourceLocation.tryParse(GsonHelper.getAsString(obj, "fluid"));
            long amount = GsonHelper.getAsLong(obj, "amount", 0L);
            return RecipeHooks.fluidStackOf(id, amount);
        }
    }
}
