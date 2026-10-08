package com.hbm_m.compat.jei;

import java.util.ArrayList;
import java.util.List;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.block.machines.anvils.AnvilBlock;
import com.hbm_m.block.machines.anvils.AnvilTier;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.recipe.AnvilRecipe;
import com.hbm_m.recipe.AnvilRecipe.AStack;
import com.hbm_m.recipe.AnvilRecipe.ResultEntry;

import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * JEI-Port von {@code AnvilRecipeHandler} (Konstruktion): {@code gui_nei_anvil.png}, Anordnung nach Anzahl der
 * Zutaten/Ausgaben, Ambosse genau der unteren Stufe als Katalysator, Chance als roter Tooltip.
 */
public class AnvilJeiCategory implements IRecipeCategory<AnvilRecipe> {

    public static final RecipeType<AnvilRecipe> RECIPE_TYPE =
            RecipeType.create(RefStrings.MODID, "anvil", AnvilRecipe.class);

    private final IDrawable background;
    private final IDrawable icon;
    private final IDrawable itemSlotBackground;

    public AnvilJeiCategory(IGuiHelper guiHelper) {
        this.background = JeiAnvilTextures.createRecipeBackground(guiHelper);
        this.itemSlotBackground = JeiAnvilTextures.createItemSlotBackground(guiHelper);
        this.icon = guiHelper.createDrawableIngredient(VanillaTypes.ITEM_STACK, new ItemStack(ModBlocks.ANVIL_STEEL.get()));
    }

    @Override
    public RecipeType<AnvilRecipe> getRecipeType() {
        return RECIPE_TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.literal("Anvil");
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
    public int getWidth() {
        return JeiAnvilTextures.RECIPE_WIDTH;
    }

    @Override
    public int getHeight() {
        return JeiAnvilTextures.RECIPE_HEIGHT;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, AnvilRecipe recipe, IFocusGroup focuses) {
        List<AStack> inputs = recipe.getInputs();
        List<ResultEntry> outputs = recipe.getOutputs();
        JeiAnvilLayout.Layout layout = JeiAnvilLayout.resolve(inputs.size(), outputs.size());

        int[][] inPos = JeiAnvilLayout.getInputPositions(layout, inputs.size());
        for (int i = 0; i < inputs.size(); i++) {
            addItemSlot(builder, RecipeIngredientRole.INPUT, inPos[i][0], inPos[i][1])
                    .addItemStacks(inputs.get(i).displayStacks());
        }

        int[][] outPos = JeiAnvilLayout.getOutputPositions(layout, outputs.size());
        for (int i = 0; i < outputs.size(); i++) {
            ItemStack output = outputs.get(i).stack();
            IRecipeSlotBuilder slot = addItemSlot(builder, RecipeIngredientRole.OUTPUT, outPos[i][0], outPos[i][1])
                    .addItemStack(output);
            float chance = outputs.get(i).chance();
            if (chance != 1.0F) {
                double percent = ((int) (chance * 1000)) / 10.0D;
                slot.addRichTooltipCallback((view, tooltip) ->
                        tooltip.add(Component.literal(percent + "%").withStyle(ChatFormatting.RED)));
            }
        }

        builder.addSlot(RecipeIngredientRole.CATALYST, layout.anvX(), layout.anvY())
                .addItemStacks(getAnvilsForTier(recipe.getRequiredTier()));
    }

    @Override
    public void draw(AnvilRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics guiGraphics,
                     double mouseX, double mouseY) {
        JeiAnvilLayout.Layout layout = JeiAnvilLayout.resolve(recipe.getInputs().size(), recipe.getOutputs().size());
        JeiAnvilRendering.drawOverlay(layout.shape(), guiGraphics);
    }

    private IRecipeSlotBuilder addItemSlot(IRecipeLayoutBuilder builder, RecipeIngredientRole role, int x, int y) {
        return builder.addSlot(role, x, y).setBackground(itemSlotBackground, -1, -1);
    }

    /** {@code NTMAnvil.getAnvilsFromTier}: genau diese Stufe. */
    public static List<ItemStack> getAnvilsForTier(AnvilTier tier) {
        List<ItemStack> stacks = new ArrayList<>();
        for (var block : ModBlocks.getAnvilBlocks()) {
            if (block.get() instanceof AnvilBlock anvil && anvil.getTier() == tier) {
                stacks.add(new ItemStack(block.get()));
            }
        }
        return stacks;
    }
}
