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
            ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/gui/processing/gui_assembler.png");

    private final IDrawable background;
    private final IDrawable icon;

    public AssemblerJeiCategory(IGuiHelper guiHelper) {
        this.background = guiHelper.createDrawable(TEXTURE, 0, 0, 176, 144);
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

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, AssemblerRecipe recipe, mezz.jei.api.recipe.IFocusGroup focuses) {
        int slot = 0;

        // 4x3 input grid aligned with the machine GUI.
        for (var ingredient : recipe.getIngredients()) {
            int x = 8 + (slot % 3) * 18;
            int y = 18 + (slot / 3) * 18;
            builder.addSlot(RecipeIngredientRole.INPUT, x, y).addIngredients(ingredient);
            slot++;
            if (slot >= 12) {
                break;
            }
        }

        builder.addSlot(RecipeIngredientRole.OUTPUT, 98, 45)
                .addItemStack(recipe.getResultItem(null));
    }
}