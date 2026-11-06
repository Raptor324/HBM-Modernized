package com.hbm_m.compat.jei;

import com.hbm_m.api.fluids.VanillaFluidEquivalence;
import com.hbm_m.block.ModBlocks;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.recipe.CompressorRecipe;

import dev.architectury.fluid.FluidStack;
//? if forge {
import mezz.jei.api.forge.ForgeTypes;
import dev.architectury.hooks.fluid.forge.FluidStackHooksForge;
//?} elif neoforge {
/*import mezz.jei.api.neoforge.NeoForgeTypes;
*///?}
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * JEI category for Compressor special-case recipes (siehe {@link CompressorRecipe} - der
 * generische "immer +1 Druck"-Fallback ist selbsterklaerend und wird nicht als JEI-Rezept gezeigt).
 *
 * <p>Data-driven: рецепты читаются напрямую из {@code RecipeManager} (JSON {@code hbm_m:compressor}),
 * ранее — статический {@code CompressorRecipes}.</p>
 */
public class CompressorJeiCategory extends JeiGenericRecipeCategory<CompressorRecipe> {

    public static final RecipeType<CompressorRecipe> RECIPE_TYPE =
            RecipeType.create(RefStrings.MODID, "compressor", CompressorRecipe.class);

    private static final int FLUID_RENDERER_CAPACITY = 16_000;

    public CompressorJeiCategory(IGuiHelper guiHelper) {
        // Оригинальный CompressorHandler передавал ОБЕ машины (machine_compressor +
        // machine_compressor_compact) — NEI циклически показывал их в слоте машины.
        super(guiHelper, new ItemStack[]{
                new ItemStack(ModBlocks.COMPRESSOR.get()),
                new ItemStack(ModBlocks.COMPRESSOR_COMPACT.get())
        });
    }

    @Override
    public RecipeType<CompressorRecipe> getRecipeType() {
        return RECIPE_TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("container.hbm_m.compressor");
    }

    /**
     * Фильтр отображения (порт {@code CompressorRecipes.getRecipes()}, строка
     * {@code if(input.getItemDamage() == output.getItemDamage()) continue;}): damage иконки
     * жидкости — ordinal самого флюида, поэтому оригинальный NEI скрывал рецепты, у которых
     * входная и выходная жидкость совпадают. Это ровно генерический сценарий «сжать ту же
     * жидкость на +1 давление», машина обрабатывает такие рецепты и без спец-рецепта —
     * они остаются в {@code RecipeManager}, но в JEI скрыты, как в оригинальном NEI.
     */
    public static boolean isVisible(CompressorRecipe recipe) {
        return !VanillaFluidEquivalence.sameSubstance(recipe.getInputFluid(), recipe.getOutputFluid());
    }

    @Override
    protected int getInputCount(CompressorRecipe recipe) {
        return 1;
    }

    @Override
    protected int getOutputCount(CompressorRecipe recipe) {
        return 1;
    }

    @Override
    protected boolean hasBlueprintTemplate(CompressorRecipe recipe) {
        return false;
    }

    @Override
    protected void addInputSlots(IRecipeLayoutBuilder builder, CompressorRecipe recipe, int inputXOffset) {
        // CompressorHandler extends NEIUniversalHandler: dessen getInputCoords(1) legt das
        // Eingangsfluid auf {48,24} (direkt am Pfeil) — in JeiNeiLayout.getUniversalInputCoords gespiegelt.
        int[][] positions = JeiNeiLayout.getUniversalInputCoords(1);
        addFluidSlot(builder, RecipeIngredientRole.INPUT,
                positions[0][0] + inputXOffset, positions[0][1],
                FluidStack.create(recipe.getInputFluid(), recipe.getInputMb()),
                recipe.getInputPressure());
    }

    @Override
    protected void addOutputSlots(IRecipeLayoutBuilder builder, CompressorRecipe recipe, int outputXOffset) {
        int[][] positions = JeiNeiLayout.getUniversalOutputCoords(1);
        addFluidSlot(builder, RecipeIngredientRole.OUTPUT,
                positions[0][0] + outputXOffset, positions[0][1],
                FluidStack.create(recipe.getOutputFluid(), recipe.getOutputMb()),
                recipe.getOutputPressure());
    }

    private void addFluidSlot(IRecipeLayoutBuilder builder, RecipeIngredientRole role, int x, int y,
            FluidStack fluid, int pressure) {
        IRecipeSlotBuilder slot = addItemSlot(builder, role, x, y)
                .setFluidRenderer(FLUID_RENDERER_CAPACITY, false, 16, 16)
                //? if forge {
                .setCustomRenderer(ForgeTypes.FLUID_STACK, new HbmFluidJeiRenderer(16, 16))
                .addIngredient(ForgeTypes.FLUID_STACK, FluidStackHooksForge.toForge(fluid));
                //?} elif neoforge {
                /*.setCustomRenderer(NeoForgeTypes.FLUID_STACK, new HbmFluidJeiRenderer(16, 16))
                .addIngredient(NeoForgeTypes.FLUID_STACK, new net.minecraftforge.fluids.FluidStack(fluid.getFluid(), (int) fluid.getAmount()));
                *///?}
        // Original ItemFluidIcon.addInformation: rote Zeile "<N>PU" plus blinkende
        // "Pressurized, use compressor!"-Zeile (ROT/DUNKELROT, 500ms-Blink) bei Druck > 0.
        if (pressure > 0) {
            Component pu = Component.literal(pressure + "PU").withStyle(net.minecraft.ChatFormatting.RED);
            slot.addRichTooltipCallback((view, tooltip) -> {
                tooltip.add(pu);
                boolean blink = (System.currentTimeMillis() / 500) % 2 == 0;
                tooltip.add(Component.translatable("gui.hbm_m.fluid_tank.pressurized")
                        .withStyle(blink ? net.minecraft.ChatFormatting.RED : net.minecraft.ChatFormatting.DARK_RED));
            });
        }
    }

    @Override
    protected void addBlueprintSlot(IRecipeLayoutBuilder builder, CompressorRecipe recipe, int machineXOffset) {
        // Kein Blueprint-Slot fuer Compressor-Rezepte.
    }
}
