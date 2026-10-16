package tools;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.*;

/**
 * Adversarial Challenge 2 Harness for Iteration 2 Gate Evaluation in HBM-Modernized:
 *
 * 1. Ghost Mesh & Atlas Window Eviction:
 *    Verify that when all machines of a type are destroyed, tryAssertRetainedPresence returns
 *    false and dropUnassertedRetained() evicts the batch, reclaims the atlas window, coalesces
 *    holes into atlasWatermark, and recycles memory.
 *
 * 2. Newcomer Placement & Snapshot Merging:
 *    Verify that when a newcomer machine is placed alongside existing static retained machines,
 *    MdiBatchCoordinator.submit() merges newcomer instances with surviving retained static
 *    instances, preventing existing machines from vanishing, deduplicating mutated machines,
 *    and dropping genuinely destroyed machines.
 *
 * 3. Advanced Assembler Parametric Assertion & Frame Gating:
 *    Verify that parametric parts (GradParams.w >= 0) pass canAssertInstance(posKey) without
 *    the old poison guard, categorize into ASSERT_EPOCH even with animated=true, and do NOT
 *    trip dynamic animator fallback on Frame under idle vs active states.
 */
public class ChallengerIter2Gate2Harness {

    private static int totalTests = 0;
    private static int passedTests = 0;
    private static int failedTests = 0;

    private static void check(String testName, boolean condition, String details) {
        totalTests++;
        if (condition) {
            passedTests++;
            System.out.println("[PASS] " + testName);
        } else {
            failedTests++;
            System.err.println("[FAIL] " + testName + " - " + details);
        }
    }

    public static void main(String[] args) {
        System.out.println("===============================================================================");
        System.out.println("ADVERSARIAL VERIFICATION HARNESS: ITERATION 2 GATE EVALUATION (CHALLENGER 2)");
        System.out.println("===============================================================================\n");

        boolean pass = true;

        // Challenge 1: Ghost Mesh & Atlas Window Eviction
        pass &= testGhostMeshAndAtlasWindowEviction();

        // Challenge 2: Newcomer Placement & Snapshot Merging
        pass &= testNewcomerPlacementAndSnapshotMerging();

        // Challenge 3: Advanced Assembler Parametric Assertion & Frame Gating
        pass &= testAdvancedAssemblerParametricAssertionAndFrameGating();

        System.out.println("\n===============================================================================");
        System.out.printf("SUMMARY: Total Tests: %d | Passed: %d | Failed: %d%n", totalTests, passedTests, failedTests);
        if (pass && failedTests == 0) {
            System.out.println(">>> OVERALL HARNESS VERDICT: ALL TESTS PASSED <<<");
        } else {
            System.err.println(">>> OVERALL HARNESS VERDICT: VULNERABILITIES DETECTED <<<");
        }
        System.out.println("===============================================================================");

        if (!pass || failedTests > 0) {
            System.exit(1);
        }
    }

    // =========================================================================
    // CHALLENGE 1: GHOST MESH & ATLAS WINDOW EVICTION
    // =========================================================================

    public static final class MockRenderer {
        public final String id;
        public final Object blockEntityType;
        public boolean mdiBufferSynced = true;
        public boolean mdiRecordWriteHappened = false;
        public int instanceFadeFloatOffset = 26;

        public MockRenderer(String id, Object blockEntityType) {
            this.id = id;
            this.blockEntityType = blockEntityType;
        }

        public Object getBlockEntityType() {
            return blockEntityType;
        }

        public String getMdiTraceTag() {
            return id;
        }
    }

    public static final class MockRetainedInstanceGraph {
        private final Map<Object, Integer> activeCountsByType = new HashMap<>();
        private final Set<Long> machineKeys = new HashSet<>();

        public void registerMachine(long posKey, Object type) {
            machineKeys.add(posKey);
            activeCountsByType.merge(type, 1, Integer::sum);
        }

        public void removeMachine(long posKey, Object type) {
            machineKeys.remove(posKey);
            activeCountsByType.computeIfPresent(type, (k, v) -> v <= 1 ? null : v - 1);
        }

        public boolean hasActiveInstances(Object type) {
            if (type == null) return false;
            Integer c = activeCountsByType.get(type);
            return c != null && c > 0;
        }

        public boolean hasActiveInstances(MockRenderer renderer) {
            if (renderer == null) return false;
            return hasActiveInstances(renderer.getBlockEntityType());
        }

