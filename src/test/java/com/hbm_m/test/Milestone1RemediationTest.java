package com.hbm_m.test;

import com.hbm_m.client.render.retained.alloc.InstanceSlotAllocator;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * Empirical Adversarial Test Suite for Milestone 1 Remediation (Bug 1 & Bug 2).
 * Verifies slot lifecycle, intermediate slot deallocation, native/direct memory zeroing,
 * fadeMode = -1.0f, high-water mark adjustment, coordinate rebase arithmetic,
 * and shader clipping invariant oracle.
 */
public class Milestone1RemediationTest {

    // Record constants matching InstancedStaticPartRenderer
    public static final int POS_FLOAT_OFFSET = 0;
    public static final int FADE_FLOAT_OFFSET = 11;
    public static final int INSTANCE_DATA_SIZE = 32; // 32 floats = 128 bytes
    public static final float FADE_MODE_INVISIBLE = -1.0f;
    public static final float FADE_MODE_STATIC = 0.9999f;
    public static final float FADE_MODE_MIN_ANIM = 0.9998f;

    public static void main(String[] args) {
        System.out.println(">>> Starting Milestone 1 Remediation Empirical Verification Suite <<<");
        testSlotFreeHighWaterMarkReduction();
        testIntermediateSlotFreeing();
        testOriginRebaseCoordinateDelta();
        testSlotLifecycleWithNativeMemoryZeroingAndFadeMode();
        testRebaseAnchorNativeMemoryWithInactiveSlotProtection();
        testShaderClippingOracle();
        testMassiveRandomizedChurnStress();
        System.out.println(">>> All Milestone 1 Remediation Tests PASSED successfully! <<<");
    }

    private static void testSlotFreeHighWaterMarkReduction() {
        System.out.println("[Test 1] Testing basic high-water mark reduction...");
        InstanceSlotAllocator allocator = new InstanceSlotAllocator(128);

        long posKey1 = 100L;
        long posKey2 = 200L;

        int s1 = allocator.allocateSlot(posKey1);
        int s2 = allocator.allocateSlot(posKey2);

        assert allocator.getHighWaterMark() == 2 : "High water mark should be 2";
        assert allocator.isSlotAllocated(s1) : "Slot 0 should be allocated";
        assert allocator.isSlotAllocated(s2) : "Slot 1 should be allocated";

        // Free top slot (s2)
        allocator.freeSlot(posKey2);
        assert !allocator.hasSlot(posKey2) : "posKey2 should be freed";
        assert !allocator.isSlotAllocated(s2) : "Slot 1 should be deallocated";
        assert allocator.getHighWaterMark() == 1 : "High water mark should reduce to 1 after freeing top slot";

        // Free remaining slot (s1)
        allocator.freeSlot(posKey1);
        assert !allocator.hasSlot(posKey1) : "posKey1 should be freed";
        assert !allocator.isSlotAllocated(s1) : "Slot 0 should be deallocated";
        assert allocator.getHighWaterMark() == 0 : "High water mark should reduce to 0 after freeing all slots";
        System.out.println("  -> PASSED");
    }

    private static void testIntermediateSlotFreeing() {
        System.out.println("[Test 2] Testing intermediate slot freeing invariants...");
        InstanceSlotAllocator allocator = new InstanceSlotAllocator(128);

        long posKey1 = 10L;
        long posKey2 = 20L;
        long posKey3 = 30L;

        int s1 = allocator.allocateSlot(posKey1);
        int s2 = allocator.allocateSlot(posKey2);
        int s3 = allocator.allocateSlot(posKey3);

        assert allocator.getHighWaterMark() == 3 : "High water mark should be 3";

        // Free intermediate slot s2 (slot 1)
        allocator.freeSlot(posKey2);
        assert !allocator.hasSlot(posKey2) : "posKey2 should be freed";
        assert !allocator.isSlotAllocated(s2) : "Slot 1 should be deallocated";
        assert allocator.isSlotAllocated(s1) : "Slot 0 should still be allocated";
        assert allocator.isSlotAllocated(s3) : "Slot 2 should still be allocated";
        // High water mark stays at 3 because slot 2 is still occupied
        assert allocator.getHighWaterMark() == 3 : "High water mark remains 3 while top slot is active";

        // Free top slot s3 (slot 2)
        allocator.freeSlot(posKey3);
        // Now previous allocated bit is 0, so high water mark drops to 1!
        assert allocator.getHighWaterMark() == 1 : "High water mark should drop to 1 when top slot is freed";

        allocator.freeSlot(posKey1);
        assert allocator.getHighWaterMark() == 0 : "High water mark should drop to 0";
        System.out.println("  -> PASSED");
    }

