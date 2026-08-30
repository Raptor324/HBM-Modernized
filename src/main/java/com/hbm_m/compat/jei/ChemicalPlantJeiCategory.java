package com.hbm_m.compat.jei;

import java.util.ArrayList;
import java.util.List;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.item.ModItems;
import com.hbm_m.item.industrial.ItemBlueprintFolder;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.recipe.ChemicalPlantRecipe;
import com.hbm_m.recipe.ChemicalPlantRecipe.CountedIngredient;

import dev.architectury.fluid.FluidStack;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * JEI port of ChemicalPlantRecipeHandler.
 */

//? if forge {
@net.minecraftforge.api.distmarker.OnlyIn(net.minecraftforge.api.distmarker.Dist.CLIENT)
//?} elif fabric {
/*@net.fabricmc.api.Environment(net.fabricmc.api.EnvType.CLIENT)
*///?} elif neoforge {
/*@net.neoforged.api.distmarker.OnlyIn(net.neoforged.api.distmarker.Dist.CLIENT)
*///?}
public class ChemicalPlantJeiCategory extends JeiGenericRecipeCategory<ChemicalPlantRecipe> {

    public static final RecipeType<ChemicalPlantRecipe> RECIPE_TYPE =
            RecipeType.create(RefStrings.MODID, "chemical_plant", ChemicalPlantRecipe.class);

    private static final int FLUID_RENDERER_CAPACITY = 24_000;

    public ChemicalPlantJeiCategory(IGuiHelper guiHelper) {
        super(guiHelper, new ItemStack[]{
                new ItemStack(ModBlocks.CHEMICAL_PLANT.get()),
                new ItemStack(ModBlocks.CHEMICAL_FACTORY.get())
        });
    }

    @Override
    public RecipeType<ChemicalPlantRecipe> getRecipeType() {
        return RECIPE_TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("container.hbm_m.chemical_plant");
    }

    @Override
    protected int getInputCount(ChemicalPlantRecipe recipe) {
        return countItemInputs(recipe) + countFluidInputs(recipe);
    }

    @Override
    protected int getOutputCount(ChemicalPlantRecipe recipe) {
        return countItemOutputs(recipe) + countFluidOutputs(recipe);
    }

    @Override
    protected boolean hasBlueprintTemplate(ChemicalPlantRecipe recipe) {
        return recipe.requiresBlueprint();
    }

    @Override
    protected int getInputXOffset(ChemicalPlantRecipe recipe, int inputCount) {
        if (inputCount > 12) return -9;
        if (inputCount > 9) return 18;
        return 0;
    }

    @Override
    protected int getOutputXOffset(ChemicalPlantRecipe recipe, int outputCount) {
        return getOffset(getInputCount(recipe));
    }

    @Override
    protected int getMachineXOffset(ChemicalPlantRecipe recipe) {
        return getOffset(getInputCount(recipe));
    }

    private static int getOffset(int inputCount) {
        if (inputCount > 12) return 27;
        if (inputCount > 9) return 18;
        return 0;
    }

    @Override
    protected void addInputSlots(IRecipeLayoutBuilder builder, ChemicalPlantRecipe recipe, int inputXOffset) {
        int inputCount = getInputCount(recipe);
        int[][] positions = JeiNeiLayout.getGenericInputSlotPositions(inputCount);
        int slotIndex = 0;

        for (CountedIngredient input : recipe.getItemInputs()) {
            if (input.ingredient().isEmpty()) {
                continue;
            }
            IRecipeSlotBuilder jeiSlot = addItemSlot(builder, RecipeIngredientRole.INPUT,
                    positions[slotIndex][0] + inputXOffset, positions[slotIndex][1]);
            JeiIngredientSlots.addCountedIngredient(jeiSlot, input.ingredient(), input.count());
            slotIndex++;
        }

        for (FluidStack fluid : recipe.getFluidInputs()) {
            if (fluid.isEmpty()) {
                continue;
            }
            //? if forge {
            addItemSlot(builder, RecipeIngredientRole.INPUT,
                    positions[slotIndex][0] + inputXOffset, positions[slotIndex][1])
                    .setFluidRenderer(FLUID_RENDERER_CAPACITY, false, 16, 16)
                    .setCustomRenderer(mezz.jei.api.forge.ForgeTypes.FLUID_STACK, new HbmFluidJeiRenderer(16, 16))
                    .addIngredient(mezz.jei.api.forge.ForgeTypes.FLUID_STACK, new net.minecraftforge.fluids.FluidStack(fluid.getFluid(), (int) fluid.getAmount(), fluid.getTag()));
            //?} elif neoforge {
            /*addItemSlot(builder, RecipeIngredientRole.INPUT,
                    positions[slotIndex][0] + inputXOffset, positions[slotIndex][1])
                    .setFluidRenderer(FLUID_RENDERER_CAPACITY, false, 16, 16)
                    .setCustomRenderer(mezz.jei.api.neoforge.NeoForgeTypes.FLUID_STACK, new HbmFluidJeiRenderer(16, 16))
                    .addIngredient(mezz.jei.api.neoforge.NeoForgeTypes.FLUID_STACK, new net.neoforged.neoforge.fluids.FluidStack(fluid.getFluid(), (int) fluid.getAmount()));
            *///?}
            slotIndex++;
        }
    }

