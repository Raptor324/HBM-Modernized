package com.hbm_m.compat.jei;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.inventory.fluid.ModFluids;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.recipe.CrackingTowerRecipes;
import com.hbm_m.recipe.CrackingTowerRecipes.Crack;
import com.hbm_m.recipe.RadiolysisRecipes;

import dev.architectury.fluid.FluidStack;
import dev.architectury.hooks.fluid.forge.FluidStackHooksForge;
import mezz.jei.api.forge.ForgeTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;

/**
 * JEI category for Radiolysis-Kammer recipes (siehe {@link RadiolysisRecipes}): 100mB Eingangsfluid
 * -&gt; outA + optional outB (kein Dampf-Co-Input, anders als {@link CrackingTowerJeiCategory}).
 */
//? if forge {
public class RadiolysisJeiCategory extends JeiGenericRecipeCategory<RadiolysisJeiCategory.JeiRecipe> {

    public record JeiRecipe(Fluid in, int inAmount, Crack crack) {}

    public static final RecipeType<JeiRecipe> RECIPE_TYPE =
            RecipeType.create(RefStrings.MODID, "radiolysis", JeiRecipe.class);

    private static final int FLUID_RENDERER_CAPACITY = 24_000;

    public RadiolysisJeiCategory(IGuiHelper guiHelper) {
        super(guiHelper, new ItemStack[]{
                new ItemStack(ModBlocks.RADIOLYSIS.get())
        });
    }

    public static List<JeiRecipe> fromRecipes() {
        List<JeiRecipe> result = new ArrayList<>();
        result.add(new JeiRecipe(ModFluids.WATER.getSource(), 100,
                new Crack(ModFluids.PEROXIDE.getSource(), 80, ModFluids.HYDROGEN.getSource(), 20)));
        for (Map.Entry<Fluid, Crack> entry : CrackingTowerRecipes.getAll().entrySet()) {
            result.add(new JeiRecipe(entry.getKey(), 100, entry.getValue()));
        }
        return result;
    }

    @Override
    public RecipeType<JeiRecipe> getRecipeType() {
        return RECIPE_TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("block.hbm_m.radiolysis");
    }

    @Override
    protected int getInputCount(JeiRecipe recipe) {
        return 1;
    }

    @Override
    protected int getOutputCount(JeiRecipe recipe) {
        return recipe.crack().outB() != ModFluids.NONE.getSource() ? 2 : 1;
    }

    @Override
    protected boolean hasBlueprintTemplate(JeiRecipe recipe) {
        return false;
    }

    @Override
    protected void addInputSlots(IRecipeLayoutBuilder builder, JeiRecipe recipe, int inputXOffset) {
        addFluidSlot(builder, RecipeIngredientRole.INPUT, inputXOffset, 22,
                FluidStack.create(recipe.in(), recipe.inAmount()));
    }

    @Override
    protected void addOutputSlots(IRecipeLayoutBuilder builder, JeiRecipe recipe, int outputXOffset) {
        int[][] positions = JeiNeiLayout.getGenericOutputSlotPositions(getOutputCount(recipe));
        addFluidSlot(builder, RecipeIngredientRole.OUTPUT,
                positions[0][0] + outputXOffset, positions[0][1],
                FluidStack.create(recipe.crack().outA(), recipe.crack().amountA()));

        if (recipe.crack().outB() != ModFluids.NONE.getSource()) {
            addFluidSlot(builder, RecipeIngredientRole.OUTPUT,
                    positions[1][0] + outputXOffset, positions[1][1],
                    FluidStack.create(recipe.crack().outB(), recipe.crack().amountB()));
        }
    }

    private void addFluidSlot(IRecipeLayoutBuilder builder, RecipeIngredientRole role, int x, int y, FluidStack fluid) {
        addItemSlot(builder, role, x, y)
                .setFluidRenderer(FLUID_RENDERER_CAPACITY, false, 16, 16)
                .setCustomRenderer(ForgeTypes.FLUID_STACK, new HbmFluidJeiRenderer(16, 16))
                .addIngredient(ForgeTypes.FLUID_STACK, FluidStackHooksForge.toForge(fluid));
    }

    @Override
    protected void addBlueprintSlot(IRecipeLayoutBuilder builder, JeiRecipe recipe, int machineXOffset) {
        // Kein Blueprint-Slot fuer Radiolysis-Rezepte.
    }
}
//?} else {
/*public final class RadiolysisJeiCategory {
    private RadiolysisJeiCategory() {}
}*///?}