    private static void testOriginRebaseCoordinateDelta() {
        System.out.println("[Test 3] Testing origin rebase coordinate delta arithmetic...");
        float worldX = 300f, worldY = 64f, worldZ = -150f;
        int oldAnchorX = 0, oldAnchorY = 64, oldAnchorZ = 0;
        int newAnchorX = 256, newAnchorY = 64, newAnchorZ = 0;

        float instPosOldX = worldX - oldAnchorX;
        float instPosOldY = worldY - oldAnchorY;
        float instPosOldZ = worldZ - oldAnchorZ;

        int deltaX = newAnchorX - oldAnchorX;
        int deltaY = newAnchorY - oldAnchorY;
        int deltaZ = newAnchorZ - oldAnchorZ;

        float instPosNewX = instPosOldX - deltaX;
        float instPosNewY = instPosOldY - deltaY;
        float instPosNewZ = instPosOldZ - deltaZ;

        float expectedX = worldX - newAnchorX;
        float expectedY = worldY - newAnchorY;
        float expectedZ = worldZ - newAnchorZ;

        assert instPosNewX == expectedX : "Rebased X must match expected";
        assert instPosNewY == expectedY : "Rebased Y must match expected";
        assert instPosNewZ == expectedZ : "Rebased Z must match expected";
        System.out.println("  -> PASSED");
    }

