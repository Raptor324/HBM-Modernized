package com.hbm_m.client.render.debug;

//? if forge {
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
//?} elif neoforge {
/*import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
*///?}

import java.nio.ByteBuffer;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL43;
import org.lwjgl.opengl.GLCapabilities;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

/**
 * Zero-overhead OpenGL KHR_debug / GL 4.3 instrumentation helper.
 * <p>
 * In production builds ({@code hbm.glDebug} unset / false), {@link #ENABLED} is {@code false},
 * allowing the HotSpot C2 JIT compiler to eliminate dead branches and string formatting at call sites
 * guarded by {@code if (GlDebugMarkers.ENABLED)} or {@code if (GlDebugMarkers.isActive())}.
 */
@OnlyIn(Dist.CLIENT)
public final class GlDebugMarkers {

    public static final boolean ENABLED = Boolean.getBoolean("hbm.glDebug");

    private static boolean supportedChecked = false;
    private static boolean supported = false;
    private static boolean debugOutputInitialized = false;

    private GlDebugMarkers() {}

    /**
     * Checks if OpenGL debug markers and labeling are supported by the current GL context.
     */
    public static boolean isSupported() {
        if (!ENABLED) return false;
        if (!supportedChecked) {
            try {
                GLCapabilities caps = GL.getCapabilities();
                if (caps != null) {
                    supported = caps.GL_KHR_debug || caps.OpenGL43;
                    supportedChecked = true;
                    if (supported && !debugOutputInitialized) {
                        try {
                            GL11.glEnable(GL43.GL_DEBUG_OUTPUT);
                            GL11.glEnable(GL43.GL_DEBUG_OUTPUT_SYNCHRONOUS);
                            debugOutputInitialized = true;
                        } catch (Throwable ignored) {
                        }
                    }
                }
            } catch (Throwable t) {
                return false;
            }
        }
        return supported;
    }

    /**
     * True if debug markers are enabled via system property and supported by the driver.
     */
    public static boolean isActive() {
        return ENABLED && isSupported();
    }

    private static String truncate(String s) {
        if (s == null) return "";
        return s.length() > 128 ? s.substring(0, 128) : s;
    }

    /**
     * Pushes a debug group onto the OpenGL debug group stack.
     */
    public static void pushGroup(int id, String message) {
        if (!isActive() || message == null) return;
        String str = truncate(message);
        try (MemoryStack stack = MemoryStack.stackPush()) {
            ByteBuffer buf = stack.UTF8(str, false);
            GL43.nglPushDebugGroup(GL43.GL_DEBUG_SOURCE_APPLICATION, id, buf.remaining(), MemoryUtil.memAddress(buf));
        } catch (Throwable ignored) {
        }
    }

    /**
     * Pushes a debug group with default id 0.
     */
    public static void pushGroup(String message) {
        pushGroup(0, message);
    }

    /**
     * Pops the top debug group from the OpenGL debug group stack.
     */
    public static void popGroup() {
        if (!isActive()) return;
        try {
            GL43.glPopDebugGroup();
        } catch (Throwable ignored) {
        }
    }

    /**
     * Inserts a debug marker/event.
     */
    public static void insertMarker(int id, String message) {
        if (!isActive() || message == null) return;
        String str = truncate(message);
        try (MemoryStack stack = MemoryStack.stackPush()) {
            ByteBuffer buf = stack.UTF8(str, false);
            GL43.nglDebugMessageInsert(
                GL43.GL_DEBUG_SOURCE_APPLICATION,
                GL43.GL_DEBUG_TYPE_MARKER,
                id,
                GL43.GL_DEBUG_SEVERITY_NOTIFICATION,
                buf.remaining(),
                MemoryUtil.memAddress(buf)
            );
        } catch (Throwable ignored) {
        }
    }

    /**
     * Inserts a debug marker/event with default id 0.
     */
    public static void insertMarker(String message) {
        insertMarker(0, message);
    }

    /**
     * Labels an OpenGL object with a human-readable identifier.
     */
    public static void labelObject(int identifier, int name, String label) {
        if (!isActive() || name <= 0 || label == null) return;
        String str = truncate(label);
        try (MemoryStack stack = MemoryStack.stackPush()) {
            ByteBuffer buf = stack.UTF8(str, false);
            GL43.nglObjectLabel(identifier, name, buf.remaining(), MemoryUtil.memAddress(buf));
        } catch (Throwable ignored) {
        }
    }

    /**
     * Labels an OpenGL buffer object (GL_BUFFER = 0x82E0).
     */
    public static void labelBuffer(int id, String label) {
        labelObject(GL43.GL_BUFFER, id, label);
    }

    /**
     * Labels an OpenGL vertex array object (GL_VERTEX_ARRAY = 0x8074).
     */
    public static void labelVao(int id, String label) {
        labelObject(GL43.GL_VERTEX_ARRAY, id, label);
    }

    /**
     * Labels an OpenGL shader program object (GL_PROGRAM = 0x82E2).
     */
    public static void labelProgram(int id, String label) {
        labelObject(GL43.GL_PROGRAM, id, label);
    }

    /**
     * Labels an OpenGL texture object (GL_TEXTURE = 0x1702).
     */
    public static void labelTexture(int id, String label) {
        labelObject(GL43.GL_TEXTURE, id, label);
    }
}
