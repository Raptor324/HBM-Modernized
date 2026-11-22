package com.hbm_m.inventory.recipes;

import java.util.LinkedHashMap;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.inventory.fluid.ModFluids;

import dev.architectury.fluid.FluidStack;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;

/**
 * 1:1 {@code ElectrolyserFluidRecipes}: je Eingangsfluid eine Menge, zwei Ausgangsfluide, optionale Nebenprodukte
 * und eine Dauer (Standard 20 Ticks). Der Inhalt der Code-Tabelle ist seit der Datapack-Umstellung leer —
 * Quelle sind die {@code hbm_m:electrolyser_fluid}-JSONs (Datagen: {@code ElectrolyserFluidRecipeGenerator}).
 */
public final class ElectrolyserFluidRecipes {

    public static final Map<Fluid, ElectrolysisRecipe> recipes = new LinkedHashMap<>();
    private static boolean loaded;

    private ElectrolyserFluidRecipes() { }

    public static synchronized void ensureLoaded() {
        if (loaded) return;
        loaded = true;
        registerDefaults();
    }

    /** W-R9B5-2 (Space-Addon): Elektrolyse-Rezept fuer ein Eingangsfluid anmelden (nach den HBM-Standards). */
    public static synchronized void addExternal(Fluid input, ElectrolysisRecipe recipe) {
        ensureLoaded();
        recipes.put(input, recipe);
        baseVersion++;
    }

    // ---------------------------------------------------------------- Datapack-Rezepte (hbm_m:electrolyser_fluid)

    private static int baseVersion;

    private record Merged(int baseVersion, int fingerprint, Map<Fluid, ElectrolysisRecipe> map) { }

    private static final java.util.WeakHashMap<net.minecraft.world.item.crafting.RecipeManager, Merged> MERGED = new java.util.WeakHashMap<>();

    /**
     * Code-Tabelle plus {@link com.hbm_m.recipe.ElectrolyserFluidRecipe}s des RecipeManagers (gleiches Eingangsfluid =
     * ersetzt, Reihenfolge Rezept-ID). Der Manager wird daneben an {@link ElectrolyserMetalRecipes#noteManager}
     * weitergereicht: die Metallseite wird vom BlockEntity ohne Level abgefragt und materialisiert ihre JSONs
     * deshalb direkt in die Tabelle (siehe dort).
     */
    public static synchronized Map<Fluid, ElectrolysisRecipe> all(@Nullable net.minecraft.world.item.crafting.RecipeManager manager) {
        Map<Fluid, ElectrolysisRecipe> base = all();
        if (manager == null) return base;
        ElectrolyserMetalRecipes.noteManager(manager);
        Map<net.minecraft.resources.ResourceLocation, com.hbm_m.recipe.ElectrolyserFluidRecipe> data =
                com.hbm_m.platform.recipe.RecipeHooks.getAllRecipesById(manager, com.hbm_m.recipe.ElectrolyserFluidRecipe.Type.INSTANCE);
        if (data.isEmpty()) return base;
        int fp = 1;
        for (Object r : data.values()) fp = 31 * fp + System.identityHashCode(r);
        Merged m = MERGED.get(manager);
        if (m != null && m.baseVersion == baseVersion && m.fingerprint == fp) return m.map;
        Map<Fluid, ElectrolysisRecipe> merged = new LinkedHashMap<>(base);
        data.entrySet().stream().sorted(Map.Entry.comparingByKey(java.util.Comparator.comparing(Object::toString))).forEach(e -> {
            com.hbm_m.recipe.ElectrolyserFluidRecipe r = e.getValue();
            if (r.getInput().isEmpty()) return; // Eingangsfluid nicht registriert -> ueberspringen
            ElectrolysisRecipe rec = toElectrolysis(r);
            if (rec != null) merged.put(r.getInputFluid(), rec);
        });
        MERGED.put(manager, new Merged(baseVersion, fp, merged));
        return merged;
    }

    /** {@code ElectrolyserFluidRecipe} (JSON) als Code-{@code ElectrolysisRecipe} der Maschine. */
    private static ElectrolysisRecipe toElectrolysis(com.hbm_m.recipe.ElectrolyserFluidRecipe r) {
        ItemStack[] bp = new ItemStack[r.getByproducts().length];
        for (int i = 0; i < bp.length; i++) bp[i] = r.getByproducts()[i].copy();
        return new ElectrolysisRecipe(r.getInputAmount(), out(r.getOutputA()), out(r.getOutputB()), r.getDuration(), bp);
    }

    /** Fehlender Ausgang = {@code NONE}/0 mB (kein zweites Produkt), wie die FFA-Tabelle. */
    private static FluidOut out(@Nullable FluidStack stack) {
        if (stack == null || stack.isEmpty() || stack.getAmount() <= 0) return new FluidOut(ModFluids.NONE.getSource(), 0);
        return new FluidOut(stack.getFluid(), (int) Math.min(Integer.MAX_VALUE, stack.getAmount()));
    }

    /** Rezept fuer ein Eingangsfluid inkl. Datapack ({@code level} null = nur Code-Tabelle). */
    @Nullable
    public static ElectrolysisRecipe getRecipe(@Nullable net.minecraft.world.level.Level level, @Nullable Fluid type) {
        if (type == null) return null;
        return all(level != null ? level.getRecipeManager() : null).get(type);
    }

    public static Map<Fluid, ElectrolysisRecipe> all() {
        ensureLoaded();
        return recipes;
    }

    /**
     * Datapack-Umstellung: die acht FFA-Standardeintraege (Wasser, Schweres Wasser, Vitriol, Slop, Rotschlamm,
     * Tonerde, Kaliumchlorid, Calciumchlorid) liegen als {@code hbm_m:electrolyser_fluid}-JSONs im Datagen
     * ({@code ElectrolyserFluidRecipeGenerator}); die Code-Tabelle bleibt leer.
     */
    private static void registerDefaults() { }

    @Nullable
    public static ElectrolysisRecipe getRecipe(@Nullable Fluid type) {
        if (type == null) return null;
        ensureLoaded();
        return recipes.get(type);
    }

    /** Gegenstueck zu {@code FluidStack}; {@code NONE} mit 0 mB bedeutet: kein zweites Produkt. */
    public record FluidOut(Fluid type, int fill) { }

    public static class ElectrolysisRecipe {
        public FluidOut output1;
        public FluidOut output2;
        public int amount;
        public ItemStack[] byproduct;
        public int duration;

        public ElectrolysisRecipe(int amount, FluidOut output1, FluidOut output2, ItemStack... byproduct) {
            this(amount, output1, output2, 20, byproduct);
        }

        public ElectrolysisRecipe(int amount, FluidOut output1, FluidOut output2, int duration, ItemStack... byproduct) {
            this.output1 = output1;
            this.output2 = output2;
            this.amount = amount;
            this.byproduct = byproduct;
            this.duration = duration;
        }
    }
}