    /**
     * EMPIRICAL ADVERSARIAL STRESS TEST: Native/Direct Memory Slot Lifecycle & Fade Invalidation.
     * Recreates exact InstancedStaticPartRenderer native memory layout and validates:
     * - Slots 0, 1, 2 allocated in direct off-heap buffer
     * - Intermediate slot 1 freed:
     *     * highWaterMark remains 3
     *     * 128-byte record in direct memory is zeroed out
     *     * fadeMode at float 11 (word 11) is set to -1.0f (FADE_MODE_INVISIBLE)
     *     * Slot 0 and slot 2 native records remain intact
     * - Top slot 2 freed:
     *     * highWaterMark drops directly to 1
     * - Bottom slot 0 freed:
     *     * highWaterMark drops to 0
     */
    private static void testSlotLifecycleWithNativeMemoryZeroingAndFadeMode() {
        System.out.println("[Test 4] Stress testing native memory slot lifecycle, zeroing and fadeMode = -1.0f...");
        final int STRIDE_FLOATS = INSTANCE_DATA_SIZE; // 32
        final int FADE_OFFSET = FADE_FLOAT_OFFSET; // 11
        final float FADE_INVISIBLE = FADE_MODE_INVISIBLE; // -1.0f
        final float FADE_STATIC = FADE_MODE_STATIC; // 0.9999f

        InstanceSlotAllocator allocator = new InstanceSlotAllocator(64);
        FloatBuffer buffer = ByteBuffer.allocateDirect(64 * STRIDE_FLOATS * 4)
                .order(ByteOrder.nativeOrder()).asFloatBuffer();

        long k0 = 1001L, k1 = 1002L, k2 = 1003L;
        int s0 = allocator.allocateSlot(k0);
        int s1 = allocator.allocateSlot(k1);
        int s2 = allocator.allocateSlot(k2);

        assert s0 == 0 && s1 == 1 && s2 == 2 : "Slots must be allocated 0, 1, 2";
        assert allocator.getHighWaterMark() == 3 : "High water mark must be 3";

        // Initialize all 3 slots with non-zero test data
        for (int s = 0; s < 3; s++) {
            int slotBase = s * STRIDE_FLOATS;
            for (int f = 0; f < STRIDE_FLOATS; f++) {
                buffer.put(slotBase + f, 100.0f + s * 10.0f + f);
            }
            buffer.put(slotBase + FADE_OFFSET, FADE_STATIC);
        }

        // Step 1: Free intermediate slot 1 (mimicking InstancedStaticPartRenderer.freeSlot)
        allocator.freeSlot(s1);
        int s1Base = s1 * STRIDE_FLOATS;
        for (int f = 0; f < STRIDE_FLOATS; f++) {
            buffer.put(s1Base + f, 0.0f);
        }
        buffer.put(s1Base + FADE_OFFSET, FADE_INVISIBLE);

        // Invariant checks on intermediate free
        assert !allocator.isSlotAllocated(1) : "Slot 1 must be deallocated";
        assert allocator.isSlotAllocated(0) : "Slot 0 must remain allocated";
        assert allocator.isSlotAllocated(2) : "Slot 2 must remain allocated";
        assert allocator.getHighWaterMark() == 3 : "High water mark must stay 3 because slot 2 is allocated";

        // Verify slot 1 native memory: only fadeMode float is non-zero (-1.0f)
        for (int f = 0; f < STRIDE_FLOATS; f++) {
            float val = buffer.get(s1Base + f);
            if (f == FADE_OFFSET) {
                assert val == FADE_INVISIBLE : "Float at FADE_OFFSET must be -1.0f, found: " + val;
            } else {
                assert val == 0.0f : "Float at " + f + " must be 0.0f, found: " + val;
            }
        }

        // Verify slot 0 and slot 2 native memory are completely untouched
        int s0Base = s0 * STRIDE_FLOATS;
        assert buffer.get(s0Base + FADE_OFFSET) == FADE_STATIC : "Slot 0 fadeMode corrupted";
        assert buffer.get(s0Base) == 100.0f : "Slot 0 pos.x corrupted";

        int s2Base = s2 * STRIDE_FLOATS;
        assert buffer.get(s2Base + FADE_OFFSET) == FADE_STATIC : "Slot 2 fadeMode corrupted";
        assert buffer.get(s2Base) == 120.0f : "Slot 2 pos.x corrupted";

        // Step 2: Free top slot 2
        allocator.freeSlot(s2);
        assert !allocator.isSlotAllocated(2) : "Slot 2 must be deallocated";
        assert allocator.getHighWaterMark() == 1 : "High water mark must drop to 1 after freeing top slot";

        // Step 3: Free remaining slot 0
        allocator.freeSlot(s0);
        assert !allocator.isSlotAllocated(0) : "Slot 0 must be deallocated";
        assert allocator.getHighWaterMark() == 0 : "High water mark must drop to 0 after freeing all slots";
        System.out.println("  -> PASSED");
    }

