package com.hbm_m.compat.jei;

import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * JEI display wrapper for a single crucible smelting recipe.
 *
 * Slot layout (matches legacy NEI CrucibleSmeltingHandler):
 *   input   @ x=48,  y=24   — single stack
 *   crucible@ x=75,  y=42   — catalyst
 *   outputs @ 3-wide grid: x=102+(col*18), y=6+(row*18) — up to 6 stacks
 */
public class CrucibleSmeltingJeiRecipe {

    private final ItemStack input;
    private final List<ItemStack> outputs;

    public CrucibleSmeltingJeiRecipe(ItemStack input, List<ItemStack> outputs) {
        this.input   = input;
        this.outputs = outputs;
    }

    public ItemStack getInput()         { return input; }
    public List<ItemStack> getOutputs() { return outputs; }
}
