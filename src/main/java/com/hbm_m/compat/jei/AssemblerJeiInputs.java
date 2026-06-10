package com.hbm_m.compat.jei;

import java.util.ArrayList;
import java.util.List;

import com.hbm_m.recipe.AssemblerRecipe.AssemblerInputSlot;

import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

/**
 * Fallback helpers when legacy recipes lack {@link AssemblerInputSlot} data.
 */
public final class AssemblerJeiInputs {

    private AssemblerJeiInputs() {
    }

    public static List<AssemblerInputSlot> fallbackFromExpanded(NonNullList<Ingredient> ingredients) {
        List<AssemblerInputSlot> slots = new ArrayList<>();

        for (Ingredient ingredient : ingredients) {
            if (ingredient.isEmpty()) {
                continue;
            }
            if (!slots.isEmpty()) {
                AssemblerInputSlot previous = slots.get(slots.size() - 1);
                if (previous.ingredient() == ingredient) {
                    slots.set(slots.size() - 1, new AssemblerInputSlot(ingredient, previous.count() + 1));
                    continue;
                }
            }
            slots.add(new AssemblerInputSlot(ingredient, 1));
        }

        return slots;
    }
}
