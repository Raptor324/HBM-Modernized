package com.hbm_m.client.render.retained;

import java.lang.ref.WeakReference;
import com.hbm_m.client.render.retained.alloc.InstanceSlotAllocator;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.phys.AABB;

/**
 * Persistent handle coordinating allocated GPU instance slots and spatial bounds for a machine.
 *
 * @credit CrankShaft / fewizz / Jdb100
 */
public final class MachineInstanceHandle {

    private final long blockPosKey;
    private final long chunkPosKey;
    private final BlockEntityType<?> type;
    private final int[] slotIndices;
    private AABB bounds;
    private boolean active = true;
    private final WeakReference<BlockEntity> blockEntityRef;

    public MachineInstanceHandle(long blockPosKey, long chunkPosKey, BlockEntityType<?> type,
                                 int[] slotIndices, AABB bounds) {
        this(blockPosKey, chunkPosKey, type, slotIndices, bounds, null);
    }

    public MachineInstanceHandle(long blockPosKey, long chunkPosKey, BlockEntityType<?> type,
                                 int[] slotIndices, AABB bounds, BlockEntity blockEntity) {
        this.blockPosKey = blockPosKey;
        this.chunkPosKey = chunkPosKey;
        this.type = type;
        this.slotIndices = slotIndices;
        this.bounds = bounds;
        this.blockEntityRef = blockEntity != null ? new WeakReference<>(blockEntity) : null;
    }

    public BlockEntity getBlockEntity() {
        return blockEntityRef != null ? blockEntityRef.get() : null;
    }

    public synchronized void free(InstanceSlotAllocator allocator) {
        if (!active) return;
        if (allocator != null && slotIndices != null) {
            for (int slot : slotIndices) {
                if (slot > 0 || allocator.isSlotAllocated(slot)) {
                    allocator.freeSlot(slot);
                }
            }
        }
        active = false;
    }

    public long getBlockPosKey() { return blockPosKey; }
    public long getChunkPosKey() { return chunkPosKey; }
    public BlockEntityType<?> getType() { return type; }
    public int[] getSlotIndices() { return slotIndices; }
    public AABB getBounds() { return bounds; }
    public void setBounds(AABB bounds) { this.bounds = bounds; }
    public boolean isActive() { return active; }
}
