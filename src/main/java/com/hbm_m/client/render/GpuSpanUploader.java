package com.hbm_m.client.render;

//? if forge {
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
//?} elif neoforge {
/*import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
*///?}

import java.nio.FloatBuffer;

import org.lwjgl.opengl.GL15;
import org.lwjgl.system.MemoryUtil;

@OnlyIn(Dist.CLIENT)
/**
 * Span-дифф аплоадов инстанс-данных (dirty-detect уровня 2).
 * <p>
 * У вызывающего есть CPU-копия («shadow») содержимого GPU-буфера.
 * {@link #diffUpload} сравнивает новое окно данных с теневой копией и грузит
 * только изменившиеся диапазоны: статичная сцена с мировыми координатами записей
 * (FrameViewState) даёт ноль аплоадов, чистое вращение части — 16-байтный span
 * вместо полной записи 120 байт (эквивалент OrientedInstance по трафику).
 * <p>
 * Инвариант: скипнутые span-ы обязаны уже присутствовать в GPU-буфере —
 * orphan (glBufferData) на этих путях ЗАПРЕЩЁН, он уничтожает содержимое,
 * которое shadow считает актуальным.
 * <p>
 * Канал доставки span-а — {@link PersistentUploadStaging} (persistent-mapped
 * кольцо + glCopyBufferSubData); при его отсутствии — glBufferSubData.
 */
public final class GpuSpanUploader {

    /** Больше такого числа span-ов в окне — дешевле залить окно целиком. */
    private static final int MAX_SPANS = 48;
    /** Неизменные промежутки короче этого (флоатов) приклеиваются к соседнему span-у. */
    private static final int MERGE_GAP_FLOATS = 24;

    private GpuSpanUploader() {}

    /**
     * Полный аплоад окна + синхронизация shadow (первый кадр, рост буфера,
     * переполнение span-логики).
     */
    public static void fullUpload(FloatBuffer shadow, int shadowFloatOffset,
                                  FloatBuffer src, int srcFloatOffset,
                                  int floatCount, int destVbo) {
        uploadSpan(src, srcFloatOffset, floatCount, shadowFloatOffset, destVbo);
        copyToShadow(shadow, shadowFloatOffset, src, srcFloatOffset, floatCount);
    }

    /**
     * Дифф окна {@code src[srcFloatOffset, +floatCount)} против
     * {@code shadow[shadowFloatOffset, +floatCount)} и аплоад только отличий.
     * shadow обновляется по факту аплоада. Буферы — direct (memAlloc*).
     */
    public static void diffUpload(FloatBuffer shadow, FloatBuffer src,
                                  int srcFloatOffset, int shadowFloatOffset,
                                  int floatCount, int destVbo) {
        int[] spans = new int[MAX_SPANS * 2];
        int n = 0;
        boolean overflow = false;
        int i = 0;
        while (i < floatCount) {
            if (src.get(srcFloatOffset + i) == shadow.get(shadowFloatOffset + i)) {
                i++;
                continue;
            }
            int start = i;
            int end = i + 1;
            int gap = 0;
            i++;
            while (i < floatCount) {
                if (src.get(srcFloatOffset + i) != shadow.get(shadowFloatOffset + i)) {
                    end = i + 1;
                    gap = 0;
                } else {
                    gap++;
                    if (gap >= MERGE_GAP_FLOATS) {
                        break;
                    }
                }
                i++;
            }
            if (n >= MAX_SPANS) {
                overflow = true;
                break;
            }
            spans[n * 2] = start;
            spans[n * 2 + 1] = end;
            n++;
        }
        if (overflow) {
            fullUpload(shadow, shadowFloatOffset, src, srcFloatOffset, floatCount, destVbo);
            return;
        }
        for (int k = 0; k < n; k++) {
            int start = spans[k * 2];
            int end = spans[k * 2 + 1];
            uploadSpan(src, srcFloatOffset + start, end - start, shadowFloatOffset + start, destVbo);
            copyToShadow(shadow, shadowFloatOffset + start, src, srcFloatOffset + start, end - start);
        }
    }

    private static void uploadSpan(FloatBuffer src, int srcFloatOffset, int floatCount,
                                   int destFloatOffset, int destVbo) {
        if (floatCount <= 0) {
            return;
        }
        NucleusDebug.recordUpload(1, (long) floatCount * 4L);
        long destByteOffset = (long) destFloatOffset * 4L;
        PersistentUploadStaging staging = PersistentUploadStaging.getOrCreate();
        if (staging != null
                && staging.copyRegion(src, srcFloatOffset, floatCount, destVbo, destByteOffset)) {
            return;
        }
        // Fallback: прямая загрузка из client-memory буфера (может имплицитно
        // синхронизировать драйвер, но корректно).
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, destVbo);
        FloatBuffer view = src.duplicate();
        view.position(srcFloatOffset);
        view.limit(srcFloatOffset + floatCount);
        GL15.glBufferSubData(GL15.GL_ARRAY_BUFFER, destByteOffset, view);
    }

    private static void copyToShadow(FloatBuffer shadow, int shadowFloatOffset,
                                     FloatBuffer src, int srcFloatOffset, int floatCount) {
        if (floatCount <= 0) {
            return;
        }
        long srcAddr = MemoryUtil.memAddress(src) + (long) srcFloatOffset * 4L;
        long dstAddr = MemoryUtil.memAddress(shadow) + (long) shadowFloatOffset * 4L;
        MemoryUtil.memCopy(srcAddr, dstAddr, (long) floatCount * 4L);
    }
}
