package com.hbm_m.compat.jei;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.recipe.RotaryFurnaceRecipe;

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
 * JEI category for Rotary Furnace recipes - bis zu 3 Item-Eingaenge + optionales Eingangs-Fluid
 * -&gt; 1 Ausgabe. Arbeitet direkt mit dem data-driven {@link RotaryFurnaceRecipe} (JSON) -
 * Eingaenge aus {@link RotaryFurnaceRecipe#getInputs()}, Ausgabe aus {@link RotaryFurnaceRecipe#getOutput()}.
 */
//? if forge {
public class RotaryFurnaceJeiCategory extends JeiGenericRecipeCategory<RotaryFurnaceRecipe> {

    public static final RecipeType<RotaryFurnaceRecipe> RECIPE_TYPE =
            RecipeType.create(RefStrings.MODID, "rotary_furnace", RotaryFurnaceRecipe.class);

    private static final int FLUID_RENDERER_CAPACITY = 4_000;

    public RotaryFurnaceJeiCategory(IGuiHelper guiHelper) {
        super(guiHelper, new ItemStack[]{
                new ItemStack(ModBlocks.ROTARY_FURNACE.get())
        });
    }

    @Override
    public RecipeType<RotaryFurnaceRecipe> getRecipeType() {
        return RECIPE_TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("container.hbm_m.rotary_furnace");
    }

    @Override
    protected int getInputCount(RotaryFurnaceRecipe recipe) {
        return recipe.getInputs().length + (recipe.getFluid() != null ? 1 : 0);
    }

    @Override
    protected int getOutputCount(RotaryFurnaceRecipe recipe) {
        return 1;
    }

    @Override
    protected boolean hasBlueprintTemplate(RotaryFurnaceRecipe recipe) {
        return false;
    }

    @Override
    protected void addInputSlots(IRecipeLayoutBuilder builder, RotaryFurnaceRecipe recipe, int inputXOffset) {
        int inputCount = getInputCount(recipe);
        int[][] positions = JeiNeiLayout.getGenericInputSlotPositions(inputCount);
        int slotIndex = 0;

        for (var ingredient : recipe.getInputs()) {
            addItemSlot(builder, RecipeIngredientRole.INPUT,
                    positions[slotIndex][0] + inputXOffset, positions[slotIndex][1])
                    .addIngredients(ingredient);
            slotIndex++;
        }

        if (recipe.getFluid() != null) {
            FluidStack fluid = FluidStack.create(recipe.getFluid().getFluid(), recipe.getFluidAmountMb());
            addItemSlot(builder, RecipeIngredientRole.INPUT,
                    positions[slotIndex][0] + inputXOffset, positions[slotIndex][1])
                    .setFluidRenderer(FLUID_RENDERER_CAPACITY, false, 16, 16)
                    //? if forge {
                    .setCustomRenderer(ForgeTypes.FLUID_STACK, new HbmFluidJeiRenderer(16, 16))
                    .addIngredient(ForgeTypes.FLUID_STACK, FluidStackHooksForge.toForge(fluid));
                    //?} elif neoforge {
                    /*.setCustomRenderer(NeoForgeTypes.FLUID_STACK, new HbmFluidJeiRenderer(16, 16))
                    .addIngredient(NeoForgeTypes.FLUID_STACK, new net.neoforged.neoforge.fluids.FluidStack(fluid.getFluid(), (int) fluid.getAmount()));
                    *///?}
        }
    }

    @Override
    protected void addOutputSlots(IRecipeLayoutBuilder builder, RotaryFurnaceRecipe recipe, int outputXOffset) {
        addItemSlot(builder, RecipeIngredientRole.OUTPUT, outputXOffset + 22, 22)
                .addItemStack(recipe.getOutput());
    }

    @Override
    protected void addBlueprintSlot(IRecipeLayoutBuilder builder, RotaryFurnaceRecipe recipe, int machineXOffset) {
        // Kein Blueprint-Slot fuer Rotary-Furnace-Rezepte.
    }

    @Override
    protected void drawRecipeExtras(RotaryFurnaceRecipe recipe, GuiGraphics graphics) {
        JeiNeiRendering.drawGenericRecipeExtras(graphics, recipe.getDuration(), 0);
    }
}
//?} else {
/*public final class RotaryFurnaceJeiCategory {
    private RotaryFurnaceJeiCategory() {}
}*///?}
