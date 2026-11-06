package com.hbm_m.compat.jei;

import java.util.List;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.item.ModItems;
import com.hbm_m.item.machine.ItemFELCrystal.EnumWavelengths;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.recipe.SilexRecipe;

import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * 1:1 Port of 1.7.10 {@code SILEXRecipeHandler}.
 *
 * <p>Uses {@code textures/gui/nei/gui_nei_silex.png} (166x65):
 * <ul>
 *   <li>Input stack at (12, 24)</li>
 *   <li>Transfer arrow at (42, 23) [24x18]</li>
 *   <li>Produced multiplier text at (52, 43), centered: {@code produced = fluidProduced / fluidConsumed}
 *   (int division, 1:1 with {@code RecipeSet}; vitriol 1000/300 &rarr; "3.0x")</li>
 *   <li>Required FEL laser wavelength text at (33, 8), centered, formatted with wavelength textColor</li>
 *   <li>Outputs formatted in 1 or 2 columns (sep = 2 if &le; 4 outputs, else 3) with percentage chances</li>
 * </ul>
 */
@OnlyIn(Dist.CLIENT)
public class SilexJeiCategory implements IRecipeCategory<SilexRecipe> {

    public static final RecipeType<SilexRecipe> RECIPE_TYPE =
            RecipeType.create(RefStrings.MODID, "silex", SilexRecipe.class);

    public static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/gui/nei/gui_nei_silex.png");

    public static final int RECIPE_WIDTH = 166;
    public static final int RECIPE_HEIGHT = 65;

    private final IDrawable background;
    private final IDrawable icon;

    public SilexJeiCategory(IGuiHelper guiHelper) {
        // In 1.7.10 NEI, TemplateRecipeHandler.drawBackground draws gui_nei_silex.png at (u=5, v=11).
        this.background = guiHelper.createDrawable(TEXTURE, 5, 11, RECIPE_WIDTH, RECIPE_HEIGHT);
        this.icon = guiHelper.createDrawableIngredient(VanillaTypes.ITEM_STACK, new ItemStack(ModBlocks.SILEX.get()));
    }

    public static EnumWavelengths getWavelength(SilexRecipe recipe) {
        if (recipe.isXray()) return EnumWavelengths.XRAY;
        int laser = recipe.getLaser();
        for (EnumWavelengths w : EnumWavelengths.values()) {
            if (w != EnumWavelengths.XRAY && w.tier == laser) return w;
        }
        return EnumWavelengths.NULL;
    }

    @Override
    public RecipeType<SilexRecipe> getRecipeType() {
        return RECIPE_TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatableWithFallback("jei.hbm_m.silex", "SILEX");
    }

    @Override
    @SuppressWarnings("removal")
    public IDrawable getBackground() {
        return background;
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }

    @Override
    public int getWidth() {
        return RECIPE_WIDTH;
    }

