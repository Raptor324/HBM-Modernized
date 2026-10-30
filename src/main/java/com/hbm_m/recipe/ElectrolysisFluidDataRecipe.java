package com.hbm_m.recipe;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.hbm_m.inventory.fluid.ModFluids;
import com.hbm_m.inventory.recipes.ElectrolyserFluidRecipes;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.platform.recipe.PlatformRecipe;
import com.hbm_m.platform.recipe.PlatformRecipeSerializer;
import com.hbm_m.platform.recipe.RecipeHooks;
import com.hbm_m.platform.recipe.RecipeInputWrapper;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;

/**
 * W-R9B5-2: Elektrolyse-Rezept (Fluid) aus dem Datapack ({@code hbm_m:electrolysis_fluid}), Gegenstueck zu
 * {@code ElectrolyserFluidRecipes.recipes.put(input, new ElectrolysisRecipe(...))}. HBM liefert keine solchen JSONs;
 * {@link ElectrolyserFluidRecipes#all(net.minecraft.world.item.crafting.RecipeManager)} legt sie ueber die Code-Tabelle
 * (gleiches Eingangsfluid = ersetzt, wie {@code Map.put} im Original).
 *
 * <pre>{@code
 * {
 *   "type": "hbm_m:electrolysis_fluid",
 *   "input":    { "fluid": "space_m:brine", "amount": 400 },
 *   "output1":  { "fluid": "hbm_m:hydrogen", "amount": 200 },   // optional, fehlt = NONE
 *   "output2":  { "fluid": "hbm_m:oxygen", "amount": 200 },     // optional, fehlt = NONE
 *   "duration": 40,                                             // optional, Standard 20
 *   "byproducts": [ { "item": "hbm_m:sodium_powder", "count": 2 } ]  // optional, hoechstens 3
 * }
 * }</pre>
 */
public class ElectrolysisFluidDataRecipe extends PlatformRecipe {

    private final MixerDataRecipe.FluidAmount input;
    @Nullable private final MixerDataRecipe.FluidAmount output1;
    @Nullable private final MixerDataRecipe.FluidAmount output2;
    private final int duration;
    private final List<ItemStack> byproducts;

    public ElectrolysisFluidDataRecipe(ResourceLocation id, MixerDataRecipe.FluidAmount input, @Nullable MixerDataRecipe.FluidAmount output1,
                                       @Nullable MixerDataRecipe.FluidAmount output2, int duration, List<ItemStack> byproducts) {
        super(id);
        this.input = input;
        this.output1 = output1;
        this.output2 = output2;
        this.duration = duration;
        this.byproducts = byproducts;
    }

    @Nullable
    public Fluid inputFluid() { return input.resolve(); }

    /** Als Code-Rezept; null, wenn ein Ausgabefluid nicht registriert ist (Rezept wird uebergangen). */
    @Nullable
    public ElectrolyserFluidRecipes.ElectrolysisRecipe toRecipe() {
        ElectrolyserFluidRecipes.FluidOut o1 = out(output1);
        ElectrolyserFluidRecipes.FluidOut o2 = out(output2);
        if (o1 == null || o2 == null) return null;
        ItemStack[] bp = new ItemStack[byproducts.size()];
        for (int i = 0; i < bp.length; i++) bp[i] = byproducts.get(i).copy();
        return new ElectrolyserFluidRecipes.ElectrolysisRecipe(input.amount(), o1, o2, duration, bp);
    }

    @Nullable
    private static ElectrolyserFluidRecipes.FluidOut out(@Nullable MixerDataRecipe.FluidAmount f) {
        if (f == null || f.amount() <= 0) return new ElectrolyserFluidRecipes.FluidOut(ModFluids.NONE.getSource(), 0);
        Fluid fluid = f.resolve();
        return fluid == null ? null : new ElectrolyserFluidRecipes.FluidOut(fluid, f.amount());
    }

