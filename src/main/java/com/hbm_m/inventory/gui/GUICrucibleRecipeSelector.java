package com.hbm_m.inventory.gui;

import java.util.List;

import com.hbm_m.blockentity.machines.MachineCrucibleBlockEntity;
import com.hbm_m.inventory.recipes.CrucibleRecipes;
import com.hbm_m.inventory.recipes.CrucibleRecipes.CrucibleRecipe;
import com.hbm_m.network.NBTControlPacket;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;

/**
 * Rezeptwaehler fuer den Schmelztiegel (Original: {@code GUIScreenRecipeSelector.openSelector(CrucibleRecipes, ...)}):
 * alle Legierungsrezepte als Symbole mit ihrem Rezeptausdruck als Tooltip, ein leeres Feld loescht die Auswahl.
 * Gewaehlt wird ueber das Steuerpaket mit {@code index 0} und {@code selection = Rezeptname}.
 */
public class GUICrucibleRecipeSelector extends Screen {

    private static final int COLS = 8;
    private static final int CELL = 18;

    private final MachineCrucibleBlockEntity crucible;
    private final Screen previous;
    private int left;
    private int top;

    public GUICrucibleRecipeSelector(MachineCrucibleBlockEntity crucible, Screen previous) {
        super(Component.translatable("gui.recipe.setRecipe"));
        this.crucible = crucible;
        this.previous = previous;
    }

    private List<CrucibleRecipe> recipes() {
        return CrucibleRecipes.all();
    }

    private int rows() {
        return (recipes().size() + 1 + COLS - 1) / COLS;
    }

    @Override
    protected void init() {
        int w = COLS * CELL + 16;
        int h = rows() * CELL + 30;
        this.left = (this.width - w) / 2;
        this.top = (this.height - h) / 2;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        com.hbm_m.client.GuiCompat.renderBackground(this, g, mouseX, mouseY, partialTick);
        int w = COLS * CELL + 16;
        int h = rows() * CELL + 30;
        g.fill(left, top, left + w, top + h, 0xFF2B2B2B);
        g.fill(left + 1, top + 1, left + w - 1, top + h - 1, 0xFF4A4A4A);
        g.drawString(this.font, this.title, left + 8, top + 7, 0xFFFFFF, false);

        List<CrucibleRecipe> list = recipes();
        Component tooltip = null;
        List<Component> recipeTooltip = null;

        for (int i = 0; i <= list.size(); i++) {
            int cx = left + 8 + (i % COLS) * CELL;
            int cy = top + 20 + (i / COLS) * CELL;
            boolean selected = i == 0 ? CrucibleRecipes.get(crucible.recipe) == null : list.get(i - 1).name.equals(crucible.recipe);
            g.fill(cx, cy, cx + 18, cy + 18, selected ? 0xFF8BCF5A : 0xFF8B8B8B);
            g.fill(cx + 1, cy + 1, cx + 17, cy + 17, 0xFF373737);

            if (i > 0) g.renderItem(list.get(i - 1).getIcon(), cx + 1, cy + 1);
            else g.drawString(this.font, "X", cx + 6, cy + 5, 0xFF5555, false);

            if (mouseX >= cx && mouseX < cx + 18 && mouseY >= cy && mouseY < cy + 18) {
                if (i == 0) tooltip = Component.literal("Clear").withStyle(ChatFormatting.RED);
                else recipeTooltip = list.get(i - 1).print(Screen.hasShiftDown());
            }
        }

        super.render(g, mouseX, mouseY, partialTick);

        if (tooltip != null) g.renderTooltip(this.font, tooltip, mouseX, mouseY);
        if (recipeTooltip != null) g.renderComponentTooltip(this.font, recipeTooltip, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        List<CrucibleRecipe> list = recipes();
        for (int i = 0; i <= list.size(); i++) {
            int cx = left + 8 + (i % COLS) * CELL;
            int cy = top + 20 + (i / COLS) * CELL;
            if (mouseX >= cx && mouseX < cx + 18 && mouseY >= cy && mouseY < cy + 18) {
                String selection = i == 0 ? "null" : list.get(i - 1).name;
                CompoundTag data = new CompoundTag();
                data.putInt("index", 0);
                data.putString("selection", selection);
                NBTControlPacket.sendToServer(crucible.getBlockPos(), data);
                crucible.recipe = selection;
                Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
                onClose();
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(previous);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
