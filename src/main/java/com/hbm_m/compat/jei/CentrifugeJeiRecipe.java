package com.hbm_m.compat.jei;

import net.minecraft.world.item.ItemStack;
import java.util.Arrays;
import java.util.List;

/**
 * Simple wrapper class for displaying Centrifuge recipes in JEI.
 * This does not implement Recipe<Container>, it's just a display object.
 */
public class CentrifugeJeiRecipe {
    private final List<ItemStack> inputStacks;
    private final List<ItemStack> outputs;

    public CentrifugeJeiRecipe(List<ItemStack> inputStacks, ItemStack[] outputs) {
        this.inputStacks = inputStacks;
        this.outputs = Arrays.asList(outputs);
    }

    public List<ItemStack> getInputStacks() {
        return inputStacks;
    }

    public List<ItemStack> getOutputs() {
        return outputs;
    }
}
