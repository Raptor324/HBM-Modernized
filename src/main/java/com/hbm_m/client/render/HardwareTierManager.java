package com.hbm_m.client.render;

import com.mojang.logging.LogUtils;
import dev.architectury.platform.Platform;
import dev.architectury.utils.Env;
import net.minecraft.client.Minecraft;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL40;
import org.lwjgl.opengl.GL43;
import org.lwjgl.opengl.GLCapabilities;
import org.slf4j.Logger;

/**
 * Centralized, thread-safe hardware capability evaluator and 2-tier architecture manager
 * for the Nucleus rendering engine.
 *
 * <p>Evaluates host graphics capabilities safely at startup and governs dynamic runtime degradation:
 * <ul>
 *   <li><b>Tier 1 (Modern Desktop OpenGL 4.3+):</b> Multi-Draw Indirect, GPU Compute Culling,
 *       SSBO Instance Storage, Hi-Z Occlusion Pyramid, Persistent Staging Ring.</li>
 *   <li><b>Tier 2 (Baseline Fallback OpenGL 3.2 Core):</b> Core Instanced Draws
 *       ({@code glDrawElementsInstanced}), CPU Frustum Culling (Gribb-Hartmann AABB),
 *       Dynamic Streaming VBOs ({@code glBufferSubData}). Guaranteed zero compute/SSBO/MDI calls.</li>
 * </ul>
 *
 * <p>Handles platform quirks and invariants:
 * <ul>
 *   <li>Headless server and datagen safety (never invokes GL or crashes).</li>
 *   <li>Apple macOS OpenGL 4.1 Core Profile hard cap (strictly lacks compute/SSBO/MDI).</li>
 *   <li>Legacy Intel integrated graphics (HD 3000/4000/Haswell) driver instability and UMA bus contention.</li>
 *   <li>Decouples OpenGL 4.4 {@code glBufferStorage} so GL 4.3 desktop GPUs run Tier 1 rendering
 *       with safe VBO streaming upload fallback.</li>
 * </ul>
 *
 * @credit CrankShaft / fewizz / Jdb100
 */
public final class HardwareTierManager {
    private static final Logger LOGGER = LogUtils.getLogger();

    public enum HardwareTier {
        /** Tier 1: Modern Desktop OpenGL 4.3+ (MDI, Compute Occlusion, SSBO, Hi-Z, Persistent Staging). */
        TIER_1_MODERN_GL43,
        /** Tier 2: Baseline Fallback OpenGL 3.2 Core (Instanced Arrays, CPU Frustum Culling, VBO Streaming). */
        TIER_2_BASELINE_GL32;

        // Backwards-compatible aliases for alternative naming conventions
        public static final HardwareTier TIER_1_MODERN = TIER_1_MODERN_GL43;
        public static final HardwareTier TIER_2_LEGACY_CORE = TIER_2_BASELINE_GL32;
    }

    private static volatile boolean probed = false;
    private static volatile HardwareTier activeTier = HardwareTier.TIER_2_BASELINE_GL32;
    private static volatile String degradationReason = null;
    private static volatile boolean runtimeDegraded = false;
    private static volatile boolean persistentStorageSupported = false;

    private HardwareTierManager() {}

    /**
     * Returns the active hardware rendering tier. Probes lazily if not yet resolved.
     */
    public static HardwareTier getActiveTier() {
        if (!probed) {
            probe();
        }
        return activeTier;
    }

    /**
     * Returns true if the host hardware and driver satisfy all prerequisites for Tier 1 Modern (OpenGL 4.3+).
     */
    public static boolean isTier1() {
        return getActiveTier() == HardwareTier.TIER_1_MODERN_GL43;
    }

    /**
     * Returns true if the engine is running on Tier 2 Baseline Fallback (OpenGL 3.2 Core).
     */
    public static boolean isTier2() {
        return getActiveTier() == HardwareTier.TIER_2_BASELINE_GL32;
    }

    /**
     * Returns true if OpenGL 4.4 persistent mapped buffer storage (glBufferStorage) is supported.
     */
    public static boolean isPersistentStorageSupported() {
        if (!probed) {
            getActiveTier();
        }
        return persistentStorageSupported;
    }

    /**
     * Returns true if Tier 1 was demoted to Tier 2 dynamically due to a runtime driver exception.
     */
    public static boolean isRuntimeDegraded() {
        return runtimeDegraded;
    }

    /**
     * Returns the human-readable explanation why the system is running on Tier 2, or null if Tier 1 is active.
     */
    @Nullable
    public static String getDegradationReason() {
        if (!probed) {
            probe();
        }
        return degradationReason;
    }

