package com.hbm_m.compat.jei;

//? if forge {
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
//?} elif neoforge {
/*import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
*///?}

//? if forge || neoforge {
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.inventory.material.MaterialStack;
import com.hbm_m.item.material.ItemCastMold;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.recipe.MoldCastingRecipe;

import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

@OnlyIn(Dist.CLIENT)
/**
 * Литьё в форму: расплав из ковша плюс форма дают предмет. Расплав рисуется свотчем,
 * форма и результат — обычными слотами.
 */
public class MoldCastingJeiCategory implements IRecipeCategory<MoldCastingRecipe> {

    public static final RecipeType<MoldCastingRecipe> RECIPE_TYPE =
            RecipeType.create(RefStrings.MODID, "mold_casting", MoldCastingRecipe.class);

    private static final int BG_WIDTH = 156;
    private static final int BG_HEIGHT = 46;
    private static final int MOLD_X = 30;
    private static final int SLOT_Y = 6;
    private static final int OUTPUT_X = 116;

    private static Map<ItemCastMold.MoldType, ItemStack> moldItems;

    private final IDrawable background;
    private final IDrawable icon;

    public MoldCastingJeiCategory(IGuiHelper guiHelper) {
        this.background = guiHelper.createBlankDrawable(BG_WIDTH, BG_HEIGHT);
        this.icon = guiHelper.createDrawableIngredient(
                VanillaTypes.ITEM_STACK, new ItemStack(ModBlocks.FOUNDRY_MOLD.get()));
    }

    @Override public RecipeType<MoldCastingRecipe> getRecipeType() { return RECIPE_TYPE; }

    @Override public Component getTitle() { return Component.translatable("block.hbm_m.foundry_mold"); }

    @Override @SuppressWarnings("removal") public IDrawable getBackground() { return background; }

    @Override public IDrawable getIcon() { return icon; }

    /** Формы регистрируются поштучно; собираем соответствие тип → предмет один раз по реестру. */
    private static Map<ItemCastMold.MoldType, ItemStack> molds() {
        if (moldItems == null) {
            Map<ItemCastMold.MoldType, ItemStack> map = new EnumMap<>(ItemCastMold.MoldType.class);
            for (Item item : BuiltInRegistries.ITEM) {
                if (item instanceof ItemCastMold mold) {
                    map.putIfAbsent(mold.getMoldType(), new ItemStack(item));
                }
            }
            moldItems = map;
        }
        return moldItems;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, MoldCastingRecipe recipe, IFocusGroup focuses) {
        ItemStack mold = molds().get(recipe.getMold());
        if (mold != null && !mold.isEmpty()) {
            builder.addSlot(RecipeIngredientRole.CATALYST, MOLD_X, SLOT_Y).addItemStack(mold);
        }
        builder.addSlot(RecipeIngredientRole.OUTPUT, OUTPUT_X, SLOT_Y).addItemStack(recipe.getOutput());
    }

    @Override
    public void draw(MoldCastingRecipe recipe, IRecipeSlotsView slots, GuiGraphics graphics, double mouseX, double mouseY) {
        int cost = recipe.getMold().getCostMb();
        MaterialStack molten = new MaterialStack(recipe.getMaterial(), cost);
        JeiMoltenRendering.drawSwatch(graphics, 6, SLOT_Y, molten);
        JeiMoltenRendering.drawLabel(graphics, 6, SLOT_Y + 22, molten);
    }

    /** Рецептов литья сотни; вытаскиваем их один раз для регистрации. */
    public static List<MoldCastingRecipe> filter(List<MoldCastingRecipe> all) {
        List<MoldCastingRecipe> out = new ArrayList<>(all.size());
        for (MoldCastingRecipe recipe : all) {
            if (!recipe.getOutput().isEmpty()) out.add(recipe);
        }
        return out;
    }
}
//?}
