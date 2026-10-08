package com.hbm_m.qmaw.components;

import com.hbm_m.qmaw.GuiQMAW;
import com.hbm_m.qmaw.ManualElement;
import com.hbm_m.qmaw.QMAWLoader;
import com.hbm_m.qmaw.QuickManualAndWiki;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;

/** 1:1 {@code com.hbm.qmaw.components.QComponentLink}: Verweis auf einen anderen Handbucheintrag. */
public class QComponentLink extends ManualElement {

    protected String link;
    protected ItemStack icon;
    protected String text;
    protected Font font;
    protected int color = 0x0094FF;
    protected int hoverColor = 0xFFD800;

    public QComponentLink(String link, String text) {
        this.text = text;
        this.link = link;

        QuickManualAndWiki qmaw = QMAWLoader.qmaw.get(link);
        if (qmaw == null) {
            this.color = this.hoverColor = 0xFF7F7F;
        } else {
            this.icon = qmaw.icon;
        }

        this.font = Minecraft.getInstance().font;
    }

    public QComponentLink setColor(int color, int hoverColor) {
        this.color = color;
        this.hoverColor = hoverColor;
        return this;
    }

    @Override
    public int getWidth() {
        return font.width(text) + (icon != null ? 18 : 0);
    }

    @Override
    public int getHeight() {
        return Math.max(font.lineHeight, icon != null ? 16 : 0);
    }

    @Override
    public void render(GuiGraphics gfx, boolean isMouseOver, int x, int y, int mouseX, int mouseY) {

        if (this.icon != null) {
            gfx.renderItem(this.icon, x, y - 1);
            gfx.renderItemDecorations(this.font, this.icon, x, y - 1);

            x += 18;
            y += (16 - font.lineHeight) / 2;
        }

        gfx.drawString(font, text, x, y, isMouseOver ? hoverColor : color, false);
    }

    @Override public void onClick(GuiQMAW gui) {
        QuickManualAndWiki qmaw = QMAWLoader.qmaw.get(link);
        if (qmaw != null) {
            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
            GuiQMAW screen = new GuiQMAW(qmaw);
            screen.back.addAll(gui.back);
            screen.back.add(gui.qmawID);
            Minecraft.getInstance().setScreen(screen);
        }
    }
}
