package com.hbm_m.compat.jei;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.item.ModItems;
import com.hbm_m.lib.RefStrings;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * JEI listing for nuclear waste decaying inside a storage drum, ported from the original's
 * {@code RBMKWasteDecayHandler}.
 *
 * <p>Wie im Original steht hier jede Abfallklasse einzeln: welcher Muell zu welchem
 * abgereicherten Stueck wird, haengt an der Klasse, und nur die entscheidet auch darueber, wieviel
 * Fluessigkeit und Gas das Fass dabei abwirft.</p>
 */
public class RBMKWasteDecayJeiCategory extends JeiGenericRecipeCategory<RBMKWasteDecayJeiCategory.Decay> {

    public record Decay(ItemStack input, ItemStack output) {}

    public static final RecipeType<Decay> RECIPE_TYPE =
            RecipeType.create(RefStrings.MODID, "rbmk_waste_decay", Decay.class);

    public RBMKWasteDecayJeiCategory(IGuiHelper guiHelper) {
        super(guiHelper, new ItemStack[]{ new ItemStack(ModBlocks.MACHINE_STORAGE_DRUM.get()) });
    }

    public static List<Decay> all() {
        List<Decay> out = new java.util.ArrayList<>();
        addGroup(out, "nw_short", "nw_short_dep");
        addGroup(out, "nw_short_tiny", "nw_short_dep_tiny");
        addGroup(out, "nw_long", "nw_long_dep");
        addGroup(out, "nw_long_tiny", "nw_long_dep_tiny");
        return out;
    }

    /** Frische und abgereicherte Gruppe stehen in derselben Klassenreihenfolge. */
    private static void addGroup(List<Decay> out, String fresh, String spent) {
        var freshItems = com.hbm_m.item.PartTabMetaItems.group(fresh);
        var spentItems = com.hbm_m.item.PartTabMetaItems.group(spent);

        for (int i = 0; i < freshItems.size() && i < spentItems.size(); i++) {
            out.add(new Decay(new ItemStack(freshItems.get(i)), new ItemStack(spentItems.get(i))));
        }
    }

    @Override public RecipeType<Decay> getRecipeType() { return RECIPE_TYPE; }

    @Override
    public Component getTitle() {
        return Component.translatable("gui.hbm_m.jei.rbmk_waste_decay");
    }

    @Override protected int getInputCount(Decay recipe)  { return 1; }
    @Override protected int getOutputCount(Decay recipe) { return 1; }
    @Override protected boolean hasBlueprintTemplate(Decay recipe) { return false; }

    @Override
    protected void addInputSlots(IRecipeLayoutBuilder builder, Decay recipe, int inputXOffset) {
        int[][] positions = JeiNeiLayout.getGenericInputSlotPositions(1);
        addItemSlot(builder, RecipeIngredientRole.INPUT, positions[0][0] + inputXOffset, positions[0][1])
                .addItemStack(recipe.input());
    }

    @Override
    protected void addOutputSlots(IRecipeLayoutBuilder builder, Decay recipe, int outputXOffset) {
        int[][] positions = JeiNeiLayout.getGenericOutputSlotPositions(1);
        addItemSlot(builder, RecipeIngredientRole.OUTPUT, positions[0][0] + outputXOffset, positions[0][1])
                .addItemStack(recipe.output());
    }

    @Override
    protected void addBlueprintSlot(IRecipeLayoutBuilder builder, Decay recipe, int machineXOffset) {
        // Decay happens on its own inside the drum; no blueprint.
    }
}
