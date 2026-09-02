package com.hbm_m.compat.jei;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.recipe.SolderingRecipe;
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
 * JEI-категория паяльной станции ({@code hbm_m:soldering_station}).
 *
 * <p>Работает напрямую с data-driven {@link SolderingRecipe} (JSON) — без промежуточной
 * {@code *JeiRecipe}-обёртки. Toppings/PCB/solder берутся из {@link SolderingRecipe#getToppings()} / 
 * {@link SolderingRecipe#getPcb()} / {@link SolderingRecipe#getSolder()}, выход — из
 * {@link SolderingRecipe#getOutput()}.</p>
 */
public class SolderingStationJeiCategory implements IRecipeCategory<SolderingRecipe> {

    public static final RecipeType<SolderingRecipe> RECIPE_TYPE =
            RecipeType.create(RefStrings.MODID, "soldering_station", SolderingRecipe.class);

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/gui/processing/gui_soldering_station.png");

    private final IDrawable background;
    private final IDrawable icon;

    public SolderingStationJeiCategory(IGuiHelper guiHelper) {
        this.background = guiHelper.createDrawable(TEXTURE, 0, 0, 176, 90);
        this.icon = guiHelper.createDrawableIngredient(VanillaTypes.ITEM_STACK, new ItemStack(ModBlocks.SOLDERING_STATION.get()));
    }

    @Override public RecipeType<SolderingRecipe> getRecipeType() { return RECIPE_TYPE; }
    @Override public Component getTitle() { return Component.translatable("block.hbm_m.soldering_station"); }
    @Override @SuppressWarnings("removal") public IDrawable getBackground() { return background; }
    @Override public IDrawable getIcon()       { return icon; }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, SolderingRecipe recipe, mezz.jei.api.recipe.IFocusGroup focuses) {
        // Toppings row — slots 0-2 at (17,18), (35,18), (53,18)
        addIngredientRow(builder, recipe.getToppings(), 18);
        // PCB row — (17,36), (35,36)
        addIngredientRow(builder, recipe.getPcb(), 36);
        // Solder — (53,36)
        Ingredient[] solder = recipe.getSolder();
        if (solder.length > 0) {
            builder.addSlot(RecipeIngredientRole.INPUT, 53, 36).addIngredients(solder[0]);
        }
        // Output
        ItemStack out = recipe.getOutput();
        if (!out.isEmpty()) {
            builder.addSlot(RecipeIngredientRole.OUTPUT, 107, 27).addItemStack(out);
        }
    }

    private static void addIngredientRow(IRecipeLayoutBuilder b, Ingredient[] group, int y) {
        int[] xs = {17, 35, 53};
        for (int i = 0; i < Math.min(xs.length, group.length); i++) {
            b.addSlot(RecipeIngredientRole.INPUT, xs[i], y).addIngredients(group[i]);
        }
    }
}
//?} else {
/*public final class SolderingStationJeiCategory { private SolderingStationJeiCategory() {} }
*///?}
