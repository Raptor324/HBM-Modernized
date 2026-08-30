package com.hbm_m.compat.jei;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.recipe.ArcWelderRecipe;
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
import net.minecraft.world.item.crafting.Ingredient;

/**
 * JEI-категория дуговой сварки ({@code hbm_m:arc_welder}).
 *
 * <p>Работает напрямую с data-driven {@link ArcWelderRecipe} (JSON) — без промежуточной
 * {@code *JeiRecipe}-обёртки. Входы берутся из {@link ArcWelderRecipe#getInputs()}, выход —
 * из {@link ArcWelderRecipe#getOutput()}.</p>
 */
public class ArcWelderJeiCategory implements IRecipeCategory<ArcWelderRecipe> {

    public static final RecipeType<ArcWelderRecipe> RECIPE_TYPE =
            RecipeType.create(RefStrings.MODID, "arc_welder", ArcWelderRecipe.class);

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/gui/processing/gui_arc_welder.png");

    private final IDrawable background;
    private final IDrawable icon;

    public ArcWelderJeiCategory(IGuiHelper guiHelper) {
        this.background = guiHelper.createDrawable(TEXTURE, 0, 0, 176, 100);
        this.icon = guiHelper.createDrawableIngredient(VanillaTypes.ITEM_STACK, new ItemStack(ModBlocks.ARC_WELDER.get()));
    }

    @Override public RecipeType<ArcWelderRecipe> getRecipeType() { return RECIPE_TYPE; }
    @Override public Component getTitle() { return Component.translatable("block.hbm_m.arc_welder"); }
    @Override @SuppressWarnings("removal") public IDrawable getBackground() { return background; }
    @Override public IDrawable getIcon()       { return icon; }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, ArcWelderRecipe recipe, mezz.jei.api.recipe.IFocusGroup focuses) {
        // До 3 входов на y=36, x позициями 17/35/53 — как в оригинальной GUI.
        Ingredient[] inputs = recipe.getInputs();
        int[] xs = {17, 35, 53};
        for (int i = 0; i < Math.min(3, inputs.length); i++) {
            builder.addSlot(RecipeIngredientRole.INPUT, xs[i], 36)
                    .addIngredients(inputs[i]);
        }
        ItemStack output = recipe.getOutput();
        if (!output.isEmpty()) {
            builder.addSlot(RecipeIngredientRole.OUTPUT, 107, 36)
                    .addItemStack(output);
        }
    }
}
//?} else {
/*public final class ArcWelderJeiCategory { private ArcWelderJeiCategory() {} }
*///?}
