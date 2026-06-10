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
            /*new ResourceLocation(RefStrings.MODID, "textures/jei_gui/gui_nei_chemplant.png");
            *///?} else {
                        ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/jei_gui/gui_nei_chemplant.png");
            //?}


    private final IDrawable background;
    private final IDrawable icon;

    public ChemicalPlantJeiCategory(IGuiHelper guiHelper) {
        // Dedicated JEI background: fluid tank on the left, item inputs (2x3) and item outputs (2x3) on either
        // side of the processing arrow.
        this.background = guiHelper.createDrawable(TEXTURE, 0, 0, 176, 84);
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
        // Layout of gui_nei_chemplant.png:
        // - Fluid input tank on the left at (8, 17), 16 wide x 52 tall
        // - Item inputs in a 2x3 grid at x = 35/53, y = 17/35/53
        // - Item outputs in a 2x3 grid at x = 125/143, y = 17/35/53

        // Add fluid input
        List<FluidStack> fluidInputs = recipe.getFluidInputs();
        if (!fluidInputs.isEmpty() && !fluidInputs.get(0).isEmpty()) {
            builder.addSlot(RecipeIngredientRole.INPUT, 8, 17)
                    .setFluidRenderer(24000, false, 16, 52)
                    .addIngredient(ForgeTypes.FLUID_STACK, FluidStackHooksForge.toForge(fluidInputs.get(0)));
        }

        // Add item inputs
        List<List<ItemStack>> itemInputs = recipe.getItemInputStacks();
        for (int i = 0; i < 6 && i < itemInputs.size(); i++) {
            List<ItemStack> inputVariants = itemInputs.get(i);
            if (!inputVariants.isEmpty() && !inputVariants.get(0).isEmpty()) {
                int x = 35 + (i % 2) * 18;
                int y = 17 + (i / 2) * 18;
                builder.addSlot(RecipeIngredientRole.INPUT, x, y)
                        .addItemStacks(inputVariants);
            }
        }

        // Add item outputs
        List<ItemStack> itemOutputs = recipe.getItemOutputs();
        for (int i = 0; i < 6 && i < itemOutputs.size(); i++) {
            ItemStack output = itemOutputs.get(i);
            if (!output.isEmpty()) {
                int x = 125 + (i % 2) * 18;
                int y = 17 + (i / 2) * 18;
                builder.addSlot(RecipeIngredientRole.OUTPUT, x, y)
                        .addItemStack(output);
            }
        }
    }
}
//?} else {
/*public final class ChemicalPlantJeiCategory {
    private ChemicalPlantJeiCategory() {}
}*///?}
