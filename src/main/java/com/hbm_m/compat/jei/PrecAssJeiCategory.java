package com.hbm_m.compat.jei;

import java.util.ArrayList;
import java.util.List;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.item.ModItems;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.recipe.PrecAssRecipe;
import com.hbm_m.recipe.PrecAssRecipe.CountedIngredient;
import com.hbm_m.recipe.PrecAssRecipe.FluidIngredient;

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
 * JEI port of {@code PrecAssRecipeHandler} (extends {@code NEIGenericRecipeHandler}).
 */
//? if forge {
public class PrecAssJeiCategory extends JeiGenericRecipeCategory<PrecAssRecipe> {

    public static final RecipeType<PrecAssRecipe> RECIPE_TYPE =
            RecipeType.create(RefStrings.MODID, "precass", PrecAssRecipe.class);

    private static final int FLUID_RENDERER_CAPACITY = 4_000;

    public PrecAssJeiCategory(IGuiHelper guiHelper) {
        super(guiHelper, new ItemStack[]{
                new ItemStack(ModBlocks.MACHINE_PRECASS.get()),
        });
    }

    @Override
    public RecipeType<PrecAssRecipe> getRecipeType() {
        return RECIPE_TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("container.hbm_m.machine_precass");
    }

    @Override
    protected int getInputCount(PrecAssRecipe recipe) {
        return countItemInputs(recipe) + countFluidInputs(recipe);
    }

    @Override
    protected int getOutputCount(PrecAssRecipe recipe) {
        return countItemOutputs(recipe) + countFluidOutputs(recipe);
    }

    @Override
    protected boolean hasBlueprintTemplate(PrecAssRecipe recipe) {
        return recipe.isPooled();
    }

    @Override
    protected int getInputXOffset(PrecAssRecipe recipe, int inputCount) {
        if (inputCount > 12) return -9;
        if (inputCount > 9) return 18;
        return 0;
    }

    @Override
    protected int getOutputXOffset(PrecAssRecipe recipe, int outputCount) {
        return getOffset(getInputCount(recipe));
    }

    @Override
    protected int getMachineXOffset(PrecAssRecipe recipe) {
        return getOffset(getInputCount(recipe));
    }

    private static int getOffset(int inputCount) {
        if (inputCount > 12) return 27;
        if (inputCount > 9) return 18;
        return 0;
    }

    @Override
    protected void addInputSlots(IRecipeLayoutBuilder builder, PrecAssRecipe recipe, int inputXOffset) {
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
    protected void addOutputSlots(IRecipeLayoutBuilder builder, PrecAssRecipe recipe, int outputXOffset) {
        int outputCount = getOutputCount(recipe);
        int[][] positions = JeiNeiLayout.getGenericOutputSlotPositions(outputCount);
        int slotIndex = 0;

        for (PrecAssRecipe.OutputGroup group : recipe.getItemOutputs()) {
            if (group.entries().isEmpty()) {
                continue;
            }
            IRecipeSlotBuilder slot = addItemSlot(builder, RecipeIngredientRole.OUTPUT,
                    positions[slotIndex][0] + outputXOffset, positions[slotIndex][1]);
            List<Component> chances = new ArrayList<>();
            int total = 0;
            for (var e : group.entries()) total += e.weight();
            for (var e : group.entries()) {
                slot.addItemStack(e.stack());
                float chance = group.entries().size() == 1 ? group.chance() : (total > 0 ? (float) e.weight() / total : 0F);
                if (chance < 1F) chances.add(Component.literal(e.stack().getCount() + "x ").append(e.stack().getHoverName())
                        .append(": " + ((int) (chance * 1000)) / 10.0D + "%").withStyle(net.minecraft.ChatFormatting.RED));
            }
            if (!chances.isEmpty()) slot.addRichTooltipCallback((view, tooltip) -> { for (Component c : chances) tooltip.add(c); });
            slotIndex++;
        }

        for (FluidStack fluid : toFluidStacks(recipe.getFluidOutputs())) {
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
    protected void addBlueprintSlot(IRecipeLayoutBuilder builder, PrecAssRecipe recipe, int machineXOffset) {
        if (!recipe.isPooled()) {
            return;
        }

        ItemStack folder = com.hbm_m.item.industrial.ItemBlueprints.make(recipe.getBlueprintPool());
        addUnframedSlot(builder, RecipeIngredientRole.RENDER_ONLY, 75 + machineXOffset, 10)
                .addItemStack(folder);
    }

    @Override
    protected void drawRecipeExtras(PrecAssRecipe recipe, GuiGraphics graphics) {
        JeiNeiRendering.drawGenericRecipeExtras(graphics, recipe.getDuration(), (int) Math.min(Integer.MAX_VALUE, recipe.getPower()));
    }

    private static int countItemInputs(PrecAssRecipe recipe) {
        int count = 0;
        for (CountedIngredient input : recipe.getItemInputs()) {
            if (!input.ingredient().isEmpty()) {
                count++;
            }
        }
        return count;
    }

    private static int countFluidInputs(PrecAssRecipe recipe) {
        int count = 0;
        for (FluidIngredient fluidInput : recipe.getFluidInputs()) {
            if (!toFluidStack(fluidInput).isEmpty()) {
                count++;
            }
        }
        return count;
    }

    private static int countItemOutputs(PrecAssRecipe recipe) {
        int count = 0;
        for (PrecAssRecipe.OutputGroup group : recipe.getItemOutputs()) {
            if (!group.entries().isEmpty()) {
                count++;
            }
        }
        return count;
    }

    private static int countFluidOutputs(PrecAssRecipe recipe) {
        int count = 0;
        for (FluidStack fluid : toFluidStacks(recipe.getFluidOutputs())) {
            if (!fluid.isEmpty()) {
                count++;
            }
        }
        return count;
    }

    private static List<FluidStack> toFluidStacks(List<FluidIngredient> list) {
        List<FluidStack> out = new ArrayList<>();
        for (FluidIngredient f : list) out.add(toFluidStack(f));
        return out;
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
/*public final class PrecAssJeiCategory {
    private PrecAssJeiCategory() {}
}*///?}
