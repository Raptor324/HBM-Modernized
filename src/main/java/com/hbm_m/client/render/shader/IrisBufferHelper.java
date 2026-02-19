package com.hbm_m.client.render.shader;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;

import java.lang.reflect.Method;

/**
 * Хелпер для совместимости BufferBuilder с Iris/Oculus.
 * <p>
 * <b>Когда НЕ использовать:</b> При level render (RenderLevelStageEvent, block entities) с
 * включёнными шейдерами ({@link ShaderCompatibilityDetector#isExternalShaderActive()}).
 * В этом случае вызывайте {@code buffer.begin(mode, DefaultVertexFormat.BLOCK)} напрямую —
 * MixinBufferBuilder расширит формат до TERRAIN, и putBulkData будет дополняться extended data.
 * IrisBufferHelper отключает расширение и приведёт к stride mismatch.
 * <p>
 * <b>Когда использовать:</b> GUI, overlay, не-level рендер — когда нужен именно BLOCK без
 * расширения. Iris предоставляет iris$beginWithoutExtending() — отключает расширение формата.
 * Вызываем через reflection, т.к. Oculus — опциональная зависимость.
 */
public final class IrisBufferHelper {

    private static final int GL_QUADS = 7;

    private static Method irisBeginWithoutExtending;
    private static boolean irisChecked;

    /**
     * Начинает BufferBuilder с DefaultVertexFormat.BLOCK без расширения Iris.
     * При активном Iris/Oculus предотвращает переключение на IrisVertexFormats.TERRAIN.
     * <p>
     * Не использовать для level render с шейдерами — там нужен расширенный TERRAIN формат.
     */
    public static void beginBlockQuads(BufferBuilder buffer) {
        begin(buffer, VertexFormat.Mode.QUADS, DefaultVertexFormat.BLOCK);
    }

    /**
     * Универсальный begin с отключением Iris-расширения при необходимости.
     * Для BLOCK/NEW_ENTITY/POSITION_COLOR_TEX_LIGHTMAP вызывает iris$beginWithoutExtending.
     * <p>
     * Не использовать при рендере block entities во время renderLevel с включёнными шейдерами.
     */
    public static void begin(BufferBuilder buffer, VertexFormat.Mode mode, VertexFormat format) {
        if (format != DefaultVertexFormat.BLOCK && format != DefaultVertexFormat.NEW_ENTITY
                && format != DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP) {
            buffer.begin(mode, format);
            return;
        }
        boolean usedIris = tryIrisBeginWithoutExtending(buffer, getGlMode(mode), format);
        if (usedIris) return;
        buffer.begin(mode, format);
    }

    private static int getGlMode(VertexFormat.Mode mode) {
        return switch (mode) {
            case QUADS -> 7;           // GL_QUADS
            case TRIANGLES -> 4;       // GL_TRIANGLES
            case LINES -> 1;           // GL_LINES
            case LINE_STRIP -> 3;      // GL_LINE_STRIP
            case TRIANGLE_STRIP -> 5; // GL_TRIANGLE_STRIP
            case TRIANGLE_FAN -> 6;   // GL_TRIANGLE_FAN
            default -> GL_QUADS;
        };
    }

    private static boolean tryIrisBeginWithoutExtending(BufferBuilder buffer, int drawMode, VertexFormat vertexFormat) {
        if (!irisChecked) {
            irisChecked = true;
            try {
                Class<?> iface = Class.forName("net.coderbot.iris.vertices.ExtendingBufferBuilder");
                if (iface.isInstance(buffer)) {
                    irisBeginWithoutExtending = iface.getMethod("iris$beginWithoutExtending", int.class, VertexFormat.class);
                }
            } catch (ClassNotFoundException | NoSuchMethodException ignored) {
                // Iris/Oculus не установлен
            }
        }
        if (irisBeginWithoutExtending != null) {
            try {
                irisBeginWithoutExtending.invoke(buffer, drawMode, vertexFormat);
                return true;
            } catch (Exception ignored) {
            }
        }
        return false;
    }
}
