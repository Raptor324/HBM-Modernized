package com.hbm_m.client.render;

import org.jetbrains.annotations.Nullable;
import org.lwjgl.opengl.ARBDrawInstanced;
import org.lwjgl.opengl.ARBInstancedArrays;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL31;
import org.lwjgl.opengl.GL33;
import org.lwjgl.opengl.GLCapabilities;
import org.lwjgl.glfw.GLFW;

//? if forge {
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
//?}
//? if fabric {
/*import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
*///?}

/**
 * Static GL compatibility helpers for instanced rendering.
 * <p>
 * Extracted from {@link InstancedStaticPartRenderer} to keep the main
 * renderer focused on batching logic. All methods are stateless and
 * thread-safe (they only query/call GL entry points).
 */
//? if forge {
@OnlyIn(Dist.CLIENT)
//?}
//? if fabric {
/*@Environment(EnvType.CLIENT)*///?}
public final class InstancedGlCompat {

    private InstancedGlCompat() {}

    /**
     * Checks whether the current GL context supports both
     * {@code glVertexAttribDivisor} (core or ARB) and
     * {@code glDrawElementsInstanced} (core or ARB).
     * <p>
     * Without a bound GLFW context, returns {@code false} to avoid
     * stale TLS capabilities left by Sodium/Iris worker threads.
     */
    public static boolean supportsInstancedAttributeDivisor() {
        try {
            var caps = resolveGlCapabilities();
            if (caps == null) {
                return false;
            }
            boolean hasDivisor = caps.glVertexAttribDivisor != 0L || caps.glVertexAttribDivisorARB != 0L;
            boolean hasDrawInstanced = caps.glDrawElementsInstanced != 0L || caps.glDrawElementsInstancedARB != 0L;
            return hasDivisor && hasDrawInstanced;
        } catch (Throwable ignored) {
            return false;
        }
    }

    @Nullable
    static GLCapabilities resolveGlCapabilities() {
        if (GLFW.glfwGetCurrentContext() == 0L) {
            return null;
        }
        var caps = GL.getCapabilities();
        if (caps != null) {
            return caps;
        }
        try {
            GL.createCapabilities();
        } catch (Throwable ignored) {
            return null;
        }
        return GL.getCapabilities();
    }

    /**
     * Core GL 3.3 divisor, otherwise {@link ARBInstancedArrays}.
     * Must agree with the check in {@link #supportsInstancedAttributeDivisor()}.
     */
    public static void glVertexAttribDivisorCompat(int index, int divisor) {
        GLCapabilities caps = GL.getCapabilities();
        if (caps != null && caps.glVertexAttribDivisor != 0L) {
            GL33.glVertexAttribDivisor(index, divisor);
        } else {
            ARBInstancedArrays.glVertexAttribDivisorARB(index, divisor);
        }
    }

    /**
     * Core GL 3.1 draw instanced, otherwise {@link ARBDrawInstanced}.
     */
    public static void glDrawElementsInstancedCompat(int mode, int count, int type, long indices, int primcount) {
        GLCapabilities caps = GL.getCapabilities();
        if (caps != null && caps.glDrawElementsInstanced != 0L) {
            GL31.glDrawElementsInstanced(mode, count, type, indices, primcount);
        } else {
            ARBDrawInstanced.glDrawElementsInstancedARB(mode, count, type, indices, primcount);
        }
    }
}
