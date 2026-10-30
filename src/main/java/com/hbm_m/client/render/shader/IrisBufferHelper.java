package com.hbm_m.client.render.shader;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;


/**
 * BufferBuilder compatibility helper for Iris/Oculus.
 *
 * <p>Attribution / Credits:
 * Adapted from Iris (https://github.com/IrisShaders/Iris, LGPL-3.0) and
 * Oculus (https://github.com/Asek3/Oculus, LGPL-3.0).
 * Authors: coderbot, IMS212, Iris Team, Asek3.</p>
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
     * {@code begin} without Iris extension - for {@code POSITION_TEX_COLOR} and
     * other immediate-draw paths.
     */
    public static void beginWithoutExtending(BufferBuilder buffer, VertexFormat.Mode mode, VertexFormat format) {
        if (IrisReflectionBridge.beginWithoutExtending(buffer, mode, format)) {
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
        if (IrisReflectionBridge.beginWithoutExtending(buffer, mode, format)) {
            return;
        }
        //? if < 1.21.1 {
        buffer.begin(mode, format);
        //?}
    }
}