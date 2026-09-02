package com.hbm_m.compat.jei;

import java.util.List;

import com.hbm_m.item.ModItems;
import com.hbm_m.item.industrial.ItemBlueprintFolder;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.recipe.AssemblerRecipe;
import com.hbm_m.recipe.AssemblerRecipe.AssemblerInputSlot;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * JEI port of {@code AssemblyMachineRecipeHandler} (extends {@code NEIGenericRecipeHandler}).
 */
public class AssemblerJeiCategory extends JeiGenericRecipeCategory<AssemblerRecipe> {

    public static final RecipeType<AssemblerRecipe> RECIPE_TYPE =
            RecipeType.create(RefStrings.MODID, "assembler", AssemblerRecipe.class);

    public AssemblerJeiCategory(IGuiHelper guiHelper) {
        super(guiHelper, new ItemStack[]{
                new ItemStack(ModItems.ADVANCED_ASSEMBLY_MACHINE.get())
        });
    }

    @Override
    public RecipeType<AssemblerRecipe> getRecipeType() {
        return RECIPE_TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("container.hbm_m.advanced_assembly_machine");
    }

    private static List<AssemblerInputSlot> getInputSlots(AssemblerRecipe recipe) {
        List<AssemblerInputSlot> slots = recipe.getInputDisplaySlots();
        if (!slots.isEmpty()) {
            return slots;
        }
        return AssemblerJeiInputs.fallbackFromExpanded(recipe.getIngredients());
    }

    @Override
    protected int getInputCount(AssemblerRecipe recipe) {
        return getInputSlots(recipe).size();
    }

    @Override
    protected int getOutputCount(AssemblerRecipe recipe) {
        return 1;
    }

    @Override
    protected boolean hasBlueprintTemplate(AssemblerRecipe recipe) {
        return recipe.requiresBlueprint();
    }

    @Override
    protected int getInputXOffset(AssemblerRecipe recipe, int inputCount) {
        if (inputCount > 12) return -9;
        if (inputCount > 9) return 18;
        return 0;
    }

    @Override
    protected int getOutputXOffset(AssemblerRecipe recipe, int outputCount) {
        return getOffset(recipe);
    }

    @Override
    protected int getMachineXOffset(AssemblerRecipe recipe) {
        return getOffset(recipe);
    }

    private static int getOffset(AssemblerRecipe recipe) {
        int length = getInputSlots(recipe).size();
        if (length > 12) return 27;
        if (length > 9) return 18;
        return 0;
    }

    @Override
    protected void addInputSlots(IRecipeLayoutBuilder builder, AssemblerRecipe recipe, int inputXOffset) {
        List<AssemblerInputSlot> inputs = getInputSlots(recipe);
        int[][] positions = JeiNeiLayout.getGenericInputSlotPositions(inputs.size());

        for (int i = 0; i < inputs.size() && i < positions.length; i++) {
            AssemblerInputSlot slot = inputs.get(i);
            IRecipeSlotBuilder jeiSlot = addItemSlot(builder, RecipeIngredientRole.INPUT,
                    positions[i][0] + inputXOffset, positions[i][1]);
            JeiIngredientSlots.addCountedIngredient(jeiSlot, slot.ingredient(), slot.count());
        }
    }

    @Override
    protected void addOutputSlots(IRecipeLayoutBuilder builder, AssemblerRecipe recipe, int outputXOffset) {
        int[][] positions = JeiNeiLayout.getGenericOutputSlotPositions(1);
        addItemSlot(builder, RecipeIngredientRole.OUTPUT, positions[0][0] + outputXOffset, positions[0][1])
                .addItemStack(recipe.getResultItemSafe());
    }

    @Override
    protected void addBlueprintSlot(IRecipeLayoutBuilder builder, AssemblerRecipe recipe, int machineXOffset) {
        if (!recipe.requiresBlueprint()) {
            return;
        }

        ItemStack folder = new ItemStack(ModItems.BLUEPRINT_FOLDER.get());
        ItemBlueprintFolder.writeBlueprintPool(folder, recipe.getBlueprintPool());
        addUnframedSlot(builder, RecipeIngredientRole.RENDER_ONLY, 75 + machineXOffset, 10)
                .addItemStack(folder);
    }

    @Override
    protected void drawRecipeExtras(AssemblerRecipe recipe, GuiGraphics graphics) {
        JeiNeiRendering.drawGenericRecipeExtras(graphics, recipe.getDuration(), recipe.getPowerConsumption());
    }
}
