package com.hbm_m.compat.jei;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.lib.RefStrings;

import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

//? if forge {
public class CyclotronJeiCategory implements IRecipeCategory<CyclotronJeiRecipe> {

    public static final RecipeType<CyclotronJeiRecipe> RECIPE_TYPE =
            RecipeType.create(RefStrings.MODID, "cyclotron", CyclotronJeiRecipe.class);

    private static final ResourceLocation TEXTURE =
            //? if fabric && < 1.21.1 {
            /*new ResourceLocation(RefStrings.MODID, "textures/jei_gui/gui_jei_cyclotron.png");
            *///?} else {
                        ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/jei_gui/gui_jei_cyclotron.png");
            //?}


    private final IDrawable background;
    private final IDrawable icon;

    public CyclotronJeiCategory(IGuiHelper guiHelper) {
        this.background = guiHelper.createDrawable(TEXTURE, 0, 0, 176, 82);
        this.icon = guiHelper.createDrawableIngredient(VanillaTypes.ITEM_STACK, new ItemStack(ModBlocks.CYCLOTRON.get()));
    }

    @Override
    public RecipeType<CyclotronJeiRecipe> getRecipeType() {
        return RECIPE_TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("block.hbm_m.cyclotron");
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
    public void setRecipe(IRecipeLayoutBuilder builder, CyclotronJeiRecipe recipe, mezz.jei.api.recipe.IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.INPUT, 17, 34)
                .addItemStacks(recipe.getTargetStacks());

        builder.addSlot(RecipeIngredientRole.INPUT, 71, 34)
                .addItemStacks(recipe.getInputStacks());

        builder.addSlot(RecipeIngredientRole.OUTPUT, 127, 34)
                .addItemStack(recipe.getOutput());
    }
}
//?} else {
/*public final class CyclotronJeiCategory {
    private CyclotronJeiCategory() {}
}*///?}
