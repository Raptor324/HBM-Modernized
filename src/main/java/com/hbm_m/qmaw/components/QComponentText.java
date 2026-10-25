package com.hbm_m.qmaw.components;

import com.hbm_m.qmaw.GuiQMAW;
import com.hbm_m.qmaw.ManualElement;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

/** 1:1 {@code com.hbm.qmaw.components.QComponentText}. */
public class QComponentText extends ManualElement {

    protected String text;
    protected Font font;
    protected int color = 0xFFFFFF;

    public QComponentText(String text) {
        this(text, Minecraft.getInstance().font);
    }

    public QComponentText(String text, Font font) {
        this.text = text;
        this.font = font;
    }

    public QComponentText setColor(int color) {
        this.color = color;
        return this;
    }

    @Override
    public int getWidth() {
        return font.width(text);
    }

    @Override
    public int getHeight() {
        return font.lineHeight;
    }

    @Override
    public void render(GuiGraphics gfx, boolean isMouseOver, int x, int y, int mouseX, int mouseY) {
        gfx.drawString(font, text, x, y, color, false);
    }

    @Override public void onClick(GuiQMAW gui) { }
}
