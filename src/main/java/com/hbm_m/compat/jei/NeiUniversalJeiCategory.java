package com.hbm_m.compat.jei;
//? if forge || neoforge {

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.annotation.Nullable;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;

/**
 * 1:1 {@code NEIUniversalHandler} als frei belegbare JEI-Kategorie: Eingaenge links (Original
 * {@code getInputCoords}), Ergebnisse rechts ({@code getOutputCoords}), Maschine im hohen Rahmen
 * bei (75, 31). Jede Original-Unterklasse, die nur eine Rezeptliste mitbringt (Werkzeugumbau,
 * Bauplaene, Brennstoffe, Kessel ...), wird damit zu einer Instanz statt einer eigenen Klasse.
 *
 * <p>Fluessigkeiten stehen wie im Original als eigenes Feld ({@code ItemFluidIcon}); die rote
 * Zusatzzeile von {@code ItemStackUtil.addTooltipToStack} wird zur Tooltipzeile des Feldes.
 * {@code machineOverrides} des Originals entspricht {@link Entry#machines()}.</p>
 */
public class NeiUniversalJeiCategory implements IRecipeCategory<NeiUniversalJeiCategory.Entry> {

    /** Ein Feld: entweder durchwechselnde Gegenstaende oder eine Fluessigkeit, dazu Tooltipzeilen. */
    public record Slot(List<ItemStack> items, @Nullable Fluid fluid, int amount, List<Component> tooltip) {

        public static Slot of(ItemStack... stacks) {
            return new Slot(List.of(stacks), null, 0, List.of());
        }

        public static Slot of(List<ItemStack> stacks) {
            return new Slot(List.copyOf(stacks), null, 0, List.of());
        }

        public static Slot fluid(Fluid fluid, int amount) {
            return new Slot(List.of(), fluid, amount, List.of());
        }

        public Slot tip(Component line) {
            List<Component> lines = new ArrayList<>(tooltip);
            lines.add(line);
            return new Slot(items, fluid, amount, lines);
        }

        public boolean isEmpty() {
            return fluid == null && items.stream().allMatch(ItemStack::isEmpty);
        }
    }

    /** Ein Rezept; {@code machines} ersetzt fuer dieses Rezept die Maschine (null = Kategorie-Maschine). */
    public record Entry(List<Slot> inputs, List<Slot> outputs, @Nullable List<ItemStack> machines) {
        public Entry(List<Slot> inputs, List<Slot> outputs) {
            this(inputs, outputs, null);
        }
    }

    private final RecipeType<Entry> type;
    private final Component title;
    private final ItemStack[] machines;
    private final IDrawable background;
    private final IDrawable slotBackground;
    private final IDrawable icon;

    public NeiUniversalJeiCategory(IGuiHelper guiHelper, RecipeType<Entry> type, Component title, ItemStack... machines) {
        this.type = type;
        this.title = title;
        this.machines = machines;
        this.background = JeiNeiTextures.createRecipeBackground(guiHelper);
        this.slotBackground = JeiNeiTextures.createItemSlotBackground(guiHelper);
        this.icon = guiHelper.createDrawableItemStack(machines.length > 0 ? machines[0] : ItemStack.EMPTY);
    }

    public ItemStack[] getMachines() {
        return machines;
    }

    @Override public RecipeType<Entry> getRecipeType() { return type; }
    @Override public Component getTitle() { return title; }
    @Override public IDrawable getIcon() { return icon; }
    @Override public int getWidth() { return JeiNeiTextures.RECIPE_WIDTH; }
    @Override public int getHeight() { return JeiNeiTextures.RECIPE_HEIGHT; }

    @Override
    @SuppressWarnings("removal")
    public IDrawable getBackground() {
        return background;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, Entry recipe, IFocusGroup focuses) {
        int[][] inPos = JeiNeiLayout.getUniversalInputCoords(recipe.inputs().size());
        for (int i = 0; i < recipe.inputs().size(); i++) {
            addSlot(builder, RecipeIngredientRole.INPUT, inPos[i][0], inPos[i][1], recipe.inputs().get(i), slotBackground);
        }

        int[][] outPos = JeiNeiLayout.getUniversalOutputCoords(recipe.outputs().size());
        for (int i = 0; i < recipe.outputs().size(); i++) {
            addSlot(builder, RecipeIngredientRole.OUTPUT, outPos[i][0], outPos[i][1], recipe.outputs().get(i), slotBackground);
        }

        List<ItemStack> machine = recipe.machines() != null ? recipe.machines() : Arrays.asList(machines);
        builder.addSlot(RecipeIngredientRole.CATALYST, 75, 31).addItemStacks(machine);
    }

    /** Legt ein Feld an; gemeinsam fuer alle Kategorien mit eigener Textur. */
    static IRecipeSlotBuilder addSlot(IRecipeLayoutBuilder builder, RecipeIngredientRole role, int x, int y,
                                      Slot slot, @Nullable IDrawable frame) {
        IRecipeSlotBuilder b = builder.addSlot(role, x, y);
        if (frame != null) b.setBackground(frame, -1, -1);
        if (slot.fluid() != null) {
            b.setCustomRenderer(JeiFluidTypes.FLUID_STACK, new HbmFluidJeiRenderer(16, 16))
                    .addFluidStack(slot.fluid(), Math.max(slot.amount(), 1));
        } else {
            b.addItemStacks(slot.items());
        }
        if (!slot.tooltip().isEmpty()) {
            b.addRichTooltipCallback((view, tooltip) -> { for (Component c : slot.tooltip()) tooltip.add(c); });
        }
        return b;
    }

    @Override
    public void draw(Entry recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics guiGraphics, double mouseX, double mouseY) {
        // Original drawBackground: drawTexturedModalRect(74, 14, 59, 87, 18, 36)
        JeiNeiRendering.drawMachineSlot(guiGraphics, 0, false);
    }
}
//?} else {
/*public final class NeiUniversalJeiCategory {
    private NeiUniversalJeiCategory() {}
}*///?}
