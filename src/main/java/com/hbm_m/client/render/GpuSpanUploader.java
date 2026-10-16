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
 * Span-diff uploads of instance data (level-2 dirty detection).
 * <p>
 * The caller holds a CPU copy ("shadow") of the GPU buffer contents.
 * {@link #diffUpload} compares the new data window against the shadow copy and uploads
 * only the changed ranges: a static scene with world-space record coordinates
 * (FrameViewState) yields zero uploads, a pure part rotation a 16-byte span
 * instead of the full record (184 bytes at 46 floats, OrientedInstance-equivalent traffic).
 * <p>
 * Invariant: skipped spans must already be present in the GPU buffer -
 * orphaning (glBufferData) is FORBIDDEN on these paths; it destroys content
 * the shadow considers up to date.
 * <p>
 * Span delivery channel - {@link PersistentUploadStaging} (persistent-mapped
 * ring + glCopyBufferSubData); if unavailable - glBufferSubData.
 */
public final class GpuSpanUploader {

    /** Above this span count in a window, uploading the whole window is cheaper. */
    private static final int MAX_SPANS = 48;
    /** Unchanged gaps shorter than this (in floats) are glued to the neighboring span. */
    private static final int MERGE_GAP_FLOATS = 24;

    // Compute scatter thresholds (Phase 4): below - the DMA path glCopyBufferSubData,
    // above - one compute dispatch instead of N driver copies (not Intel, GL 4.3;
    // gates inside ComputeScatterUploader, kill-switch -Dhbm.gpuScatter=false,
    // threshold -Dhbm.gpuScatter.minSpans=N).
    /** Config nucleusGpuScatterMinSpans; -Dhbm.gpuScatter.minSpans=N overrides. */
    private static int scatterMinSpans() {
        Integer flag = Integer.getInteger("hbm.gpuScatter.minSpans");
        if (flag != null) {
            return Math.max(2, flag);
        }
        return Math.max(2, com.hbm_m.config.ModClothConfig.get().nucleusGpuScatterMinSpans);
    }
    private static final long SCATTER_MIN_BYTES = 4096L;

    /** A/B JVM flag -Dhbm.stagingDirect=true forces the direct path (config is the primary source). */
    private static final boolean STAGING_DIRECT_FLAG = Boolean.getBoolean("hbm.stagingDirect");

    private GpuSpanUploader() {}

    /**
     * Full window upload + shadow sync (first frame, buffer growth,
     * span-logic overflow).
     */
    public static void fullUpload(FloatBuffer shadow, int shadowFloatOffset,
                                  FloatBuffer src, int srcFloatOffset,
                                  int floatCount, int destVbo) {
        uploadSpan(src, srcFloatOffset, floatCount, shadowFloatOffset, destVbo);
        copyToShadow(shadow, shadowFloatOffset, src, srcFloatOffset, floatCount);
    }

    /**
     * Diffs the window {@code src[srcFloatOffset, +floatCount)} against
     * {@code shadow[shadowFloatOffset, +floatCount)} and uploads only the differences.
     * The shadow is updated once the upload lands. Buffers are direct (memAlloc*).
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
        if (n >= scatterMinSpans() && ComputeScatterUploader.isGloballyEnabled()
                && tryScatterBatch(shadow, src, srcFloatOffset, shadowFloatOffset, spans, n, destVbo)) {
            return;
        }
        for (int k = 0; k < n; k++) {
            int start = spans[k * 2];
            int end = spans[k * 2 + 1];
            uploadSpan(src, srcFloatOffset + start, end - start, shadowFloatOffset + start, destVbo);
            copyToShadow(shadow, shadowFloatOffset + start, src, srcFloatOffset + start, end - start);
        }
    }

    /**
     * Compute scatter batch (Phase 4): all spans of the window are written to the staging
     * ring ({@link PersistentUploadStaging#stageRegion}), then transferred
     * to the dest VBO in ONE compute dispatch instead of N {@code glCopyBufferSubData} calls.
     * The shadow is synced only after the dispatch is issued successfully.
     *
     * @return true if the batch was issued; false for any reason (no compute/Intel/
     *         ring busy/too few bytes) - the caller falls back to the per-span path.
     */
    private static boolean tryScatterBatch(FloatBuffer shadow, FloatBuffer src, int srcFloatOffset,
                                           int shadowFloatOffset, int[] spans, int spanCount, int destVbo) {
        PersistentUploadStaging staging = PersistentUploadStaging.getOrCreate();
        ComputeScatterUploader scatter = ComputeScatterUploader.getOrCreate();
        if (staging == null || scatter == null) {
            return false;
        }
        long totalBytes = 0;
        for (int k = 0; k < spanCount; k++) {
            totalBytes += (long) (spans[k * 2 + 1] - spans[k * 2]) * 4L;
        }
        if (totalBytes < SCATTER_MIN_BYTES) {
            return false;
        }
        long[] ops = new long[spanCount * 3];
        for (int k = 0; k < spanCount; k++) {
            int start = spans[k * 2];
            int end = spans[k * 2 + 1];
            int floatCount = end - start;
            long ringOffset = staging.stageRegion(src, srcFloatOffset + start, floatCount);
            if (ringOffset < 0) {
                return false;
            }
            ops[k * 3] = ringOffset >> 2;
            ops[k * 3 + 1] = shadowFloatOffset + start;
            ops[k * 3 + 2] = floatCount;
        }
        if (!scatter.scatter(staging.getBufferId(), destVbo, ops, spanCount)) {
            return false;
        }
        for (int k = 0; k < spanCount; k++) {
            int start = spans[k * 2];
            int end = spans[k * 2 + 1];
            copyToShadow(shadow, shadowFloatOffset + start, src, srcFloatOffset + start, end - start);
        }
        NucleusDebug.recordUpload(spanCount, totalBytes);
        return true;
    }

    private static void uploadSpan(FloatBuffer src, int srcFloatOffset, int floatCount,
                                   int destFloatOffset, int destVbo) {
        if (floatCount <= 0) {
            return;
        }
        NucleusDebug.recordUpload(1, (long) floatCount * 4L);
        long destByteOffset = (long) destFloatOffset * 4L;
        // -Dhbm.stagingDirect=true - A/B for UMA iGPUs: glCopyBufferSubData on
        // shared memory = an extra memcpy + sync; direct glBufferSubData is
        // sometimes faster. Default is the ring (safer for synchronization).
        boolean stagingDirect = com.hbm_m.config.ModClothConfig.get().nucleusStagingDirect
                || STAGING_DIRECT_FLAG;
        PersistentUploadStaging staging = stagingDirect ? null : PersistentUploadStaging.getOrCreate();
        if (staging != null
                && staging.copyRegion(src, srcFloatOffset, floatCount, destVbo, destByteOffset)) {
            return;
        }
        // Fallback: direct upload from a client-memory buffer (may implicitly
        // synchronize the driver, but is correct).
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
