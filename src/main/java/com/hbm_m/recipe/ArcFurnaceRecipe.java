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
 * <p>Bewusst NICHT uebernommen (Konsistenz mit Combination Oven/Ore Slopper in dieser Session):
 * Energieverbrauch, die Multiblock-Struktur, Electrode-Hitze-Animation.</p>
 */
public class ArcFurnaceRecipe implements Recipe<Container> {

    private final ResourceLocation id;
    private final Ingredient input;
    private final int inputCount;
    private final ItemStack output;
    private final ResourceLocation fluidId1;
    private final int fluidAmount1;
    private final ResourceLocation fluidId2;
    private final int fluidAmount2;
    private final int duration;

    public ArcFurnaceRecipe(ResourceLocation id, Ingredient input, int inputCount, ItemStack output,
                             ResourceLocation fluidId1, int fluidAmount1,
                             ResourceLocation fluidId2, int fluidAmount2,
                             int duration) {
        this.id = id;
        this.input = input;
        this.inputCount = Math.max(1, inputCount);
        this.output = output == null ? ItemStack.EMPTY : output;
        this.fluidId1 = fluidId1;
        this.fluidAmount1 = Math.max(0, fluidAmount1);
        this.fluidId2 = fluidId2;
        this.fluidAmount2 = Math.max(0, fluidAmount2);
        this.duration = Math.max(1, duration);
    }

    public Ingredient getInput() {
        return input;
    }

    public int getInputCount() {
        return inputCount;
    }

    public ItemStack getOutput() {
        return output.isEmpty() ? ItemStack.EMPTY : output.copy();
    }

    public boolean hasItemOutput() {
        return !output.isEmpty();
    }

    @Nullable
    public ResourceLocation getFluidId1() {
        return fluidId1;
    }

    public int getFluidAmount1() {
        return fluidAmount1;
    }

    @Nullable
    public ResourceLocation getFluidId2() {
        return fluidId2;
    }

    public int getFluidAmount2() {
        return fluidAmount2;
    }

    /** Resolves fluid 1 lazily (registries may not be fully populated at recipe-load time). */
    @NotNull
    public Fluid getFluid1() {
        if (fluidId1 == null) return Fluids.EMPTY;
        Fluid f = BuiltInRegistries.FLUID.get(fluidId1);
        return f != null ? f : Fluids.EMPTY;
    }

    /** Resolves fluid 2 lazily (registries may not be fully populated at recipe-load time). */
    @NotNull
    public Fluid getFluid2() {
        if (fluidId2 == null) return Fluids.EMPTY;
        Fluid f = BuiltInRegistries.FLUID.get(fluidId2);
        return f != null ? f : Fluids.EMPTY;
    }

    public boolean hasFluidOutput1() {
        return fluidId1 != null && fluidAmount1 > 0;
    }

    public boolean hasFluidOutput2() {
        return fluidId2 != null && fluidAmount2 > 0;
    }

    public int getDuration() {
        return duration;
    }

    public boolean matchesInput(ItemStack stack) {
        return input.test(stack);
    }

    @Override
    public boolean matches(@NotNull Container container, @NotNull Level level) {
        return input.test(container.getItem(0));
    }

    @Override
    public @NotNull ItemStack assemble(@NotNull Container container, @NotNull RegistryAccess registryAccess) {
        return getOutput();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public @NotNull ItemStack getResultItem(@NotNull RegistryAccess registryAccess) {
        return getOutput();
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

    public static class Type implements RecipeType<ArcFurnaceRecipe> {
        public static final Type INSTANCE = new Type();
        public static final String ID = "arc_furnace";
    }

    public static class Serializer implements RecipeSerializer<ArcFurnaceRecipe> {
        public static final Serializer INSTANCE = new Serializer();
        //? if fabric && < 1.21.1 {
        /*public static final ResourceLocation ID = new ResourceLocation(RefStrings.MODID, "arc_furnace");
        *///?} else {
        public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "arc_furnace");
        //?}

        @Override
        public @NotNull ArcFurnaceRecipe fromJson(@NotNull ResourceLocation recipeId, @NotNull JsonObject json) {
            Ingredient input = Ingredient.fromJson(json.get("ingredient"));
            int inputCount = GsonHelper.getAsInt(json, "count", 1);

            ItemStack output = ItemStack.EMPTY;
            if (json.has("result")) {
                output = ShapedRecipe.itemStackFromJson(GsonHelper.getAsJsonObject(json, "result"));
            }

            ResourceLocation fluidId1 = null;
            int fluidAmount1 = 0;
            if (json.has("fluid1")) {
                JsonObject fluidObj = GsonHelper.getAsJsonObject(json, "fluid1");
                fluidId1 = ResourceLocation.tryParse(GsonHelper.getAsString(fluidObj, "fluid"));
                fluidAmount1 = GsonHelper.getAsInt(fluidObj, "amount", 0);
            }

            ResourceLocation fluidId2 = null;
            int fluidAmount2 = 0;
            if (json.has("fluid2")) {
                JsonObject fluidObj = GsonHelper.getAsJsonObject(json, "fluid2");
                fluidId2 = ResourceLocation.tryParse(GsonHelper.getAsString(fluidObj, "fluid"));
                fluidAmount2 = GsonHelper.getAsInt(fluidObj, "amount", 0);
            }

            int duration = GsonHelper.getAsInt(json, "duration", 200);

            return new ArcFurnaceRecipe(recipeId, input, inputCount, output,
                    fluidId1, fluidAmount1, fluidId2, fluidAmount2, duration);
        }

        @Override
        public @Nullable ArcFurnaceRecipe fromNetwork(@NotNull ResourceLocation recipeId, @NotNull FriendlyByteBuf buf) {
            Ingredient input = Ingredient.fromNetwork(buf);
            int inputCount = buf.readVarInt();

            ItemStack output = buf.readItem();

            boolean hasFluid1 = buf.readBoolean();
            ResourceLocation fluidId1 = hasFluid1 ? buf.readResourceLocation() : null;
            int fluidAmount1 = buf.readVarInt();

            boolean hasFluid2 = buf.readBoolean();
            ResourceLocation fluidId2 = hasFluid2 ? buf.readResourceLocation() : null;
            int fluidAmount2 = buf.readVarInt();

            int duration = buf.readVarInt();

            return new ArcFurnaceRecipe(recipeId, input, inputCount, output,
                    fluidId1, fluidAmount1, fluidId2, fluidAmount2, duration);
        }

        @Override
        public void toNetwork(@NotNull FriendlyByteBuf buf, @NotNull ArcFurnaceRecipe recipe) {
            recipe.input.toNetwork(buf);
            buf.writeVarInt(recipe.inputCount);

            buf.writeItem(recipe.output);

            buf.writeBoolean(recipe.fluidId1 != null);
            if (recipe.fluidId1 != null) {
                buf.writeResourceLocation(recipe.fluidId1);
            }
            buf.writeVarInt(recipe.fluidAmount1);

            buf.writeBoolean(recipe.fluidId2 != null);
            if (recipe.fluidId2 != null) {
                buf.writeResourceLocation(recipe.fluidId2);
            }
            buf.writeVarInt(recipe.fluidAmount2);

            buf.writeVarInt(recipe.duration);
        }
    }
}
