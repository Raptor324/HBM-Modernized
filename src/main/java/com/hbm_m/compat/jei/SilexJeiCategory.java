package com.hbm_m.compat.jei;

import java.util.ArrayList;
import java.util.List;

import com.hbm_m.inventory.fluid.ModFluids;
import com.hbm_m.item.ModItems;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.recipe.SilexRecipe;

import dev.architectury.fluid.FluidStack;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.RecipeType;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

//? if forge {
/**
 * JEI-Kategorie des SILEX (siehe {@link SilexRecipe}).
 *
 * <p>Der Laser zerlegt einen Gegenstand unter Zugabe von Peroxid in mehrere moegliche Ergebnisse -
 * <b>gewichtet</b>, es kommt also nicht jedes Mal dasselbe heraus. Die Anzeige listet alle
 * Moeglichkeiten nebeneinander; welchen Anteil jede hat, steht in ihrem Hinweistext.</p>
 */
public class SilexJeiCategory extends JeiFluidRecipeCategory<SilexRecipe> {

    public static final RecipeType<SilexRecipe> RECIPE_TYPE =
            RecipeType.create(RefStrings.MODID, "silex", SilexRecipe.class);

    public SilexJeiCategory(IGuiHelper guiHelper) {
        super(guiHelper, new ItemStack[]{ new ItemStack(ModItems.SILEX.get()) });
    }

    @Override
    public RecipeType<SilexRecipe> getRecipeType() {
        return RECIPE_TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("block.hbm_m.silex");
    }

    @Override
    protected List<FluidStack> getInputFluids(SilexRecipe recipe) {
        return List.of(FluidStack.create(ModFluids.PEROXIDE.getSource(), recipe.getPeroxideMb()));
    }

    @Override
    protected List<Ingredient> getInputItems(SilexRecipe recipe) {
        return List.of(recipe.getInput());
    }

    @Override
    protected List<FluidStack> getOutputFluids(SilexRecipe recipe) {
        return List.of();
    }

    /**
     * Alle moeglichen Ergebnisse. Die Gewichtung selbst laesst sich in einem Platz nicht
     * darstellen; sie steht als Anteil im Hinweistext des jeweiligen Stapels.
     */
    @Override
    protected List<ItemStack> getOutputItems(SilexRecipe recipe) {
        List<ItemStack> outputs = new ArrayList<>();
        for (SilexRecipe.WeightedOutput out : recipe.getOutputs()) {
            if (out.stack() != null && !out.stack().isEmpty()) outputs.add(out.stack().copy());
        }
        return outputs;
    }
}
//?} else {
/*public final class SilexJeiCategory {
    private SilexJeiCategory() {}
}*///?}