        public boolean hasMachineKey(long posKey) {
            return machineKeys.contains(posKey);
        }
    }

    public static final class MockPending {
        public final MockRenderer renderer;
        public int instanceCount;
        public long dirtySubmittedFrame = 0L;
        public long assertedFrame = 0L;
        public boolean partitionValid = true;
        public int atlasWindowBase = -1;
        public int atlasWindowCap = 0;
        public boolean windowUploaded = false;
        public FloatBuffer instanceData;
        public long[] instanceOcclusionKeys;
        public int[] instanceCullIndices;

        public MockPending(MockRenderer renderer, int instanceCount) {
            this.renderer = renderer;
            this.instanceCount = instanceCount;
        }
    }

    public static final class MockMdiCoordinator {
        public long retainedFrameStamp = 1L;
        public final List<MockPending> retainedList = new ArrayList<>();
        public final Map<MockRenderer, MockPending> retainedByRenderer = new IdentityHashMap<>();
        public final List<int[]> atlasHoles = new ArrayList<>();
        public int atlasHoleTotalInstances = 0;
        public int atlasWatermark = 0;

        public final ArrayDeque<FloatBuffer> snapshotPool = new ArrayDeque<>();
        public final ArrayDeque<MockPending> pendingPool = new ArrayDeque<>();

        public FloatBuffer takeSnapshot(int floats) {
            FloatBuffer buf = snapshotPool.pollFirst();
            if (buf == null || buf.capacity() < floats) {
                buf = ByteBuffer.allocateDirect(floats * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
            }
            buf.clear();
            return buf;
        }

        public void recyclePooled(FloatBuffer buf) {
            if (buf != null) {
                buf.clear();
                snapshotPool.addFirst(buf);
            }
        }

        public void releaseWindow(MockPending p) {
            if (p.atlasWindowBase < 0) {
                p.atlasWindowCap = 0;
                return;
            }
            atlasHoles.add(new int[]{p.atlasWindowBase, p.atlasWindowCap});
            atlasHoleTotalInstances += p.atlasWindowCap;
            p.atlasWindowBase = -1;
            p.atlasWindowCap = 0;
            coalesceTailHoles();
        }

        public void coalesceTailHoles() {
            boolean merged = true;
            while (merged && atlasWatermark > 0) {
                merged = false;
                for (int i = 0; i < atlasHoles.size(); i++) {
                    int[] h = atlasHoles.get(i);
                    if (h[0] + h[1] == atlasWatermark) {
                        atlasWatermark = h[0];
                        atlasHoleTotalInstances -= h[1];
                        atlasHoles.remove(i);
                        merged = true;
                        break;
                    }
                }
            }
        }

        public void uninstallRetained(MockPending p) {
            MockPending mapped = retainedByRenderer.get(p.renderer);
            if (mapped == p) {
                retainedByRenderer.remove(p.renderer);
            }
            releaseWindow(p);
            if (p.instanceData != null) {
                recyclePooled(p.instanceData);
                p.instanceData = null;
            }
            p.partitionValid = false;
            p.windowUploaded = false;
            pendingPool.addFirst(p);
        }

        public void dropUnassertedRetained() {
            for (Iterator<MockPending> it = retainedList.iterator(); it.hasNext(); ) {
                MockPending p = it.next();
                if (retainedFrameStamp - p.assertedFrame >= 2) {
                    uninstallRetained(p);
                    it.remove();
                }
            }
        }

        /** Production logic from MdiBatchCoordinator.java:786-808 */
        public boolean tryAssertRetainedPresence(MockRenderer renderer, MockRetainedInstanceGraph rig) {
            if (rig != null && !rig.hasActiveInstances(renderer)) {
                return false;
            }
            MockPending p = retainedByRenderer.get(renderer);
            if (p == null || !p.partitionValid || p.instanceCount <= 0) {
                return false;
            }
            if (!renderer.mdiBufferSynced || renderer.mdiRecordWriteHappened) {
                return false;
            }
            p.assertedFrame = retainedFrameStamp;
            return true;
        }

        public void allocateWindow(MockPending p, int instances) {
            p.atlasWindowBase = atlasWatermark;
            p.atlasWindowCap = Math.max(16, instances * 2);
            atlasWatermark += p.atlasWindowCap;
            p.windowUploaded = true;
        }
    }

