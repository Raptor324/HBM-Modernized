package com.hbm_m.inventory.gui;

import com.hbm_m.item.special.ItemHolotapeImage.EnumHoloImage;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

/**
 * 1:1 {@code com.hbm.inventory.gui.GUIScreenHolotape}: dunkelgruenes, halbtransparentes Feld 300x150 mit dem
 * zentrierten, umbrochenen Bandtext in 0x009900; Oeffnen spielt {@code hbm:block.bobble}, das Spiel laeuft weiter.
 */
public class GUIScreenHolotape extends Screen {

    private final EnumHoloImage holo;

    public static void open(EnumHoloImage holo) {
        Minecraft.getInstance().setScreen(new GUIScreenHolotape(holo));
    }

    public GUIScreenHolotape(EnumHoloImage holo) {
        super(Component.empty());
        this.holo = holo;
    }

    @Override
    protected void init() {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(com.hbm_m.sound.HbmSoundsNT.get("hbm:block.bobble"), 1.0F));
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float f) {
        if (this.holo == null)
            return;

        this.renderBackground(g);

        double sizeX = 300;
        double sizeY = 150;
        double left = (this.width - sizeX) / 2;
        double top = (this.height - sizeY) / 2;

        // setColorRGBA_F(0, 0.2, 0, 0.8)
        g.fill((int) left, (int) top, (int) (left + sizeX), (int) (top + sizeY), (204 << 24) | (51 << 8));

        int nextLevel = (int) top + 30;

        if (this.holo.getText() != null) {
            for (FormattedCharSequence text : this.font.split(Component.literal(this.holo.getText()), 275)) {
                g.drawString(this.font, text, (int) (left + sizeX / 2 - this.font.width(text) / 2), nextLevel, 0x009900, true);
                nextLevel += 10;
            }
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.minecraft != null && this.minecraft.options.keyInventory.matches(keyCode, scanCode)) {
            this.onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
