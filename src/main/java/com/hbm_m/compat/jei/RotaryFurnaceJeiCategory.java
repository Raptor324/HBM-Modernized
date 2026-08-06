package com.hbm_m.compat.jei;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.recipe.RotaryFurnaceRecipes;
import com.hbm_m.recipe.RotaryFurnaceRecipes.RotaryFurnaceRecipe;

import dev.architectury.fluid.FluidStack;
import dev.architectury.hooks.fluid.forge.FluidStackHooksForge;
import mezz.jei.api.forge.ForgeTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * JEI category for Rotary Furnace recipes - bis zu 3 Item-Eingaenge + optionales Eingangs-Fluid
 * -&gt; 1 Ausgabe (siehe {@link RotaryFurnaceRecipes}).
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
        return recipe.ingredients().length + (recipe.fluid() != null ? 1 : 0);
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

        for (var ingredient : recipe.ingredients()) {
            addItemSlot(builder, RecipeIngredientRole.INPUT,
                    positions[slotIndex][0] + inputXOffset, positions[slotIndex][1])
                    .addItemStack(new ItemStack(ingredient.item(), ingredient.count()));
            slotIndex++;
        }

        if (recipe.fluid() != null) {
            FluidStack fluid = FluidStack.create(recipe.fluid().fluid(), recipe.fluid().amountMb());
            addItemSlot(builder, RecipeIngredientRole.INPUT,
                    positions[slotIndex][0] + inputXOffset, positions[slotIndex][1])
                    .setFluidRenderer(FLUID_RENDERER_CAPACITY, false, 16, 16)
                    .setCustomRenderer(ForgeTypes.FLUID_STACK, new HbmFluidJeiRenderer(16, 16))
                    .addIngredient(ForgeTypes.FLUID_STACK, FluidStackHooksForge.toForge(fluid));
        }
    }

    @Override
    protected void addOutputSlots(IRecipeLayoutBuilder builder, RotaryFurnaceRecipe recipe, int outputXOffset) {
        addItemSlot(builder, RecipeIngredientRole.OUTPUT, outputXOffset + 22, 22)
                .addItemStack(recipe.output());
    }

    @Override
    protected void addBlueprintSlot(IRecipeLayoutBuilder builder, RotaryFurnaceRecipe recipe, int machineXOffset) {
        // Kein Blueprint-Slot fuer Rotary-Furnace-Rezepte.
    }

    @Override
    protected void drawRecipeExtras(RotaryFurnaceRecipe recipe, GuiGraphics graphics) {
        JeiNeiRendering.drawGenericRecipeExtras(graphics, recipe.duration(), 0);
    }
}
//?} else {
/*public final class RotaryFurnaceJeiCategory {
    private RotaryFurnaceJeiCategory() {}
}*///?}
