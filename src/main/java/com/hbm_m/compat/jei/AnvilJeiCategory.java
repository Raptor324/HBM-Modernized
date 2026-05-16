package com.hbm_m.compat.jei;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.block.machines.anvils.AnvilTier;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.recipe.AnvilRecipe;
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

import java.util.List;

public class AnvilJeiCategory implements IRecipeCategory<AnvilRecipe> {

    public static final RecipeType<AnvilRecipe> RECIPE_TYPE =
            RecipeType.create(RefStrings.MODID, "anvil_steel", AnvilRecipe.class);

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/gui/processing/gui_anvil.png");

    private final IDrawable background;
    private final IDrawable icon;

    public AnvilJeiCategory(IGuiHelper guiHelper) {
        this.background = guiHelper.createDrawable(TEXTURE, 0, 0, 118, 86);
        this.icon = guiHelper.createDrawableIngredient(VanillaTypes.ITEM_STACK, new ItemStack(ModBlocks.ANVIL_STEEL.get()));
    }

    @Override
    public RecipeType<AnvilRecipe> getRecipeType() {
        return RECIPE_TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("container.hbm_m.anvil", AnvilTier.STEEL.getDisplayName());
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
    public void setRecipe(IRecipeLayoutBuilder builder, AnvilRecipe recipe, mezz.jei.api.recipe.IFocusGroup focuses) {
        if (!recipe.getInputA().isEmpty()) {
            builder.addSlot(RecipeIngredientRole.INPUT, 17, 27)
                    .addItemStack(recipe.getInputA());
        }

        if (!recipe.getInputB().isEmpty()) {
            builder.addSlot(RecipeIngredientRole.INPUT, 53, 27)
                    .addItemStack(recipe.getInputB());
        }

        List<ItemStack> requiredItems = recipe.getInventoryInputs();
        for (int index = 0; index < requiredItems.size(); index++) {
            ItemStack required = requiredItems.get(index);
            if (required.isEmpty()) {
                continue;
            }

            builder.addSlot(RecipeIngredientRole.INPUT, 8 + (index % 5) * 18, 50 + (index / 5) * 18)
                    .addItemStack(required);
        }

        for (int index = 0; index < recipe.getOutputs().size(); index++) {
            ItemStack output = recipe.getOutputs().get(index).stack();
            if (output.isEmpty()) {
                continue;
            }

            builder.addSlot(RecipeIngredientRole.OUTPUT, 89 + (index % 2) * 18, 27 + (index / 2) * 18)
                    .addItemStack(output);
        }
    }
}
