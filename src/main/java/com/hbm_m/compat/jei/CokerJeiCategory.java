package com.hbm_m.compat.jei;

import java.util.ArrayList;
import java.util.List;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.recipe.CokerRecipe;

import dev.architectury.fluid.FluidStack;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * JEI-Kategorie des Verkokers (siehe {@link CokerRecipe}).
 *
 * <p>Als einzige dieser Maschinen liefert der Verkoker einen <b>festen</b> Stoff, dazu
 * wahlweise ein fluessiges Nebenerzeugnis.</p>
 */
public class CokerJeiCategory extends JeiFluidRecipeCategory<CokerRecipe> {

    public static final RecipeType<CokerRecipe> RECIPE_TYPE =
            RecipeType.create(RefStrings.MODID, "coker", CokerRecipe.class);

    public CokerJeiCategory(IGuiHelper guiHelper) {
        super(guiHelper, new ItemStack[]{ new ItemStack(ModBlocks.COKER.get()) });
    }

    @Override
    public RecipeType<CokerRecipe> getRecipeType() {
        return RECIPE_TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("block.hbm_m.coker");
    }

    @Override
    protected List<FluidStack> getInputFluids(CokerRecipe recipe) {
        return List.of(FluidStack.create(recipe.getInputFluid(), recipe.getInputMb()));
    }

    @Override
    protected List<FluidStack> getOutputFluids(CokerRecipe recipe) {
        List<FluidStack> out = new ArrayList<>();
        if (recipe.getByproductFluid() != null) {
            out.add(FluidStack.create(recipe.getByproductFluid(), recipe.getByproductMb()));
        }
        return out;
    }

    @Override
    protected List<ItemStack> getOutputItems(CokerRecipe recipe) {
        return List.of(recipe.getOutput());
    }

    /**
     * Reihenfolge wie im Original: {@code CokerRecipes.getRecipes()} legt den festen Ausgang
     * zuerst ab ({@code new ItemStack[] {out, ItemFluidIcon.make(byproduct)}}) - NEI setzt also
     * das Koks auf den Hauptergebnisplatz (102, 24) und die Nebenfluessigkeit daneben (120, 24).
     * Die Basisklasse ordnet Fluessigkeiten zuerst, hier laeuft es fuer den Verkoker umgekehrt.
     */
    @Override
    protected void addOutputSlots(IRecipeLayoutBuilder builder, CokerRecipe recipe, int outputXOffset) {
        List<ItemStack> items = getOutputItems(recipe);
        List<FluidStack> fluids = getOutputFluids(recipe);

        int[][] positions = JeiNeiLayout.getGenericOutputSlotPositions(items.size() + fluids.size());
        int slot = 0;

        for (ItemStack stack : items) {
            addItemSlot(builder, RecipeIngredientRole.OUTPUT,
                    positions[slot][0] + outputXOffset, positions[slot][1])
                    .addItemStack(stack);
            slot++;
        }

        for (FluidStack fluid : fluids) {
            addFluidSlot(builder, RecipeIngredientRole.OUTPUT,
                    positions[slot][0] + outputXOffset, positions[slot][1], fluid);
            slot++;
        }
    }
}
