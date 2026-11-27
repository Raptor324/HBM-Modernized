package com.hbm_m.platform.legacy.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.network.chat.Component;

/**
 * [26.x] Basis für einfache GUIs (ohne Container) im 1.21.1-Stil. Stonecutter ersetzt {@code extends Screen}
 * durch {@code extends LegacyScreen} (Regel {@code mc26_screen}). Abbildung wie {@link LegacyContainerScreen}.
 */
@SuppressWarnings("unused")
public abstract class LegacyScreen extends net.minecraft.client.gui.screens.Screen {

    private int lastMouseModifiers;
    private boolean lastDoubleClick;

    protected LegacyScreen(Component title) {
        super(title);
    }

    // ---------------------------------------------------------------- Rendering

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        render(g, mouseX, mouseY, partialTick);
    }

    public void render(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(g, mouseX, mouseY, partialTick);
    }

    public void renderBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
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
