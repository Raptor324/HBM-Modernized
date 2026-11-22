package com.hbm_m.test;

import com.hbm_m.client.render.retained.alloc.InstanceSlotAllocator;

import java.util.BitSet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Empirical adversarial stress test suite for {@link InstanceSlotAllocator}.
 * (Scenario 4/4b, which exercised PersistentIndirectCommandTable, were removed
 * together with that dead table - audit R4-3.4.5.)
 */
public final class RetainedAllocAdversarialTest {

    private RetainedAllocAdversarialTest() {}

    private static void assertTrue(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError("ASSERTION FAILED: " + message);
        }
    }

    private static void assertEquals(long expected, long actual, String message) {
        if (expected != actual) {
            throw new AssertionError("ASSERTION FAILED: " + message + " (expected: " + expected + ", actual: " + actual + ")");
        }
    }

    /**
     * Scenario 1: O(1) Allocation & Free Timing + FIFO Queue integrity over 100,000+ slots.
     */
    public static void testO1AllocationAndFreeFIFO() {
        System.out.println("=== Running Scenario 1: O(1) Allocation & Free FIFO Stress Test ===");
        InstanceSlotAllocator allocator = new InstanceSlotAllocator(65536);
        int testCount = 120_000;
        int[] allocated = new int[testCount];

        // Measure allocation time
        long startAlloc = System.nanoTime();
        for (int i = 0; i < testCount; i++) {
            allocated[i] = allocator.allocateSlot();
        }
        long allocNanos = System.nanoTime() - startAlloc;
        double avgAllocNs = (double) allocNanos / testCount;
        System.out.printf("Allocated %d slots in %.2f ms (average: %.2f ns/op)%n",
                testCount, allocNanos / 1_000_000.0, avgAllocNs);
        assertTrue(avgAllocNs < 500.0, "Average allocation time exceeded 500ns: " + avgAllocNs);

        // Verify FIFO order initially: slot indices 0..65535, then 65536..119999
        for (int i = 0; i < testCount; i++) {
            assertEquals(i, allocated[i], "Slot index mismatch at index " + i);
        }

        // Measure free time in FIFO order
        long startFree = System.nanoTime();
        for (int i = 0; i < testCount; i++) {
            allocator.freeSlot(allocated[i]);
        }
        long freeNanos = System.nanoTime() - startFree;
        double avgFreeNs = (double) freeNanos / testCount;
        System.out.printf("Freed %d slots in %.2f ms (average: %.2f ns/op)%n",
                testCount, freeNanos / 1_000_000.0, avgFreeNs);
        assertTrue(avgFreeNs < 500.0, "Average free time exceeded 500ns: " + avgFreeNs);

        // Re-allocation should return the freed slots in FIFO order
        for (int i = 0; i < testCount; i++) {
            int reallocated = allocator.allocateSlot();
            assertEquals(allocated[i], reallocated, "Reallocated slot must match FIFO order at " + i);
        }

        System.out.println("Scenario 1 PASSED: Strict O(1) performance and FIFO order verified.");
    }

    /**
     * Scenario 1b: Double-Free and Boundary Free Protection.
     */
    public static void testDoubleFreeProtection() {
        System.out.println("=== Running Scenario 1b: Double-Free & Boundary Protection ===");
        InstanceSlotAllocator allocator = new InstanceSlotAllocator(1024);

        int slotA = allocator.allocateSlot();
        int slotB = allocator.allocateSlot();
        assertEquals(0, slotA, "First slot should be 0");
        assertEquals(1, slotB, "Second slot should be 1");

        // Normal free
        allocator.freeSlot(slotA);

        // Attempt DOUBLE-FREE of slotA
        allocator.freeSlot(slotA);
        allocator.freeSlot(slotA);

        // Attempt invalid boundary frees
        allocator.freeSlot(-1);
        allocator.freeSlot(-9999);
        allocator.freeSlot(1024);
        allocator.freeSlot(500000);

        // Next allocation MUST yield slotA exactly once, then next fresh slot (slot 2)
        int realloc1 = allocator.allocateSlot();
        assertEquals(slotA, realloc1, "First reallocation must be slotA");

        int realloc2 = allocator.allocateSlot();
        assertEquals(2, realloc2, "Second reallocation must be fresh slot 2, NOT a duplicated slotA!");

        System.out.println("Scenario 1b PASSED: Double-free and boundary frees safely rejected.");
    }

    /**
     * Scenario 2: High-Water Mark Dynamic Contraction via Bitscan.
     */
    public static void testHighWaterMarkContraction() {
        System.out.println("=== Running Scenario 2: High-Water Mark Dynamic Contraction ===");
        InstanceSlotAllocator allocator = new InstanceSlotAllocator(65536);

        int targetSlots = 50_000;
        int[] slots = new int[targetSlots];
        for (int i = 0; i < targetSlots; i++) {
            slots[i] = allocator.allocateSlot();
        }
        assertEquals(50_000, allocator.getHighWaterMark(), "High-water mark after 50,000 sequential allocations");

        // 1. Monotonic contraction from 50,000 down to 40,000
        for (int i = 49_999; i >= 40_000; i--) {
            allocator.freeSlot(i);
            assertEquals(i, allocator.getHighWaterMark(), "High-water mark should contract to " + i);
        }
        assertEquals(40_000, allocator.getHighWaterMark(), "High-water mark after freeing top 10,000 slots");

        // 2. Out-of-order freeing: free slots 30,000 to 39,998 first (keeping 39,999 active)
        for (int i = 30_000; i < 39_999; i++) {
            allocator.freeSlot(i);
            // High-water mark must NOT contract yet because 39,999 is still active!
            assertEquals(40_000, allocator.getHighWaterMark(), "High-water mark must remain 40,000 while 39,999 is active");
        }

        // Now free the single highest active slot (39,999)
        allocator.freeSlot(39_999);
        // High-water mark must immediately contract past the entire freed hole down to 30,000!
        assertEquals(30_000, allocator.getHighWaterMark(),
                "High-water mark must dynamically contract from 40,000 to 30,000 via bitscan");

        // 3. Free remaining slots down to 0, verifying complete contraction to 0
        for (int i = 29_999; i >= 0; i--) {
            allocator.freeSlot(i);
            assertEquals(i, allocator.getHighWaterMark(), "High-water mark contracting down to " + i);
        }
        assertEquals(0, allocator.getHighWaterMark(), "High-water mark must be exactly 0 after freeing all slots");

        System.out.println("Scenario 2 PASSED: High-water mark dynamic contraction verified.");
    }

    /**
     * Scenario 3: Geometric Slab Expansion & Zero Data Loss.
     */
    public static void testGeometricExpansionAndZeroDataLoss() {
        System.out.println("=== Running Scenario 3: Geometric Capacity Expansion Stress ===");
        InstanceSlotAllocator allocator = new InstanceSlotAllocator(65536);
        assertEquals(65536, allocator.getCurrentCapacity(), "Initial capacity must be 65,536");

        BitSet activeOracle = new BitSet();

        // 1. Fill initial capacity (65,536 slots)
        for (int i = 0; i < 65536; i++) {
            int slot = allocator.allocateSlot();
            assertTrue(!activeOracle.get(slot), "Duplicate slot returned: " + slot);
            activeOracle.set(slot);
        }
        assertEquals(65536, allocator.getCurrentCapacity(), "Capacity should still be 65,536 before overflow");
        assertEquals(65536, allocator.getHighWaterMark(), "High-water mark at 65,536");

        // 2. Trigger expansion to 131,072
        int slot65536 = allocator.allocateSlot();
        assertEquals(131072, allocator.getCurrentCapacity(), "Capacity must double to 131,072");
        activeOracle.set(slot65536);

        // Fill up to 131,072
        for (int i = 65537; i < 131072; i++) {
            int s = allocator.allocateSlot();
            assertTrue(!activeOracle.get(s), "Duplicate slot returned: " + s);
            activeOracle.set(s);
        }
        assertEquals(131072, allocator.getCurrentCapacity(), "Capacity at 131,072");
        assertEquals(131072, allocator.getHighWaterMark(), "High-water mark at 131,072");

        // 3. Trigger expansion to 262,144
        int slot131072 = allocator.allocateSlot();
        assertEquals(262144, allocator.getCurrentCapacity(), "Capacity must double to 262,144");
        activeOracle.set(slot131072);

        // Fill up to 262,144
        for (int i = 131073; i < 262144; i++) {
            int s = allocator.allocateSlot();
            assertTrue(!activeOracle.get(s), "Duplicate slot returned: " + s);
            activeOracle.set(s);
        }
        assertEquals(262144, allocator.getCurrentCapacity(), "Capacity at 262,144");
        assertEquals(262144, allocator.getHighWaterMark(), "High-water mark at 262,144");

        // 4. Verify ZERO DATA LOSS: all 262,144 slots in activeOracle must be valid and distinct
        assertEquals(262144, activeOracle.cardinality(), "Active oracle must have exactly 262,144 distinct bits set");
        for (int i = 0; i < 262144; i++) {
            assertTrue(activeOracle.get(i), "Slot " + i + " must have been tracked without loss");
        }

        System.out.println("Scenario 3 PASSED: Geometric slab expansion to 262,144 with zero data loss verified.");
    }

    /**
     * Scenario 3b: Hard Ceiling Enforcement (1,048,576 slots / 128 MB VRAM).
     */
    public static void testHardCeilingEnforcement() {
        System.out.println("=== Running Scenario 3b: Hard Ceiling Enforcement ===");
        // Start near ceiling to keep test execution fast
        InstanceSlotAllocator allocator = new InstanceSlotAllocator(524288);
        for (int i = 0; i < 524288; i++) {
            allocator.allocateSlot();
        }
        assertEquals(524288, allocator.getCurrentCapacity(), "Capacity at 524,288");

        // Expand to MAX_ALLOWED_CAPACITY = 1,048,576
        allocator.allocateSlot();
        assertEquals(1048576, allocator.getCurrentCapacity(), "Capacity doubled to ceiling 1,048,576");

        // Allocate remaining slots up to 1,048,576
        for (int i = 524289; i < 1048576; i++) {
            allocator.allocateSlot();
        }
        assertEquals(1048576, allocator.getCurrentCapacity(), "Capacity at ceiling 1,048,576");

        // Attempting to allocate 1,048,577th slot MUST throw IllegalStateException
        boolean ceilingTriggered = false;
        try {
            allocator.allocateSlot();
        } catch (IllegalStateException e) {
            ceilingTriggered = true;
            assertTrue(e.getMessage().contains("VRAM Instance buffer hard ceiling reached"),
                    "Exception message must specify hard ceiling: " + e.getMessage());
        }
        assertTrue(ceilingTriggered, "Allocating beyond 1,048,576 slots must throw IllegalStateException");

        System.out.println("Scenario 3b PASSED: Hard ceiling of 1,048,576 slots strictly enforced.");
    }

    /**
     * Scenario 5: Multi-threaded Concurrency Safety.
     */
    public static void testConcurrentAllocationSafety() throws InterruptedException {
        System.out.println("=== Running Scenario 5: Multi-threaded Concurrency Safety ===");
        InstanceSlotAllocator allocator = new InstanceSlotAllocator(65536);

        int threads = 8;
        int opsPerThread = 10_000;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch latch = new CountDownLatch(threads);
        AtomicInteger totalAllocated = new AtomicInteger(0);
        AtomicBoolean failed = new AtomicBoolean(false);

        BitSet globalSet = new BitSet();
        Object setLock = new Object();

        for (int t = 0; t < threads; t++) {
            pool.submit(() -> {
                try {
                    for (int i = 0; i < opsPerThread; i++) {
                        int slot = allocator.allocateSlot();
                        synchronized (setLock) {
                            if (globalSet.get(slot)) {
                                failed.set(true);
                                System.err.println("RACE CONDITION: Slot " + slot + " allocated concurrently!");
                            }
                            globalSet.set(slot);
                        }
                        totalAllocated.incrementAndGet();
                    }
                } catch (Throwable e) {
                    failed.set(true);
                    e.printStackTrace();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        pool.shutdown();

        assertTrue(!failed.get(), "Concurrency race condition detected during multi-threaded allocations!");
        assertEquals(threads * opsPerThread, totalAllocated.get(), "Total allocated slots mismatch");
        assertEquals(threads * opsPerThread, globalSet.cardinality(), "All concurrently allocated slots must be distinct");

        System.out.println("Scenario 5 PASSED: Multi-threaded synchronization verified.");
    }

    public static void main(String[] args) {
        System.out.println("=================================================================");
        System.out.println("HBM-Modernized: Empirical Adversarial Stress Test Suite");
        System.out.println("Target: InstanceSlotAllocator");
        System.out.println("=================================================================");

        try {
            testO1AllocationAndFreeFIFO();
            testDoubleFreeProtection();
            testHighWaterMarkContraction();
            testGeometricExpansionAndZeroDataLoss();
            testHardCeilingEnforcement();
            testConcurrentAllocationSafety();

            System.out.println("=================================================================");
            System.out.println(">>> ALL EMPIRICAL ADVERSARIAL STRESS TESTS PASSED SUCCESSFULLY <<<");
            System.out.println("=================================================================");
        } catch (Throwable t) {
            System.err.println("=================================================================");
            System.err.println(">>> EMPIRICAL STRESS TEST FAILURE DETECTED <<<");
            System.err.println("=================================================================");
            t.printStackTrace();
            System.exit(1);
        }
    }
}
