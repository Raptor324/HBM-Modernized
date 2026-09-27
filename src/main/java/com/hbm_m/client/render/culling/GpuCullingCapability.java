package com.hbm_m.client.render.culling;

import com.mojang.logging.LogUtils;
import dev.architectury.platform.Platform;
import dev.architectury.utils.Env;
import net.minecraft.client.Minecraft;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GLCapabilities;
import org.slf4j.Logger;

/**
 * Thread-safe, lazily initialized capability probe for OpenGL 4.3 GPU occlusion culling.
 *
 * <p>Safely verifies all required OpenGL extensions and entrypoints (Compute Shaders,
 * SSBO, Image Load/Store, Texture Storage) with multi-stage guards against:
 * <ul>
 *   <li>Headless / Dedicated Server execution (early return via {@link Platform#getEnvironment()})</li>
 *   <li>Premature invocation before GLFW window/context creation (early return when context is 0L)</li>
 *   <li>Apple macOS OpenGL 4.1 Core Profile cap without compute shader support</li>
 *   <li>Legacy GPUs / driver bugs missing required native function pointers</li>
 * </ul>
 *
 * @credit CrankShaft / fewizz / Jdb100
 */
public final class GpuCullingCapability {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static volatile boolean checked = false;
    private static volatile boolean supported = false;
    private static volatile String unsupportedReason = null;

    private GpuCullingCapability() {}

    /**
     * Returns true if the active hardware supports OpenGL 4.3 GPU compute occlusion culling.
     */
    public static boolean isSupported() {
        if (!checked) {
            checkCapabilities();
        }
        return supported;
    }

    /**
     * Returns the human-readable reason why GPU culling is unsupported, or null if supported.
     */
    @Nullable
    public static String getUnsupportedReason() {
        if (!checked) {
            checkCapabilities();
        }
        return unsupportedReason;
    }

    /**
     * Performs thread-safe lazy probing of GPU capabilities.
     * Exception-shielded to ensure this method never throws.
     */
    public static synchronized void checkCapabilities() {
        if (checked) {
            return;
        }

        try {
            // 1. Guard against dedicated server / headless environment
            if (Platform.getEnvironment() != Env.CLIENT) {
                supported = false;
                unsupportedReason = "Headless/Server environment";
                checked = true;
                return;
            }

            // 2. Guard against premature invocation before GLFW window creation
            long currentContext = 0L;
            try {
                currentContext = GLFW.glfwGetCurrentContext();
            } catch (Throwable t) {
                supported = false;
                unsupportedReason = "No active OpenGL context";
                return; // Do NOT seal checked=true; re-probe when context is available
            }
            if (currentContext == 0L) {
                supported = false;
                unsupportedReason = "No active OpenGL context";
                return; // Do NOT seal checked=true; re-probe when context is available
            }

            // 3. Apple macOS Check (Apple Core Profile capped at 4.1; no compute shaders or SSBOs)
            boolean isMac = false;
            try {
                isMac = Minecraft.ON_OSX;
            } catch (Throwable t) {
                String os = System.getProperty("os.name", "").toLowerCase(java.util.Locale.ROOT);
                isMac = os.contains("mac") || os.contains("darwin");
            }
            if (isMac) {
                supported = false;
                unsupportedReason = "macOS OpenGL is capped at 4.1 without compute shader support.";
                checked = true;
                LOGGER.info("[HBM-M] GPU Culling capability: unsupported ({})", unsupportedReason);
                return;
            }

            // 4. Query LWJGL GLCapabilities safely with exception shielding
            GLCapabilities caps;
            try {
                caps = GL.getCapabilities();
            } catch (Throwable t) {
                supported = false;
                unsupportedReason = "Failed to query OpenGL capabilities: " + t.getMessage();
                checked = true;
                LOGGER.warn("[HBM-M] GPU Culling capability: {}", unsupportedReason);
                return;
            }

            if (caps == null) {
                supported = false;
                unsupportedReason = "GLCapabilities is null";
                checked = true;
                LOGGER.warn("[HBM-M] GPU Culling capability: {}", unsupportedReason);
                return;
            }

            // 5. OpenGL 4.3 or GL_ARB_compute_shader check
            boolean hasCompute = (caps.OpenGL43 || caps.GL_ARB_compute_shader) && caps.glDispatchCompute != 0L;
            if (!hasCompute) {
                supported = false;
                unsupportedReason = "OpenGL 4.3 compute shaders are not supported.";
                checked = true;
                LOGGER.info("[HBM-M] GPU Culling capability: unsupported (GL43={}, ARB_compute={}, dispatchCompute={})",
                        caps.OpenGL43, caps.GL_ARB_compute_shader, caps.glDispatchCompute != 0L);
                return;
            }

            // 6. Shader Storage Buffer Object (SSBO) check
            boolean hasSsbo = (caps.OpenGL43 || caps.GL_ARB_shader_storage_buffer_object) && caps.glBindBufferBase != 0L;
            if (!hasSsbo) {
                supported = false;
                unsupportedReason = "OpenGL 4.3 SSBO (Shader Storage Buffer Object) is not supported.";
                checked = true;
                LOGGER.info("[HBM-M] GPU Culling capability: unsupported (GL43={}, ARB_ssbo={}, bindBufferBase={})",
                        caps.OpenGL43, caps.GL_ARB_shader_storage_buffer_object, caps.glBindBufferBase != 0L);
                return;
            }

            // 7. Image Load/Store & Memory Barrier check (for downsampling mip pyramid)
            boolean hasImage = (caps.OpenGL42 || (caps.GL_ARB_shader_image_load_store && caps.GL_ARB_shader_image_size))
                    && caps.glBindImageTexture != 0L && caps.glMemoryBarrier != 0L;
            if (!hasImage) {
                supported = false;
                unsupportedReason = "OpenGL 4.2 shader image load/store or memory barrier is not supported.";
                checked = true;
                LOGGER.info("[HBM-M] GPU Culling capability: unsupported (GL42={}, ARB_image={}, bindImageTexture={}, glMemoryBarrier={})",
                        caps.OpenGL42, caps.GL_ARB_shader_image_load_store, caps.glBindImageTexture != 0L, caps.glMemoryBarrier != 0L);
                return;
            }

            // 8. Texture Storage check (for immutable mipmap chain allocation)
            boolean hasTexStorage = (caps.OpenGL42 || caps.GL_ARB_texture_storage) && caps.glTexStorage2D != 0L;
            if (!hasTexStorage) {
                supported = false;
                unsupportedReason = "OpenGL 4.2 texture storage (glTexStorage2D) is not supported.";
                checked = true;
                LOGGER.info("[HBM-M] GPU Culling capability: unsupported (GL42={}, ARB_texture_storage={}, glTexStorage2D={})",
                        caps.OpenGL42, caps.GL_ARB_texture_storage, caps.glTexStorage2D != 0L);
                return;
            }

            // All capabilities satisfied
            supported = true;
            unsupportedReason = null;
            checked = true;
            LOGGER.info("[HBM-M] GPU Culling capability: supported (OpenGL 4.3+ Compute, SSBO, Image Load/Store, Texture Storage)");
        } catch (Throwable t) {
            supported = false;
            unsupportedReason = "Unexpected error probing GPU capabilities: " + t.getMessage();
            checked = true;
            LOGGER.error("[HBM-M] Unexpected error probing GPU capabilities", t);
        }
    }
}
