package com.hbm_m.compat.jei;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.recipe.GasCentrifugeRecipe;

import dev.architectury.fluid.FluidStack;
//? if forge {
import mezz.jei.api.forge.ForgeTypes;
//?} elif neoforge {
/*import mezz.jei.api.neoforge.NeoForgeTypes;
*///?}
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.RecipeType;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * JEI-категория газового центрифуга ({@code hbm_m:gas_centrifuge}).
 *
 * <p>Работает напрямую с data-driven {@link GasCentrifugeRecipe} (JSON, JEI-only) — без внутреннего
 * {@code record Recipe} и {@code getDefaultRecipes()}. 4 канонических каскад-результата (UF6/PUF6/WATZ)
 * теперь живут в JSON (см. {@code GasCentrifugeRecipeGenerator}). Runtime по-прежнему работает на
 * cascade-enrichment через {@code PseudoFluidType} — эти рецепты только для JEI.</p>
 *
 * <p>Static JEI display of the 1.7.10 Gas Centrifuge's four canonical cascade outcomes (the original
 * NEI handler showed a fully-cascaded result rather than the per-tick enrichment logic).</p>
 */

//? if forge {
@net.minecraftforge.api.distmarker.OnlyIn(net.minecraftforge.api.distmarker.Dist.CLIENT)
//?} elif fabric {
/*@net.fabricmc.api.Environment(net.fabricmc.api.EnvType.CLIENT)
*///?} elif neoforge {
/*@net.neoforged.api.distmarker.OnlyIn(net.neoforged.api.distmarker.Dist.CLIENT)
*///?}
public class GasCentrifugeJeiCategory extends JeiGenericRecipeCategory<GasCentrifugeRecipe> {

    public static final RecipeType<GasCentrifugeRecipe> RECIPE_TYPE =
            RecipeType.create(RefStrings.MODID, "gas_centrifuge", GasCentrifugeRecipe.class);

    private static final int FLUID_RENDERER_CAPACITY = 2000;

    public GasCentrifugeJeiCategory(IGuiHelper guiHelper) {
        super(guiHelper, new ItemStack[]{ new ItemStack(ModBlocks.GAS_CENTRIFUGE.get()) });
    }

    @Override
    public RecipeType<GasCentrifugeRecipe> getRecipeType() {
        return RECIPE_TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("container.hbm_m.gas_centrifuge");
    }

    @Override
    protected int getInputCount(GasCentrifugeRecipe recipe) {
        return 1;
    }

    @Override
    protected int getOutputCount(GasCentrifugeRecipe recipe) {
        int count = 0;
        for (ItemStack stack : recipe.getOutputs()) {
            if (!stack.isEmpty()) count++;
        }
        return count;
    }

    @Override
    protected boolean hasBlueprintTemplate(GasCentrifugeRecipe recipe) {
        return false;
    }

    @Override
    protected void addInputSlots(IRecipeLayoutBuilder builder, GasCentrifugeRecipe recipe, int inputXOffset) {
        int[][] positions = JeiNeiLayout.getGenericInputSlotPositions(1);
        FluidStack fluid = recipe.getInput();
        //? if forge {
        addItemSlot(builder, mezz.jei.api.recipe.RecipeIngredientRole.INPUT,
                positions[0][0] + inputXOffset, positions[0][1])
                .setFluidRenderer(FLUID_RENDERER_CAPACITY, false, 16, 16)
                .setCustomRenderer(ForgeTypes.FLUID_STACK, new HbmFluidJeiRenderer(16, 16))
                .addIngredient(ForgeTypes.FLUID_STACK, new net.minecraftforge.fluids.FluidStack(fluid.getFluid(), (int) fluid.getAmount(), fluid.getTag()));
        //?} elif neoforge {
        /*addItemSlot(builder, mezz.jei.api.recipe.RecipeIngredientRole.INPUT,
                positions[0][0] + inputXOffset, positions[0][1])
                .setFluidRenderer(FLUID_RENDERER_CAPACITY, false, 16, 16)
                .setCustomRenderer(NeoForgeTypes.FLUID_STACK, new HbmFluidJeiRenderer(16, 16))
                .addIngredient(NeoForgeTypes.FLUID_STACK, new net.neoforged.neoforge.fluids.FluidStack(fluid.getFluid(), (int) fluid.getAmount()));
        *///?}
    }

    @Override
    protected void addOutputSlots(IRecipeLayoutBuilder builder, GasCentrifugeRecipe recipe, int outputXOffset) {
        int outputCount = getOutputCount(recipe);
        int[][] positions = JeiNeiLayout.getGenericOutputSlotPositions(outputCount);
        int slotIndex = 0;

        for (ItemStack output : recipe.getOutputs()) {
            if (output.isEmpty()) continue;
            addItemSlot(builder, mezz.jei.api.recipe.RecipeIngredientRole.OUTPUT,
                    positions[slotIndex][0] + outputXOffset, positions[slotIndex][1])
                    .addItemStack(output);
            slotIndex++;
        }
    }

    @Override
    protected void addBlueprintSlot(IRecipeLayoutBuilder builder, GasCentrifugeRecipe recipe, int machineXOffset) {
    }

    @Override
    protected void drawRecipeExtras(GasCentrifugeRecipe recipe, GuiGraphics graphics) {
        Component info = recipe.isHighSpeed()
                ? Component.translatable("jei.hbm_m.gas_centrifuge.info_high_speed", recipe.getCentrifugeCount())
                : Component.translatable("jei.hbm_m.gas_centrifuge.info", recipe.getCentrifugeCount());
        graphics.drawString(net.minecraft.client.Minecraft.getInstance().font, info, 0, 60, 0x404040, false);
    }
}