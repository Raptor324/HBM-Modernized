package com.hbm_m.compat.jei;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.handler.rbmk.RBMKOutgasserRecipes;
import com.hbm_m.handler.rbmk.RBMKOutgasserRecipes.OutgasserRecipe;
import com.hbm_m.lib.RefStrings;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * JEI listing for the RBMK irradiation channel, ported from CE's {@code RBMKOutgasserRecipeHandler}.
 *
 * <p>Without it the outgasser's whole recipe table is invisible: nothing else in the game hints
 * that lithium becomes tritium or that gold becomes gold-198 inside a reactor channel.</p>
 */
public class RBMKOutgasserJeiCategory extends JeiGenericRecipeCategory<RBMKOutgasserJeiCategory.Activation> {

    /** {@code fluidOutput} is null for recipes that only produce a solid, and vice versa. */
    public record Activation(ItemStack input, ItemStack solidOutput, Fluid fluidType, int fluidAmount) {

        public boolean hasSolid() { return solidOutput != null && !solidOutput.isEmpty(); }
        public boolean hasFluid() { return fluidType != null && fluidAmount > 0; }
    }

    public static final RecipeType<Activation> RECIPE_TYPE =
            RecipeType.create(RefStrings.MODID, "rbmk_outgasser", Activation.class);

    public RBMKOutgasserJeiCategory(IGuiHelper guiHelper) {
        super(guiHelper, new ItemStack[]{ new ItemStack(ModBlocks.RBMK_OUTGASSER.get()) });
    }

    public static List<Activation> all() {
        List<Activation> out = new ArrayList<>();
        for (Map.Entry<net.minecraft.world.item.Item, OutgasserRecipe> e : RBMKOutgasserRecipes.getRecipes().entrySet()) {
            OutgasserRecipe r = e.getValue();
            out.add(new Activation(new ItemStack(e.getKey()),
                    r.solidOutput(), r.fluidType(), r.fluidAmount()));
        }
        return out;
    }

    @Override public RecipeType<Activation> getRecipeType() { return RECIPE_TYPE; }

    @Override
    public Component getTitle() {
        return Component.translatable("gui.hbm_m.jei.rbmk_outgasser");
    }

    @Override protected int getInputCount(Activation recipe)  { return 1; }
    @Override protected int getOutputCount(Activation recipe) { return recipe.hasSolid() ? 1 : 0; }
    @Override protected boolean hasBlueprintTemplate(Activation recipe) { return false; }

    @Override
    protected void addInputSlots(IRecipeLayoutBuilder builder, Activation recipe, int inputXOffset) {
        int[][] positions = JeiNeiLayout.getGenericInputSlotPositions(1);
        addItemSlot(builder, RecipeIngredientRole.INPUT, positions[0][0] + inputXOffset, positions[0][1])
                .addItemStack(recipe.input());
    }

    @Override
    protected void addOutputSlots(IRecipeLayoutBuilder builder, Activation recipe, int outputXOffset) {
        if (!recipe.hasSolid()) return;
        int[][] positions = JeiNeiLayout.getGenericOutputSlotPositions(1);
        addItemSlot(builder, RecipeIngredientRole.OUTPUT, positions[0][0] + outputXOffset, positions[0][1])
                .addItemStack(recipe.solidOutput());
    }

    @Override
    protected void addBlueprintSlot(IRecipeLayoutBuilder builder, Activation recipe, int machineXOffset) {
        // Activation needs no blueprint - only neutron flux.
    }
}
