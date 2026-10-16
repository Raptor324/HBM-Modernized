package com.hbm_m.compat.jei;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.recipe.ArcFurnaceRecipe;

import dev.architectury.fluid.FluidStack;
//? if forge {
//? if forge {
import mezz.jei.api.forge.ForgeTypes;
//?} elif neoforge {
/*import mezz.jei.api.neoforge.NeoForgeTypes;
*///?}
//? if forge {
import dev.architectury.hooks.fluid.forge.FluidStackHooksForge;
//?}
//?} elif neoforge {
/*import mezz.jei.api.neoforge.NeoForgeTypes;
*///?}
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * JEI category for Arc Furnace recipes - 1 Item-Eingang -&gt; optionaler Item-Ausgang + bis zu 2
 * Fluid-Ausgaenge (siehe {@link ArcFurnaceRecipe}).
 */
//? if forge {
public class ArcFurnaceJeiCategory extends JeiGenericRecipeCategory<ArcFurnaceRecipe> {

    public static final RecipeType<ArcFurnaceRecipe> RECIPE_TYPE =
            RecipeType.create(RefStrings.MODID, "arc_furnace", ArcFurnaceRecipe.class);

    private static final int FLUID_RENDERER_CAPACITY = 4_000;

    public ArcFurnaceJeiCategory(IGuiHelper guiHelper) {
        super(guiHelper, new ItemStack[]{
                new ItemStack(ModBlocks.ARC_FURNACE.get())
        });
    }

    @Override
    public RecipeType<ArcFurnaceRecipe> getRecipeType() {
        return RECIPE_TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("container.hbm_m.arc_furnace");
    }

    @Override
    protected int getInputCount(ArcFurnaceRecipe recipe) {
        return 1;
    }

    @Override
    protected int getOutputCount(ArcFurnaceRecipe recipe) {
        int count = recipe.hasItemOutput() ? 1 : 0;
        if (recipe.hasFluidOutput1()) count++;
        if (recipe.hasFluidOutput2()) count++;
        return count;
    }

    @Override
    protected boolean hasBlueprintTemplate(ArcFurnaceRecipe recipe) {
        return false;
    }

    @Override
    protected void addInputSlots(IRecipeLayoutBuilder builder, ArcFurnaceRecipe recipe, int inputXOffset) {
        var slot = addItemSlot(builder, RecipeIngredientRole.INPUT, inputXOffset, 22);
        JeiIngredientSlots.addCountedIngredient(slot, recipe.getInput(), recipe.getInputCount());
    }

    @Override
    protected void addOutputSlots(IRecipeLayoutBuilder builder, ArcFurnaceRecipe recipe, int outputXOffset) {
        int[][] positions = JeiNeiLayout.getGenericOutputSlotPositions(getOutputCount(recipe));
        int slotIndex = 0;

        if (recipe.hasItemOutput()) {
            addItemSlot(builder, RecipeIngredientRole.OUTPUT,
                    positions[slotIndex][0] + outputXOffset, positions[slotIndex][1])
                    .addItemStack(recipe.getOutput());
            slotIndex++;
        }
        if (recipe.hasFluidOutput1()) {
            addFluidSlot(builder, positions[slotIndex][0] + outputXOffset, positions[slotIndex][1],
                    FluidStack.create(recipe.getFluid1(), recipe.getFluidAmount1()));
            slotIndex++;
        }
        if (recipe.hasFluidOutput2()) {
            addFluidSlot(builder, positions[slotIndex][0] + outputXOffset, positions[slotIndex][1],
                    FluidStack.create(recipe.getFluid2(), recipe.getFluidAmount2()));
            slotIndex++;
        }
    }

    private void addFluidSlot(IRecipeLayoutBuilder builder, int x, int y, FluidStack fluid) {
        addItemSlot(builder, RecipeIngredientRole.OUTPUT, x, y)
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
    protected void addBlueprintSlot(IRecipeLayoutBuilder builder, ArcFurnaceRecipe recipe, int machineXOffset) {
        // Kein Blueprint-Slot fuer Arc-Furnace-Rezepte.
    }

    @Override
    protected void drawRecipeExtras(ArcFurnaceRecipe recipe, GuiGraphics graphics) {
        JeiNeiRendering.drawGenericRecipeExtras(graphics, recipe.getDuration(), 0);
    }
}
//?} else {
/*public final class ArcFurnaceJeiCategory {
    private ArcFurnaceJeiCategory() {}
}*///?}
