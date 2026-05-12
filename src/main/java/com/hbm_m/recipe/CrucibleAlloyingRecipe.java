package com.hbm_m.recipe;

import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * Concrete crucible alloying recipe — extends {@link CrucibleRecipe}.
 *
 * <p>Alloying combines multiple input materials into one or more outputs inside the crucible.
 * This class exists as a distinct type so JEI can register a separate category for alloying
 * vs. casting vs. smelting.
 *
 * <p>Preferred construction via the inherited builder API:
 * <pre>{@code
 * new CrucibleAlloyingRecipe("bronze_alloy")
 *         .setup(1, new ItemStack(ModItems.INGOT_BRONZE.get()))
 *         .inputs(copperStack, tinStack)
 *         .outputs(bronzeStack);
 * }</pre>
 *
 * <p>Legacy equivalent: {@code CrucibleRecipe} entries registered into
 * {@code CrucibleRecipes.INSTANCE.recipeOrderedList}.
 */
public class CrucibleAlloyingRecipe extends CrucibleRecipe {

    /** Convenience constructor — equivalent to {@code new CrucibleRecipe(name)}. */
    public CrucibleAlloyingRecipe(String name) {
        super(name);
    }

    /**
     * Legacy-compatible constructor for migration convenience.
     * Prefer the builder-style API going forward.
     */
    public CrucibleAlloyingRecipe(List<ItemStack> inputs, List<ItemStack> outputs) {
        super("legacy_" + System.identityHashCode(inputs));
        inputs(inputs);
        outputs(outputs);
    }

    @Override
    public CrucibleAlloyingRecipe setup(int frequency, ItemStack icon) {
        super.setup(frequency, icon);
        return this;
    }

    @Override
    public CrucibleAlloyingRecipe inputs(ItemStack... inputs) {
        super.inputs(inputs);
        return this;
    }

    @Override
    public CrucibleAlloyingRecipe inputs(List<ItemStack> inputs) {
        super.inputs(inputs);
        return this;
    }

    @Override
    public CrucibleAlloyingRecipe outputs(ItemStack... outputs) {
        super.outputs(outputs);
        return this;
    }

    @Override
    public CrucibleAlloyingRecipe outputs(List<ItemStack> outputs) {
        super.outputs(outputs);
        return this;
    }
}

