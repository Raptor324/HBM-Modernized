package com.hbm_m.compat.jei;

import java.util.ArrayList;
import java.util.List;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.item.ModItems;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.recipe.RockMillRecipe;
import com.hbm_m.recipe.RockMillRecipe.CountedIngredient;
import com.hbm_m.recipe.RockMillRecipe.FluidIngredient;

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
 * JEI port of {@code RockMillRecipeHandler} (extends {@code NEIGenericRecipeHandler}).
 */
//? if forge {
public class RockMillJeiCategory extends JeiGenericRecipeCategory<RockMillRecipe> {

    public static final RecipeType<RockMillRecipe> RECIPE_TYPE =
            RecipeType.create(RefStrings.MODID, "rockmill", RockMillRecipe.class);

    private static final int FLUID_RENDERER_CAPACITY = 2_500;

    public RockMillJeiCategory(IGuiHelper guiHelper) {
        super(guiHelper, new ItemStack[]{
                new ItemStack(ModBlocks.MACHINE_ROCKMILL.get()),
        });
    }

    @Override
    public RecipeType<RockMillRecipe> getRecipeType() {
        return RECIPE_TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("container.hbm_m.machine_rockmill");
    }

    @Override
    protected int getInputCount(RockMillRecipe recipe) {
        return countItemInputs(recipe) + countFluidInputs(recipe);
    }

    @Override
    protected int getOutputCount(RockMillRecipe recipe) {
        return countItemOutputs(recipe) + countFluidOutputs(recipe);
    }

    @Override
    protected boolean hasBlueprintTemplate(RockMillRecipe recipe) {
        return recipe.requiresBlueprint();
    }

    @Override
    protected int getInputXOffset(RockMillRecipe recipe, int inputCount) {
        if (inputCount > 12) return -9;
        if (inputCount > 9) return 18;
        return 0;
    }

    @Override
    protected int getOutputXOffset(RockMillRecipe recipe, int outputCount) {
        return getOffset(getInputCount(recipe));
    }

    @Override
    protected int getMachineXOffset(RockMillRecipe recipe) {
        return getOffset(getInputCount(recipe));
    }

    private static int getOffset(int inputCount) {
        if (inputCount > 12) return 27;
        if (inputCount > 9) return 18;
        return 0;
    }

    @Override
    protected void addInputSlots(IRecipeLayoutBuilder builder, RockMillRecipe recipe, int inputXOffset) {
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
                    //? if forge {
                    .setCustomRenderer(ForgeTypes.FLUID_STACK, new HbmFluidJeiRenderer(16, 16))
                    .addIngredient(ForgeTypes.FLUID_STACK, FluidStackHooksForge.toForge(fluid));
                    //?} elif neoforge {
                    /*.setCustomRenderer(NeoForgeTypes.FLUID_STACK, new HbmFluidJeiRenderer(16, 16))
                    .addIngredient(NeoForgeTypes.FLUID_STACK, new net.neoforged.neoforge.fluids.FluidStack(fluid.getFluid(), (int) fluid.getAmount()));
                    *///?}
            slotIndex++;
        }
    }

    @Override
    protected void addOutputSlots(IRecipeLayoutBuilder builder, RockMillRecipe recipe, int outputXOffset) {
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

        for (var group : recipe.getChanceOutputs()) {
            java.util.List<ItemStack> variants = new java.util.ArrayList<>();
            for (var w : group) if (!w.stack().isEmpty()) variants.add(w.stack());
            if (variants.isEmpty()) continue;
            addItemSlot(builder, RecipeIngredientRole.OUTPUT,
                    positions[slotIndex][0] + outputXOffset, positions[slotIndex][1])
                    .addItemStacks(variants);
            slotIndex++;
        }

        for (FluidStack fluid : recipe.getFluidOutputs()) {
            if (fluid.isEmpty()) {
                continue;
            }
            addItemSlot(builder, RecipeIngredientRole.OUTPUT,
                    positions[slotIndex][0] + outputXOffset, positions[slotIndex][1])
                    .setFluidRenderer(FLUID_RENDERER_CAPACITY, false, 16, 16)
                    //? if forge {
                    .setCustomRenderer(ForgeTypes.FLUID_STACK, new HbmFluidJeiRenderer(16, 16))
                    .addIngredient(ForgeTypes.FLUID_STACK, FluidStackHooksForge.toForge(fluid));
                    //?} elif neoforge {
                    /*.setCustomRenderer(NeoForgeTypes.FLUID_STACK, new HbmFluidJeiRenderer(16, 16))
                    .addIngredient(NeoForgeTypes.FLUID_STACK, new net.neoforged.neoforge.fluids.FluidStack(fluid.getFluid(), (int) fluid.getAmount()));
                    *///?}
            slotIndex++;
        }
    }

    @Override
    protected void addBlueprintSlot(IRecipeLayoutBuilder builder, RockMillRecipe recipe, int machineXOffset) {
        if (!recipe.requiresBlueprint()) {
            return;
        }

        ItemStack folder = com.hbm_m.item.industrial.ItemBlueprints.make(recipe.getBlueprintPool());
        addUnframedSlot(builder, RecipeIngredientRole.RENDER_ONLY, 75 + machineXOffset, 10)
                .addItemStack(folder);
    }

    @Override
    protected void drawRecipeExtras(RockMillRecipe recipe, GuiGraphics graphics) {
        JeiNeiRendering.drawGenericRecipeExtras(graphics, recipe.getDuration(), recipe.getPowerConsumption());
    }

    private static int countItemInputs(RockMillRecipe recipe) {
        int count = 0;
        for (CountedIngredient input : recipe.getItemInputs()) {
            if (!input.ingredient().isEmpty()) {
                count++;
            }
        }
        return count;
    }

    private static int countFluidInputs(RockMillRecipe recipe) {
        int count = 0;
        for (FluidIngredient fluidInput : recipe.getFluidInputs()) {
            if (!toFluidStack(fluidInput).isEmpty()) {
                count++;
            }
        }
        return count;
    }

    private static int countItemOutputs(RockMillRecipe recipe) {
        int count = 0;
        for (var group : recipe.getChanceOutputs()) if (!group.isEmpty()) count++;
        for (ItemStack output : recipe.getItemOutputs()) {
            if (!output.isEmpty()) {
                count++;
            }
        }
        return count;
    }

    private static int countFluidOutputs(RockMillRecipe recipe) {
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
/*public final class RockMillJeiCategory {
    private RockMillJeiCategory() {}
}*///?}
