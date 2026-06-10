package com.hbm_m.compat.jei;

import com.hbm_m.item.ModItems;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.recipe.AssemblerRecipe;

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

public class AssemblerJeiCategory implements IRecipeCategory<AssemblerRecipe> {

    public static final RecipeType<AssemblerRecipe> RECIPE_TYPE =
            RecipeType.create(RefStrings.MODID, "assembler", AssemblerRecipe.class);

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/jei_gui/gui_nei_assembler.png");

    private final IDrawable background;
    private final IDrawable icon;

    public AssemblerJeiCategory(IGuiHelper guiHelper) {
        this.background = guiHelper.createDrawable(TEXTURE, 0, 0, 176, 90);
        this.icon = guiHelper.createDrawableIngredient(VanillaTypes.ITEM_STACK, new ItemStack(ModItems.ADVANCED_ASSEMBLY_MACHINE.get()));
    }

    @Override
    public RecipeType<AssemblerRecipe> getRecipeType() {
        return RECIPE_TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("container.hbm_m.advanced_assembly_machine");
    }

    @Override
    public IDrawable getBackground() {
        return background;
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }

    // Slot positions matching gui_nei_assembler.png: a 1x3 column on the left
    // followed by a 4x3 grid of input slots.
    private static final int[][] INPUT_SLOTS = {
            {7, 16}, {7, 34}, {7, 52},
            {34, 16}, {52, 16}, {70, 16}, {88, 16},
            {34, 34}, {52, 34}, {70, 34}, {88, 34},
            {34, 52}, {52, 52}, {70, 52}, {88, 52}
    };

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, AssemblerRecipe recipe, mezz.jei.api.recipe.IFocusGroup focuses) {
        int slot = 0;
        for (var ingredient : recipe.getIngredients()) {
            if (slot >= INPUT_SLOTS.length) {
                break;
            }
            int[] pos = INPUT_SLOTS[slot];
            builder.addSlot(RecipeIngredientRole.INPUT, pos[0], pos[1]).addIngredients(ingredient);
            slot++;
        }

        builder.addSlot(RecipeIngredientRole.OUTPUT, 142, 34)
                .addItemStack(recipe.getResultItem(null));
    }
}