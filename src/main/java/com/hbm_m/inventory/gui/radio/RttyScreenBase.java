package com.hbm_m.inventory.gui.radio;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Gemeinsames Geruest der Funk-GUIs des Originals ({@code GuiScreen} mit {@code GuiTextField}s ohne Rahmen, gruene
 * Schrift): Mittelpunkt-Layout, Fokus per Klick, Tastatur an das fokussierte Feld, Inventar-Taste schliesst.
 */
public abstract class RttyScreenBase extends Screen {

    protected int xSize;
    protected int ySize;
    protected int guiLeft;
    protected int guiTop;
    protected final List<EditBox> fields = new ArrayList<>();

    protected RttyScreenBase(Component title, int xSize, int ySize) {
        super(title);
        this.xSize = xSize;
        this.ySize = ySize;
    }

    @Override
    protected void init() {
        this.guiLeft = (this.width - this.xSize) / 2;
        this.guiTop = (this.height - this.ySize) / 2;
        this.fields.clear();
    }

    /** Original {@code GuiTextField} mit gruener Schrift ohne Hintergrund. */
    protected EditBox field(int x, int y, int w, int h, int maxLen, String text) {
        EditBox f = new EditBox(this.font, x, y, w, h, Component.empty());
        f.setTextColor(0x00ff00);
        f.setTextColorUneditable(0x00ff00);
        f.setBordered(false);
        f.setMaxLength(maxLen);
        f.setValue(text == null ? "" : text);
        fields.add(f);
        return f;
    }

    /** Nur sichtbare/aktive Felder bekommen Klicks und Tasten. */
    protected boolean isActive(EditBox f) {
        return true;
    }

    protected void click() {
        if (this.minecraft != null) this.minecraft.getSoundManager().play(
                net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1.0F));
    }

    protected boolean in(double x, double y, int left, int top, int w, int h) {
        return guiLeft + left <= x && guiLeft + left + w > x && guiTop + top < y && guiTop + top + h >= y;
    }

    protected void tip(GuiGraphics g, int x, int y, String... lines) {
        List<Component> l = new ArrayList<>();
        for (String s : lines) l.add(Component.literal(s));
        g.renderComponentTooltip(this.font, l, x, y);
    }

    protected void tip(GuiGraphics g, int x, int y, Component line) {
        g.renderComponentTooltip(this.font, List.of(line), x, y);
    }

    protected void drawFields(GuiGraphics g, int x, int y, float f) {
        for (EditBox e : fields) if (isActive(e)) e.render(g, x, y, f);
    }

    /** Fokus wie {@code GuiTextField.mouseClicked}; true, wenn ein Feld getroffen wurde. */
    protected boolean clickFields(double x, double y, int button) {
        boolean hit = false;
        for (EditBox e : fields) {
            boolean on = isActive(e) && e.mouseClicked(x, y, button);
            e.setFocused(on);
            hit |= on;
        }
        return hit;
    }

    @Override
    public boolean charTyped(char c, int mods) {
        for (EditBox e : fields) if (isActive(e) && e.isFocused() && e.charTyped(c, mods)) return true;
        return super.charTyped(c, mods);
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (key != 256) {
            for (EditBox e : fields) {
                if (isActive(e) && e.isFocused()) {
                    e.keyPressed(key, scan, mods);
                    return true;
                }
            }
        }
        if (this.minecraft != null && this.minecraft.options.keyInventory.matches(key, scan)) {
            this.onClose();
            return true;
        }
        return super.keyPressed(key, scan, mods);
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
