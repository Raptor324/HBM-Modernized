package tools;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Empirical Adversarial Challenge Test Harness for Milestone 1 Iteration 2:
 * 1. True AABB Distance Culling vs Chunk Center
 * 2. Cross-Chunk Multiblock Boundary & Negative Coordinate Tests
 * 3. Tall Multiblock / Mining Drill Bedrock Culling Tests
 * 4. 360-Degree Perimeter Sweep
 * 5. Dynamic Bounds Refresh via markMachineBoundsDirty & Autonomous TTL
 * 6. Machine Removal and Bucket Bounds Shrinking
 * 7. Multi-threaded Producer-Consumer Concurrency Stress (200,000 ops)
 */
public class ChallengerIter2StressHarness {

    public static final class SimpleAABB {
        public final double minX, minY, minZ;
        public final double maxX, maxY, maxZ;

        public SimpleAABB(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
            this.minX = minX;
            this.minY = minY;
            this.minZ = minZ;
            this.maxX = maxX;
            this.maxY = maxY;
            this.maxZ = maxZ;
        }

        public double distanceToSqr(double x, double y, double z) {
            double dx = Math.max(0.0, Math.max(minX - x, x - maxX));
            double dy = Math.max(0.0, Math.max(minY - y, y - maxY));
            double dz = Math.max(0.0, Math.max(minZ - z, z - maxZ));
            return dx * dx + dy * dy + dz * dz;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof SimpleAABB other)) return false;
            return Double.compare(other.minX, minX) == 0 &&
                    Double.compare(other.minY, minY) == 0 &&
                    Double.compare(other.minZ, minZ) == 0 &&
                    Double.compare(other.maxX, maxX) == 0 &&
                    Double.compare(other.maxY, maxY) == 0 &&
                    Double.compare(other.maxZ, maxZ) == 0;
        }

        @Override
        public String toString() {
            return String.format("[%.1f, %.1f, %.1f] -> [%.1f, %.1f, %.1f]", minX, minY, minZ, maxX, maxY, maxZ);
        }
    }

    public static class MockBlockEntity {
        public final int x, y, z;
        public SimpleAABB bounds;
        public boolean removed = false;
        public final boolean isClient;

        public MockBlockEntity(int x, int y, int z, SimpleAABB bounds, boolean isClient) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.bounds = bounds;
            this.isClient = isClient;
        }
    }

    /**
     * Exact replication of NucleusDispatcherBypass.ChunkBucket logic in HBM-Modernized.
     */
    public static final class ChunkBucketReplica {
        public final long chunkKey;
        public final int chunkX;
        public final int chunkZ;
        public final ArrayList<MockBlockEntity> machines = new ArrayList<>(16);

        private double minX, maxX;
        private double minZ, maxZ;
        private double minY = Double.MAX_VALUE;
        private double maxY = -Double.MAX_VALUE;
        private SimpleAABB cachedAabb = null;
        public boolean boundsDirty = true;
        private long lastBoundsTick = -1L;
        private static final int BOUNDS_REFRESH_TICKS = 20;

        public ChunkBucketReplica(long chunkKey, int chunkX, int chunkZ) {
            this.chunkKey = chunkKey;
            this.chunkX = chunkX;
            this.chunkZ = chunkZ;
            resetBaseBounds();
        }

        private void resetBaseBounds() {
            this.minX = (double) (chunkX << 4);
            this.maxX = this.minX + 16.0;
            this.minZ = (double) (chunkZ << 4);
            this.maxZ = this.minZ + 16.0;
            this.minY = Double.MAX_VALUE;
            this.maxY = -Double.MAX_VALUE;
            this.boundsDirty = true;
        }

        public void add(MockBlockEntity be) {
            machines.add(be);
            expandBoundsOnly(be);
            commitCachedAabb();
        }

        public void remove(MockBlockEntity be) {
            machines.remove(be);
            boundsDirty = true;
        }

        public boolean isEmpty() {
            return machines.isEmpty();
        }

        public void markBoundsDirty() {
            this.boundsDirty = true;
        }

        private void expandBoundsOnly(MockBlockEntity be) {
            SimpleAABB box = be.bounds;
            if (box != null) {
                minX = Math.min(minX, box.minX);
                maxX = Math.max(maxX, box.maxX);
                minY = Math.min(minY, box.minY);
                maxY = Math.max(maxY, box.maxY);
                minZ = Math.min(minZ, box.minZ);
                maxZ = Math.max(maxZ, box.maxZ);
            } else {
                minX = Math.min(minX, be.x - 1.0);
                maxX = Math.max(maxX, be.x + 2.0);
                minY = Math.min(minY, be.y - 1.0);
                maxY = Math.max(maxY, be.y + 3.0);
                minZ = Math.min(minZ, be.z - 1.0);
                maxZ = Math.max(maxZ, be.z + 2.0);
            }
        }

        private void commitCachedAabb() {
            double finalMinY = (minY > maxY) ? -64.0 : minY;
            double finalMaxY = (minY > maxY) ? 320.0 : maxY;
            cachedAabb = new SimpleAABB(minX, finalMinY, minZ, maxX, finalMaxY, maxZ);
            boundsDirty = false;
        }

        public void recomputeBounds() {
            resetBaseBounds();
            for (int i = 0; i < machines.size(); i++) {
                expandBoundsOnly(machines.get(i));
            }
            commitCachedAabb();
        }

        public SimpleAABB getBounds(long tick) {
            if (boundsDirty || (tick - lastBoundsTick >= BOUNDS_REFRESH_TICKS)) {
                recomputeBounds();
                lastBoundsTick = tick;
            }
            return cachedAabb;
        }

        public double distSq(double camX, double camY, double camZ, long tick) {
            return getBounds(tick).distanceToSqr(camX, camY, camZ);
        }

        // Legacy chunk-center calculation for adversarial comparison
        public double legacyChunkCenterDistSq(double camX, double camZ) {
            double cx = (chunkX << 4) + 8.0 - camX;
            double cz = (chunkZ << 4) + 8.0 - camZ;
            return cx * cx + cz * cz;
        }
    }

    public static void main(String[] args) {
        System.out.println("===============================================================================");
        System.out.println("EMPIRICAL ADVERSARIAL CHALLENGE HARNESS: NucleusDispatcherBypass (ITERATION 2)");
        System.out.println("===============================================================================");

        boolean allPass = true;
        allPass &= test1_TrueAabbDistanceCalculation();
        allPass &= test2_LargeCrossChunkMultiblockExtension();
        allPass &= test3_NegativeCoordinateCrossChunkMultiblock();
        allPass &= test4_TallMultiblockVerticalCulling();
        allPass &= test5_PerimeterSweep360Degrees();
        allPass &= test6_DynamicBoundsRefreshLifecycle();
        allPass &= test7_BucketBoundsShrinkingOnRemoval();
        allPass &= test8_MultiThreadedConcurrencyStress();

        System.out.println("===============================================================================");
        System.out.println("FINAL VERDICT: " + (allPass ? "ALL EMPIRICAL TESTS PASSED (APPROVED)" : "FAILURES DETECTED"));
        System.out.println("===============================================================================");
        if (!allPass) {
            System.exit(1);
        }
    }

    /**
     * Test 1: Verify distanceToSqr calculation against exact Euclidean geometry.
     */
    private static boolean test1_TrueAabbDistanceCalculation() {
        System.out.println("\n[TEST 1] Mathematical Precision of True AABB Distance Calculation");
        SimpleAABB box = new SimpleAABB(10.0, 60.0, 10.0, 20.0, 70.0, 20.0);

        // Case A: Inside box -> dist = 0.0
        double distInside = box.distanceToSqr(15.0, 65.0, 15.0);
        boolean passA = (distInside == 0.0);

        // Case B: Boundary -> dist = 0.0
        double distBoundary = box.distanceToSqr(20.0, 70.0, 20.0);
        boolean passB = (distBoundary == 0.0);

        // Case C: Pure X displacement (+5 blocks) -> distSq = 25.0
        double distX = box.distanceToSqr(25.0, 65.0, 15.0);
        boolean passC = (Math.abs(distX - 25.0) < 1e-9);

        // Case D: Pure Y displacement (-10 blocks) -> distSq = 100.0
        double distY = box.distanceToSqr(15.0, 50.0, 15.0);
        boolean passD = (Math.abs(distY - 100.0) < 1e-9);

        // Case E: Corner diagonal (+3, +4, 0) -> distSq = 9 + 16 = 25.0
        double distCorner = box.distanceToSqr(23.0, 74.0, 15.0);
        boolean passE = (Math.abs(distCorner - 25.0) < 1e-9);

        boolean pass = passA && passB && passC && passD && passE;
        System.out.printf("  Inside: %.1f, Boundary: %.1f, dx=5: %.1f, dy=10: %.1f, 3-4 corner: %.1f -> %s\n",
                distInside, distBoundary, distX, distY, distCorner, (pass ? "PASS" : "FAIL"));
        return pass;
    }

    /**
     * Test 2: Large cross-chunk multiblock (64 blocks long, crossing 4 chunk boundaries).
     * Compares legacy chunk center culling against true AABB distance.
     */
    private static boolean test2_LargeCrossChunkMultiblockExtension() {
        System.out.println("\n[TEST 2] Adversarial 64-Block Long Cross-Chunk Multiblock Extension");
        ChunkBucketReplica bucket = new ChunkBucketReplica(0L, 0, 0); // Chunk (0, 0)

        // Origin at (8, 64, 8) in chunk 0, extending 64 blocks along +X to X=72 (chunk 4)
        MockBlockEntity linearAccelerator = new MockBlockEntity(8, 64, 8,
                new SimpleAABB(8.0, 64.0, 7.0, 72.0, 68.0, 9.0), true);
        bucket.add(linearAccelerator);

        double viewDist = 48.0;
        double maxDistBucket = viewDist + 24.0; // 72.0
        double maxDistMachine = viewDist + 8.0;  // 56.0
        double maxDistSqBucket = maxDistBucket * maxDistBucket;
        double maxDistSqMachine = maxDistMachine * maxDistMachine;

        // Player standing at X=82, Y=64, Z=8 (only 10 blocks away from machine tip X=72! View distance is 48 blocks)
        double camX = 82.0, camY = 64.0, camZ = 8.0;

        double legacyCenterDistSq = bucket.legacyChunkCenterDistSq(camX, camZ);
        double aabbDistSq = bucket.distSq(camX, camY, camZ, 0L);
        double machineDistSq = linearAccelerator.bounds.distanceToSqr(camX, camY, camZ);

        System.out.printf("  Player pos: (%.1f, %.1f, %.1f) - 4 blocks from multiblock tip\n", camX, camY, camZ);
        System.out.printf("  Legacy Chunk Center distSq: %.1f (dist: %.1f blocks) vs maxDistSq: %.1f\n",
                legacyCenterDistSq, Math.sqrt(legacyCenterDistSq), maxDistSqBucket);
        System.out.printf("  True Bucket AABB distSq: %.1f (dist: %.1f blocks) vs maxDistSq: %.1f\n",
                aabbDistSq, Math.sqrt(aabbDistSq), maxDistSqBucket);
        System.out.printf("  True Machine AABB distSq: %.1f (dist: %.1f blocks) vs maxDistSq: %.1f\n",
                machineDistSq, Math.sqrt(machineDistSq), maxDistSqMachine);

        boolean legacyCulled = legacyCenterDistSq > maxDistSqBucket;
        boolean aabbVisible = aabbDistSq <= maxDistSqBucket;
        boolean machineVisible = machineDistSq <= maxDistSqMachine;

        System.out.println("  Legacy behavior: " + (legacyCulled ? "CULLED (VULNERABLE)" : "VISIBLE"));
        System.out.println("  Remediated Level 1 (Bucket AABB): " + (aabbVisible ? "PASS (VISIBLE)" : "FAIL (CULLED)"));
        System.out.println("  Remediated Level 2 (Machine AABB): " + (machineVisible ? "PASS (VISIBLE)" : "FAIL (CULLED)"));

        return legacyCulled && aabbVisible && machineVisible;
    }

    /**
     * Test 3: Negative chunk coordinates multiblock spanning multiple negative chunks.
     */
    private static boolean test3_NegativeCoordinateCrossChunkMultiblock() {
        System.out.println("\n[TEST 3] Negative Chunk Coordinates Multiblock Spanning Across Chunks");
        int chunkX = -2; // World X: [-32, -16]
        int chunkZ = -2; // World Z: [-32, -16]
        long key = (((long) chunkX) << 32) | (chunkZ & 0xFFFFFFFFL);
        ChunkBucketReplica bucket = new ChunkBucketReplica(key, chunkX, chunkZ);

        // Machine at (-20, 64, -20) extending to (-50, 75, -45)
        MockBlockEntity negMachine = new MockBlockEntity(-20, 64, -20,
                new SimpleAABB(-50.0, 64.0, -45.0, -18.0, 75.0, -18.0), true);
        bucket.add(negMachine);

        SimpleAABB b = bucket.getBounds(0L);
        System.out.println("  Bucket bounds: " + b);

        // Check bounds min/max
        boolean boundsOk = (b.minX <= -50.0 && b.maxX >= -16.0 && b.minZ <= -45.0 && b.maxZ >= -16.0);

        // Player standing at (-53, 64, -45) (3 blocks from machine edge)
        double camX = -53.0, camY = 64.0, camZ = -45.0;
        double distSq = bucket.distSq(camX, camY, camZ, 0L);
        double dist = Math.sqrt(distSq);

        System.out.printf("  Player at (%.1f, %.1f, %.1f), calculated distance: %.1f blocks (distSq: %.1f)\n",
                camX, camY, camZ, dist, distSq);

        boolean pass = boundsOk && Math.abs(distSq - 9.0) < 1e-9;
        System.out.println("  Result: " + (pass ? "PASS" : "FAIL"));
        return pass;
    }

    /**
     * Test 4: Tall vertical multiblock (Mining Drill digging down to bedrock).
     * Camera deep in ravine/bedrock looking at the drill bit.
     */
    private static boolean test4_TallMultiblockVerticalCulling() {
        System.out.println("\n[TEST 4] Tall Vertical Multiblock / Mining Drill Bedrock Culling");
        ChunkBucketReplica bucket = new ChunkBucketReplica(0L, 0, 0);

        // Drill at surface Y=64, digging down to Y=-60 (124 blocks deep)
        MockBlockEntity drill = new MockBlockEntity(8, 64, 8,
                new SimpleAABB(6.0, -60.0, 6.0, 10.0, 68.0, 10.0), true);
        bucket.add(drill);

        double maxDist = 64.0 + 24.0;
        double maxDistSq = maxDist * maxDist;

        // Player at bedrock Y=-55, X=8, Z=8 (inside the drill column shaft, right next to drill bit)
        double camX = 8.0, camY = -55.0, camZ = 8.0;

        // Legacy BE pos distance:
        double dyPos = 64.0 - (-55.0); // 119 blocks!
        double legacyDistSq = dyPos * dyPos; // 14,161

        double aabbDistSq = bucket.distSq(camX, camY, camZ, 0L);
        double machineDistSq = drill.bounds.distanceToSqr(camX, camY, camZ);

        System.out.printf("  Player at Y=-55 (bedrock). Machine BE pos at Y=64.\n");
        System.out.printf("  Legacy BE pos distance squared: %.1f (dist: %.1f) vs maxDistSq: %.1f\n",
                legacyDistSq, Math.sqrt(legacyDistSq), maxDistSq);
        System.out.printf("  True AABB distance squared: %.1f vs maxDistSq: %.1f\n", aabbDistSq, maxDistSq);

        boolean legacyCulled = legacyDistSq > maxDistSq;
        boolean aabbVisible = aabbDistSq == 0.0 && machineDistSq == 0.0;

        System.out.println("  Legacy behavior: " + (legacyCulled ? "CULLED AT BEDROCK (VULNERABLE)" : "VISIBLE"));
        System.out.println("  Remediated behavior: " + (aabbVisible ? "PASS (PERFECTLY VISIBLE)" : "FAIL"));

        return legacyCulled && aabbVisible;
    }

    /**
     * Test 5: 360-Degree Perimeter Sweep around a 30x30 multiblock.
     * Sweeps camera around at a fixed distance of 5 blocks, verifying distance <= 25.0 at all angles.
     */
    private static boolean test5_PerimeterSweep360Degrees() {
        System.out.println("\n[TEST 5] 360-Degree Perimeter Sweep (72 Ray Samples at 5-degree increments)");
        ChunkBucketReplica bucket = new ChunkBucketReplica(0L, 0, 0);

        // 30x30 Multiblock centered at (8, 64, 8) extending [-7, 23]
        SimpleAABB box = new SimpleAABB(-7.0, 60.0, -7.0, 23.0, 70.0, 23.0);
        MockBlockEntity machine = new MockBlockEntity(8, 64, 8, box, true);
        bucket.add(machine);

        double centerX = 8.0, centerZ = 8.0;
        double radius = 15.0 + 5.0; // 15 half-extent + 5 margin = 20 blocks
        int steps = 72;
        boolean sweepPass = true;

        for (int i = 0; i < steps; i++) {
            double angle = Math.toRadians(i * (360.0 / steps));
            double camX = centerX + radius * Math.cos(angle);
            double camZ = centerZ + radius * Math.sin(angle);
            double camY = 65.0;

            double distSq = bucket.distSq(camX, camY, camZ, 0L);
            double dist = Math.sqrt(distSq);

            // Since box is square [-7, 23] (size 30x30), max distance at corners is slightly less than 5
            if (dist > 5.0001 || dist < 0.0) {
                System.out.printf("  Anomaly at angle %.1f deg: dist = %.3f\n", Math.toDegrees(angle), dist);
                sweepPass = false;
            }
        }

        System.out.println("  All 72 angular samples verified within expected [0.0, 5.0] distance: " + (sweepPass ? "PASS" : "FAIL"));
        return sweepPass;
    }

    /**
     * Test 6: Dynamic Bounds Refresh via markMachineBoundsDirty and Autonomous TTL.
     */
    private static boolean test6_DynamicBoundsRefreshLifecycle() {
        System.out.println("\n[TEST 6] Dynamic Bounds Refresh Lifecycle (Progressive Digging)");
        ChunkBucketReplica bucket = new ChunkBucketReplica(0L, 0, 0);
        MockBlockEntity drill = new MockBlockEntity(8, 64, 8,
                new SimpleAABB(7.0, 63.0, 7.0, 9.0, 67.0, 9.0), true);
        bucket.add(drill);

        SimpleAABB initial = bucket.getBounds(0L);
        boolean passInitial = (initial.minY == 63.0);

        // 1. Explicit markBoundsDirty trigger
        drill.bounds = new SimpleAABB(7.0, 40.0, 7.0, 9.0, 67.0, 9.0);
        bucket.markBoundsDirty();
        SimpleAABB afterDirty = bucket.getBounds(0L);
        boolean passDirty = (afterDirty.minY == 40.0);

        // 2. Progressive digging without markBoundsDirty (TTL autonomous refresh)
        drill.bounds = new SimpleAABB(7.0, 20.0, 7.0, 9.0, 67.0, 9.0);
        // Before TTL fires (tick 10 < 20) -> cached value
        SimpleAABB beforeTtl = bucket.getBounds(10L);
        boolean passBeforeTtl = (beforeTtl.minY == 40.0);

        // After TTL fires (tick 25 >= 20) -> refreshed
        SimpleAABB afterTtl = bucket.getBounds(25L);
        boolean passAfterTtl = (afterTtl.minY == 20.0);

        boolean pass = passInitial && passDirty && passBeforeTtl && passAfterTtl;
        System.out.println("  Initial: " + passInitial + ", Explicit dirty: " + passDirty +
                ", Stale before TTL: " + passBeforeTtl + ", Refreshed after TTL: " + passAfterTtl +
                " -> " + (pass ? "PASS" : "FAIL"));
        return pass;
    }

    /**
     * Test 7: Bucket Bounds Shrinking on Machine Removal.
     */
    private static boolean test7_BucketBoundsShrinkingOnRemoval() {
        System.out.println("\n[TEST 7] Bucket Bounds Shrinking upon Machine Removal");
        ChunkBucketReplica bucket = new ChunkBucketReplica(0L, 0, 0);

        MockBlockEntity smallMachine = new MockBlockEntity(2, 64, 2,
                new SimpleAABB(2.0, 64.0, 2.0, 3.0, 65.0, 3.0), true);
        MockBlockEntity giantMachine = new MockBlockEntity(8, 64, 8,
                new SimpleAABB(0.0, 10.0, 0.0, 50.0, 90.0, 50.0), true);

        bucket.add(smallMachine);
        bucket.add(giantMachine);

        SimpleAABB inflated = bucket.getBounds(0L);
        boolean wasInflated = (inflated.maxX == 50.0 && inflated.maxY == 90.0 && inflated.minY == 10.0);

        // Remove giant machine
        bucket.remove(giantMachine);
        SimpleAABB deflated = bucket.getBounds(1L);

        // Bounds should shrink back to chunk (0,0) [0, 16] and smallMachine Y [64, 65]
        boolean shrank = (deflated.maxX == 16.0 && deflated.maxY == 65.0 && deflated.minY == 64.0);

        System.out.printf("  Inflated bounds: %s\n", inflated);
        System.out.printf("  Deflated bounds: %s\n", deflated);

        boolean pass = wasInflated && shrank;
        System.out.println("  Result: " + (pass ? "PASS" : "FAIL"));
        return pass;
    }

    /**
     * Test 8: High-contention multi-threaded producer/consumer concurrency stress.
     * 4 Producer threads (world events) + 1 Consumer thread (render loop). 200,000 total items.
     */
    private static boolean test8_MultiThreadedConcurrencyStress() {
        System.out.println("\n[TEST 8] Lock-Free ConcurrentLinkedQueue Multi-Threaded Stress (200,000 Operations)");
        final ConcurrentLinkedQueue<MockBlockEntity> queue = new ConcurrentLinkedQueue<>();
        final int PRODUCERS = 4;
        final int OPS_PER_PRODUCER = 50_000;
        final int TOTAL_OPS = PRODUCERS * OPS_PER_PRODUCER;

        final AtomicInteger activeProducers = new AtomicInteger(PRODUCERS);
        final AtomicInteger totalProduced = new AtomicInteger(0);
        final AtomicInteger totalConsumed = new AtomicInteger(0);
        final AtomicInteger errors = new AtomicInteger(0);
        final CountDownLatch latch = new CountDownLatch(PRODUCERS + 1);

        // Start producers
        for (int p = 0; p < PRODUCERS; p++) {
            final int producerId = p;
            new Thread(() -> {
                try {
                    for (int i = 0; i < OPS_PER_PRODUCER; i++) {
                        try {
                            MockBlockEntity be = new MockBlockEntity(i, 64, producerId, null, true);
                            queue.add(be);
                            totalProduced.incrementAndGet();
                        } catch (Throwable t) {
                            errors.incrementAndGet();
                        }
                        if ((i & 255) == 0) Thread.yield();
                    }
                } finally {
                    activeProducers.decrementAndGet();
                    latch.countDown();
                }
            }, "Producer-" + p).start();
        }

        // Start consumer
        new Thread(() -> {
            try {
                while (activeProducers.get() > 0 || !queue.isEmpty()) {
                    try {
                        MockBlockEntity be = queue.poll();
                        if (be != null) {
                            totalConsumed.incrementAndGet();
                        }
                    } catch (Throwable t) {
                        errors.incrementAndGet();
                    }
                }
            } finally {
                latch.countDown();
            }
        }, "RenderThread-Consumer").start();

        try {
            boolean done = latch.await(15, TimeUnit.SECONDS);
            if (!done) {
                System.out.println("  TIMED OUT waiting for threads!");
                return false;
            }
        } catch (InterruptedException e) {
            return false;
        }

        int produced = totalProduced.get();
        int consumed = totalConsumed.get();
        int err = errors.get();

        System.out.printf("  Produced: %d, Consumed: %d, Errors: %d\n", produced, consumed, err);
        boolean pass = (produced == TOTAL_OPS && consumed == TOTAL_OPS && err == 0);
        System.out.println("  Result: " + (pass ? "PASS" : "FAIL"));
        return pass;
    }
}
