package com.hbm_m.inventory.gui;

import com.hbm_m.blockentity.machines.MachineReactorControlBlockEntity.RodFunction;
import com.hbm_m.inventory.menu.MachineReactorControlMenu;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.network.SetReactorControlC2SPacket;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;

/**
 * GUI des Reaktorsteuerpults, 1:1 aus {@code GUIReactorControl} (1.7.10).
 *
 * <p>Vier Eingabefelder (obere/untere Stabstellung, obere/untere Waermeschwelle), darunter die
 * Uebernahmeflaeche, links drei Flaechen fuer die Kennlinie. Die beiden Waermefelder zeigen den
 * Wert wie im Original geteilt durch 50 und rechnen ihn beim Uebernehmen wieder hoch.</p>
 */
public class GUIReactorControl extends GuiInfoScreen<MachineReactorControlMenu> {

    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            RefStrings.MODID, "textures/gui/gui_reactor_control.png");

    /** Original: {@code heatUpper / 50} in der Anzeige, beim Senden wieder {@code * 50}. */
    private static final int HEAT_SCALE = 50;

    private static final int FIELD_Y_LEVEL = 38;
    private static final int FIELD_Y_HEAT = 49;
    private static final int FIELD_X = 35;
    private static final int FIELD_DX = 30;
    private static final int FIELD_W = 26;
    private static final int FIELD_H = 7;

    private static final int APPLY_X = 33, APPLY_Y = 59, APPLY_W = 58, APPLY_H = 10;
    private static final int FUNC_X = 7, FUNC_Y = 37, FUNC_W = 22, FUNC_H = 10, FUNC_DY = 11;

    /** 0 = levelUpper, 1 = levelLower, 2 = heatUpper, 3 = heatLower - Reihenfolge wie im Original. */
    private final EditBox[] fields = new EditBox[4];

    public GUIReactorControl(MachineReactorControlMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
    }

    @Override
    protected void init() {
        super.init();

        for (int i = 0; i < 2; i++) {
            fields[i] = makeField(FIELD_X + FIELD_DX * i, FIELD_Y_LEVEL, 3);
            fields[i + 2] = makeField(FIELD_X + FIELD_DX * i, FIELD_Y_HEAT, 4);
        }

        fields[0].setValue(String.valueOf(menu.getLevelUpper()));
        fields[1].setValue(String.valueOf(menu.getLevelLower()));
        fields[2].setValue(String.valueOf(menu.getHeatUpper() / HEAT_SCALE));
        fields[3].setValue(String.valueOf(menu.getHeatLower() / HEAT_SCALE));
    }

    private EditBox makeField(int x, int y, int maxLength) {
        EditBox box = new EditBox(this.font, this.leftPos + x, this.topPos + y, FIELD_W, FIELD_H, Component.empty());
        box.setMaxLength(maxLength);
        box.setBordered(false);
        box.setTextColor(0x08FF00);
        box.setFilter(s -> s.chars().allMatch(Character::isDigit));
        addRenderableWidget(box);
        return box;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int x = (int) mouseX;
        int y = (int) mouseY;

        // Uebernahmeflaeche
        if (inBox(x, y, APPLY_X, APPLY_Y, APPLY_W, APPLY_H)) {
            playClick();
            applyBounds();
            return true;
        }

        // Kennlinie: LINEAR, QUAD, LOG - in dieser Reihenfolge untereinander.
        for (int k = 0; k < RodFunction.values().length; k++) {
            if (inBox(x, y, FUNC_X, FUNC_Y + k * FUNC_DY, FUNC_W, FUNC_H)) {
                playClick();
                SetReactorControlC2SPacket.sendFunction(
                        menu.getBlockEntity().getBlockPos(), RodFunction.values()[k]);
                return true;
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    /** Original: klemmt Stellung auf 0..100 und Waerme auf 0..1000, letztere mal 50. */
    private void applyBounds() {
        double[] vals = new double[4];

        for (int k = 0; k < 4; k++) {
            int clamp = k < 2 ? 100 : 1000;
            int mod = k < 2 ? 1 : HEAT_SCALE;

            int parsed;
            try {
                parsed = Math.max(0, Math.min(clamp, Integer.parseInt(fields[k].getValue())));
            } catch (NumberFormatException e) {
                parsed = 0;
            }

            fields[k].setValue(String.valueOf(parsed));
            vals[k] = (double) parsed * mod;
        }

        SetReactorControlC2SPacket.sendBounds(menu.getBlockEntity().getBlockPos(),
                vals[1], vals[0], vals[3], vals[2]);
    }

    private boolean inBox(int mouseX, int mouseY, int x, int y, int w, int h) {
        return mouseX >= this.leftPos + x && mouseX < this.leftPos + x + w
                && mouseY >= this.topPos + y && mouseY < this.topPos + y + h;
    }

    private void playClick() {
        if (this.minecraft != null) {
            this.minecraft.getSoundManager().play(
                    SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
        }
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        com.mojang.blaze3d.systems.RenderSystem.setShader(GameRenderer::getPositionTexShader);
        com.mojang.blaze3d.systems.RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        guiGraphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        String name = this.title.getString();
        guiGraphics.drawString(this.font, name, this.imageWidth / 2 - this.font.width(name) / 2, 6, 0x404040, false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, 8, this.imageHeight - 96 + 2, 0x404040, false);

        // Original: drei Zahlenanzeigen - Stabstellung, Fluss, Temperatur.
        drawNumber(guiGraphics, 6, 20, menu.getRodPercent());
        drawNumber(guiGraphics, 66, 20, menu.getFlux());
        drawNumber(guiGraphics, 126, 20, menu.getTemperature());

        // Die aktive Kennlinie wird links markiert.
        String marker = ">";
        guiGraphics.drawString(this.font, marker,
                FUNC_X - 4, FUNC_Y + menu.getFunction().ordinal() * FUNC_DY + 1, 0x08FF00, false);
    }

    private void drawNumber(GuiGraphics guiGraphics, int x, int y, int value) {
        guiGraphics.drawString(this.font, String.valueOf(value), x, y, 0x08FF00, false);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        com.hbm_m.client.GuiCompat.renderBackground(this, guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // Damit die Eingabefelder nicht bei jedem "e" das Menue schliessen.
        for (EditBox field : fields) {
            if (field != null && field.isFocused()) {
                return field.keyPressed(keyCode, scanCode, modifiers) || super.keyPressed(keyCode, scanCode, modifiers);
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}
