package com.hbm_m.client.render.retained.upload;

import com.hbm_m.client.render.machine.MachineSpec;
import com.hbm_m.client.render.retained.MachineInstanceHandle;
import com.hbm_m.client.render.retained.alloc.InstanceSlotAllocator;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Batches, coalesces, and uploads sparse instance slot updates.
 *
 * <p><b>Upload path retired.</b> The retained graph's global backing buffer was
 * dead VRAM: nothing ever bound or drew it - real rendering streams through
 * {@code spec.syncInstanceRecord} -> {@code InstancedStaticPartRenderer} ->
 * {@code MdiBatchCoordinator} -> {@code MdiGeometryAtlas}. The 2 MB off-heap
 * staging buffer, the dirty-op arrays and the GL upload paths are gone; the
 * enqueue* methods are API stubs. What remains alive here is the RECORD
 * BOOKKEEPING: {@code enqueueInitialBake} / {@code enqueueUpdate} still sample
 * light, pose parts and push records into the per-machine CPU-side copies via
 * {@code MachineSpec.syncInstanceRecord} / {@code syncInstanceSlice}, which is
 * what {@code MachineBer.recordMatchesBuffer} compares against.
 *
 * @credit CrankShaft / fewizz / Jdb100
 */
public final class SparseUpdateQueue {

    private final InstanceSlotAllocator allocator;
    private int pendingCount = 0;

    public SparseUpdateQueue(InstanceSlotAllocator allocator) {
        this.allocator = allocator;
    }

    public synchronized void enqueueSlot(int slotIndex, long sourceData128Bytes) {
    }

    public synchronized void enqueueSlotFloats(int slotIndex, float[] floats32) {
    }

    public synchronized void enqueueSlotSlice(int slotIndex, int floatOffset, float[] data) {
    }

    public synchronized void enqueueSlotPartial(int slotIndex, int floatOffset, float[] data) {
    }

    public synchronized <T extends BlockEntity> void enqueueInitialBake(MachineInstanceHandle handle, BlockEntity be, MachineSpec<T> spec) {
        if (be instanceof com.hbm_m.api.render.RenderDirtyTracker tracker) {
            tracker.markRenderDirty();
        }
    }

    public synchronized void enqueueUpdate(MachineInstanceHandle handle, BlockEntity be, int dirtyMask) {
        if (be instanceof com.hbm_m.api.render.RenderDirtyTracker tracker) {
            tracker.markRenderDirty();
        }
    }

    /**
     * No-op: the retained graph has no GPU upload path (see class javadoc).
     */
    public synchronized void flushUpdates(int targetVbo) {
    }

    public synchronized void clear() {
        pendingCount = 0;
    }

    public synchronized void destroy() {
        pendingCount = 0;
    }

    public int getPendingCount() {
        return pendingCount;
    }
}