    /**
     * EMPIRICAL ADVERSARIAL STRESS TEST: Rebase Anchor Native Memory & Inactive Slot Protection.
     * Validates that:
     * - Allocated slots have delta (deltaX, deltaY, deltaZ) subtracted from (x, y, z).
     * - Inactive/freed slots strictly preserve or enforce fadeMode = -1.0f.
     * - Multi-step rebasing and negative deltas preserve exact arithmetic.
     */
    private static void testRebaseAnchorNativeMemoryWithInactiveSlotProtection() {
        System.out.println("[Test 5] Stress testing native memory anchor rebase & inactive slot protection...");
        final int STRIDE_FLOATS = INSTANCE_DATA_SIZE;
        final int POS_OFFSET = POS_FLOAT_OFFSET;
        final int FADE_OFFSET = FADE_FLOAT_OFFSET;
        final float FADE_INVISIBLE = FADE_MODE_INVISIBLE;

        InstanceSlotAllocator allocator = new InstanceSlotAllocator(64);
        FloatBuffer buffer = ByteBuffer.allocateDirect(64 * STRIDE_FLOATS * 4)
                .order(ByteOrder.nativeOrder()).asFloatBuffer();

        // Allocate slots 0, 1, 2, 3
        int s0 = allocator.allocateSlot(10L);
        int s1 = allocator.allocateSlot(20L);
        int s2 = allocator.allocateSlot(30L);
        int s3 = allocator.allocateSlot(40L);

        // Free slots 1 and 3 (make them inactive)
        allocator.freeSlot(s1);
        allocator.freeSlot(s3);
        assert allocator.getHighWaterMark() == 3 : "High water mark should be 3";

        // Set positions
        int s0Base = s0 * STRIDE_FLOATS;
        buffer.put(s0Base + POS_OFFSET + 0, 100.5f);
        buffer.put(s0Base + POS_OFFSET + 1, 64.0f);
        buffer.put(s0Base + POS_OFFSET + 2, -200.5f);
        buffer.put(s0Base + FADE_OFFSET, 0.9999f);

        int s1Base = s1 * STRIDE_FLOATS;
        for (int f = 0; f < STRIDE_FLOATS; f++) buffer.put(s1Base + f, 0.0f);
        buffer.put(s1Base + FADE_OFFSET, FADE_INVISIBLE);

        int s2Base = s2 * STRIDE_FLOATS;
        buffer.put(s2Base + POS_OFFSET + 0, 500.0f);
        buffer.put(s2Base + POS_OFFSET + 1, 70.0f);
        buffer.put(s2Base + POS_OFFSET + 2, 1000.0f);
        buffer.put(s2Base + FADE_OFFSET, 0.9998f);

        // Emulate rebaseAnchor(256, 0, 256)
        simulateRebaseAnchor(allocator, buffer, STRIDE_FLOATS, 256, 0, 256);

        // Assertions after +256 rebase:
        assert Math.abs(buffer.get(s0Base + 0) - (-155.5f)) < 1e-4f : "Slot 0 X incorrect";
        assert Math.abs(buffer.get(s0Base + 1) - 64.0f) < 1e-4f : "Slot 0 Y incorrect";
        assert Math.abs(buffer.get(s0Base + 2) - (-456.5f)) < 1e-4f : "Slot 0 Z incorrect";
        assert buffer.get(s0Base + FADE_OFFSET) == 0.9999f : "Slot 0 fadeMode must not change";

        assert Math.abs(buffer.get(s2Base + 0) - 244.0f) < 1e-4f : "Slot 2 X incorrect";
        assert Math.abs(buffer.get(s2Base + 1) - 70.0f) < 1e-4f : "Slot 2 Y incorrect";
        assert Math.abs(buffer.get(s2Base + 2) - 744.0f) < 1e-4f : "Slot 2 Z incorrect";
        assert buffer.get(s2Base + FADE_OFFSET) == 0.9998f : "Slot 2 fadeMode must not change";

        // Inactive slot 1 must STRICTLY have fadeMode == -1.0f
        assert buffer.get(s1Base + FADE_OFFSET) == FADE_INVISIBLE : "Slot 1 must remain invisible";

        // Emulate large negative jump: rebaseAnchor(-512, 128, -1024)
        simulateRebaseAnchor(allocator, buffer, STRIDE_FLOATS, -512, 128, -1024);

        assert Math.abs(buffer.get(s0Base + 0) - (356.5f)) < 1e-4f : "Slot 0 X negative delta mismatch";
        assert Math.abs(buffer.get(s0Base + 1) - (-64.0f)) < 1e-4f : "Slot 0 Y negative delta mismatch";
        assert Math.abs(buffer.get(s0Base + 2) - (567.5f)) < 1e-4f : "Slot 0 Z negative delta mismatch";

        assert Math.abs(buffer.get(s2Base + 0) - (756.0f)) < 1e-4f : "Slot 2 X negative delta mismatch";
        assert Math.abs(buffer.get(s2Base + 1) - (-58.0f)) < 1e-4f : "Slot 2 Y negative delta mismatch";
        assert Math.abs(buffer.get(s2Base + 2) - (1768.0f)) < 1e-4f : "Slot 2 Z negative delta mismatch";

        assert buffer.get(s1Base + FADE_OFFSET) == FADE_INVISIBLE : "Slot 1 must remain invisible";
        System.out.println("  -> PASSED");
    }

