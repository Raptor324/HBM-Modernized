package com.hbm_m.recipe;

import net.minecraft.world.item.ItemStack;
import java.util.List;

public class AnvilRecipe {
    private final ItemStack inputA;
    private final ItemStack inputB;
    private final ItemStack output;
    private final List<ItemStack> requiredItems;
    private final String recipeName;

    public AnvilRecipe(ItemStack inputA, ItemStack inputB, ItemStack output, List<ItemStack> requiredItems, String recipeName) {
        this.inputA = inputA;
        this.inputB = inputB;
        this.output = output;
        this.requiredItems = requiredItems;
        this.recipeName = recipeName;
    }

    public ItemStack getInputA() {
        return inputA;
    }

    public ItemStack getInputB() {
        return inputB;
    }

    public ItemStack getOutput() {
        return output;
    }

    public List<ItemStack> getRequiredItems() {
        return requiredItems;
    }

    public String getRecipeName() {
        return recipeName;
    }

    public boolean matches(ItemStack slotA, ItemStack slotB) {
        return ItemStack.isSameItemSameTags(slotA, inputA) &&
                ItemStack.isSameItemSameTags(slotB, inputB) &&
                slotA.getCount() >= inputA.getCount() &&
                slotB.getCount() >= inputB.getCount();
    }
}