package com.hbm_m.compat.jei;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.handler.rbmk.RBMKOutgasserRecipes;
import com.hbm_m.handler.rbmk.RBMKOutgasserRecipes.OutgasserRecipe;
import com.hbm_m.item.ModItems;
import com.hbm_m.lib.RefStrings;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * JEI port of {@code OutgasserHandler}: the RBMK irradiation channel's recipe table.
 *
 * <p>Without it the outgasser's whole recipe table is invisible: nothing else in the game hints
 * that lithium becomes tritium or that gold becomes gold-198 inside a reactor channel.</p>
 *
 * <p>Like the original, the fluid half of a result is its own output field (NEI showed
 * {@code ItemFluidIcon.make(fluidStack)}), and the machines are both the outgasser and the
 * fusion breeder - {@code TileEntityFusionBreeder} runs the very same table.</p>
 */
public class RBMKOutgasserJeiCategory extends JeiGenericRecipeCategory<RBMKOutgasserJeiCategory.Activation> {

    /** {@code fluidOutput} is null for recipes that only produce a solid, and vice versa. */
    public record Activation(ItemStack input, ItemStack solidOutput, Fluid fluidType, int fluidAmount) {

        public boolean hasSolid() { return solidOutput != null && !solidOutput.isEmpty(); }
        public boolean hasFluid() { return fluidType != null && fluidAmount > 0; }
    }

    public static final RecipeType<Activation> RECIPE_TYPE =
            RecipeType.create(RefStrings.MODID, "rbmk_outgasser", Activation.class);

    public RBMKOutgasserJeiCategory(IGuiHelper guiHelper) {
        // OutgasserHandler: machines = { rbmk_outgasser, fusion_breeder } - the breeder's
        // irradiation channels process the same table, so it cycles in the machine slot too.
        super(guiHelper, new ItemStack[]{ new ItemStack(ModBlocks.RBMK_OUTGASSER.get()),
                new ItemStack(ModItems.FUSION_BREEDER_ITEM.get()) });
    }

    public static List<Activation> all() {
        List<Activation> out = new ArrayList<>();
        for (Map.Entry<net.minecraft.world.item.Item, OutgasserRecipe> e : RBMKOutgasserRecipes.getRecipes().entrySet()) {
            OutgasserRecipe r = e.getValue();
            out.add(new Activation(new ItemStack(e.getKey()),
                    r.solidOutput(), r.fluidType(), r.fluidAmount()));
        }
        return out;
    }

    @Override public RecipeType<Activation> getRecipeType() { return RECIPE_TYPE; }

    @Override
    public Component getTitle() {
        return Component.translatable("gui.hbm_m.jei.rbmk_outgasser");
    }

    @Override protected int getInputCount(Activation recipe)  { return 1; }
    @Override protected int getOutputCount(Activation recipe) {
        return (recipe.hasSolid() ? 1 : 0) + (recipe.hasFluid() ? 1 : 0);
    }
    @Override protected boolean hasBlueprintTemplate(Activation recipe) { return false; }

    @Override
    protected void addInputSlots(IRecipeLayoutBuilder builder, Activation recipe, int inputXOffset) {
        int[][] positions = JeiNeiLayout.getGenericInputSlotPositions(1);
        addItemSlot(builder, RecipeIngredientRole.INPUT, positions[0][0] + inputXOffset, positions[0][1])
                .addItemStack(recipe.input());
    }

    @Override
    protected void addOutputSlots(IRecipeLayoutBuilder builder, Activation recipe, int outputXOffset) {
        int count = getOutputCount(recipe);
        if (count == 0) return;
        int[][] positions = JeiNeiLayout.getGenericOutputSlotPositions(count);
        int index = 0;
        if (recipe.hasSolid()) {
            addItemSlot(builder, RecipeIngredientRole.OUTPUT, positions[index][0] + outputXOffset, positions[index][1])
                    .addItemStack(recipe.solidOutput());
            index++;
        }
        // Original OutgasserRecipes.getRecipes() rendered the fluid half as ItemFluidIcon.make(fluidStack):
        // a second result slot at (120, 24) when a solid is present, or the only slot at (102, 24) otherwise.
        if (recipe.hasFluid()) {
            addItemSlot(builder, RecipeIngredientRole.OUTPUT, positions[index][0] + outputXOffset, positions[index][1])
                    .setCustomRenderer(JeiFluidTypes.FLUID_STACK, new HbmFluidJeiRenderer(16, 16))
                    .addFluidStack(recipe.fluidType(), Math.max(recipe.fluidAmount(), 1));
        }
    }

    @Override
    protected void addBlueprintSlot(IRecipeLayoutBuilder builder, Activation recipe, int machineXOffset) {
        // Activation needs no blueprint - only neutron flux.
    }
}
