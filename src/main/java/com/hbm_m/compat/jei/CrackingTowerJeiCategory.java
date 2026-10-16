package com.hbm_m.compat.jei;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.inventory.fluid.ModFluids;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.recipe.CrackingTowerRecipe;

import dev.architectury.fluid.FluidStack;
//? if forge {
//? if forge {
import mezz.jei.api.forge.ForgeTypes;
//?} elif neoforge {
/*import mezz.jei.api.neoforge.NeoForgeTypes;
*///?}
//? if forge {
import dev.architectury.hooks.fluid.forge.FluidStackHooksForge;
//?}
//?} elif neoforge {
/*import mezz.jei.api.neoforge.NeoForgeTypes;
*///?}
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * JEI category for Cracking Tower recipes (siehe {@link CrackingTowerRecipe}): 100mB Eingangsfluid
 * + 200mB Dampf -> outA + optional outB + 2mB Restdampf.
 *
 * <p>Data-driven: рецепты читаются напрямую из {@code RecipeManager} (JSON {@code hbm_m:cracking_tower}),
 * ранее — статический {@code CrackingTowerRecipes}.</p>
 */
//? if forge {
public class CrackingTowerJeiCategory extends JeiGenericRecipeCategory<CrackingTowerRecipe> {

    public static final RecipeType<CrackingTowerRecipe> RECIPE_TYPE =
            RecipeType.create(RefStrings.MODID, "cracking_tower", CrackingTowerRecipe.class);

    private static final int FLUID_RENDERER_CAPACITY = 24_000;

    public CrackingTowerJeiCategory(IGuiHelper guiHelper) {
        super(guiHelper, new ItemStack[]{
                new ItemStack(ModBlocks.CRACKING_TOWER.get())
        });
    }

    @Override
    public RecipeType<CrackingTowerRecipe> getRecipeType() {
        return RECIPE_TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("block.hbm_m.cracking_tower");
    }

    @Override
    protected int getInputCount(CrackingTowerRecipe recipe) {
        return 2;
    }

    @Override
    protected int getOutputCount(CrackingTowerRecipe recipe) {
        int count = 1; // outA
        if (recipe.hasOutputB()) count++;
        return count + 1; // + spent steam
    }

    @Override
    protected boolean hasBlueprintTemplate(CrackingTowerRecipe recipe) {
        return false;
    }

    @Override
    protected void addInputSlots(IRecipeLayoutBuilder builder, CrackingTowerRecipe recipe, int inputXOffset) {
        int[][] positions = JeiNeiLayout.getGenericInputSlotPositions(2);
        addFluidSlot(builder, RecipeIngredientRole.INPUT,
                positions[0][0] + inputXOffset, positions[0][1],
                FluidStack.create(recipe.getInputFluid(), recipe.getInputMb()));
        addFluidSlot(builder, RecipeIngredientRole.INPUT,
                positions[1][0] + inputXOffset, positions[1][1],
                FluidStack.create(ModFluids.STEAM.getSource(), CrackingTowerRecipe.STEAM_PER_100_INPUT));
    }

    @Override
    protected void addOutputSlots(IRecipeLayoutBuilder builder, CrackingTowerRecipe recipe, int outputXOffset) {
        int[][] positions = JeiNeiLayout.getGenericOutputSlotPositions(getOutputCount(recipe));
        int slotIndex = 0;

        addFluidSlot(builder, RecipeIngredientRole.OUTPUT,
                positions[slotIndex][0] + outputXOffset, positions[slotIndex][1],
                FluidStack.create(recipe.getOutputA(), recipe.getOutputAMb()));
        slotIndex++;

        if (recipe.hasOutputB()) {
            addFluidSlot(builder, RecipeIngredientRole.OUTPUT,
                    positions[slotIndex][0] + outputXOffset, positions[slotIndex][1],
                    FluidStack.create(recipe.getOutputB(), recipe.getOutputBMb()));
            slotIndex++;
        }

        addFluidSlot(builder, RecipeIngredientRole.OUTPUT,
                positions[slotIndex][0] + outputXOffset, positions[slotIndex][1],
                FluidStack.create(ModFluids.SPENTSTEAM.getSource(), CrackingTowerRecipe.SPENTSTEAM_PRODUCED));
    }

    private void addFluidSlot(IRecipeLayoutBuilder builder, RecipeIngredientRole role, int x, int y, FluidStack fluid) {
        addItemSlot(builder, role, x, y)
                .setFluidRenderer(FLUID_RENDERER_CAPACITY, false, 16, 16)
                //? if forge {
                .setCustomRenderer(ForgeTypes.FLUID_STACK, new HbmFluidJeiRenderer(16, 16))
                .addIngredient(ForgeTypes.FLUID_STACK, FluidStackHooksForge.toForge(fluid));
                //?} elif neoforge {
                /*.setCustomRenderer(NeoForgeTypes.FLUID_STACK, new HbmFluidJeiRenderer(16, 16))
                .addIngredient(NeoForgeTypes.FLUID_STACK, new net.neoforged.neoforge.fluids.FluidStack(fluid.getFluid(), (int) fluid.getAmount()));
                *///?}
    }

    @Override
    protected void addBlueprintSlot(IRecipeLayoutBuilder builder, CrackingTowerRecipe recipe, int machineXOffset) {
        // Kein Blueprint-Slot fuer Cracking-Tower-Rezepte.
    }
}
//?} else {
/*public final class CrackingTowerJeiCategory {
    private CrackingTowerJeiCategory() {}
}*///?}
