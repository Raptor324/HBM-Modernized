package com.hbm_m.compat.jei;

import java.util.ArrayList;
import java.util.List;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.inventory.material.MaterialStack;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.recipe.MoldCastingRecipe;

import dev.architectury.fluid.FluidStack;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.RecipeType;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

//? if forge {
/**
 * JEI-Kategorie des Giessbeckens (siehe {@link MoldCastingRecipe}).
 *
 * <p>Gegossen wird aus einer <b>Schmelze</b>, nicht aus Gegenstaenden: die Form gibt vor, was
 * entsteht, das Material woraus. Weil sich eine Schmelze nicht als Gegenstand zeigen laesst, steht
 * an ihrer Stelle das gegossene Blech desselben Materials - das ist im Spiel die uebliche
 * Darstellung dafuer.</p>
 *
 * <p>Materialien ohne eigenes Gussblech (etwa BSCCO oder Schrabidat) lassen den Platz leer; das
 * Rezept bleibt trotzdem sichtbar, weil Form und Ergebnis genuegen, um es zu finden.</p>
 */
public class MoldCastingJeiCategory extends JeiFluidRecipeCategory<MoldCastingRecipe> {

    public static final RecipeType<MoldCastingRecipe> RECIPE_TYPE =
            RecipeType.create(RefStrings.MODID, "mold_casting", MoldCastingRecipe.class);

    public MoldCastingJeiCategory(IGuiHelper guiHelper) {
        super(guiHelper, new ItemStack[]{ new ItemStack(ModBlocks.FOUNDRY_MOLD.get()) });
    }

    @Override
    public RecipeType<MoldCastingRecipe> getRecipeType() {
        return RECIPE_TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("block.hbm_m.foundry_mold");
    }

    @Override
    protected List<FluidStack> getInputFluids(MoldCastingRecipe recipe) {
        return List.of();
    }

    @Override
    protected List<Ingredient> getInputItems(MoldCastingRecipe recipe) {
        List<Ingredient> inputs = new ArrayList<>();

        // Die Schmelze, dargestellt durch ein Gussblech desselben Materials.
        ItemStack plate = recipe.getMaterial() != null
                ? recipe.getMaterial().getCastPlate(1) : null;
        if (plate != null && !plate.isEmpty()) inputs.add(Ingredient.of(plate));

        return inputs;
    }

    @Override
    protected List<FluidStack> getOutputFluids(MoldCastingRecipe recipe) {
        return List.of();
    }

    @Override
    protected List<ItemStack> getOutputItems(MoldCastingRecipe recipe) {
        ItemStack out = recipe.getOutput();
        return out == null || out.isEmpty() ? List.of() : List.of(out);
    }
}
//?} else {
/*public final class MoldCastingJeiCategory {
    private MoldCastingJeiCategory() {}
}*///?}
