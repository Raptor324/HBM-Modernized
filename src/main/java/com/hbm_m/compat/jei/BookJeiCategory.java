package com.hbm_m.compat.jei;
//? if forge || neoforge {

import java.util.Arrays;

import com.hbm_m.inventory.recipes.MagicRecipes;
import com.hbm_m.inventory.recipes.MagicRecipes.MagicRecipe;
import com.hbm_m.item.ModItems;
import com.hbm_m.lib.RefStrings;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/**
 * 1:1 {@code BookRecipeHandler} ("Black Book"): Hintergrund ist die Textur von {@code GUIBook},
 * bis zu vier Zutaten im 2x2-Raster (Abstand 36), Ergebnis bei (119, 24).
 */
public class BookJeiCategory implements IRecipeCategory<MagicRecipe> {

    public static final RecipeType<MagicRecipe> RECIPE_TYPE = RecipeType.create(RefStrings.MODID, "book_of_boxcars", MagicRecipe.class);

    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/gui/gui_book.png");

    private final IDrawable background;
    private final IDrawable icon;

    public BookJeiCategory(IGuiHelper guiHelper) {
        this.background = guiHelper.drawableBuilder(TEXTURE, 5, 11, 166, 65).setTextureSize(256, 256).build();
        this.icon = guiHelper.createDrawableItemStack(new ItemStack(ModItems.BOOK_OF_.get()));
    }

    public static java.util.List<MagicRecipe> recipes() {
        return MagicRecipes.getRecipes();
    }

    @Override public RecipeType<MagicRecipe> getRecipeType() { return RECIPE_TYPE; }
    @Override public Component getTitle() { return Component.translatableWithFallback("jei.hbm_m.book_of_boxcars", "Black Book"); }
    @Override public IDrawable getIcon() { return icon; }
    @Override public int getWidth() { return 166; }
    @Override public int getHeight() { return 65; }

    @Override
    @SuppressWarnings("removal")
    public IDrawable getBackground() { return background; }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, MagicRecipe recipe, IFocusGroup focuses) {
        for (int i = 0; i < Math.min(recipe.in.size(), 4); i++) {
            builder.addSlot(RecipeIngredientRole.INPUT, 25 + (i % 2) * 36, 6 + (i / 2) * 36)
                    .addItemStacks(Arrays.asList(recipe.in.get(i).get().getItems()));
        }
        builder.addSlot(RecipeIngredientRole.OUTPUT, 119, 24).addItemStack(recipe.getResult());
    }
}
//?} else {
/*public final class BookJeiCategory {
    private BookJeiCategory() {}
}*///?}
