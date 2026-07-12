package com.hbm_m.recipe;

import com.hbm_m.inventory.material.MaterialStack;

/**
 * A crucible alloying reaction: combines several molten materials held
 * simultaneously in the crucible's pool into one or more different molten
 * outputs. Direct port of the 1.7.10 {@code CrucibleRecipe} concept
 * (input MaterialStack[] + output MaterialStack[]), minus the item-based
 * icon/JEI bookkeeping which lives in {@link CrucibleAlloyingRecipe} instead.
 */
public class MoltenAlloyRecipe {

    public final String name;
    /** Ticks between processing attempts — mirrors legacy {@code CrucibleRecipe.frequency}. */
    public final int frequency;
    public final MaterialStack[] inputs;
    public final MaterialStack[] outputs;

    public MoltenAlloyRecipe(String name, int frequency, MaterialStack[] inputs, MaterialStack[] outputs) {
        this.name = name;
        this.frequency = frequency;
        this.inputs = inputs;
        this.outputs = outputs;
    }
}
