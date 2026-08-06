package com.hbm_m.compat.jei;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.recipe.ElectrolyserRecipes;
import com.hbm_m.recipe.ElectrolyserRecipes.FluidRecipe;

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
 * JEI category for Electrolyser fluid-mode recipes - 1 Fluid-Eingang -&gt; bis zu 2 Fluid-Ausgaenge
 * + Byprodukt-Items (siehe {@link ElectrolyserRecipes}).
 */
//? if forge {
public class ElectrolyserFluidJeiCategory extends JeiGenericRecipeCategory<Map.Entry<Fluid, FluidRecipe>> {

    public static final RecipeType<Map.Entry<Fluid, FluidRecipe>> RECIPE_TYPE =
            RecipeType.create(RefStrings.MODID, "electrolyser_fluid", castEntry());

    private static final int FLUID_RENDERER_CAPACITY = 4_000;

    public ElectrolyserFluidJeiCategory(IGuiHelper guiHelper) {
        super(guiHelper, new ItemStack[]{
                new ItemStack(ModBlocks.ELECTROLYSER.get())
        });
    }

    @SuppressWarnings("unchecked")
    private static Class<Map.Entry<Fluid, FluidRecipe>> castEntry() {
        return (Class<Map.Entry<Fluid, FluidRecipe>>) (Class<?>) Map.Entry.class;
    }

    public static List<Map.Entry<Fluid, FluidRecipe>> fromRecipes() {
        return new ArrayList<>(ElectrolyserRecipes.getAllFluidRecipes().entrySet());
    }

    @Override
    public RecipeType<Map.Entry<Fluid, FluidRecipe>> getRecipeType() {
        return RECIPE_TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("container.hbm_m.electrolyser");
    }

    @Override
    protected int getInputCount(Map.Entry<Fluid, FluidRecipe> recipe) {
        return 1;
    }

    @Override
    protected int getOutputCount(Map.Entry<Fluid, FluidRecipe> recipe) {
        FluidRecipe r = recipe.getValue();
        int count = 0;
        if (r.fillA() > 0) count++;
        if (r.fillB() > 0) count++;
        count += r.byproducts().length;
        return count;
    }

    @Override
    protected boolean hasBlueprintTemplate(Map.Entry<Fluid, FluidRecipe> recipe) {
        return false;
    }

    @Override
    protected void addInputSlots(IRecipeLayoutBuilder builder, Map.Entry<Fluid, FluidRecipe> recipe, int inputXOffset) {
        addFluidSlot(builder, RecipeIngredientRole.INPUT, inputXOffset, 22,
                FluidStack.create(recipe.getKey(), recipe.getValue().amount()));
    }

    @Override
    protected void addOutputSlots(IRecipeLayoutBuilder builder, Map.Entry<Fluid, FluidRecipe> recipe, int outputXOffset) {
        FluidRecipe r = recipe.getValue();
        int outputCount = getOutputCount(recipe);
        int[][] positions = JeiNeiLayout.getGenericOutputSlotPositions(outputCount);
        int slotIndex = 0;

        if (r.fillA() > 0) {
            addFluidSlot(builder, RecipeIngredientRole.OUTPUT, positions[slotIndex][0] + outputXOffset, positions[slotIndex][1],
                    FluidStack.create(r.outA(), r.fillA()));
            slotIndex++;
        }
        if (r.fillB() > 0) {
            addFluidSlot(builder, RecipeIngredientRole.OUTPUT, positions[slotIndex][0] + outputXOffset, positions[slotIndex][1],
                    FluidStack.create(r.outB(), r.fillB()));
            slotIndex++;
        }
        for (ItemStack byproduct : r.byproducts()) {
            addItemSlot(builder, RecipeIngredientRole.OUTPUT, positions[slotIndex][0] + outputXOffset, positions[slotIndex][1])
                    .addItemStack(byproduct);
            slotIndex++;
        }
    }

    private void addFluidSlot(IRecipeLayoutBuilder builder, RecipeIngredientRole role, int x, int y, FluidStack fluid) {
        addItemSlot(builder, role, x, y)
                .setFluidRenderer(FLUID_RENDERER_CAPACITY, false, 16, 16)
                .setCustomRenderer(ForgeTypes.FLUID_STACK, new HbmFluidJeiRenderer(16, 16))
                .addIngredient(ForgeTypes.FLUID_STACK, FluidStackHooksForge.toForge(fluid));
    }

    @Override
    protected void addBlueprintSlot(IRecipeLayoutBuilder builder, Map.Entry<Fluid, FluidRecipe> recipe, int machineXOffset) {
        // Kein Blueprint-Slot fuer Electrolyser-Fluid-Rezepte.
    }
}
//?} else {
/*public final class ElectrolyserFluidJeiCategory {
    private ElectrolyserFluidJeiCategory() {}
}*///?}
