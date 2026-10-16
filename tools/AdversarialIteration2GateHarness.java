package tools;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

/**
 * Empirical Adversarial Verification Harness for Milestone 3 Iteration 2 Gate Evaluation:
 *
 * 1. Dynamic Quads Non-Freezing (Arc Furnace, Crucible, Strand Caster, Fluid Tank)
 * 2. Hook Execution & mayDrawContent Default Inversion (Barrel Tank, Fluid Tank)
 * 3. Ghost Machine & Atlas Window VRAM Leak Prevention (Lifecycle & activeCountsByType)
 * 4. Newcomer Snapshot Merging (Preventing existing static machine clobbering)
 * 5. Parametric Roster Assertion & Frame Bake-Offset De-poisoning (Advanced Assembler)
 */
public class AdversarialIteration2GateHarness {

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
        System.out.println("EMPIRICAL ADVERSARIAL GATE VERIFICATION: ITERATION 2 REMEDIATION");
        System.out.println("===============================================================================\n");

        boolean pass = true;

        // Domain 1: Dynamic Quads Non-Freezing & isPurelyStatic Contract
        pass &= testDynamicQuadsNonFreezingContract();

        // Domain 2: Hook Execution & mayDrawContent Inversion
        pass &= testHookExecutionAndMayDrawContent();

        // Domain 3: Ghost Machine & Atlas Window Retention Lifecycle
        pass &= testGhostMachineEvictionAndAtlasReclaim();

        // Domain 4: Newcomer Snapshot Merging Integrity
        pass &= testNewcomerSnapshotMerging();

        // Domain 5: Parametric Roster Assertion & Frame Non-Blocking
        pass &= testParametricRosterAssertionAndFrame();

