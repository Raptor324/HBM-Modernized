package com.hbm_m.recipe;

import javax.annotation.Nullable;

import com.hbm_m.inventory.fluid.ModFluids;
import com.hbm_m.recipe.CrackingTowerRecipes.Crack;

import net.minecraft.world.level.material.Fluid;

/**
 * Port of {@code com.hbm.inventory.recipes.RadiolysisRecipes} (1.7.10 Original). The original
 * auto-imports every {@code CrackingRecipes} entry (oil-cracking pairs) plus one dedicated
 * Water -> Peroxide + Hydrogen entry; this port reuses {@link CrackingTowerRecipes} directly as
 * the shared source of truth (already ported 1:1 this session) instead of duplicating the table.
 */
public final class RadiolysisRecipes {

    private RadiolysisRecipes() {}

    @Nullable
    public static Crack get(Fluid input) {
        if (input.isSame(ModFluids.WATER.getSource())) {
            return new Crack(ModFluids.PEROXIDE.getSource(), 80, ModFluids.HYDROGEN.getSource(), 20);
        }
        return CrackingTowerRecipes.get(input);
    }
}
