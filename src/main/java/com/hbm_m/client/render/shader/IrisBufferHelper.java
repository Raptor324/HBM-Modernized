package com.hbm_m.client.render.shader;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;

import java.lang.reflect.Method;

/**
 * BufferBuilder compatibility helper for Iris/Oculus.
 *
 * <p><b>When NOT to use:</b> during level render (RenderLevelStageEvent, block
 * entities) with shaders enabled
 * ({@link ShaderCompatibilityDetector#isExternalShaderActive()}). In that case
 * call {@code buffer.begin(mode, DefaultVertexFormat.BLOCK)} directly -
 * MixinBufferBuilder will extend the format to TERRAIN and putBulkData will be
 * backfilled with extended data. IrisBufferHelper disables the extension and
 * would cause a stride mismatch.</p>
 *
 * <p><b>When to use:</b> GUI, overlays, non-level render - when BLOCK without
 * extension is specifically needed. Iris provides iris$beginWithoutExtending(),
 * which disables format extension. Invoked via reflection because Oculus is an
 * optional dependency.</p>
 */
public final class IrisBufferHelper {

    private static final int GL_QUADS = 7;

    private static Method irisBeginWithoutExtending;
    private static boolean irisChecked;

    /**
     * Cross-version {@link BufferBuilder} factory.
     * <p>
     * On 1.20.1 the constructor is {@code new BufferBuilder(int capacity)} and
     * {@code begin(mode, format)} is called separately. On 1.21.1 the constructor
     * requires {@code (VertexFormat, VertexFormat.Mode, int)} immediately. This
     * method hides the difference and returns a builder ready to be filled.
     *
     * @param mode     vertex mode (QUADS, TRIANGLES, etc.)
     * @param format   vertex format (DefaultVertexFormat.BLOCK, etc.)
     * @param capacity initial capacity in bytes (allocation hint)
     * @return a new BufferBuilder with {@code begin} already called (format fixed)
     */
    public static BufferBuilder create(VertexFormat.Mode mode, VertexFormat format, int capacity) {
        //? if < 1.21.1 {
        BufferBuilder buffer = new BufferBuilder(capacity);
        buffer.begin(mode, format);
        return buffer;
        //?} else {
        /*return new BufferBuilder(new com.mojang.blaze3d.vertex.ByteBufferBuilder(capacity), mode, format);
        *///?}
    }

    /**
     * ExtendingBufferBuilder class from Connector/FFAPI (Forgified Fabric API).
     * Connector uses the same interface as Iris, but in a different package.
     */
    private static Method connectorBeginWithoutExtending;
    private static boolean connectorChecked;

    /**
     * Begins a BufferBuilder with DefaultVertexFormat.BLOCK without Iris
     * extension. Prevents the switch to IrisVertexFormats.TERRAIN when
     * Iris/Oculus is active.
     * <p>
     * Do not use for level render with shaders - the extended TERRAIN format
     * is required there.
     */
    public static void beginBlockQuads(BufferBuilder buffer) {
        begin(buffer, VertexFormat.Mode.QUADS, DefaultVertexFormat.BLOCK);
    }

    /**
     * {@code begin} without Iris extension - for {@code POSITION_TEX_COLOR} and
     * other immediate-draw paths.
     */
    public static void beginWithoutExtending(BufferBuilder buffer, VertexFormat.Mode mode, VertexFormat format) {
        if (tryIrisBeginWithoutExtending(buffer, mode, format)) {
            return;
        }
        //? if < 1.21.1 {
        buffer.begin(mode, format);
        //?}
    }

    /**
     * Universal begin that disables the Iris extension when needed.
     * For BLOCK/NEW_ENTITY/POSITION_COLOR_TEX_LIGHTMAP calls
     * iris$beginWithoutExtending.
     * <p>
     * Do not use when rendering block entities during renderLevel with shaders
     * enabled.
     */
    public static void begin(BufferBuilder buffer, VertexFormat.Mode mode, VertexFormat format) {
        if (format != DefaultVertexFormat.BLOCK && format != DefaultVertexFormat.NEW_ENTITY
                && format != DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP) {
            //? if < 1.21.1 {
            buffer.begin(mode, format);
            //?}
            return;
        }
        if (tryIrisBeginWithoutExtending(buffer, mode, format)) {
            return;
        }
        //? if < 1.21.1 {
        buffer.begin(mode, format);
        //?}
    }

    private static int getGlMode(VertexFormat.Mode mode) {
        return switch (mode) {
            case QUADS -> GL_QUADS;
            case TRIANGLES -> 4;
            case LINES -> 1;
            case LINE_STRIP -> 3;
            case TRIANGLE_STRIP -> 5;
            case TRIANGLE_FAN -> 6;
            default -> GL_QUADS;
        };
    }

    private static boolean tryIrisBeginWithoutExtending(BufferBuilder buffer, VertexFormat.Mode drawMode, VertexFormat vertexFormat) {
        // --- Iris / Oculus (1.20+) ---
        if (!irisChecked) {
            irisChecked = true;
            for (String className : new String[]{
                    "net.irisshaders.iris.vertices.ExtendingBufferBuilder",
                    "net.coderbot.iris.vertices.ExtendingBufferBuilder"
            }) {
                try {
                    Class<?> iface = Class.forName(className);
                    if (iface.isInstance(buffer)) {
                        irisBeginWithoutExtending = iface.getMethod("iris$beginWithoutExtending", VertexFormat.Mode.class, VertexFormat.class);
                        break;
                    }
                } catch (ClassNotFoundException | NoSuchMethodException ignored) {
                }
            }
        }
        if (irisBeginWithoutExtending != null) {
            try {
                irisBeginWithoutExtending.invoke(buffer, drawMode, vertexFormat);
                return true;
            } catch (Exception ignored) {
            }
        }

        // --- Connector / Forgified Fabric API (FFAPI) ---
        int glMode = getGlMode(drawMode);
        if (!connectorChecked) {
            connectorChecked = true;
            try {
                Class<?> iface = Class.forName("com.sinytra.forgified_fabric_api.fabric.mixin.renderer.indigo.MixinBufferBuilder");
                if (iface.isInstance(buffer)) {
                    connectorBeginWithoutExtending = iface.getMethod("iris$beginWithoutExtending", int.class, VertexFormat.class);
                }
            } catch (ClassNotFoundException | NoSuchMethodException ignored) {
            }
            if (connectorBeginWithoutExtending == null) {
                try {
                    Class<?> iface = Class.forName("link.infra.indium.renderer.render.ExtendingBufferBuilder");
                    if (iface.isInstance(buffer)) {
                        connectorBeginWithoutExtending = iface.getMethod("iris$beginWithoutExtending", int.class, VertexFormat.class);
                    }
                } catch (ClassNotFoundException | NoSuchMethodException ignored) {
                }
            }
        }
        if (connectorBeginWithoutExtending != null) {
            try {
                connectorBeginWithoutExtending.invoke(buffer, glMode, vertexFormat);
                return true;
            } catch (Exception ignored) {
            }
        }

        return false;
    }
}