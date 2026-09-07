package com.hbm_m.compat.jei;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.item.ModItems;
import com.hbm_m.item.industrial.ItemBlueprintFolder;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.recipe.PlasmaForgeRecipe;
import com.hbm_m.recipe.PlasmaForgeRecipe.CountedIngredient;

import dev.architectury.fluid.FluidStack;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * JEI-Kategorie der Plasmaschmiede - Port von {@code PlasmaForgeRecipe.printNEIExtras} (1.7.10):
 * die untere Zeile wechselt im Sekundentakt zwischen Stromverbrauch und benoetigter Plasmaleistung.
 */

//? if forge {
@net.minecraftforge.api.distmarker.OnlyIn(net.minecraftforge.api.distmarker.Dist.CLIENT)
//?} elif fabric {
/*@net.fabricmc.api.Environment(net.fabricmc.api.EnvType.CLIENT)
*///?} elif neoforge {
/*@net.neoforged.api.distmarker.OnlyIn(net.neoforged.api.distmarker.Dist.CLIENT)
*///?}
public class PlasmaForgeJeiCategory extends JeiGenericRecipeCategory<PlasmaForgeRecipe> {

    public static final RecipeType<PlasmaForgeRecipe> RECIPE_TYPE =
            RecipeType.create(RefStrings.MODID, "plasma_forge", PlasmaForgeRecipe.class);

    private static final int FLUID_RENDERER_CAPACITY = 24_000;

    public PlasmaForgeJeiCategory(IGuiHelper guiHelper) {
        super(guiHelper, new ItemStack[]{ new ItemStack(ModBlocks.PLASMA_FORGE.get()) });
    }

    @Override
    public RecipeType<PlasmaForgeRecipe> getRecipeType() {
        return RECIPE_TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("container.hbm_m.plasma_forge");
    }

    @Override
    protected int getInputCount(PlasmaForgeRecipe recipe) {
        int count = 0;
        for (CountedIngredient ci : recipe.getItemInputs()) if (!ci.ingredient().isEmpty()) count++;
        for (FluidStack fluid : recipe.getFluidInputs()) if (!fluid.isEmpty()) count++;
        return count;
    }

    @Override
    protected int getOutputCount(PlasmaForgeRecipe recipe) {
        return recipe.getOutput().isEmpty() ? 0 : 1;
    }

    @Override
    protected boolean hasBlueprintTemplate(PlasmaForgeRecipe recipe) {
        return recipe.requiresBlueprint();
    }

    @Override
    protected int getInputXOffset(PlasmaForgeRecipe recipe, int inputCount) {
        if (inputCount > 12) return -9;
        if (inputCount > 9) return 18;
        return 0;
    }

    @Override
    protected int getOutputXOffset(PlasmaForgeRecipe recipe, int outputCount) {
        return getOffset(getInputCount(recipe));
    }

    @Override
    protected int getMachineXOffset(PlasmaForgeRecipe recipe) {
        return getOffset(getInputCount(recipe));
    }

    private static int getOffset(int inputCount) {
        if (inputCount > 12) return 27;
        if (inputCount > 9) return 18;
        return 0;
    }

    @Override
    protected void addInputSlots(IRecipeLayoutBuilder builder, PlasmaForgeRecipe recipe, int inputXOffset) {
        int[][] positions = JeiNeiLayout.getGenericInputSlotPositions(getInputCount(recipe));
        int slotIndex = 0;

        for (CountedIngredient input : recipe.getItemInputs()) {
            if (input.ingredient().isEmpty()) continue;
            IRecipeSlotBuilder slot = addItemSlot(builder, RecipeIngredientRole.INPUT,
                    positions[slotIndex][0] + inputXOffset, positions[slotIndex][1]);
            JeiIngredientSlots.addCountedIngredient(slot, input.ingredient(), input.count());
            slotIndex++;
        }

        for (FluidStack fluid : recipe.getFluidInputs()) {
            if (fluid.isEmpty()) continue;
            //? if forge {
            addItemSlot(builder, RecipeIngredientRole.INPUT,
                    positions[slotIndex][0] + inputXOffset, positions[slotIndex][1])
                    .setFluidRenderer(FLUID_RENDERER_CAPACITY, false, 16, 16)
                    .setCustomRenderer(mezz.jei.api.forge.ForgeTypes.FLUID_STACK, new HbmFluidJeiRenderer(16, 16))
                    .addIngredient(mezz.jei.api.forge.ForgeTypes.FLUID_STACK,
                            new net.minecraftforge.fluids.FluidStack(fluid.getFluid(), (int) fluid.getAmount(), fluid.getTag()));
            //?} elif neoforge {
            /*addItemSlot(builder, RecipeIngredientRole.INPUT,
                    positions[slotIndex][0] + inputXOffset, positions[slotIndex][1])
                    .setFluidRenderer(FLUID_RENDERER_CAPACITY, false, 16, 16)
                    .setCustomRenderer(mezz.jei.api.neoforge.NeoForgeTypes.FLUID_STACK, new HbmFluidJeiRenderer(16, 16))
                    .addIngredient(mezz.jei.api.neoforge.NeoForgeTypes.FLUID_STACK,
                            new net.neoforged.neoforge.fluids.FluidStack(fluid.getFluid(), (int) fluid.getAmount()));
            *///?}
            slotIndex++;
        }
    }

    @Override
    protected void addOutputSlots(IRecipeLayoutBuilder builder, PlasmaForgeRecipe recipe, int outputXOffset) {
        ItemStack output = recipe.getOutput();
        if (output.isEmpty()) return;

        int[][] positions = JeiNeiLayout.getGenericOutputSlotPositions(1);
        addItemSlot(builder, RecipeIngredientRole.OUTPUT,
                positions[0][0] + outputXOffset, positions[0][1])
                .addItemStack(output);
    }

    @Override
    protected void addBlueprintSlot(IRecipeLayoutBuilder builder, PlasmaForgeRecipe recipe, int machineXOffset) {
        if (!recipe.requiresBlueprint()) return;

        ItemStack folder = new ItemStack(ModItems.BLUEPRINT_FOLDER.get());
        ItemBlueprintFolder.writeBlueprintPool(folder, recipe.getBlueprintPool());
        addUnframedSlot(builder, RecipeIngredientRole.RENDER_ONLY, 75 + machineXOffset, 10)
                .addItemStack(folder);
    }

    @Override
    protected void drawRecipeExtras(PlasmaForgeRecipe recipe, GuiGraphics graphics) {
        var font = Minecraft.getInstance().font;
        int side = 164;

        String duration = JeiNeiRendering.formatShortNumber(recipe.getDuration()) + " ticks";
        graphics.drawString(font, duration, side - font.width(duration), 45, 0x404040, false);

        if (System.currentTimeMillis() % 2000 < 1000) {
            String consumption = JeiNeiRendering.formatShortNumber(recipe.getPower()) + "HE/t";
            graphics.drawString(font, consumption, side - font.width(consumption), 57, 0x404040, false);
        } else {
            String temp = JeiNeiRendering.formatShortNumber(recipe.getIgnitionTemp()) + "TU/t";
            graphics.drawString(font, temp, side - font.width(temp), 57, 0xa000a0, false);
        }
    }
}
