package com.hbm_m.compat.jei;
//? if forge || neoforge {

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.inventory.fluid.ModFluids;
import com.hbm_m.inventory.recipes.ElectrolyserMetalRecipes;
import com.hbm_m.inventory.recipes.ElectrolyserMetalRecipes.ElectrolysisMetalRecipe;
import com.hbm_m.item.material.ItemScraps;
import com.hbm_m.lib.RefStrings;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Original {@code ElectrolyserMetalRecipes.getRecipes()}: Eingabe plus 100 mB Salpetersaeure -> zwei Schmelzen
 * (als fluessiger Schrott) und feste Nebenprodukte.
 */
public class ElectrolyserMetalJeiCategory extends JeiUniversalRecipeCategory<ElectrolyserMetalJeiCategory.Entry> {

    public record Entry(Item input, ElectrolysisMetalRecipe recipe) { }

    public static final RecipeType<Entry> RECIPE_TYPE = RecipeType.create(RefStrings.MODID, "electrolyser_metal", Entry.class);

    public ElectrolyserMetalJeiCategory(IGuiHelper guiHelper) {
        super(guiHelper, new ItemStack[] { new ItemStack(ModBlocks.ELECTROLYSER.get()) });
    }

    public static List<Entry> recipes() {
        List<Entry> list = new ArrayList<>();
        for (Map.Entry<Item, ElectrolysisMetalRecipe> e : ElectrolyserMetalRecipes.all().entrySet()) list.add(new Entry(e.getKey(), e.getValue()));
        return list;
    }

    @Override public RecipeType<Entry> getRecipeType() { return RECIPE_TYPE; }
    @Override public Component getTitle() { return Component.translatable("container.machineElectrolyser"); }

    @Override protected int getInputCount(Entry e) { return 2; }
    @Override protected int getOutputCount(Entry e) { return getOutputStacks(e).size(); }
    @Override protected List<List<ItemStack>> getInputStacks(Entry e) { return List.of(List.of(new ItemStack(e.input()))); }

    @Override
    protected List<ItemStack> getOutputStacks(Entry e) {
        List<ItemStack> outputs = new ArrayList<>();
        if (e.recipe().output1 != null) outputs.add(ItemScraps.create(e.recipe().output1, true));
        if (e.recipe().output2 != null) outputs.add(ItemScraps.create(e.recipe().output2, true));
        for (ItemStack byproduct : e.recipe().byproduct) outputs.add(byproduct);
        return outputs;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, Entry e, IFocusGroup focuses) {
        int[][] inPos = JeiNeiLayout.getUniversalInputCoords(2);
        List<ItemStack> outputs = getOutputStacks(e);
        int[][] outPos = JeiNeiLayout.getUniversalOutputCoords(outputs.size());

        builder.addSlot(RecipeIngredientRole.INPUT, inPos[0][0], inPos[0][1])
                .setBackground(itemSlotBackground, -1, -1)
                .addItemStack(new ItemStack(e.input()));
        builder.addSlot(RecipeIngredientRole.INPUT, inPos[1][0], inPos[1][1])
                .setBackground(itemSlotBackground, -1, -1)
                .setCustomRenderer(JeiFluidTypes.FLUID_STACK, new HbmFluidJeiRenderer(16, 16))
                .addFluidStack(ModFluids.NITRIC_ACID.getSource(), 100);

        for (int i = 0; i < outputs.size(); i++) {
            if (outputs.get(i).isEmpty()) continue;
            builder.addSlot(RecipeIngredientRole.OUTPUT, outPos[i][0], outPos[i][1])
                    .setBackground(itemSlotBackground, -1, -1)
                    .addItemStack(outputs.get(i));
        }

        builder.addSlot(RecipeIngredientRole.CATALYST, 75, 31)
                .addItemStacks(java.util.Arrays.asList(getMachines(e)));
    }
}
//?} else {
/*public final class ElectrolyserMetalJeiCategory {
    private ElectrolyserMetalJeiCategory() {}
}*///?}
