package com.hbm_m.client.render;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL43;
import org.lwjgl.opengl.GLDebugMessageCallback;
import org.lwjgl.system.MemoryUtil;

import com.hbm_m.main.MainRegistry;

/**
 * ВРЕМЕННАЯ диагностика «чёрного экрана»: колбэк KHR_debug без миксинов.
 *
 * <p>Включается один раз на первом RenderLevelStageEvent (render thread,
 * контекст гарантированно жив). Флаг GL_DEBUG_OUTPUT работает и без
 * GLFW_OPENGL_DEBUG_CONTEXT на десктопном GL — хинт при создании контекста
 * лишь добавляет дополнительные сообщения валидации.</p>
 *
 * <p>Регистрирует сообщения драйвера severity HIGH и MEDIUM (невалидные
 * бинды, ошибки компиляции/линковки шейдеров, чтение незавершённых буферов,
 * фидбек-лупы и т.п.) в главный лог. Дедупликация: HIGH — всегда, MEDIUM —
 * один раз на id, чтобы перформанс-ворнинги драйвера не завалили лог.</p>
 *
 * <p>Отключение: {@code -Dhbm.glDebug=0} или переменная окружения
 * {@code HBM_GL_DEBUG=0}.</p>
 */
public final class GlDebugProbe {

    private static boolean attempted;
    private static GLDebugMessageCallback callback;
    private static final java.util.Set<Integer> seenIds = java.util.concurrent.ConcurrentHashMap.newKeySet();

    private GlDebugProbe() {}

    public static void enableOnce() {
        if (attempted) {
            return;
        }
        attempted = true;
        if ("0".equals(System.getProperty("hbm.glDebug")) || "0".equals(System.getenv("HBM_GL_DEBUG"))) {
            MainRegistry.LOGGER.info("HBM GlDebugProbe disabled by flag");
            return;
        }
        if (!com.mojang.blaze3d.systems.RenderSystem.isOnRenderThread()) {
            MainRegistry.LOGGER.info("HBM GlDebugProbe skipped: not on render thread");
            return;
        }
        try {
            GL11.glEnable(GL43.GL_DEBUG_OUTPUT);
            GL11.glEnable(GL43.GL_DEBUG_OUTPUT_SYNCHRONOUS);
            callback = new GLDebugMessageCallback() {
                @Override
                public void invoke(int source, int type, int id, int severity, int length,
                                   long message, long userParam) {
                    boolean high = severity == GL43.GL_DEBUG_SEVERITY_HIGH;
                    if (!high && severity != GL43.GL_DEBUG_SEVERITY_MEDIUM) {
                        return;
                    }
                    String msg = MemoryUtil.memUTF8(message, length);
                    if (!high && !seenIds.add(id)) {
                        return; // MEDIUM — не более одного раза на id
                    }
                    MainRegistry.LOGGER.error(String.format(
                            "HBM GLDebug[sev=0x%X type=0x%X id=%d src=0x%X]: %s",
                            severity, type, id, source, msg));
                }
            };
            GL43.glDebugMessageCallback(callback, 0L);
            MainRegistry.LOGGER.info("HBM GlDebugProbe enabled (KHR_debug, synchronous)");
        } catch (Throwable t) {
            MainRegistry.LOGGER.info("HBM GlDebugProbe failed: {}", t.toString());
            callback = null;
        }
    }
}
