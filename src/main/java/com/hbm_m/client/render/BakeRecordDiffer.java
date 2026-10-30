package com.hbm_m.client.render;

import com.hbm_m.client.render.debug.GlDebugMarkers;
import com.hbm_m.main.MainRegistry;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL43;
import org.lwjgl.system.MemoryUtil;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.HashSet;
import java.util.Set;

/**
 * Instance record differential analysis and GPU SSBO upload streaming.
 * Handles IEEE-754 sign canonicalization, shadow mantissa quantization,
 * NaN poison guards, contiguous dirty span coalescing, and dirty-index lists.
 */
public final class BakeRecordDiffer {

    private BakeRecordDiffer() {}

    /**
     * Normalizes -0.0f to +0.0f so bitwise comparisons never falsely dirty a record
     * across math libraries that differ on IEEE-754 signed zeros.
     */
    public static float canonicalize(float v) {
        return Float.floatToRawIntBits(v) == 0x80000000 ? 0.0f : v;
    }

    /**
     * SHADOW comparison quantization: raw-bits mask of the LOW 3 MANTISSA BITS
     * (rawBits &amp; ~0x7), applied to the pos words (0..2) and the packed quat words (3..4)
     * ONLY, on BOTH sides of the comparison. Swallows ULP jitter in low mantissa bits.
     */
    public static int quantizeCmpBits(int rawBits) {
        return rawBits & ~0x7;
    }

    /**
     * Last record word covered by shadow quantization: pos 0..2 + packed snorm16 quat words 3..4.
     */
    public static final int SHADOW_QUANT_LAST_WORD = 4;

    /** Scratch for record comparison — grown lazily. */
    private static float[] SCRATCH_CMP = new float[InstancedStaticPartRenderer.INSTANCE_DATA_SIZE * 4096];

    /** Scratch for dirty-list upload ([count, idx...]) — grown lazily. */
    private static IntBuffer SCRATCH_DIRTY_LIST = null;

    /** One invariant-dump per mode cache for out-of-range dirty indices (prevents log spam). */
    private static final Set<Integer> DIRTY_RANGE_DUMP = new HashSet<>();

    /**
     * Per-record diff of the live records against the cached snapshot, filling
     * {@code mc.dirtyIdx[0..dirtyCount)} with the changed instance indices.
     * Returns the dirty count. MAIN records: exact raw-bits equality after -0.0f
     * canonicalization on both sides. SHADOW records: pos/quat words additionally
     * quantized on both sides (see {@link #quantizeCmpBits}); all other words
     * exact. Bitwise-equal words short-circuit before any canonicalization.
     */
    public static int diffRecords(ModeCache mc, FloatBuffer records, int count, int mode) {
        int stride = InstancedStaticPartRenderer.INSTANCE_DATA_SIZE;
        int floats = count * stride;
        if (mc.recordCopy == null || mc.recordCopy.length < floats) {
            return count; // no usable snapshot: full re-bake
        }
        if (mc.dirtyIdx == null || mc.dirtyIdx.length < count) {
            mc.dirtyIdx = new int[count];
        }
        boolean isDirect = records.isDirect();
        long baseAddr = isDirect ? MemoryUtil.memAddress(records) : 0L;
        if (!isDirect) {
            if (SCRATCH_CMP.length < floats) {
                SCRATCH_CMP = new float[floats];
            }
            int pos = records.position();
            records.get(SCRATCH_CMP, 0, floats);
            records.position(pos);
        }
        boolean quant = mode == NucleusGpuBaker.MODE_SHADOW;
        float[] snap = mc.recordCopy;
        int[] dirty = mc.dirtyIdx;
        int n = 0;
        int o = 0;
        final int gradWWord = InstancedStaticPartRenderer.GRAD_FLOAT_OFFSET + 3; // word 27
        final int animYWord = InstancedStaticPartRenderer.ANIM_FLOAT_OFFSET + 1; // word 29
        for (int r = 0; r < count; r++, o += stride) {
            boolean rowDirty = false;
            for (int w = 0; w < stride; w++) {
                int rawA = isDirect
                        ? MemoryUtil.memGetInt(baseAddr + (long) (o + w) * 4L)
                        : Float.floatToRawIntBits(SCRATCH_CMP[o + w]);
                int rawB = Float.floatToRawIntBits(snap[o + w]);
                if (rawA == rawB) {
                    // NaN poison guard: raw-bits equality can hold across two
                    // NaN-poisoned words forever. Any non-finite tint/grad word
                    // forces the record dirty so a poisoned record can never be
                    // cached instead of re-uploaded.
                    if ((rawA & 0x7f800000) != 0x7f800000) {
                        continue;
                    }
                    rowDirty = true;
                    break;
                }
                int ia = (rawA == 0x80000000) ? 0 : rawA;
                int ib = (rawB == 0x80000000) ? 0 : rawB;
                if (quant && w <= SHADOW_QUANT_LAST_WORD) {
                    ia = quantizeCmpBits(ia);
                    ib = quantizeCmpBits(ib);
                }
                if (ia != ib) {
                    rowDirty = true;
                    break;
                }
            }
            if (!rowDirty) {
                // Check if this instance uses a time-tracked GPU joint chain that is
                // actively moving (duration halfword in high 16 bits of word 29 != 0).
                float gradW = isDirect
                        ? MemoryUtil.memGetFloat(baseAddr + (long) (o + gradWWord) * 4L)
                        : SCRATCH_CMP[o + gradWWord];
                if (gradW >= 0.0f && NucleusJointSpecs.isTrackedChain((int) gradW)) {
                    int rawAnimY = isDirect
                            ? MemoryUtil.memGetInt(baseAddr + (long) (o + animYWord) * 4L)
                            : Float.floatToRawIntBits(SCRATCH_CMP[o + animYWord]);
                    // High 16 bits of AnimParams.y hold binary16 durationTicks; non-zero magnitude means moving
                    if ((rawAnimY & 0x7FFF0000) != 0) {
                        rowDirty = true;
                    }
                }
            }
            if (rowDirty) {
                dirty[n++] = r;
            }
        }
        mc.dirtyCount = n;
        return n;
    }

