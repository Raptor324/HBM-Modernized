package com.hbm_m.client.overlay;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.hbm_m.config.ModClothConfig;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
//? if forge {
import net.minecraftforge.client.gui.overlay.IGuiOverlay;
//?}
@OnlyIn(Dist.CLIENT)
public class OverlayInfoToast {

    private static final List<Entry> ENTRIES = new ArrayList<>();

    // Разные ID -> обновление строки вместо спама.
    public static final int ID_DASH    = 2001;
    public static final int ID_VATS    = 2002;
    public static final int ID_THERMAL = 2003;
    public static final int ID_FLUID_IDENTIFIER_SWAP = 7;
    /** Режимы кирки / топора / лопаты (аналог ID_TOOLABILITY в 1.7.10). */
    public static final int ID_TOOL_MODE = 11;
    /** Дальномер / лазерный детонатор (аналог ID_DETONATOR = 8 в 1.7.10). */
    public static final int ID_DETONATOR = 8;
    /** Маска без фильтра (аналог ID_FILTER в 1.7.10). */
    public static final int ID_FILTER = 1;

    // Стиль оригинала: один общий фон 0.25/0.5.
    private static final int BG_COLOR = 0x7F3F3F3F;

    // Оригинал рисовал строки шагом 10px.
    private static final int LINE_STEP = 10;

    public static class Entry {
        public Component text;
        public int ticksLeft;
        public int durationTicks;
        public int rgb;
        public int id;
        public long startMs = System.currentTimeMillis();

        public Entry(Component text, int ticks, int id, int rgb) {
            this.text = text;
            this.ticksLeft = ticks;
            this.durationTicks = Math.max(1, ticks);
            this.id = id;
            this.rgb = rgb & 0xFFFFFF;
        }
    }

    /** Аналог старого displayTooltip(msg, time, id): id обновляет существующую запись. */
    public static void show(Component text, int ticks, int id, int rgb) {
        if (text == null) return;

        // id < 0 = всегда новая запись (если вдруг понадобится).
        if (id > 0) { // Original: id 0 = immer neuer Eintrag (push ohne ID)
            for (Entry e : ENTRIES) {
                if (e.id == id) {
                    e.text = text;
                    e.ticksLeft = ticks;
                    e.startMs = System.currentTimeMillis();
                    e.durationTicks = Math.max(1, ticks);
                    e.rgb = rgb & 0xFFFFFF;
                    return;
                }
            }
        }

        ENTRIES.add(new Entry(text, ticks, id, rgb));
        // Original: Sortierung nach Dauer aufsteigend (InfoEntry.compareTo), stabil.
        ENTRIES.sort(java.util.Comparator.comparingInt(x -> x.durationTicks));
    }

    public static void dismiss(int id) {
        ENTRIES.removeIf(e -> e.id == id);
    }

    public static void show(Component text, int ticks, int id) {
        show(text, ticks, id, 0xFFFFFF);
    }

    public static int rgbFromFormatting(ChatFormatting formatting) {
        return com.hbm_m.network.InfoToastPacket.rgbFromFormatting(formatting);
    }

    /** Сообщение о переключении режима инструмента (60 тиков, один слот по id). */
    public static void showToolMode(Component text, ChatFormatting formatting) {
        show(text, 60, ID_TOOL_MODE, rgbFromFormatting(formatting));
    }

    /** Кросс-лоадерное затухание записей; вызывается из ClientModEvents (Architectury ClientTickEvent.CLIENT_POST). */
    public static void tick() {
        // Original RenderInfoSystem: Ablauf per Wanduhr (millis), auch waehrend Pause.
        long now = System.currentTimeMillis();
        Iterator<Entry> it = ENTRIES.iterator();
        while (it.hasNext()) {
            Entry e = it.next();
            e.ticksLeft--;
            if (now - e.startMs > e.durationTicks * 50L) it.remove();
        }
    }

    public static void render(GuiGraphics gfx, float partialTick, int screenWidth, int screenHeight) {
        if (ENTRIES.isEmpty()) return;

        Minecraft mc = Minecraft.getInstance();
        Font font = mc.font;

        ModClothConfig cfg = ModClothConfig.get();

        int longest = 0;
        for (Entry e : ENTRIES) {
            int w = font.width(e.text);
            if (w > longest) longest = w;
        }

        // Original RenderInfoSystem: Position nach INFO_POSITION, dann INFO_OFFSET_*. Die Port-Werte
        // infoToastOffsetX/Y (Vorgabe 15) ersetzen dabei die feste 15 der oberen Ecken.
        int mode = com.hbm_m.config.ClientConfig.infoPosition;
        int pX = mode == 0 ? cfg.infoToastOffsetX : mode == 1 ? (screenWidth - longest - cfg.infoToastOffsetX) : mode == 2 ? (screenWidth / 2 + 7) : (screenWidth / 2 - longest - 6);
        int pZ = mode == 0 ? cfg.infoToastOffsetY : mode == 1 ? cfg.infoToastOffsetY : screenHeight / 2 + 7;

        pX += com.hbm_m.config.ClientConfig.infoOffsetHorizontal;
        pZ += com.hbm_m.config.ClientConfig.infoOffsetVertical;

        int padY = 5;

        int left = pX - 5;
        int top = pZ - padY;
        int right = pX + 5 + longest;

        int bottom = ENTRIES.size() * LINE_STEP + pZ + 2; // Original: messages.size() * 10 + pZ + 2

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        gfx.fill(left, top, right, bottom, BG_COLOR);

        int off = 0;
        for (Entry e : ENTRIES) {
            float remaining = e.durationTicks - (System.currentTimeMillis() - e.startMs) / 50.0f;
            if (remaining < 0) remaining = 0;

            // Оригинальная формула: clamp(510 * remaining / duration, 5..255).
            int alpha = (int) (510.0f * (remaining / (float) e.durationTicks));
            if (alpha > 255) alpha = 255;
            if (alpha < 5) alpha = 5;

            int argb = (alpha << 24) | (e.rgb & 0xFFFFFF);

            // Оригинал: без тени.
            gfx.drawString(font, e.text, pX, pZ + off, argb, false);
            off += LINE_STEP;
        }

        RenderSystem.disableBlend();
    }

    // neo-pendant: ClientSetup.onRegisterGuiOverlays (neoforge-Zweig)
    //? if forge {
    public static final IGuiOverlay OVERLAY = (gui, gfx, partialTick, screenWidth, screenHeight) ->
            render(gfx, partialTick, screenWidth, screenHeight);
    //?}
}
