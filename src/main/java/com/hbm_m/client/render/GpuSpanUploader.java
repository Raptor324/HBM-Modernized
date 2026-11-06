package com.hbm_m.client.render;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
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
 * Span delivery channel: direct glBufferSubData with single-bind window batching.
 */
public final class GpuSpanUploader {

    private GpuSpanUploader() {}

    /**
     * Full window upload + optional shadow sync.
     */
    public static void fullUpload(FloatBuffer shadow, int shadowFloatOffset,
                                  FloatBuffer src, int srcFloatOffset,
                                  int floatCount, int destVbo) {
        if (uploadSpan(src, srcFloatOffset, floatCount, shadowFloatOffset, destVbo)) {
            if (shadow != null) {
                copyToShadow(shadow, shadowFloatOffset, src, srcFloatOffset, floatCount);
            }
        }
    }

    /**
     * Span-diff window upload: compares {@code src} against the caller-held
     * {@code shadow} (the CPU mirror of the GPU window contents) and uploads
     * only the changed contiguous sub-spans; each uploaded span is mirrored
     * back into the shadow, so it stays a valid diff base for the next call.
     * Words beyond {@code shadowValidFloats} (e.g. records appended since the
     * shadow was last synced) are treated as changed and uploaded in full.
     * A null/undersized shadow degrades to {@link #fullUpload} (whole window,
     * shadow untouched) - the caller is then responsible for re-syncing it.
     * <p>
     * Invariant: skipped spans must already be present in the GPU buffer - a
     * shadow whose validity is uncertain (upload failure, window re-placement)
     * must NOT be passed here; the coordinator only transfers shadows from
     * records whose previous upload succeeded.
     *
     * @param shadowValidFloats how many words of {@code shadow} (from
     *                          {@code shadowFloatOffset}) are known to mirror
     *                          the GPU contents; may be less than floatCount.
     */
    public static void diffUpload(FloatBuffer shadow, int shadowFloatOffset, int shadowValidFloats,
                                  FloatBuffer src, int srcFloatOffset,
                                  int destFloatOffset, int floatCount, int destVbo) {
        if (floatCount <= 0) {
            return;
        }
        if (shadow == null
                || shadowFloatOffset < 0
                || shadow.capacity() < shadowFloatOffset + floatCount) {
            // Degraded path: upload the whole window at its REAL destination
            // (destFloatOffset - NOT shadowFloatOffset; fullUpload reuses its
            // shadow-offset parameter as the destination). Passing the shadow
            // offset (0 for per-window shadows) stacked every new/resized
            // window on top of slot 0 of the atlas instance VBO, overwrote the
            // window living there and left all other windows full of
            // uninitialized VRAM garbage. Sync the shadow back when it can
            // hold the window so the next call can still span-diff.
            if (uploadSpan(src, srcFloatOffset, floatCount, destFloatOffset, destVbo)) {
                if (shadow != null && shadowFloatOffset >= 0 && shadow.capacity() >= shadowFloatOffset + floatCount) {
                    copyToShadow(shadow, shadowFloatOffset, src, srcFloatOffset, floatCount);
                }
            }
            return;
        }
        int valid = Math.min(floatCount, Math.max(0, shadowValidFloats));
        long srcBaseAddr = MemoryUtil.memAddress0(src) + (long) srcFloatOffset * 4L;
        long shdBaseAddr = MemoryUtil.memAddress0(shadow) + (long) shadowFloatOffset * 4L;

        final int COALESCE_GAP_FLOATS = 8;
        int spanCount = 0;
        int runStart = -1;
        int runEnd = -1;
        for (int i = 0; i < valid; i++) {
            long byteOff = (long) i * 4L;
            boolean changed = MemoryUtil.memGetInt(srcBaseAddr + byteOff) != MemoryUtil.memGetInt(shdBaseAddr + byteOff);
            if (changed) {
                if (runStart < 0) {
                    runStart = i;
                }
                runEnd = i + 1;
            } else if (runStart >= 0 && (i - runEnd) >= COALESCE_GAP_FLOATS) {
                spanCount = recordSpan(spanCount, runStart, runEnd - runStart);
                runStart = -1;
            }
        }
        if (runStart >= 0) {
            spanCount = recordSpan(spanCount, runStart, runEnd - runStart);
        }
        if (valid < floatCount) {
            spanCount = recordSpan(spanCount, valid, floatCount - valid);
        }
        if (spanCount == 0) {
            return;
        }

        PersistentUploadStaging staging = PersistentUploadStaging.getOrCreate();
        if (spanCount >= SCATTER_MIN_SPANS && staging != null
                && tryScatterUpload(staging, src, srcFloatOffset, shadow, shadowFloatOffset,
                        destFloatOffset, destVbo, spanCount)) {
            return;
        }

        boolean arrayBound = false;
        if (staging != null) {
            staging.beginWindowCopy(destVbo);
        } else {
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, destVbo);
            arrayBound = true;
        }
        try {
            for (int s = 0; s < spanCount; s++) {
                int rStart = spanStarts[s];
                int rLen = spanLengths[s];
                arrayBound = uploadDiffedSpan(staging, destVbo, arrayBound,
                        src, srcFloatOffset + rStart, shadow, shadowFloatOffset + rStart,
                        rLen, destFloatOffset + rStart);
            }
        } finally {
            if (staging != null) {
                staging.endWindowCopy();
            }
            if (arrayBound) {
                GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
            }
        }
    }

    private static boolean uploadDiffedSpan(PersistentUploadStaging staging, int destVbo, boolean arrayBound,
                                            FloatBuffer src, int srcFloatOffset,
                                            FloatBuffer shadow, int shadowFloatOffset,
                                            int floatCount, int destFloatOffset) {
        NucleusDebug.recordUpload(1, (long) floatCount * 4L);
        long destByteOffset = (long) destFloatOffset * 4L;
        if (staging == null || !staging.copyRegion(src, srcFloatOffset, floatCount, destVbo, destByteOffset)) {
            if (!arrayBound) {
                GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, destVbo);
                arrayBound = true;
            }
            uploadSpanBound(src, srcFloatOffset, floatCount, destByteOffset);
        }
        copyToShadow(shadow, shadowFloatOffset, src, srcFloatOffset, floatCount);
        return arrayBound;
    }

    /** @return true if the GPU write succeeded (or was a no-op). */
    private static boolean uploadSpan(FloatBuffer src, int srcFloatOffset, int floatCount,
                                      int destFloatOffset, int destVbo) {
        if (floatCount <= 0) {
            return true;
        }
        NucleusDebug.recordUpload(1, (long) floatCount * 4L);
        long destByteOffset = (long) destFloatOffset * 4L;
        PersistentUploadStaging staging = PersistentUploadStaging.getOrCreate();
        if (staging != null && staging.copyRegion(src, srcFloatOffset, floatCount, destVbo, destByteOffset)) {
            return true;
        }
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, destVbo);
        try {
            uploadSpanBound(src, srcFloatOffset, floatCount, destByteOffset);
            return true;
        } finally {
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
        }
    }

    private static void uploadSpanBound(FloatBuffer src, int srcFloatOffset, int floatCount, long destByteOffset) {
        FloatBuffer view = src.duplicate();
        view.position(srcFloatOffset);
        view.limit(srcFloatOffset + floatCount);
        GL15.glBufferSubData(GL15.GL_ARRAY_BUFFER, destByteOffset, view);
    }

    /**
     * Copies {@code floatCount} words from {@code src} into the caller-held
     * {@code shadow}: seeds/repairs the CPU mirror of a GPU window after a full
     * upload (see MdiBatchCoordinator.uploadWindowsToAtlas).
     */
    public static void syncShadow(FloatBuffer shadow, int shadowFloatOffset,
                                  FloatBuffer src, int srcFloatOffset, int floatCount) {
        copyToShadow(shadow, shadowFloatOffset, src, srcFloatOffset, floatCount);
    }

    private static void copyToShadow(FloatBuffer shadow, int shadowFloatOffset,
                                     FloatBuffer src, int srcFloatOffset, int floatCount) {
        if (shadow == null || floatCount <= 0) {
            return;
        }
        // memAddress0: offsets are absolute float indices; see diffUpload.
        long srcAddr = MemoryUtil.memAddress0(src) + (long) srcFloatOffset * 4L;
        long dstAddr = MemoryUtil.memAddress0(shadow) + (long) shadowFloatOffset * 4L;
        MemoryUtil.memCopy(srcAddr, dstAddr, (long) floatCount * 4L);
    }

    private static final int SCATTER_MIN_SPANS = 16;
    private static int[] spanStarts = new int[128];
    private static int[] spanLengths = new int[128];
    private static final long[] scatterOps = new long[ComputeScatterUploader.maxOpsPerDispatch() * 3];

    private static int recordSpan(int idx, int start, int len) {
        if (idx >= spanStarts.length) {
            int newCap = spanStarts.length * 2;
            spanStarts = java.util.Arrays.copyOf(spanStarts, newCap);
            spanLengths = java.util.Arrays.copyOf(spanLengths, newCap);
        }
        spanStarts[idx] = start;
        spanLengths[idx] = len;
        return idx + 1;
    }

    private static boolean tryScatterUpload(PersistentUploadStaging staging,
                                            FloatBuffer src, int srcFloatOffset,
                                            FloatBuffer shadow, int shadowFloatOffset,
                                            int destFloatOffset, int destVbo, int spanCount) {
        ComputeScatterUploader scatter = ComputeScatterUploader.getOrCreate();
        if (scatter == null || staging.getBufferId() <= 0) {
            return false;
        }
        int maxOps = ComputeScatterUploader.maxOpsPerDispatch();
        int previousProgram = org.lwjgl.opengl.GL11.glGetInteger(org.lwjgl.opengl.GL20.GL_CURRENT_PROGRAM);
        PersistentUploadStaging.BatchMark batchMark = staging.markBatch();
        long totalBytes = 0L;

        for (int base = 0; base < spanCount; base += maxOps) {
            int chunkOps = Math.min(maxOps, spanCount - base);
            for (int k = 0; k < chunkOps; k++) {
                int s = base + k;
                int rStart = spanStarts[s];
                int rLen = spanLengths[s];
                long byteOff = staging.stageRegion(src, srcFloatOffset + rStart, rLen);
                if (byteOff < 0 || (byteOff & 3L) != 0L) {
                    staging.abortBatch(batchMark);
                    return false;
                }
                scatterOps[k * 3] = byteOff >>> 2;
                scatterOps[k * 3 + 1] = (long) (destFloatOffset + rStart);
                scatterOps[k * 3 + 2] = (long) rLen;
                totalBytes += (long) rLen * 4L;
            }
            if (!scatter.scatter(staging.getBufferId(), destVbo, scatterOps, chunkOps, previousProgram)) {
                staging.abortBatch(batchMark);
                return false;
            }
        }

        org.lwjgl.opengl.GL42.glMemoryBarrier(org.lwjgl.opengl.GL42.GL_VERTEX_ATTRIB_ARRAY_BARRIER_BIT
                | org.lwjgl.opengl.GL43.GL_SHADER_STORAGE_BARRIER_BIT);
        com.hbm_m.client.render.shader.ShaderBindResync.markForeignProgramBind();
        staging.fenceBatch(batchMark);

        for (int s = 0; s < spanCount; s++) {
            int rStart = spanStarts[s];
            int rLen = spanLengths[s];
            copyToShadow(shadow, shadowFloatOffset + rStart, src, srcFloatOffset + rStart, rLen);
        }
        NucleusDebug.recordUpload(spanCount, totalBytes);
        return true;
    }
}
