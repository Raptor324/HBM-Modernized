package com.hbm_m.compat.jei;

import java.util.Arrays;
import java.util.List;

import net.minecraft.world.item.ItemStack;

public class CyclotronJeiRecipe {
    private final List<ItemStack> targetStacks;
    private final List<ItemStack> inputStacks;
    private final ItemStack output;
    private final int amatProduced;

    public CyclotronJeiRecipe(List<ItemStack> targetStacks, List<ItemStack> inputStacks, ItemStack output, int amatProduced) {
        this.targetStacks = targetStacks;
        this.inputStacks = inputStacks;
        this.output = output;
        this.amatProduced = amatProduced;
    }

    public static CyclotronJeiRecipe of(net.minecraft.world.item.crafting.Ingredient target,
            net.minecraft.world.item.crafting.Ingredient input,
            ItemStack output,
            int amatProduced) {
        return new CyclotronJeiRecipe(Arrays.asList(target.getItems()), Arrays.asList(input.getItems()), output.copy(), amatProduced);
    }

    public List<ItemStack> getTargetStacks() {
        return targetStacks;
    }

    public List<ItemStack> getInputStacks() {
        return inputStacks;
    }

    public ItemStack getOutput() {
        return output;
    }

    public int getAmatProduced() {
        return amatProduced;
    }
}