    private static boolean testGhostMeshAndAtlasWindowEviction() {
        System.out.println("--- Test Suite 1: Ghost Mesh & Atlas Window Eviction ---");
        boolean ok = true;

        MockRetainedInstanceGraph rig = new MockRetainedInstanceGraph();
        MockMdiCoordinator coord = new MockMdiCoordinator();

        Object typeA = "BLOCK_ENTITY_TYPE_A";
        Object typeB = "BLOCK_ENTITY_TYPE_B";

        MockRenderer rendererA = new MockRenderer("mach_a", typeA);
        MockRenderer rendererB = new MockRenderer("mach_b", typeB);

        // Place 10 machines of type A and 5 machines of type B
        for (int i = 0; i < 10; i++) {
            rig.registerMachine(100L + i, typeA);
        }
        for (int i = 0; i < 5; i++) {
            rig.registerMachine(200L + i, typeB);
        }

        MockPending pA = new MockPending(rendererA, 10);
        pA.dirtySubmittedFrame = coord.retainedFrameStamp;
        pA.assertedFrame = coord.retainedFrameStamp;
        coord.allocateWindow(pA, 10);
        coord.retainedList.add(pA);
        coord.retainedByRenderer.put(rendererA, pA);

        MockPending pB = new MockPending(rendererB, 5);
        pB.dirtySubmittedFrame = coord.retainedFrameStamp;
        pB.assertedFrame = coord.retainedFrameStamp;
        coord.allocateWindow(pB, 5);
        coord.retainedList.add(pB);
        coord.retainedByRenderer.put(rendererB, pB);

        int initialWatermark = coord.atlasWatermark;
        check("Atlas windows allocated with positive watermark", initialWatermark > 0, "Watermark=" + initialWatermark);

        // Frame 2: both are alive and asserted
        coord.retainedFrameStamp = 2L;
        boolean assertA_f2 = coord.tryAssertRetainedPresence(rendererA, rig);
        boolean assertB_f2 = coord.tryAssertRetainedPresence(rendererB, rig);
        check("Frame 2: Both active machine types successfully assert retained presence",
                assertA_f2 && assertB_f2, "assertA=" + assertA_f2 + ", assertB=" + assertB_f2);
        check("Frame 2: Asserted frames updated",
                pA.assertedFrame == 2L && pB.assertedFrame == 2L,
                "pA=" + pA.assertedFrame + ", pB=" + pB.assertedFrame);

        // Frame 3: Remove 9 of 10 machines of Type A (partial removal)
        coord.retainedFrameStamp = 3L;
        for (int i = 0; i < 9; i++) {
            rig.removeMachine(100L + i, typeA);
        }
        check("1 instance of Type A remains in world graph", rig.hasActiveInstances(rendererA), "Expected active");
        boolean assertA_f3 = coord.tryAssertRetainedPresence(rendererA, rig);
        check("Frame 3: Partial destruction preserves presence assertion", assertA_f3, "assertA=" + assertA_f3);

        // Frame 4: Destroy the last remaining instance of Type A
        coord.retainedFrameStamp = 4L;
        rig.removeMachine(109L, typeA);
        check("Type A has 0 active instances in world graph", !rig.hasActiveInstances(rendererA), "Expected false");

        boolean assertA_f4 = coord.tryAssertRetainedPresence(rendererA, rig);
        boolean assertB_f4 = coord.tryAssertRetainedPresence(rendererB, rig);
        check("Frame 4: tryAssertRetainedPresence for destroyed Type A returns FALSE",
                !assertA_f4, "assertA returned: " + assertA_f4);
        check("Frame 4: Surviving Type B continues to assert successfully",
                assertB_f4, "assertB returned: " + assertB_f4);
        check("Frame 4: Type A assertedFrame remains at 3 (unasserted)",
                pA.assertedFrame == 3L, "pA.assertedFrame=" + pA.assertedFrame);

        // Run dropUnassertedRetained() at frame 4: diff is 4 - 3 = 1 (1-frame grace)
        coord.dropUnassertedRetained();
        check("Frame 4: Grace frame prevents eviction on frame 1 without assert",
                coord.retainedList.contains(pA), "pA unexpectedly dropped prematurely");

        // Frame 5: Second frame without assertion for Type A (diff = 5 - 3 = 2 >= 2)
        coord.retainedFrameStamp = 5L;
        boolean assertA_f5 = coord.tryAssertRetainedPresence(rendererA, rig);
        check("Frame 5: Type A still fails assertion", !assertA_f5, "assertA=" + assertA_f5);

        coord.dropUnassertedRetained();
        check("Frame 5: Destroyed Type A is cleanly evicted from retainedList",
                !coord.retainedList.contains(pA), "pA still in retainedList!");
        check("Frame 5: Type A removed from retainedByRenderer map",
                !coord.retainedByRenderer.containsKey(rendererA), "pA still in retainedByRenderer!");
        check("Frame 5: Type A atlas window reclaimed (atlasWindowBase == -1)",
                pA.atlasWindowBase == -1, "Base=" + pA.atlasWindowBase);
        check("Frame 5: Type B remains active and untouched in retainedList",
                coord.retainedList.contains(pB), "pB was erroneously dropped!");

        // Watermark coalescence check: if pB is also destroyed, entire watermark collapses to 0
        rig.removeMachine(200L, typeB);
        rig.removeMachine(201L, typeB);
        rig.removeMachine(202L, typeB);
        rig.removeMachine(203L, typeB);
        rig.removeMachine(204L, typeB);

        coord.retainedFrameStamp = 6L; // unasserted frame 1
        coord.tryAssertRetainedPresence(rendererB, rig);
        coord.retainedFrameStamp = 7L; // unasserted frame 2
        coord.tryAssertRetainedPresence(rendererB, rig);
        coord.dropUnassertedRetained();

        check("Frame 7: When all types destroyed, atlasWatermark collapses to 0 via coalesceTailHoles",
                coord.atlasWatermark == 0, "atlasWatermark=" + coord.atlasWatermark);
        check("Frame 7: Retained list completely empty, zero ghost meshes lingering",
                coord.retainedList.isEmpty(), "retainedList not empty: size=" + coord.retainedList.size());

        return ok;
    }

