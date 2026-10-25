package com.hbm_m.qmaw;

import net.minecraft.client.gui.GuiGraphics;

/** 1:1 {@code com.hbm.qmaw.ManualElement}; {@code render} bekommt zusaetzlich die {@link GuiGraphics}. */
public abstract class ManualElement {

    public abstract int getWidth();
    public abstract int getHeight();
    public abstract void render(GuiGraphics gfx, boolean isMouseOver, int x, int y, int mouseX, int mouseY);
    public abstract void onClick(GuiQMAW gui);
}
