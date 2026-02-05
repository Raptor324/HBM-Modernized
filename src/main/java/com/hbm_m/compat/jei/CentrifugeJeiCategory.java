package com.hbm_m.compat.jei;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.recipe.CentrifugeRecipe;
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

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class CentrifugeJeiCategory implements IRecipeCategory<CentrifugeRecipe> {

    public static final RecipeType<CentrifugeRecipe> RECIPE_TYPE =
            RecipeType.create(RefStrings.MODID, "centrifuge", CentrifugeRecipe.class);

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/gui/processing/gui_centrifuge.png");

    private final IDrawable background;
    private final IDrawable icon;

    public CentrifugeJeiCategory(IGuiHelper guiHelper) {
        // Reuse the machine GUI texture (top part) as JEI background.
        this.background = guiHelper.createDrawable(TEXTURE, 0, 0, 176, 80);
        this.icon = guiHelper.createDrawableIngredient(VanillaTypes.ITEM_STACK, new ItemStack(ModBlocks.CENTRIFUGE.get()));
    }

    @Override
    public RecipeType<CentrifugeRecipe> getRecipeType() {
        return RECIPE_TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("container.hbm_m.centrifuge");
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
    public void setRecipe(IRecipeLayoutBuilder builder, CentrifugeRecipe recipe, mezz.jei.api.recipe.IFocusGroup focuses) {
        // Slot coordinates are aligned to the in-game GUI for consistency.
        List<ItemStack> inputs = Arrays.stream(recipe.getInput().getItems())
            .map(ItemStack::copy)
            .collect(Collectors.toList());
        builder.addSlot(RecipeIngredientRole.INPUT, 44, 35)
            .addItemStacks(inputs);

        var outs = recipe.getOutputs();
        int baseX = 65;
        int y = 54;
        int step = 20;
        for (int i = 0; i < 4 && i < outs.size(); i++) {
            ItemStack out = outs.get(i);
            if (out.isEmpty()) continue;
            builder.addSlot(RecipeIngredientRole.OUTPUT, baseX + i * step, y)
                    .addItemStack(out);
        }
    }
}
