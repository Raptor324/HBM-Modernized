package com.hbm_m.recipe;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.google.gson.JsonObject;
import com.hbm_m.lib.RefStrings;

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
import com.hbm_m.platform.recipe.PlatformRecipe;
import com.hbm_m.platform.recipe.PlatformRecipeSerializer;
import com.hbm_m.platform.recipe.RecipeInputWrapper;
import com.hbm_m.platform.recipe.RecipeHooks;

import dev.architectury.fluid.FluidStack;

/**
 * Datapack-facing recipe shape ({@code hbm_m:arc_furnace}) for the Arc Furnace — generic port of
 * the 1.7.10 {@code ArcFurnaceRecipes}/{@code ArcFurnaceRecipe} (see
 * {@code TileEntityMachineArcFurnaceLarge} for the original single-block-per-recipe application
 * logic; that class is a multiblock and was NOT ported structurally).
 *
 * <p>Shape: 1 item input (with count) -> optional item output (with count) AND/OR up to 2 fluid
 * outputs (fluid + amount each). A recipe may produce only a fluid (e.g. melting ore into molten
 * metal), only an item (e.g. flint -> silicon nuggets... in the original those also produce a
 * fluid, but the shape supports item-only for completeness), or both. Mirrors
 * {@link CombinationOvenRecipe} architecturally (own {@link RecipeType}/{@link RecipeSerializer}
 * pair, JSON-driven via datapacks, no recipes hardcoded in Java).</p>
 *
 * <p>Внутреннее хранение жидкостных выходов унифицировано с {@link ChemicalPlantRecipe}: используется
 * Architectury {@link FluidStack} вместо пары {@code ResourceLocation + int}. Getters
 * {@link #getFluid1()}/{@link #getFluidAmount1()}/etc. сохранены для совместимости с
 * {@code MachineArcFurnaceBlockEntity} (они маппят из {@link FluidStack}).</p>
 */
public class ArcFurnaceRecipe extends PlatformRecipe {

    private final Ingredient input;
    private final int inputCount;
    private final ItemStack output;
    private final FluidStack fluidOutput1;
    private final FluidStack fluidOutput2;
    private final int duration;

    public ArcFurnaceRecipe(ResourceLocation id, Ingredient input, int inputCount, ItemStack output,
                             @Nullable FluidStack fluidOutput1,
                             @Nullable FluidStack fluidOutput2,
                             int duration) {
        super(id);
        this.input = input;
        this.inputCount = Math.max(1, inputCount);
        this.output = output == null ? ItemStack.EMPTY : output;
        this.fluidOutput1 = fluidOutput1 != null && !fluidOutput1.isEmpty() ? fluidOutput1 : FluidStack.empty();
        this.fluidOutput2 = fluidOutput2 != null && !fluidOutput2.isEmpty() ? fluidOutput2 : FluidStack.empty();
        this.duration = Math.max(1, duration);
    }

    public Ingredient getInput() { return input; }
    public int getInputCount() { return inputCount; }
    public ItemStack getOutput() { return output.isEmpty() ? ItemStack.EMPTY : output.copy(); }
    public boolean hasItemOutput() { return !output.isEmpty(); }

    @NotNull
    public Fluid getFluid1() { return fluidOutput1.isEmpty() ? Fluids.EMPTY : fluidOutput1.getFluid(); }
    public int getFluidAmount1() { return fluidOutput1.isEmpty() ? 0 : (int) fluidOutput1.getAmount(); }

    @NotNull
    public Fluid getFluid2() { return fluidOutput2.isEmpty() ? Fluids.EMPTY : fluidOutput2.getFluid(); }
    public int getFluidAmount2() { return fluidOutput2.isEmpty() ? 0 : (int) fluidOutput2.getAmount(); }

    public boolean hasFluidOutput1() { return !fluidOutput1.isEmpty() && fluidOutput1.getAmount() > 0; }
    public boolean hasFluidOutput2() { return !fluidOutput2.isEmpty() && fluidOutput2.getAmount() > 0; }

