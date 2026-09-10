package com.hbm_m.compat.jei;

import java.util.ArrayList;
import java.util.List;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.recipe.CokerRecipe;

import dev.architectury.fluid.FluidStack;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.RecipeType;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

//? if forge {
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
}
//?} else {
/*public final class CokerJeiCategory {
    private CokerJeiCategory() {}
}*///?}
