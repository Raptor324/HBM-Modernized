package com.hbm_m.client.render;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
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

    /**
     * Rate limit for HIGH-severity storms. A stuck uniform-push desync can raise
     * the same error ~18k times per second (1.7M lines in one 90 s session,
     * 2026-10-05) without the rendering being wrong; the log volume alone makes
     * the game unplayable. Per id: at most one line per second, suppressed
     * repeats counted, one summary line per 5 s window while suppressed.
     * Index = error id (only a handful of distinct ids exist per session).
     */
    private static final java.util.Map<Integer, long[]> HIGH_RATE = new java.util.concurrent.ConcurrentHashMap<>();
    private static final long MIN_INTERVAL_MS = 1000;
    private static final long SUMMARY_INTERVAL_MS = 5000;

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
                    int verdict = 0;
                    if (high) {
                        verdict = highRateAllow(id);
                        if (verdict == 0) {
                            return; // storm: same id already logged this second, summary later
                        }
                    }
                    String msg = MemoryUtil.memUTF8(message, length);
                    if (!high && !seenIds.add(id)) {
                        return; // MEDIUM - at most once per id
                    }
                    MainRegistry.LOGGER.error(String.format(
                            "HBM GLDebug[sev=0x%X type=0x%X id=%d src=0x%X]: %s | GL state: %s",
                            severity, type, id, source, msg, describeGlState()));
                    // HIGH errors (spam-prone): capture the Java call site. The callback is
                    // SYNCHRONOUS - this stack frame chain ends at the exact GL call that
                    // generated the error. Printed for the first occurrence of an id and
                    // then for the first sampled line of every 5 s rate-limit window: a
                    // storm's call SITE can change over time (per-shader, per-path), and
                    // the first-hit-only stack hid that (2026-10-05 session).
                    if (high && (verdict == 2 || seenIds.add(id))) {
                        MainRegistry.LOGGER.error("HBM GLDebug call site (id=" + id + "):",
                                new RuntimeException("GL error " + id));
                    }
                }
            };
            GL43.glDebugMessageCallback(callback, 0L);
            MainRegistry.LOGGER.info("HBM GlDebugProbe enabled (KHR_debug, synchronous)");
        } catch (Throwable t) {
            MainRegistry.LOGGER.info("HBM GlDebugProbe failed: {}", t.toString());
            callback = null;
        }
    }

    /**
     * Rate-limit gate for HIGH-severity storms. long[3] = {windowStartMs,
     * loggedThisWindow, suppressedThisWindow}. Returns 0 = suppress, 1 = log the
     * line, 2 = log the line AND the call-site stack (first sample of a window).
     */
    private static int highRateAllow(int id) {
        long[] st = HIGH_RATE.computeIfAbsent(id, k -> new long[3]);
        long now = System.currentTimeMillis();
        synchronized (st) {
            if (now - st[0] >= SUMMARY_INTERVAL_MS) {
                if (st[2] > 0) {
                    MainRegistry.LOGGER.error(String.format(
                            "HBM GLDebug: id=%d suppressed %d repeats in the last %d s (GL state keeps erroring; see the sampled lines above)",
                            id, st[2], SUMMARY_INTERVAL_MS / 1000));
                }
                st[0] = now;
                st[1] = 0;
                st[2] = 0;
                return 2;
            }
            if (now - st[0] < MIN_INTERVAL_MS && st[1] >= 1) {
                st[2]++;
                return 0;
            }
            st[1]++;
            return 1;
        }
    }

    private static boolean inCallback;

    /**
     * Snapshot of the mutable GL state that decides "who was drawing what" at
     * the error moment: bound VAO (draws with a stale/zero VAO produce
     * "Array object is not active"), current program (wrong-program rendering =
     * black/wrong-content draws), and the draw FBO. The spec discourages GL
     * calls inside the debug callback (possible re-entry), so this is guarded:
     * a nested callback returns a placeholder instead of recursing.
     */
    private static String describeGlState() {
        if (inCallback) {
            return "(nested callback)";
        }
        inCallback = true;
        try {
            int vao = GL11.glGetInteger(GL30.GL_VERTEX_ARRAY_BINDING);
            int program = GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM);
            int fbo = GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING);
            return "VAO=" + vao + " prog=" + program + " fbo=" + fbo;
        } catch (Throwable t) {
            return "unavailable";
        } finally {
            inCallback = false;
        }
    }
}
