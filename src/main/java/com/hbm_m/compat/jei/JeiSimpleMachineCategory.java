package com.hbm_m.compat.jei;

//? if forge || neoforge {
import java.util.List;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

//? if forge {
@net.minecraftforge.api.distmarker.OnlyIn(net.minecraftforge.api.distmarker.Dist.CLIENT)
//?} elif neoforge {
/*@net.neoforged.api.distmarker.OnlyIn(net.neoforged.api.distmarker.Dist.CLIENT)
*///?}
/**
 * Категория «предметы и жидкости на вход, предметы и жидкости на выход» на общем NEI-фоне.
 * Конкретная машина задаётся типом рецепта, заголовком, иконкой и {@link JeiMachineView}.
 */
public final class JeiSimpleMachineCategory<R> extends JeiGenericRecipeCategory<R> {

    private final RecipeType<R> recipeType;
    private final String titleKey;
    private final JeiMachineView<R> view;
    private final int fluidCapacityMb;
    private int noteLines;

    public JeiSimpleMachineCategory(IGuiHelper guiHelper, RecipeType<R> recipeType, String titleKey,
                                    ItemStack machine, JeiMachineView<R> view) {
        this(guiHelper, recipeType, titleKey, new ItemStack[]{machine}, view, JeiFluidSlots.DEFAULT_CAPACITY_MB);
    }

    public JeiSimpleMachineCategory(IGuiHelper guiHelper, RecipeType<R> recipeType, String titleKey,
                                    ItemStack[] machines, JeiMachineView<R> view, int fluidCapacityMb) {
        super(guiHelper, machines);
        this.recipeType = recipeType;
        this.titleKey = titleKey;
        this.view = view;
        this.fluidCapacityMb = fluidCapacityMb;
    }

    /** Строки-примечания рисуются под фоном, поэтому категории нужна дополнительная высота. */
    public JeiSimpleMachineCategory<R> withNotes(int lines) { this.noteLines = lines; return this; }

    @Override public int getHeight() { return super.getHeight() + noteLines * 10; }

    @Override public RecipeType<R> getRecipeType() { return recipeType; }

    @Override public Component getTitle() { return Component.translatable(titleKey); }

    @Override
    protected int getInputCount(R recipe) {
        return view.itemInputs(recipe).size() + view.fluidInputs(recipe).size();
    }

    @Override
    protected int getOutputCount(R recipe) {
        return view.itemOutputs(recipe).size() + view.fluidOutputs(recipe).size();
    }

    @Override protected boolean hasBlueprintTemplate(R recipe) { return false; }

    @Override protected void addBlueprintSlot(IRecipeLayoutBuilder builder, R recipe, int machineXOffset) { }

    @Override
    protected void addInputSlots(IRecipeLayoutBuilder builder, R recipe, int inputXOffset) {
        int[][] positions = JeiNeiLayout.getGenericInputSlotPositions(getInputCount(recipe));
        int slot = 0;
        for (JeiMachineView.ItemInput input : view.itemInputs(recipe)) {
            if (slot >= positions.length) break;
            JeiIngredientSlots.addCountedIngredient(
                    addItemSlot(builder, RecipeIngredientRole.INPUT,
                            positions[slot][0] + inputXOffset, positions[slot][1]),
                    input.ingredient(), input.count());
            slot++;
        }
        for (JeiMachineView.FluidAmount fluid : view.fluidInputs(recipe)) {
            if (slot >= positions.length) break;
            JeiFluidSlots.addFluid(
                    addItemSlot(builder, RecipeIngredientRole.INPUT,
                            positions[slot][0] + inputXOffset, positions[slot][1]),
                    fluid.fluid(), fluid.mb(), fluidCapacityMb);
            slot++;
        }
    }

    @Override
    protected void addOutputSlots(IRecipeLayoutBuilder builder, R recipe, int outputXOffset) {
        int[][] positions = JeiNeiLayout.getGenericOutputSlotPositions(getOutputCount(recipe));
        int slot = 0;
        for (ItemStack output : view.itemOutputs(recipe)) {
            if (slot >= positions.length) break;
            addItemSlot(builder, RecipeIngredientRole.OUTPUT,
                    positions[slot][0] + outputXOffset, positions[slot][1])
                    .addItemStack(output);
            slot++;
        }
        for (JeiMachineView.FluidAmount fluid : view.fluidOutputs(recipe)) {
            if (slot >= positions.length) break;
            JeiFluidSlots.addFluid(
                    addItemSlot(builder, RecipeIngredientRole.OUTPUT,
                            positions[slot][0] + outputXOffset, positions[slot][1]),
                    fluid.fluid(), fluid.mb(), fluidCapacityMb);
            slot++;
        }
    }

    @Override
    protected void drawRecipeExtras(R recipe, GuiGraphics graphics) {
        if (noteLines <= 0) return;
        List<Component> notes = view.notes(recipe);
        int y = com.hbm_m.compat.jei.JeiNeiTextures.RECIPE_HEIGHT + 1;
        for (Component note : notes) {
            graphics.drawString(Minecraft.getInstance().font, note, 2, y, 0x404040, false);
            y += 9;
        }
    }
}
//?}
