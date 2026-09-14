package com.hbm_m.compat.jei;

import java.util.List;

import dev.architectury.fluid.FluidStack;
//? if forge {
import mezz.jei.api.forge.ForgeTypes;
import dev.architectury.hooks.fluid.forge.FluidStackHooksForge;
//?} elif neoforge {
/*import mezz.jei.api.neoforge.NeoForgeTypes;
*///?}
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.RecipeIngredientRole;
import net.minecraft.world.item.ItemStack;

//? if forge {
/**
 * Gemeinsame Grundlage aller JEI-Kategorien, die ueberwiegend mit Fluessigkeiten arbeiten -
 * Reformer, Verkoker, Fraktionierturm, Hydrotreater, Vakuumdestillation, Bruetreaktor,
 * Verfluessiger, Verfestigung und Pyrolyseofen.
 *
 * <p>Sie alle sehen gleich aus: ein paar Eingaenge links, ein paar Ergebnisse rechts, dazwischen
 * der Maschinenblock. Die Unterklassen sagen nur noch, <b>was</b> hinein- und herauskommt; das
 * Zeichnen der Plaetze steht hier einmal. Fluessigkeiten und feste Stoffe lassen sich dabei frei
 * mischen - der Pyrolyseofen etwa nimmt beides und gibt beides zurueck.</p>
 *
 * <p>Der Aufbau ist derselbe wie in {@code CrackingTowerJeiCategory}, nur ohne die dortige
 * Sonderbehandlung von Dampf.</p>
 */
public abstract class JeiFluidRecipeCategory<R> extends JeiGenericRecipeCategory<R> {

    /** Wie voll der Fluessigkeitsplatz gezeichnet wird - rein optisch. */
    private static final int FLUID_RENDERER_CAPACITY = 24_000;

    protected JeiFluidRecipeCategory(IGuiHelper guiHelper, ItemStack[] machines) {
        super(guiHelper, machines);
    }

    /** Die Eingangsfluessigkeiten des Rezepts, in Anzeigereihenfolge. */
    protected abstract List<FluidStack> getInputFluids(R recipe);

    /** Die Ergebnisfluessigkeiten des Rezepts, in Anzeigereihenfolge. */
    protected abstract List<FluidStack> getOutputFluids(R recipe);

    /** Feste Eingaenge - etwa beim Brueter, Verfluessiger und Pyrolyseofen. */
    protected List<net.minecraft.world.item.crafting.Ingredient> getInputItems(R recipe) {
        return List.of();
    }

    /** Feste Ergebnisse - etwa beim Verkoker und der Verfestigung. */
    protected List<ItemStack> getOutputItems(R recipe) {
        return List.of();
    }

    @Override
    protected int getInputCount(R recipe) {
        return getInputFluids(recipe).size() + getInputItems(recipe).size();
    }

    @Override
    protected int getOutputCount(R recipe) {
        return getOutputFluids(recipe).size() + getOutputItems(recipe).size();
    }

    @Override
    protected boolean hasBlueprintTemplate(R recipe) {
        return false;
    }

    @Override
    protected void addInputSlots(IRecipeLayoutBuilder builder, R recipe, int inputXOffset) {
        List<FluidStack> fluids = getInputFluids(recipe);
        List<net.minecraft.world.item.crafting.Ingredient> items = getInputItems(recipe);

        int[][] positions = JeiNeiLayout.getGenericInputSlotPositions(fluids.size() + items.size());
        int slot = 0;

        for (FluidStack fluid : fluids) {
            addFluidSlot(builder, RecipeIngredientRole.INPUT,
                    positions[slot][0] + inputXOffset, positions[slot][1], fluid);
            slot++;
        }

        for (net.minecraft.world.item.crafting.Ingredient ingredient : items) {
            addItemSlot(builder, RecipeIngredientRole.INPUT,
                    positions[slot][0] + inputXOffset, positions[slot][1])
                    .addItemStacks(java.util.Arrays.asList(ingredient.getItems()));
            slot++;
        }
    }

    @Override
    protected void addOutputSlots(IRecipeLayoutBuilder builder, R recipe, int outputXOffset) {
        List<FluidStack> fluids = getOutputFluids(recipe);
        List<ItemStack> items = getOutputItems(recipe);

        int[][] positions = JeiNeiLayout.getGenericOutputSlotPositions(fluids.size() + items.size());
        int slot = 0;

        for (FluidStack fluid : fluids) {
            addFluidSlot(builder, RecipeIngredientRole.OUTPUT,
                    positions[slot][0] + outputXOffset, positions[slot][1], fluid);
            slot++;
        }

        for (ItemStack stack : items) {
            addItemSlot(builder, RecipeIngredientRole.OUTPUT,
                    positions[slot][0] + outputXOffset, positions[slot][1])
                    .addItemStack(stack);
            slot++;
        }
    }

    protected void addFluidSlot(IRecipeLayoutBuilder builder, RecipeIngredientRole role,
                                int x, int y, FluidStack fluid) {
        addItemSlot(builder, role, x, y)
                .setFluidRenderer(FLUID_RENDERER_CAPACITY, false, 16, 16)
                //? if forge {
                .setCustomRenderer(ForgeTypes.FLUID_STACK, new HbmFluidJeiRenderer(16, 16))
                .addIngredient(ForgeTypes.FLUID_STACK, FluidStackHooksForge.toForge(fluid));
                //?} elif neoforge {
                /*.setCustomRenderer(NeoForgeTypes.FLUID_STACK, new HbmFluidJeiRenderer(16, 16))
                .addIngredient(NeoForgeTypes.FLUID_STACK, new net.neoforged.neoforge.fluids.FluidStack(fluid.getFluid(), (int) fluid.getAmount()));
                *///?}
    }

    @Override
    protected void addBlueprintSlot(IRecipeLayoutBuilder builder, R recipe, int machineXOffset) {
        // Diese Maschinen kennen keine Blaupausen.
    }
}
//?} else {
/*public abstract class JeiFluidRecipeCategory<R> {
    protected JeiFluidRecipeCategory() {}
}*///?}
