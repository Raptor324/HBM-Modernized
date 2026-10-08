package com.hbm_m.compat.jei;

import java.util.List;

import com.hbm_m.item.ModItems;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.recipe.AssemblerRecipe;
import com.hbm_m.recipe.AssemblerRecipe.AssemblerInputSlot;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * JEI port of {@code AssemblyMachineRecipeHandler} (extends {@code NEIGenericRecipeHandler}).
 */
public class AssemblerJeiCategory extends JeiGenericRecipeCategory<AssemblerRecipe> {

    public static final RecipeType<AssemblerRecipe> RECIPE_TYPE =
            RecipeType.create(RefStrings.MODID, "assembler", AssemblerRecipe.class);

    public AssemblerJeiCategory(IGuiHelper guiHelper) {
        super(guiHelper, new ItemStack[]{
                new ItemStack(ModItems.ADVANCED_ASSEMBLY_MACHINE.get())
        });
    }

    @Override
    public RecipeType<AssemblerRecipe> getRecipeType() {
        return RECIPE_TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("container.hbm_m.advanced_assembly_machine");
    }

    private static List<AssemblerInputSlot> getInputSlots(AssemblerRecipe recipe) {
        List<AssemblerInputSlot> slots = recipe.getInputDisplaySlots();
        if (!slots.isEmpty()) {
            return slots;
        }
        return AssemblerJeiInputs.fallbackFromExpanded(recipe.getIngredients());
    }

    @Override
    protected int getInputCount(AssemblerRecipe recipe) {
        return getInputSlots(recipe).size() + recipe.getFluidInputs().size();
    }

    @Override
    protected int getOutputCount(AssemblerRecipe recipe) {
        return 1 + recipe.getFluidOutputs().size();
    }

    /** Fluessigkeit des Rezepts (inputFluids/outputFluids des Originals) als JEI-Slot. */
    private static void addFluidSlot(IRecipeSlotBuilder slot, dev.architectury.fluid.FluidStack fluid) {
        //? if forge {
        slot.setFluidRenderer(4_000, false, 16, 16)
                .setCustomRenderer(mezz.jei.api.forge.ForgeTypes.FLUID_STACK, new HbmFluidJeiRenderer(16, 16))
                .addIngredient(mezz.jei.api.forge.ForgeTypes.FLUID_STACK, new net.minecraftforge.fluids.FluidStack(fluid.getFluid(), (int) fluid.getAmount(), fluid.getTag()));
        //?} elif neoforge {
        /*slot.setFluidRenderer(4_000, false, 16, 16)
                .setCustomRenderer(mezz.jei.api.neoforge.NeoForgeTypes.FLUID_STACK, new HbmFluidJeiRenderer(16, 16))
                .addIngredient(mezz.jei.api.neoforge.NeoForgeTypes.FLUID_STACK, new net.neoforged.neoforge.fluids.FluidStack(fluid.getFluid(), (int) fluid.getAmount()));
        *///?}
    }

    @Override
    protected boolean hasBlueprintTemplate(AssemblerRecipe recipe) {
        return recipe.requiresBlueprint();
    }

    @Override
    protected int getInputXOffset(AssemblerRecipe recipe, int inputCount) {
        if (inputCount > 12) return -9;
        if (inputCount > 9) return 18;
        return 0;
    }

    @Override
    protected int getOutputXOffset(AssemblerRecipe recipe, int outputCount) {
        return getOffset(recipe);
    }

    @Override
    protected int getMachineXOffset(AssemblerRecipe recipe) {
        return getOffset(recipe);
    }

    private static int getOffset(AssemblerRecipe recipe) {
        int length = getInputSlots(recipe).size() + recipe.getFluidInputs().size();
        if (length > 12) return 27;
        if (length > 9) return 18;
        return 0;
    }

    @Override
    protected void addInputSlots(IRecipeLayoutBuilder builder, AssemblerRecipe recipe, int inputXOffset) {
        List<AssemblerInputSlot> inputs = getInputSlots(recipe);
        int[][] positions = JeiNeiLayout.getGenericInputSlotPositions(getInputCount(recipe));

        for (int i = 0; i < inputs.size() && i < positions.length; i++) {
            AssemblerInputSlot slot = inputs.get(i);
            IRecipeSlotBuilder jeiSlot = addItemSlot(builder, RecipeIngredientRole.INPUT,
                    positions[i][0] + inputXOffset, positions[i][1]);
            JeiIngredientSlots.addCountedIngredient(jeiSlot, slot.ingredient(), slot.count());
        }
        int idx = inputs.size();
        for (dev.architectury.fluid.FluidStack fluid : recipe.getFluidInputs()) {
            if (idx >= positions.length) break;
            addFluidSlot(addItemSlot(builder, RecipeIngredientRole.INPUT, positions[idx][0] + inputXOffset, positions[idx][1]), fluid);
            idx++;
        }
    }

    @Override
    protected void addOutputSlots(IRecipeLayoutBuilder builder, AssemblerRecipe recipe, int outputXOffset) {
        int[][] positions = JeiNeiLayout.getGenericOutputSlotPositions(getOutputCount(recipe));
        addItemSlot(builder, RecipeIngredientRole.OUTPUT, positions[0][0] + outputXOffset, positions[0][1])
                .addItemStack(recipe.getResultItemSafe());
        int idx = 1;
        for (dev.architectury.fluid.FluidStack fluid : recipe.getFluidOutputs()) {
            if (idx >= positions.length) break;
            addFluidSlot(addItemSlot(builder, RecipeIngredientRole.OUTPUT, positions[idx][0] + outputXOffset, positions[idx][1]), fluid);
            idx++;
        }
    }

    @Override
    protected void addBlueprintSlot(IRecipeLayoutBuilder builder, AssemblerRecipe recipe, int machineXOffset) {
        if (!recipe.requiresBlueprint()) {
            return;
        }

        ItemStack folder = com.hbm_m.item.industrial.ItemBlueprints.make(recipe.getBlueprintPool());
        addUnframedSlot(builder, RecipeIngredientRole.RENDER_ONLY, 75 + machineXOffset, 10)
                .addItemStack(folder);
    }

    @Override
    protected void drawRecipeExtras(AssemblerRecipe recipe, GuiGraphics graphics) {
        JeiNeiRendering.drawGenericRecipeExtras(graphics, recipe.getDuration(), recipe.getPowerConsumption());
    }
}
