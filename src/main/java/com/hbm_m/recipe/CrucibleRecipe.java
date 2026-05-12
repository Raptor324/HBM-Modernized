package com.hbm_m.recipe;

import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Modern port of legacy {@code CrucibleRecipe extends GenericRecipe}.
 *
 * <p>Each recipe has a {@link #name} (used as a translation key), a display {@link #icon},
 * a {@link #frequency} (relative weight for JEI ordering) and immutable input / output
 * item lists.
 *
 * <p><b>MaterialStack TODO:</b> The legacy class stored {@code MaterialStack[]} arrays so
 * that amounts were expressed in the Mats unit system (mB-like). Once
 * {@code com.hbm_m.inventory.material.Mats} and {@code MaterialStack} are ported, replace
 * {@code List<ItemStack>} with {@code MaterialStack[]} and restore {@code getInputAmount()}.
 *
 * <p>Builder usage (mirrors legacy API):
 * <pre>{@code
 * CrucibleRecipe recipe = new CrucibleRecipe("steel_alloy")
 *         .setup(1, new ItemStack(ModItems.INGOT_STEEL.get()))
 *         .inputs(ironStack, carbonStack)
 *         .outputs(steelStack);
 * }</pre>
 */
public class CrucibleRecipe {

    /** Internal identifier / translation-key base (mirrors {@code GenericRecipe.name}). */
    public final String name;

    /**
     * Relative processing frequency / weight.
     * Higher values mean the recipe is attempted more often in random-pick scenarios.
     * Default: 1.
     */
    public int frequency = 1;

    /** Icon displayed in JEI and the recipe-selector screen. */
    private ItemStack icon = ItemStack.EMPTY;

    /** Input items for this recipe. */
    private List<ItemStack> inputs = Collections.emptyList();

    /** Output items produced by this recipe. */
    private List<ItemStack> outputs = Collections.emptyList();

    // -------------------------------------------------------------------------
    // Construction
    // -------------------------------------------------------------------------

    public CrucibleRecipe(String name) {
        this.name = name;
    }

    // -------------------------------------------------------------------------
    // Builder methods (mirror legacy CrucibleRecipe.setup / .inputs / .outputs)
    // -------------------------------------------------------------------------

    /**
     * Sets the frequency weight and the display icon.
     *
     * @param frequency relative weight (≥ 1)
     * @param icon      display ItemStack shown in JEI / recipe selector
     * @return {@code this} for chaining
     */
    public CrucibleRecipe setup(int frequency, ItemStack icon) {
        this.frequency = frequency;
        this.icon = icon.copy();
        return this;
    }

    /** Sets the input item list. Defensive copy is made. */
    public CrucibleRecipe inputs(ItemStack... inputs) {
        List<ItemStack> copy = new ArrayList<>(inputs.length);
        for (ItemStack s : inputs) copy.add(s.copy());
        this.inputs = Collections.unmodifiableList(copy);
        return this;
    }

    /** Sets the input item list from an existing collection. Defensive copy is made. */
    public CrucibleRecipe inputs(List<ItemStack> inputs) {
        return inputs(inputs.toArray(new ItemStack[0]));
    }

    /** Sets the output item list. Defensive copy is made. */
    public CrucibleRecipe outputs(ItemStack... outputs) {
        List<ItemStack> copy = new ArrayList<>(outputs.length);
        for (ItemStack s : outputs) copy.add(s.copy());
        this.outputs = Collections.unmodifiableList(copy);
        return this;
    }

    /** Sets the output item list from an existing collection. Defensive copy is made. */
    public CrucibleRecipe outputs(List<ItemStack> outputs) {
        return outputs(outputs.toArray(new ItemStack[0]));
    }

    // -------------------------------------------------------------------------
    // Accessors
    // -------------------------------------------------------------------------

    public List<ItemStack> getInputs()  { return inputs; }
    public List<ItemStack> getOutputs() { return outputs; }
    public ItemStack       getIcon()    { return icon; }

    /**
     * Returns the total number of input items across all input stacks.
     * In the legacy code this summed {@code MaterialStack.amount} values;
     * here it sums {@link ItemStack#getCount()} as a placeholder.
     *
     * <p>TODO: once MaterialStack is ported, sum {@code MaterialStack.amount} instead.
     */
    public int getInputAmount() {
        int total = 0;
        for (ItemStack stack : inputs) total += stack.getCount();
        return total;
    }

    /** Returns a human-readable name derived from the recipe's name key. */
    public String getLocalizedName() {
        // TODO: once I18nUtil is ported, delegate to I18nUtil.resolveKey("crucible." + name)
        return name;
    }
}