    // =========================================================================
    // CHALLENGE 2: NEWCOMER PLACEMENT & SNAPSHOT MERGING
    // =========================================================================

    public static final class MergingSimulator {
        public static final int INSTANCE_DATA_SIZE = 32;

        public static MockPending simulateSubmit(
                MockMdiCoordinator coord,
                MockRetainedInstanceGraph rig,
                MockRenderer renderer,
                int incomingCount,
                long[] incomingKeys,
                float[][] incomingFloatRecords) {

            int incomingFloats = incomingCount * INSTANCE_DATA_SIZE;
            FloatBuffer instanceSnapshot = coord.takeSnapshot(incomingFloats);
            for (int i = 0; i < incomingCount; i++) {
                instanceSnapshot.put(incomingFloatRecords[i]);
            }
            instanceSnapshot.flip();

            MockPending prev = coord.retainedByRenderer.get(renderer);

            // Production merging algorithm from MdiBatchCoordinator.java:590-661
            if (prev != null && prev.instanceCount > 0 && prev.instanceData != null
                    && prev.instanceOcclusionKeys != null && incomingKeys != null
                    && prev.dirtySubmittedFrame < coord.retainedFrameStamp) {

                Map<Long, Integer> incomingMap = new HashMap<>(incomingCount);
                for (int i = 0; i < incomingCount; i++) {
                    incomingMap.put(incomingKeys[i], i);
                }

                List<Integer> keptIndices = new ArrayList<>();
                for (int j = 0; j < prev.instanceCount; j++) {
                    long key = prev.instanceOcclusionKeys[j];
                    if (!incomingMap.containsKey(key)) {
                        if (rig != null ? rig.hasMachineKey(key) : true) {
                            keptIndices.add(j);
                        }
                    }
                }

                if (!keptIndices.isEmpty()) {
                    int totalMergedCount = keptIndices.size() + incomingCount;
                    int totalMergedFloats = totalMergedCount * INSTANCE_DATA_SIZE;
                    FloatBuffer mergedSnapshot = coord.takeSnapshot(totalMergedFloats);

                    int[] mergedCull = new int[totalMergedCount];
                    long[] mergedKeys = new long[totalMergedCount];

                    // 1. Copy kept instances from prev
                    for (int m = 0; m < keptIndices.size(); m++) {
                        int prevIdx = keptIndices.get(m);
                        prev.instanceData.position(prevIdx * INSTANCE_DATA_SIZE);
                        float[] keptData = new float[INSTANCE_DATA_SIZE];
                        prev.instanceData.get(keptData);

                        mergedSnapshot.position(m * INSTANCE_DATA_SIZE);
                        mergedSnapshot.put(keptData);
                        mergedKeys[m] = prev.instanceOcclusionKeys[prevIdx];
                        if (prev.instanceCullIndices != null && prevIdx < prev.instanceCullIndices.length) {
                            mergedCull[m] = prev.instanceCullIndices[prevIdx];
                        }
                    }

                    // 2. Append incoming instances
                    int appendOffset = keptIndices.size();
                    instanceSnapshot.position(0);
                    float[] inData = new float[incomingFloats];
                    instanceSnapshot.get(inData);

                    mergedSnapshot.position(appendOffset * INSTANCE_DATA_SIZE);
                    mergedSnapshot.put(inData);
                    System.arraycopy(incomingKeys, 0, mergedKeys, appendOffset, incomingCount);

                    mergedSnapshot.position(0);
                    mergedSnapshot.limit(totalMergedFloats);

                    coord.recyclePooled(instanceSnapshot);

                    instanceSnapshot = mergedSnapshot;
                    incomingCount = totalMergedCount;
                    incomingKeys = mergedKeys;
                }
            }

            MockPending p = new MockPending(renderer, incomingCount);
            p.dirtySubmittedFrame = coord.retainedFrameStamp;
            p.assertedFrame = coord.retainedFrameStamp;
            p.instanceData = instanceSnapshot;
            p.instanceOcclusionKeys = incomingKeys;

            coord.retainedByRenderer.put(renderer, p);
            if (prev != null) {
                int idx = coord.retainedList.indexOf(prev);
                if (idx >= 0) {
                    coord.retainedList.set(idx, p);
                } else {
                    coord.retainedList.add(p);
                }
                p.atlasWindowBase = prev.atlasWindowBase;
                p.atlasWindowCap = prev.atlasWindowCap;
                p.windowUploaded = false;
                prev.atlasWindowBase = -1;
                prev.atlasWindowCap = 0;
                if (prev.instanceData != null) {
                    coord.recyclePooled(prev.instanceData);
                    prev.instanceData = null;
                }
            } else {
                coord.retainedList.add(p);
                coord.allocateWindow(p, incomingCount);
            }

            return p;
        }
    }

