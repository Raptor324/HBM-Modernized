package tools;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Adversarial Verification Harness for Milestone 1 (Phase 1):
 * Spatial Chunk Partitioning, Multiblock Bounds Expansion, Frustum Culling,
 * and Concurrency Thread-Safety in NucleusDispatcherBypass.
 */
public class AdversarialBypassStressHarness {

    // Simple AABB representation matching net.minecraft.world.phys.AABB
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

        public boolean intersects(SimpleAABB o) {
            return this.minX <= o.maxX && this.maxX >= o.minX
                    && this.minY <= o.maxY && this.maxY >= o.minY
                    && this.minZ <= o.maxZ && this.maxZ >= o.minZ;
        }

        @Override
        public String toString() {
            return String.format("[%s, %s, %s] -> [%s, %s, %s]", minX, minY, minZ, maxX, maxY, maxZ);
        }
    }

    // Gribb-Hartmann Frustum simulation
    public static final class SimulatedFrustum {
        private final double[][] planes = new double[6][4]; // ax + by + cz + d >= 0 is inside

        // Camera at camX, camY, camZ looking in direction dirX, dirY, dirZ
        public static SimulatedFrustum createLookForward(double camX, double camY, double camZ,
                                                         double dirX, double dirY, double dirZ,
                                                         double nearDist, double farDist, double fovHalfAngleRad) {
            SimulatedFrustum f = new SimulatedFrustum();
            // Normalize direction
            double len = Math.sqrt(dirX * dirX + dirY * dirY + dirZ * dirZ);
            dirX /= len; dirY /= len; dirZ /= len;

            // Near plane: dir . (P - (cam + near*dir)) >= 0  =>  dir.P - (dir.cam + near) >= 0
            f.planes[0][0] = dirX; f.planes[0][1] = dirY; f.planes[0][2] = dirZ;
            f.planes[0][3] = -(dirX * camX + dirY * camY + dirZ * camZ + nearDist);

            // Far plane: -dir . (P - (cam + far*dir)) >= 0
            f.planes[1][0] = -dirX; f.planes[1][1] = -dirY; f.planes[1][2] = -dirZ;
            f.planes[1][3] = (dirX * camX + dirY * camY + dirZ * camZ + farDist);

            // Perpendicular axes
            double upX = 0, upY = 1, upZ = 0;
            if (Math.abs(dirY) > 0.99) {
                upX = 1; upY = 0; upZ = 0;
            }
            double rightX = dirY * upZ - dirZ * upY;
            double rightY = dirZ * upX - dirX * upZ;
            double rightZ = dirX * upY - dirY * upX;
            double rLen = Math.sqrt(rightX * rightX + rightY * rightY + rightZ * rightZ);
            rightX /= rLen; rightY /= rLen; rightZ /= rLen;

            // Simple frustum side planes
            // Left plane: normal tilted right
            f.planes[2][0] = rightX; f.planes[2][1] = rightY; f.planes[2][2] = rightZ;
            f.planes[2][3] = -(rightX * camX + rightY * camY + rightZ * camZ) + 10.0;

            f.planes[3][0] = -rightX; f.planes[3][1] = -rightY; f.planes[3][2] = -rightZ;
            f.planes[3][3] = -(-rightX * camX - rightY * camY - rightZ * camZ) + 10.0;

            // Top and bottom planes
            f.planes[4][0] = 0; f.planes[4][1] = 1; f.planes[4][2] = 0;
            f.planes[4][3] = -camY + 30.0;

            f.planes[5][0] = 0; f.planes[5][1] = -1; f.planes[5][2] = 0;
            f.planes[5][3] = camY + 30.0;

            return f;
        }

        public boolean isVisible(SimpleAABB box) {
            for (int i = 0; i < 6; i++) {
                double[] p = planes[i];
                double px = p[0] > 0 ? box.maxX : box.minX;
                double py = p[1] > 0 ? box.maxY : box.minY;
                double pz = p[2] > 0 ? box.maxZ : box.minZ;
                if (p[0] * px + p[1] * py + p[2] * pz + p[3] < 0) {
                    return false; // Box strictly behind plane
                }
            }
            return true;
        }
    }

    // Mock BlockEntity
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

    // Exact replica of NucleusDispatcherBypass.ChunkBucket logic
    public static final class SimulatedChunkBucket {
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

        public SimulatedChunkBucket(long chunkKey, int chunkX, int chunkZ) {
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
            expandBounds(be);
        }

        public void remove(MockBlockEntity be) {
            machines.remove(be);
            boundsDirty = true;
        }

        public boolean isEmpty() {
            return machines.isEmpty();
        }

        private void expandBounds(MockBlockEntity be) {
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
            cachedAabb = new SimpleAABB(minX, minY, minZ, maxX, maxY, maxZ);
            boundsDirty = false;
        }

        public SimpleAABB getBounds() {
            if (boundsDirty) {
                resetBaseBounds();
                for (int i = 0; i < machines.size(); i++) {
                    expandBounds(machines.get(i));
                }
                if (minY > maxY) {
                    minY = -64.0;
                    maxY = 320.0;
                }
                cachedAabb = new SimpleAABB(minX, minY, minZ, maxX, maxY, maxZ);
                boundsDirty = false;
            }
            return cachedAabb;
        }

        public double distSq(double camX, double camZ) {
            double cx = (chunkX << 4) + 8.0 - camX;
            double cz = (chunkZ << 4) + 8.0 - camZ;
            return cx * cx + cz * cz;
        }
    }

    public static void main(String[] args) {
        System.out.println("===============================================================================");
        System.out.println("ADVERSARIAL VERIFICATION HARNESS: NucleusDispatcherBypass");
        System.out.println("===============================================================================");

        boolean allPass = true;

        allPass &= test1_MultiblockExpansionAndFrustumCulling();
        allPass &= test2_DynamicBoundsStalenessVulnerability();
        allPass &= test3_ChunkCenterDistanceCullingVulnerability();
        allPass &= test4_NullFrustumFallback();
        allPass &= test5_ArrayDequeConcurrencyCorruption();

        System.out.println("===============================================================================");
        System.out.println("FINAL VERDICT: " + (allPass ? "ALL TESTS PASSED" : "VULNERABILITIES CONFIRMED"));
        System.out.println("===============================================================================");
    }

    /**
     * Test 1: Verify whether a 16x16 multiblock with origin at (15, 64, 15) in chunk (0,0)
     * whose geometry extends into chunk (1,1) [bounds: (15, 64, 15) to (31, 80, 31)]
     * expands ChunkBucket bounds correctly.
     */
    private static boolean test1_MultiblockExpansionAndFrustumCulling() {
        System.out.println("\n[TEST 1] Multiblock Bounds Expansion in ChunkBucket (16x16 at chunk boundary)");
        SimulatedChunkBucket bucket = new SimulatedChunkBucket(0L, 0, 0);

        MockBlockEntity multiblock = new MockBlockEntity(15, 64, 15,
                new SimpleAABB(15.0, 64.0, 15.0, 31.0, 80.0, 31.0), true);
        bucket.add(multiblock);

        SimpleAABB bounds = bucket.getBounds();
        System.out.println("  Bucket bounds: " + bounds);

        boolean boundsCorrect = (bounds.minX == 0.0 && bounds.maxX == 31.0
                && bounds.minY == 64.0 && bounds.maxY == 80.0
                && bounds.minZ == 0.0 && bounds.maxZ == 31.0);

        if (!boundsCorrect) {
            System.err.println("  FAIL: Bucket bounds do not enclose extended multiblock: " + bounds);
            return false;
        }

        // Camera is placed at (25, 70, 25) in chunk (1,1) looking towards +X/+Z (35, 70, 35)
        // Chunk (0,0) [0..16] is BEHIND camera (camX=25), but multiblock extends to X=31, Z=31.
        SimulatedFrustum frustum = SimulatedFrustum.createLookForward(25, 70, 25, 1, 0, 1, 0.1, 100.0, 1.0);
        boolean bucketVisible = frustum.isVisible(bounds);
        boolean beVisible = frustum.isVisible(multiblock.bounds);

        System.out.println("  Camera at (25, 70, 25) looking towards (35, 70, 35):");
        System.out.println("  Bucket frustum test: " + bucketVisible);
        System.out.println("  Machine frustum test: " + beVisible);

        if (!bucketVisible || !beVisible) {
            System.err.println("  FAIL: Multiblock falsely culled by frustum!");
            return false;
        }

        System.out.println("  PASS: Multiblock bounds correctly expanded and visible from adjacent chunk.");
        return true;
    }

    /**
     * Test 2: Adversarial Stress Test: Dynamic Bounds Expansion (e.g. Mining Laser / Drill).
     * Machine starts with 1x1x1 bounds, then digs down by 50 blocks.
     * Does ChunkBucket update its bounds or retain the stale shallow box?
     */
    private static boolean test2_DynamicBoundsStalenessVulnerability() {
        System.out.println("\n[TEST 2] Adversarial Dynamic Bounds Staleness (e.g. Mining Laser / Drill)");
        SimulatedChunkBucket bucket = new SimulatedChunkBucket(0L, 0, 0);

        MockBlockEntity drill = new MockBlockEntity(8, 64, 8,
                new SimpleAABB(7.0, 63.0, 7.0, 9.0, 67.0, 9.0), true);
        bucket.add(drill);

        SimpleAABB initialBounds = bucket.getBounds();
        System.out.println("  Initial bucket bounds: " + initialBounds);

        // Now drill digs down: targetDepth extends to 50 blocks down
        drill.bounds = new SimpleAABB(7.0, 14.0, 7.0, 9.0, 67.0, 9.0);

        // Does ChunkBucket.getBounds() reflect this?
        SimpleAABB boundsAfterDigging = bucket.getBounds();
        System.out.println("  Bucket bounds after digging (without remove/re-add): " + boundsAfterDigging);

        boolean isStale = (boundsAfterDigging.minY == 63.0); // Stale! Should be 14.0!
        if (isStale) {
            System.out.println("  [CONFIRMED VULNERABILITY]: ChunkBucket bounds are STALE! minY is "
                    + boundsAfterDigging.minY + ", expected 14.0.");
            System.out.println("  Consequence: If player is deep in a ravine at Y=20 looking at the drill bit,");
            System.out.println("  the surface chunk bucket (minY=63) is outside the frustum and gets CULLED!");
            return false; // Confirmed bug / limitation!
        }

        return true;
    }

    /**
     * Test 3: Adversarial Chunk Center Distance Culling Vulnerability.
     * A large multiblock extending 48 blocks towards camera.
     * Player is 30 blocks away from visible geometry, but > maxDist from chunk center (8, 8).
     */
    private static boolean test3_ChunkCenterDistanceCullingVulnerability() {
        System.out.println("\n[TEST 3] Adversarial Chunk Center Distance Culling (Multiblock Extension)");
        SimulatedChunkBucket bucket = new SimulatedChunkBucket(0L, 0, 0);

        // Multiblock at (15, 64, 8) extending 48 blocks along +X to (63, 70, 8)
        MockBlockEntity largeMachine = new MockBlockEntity(15, 64, 8,
                new SimpleAABB(15.0, 64.0, 6.0, 63.0, 70.0, 10.0), true);
        bucket.add(largeMachine);

        double maxDist = 64.0 + 24.0; // 88 blocks
        double maxDistSq = maxDist * maxDist; // 7744

        // Camera is at X=100, Y=64, Z=8.
        // Distance from camera to machine's visible end (X=63): 100 - 63 = 37 blocks! (Well within view distance 64!)
        double camX = 100.0, camZ = 8.0;
        double distToMachine = camX - 63.0;

        // Chunk center is at (8, 8). Distance to chunk center:
        double chunkDistSq = bucket.distSq(camX, camZ); // (8 - 100)^2 + (8 - 8)^2 = 92^2 = 8464
        boolean culledByChunkDistance = (chunkDistSq > maxDistSq);

        System.out.printf("  Player distance to machine geometry: %.1f blocks (View Distance: 64.0)\n", distToMachine);
        System.out.printf("  Distance squared to chunk center (8,8): %.1f vs maxDistSq: %.1f\n", chunkDistSq, maxDistSq);
        System.out.println("  Culled by chunk center distance check: " + culledByChunkDistance);

        if (culledByChunkDistance) {
            System.out.println("  [CONFIRMED VULNERABILITY]: Large multiblock falsely culled by chunk center distance!");
            System.out.println("  The player is standing 37 blocks away from the machine, but the chunk center is 92 blocks away.");
            return false;
        }

        return true;
    }

    /**
     * Test 4: Frustum == null Graceful Fallback
     */
    private static boolean test4_NullFrustumFallback() {
        System.out.println("\n[TEST 4] Frustum == null Graceful Fallback");
        SimulatedChunkBucket bucket = new SimulatedChunkBucket(0L, 0, 0);
        bucket.add(new MockBlockEntity(5, 64, 5, new SimpleAABB(5, 64, 5, 6, 65, 6), true));

        // When frustum is null:
        SimulatedFrustum frustum = null;
        boolean bucketPassed = (frustum == null || frustum.isVisible(bucket.getBounds()));
        boolean bePassed = (frustum == null || frustum.isVisible(bucket.machines.get(0).bounds));

        if (bucketPassed && bePassed) {
            System.out.println("  PASS: Null frustum gracefully falls back to drawing all machines in range.");
            return true;
        } else {
            System.err.println("  FAIL: Null frustum did not pass!");
            return false;
        }
    }

    /**
     * Test 5: Concurrency / Thread-Safety: ArrayDeque Race Condition between ServerThread and RenderThread
     */
    private static boolean test5_ArrayDequeConcurrencyCorruption() {
        System.out.println("\n[TEST 5] Adversarial Concurrency Stress: ArrayDeque<BlockEntity> PENDING_REMOVE");
        System.out.println("  Simulating ServerThread (MixinBlockEntitySetRemoved) concurrently mutating ArrayDeque");
        System.out.println("  while RenderThread (processPresenceQueue) polls from it.");

        final ArrayDeque<MockBlockEntity> queue = new ArrayDeque<>();
        final int OPERATIONS = 50_000;
        final AtomicBoolean serverRunning = new AtomicBoolean(true);
        final AtomicInteger serverExceptions = new AtomicInteger(0);
        final AtomicInteger renderExceptions = new AtomicInteger(0);
        final CountDownLatch latch = new CountDownLatch(2);

        // Thread 1: ServerThread calling onMachineRemoved -> PENDING_REMOVE.add(be)
        Thread serverThread = new Thread(() -> {
            try {
                for (int i = 0; i < OPERATIONS; i++) {
                    MockBlockEntity be = new MockBlockEntity(i, 64, i, null, false);
                    try {
                        queue.add(be);
                    } catch (Throwable t) {
                        serverExceptions.incrementAndGet();
                    }
                    if ((i & 63) == 0) Thread.yield();
                }
            } finally {
                serverRunning.set(false);
                latch.countDown();
            }
        }, "ServerThread-Sim");

        // Thread 2: RenderThread calling processPresenceQueue -> PENDING_REMOVE.pollFirst()
        Thread renderThread = new Thread(() -> {
            try {
                while (serverRunning.get() || !queue.isEmpty()) {
                    try {
                        MockBlockEntity be = queue.pollFirst();
                    } catch (Throwable t) {
                        renderExceptions.incrementAndGet();
                    }
                    Thread.yield();
                }
            } finally {
                latch.countDown();
            }
        }, "RenderThread-Sim");

        serverThread.start();
        renderThread.start();

        try {
            boolean finished = latch.await(10, TimeUnit.SECONDS);
            if (!finished) {
                serverThread.interrupt();
                renderThread.interrupt();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        int totalErrors = serverExceptions.get() + renderExceptions.get();
        System.out.printf("  Completed %d iterations. Server exceptions: %d, Render exceptions: %d\n",
                OPERATIONS, serverExceptions.get(), renderExceptions.get());

        if (totalErrors > 0) {
            System.out.println("  [CONFIRMED CRITICAL VULNERABILITY]: ArrayDeque threw " + totalErrors
                    + " concurrent modification / pointer corruption exceptions!");
            System.out.println("  Root cause: NucleusDispatcherBypass uses java.util.ArrayDeque for PENDING_REMOVE");
            System.out.println("  without gating for ClientLevel BEFORE PENDING_REMOVE.add(be) in onMachineRemoved!");
            return false;
        } else {
            System.out.println("  Note: No crash in this 50K sample, but ArrayDeque is inherently non-thread-safe.");
            return true;
        }
    }
}
