package com.hbm_m.recipe;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.google.gson.JsonObject;
import com.hbm_m.lib.RefStrings;

import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

/**
 * Datapack-facing recipe shape ({@code hbm_m:combination_oven}) for the Combination Oven —
 * generic port of the 1.7.10 {@code TileEntityFurnaceCombination}/{@code CombinationRecipes}.
 *
 * <p>Shape: 1 item input (with count) + N mB of a specific fluid -> 1 item output, over a
 * fixed duration. Unlike the original (which is a hand-populated Java {@code HashMap}), this
 * is a proper vanilla {@link Recipe}/{@link RecipeSerializer} pair so recipes can be added as
 * datapack JSON later without touching machine code.</p>
 */
public class CombinationOvenRecipe implements Recipe<Container> {

    private final ResourceLocation id;
    private final Ingredient input;
    private final int inputCount;
    private final ResourceLocation fluidId;
    private final int fluidAmount;
    private final ItemStack output;
    private final int duration;

    public CombinationOvenRecipe(ResourceLocation id, Ingredient input, int inputCount,
                                  ResourceLocation fluidId, int fluidAmount,
                                  ItemStack output, int duration) {
        this.id = id;
        this.input = input;
        this.inputCount = Math.max(1, inputCount);
        this.fluidId = fluidId;
        this.fluidAmount = Math.max(0, fluidAmount);
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
        return fluidId;
    }

    /** Resolves the required fluid lazily (registries may not be fully populated at recipe-load time). */
    @NotNull
    public Fluid getFluid() {
        if (fluidId == null) return Fluids.EMPTY;
        Fluid f = BuiltInRegistries.FLUID.get(fluidId);
        return f != null ? f : Fluids.EMPTY;
    }

    public int getFluidAmount() {
        return fluidAmount;
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
        if (fluidAmount <= 0 || fluidId == null) return true;
        return getFluid() == tankFluid;
    }

    @Override
    public boolean matches(@NotNull Container container, @NotNull Level level) {
        return input.test(container.getItem(0));
    }

    @Override
    public @NotNull ItemStack assemble(@NotNull Container container, @NotNull RegistryAccess registryAccess) {
        return output.copy();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public @NotNull ItemStack getResultItem(@NotNull RegistryAccess registryAccess) {
        return output.copy();
    }

    @Override
    public @NotNull ResourceLocation getId() {
        return id;
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

    public static class Serializer implements RecipeSerializer<CombinationOvenRecipe> {
        public static final Serializer INSTANCE = new Serializer();
        //? if fabric && < 1.21.1 {
        /*public static final ResourceLocation ID = new ResourceLocation(RefStrings.MODID, "combination_oven");
        *///?} else {
        public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "combination_oven");
        //?}

        @Override
        public @NotNull CombinationOvenRecipe fromJson(@NotNull ResourceLocation recipeId, @NotNull JsonObject json) {
            Ingredient input = Ingredient.fromJson(json.get("ingredient"));
            int inputCount = GsonHelper.getAsInt(json, "count", 1);

            ResourceLocation fluidId = null;
            int fluidAmount = 0;
            if (json.has("fluid")) {
                JsonObject fluidObj = GsonHelper.getAsJsonObject(json, "fluid");
                fluidId = ResourceLocation.tryParse(GsonHelper.getAsString(fluidObj, "fluid"));
                fluidAmount = GsonHelper.getAsInt(fluidObj, "amount", 0);
            }

            ItemStack output = ShapedRecipe.itemStackFromJson(GsonHelper.getAsJsonObject(json, "result"));
            int duration = GsonHelper.getAsInt(json, "duration", 200);

            return new CombinationOvenRecipe(recipeId, input, inputCount, fluidId, fluidAmount, output, duration);
        }

        @Override
        public @Nullable CombinationOvenRecipe fromNetwork(@NotNull ResourceLocation recipeId, @NotNull FriendlyByteBuf buf) {
            Ingredient input = Ingredient.fromNetwork(buf);
            int inputCount = buf.readVarInt();

            boolean hasFluid = buf.readBoolean();
            ResourceLocation fluidId = hasFluid ? buf.readResourceLocation() : null;
            int fluidAmount = buf.readVarInt();

            ItemStack output = buf.readItem();
            int duration = buf.readVarInt();

            return new CombinationOvenRecipe(recipeId, input, inputCount, fluidId, fluidAmount, output, duration);
        }

        @Override
        public void toNetwork(@NotNull FriendlyByteBuf buf, @NotNull CombinationOvenRecipe recipe) {
            recipe.input.toNetwork(buf);
            buf.writeVarInt(recipe.inputCount);

            buf.writeBoolean(recipe.fluidId != null);
            if (recipe.fluidId != null) {
                buf.writeResourceLocation(recipe.fluidId);
            }
            buf.writeVarInt(recipe.fluidAmount);

            buf.writeItem(recipe.output);
            buf.writeVarInt(recipe.duration);
        }
    }
}