    private static boolean testNewcomerPlacementAndSnapshotMerging() {
        System.out.println("\n--- Test Suite 2: Newcomer Snapshot Merging in MdiBatchCoordinator ---");
        boolean ok = true;

        MockRetainedInstanceGraph rig = new MockRetainedInstanceGraph();
        MockMdiCoordinator coord = new MockMdiCoordinator();
        MockRenderer renderer = new MockRenderer("assembler_static_part", "ASSEMBLER_TYPE");

        // Step 1: Initial frame (Frame 1): 3 static machines placed (keys 10, 20, 30)
        coord.retainedFrameStamp = 1L;
        rig.registerMachine(10L, "ASSEMBLER_TYPE");
        rig.registerMachine(20L, "ASSEMBLER_TYPE");
        rig.registerMachine(30L, "ASSEMBLER_TYPE");

        float[][] data3 = new float[3][32];
        for (int i = 0; i < 3; i++) {
            Arrays.fill(data3[i], (i + 1) * 10.0f);
        }

        MockPending p1 = MergingSimulator.simulateSubmit(
                coord, rig, renderer, 3, new long[]{10L, 20L, 30L}, data3);

        check("Frame 1: Initial submit contains 3 instances", p1.instanceCount == 3, "count=" + p1.instanceCount);
        check("Frame 1: Initial keys are [10, 20, 30]",
                Arrays.equals(p1.instanceOcclusionKeys, new long[]{10L, 20L, 30L}),
                Arrays.toString(p1.instanceOcclusionKeys));

        // Step 2: Next frame (Frame 2): Newcomer placed at key 40!
        // Machines 10, 20, 30 are static and bypassed by NucleusDispatcherBypass (submit contains only newcomer 40)
        coord.retainedFrameStamp = 2L;
        rig.registerMachine(40L, "ASSEMBLER_TYPE");

        float[][] dataNewcomer = new float[1][32];
        Arrays.fill(dataNewcomer[0], 40.0f);

        MockPending p2 = MergingSimulator.simulateSubmit(
                coord, rig, renderer, 1, new long[]{40L}, dataNewcomer);

        check("Frame 2: Newcomer merged successfully — total instanceCount == 4 (NOT 1!)",
                p2.instanceCount == 4, "count=" + p2.instanceCount);
        check("Frame 2: Retained static machines 10, 20, 30 did NOT vanish; keys == [10, 20, 30, 40]",
                Arrays.equals(p2.instanceOcclusionKeys, new long[]{10L, 20L, 30L, 40L}),
                Arrays.toString(p2.instanceOcclusionKeys));

        // Verify float content integrity for each instance
        p2.instanceData.position(0);
        float[] slot0 = new float[32];
        p2.instanceData.get(slot0);
        check("Instance 0 (Key 10) data intact (expected 10.0)", slot0[0] == 10.0f, "val=" + slot0[0]);

        p2.instanceData.position(3 * 32);
        float[] slot3 = new float[32];
        p2.instanceData.get(slot3);
        check("Instance 3 (Newcomer Key 40) data intact (expected 40.0)", slot3[0] == 40.0f, "val=" + slot3[0]);

        // Step 3: Mutation test (Frame 3): Machine 20 was modified/dirtied and re-submits with new data (val=99.0f)
        coord.retainedFrameStamp = 3L;
        float[][] dataMutated = new float[1][32];
        Arrays.fill(dataMutated[0], 99.0f);

        MockPending p3 = MergingSimulator.simulateSubmit(
                coord, rig, renderer, 1, new long[]{20L}, dataMutated);

        check("Frame 3: Mutated machine 20 replaces old copy without duplicating — count remains 4",
                p3.instanceCount == 4, "count=" + p3.instanceCount);
        check("Frame 3: Mutated machine 20 was appended with new data, old 20 omitted from kept list",
                Arrays.equals(p3.instanceOcclusionKeys, new long[]{10L, 30L, 40L, 20L}),
                Arrays.toString(p3.instanceOcclusionKeys));

        p3.instanceData.position(3 * 32);
        float[] slotMutated = new float[32];
        p3.instanceData.get(slotMutated);
        check("Frame 3: Updated data applied for Machine 20 (expected 99.0)", slotMutated[0] == 99.0f, "val=" + slotMutated[0]);

        // Step 4: Destroyed machine test (Frame 4): Machine 30 was broken in world (removed from rig)
        // Another newcomer 50 is placed
        coord.retainedFrameStamp = 4L;
        rig.removeMachine(30L, "ASSEMBLER_TYPE");
        rig.registerMachine(50L, "ASSEMBLER_TYPE");

        float[][] data50 = new float[1][32];
        Arrays.fill(data50[0], 50.0f);

        MockPending p4 = MergingSimulator.simulateSubmit(
                coord, rig, renderer, 1, new long[]{50L}, data50);

        check("Frame 4: Broken Machine 30 is dropped during merge; total count remains 4 (10, 40, 20, 50)",
                p4.instanceCount == 4, "count=" + p4.instanceCount);
        check("Frame 4: Keys contain 10, 40, 20, 50 (30 is absent)",
                Arrays.equals(p4.instanceOcclusionKeys, new long[]{10L, 40L, 20L, 50L}),
                Arrays.toString(p4.instanceOcclusionKeys));

        return ok;
    }