    /**
     * Grows the per-cache record SSBO capacity under VRAM budget control.
     */
    public static boolean ensureCacheRecords(ModeCache mc, long bytes) {
        if (bytes > mc.recordSize[0]) {
            long newBytes = Math.max(bytes, Math.max(mc.recordSize[0], 0) * 2);
            long delta = newBytes - Math.max(mc.recordSize[0], 0);
            synchronized (BakeVramPoolManager.class) {
                if (BakeVramPoolManager.getCacheBytes() + delta > BakeVramPoolManager.cacheBudgetBytes()) {
                    BakeVramPoolManager.evictStaleCaches();
                }
                BakeVramPoolManager.adjustCacheBytes(delta);
            }
            GL15.glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, mc.recordSsbo);
            GL15.glBufferData(GL43.GL_SHADER_STORAGE_BUFFER, newBytes, GL15.GL_DYNAMIC_COPY);
            mc.recordSize[0] = newBytes;
        }
        return true;
    }

    /**
     * Uploads {@code count} instance records to the destination SSBO.
     * The destination storage must be pre-sized (ensureCacheRecords).
     */
    public static boolean uploadRecords(FloatBuffer records, int count, int destSsbo) {
        int floats = count * InstancedStaticPartRenderer.INSTANCE_DATA_SIZE;
        if (records == null || records.remaining() < floats) {
            return false;
        }
        int pos = records.position();
        int lim = records.limit();
        GL15.glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, destSsbo);
        try {
            records.limit(pos + floats);
            GL15.glBufferSubData(GL43.GL_SHADER_STORAGE_BUFFER, 0L, records);
        } finally {
            records.limit(lim).position(pos);
            GL15.glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, 0);
        }
        return true;
    }

    /**
     * Span-level record upload: only the dirty instance records reach the GPU, as
     * ONE span per contiguous dirty run (records are 32-float aligned, so a run of
     * {@code n} instances is {@code n * 32} floats at byte offset
     * {@code startIndex * 32 * 4}). Consecutive dirty indices separated by &lt;= 64
     * slots coalesce into runs. If &gt; 4 spans remain, collapse into a single bounding span.
     */
    public static boolean uploadDirtyRecords(FloatBuffer records, int count, ModeCache mc,
                                            int dirtyCount) {
        int stride = InstancedStaticPartRenderer.INSTANCE_DATA_SIZE;
        if (records == null || records.remaining() < count * stride
                || mc.dirtyIdx == null || mc.dirtyIdx.length < dirtyCount) {
            return false;
        }
        for (int i = 0; i < dirtyCount; i++) {
            int idx = mc.dirtyIdx[i];
            if (idx < 0 || idx >= count) {
                if (DIRTY_RANGE_DUMP.add(System.identityHashCode(mc))) {
                    MainRegistry.LOGGER.error(
                            "[HBM-M] NucleusGpuBaker: out-of-range dirty index (modeCache@{}): dirty[{}]={}, "
                                    + "count={}, recordCount={}, stride={}, buffer pos={} lim={} cap={}"
                                    + " - invalidating the diff snapshot (full re-bake next call), bake stays enabled",
                            System.identityHashCode(mc), i, idx, count, mc.recordCount, stride,
                            records.position(), records.limit(), records.capacity());
                }
                mc.recordCopy = null;
                mc.recordCount = -1;
                mc.outputValid = false;
                return false;
            }
        }
        int spanCount = 0;
        int[] spanStart = new int[4];
        int[] spanEnd = new int[4];
        int i = 0;
        while (i < dirtyCount) {
            int rStart = mc.dirtyIdx[i];
            int j = i;
            while (j + 1 < dirtyCount && mc.dirtyIdx[j + 1] - mc.dirtyIdx[j] <= 64) {
                j++;
            }
            int rEnd = mc.dirtyIdx[j];
            if (spanCount < 4) {
                spanStart[spanCount] = rStart;
                spanEnd[spanCount] = rEnd;
                spanCount++;
            } else {
                spanStart[0] = mc.dirtyIdx[0];
                spanEnd[0] = mc.dirtyIdx[dirtyCount - 1];
                spanCount = 1;
                break;
            }
            i = j + 1;
        }

        int base = records.position();
        int limit = records.limit();
        GL15.glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, mc.recordSsbo);
        try {
            for (int s = 0; s < spanCount; s++) {
                int rStart = spanStart[s];
                int rEnd = spanEnd[s];
                int from = base + rStart * stride;
                int floatLen = (rEnd - rStart + 1) * stride;
                records.position(base).limit(limit);
                records.position(from).limit(from + floatLen);
                GL15.glBufferSubData(GL43.GL_SHADER_STORAGE_BUFFER,
                        (long) rStart * stride * 4L, records);
            }
        } finally {
            records.position(base).limit(limit);
            GL15.glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, 0);
        }
        return true;
    }

    /** Grows the per-cache dirty-index SSBO (instance capacity + count word, budget-accounted). */
    public static boolean ensureDirtyBuffer(ModeCache mc, int count) {
        long bytes = (long) (1 + count) * 4L;
        if (bytes > mc.dirtyBytes) {
            long newBytes = Math.max(bytes, Math.max(mc.dirtyBytes, 0) * 2);
            long delta = newBytes - Math.max(mc.dirtyBytes, 0);
            synchronized (BakeVramPoolManager.class) {
                if (BakeVramPoolManager.getCacheBytes() + delta > BakeVramPoolManager.cacheBudgetBytes()) {
                    BakeVramPoolManager.evictStaleCaches();
                }
            }
            if (mc.dirtySsbo == -1) {
                mc.dirtySsbo = GL15.glGenBuffers();
                GlDebugMarkers.labelBuffer(mc.dirtySsbo, "HBM_Dirty_Grow_SSBO");
            }
            GL15.glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, mc.dirtySsbo);
            GL15.glBufferData(GL43.GL_SHADER_STORAGE_BUFFER, newBytes, GL15.GL_DYNAMIC_COPY);
            GL15.glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, 0);
            synchronized (BakeVramPoolManager.class) {
                BakeVramPoolManager.adjustCacheBytes(delta);
            }
            mc.dirtyBytes = newBytes;
        } else if (mc.dirtySsbo == -1) {
            return false;
        }
        return true;
    }

    /** Uploads the CPU-side dirty list ([dirtyCount, instance indices...]) into the SSBO. */
    public static void uploadDirtyList(ModeCache mc, int dirtyCount) {
        int words = 1 + dirtyCount;
        if (SCRATCH_DIRTY_LIST == null || SCRATCH_DIRTY_LIST.capacity() < words) {
            SCRATCH_DIRTY_LIST = BufferUtils
                    .createIntBuffer(1 + Math.max(mc.dirtyIdx != null ? mc.dirtyIdx.length : 0, dirtyCount));
        }
        SCRATCH_DIRTY_LIST.clear();
        SCRATCH_DIRTY_LIST.put(dirtyCount);
        SCRATCH_DIRTY_LIST.put(mc.dirtyIdx, 0, dirtyCount);
        SCRATCH_DIRTY_LIST.flip();
        GL15.glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, mc.dirtySsbo);
        GL15.glBufferSubData(GL43.GL_SHADER_STORAGE_BUFFER, 0L, SCRATCH_DIRTY_LIST);
        GL15.glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, 0);
    }
}