    /**
     * Performs synchronized, multi-stage hardware capability probing.
     * Guaranteed never to throw an unhandled exception or crash the process.
     */
    public static synchronized void probe() {
        if (probed) {
            return;
        }

        // 1. Guard against dedicated server or headless execution
        if (Platform.getEnvironment() != Env.CLIENT) {
            activeTier = HardwareTier.TIER_2_BASELINE_GL32;
            degradationReason = "Dedicated server environment";
            probed = true;
            return;
        }

        // 2. Guard against premature invocation before GLFW window and context creation
        long context = 0L;
        try {
            context = GLFW.glfwGetCurrentContext();
        } catch (Throwable ignored) {}

        if (context == 0L) {
            // Context not yet bound (early pre-init / static init);
            // do NOT seal probed=true so probe() executes upon first render frame.
            return;
        }

        try {
            // 3. Operating System Gate (macOS capped at 4.1 Core Profile)
            boolean isMac = false;
            try {
                isMac = Minecraft.ON_OSX;
            } catch (Throwable t) {
                String os = System.getProperty("os.name", "").toLowerCase(java.util.Locale.ROOT);
                isMac = os.contains("mac") || os.contains("darwin");
            }

            if (isMac) {
                activeTier = HardwareTier.TIER_2_BASELINE_GL32;
                degradationReason = "Apple macOS OpenGL is capped at 4.1 Core Profile (strictly lacks Compute Shaders, SSBOs, and Multi-Draw Indirect).";
                probed = true;
                logTierResolution();
                return;
            }

            // 4. Query LWJGL GLCapabilities
            GLCapabilities caps = GL.getCapabilities();
            if (caps == null) {
                try {
                    caps = GL.createCapabilities();
                } catch (Throwable ignored) {}
            }

            if (caps == null) {
                activeTier = HardwareTier.TIER_2_BASELINE_GL32;
                degradationReason = "GLCapabilities is null.";
                probed = true;
                logTierResolution();
                return;
            }

            // 5. Evaluate Tier 1 Feature Prerequisites (OpenGL 4.3 Core & Extensions)
            boolean hasCompute = (caps.OpenGL43 || caps.GL_ARB_compute_shader) && caps.glDispatchCompute != 0L;
            boolean hasSsbo = (caps.OpenGL43 || caps.GL_ARB_shader_storage_buffer_object) && caps.glBindBufferBase != 0L;
            boolean hasMdi = (caps.OpenGL43 || caps.GL_ARB_multi_draw_indirect) && caps.glMultiDrawElementsIndirect != 0L;
            boolean hasBaseInstance = (caps.OpenGL42 || caps.GL_ARB_base_instance) && caps.glDrawElementsInstancedBaseVertexBaseInstance != 0L;
            boolean hasImage = (caps.OpenGL42 || (caps.GL_ARB_shader_image_load_store && caps.GL_ARB_shader_image_size))
                    && caps.glBindImageTexture != 0L && caps.glMemoryBarrier != 0L;
            boolean hasTexStorage = (caps.OpenGL42 || caps.GL_ARB_texture_storage) && caps.glTexStorage2D != 0L;

            // Probe OpenGL 4.4 persistent mapped buffer storage independently
            boolean hasBufferStorage = (caps.OpenGL44 || caps.GL_ARB_buffer_storage) && caps.glBufferStorage != 0L && caps.glFenceSync != 0L;

            // 6. Check for legacy Intel integrated GPU instability (Gen7/Gen8/Gen9 HD Graphics & old Iris 5xxx/6xxx)
            String renderer = (caps.glGetString != 0L) ? GL11.glGetString(GL11.GL_RENDERER) : null;
            boolean isLegacyIntel = renderer != null && renderer.contains("Intel")
                    && (renderer.contains("HD Graphics")
                        || renderer.contains("Iris(TM) Graphics")
                        || renderer.contains("Iris(TM) Pro")
                        || renderer.contains("Iris Graphics 5")
                        || renderer.contains("Iris Graphics 6"))
                    && !renderer.contains("Arc") && !renderer.contains("Xe") && !renderer.contains("Plus");

            if (isLegacyIntel) {
                activeTier = HardwareTier.TIER_2_BASELINE_GL32;
                persistentStorageSupported = false;
                degradationReason = "Legacy Intel integrated GPU detected (" + renderer + "). Degraded to Tier 2 to prevent UMA bus contention and driver lockups.";
            } else if (hasCompute && hasSsbo && hasMdi && hasBaseInstance && hasImage && hasTexStorage) {
                activeTier = HardwareTier.TIER_1_MODERN_GL43;
                persistentStorageSupported = hasBufferStorage;
                degradationReason = null;
            } else {
                activeTier = HardwareTier.TIER_2_BASELINE_GL32;
                persistentStorageSupported = false;
                StringBuilder sb = new StringBuilder("Missing Tier 1 features: ");
                if (!hasCompute) sb.append("[Compute Shaders] ");
                if (!hasSsbo) sb.append("[SSBO] ");
                if (!hasMdi) sb.append("[Multi-Draw Indirect] ");
                if (!hasBaseInstance) sb.append("[Base Instance] ");
                if (!hasImage) sb.append("[Image Load/Store] ");
                if (!hasTexStorage) sb.append("[Texture Storage] ");
                degradationReason = sb.toString().trim();
            }

            probed = true;
            logTierResolution();
        } catch (Throwable t) {
            activeTier = HardwareTier.TIER_2_BASELINE_GL32;
            degradationReason = "Exception during capability probe: " + t.getMessage();
            probed = true;
            LOGGER.error("[HBM-M] HardwareTierManager probe failed; defaulting to Tier 2", t);
        }
    }

