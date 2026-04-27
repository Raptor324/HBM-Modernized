package com.hbm_m.compat.jei;

import java.util.List;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.lib.RefStrings;

import dev.architectury.fluid.FluidStack;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/**
 * JEI category for Chemical Plant recipes.
 */
//? if forge {
import dev.architectury.hooks.fluid.forge.FluidStackHooksForge;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.forge.ForgeTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;

public class ChemicalPlantJeiCategory implements IRecipeCategory<ChemicalPlantJeiRecipe> {

    public static final RecipeType<ChemicalPlantJeiRecipe> RECIPE_TYPE =
            RecipeType.create(RefStrings.MODID, "chemical_plant", ChemicalPlantJeiRecipe.class);

    private static final ResourceLocation TEXTURE =
            //? if fabric && < 1.21.1 {
            /*new ResourceLocation(RefStrings.MODID, "textures/gui/processing/gui_chemplant.png");
            *///?} else {
                        ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/gui/processing/gui_chemplant.png");
            //?}


    private final IDrawable background;
    private final IDrawable icon;

    public ChemicalPlantJeiCategory(IGuiHelper guiHelper) {
        // Use a portion of the machine GUI texture as JEI background
        // Showing the input/output area (slots 4-9 and tanks)
        this.background = guiHelper.createDrawable(TEXTURE, 0, 45, 140, 85);
        this.icon = guiHelper.createDrawableIngredient(VanillaTypes.ITEM_STACK, new ItemStack(ModBlocks.CHEMICAL_PLANT.get()));
    }

    @Override
    public RecipeType<ChemicalPlantJeiRecipe> getRecipeType() {
        return RECIPE_TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("container.hbm_m.chemical_plant");
    }

    @Override
    public IDrawable getBackground() {
        return background;
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, ChemicalPlantJeiRecipe recipe, mezz.jei.api.recipe.IFocusGroup focuses) {
        // GUI background is extracted from y=45, so we need to adjust positions
        // Original positions in GUI:
        // - Item inputs: (8, 99), (26, 99), (44, 99) -> adjusted: (8, 54), (26, 54), (44, 54)
        // - Item outputs: (80, 99), (98, 99), (116, 99) -> adjusted: (80, 54), (98, 54), (116, 54)
        // - Fluid input tanks: (8, 52), (26, 52), (44, 52) -> adjusted: (8, 7), (26, 7), (44, 7)
        // - Fluid output tanks: (80, 52), (98, 52), (116, 52) -> adjusted: (80, 7), (98, 7), (116, 7)

        // Add item inputs
        List<List<ItemStack>> itemInputs = recipe.getItemInputStacks();
        for (int i = 0; i < 3 && i < itemInputs.size(); i++) {
            List<ItemStack> inputVariants = itemInputs.get(i);
            if (!inputVariants.isEmpty() && !inputVariants.get(0).isEmpty()) {
                builder.addSlot(RecipeIngredientRole.INPUT, 8 + i * 18, 54)
                        .addItemStacks(inputVariants);
            }
        }

        // Add fluid inputs
        List<FluidStack> fluidInputs = recipe.getFluidInputs();
        for (int i = 0; i < 3 && i < fluidInputs.size(); i++) {
            FluidStack fluid = fluidInputs.get(i);
            if (!fluid.isEmpty()) {
                builder.addSlot(RecipeIngredientRole.INPUT, 8 + i * 18, 7)
                        .setFluidRenderer(24000, false, 16, 34)
                        .addIngredient(ForgeTypes.FLUID_STACK, FluidStackHooksForge.toForge(fluid));
            }
        }

        // Add item outputs
        List<ItemStack> itemOutputs = recipe.getItemOutputs();
        for (int i = 0; i < 3 && i < itemOutputs.size(); i++) {
            ItemStack output = itemOutputs.get(i);
            if (!output.isEmpty()) {
                builder.addSlot(RecipeIngredientRole.OUTPUT, 80 + i * 18, 54)
                        .addItemStack(output);
            }
        }

        // Add fluid outputs
        List<FluidStack> fluidOutputs = recipe.getFluidOutputs();
        for (int i = 0; i < 3 && i < fluidOutputs.size(); i++) {
            FluidStack fluid = fluidOutputs.get(i);
            if (!fluid.isEmpty()) {
                builder.addSlot(RecipeIngredientRole.OUTPUT, 80 + i * 18, 7)
                        .setFluidRenderer(24000, false, 16, 34)
                        .addIngredient(ForgeTypes.FLUID_STACK, FluidStackHooksForge.toForge(fluid));
            }
        }
    }
}
//?} else {
/*public final class ChemicalPlantJeiCategory {
    private ChemicalPlantJeiCategory() {}
}*///?}