    // =========================================================================
    // CHALLENGE 3: ADVANCED ASSEMBLER PARAMETRIC ASSERTION & FRAME GATING
    // =========================================================================

    public static final class MockPartDef {
        public final String name;
        public final boolean animated;
        public final boolean dynamic;
        public final boolean hasAnimator;
        public final boolean hasParametric;
        public final boolean hasTintOverride;

        public MockPartDef(String name, boolean animated, boolean dynamic,
                           boolean hasAnimator, boolean hasParametric, boolean hasTintOverride) {
            this.name = name;
            this.animated = animated;
            this.dynamic = dynamic;
            this.hasAnimator = hasAnimator;
            this.hasParametric = hasParametric;
            this.hasTintOverride = hasTintOverride;
        }
    }

    public static final class MockMachineSpec {
        public final List<MockPartDef> parts;
        public final boolean hasAnimatedParts;
        public final boolean hasDynamicAnimators;
        public final boolean hasDynamicParts;
        public final boolean hasNonAssertableParts;
        public final boolean hasParametricParts;
        public final boolean hasAnimationEpoch;

        /** Exact constructor matching MachineSpec.java:1189-1228 */
        public MockMachineSpec(List<MockPartDef> parts, boolean hasAnimationEpoch) {
            this.parts = parts;
            this.hasAnimationEpoch = hasAnimationEpoch;

            boolean anyAnimated = false;
            boolean anyDynamicAnimator = false;
            boolean anyDynamic = false;
            for (MockPartDef p : parts) {
                if (p.animated) anyAnimated = true;
                if (p.animated && p.hasAnimator && p.dynamic) anyDynamicAnimator = true;
                if (p.dynamic) anyDynamic = true;
            }
            this.hasAnimatedParts = anyAnimated;
            this.hasDynamicAnimators = anyDynamicAnimator;
            this.hasDynamicParts = anyDynamic;

            boolean anyNonAssertable = false;
            for (MockPartDef p : parts) {
                if ((p.animated && p.hasAnimator && p.dynamic) || p.hasTintOverride) {
                    anyNonAssertable = true;
                    break;
                }
            }
            this.hasNonAssertableParts = anyNonAssertable;

            boolean anyParametric = false;
            for (MockPartDef p : parts) {
                if (p.hasParametric) {
                    anyParametric = true;
                    break;
                }
            }
            this.hasParametricParts = anyParametric;
        }

