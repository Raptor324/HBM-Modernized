package com.hbm_m.recipe;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.platform.recipe.PlatformRecipe;
import com.hbm_m.platform.recipe.PlatformRecipeSerializer;
import com.hbm_m.platform.recipe.RecipeHooks;
import com.hbm_m.platform.recipe.RecipeInputWrapper;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;

/**
 * 1:1 {@code SILEXRecipes.SILEXRecipe} als Datapack-Rezept ({@code hbm_m:silex}).
 *
 * <ul>
 *   <li>{@code ingredient} - Item-Eingang (inkl. der Original-Uebersetzungen Pulver->Barren als Alternativen);
 *   optional {@code pellet_state} fuer RBMK-Pellets (Abbrandstufe 0-4, +5 = Xenon).</li>
 *   <li>{@code fluids} - stattdessen/zusaetzlich Fluide, die direkt in die Ladeleiste gepumpt werden
 *   (Original {@code fluid_icon}-Eingaenge, UF6/PUF6 ueber {@code itemTranslation}).</li>
 *   <li>{@code fluid_produced} - so viel Ladung (mB) liefert ein Item (bei Items: gleich viel Peroxid wird verbraucht).</li>
 *   <li>{@code fluid_consumed} - Ladung je Arbeitsgang.</li>
 *   <li>{@code laser} - noetige Wellenlaenge ({@code EnumWavelengths.ordinal()}).</li>
 * </ul>
 */
public class SilexRecipe extends PlatformRecipe {

    /** Gewichteter Ausgang: Stapel + relatives Gewicht. */
    public record WeightedOutput(ItemStack stack, int weight) {}

    private final Ingredient input;
    private final int pelletState;
    private final List<ResourceLocation> fluids;
    private final int fluidProduced;
    private final int fluidConsumed;
    private final int laser;
    private final List<WeightedOutput> outputs;

    public SilexRecipe(ResourceLocation id, Ingredient input, int pelletState, List<ResourceLocation> fluids,
                       int fluidProduced, int fluidConsumed, int laser, List<WeightedOutput> outputs) {
        super(id);
        this.input = input;
        this.pelletState = pelletState;
        this.fluids = Collections.unmodifiableList(new ArrayList<>(fluids));
        this.fluidProduced = fluidProduced;
        this.fluidConsumed = fluidConsumed;
        this.laser = laser;
        this.outputs = Collections.unmodifiableList(new ArrayList<>(outputs));
    }

    public Ingredient getInput() { return input; }
    public int getPelletState() { return pelletState; }
    public List<ResourceLocation> getFluids() { return fluids; }
    public int getFluidProduced() { return fluidProduced; }
    public int getFluidConsumed() { return fluidConsumed; }
    /** Noetige Wellenlaenge als {@code EnumWavelengths.ordinal()}. */
    public int getLaser() { return laser; }
    public List<WeightedOutput> getOutputs() { return outputs; }

    /** Kompatibilitaet (JEI): bei Item-Eingang verbrauchtes Peroxid je Item. */
    public int getPeroxideMb() { return fluidProduced; }

    public int getTotalWeight() {
        int total = 0;
        for (WeightedOutput out : outputs) total += out.weight();
        return total;
    }

    public boolean matches(ItemStack stack) {
        if (stack == null || stack.isEmpty() || input.isEmpty() || !input.test(stack)) return false;
        return pelletState < 0 || com.hbm_m.item.rbmk.RBMKPelletItem.getState(stack) == pelletState;
    }

    public boolean matchesFluid(Fluid fluid) {
        if (fluid == null) return false;
        return fluids.contains(BuiltInRegistries.FLUID.getKey(fluid));
    }

    /** Original {@code SILEXRecipes.getOutput(stack)}. */
    @Nullable
    public static SilexRecipe forItem(Level level, ItemStack stack) {
        if (level == null || stack == null || stack.isEmpty()) return null;
        for (SilexRecipe r : RecipeHooks.getAllRecipes(level, Type.INSTANCE)) {
            if (r.matches(stack)) return r;
        }
        return null;
    }

    /** Original {@code SILEXRecipes.getOutput(fluid_icon)} inkl. {@code fluidConversion}. */
    @Nullable
    public static SilexRecipe forFluid(Level level, Fluid fluid) {
        if (level == null || fluid == null) return null;
        for (SilexRecipe r : RecipeHooks.getAllRecipes(level, Type.INSTANCE)) {
            if (r.matchesFluid(fluid)) return r;
        }
        return null;
    }

