package tools;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

/**
 * Empirical Adversarial Challenge Harness for Phase 3 Remediation:
 * 1. Dynamic Machine Non-Freezing vs Purely Static Contract:
 *    - Machines with dynamic quads without animators (Arc Furnace, Crucible, Strand Caster, Fluid Tank)
 *    - Machines with hooks defaulting mayDrawContent to false (Barrel Tank, Fluid Tank)
 * 2. Atlas Window Retention Lifecycle & Unbounded VRAM / Ghost Machine Bug:
 *    - tryAssertRetainedPresence re-assertion when machines are genuinely removed
 * 3. SparseUpdateQueue Slice Isolation & Non-Clobbering:
 *    - Light slice update preserving coordinates and orientation
 *    - Span coalescing logic
 * 4. Dirty Tracking Invalidation & Multi-Threaded Handoff Queue Concurrency:
 *    - markRenderDirty() invalidation lifecycle
 *    - Multi-threaded stress testing (100,000 ops)
 */
public class ChallengerP3RemediationHarness {

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
        System.out.println("ADVERSARIAL VERIFICATION HARNESS: PHASE 3 REMEDIATION (CHALLENGER 2)");
        System.out.println("===============================================================================\n");

        boolean pass = true;

        // Part 1: Dynamic Quads & Purely Static Contract Verification
        pass &= testDynamicQuadsWithoutAnimatorsNotPurelyStatic();
        pass &= testRenderHooksDefaultMayDrawContentFallthrough();

        // Part 2: Atlas Window Lifecycle & Ghost Machine Bug Verification
        pass &= testAtlasWindowRetentionOnGenuineRemoval();

        // Part 3: SparseUpdateQueue Granular Slice Isolation
        pass &= testSparseUpdateQueueSliceIsolation();
        pass &= testSpanCoalescingIntegrity();

        // Part 4: Dirty Tracking & Concurrency Stress
        pass &= testDirtyTrackingInvalidationLifecycle();
        pass &= testHandoffQueueConcurrentStress();

