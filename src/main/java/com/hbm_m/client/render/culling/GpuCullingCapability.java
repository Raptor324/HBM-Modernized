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
    private GpuCullingCapability() {}

    /**
     * Returns true if the active hardware supports OpenGL 4.3 GPU compute occlusion culling.
     */
    public static boolean isSupported() {
        return com.hbm_m.client.render.HardwareTierManager.isTier1();
    }

    /**
     * Returns the human-readable reason why GPU culling is unsupported, or null if supported.
     */
    @Nullable
    public static String getUnsupportedReason() {
        return com.hbm_m.client.render.HardwareTierManager.getDegradationReason();
    }

    /**
     * Performs thread-safe lazy probing of GPU capabilities.
     * Exception-shielded to ensure this method never throws.
     */
    public static synchronized void checkCapabilities() {
        com.hbm_m.client.render.HardwareTierManager.probe();
    }
}
