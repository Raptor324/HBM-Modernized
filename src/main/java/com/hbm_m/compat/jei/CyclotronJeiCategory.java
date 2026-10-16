package com.hbm_m.compat.jei;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.recipe.CyclotronRecipe;

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

//? if forge {
/**
 * JEI-категория циклотрона ({@code hbm_m:cyclotron}).
 *
 * <p>Работает напрямую с data-driven {@link CyclotronRecipe} (JSON) — без промежуточной
 * {@code *JeiRecipe}-обёртки. Target/input — {@link CyclotronRecipe#getTarget()} / #{@link CyclotronRecipe#getInput()},
 * выход — {@link CyclotronRecipe#getOutput()}.</p>
 */
public class CyclotronJeiCategory implements IRecipeCategory<CyclotronRecipe> {

    public static final RecipeType<CyclotronRecipe> RECIPE_TYPE =
            RecipeType.create(RefStrings.MODID, "cyclotron", CyclotronRecipe.class);

    private static final ResourceLocation TEXTURE =
            //? if fabric && < 1.21.1 {
            /*new ResourceLocation(RefStrings.MODID, "textures/jei_gui/gui_jei_cyclotron.png");
            *///?} else {
                        ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/jei_gui/gui_jei_cyclotron.png");
            //?}


    private final IDrawable background;
    private final IDrawable icon;

    public CyclotronJeiCategory(IGuiHelper guiHelper) {
        this.background = guiHelper.createDrawable(TEXTURE, 0, 0, 176, 82);
        this.icon = guiHelper.createDrawableIngredient(VanillaTypes.ITEM_STACK, new ItemStack(ModBlocks.CYCLOTRON.get()));
    }

    @Override
    public RecipeType<CyclotronRecipe> getRecipeType() {
        return RECIPE_TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("block.hbm_m.cyclotron");
    }

    @Override
    @SuppressWarnings("removal")
    public IDrawable getBackground() {
        return background;
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, CyclotronRecipe recipe, mezz.jei.api.recipe.IFocusGroup focuses) {
        // Таргет-слот (что бомбардируем) — slot 0, позиция (17, 34).
        builder.addSlot(RecipeIngredientRole.INPUT, 17, 34)
                .addIngredients(recipe.getTarget());

        // Реактив-слот (чем бомбардируем) — slot 1, позиция (71, 34).
        builder.addSlot(RecipeIngredientRole.INPUT, 71, 34)
                .addIngredients(recipe.getInput());

        // Выход — позиция (127, 34). Пустой выход не показываем (фильтр по источнику уже отсеивает
        // рецепты без result, но на всякий случай — guard).
        ItemStack output = recipe.getOutput();
        if (!output.isEmpty()) {
            builder.addSlot(RecipeIngredientRole.OUTPUT, 127, 34)
                    .addItemStack(output);
        }
    }
}
//?} else {
/*public final class CyclotronJeiCategory {
    private CyclotronJeiCategory() {}
}*///?}