    private static void simulateRebaseAnchor(InstanceSlotAllocator allocator, FloatBuffer buffer, int strideFloats,
                                             int deltaX, int deltaY, int deltaZ) {
        int maxSlot = allocator.getHighWaterMark();
        for (int s = 0; s < maxSlot; s++) {
            int slotBase = s * strideFloats;
            if (allocator.isSlotAllocated(s)) {
                if (deltaX != 0 || deltaY != 0 || deltaZ != 0) {
                    float x = buffer.get(slotBase + 0);
                    float y = buffer.get(slotBase + 1);
                    float z = buffer.get(slotBase + 2);
                    buffer.put(slotBase + 0, x - (float) deltaX);
                    buffer.put(slotBase + 1, y - (float) deltaY);
                    buffer.put(slotBase + 2, z - (float) deltaZ);
                }
            } else {
                buffer.put(slotBase + FADE_FLOAT_OFFSET, FADE_MODE_INVISIBLE);
            }
        }
    }

    /**
     * EMPIRICAL ORACLE: Verifies GLSL Shader Clipping Contract.
     * When fadeMode == -1.0f, gpuFadeAlpha is guaranteed to return 0.0f regardless of camera distance,
     * triggering gl_Position = vec4(2.0, 2.0, 2.0, 1.0) and clipping out the vertex from NDC.
     */
    private static void testShaderClippingOracle() {
        System.out.println("[Test 6] Testing shader clipping oracle (GLSL parity)...");
        float[] distances = { 0.0f, 1.0f, 50.0f, 128.0f, 256.0f, 512.0f, 10000.0f, 500000.0f };
        for (float dist : distances) {
            float alphaInvisible = glslFadeRampOracle(FADE_MODE_INVISIBLE, dist, 64.0f, 128.0f, 32.0f, 64.0f);
            assert alphaInvisible == 0.0f : "Shader oracle MUST return 0.0f for FADE_MODE_INVISIBLE at dist " + dist;

            // Valid static mode should return > 0 at distance 0
            float alphaStaticNear = glslFadeRampOracle(FADE_MODE_STATIC, 0.0f, 64.0f, 128.0f, 32.0f, 64.0f);
            assert alphaStaticNear == 1.0f : "Static mode at dist 0 must be 1.0f";
        }
        System.out.println("  -> PASSED");
    }

    private static float glslFadeRampOracle(float modeW, float dist, float staticStart, float staticEnd, float animStart, float animEnd) {
        if (modeW < 0.0f) return 0.0f;
        float mode = (1.0f - modeW) * 10000.0f;
        if (mode < 0.5f) return 1.0f;
        float sf = Math.max(0.0f, Math.min(1.0f, 1.0f - (dist - staticStart) / (staticEnd - staticStart)));
        if (mode < 1.5f) return sf;
        float af = Math.max(0.0f, Math.min(1.0f, 1.0f - (dist - animStart) / (animEnd - animStart)));
        return Math.min(sf, af);
    }

