package com.hbm_m.compat.jei;
//? if forge || neoforge {

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.hbm_m.compat.jei.NeiUniversalJeiCategory.Slot;
import com.hbm_m.inventory.recipes.RefineryRecipes;
import com.hbm_m.item.ModItems;
import com.hbm_m.lib.RefStrings;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.drawable.IDrawableAnimated;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;

/**
 * 1:1 {@code RefineryRecipeHandler}: eigene Textur {@code gui_nei_refinery.png}, 1000 mB heisses Oel
 * links, vier Fraktionen (Anteil x10) und das feste Nebenprodukt rechts, dazu die Tank- und
 * Pfeilanimation aus {@code drawExtras}.
 */
public class RefineryJeiCategory implements IRecipeCategory<RefineryJeiCategory.Recipe> {

    public record Recipe(Slot input, List<Slot> outputs) { }

    public static final RecipeType<Recipe> RECIPE_TYPE = RecipeType.create(RefStrings.MODID, "refinery", Recipe.class);

    public static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/gui/nei/gui_nei_refinery.png");

    /** Original {@code SmeltingSet}: Ergebnisplaetze 1-5. */
    private static final int[][] OUT_POS = {{111, 6}, {129, 15}, {111, 24}, {129, 33}, {111, 42}};

    private final IDrawable background;
    private final IDrawable icon;
    private final IDrawableAnimated tank;
    private final IDrawableAnimated arrow;

    public RefineryJeiCategory(IGuiHelper guiHelper) {
        // TemplateRecipeHandler.drawBackground: drawTexturedModalRect(0, 0, 5, 11, 166, 65)
        this.background = guiHelper.drawableBuilder(TEXTURE, 5, 11, 166, 65).setTextureSize(256, 256).build();
        this.icon = guiHelper.createDrawableItemStack(new ItemStack(ModItems.REFINERY.get()));
        // drawProgressBar(83 - 72 - 9 + 1, 6, 0, 86, 16, 52, 480, 7): nach oben, ablaufend
        this.tank = guiHelper.drawableBuilder(TEXTURE, 0, 86, 16, 52).setTextureSize(256, 256)
                .buildAnimated(480, IDrawableAnimated.StartDirection.BOTTOM, true);
        // drawProgressBar(56 + 22, 5 + 19, 16, 86, 24, 17, 48, 0)
        this.arrow = guiHelper.drawableBuilder(TEXTURE, 16, 86, 24, 17).setTextureSize(256, 256)
                .buildAnimated(48, IDrawableAnimated.StartDirection.LEFT, false);
    }

    /** Original {@code RefineryRecipes.getRefineryRecipe}: Eingang 1000 mB, Fraktionen x10, gelesen aus {@link RefineryRecipes}. */
    public static List<Recipe> recipes() {
        List<Recipe> list = new ArrayList<>();
        for (Map.Entry<Fluid, RefineryRecipes.RefineryRecipe> e : RefineryRecipes.all().entrySet()) {
            RefineryRecipes.RefineryRecipe r = e.getValue();
            List<Slot> outs = new ArrayList<>();
            for (RefineryRecipes.FluidOut f : r.outputs) outs.add(Slot.fluid(f.type(), f.fill() * 10));
            outs.add(Slot.of(r.solid.copy()));
            list.add(new Recipe(Slot.fluid(e.getKey(), 1000), List.copyOf(outs)));
        }
        return list;
    }

    @Override public RecipeType<Recipe> getRecipeType() { return RECIPE_TYPE; }
    @Override public Component getTitle() { return Component.translatableWithFallback("jei.hbm_m.refinery", "Refinery"); }
    @Override public IDrawable getIcon() { return icon; }
    @Override public int getWidth() { return 166; }
    @Override public int getHeight() { return 65; }

    @Override
    @SuppressWarnings("removal")
    public IDrawable getBackground() { return background; }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, Recipe recipe, IFocusGroup focuses) {
        // input: 21 + 27, 6 + 18
        NeiUniversalJeiCategory.addSlot(builder, RecipeIngredientRole.INPUT, 48, 24, recipe.input(), null);
        for (int i = 0; i < recipe.outputs().size() && i < OUT_POS.length; i++) {
            NeiUniversalJeiCategory.addSlot(builder, RecipeIngredientRole.OUTPUT, OUT_POS[i][0], OUT_POS[i][1], recipe.outputs().get(i), null);
        }
    }

    @Override
    public void draw(Recipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics guiGraphics, double mouseX, double mouseY) {
        tank.draw(guiGraphics, 3, 6);
        arrow.draw(guiGraphics, 78, 24);
    }
}
//?} else {
/*public final class RefineryJeiCategory {
    private RefineryJeiCategory() {}
}*///?}
