package com.hbm_m.compat.jei;

import com.hbm_m.lib.RefStrings;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * JEI listing for taking an RBMK fuel rod apart at a crafting table, ported from the original's
 * {@code RBMKRodDisassemblyHandler}. Without it the disassembly recipe is invisible: it is a
 * special recipe with no JSON ingredients, so nothing in the recipe viewer would ever mention that
 * a spent rod can be recovered as pellets.
 */
public class RBMKDisassemblyJeiCategory extends JeiGenericRecipeCategory<RBMKDisassemblyJeiRecipe> {

    public static final RecipeType<RBMKDisassemblyJeiRecipe> RECIPE_TYPE =
            RecipeType.create(RefStrings.MODID, "rbmk_disassembly", RBMKDisassemblyJeiRecipe.class);

    public RBMKDisassemblyJeiCategory(IGuiHelper guiHelper) {
        super(guiHelper, new ItemStack[]{ new ItemStack(Items.CRAFTING_TABLE) });
    }

    @Override public RecipeType<RBMKDisassemblyJeiRecipe> getRecipeType() { return RECIPE_TYPE; }

    @Override
    public Component getTitle() {
        return Component.translatable("gui.hbm_m.jei.rbmk_disassembly");
    }

    @Override protected int getInputCount(RBMKDisassemblyJeiRecipe recipe)  { return 1; }
    @Override protected int getOutputCount(RBMKDisassemblyJeiRecipe recipe) { return 1; }
    @Override protected boolean hasBlueprintTemplate(RBMKDisassemblyJeiRecipe recipe) { return false; }

    @Override
    protected void addInputSlots(IRecipeLayoutBuilder builder, RBMKDisassemblyJeiRecipe recipe, int inputXOffset) {
        int[][] positions = JeiNeiLayout.getGenericInputSlotPositions(1);
        addItemSlot(builder, RecipeIngredientRole.INPUT, positions[0][0] + inputXOffset, positions[0][1])
                .addItemStack(recipe.rod());
    }

    @Override
    protected void addOutputSlots(IRecipeLayoutBuilder builder, RBMKDisassemblyJeiRecipe recipe, int outputXOffset) {
        int[][] positions = JeiNeiLayout.getGenericOutputSlotPositions(1);
        addItemSlot(builder, RecipeIngredientRole.OUTPUT, positions[0][0] + outputXOffset, positions[0][1])
                .addItemStack(recipe.pellets());
    }

    @Override
    protected void addBlueprintSlot(IRecipeLayoutBuilder builder, RBMKDisassemblyJeiRecipe recipe, int machineXOffset) {
        // Plain crafting-table recipe, no blueprint involved.
    }
}