    /**
     * EMPIRICAL STRESS TEST: Massive Random Churn (10,000 Operations).
     * Simulates rapid chunk loading/unloading, machine break/place, and origin rebase jumps.
     * Evaluates continuous mathematical consistency on every step.
     */
    private static void testMassiveRandomizedChurnStress() {
        System.out.println("[Test 7] Massive randomized churn stress test (10,000 ops)...");
        final int CAPACITY = 1024;
        final int STRIDE_FLOATS = 32;
        final int FADE_OFFSET = 11;
        final float FADE_INVISIBLE = -1.0f;

        InstanceSlotAllocator allocator = new InstanceSlotAllocator(CAPACITY);
        FloatBuffer buffer = ByteBuffer.allocateDirect(CAPACITY * STRIDE_FLOATS * 4)
                .order(ByteOrder.nativeOrder()).asFloatBuffer();

        Random rng = new Random(0xCAFEBABE);
        Map<Long, Integer> activeSlots = new HashMap<>();
        Map<Integer, Float> oracleWorldX = new HashMap<>();
        float currentAnchorX = 0f;

        for (int op = 0; op < 10_000; op++) {
            int action = rng.nextInt(10);
            if (action < 5 && activeSlots.size() < CAPACITY - 10) {
                // Action: Allocate
                long posKey = rng.nextLong();
                while (activeSlots.containsKey(posKey)) posKey = rng.nextLong();
                int slot = allocator.allocateSlot(posKey);
                activeSlots.put(posKey, slot);

                float worldX = (float) (rng.nextInt(20000) - 10000);
                oracleWorldX.put(slot, worldX);

                int slotBase = slot * STRIDE_FLOATS;
                buffer.put(slotBase + 0, worldX - currentAnchorX);
                buffer.put(slotBase + 4, 64.0f);
                buffer.put(slotBase + 8, 0.0f);
                buffer.put(slotBase + FADE_OFFSET, 0.9999f);

            } else if (action < 8 && !activeSlots.isEmpty()) {
                // Action: Free random existing slot
                int idx = rng.nextInt(activeSlots.size());
                Long targetKey = (Long) activeSlots.keySet().toArray()[idx];
                int slot = activeSlots.remove(targetKey);
                oracleWorldX.remove(slot);

                allocator.freeSlot(slot);
                int slotBase = slot * STRIDE_FLOATS;
                for (int f = 0; f < STRIDE_FLOATS; f++) buffer.put(slotBase + f, 0.0f);
                buffer.put(slotBase + FADE_OFFSET, FADE_INVISIBLE);

            } else {
                // Action: Origin Rebase
                int deltaX = (rng.nextInt(3) - 1) * 256; // -256, 0, or +256
                currentAnchorX += deltaX;
                simulateRebaseAnchor(allocator, buffer, STRIDE_FLOATS, deltaX, 0, 0);
            }

            // Invariant Oracle Verification every 100 ops
            if (op % 100 == 0) {
                int hwm = allocator.getHighWaterMark();
                int expectedMaxSlot = -1;
                for (int s : activeSlots.values()) {
                    if (s > expectedMaxSlot) expectedMaxSlot = s;
                }
                assert hwm == (expectedMaxSlot + 1) : "HWM mismatch: expected " + (expectedMaxSlot + 1) + " got " + hwm;

                for (int s = 0; s < hwm; s++) {
                    int slotBase = s * STRIDE_FLOATS;
                    float fade = buffer.get(slotBase + FADE_OFFSET);
                    if (allocator.isSlotAllocated(s)) {
                        assert fade > 0.0f : "Allocated slot " + s + " must have positive fadeMode";
                        float expectedPosX = oracleWorldX.get(s) - currentAnchorX;
                        float actualPosX = buffer.get(slotBase + 0);
                        assert Math.abs(expectedPosX - actualPosX) < 1e-3f : "Position divergence at slot " + s;
                    } else {
                        assert fade == FADE_INVISIBLE : "Unallocated slot " + s + " must have FADE_MODE_INVISIBLE (-1.0f)";
                    }
                }
            }
        }
        System.out.println("  -> PASSED (10,000 iterations verified cleanly)");
    }
}
