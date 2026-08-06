package com.hbm_m.compat.jei;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.inventory.fluid.ModFluids;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.recipe.CrackingTowerRecipes;
import com.hbm_m.recipe.CrackingTowerRecipes.Crack;

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
 * JEI category for Cracking Tower recipes (siehe {@link CrackingTowerRecipes}): 100mB Eingangsfluid
 * + 200mB Dampf -&gt; outA + optional outB + 2mB Restdampf.
 */
//? if forge {
public class CrackingTowerJeiCategory extends JeiGenericRecipeCategory<CrackingTowerJeiCategory.JeiRecipe> {

    public record JeiRecipe(Fluid in, int inAmount, Crack crack) {}

    public static final RecipeType<JeiRecipe> RECIPE_TYPE =
            RecipeType.create(RefStrings.MODID, "cracking_tower", JeiRecipe.class);

    private static final int FLUID_RENDERER_CAPACITY = 24_000;

    public CrackingTowerJeiCategory(IGuiHelper guiHelper) {
        super(guiHelper, new ItemStack[]{
                new ItemStack(ModBlocks.CRACKING_TOWER.get())
        });
    }

    public static List<JeiRecipe> fromRecipes() {
        List<JeiRecipe> result = new ArrayList<>();
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
        return Component.translatable("block.hbm_m.cracking_tower");
    }

    @Override
    protected int getInputCount(JeiRecipe recipe) {
        return 2;
    }

    @Override
    protected int getOutputCount(JeiRecipe recipe) {
        int count = 1; // outA
        if (recipe.crack().outB() != ModFluids.NONE.getSource()) count++;
        return count + 1; // + spent steam
    }

    @Override
    protected boolean hasBlueprintTemplate(JeiRecipe recipe) {
        return false;
    }

    @Override
    protected void addInputSlots(IRecipeLayoutBuilder builder, JeiRecipe recipe, int inputXOffset) {
        int[][] positions = JeiNeiLayout.getGenericInputSlotPositions(2);
        addFluidSlot(builder, RecipeIngredientRole.INPUT,
                positions[0][0] + inputXOffset, positions[0][1],
                FluidStack.create(recipe.in(), recipe.inAmount()));
        addFluidSlot(builder, RecipeIngredientRole.INPUT,
                positions[1][0] + inputXOffset, positions[1][1],
                FluidStack.create(ModFluids.STEAM.getSource(), CrackingTowerRecipes.STEAM_PER_100_INPUT));
    }

    @Override
    protected void addOutputSlots(IRecipeLayoutBuilder builder, JeiRecipe recipe, int outputXOffset) {
        int[][] positions = JeiNeiLayout.getGenericOutputSlotPositions(getOutputCount(recipe));
        int slotIndex = 0;

        addFluidSlot(builder, RecipeIngredientRole.OUTPUT,
                positions[slotIndex][0] + outputXOffset, positions[slotIndex][1],
                FluidStack.create(recipe.crack().outA(), recipe.crack().amountA()));
        slotIndex++;

        if (recipe.crack().outB() != ModFluids.NONE.getSource()) {
            addFluidSlot(builder, RecipeIngredientRole.OUTPUT,
                    positions[slotIndex][0] + outputXOffset, positions[slotIndex][1],
                    FluidStack.create(recipe.crack().outB(), recipe.crack().amountB()));
            slotIndex++;
        }

        addFluidSlot(builder, RecipeIngredientRole.OUTPUT,
                positions[slotIndex][0] + outputXOffset, positions[slotIndex][1],
                FluidStack.create(ModFluids.SPENTSTEAM.getSource(), CrackingTowerRecipes.SPENTSTEAM_PRODUCED));
    }

    private void addFluidSlot(IRecipeLayoutBuilder builder, RecipeIngredientRole role, int x, int y, FluidStack fluid) {
        addItemSlot(builder, role, x, y)
                .setFluidRenderer(FLUID_RENDERER_CAPACITY, false, 16, 16)
                .setCustomRenderer(ForgeTypes.FLUID_STACK, new HbmFluidJeiRenderer(16, 16))
                .addIngredient(ForgeTypes.FLUID_STACK, FluidStackHooksForge.toForge(fluid));
    }

    @Override
    protected void addBlueprintSlot(IRecipeLayoutBuilder builder, JeiRecipe recipe, int machineXOffset) {
        // Kein Blueprint-Slot fuer Cracking-Tower-Rezepte.
    }
}
//?} else {
/*public final class CrackingTowerJeiCategory {
    private CrackingTowerJeiCategory() {}
}*///?}
