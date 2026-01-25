package com.hbm_m.client.overlay;

import com.hbm_m.config.ModClothConfig;
import com.hbm_m.lib.RefStrings;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(modid = RefStrings.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class OverlayInfoToast {

    private static final List<Entry> ENTRIES = new ArrayList<>();

    // Разные ID -> обновление строки вместо спама.
    public static final int ID_DASH    = 2001;
    public static final int ID_VATS    = 2002;
    public static final int ID_THERMAL = 2003;

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
        if (id >= 0) {
            for (Entry e : ENTRIES) {
                if (e.id == id) {
                    e.text = text;
                    e.ticksLeft = ticks;
                    e.durationTicks = Math.max(1, ticks);
                    e.rgb = rgb & 0xFFFFFF;
                    return;
                }
            }
        }

        ENTRIES.add(new Entry(text, ticks, id, rgb));
    }

    public static void show(Component text, int ticks, int id) {
        show(text, ticks, id, 0xFFFFFF);
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (Minecraft.getInstance().isPaused()) return;

        Iterator<Entry> it = ENTRIES.iterator();
        while (it.hasNext()) {
            Entry e = it.next();
            e.ticksLeft--;
            if (e.ticksLeft <= 0) it.remove();
        }
    }

    public static final IGuiOverlay OVERLAY = (gui, gfx, partialTick, screenWidth, screenHeight) -> {
        if (ENTRIES.isEmpty()) return;

        Minecraft mc = Minecraft.getInstance();
        Font font = mc.font;

        ModClothConfig cfg = ModClothConfig.get();
        int pX = cfg.infoToastOffsetX;
        int pZ = cfg.infoToastOffsetY; // 15 by default

        int longest = 0;
        for (Entry e : ENTRIES) {
            int w = font.width(e.text);
            if (w > longest) longest = w;
        }

        int padY = 5;

        int left = pX - 5;
        int top = pZ - padY;
        int right = pX + 5 + longest;

        int bottom = pZ + ((ENTRIES.size() - 1) * LINE_STEP) + (font.lineHeight - 1) + padY;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        gfx.fill(left, top, right, bottom, BG_COLOR);

        int off = 0;
        for (Entry e : ENTRIES) {
            float remaining = e.ticksLeft - partialTick;
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
    };
}
