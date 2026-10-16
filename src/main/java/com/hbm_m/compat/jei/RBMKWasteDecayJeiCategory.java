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
 * <p>The original enumerates every {@code WasteClass} metadata variant; this port collapsed those
 * into plain items (see {@code MachineStorageDrumBlockEntity} and {@code RadGenRecipes} for that
 * scope decision), so the same listing here is the four item pairs those variants reduce to.</p>
 */
public class RBMKWasteDecayJeiCategory extends JeiGenericRecipeCategory<RBMKWasteDecayJeiCategory.Decay> {

    public record Decay(ItemStack input, ItemStack output) {}

    public static final RecipeType<Decay> RECIPE_TYPE =
            RecipeType.create(RefStrings.MODID, "rbmk_waste_decay", Decay.class);

    public RBMKWasteDecayJeiCategory(IGuiHelper guiHelper) {
        super(guiHelper, new ItemStack[]{ new ItemStack(ModBlocks.MACHINE_STORAGE_DRUM.get()) });
    }

    public static List<Decay> all() {
        return List.of(
                new Decay(new ItemStack(ModItems.NUCLEAR_WASTE_SHORT.get()),
                          new ItemStack(ModItems.NUCLEAR_WASTE_SHORT_DEPLETED.get())),
                new Decay(new ItemStack(ModItems.NUCLEAR_WASTE_SHORT_TINY.get()),
                          new ItemStack(ModItems.NUCLEAR_WASTE_SHORT_DEPLETED_TINY.get())),
                new Decay(new ItemStack(ModItems.NUCLEAR_WASTE_LONG.get()),
                          new ItemStack(ModItems.NUCLEAR_WASTE_LONG_DEPLETED.get())),
                new Decay(new ItemStack(ModItems.NUCLEAR_WASTE_LONG_TINY.get()),
                          new ItemStack(ModItems.NUCLEAR_WASTE_LONG_DEPLETED_TINY.get()))
        );
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