    @Override
    public int getHeight() {
        return RECIPE_HEIGHT;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, SilexRecipe recipe, IFocusGroup focuses) {
        // Input slot at (12, 24)
        if (!recipe.getInput().isEmpty()) {
            var slot = builder.addSlot(RecipeIngredientRole.INPUT, 12, 24);
            if (recipe.getPelletState() >= 0) {
                java.util.List<ItemStack> stacks = new java.util.ArrayList<>();
                for (ItemStack stack : recipe.getInput().getItems()) {
                    ItemStack copy = stack.copy();
                    com.hbm_m.item.rbmk.RBMKPelletItem.setState(copy, recipe.getPelletState());
                    stacks.add(copy);
                }
                slot.addItemStacks(stacks);
            } else {
                slot.addIngredients(recipe.getInput());
            }

            if (!recipe.getFluids().isEmpty()) {
                slot.addRichTooltipCallback((view, tooltip) -> {
                    for (ResourceLocation fluidId : recipe.getFluids()) {
                        Fluid fluid = BuiltInRegistries.FLUID.get(fluidId);
                        if (fluid != null) {
                            tooltip.add(Component.translatable("jei.hbm_m.silex.fluid_alternative",
                                    fluid.getFluidType().getDescription(), recipe.getFluidConsumed()));
                        }
                    }
                });
            }
        } else if (!recipe.getFluids().isEmpty()) {
            var slot = builder.addSlot(RecipeIngredientRole.INPUT, 12, 24);
            for (ResourceLocation fluidId : recipe.getFluids()) {
                Fluid fluid = BuiltInRegistries.FLUID.get(fluidId);
                if (fluid != null) {
                    // 16 000 mB = SILEX peroxide tank (original TileEntitySILEX line 56, port
                    // MachineSilexBlockEntity.tank), same sibling convention as GasCentrifuge (2000).
                    JeiFluidSlots.addFluid(slot, fluid, recipe.getFluidConsumed(), 16_000);
                }
            }
        }

        // Outputs in 1 or 2 columns (1:1 with 1.7.10 SILEXRecipeHandler.RecipeSet)
        List<SilexRecipe.WeightedOutput> outputs = recipe.getOutputs();
        int sep = outputs.size() > 4 ? 3 : 2;

        for (int i = 0; i < outputs.size(); i++) {
            SilexRecipe.WeightedOutput out = outputs.get(i);
            int x = (i < sep) ? 68 : 116;
            int colSize = (i < sep) ? Math.min(outputs.size(), sep) : outputs.size() - sep;
            int colIndex = (i < sep) ? i : i - sep;
            int y = 24 + colIndex * 18 - 9 * ((Math.min(colSize, sep) + 1) / 2);

            builder.addSlot(RecipeIngredientRole.OUTPUT, x, y)
                    .addItemStack(out.stack());
        }
    }

    @Override
    public void draw(SilexRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics guiGraphics,
                     double mouseX, double mouseY) {
        Font font = Minecraft.getInstance().font;

        // Produced multiplier under arrow at (52, 43): 1:1 with 1.7.10.
        // Original SILEXRecipeHandler.RecipeSet line 58: `this.produced = recipe.fluidProduced / recipe.fluidConsumed;`
        // with both fields int (SILEXRecipes.SILEXRecipe) — i.e. INTEGER division, so vitriol (1000/300)
        // displays "3.0x" like the original, not "3.3x".
        int produced = recipe.getFluidConsumed() > 0 ? recipe.getFluidProduced() / recipe.getFluidConsumed() : 0;
        String am = ((int) (produced * 10.0) / 10.0) + "x";
        guiGraphics.drawString(font, am, 52 - font.width(am) / 2, 43, 0x404040, false);

        // Required laser wavelength at (33, 8): 1:1 with 1.7.10
        EnumWavelengths wl = getWavelength(recipe);
        Component wavelength = (wl == EnumWavelengths.NULL)
                ? Component.literal("N/A").withStyle(ChatFormatting.WHITE)
                : Component.translatable(wl.name).withStyle(wl.textColor);
        guiGraphics.drawString(font, wavelength, 33 - font.width(wavelength) / 2, 8, 0x404040, false);

        // Output percentage chances next to each output stack: 1:1 with 1.7.10
        List<SilexRecipe.WeightedOutput> outputs = recipe.getOutputs();
        int totalWeight = recipe.getTotalWeight();
        int sep = outputs.size() > 4 ? 3 : 2;

        for (int i = 0; i < outputs.size(); i++) {
            SilexRecipe.WeightedOutput out = outputs.get(i);
            int x = (i < sep) ? 68 : 116;
            int colSize = (i < sep) ? Math.min(outputs.size(), sep) : outputs.size() - sep;
            int colIndex = (i < sep) ? i : i - sep;
            int y = 24 + colIndex * 18 - 9 * ((Math.min(colSize, sep) + 1) / 2);

            double chance = totalWeight > 0 ? (100.0 * out.weight() / totalWeight) : 0.0;
            String chanceStr = ((int) (chance * 10.0) / 10.0) + "%";
            guiGraphics.drawString(font, chanceStr, x + 18, y + 4, 0x404040, false);
        }
    }
}