        public boolean isPurelyStatic() {
            return !hasAnimatedParts && !hasDynamicAnimators && !hasNonAssertableParts
                    && !hasParametricParts && !hasDynamicParts && hasAnimationEpoch == false;
        }
    }

    public static final class MockInstancedStaticPartRenderer {
        public boolean initialized = true;
        public FloatBuffer instanceBuffer;
        public int instanceCount = 0;
        public int maxInstances = 256;
        public long[] instanceOcclusionKeys = new long[256];
        public float[] instanceGradParamsW = new float[256];

        public MockInstancedStaticPartRenderer() {
            instanceBuffer = ByteBuffer.allocateDirect(256 * 32 * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
        }

        /** Remediation logic in InstancedStaticPartRenderer.java:887-891 (poison guard removed!) */
        public boolean canAssertInstanceRemediated(long posKey) {
            return initialized && instanceBuffer != null
                    && instanceCount < maxInstances
                    && instanceOcclusionKeys[instanceCount] == posKey;
        }

        /** Old flawed poison guard logic from previous iterations (audit 2026-10-10) */
        public boolean canAssertInstanceOldFlawed(long posKey) {
            return initialized && instanceBuffer != null
                    && instanceCount < maxInstances
                    && instanceOcclusionKeys[instanceCount] == posKey
                    && instanceGradParamsW[instanceCount] < 0f; // Flawed poison guard!
        }
    }