    @Override
    public boolean matchesRecipe(@NotNull RecipeInputWrapper container, @NotNull Level level) {
        return matches(container.getItem(0));
    }

    @Override
    public ItemStack assembleSafe() {
        return outputs.isEmpty() ? ItemStack.EMPTY : outputs.get(0).stack().copy();
    }

    @Override
    public ItemStack getResultItemSafe() { return assembleSafe(); }

    @Override
    public RecipeSerializer<?> getSerializer() { return Serializer.INSTANCE; }

    @Override
    public RecipeType<?> getType() { return Type.INSTANCE; }

    public static class Type implements RecipeType<SilexRecipe> {
        public static final Type INSTANCE = new Type();
        public static final String ID = "silex";
    }

    public static class Serializer extends PlatformRecipeSerializer<SilexRecipe> {
        public static final Serializer INSTANCE = new Serializer();
        //? if fabric && < 1.21.1 {
        /*public static final ResourceLocation ID = new ResourceLocation(RefStrings.MODID, "silex");
        *///?} else {
        public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "silex");
        //?}

        @Override
        public SilexRecipe readJson(ResourceLocation recipeId, JsonObject json) {
            Ingredient input = json.has("ingredient") ? RecipeHooks.ingredientFromJson(json.get("ingredient")) : Ingredient.EMPTY;
            int pelletState = GsonHelper.getAsInt(json, "pellet_state", -1);
            List<ResourceLocation> fluids = new ArrayList<>();
            if (json.has("fluids")) {
                JsonArray fa = GsonHelper.getAsJsonArray(json, "fluids");
                for (int i = 0; i < fa.size(); i++) fluids.add(ResourceLocation.tryParse(fa.get(i).getAsString()));
            }
            int produced = GsonHelper.getAsInt(json, "fluid_produced", 900);
            int consumed = GsonHelper.getAsInt(json, "fluid_consumed", 100);
            int laser = GsonHelper.getAsInt(json, "laser", 1);

            JsonArray arr = GsonHelper.getAsJsonArray(json, "outputs");
            List<WeightedOutput> outputs = new ArrayList<>();
            for (int i = 0; i < arr.size(); i++) {
                JsonObject entry = arr.get(i).getAsJsonObject();
                ItemStack stack = RecipeHooks.itemStackFromJson(GsonHelper.getAsJsonObject(entry, "result"));
                int weight = GsonHelper.getAsInt(entry, "weight", 1);
                outputs.add(new WeightedOutput(stack, Math.max(1, weight)));
            }
            return new SilexRecipe(recipeId, input, pelletState, fluids, produced, consumed, laser, outputs);
        }

        @Override
        public SilexRecipe readNetwork(ResourceLocation recipeId, FriendlyByteBuf buf) {
            Ingredient input = RecipeHooks.readIngredient(buf);
            int pelletState = buf.readVarInt() - 1;
            int nf = buf.readVarInt();
            List<ResourceLocation> fluids = new ArrayList<>(nf);
            for (int i = 0; i < nf; i++) fluids.add(buf.readResourceLocation());
            int produced = buf.readVarInt();
            int consumed = buf.readVarInt();
            int laser = buf.readVarInt();
            int n = buf.readVarInt();
            List<WeightedOutput> outputs = new ArrayList<>(n);
            for (int i = 0; i < n; i++) {
                ItemStack stack = RecipeHooks.readItem(buf);
                int weight = buf.readVarInt();
                outputs.add(new WeightedOutput(stack, weight));
            }
            return new SilexRecipe(recipeId, input, pelletState, fluids, produced, consumed, laser, outputs);
        }

        @Override
        public void writeNetwork(FriendlyByteBuf buf, SilexRecipe recipe) {
            RecipeHooks.writeIngredient(buf, recipe.input);
            buf.writeVarInt(recipe.pelletState + 1);
            buf.writeVarInt(recipe.fluids.size());
            for (ResourceLocation f : recipe.fluids) buf.writeResourceLocation(f);
            buf.writeVarInt(recipe.fluidProduced);
            buf.writeVarInt(recipe.fluidConsumed);
            buf.writeVarInt(recipe.laser);
            buf.writeVarInt(recipe.outputs.size());
            for (WeightedOutput out : recipe.outputs) {
                RecipeHooks.writeItem(buf, out.stack());
                buf.writeVarInt(out.weight());
            }
        }
    }
}
