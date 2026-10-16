package com.hbm_m.client.render;

import org.lwjgl.opengl.GL11;
import org.lwjgl.system.MemoryStack;

import com.hbm_m.main.MainRegistry;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.Minecraft;

/**
 * ВРЕМЕННАЯ диагностика «чёрного экрана в GUI-фазе»: бисекция по оверлеям.
 *
 * <p>Прогон 18:58 27.08 (px-зонды) показал: на {@code px.gui.pre} кадр живой,
 * на {@code px.gui.post} — центр = белый крестхейр (255,255,255) на чистом
 * (0,0,0). Значит, полноэкранный чёрный квад рисуется МЕЖДУ gui.pre и gui.post
 * каким-то GUI-оверлеем. Пользователь дополнительно подтвердил: при тряске GUI
 * (hard landing силовой брони) чёрный прямоугольник едет ВМЕСТЕ с GUI — т.е.
 * он рисуется именно в GUI-стопке, поверх мира, до хотбара/крестхейра.</p>
 *
 * <p>Пробник на Pre каждого оверлея читает центральный пиксель в буфер цепочки;
 * в конце кадра (gui.post) если первый замер живой, а последний чёрный —
 * печатает ВСЮ цепочку «оверлей=значение», по которой видно, на входе в какой
 * оверлей кадр уже был чёрным (значит, чёрным его закрасил ПРЕДЫДУЩИЙ).</p>
 *
 * <p>Чтение пикселя — синхронный glReadPixels, поэтому троттлится до ~150 мс.
 * Если за кадр сделано меньше двух замеров, вердикт не выносится.</p>
 */
public final class GuiOverlayBisectProbe {

    private static final int READ_INTERVAL_MS = 150;
    private static final int LOG_INTERVAL_MS = 1000;
    private static final int MAX_SAMPLES = 64;

    private static long lastReadMs;
    private static long lastLogMs;
    private static final String[] sampleIds = new String[MAX_SAMPLES];
    private static final int[] sampleValues = new int[MAX_SAMPLES];
    private static int sampleCount;

    private GuiOverlayBisectProbe() {}

    /** Сброс на границе кадра — вызывать из RenderGuiEvent.Post. */
    public static void resetFrame() {
        if (sampleCount >= 2) {
            evaluate();
        }
        sampleCount = 0;
    }

    /** Вызывать из RenderGuiOverlayEvent.Pre — id оверлея, который СЕЙЧАС начнёт рисоваться. */
    public static void onOverlayPre(Object overlayId) {
        if (sampleCount >= MAX_SAMPLES || !RenderSystem.isOnRenderThread()) {
            return;
        }
        try {
            long now = System.currentTimeMillis();
            if (now - lastReadMs < READ_INTERVAL_MS) {
                return;
            }
            lastReadMs = now;
            sampleIds[sampleCount] = String.valueOf(overlayId);
            sampleValues[sampleCount] = readCenterRed();
            sampleCount++;
        } catch (Throwable ignored) {
            sampleCount = 0;
        }
    }

    private static void evaluate() {
        int first = sampleValues[0];
        int last = sampleValues[sampleCount - 1];
        if (first >= 10 && last <= 2 && first - last >= 8) {
            long now = System.currentTimeMillis();
            if (now - lastLogMs >= LOG_INTERVAL_MS) {
                lastLogMs = now;
                StringBuilder sb = new StringBuilder("HBM gui-bisect: кадр зачернился в GUI-фазе, цепочка: ");
                for (int i = 0; i < sampleCount; i++) {
                    if (i > 0) sb.append(" -> ");
                    sb.append(sampleIds[i]).append('=').append(sampleValues[i]);
                }
                sb.append("  (первый чёрный замер = чёрное рисует ПРЕДЫДУЩИЙ оверлей или ванильный код между ними)");
                MainRegistry.LOGGER.error(sb.toString());
            }
        }
    }

    private static int readCenterRed() {
        var win = Minecraft.getInstance().getWindow();
        try (MemoryStack st = MemoryStack.stackPush()) {
            var buf = st.malloc(3);
            GL11.glReadPixels(win.getWidth() / 2, win.getHeight() / 2, 1, 1,
                    GL11.GL_RGB, GL11.GL_UNSIGNED_BYTE, buf);
            GL11.glGetError(); // глотаем возможный флаг, чтобы не портить чужие проверки
            return buf.get(0) & 0xFF;
        }
    }
}