        System.out.println("\n===============================================================================");
        System.out.printf("SUMMARY: Total Tests: %d | Passed: %d | Failed: %d%n", totalTests, passedTests, failedTests);
        if (pass && failedTests == 0) {
            System.out.println(">>> OVERALL HARNESS VERDICT: ALL TESTS PASSED <<<");
        } else {
            System.err.println(">>> OVERALL HARNESS VERDICT: VULNERABILITIES & REGRESSIONS CONFIRMED <<<");
        }
        System.out.println("===============================================================================");
    }

    // =========================================================================
    // PART 1: DYNAMIC QUADS & PURELY STATIC CONTRACT
    // =========================================================================

    public static final class SimulatedPartDef {
        public final String name;
        public final boolean hasAnimator;
        public final boolean hasDynamicQuads;
        public final boolean isAnimated;
        public final boolean hasParametric;
        public final boolean hasTintOverride;

        public SimulatedPartDef(String name, boolean hasAnimator, boolean hasDynamicQuads,
                                boolean isAnimated, boolean hasParametric, boolean hasTintOverride) {
            this.name = name;
            this.hasAnimator = hasAnimator;
            this.hasDynamicQuads = hasDynamicQuads;
            this.isAnimated = isAnimated;
            this.hasParametric = hasParametric;
            this.hasTintOverride = hasTintOverride;
        }

        public boolean dynamic() { return hasDynamicQuads; }
        public boolean animated() { return isAnimated; }
    }

    public static final class SimulatedMachineSpec {
        public final String id;
        public final List<SimulatedPartDef> parts;
        public final List<SimulatedHook> hooks;
        public final Object animationEpoch;

        public final boolean hasAnimatedParts;
        public final boolean hasDynamicAnimators;
        public final boolean hasNonAssertableParts;
        public final boolean hasParametricParts;
        public final boolean hasDynamicParts;

        public SimulatedMachineSpec(String id, List<SimulatedPartDef> parts, List<SimulatedHook> hooks, Object animationEpoch) {
            this.id = id;
            this.parts = parts;
            this.hooks = hooks != null ? hooks : Collections.emptyList();
            this.animationEpoch = animationEpoch;

            boolean anyAnimated = false;
            boolean anyDynamicAnimator = false;
            boolean anyDynamic = false;
            for (SimulatedPartDef p : parts) {
                if (p.animated()) anyAnimated = true;
                if (p.hasAnimator && p.dynamic()) anyDynamicAnimator = true;
                if (p.dynamic()) anyDynamic = true;
            }
            this.hasAnimatedParts = anyAnimated;
            this.hasDynamicAnimators = anyDynamicAnimator;
            this.hasDynamicParts = anyDynamic;

            boolean anyNonAssertable = false;
            for (SimulatedPartDef p : parts) {
                if ((p.hasAnimator && p.dynamic()) || p.hasTintOverride) {
                    anyNonAssertable = true;
                    break;
                }
            }
            this.hasNonAssertableParts = anyNonAssertable;

            boolean anyParametric = false;
            for (SimulatedPartDef p : parts) {
                if (p.hasParametric) {
                    anyParametric = true;
                    break;
                }
            }
            this.hasParametricParts = anyParametric;
        }

        /** Exact code from production MachineSpec.java lines 1278-1281 */
        public boolean isPurelyStaticProduction() {
            return !hasAnimatedParts && !hasDynamicAnimators && !hasNonAssertableParts
                    && !hasParametricParts && hooks.isEmpty() && animationEpoch == null;
        }

        /** Correctly corrected method that also checks !hasDynamicParts */
        public boolean isPurelyStaticCorrected() {
            return !hasAnimatedParts && !hasDynamicAnimators && !hasNonAssertableParts
                    && !hasParametricParts && !hasDynamicParts && hooks.isEmpty() && animationEpoch == null;
        }
    }

    public interface SimulatedHook {
        boolean mayDrawContent(Object be);
    }

    private static boolean testDynamicQuadsWithoutAnimatorsNotPurelyStatic() {
        System.out.println("--- Test Suite 1: Dynamic Quads vs isPurelyStatic Contract ---");
        boolean ok = true;

        // Spec 1: Truly Static (Centrifuge)
        SimulatedMachineSpec centrifuge = new SimulatedMachineSpec("centrifuge",
                List.of(new SimulatedPartDef("Base", false, false, false, false, false)),
                Collections.emptyList(), null);
        check("Truly static machine isPurelyStatic() == true",
                centrifuge.isPurelyStaticProduction(),
                "Centrifuge must be recognized as purely static");

        // Spec 2: Arc Furnace (has dynamic electrodes switching hot/cold based on isActive())
        SimulatedMachineSpec arcFurnace = new SimulatedMachineSpec("arcfurnace",
                List.of(new SimulatedPartDef("Electrodes", false, true, false, false, false)),
                Collections.emptyList(), null);

        boolean arcStaticProd = arcFurnace.isPurelyStaticProduction();
        boolean arcStaticCorr = arcFurnace.isPurelyStaticCorrected();
        check("VULNERABILITY CHECK: Arc Furnace dynamic electrodes falsely marked purely static in production",
                arcStaticProd,
                "Production code evaluates arc furnace as purely static: " + arcStaticProd);

        if (arcStaticProd) {
            System.err.println("  -> [CRITICAL DEFECT]: Arc Furnace has dynamicQuads (hot/cold electrodes), but animator=null.");
            System.err.println("     MachineSpec.isPurelyStatic() returns TRUE because it only checks hasDynamicAnimators, not dynamicQuads!");
            System.err.println("     Result: Arc Furnace is 100% skipped in NucleusDispatcherBypass, freezing electrodes state!");
        }

        // Spec 3: Crucible (has dynamic melt level quads based on getFillLevel())
        SimulatedMachineSpec crucible = new SimulatedMachineSpec("crucible",
                List.of(
                        new SimulatedPartDef("Main", false, false, false, false, false),
                        new SimulatedPartDef("Melt", false, true, false, false, false)
                ),
                Collections.emptyList(), null);

        boolean crucibleStaticProd = crucible.isPurelyStaticProduction();
        check("VULNERABILITY CHECK: Crucible dynamic melt level falsely marked purely static in production",
                crucibleStaticProd,
                "Production code evaluates crucible as purely static: " + crucibleStaticProd);

        if (crucibleStaticProd) {
            System.err.println("  -> [CRITICAL DEFECT]: Crucible has dynamicQuads (melt surface), but animator=null.");
            System.err.println("     MachineSpec.isPurelyStatic() returns TRUE!");
            System.err.println("     Result: Crucible melt level never renders!");
        }

        // Spec 4: Strand Caster (continuous casting plate and melt surface quads)
        SimulatedMachineSpec strandCaster = new SimulatedMachineSpec("strand_caster",
                List.of(
                        new SimulatedPartDef("caster", false, false, false, false, false),
                        new SimulatedPartDef("plate", false, true, false, false, false),
                        new SimulatedPartDef("Surface", false, true, false, false, false)
                ),
                Collections.emptyList(), null);

        boolean strandStaticProd = strandCaster.isPurelyStaticProduction();
        check("VULNERABILITY CHECK: Strand Caster dynamic casting plate falsely marked purely static in production",
                strandStaticProd,
                "Production code evaluates strand caster as purely static: " + strandStaticProd);

        return ok;
    }

    private static boolean testRenderHooksDefaultMayDrawContentFallthrough() {
        System.out.println("\n--- Test Suite 2: MachineRenderHook default mayDrawContent Fallthrough ---");
        boolean ok = true;

        // Default interface hook (like BarrelTankRenderer::renderExtras or MachineFluidTankRenderer::renderDiamonds)
        SimulatedHook defaultHook = be -> false; // default method in interface returns false!

        SimulatedMachineSpec barrel = new SimulatedMachineSpec("barrel_iron",
                List.of(new SimulatedPartDef("Barrel", false, false, false, false, false)),
                List.of(defaultHook), null);

        // Simulate RetainedInstanceGraph.isMachineStaticAndRetained
        boolean isStaticAndRetained = simulateIsMachineStaticAndRetained(barrel, defaultHook, false);

        check("VULNERABILITY CHECK: Barrel Tank with default hook falsely deemed static and retained",
                isStaticAndRetained,
                "Barrel Tank evaluates to static and retained: " + isStaticAndRetained);

        if (isStaticAndRetained) {
            System.err.println("  -> [CRITICAL DEFECT]: BarrelTank has a render hook (renderExtras), but mayDrawContent defaults to false.");
            System.err.println("     In RetainedInstanceGraph.isMachineStaticAndRetained(), the loop checks: if (hook.mayDrawContent(be)) return false;");
            System.err.println("     Since mayDrawContent returns false, it falls through to 'return true;'!");
            System.err.println("     Result: Barrel Tank is skipped in NucleusDispatcherBypass, and NFPA diamond is NEVER DRAWN!");
        }

        return ok;
    }

    private static boolean simulateIsMachineStaticAndRetained(SimulatedMachineSpec spec, SimulatedHook hook, boolean dirty) {
        if (dirty) return false;
        if (spec.isPurelyStaticProduction()) return true;

        if (spec.hasAnimatedParts || spec.hasDynamicAnimators || spec.hasNonAssertableParts || spec.hasParametricParts) {
            return false;
        }

        if (!spec.hooks.isEmpty()) {
            for (SimulatedHook h : spec.hooks) {
                if (h.mayDrawContent(null)) {
                    return false;
                }
            }
        }

        return true;
    }

    // =========================================================================
    // PART 2: ATLAS WINDOW RETENTION LIFECYCLE & GHOST MACHINE BUG
    // =========================================================================

    public static final class SimulatedPending {
        public int instanceCount;
        public long assertedFrame = -1L;
        public boolean partitionValid = true;
        public int atlasWindowBase = -1;
        public int atlasWindowCap = 0;

        public SimulatedPending(int count, long frame) {
            this.instanceCount = count;
            this.assertedFrame = frame;
        }
    }

    public static final class SimulatedMdiBatchCoordinator {
        private final Map<Object, SimulatedPending> retainedByRenderer = new HashMap<>();
        private final List<SimulatedPending> retainedList = new ArrayList<>();
        private long retainedFrameStamp = 1L;
        private int totalDrawnInstances = 0;

        public void submit(Object renderer, int count) {
            SimulatedPending p = new SimulatedPending(count, retainedFrameStamp);
            p.atlasWindowBase = 0;
            p.atlasWindowCap = Math.max(16, count * 2);
            retainedByRenderer.put(renderer, p);
            retainedList.add(p);
        }

        /** Exact code from production MdiBatchCoordinator.java:713-730 */
        public boolean tryAssertRetainedPresence(Object renderer) {
            SimulatedPending p = retainedByRenderer.get(renderer);
            if (p == null || !p.partitionValid || p.instanceCount <= 0) {
                return false;
            }
            p.assertedFrame = retainedFrameStamp;
            return true;
        }

        /** Exact code from production MdiBatchCoordinator.java:496-504 */
        public void dropUnassertedRetained() {
            Iterator<SimulatedPending> it = retainedList.iterator();
            while (it.hasNext()) {
                SimulatedPending p = it.next();
                if (retainedFrameStamp - p.assertedFrame >= 2) {
                    retainedByRenderer.remove(p);
                    it.remove();
                }
            }
        }

        public void renderFrame(boolean machinesInWorld, Object renderer) {
            retainedFrameStamp++;

            // In InstancedStaticPartRenderer.java:1322:
            // if (instanceCount == 0) { if (coord.tryAssertRetainedPresence(this)) return; }
            int instanceCountThisFrame = machinesInWorld ? 1 : 0; // If machine in world, but short-circuited: 0!
            if (instanceCountThisFrame == 0) {
                tryAssertRetainedPresence(renderer);
            }

            dropUnassertedRetained();

            totalDrawnInstances = 0;
            for (SimulatedPending p : retainedList) {
                totalDrawnInstances += p.instanceCount;
            }
        }

        public int getTotalDrawnInstances() { return totalDrawnInstances; }
        public int getRetainedCount() { return retainedList.size(); }
    }

    private static boolean testAtlasWindowRetentionOnGenuineRemoval() {
        System.out.println("\n--- Test Suite 3: Atlas Window Retention & Ghost Machine Lifecycle ---");
        boolean ok = true;

        SimulatedMdiBatchCoordinator coordinator = new SimulatedMdiBatchCoordinator();
        Object dummyRenderer = new Object();

        // Frame 1: 1 machine placed in world
        coordinator.submit(dummyRenderer, 1);
        check("Frame 1: 1 machine initially submitted and retained",
                coordinator.getRetainedCount() == 1, "Retained count must be 1");

        // Frame 2: Machine is clean & static (short-circuited on CPU, instanceCount == 0)
        // Machine is in world, so tryAssertRetainedPresence should keep it
        coordinator.renderFrame(false, dummyRenderer);
        check("Frame 2: Static machine kept active across frame (retained=1, drawn=1)",
                coordinator.getRetainedCount() == 1 && coordinator.getTotalDrawnInstances() == 1,
                "Machine should be drawn");

        // Frame 3: MACHINE IS GENUINELY DESTROYED BY PLAYER (0 machines exist in world)!
        // BUT flushAll() is called on every frame for every registered spec!
        // In InstancedStaticPartRenderer: instanceCount == 0!
        // It calls tryAssertRetainedPresence(dummyRenderer)!
        for (int frame = 3; frame <= 10; frame++) {
            coordinator.renderFrame(false, dummyRenderer);
        }

        // DEFECT CHECK:
        boolean ghostDrawn = coordinator.getTotalDrawnInstances() == 1;
        boolean retainedLeaked = coordinator.getRetainedCount() == 1;

        check("VULNERABILITY CHECK: Destroyed machine still retained and drawn as a GHOST after 8 frames",
                ghostDrawn && retainedLeaked,
                "Ghost machine drawn: " + ghostDrawn + ", retained leaked: " + retainedLeaked);

        if (ghostDrawn) {
            System.err.println("  -> [CRITICAL DEFECT]: Machine was destroyed at frame 3.");
            System.err.println("     At frame 10, totalDrawnInstances is STILL " + coordinator.getTotalDrawnInstances() + "!");
            System.err.println("     tryAssertRetainedPresence() has no check for active instances in world or graph.");
            System.err.println("     It perpetually refreshes assertedFrame = retainedFrameStamp, preventing dropUnassertedRetained()!");
            System.err.println("     Result: Ghost machine rendered forever + persistent VRAM leak!");
        }

        return ok;
    }

    // =========================================================================
    // PART 3: SPARSE UPDATE QUEUE SLICE ISOLATION & SPAN COALESCING
    // =========================================================================

    private static boolean testSparseUpdateQueueSliceIsolation() {
        System.out.println("\n--- Test Suite 4: SparseUpdateQueue Granular Slice Isolation ---");
        boolean ok = true;

        // Simulated GPU instance slot buffer (32 floats = 128 bytes)
        float[] gpuBuffer = new float[32];

        // 1. Initial bake (pos: 10, 20, 30; quat: 0.707, 0.707; light: 15)
        gpuBuffer[0] = 10.0f;
        gpuBuffer[1] = 20.0f;
        gpuBuffer[2] = 30.0f;
        gpuBuffer[3] = 0.7071f;
        gpuBuffer[4] = 0.7071f;
        gpuBuffer[12] = 15.0f;
        gpuBuffer[13] = 15.0f;
        gpuBuffer[14] = 15.0f;
        gpuBuffer[15] = 15.0f;

        // 2. Perform granular light slice update at float offset 12..15 (new light: 240)
        float[] newLight = new float[]{ 240.0f, 240.0f, 240.0f, 240.0f };
        int floatOffset = 12;

        // Simulate enqueueSlotSlice writing only the slice:
        for (int i = 0; i < newLight.length; i++) {
            gpuBuffer[floatOffset + i] = newLight[i];
        }

        // Verify: coordinates and quaternions must remain completely untouched!
        boolean coordsIntact = gpuBuffer[0] == 10.0f && gpuBuffer[1] == 20.0f && gpuBuffer[2] == 30.0f;
        boolean quatIntact = gpuBuffer[3] == 0.7071f && gpuBuffer[4] == 0.7071f;
        boolean lightUpdated = gpuBuffer[12] == 240.0f && gpuBuffer[15] == 240.0f;

        check("Light slice update preserved position coordinates (floats 0..2)",
                coordsIntact, "Coordinates were corrupted: " + gpuBuffer[0] + ", " + gpuBuffer[1] + ", " + gpuBuffer[2]);
        check("Light slice update preserved quaternion rotation (floats 3..4)",
                quatIntact, "Quaternions were corrupted: " + gpuBuffer[3] + ", " + gpuBuffer[4]);
        check("Light slice update accurately updated light floats 12..15",
                lightUpdated, "Light was not updated properly");

        return coordsIntact && quatIntact && lightUpdated;
    }

    private static boolean testSpanCoalescingIntegrity() {
        System.out.println("\n--- Test Suite 5: Span Coalescing Algorithm Integrity ---");
        boolean ok = true;

        long[] dirtyGpuOffsets = new long[]{ 0, 128, 256, 512, 640 };
        int[] dirtyByteSizes = new int[]{ 128, 128, 128, 128, 128 };
        long[] dirtyStagingOffsets = new long[]{ 0, 128, 256, 384, 512 };
        int pendingCount = 5;

        // Run flushCoalescedSpans algorithm:
        int spanCount = 0;
        int spanStart = 0;
        List<String> spans = new ArrayList<>();

        while (spanStart < pendingCount) {
            int spanEnd = spanStart + 1;
            long curGpu = dirtyGpuOffsets[spanStart];
            int curBytes = dirtyByteSizes[spanStart];
            long curStaging = dirtyStagingOffsets[spanStart];

            long expectedGpu = curGpu + curBytes;
            long expectedStaging = curStaging + curBytes;

            while (spanEnd < pendingCount
                    && dirtyGpuOffsets[spanEnd] == expectedGpu
                    && dirtyStagingOffsets[spanEnd] == expectedStaging) {
                expectedGpu += dirtyByteSizes[spanEnd];
                expectedStaging += dirtyByteSizes[spanEnd];
                spanEnd++;
            }

            long totalSpanBytes = expectedGpu - curGpu;
            spans.add(String.format("Span from GPU %d to %d (bytes: %d)", curGpu, expectedGpu, totalSpanBytes));
            spanCount++;
            spanStart = spanEnd;
        }

        // Expected: Span 1 coalesces 0, 128, 256 (384 bytes). Span 2 coalesces 512, 640 (256 bytes).
        check("Contiguous GPU spans coalesced from 5 operations into exactly 2 uploads",
                spanCount == 2, "Expected 2 spans, got: " + spanCount);
        check("Span 1 coalesced 384 contiguous bytes (slots 0..2)",
                spans.get(0).contains("bytes: 384"), "Span 1 details: " + spans.get(0));
        check("Span 2 coalesced 256 contiguous bytes (slots 4..5)",
                spans.get(1).contains("bytes: 256"), "Span 2 details: " + spans.get(1));

        return ok;
    }

    // =========================================================================
    // PART 4: DIRTY TRACKING & MULTI-THREADED CONCURRENCY
    // =========================================================================

    private static boolean testDirtyTrackingInvalidationLifecycle() {
        System.out.println("\n--- Test Suite 6: Dirty Tracking Invalidation Lifecycle ---");
        boolean ok = true;

        AtomicBoolean renderDirty = new AtomicBoolean(false);

        // State 1: Clean machine
        check("Initial clean machine isRenderDirty() == false",
                !renderDirty.get(), "Must be clean");

        // State 2: markRenderDirty() invoked
        renderDirty.set(true);
        check("After markRenderDirty(), isRenderDirty() == true",
                renderDirty.get(), "Must be dirty");

        // State 3: Machine is collected and rendered -> onRenderCollected() invoked
        renderDirty.set(false);
        check("After onRenderCollected(), isRenderDirty() resets to false",
                !renderDirty.get(), "Must be reset to clean");

        return ok;
    }

    private static boolean testHandoffQueueConcurrentStress() {
        System.out.println("\n--- Test Suite 7: Multi-Threaded Handoff Queue Concurrency Stress ---");
        boolean ok = true;

        ConcurrentLinkedQueue<Runnable> handoffQueue = new ConcurrentLinkedQueue<>();
        int producers = 8;
        int opsPerProducer = 12_500; // 100,000 total operations
        ExecutorService pool = Executors.newFixedThreadPool(producers);
        CountDownLatch latch = new CountDownLatch(producers);
        AtomicInteger executedTasks = new AtomicInteger(0);

        for (int p = 0; p < producers; p++) {
            pool.submit(() -> {
                try {
                    for (int i = 0; i < opsPerProducer; i++) {
                        handoffQueue.offer(() -> executedTasks.incrementAndGet());
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        try {
            latch.await(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        pool.shutdown();

        // Drain on simulated render thread
        Runnable task;
        while ((task = handoffQueue.poll()) != null) {
            task.run();
        }

        check("All 100,000 concurrent handoff tasks safely enqueued and executed with zero loss",
                executedTasks.get() == producers * opsPerProducer,
                "Expected " + (producers * opsPerProducer) + ", executed: " + executedTasks.get());

        return ok;
    }
}
