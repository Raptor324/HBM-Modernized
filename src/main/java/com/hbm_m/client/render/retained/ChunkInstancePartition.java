package com.hbm_m.client.render.retained;

import com.hbm_m.client.render.retained.alloc.InstanceSlotAllocator;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;

/**
 * Spatial partition coordinating all retained machine instance handles within a chunk.
 * Maintains conservative world-space AABB enclosing all contained machines.
 *
 * @credit CrankShaft / fewizz / Jdb100
 */
public final class ChunkInstancePartition {

    private final long chunkKey;
    private final int chunkX;
    private final int chunkZ;
    private final Long2ObjectOpenHashMap<MachineInstanceHandle> handles = new Long2ObjectOpenHashMap<>();

    private double minX, minY, minZ;
    private double maxX, maxY, maxZ;
    private AABB cachedAabb;
    private boolean boundsDirty = true;
    private boolean statusDirty = true;
    private boolean cachedPurelyStatic = false;

    public ChunkInstancePartition(long chunkKey) {
        this.chunkKey = chunkKey;
        this.chunkX = (int) (chunkKey & 0xFFFFFFFFL);
        this.chunkZ = (int) (chunkKey >>> 32);
        resetBounds();
    }

    public synchronized void markDirty() {
        this.statusDirty = true;
    }

    private void resetBounds() {
        // R4-2.4: init to ±MAX_VALUE like the Y axis - pinning X/Z to the chunk
        // footprint made the partition AABB permanently >= 16x16 even for a
        // single small machine.
        this.minX = Double.MAX_VALUE;
        this.maxX = -Double.MAX_VALUE;
        this.minZ = Double.MAX_VALUE;
        this.maxZ = -Double.MAX_VALUE;
        this.minY = Double.MAX_VALUE;
        this.maxY = -Double.MAX_VALUE;
        this.boundsDirty = true;
    }

    public synchronized void addHandle(MachineInstanceHandle handle) {
        if (handle == null) return;
        handles.put(handle.getBlockPosKey(), handle);
        // R4-2.4: after a removeHandle the cached bounds are over-inclusive (they
        // still contain the removed machine). expandBounds would bake those stale
        // coordinates into cachedAabb AND clear boundsDirty - the recompute would
        // never happen. Leave it to getBounds() instead.
        if (!boundsDirty) {
            expandBounds(handle.getBounds());
        }
        this.statusDirty = true;
    }

    public synchronized MachineInstanceHandle removeHandle(long blockPosKey) {
        MachineInstanceHandle removed = handles.remove(blockPosKey);
        if (removed != null) {
            boundsDirty = true;
            statusDirty = true;
        }
        return removed;
    }

    public synchronized void freeAll(InstanceSlotAllocator allocator) {
        for (MachineInstanceHandle handle : handles.values()) {
            handle.free(allocator);
        }
        handles.clear();
        boundsDirty = true;
        statusDirty = true;
        cachedPurelyStatic = false;
    }

    public synchronized boolean isEmpty() {
        return handles.isEmpty();
    }

    public synchronized Long2ObjectOpenHashMap<MachineInstanceHandle> getHandles() {
        return handles;
    }

    public long getChunkKey() {
        return chunkKey;
    }

    public int getChunkX() {
        return chunkX;
    }

    public int getChunkZ() {
        return chunkZ;
    }

    public synchronized AABB getBounds() {
        if (boundsDirty) {
            recomputeBounds();
        }
        return cachedAabb;
    }

    private void expandBounds(AABB b) {
        if (b == null) return;
        minX = Math.min(minX, b.minX);
        maxX = Math.max(maxX, b.maxX);
        minY = Math.min(minY, b.minY);
        maxY = Math.max(maxY, b.maxY);
        minZ = Math.min(minZ, b.minZ);
        maxZ = Math.max(maxZ, b.maxZ);
        cachedAabb = new AABB(minX, (minY > maxY ? -64.0 : minY), minZ,
                maxX, (minY > maxY ? 320.0 : maxY), maxZ);
        boundsDirty = false;
    }

    private void recomputeBounds() {
        resetBounds();
        for (MachineInstanceHandle handle : handles.values()) {
            expandBounds(handle.getBounds());
        }
        if (minY > maxY) {
            // No live handles (or degenerate): fall back to the chunk footprint
            // so callers never see an inverted/empty-world AABB.
            cachedAabb = new AABB((double) (chunkX << 4), -64.0, (double) (chunkZ << 4),
                    (double) (chunkX << 4) + 16.0, 320.0, (double) (chunkZ << 4) + 16.0);
        }
        boundsDirty = false;
    }

    /**
     * Checks if all machine handles within this chunk partition are active, static,
     * not marked dirty, and safely retained in RetainedInstanceGraph.
     * When true, this entire chunk can bypass Level 2 machine iteration in collectMain.
     */
    public synchronized boolean isPurelyStaticAndClean() {
        if (handles.isEmpty()) return false;
        RetainedInstanceGraph rig = RetainedInstanceGraph.get();
        if (rig == null) return false;

        if (!statusDirty) {
            return cachedPurelyStatic;
        }

        for (MachineInstanceHandle handle : handles.values()) {
            if (handle == null || !handle.isActive()) {
                cachedPurelyStatic = false;
                statusDirty = false;
                return false;
            }
            BlockEntity be = handle.getBlockEntity();
            if (be == null || be.isRemoved()) {
                cachedPurelyStatic = false;
                statusDirty = false;
                return false;
            }
            if (!rig.isMachineStaticAndRetained(be)) {
                cachedPurelyStatic = false;
                statusDirty = false;
                return false;
            }
        }
        cachedPurelyStatic = true;
        statusDirty = false;
        return true;
    }
}