    @Override
    public boolean matchesRecipe(@NotNull RecipeInputWrapper input, @NotNull Level level) { return false; }

    @Override
    public ItemStack assembleSafe() { return ItemStack.EMPTY; }

    @Override
    public ItemStack getResultItemSafe() { return ItemStack.EMPTY; }

    /** Kein Rezeptbuch-Eintrag (nur Maschinentabelle). */
    @Override
    public boolean isSpecial() { return true; }

    @Override
    public RecipeSerializer<?> getSerializer() { return Serializer.INSTANCE; }

    @Override
    public RecipeType<?> getType() { return Type.INSTANCE; }

    public static class Type implements RecipeType<ElectrolysisFluidDataRecipe> {
        public static final Type INSTANCE = new Type();
        public static final String ID = "electrolysis_fluid";

        @Override
        public String toString() { return RefStrings.MODID + ":" + ID; }
    }

    public static class Serializer extends PlatformRecipeSerializer<ElectrolysisFluidDataRecipe> {
        public static final Serializer INSTANCE = new Serializer();

        @Override
        public ElectrolysisFluidDataRecipe readJson(ResourceLocation recipeId, JsonObject json) {
            MixerDataRecipe.FluidAmount in = MixerDataRecipe.Serializer.fluid(GsonHelper.getAsJsonObject(json, "input"));
            MixerDataRecipe.FluidAmount o1 = json.has("output1") ? MixerDataRecipe.Serializer.fluid(GsonHelper.getAsJsonObject(json, "output1")) : null;
            MixerDataRecipe.FluidAmount o2 = json.has("output2") ? MixerDataRecipe.Serializer.fluid(GsonHelper.getAsJsonObject(json, "output2")) : null;
            List<ItemStack> bp = new ArrayList<>();
            if (json.has("byproducts")) {
                for (JsonElement e : GsonHelper.getAsJsonArray(json, "byproducts")) bp.add(RecipeHooks.itemStackFromJson(e.getAsJsonObject()));
            }
            if (bp.size() > 3) throw new IllegalArgumentException("electrolysis_fluid: hoechstens 3 Nebenprodukte (" + recipeId + ")");
            return new ElectrolysisFluidDataRecipe(recipeId, in, o1, o2, GsonHelper.getAsInt(json, "duration", 20), bp);
        }

        @Override
        public ElectrolysisFluidDataRecipe readNetwork(ResourceLocation recipeId, FriendlyByteBuf buf) {
            MixerDataRecipe.FluidAmount in = MixerDataRecipe.Serializer.readFluid(buf);
            MixerDataRecipe.FluidAmount o1 = buf.readBoolean() ? MixerDataRecipe.Serializer.readFluid(buf) : null;
            MixerDataRecipe.FluidAmount o2 = buf.readBoolean() ? MixerDataRecipe.Serializer.readFluid(buf) : null;
            int duration = buf.readVarInt();
            int n = buf.readVarInt();
            List<ItemStack> bp = new ArrayList<>();
            for (int i = 0; i < n; i++) bp.add(RecipeHooks.readItem(buf));
            return new ElectrolysisFluidDataRecipe(recipeId, in, o1, o2, duration, bp);
        }

        @Override
        public void writeNetwork(FriendlyByteBuf buf, ElectrolysisFluidDataRecipe r) {
            MixerDataRecipe.Serializer.writeFluid(buf, r.input);
            buf.writeBoolean(r.output1 != null);
            if (r.output1 != null) MixerDataRecipe.Serializer.writeFluid(buf, r.output1);
            buf.writeBoolean(r.output2 != null);
            if (r.output2 != null) MixerDataRecipe.Serializer.writeFluid(buf, r.output2);
            buf.writeVarInt(r.duration);
            buf.writeVarInt(r.byproducts.size());
            for (ItemStack s : r.byproducts) RecipeHooks.writeItem(buf, s);
        }
    }
}
