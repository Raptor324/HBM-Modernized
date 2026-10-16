package com.hbm_m.compat.jei;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.recipe.ElectrolyserFluidRecipe;

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
 * JEI category for Electrolyser fluid-mode recipes - 1 Fluid-Eingang -> bis zu 2 Fluid-Ausgaenge
 * + Byprodukt-Items (siehe {@link ElectrolyserFluidRecipe}).
 *
 * <p>Data-driven: рецепты читаются напрямую aus {@code RecipeManager} (JSON {@code hbm_m:electrolyser_fluid}),
 * ранее — статический {@code ElectrolyserRecipes} (fluid-mode).</p>
 */
//? if forge {
public class ElectrolyserFluidJeiCategory extends JeiGenericRecipeCategory<ElectrolyserFluidRecipe> {

    public static final RecipeType<ElectrolyserFluidRecipe> RECIPE_TYPE =
            RecipeType.create(RefStrings.MODID, "electrolyser_fluid", ElectrolyserFluidRecipe.class);

    private static final int FLUID_RENDERER_CAPACITY = 4_000;

    public ElectrolyserFluidJeiCategory(IGuiHelper guiHelper) {
        super(guiHelper, new ItemStack[]{
                new ItemStack(ModBlocks.ELECTROLYSER.get())
        });
    }

    @Override
    public RecipeType<ElectrolyserFluidRecipe> getRecipeType() {
        return RECIPE_TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("container.hbm_m.electrolyser");
    }

    @Override
    protected int getInputCount(ElectrolyserFluidRecipe recipe) {
        return 1;
    }

    @Override
    protected int getOutputCount(ElectrolyserFluidRecipe recipe) {
        int count = 0;
        if (recipe.getOutputA() != null) count++;
        if (recipe.getOutputB() != null) count++;
        count += recipe.getByproducts().length;
        return count;
    }

    @Override
    protected boolean hasBlueprintTemplate(ElectrolyserFluidRecipe recipe) {
        return false;
    }

    @Override
    protected void addInputSlots(IRecipeLayoutBuilder builder, ElectrolyserFluidRecipe recipe, int inputXOffset) {
        addFluidSlot(builder, RecipeIngredientRole.INPUT, inputXOffset, 22,
                FluidStack.create(recipe.getInputFluid(), recipe.getInputAmount()));
    }

    @Override
    protected void addOutputSlots(IRecipeLayoutBuilder builder, ElectrolyserFluidRecipe recipe, int outputXOffset) {
        int outputCount = getOutputCount(recipe);
        int[][] positions = JeiNeiLayout.getGenericOutputSlotPositions(outputCount);
        int slotIndex = 0;

        if (recipe.getOutputA() != null) {
            addFluidSlot(builder, RecipeIngredientRole.OUTPUT, positions[slotIndex][0] + outputXOffset, positions[slotIndex][1],
                    FluidStack.create(recipe.getOutputA().getFluid(), recipe.getOutputA().getAmount()));
            slotIndex++;
        }
        if (recipe.getOutputB() != null) {
            addFluidSlot(builder, RecipeIngredientRole.OUTPUT, positions[slotIndex][0] + outputXOffset, positions[slotIndex][1],
                    FluidStack.create(recipe.getOutputB().getFluid(), recipe.getOutputB().getAmount()));
            slotIndex++;
        }
        for (ItemStack byproduct : recipe.getByproducts()) {
            addItemSlot(builder, RecipeIngredientRole.OUTPUT, positions[slotIndex][0] + outputXOffset, positions[slotIndex][1])
                    .addItemStack(byproduct);
            slotIndex++;
        }
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
    protected void addBlueprintSlot(IRecipeLayoutBuilder builder, ElectrolyserFluidRecipe recipe, int machineXOffset) {
        // Kein Blueprint-Slot fuer Electrolyser-Fluid-Rezepte.
    }
}
//?} else {
/*public final class ElectrolyserFluidJeiCategory {
    private ElectrolyserFluidJeiCategory() {}
}*///?}
