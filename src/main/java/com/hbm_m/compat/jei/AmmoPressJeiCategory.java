package com.hbm_m.compat.jei;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.recipe.AmmoPressRecipe;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * JEI category for Ammo Press recipes - 9 positionsfeste 3x3-Slots -&gt; 1 Ausgabe (siehe
 * {@link AmmoPressRecipe}/{@code MachineAmmoPressBlockEntity}).
 */
//? if forge {
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
        return AmmoPressRecipe.GRID_SIZE;
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
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                int index = row * 3 + col;
                var ingredient = inputs.get(index);
                if (ingredient.isEmpty()) continue;
                var slot = addItemSlot(builder, RecipeIngredientRole.INPUT,
                        inputXOffset + col * 18, 4 + row * 18);
                JeiIngredientSlots.addCountedIngredient(slot, ingredient, 1);
            }
        }
    }

    @Override
    protected void addOutputSlots(IRecipeLayoutBuilder builder, AmmoPressRecipe recipe, int outputXOffset) {
        addItemSlot(builder, RecipeIngredientRole.OUTPUT, outputXOffset + 4, 22)
                .addItemStack(recipe.getOutput());
    }

    @Override
    protected void addBlueprintSlot(IRecipeLayoutBuilder builder, AmmoPressRecipe recipe, int machineXOffset) {
        // Kein Blueprint-Slot fuer Ammo-Press-Rezepte.
    }
}
//?} else {
/*public final class AmmoPressJeiCategory {
    private AmmoPressJeiCategory() {}
}*///?}
