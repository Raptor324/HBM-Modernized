package com.hbm_m.compat.jei;

import java.util.ArrayList;
import java.util.List;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.lib.RefStrings;

import dev.architectury.fluid.FluidStack;
import dev.architectury.hooks.fluid.forge.FluidStackHooksForge;
import mezz.jei.api.forge.ForgeTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;

/**
 * Static JEI display of the 1.7.10 Gas Centrifuge's four canonical cascade outcomes (the original
 * NEI handler showed a fully-cascaded result rather than the per-tick enrichment logic).
 */
//? if forge {
public class GasCentrifugeJeiCategory extends JeiGenericRecipeCategory<GasCentrifugeJeiCategory.Recipe> {

    public static final RecipeType<Recipe> RECIPE_TYPE =
            RecipeType.create(RefStrings.MODID, "gas_centrifuge", Recipe.class);

    private static final int FLUID_RENDERER_CAPACITY = 2000;

    public GasCentrifugeJeiCategory(IGuiHelper guiHelper) {
        super(guiHelper, new ItemStack[]{ new ItemStack(ModBlocks.GAS_CENTRIFUGE.get()) });
    }

    @Override
    public RecipeType<Recipe> getRecipeType() {
        return RECIPE_TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("container.hbm_m.gas_centrifuge");
    }

    @Override
    protected int getInputCount(Recipe recipe) {
        return 1;
    }

    @Override
    protected int getOutputCount(Recipe recipe) {
        int count = 0;
        for (ItemStack stack : recipe.outputs()) {
            if (!stack.isEmpty()) count++;
        }
        return count;
    }

    @Override
    protected boolean hasBlueprintTemplate(Recipe recipe) {
        return false;
    }

    @Override
    protected void addInputSlots(IRecipeLayoutBuilder builder, Recipe recipe, int inputXOffset) {
        int[][] positions = JeiNeiLayout.getGenericInputSlotPositions(1);
        FluidStack fluid = FluidStack.create(recipe.inputFluid(), recipe.inputAmount());
        addItemSlot(builder, RecipeIngredientRole.INPUT, positions[0][0] + inputXOffset, positions[0][1])
                .setFluidRenderer(FLUID_RENDERER_CAPACITY, false, 16, 16)
                .setCustomRenderer(ForgeTypes.FLUID_STACK, new HbmFluidJeiRenderer(16, 16))
                .addIngredient(ForgeTypes.FLUID_STACK, FluidStackHooksForge.toForge(fluid));
    }

    @Override
    protected void addOutputSlots(IRecipeLayoutBuilder builder, Recipe recipe, int outputXOffset) {
        int outputCount = getOutputCount(recipe);
        int[][] positions = JeiNeiLayout.getGenericOutputSlotPositions(outputCount);
        int slotIndex = 0;

        for (ItemStack output : recipe.outputs()) {
            if (output.isEmpty()) continue;
            addItemSlot(builder, RecipeIngredientRole.OUTPUT,
                    positions[slotIndex][0] + outputXOffset, positions[slotIndex][1])
                    .addItemStack(output);
            slotIndex++;
        }
    }

    @Override
    protected void addBlueprintSlot(IRecipeLayoutBuilder builder, Recipe recipe, int machineXOffset) {
    }

    @Override
    protected void drawRecipeExtras(Recipe recipe, GuiGraphics graphics) {
        Component info = recipe.highSpeed()
                ? Component.translatable("jei.hbm_m.gas_centrifuge.info_high_speed", recipe.centrifugeCount())
                : Component.translatable("jei.hbm_m.gas_centrifuge.info", recipe.centrifugeCount());
        graphics.drawString(net.minecraft.client.Minecraft.getInstance().font, info, 0, 60, 0x404040, false);
    }

    public record Recipe(Fluid inputFluid, int inputAmount, ItemStack[] outputs, boolean highSpeed, int centrifugeCount) {
    }

    public static List<Recipe> getDefaultRecipes() {
        List<Recipe> recipes = new ArrayList<>();

        recipes.add(new Recipe(com.hbm_m.inventory.fluid.ModFluids.UF6.getSource(), 1200,
                new ItemStack[]{
                        new ItemStack(com.hbm_m.item.ModItems.NUGGET_U238.get(), 11),
                        new ItemStack(com.hbm_m.item.ModItems.NUGGET_U235.get(), 1),
                        new ItemStack(com.hbm_m.item.ModItems.FLUORITE.get(), 4)
                }, true, 4));

        recipes.add(new Recipe(com.hbm_m.inventory.fluid.ModFluids.UF6.getSource(), 1200,
                new ItemStack[]{
                        new ItemStack(com.hbm_m.item.ModItems.NUGGET_U238.get(), 6),
                        new ItemStack(com.hbm_m.item.ModItems.NUGGET_URANIUM_FUEL.get(), 6),
                        new ItemStack(com.hbm_m.item.ModItems.FLUORITE.get(), 4)
                }, false, 2));

        recipes.add(new Recipe(com.hbm_m.inventory.fluid.ModFluids.PUF6.getSource(), 900,
                new ItemStack[]{
                        new ItemStack(com.hbm_m.item.ModItems.NUGGET_PU238.get(), 3),
                        new ItemStack(com.hbm_m.item.ModItems.NUGGET_PU_MIX.get(), 6),
                        new ItemStack(com.hbm_m.item.ModItems.FLUORITE.get(), 3)
                }, false, 1));

        recipes.add(new Recipe(com.hbm_m.inventory.fluid.ModFluids.WATZ.getSource(), 1000,
                new ItemStack[]{
                        new ItemStack(com.hbm_m.item.ModItems.getPowders(com.hbm_m.item.tags_and_tiers.ModPowders.IRON).get(), 1),
                        new ItemStack(com.hbm_m.item.ModItems.getPowder(com.hbm_m.item.tags_and_tiers.ModIngots.LEAD).get(), 1),
                        new ItemStack(com.hbm_m.item.ModItems.NUCLEAR_WASTE_TINY.get(), 1),
                        new ItemStack(com.hbm_m.item.ModItems.DUST.get(), 2)
                }, false, 2));

        return recipes;
    }
}
//?} else {
/*public final class GasCentrifugeJeiCategory {
    private GasCentrifugeJeiCategory() {}
}*///?}
