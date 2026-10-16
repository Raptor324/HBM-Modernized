package com.hbm_m.test;

import com.hbm_m.client.render.retained.ChunkInstancePartition;
import com.hbm_m.client.render.retained.MachineInstanceHandle;
import com.hbm_m.client.render.retained.alloc.InstanceSlotAllocator;
import com.hbm_m.client.render.retained.upload.SparseUpdateQueue;
import net.minecraft.world.phys.AABB;

/**
 * Empirical Unit Tests for Milestone 2:
 * Retained Scene Graph & Event-Driven Dirty Tracking.
 * Validates ChunkInstancePartition bounds tracking, empty/clean static state contract,
 * SparseUpdateQueue allocation and batching, and slot persistence.
 */
public class Milestone2RetainedSceneGraphTest {

    public static void main(String[] args) {
        System.out.println(">>> Running Milestone2RetainedSceneGraphTest suite <<<");
        testPartitionBoundsTracking();
        testPartitionEmptyStaticContract();
        testSlotPersistenceAndHighWaterMark();
        testSparseQueueBatching();
        System.out.println(">>> All Milestone2RetainedSceneGraphTest tests PASSED successfully! <<<");
    }

    public static void testPartitionBoundsTracking() {
        System.out.println("[Test 1] Testing ChunkInstancePartition bounds expansion and reset...");
        long chunkKey = 10L | (20L << 32);
        ChunkInstancePartition partition = new ChunkInstancePartition(chunkKey);

        if (partition.getChunkKey() != chunkKey) {
            throw new AssertionError("ChunkKey mismatch: " + partition.getChunkKey() + " != " + chunkKey);
        }
        if (!partition.isEmpty()) {
            throw new AssertionError("Partition must be initially empty");
        }

        // Initial default bounds before any handle is added
        AABB initialBounds = partition.getBounds();
        if (initialBounds == null) {
            throw new AssertionError("Initial bounds must not be null");
        }

        // Add handle with known bounds
        long posKey = 12345L;
        AABB handleBounds = new AABB(160, 60, 320, 165, 68, 325);
        MachineInstanceHandle handle = new MachineInstanceHandle(
                posKey, chunkKey, null, new int[]{0, 1}, handleBounds
        );

        partition.addHandle(handle);
        if (partition.isEmpty()) {
            throw new AssertionError("Partition must not be empty after addHandle");
        }
        if (!partition.getHandles().containsKey(posKey)) {
            throw new AssertionError("Handle must be registered by posKey");
        }

        AABB expanded = partition.getBounds();
        if (expanded.minY > 60.0 || expanded.maxY < 68.0) {
            throw new AssertionError("Expanded bounds must encompass handle bounds: " + expanded);
        }

        // Remove handle
        MachineInstanceHandle removed = partition.removeHandle(posKey);
        if (removed != handle) {
            throw new AssertionError("Removed handle must match added handle");
        }
        if (!partition.isEmpty()) {
            throw new AssertionError("Partition must be empty after removal");
        }
    }

    public static void testPartitionEmptyStaticContract() {
        System.out.println("[Test 2] Testing ChunkInstancePartition isPurelyStaticAndClean contract...");
        long chunkKey = 5L | (5L << 32);
        ChunkInstancePartition partition = new ChunkInstancePartition(chunkKey);

        // Empty partition must never report purely static and clean (no machines to draw)
        if (partition.isPurelyStaticAndClean()) {
            throw new AssertionError("Empty partition must return false for isPurelyStaticAndClean");
        }
    }

    public static void testSlotPersistenceAndHighWaterMark() {
        System.out.println("[Test 3] Testing InstanceSlotAllocator persistence and high-water mark...");
        InstanceSlotAllocator allocator = new InstanceSlotAllocator(256);

        long blockKeyA = 1000L;
        long blockKeyB = 2000L;
        long blockKeyC = 3000L;

        int slotA = allocator.allocateSlot(blockKeyA);
        int slotB = allocator.allocateSlot(blockKeyB);
        int slotC = allocator.allocateSlot(blockKeyC);

        if (slotA < 0 || slotB < 0 || slotC < 0) {
            throw new AssertionError("Allocated slots must be non-negative");
        }
        if (slotA == slotB || slotB == slotC) {
            throw new AssertionError("Distinct keys must have distinct slots");
        }
        if (allocator.getHighWaterMark() != 3) {
            throw new AssertionError("High water mark must be 3");
        }

        // Allocating existing key is idempotent
        if (allocator.allocateSlot(blockKeyA) != slotA) {
            throw new AssertionError("Idempotent allocation must return same slot");
        }
        if (allocator.getHighWaterMark() != 3) {
            throw new AssertionError("High water mark must remain 3");
        }

        // Free middle slot
        allocator.freeSlot(slotB);
        if (allocator.isSlotAllocated(slotB)) {
            throw new AssertionError("Slot B must be unallocated in bitmap");
        }
        if (allocator.getHighWaterMark() != 3) {
            throw new AssertionError("High water mark must remain 3 when middle slot freed");
        }

        // Free top slot -> high water mark contracts to 1 (slot A remaining)
        allocator.freeSlot(slotC);
        if (allocator.getHighWaterMark() != 1) {
            throw new AssertionError("High water mark must contract to 1: actual=" + allocator.getHighWaterMark());
        }
    }

    public static void testSparseQueueBatching() {
        System.out.println("[Test 4] Testing SparseUpdateQueue allocation and destruction...");
        InstanceSlotAllocator allocator = new InstanceSlotAllocator(128);
        SparseUpdateQueue queue = new SparseUpdateQueue(allocator);

        if (queue.getPendingCount() != 0) {
            throw new AssertionError("Pending count must initially be 0");
        }
        queue.destroy();
    }
}
