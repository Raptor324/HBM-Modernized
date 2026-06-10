package com.hbm_m.compat.jei;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.lib.RefStrings;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

//? if forge {
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;

public class CrystallizerJeiCategory implements IRecipeCategory<CrystallizerJeiRecipe> {

    public static final RecipeType<CrystallizerJeiRecipe> RECIPE_TYPE =
            RecipeType.create(RefStrings.MODID, "crystallizer", CrystallizerJeiRecipe.class);

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/gui/processing/gui_crystallizer.png");

    private final IDrawable background;
    private final IDrawable icon;

    public CrystallizerJeiCategory(IGuiHelper guiHelper) {
        // Top 100 px of the crystallizer GUI captures the machine area with all slots.
        this.background = guiHelper.createDrawable(TEXTURE, 0, 0, 176, 100);
        this.icon = guiHelper.createDrawableIngredient(VanillaTypes.ITEM_STACK,
                new ItemStack(ModBlocks.CRYSTALLIZER.get()));
    }

    @Override public RecipeType<CrystallizerJeiRecipe> getRecipeType() { return RECIPE_TYPE; }
    @Override public Component getTitle() { return Component.translatable("container.hbm_m.crystallizer"); }
    @Override public IDrawable getBackground() { return background; }
    @Override public IDrawable getIcon()       { return icon; }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, CrystallizerJeiRecipe recipe,
                          mezz.jei.api.recipe.IFocusGroup focuses) {
        // Input — slot 0 at (62, 45) in the GUI
        if (!recipe.getInputs().isEmpty())
            builder.addSlot(RecipeIngredientRole.INPUT, 62, 45)
                    .addItemStacks(recipe.getInputs());

        // Output — slot 2 at (113, 45)
        builder.addSlot(RecipeIngredientRole.OUTPUT, 113, 45)
                .addItemStack(recipe.getOutput());

        // Fluid input — slot 3 at (17, 18), show acid requirement
        if (!recipe.getAcid().isEmpty()) {
            builder.addSlot(RecipeIngredientRole.INPUT, 17, 18)
                    .addFluidStack(recipe.getAcid().getFluid(), recipe.getAcid().getAmount());
        }
    }
}
//?} else {
/*public final class CrystallizerJeiCategory { private CrystallizerJeiCategory() {} }
*///?}
