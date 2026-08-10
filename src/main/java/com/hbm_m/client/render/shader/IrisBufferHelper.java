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
 * В этом случае вызывайте {@code buffer.begin(mode, DefaultVertexFormat.BLOCK)} напрямую -
 * MixinBufferBuilder расширит формат до TERRAIN, и putBulkData будет дополняться extended data.
 * IrisBufferHelper отключает расширение и приведёт к stride mismatch.
 * <p>
 * <b>Когда использовать:</b> GUI, overlay, не-level рендер - когда нужен именно BLOCK без
 * расширения. Iris предоставляет iris$beginWithoutExtending() - отключает расширение формата.
 * Вызываем через reflection, т.к. Oculus - опциональная зависимость.
 */
public final class IrisBufferHelper {

    private static final int GL_QUADS = 7;

    private static Method irisBeginWithoutExtending;
    private static boolean irisChecked;

    /**
     * Кросс-версионная фабрика {@link BufferBuilder}.
     * <p>
     * На 1.20.1 конструктор {@code new BufferBuilder(int capacity)}, а {@code begin(mode, format)}
     * вызывается отдельно. На 1.21.1 конструктор требует {@code (VertexFormat, VertexFormat.Mode, int)}
     * сразу. Этот метод инкапсулирует различие и возвращает builder, готовый к заполнению.
     *
     * @param mode     режим вершин (QUADS, TRIANGLES, …)
     * @param format   формат вершин (DefaultVertexFormat.BLOCK, …)
     * @param capacity начальная вместимость в байтах (хинт аллокации)
     * @return новый BufferBuilder с уже вызванным {@code begin} (формат зафиксирован)
     */
    public static BufferBuilder create(VertexFormat.Mode mode, VertexFormat format, int capacity) {
        //? if forge {
        BufferBuilder buffer = new BufferBuilder(capacity);
        buffer.begin(mode, format);
        return buffer;
        //?}
        //? if neoforge {
        /*return new BufferBuilder(format, mode, capacity);
        *///?}
    }

    /**
     * Класс ExtendingBufferBuilder от Connector/FFAPI (Forgified Fabric API).
     * Connector использует тот же интерфейс что и Iris, но в другом пакете.
     */
    private static Method connectorBeginWithoutExtending;
    private static boolean connectorChecked;

    /**
     * Начинает BufferBuilder с DefaultVertexFormat.BLOCK без расширения Iris.
     * При активном Iris/Oculus предотвращает переключение на IrisVertexFormats.TERRAIN.
     * <p>
     * Не использовать для level render с шейдерами - там нужен расширенный TERRAIN формат.
     */
    public static void beginBlockQuads(BufferBuilder buffer) {
        begin(buffer, VertexFormat.Mode.QUADS, DefaultVertexFormat.BLOCK);
    }

    /**
     * {@code begin} без расширения Iris — для {@code POSITION_TEX_COLOR} и прочих immediate-draw путей.
     */
    public static void beginWithoutExtending(BufferBuilder buffer, VertexFormat.Mode mode, VertexFormat format) {
        if (tryIrisBeginWithoutExtending(buffer, mode, format)) {
            return;
        }
        buffer.begin(mode, format);
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
        if (tryIrisBeginWithoutExtending(buffer, mode, format)) {
            return;
        }
        buffer.begin(mode, format);
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