    private static void logTierResolution() {
        String vendor = "Unknown", renderer = "Unknown", version = "Unknown";
        try {
            GLCapabilities caps = null;
            try {
                caps = GL.getCapabilities();
            } catch (Throwable ignored) {}
            if (caps != null && caps.glGetString != 0L) {
                vendor = GL11.glGetString(GL11.GL_VENDOR);
                renderer = GL11.glGetString(GL11.GL_RENDERER);
                version = GL11.glGetString(GL11.GL_VERSION);
            }
        } catch (Throwable ignored) {}

        if (activeTier == HardwareTier.TIER_1_MODERN_GL43) {
            LOGGER.info("[HBM-M] Nucleus Hardware Tier: TIER 1 (Modern Desktop OpenGL 4.3+). PersistentStorage={}, Vendor='{}', Renderer='{}', Version='{}'",
                    persistentStorageSupported, vendor, renderer, version);
        } else {
            LOGGER.warn("[HBM-M] Nucleus Hardware Tier: TIER 2 (Baseline Fallback OpenGL 3.2 Core). Reason: {}. Vendor='{}', Renderer='{}', Version='{}'",
                    degradationReason, vendor, renderer, version);
        }
    }

    /**
     * Emergency dynamic fail-safe latch: if a Tier 1 draw, compute dispatch, or staging upload
     * fails at runtime, immediately degrades the engine to Tier 2 for the remainder of the session
     * without crashing the client.
     */
    public static synchronized void triggerRuntimeDegradation(String faultContext, Throwable cause) {
        if (activeTier == HardwareTier.TIER_1_MODERN_GL43) {
            activeTier = HardwareTier.TIER_2_BASELINE_GL32;
            runtimeDegraded = true;
            degradationReason = "Runtime failure in " + faultContext + ": " + (cause != null ? cause.getMessage() : "Unknown GL error");
            LOGGER.error("[HBM-M] CRITICAL: Runtime Tier 1 failure detected in '{}'. Gracefully degrading to Tier 2 Baseline for the session.",
                    faultContext, cause);

            // Sanitize active OpenGL bindings only if invoked from a thread with an active OpenGL context
            long context = 0L;
            try {
                context = GLFW.glfwGetCurrentContext();
            } catch (Throwable ignored) {}

            if (context != 0L) {
                try {
                    GLCapabilities caps = null;
                    try {
                        caps = GL.getCapabilities();
                    } catch (Throwable ignored) {}
                    if (caps != null) {
                        // Unbind custom VAO first so VAO 0 is active when buffer bindings are cleared
                        GlVaoSafety.bindVertexArray(0);
                        GL15.glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, 0);
                        GL15.glBindBuffer(GL40.GL_DRAW_INDIRECT_BUFFER, 0);
                        GL15.glBindBuffer(GL43.GL_DISPATCH_INDIRECT_BUFFER, 0);
                        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
                        GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, 0);
                        // Go through GlStateManager, not raw glUseProgram: the raw
                        // call desyncs the managed program cache (and Iris's
                        // ExtendedShader.lastApplied skip-gate) exactly like the
                        // compute passes do - arm ShaderBindResync to force an
                        // honest re-bind on the next ensureFreshBind.
                        com.mojang.blaze3d.platform.GlStateManager._glUseProgram(0);
                        com.hbm_m.client.render.shader.ShaderBindResync.markForeignProgramBind();
                        while (GL11.glGetError() != GL11.GL_NO_ERROR) {
                            // Drain driver error queue completely
                        }
                    }
                } catch (Throwable ignored) {}

                // Release Tier 1 GPU resources only on the thread holding the OpenGL context
                try {
                    NucleusGpuBaker.releaseAll();
                } catch (Throwable ignored) {}
            }

            try {
                MdiBatchCoordinator.discardActiveSessionNoDispatch();
            } catch (Throwable ignored) {}
            // Degradation may fire mid-dispatch (MdiBatchCoordinator.executeMdiGlDraw
            // failure path), so NOT clearCachedRedraw(): its snapshot-pool /
            // cmdBufScratch frees are documented event-driven-only. The retained
            // records themselves are safe to drop from any failure path and would
            // otherwise keep referencing the GPU content we just invalidated.
            try {
                MdiBatchCoordinator.dropRetainedForDegradation();
            } catch (Throwable ignored) {}
            try {
                com.hbm_m.client.render.retained.RetainedInstanceGraph.get().clear();
            } catch (Throwable ignored) {}
        }
    }

    /**
     * Test-only helper to reset probing state during automated unit/game testing.
     */
    public static synchronized void resetForTest() {
        probed = false;
        activeTier = HardwareTier.TIER_2_BASELINE_GL32;
        degradationReason = null;
        runtimeDegraded = false;
        persistentStorageSupported = false;
    }
}
