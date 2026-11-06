package com.hbm_m.compat.jei;

import java.util.Locale;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.inventory.fluid.FluidType;
import com.hbm_m.inventory.fluid.ModFluids;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.recipe.RotaryFurnaceRecipe;

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
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * JEI category for Rotary Furnace recipes - bis zu 3 Item-Eingaenge + optionales Eingangs-Fluid
 * -&gt; 1 Ausgabe. Arbeitet direkt mit dem data-driven {@link RotaryFurnaceRecipe} (JSON) -
 * Eingaenge aus {@link RotaryFurnaceRecipe#getInputs()}, Ausgabe aus {@link RotaryFurnaceRecipe#getOutput()}.
 *
 * <p>Layout 1:1 wie der Original-NEI-Handler: {@code RotaryFurnaceHandler} erweitert
 * {@code NEIUniversalHandler}, also Universal-Slotkoordinaten (Item-Eingaenge fuellen von rechts
 * nach links, der Fluid-Eingang liegt auf dem linksten Platz), ein Ausgang auf {@code (102, 24)}
 * und rechtsbuendige Zusatztexte {@code Dauer} (y=43) + {@code Dampf: X mB/t} (y=55) an x=160.</p>
 */
@OnlyIn(Dist.CLIENT)
public class RotaryFurnaceJeiCategory extends JeiGenericRecipeCategory<RotaryFurnaceRecipe> {

    public static final RecipeType<RotaryFurnaceRecipe> RECIPE_TYPE =
            RecipeType.create(RefStrings.MODID, "rotary_furnace", RotaryFurnaceRecipe.class);

    private static final int FLUID_RENDERER_CAPACITY = 4_000;

    public RotaryFurnaceJeiCategory(IGuiHelper guiHelper) {
        super(guiHelper, new ItemStack[]{
                new ItemStack(ModBlocks.ROTARY_FURNACE.get())
        });
    }

    @Override
    public RecipeType<RotaryFurnaceRecipe> getRecipeType() {
        return RECIPE_TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("container.hbm_m.rotary_furnace");
    }

    @Override
    protected int getInputCount(RotaryFurnaceRecipe recipe) {
        return recipe.getInputs().length + (recipe.getFluid() != null ? 1 : 0);
    }

    @Override
    protected int getOutputCount(RotaryFurnaceRecipe recipe) {
        return 1;
    }

    @Override
    protected boolean hasBlueprintTemplate(RotaryFurnaceRecipe recipe) {
        return false;
    }

    @Override
    protected void addInputSlots(IRecipeLayoutBuilder builder, RotaryFurnaceRecipe recipe, int inputXOffset) {
        // Original: NEIUniversalHandler.getInputCoords (RotaryFurnaceHandler extends NEIUniversalHandler) —
        // die Eingaenge fuellen von rechts (48, 24) nach links, der Fluid-Eingang liegt auf dem letzten,
        // also linksten Platz (im Original ist der Fluid-Icon-Stack das letzte Element des Eingabe-Arrays).
        int inputCount = getInputCount(recipe);
        int[][] positions = JeiNeiLayout.getUniversalInputCoords(inputCount);
        int slotIndex = 0;

        var inputs = recipe.getInputs();
        for (int i = 0; i < inputs.length; i++) {
            var slot = addItemSlot(builder, RecipeIngredientRole.INPUT,
                    positions[slotIndex][0] + inputXOffset, positions[slotIndex][1]);
            JeiIngredientSlots.addCountedIngredient(slot, inputs[i], recipe.getInputCount(i));
            slotIndex++;
        }

        if (recipe.getFluid() != null) {
            FluidStack fluid = FluidStack.create(recipe.getFluid().getFluid(), recipe.getFluidAmountMb());
            addItemSlot(builder, RecipeIngredientRole.INPUT,
                    positions[slotIndex][0] + inputXOffset, positions[slotIndex][1])
                    .setFluidRenderer(FLUID_RENDERER_CAPACITY, false, 16, 16)
                    //? if forge {
                    .setCustomRenderer(ForgeTypes.FLUID_STACK, new HbmFluidJeiRenderer(16, 16))
                    .addIngredient(ForgeTypes.FLUID_STACK, FluidStackHooksForge.toForge(fluid));
                    //?} elif neoforge {
                    /*.setCustomRenderer(NeoForgeTypes.FLUID_STACK, new HbmFluidJeiRenderer(16, 16))
                    .addIngredient(NeoForgeTypes.FLUID_STACK, new net.minecraftforge.fluids.FluidStack(fluid.getFluid(), (int) fluid.getAmount()));
                    *///?}
        }
    }

    @Override
    protected void addOutputSlots(IRecipeLayoutBuilder builder, RotaryFurnaceRecipe recipe, int outputXOffset) {
        // Original: NEIUniversalHandler.getOutputCoords(1) = (102, 24) (Slot-Rahmen auf (101, 23)).
        int[][] positions = JeiNeiLayout.getUniversalOutputCoords(getOutputCount(recipe));
        addItemSlot(builder, RecipeIngredientRole.OUTPUT, positions[0][0] + outputXOffset, positions[0][1])
                .addItemStack(recipe.getOutput());
    }

    @Override
    protected void addBlueprintSlot(IRecipeLayoutBuilder builder, RotaryFurnaceRecipe recipe, int machineXOffset) {
        // Kein Blueprint-Slot fuer Rotary-Furnace-Rezepte.
    }

    @Override
    protected void drawRecipeExtras(RotaryFurnaceRecipe recipe, GuiGraphics graphics) {
        // 1:1 RotaryFurnaceHandler.drawExtras: Dauer und Dampfverbrauch rechtsbuendig an x=160,
        // mit dem lokalisierten Namen des STEAM-Fluidtyps statt der generischen "HE/t"-Zeile.
        var font = Minecraft.getInstance().font;
        String duration = String.format(Locale.US, "%,d", recipe.getDuration()) + " ticks";
        String consumption = FluidType.forFluid(ModFluids.STEAM.getSource()).getLocalizedName().getString()
                + ": " + String.format(Locale.US, "%,d", recipe.getSteam()) + " mB/t";
        int side = 160;
        int color = 0x404040;

        graphics.drawString(font, duration, side - font.width(duration), 43, color, false);
        graphics.drawString(font, consumption, side - font.width(consumption), 55, color, false);
    }
}
