package com.hbm_m.compat.jei;

import com.hbm_m.item.rbmk.RBMKPelletItem;
import com.hbm_m.item.rbmk.RBMKRodItem;

import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * One entry of the RBMK rod disassembly listing, ported from the original's
 * {@code RBMKRodDisassemblyHandler}: a spent rod at a given enrichment tier yields eight pellets
 * in the matching condition.
 *
 * @param rod     the rod as it goes in, its stored yield set to the tier being shown
 * @param pellets the eight pellets that come out, already carrying their depletion/xenon state
 */
public record RBMKDisassemblyJeiRecipe(ItemStack rod, ItemStack pellets) {

    /**
     * Builds the full listing exactly like the original: every craftable rod, crossed with the
     * five enrichment tiers, plus the xenon-poisoned variant for the pellet types that have one.
     */
    public static List<RBMKDisassemblyJeiRecipe> all() {
        List<RBMKDisassemblyJeiRecipe> recipes = new ArrayList<>();

        for (RBMKRodItem rod : RBMKRodItem.craftableRods) {
            RBMKPelletItem pellet = rod.getPellet();
            if (pellet == null) continue;

            for (int tier = 0; tier <= 4; tier++) {
                recipes.add(new RBMKDisassemblyJeiRecipe(rodAt(rod, tier, false), pellet.withState(8, tier)));

                if (pellet.isXenonEnabled())
                    recipes.add(new RBMKDisassemblyJeiRecipe(rodAt(rod, tier, true), pellet.withState(8, tier + 5)));
            }
        }
        return recipes;
    }

    /**
     * A rod stack whose remaining yield matches the shown tier. Tier 0 is a fresh rod and tier 4 a
     * fully spent one, which is the inverse of the enrichment fraction the disassembly recipe
     * reads back out. Mirrors the original's {@code ComparableStackHeat.toStack()}: the yield is
     * capped at 99 % so even the freshest tier still reads as very slightly depleted, and the
     * xenon-poisoned variants carry 50 poison on the rod going in.
     */
    private static ItemStack rodAt(RBMKRodItem rod, int tier, boolean hasPoison) {
        ItemStack stack = new ItemStack(rod);
        RBMKRodItem.setYield(stack, Math.min(1.0 - tier / 5.0, 0.99) * rod.yield);
        if (hasPoison) RBMKRodItem.setPoison(stack, 50);
        RBMKRodItem.setCoreHeat(stack, 20.0);
        RBMKRodItem.setHullHeat(stack, 20.0);
        return stack;
    }
}
