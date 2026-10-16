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

    /** A/B JVM flag -Dhbm.stagingDirect=true forces the direct path (config is the primary source). */
    private static final boolean STAGING_DIRECT_FLAG = Boolean.getBoolean("hbm.stagingDirect");

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
        boolean stagingDirect = com.hbm_m.config.ModClothConfig.get().nucleusStagingDirect
                || STAGING_DIRECT_FLAG;
        PersistentUploadStaging staging = stagingDirect ? null : PersistentUploadStaging.getOrCreate();
        if (staging != null) {
            // Bind ring + destination ONCE for the whole window's span loop -
            // a per-span rebind of the same two buffers was 3.16 ms/frame in spark.
            staging.beginWindowCopy(destVbo);
        }
        try {
            int valid = Math.min(floatCount, Math.max(0, shadowValidFloats));
            // memAddress0 (not memAddress): these offsets are absolute float
            // indices, so the base must be position-independent or a non-flipped
            // src with a nonzero srcFloatOffset would be shifted twice.
            long srcBaseAddr = MemoryUtil.memAddress0(src) + (long) srcFloatOffset * 4L;
            long shdBaseAddr = MemoryUtil.memAddress0(shadow) + (long) shadowFloatOffset * 4L;
            // Coalesce changed runs within one instance record (gaps <= 8 floats,
            // e.g. scattered light words) but NOT across records: consecutive
            // animated machines update only their 4-float AnimParams tail, and the
            // 28 unchanged floats in between must NOT be bridged - a 64-float gap
            // glued the whole window into one giant span and DMA'd 8x the changed
            // bytes every frame. Raw memGetInt keeps the scan virtual-call-free.
            final int COALESCE_GAP_FLOATS = 8;
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
                    uploadDiffedSpan(src, srcFloatOffset + runStart, shadow, shadowFloatOffset + runStart,
                            runEnd - runStart, destFloatOffset + runStart, destVbo);
                    runStart = -1;
                }
            }
            if (runStart >= 0) {
                uploadDiffedSpan(src, srcFloatOffset + runStart, shadow, shadowFloatOffset + runStart,
                        runEnd - runStart, destFloatOffset + runStart, destVbo);
            }
            // Tail beyond the shadow's known-valid region: unconditional full span.
            if (valid < floatCount) {
                if (uploadSpan(src, srcFloatOffset + valid, floatCount - valid,
                        destFloatOffset + valid, destVbo)) {
                    copyToShadow(shadow, shadowFloatOffset + valid, src, srcFloatOffset + valid,
                            floatCount - valid);
                }
            }
        } finally {
            if (staging != null) {
                staging.endWindowCopy();
            }
        }
    }

    /** One diffed span: GPU write + shadow mirror (mirror skipped only when the GPU write failed). */
    private static void uploadDiffedSpan(FloatBuffer src, int srcFloatOffset,
                                         FloatBuffer shadow, int shadowFloatOffset,
                                         int floatCount, int destFloatOffset, int destVbo) {
        if (uploadSpan(src, srcFloatOffset, floatCount, destFloatOffset, destVbo)) {
            copyToShadow(shadow, shadowFloatOffset, src, srcFloatOffset, floatCount);
        }
    }

    /** @return true if the GPU write succeeded (or was a no-op). */
    private static boolean uploadSpan(FloatBuffer src, int srcFloatOffset, int floatCount,
                                   int destFloatOffset, int destVbo) {
        if (floatCount <= 0) {
            return true;
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
            return true;
        }
        // Fallback: direct upload from a client-memory buffer (may implicitly
        // synchronize the driver, but is correct). NO synchronous glGetError
        // polling here: an error drain + verify per span hard-freezes old Tier 2
        // drivers (glGetError forces a pipeline flush on some of them); the
        // persistent-mapped ring is the Tier-2-safe path, this fallback only has
        // to be sized correctly.
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, destVbo);
        try {
            FloatBuffer view = src.duplicate();
            view.position(srcFloatOffset);
            view.limit(srcFloatOffset + floatCount);
            GL15.glBufferSubData(GL15.GL_ARRAY_BUFFER, destByteOffset, view);
            return true;
        } finally {
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
        }
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
}
