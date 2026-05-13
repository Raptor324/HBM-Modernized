package com.hbm_m.compat.jei;

import net.minecraft.world.item.ItemStack;

/**
 * JEI display wrapper for a single crucible mold-casting recipe.
 * Matches the slot layout of the legacy NEI CrucibleCastingHandler:
 *   input  @ (48, 24)
 *   mold   @ (75,  6)
 *   basin  @ (75, 42)  — foundry basin as catalyst display
 *   output @ (102, 24)
 */
public class CrucibleCastingJeiRecipe {

    private final ItemStack input;
    private final ItemStack mold;
    private final ItemStack output;

    public CrucibleCastingJeiRecipe(ItemStack input, ItemStack mold, ItemStack output) {
        this.input  = input;
        this.mold   = mold;
        this.output = output;
    }

    public ItemStack getInput()  { return input; }
    public ItemStack getMold()   { return mold; }
    public ItemStack getOutput() { return output; }
}
