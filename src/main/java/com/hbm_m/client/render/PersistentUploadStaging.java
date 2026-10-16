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
 * each frame {@link #endFrame()} places a fence over the written range, and a
 * range can be reused only after {@code glClientWaitSync} (short timeouts; if
 * the fence is not signaled, {@link #copyRegion} returns false and the caller
 * degrades to glBufferSubData).
 * <p>
 * {@link GL44#GL_MAP_COHERENT_BIT}: a CPU write is visible to a GPU copy issued
 * AFTER it from the same thread; flush calls are not needed.
 * <p>
 * Allocated per process (the GL context lives as long); F3+T does not destroy
 * the context, no recreation required.
 */
public final class PersistentUploadStaging {

    private static final int INITIAL_CAPACITY_BYTES = 4 * 1024 * 1024;

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
     * degraded to glBufferSubData. Short fences every 256 KiB yield a wait on
     * the exact oldest range (the fence signals as soon as the copies BEFORE it
     * finish - copies execute on the stream in order).
     */
    private static final long FENCE_GRANULARITY_BYTES = 256L * 1024L;
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
        capacityBytes = INITIAL_CAPACITY_BYTES;
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, bufferId);
        GL44.glBufferStorage(GL15.GL_ARRAY_BUFFER, capacityBytes,
                GL44.GL_MAP_WRITE_BIT | GL44.GL_MAP_PERSISTENT_BIT | GL44.GL_MAP_COHERENT_BIT);
        ByteBuffer mapped = GL30.glMapBufferRange(GL15.GL_ARRAY_BUFFER, 0, capacityBytes,
                GL30.GL_MAP_WRITE_BIT | GL44.GL_MAP_PERSISTENT_BIT | GL44.GL_MAP_COHERENT_BIT);
        if (mapped == null) {
            GL15.glDeleteBuffers(bufferId);
            bufferId = 0;
            return false;
        }
        baseAddr = MemoryUtil.memAddress(mapped);
        writePtr = 0;
        return true;
    }

    /**
     * Copies {@code floatCount} floats from {@code src} (at {@code srcFloatOffset})
     * into the target buffer at byte offset {@code destByteOffset} via staging.
     * @return false - the GPU has not released the ring yet; the caller must fall back.
     */
    public boolean copyRegion(FloatBuffer src, int srcFloatOffset, int floatCount, int destVbo, long destByteOffset) {
        long bytes = (long) floatCount * 4L;
        if (bytes <= 0) {
            return true;
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

        GL15.glBindBuffer(GL31.GL_COPY_READ_BUFFER, bufferId);
        GL15.glBindBuffer(GL31.GL_COPY_WRITE_BUFFER, destVbo);
        GL31.glCopyBufferSubData(GL31.GL_COPY_READ_BUFFER, GL31.GL_COPY_WRITE_BUFFER,
                writePtr, destByteOffset, bytes);
        writePtr += bytes;
        maybeFenceChunk();
        pendingAny = true;
        return true;
    }

    /**
     * Writes into the ring WITHOUT an immediate copy - for batching (compute
     * scatter): the caller accumulates several regions and transfers them with
     * a single {@link ComputeScatterUploader#scatter} dispatch.
     * @return the byte offset of the region in the ring, or -1 (fence not released /
     *         does not fit) - the caller must fall back to per-span uploads.
     */
    public long stageRegion(FloatBuffer src, int srcFloatOffset, int floatCount) {
        long bytes = (long) floatCount * 4L;
        if (bytes <= 0) {
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
        if (pendingAny && pendingStart < writePtr) {
            placeFence(pendingStart, writePtr);
        }
        pendingStart = writePtr;
        pendingAny = false;
    }

    private void placeFence(long start, long end) {
        long sync = GL32.glFenceSync(GL32.GL_SYNC_GPU_COMMANDS_COMPLETE, 0);
        if (sync != 0L) {
            fences.add(new FenceRange(sync, start, end));
        }
    }

    /** Intermediate fence of a run: FIFO of short ranges instead of one per frame. */
    private void maybeFenceChunk() {
        if (writePtr - pendingStart >= FENCE_GRANULARITY_BYTES) {
            placeFence(pendingStart, writePtr);
            pendingStart = writePtr;
        }
    }

    private boolean waitForRange(long start, long len) {
        long end = start + len;
        Iterator<FenceRange> it = fences.iterator();
        while (it.hasNext()) {
            FenceRange f = it.next();
            if (f.start >= end || start >= f.end) {
                continue;
            }
            int res = GL32.glClientWaitSync(f.sync, 0, 1_000_000L);
            int tries = 0;
            while (res == GL32.GL_TIMEOUT_EXPIRED && tries < 8) {
                res = GL32.glClientWaitSync(f.sync, 0, 2_000_000L);
                tries++;
            }
            if (res != GL32.GL_ALREADY_SIGNALED && res != GL32.GL_CONDITION_SATISFIED) {
                return false;
            }
            GL32.glDeleteSync(f.sync);
            it.remove();
        }
        return true;
    }
}