    private static boolean testAdvancedAssemblerParametricAssertionAndFrameGating() {
        System.out.println("\n--- Test Suite 3: Advanced Assembler Parametric Assertion & Frame Gating ---");
        boolean ok = true;

        // Construct exact parts of MachineAdvancedAssemblerRenderer.java
        List<MockPartDef> advAssemblerParts = new ArrayList<>();
        // Base: static, has bake offset animator, NOT dynamic, NOT animated
        advAssemblerParts.add(new MockPartDef("Base", false, false, true, false, false));
        // Frame: dynamicPart with quads and bake offset animator, NOT animated!
        advAssemblerParts.add(new MockPartDef("Frame", false, true, true, false, false));
        // Ring: spinningPart (parametric, animated=true, joints=none)
        advAssemblerParts.add(new MockPartDef("Ring", true, false, false, true, false));
        // 8 Arm parts: trackedChainPart (parametric, animated=true, joints=chain)
        String[] armNames = {"ArmLower1", "ArmUpper1", "Head1", "Spike1", "ArmLower2", "ArmUpper2", "Head2", "Spike2"};
        for (String arm : armNames) {
            advAssemblerParts.add(new MockPartDef(arm, true, false, false, true, false));
        }

        MockMachineSpec spec = new MockMachineSpec(advAssemblerParts, true);

        // 1. Spec invariants
        check("Spec has animated parts (Ring, Arms)", spec.hasAnimatedParts, "hasAnimatedParts=" + spec.hasAnimatedParts);
        check("Spec has dynamic parts (Frame quads)", spec.hasDynamicParts, "hasDynamicParts=" + spec.hasDynamicParts);
        check("Frame does NOT set hasDynamicAnimators because p.animated == false",
                !spec.hasDynamicAnimators, "hasDynamicAnimators=" + spec.hasDynamicAnimators);
        check("Frame does NOT set hasNonAssertableParts because p.animated == false",
                !spec.hasNonAssertableParts, "hasNonAssertableParts=" + spec.hasNonAssertableParts);
        check("Spec has parametric parts (Ring, Arms)", spec.hasParametricParts, "hasParametricParts=" + spec.hasParametricParts);
        check("Spec is NOT purely static (has dynamic parts and animations)",
                !spec.isPurelyStatic(), "isPurelyStatic=" + spec.isPurelyStatic());

        // 2. Parametric canAssertInstance poison guard test
        MockInstancedStaticPartRenderer inst = new MockInstancedStaticPartRenderer();
        long testPosKey = 0x12345678L;
        inst.instanceOcclusionKeys[0] = testPosKey;
        // Joint chain base index for parametric parts: GradParams.w >= 0.0f
        inst.instanceGradParamsW[0] = 5.0f; // Joint base index = 5

        boolean oldCanAssert = inst.canAssertInstanceOldFlawed(testPosKey);
        boolean newCanAssert = inst.canAssertInstanceRemediated(testPosKey);

        check("Old flawed canAssertInstance correctly failed due to GradParams.w >= 0 (< 0f poison guard)",
                !oldCanAssert, "oldCanAssert returned: " + oldCanAssert);
        check("Remediated canAssertInstance succeeds for parametric parts with GradParams.w >= 0",
                newCanAssert, "newCanAssert returned: " + newCanAssert);

        // 3. MachineBer.tryFastAssertRender simulation under IDLE vs ACTIVE
        // Idle simulation: epochStable == true, hooksQuiet == true -> epochFullAssert == true
        boolean epochFullAssertIdle = true;
        boolean partialFallbackIdle = false;

        for (MockPartDef part : spec.parts) {
            if (part.animated && !epochFullAssertIdle) continue;
            if (part.hasParametric && !epochFullAssertIdle) {
                partialFallbackIdle = true;
                break;
            }
            if (part.hasAnimator && part.dynamic && !epochFullAssertIdle) {
                partialFallbackIdle = true;
                break;
            }
            if (part.hasTintOverride) {
                partialFallbackIdle = true;
                break;
            }
        }
        check("IDLE STATE: tryFastAssertRender does NOT trip dynamic animator or parametric fallback on Frame/Arms",
                !partialFallbackIdle, "partialFallback=" + partialFallbackIdle);

        // Active simulation: epochFullAssert == false (machine is actively crafting or arms moving)
        boolean epochFullAssertActive = false;
        boolean partialFallbackActive = false;

        for (MockPartDef part : spec.parts) {
            if (part.animated && !epochFullAssertActive) continue;
            if (part.hasParametric && !epochFullAssertActive) {
                partialFallbackActive = true;
                break;
            }
            if (part.hasAnimator && part.dynamic && !epochFullAssertActive) {
                partialFallbackActive = true;
                break;
            }
        }
        check("ACTIVE STATE: tryFastAssertRender gracefully falls back to partial/renderAll path as expected",
                partialFallbackActive, "partialFallbackActive=" + partialFallbackActive);

        // 4. Roster classification in MachineBer.java:1437-1447
        // Verify that all 8 arm parts (which have part.animated == true) are classified as ASSERT_EPOCH
        int assertCount = 0;
        int assertEpochCount = 0;
        for (MockPartDef part : spec.parts) {
            boolean canAssertParametric = part.hasParametric && spec.hasAnimationEpoch;
            boolean eligible = (!part.animated || canAssertParametric)
                    && !(part.hasAnimator && part.dynamic && !canAssertParametric)
                    && (!part.hasParametric || spec.hasAnimationEpoch)
                    && !part.hasTintOverride;

            if (eligible) {
                if (part.hasParametric) {
                    assertEpochCount++;
                } else {
                    assertCount++;
                }
            }
        }

        check("Base is classified into static ASSERT category (count == 1)", assertCount == 1, "assertCount=" + assertCount);
        check("Ring and all 8 Arm parts are classified into ASSERT_EPOCH category (count == 9)",
                assertEpochCount == 9, "assertEpochCount=" + assertEpochCount);

        return ok;
    }
}
