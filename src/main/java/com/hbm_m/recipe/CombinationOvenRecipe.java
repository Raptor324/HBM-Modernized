package com.hbm_m.recipe;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

/**
 * 1:1 {@code CombinationRecipes}: ein Item rein, ein Item <b>und/oder</b> ein Fluid heraus
 * ({@code Pair<ItemStack, FluidStack>}). Die Verarbeitungszeit ist fest und haengt an der Hitze des Ofens,
 * nicht am Rezept.
 *
 * <pre>{@code
 * { "type": "hbm_m:combination_oven", "ingredient": {...},
 *   "result": { "item": "...", "count": 1 },          // optional
 *   "fluid": { "fluid": "hbm_m:coalcreosote", "amount": 100 } }   // optional, AUSGABE
 * }</pre>
 */
public class CombinationOvenRecipe extends PlatformRecipe {

    private final Ingredient input;
    private final ItemStack output;
    @Nullable private final FluidStack fluidOutput;

    public CombinationOvenRecipe(ResourceLocation id, Ingredient input, ItemStack output, @Nullable FluidStack fluidOutput) {
        super(id);
        this.input = input;
        this.output = output == null ? ItemStack.EMPTY : output;
        this.fluidOutput = (fluidOutput != null && !fluidOutput.isEmpty() && fluidOutput.getAmount() > 0) ? fluidOutput : null;
    }

    public Ingredient getInput() { return input; }

    /** Item-Ausgabe, kann leer sein. */
    public ItemStack getOutput() { return output.copy(); }

    @Nullable public FluidStack getFluidOutput() { return fluidOutput; }

    @NotNull
    public Fluid getOutputFluid() {
        if (fluidOutput == null) return Fluids.EMPTY;
        Fluid f = fluidOutput.getFluid();
        return f != null ? f : Fluids.EMPTY;
    }

    public int getOutputFluidAmount() {
        return fluidOutput != null ? (int) Math.min(Integer.MAX_VALUE, fluidOutput.getAmount()) : 0;
    }

    public boolean matchesInput(ItemStack stack) {
        return !stack.isEmpty() && input.test(stack);
    }

    @Override
    public boolean matchesRecipe(@NotNull RecipeInputWrapper container, @NotNull Level level) {
        return matchesInput(container.getItem(0));
    }

    @Override
    public @NotNull ItemStack assembleSafe() { return output.copy(); }

    @Override
    public @NotNull ItemStack getResultItemSafe() { return output.copy(); }

    @Override
    public @NotNull RecipeSerializer<?> getSerializer() { return Serializer.INSTANCE; }

    @Override
    public @NotNull RecipeType<?> getType() { return Type.INSTANCE; }

    public static class Type implements RecipeType<CombinationOvenRecipe> {
        public static final Type INSTANCE = new Type();
        public static final String ID = "combination_oven";
    }

    public static class Serializer extends PlatformRecipeSerializer<CombinationOvenRecipe> {
        public static final Serializer INSTANCE = new Serializer();
        //? if fabric && < 1.21.1 {
        /*public static final ResourceLocation ID = new ResourceLocation(RefStrings.MODID, "combination_oven");
        *///?} else {
        public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "combination_oven");
        //?}

        @Override
        public @NotNull CombinationOvenRecipe readJson(@NotNull ResourceLocation recipeId, @NotNull JsonObject json) {
            Ingredient input = RecipeHooks.ingredientFromJson(json.get("ingredient"));

            FluidStack fluid = FluidStack.empty();
            if (json.has("fluid")) {
                JsonObject fluidObj = GsonHelper.getAsJsonObject(json, "fluid");
                ResourceLocation id = ResourceLocation.tryParse(GsonHelper.getAsString(fluidObj, "fluid"));
                int amount = GsonHelper.getAsInt(fluidObj, "amount", 0);
                if (id != null && amount > 0) fluid = RecipeHooks.fluidStackOf(id, amount);
            }

            ItemStack output = json.has("result") ? RecipeHooks.itemStackFromJson(GsonHelper.getAsJsonObject(json, "result")) : ItemStack.EMPTY;

            return new CombinationOvenRecipe(recipeId, input, output, fluid);
        }

        @Override
        public CombinationOvenRecipe readNetwork(@NotNull ResourceLocation recipeId, @NotNull FriendlyByteBuf buf) {
            Ingredient input = RecipeHooks.readIngredient(buf);
            ItemStack output = RecipeHooks.readItem(buf);
            FluidStack fluid = RecipeHooks.readFluidStack(buf);
            return new CombinationOvenRecipe(recipeId, input, output, fluid);
        }

        @Override
        public void writeNetwork(@NotNull FriendlyByteBuf buf, @NotNull CombinationOvenRecipe recipe) {
            RecipeHooks.writeIngredient(buf, recipe.input);
            RecipeHooks.writeItem(buf, recipe.output);
            RecipeHooks.writeFluidStack(buf, recipe.fluidOutput != null ? recipe.fluidOutput : FluidStack.empty());
        }
    }
}
