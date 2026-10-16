package com.hbm_m.client.render;

//? if forge {
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
//?} elif neoforge {
/*import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
*///?}

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.ArrayDeque;
import java.util.Iterator;

import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL31;
import org.lwjgl.opengl.GL32;
import org.lwjgl.opengl.GL44;
import org.lwjgl.opengl.GLCapabilities;
import org.lwjgl.opengl.GL;
import org.lwjgl.system.MemoryUtil;

import com.hbm_m.main.MainRegistry;

@OnlyIn(Dist.CLIENT)
/**
 * Ring persistently-mapped staging buffer for instance-data uploads.
 * <p>
 * The CPU writes a span into mapped memory (a driver-free memcpy), then a
 * single {@code glCopyBufferSubData} transfers it into the target VBO.
 * The GPU-GPU copy is ordered in the command stream - there is no race with
 * the draw reading the target VBO; the only race is the CPU write against an
 * unfinished copy out of staging. It is closed by the ring + {@code glFenceSync}:
 * fences are placed over written ranges ({@link #endFrame} per frame plus
 * intermediate fences), and a range can be reused only after every overlapping
 * fence is signaled - checked with a NON-BLOCKING zero-timeout
 * {@code glClientWaitSync} poll ({@link #waitForRange}); if any overlapping
 * fence is not yet signaled, {@link #copyRegion} / {@link #stageRegion}
 * return failure immediately (no render-thread stall) and the caller degrades
 * to glBufferSubData.
 * <p>
 * {@link GL44#GL_MAP_COHERENT_BIT}: a CPU write is visible to a GPU copy issued
 * AFTER it from the same thread; flush calls are not needed.
 * <p>
 * Adapted from CrankShaft / Flywheel GlFence & StagingBuffer non-blocking FIFO fence management
 * (Apache-2.0 / MIT License, dev.engine_room.flywheel.backend.gl.GlFence).
 * <p>
 * Allocated per process (the GL context lives as long); F3+T does not destroy
 * the context, no recreation required.
 */
public final class PersistentUploadStaging {

    private static final int INITIAL_CAPACITY_BYTES = 32 * 1024 * 1024;

    private static PersistentUploadStaging instance;
    private static boolean resolved = false;

    private int bufferId = 0;
    private long capacityBytes = 0;
    private long baseAddr = 0;
    private long writePtr = 0;
    private boolean pendingAny = false;
    private ArrayDeque<FenceRange> fences = new ArrayDeque<>();

    /**
     * Granularity of intermediate fences (FIFO of short fences - a borrow-list
     * item of the nucleus_cpu_audit 0925, item 2). One fence per frame forced
     * a wait on a WRITTEN range to wait for the entire frame: on a weak GPU the
     * frame tail did not fit into the 17 ms of retries -> the ring stalled and
     * degraded to glBufferSubData. Short fences every 2 MiB yield a wait on
     * the exact oldest range (the fence signals as soon as the copies BEFORE it
     * finish - copies execute on the stream in order).
     */
    private static final long FENCE_GRANULARITY_BYTES = 2L * 1024L * 1024L;
    /** Start of the current unfenced write run. */
    private long pendingStart = 0;

    private static final class FenceRange {
        final long sync;
        final long start;
        final long end;

        FenceRange(long sync, long start, long end) {
            this.sync = sync;
            this.start = start;
            this.end = end;
        }
    }

    private PersistentUploadStaging() {}

    /** Current instance without lazy creation (for diagnostics; null = staging not in use). */
    public static PersistentUploadStaging peekOrNull() {
        return instance;
    }

    /** Null when GL44 persistent mapping is unavailable - callers fall back to glBufferSubData. */
    public static PersistentUploadStaging getOrCreate() {
        if (!HardwareTierManager.isPersistentStorageSupported()) {
            return null;
        }
        if (resolved) {
            return instance;
        }
        resolved = true;
        try {
            PersistentUploadStaging s = new PersistentUploadStaging();
            if (s.initialise()) {
                instance = s;
                MainRegistry.LOGGER.info("[HBM-M] PersistentUploadStaging ready ({} KiB ring)", INITIAL_CAPACITY_BYTES / 1024);
            }
        } catch (Throwable t) {
            MainRegistry.LOGGER.warn("[HBM-M] PersistentUploadStaging unavailable: {}", t.toString());
            instance = null;
        }
        return instance;
    }

