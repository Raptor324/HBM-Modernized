package com.hbm_m.compat.jei;

import com.hbm_m.recipe.CrucibleAlloyingRecipe;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * JEI display wrapper for a single crucible alloying recipe.
 *
 * Slot layout (matches legacy NEI CrucibleAlloyingHandler):
 *   inputs  → 3-wide grid: x=12+(col*18), y=6+(row*18)   — up to 6 stacks
 *   crucible→ catalyst at x=75, y=42
 *   outputs → 3-wide grid: x=102+(col*18), y=6+(row*18)  — up to 6 stacks
 */
public class CrucibleAlloyingJeiRecipe {

    private final List<ItemStack> inputs;
    private final List<ItemStack> outputs;

    public CrucibleAlloyingJeiRecipe(CrucibleAlloyingRecipe recipe) {
        this.inputs  = recipe.getInputs();
        this.outputs = recipe.getOutputs();
    }

    public List<ItemStack> getInputs()  { return inputs; }
    public List<ItemStack> getOutputs() { return outputs; }
}
