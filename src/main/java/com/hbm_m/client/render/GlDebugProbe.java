package com.hbm_m.client.render;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL43;
import org.lwjgl.opengl.GLDebugMessageCallback;
import org.lwjgl.system.MemoryUtil;

import com.hbm_m.main.MainRegistry;

/**
 * Temporary "black screen" diagnostics: KHR_debug callback without mixins.
 *
 * <p>Enabled once at the first RenderLevelStageEvent (render thread, the context is
 * guaranteed alive). The GL_DEBUG_OUTPUT flag works even without GLFW_OPENGL_DEBUG_CONTEXT
 * on desktop GL - the context-creation hint only adds extra validation messages.</p>
 *
 * <p>Registers driver messages of severity HIGH and MEDIUM (invalid binds, shader
 * compile/link errors, reads of incomplete buffers, feedback loops, etc.) into the main
 * log. Deduplication: HIGH - always, MEDIUM - once per id so driver performance warnings
 * do not flood the log.</p>
 *
 * <p>Disable with {@code -Dhbm.glDebug=0} or the environment variable
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
        // The glDebugOutput config is the primary source (takes effect at the first enableOnce,
        // i.e. after a restart); -Dhbm.glDebug=0 / env HBM_GL_DEBUG=0 is the emergency override.
        if (!com.hbm_m.config.ModClothConfig.get().glDebugOutput
                || "0".equals(System.getProperty("hbm.glDebug"))
                || "0".equals(System.getenv("HBM_GL_DEBUG"))) {
            MainRegistry.LOGGER.info("HBM GlDebugProbe disabled by config/flag");
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
                        return; // MEDIUM - at most once per id
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
