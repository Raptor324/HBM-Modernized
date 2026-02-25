package com.hbm_m.config;

import net.minecraftforge.common.ForgeConfigSpec;

/**
 * Конфигурация машин.
 * Значения загружаются из файла конфигурации и могут быть изменены пользователем.
 */
public class MachineConfig {

    public static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec SPEC;

    //=====================================================================================//
    // FRACTURING TOWER CONFIG
    //=====================================================================================//

    public static final ForgeConfigSpec.LongValue MAX_POWER;
    public static final ForgeConfigSpec.LongValue CONSUMPTION;
    public static final ForgeConfigSpec.IntValue SOLUTION_REQUIRED;
    public static final ForgeConfigSpec.IntValue DELAY;
    public static final ForgeConfigSpec.IntValue OIL_PER_DEPOSIT;
    public static final ForgeConfigSpec.IntValue GAS_PER_DEPOSIT_MIN;
    public static final ForgeConfigSpec.IntValue GAS_PER_DEPOSIT_MAX;
    public static final ForgeConfigSpec.DoubleValue DRAIN_CHANCE;
    public static final ForgeConfigSpec.IntValue OIL_PER_BEDROCK_DEPOSIT;
    public static final ForgeConfigSpec.IntValue GAS_PER_BEDROCK_DEPOSIT_MIN;
    public static final ForgeConfigSpec.IntValue GAS_PER_BEDROCK_DEPOSIT_MAX;
    public static final ForgeConfigSpec.IntValue DESTRUCTION_RANGE;

    //=====================================================================================//
    // КЛАСС КОНФИГУРАЦИИ ДЛЯ ПЕРЕДАЧИ В BLOCKENTITY
    //=====================================================================================//

    public static class FrackingTowerConfig {
        public long maxPower;
        public long consumption;
        public int solutionRequired;
        public int delay;
        public int oilPerDeposit;
        public int gasPerDepositMin;
        public int gasPerDepositMax;
        public double drainChance;
        public int oilPerBedrockDeposit;
        public int gasPerBedrockDepositMin;
        public int gasPerBedrockDepositMax;
        public int destructionRange;

        public FrackingTowerConfig() {
            this.maxPower = MAX_POWER.get();
            this.consumption = CONSUMPTION.get();
            this.solutionRequired = SOLUTION_REQUIRED.get();
            this.delay = DELAY.get();
            this.oilPerDeposit = OIL_PER_DEPOSIT.get();
            this.gasPerDepositMin = GAS_PER_DEPOSIT_MIN.get();
            this.gasPerDepositMax = GAS_PER_DEPOSIT_MAX.get();
            this.drainChance = DRAIN_CHANCE.get();
            this.oilPerBedrockDeposit = OIL_PER_BEDROCK_DEPOSIT.get();
            this.gasPerBedrockDepositMin = GAS_PER_BEDROCK_DEPOSIT_MIN.get();
            this.gasPerBedrockDepositMax = GAS_PER_BEDROCK_DEPOSIT_MAX.get();
            this.destructionRange = DESTRUCTION_RANGE.get();
        }
    }

    static {
        BUILDER.push("fracking_tower");

        MAX_POWER = BUILDER
                .comment("Maximum energy storage (HE)")
                .defineInRange("maxPower", 5_000_000L, 1_000L, Long.MAX_VALUE);

        CONSUMPTION = BUILDER
                .comment("Energy consumption per operation (HE)")
                .defineInRange("consumption", 5_000L, 1L, Long.MAX_VALUE);

        SOLUTION_REQUIRED = BUILDER
                .comment("FrackSol required per operation (mB)")
                .defineInRange("solutionRequired", 10, 1, 10_000);

        DELAY = BUILDER
                .comment("Delay between operations (ticks)")
                .defineInRange("delay", 20, 1, 1200);

        OIL_PER_DEPOSIT = BUILDER
                .comment("Oil produced per regular oil ore (mB)")
                .defineInRange("oilPerDeposit", 1000, 1, 64_000);

        GAS_PER_DEPOSIT_MIN = BUILDER
                .comment("Minimum gas produced per regular oil ore (mB)")
                .defineInRange("gasPerDepositMin", 100, 1, 64_000);

        GAS_PER_DEPOSIT_MAX = BUILDER
                .comment("Maximum gas produced per regular oil ore (mB)")
                .defineInRange("gasPerDepositMax", 500, 1, 64_000);

        DRAIN_CHANCE = BUILDER
                .comment("Chance for oil ore to deplete (0.0 - 1.0)")
                .defineInRange("drainChance", 0.02D, 0.0D, 1.0D);

        OIL_PER_BEDROCK_DEPOSIT = BUILDER
                .comment("Oil produced per bedrock oil ore (mB)")
                .defineInRange("oilPerBedrockDeposit", 100, 1, 64_000);

        GAS_PER_BEDROCK_DEPOSIT_MIN = BUILDER
                .comment("Minimum gas produced per bedrock oil ore (mB)")
                .defineInRange("gasPerBedrockDepositMin", 10, 1, 64_000);

        GAS_PER_BEDROCK_DEPOSIT_MAX = BUILDER
                .comment("Maximum gas produced per bedrock oil ore (mB)")
                .defineInRange("gasPerBedrockDepositMax", 50, 1, 64_000);

        DESTRUCTION_RANGE = BUILDER
                .comment("Range for oil spot generation")
                .defineInRange("destructionRange", 75, 1, 256);

        BUILDER.pop();

        SPEC = BUILDER.build();
    }

    /**
     * Получение конфигурации Fracking Tower.
     */
    public static FrackingTowerConfig getFrackingTowerConfig() {
        return new FrackingTowerConfig();
    }
}
