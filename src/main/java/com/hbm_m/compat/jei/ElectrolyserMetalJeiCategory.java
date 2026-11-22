package com.hbm_m.compat.jei;

import java.util.Arrays;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.inventory.fluid.ModFluids;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.recipe.ElectrolyserMetalRecipe;

import dev.architectury.fluid.FluidStack;
//? if forge {
import mezz.jei.api.forge.ForgeTypes;
import dev.architectury.hooks.fluid.forge.FluidStackHooksForge;
//?} elif neoforge {
/*import mezz.jei.api.neoforge.NeoForgeTypes;
*///?}
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * JEI category for Electrolyser metal-mode recipes - Kristall + 100mB Salpetersaeure -> 2 Metall-Ausgaenge +
 * Byprodukt-Items (siehe {@link ElectrolyserMetalRecipe}).
 *
 * <p>Wie im 1.7.10-NEI ({@code ElectrolyserMetalRecipes.getRecipes}) wird die Salpetersaeure als zweiter
 * Eingang gezeigt — die Maschine verbraucht 100mB pro Durchlauf (Tank 3).</p>
 *
 * <p>Data-driven: рецепты читаются напрямую aus {@code RecipeManager} (JSON {@code hbm_m:electrolyser_metal}),
 * ранее — статический {@code ElectrolyserRecipes} (metal-mode).</p>
 */
public class ElectrolyserMetalJeiCategory extends JeiGenericRecipeCategory<ElectrolyserMetalRecipe> {

    public static final RecipeType<ElectrolyserMetalRecipe> RECIPE_TYPE =
            RecipeType.create(RefStrings.MODID, "electrolyser_metal", ElectrolyserMetalRecipe.class);

    private static final int FLUID_RENDERER_CAPACITY = 4_000;
    /** 100mB Salpetersaeure pro Durchlauf, wie im Original (NEI-Eingang 2 + Tank 3 der Maschine). */
    private static final long NITRIC_ACID_PER_OPERATION = 100;

    public ElectrolyserMetalJeiCategory(IGuiHelper guiHelper) {
        super(guiHelper, new ItemStack[]{
                new ItemStack(ModBlocks.ELECTROLYSER.get())
        });
    }

    @Override
    public RecipeType<ElectrolyserMetalRecipe> getRecipeType() {
        return RECIPE_TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("container.hbm_m.electrolyser");
    }

    @Override
    protected int getInputCount(ElectrolyserMetalRecipe recipe) {
        // Kristall + 100mB Salpetersaeure — wie im 1.7.10-NEI (zwei Eingangs-Slots).
        return 2;
    }

    @Override
    protected int getOutputCount(ElectrolyserMetalRecipe recipe) {
        int count = 0;
        if (!recipe.getOutputA().isEmpty()) count++;
        if (!recipe.getOutputB().isEmpty()) count++;
        count += recipe.getByproducts().length;
        return count;
    }

    @Override
    protected boolean hasBlueprintTemplate(ElectrolyserMetalRecipe recipe) {
        return false;
    }

    @Override
    protected void addInputSlots(IRecipeLayoutBuilder builder, ElectrolyserMetalRecipe recipe, int inputXOffset) {
        // ElectrolyserMetalHandler extends NEIUniversalHandler: dessen getInputCoords(2) setzt den
        // Kristall auf {48,24} (direkt am Pfeil) und die Salpetersaeure auf {30,24} — wie in 1.7.10.
        int[][] positions = JeiNeiLayout.getUniversalInputCoords(2);

        // Ingredient-вход (кристалл): NEI чередовал все варианты ингредиента — показываем все.
        ItemStack[] matching = recipe.getInput().getItems();
        if (matching != null && matching.length > 0) {
            addItemSlot(builder, RecipeIngredientRole.INPUT,
                    positions[0][0] + inputXOffset, positions[0][1])
                    .addItemStacks(Arrays.asList(matching));
        }

        addFluidSlot(builder, RecipeIngredientRole.INPUT,
                positions[1][0] + inputXOffset, positions[1][1],
                FluidStack.create(ModFluids.NITRIC_ACID.getSource(), NITRIC_ACID_PER_OPERATION));
    }

    private void addFluidSlot(IRecipeLayoutBuilder builder, RecipeIngredientRole role, int x, int y, FluidStack fluid) {
        addItemSlot(builder, role, x, y)
                .setFluidRenderer(FLUID_RENDERER_CAPACITY, false, 16, 16)
                //? if forge {
                .setCustomRenderer(ForgeTypes.FLUID_STACK, new HbmFluidJeiRenderer(16, 16))
                .addIngredient(ForgeTypes.FLUID_STACK, FluidStackHooksForge.toForge(fluid));
                //?} elif neoforge {
                /*.setCustomRenderer(NeoForgeTypes.FLUID_STACK, new HbmFluidJeiRenderer(16, 16))
                .addIngredient(NeoForgeTypes.FLUID_STACK, new net.minecraftforge.fluids.FluidStack(fluid.getFluid(), (int) fluid.getAmount()));
                *///?}
    }

    @Override
    protected void addOutputSlots(IRecipeLayoutBuilder builder, ElectrolyserMetalRecipe recipe, int outputXOffset) {
        int outputCount = getOutputCount(recipe);
        int[][] positions = JeiNeiLayout.getGenericOutputSlotPositions(outputCount);
        int slotIndex = 0;

        if (!recipe.getOutputA().isEmpty()) {
            addItemSlot(builder, RecipeIngredientRole.OUTPUT, positions[slotIndex][0] + outputXOffset, positions[slotIndex][1])
                    .addItemStack(recipe.getOutputA());
            slotIndex++;
        }
        if (!recipe.getOutputB().isEmpty()) {
            addItemSlot(builder, RecipeIngredientRole.OUTPUT, positions[slotIndex][0] + outputXOffset, positions[slotIndex][1])
                    .addItemStack(recipe.getOutputB());
            slotIndex++;
        }
        for (ItemStack byproduct : recipe.getByproducts()) {
            addItemSlot(builder, RecipeIngredientRole.OUTPUT, positions[slotIndex][0] + outputXOffset, positions[slotIndex][1])
                    .addItemStack(byproduct);
            slotIndex++;
        }
    }

    @Override
    protected void addBlueprintSlot(IRecipeLayoutBuilder builder, ElectrolyserMetalRecipe recipe, int machineXOffset) {
        // Kein Blueprint-Slot fuer Electrolyser-Metal-Rezepte.
    }
}
