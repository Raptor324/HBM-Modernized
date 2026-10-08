package com.hbm_m.compat.jei;
//? if forge {

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.inventory.fluid.ModFluids;
import com.hbm_m.inventory.recipes.ElectrolyserFluidRecipes;
import com.hbm_m.inventory.recipes.ElectrolyserFluidRecipes.ElectrolysisRecipe;
import com.hbm_m.inventory.recipes.ElectrolyserFluidRecipes.FluidOut;
import com.hbm_m.lib.RefStrings;

import mezz.jei.api.forge.ForgeTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;

/**
 * Original {@code ElectrolyserFluidRecipes.getRecipes()}: Eingangsfluid -> zwei Fluide (ausser NONE) plus Nebenprodukte.
 */
public class ElectrolyserFluidJeiCategory extends JeiUniversalRecipeCategory<ElectrolyserFluidJeiCategory.Entry> {

    public record Entry(Fluid input, ElectrolysisRecipe recipe) { }

    public static final RecipeType<Entry> RECIPE_TYPE = RecipeType.create(RefStrings.MODID, "electrolyser_fluid", Entry.class);

    public ElectrolyserFluidJeiCategory(IGuiHelper guiHelper) {
        super(guiHelper, new ItemStack[] { new ItemStack(ModBlocks.ELECTROLYSER.get()) });
    }

    public static List<Entry> recipes() {
        List<Entry> list = new ArrayList<>();
        for (Map.Entry<Fluid, ElectrolysisRecipe> e : ElectrolyserFluidRecipes.all().entrySet()) list.add(new Entry(e.getKey(), e.getValue()));
        return list;
    }

    @Override public RecipeType<Entry> getRecipeType() { return RECIPE_TYPE; }
    @Override public Component getTitle() { return Component.translatable("container.machineElectrolyser"); }

    private static List<FluidOut> fluidOutputs(ElectrolysisRecipe r) {
        List<FluidOut> list = new ArrayList<>();
        if (r.output1.type() != ModFluids.NONE.getSource()) list.add(r.output1);
        if (r.output2.type() != ModFluids.NONE.getSource()) list.add(r.output2);
        return list;
    }

    @Override protected int getInputCount(Entry e) { return 1; }
    @Override protected int getOutputCount(Entry e) { return fluidOutputs(e.recipe()).size() + e.recipe().byproduct.length; }
    @Override protected List<List<ItemStack>> getInputStacks(Entry e) { return List.of(); }

    @Override
    protected List<ItemStack> getOutputStacks(Entry e) {
        return List.of(e.recipe().byproduct);
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, Entry e, IFocusGroup focuses) {
        int[][] inPos = JeiNeiLayout.getUniversalInputCoords(1);
        List<FluidOut> fluids = fluidOutputs(e.recipe());
        int outCount = getOutputCount(e);
        int[][] outPos = JeiNeiLayout.getUniversalOutputCoords(outCount);

        builder.addSlot(RecipeIngredientRole.INPUT, inPos[0][0], inPos[0][1])
                .setBackground(itemSlotBackground, -1, -1)
                .setCustomRenderer(ForgeTypes.FLUID_STACK, new HbmFluidJeiRenderer(16, 16))
                .addFluidStack(e.input(), e.recipe().amount);

        int i = 0;
        for (FluidOut f : fluids) {
            builder.addSlot(RecipeIngredientRole.OUTPUT, outPos[i][0], outPos[i][1])
                    .setBackground(itemSlotBackground, -1, -1)
                    .setCustomRenderer(ForgeTypes.FLUID_STACK, new HbmFluidJeiRenderer(16, 16))
                    .addFluidStack(f.type(), f.fill());
            i++;
        }
        for (ItemStack stack : e.recipe().byproduct) {
            builder.addSlot(RecipeIngredientRole.OUTPUT, outPos[i][0], outPos[i][1])
                    .setBackground(itemSlotBackground, -1, -1)
                    .addItemStack(stack);
            i++;
        }

        builder.addSlot(RecipeIngredientRole.CATALYST, 75, 31)
                .addItemStacks(java.util.Arrays.asList(getMachines(e)));
    }
}
//?} else {
/*public final class ElectrolyserFluidJeiCategory {
    private ElectrolyserFluidJeiCategory() {}
}*///?}
