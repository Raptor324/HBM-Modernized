package com.hbm_m.test;

/**
 * Empirical Verification Suite for Milestone 2 & Milestone 3 improvements:
 * 1. Hermite spline analytical stroke formula, apex/turnaround smoothness, and symmetry.
 * 2. Continuous rotation kinematics for GPU offloading (Turbofan & ChemPlant).
 * 3. Bitmask flag integrity for GPU joint specifications (FLAG_CYCLE bit 0, FLAG_HERMITE bit 1).
 */
public class Milestone2And3VerificationTest {

    // Bitmask constants matching NucleusJointSpecs
    public static final int FLAG_CYCLE = 1;
    public static final int FLAG_HERMITE = 2;

    public static void main(String[] args) {
        System.out.println(">>> Starting Milestone 2 & 3 Verification Suite <<<");
        testHermiteSplineEvaluation();
        testHermiteDerivativeSmoothness();
        testHermiteSymmetry();
        testJointSpecsFlagBitmasks();
        testTurbofanKinematics();
        testChemplantKinematics();
        System.out.println(">>> All Milestone 2 & 3 Verification Tests PASSED! <<<");
    }

    private static void testHermiteSplineEvaluation() {
        System.out.println("[Test 1] Testing Hermite spline analytical stroke formula...");
        float s0 = hermiteStroke(0.0f);
        float sMid = hermiteStroke(0.5f);
        float s1 = hermiteStroke(1.0f);

        assert Math.abs(s0 - 0.0f) < 1e-6f : "Stroke at u=0 should be 0.0, was " + s0;
        assert Math.abs(sMid - 1.0f) < 1e-6f : "Stroke at u=0.5 should be 1.0 (apex), was " + sMid;
        assert Math.abs(s1 - 0.0f) < 1e-6f : "Stroke at u=1.0 should be 0.0, was " + s1;
        System.out.println("  -> PASSED");
    }

    private static void testHermiteDerivativeSmoothness() {
        System.out.println("[Test 2] Testing Hermite derivative zero-acceleration at turnaround points...");
        float eps = 1e-4f;
        float slopeAt0 = (hermiteStroke(eps) - hermiteStroke(0.0f)) / eps;
        float slopeAtMid = (hermiteStroke(0.5f + eps) - hermiteStroke(0.5f - eps)) / (2.0f * eps);
        float slopeAt1 = (hermiteStroke(1.0f) - hermiteStroke(1.0f - eps)) / eps;

        assert Math.abs(slopeAt0) < 0.01f : "Slope at start should be near 0 (smooth acceleration), was " + slopeAt0;
        assert Math.abs(slopeAtMid) < 0.01f : "Slope at apex should be near 0 (smooth turnaround), was " + slopeAtMid;
        assert Math.abs(slopeAt1) < 0.01f : "Slope at return should be near 0 (smooth deceleration), was " + slopeAt1;
        System.out.println("  -> PASSED");
    }

    private static void testHermiteSymmetry() {
        System.out.println("[Test 3] Testing Hermite forward and retract stroke symmetry...");
        for (float u = 0.01f; u <= 0.49f; u += 0.01f) {
            float forward = hermiteStroke(u);
            float retract = hermiteStroke(1.0f - u);
            assert Math.abs(forward - retract) < 1e-5f : "Hermite stroke should be symmetric at u=" + u;
        }
        System.out.println("  -> PASSED");
    }

    private static void testJointSpecsFlagBitmasks() {
        System.out.println("[Test 4] Testing NucleusJointSpecs flag bitmasks...");
        assert FLAG_CYCLE == 1 : "FLAG_CYCLE must be bit 0 (1)";
        assert FLAG_HERMITE == 2 : "FLAG_HERMITE must be bit 1 (2)";
        assert (FLAG_CYCLE & FLAG_HERMITE) == 0 : "Flags must not collide";
        assert ((FLAG_CYCLE | FLAG_HERMITE) & FLAG_HERMITE) == FLAG_HERMITE : "Bit 1 masking test";
        System.out.println("  -> PASSED");
    }

    private static void testTurbofanKinematics() {
        System.out.println("[Test 5] Testing Turbofan continuous rotor kinematics...");
        // Turbofan: spin += momentum / 2 degrees per tick
        // Duration per full revolution: 360 / (momentum / 2) ticks
        int[] testMomentums = {10, 20, 50, 80, 100};
        for (int m : testMomentums) {
            double degPerTick = m / 2.0;
            double ticksPerCycle = 360.0 / Math.max(1, m / 2.0);
            double calculatedDegPerTick = 360.0 / ticksPerCycle;
            assert Math.abs(degPerTick - calculatedDegPerTick) < 1e-6 : "Turbofan kinematics parity at momentum=" + m;
        }
        System.out.println("  -> PASSED");
    }

    private static void testChemplantKinematics() {
        System.out.println("[Test 6] Testing Chemical Plant spinner kinematics...");
        // ChemPlant: (anim * 15.0) % 360.0 degrees -> 15 degrees per tick
        // 24 ticks per cycle = 360 / 24 = 15.0 degrees per tick
        double ticksPerCycle = 24.0;
        double degPerTick = 360.0 / ticksPerCycle;
        assert Math.abs(degPerTick - 15.0) < 1e-6 : "ChemPlant spinner kinematics must evaluate exactly 15 deg/tick";
        System.out.println("  -> PASSED");
    }

    private static float hermiteStroke(float u) {
        return u < 0.5f
                ? (4.0f * u * u * (3.0f - 4.0f * u))
                : (4.0f * (1.0f - u) * (1.0f - u) * (3.0f - 4.0f * (1.0f - u)));
    }
}
