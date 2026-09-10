package com.hbm_m.compat.jei;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.recipe.ParticleAcceleratorRecipe;

import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.RecipeType;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

//? if forge {
/**
 * JEI-Kategorie des Teilchenbeschleunigers ({@code hbm_m:particle_accelerator}).
 *
 * <p>Zwei Ausgangsstoffe, zwei Ergebnisse. Der noetige <b>Impuls</b> steht nicht in den Plaetzen,
 * sondern als Zeile unter dem Rezept - er ist die eigentliche Bedingung: erreicht das Teilchen ihn
 * am Detektor nicht, kommt gar nichts heraus.</p>
 */
public class ParticleAcceleratorJeiCategory extends JeiUniversalRecipeCategory<ParticleAcceleratorRecipe> {

    public static final RecipeType<ParticleAcceleratorRecipe> RECIPE_TYPE =
            RecipeType.create(RefStrings.MODID, "particle_accelerator", ParticleAcceleratorRecipe.class);

    public ParticleAcceleratorJeiCategory(IGuiHelper guiHelper) {
        super(guiHelper, new ItemStack[]{ new ItemStack(ModBlocks.PA_DETECTOR.get()) });
    }

    @Override
    public RecipeType<ParticleAcceleratorRecipe> getRecipeType() {
        return RECIPE_TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("container.hbm_m.particle_accelerator");
    }

    @Override
    protected int getInputCount(ParticleAcceleratorRecipe recipe) {
        return 2;
    }

    @Override
    protected int getOutputCount(ParticleAcceleratorRecipe recipe) {
        int count = 0;
        if (!recipe.getOutputA().isEmpty()) count++;
        if (!recipe.getOutputB().isEmpty()) count++;
        return count;
    }

    @Override
    protected List<List<ItemStack>> getInputStacks(ParticleAcceleratorRecipe recipe) {
        return List.of(
                Arrays.asList(recipe.getInputA().getItems()),
                Arrays.asList(recipe.getInputB().getItems()));
    }

    @Override
    protected List<ItemStack> getOutputStacks(ParticleAcceleratorRecipe recipe) {
        List<ItemStack> outputs = new ArrayList<>();
        if (!recipe.getOutputA().isEmpty()) outputs.add(recipe.getOutputA());
        if (!recipe.getOutputB().isEmpty()) outputs.add(recipe.getOutputB());
        return outputs;
    }
}
//?} else {
/*public final class ParticleAcceleratorJeiCategory {
    private ParticleAcceleratorJeiCategory() {}
}*///?}