    private boolean initialise() {
        GLCapabilities caps = GL.getCapabilities();
        if (caps.glBufferStorage == 0L || caps.glFenceSync == 0L
                || caps.glMapBufferRange == 0L || caps.glCopyBufferSubData == 0L) {
            return false;
        }
        bufferId = GL15.glGenBuffers();
        com.hbm_m.client.render.debug.GlDebugMarkers.labelBuffer(bufferId, "HBM_UploadStagingRing_VBO");
        capacityBytes = INITIAL_CAPACITY_BYTES;
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, bufferId);
        GL44.glBufferStorage(GL15.GL_ARRAY_BUFFER, capacityBytes,
                GL44.GL_MAP_WRITE_BIT | GL44.GL_MAP_PERSISTENT_BIT | GL44.GL_MAP_COHERENT_BIT);
        ByteBuffer mapped = GL30.glMapBufferRange(GL15.GL_ARRAY_BUFFER, 0, capacityBytes,
                GL30.GL_MAP_WRITE_BIT | GL44.GL_MAP_PERSISTENT_BIT | GL44.GL_MAP_COHERENT_BIT);
        if (mapped == null) {
            GL15.glDeleteBuffers(bufferId);
            bufferId = 0;
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
            return false;
        }
        baseAddr = MemoryUtil.memAddress(mapped);
        writePtr = 0;
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
        return true;
    }

    /**
     * Copies {@code floatCount} floats from {@code src} (at {@code srcFloatOffset})
     * into the target buffer at byte offset {@code destByteOffset} via staging.
     * @return false - the GPU has not released the ring yet; the caller must fall back.
     */
    /** Copy-target VBO currently bound by {@link #beginWindowCopy} (0 = none). */
    private int boundCopyWriteVbo = 0;

    /** Binds GL_COPY_READ_BUFFER and GL_COPY_WRITE_BUFFER once for a multi-span window diff. */
    public void beginWindowCopy(int destVbo) {
        if (bufferId > 0 && destVbo > 0 && !fenceCreationFailed) {
            GL15.glBindBuffer(GL31.GL_COPY_READ_BUFFER, bufferId);
            GL15.glBindBuffer(GL31.GL_COPY_WRITE_BUFFER, destVbo);
            boundCopyWriteVbo = destVbo;
        }
    }

    /** Unbinds copy buffers after a multi-span window diff. */
    public void endWindowCopy() {
        if (boundCopyWriteVbo != 0) {
            GL15.glBindBuffer(GL31.GL_COPY_READ_BUFFER, 0);
            GL15.glBindBuffer(GL31.GL_COPY_WRITE_BUFFER, 0);
            boundCopyWriteVbo = 0;
        }
    }

    public boolean copyRegion(FloatBuffer src, int srcFloatOffset, int floatCount, int destVbo, long destByteOffset) {
        long bytes = (long) floatCount * 4L;
        if (bytes < 0) {
            // Fail-closed: a negative count is caller drift - reporting success
            // would let the caller sync its shadow/snapshot against data that
            // was never written (the exact GPU-shadow divergence this class
            // guards against).
            return false;
        }
        if (bytes == 0) {
            return true; // no-op: nothing to write, nothing to sync
        }
        if (fenceCreationFailed) {
            return false;
        }
        if (bytes > capacityBytes) {
            return false;
        }
        if (writePtr + bytes > capacityBytes) {
            // Wrap-around: fence the tail of the ring, restart the run at zero - fence
            // ranges always have start <= end (otherwise waitForRange cannot match them).
            if (pendingAny && pendingStart < writePtr) {
                placeFence(pendingStart, writePtr);
            }
            writePtr = 0;
            pendingStart = 0;
            pendingAny = false;
        }
        if (!waitForRange(writePtr, bytes)) {
            return false;
        }
        FloatBuffer view = src.duplicate();
        view.position(srcFloatOffset);
        view.limit(srcFloatOffset + floatCount);
        MemoryUtil.memCopy(MemoryUtil.memAddress(view), baseAddr + writePtr, bytes);

        // Destination bounds are validated against the CACHED GL_BUFFER_SIZE of
        // destVbo instead of the old drain-glGetError-copy-glGetError trio: the
        // two synchronous error polls per span forced a driver sync on every
        // tiny copy (1.9 ms/frame in spark on the retained-farm scene), and the
        // drain-first pattern existed only to keep foreign error storms from
        // misattributing errors to this copy - with no polling at all there is
        // nothing to misattribute. The bounds check is the honest equivalent of
        // the old GL_INVALID_VALUE self-healing: a copy that would overflow the
        // destination fails CLOSED (caller falls back to glBufferSubData)
        // instead of silently succeeding and desyncing the CPU shadow.
        if (destByteOffset < 0) {
            return false;
        }
        long dstSize = (destVbo == lastDestVbo) ? lastDestSize : -1L;
        if (dstSize < 0 || destByteOffset + bytes > dstSize) {
            // First sight of this destination, or the copy would overflow the
            // cached size (the buffer may have grown since): re-probe once.
            GL15.glBindBuffer(GL31.GL_COPY_WRITE_BUFFER, destVbo);
            dstSize = GL15.glGetBufferParameteri(GL31.GL_COPY_WRITE_BUFFER, GL15.GL_BUFFER_SIZE);
            lastDestVbo = destVbo;
            lastDestSize = dstSize;
            // The probe left COPY_WRITE bound to THIS destVbo, which is exactly
            // what the copy below needs - no restore required.
            if (destByteOffset + bytes > dstSize) {
                if (!copyErrorLogged) {
                    copyErrorLogged = true;
                    com.hbm_m.main.MainRegistry.LOGGER.error(
                            "[HBM-M] PersistentUploadStaging: copy out of destination bounds - "
                                    + "writeOffset={} dstSize={} bytes={}; falling this upload to glBufferSubData",
                            destByteOffset, dstSize, bytes);
                }
                return false;
            }
        }

        // Rebind only when the target changed: a window diff issues dozens of
        // copyRegion calls for the SAME destVbo - rebinding per span was 3.16 ms/frame
        // of pure redundant binds in spark (animated 5000-assembler farm).
        if (boundCopyWriteVbo != destVbo) {
            GL15.glBindBuffer(GL31.GL_COPY_READ_BUFFER, bufferId);
            GL15.glBindBuffer(GL31.GL_COPY_WRITE_BUFFER, destVbo);
        }
        GL31.glCopyBufferSubData(GL31.GL_COPY_READ_BUFFER, GL31.GL_COPY_WRITE_BUFFER,
                writePtr, destByteOffset, bytes);
        writePtr += bytes;
        maybeFenceChunk();
        pendingAny = true;
        return true;
    }

    /** Cached GL_BUFFER_SIZE of the last copy destination (bounds check without glGetError polling). */
    private static int lastDestVbo = -1;
    private static long lastDestSize = -1L;

    /** One-shot flag for the glCopyBufferSubData failure diagnostic. */
    private static boolean copyErrorLogged = false;

    /**
     * Writes into the ring WITHOUT an immediate copy - for batching (compute
     * scatter): the caller accumulates several regions and transfers them with
     * a single {@link ComputeScatterUploader#scatter} dispatch.
     * @return the byte offset of the region in the ring, or -1 (fence not released /
     *         does not fit) - the caller must fall back to per-span uploads.
     */
    public long stageRegion(FloatBuffer src, int srcFloatOffset, int floatCount) {
        long bytes = (long) floatCount * 4L;
        if (bytes < 0) {
            return -1;
        }
        if (bytes == 0) {
            // No-op: contract parity with copyRegion - a zero-length span must
            // not abort a whole scatter batch (return the (unadvanced) pointer
            // as the region offset; the caller's op copies zero words).
            return writePtr;
        }
        if (fenceCreationFailed) {
            return -1;
        }
        if (bytes > capacityBytes) {
            return -1;
        }
        if (writePtr + bytes > capacityBytes) {
            if (pendingAny && pendingStart < writePtr) {
                placeFence(pendingStart, writePtr);
            }
            writePtr = 0;
            pendingStart = 0;
            pendingAny = false;
        }
        if (!waitForRange(writePtr, bytes)) {
            return -1;
        }
        FloatBuffer view = src.duplicate();
        view.position(srcFloatOffset);
        view.limit(srcFloatOffset + floatCount);
        MemoryUtil.memCopy(MemoryUtil.memAddress(view), baseAddr + writePtr, bytes);
        long offset = writePtr;
        writePtr += bytes;
        maybeFenceChunk();
        pendingAny = true;
        return offset;
    }

    /**
     * Immutable ring-state snapshot for multi-region batch rollback (the compute
     * scatter batch in {@link GpuSpanUploader}): capturing writePtr alone is
     * not enough because {@link #stageRegion} may wrap mid-batch, restarting
     * the pointer at zero (after which a plain pointer rewind is a silent no-op).
     */
    public record BatchMark(long writePtr, long pendingStart, boolean pendingAny) {}

    /** Snapshots the ring state before a staging batch; restore via {@link #abortBatch}. */
    public BatchMark markBatch() {
        return new BatchMark(writePtr, pendingStart, pendingAny);
    }

    /**
     * Restores the full ring state captured by {@link #markBatch} on batch abort.
     * Fences placed during the aborted batch deliberately remain: they fence
     * genuinely written bytes and signal normally (GL_SYNC_GPU_COMMANDS_COMPLETE),
     * and their ranges are reused only after they signal (waitForRange).
     */
    public void abortBatch(BatchMark mark) {
        writePtr = mark.writePtr();
        pendingStart = mark.pendingStart();
        pendingAny = mark.pendingAny();
    }

    /**
     * Fences every byte staged since {@code mark} - MUST be called after the
     * consuming command (the compute scatter dispatch) has been ENQUEUED, not
     * before. GL_SYNC_GPU_COMMANDS_COMPLETE covers only commands issued before
     * the fence, and the fences placed while staging ({@link #stageRegion} ->
     * {@link #maybeFenceChunk} / wrap) precede the dispatch in the command
     * stream: without this post-dispatch fence, ring reuse gated on those
     * pre-dispatch fences could overwrite the copy SOURCE while the dispatch
     * is still reading it. A fence enqueued after the dispatch signals only
     * once the dispatch completed (in-order stream), so any later
     * {@link #waitForRange} at reuse blocks correctly. Overlapping fences are
     * safe: waitForRange requires EVERY overlapping fence to be signaled.
     */
    public void fenceBatch(BatchMark mark) {
        long start = mark.writePtr();
        if (writePtr >= start) {
            // Contiguous batch: one fence over [mark.writePtr, writePtr).
            if (writePtr > start) {
                placeFence(start, writePtr);
            }
        } else {
            // Wrapped batch: two segments [start, capacity) + [0, writePtr)
            // (fence ranges always keep start <= end for waitForRange).
            placeFence(start, capacityBytes);
            if (writePtr > 0) {
                placeFence(0, writePtr);
            }
        }
    }

    /** Returns current write pointer for batch rollback. */
    public long getWritePtr() {
        return writePtr;
    }

    /** GL id of the ring (SSBO source for compute scatter; 0 = unavailable). */
    public int getBufferId() {
        return bufferId;
    }

    /** Ring capacity in bytes (0 if staging is unavailable). Used for the F3 VRAM metric. */
    public long getCapacityBytes() {
        return capacityBytes;
    }

    /** Fences everything written since the last call. Runs at the end of each frame (present). */
    public void endFrame() {
        reclaimSignaledFences();
        if (pendingAny && pendingStart < writePtr) {
            placeFence(pendingStart, writePtr);
        }
        pendingStart = writePtr;
        pendingAny = false;
    }

    /** One-shot flag for the glFenceSync failure diagnostic. */
    private static boolean fenceFailLogged = false;
    /**
     * Fail-closed kill-switch: once fence creation fails, ring ranges can no
     * longer be safely reused (waitForRange would see no fence and allow
     * immediate reuse while a GPU copy/dispatch may still read the range), so
     * all staging is refused and callers degrade to glBufferSubData.
     */
    private boolean fenceCreationFailed = false;

    private void placeFence(long start, long end) {
        long sync = GL32.glFenceSync(GL32.GL_SYNC_GPU_COMMANDS_COMPLETE, 0);
        if (sync != 0L) {
            fences.addLast(new FenceRange(sync, start, end));
        } else {
            // glFenceSync returned 0: silently dropping the fence would make the
            // just-written range immediately reusable while GPU work may still
            // read it. Fail closed - refuse all further staging.
            fenceCreationFailed = true;
            if (!fenceFailLogged) {
                fenceFailLogged = true;
                MainRegistry.LOGGER.error(
                        "[HBM-M] PersistentUploadStaging: glFenceSync failed - disabling the staging ring "
                                + "(all uploads degrade to glBufferSubData)");
            }
        }
    }

    /** Intermediate fence of a run: FIFO of short ranges instead of one per frame. */
    private void maybeFenceChunk() {
        if (writePtr - pendingStart >= FENCE_GRANULARITY_BYTES) {
            placeFence(pendingStart, writePtr);
            pendingStart = writePtr;
        }
    }

    /**
     * Non-blocking poll of a GL sync object with zero timeout.
     */
    private static boolean isSignaled(long sync) {
        int res = GL32.glClientWaitSync(sync, 0, 0L);
        return res == GL32.GL_ALREADY_SIGNALED || res == GL32.GL_CONDITION_SATISFIED;
    }

    /**
     * Pops and deletes signaled fences strictly from the FIFO queue head.
     * Since GPU commands execute in order, when the head is unsignaled,
     * all subsequent fences in the queue are also unsignaled.
     */
    private void reclaimSignaledFences() {
        while (!fences.isEmpty()) {
            FenceRange head = fences.peekFirst();
            if (isSignaled(head.sync)) {
                GL32.glDeleteSync(head.sync);
                fences.pollFirst();
            } else {
                break;
            }
        }
    }

    private boolean waitForRange(long start, long len) {
        reclaimSignaledFences();
        if (fences.isEmpty()) {
            return true;
        }
        long end = start + len;
        Iterator<FenceRange> it = fences.iterator();
        while (it.hasNext()) {
            FenceRange f = it.next();
            if (f.start >= end || start >= f.end) {
                continue;
            }
            // Non-blocking: if the GPU has not finished reading from this range,
            // do NOT block the render thread for 17 ms. Return false immediately
            // so the caller falls back to glBufferSubData without a frame drop.
            if (!isSignaled(f.sync)) {
                return false;
            }
            GL32.glDeleteSync(f.sync);
            it.remove();
        }
        return true;
    }
}