    @Override
    protected void addOutputSlots(IRecipeLayoutBuilder builder, ChemicalPlantRecipe recipe, int outputXOffset) {
        int outputCount = getOutputCount(recipe);
        int[][] positions = JeiNeiLayout.getGenericOutputSlotPositions(outputCount);
        int slotIndex = 0;

        for (ItemStack output : recipe.getItemOutputs()) {
            if (output.isEmpty()) {
                continue;
            }
            addItemSlot(builder, RecipeIngredientRole.OUTPUT,
                    positions[slotIndex][0] + outputXOffset, positions[slotIndex][1])
                    .addItemStack(output);
            slotIndex++;
        }

        for (FluidStack fluid : recipe.getFluidOutputs()) {
            if (fluid.isEmpty()) {
                continue;
            }
            //? if forge {
            addItemSlot(builder, RecipeIngredientRole.OUTPUT,
                    positions[slotIndex][0] + outputXOffset, positions[slotIndex][1])
                    .setFluidRenderer(FLUID_RENDERER_CAPACITY, false, 16, 16)
                    .setCustomRenderer(mezz.jei.api.forge.ForgeTypes.FLUID_STACK, new HbmFluidJeiRenderer(16, 16))
                    .addIngredient(mezz.jei.api.forge.ForgeTypes.FLUID_STACK, new net.minecraftforge.fluids.FluidStack(fluid.getFluid(), (int) fluid.getAmount(), fluid.getTag()));
            //?} elif neoforge {
            /*addItemSlot(builder, RecipeIngredientRole.OUTPUT,
                    positions[slotIndex][0] + outputXOffset, positions[slotIndex][1])
                    .setFluidRenderer(FLUID_RENDERER_CAPACITY, false, 16, 16)
                    .setCustomRenderer(mezz.jei.api.neoforge.NeoForgeTypes.FLUID_STACK, new HbmFluidJeiRenderer(16, 16))
                    .addIngredient(mezz.jei.api.neoforge.NeoForgeTypes.FLUID_STACK, new net.neoforged.neoforge.fluids.FluidStack(fluid.getFluid(), (int) fluid.getAmount()));
            *///?}
            slotIndex++;
        }
    }

    @Override
    protected void addBlueprintSlot(IRecipeLayoutBuilder builder, ChemicalPlantRecipe recipe, int machineXOffset) {
        if (!recipe.requiresBlueprint()) {
            return;
        }

        ItemStack folder = new ItemStack(ModItems.BLUEPRINT_FOLDER.get());
        ItemBlueprintFolder.writeBlueprintPool(folder, recipe.getBlueprintPool());
        addUnframedSlot(builder, RecipeIngredientRole.RENDER_ONLY, 75 + machineXOffset, 10)
                .addItemStack(folder);
    }

    @Override
    protected void drawRecipeExtras(ChemicalPlantRecipe recipe, GuiGraphics graphics) {
        JeiNeiRendering.drawGenericRecipeExtras(graphics, recipe.getDuration(), recipe.getPowerConsumption());
    }

    private static int countItemInputs(ChemicalPlantRecipe recipe) {
        int count = 0;
        for (CountedIngredient input : recipe.getItemInputs()) {
            if (!input.ingredient().isEmpty()) {
                count++;
            }
        }
        return count;
    }

    private static int countFluidInputs(ChemicalPlantRecipe recipe) {
        int count = 0;
        for (FluidStack fluid : recipe.getFluidInputs()) {
            if (!fluid.isEmpty()) {
                count++;
            }
        }
        return count;
    }

    private static int countItemOutputs(ChemicalPlantRecipe recipe) {
        int count = 0;
        for (ItemStack output : recipe.getItemOutputs()) {
            if (!output.isEmpty()) {
                count++;
            }
        }
        return count;
    }

    private static int countFluidOutputs(ChemicalPlantRecipe recipe) {
        int count = 0;
        for (FluidStack fluid : recipe.getFluidOutputs()) {
            if (!fluid.isEmpty()) {
                count++;
            }
        }
        return count;
    }
}