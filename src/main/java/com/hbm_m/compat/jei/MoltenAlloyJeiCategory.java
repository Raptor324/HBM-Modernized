package com.hbm_m.compat.jei;

import java.util.ArrayList;
import java.util.List;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.inventory.material.MaterialStack;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.recipe.MoltenAlloyRecipe;

import dev.architectury.fluid.FluidStack;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.RecipeType;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

//? if forge {
/**
 * JEI-Kategorie der Schmelzlegierung (siehe {@link MoltenAlloyRecipe}).
 *
 * <p>Hier trifft Schmelze auf Schmelze: mehrere fluessige Metalle im Tiegel werden zu einem neuen.
 * Wie beim Giessbecken steht fuer jede Schmelze das gegossene Blech desselben Materials, weil sich
 * eine Schmelze nicht als Gegenstand zeigen laesst - die <b>Mengen</b> in Millibucket gehen dabei
 * verloren, sie stehen nur im Rezept selbst.</p>
 */
public class MoltenAlloyJeiCategory extends JeiFluidRecipeCategory<MoltenAlloyRecipe> {

    public static final RecipeType<MoltenAlloyRecipe> RECIPE_TYPE =
            RecipeType.create(RefStrings.MODID, "molten_alloy", MoltenAlloyRecipe.class);

    public MoltenAlloyJeiCategory(IGuiHelper guiHelper) {
        super(guiHelper, new ItemStack[]{ new ItemStack(ModBlocks.FOUNDRY_TANK.get()) });
    }

    @Override
    public RecipeType<MoltenAlloyRecipe> getRecipeType() {
        return RECIPE_TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("block.hbm_m.foundry_tank");
    }

    @Override
    protected List<FluidStack> getInputFluids(MoltenAlloyRecipe recipe) {
        return List.of();
    }

    @Override
    protected List<Ingredient> getInputItems(MoltenAlloyRecipe recipe) {
        return toIngredients(recipe.getInputs());
    }

    @Override
    protected List<FluidStack> getOutputFluids(MoltenAlloyRecipe recipe) {
        return List.of();
    }

    @Override
    protected List<ItemStack> getOutputItems(MoltenAlloyRecipe recipe) {
        List<ItemStack> outputs = new ArrayList<>();
        for (MaterialStack stack : recipe.getOutputs()) {
            ItemStack plate = plateOf(stack);
            if (plate != null) outputs.add(plate);
        }
        return outputs;
    }

    private static List<Ingredient> toIngredients(MaterialStack[] stacks) {
        List<Ingredient> list = new ArrayList<>();
        for (MaterialStack stack : stacks) {
            ItemStack plate = plateOf(stack);
            if (plate != null) list.add(Ingredient.of(plate));
        }
        return list;
    }

    /** Das gegossene Blech als Stellvertreter der Schmelze, oder {@code null} wenn es keines gibt. */
    private static ItemStack plateOf(MaterialStack stack) {
        if (stack == null || stack.type == null) return null;
        ItemStack plate = stack.type.getCastPlate(1);
        return plate == null || plate.isEmpty() ? null : plate;
    }
}
//?} else {
/*public final class MoltenAlloyJeiCategory {
    private MoltenAlloyJeiCategory() {}
}*///?}
