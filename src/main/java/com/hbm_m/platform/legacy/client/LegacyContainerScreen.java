package com.hbm_m.platform.legacy.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;

/**
 * [26.x] Basis für Container-GUIs im 1.21.1-Stil. Stonecutter ersetzt
 * {@code extends AbstractContainerScreen<} durch {@code extends LegacyContainerScreen<} (Regel
 * {@code mc26_container_screen}).
 *
 * <p>Abbildung:
 * <ul>
 *   <li>{@code render/renderBg/renderLabels/renderTooltip} &lt;- {@code extractRenderState/extractBackground/
 *       extractLabels/extractTooltip}</li>
 *   <li>{@code mouseClicked(x, y, button)}, {@code keyPressed(key, scan, mods)}, {@code charTyped(char, mods)} ... &lt;-
 *       die Event-Varianten von 26.x</li>
 *   <li>{@code imageWidth/imageHeight} sind in 26.x final; die Felder hier verdecken sie, damit
 *       {@code this.imageWidth = 200} in Konstruktoren weiter geht. {@link #init()} rechnet
 *       {@code leftPos/topPos} damit neu.</li>
 * </ul>
 * {@code renderBackground(...)} ist ein No-op, weil 26.x den Hintergrund selbst zeichnet.
 */
@SuppressWarnings("unused")
public abstract class LegacyContainerScreen<T extends AbstractContainerMenu> extends net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<T> {

    protected int imageWidth = 176;
    protected int imageHeight = 166;

    private int lastMouseModifiers;
    private boolean lastDoubleClick;

    protected LegacyContainerScreen(T menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Override
    protected void init() {
        super.init();
        this.leftPos = (this.width - this.imageWidth) / 2;
        this.topPos = (this.height - this.imageHeight) / 2;
    }

    public int getXSize() {
        return imageWidth;
    }

    public int getYSize() {
        return imageHeight;
    }

    // ---------------------------------------------------------------- Rendering

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        render(g, mouseX, mouseY, partialTick);
    }

    public void render(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(g, mouseX, mouseY, partialTick);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(g, mouseX, mouseY, partialTick);
        renderBg(g, partialTick, mouseX, mouseY);
    }

    protected void renderBg(GuiGraphicsExtractor g, float partialTick, int mouseX, int mouseY) {
    }

    public void renderBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        renderLabels(g, mouseX, mouseY);
    }

    protected void renderLabels(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        super.extractLabels(g, mouseX, mouseY);
    }

    @Override
    protected void extractTooltip(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        renderTooltip(g, mouseX, mouseY);
    }

    protected void renderTooltip(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        super.extractTooltip(g, mouseX, mouseY);
    }

    // ---------------------------------------------------------------- Eingabe

    private MouseButtonEvent mouse(double x, double y, int button) {
        return new MouseButtonEvent(x, y, new MouseButtonInfo(button, lastMouseModifiers));
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        lastMouseModifiers = event.modifiers();
        lastDoubleClick = doubleClick;
        return mouseClicked(event.x(), event.y(), event.button());
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return super.mouseClicked(mouse(mouseX, mouseY, button), lastDoubleClick);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        lastMouseModifiers = event.modifiers();
        return mouseReleased(event.x(), event.y(), event.button());
    }

    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        return super.mouseReleased(mouse(mouseX, mouseY, button));
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        lastMouseModifiers = event.modifiers();
        return mouseDragged(event.x(), event.y(), event.button(), dragX, dragY);
    }

    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        return super.mouseDragged(mouse(mouseX, mouseY, button), dragX, dragY);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        return keyPressed(event.key(), event.keycode(), event.modifiers());
    }

    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        return super.keyPressed(new KeyEvent(keyCode, scanCode, modifiers));
    }

    @Override
    public boolean keyReleased(KeyEvent event) {
        return keyReleased(event.key(), event.keycode(), event.modifiers());
    }

    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        return super.keyReleased(new KeyEvent(keyCode, scanCode, modifiers));
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        return charTyped((char) event.codepoint(), 0);
    }

    public boolean charTyped(char codePoint, int modifiers) {
        return super.charTyped(new CharacterEvent(codePoint));
    }
}