    /** Прямой доступ к жидкостным выходам (как в ChemicalPlantRecipe). */
    public FluidStack getFluidStack1() { return fluidOutput1; }
    public FluidStack getFluidStack2() { return fluidOutput2; }

    public int getDuration() { return duration; }
    public boolean matchesInput(ItemStack stack) { return input.test(stack); }

    @Override
    public boolean matchesRecipe(RecipeInputWrapper container, Level level) {
        return input.test(container.getItem(0));
    }

    @Override
    public ItemStack assembleSafe() {
        return getOutput();
    }

    @Override
    public ItemStack getResultItemSafe() {
        return getOutput();
    }

    @Override
    public @NotNull RecipeSerializer<?> getSerializer() {
        return Serializer.INSTANCE;
    }

    @Override
    public @NotNull RecipeType<?> getType() {
        return Type.INSTANCE;
    }

    public static class Type implements RecipeType<ArcFurnaceRecipe> {
        public static final Type INSTANCE = new Type();
        public static final String ID = "arc_furnace";
    }

    public static class Serializer extends PlatformRecipeSerializer<ArcFurnaceRecipe> {
        public static final Serializer INSTANCE = new Serializer();
        public static final ResourceLocation ID = ResourceLocation.tryParse(RefStrings.MODID + ":arc_furnace");

        @Override
        public ArcFurnaceRecipe readJson(ResourceLocation recipeId, JsonObject json) {
            Ingredient input = RecipeHooks.ingredientFromJson(json.get("ingredient"));
            int inputCount = GsonHelper.getAsInt(json, "count", 1);

            ItemStack output = ItemStack.EMPTY;
            if (json.has("result")) {
                output = RecipeHooks.itemStackFromJson(GsonHelper.getAsJsonObject(json, "result"));
            }

            FluidStack fluid1 = FluidStack.empty();
            if (json.has("fluid1")) {
                JsonObject fluidObj = GsonHelper.getAsJsonObject(json, "fluid1");
                ResourceLocation id = ResourceLocation.tryParse(GsonHelper.getAsString(fluidObj, "fluid"));
                int amount = GsonHelper.getAsInt(fluidObj, "amount", 0);
                fluid1 = RecipeHooks.fluidStackOf(id, amount);
            }

            FluidStack fluid2 = FluidStack.empty();
            if (json.has("fluid2")) {
                JsonObject fluidObj = GsonHelper.getAsJsonObject(json, "fluid2");
                ResourceLocation id = ResourceLocation.tryParse(GsonHelper.getAsString(fluidObj, "fluid"));
                int amount = GsonHelper.getAsInt(fluidObj, "amount", 0);
                fluid2 = RecipeHooks.fluidStackOf(id, amount);
            }

            int duration = GsonHelper.getAsInt(json, "duration", 200);

            return new ArcFurnaceRecipe(recipeId, input, inputCount, output, fluid1, fluid2, duration);
        }

        @Override
        public ArcFurnaceRecipe readNetwork(ResourceLocation recipeId, FriendlyByteBuf buf) {
            Ingredient input = RecipeHooks.readIngredient(buf);
            int inputCount = buf.readVarInt();

            ItemStack output = RecipeHooks.readItem(buf);

            FluidStack fluid1 = RecipeHooks.readFluidStack(buf);
            FluidStack fluid2 = RecipeHooks.readFluidStack(buf);

            int duration = buf.readVarInt();

            return new ArcFurnaceRecipe(recipeId, input, inputCount, output, fluid1, fluid2, duration);
        }

        @Override
        public void writeNetwork(FriendlyByteBuf buf, ArcFurnaceRecipe recipe) {
            RecipeHooks.writeIngredient(buf, recipe.input);
            buf.writeVarInt(recipe.inputCount);

            RecipeHooks.writeItem(buf, recipe.output);

            RecipeHooks.writeFluidStack(buf, recipe.fluidOutput1);
            RecipeHooks.writeFluidStack(buf, recipe.fluidOutput2);

            buf.writeVarInt(recipe.duration);
        }
    }
}
