package com.hbm_m.inventory.recipes;

import java.util.LinkedHashMap;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.inventory.fluid.ModFluids;
import com.hbm_m.item.ModItems;
import com.hbm_m.item.material.MaterialShape;
import com.hbm_m.item.material.ModMaterialItems;
import com.hbm_m.item.material.ModMaterials;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;

/**
 * 1:1 {@code ElectrolyserFluidRecipes}: je Eingangsfluid eine Menge, zwei Ausgangsfluide, optionale Nebenprodukte
 * und eine Dauer (Standard 20 Ticks).
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

    // ---------------------------------------------------------------- W-R9B5-2: Datapack-Rezepte (hbm_m:electrolysis_fluid)

    private static int baseVersion;

    private record Merged(int baseVersion, int fingerprint, Map<Fluid, ElectrolysisRecipe> map) { }

    private static final java.util.WeakHashMap<net.minecraft.world.item.crafting.RecipeManager, Merged> MERGED = new java.util.WeakHashMap<>();

    /**
     * Code-Tabelle plus {@link com.hbm_m.recipe.ElectrolysisFluidDataRecipe}s des RecipeManagers (gleiches Eingangsfluid =
     * ersetzt, Reihenfolge Rezept-ID). Ohne solche JSONs (HBM liefert keine) identisch mit {@link #all()}.
     */
    public static synchronized Map<Fluid, ElectrolysisRecipe> all(@Nullable net.minecraft.world.item.crafting.RecipeManager manager) {
        Map<Fluid, ElectrolysisRecipe> base = all();
        if (manager == null) return base;
        Map<net.minecraft.resources.ResourceLocation, com.hbm_m.recipe.ElectrolysisFluidDataRecipe> data =
                com.hbm_m.platform.recipe.RecipeHooks.getAllRecipesById(manager, com.hbm_m.recipe.ElectrolysisFluidDataRecipe.Type.INSTANCE);
        if (data.isEmpty()) return base;
        int fp = 1;
        for (Object r : data.values()) fp = 31 * fp + System.identityHashCode(r);
        Merged m = MERGED.get(manager);
        if (m != null && m.baseVersion == baseVersion && m.fingerprint == fp) return m.map;
        Map<Fluid, ElectrolysisRecipe> merged = new LinkedHashMap<>(base);
        data.entrySet().stream().sorted(Map.Entry.comparingByKey(java.util.Comparator.comparing(Object::toString))).forEach(e -> {
            Fluid in = e.getValue().inputFluid();
            ElectrolysisRecipe rec = e.getValue().toRecipe();
            if (in != null && rec != null) merged.put(in, rec);
        });
        MERGED.put(manager, new Merged(baseVersion, fp, merged));
        return merged;
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

    private static void registerDefaults() {
        recipes.put(ModFluids.WATER.getSource(), new ElectrolysisRecipe(2_000, fs(ModFluids.HYDROGEN, 200), fs(ModFluids.OXYGEN, 200), 10));
        recipes.put(ModFluids.HEAVYWATER.getSource(), new ElectrolysisRecipe(2_000, fs(ModFluids.DEUTERIUM, 200), fs(ModFluids.OXYGEN, 200), 10));
        recipes.put(ModFluids.VITRIOL.getSource(), new ElectrolysisRecipe(1_000, fs(ModFluids.SULFURIC_ACID, 500), fs(ModFluids.CHLORINE, 500),
                new ItemStack(ModMaterialItems.item(ModMaterials.IRON, MaterialShape.POWDER)), new ItemStack(ModItems.NUGGET_MERCURY.get())));
        recipes.put(ModFluids.SLOP.getSource(), new ElectrolysisRecipe(1_000, fs(ModFluids.MERCURY, 250), none(),
                new ItemStack(ModItems.NITER.get(), 2), new ItemStack(ModMaterialItems.item(ModMaterials.LIMESTONE, MaterialShape.POWDER), 2), new ItemStack(ModItems.SULFUR.get())));
        recipes.put(ModFluids.REDMUD.getSource(), new ElectrolysisRecipe(450, fs(ModFluids.MERCURY, 150), fs(ModFluids.LYE, 50),
                new ItemStack(ModMaterialItems.item(ModMaterials.TITANIUM, MaterialShape.POWDER), 3),
                new ItemStack(ModMaterialItems.item(ModMaterials.IRON, MaterialShape.POWDER), 3),
                new ItemStack(ModMaterialItems.item(ModMaterials.ALUMINUM, MaterialShape.POWDER), 2)));
        recipes.put(ModFluids.ALUMINA.getSource(), new ElectrolysisRecipe(200, fs(ModFluids.CARBONDIOXIDE, 100), none(), 40,
                new ItemStack(ModMaterialItems.item(ModMaterials.ALUMINUM, MaterialShape.POWDER), 7), new ItemStack(ModItems.FLUORITE.get(), 2)));

        recipes.put(ModFluids.POTASSIUM_CHLORIDE.getSource(), new ElectrolysisRecipe(250, fs(ModFluids.CHLORINE, 125), none(), new ItemStack(ModItems.DUST.get())));
        recipes.put(ModFluids.CALCIUM_CHLORIDE.getSource(), new ElectrolysisRecipe(250, fs(ModFluids.CHLORINE, 125), fs(ModFluids.CALCIUM_SOLUTION, 125)));
    }

    @Nullable
    public static ElectrolysisRecipe getRecipe(@Nullable Fluid type) {
        if (type == null) return null;
        ensureLoaded();
        return recipes.get(type);
    }

    /** Gegenstueck zu {@code FluidStack}; {@code NONE} mit 0 mB bedeutet: kein zweites Produkt. */
    public record FluidOut(Fluid type, int fill) { }

    private static FluidOut fs(ModFluids.FluidEntry entry, int fill) {
        return new FluidOut(entry.getSource(), fill);
    }

    private static FluidOut none() {
        return new FluidOut(ModFluids.NONE.getSource(), 0);
    }

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
