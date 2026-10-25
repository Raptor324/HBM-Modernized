package com.hbm_m.inventory.gui;

import java.util.Arrays;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;

/**
 * 1:1 {@code com.hbm.module.NumberDisplay} (UFFR): Siebensegmentanzeige fuer GUIs aus einfarbigen Rechtecken.
 * Koordinaten relativ zur GUI-Ecke; gezeichnet wird mit {@link #drawNumber(GuiGraphics, int, int, Number)}.
 */
public class NumberDisplay {

    private final int displayX;
    private final int displayY;
    private int color;
    private byte padding = 3;
    private boolean blink = false;
    private float maxNum;
    private float minNum;
    private boolean customBounds = false;
    private boolean isFloat = false;
    private byte floatPad = 1;
    private boolean pads = false;
    private byte digitLength = 3;
    private Number numIn = 0;
    private char[] toDisp = {'0', '0', '0'};
    private short dispOffset = 0;
    private int verticalLength = 5;
    private int horizontalLength = 4;
    private int thickness = 1;

    private GuiGraphics g;
    private int guiLeft;
    private int guiTop;

    public NumberDisplay(int x, int y, int color) {
        this(x, y);
        setColor(color);
    }

    public NumberDisplay(int x, int y) {
        displayX = x;
        displayY = y;
        setColor(0xFFFF55);
    }

    public void setColor(int color) {
        this.color = color;
    }

    public void drawNumber(GuiGraphics g, int guiLeft, int guiTop, Number num) {
        setNumber(num);
        drawNumber(g, guiLeft, guiTop);
    }

    public void drawNumber(GuiGraphics g, int guiLeft, int guiTop) {
        if (isFloat) formatForFloat();
        drawNumber(g, guiLeft, guiTop, toDisp);
    }

    public void drawNumber(GuiGraphics g, int guiLeft, int guiTop, char[] num) {
        this.g = g;
        this.guiLeft = guiLeft;
        this.guiTop = guiTop;

        if (blink && !(System.currentTimeMillis() % 1000 < 500)) return;
        short gap = (short) (digitLength - num.length);
        for (int i = 0; i < num.length; i++) {
            if (num[i] == '.') gap--;
            dispOffset = (short) ((padding + horizontalLength + 2 * thickness) * (i + gap));
            drawChar(num[i]);
        }
        if (pads) padOut(gap);
    }

    private void padOut(short gap) {
        if (gap == 0) return;
        for (int i = 0; i < gap; i++) {
            dispOffset = (short) ((padding + horizontalLength + 2 * thickness) * i);
            drawChar('0');
        }
    }

    private void drawChar(char num) {
        switch (num) {
            case '1' -> { drawVertical(1, 0); drawVertical(1, 1); }
            case '2' -> { drawHorizontal(0); drawVertical(1, 0); drawHorizontal(1); drawVertical(0, 1); drawHorizontal(2); }
            case '3' -> { drawHorizontal(0); drawHorizontal(1); drawHorizontal(2); drawVertical(1, 0); drawVertical(1, 1); }
            case '4' -> { drawVertical(0, 0); drawVertical(1, 0); drawVertical(1, 1); drawHorizontal(1); }
            case '5' -> { drawHorizontal(0); drawHorizontal(1); drawHorizontal(2); drawVertical(0, 0); drawVertical(1, 1); }
            case '6' -> { drawHorizontal(0); drawHorizontal(1); drawHorizontal(2); drawVertical(0, 0); drawVertical(0, 1); drawVertical(1, 1); }
            case '7' -> { drawHorizontal(0); drawVertical(1, 0); drawVertical(1, 1); }
            case '8' -> { drawHorizontal(0); drawHorizontal(1); drawHorizontal(2); drawVertical(0, 0); drawVertical(1, 0); drawVertical(0, 1); drawVertical(1, 1); }
            case '9' -> { drawHorizontal(0); drawHorizontal(1); drawHorizontal(2); drawVertical(0, 0); drawVertical(1, 0); drawVertical(1, 1); }
            case '0' -> { drawHorizontal(0); drawHorizontal(2); drawVertical(0, 0); drawVertical(0, 1); drawVertical(1, 0); drawVertical(1, 1); }
            case '-' -> drawHorizontal(1);
            case '.' -> drawPeriod();
            default -> { drawHorizontal(0); drawHorizontal(1); drawHorizontal(2); drawVertical(0, 0); drawVertical(0, 1); }
        }
    }

    private void drawHorizontal(int pos) {
        byte offset = (byte) (pos * (verticalLength + thickness));
        renderSegment(guiLeft + displayX + dispOffset + thickness, guiTop + displayY + offset, horizontalLength, thickness);
    }

    /** Original rechnet die Y-Lage des Punkts mit {@code getGuiLeft()} - so uebernommen. */
    private void drawPeriod() {
        renderSegment(guiLeft + displayX + dispOffset + padding - (int) Math.ceil(padding / 2) + (horizontalLength + thickness),
                guiLeft + displayY + 2 * (verticalLength + thickness), thickness, thickness);
    }

    private void drawVertical(int posX, int posY) {
        byte offsetX = (byte) (posX * (horizontalLength + thickness));
        byte offsetY = (byte) (posY * (verticalLength + thickness));
        renderSegment(guiLeft + displayX + offsetX + dispOffset, guiTop + displayY + offsetY + thickness, thickness, verticalLength);
    }

    private void renderSegment(int renX, int renY, int width, int height) {
        g.fill(renX, renY, renX + width, renY + height, 0xFF000000 | color);
    }

    public void setNumber(Number num) {
        numIn = num;
        if (customBounds) numIn = Mth.clamp(num.doubleValue(), minNum, maxNum);
        if (isFloat) {
            formatForFloat();
        } else {
            toDisp = Long.toString(Math.round(numIn.doubleValue())).toCharArray();
            toDisp = truncOrExpand();
        }
    }

    public Number getNumber() { return numIn; }

    public char[] getDispNumber() { return toDisp.clone(); }

    public NumberDisplay setBlinks(boolean doesBlink) { blink = doesBlink; return this; }

    public NumberDisplay setPadding(int p) { padding = (byte) p; return this; }

    public NumberDisplay setDigitLength(int l) {
        digitLength = (byte) l;
        toDisp = truncOrExpand();
        return this;
    }

    public NumberDisplay setSegmentSize(int vertical, int horizontal, int thickness) {
        this.verticalLength = vertical;
        this.horizontalLength = horizontal;
        this.thickness = thickness;
        return this;
    }

    public NumberDisplay setMaxMin(float max, float min) {
        if (min > max) throw new IllegalArgumentException("Minimum value is larger than maximum value!");
        maxNum = max;
        minNum = min;
        customBounds = true;
        return this;
    }

    public NumberDisplay setPadNumber() { pads = true; return this; }

    public NumberDisplay setFloat() { return setFloat(1); }

    public NumberDisplay setFloat(int pad) {
        floatPad = (byte) pad;
        isFloat = true;
        formatForFloat();
        return this;
    }

    private void formatForFloat() {
        double scale = Math.pow(10, floatPad);
        char[] proc = Double.toString(Math.round(numIn.doubleValue() * scale) / scale).toCharArray();
        if (proc.length == digitLength) toDisp = proc;
        else toDisp = truncOrExpand();
    }

    private char[] truncOrExpand() {
        if (isFloat) {
            char[] out = Arrays.copyOf(toDisp, digitLength);
            for (int i = 0; i < digitLength; i++)
                if (out[i] == '\u0000') out[i] = '0';
            return out.clone();
        }
        return toDisp;
    }
}
