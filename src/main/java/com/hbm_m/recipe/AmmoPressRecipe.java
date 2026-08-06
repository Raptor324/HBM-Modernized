package com.hbm_m.recipe;

import org.jetbrains.annotations.NotNull;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.hbm_m.lib.RefStrings;

import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
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

/**
 * Datapack-facing recipe shape ({@code hbm_m:ammo_press}) - Port von {@code AmmoPressRecipes}
 * (1.7.10 Original, {@code TileEntityMachineAmmoPress}). Anders als eine vanilla Shaped-Recipe
 * matcht das Original NICHT verschiebbar innerhalb des 3x3-Rasters, sondern exakt positionsgleich
 * (Slot 0-8 der GUI = Index 0-8 hier) - fachlich aber identisch zu einer 3x3-Shaped-Recipe, die
 * das gesamte Raster ausfuellt (dort ist wegen der vollen Breite/Hoehe ohnehin keine Verschiebung
 * moeglich). Deshalb hier als eigener, einfacherer Recipe-Typ mit 9 festen {@link Ingredient}-
 * Slots statt vanilla Pattern-Key-Syntax.
 */
public class AmmoPressRecipe implements Recipe<Container> {

    public static final int GRID_SIZE = 9;

    private final ResourceLocation id;
    private final NonNullList<Ingredient> inputs;
    private final ItemStack output;

    public AmmoPressRecipe(ResourceLocation id, NonNullList<Ingredient> inputs, ItemStack output) {
        this.id = id;
        this.inputs = inputs;
        this.output = output;
    }

    public NonNullList<Ingredient> getInputs() {
        return inputs;
    }

    public ItemStack getOutput() {
        return output.copy();
    }

    /** Prueft, ob die 9 GUI-Input-Slots exakt (nicht verschiebbar) auf dieses Rezept passen. */
    public boolean matchesGrid(NonNullList<ItemStack> grid) {
        for (int i = 0; i < GRID_SIZE; i++) {
            if (!inputs.get(i).test(grid.get(i))) return false;
        }
        return true;
    }

    @Override
    public boolean matches(@NotNull Container container, @NotNull Level level) {
        for (int i = 0; i < GRID_SIZE; i++) {
            if (!inputs.get(i).test(container.getItem(i))) return false;
        }
        return true;
    }

    @Override
    public @NotNull ItemStack assemble(@NotNull Container container, @NotNull RegistryAccess registryAccess) {
        return getOutput();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= GRID_SIZE;
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

    public static class Type implements RecipeType<AmmoPressRecipe> {
        public static final Type INSTANCE = new Type();
        public static final String ID = "ammo_press";
    }

    public static class Serializer implements RecipeSerializer<AmmoPressRecipe> {
        public static final Serializer INSTANCE = new Serializer();
        //? if fabric && < 1.21.1 {
        /*public static final ResourceLocation ID = new ResourceLocation(RefStrings.MODID, "ammo_press");
        *///?} else {
        public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "ammo_press");
        //?}

        @Override
        public @NotNull AmmoPressRecipe fromJson(@NotNull ResourceLocation recipeId, @NotNull JsonObject json) {
            JsonArray ingredientsArray = GsonHelper.getAsJsonArray(json, "ingredients");
            NonNullList<Ingredient> inputs = NonNullList.withSize(AmmoPressRecipe.GRID_SIZE, Ingredient.EMPTY);
            for (int i = 0; i < AmmoPressRecipe.GRID_SIZE && i < ingredientsArray.size(); i++) {
                inputs.set(i, Ingredient.fromJson(ingredientsArray.get(i)));
            }

            ItemStack output = ShapedRecipe.itemStackFromJson(GsonHelper.getAsJsonObject(json, "result"));

            return new AmmoPressRecipe(recipeId, inputs, output);
        }

        @Override
        public AmmoPressRecipe fromNetwork(@NotNull ResourceLocation recipeId, @NotNull FriendlyByteBuf buf) {
            NonNullList<Ingredient> inputs = NonNullList.withSize(AmmoPressRecipe.GRID_SIZE, Ingredient.EMPTY);
            for (int i = 0; i < AmmoPressRecipe.GRID_SIZE; i++) {
                inputs.set(i, Ingredient.fromNetwork(buf));
            }
            ItemStack output = buf.readItem();
            return new AmmoPressRecipe(recipeId, inputs, output);
        }

        @Override
        public void toNetwork(@NotNull FriendlyByteBuf buf, @NotNull AmmoPressRecipe recipe) {
            for (int i = 0; i < AmmoPressRecipe.GRID_SIZE; i++) {
                recipe.inputs.get(i).toNetwork(buf);
            }
            buf.writeItem(recipe.output);
        }
    }
}
