package com.hbm_m.compat.jei;

import java.util.ArrayList;
import java.util.List;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.compat.jei.NeiUniversalJeiCategory.Slot;
import com.hbm_m.item.ModItems;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.platform.recipe.RecipeHooks;
import com.hbm_m.recipe.CrackingTowerRecipe;
import com.hbm_m.recipe.RadiolysisRecipe;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.drawable.IDrawableAnimated;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * 1:1 port of the 1.7.10 {@code RadiolysisRecipeHandler} (Custom-{@code TemplateRecipeHandler},
 * kein {@code NEIGenericRecipeHandler}-Nachfahre).
 *
 * <p>Eigene Textur {@code textures/gui/nei/gui_nei_radiolysis.png} (256x256, Panel 166x65 bei
 * u=5, v=11): Eingang bei (34, 25), beide Ausgaenge uebereinander bei (118, 16) und (118, 34) -
 * die Slotrahmen stecken in der Textur. {@code drawExtras} zeichnet
 * {@code drawProgressBar(52, 19, 5, 87, 64, 28, 60, 0)} - die 64x28-Wellenfuellung aus der
 * gleichen Textur, 60 Ticks Zyklus, von links fuellend.</p>
 *
 * <p>Rezeptset wie {@code RadiolysisRecipes.getRecipesForNEI()}: der eigene Wassereintrag
 * ({@link RadiolysisRecipe}, 100mB Wasser &rarr; 80mB Peroxid + 20mB Wasserstoff) gefolgt von
 * allen Cracking-Rezepten ({@link CrackingTowerRecipe}) - die Maschine faellt dorthin zurueck
 * ({@code MachineRadiolysisBlockEntity}), die Dampf-Spalten der Cracking-Tower-Kategorie gibt
 * es hier wie im Original nicht. Ein fehlender zweiter Ausgang ({@code Fluids.NONE}, Kerosin)
 * erscheint wie im Original als {@code nothing}-Item.</p>
 */
@OnlyIn(Dist.CLIENT)
public class RadiolysisJeiCategory implements IRecipeCategory<RadiolysisJeiCategory.Entry> {

    /** Ein Radiolysis-Rezept: Eingangsfluid und die beiden Original-NEI-Ausgaenge. */
    public record Entry(Slot input, Slot outputA, Slot outputB) {

        /** Eigener Eintrag aus {@code hbm_m:radiolysis} (Original: {@code RadiolysisRecipes}). */
        public static Entry own(RadiolysisRecipe recipe) {
            return new Entry(
                    Slot.fluid(recipe.getInputFluid(), recipe.getInputMb()),
                    Slot.fluid(recipe.getOutputA(), recipe.getOutputAMb()),
                    recipe.hasOutputB() ? Slot.fluid(recipe.getOutputB(), recipe.getOutputBMb())
                            : Slot.of(new ItemStack(ModItems.NOTHING.get())));
        }

        /** Cracking-Eintrag aus {@code hbm_m:cracking_tower} (Original: {@code CrackingRecipes}-Map). */
        public static Entry crack(CrackingTowerRecipe recipe) {
            return new Entry(
                    Slot.fluid(recipe.getInputFluid(), recipe.getInputMb()),
                    Slot.fluid(recipe.getOutputA(), recipe.getOutputAMb()),
                    recipe.hasOutputB() ? Slot.fluid(recipe.getOutputB(), recipe.getOutputBMb())
                            : Slot.of(new ItemStack(ModItems.NOTHING.get())));
        }
    }

    public static final RecipeType<Entry> RECIPE_TYPE =
            RecipeType.create(RefStrings.MODID, "radiolysis", Entry.class);

    public static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/gui/nei/gui_nei_radiolysis.png");

    public static final int RECIPE_WIDTH = 166;
    public static final int RECIPE_HEIGHT = 65;

    private final IDrawable background;
    private final IDrawable icon;
    private final IDrawableAnimated progress;

    public RadiolysisJeiCategory(IGuiHelper guiHelper) {
        // TemplateRecipeHandler.drawBackground: drawTexturedModalRect(0, 0, 5, 11, 166, 65)
        this.background = guiHelper.drawableBuilder(TEXTURE, 5, 11, RECIPE_WIDTH, RECIPE_HEIGHT)
                .setTextureSize(256, 256).build();
        this.icon = guiHelper.createDrawableItemStack(new ItemStack(ModBlocks.RADIOLYSIS.get()));
        // drawProgressBar(52, 19, 5, 87, 64, 28, 60, 0): Richtung 0 = von links fuellend
        this.progress = guiHelper.drawableBuilder(TEXTURE, 5, 87, 64, 28).setTextureSize(256, 256)
                .buildAnimated(60, IDrawableAnimated.StartDirection.LEFT, false);
    }

    /** {@code RadiolysisRecipes.getRecipesForNEI()}: Wassereintrag zuerst, danach alle Cracking-Rezepte. */
    public static List<Entry> recipes(Level level) {
        List<Entry> list = new ArrayList<>();
        for (RadiolysisRecipe recipe : RecipeHooks.getAllRecipes(level, RadiolysisRecipe.Type.INSTANCE)) list.add(Entry.own(recipe));
        for (CrackingTowerRecipe recipe : RecipeHooks.getAllRecipes(level, CrackingTowerRecipe.Type.INSTANCE)) list.add(Entry.crack(recipe));
        return list;
    }

    @Override
    public RecipeType<Entry> getRecipeType() {
        return RECIPE_TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatableWithFallback("jei.hbm_m.radiolysis", "Radiolysis");
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
    public void setRecipe(IRecipeLayoutBuilder builder, Entry recipe, IFocusGroup focuses) {
        // RecipeSet: input (34, 25), output1 (118, 16), output2 (118, 34); Rahmen stecken in der Textur.
        NeiUniversalJeiCategory.addSlot(builder, RecipeIngredientRole.INPUT, 34, 25, recipe.input(), null);
        NeiUniversalJeiCategory.addSlot(builder, RecipeIngredientRole.OUTPUT, 118, 16, recipe.outputA(), null);
        NeiUniversalJeiCategory.addSlot(builder, RecipeIngredientRole.OUTPUT, 118, 34, recipe.outputB(), null);
    }

    @Override
    public void draw(Entry recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics guiGraphics, double mouseX, double mouseY) {
        progress.draw(guiGraphics, 52, 19);
    }
}
