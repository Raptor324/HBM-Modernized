package com.hbm_m.compat.jei;

import java.util.ArrayList;
import java.util.List;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.item.ModItems;
import com.hbm_m.item.industrial.ItemBlueprintFolder;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.recipe.PurexRecipe;
import com.hbm_m.recipe.PurexRecipe.CountedIngredient;
import com.hbm_m.recipe.PurexRecipe.FluidIngredient;

import dev.architectury.fluid.FluidStack;
import dev.architectury.hooks.fluid.forge.FluidStackHooksForge;
import mezz.jei.api.forge.ForgeTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

/**
 * JEI port of {@code PurexRecipeHandler} (extends {@code NEIGenericRecipeHandler}).
 */
//? if forge {
public class PurexJeiCategory extends JeiGenericRecipeCategory<PurexRecipe> {

    public static final RecipeType<PurexRecipe> RECIPE_TYPE =
            RecipeType.create(RefStrings.MODID, "purex", PurexRecipe.class);

    private static final int FLUID_RENDERER_CAPACITY = 24_000;

    public PurexJeiCategory(IGuiHelper guiHelper) {
        super(guiHelper, new ItemStack[]{
                new ItemStack(ModBlocks.PUREX.get()),
        });
    }

    @Override
    public RecipeType<PurexRecipe> getRecipeType() {
        return RECIPE_TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("container.hbm_m.purex");
    }

    @Override
    protected int getInputCount(PurexRecipe recipe) {
        return countItemInputs(recipe) + countFluidInputs(recipe);
    }

    @Override
    protected int getOutputCount(PurexRecipe recipe) {
        return countItemOutputs(recipe) + countFluidOutputs(recipe);
    }

    @Override
    protected boolean hasBlueprintTemplate(PurexRecipe recipe) {
        return recipe.requiresBlueprint();
    }

    @Override
    protected int getInputXOffset(PurexRecipe recipe, int inputCount) {
        if (inputCount > 12) return -9;
        if (inputCount > 9) return 18;
        return 0;
    }

    @Override
    protected int getOutputXOffset(PurexRecipe recipe, int outputCount) {
        return getOffset(getInputCount(recipe));
    }

    @Override
    protected int getMachineXOffset(PurexRecipe recipe) {
        return getOffset(getInputCount(recipe));
    }

    private static int getOffset(int inputCount) {
        if (inputCount > 12) return 27;
        if (inputCount > 9) return 18;
        return 0;
    }

    @Override
    protected void addInputSlots(IRecipeLayoutBuilder builder, PurexRecipe recipe, int inputXOffset) {
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

        for (FluidIngredient fluidInput : recipe.getFluidInputs()) {
            FluidStack fluid = toFluidStack(fluidInput);
            if (fluid.isEmpty()) {
                continue;
            }
            addItemSlot(builder, RecipeIngredientRole.INPUT,
                    positions[slotIndex][0] + inputXOffset, positions[slotIndex][1])
                    .setFluidRenderer(FLUID_RENDERER_CAPACITY, false, 16, 16)
                    .setCustomRenderer(ForgeTypes.FLUID_STACK, new HbmFluidJeiRenderer(16, 16))
                    .addIngredient(ForgeTypes.FLUID_STACK, FluidStackHooksForge.toForge(fluid));
            slotIndex++;
        }
    }

    @Override
    protected void addOutputSlots(IRecipeLayoutBuilder builder, PurexRecipe recipe, int outputXOffset) {
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
            addItemSlot(builder, RecipeIngredientRole.OUTPUT,
                    positions[slotIndex][0] + outputXOffset, positions[slotIndex][1])
                    .setFluidRenderer(FLUID_RENDERER_CAPACITY, false, 16, 16)
                    .setCustomRenderer(ForgeTypes.FLUID_STACK, new HbmFluidJeiRenderer(16, 16))
                    .addIngredient(ForgeTypes.FLUID_STACK, FluidStackHooksForge.toForge(fluid));
            slotIndex++;
        }
    }

    @Override
    protected void addBlueprintSlot(IRecipeLayoutBuilder builder, PurexRecipe recipe, int machineXOffset) {
        if (!recipe.requiresBlueprint()) {
            return;
        }

        ItemStack folder = new ItemStack(ModItems.BLUEPRINT_FOLDER.get());
        ItemBlueprintFolder.writeBlueprintPool(folder, recipe.getBlueprintPool());
        addUnframedSlot(builder, RecipeIngredientRole.RENDER_ONLY, 75 + machineXOffset, 10)
                .addItemStack(folder);
    }

    @Override
    protected void drawRecipeExtras(PurexRecipe recipe, GuiGraphics graphics) {
        JeiNeiRendering.drawGenericRecipeExtras(graphics, recipe.getDuration(), recipe.getPowerConsumption());
    }

    private static int countItemInputs(PurexRecipe recipe) {
        int count = 0;
        for (CountedIngredient input : recipe.getItemInputs()) {
            if (!input.ingredient().isEmpty()) {
                count++;
            }
        }
        return count;
    }

    private static int countFluidInputs(PurexRecipe recipe) {
        int count = 0;
        for (FluidIngredient fluidInput : recipe.getFluidInputs()) {
            if (!toFluidStack(fluidInput).isEmpty()) {
                count++;
            }
        }
        return count;
    }

    private static int countItemOutputs(PurexRecipe recipe) {
        int count = 0;
        for (ItemStack output : recipe.getItemOutputs()) {
            if (!output.isEmpty()) {
                count++;
            }
        }
        return count;
    }

    private static int countFluidOutputs(PurexRecipe recipe) {
        int count = 0;
        for (FluidStack fluid : recipe.getFluidOutputs()) {
            if (!fluid.isEmpty()) {
                count++;
            }
        }
        return count;
    }

    private static FluidStack toFluidStack(FluidIngredient fluidInput) {
        Fluid fluid = BuiltInRegistries.FLUID.get(fluidInput.fluidId());
        if (fluid == null || fluid == Fluids.EMPTY) {
            return FluidStack.empty();
        }
        return FluidStack.create(fluid, fluidInput.amount());
    }
}
//?} else {
/*public final class PurexJeiCategory {
    private PurexJeiCategory() {}
}*///?}
