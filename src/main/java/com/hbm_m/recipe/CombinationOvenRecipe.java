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
 * Datapack-facing recipe shape ({@code hbm_m:combination_oven}) for the Combination Oven —
 * generic port of the 1.7.10 {@code TileEntityFurnaceCombination}/{@code CombinationRecipes}.
 *
 * <p>Shape: 1 item input (with count) + N mB of a specific fluid -> 1 item output, over a
 * fixed duration. Unlike the original (which is a hand-populated Java {@code HashMap}), this
 * is a proper vanilla {@link net.minecraft.world.item.crafting.Recipe}/{@link RecipeSerializer}
 * pair so recipes can be added as datapack JSON later without touching machine code.</p>
 *
 * <p><b>Унификация жидкостей:</b> жидкостный вход хранится как Architectury {@link FluidStack}
 * (единый формат с {@code ChemicalPlantRecipe}/{@code ArcFurnaceRecipe}/...).
 * Прежняя зависимость от {@code ChemicalPlantRecipe.FluidIngredient} устранена — это был
 * дублирующий слой абстракции, мешавший использовать общий сериализатор
 * {@link RecipeHooks#readFluidStack}/{@link RecipeHooks#writeFluidStack}.
 * Getters {@link #getFluidId()}/{@link #getFluidAmount()}/{@link #getFluid()} сохранены для
 * совместимости с {@code MachineCombinationOvenBlockEntity}.</p>
 */
public class CombinationOvenRecipe extends PlatformRecipe {

    @Nullable
    private final FluidStack fluidInput;
    private final Ingredient input;
    private final int inputCount;
    private final ItemStack output;
    private final int duration;

    public CombinationOvenRecipe(ResourceLocation id, Ingredient input, int inputCount,
                                  @Nullable FluidStack fluidInput,
                                  ItemStack output, int duration) {
        super(id);
        this.input = input;
        this.inputCount = Math.max(1, inputCount);
        this.fluidInput = (fluidInput != null && !fluidInput.isEmpty() && fluidInput.getAmount() > 0)
                ? fluidInput : null;
        this.output = output;
        this.duration = Math.max(1, duration);
    }

    public Ingredient getInput() {
        return input;
    }

    public int getInputCount() {
        return inputCount;
    }

    @Nullable
    public ResourceLocation getFluidId() {
        if (fluidInput == null || fluidInput.isEmpty()) return null;
        return net.minecraft.core.registries.BuiltInRegistries.FLUID.getKey(fluidInput.getFluid());
    }

    public int getFluidAmount() {
        return fluidInput != null ? (int) Math.min(Integer.MAX_VALUE, fluidInput.getAmount()) : 0;
    }

    /** Resolves the required fluid lazily (registries may not be fully populated at recipe-load time). */
    @NotNull
    public Fluid getFluid() {
        if (fluidInput == null || fluidInput.isEmpty()) return Fluids.EMPTY;
        Fluid f = fluidInput.getFluid();
        return f != null ? f : Fluids.EMPTY;
    }

    /** Прямой доступ к жидкостному входу как {@link FluidStack} (унифицированный формат). */
    @Nullable
    public FluidStack getFluidInput() {
        return fluidInput;
    }

    public ItemStack getOutput() {
        return output.copy();
    }

    public int getDuration() {
        return duration;
    }

    public boolean matchesInput(ItemStack stack) {
        return input.test(stack);
    }

    public boolean matchesFluid(Fluid tankFluid) {
        if (fluidInput == null || fluidInput.isEmpty() || fluidInput.getAmount() <= 0) return true;
        return getFluid() == tankFluid;
    }

    @Override
    public boolean matchesRecipe(@NotNull RecipeInputWrapper container, @NotNull Level level) {
        return input.test(container.getItem(0));
    }

    @Override
    public @NotNull ItemStack assembleSafe() {
        return output.copy();
    }

    @Override
    public @NotNull ItemStack getResultItemSafe() {
        return output.copy();
    }

    @Override
    public @NotNull RecipeSerializer<?> getSerializer() {
        return Serializer.INSTANCE;
    }

    @Override
    public @NotNull RecipeType<?> getType() {
        return Type.INSTANCE;
    }

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
            int inputCount = GsonHelper.getAsInt(json, "count", 1);

            // Жидкостный вход — единый формат { "fluid": <id>, "amount": <mB> } через RecipeHooks.fluidStackOf.
            FluidStack fluidInput = FluidStack.empty();
            if (json.has("fluid")) {
                JsonObject fluidObj = GsonHelper.getAsJsonObject(json, "fluid");
                ResourceLocation id = ResourceLocation.tryParse(GsonHelper.getAsString(fluidObj, "fluid"));
                int amount = GsonHelper.getAsInt(fluidObj, "amount", 0);
                if (id != null && amount > 0) {
                    fluidInput = RecipeHooks.fluidStackOf(id, amount);
                }
            }

            ItemStack output = RecipeHooks.itemStackFromJson(GsonHelper.getAsJsonObject(json, "result"));
            int duration = GsonHelper.getAsInt(json, "duration", 200);

            return new CombinationOvenRecipe(recipeId, input, inputCount, fluidInput, output, duration);
        }

        @Override
        public CombinationOvenRecipe readNetwork(@NotNull ResourceLocation recipeId, @NotNull FriendlyByteBuf buf) {
            Ingredient input = RecipeHooks.readIngredient(buf);
            int inputCount = buf.readVarInt();

            // Жидкостный вход — единый кросс-лоадерный формат (RecipeHooks.readFluidStack).
            FluidStack fluidInput = RecipeHooks.readFluidStack(buf);

            ItemStack output = RecipeHooks.readItem(buf);
            int duration = buf.readVarInt();

            return new CombinationOvenRecipe(recipeId, input, inputCount, fluidInput, output, duration);
        }

        @Override
        public void writeNetwork(@NotNull FriendlyByteBuf buf, @NotNull CombinationOvenRecipe recipe) {
            RecipeHooks.writeIngredient(buf, recipe.input);
            buf.writeVarInt(recipe.inputCount);

            // Жидкостный вход — единый кросс-лоадерный формат (RecipeHooks.writeFluidStack).
            RecipeHooks.writeFluidStack(buf, recipe.fluidInput != null ? recipe.fluidInput : FluidStack.empty());

            RecipeHooks.writeItem(buf, recipe.output);
            buf.writeVarInt(recipe.duration);
        }
    }
}