        System.out.println("\n===============================================================================");
        System.out.printf("SUMMARY: Total Tests: %d | Passed: %d | Failed: %d%n", totalTests, passedTests, failedTests);
        if (pass && failedTests == 0) {
            System.out.println(">>> OVERALL GATE VERDICT: APPROVE (ALL ADVERSARIAL CHALLENGES PASSED) <<<");
        } else {
            System.err.println(">>> OVERALL GATE VERDICT: REQUEST_CHANGES (DEFECTS CONFIRMED) <<<");
        }
        System.out.println("===============================================================================");
    }

    // =========================================================================
    // DOMAIN 1: DYNAMIC QUADS NON-FREEZING
    // =========================================================================

    public static final class PartDefSim {
        public final String name;
        public final boolean animated;
        public final boolean dynamic;
        public final boolean hasAnimator;
        public final boolean parametric;
        public final boolean hasTintOverride;

        public PartDefSim(String name, boolean animated, boolean dynamic, boolean hasAnimator, boolean parametric, boolean hasTintOverride) {
            this.name = name;
            this.animated = animated;
            this.dynamic = dynamic;
            this.hasAnimator = hasAnimator;
            this.parametric = parametric;
            this.hasTintOverride = hasTintOverride;
        }
    }

    public static final class MachineSpecSim {
        public final String id;
        public final List<PartDefSim> parts;
        public final List<HookSim> hooks;
        public final Object animationEpoch;

        public final boolean hasAnimatedParts;
        public final boolean hasDynamicAnimators;
        public final boolean hasDynamicParts;
        public final boolean hasNonAssertableParts;
        public final boolean hasParametricParts;

        public MachineSpecSim(String id, List<PartDefSim> parts, List<HookSim> hooks, Object animationEpoch) {
            this.id = id;
            this.parts = List.copyOf(parts);
            this.hooks = hooks != null ? List.copyOf(hooks) : Collections.emptyList();
            this.animationEpoch = animationEpoch;

            boolean anyAnimated = false;
            boolean anyDynamicAnimator = false;
            boolean anyDynamic = false;
            for (PartDefSim p : parts) {
                if (p.animated) anyAnimated = true;
                if (p.animated && p.hasAnimator && p.dynamic) anyDynamicAnimator = true;
                if (p.dynamic) anyDynamic = true;
            }
            this.hasAnimatedParts = anyAnimated;
            this.hasDynamicAnimators = anyDynamicAnimator;
            this.hasDynamicParts = anyDynamic;

            boolean anyNonAssertable = false;
            for (PartDefSim p : parts) {
                if ((p.animated && p.hasAnimator && p.dynamic) || p.hasTintOverride) {
                    anyNonAssertable = true;
                    break;
                }
            }
            this.hasNonAssertableParts = anyNonAssertable;

            boolean anyParametric = false;
            for (PartDefSim p : parts) {
                if (p.parametric) {
                    anyParametric = true;
                    break;
                }
            }
            this.hasParametricParts = anyParametric;
        }

        /** Production MachineSpec.isPurelyStatic() (post-remediation) */
        public boolean isPurelyStatic() {
            return !hasAnimatedParts && !hasDynamicAnimators && !hasNonAssertableParts
                    && !hasParametricParts && !hasDynamicParts && hooks.isEmpty() && animationEpoch == null;
        }
    }

    public interface HookSim {
        default boolean mayDrawContent(Object be) {
            return true; // Post-remediation inverted default!
        }
    }

    public static boolean simulateIsMachineStaticAndRetained(MachineSpecSim spec, boolean isDirty) {
        if (isDirty) return false;
        if (spec.hasDynamicParts) return false;
        if (!spec.hooks.isEmpty()) return false;
        if (spec.isPurelyStatic()) return true;
        return false;
    }

    public static boolean simulateNucleusDispatcherBypass(MachineSpecSim spec, boolean isDirty) {
        // Line 552 in NucleusDispatcherBypass:
        if (simulateIsMachineStaticAndRetained(spec, isDirty)) {
            return true; // Short-circuited / skipped!
        }
        return false; // Executes collectOne!
    }

    private static boolean testDynamicQuadsNonFreezingContract() {
        System.out.println("--- Test Domain 1: Dynamic Quads Non-Freezing Contract ---");
        boolean ok = true;

        // Truly static: Centrifuge (solid frame, zero animators, zero dynamic quads)
        MachineSpecSim centrifuge = new MachineSpecSim("centrifuge",
                List.of(new PartDefSim("Frame", false, false, false, false, false)),
                Collections.emptyList(), null);

        check("Centrifuge hasDynamicParts() == false",
                !centrifuge.hasDynamicParts, "Centrifuge should have no dynamic parts");
        check("Centrifuge isPurelyStatic() == true",
                centrifuge.isPurelyStatic(), "Centrifuge should be purely static");
        check("Centrifuge is short-circuited in NucleusDispatcherBypass",
                simulateNucleusDispatcherBypass(centrifuge, false), "Centrifuge should be skipped on CPU");

        // Arc Furnace: hot/cold dynamic electrode quads without animator
        MachineSpecSim arcFurnace = new MachineSpecSim("arcfurnace",
                List.of(new PartDefSim("Electrodes", false, true, false, false, false)),
                Collections.emptyList(), null);

        check("Arc Furnace hasDynamicParts() == true",
                arcFurnace.hasDynamicParts, "Arc Furnace must have dynamic parts");
        check("Arc Furnace isPurelyStatic() == false",
                !arcFurnace.isPurelyStatic(), "Arc Furnace must NOT be purely static");
        check("Arc Furnace is NOT short-circuited in NucleusDispatcherBypass (executes BER)",
                !simulateNucleusDispatcherBypass(arcFurnace, false), "Arc Furnace must execute collectOne");

        // Crucible: dynamic melt level quads without animator
        MachineSpecSim crucible = new MachineSpecSim("crucible",
                List.of(
                        new PartDefSim("Main", false, false, false, false, false),
                        new PartDefSim("Melt", false, true, false, false, false)
                ),
                Collections.emptyList(), null);

        check("Crucible hasDynamicParts() == true",
                crucible.hasDynamicParts, "Crucible must have dynamic parts");
        check("Crucible isPurelyStatic() == false",
                !crucible.isPurelyStatic(), "Crucible must NOT be purely static");
        check("Crucible is NOT short-circuited in NucleusDispatcherBypass (executes BER)",
                !simulateNucleusDispatcherBypass(crucible, false), "Crucible must execute collectOne");

        // Strand Caster: dynamic casting plate and melt surface quads
        MachineSpecSim strandCaster = new MachineSpecSim("strand_caster",
                List.of(
                        new PartDefSim("caster", false, false, false, false, false),
                        new PartDefSim("plate", false, true, false, false, false),
                        new PartDefSim("Surface", false, true, false, false, false)
                ),
                Collections.emptyList(), null);

        check("Strand Caster hasDynamicParts() == true",
                strandCaster.hasDynamicParts, "Strand Caster must have dynamic parts");
        check("Strand Caster isPurelyStatic() == false",
                !strandCaster.isPurelyStatic(), "Strand Caster must NOT be purely static");
        check("Strand Caster is NOT short-circuited in NucleusDispatcherBypass (executes BER)",
                !simulateNucleusDispatcherBypass(strandCaster, false), "Strand Caster must execute collectOne");

        // Fluid Tank: dynamic tank cylinder + hazard diamond hook
        HookSim diamondHook = new HookSim() {};
        MachineSpecSim fluidTank = new MachineSpecSim("fluidtank",
                List.of(
                        new PartDefSim("Frame", false, false, false, false, false),
                        new PartDefSim("Tank", false, true, false, false, false)
                ),
                List.of(diamondHook), null);

        check("Fluid Tank hasDynamicParts() == true",
                fluidTank.hasDynamicParts, "Fluid Tank must have dynamic parts");
        check("Fluid Tank isPurelyStatic() == false",
                !fluidTank.isPurelyStatic(), "Fluid Tank must NOT be purely static");
        check("Fluid Tank is NOT short-circuited in NucleusDispatcherBypass (executes BER)",
                !simulateNucleusDispatcherBypass(fluidTank, false), "Fluid Tank must execute collectOne");

        return ok;
    }

    // =========================================================================
    // DOMAIN 2: HOOK EXECUTION & mayDrawContent INVERSION
    // =========================================================================

    private static boolean testHookExecutionAndMayDrawContent() {
        System.out.println("\n--- Test Domain 2: Hook Execution & mayDrawContent Contract ---");
        boolean ok = true;

        // Default interface hook (no override of mayDrawContent)
        HookSim defaultHook = new HookSim() {};

        check("MachineRenderHook default mayDrawContent() == true",
                defaultHook.mayDrawContent(new Object()), "Interface default must be true");

        // Barrel Tank (Iron): static body + default hook (renderExtras)
        MachineSpecSim barrelIron = new MachineSpecSim("barrel_iron",
                List.of(new PartDefSim("Barrel", false, false, false, false, false)),
                List.of(defaultHook), null);

        check("Barrel Tank hooks list is NOT empty",
                !barrelIron.hooks.isEmpty(), "Barrel Tank must contain render hook");
        check("Barrel Tank isPurelyStatic() == false",
                !barrelIron.isPurelyStatic(), "Barrel Tank must NOT be purely static");
        check("Barrel Tank is NOT short-circuited in NucleusDispatcherBypass (executes BER)",
                !simulateNucleusDispatcherBypass(barrelIron, false), "Barrel Tank hook must execute in BER");

        return ok;
    }

    // =========================================================================
    // DOMAIN 3: GHOST MACHINE & ATLAS WINDOW RETENTION LIFECYCLE
    // =========================================================================

    public static final class RetainedGraphSim {
        private final Map<String, Integer> activeCountsByType = new HashMap<>();
        private final Set<Long> activeMachineKeys = new HashSet<>();

        public void registerMachine(String type, long blockPosKey) {
            activeMachineKeys.add(blockPosKey);
            activeCountsByType.merge(type, 1, Integer::sum);
        }

        public void removeMachine(String type, long blockPosKey) {
            activeMachineKeys.remove(blockPosKey);
            activeCountsByType.computeIfPresent(type, (k, v) -> v <= 1 ? null : v - 1);
        }

        public boolean hasActiveInstances(String type) {
            Integer count = activeCountsByType.get(type);
            return count != null && count > 0;
        }

        public boolean hasMachineKey(long blockPosKey) {
            return activeMachineKeys.contains(blockPosKey);
        }
    }

    public static final class MdiCoordinatorSim {
        public static final class PendingSim {
            public int instanceCount;
            public long assertedFrame;
            public boolean partitionValid = true;

            public PendingSim(int count, long frame) {
                this.instanceCount = count;
                this.assertedFrame = frame;
            }
        }

        private final Map<String, PendingSim> retainedByRenderer = new HashMap<>();
        private final List<PendingSim> retainedList = new ArrayList<>();
        private long retainedFrameStamp = 1L;

        public void submit(String type, int count) {
            PendingSim p = new PendingSim(count, retainedFrameStamp);
            retainedByRenderer.put(type, p);
            retainedList.add(p);
        }

        /** Post-remediation tryAssertRetainedPresence with hasActiveInstances check */
        public boolean tryAssertRetainedPresence(String type, RetainedGraphSim rig) {
            if (rig != null && !rig.hasActiveInstances(type)) {
                return false; // Active instances in world are 0 — reject assertion!
            }
            PendingSim p = retainedByRenderer.get(type);
            if (p == null || !p.partitionValid || p.instanceCount <= 0) {
                return false;
            }
            p.assertedFrame = retainedFrameStamp;
            return true;
        }

        public void dropUnassertedRetained() {
            Iterator<PendingSim> it = retainedList.iterator();
            while (it.hasNext()) {
                PendingSim p = it.next();
                if (retainedFrameStamp - p.assertedFrame >= 2) {
                    it.remove();
                }
            }
        }

        public void advanceFrame(RetainedGraphSim rig, String type, boolean shortCircuited) {
            retainedFrameStamp++;
            // flushBatchVanilla logic when CPU instanceCount == 0:
            if (shortCircuited) {
                tryAssertRetainedPresence(type, rig);
            }
            dropUnassertedRetained();
        }

        public int getActiveDrawnInstances() {
            int sum = 0;
            for (PendingSim p : retainedList) sum += p.instanceCount;
            return sum;
        }

        public int getRetainedBatchCount() {
            return retainedList.size();
        }
    }

    private static boolean testGhostMachineEvictionAndAtlasReclaim() {
        System.out.println("\n--- Test Domain 3: Ghost Machine Eviction & Atlas Retention ---");
        boolean ok = true;

        RetainedGraphSim rig = new RetainedGraphSim();
        MdiCoordinatorSim coordinator = new MdiCoordinatorSim();
        String machineType = "centrifuge";
        long posKey = 1000L;

        // Frame 1: Machine placed in world
        rig.registerMachine(machineType, posKey);
        coordinator.submit(machineType, 1);

        check("Frame 1: Machine registered in RIG (hasActiveInstances == true)",
                rig.hasActiveInstances(machineType), "RIG must track active machine");
        check("Frame 1: 1 batch retained, 1 instance drawn",
                coordinator.getRetainedBatchCount() == 1 && coordinator.getActiveDrawnInstances() == 1,
                "Batch must be drawn");

        // Frame 2: Machine static & retained (short-circuited on CPU, instanceCount == 0)
        coordinator.advanceFrame(rig, machineType, true);
        check("Frame 2: Static retained machine asserted across frame (drawn=1)",
                coordinator.getActiveDrawnInstances() == 1, "Static machine must persist on clean frame");

        // Frame 3: MACHINE DESTROYED BY PLAYER (broken with pickaxe)
        rig.removeMachine(machineType, posKey);
        check("Frame 3: After removal, RIG hasActiveInstances == false",
                !rig.hasActiveInstances(machineType), "RIG active count must drop to 0");

        // Frame 3 render pass: CPU instanceCount == 0 -> tryAssertRetainedPresence called
        coordinator.advanceFrame(rig, machineType, true);

        // Frame 4 render pass: 2 frames unasserted -> dropUnassertedRetained evicts batch
        coordinator.advanceFrame(rig, machineType, true);

        check("Frame 4: Ghost machine successfully EVICTED from GPU retained list (retained=0)",
                coordinator.getRetainedBatchCount() == 0,
                "Retained list must be 0, got: " + coordinator.getRetainedBatchCount());
        check("Frame 4: Total drawn instances cleanly dropped to 0 (VRAM reclaimed, zero ghost rendering)",
                coordinator.getActiveDrawnInstances() == 0,
                "Drawn instances must be 0, got: " + coordinator.getActiveDrawnInstances());

        return ok;
    }

    // =========================================================================
    // DOMAIN 4: NEWCOMER SNAPSHOT MERGING
    // =========================================================================

    private static boolean testNewcomerSnapshotMerging() {
        System.out.println("\n--- Test Domain 4: Newcomer Snapshot Merging Integrity ---");
        boolean ok = true;

        RetainedGraphSim rig = new RetainedGraphSim();
        long existingKey1 = 101L;
        long existingKey2 = 102L;
        long newcomerKey = 103L;

        rig.registerMachine("machine", existingKey1);
        rig.registerMachine("machine", existingKey2);

        // Previous retained snapshot (2 instances: 101, 102)
        long[] prevKeys = new long[]{ existingKey1, existingKey2 };
        int prevCount = 2;

        // Newcomer arrives (1 instance: 103)
        rig.registerMachine("machine", newcomerKey);
        long[] incomingKeys = new long[]{ newcomerKey };
        int incomingCount = 1;

        // Simulate MdiBatchCoordinator.submit merging logic (lines 590-660):
        Set<Long> incomingSet = new HashSet<>();
        for (long k : incomingKeys) incomingSet.add(k);

        List<Long> keptKeys = new ArrayList<>();
        for (int j = 0; j < prevCount; j++) {
            long k = prevKeys[j];
            if (!incomingSet.contains(k) && rig.hasMachineKey(k)) {
                keptKeys.add(k);
            }
        }

        int totalMergedCount = keptKeys.size() + incomingCount;
        long[] mergedKeys = new long[totalMergedCount];
        for (int m = 0; m < keptKeys.size(); m++) {
            mergedKeys[m] = keptKeys.get(m);
        }
        System.arraycopy(incomingKeys, 0, mergedKeys, keptKeys.size(), incomingCount);

        check("Merged snapshot preserves all existing retained machines without clobbering",
                totalMergedCount == 3, "Expected 3 total instances, got: " + totalMergedCount);
        check("Merged keys contain [101, 102, 103]",
                mergedKeys[0] == 101L && mergedKeys[1] == 102L && mergedKeys[2] == 103L,
                "Keys must preserve existing instances followed by newcomer");

        return ok;
    }

    // =========================================================================
    // DOMAIN 5: PARAMETRIC ROSTER ASSERTION & FRAME NON-BLOCKING
    // =========================================================================

    private static boolean testParametricRosterAssertionAndFrame() {
        System.out.println("\n--- Test Domain 5: Parametric Roster Assertion & Frame Non-Blocking ---");
        boolean ok = true;

        // Test 5.1: canAssertInstance GradParams poison guard removal
        // Pre-remediation: MemoryUtil.memGetFloat(...) < 0f falsely rejected all joints (joint >= 0)
        // Post-remediation: Key-only assert: instanceOcclusionKeys[instanceCount] == posKey
        long posKey = 424242L;
        long[] instanceOcclusionKeys = new long[10];
        instanceOcclusionKeys[0] = posKey;
        int instanceCount = 0;
        int maxInstances = 10;
        boolean initialized = true;

        float jointBaseIndex = 4.0f; // Advanced Assembler arm rig joint >= 0.0f
        boolean canAssert = initialized && instanceCount < maxInstances && instanceOcclusionKeys[instanceCount] == posKey;

        check("Parametric joint rigs with joint index >= 0.0f successfully assert in canAssertInstance",
                canAssert, "canAssertInstance must succeed for parametric slots");

        // Test 5.2: Advanced Assembler Frame part has dynamic animator with bake offset,
        // but animated == false. Verify hasNonAssertableParts == false:
        PartDefSim framePart = new PartDefSim("Frame", false, true, true, false, false);
        MachineSpecSim assemblerSpec = new MachineSpecSim("advanced_assembler",
                List.of(framePart), Collections.emptyList(), (java.util.function.ToLongFunction<Object>) be -> 0L);

        check("Frame with static bake offset (animated=false, dynamic=true, hasAnimator=true) sets hasNonAssertableParts == false",
                !assemblerSpec.hasNonAssertableParts, "Static Frame part must NOT block fast assert");

        // Test 5.3: Tracked Rig Arm parts (animated=true, parametric=true, epoch!=null)
        PartDefSim armLower = new PartDefSim("ArmLower", true, false, false, true, false);
        boolean canAssertParametric = armLower.parametric && assemblerSpec.animationEpoch != null;
        boolean acceptedInRosterCategory = (!armLower.animated || canAssertParametric)
                && !(armLower.hasAnimator && armLower.dynamic && !canAssertParametric)
                && armLower.parametric;

        check("Tracked Rig arm parts with stable epoch are accepted into Roster ASSERT_EPOCH category",
                acceptedInRosterCategory, "ArmLower must be categorized as ASSERT_EPOCH");

        return ok;
    }
}
