package com.hbm_m.compat.jei;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.recipe.FusionRecipe;

import dev.architectury.fluid.FluidStack;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * JEI-Kategorie fuer die Fusionsrezepte - Port von {@code FusionRecipeHandler} /
 * {@code FusionRecipe.printNEIExtras} (1.7.10).
 *
 * <p>Wie im Original wechselt die untere Zeile im Sekundentakt zwischen Stromverbrauch und
 * Zuendschwelle; darunter stehen Plasmaausgang und Neutronenfluss.</p>
 */

//? if forge {
@net.minecraftforge.api.distmarker.OnlyIn(net.minecraftforge.api.distmarker.Dist.CLIENT)
//?} elif fabric {
/*@net.fabricmc.api.Environment(net.fabricmc.api.EnvType.CLIENT)
*///?} elif neoforge {
/*@net.neoforged.api.distmarker.OnlyIn(net.neoforged.api.distmarker.Dist.CLIENT)
*///?}
public class FusionJeiCategory extends JeiGenericRecipeCategory<FusionRecipe> {

    public static final RecipeType<FusionRecipe> RECIPE_TYPE =
            RecipeType.create(RefStrings.MODID, "fusion", FusionRecipe.class);

    private static final int FLUID_RENDERER_CAPACITY = 24_000;

    public FusionJeiCategory(IGuiHelper guiHelper) {
        super(guiHelper, new ItemStack[]{ new ItemStack(ModBlocks.TORUS.get()) });
    }

    @Override
    public RecipeType<FusionRecipe> getRecipeType() {
        return RECIPE_TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("container.hbm_m.fusion_torus");
    }

    @Override
    protected int getInputCount(FusionRecipe recipe) {
        return countFluids(recipe.getFluidInputs());
    }

    @Override
    protected int getOutputCount(FusionRecipe recipe) {
        return countItems(recipe) + countFluids(recipe.getFluidOutputs());
    }

    @Override
    protected boolean hasBlueprintTemplate(FusionRecipe recipe) {
        return false;
    }

    @Override
    protected void addInputSlots(IRecipeLayoutBuilder builder, FusionRecipe recipe, int inputXOffset) {
        int[][] positions = JeiNeiLayout.getGenericInputSlotPositions(getInputCount(recipe));
        int slotIndex = 0;

        for (FluidStack fluid : recipe.getFluidInputs()) {
            if (fluid.isEmpty()) continue;
            addFluidSlot(builder, RecipeIngredientRole.INPUT,
                    positions[slotIndex][0] + inputXOffset, positions[slotIndex][1], fluid);
            slotIndex++;
        }
    }

    @Override
    protected void addOutputSlots(IRecipeLayoutBuilder builder, FusionRecipe recipe, int outputXOffset) {
        int[][] positions = JeiNeiLayout.getGenericOutputSlotPositions(getOutputCount(recipe));
        int slotIndex = 0;

        for (ItemStack output : recipe.getItemOutputs()) {
            if (output.isEmpty()) continue;
            addItemSlot(builder, RecipeIngredientRole.OUTPUT,
                    positions[slotIndex][0] + outputXOffset, positions[slotIndex][1])
                    .addItemStack(output);
            slotIndex++;
        }

        for (FluidStack fluid : recipe.getFluidOutputs()) {
            if (fluid.isEmpty()) continue;
            addFluidSlot(builder, RecipeIngredientRole.OUTPUT,
                    positions[slotIndex][0] + outputXOffset, positions[slotIndex][1], fluid);
            slotIndex++;
        }
    }

    private void addFluidSlot(IRecipeLayoutBuilder builder, RecipeIngredientRole role,
                              int x, int y, FluidStack fluid) {
        //? if forge {
        addItemSlot(builder, role, x, y)
                .setFluidRenderer(FLUID_RENDERER_CAPACITY, false, 16, 16)
                .setCustomRenderer(mezz.jei.api.forge.ForgeTypes.FLUID_STACK, new HbmFluidJeiRenderer(16, 16))
                .addIngredient(mezz.jei.api.forge.ForgeTypes.FLUID_STACK,
                        new net.minecraftforge.fluids.FluidStack(fluid.getFluid(), (int) fluid.getAmount(), fluid.getTag()));
        //?} elif neoforge {
        /*addItemSlot(builder, role, x, y)
                .setFluidRenderer(FLUID_RENDERER_CAPACITY, false, 16, 16)
                .setCustomRenderer(mezz.jei.api.neoforge.NeoForgeTypes.FLUID_STACK, new HbmFluidJeiRenderer(16, 16))
                .addIngredient(mezz.jei.api.neoforge.NeoForgeTypes.FLUID_STACK,
                        new net.neoforged.neoforge.fluids.FluidStack(fluid.getFluid(), (int) fluid.getAmount()));
        *///?}
    }

    @Override
    protected void addBlueprintSlot(IRecipeLayoutBuilder builder, FusionRecipe recipe, int machineXOffset) {
        // Fusionsrezepte sind nicht blaupausengebunden.
    }

    @Override
    protected void drawRecipeExtras(FusionRecipe recipe, GuiGraphics graphics) {
        var font = Minecraft.getInstance().font;
        int side = 164;

        String duration = JeiNeiRendering.formatShortNumber(recipe.getDuration()) + " ticks";
        graphics.drawString(font, duration, side - font.width(duration), 45, 0x404040, false);

        // Original: der Verbrauch wechselt jede Sekunde mit der Zuendschwelle.
        if (System.currentTimeMillis() % 2000 < 1000) {
            String consumption = JeiNeiRendering.formatShortNumber(recipe.getPower()) + "HE/t";
            graphics.drawString(font, consumption, side - font.width(consumption), 57, 0x404040, false);
        } else {
            String temp = JeiNeiRendering.formatShortNumber(recipe.getIgnitionTemp()) + "Ky/t";
            graphics.drawString(font, temp, side - font.width(temp), 57, 0xa000a0, false);
        }

        String output = JeiNeiRendering.formatShortNumber(recipe.getOutputTemp()) + "TU/t";
        graphics.drawString(font, output, side - font.width(output), 69, 0xa000a0, false);

        String flux = (((int) (recipe.getNeutronFlux() * 10)) / 10D) + " flux/t";
        graphics.drawString(font, flux, side - font.width(flux), 81, 0xa000a0, false);
    }

    private static int countItems(FusionRecipe recipe) {
        int count = 0;
        for (ItemStack stack : recipe.getItemOutputs()) if (!stack.isEmpty()) count++;
        return count;
    }

    private static int countFluids(java.util.List<FluidStack> fluids) {
        int count = 0;
        for (FluidStack fluid : fluids) if (!fluid.isEmpty()) count++;
        return count;
    }
}
