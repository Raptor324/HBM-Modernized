package com.hbm_m.inventory.gui;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;

import org.lwjgl.glfw.GLFW;

import com.hbm_m.util.Calculator;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * 1:1-Port von {@code GUICalculator} (Taste N): Eingabezeile, Live-Ergebnis, sechs Zeilen Verlauf
 * (Pfeil hoch/runter waehlen, Enter uebernimmt), Enter rechnet und legt das Ergebnis in die
 * Zwischenablage.
 */
public class GUICalculator extends Screen {

    private final int xSize = 220;
    private final int ySize = 50;
    private final int borderWidth = 2;
    private EditBox inputField;
    private int selectedHist = -1;
    private static final int maxHistory = 6;
    private static final Deque<String[]> history = new ArrayDeque<>();
    private String latestResult = "?";

    public GUICalculator() {
        super(Component.empty());
    }

    public static void open() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) mc.player.closeContainer();
        mc.setScreen(new GUICalculator());
    }

    @Override
    protected void init() {
        int x = (width - xSize) / 2;
        int y = (height - ySize) / 2;
        inputField = new EditBox(font, x + 5, y + 8, 210, 13, Component.empty());
        inputField.setTextColor(-1);
        inputField.setCanLoseFocus(false);
        inputField.setFocused(true);
        inputField.setMaxLength(1000);
        inputField.setResponder(s -> recalc());
        addRenderableWidget(inputField);
        setInitialFocus(inputField);
    }

    private String sanitized() {
        return inputField.getValue().replaceAll("[^\\d+\\-*/%^!.()\\sA-Za-z]+", "");
    }

    private void recalc() {
        String input = sanitized();
        if (input.isEmpty()) {
            latestResult = "?";
            return;
        }
        try {
            latestResult = Double.toString(Calculator.evaluateExpression(input));
        } catch (Exception e) {
            latestResult = e.toString();
        }
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) {
            String input = sanitized();
            if (selectedHist != -1) {
                input = new ArrayList<>(history).get(selectedHist)[0];
                inputField.setValue(input);
                selectedHist = -1;
            } else {
                try {
                    double result = Calculator.evaluateExpression(input);
                    history.addFirst(new String[] { input, Double.toString(result) });
                    if (history.size() > maxHistory) history.removeLast();
                    String plain = new BigDecimal(result, MathContext.DECIMAL64).toPlainString();
                    Minecraft.getInstance().keyboardHandler.setClipboard(plain);
                    inputField.setValue(plain);
                    //? if < 1.21.1 {
                    inputField.moveCursorToEnd();
                    //?} else {
                    /*inputField.moveCursorToEnd(false);
                    *///?}
                    inputField.setHighlightPos(0);
                } catch (Exception ignored) { }
            }
            return true;
        }
        if (key == GLFW.GLFW_KEY_UP) {
            selectedHist = Math.max(selectedHist - 1, -1);
            return true;
        } else if (key == GLFW.GLFW_KEY_DOWN) {
            selectedHist = Math.min(selectedHist + 1, history.size() - 1);
            return true;
        } else if (key != GLFW.GLFW_KEY_ESCAPE) {
            selectedHist = -1;
        }
        return super.keyPressed(key, scan, mods);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTicks) {
        int x = (width - xSize) / 2;
        int y = (height - ySize) / 2;
        int histHeight = (font.lineHeight + 2) * maxHistory;
        int histStart = y + 30 + font.lineHeight + 8;
        g.fill(x, y, x + xSize, y + ySize + histHeight, 0xFF2d2d2d);
        g.fill(x + borderWidth, y + borderWidth, x + xSize - borderWidth, y + ySize - borderWidth + histHeight, 0xFF3d3d3d);
        g.fill(x, histStart - 5, x + xSize, histStart - 3, 0xFF2d2d2d);
        super.render(g, mouseX, mouseY, partialTicks);
        g.drawString(font, "=" + latestResult, x + 5, y + 30, -1, false);
        int i = 0;
        for (String[] prev : history) {
            int hy = y + 50 + (font.lineHeight + 1) * i;
            if (i == selectedHist) g.fill(x + 4, hy - 1, x + 4 + xSize - 9, hy + font.lineHeight, 0xFF111111);
            g.drawString(font, prev[0] + " = " + prev[1], x + 5, hy, -1, false);
            i++;
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
