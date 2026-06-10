package com.hbm_m.compat.jei;

import java.util.ArrayList;
import java.util.List;

import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

public final class JeiIngredientSlots {

    private JeiIngredientSlots() {
    }

    /**
     * Adds a counted ingredient to a JEI slot. Tag-based ingredients use {@code addIngredients};
     * concrete items are shown as stacks with the required count.
     */
    public static void addCountedIngredient(IRecipeSlotBuilder builder, Ingredient ingredient, int count) {
        ItemStack[] items = ingredient.getItems();
        if (items.length > 0) {
            List<ItemStack> stacks = new ArrayList<>(items.length);
            for (ItemStack item : items) {
                ItemStack copy = item.copy();
                copy.setCount(count);
                stacks.add(copy);
            }
            builder.addItemStacks(stacks);
        } else {
            builder.addIngredients(ingredient);
        }
    }
}
