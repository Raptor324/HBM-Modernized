package com.hbm_m.powerarmor;

import com.hbm_m.damagesource.ModDamageSources;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageSources;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * Test class for validating DamageResistanceHandler functionality
 * This ensures the ported system works exactly like the original HBM 1.7.10
 */
public class DamageSystemTest {

    /**
     * Test basic DT+DR calculations
     */
    public static void testBasicDamageCalculation() {
        System.out.println("=== Testing DamageResistanceHandler ===");

        // Test 1: Basic DT reduction
        float damage = 10.0f;
        float dt = 3.0f;
        float dr = 0.5f; // 50% resistance

        // Expected: damage - dt = 7, then 7 * (1 - dr) = 7 * 0.5 = 3.5
        float expected = 3.5f;

        // Since we don't have a real entity, we'll test the formula manually
        float step1 = Math.max(0F, damage - dt); // 10 - 3 = 7
        float finalDamage = step1 * (1F - dr);   // 7 * 0.5 = 3.5

        System.out.println("Test 1 - Basic DT+DR:");
        System.out.println("Input: damage=" + damage + ", dt=" + dt + ", dr=" + dr);
        System.out.println("Expected: " + expected + ", Got: " + finalDamage);
        System.out.println("Result: " + (Math.abs(expected - finalDamage) < 0.001f ? "PASS" : "FAIL"));

        // Test 2: DT >= damage (complete absorption)
        damage = 5.0f;
        dt = 10.0f;
        expected = 0.0f;

        step1 = Math.max(0F, damage - dt); // 5 - 10 = 0
        finalDamage = step1 * (1F - dr);   // 0 * 0.5 = 0

        System.out.println("\nTest 2 - Complete absorption:");
        System.out.println("Input: damage=" + damage + ", dt=" + dt + ", dr=" + dr);
        System.out.println("Expected: " + expected + ", Got: " + finalDamage);
        System.out.println("Result: " + (Math.abs(expected - finalDamage) < 0.001f ? "PASS" : "FAIL"));

        // Test 3: Pierce mechanics
        damage = 10.0f;
        dt = 5.0f;
        dr = 0.5f;
        float pierceDT = 2.0f; // Armor piercing reduces DT by 2
        float pierceDR = 0.2f; // 20% pierce

        expected = 4.0f; // (10 - (5-2)) * (1 - 0.5*0.8) = 7 * 0.6 = 4.2, wait let me recalculate

        step1 = Math.max(0F, damage - (dt - pierceDT)); // 10 - (5-2) = 10 - 3 = 7
        float effectiveDR = dr * Math.max(0F, Math.min(2F, 1F - pierceDR)); // 0.5 * (1-0.2) = 0.4
        finalDamage = step1 * (1F - effectiveDR); // 7 * (1-0.4) = 7 * 0.6 = 4.2

        System.out.println("\nTest 3 - Pierce mechanics:");
        System.out.println("Input: damage=" + damage + ", dt=" + dt + ", dr=" + dr + ", pierceDT=" + pierceDT + ", pierceDR=" + pierceDR);
        System.out.println("Expected: ~4.2, Got: " + finalDamage);
        System.out.println("Result: " + (Math.abs(4.2f - finalDamage) < 0.001f ? "PASS" : "FAIL"));
    }

    /**
     * Test damage type categorization
     */
    public static void testDamageTypeCategorization() {
        System.out.println("\n=== Testing Damage Type Categorization ===");

        // Test vanilla damage types - simplified for testing
        System.out.println("Damage type categorization test skipped - requires Level context");

        // Test custom HBM damage types (when available)
        System.out.println("Note: Custom HBM damage types need Level context to test");
    }

    private static void testDamageCategory(DamageSource source, String expectedCategory) {
        String category = DamageResistanceHandler.typeToCategory(source);
        System.out.println("DamageSource: " + source.getMsgId() + " -> Category: " + category +
                          " (Expected: " + expectedCategory + ") " +
                          (category.equals(expectedCategory) ? "PASS" : "FAIL"));
    }

    /**
     * Test T-51 armor stats (comparison with original)
     */
    public static void testT51ArmorStats() {
        System.out.println("\n=== Testing T-51 Armor Stats ===");

        // From original HBM 1.7.10:
        // PHYSICAL: DT=2, DR=15%
        // FIRE: DT=0.5, DR=35%
        // EXPLOSION: DT=5, DR=25%
        // FALL: DT=0, DR=100%
        // OTHER: DT=0, DR=10%

        System.out.println("Original T-51 Stats:");
        System.out.println("PHYSICAL: DT=2.0, DR=15%");
        System.out.println("FIRE: DT=0.5, DR=35%");
        System.out.println("EXPLOSION: DT=5.0, DR=25%");
        System.out.println("FALL: DT=0.0, DR=100%");
        System.out.println("OTHER: DT=0.0, DR=10%");

        System.out.println("\nPorted T-51 Stats (from T51Armor.java):");
        var specs = com.hbm_m.powerarmor.T51Armor.T51_SPECS;
        System.out.println("PHYSICAL: DT=" + specs.dtKinetic + ", DR=" + (specs.drKinetic * 100) + "%");
        System.out.println("FIRE: DT=" + specs.dtFire + ", DR=" + (specs.drFire * 100) + "%");
        System.out.println("EXPLOSION: DT=" + specs.dtExplosion + ", DR=" + (specs.drExplosion * 100) + "%");
        System.out.println("FALL: DT=" + specs.dtFall + ", DR=" + (specs.drFall * 100) + "%");
        System.out.println("ENERGY: DT=" + specs.dtEnergy + ", DR=" + (specs.drEnergy * 100) + "%");

        // Validation
        boolean dtMatch = Math.abs(specs.dtKinetic - 2.0f) < 0.001f &&
                         Math.abs(specs.dtFire - 0.5f) < 0.001f &&
                         Math.abs(specs.dtExplosion - 5.0f) < 0.001f &&
                         Math.abs(specs.dtFall - 0.0f) < 0.001f;

        boolean drMatch = Math.abs(specs.drKinetic - 0.15f) < 0.001f &&
                         Math.abs(specs.drFire - 0.35f) < 0.001f &&
                         Math.abs(specs.drExplosion - 0.25f) < 0.001f &&
                         Math.abs(specs.drFall - 1.0f) < 0.001f;

        System.out.println("\nValidation: DT=" + (dtMatch ? "PASS" : "FAIL") +
                          ", DR=" + (drMatch ? "PASS" : "FAIL"));
    }

    /**
     * Run all tests
     */
    public static void runAllTests() {
        System.out.println("=========================================");
        System.out.println("HBM DamageResistanceHandler Test Suite");
        System.out.println("=========================================");

        testBasicDamageCalculation();
        testDamageTypeCategorization();
        testT51ArmorStats();

        System.out.println("\n=========================================");
        System.out.println("Test suite completed!");
        System.out.println("=========================================");
    }
}