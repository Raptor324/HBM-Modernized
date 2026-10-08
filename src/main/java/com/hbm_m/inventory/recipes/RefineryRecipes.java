package com.hbm_m.inventory.recipes;

import java.util.LinkedHashMap;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.api.fluids.VanillaFluidEquivalence;
import com.hbm_m.inventory.fluid.ModFluids;
import com.hbm_m.item.ModItems;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;

/**
 * 1:1 {@code RefineryRecipes}: je heissem Oel (Eingang immer 100 mB) vier Fraktionen in Prozent und
 * ein festes Nebenprodukt, das alle {@code maxSulfur} Takte ausgegeben wird. Kaltes Oel hat keinen
 * Eintrag - es muss erst im Heizofen erhitzt werden.
 */
public final class RefineryRecipes {

    /// Fraktionen in Prozent ///
    public static final int oil_frac_heavy = 50;
    public static final int oil_frac_naph = 25;
    public static final int oil_frac_light = 15;
    public static final int oil_frac_petro = 10;
    public static final int crack_frac_naph = 40;
    public static final int crack_frac_light = 30;
    public static final int crack_frac_aroma = 15;
    public static final int crack_frac_unsat = 15;

    public static final int oilds_frac_heavy = 30;
    public static final int oilds_frac_naph = 35;
    public static final int oilds_frac_light = 20;
    public static final int oilds_frac_unsat = 15;
    public static final int crackds_frac_naph = 35;
    public static final int crackds_frac_light = 35;
    public static final int crackds_frac_aroma = 15;
    public static final int crackds_frac_unsat = 15;

    private static final Map<Fluid, RefineryRecipe> recipes = new LinkedHashMap<>();
    private static boolean loaded;

    private RefineryRecipes() { }

    public static synchronized void ensureLoaded() {
        if (loaded) return;
        loaded = true;
        registerDefaults();
    }

    private static void registerDefaults() {
        recipes.put(ModFluids.HOTOIL.getSource(), new RefineryRecipe(
                fs(ModFluids.HEAVYOIL, oil_frac_heavy),
                fs(ModFluids.NAPHTHA, oil_frac_naph),
                fs(ModFluids.LIGHTOIL, oil_frac_light),
                fs(ModFluids.PETROLEUM, oil_frac_petro),
                new ItemStack(ModItems.SULFUR.get())
        ));
        recipes.put(ModFluids.HOTCRACKOIL.getSource(), new RefineryRecipe(
                fs(ModFluids.NAPHTHA_CRACK, crack_frac_naph),
                fs(ModFluids.LIGHTOIL_CRACK, crack_frac_light),
                fs(ModFluids.AROMATICS, crack_frac_aroma),
                fs(ModFluids.UNSATURATEDS, crack_frac_unsat),
                new ItemStack(ModItems.OIL_TAR_CRACK.get())
        ));
        recipes.put(ModFluids.HOTOIL_DS.getSource(), new RefineryRecipe(
                fs(ModFluids.HEAVYOIL, oilds_frac_heavy),
                fs(ModFluids.NAPHTHA_DS, oilds_frac_naph),
                fs(ModFluids.LIGHTOIL_DS, oilds_frac_light),
                fs(ModFluids.UNSATURATEDS, oilds_frac_unsat),
                new ItemStack(ModItems.OIL_TAR_PARAFFIN.get())
        ));
        recipes.put(ModFluids.HOTCRACKOIL_DS.getSource(), new RefineryRecipe(
                fs(ModFluids.NAPHTHA_DS, crackds_frac_naph),
                fs(ModFluids.LIGHTOIL_DS, crackds_frac_light),
                fs(ModFluids.AROMATICS, crackds_frac_aroma),
                fs(ModFluids.UNSATURATEDS, crackds_frac_unsat),
                new ItemStack(ModItems.OIL_TAR_PARAFFIN.get())
        ));
    }

    /** Original {@code getRefinery}; Quelle/Fliessend gelten als dasselbe Fluid. */
    @Nullable
    public static RefineryRecipe getRefinery(@Nullable Fluid oil) {
        if (oil == null) return null;
        ensureLoaded();
        RefineryRecipe direct = recipes.get(oil);
        if (direct != null) return direct;
        for (Map.Entry<Fluid, RefineryRecipe> e : recipes.entrySet()) {
            if (VanillaFluidEquivalence.sameSubstance(e.getKey(), oil)) return e.getValue();
        }
        return null;
    }

    public static Map<Fluid, RefineryRecipe> all() {
        ensureLoaded();
        return recipes;
    }

    /** Gegenstueck zu {@code FluidStack}. */
    public record FluidOut(Fluid type, int fill) { }

    private static FluidOut fs(ModFluids.FluidEntry entry, int fill) {
        return new FluidOut(entry.getSource(), fill);
    }

    public static class RefineryRecipe {

        public final FluidOut[] outputs;
        public final ItemStack solid;

        public RefineryRecipe(FluidOut f0, FluidOut f1, FluidOut f2, FluidOut f3, ItemStack f4) {
            this.outputs = new FluidOut[] {f0, f1, f2, f3};
            this.solid = f4;
        }
    }
}
