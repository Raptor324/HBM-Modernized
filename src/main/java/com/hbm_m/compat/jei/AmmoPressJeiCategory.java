package com.hbm_m.compat.jei;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.recipe.AmmoPressRecipe;

import java.util.ArrayList;
import java.util.List;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * JEI port of {@code AmmoPressHandler} (1:1 {@code NEIUniversalHandler}) - siehe
 * {@link AmmoPressRecipe}/{@code MachineAmmoPressBlockEntity}. Wie im Original
 * ({@code AmmoPressRecipes.getRecipes}) werden die 9 positionsgleichen Grid-Felder auf die
 * belegten komprimiert und dynamisch nach Anzahl layoutet ({@code NEIUniversalHandler.getInputCoords}:
 * 1-3 Felder -> Mittelreihe rechts-nach-links, 4 Felder -> 2x2-Block); Ausgabe bei (102, 24),
 * Maschine im hohen Rahmen bei (75, 31). Mengen erscheinen wie dort als Stackgroesse
 * ({@code AStack.stacksize} -> {@code AmmoPressRecipe#getCount}).
 */
public class AmmoPressJeiCategory extends JeiGenericRecipeCategory<AmmoPressRecipe> {

    public static final RecipeType<AmmoPressRecipe> RECIPE_TYPE =
            RecipeType.create(RefStrings.MODID, "ammo_press", AmmoPressRecipe.class);

    public AmmoPressJeiCategory(IGuiHelper guiHelper) {
        super(guiHelper, new ItemStack[]{
                new ItemStack(ModBlocks.AMMO_PRESS.get())
        });
    }

    @Override
    public RecipeType<AmmoPressRecipe> getRecipeType() {
        return RECIPE_TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("container.hbm_m.ammo_press");
    }

    @Override
    protected int getInputCount(AmmoPressRecipe recipe) {
        int count = 0;
        for (var ingredient : recipe.getInputs()) {
            if (!ingredient.isEmpty()) count++;
        }
        return count;
    }

    @Override
    protected int getOutputCount(AmmoPressRecipe recipe) {
        return 1;
    }

    @Override
    protected boolean hasBlueprintTemplate(AmmoPressRecipe recipe) {
        return false;
    }

    @Override
    protected void addInputSlots(IRecipeLayoutBuilder builder, AmmoPressRecipe recipe, int inputXOffset) {
        var inputs = recipe.getInputs();
        List<Integer> filled = new ArrayList<>(inputs.size());
        for (int index = 0; index < inputs.size(); index++) {
            if (!inputs.get(index).isEmpty()) filled.add(index);
        }
        int[][] positions = JeiNeiLayout.getUniversalInputCoords(filled.size());
        for (int i = 0; i < filled.size() && i < positions.length; i++) {
            int gridIndex = filled.get(i);
            var slot = addItemSlot(builder, RecipeIngredientRole.INPUT,
                    inputXOffset + positions[i][0], positions[i][1]);
            JeiIngredientSlots.addCountedIngredient(slot, inputs.get(gridIndex), recipe.getCount(gridIndex));
        }
    }

    @Override
    protected void addOutputSlots(IRecipeLayoutBuilder builder, AmmoPressRecipe recipe, int outputXOffset) {
        int[][] positions = JeiNeiLayout.getUniversalOutputCoords(1);
        addItemSlot(builder, RecipeIngredientRole.OUTPUT, outputXOffset + positions[0][0], positions[0][1])
                .addItemStack(recipe.getOutput());
    }

    @Override
    protected void addBlueprintSlot(IRecipeLayoutBuilder builder, AmmoPressRecipe recipe, int machineXOffset) {
        // Kein Blueprint-Slot fuer Ammo-Press-Rezepte.
    }
}
