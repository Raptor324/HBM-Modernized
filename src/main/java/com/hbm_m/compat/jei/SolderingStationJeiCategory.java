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

import java.util.List;

public class SolderingStationJeiCategory implements IRecipeCategory<SolderingStationJeiRecipe> {

    public static final RecipeType<SolderingStationJeiRecipe> RECIPE_TYPE =
            RecipeType.create(RefStrings.MODID, "soldering_station", SolderingStationJeiRecipe.class);

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/gui/processing/gui_soldering_station.png");

    private final IDrawable background;
    private final IDrawable icon;

    public SolderingStationJeiCategory(IGuiHelper guiHelper) {
        this.background = guiHelper.createDrawable(TEXTURE, 0, 0, 176, 90);
        this.icon = guiHelper.createDrawableIngredient(VanillaTypes.ITEM_STACK, new ItemStack(ModBlocks.SOLDERING_STATION.get()));
    }

    @Override public RecipeType<SolderingStationJeiRecipe> getRecipeType() { return RECIPE_TYPE; }
    @Override public Component getTitle() { return Component.translatable("block.hbm_m.soldering_station"); }
    @Override public IDrawable getBackground() { return background; }
    @Override public IDrawable getIcon()       { return icon; }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, SolderingStationJeiRecipe recipe, mezz.jei.api.recipe.IFocusGroup focuses) {
        // Toppings row — slots 0-2 at (17,18), (35,18), (53,18)
        addRow(builder, recipe.getToppings(), 18);
        // PCB + solder row — PCB at (17,36), (35,36); solder at (53,36)
        addRow(builder, recipe.getPcb(), 36);
        if (!recipe.getSolder().isEmpty())
            addSlotAt(builder, recipe.getSolder().get(0), 53, 36);
        // Output
        builder.addSlot(RecipeIngredientRole.OUTPUT, 107, 27)
                .addItemStack(recipe.getOutput());
    }

    private static void addRow(IRecipeLayoutBuilder b, List<List<ItemStack>> list, int y) {
        int[] xs = {17, 35, 53};
        for (int i = 0; i < Math.min(xs.length, list.size()); i++) {
            var stacks = list.get(i).stream().filter(s -> !s.isEmpty()).toList();
            if (!stacks.isEmpty())
                b.addSlot(RecipeIngredientRole.INPUT, xs[i], y).addItemStacks(stacks);
        }
    }

    private static void addSlotAt(IRecipeLayoutBuilder b, List<ItemStack> items, int x, int y) {
        var stacks = items.stream().filter(s -> !s.isEmpty()).toList();
        if (!stacks.isEmpty())
            b.addSlot(RecipeIngredientRole.INPUT, x, y).addItemStacks(stacks);
    }
}
//?} else {
/*public final class SolderingStationJeiCategory { private SolderingStationJeiCategory() {} }
*///?}
