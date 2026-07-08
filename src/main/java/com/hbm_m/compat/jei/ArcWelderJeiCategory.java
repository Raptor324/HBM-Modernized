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

public class ArcWelderJeiCategory implements IRecipeCategory<ArcWelderJeiRecipe> {

    public static final RecipeType<ArcWelderJeiRecipe> RECIPE_TYPE =
            RecipeType.create(RefStrings.MODID, "arc_welder", ArcWelderJeiRecipe.class);

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/gui/processing/gui_arc_welder.png");

    private final IDrawable background;
    private final IDrawable icon;

    public ArcWelderJeiCategory(IGuiHelper guiHelper) {
        this.background = guiHelper.createDrawable(TEXTURE, 0, 0, 176, 100);
        this.icon = guiHelper.createDrawableIngredient(VanillaTypes.ITEM_STACK, new ItemStack(ModBlocks.ARC_WELDER.get()));
    }

    @Override public RecipeType<ArcWelderJeiRecipe> getRecipeType() { return RECIPE_TYPE; }
    @Override public Component getTitle() { return Component.translatable("block.hbm_m.arc_welder"); }
    @Override @SuppressWarnings("removal") public IDrawable getBackground() { return background; }
    @Override public IDrawable getIcon()       { return icon; }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, ArcWelderJeiRecipe recipe, mezz.jei.api.recipe.IFocusGroup focuses) {
        var inputs = recipe.getInputs();
        int[] xs = {17, 35, 53};
        for (int i = 0; i < Math.min(3, inputs.size()); i++) {
            var stacks = inputs.get(i).stream().filter(s -> !s.isEmpty()).toList();
            if (!stacks.isEmpty())
                builder.addSlot(RecipeIngredientRole.INPUT, xs[i], 36)
                        .addItemStacks(stacks);
        }
        builder.addSlot(RecipeIngredientRole.OUTPUT, 107, 36)
                .addItemStack(recipe.getOutput());
    }
}
//?} else {
/*public final class ArcWelderJeiCategory { private ArcWelderJeiCategory() {} }
*///?}
